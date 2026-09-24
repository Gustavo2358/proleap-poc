package io.github.gustavo2358.lower.application;

import java.util.*;
import io.github.gustavo2358.lower.domain.SpInput;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Positive entry/completion and independently supported repetition facts. */
final class CompositionalPerformAdmission {
    private static boolean repetition(ProcedurePerformFact p, EntryGobackAdmission.Context c) {
        return p.times().filter(t->t.profile()==PerformCountProfile.UNAVAILABLE).isEmpty()
            && p.loop().filter(l->l.predicate().availability()!=Availability.KNOWN).isEmpty()
            && PerformVaryingAdmission.executable(p,c);
    }
    static Map<StatementId,List<StatementFact>> plan(SpInput input, EntryGobackAdmission.Context c,
            Map<StatementId,List<StatementFact>> legacy) {
        var result=new HashMap<StatementId,List<StatementFact>>();
        for(var s:input.statements())if(s instanceof ProcedurePerformFact p && !legacy.containsKey(p.header().id())
                && p.targetEntry().isPresent() && repetition(p,c)) {
            var frontiers=new HashSet<StatementId>();p.procedures().forEach(r->frontiers.addAll(r.completions()));
            var seen=new HashSet<StatementId>();var pending=new ArrayDeque<StatementId>();
            pending.add(p.targetEntry().orElseThrow());pending.addAll(ProcedurePerformAdmission.members(p));
            while(!pending.isEmpty()) {
                var id=pending.removeFirst();if(!seen.add(id))continue;
                c.touch();var member=c.lookup(id);
                var next=PartialProgramAdmission.ordinaryNext(input,member);
                if(next!=null&&!frontiers.contains(id))next.statement().ifPresent(pending::addLast);
                if(member instanceof GoToFact g)g.targetEntry().ifPresent(pending::addLast);
                if(member instanceof ConditionalGoToFact g)g.destinations().forEach(d->d.targetEntry().ifPresent(pending::addLast));
                if(member instanceof IfFact f) {
                    f.thenArm().entry().statement().ifPresent(pending::addLast);
                    f.elseArm().entry().statement().ifPresent(pending::addLast);
                }
                if(member instanceof EvaluateFact e) {
                    e.arms().forEach(a->a.control().entry().statement().ifPresent(pending::addLast));
                    e.otherArm().entry().statement().ifPresent(pending::addLast);
                }
            }
            // Explicit edge closure is not inferred paragraph membership. Only published
            // paragraph completion IDs may return; missing membership adds no frontier.
            result.put(p.header().id(),seen.stream().map(c::lookup)
                .sorted(Comparator.comparingInt(f->f.header().programPoint())).toList());
        }
        return Map.copyOf(result);
    }
}
