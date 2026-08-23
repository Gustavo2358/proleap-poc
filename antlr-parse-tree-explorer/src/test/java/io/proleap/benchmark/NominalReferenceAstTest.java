package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NominalReferenceAstTest {
    @Test
    void modelsProcedureReferencesInFlowAndPreservedStatements() throws Exception {
        Ast.Program ast = parse();
        List<Ast.GoToStatement> goTos = nodes(ast, Ast.GoToStatement.class);
        assertEquals("TARGET-PARA", goTos.get(0).targets().get(0).baseName());
        assertEquals(List.of("FIRST-PARA", "SECOND-PARA"), goTos.get(1).targets().stream()
                .map(Ast.ProcedureReference::baseName).toList());
        assertInstanceOf(Ast.DataReference.class, goTos.get(1).dependingOn());
        Ast.ProcedureReference qualified = goTos.get(2).targets().get(0);
        assertEquals("TARGET-SECTION", qualified.qualifier().sectionName());
        assertEquals(Ast.QualifierConnector.OF, qualified.qualifier().connector());

        List<Ast.PerformStatement> performs = nodes(ast, Ast.PerformStatement.class);
        assertEquals("FIRST-PARA", performs.get(0).fromReference().baseName());
        assertNull(performs.get(0).throughReference());
        assertEquals("SECOND-PARA", performs.get(1).throughReference().baseName());

        List<Ast.UnsupportedStatement> preserved = nodes(ast, Ast.UnsupportedStatement.class);
        Ast.UnsupportedStatement alter = byRule(preserved, "alterStatement");
        assertEquals(List.of("FIRST-PARA", "SECOND-PARA"), alter.recognizedReferences().stream()
                .filter(Ast.ProcedureReference.class::isInstance).map(Ast.ProcedureReference.class::cast)
                .map(Ast.ProcedureReference::baseName).toList());
        assertTrue(byRule(preserved, "sortStatement").recognizedReferences().stream()
                .anyMatch(Ast.FileReference.class::isInstance));
        assertTrue(byRule(preserved, "mergeStatement").recognizedReferences().stream()
                .filter(Ast.ProcedureReference.class::isInstance).count() >= 2);
    }

    @Test
    void modelsProgramAndFileNamesWithoutBindingAndKeepsAllReferencesReachable() throws Exception {
        Ast.Program ast = parse();
        Ast.CallStatement call = nodes(ast, Ast.CallStatement.class).get(0);
        Ast.ProgramReference program = assertInstanceOf(Ast.ProgramReference.class, call.target());
        assertEquals("STATIC-PGM", program.programName());
        assertEquals("'STATIC-PGM'", program.writtenText());

        List<Ast.FileReference> files = nodes(ast, Ast.FileReference.class);
        assertTrue(files.stream().map(Ast.FileReference::baseName).toList()
                .containsAll(List.of("SORT-FILE", "INPUT-FILE", "OUTPUT-FILE", "MERGE-FILE")));
        assertTrue(nodes(ast, Ast.ProcedureReference.class).stream().allMatch(reference ->
                reference.meta().span().startLine() > 0 && !reference.writtenText().isBlank()));
        assertTrue(AstSnapshot.from(ast).nodes().stream()
                .anyMatch(node -> node.type().equals("ProcedureReference")));
    }

    private static Ast.UnsupportedStatement byRule(List<Ast.UnsupportedStatement> statements, String rule) {
        return statements.stream().filter(statement -> statement.grammarRule().equals(rule)).findFirst().orElseThrow();
    }

    private static Ast.Program parse() throws Exception {
        Path file = Path.of("src/test/resources/cobol/semantic/nominal-references.cbl").toAbsolutePath();
        String source = SourceNormalizer.fixed(Files.readString(file, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.proleap();
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(source, file.getFileName().toString()));
        Parser parser = binding.cobolParser(new CommonTokenStream(lexer));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>(), sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        return new AstBuilder(parser, source, SourceMap.identity(source, file.getFileName().toString()), ids, sizes).build(tree).program();
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++); int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size); return size;
    }

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return result;
    }
}
