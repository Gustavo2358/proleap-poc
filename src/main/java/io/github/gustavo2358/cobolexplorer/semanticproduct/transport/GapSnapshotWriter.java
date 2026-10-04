package io.github.gustavo2358.cobolexplorer.semanticproduct.transport;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/** Browser projection of the same gaps published in each Semantic Product. No reconciliation. */
public final class GapSnapshotWriter {
    private GapSnapshotWriter() { }
    public static void write(List<CobolSemanticPort> products, Path javascript) throws IOException {
        var mapper = new JsonMapper(); var root = mapper.createObjectNode(); var units = root.putArray("units");
        for (var product : products) units.add(mapper.valueToTree(SemanticProductJsonWriter.gapSnapshot(product)));
        Files.writeString(javascript, "window.SEMANTIC_GAPS=" + mapper.writeValueAsString(root) + ";\n", StandardCharsets.UTF_8);
    }
}
