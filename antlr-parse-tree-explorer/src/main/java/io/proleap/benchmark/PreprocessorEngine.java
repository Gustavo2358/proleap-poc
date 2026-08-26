package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

final class PreprocessorEngine {
    enum PreprocessorPolicy {
        PASS_THROUGH,
        REMOVE,
        EXPAND_COPY,
        UNSUPPORTED,
        PRESERVE_EMBEDDED_LANGUAGE,
        EXTRACT_COMPILER_OPTIONS
    }

    private static final Map<String, PreprocessorPolicy> POLICIES = Map.ofEntries(
            Map.entry("compilerOptions", PreprocessorPolicy.EXTRACT_COMPILER_OPTIONS),
            Map.entry("copyStatement", PreprocessorPolicy.EXPAND_COPY),
            Map.entry("execCicsStatement", PreprocessorPolicy.PRESERVE_EMBEDDED_LANGUAGE),
            Map.entry("execSqlStatement", PreprocessorPolicy.PRESERVE_EMBEDDED_LANGUAGE),
            Map.entry("execSqlImsStatement", PreprocessorPolicy.PRESERVE_EMBEDDED_LANGUAGE),
            Map.entry("replaceOffStatement", PreprocessorPolicy.UNSUPPORTED),
            Map.entry("replaceArea", PreprocessorPolicy.UNSUPPORTED),
            Map.entry("ejectStatement", PreprocessorPolicy.REMOVE),
            Map.entry("skipStatement", PreprocessorPolicy.REMOVE),
            Map.entry("titleStatement", PreprocessorPolicy.REMOVE),
            Map.entry("charDataLine", PreprocessorPolicy.PASS_THROUGH),
            Map.entry("NEWLINE", PreprocessorPolicy.PASS_THROUGH));

    record CompilerOption(String name, String value, String writtenText) {
        CompilerOption {
            name = Objects.requireNonNull(name, "name");
            value = value == null ? "" : value;
            writtenText = Objects.requireNonNull(writtenText, "writtenText");
        }
    }
    record Outcome(String text, int errors, int unresolved, List<Diagnostic> diagnostics,
                   List<CompilerOption> compilerOptions,
                   ResolutionContracts.PgmnameMode pgmnameMode,
                   ResolutionContracts.DynamMode dynamMode,
                   ResolutionContracts.DllMode dllMode, SourceMap sourceMap) {
        Outcome {
            diagnostics = List.copyOf(diagnostics);
            compilerOptions = List.copyOf(compilerOptions);
            pgmnameMode = Objects.requireNonNull(pgmnameMode, "pgmnameMode");
            dynamMode = Objects.requireNonNull(dynamMode, "dynamMode");
            dllMode = Objects.requireNonNull(dllMode, "dllMode");
        }
    }
    private record Edit(int start, int end, SourceMap replacement) {}
    private record CopyReplacement(String replaceable, String replacement) {}

    private final GrammarBinding binding;
    private final CopybookLibrary library;

    PreprocessorEngine(GrammarBinding binding, CopybookLibrary library) {
        this.binding = binding; this.library = library;
    }

    Outcome process(SourceMap normalized, String file) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        List<CompilerOption> compilerOptions = new ArrayList<>();
        int[] unresolved = {0};
        SourceMap document = processRecursive(normalized, file,
                diagnostics, compilerOptions, unresolved, new HashSet<>());
        long errors = diagnostics.stream().filter(d -> d.phase() == Diagnostic.Phase.PREPROCESSOR)
                .filter(d -> !d.message().startsWith("unresolved_copy") && !d.message().startsWith("cyclic COPY")).count();
        ResolutionContracts.PgmnameMode pgmnameMode = compilerOptions.stream()
                .filter(option -> option.name().equals("PGMNAME") || option.name().equals("PGMN"))
                .map(CompilerOption::value).map(ResolutionContracts.PgmnameMode::fromCompilerValue)
                .reduce((first, second) -> second).orElse(ResolutionContracts.PgmnameMode.UNSPECIFIED);
        ResolutionContracts.DynamMode dynamMode = compilerOptions.stream()
                .map(option -> ResolutionContracts.DynamMode.fromCompilerOption(
                        option.name(), option.value()))
                .filter(mode -> mode != ResolutionContracts.DynamMode.UNSPECIFIED)
                .reduce((first, second) -> second).orElse(ResolutionContracts.DynamMode.UNSPECIFIED);
        ResolutionContracts.DllMode dllMode = compilerOptions.stream()
                .map(option -> ResolutionContracts.DllMode.fromCompilerOption(
                        option.name(), option.value()))
                .filter(mode -> mode != ResolutionContracts.DllMode.UNSPECIFIED)
                .reduce((first, second) -> second).orElse(ResolutionContracts.DllMode.UNSPECIFIED);
        return new Outcome(document.text(), Math.toIntExact(errors), unresolved[0],
                diagnostics, compilerOptions, pgmnameMode, dynamMode, dllMode, document);
    }

    private SourceMap processRecursive(SourceMap document, String file, List<Diagnostic> diagnostics,
                                       List<CompilerOption> compilerOptions,
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
        validatePolicyCatalog(tree.getClass());

        List<ParserRuleContext> actionable = new ArrayList<>();
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof ParserRuleContext context) {
                String rule = parser.getRuleNames()[context.getRuleIndex()];
                PreprocessorPolicy policy = policyFor(rule);
                if (policy == PreprocessorPolicy.EXTRACT_COMPILER_OPTIONS) {
                    collectCompilerOptions(context, parser.getRuleNames(), source, compilerOptions);
                }
                if (policy != PreprocessorPolicy.PASS_THROUGH) actionable.add(context);
            } else if (child instanceof TerminalNode terminal
                    && terminal.getSymbol().getType() != Token.EOF) {
                String token = parser.getVocabulary().getSymbolicName(terminal.getSymbol().getType());
                PreprocessorPolicy policy = policyFor(token);
                if (policy != PreprocessorPolicy.PASS_THROUGH) {
                    throw new IllegalStateException(
                            "Terminal preprocessing construct requires rule-based handling: " + token);
                }
            }
        }
        List<Edit> edits = new ArrayList<>();
        for (ParserRuleContext context : actionable) {
            Token startToken = context.getStart(), stopToken = context.getStop();
            if (startToken == null || stopToken == null || startToken.getStartIndex() < 0
                    || stopToken.getStopIndex() < startToken.getStartIndex()
                    || stopToken.getStopIndex() >= source.length()) {
                throw new IllegalStateException("Actionable preprocessing context has no valid source interval: "
                        + context.getClass().getSimpleName());
            }
            int start = startToken.getStartIndex();
            int end = stopToken.getStopIndex() + 1;
            String rule = parser.getRuleNames()[context.getRuleIndex()];
            PreprocessorPolicy policy = policyFor(rule);
            String original = source.substring(start, end);
            if (policy == PreprocessorPolicy.EXTRACT_COMPILER_OPTIONS
                    || policy == PreprocessorPolicy.REMOVE) {
                edits.add(new Edit(start, end, document.transformedSlice(
                        start, end, blankPreservingLineBreaks(original))));
            } else if (policy == PreprocessorPolicy.EXPAND_COPY) {
                String requested = copySourceName(context, parser.getRuleNames(), source);
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
                    edits.add(new Edit(start, end, document.transformedSlice(start, end,
                            "*> CYCLIC COPY " + requested + "\n")));
                } else {
                    try {
                        String includedFile = path.get().getFileName().toString();
                        SourceMap copySource = library.readNormalized(path.get());
                        SourceMap copyText = processRecursive(copySource, includedFile,
                                diagnostics, compilerOptions, unresolved, expansionStack);
                        for (CopyReplacement replacement : copyReplacements(
                                context, parser.getRuleNames(), source)) {
                            copyText = copyText.replaceLiteral(
                                    replacement.replaceable(), replacement.replacement());
                        }
                        int includeLine = document.provenance(start, end).original().startLine();
                        Ast.CopyFrame frame = new Ast.CopyFrame(file, requested, includedFile, includeLine);
                        edits.add(new Edit(start, end, copyText.withCopyFrame(frame)));
                    } catch (IOException e) {
                        diagnostics.add(new Diagnostic(binding.name(), Diagnostic.Phase.IO, file, 0, 0,
                                e.getMessage(), requested, e.getClass().getName()));
                        edits.add(new Edit(start, end, document.transformedSlice(start, end,
                                "*> COPY IO ERROR " + requested + "\n")));
                    } finally {
                        expansionStack.remove(path.get().toAbsolutePath().normalize());
                    }
                }
            } else if (policy == PreprocessorPolicy.PRESERVE_EMBEDDED_LANGUAGE) {
                String tag = switch (rule) {
                    case "execCicsStatement" -> "*>EXECCICS";
                    case "execSqlStatement" -> "*>EXECSQL";
                    case "execSqlImsStatement" -> "*>EXECSQLIMS";
                    default -> throw new IllegalStateException(
                            "Embedded-language policy has no renderer: " + rule);
                };
                String opaque = original.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
                boolean sentenceEnd = opaque.endsWith(".");
                if (sentenceEnd) opaque = opaque.substring(0, opaque.length() - 1).stripTrailing();
                edits.add(new Edit(start, end, document.transformedSlice(start, end,
                        tag + " " + opaque + "\n" + (sentenceEnd ? ". \n" : ""))));
            } else if (policy == PreprocessorPolicy.UNSUPPORTED) {
                throw new UnsupportedOperationException(
                        "Unsupported preprocessing construct: " + rule);
            } else {
                throw new IllegalStateException(
                        "Unexpected actionable preprocessing policy for " + rule + ": " + policy);
            }
        }
        edits.sort(Comparator.comparingInt(Edit::start).reversed());
        SourceMap result = document;
        int lastStart = Integer.MAX_VALUE;
        for (Edit edit : edits) {
            if (edit.end() > lastStart) {
                throw new IllegalStateException("Overlapping top-level preprocessing edits at "
                        + edit.start() + ".." + edit.end());
            }
            result = result.replace(edit.start(), edit.end(), edit.replacement());
            lastStart = edit.start();
        }
        return result;
    }

    static Map<String, PreprocessorPolicy> policies() {
        return POLICIES;
    }

    static PreprocessorPolicy policyFor(String construct) {
        PreprocessorPolicy policy = POLICIES.get(construct);
        if (policy == null) {
            throw new IllegalStateException(
                    "Preprocessor grammar construct has no explicit policy: " + construct);
        }
        return policy;
    }

    private static void validatePolicyCatalog(Class<?> startRuleContextClass) {
        Set<String> grammarConstructs = new HashSet<>();
        for (java.lang.reflect.Method method : startRuleContextClass.getDeclaredMethods()) {
            if (method.getParameterCount() != 0
                    || !(method.getGenericReturnType() instanceof ParameterizedType listType)) continue;
            Type[] arguments = listType.getActualTypeArguments();
            if (arguments.length != 1 || !(arguments[0] instanceof Class<?> elementType)) continue;
            if (ParserRuleContext.class.isAssignableFrom(elementType)
                    || TerminalNode.class.isAssignableFrom(elementType)) {
                grammarConstructs.add(method.getName());
            }
        }
        if (!grammarConstructs.equals(POLICIES.keySet())) {
            Set<String> missing = new TreeSet<>(grammarConstructs);
            missing.removeAll(POLICIES.keySet());
            Set<String> stale = new TreeSet<>(POLICIES.keySet());
            stale.removeAll(grammarConstructs);
            throw new IllegalStateException("Preprocessor policy catalog does not match startRule; "
                    + "missing policies=" + missing + ", stale policies=" + stale);
        }
    }

    private static String blankPreservingLineBreaks(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            result.append(character == '\n' || character == '\r' ? character : ' ');
        }
        return result.toString();
    }

    private static String copySourceName(ParserRuleContext copyStatement, String[] ruleNames,
                                         String source) {
        ParserRuleContext copySource = directRuleChild(copyStatement, ruleNames, "copySource");
        if (copySource == null) {
            throw new IllegalStateException("COPY recognized without copySource context");
        }
        for (int i = 0; i < copySource.getChildCount(); i++) {
            if (!(copySource.getChild(i) instanceof ParserRuleContext candidate)) continue;
            String rule = ruleNames[candidate.getRuleIndex()];
            if (rule.equals("literal") || rule.equals("cobolWord") || rule.equals("filename")) {
                return unquote(sourceText(candidate, source).trim());
            }
        }
        throw new IllegalStateException("COPY recognized without a supported source name");
    }

    private static List<CopyReplacement> copyReplacements(ParserRuleContext copyStatement,
                                                            String[] ruleNames, String source) {
        List<CopyReplacement> result = new ArrayList<>();
        collectCopyReplacements(copyStatement, ruleNames, source, result);
        return result;
    }

    private static void collectCopyReplacements(ParseTree tree, String[] ruleNames, String source,
                                                List<CopyReplacement> out) {
        if (tree instanceof ParserRuleContext context
                && ruleNames[context.getRuleIndex()].equals("replaceClause")) {
            ParserRuleContext replaceable = directRuleChild(context, ruleNames, "replaceable");
            ParserRuleContext replacement = directRuleChild(context, ruleNames, "replacement");
            if (replaceable == null || replacement == null) {
                throw new IllegalStateException("COPY REPLACING clause has an incomplete parse tree");
            }
            out.add(new CopyReplacement(replacementOperand(replaceable, ruleNames, source),
                    replacementOperand(replacement, ruleNames, source)));
            return;
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            collectCopyReplacements(tree.getChild(i), ruleNames, source, out);
        }
    }

    private static String replacementOperand(ParserRuleContext operand, String[] ruleNames,
                                             String source) {
        ParserRuleContext pseudoText = directRuleChild(operand, ruleNames, "pseudoText");
        String written = sourceText(pseudoText == null ? operand : pseudoText, source).trim();
        if (pseudoText != null) {
            if (written.length() < 4 || !written.startsWith("==") || !written.endsWith("==")) {
                throw new IllegalStateException("Malformed pseudo-text in COPY REPLACING: " + written);
            }
            return written.substring(2, written.length() - 2).trim();
        }
        return written;
    }

    private static ParserRuleContext directRuleChild(ParserRuleContext parent, String[] ruleNames,
                                                     String expectedRule) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChild(i) instanceof ParserRuleContext child
                    && ruleNames[child.getRuleIndex()].equals(expectedRule)) return child;
        }
        return null;
    }

    private static String sourceText(ParserRuleContext context, String source) {
        Token start = context.getStart();
        Token stop = context.getStop();
        if (start == null || stop == null || start.getStartIndex() < 0
                || stop.getStopIndex() < start.getStartIndex()
                || stop.getStopIndex() >= source.length()) {
            throw new IllegalStateException("Preprocessor context has no valid source interval: "
                    + context.getClass().getSimpleName());
        }
        return source.substring(start.getStartIndex(), stop.getStopIndex() + 1);
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static void collectCompilerOptions(ParseTree tree, String[] ruleNames, String source,
                                               List<CompilerOption> out) {
        if (tree instanceof ParserRuleContext context
                && ruleNames[context.getRuleIndex()].equals("compilerOption")) {
            List<String> terminals = new ArrayList<>();
            collectTerminals(context, terminals);
            if (!terminals.isEmpty()) {
                String name = terminals.get(0).toUpperCase(Locale.ROOT);
                String value = terminals.stream()
                        .filter(token -> !token.equals("(") && !token.equals(")") && !token.equals(","))
                        .skip(1).reduce((first, second) -> second).orElse("");
                Token start = context.getStart(), stop = context.getStop();
                int from = start == null ? -1 : start.getStartIndex();
                int to = stop == null ? -1 : stop.getStopIndex() + 1;
                String written = from >= 0 && to >= from && to <= source.length()
                        ? source.substring(from, to) : context.getText();
                out.add(new CompilerOption(name, value, written));
            }
        }
        for (int i = 0; i < tree.getChildCount(); i++)
            collectCompilerOptions(tree.getChild(i), ruleNames, source, out);
    }

    private static void collectTerminals(ParseTree tree, List<String> out) {
        if (tree instanceof TerminalNode terminal) {
            out.add(terminal.getText());
            return;
        }
        for (int i = 0; i < tree.getChildCount(); i++) collectTerminals(tree.getChild(i), out);
    }
}
