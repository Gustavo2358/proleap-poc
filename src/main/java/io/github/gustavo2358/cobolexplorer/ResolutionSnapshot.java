package io.github.gustavo2358.cobolexplorer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Deterministic browser projection of name binding; it performs no analysis. */
final class ResolutionSnapshot {
    private final String sourceName;
    private final List<String> sourceLines;
    private final CompilationUnitModel model;
    private final ReferenceResolution resolution;
    private final ResolutionAnalysisReport report;

    private ResolutionSnapshot(String sourceName, List<String> sourceLines,
                               CompilationUnitModel model, ReferenceResolution resolution,
                               ResolutionAnalysisReport report) {
        this.sourceName = Objects.requireNonNull(sourceName, "sourceName");
        this.sourceLines = List.copyOf(sourceLines);
        this.model = Objects.requireNonNull(model, "model");
        this.resolution = Objects.requireNonNull(resolution, "resolution");
        this.report = Objects.requireNonNull(report, "report");
    }

    static ResolutionSnapshot from(String sourceName, List<String> sourceLines,
                                   CompilationUnitModel model, ReferenceResolution resolution,
                                   ResolutionAnalysisReport report) {
        return new ResolutionSnapshot(sourceName, sourceLines, model, resolution, report);
    }

    void write(Path path) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            out.write("window.RESOLUTION_DATA={\n\"meta\":{");
            field(out, "source", sourceName); out.write(',');
            field(out, "compilationUnitId", model.compilationUnitId()); out.write(',');
            field(out, "policyId", resolution.policy().policyId()); out.write(',');
            field(out, "policyVersion", resolution.policy().version()); out.write(',');
            field(out, "qualifyMode", resolution.policy().qualifyMode().name()); out.write(',');
            field(out, "pgmnameMode", resolution.policy().pgmnameMode().name()); out.write(',');
            field(out, "dynamMode", resolution.policy().dynamMode().name()); out.write(',');
            field(out, "dllMode", resolution.policy().dllMode().name()); out.write(',');
            field(out, "claim", report.analysisClaim().name()); out.write(',');
            field(out, "inputCompleteness", report.frontendState()
                    .externalClassificationInputCompleteness().name()); out.write(',');
            out.write("\"referenceBindingComplete\":" + report.completeness().referenceBindingComplete() + ',');
            out.write("\"dependencyAnalysisReady\":" + report.completeness().dependencyAnalysisReady() + ',');
            out.write("\"programUnits\":" + model.programUnits().size() + ',');
            out.write("\"references\":" + resolution.entries().size() + ',');
            out.write("\"externalClassifications\":"
                    + report.externalClassifications().entries().size() + ',');
            out.write("\"unresolvedCopies\":" + report.frontendState().unresolvedCopies() + ',');
            out.write("\"gaps\":" + report.gaps().size() + "},\n");
            writeCompleteness(out);
            out.write(",\n");
            writeCounts(out);
            out.write(",\n");
            writeMetrics(out);
            out.write(",\n");
            writeSourceLines(out);
            out.write(",\n");
            writeUnits(out);
            out.write(",\n");
            writeEntries(out);
            out.write(",\n");
            writeClassifications(out);
            out.write(",\n");
            writeGaps(out);
            out.write(",\n");
            writeDiagnostics(out);
            out.write(",\n");
            writeRelations(out);
            out.write("};\n");
        }
    }

    private void writeCompleteness(Writer out) throws IOException {
        out.write("\"completeness\":{\"blockingReasons\":[");
        strings(out, report.completeness().blockingReasons());
        out.write("]}");
    }

    private void writeCounts(Writer out) throws IOException {
        out.write("\"counts\":{");
        enumCounts(out, "status", ResolutionContracts.ResolutionStatus.values(), report.statusCounts());
        out.write(',');
        enumCounts(out, "reason", ResolutionContracts.ResolutionReason.values(), report.reasonCounts());
        out.write(',');
        enumCounts(out, "syntacticKind", ResolutionContracts.ReferenceKind.values(),
                report.syntacticKindCounts());
        out.write(',');
        enumCounts(out, "resolvedSemanticKind", ResolutionContracts.ReferenceKind.values(),
                report.resolvedSemanticKindCounts());
        out.write(',');
        enumCounts(out, "role", ResolutionContracts.ReferenceRole.values(), report.roleCounts());
        out.write('}');
    }

    private static <E extends Enum<E>> void enumCounts(Writer out, String name, E[] values,
                                                        Map<E, Long> counts) throws IOException {
        string(out, name); out.write(":{");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) out.write(',');
            string(out, values[index].name());
            out.write(':' + String.valueOf(counts.getOrDefault(values[index], 0L)));
        }
        out.write('}');
    }

    private void writeMetrics(Writer out) throws IOException {
        ResolutionAnalysisReport.OperationalMetrics metrics = report.operationalMetrics();
        out.write("\"metrics\":{");
        out.write("\"indexedDeclarations\":" + metrics.indexedDeclarations() + ',');
        out.write("\"nominalLookups\":" + metrics.nominalLookups() + ',');
        out.write("\"candidateInspections\":" + metrics.candidateInspections() + ',');
        out.write("\"maximumCandidates\":" + metrics.maximumCandidates() + ',');
        out.write("\"collectedReferences\":" + metrics.collectedReferences() + "}");
    }

    private void writeSourceLines(Writer out) throws IOException {
        out.write("\"sourceLines\":[");
        strings(out, sourceLines);
        out.write(']');
    }

    private void writeUnits(Writer out) throws IOException {
        Map<ResolutionContracts.ProgramUnitId, ResolutionAnalysisReport.ProgramUnitSummary> summaries =
                new LinkedHashMap<>();
        for (ResolutionAnalysisReport.ProgramUnitSummary summary : report.programUnits())
            summaries.put(summary.programUnitId(), summary);
        out.write("\"units\":[");
        for (int index = 0; index < model.programUnits().size(); index++) {
            if (index > 0) out.write(',');
            CompilationUnitModel.ProgramUnit unit = model.programUnits().get(index);
            ResolutionAnalysisReport.ProgramUnitSummary summary = summaries.get(unit.id());
            out.write('{');
            field(out, "id", unitKey(unit.id())); out.write(',');
            field(out, "path", path(unit.id())); out.write(',');
            field(out, "name", unit.program().name()); out.write(',');
            field(out, "canonicalName", unit.id().canonicalProgramName()); out.write(',');
            nullableField(out, "parentId", unit.parentId() == null ? null : unitKey(unit.parentId()));
            if (summary != null) {
                out.write(",\"references\":" + summary.references());
                out.write(",\"resolved\":" + summary.resolved());
                out.write(",\"externalObserved\":" + summary.externalObserved());
                out.write(",\"ambiguous\":" + summary.ambiguous());
                out.write(",\"unresolved\":" + summary.unresolved());
                out.write(",\"unsupported\":" + summary.unsupported());
                out.write(",\"gaps\":" + summary.gaps());
                out.write(",\"complete\":" + summary.referenceBindingComplete());
            }
            out.write('}');
        }
        out.write(']');
    }

    private void writeEntries(Writer out) throws IOException {
        out.write("\"entries\":[");
        for (int index = 0; index < resolution.entries().size(); index++) {
            if (index > 0) out.write(',');
            ReferenceResolution.Entry entry = resolution.entries().get(index);
            ReferenceOccurrences.Occurrence occurrence = entry.occurrence();
            Ast.Meta meta = occurrence.meta();
            out.write("{\"id\":" + entry.id() + ',');
            field(out, "unitId", unitKey(occurrence.programUnitId())); out.write(',');
            out.write("\"occurrenceId\":" + occurrence.id() + ',');
            out.write("\"astNodeId\":" + occurrence.referenceAstNodeId() + ',');
            out.write("\"parseNodeId\":" + meta.origin().rootNodeId() + ',');
            out.write("\"scopeId\":" + occurrence.scopeId() + ',');
            field(out, "kind", occurrence.kind().name()); out.write(',');
            out.write("\"admissibleKinds\":[");
            strings(out, occurrence.admissibleKinds().stream().map(Enum::name).sorted().toList());
            out.write("],");
            field(out, "role", occurrence.role().name()); out.write(',');
            out.write("\"callSemantics\":");
            if (entry.callSemantics().isPresent()) {
                ReferenceResolution.CallSemantics semantics = entry.callSemantics().orElseThrow();
                out.write('{'); field(out, "targetSyntax", semantics.targetSyntax().name()); out.write(',');
                field(out, "linkage", semantics.linkage().name()); out.write('}');
            } else out.write("null");
            out.write(',');
            field(out, "status", entry.status().name()); out.write(',');
            field(out, "reason", entry.reason().name()); out.write(',');
            field(out, "grammarRule", occurrence.grammarRule()); out.write(',');
            field(out, "writtenText", occurrence.writtenText()); out.write(',');
            field(out, "preservation", occurrence.preservation().name()); out.write(',');
            writeSpan(out, meta.span()); out.write(',');
            writeProvenance(out, meta.provenance()); out.write(',');
            out.write("\"diagnosticIds\":[");
            integers(out, entry.diagnosticIds()); out.write("],");
            out.write("\"candidates\":[");
            for (int candidateIndex = 0; candidateIndex < entry.candidates().size(); candidateIndex++) {
                if (candidateIndex > 0) out.write(',');
                writeCandidate(out, entry.candidates().get(candidateIndex));
            }
            out.write("]}");
        }
        out.write(']');
    }

    private static void writeSpan(Writer out, Ast.SourceSpan span) throws IOException {
        out.write("\"span\":{\"startLine\":" + span.startLine()
                + ",\"startColumn\":" + span.startColumn()
                + ",\"endLine\":" + span.endLine()
                + ",\"endColumn\":" + span.endColumn() + '}');
    }

    private static void writeProvenance(Writer out, Ast.SourceProvenance provenance) throws IOException {
        out.write("\"provenance\":{");
        out.write("\"exact\":" + provenance.exact() + ',');
        writeLocation(out, "expanded", provenance.expanded()); out.write(',');
        writeLocation(out, "original", provenance.original());
        out.write(",\"includeChain\":[");
        for (int index = 0; index < provenance.includeChain().size(); index++) {
            if (index > 0) out.write(',');
            Ast.CopyFrame frame = provenance.includeChain().get(index);
            out.write('{'); field(out, "includingFile", frame.includingFile()); out.write(',');
            field(out, "requestedName", frame.requestedName()); out.write(',');
            field(out, "includedFile", frame.includedFile());
            out.write(",\"includeLine\":" + frame.includeLine() + '}');
        }
        out.write("]}");
    }

    private static void writeLocation(Writer out, String name, Ast.SourceLocation location) throws IOException {
        string(out, name); out.write(":{");
        field(out, "file", location.file());
        out.write(",\"startLine\":" + location.startLine()
                + ",\"startColumn\":" + location.startColumn()
                + ",\"endLine\":" + location.endLine()
                + ",\"endColumn\":" + location.endColumn() + '}');
    }

    private static void writeCandidate(Writer out, ReferenceResolution.Candidate candidate) throws IOException {
        ResolutionContracts.SemanticEntityId entity = candidate.entityId();
        out.write('{');
        field(out, "unitId", unitKey(entity.programUnitId())); out.write(',');
        field(out, "domain", entity.domain().name());
        out.write(",\"localId\":" + entity.localId() + ',');
        field(out, "kind", candidate.kind().name()); out.write(',');
        field(out, "writtenName", candidate.writtenName()); out.write(',');
        field(out, "canonicalName", candidate.canonicalName());
        out.write(",\"symbolIds\":["); integers(out, candidate.declarationSymbolIds());
        out.write("],\"attributes\":{");
        boolean first = true;
        for (var attribute : new TreeMap<>(candidate.attributes()).entrySet()) {
            if (!first) out.write(','); first = false;
            field(out, attribute.getKey(), attribute.getValue());
        }
        out.write("}}");
    }

    private void writeClassifications(Writer out) throws IOException {
        out.write("\"classifications\":[");
        List<ExternalClassification.Entry> classifications =
                report.externalClassifications().entries();
        for (int index = 0; index < classifications.size(); index++) {
            if (index > 0) out.write(',');
            ExternalClassification.Entry classification = classifications.get(index);
            out.write("{\"id\":" + classification.id() + ',');
            field(out, "unitId", unitKey(classification.programUnitId())); out.write(',');
            out.write("\"rootAstNodeId\":" + classification.rootAstNodeId() + ',');
            out.write("\"rootOccurrenceId\":" + classification.rootOccurrenceId() + ',');
            field(out, "constructWrittenText", classification.constructWrittenText()); out.write(',');
            field(out, "technology", classification.technology().name()); out.write(',');
            field(out, "kind", classification.kind().name()); out.write(',');
            field(out, "certainty", classification.certainty().name()); out.write(',');
            field(out, "reason", classification.reason().name()); out.write(',');
            field(out, "inputCompleteness", classification.inputCompleteness().name()); out.write(',');
            writeSpan(out, classification.meta().span()); out.write(',');
            writeProvenance(out, classification.meta().provenance()); out.write(',');
            out.write("\"coveredOccurrenceIds\":[");
            integers(out, classification.coveredOccurrenceIds());
            out.write("]}");
        }
        out.write(']');
    }

    private void writeGaps(Writer out) throws IOException {
        out.write("\"gaps\":[");
        for (int index = 0; index < report.gaps().size(); index++) {
            if (index > 0) out.write(',');
            ResolutionAnalysisReport.Gap gap = report.gaps().get(index);
            out.write("{\"id\":" + gap.id() + ',');
            field(out, "category", gap.category().name()); out.write(',');
            field(out, "code", gap.code()); out.write(',');
            field(out, "message", gap.message()); out.write(',');
            nullableField(out, "unitId", gap.programUnitId() == null ? null : unitKey(gap.programUnitId()));
            out.write(','); field(out, "grammarRule", gap.grammarRule());
            out.write(",\"line\":" + gap.line() + ",\"occurrenceId\":" + gap.occurrenceId() + '}');
        }
        out.write(']');
    }

    private void writeDiagnostics(Writer out) throws IOException {
        out.write("\"diagnostics\":[");
        for (int index = 0; index < resolution.diagnostics().size(); index++) {
            if (index > 0) out.write(',');
            ReferenceResolution.Diagnostic diagnostic = resolution.diagnostics().get(index);
            out.write("{\"id\":" + diagnostic.id() + ',');
            field(out, "code", diagnostic.code()); out.write(',');
            field(out, "message", diagnostic.message()); out.write(',');
            field(out, "unitId", unitKey(diagnostic.programUnitId()));
            out.write(",\"occurrenceId\":" + diagnostic.occurrenceId() + '}');
        }
        out.write(']');
    }

    private void writeRelations(Writer out) throws IOException {
        out.write("\"relations\":[");
        List<DeclarationRelationResolution.Entry> relations = resolution.declarationRelations().entries();
        for (int index = 0; index < relations.size(); index++) {
            if (index > 0) out.write(',');
            DeclarationRelationResolution.Entry relation = relations.get(index);
            out.write("{\"id\":" + relation.id() + ',');
            field(out, "unitId", unitKey(relation.programUnitId())); out.write(',');
            field(out, "kind", relation.kind().name()); out.write(',');
            field(out, "status", relation.status().name()); out.write(',');
            field(out, "reason", relation.reason().name());
            out.write(",\"referenceAstNodeId\":" + relation.referenceAstNodeId()
                    + ",\"candidateCount\":" + relation.candidates().size() + '}');
        }
        out.write(']');
    }

    private static String unitKey(ResolutionContracts.ProgramUnitId id) {
        return id.compilationUnitId() + "::" + path(id) + "::" + id.canonicalProgramName();
    }

    private static String path(ResolutionContracts.ProgramUnitId id) {
        return id.structuralPath().stream().map(String::valueOf).reduce((a, b) -> a + "." + b).orElse("root");
    }

    private static void nullableField(Writer out, String name, String value) throws IOException {
        string(out, name); out.write(':');
        if (value == null) out.write("null"); else string(out, value);
    }

    private static void field(Writer out, String name, String value) throws IOException {
        string(out, name); out.write(':'); string(out, Objects.requireNonNullElse(value, ""));
    }

    private static void strings(Writer out, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) out.write(','); string(out, values.get(index));
        }
    }

    private static void integers(Writer out, List<Integer> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) out.write(','); out.write(String.valueOf(values.get(index)));
        }
    }

    private static void string(Writer out, String value) throws IOException {
        out.write('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> out.write("\\\"");
                case '\\' -> out.write("\\\\");
                case '\b' -> out.write("\\b");
                case '\f' -> out.write("\\f");
                case '\n' -> out.write("\\n");
                case '\r' -> out.write("\\r");
                case '\t' -> out.write("\\t");
                default -> {
                    if (character < 0x20 || character == '\u2028' || character == '\u2029')
                        out.write(String.format("\\u%04x", (int) character));
                    else out.write(character);
                }
            }
        }
        out.write('"');
    }
}
