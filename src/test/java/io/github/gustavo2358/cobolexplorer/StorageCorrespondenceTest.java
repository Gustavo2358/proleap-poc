package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageAccessTest.fixture;
import static io.github.gustavo2358.cobolexplorer.StorageAccessSemantics.*;
class StorageCorrespondenceTest {
    static List<Move> sequence(String data,String code) {
        var f=fixture(data,code);return f.effects().sequence(f.effects().moves().iterator().next().statement());
    }
    @Test void matchesOnlyQualifiedFieldsInSendingOrderAndFitsEachPair() {
        var effects=sequence("01 SRC.\n05 A PIC X(2).\n05 FILLER PIC X.\n05 B PIC X(2).\n05 ONLY-S PIC X.\n01 DST.\n05 B PIC X.\n05 ONLY-D PIC X(3).\n05 A PIC X(4).","MOVE CORR SRC TO DST.");
        assertEquals(2,effects.size());assertEquals(List.of("FIT_TEXT","FIT_TEXT"),effects.stream().map(e->e.kind().name()).toList());
        assertEquals(List.of(0,3),effects.stream().map(e->e.source().orElseThrow().view().offset().value().orElseThrow().intValueExact()).toList());
        assertEquals(List.of(4,0),effects.stream().map(e->e.destination().orElseThrow().view().offset().value().orElseThrow().intValueExact()).toList());
        assertEquals(List.of(4,1),effects.stream().map(e->e.destination().orElseThrow().view().extent().value().orElseThrow().intValueExact()).toList());
    }
    @Test void equalNamesWithDifferentQualifiersDoNotCorrespond() {
        var effects=sequence("01 SRC.\n05 G1.\n10 A PIC X.\n05 G2.\n10 A PIC X(2).\n01 DST.\n05 G2.\n10 A PIC X(3).\n05 G3.\n10 A PIC X.","MOVE CORRESPONDING SRC TO DST.");
        assertEquals(1,effects.size());assertEquals(1,effects.get(0).source().orElseThrow().view().offset().value().orElseThrow().intValueExact());
        assertEquals(3,effects.get(0).destination().orElseThrow().view().extent().value().orElseThrow().intValueExact());
    }
    @Test void redefiningSubtreesAreExcludedButRedefiningRootIsAllowed() {
        var effects=sequence("01 SRC.\n05 A PIC X(2).\n05 G REDEFINES A.\n10 B PIC X(2).\n01 ORIGINAL.\n05 A PIC X(2).\n05 G.\n10 B PIC X(2).\n01 DST REDEFINES ORIGINAL.\n05 A PIC X(2).\n05 G.\n10 B PIC X(2).","MOVE CORR SRC TO DST.");
        assertEquals(1,effects.size());assertEquals(MoveKind.COPY_BYTES,effects.get(0).kind());
    }
    @Test void aliasedGroupsNeverInventSequentialSourceValues() {
        var effects=sequence("01 SRC.\n05 A PIC X(2).\n05 B PIC X(2).\n01 DST REDEFINES SRC.\n05 B PIC X(2).\n05 A PIC X(2).","MOVE CORR SRC TO DST.");
        assertEquals(2,effects.size());assertTrue(effects.stream().allMatch(e->e.kind()==MoveKind.MUST_UNKNOWN));
    }
    @Test void ambiguousQualifiedNamesAndNoPairsDoNotInventWholeCopies() {
        assertTrue(sequence("01 SRC.\n05 A PIC X.\n05 A PIC X.\n01 DST.\n05 A PIC X.","MOVE CORR SRC TO DST.").isEmpty());
        assertTrue(sequence("01 SRC.\n05 A PIC X.\n01 DST.\n05 B PIC X.","MOVE CORR SRC TO DST.").isEmpty());
    }
    @Test void unsupportedPairsAndRootSlicesKeepExplicitFallback() {
        for(String data:List.of("01 SRC.\n05 A PIC 9.\n01 DST.\n05 A PIC X.","01 SRC.\n05 A PIC X.\n01 DST.\n05 A.\n10 B PIC X.","01 SRC.\n05 A OCCURS 2 PIC X.\n01 DST.\n05 A PIC X."))
            assertTrue(sequence(data,"MOVE CORR SRC TO DST.").isEmpty());
        assertTrue(sequence("01 SRC.\n05 A PIC X(2).\n01 DST.\n05 A PIC X(2).","MOVE CORR SRC(1:2) TO DST.").isEmpty());
    }
}
