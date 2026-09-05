package io.github.gustavo2358.cobolexplorer.semanticproduct;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void literalKindDistinguishesNumericFromAlphanumericAfterTheBoundary() {
        CobolSemanticProduct.StatementId numericMove = statementId(20);
        CobolSemanticProduct.StatementId alphanumericMove = statementId(21);
        CobolSemanticProduct.State state = new CobolSemanticProduct.State(
                UNIT, CobolSemanticProduct.Policy.unspecified(),
                List.of(declaration(WS_PGM, "WS-PGM", "X(8)")),
                List.of(
                        move(numericMove, CobolSemanticProduct.LiteralKind.NUMERIC,
                                "1", WS_PGM, CobolSemanticProduct.Containment.root()),
                        move(alphanumericMove, CobolSemanticProduct.LiteralKind.ALPHANUMERIC,
                                "1", WS_PGM, CobolSemanticProduct.Containment.root())),
                List.of(),
                coverage(CobolSemanticProduct.InventoryStatus.COMPLETE,
                        2, 2, 0, 0, 0, modeledReadiness()));

        List<CobolSemanticProduct.LiteralSource> sources = CobolSemanticPort.open(state)
                .moves().stream().map(CobolSemanticProduct.MoveFact::source).toList();

        assertEquals(List.of("1", "1"), sources.stream()
                .map(CobolSemanticProduct.LiteralSource::value).toList());
        assertEquals(List.of(CobolSemanticProduct.LiteralKind.NUMERIC,
                        CobolSemanticProduct.LiteralKind.ALPHANUMERIC),
                sources.stream().map(CobolSemanticProduct.LiteralSource::kind).toList());
        assertNotEquals(sources.get(0).kind(), sources.get(1).kind(),
                "equal normalized values must retain distinct literal semantics");
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
    void indexedQueriesPreserveTheStateFactsIdentityAndStructuralOrder() {
        CobolSemanticProduct.State state = pluralState();
        CobolSemanticPort port = CobolSemanticPort.open(state);
        List<CobolSemanticProduct.StatementFact> expectedMoves = state.statements().stream()
                .filter(CobolSemanticProduct.MoveFact.class::isInstance).toList();
        List<CobolSemanticProduct.StatementFact> expectedCalls = state.statements().stream()
                .filter(CobolSemanticProduct.CallFact.class::isInstance).toList();
        List<CobolSemanticProduct.StatementFact> expectedIfs = state.statements().stream()
                .filter(CobolSemanticProduct.IfFact.class::isInstance).toList();
        List<CobolSemanticProduct.StatementFact> expectedObserved = state.statements().stream()
                .filter(CobolSemanticProduct.ObservedStatement.class::isInstance).toList();
        List<CobolSemanticProduct.StatementId> expectedRoots = state.statements().stream()
                .filter(statement -> statement.header().containment()
                        .equals(CobolSemanticProduct.Containment.root()))
                .map(statement -> statement.header().id()).toList();

        assertSame(state.unit(), port.unit());
        assertSame(state.policy(), port.policy());
        assertSame(state.dataDeclarations(), port.dataDeclarations());
        assertSame(state.statements(), port.statements());
        assertSame(state.gaps(), port.gaps());
        assertSame(state.coverage(), port.coverage());
        assertEquals(expectedRoots, port.rootStatements());
        assertSameElements(expectedRoots, port.rootStatements());
        assertSameElements(expectedMoves, port.moves());
        assertSameElements(expectedCalls, port.calls());
        assertSameElements(expectedIfs, port.ifs());
        assertSameElements(expectedObserved, port.observedStatements());

        for (CobolSemanticProduct.StatementFact fact : state.statements())
            assertSame(fact, port.statement(fact.header().id()).orElseThrow());
        assertEquals(Optional.empty(), port.statement(statementId(100)));

        CobolSemanticProduct.StatementId outer = statementId(1);
        List<CobolSemanticProduct.StatementFact> thenChildren = port.children(
                outer, CobolSemanticProduct.Branch.THEN);
        assertSameElements(List.of(state.statements().get(2), state.statements().get(3)),
                thenChildren);
        assertSame(thenChildren, port.children(outer, CobolSemanticProduct.Branch.THEN));
        assertSame(port.rootStatements(), port.rootStatements());
        assertSame(port.moves(), port.moves());
        assertSame(port.calls(), port.calls());
        assertSame(port.ifs(), port.ifs());
        assertSame(port.observedStatements(), port.observedStatements());
    }

    @Test
    void equivalentStatesProduceDeterministicIndexedViewsRegardlessOfQueryOrder() {
        CobolSemanticPort first = CobolSemanticPort.open(pluralState());
        CobolSemanticPort second = CobolSemanticPort.open(pluralState());

        first.observedStatements();
        first.children(statementId(1), CobolSemanticProduct.Branch.ELSE);
        first.moves();
        second.ifs();
        second.rootStatements();
        second.calls();

        assertEquals(first.statements(), second.statements());
        assertEquals(first.rootStatements(), second.rootStatements());
        assertEquals(first.moves(), second.moves());
        assertEquals(first.calls(), second.calls());
        assertEquals(first.ifs(), second.ifs());
        assertEquals(first.observedStatements(), second.observedStatements());
        for (CobolSemanticProduct.StatementId parent : List.of(statementId(1), statementId(3),
                statementId(7))) {
            assertEquals(first.children(parent, CobolSemanticProduct.Branch.THEN),
                    second.children(parent, CobolSemanticProduct.Branch.THEN));
            assertEquals(first.children(parent, CobolSemanticProduct.Branch.ELSE),
                    second.children(parent, CobolSemanticProduct.Branch.ELSE));
        }
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
    void collectionsAndIndexedViewsAreImmutableAndRetainOnlyStateDerivedData()
            throws ReflectiveOperationException {
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
        assertThrows(UnsupportedOperationException.class, () -> port.dataDeclarations().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.statements().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.gaps().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.rootStatements().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.moves().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.calls().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.ifs().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> port.observedStatements().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.children(
                statementId(1), CobolSemanticProduct.Branch.THEN).clear());
        assertThrows(UnsupportedOperationException.class, () -> port.children(
                statementId(3), CobolSemanticProduct.Branch.ELSE).clear());
        assertThrows(UnsupportedOperationException.class,
                () -> state.unit().structuralPath().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                state.statements().stream().filter(CobolSemanticProduct.IfFact.class::isInstance)
                        .map(CobolSemanticProduct.IfFact.class::cast).findFirst().orElseThrow()
                        .condition().references().clear());
        assertTrue(Modifier.isFinal(CobolSemanticProduct.State.class.getModifiers()));

        List<Field> retainedFields = Arrays.stream(port.getClass().getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers())).toList();
        assertTrue(retainedFields.size() > 1, "the materialized port must build eager indexes");
        assertTrue(retainedFields.stream().allMatch(field ->
                Modifier.isFinal(field.getModifiers())));
        int retainedStates = 0;
        for (Field field : retainedFields) {
            field.setAccessible(true);
            Object retained = field.get(port);
            assertTrue(retained != null, () -> field.getName() + " must be built eagerly");
            if (retained == state) {
                retainedStates++;
            } else {
                assertTrue(retained instanceof List<?> || retained instanceof Map<?, ?>,
                        () -> field.getName()
                                + " must contain only collections derived from State");
                assertDerivedOnlyFromState(retained, state);
            }
        }
        assertEquals(1, retainedStates,
                "the only directly retained publication is the materialized State");
    }

    @Test
    void materializedPortIndexesScaleLinearly()
            throws ReflectiveOperationException {
        int branches = 1_024;
        CobolSemanticProduct.State state = indexedScaleState(branches);
        CobolSemanticPort port = CobolSemanticPort.open(state);

        Map<?, ?> statementsById = (Map<?, ?>) retainedField(port, "statementById");
        Map<?, ?> childrenByContainment =
                (Map<?, ?>) retainedField(port, "childrenByContainment");
        List<?> roots = (List<?>) retainedField(port, "rootStatements");
        List<?> moves = (List<?>) retainedField(port, "moves");
        List<?> calls = (List<?>) retainedField(port, "calls");
        List<?> ifs = (List<?>) retainedField(port, "ifs");
        List<?> observed = (List<?>) retainedField(port, "observedStatements");
        int statementCount = branches * 2;
        long indexedChildren = childrenByContainment.values().stream()
                .map(List.class::cast).mapToLong(List::size).sum();
        long indexedReferences = statementsById.size() + indexedChildren + roots.size()
                + moves.size() + calls.size() + ifs.size() + observed.size();

        assertEquals(statementCount, statementsById.size());
        assertEquals(branches, childrenByContainment.size());
        assertEquals(branches, indexedChildren);
        assertEquals(branches, roots.size());
        assertEquals(branches, moves.size());
        assertEquals(branches, ifs.size());
        assertTrue(calls.isEmpty());
        assertTrue(observed.isEmpty());
        assertTrue(indexedReferences <= statementCount * 4L,
                "derived indexes must retain only O(N) references");
        assertSame(port.rootStatements(), port.rootStatements());
        assertSame(port.moves(), port.moves());
        assertSame(port.ifs(), port.ifs());
        assertSame(state.statements().get(statementCount - 1),
                port.statement(statementId(statementCount - 1)).orElseThrow());
        assertSame(port.children(statementId(0), CobolSemanticProduct.Branch.THEN),
                port.children(statementId(0), CobolSemanticProduct.Branch.THEN));
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

    private static CobolSemanticProduct.State indexedScaleState(int branches) {
        List<CobolSemanticProduct.StatementFact> statements =
                new ArrayList<>(branches * 2);
        List<CobolSemanticProduct.Gap> gaps = new ArrayList<>(branches);
        for (int index = 0; index < branches; index++) {
            CobolSemanticProduct.StatementId parent = statementId(index * 2);
            CobolSemanticProduct.StatementId child = statementId(index * 2 + 1);
            statements.add(branch(parent, CobolSemanticProduct.Containment.root(),
                    Optional.empty()));
            statements.add(move(child, "VALUE-" + index, WS_PGM,
                    CobolSemanticProduct.Containment.childOf(
                            parent, CobolSemanticProduct.Branch.THEN)));
            gaps.add(gap(parent, CobolSemanticProduct.GapScope.CONDITION_SEMANTICS,
                    "CONDITION_SEMANTICS_NOT_AVAILABLE"));
        }
        return new CobolSemanticProduct.State(UNIT, CobolSemanticProduct.Policy.unspecified(),
                List.of(declaration(WS_PGM, "WS-PGM", "X(8)"),
                        declaration(FLAG, "FLAG", "9")),
                statements, gaps,
                coverage(CobolSemanticProduct.InventoryStatus.COMPLETE,
                        branches * 2, branches, branches, 0, 0, blockedReadiness()));
    }

    private static Object retainedField(CobolSemanticPort port, String name)
            throws ReflectiveOperationException {
        Field field = port.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(port);
    }

    private static void assertDerivedOnlyFromState(
            Object derived, CobolSemanticProduct.State state) {
        if (derived instanceof List<?> list) {
            assertThrows(UnsupportedOperationException.class, list::clear);
            list.forEach(element -> assertDerivedOnlyFromState(element, state));
            return;
        }
        if (derived instanceof Map<?, ?> map) {
            assertThrows(UnsupportedOperationException.class, map::clear);
            map.forEach((key, value) -> {
                assertDerivedOnlyFromState(key, state);
                assertDerivedOnlyFromState(value, state);
            });
            return;
        }
        if (derived instanceof CobolSemanticProduct.StatementFact) {
            assertTrue(state.statements().stream().anyMatch(fact -> fact == derived),
                    "an index must reference a fact already materialized in State");
            return;
        }
        if (derived instanceof CobolSemanticProduct.StatementId) {
            assertTrue(state.statements().stream()
                            .anyMatch(fact -> fact.header().id() == derived),
                    "a derived root/map key must reuse a statement identity from State");
            return;
        }
        if (derived instanceof CobolSemanticProduct.Containment) {
            assertTrue(state.statements().stream()
                            .anyMatch(fact -> fact.header().containment() == derived),
                    "a containment index key must originate in State");
            return;
        }
        assertTrue(false, () -> "unexpected retained index value: "
                + derived.getClass().getName());
    }

    private static void assertSameElements(List<?> expected, List<?> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++)
            assertSame(expected.get(index), actual.get(index));
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
        return move(id, CobolSemanticProduct.LiteralKind.ALPHANUMERIC,
                value, target, containment);
    }

    private static CobolSemanticProduct.MoveFact move(
            CobolSemanticProduct.StatementId id,
            CobolSemanticProduct.LiteralKind literalKind, String value,
            CobolSemanticProduct.DataItemId target,
            CobolSemanticProduct.Containment containment) {
        return new CobolSemanticProduct.MoveFact(
                header(id, containment, CobolSemanticProduct.CoverageStatus.MODELED,
                        modeledReadiness()), literal(id, literalKind, value),
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
        return literal(id, CobolSemanticProduct.LiteralKind.ALPHANUMERIC, value);
    }

    private static CobolSemanticProduct.LiteralSource literal(
            CobolSemanticProduct.StatementId id,
            CobolSemanticProduct.LiteralKind kind, String value) {
        return new CobolSemanticProduct.LiteralSource(operandId(id, 0),
                kind, value, PROVENANCE);
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
