package io.github.gustavo2358.cobolexplorer;

import java.util.ArrayList;
import java.util.List;

/** Builds independent declaration tables while preserving program ancestry metadata. */
final class CompilationUnitSymbolTableBuilder {
    CompilationUnitSymbolTables build(CompilationUnitModel compilationUnit) {
        List<CompilationUnitSymbolTables.UnitSymbols> units = new ArrayList<>();
        for (CompilationUnitModel.ProgramUnit unit : compilationUnit.programUnits()) {
            units.add(new CompilationUnitSymbolTables.UnitSymbols(unit.id(), unit.parentId(),
                    new SymbolTableBuilder().build(unit.program())));
        }
        return new CompilationUnitSymbolTables(units);
    }
}
