package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConditionNameSemanticsTest {
    @Test void setUsesFirstValueAndTheAssociatedVariableInWrittenOrder() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 FLAG-A PIC X.\n88 ACTIVE-A VALUES 'Y' 'T'.\n88 INACTIVE-A VALUE 'N'.",
            "SET ACTIVE-A INACTIVE-A TO TRUE.\nIF ACTIVE-A CONTINUE END-IF.\nGOBACK."),"conditions.cbl");
        var facts=ConditionNameSemantics.analyze(a.build(),a.tables(),a.resolution());
        var unit=a.model().programUnits().get(0).id();
        var assignments=facts.sets(unit).values().iterator().next();
        assertEquals(List.of("Y","N"),assignments.stream().map(x->x.value().value()).toList());
        assertEquals(assignments.get(0).use().declaration().parent(),assignments.get(1).use().declaration().parent());
        assertEquals(3,facts.uses(unit).size());
    }
    @Test void falseNeedsAnExplicitValueAndRangesAreNotEnumerated() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 FLAG-A PIC 9(9).\n88 ACTIVE-A VALUE 7 THRU 999999999\n WHEN SET TO FALSE IS 0.\n88 OTHER-A VALUE 2.",
            "SET ACTIVE-A TO FALSE.\nSET OTHER-A TO FALSE.\nGOBACK."),"conditions.cbl");
        var facts=ConditionNameSemantics.analyze(a.build(),a.tables(),a.resolution());
        var unit=a.model().programUnits().get(0).id();
        assertEquals(1,facts.sets(unit).size());
        assertEquals("0",facts.sets(unit).values().iterator().next().get(0).value().value());
        assertEquals(1,facts.failures(unit).size());
        assertEquals("SET_FALSE_VALUE_NOT_DECLARED",facts.failures(unit).values().iterator().next());
        assertEquals(2,facts.metrics().rangeVisits());
    }
    @Test void publishesUsesAndTreesInEveryConditionContext() throws Exception {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 FLAG-A PIC X.\n88 ACTIVE-A VALUE 'Y'.",
            "SET ACTIVE-A TO TRUE.\nIF NOT ACTIVE-A CONTINUE END-IF.\n"
            +"EVALUATE TRUE WHEN ACTIVE-A CONTINUE END-EVALUATE.\n"
            +"PERFORM UNTIL ACTIVE-A CONTINUE END-PERFORM.\nGOBACK."),"conditions.cbl");
        var p=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        var facts=p.conditionNames().orElseThrow();
        assertEquals(4,facts.uses().size());assertEquals(1,facts.assignments().size());
        assertEquals(Set.of("IF","EVALUATE_WHEN/0","PERFORM_UNTIL/0"),facts.predicates().stream().map(x->x.role()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(facts.predicates().stream().allMatch(x->x.tree().complete()));
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(p));
        assertEquals("2.65.0",json.path("contractVersion").asText());
        assertEquals(4,json.path("conditionNames").path("uses").size());
    }
    @Test void publishesLinkageAndQualifiedTableParentsWithoutAllocatingBooleanCells() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 FLAGS.\n05 FLAG-A OCCURS 10 PIC X.\n88 ACTIVE-A VALUE 'Y'.\n01 IDX PIC 9.\nLINKAGE SECTION.\n01 ARG PIC X.\n88 ARG-READY VALUE 'R'.",
            "IF ACTIVE-A OF FLAGS(IDX) CONTINUE END-IF.\nSET ARG-READY TO TRUE.\nGOBACK."),"parents.cbl");
        var p=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);var facts=p.conditionNames().orElseThrow();
        assertEquals(2,facts.uses().size());assertEquals(1,facts.uses().stream().filter(u->!u.indices().isEmpty()).count());
        assertEquals(1,facts.assignments().size());
        assertTrue(facts.definitions().stream().allMatch(d->d.parent().startsWith("data:")));
    }
    @Test void unknownSiblingDoesNotEraseModeledConditionAndOrderedSetsDischargeOnlyTheirGap() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 FLAG-A PIC X.\n88 ACTIVE-A VALUE 'Y'.\n01 COUNT-A PIC 9.",
            "SET ACTIVE-A TO TRUE.\nIF ACTIVE-A AND COUNT-A > 3 CONTINUE END-IF.\nGOBACK."),"mixed.cbl");
        var p=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);var facts=p.conditionNames().orElseThrow();
        assertFalse(facts.predicates().get(0).tree().complete());
        assertEquals(2,facts.uses().size());
        assertFalse(p.gaps().stream().anyMatch(g->g.code().equals("CONDITION_REFERENCE_KIND_NOT_PROJECTED")));
        assertFalse(p.gaps().stream().anyMatch(g->g.code().equals("OBSERVED_STATEMENT_UNSUPPORTED")));
        assertTrue(p.gaps().stream().anyMatch(g->g.code().equals("CONDITION_SEMANTICS_NOT_AVAILABLE")));
    }
    @Test void incompleteInputRetainsEvaluateConditionsAndExplicitArmMembership() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 FLAG-A PIC X.\n88 ACTIVE-A VALUE 'Y'.\nCOPY ABSENT-MEMBER.",
            "EVALUATE TRUE\nWHEN ACTIVE-A CONTINUE\nWHEN OTHER CONTINUE\nEND-EVALUATE.\nGOBACK."),"partial.cbl");
        var p=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertEquals(1,p.conditionNames().orElseThrow().uses().size());
        assertTrue(p.statements().stream().anyMatch(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.EvaluateFact.class::isInstance));
        assertFalse(p.gaps().isEmpty());
    }
    @Test void whenNegationAndFalseSubjectComposeWithoutLosingEitherNot() {
        for(boolean subject:List.of(true,false)) {
            var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("01 FLAG-A PIC X.\n88 ACTIVE-A VALUE 'Y'.",
                "EVALUATE "+(subject?"TRUE":"FALSE")+"\nWHEN NOT ACTIVE-A CONTINUE\nWHEN ACTIVE-A CONTINUE\nEND-EVALUATE.\nGOBACK."),"negative-when.cbl");
            var p=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);var facts=p.conditionNames().orElseThrow();
            var kinds=facts.predicates().stream().collect(java.util.stream.Collectors.toMap(x->x.role(),x->x.tree().kind()));
            assertEquals(subject?"NOT":"TEST",kinds.get("EVALUATE_WHEN/0"));
            assertEquals(subject?"TEST":"NOT",kinds.get("EVALUATE_WHEN/1"));
        }
    }
    @Test void fillerAssociatedVariableHasSourceIdentityWithoutInventedNamedStorage() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 DUMMY-A PIC X.\nLINKAGE SECTION.\n01 ARGUMENT-A.\n05 FILLER PIC X.\n88 READY-A VALUE 'Y'.",
            "SET READY-A TO TRUE.\nIF READY-A CONTINUE END-IF.\nGOBACK."),"anonymous.cbl");
        var p=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertEquals(2,p.conditionNames().orElseThrow().uses().size());
        assertFalse(p.gaps().stream().anyMatch(g->g.code().equals("CONDITION_REFERENCE_KIND_NOT_PROJECTED")));
    }
    @Test void unresolvedEvaluateSelectorRetainsItsNominalGap() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 FLAG-A PIC X.\n88 ACTIVE-A VALUE 'Y'.",
            "EVALUATE TRUE\nWHEN ACTIVE-A CONTINUE\nWHEN MISSING-A CONTINUE\nEND-EVALUATE.\nGOBACK."),"unresolved-when.cbl");
        var p=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertTrue(p.gaps().stream().anyMatch(g->g.scope()==io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.GapScope.NOMINAL_BINDING));
        assertEquals(1,p.conditionNames().orElseThrow().uses().size());
    }

    @Test void resolvedConditionTailDoesNotInheritThePreviousRelation() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 FLAG-A PIC X.\n88 ACTIVE-A VALUE 'Y'.\n01 COUNT-A PIC 9.",
            "IF COUNT-A > 3 OR NOT ACTIVE-A CONTINUE END-IF.\n"
            +"PERFORM UNTIL COUNT-A > 3 OR ACTIVE-A CONTINUE END-PERFORM.\nGOBACK."),"tail.cbl");
        var p=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        var names=p.conditionNames().orElseThrow();
        assertEquals(2,names.predicates().size());
        for(var predicate:names.predicates()) {
            var tree=predicate.tree();assertEquals("OR",tree.kind());
            assertEquals("UNKNOWN",tree.children().get(0).kind());
            var tail=tree.children().get(1);
            if(predicate.role().equals("IF")){assertEquals("NOT",tail.kind());tail=tail.children().get(0);}
            assertEquals("TEST",tail.kind());
        }
    }

    @Test void representationGrowsWithSyntaxAndNeverEnumeratesRangesOrBooleanCombinations() {
        for(int count:List.of(8,32,128)) {
            String condition=String.join("\n OR ",Collections.nCopies(count,"(ACTIVE-A AND NOT ACTIVE-A)"));
            var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
                "01 FLAG-A PIC 9(9).\n88 ACTIVE-A VALUE 1 THRU 999999999.",
                "IF "+condition+" CONTINUE END-IF.\nGOBACK."),"size.cbl");
            var facts=ConditionNameSemantics.analyze(a.build(),a.tables(),a.resolution());
            var unit=a.model().programUnits().get(0).id();
            assertEquals(1,facts.metrics().rangeVisits());
            assertEquals(2*count,facts.uses(unit).size());
            var todo=new ArrayDeque<io.github.gustavo2358.cobolexplorer.semanticproduct.ConditionNames.Tree>();
            todo.push(facts.predicates(unit).get(0).tree());int nodes=0;
            while(!todo.isEmpty()){var t=todo.pop();nodes++;todo.addAll(t.children());}
            assertTrue(nodes<=5*count,"no distributive expansion");
        }
    }

}
