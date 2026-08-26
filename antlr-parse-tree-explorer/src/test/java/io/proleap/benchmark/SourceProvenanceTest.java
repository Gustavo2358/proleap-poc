package io.proleap.benchmark;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceProvenanceTest {

    @Test
    void mapsNestedCopyReplacingAndMissingCopyWithoutLosingOrigin() throws Exception {
        Path fixture = Path.of("src/test/resources/cobol/provenance").toAbsolutePath().normalize();
        Path main = fixture.resolve("main.cbl");
        GrammarBinding binding = Bindings.proleap();
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(
                Files.readString(main, StandardCharsets.UTF_8), "main.cbl");

        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(binding,
                new CopybookLibrary(fixture.resolve("cpy"))).process(normalized.sourceMap(), "main.cbl");

        assertEquals(1, outcome.unresolved());
        assertTrue(outcome.text().contains("RENAMED-FIELD"));
        assertTrue(outcome.text().contains("SECOND-FIELD"));
        assertTrue(outcome.text().contains("UNRESOLVED COPY MISSING"));

        Ast.SourceProvenance replaced = provenanceOf(outcome, "RENAMED-FIELD");
        assertEquals("FIRST.cpy", replaced.original().file());
        assertEquals(2, replaced.original().startLine());
        assertEquals(List.of("FIRST.cpy"), includedFiles(replaced));
        assertFalse(replaced.exact(), "REPLACING must preserve origin but mark transformed text");

        Ast.SourceProvenance nested = provenanceOf(outcome, "SECOND-FIELD");
        assertEquals("SECOND.cpy", nested.original().file());
        assertEquals(1, nested.original().startLine());
        assertEquals(14, nested.original().startColumn());
        assertEquals(List.of("FIRST.cpy", "SECOND.cpy"), includedFiles(nested));
        assertTrue(nested.exact());

        Ast.SourceProvenance missing = provenanceOf(outcome, "UNRESOLVED COPY MISSING");
        assertEquals("main.cbl", missing.original().file());
        assertEquals(6, missing.original().startLine());
        assertTrue(missing.includeChain().isEmpty());
        assertFalse(missing.exact(), "generated diagnostic text is not an exact source slice");
    }

    @Test
    void carriesPreprocessorOriginIntoEveryAstNodeMeta() throws Exception {
        Path fixture = Path.of("src/test/resources/cobol/provenance").toAbsolutePath().normalize();
        Path main = fixture.resolve("main.cbl");
        GrammarBinding binding = Bindings.proleap();
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(
                Files.readString(main, StandardCharsets.UTF_8), "main.cbl");
        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(binding,
                new CopybookLibrary(fixture.resolve("cpy"))).process(normalized.sourceMap(), "main.cbl");

        Lexer lexer = binding.cobolLexer(CharStreams.fromString(outcome.text(), "main.cbl"));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = binding.cobolParser(tokens);
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());

        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        Ast.Program ast = new AstBuilder(parser, outcome.text(), outcome.sourceMap(), ids, sizes)
                .build(tree).program();

        Ast.DataEntry nested = nodes(ast, Ast.DataEntry.class).stream()
                .filter(entry -> entry.name().equals("SECOND-FIELD")).findFirst().orElseThrow();
        assertEquals("SECOND.cpy", nested.meta().provenance().original().file());
        assertEquals(List.of("FIRST.cpy", "SECOND.cpy"),
                includedFiles(nested.meta().provenance()));

        Ast.MoveStatement move = nodes(ast, Ast.MoveStatement.class).stream().findFirst().orElseThrow();
        assertEquals("main.cbl", move.meta().provenance().original().file());
        assertEquals(8, move.meta().provenance().original().startLine());
        assertEquals(11, move.meta().provenance().original().startColumn());
        assertTrue(move.meta().provenance().includeChain().isEmpty());

        AstSnapshot.Node snapshotNode = AstSnapshot.from(ast).nodes().stream()
                .filter(node -> node.id() == nested.meta().id()).findFirst().orElseThrow();
        assertEquals("SECOND.cpy", snapshotNode.sourceFile());
        assertEquals(1, snapshotNode.sourceLine());
        assertEquals(2, snapshotNode.includeDepth());
        assertFalse(snapshotNode.sourceExact(),
                "the grammar context crosses the COPY boundary, so the snapshot must not claim an exact slice");
    }

    private static Ast.SourceProvenance provenanceOf(PreprocessorEngine.Outcome outcome, String text) {
        int start = outcome.text().indexOf(text);
        return outcome.sourceMap().provenance(start, start + text.length());
    }

    private static List<String> includedFiles(Ast.SourceProvenance provenance) {
        return provenance.includeChain().stream().map(Ast.CopyFrame::includedFile).toList();
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return result;
    }
}
