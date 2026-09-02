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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredExpressionAstTest {

    @Test
    void modelsEveryGrammarDimensionOfADataReference() throws Exception {
        Ast.Program ast = parseFixture("references.cbl");
        List<Ast.MoveStatement> moves = nodes(ast, Ast.MoveStatement.class);

        Ast.DataReference simple = assertInstanceOf(Ast.DataReference.class, moves.get(0).source());
        assertEquals("WS-SIMPLE", simple.baseName());
        assertEquals("WS-SIMPLE", simple.writtenText());
        assertTrue(simple.qualifiers().isEmpty());
        assertTrue(simple.subscriptGroups().isEmpty());
        assertNull(simple.referenceModification());

        Ast.DataReference qualifiedOf = assertInstanceOf(Ast.DataReference.class, moves.get(1).source());
        assertEquals(List.of(Ast.QualifierConnector.OF), connectors(qualifiedOf));
        assertEquals(List.of("GROUP-A"), qualifierNames(qualifiedOf));
        assertEquals("WS-SIMPLE OF GROUP-A", qualifiedOf.writtenText());

        Ast.DataReference qualifiedIn = assertInstanceOf(Ast.DataReference.class, moves.get(2).source());
        assertEquals(List.of(Ast.QualifierConnector.IN), connectors(qualifiedIn));

        Ast.DataReference multiple = assertInstanceOf(Ast.DataReference.class, moves.get(3).source());
        assertEquals(List.of("GROUP-A", "OUTER-GROUP"), qualifierNames(multiple));
        assertTrue(multiple.qualifiers().get(0).meta().span().startLine() > 0);

        Ast.DataReference subscripted = assertInstanceOf(Ast.DataReference.class, moves.get(4).source());
        assertEquals(1, subscripted.subscriptGroups().size(), subscripted.toString());
        assertInstanceOf(Ast.DataReference.class, subscripted.subscriptGroups().get(0).subscripts().get(0));

        Ast.DataReference qualifiedAndSubscripted = assertInstanceOf(Ast.DataReference.class, moves.get(5).source());
        assertEquals("GROUP-A", qualifiedAndSubscripted.qualifiers().get(0).name());
        assertEquals(1, qualifiedAndSubscripted.qualifiers().get(0).reference().subscriptGroups().size());

        Ast.DataReference relativeSubscript = assertInstanceOf(Ast.DataReference.class, moves.get(6).source());
        Ast.OperationExpression relative = assertInstanceOf(Ast.OperationExpression.class,
                relativeSubscript.subscriptGroups().get(0).subscripts().get(0));
        assertEquals("RELATIVE_SUBSCRIPT", relative.operator());
        assertEquals(2, relative.operands().size());

        Ast.DataReference multiSubscript = assertInstanceOf(Ast.DataReference.class, moves.get(7).source());
        assertEquals(2, multiSubscript.subscriptGroups().size());
        assertEquals(2, multiSubscript.subscriptGroups().get(0).subscripts().size(), multiSubscript.toString());
        assertEquals(1, multiSubscript.subscriptGroups().get(1).subscripts().size());

        Ast.DataReference modified = assertInstanceOf(Ast.DataReference.class, moves.get(8).source());
        assertInstanceOf(Ast.DataReference.class, modified.referenceModification().offset());
        assertInstanceOf(Ast.DataReference.class, modified.referenceModification().length());
        assertEquals("WS-SIMPLE(WS-START:WS-LENGTH)", modified.writtenText());

        Ast.DataReference openEnded = assertInstanceOf(Ast.DataReference.class, moves.get(9).source());
        assertInstanceOf(Ast.DataReference.class, openEnded.referenceModification().offset());
        assertNull(openEnded.referenceModification().length());

        List<Ast.Node> reachable = nodes(ast, Ast.Node.class);
        assertTrue(reachable.containsAll(multiple.qualifiers()));
        assertTrue(reachable.containsAll(multiSubscript.subscriptGroups()));
        assertTrue(reachable.contains(modified.referenceModification()));
        assertTrue(nodes(ast, Ast.DataReference.class).stream().allMatch(reference ->
                reference.meta().span().startLine() > 0
                        && reference.meta().provenance().original().file().equals("references.cbl")
                        && !reference.writtenText().isBlank()));

        AstSnapshot snapshot = AstSnapshot.from(ast);
        long snapshotReferences = snapshot.nodes().stream()
                .filter(node -> node.type().equals("DataReference")).count();
        assertEquals(nodes(ast, Ast.DataReference.class).size(), snapshotReferences,
                "Ast.children and snapshot must expose every modeled reference");
    }

    @Test
    void structuresArithmeticConditionsFunctionsRegistersAndPerformControl() throws Exception {
        Ast.Program ast = parseFixture("expressions.cbl");
        Ast.IfStatement ifStatement = nodes(ast, Ast.IfStatement.class).get(0);
        Ast.LogicalCondition condition = assertInstanceOf(Ast.LogicalCondition.class,
                ifStatement.condition());
        assertEquals(Ast.LogicalConnector.AND, condition.connector());
        assertTrue(nodes(condition, Ast.DataReference.class).stream()
                .map(Ast.DataReference::baseName).toList().containsAll(List.of("A", "B")));

        Ast.FunctionExpression function = nodes(ifStatement, Ast.FunctionExpression.class).get(0);
        assertEquals("MAX", function.functionName());
        assertEquals(2, function.arguments().size());
        assertEquals("FUNCTION MAX(A, B)", function.writtenText());

        Ast.SpecialRegisterExpression register = nodes(ifStatement, Ast.SpecialRegisterExpression.class).get(0);
        assertEquals("RETURN-CODE", register.registerName());

        Ast.RelationCondition abbreviated = nodes(ast, Ast.RelationCondition.class).stream()
                .filter(relation -> relation.subject() == null)
                .findFirst().orElseThrow();
        assertEquals("abbreviation", abbreviated.meta().origin().grammarRule());
        assertNull(abbreviated.relationalOperator(),
                "subject and operator omitted must stay omitted on the surface");
        assertInstanceOf(Ast.LiteralExpression.class, abbreviated.object(),
                "the written abbreviated object remains reachable");

        Ast.EvaluateStatement evaluate = nodes(ast, Ast.EvaluateStatement.class).get(0);
        assertInstanceOf(Ast.OperationExpression.class, evaluate.subjects().get(0));
        assertEquals(2, evaluate.subjects().size());
        assertEquals(2, evaluate.branches().get(0).selectorExpressions().size());
        assertTrue(evaluate.branches().get(0).selectorExpressions().stream()
                .allMatch(expression -> expression.meta().span().startLine() > 0));

        Ast.PerformStatement perform = nodes(ast, Ast.PerformStatement.class).stream()
                .filter(statement -> statement.performKind() == Ast.PerformKind.INLINE).findFirst().orElseThrow();
        assertFalse(perform.controlExpressions().isEmpty());
        assertTrue(nodes(perform, Ast.DataReference.class).stream()
                .map(Ast.DataReference::baseName).toList().containsAll(List.of("A", "TARGET")));

        AstSnapshot snapshot = AstSnapshot.from(ast);
        assertTrue(snapshot.nodes().stream().anyMatch(node -> node.type().equals("FunctionExpression")));
        assertTrue(snapshot.nodes().stream().anyMatch(node -> node.type().equals("SpecialRegisterExpression")));
        assertTrue(snapshot.nodes().stream().anyMatch(node -> node.type().equals("OperationExpression")));
        assertTrue(snapshot.nodes().stream().anyMatch(node -> node.type().equals("LogicalCondition")));
    }

    private static List<Ast.QualifierConnector> connectors(Ast.DataReference reference) {
        return reference.qualifiers().stream().map(Ast.DataQualifier::connector).toList();
    }

    private static List<String> qualifierNames(Ast.DataReference reference) {
        return reference.qualifiers().stream().map(Ast.DataQualifier::name).toList();
    }

    private static Ast.Program parseFixture(String fileName) throws Exception {
        Path file = Path.of("src/test/resources/cobol/semantic", fileName).toAbsolutePath().normalize();
        String source = SourceNormalizerTestSupport.fixed(Files.readString(file, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.cobol();
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(source, fileName));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = binding.cobolParser(tokens);
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        return new AstBuilder(parser, source, SourceMap.identity(source, fileName), ids, sizes)
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

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return result;
    }
}
