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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Freezes the semantic facts that exist before semantic-model hardening starts. */
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

        assertMetrics(coactupc, "COACTUPC", 9_189, 11, 1, 0, 14, 0, 651, 853, 2);
        assertMetrics(cbstm03a, "CBSTM03A", 2_740, 11, 14, 0, 0, 0, 219, 209, 0);
        assertMetrics(cbstm03d, "CBSTM03D", 2_752, 11, 0, 14, 0, 0, 221, 211, 0);

        List<Ast.CallStatement> dynamicCalls = nodes(cbstm03d.ast(), Ast.CallStatement.class);
        assertEquals(14, dynamicCalls.size());
        assertTrue(dynamicCalls.stream().allMatch(call ->
                call.targetSyntax() == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION
                        && call.target() instanceof Ast.DataReference reference
                        && reference.writtenName().equals("WS-CALL-TARGET")));
        assertEquals(1, cbstm03d.symbolTable().lookupAll(
                SymbolTable.Namespace.DATA, "WS-CALL-TARGET").size());

        assertEquals(Map.of(
                "computeStatement", 9,
                "continueStatement", 59,
                "divideStatement", 1,
                "exitStatement", 49,
                "initializeStatement", 10,
                "inspectStatement", 4,
                "setStatement", 226,
                "stringStatement", 53), structuredByRule(coactupc.ast()));
        assertEquals(Map.ofEntries(
                Map.entry("addStatement", 3),
                Map.entry("alterStatement", 4),
                Map.entry("closeStatement", 1),
                Map.entry("computeStatement", 4),
                Map.entry("continueStatement", 12),
                Map.entry("displayStatement", 33),
                Map.entry("exitStatement", 17),
                Map.entry("gobackStatement", 1),
                Map.entry("initializeStatement", 2),
                Map.entry("openStatement", 1),
                Map.entry("setStatement", 83),
                Map.entry("stringStatement", 12),
                Map.entry("writeStatement", 95)), structuredByRule(cbstm03d.ast()));
    }

    @Test
    void preservesTheWrittenFormInsteadOfConcatenatingParserTokens() throws Exception {
        Path project = Path.of("").toAbsolutePath().normalize();
        Analysis analysis = analyze(project.resolve("corpus/cbl/COACTUPC.cbl"), project.resolve("corpus/cpy"));
        List<String> names = nodes(analysis.ast(), Ast.DataReference.class).stream()
                .map(Ast.DataReference::writtenName)
                .toList();

        assertEquals(2_871, names.size());
        assertTrue(names.contains("ACCTSIDI OF CACTUPAI"),
                "OF qualification keeps the source spelling and separators");
        assertTrue(names.contains("DFHCOMMAREA (1:LENGTH OF CARDDEMO-COMMAREA)"),
                "reference modification text remains faithfully preserved until structural modeling");
    }

    private static void assertMetrics(Analysis analysis, String programName, int astNodes, int maxDepth,
                                      int literalTargetCalls, int identifierTargetCalls,
                                      int embedded, int unsupported,
                                      int scopes, int symbols, int symbolDiagnostics) {
        AstSnapshot.Metrics metrics = analysis.snapshot().metrics();
        assertAll(programName,
                () -> assertEquals(programName, analysis.ast().name()),
                () -> assertEquals(astNodes, metrics.nodes()),
                () -> assertEquals(maxDepth, metrics.maxDepth()),
                () -> assertEquals(literalTargetCalls, metrics.literalTargetCalls()),
                () -> assertEquals(identifierTargetCalls, metrics.identifierTargetCalls()),
                () -> assertEquals(embedded, metrics.embeddedLanguages()),
                () -> assertEquals(unsupported, metrics.unsupportedStatements()),
                () -> assertEquals(scopes, analysis.symbolTable().scopes().size()),
                () -> assertEquals(symbols, analysis.symbolTable().symbols().size()),
                () -> assertEquals(symbolDiagnostics, analysis.symbolTable().diagnostics().size()));
    }

    private static Map<String, Integer> structuredByRule(Ast.Program program) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Ast.ModeledStatement statement : nodes(program, Ast.ModeledStatement.class))
            counts.merge(statement.grammarRule(), 1, Integer::sum);
        for (Ast.PreservedStatement statement : nodes(program, Ast.PreservedStatement.class))
            counts.merge(statement.grammarRule(), 1, Integer::sum);
        return Map.copyOf(counts);
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
        return new Analysis(ast, AstSnapshot.from(ast), new SymbolTableBuilder().build(ast),
                parser.getNumberOfSyntaxErrors(), preprocessing.unresolved());
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

    private record Analysis(Ast.Program ast, AstSnapshot snapshot, SymbolTable symbolTable,
                            int parserErrors, int unresolvedCopies) { }
}
