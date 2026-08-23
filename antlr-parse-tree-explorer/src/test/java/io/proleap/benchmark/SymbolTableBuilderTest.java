package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SymbolTableBuilderTest {
    @Test
    void collectsScopedDeclarationsWithoutResolvingReferences() throws Exception {
        Ast.Program ast = parseSelectedProgram();
        SymbolTable table = new SymbolTableBuilder().build(ast);

        assertEquals(SymbolTable.ScopeKind.ROOT, table.rootScope().kind());
        assertTrue(table.scopes().size() > 500);
        assertTrue(table.symbols().size() > 500);

        SymbolTable.Symbol program = table.lookupLocal(0, SymbolTable.Namespace.PROGRAM, "coactupc")
                .stream().findFirst().orElseThrow();
        assertEquals(SymbolTable.SymbolKind.PROGRAM, program.kind());
        assertEquals("COACTUPC", program.canonicalName());

        SymbolTable.Symbol data = table.symbols().stream()
                .filter(symbol -> symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM)
                .filter(symbol -> symbol.canonicalName().equals("WS-RESP-CD"))
                .findFirst().orElseThrow();
        assertEquals("07", data.attributes().get("level"));
        assertEquals(data, table.lookupLocal(data.scopeId(), SymbolTable.Namespace.DATA, "ws-resp-cd").get(0));

        SymbolTable.Symbol paragraph = table.symbols().stream()
                .filter(symbol -> symbol.kind() == SymbolTable.SymbolKind.PARAGRAPH)
                .filter(symbol -> symbol.canonicalName().equals("COMMON-RETURN"))
                .findFirst().orElseThrow();
        assertTrue(paragraph.declarationAstNodeId() >= 0);
        assertTrue(table.lookupAll(SymbolTable.Namespace.PROCEDURE, "common-return").contains(paragraph));

        assertTrue(table.symbols().stream().noneMatch(symbol -> symbol.writtenName().equals("FILLER")));
        assertTrue(table.symbols().stream().noneMatch(symbol -> symbol.writtenName().equals("<entry>")));
        assertTrue(table.symbols().stream().noneMatch(symbol -> symbol.canonicalName().equals("CSUTLDTC")),
                "external CALL targets are references, not declarations in this program");

        for (SymbolTable.Scope scope : table.scopes()) {
            if (scope.id() == 0) assertEquals(-1, scope.parentId());
            else assertTrue(scope.parentId() >= 0 && scope.parentId() < scope.id());
        }
        for (SymbolTable.Symbol symbol : table.symbols()) {
            assertEquals(SymbolTable.canonical(symbol.writtenName()), symbol.canonicalName());
            assertTrue(symbol.scopeId() >= 0 && symbol.scopeId() < table.scopes().size());
        }
    }

    private static Ast.Program parseSelectedProgram() throws Exception {
        Path project = Path.of("").toAbsolutePath().normalize();
        Path sourceFile = project.resolve("corpus/cbl/COACTUPC.cbl");
        GrammarBinding binding = Bindings.proleap();
        String fixed = SourceNormalizer.fixed(Files.readString(sourceFile, StandardCharsets.UTF_8));
        String source = new PreprocessorEngine(binding, new CopybookLibrary(project.resolve("corpus/cpy")))
                .process(fixed, sourceFile.getFileName().toString()).text();
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = binding.cobolParser(tokens);
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());

        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[] {0});
        return new AstBuilder(parser, source, ids, sizes).build(tree).program();
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }
}
