package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Required regression oracles promoted from the WORK-AST-004 discovery. */
class NextSentenceCoverageDiscoveryTest {
    private static final String MISSING = "typed AST node has no canonical coverage finding";

    record Scenario(String name, String source, int nextCount, int directCount) {
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
    void preservesGrammarAstCoverageAndProjection(Scenario scenario) throws Exception {
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
        assertEquals(scenario.directCount(), directAlternatives);
        assertEquals(scenario.nextCount() - scenario.directCount(),
                AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.NextSentenceStatementContext.class).size());

        assertStatementCoverage(analysis);
        for (Ast.NextSentenceStatement statement : next) {
            var finding = findings(analysis, statement).get(0);
            var origin = statement.meta().origin();
            var context = contexts.stream().filter(candidate ->
                    CobolParser.ruleNames[candidate.getRuleIndex()].equals(origin.grammarRule())
                            && candidate.getStart().getTokenIndex() == statement.meta().span().startToken()
                            && candidate.getStop().getTokenIndex() == statement.meta().span().endToken())
                    .findFirst().orElseThrow();
            assertEquals(context.getStart().getInputStream().getText(
                    org.antlr.v4.runtime.misc.Interval.of(context.getStart().getStartIndex(),
                            context.getStop().getStopIndex())), finding.writtenText());
            assertNextPolicy(finding);
        }
        for (Ast.SearchWhen when : AstBoundaryTestSupport.nodes(analysis, Ast.SearchWhen.class)) {
            var container = findings(analysis, when).get(0);
            for (Ast.Statement child : when.statements()) {
                var finding = findings(analysis, child).get(0);
                assertNotEquals(container.astNodeId(), finding.astNodeId());
                assertNotEquals(container.id(), finding.id(), "WHEN and action need distinct findings");
            }
        }
        AstBoundaryTestSupport.assertActualProductsJoin(analysis);
        var state = project(analysis, analysis.build());
        assertNextInventory(state, scenario.nextCount());
        assertPublishedIdentityAndContainment(analysis.model().programUnits().get(0).program(), state);
        var repeated = analyze(scenario);
        assertEquals(analysis.model().programUnits(), repeated.model().programUnits(),
                "AST IDs, Meta and ownership must be deterministic");
        assertEquals(analysis.build().coverageByProgramUnit(), repeated.build().coverageByProgramUnit());
        assertArrayEquals(json(state), json(project(repeated, repeated.build())));

        // A contrast in pipeline behavior, explicitly not a runtime equivalence claim.
        var control = AstBoundaryTestSupport.analyze(scenario.source().replace("NEXT SENTENCE", "CONTINUE"),
                scenario.name() + "-continue.cbl");
        assertTrue(AstBoundaryTestSupport.nodes(control, Ast.NextSentenceStatement.class).isEmpty());
        assertStatementCoverage(control);
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
        assertAll("Every materialized statement has coverage and NEXT SENTENCE remains publishable", requirements);
    }

    private static void assertNextInventory(CobolSemanticProduct.State state, int expected) throws Exception {
        var next = state.statements().stream().filter(CobolSemanticProduct.ObservedStatement.class::isInstance)
                .map(CobolSemanticProduct.ObservedStatement.class::cast)
                .filter(statement -> statement.observedKind().equals("NEXT_SENTENCE")).toList();
        assertEquals(expected, next.size());
        for (var statement : next) {
            assertEquals("TYPED_NEXT_SENTENCE", statement.observedShape());
            assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED, statement.header().readiness().lowering().status());
            assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED, statement.header().readiness().cfg().status());
            assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED, statement.header().readiness().effectsDataflow().status());
            assertFalse(statement.gapCode().isBlank());
        }
        var json = new ObjectMapper().readTree(json(state));
        assertTrue(json.isObject());
        for (var statement : json.path("statements")) {
            if (!statement.path("observedKind").asText().equals("NEXT_SENTENCE")) continue;
            assertEquals("TYPED_NEXT_SENTENCE", statement.path("observedShape").asText());
            for (String dimension : List.of("lowering", "cfg", "effectsDataflow")) {
                assertEquals("BLOCKED", statement.path("header").path("readiness")
                        .path(dimension).path("status").asText());
            }
            assertFalse(statement.path("gapCode").asText().isBlank());
        }
        assertEquals(expected, json.findValues("observedKind").stream()
                .filter(value -> value.asText().equals("NEXT_SENTENCE")).count());
    }

    @Test
    void secondWhenRetainsItsOwnNextSentence() throws Exception {
        String source = fixture("search").replace("NEXT SENTENCE", "CONTINUE")
                .replace("END-SEARCH", "WHEN TABLE-ITEM(IDX) = 2 NEXT SENTENCE\n           END-SEARCH");
        var analysis = AstBoundaryTestSupport.analyze(source, "second-when.cbl");
        var whens = AstBoundaryTestSupport.nodes(analysis, Ast.SearchWhen.class);
        assertEquals(2, whens.size());
        assertInstanceOf(Ast.ModeledStatement.class, whens.get(0).statements().get(0));
        var next = assertInstanceOf(Ast.NextSentenceStatement.class, whens.get(1).statements().get(0));
        assertStatementCoverage(analysis);
        assertNotEquals(findings(analysis, whens.get(1)).get(0).id(), findings(analysis, next).get(0).id());
        assertNextInventory(project(analysis, analysis.build()), 1);
    }

    @Test
    void repeatedLocalAstIdsRemainScopedToTheirProgramUnit() throws Exception {
        String source = program("IF 1 = 1 NEXT SENTENCE ELSE NEXT SENTENCE END-IF.")
                + program("IF 1 = 1 NEXT SENTENCE ELSE NEXT SENTENCE END-IF.")
                .replace("NEXT-PROBE", "SECOND-PROBE");
        var analysis = AstBoundaryTestSupport.analyze(source, "two-units.cbl");
        assertEquals(2, analysis.model().programUnits().size());
        assertStatementCoverage(analysis);
        AstBoundaryTestSupport.assertActualProductsJoin(analysis);
        var publishedIds = new HashSet<CobolSemanticProduct.StatementId>();
        for (var unit : analysis.model().programUnits()) {
            var state = project(analysis, analysis.build(), unit.id());
            assertNextInventory(state, 2);
            assertPublishedIdentityAndContainment(unit.program(), state);
            for (var fact : state.statements()) assertTrue(publishedIds.add(fact.header().id()));
        }
    }

    private static void assertStatementCoverage(AstBoundaryTestSupport.Analysis analysis) {
        for (var unit : analysis.model().programUnits()) {
            var nodes = AstBoundaryTestSupport.nodes(unit.program());
            var unique = java.util.Collections.newSetFromMap(new IdentityHashMap<Ast.Node, Boolean>());
            for (int index = 0; index < nodes.size(); index++) {
                var node = nodes.get(index);
                assertTrue(unique.add(node), "AST ownership must not share a child instance");
                assertEquals(index, node.meta().id(), "AST IDs must follow canonical pre-order");
                if (!(node instanceof Ast.Statement) && !(node instanceof Ast.SearchWhen)) continue;
                var matching = analysis.build().coverageByProgramUnit().get(unit.id()).findings().stream()
                        .filter(finding -> finding.astNodeId() == node.meta().id()).toList();
                assertEquals(1, matching.size(), "every materialized statement and WHEN needs exactly one finding");
                assertEquals(node.meta(), matching.get(0).meta());
                assertEquals(node.meta().origin().grammarRule(), matching.get(0).grammarRule());
            }
        }
    }

    private static void assertNextPolicy(SemanticCoverage.Finding finding) {
        switch (finding.grammarRule()) {
            case "nextSentenceStatement" -> {
                assertEquals(SemanticCoverage.ConstructionCoverage.MODELED, finding.coverage());
                assertEquals(SemanticCoverage.DependencyKnowledge.NOT_DEPENDENCY_BEARING, finding.dependencyKnowledge());
            }
            case "ifThen", "ifElse" -> {
                assertEquals(SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED, finding.coverage());
                assertEquals(SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN, finding.dependencyKnowledge());
            }
            case "searchWhen" -> {
                assertEquals(SemanticCoverage.ConstructionCoverage.MODELED, finding.coverage());
                assertEquals(SemanticCoverage.DependencyKnowledge.REFERENCE_READY, finding.dependencyKnowledge());
            }
            default -> fail("NEXT SENTENCE must retain its real grammatical origin: " + finding.grammarRule());
        }
    }

    private static void assertPublishedIdentityAndContainment(Ast.Program program, CobolSemanticProduct.State state) {
        var statements = AstBoundaryTestSupport.nodes(program).stream()
                .filter(Ast.Statement.class::isInstance).map(Ast.Statement.class::cast).toList();
        assertEquals(statements.size(), state.statements().size());
        var byNode = new IdentityHashMap<Ast.Statement, CobolSemanticProduct.StatementFact>();
        var ids = new HashSet<CobolSemanticProduct.StatementId>();
        for (int index = 0; index < statements.size(); index++) {
            var node = statements.get(index);
            var fact = state.statements().get(index);
            byNode.put(node, fact);
            assertTrue(ids.add(fact.header().id()), "each statement crosses the boundary once");
            assertEquals(index, fact.header().point().ordinal());
            var original = node.meta().provenance().original();
            var published = fact.header().provenance().original();
            assertEquals(original.file(), published.file());
            assertEquals(original.startLine(), published.startLine());
            assertEquals(original.startColumn(), published.startColumn());
            assertEquals(original.endLine(), published.endLine());
            assertEquals(original.endColumn(), published.endColumn());
            assertEquals(node.meta().provenance().exact(), fact.header().provenance().exact());
            if (node instanceof Ast.NextSentenceStatement) {
                var observed = assertInstanceOf(CobolSemanticProduct.ObservedStatement.class, fact);
                assertEquals("NEXT_SENTENCE", observed.observedKind());
                var expected = switch (node.meta().origin().grammarRule()) {
                    case "ifThen", "ifElse" -> CobolSemanticProduct.CoverageStatus.PARTIAL;
                    default -> CobolSemanticProduct.CoverageStatus.UNSUPPORTED;
                };
                assertEquals(expected, observed.header().coverage());
            }
        }
        for (var node : statements) {
            if (node instanceof Ast.IfStatement conditional) {
                for (var child : conditional.thenBranch()) assertEquals(
                        CobolSemanticProduct.Containment.childOf(byNode.get(node).header().id(), CobolSemanticProduct.Branch.THEN),
                        byNode.get(child).header().containment());
                for (var child : conditional.elseBranch()) assertEquals(
                        CobolSemanticProduct.Containment.childOf(byNode.get(node).header().id(), CobolSemanticProduct.Branch.ELSE),
                        byNode.get(child).header().containment());
            }
            if (node instanceof Ast.SearchStatement search) {
                for (var when : search.whens()) for (var child : when.statements())
                    assertEquals(CobolSemanticProduct.Containment.unknown(), byNode.get(child).header().containment());
            }
        }
    }

    private static byte[] json(CobolSemanticProduct.State state) throws java.io.IOException {
        return SemanticProductJsonWriter.serialize(
                io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state));
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
        return project(analysis, build, analysis.model().programUnits().get(0).id());
    }

    private static CobolSemanticProduct.State project(AstBoundaryTestSupport.Analysis analysis,
                                                     CompilationUnitBuildResult build,
                                                     ResolutionContracts.ProgramUnitId unit) {
        return CobolSemanticProductProjector.project(new CobolSemanticProductProjector.FrontendProducts(
                build, analysis.tables(), analysis.occurrences(), analysis.resolution(), analysis.report(),
                ScalarMoveSemantics.analyze(build, analysis.tables(), analysis.resolution(), analysis.report())),
                unit);
    }

    private static String fixture(String suffix) throws Exception {
        return Files.readString(Path.of("src/test/resources/cobol/semantic/next-sentence-" + suffix + ".cbl"));
    }

    private static String program(String body) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. NEXT-PROBE.\nPROCEDURE DIVISION.\n"
                + body + "\nEND PROGRAM NEXT-PROBE.\n";
    }
}
