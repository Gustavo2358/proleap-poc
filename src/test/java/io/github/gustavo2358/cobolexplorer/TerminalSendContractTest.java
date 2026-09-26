package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** SP2.44: source family/operand authority, not physical terminal inference. */
class TerminalSendContractTest {
    static JsonNode publish(String body)throws Exception{return CicsCommandContractTest.publish(body.replace(" LENGTH(","\n LENGTH(").replace(" NOHANDLE","\n NOHANDLE").replace(" ERASE","\n ERASE"));}
    static JsonNode one(String options)throws Exception{return publish("EXEC CICS SEND "+options+" END-EXEC.\nGOBACK.");}
    static JsonNode c(JsonNode j){return CicsCommandContractTest.command(j);}
    static List<JsonNode> outcomes(JsonNode j){return CicsCommandContractTest.outcomes(j,c(j));}
    @Test void requiredProbes()throws Exception {
        for(var options:List.of("FROM(WS-AREA)","FROM(WS-AREA) LENGTH(80)","FROM(WS-AREA) LENGTH(LENGTH OF WS-AREA)","FROM(WS-AREA) NOHANDLE","FROM(WS-AREA) ERASE","FROM(WS-AREA) LENGTH(LENGTH OF WS-AREA) NOHANDLE ERASE")) {
            var j=one(options);var f=c(j);assertEquals("2.46.0",j.path("contractVersion").asText());assertEquals("SEND_TERMINAL",f.path("commandKind").asText());assertEquals("SUPPORTED",f.path("syntaxStatus").asText());
            assertEquals("RESOLVED",f.path("options").get(0).path("reference").path("binding").path("status").asText());
            var normal=outcomes(j).stream().filter(o->o.path("kind").asText().equals("NORMAL")).toList();assertEquals(1,normal.size());
            assertEquals(!options.contains("NOHANDLE"),outcomes(j).stream().anyMatch(o->o.path("kind").asText().equals("UNKNOWN_LOCAL")));
        }
    }
    @Test void lengthUsesCanonicalStructureAndOperandOrigin()throws Exception {
        var f=c(one("FROM(WS-AREA) LENGTH(LENGTH OF WS-AREA) NOHANDLE"));var length=f.path("length");
        assertEquals("LENGTH_OF",length.path("kind").asText());assertTrue(length.path("integer").isNull());
        assertEquals(f.path("options").get(0).path("reference").path("binding").path("selected"),length.path("reference").path("binding").path("selected"));
        assertEquals(length.path("reference").path("binding").path("selected"),length.path("reference").path("logicalWholeItem"));
        assertNotEquals(f.path("header").path("provenance"),length.path("provenance"));
        assertEquals(10,length.path("reference").path("provenance").path("expanded").path("startColumn").asInt()-length.path("provenance").path("expanded").path("startColumn").asInt());
        assertEquals("INTEGER",c(one("FROM(WS-AREA) LENGTH(80)")).path("length").path("kind").asText());
        assertEquals("DATA_REFERENCE",c(one("FROM(WS-AREA) LENGTH(RC)")).path("length").path("kind").asText());
    }
    @Test void explicitNohandleQualifiesSourceConditionDisposition()throws Exception {
        var bare=one("FROM(WS-AREA)");var local=one("FROM(WS-AREA) NOHANDLE");
        assertEquals(Set.of("NORMAL:normal","UNKNOWN_LOCAL:cics/handler-or-default-condition"),CicsCommandContractTest.roles(bare));
        assertEquals(Set.of("NORMAL:normal"),CicsCommandContractTest.roles(local));
        assertEquals(List.of("FROM","NOHANDLE"),c(local).path("options").findValuesAsText("name"));
        assertEquals(List.of("FROM","RESP"),c(one("FROM(WS-AREA) RESP(RC)")).path("options").findValuesAsText("name"));
    }
    @Test void familyAndUnsupportedNegatives()throws Exception {
        assertEquals("SEND_MAP",c(one("MAP('M') FROM(WS-AREA)")).path("commandKind").asText());
        for(var s:List.of("TEXT FROM(WS-AREA)","CONTROL ERASE","CONVID(RC) FROM(WS-AREA)"))
            assertTrue(one(s).path("statements").findValuesAsText("variant").stream().noneMatch("CICS_COMMAND"::equals));
        for(var s:List.of("ERASE","FROM()","FROM(WS-AREA) FROM(WS-AREA)","FROM(WS-AREA) MYSTERY","FROM(WS-AREA) LENGTH(FUNCTION LENGTH(WS-AREA))")) {
            var j=one(s);assertEquals("UNAVAILABLE",c(j).path("syntaxStatus").asText());assertTrue(outcomes(j).stream().noneMatch(o->o.path("kind").asText().equals("NORMAL")));
        }
    }
    @Test void copyProvenanceAndDistinctOccurrences()throws Exception {
        var j=publish("COPY R7BSEND.\nCOPY R7BSEND.\nGOBACK.");var commands=new ArrayList<JsonNode>();for(var f:j.path("statements"))if(f.path("variant").asText().equals("CICS_COMMAND"))commands.add(f);
        assertEquals(2,commands.size());assertNotEquals(commands.get(0).path("header").path("id"),commands.get(1).path("header").path("id"));
        for(var f:commands) {
            var p=f.path("length").path("provenance");assertEquals("R7BSEND.cpy",p.path("original").path("file").asText());assertEquals(3,p.path("original").path("startLine").asInt());assertEquals(1,p.path("includeChain").size());
            assertEquals(p.path("includeChain"),f.path("length").path("reference").path("provenance").path("includeChain"));
        }
    }
    @Test void metamorphicClassificationAndDeterminism()throws Exception {
        for(var s:List.of("FROM(WS-AREA) LENGTH(LENGTH OF WS-AREA) NOHANDLE","FROM(MAP-NAME) LENGTH(LENGTH OF MAP-NAME) NOHANDLE","FROM(WS-AREA) LENGTH(80) NOHANDLE","FROM(WS-AREA) LENGTH(LENGTH OF WS-AREA) NOHANDLE ERASE")) {
            assertEquals("SEND_TERMINAL",c(one(s)).path("commandKind").asText());assertEquals(one(s),one(s));
        }
        var j=publish("GO TO LIVE.\nDEAD.\nEXEC CICS SEND FROM(WS-AREA) END-EXEC.\nLIVE.\nGOBACK.");
        assertEquals("SEND_TERMINAL",c(j).path("commandKind").asText()); // qualification does not create an incoming path
        var branched=publish("IF RC = 0\nEXEC CICS SEND FROM(WS-AREA) NOHANDLE END-EXEC\nELSE\nDISPLAY 'ELSE'\nEND-IF.\nGOBACK.");
        assertEquals("COMPLETE",outcomes(branched).get(0).path("target").path("kind").asText());
    }
}
