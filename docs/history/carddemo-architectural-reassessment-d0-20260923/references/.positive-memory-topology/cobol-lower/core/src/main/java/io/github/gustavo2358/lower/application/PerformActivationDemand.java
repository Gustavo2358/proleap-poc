package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Scheduling upper bound only: never emits an edge or infers source membership. */
final class PerformActivationDemand {
    static Set<SpInput.StatementId> inContext(PartialProgramAdmission.Plan plan,List<SpInput.StatementFact> body,
            Map<SpInput.StatementId,LabelId> completions,boolean intrinsic,SpInput.StatementId entry,UnitId unit,LocalIds ids) {
        var byId=new HashMap<SpInput.StatementId,SpInput.StatementFact>();
        var byLabel=new HashMap<LabelId,SpInput.StatementId>();
        for(var s:body) {
            byId.put(s.header().id(),s);
            byLabel.put(PartialProgramAssembler.label(s.header().id(),unit,ids),s.header().id());
        }
        // FILE declaratives/SORT add auxiliary callback routes. Without a scheduling
        // proof for those graphs, keep eager inventory scheduling; do not prune them.
        if(!plan.admission().input().orElseThrow().fileInventory().operations().uses().isEmpty())return Set.copyOf(byId.keySet());
        var seen=new HashSet<SpInput.StatementId>();var todo=new ArrayDeque<SpInput.StatementId>();todo.add(entry);
        while(!todo.isEmpty()) {
            var id=todo.removeFirst();var fact=byId.get(id);if(fact==null||!seen.add(id))continue;
            if(completions.containsKey(id)) {
                var local=byLabel.get(completions.get(id));if(local!=null)todo.addLast(local);
            } else {
                var next=PartialProgramAdmission.ordinaryNext(plan.admission().input().orElseThrow(),fact);
                // A call's source successor is a may-return scheduling bound, not a
                // bypass. Actual AIR reaches it only through the emitted completion.
                if(next!=null)next.statement().ifPresent(todo::addLast);
            }
            if(fact instanceof SpInput.GoToFact g)g.targetEntry().ifPresent(todo::addLast);
            if(fact instanceof SpInput.ConditionalGoToFact g)g.destinations().forEach(d->d.targetEntry().ifPresent(todo::addLast));
            if(fact instanceof SpInput.IfFact f) {
                f.thenArm().entry().statement().ifPresent(todo::addLast);
                f.elseArm().entry().statement().ifPresent(todo::addLast);
            }
            if(fact instanceof SpInput.EvaluateFact e) {
                e.arms().forEach(a->a.control().entry().statement().ifPresent(todo::addLast));
                e.otherArm().entry().statement().ifPresent(todo::addLast);
            }
        }
        return Set.copyOf(seen);
    }
}
