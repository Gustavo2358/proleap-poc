package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

/** Every supported family must join this contract before its feature is complete. */
class CompositionalityContractTest {
    static String program(int calls, int ifs, int performs) {
        var b = new StringBuilder("IDENTIFICATION DIVISION.\nPROGRAM-ID. COMPOSE.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n01 WS-A PIC X(8).\n01 FLAG PIC X.\nPROCEDURE DIVISION.\nMAIN.\n");
        for (int n = 0; n < calls; n++) b.append("MOVE 'PROGA' TO WS-A.\nCALL WS-A.\n");
        for (int n = 0; n < ifs; n++) b.append("IF FLAG = 'Y'\n MOVE 'PROGA' TO WS-A\nELSE\n MOVE 'PROGB' TO WS-A\nEND-IF.\n");
        for (int n = 0; n < performs; n++) b.append("PERFORM DEFINE-A.\nCALL WS-A.\n");
        b.append("GOBACK.\n");
        if (performs > 0) b.append("DEFINE-A.\nMOVE 'PROGA' TO WS-A.\n");
        return b.toString();
    }
    @Test void everySupportedFamilyAcceptsOneTwoFiveAndFortyOccurrences() throws Exception {
        // A new typed semantic family cannot be added without a multiplicity generator.
        // ObservedStatement is fallback; GOBACK terminates each generated primary path.
        var families = java.util.Arrays.stream(StatementFact.class.getPermittedSubclasses())
                .filter(f -> f != ObservedStatement.class && f != GobackFact.class).toList();
        for (int n : List.of(1, 2, 5, 40)) {
            var mixed = ScalarMoveCheckpoint4ATest.publish(program(n, n, n));
            for (var family : families) assertTrue(mixed.statements().stream().filter(family::isInstance).count() >= n,
                    "Missing compositional generator for " + family.getSimpleName() + " at N=" + n);
        }
        for (int n : List.of(1, 2, 5, 40)) for (var counts : List.of(new int[]{n,0,0}, new int[]{1,n,0}, new int[]{0,0,n})) {
            var source = program(counts[0], counts[1], counts[2]);
            var p = ScalarMoveCheckpoint4ATest.publish(source);
            assertEquals(counts[0]+counts[2], p.calls().size()); assertEquals(counts[1], p.ifs().size());
            assertEquals(counts[2], p.performs().size(), "PERFORM multiplicity " + n);
            assertTrue(p.observedStatements().isEmpty());
            for (var f : p.performs()) {
                assertTrue(f.gapCodes().isEmpty());
                var last = (MoveFact) p.statement(f.targetExit().orElseThrow()).orElseThrow();
                assertTrue(last.normalContinuation().statement().isEmpty(), "intrinsic paragraph end cannot own an activation resume");
            }
            assertArrayEquals(SemanticProductJsonWriter.serialize(p), SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)));
        }
    }
    @Test void focalMixedStressHasTwentyCallsTenIfsFivePerformsAndDozensOfMoves() {
        var p = ScalarMoveCheckpoint4ATest.publish(program(15, 10, 5));
        assertEquals(20, p.calls().size()); assertEquals(10, p.ifs().size()); assertEquals(5, p.performs().size());
        assertEquals(36, p.moves().size()); assertTrue(p.observedStatements().isEmpty());
    }
}
