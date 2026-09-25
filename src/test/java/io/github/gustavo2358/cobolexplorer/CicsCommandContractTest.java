package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R7-R6: source command contracts and positive control with honest condition remainder. */
class CicsCommandContractTest {
    static JsonNode publish(String body) throws Exception {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 MAP-NAME PIC X(8).\n01 WS-AREA PIC X(80).\n01 RC PIC S9(8) COMP.\n01 RC2 PIC S9(8) COMP.",body.replace(" RESP", "\n RESP").replace(" MAPSET", "\n MAPSET").replace(" INTO", "\n INTO").replace(" FROM", "\n FROM")),"command.cbl");
        return CicsAbendContractTest.json(EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED));
    }
    static JsonNode command(JsonNode j) {for(var s:j.path("statements"))if(s.path("variant").asText().equals("CICS_COMMAND"))return s;throw new AssertionError("typed command missing");}
    static List<JsonNode> outcomes(JsonNode j,JsonNode s) {var out=new ArrayList<JsonNode>();for(var o:j.path("controlTopology").path("outcomes"))if(o.path("statement").equals(s.path("header").path("id")))out.add(o);return out;}
    static Set<String> roles(JsonNode j) {var result=new TreeSet<String>();outcomes(j,command(j)).forEach(o->result.add(o.path("kind").asText()+":"+o.path("role").asText()));return result;}
    static JsonNode one(String cmd) throws Exception {return publish("EXEC CICS "+cmd+" END-EXEC.\nDISPLAY 'NEXT'.\nGOBACK.");}
    @Test void requiredTypedProbes() throws Exception {
        for(var pair:Map.of("SYNCPOINT","SYNCPOINT","RECEIVE MAP('M')","RECEIVE_MAP","RECEIVE MAP('M') RESP(RC)","RECEIVE_MAP",
            "RECEIVE MAP('M') RESP(RC) RESP2(RC2)","RECEIVE_MAP","SEND MAP('M')","SEND_MAP","SEND MAP('M') RESP(RC)","SEND_MAP").entrySet()) {
            var j=one(pair.getKey());var c=command(j);assertEquals("2.43.0",j.path("contractVersion").asText());assertEquals(pair.getValue(),c.path("commandKind").asText());assertEquals("SUPPORTED",c.path("syntaxStatus").asText());
            var normals=outcomes(j,c).stream().filter(o->o.path("kind").asText().equals("NORMAL")).toList();assertEquals(1,normals.size());
            assertEquals(j.path("statements").get(1).path("header").path("id"),normals.get(0).path("target").path("reference"));
        }
    }
    @Test void responseAndNohandleAreFamilySpecific() throws Exception {
        for(var family:List.of("SYNCPOINT","RECEIVE MAP('M')")) {
            assertEquals(Set.of("NORMAL:normal","UNKNOWN_LOCAL:cics/handler-or-default-condition"),roles(one(family)));
            assertEquals(Set.of("NORMAL:normal"),roles(one(family+" RESP(RC)")));
            assertEquals(Set.of("NORMAL:normal"),roles(one(family+" NOHANDLE")));
            assertEquals(roles(one(family)),roles(one(family+" RESP2(RC2)")));
        }
        assertEquals(Set.of("NORMAL:normal","UNKNOWN_LOCAL:cics/overflow"),roles(one("SEND MAP('M') RESP(RC)")));
        assertEquals(Set.of("NORMAL:normal","UNKNOWN_LOCAL:cics/overflow"),roles(one("SEND MAP('M') NOHANDLE")));
        assertEquals(Set.of("NORMAL:normal","UNKNOWN_LOCAL:cics/overflow","UNKNOWN_LOCAL:cics/handler-or-default-condition"),roles(one("SEND MAP('M')")));
    }
    @Test void explicitOptionsAndCanonicalReferences() throws Exception {
        var c=command(one("RECEIVE MAP(MAP-NAME) INTO(WS-AREA) RESP(RC) RESP2(RC2)"));var names=new ArrayList<String>();int end=0;
        for(var o:c.path("options")) {names.add(o.path("name").asText());assertTrue(o.path("start").asInt()>=end);end=o.path("end").asInt();
            assertTrue(o.hasNonNull("reference"));assertEquals(o.path("operand").asText().length()-1,o.path("reference").path("provenance").path("expanded").path("endColumn").asInt()-o.path("reference").path("provenance").path("expanded").path("startColumn").asInt());
            assertEquals(o.path("name").asText().equals("MAP")?"READ":"WRITE",o.path("reference").path("role").asText());}
        assertEquals(List.of("MAP","INTO","RESP","RESP2"),names);assertFalse(names.contains("NOHANDLE"));
    }
    @Test void unsupportedSubsetCannotContinue() throws Exception {
        for(var cmd:List.of("SYNCPOINT ROLLBACK","SYNCPOINT MYSTERY","RECEIVE MAP()","RECEIVE MAP('M') RESP(RC) RESP(RC)","SEND MAP('M') ACCUM","SEND MAP('M') CURSOR(RC)","RECEIVE MAP(MAP-NAME)")) {
            var j=one(cmd);assertEquals("UNAVAILABLE",command(j).path("syntaxStatus").asText(),cmd);
            assertTrue(outcomes(j,command(j)).stream().allMatch(o->o.path("kind").asText().equals("UNKNOWN_LOCAL")),cmd);
        }
    }
    @Test void unrelatedAndTerminalCommandsStayOutsideNewFamily() throws Exception {
        for(var cmd:List.of("SEND TEXT FROM(WS-AREA)","RETURN","XCTL PROGRAM('X')","ABEND","READ FILE('F') INTO(WS-AREA) RESP(RC)","MYSTERY RESP(RC)")) {
            var j=one(cmd);assertTrue(j.path("statements").findValuesAsText("variant").stream().noneMatch("CICS_COMMAND"::equals));
            var first=j.path("statements").get(0);
            if(!cmd.startsWith("READ"))assertTrue(outcomes(j,first).stream().noneMatch(o->o.path("target").path("reference").equals(j.path("statements").get(1).path("header").path("id"))),cmd);
        }
    }
    @Test void handlerTargetIsNeverOrdinaryDestination() throws Exception {
        var j=publish("EXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC.\nEXEC CICS SEND MAP('M') RESP(RC) END-EXEC.\nEXEC CICS ABEND END-EXEC.\nERR.\nGOBACK.");
        var h=j.path("statements").get(0);var c=command(j);assertEquals(2,outcomes(j,c).size());
        for(var o:outcomes(j,c))assertNotEquals(h.path("targetEntry"),o.path("target").path("reference"));
        assertEquals(j.path("statements").get(2).path("header").path("id"),outcomes(j,c).stream().filter(o->o.path("kind").asText().equals("NORMAL")).findFirst().orElseThrow().path("target").path("reference"));
    }
    @Test void sourceOrderCannotChooseOutOfScopeSuccessor() throws Exception {
        var j=publish("IF RC = 0\n EXEC CICS SYNCPOINT NOHANDLE END-EXEC\nELSE\n DISPLAY 'ELSE'\nEND-IF.\nDISPLAY 'JOIN'.\nGOBACK.");
        var c=command(j);var target=outcomes(j,c).stream().filter(o->o.path("kind").asText().equals("NORMAL")).findFirst().orElseThrow().path("target");
        assertEquals("COMPLETE",target.path("kind").asText()); // grammar join, never the textual ELSE statement
    }
    @Test void irrelevantRenameAndDeadCodeDoNotChangeCommandQualification() throws Exception {
        var baseline=roles(one("RECEIVE MAP('M') RESP(RC)"));
        for(var label:List.of("UNRELATED","RENAMED")) {
            var j=publish("GO TO LIVE.\n"+label+".\nEXEC CICS MYSTERY END-EXEC.\nLIVE.\nEXEC CICS RECEIVE MAP('M') RESP(RC) END-EXEC.\nGOBACK.");
            assertEquals(baseline,roles(j));assertEquals("SUPPORTED",command(j).path("syntaxStatus").asText());
        }
    }
    @Test void serializationDeterministic() throws Exception {assertEquals(one("SEND MAP('M') RESP(RC)"),one("SEND MAP('M') RESP(RC)"));}
    @Test void copyAndUnitsPreserveCanonicalOperandOwnership() throws Exception {
        var j=publish("COPY R6COMMAND.\nCOPY R6COMMAND.\nGOBACK.");
        var commands=new ArrayList<JsonNode>();for(var f:j.path("statements"))if(f.path("variant").asText().equals("CICS_COMMAND"))commands.add(f);
        assertEquals(2,commands.size());assertNotEquals(commands.get(0).path("header").path("id"),commands.get(1).path("header").path("id"));
        var origins=new ArrayList<JsonNode>();
        for(var c:commands)for(var o:c.path("options"))if(o.path("name").asText().equals("RESP")) {
            var origin=o.path("reference").path("provenance");origins.add(origin);
            assertEquals("R6COMMAND.cpy",origin.path("original").path("file").asText());assertTrue(origin.path("original").path("startLine").asInt()<=2 && origin.path("original").path("endLine").asInt()>=2);
            assertEquals(1,origin.path("includeChain").size());assertFalse(origin.path("exact").asBoolean());
        }
        assertNotEquals(origins.get(0),origins.get(1));
        String body=ScalarMoveCheckpoint4ATest.program("01 RC PIC S9(8) COMP.","EXEC CICS SYNCPOINT RESP(RC) END-EXEC.\nGOBACK.");
        var a=AstBoundaryTestSupport.analyze(body+body.replace("SAMPLE","SECOND"),"units.cbl");
        assertEquals(2,a.model().programUnits().size());
        var first=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        var second=EofUnitBoundaryTest.publish(a,1,StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertNotEquals(first.statements().get(0).header().id(),second.statements().get(0).header().id());
    }
    @Test void diagnosticAbsenceDoesNotSupplySyntaxQualification() {
        var unsupported=CicsCommandSemantics.parse("EXEC CICS SYNCPOINT ROLLBACK END-EXEC").orElseThrow();
        var withoutDiagnostic=new CicsCommandSemantics.Fact(unsupported.command(),unsupported.syntaxStatus(),unsupported.raw(),unsupported.options(),List.of());
        assertFalse(withoutDiagnostic.supported());assertTrue(CicsCommandControl.qualify(withoutDiagnostic).isEmpty());
        var supported=CicsCommandSemantics.parse("EXEC CICS SYNCPOINT END-EXEC").orElseThrow();
        var unrelatedDiagnostic=new CicsCommandSemantics.Fact(supported.command(),supported.syntaxStatus(),supported.raw(),supported.options(),List.of("UNRELATED_MODELING_GAP"));
        assertTrue(unrelatedDiagnostic.supported());assertTrue(CicsCommandControl.qualify(unrelatedDiagnostic).isPresent());
    }
}
