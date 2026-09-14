package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageProductTest.state;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
class StorageProjectionFallbackTest {
    @Test void nonReferenceMoveSourcesKeepObservedFallbackAndFollowingCall() {
        for(String source:java.util.List.of("FUNCTION CURRENT-DATE","SPACES","ZERO")) {
            var p=state("01 TARGET-TEXT PIC X(21).","MOVE "+source+" TO TARGET-TEXT.\nCALL 'LITERAL1'.");
            assertEquals(3,p.statements().size(),"MOVE, CALL and helper-appended GOBACK: "+source);
            assertFalse(p.gaps().isEmpty(),source);
            assertEquals(1,p.statements().stream().filter(CallFact.class::isInstance).count(),source);
        }
    }
    @Test void fileRecordValuesCannotReferenceAbsentPhysicalNodes() {
        String source="IDENTIFICATION DIVISION.\nPROGRAM-ID. FILE-VALUE.\nENVIRONMENT DIVISION.\nINPUT-OUTPUT SECTION.\nFILE-CONTROL.\nSELECT INPUT-FILE ASSIGN TO 'input'.\nDATA DIVISION.\nFILE SECTION.\nFD INPUT-FILE.\n01 INPUT-RECORD.\n05 INPUT-TEXT PIC X(4) VALUE 'ABCD'.\nWORKING-STORAGE SECTION.\n01 WORK-TEXT PIC X(4) VALUE 'WORK'.\nPROCEDURE DIVISION.\nCALL 'LITERAL1'.\nGOBACK.\n";
        var p=StorageInitialProductTest.initialSource(source,StorageInitialSemantics.EntryMode.INITIAL);
        assertEquals(1,p.storage().entryState().conditions().size(),"only physical working-storage condition");
        assertEquals(InitialStorageKind.LITERAL_BYTES,p.storage().entryState().conditions().get(0).kind());
        assertEquals(1,p.calls().size());
    }
}
