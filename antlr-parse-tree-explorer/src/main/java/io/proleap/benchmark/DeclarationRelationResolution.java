package io.proleap.benchmark;

import java.util.*;

/** Binding results for declaration relations, kept separate from the Symbol Table. */
public final class DeclarationRelationResolution {
    /** This product binds structurally admissible targets; it is not a full clause validator. */
    public enum SemanticScope { NOMINAL_STRUCTURAL_TARGET_BINDING }

    /**
     * A {@code RESOLVED} status means that the nominal/structural target was identified.
     * It does not certify every COBOL semantic constraint of REDEFINES or RENAMES.
     */
    public record Entry(int id, ResolutionContracts.ProgramUnitId programUnitId, int relationId,
                        SymbolTable.RelationKind kind, int referenceAstNodeId,
                        ResolutionContracts.ResolutionStatus status,
                        ResolutionContracts.ResolutionReason reason,
                        List<ReferenceResolution.Candidate> candidates) {
        public Entry { candidates = List.copyOf(candidates); }
    }

    private final List<Entry> entries;

    DeclarationRelationResolution(List<Entry> entries) {
        this.entries = List.copyOf(entries);
        for (int index = 0; index < entries.size(); index++)
            if (entries.get(index).id() != index)
                throw new IllegalArgumentException("relation resolution ids must be contiguous");
    }

    public List<Entry> entries() { return entries; }
    public SemanticScope semanticScope() { return SemanticScope.NOMINAL_STRUCTURAL_TARGET_BINDING; }
}
