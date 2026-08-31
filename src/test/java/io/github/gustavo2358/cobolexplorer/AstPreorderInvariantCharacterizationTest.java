package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterizes BUG-AST-PREORDER-001 without changing production behavior.
 *
 * <p>The exact broken IDs asserted by the focused tests are Discovery evidence only. They must not
 * become Phase 2 acceptance values or be updated to whatever numbers a future implementation emits.
 * The future regression contract is structural: traverse with {@link Ast#children(Ast.Node)} and
 * require each node ID to equal its canonical pre-order position, as encoded by
 * {@link #assertCanonicalPreOrder(Ast.Program)}.</p>
 */
class AstPreorderInvariantCharacterizationTest {
    private static final Path FIXTURES = Path.of("src/test/resources/cobol/semantic");

    @Test
    void procedurePerformUntilBuildsSuccessfullyButSnapshotRejectsItsChildOrder() throws Exception {
        Analysis analysis = analyze("ast-preorder-perform-until.cbl");
        Ast.PerformStatement perform = onlyPerform(analysis.program());

        assertFrontendAndAstSucceeded(analysis, perform);
        assertEquals(Ast.PerformKind.PROCEDURE, perform.performKind());
        assertNotNull(perform.fromReference());
        assertEquals("TARGET-PARA", perform.fromReference().baseName());
        assertEquals(1, perform.controlExpressions().size());
        // Discovery-only snapshot of the current defect; not a future implementation contract.
        assertEquals(List.of(21, 14), List.of(
                perform.fromReference().meta().id(), perform.controlExpressions().get(0).meta().id()),
                "observed broken IDs: the control subtree is allocated before the structurally "
                        + "preceding procedure reference");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> AstSnapshot.from(analysis.program()));
        assertEquals("AST ids are not pre-order at ProcedureReference: expected 14 but got 21",
                failure.getMessage());
    }

    @Test
    void procedurePerformThruUntilShowsTheSameDefectWithAConditionDependentOffset() throws Exception {
        Analysis analysis = analyze("ast-preorder-perform-thru.cbl");
        Ast.PerformStatement perform = onlyPerform(analysis.program());

        assertFrontendAndAstSucceeded(analysis, perform);
        assertEquals(Ast.PerformKind.PROCEDURE, perform.performKind());
        assertNotNull(perform.fromReference());
        assertNotNull(perform.throughReference());
        assertEquals(List.of("FIRST-PARA", "LAST-PARA"), List.of(
                perform.fromReference().baseName(), perform.throughReference().baseName()));
        assertEquals(1, perform.controlExpressions().size());
        // Discovery-only snapshot of the current THRU + UNTIL defect; not a future acceptance value.
        assertEquals(List.of(17, 18, 14), List.of(
                perform.fromReference().meta().id(), perform.throughReference().meta().id(),
                perform.controlExpressions().get(0).meta().id()),
                "observed broken IDs: both procedure references are allocated after the "
                        + "structurally later UNTIL control subtree");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> AstSnapshot.from(analysis.program()));
        assertEquals("AST ids are not pre-order at ProcedureReference: expected 14 but got 17",
                failure.getMessage());
    }

    @Test
    void diagnosticMetadataAllocatedFromTheNodeCounterCreatesAnIndependentGap() throws Exception {
        Analysis analysis = analyze("ast-preorder-conflicting-visibility.cbl");

        assertEquals(0, analysis.preprocessingErrors());
        assertEquals(0, analysis.unresolvedCopies());
        assertEquals(0, analysis.lexerErrors());
        assertEquals(0, analysis.parserErrors());
        assertEquals(1, analysis.semanticDiagnostics().size());
        SemanticCoverage.Diagnostic diagnostic = analysis.semanticDiagnostics().get(0);
        assertEquals("CONFLICTING_DECLARATION_VISIBILITY", diagnostic.code());
        assertEquals(8, diagnostic.meta().id(),
                "diagnostic metadata currently consumes the same counter as AST nodes");
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12),
                canonicalNodes(analysis.program()).stream().map(node -> node.meta().id()).toList(),
                "the diagnostic-only allocation leaves an AST id gap");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> AstSnapshot.from(analysis.program()));
        assertEquals("AST ids are not pre-order at Division: expected 8 but got 9",
                failure.getMessage());
    }

    @Test
    void procedureWithoutControlAndInlineWithControlRemainCanonicalNegativeControls() throws Exception {
        Analysis analysis = analyze("ast-preorder-consistent-controls.cbl");
        List<Ast.PerformStatement> performs = canonicalNodes(analysis.program()).stream()
                .filter(Ast.PerformStatement.class::isInstance)
                .map(Ast.PerformStatement.class::cast).toList();

        assertEquals(2, performs.size());
        Ast.PerformStatement procedure = performs.get(0);
        Ast.PerformStatement inline = performs.get(1);
        assertEquals(Ast.PerformKind.PROCEDURE, procedure.performKind());
        assertTrue(procedure.controlExpressions().isEmpty());
        assertEquals(Ast.PerformKind.INLINE, inline.performKind());
        assertFalse(inline.controlExpressions().isEmpty());
        assertCanonicalPreOrder(analysis.program());
        assertDoesNotThrow(() -> AstSnapshot.from(analysis.program()));
    }

    @Test
    void representativeExistingAstSurfaceHasCanonicalReachabilityAndPreOrder() throws Exception {
        for (String fixture : List.of(
                "ast-cfg-boundary.cbl",
                "statements.cbl",
                "declarations.cbl",
                "expressions.cbl",
                "references.cbl")) {
            Analysis analysis = analyze(fixture);
            assertEquals(0, analysis.preprocessingErrors(), fixture);
            assertEquals(0, analysis.lexerErrors(), fixture);
            assertEquals(0, analysis.parserErrors(), fixture);
            assertCanonicalPreOrder(analysis.program());
            assertDoesNotThrow(() -> AstSnapshot.from(analysis.program()), fixture);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "ast.preorder.required", matches = "true")
    void everyAstNodeIdMatchesCanonicalPreOrder() throws Exception {
        // This property—not replacement hardcodes in the focused tests—is the future regression oracle.
        for (String fixture : List.of(
                "ast-preorder-perform-until.cbl",
                "ast-preorder-perform-thru.cbl",
                "ast-preorder-conflicting-visibility.cbl")) {
            Analysis analysis = analyze(fixture);
            assertCanonicalPreOrder(analysis.program());
        }
    }

    private static void assertFrontendAndAstSucceeded(Analysis analysis, Ast.PerformStatement perform) {
        assertEquals(0, analysis.preprocessingErrors(), "preprocessing must succeed");
        assertEquals(0, analysis.unresolvedCopies(), "the fixtures have no COPY dependency");
        assertEquals(0, analysis.lexerErrors(), "lexing must succeed");
        assertEquals(0, analysis.parserErrors(), "parsing must succeed");
        assertNotNull(analysis.program(), "AST construction must succeed");
        assertNotNull(perform, "the typed PerformStatement must be reachable");
        assertTrue(Ast.children(perform).stream().anyMatch(Ast.ProcedureReference.class::isInstance));
        assertFalse(perform.controlExpressions().isEmpty());
    }

    private static Ast.PerformStatement onlyPerform(Ast.Program program) {
        List<Ast.PerformStatement> performs = canonicalNodes(program).stream()
                .filter(Ast.PerformStatement.class::isInstance)
                .map(Ast.PerformStatement.class::cast).toList();
        assertEquals(1, performs.size());
        return performs.get(0);
    }

    private static void assertCanonicalPreOrder(Ast.Program program) {
        List<Ast.Node> nodes = canonicalNodes(program);
        for (int expected = 0; expected < nodes.size(); expected++) {
            Ast.Node node = nodes.get(expected);
            int position = expected;
            assertEquals(expected, node.meta().id(), () -> "canonical pre-order mismatch at "
                    + node.getClass().getSimpleName() + ": expected " + position
                    + " but got " + node.meta().id());
        }
    }

    private static List<Ast.Node> canonicalNodes(Ast.Node root) {
        List<Ast.Node> result = new ArrayList<>();
        IdentityHashMap<Ast.Node, Boolean> active = new IdentityHashMap<>();
        IdentityHashMap<Ast.Node, Boolean> reached = new IdentityHashMap<>();
        addCanonical(root, result, active, reached);
        return result;
    }

    private static void addCanonical(Ast.Node node, List<Ast.Node> result,
                                     IdentityHashMap<Ast.Node, Boolean> active,
                                     IdentityHashMap<Ast.Node, Boolean> reached) {
        assertFalse(active.containsKey(node), "Ast.children must not contain cycles");
        assertFalse(reached.containsKey(node), "an AST node instance must be reachable exactly once");
        active.put(node, Boolean.TRUE);
        reached.put(node, Boolean.TRUE);
        result.add(node);
        for (Ast.Node child : Ast.children(node)) {
            assertNotNull(child, "Ast.children must not expose null children");
            addCanonical(child, result, active, reached);
        }
        active.remove(node);
    }

    private static Analysis analyze(String fixtureName) throws Exception {
        Path fixture = FIXTURES.resolve(fixtureName);
        String raw = Files.readString(fixture, StandardCharsets.UTF_8);
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(raw, fixtureName,
                SourceNormalizer.SourceFormat.FIXED);
        GrammarBinding binding = Bindings.cobol();
        PreprocessorEngine.Outcome preprocessed = new PreprocessorEngine(binding,
                new CopybookLibrary(FIXTURES)).process(normalized.sourceMap(), fixtureName);

        List<Diagnostic> diagnostics = new ArrayList<>(preprocessed.diagnostics());
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(preprocessed.text(), fixtureName));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new AntlrDiagnosticListener(binding.name(), Diagnostic.Phase.LEXER,
                fixtureName, diagnostics));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        tokens.seek(0);
        Parser parser = binding.cobolParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new AntlrDiagnosticListener(binding.name(), Diagnostic.Phase.PARSER,
                fixtureName, diagnostics));
        ParseTree tree = binding.cobolStart(parser);

        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, preprocessed.text(),
                preprocessed.sourceMap(), ids, sizes).buildCompilationUnit(tree, fixtureName);
        CompilationUnitModel.ProgramUnit unit = build.compilationUnit().programUnits().get(0);
        Ast.Program program = unit.program();
        int lexerErrors = (int) diagnostics.stream()
                .filter(diagnostic -> diagnostic.phase() == Diagnostic.Phase.LEXER).count();
        int parserErrors = (int) diagnostics.stream()
                .filter(diagnostic -> diagnostic.phase() == Diagnostic.Phase.PARSER).count();
        return new Analysis(program, preprocessed.errors(), preprocessed.unresolved(),
                lexerErrors, parserErrors, build.diagnosticsByProgramUnit().get(unit.id()));
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int child = 0; child < tree.getChildCount(); child++) {
            size += index(tree.getChild(child), ids, sizes, next);
        }
        sizes.put(tree, size);
        return size;
    }

    private record Analysis(Ast.Program program, int preprocessingErrors, int unresolvedCopies,
                            int lexerErrors, int parserErrors,
                            List<SemanticCoverage.Diagnostic> semanticDiagnostics) {}
}
