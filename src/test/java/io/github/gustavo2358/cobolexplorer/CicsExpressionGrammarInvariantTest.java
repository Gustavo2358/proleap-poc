package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolLexer;
import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CicsExpressionGrammarInvariantTest {

    private static final List<HostCase> HOSTS = List.of(
            host("relational arithmetic operand", "IF X = %s CONTINUE END-IF.",
                    CobolParser.RelationArithmeticComparisonContext.class),
            host("standalone arithmetic expression", "COMPUTE X = %s.",
                    CobolParser.BasisContext.class),
            host("EVALUATE subject", "EVALUATE %s WHEN 0 CONTINUE END-EVALUATE.",
                    CobolParser.EvaluateSelectContext.class),
            host("EVALUATE selector", "EVALUATE X WHEN %s CONTINUE END-EVALUATE.",
                    CobolParser.EvaluateValueContext.class),
            host("MOVE sending operand", "MOVE %s TO X.",
                    CobolParser.MoveToSendingAreaContext.class),
            host("DISPLAY operand", "DISPLAY %s.",
                    CobolParser.DisplayOperandContext.class),
            host("ADD operand", "ADD %s TO X.",
                    CobolParser.AddFromContext.class),
            host("SUBTRACT operand", "SUBTRACT %s FROM X.",
                    CobolParser.SubtractSubtrahendContext.class),
            host("MULTIPLY operand", "MULTIPLY %s BY X.",
                    CobolParser.MultiplyStatementContext.class),
            host("DIVIDE operand", "DIVIDE %s INTO X.",
                    CobolParser.DivideStatementContext.class),
            host("PERFORM FROM value", "PERFORM VARYING X FROM %s BY 1 UNTIL X > 9 CONTINUE END-PERFORM.",
                    CobolParser.PerformFromContext.class),
            host("SET increment", "SET X UP BY %s.",
                    CobolParser.SetByValueContext.class),
            host("ENABLE key literal-first control", "ENABLE INPUT TERMINAL CHANNEL WITH KEY %s.",
                    CobolParser.EnableStatementContext.class)
    );

    private static final List<CicsCase> CICS_CASES = List.of(
            new CicsCase("DFHRESP(NORMAL)", CobolParser.CicsDfhRespLiteralContext.class),
            new CicsCase("DFHRESP(INVREQ)", CobolParser.CicsDfhRespLiteralContext.class),
            new CicsCase("DFHVALUE(EMPTYREQ)", CobolParser.CicsDfhValueLiteralContext.class),
            new CicsCase("DFHVALUE(FULLAPI)", CobolParser.CicsDfhValueLiteralContext.class)
    );

    @TestFactory
    Stream<DynamicTest> cicsExpressionInvariantAcrossGrammarDerivedHosts() {
        return HOSTS.stream().flatMap(host -> CICS_CASES.stream().map(cics -> DynamicTest.dynamicTest(
                host.name() + " / " + cics.expression(), () -> assertSpecialized(host, cics))));
    }

    @TestFactory
    Stream<DynamicTest> ordinaryTableCallsRemainTableCallsInTheSameHosts() {
        return HOSTS.stream().map(host -> DynamicTest.dynamicTest(
                host.name() + " / MY-TABLE(IDX)", () -> assertOrdinaryTableCall(host)));
    }

    @Test
    void specializedCicsLiteralsDoNotLeakNominalReferencesButTableCallsStillDo() {
        Parsed parsed = parse("""
                IF X = DFHRESP(NORMAL) CONTINUE END-IF.
                IF X = DFHVALUE(EMPTYREQ) CONTINUE END-IF.
                EVALUATE X WHEN DFHRESP(INVREQ) CONTINUE END-EVALUATE.
                EVALUATE X WHEN DFHVALUE(FULLAPI) CONTINUE END-EVALUATE.
                IF X = MY-TABLE(IDX) CONTINUE END-IF.
                """);
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(parsed.tree(), ids, sizes, new int[]{0});
        Ast.Program program = new AstBuilder(parsed.parser(), parsed.source(),
                SourceMap.identity(parsed.source(), "cics-expression-invariant.cbl"), ids, sizes)
                .build(parsed.tree()).program();

        assertEquals(List.of(
                        "DFHRESP(NORMAL)", "DFHVALUE(EMPTYREQ)",
                        "DFHRESP(INVREQ)", "DFHVALUE(FULLAPI)"),
                astNodes(program, Ast.LiteralExpression.class).stream()
                        .map(Ast.LiteralExpression::rawLexeme)
                        .filter(text -> text.startsWith("DFH"))
                        .toList());
        List<String> astReferenceNames = astNodes(program, Ast.DataReference.class).stream()
                .map(Ast.DataReference::baseName).toList();
        assertTrue(astReferenceNames.containsAll(List.of("MY-TABLE", "IDX")));
        assertFalse(astReferenceNames.stream().anyMatch(CicsExpressionGrammarInvariantTest::isCicsBuiltinPart));

        SymbolTable table = new SymbolTableBuilder().build(program);
        ResolutionContracts.ProgramUnitId unitId = new ResolutionContracts.ProgramUnitId(
                "CICS-EXPRESSION-INVARIANT.CBL", List.of(0), SymbolTable.canonical(program.name()));
        ReferenceOccurrences occurrences = new ReferenceOccurrenceCollector().collect(
                unitId, program, AstScopeIndex.build(program, table));
        List<String> occurrenceTexts = occurrences.occurrences().stream()
                .map(ReferenceOccurrences.Occurrence::writtenText).toList();

        assertFalse(occurrenceTexts.stream().anyMatch(CicsExpressionGrammarInvariantTest::isCicsBuiltinPart),
                occurrenceTexts.toString());
        assertTrue(occurrenceTexts.contains("MY-TABLE(IDX)"), occurrenceTexts.toString());
        assertTrue(occurrenceTexts.contains("IDX"), occurrenceTexts.toString());
    }

    private static void assertSpecialized(HostCase host, CicsCase cics) {
        Parsed parsed = parse(host.statement().apply(cics.expression()));
        List<? extends ParserRuleContext> specialized = descendants(parsed.tree(), cics.contextType()).stream()
                .filter(context -> text(parsed.tokens(), context).equals(cics.expression()))
                .toList();
        List<CobolParser.TableCallContext> equivalentTableCalls = descendants(
                parsed.tree(), CobolParser.TableCallContext.class).stream()
                .filter(context -> text(parsed.tokens(), context).equals(cics.expression()))
                .toList();

        assertAll(
                () -> assertEquals(1, specialized.size(),
                        "CICS-EXPRESSION-001 requires one specialized context"),
                () -> assertTrue(equivalentTableCalls.isEmpty(),
                        "CICS-EXPRESSION-001 forbids an equivalent tableCall context"));
        assertNotNull(ancestor(specialized.get(0), host.contextType()),
                "the specialized value must remain inside the grammar-derived host");
    }

    private static void assertOrdinaryTableCall(HostCase host) {
        String expression = "MY-TABLE(IDX)";
        Parsed parsed = parse(host.statement().apply(expression));
        List<CobolParser.TableCallContext> tableCalls = descendants(
                parsed.tree(), CobolParser.TableCallContext.class).stream()
                .filter(context -> text(parsed.tokens(), context).equals(expression))
                .toList();

        assertEquals(1, tableCalls.size(), "the negative control must remain a tableCall");
        assertFalse(descendants(parsed.tree(), CobolParser.CicsDfhRespLiteralContext.class).stream()
                .anyMatch(context -> text(parsed.tokens(), context).equals(expression)));
        assertFalse(descendants(parsed.tree(), CobolParser.CicsDfhValueLiteralContext.class).stream()
                .anyMatch(context -> text(parsed.tokens(), context).equals(expression)));
        assertNotNull(ancestor(tableCalls.get(0), host.contextType()),
                "the ordinary table call must remain inside the same grammar-derived host");
    }

    private static Parsed parse(String statement) {
        String source = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CICS-INVARIANT.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 X PIC S9(9) COMP.
                01 IDX PIC 9.
                01 MY-TABLE OCCURS 9 TIMES PIC S9(9) COMP.
                PROCEDURE DIVISION.
                %s
                GOBACK.
                """.formatted(statement);
        CobolLexer lexer = new CobolLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CobolParser parser = new CobolParser(tokens);
        ParseTree tree = parser.startRule();
        assertEquals(0, parser.getNumberOfSyntaxErrors(), statement);
        return new Parsed(tree, tokens, parser, source);
    }

    private static String text(CommonTokenStream tokens, ParserRuleContext context) {
        Token start = context.getStart();
        Token stop = context.getStop();
        return tokens.getText(start, stop);
    }

    private static <T extends ParserRuleContext> T ancestor(
            ParserRuleContext context, Class<T> type) {
        for (ParseTree current = context; current != null; current = current.getParent()) {
            if (type.isInstance(current)) return type.cast(current);
        }
        return null;
    }

    private static <T extends ParserRuleContext> List<T> descendants(ParseTree root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (int i = 0; i < root.getChildCount(); i++) {
            result.addAll(descendants(root.getChild(i), type));
        }
        return result;
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

    private static <T extends Ast.Node> List<T> astNodes(Ast.Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(astNodes(child, type));
        return result;
    }

    private static boolean isCicsBuiltinPart(String text) {
        return List.of("DFHRESP", "DFHVALUE", "NORMAL", "INVREQ", "EMPTYREQ", "FULLAPI").contains(text);
    }

    private static HostCase host(String name, String statement, Class<? extends ParserRuleContext> contextType) {
        return new HostCase(name, expression -> statement.formatted(expression), contextType);
    }

    private record HostCase(String name, Function<String, String> statement,
                            Class<? extends ParserRuleContext> contextType) {}

    private record CicsCase(String expression, Class<? extends ParserRuleContext> contextType) {}

    private record Parsed(ParseTree tree, CommonTokenStream tokens, CobolParser parser, String source) {}
}
