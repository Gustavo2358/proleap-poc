package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Production projection coverage for WORK-SEMANTIC-PRODUCT-002 Checkpoint 3. */
class CobolSemanticProductProjectorTest {
    private static final String SOURCE_NAME = "semantic-product-projection.cbl";
    private static final String MULTIPLE_SOURCE = String.join("\n",
            "       IDENTIFICATION DIVISION.",
            "       PROGRAM-ID. SEMANTIC-PROJECTION.",
            "       DATA DIVISION.",
            "       WORKING-STORAGE SECTION.",
            "       01 WS-A PIC X(8).",
            "       01 WS-B PIC X(8).",
            "       01 WS-N PIC 9.",
            "       01 UNUSED PIC X.",
            "       PROCEDURE DIVISION.",
            "           MOVE 'A' TO WS-A.",
            "           CALL WS-B.",
            "           MOVE 'B' TO WS-B.",
            "           CALL WS-A.",
            "           MOVE 1 TO WS-N.",
            "           CALL WS-B.",
            "           GOBACK.",
            "       END PROGRAM SEMANTIC-PROJECTION.", "");
    private static final String SINGLE_SOURCE = String.join("\n",
            "       IDENTIFICATION DIVISION.",
            "       PROGRAM-ID. SEMANTIC-SINGLE.",
            "       DATA DIVISION.",
            "       WORKING-STORAGE SECTION.",
            "       01 WS-PGM PIC X(8).",
            "       PROCEDURE DIVISION.",
            "           MOVE 'PGMA' TO WS-PGM.",
            "           CALL WS-PGM.",
            "           GOBACK.",
            "       END PROGRAM SEMANTIC-SINGLE.", "");

    @Test
    void projectsEverySupportedDataMoveAndCallWithoutPairing() {
        AstBoundaryTestSupport.Analysis frontend = analyze(MULTIPLE_SOURCE, SOURCE_NAME);
        CobolSemanticProduct.State state = project(frontend);
        CobolSemanticPort port = CobolSemanticProductProjector.open(
                products(frontend), unit(frontend));

        assertEquals(List.of("WS-A", "WS-B", "WS-N", "UNUSED"),
                port.dataDeclarations().stream()
                .map(CobolSemanticProduct.DataDeclaration::canonicalName).toList());
        assertEquals(List.of(Optional.of("X(8)"), Optional.of("X(8)"), Optional.of("9"),
                        Optional.of("X")),
                port.dataDeclarations().stream()
                        .map(CobolSemanticProduct.DataDeclaration::picture).toList());
        assertEquals(6, port.statements().size());
        assertEquals(3, port.moves().size());
        assertEquals(3, port.calls().size());
        assertTrue(port.observedStatements().isEmpty());

        Map<CobolSemanticProduct.DataItemId, String> names = new LinkedHashMap<>();
        port.dataDeclarations().forEach(declaration ->
                names.put(declaration.id(), declaration.canonicalName()));
        assertEquals(List.of("WS-A", "WS-B", "WS-N"), port.moves().stream()
                .map(move -> names.get(move.target().binding().selected().orElseThrow()))
                .toList());
        assertEquals(List.of("WS-B", "WS-A", "WS-B"), port.calls().stream()
                .map(call -> names.get(call.operand().binding().selected().orElseThrow()))
                .toList(), "CALL facts must not depend on a MOVE pair or source proximity");
        assertEquals(List.of("A", "B", "1"), port.moves().stream()
                .map(move -> move.source().value()).toList());

        assertEquals(6, new HashSet<>(port.statements().stream()
                .map(statement -> statement.header().id()).toList()).size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5), port.statements().stream()
                .map(statement -> statement.header().point().ordinal()).toList());
        assertTrue(port.statements().stream().allMatch(statement ->
                statement.header().id().unit().equals(port.unit())));
        assertTrue(port.moves().stream().allMatch(move ->
                move.target().role() == CobolSemanticProduct.OperandRole.WRITE
                        && move.target().binding().status()
                        == CobolSemanticProduct.ResolutionStatus.RESOLVED));
        assertTrue(port.calls().stream().allMatch(call ->
                call.operand().role() == CobolSemanticProduct.OperandRole.CALL_TARGET
                        && call.operand().binding().status()
                        == CobolSemanticProduct.ResolutionStatus.RESOLVED
                        && call.runtimeTarget()
                        == CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN));

        assertEquals(CobolSemanticProduct.InventoryStatus.PARTIAL,
                state.coverage().inventoryStatus());
        assertEquals(6, state.coverage().observedStatements());
        assertEquals(3, state.coverage().modeledStatements());
        assertEquals(3, state.coverage().partialStatements());
        assertEquals(CobolSemanticProduct.ReadinessStatus.PARTIAL,
                state.coverage().readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.PARTIAL,
                state.coverage().readiness().cfg().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                state.coverage().readiness().effectsDataflow().status());
        assertEquals(state, portState(port));

        assertTrue(port.dataDeclarations().stream().allMatch(declaration ->
                declaration.provenance().original().file().equals(SOURCE_NAME)
                        && declaration.provenance().exact()));
        assertTrue(port.statements().stream().allMatch(statement ->
                statement.header().provenance().original().file().equals(SOURCE_NAME)
                        && statement.header().provenance().exact()));
    }

    @Test
    void historicalCardinalityOneCaseRemainsARegression() {
        AstBoundaryTestSupport.Analysis frontend = analyze(
                SINGLE_SOURCE, "semantic-product-single.cbl");
        CobolSemanticPort port = CobolSemanticPort.open(project(frontend));

        assertEquals(1, port.dataDeclarations().size());
        assertEquals(1, port.moves().size());
        assertEquals(1, port.calls().size());
        assertEquals("PGMA", port.moves().get(0).source().value());
        assertEquals(port.moves().get(0).target().binding().selected(),
                port.calls().get(0).operand().binding().selected());
    }

    @Test
    void missingLiteralKindAuthorityStaysUnknownForEveryLiteralSpelling() {
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(MULTIPLE_SOURCE, SOURCE_NAME)));

        assertTrue(port.moves().stream().allMatch(move ->
                move.source().kind() == CobolSemanticProduct.LiteralKind.UNKNOWN));
        assertEquals(3, port.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.LITERAL_KIND
                        && gap.code().equals("LITERAL_KIND_NOT_PUBLISHED")).count());
    }

    @Test
    void shapesOutsideTheCapabilityRemainObservedInsteadOfDisappearing() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-UNSUPPORTED.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-A PIC X(8).",
                "       01 WS-B PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           MOVE WS-A TO WS-B.",
                "           CALL 'PGMA'.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-UNSUPPORTED.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-unsupported.cbl")));

        assertTrue(port.moves().isEmpty());
        assertTrue(port.calls().isEmpty());
        assertEquals(List.of("MOVE_NON_LITERAL_SOURCE", "CALL_LITERAL_TARGET"),
                port.observedStatements().stream()
                        .map(CobolSemanticProduct.ObservedStatement::observedShape).toList());
        assertEquals(List.of("MOVE_NON_LITERAL_SOURCE_OUTSIDE_CAPABILITY",
                        "CALL_LITERAL_TARGET_OUTSIDE_CAPABILITY"),
                port.observedStatements().stream()
                        .map(CobolSemanticProduct.ObservedStatement::gapCode).toList());
        assertTrue(port.observedStatements().stream().allMatch(observed ->
                observed.header().coverage() == CobolSemanticProduct.CoverageStatus.UNSUPPORTED));
        assertEquals(2, port.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.CAPABILITY
                        && gap.code().endsWith("OUTSIDE_CAPABILITY")).count());
    }

    @Test
    void callUncertaintiesComeFromTheCanonicalReport() {
        AstBoundaryTestSupport.Analysis frontend = analyze(MULTIPLE_SOURCE, SOURCE_NAME);
        CobolSemanticProduct.State state = project(frontend);
        List<String> reportDetails = frontend.report().gaps().stream()
                .filter(gap -> gap.category() == ResolutionAnalysisReport.GapCategory.CALL_SEMANTICS)
                .filter(gap -> gap.code().equals("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"))
                .map(ResolutionAnalysisReport.Gap::message).toList();
        List<String> projectedDetails = state.gaps().stream()
                .filter(gap -> gap.scope() == CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET)
                .map(CobolSemanticProduct.Gap::detail).toList();

        assertEquals(3, reportDetails.size());
        assertEquals(reportDetails, projectedDetails);
        assertEquals(frontend.report().policy().policyId(), state.policy().policyId());
        assertEquals(frontend.report().policy().version(), state.policy().version());
    }

    @Test
    void ambiguousBindingsPreserveEveryCanonicalCandidateWithoutSelection() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-AMBIGUOUS.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-PGM PIC X(8).",
                "       01 WS-PGM PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           MOVE 'A' TO WS-PGM.",
                "           CALL WS-PGM.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-AMBIGUOUS.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-ambiguous.cbl")));

        assertEquals(2, port.dataDeclarations().size());
        assertEquals(CobolSemanticProduct.ResolutionStatus.AMBIGUOUS,
                port.moves().get(0).target().binding().status());
        assertEquals(CobolSemanticProduct.ResolutionStatus.AMBIGUOUS,
                port.calls().get(0).operand().binding().status());
        assertEquals(2, port.moves().get(0).target().binding().candidates().size());
        assertEquals(2, port.calls().get(0).operand().binding().candidates().size());
        assertTrue(port.moves().get(0).target().binding().selected().isEmpty());
        assertTrue(port.calls().get(0).operand().binding().selected().isEmpty());
        assertEquals(2, port.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.NOMINAL_BINDING).count());
    }

    @Test
    void mismatchedReportFailsClosedInsteadOfReclassifyingFacts() {
        AstBoundaryTestSupport.Analysis frontend = analyze(MULTIPLE_SOURCE, SOURCE_NAME);
        AstBoundaryTestSupport.Analysis other = analyze(
                MULTIPLE_SOURCE.replace("SEMANTIC-PROJECTION", "SEMANTIC-OTHER"),
                "semantic-product-other.cbl");
        CobolSemanticProductProjector.FrontendProducts mismatched =
                new CobolSemanticProductProjector.FrontendProducts(
                        frontend.build(), frontend.tables(), frontend.occurrences(),
                        frontend.resolution(), other.report());

        assertThrows(IllegalArgumentException.class,
                () -> CobolSemanticProductProjector.project(mismatched, unit(frontend)));
    }

    @Test
    void canonicalNamespaceMismatchFailsInsteadOfJoiningByName() {
        AstBoundaryTestSupport.Analysis frontend = analyze(MULTIPLE_SOURCE, SOURCE_NAME);
        ReferenceResolution.Entry originalCall = null;
        for (ReferenceResolution.Entry entry : frontend.resolution().entries()) {
            if (entry.occurrence().role() == ResolutionContracts.ReferenceRole.CALL_TARGET) {
                originalCall = entry;
                break;
            }
        }
        if (originalCall == null) throw new AssertionError("fixture must contain a CALL target");
        ReferenceResolution.Candidate originalCandidate =
                originalCall.selectedCandidate().orElseThrow();
        ResolutionContracts.ProgramUnitId foreignUnit = new ResolutionContracts.ProgramUnitId(
                "OTHER.CBL", List.of(0), originalCandidate.canonicalName());
        ReferenceResolution.Candidate foreignCandidate = new ReferenceResolution.Candidate(
                new ResolutionContracts.SemanticEntityId(foreignUnit,
                        ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                        originalCandidate.entityId().localId()),
                originalCandidate.kind(), originalCandidate.writtenName(),
                originalCandidate.canonicalName(), originalCandidate.declarationSymbolIds(),
                originalCandidate.attributes());
        ReferenceResolution.Entry forgedCall = new ReferenceResolution.Entry(
                originalCall.id(), originalCall.occurrence(), originalCall.status(),
                originalCall.reason(), List.of(foreignCandidate), originalCall.diagnosticIds(),
                originalCall.callSemantics());
        List<ReferenceResolution.Entry> entries = new ArrayList<>(frontend.resolution().entries());
        entries.set(originalCall.id(), forgedCall);
        ReferenceResolution forgedResolution = new ReferenceResolution(
                frontend.resolution().policy(), entries, frontend.resolution().diagnostics(),
                frontend.resolution().metrics(), frontend.resolution().declarationRelations());
        CobolSemanticProductProjector.FrontendProducts forged =
                new CobolSemanticProductProjector.FrontendProducts(
                        frontend.build(), frontend.tables(), frontend.occurrences(),
                        forgedResolution, frontend.report());

        assertThrows(IllegalArgumentException.class,
                () -> CobolSemanticProductProjector.project(forged, unit(frontend)));
    }

    @Test
    void projectionDoesNotReparseOrDependOnPresentationOrAnalysisEngines()
            throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/projection/"
                        + "CobolSemanticProductProjector.java"), StandardCharsets.UTF_8);

        assertFalse(source.contains("findFirst"));
        assertFalse(source.contains("findLast"));
        assertFalse(source.contains("single("));
        assertFalse(source.contains("writtenText"));
        assertFalse(source.contains("rawLexeme"));
        assertFalse(source.contains("grammarRule"));
        assertFalse(source.contains("SourceMap"));
        assertFalse(source.contains("Snapshot"));
        assertFalse(source.contains("ExplorerMain"));
        assertFalse(source.contains("AstBuilder"));
        assertFalse(source.contains("CobolReferenceResolver"));
        assertFalse(source.contains("ReferenceOccurrenceCollector"));
        assertFalse(source.contains("org.antlr"));
    }

    private static AstBoundaryTestSupport.Analysis analyze(String source, String sourceName) {
        return AstBoundaryTestSupport.analyze(source, sourceName);
    }

    private static ResolutionContracts.ProgramUnitId unit(
            AstBoundaryTestSupport.Analysis frontend) {
        return frontend.model().programUnits().get(0).id();
    }

    private static CobolSemanticProduct.State project(
            AstBoundaryTestSupport.Analysis frontend) {
        return CobolSemanticProductProjector.project(products(frontend), unit(frontend));
    }

    private static CobolSemanticProductProjector.FrontendProducts products(
            AstBoundaryTestSupport.Analysis frontend) {
        return new CobolSemanticProductProjector.FrontendProducts(
                frontend.build(), frontend.tables(), frontend.occurrences(),
                frontend.resolution(), frontend.report());
    }

    private static CobolSemanticProduct.State portState(CobolSemanticPort port) {
        return new CobolSemanticProduct.State(port.unit(), port.policy(),
                port.dataDeclarations(), port.statements(), port.gaps(), port.coverage());
    }
}
