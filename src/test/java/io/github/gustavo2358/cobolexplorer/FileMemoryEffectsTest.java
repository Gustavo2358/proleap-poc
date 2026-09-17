package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** IBM SC27-8713-03, update 2026-04-28: READ/INTO, WRITE/FROM and FILE STATUS.
 * Expected separation is established by source allocation, independently of effects output. */
class FileMemoryEffectsTest {
    record Fixture(AstBoundaryTestSupport.Analysis source,StorageAccessSemantics storage) {
        StorageInitialSemantics.Condition initial(String name) {
            var unit=source.model().programUnits().get(0).id();
            var symbol=source.tables().forProgramUnit(unit).orElseThrow().symbolTable().symbols().stream()
                .filter(s->s.canonicalName().equals(name)).findFirst().orElseThrow();
            return storage.initial().facts(unit).conditions().stream().filter(c->c.declaration().node()==symbol.declarationAstNodeId()).findFirst().orElseThrow();
        }
    }
    static Fixture fixture(String select,String records,String working,String code) {
        var a=AstBoundaryTestSupport.analyze("IDENTIFICATION DIVISION.\nPROGRAM-ID. MEMIO.\nENVIRONMENT DIVISION.\nINPUT-OUTPUT SECTION.\nFILE-CONTROL.\n"+select+
            "\nDATA DIVISION.\nFILE SECTION.\n"+records+"\nWORKING-STORAGE SECTION.\n"+working+"\nPROCEDURE DIVISION.\n"+code+"\nGOBACK.\n","memory-io.cbl");
        var layout=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        return new Fixture(a,StorageAccessSemantics.analyze(a.build(),a.resolution(),layout));
    }
    private static final String SAFE="01 SAFE-PGM PIC X(8) VALUE 'SAFE'.";
    @Test void everyNativeCommandPreservesProvablyDisjointSourceEvidence() {
        for(var command:List.of("READ F.","OPEN INPUT F.","CLOSE F.","WRITE REC.","REWRITE REC.","DELETE F RECORD.","START F KEY IS EQUAL TO REC.")) {
            var f=fixture("SELECT F ASSIGN TO INDD.","FD F.\n01 REC PIC X(8).",SAFE,command+"\nCALL SAFE-PGM.");
            assertEquals(StorageInitialSemantics.Kind.LITERAL_BYTES,f.initial("SAFE-PGM").kind(),command);
        }
    }
    @Test void intoAndStatusAreWritesButDoNotEraseUnrelatedSourceEvidence() {
        var f=fixture("SELECT F ASSIGN TO INDD FILE STATUS IO-STATUS.","FD F.\n01 REC PIC X(8).",
            SAFE+"\n01 DEST PIC X(8) VALUE 'OLD'.\n01 IO-STATUS PIC XX VALUE 'AA'.","READ F INTO DEST.\nCALL SAFE-PGM.");
        assertEquals(StorageInitialSemantics.Kind.LITERAL_BYTES,f.initial("SAFE-PGM").kind());
        assertEquals(StorageInitialSemantics.Kind.POSSIBLE_LITERAL_BYTES,f.initial("DEST").kind());
        assertEquals(StorageInitialSemantics.Kind.POSSIBLE_LITERAL_BYTES,f.initial("IO-STATUS").kind());
        assertTrue(f.initial("DEST").reasons().contains(StorageInitialSemantics.Reason.OVERLAPPING_WRITE));
    }
    @Test void opaqueRecordAndLengthOutputStillHaveLocalAllocation() {
        var f=fixture("SELECT F ASSIGN TO INDD.","FD F RECORD IS VARYING FROM 2 TO 20 CHARACTERS\n DEPENDING ON REC-LENGTH.\n01 REC.\n 05 COUNT-FIELD PIC 9(4).\n 05 CONTENT-FIELD PIC X(16).",
            SAFE+"\n01 REC-LENGTH PIC 9(4).","READ F.\nCALL SAFE-PGM.");
        assertEquals(StorageInitialSemantics.Kind.LITERAL_BYTES,f.initial("SAFE-PGM").kind());
    }
    @Test void unresolvedIntoCannotBecomeAnEmptyWriteSet() {
        var f=fixture("SELECT F ASSIGN TO INDD.","FD F.\n01 REC PIC X(8).",SAFE,"READ F INTO MISSING.\nCALL SAFE-PGM.");
        assertNotEquals(StorageInitialSemantics.Kind.LITERAL_BYTES,f.initial("SAFE-PGM").kind());
    }
    @Test void writeFromPreservesTheDisjointSource() {
        var f=fixture("SELECT F ASSIGN TO OUTDD.","FD F.\n01 REC PIC X(8).",SAFE,"WRITE REC FROM SAFE-PGM.\nCALL SAFE-PGM.");
        assertEquals(StorageInitialSemantics.Kind.LITERAL_BYTES,f.initial("SAFE-PGM").kind());
    }
}
