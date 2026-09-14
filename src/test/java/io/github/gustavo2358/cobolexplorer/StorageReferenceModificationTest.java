package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutTest.known;

class StorageReferenceModificationTest {
    static final String DATA="01 RECORD-AREA.\n05 PREFIX-PART PIC X(2).\n05 TEXT-PART PIC X(6).\n05 VIEW-PART REDEFINES TEXT-PART PIC X(6).\n66 TEXT-ALIAS RENAMES TEXT-PART.";
    @Test void firstLastAndInteriorUseOneBasedCharactersAndZeroBasedBytes() {
        for(int[] interval:new int[][]{{1,1,2},{6,1,7},{2,3,3},{1,6,2}}) {
            var f=StorageAccessTest.fixture(DATA,"MOVE '"+"A".repeat(interval[1])+"' TO TEXT-PART("+interval[0]+":"+interval[1]+").");
            var move=f.effects().moves().iterator().next();assertEquals(StorageAccessSemantics.MoveKind.LITERAL_BYTES,move.kind());
            known(interval[2],move.destination().orElseThrow().view().offset());known(interval[1],move.destination().get().view().extent());
        }
    }
    @Test void redefinesAndRenamesComposeSlicesAgainstSameExistingBase() {
        for(var name:List.of("TEXT-PART","VIEW-PART","TEXT-ALIAS")) {
            var f=StorageAccessTest.fixture(DATA,"CALL "+name+"(2:3).");
            var a=f.effects().accesses().stream().filter(x->x.role()==StorageAccessSemantics.Role.CALL_TARGET).findFirst().orElseThrow();
            known(3,a.view().offset());known(3,a.view().extent());
            assertEquals(1,f.effects().layout().layout(f.source().model().programUnits().get(0).id()).bases().size());
        }
    }
    @Test void groupSliceHasTextualElementaryInterpretation() {
        var f=StorageAccessTest.fixture(DATA,"CALL RECORD-AREA(3:6).");
        assertEquals(1,f.effects().accesses().stream().filter(a->a.role()==StorageAccessSemantics.Role.CALL_TARGET).count());
    }
    @Test void zeroNegativeOutOfBoundsDynamicAndOmittedLengthNeverDefault() {
        for(var mod:List.of("0:1","1:0","7:1","6:2","-1:1","1:-1","POSITION-VAR:1","1:POSITION-VAR","2:")) {
            var f=StorageAccessTest.fixture(DATA+"\n01 POSITION-VAR PIC 9.","MOVE 'A' TO TEXT-PART("+mod+").");
            assertEquals(StorageAccessSemantics.MoveKind.UNAVAILABLE,f.effects().moves().iterator().next().kind(),mod);
            assertTrue(f.effects().accesses().stream().noneMatch(a->a.role()==StorageAccessSemantics.Role.WRITE),mod);
        }
    }
    @Test void unknownCodecNeverTurnsCharacterPositionIntoByteOffset() {
        var f=StorageAccessTest.fixture(DATA,"CALL TEXT-PART(1:1).",StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertTrue(f.effects().accesses().isEmpty());
    }
}
