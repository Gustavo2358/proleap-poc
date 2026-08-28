package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Immutable, complete AST inventory for one parsed COBOL compilation unit. */
public final class CompilationUnitModel {
    public record ProgramUnit(ResolutionContracts.ProgramUnitId id,
                              ResolutionContracts.ProgramUnitId parentId,
                              Ast.Program program) {
        public ProgramUnit {
            id = Objects.requireNonNull(id, "id");
            program = Objects.requireNonNull(program, "program");
        }
    }

    private final String compilationUnitId;
    private final List<ProgramUnit> programUnits;
    private final Map<ResolutionContracts.ProgramUnitId, ProgramUnit> byId;

    public CompilationUnitModel(String compilationUnitId, List<ProgramUnit> programUnits) {
        if (compilationUnitId == null || compilationUnitId.isBlank())
            throw new IllegalArgumentException("compilationUnitId must not be blank");
        this.compilationUnitId = compilationUnitId;
        this.programUnits = List.copyOf(programUnits);
        LinkedHashMap<ResolutionContracts.ProgramUnitId, ProgramUnit> index = new LinkedHashMap<>();
        for (ProgramUnit unit : this.programUnits) {
            if (!unit.id().compilationUnitId().equals(compilationUnitId))
                throw new IllegalArgumentException("program unit belongs to a different compilation unit");
            if (index.put(unit.id(), unit) != null)
                throw new IllegalArgumentException("duplicate ProgramUnitId " + unit.id());
            if (unit.parentId() != null && !index.containsKey(unit.parentId()))
                throw new IllegalArgumentException("parent program must precede nested program");
        }
        this.byId = Collections.unmodifiableMap(index);
    }

    public String compilationUnitId() { return compilationUnitId; }
    public List<ProgramUnit> programUnits() { return programUnits; }
    public Optional<ProgramUnit> find(ResolutionContracts.ProgramUnitId id) {
        return Optional.ofNullable(byId.get(id));
    }
}
