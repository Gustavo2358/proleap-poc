package io.github.gustavo2358.cobolexplorer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
class StorageInitialProductTest {
    static CobolSemanticPort initial(StorageInitialSemantics.EntryMode mode) {
        var a=StorageAccessTest.fixture("01 WS-AREA.\n05 PREFIX-TEXT PIC X(2).\n05 PGM-TEXT PIC X(8) VALUE 'PGM00001'.","CALL PGM-TEXT.\nPERFORM SET-PGM.\nCALL PGM-TEXT.\nGOBACK.\nSET-PGM.\nMOVE 'OTHERPGM' TO PGM-TEXT.").source();
        return ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,mode);
    }
    @Test void initialStateIsClosedTypedTransportWithNoSyntheticMove() throws Exception {
        var p=initial(StorageInitialSemantics.EntryMode.INITIAL);assertEquals(1,p.moves().size());
        var c=p.storage().entryState().conditions().get(0);assertEquals(InitialStorageKind.LITERAL_BYTES,c.kind());assertTrue(c.provenance().exact());
        var bytes=SemanticProductJsonWriter.serialize(p);var d=new ObjectMapper().readTree(bytes);
        assertEquals("2.12.0",d.path("contractVersion").asText());assertEquals("1.3.0",d.path("storage").path("version").asText());
        assertEquals("INITIAL",d.path("storage").path("entryState").path("mode").asText());
        assertEquals(8,d.path("storage").path("entryState").path("conditions").get(0).path("bytes").size());
        if(System.getProperty("storage.fixture.output")!=null)java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("storage.fixture.output")),bytes);
    }
    @Test void unknownAndPreservedCannotClaimValueLiteral() {
        for(var mode:List.of(StorageInitialSemantics.EntryMode.UNKNOWN,StorageInitialSemantics.EntryMode.PRESERVED)) {
            var p=initial(mode);assertEquals(mode.name(),p.storage().entryState().mode().name());
            assertEquals(mode==StorageInitialSemantics.EntryMode.UNKNOWN?InitialStorageKind.UNKNOWN:InitialStorageKind.PRESERVE,p.storage().entryState().conditions().get(0).kind());
        }
        assertThrows(IllegalArgumentException.class,()->ExplorerMain.entryStorageState("first-ish"));
    }
    @Test void conditionNameValueIsNotStorageInitialization() {
        var s=StorageProductTest.state("01 SWITCH-TEXT PIC X VALUE 'Y'.\n88 SWITCH-ON VALUE 'Y'.","CALL 'PGM00001'.");
        assertEquals(1,s.storage().entryState().conditions().size());
    }
}
