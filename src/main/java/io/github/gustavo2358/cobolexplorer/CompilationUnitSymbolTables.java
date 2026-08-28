package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Declaration tables namespaced by program unit; this is not a binding result. */
public final class CompilationUnitSymbolTables {
    public record UnitSymbols(ResolutionContracts.ProgramUnitId id,
                              ResolutionContracts.ProgramUnitId parentId,
                              SymbolTable symbolTable) {
        public UnitSymbols {
            id = Objects.requireNonNull(id, "id");
            symbolTable = Objects.requireNonNull(symbolTable, "symbolTable");
        }
    }

    private final List<UnitSymbols> units;
    private final Map<ResolutionContracts.ProgramUnitId, UnitSymbols> byId;

    CompilationUnitSymbolTables(List<UnitSymbols> units) {
        this.units = List.copyOf(units);
        LinkedHashMap<ResolutionContracts.ProgramUnitId, UnitSymbols> index = new LinkedHashMap<>();
        for (UnitSymbols unit : units) {
            if (index.put(unit.id(), unit) != null)
                throw new IllegalArgumentException("duplicate program unit symbol table");
        }
        this.byId = Collections.unmodifiableMap(index);
    }

    public List<UnitSymbols> units() { return units; }
    public Optional<UnitSymbols> forProgramUnit(ResolutionContracts.ProgramUnitId id) {
        return Optional.ofNullable(byId.get(id));
    }
    public String bindingStatus() { return "NOT_PERFORMED"; }
}
