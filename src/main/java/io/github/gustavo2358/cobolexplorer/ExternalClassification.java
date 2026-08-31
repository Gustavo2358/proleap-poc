package io.github.gustavo2358.cobolexplorer;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable post-resolution classification product; it never represents nominal binding. */
public final class ExternalClassification {
    public enum Technology { CICS }

    public enum Kind { POSSIBLE_INTRINSIC }

    public enum Certainty { INFERRED }

    public enum Reason { COBOL_REFERENCE_UNRESOLVED_WITH_KNOWN_CICS_SHAPE }

    public record Entry(int id, ResolutionContracts.ProgramUnitId programUnitId,
                        int rootAstNodeId, int rootOccurrenceId, String constructWrittenText,
                        Technology technology, Kind kind, Certainty certainty, Reason reason,
                        Ast.Meta meta, List<Integer> coveredOccurrenceIds) {
        public Entry {
            if (id < 0) throw new IllegalArgumentException("classification id must be non-negative");
            programUnitId = Objects.requireNonNull(programUnitId, "programUnitId");
            if (rootAstNodeId < 0) throw new IllegalArgumentException("rootAstNodeId must be non-negative");
            if (rootOccurrenceId < 0)
                throw new IllegalArgumentException("rootOccurrenceId must be non-negative");
            if (constructWrittenText == null || constructWrittenText.isBlank())
                throw new IllegalArgumentException("constructWrittenText must not be blank");
            technology = Objects.requireNonNull(technology, "technology");
            kind = Objects.requireNonNull(kind, "kind");
            certainty = Objects.requireNonNull(certainty, "certainty");
            reason = Objects.requireNonNull(reason, "reason");
            meta = Objects.requireNonNull(meta, "meta");
            if (meta.id() != rootAstNodeId)
                throw new IllegalArgumentException("classification meta must belong to the root AST node");
            coveredOccurrenceIds = List.copyOf(coveredOccurrenceIds);
            if (coveredOccurrenceIds.isEmpty())
                throw new IllegalArgumentException("classification must cover its root occurrence");
            if (!coveredOccurrenceIds.contains(rootOccurrenceId))
                throw new IllegalArgumentException("covered occurrences must contain the root occurrence");
            if (coveredOccurrenceIds.stream().anyMatch(occurrenceId -> occurrenceId == null || occurrenceId < 0))
                throw new IllegalArgumentException("covered occurrence ids must be non-negative");
            List<Integer> sorted = coveredOccurrenceIds.stream().sorted().toList();
            if (!coveredOccurrenceIds.equals(sorted)
                    || new HashSet<>(coveredOccurrenceIds).size() != coveredOccurrenceIds.size())
                throw new IllegalArgumentException("covered occurrence ids must be unique and sorted");
        }
    }

    private record RootKey(ResolutionContracts.ProgramUnitId programUnitId, int rootAstNodeId) { }

    private record OccurrenceKey(ResolutionContracts.ProgramUnitId programUnitId, int occurrenceId) { }

    private final List<Entry> entries;

    ExternalClassification(List<Entry> entries) {
        this.entries = List.copyOf(entries);
        Set<RootKey> roots = new HashSet<>();
        Set<OccurrenceKey> coveredOccurrences = new HashSet<>();
        for (int index = 0; index < this.entries.size(); index++) {
            Entry entry = this.entries.get(index);
            if (entry.id() != index)
                throw new IllegalArgumentException("classification ids must be contiguous and deterministic");
            if (!roots.add(new RootKey(entry.programUnitId(), entry.rootAstNodeId())))
                throw new IllegalArgumentException("duplicate classification root");
            for (int occurrenceId : entry.coveredOccurrenceIds()) {
                if (!coveredOccurrences.add(new OccurrenceKey(entry.programUnitId(), occurrenceId)))
                    throw new IllegalArgumentException("an occurrence cannot be covered by two classifications");
            }
        }
    }

    public static ExternalClassification empty() {
        return new ExternalClassification(List.of());
    }

    public List<Entry> entries() {
        return entries;
    }
}
