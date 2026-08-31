package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CicsIntrinsicClassifierTest {
    private static final Path POSSIBLE = Path.of(
            "src/test/resources/cobol/semantic/external-cics-possible.cbl");
    private static final Path TABLES = Path.of(
            "src/test/resources/cobol/semantic/external-cics-cobol-precedence.cbl");
    private static final Path NEGATIVE_CONTROLS = Path.of(
            "src/test/resources/cobol/semantic/external-cics-negative-controls.cbl");

    @Test
    void classifiesWholeUnresolvedDfhrespAndDfhvalueConstructsAfterCobolResolution() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(POSSIBLE);
        List<String> bindingBefore = bindingSignature(analysis.resolution());

        ExternalClassification classifications = new CicsIntrinsicClassifier().classify(
                analysis.model(), analysis.occurrences(), analysis.resolution());

        assertEquals(2, classifications.entries().size());
        for (ExternalClassification.Entry classification : classifications.entries()) {
            assertAll(classification.toString(),
                    () -> assertEquals(ExternalClassification.Technology.CICS,
                            classification.technology()),
                    () -> assertEquals(ExternalClassification.Kind.POSSIBLE_INTRINSIC,
                            classification.kind()),
                    () -> assertEquals(ExternalClassification.Certainty.INFERRED,
                            classification.certainty()),
                    () -> assertEquals(
                            ExternalClassification.Reason.COBOL_REFERENCE_UNRESOLVED_WITH_KNOWN_CICS_SHAPE,
                            classification.reason()),
                    () -> assertEquals(2, classification.coveredOccurrenceIds().size()),
                    () -> assertEquals(classification.coveredOccurrenceIds().stream().sorted().toList(),
                            classification.coveredOccurrenceIds()),
                    () -> assertTrue(classification.meta().provenance().exact()),
                    () -> assertEquals(classification.rootAstNodeId(), classification.meta().id()));

            Set<Integer> coveredAstNodes = analysis.resolution().entries().stream()
                    .filter(entry -> entry.occurrence().programUnitId().equals(classification.programUnitId())
                            && classification.coveredOccurrenceIds().contains(entry.occurrence().id()))
                    .map(entry -> entry.occurrence().referenceAstNodeId())
                    .collect(java.util.stream.Collectors.toSet());
            assertTrue(coveredAstNodes.contains(classification.rootAstNodeId()));
        }
        assertEquals(bindingBefore, bindingSignature(analysis.resolution()),
                "classification must not mutate nominal binding");
        assertTrue(analysis.resolution().entries().stream()
                .filter(entry -> Set.of("DFHRESP(NORMAL)", "DFHVALUE(SOME-NAME)")
                        .contains(entry.occurrence().writtenText()))
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.UNRESOLVED));
    }

    @Test
    void preservesResolvedCobolTableCallsWithoutExternalClassification() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(TABLES);

        ExternalClassification classifications = new CicsIntrinsicClassifier().classify(
                analysis.model(), analysis.occurrences(), analysis.resolution());

        assertTrue(classifications.entries().isEmpty());
        assertTrue(analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().writtenText().startsWith("DFH"))
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED));
    }

    @Test
    void rejectsSuperficialNamesAndUnsupportedStructuralShapes() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(NEGATIVE_CONTROLS);

        ExternalClassification classifications = classify(analysis);

        assertTrue(classifications.entries().isEmpty());
        List<Ast.DataReference> references = ExternalClassificationTestSupport
                .nodes(analysis.model().programUnits().get(0).program()).stream()
                .filter(Ast.DataReference.class::isInstance)
                .map(Ast.DataReference.class::cast)
                .toList();
        assertAll("fixture kills name-only and shallow shape checks",
                () -> assertTrue(references.stream().anyMatch(reference ->
                        reference.baseName().equals("MY-TABLE"))),
                () -> assertTrue(references.stream().anyMatch(reference ->
                        reference.baseName().equals("DFHOTHER"))),
                () -> assertTrue(references.stream().anyMatch(reference ->
                        reference.baseName().equals("DFHRESP") && reference.subscriptGroups().isEmpty())),
                () -> assertTrue(references.stream().anyMatch(reference ->
                        reference.baseName().equals("DFHRESP") && !reference.qualifiers().isEmpty())),
                () -> assertTrue(references.stream().anyMatch(reference ->
                        reference.baseName().equals("DFHVALUE")
                                && reference.referenceModification() != null)));
    }

    @Test
    void resolvedRootWinsEvenWhenItsSubscriptIsUnresolved() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis = ExternalClassificationTestSupport.analyze(fixed("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. ROOTWINS.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-RESP PIC S9(8) COMP.
                01 DFHRESP OCCURS 10 TIMES PIC 9.
                PROCEDURE DIVISION.
                    IF WS-RESP = DFHRESP(MISSING-IDX)
                        CONTINUE
                    END-IF.
                    GOBACK.
                END PROGRAM ROOTWINS.
                """), "root-wins.cbl");

        assertTrue(classify(analysis).entries().isEmpty());
        assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                entry(analysis, "DFHRESP(MISSING-IDX)").status());
        assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                entry(analysis, "MISSING-IDX").status());
    }

    @Test
    void ambiguousCobolBindingIsNeverPromotedToExternalClassification() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis = ExternalClassificationTestSupport.analyze(fixed("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. AMBIGUOUSBASE.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-RESP PIC S9(8) COMP.
                01 GROUP-A.
                   05 DFHRESP OCCURS 10 TIMES PIC 9.
                01 GROUP-B.
                   05 DFHRESP OCCURS 10 TIMES PIC 9.
                01 IDX PIC 9.
                PROCEDURE DIVISION.
                    IF WS-RESP = DFHRESP(IDX)
                        CONTINUE
                    END-IF.
                    GOBACK.
                END PROGRAM AMBIGUOUSBASE.
                """), "ambiguous-base.cbl");

        assertEquals(ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                entry(analysis, "DFHRESP(IDX)").status());
        assertTrue(classify(analysis).entries().isEmpty());
    }

    @Test
    void unsupportedCobolBindingIsNeverPromotedToExternalClassification() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(POSSIBLE);
        ReferenceResolution unsupported = withRootStatus(analysis.resolution(), "DFHRESP(NORMAL)",
                ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                ResolutionContracts.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM);

        ExternalClassification classifications = new CicsIntrinsicClassifier().classify(
                analysis.model(), analysis.occurrences(), unsupported);

        assertEquals(1, classifications.entries().size());
        assertEquals("DFHVALUE(SOME-NAME)",
                classifications.entries().get(0).constructWrittenText());
    }

    @Test
    void declarationAvailabilityCaseArgumentAndUnrelatedDeclarationsObeyMetamorphicRelations()
            throws Exception {
        ExternalClassificationTestSupport.Analysis baseline = analyzeMetamorphic("DFHRESP", "IDX", "", "");
        ExternalClassificationTestSupport.Analysis caseChanged = analyzeMetamorphic("dfhresp", "idx", "", "");
        ExternalClassificationTestSupport.Analysis argumentChanged = analyzeMetamorphic(
                "DFHRESP", "OTHER-IDX", "", "");
        ExternalClassificationTestSupport.Analysis unrelatedDeclaration = analyzeMetamorphic(
                "DFHRESP", "IDX", "", "01 UNRELATED PIC X.");
        ExternalClassificationTestSupport.Analysis cobolDeclaration = analyzeMetamorphic(
                "DFHRESP", "IDX", "01 DFHRESP OCCURS 10 TIMES PIC 9.", "");

        assertAll("metamorphic classification relations",
                () -> assertEquals(semanticSignature(classify(baseline)), semanticSignature(classify(caseChanged))),
                () -> assertEquals(semanticSignature(classify(baseline)), semanticSignature(classify(argumentChanged))),
                () -> assertEquals(semanticSignature(classify(baseline)),
                        semanticSignature(classify(unrelatedDeclaration))),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        entry(baseline, "DFHRESP(IDX)").status()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                        entry(cobolDeclaration, "DFHRESP(IDX)").status()),
                () -> assertTrue(classify(cobolDeclaration).entries().isEmpty()));
    }

    @Test
    void argumentBindingDoesNotDetermineClassificationAndRepeatedRunsAreDeterministic() throws Exception {
        ExternalClassificationTestSupport.Analysis resolvedArgument = analyzeMetamorphic(
                "DFHVALUE", "IDX", "", "01 IDX PIC 9.");
        ExternalClassificationTestSupport.Analysis unresolvedArgument = analyzeMetamorphic(
                "DFHVALUE", "MISSING", "", "");

        ExternalClassification first = classify(resolvedArgument);
        ExternalClassification second = classify(resolvedArgument);
        ExternalClassification unresolved = classify(unresolvedArgument);

        assertAll("argument and determinism",
                () -> assertEquals(1, first.entries().size()),
                () -> assertEquals(first.entries(), second.entries()),
                () -> assertEquals(semanticSignature(first), semanticSignature(unresolved)),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                        entry(resolvedArgument, "IDX").status()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        entry(unresolvedArgument, "MISSING").status()),
                () -> assertTrue(first.entries().get(0).coveredOccurrenceIds().contains(
                        entry(resolvedArgument, "IDX").occurrence().id())));
    }

    @Test
    void literalAndAmbiguousArgumentsDoNotActAsAHiddenArgumentCatalog() throws Exception {
        ExternalClassificationTestSupport.Analysis literal = ExternalClassificationTestSupport.analyze(fixed("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. LITERALARG.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-RESP PIC S9(8) COMP.
                PROCEDURE DIVISION.
                    IF WS-RESP = DFHRESP(7)
                        CONTINUE
                    END-IF.
                    GOBACK.
                END PROGRAM LITERALARG.
                """), "literal-argument.cbl");
        ExternalClassificationTestSupport.Analysis ambiguous = ExternalClassificationTestSupport.analyze(fixed("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. AMBIGUOUSARG.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-RESP PIC S9(8) COMP.
                01 GROUP-A.
                   05 ARGUMENT-NAME PIC 9.
                01 GROUP-B.
                   05 ARGUMENT-NAME PIC 9.
                PROCEDURE DIVISION.
                    IF WS-RESP = DFHVALUE(ARGUMENT-NAME)
                        CONTINUE
                    END-IF.
                    GOBACK.
                END PROGRAM AMBIGUOUSARG.
                """), "ambiguous-argument.cbl");

        assertAll("argument states remain orthogonal to the base decision",
                () -> assertEquals(1, classify(literal).entries().size()),
                () -> assertEquals(1, classify(literal).entries().get(0).coveredOccurrenceIds().size()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                        entry(ambiguous, "ARGUMENT-NAME").status()),
                () -> assertEquals(1, classify(ambiguous).entries().size()),
                () -> assertTrue(classify(ambiguous).entries().get(0).coveredOccurrenceIds().contains(
                        entry(ambiguous, "ARGUMENT-NAME").occurrence().id())));
    }

    private static ExternalClassificationTestSupport.Analysis analyzeMetamorphic(
            String base, String argument, String baseDeclaration, String unrelatedDeclaration) throws Exception {
        return ExternalClassificationTestSupport.analyze(fixed("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. METAMORPHIC.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-RESP PIC S9(8) COMP.
                %s
                %s
                PROCEDURE DIVISION.
                    IF WS-RESP = %s(%s)
                        CONTINUE
                    END-IF.
                    GOBACK.
                END PROGRAM METAMORPHIC.
                """.formatted(baseDeclaration, unrelatedDeclaration, base, argument)), "metamorphic.cbl");
    }

    private static ExternalClassification classify(ExternalClassificationTestSupport.Analysis analysis) {
        return new CicsIntrinsicClassifier().classify(
                analysis.model(), analysis.occurrences(), analysis.resolution());
    }

    private static ReferenceResolution.Entry entry(
            ExternalClassificationTestSupport.Analysis analysis, String writtenText) {
        return analysis.resolution().entries().stream()
                .filter(value -> value.occurrence().writtenText().equals(writtenText))
                .findFirst().orElseThrow();
    }

    private static List<String> semanticSignature(ExternalClassification classifications) {
        return classifications.entries().stream().map(entry -> entry.technology() + "|" + entry.kind()
                + "|" + entry.certainty() + "|" + entry.reason() + "|"
                + entry.coveredOccurrenceIds().size()).toList();
    }

    private static String fixed(String freeFormat) {
        return freeFormat.lines().map(line -> line.isBlank() ? "" : "       " + line)
                .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
    }

    private static ReferenceResolution withRootStatus(
            ReferenceResolution resolution, String writtenText,
            ResolutionContracts.ResolutionStatus status,
            ResolutionContracts.ResolutionReason reason) {
        List<ReferenceResolution.Entry> entries = resolution.entries().stream().map(entry ->
                entry.occurrence().writtenText().equals(writtenText)
                        ? new ReferenceResolution.Entry(entry.id(), entry.occurrence(), status, reason,
                        List.of(), entry.diagnosticIds(), entry.callSemantics())
                        : entry).toList();
        return new ReferenceResolution(resolution.policy(), entries, resolution.diagnostics(),
                resolution.metrics(), resolution.declarationRelations());
    }

    private static List<String> bindingSignature(ReferenceResolution resolution) {
        return resolution.entries().stream().map(entry -> entry.id() + "|"
                + entry.occurrence().programUnitId() + "|" + entry.occurrence().id() + "|"
                + entry.status() + "|" + entry.reason() + "|" + entry.candidates()).toList();
    }
}
