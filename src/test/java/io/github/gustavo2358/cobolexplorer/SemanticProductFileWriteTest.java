package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Small fixtures may materialize bytes to compare both public transport paths. */
class SemanticProductFileWriteTest {
    @Test
    void filePublicationIsByteIdenticalToSerializationAndTruncatesPreviousOutput(@TempDir Path directory)
            throws Exception {
        List<String> sources = List.of(
                Files.readString(ScalarMoveCheckpoint4ATest.FIXTURE),
                Files.readString(Path.of("src/test/resources/cobol/semantic/semantic-product-entry-goback.cbl")),
                ScalarMoveCheckpoint4ATest.program("01 WS-X PIC X(5).", "MOVE 'ABC' TO WS-X."),
                ScalarMoveCheckpoint4ATest.program("01 WS-X PIC X(5).", "MOVE 123 TO WS-X.\nGOBACK."));
        Path destination = directory.resolve("semantic-product.json");
        for (String source : sources) {
            var port = ScalarMoveCheckpoint4ATest.publish(source);
            byte[] expected = SemanticProductJsonWriter.serialize(port);
            Files.writeString(destination, "obsolete trailing bytes\n".repeat(expected.length));
            SemanticProductJsonWriter.write(port, destination);
            assertArrayEquals(expected, Files.readAllBytes(destination));
            SemanticProductJsonWriter.write(port, destination);
            assertArrayEquals(expected, Files.readAllBytes(destination), "repeated file publication must be deterministic");
        }
    }
}
