package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SymbolTableBuilderTest {
    @Test
    void ancestorScopeLookupDoesNotImplementCobolVisibilityAcrossStructuralBranches() {
        List<SymbolTable.Scope> scopes = List.of(
                new SymbolTable.Scope(0, -1, SymbolTable.ScopeKind.ROOT, "<root>", -1, -1),
                new SymbolTable.Scope(1, 0, SymbolTable.ScopeKind.PROGRAM, "PROGRAM", -1, 0),
                new SymbolTable.Scope(2, 1, SymbolTable.ScopeKind.DIVISION, "DATA DIVISION", -1, 1),
                new SymbolTable.Scope(3, 2, SymbolTable.ScopeKind.SECTION, "WORKING-STORAGE", -1, 2),
                new SymbolTable.Scope(4, 1, SymbolTable.ScopeKind.DIVISION, "PROCEDURE DIVISION", -1, 3),
                new SymbolTable.Scope(5, 4, SymbolTable.ScopeKind.PARAGRAPH, "MAIN", -1, 4)
        );
        SymbolTable.Symbol wsX = new SymbolTable.Symbol(
                0,
                SymbolTable.SymbolKind.DATA_ITEM,
                SymbolTable.Namespace.DATA,
                "WS-X",
                "WS-X",
                3,
                5,
                new Ast.SourceSpan(3, 7, 3, 10, 0, 0),
                Map.of("level", "01")
        );
        SymbolTable table = new SymbolTable(scopes, List.of(wsX), List.of(), List.of(), List.of());

        assertTrue(table.lookupInAncestorScopes(5, SymbolTable.Namespace.DATA, "WS-X").isEmpty(),
                "ancestor lookup must not be mistaken for COBOL visibility across structural branches");
        assertEquals(List.of(wsX), table.lookupLocal(3, SymbolTable.Namespace.DATA, "WS-X"));
    }

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
        GrammarBinding binding = Bindings.cobol();
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(
                Files.readString(sourceFile, StandardCharsets.UTF_8),
                sourceFile.getFileName().toString(), SourceNormalizer.SourceFormat.FIXED);
        PreprocessorEngine.Outcome preprocessing = new PreprocessorEngine(
                binding, new CopybookLibrary(project.resolve("corpus/cpy")))
                .process(normalized.text(), sourceFile.getFileName().toString());
        String source = preprocessing.text();
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = binding.cobolParser(tokens);
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());

        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[] {0});
        return new AstBuilder(parser, source, ids, sizes)
                .build(tree).program();
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
