package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CicsConditionRegistrationTest {
    @Test void registeringConditionsContinuesWithoutEnteringTheHandler()throws Exception {
        var j=LogicalInitialInvariantTest.publish("01 NAME-A PIC X(8) VALUE 'PROGA001'.",
            "MAIN-PARA.\nEXEC CICS HANDLE CONDITION\n PGMIDERR(ERROR-PARA) LENGERR\n END-EXEC.\nCALL NAME-A.\nGOBACK.\nERROR-PARA.\nGOBACK.");
        assertTrue(j.path("statementEffects").toString().contains("CICS_CONDITION_REGISTRATION"));
        assertTrue(j.path("controlTopology").toString().contains("cics-handle-condition-ordinary-return"));
        assertEquals("LOGICAL_TEXT",LogicalInitialInvariantTest.constant(j).path("kind").asText());
        var out=java.nio.file.Path.of("target/recall-cics");java.nio.file.Files.createDirectories(out);
        java.nio.file.Files.writeString(out.resolve("condition-registration.json"),j.toPrettyString());
    }
    @Test void malformedAndUnboundLabelsDoNotClaimCompletion()throws Exception {
        for(var options:java.util.List.of("PGMIDERR(MISSING-PARA)","PGMIDERR('LABEL')","TYPO(ERROR-PARA)","PGMIDERR(ERROR-PARA) PGMIDERR(ERROR-PARA)","PGMIDERR(ERROR-PARA) RESP(NAME-A)")) {
            var j=LogicalInitialInvariantTest.publish("01 NAME-A PIC X(8).","EXEC CICS HANDLE CONDITION\n "+options.replace(" PGMIDERR", "\n PGMIDERR").replace(" RESP", "\n RESP")+"\n END-EXEC.\nGOBACK.\nERROR-PARA.\nGOBACK.");
            assertFalse(j.path("controlTopology").toString().contains("cics-handle-condition-ordinary-return"),options);
        }
    }
}
