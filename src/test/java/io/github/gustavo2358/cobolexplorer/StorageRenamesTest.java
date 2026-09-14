package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutTest.*;

class StorageRenamesTest {
    private static final String DATA = "01 RECORD-AREA.\n05 FIRST-PART PIC X(2).\n05 FILLER PIC X.\n05 LAST-PART PIC X(3).\n";
    @Test void rangeIncludesIntermediateFillerWithoutAllocatingAnotherBase() {
        var f=fixture(DATA+"66 RANGE-ALIAS RENAMES FIRST-PART THRU LAST-PART.");
        assertEquals(1,f.layout().bases().size());known(6,f.view("RECORD-AREA").extent());
        known(0,f.view("RANGE-ALIAS").offset());known(6,f.view("RANGE-ALIAS").extent());
        assertEquals(f.view("RECORD-AREA").base(),f.view("RANGE-ALIAS").base());
        assertTrue(f.view("RANGE-ALIAS").textual());
    }
    @Test void singleItemAndPartialRangePreserveExactPhysicalIdentity() {
        var f=fixture(DATA+"66 SHORT-ALIAS RENAMES LAST-PART.\n66 SECOND-ALIAS RENAMES LAST-PART.");
        known(3,f.view("SHORT-ALIAS").offset());known(3,f.view("SHORT-ALIAS").extent());
        assertEquals(f.view("LAST-PART").base(),f.view("SHORT-ALIAS").base());
        assertEquals(f.view("SHORT-ALIAS").offset(),f.view("SECOND-ALIAS").offset());
        assertEquals(1,f.layout().bases().size());known(6,f.layout().bases().get(0).extent());
    }
    @Test void invalidEndpointsAndUnknownIntermediateNeverAdmitRange() {
        for(var clause:List.of("LAST-PART THRU FIRST-PART","MISSING-PART THRU LAST-PART","RECORD-AREA","FIRST-PART THRU FIRST-PART")) {
            var f=fixture(DATA+"66 RANGE-ALIAS RENAMES "+clause+".");
            assertFalse(f.view("RANGE-ALIAS").textual(),clause);
            assertTrue(f.view("RANGE-ALIAS").extent().value().isEmpty(),clause);
            assertEquals(1,f.layout().bases().size());
        }
        var f=fixture(DATA.replace("05 FILLER PIC X.","05 OPEN-PART PIC X OCCURS 2 TIMES.")+"66 RANGE-ALIAS RENAMES FIRST-PART THRU LAST-PART.");
        assertFalse(f.view("RANGE-ALIAS").textual());assertTrue(f.view("RANGE-ALIAS").offset().value().isEmpty());
    }
    @Test void otherRecordAndNestedEndAreRejectedDespiteNominalBinding() {
        var f=fixture(DATA+"01 OTHER-AREA.\n05 OTHER-PART PIC X.\n66 RANGE-ALIAS RENAMES FIRST-PART THRU OTHER-PART.");
        assertFalse(f.view("RANGE-ALIAS").textual());
        var nested=fixture("01 RECORD-AREA.\n05 GROUP-PART.\n10 CHILD-PART PIC X(3).\n66 RANGE-ALIAS RENAMES GROUP-PART THRU CHILD-PART.");
        assertFalse(nested.view("RANGE-ALIAS").textual());
    }
    @Test void redefinesCompositionAndReadWriteUseExistingRanges() {
        var f=fixture("01 RECORD-AREA.\n05 RAW-PART PIC X(4).\n05 VIEW-PART REDEFINES RAW-PART PIC X(4).\n66 RANGE-ALIAS RENAMES VIEW-PART.");
        assertEquals(f.view("RAW-PART").base(),f.view("RANGE-ALIAS").base());known(4,f.view("RANGE-ALIAS").extent());
        var accesses=StorageAccessTest.fixture(DATA+"66 RANGE-ALIAS RENAMES LAST-PART.","MOVE 'ABC' TO RANGE-ALIAS.\nCALL RANGE-ALIAS.").effects();
        assertEquals(StorageAccessSemantics.MoveKind.LITERAL_BYTES,accesses.moves().iterator().next().kind());
        assertEquals(1,accesses.accesses().stream().filter(a->a.role()==StorageAccessSemantics.Role.CALL_TARGET).count());
    }
}
