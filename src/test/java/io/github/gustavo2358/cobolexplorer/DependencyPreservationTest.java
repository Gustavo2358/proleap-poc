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
    @Test void redefinesWithoutLayoutPublishesExactLocalTextIdentity() throws Exception {
        var state=CicsProgramControlTest.regional("COPY MISSINGDP.\n01 A PIC X(8).\n01 B REDEFINES A PIC X(8).",
            "MOVE 'PROGA' TO B.\nMOVE 'PROGB' TO A.\nEXEC CICS XCTL PROGRAM(B) END-EXEC.");
        var first=(MoveFact)state.statements().get(0);var second=(MoveFact)state.statements().get(1);
        assertEquals(CopySemantics.POSSIBLE_TEXT,first.copySemantics());
        assertEquals(CopySemantics.POSSIBLE_TEXT,second.copySemantics());
        assertEquals(second.header().id(),first.normalContinuation().statement().orElseThrow());
        assertEquals(state.statements().get(2).header().id(),second.normalContinuation().statement().orElseThrow());
        assertNotEquals(first.target().logicalWholeItem(),second.target().logicalWholeItem());
        assertEquals(2,state.storage().logicalExactViews().size());
        assertEquals(state.storage().logicalExactViews().get(0).representative(),state.storage().logicalExactViews().get(1).representative());
        assertEquals(java.math.BigInteger.valueOf(8),state.storage().logicalExactViews().get(0).length());
        assertTrue(first.target().regionalAccess().isEmpty());assertTrue(second.target().regionalAccess().isEmpty());
        var target=(DataReference)((CicsFact)state.statements().get(2)).target().orElseThrow();
        assertEquals(first.target().logicalWholeItem(),target.logicalWholeItem());
        CicsProgramControlTest.emit("dependency-preservation-redefines",state);
    }
    @Test void exactViewDependsOnDeclarationsAndPositiveRelationOnly() throws Exception {
        String pair="01 A PIC X(8).\n01 B REDEFINES A PIC X(8).";
        String statements="MOVE 'PROGA' TO B.\nMOVE 'PROGB' TO A.\nEXEC CICS XCTL PROGRAM(B) END-EXEC.";
        var missing=CicsProgramControlTest.regional("COPY MISSINGDP.\n"+pair,statements);
        var complete=CicsProgramControlTest.regional(pair,statements);
        CicsProgramControlTest.emit("dependency-preservation-redefines-complete",complete);
        assertEquals(missing.storage().logicalExactViews().stream().map(v->v.length()).toList(),
            complete.storage().logicalExactViews().stream().map(v->v.length()).toList());
        assertEquals(2,complete.storage().logicalExactViews().size());
        assertEquals(complete.storage().logicalExactViews().get(0).representative(),complete.storage().logicalExactViews().get(1).representative());
        assertTrue(missing.storage().gapCodes().contains("INPUT_MISSING"));
        assertFalse(complete.storage().gapCodes().contains("INPUT_MISSING"));
        for(var changed:java.util.List.of("MOVE 'OTHER' TO B.\nEXEC CICS XCTL PROGRAM(B) END-EXEC.",
                "MOVE 'PROGB' TO A.\nMOVE 'PROGA' TO B.\nEXEC CICS XCTL PROGRAM(B) END-EXEC."))
            assertEquals(missing.storage().logicalExactViews(),CicsProgramControlTest.regional("COPY MISSINGDP.\n"+pair,changed).storage().logicalExactViews());
        assertTrue(CicsProgramControlTest.regional("COPY MISSINGDP.\n01 A PIC X(8).\n01 B PIC X(8).",statements)
            .storage().logicalExactViews().isEmpty());
        assertTrue(CicsProgramControlTest.regional("COPY MISSINGDP.\n01 A PIC X(8).\n01 B REDEFINES A PIC X(4).",statements)
            .storage().logicalExactViews().isEmpty());
        var partial=CicsProgramControlTest.regional("01 A PIC X(8).\n01 B REDEFINES A PIC X(4).",statements).storage();
        assertTrue(partial.logicalExactViews().isEmpty());
        assertEquals(partial.views().get(0).base(),partial.views().get(1).base());
        assertEquals(partial.views().get(0).offset(),partial.views().get(1).offset());
        assertNotEquals(partial.views().get(0).extent(),partial.views().get(1).extent());
    }

}
