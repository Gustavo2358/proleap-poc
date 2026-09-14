package io.github.gustavo2358.cobolexplorer;

import java.util.List;

/** Missing-input occurrences wholly enclosed by a grammar-owned, explicitly ended
 * top-level program. A nested gap belongs to its containing top-level unit.
 * This is ownership proof only: it proves neither missing content nor layout. */
public record UnitInputProof(List<Diagnostic> copies) {
    public UnitInputProof { copies=List.copyOf(copies); }
    public static UnitInputProof unknown() { return new UnitInputProof(List.of()); }
}
