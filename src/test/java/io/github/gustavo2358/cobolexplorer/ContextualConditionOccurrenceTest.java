package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Normative production oracles for WORK-COND-005, Checkpoint 2. */
class ContextualConditionOccurrenceTest {
    private static final Set<ResolutionContracts.ReferenceKind> CONTEXTUAL_BARE = EnumSet.of(
            ResolutionContracts.ReferenceKind.DATA,
            ResolutionContracts.ReferenceKind.INDEX,
            ResolutionContracts.ReferenceKind.CONDITION);

    @Test
    void bug01AndBug02CollectEveryContextualTailAndResolveData() {
        AstBoundaryTestSupport.Analysis analysis = analyze("BUGS", "01 C PIC 9(4).\n01 D PIC 9(4).",
                "IF A = B OR C END-IF.\nIF A = B OR C OR D END-IF.");

        List<Ast.ContextualConditionTail> tails = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class);
        assertEquals(List.of("C", "C", "D"), tails.stream()
                .map(tail -> tail.nominalReference().baseName()).toList());
        for (Ast.ContextualConditionTail tail : tails) {
            ReferenceResolution.Entry entry = entryFor(analysis, tail.nominalReference());
            assertEquals(ResolutionContracts.ReferenceKind.CONDITION, entry.occurrence().kind());
            assertEquals(CONTEXTUAL_BARE, entry.occurrence().admissibleKinds());
            assertEquals(ResolutionContracts.ReferenceKind.DATA,
                    entry.selectedCandidate().orElseThrow().kind());
            assertFalse(entry.reason() == ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT);
        }
    }

    @Test
    void longAbbreviatedRelationChainResolvesEveryContextualDataTail() {
        AstBoundaryTestSupport.Analysis analysis = analyze("WAUX-LIKE",
                "01 INPUT-CODE PIC 9(4).\n01 X1 PIC 9(4).\n01 X2 PIC 9(4).\n"
                        + "01 X3 PIC 9(4).\n01 X4 PIC 9(4).",
                "IF INPUT-CODE = X1 OR X2 OR X3 OR X4 END-IF.");

        Ast.RelationCondition relation = AstBoundaryTestSupport.nodes(analysis, Ast.RelationCondition.class)
                .get(0);
        List<Ast.ContextualConditionTail> tails = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class);
        assertEquals(List.of("X2", "X3", "X4"), tails.stream()
                .map(tail -> tail.nominalReference().baseName()).toList());
        for (Ast.ContextualConditionTail tail : tails) {
            ReferenceResolution.Entry entry = entryFor(analysis, tail.nominalReference());
            assertEquals(ResolutionContracts.ReferenceKind.CONDITION, entry.occurrence().kind());
            assertEquals(CONTEXTUAL_BARE, entry.occurrence().admissibleKinds());
            assertEquals(ResolutionContracts.ReferenceKind.DATA,
                    entry.selectedCandidate().orElseThrow().kind());
        }

        List<Ast.DataReference> writtenNominals = new java.util.ArrayList<>();
        writtenNominals.add((Ast.DataReference) relation.object());
        writtenNominals.addAll(tails.stream().map(Ast.ContextualConditionTail::nominalReference).toList());
        for (Ast.DataReference reference : writtenNominals) {
            assertEquals(1, analysis.occurrences().values().stream()
                    .flatMap(product -> product.occurrences().stream())
                    .filter(occurrence -> occurrence.referenceAstNodeId() == reference.meta().id()).count());
            assertEquals(1, analysis.resolution().entries().stream()
                    .filter(entry -> entry.occurrence().referenceAstNodeId() == reference.meta().id()).count());
        }
        assertFalse(analysis.resolution().entries().stream().anyMatch(entry ->
                entry.reason() == ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT));
    }

    @Test
    void connectorSpellingDoesNotChangeContextualPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyze("CONNECTORS", "01 C PIC 9(4).\n01 D PIC 9(4).",
                "IF A = B OR C END-IF.\nIF A = B AND D END-IF.");
        List<Ast.ContextualConditionTail> tails = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class);
        assertEquals(2, tails.size());
        for (Ast.ContextualConditionTail tail : tails)
            assertEquals(CONTEXTUAL_BARE,
                    entryFor(analysis, tail.nominalReference()).occurrence().admissibleKinds());
    }

    @Test
    void declarationSubstitutionChangesOnlyBindingForBareContextualSurface() {
        List<String> declarations = List.of(
                "01 C PIC 9(4).",
                "01 T OCCURS 2 TIMES INDEXED BY C.\n05 V PIC X.",
                "01 FLAG PIC X.\n88 C VALUE 'Y'.",
                "01 G.\n05 X PIC X.\n05 Y PIC X.\n66 C RENAMES X THRU Y.",
                "01 PRESENT PIC X.");
        List<ResolutionContracts.ReferenceKind> selected = java.util.Arrays.asList(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.INDEX,
                ResolutionContracts.ReferenceKind.CONDITION,
                ResolutionContracts.ReferenceKind.DATA,
                null);
        Ast.ContextualConditionTail first = null;
        for (int i = 0; i < declarations.size(); i++) {
            AstBoundaryTestSupport.Analysis analysis = analyze("SUBSTITUTION-" + i, declarations.get(i),
                    "IF A = B OR C END-IF.");
            Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                    analysis, Ast.ContextualConditionTail.class).get(0);
            if (first == null) first = tail;
            assertEquals(CONTEXTUAL_BARE,
                    entryFor(analysis, tail.nominalReference()).occurrence().admissibleKinds());
            assertEquals(selected.get(i), entryFor(analysis, tail.nominalReference())
                    .selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null));
        }
        assertNotNull(first);
    }

    @Test
    void standaloneConditionRemainsConditionOnly() {
        AstBoundaryTestSupport.Analysis analysis = analyze("STANDALONE", "01 C PIC 9(4).",
                "IF C END-IF.");
        Ast.DataReference reference = AstBoundaryTestSupport.nodes(analysis, Ast.DataReference.class).stream()
                .filter(item -> item.baseName().equals("C")).findFirst().orElseThrow();
        ReferenceResolution.Entry entry = entryFor(analysis, reference);
        assertEquals(ResolutionContracts.ReferenceKind.CONDITION, entry.occurrence().kind());
        assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                entry.occurrence().admissibleKinds());
        assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, entry.status());
    }

    @Test
    void relationPolicyUsesAllThreeTypedShapeDimensions() {
        AstBoundaryTestSupport.Analysis analysis = analyze("SHAPES", "01 C PIC X(10).\n01 G.\n05 C PIC X(10).\n01 I PIC 9.",
                "IF A = C END-IF.\nIF A = C OF G END-IF.\nIF A = C(I) END-IF.\nIF A = C(1:2) END-IF.");
        List<Ast.RelationCondition> relations = AstBoundaryTestSupport.nodes(
                analysis, Ast.RelationCondition.class);
        assertEquals(4, relations.size());
        assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.ReferenceKind.INDEX),
                entryFor(analysis, (Ast.DataReference) relations.get(0).object())
                        .occurrence().admissibleKinds());
        for (int index = 1; index < relations.size(); index++)
            assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                    entryFor(analysis, (Ast.DataReference) relations.get(index).object())
                            .occurrence().admissibleKinds());
    }

    @Test
    void contextualShapeExcludesIndexButChildrenKeepTheirOwnPolicies() {
        AstBoundaryTestSupport.Analysis analysis = analyze("CONTEXTUAL-SHAPES",
                "01 G OCCURS 3 TIMES.\n05 C PIC 9(4).\n01 I PIC 9.",
                "IF A = B OR C END-IF.\nIF A = B OR C OF G END-IF.\nIF A = B OR C(I) END-IF.\nIF A = B OR C OF G(I) END-IF.");
        List<Ast.ContextualConditionTail> tails = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class);
        assertEquals(4, tails.size());
        assertEquals(CONTEXTUAL_BARE,
                entryFor(analysis, tails.get(0).nominalReference()).occurrence().admissibleKinds());
        for (int index = 1; index < tails.size(); index++)
            assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                            ResolutionContracts.ReferenceKind.CONDITION),
                    entryFor(analysis, tails.get(index).nominalReference()).occurrence().admissibleKinds());
        ReferenceOccurrences occurrences = analysis.occurrences().values().iterator().next();
        assertTrue(occurrences.occurrences().stream().anyMatch(item ->
                item.role() == ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT
                        && item.admissibleKinds().equals(Set.of(ResolutionContracts.ReferenceKind.DATA))));
        assertTrue(occurrences.occurrences().stream().anyMatch(item ->
                item.role() == ResolutionContracts.ReferenceRole.SUBSCRIPT
                        && item.admissibleKinds().equals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.ReferenceKind.INDEX))));
    }

    @Test
    void boundariesAndNotDoNotLeakOrCloseContextuality() {
        AstBoundaryTestSupport.Analysis analysis = analyze("BOUNDARIES",
                "01 C PIC 9(4).", "IF (A = B OR C) END-IF.\nIF (A = B) OR C END-IF.\n"
                        + "IF NOT A = B OR C END-IF.\nIF A = B OR NOT C END-IF.");
        List<Ast.ContextualConditionTail> tails = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class);
        assertEquals(3, tails.size(), "the grouped relation, NOT relation and NOT tail retain context");
        for (Ast.ContextualConditionTail tail : tails)
            assertEquals(CONTEXTUAL_BARE,
                    entryFor(analysis, tail.nominalReference()).occurrence().admissibleKinds());
        List<Ast.DataReference> cReferences = AstBoundaryTestSupport.nodes(analysis, Ast.DataReference.class)
                .stream().filter(reference -> reference.baseName().equals("C")).toList();
        assertEquals(4, cReferences.size());
        assertEquals(1, cReferences.stream().filter(reference -> entryFor(analysis, reference)
                        .occurrence().admissibleKinds().equals(Set.of(ResolutionContracts.ReferenceKind.CONDITION)))
                .count(), "only C after a closed group is standalone");
    }

    @Test
    void performControlsExposeTypedContextWithoutChangingExpressionView() throws Exception {
        AstBoundaryTestSupport.Analysis analysis = analyze("PERFORM",
                "01 N PIC 9(4).\n01 FLAG PIC X.\n88 C VALUE 'Y'.", "PERFORM N TIMES\n"
                        + "   CONTINUE\nEND-PERFORM.\nPERFORM UNTIL C\n   CONTINUE\nEND-PERFORM.");
        List<Ast.PerformStatement> performs = AstBoundaryTestSupport.nodes(
                analysis, Ast.PerformStatement.class);
        assertEquals(2, performs.size());
        Method controlsMethod = Ast.PerformStatement.class.getMethod("controls");
        List<?> times = (List<?>) controlsMethod.invoke(performs.get(0));
        List<?> until = (List<?>) controlsMethod.invoke(performs.get(1));
        assertEquals(1, times.size());
        assertEquals(1, until.size());
        assertEquals("VALUE", controlsMethod.getReturnType().getTypeName().contains("List")
                ? times.get(0).getClass().getMethod("context").invoke(times.get(0)).toString() : "");
        assertEquals("CONDITION", until.get(0).getClass().getMethod("context").invoke(until.get(0)).toString());
        assertFalse(Ast.Node.class.isAssignableFrom(times.get(0).getClass()));
        assertFalse(Ast.Node.class.isAssignableFrom(until.get(0).getClass()));
        assertEquals(1, performs.get(0).controlExpressions().size());
        assertEquals(1, performs.get(1).controlExpressions().size());
    }

    @Test
    void manifestSeparatesContextualOriginFromOccurrencePolicy() {
        ReferenceResolutionManifest.Entry contextual = ReferenceResolutionManifest.entry(
                GrammarCoverageManifest.Grammar.COBOL, "conditionNameReference");
        ReferenceResolutionManifest.Entry qualified = ReferenceResolutionManifest.entry(
                GrammarCoverageManifest.Grammar.COBOL, "qualifiedDataName");
        assertEquals("CONTEXTUAL_REFERENCE_ORIGIN", contextual.ruleClass().name());
        assertEquals(null, contextual.referenceKind());
        assertEquals(ReferenceResolutionManifest.RuleClass.REFERENCE_ORIGIN, qualified.ruleClass());
        assertEquals(ResolutionContracts.ReferenceKind.DATA, qualified.referenceKind());
        assertTrue(ReferenceResolutionManifest.VERSION.compareTo("1.0.0") > 0);
    }

    @Test
    void diagnosticsDistinguishContextualUncertaintyFromStandaloneCondition() {
        AstBoundaryTestSupport.Analysis contextual = analyze("DIAGNOSTIC-CONTEXT",
                "01 PRESENT PIC X.", "IF A = B OR MISSING END-IF.");
        AstBoundaryTestSupport.Analysis standalone = analyze("DIAGNOSTIC-STANDALONE",
                "01 PRESENT PIC X.", "IF MISSING END-IF.");
        String contextualMessage = contextual.resolution().diagnostics().stream()
                .filter(diagnostic -> diagnostic.message().contains("MISSING"))
                .findFirst().orElseThrow().message();
        String standaloneMessage = standalone.resolution().diagnostics().stream()
                .filter(diagnostic -> diagnostic.message().contains("MISSING"))
                .findFirst().orElseThrow().message();
        assertTrue(contextualMessage.contains("CONTEXTUAL_CONDITION"));
        assertFalse(contextualMessage.contains("UNRESOLVED CONDITION reference"));
        assertTrue(standaloneMessage.contains("CONDITION reference"));
    }

    @Test
    void distributedOperandsReuseRelationShapePolicyWithoutContextLeakage() {
        AstBoundaryTestSupport.Analysis analysis = analyze("DISTRIBUTED",
                "01 C PIC X(10).\n01 I PIC 9.",
                "IF A = (B OR C(I)) END-IF.\nIF A = (B AND C) END-IF.");
        List<Ast.DataReference> cReferences = AstBoundaryTestSupport.nodes(analysis, Ast.DataReference.class)
                .stream().filter(reference -> reference.baseName().equals("C")).toList();
        assertEquals(2, cReferences.size());
        assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                entryFor(analysis, cReferences.get(0)).occurrence().admissibleKinds());
        assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.ReferenceKind.INDEX),
                entryFor(analysis, cReferences.get(1)).occurrence().admissibleKinds());
        assertEquals(2, AstBoundaryTestSupport.nodes(analysis, Ast.DistributedOperandGroup.class).size());
    }

    @Test
    void oneWrittenContextualNominalHasOneOccurrenceAndOneResolutionEntry() {
        AstBoundaryTestSupport.Analysis analysis = analyze("CARDINALITY", "01 C PIC 9(4).",
                "IF A = B OR C END-IF.");
        Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class).get(0);
        int id = tail.nominalReference().meta().id();
        assertEquals(1, analysis.occurrences().values().stream().flatMap(product -> product.occurrences().stream())
                .filter(occurrence -> occurrence.referenceAstNodeId() == id).count());
        assertEquals(1, analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().referenceAstNodeId() == id).count());
        assertEquals(3, entryFor(analysis, tail.nominalReference()).occurrence().admissibleKinds().size());
        assertTrue(AstBoundaryTestSupport.nodes(analysis).stream()
                .filter(node -> node.meta().id() == id).allMatch(node -> node == tail.nominalReference()));
    }

    @Test
    void notFormsRemainStructurallyAndContextuallyDistinct() {
        AstBoundaryTestSupport.Analysis analysis = analyze("NOT-FORMS", "01 C PIC 9(4).",
                "IF NOT C END-IF.\nIF A NOT = C END-IF.\nIF NOT A = B OR C END-IF.\n"
                        + "IF A = B OR NOT C END-IF.");
        List<Ast.IfStatement> ifs = AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class);
        assertEquals(4, ifs.size());
        assertTrue(ifs.get(0).condition() instanceof Ast.NegatedCondition);
        assertTrue(ifs.get(1).condition() instanceof Ast.RelationCondition);
        assertTrue(ifs.get(2).condition() instanceof Ast.LogicalCondition);
        assertTrue(ifs.get(3).condition() instanceof Ast.LogicalCondition);
        assertEquals(2, AstBoundaryTestSupport.nodes(analysis, Ast.ContextualConditionTail.class).size());
        for (Ast.ContextualConditionTail tail : AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class))
            assertEquals(CONTEXTUAL_BARE,
                    entryFor(analysis, tail.nominalReference()).occurrence().admissibleKinds());
    }

    private static ReferenceResolution.Entry entryFor(AstBoundaryTestSupport.Analysis analysis,
                                                       Ast.DataReference reference) {
        return analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().referenceAstNodeId() == reference.meta().id())
                .findFirst().orElseThrow();
    }

    private static AstBoundaryTestSupport.Analysis analyze(String suffix, String declarations,
                                                            String statements) {
        String program = "T-COND-" + suffix;
        return AstBoundaryTestSupport.analyze("""
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
                """.formatted(program, declarations, statements, program), suffix + ".cbl");
    }
}
