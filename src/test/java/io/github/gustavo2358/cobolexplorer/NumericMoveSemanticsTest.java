package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
class NumericMoveSemanticsTest {
    private JsonNode publish(String data,String procedure) throws Exception {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data,procedure),"numeric.cbl");
        return new ObjectMapper().readTree(io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED)));
    }
    @Test void logicalIntegersUseLocalProofsInsideGroupsAndWithValue() throws Exception {
        var p=publish("01 REC-A.\n05 COUNT-A PIC 99 VALUE 12.\n05 COUNT-B PIC 9(4).", "MOVE 12 TO COUNT-A.\nMOVE COUNT-A OF REC-A TO COUNT-B.\nGOBACK.");
        assertEquals("2.65.0",p.path("contractVersion").asText());
        int transfers=0;for(var s:p.path("statements"))transfers+=s.path("integerTransfers").size();
        assertEquals(2,transfers);
        assertFalse(p.path("gaps").toString().contains("LITERAL_KIND_NOT_PUBLISHED"));
        assertFalse(p.path("gaps").toString().contains("MOVE_IDENTITY_NOT_PROVEN"));
    }
    @Test void numericLiteralPublicationDoesNotInventUnsupportedConversion() throws Exception {
        var p=publish("01 SMALL-A PIC 99.\n01 PACKED-A PIC 99 COMP-3.","MOVE 123 TO SMALL-A.\nMOVE -1 TO SMALL-A.\nMOVE 1.5 TO SMALL-A.\nMOVE 1 TO PACKED-A.\nGOBACK.");
        int numeric=0,transfers=0;for(var s:p.path("statements")){if(s.path("source").path("kind").asText().equals("NUMERIC"))numeric++;transfers+=s.path("integerTransfers").size();}
        assertEquals(4,numeric);assertEquals(0,transfers);
        assertTrue(p.path("gaps").toString().contains("MOVE_IDENTITY_NOT_PROVEN"));
        assertFalse(p.path("gaps").toString().contains("LITERAL_KIND_NOT_PUBLISHED"));
    }
    @Test void zeroAndMultipleReceiversKeepWrittenOrder() throws Exception {
        var p=publish("01 COUNT-A PIC 99.\n01 COUNT-B PIC 9(4).","MOVE ZERO TO COUNT-A COUNT-B.\nGOBACK.");
        var move=p.path("statements").get(0);assertEquals(2,move.path("integerTransfers").size());
        assertEquals("0",move.path("integerTransfers").get(0).path("value").asText());
        assertFalse(p.path("gaps").toString().contains("MOVE_IDENTITY_NOT_PROVEN"));
    }
    @Test void aliasAndTableDoNotBecomeIndependentIntegerCells() throws Exception {
        var p=publish("01 AREA-A PIC 99.\n01 ALIAS-A REDEFINES AREA-A PIC XX.\n01 TABLE-A.\n05 ITEM-A PIC 99 OCCURS 2.","MOVE 1 TO AREA-A.\nMOVE 1 TO ITEM-A(1).\nGOBACK.");
        for(var s:p.path("statements"))assertEquals(0,s.path("integerTransfers").size());
    }
    @Test void integralFixedPointSpellingUsesItsValueAndHugePicDoesNotExpand() throws Exception {
        var p=publish("01 COUNT-A PIC 999.\n01 HUGE-A PIC 9(1000000000).", "MOVE 12.00 TO COUNT-A.\nMOVE 1 TO HUGE-A.\nMOVE 1.0E+2 TO COUNT-A.\nGOBACK.");
        assertEquals("12",p.path("statements").get(0).path("integerTransfers").get(0).path("value").asText());
        assertEquals(0,p.path("statements").get(1).path("integerTransfers").size());
        assertEquals(0,p.path("statements").get(2).path("integerTransfers").size());
    }

    @Test void dataPrefixSurvivesAnUnsupportedPeerWithoutCertifyingLaterReads() throws Exception {
        var p=publish("01 COUNT-A PIC 99.\n01 COUNT-B PIC 99.\n01 TEXT-A PIC XX.","MOVE COUNT-A TO COUNT-B TEXT-A COUNT-B.\nGOBACK.");
        var m=p.path("statements").get(0);assertEquals("MOVE",m.path("variant").asText());
        assertEquals(1,m.path("integerTransfers").size());
        assertEquals("operand:0:1",m.path("integerTransfers").get(0).path("target").asText());
        assertTrue(p.path("gaps").toString().contains("MOVE_IDENTITY_NOT_PROVEN"));
    }

    @Test void unresolvedPeerRetainsBindingGapWithoutErasingProvedLiteralReceiver() throws Exception {
        var p=publish("01 COUNT-A PIC 99.","MOVE 12 TO COUNT-A MISSING-A.\nGOBACK.");
        var m=p.path("statements").get(0);assertEquals("MOVE",m.path("variant").asText());
        assertEquals(1,m.path("integerTransfers").size());
        assertTrue(p.path("gaps").toString().contains("REFERENCE_UNRESOLVED_DECLARATION_NOT_FOUND"));
    }

    @Test void usageOfTheGroupAppliesToItsNumericChildren() throws Exception {
        for(String usage:new String[]{"USAGE COMP-5","GROUP-USAGE NATIONAL"}) {
            var p=publish("01 REC-A "+usage+".\n05 COUNT-A PIC 99.\n01 COUNT-B PIC 9999.","MOVE 1 TO COUNT-A.\nMOVE COUNT-A TO COUNT-B.\nGOBACK.");
            for(var m:p.path("statements"))assertEquals(0,m.path("integerTransfers").size(),usage);
        }
        var p=publish("01 REC-A USAGE DISPLAY.\n05 COUNT-A PIC 99.","MOVE 1 TO COUNT-A.\nGOBACK.");
        assertEquals(1,p.path("statements").get(0).path("integerTransfers").size());
    }

}
