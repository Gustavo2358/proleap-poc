package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** AST construction products kept separate and namespaced for every program unit. */
public record CompilationUnitBuildResult(
        CompilationUnitModel compilationUnit,
        Map<ResolutionContracts.ProgramUnitId, SemanticCoverage.Report> coverageByProgramUnit,
        Map<ResolutionContracts.ProgramUnitId, List<SemanticCoverage.Diagnostic>> diagnosticsByProgramUnit) {
    public CompilationUnitBuildResult {
        compilationUnit = Objects.requireNonNull(compilationUnit, "compilationUnit");
        coverageByProgramUnit = immutableMapOfValues(coverageByProgramUnit);
        LinkedHashMap<ResolutionContracts.ProgramUnitId, List<SemanticCoverage.Diagnostic>> diagnostics =
                new LinkedHashMap<>();
        diagnosticsByProgramUnit.forEach((id, value) -> diagnostics.put(id, List.copyOf(value)));
        diagnosticsByProgramUnit = Collections.unmodifiableMap(diagnostics);
        Set<ResolutionContracts.ProgramUnitId> expected = new LinkedHashSet<>(compilationUnit.programUnits()
                .stream().map(CompilationUnitModel.ProgramUnit::id).toList());
        if (!coverageByProgramUnit.keySet().equals(expected)
                || !diagnosticsByProgramUnit.keySet().equals(expected))
            throw new IllegalArgumentException("each program unit must have coverage and diagnostics products");
    }

    private static <T> Map<ResolutionContracts.ProgramUnitId, T> immutableMapOfValues(
            Map<ResolutionContracts.ProgramUnitId, T> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
