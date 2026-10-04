package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import org.junit.jupiter.api.Test;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static org.junit.jupiter.api.Assertions.*;

class EvaluatePartialContinuationTest {
    private static String source(String body, boolean split) {
        var text = ScalarMoveCheckpoint4ATest.program("01 FLAG PIC X VALUE '0'.", body);
        var fixed = text.lines().map(line -> "       " + line + "\n").collect(java.util.stream.Collectors.joining());
        return split ? fixed.replace("       EVALUATE FLAG", "           EVA\n      -    LUATE FLAG") : fixed;
    }
    private static StatementId call(CobolSemanticPort p, String target) {
        return p.calls().stream().filter(c -> c.target() instanceof LiteralCallTarget l && l.text().equals(target))
            .findFirst().orElseThrow().header().id();
    }
    private static void next(IfFact branch, StatementId target) {
        assertEquals(Optional.of(target), branch.continuation());
        assertEquals(Optional.of(target), branch.normalContinuation().statement());
    }
    @Test void splitEvaluateKeepsCanonicalIfCompletionWithoutInventingExactProvenance() {
        String body = "EVALUATE FLAG\nWHEN '0'\nIF FLAG = '0' CALL 'FIRST' END-IF\n"
            + "WHEN '1' CALL 'SECOND'\nEND-EVALUATE\nCALL 'AFTER'\nGOBACK.";
        for (boolean split : List.of(false, true)) {
            var p = ScalarMoveCheckpoint4ATest.publish(source(body, split));
            next(p.ifs().get(0), call(p, "AFTER"));
            assertNotEquals(call(p, "SECOND"), p.ifs().get(0).continuation().orElseThrow());
            var evaluate = p.statements().get(0);
            assertEquals(!split, evaluate.header().provenance().exact());
            if (split) {
                var typed=assertInstanceOf(EvaluateFact.class, evaluate);
                assertFalse(typed.gapCodes().isEmpty());
                assertEquals(ContinuationAvailability.UNAVAILABLE,typed.normalContinuation().availability());
                assertEquals(Branch.EVALUATE_ARM, p.ifs().get(0).header().containment().branch());
                assertEquals(CoverageStatus.PARTIAL, p.ifs().get(0).header().coverage());
                assertFalse(p.gaps().isEmpty());
            }
        }
    }
    @Test void sameArmSiblingRemainsTheIfCompletion() {
        var p = ScalarMoveCheckpoint4ATest.publish(source("EVALUATE FLAG\nWHEN '0'\n"
            + "IF FLAG = '0' CALL 'FIRST' END-IF\nCALL 'TAIL'\nWHEN OTHER CALL 'SECOND'\n"
            + "END-EVALUATE\nCALL 'AFTER'\nGOBACK.", true));
        next(p.ifs().get(0), call(p, "TAIL"));
    }
    @Test void nestedIfAndOtherArmKeepTheirOwnCompletion() {
        var p = ScalarMoveCheckpoint4ATest.publish(source("EVALUATE FLAG\nWHEN '0'\n"
            + "IF FLAG = '0'\nIF FLAG = '1' CALL 'FIRST' END-IF\nEND-IF\n"
            + "WHEN OTHER\nIF FLAG = '2' CALL 'SECOND' END-IF\nEND-EVALUATE\nCALL 'AFTER'\nGOBACK.", true));
        assertEquals(3, p.ifs().size());
        for (var branch : p.ifs()) next(branch, call(p, "AFTER"));
    }
    @Test void nestedEvaluateDoesNotMakeTheNextOuterArmASuccessor() {
        var p = ScalarMoveCheckpoint4ATest.publish(source("EVALUATE FLAG\nWHEN '0'\nEVALUATE FLAG\n"
            + "WHEN '1' IF FLAG = '0' CALL 'INNER' END-IF\nWHEN OTHER CALL 'INNEROTHER'\nEND-EVALUATE\n"
            + "CALL 'TAIL'\nWHEN OTHER CALL 'OUTEROTHER'\nEND-EVALUATE\nCALL 'AFTER'\nGOBACK.", true));
        next(p.ifs().get(0), call(p, "TAIL"));
    }
    @Test void transformedWhenKeepsTheSameBranchBoundary() {
        String fixed = source("EVALUATE FLAG\nWHEN '0' IF FLAG = '0' CALL 'FIRST' END-IF\n"
            + "WHEN OTHER CALL 'SECOND'\nEND-EVALUATE\nCALL 'AFTER'\nGOBACK.", false)
            .replace("       WHEN OTHER", "           WH\n      -    EN OTHER");
        var p = ScalarMoveCheckpoint4ATest.publish(fixed);
        next(p.ifs().get(0), call(p, "AFTER"));
    }
    @Test void missingDataCopyDoesNotCreateAContinuationAcrossWhenArms() {
        String fixed = source("EVALUATE FLAG\nWHEN '0' IF FLAG = '0' CALL 'FIRST' END-IF\n"
            + "WHEN OTHER CALL 'SECOND'\nEND-EVALUATE\nCALL 'AFTER'\nGOBACK.", false)
            .replace("       PROCEDURE DIVISION.", "       COPY MISSING-DATA.\n       PROCEDURE DIVISION.");
        var p = ScalarMoveCheckpoint4ATest.publish(fixed);
        assertTrue(p.ifs().get(0).normalContinuation().statement().isEmpty());
        assertTrue(p.ifs().get(0).continuation().isEmpty(), "unknown parent must not invent a sibling from another arm");
        assertFalse(p.gaps().isEmpty());
    }
}
