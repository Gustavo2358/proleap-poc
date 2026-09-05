package io.github.gustavo2358.cobolexplorer.semanticproduct;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Direct boundary-only contract for WORK-SEMANTIC-PRODUCT-002 Checkpoint 2. */
class SemanticProductMoveCallContractTest {
    private static final CobolSemanticProduct.UnitId UNIT =
            new CobolSemanticProduct.UnitId("semantic-product.cbl", List.of(0),
                    "SEMANTIC-PRODUCT");
    private static final CobolSemanticProduct.DataItemId WS_PGM = dataId(17);
    private static final CobolSemanticProduct.DataItemId FLAG = dataId(18);
    private static final CobolSemanticProduct.DataItemId AUX_PGM = dataId(19);
    private static final CobolSemanticProduct.Provenance PROVENANCE = provenance(5);

    @Test
    void pluralStatePublishesNDataAndTypedStatementFacts() {
        CobolSemanticPort port = CobolSemanticPort.open(pluralState());

        assertEquals(List.of("WS-PGM", "FLAG", "AUX-PGM"), port.dataDeclarations().stream()
                .map(CobolSemanticProduct.DataDeclaration::canonicalName).toList());
        assertEquals(9, port.statements().size());
        assertEquals(3, port.moves().size());
        assertEquals(2, port.calls().size());
        assertEquals(3, port.ifs().size());
        assertEquals(1, port.observedStatements().size());
        assertEquals(9, new HashSet<>(port.statements().stream()
                .map(fact -> fact.header().id()).toList()).size());
        assertEquals(9, new HashSet<>(port.statements().stream()
                .map(fact -> fact.header().point()).toList()).size());
        assertTrue(port.statements().stream()
                .allMatch(fact -> fact.header().id().unit().equals(UNIT)));
        assertEquals(List.of(WS_PGM, FLAG, AUX_PGM), port.dataDeclarations().stream()
                .map(CobolSemanticProduct.DataDeclaration::id).toList());
        assertEquals(List.of(AUX_PGM, WS_PGM), port.calls().stream()
                .map(call -> call.operand().binding().selected().orElseThrow()).toList(),
                "CALL facts do not depend on a MOVE pair or common DATA identity");
    }

    @Test
    void structuralIfFactsExposeContainmentNestingEmptyBranchesAndContinuation() {
        CobolSemanticPort port = CobolSemanticPort.open(pluralState());
        CobolSemanticProduct.StatementId outer = statementId(1);
        CobolSemanticProduct.StatementId nested = statementId(3);
        CobolSemanticProduct.StatementId empty = statementId(7);

        assertEquals(List.of(statementId(0), outer, statementId(6), empty, statementId(8)),
                port.rootStatements());
        assertEquals(List.of(statementId(2), nested), ids(port.children(
                outer, CobolSemanticProduct.Branch.THEN)));
        assertEquals(List.of(statementId(5)), ids(port.children(
                outer, CobolSemanticProduct.Branch.ELSE)));
        assertEquals(List.of(statementId(4)), ids(port.children(
                nested, CobolSemanticProduct.Branch.THEN)));
        assertTrue(port.children(nested, CobolSemanticProduct.Branch.ELSE).isEmpty());
        assertTrue(port.children(empty, CobolSemanticProduct.Branch.THEN).isEmpty());
        assertTrue(port.children(empty, CobolSemanticProduct.Branch.ELSE).isEmpty());
        assertEquals(Optional.of(statementId(6)), port.ifs().get(0).continuation());
        assertEquals(Optional.of(statementId(8)), port.ifs().get(2).continuation());
        assertEquals(CobolSemanticProduct.Containment.childOf(
                        outer, CobolSemanticProduct.Branch.THEN),
                port.statement(nested).orElseThrow().header().containment());
    }

    @Test
    void incompleteNominalBindingsKeepOccurrenceIdentityWithoutFabricatingDataIdentity() {
        CobolSemanticPort port = CobolSemanticPort.open(incompleteBindingState());
        List<CobolSemanticProduct.NominalBinding> bindings = port.moves().stream()
                .map(move -> move.target().binding()).toList();

        assertEquals(List.of(CobolSemanticProduct.ResolutionStatus.AMBIGUOUS,
                        CobolSemanticProduct.ResolutionStatus.UNRESOLVED,
                        CobolSemanticProduct.ResolutionStatus.INPUT_MISSING),
                bindings.stream().map(CobolSemanticProduct.NominalBinding::status).toList());
        assertEquals(List.of(WS_PGM, AUX_PGM), bindings.get(0).candidates().stream()
                .map(CobolSemanticProduct.DataCandidate::id).toList());
        assertTrue(bindings.stream().allMatch(binding -> binding.selected().isEmpty()));
        assertEquals(3, new HashSet<>(port.moves().stream()
                .map(move -> move.target().id()).toList()).size(),
                "operand occurrence identity survives incomplete nominal binding");
        assertTrue(port.gaps().stream().allMatch(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.NOMINAL_BINDING));
    }

    @Test
    void observedUnmodeledStatementIsGenericAndKeepsItsGapAndStructure() {
        CobolSemanticPort port = CobolSemanticPort.open(pluralState());
        CobolSemanticProduct.ObservedStatement observed = port.observedStatements().get(0);

        assertEquals("DISPLAY", observed.observedKind());
        assertEquals("DISPLAY_STATEMENT", observed.observedShape());
        assertEquals(CobolSemanticProduct.CoverageStatus.UNSUPPORTED,
                observed.header().coverage());
        assertEquals(CobolSemanticProduct.Containment.root(),
                observed.header().containment());
        assertTrue(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(observed.header().id())
                        && gap.scope() == CobolSemanticProduct.GapScope.CAPABILITY
                        && gap.code().equals(observed.gapCode())));
        assertFalse(Arrays.stream(CobolSemanticProduct.ObservedStatement.class
                        .getRecordComponents()).map(component -> component.getType().getName())
                .anyMatch(type -> type.equals("java.util.Map")));
    }

    @Test
    void coverageIsExactAndAggregateReadinessCannotExceedIndividualFacts() {
        CobolSemanticProduct.State state = pluralState();
        CobolSemanticProduct.CoverageSummary coverage = state.coverage();

        assertEquals(CobolSemanticProduct.InventoryStatus.COMPLETE,
                coverage.inventoryStatus());
        assertEquals(9, coverage.observedStatements());
        assertEquals(5, coverage.modeledStatements());
        assertEquals(3, coverage.partialStatements());
        assertEquals(1, coverage.unsupportedStatements());
        assertEquals(0, coverage.inputMissingStatements());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                coverage.readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                coverage.readiness().cfg().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                coverage.readiness().effectsDataflow().status());

        CobolSemanticProduct.CoverageSummary falseSummary =
                new CobolSemanticProduct.CoverageSummary(
                        CobolSemanticProduct.InventoryStatus.COMPLETE,
                        9, 5, 3, 1, 0,
                        readiness(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                                CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                                CobolSemanticProduct.ReadinessStatus.SUFFICIENT));
        assertThrows(IllegalArgumentException.class, () -> new CobolSemanticProduct.State(
                state.unit(), state.policy(), state.dataDeclarations(), state.statements(),
                state.gaps(), falseSummary));
    }

    @Test
    void zeroFactsIsDistinctFromUnavailableInventory() {
        CobolSemanticProduct.State empty = new CobolSemanticProduct.State(
                UNIT, CobolSemanticProduct.Policy.unspecified(), List.of(), List.of(), List.of(),
                coverage(CobolSemanticProduct.InventoryStatus.COMPLETE,
                        0, 0, 0, 0, 0,
                        readiness(CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE,
                                CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE,
                                CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE)));
        CobolSemanticProduct.State unavailable = new CobolSemanticProduct.State(
                UNIT, CobolSemanticProduct.Policy.unspecified(), List.of(), List.of(), List.of(),
                coverage(CobolSemanticProduct.InventoryStatus.INPUT_MISSING,
                        0, 0, 0, 0, 0, blockedReadiness()));

        assertTrue(empty.statements().isEmpty());
        assertTrue(unavailable.statements().isEmpty());
        assertEquals(CobolSemanticProduct.InventoryStatus.COMPLETE,
                empty.coverage().inventoryStatus());
        assertEquals(CobolSemanticProduct.InventoryStatus.INPUT_MISSING,
                unavailable.coverage().inventoryStatus());
    }

    @Test
    void originalMoveCallExampleRemainsAValidCardinalityOneRegression() {
        CobolSemanticPort port = CobolSemanticPort.open(singleMoveCallState());
        CobolSemanticProduct.MoveFact move = port.moves().get(0);
        CobolSemanticProduct.CallFact call = port.calls().get(0);

        assertEquals(1, port.dataDeclarations().size());
        assertEquals(2, port.statements().size());
        assertEquals(1, port.moves().size());
        assertEquals(1, port.calls().size());
        assertEquals("PGMA", move.source().value());
        assertEquals(Optional.of(WS_PGM), move.target().binding().selected());
        assertEquals(Optional.of(WS_PGM), call.operand().binding().selected());
        assertEquals(CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                call.runtimeTarget());
        assertTrue(port.gaps().stream().anyMatch(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET));
    }

    @Test
    void collectionsAreImmutableAndPortRetainsOnlyTheMaterializedState() {
        List<CobolSemanticProduct.DataDeclaration> declarations =
                new ArrayList<>(pluralState().dataDeclarations());
        List<CobolSemanticProduct.StatementFact> statements =
                new ArrayList<>(pluralState().statements());
        List<CobolSemanticProduct.Gap> gaps = new ArrayList<>(pluralState().gaps());
        CobolSemanticProduct.State state = new CobolSemanticProduct.State(
                UNIT, CobolSemanticProduct.Policy.unspecified(), declarations, statements,
                gaps, pluralState().coverage());
        CobolSemanticPort port = CobolSemanticPort.open(state);
        declarations.clear();
        statements.clear();
        gaps.clear();

        assertEquals(3, state.dataDeclarations().size());
        assertEquals(9, state.statements().size());
        assertThrows(UnsupportedOperationException.class,
                () -> state.dataDeclarations().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.statements().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.gaps().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.moves().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> state.unit().structuralPath().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                state.statements().stream().filter(CobolSemanticProduct.IfFact.class::isInstance)
                        .map(CobolSemanticProduct.IfFact.class::cast).findFirst().orElseThrow()
                        .condition().references().clear());
        assertTrue(Modifier.isFinal(CobolSemanticProduct.State.class.getModifiers()));
        assertEquals(1, port.getClass().getDeclaredFields().length,
                "the facade retains only the immutable state");
    }

    @Test
    void stateAndPortHaveNoSingletonConstructContractOrSpecialMoveCallOrdering() {
        Set<String> stateComponents = new HashSet<>(Arrays.stream(
                CobolSemanticProduct.State.class.getRecordComponents())
                .map(component -> component.getName()).toList());
        Set<String> portMethods = new HashSet<>(Arrays.stream(
                CobolSemanticPort.class.getDeclaredMethods())
                .map(method -> method.getName()).toList());

        assertEquals(Set.of("unit", "policy", "dataDeclarations", "statements",
                "gaps", "coverage"), stateComponents);
        assertTrue(portMethods.containsAll(Set.of("statements", "moves", "calls", "ifs",
                "observedStatements", "rootStatements", "children")));
        assertFalse(portMethods.contains("move"));
        assertFalse(portMethods.contains("call"));
        assertFalse(portMethods.contains("ordering"));
        assertFalse(stateComponents.contains("move"));
        assertFalse(stateComponents.contains("call"));
        assertFalse(stateComponents.contains("ordering"));
    }

    @Test
    void namespaceViolationsFailClosedAndLocalIdsDoNotCrossUnits() {
        CobolSemanticProduct.UnitId otherUnit = new CobolSemanticProduct.UnitId(
                "semantic-product.cbl", List.of(1), "SEMANTIC-PRODUCT");
        CobolSemanticProduct.DataItemId foreign =
                new CobolSemanticProduct.DataItemId(otherUnit, WS_PGM.localId());

        assertNotEquals(WS_PGM, foreign);
        CobolSemanticProduct.StatementId statement = statementId(0);
        CobolSemanticProduct.MoveFact forged = new CobolSemanticProduct.MoveFact(
                header(statement, CobolSemanticProduct.Containment.root(),
                        CobolSemanticProduct.CoverageStatus.MODELED, modeledReadiness()),
                literal(statement, "X"),
                new CobolSemanticProduct.DataReference(operandId(statement, 1),
                        CobolSemanticProduct.OperandRole.WRITE,
                        CobolSemanticProduct.NominalBinding.resolved(foreign, "WS-PGM"),
                        PROVENANCE));
        assertThrows(IllegalArgumentException.class, () -> new CobolSemanticProduct.State(
                UNIT, CobolSemanticProduct.Policy.unspecified(),
                List.of(declaration(WS_PGM, "WS-PGM", "X(8)")),
                List.of(forged), List.of(),
                coverage(CobolSemanticProduct.InventoryStatus.COMPLETE,
                        1, 1, 0, 0, 0, modeledReadiness())));
    }

    private static CobolSemanticProduct.State pluralState() {
        CobolSemanticProduct.StatementId moveRoot = statementId(0);
        CobolSemanticProduct.StatementId outerIf = statementId(1);
        CobolSemanticProduct.StatementId moveThen = statementId(2);
        CobolSemanticProduct.StatementId nestedIf = statementId(3);
        CobolSemanticProduct.StatementId nestedCall = statementId(4);
        CobolSemanticProduct.StatementId moveElse = statementId(5);
        CobolSemanticProduct.StatementId callRoot = statementId(6);
        CobolSemanticProduct.StatementId emptyIf = statementId(7);
        CobolSemanticProduct.StatementId observed = statementId(8);

        List<CobolSemanticProduct.StatementFact> statements = List.of(
                move(moveRoot, "BASE", WS_PGM, CobolSemanticProduct.Containment.root()),
                branch(outerIf, CobolSemanticProduct.Containment.root(), Optional.of(callRoot)),
                move(moveThen, "THEN", WS_PGM, CobolSemanticProduct.Containment.childOf(
                        outerIf, CobolSemanticProduct.Branch.THEN)),
                branch(nestedIf, CobolSemanticProduct.Containment.childOf(
                                outerIf, CobolSemanticProduct.Branch.THEN),
                        Optional.of(callRoot)),
                call(nestedCall, AUX_PGM, CobolSemanticProduct.Containment.childOf(
                        nestedIf, CobolSemanticProduct.Branch.THEN)),
                move(moveElse, "ELSE", AUX_PGM, CobolSemanticProduct.Containment.childOf(
                        outerIf, CobolSemanticProduct.Branch.ELSE)),
                call(callRoot, WS_PGM, CobolSemanticProduct.Containment.root()),
                branch(emptyIf, CobolSemanticProduct.Containment.root(), Optional.of(observed)),
                observed(observed));
        List<CobolSemanticProduct.Gap> gaps = List.of(
                gap(outerIf, CobolSemanticProduct.GapScope.CONDITION_SEMANTICS,
                        "CONDITION_SEMANTICS_NOT_AVAILABLE"),
                gap(nestedIf, CobolSemanticProduct.GapScope.CONDITION_SEMANTICS,
                        "CONDITION_SEMANTICS_NOT_AVAILABLE"),
                gap(nestedCall, CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET,
                        "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"),
                gap(callRoot, CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET,
                        "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"),
                gap(emptyIf, CobolSemanticProduct.GapScope.CONDITION_SEMANTICS,
                        "CONDITION_SEMANTICS_NOT_AVAILABLE"),
                gap(observed, CobolSemanticProduct.GapScope.CAPABILITY,
                        "DISPLAY_OUTSIDE_INITIAL_CAPABILITY"));
        return new CobolSemanticProduct.State(UNIT, CobolSemanticProduct.Policy.unspecified(),
                List.of(declaration(WS_PGM, "WS-PGM", "X(8)"),
                        declaration(FLAG, "FLAG", "9"),
                        declaration(AUX_PGM, "AUX-PGM", "X(8)")),
                statements, gaps,
                coverage(CobolSemanticProduct.InventoryStatus.COMPLETE,
                        9, 5, 3, 1, 0, blockedReadiness()));
    }

    private static CobolSemanticProduct.State incompleteBindingState() {
        CobolSemanticProduct.StatementId ambiguousId = statementId(0);
        CobolSemanticProduct.StatementId unresolvedId = statementId(1);
        CobolSemanticProduct.StatementId missingId = statementId(2);
        CobolSemanticProduct.NominalBinding ambiguous =
                CobolSemanticProduct.NominalBinding.incomplete(
                        CobolSemanticProduct.ResolutionStatus.AMBIGUOUS,
                        CobolSemanticProduct.ResolutionReason.MULTIPLE_VALID_CANDIDATES,
                        List.of(new CobolSemanticProduct.DataCandidate(WS_PGM, "WS-PGM"),
                                new CobolSemanticProduct.DataCandidate(AUX_PGM, "AUX-PGM")));
        CobolSemanticProduct.NominalBinding unresolved =
                CobolSemanticProduct.NominalBinding.incomplete(
                        CobolSemanticProduct.ResolutionStatus.UNRESOLVED,
                        CobolSemanticProduct.ResolutionReason.DECLARATION_NOT_FOUND, List.of());
        CobolSemanticProduct.NominalBinding inputMissing =
                CobolSemanticProduct.NominalBinding.incomplete(
                        CobolSemanticProduct.ResolutionStatus.INPUT_MISSING,
                        CobolSemanticProduct.ResolutionReason.INPUT_INCOMPLETE, List.of());
        List<CobolSemanticProduct.StatementFact> statements = List.of(
                move(ambiguousId, "A", ambiguous, CobolSemanticProduct.CoverageStatus.PARTIAL),
                move(unresolvedId, "B", unresolved, CobolSemanticProduct.CoverageStatus.PARTIAL),
                move(missingId, "C", inputMissing,
                        CobolSemanticProduct.CoverageStatus.INPUT_MISSING));
        List<CobolSemanticProduct.Gap> gaps = List.of(
                gap(ambiguousId, CobolSemanticProduct.GapScope.NOMINAL_BINDING,
                        "AMBIGUOUS_DATA_BINDING"),
                gap(unresolvedId, CobolSemanticProduct.GapScope.NOMINAL_BINDING,
                        "UNRESOLVED_DATA_BINDING"),
                gap(missingId, CobolSemanticProduct.GapScope.NOMINAL_BINDING,
                        "DATA_BINDING_INPUT_MISSING"));
        return new CobolSemanticProduct.State(UNIT, CobolSemanticProduct.Policy.unspecified(),
                List.of(declaration(WS_PGM, "WS-PGM", "X(8)"),
                        declaration(AUX_PGM, "AUX-PGM", "X(8)")),
                statements, gaps,
                coverage(CobolSemanticProduct.InventoryStatus.COMPLETE,
                        3, 0, 2, 0, 1, blockedReadiness()));
    }

    private static CobolSemanticProduct.State singleMoveCallState() {
        CobolSemanticProduct.StatementId move = statementId(0);
        CobolSemanticProduct.StatementId call = statementId(1);
        return new CobolSemanticProduct.State(UNIT, CobolSemanticProduct.Policy.unspecified(),
                List.of(declaration(WS_PGM, "WS-PGM", "X(8)")),
                List.of(move(move, "PGMA", WS_PGM, CobolSemanticProduct.Containment.root()),
                        call(call, WS_PGM, CobolSemanticProduct.Containment.root())),
                List.of(gap(call, CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET,
                        "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN")),
                coverage(CobolSemanticProduct.InventoryStatus.COMPLETE,
                        2, 2, 0, 0, 0, modeledReadiness()));
    }

    private static CobolSemanticProduct.MoveFact move(
            CobolSemanticProduct.StatementId id, String value,
            CobolSemanticProduct.DataItemId target,
            CobolSemanticProduct.Containment containment) {
        return new CobolSemanticProduct.MoveFact(
                header(id, containment, CobolSemanticProduct.CoverageStatus.MODELED,
                        modeledReadiness()), literal(id, value),
                reference(id, 1, target, CobolSemanticProduct.OperandRole.WRITE));
    }

    private static CobolSemanticProduct.MoveFact move(
            CobolSemanticProduct.StatementId id, String value,
            CobolSemanticProduct.NominalBinding binding,
            CobolSemanticProduct.CoverageStatus coverage) {
        return new CobolSemanticProduct.MoveFact(
                header(id, CobolSemanticProduct.Containment.root(), coverage,
                        blockedReadiness()), literal(id, value),
                new CobolSemanticProduct.DataReference(operandId(id, 1),
                        CobolSemanticProduct.OperandRole.WRITE, binding, PROVENANCE));
    }

    private static CobolSemanticProduct.CallFact call(
            CobolSemanticProduct.StatementId id, CobolSemanticProduct.DataItemId operand,
            CobolSemanticProduct.Containment containment) {
        return new CobolSemanticProduct.CallFact(
                header(id, containment, CobolSemanticProduct.CoverageStatus.MODELED,
                        modeledReadiness()),
                CobolSemanticProduct.CallSyntax.IDENTIFIER_OR_EXPRESSION,
                reference(id, 0, operand, CobolSemanticProduct.OperandRole.CALL_TARGET),
                CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN");
    }

    private static CobolSemanticProduct.IfFact branch(
            CobolSemanticProduct.StatementId id,
            CobolSemanticProduct.Containment containment,
            Optional<CobolSemanticProduct.StatementId> continuation) {
        return new CobolSemanticProduct.IfFact(
                header(id, containment, CobolSemanticProduct.CoverageStatus.PARTIAL,
                        readiness(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                                CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                                CobolSemanticProduct.ReadinessStatus.PARTIAL)),
                new CobolSemanticProduct.ConditionSurface("RELATION",
                        List.of(reference(id, 0, FLAG, CobolSemanticProduct.OperandRole.READ)),
                        PROVENANCE), continuation);
    }

    private static CobolSemanticProduct.ObservedStatement observed(
            CobolSemanticProduct.StatementId id) {
        return new CobolSemanticProduct.ObservedStatement(
                header(id, CobolSemanticProduct.Containment.root(),
                        CobolSemanticProduct.CoverageStatus.UNSUPPORTED, blockedReadiness()),
                "DISPLAY", "DISPLAY_STATEMENT", "DISPLAY_OUTSIDE_INITIAL_CAPABILITY");
    }

    private static CobolSemanticProduct.StatementHeader header(
            CobolSemanticProduct.StatementId id,
            CobolSemanticProduct.Containment containment,
            CobolSemanticProduct.CoverageStatus coverage,
            CobolSemanticProduct.Readiness readiness) {
        return new CobolSemanticProduct.StatementHeader(id,
                new CobolSemanticProduct.ProgramPoint(id.localId()), containment,
                PROVENANCE, coverage, readiness);
    }

    private static CobolSemanticProduct.LiteralSource literal(
            CobolSemanticProduct.StatementId id, String value) {
        return new CobolSemanticProduct.LiteralSource(operandId(id, 0),
                value, PROVENANCE);
    }

    private static CobolSemanticProduct.DataReference reference(
            CobolSemanticProduct.StatementId statement, int operand,
            CobolSemanticProduct.DataItemId data, CobolSemanticProduct.OperandRole role) {
        String name = data.equals(WS_PGM) ? "WS-PGM"
                : data.equals(FLAG) ? "FLAG" : "AUX-PGM";
        return new CobolSemanticProduct.DataReference(operandId(statement, operand), role,
                CobolSemanticProduct.NominalBinding.resolved(data, name), PROVENANCE);
    }

    private static CobolSemanticProduct.DataDeclaration declaration(
            CobolSemanticProduct.DataItemId id, String name, String picture) {
        return new CobolSemanticProduct.DataDeclaration(id, name, Optional.of(picture),
                PROVENANCE, CobolSemanticProduct.CoverageStatus.MODELED,
                readiness(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                        CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE,
                        CobolSemanticProduct.ReadinessStatus.PARTIAL));
    }

    private static CobolSemanticProduct.Gap gap(
            CobolSemanticProduct.StatementId statement, CobolSemanticProduct.GapScope scope,
            String code) {
        return new CobolSemanticProduct.Gap(statement, scope, code,
                "boundary-only test preserves this unavailable semantic dimension", PROVENANCE);
    }

    private static CobolSemanticProduct.CoverageSummary coverage(
            CobolSemanticProduct.InventoryStatus inventory, int observed, int modeled,
            int partial, int unsupported, int inputMissing,
            CobolSemanticProduct.Readiness readiness) {
        return new CobolSemanticProduct.CoverageSummary(inventory, observed, modeled,
                partial, unsupported, inputMissing, readiness);
    }

    private static CobolSemanticProduct.Readiness modeledReadiness() {
        return readiness(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                CobolSemanticProduct.ReadinessStatus.PARTIAL);
    }

    private static CobolSemanticProduct.Readiness blockedReadiness() {
        return readiness(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                CobolSemanticProduct.ReadinessStatus.BLOCKED,
                CobolSemanticProduct.ReadinessStatus.BLOCKED);
    }

    private static CobolSemanticProduct.Readiness readiness(
            CobolSemanticProduct.ReadinessStatus lowering,
            CobolSemanticProduct.ReadinessStatus cfg,
            CobolSemanticProduct.ReadinessStatus effects) {
        return new CobolSemanticProduct.Readiness(
                new CobolSemanticProduct.ReadinessClaim(lowering, "lowering claim"),
                new CobolSemanticProduct.ReadinessClaim(cfg, "CFG input claim"),
                new CobolSemanticProduct.ReadinessClaim(effects,
                        "effects and dataflow input claim"));
    }

    private static List<CobolSemanticProduct.StatementId> ids(
            List<CobolSemanticProduct.StatementFact> statements) {
        return statements.stream().map(statement -> statement.header().id()).toList();
    }

    private static CobolSemanticProduct.DataItemId dataId(int localId) {
        return new CobolSemanticProduct.DataItemId(UNIT, localId);
    }

    private static CobolSemanticProduct.StatementId statementId(int localId) {
        return new CobolSemanticProduct.StatementId(UNIT, localId);
    }

    private static CobolSemanticProduct.OperandId operandId(
            CobolSemanticProduct.StatementId statement, int localId) {
        return new CobolSemanticProduct.OperandId(statement, localId);
    }

    private static CobolSemanticProduct.Provenance provenance(int line) {
        CobolSemanticProduct.Location location =
                new CobolSemanticProduct.Location("semantic-product.cbl", line, 7, line, 30);
        return new CobolSemanticProduct.Provenance(location, location, List.of(), true);
    }
}
