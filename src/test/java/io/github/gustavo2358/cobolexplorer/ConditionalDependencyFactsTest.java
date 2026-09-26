package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConditionalDependencyFactsTest {
    @Test void missingPrefixRetainsNominalCopyAndComparisonFacts() throws Exception {
        var j=LogicalInitialInvariantTest.publish("COPY MISSING-AREA.\n01 NAME-A PIC X(8).\n01 NAME-B PIC X(8) VALUE 'DEST0001'.",
            "IF NAME-A = NAME-B OR SPACES\n MOVE NAME-B TO NAME-A\nEND-IF.\nCALL NAME-A.\nGOBACK.");
        var out=java.nio.file.Path.of("target/conditional-source");java.nio.file.Files.createDirectories(out);java.nio.file.Files.writeString(out.resolve("nominal.json"),j.toPrettyString());
        var f=j.path("nominalValues");
        assertEquals("NOMINAL_TEXT_SOURCE_V1",f.path("authority").asText());
        assertEquals(2,f.path("symbols").size());
        assertEquals(1,f.path("assignments").size());
        assertEquals("READ",f.path("assignments").get(0).path("source").path("kind").asText());
        assertEquals(1,f.path("queries").size());
        assertEquals("OR",f.path("conditions").get(0).path("predicate").path("kind").asText());
        assertEquals("EQ",f.path("conditions").get(0).path("predicate").path("children").get(0).path("kind").asText());
    }
    @Test void wrappedCicsOperandAndRepeatedReceiverDoNotEraseNominalFacts() throws Exception {
        var j=LogicalInitialInvariantTest.publish("COPY MISSING-AREA.\n01 NAME-A PIC X(8).",
            "MOVE 'DEST0001' TO NAME-A NAME-A.\nEXEC CICS XCTL\n PROGRAM(NAME-A)\n END-EXEC.\nGOBACK.");
        assertEquals(1,j.path("nominalValues").path("assignments").size());
        assertEquals(1,j.path("nominalValues").path("queries").size());
    }
}
