package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExternalClassificationProjectionTest {
    private static final Path POSSIBLE = Path.of(
            "src/test/resources/cobol/semantic/external-cics-possible.cbl");

    @Test
    void replacesOnlyCoveredArtificialBindingGapsWithExternalInferredFacts() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(POSSIBLE);
        ExternalClassification classifications = classify(analysis);

        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(),
                analysis.occurrences(), analysis.resolution(), classifications);

        assertSame(classifications, report.externalClassifications());
        assertEquals(4, analysis.resolution().diagnostics().size(),
                "nominal diagnostics remain intact");
        assertEquals(4L, report.statusCounts().get(ResolutionContracts.ResolutionStatus.UNRESOLVED));
        assertEquals(2, report.gaps().size(), "one external fact replaces each two-entry subtree");
        assertTrue(report.gaps().stream().allMatch(gap ->
                gap.category() == ResolutionAnalysisReport.GapCategory.EXTERNAL_CLASSIFICATION
                        && gap.code().equals("EXTERNAL_INFERRED_CICS_POSSIBLE_INTRINSIC")));
        assertEquals(classifications.entries().stream().map(ExternalClassification.Entry::rootOccurrenceId)
                        .collect(java.util.stream.Collectors.toSet()),
                report.gaps().stream().map(ResolutionAnalysisReport.Gap::occurrenceId)
                        .collect(java.util.stream.Collectors.toSet()));
        assertFalse(report.completeness().referenceBindingComplete());
        assertFalse(report.completeness().dependencyAnalysisReady());
        assertEquals(ResolutionAnalysisReport.AnalysisClaim.INCOMPLETE, report.analysisClaim());
    }

    @Test
    void emptyClassificationProductPreservesTheExistingReportProjection() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(POSSIBLE);
        ResolutionAnalysisReport legacy = ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(),
                analysis.occurrences(), analysis.resolution());
        ResolutionAnalysisReport explicitEmpty = ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(),
                analysis.occurrences(), analysis.resolution(), ExternalClassification.empty());

        assertAll("pre-existing report fields remain semantically identical",
                () -> assertEquals(legacy.completeness(), explicitEmpty.completeness()),
                () -> assertEquals(legacy.analysisClaim(), explicitEmpty.analysisClaim()),
                () -> assertEquals(legacy.gaps(), explicitEmpty.gaps()),
                () -> assertEquals(legacy.programUnits(), explicitEmpty.programUnits()),
                () -> assertEquals(legacy.statusCounts(), explicitEmpty.statusCounts()),
                () -> assertEquals(legacy.reasonCounts(), explicitEmpty.reasonCounts()),
                () -> assertEquals(legacy.operationalMetrics(), explicitEmpty.operationalMetrics()),
                () -> assertTrue(explicitEmpty.externalClassifications().entries().isEmpty()));
    }

    @Test
    void uncoveredBindingGapsRemainVisibleBesideTheExternalConstruct() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis = ExternalClassificationTestSupport.analyze(
                fixed("""
                        IDENTIFICATION DIVISION.
                        PROGRAM-ID. UNCOVEREDGAP.
                        DATA DIVISION.
                        WORKING-STORAGE SECTION.
                        01 WS-RESP PIC S9(8) COMP.
                        PROCEDURE DIVISION.
                            IF WS-RESP = DFHRESP(NORMAL)
                                CONTINUE
                            END-IF.
                            MOVE OTHER-MISSING TO WS-RESP.
                            GOBACK.
                        END PROGRAM UNCOVEREDGAP.
                        """), "uncovered-gap.cbl");
        ExternalClassification classifications = classify(analysis);

        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(),
                analysis.occurrences(), analysis.resolution(), classifications);

        assertEquals(2, report.gaps().size());
        assertTrue(report.gaps().stream().anyMatch(gap ->
                gap.category() == ResolutionAnalysisReport.GapCategory.EXTERNAL_CLASSIFICATION));
        assertTrue(report.gaps().stream().anyMatch(gap ->
                gap.category() == ResolutionAnalysisReport.GapCategory.REFERENCE_BINDING
                        && gap.message().contains("OTHER-MISSING")));
    }

    @Test
    void inconsistentClassificationFailsClosedWithoutSuppressingNominalGaps() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(POSSIBLE);
        ExternalClassification.Entry valid = classify(analysis).entries().get(0);
        ReferenceResolution.Entry argument = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().programUnitId().equals(valid.programUnitId())
                        && valid.coveredOccurrenceIds().contains(entry.occurrence().id())
                        && entry.occurrence().id() != valid.rootOccurrenceId())
                .findFirst().orElseThrow();
        ExternalClassification.Entry inconsistentEntry = new ExternalClassification.Entry(
                0, valid.programUnitId(), argument.occurrence().referenceAstNodeId(),
                valid.rootOccurrenceId(), valid.constructWrittenText(), valid.technology(), valid.kind(),
                valid.certainty(), valid.reason(), argument.occurrence().meta(), valid.coveredOccurrenceIds());

        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(),
                analysis.occurrences(), analysis.resolution(),
                new ExternalClassification(List.of(inconsistentEntry)));

        assertTrue(report.externalClassifications().entries().isEmpty());
        assertEquals(5, report.gaps().size(), "four original binding gaps plus integrity diagnostic");
        assertTrue(report.gaps().stream().anyMatch(gap ->
                gap.code().equals("INCONSISTENT_EXTERNAL_CLASSIFICATION")));
        assertEquals(4, report.gaps().stream().filter(gap ->
                gap.category() == ResolutionAnalysisReport.GapCategory.REFERENCE_BINDING).count());
    }

    @Test
    void classificationWithWrongProvenanceOrWrittenTextFailsClosed() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(POSSIBLE);
        ExternalClassification.Entry valid = classify(analysis).entries().get(0);
        Ast.SourceProvenance provenance = valid.meta().provenance();
        Ast.Meta wrongMeta = new Ast.Meta(valid.rootAstNodeId(), valid.meta().span(),
                valid.meta().origin(), new Ast.SourceProvenance(
                provenance.expanded(), provenance.original(), provenance.includeChain(),
                !provenance.exact()));
        ExternalClassification.Entry wrongProvenance = copyClassification(
                valid, valid.constructWrittenText(), wrongMeta);
        ExternalClassification.Entry wrongWrittenText = copyClassification(
                valid, valid.constructWrittenText() + "-WRONG", valid.meta());

        assertInconsistentClassificationFailsClosed(analysis, wrongProvenance);
        assertInconsistentClassificationFailsClosed(analysis, wrongWrittenText);
    }

    @Test
    void snapshotKeepsNominalEntriesAndPublishesTraceableExternalClassifications() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(POSSIBLE);
        ExternalClassification classifications = classify(analysis);
        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(),
                analysis.occurrences(), analysis.resolution(), classifications);
        ResolutionSnapshot snapshot = ResolutionSnapshot.from(POSSIBLE.getFileName().toString(),
                Arrays.asList(analysis.source().split("\\R", -1)), analysis.model(),
                analysis.resolution(), report);
        Path output = Files.createTempFile("external-classification", ".js");

        snapshot.write(output);
        String text = Files.readString(output, StandardCharsets.UTF_8);

        assertAll("orthogonal snapshot collections",
                () -> assertTrue(text.contains("\"externalClassifications\":2")),
                () -> assertTrue(text.contains("\"classifications\":[")),
                () -> assertTrue(text.contains("\"technology\":\"CICS\"")),
                () -> assertTrue(text.contains("\"kind\":\"POSSIBLE_INTRINSIC\"")),
                () -> assertTrue(text.contains("\"certainty\":\"INFERRED\"")),
                () -> assertTrue(text.contains("\"reason\":\"COBOL_REFERENCE_UNRESOLVED_WITH_KNOWN_CICS_SHAPE\"")),
                () -> assertTrue(text.contains("\"coveredOccurrenceIds\":[")),
                () -> assertTrue(text.contains("\"includeChain\":[]")),
                () -> assertTrue(text.contains("\"status\":\"UNRESOLVED\"")),
                () -> assertEquals(4, occurrences(text, "\"status\":\"UNRESOLVED\"")),
                () -> assertEquals(2, occurrences(text, "\"category\":\"EXTERNAL_CLASSIFICATION\"")));
    }

    @Test
    void browserPresentationUsesPublishedClassificationFieldsWithoutRecomputingSemantics() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/web/resolution-app.js"));

        for (String field : List.of("classifications", "technology", "kind", "certainty", "reason",
                "coveredOccurrenceIds")) assertTrue(script.contains(field), field);
        assertFalse(script.contains("startsWith(\"DFH\")"));
        assertFalse(script.contains("DFHRESP"));
        assertFalse(script.contains("DFHVALUE"));
    }

    @Test
    void explorerCompositionRunsTheClassifierOnlyAfterNominalResolution() throws Exception {
        Path output = Files.createTempDirectory("external-classification-explorer");
        Path copybooks = Files.createTempDirectory("external-classification-copybooks");

        ExplorerMain.main(new String[]{"--source", POSSIBLE.toAbsolutePath().toString(),
                "--copybooks", copybooks.toString(), "--output", output.toString()});
        String text = Files.readString(output.resolve("resolution-data.js"), StandardCharsets.UTF_8);

        assertAll("composition root publishes binding and post-resolution classification",
                () -> assertTrue(text.contains("\"externalClassifications\":2")),
                () -> assertEquals(2, occurrences(text, "\"category\":\"EXTERNAL_CLASSIFICATION\"")),
                () -> assertEquals(4, occurrences(text, "\"status\":\"UNRESOLVED\"")));
    }

    @Test
    void parserRecoveryLeavesExternalClassificationDisabledAndItsInputGapVisible() throws Exception {
        Path source = Files.createTempFile("external-classification-parser-error", ".cbl");
        Files.writeString(source, String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. PARSERERROR.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-RESP PIC S9(8) COMP.",
                "       PROCEDURE DIVISION.",
                "           IF WS-RESP = DFHRESP(IDX)(OTHER)",
                "               CONTINUE",
                "           END-IF.",
                "           GOBACK.",
                "       END PROGRAM PARSERERROR.", ""), StandardCharsets.UTF_8);
        Path output = Files.createTempDirectory("external-classification-parser-output");
        Path copybooks = Files.createTempDirectory("external-classification-parser-copybooks");

        ExplorerMain.main(new String[]{"--source", source.toString(),
                "--copybooks", copybooks.toString(), "--output", output.toString()});
        String text = Files.readString(output.resolve("resolution-data.js"), StandardCharsets.UTF_8);

        assertAll("parser recovery cannot be promoted into an external hypothesis",
                () -> assertTrue(text.contains("\"externalClassifications\":0")),
                () -> assertTrue(text.contains("\"code\":\"PARSER_ERROR\"")),
                () -> assertFalse(text.contains("\"category\":\"EXTERNAL_CLASSIFICATION\"")));
    }

    private static ExternalClassification classify(ExternalClassificationTestSupport.Analysis analysis) {
        return new CicsIntrinsicClassifier().classify(
                analysis.model(), analysis.occurrences(), analysis.resolution());
    }

    private static ExternalClassification.Entry copyClassification(
            ExternalClassification.Entry source, String writtenText, Ast.Meta meta) {
        return new ExternalClassification.Entry(0, source.programUnitId(), source.rootAstNodeId(),
                source.rootOccurrenceId(), writtenText, source.technology(), source.kind(),
                source.certainty(), source.reason(), meta, source.coveredOccurrenceIds());
    }

    private static void assertInconsistentClassificationFailsClosed(
            ExternalClassificationTestSupport.Analysis analysis,
            ExternalClassification.Entry inconsistent) {
        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(),
                analysis.occurrences(), analysis.resolution(),
                new ExternalClassification(List.of(inconsistent)));

        assertAll("inconsistent classification integrity",
                () -> assertTrue(report.externalClassifications().entries().isEmpty()),
                () -> assertEquals(5, report.gaps().size()),
                () -> assertTrue(report.gaps().stream().anyMatch(gap ->
                        gap.code().equals("INCONSISTENT_EXTERNAL_CLASSIFICATION"))),
                () -> assertEquals(4, report.gaps().stream().filter(gap ->
                        gap.category() == ResolutionAnalysisReport.GapCategory.REFERENCE_BINDING).count()));
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int from = 0; (from = value.indexOf(needle, from)) >= 0; from += needle.length()) count++;
        return count;
    }

    private static String fixed(String freeFormat) {
        return freeFormat.lines().map(line -> line.isBlank() ? "" : "       " + line)
                .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
    }
}
