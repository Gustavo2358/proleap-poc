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
                    () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                    ResolutionContracts.ReferenceKind.INDEX,
                                    ResolutionContracts.ReferenceKind.CONDITION),
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
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.CONDITION),
                        root.admissibleKinds(), "qualified+subscripted contextual root excludes INDEX"),
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

    // -------------------------------------------------------------------------
    // FACT-R2-01 — bare INDEX é admissível no tail contextual (fixture IBM-válida:
    // C é index-name e único candidate do spelling; object abreviado nu admite INDEX).
    // -------------------------------------------------------------------------
    @Test
    void bareContextualTailAdmitsIndexAndResolverSelectsTheIndexName() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source("R2-BARE-INDEX", """
                    01 T OCCURS 2 TIMES INDEXED BY C.
                       05 V PIC X.
                    """, "IF A = B OR C CONTINUE END-IF."),
                "r2-bare-contextual-index.cbl");
        Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class).get(0);
        Ast.DataReference root = tail.nominalReference();
        int rootId = root.meta().id();
        assertAll("bare shape stays typed",
                () -> assertTrue(root.qualifiers().isEmpty()),
                () -> assertTrue(root.subscriptGroups().isEmpty()),
                () -> assertEquals(null, root.referenceModification()));

        Set<ResolutionContracts.ReferenceKind> contextualBare = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.INDEX,
                ResolutionContracts.ReferenceKind.CONDITION);
        ReferenceResolution resolution = reResolve(analysis,
                withOccurrencePolicy(analysis, rootId,
                        ResolutionContracts.ReferenceKind.CONDITION, contextualBare));
        ReferenceResolution.Entry entry = resolutionEntry(resolution, rootId);
        assertAll("FACT-R2-01",
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, entry.occurrence().kind(),
                        "primary kind remains a surface hint"),
                () -> assertEquals(contextualBare, entry.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                        entry.reason()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        entry.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)));
    }

    // -------------------------------------------------------------------------
    // FACT-R2-02 — qualified root não admite INDEX. A shape C OF T é grammar-válida;
    // regras de declaração não podem tornar um index-name o candidate único de um root
    // qualified em source IBM-válido, então o collision é exercitado como what-if
    // controlado sobre o occurrence/candidate model: o index C de T é visível ao
    // machinery nominal e seria selecionado somente se INDEX fosse admissível.
    // -------------------------------------------------------------------------
    @Test
    void qualifiedContextualTailDoesNotAdmitIndexAsRoot() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source("R2-QUAL-INDEX", """
                    01 T OCCURS 2 TIMES INDEXED BY C.
                       05 V PIC X.
                    """, "IF A = B OR C OF T CONTINUE END-IF."),
                "r2-qualified-contextual-index.cbl");
        Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class).get(0);
        Ast.DataReference root = tail.nominalReference();
        int rootId = root.meta().id();
        assertAll("qualified shape stays typed",
                () -> assertEquals("C", root.baseName()),
                () -> assertEquals(1, root.qualifiers().size()),
                () -> assertEquals("T", root.qualifiers().get(0).name()),
                () -> assertTrue(root.subscriptGroups().isEmpty()),
                () -> assertEquals(null, root.referenceModification()));

        // Control A — "if INDEX were permitted" (old fixed contextual set): the only
        // same-name candidate is the index C declared under T; it matches the written
        // qualifier T and would resolve as INDEX. This proves the collision is real in
        // the candidate model and that only the admissible policy keeps INDEX out.
        Set<ResolutionContracts.ReferenceKind> fixedSet = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.INDEX,
                ResolutionContracts.ReferenceKind.CONDITION);
        ReferenceResolution.Entry control = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, rootId,
                        ResolutionContracts.ReferenceKind.CONDITION, fixedSet)), rootId);
        assertAll("FACT-R2-02 control",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, control.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        control.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)));

        // Oracle — future qualified contextual policy {DATA, CONDITION}: INDEX is not an
        // admissible namespace for the qualified root; the resolver must not select INDEX.
        Set<ResolutionContracts.ReferenceKind> qualifiedContextual = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.CONDITION);
        ReferenceResolution.Entry oracle = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, rootId,
                        ResolutionContracts.ReferenceKind.CONDITION, qualifiedContextual)), rootId);
        assertAll("FACT-R2-02 oracle",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, oracle.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        oracle.reason()),
                () -> assertEquals(0, oracle.candidates().size()),
                () -> assertEquals(java.util.Optional.empty(), oracle.selectedCandidate()));
    }

    @Test
    void qualifiedContextualTailBindsADataCandidateUnderTheFuturePolicy() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source("R2-QUAL-DATA", """
                    01 G.
                       05 C PIC 9(4).
                    """, "IF A = B OR C OF G CONTINUE END-IF."),
                "r2-qualified-contextual-data.cbl");
        Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class).get(0);
        int rootId = tail.nominalReference().meta().id();
        Set<ResolutionContracts.ReferenceKind> qualifiedContextual = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.CONDITION);
        ReferenceResolution.Entry entry = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, rootId,
                        ResolutionContracts.ReferenceKind.CONDITION, qualifiedContextual)), rootId);
        assertAll("qualified root resolves DATA, never INDEX",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH,
                        entry.reason()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        entry.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)));
    }

    // -------------------------------------------------------------------------
    // FACT-R2-03 — subscripted root não admite INDEX; o subscript mantém a policy
    // própria INDEX/{DATA, INDEX} (what-if controlado como em FACT-R2-02).
    // -------------------------------------------------------------------------
    @Test
    void subscriptedContextualTailDoesNotAdmitIndexAsRootAndSubscriptKeepsOwnPolicy() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source("R2-SUB-INDEX", """
                    01 T OCCURS 2 TIMES INDEXED BY C.
                       05 V PIC X.
                    01 I PIC 9.
                    """, "IF A = B OR C(I) CONTINUE END-IF."),
                "r2-subscripted-contextual-index.cbl");
        Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class).get(0);
        Ast.DataReference root = tail.nominalReference();
        int rootId = root.meta().id();
        int subscriptId = root.subscriptGroups().get(0).subscripts().get(0).meta().id();
        assertAll("subscripted shape stays typed",
                () -> assertEquals("C", root.baseName()),
                () -> assertTrue(root.qualifiers().isEmpty()),
                () -> assertEquals(1, root.subscriptGroups().size()),
                () -> assertEquals(null, root.referenceModification()));

        Set<ResolutionContracts.ReferenceKind> fixedSet = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.INDEX,
                ResolutionContracts.ReferenceKind.CONDITION);
        ReferenceResolution.Entry control = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, rootId,
                        ResolutionContracts.ReferenceKind.CONDITION, fixedSet)), rootId);
        assertAll("FACT-R2-03 control: fixed set would wrongly bind the index as subscripted root",
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        control.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)));

        Set<ResolutionContracts.ReferenceKind> subscriptedContextual = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.CONDITION);
        ReferenceResolution resolution = reResolve(analysis,
                withOccurrencePolicy(analysis, rootId,
                        ResolutionContracts.ReferenceKind.CONDITION, subscriptedContextual));
        ReferenceResolution.Entry oracle = resolutionEntry(resolution, rootId);
        assertAll("FACT-R2-03 oracle: INDEX is not an admissible namespace of the subscripted root",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, oracle.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        oracle.reason()),
                () -> assertEquals(0, oracle.candidates().size()),
                () -> assertEquals(java.util.Optional.empty(), oracle.selectedCandidate()));
        ReferenceResolution.Entry subscript = resolutionEntry(resolution, subscriptId);
        assertAll("subscript keeps its own policy and binds the data candidate",
                () -> assertEquals(ResolutionContracts.ReferenceRole.SUBSCRIPT, subscript.occurrence().role()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX, subscript.occurrence().kind()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        subscript.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, subscript.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        subscript.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)));
    }

    // -------------------------------------------------------------------------
    // FACT-R2-04 — a policy de relation operand também é shape-sensitive. O collector
    // atual entrega INDEX/{DATA, INDEX} para qualquer shape do object (registra
    // PREEXISTING_RELATION_OCCURRENCE_OVERADMISSIBILITY); a projeção future
    // {DATA} para qualified/subscripted fecha o gap sem mudar o resolver.
    // -------------------------------------------------------------------------
    @Test
    void relationOperandPolicyIsCurrentlyShapeInsensitiveAndFutureProjectionClosesTheGap() {
        // PREEXISTING_RELATION_OCCURRENCE_OVERADMISSIBILITY: the collector today attaches
        // INDEX/{DATA, INDEX} to every relation object regardless of nominal shape. Valid
        // IBM sources cannot expose the wrong binding (their roots are DATA/CONDITION), so
        // the overadmissibility is registered on the occurrence policy itself and then
        // closed by the documented shape-sensitive projection.
        AstBoundaryTestSupport.Analysis qualified = AstBoundaryTestSupport.analyze(source("R2-REL-QUAL", """
                    01 G.
                       05 C PIC 9(4).
                    """, """
                    IF A = C CONTINUE END-IF.
                    IF A = C OF G CONTINUE END-IF.
                    """),
                "r2-relation-operand-qualified.cbl");
        List<Ast.RelationCondition> qualifiedRelations = AstBoundaryTestSupport.nodes(
                qualified, Ast.RelationCondition.class);
        assertEquals(2, qualifiedRelations.size());
        assertRelationPolicyUsesShape(qualified, qualifiedRelations);

        AstBoundaryTestSupport.Analysis subscripted = AstBoundaryTestSupport.analyze(source("R2-REL-SUB", """
                    01 T.
                       05 C PIC 9(4) OCCURS 3 TIMES.
                    01 I PIC 9.
                    """, "IF A = C(I) CONTINUE END-IF."),
                "r2-relation-operand-subscripted.cbl");
        List<Ast.RelationCondition> subscriptedRelations = AstBoundaryTestSupport.nodes(
                subscripted, Ast.RelationCondition.class);
        assertEquals(1, subscriptedRelations.size());
        assertRelationPolicyUsesShape(subscripted, subscriptedRelations);

        // Future shape-sensitive relation policy on valid roots: qualified/subscripted
        // roots bind DATA under {DATA}; the bare root keeps INDEX admissible ({DATA, INDEX}).
        Set<ResolutionContracts.ReferenceKind> relationBare = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.INDEX);
        Set<ResolutionContracts.ReferenceKind> relationQualifiedOrSubscripted = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA);
        Ast.RelationCondition bareRelation = qualifiedRelations.get(0);
        Ast.RelationCondition qualifiedRelation = qualifiedRelations.get(1);
        ReferenceResolution.Entry bareEntry = resolutionEntry(reResolve(qualified,
                withOccurrencePolicy(qualified, ((Ast.DataReference) bareRelation.object()).meta().id(),
                        ResolutionContracts.ReferenceKind.INDEX, relationBare)),
                ((Ast.DataReference) bareRelation.object()).meta().id());
        assertAll("bare relation root keeps INDEX admissible and binds the DATA candidate",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, bareEntry.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        bareEntry.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)));
        Ast.DataReference qualifiedRoot = (Ast.DataReference) qualifiedRelation.object();
        ReferenceResolution.Entry qualifiedEntry = resolutionEntry(reResolve(qualified,
                withOccurrencePolicy(qualified, qualifiedRoot.meta().id(),
                        ResolutionContracts.ReferenceKind.DATA, relationQualifiedOrSubscripted)),
                qualifiedRoot.meta().id());
        assertAll("qualified relation root under {DATA} binds DATA and never INDEX",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, qualifiedEntry.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        qualifiedEntry.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)),
                () -> assertEquals(0, qualifiedEntry.candidates().stream()
                        .filter(candidate -> candidate.kind() == ResolutionContracts.ReferenceKind.INDEX)
                        .count()));
        Ast.DataReference subscriptedRoot = (Ast.DataReference) subscriptedRelations.get(0).object();
        ReferenceResolution.Entry subscriptedEntry = resolutionEntry(reResolve(subscripted,
                withOccurrencePolicy(subscripted, subscriptedRoot.meta().id(),
                        ResolutionContracts.ReferenceKind.DATA, relationQualifiedOrSubscripted)),
                subscriptedRoot.meta().id());
        assertAll("subscripted relation root under {DATA} binds DATA and never INDEX",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, subscriptedEntry.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        subscriptedEntry.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)),
                () -> assertEquals(0, subscriptedEntry.candidates().stream()
                        .filter(candidate -> candidate.kind() == ResolutionContracts.ReferenceKind.INDEX)
                        .count()));
    }

    private static void assertRelationPolicyUsesShape(
            AstBoundaryTestSupport.Analysis analysis, List<Ast.RelationCondition> relations) {
        ReferenceOccurrences occurrences = analysis.occurrences().values().iterator().next();
        for (Ast.RelationCondition relation : relations) {
            Ast.DataReference object = (Ast.DataReference) relation.object();
            ReferenceOccurrences.Occurrence root = occurrence(occurrences, object.meta().id());
            Set<ResolutionContracts.ReferenceKind> expected = ReferenceOccurrenceCollector
                    .relationOperandKinds(object);
            ResolutionContracts.ReferenceKind primary = ReferenceOccurrenceCollector
                    .indexAdmissibleNominalShape(object)
                    ? ResolutionContracts.ReferenceKind.INDEX : ResolutionContracts.ReferenceKind.DATA;
            assertAll("shape-sensitive relation-operand policy: " + object.writtenText(),
                    () -> assertEquals(primary, root.kind()),
                    () -> assertEquals(expected, root.admissibleKinds()));
        }
    }

    @Test
    void qualifiedAndSubscriptedRelationRootsRejectAnIndexCandidateOnlyUnderShapePolicy() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source("R2-REL-INDEX", """
                    01 T OCCURS 2 TIMES INDEXED BY C.
                       05 V PIC X.
                    01 I PIC 9.
                    """, """
                    IF A = C OF T CONTINUE END-IF.
                    IF A = C(I) CONTINUE END-IF.
                    """),
                "r2-relation-operand-index-shape.cbl");
        List<Ast.RelationCondition> relations = AstBoundaryTestSupport.nodes(
                analysis, Ast.RelationCondition.class);
        assertEquals(2, relations.size());
        Set<ResolutionContracts.ReferenceKind> relationBare = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.INDEX);
        Set<ResolutionContracts.ReferenceKind> relationQualifiedOrSubscripted = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA);
        for (Ast.RelationCondition relation : relations) {
            Ast.DataReference root = (Ast.DataReference) relation.object();
            int rootId = root.meta().id();
            // Control: the only same-name candidate is the index-name C declared under T;
            // under the current relation policy {DATA, INDEX} the resolver WOULD bind INDEX
            // for the qualified/subscripted shape (shape-blind overadmission at model level).
            ReferenceResolution.Entry current = resolutionEntry(reResolve(analysis,
                    withOccurrencePolicy(analysis, rootId,
                            ResolutionContracts.ReferenceKind.INDEX, relationBare)), rootId);
            assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                    current.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null),
                    "control: current policy would bind the index-name for " + root.writtenText());
            // Oracle: the future shape-sensitive relation policy {DATA} excludes INDEX.
            ReferenceResolution.Entry entry = resolutionEntry(reResolve(analysis,
                    withOccurrencePolicy(analysis, rootId,
                            ResolutionContracts.ReferenceKind.DATA,
                            relationQualifiedOrSubscripted)), rootId);
            assertAll("shape policy for " + root.writtenText(),
                    () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, entry.status()),
                    () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                            entry.reason()),
                    () -> assertEquals(java.util.Optional.empty(), entry.selectedCandidate()),
                    () -> assertEquals(0, entry.candidates().stream()
                            .filter(candidate -> candidate.kind()
                                    == ResolutionContracts.ReferenceKind.INDEX)
                            .count()));
        }
    }

    // -------------------------------------------------------------------------
    // FACT-R3-01/R3-02/R3-03 — reference modification is a distinct typed
    // dimension of the relation root. It is not a subscript, even though both
    // spellings use parentheses, and it invalidates the old "bare" predicate
    // that inspected only qualifiers and subscript groups.
    // -------------------------------------------------------------------------
    @Test
    void referenceModifiedRelationOperandIsNotIndexAdmissibleByNominalShape() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                source("R3-REFMOD-DATA", "01 C PIC X(10).",
                        "IF A = C(1:2) CONTINUE END-IF."),
                "r3-reference-modified-relation.cbl");
        Ast.RelationCondition relation = AstBoundaryTestSupport.nodes(
                analysis, Ast.RelationCondition.class).get(0);
        Ast.DataReference root = assertInstanceOf(Ast.DataReference.class, relation.object());

        assertAll("FACT-R3-01 typed root shape",
                () -> assertEquals("C", root.baseName()),
                () -> assertTrue(root.qualifiers().isEmpty()),
                () -> assertTrue(root.subscriptGroups().isEmpty(),
                        "C(I) would be a table element/subscript shape"),
                () -> assertTrue(root.referenceModification() != null,
                        "C(1:2) is a reference modification shape"));

        ReferenceOccurrences occurrences = analysis.occurrences().values().iterator().next();
        ReferenceOccurrences.Occurrence current = occurrence(occurrences, root.meta().id());
        assertAll("FACT-R3-02 implemented reference-modification shape policy",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, current.kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        current.admissibleKinds(),
                        "reference modification excludes INDEX"));

        Set<ResolutionContracts.ReferenceKind> futureKinds = Set.of(
                ResolutionContracts.ReferenceKind.DATA);
        ReferenceResolution.Entry future = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, root.meta().id(),
                        ResolutionContracts.ReferenceKind.DATA, futureKinds)), root.meta().id());
        assertAll("FACT-R3-03 future policy and unchanged resolver",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, future.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                        future.reason()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        future.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)),
                () -> assertEquals(futureKinds, future.occurrence().admissibleKinds()));
    }

    @Test
    void referenceModifiedIndexNameIsExcludedByFuturePolicyInControlledWhatIf() {
        // IBM does not make a reference-modified index-name a valid source
        // construct. This is therefore a nominal candidate-model what-if only:
        // it demonstrates the exact false acceptance caused by retaining INDEX
        // in the old shape-blind policy, without treating invalid source as an
        // IBM language oracle.
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                source("R3-REFMOD-INDEX-WHAT-IF", """
                    01 T OCCURS 2 TIMES INDEXED BY C.
                       05 V PIC X.
                    """, "IF A = C(1:2) CONTINUE END-IF."),
                "r3-reference-modified-index-what-if.cbl");
        Ast.DataReference root = assertInstanceOf(Ast.DataReference.class,
                AstBoundaryTestSupport.nodes(analysis, Ast.RelationCondition.class)
                        .get(0).object());
        assertTrue(root.referenceModification() != null);
        assertTrue(root.qualifiers().isEmpty());
        assertTrue(root.subscriptGroups().isEmpty());

        Set<ResolutionContracts.ReferenceKind> oldKinds = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.INDEX);
        ReferenceResolution.Entry oldPolicy = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, root.meta().id(),
                        ResolutionContracts.ReferenceKind.INDEX, oldKinds)), root.meta().id());
        assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                oldPolicy.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null),
                "negative control: old policy could accept INDEX as the root");

        Set<ResolutionContracts.ReferenceKind> futureKinds = Set.of(
                ResolutionContracts.ReferenceKind.DATA);
        ReferenceResolution.Entry futurePolicy = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, root.meta().id(),
                        ResolutionContracts.ReferenceKind.DATA, futureKinds)), root.meta().id());
        assertAll("NEG-INDEX-REFMOD-01",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        futurePolicy.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        futurePolicy.reason()),
                () -> assertEquals(java.util.Optional.empty(), futurePolicy.selectedCandidate()),
                () -> assertTrue(futurePolicy.candidates().stream().noneMatch(candidate ->
                        candidate.kind() == ResolutionContracts.ReferenceKind.INDEX)));
    }

    @Test
    void distributedReferenceModifiedRelationOperandUsesTheSameFutureShapePolicy() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                source("R3-DISTRIBUTED-REFMOD", "01 C PIC X(10).",
                        "IF A = (C(1:2) OR D) CONTINUE END-IF."),
                "r3-distributed-reference-modified-relation.cbl");
        Ast.RelationCondition relation = AstBoundaryTestSupport.nodes(
                analysis, Ast.RelationCondition.class).get(0);
        Ast.DistributedOperandGroup distributed = assertInstanceOf(
                Ast.DistributedOperandGroup.class, relation.object());
        Ast.DataReference modified = assertInstanceOf(Ast.DataReference.class,
                distributed.operands().get(0));
        assertAll("distributed operand is structurally supported",
                () -> assertEquals("C", modified.baseName()),
                () -> assertTrue(modified.qualifiers().isEmpty()),
                () -> assertTrue(modified.subscriptGroups().isEmpty()),
                () -> assertTrue(modified.referenceModification() != null));

        ReferenceOccurrences occurrences = analysis.occurrences().values().iterator().next();
        ReferenceOccurrences.Occurrence current = occurrence(occurrences, modified.meta().id());
        assertAll("distributed implemented shape policy",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, current.kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        current.admissibleKinds()));

        Set<ResolutionContracts.ReferenceKind> futureKinds = Set.of(
                ResolutionContracts.ReferenceKind.DATA);
        ReferenceResolution.Entry future = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, modified.meta().id(),
                        ResolutionContracts.ReferenceKind.DATA, futureKinds)), modified.meta().id());
        assertAll("distributed future policy reuses one helper contract",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, future.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        future.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)),
                () -> assertEquals(futureKinds, future.occurrence().admissibleKinds()));
    }

    // -------------------------------------------------------------------------
    // FACT-R2-QS — C OF G(I): root C shape-sensitive; G e I independentes; o root
    // nunca recebe reference modification (grammar conditionNameReference não tem
    // referenceModifier).
    // -------------------------------------------------------------------------
    @Test
    void qualifiedSubscriptedTailKeepsRootPolicyAndIndependentChildren() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source("R2-QS", """
                    01 G OCCURS 3 TIMES.
                       05 C PIC 9(4).
                    01 I PIC 9.
                    """, "IF A = B OR C OF G(I) CONTINUE END-IF."),
                "r2-qualified-subscripted-tail.cbl");
        Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(
                analysis, Ast.ContextualConditionTail.class).get(0);
        Ast.DataReference root = tail.nominalReference();
        int rootId = root.meta().id();
        int qualifierId = root.qualifiers().get(0).reference().meta().id();
        int subscriptId = root.subscriptGroups().get(0).subscripts().get(0).meta().id();
        assertAll("typed root shape C OF G(I)",
                () -> assertEquals("C", root.baseName()),
                () -> assertEquals(1, root.qualifiers().size()),
                () -> assertEquals("G", root.qualifiers().get(0).name()),
                () -> assertEquals(1, root.subscriptGroups().size()),
                () -> assertEquals(null, root.referenceModification(),
                        "reference modification is not producible on a contextual tail root"));

        ReferenceOccurrences occurrences = analysis.occurrences().values().iterator().next();
        ReferenceOccurrences.Occurrence qualifier = occurrence(occurrences, qualifierId);
        ReferenceOccurrences.Occurrence subscript = occurrence(occurrences, subscriptId);
        assertAll("children remain independent of the root override",
                () -> assertEquals(ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT,
                        qualifier.role()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        qualifier.admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ReferenceRole.SUBSCRIPT, subscript.role()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        subscript.admissibleKinds()));

        Set<ResolutionContracts.ReferenceKind> qualifiedSubscriptedContextual = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.CONDITION);
        ReferenceResolution resolution = reResolve(analysis,
                withOccurrencePolicy(analysis, rootId,
                        ResolutionContracts.ReferenceKind.CONDITION,
                        qualifiedSubscriptedContextual));
        ReferenceResolution.Entry rootEntry = resolutionEntry(resolution, rootId);
        assertAll("root resolves DATA under the future policy",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, rootEntry.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH,
                        rootEntry.reason()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        rootEntry.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)));
    }

    // -------------------------------------------------------------------------
    // F4 / CO-20 — NOT sobre a primeira relation não termina a abbreviation nem
    // torna C standalone; os três casos NOT C / NOT A = B OR C / A NOT = C
    // permanecem estruturalmente distintos.
    // -------------------------------------------------------------------------
    @Test
    void leadingLogicalNotDoesNotTerminateAbbreviationNorTurnTailStandalone() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source("R2-NOT", """
                    01 T OCCURS 2 TIMES INDEXED BY C.
                       05 V PIC X.
                    """, """
                    IF NOT A = B OR C CONTINUE END-IF.
                    IF NOT C CONTINUE END-IF.
                    IF A NOT = C CONTINUE END-IF.
                    """),
                "r2-not-abbreviation.cbl");
        List<Ast.IfStatement> statements = AstBoundaryTestSupport.nodes(
                analysis, Ast.IfStatement.class);
        assertEquals(3, statements.size());

        Ast.LogicalCondition abbreviated = assertInstanceOf(
                Ast.LogicalCondition.class, statements.get(0).condition());
        Ast.NegatedCondition negatedFirstRelation = assertInstanceOf(
                Ast.NegatedCondition.class, abbreviated.operands().get(0));
        Ast.RelationCondition firstRelation = assertInstanceOf(
                Ast.RelationCondition.class, negatedFirstRelation.operand());
        assertInstanceOf(Ast.DataReference.class, firstRelation.subject());
        assertInstanceOf(Ast.DataReference.class, firstRelation.object());
        Ast.ContextualConditionTail tail = assertInstanceOf(
                Ast.ContextualConditionTail.class, abbreviated.operands().get(1),
                "the NOT over the first relation keeps C in the contextual tail position");

        Ast.NegatedCondition standalone = assertInstanceOf(
                Ast.NegatedCondition.class, statements.get(1).condition());
        assertInstanceOf(Ast.DataReference.class, standalone.operand(),
                "NOT C stays a standalone condition-name surface");

        Ast.RelationCondition relationalNot = assertInstanceOf(
                Ast.RelationCondition.class, statements.get(2).condition());
        assertTrue(relationalNot.relationalOperator().toUpperCase(java.util.Locale.ROOT)
                        .contains("NOT"),
                "A NOT = C keeps NOT inside the relational operator");
        assertInstanceOf(Ast.DataReference.class, relationalNot.object());

        // CO-20: C bare uses contextualPolicy(bare) = {DATA, INDEX, CONDITION}, not
        // standaloneConditionPolicy = {CONDITION}; the resolver binds the index-name
        // only through the contextual projection.
        Ast.DataReference root = tail.nominalReference();
        int rootId = root.meta().id();
        Set<ResolutionContracts.ReferenceKind> contextualBare = EnumSet.of(
                ResolutionContracts.ReferenceKind.DATA,
                ResolutionContracts.ReferenceKind.INDEX,
                ResolutionContracts.ReferenceKind.CONDITION);
        ReferenceResolution.Entry contextual = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, rootId,
                        ResolutionContracts.ReferenceKind.CONDITION, contextualBare)), rootId);
        assertAll("CO-20 contextual",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, contextual.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        contextual.selectedCandidate().map(ReferenceResolution.Candidate::kind).orElse(null)));

        Set<ResolutionContracts.ReferenceKind> standaloneCondition = Set.of(
                ResolutionContracts.ReferenceKind.CONDITION);
        ReferenceResolution.Entry standaloneEntry = resolutionEntry(reResolve(analysis,
                withOccurrencePolicy(analysis, rootId,
                        ResolutionContracts.ReferenceKind.CONDITION, standaloneCondition)), rootId);
        assertAll("CO-20 standalone contrast: the standalone policy cannot bind the index-name",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        standaloneEntry.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        standaloneEntry.reason()));
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
        return withOccurrencePolicy(analysis, astNodeId,
                ResolutionContracts.ReferenceKind.CONDITION, admissibleKinds);
    }

    /**
     * What-if projection (Discovery FACT only): rebuilds a single collected occurrence with a
     * documented future policy ({@code kind} primary + {@code admissibleKinds}) and leaves every
     * other occurrence untouched, so the CURRENT resolver can be exercised against the
     * shape-sensitive policies of WORK-COND-005 Round 2 without any production change.
     */
    private static Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> withOccurrencePolicy(
            AstBoundaryTestSupport.Analysis analysis, int astNodeId,
            ResolutionContracts.ReferenceKind kind,
            Set<ResolutionContracts.ReferenceKind> admissibleKinds) {
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> result = new LinkedHashMap<>();
        analysis.occurrences().forEach((unitId, product) -> result.put(unitId,
                new ReferenceOccurrences(product.occurrences().stream().map(occurrence -> {
                    if (occurrence.referenceAstNodeId() != astNodeId) return occurrence;
                    return new ReferenceOccurrences.Occurrence(occurrence.id(), occurrence.programUnitId(),
                            occurrence.referenceAstNodeId(), occurrence.scopeId(), kind, admissibleKinds,
                            occurrence.role(), occurrence.grammarRule(), occurrence.writtenText(),
                            occurrence.meta(), occurrence.preservation());
                }).toList())));
        return Map.copyOf(result);
    }

    private static ReferenceResolution reResolve(AstBoundaryTestSupport.Analysis analysis,
                                                 Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> projected) {
        return new CobolReferenceResolver(ResolutionContracts.CobolResolutionPolicy.initial())
                .resolve(analysis.model(), analysis.tables(), projected);
    }

    private static ReferenceResolution.Entry resolutionEntry(ReferenceResolution resolution, int astNodeId) {
        return resolution.entries().stream()
                .filter(entry -> entry.occurrence().referenceAstNodeId() == astNodeId)
                .findFirst().orElseThrow();
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
