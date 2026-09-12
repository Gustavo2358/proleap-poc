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
            assertTrue(port.moves().stream().allMatch(m -> m.normalContinuation().availability() == ContinuationAvailability.KNOWN));
            assertEquals(n >= 4 && n <= 6 ? 1 : 0, port.performs().size());
            for (var p : port.performs()) {
                var members = new HashSet<>(p.primaryStatements()); members.addAll(p.targetStatements());
                for (var s : port.statements()) if (s.header().containment().parent().isPresent()) assertTrue(members.add(s.header().id()));
                assertEquals(port.statements().size(), members.size());
                assertEquals(p.primaryStatements().get(0), port.entries().get(0).start().statement().orElseThrow());
                assertTrue(p.primaryStatements().contains(p.normalContinuation().statement().orElseThrow()));
                var last = (MoveFact) port.statement(p.targetExit().orElseThrow()).orElseThrow();
                assertEquals(p.normalContinuation().statement(), last.normalContinuation().statement());
            }
            var bytes = SemanticProductJsonWriter.serialize(port);
            assertEquals("1.7.0", new ObjectMapper().readTree(bytes).path("contractVersion").asText());
            assertArrayEquals(bytes, SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source(n))));
            var out = Path.of("target/multi-call"); Files.createDirectories(out); Files.write(out.resolve("fixture-" + n + ".json"), bytes);
        }
    }
    @Test void performResumesAtEachSupportedRootKind() {
        for (String resume : List.of("MOVE 'PROGA' TO WS-A.", "CALL 'PROGB'.", "IF WS-A = 'X'\n MOVE 'PROGA' TO WS-PGM\nELSE\n MOVE 'PROGB' TO WS-PGM\nEND-IF.", "GOBACK.")) {
            var source = PerformDiscoveryTest.source("", "PERFORM DEFINE-PGM.", "MOVE 'PROGA' TO WS-PGM.");
            source = source.replace("CALL WS-PGM.\nGOBACK.", resume + (resume.equals("GOBACK.") ? "" : "\nGOBACK."));
            var port = ScalarMoveCheckpoint4ATest.publish(source); assertEquals(1, port.performs().size(), resume);
            var p = port.performs().get(0); assertEquals(p.primaryStatements().get(1), p.normalContinuation().statement().orElseThrow());
        }
    }
    @Test void extraPerformAndUnsupportedRootRemainObserved() throws Exception {
        for (String root : List.of("PERFORM DEFINE-A.", "GO TO DEFINE-A.", "CONTINUE.", "ENTRY 'OTHER'.", "EVALUATE FLAG WHEN 'Y' MOVE 'PROGA' TO WS-A END-EVALUATE.")) {
            var port = ScalarMoveCheckpoint4ATest.publish(source(5).replace("       CALL WS-A.", "       " + root + "\n       CALL WS-A."));
            assertTrue(port.performs().isEmpty(), root); assertFalse(port.observedStatements().isEmpty(), root);
        }
    }
}
