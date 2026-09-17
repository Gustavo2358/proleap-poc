package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.FileEffectsContractTest.publish;
import static io.github.gustavo2358.cobolexplorer.FileControlPlanTest.*;

class FileControlContractTest {
    @Test void useBodyAndPrimaryEntrySurviveTransportWithConditionalDispatch()throws Exception {
        var p=publish(source(FILE_USE,"READ F INTO DEST\n AT END CALL 'EOFPGM' END-READ.\nCALL 'AFTER'."));
        assertEquals("2.25.0",p.path("contractVersion").asText());var inventory=p.path("fileInventory");assertEquals("1.4.0",inventory.path("version").asText());
        assertEquals(1,inventory.path("declaratives").size());var declaration=inventory.path("declaratives").get(0);
        assertFalse(declaration.path("entry").isNull());assertEquals(1,declaration.path("roots").size());assertEquals(1,declaration.path("completions").size());
        var c=inventory.path("operations").path("uses").get(0).path("control");assertEquals("KNOWN",c.path("availability").asText());
        assertTrue(java.util.stream.StreamSupport.stream(p.path("structure").path("branches").spliterator(),false).anyMatch(b->b.path("branch").asText().equals("FILE_HANDLER")),"wire closes handler containment inventory");
        assertFalse(c.path("continuation").isNull());assertEquals(3,c.path("routes").size());
        var events=new HashMap<String,com.fasterxml.jackson.databind.JsonNode>();for(var r:c.path("routes"))events.put(r.path("event").asText(),r);
        assertEquals("HANDLER",events.get("END").path("destinations").get(0).path("kind").asText());
        assertEquals("USE",events.get("OTHER_ERROR").path("destinations").get(0).path("kind").asText());
        assertEquals(declaration.path("id"),events.get("OTHER_ERROR").path("destinations").get(0).path("declarative"));
        assertFalse(p.toString().contains("DECLARATIVES_NOT_PROJECTED"));
        Files.createDirectories(Path.of("target/fd-w4"));Files.writeString(Path.of("target/fd-w4/use-and-handler.json"),p.toPrettyString());
        assertEquals(p,publish(source(FILE_USE,"READ F INTO DEST\n AT END CALL 'EOFPGM' END-READ.\nCALL 'AFTER'.")));
    }
    @Test void modeUncertaintyAndNestedHandlerContinuationsArePublished()throws Exception {
        var p=publish(source("MODE-ERROR SECTION.\nUSE AFTER ERROR PROCEDURE ON INPUT.\nMODE-PARA.\nCALL 'MODEPGM'.",
            "OPEN INPUT F.\nREAD F\n AT END IF FS = '10' CALL 'EOFPGM'\n ELSE CALL 'OTHERPGM' END-IF\n NOT AT END CALL 'NOTEOF' END-READ.\nCALL 'AFTER'."));
        var uses=p.path("fileInventory").path("operations").path("uses");assertEquals(2,uses.size());
        assertEquals("KNOWN",uses.get(0).path("control").path("availability").asText());assertEquals("PARTIAL",uses.get(1).path("control").path("availability").asText());
        assertTrue(uses.get(1).path("control").path("gapCodes").toString().contains("FILE_CURRENT_OPEN_MODE_NOT_PROVEN"));
        Files.createDirectories(Path.of("target/fd-w4"));Files.writeString(Path.of("target/fd-w4/mode-and-nested-handler.json"),p.toPrettyString());
    }
    @Test void nestedIoRecursiveUseAndPeriodDoNotCloneOccurrences()throws Exception {
        var p=publish(source("FILE-ERROR SECTION.\nUSE AFTER ERROR PROCEDURE ON F.\nERROR-PARA.\nREAD F AT END CALL 'INNEREND' END-READ.\nCALL 'USETAIL'.",
            "READ F NOT AT END\n READ F AT END CALL 'NESTEOF' END-READ END-READ.\nCALL 'AFTER'."));
        assertEquals(3,p.path("fileInventory").path("operations").path("uses").size());
        assertEquals(1,p.path("fileInventory").path("declaratives").size());
        assertEquals(1,p.path("fileInventory").path("declaratives").get(0).path("completions").size());
        save("nested-use",p);
        var implicit=publish(source("","READ F AT END CALL 'EOFPGM'.\nCALL 'AFTER'."));
        assertEquals("HANDLER",implicit.path("fileInventory").path("operations").path("uses").get(0).path("control").path("routes").get(1).path("destinations").get(0).path("kind").asText());
        assertTrue(implicit.path("fileInventory").path("declaratives").isEmpty());save("implicit-handler",implicit);
    }
    @Test void invalidKeyStatusEvaluateAndNonlocalExitsKeepTheirControl()throws Exception {
        var f=FileMemoryEffectsTest.fixture("SELECT F ASSIGN TO INDD ORGANIZATION IS INDEXED\n ACCESS IS DYNAMIC RECORD KEY REC FILE STATUS FS.","FD F.\n01 REC PIC X(8).","01 FS PIC XX.",
            "DECLARATIVES.\n"+FILE_USE+"\nEND DECLARATIVES.\nMAIN-SECTION SECTION.\nMAIN-PARA.\nDELETE F RECORD INVALID KEY\n IF FS = '23' CALL 'MISS' ELSE CALL 'OTHER' END-IF\n NOT INVALID KEY CALL 'GOOD' END-DELETE.\nEVALUATE FS WHEN '00' CALL 'ZERO'\n WHEN OTHER CALL 'NONZERO' END-EVALUATE.");
        var p=publish(f);var routes=p.path("fileInventory").path("operations").path("uses").get(0).path("control").path("routes");
        assertEquals("INVALID_KEY",routes.get(1).path("event").asText());assertEquals("HANDLER",routes.get(1).path("destinations").get(0).path("kind").asText());
        save("key-status",p);
        var escape=publish(source("FILE-ERROR SECTION.\nUSE AFTER ERROR PROCEDURE ON F.\nERROR-PARA.\nIF FS = '99' GOBACK ELSE CALL 'USEFILE' END-IF.",
            "READ F AT END GO TO FINISH END-READ.\nCALL 'BETWEEN'.\nFINISH.\nCALL 'AFTER'."));
        assertFalse(escape.path("fileInventory").path("declaratives").get(0).path("completions").isEmpty());save("escape-use",escape);
    }
    private static void save(String name,com.fasterxml.jackson.databind.JsonNode p)throws Exception {
        Files.createDirectories(Path.of("target/fd-w4"));Files.writeString(Path.of("target/fd-w4/"+name+".json"),p.toPrettyString());
    }
}
