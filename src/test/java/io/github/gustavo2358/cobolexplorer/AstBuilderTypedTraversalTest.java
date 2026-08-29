package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolBaseVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstBuilderTypedTraversalTest {

    @Test
    void builderUsesTheGeneratedCobolVisitor() {
        assertTrue(CobolBaseVisitor.class.isAssignableFrom(AstBuilder.class));
    }

    @Test
    void programNameTokensDoNotBecomeProgramAttributes() {
        for (String name : List.of("LIBRARY", "RECURSIVE", "DEFINITION")) {
            Ast.Program program = parse("""
                    IDENTIFICATION DIVISION.
                    PROGRAM-ID. %s.
                    PROCEDURE DIVISION.
                    GOBACK.
                    """.formatted(name));

            assertEquals(name, program.name());
            assertFalse(program.attributes().library(), name);
            assertFalse(program.attributes().recursive(), name);
            assertFalse(program.attributes().definition(), name);
        }
    }

    @Test
    void directProgramAttributesRemainModeled() {
        Ast.Program library = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE IS LIBRARY PROGRAM.
                PROCEDURE DIVISION.
                GOBACK.
                """);
        Ast.Program recursive = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE IS RECURSIVE PROGRAM.
                PROCEDURE DIVISION.
                GOBACK.
                """);
        Ast.Program definition = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE IS DEFINITION PROGRAM.
                PROCEDURE DIVISION.
                GOBACK.
                """);

        assertTrue(library.attributes().library());
        assertTrue(recursive.attributes().recursive());
        assertTrue(definition.attributes().definition());
    }

    @Test
    void nestedEndIfDoesNotTerminateOuterIf() {
        Ast.Program program = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. NESTED-IF.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC 9.
                01 B PIC 9.
                PROCEDURE DIVISION.
                IF A = B IF A = B CONTINUE END-IF.
                """);

        assertEquals(List.of(false, true), nodes(program, Ast.IfStatement.class).stream()
                .map(Ast.IfStatement::explicitlyTerminated).toList());
    }

    @Test
    void nestedEndEvaluateDoesNotTerminateOuterEvaluate() {
        Ast.Program program = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. NESTED-EVALUATE.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC 9.
                01 B PIC 9.
                PROCEDURE DIVISION.
                EVALUATE A
                    WHEN 1
                        EVALUATE B WHEN 2 CONTINUE END-EVALUATE
                .
                """);

        assertEquals(List.of(false, true), nodes(program, Ast.EvaluateStatement.class).stream()
                .map(Ast.EvaluateStatement::explicitlyTerminated).toList());
    }

    @Test
    void outerEvaluateUsesItsDirectWhenOtherInsteadOfNestedOne() {
        Ast.Program program = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. NESTED-OTHER.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC 9.
                01 B PIC 9.
                01 RESULT PIC 9.
                PROCEDURE DIVISION.
                EVALUATE A
                    WHEN 1
                        EVALUATE B
                            WHEN 1 CONTINUE
                            WHEN OTHER SET RESULT TO TRUE
                        END-EVALUATE
                    WHEN OTHER MOVE 1 TO RESULT
                END-EVALUATE.
                """);

        List<Ast.EvaluateStatement> evaluates = nodes(program, Ast.EvaluateStatement.class);
        Ast.EvaluateStatement outer = evaluates.get(0);
        Ast.EvaluateStatement nested = evaluates.get(1);
        assertEquals(2, outer.branches().size());
        assertEquals(2, nested.branches().size());
        assertTrue(outer.branches().get(1).statements().get(0) instanceof Ast.MoveStatement);
        assertTrue(nested.branches().get(1).statements().get(0) instanceof Ast.ModeledStatement);
    }

    @Test
    void fillerNameIsNotBorrowedFromARedefinesClause() {
        Ast.Program program = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. FILLER-NAME.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 BASE-ITEM PIC X.
                01 FILLER REDEFINES BASE-ITEM PIC X.
                PROCEDURE DIVISION.
                GOBACK.
                """);

        List<Ast.DataEntry> entries = nodes(program, Ast.DataEntry.class);
        assertEquals(1, entries.stream().filter(entry -> entry.name().equals("BASE-ITEM")).count());
        Ast.DataEntry filler = entries.stream().filter(Ast.DataEntry::filler).findFirst().orElseThrow();
        assertEquals("FILLER", filler.name());
        assertTrue(filler.clauses().stream().anyMatch(Ast.RedefinesClause.class::isInstance));
    }

    @Test
    void anonymousDataNamesAreNotBorrowedFromAnyNestedClauseReference() {
        Ast.Program program = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. ANONYMOUS-DATA.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 ROOT-ITEM.
                   05 BASE-ITEM PIC X.
                   05 COUNT-ITEM PIC 9.
                   05 KEY-ITEM PIC X.
                   05 CONV-NAME PIC X.
                   05 FILLER REDEFINES BASE-ITEM PIC X.
                   05 REDEFINES BASE-ITEM PIC X.
                   05 FILLER OCCURS 1 TO 2 TIMES
                             DEPENDING ON COUNT-ITEM PIC X.
                   05 FILLER OCCURS 2 TIMES
                             ASCENDING KEY IS KEY-ITEM PIC X.
                   05 FILLER USING CONVENTION OF CONV-NAME PIC X.
                PROCEDURE DIVISION.
                GOBACK.
                """);

        List<Ast.DataEntry> entries = nodes(program, Ast.DataEntry.class);
        assertEquals(5, entries.stream().filter(Ast.DataEntry::filler).count());
        for (String declaredName : List.of("BASE-ITEM", "COUNT-ITEM", "KEY-ITEM", "CONV-NAME")) {
            assertEquals(1, entries.stream().filter(entry -> entry.name().equals(declaredName)).count(),
                    declaredName + " must only be the direct declaration");
        }
    }

    @Test
    void relationalCategoryComesFromTheGrammarContextNotOperatorSpelling() {
        Ast.Program program = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. RELATION-CATEGORY.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 LEFT-VALUE PIC 9.
                01 RIGHT-VALUE PIC 9.
                PROCEDURE DIVISION.
                IF LEFT-VALUE = RIGHT-VALUE CONTINUE END-IF.
                IF LEFT-VALUE GREATER THAN RIGHT-VALUE CONTINUE END-IF.
                IF LEFT-VALUE IS NOT LESS THAN RIGHT-VALUE CONTINUE END-IF.
                GOBACK.
                """);

        List<Ast.OperationExpression> relations = nodes(program, Ast.OperationExpression.class).stream()
                .filter(operation -> "relationArithmeticComparison".equals(
                        operation.meta().origin().grammarRule()))
                .toList();
        assertEquals(3, relations.size());
        assertTrue(relations.stream().allMatch(operation ->
                operation.category() == Ast.OperationCategory.RELATIONAL));
    }

    @Test
    void setBooleanTargetsCarryTheConditionContextFromTypedGrammarChildren() {
        Ast.Program program = parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SET-CONTEXT.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 STATE-FIELD PIC X.
                   88 STATE-OPEN VALUE 'O'.
                01 SOURCE-VALUE PIC X.
                PROCEDURE DIVISION.
                SET STATE-OPEN TO TRUE.
                SET SOURCE-VALUE TO STATE-FIELD.
                GOBACK.
                """);

        Map<String, Ast.StatementOperandContext> contextByName = nodes(program, Ast.StatementOperand.class).stream()
                .filter(operand -> operand.value() instanceof Ast.DataReference)
                .collect(java.util.stream.Collectors.toMap(
                        operand -> ((Ast.DataReference) operand.value()).baseName(),
                        Ast.StatementOperand::context));
        assertEquals(Ast.StatementOperandContext.SET_CONDITION_TARGET, contextByName.get("STATE-OPEN"));
        assertEquals(Ast.StatementOperandContext.SET_DATA_OR_INDEX, contextByName.get("SOURCE-VALUE"));
        assertEquals(Ast.StatementOperandContext.SET_DATA_OR_INDEX, contextByName.get("STATE-FIELD"));
    }

    private static Ast.Program parse(String source) {
        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        return new AstBuilder(parser, source, SourceMap.identity(source, "typed-traversal.cbl"), ids, sizes)
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
