package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

class MultiCallProgramTest {
    static String source(int n) throws Exception { return Files.readString(Path.of("src/test/resources/multi-call/fixture-" + n + ".cbl")); }
    @Test void sevenProgramsPublishEveryTypedFactAndClosedContinuations() throws Exception {
        for (int n = 1; n <= 7; n++) {
            var port = ScalarMoveCheckpoint4ATest.publish(source(n));
            assertEquals(n == 2 || n == 6 ? 2 : 3, port.calls().size());
            assertTrue(port.observedStatements().isEmpty(), "fixture " + n);
            assertTrue(port.statements().stream().allMatch(s -> s.header().coverage() == CoverageStatus.MODELED));
            assertTrue(port.calls().stream().allMatch(c -> c.normalContinuation().availability() == ContinuationAvailability.KNOWN));
            var exits = port.performs().stream().flatMap(p -> p.targetExit().stream()).collect(java.util.stream.Collectors.toSet());
            assertTrue(port.moves().stream().filter(m -> !exits.contains(m.header().id())).allMatch(m -> m.normalContinuation().availability() == ContinuationAvailability.KNOWN));
            assertEquals(n >= 4 && n <= 6 ? 1 : 0, port.performs().size());
            for (var p : port.performs()) {
                assertTrue(p.primaryStatements().isEmpty());
                assertFalse(p.targetStatements().contains(port.entries().get(0).start().statement().orElseThrow()));
                assertFalse(p.targetStatements().contains(p.normalContinuation().statement().orElseThrow()));
                var last = (MoveFact) port.statement(p.targetExit().orElseThrow()).orElseThrow();
                assertEquals(Optional.empty(), last.normalContinuation().statement());
            }
            var bytes = SemanticProductJsonWriter.serialize(port);
            assertEquals("2.0.0", new ObjectMapper().readTree(bytes).path("contractVersion").asText());
            assertArrayEquals(bytes, SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source(n))));
            var out = Path.of("target/multi-call"); Files.createDirectories(out); Files.write(out.resolve("fixture-" + n + ".json"), bytes);
        }
    }
    @Test void performResumesAtEachSupportedRootKind() {
        for (String resume : List.of("MOVE 'PROGA' TO WS-A.", "CALL 'PROGB'.", "IF WS-A = 'X'\n MOVE 'PROGA' TO WS-PGM\nELSE\n MOVE 'PROGB' TO WS-PGM\nEND-IF.", "GOBACK.")) {
            var source = PerformDiscoveryTest.source("", "PERFORM DEFINE-PGM.", "MOVE 'PROGA' TO WS-PGM.");
            source = source.replace("CALL WS-PGM.\nGOBACK.", resume + (resume.equals("GOBACK.") ? "" : "\nGOBACK."));
            var port = ScalarMoveCheckpoint4ATest.publish(source); assertEquals(1, port.performs().size(), resume);
            var p = port.performs().get(0); assertNotEquals(p.header().id(), p.normalContinuation().statement().orElseThrow());
        }
    }
    @Test void extraPerformAndUnsupportedRootRemainObserved() throws Exception {
        for (String root : List.of("GO TO DEFINE-A.", "CONTINUE.", "ENTRY 'OTHER'.", "EVALUATE TRUE WHEN TRUE MOVE 'PROGA' TO WS-A END-EVALUATE.")) {
            var port = ScalarMoveCheckpoint4ATest.publish(source(5).replace("       CALL WS-A.", "       " + root + "\n       CALL WS-A."));
            assertTrue(port.performs().isEmpty(), root); assertFalse(port.observedStatements().isEmpty(), root);
        }
    }
}
