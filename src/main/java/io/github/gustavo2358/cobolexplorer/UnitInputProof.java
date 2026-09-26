package io.github.gustavo2358.cobolexplorer;

import java.util.List;

/** Missing-input occurrences wholly enclosed by a grammar-owned top-level program,
 * explicitly ended or qualified at the physical source boundary. A nested gap belongs to its containing top-level unit.
 * This is ownership proof only: it proves neither missing content nor layout. */
public record UnitInputProof(List<Diagnostic> copies, List<Ast.SourceProvenance> regions) {
    public UnitInputProof { copies=List.copyOf(copies); regions=List.copyOf(regions); }
    public UnitInputProof(List<Diagnostic> copies) { this(copies,List.of()); }
    public static UnitInputProof unknown() { return new UnitInputProof(List.of()); }
}
