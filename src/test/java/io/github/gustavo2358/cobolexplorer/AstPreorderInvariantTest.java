package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression oracle for the canonical AST pre-order contract confirmed by BUG-AST-PREORDER-001.
 * The acceptance rule is structural: traversal through {@link Ast#children(Ast.Node)} must reach
 * every node instance exactly once and each node ID must equal its canonical pre-order position.
 */
class AstPreorderInvariantTest {
    private static final Path FIXTURES = Path.of("src/test/resources/cobol/semantic");

    @Test
    void procedurePerformUntilPreservesReferencesControlsAndCanonicalSnapshot() throws Exception {
        Analysis analysis = analyze("ast-preorder-perform-until.cbl");
        Ast.PerformStatement perform = onlyPerform(analysis.program());

        assertFrontendAndAstSucceeded(analysis, perform);
        assertEquals(Ast.PerformKind.PROCEDURE, perform.performKind());
        assertNotNull(perform.fromReference());
        assertEquals("TARGET-PARA", perform.fromReference().baseName());
        assertEquals(1, perform.controlExpressions().size());
        assertCanonicalPreOrder(analysis.program());
        assertDoesNotThrow(() -> AstSnapshot.from(analysis.program()));
    }

    @Test
    void procedurePerformThruUntilPreservesBothReferencesControlsAndCanonicalSnapshot() throws Exception {
        Analysis analysis = analyze("ast-preorder-perform-thru.cbl");
        Ast.PerformStatement perform = onlyPerform(analysis.program());

        assertFrontendAndAstSucceeded(analysis, perform);
        assertEquals(Ast.PerformKind.PROCEDURE, perform.performKind());
        assertNotNull(perform.fromReference());
        assertNotNull(perform.throughReference());
        assertEquals(List.of("FIRST-PARA", "LAST-PARA"), List.of(
                perform.fromReference().baseName(), perform.throughReference().baseName()));
        assertEquals(1, perform.controlExpressions().size());
        assertCanonicalPreOrder(analysis.program());
        assertDoesNotThrow(() -> AstSnapshot.from(analysis.program()));
    }

    @Test
    void conflictingVisibilityDiagnosticsReuseTheirDeclarationMetadataWithoutIdGaps() throws Exception {
        Analysis analysis = analyze("ast-preorder-conflicting-visibility.cbl");

        assertEquals(0, analysis.preprocessingErrors());
        assertEquals(0, analysis.unresolvedCopies());
        assertEquals(0, analysis.lexerErrors());
        assertEquals(0, analysis.parserErrors());
        Ast.FileDescription fileDescription = canonicalNodes(analysis.program()).stream()
                .filter(Ast.FileDescription.class::isInstance)
                .map(Ast.FileDescription.class::cast)
                .filter(file -> file.fileName().equals("CONFLICTING-FILE"))
                .findFirst().orElseThrow();
        Ast.DataEntry dataEntry = canonicalNodes(analysis.program()).stream()
                .filter(Ast.DataEntry.class::isInstance)
                .map(Ast.DataEntry.class::cast)
                .filter(entry -> entry.name().equals("CONFLICTING-ITEM"))
                .findFirst().orElseThrow();

        assertEquals(2, analysis.semanticDiagnostics().size());
        assertTrue(analysis.semanticDiagnostics().stream()
                .allMatch(diagnostic -> diagnostic.code().equals("CONFLICTING_DECLARATION_VISIBILITY")));
        assertEquals(Ast.DeclarationVisibility.CONFLICTING, fileDescription.visibility());
        assertEquals(Ast.DeclarationVisibility.CONFLICTING, dataEntry.visibility());
        assertTrue(analysis.semanticDiagnostics().stream().anyMatch(diagnostic ->
                        diagnostic.meta() == fileDescription.meta()),
                "the FileDescription diagnostic must be anchored to its AST declaration");
        assertTrue(analysis.semanticDiagnostics().stream().anyMatch(diagnostic ->
                        diagnostic.meta() == dataEntry.meta()),
                "the DataEntry diagnostic must be anchored to its AST declaration");
        assertCanonicalPreOrder(analysis.program());
        assertDoesNotThrow(() -> AstSnapshot.from(analysis.program()));
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
    void everyAstNodeIdMatchesCanonicalPreOrder() throws Exception {
        for (String fixture : List.of(
                "ast-preorder-perform-until.cbl",
                "ast-preorder-perform-thru.cbl",
                "ast-preorder-conflicting-visibility.cbl",
                "ast-preorder-consistent-controls.cbl",
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
            AstSnapshot snapshot = assertDoesNotThrow(() -> AstSnapshot.from(analysis.program()), fixture);

            Analysis repeated = analyze(fixture);
            assertCanonicalPreOrder(repeated.program());
            assertEquals(snapshot.nodes(), AstSnapshot.from(repeated.program()).nodes(),
                    fixture + " must produce deterministic AST ids and structure");
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
