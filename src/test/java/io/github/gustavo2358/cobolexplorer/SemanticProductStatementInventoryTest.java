package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Independent statement-inventory oracle for WORK-SEMANTIC-PRODUCT-002 CP5. */
class SemanticProductStatementInventoryTest {
    private static final String SOURCE_NAME = "semantic-product-cp5-inventory.cbl";
    private static final String SOURCE = String.join("\n",
            "       IDENTIFICATION DIVISION.",
            "       PROGRAM-ID. SEMANTIC-CP5-INVENTORY.",
            "       DATA DIVISION.",
            "       WORKING-STORAGE SECTION.",
            "       01 WS-PGM PIC X(8).",
            "       01 FLAG PIC 9.",
            "       PROCEDURE DIVISION.",
            "           DISPLAY 'HEAD'",
            "           MOVE 'A' TO WS-PGM",
            "           DISPLAY 'MIDDLE'",
            "           IF FLAG = 1",
            "               DISPLAY 'THEN-FIRST'",
            "               MOVE 'B' TO WS-PGM",
            "               PERFORM WORK-PARA",
            "           ELSE",
            "               CALL WS-PGM",
            "           END-IF",
            "           GOBACK",
            "           CALL WS-PGM",
            "           PERFORM UNTIL FLAG = 9",
            "               DISPLAY 'INLINE'",
            "           END-PERFORM",
            "           GOBACK.",
            "       WORK-PARA.",
            "           CONTINUE.",
            "       END PROGRAM SEMANTIC-CP5-INVENTORY.", "");

    @Test
    void canonicalAstStatementTraversalAndProjectionMustBeBijective() {
        Projection projection = projection();
        List<ObservedAstStatement> expected = inventory(projection.program());
        List<CobolSemanticProduct.StatementFact> actual = projection.port().statements();

        assertAll(
                () -> assertEquals(expected.size(), actual.size(),
                        "every typed AST statement must cross the boundary once"),
                () -> assertEquals(IntStream.range(0, expected.size()).boxed().toList(),
                        actual.stream().map(fact -> fact.header().point().ordinal()).toList(),
                        "the full statement inventory must have contiguous structural points"),
                () -> assertEquals(actual.size(), new HashSet<>(actual.stream()
                        .map(fact -> fact.header().id()).toList()).size(),
                        "no AST statement may be replaced by a duplicate fact"),
                () -> assertAnchorsMatch(expected, actual),
                () -> assertFactFamiliesMatch(expected, actual),
                () -> assertContainmentMatches(expected, actual));
    }

    @Test
    void mixedCapabilitiesMustRetainUnsupportedStatementsAndRefineIfStructure() {
        Projection projection = projection();
        List<ObservedAstStatement> expected = inventory(projection.program());
        CobolSemanticPort port = projection.port();
        long expectedObserved = expected.stream()
                .filter(item -> !(item.statement() instanceof Ast.MoveStatement)
                        && !(item.statement() instanceof Ast.CallStatement)
                        && !(item.statement() instanceof Ast.IfStatement))
                .count();
        CobolSemanticProduct.IfFact actualIf = port.ifs().get(0);

        assertAll(
                () -> assertEquals(2, port.moves().size()),
                () -> assertEquals(2, port.calls().size()),
                () -> assertEquals(1, port.ifs().size()),
                () -> assertEquals(expectedObserved, port.observedStatements().size(),
                        "DISPLAY/PERFORM/GOBACK/CONTINUE must remain positive facts"),
                () -> assertEquals(3, port.children(actualIf.header().id(),
                        CobolSemanticProduct.Branch.THEN).size(),
                        "unsupported direct IF children are still known branch members"),
                () -> assertEquals(1, port.children(actualIf.header().id(),
                        CobolSemanticProduct.Branch.ELSE).size()),
                () -> assertFalse(hasGap(port, actualIf.header().id(),
                        "BRANCH_CONTENT_NOT_PROJECTED"),
                        "a fully inventoried branch must not retain the CP4 gap"),
                () -> assertFalse(hasGap(port, actualIf.header().id(),
                        "CONTINUATION_NOT_PROJECTED"),
                        "an observed immediate successor must make continuation exact"),
                () -> assertTrue(port.observedStatements().stream().anyMatch(observed ->
                        observed.header().containment().branch()
                                == CobolSemanticProduct.Branch.UNKNOWN),
                        "a child under an unmodeled structural family stays explicitly unknown"));
    }

    @Test
    void summaryMustReconcileTheIndependentInventoryAndRejectOptimism() {
        Projection projection = projection();
        int expected = inventory(projection.program()).size();
        CobolSemanticProduct.State state = projection.state();
        CobolSemanticProduct.CoverageSummary summary = state.coverage();

        assertAll(
                () -> assertEquals(CobolSemanticProduct.InventoryStatus.COMPLETE,
                        summary.inventoryStatus()),
                () -> assertEquals(expected, summary.observedStatements()),
                () -> assertEquals(summary.observedStatements(),
                        summary.modeledStatements() + summary.partialStatements()
                                + summary.unsupportedStatements()
                                + summary.inputMissingStatements()),
                () -> assertEquals(state.statements().stream().filter(statement ->
                                statement.header().coverage()
                                        == CobolSemanticProduct.CoverageStatus.MODELED).count(),
                        summary.modeledStatements()),
                () -> assertEquals(state.statements().stream().filter(statement ->
                                statement.header().coverage()
                                        == CobolSemanticProduct.CoverageStatus.PARTIAL).count(),
                        summary.partialStatements()),
                () -> assertEquals(state.statements().stream().filter(statement ->
                                statement.header().coverage()
                                        == CobolSemanticProduct.CoverageStatus.UNSUPPORTED).count(),
                        summary.unsupportedStatements()),
                () -> assertEquals(state.statements().stream().filter(statement ->
                                statement.header().coverage()
                                        == CobolSemanticProduct.CoverageStatus.INPUT_MISSING).count(),
                        summary.inputMissingStatements()),
                () -> assertEveryIncompleteFactHasReason(state),
                () -> assertOptimisticSummaryFailsClosed(state));
    }

    @Test
    void postImplementationAdversarialMutationsRetainEveryIndependentFact() {
        List<Scenario> scenarios = List.of(
                scenario("unsupported-before-move", 1, 0, 0,
                        "           DISPLAY 'BEFORE'.",
                        "           MOVE 'A' TO WS-PGM.",
                        "           GOBACK."),
                scenario("unsupported-between-move-call", 1, 1, 0,
                        "           MOVE 'A' TO WS-PGM.",
                        "           DISPLAY 'BETWEEN'.",
                        "           CALL WS-PGM.",
                        "           GOBACK."),
                scenario("two-consecutive-unsupported", 1, 0, 0,
                        "           DISPLAY 'FIRST'.",
                        "           DISPLAY 'SECOND'.",
                        "           MOVE 'A' TO WS-PGM.",
                        "           GOBACK."),
                scenario("unsupported-first-then-child", 1, 0, 1,
                        "           IF FLAG = 1",
                        "               DISPLAY 'FIRST-THEN'",
                        "               MOVE 'A' TO WS-PGM",
                        "           END-IF.",
                        "           GOBACK."),
                scenario("unsupported-only-else-child", 1, 0, 1,
                        "           IF FLAG = 1",
                        "               MOVE 'A' TO WS-PGM",
                        "           ELSE",
                        "               DISPLAY 'ONLY-ELSE'",
                        "           END-IF.",
                        "           GOBACK."),
                scenario("unsupported-under-unmodeled-structure", 0, 0, 0,
                        "           PERFORM UNTIL FLAG = 9",
                        "               DISPLAY 'INLINE'",
                        "           END-PERFORM.",
                        "           GOBACK."),
                scenario("only-unmodeled-statements", 0, 0, 0,
                        "           DISPLAY 'ONLY'.",
                        "           GOBACK."),
                scenario("partial-between-supported", 1, 1, 0,
                        "           MOVE 'A' TO WS-PGM.",
                        "           EXEC CICS",
                        "                SYNCPOINT",
                        "           END-EXEC.",
                        "           CALL WS-PGM.",
                        "           GOBACK."),
                scenario("multiple-if-nesting", 2, 1, 3,
                        "           IF FLAG = 1",
                        "               DISPLAY 'OUTER-THEN'",
                        "               IF FLAG = 2",
                        "                   MOVE 'A' TO WS-PGM",
                        "               ELSE",
                        "                   DISPLAY 'INNER-ELSE'",
                        "               END-IF",
                        "           ELSE",
                        "               CALL WS-PGM",
                        "           END-IF.",
                        "           IF FLAG = 3",
                        "               MOVE 'B' TO WS-PGM",
                        "           END-IF.",
                        "           GOBACK."));

        assertAll(scenarios.stream().map(scenario -> (Executable) () -> {
            Projection projection = projection(scenario.source(), scenario.sourceName());
            List<ObservedAstStatement> expected = inventory(projection.program());
            CobolSemanticProduct.State state = projection.state();
            CobolSemanticPort port = projection.port();
            int typed = scenario.moves() + scenario.calls() + scenario.ifs();

            assertAll(scenario.label(),
                    () -> assertEquals(expected.size(), state.statements().size()),
                    () -> assertEquals(scenario.moves(), port.moves().size()),
                    () -> assertEquals(scenario.calls(), port.calls().size()),
                    () -> assertEquals(scenario.ifs(), port.ifs().size()),
                    () -> assertEquals(expected.size() - typed,
                            port.observedStatements().size()),
                    () -> assertEquals(CobolSemanticProduct.InventoryStatus.COMPLETE,
                            state.coverage().inventoryStatus()),
                    () -> assertEquals(IntStream.range(0, expected.size()).boxed().toList(),
                            state.statements().stream().map(fact ->
                                    fact.header().point().ordinal()).toList()),
                    () -> assertAnchorsMatch(expected, state.statements()),
                    () -> assertFactFamiliesMatch(expected, state.statements()),
                    () -> assertContainmentMatches(expected, state.statements()),
                    () -> assertSummaryMatchesFacts(state),
                    () -> assertEveryIncompleteFactHasReason(state),
                    () -> assertAggregateReadinessDoesNotExceedFacts(state));
        }));
    }

    @Test
    void insertingUnsupportedStatementsChangesOnlyTheDependentInventoryStructure() {
        String sourceName = "cp5-independent-facts.cbl";
        Projection baseline = projection(
                source("CP5-INDEPENDENT",
                        "           MOVE 'A' TO WS-PGM.",
                        "           CALL WS-PGM.",
                        "           GOBACK."),
                sourceName);
        Projection withUnsupported = projection(
                source("CP5-INDEPENDENT",
                        "           DISPLAY 'BEFORE'.",
                        "           MOVE 'A' TO WS-PGM.",
                        "           DISPLAY 'BETWEEN'.",
                        "           CALL WS-PGM.",
                        "           GOBACK."),
                sourceName);
        CobolSemanticProduct.MoveFact baselineMove = baseline.port().moves().get(0);
        CobolSemanticProduct.MoveFact mutatedMove = withUnsupported.port().moves().get(0);
        CobolSemanticProduct.CallFact baselineCall = baseline.port().calls().get(0);
        CobolSemanticProduct.CallFact mutatedCall = withUnsupported.port().calls().get(0);

        assertAll(
                () -> assertEquals(baseline.state().statements().size() + 2,
                        withUnsupported.state().statements().size()),
                () -> assertEquals(baselineMove.source().value(), mutatedMove.source().value()),
                () -> assertEquals(baselineMove.source().kind(), mutatedMove.source().kind()),
                () -> assertEquals(baselineMove.target().role(), mutatedMove.target().role()),
                () -> assertEquals(baselineMove.target().binding(),
                        mutatedMove.target().binding()),
                () -> assertEquals(baselineCall.syntax(), mutatedCall.syntax()),
                () -> assertEquals(baselineCall.operand().role(), mutatedCall.operand().role()),
                () -> assertEquals(baselineCall.operand().binding(),
                        mutatedCall.operand().binding()),
                () -> assertEquals(baselineCall.runtimeTarget(), mutatedCall.runtimeTarget()),
                () -> assertSummaryMatchesFacts(withUnsupported.state()));
    }

    @Test
    void observedBranchChildrenRefineOnlyTheGapsThatTheFullInventoryResolves() {
        Projection thenProjection = projection(
                source("CP5-THEN",
                        "           IF FLAG = 1",
                        "               DISPLAY 'FIRST-THEN'",
                        "               MOVE 'A' TO WS-PGM",
                        "           END-IF.",
                        "           GOBACK."),
                "cp5-then.cbl");
        Projection elseProjection = projection(
                source("CP5-ELSE",
                        "           IF FLAG = 1",
                        "               MOVE 'A' TO WS-PGM",
                        "           ELSE",
                        "               DISPLAY 'ONLY-ELSE'",
                        "           END-IF.",
                        "           GOBACK."),
                "cp5-else.cbl");
        CobolSemanticProduct.IfFact thenIf = thenProjection.port().ifs().get(0);
        CobolSemanticProduct.IfFact elseIf = elseProjection.port().ifs().get(0);

        assertAll(
                () -> assertInstanceOf(CobolSemanticProduct.ObservedStatement.class,
                        thenProjection.port().children(thenIf.header().id(),
                                CobolSemanticProduct.Branch.THEN).get(0)),
                () -> assertInstanceOf(CobolSemanticProduct.ObservedStatement.class,
                        elseProjection.port().children(elseIf.header().id(),
                                CobolSemanticProduct.Branch.ELSE).get(0)),
                () -> assertFalse(hasGap(thenProjection.port(), thenIf.header().id(),
                        "BRANCH_CONTENT_NOT_PROJECTED")),
                () -> assertFalse(hasGap(elseProjection.port(), elseIf.header().id(),
                        "BRANCH_CONTENT_NOT_PROJECTED")),
                () -> assertTrue(thenProjection.port().gaps().stream().anyMatch(gap ->
                        gap.statement().equals(thenIf.header().id())
                                && gap.code().equals("CONDITION_SEMANTICS_NOT_AVAILABLE"))),
                () -> assertTrue(elseProjection.port().gaps().stream().anyMatch(gap ->
                        gap.statement().equals(elseIf.header().id())
                                && gap.code().equals("CONDITION_SEMANTICS_NOT_AVAILABLE"))),
                () -> assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                        thenIf.header().readiness().cfg().status()),
                () -> assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                        elseIf.header().readiness().cfg().status()));
    }

    @Test
    void preservedEmbeddedStatementBetweenSupportedFactsIsExplicitlyPartial() {
        Projection projection = projection(
                source("CP5-PARTIAL",
                        "           MOVE 'A' TO WS-PGM.",
                        "           EXEC CICS",
                        "                SYNCPOINT",
                        "           END-EXEC.",
                        "           CALL WS-PGM.",
                        "           GOBACK."),
                "cp5-partial.cbl");
        CobolSemanticProduct.ObservedStatement embedded = observedByShape(
                projection.port(), "OPAQUE_CICS");

        assertAll(
                () -> assertEquals(CobolSemanticProduct.CoverageStatus.PARTIAL,
                        embedded.header().coverage()),
                () -> assertEquals("OBSERVED_STATEMENT_PARTIAL", embedded.gapCode()),
                () -> assertTrue(hasGap(projection.port(), embedded.header().id(),
                        embedded.gapCode())),
                () -> assertEquals(1, projection.port().moves().size()),
                () -> assertEquals(1, projection.port().calls().size()));
    }

    @Test
    void realZeroStatementUnitHasCompleteInventoryAndNoApplicableReadiness() {
        Projection projection = projection(source("CP5-EMPTY"), "cp5-empty.cbl");

        assertAll(
                () -> assertTrue(inventory(projection.program()).isEmpty()),
                () -> assertTrue(projection.state().statements().isEmpty()),
                () -> assertEquals(CobolSemanticProduct.InventoryStatus.COMPLETE,
                        projection.state().coverage().inventoryStatus()),
                () -> assertEquals(0, projection.state().coverage().observedStatements()),
                () -> assertEquals(CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE,
                        projection.state().coverage().readiness().lowering().status()),
                () -> assertEquals(CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE,
                        projection.state().coverage().readiness().cfg().status()),
                () -> assertEquals(CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE,
                        projection.state().coverage().readiness().effectsDataflow().status()));
    }

    @Test
    void unavailableInventoryCapsEveryAggregateReadinessWithoutInventingAStatementGap() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                source("CP5-INPUT-GAP", "           CALL WS-PGM."),
                "cp5-input-gap.cbl");
        ResolutionAnalysisReport.FrontendState incomplete =
                new ResolutionAnalysisReport.FrontendState(0, 0, 0,
                        List.of(new Diagnostic("COBOL", Diagnostic.Phase.PREPROCESSOR,
                                Diagnostic.Code.UNRESOLVED_COPY, "cp5-input-gap.cbl",
                                7, 11, "configured COPY member is unavailable",
                                "MISSING", "")));
        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(
                analysis.build(), incomplete, analysis.occurrences(), analysis.resolution());
        CompilationUnitModel.ProgramUnit unit = analysis.model().programUnits().get(0);
        CobolSemanticProduct.State state = CobolSemanticProductProjector.project(
                new CobolSemanticProductProjector.FrontendProducts(
                        analysis.build(), analysis.tables(), analysis.occurrences(),
                        analysis.resolution(), report),
                unit.id());

        assertAll(
                () -> assertEquals(1, state.statements().size(),
                        "known statements remain published despite unavailable input"),
                () -> assertInstanceOf(CobolSemanticProduct.CallFact.class,
                        state.statements().get(0)),
                () -> assertEquals(CobolSemanticProduct.InventoryStatus.INPUT_MISSING,
                        state.coverage().inventoryStatus()),
                () -> assertEquals(0, state.coverage().inputMissingStatements(),
                        "global inventory uncertainty must not be assigned to a known fact"),
                () -> assertFalse(state.gaps().stream().anyMatch(gap ->
                        gap.scope() == CobolSemanticProduct.GapScope.ANALYSIS_INPUT),
                        "a global input gap must not be localized on an arbitrary statement"),
                () -> assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                        state.coverage().readiness().lowering().status()),
                () -> assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                        state.coverage().readiness().cfg().status()),
                () -> assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                        state.coverage().readiness().effectsDataflow().status()));
    }

    private static Projection projection() {
        return projection(SOURCE, SOURCE_NAME);
    }

    private static Projection projection(String source, String sourceName) {
        AstBoundaryTestSupport.Analysis analysis =
                AstBoundaryTestSupport.analyze(source, sourceName);
        CompilationUnitModel.ProgramUnit unit = analysis.model().programUnits().get(0);
        CobolSemanticProductProjector.FrontendProducts products =
                new CobolSemanticProductProjector.FrontendProducts(
                        analysis.build(), analysis.tables(), analysis.occurrences(),
                        analysis.resolution(), analysis.report());
        CobolSemanticProduct.State state =
                CobolSemanticProductProjector.project(products, unit.id());
        return new Projection(unit.program(), state, CobolSemanticPort.open(state));
    }

    private static Scenario scenario(String label, int moves, int calls, int ifs,
                                     String... procedureLines) {
        String normalized = label.replace("-", "").toUpperCase();
        String programName = "CP5" + normalized.substring(0, Math.min(20, normalized.length()));
        return new Scenario(label, source(programName, procedureLines),
                label + ".cbl", moves, calls, ifs);
    }

    private static String source(String programName, String... procedureLines) {
        List<String> lines = new ArrayList<>(List.of(
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. " + programName + ".",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-PGM PIC X(8).",
                "       01 FLAG PIC 9.",
                "       PROCEDURE DIVISION."));
        lines.addAll(List.of(procedureLines));
        lines.add("       END PROGRAM " + programName + ".");
        lines.add("");
        return String.join("\n", lines);
    }

    /** Independent statement oracle over the canonical typed AST only. */
    private static List<ObservedAstStatement> inventory(Ast.Program program) {
        List<ObservedAstStatement> result = new ArrayList<>();
        Map<Ast.Statement, Boolean> seen = new IdentityHashMap<>();
        observe(program, null, CobolSemanticProduct.Branch.ROOT, result, seen);
        return List.copyOf(result);
    }

    private static void observe(
            Ast.Node node,
            Ast.Statement nearestStatement,
            CobolSemanticProduct.Branch branch,
            List<ObservedAstStatement> output,
            Map<Ast.Statement, Boolean> seen) {
        if (!(node instanceof Ast.Statement statement)) {
            for (Ast.Node child : Ast.children(node))
                observe(child, nearestStatement, branch, output, seen);
            return;
        }

        assertTrue(seen.put(statement, Boolean.TRUE) == null,
                "canonical traversal must encounter each statement instance once");
        output.add(new ObservedAstStatement(statement, nearestStatement, branch, output.size()));
        if (statement instanceof Ast.IfStatement conditional) {
            observe(conditional.condition(), statement, CobolSemanticProduct.Branch.UNKNOWN,
                    output, seen);
            for (Ast.Statement child : conditional.thenBranch())
                observe(child, statement, CobolSemanticProduct.Branch.THEN, output, seen);
            for (Ast.Statement child : conditional.elseBranch())
                observe(child, statement, CobolSemanticProduct.Branch.ELSE, output, seen);
            return;
        }
        for (Ast.Node child : Ast.children(statement))
            observe(child, statement, CobolSemanticProduct.Branch.UNKNOWN, output, seen);
    }

    private static void assertAnchorsMatch(
            List<ObservedAstStatement> expected,
            List<CobolSemanticProduct.StatementFact> actual) {
        assertEquals(expected.size(), actual.size(), "anchor comparison requires a bijection");
        for (int index = 0; index < expected.size(); index++) {
            Ast.SourceProvenance source = expected.get(index).statement().meta().provenance();
            CobolSemanticProduct.Provenance fact = actual.get(index).header().provenance();
            assertEquals(source.original().file(), fact.original().file());
            assertEquals(source.original().startLine(), fact.original().startLine());
            assertEquals(source.original().startColumn(), fact.original().startColumn());
            assertEquals(source.original().endLine(), fact.original().endLine());
            assertEquals(source.original().endColumn(), fact.original().endColumn());
            assertEquals(source.exact(), fact.exact());
        }
    }

    private static void assertFactFamiliesMatch(
            List<ObservedAstStatement> expected,
            List<CobolSemanticProduct.StatementFact> actual) {
        assertEquals(expected.size(), actual.size(), "family comparison requires a bijection");
        for (int index = 0; index < expected.size(); index++) {
            Ast.Statement statement = expected.get(index).statement();
            CobolSemanticProduct.StatementFact fact = actual.get(index);
            if (statement instanceof Ast.MoveStatement) {
                assertInstanceOf(CobolSemanticProduct.MoveFact.class, fact);
            } else if (statement instanceof Ast.CallStatement) {
                assertInstanceOf(CobolSemanticProduct.CallFact.class, fact);
            } else if (statement instanceof Ast.IfStatement) {
                assertInstanceOf(CobolSemanticProduct.IfFact.class, fact);
            } else {
                assertInstanceOf(CobolSemanticProduct.ObservedStatement.class, fact);
            }
        }
    }

    private static void assertContainmentMatches(
            List<ObservedAstStatement> expected,
            List<CobolSemanticProduct.StatementFact> actual) {
        assertEquals(expected.size(), actual.size(), "containment comparison requires a bijection");
        Map<Ast.Statement, CobolSemanticProduct.StatementId> ids = new IdentityHashMap<>();
        for (int index = 0; index < expected.size(); index++)
            ids.put(expected.get(index).statement(), actual.get(index).header().id());
        for (int index = 0; index < expected.size(); index++) {
            ObservedAstStatement item = expected.get(index);
            CobolSemanticProduct.Containment expectedContainment;
            if (item.parent() == null) {
                expectedContainment = CobolSemanticProduct.Containment.root();
            } else if (item.parent() instanceof Ast.IfStatement) {
                expectedContainment = CobolSemanticProduct.Containment.childOf(
                        ids.get(item.parent()), item.branch());
            } else {
                expectedContainment = CobolSemanticProduct.Containment.unknown();
            }
            assertEquals(expectedContainment, actual.get(index).header().containment());
        }
    }

    private static void assertEveryIncompleteFactHasReason(CobolSemanticProduct.State state) {
        for (CobolSemanticProduct.StatementFact statement : state.statements()) {
            if (statement.header().coverage() == CobolSemanticProduct.CoverageStatus.MODELED)
                continue;
            assertTrue(state.gaps().stream().anyMatch(gap ->
                            gap.statement().equals(statement.header().id())),
                    () -> "missing localized reason for " + statement.header().id());
        }
    }

    private static void assertSummaryMatchesFacts(CobolSemanticProduct.State state) {
        CobolSemanticProduct.CoverageSummary summary = state.coverage();
        assertEquals(state.statements().size(), summary.observedStatements());
        assertEquals(summary.observedStatements(), summary.modeledStatements()
                + summary.partialStatements() + summary.unsupportedStatements()
                + summary.inputMissingStatements());
        assertEquals(count(state, CobolSemanticProduct.CoverageStatus.MODELED),
                summary.modeledStatements());
        assertEquals(count(state, CobolSemanticProduct.CoverageStatus.PARTIAL),
                summary.partialStatements());
        assertEquals(count(state, CobolSemanticProduct.CoverageStatus.UNSUPPORTED),
                summary.unsupportedStatements());
        assertEquals(count(state, CobolSemanticProduct.CoverageStatus.INPUT_MISSING),
                summary.inputMissingStatements());
    }

    private static long count(CobolSemanticProduct.State state,
                              CobolSemanticProduct.CoverageStatus coverage) {
        return state.statements().stream().filter(statement ->
                statement.header().coverage() == coverage).count();
    }

    private static void assertAggregateReadinessDoesNotExceedFacts(
            CobolSemanticProduct.State state) {
        assertDimensionDoesNotExceedFacts(state,
                state.coverage().readiness().lowering().status(),
                readiness -> readiness.lowering().status());
        assertDimensionDoesNotExceedFacts(state,
                state.coverage().readiness().cfg().status(),
                readiness -> readiness.cfg().status());
        assertDimensionDoesNotExceedFacts(state,
                state.coverage().readiness().effectsDataflow().status(),
                readiness -> readiness.effectsDataflow().status());
    }

    private static void assertDimensionDoesNotExceedFacts(
            CobolSemanticProduct.State state,
            CobolSemanticProduct.ReadinessStatus aggregate,
            Function<CobolSemanticProduct.Readiness,
                    CobolSemanticProduct.ReadinessStatus> dimension) {
        for (CobolSemanticProduct.StatementFact statement : state.statements()) {
            CobolSemanticProduct.ReadinessStatus fact =
                    dimension.apply(statement.header().readiness());
            if (fact == CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE) continue;
            assertTrue(readinessRank(aggregate) <= readinessRank(fact),
                    () -> "aggregate readiness exceeds " + statement.header().id());
        }
    }

    private static int readinessRank(CobolSemanticProduct.ReadinessStatus status) {
        return switch (status) {
            case BLOCKED -> 0;
            case PARTIAL -> 1;
            case SUFFICIENT -> 2;
            case NOT_APPLICABLE -> 3;
        };
    }

    private static void assertOptimisticSummaryFailsClosed(CobolSemanticProduct.State state) {
        CobolSemanticProduct.CoverageSummary current = state.coverage();
        CobolSemanticProduct.Readiness optimistic = new CobolSemanticProduct.Readiness(
                claim(CobolSemanticProduct.ReadinessStatus.SUFFICIENT),
                claim(CobolSemanticProduct.ReadinessStatus.SUFFICIENT),
                claim(CobolSemanticProduct.ReadinessStatus.SUFFICIENT));
        CobolSemanticProduct.CoverageSummary forged = new CobolSemanticProduct.CoverageSummary(
                current.inventoryStatus(), current.observedStatements(),
                current.modeledStatements(), current.partialStatements(),
                current.unsupportedStatements(), current.inputMissingStatements(), optimistic);
        assertThrows(IllegalArgumentException.class, () -> new CobolSemanticProduct.State(
                state.unit(), state.policy(), state.dataDeclarations(), state.statements(),
                state.gaps(), forged));
    }

    private static CobolSemanticProduct.ReadinessClaim claim(
            CobolSemanticProduct.ReadinessStatus status) {
        return new CobolSemanticProduct.ReadinessClaim(status, "controlled optimistic mutation");
    }

    private static boolean hasGap(
            CobolSemanticPort port,
            CobolSemanticProduct.StatementId statement,
            String code) {
        return port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(statement) && gap.code().equals(code));
    }

    private static CobolSemanticProduct.ObservedStatement observedByShape(
            CobolSemanticPort port, String shape) {
        CobolSemanticProduct.ObservedStatement result = null;
        for (CobolSemanticProduct.ObservedStatement observed : port.observedStatements()) {
            if (!observed.observedShape().equals(shape)) continue;
            if (result != null)
                throw new AssertionError("fixture has multiple observed shapes " + shape);
            result = observed;
        }
        if (result == null)
            throw new AssertionError("fixture has no observed shape " + shape);
        return result;
    }

    private record ObservedAstStatement(
            Ast.Statement statement,
            Ast.Statement parent,
            CobolSemanticProduct.Branch branch,
            int ordinal) { }

    private record Projection(
            Ast.Program program,
            CobolSemanticProduct.State state,
            CobolSemanticPort port) { }

    private record Scenario(String label, String source, String sourceName,
                            int moves, int calls, int ifs) { }
}
