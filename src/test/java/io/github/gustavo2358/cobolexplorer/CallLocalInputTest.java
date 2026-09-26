package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CallLocalInputTest {
    @Test void unrelatedDataCopyDoesNotEraseCallSyntaxOrIndependentTarget()throws Exception {
        var j=LogicalInitialInvariantTest.publish("01 NAME-A PIC X(8).\n01 ARG-A PIC X(8).\nCOPY MISSING-DATA.","MOVE 'PROGA001' TO NAME-A.\nCALL NAME-A USING ARG-A.\nGOBACK.");
        var call=j.path("statements").get(1);
        assertEquals("PRESENT",call.path("surface").path("using").asText());
        assertEquals(1,call.path("surface").path("argumentCount").asInt());
        assertFalse(call.path("target").path("reference").path("wholeItemAccess").isNull());
        assertEquals("KNOWN",call.path("normalContinuation").path("availability").asText());
        var out=java.nio.file.Path.of("target/recall-cics");java.nio.file.Files.createDirectories(out);
        java.nio.file.Files.writeString(out.resolve("call-local-input.json"),j.toPrettyString());
    }
    @Test void missingProcedureInputStillBlocksTheProof()throws Exception {
        var j=LogicalInitialInvariantTest.publish("01 NAME-A PIC X(8).","COPY MISSING-CODE.\nCALL NAME-A USING NAME-A.\nGOBACK.");
        for(var s:j.path("statements"))if(s.path("variant").asText().equals("CALL"))assertEquals("UNKNOWN",s.path("surface").path("using").asText());
    }
}
