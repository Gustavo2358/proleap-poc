package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
/** N-LR 2026-04-28 OPEN/CLOSE/READ: nominal site facts only, no outcome effect claim. */
class FileOperationSliceTest {
    @Test void staticOpenReadClosePreserveStatementAndFileIds() throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT F ASSIGN TO CLIENTDD.","FD F.\n01 R PIC X(8).","","OPEN INPUT F.\nREAD F.\nCLOSE F.\nGOBACK.");
        assertEquals("2.39.0",p.path("contractVersion").asText());
        var operations=p.path("fileInventory").path("operations");
        assertEquals("PARTIAL",operations.path("availability").asText());
        var uses=operations.path("uses");assertEquals(3,uses.size());
        String[] commands={"OPEN","READ","CLOSE"};
        for(int i=0;i<3;i++){
            var use=uses.get(i);assertEquals(commands[i],use.path("command").asText());
            assertEquals(p.path("statements").get(i).path("header").path("id"),use.path("statement"));
            assertEquals("RESOLVED",use.path("bindingStatus").asText());
            assertEquals(p.path("fileInventory").path("declarations").get(0).path("id"),use.path("candidates").get(0).path("id"));
            assertEquals(p.path("unit"),use.path("candidates").get(0).path("owner"));
            assertTrue(use.path("gapCodes").toString().contains("FILE_EFFECTS_CONTROL_PARTIAL"));
        }
        assertEquals("INPUT",uses.get(0).path("mode").asText());
        Files.createDirectories(Path.of("target/fd-w1"));Files.writeString(Path.of("target/fd-w1/static.json"),p.toPrettyString());
    }
    @Test void multiFileOpenKeepsModesAndDoesNotCreateRecordReads() throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT A ASSIGN TO INA.\nSELECT B ASSIGN TO OUTB.","FD A.\n01 RA PIC X.\nFD B.\n01 RB PIC X.","","OPEN INPUT A OUTPUT B.\nCLOSE A B.\nGOBACK.");
        var uses=p.path("fileInventory").path("operations").path("uses");assertEquals(4,uses.size());
        assertEquals("INPUT",uses.get(0).path("mode").asText());assertEquals("OUTPUT",uses.get(1).path("mode").asText());
        assertEquals(0,uses.get(0).path("ordinal").asInt());assertEquals(1,uses.get(1).path("ordinal").asInt());
        assertEquals(uses.get(0).path("statement"),uses.get(1).path("statement"));
        for(var use:uses)assertNotEquals("READ",use.path("command").asText());
    }
    @Test void missingFileKeepsUnresolvedSiteAndDIsNotPromoted() throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT F ASSIGN TO DYNAMIC X.","FD F.\n01 R PIC X.","WORKING-STORAGE SECTION.\n01 X PIC X(8).","OPEN INPUT F.\nREAD MISSING-FILE.\nGOBACK.");
        var uses=p.path("fileInventory").path("operations").path("uses");assertEquals(2,uses.size());
        assertEquals("UNRESOLVED",uses.get(1).path("bindingStatus").asText());assertTrue(uses.get(1).path("candidates").isEmpty());
        assertTrue(p.path("fileInventory").path("declarations").get(0).path("assignment").path("externalFileName").isNull());
    }
    @Test void nativeReadAndHandlerCallCoexistWithoutEffectClosure() throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT F ASSIGN TO CLIENTDD.","FD F.\n01 R PIC X.","","READ F AT END CALL 'ATEND' END-READ.\nGOBACK.");
        var uses=p.path("fileInventory").path("operations").path("uses");assertEquals(1,uses.size());
        assertEquals("READ",uses.get(0).path("command").asText());
        assertTrue(p.path("statements").toString().contains("ATEND"));
        assertTrue(uses.get(0).path("gapCodes").toString().contains("FILE_EFFECTS_CONTROL_PARTIAL"));
    }
    @Test void dialectReadOptionsCannotBecomeCore() throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT F ASSIGN TO CLIENTDD.","FD F.\n01 R PIC X.","","READ F WITH KEPT LOCK.\nGOBACK.");
        var use=p.path("fileInventory").path("operations").path("uses").get(0);
        assertEquals("UNSUPPORTED",use.path("profile").asText());
        assertTrue(use.path("gapCodes").toString().contains("FILE_SYNTAX_OUTSIDE_N_LR"));
    }
    @Test void declarationOnlyHasNoUses() throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT F ASSIGN TO CLIENTDD.","FD F.\n01 R PIC X.","");
        assertTrue(p.path("fileInventory").path("operations").path("uses").isArray());
        assertEquals(0,p.path("fileInventory").path("operations").path("uses").size());
    }
}
