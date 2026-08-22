package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ExplorerMain {
    private record Node(int id, int parent, String kind, String name, String text,
                        int line, int column, int stopLine, int tokenStart, int tokenStop,
                        int depth, int childCount) {}

    private ExplorerMain() {}

    public static void main(String[] args) throws Exception {
        Path project = Path.of("").toAbsolutePath().normalize();
        Path source = project.resolve(argument(args, "--source", "corpus/cbl/COACTUPC.cbl"));
        Path copybooks = project.resolve(argument(args, "--copybooks", "corpus/cpy"));
        Path output = project.resolve(argument(args, "--output", "dist"));

        GrammarBinding binding = Bindings.proleap();
        List<Diagnostic> diagnostics = new ArrayList<>();
        String raw = Files.readString(source, StandardCharsets.UTF_8);
        String normalized = SourceNormalizer.fixed(raw);
        PreprocessorEngine.Outcome preprocessed =
                new PreprocessorEngine(binding, new CopybookLibrary(copybooks))
                        .process(normalized, source.getFileName().toString());
        normalized = preprocessed.text();
        diagnostics.addAll(preprocessed.diagnostics());

        Lexer lexer = binding.cobolLexer(CharStreams.fromString(normalized, source.getFileName().toString()));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new AntlrDiagnosticListener(binding.name(), Diagnostic.Phase.LEXER,
                source.getFileName().toString(), diagnostics));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        tokens.seek(0);

        Parser parser = binding.cobolParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new AntlrDiagnosticListener(binding.name(), Diagnostic.Phase.PARSER,
                source.getFileName().toString(), diagnostics));
        ParseTree tree = binding.cobolStart(parser);

        List<Node> nodes = new ArrayList<>();
        Map<String, Integer> ruleCounts = new TreeMap<>();
        IdentityHashMap<ParseTree, Integer> parseIds = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> parseSubtreeSizes = new IdentityHashMap<>();
        walk(tree, -1, 0, parser, nodes, ruleCounts, parseIds, parseSubtreeSizes);
        int maxDepth = nodes.stream().mapToInt(Node::depth).max().orElse(0);
        long tokenCount = tokens.getTokens().stream().filter(t -> t.getType() != Token.EOF).count();
        long lexerErrors = diagnostics.stream().filter(d -> d.phase() == Diagnostic.Phase.LEXER).count();
        long parserErrors = diagnostics.stream().filter(d -> d.phase() == Diagnostic.Phase.PARSER).count();

        Files.createDirectories(output);
        copyWebResources(output);
        Files.writeString(output.resolve("preprocessed.cbl"), normalized, StandardCharsets.UTF_8);
        writeData(output.resolve("tree-data.js"), source.getFileName().toString(), raw.lines().count(),
                normalized, preprocessed.unresolved(), tokenCount, maxDepth, lexerErrors, parserErrors,
                nodes, ruleCounts, diagnostics);

        Ast.Program ast = new AstBuilder(parser, normalized, parseIds, parseSubtreeSizes).build(tree);
        AstSnapshot astSnapshot = AstSnapshot.from(ast);
        astSnapshot.write(output.resolve("ast-data.js"), source.getFileName().toString(), nodes.size(),
                Arrays.asList(normalized.split("\\R", -1)));

        SymbolTable symbolTable = new SymbolTableBuilder().build(ast);
        SymbolTableSnapshot symbolSnapshot = SymbolTableSnapshot.from(symbolTable);
        symbolSnapshot.write(output.resolve("symbol-data.js"), source.getFileName().toString(),
                Arrays.asList(normalized.split("\\R", -1)));

        System.out.printf(Locale.ROOT,
                "Generated %s, %s and %s%nSource: %s%nParse tree: %,d nodes | %,d tokens | depth %d%n" +
                        "AST: %,d nodes | depth %d | static CALLs %d%n" +
                        "Symbols: %,d declarations | %,d scopes | %,d diagnostics | parser errors %d%n",
                output.resolve("index.html"), output.resolve("ast.html"), output.resolve("symbols.html"),
                source.getFileName(), nodes.size(),
                tokenCount, maxDepth, astSnapshot.metrics().nodes(), astSnapshot.metrics().maxDepth(),
                astSnapshot.metrics().staticCalls(), symbolSnapshot.metrics().symbols(),
                symbolSnapshot.metrics().scopes(), symbolSnapshot.metrics().diagnostics(), parserErrors);
    }

    private static int walk(ParseTree tree, int parent, int depth, Parser parser,
                            List<Node> nodes, Map<String, Integer> ruleCounts,
                            IdentityHashMap<ParseTree, Integer> parseIds,
                            IdentityHashMap<ParseTree, Integer> parseSubtreeSizes) {
        int id = nodes.size();
        parseIds.put(tree, id);
        String kind;
        String name;
        String text = "";
        int line = 0, column = 0, stopLine = 0, tokenStart = -1, tokenStop = -1;

        if (tree instanceof ParserRuleContext context) {
            kind = "rule";
            name = parser.getRuleNames()[context.getRuleIndex()];
            ruleCounts.merge(name, 1, Integer::sum);
            Token start = context.getStart(), stop = context.getStop();
            if (start != null) {
                line = start.getLine(); column = start.getCharPositionInLine(); tokenStart = start.getTokenIndex();
            }
            if (stop != null) {
                stopLine = stop.getLine(); tokenStop = stop.getTokenIndex();
            }
        } else if (tree instanceof ErrorNode error) {
            kind = "error";
            Token token = error.getSymbol();
            name = tokenName(parser, token);
            text = token.getText();
            line = stopLine = token.getLine(); column = token.getCharPositionInLine();
            tokenStart = tokenStop = token.getTokenIndex();
        } else if (tree instanceof TerminalNode terminal) {
            kind = "terminal";
            Token token = terminal.getSymbol();
            name = tokenName(parser, token);
            text = token.getText();
            line = stopLine = token.getLine(); column = token.getCharPositionInLine();
            tokenStart = tokenStop = token.getTokenIndex();
        } else {
            kind = "unknown";
            name = tree.getClass().getSimpleName();
        }

        nodes.add(new Node(id, parent, kind, name, text, line, column, stopLine,
                tokenStart, tokenStop, depth, tree.getChildCount()));
        int subtreeSize = 1;
        for (int i = 0; i < tree.getChildCount(); i++) {
            subtreeSize += walk(tree.getChild(i), id, depth + 1, parser, nodes, ruleCounts,
                    parseIds, parseSubtreeSizes);
        }
        parseSubtreeSizes.put(tree, subtreeSize);
        return subtreeSize;
    }

    private static String tokenName(Parser parser, Token token) {
        if (token.getType() == Token.EOF) return "EOF";
        String symbolic = parser.getVocabulary().getSymbolicName(token.getType());
        if (symbolic != null) return symbolic;
        String literal = parser.getVocabulary().getLiteralName(token.getType());
        return literal == null ? "TOKEN_" + token.getType() : literal;
    }

    private static void copyWebResources(Path output) throws IOException {
        Path resources = Path.of("src/main/resources/web");
        try (var files = Files.walk(resources)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Files.copy(file, output.resolve(resources.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void writeData(Path path, String sourceName, long originalLines, String source,
                                  int unresolvedCopies, long tokenCount, int maxDepth,
                                  long lexerErrors, long parserErrors, List<Node> nodes,
                                  Map<String, Integer> ruleCounts, List<Diagnostic> diagnostics) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            out.write("window.PARSE_TREE_DATA={\n\"meta\":{");
            field(out, "source", sourceName); out.write(',');
            out.write("\"originalLines\":" + originalLines + ',');
            out.write("\"preprocessedLines\":" + source.lines().count() + ',');
            out.write("\"nodes\":" + nodes.size() + ',');
            out.write("\"tokens\":" + tokenCount + ',');
            out.write("\"maxDepth\":" + maxDepth + ',');
            out.write("\"unresolvedCopies\":" + unresolvedCopies + ',');
            out.write("\"lexerErrors\":" + lexerErrors + ',');
            out.write("\"parserErrors\":" + parserErrors);
            out.write("},\n\"sourceLines\":[");
            String[] lines = source.split("\\R", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) out.write(',');
                string(out, lines[i]);
            }
            out.write("],\n\"nodes\":[");
            for (int i = 0; i < nodes.size(); i++) {
                if (i > 0) out.write(',');
                Node n = nodes.get(i);
                out.write("{\"id\":" + n.id + ",\"p\":" + n.parent + ",\"k\":"); string(out, n.kind);
                out.write(",\"n\":"); string(out, n.name);
                if (!n.text.isEmpty()) { out.write(",\"x\":"); string(out, n.text); }
                out.write(",\"l\":" + n.line + ",\"c\":" + n.column + ",\"e\":" + n.stopLine);
                out.write(",\"a\":" + n.tokenStart + ",\"b\":" + n.tokenStop);
                out.write(",\"d\":" + n.depth + ",\"q\":" + n.childCount + '}');
            }
            out.write("],\n\"ruleCounts\":{");
            boolean first = true;
            for (var entry : ruleCounts.entrySet()) {
                if (!first) out.write(','); first = false;
                string(out, entry.getKey()); out.write(':' + String.valueOf(entry.getValue()));
            }
            out.write("},\n\"diagnostics\":[");
            for (int i = 0; i < diagnostics.size(); i++) {
                if (i > 0) out.write(',');
                Diagnostic d = diagnostics.get(i);
                out.write("{\"phase\":"); string(out, d.phase().name());
                out.write(",\"line\":" + d.line() + ",\"message\":"); string(out, d.message());
                out.write('}');
            }
            out.write("]};\n");
        }
    }

    private static void field(Writer out, String name, String value) throws IOException {
        string(out, name); out.write(':'); string(out, value);
    }

    private static void string(Writer out, String value) throws IOException {
        out.write('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.write("\\\"");
                case '\\' -> out.write("\\\\");
                case '\b' -> out.write("\\b");
                case '\f' -> out.write("\\f");
                case '\n' -> out.write("\\n");
                case '\r' -> out.write("\\r");
                case '\t' -> out.write("\\t");
                default -> {
                    if (ch < 0x20 || ch == '\u2028' || ch == '\u2029') out.write(String.format("\\u%04x", (int) ch));
                    else out.write(ch);
                }
            }
        }
        out.write('"');
    }

    private static String argument(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) if (args[i].equals(name)) return args[i + 1];
        return fallback;
    }
}
