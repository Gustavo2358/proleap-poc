package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.util.*;


/** Positive evidence for current diagnostics, scoped to one publication's control topology. */
public final class DiagnosticEvidence {
    private final Map<String, ControlTopology.Occurrence> occurrences = new HashMap<>();
    private final Map<String, ControlTopology.Region> regions = new HashMap<>();
    private final Map<String, Set<String>> members = new HashMap<>();
    private final Map<String, ControlTopology.Outcome> outcomes = new HashMap<>();
    private final Map<String, ControlTopology.Binding> bindings = new HashMap<>();
    private final Map<String, ControlTopology.Proof> proofs = new HashMap<>();
    private final Map<String, Boolean> closed = new HashMap<>();

    public DiagnosticEvidence(Optional<ControlTopology> topology) {
        topology.ifPresent(t -> {
            t.occurrences().forEach(o -> occurrences.put(o.statement(), o));
            t.regions().forEach(r -> { regions.put(r.id(), r); members.put(r.id(), Set.copyOf(r.members())); });
            t.outcomes().forEach(o -> outcomes.put(o.id(), o));
            t.bindings().forEach(b -> bindings.put(b.caller(), b));
            t.proofs().forEach(p -> proofs.put(p.id(), p));
        });
    }

    public boolean membership(String id) {
        var o = occurrences.get(id);
        var r = o == null ? null : regions.get(o.region());
        return r != null && members.get(r.id()).contains(id) && positive(o.proofs()) && positive(r.proofs());
    }

    public boolean invocation(String id) {
        var b = bindings.get(id); var o = occurrences.get(id);
        return b != null && o != null && regions.containsKey(b.region()) && positive(b.proofs())
            && positive(o.proofs()) && b.resume().kind() != ControlTopology.TargetKind.UNKNOWN_LOCAL
            && positive(b.resume().proofs()) && o.outcomes().stream().map(outcomes::get).anyMatch(out -> out != null
                && out.kind() == ControlTopology.OutcomeKind.LOCAL_INVOKE && out.binding().equals(b.id())
                && positive(out.proofs()));
    }

    public boolean localControl(String id) {
        var o = occurrences.get(id);
        return o != null && !o.outcomes().isEmpty() && positive(o.proofs())
            && o.outcomes().stream().map(outcomes::get).allMatch(out -> out != null
                && out.kind() != ControlTopology.OutcomeKind.UNKNOWN_LOCAL
                && out.target().kind() != ControlTopology.TargetKind.UNKNOWN_LOCAL
                && positive(out.proofs()) && positive(out.target().proofs()));
    }

    private boolean positive(List<String> ids) {
        if (ids.isEmpty()) return false;
        for (var id : ids) if (!positive(id)) return false;
        return true;
    }

    private boolean positive(String root) {
        if (closed.containsKey(root)) return closed.get(root);
        var seen = new HashSet<String>();
        var pending = new ArrayDeque<String>(); pending.add(root);
        while (!pending.isEmpty()) {
            var id = pending.removeFirst();
            if (!seen.add(id)) continue;
            var p = proofs.get(id);
            if (p == null || p.kind() == ControlTopology.ProofKind.PARTIAL_UNKNOWN
                    || p.kind() == ControlTopology.ProofKind.CONTROL_POSSIBILITY
                    || Boolean.FALSE.equals(closed.get(id))) {
                closed.put(root, false); return false;
            }
            if (!Boolean.TRUE.equals(closed.get(id))) pending.addAll(p.dependencies());
        }
        seen.forEach(id -> closed.put(id, true));
        return true;
    }
}
