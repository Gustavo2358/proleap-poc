package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.DeclarativeValueInferenceTest.*;

/** RF-W2 independent wire oracles: effect precision is not value interpretation. */
class StatementEffectSummaryTest {
    private static JsonNode statement(String code) throws Exception {
        var wire=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(product(VALUE,code+"\nCALL LIT-PGM.")));
        var statement=(com.fasterxml.jackson.databind.node.ObjectNode)wire.path("statements").get(0);
        if(!wire.path("statementEffects").isEmpty())statement.set("effects",wire.path("statementEffects").get(0));
        return statement;
    }
    @Test void literalDisplayProvesNoMemoryWriteWithoutBecomingNop() throws Exception {
        var s=statement("DISPLAY 'TRACE'.");var e=s.path("effects");
        assertEquals("OBSERVED",s.path("variant").asText());
        assertEquals("1.0.0",e.path("version").asText());
        assertEquals("NONE",e.path("unknownWriteBound").asText());
        assertEquals("NONE",e.path("unknownExposureBound").asText());
        assertEquals("OUTPUT",e.path("environment").asText());
        assertEquals(0,e.path("knownReads").size());
        invariant(VALUE,"DISPLAY 'TRACE'.\nCALL LIT-PGM.");
    }
    @Test void simpleReferenceDisplayPublishesCanonicalRead() throws Exception {
        var s=statement("DISPLAY LIT-PGM.");var e=s.path("effects");
        assertEquals("NONE",e.path("unknownWriteBound").asText());
        assertEquals(1,e.path("knownReads").size());
        assertEquals(1,s.path("knownReferences").size());
        assertFalse(s.path("knownReferences").get(0).path("regionalAccess").isNull());
        invariant(VALUE,"DISPLAY LIT-PGM.\nCALL LIT-PGM.");
    }
    @Test void unsupportedDisplayFormsAndOtherStatementsDoNotInventWrites() throws Exception {
        for(var code:new String[]{"DISPLAY FUNCTION CURRENT-DATE.","DISPLAY LIT-PGM(1:1).", "DISPLAY 'X' AT 1.",
                "DISPLAY 'X' ON EXCEPTION MOVE 'OTHER' TO LIT-PGM END-DISPLAY.","EXHIBIT LIT-PGM."}) {
            var s=statement(code);
            assertTrue(s.path("effects").isMissingNode()||s.path("effects").isNull()
                ||s.path("effects").path("unknownWriteBound").asText().equals("NONE"),code);
            if(code.contains("ON EXCEPTION"))notInvariant(VALUE,code+"\nCALL LIT-PGM.");
            else invariant(VALUE,code+"\nCALL LIT-PGM.");
        }
    }
}
