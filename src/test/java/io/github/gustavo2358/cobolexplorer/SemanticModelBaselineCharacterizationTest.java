package io.github.gustavo2358.cobolexplorer;

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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Characterizes semantic facts without freezing incidental corpus-wide cardinalities. */
class SemanticModelBaselineCharacterizationTest {

    @Test
    void characterizesCurrentDemonstrationPrograms() throws Exception {
        Path project = Path.of("").toAbsolutePath().normalize();
        Analysis coactupc = analyze(project.resolve("corpus/cbl/COACTUPC.cbl"), project.resolve("corpus/cpy"));
        Analysis cbstm03a = analyze(project.resolve("corpus/cbl/CBSTM03A.CBL"), project.resolve("corpus/cpy"));
        Analysis cbstm03d = analyze(project.resolve("corpus/cbl/CBSTM03D.CBL"), project.resolve("corpus/cpy"));

        assertAll("frontend baseline",
                () -> assertEquals(0, coactupc.parserErrors()),
                () -> assertEquals(0, cbstm03a.parserErrors()),
                () -> assertEquals(0, cbstm03d.parserErrors()),
                () -> assertEquals(3, coactupc.unresolvedCopies()),
                () -> assertEquals(0, cbstm03a.unresolvedCopies()),
                () -> assertEquals(0, cbstm03d.unresolvedCopies()));

        assertEquals("COACTUPC", coactupc.ast().name());
        assertEquals("CBSTM03A", cbstm03a.ast().name());
        assertEquals("CBSTM03D", cbstm03d.ast().name());

        List<Ast.CallStatement> coactupcCalls = nodes(coactupc.ast(), Ast.CallStatement.class);
        assertEquals(1, coactupcCalls.size(), "the source has one known literal CALL from CSUTLDPY");
        Ast.CallStatement coactupcCall = coactupcCalls.get(0);
        assertEquals(Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME, coactupcCall.targetSyntax());
        Ast.ProgramReference programTarget = assertInstanceOf(Ast.ProgramReference.class, coactupcCall.target());
        assertEquals("CSUTLDTC", programTarget.programName());

        List<Ast.CallStatement> literalCalls = nodes(cbstm03a.ast(), Ast.CallStatement.class);
        assertFalse(literalCalls.isEmpty());
        assertTrue(literalCalls.stream().allMatch(call ->
                call.targetSyntax() == Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME
                        && call.target() instanceof Ast.ProgramReference));

        List<Ast.CallStatement> dynamicCalls = nodes(cbstm03d.ast(), Ast.CallStatement.class);
        assertFalse(dynamicCalls.isEmpty());
        assertTrue(dynamicCalls.stream().allMatch(call ->
                call.targetSyntax() == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION
                        && call.target() instanceof Ast.DataReference reference
                        && reference.writtenName().equals("WS-CALL-TARGET")));

        List<Ast.EmbeddedLanguageStatement> embedded = nodes(
                coactupc.ast(), Ast.EmbeddedLanguageStatement.class);
        assertFalse(embedded.isEmpty());
        assertTrue(embedded.stream().allMatch(statement ->
                statement.language() == Ast.EmbeddedLanguage.CICS
                        && statement.rawText().contains("EXEC CICS")));

        assertModeledRules(coactupc.ast(), "computeStatement", "setStatement", "stringStatement");
        assertModeledRules(cbstm03d.ast(), "writeStatement");
        assertPreservedRules(cbstm03d.ast(), "alterStatement", "displayStatement");
    }

    @Test
    void preservesTheWrittenFormInsteadOfConcatenatingParserTokens() throws Exception {
        Path project = Path.of("").toAbsolutePath().normalize();
        Analysis analysis = analyze(project.resolve("corpus/cbl/COACTUPC.cbl"), project.resolve("corpus/cpy"));
        List<String> names = nodes(analysis.ast(), Ast.DataReference.class).stream()
                .map(Ast.DataReference::writtenName)
                .toList();

        assertTrue(names.contains("ACCTSIDI OF CACTUPAI"),
                "OF qualification keeps the source spelling and separators");
        assertTrue(names.contains("DFHCOMMAREA (1:LENGTH OF CARDDEMO-COMMAREA)"),
                "reference modification text remains faithfully preserved until structural modeling");
    }

    private static void assertModeledRules(Ast.Program program, String... expectedRules) {
        List<Ast.ModeledStatement> modeled = nodes(program, Ast.ModeledStatement.class);
        List<Ast.PreservedStatement> preserved = nodes(program, Ast.PreservedStatement.class);
        for (String expectedRule : expectedRules) {
            assertTrue(modeled.stream().anyMatch(statement -> statement.grammarRule().equals(expectedRule)),
                    () -> expectedRule + " must remain structurally modeled");
            assertTrue(preserved.stream().noneMatch(statement -> statement.grammarRule().equals(expectedRule)),
                    () -> expectedRule + " must not fall back to preserved text");
        }
    }

    private static void assertPreservedRules(Ast.Program program, String... expectedRules) {
        List<Ast.ModeledStatement> modeled = nodes(program, Ast.ModeledStatement.class);
        List<Ast.PreservedStatement> preserved = nodes(program, Ast.PreservedStatement.class);
        for (String expectedRule : expectedRules) {
            assertTrue(preserved.stream().anyMatch(statement -> statement.grammarRule().equals(expectedRule)),
                    () -> expectedRule + " must remain explicitly preserved");
            assertTrue(modeled.stream().noneMatch(statement -> statement.grammarRule().equals(expectedRule)),
                    () -> expectedRule + " must not be reported as semantically modeled");
        }
    }

    private static Analysis analyze(Path sourceFile, Path copybooks) throws Exception {
        GrammarBinding binding = Bindings.cobol();
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(
                Files.readString(sourceFile, StandardCharsets.UTF_8),
                sourceFile.getFileName().toString(), SourceNormalizer.SourceFormat.FIXED);
        PreprocessorEngine.Outcome preprocessing =
                new PreprocessorEngine(binding, new CopybookLibrary(copybooks))
                        .process(normalized.sourceMap(), sourceFile.getFileName().toString());
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(preprocessing.text()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = binding.cobolParser(tokens);
        ParseTree tree = binding.cobolStart(parser);

        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        Ast.Program ast = new AstBuilder(parser, preprocessing.text(), preprocessing.sourceMap(),
                ids, sizes).build(tree).program();
        return new Analysis(ast, parser.getNumberOfSyntaxErrors(), preprocessing.unresolved());
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

    private record Analysis(Ast.Program ast, int parserErrors, int unresolvedCopies) { }
}
