package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CicsCompletionTest {
    @Test void rollbackHasIndependentCommandAndPossibleCompletion()throws Exception {
        for(var suffix:List.of("", " NOHANDLE", " RESP(RC)", " RESP2(RC2)")) {
            var j=CicsCommandContractTest.one("SYNCPOINT ROLLBACK"+suffix);
            var c=CicsCommandContractTest.command(j);
            assertEquals("2.65.0",j.path("contractVersion").asText());
            assertEquals("SYNCPOINT_ROLLBACK",c.path("commandKind").asText());
            assertEquals("SUPPORTED",c.path("syntaxStatus").asText());
            assertTrue(c.hasNonNull("hostEffects"));
            var roles=CicsCommandContractTest.roles(j);
            assertTrue(roles.contains("NORMAL:normal"));
            assertEquals(suffix.isEmpty()||suffix.startsWith(" RESP2"),roles.contains("UNKNOWN_LOCAL:cics/handler-or-default-condition"));
            var out=java.nio.file.Path.of("target/cics-completion");java.nio.file.Files.createDirectories(out);
            java.nio.file.Files.writeString(out.resolve(suffix.isEmpty()?"rollback.json":"rollback-"+suffix.strip().replaceAll("[^A-Z0-9]","_")+".json"),j.toPrettyString());
        }
    }
    @Test void rollbackDoesNotAdmitMalformedOrUnknownOptions()throws Exception {
        for(var cmd:List.of("SYNCPOINT ROLLBACK(RC)","SYNCPOINT ROLLBACK ROLLBACK","SYNCPOINT ROLLBACK MYSTERY")) {
            var j=CicsCommandContractTest.one(cmd);var c=CicsCommandContractTest.command(j);
            assertEquals("UNAVAILABLE",c.path("syntaxStatus").asText());assertFalse(c.hasNonNull("hostEffects"));
            assertTrue(CicsCommandContractTest.outcomes(j,c).stream().allMatch(o->o.path("kind").asText().equals("UNKNOWN_LOCAL")));
        }
    }
    @Test void cicsReturnExitsProgramAndOnlyErrorsMayContinueLocally()throws Exception {
        for(var cmd:List.of("RETURN","RETURN TRANSID('NEXT')", "RETURN COMMAREA(WS-AREA) LENGTH(8)")) {
            var j=CicsCommandContractTest.one(cmd);var c=CicsCommandContractTest.command(j);
            assertEquals("RETURN",c.path("commandKind").asText());assertEquals("2.65.0",j.path("contractVersion").asText());
            assertTrue(c.hasNonNull("hostEffects"));assertTrue(CicsCommandContractTest.roles(j).contains("PROGRAM_RETURN:return"));
            assertFalse(CicsCommandContractTest.roles(j).contains("NORMAL:normal"));
        }
        for(var suffix:List.of(" RESP(RC)", " NOHANDLE", " RESP2(RC2)")) {
            var j=CicsCommandContractTest.one("RETURN TRANSID('NEXT')"+suffix);
            assertTrue(CicsCommandContractTest.roles(j).contains("PROGRAM_RETURN:return"));
            assertEquals(!suffix.startsWith(" RESP2"),CicsCommandContractTest.roles(j).contains("NORMAL:condition-return"));
        }
        assertEquals(Set.of("PROGRAM_RETURN:return"),CicsCommandContractTest.roles(CicsCommandContractTest.one("RETURN NOHANDLE")));
    }
    @Test void returnRejectsUnsupportedAndInvalidOptionCombinations()throws Exception {
        for(var cmd:List.of("RETURN MYSTERY","RETURN LENGTH(8)","RETURN IMMEDIATE","RETURN COMMAREA(WS-AREA) CHANNEL('C')")) {
            var j=CicsCommandContractTest.one(cmd);assertEquals("UNAVAILABLE",CicsCommandContractTest.command(j).path("syntaxStatus").asText());
            assertTrue(CicsCommandContractTest.roles(j).stream().allMatch(r->r.startsWith("UNKNOWN_LOCAL")));
        }
    }
}
