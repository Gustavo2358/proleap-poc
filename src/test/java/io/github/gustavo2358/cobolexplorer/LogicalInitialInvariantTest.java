package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogicalInitialInvariantTest {
    static JsonNode publish(String data,String code)throws Exception {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data,code),"logical-invariant.cbl");
        return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED)));
    }
    static JsonNode constant(JsonNode j) {
        for(var c:j.path("storage").path("entryState").path("conditions"))
            if(c.path("logicalText").asText().equals("PROGA001"))return c;
        throw new AssertionError("constant source VALUE missing");
    }
    @Test void closedConstantDoesNotRequireAByteCodecOrFirstEntryAssumption()throws Exception {
        var j=publish("01 REC-A.\n 05 NAME-A PIC X(8) VALUE 'PROGA001'.\n 05 OTHER-A PIC X(8).", "MOVE 'OTHER001' TO OTHER-A.\nCALL NAME-A.\nGOBACK.");
        var c=constant(j);assertEquals("LOGICAL_TEXT",c.path("kind").asText());assertEquals("DECLARATIVE_INVARIANT",c.path("proof").asText());
        assertEquals("UNKNOWN",j.path("storage").path("entryState").path("mode").asText());
        assertEquals("2.46.0",j.path("contractVersion").asText());
        var out=java.nio.file.Path.of("target/recall-cics");java.nio.file.Files.createDirectories(out);
        java.nio.file.Files.writeString(out.resolve("logical-invariant.json"),j.toPrettyString());
    }
    @Test void taskReturnDoesNotMutateAnUnexposedSibling()throws Exception {
        var data="01 NAME-A PIC X(8) VALUE 'PROGA001'.\n01 AREA-A PIC X(8).";
        var j=publish(data,"EXEC CICS RETURN TRANSID('ABCD')\n COMMAREA(AREA-A) LENGTH(8) END-EXEC.");
        assertEquals("LOGICAL_TEXT",constant(j).path("kind").asText());
    }
    @Test void implicitLengthRegisterDoesNotAliasTheMeasuredItem()throws Exception {
        var data="01 NAME-A PIC X(8) VALUE 'PROGA001'.\n01 AREA-A PIC X(16).\n01 KEY-A PIC X(8).\n01 RESP-A PIC S9(8) COMP.";
        var j=publish(data,"EXEC CICS READ FILE('FILEA') RIDFLD(KEY-A)\n INTO(AREA-A) LENGTH(LENGTH OF NAME-A) RESP(RESP-A)\n END-EXEC.\nGOBACK.");
        assertEquals("LOGICAL_TEXT",constant(j).path("kind").asText());
        j=publish(data,"EXEC CICS READ FILE('FILEA') RIDFLD(KEY-A)\n INTO(NAME-A) LENGTH(LENGTH OF NAME-A) RESP(RESP-A)\n END-EXEC.\nGOBACK.");
        assertEquals("POSSIBLE_LOGICAL_TEXT",constant(j).path("kind").asText());
    }
    @Test void writesAliasesEscapesAndMissingCodeRetainLifecycleUncertainty()throws Exception {
        var data="01 REC-A.\n 05 NAME-A PIC X(8) VALUE 'PROGA001'.\n 05 OTHER-A PIC X(8).";
        for(var code:java.util.List.of("MOVE SPACES TO NAME-A.","MOVE SPACES TO REC-A.","CALL 'FOREIGN' USING REC-A.","MOVE FUNCTION CUSTOM-FUNC(NAME-A) TO OTHER-A.","COPY MISSING-CODE.","EXEC CICS UNKNOWN END-EXEC.")) {
            var c=constant(publish(data,code+"\nGOBACK."));assertEquals("POSSIBLE_LOGICAL_TEXT",c.path("kind").asText(),code);
        }
        var c=constant(publish(data+"\n01 ALT-A REDEFINES REC-A PIC X(16).","MOVE SPACES TO ALT-A.\nGOBACK."));
        assertEquals("POSSIBLE_LOGICAL_TEXT",c.path("kind").asText());
    }
}
