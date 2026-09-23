package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Positive control facts have grammar authority, independent of unrelated features or predicate coverage. */
class ControlCompositionTest {
    private static String program(String body) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. CONTROL7.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n"
            + "01 WS-TARGET PIC X(8).\n01 FLAG PIC X.\n01 N PIC 9.\nPROCEDURE DIVISION.\n"+body;
    }
    private static JsonNode publish(String body) throws Exception { return PerformFamilyTest.publish(program(body)); }
    private static List<JsonNode> facts(JsonNode sp,String variant) {
        var result=new ArrayList<JsonNode>();for(var f:sp.path("statements"))if(f.path("variant").asText().equals(variant))result.add(f);return result;
    }
    @Test void irrelevantDependingDoesNotPromoteIntrinsicMoveOrChangeOrdinaryRelation() throws Exception {
        var body="MAIN.\nMOVE 'PROGA001' TO WS-TARGET.\nP-CALL.\nCALL WS-TARGET.\nGOBACK.\n";
        var base=publish(body);var peer=publish(body+"UNUSED.\nGO TO P-A P-B DEPENDING ON N.\nP-A.\nCONTINUE.\nP-B.\nGOBACK.\n");
        var a=facts(base,"MOVE").get(0);var b=facts(peer,"MOVE").get(0);
        assertEquals(a,b,"unrelated peer cannot mutate MOVE facts");
        assertEquals("2.37.0",base.path("contractVersion").asText());
        assertEquals(base.path("ordinaryContinuations"),peer.path("ordinaryContinuations"));
        assertEquals(a.path("header").path("id"),base.path("ordinaryContinuations").get(0).path("statement"));
        assertEquals(facts(base,"CALL").get(0).path("header").path("id"),base.path("ordinaryContinuations").get(0).path("destination"));
        assertNotEquals("KNOWN",a.path("normalContinuation").path("availability").asText());
    }
    @Test void absentClausesKeepEntryAndConditionalFrontierWithoutOrdinaryResume() throws Exception {
        for(var body:List.of("IF FLAG = 'Y' MOVE 'PROGB001' TO WS-TARGET END-IF.",
                "EVALUATE FLAG WHEN 'Y' MOVE 'PROGB001' TO WS-TARGET END-EVALUATE.")) {
            var sp=publish("MAIN.\nPERFORM P-WORK.\nCALL WS-TARGET.\nGOBACK.\nP-WORK.\n"+body);
            var branch=facts(sp,body.startsWith("IF")?"IF":"EVALUATE").get(0);
            var arm=branch.path(body.startsWith("IF")?"thenArm":"arms");if(!body.startsWith("IF"))arm=arm.get(0).path("control");
            assertEquals("KNOWN",arm.path("entry").path("availability").asText());
            assertEquals("ABSENT",branch.path(body.startsWith("IF")?"elseArm":"otherArm").path("presence").asText());
            assertEquals("UNAVAILABLE",branch.path("normalContinuation").path("availability").asText());
            var id=branch.path("header").path("id");
            assertTrue(PerformFamilyTest.ranges(sp).stream().anyMatch(p->p.path("procedures").toString().contains(id.asText())));
            for(var relation:sp.path("ordinaryContinuations"))assertNotEquals(id,relation.path("statement"));
        }
    }
    @Test void numericEvaluatePublishesArmsWithoutInventingScalarAccess() throws Exception {
        var sp=publish("MAIN.\nEVALUATE N WHEN 1 GO TO P-A WHEN OTHER GOBACK END-EVALUATE.\nP-A.\nCALL WS-TARGET.\nGOBACK.\n");
        var e=facts(sp,"EVALUATE").get(0);assertEquals(1,e.path("arms").size());
        assertEquals("KNOWN",e.path("arms").get(0).path("control").path("entry").path("availability").asText());
        assertTrue(e.path("subject").path("wholeItemAccess").isMissingNode()||e.path("subject").path("wholeItemAccess").isNull());
        assertFalse(e.path("subject").path("binding").isNull());
    }
    @Test void transferAndSpecialExitNeverAcquireOrdinaryContinuation() throws Exception {
        var sp=publish("MAIN.\nGO TO P-A.\nMOVE 'BADPGM01' TO WS-TARGET.\nP-A.\nGOBACK.\nP-X.\nEXIT PARAGRAPH.\nP-Y.\nCALL WS-TARGET.\nGOBACK.\n");
        var excluded=new HashSet<JsonNode>();for(var f:sp.path("statements"))if(!f.path("variant").asText().equals("MOVE"))excluded.add(f.path("header").path("id"));
        for(var relation:sp.path("ordinaryContinuations"))assertFalse(excluded.contains(relation.path("statement")));
    }
}
