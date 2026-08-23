package io.proleap.benchmark;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Flat, browser-friendly view of the semantic AST. */
final class AstSnapshot {
    record Node(int id, int parent, String type, String category, String label,
                Map<String, String> attributes, int line, int column, int stopLine,
                int depth, int childCount, int parseTreeId, String grammarRule,
                int parseTreeNodes, String sourceFile, int sourceLine,
                int includeDepth, boolean sourceExact) {}

    record Metrics(int nodes, int maxDepth, int staticCalls, int dynamicCalls,
                   int embeddedLanguages, int unsupportedStatements) {}

    private final List<Node> nodes;
    private final Metrics metrics;
    private final Map<String, Integer> typeCounts;
    private final Map<Integer, List<Integer>> parseToAst;

    private AstSnapshot(List<Node> nodes, Metrics metrics, Map<String, Integer> typeCounts,
                        Map<Integer, List<Integer>> parseToAst) {
        this.nodes = List.copyOf(nodes);
        this.metrics = metrics;
        this.typeCounts = Collections.unmodifiableMap(new TreeMap<>(typeCounts));
        Map<Integer, List<Integer>> stableParseIndex = new TreeMap<>();
        parseToAst.forEach((key, value) -> stableParseIndex.put(key, List.copyOf(value)));
        this.parseToAst = Collections.unmodifiableMap(stableParseIndex);
    }

    static AstSnapshot from(Ast.Program program) {
        List<Node> nodes = new ArrayList<>();
        Map<String, Integer> typeCounts = new TreeMap<>();
        Map<Integer, List<Integer>> parseToAst = new TreeMap<>();
        flatten(program, -1, 0, nodes, typeCounts, parseToAst);
        int maxDepth = nodes.stream().mapToInt(Node::depth).max().orElse(0);
        int staticCalls = 0, dynamicCalls = 0, embedded = 0, unsupported = 0;
        for (Node node : nodes) {
            if (node.type.equals("CallStatement")) {
                if ("STATIC_LITERAL".equals(node.attributes.get("targetKind"))) staticCalls++; else dynamicCalls++;
            }
            if (node.type.equals("EmbeddedLanguageStatement")) embedded++;
            if (node.type.equals("UnsupportedStatement")) unsupported++;
        }
        return new AstSnapshot(nodes, new Metrics(nodes.size(), maxDepth, staticCalls, dynamicCalls,
                embedded, unsupported), typeCounts, parseToAst);
    }

    Metrics metrics() { return metrics; }
    List<Node> nodes() { return nodes; }

    private static void flatten(Ast.Node ast, int parent, int depth, List<Node> output,
                                Map<String, Integer> typeCounts, Map<Integer, List<Integer>> parseToAst) {
        int id = output.size();
        if (ast.meta().id() != id)
            throw new IllegalStateException("AST ids are not pre-order: expected " + id + " but got " + ast.meta().id());
        List<? extends Ast.Node> children = Ast.children(ast);
        String type = ast.getClass().getSimpleName();
        typeCounts.merge(type, 1, Integer::sum);
        Ast.ParseTreeOrigin origin = ast.meta().origin();
        Ast.SourceProvenance provenance = ast.meta().provenance();
        if (origin.rootNodeId() >= 0) parseToAst.computeIfAbsent(origin.rootNodeId(), ignored -> new ArrayList<>()).add(id);
        output.add(new Node(id, parent, type, category(ast), label(ast), attributes(ast),
                ast.meta().span().startLine(), ast.meta().span().startColumn(), ast.meta().span().endLine(),
                depth, children.size(), origin.rootNodeId(), origin.grammarRule(), origin.subtreeNodeCount(),
                provenance.original().file(), provenance.original().startLine(),
                provenance.includeChain().size(), provenance.exact()));
        for (Ast.Node child : children) flatten(child, id, depth + 1, output, typeCounts, parseToAst);
    }

    private static String category(Ast.Node node) {
        if (node instanceof Ast.Program) return "program";
        if (node instanceof Ast.Division || node instanceof Ast.Section || node instanceof Ast.Paragraph || node instanceof Ast.Sentence) return "structure";
        if (node instanceof Ast.Statement) return "statement";
        if (node instanceof Ast.Expression) return "expression";
        if (node instanceof Ast.FileBinding || node instanceof Ast.FileDescription || node instanceof Ast.DataEntry) return "declaration";
        return "semantic";
    }

    private static String label(Ast.Node node) {
        if (node instanceof Ast.Program n) return n.name();
        if (node instanceof Ast.Division n) return n.divisionKind().name();
        if (node instanceof Ast.Section n) return n.name();
        if (node instanceof Ast.FileBinding n) return n.logicalName();
        if (node instanceof Ast.FileDescription n) return n.fileName();
        if (node instanceof Ast.DataEntry n) return n.level() + " " + n.name();
        if (node instanceof Ast.DataQualifier n) return n.writtenText();
        if (node instanceof Ast.SubscriptGroup n) return n.writtenText();
        if (node instanceof Ast.ReferenceModification n) return n.writtenText();
        if (node instanceof Ast.Paragraph n) return n.name();
        if (node instanceof Ast.Sentence) return "sentence";
        if (node instanceof Ast.CallStatement n) return expressionLabel(n.target());
        if (node instanceof Ast.CallArgument n) return n.passingMode().name();
        if (node instanceof Ast.IfStatement) return "IF";
        if (node instanceof Ast.EvaluateStatement) return "EVALUATE";
        if (node instanceof Ast.EvaluateBranch n) return n.selector();
        if (node instanceof Ast.PerformStatement n) return n.performKind() == Ast.PerformKind.INLINE ? "inline" : n.fromProcedure();
        if (node instanceof Ast.GoToStatement n) return String.join(", ", n.targets());
        if (node instanceof Ast.MoveStatement) return "MOVE";
        if (node instanceof Ast.EmbeddedLanguageStatement n) return n.language().name();
        if (node instanceof Ast.NextSentenceStatement) return "NEXT SENTENCE";
        if (node instanceof Ast.UnsupportedStatement n) return n.grammarRule();
        if (node instanceof Ast.LiteralExpression n) return n.rawLexeme();
        if (node instanceof Ast.DataReference n) return n.writtenName();
        if (node instanceof Ast.OperationExpression n) return n.operator();
        if (node instanceof Ast.FunctionExpression n) return n.functionName();
        if (node instanceof Ast.SpecialRegisterExpression n) return n.registerName();
        if (node instanceof Ast.PreservedExpression n) return n.writtenText();
        if (node instanceof Ast.RawExpression n) return n.rawText();
        return node.getClass().getSimpleName();
    }

    private static Map<String, String> attributes(Ast.Node node) {
        Map<String, String> result = new LinkedHashMap<>();
        if (node instanceof Ast.Program n) result.put("programName", n.name());
        else if (node instanceof Ast.Division n) result.put("divisionKind", n.divisionKind().name());
        else if (node instanceof Ast.FileBinding n) {
            result.put("logicalName", n.logicalName()); result.put("assignment", n.assignment());
        } else if (node instanceof Ast.FileDescription n) result.put("fileName", n.fileName());
        else if (node instanceof Ast.DataEntry n) {
            result.put("level", n.level()); result.put("name", n.name()); result.put("declaration", n.declaration());
        } else if (node instanceof Ast.DataQualifier n) {
            result.put("connector", n.connector().name()); result.put("name", n.name());
            result.put("writtenText", n.writtenText());
        } else if (node instanceof Ast.SubscriptGroup n) result.put("writtenText", n.writtenText());
        else if (node instanceof Ast.ReferenceModification n) result.put("writtenText", n.writtenText());
        else if (node instanceof Ast.Paragraph n) result.put("name", n.name());
        else if (node instanceof Ast.Sentence n) {
            result.put("terminator", n.terminator().name());
            result.put("terminatorLine", String.valueOf(n.terminatorSpan().startLine()));
        } else if (node instanceof Ast.CallStatement n) result.put("targetKind", n.targetKind().name());
        else if (node instanceof Ast.CallArgument n) result.put("passingMode", n.passingMode().name());
        else if (node instanceof Ast.IfStatement n) result.put("explicitlyTerminated", String.valueOf(n.explicitlyTerminated()));
        else if (node instanceof Ast.EvaluateStatement n) result.put("explicitlyTerminated", String.valueOf(n.explicitlyTerminated()));
        else if (node instanceof Ast.EvaluateBranch n) {
            result.put("selector", n.selector()); result.put("other", String.valueOf(n.other()));
        } else if (node instanceof Ast.PerformStatement n) {
            result.put("performKind", n.performKind().name()); result.put("from", n.fromProcedure());
            result.put("through", n.throughProcedure()); result.put("control", n.control());
        } else if (node instanceof Ast.GoToStatement n) {
            result.put("goToKind", n.goToKind().name()); result.put("targets", String.join(", ", n.targets()));
        } else if (node instanceof Ast.MoveStatement n) result.put("corresponding", String.valueOf(n.corresponding()));
        else if (node instanceof Ast.EmbeddedLanguageStatement n) {
            result.put("language", n.language().name()); result.put("rawText", compact(n.rawText()));
            result.put("parsedContent", "deferred to plugin");
        } else if (node instanceof Ast.UnsupportedStatement n) {
            result.put("grammarRule", n.grammarRule()); result.put("rawText", n.rawText());
        } else if (node instanceof Ast.LiteralExpression n) {
            result.put("value", n.value()); result.put("rawLexeme", n.rawLexeme());
        } else if (node instanceof Ast.DataReference n) {
            result.put("baseName", n.baseName()); result.put("writtenText", n.writtenText());
            result.put("understanding", n.understanding().name());
        } else if (node instanceof Ast.OperationExpression n) {
            result.put("operator", n.operator()); result.put("writtenText", n.writtenText());
        } else if (node instanceof Ast.FunctionExpression n) {
            result.put("functionName", n.functionName()); result.put("writtenText", n.writtenText());
        } else if (node instanceof Ast.SpecialRegisterExpression n) {
            result.put("registerName", n.registerName()); result.put("writtenText", n.writtenText());
        } else if (node instanceof Ast.PreservedExpression n) {
            result.put("grammarRule", n.grammarRule()); result.put("writtenText", n.writtenText());
            result.put("understanding", n.understanding().name());
        } else if (node instanceof Ast.RawExpression n) {
            result.put("role", n.role()); result.put("rawText", n.rawText());
        }
        return result;
    }

    private static String expressionLabel(Ast.Expression expression) {
        if (expression instanceof Ast.LiteralExpression n) return n.rawLexeme();
        if (expression instanceof Ast.DataReference n) return n.writtenName();
        if (expression instanceof Ast.OperationExpression n) return n.writtenText();
        if (expression instanceof Ast.FunctionExpression n) return n.writtenText();
        if (expression instanceof Ast.SpecialRegisterExpression n) return n.writtenText();
        if (expression instanceof Ast.PreservedExpression n) return n.writtenText();
        if (expression instanceof Ast.RawExpression n) return n.rawText();
        return "target";
    }

    void write(Path path, String sourceName, int parseTreeNodes, List<String> sourceLines) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            out.write("window.AST_DATA={\n\"meta\":{");
            field(out, "source", sourceName); out.write(',');
            out.write("\"nodes\":" + metrics.nodes + ",\"parseTreeNodes\":" + parseTreeNodes + ',');
            out.write("\"maxDepth\":" + metrics.maxDepth + ",\"staticCalls\":" + metrics.staticCalls + ',');
            out.write("\"dynamicCalls\":" + metrics.dynamicCalls + ",\"embeddedLanguages\":" + metrics.embeddedLanguages + ',');
            out.write("\"unsupportedStatements\":" + metrics.unsupportedStatements + "},\n");
            out.write("\"sourceLines\":[");
            for (int i = 0; i < sourceLines.size(); i++) { if (i > 0) out.write(','); string(out, sourceLines.get(i)); }
            out.write("],\n\"nodes\":[");
            for (int i = 0; i < nodes.size(); i++) {
                if (i > 0) out.write(',');
                Node n = nodes.get(i);
                out.write("{\"id\":" + n.id + ",\"p\":" + n.parent + ",\"t\":"); string(out, n.type);
                out.write(",\"k\":"); string(out, n.category); out.write(",\"n\":"); string(out, n.label);
                out.write(",\"l\":" + n.line + ",\"c\":" + n.column + ",\"e\":" + n.stopLine);
                out.write(",\"d\":" + n.depth + ",\"q\":" + n.childCount + ",\"r\":" + n.parseTreeId);
                out.write(",\"g\":"); string(out, n.grammarRule); out.write(",\"z\":" + n.parseTreeNodes);
                out.write(",\"sf\":"); string(out, n.sourceFile); out.write(",\"sl\":" + n.sourceLine);
                out.write(",\"si\":" + n.includeDepth + ",\"sx\":" + n.sourceExact);
                out.write(",\"a\":{");
                boolean first = true;
                for (var entry : n.attributes.entrySet()) {
                    if (!first) out.write(','); first = false; string(out, entry.getKey()); out.write(':'); string(out, entry.getValue());
                }
                out.write("}}");
            }
            out.write("],\n\"typeCounts\":{");
            boolean first = true;
            for (var entry : typeCounts.entrySet()) {
                if (!first) out.write(','); first = false; string(out, entry.getKey()); out.write(':' + String.valueOf(entry.getValue()));
            }
            out.write("},\n\"parseToAst\":{");
            first = true;
            for (var entry : parseToAst.entrySet()) {
                if (!first) out.write(','); first = false; string(out, String.valueOf(entry.getKey())); out.write(':');
                out.write('[' + entry.getValue().stream().map(String::valueOf).reduce((a,b) -> a + ',' + b).orElse("") + ']');
            }
            out.write("}};\n");
        }
    }

    private static String compact(String text) { return text == null ? "" : text.replaceAll("\\s+", " ").trim(); }
    private static void field(Writer out, String name, String value) throws IOException { string(out, name); out.write(':'); string(out, value); }
    private static void string(Writer out, String value) throws IOException {
        out.write('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.write("\\\""); case '\\' -> out.write("\\\\");
                case '\b' -> out.write("\\b"); case '\f' -> out.write("\\f");
                case '\n' -> out.write("\\n"); case '\r' -> out.write("\\r"); case '\t' -> out.write("\\t");
                default -> { if (ch < 0x20 || ch == '\u2028' || ch == '\u2029') out.write(String.format("\\u%04x", (int)ch)); else out.write(ch); }
            }
        }
        out.write('"');
    }
}
