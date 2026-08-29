package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Immutable name-binding product. AST and Symbol Table remain unmodified. */
public final class ReferenceResolution {
    /** Compiler-option-dependent semantics for a CALL target, separate from target syntax. */
    public record CallSemantics(Ast.CallTargetSyntax targetSyntax,
                                ResolutionContracts.CallLinkage linkage) {
        public CallSemantics {
            targetSyntax = Objects.requireNonNull(targetSyntax, "targetSyntax");
            linkage = Objects.requireNonNull(linkage, "linkage");
        }
    }

    /** A candidate's {@code kind} is its semantic declaration category after lookup. */
    public record Candidate(ResolutionContracts.SemanticEntityId entityId,
                            ResolutionContracts.ReferenceKind kind,
                            String writtenName, String canonicalName,
                            List<Integer> declarationSymbolIds,
                            Map<String, String> attributes) {
        public Candidate {
            entityId = Objects.requireNonNull(entityId, "entityId");
            kind = Objects.requireNonNull(kind, "kind");
            writtenName = Objects.requireNonNullElse(writtenName, "");
            canonicalName = Objects.requireNonNullElse(canonicalName, "");
            declarationSymbolIds = List.copyOf(declarationSymbolIds);
            attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        }
    }

    public record Entry(int id, ReferenceOccurrences.Occurrence occurrence,
                        ResolutionContracts.ResolutionStatus status,
                        ResolutionContracts.ResolutionReason reason,
                        List<Candidate> candidates, List<Integer> diagnosticIds,
                        Optional<CallSemantics> callSemantics) {
        public Entry {
            occurrence = Objects.requireNonNull(occurrence, "occurrence");
            status = Objects.requireNonNull(status, "status");
            reason = Objects.requireNonNull(reason, "reason");
            candidates = List.copyOf(candidates);
            diagnosticIds = List.copyOf(diagnosticIds);
            callSemantics = Objects.requireNonNull(callSemantics, "callSemantics");
            if (status == ResolutionContracts.ResolutionStatus.RESOLVED && candidates.size() != 1)
                throw new IllegalArgumentException("RESOLVED entry must have exactly one candidate");
            if (status == ResolutionContracts.ResolutionStatus.AMBIGUOUS && candidates.size() < 2)
                throw new IllegalArgumentException("AMBIGUOUS entry must preserve every candidate");
            if (status == ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED && !candidates.isEmpty())
                throw new IllegalArgumentException("EXTERNAL_OBSERVED entry must not invent a candidate");
        }

        public Entry(int id, ReferenceOccurrences.Occurrence occurrence,
                     ResolutionContracts.ResolutionStatus status,
                     ResolutionContracts.ResolutionReason reason,
                     List<Candidate> candidates, List<Integer> diagnosticIds) {
            this(id, occurrence, status, reason, candidates, diagnosticIds, Optional.empty());
        }

        /** The selected candidate, including the final semantic kind, only for certain bindings. */
        public Optional<Candidate> selectedCandidate() {
            return status == ResolutionContracts.ResolutionStatus.RESOLVED
                    ? Optional.of(candidates.get(0)) : Optional.empty();
        }
    }

    public record Diagnostic(int id, String code, String message,
                             ResolutionContracts.ProgramUnitId programUnitId,
                             int occurrenceId) { }

    public record Metrics(int indexedDeclarations, int nominalLookups,
                          long candidateInspections, int maximumCandidates) { }

    private record LookupKey(ResolutionContracts.ProgramUnitId unitId, String writtenText,
                             ResolutionContracts.ReferenceRole role) { }

    private final ResolutionContracts.CobolResolutionPolicy policy;
    private final List<Entry> entries;
    private final List<Diagnostic> diagnostics;
    private final Metrics metrics;
    private final DeclarationRelationResolution declarationRelations;
    private final Map<LookupKey, List<Entry>> lookup;

    ReferenceResolution(ResolutionContracts.CobolResolutionPolicy policy, List<Entry> entries,
                        List<Diagnostic> diagnostics, Metrics metrics,
                        DeclarationRelationResolution declarationRelations) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.entries = List.copyOf(entries);
        this.diagnostics = List.copyOf(diagnostics);
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.declarationRelations = Objects.requireNonNull(declarationRelations, "declarationRelations");
        LinkedHashMap<LookupKey, List<Entry>> mutable = new LinkedHashMap<>();
        for (int index = 0; index < this.entries.size(); index++) {
            Entry entry = this.entries.get(index);
            if (entry.id() != index) throw new IllegalArgumentException("resolution entry ids must be contiguous");
            LookupKey key = new LookupKey(entry.occurrence().programUnitId(),
                    entry.occurrence().writtenText(), entry.occurrence().role());
            mutable.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
        }
        LinkedHashMap<LookupKey, List<Entry>> immutable = new LinkedHashMap<>();
        mutable.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        this.lookup = Collections.unmodifiableMap(immutable);
    }

    public ResolutionContracts.CobolResolutionPolicy policy() { return policy; }
    public List<Entry> entries() { return entries; }
    public List<Diagnostic> diagnostics() { return diagnostics; }
    public Metrics metrics() { return metrics; }
    public DeclarationRelationResolution declarationRelations() { return declarationRelations; }

    public List<Entry> find(ResolutionContracts.ProgramUnitId unitId, String writtenText,
                            ResolutionContracts.ReferenceRole role) {
        return lookup.getOrDefault(new LookupKey(unitId, writtenText, role), List.of());
    }
}
