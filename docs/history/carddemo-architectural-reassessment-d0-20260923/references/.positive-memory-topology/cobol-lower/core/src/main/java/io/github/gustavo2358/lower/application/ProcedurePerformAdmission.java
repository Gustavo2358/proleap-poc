package io.github.gustavo2358.lower.application;

import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Checks a published paragraph graph, without inferring any source boundary. */
final class ProcedurePerformAdmission {
    static Set<StatementId> members(ProcedurePerformFact p) {
        var result=new HashSet<StatementId>();p.procedures().forEach(r->result.addAll(r.statements()));return result;
    }
    static void validate(ProcedurePerformFact p,EntryGobackAdmission.Context c) {
        need(c,p,p.loop().isEmpty()||p.times().isEmpty(),"one repetition profile");
        p.times().ifPresent(t->PerformCountAdmission.validate(p,t,c));
        var operands=new HashSet<OperandId>();
        p.varying().ifPresent(v->PerformVaryingAdmission.validate(p,v,operands,c));
        p.loop().ifPresent(l->{
            c.provenance(l.provenance());c.provenance(l.predicate().provenance());
            for(var read:l.conditionReads())CallAdmission.reference(read,p.header(),operands,c);
            if(l.predicate().availability()==Availability.KNOWN)IfAdmission.admitPredicate(p.header(),l.conditionShape(),l.provenance(),l.predicate(),l.conditionReads(),c,p.varying().isPresent());
        });
        for(var endpoint:List.of(p.start(),p.end()))endpoint.ifPresent(t->{
            c.identity(t.id().unit(),t.id().handle(),"procedure",t.paragraphOrigin());c.provenance(t.referenceOrigin());c.provenance(t.paragraphOrigin());
        });
        p.normalContinuation().statement().ifPresent(id->need(c,p,c.lookup(id)!=null
            &&c.lookup(id).header().provenance().equals(p.normalContinuation().provenance()),"resume provenance agrees with referenced statement"));
        p.targetEntry().ifPresent(id->need(c,p,p.start().isPresent()&&c.lookup(id)!=null
            &&id.unit().equals(p.header().id().unit())&&c.lookup(id).header().containment().branch()==Branch.ROOT,
            "known target entry references a local root statement"));
        if(!p.procedures().isEmpty())need(c,p,p.targetEntry().filter(p.procedures().getFirst().entry()::equals).isPresent(),"target entry agrees with complete range");
        var all=new HashSet<StatementId>();var paragraphs=new HashSet<ProcedureId>();
        for(var r:p.procedures()) {
            c.identity(r.id().unit(),r.id().handle(),"procedure",r.provenance());c.provenance(r.provenance());
            need(c,p,paragraphs.add(r.id())&&!r.statements().isEmpty(),"distinct nonempty paragraphs");
            var local=new HashSet<StatementId>();
            for(var id:r.statements()) {c.touch();need(c,p,id.unit().equals(p.header().id().unit())&&c.lookup(id)!=null&&local.add(id)&&all.add(id),"distinct published range members");}
            need(c,p,local.contains(r.entry())&&c.lookup(r.entry())!=null&&c.lookup(r.entry()).header().containment().branch()==Branch.ROOT,"paragraph entry is a root member");
            var completions=new HashSet<StatementId>();
            for(var id:r.completions()) {
                need(c,p,local.contains(id)&&completions.add(id),"distinct local completion frontier");
                var s=c.lookup(id);var next=s==null?null:PartialProgramAdmission.next(s);
                need(c,p,next!=null&&(next.statement().isEmpty()||s instanceof CallFact&&next.statement().filter(local::contains).isEmpty())&&!(s instanceof GobackFact)&&!(s instanceof GoToFact),"normal completion cannot override explicit control");
            }
            for(var id:local) {
                var s=c.lookup(id);if(s==null)continue;
                s.header().containment().parent().ifPresent(parent->need(c,p,local.contains(parent),"whole structured statement belongs to paragraph"));
                var next=PartialProgramAdmission.next(s);
                if(next!=null) {
                    need(c,p,next.statement().filter(local::contains).isPresent()||next.statement().isEmpty()||s instanceof CallFact&&completions.contains(id),"intrinsic normal edge stays in paragraph");
                }
            }
        }
        if(!p.procedures().isEmpty())need(c,p,p.start().filter(t->t.id().equals(p.procedures().getFirst().id())).isPresent()
            &&p.end().filter(t->t.id().equals(p.procedures().getLast().id())).isPresent(),"range endpoints agree with typed order");
    }
    private static boolean cicsParagraph(ProcedurePerformFact p,ProcedureId id,EntryGobackAdmission.Context c) {
        return p.procedures().stream().filter(r->r.id().equals(id)).anyMatch(r->r.statements().stream().map(c::lookup)
            .allMatch(s->s!=null&&(s.header().provenance().exact()||s instanceof CicsFact x&&x.conditions()==CicsConditions.LOCAL_CONDITION||s instanceof CicsFileFact f&&f.conditions()==CicsConditions.LOCAL_CONDITION)));
    }
    static List<StatementFact> qualify(ProcedurePerformFact p,EntryGobackAdmission.Context c,Set<StatementId> precise,Set<StatementId> primary,
            List<StatementFact> inventory) {
        // W5 accepts and preserves source facts. Compositional execution is a separate capability (W6).
        if(p.publicationKind()==PerformPublicationKind.STRUCTURAL_FACTS)return List.of();
        if(p.start().isEmpty()||p.end().isEmpty()||p.procedures().isEmpty()
                ||p.normalContinuation().statement().isEmpty()
                ||p.times().filter(t->t.profile()==PerformCountProfile.UNAVAILABLE).isPresent()
                ||!PerformVaryingAdmission.executable(p,c))return List.of();
        var members=members(p);boolean structural=primary.contains(p.header().id())
            &&p.normalContinuation().statement().filter(primary::contains).isPresent()&&Collections.disjoint(primary,members);
        for(var s:inventory) {
            if(s instanceof ProcedurePerformFact other && other!=p && !other.procedures().isEmpty()) {
                var overlap=members(other);structural&=overlap.equals(members)||Collections.disjoint(overlap,members);
            }
            if(!members.contains(s.header().id())) {
                if(s instanceof ConditionalGoToFact g)structural&=GoToAdmission.precise(g)&&g.destinations().stream().noneMatch(d->d.targetEntry().filter(members::contains).isPresent());
                if(s instanceof GoToFact g)structural&=g.targetEntry().isPresent()&&g.targetEntry().filter(members::contains).isEmpty();
                var next=PartialProgramAdmission.ordinaryNext(s);if(next!=null)structural&=next.statement().filter(members::contains).isEmpty();
            }
        }
        var boundary=new HashMap<StatementId,Optional<StatementId>>();
        for(int i=0;i<p.procedures().size();i++)for(var id:p.procedures().get(i).completions())
            boundary.put(id,i+1<p.procedures().size()?Optional.of(p.procedures().get(i+1).entry()):Optional.empty());
        record Visit(StatementId id,boolean finish) { }
        var todo=new ArrayDeque<Visit>();var active=new HashSet<StatementId>();var done=new HashSet<StatementId>();
        todo.push(new Visit(p.procedures().getFirst().entry(),false));boolean supported=true;
        while(!todo.isEmpty()) {
            var v=todo.pop();if(v.finish()){active.remove(v.id());done.add(v.id());continue;}if(done.contains(v.id()))continue;
            if(!members.contains(v.id())||!active.add(v.id())){structural=false;break;}
            var s=c.lookup(v.id());if(s==null){structural=false;break;}
            if(s instanceof PerformFact||s instanceof ProcedurePerformFact){structural=false;break;}
            supported&=precise.contains(v.id());
            if(s instanceof GobackFact){active.remove(v.id());done.add(v.id());continue;}
            todo.push(new Visit(v.id(),true));
            if(s instanceof ConditionalGoToFact g) {
                if(!precise.contains(v.id())){supported=false;continue;}
                for(var d:g.destinations())todo.push(new Visit(d.targetEntry().orElseThrow(),false));
                todo.push(new Visit(g.normalContinuation().statement().orElseThrow(),false));continue;
            }
            if(s instanceof GoToFact g) {
                if(g.targetEntry().isEmpty()){structural=false;break;}todo.push(new Visit(g.targetEntry().get(),false));continue;
            }
            var next=PartialProgramAdmission.next(s);
            if(boundary.containsKey(v.id()))boundary.get(v.id()).ifPresent(id->todo.push(new Visit(id,false)));
            else if(next!=null&&next.statement().isPresent())todo.push(new Visit(next.statement().get(),false));
            else {structural=false;break;}
            if(s instanceof IfFact f) {
                f.thenArm().entry().statement().ifPresent(id->todo.push(new Visit(id,false)));
                f.elseArm().entry().statement().ifPresent(id->todo.push(new Visit(id,false)));
            } else if(s instanceof EvaluateFact e) {
                for(var a:e.arms())a.control().entry().statement().ifPresent(id->todo.push(new Visit(id,false)));
                e.otherArm().entry().statement().ifPresent(id->todo.push(new Visit(id,false)));
            }
        }
        if(!structural) {
            return List.of();
        }
        if(!supported)return List.of();
        // Stable serialization only: each successor is supplied by a fact or explicit paragraph completion.
        return members.stream().map(c::lookup).sorted(Comparator.comparingInt(s->s.header().programPoint())).toList();
    }
    private static void need(EntryGobackAdmission.Context c,ProcedurePerformFact p,boolean condition,String rule) {
        c.require(condition,Rule.STRUCTURE,p.header().id().handle(),p.header().provenance(),rule);
    }
}
