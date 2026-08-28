package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoverageSnapshotTest {
    @Test
    void writesDeterministicConservativeBrowserSnapshot() throws Exception {
        Ast.Meta meta = new Ast.Meta(0, new Ast.SourceSpan(1, 0, 1, 3, 0, 0),
                new Ast.ParseTreeOrigin(7, "programUnit", 2));
        Ast.Program program = new Ast.Program(meta, "SAMPLE", List.of());
        SemanticCoverage.Report report = new SemanticCoverage.Report(List.of(
                new SemanticCoverage.Finding(0, "sortStatement", meta, "SORT S",
                        SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED,
                        SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN,
                        "deferred", 0)));
        CoverageSnapshot snapshot = CoverageSnapshot.from("sample.cbl", program, report, 1, 0, 0);
        assertFalse(snapshot.dependencyCoverageComplete());
        Path first = Files.createTempFile("coverage-one", ".js");
        Path second = Files.createTempFile("coverage-two", ".js");
        snapshot.write(first); snapshot.write(second);
        String text = Files.readString(first, StandardCharsets.UTF_8);
        assertEquals(text, Files.readString(second, StandardCharsets.UTF_8));
        assertTrue(text.contains("\"complete\":false"));
        assertTrue(text.contains("COPY(s) ausente(s)"));
        assertTrue(text.contains("\"ast\":0"));
        assertTrue(text.contains("\"parse\":7"));
    }
}
