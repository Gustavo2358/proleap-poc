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
                new Ast.ParseTreeOrigin(7, "programUnit", 2),
                new Ast.SourceProvenance(new Ast.SourceLocation("sample.cbl", 1, 0, 1, 3),
                        new Ast.SourceLocation("sample.cbl", 1, 0, 1, 3), List.of(), true));
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

    @Test
    void serializesEachPreservedSemanticBoundaryExactlyOnce() throws Exception {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyzeFixture();
        CompilationUnitModel.ProgramUnit parent = analysis.model().programUnits().get(0);
        SemanticCoverage.Report report = analysis.build().coverageByProgramUnit().get(parent.id());
        Path output = Files.createTempFile("coverage-boundaries", ".js");

        CoverageSnapshot.from("ast-cfg-boundary.cbl", parent.program(), report, 0, 0, 0)
                .write(output);
        String text = Files.readString(output, StandardCharsets.UTF_8);

        assertEquals(1, occurrences(text, "\"rule\":\"dataBlankWhenZeroClause\""));
        assertEquals(0, occurrences(text, "\"rule\":\"abbreviation\""),
                "the modeled condition surface is no longer a preserved coverage boundary");
    }

    private static int occurrences(String text, String expected) {
        return (text.length() - text.replace(expected, "").length()) / expected.length();
    }
}
