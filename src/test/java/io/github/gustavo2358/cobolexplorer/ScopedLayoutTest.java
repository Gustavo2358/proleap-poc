package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.DeclarativeValueInferenceTest.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutTest.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** RF-W4: unknown internal relation has a record bound; unknown root relation does not. */
class ScopedLayoutTest {
    @Test void unprovedNestedRedefinesDoesNotInvalidateIndependentRoot() {
        var data=VALUE+"01 PARAM-AREA.\n05 PART-A PIC X(8).\n05 ALIAS-A REDEFINES MISSING-A PIC X(8).\n";
        var f=fixture(data);known(8,f.view("LIT-PGM").extent());
        assertTrue(f.view("ALIAS-A").offset().value().isEmpty());assertTrue(f.view("PARAM-AREA").extent().value().isEmpty());
        invariant(data,"CALL LIT-PGM.");
    }
    @Test void unprovedRenamesRetainsRecordBoundWithoutInventingEndpoints() {
        var data=VALUE+"01 PARAM-AREA.\n05 PART-A PIC X(8).\n66 NAMED-AREA RENAMES MISSING-A.\n";
        var f=fixture(data);assertFalse(f.layout().renames().get(0).proved());
        assertEquals(f.view("PARAM-AREA").base(),f.view("NAMED-AREA").base());
        assertTrue(f.view("NAMED-AREA").offset().value().isEmpty());
        invariant(data,"CALL LIT-PGM.");
    }
    @Test void rootAliasAndOwnUnknownLayoutStillPreventMaterialization() {
        var root=product(VALUE+"01 ALIAS-A REDEFINES MISSING-A PIC X(8).\n","CALL LIT-PGM.");
        assertEquals("UNKNOWN",condition(root).kind().name());
        var own=product("01 MAIN-AREA.\n05 LIT-PGM PIC X(8) VALUE 'PROGA'.\n05 ALIAS-A REDEFINES MISSING-A PIC X(8).\n","CALL LIT-PGM.");
        assertEquals("UNKNOWN",condition(own).kind().name());
    }
    @Test void unknownNumericExtentNeverBecomesZeroOrContaminatesIndependentRoot() {
        invariant(VALUE+"01 PARAM-AREA.\n05 NUMBER-A PIC 9(8) COMP.\n05 TAIL-A PIC X(8).\n","CALL LIT-PGM.");
        var f=fixture("01 PARAM-AREA.\n05 NUMBER-A PIC 9(8) COMP.\n05 TAIL-A PIC X(8).\n");
        assertTrue(f.view("TAIL-A").offset().value().isEmpty());
    }
}
