package io.proleap.benchmark;

import java.util.*;

/** Binding results for declaration relations, kept separate from the Symbol Table. */
public final class DeclarationRelationResolution {
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
}
