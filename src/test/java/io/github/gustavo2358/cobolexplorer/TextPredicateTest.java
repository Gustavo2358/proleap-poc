package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TextPredicateTest {
    @Test void publishesTypedAbbreviationWithoutInventingCollation()throws Exception {
        var j=LogicalInitialInvariantTest.publish("01 NAME-A PIC X(8) VALUE 'PROGA001'.",
            "IF NAME-A = LOW-VALUES OR SPACES\n MOVE 'FALLBACK' TO NAME-A\nEND-IF.\nCALL NAME-A.\nGOBACK.");
        var predicate=j.path("statements").get(0).path("condition").path("textPredicate");
        assertEquals("OR",predicate.path("kind").asText());
        assertEquals("EQUAL_LOW_VALUES",predicate.path("children").get(0).path("kind").asText());
        assertEquals("EQUAL_SPACES",predicate.path("children").get(1).path("kind").asText());
        assertEquals(predicate.path("children").get(0).path("reference"),predicate.path("children").get(1).path("reference"));
        var out=java.nio.file.Path.of("target/recall-cics");java.nio.file.Files.createDirectories(out);
        java.nio.file.Files.writeString(out.resolve("text-predicate.json"),j.toPrettyString());
    }
    @Test void groupingAndLogicalNotRetainTheirScope() throws Exception {
        var j=LogicalInitialInvariantTest.publish("01 NAME-A PIC X(8).\n01 NAME-B PIC X(8).",
            "IF NOT (NAME-A = 'A' OR 'B') AND NAME-B = SPACES\n CONTINUE END-IF.\nGOBACK.");
        var p=j.path("statements").get(0).path("condition").path("textPredicate");
        assertEquals("AND",p.path("kind").asText());
        assertEquals("NOT",p.path("children").get(0).path("kind").asText());
        assertEquals("OR",p.path("children").get(0).path("children").get(0).path("kind").asText());
        assertEquals("EQUAL_SPACES",p.path("children").get(1).path("kind").asText());
    }
    @Test void numericOrderingAndAmbiguousNamesStayUnknown() throws Exception {
        for(var condition:java.util.List.of("NAME-A = 1", "NAME-A > 'A'", "NAME-A(1:2) = 'AB'")) {
            var j=LogicalInitialInvariantTest.publish("01 NAME-A PIC X(8).", "IF "+condition+"\n CONTINUE END-IF.\nGOBACK.");
            assertTrue(j.path("statements").get(0).path("condition").path("textPredicate").isNull() || j.path("statements").get(0).path("condition").path("textPredicate").isMissingNode(),condition);
        }
        var j=LogicalInitialInvariantTest.publish("01 NAME-A PIC X(8).\n01 NAME-A PIC X(8).", "IF NAME-A = 'A'\n CONTINUE END-IF.\nGOBACK.");
        assertTrue(j.path("statements").get(0).path("condition").path("textPredicate").isNull() || j.path("statements").get(0).path("condition").path("textPredicate").isMissingNode());
    }
}
