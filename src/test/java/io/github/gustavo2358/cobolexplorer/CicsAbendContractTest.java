package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Source/event authority only. No state engine or dispatch oracle is implemented here. */
class CicsAbendContractTest {
    static CobolSemanticPort port(String code) {
        return EofUnitBoundaryTest.publish(AstBoundaryTestSupport.analyze(
            ScalarMoveCheckpoint4ATest.program("01 CODE-VALUE PIC X(4).",code),"event.cbl"),0,StorageLayoutSemantics.Profile.UNSPECIFIED);
    }
    static JsonNode json(CobolSemanticPort p) throws Exception {return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(p));}
    static JsonNode publish(String code) throws Exception {return json(port(code));}
    static List<JsonNode> events(JsonNode j) {
        var out=new ArrayList<JsonNode>();j.path("statements").forEach(s->{if(s.path("variant").asText().equals("CICS_ABEND"))out.add(s);});return out;
    }
    static JsonNode event(String options,String eligibility) throws Exception {
        var j=publish("EXEC CICS ABEND "+options+" END-EXEC.\nDISPLAY 'AFTER'.\nGOBACK.");
        assertEquals(1,events(j).size(),"one explicit event occurrence");var e=events(j).get(0);
        assertEquals("ABEND",e.path("eventKind").asText());assertEquals(eligibility,e.path("dispatchEligibility").asText());return e;
    }
    @Test void plainAbendIsEligible() throws Exception {event("","HANDLER_ELIGIBLE");}
    @Test void cancelBypassesHandlers() throws Exception {event("CANCEL","HANDLERS_BYPASSED");}
    @Test void versionReflectsNewEventOrPositiveControlFeature() throws Exception {
        assertEquals("2.41.0",publish("GOBACK.").path("contractVersion").asText());
        for(var source:List.of("EXEC CICS HANDLE ABEND CANCEL END-EXEC.\nGOBACK.","EXEC CICS ABEND END-EXEC."))assertEquals("2.42.0",publish(source).path("contractVersion").asText());
    }
    @Test void registrationIsSeparateFromEvent() throws Exception {
        var j=publish("EXEC CICS HANDLE ABEND END-EXEC.\nEXEC CICS ABEND END-EXEC.");
        assertEquals("CICS_HANDLER",j.path("statements").get(0).path("variant").asText());assertEquals(1,events(j).size());
    }
    @Test void mentionsElsewhereDoNotCreateEvents() throws Exception {
        var j=publish("DISPLAY 'EXEC CICS ABEND'.\nEXEC CICS LINK PROGRAM('ABEND') NOHANDLE END-EXEC.\nEXEC CICS HANDLE CONDITION ERROR(ABEND-EXIT) END-EXEC.\nGOBACK.\nABEND-EXIT.\nGOBACK.");
        assertTrue(events(j).isEmpty());
    }
    @Test void malformedAndUnsupportedAreUnavailable() throws Exception {
        for(var s:List.of("CANCEL(X)","ABCODE()","NODUMP(X)","MYSTERY","CANCEL MYSTERY","ABCODE(FUNCTION UNKNOWN(X))","RESP(CODE-VALUE)"))
            assertFalse(event(s,"UNAVAILABLE").path("gapCodes").isEmpty(),s);
    }
    @Test void duplicateOptionsAreUnavailable() throws Exception {
        for(var s:List.of("CANCEL CANCEL","NODUMP NODUMP","ABCODE('AAAA') ABCODE('BBBB')"))event(s,"UNAVAILABLE");
    }
    @Test void dumpOptionsKeepEligibilityAndOrderedOffsets() throws Exception {
        for(var operand:List.of("'T001'","\"T002\"","CODE-VALUE")) {
            var e=event("NODUMP ABCODE("+operand+") CANCEL","HANDLERS_BYPASSED");
            var names=new ArrayList<String>();int previous=-1;String raw=e.path("rawText").asText();
            for(var option:e.path("options")) {
                names.add(option.path("name").asText());int start=option.path("start").asInt(),end=option.path("end").asInt();
                assertTrue(start>previous && end>start && end<=raw.length());
                assertTrue(raw.substring(start,end).startsWith(option.path("name").asText()));previous=start;
            }
            assertEquals(List.of("NODUMP","ABCODE","CANCEL"),names);
            assertEquals(operand,e.path("options").get(1).path("operand").asText());
            event("ABCODE("+operand+") NODUMP","HANDLER_ELIGIBLE");
        }
    }
    @Test void eventHasNoTargetOrNormalContinuation() throws Exception {
        var e=event("CANCEL","HANDLERS_BYPASSED");
        var fields=new TreeSet<String>();e.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("variant","header","eventKind","dispatchEligibility","rawText","options","gapCodes"),fields);
    }
    @Test void abendNeverAcquiresNormalOrDispatchEdge() throws Exception {
        var j=publish("EXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC.\nEXEC CICS ABEND END-EXEC.\nDISPLAY 'AFTER'.\nGOBACK.\nERR.\nGOBACK.");
        assertEquals(1,events(j).size());String id=events(j).get(0).path("header").path("id").asText();int count=0;
        for(var o:j.path("controlTopology").path("outcomes"))if(o.path("statement").asText().equals(id)) {
            count++;assertEquals("UNKNOWN_LOCAL",o.path("kind").asText());assertEquals("UNKNOWN_LOCAL",o.path("target").path("kind").asText());
        }
        assertEquals(1,count);
        for(var o:j.path("ordinaryContinuations"))assertNotEquals(id,o.path("statement").asText());
    }
    @Test void knownRegistrationUsesOrdinaryStructureNotHandlerTarget() throws Exception {
        for(var selector:List.of("LABEL(ERR)","CANCEL","RESET","")) {
            var j=publish("EXEC CICS HANDLE ABEND "+selector+" END-EXEC.\nDISPLAY 'ORDINARY'.\nGOBACK.\nERR.\nGOBACK.");
            var h=j.path("statements").get(0);String id=h.path("header").path("id").asText();int count=0;
            for(var o:j.path("controlTopology").path("outcomes"))if(o.path("statement").asText().equals(id)) {
                count++;assertEquals("NORMAL",o.path("kind").asText());
                assertEquals(j.path("statements").get(1).path("header").path("id").asText(),o.path("target").path("reference").asText());
                assertNotEquals(h.path("targetEntry").asText(),o.path("target").path("reference").asText());
            }
            assertEquals(1,count);
        }
    }
    @Test void unresolvedAndProgramRegistrationsRemainPartial() throws Exception {
        for(var selector:List.of("LABEL(MISSING)","PROGRAM('EXITPGM')","CANCEL RESET")) {
            var j=publish("EXEC CICS HANDLE ABEND "+selector+" END-EXEC.\nGOBACK.");
            String id=j.path("statements").get(0).path("header").path("id").asText();
            for(var o:j.path("controlTopology").path("outcomes"))if(o.path("statement").asText().equals(id))assertEquals("UNKNOWN_LOCAL",o.path("kind").asText());
        }
    }
    @Test void duplicateTextKeepsOccurrenceAndUnitOwnership() throws Exception {
        var j=publish("EXEC CICS ABEND END-EXEC.\nEXEC CICS ABEND END-EXEC.");var es=events(j);assertEquals(2,es.size());
        assertNotEquals(es.get(0).path("header").path("id"),es.get(1).path("header").path("id"));
        assertNotEquals(es.get(0).path("header").path("provenance"),es.get(1).path("header").path("provenance"));
        var a=AstBoundaryTestSupport.analyze("IDENTIFICATION DIVISION.\nPROGRAM-ID. A.\nPROCEDURE DIVISION.\nEXEC CICS ABEND END-EXEC.\nEND PROGRAM A.\nIDENTIFICATION DIVISION.\nPROGRAM-ID. B.\nPROCEDURE DIVISION.\nEXEC CICS ABEND END-EXEC.\nEND PROGRAM B.","units.cbl");
        var first=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);var second=EofUnitBoundaryTest.publish(a,1,StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertNotEquals(first.statements().get(0).header().id(),second.statements().get(0).header().id());
        assertEquals(1,events(json(first)).size());assertEquals(1,events(json(second)).size());
    }
    @Test void copyRetainsOriginalAndIncludeChain() throws Exception {
        var es=events(publish("COPY R7ABEND.\nCOPY R7ABEND.\nGOBACK."));assertEquals(2,es.size());
        for(var e:es) {
            var p=e.path("header").path("provenance");assertEquals("R7ABEND.cpy",p.path("original").path("file").asText());
            assertEquals(1,p.path("includeChain").size());assertFalse(p.path("exact").asBoolean());
            assertEquals("HANDLERS_BYPASSED",e.path("dispatchEligibility").asText());
        }
        assertNotEquals(es.get(0).path("header").path("provenance"),es.get(1).path("header").path("provenance"));
    }
    @Test void eventHeaderKeepsTheCompleteCanonicalObservedProvenance() throws Exception {
        for(var code:List.of("EXEC CICS ABEND\n ABCODE('T001') CANCEL\n END-EXEC.","COPY R7ABEND.\nCOPY R7ABEND.")) {
            var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("",code),"canonical.cbl");
            var old=json(HistoricalCicsHandler241TestSupport.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED));
            var current=json(EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED));
            for(var event:events(current)) {
                var previous=new ArrayList<JsonNode>();old.path("statements").forEach(s->{if(s.path("header").path("id").equals(event.path("header").path("id")))previous.add(s);});
                assertEquals(1,previous.size());assertEquals("OBSERVED",previous.get(0).path("variant").asText());
                for(var field:List.of("id","ordinal","containment","provenance"))assertEquals(previous.get(0).path("header").path(field),event.path("header").path(field),field);
            }
        }
    }
    @Test void deterministicIndependentPublications() throws Exception {
        for(var code:List.of("EXEC CICS ABEND END-EXEC.","EXEC CICS ABEND CANCEL END-EXEC.","EXEC CICS ABEND ABCODE(CODE-VALUE) NODUMP END-EXEC.","COPY R7ABEND.","EXEC CICS ABEND MYSTERY END-EXEC."))
            assertArrayEquals(SemanticProductJsonWriter.serialize(port(code)),SemanticProductJsonWriter.serialize(port(code)));
    }
}
