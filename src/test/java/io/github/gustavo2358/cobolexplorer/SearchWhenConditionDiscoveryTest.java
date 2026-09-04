package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checkpoint 1 characterization for SEARCH WHEN.  This class intentionally
 * records the current preserved boundary; it does not authorize or exercise a
 * production implementation.
 */
class SearchWhenConditionDiscoveryTest {
    @Test
    void S1_standaloneConditionNameIsInParseTreeButLostBeforeOccurrences() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S1", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        CobolParser.SearchWhenContext when = onlyWhen(analysis);
        assertAll("S1 standalone condition-name",
                () -> assertEquals("FLAG-ON", when.condition().getText()),
                () -> assertEquals(List.of("TABLE-ITEM"), operandNames(search)),
                () -> assertEquals(List.of("searchWhen"), search.clauses().stream()
                        .map(Ast.StatementClause::grammarRule).toList()),
                () -> assertTrue(search.clauses().get(0).recognizedNodes().isEmpty()),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertFalse(AstBoundaryTestSupport.nodes(analysis).stream()
                        .anyMatch(node -> node instanceof Ast.ContextualConditionTail)),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        entry(analysis, "TABLE-ITEM").selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                        entry(analysis, "TABLE-ITEM").status()),
                () -> assertEquals(1, AstBoundaryTestSupport.contexts(
                        analysis.tree(), CobolParser.ConditionNameReferenceContext.class).size()));
    }

    @Test
    void S2_completeRelationPreservesBothOperandsThroughThePreservedPath() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S2", """
                SEARCH TABLE-ITEM
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        assertAll("S2 complete relation",
                () -> assertEquals(List.of("TABLE-ITEM", "SEARCH-A", "SEARCH-B"), operandNames(search)),
                () -> assertEquals(List.of("SEARCH-IDX", "TABLE-ITEM", "SEARCH-A", "SEARCH-B"),
                        writtenNames(analysis).stream().filter(name -> name.startsWith("SEARCH-")
                                || name.equals("TABLE-ITEM")).toList()),
                () -> assertEquals(List.of("SEARCH-A", "SEARCH-B"),
                        writtenNames(analysis).stream().filter(name -> name.equals("SEARCH-A")
                                || name.equals("SEARCH-B")).toList()),
                () -> assertEquals(List.of(ResolutionContracts.ResolutionStatus.RESOLVED,
                                ResolutionContracts.ResolutionStatus.RESOLVED),
                        List.of(entry(analysis, "SEARCH-A").status(), entry(analysis, "SEARCH-B").status())),
                () -> assertTrue(List.of(entry(analysis, "SEARCH-A"), entry(analysis, "SEARCH-B")).stream()
                        .allMatch(entry -> entry.selectedCandidate().orElseThrow().kind()
                                == ResolutionContracts.ReferenceKind.DATA)),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.RelationCondition.class).isEmpty()));
    }

    @Test
    void S3_abbreviatedTailDisappearsWhileCompleteOperandsRemain() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3", """
                SEARCH TABLE-ITEM
                   WHEN SEARCH-A = SEARCH-B OR SEARCH-C
                      CONTINUE
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        assertAll("S3 abbreviated relation",
                () -> assertEquals(List.of("TABLE-ITEM", "SEARCH-A", "SEARCH-B"), operandNames(search)),
                () -> assertFalse(writtenNames(analysis).contains("SEARCH-C")),
                () -> assertEquals(0, AstBoundaryTestSupport.nodes(analysis, Ast.ContextualConditionTail.class).size()),
                () -> assertEquals(List.of(ResolutionContracts.ResolutionStatus.RESOLVED,
                                ResolutionContracts.ResolutionStatus.RESOLVED),
                        List.of(entry(analysis, "SEARCH-A").status(), entry(analysis, "SEARCH-B").status())),
                () -> assertEquals(0, AstBoundaryTestSupport.contexts(
                        analysis.tree(), CobolParser.AbbreviationContext.class).size()),
                () -> assertTrue(AstBoundaryTestSupport.contexts(
                        analysis.tree(), CobolParser.ConditionNameReferenceContext.class).stream()
                        .anyMatch(context -> context.getText().equals("SEARCH-C"))));
    }

    @Test
    void S4_multipleWhenKeepParseOrderAndBranchStatementsButLoseBothConditions() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S4", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-A
                      DISPLAY 'A'
                   WHEN FLAG-B
                      DISPLAY 'B'
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        List<CobolParser.SearchWhenContext> whens = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.SearchWhenContext.class);
        assertAll("S4 multiple WHEN",
                () -> assertEquals(List.of("FLAG-A", "FLAG-B"), whens.stream()
                        .map(context -> context.condition().getText()).toList()),
                () -> assertEquals(List.of("searchWhen", "searchWhen"), search.clauses().stream()
                        .map(Ast.StatementClause::grammarRule).toList()),
                () -> assertEquals(List.of(1, 1), search.clauses().stream()
                        .map(clause -> clause.nestedStatements().size()).toList()),
                () -> assertEquals(List.of("DISPLAY 'A'", "DISPLAY 'B'"), search.clauses().stream()
                        .flatMap(clause -> clause.nestedStatements().stream())
                        .map(Ast.Statement::toString)
                        .map(text -> text.substring(text.indexOf("writtenText=") + 12,
                                text.indexOf(", operands=")))
                        .toList()),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-A")),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-B")),
                () -> { AstBoundaryTestSupport.assertActualProductsJoin(analysis); });
    }

    @Test
    void S5_notConditionNameHasLogicalParseShapeButNoConditionOccurrence() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S5", """
                SEARCH TABLE-ITEM
                   WHEN NOT FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);

        CobolParser.SearchWhenContext when = onlyWhen(analysis);
        assertAll("S5 NOT",
                () -> assertEquals(1, when.condition().combinableCondition().NOT().getSymbol().getTokenIndex()
                        >= 0 ? 1 : 0),
                () -> assertEquals("NOTFLAG-ON", when.condition().getText()),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.NegatedCondition.class).isEmpty()));
    }

    @Test
    void S6_qualifiedConditionNameIsParsedButItsQualificationAndRootAreLost() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S6", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON OF GROUP-X
                      CONTINUE
                END-SEARCH.
                """);

        CobolParser.ConditionNameReferenceContext condition = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.ConditionNameReferenceContext.class).stream()
                .filter(context -> context.getText().contains("FLAG-ON"))
                .findFirst().orElseThrow();
        assertAll("S6 qualified condition-name",
                () -> assertEquals("FLAG-ONOFGROUP-X", condition.getText()),
                () -> assertEquals(1, condition.inData().size()),
                () -> assertEquals("GROUP-X", condition.inData(0).getText().replaceFirst("(?i)^OF", "").trim()),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertTrue(writtenNames(analysis).contains("GROUP-X")),
                () -> assertEquals(List.of("TABLE-ITEM", "GROUP-X"), operandNames(search(analysis))),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.DataReference.class).stream()
                        .noneMatch(reference -> reference.baseName().equals("FLAG-ON"))));
    }

    @Test
    void searchAllSharesTheGrammarBoundaryButHasDistinctNormativeRestrictions() {
        AstBoundaryTestSupport.Analysis analysis = analyze("ALL", """
                SEARCH ALL TABLE-ITEM
                   WHEN SEARCH-A = SEARCH-B AND SEARCH-C = SEARCH-D
                      CONTINUE
                END-SEARCH.
                """);

        CobolParser.SearchStatementContext statement = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.SearchStatementContext.class).get(0);
        assertAll("SEARCH ALL",
                () -> assertEquals("ALL", statement.ALL().getText()),
                () -> assertEquals(1, statement.searchWhen().size()),
                () -> assertEquals("SEARCH-A=SEARCH-BANDSEARCH-C=SEARCH-D",
                        statement.searchWhen(0).condition().getText()),
                () -> assertEquals(List.of("SEARCH-IDX", "SEARCH-A", "SEARCH-B", "SEARCH-C", "SEARCH-D"),
                        writtenNames(analysis).stream().filter(name -> name.startsWith("SEARCH-")).toList()),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.PreservedStatement.class).stream()
                        .anyMatch(node -> node.grammarRule().equals("searchStatement"))));
    }

    @Test
    void controlNegativeKeepsTableVaryingAndRelationOperandsOutOfConditionPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyze("CONTROL", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX
                   WHEN TABLE-VALUE (SEARCH-IDX) = SEARCH-KEY
                      CONTINUE
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        Map<String, List<ReferenceOccurrences.Occurrence>> occurrences = analysis.occurrences().values().stream()
                .flatMap(product -> product.occurrences().stream())
                .collect(Collectors.groupingBy(ReferenceOccurrences.Occurrence::writtenText));
        assertAll("control negative",
                () -> assertEquals(List.of("TABLE-ITEM", "SEARCH-IDX", "TABLE-VALUE (SEARCH-IDX)", "SEARCH-KEY"),
                        operandNames(search)),
                () -> assertTrue(occurrences.containsKey("TABLE-ITEM")),
                () -> assertTrue(occurrences.containsKey("SEARCH-IDX")),
                () -> assertTrue(occurrences.containsKey("TABLE-VALUE (SEARCH-IDX)")),
                () -> assertTrue(occurrences.containsKey("SEARCH-KEY")),
                () -> assertTrue(occurrences.get("SEARCH-IDX").stream()
                        .allMatch(occurrence -> occurrence.kind() != ResolutionContracts.ReferenceKind.CONDITION)),
                () -> assertFalse(occurrences.get("TABLE-VALUE (SEARCH-IDX)").stream()
                        .anyMatch(occurrence -> occurrence.kind() == ResolutionContracts.ReferenceKind.CONDITION)));
    }

    private static AstBoundaryTestSupport.Analysis analyze(String id, String search) {
        return AstBoundaryTestSupport.analyze(program(id, search), "search-when-" + id + ".cbl");
    }

    private static String program(String id, String search) {
        String program = "SEARCH-WHEN-" + id;
        return """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01  SEARCH-A PIC 9(4).
                01  SEARCH-B PIC 9(4).
                01  SEARCH-C PIC 9(4).
                01  SEARCH-D PIC 9(4).
                01  SEARCH-KEY PIC 9(4).
                01  GROUP-X.
                    05  FLAG PIC X.
                        88  FLAG-ON VALUE 'Y'.
                        88  FLAG-A VALUE 'A'.
                        88  FLAG-B VALUE 'B'.
                01  TABLE-ITEM OCCURS 2 TIMES INDEXED BY SEARCH-IDX.
                    05  TABLE-VALUE PIC 9(4).
                PROCEDURE DIVISION.
                %s
                END PROGRAM %s.
                """.formatted(program, search, program);
    }

    private static Ast.PreservedStatement search(AstBoundaryTestSupport.Analysis analysis) {
        return AstBoundaryTestSupport.nodes(analysis, Ast.PreservedStatement.class).stream()
                .filter(statement -> statement.grammarRule().equals("searchStatement"))
                .findFirst().orElseThrow();
    }

    private static CobolParser.SearchWhenContext onlyWhen(AstBoundaryTestSupport.Analysis analysis) {
        return AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.SearchWhenContext.class)
                .stream().findFirst().orElseThrow();
    }

    private static List<String> operandNames(Ast.PreservedStatement statement) {
        return statement.operands().stream().map(operand -> {
            if (operand.value() instanceof Ast.DataReference reference) return reference.writtenText();
            if (operand.value() instanceof Ast.NamedReference reference) return reference.writtenText();
            return operand.value().toString();
        }).toList();
    }

    private static Set<String> writtenNames(AstBoundaryTestSupport.Analysis analysis) {
        return analysis.occurrences().values().stream()
                .flatMap(product -> product.occurrences().stream())
                .map(ReferenceOccurrences.Occurrence::writtenText)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static ReferenceResolution.Entry entry(AstBoundaryTestSupport.Analysis analysis, String writtenText) {
        return analysis.resolution().entries().stream()
                .filter(candidate -> candidate.occurrence().writtenText().equals(writtenText))
                .findFirst().orElseThrow(() -> new AssertionError("missing resolution entry: " + writtenText));
    }
}
