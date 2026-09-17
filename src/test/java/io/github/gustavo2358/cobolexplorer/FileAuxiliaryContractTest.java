package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** LR SC27-8713-03 (2026-04-28), pp146–158, 181–192; oracle predates implementation. */
class FileAuxiliaryContractTest {
    @Test void sortCheckpointNeedsNoSelectAndNoEvery() throws Exception {
        var j=publish("SELECT S ASSIGN TO SORTWK.\nI-O-CONTROL. RERUN ON LABEL-S-CHKPT.",
            "SD S.\n01 SR PIC X.","", "GOBACK.");
        var c=clause(j,"RERUN");
        assertEquals("SORT_MERGE",c.path("trigger").asText());
        assertEquals("CHECKPOINT",c.path("effect").asText());
        assertEquals("CHKPT",c.path("checkpoint").path("externalFileName").asText());
        assertEquals(0,c.path("fileReferences").size());
        assertEquals(1,j.path("fileInventory").path("declarations").size());
        assertEquals(1,j.path("statements").size());
        assertTrue(c.path("provenance").path("original").path("startLine").asInt()>0);
        save(j,"rerun-sort-declaration");
    }
    @Test void intervalAndEndVolumeKeepTriggerFile() throws Exception {
        for(var entry:new String[][]{{"EVERY 100 RECORDS OF F","RECORD_COUNT"},{"EVERY END OF REEL OF F","END_VOLUME"}}) {
            var j=publish("SELECT F ASSIGN TO INPUTDD.\nI-O-CONTROL. RERUN CHKPT "+entry[0]+".",
                "FD F.\n01 R PIC X.","","OPEN INPUT F. READ F. CLOSE F. GOBACK.");
            var c=clause(j,"RERUN");assertEquals(entry[1],c.path("trigger").asText());
            assertEquals("RESOLVED",c.path("fileReferences").get(0).path("status").asText());
            assertEquals("CHKPT",c.path("checkpoint").path("externalFileName").asText());
            save(j,"rerun-"+entry[1].toLowerCase(java.util.Locale.ROOT));
        }
    }
    @Test void documentaryClausesDoNotRequireFakeDataOrAddIo() throws Exception {
        var j=publish("SELECT F ASSIGN TO INPUTDD PADDING CHARACTER IS 'X'\n RECORD DELIMITER STANDARD-1.\n"
            +"SELECT G ASSIGN TO OTHERDD.\nI-O-CONTROL. MULTIPLE FILE TAPE F POSITION 1 G POSITION 2.",
            "FD F LABEL RECORDS STANDARD\n VALUE OF FILE-ID IS 'LABEL'\n DATA RECORDS ARE NOT-AN-ACTUAL-RECORD.\n01 R PIC X.\nFD G.\n01 GREC PIC X.","","GOBACK.");
        for(var kind:new String[]{"PADDING","RECORD_DELIMITER","LABEL_RECORDS","VALUE_OF","DATA_RECORDS","MULTIPLE_FILE"}) {
            var c=clause(j,kind);assertEquals("DOCUMENTARY",c.path("effect").asText(),kind);
            assertTrue(c.path("gapCodes").isEmpty(),kind+" is not missing execution semantics");
        }
        assertEquals(1,j.path("statements").size());save(j,"documentary");
    }
    @Test void allocationLayoutAndPageControlAreNotDocumentary() throws Exception {
        var j=publish("SELECT F ASSIGN TO INPUTDD RESERVE 3 AREAS.\nI-O-CONTROL. APPLY WRITE-ONLY ON F.",
            "FD F BLOCK CONTAINS 0 RECORDS\n RECORD VARYING FROM 1 TO 8 DEPENDING ON N\n"
            +" RECORDING MODE V LINAGE IS PAGE-LINES\n WITH FOOTING AT 50 LINES AT TOP 2\n LINES AT BOTTOM 3 CODE-SET IS ALPHA.\n01 R PIC X(8).",
            "WORKING-STORAGE SECTION.\n01 N PIC 9.\n01 PAGE-LINES PIC 99 VALUE 60.\n01 KEEP-NAME PIC X(8) VALUE 'SAFE0001'.",
            "OPEN OUTPUT F. WRITE R AFTER ADVANCING PAGE. CALL KEEP-NAME. GOBACK.");
        for(var kind:new String[]{"RESERVE","APPLY_WRITE_ONLY","BLOCK","RECORDING_MODE","RECORD","LINAGE","CODE_SET"})assertFalse(clause(j,kind).path("effect").asText().equals("DOCUMENTARY"),kind);
        assertEquals("PAGE_CONTROL",clause(j,"LINAGE").path("effect").asText());
        assertEquals("RESOLVED",clause(j,"LINAGE").path("dataReferences").get(0).path("binding").path("status").asText());
        save(j,"effective");
    }
    @Test void sdPageAndCodeSetAreDocumentary() throws Exception {
        var j=publish("SELECT S ASSIGN TO SORTWK.","SD S LINAGE 60 CODE-SET ALPHA.\n01 R PIC X.","","GOBACK.");
        assertEquals("DOCUMENTARY",clause(j,"LINAGE").path("effect").asText());
        assertEquals("DOCUMENTARY",clause(j,"CODE_SET").path("effect").asText());save(j,"sd-documentary");
    }
    @Test void sameVariantsRemainDistinct() throws Exception {
        for(var kind:new String[]{"SAME AREA", "SAME RECORD AREA", "SAME SORT AREA", "SAME SORT-MERGE AREA"}) {
            var sort=kind.contains("SORT");
            var j=publish("SELECT F ASSIGN TO FIRSTDD.\nSELECT G ASSIGN TO SECONDDD.\nI-O-CONTROL. "+kind+" FOR F G.",
                "FD F.\n01 R PIC X.\n"+(sort?"SD":"FD")+" G.\n01 GR PIC X.","","GOBACK.");
            var c=clause(j,kind.replace(' ','_').replace('-','_'));
            assertEquals(sort?"DOCUMENTARY":kind.contains("RECORD")?"RECORD_ALIAS":"DOCUMENTARY",c.path("effect").asText());
            assertEquals(2,c.path("fileReferences").size());save(j,kind.replace(' ','-').toLowerCase(java.util.Locale.ROOT));
        }
    }
    @Test void dialectAndInvalidRerunStayLocalized() throws Exception {
        var j=publish("SELECT F ASSIGN TO INPUTDD.\nI-O-CONTROL. RERUN ON CHKPT EVERY 3 CLOCK-UNITS\n COMMITMENT CONTROL FOR F.","FD F.\n01 R PIC X.","","GOBACK.");
        assertEquals("OUTSIDE_N_LR",clause(j,"RERUN").path("effect").asText());
        assertEquals("OUTSIDE_N_LR",clause(j,"COMMITMENT_CONTROL").path("effect").asText());
        assertFalse(clause(j,"RERUN").path("gapCodes").isEmpty());save(j,"outside-profile");
    }

    @Test void passwordIsAReadDependencyWithNominalIdentity()throws Exception {
        var j=publish("SELECT F ASSIGN TO INPUTDD ORGANIZATION INDEXED\n RECORD KEY K PASSWORD SECRET.","FD F.\n01 R.\n 02 K PIC X.","WORKING-STORAGE SECTION.\n01 SECRET PIC X(8).","OPEN INPUT F. GOBACK.");
        var c=clause(j,"PASSWORD");assertEquals("ACCESS_CHECK",c.path("effect").asText());
        assertEquals("RESOLVED",c.path("dataReferences").get(0).path("binding").path("status").asText());
        var secret=c.path("dataReferences").get(0).path("binding").path("selected");
        assertTrue(j.path("fileInventory").path("operations").path("uses").get(0).path("effects").path("ioReads").toString().contains(secret.asText()),"OPEN reads PASSWORD data");save(j,"password");
    }
    @Test void checkpointCollisionAndMissingTriggerAreExplicit()throws Exception {
        var collision=publish("SELECT S ASSIGN TO CHKPT.\nI-O-CONTROL. RERUN ON CHKPT.","SD S.\n01 R PIC X.","","GOBACK.");
        assertTrue(clause(collision,"RERUN").path("gapCodes").toString().contains("CHECKPOINT_ASSIGNMENT_COLLISION"));
        var missing=publish("SELECT F ASSIGN TO INPUTDD.\nI-O-CONTROL. RERUN ON CHKPT EVERY 100 RECORDS.","FD F.\n01 R PIC X.","","GOBACK.");
        assertTrue(clause(missing,"RERUN").path("gapCodes").toString().contains("RERUN_TRIGGER_FILE_MISSING"));
    }
    @Test void linageCounterCannotPreserveAnOldExactMoveValue()throws Exception {
        var j=publish("SELECT F ASSIGN TO OUTPUTDD.","FD F LINAGE 60.\n01 R PIC X(8).","WORKING-STORAGE SECTION.\n01 TARGET-NAME PIC X(8) VALUE 'STALE001'.", "OPEN OUTPUT F.\nWRITE R AFTER ADVANCING 1 LINE.\nMOVE LINAGE-COUNTER OF F TO TARGET-NAME.\nCALL TARGET-NAME.\nGOBACK.");
        assertEquals("PAGE_CONTROL",clause(j,"LINAGE").path("effect").asText());save(j,"linage-counter");
    }
    @Test void checkpointHasAnOperationalSortTrigger()throws Exception {
        var j=publish("SELECT S ASSIGN TO SORTWK.\nSELECT A ASSIGN TO INPUTDD.\nSELECT B ASSIGN TO OUTPUTDD.\nI-O-CONTROL. RERUN ON CHKPT.","SD S.\n01 SR.\n 02 SK PIC X(8).\nFD A.\n01 AR PIC X(8).\nFD B.\n01 BR PIC X(8).","WORKING-STORAGE SECTION.\n01 SAFE-NAME PIC X(8) VALUE 'SAFE0001'.","SORT S ON ASCENDING KEY SK USING A GIVING B.\nCALL SAFE-NAME.\nGOBACK.");
        assertEquals("SORT_MERGE",clause(j,"RERUN").path("trigger").asText());save(j,"rerun-sort-use");
    }
    @Test void copyClauseRetainsIncludeProvenance()throws Exception {
        var j=publish("SELECT F ASSIGN TO OUTPUTDD.","COPY FDW6 REPLACING FILE-TEMPLATE BY F\n RECORD-TEMPLATE BY R.","","GOBACK.");
        var p=clause(j,"LINAGE").path("provenance");assertTrue(p.path("original").path("file").asText().endsWith("FDW6.cpy"));assertEquals(1,p.path("includeChain").size());save(j,"copy");
    }
    @Test void endVolumeAmbiguousTargetFormDoesNotInventAnAssignmentName()throws Exception {
        var j=publish("SELECT F ASSIGN TO INPUTDD.\nI-O-CONTROL. RERUN ON F EVERY END OF REEL OF F.","FD F.\n01 R PIC X.","","GOBACK.");
        var c=clause(j,"RERUN");assertTrue(c.path("checkpoint").path("externalFileName").isNull());assertTrue(c.path("gapCodes").toString().contains("RERUN_END_VOLUME_TARGET_FORM_NOT_PROVEN"));save(j,"rerun-ambiguous");
    }
    @Test void ibmLineSequentialIsCoreAndKeepsAssignmentName()throws Exception {
        var j=publish("SELECT F ASSIGN TO TEXTDD ORGANIZATION LINE SEQUENTIAL.","FD F.\n01 R PIC X(8).","","OPEN INPUT F. READ F. CLOSE F. GOBACK.");
        assertEquals("LINE_SEQUENTIAL",j.path("fileInventory").path("declarations").get(0).path("organization").asText());
        assertEquals("TEXTDD",j.path("fileInventory").path("declarations").get(0).path("assignment").path("externalFileName").asText());save(j,"line-sequential");
    }
    @Test void sameAreaSourceAccessMethodSeparatesVsamFromQsam()throws Exception {
        for(var prefix:new String[]{"AS-","S-"}){
            var j=publish("SELECT F ASSIGN TO "+prefix+"FIRSTDD.\nSELECT G ASSIGN TO "+prefix+"SECONDDD.\nI-O-CONTROL. SAME AREA FOR F G.","FD F.\n01 FR PIC X(8).\nFD G.\n01 GR PIC X(8).","","GOBACK.");
            var c=clause(j,"SAME_AREA");assertEquals(prefix.equals("AS-")?"RECORD_ALIAS":"DOCUMENTARY",c.path("effect").asText());
            for(var f:c.path("fileReferences"))assertEquals(prefix.equals("AS-")?"VSAM":"QSAM",f.path("accessMethod").asText());
            save(j,prefix.equals("AS-")?"same-vsam-sequential":"same-qsam");
        }
    }
    static JsonNode clause(JsonNode j,String kind) {
        for(var c:j.path("fileInventory").path("auxiliary").path("clauses"))if(c.path("kind").asText().equals(kind))return c;
        fail("missing auxiliary clause "+kind);return null;
    }
    static JsonNode publish(String select,String files,String data,String body)throws Exception{
        var a=AstBoundaryTestSupport.analyze("IDENTIFICATION DIVISION.\nPROGRAM-ID. AUXIO.\nENVIRONMENT DIVISION.\n"+(files.contains("CODE-SET")?"CONFIGURATION SECTION.\nSPECIAL-NAMES. ALPHABET ALPHA IS EBCDIC.\n":"")+"INPUT-OUTPUT SECTION.\nFILE-CONTROL.\n"+select+"\nDATA DIVISION.\nFILE SECTION.\n"+files+"\n"+data+"\nPROCEDURE DIVISION.\n"+body+"\nEND PROGRAM AUXIO.\n","auxiliary.cbl");
        var port=ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(port));
    }
    static void save(JsonNode j,String name)throws Exception{Files.createDirectories(Path.of("target/fd-w6"));Files.writeString(Path.of("target/fd-w6/"+name+".json"),j.toPrettyString());}
}
