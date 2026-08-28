package io.github.gustavo2358.cobolexplorer;

import java.util.List;
import java.util.Objects;

/** Separate immutable products of semantic AST construction. */
public record AstBuildResult(Ast.Program program, SemanticCoverage.Report coverage,
                             List<SemanticCoverage.Diagnostic> diagnostics) {
    public AstBuildResult {
        program = Objects.requireNonNull(program, "program");
        coverage = Objects.requireNonNull(coverage, "coverage");
        diagnostics = List.copyOf(diagnostics);
    }
}
