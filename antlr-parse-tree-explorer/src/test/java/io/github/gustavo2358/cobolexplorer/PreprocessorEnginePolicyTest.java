package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolPreprocessorParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreprocessorEnginePolicyTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/preprocessor/page-directives.cbl");

    @Test
    void removesPageDirectivesWithoutMovingSourceAndTheyNeverReachTheSemanticAst() throws Exception {
        GrammarBinding binding = Bindings.cobol();
        String normalized = SourceNormalizerTestSupport.fixed(Files.readString(FIXTURE, StandardCharsets.UTF_8));

        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(
                binding, new CopybookLibrary(FIXTURE.getParent()))
                .process(SourceMap.identity(normalized, FIXTURE.getFileName().toString()),
                        FIXTURE.getFileName().toString());

        assertEquals(normalized.length(), outcome.text().length());
        assertEquals(normalized.chars().filter(c -> c == '\n').count(),
                outcome.text().chars().filter(c -> c == '\n').count());
        for (String directive : List.of("EJECT", "SKIP1", "SKIP2", "SKIP3", "TITLE")) {
            assertFalse(outcome.text().contains(directive), directive + " leaked through preprocessing");
        }

        int moveStart = outcome.text().indexOf("MOVE");
        Ast.SourceProvenance move = outcome.sourceMap().provenance(moveStart, moveStart + 4);
        assertEquals(normalized.indexOf("MOVE"), moveStart);
        assertEquals(12, move.original().startLine());
        assertTrue(move.exact());

        int ejectStart = normalized.indexOf("EJECT");
        Ast.SourceProvenance removed = outcome.sourceMap().provenance(ejectStart, ejectStart + 5);
        assertEquals(3, removed.original().startLine());
        assertFalse(removed.exact());

        Parser parser = cobolParser(binding, outcome.text());
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        Ast.Program ast = buildAst(parser, tree, outcome);
        assertEquals(2, nodes(ast, Ast.Statement.class).size());
        assertTrue(AstSnapshot.from(ast).nodes().stream().noneMatch(node ->
                List.of("EJECT", "SKIP1", "SKIP2", "SKIP3", "TITLE").stream()
                        .anyMatch(directive -> node.label().contains(directive)
                                || node.attributes().values().stream().anyMatch(value -> value.contains(directive)))));
    }

    @Test
    void policyCatalogIsExhaustiveForGeneratedStartRuleAlternatives() {
        Set<String> grammarConstructs = Arrays.stream(
                        CobolPreprocessorParser.StartRuleContext.class.getDeclaredMethods())
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> method.getGenericReturnType() instanceof ParameterizedType)
                .filter(method -> {
                    ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
                    if (type.getActualTypeArguments().length != 1
                            || !(type.getActualTypeArguments()[0] instanceof Class<?> element)) return false;
                    return ParserRuleContext.class.isAssignableFrom(element)
                            || TerminalNode.class.isAssignableFrom(element);
                })
                .map(java.lang.reflect.Method::getName)
                .collect(Collectors.toSet());

        assertEquals(grammarConstructs, PreprocessorEngine.policies().keySet());
        assertEquals(PreprocessorEngine.PreprocessorPolicy.REMOVE,
                PreprocessorEngine.policyFor("ejectStatement"));
        assertEquals(PreprocessorEngine.PreprocessorPolicy.REMOVE,
                PreprocessorEngine.policyFor("skipStatement"));
        assertEquals(PreprocessorEngine.PreprocessorPolicy.REMOVE,
                PreprocessorEngine.policyFor("titleStatement"));
        assertEquals(PreprocessorEngine.PreprocessorPolicy.UNSUPPORTED,
                PreprocessorEngine.policyFor("replaceArea"));
        assertEquals(PreprocessorEngine.PreprocessorPolicy.UNSUPPORTED,
                PreprocessorEngine.policyFor("replaceOffStatement"));
        assertThrows(IllegalStateException.class,
                () -> PreprocessorEngine.policyFor("futureGrammarConstruct"));
    }

    @Test
    void recognizedReplaceAreaFailsAtThePreprocessorBoundaryWithItsOwnDiagnostic() throws Exception {
        PreprocessorEngine engine = new PreprocessorEngine(
                Bindings.cobol(), new CopybookLibrary(FIXTURE.getParent()));

        UnsupportedOperationException areaFailure = assertThrows(
                UnsupportedOperationException.class,
                () -> engine.process(SourceMap.identity(
                        "REPLACE ==OLD== BY ==NEW==.\nOLD\nREPLACE OFF.\n", "replace.cbl"),
                        "replace.cbl"));
        UnsupportedOperationException offFailure = assertThrows(
                UnsupportedOperationException.class,
                () -> engine.process(SourceMap.identity("REPLACE OFF.\n", "replace-off.cbl"),
                        "replace-off.cbl"));

        assertTrue(areaFailure.getMessage().contains("replaceArea"), areaFailure.getMessage());
        assertTrue(offFailure.getMessage().contains("replaceOffStatement"), offFailure.getMessage());
    }

    private static Parser cobolParser(GrammarBinding binding, String source) {
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        Parser parser = binding.cobolParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        return parser;
    }

    private static Ast.Program buildAst(Parser parser, ParseTree tree,
                                        PreprocessorEngine.Outcome outcome) {
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        return new AstBuilder(parser, outcome.text(), outcome.sourceMap(), ids, sizes)
                .build(tree).program();
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) {
            size += index(tree.getChild(i), ids, sizes, next);
        }
        sizes.put(tree, size);
        return size;
    }

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        java.util.ArrayList<T> result = new java.util.ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return result;
    }
}
