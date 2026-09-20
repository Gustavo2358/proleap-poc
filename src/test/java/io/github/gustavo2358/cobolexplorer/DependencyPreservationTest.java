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
            if(declaration.contains("OCCURS"))assertNotEquals("POSSIBLE_TEXT",((MoveFact)state.statements().get(0)).copySemantics().name());
            else assertEquals("POSSIBLE_TEXT",((MoveFact)state.statements().get(0)).copySemantics().name(),"omitted representation does not erase literal transfer");
            var target=(DataReference)((CicsFact)state.statements().get(1)).target().orElseThrow();
            assertTrue(target.logicalWholeItem().isEmpty());
        }
    }
    @Test void redefinesWithoutLayoutRetainsSeparateNominalEvidence() throws Exception {
        var state=CicsProgramControlTest.regional("COPY MISSINGDP.\n01 A PIC X(8).\n01 B REDEFINES A PIC X(8).",
            "MOVE 'PROGA' TO B.\nMOVE 'PROGB' TO A.\nEXEC CICS XCTL PROGRAM(B) END-EXEC.");
        var first=(MoveFact)state.statements().get(0);var second=(MoveFact)state.statements().get(1);
        assertEquals(CopySemantics.POSSIBLE_TEXT,first.copySemantics());
        assertEquals(CopySemantics.POSSIBLE_TEXT,second.copySemantics());
        assertNotEquals(first.target().logicalWholeItem(),second.target().logicalWholeItem());
        assertTrue(first.target().regionalAccess().isEmpty());assertTrue(second.target().regionalAccess().isEmpty());
        var target=(DataReference)((CicsFact)state.statements().get(2)).target().orElseThrow();
        assertEquals(first.target().logicalWholeItem(),target.logicalWholeItem());
        CicsProgramControlTest.emit("dependency-preservation-redefines",state);
    }

}
