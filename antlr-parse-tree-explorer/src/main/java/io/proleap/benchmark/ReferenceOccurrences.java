package io.proleap.benchmark;

import java.util.*;

/** Typed nominal occurrences collected before any candidate lookup or binding. */
public final class ReferenceOccurrences {
    public enum Preservation { STRUCTURED, PRESERVED_CONTAINER, PRESERVED_NODE }

    public record Occurrence(int id, ResolutionContracts.ProgramUnitId programUnitId,
                             int referenceAstNodeId, int scopeId,
                             ResolutionContracts.ReferenceKind kind,
                             ResolutionContracts.ReferenceRole role,
                             String grammarRule, String writtenText, Ast.Meta meta,
                             Preservation preservation) {
        public Occurrence {
            programUnitId = Objects.requireNonNull(programUnitId, "programUnitId");
            kind = Objects.requireNonNull(kind, "kind");
            role = Objects.requireNonNull(role, "role");
            grammarRule = Objects.requireNonNullElse(grammarRule, "<unknown>");
            writtenText = Objects.requireNonNullElse(writtenText, "");
            meta = Objects.requireNonNull(meta, "meta");
            preservation = Objects.requireNonNull(preservation, "preservation");
        }
    }

    private final List<Occurrence> occurrences;

    ReferenceOccurrences(List<Occurrence> occurrences) {
        this.occurrences = List.copyOf(occurrences);
        Set<Integer> nodeIds = new HashSet<>();
        for (int index = 0; index < this.occurrences.size(); index++) {
            Occurrence occurrence = this.occurrences.get(index);
            if (occurrence.id() != index)
                throw new IllegalArgumentException("occurrence ids must be contiguous and deterministic");
            if (!nodeIds.add(occurrence.referenceAstNodeId()))
                throw new IllegalArgumentException("duplicate reference occurrence for AST node "
                        + occurrence.referenceAstNodeId());
        }
    }

    public List<Occurrence> occurrences() { return occurrences; }
    public String bindingStatus() { return "NOT_PERFORMED"; }
}
