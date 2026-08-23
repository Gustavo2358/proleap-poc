package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

final class PreprocessorEngine {
    record Outcome(String text, int errors, int unresolved, List<Diagnostic> diagnostics,
                   SourceMap sourceMap) {}
    private record Edit(int start, int end, SourceMap replacement) {}
    private static final Pattern COPY = Pattern.compile("(?is)\\bCOPY\\s+(['\"]?)([A-Z0-9_-]+(?:\\.[A-Z0-9]+)?)\\1");
    private static final Pattern PSEUDO_REPLACE = Pattern.compile("(?is)==(.*?)==\\s+BY\\s+==(.*?)==");

    private final GrammarBinding binding;
    private final CopybookLibrary library;

    PreprocessorEngine(GrammarBinding binding, CopybookLibrary library) {
        this.binding = binding; this.library = library;
    }

    Outcome process(String normalized, String file) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        int[] unresolved = {0};
        SourceMap document = processRecursive(SourceMap.identity(normalized, file), file,
                diagnostics, unresolved, new HashSet<>());
        long errors = diagnostics.stream().filter(d -> d.phase() == Diagnostic.Phase.PREPROCESSOR)
                .filter(d -> !d.message().startsWith("unresolved_copy") && !d.message().startsWith("cyclic COPY")).count();
        return new Outcome(document.text(), Math.toIntExact(errors), unresolved[0],
                List.copyOf(diagnostics), document);
    }

    private SourceMap processRecursive(SourceMap document, String file, List<Diagnostic> diagnostics,
                                       int[] unresolved, Set<Path> expansionStack) {
        String source = document.text();
        Lexer lexer = binding.preprocessorLexer(CharStreams.fromString(source, file));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new AntlrDiagnosticListener(binding.name(), Diagnostic.Phase.PREPROCESSOR, file, diagnostics));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = binding.preprocessorParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new AntlrDiagnosticListener(binding.name(), Diagnostic.Phase.PREPROCESSOR, file, diagnostics));
        ParseTree tree = binding.preprocessorStart(parser);

        List<ParserRuleContext> actionable = new ArrayList<>();
        collect(tree, parser.getRuleNames(), actionable);
        List<Edit> edits = new ArrayList<>();
        for (ParserRuleContext context : actionable) {
            Token startToken = context.getStart(), stopToken = context.getStop();
            if (startToken == null || stopToken == null || startToken.getStartIndex() < 0) continue;
            int start = startToken.getStartIndex();
            int end = Math.min(source.length(), stopToken.getStopIndex() + 1);
            String rule = parser.getRuleNames()[context.getRuleIndex()];
            String original = source.substring(start, end);
            if (rule.equals("copyStatement")) {
                Matcher matcher = COPY.matcher(original);
                if (!matcher.find()) continue;
                String requested = matcher.group(2);
                Optional<Path> path = library.resolve(requested);
                if (path.isEmpty()) {
                    unresolved[0]++;
                    int line = 1 + (int) source.substring(0, start).chars().filter(c -> c == '\n').count();
                    diagnostics.add(new Diagnostic(binding.name(), Diagnostic.Phase.PREPROCESSOR, file, line, 0,
                            "unresolved_copy: " + requested, requested, ""));
                    edits.add(new Edit(start, end, document.transformedSlice(start, end,
                            "*> UNRESOLVED COPY " + requested + "\n")));
                } else if (!expansionStack.add(path.get().toAbsolutePath().normalize())) {
                    diagnostics.add(new Diagnostic(binding.name(), Diagnostic.Phase.PREPROCESSOR, file, 0, 0,
                            "cyclic COPY: " + requested, requested, ""));
                } else {
                    try {
                        String includedFile = path.get().getFileName().toString();
                        String copySource = library.readNormalized(path.get());
                        SourceMap copyText = processRecursive(SourceMap.identity(copySource, includedFile), includedFile,
                                diagnostics, unresolved, expansionStack);
                        Matcher replacements = PSEUDO_REPLACE.matcher(original);
                        while (replacements.find()) copyText = copyText.replaceLiteral(
                                replacements.group(1).trim(), replacements.group(2).trim());
                        int includeLine = document.provenance(start, end).original().startLine();
                        Ast.CopyFrame frame = new Ast.CopyFrame(file, requested, includedFile, includeLine);
                        edits.add(new Edit(start, end, copyText.withCopyFrame(frame)));
                    } catch (IOException e) {
                        diagnostics.add(new Diagnostic(binding.name(), Diagnostic.Phase.IO, file, 0, 0,
                                e.getMessage(), requested, e.getClass().getName()));
                    } finally {
                        expansionStack.remove(path.get().toAbsolutePath().normalize());
                    }
                }
            } else if (rule.startsWith("exec") && rule.endsWith("Statement")) {
                String tag = rule.equals("execCicsStatement") ? "*>EXECCICS" :
                        rule.equals("execSqlImsStatement") ? "*>EXECSQLIMS" : "*>EXECSQL";
                String opaque = original.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
                boolean sentenceEnd = opaque.endsWith(".");
                if (sentenceEnd) opaque = opaque.substring(0, opaque.length() - 1).stripTrailing();
                edits.add(new Edit(start, end, document.transformedSlice(start, end,
                        tag + " " + opaque + "\n" + (sentenceEnd ? ". \n" : ""))));
            }
        }
        edits.sort(Comparator.comparingInt(Edit::start).reversed());
        SourceMap result = document;
        int lastStart = Integer.MAX_VALUE;
        for (Edit edit : edits) {
            if (edit.end() > lastStart) continue;
            result = result.replace(edit.start(), edit.end(), edit.replacement());
            lastStart = edit.start();
        }
        return result;
    }

    private static void collect(ParseTree tree, String[] ruleNames, List<ParserRuleContext> out) {
        if (tree instanceof ParserRuleContext context) {
            String rule = ruleNames[context.getRuleIndex()];
            if (rule.equals("copyStatement") || rule.equals("execCicsStatement") ||
                    rule.equals("execSqlStatement") || rule.equals("execSqlImsStatement")) out.add(context);
        }
        for (int i = 0; i < tree.getChildCount(); i++) collect(tree.getChild(i), ruleNames, out);
    }
}
