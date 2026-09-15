package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

/** RF-W4: a mapped missing COPY in a separate, delimited program cannot erase A. */
class ScopedInputTest {
    static String unit(String name,String data,String code) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. "+name+".\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n"+data+"\nPROCEDURE DIVISION.\n"+code+"\nGOBACK.\nEND PROGRAM "+name+".\n";
    }
    static CobolSemanticPort product(AstBoundaryTestSupport.Analysis a,int index) {
        return ExplorerMain.publishSemanticProduct(a.model().programUnits().get(index).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,StorageInitialSemantics.EntryMode.UNKNOWN);
    }
    @Test void completeUnitSurvivesAnotherUnitsDataOrProcedureGap() {
        for(boolean procedure:new boolean[]{false,true}) {
            var a=AstBoundaryTestSupport.analyze(unit("UNIT-A","77 PGM PIC X(8) VALUE 'PROGA'.","CALL PGM.")+
                    unit("UNIT-B",procedure?"01 OTHER-FIELD PIC X.":"01 OTHER-FIELD PIC X.\nCOPY SECRET.",procedure?"COPY SECRET.\nCALL 'PROGB'.":"CALL 'PROGB'."),"scoped.cbl");
            assertEquals(1,a.report().frontendState().unresolvedCopies());
            var good=product(a,0);var incomplete=product(a,1);
            assertEquals(Availability.KNOWN,good.entries().get(0).start().availability());
            assertEquals(InitialStorageKind.LITERAL_BYTES,good.storage().entryState().conditions().get(0).kind());
            assertFalse(good.gaps().stream().anyMatch(g->g.code().equals("UNRESOLVED_COPY")));
            assertEquals(InventoryStatus.INPUT_MISSING,incomplete.entryInventory().status());
            assertFalse(incomplete.calls().isEmpty(),"observed literal site must survive even when entry is unavailable");
            if(procedure)assertEquals(Availability.INPUT_MISSING,incomplete.entries().get(0).start().availability());
        }
    }
    @Test void nestedGapAffectsItsContainingUnitAndNeverBecomesEmptyInput() {
        String parent="IDENTIFICATION DIVISION.\nPROGRAM-ID. PARENT-PGM.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n77 PGM PIC X(8) VALUE 'PROGA'.\nPROCEDURE DIVISION.\nCALL PGM.\nGOBACK.\n";
        var a=AstBoundaryTestSupport.analyze(parent+unit("CHILD-PGM","01 ARG PIC X.\nCOPY SECRET.","CALL 'PROGB'.")+"END PROGRAM PARENT-PGM.\n","nested.cbl");
        assertEquals(2,a.model().programUnits().size());
        assertFalse(a.report().inputComplete(a.model().programUnits().get(0).id()));
        assertFalse(a.report().inputComplete(a.model().programUnits().get(1).id()));
        assertEquals(InventoryStatus.INPUT_MISSING,product(a,0).entryInventory().status());
    }
    @Test void gapBeforeOrAfterObservedCallDoesNotProveEntry() {
        for(String code:new String[]{"COPY SECRET.\nCALL 'PROGA'.","CALL 'PROGA'.\nCOPY SECRET."}) {
            var a=AstBoundaryTestSupport.analyze(unit("OBSERVED-PGM","01 ARG PIC X.",code),"observed.cbl");
            var p=product(a,0);assertEquals(1,p.calls().size());
            assertEquals(Availability.INPUT_MISSING,p.entries().get(0).start().availability());
        }
    }
    @Test void unownedGapCannotProveUnitIsolation() {
        var a=AstBoundaryTestSupport.analyze("COPY SECRET.\n"+unit("UNIT-A","77 PGM PIC X(8) VALUE 'PROGA'.","CALL PGM."),"unowned.cbl");
        assertEquals(InventoryStatus.INPUT_MISSING,product(a,0).entryInventory().status());
        assertNotEquals(InitialStorageKind.LITERAL_BYTES,product(a,0).storage().entryState().conditions().get(0).kind());
    }
}
