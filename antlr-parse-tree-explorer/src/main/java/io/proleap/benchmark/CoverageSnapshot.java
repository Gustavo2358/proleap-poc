package io.proleap.benchmark;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Deterministic, browser-facing semantic coverage report. */
final class CoverageSnapshot {
    private record Gap(Ast.Meta meta, String grammarRule, String writtenText, String reason) {}
    record Metrics(int dataReferences, int qualifiedDataReferences, int subscriptedDataReferences,
                   int modifiedDataReferences, int preservedDataReferences,
                   int procedureReferences, int fileReferences,
                   int programReferences, int modeledStatements, int preservedStatements,
                   int typedDataClauses, int preservedDataClauses, int opaqueExpressions,
                   int embeddedLanguages) {}

    private final String source;
    private final SemanticCoverage.Report report;
    private final Metrics metrics;
    private final List<Gap> structuralGaps;
    private final int unresolvedCopies;
    private final int lexerErrors;
    private final int parserErrors;

    private CoverageSnapshot(String source, SemanticCoverage.Report report, Metrics metrics, List<Gap> structuralGaps,
                             int unresolvedCopies, int lexerErrors, int parserErrors) {
        this.source = source;
        this.report = report;
        this.metrics = metrics;
        this.structuralGaps = List.copyOf(structuralGaps);
        this.unresolvedCopies = unresolvedCopies;
        this.lexerErrors = lexerErrors;
        this.parserErrors = parserErrors;
    }

    static CoverageSnapshot from(String source, Ast.Program program, SemanticCoverage.Report report,
                                 int unresolvedCopies, int lexerErrors, int parserErrors) {
        List<Ast.Node> nodes = flatten(program);
        List<Ast.DataReference> dataReferences = nodes.stream().filter(Ast.DataReference.class::isInstance)
                .map(Ast.DataReference.class::cast).toList();
        Metrics metrics = new Metrics(dataReferences.size(),
                (int) dataReferences.stream().filter(r -> !r.qualifiers().isEmpty()).count(),
                (int) dataReferences.stream().filter(r -> !r.subscriptGroups().isEmpty()).count(),
                (int) dataReferences.stream().filter(r -> r.referenceModification() != null).count(),
                (int) dataReferences.stream().filter(r -> r.understanding() == Ast.ReferenceUnderstanding.PRESERVED).count(),
                count(nodes, Ast.ProcedureReference.class), count(nodes, Ast.FileReference.class),
                count(nodes, Ast.ProgramReference.class), count(nodes, Ast.ModeledStatement.class),
                count(nodes, Ast.PreservedStatement.class),
                (int) nodes.stream().filter(Ast.DataClause.class::isInstance)
                        .filter(node -> !(node instanceof Ast.PreservedDataClause)).count(),
                count(nodes, Ast.PreservedDataClause.class), count(nodes, Ast.PreservedExpression.class),
                count(nodes, Ast.EmbeddedLanguageStatement.class));
        List<Gap> gaps = nodes.stream().map(CoverageSnapshot::structuralGap)
                .filter(Objects::nonNull).toList();
        return new CoverageSnapshot(source, report, metrics, gaps, unresolvedCopies, lexerErrors, parserErrors);
    }

    boolean dependencyCoverageComplete() {
        return report.dependencyCoverageComplete() && structuralGaps.isEmpty() && unresolvedCopies == 0
                && lexerErrors == 0 && parserErrors == 0;
    }

    Metrics metrics() { return metrics; }

    void write(Path path) throws IOException {
        try (Writer out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            out.write("window.SEMANTIC_COVERAGE_DATA={\n\"meta\":{");
            field(out, "source", source); out.write(",\"complete\":" + dependencyCoverageComplete());
            out.write(",\"unresolvedCopies\":" + unresolvedCopies + ",\"lexerErrors\":" + lexerErrors
                    + ",\"parserErrors\":" + parserErrors + "},\n\"constructionCounts\":");
            enumCounts(out, constructionCounts());
            out.write(",\n\"dependencyCounts\":"); enumCounts(out, dependencyCounts());
            out.write(",\n\"metrics\":{");
            out.write("\"dataReferences\":" + metrics.dataReferences
                    + ",\"qualifiedDataReferences\":" + metrics.qualifiedDataReferences
                    + ",\"subscriptedDataReferences\":" + metrics.subscriptedDataReferences
                    + ",\"modifiedDataReferences\":" + metrics.modifiedDataReferences
                    + ",\"preservedDataReferences\":" + metrics.preservedDataReferences
                    + ",\"procedureReferences\":" + metrics.procedureReferences
                    + ",\"fileReferences\":" + metrics.fileReferences
                    + ",\"programReferences\":" + metrics.programReferences
                    + ",\"modeledStatements\":" + metrics.modeledStatements
                    + ",\"preservedStatements\":" + metrics.preservedStatements
                    + ",\"typedDataClauses\":" + metrics.typedDataClauses
                    + ",\"preservedDataClauses\":" + metrics.preservedDataClauses
                    + ",\"opaqueExpressions\":" + metrics.opaqueExpressions
                    + ",\"embeddedLanguages\":" + metrics.embeddedLanguages + "},\n\"findings\":[");
            for (int i = 0; i < report.findings().size(); i++) {
                if (i > 0) out.write(',');
                SemanticCoverage.Finding finding = report.findings().get(i);
                Ast.SourceProvenance provenance = finding.meta().provenance();
                out.write("{\"id\":" + finding.id() + ",\"ast\":" + finding.astNodeId()
                        + ",\"parse\":" + finding.meta().origin().rootNodeId() + ",\"line\":"
                        + finding.meta().span().startLine() + ",\"sourceLine\":"
                        + provenance.original().startLine() + ",\"rule\":"); string(out, finding.grammarRule());
                out.write(",\"coverage\":"); string(out, finding.coverage().name());
                out.write(",\"dependency\":"); string(out, finding.dependencyKnowledge().name());
                out.write(",\"sourceFile\":"); string(out, provenance.original().file());
                out.write(",\"text\":"); string(out, finding.writtenText());
                out.write(",\"reason\":"); string(out, finding.reason()); out.write('}');
            }
            for (int i = 0; i < structuralGaps.size(); i++) {
                if (!report.findings().isEmpty() || i > 0) out.write(',');
                Gap gap = structuralGaps.get(i);
                Ast.SourceProvenance provenance = gap.meta().provenance();
                out.write("{\"id\":" + (report.findings().size() + i) + ",\"ast\":" + gap.meta().id()
                        + ",\"parse\":" + gap.meta().origin().rootNodeId() + ",\"line\":"
                        + gap.meta().span().startLine() + ",\"sourceLine\":"
                        + provenance.original().startLine() + ",\"rule\":"); string(out, gap.grammarRule());
                out.write(",\"coverage\":\"PRESERVED_UNINTERPRETED\",\"dependency\":\"DEPENDENCY_UNKNOWN\",\"sourceFile\":");
                string(out, provenance.original().file()); out.write(",\"text\":"); string(out, gap.writtenText());
                out.write(",\"reason\":"); string(out, gap.reason()); out.write('}');
            }
            out.write("],\n\"blockingReasons\":[");
            List<String> reasons = blockingReasons();
            for (int i = 0; i < reasons.size(); i++) { if (i > 0) out.write(','); string(out, reasons.get(i)); }
            out.write("]};\n");
        }
    }

    private List<String> blockingReasons() {
        List<String> result = new ArrayList<>();
        if (unresolvedCopies > 0) result.add(unresolvedCopies + " COPY(s) ausente(s)");
        if (lexerErrors > 0) result.add(lexerErrors + " erro(s) léxico(s)");
        if (parserErrors > 0) result.add(parserErrors + " erro(s) sintático(s)");
        long unknown = report.findings().stream().filter(f ->
                f.dependencyKnowledge() == SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN).count();
        unknown += structuralGaps.size();
        if (unknown > 0) result.add(unknown + " construção(ões) com dependência desconhecida");
        return List.copyOf(result);
    }

    private static List<Ast.Node> flatten(Ast.Node root) {
        List<Ast.Node> result = new ArrayList<>(); result.add(root);
        for (Ast.Node child : Ast.children(root)) result.addAll(flatten(child));
        return result;
    }
    private static int count(List<Ast.Node> nodes, Class<?> type) {
        return (int) nodes.stream().filter(type::isInstance).count();
    }
    private Map<SemanticCoverage.ConstructionCoverage, Long> constructionCounts() {
        EnumMap<SemanticCoverage.ConstructionCoverage, Long> result =
                new EnumMap<>(SemanticCoverage.ConstructionCoverage.class);
        result.putAll(report.constructionCounts());
        result.merge(SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED,
                (long) structuralGaps.size(), Long::sum);
        return result;
    }
    private Map<SemanticCoverage.DependencyKnowledge, Long> dependencyCounts() {
        EnumMap<SemanticCoverage.DependencyKnowledge, Long> result =
                new EnumMap<>(SemanticCoverage.DependencyKnowledge.class);
        result.putAll(report.dependencyCounts());
        result.merge(SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN,
                (long) structuralGaps.size(), Long::sum);
        return result;
    }
    private static Gap structuralGap(Ast.Node node) {
        if (node instanceof Ast.PreservedDataClause n)
            return new Gap(n.meta(), n.grammarRule(), n.writtenText(), "Data clause preserved but not interpreted");
        if (node instanceof Ast.PreservedExpression n)
            return new Gap(n.meta(), n.grammarRule(), n.writtenText(), "Expression preserved but not interpreted");
        if (node instanceof Ast.RawExpression n)
            return new Gap(n.meta(), n.role(), n.rawText(), "Expression input is structurally unavailable");
        if (node instanceof Ast.UnsupportedStatement n)
            return new Gap(n.meta(), n.grammarRule(), n.rawText(), "Legacy unsupported statement");
        return null;
    }
    private static void enumCounts(Writer out, Map<? extends Enum<?>, Long> counts) throws IOException {
        out.write('{'); boolean first = true;
        for (var entry : counts.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name))).toList()) {
            if (!first) out.write(','); first = false; string(out, entry.getKey().name());
            out.write(':' + String.valueOf(entry.getValue()));
        }
        out.write('}');
    }
    private static void field(Writer out, String name, String value) throws IOException {
        string(out, name); out.write(':'); string(out, value);
    }
    private static void string(Writer out, String value) throws IOException {
        out.write('"');
        for (char ch : String.valueOf(value).toCharArray()) switch (ch) {
            case '"' -> out.write("\\\""); case '\\' -> out.write("\\\\");
            case '\n' -> out.write("\\n"); case '\r' -> out.write("\\r"); case '\t' -> out.write("\\t");
            default -> { if (ch < 0x20) out.write(String.format("\\u%04x", (int) ch)); else out.write(ch); }
        }
        out.write('"');
    }
}
