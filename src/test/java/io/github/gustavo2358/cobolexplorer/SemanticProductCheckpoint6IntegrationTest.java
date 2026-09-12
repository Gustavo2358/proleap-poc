package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.consumer.CobolLoweringReadinessConsumer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Boundary-only production integration oracle for WORK-SEMANTIC-PRODUCT-002 CP6. */
class SemanticProductCheckpoint6IntegrationTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/semantic/semantic-product-lowering-readiness.cbl");

    @Test
    void compositionRootPublishesAClosedPortConsumedWithoutFrontend() throws IOException {
        CobolSemanticPort port = publishFixtureAndReleaseFrontend();
        CobolLoweringReadinessConsumer.Audit audit = consumeOnly(port);

        assertEquals("SEMANTIC-TARGET", audit.unit().canonicalProgramName());
        assertEquals(List.of("WS-X", "FLAG", "AUX-PGM"), audit.dataDeclarations().stream()
                .map(CobolLoweringReadinessConsumer.DataAudit::canonicalName).toList());
        assertEquals(List.of("X(8)", "9", "X(8)"), audit.dataDeclarations().stream()
                .map(data -> data.picture().orElseThrow()).toList());
        assertEquals(14, audit.statements().size());
        assertEquals(IntStream.range(0, 14).boxed().toList(), audit.statements().stream()
                .map(statement -> statement.header().programPoint()).toList());
        assertEquals(7, statements(audit, CobolLoweringReadinessConsumer.MoveAudit.class).size());
        assertEquals(3, statements(audit, CobolLoweringReadinessConsumer.CallAudit.class).size());
        assertEquals(3, statements(audit, CobolLoweringReadinessConsumer.IfAudit.class).size());
        assertEquals(1, statements(audit, CobolLoweringReadinessConsumer.ObservedAudit.class).size());

        assertTrue(audit.dataDeclarations().stream().allMatch(data ->
                data.id().unit().equals(audit.unit()) && data.provenance().exact()));
        assertTrue(audit.statements().stream().allMatch(statement ->
                statement.header().id().unit().equals(audit.unit())
                        && statement.header().provenance().exact()));
        assertEquals(CobolSemanticProduct.QualifyMode.UNSPECIFIED,
                audit.policy().qualifyMode());
        assertEquals(CobolSemanticProduct.DynamMode.UNSPECIFIED,
                audit.policy().dynamMode());

        assertThrows(UnsupportedOperationException.class, audit.statements()::clear);
        assertThrows(UnsupportedOperationException.class, audit.gaps()::clear);
        assertTrue(Arrays.stream(CobolLoweringReadinessConsumer.Audit.class
                        .getRecordComponents()).map(RecordComponent::getType)
                .noneMatch(CobolSemanticPort.class::isAssignableFrom),
                "the materialized audit must not retain its input port");
        assertEquals(List.of(CobolSemanticPort.class), Arrays.stream(
                        CobolLoweringReadinessConsumer.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("audit"))
                .map(method -> List.of(method.getParameterTypes())).findFirst().orElseThrow());
    }

    @Test
    void consumerReconstructsMoveCallIfObservedStructureAndBindings() throws IOException {
        CobolLoweringReadinessConsumer.Audit audit = consumeOnly(
                publishFixtureAndReleaseFrontend());
        Map<CobolSemanticProduct.DataItemId, String> dataNames = new HashMap<>();
        audit.dataDeclarations().forEach(data -> dataNames.put(data.id(), data.canonicalName()));

        List<CobolLoweringReadinessConsumer.MoveAudit> moves = statements(
                audit, CobolLoweringReadinessConsumer.MoveAudit.class);
        assertEquals(List.of("A", "AUXPGM", "B", "NEST", "AFTER", "C", "D"),
                moves.stream().map(move -> move.source().value()).toList());
        assertTrue(moves.stream().allMatch(move ->
                move.source().kind() == CobolSemanticProduct.LiteralKind.ALPHANUMERIC
                        && move.target().role() == CobolSemanticProduct.OperandRole.WRITE
                        && move.target().binding().status()
                        == CobolSemanticProduct.ResolutionStatus.RESOLVED));
        assertEquals(List.of("WS-X", "AUX-PGM", "WS-X", "AUX-PGM", "AUX-PGM",
                        "WS-X", "WS-X"), moves.stream().map(move -> dataNames.get(
                        move.target().binding().selected().orElseThrow())).toList());

        List<CobolLoweringReadinessConsumer.CallAudit> calls = statements(
                audit, CobolLoweringReadinessConsumer.CallAudit.class);
        assertEquals(List.of("AUX-PGM", "WS-X", "AUX-PGM"), calls.stream()
                .map(call -> dataNames.get(((CobolSemanticProduct.DataReference) call.target()).binding().selected().orElseThrow()))
                .toList());
        assertTrue(calls.stream().allMatch(call ->
                call.syntax() == CobolSemanticProduct.CallSyntax.IDENTIFIER_OR_EXPRESSION
                        && ((CobolSemanticProduct.DataReference) call.target()).role() == CobolSemanticProduct.OperandRole.CALL_TARGET
                        && call.runtimeTarget()
                        == CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN
                        && call.runtimeUncertaintyCode()
                        .equals("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN")));

        CobolLoweringReadinessConsumer.IfAudit outer = statement(audit, 10,
                CobolLoweringReadinessConsumer.IfAudit.class);
        CobolLoweringReadinessConsumer.IfAudit nested = statement(audit, 11,
                CobolLoweringReadinessConsumer.IfAudit.class);
        CobolLoweringReadinessConsumer.IfAudit emptyElse = statement(audit, 12,
                CobolLoweringReadinessConsumer.IfAudit.class);
        assertEquals("RELATION", outer.condition().shape());
        assertEquals(1, outer.condition().references().size());
        assertEquals(CobolSemanticProduct.OperandRole.READ,
                outer.condition().references().get(0).role());
        assertEquals("FLAG", dataNames.get(outer.condition().references().get(0)
                .binding().selected().orElseThrow()));
        assertEquals(List.of(id(audit, 2), id(audit, 11), id(audit, 5)),
                outer.thenMembers());
        assertEquals(List.of(id(audit, 6)), outer.elseMembers());
        assertEquals(java.util.Optional.of(id(audit, 7)), outer.continuation());
        assertEquals(List.of(id(audit, 3)), nested.thenMembers());
        assertEquals(List.of(id(audit, 4)), nested.elseMembers());
        assertEquals(java.util.Optional.of(id(audit, 5)), nested.continuation());
        assertEquals(java.util.Optional.of(id(audit, 10)),
                nested.header().containment().parent());
        assertEquals(CobolSemanticProduct.Branch.THEN,
                nested.header().containment().branch());
        assertEquals(List.of(id(audit, 9)), emptyElse.thenMembers());
        assertTrue(emptyElse.elseMembers().isEmpty());
        assertEquals(java.util.Optional.of(id(audit, 13)), emptyElse.continuation());
        assertTrue(List.of(outer, nested, emptyElse).stream()
                .allMatch(CobolLoweringReadinessConsumer.IfAudit::explicitlyTerminated));

        CobolLoweringReadinessConsumer.ObservedAudit observed = statements(
                audit, CobolLoweringReadinessConsumer.ObservedAudit.class).get(0);
        assertEquals("PRESERVED_STATEMENT", observed.observedKind());
        assertEquals("GENERIC_PRESERVED_STATEMENT", observed.observedShape());
        assertEquals("OBSERVED_STATEMENT_PARTIAL", observed.gapCode());
        assertEquals(CobolSemanticProduct.CoverageStatus.PARTIAL,
                observed.header().coverage());
        assertTrue(observed.header().gaps().stream().anyMatch(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.CAPABILITY
                        && gap.code().equals(observed.gapCode())));

        assertEquals(List.of(id(audit, 0), id(audit, 1), id(audit, 10), id(audit, 7),
                id(audit, 8), id(audit, 12), id(audit, 13)), audit.rootStatements());
        assertEquals(14, audit.statements().stream().map(statement -> statement.header().id())
                .collect(java.util.stream.Collectors.toSet()).size());
    }

    @Test
    void readinessMatrixAndGapsRemainDimensionalAndUnpromoted() throws IOException {
        CobolSemanticPort port = publishFixtureAndReleaseFrontend();
        CobolLoweringReadinessConsumer.Audit audit = consumeOnly(port);
        Map<CobolSemanticProduct.StatementId, CobolSemanticProduct.StatementFact> sourceFacts =
                new HashMap<>();
        port.statements().forEach(fact -> sourceFacts.put(fact.header().id(), fact));

        for (CobolLoweringReadinessConsumer.StatementAudit statement : audit.statements()) {
            CobolSemanticProduct.StatementHeader source = sourceFacts.get(statement.header().id())
                    .header();
            assertEquals(source.coverage(), statement.header().coverage());
            assertDimension(source.readiness().lowering(), statement.header().readiness().lowering());
            assertDimension(source.readiness().cfg(), statement.header().readiness().cfg());
            assertDimension(source.readiness().effectsDataflow(),
                    statement.header().readiness().effectsDataflow());
            assertEquals(port.gaps().stream().filter(gap ->
                            gap.statement().equals(statement.header().id())).map(
                            CobolSemanticProduct.Gap::code).toList(),
                    statement.header().gaps().stream()
                            .map(CobolLoweringReadinessConsumer.GapAudit::code).toList());
        }

        CobolLoweringReadinessConsumer.MoveAudit move = statements(
                audit, CobolLoweringReadinessConsumer.MoveAudit.class).get(0);
        CobolLoweringReadinessConsumer.CallAudit call = statement(audit, 7,
                CobolLoweringReadinessConsumer.CallAudit.class);
        CobolLoweringReadinessConsumer.IfAudit branch = statement(audit, 10,
                CobolLoweringReadinessConsumer.IfAudit.class);
        CobolLoweringReadinessConsumer.ObservedAudit observed = statement(audit, 13,
                CobolLoweringReadinessConsumer.ObservedAudit.class);

        assertTrue(audit.dataDeclarations().stream().allMatch(data ->
                data.readiness().lowering().status()
                        == CobolSemanticProduct.ReadinessStatus.SUFFICIENT
                        && data.readiness().cfg().status()
                        == CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE
                        && data.readiness().effectsDataflow().status()
                        == CobolSemanticProduct.ReadinessStatus.PARTIAL));
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                move.header().readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                move.header().readiness().cfg().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.PARTIAL,
                move.header().readiness().effectsDataflow().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                call.header().readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                call.header().readiness().cfg().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.PARTIAL,
                call.header().readiness().effectsDataflow().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.PARTIAL,
                branch.header().readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                branch.header().readiness().cfg().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                observed.header().readiness().lowering().status());

        assertEquals(CobolSemanticProduct.InventoryStatus.COMPLETE,
                audit.coverage().inventoryStatus());
        assertEquals(14, audit.coverage().observedStatements());
        // SP 1.4 proves completion for the five existing IF-contained MOVEs.
        assertEquals(9, audit.coverage().modeledStatements());
        assertEquals(5, audit.coverage().partialStatements());
        assertEquals(0, audit.coverage().unsupportedStatements());
        assertEquals(0, audit.coverage().inputMissingStatements());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                audit.coverage().readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                audit.coverage().readiness().cfg().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                audit.coverage().readiness().effectsDataflow().status());

        assertEquals(3, audit.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET).count());
        assertEquals(3, audit.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.CONDITION_SEMANTICS
                        && gap.code().equals("CONDITION_SEMANTICS_NOT_AVAILABLE")).count());
        assertEquals(1, audit.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.CAPABILITY
                        && gap.code().equals("OBSERVED_STATEMENT_PARTIAL")).count());
        assertFalse(audit.gaps().isEmpty());
    }

    @Test
    void compositionKeepsCanonicalExternalClassificationOutsideDataBinding() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-EXTERNAL.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-RESP PIC S9(8) COMP.",
                "       PROCEDURE DIVISION.",
                "           IF WS-RESP = DFHRESP(NORMAL)",
                "               CONTINUE",
                "           END-IF.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-EXTERNAL.", "");
        AstBoundaryTestSupport.Analysis frontend = AstBoundaryTestSupport.analyze(
                source, "semantic-product-external.cbl");
        ExternalClassification classifications = new CicsIntrinsicClassifier().classify(
                frontend.model(), frontend.occurrences(), frontend.resolution());
        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(frontend.build(),
                ResolutionAnalysisReport.FrontendState.complete(), frontend.occurrences(),
                frontend.resolution(), classifications);
        ResolutionContracts.ProgramUnitId unitId = frontend.model().programUnits().get(0).id();

        CobolLoweringReadinessConsumer.Audit audit = consumeOnly(
                ExplorerMain.publishSemanticProduct(unitId, frontend.build(), frontend.tables(),
                        frontend.occurrences(), frontend.resolution(), report));
        CobolLoweringReadinessConsumer.IfAudit branch = statements(
                audit, CobolLoweringReadinessConsumer.IfAudit.class).get(0);

        assertEquals(1, classifications.entries().size());
        assertEquals(1, branch.condition().references().size());
        assertEquals("WS-RESP", audit.dataDeclarations().stream().filter(data ->
                        data.id().equals(branch.condition().references().get(0).binding()
                                .selected().orElseThrow()))
                .findFirst().orElseThrow().canonicalName());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                branch.header().readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                branch.header().readiness().cfg().status());
        assertTrue(branch.header().gaps().stream().anyMatch(gap ->
                gap.code().equals("CONDITION_REFERENCE_KIND_NOT_PROJECTED")));
        assertTrue(branch.header().gaps().stream().anyMatch(gap ->
                gap.code().equals("EXTERNAL_INFERRED_CICS_POSSIBLE_INTRINSIC")));
    }

    private static CobolSemanticPort publishFixtureAndReleaseFrontend() throws IOException {
        AstBoundaryTestSupport.Analysis frontend = AstBoundaryTestSupport.analyze(
                Files.readString(FIXTURE, StandardCharsets.UTF_8), FIXTURE.getFileName().toString());
        ResolutionContracts.ProgramUnitId unitId = frontend.model().programUnits().get(0).id();
        return ExplorerMain.publishSemanticProduct(unitId, frontend.build(), frontend.tables(),
                frontend.occurrences(), frontend.resolution(), frontend.report());
    }

    /** This is the complete dependency surface delivered to the production consumer. */
    private static CobolLoweringReadinessConsumer.Audit consumeOnly(CobolSemanticPort port) {
        return CobolLoweringReadinessConsumer.audit(port);
    }

    private static void assertDimension(CobolSemanticProduct.ReadinessClaim expected,
                                        CobolLoweringReadinessConsumer.DimensionAudit actual) {
        assertEquals(expected.status(), actual.status());
        assertEquals(expected.scope(), actual.scope());
    }

    private static CobolSemanticProduct.StatementId id(
            CobolLoweringReadinessConsumer.Audit audit, int localId) {
        return new CobolSemanticProduct.StatementId(audit.unit(), localId);
    }

    private static <T extends CobolLoweringReadinessConsumer.StatementAudit> T statement(
            CobolLoweringReadinessConsumer.Audit audit, int localId, Class<T> type) {
        return type.cast(audit.statements().stream().filter(statement ->
                        statement.header().id().localId() == localId)
                .findFirst().orElseThrow());
    }

    private static <T extends CobolLoweringReadinessConsumer.StatementAudit> List<T> statements(
            CobolLoweringReadinessConsumer.Audit audit, Class<T> type) {
        return audit.statements().stream().filter(type::isInstance).map(type::cast).toList();
    }
}
