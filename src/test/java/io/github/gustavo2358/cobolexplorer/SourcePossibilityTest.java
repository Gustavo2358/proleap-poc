package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourcePossibilityTest {
    private static JsonNode product(String body) throws Exception {
        return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(ControlTopologyAuthorityTest.publish(body)));
    }
    // Dynamic SQL remains outside W5: retain the unknown-control oracle after static UPDATE is admitted.
    @Test void unknownEmbeddedCompletionHasOnlySourcePossibility() throws Exception {
        var p=product("EXEC SQL EXECUTE IMMEDIATE SQL-TEXT END-EXEC\nCALL 'AFTERIO'\nGOBACK.\n");
        var t=p.path("controlTopology");
        assertEquals("2.65.0",p.path("contractVersion").asText());
        assertEquals(1,t.path("sourceContinuations").size());
        var c=t.path("sourceContinuations").get(0);
        assertEquals("OCCURRENCE",c.path("target").path("kind").asText());
        var statement=c.path("statement").asText();
        for(var o:t.path("outcomes"))if(o.path("statement").asText().equals(statement))
            assertEquals("UNKNOWN_LOCAL",o.path("kind").asText(),"source hypothesis must not become executable completion");
        assertTrue(t.path("proofs").findValuesAsText("kind").contains("CONTROL_POSSIBILITY"));
    }
    @Test void terminalAndOrdinaryStatementsDoNotGainHypotheses() throws Exception {
        for(var body:new String[]{"GOBACK.\nCALL 'DEAD'.\n","GO TO DONE.\nCALL 'DEAD'.\nDONE.\nGOBACK.\n","MOVE 'X' TO PGM\nCALL PGM\nGOBACK.\n"})
            assertEquals(0,product(body).path("controlTopology").path("sourceContinuations").size());
    }
    @Test void hypotheticalCompletionAtParagraphBoundaryIsSymbolic() throws Exception {
        var p=product("PERFORM P\nCALL 'AFTERIO'\nGOBACK.\nP.\nEXEC SQL EXECUTE IMMEDIATE SQL-TEXT END-EXEC.\n");
        var c=p.path("controlTopology").path("sourceContinuations");
        assertEquals(1,c.size());assertEquals("COMPLETE",c.get(0).path("target").path("kind").asText());
    }

    @Test void possibilityProofCannotAuthorizeAnyExecutableSlot() throws Exception {
        var t=(com.fasterxml.jackson.databind.node.ObjectNode)product("PERFORM P\nGOBACK.\nP.\nEXEC SQL EXECUTE IMMEDIATE SQL-TEXT END-EXEC.\n").path("controlTopology");
        var hypothesis=t.path("proofs").findValues("kind");
        String proof=null;for(var p:t.path("proofs"))if(p.path("kind").asText().equals("CONTROL_POSSIBILITY"))proof=p.path("id").asText();
        assertNotNull(proof);
        for(var slot:new String[]{"occurrences","regions","boundaries","outcomes","bindings"}) {
            var copy=t.deepCopy();assertFalse(copy.path(slot).isEmpty());
            ((com.fasterxml.jackson.databind.node.ObjectNode)copy.path(slot).get(0)).putArray("proofs").add(proof);
            assertThrows(Exception.class,()->new ObjectMapper().treeToValue(copy,io.github.gustavo2358.cobolexplorer.semanticproduct.ControlTopology.class),slot);
        }
        var target=t.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)target.path("regions").get(0).path("entry")).putArray("proofs").add(proof);
        assertThrows(Exception.class,()->new ObjectMapper().treeToValue(target,io.github.gustavo2358.cobolexplorer.semanticproduct.ControlTopology.class));
    }

    @Test void unmodeledControlMutationRequiresItsOwnSourcePrerequisite() throws Exception {
        var p=product("ALTER ROUTE TO PROCEED TO DEST\nGO TO ROUTE.\nROUTE.\nGO TO ROUTE.\nDEST.\nCALL 'POSSIBLE'\nGOBACK.\n");
        var t=p.path("controlTopology");assertFalse(t.path("sourceContinuations").isEmpty());
        for(var c:t.path("sourceContinuations")) {
            assertEquals(1,c.path("prerequisites").size());assertEquals("REGION_ENTRY",c.path("target").path("kind").asText());
            var owner=c.path("statement").asText();
            assertTrue(java.util.stream.StreamSupport.stream(t.path("outcomes").spliterator(),false).anyMatch(o->o.path("statement").asText().equals(owner)&&o.path("kind").asText().equals("EXPLICIT_TRANSFER")));
        }
        // The ordinary GO TO target still has its original resolved proof; no executable edge is synthesized.
        for(var o:t.path("outcomes"))if(o.path("kind").asText().equals("EXPLICIT_TRANSFER"))
            assertEquals(1,o.path("target").path("proofs").size());
    }
}
