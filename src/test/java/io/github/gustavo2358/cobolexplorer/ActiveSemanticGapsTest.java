package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static org.junit.jupiter.api.Assertions.*;

class ActiveSemanticGapsTest {
    private static CobolSemanticPort publish(String data, String code) {
        var a = AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data, code), "active-gaps.cbl");
        return EofUnitBoundaryTest.publish(a, 0, StorageLayoutSemantics.Profile.UNSPECIFIED);
    }
    private static List<Gap> gaps(CobolSemanticPort p, String code) {
        return p.gaps().stream().filter(g -> g.code().equals(code)).toList();
    }
    private static State copy(CobolSemanticPort p, List<Gap> gaps, Optional<ControlTopology> t) {
        return new State(p.unit(), p.policy(), p.dataDeclarations(), p.statements(), gaps,
            p.coverage(), p.entryInventory(), p.storageIndependence(), p.storage(), p.fileInventory(),
            p.sourceDependencies(), p.ordinaryContinuations(), t, t.isPresent() ? p.factDependencies() : Optional.empty(), p.nominalValues());
    }
    @Test void noOpHasNoCapabilityGapButArithmeticStillDoes() {
        var p = publish("01 N PIC 9.", "CONTINUE.\nADD 1 TO N.\nGOBACK.");
        var noOp = p.statements().stream().filter(s -> s instanceof ObservedStatement o && o.effects().filter(e -> e.proof() == EffectProof.NO_OP).isPresent()).findFirst().orElseThrow();
        assertTrue(p.gaps().stream().noneMatch(g -> g.statement().equals(noOp.header().id())));
        assertEquals(1, gaps(p,"OBSERVED_STATEMENT_UNSUPPORTED").size());
        assertThrows(IllegalArgumentException.class, () -> copy(p,p.gaps(),Optional.empty()));
    }
    @Test void performIsolationRestrictionsAreNotPublishedAndUnknownTargetsRemain() {
        var p = publish("01 N PIC 9.", "PERFORM BODY-A.\nPERFORM BODY-A.\nGOBACK.\nBODY-A.\nADD 1 TO N.");
        assertTrue(p.gaps().stream().noneMatch(g -> g.code().contains("ISOLATED_PRIMARY") || g.code().equals("PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN") || g.code().equals("PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED")));
        assertFalse(gaps(p,"PERFORM_RANGE_CONTROL_NOT_PROVEN").isEmpty());
        assertFalse(p.controlTopology().orElseThrow().bindings().isEmpty());
        var unknown = publish("", "PERFORM ABSENT-PARAGRAPH.\nGOBACK.");
        assertTrue(!unknown.gaps().isEmpty());
    }
    @Test void partialAndPossibilityProofsCannotDischargeNoOp() {
        var p = publish("", "CONTINUE.\nGOBACK.");
        var t = p.controlTopology().orElseThrow();
        for (var kind : List.of(ControlTopology.ProofKind.PARTIAL_UNKNOWN, ControlTopology.ProofKind.CONTROL_POSSIBILITY)) {
            var proofs = t.proofs().stream().map(x -> x.rule().equals("statement-scope")
                ? new ControlTopology.Proof(x.id(),kind,x.rule(),x.provenance(),x.dependencies()) : x).toList();
            assertThrows(IllegalArgumentException.class, () -> {
            var weaker = new ControlTopology(t.authority(),t.occurrences(),t.regions(),t.boundaries(),t.outcomes(),t.bindings(),proofs,
                t.exceptionalEvents(),t.fileFlows(),t.sourceContinuations(),t.entryPoints(),t.conditionRegistrations(),t.conditionEvents());
            copy(p,p.gaps(),Optional.of(weaker));
            });
        }
    }
    @Test void currentTextPredicateAndNestedMembershipHaveNoObsoleteGaps() {
        var p = publish("01 NAME-A PIC X(8).", "IF NAME-A = LOW-VALUES OR SPACES\nCONTINUE\nEND-IF.\nGOBACK.");
        assertTrue(gaps(p,"CONDITION_SEMANTICS_NOT_AVAILABLE").isEmpty());
        assertFalse(gaps(p,"IF_OUTSIDE_SIMPLE_PROFILE").isEmpty());
        var nested = publish("01 N PIC 9.", "PERFORM UNTIL N > 2\nCONTINUE\nADD 1 TO N\nEND-PERFORM.\nGOBACK.");
        assertTrue(gaps(nested,"CONTAINMENT_NOT_PROJECTED").isEmpty());
        assertFalse(nested.gaps().isEmpty());
    }
    @Test void cicsAndPhysicalMoveLimitationsRemainExplicit() {
        var p = publish("01 REC-A.\n 05 NAME-A PIC X(8) OCCURS 2.\n01 NAME-B PIC X(8).",
            "MOVE NAME-A(1) TO NAME-B.\nCALL NAME-B.\nEXEC CICS SYNCPOINT END-EXEC.\nGOBACK.");
        assertFalse(gaps(p,"MOVE_IDENTITY_NOT_PROVEN").isEmpty());
        assertFalse(gaps(p,"DYNAMIC_CALL_TARGET_VALUE_UNKNOWN").isEmpty());
        assertFalse(gaps(p,"CICS_COMMAND_EFFECTS_NOT_MODELED").isEmpty());
    }
    @Test void removingAnUnprovedCapabilityIsRejected() {
        var p = publish("01 N PIC 9.", "ADD 1 TO N.\nGOBACK.");
        assertThrows(IllegalArgumentException.class, () -> copy(p,List.of(),p.controlTopology()));
    }
}
