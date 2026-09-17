package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Independent N-LR W2 oracles. No effects/outcome closure is inferred from verb names. */
class FileNativeOperationTest {
    private static final String SELECT="SELECT F ASSIGN TO CLIENTDD\n ORGANIZATION INDEXED ACCESS DYNAMIC\n RECORD KEY K.\nSELECT G ASSIGN TO OTHERDD.";
    private static final String FILES="FD F.\n01 R.\n 02 K PIC X(8).\nFD G.\n01 OTHER-R PIC X(8).";
    private static JsonNode publish(String body)throws Exception{return FileDeclarationContractTest.publish(SELECT,FILES,"WORKING-STORAGE SECTION.\n01 BUF PIC X(8).",body);}
    private static JsonNode uses(JsonNode p){return p.path("fileInventory").path("operations").path("uses");}
    private static String file(JsonNode p,String name){for(var d:p.path("fileInventory").path("declarations"))if(d.path("logicalFile").asText().equals(name))return d.path("id").asText();throw new AssertionError(name);}
    private static Set<String> values(JsonNode list,String field){var out=new HashSet<String>();for(var x:list)out.add(x.path(field).asText());return out;}
    @Test void sevenNativeVerbsUseTypedCommandAndRecordOwner()throws Exception {
        var p=publish("OPEN I-O F.\nREAD F KEY K.\nWRITE R FROM OTHER-R.\nREWRITE R FROM BUF.\nDELETE F RECORD.\nSTART F KEY GREATER THAN K.\nCLOSE F.\nGOBACK.");
        assertEquals("2.27.0",p.path("contractVersion").asText());assertEquals("1.6.0",p.path("fileInventory").path("version").asText());
        assertEquals(Set.of("OPEN","READ","WRITE","REWRITE","DELETE_RECORD","START","CLOSE"),values(uses(p),"command"));assertEquals(7,uses(p).size());
        for(var use:uses(p)){assertEquals("N_LR",use.path("profile").asText());assertEquals("RESOLVED",use.path("bindingStatus").asText());assertEquals(file(p,"F"),use.path("candidates").get(0).path("id").asText());}
        Files.createDirectories(Path.of("target/fd-w2"));Files.writeString(Path.of("target/fd-w2/native.json"),p.toPrettyString());
    }
    @Test void fromRecordOfAnotherFileDoesNotAddAnotherFileUse()throws Exception {
        var p=publish("WRITE R FROM OTHER-R.\nREWRITE R FROM OTHER-R.\nGOBACK.");assertEquals(2,uses(p).size());
        for(var use:uses(p)){assertEquals(1,use.path("candidates").size());assertEquals(file(p,"F"),use.path("candidates").get(0).path("id").asText());assertEquals(Set.of("RECORD","FROM"),values(use.path("operands"),"role"));}
    }
    @Test void readIntoKeyAndStartRelationHaveIndependentRoles()throws Exception {
        var p=publish("READ F NEXT RECORD INTO BUF.\nREAD F INTO BUF KEY K.\nSTART F KEY NOT LESS THAN K.\nGOBACK.");
        assertTrue(uses(p).get(0).path("options").toString().contains("NEXT"));
        assertEquals(Set.of("INTO"),values(uses(p).get(0).path("operands"),"role"));assertEquals(Set.of("INTO","KEY"),values(uses(p).get(1).path("operands"),"role"));
        assertEquals("GREATER_OR_EQUAL",uses(p).get(2).path("keyRelation").asText());
        for(var use:uses(p))for(var operand:use.path("operands"))assertFalse(operand.path("references").isEmpty(),"canonical DATA operand refs");
    }
    @Test void groupedOpenCloseKeepPerFileOptions()throws Exception {
        var p=publish("OPEN INPUT G REVERSED I-O F.\nCLOSE G WITH NO REWIND F WITH LOCK.\nGOBACK.");assertEquals(4,uses(p).size());
        assertEquals("INPUT",uses(p).get(0).path("mode").asText());assertTrue(uses(p).get(0).path("options").toString().contains("REVERSED"));assertTrue(uses(p).get(1).path("options").isEmpty());
        assertTrue(uses(p).get(2).path("options").toString().contains("NO_REWIND"));assertTrue(uses(p).get(3).path("options").toString().contains("LOCK"));
    }
    @Test void deleteOnlyUsePreservesTwoDistinctHandlerBodies()throws Exception {
        var p=publish("DELETE F RECORD INVALID KEY CALL 'BAD'\n NOT INVALID KEY CALL 'GOOD' END-DELETE.\nGOBACK.");
        assertEquals(1,uses(p).size());var use=uses(p).get(0);assertEquals("DELETE_RECORD",use.path("command").asText());assertTrue(use.path("explicitTerminator").asBoolean());
        assertEquals(Set.of("INVALID_KEY","NOT_INVALID_KEY"),values(use.path("handlers"),"kind"));var ids=new HashSet<String>();
        for(var h:use.path("handlers")){assertEquals(1,h.path("statements").size());assertTrue(ids.add(h.path("statements").get(0).asText()));}
        assertEquals(2,java.util.stream.StreamSupport.stream(p.path("statements").spliterator(),false).filter(s->s.path("variant").asText().equals("CALL")).count());
        Files.createDirectories(Path.of("target/fd-w2"));Files.writeString(Path.of("target/fd-w2/delete-handlers.json"),p.toPrettyString());
    }
    @Test void ambiguousRecordAndNonFileDataDoNotInventOwnership()throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT F ASSIGN TO INA.\nSELECT G ASSIGN TO INB.","FD F.\n01 R PIC X.\nFD G.\n01 R PIC X.","WORKING-STORAGE SECTION.\n01 WS PIC X.","WRITE R.\nWRITE WS.\nGOBACK.");
        assertEquals(2,uses(p).size());assertEquals("AMBIGUOUS",uses(p).get(0).path("bindingStatus").asText());assertEquals(2,uses(p).get(0).path("candidates").size());assertTrue(uses(p).get(1).path("candidates").isEmpty());assertNotEquals("RESOLVED",uses(p).get(1).path("bindingStatus").asText());
    }
    @Test void nonNlrOperandsAndLockFormsRemainUnsupported()throws Exception {
        for(String body:List.of("WRITE R FROM 'X'.","READ F WITH KEPT LOCK.","CLOSE F WITH NO WAIT.")){
            var p=publish(body+"\nGOBACK.");assertEquals(1,uses(p).size());assertEquals("UNSUPPORTED",uses(p).get(0).path("profile").asText());
        }
    }
    @Test void writeAdvancingAndEopAreStructured()throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT F ASSIGN TO PRINTDD.","FD F LINAGE IS 60 LINES.\n01 R PIC X(80).","", "WRITE R AFTER ADVANCING 2 LINES AT EOP CALL 'PAGEEND' END-WRITE.\nGOBACK.");
        var use=uses(p).get(0);assertEquals("WRITE",use.path("command").asText());assertTrue(use.path("options").toString().contains("AFTER_ADVANCING"));assertEquals(Set.of("RECORD","ADVANCING"),values(use.path("operands"),"role"));assertEquals("AT_END_OF_PAGE",use.path("handlers").get(0).path("kind").asText());
    }

    @Test void missingTargetsNeverBecomeAnArbitraryDeclaration()throws Exception {
        for(var body:List.of("OPEN INPUT MISSING-F.","READ MISSING-F.","CLOSE MISSING-F.","START MISSING-F.","DELETE MISSING-F RECORD.","WRITE BUF.","REWRITE BUF.")) {
            var use=uses(publish(body+"\nGOBACK.")).get(0);
            assertNotEquals("RESOLVED",use.path("bindingStatus").asText());assertTrue(use.path("candidates").isEmpty());assertFalse(use.path("gapCodes").isEmpty());
        }
    }
    @Test void qualifiedRecordKeepsOwnerInsteadOfSameSpelling()throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT F ASSIGN TO INA.\nSELECT G ASSIGN TO INB.","FD F.\n01 R PIC X.\nFD G.\n01 R PIC X.","", "WRITE R OF G.\nGOBACK.");
        var use=uses(p).get(0);assertEquals("RESOLVED",use.path("bindingStatus").asText());assertEquals(file(p,"G"),use.path("candidates").get(0).path("id").asText());
    }
    @Test void handlersPreserveNestedControlAndOppositeOutcomes()throws Exception {
        var p=publish("READ F NEXT RECORD INTO BUF\n AT END IF BUF = 'A' CALL 'EOF' END-IF\n NOT AT END MOVE 'B' TO BUF END-READ.\nWRITE R FROM BUF\n INVALID KEY CALL 'BAD'\n NOT INVALID KEY CALL 'OK' END-WRITE.\nGOBACK.");
        assertEquals(Set.of("AT_END","NOT_AT_END"),values(uses(p).get(0).path("handlers"),"kind"));
        assertEquals(Set.of("INVALID_KEY","NOT_INVALID_KEY"),values(uses(p).get(1).path("handlers"),"kind"));
        assertEquals(3,java.util.stream.StreamSupport.stream(p.path("statements").spliterator(),false).filter(x->x.path("variant").asText().equals("CALL")).count());
        assertEquals(1,java.util.stream.StreamSupport.stream(p.path("statements").spliterator(),false).filter(x->x.path("variant").asText().equals("IF")).count());
        Files.createDirectories(Path.of("target/fd-w2"));Files.writeString(Path.of("target/fd-w2/composition.json"),p.toPrettyString());
    }
    @Test void eopPositiveAndNegativeBodiesAndNoCardinalityCutoff()throws Exception {
        var p=FileDeclarationContractTest.publish("SELECT F ASSIGN TO PRINTDD.","FD F LINAGE IS 60 LINES.\n01 R PIC X(80).","", "WRITE R BEFORE ADVANCING PAGE\n AT EOP CALL 'PAGEEND'\n NOT AT EOP CALL 'MORE' END-WRITE.\nGOBACK.");
        assertEquals(Set.of("AT_END_OF_PAGE","NOT_AT_END_OF_PAGE"),values(uses(p).get(0).path("handlers"),"kind"));
        var many=publish("DELETE F RECORD.\n".repeat(130)+"GOBACK.");assertEquals(130,uses(many).size());
        assertEquals(130,java.util.stream.StreamSupport.stream(uses(many).spliterator(),false).map(u->u.path("statement").asText()).distinct().count());
    }
}
