package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executable surface oracles for WORK-COND-003 (Slice 3 of BACKLOG-COND-001).
 * These oracles derive from the COND-* normative catalog, not from the previous
 * lossy lowering. The tests observe the semantic AST shape only; no test here
 * consults resolution for the surface decision.
 */
class ConditionSurfaceAstTest {

    // ---------------------------------------------------------------------
    // S3-01 — bare contextual tail
    // ---------------------------------------------------------------------

    @Test
    void s3_01_bareTailRemainsContextualInsteadOfSyntheticRelation() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-01", "IF A = B OR C");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.OR, or.connector());
        assertEquals(2, or.operands().size());

        Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class, or.operands().get(0));
        assertAll("written relation A = B",
                () -> assertNotNull(relation.subject()),
                () -> assertEquals("A", ((Ast.DataReference) relation.subject()).baseName()),
                () -> assertEquals("=", relation.relationalOperator()),
                () -> assertEquals("B", ((Ast.DataReference) relation.object()).baseName()));

        Ast.ContextualConditionTail tail = assertInstanceOf(Ast.ContextualConditionTail.class,
                or.operands().get(1));
        assertAll("C stays a binding-dependent contextual tail",
                () -> assertEquals("C", tail.nominalReference().baseName()),
                () -> assertEquals("C", tail.writtenText()),
                () -> assertEquals(1, referencesNamed(analysis, "A"), "A is written exactly once"));

        assertAll("no synthetic A = C",
                () -> assertEquals(1, nodes(analysis, Ast.RelationCondition.class).size()),
                () -> assertEquals(1, nodes(analysis, Ast.ContextualConditionTail.class).size()),
                () -> assertFalse(nodes(analysis, Ast.ContextualConditionTail.class).stream()
                        .anyMatch(t -> t.nominalReference().baseName().equals("A"))));
    }

    // ---------------------------------------------------------------------
    // S3-02 — multiple tails, no abbreviation(0) truncation
    // ---------------------------------------------------------------------

    @Test
    void s3_02_multipleTailsAreAllPreservedAndReachable() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-02", "IF A = B OR C OR D");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.OR, or.connector());
        assertEquals(3, or.operands().size(), or.toString());
        assertInstanceOf(Ast.RelationCondition.class, or.operands().get(0));
        Ast.ContextualConditionTail c = assertInstanceOf(Ast.ContextualConditionTail.class, or.operands().get(1));
        Ast.ContextualConditionTail d = assertInstanceOf(Ast.ContextualConditionTail.class, or.operands().get(2));
        assertAll("both tails preserved",
                () -> assertEquals("C", c.nominalReference().baseName()),
                () -> assertEquals("D", d.nominalReference().baseName()));

        List<Ast.Node> reachable = nodes(analysis);
        assertAll("both tails reachable exactly once through Ast.children",
                () -> assertTrue(reachable.contains(c)),
                () -> assertTrue(reachable.contains(d)),
                () -> assertEquals(1, reachable.stream().filter(n -> n == c).count()),
                () -> assertEquals(1, reachable.stream().filter(n -> n == d).count()));
        assertEquals(1, nodes(analysis, Ast.RelationCondition.class).size(),
                "no synthetic subject/operator materialization");
    }

    // ---------------------------------------------------------------------
    // S3-03 — explicit operator with omitted subject
    // ---------------------------------------------------------------------

    @Test
    void s3_03_explicitOperatorAbbreviationKeepsSubjectOmitted() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-03", "IF A = B OR < C");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(2, or.operands().size());

        Ast.RelationCondition full = assertInstanceOf(Ast.RelationCondition.class, or.operands().get(0));
        assertNotNull(full.subject());

        Ast.RelationCondition abbreviated = assertInstanceOf(Ast.RelationCondition.class, or.operands().get(1));
        assertAll("abbreviated relation preserves written operator and omits subject",
                () -> assertNull(abbreviated.subject(), "subject must stay OMITTED, never cloned"),
                () -> assertEquals("<", abbreviated.relationalOperator()),
                () -> assertEquals("C", ((Ast.DataReference) abbreviated.object()).baseName()),
                () -> assertEquals(1, referencesNamed(analysis, "A"), "A must not be cloned"));
    }

    // ---------------------------------------------------------------------
    // S3-04 — relational NOT belongs to the relational operator
    // ---------------------------------------------------------------------

    @Test
    void s3_04_relationalNotStaysInsideTheOperator() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-04", "IF A NOT = B OR C");
        Ast.LogicalCondition or = conditionOf(analysis);
        Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class, or.operands().get(0));
        assertAll("NOT = is one relational operator",
                () -> assertEquals("NOT =", relation.relationalOperator()),
                () -> assertNotNull(relation.subject()),
                () -> assertInstanceOf(Ast.ContextualConditionTail.class, or.operands().get(1)),
                () -> assertEquals(0, nodes(analysis, Ast.NegatedCondition.class).size(),
                        "no artificial logical NOT around the relation"));
    }

    // ---------------------------------------------------------------------
    // S3-05 — logical NOT wraps only the following fragment
    // ---------------------------------------------------------------------

    @Test
    void s3_05_logicalNotWrapsOnlyTheImmediateTail() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-05", "IF A = B OR NOT C OR D");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(3, or.operands().size());

        Ast.NegatedCondition not = assertInstanceOf(Ast.NegatedCondition.class, or.operands().get(1));
        Ast.ContextualConditionTail tail = assertInstanceOf(Ast.ContextualConditionTail.class, not.operand());
        assertEquals("C", tail.nominalReference().baseName());

        Ast.ContextualConditionTail d = assertInstanceOf(Ast.ContextualConditionTail.class, or.operands().get(2));
        assertAll("D is a later sibling of the NOT, not its child",
                () -> assertEquals("D", d.nominalReference().baseName()),
                () -> assertNotEquals(not, or.operands().get(2)),
                () -> assertEquals(0, nodes(analysis, Ast.RelationCondition.class).stream()
                        .filter(r -> "NOT".equals(r.relationalOperator())).count(),
                        "logical NOT must not become a relational operator"));
    }

    // ---------------------------------------------------------------------
    // S3-06 — structural precedence, no flat MIXED_LOGICAL
    // ---------------------------------------------------------------------

    @Test
    void s3_06_andBindsTighterThanOr() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-06", "IF A = B OR C AND D");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.OR, or.connector());
        assertEquals(2, or.operands().size());
        assertInstanceOf(Ast.RelationCondition.class, or.operands().get(0));

        Ast.LogicalCondition and = assertInstanceOf(Ast.LogicalCondition.class, or.operands().get(1));
        assertAll("AND nests structurally under OR",
                () -> assertEquals(Ast.LogicalConnector.AND, and.connector()),
                () -> assertEquals(2, and.operands().size()),
                () -> assertEquals("C", ((Ast.ContextualConditionTail) and.operands().get(0))
                        .nominalReference().baseName()),
                () -> assertEquals("D", ((Ast.ContextualConditionTail) and.operands().get(1))
                        .nominalReference().baseName()));

        assertAll("no flat mixed list survives anywhere",
                () -> assertEquals(0, nodes(analysis, Ast.OperationExpression.class).stream()
                        .filter(op -> op.operator().equals("MIXED_LOGICAL")).count()),
                () -> assertEquals(2, nodes(analysis, Ast.LogicalCondition.class).size()));
    }

    @Test
    void s3_06b_andChainBeforeOrKeepsTheSameFold() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-06B", "IF A = B AND C OR D");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.OR, or.connector());
        assertEquals(2, or.operands().size());
        Ast.LogicalCondition and = assertInstanceOf(Ast.LogicalCondition.class, or.operands().get(0));
        assertEquals(Ast.LogicalConnector.AND, and.connector());
        assertInstanceOf(Ast.RelationCondition.class, and.operands().get(0));
        assertEquals("C", ((Ast.ContextualConditionTail) and.operands().get(1)).nominalReference().baseName());
        assertEquals("D", ((Ast.ContextualConditionTail) or.operands().get(1)).nominalReference().baseName());
    }

    // ---------------------------------------------------------------------
    // S3-07 — explicit grouping keeps the parenthesis boundary observable
    // ---------------------------------------------------------------------

    @Test
    void s3_07_explicitGroupKeepsBoundaryAndLeavesOutsideTheGroup() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-07", "IF (A = B OR C) AND D");
        Ast.LogicalCondition and = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.AND, and.connector());
        assertEquals(2, and.operands().size());

        Ast.GroupedCondition group = assertInstanceOf(Ast.GroupedCondition.class, and.operands().get(0));
        assertAll("group boundaries are observable spans of the written tokens",
                () -> assertTrue(group.openParenSpan().startLine() > 0),
                () -> assertTrue(group.closeParenSpan().startLine() > 0),
                () -> assertEquals(group.meta().span().startToken(), group.openParenSpan().startToken(),
                        "the group starts at the written ( token"),
                () -> assertEquals(group.meta().span().endToken(), group.closeParenSpan().startToken(),
                        "the group ends at the written ) token"),
                () -> assertTrue(group.openParenSpan().startToken() < group.closeParenSpan().startToken()));

        Ast.LogicalCondition inner = assertInstanceOf(Ast.LogicalCondition.class, group.inner());
        assertEquals(Ast.LogicalConnector.OR, inner.connector());
        assertEquals("C", ((Ast.ContextualConditionTail) inner.operands().get(1))
                .nominalReference().baseName());

        // D is outside the group: it starts its own simple condition after the boundary.
        assertAll("D remains outside the group",
                () -> assertInstanceOf(Ast.DataReference.class, and.operands().get(1)),
                () -> assertEquals("D", ((Ast.DataReference) and.operands().get(1)).baseName()));
    }

    // ---------------------------------------------------------------------
    // S3-08 — distributed operator group is distinguishable from grouping
    // ---------------------------------------------------------------------

    @Test
    void s3_08_distributedOperatorGroupIsNotAGroupingBoundary() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-08", "IF A = (B OR C) AND D");
        Ast.LogicalCondition and = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.AND, and.connector());

        Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class, and.operands().get(0));
        assertEquals("A", ((Ast.DataReference) relation.subject()).baseName());
        assertEquals("=", relation.relationalOperator());
        Ast.DistributedOperandGroup distributed = assertInstanceOf(Ast.DistributedOperandGroup.class,
                relation.object());
        assertAll("the distributed operand group preserves its operands and connectors",
                () -> assertEquals(List.of("B", "C"), distributed.operands().stream()
                        .map(op -> ((Ast.DataReference) op).baseName()).toList()),
                () -> assertEquals(List.of(Ast.LogicalConnector.OR), distributed.connectors()),
                () -> assertEquals(0, nodes(analysis, Ast.GroupedCondition.class).size(),
                        "distribution is not a grouping boundary"));

        assertAll("no expansion to A = B OR A = C and no cloning",
                () -> assertEquals(1, nodes(analysis, Ast.RelationCondition.class).size()),
                () -> assertEquals(1, referencesNamed(analysis, "A")),
                () -> assertEquals(1, nodes(analysis, Ast.DistributedOperandGroup.class).size()));
    }

    // ---------------------------------------------------------------------
    // S3-09 — later complete relation stays complete; E stays a tail
    // ---------------------------------------------------------------------

    @Test
    void s3_09_laterCompleteRelationRedefinesStateWithoutExpansion() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-09", "IF A = B OR C = D OR E");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(3, or.operands().size());

        Ast.RelationCondition first = assertInstanceOf(Ast.RelationCondition.class, or.operands().get(0));
        Ast.RelationCondition second = assertInstanceOf(Ast.RelationCondition.class, or.operands().get(1));
        Ast.ContextualConditionTail e = assertInstanceOf(Ast.ContextualConditionTail.class, or.operands().get(2));
        assertAll("two written complete relations and one contextual tail",
                () -> assertEquals("A", ((Ast.DataReference) first.subject()).baseName()),
                () -> assertEquals("B", ((Ast.DataReference) first.object()).baseName()),
                () -> assertEquals("C", ((Ast.DataReference) second.subject()).baseName()),
                () -> assertEquals("D", ((Ast.DataReference) second.object()).baseName()),
                () -> assertEquals("E", e.nominalReference().baseName()),
                () -> assertEquals(2, nodes(analysis, Ast.RelationCondition.class).size(),
                        "E is not expanded into C = E"));
    }

    // ---------------------------------------------------------------------
    // S3-10 — class condition stays a distinct simple condition
    // ---------------------------------------------------------------------

    @Test
    void s3_10_classConditionIsNeverDowngradedToAContextualTail() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3-10", "IF A = B OR C IS NUMERIC OR D");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(3, or.operands().size());

        Ast.ClassCondition numeric = assertInstanceOf(Ast.ClassCondition.class, or.operands().get(1));
        assertAll("C IS NUMERIC is a structural simple condition",
                () -> assertEquals("C", ((Ast.DataReference) numeric.subject()).baseName()),
                () -> assertEquals("NUMERIC", numeric.className()),
                () -> assertFalse(numeric.negated()));

        assertAll("D survives after the class condition and is not a contextual tail",
                () -> assertInstanceOf(Ast.DataReference.class, or.operands().get(2)),
                () -> assertEquals("D", ((Ast.DataReference) or.operands().get(2)).baseName()),
                () -> assertEquals(0, nodes(analysis, Ast.ContextualConditionTail.class).size()));
    }

    // ---------------------------------------------------------------------
    // Identity, pre-order and provenance
    // ---------------------------------------------------------------------

    @Test
    void newNodesFollowTheCanonicalPreorderAndWrittenProvenance() {
        AstBoundaryTestSupport.Analysis analysis = analyze("IDS", "IF A = B OR C AND D");
        List<Ast.Node> preorder = new ArrayList<>();
        collectPreorder(analysis.model().programUnits().get(0).program(), preorder);
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < preorder.size(); i++) {
            Ast.Node node = preorder.get(i);
            assertEquals(i, node.meta().id(), "pre-order id at position " + i);
            assertTrue(ids.add(node.meta().id()), "id reached twice: " + node.meta().id());
        }
        assertEquals(preorder.size(), ids.size(), "ids must be contiguous with no gaps");
        for (Ast.Node node : preorder) {
            assertTrue(node.meta().span().startLine() > 0,
                    "every written node carries a source span: " + node);
        }

        Ast.RelationCondition abbreviated = nodes(analyze("OMITTED-SPAN", "IF A = B OR 5"),
                Ast.RelationCondition.class).stream()
                .filter(relation -> relation.subject() == null).findFirst().orElseThrow();
        assertAll("omitted subject and operator carry no invented node or span",
                () -> assertNull(abbreviated.subject()),
                () -> assertNull(abbreviated.relationalOperator()),
                () -> assertTrue(nodes(analyze("OMITTED-SPAN", "IF A = B OR 5"), Ast.Node.class).stream()
                        .noneMatch(node -> node.meta().span().startToken() < 0)));
    }

    // ---------------------------------------------------------------------
    // Metamorphic relations
    // ---------------------------------------------------------------------

    @Test
    void m1_caseVariationDoesNotChangeTheStructuralShape() {
        assertEquals(shapeOf(analyze("M1-L", "IF a = b or c")),
                shapeOf(analyze("M1-U", "IF A = B OR C")));
    }

    @Test
    void m2_alphaRenamePreservesTheTopology() {
        assertEquals(topology(analyze("M2-A", "IF A = B OR C AND D")),
                topology(analyze("M2-B", "IF X = Y OR Z AND W")));
    }

    @Test
    void m3_neutralExplicitGroupingOnlyAddsTheGroupNode() {
        Ast.Expression flat = AstBoundaryTestSupport.nodes(analyze("M3-FLAT", "IF A = B"),
                Ast.IfStatement.class).get(0).condition();
        Ast.Expression grouped = AstBoundaryTestSupport.nodes(analyze("M3-GROUPED", "IF (A = B)"),
                Ast.IfStatement.class).get(0).condition();

        Ast.GroupedCondition group = assertInstanceOf(Ast.GroupedCondition.class, grouped);
        Ast.RelationCondition inner = assertInstanceOf(Ast.RelationCondition.class, group.inner());
        assertInstanceOf(Ast.RelationCondition.class, flat);
        assertAll("connectors and operands survive the neutral group",
                () -> assertEquals("A", ((Ast.DataReference) inner.subject()).baseName()),
                () -> assertEquals("B", ((Ast.DataReference) inner.object()).baseName()));
    }

    // ---------------------------------------------------------------------
    // Negative oracle against the naive grammar-rule implementation
    // ---------------------------------------------------------------------

    @Test
    void negativeOracle_bareTailShapeIsContextualRegardlessOfTheInternalGrammarBranch() {
        // If the builder decided "conditionNameReference => CONDITION", the AST would
        // expose a plain DataReference (or a condition node) here. The surface must
        // expose ContextualConditionTail instead: the semantic shape, not grammarRule.
        AstBoundaryTestSupport.Analysis analysis = analyze("NEG-ORACLE", "IF A = B OR C");
        Ast.ContextualConditionTail tail = nodes(analysis, Ast.ContextualConditionTail.class).get(0);
        assertEquals("conditionNameReference",
                tail.meta().origin().grammarRule(),
                "the tail originates from the conditionNameReference branch");
        assertEquals("C", tail.nominalReference().baseName());

        AstBoundaryTestSupport.Analysis explicitOperator = analyze("NEG-ORACLE-2", "IF A = B OR < C");
        Ast.RelationCondition abbreviated = nodes(explicitOperator, Ast.RelationCondition.class).stream()
                .filter(r -> r.subject() == null).findFirst().orElseThrow();
        assertEquals("abbreviation", abbreviated.meta().origin().grammarRule());
        assertAll("both grammar branches converge to the same surface categories",
                () -> assertNull(abbreviated.subject()),
                () -> assertEquals("<", abbreviated.relationalOperator()));
    }

    // ---------------------------------------------------------------------
    // Additional surface completeness checks against known losses
    // ---------------------------------------------------------------------

    @Test
    void multipleAbbreviationsUnderOneConnectorStayFailClosedAndLossless() {
        AstBoundaryTestSupport.Analysis analysis = analyze("MULTI-ABBREV", "IF A = B OR < C > D");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(2, or.operands().size(), or.toString());
        assertInstanceOf(Ast.RelationCondition.class, or.operands().get(0));

        Ast.PreservedExpression preserved = assertInstanceOf(Ast.PreservedExpression.class,
                or.operands().get(1));
        assertAll("grammar-only abbreviation+ stays preserved without invented connectors",
                () -> assertEquals("andOrCondition", preserved.grammarRule()),
                () -> assertEquals(Ast.ReferenceUnderstanding.PRESERVED, preserved.understanding()),
                () -> assertEquals("< C > D", preserved.writtenText()),
                () -> assertEquals(List.of("C", "D"), preserved.recognizedOperands().stream()
                        .map(operand -> ((Ast.DataReference) operand).baseName()).toList()),
                () -> assertEquals(1, nodes(analysis, Ast.LogicalCondition.class).size(),
                        "no synthetic AND/OR between < C and > D"),
                () -> assertEquals(1, nodes(analysis, Ast.RelationCondition.class).size()));

        List<Ast.Node> reachable = nodes(analysis);
        assertAll("no abbreviation is lost; both operands remain reachable",
                () -> assertEquals(1, referencesNamed(analysis, "C")),
                () -> assertEquals(1, referencesNamed(analysis, "D")),
                () -> assertTrue(reachable.stream().anyMatch(node ->
                        node instanceof Ast.DataReference reference && reference.baseName().equals("C"))),
                () -> assertTrue(reachable.stream().anyMatch(node ->
                        node instanceof Ast.DataReference reference && reference.baseName().equals("D"))));
    }

    @Test
    void logicalNotBeforeARelationWrapsOnlyThatRelation() {
        AstBoundaryTestSupport.Analysis analysis = analyze("NOT-RELATION", "IF NOT A = B OR C");
        Ast.LogicalCondition or = conditionOf(analysis);
        Ast.NegatedCondition not = assertInstanceOf(Ast.NegatedCondition.class, or.operands().get(0));
        assertInstanceOf(Ast.RelationCondition.class, not.operand());
        Ast.ContextualConditionTail c = assertInstanceOf(Ast.ContextualConditionTail.class, or.operands().get(1));
        assertEquals("C", c.nominalReference().baseName());
    }

    @Test
    void relationalNotAbbreviationKeepsNotInsideTheOperator() {
        AstBoundaryTestSupport.Analysis analysis = analyze("NOT-EQ-ABBREV", "IF A = B OR NOT = C");
        Ast.RelationCondition abbreviated = nodes(analysis, Ast.RelationCondition.class).stream()
                .filter(r -> r.subject() == null).findFirst().orElseThrow();
        assertAll("NOT = written in the abbreviation is the relational operator",
                () -> assertEquals("NOT =", abbreviated.relationalOperator()),
                () -> assertEquals(0, nodes(analysis, Ast.NegatedCondition.class).size()));
    }

    // ---------------------------------------------------------------------
    // PAREN — the boundary of a group is relative to the current subject
    // ---------------------------------------------------------------------

    @Test
    void paren_01_groupEnclosingTheCurrentSubjectClosesTheState() {
        AstBoundaryTestSupport.Analysis analysis = analyze("PAREN-01", "IF (A = B OR C) AND D");
        Ast.LogicalCondition and = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.AND, and.connector());
        Ast.GroupedCondition group = assertInstanceOf(Ast.GroupedCondition.class, and.operands().get(0));
        Ast.LogicalCondition inner = assertInstanceOf(Ast.LogicalCondition.class, group.inner());
        assertAll("C stays contextual inside the group",
                () -> assertEquals(Ast.LogicalConnector.OR, inner.connector()),
                () -> assertInstanceOf(Ast.RelationCondition.class, inner.operands().get(0)),
                () -> assertEquals("C", ((Ast.ContextualConditionTail) inner.operands().get(1))
                        .nominalReference().baseName()));
        assertAll("D is outside the closed group, not a contextual tail",
                () -> assertInstanceOf(Ast.DataReference.class, and.operands().get(1)),
                () -> assertEquals("D", ((Ast.DataReference) and.operands().get(1)).baseName()));
    }

    @Test
    void paren_02_groupAfterTheCurrentSubjectKeepsTheState() {
        AstBoundaryTestSupport.Analysis analysis = analyze("PAREN-02", "IF A = B OR (C AND D) OR E");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.OR, or.connector());
        assertEquals(3, or.operands().size());
        assertInstanceOf(Ast.RelationCondition.class, or.operands().get(0));

        Ast.GroupedCondition group = assertInstanceOf(Ast.GroupedCondition.class, or.operands().get(1));
        Ast.LogicalCondition inner = assertInstanceOf(Ast.LogicalCondition.class, group.inner());
        assertAll("C and D stay contextual inside the group",
                () -> assertEquals(Ast.LogicalConnector.AND, inner.connector()),
                () -> assertEquals("C", ((Ast.ContextualConditionTail) inner.operands().get(0))
                        .nominalReference().baseName()),
                () -> assertEquals("D", ((Ast.ContextualConditionTail) inner.operands().get(1))
                        .nominalReference().baseName()));

        Ast.ContextualConditionTail e = assertInstanceOf(Ast.ContextualConditionTail.class,
                or.operands().get(2));
        assertEquals("E", e.nominalReference().baseName(),
                "the group after the current subject must not kill the inherited state");
    }

    @Test
    void paren_03_closedGroupLeavesTheFollowingNominalOutsideTheInheritedSequence() {
        AstBoundaryTestSupport.Analysis analysis = analyze("PAREN-03", "IF (A = B) OR C");
        Ast.LogicalCondition or = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.OR, or.connector());
        assertInstanceOf(Ast.GroupedCondition.class, or.operands().get(0));
        assertAll("C is outside the inheritable sequence",
                () -> assertInstanceOf(Ast.DataReference.class, or.operands().get(1)),
                () -> assertEquals("C", ((Ast.DataReference) or.operands().get(1)).baseName()),
                () -> assertEquals(0, nodes(analysis, Ast.ContextualConditionTail.class).size()));
    }

    // ---------------------------------------------------------------------
    // NOT-DOUBLE — leading NOT is logical only over an already-NOT operator
    // ---------------------------------------------------------------------

    @Test
    void notDouble_01_doubleNotSplitsIntoLogicalAndRelationalRoles() {
        AstBoundaryTestSupport.Analysis analysis = analyze("NOT-DOUBLE-01", "IF A = B AND NOT NOT = C");
        Ast.LogicalCondition and = conditionOf(analysis);
        assertEquals(Ast.LogicalConnector.AND, and.connector());
        assertInstanceOf(Ast.RelationCondition.class, and.operands().get(0));

        Ast.NegatedCondition negated = assertInstanceOf(Ast.NegatedCondition.class, and.operands().get(1));
        Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class, negated.operand());
        assertAll("logical NOT wraps the abbreviated relation with relational NOT =",
                () -> assertNull(relation.subject()),
                () -> assertEquals("NOT =", relation.relationalOperator()),
                () -> assertEquals("C", ((Ast.DataReference) relation.object()).baseName()),
                () -> assertEquals(1, nodes(analysis, Ast.NegatedCondition.class).size()),
                () -> assertTrue(nodes(analysis, Ast.RelationCondition.class).stream()
                        .noneMatch(r -> r.relationalOperator() != null
                                && r.relationalOperator().contains("NOT NOT")),
                        "the operator must never collapse into NOT NOT ="));
    }

    @Test
    void notDouble_02_singleNotIsRelationalAndDoubleNotIsLogical() {
        AstBoundaryTestSupport.Analysis single = analyze("NOT-DOUBLE-02A", "IF A = B OR NOT = C");
        AstBoundaryTestSupport.Analysis doubleNot = analyze("NOT-DOUBLE-02B", "IF A = B OR NOT NOT = C");

        Ast.RelationCondition singleRelation = nodes(single, Ast.RelationCondition.class).stream()
                .filter(r -> r.subject() == null).findFirst().orElseThrow();
        assertAll("single NOT = stays the relational operator",
                () -> assertEquals("NOT =", singleRelation.relationalOperator()),
                () -> assertEquals(0, nodes(single, Ast.NegatedCondition.class).size()));

        Ast.NegatedCondition doubleNegation = nodes(doubleNot, Ast.NegatedCondition.class).stream()
                .findFirst().orElseThrow();
        Ast.RelationCondition doubleRelation = assertInstanceOf(Ast.RelationCondition.class,
                doubleNegation.operand());
        assertAll("double NOT splits logical NOT from relational NOT =",
                () -> assertEquals(1, nodes(doubleNot, Ast.NegatedCondition.class).size()),
                () -> assertNull(doubleRelation.subject()),
                () -> assertEquals("NOT =", doubleRelation.relationalOperator()));
    }

    // ---------------------------------------------------------------------
    // SPAN — synthetic AND nodes cover only their own semantic subtree
    // ---------------------------------------------------------------------

    @Test
    void span_01_innerAndStartsAtItsFirstOperandNotTheParentConnector() {
        AstBoundaryTestSupport.Analysis analysis = analyze("SPAN-01", "IF A = B OR C AND D");
        Ast.LogicalCondition or = conditionOf(analysis);
        Ast.LogicalCondition and = assertInstanceOf(Ast.LogicalCondition.class, or.operands().get(1));
        Ast.ContextualConditionTail c = assertInstanceOf(Ast.ContextualConditionTail.class,
                and.operands().get(0));
        assertAll("AND provenance covers only C AND D",
                () -> assertEquals("C AND D", and.writtenText()),
                () -> assertEquals(c.nominalReference().meta().span().startToken(),
                        and.meta().span().startToken()),
                () -> assertEquals("A = B OR C AND D", or.writtenText()));
    }

    @Test
    void span_02_longerAndChainKeepsTheFullOwnSubtree() {
        AstBoundaryTestSupport.Analysis analysis = analyze("SPAN-02", "IF A = B OR C AND D AND E");
        Ast.LogicalCondition or = conditionOf(analysis);
        Ast.LogicalCondition and = assertInstanceOf(Ast.LogicalCondition.class, or.operands().get(1));
        assertEquals(Ast.LogicalConnector.AND, and.connector());
        assertEquals("C AND D AND E", and.writtenText());
        assertEquals(3, and.operands().size());
    }

    @Test
    void span_03_firstChainMayStartAtTheOriginalRelation() {
        AstBoundaryTestSupport.Analysis analysis = analyze("SPAN-03", "IF A = B AND C OR D");
        Ast.LogicalCondition or = conditionOf(analysis);
        Ast.LogicalCondition and = assertInstanceOf(Ast.LogicalCondition.class, or.operands().get(0));
        Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class,
                and.operands().get(0));
        assertAll("the first AND chain starts at its written relation",
                () -> assertEquals("A = B AND C", and.writtenText()),
                () -> assertEquals(relation.meta().span().startToken(), and.meta().span().startToken()));
    }

    // ---------------------------------------------------------------------
    // Distributed operands reuse the existing relation-operand policy
    // ---------------------------------------------------------------------

    @Test
    void distributedOperandsReuseTheRelationOperandPolicy() {
        for (String condition : List.of("IF A = (B OR C)", "IF A = (B AND C)")) {
            AstBoundaryTestSupport.Analysis analysis = analyze("DIST-KINDS", condition);
            for (String name : List.of("A", "B", "C")) {
                List<Set<ResolutionContracts.ReferenceKind>> kinds = admissibleKinds(analysis, name);
                assertAll(condition + " -> " + name,
                        () -> assertEquals(1, kinds.size(),
                                "each written reference keeps exactly one occurrence"),
                        () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                        ResolutionContracts.ReferenceKind.INDEX), kinds.get(0)));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Adversarial surface state machine regression suite
    // ---------------------------------------------------------------------

    @Test
    void surfaceStateMachineRegressionSuite() {
        record Case(String condition, List<String> reachableNames) { }
        List<Case> cases = List.of(
                new Case("A = B OR C", List.of("A", "B", "C")),
                new Case("A = B OR C OR D", List.of("A", "B", "C", "D")),
                new Case("A = B OR < C", List.of("A", "B", "C")),
                new Case("A NOT = B OR C", List.of("A", "B", "C")),
                new Case("A = B OR NOT C OR D", List.of("A", "B", "C", "D")),
                new Case("A = B AND NOT NOT = C", List.of("A", "B", "C")),
                new Case("A = B OR C AND D", List.of("A", "B", "C", "D")),
                new Case("A = B AND C OR D", List.of("A", "B", "C", "D")),
                new Case("(A = B OR C) AND D", List.of("A", "B", "C", "D")),
                new Case("(A = B) OR C", List.of("A", "B", "C")),
                new Case("A = B OR (C AND D) OR E", List.of("A", "B", "C", "D", "E")),
                new Case("A = (B OR C) AND D", List.of("A", "B", "C", "D")),
                new Case("A = B OR C = D OR E", List.of("A", "B", "C", "D", "E")),
                new Case("A = B OR C IS NUMERIC OR D", List.of("A", "B", "C", "D")),
                new Case("A = B OR < C > D", List.of("A", "B", "C", "D")));
        for (Case surfaceCase : cases) {
            AstBoundaryTestSupport.Analysis analysis = analyze("MACHINE", "IF " + surfaceCase.condition());
            List<Ast.Node> preorder = new ArrayList<>();
            collectPreorder(analysis.model().programUnits().get(0).program(), preorder);
            List<String> reachable = new ArrayList<>();
            for (Ast.Node node : preorder) {
                if (node instanceof Ast.DataReference reference) reachable.add(reference.baseName());
            }
            assertAll(surfaceCase.condition(),
                    () -> assertNotNull(AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class)
                            .get(0).condition()),
                    () -> assertEquals(surfaceCase.reachableNames(), reachable,
                            "every written reference must survive the surface"),
                    () -> assertEquals(preorder.size() - 1,
                            preorder.get(preorder.size() - 1).meta().id(),
                            "ids remain contiguous 0..N-1"));
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static AstBoundaryTestSupport.Analysis analyze(String suffix, String condition) {
        String program = "S3-" + suffix.replaceAll("[^A-Za-z0-9]", "-");
        return AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC X.
                01 B PIC X.
                01 C PIC X.
                01 D PIC X.
                01 E PIC X.
                01 W PIC X.
                01 X PIC X.
                01 Y PIC X.
                01 Z PIC X.
                PROCEDURE DIVISION.
                    %s CONTINUE END-IF.
                END PROGRAM %s.
                """.formatted(program, condition, program), program.toLowerCase() + ".cbl");
    }

    private static Ast.LogicalCondition conditionOf(AstBoundaryTestSupport.Analysis analysis) {
        return assertInstanceOf(Ast.LogicalCondition.class,
                AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).get(0).condition());
    }

    private static <T extends Ast.Node> List<T> nodes(AstBoundaryTestSupport.Analysis analysis, Class<T> type) {
        return AstBoundaryTestSupport.nodes(analysis, type);
    }

    private static List<Ast.Node> nodes(AstBoundaryTestSupport.Analysis analysis) {
        return AstBoundaryTestSupport.nodes(analysis);
    }

    private static int referencesNamed(AstBoundaryTestSupport.Analysis analysis, String name) {
        return (int) AstBoundaryTestSupport.nodes(analysis, Ast.DataReference.class).stream()
                .filter(reference -> reference.baseName().equals(name)).count();
    }

    private static List<Set<ResolutionContracts.ReferenceKind>> admissibleKinds(
            AstBoundaryTestSupport.Analysis analysis, String name) {
        return analysis.occurrences().values().stream()
                .flatMap(product -> product.occurrences().stream())
                .filter(occurrence -> occurrence.writtenText().equals(name))
                .map(ReferenceOccurrences.Occurrence::admissibleKinds)
                .toList();
    }

    private static void collectPreorder(Ast.Node root, List<Ast.Node> output) {
        output.add(root);
        for (Ast.Node child : Ast.children(root)) collectPreorder(child, output);
    }

    private static String shapeOf(AstBoundaryTestSupport.Analysis analysis) {
        return nodeShape(AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).get(0).condition());
    }

    private static String nodeShape(Ast.Node node) {
        StringBuilder shape = new StringBuilder(node.getClass().getSimpleName());
        if (node instanceof Ast.LogicalCondition logical) {
            shape.append('[').append(logical.connector()).append('|');
            for (Ast.Expression operand : logical.operands()) shape.append(nodeShape(operand)).append(',');
            shape.append(']');
        } else if (node instanceof Ast.RelationCondition relation) {
            shape.append("[op=").append(relation.relationalOperator());
            if (relation.subject() == null) shape.append(",subject=OMITTED");
            else shape.append(",subject=").append(referenceName(relation.subject()));
            shape.append(",object=").append(nodeShape(relation.object())).append(']');
        } else if (node instanceof Ast.ContextualConditionTail tail) {
            shape.append("[tail:").append(tail.nominalReference().baseName().toUpperCase()).append(']');
        } else if (node instanceof Ast.NegatedCondition negated) {
            shape.append('[').append(nodeShape(negated.operand())).append(']');
        } else if (node instanceof Ast.GroupedCondition group) {
            shape.append("[group:").append(nodeShape(group.inner())).append(']');
        } else if (node instanceof Ast.DistributedOperandGroup distributed) {
            shape.append("[dist:");
            for (Ast.Expression operand : distributed.operands()) shape.append(nodeShape(operand)).append(',');
            shape.append(']');
        } else if (node instanceof Ast.ClassCondition classCondition) {
            shape.append("[class:").append(classCondition.className())
                    .append(":").append(nodeShape(classCondition.subject())).append(']');
        } else if (node instanceof Ast.DataReference reference) {
            shape.append("[ref:").append(reference.baseName().toUpperCase()).append(']');
        } else if (node instanceof Ast.OperationExpression operation) {
            shape.append("[opx:").append(operation.operator()).append(']');
        } else if (node instanceof Ast.LiteralExpression literal) {
            shape.append("[lit]");
        } else {
            shape.append("[node]");
        }
        return shape.toString();
    }

    private static String referenceName(Ast.Expression expression) {
        if (expression instanceof Ast.DataReference reference) return reference.baseName().toUpperCase();
        return expression.getClass().getSimpleName();
    }

    private static String topology(AstBoundaryTestSupport.Analysis analysis) {
        return shapeOf(analysis).replace("A", "N").replace("B", "N").replace("C", "N")
                .replace("D", "N").replace("X", "N").replace("Y", "N")
                .replace("Z", "N").replace("W", "N");
    }
}
