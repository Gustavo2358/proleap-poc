package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
class DependencyPreservationTest {
    @Test void missingInputDoesNotEraseLocalLiteralTransferOrCicsWholeReference() throws Exception {
        var state=CicsProgramControlTest.regional("COPY MISSINGDP.\n01 REC.\n05 TARGET-PGM PIC X(8).",
            "MOVE 'PROGA' TO TARGET-PGM.\nEXEC CICS XCTL PROGRAM(TARGET-PGM) END-EXEC.");
        var move=(MoveFact)state.statements().get(0);
        assertEquals("POSSIBLE_TEXT",move.copySemantics().name());
        assertEquals("PROGA   ",move.textAdjustment().orElseThrow().result().value());
        assertEquals(move.target().binding().selected(),move.target().logicalWholeItem());
        assertTrue(move.target().wholeItemAccess().isEmpty());
        var target=(DataReference)((CicsFact)state.statements().get(1)).target().orElseThrow();
        assertEquals(target.binding().selected(),target.logicalWholeItem());
        assertFalse(target.provenance().exact());assertTrue(target.regionalAccess().isEmpty());
        CicsProgramControlTest.emit("dependency-preservation",state);
    }
    @Test void sliceAndRepeatedReceiverDoNotAcquireWholeLiteralEvidence() {
        for(String declaration:new String[]{"05 TARGET-PGM PIC X(8) OCCURS 2.","05 TARGET-PGM PIC X(8) JUSTIFIED RIGHT."}) {
            var state=CicsProgramControlTest.regional("COPY MISSINGDP.\n01 REC.\n"+declaration,
                "MOVE 'PROGA' TO TARGET-PGM.\nEXEC CICS XCTL PROGRAM(TARGET-PGM(2:4)) END-EXEC.");
            assertNotEquals("POSSIBLE_TEXT",((MoveFact)state.statements().get(0)).copySemantics().name());
            var target=(DataReference)((CicsFact)state.statements().get(1)).target().orElseThrow();
            assertTrue(target.logicalWholeItem().isEmpty());
        }
    }
}
