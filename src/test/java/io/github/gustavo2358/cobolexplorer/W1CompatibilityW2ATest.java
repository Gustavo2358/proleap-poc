package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Frozen W1 byte-derived facts survive the explicitly versioned additive envelope. */
class W1CompatibilityW2ATest {
    @Test void historicalW1FactsAreUnchangedExceptVersionAndNewUnavailableStorageFact() throws Exception {
        var json = new ObjectMapper();
        for (String name : List.of("dynamic-x8", "literal", "cp5")) {
            Path source = name.equals("cp5") ? Path.of("src/test/resources/cobol/semantic/AIR-MOVE.cbl")
                    : Path.of("docs/work/evidence/WORK-AST-005/red/" + name + ".cbl");
            Path golden = Path.of("docs/work/evidence/WORK-AST-005/cli/" + (name.equals("cp5") ? "cp5" : name + "-1") + ".json");
            byte[] frozen = Files.readAllBytes(golden);
            var old = (ObjectNode) json.readTree(frozen);
            var a = AstBoundaryTestSupport.analyze(Files.readString(source), source.getFileName().toString());
            var port = io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.open(
                    ScalarMoveCheckpoint4ATest.products(a), a.model().programUnits().get(0).id());
            byte[] currentBytes = SemanticProductJsonWriter.serialize(port);
            var current = (ObjectNode) json.readTree(currentBytes);
            assertEquals("1.3.0", old.path("contractVersion").asText());
            assertEquals("1.8.0", current.path("contractVersion").asText());
            assertEquals("UNAVAILABLE", current.path("storageIndependence").path("availability").asText());
            assertTrue(current.path("storageIndependence").path("members").isEmpty());
            for (var statement : current.path("statements")) if (statement.path("variant").asText().equals("MOVE")) {
                assertEquals("LITERAL", statement.path("source").path("variant").asText());
                ((ObjectNode) statement.path("source")).remove("variant");
            }
            current.remove("storageIndependence"); current.remove("contractVersion"); old.remove("contractVersion");
            assertEquals(old, current, name + ": all W1 facts, IDs, bindings, origins, provenance, fitting, gaps and readiness must match");
            assertArrayEquals(frozen, Files.readAllBytes(golden));
            Path output = Path.of("target/cp6-w2a/w1-compatibility"); Files.createDirectories(output);
            Files.write(output.resolve(name + "-1.5.0.json"), currentBytes);
        }
    }
}
