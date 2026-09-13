package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConditionalGoToTest {
    static String source(String body) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. DEPENDTEST.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n"
            + "01 WS-IDX PIC 9(4).\n01 WS-PGM PIC X(8).\n01 FLAG PIC X.\nPROCEDURE DIVISION.\nMAIN-PARA.\n"+body;
    }
    static String capacity(int n) {
        var s=new StringBuilder("GO TO\n");
        for(int i=1;i<=n;i++)s.append("P-").append(i).append('\n');
        s.append("DEPENDING ON WS-IDX.\nMOVE 'FALLPGM' TO WS-PGM.\nGO TO JOIN-PARA.\n");
        for(int i=1;i<=n;i++)s.append("P-").append(i).append(".\nGO TO JOIN-PARA.\n");
        return source(s+"JOIN-PARA.\nCALL WS-PGM.\nGOBACK.\n");
    }
    static JsonNode wire(String source) throws Exception {
        return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)));
    }
    static JsonNode conditional(JsonNode p) {
        for(var s:p.path("statements"))if(s.path("variant").asText().equals("GO_TO_DEPENDING_ON"))return s;
        fail("typed conditional GO TO must survive SP");return null;
    }
    @Test void finiteMultiplicity() throws Exception {
        var out=Path.of("target/goto-depending");Files.createDirectories(out);
        for(int n:new int[]{1,2,5,40,100,200,255,256}) {
            var source=capacity(n);var p=wire(source);var g=conditional(p);
            assertEquals(n,g.path("destinations").size());
            for(int i=0;i<n;i++) {
                var d=g.path("destinations").get(i);assertEquals(i,d.path("ordinal").asInt(-1));
                assertTrue(d.path("target").isTextual());assertTrue(d.path("targetEntry").isTextual());
                assertTrue(d.path("gapCodes").isEmpty());
            }
            assertTrue(g.path("selectorInteger").asBoolean());
            assertEquals("READ",g.path("selector").path("role").asText());
            assertEquals("KNOWN",g.path("normalContinuation").path("availability").asText());
            assertEquals(n>255,!g.path("gapCodes").isEmpty());
            assertEquals(p,wire(source));
            Files.writeString(out.resolve("capacity-"+n+".json"),p.toPrettyString()+"\n");
        }
    }
    @Test void indexedResolutionAndOccurrenceMultiplicity() {
        for(int n:new int[]{1,2,5,40,100,200,255}) {
            var a=AstBoundaryTestSupport.analyze(capacity(n),"capacity.cbl");
            var semantics=ScalarMoveCheckpoint4ATest.products(a).scalarMoves();
            assertEquals(2L*n+1,semantics.goTos().resolutionAttempts(),"one indexed resolution attempt per observed reference, including simple jumps");
        }
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(10),()->ScalarMoveCheckpoint4ATest.publish(capacity(255)),"hundreds of unique targets are ordinary inputs");
        for(int n:new int[]{1,2,5,40}) {
            var body="GO TO A B DEPENDING ON WS-IDX.\n".repeat(n)+"GOBACK.\nA.\nGOBACK.\nB.\nGOBACK.\n";
            var p=ScalarMoveCheckpoint4ATest.publish(source(body));assertEquals(n,p.conditionalGoTos().size());
            for(var g:p.conditionalGoTos()){assertTrue(g.gapCodes().isEmpty());assertEquals(2,g.destinations().size());}
        }
    }
    @Test void orderDuplicatesAndPartialTargets() throws Exception {
        var g=conditional(wire(source("GO TO C A B A UNKNOWN DEPENDING ON WS-IDX.\nGOBACK.\nA.\nGOBACK.\nB.\nGOBACK.\nC.\nGOBACK.\n")));
        var d=g.path("destinations");assertEquals(5,d.size());assertEquals(d.get(1).path("target"),d.get(3).path("target"));
        for(int i=0;i<4;i++)assertTrue(d.get(i).path("targetEntry").isTextual());
        assertTrue(d.get(4).path("target").isNull());assertFalse(d.get(4).path("gapCodes").isEmpty());
        assertEquals("KNOWN",g.path("normalContinuation").path("availability").asText());
    }
}
