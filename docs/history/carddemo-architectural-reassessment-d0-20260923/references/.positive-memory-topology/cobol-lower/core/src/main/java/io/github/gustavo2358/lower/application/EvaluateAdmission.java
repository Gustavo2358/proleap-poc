package io.github.gustavo2358.lower.application;

import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Validates published control; never derives successors from inventory order. */
final class EvaluateAdmission {
    static void validate(EvaluateFact e, Set<StatementId> expected, EntryGobackAdmission.Context c) {
        var operands = new HashSet<OperandId>();
        e.subject().ifPresent(s -> {
            CallAdmission.reference(s,e.header(),operands,c);
            c.require(s.role()==OperandRole.READ,Rule.PROFILE_FACT,e.header().id().handle(),s.provenance(),"EVALUATE subject is read");
        });
        c.require(!e.arms().isEmpty(),Rule.STRUCTURE,e.header().id().handle(),e.header().provenance(),"literal arms required");
        var members=new HashSet<StatementId>();
        for(int i=0;i<e.arms().size();i++) {
            c.touch(); var a=e.arms().get(i); c.provenance(a.conditionOrigin());
            c.require(a.ordinal()==i && a.control().presence()==ClausePresence.PRESENT,Rule.STRUCTURE,e.header().id().handle(),a.conditionOrigin(),"semantic WHEN ordinal and presence required");
            a.selection().ifPresent(l -> {
                c.provenance(l.provenance());CallAdmission.operand(l.id(),e.header(),operands,c);
                c.require(l.kind()==LiteralKind.ALPHANUMERIC && l.logicalValue().filter(v -> v.logicalDomain()==LogicalDomain.TEXT
                            && v.logicalExtent()==v.value().codePointCount(0,v.value().length())).isPresent(),
                    Rule.PROFILE_FACT,e.header().id().handle(),l.provenance(),"typed simple literal and unique operand identity required");
            });
            for(var read:a.conditionReads()) CallAdmission.reference(read,e.header(),operands,c);
            c.require(a.selection().isPresent() || a.conditionOrigin().exact(),Rule.STRUCTURE,e.header().id().handle(),a.conditionOrigin(),"unmodeled WHEN has exact source origin");
            arm(e,a.control(),a.statements(),members,c);
        }
        arm(e,e.otherArm(),e.otherStatements(),members,c);
        c.require(members.equals(expected),Rule.STRUCTURE,e.header().id().handle(),e.header().provenance(),"all direct EVALUATE members are assigned to exactly one arm");
        c.require(e.normalContinuation().availability()!=ContinuationAvailability.NONE
                && e.normalContinuation().statement().filter(members::contains).isEmpty()
                && !e.normalContinuation().statement().equals(Optional.of(e.header().id())),Rule.STRUCTURE,e.header().id().handle(),e.header().provenance(),"normal continuation is outside arms");
    }
    private static void arm(EvaluateFact e,IfArm arm,List<StatementId> body,Set<StatementId> all,EntryGobackAdmission.Context c) {
        c.provenance(arm.provenance());
        var local=new HashSet<>(body);
        c.require(arm.presence()!=ClausePresence.ABSENT || body.isEmpty(),Rule.STRUCTURE,e.header().id().handle(),arm.provenance(),"absent arm has no members");
        c.require(arm.entry().availability()==Availability.KNOWN == arm.entry().statement().isPresent(),Rule.STRUCTURE,e.header().id().handle(),arm.provenance(),"entry availability is coherent");
        c.require(arm.entry().statement().isEmpty() || !body.isEmpty() && arm.entry().statement().get().equals(body.getFirst()),Rule.STRUCTURE,e.header().id().handle(),arm.provenance(),"entry names the published first direct member");
        for(var id:body) {
            c.touch(); var member=c.lookup(id);
            c.require(all.add(id) && member!=null && member.header().containment().equals(new Containment(Optional.of(e.header().id()),Branch.EVALUATE_ARM)),Rule.STRUCTURE,id.handle(),arm.provenance(),"arm members are distinct and owned");
            if(member!=null) { var next=PartialProgramAdmission.next(member);
                if(next!=null) c.require(next.statement().isEmpty() || next.statement().filter(local::contains).isPresent()
                        || next.statement().equals(e.normalContinuation().statement()),Rule.STRUCTURE,id.handle(),arm.provenance(),"normal arm completion cannot enter a sibling arm");
            }
        }
    }
    static boolean structured(EvaluateFact e) {
        return e.header().provenance().exact() && !e.arms().isEmpty()
            && e.arms().stream().allMatch(a -> a.control().provenance().exact() && a.conditionOrigin().exact());
    }
}
