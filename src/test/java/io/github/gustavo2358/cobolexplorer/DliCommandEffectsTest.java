package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DliCommandEffectsTest {
    private static final String DATA="01 NAME-A PIC X(8) VALUE 'PROGA001'.\n01 AREA-A PIC X(16).\n01 KEY-A PIC X(8).\n01 PCB-A PIC S9(4) COMP VALUE 1.\n01 PSB-A PIC X(8).";
    @Test void databaseReadHasBoundedWritesAndOrdinaryReturn()throws Exception {
        var j=LogicalInitialInvariantTest.publish(DATA,"EXEC DLI GU USING PCB(PCB-A)\n SEGMENT(ROOTSEG) INTO(AREA-A) WHERE(KEYFIELD=KEY-A)\n END-EXEC.\nEXEC DLI TERM END-EXEC.\nCALL NAME-A.\nGOBACK.");
        assertEquals("LOGICAL_TEXT",LogicalInitialInvariantTest.constant(j).path("kind").asText());
        var effect=findEffect(j);assertEquals(1,effect.path("mayWrites").size());
        assertEquals(2,effect.path("knownReads").size());assertTrue(effect.path("mustOverwrite").isEmpty());
        assertEquals("NONE",effect.path("unknownWriteBound").asText());
        assertTrue(j.path("controlTopology").toString().contains("dli-command-ordinary-return"));
        assertTrue(j.path("controlTopology").toString().contains("dli-command-failure"));
        var out=java.nio.file.Path.of("target/recall-cics");java.nio.file.Files.createDirectories(out);
        java.nio.file.Files.writeString(out.resolve("dli-effects.json"),j.toPrettyString());
    }
    @Test void schedulerAndTerminationDoNotMutatePrivateConstants()throws Exception {
        for(var command:java.util.List.of("SCHD PSB((PSB-A)) NODHABEND","SCHD PSB(TESTPSB) SYSSERVE","TERM")) {
            var j=LogicalInitialInvariantTest.publish(DATA,"EXEC DLI "+command.replace(" SEGMENT", "\n SEGMENT").replace(" SECRET", "\n SECRET")+"\n END-EXEC.\nCALL NAME-A.\nGOBACK.");
            assertEquals("LOGICAL_TEXT",LogicalInitialInvariantTest.constant(j).path("kind").asText(),command);
            assertTrue(findEffect(j).path("mayWrites").isEmpty());
        }
    }
    @Test void unknownOperandsAndWritesRetainUncertainty()throws Exception {
        for(var command:java.util.List.of("GU USING PCB(PCB-A) SEGMENT(ROOTSEG) INTO(NAME-A)",
                "GU USING PCB(PCB-A) SEGMENT(ROOTSEG) INTO(AREA-A) SECRET(NAME-A)",
                "GU USING PCB(PCB-A) SEGMENT(ROOTSEG)","SCHD PSB((MISSING-A)) SECRET", "TERM EXTRA")) {
            var j=LogicalInitialInvariantTest.publish(DATA,"EXEC DLI "+command.replace(" SEGMENT", "\n SEGMENT").replace(" SECRET", "\n SECRET")+"\n END-EXEC.\nCALL NAME-A.\nGOBACK.");
            assertEquals("POSSIBLE_LOGICAL_TEXT",LogicalInitialInvariantTest.constant(j).path("kind").asText(),command);
        }
    }
    private static JsonNode findEffect(JsonNode j) {
        for(var e:j.path("statementEffects"))if(e.path("proof").asText().equals("DLI_HOST_OPERANDS"))return e;
        throw new AssertionError("DLI canonical effects missing");
    }
}
