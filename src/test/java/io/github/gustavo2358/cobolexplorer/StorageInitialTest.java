package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageAccessTest.fixture;
import static io.github.gustavo2358.cobolexplorer.StorageInitialSemantics.*;
class StorageInitialTest {
    static Facts initial(String data,EntryMode mode) {
        var f=fixture(data,"CONTINUE.");var a=f.source();return StorageInitialSemantics.analyze(a.build(),a.resolution(),f.effects().layout(),mode).facts(a.model().programUnits().get(0).id());
    }
    @Test void wholeAndChildValuesAreInitialConditionsWithoutExecutableMoves() {
        var child=initial("01 WS-AREA.\n05 PREFIX-TEXT PIC X(2).\n05 PGM-TEXT PIC X(8) VALUE 'PGM00001'.",EntryMode.INITIAL);
        assertEquals(1,child.conditions().size());var c=child.conditions().get(0);assertEquals(Kind.LITERAL_BYTES,c.kind());
        assertEquals(List.of(215,199,212,240,240,240,240,241),c.bytes());assertEquals(java.math.BigInteger.TWO,c.view().orElseThrow().offset().value().orElseThrow());
        var whole=initial("01 WS-AREA VALUE 'AB'.\n05 X-TEXT PIC X(4).",EntryMode.INITIAL);
        assertEquals(List.of(193,194,64,64),whole.conditions().get(0).bytes());
        assertTrue(initial("01 UNKNOWN-TEXT PIC X(8).",EntryMode.INITIAL).conditions().isEmpty());
    }
    @Test void preservedAndUnknownEntriesDoNotClaimLiteralInitialContent() {
        for(var mode:List.of(EntryMode.PRESERVED,EntryMode.UNKNOWN)) {
            var f=initial("01 PGM-TEXT PIC X(8) VALUE 'PGM00001'.",mode);assertEquals(mode,f.mode());assertEquals(1,f.conditions().size());
            var c=f.conditions().get(0);assertEquals(mode==EntryMode.PRESERVED?Kind.PRESERVE:Kind.UNKNOWN,c.kind());assertTrue(c.bytes().isEmpty());
        }
    }
    @Test void invalidTooLongAndUnmodeledLiteralsAreNotMoveTruncation() {
        for(var literal:List.of("'ABCDE'","SPACES","X'C1'","'€'")) {
            var c=initial("01 PGM-TEXT PIC X(4) VALUE "+literal+".",EntryMode.INITIAL).conditions().get(0);
            assertEquals(Kind.UNKNOWN,c.kind());assertTrue(c.bytes().isEmpty());assertFalse(c.reasons().isEmpty());
        }
    }
    @Test void nestedValueAndValueOnRedefinitionDoNotPublishContradictorySeeds() {
        var nested=initial("01 WS-AREA VALUE 'ABCD'.\n05 X-TEXT PIC X(4) VALUE 'WXYZ'.",EntryMode.INITIAL);
        assertEquals(2,nested.conditions().size());assertTrue(nested.conditions().stream().allMatch(c->c.kind()==Kind.UNKNOWN&&c.reasons().contains(Reason.NESTED_VALUE)));
        var overlay=initial("01 WS-AREA PIC X(4).\n01 ALT-AREA REDEFINES WS-AREA.\n05 X-TEXT PIC X(4) VALUE 'ABCD'.",EntryMode.INITIAL);
        assertEquals(Kind.UNKNOWN,overlay.conditions().get(0).kind());assertTrue(overlay.conditions().get(0).reasons().contains(Reason.VALUE_ON_REDEFINITION));
    }
    @Test void valueProofUsesTypedLiteralWithEscapesAndUnknownLayoutIsExplicit() {
        assertEquals(List.of(193,125,194),initial("01 TXT PIC X(3) VALUE 'A''B'.",EntryMode.INITIAL).conditions().get(0).bytes());
        var unknown=initial("01 TXT OCCURS 2 PIC X VALUE 'A'.",EntryMode.INITIAL).conditions().get(0);
        assertEquals(Kind.UNKNOWN,unknown.kind());assertTrue(unknown.view().isEmpty());assertTrue(unknown.reasons().contains(Reason.STORAGE_NOT_PROVEN));
    }
}
