package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.loweringreadiness.SemanticPortLoweringProbe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Final boundary falsification and handoff oracle for Checkpoint 8. */
class SemanticProductCheckpoint8LoweringReadinessTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/semantic/semantic-product-lowering-readiness.cbl");

    @Test
    void portAloneReconstructsTheDeclaredSliceWithoutPromotingUnknownSemantics()
            throws IOException {
        CobolSemanticPort port = publishFixtureAndReleaseFrontend();

        SemanticPortLoweringProbe.Reconstruction result =
                SemanticPortLoweringProbe.reconstruct(port);

        assertTrue(result.valid(), () -> "unexpected boundary violations: "
                + result.violations());
        assertEquals(CobolSemanticProduct.InventoryStatus.COMPLETE,
                result.coverage().inventoryStatus());
        assertEquals(14, result.statements().size());
        assertEquals(IntStream.range(0, 14).boxed().toList(), result.statements().stream()
                .map(statement -> statement.anchor().programPoint()).toList());
        assertEquals(14, new HashSet<>(result.statements().stream()
                .map(statement -> statement.anchor().id()).toList()).size());
        assertTrue(result.statements().stream().allMatch(statement ->
                statement.anchor().id().unit().equals(result.unit())
                        && statement.anchor().provenance().exact()));
        assertEquals(List.of("WS-X", "FLAG", "AUX-PGM"), result.data().stream()
                .map(SemanticPortLoweringProbe.DataNode::canonicalName).toList());
        assertTrue(result.data().stream().allMatch(data ->
                data.readiness().lowering().status()
                        == CobolSemanticProduct.ReadinessStatus.SUFFICIENT));

        SemanticPortLoweringProbe.MoveNode initialMove = exactlyOne(result.statements(
                SemanticPortLoweringProbe.MoveNode.class).stream()
                .filter(move -> move.source().value().equals("A"))
                .filter(move -> move.target().selectedName().equals(Optional.of("WS-X")))
                .toList());
        SemanticPortLoweringProbe.MoveNode thenMove = exactlyOne(result.statements(
                SemanticPortLoweringProbe.MoveNode.class).stream()
                .filter(move -> move.source().value().equals("B"))
                .filter(move -> move.target().selectedName().equals(Optional.of("WS-X")))
                .toList());
        SemanticPortLoweringProbe.MoveNode elseMove = exactlyOne(result.statements(
                SemanticPortLoweringProbe.MoveNode.class).stream()
                .filter(move -> move.source().value().equals("C"))
                .filter(move -> move.target().selectedName().equals(Optional.of("WS-X")))
                .toList());
        assertEquals(CobolSemanticProduct.LiteralKind.ALPHANUMERIC, initialMove.source().kind());
        assertEquals(CobolSemanticProduct.OperandRole.WRITE, initialMove.target().role());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                initialMove.anchor().readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                initialMove.anchor().readiness().cfg().status());

        SemanticPortLoweringProbe.IfNode branch = exactlyOne(result.statements(
                SemanticPortLoweringProbe.IfNode.class).stream()
                .filter(candidate -> candidate.thenMembers().contains(thenMove.anchor().id()))
                .filter(candidate -> candidate.elseMembers().contains(elseMove.anchor().id()))
                .toList());
        assertEquals("RELATION", branch.condition().shape());
        assertEquals(List.of("FLAG"), branch.condition().references().stream()
                .map(reference -> reference.selectedName().orElseThrow()).toList());
        assertEquals(SemanticPortLoweringProbe.PredicateKnowledge.NOT_PUBLISHED,
                branch.condition().predicateKnowledge(),
                "the probe must not invent the missing '= 1' predicate semantics");
        assertEquals(CobolSemanticProduct.ReadinessStatus.PARTIAL,
                branch.anchor().readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                branch.anchor().readiness().cfg().status());

        SemanticPortLoweringProbe.CallNode continuation = exactlyOne(result.statements(
                SemanticPortLoweringProbe.CallNode.class).stream()
                .filter(call -> call.anchor().id().equals(branch.continuation().orElseThrow()))
                .toList());
        assertEquals(Optional.of("WS-X"), continuation.operand().selectedName());
        assertEquals(CobolSemanticProduct.OperandRole.CALL_TARGET,
                continuation.operand().role());
        assertEquals(CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                continuation.runtimeTarget());
        assertEquals("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN",
                continuation.runtimeUncertaintyCode());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                continuation.anchor().readiness().lowering().status());
        assertTrue(initialMove.anchor().programPoint() < branch.anchor().programPoint());
        assertTrue(branch.anchor().programPoint() < continuation.anchor().programPoint());

        assertTrue(result.statements().stream()
                .filter(statement -> statement.anchor().readiness().lowering().status()
                        == CobolSemanticProduct.ReadinessStatus.SUFFICIENT)
                .allMatch(statement -> statement instanceof SemanticPortLoweringProbe.CallNode
                        || statement instanceof SemanticPortLoweringProbe.MoveNode),
                "only proven CALL and scalar MOVE source facts claim sufficient lowering input");
        assertEquals(1, result.statements(
                SemanticPortLoweringProbe.ObservedNode.class).size());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                result.coverage().readiness().lowering().status());
    }

    @Test
    void probePublicEntryReceivesOnlyTheSemanticPort() throws ReflectiveOperationException {
        var method = SemanticPortLoweringProbe.class.getDeclaredMethod(
                "reconstruct", CobolSemanticPort.class);

        assertEquals(List.of(CobolSemanticPort.class),
                List.of(method.getParameterTypes()));
    }

    @Test
    void falsificationARejectsMissingIfBranchMembership() throws IOException {
        CobolSemanticPort port = publishFixtureAndReleaseFrontend();
        CobolSemanticProduct.IfFact outer = port.ifs().stream()
                .filter(branch -> port.children(branch.header().id(),
                        CobolSemanticProduct.Branch.ELSE).stream()
                        .anyMatch(CobolSemanticProduct.MoveFact.class::isInstance))
                .findFirst().orElseThrow();
        CobolSemanticProduct.StatementId hidden = port.children(outer.header().id(),
                CobolSemanticProduct.Branch.THEN).get(0).header().id();
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                outer.header().readiness().cfg().status());
        CobolSemanticPort mutated = MutatedPort.hideBranchMember(
                port, outer.header().id(), CobolSemanticProduct.Branch.THEN, hidden);

        SemanticPortLoweringProbe.Reconstruction result =
                SemanticPortLoweringProbe.reconstruct(mutated);

        assertViolation(result, "BRANCH_MEMBERSHIP_MISMATCH");
    }

    @Test
    void falsificationBRejectsAReadyCallWithDegradedNominalBinding() throws IOException {
        CobolSemanticPort port = publishFixtureAndReleaseFrontend();
        CobolSemanticProduct.CallFact call = port.calls().stream()
                .filter(candidate -> candidate.header().readiness().lowering().status()
                        == CobolSemanticProduct.ReadinessStatus.SUFFICIENT)
                .findFirst().orElseThrow();
        assertEquals(CobolSemanticProduct.ResolutionStatus.RESOLVED,
                ((CobolSemanticProduct.DataReference) call.target()).binding().status());
        // The strengthened boundary constructor now rejects the mutation before the port probe.
        assertThrows(IllegalArgumentException.class,
                () -> MutatedPort.degradeCallBinding(port, call.header().id()));
    }

    @Test
    void falsificationCRejectsAnObservedStatementWhoseUnknownGapWasHidden()
            throws IOException {
        CobolSemanticPort port = publishFixtureAndReleaseFrontend();
        CobolSemanticProduct.ObservedStatement observed =
                port.observedStatements().get(0);
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                observed.header().readiness().lowering().status());
        CobolSemanticPort mutated = MutatedPort.hideGap(
                port, observed.header().id(), observed.gapCode());

        SemanticPortLoweringProbe.Reconstruction result =
                SemanticPortLoweringProbe.reconstruct(mutated);

        assertViolation(result, "OBSERVED_WITHOUT_CAPABILITY_GAP");
    }

    @Test
    void falsificationDRejectsAHiddenObservedFactDespiteConsistentPortIndexes()
            throws IOException {
        CobolSemanticPort port = publishFixtureAndReleaseFrontend();
        CobolSemanticProduct.ObservedStatement observed =
                port.observedStatements().get(0);
        assertEquals(14, port.coverage().observedStatements());
        CobolSemanticPort mutated = MutatedPort.hideStatement(
                port, observed.header().id());

        SemanticPortLoweringProbe.Reconstruction result =
                SemanticPortLoweringProbe.reconstruct(mutated);

        assertViolation(result, "COVERAGE_STATEMENT_COUNT_MISMATCH");
    }

    private static CobolSemanticPort publishFixtureAndReleaseFrontend() throws IOException {
        AstBoundaryTestSupport.Analysis frontend = AstBoundaryTestSupport.analyze(
                Files.readString(FIXTURE, StandardCharsets.UTF_8), FIXTURE.getFileName().toString());
        ResolutionContracts.ProgramUnitId unitId = frontend.model().programUnits().get(0).id();
        return ExplorerMain.publishSemanticProduct(unitId, frontend.build(), frontend.tables(),
                frontend.occurrences(), frontend.resolution(), frontend.report());
    }

    private static void assertViolation(
            SemanticPortLoweringProbe.Reconstruction reconstruction,
            String expectedCode) {
        assertFalse(reconstruction.valid());
        assertTrue(reconstruction.violations().stream()
                        .anyMatch(violation -> violation.code().equals(expectedCode)),
                () -> "missing " + expectedCode + " in " + reconstruction.violations());
    }

    private static <T> T exactlyOne(List<T> values) {
        assertEquals(1, values.size());
        return values.get(0);
    }

    /** Deliberately inconsistent test double; no mutated production state is persisted. */
    private static final class MutatedPort implements CobolSemanticPort {
        private final CobolSemanticPort delegate;
        private final List<CobolSemanticProduct.StatementFact> statements;
        private final List<CobolSemanticProduct.Gap> gaps;
        private final Optional<HiddenMembership> hiddenMembership;

        private MutatedPort(
                CobolSemanticPort delegate,
                List<CobolSemanticProduct.StatementFact> statements,
                List<CobolSemanticProduct.Gap> gaps,
                Optional<HiddenMembership> hiddenMembership) {
            this.delegate = delegate;
            this.statements = List.copyOf(statements);
            this.gaps = List.copyOf(gaps);
            this.hiddenMembership = hiddenMembership;
        }

        static CobolSemanticPort hideBranchMember(
                CobolSemanticPort delegate,
                CobolSemanticProduct.StatementId parent,
                CobolSemanticProduct.Branch branch,
                CobolSemanticProduct.StatementId child) {
            return new MutatedPort(delegate, delegate.statements(), delegate.gaps(),
                    Optional.of(new HiddenMembership(parent, branch, child)));
        }

        static CobolSemanticPort degradeCallBinding(
                CobolSemanticPort delegate,
                CobolSemanticProduct.StatementId statement) {
            List<CobolSemanticProduct.StatementFact> facts = delegate.statements().stream()
                    .map(fact -> fact.header().id().equals(statement)
                            ? degradedCall((CobolSemanticProduct.CallFact) fact) : fact)
                    .toList();
            return new MutatedPort(delegate, facts, delegate.gaps(), Optional.empty());
        }

        static CobolSemanticPort hideGap(
                CobolSemanticPort delegate,
                CobolSemanticProduct.StatementId statement,
                String code) {
            List<CobolSemanticProduct.Gap> gaps = delegate.gaps().stream()
                    .filter(gap -> !gap.statement().equals(statement)
                            || !gap.code().equals(code))
                    .toList();
            return new MutatedPort(delegate, delegate.statements(), gaps, Optional.empty());
        }

        static CobolSemanticPort hideStatement(
                CobolSemanticPort delegate,
                CobolSemanticProduct.StatementId statement) {
            List<CobolSemanticProduct.StatementFact> facts = delegate.statements().stream()
                    .filter(fact -> !fact.header().id().equals(statement)).toList();
            List<CobolSemanticProduct.Gap> gaps = delegate.gaps().stream()
                    .filter(gap -> !gap.statement().equals(statement)).toList();
            return new MutatedPort(delegate, facts, gaps, Optional.empty());
        }

        private static CobolSemanticProduct.CallFact degradedCall(
                CobolSemanticProduct.CallFact call) {
            CobolSemanticProduct.DataReference operand = new CobolSemanticProduct.DataReference(
                    ((CobolSemanticProduct.DataReference) call.target()).id(), ((CobolSemanticProduct.DataReference) call.target()).role(),
                    CobolSemanticProduct.NominalBinding.incomplete(
                            CobolSemanticProduct.ResolutionStatus.UNRESOLVED,
                            CobolSemanticProduct.ResolutionReason.DECLARATION_NOT_FOUND,
                            List.of()),
                    ((CobolSemanticProduct.DataReference) call.target()).provenance());
            return new CobolSemanticProduct.CallFact(call.header(), operand,
                    call.runtimeTarget(), call.runtimeUncertaintyCode(), call.normalContinuation(), call.surface());
        }

        @Override
        public CobolSemanticProduct.UnitId unit() {
            return delegate.unit();
        }

        @Override
        public CobolSemanticProduct.Policy policy() {
            return delegate.policy();
        }

        @Override
        public List<CobolSemanticProduct.DataDeclaration> dataDeclarations() {
            return delegate.dataDeclarations();
        }

        @Override
        public List<CobolSemanticProduct.StatementFact> statements() {
            return statements;
        }

        @Override
        public List<CobolSemanticProduct.Gap> gaps() {
            return gaps;
        }

        @Override
        public CobolSemanticProduct.CoverageSummary coverage() {
            return delegate.coverage();
        }

        @Override
        public List<CobolSemanticProduct.StatementId> rootStatements() {
            return statements.stream()
                    .filter(fact -> fact.header().containment().branch()
                            == CobolSemanticProduct.Branch.ROOT)
                    .map(fact -> fact.header().id()).toList();
        }

        @Override
        public Optional<CobolSemanticProduct.StatementFact> statement(
                CobolSemanticProduct.StatementId id) {
            return statements.stream().filter(fact -> fact.header().id().equals(id)).findFirst();
        }

        @Override
        public List<CobolSemanticProduct.StatementFact> children(
                CobolSemanticProduct.StatementId parent,
                CobolSemanticProduct.Branch branch) {
            return statements.stream()
                    .filter(fact -> fact.header().containment().equals(
                            CobolSemanticProduct.Containment.childOf(parent, branch)))
                    .filter(fact -> hiddenMembership.stream().noneMatch(hidden ->
                            hidden.parent().equals(parent) && hidden.branch() == branch
                                    && hidden.child().equals(fact.header().id())))
                    .toList();
        }

        @Override
        public List<CobolSemanticProduct.MoveFact> moves() {
            return typed(CobolSemanticProduct.MoveFact.class);
        }

        @Override
        public List<CobolSemanticProduct.CallFact> calls() {
            return typed(CobolSemanticProduct.CallFact.class);
        }

        @Override
        public List<CobolSemanticProduct.IfFact> ifs() {
            return typed(CobolSemanticProduct.IfFact.class);
        }

        @Override
        public List<CobolSemanticProduct.ObservedStatement> observedStatements() {
            return typed(CobolSemanticProduct.ObservedStatement.class);
        }

        private <T extends CobolSemanticProduct.StatementFact> List<T> typed(Class<T> type) {
            return statements.stream().filter(type::isInstance).map(type::cast).toList();
        }
    }

    private record HiddenMembership(
            CobolSemanticProduct.StatementId parent,
            CobolSemanticProduct.Branch branch,
            CobolSemanticProduct.StatementId child) { }
}
