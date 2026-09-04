package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Post-implementation adversarial contracts for WORK-COND-006. */
class SearchWhenMaterializationAdversarialTest {
    @Test
    void A1_eachWrittenNominalHasOneOccurrenceAndOneResolutionEntry() {
        AstBoundaryTestSupport.Analysis analysis = analyze("A1", """
                01  SOME-GROUP.
                    05  QUALIFIED-ITEM PIC 9(4).
                """, """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX OF SOME-GROUP
                   WHEN SEARCH-A = SEARCH-B OR SEARCH-C
                   WHEN FLAG-ON OF GROUP-X
                      CONTINUE
                END-SEARCH.
                """);

        AstBoundaryTestSupport.assertActualProductsJoin(analysis);
        List<ReferenceOccurrences.Occurrence> occurrences = analysis.occurrences().values().stream()
                .flatMap(product -> product.occurrences().stream()).toList();
        Map<Integer, Long> occurrencesByAstNode = occurrences.stream().collect(Collectors.groupingBy(
                ReferenceOccurrences.Occurrence::referenceAstNodeId, Collectors.counting()));
        Map<Integer, Long> resolutionsByAstNode = analysis.resolution().entries().stream().collect(Collectors.groupingBy(
                entry -> entry.occurrence().referenceAstNodeId(), Collectors.counting()));
        assertAll("A1 no duplicate preserved-path references",
                () -> assertTrue(occurrences.stream().anyMatch(o -> o.writtenText().contains("TABLE-ITEM"))),
                () -> assertTrue(occurrences.stream().anyMatch(o -> o.writtenText().contains("SEARCH-IDX"))),
                () -> assertTrue(occurrences.stream().anyMatch(o -> o.writtenText().equals("SEARCH-C"))),
                () -> assertTrue(occurrences.stream().anyMatch(o -> o.writtenText().startsWith("FLAG-ON"))),
                () -> assertTrue(occurrencesByAstNode.values().stream().allMatch(count -> count == 1)),
                () -> assertTrue(resolutionsByAstNode.values().stream().allMatch(count -> count == 1)));
    }

    @Test
    void A2_searchedReferenceVaryingAndConditionHaveIndependentPolicies() {
        AstBoundaryTestSupport.Analysis analysis = analyze("A2", "", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX
                   WHEN FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);
        Ast.SearchStatement search = search(analysis);
        Ast.DataReference flag = assertInstanceOf(Ast.DataReference.class, search.whens().get(0).condition());
        ReferenceResolution.Entry table = entryForAst(analysis, search.searchedReference());
        ReferenceResolution.Entry varying = entryForAst(analysis, search.varying());
        ReferenceResolution.Entry condition = entryForAst(analysis, flag);
        assertAll("A2 semantic positions",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, table.occurrence().kind()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, varying.occurrence().kind()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX), varying.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, condition.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                        condition.occurrence().admissibleKinds()));
    }

    @Test
    void A3_declarationSubstitutionChangesOnlyVaryingResolution() {
        AstBoundaryTestSupport.Analysis data = analyze("A3-DATA", "01  VARYING-NAME PIC 9(4).", varying("VARYING-NAME"));
        AstBoundaryTestSupport.Analysis index = analyze("A3-INDEX", """
                01  VARYING-TABLE OCCURS 10 TIMES INDEXED BY VARYING-NAME.
                    05  VARYING-VALUE PIC 9(4).
                """, varying("VARYING-NAME"));
        AstBoundaryTestSupport.Analysis missing = analyze("A3-MISSING", "", varying("VARYING-NAME"));
        assertAll("A3 declaration substitution",
                () -> assertEquals(varyingPolicy(data), varyingPolicy(index)),
                () -> assertEquals(varyingPolicy(data), varyingPolicy(missing)),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        entryForAst(data, search(data).varying()).selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        entryForAst(index, search(index).varying()).selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        entryForAst(missing, search(missing).varying()).status()));
    }

    @Test
    void A4_shapeSubstitutionChangesVaryingAdmissibility() {
        AstBoundaryTestSupport.Analysis bare = analyze("A4-BARE", "01  VARYING-NAME PIC 9(4).", varying("VARYING-NAME"));
        AstBoundaryTestSupport.Analysis qualified = analyze("A4-QUALIFIED", """
                01  SOME-GROUP.
                    05  VARYING-NAME PIC 9(4).
                """, """
                SEARCH TABLE-ITEM VARYING VARYING-NAME OF SOME-GROUP
                   WHEN FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);
        assertAll("A4 shape substitution",
                () -> assertTrue(search(bare).varying().qualifiers().isEmpty()),
                () -> assertEquals(1, search(qualified).varying().qualifiers().size()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX), varyingPolicy(bare)),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA), varyingPolicy(qualified)));
    }

    @Test
    void A5_conditionDeclarationSubstitutionKeepsSurfacePolicy() {
        AstBoundaryTestSupport.Analysis data = analyze("A5-DATA", "01  C PIC 9(4).", relationWithTail());
        AstBoundaryTestSupport.Analysis index = analyze("A5-INDEX", """
                01  C-TABLE OCCURS 4 TIMES INDEXED BY C.
                    05  C-VALUE PIC 9(4).
                """, relationWithTail());
        AstBoundaryTestSupport.Analysis condition = analyze("A5-CONDITION", """
                01  C-FLAG PIC X.
                    88  C VALUE 'Y'.
                """, relationWithTail());
        AstBoundaryTestSupport.Analysis missing = analyze("A5-MISSING", "", relationWithTail());
        assertAll("A5 condition declaration substitution",
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX,
                                ResolutionContracts.ReferenceKind.CONDITION), tailPolicy(data)),
                () -> assertEquals(tailPolicy(data), tailPolicy(index)),
                () -> assertEquals(tailPolicy(data), tailPolicy(condition)),
                () -> assertEquals(tailPolicy(data), tailPolicy(missing)),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        tailEntry(data).selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        tailEntry(index).selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        tailEntry(condition).selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, tailEntry(missing).status()));
    }

    @Test
    void A6_nextSentenceDoesNotMixOwnershipWithTheSecondWhen() {
        AstBoundaryTestSupport.Analysis analysis = analyze("A6", "", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-A
                      NEXT SENTENCE
                   WHEN FLAG-B
                      DISPLAY 'B'
                END-SEARCH.
                """);
        Ast.SearchStatement search = search(analysis);
        assertAll("A6 branch ownership",
                () -> assertEquals(2, search.whens().size()),
                () -> assertEquals("FLAG-A", ((Ast.DataReference) search.whens().get(0).condition()).baseName()),
                () -> assertEquals("FLAG-B", ((Ast.DataReference) search.whens().get(1).condition()).baseName()),
                () -> assertEquals(1, search.whens().get(0).statements().size()),
                () -> assertInstanceOf(Ast.NextSentenceStatement.class, search.whens().get(0).statements().get(0)),
                () -> assertEquals(1, search.whens().get(1).statements().size()),
                () -> assertEquals("DISPLAY 'B'", ((Ast.PreservedStatement) search.whens().get(1).statements().get(0)).writtenText()));
    }

    @Test
    void A7_searchAllOnlyChangesTheStructuralAllBit() {
        String condition = """
                WHEN SEARCH-A = SEARCH-B
                   CONTINUE
                """;
        Ast.SearchStatement serial = search(analyze("A7-SERIAL", "", "SEARCH TABLE-ITEM\n" + condition + "END-SEARCH."));
        Ast.SearchStatement all = search(analyze("A7-ALL", "", "SEARCH ALL TABLE-ITEM\n" + condition + "END-SEARCH."));
        assertAll("A7 SEARCH ALL boundary",
                () -> assertFalse(serial.all()),
                () -> assertTrue(all.all()),
                () -> assertEquals(serial.whens().get(0).condition().getClass(), all.whens().get(0).condition().getClass()),
                () -> assertEquals("=", ((Ast.RelationCondition) serial.whens().get(0).condition()).relationalOperator()),
                () -> assertEquals("=", ((Ast.RelationCondition) all.whens().get(0).condition()).relationalOperator()),
                () -> assertEquals("SEARCH-A", ((Ast.DataReference) ((Ast.RelationCondition)
                        serial.whens().get(0).condition()).subject()).baseName()),
                () -> assertEquals("SEARCH-A", ((Ast.DataReference) ((Ast.RelationCondition)
                        all.whens().get(0).condition()).subject()).baseName()));
    }

    @Test
    void A8_qualifiedConditionRootKeepsQualifierIndependent() {
        AstBoundaryTestSupport.Analysis analysis = analyze("A8", "", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON OF GROUP-X
                      CONTINUE
                END-SEARCH.
                """);
        Ast.DataReference root = assertInstanceOf(Ast.DataReference.class, search(analysis).whens().get(0).condition());
        ReferenceResolution.Entry rootEntry = entryForAst(analysis, root);
        ReferenceResolution.Entry qualifier = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().referenceAstNodeId() == root.qualifiers().get(0).reference().meta().id())
                .findFirst().orElseThrow();
        assertAll("A8 qualified condition root",
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, rootEntry.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION), rootEntry.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, qualifier.occurrence().kind()),
                () -> assertFalse(qualifier.occurrence().admissibleKinds().contains(ResolutionContracts.ReferenceKind.INDEX)),
                () -> assertEquals(2, List.of(rootEntry, qualifier).size()));
    }

    @Test
    void A9_notUsesTheSameLogicalAndContextualConditionShapes() {
        AstBoundaryTestSupport.Analysis analysis = analyze("A9", "", """
                SEARCH TABLE-ITEM
                   WHEN NOT SEARCH-A = SEARCH-B OR SEARCH-C
                      CONTINUE
                END-SEARCH.
                """);
        Ast.LogicalCondition logical = assertInstanceOf(Ast.LogicalCondition.class,
                search(analysis).whens().get(0).condition());
        Ast.NegatedCondition negated = assertInstanceOf(Ast.NegatedCondition.class, logical.operands().get(0));
        Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class, negated.operand());
        Ast.ContextualConditionTail tail = assertInstanceOf(Ast.ContextualConditionTail.class, logical.operands().get(1));
        assertAll("A9 NOT condition surface",
                () -> assertEquals(Ast.LogicalConnector.OR, logical.connector()),
                () -> assertEquals("SEARCH-A", ((Ast.DataReference) relation.subject()).baseName()),
                () -> assertEquals("SEARCH-B", ((Ast.DataReference) relation.object()).baseName()),
                () -> assertEquals("SEARCH-C", tail.nominalReference().baseName()),
                () -> assertTrue(analysis.resolution().entries().stream()
                        .anyMatch(entry -> entry.occurrence().writtenText().equals("SEARCH-C"))));
    }

    @Test
    void A10_standaloneConditionCannotFallBackToGenericDataTraversal() {
        AstBoundaryTestSupport.Analysis analysis = analyze("A10", "", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);
        Ast.SearchWhen when = search(analysis).whens().get(0);
        ReferenceResolution.Entry entry = entryForAst(analysis, (Ast.DataReference) when.condition());
        assertAll("A10 explicit typed boundary",
                () -> assertEquals(List.of(when.condition()), Ast.children(when).subList(0, 1)),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, entry.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                        entry.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()));
    }

    private static Ast.SearchStatement search(AstBoundaryTestSupport.Analysis analysis) {
        return AstBoundaryTestSupport.nodes(analysis, Ast.SearchStatement.class).stream()
                .findFirst().orElseThrow();
    }

    private static AstBoundaryTestSupport.Analysis analyze(String id, String declarations, String search) {
        String program = "SEARCH-ADV-" + id;
        String source = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01  SEARCH-A PIC 9(4).
                01  SEARCH-B PIC 9(4).
                01  SEARCH-C PIC 9(4).
                01  SEARCH-D PIC 9(4).
                %s
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
                """.formatted(program, declarations, search, program);
        return AstBoundaryTestSupport.analyze(source, "search-advanced-" + id + ".cbl");
    }

    private static String varying(String name) {
        return """
                SEARCH TABLE-ITEM VARYING %s
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """.formatted(name);
    }

    private static String relationWithTail() {
        return """
                SEARCH TABLE-ITEM
                   WHEN SEARCH-A = SEARCH-B OR C
                      CONTINUE
                END-SEARCH.
                """;
    }

    private static ReferenceResolution.Entry entryForAst(AstBoundaryTestSupport.Analysis analysis,
                                                         Ast.DataReference reference) {
        return analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().referenceAstNodeId() == reference.meta().id())
                .findFirst().orElseThrow(() -> new AssertionError("missing AST reference: " + reference.writtenText()));
    }

    private static Set<ResolutionContracts.ReferenceKind> varyingPolicy(AstBoundaryTestSupport.Analysis analysis) {
        return entryForAst(analysis, search(analysis).varying()).occurrence().admissibleKinds();
    }

    private static Set<ResolutionContracts.ReferenceKind> tailPolicy(AstBoundaryTestSupport.Analysis analysis) {
        Ast.LogicalCondition logical = assertInstanceOf(Ast.LogicalCondition.class,
                search(analysis).whens().get(0).condition());
        Ast.ContextualConditionTail tail = assertInstanceOf(Ast.ContextualConditionTail.class, logical.operands().get(1));
        return entryForAst(analysis, tail.nominalReference()).occurrence().admissibleKinds();
    }

    private static ReferenceResolution.Entry tailEntry(AstBoundaryTestSupport.Analysis analysis) {
        Ast.LogicalCondition logical = assertInstanceOf(Ast.LogicalCondition.class,
                search(analysis).whens().get(0).condition());
        Ast.ContextualConditionTail tail = assertInstanceOf(Ast.ContextualConditionTail.class, logical.operands().get(1));
        return entryForAst(analysis, tail.nominalReference());
    }
}
