package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DynamicCallVariantTest {
    @Test
    void modelsAllVariantCallsThroughOneWorkingStorageTarget() throws Exception {
        Path project = Path.of("").toAbsolutePath().normalize();
        Path sourceFile = project.resolve("corpus/cbl/CBSTM03D.CBL");
        String raw = Files.readString(sourceFile, StandardCharsets.UTF_8);

        assertEquals(14, occurrences(raw, "CALL WS-CALL-TARGET"));
        assertEquals(1, occurrences(raw, "MOVE 'CBSTM03B' TO WS-CALL-TARGET"));
        assertEquals(1, occurrences(raw, "MOVE 'CEE3ABD' TO WS-CALL-TARGET"));
        assertEquals(0, occurrences(raw, "CALL 'CBSTM03B'"));
        assertEquals(0, occurrences(raw, "CALL 'CEE3ABD'"));

        GrammarBinding binding = Bindings.cobol();
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(raw,
                sourceFile.getFileName().toString(), SourceNormalizer.SourceFormat.FIXED);
        PreprocessorEngine.Outcome preprocessing = new PreprocessorEngine(
                binding, new CopybookLibrary(project.resolve("corpus/cpy")))
                .process(normalized.sourceMap(), sourceFile.getFileName().toString());
        String source = preprocessing.text();
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = binding.cobolParser(tokens);
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());

        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[] {0});
        Ast.Program ast = new AstBuilder(parser, source, preprocessing.sourceMap(), ids, sizes)
                .build(tree).program();
        List<Ast.CallStatement> calls = flatten(ast).stream()
                .filter(Ast.CallStatement.class::isInstance)
                .map(Ast.CallStatement.class::cast)
                .toList();

        assertEquals("CBSTM03D", ast.name());
        assertEquals(14, calls.size());
        assertTrue(calls.stream().allMatch(call ->
                call.targetSyntax() == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION));
        assertTrue(calls.stream().allMatch(call -> call.target() instanceof Ast.DataReference reference
                && reference.writtenName().equals("WS-CALL-TARGET")));

        SymbolTable table = new SymbolTableBuilder().build(ast);
        List<SymbolTable.Symbol> targets = table.lookupAll(SymbolTable.Namespace.DATA, "ws-call-target");
        assertEquals(1, targets.size());
        assertEquals(SymbolTable.SymbolKind.DATA_ITEM, targets.get(0).kind());
        assertEquals("05", targets.get(0).attributes().get("level"));
        assertTrue(targets.get(0).attributes().get("declaration").contains("VALUE SPACES"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0, offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) { count++; offset += needle.length(); }
        return count;
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private static List<Ast.Node> flatten(Ast.Node root) {
        List<Ast.Node> result = new ArrayList<>();
        result.add(root);
        for (Ast.Node child : Ast.children(root)) result.addAll(flatten(child));
        return result;
    }
}
