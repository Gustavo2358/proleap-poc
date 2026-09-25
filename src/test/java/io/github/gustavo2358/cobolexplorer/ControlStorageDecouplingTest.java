package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source control authority does not consume storage materialization. */
class ControlStorageDecouplingTest {
    static JsonNode publish(String declarations, String body) throws Exception {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(declarations,body),"dimensions.cbl");
        return CicsAbendContractTest.json(EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED));
    }
    static JsonNode send(String declarations,String options)throws Exception {
        return publish(declarations,"EXEC CICS SEND\n FROM(WS-AREA)\n "+options+"\n END-EXEC.\nGOBACK.");
    }
    @Test void groupAndScalarHaveSamePositiveSourceControl()throws Exception {
        for(var declaration:List.of("01 WS-AREA.\n 05 CHILD PIC X(8).","01 WS-AREA PIC X(8).","01 WS-AREA.\n 05 CHILD PIC X(8) OCCURS 10 TIMES.")) {
            var j=send(declaration,"NOHANDLE");
            assertEquals(Set.of("NORMAL:normal"),CicsCommandContractTest.roles(j));
            var command=CicsCommandContractTest.command(j);
            var normal=CicsCommandContractTest.outcomes(j,command).get(0);
            assertEquals(j.path("statements").get(1).path("header").path("id"),normal.path("target").path("reference"));
            assertTrue(j.path("controlTopology").path("proofs").findValuesAsText("rule").contains("cics-command-send-terminal-ordinary-return"));
        }
    }
    @Test void conditionsAreQualifiedByFamilyAndExplicitOption()throws Exception {
        var d="01 WS-AREA.\n 05 CHILD PIC X(8).\n01 RC PIC S9(8) COMP.";
        assertEquals(Set.of("NORMAL:normal","UNKNOWN_LOCAL:cics/handler-or-default-condition"),CicsCommandContractTest.roles(send(d,"")));
        assertEquals(Set.of("NORMAL:normal"),CicsCommandContractTest.roles(send(d,"NOHANDLE")));
        assertEquals(Set.of("NORMAL:normal"),CicsCommandContractTest.roles(send(d,"RESP(RC)")));
        assertEquals(Set.of("NORMAL:normal","UNKNOWN_LOCAL:cics/handler-or-default-condition"),CicsCommandContractTest.roles(send(d,"RESP2(RC)")));
        assertEquals(Set.of("NORMAL:normal","UNKNOWN_LOCAL:cics/overflow"),CicsCommandContractTest.roles(CicsCommandContractTest.one("SEND MAP('M') NOHANDLE")));
        for(var cmd:List.of("SEND FROM(WS-AREA) MYSTERY NOHANDLE","SEND TEXT FROM(WS-AREA) NOHANDLE","RETURN NOHANDLE","ABEND NOHANDLE","MYSTERY NOHANDLE")) {
            var j=publish(d,"EXEC CICS "+cmd+" END-EXEC.\nGOBACK.");
            assertTrue(CicsCommandContractTest.outcomes(j,j.path("statements").get(0)).stream().noneMatch(o->o.path("kind").asText().equals("NORMAL")),cmd);
        }
    }
    @Test void grammarJoinIsNotTextualElseSuccessor()throws Exception {
        var j=publish("01 WS-AREA PIC X.\n01 RC PIC 9.","IF RC = 0\n EXEC CICS SEND FROM(WS-AREA) NOHANDLE END-EXEC\nELSE\n DISPLAY 'ELSE'\nEND-IF.\nGOBACK.");
        var o=CicsCommandContractTest.outcomes(j,CicsCommandContractTest.command(j)).get(0);
        assertEquals("NORMAL",o.path("kind").asText());assertEquals("COMPLETE",o.path("target").path("kind").asText());
    }
}
