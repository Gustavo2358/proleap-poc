package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.DeclarativeValueInferenceTest.*;

class PartialWriteEffectsTest {
    private static JsonNode summary(String data,String code) throws Exception {
        var wire=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(product(data,code+"\nCALL LIT-PGM.")));
        assertEquals(1,wire.path("statementEffects").size(),code);
        return wire.path("statementEffects").get(0);
    }
    @Test void initializeElementaryIsMustButGroupIsOnlyMay() throws Exception {
        assertEquals(1,summary(VALUE,"INITIALIZE LIT-PGM.").path("mustOverwrite").size());
        assertEquals(0,summary(GROUP,"INITIALIZE WS-AREA.").path("mustOverwrite").size());
        invariant(VALUE+"01 ARG PIC X(20).\n","INITIALIZE ARG.\nCALL LIT-PGM.");
        notInvariant(GROUP,"INITIALIZE WS-AREA.\nCALL LIT-PGM.");
    }
    @Test void simpleReceivingFamiliesPublishMayFootprintsWithoutInventingValues() throws Exception {
        for(var code:new String[]{"ACCEPT ARG.","STRING 'X' DELIMITED BY SIZE INTO ARG END-STRING.",
                "UNSTRING LIT-PGM DELIMITED BY SPACE INTO ARG END-UNSTRING.","INSPECT ARG REPLACING ALL 'A' BY 'B'."}) {
            var e=summary(VALUE+"01 ARG PIC X(20).\n",code);
            assertEquals(1,e.path("mayWrites").size(),code);assertEquals(0,e.path("mustOverwrite").size(),code);
            invariant(VALUE+"01 ARG PIC X(20).\n",code+"\nCALL LIT-PGM.");
        }
    }
    @Test void arithmeticAndSetHaveTypedReceiversButUnprovedStorageStaysUnknown() throws Exception {
        for(var code:new String[]{"ADD 1 TO N.","COMPUTE N = 1 + 2.","SET PTR TO ADDRESS OF LIT-PGM."}) {
            var e=summary(VALUE+"01 N PIC 9.\n01 PTR USAGE POINTER.\n",code);
            assertFalse(e.path("mayWrites").isEmpty(),code);assertEquals(0,e.path("mustOverwrite").size(),code);
            notInvariant(VALUE+"01 N PIC 9.\n01 PTR USAGE POINTER.\n",code+"\nCALL LIT-PGM.");
        }
    }
    @Test void unresolvedTargetsAndConditionalHandlersNeverBecomeMust() throws Exception {
        notInvariant(VALUE,"INITIALIZE MISSING.\nCALL LIT-PGM.");
        var wire=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(product(VALUE+"01 N PIC 9.\n",
            "ADD 1 TO N ON SIZE ERROR DISPLAY 'ERR' END-ADD.\nCALL LIT-PGM.")));
        for(var e:wire.path("statementEffects"))assertTrue(e.path("mustOverwrite").isEmpty());
    }
}
