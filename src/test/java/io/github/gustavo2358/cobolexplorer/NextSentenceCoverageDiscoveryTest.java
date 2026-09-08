package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Discovery characterization, not the desired fixed behavior. See WORK-AST-004. */
class NextSentenceCoverageDiscoveryTest {
    private static final String MISSING = "typed AST node has no canonical coverage finding";

    record Scenario(String name, String source, int nextCount, int missingCount) {
        @Override public String toString() { return name; }
    }

    static Stream<Scenario> scenarios() throws Exception {
        String ifSource = fixture("if");
        String searchSource = fixture("search");
        return Stream.of(
                new Scenario("if-then", ifSource, 1, 1),
                new Scenario("if-then-keyword", ifSource.replace("IF 1 = 1", "IF 1 = 1 THEN"), 1, 1),
                new Scenario("if-else", program("IF 1 = 1 CONTINUE ELSE NEXT SENTENCE END-IF\nCONTINUE.\nGOBACK."), 1, 1),
                new Scenario("if-both", program("IF 1 = 1 NEXT SENTENCE ELSE NEXT SENTENCE END-IF.\nGOBACK."), 2, 2),
                new Scenario("if-period", program("IF 1 = 1 NEXT SENTENCE ELSE CONTINUE.\nGOBACK."), 1, 1),
                new Scenario("if-nested", program("IF 1 = 1\nIF 2 = 2 NEXT SENTENCE END-IF\nEND-IF.\nGOBACK."), 1, 1),
                new Scenario("search-when", searchSource, 1, 1),
                // Grammar/AST probe only: this change does not prove SEARCH ALL key validity.
                new Scenario("search-all-when", searchSource.replace("SEARCH TABLE-ITEM", "SEARCH ALL TABLE-ITEM"), 1, 1),
                new Scenario("search-two-whens", searchSource.replace("END-SEARCH", "WHEN TABLE-ITEM(IDX) = 2 CONTINUE\n           END-SEARCH"), 1, 1),
                new Scenario("generic-statement", program("NEXT SENTENCE.\nGOBACK."), 1, 0),
                new Scenario("if-statement-list", program("IF 1 = 1 CONTINUE NEXT SENTENCE END-IF.\nGOBACK."), 1, 0),
                new Scenario("evaluate-statement-list", program("EVALUATE TRUE\nWHEN TRUE NEXT SENTENCE\nEND-EVALUATE.\nGOBACK."), 1, 0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void characterizesGrammarAstCoverageAndProjection(Scenario scenario) throws Exception {
        var analysis = analyze(scenario);
        var next = AstBoundaryTestSupport.nodes(analysis, Ast.NextSentenceStatement.class);
        assertEquals(scenario.nextCount(), next.size(), "typed identity must survive construction");
        var contexts = AstBoundaryTestSupport.contexts(analysis.tree(), ParserRuleContext.class);
        long directAlternatives = contexts.stream().filter(context ->
                (context instanceof CobolParser.IfThenContext
                        || context instanceof CobolParser.IfElseContext
                        || context instanceof CobolParser.SearchWhenContext)
                        && context.getToken(CobolParser.NEXT, 0) != null
                        && context.getToken(CobolParser.SENTENCE, 0) != null).count();
        assertEquals(scenario.missingCount(), directAlternatives);
        assertEquals(scenario.nextCount() - scenario.missingCount(),
                AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.NextSentenceStatementContext.class).size());

        var statements = AstBoundaryTestSupport.nodes(analysis, Ast.Statement.class);
        var missing = statements.stream().filter(statement -> findings(analysis, statement).isEmpty()).toList();
        assertEquals(scenario.missingCount(), missing.size());
        assertTrue(missing.stream().allMatch(Ast.NextSentenceStatement.class::isInstance),
                "the full reachable statement inventory is checked, not only NEXT SENTENCE");
        for (Ast.Statement statement : statements) {
            var matching = findings(analysis, statement);
            if (!matching.isEmpty()) {
                assertEquals(1, matching.size());
                assertEquals(statement.meta(), matching.get(0).meta());
            }
        }
        for (Ast.SearchWhen when : AstBoundaryTestSupport.nodes(analysis, Ast.SearchWhen.class)) {
            assertEquals(1, findings(analysis, when).size(), "WHEN coverage does not cover its action's identity");
        }
        AstBoundaryTestSupport.assertActualProductsJoin(analysis);
        if (scenario.missingCount() > 0) {
            assertEquals(MISSING, assertThrows(IllegalArgumentException.class,
                    () -> project(analysis, analysis.build())).getMessage());
        } else {
            assertNextInventory(project(analysis, analysis.build()), scenario.nextCount());
        }

        // A contrast in pipeline behavior, explicitly not a runtime equivalence claim.
        var control = AstBoundaryTestSupport.analyze(scenario.source().replace("NEXT SENTENCE", "CONTINUE"),
                scenario.name() + "-continue.cbl");
        assertTrue(AstBoundaryTestSupport.nodes(control, Ast.NextSentenceStatement.class).isEmpty());
        var controlState = project(control, control.build());
        assertEquals(AstBoundaryTestSupport.nodes(control, Ast.Statement.class).size(), controlState.statements().size());
        assertTrue(controlState.statements().stream().filter(CobolSemanticProduct.ObservedStatement.class::isInstance)
                .map(CobolSemanticProduct.ObservedStatement.class::cast)
                .noneMatch(statement -> statement.observedKind().equals("NEXT_SENTENCE")));
    }

    @Test
    void reportedIfRetainsPeriodBoundaryBeyondImmediateSuccessor() throws Exception {
        var analysis = AstBoundaryTestSupport.analyze(fixture("if"), "next-sentence-if.cbl");
        var sentences = AstBoundaryTestSupport.nodes(analysis, Ast.Sentence.class);
        assertEquals(2, sentences.size());
        var first = sentences.get(0);
        var conditional = assertInstanceOf(Ast.IfStatement.class, first.statements().get(0));
        var next = assertInstanceOf(Ast.NextSentenceStatement.class, conditional.thenBranch().get(0));
        assertInstanceOf(Ast.ModeledStatement.class, first.statements().get(1));
        assertEquals(Ast.SentenceTerminator.PERIOD, first.terminator());
        assertTrue(next.meta().span().endToken() < first.terminatorSpan().startToken());
        assertTrue(first.statements().get(1).meta().span().endToken() < first.terminatorSpan().startToken());
        assertInstanceOf(Ast.GobackStatement.class, sentences.get(1).statements().get(0));
    }

    @Test
    void removingCoverageFromNormalPathStillFailsClosed() {
        var analysis = AstBoundaryTestSupport.analyze(program("NEXT SENTENCE."), "negative.cbl");
        var unit = analysis.model().programUnits().get(0).id();
        assertEquals(1, analysis.build().coverageByProgramUnit().get(unit).findings().size());
        var corrupt = new CompilationUnitBuildResult(analysis.model(),
                Map.of(unit, new SemanticCoverage.Report(List.of())), analysis.build().diagnosticsByProgramUnit());
        assertEquals(MISSING, assertThrows(IllegalArgumentException.class,
                () -> project(analysis, corrupt)).getMessage());
    }

    @Test
    @EnabledIfSystemProperty(named = "next.sentence.required", matches = "true")
    void requiredContractEveryStatementHasCoverageAndNextSentenceCrossesBoundary() throws Exception {
        List<Executable> requirements = new ArrayList<>();
        for (Scenario scenario : scenarios().toList()) {
            requirements.add(() -> {
                var analysis = analyze(scenario);
                for (Ast.Statement statement : AstBoundaryTestSupport.nodes(analysis, Ast.Statement.class)) {
                    assertEquals(1, findings(analysis, statement).size(), scenario.name()
                            + ": every materialized statement needs one canonical finding; node=" + statement.meta().id());
                    assertEquals(statement.meta(), findings(analysis, statement).get(0).meta());
                }
                assertNextInventory(project(analysis, analysis.build()), scenario.nextCount());
            });
        }
        assertAll("Required contract; RED until explicit implementation approval", requirements);
    }

    private static void assertNextInventory(CobolSemanticProduct.State state, int expected) throws Exception {
        var next = state.statements().stream().filter(CobolSemanticProduct.ObservedStatement.class::isInstance)
                .map(CobolSemanticProduct.ObservedStatement.class::cast)
                .filter(statement -> statement.observedKind().equals("NEXT_SENTENCE")).toList();
        assertEquals(expected, next.size());
        for (var statement : next) {
            assertEquals("TYPED_NEXT_SENTENCE", statement.observedShape());
            assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED, statement.header().readiness().cfg().status());
            assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED, statement.header().readiness().effectsDataflow().status());
            assertFalse(statement.gapCode().isBlank());
        }
        var json = new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(
                io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state)));
        assertTrue(json.isObject());
        assertEquals(expected, json.findValues("observedKind").stream()
                .filter(value -> value.asText().equals("NEXT_SENTENCE")).count());
    }

    private static List<SemanticCoverage.Finding> findings(AstBoundaryTestSupport.Analysis analysis, Ast.Node node) {
        var unit = analysis.model().programUnits().get(0).id();
        return analysis.build().coverageByProgramUnit().get(unit).findings().stream()
                .filter(finding -> finding.astNodeId() == node.meta().id()).toList();
    }

    private static AstBoundaryTestSupport.Analysis analyze(Scenario scenario) {
        return AstBoundaryTestSupport.analyze(scenario.source(), scenario.name() + ".cbl");
    }

    private static CobolSemanticProduct.State project(AstBoundaryTestSupport.Analysis analysis,
                                                     CompilationUnitBuildResult build) {
        return CobolSemanticProductProjector.project(new CobolSemanticProductProjector.FrontendProducts(
                build, analysis.tables(), analysis.occurrences(), analysis.resolution(), analysis.report(),
                ScalarMoveSemantics.analyze(build, analysis.tables(), analysis.resolution(), analysis.report())),
                analysis.model().programUnits().get(0).id());
    }

    private static String fixture(String suffix) throws Exception {
        return Files.readString(Path.of("src/test/resources/cobol/semantic/next-sentence-" + suffix + ".cbl"));
    }

    private static String program(String body) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. NEXT-PROBE.\nPROCEDURE DIVISION.\n"
                + body + "\nEND PROGRAM NEXT-PROBE.\n";
    }
}
