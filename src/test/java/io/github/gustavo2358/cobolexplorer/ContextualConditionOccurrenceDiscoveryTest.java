package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FACT evidence for WORK-COND-005; production expectations remain reserved for implementation. */
class ContextualConditionOccurrenceDiscoveryTest {

    @Test
    void typedSurfaceDistinguishesStandaloneContextualRelationAndDistributedPositions() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source("SURFACE", """
                    IF C CONTINUE END-IF.
                    IF NOT C CONTINUE END-IF.
                    IF A = C CONTINUE END-IF.
                    IF A = B OR C CONTINUE END-IF.
                    IF (A = B OR C) CONTINUE END-IF.
                    IF (A = B) OR C CONTINUE END-IF.
                    IF A = (B OR C) CONTINUE END-IF.
                    IF A = B OR (C) CONTINUE END-IF.
                """), "contextual-occurrence-surface.cbl");
        List<Ast.IfStatement> statements = AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class);
        assertEquals(8, statements.size());

        assertInstanceOf(Ast.DataReference.class, statements.get(0).condition(),
                "a standalone condition-name surface is a direct DataReference");
        Ast.NegatedCondition standaloneNot = assertInstanceOf(
                Ast.NegatedCondition.class, statements.get(1).condition());
        assertInstanceOf(Ast.DataReference.class, standaloneNot.operand());

        Ast.RelationCondition relation = assertInstanceOf(
                Ast.RelationCondition.class, statements.get(2).condition());
        assertInstanceOf(Ast.DataReference.class, relation.object());

        Ast.LogicalCondition contextual = assertInstanceOf(
                Ast.LogicalCondition.class, statements.get(3).condition());
        assertInstanceOf(Ast.ContextualConditionTail.class, contextual.operands().get(1));

        Ast.GroupedCondition groupedSequence = assertInstanceOf(
                Ast.GroupedCondition.class, statements.get(4).condition());
        Ast.LogicalCondition groupedInner = assertInstanceOf(
                Ast.LogicalCondition.class, groupedSequence.inner());
        assertInstanceOf(Ast.ContextualConditionTail.class, groupedInner.operands().get(1));

        Ast.LogicalCondition closedBoundary = assertInstanceOf(
                Ast.LogicalCondition.class, statements.get(5).condition());
        assertInstanceOf(Ast.GroupedCondition.class, closedBoundary.operands().get(0));
        assertInstanceOf(Ast.DataReference.class, closedBoundary.operands().get(1),
                "the matching right parenthesis closes inheritance before C");

        Ast.RelationCondition distributed = assertInstanceOf(
                Ast.RelationCondition.class, statements.get(6).condition());
        assertInstanceOf(Ast.DistributedOperandGroup.class, distributed.object());

        Ast.LogicalCondition groupedTail = assertInstanceOf(
                Ast.LogicalCondition.class, statements.get(7).condition());
        Ast.GroupedCondition tailGroup = assertInstanceOf(
                Ast.GroupedCondition.class, groupedTail.operands().get(1));
        assertInstanceOf(Ast.ContextualConditionTail.class, tailGroup.inner(),
                "a group opened after the current subject does not close inheritance");
    }

    @Test
    void declarationSubstitutionLeavesOneIdenticalContextualSurfaceButChangesCurrentBindingOutcome() {
        record Variant(String declarations, ResolutionContracts.ResolutionStatus status,
                       ResolutionContracts.ResolutionReason reason,
                       ResolutionContracts.ReferenceKind selectedKind) { }
        Map<String, Variant> variants = new LinkedHashMap<>();
        variants.put("DATA", new Variant("01 C PIC 9(4).",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, null));
        variants.put("INDEX", new Variant("01 T OCCURS 2 TIMES INDEXED BY C.\n   05 V PIC X.",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, null));
        variants.put("CONDITION", new Variant("01 FLAG PIC X.\n   88 C VALUE 'Y'.",
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                ResolutionContracts.ReferenceKind.CONDITION));
        variants.put("RENAMES", new Variant("01 G.\n   05 X PIC X.\n   05 Y PIC X.\n   66 C RENAMES X THRU Y.",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, null));
        variants.put("MISSING", new Variant("01 PRESENT PIC X.",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, null));

        for (Map.Entry<String, Variant> item : variants.entrySet()) {
            Variant variant = item.getValue();
            AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                    source(item.getKey(), variant.declarations(),
                            "IF A = B OR C CONTINUE END-IF."),
                    "contextual-substitution-" + item.getKey().toLowerCase() + ".cbl");
            Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                    analysis, Ast.ContextualConditionTail.class).get(0);
            List<ReferenceResolution.Entry> writtenC = analysis.resolution().entries().stream()
                    .filter(entry -> entry.occurrence().referenceAstNodeId()
                            == tail.nominalReference().meta().id()).toList();
            assertEquals(1, writtenC.size(), "one written C must produce one root occurrence");
            ReferenceResolution.Entry entry = writtenC.get(0);
            assertAll(item.getKey(),
                    () -> assertEquals("C", tail.nominalReference().baseName()),
                    () -> assertEquals(Ast.ContextualConditionTail.class, tail.getClass()),
                    () -> assertEquals("conditionNameReference", entry.occurrence().grammarRule()),
                    () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                            entry.occurrence().kind()),
                    () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                            entry.occurrence().admissibleKinds()),
                    () -> assertEquals(variant.status(), entry.status()),
                    () -> assertEquals(variant.reason(), entry.reason()),
                    () -> assertEquals(variant.selectedKind(), entry.selectedCandidate()
                            .map(ReferenceResolution.Candidate::kind).orElse(null)));
        }
    }

    @Test
    void existingResolverBindsEverySupportedDeclarationKindWhenOneOccurrenceCarriesContextualAdmissibility() {
        record Variant(String declarations, ResolutionContracts.ResolutionStatus status,
                       ResolutionContracts.ResolutionReason reason,
                       ResolutionContracts.ReferenceKind selectedKind) { }
        Map<String, Variant> variants = new LinkedHashMap<>();
        variants.put("DATA", new Variant("01 C PIC 9(4).",
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                ResolutionContracts.ReferenceKind.DATA));
        variants.put("INDEX", new Variant("01 T OCCURS 2 TIMES INDEXED BY C.\n   05 V PIC X.",
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                ResolutionContracts.ReferenceKind.INDEX));
        variants.put("CONDITION", new Variant("01 FLAG PIC X.\n   88 C VALUE 'Y'.",
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                ResolutionContracts.ReferenceKind.CONDITION));
        variants.put("RENAMES", new Variant("01 G.\n   05 X PIC X.\n   05 Y PIC X.\n   66 C RENAMES X THRU Y.",
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                ResolutionContracts.ReferenceKind.DATA));
        variants.put("MISSING", new Variant("01 PRESENT PIC X.",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, null));

        Set<ResolutionContracts.ReferenceKind> contextualKinds = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.INDEX,
                ResolutionContracts.ReferenceKind.CONDITION);
        for (Map.Entry<String, Variant> item : variants.entrySet()) {
            Variant variant = item.getValue();
            AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                    source("RESOLVER-" + item.getKey(), variant.declarations(),
                            "IF A = B OR C CONTINUE END-IF."),
                    "contextual-resolver-" + item.getKey().toLowerCase() + ".cbl");
            Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                    analysis, Ast.ContextualConditionTail.class).get(0);
            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> projected =
                    withContextualOccurrence(analysis, tail.nominalReference().meta().id(), contextualKinds);
            ReferenceResolution resolution = new CobolReferenceResolver(
                    ResolutionContracts.CobolResolutionPolicy.initial())
                    .resolve(analysis.model(), analysis.tables(), projected);
            ReferenceResolution.Entry entry = resolution.entries().stream()
                    .filter(candidate -> candidate.occurrence().referenceAstNodeId()
                            == tail.nominalReference().meta().id()).findFirst().orElseThrow();
            assertAll(item.getKey(),
                    () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                            entry.occurrence().kind(), "primary kind remains a surface hint"),
                    () -> assertEquals(contextualKinds, entry.occurrence().admissibleKinds()),
                    () -> assertEquals(variant.status(), entry.status()),
                    () -> assertEquals(variant.reason(), entry.reason()),
                    () -> assertEquals(variant.selectedKind(), entry.selectedCandidate()
                            .map(ReferenceResolution.Candidate::kind).orElse(null)));
        }
    }

    @Test
    void contextualRootDoesNotChangeQualifierOrSubscriptPoliciesToday() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source(
                "CHILD-POLICIES", """
                01 GROUP-A OCCURS 2 TIMES.
                   05 FLAG PIC X.
                01 I PIC 9.
                """, "IF A = B OR FLAG OF GROUP-A(I) CONTINUE END-IF."),
                "contextual-child-policies.cbl");
        Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class).get(0);
        int rootId = tail.nominalReference().meta().id();
        int qualifierId = tail.nominalReference().qualifiers().get(0).reference().meta().id();
        int subscriptId = tail.nominalReference().subscriptGroups().get(0).subscripts().get(0).meta().id();

        ReferenceOccurrences occurrences = analysis.occurrences().values().iterator().next();
        ReferenceOccurrences.Occurrence root = occurrence(occurrences, rootId);
        ReferenceOccurrences.Occurrence qualifier = occurrence(occurrences, qualifierId);
        ReferenceOccurrences.Occurrence subscript = occurrence(occurrences, subscriptId);
        assertAll("independent child policies",
                () -> assertEquals(ResolutionContracts.ReferenceRole.VALUE_READ, root.role()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                        root.admissibleKinds(), "current root false gap remains characterized"),
                () -> assertEquals(ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT,
                        qualifier.role()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        qualifier.admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ReferenceRole.SUBSCRIPT, subscript.role()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX, subscript.kind()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        subscript.admissibleKinds()));
    }

    @Test
    void performControlListErasesTheTypedDifferenceBetweenValueAndUntilCondition() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source("PERFORM-SLOTS", """
                    PERFORM N TIMES
                       CONTINUE
                    END-PERFORM.
                    PERFORM UNTIL C
                       CONTINUE
                    END-PERFORM.
                """), "perform-control-context.cbl");
        List<Ast.PerformStatement> performs = AstBoundaryTestSupport.nodes(
                analysis, Ast.PerformStatement.class);
        assertEquals(2, performs.size());
        Ast.Expression timesValue = performs.get(0).controlExpressions().get(0);
        Ast.Expression untilCondition = performs.get(1).controlExpressions().get(0);

        assertAll("the AST container exposes both as the same untagged list position",
                () -> assertInstanceOf(Ast.DataReference.class, timesValue),
                () -> assertInstanceOf(Ast.DataReference.class, untilCondition),
                () -> assertEquals(0, performs.get(0).controlExpressions().indexOf(timesValue)),
                () -> assertEquals(0, performs.get(1).controlExpressions().indexOf(untilCondition)),
                () -> assertEquals("qualifiedDataName", timesValue.meta().origin().grammarRule()),
                () -> assertEquals("conditionNameReference",
                        untilCondition.meta().origin().grammarRule()),
                () -> assertTrue(performs.stream().allMatch(perform ->
                        perform.controlExpressions().size() == 1)));
    }

    private static ReferenceOccurrences.Occurrence occurrence(ReferenceOccurrences occurrences,
                                                               int astNodeId) {
        return occurrences.occurrences().stream()
                .filter(occurrence -> occurrence.referenceAstNodeId() == astNodeId)
                .findFirst().orElseThrow();
    }

    private static Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> withContextualOccurrence(
            AstBoundaryTestSupport.Analysis analysis, int astNodeId,
            Set<ResolutionContracts.ReferenceKind> admissibleKinds) {
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> result = new LinkedHashMap<>();
        analysis.occurrences().forEach((unitId, product) -> result.put(unitId,
                new ReferenceOccurrences(product.occurrences().stream().map(occurrence -> {
                    if (occurrence.referenceAstNodeId() != astNodeId) return occurrence;
                    return new ReferenceOccurrences.Occurrence(occurrence.id(), occurrence.programUnitId(),
                            occurrence.referenceAstNodeId(), occurrence.scopeId(),
                            ResolutionContracts.ReferenceKind.CONDITION, admissibleKinds,
                            occurrence.role(), occurrence.grammarRule(), occurrence.writtenText(),
                            occurrence.meta(), occurrence.preservation());
                }).toList())));
        return Map.copyOf(result);
    }

    private static String source(String suffix, String statements) {
        return source(suffix, "01 C PIC 9(4).\n01 N PIC 9(4).", statements);
    }

    private static String source(String suffix, String declarations, String statements) {
        String program = "D-CONTEXT-" + suffix;
        return """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC 9(4).
                01 B PIC 9(4).
                %s
                PROCEDURE DIVISION.
                %s
                END PROGRAM %s.
                """.formatted(program, declarations, statements, program);
    }
}
