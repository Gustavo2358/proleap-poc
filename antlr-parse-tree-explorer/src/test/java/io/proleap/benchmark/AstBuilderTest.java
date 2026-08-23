package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AstBuilderTest {
    @Test
    void buildsTraceableSemanticAstForSelectedProgram() throws Exception {
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
        Ast.Program ast = new AstBuilder(parser, source, ids, sizes).build(tree).program();
        AstSnapshot snapshot = AstSnapshot.from(ast);

        assertEquals("COACTUPC", ast.name());
        assertEquals(4, ast.divisions().size());
        assertTrue(snapshot.metrics().nodes() > 3_000);
        assertTrue(snapshot.metrics().nodes() < sizes.get(tree));
        assertEquals(11, snapshot.metrics().maxDepth());
        assertEquals(1, snapshot.metrics().staticCalls());
        assertEquals(0, snapshot.metrics().dynamicCalls());
        assertTrue(snapshot.metrics().embeddedLanguages() > 0);

        List<Ast.Node> all = flatten(ast);
        Ast.CallStatement call = all.stream().filter(Ast.CallStatement.class::isInstance)
                .map(Ast.CallStatement.class::cast).findFirst().orElseThrow();
        assertEquals(Ast.CallTargetKind.STATIC_LITERAL, call.targetKind());
        assertInstanceOf(Ast.ProgramReference.class, call.target());
        assertEquals("CSUTLDTC", ((Ast.ProgramReference) call.target()).programName());
        assertTrue(call.meta().origin().rootNodeId() >= 0);
        assertEquals("callStatement", call.meta().origin().grammarRule());
        assertTrue(call.meta().origin().subtreeNodeCount() > 1);

        Ast.Sentence sentence = all.stream().filter(Ast.Sentence.class::isInstance)
                .map(Ast.Sentence.class::cast).findFirst().orElseThrow();
        assertEquals(Ast.SentenceTerminator.PERIOD, sentence.terminator());
        assertTrue(sentence.terminatorSpan().startLine() >= sentence.meta().span().startLine());

        Ast.EmbeddedLanguageStatement embedded = all.stream()
                .filter(Ast.EmbeddedLanguageStatement.class::isInstance)
                .map(Ast.EmbeddedLanguageStatement.class::cast).findFirst().orElseThrow();
        assertEquals(Ast.EmbeddedLanguage.CICS, embedded.language());
        assertTrue(embedded.rawText().contains("EXEC CICS"));
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
