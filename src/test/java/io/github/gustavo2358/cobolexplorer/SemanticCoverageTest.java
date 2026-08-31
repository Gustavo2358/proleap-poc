package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticCoverageTest {

    @Test
    void keepsFindingsImmutableAndComputesCompletenessConservatively() {
        List<SemanticCoverage.Finding> mutable = new ArrayList<>();
        mutable.add(finding(0, SemanticCoverage.ConstructionCoverage.MODELED,
                SemanticCoverage.DependencyKnowledge.REFERENCE_READY));
        SemanticCoverage.Report complete = new SemanticCoverage.Report(mutable);
        mutable.add(finding(1, SemanticCoverage.ConstructionCoverage.UNSUPPORTED,
                SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN));

        assertEquals(1, complete.findings().size());
        assertTrue(complete.dependencyCoverageComplete());
        assertThrows(UnsupportedOperationException.class,
                () -> complete.findings().add(finding(2, SemanticCoverage.ConstructionCoverage.MODELED,
                        SemanticCoverage.DependencyKnowledge.NOT_DEPENDENCY_BEARING)));

        for (SemanticCoverage.ConstructionCoverage coverage : List.of(
                SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED,
                SemanticCoverage.ConstructionCoverage.UNSUPPORTED,
                SemanticCoverage.ConstructionCoverage.INPUT_MISSING)) {
            SemanticCoverage.Report incomplete = new SemanticCoverage.Report(List.of(
                    finding(0, coverage, SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN)));
            assertFalse(incomplete.dependencyCoverageComplete(), coverage.name());
        }
    }

    @Test
    void keepsBuildResultAndDiagnosticsSeparateFromTheAst() {
        Ast.Meta meta = meta(0);
        Ast.Program program = new Ast.Program(meta, "SAMPLE", List.of());
        SemanticCoverage.Report report = new SemanticCoverage.Report(List.of());
        List<SemanticCoverage.Diagnostic> mutable = new ArrayList<>();
        mutable.add(new SemanticCoverage.Diagnostic("SEM001", "message", meta));

        AstBuildResult result = new AstBuildResult(program, report, mutable);
        mutable.clear();

        assertEquals(program, result.program());
        assertEquals(report, result.coverage());
        assertEquals(1, result.diagnostics().size());
        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    }

    @Test
    void rejectsDuplicateConcreteFindingsForTheSameAstNode() {
        SemanticCoverage.Finding first = new SemanticCoverage.Finding(0, "statement", meta(0),
                "SAMPLE", SemanticCoverage.ConstructionCoverage.MODELED,
                SemanticCoverage.DependencyKnowledge.REFERENCE_READY, "concrete", 0);
        SemanticCoverage.Finding duplicate = new SemanticCoverage.Finding(1, "wrapper", first.meta(),
                "SAMPLE", SemanticCoverage.ConstructionCoverage.MODELED,
                SemanticCoverage.DependencyKnowledge.REFERENCE_READY, "duplicate wrapper", 0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new SemanticCoverage.Report(List.of(first, duplicate)));
        assertTrue(exception.getMessage().contains("exactly one coverage finding"));
    }

    private static SemanticCoverage.Finding finding(
            int id, SemanticCoverage.ConstructionCoverage coverage,
            SemanticCoverage.DependencyKnowledge knowledge) {
        return new SemanticCoverage.Finding(id, "statement", meta(id), "SAMPLE",
                coverage, knowledge, "characterization", -1);
    }

    private static Ast.Meta meta(int id) {
        return new Ast.Meta(id, new Ast.SourceSpan(1, 0, 1, 5, 0, 0),
                new Ast.ParseTreeOrigin(0, "statement", 1));
    }
}
