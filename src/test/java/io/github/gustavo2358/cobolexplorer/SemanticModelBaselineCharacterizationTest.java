package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Characterizes semantic facts without freezing the incidental total size of corpus trees. */
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

        assertSemanticMetrics(coactupc, "COACTUPC", 1, 0, 14, 0);
        assertSemanticMetrics(cbstm03a, "CBSTM03A", 14, 0, 0, 0);
        assertSemanticMetrics(cbstm03d, "CBSTM03D", 0, 14, 0, 0);

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
                "continueStatement", 57,
                "divideStatement", 1,
                "exitStatement", 49,
                "initializeStatement", 10,
                "inspectStatement", 4,
                "setStatement", 225,
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
    void coactupcPreservesCicsBuiltinsWithoutCreatingNominalReferences() throws Exception {
        Path project = Path.of("").toAbsolutePath().normalize();
        Analysis analysis = analyze(project.resolve("corpus/cbl/COACTUPC.cbl"), project.resolve("corpus/cpy"));

        Map<String, Integer> expected = Map.of("DFHRESP(NORMAL)", 7, "DFHRESP(NOTFND)", 3);
        assertEquals(expected, counts(descendants(
                analysis.tree(), CobolParser.CicsDfhRespLiteralContext.class).stream()
                .map(context -> analysis.tokens().getText(context.getStart(), context.getStop())).toList()));
        assertEquals(expected, counts(nodes(analysis.ast(), Ast.LiteralExpression.class).stream()
                .map(Ast.LiteralExpression::rawLexeme)
                .filter(text -> text.startsWith("DFHRESP(")).toList()));

        List<Ast.DataReference> references = nodes(analysis.ast(), Ast.DataReference.class);
        assertFalse(references.stream().map(Ast.DataReference::baseName)
                .anyMatch(SemanticModelBaselineCharacterizationTest::isCicsBuiltinPart));

        ResolutionContracts.ProgramUnitId unitId = new ResolutionContracts.ProgramUnitId(
                "COACTUPC.CBL", List.of(0), SymbolTable.canonical(analysis.ast().name()));
        ReferenceOccurrences occurrences = new ReferenceOccurrenceCollector().collect(
                unitId, analysis.ast(), AstScopeIndex.build(analysis.ast(), analysis.symbolTable()));
        assertFalse(occurrences.occurrences().stream()
                .map(ReferenceOccurrences.Occurrence::writtenText)
                .anyMatch(SemanticModelBaselineCharacterizationTest::isCicsBuiltinPart));

        assertTrue(descendants(analysis.tree(), CobolParser.TableCallContext.class).size() > 0,
                "the corpus must retain ordinary COBOL table/reference-modification calls");
        assertTrue(references.stream().anyMatch(reference -> !reference.subscriptGroups().isEmpty()
                        || reference.referenceModification() != null),
                "a real parenthesized COBOL data reference must remain structured in the AST");
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

    private static void assertSemanticMetrics(Analysis analysis, String programName,
                                              int literalTargetCalls, int identifierTargetCalls,
                                              int embedded, int unsupported) {
        AstSnapshot.Metrics metrics = analysis.snapshot().metrics();
        assertAll(programName,
                () -> assertEquals(programName, analysis.ast().name()),
                () -> assertEquals(literalTargetCalls, metrics.literalTargetCalls()),
                () -> assertEquals(identifierTargetCalls, metrics.identifierTargetCalls()),
                () -> assertEquals(embedded, metrics.embeddedLanguages()),
                () -> assertEquals(unsupported, metrics.unsupportedStatements()),
                () -> assertTrue(metrics.nodes() > 0),
                () -> assertTrue(analysis.symbolTable().scopes().size() > 0),
                () -> assertTrue(analysis.symbolTable().symbols().size() > 0));
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
        return new Analysis(ast, AstSnapshot.from(ast), new SymbolTableBuilder().build(ast), tree, tokens,
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

    private static <T extends ParserRuleContext> List<T> descendants(ParseTree root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (int i = 0; i < root.getChildCount(); i++) result.addAll(descendants(root.getChild(i), type));
        return result;
    }

    private static Map<String, Integer> counts(List<String> values) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String value : values) result.merge(value, 1, Integer::sum);
        return Map.copyOf(result);
    }

    private static boolean isCicsBuiltinPart(String text) {
        return List.of("DFHRESP", "DFHVALUE", "NORMAL", "NOTFND",
                "DFHRESP(NORMAL)", "DFHRESP(NOTFND)").contains(text);
    }

    private record Analysis(Ast.Program ast, AstSnapshot snapshot, SymbolTable symbolTable,
                            ParseTree tree, CommonTokenStream tokens,
                            int parserErrors, int unresolvedCopies) { }
}
