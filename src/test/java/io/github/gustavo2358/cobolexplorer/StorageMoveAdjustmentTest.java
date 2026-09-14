package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageAccessTest.fixture;
import static io.github.gustavo2358.cobolexplorer.StorageAccessSemantics.*;

class StorageMoveAdjustmentTest {
    @Test void literalsPadAndTruncateWithIbm1047Space() {
        var shortMove=fixture("01 DST.\n05 TEXT-PART PIC X(4).","MOVE 'AB' TO DST.").effects().moves().iterator().next();
        assertEquals(MoveKind.FITTED_LITERAL_BYTES,shortMove.kind());assertEquals(List.of(193,194,64,64),shortMove.bytes());
        var longMove=fixture("01 DST PIC X(2).","MOVE 'ABCD' TO DST.").effects().moves().iterator().next();
        assertEquals(List.of(193,194),longMove.bytes());
    }
    @Test void dataSizeChangesAreValueFitsAndEqualRangesRemainRawCopies() {
        for(int n:List.of(2,4,6)) {
            var m=fixture("01 SRC PIC X(4).\n01 DST PIC X("+n+").","MOVE SRC TO DST.").effects().moves().iterator().next();
            assertEquals(n==4?"COPY_BYTES":"FIT_TEXT",m.kind().name());
        }
    }
    @Test void aliasDestinationCanFitButOverlappingSourceRemainsUnknown() {
        var f=fixture("01 AREA-A PIC X(4).\n01 AREA-B REDEFINES AREA-A PIC X(4).\n01 SRC PIC X(2).","MOVE SRC TO AREA-B.");
        assertEquals("FIT_TEXT",f.effects().moves().iterator().next().kind().name());
        var overlap=fixture("01 AREA-A PIC X(4).\n01 AREA-B REDEFINES AREA-A PIC X(2).","MOVE AREA-A TO AREA-B.").effects().moves().iterator().next();
        assertEquals(MoveKind.MUST_UNKNOWN,overlap.kind());assertTrue(overlap.reasons().contains(Reason.OVERLAPPING_COPY));
    }
    @Test void multipleReceiversAreNotDiscardedByTheSourceProof() {
        var f=fixture("01 SRC PIC X(2).\n01 DST-A PIC X(4).\n01 DST-B PIC X(1).","MOVE SRC TO DST-A DST-B.");
        var primary=f.effects().moves().iterator().next();var sequence=f.effects().sequence(primary.statement());
        assertEquals(2,sequence.size());assertEquals(List.of(4,1),sequence.stream().map(e->e.destination().orElseThrow().view().extent().value().orElseThrow().intValue()).toList());
    }
    @Test void receiverWhichChangesSourcePoisonsEveryResultAndCountHasNoCap() {
        var f=fixture("01 SRC PIC X(2).\n01 DST REDEFINES SRC PIC X(2).\n01 OUT-TEXT PIC X(4).","MOVE SRC TO DST OUT-TEXT.");
        var primary=f.effects().moves().iterator().next();var sequence=f.effects().sequence(primary.statement());
        assertEquals(2,sequence.size());assertTrue(sequence.stream().allMatch(e->e.kind()==MoveKind.MUST_UNKNOWN));
        var many=fixture("01 DST PIC X(2).","MOVE 'AB' TO "+String.join("\n",Collections.nCopies(257,"DST"))+".");
        assertEquals(257,many.effects().sequence(many.effects().moves().iterator().next().statement()).size());
    }
}
