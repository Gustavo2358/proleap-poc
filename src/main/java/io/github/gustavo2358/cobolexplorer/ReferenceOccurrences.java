package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Typed nominal occurrences collected before any candidate lookup or binding. */
public final class ReferenceOccurrences {
    public enum Preservation { STRUCTURED, PRESERVED_CONTAINER, PRESERVED_NODE }

    /**
     * A nominal use before binding. {@code kind} is the primary routing hint for the
     * typed surface position; a contextual condition tail therefore keeps
     * {@code CONDITION} as its primary hint while {@code admissibleKinds} retains DATA,
     * INDEX and/or CONDITION as allowed by the written nominal shape.
     * Consumers must use the selected candidate kind, not this hint, as the final resolved category.
     */
    public record Occurrence(int id, ResolutionContracts.ProgramUnitId programUnitId,
                             int referenceAstNodeId, int scopeId,
                             ResolutionContracts.ReferenceKind kind,
                             Set<ResolutionContracts.ReferenceKind> admissibleKinds,
                             ResolutionContracts.ReferenceRole role,
                             String grammarRule, String writtenText, Ast.Meta meta,
                             Preservation preservation) {
        public Occurrence {
            programUnitId = Objects.requireNonNull(programUnitId, "programUnitId");
            kind = Objects.requireNonNull(kind, "kind");
            admissibleKinds = Collections.unmodifiableSet(EnumSet.copyOf(admissibleKinds));
            if (!admissibleKinds.contains(kind))
                throw new IllegalArgumentException("admissibleKinds must contain the primary kind");
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
