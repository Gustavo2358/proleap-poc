package io.github.gustavo2358.cobolexplorer;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CicsHostEffectsTest {
    @Test void positiveHostEffectsAreExplicitAndSeparateFromControl() throws Exception {
        for(var text:java.util.List.of("RECEIVE MAP('M') INTO(WS-AREA) RESP(RC)","SEND MAP(MAP-NAME) FROM(WS-AREA) RESP(RC)","SYNCPOINT NOHANDLE")) {
            var j=CicsCommandContractTest.one(text);var c=CicsCommandContractTest.command(j);
            assertTrue(c.hasNonNull("hostEffects"),text);
            assertEquals("2.46.0",j.path("contractVersion").asText());
            assertTrue(CicsCommandContractTest.roles(j).contains("NORMAL:normal"));
        }
        var receive=CicsCommandContractTest.one("RECEIVE MAP('M') INTO(WS-AREA) RESP(RC)");
        assertEquals(1,CicsCommandContractTest.command(receive).path("hostEffects").path("literalOptions").size());
        assertTrue(CicsCommandContractTest.roles(CicsCommandContractTest.one("SEND MAP('M') FROM(WS-AREA) NOHANDLE")).contains("UNKNOWN_LOCAL:cics/overflow"));
        var out=Path.of("target/recall-cics");Files.createDirectories(out);
        Files.writeString(out.resolve("receive.json"),receive.toPrettyString());
    }
    @Test void registrationKeepsOrdinaryContinuationWithoutRunningTheHandler()throws Exception {
        var j=CicsCommandContractTest.publish("EXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC.\nCALL 'NEXTONE'.\nGOBACK.\nERR.\nCALL 'HANDLER'.\nGOBACK.");
        var h=j.path("statements").get(0);
        assertEquals("NO_APPLICATION_MEMORY",h.path("registrationEffects").asText());
        assertEquals("2.46.0",j.path("contractVersion").asText());
        var outcomes=CicsCommandContractTest.outcomes(j,h);
        assertEquals(1,outcomes.size());assertEquals("NORMAL",outcomes.get(0).path("kind").asText());
        assertEquals(j.path("statements").get(1).path("header").path("id"),outcomes.get(0).path("target").path("reference"));
        for(var cmd:java.util.List.of("LABEL(MISSING)","LABEL(ERR) RESP(RC)","PROGRAM('PGM')","LABEL(ERR) CANCEL")) {
            var bad=CicsCommandContractTest.publish("EXEC CICS HANDLE ABEND "+cmd+" END-EXEC.\nGOBACK.\nERR.\nGOBACK.");
            assertFalse(bad.path("statements").get(0).hasNonNull("registrationEffects"),cmd);
        }
        var out=Path.of("target/recall-cics");Files.createDirectories(out);
        Files.writeString(out.resolve("registration.json"),j.toPrettyString());
    }
    @Test void retrieveCompletionDoesNotDependOnMissingHostDeclaration()throws Exception {
        var j=CicsCommandContractTest.one("RETRIEVE INTO(MISSING-AREA) NOHANDLE");
        var c=CicsCommandContractTest.command(j);assertEquals("RETRIEVE",c.path("commandKind").asText());
        assertTrue(CicsCommandContractTest.roles(j).contains("NORMAL:normal"));
        assertFalse(c.hasNonNull("hostEffects"),"missing storage cannot create executable memory proof");
    }
    @Test void missingOrUnclassifiedMemoryDoesNotAcquireAProof()throws Exception {
        for(var text:java.util.List.of("RECEIVE MAP('M') RESP(RC)","SEND MAP('M') RESP(RC)","RECEIVE MAP('M') INTO(WS-AREA(1:2)) RESP(RC)","SYNCPOINT ROLLBACK"))
            assertFalse(CicsCommandContractTest.command(CicsCommandContractTest.one(text)).hasNonNull("hostEffects"),text);
    }
}
