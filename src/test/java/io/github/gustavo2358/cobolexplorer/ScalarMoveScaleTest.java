package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScalarMoveScaleTest {
    record Observation(String probe, long physicalLines, int declarations, int moves,
                       ScalarMoveSemantics.Metrics metrics, long jsonBytes, long elapsedMillis) { }
    @Test void physicalLinesDoNotExpandSemanticIdentitiesOrPublication() throws Exception {
        String small = Files.readString(ScalarMoveCheckpoint4ATest.FIXTURE);
        String source = small.replace("       PROCEDURE DIVISION.",
                "      * irrelevant physical line\n".repeat(60_000) + "       PROCEDURE DIVISION.");
        var observation = run("physical", source, 1, 1);
        assertTrue(observation.physicalLines() >= 60_000);
        assertTrue(observation.jsonBytes() < 8_000, "physical text must not expand semantic publication");
        assertTrue(observation.metrics().nodeVisits() < 100);
    }
    @Test void declarationsAndReferencesHaveLinearVisitsAndCardinality() throws Exception {
        Observation previous = null;
        for (int n : List.of(1500, 3000)) {
            StringBuilder declarations = new StringBuilder();
            StringBuilder moves = new StringBuilder();
            for (int i = 0; i < n; i++) {
                declarations.append("01 WS-").append(i).append(" PIC X(5).\n");
                moves.append("MOVE 'PROGA' TO WS-").append(i).append(".\n");
            }
            moves.append("GOBACK.");
            var current = run("facts-" + n, ScalarMoveCheckpoint4ATest.program(declarations.toString(), moves.toString()), n, n);
            assertEquals(n, current.metrics().scalarLookups());
            assertEquals(n, current.metrics().referenceVisits());
            assertEquals(n, current.metrics().moveVisits());
            assertTrue(current.metrics().declarationVisits() <= n + 2);
            assertTrue(current.metrics().nodeVisits() <= 12L * n + 20);
            assertTrue(current.jsonBytes() < 7_000L * n);
            if (previous != null) {
                assertTrue(current.metrics().nodeVisits() <= 2 * previous.metrics().nodeVisits());
                assertTrue(current.jsonBytes() < 2.1 * previous.jsonBytes());
            }
            previous = current;
        }
    }
    @Test void tenThousandTargetsShareOneDataDeclaration() throws Exception {
        var observation = run("shared-data", ScalarMoveCheckpoint4ATest.program("01 WS-X PIC X(5).",
                "MOVE 'PROGA' TO WS-X.\n".repeat(10_000) + "GOBACK."), 1, 10_000);
        assertEquals(10_000, observation.metrics().scalarLookups());
        assertEquals(10_000, observation.metrics().moveVisits());
        assertTrue(observation.metrics().nodeVisits() < 6L * 10_000 + 30);
    }
    private static Observation run(String label, String source, int dataCount, int moveCount) throws Exception {
        long started = System.nanoTime();
        var analysis = AstBoundaryTestSupport.analyze(source, "scale.cbl");
        var products = ScalarMoveCheckpoint4ATest.products(analysis);
        CobolSemanticPort port = CobolSemanticProductProjector.open(products, analysis.model().programUnits().get(0).id());
        assertEquals(dataCount, port.dataDeclarations().size(), "declarations must not be copied per reference");
        assertEquals(moveCount, port.moves().size());
        assertEquals(moveCount + 1, port.statements().size());
        assertEquals(dataCount, new HashSet<>(port.dataDeclarations().stream().map(DataDeclaration::id).toList()).size());
        assertTrue(port.dataDeclarations().stream().allMatch(d -> d.scalarText().isPresent()));
        Set<DataItemId> referenced = new HashSet<>();
        for (var move : port.moves()) {
            assertEquals(CopySemantics.FULL_IDENTITY, move.copySemantics());
            assertEquals(ContinuationAvailability.KNOWN, move.normalContinuation().availability());
            referenced.add(move.target().wholeItemAccess().orElseThrow().data());
        }
        assertEquals(dataCount, referenced.size());
        Path output = Path.of("target/checkpoint-4a"); Files.createDirectories(output);
        Path json = output.resolve(label + ".semantic-product.json");
        SemanticProductJsonWriter.write(port, json);
        assertEquals(moveCount, new ObjectMapper().readTree(json.toFile()).path("statements").size() - 1);
        var observation = new Observation(label, source.lines().count(), dataCount, moveCount,
                products.scalarMoves().metrics(), Files.size(json), (System.nanoTime() - started) / 1_000_000);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve(label + ".metrics.json").toFile(), observation);
        System.out.println("CP4A scale " + new ObjectMapper().writeValueAsString(observation));
        return observation;
    }
}
