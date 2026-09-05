package io.github.gustavo2358.cobolexplorer.semanticproduct;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for Checkpoint 2.  Every state in this test is built directly
 * from boundary values; no frontend, parser or composition root is involved.
 */
class SemanticProductMoveCallContractTest {
    private static final CobolSemanticProduct.UnitId UNIT =
            new CobolSemanticProduct.UnitId("semantic-product.cbl", List.of(0), "SEMANTIC-PRODUCT");
    private static final CobolSemanticProduct.DataItemId DATA =
            new CobolSemanticProduct.DataItemId(UNIT, 17);
    private static final CobolSemanticProduct.Provenance PROVENANCE = provenance(5);

    @Test
    void directlyConstructedStatePublishesTheApprovedSlice() {
        CobolSemanticProduct.State state = completeSlice();
        CobolSemanticPort port = CobolSemanticPort.open(state);

        assertEquals(UNIT, port.unit());
        assertEquals(DATA, port.dataItems().get(0).id());
        assertEquals("WS-PGM", port.dataItems().get(0).name());
        assertEquals("X(8)", port.dataItems().get(0).picture());
        assertEquals(CobolSemanticProduct.QualifyMode.UNSPECIFIED,
                port.policy().qualifyMode());
        assertEquals(CobolSemanticProduct.PgmnameMode.UNSPECIFIED,
                port.policy().pgmnameMode());
        assertEquals(CobolSemanticProduct.DynamMode.UNSPECIFIED,
                port.policy().dynamMode());
        assertEquals(CobolSemanticProduct.DllMode.UNSPECIFIED,
                port.policy().dllMode());
        assertEquals("PGMA", port.move().source().value());
        assertEquals(Optional.of(DATA), port.move().target());
        assertEquals(Optional.of(DATA), port.call().operand());
        assertEquals(Optional.of(DATA), port.call().operandBinding().selected());
        assertEquals(CobolSemanticProduct.CallSyntax.IDENTIFIER_OR_EXPRESSION,
                port.call().syntax());
        assertEquals(CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                port.call().runtimeTarget());
        assertEquals(CobolSemanticProduct.BindingStatus.COMPLETE,
                port.analysis().nominalBinding());
        assertEquals(CobolSemanticProduct.AnalysisClaim.PARTIAL,
                port.analysis().claim());
    }

    @Test
    void dataIdentityIsNamespacedAndSharedByMoveAndCall() {
        CobolSemanticProduct.State state = completeSlice();

        assertEquals(state.move().target(), state.call().operand());
        assertEquals(UNIT, state.move().target().orElseThrow().unit());
        assertNotEquals(DATA,
                new CobolSemanticProduct.DataItemId(
                        new CobolSemanticProduct.UnitId("other.cbl", List.of(0),
                                "SEMANTIC-PRODUCT"), DATA.localId()));
        assertEquals(CobolSemanticProduct.ResolutionStatus.RESOLVED,
                state.move().targetBinding().resolution());
        assertEquals(List.of(DATA), state.move().targetBinding().candidates().stream()
                .map(CobolSemanticProduct.DataCandidate::id).toList());
    }

    @Test
    void nominalBindingDoesNotBecomeTheRuntimeCallTarget() {
        CobolSemanticProduct.State state = completeSlice();

        assertEquals("PGMA", state.move().source().value());
        assertEquals(CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                state.call().runtimeTarget());
        assertEquals(List.of("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"),
                state.analysis().uncertainties().stream()
                        .map(CobolSemanticProduct.Uncertainty::code).toList());
        assertEquals(CobolSemanticProduct.UncertaintyScope.RUNTIME_CALL_TARGET,
                state.analysis().uncertainties().get(0).scope());
        assertEquals(state.call().point(), state.analysis().uncertainties().get(0).point());
    }

    @Test
    void orderingIsAnExplicitStrictProgramOrderRelation() {
        CobolSemanticProduct.State state = completeSlice();

        assertEquals(state.move().point(), state.ordering().earlier());
        assertEquals(state.call().point(), state.ordering().later());
        assertTrue(state.ordering().earlier().ordinal()
                < state.ordering().later().ordinal());
        assertEquals(List.of("unit", "dataItems", "policy", "move", "call",
                        "ordering", "analysis"),
                Arrays.stream(CobolSemanticProduct.State.class.getRecordComponents())
                        .map(component -> component.getName()).toList());
    }

    @Test
    void stateAndPortCopyAndExposeImmutableCollections() {
        List<CobolSemanticProduct.DataDeclaration> declarations =
                new ArrayList<>(List.of(declaration(DATA)));
        CobolSemanticProduct.State state = completeSlice(declarations);
        CobolSemanticPort port = CobolSemanticPort.open(state);
        declarations.clear();

        assertEquals(1, state.dataItems().size());
        assertThrows(UnsupportedOperationException.class, () -> state.dataItems().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.dataItems().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> state.unit().structuralPath().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> state.move().targetBinding().candidates().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> state.analysis().uncertainties().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> state.move().source().provenance().includeChain().clear());
        assertTrue(Modifier.isFinal(CobolSemanticProduct.State.class.getModifiers()));
        assertTrue(Modifier.isFinal(CobolSemanticProduct.DataItemId.class.getModifiers()));
        assertEquals(state.move(), port.move());
        assertEquals(state.call(), port.call());
    }

    @Test
    void queriesAreIndependentOfCallOrderAndPortRetainsOnlyMaterializedState() {
        CobolSemanticPort first = CobolSemanticPort.open(completeSlice());
        CobolSemanticPort second = CobolSemanticPort.open(completeSlice());

        Object firstCall = first.call();
        Object firstAnalysis = first.analysis();
        Object firstUnit = first.unit();
        Object firstData = first.dataItems();
        Object firstOrdering = first.ordering();
        Object firstMove = first.move();
        Object firstPolicy = first.policy();

        Object secondPolicy = second.policy();
        Object secondMove = second.move();
        Object secondOrdering = second.ordering();
        Object secondData = second.dataItems();
        Object secondUnit = second.unit();
        Object secondAnalysis = second.analysis();
        Object secondCall = second.call();

        assertEquals(firstCall, secondCall);
        assertEquals(firstAnalysis, secondAnalysis);
        assertEquals(firstUnit, secondUnit);
        assertEquals(firstData, secondData);
        assertEquals(firstOrdering, secondOrdering);
        assertEquals(firstMove, secondMove);
        assertEquals(firstPolicy, secondPolicy);
        assertEquals(1, first.getClass().getDeclaredFields().length,
                "the facade must retain only the immutable state");
    }

    @Test
    void partialAndUnknownFactsRemainVisibleWithoutFabricatingAHandle() {
        CobolSemanticProduct.NominalBinding unresolved =
                CobolSemanticProduct.NominalBinding.incomplete(
                        CobolSemanticProduct.ResolutionStatus.UNRESOLVED,
                        CobolSemanticProduct.ResolutionReason.DECLARATION_NOT_FOUND,
                        List.of());
        CobolSemanticProduct.ProgramPoint movePoint = new CobolSemanticProduct.ProgramPoint(1);
        CobolSemanticProduct.ProgramPoint callPoint = new CobolSemanticProduct.ProgramPoint(2);
        CobolSemanticProduct.Uncertainty moveUncertainty = new CobolSemanticProduct.Uncertainty(
                movePoint, CobolSemanticProduct.UncertaintyScope.NOMINAL_BINDING,
                "DATA_BINDING_UNKNOWN", "the MOVE target declaration is unavailable", PROVENANCE);
        CobolSemanticProduct.Uncertainty callUncertainty = new CobolSemanticProduct.Uncertainty(
                callPoint, CobolSemanticProduct.UncertaintyScope.RUNTIME_CALL_TARGET,
                "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN", "CALL operand value is not analyzed", PROVENANCE);
        CobolSemanticProduct.State partial = new CobolSemanticProduct.State(
                UNIT, List.of(declaration(DATA)), CobolSemanticProduct.Policy.unspecified(),
                new CobolSemanticProduct.MoveFact(movePoint,
                        new CobolSemanticProduct.LiteralSource("PGMA", PROVENANCE),
                        Optional.empty(), unresolved, PROVENANCE),
                new CobolSemanticProduct.CallFact(callPoint, Optional.empty(),
                        CobolSemanticProduct.CallSyntax.IDENTIFIER_OR_EXPRESSION,
                        unresolved, CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                        PROVENANCE),
                new CobolSemanticProduct.Ordering(movePoint, callPoint),
                new CobolSemanticProduct.AnalysisStatus(
                        CobolSemanticProduct.BindingStatus.INCOMPLETE,
                        CobolSemanticProduct.AnalysisClaim.UNKNOWN,
                        CobolSemanticProduct.DependencyReadiness.UNKNOWN,
                        CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                        List.of(moveUncertainty, callUncertainty)));

        assertEquals(CobolSemanticProduct.BindingStatus.INCOMPLETE,
                partial.analysis().nominalBinding());
        assertEquals(CobolSemanticProduct.AnalysisClaim.UNKNOWN, partial.analysis().claim());
        assertTrue(partial.move().target().isEmpty());
        assertTrue(partial.call().operand().isEmpty());
        assertEquals(CobolSemanticProduct.ResolutionReason.DECLARATION_NOT_FOUND,
                partial.call().operandBinding().reason());
        assertEquals(2, partial.analysis().uncertainties().size());
    }

    @Test
    void ambiguityPreservesCandidatesAndDoesNotSelectOneByOrder() {
        CobolSemanticProduct.UnitId secondUnit =
                new CobolSemanticProduct.UnitId("semantic-product.cbl", List.of(0), "SEMANTIC-PRODUCT");
        CobolSemanticProduct.DataItemId second =
                new CobolSemanticProduct.DataItemId(secondUnit, 18);
        CobolSemanticProduct.NominalBinding ambiguous =
                CobolSemanticProduct.NominalBinding.incomplete(
                        CobolSemanticProduct.ResolutionStatus.AMBIGUOUS,
                        CobolSemanticProduct.ResolutionReason.MULTIPLE_VALID_CANDIDATES,
                        List.of(new CobolSemanticProduct.DataCandidate(DATA, "WS-PGM"),
                                new CobolSemanticProduct.DataCandidate(second, "WS-PGM")));

        assertEquals(2, ambiguous.candidates().size());
        assertTrue(ambiguous.selected().isEmpty());
        assertEquals(List.of(DATA, second), ambiguous.candidates().stream()
                .map(CobolSemanticProduct.DataCandidate::id).toList());
    }

    @Test
    void invalidCompleteOrOrderingClaimsFailClosed() {
        CobolSemanticProduct.ProgramPoint same = new CobolSemanticProduct.ProgramPoint(1);
        assertThrows(IllegalArgumentException.class,
                () -> new CobolSemanticProduct.Ordering(same, same));
        assertThrows(IllegalArgumentException.class,
                () -> new CobolSemanticProduct.AnalysisStatus(
                        CobolSemanticProduct.BindingStatus.COMPLETE,
                        CobolSemanticProduct.AnalysisClaim.COMPLETE,
                        CobolSemanticProduct.DependencyReadiness.INCOMPLETE,
                        CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                        List.of(new CobolSemanticProduct.Uncertainty(
                                same, CobolSemanticProduct.UncertaintyScope.RUNTIME_CALL_TARGET,
                                "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN", "unknown", PROVENANCE))));
    }

    @Test
    void completeNominalBindingCannotCarryNominalBindingUncertainty() {
        CobolSemanticProduct.ProgramPoint point = new CobolSemanticProduct.ProgramPoint(1);
        CobolSemanticProduct.Uncertainty runtimeUncertainty = new CobolSemanticProduct.Uncertainty(
                point, CobolSemanticProduct.UncertaintyScope.RUNTIME_CALL_TARGET,
                "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN", "runtime target is unknown", PROVENANCE);
        CobolSemanticProduct.Uncertainty nominalUncertainty = new CobolSemanticProduct.Uncertainty(
                point, CobolSemanticProduct.UncertaintyScope.NOMINAL_BINDING,
                "DATA_BINDING_UNKNOWN", "nominal binding is uncertain", PROVENANCE);

        assertThrows(IllegalArgumentException.class,
                () -> new CobolSemanticProduct.AnalysisStatus(
                        CobolSemanticProduct.BindingStatus.COMPLETE,
                        CobolSemanticProduct.AnalysisClaim.PARTIAL,
                        CobolSemanticProduct.DependencyReadiness.INCOMPLETE,
                        CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                        List.of(runtimeUncertainty, nominalUncertainty)));
    }

    private static CobolSemanticProduct.State completeSlice() {
        return completeSlice(new ArrayList<>(List.of(declaration(DATA))));
    }

    private static CobolSemanticProduct.State completeSlice(
            List<CobolSemanticProduct.DataDeclaration> declarations) {
        CobolSemanticProduct.ProgramPoint movePoint = new CobolSemanticProduct.ProgramPoint(1);
        CobolSemanticProduct.ProgramPoint callPoint = new CobolSemanticProduct.ProgramPoint(2);
        CobolSemanticProduct.NominalBinding binding =
                CobolSemanticProduct.NominalBinding.resolved(DATA, "WS-PGM");
        CobolSemanticProduct.Uncertainty uncertainty = new CobolSemanticProduct.Uncertainty(
                callPoint, CobolSemanticProduct.UncertaintyScope.RUNTIME_CALL_TARGET,
                "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN",
                "CALL operand binding does not determine its runtime program target", PROVENANCE);
        return new CobolSemanticProduct.State(
                UNIT, declarations, CobolSemanticProduct.Policy.unspecified(),
                new CobolSemanticProduct.MoveFact(movePoint,
                        new CobolSemanticProduct.LiteralSource("PGMA", PROVENANCE),
                        Optional.of(DATA), binding, PROVENANCE),
                new CobolSemanticProduct.CallFact(callPoint, Optional.of(DATA),
                        CobolSemanticProduct.CallSyntax.IDENTIFIER_OR_EXPRESSION, binding,
                        CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN, PROVENANCE),
                new CobolSemanticProduct.Ordering(movePoint, callPoint),
                new CobolSemanticProduct.AnalysisStatus(
                        CobolSemanticProduct.BindingStatus.COMPLETE,
                        CobolSemanticProduct.AnalysisClaim.PARTIAL,
                        CobolSemanticProduct.DependencyReadiness.INCOMPLETE,
                        CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                        List.of(uncertainty)));
    }

    private static CobolSemanticProduct.DataDeclaration declaration(
            CobolSemanticProduct.DataItemId identity) {
        return new CobolSemanticProduct.DataDeclaration(identity, "WS-PGM", "X(8)", PROVENANCE);
    }

    private static CobolSemanticProduct.Provenance provenance(int line) {
        CobolSemanticProduct.Location location =
                new CobolSemanticProduct.Location("semantic-product.cbl", line, 7, line, 30);
        return new CobolSemanticProduct.Provenance(location, location, List.of(), true);
    }
}
