package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.Provenance;

/** Publication-local control facts. Completion is a region outcome, never a
 * pre-bound successor. This model contains no execution contexts or value facts. */
public record ControlTopology(String authority, List<Occurrence> occurrences,
        List<Region> regions, List<Boundary> boundaries, List<Outcome> outcomes,
        List<Binding> bindings, List<Proof> proofs) {
    public enum RegionKind { PROCEDURE, PARAGRAPH, RANGE, IF, IF_ARM, EVALUATE, EVALUATE_ARM, FILE, FILE_HANDLER, INLINE_BODY, DECLARATIVE }
    public enum TargetKind { OCCURRENCE, REGION_ENTRY, COMPLETE, PROGRAM_RETURN, UNKNOWN_LOCAL }
    public enum OutcomeKind { NORMAL, BRANCH, EXPLICIT_TRANSFER, LOCAL_INVOKE, PROGRAM_RETURN, UNKNOWN_LOCAL }
    public enum PhaseKind { PREDICATE, EFFECT }
    public record PhaseEdge(String role,String target) { public PhaseEdge {text(role);text(target);} }
    public record Phase(String id,PhaseKind kind,String operation,List<PhaseEdge> edges,List<String> proofs) {
        public Phase {text(id);Objects.requireNonNull(kind);text(operation);edges=sorted(nonempty(edges),PhaseEdge::role);proofs=sorted(nonempty(proofs),x->x);}
    }
    public enum ProofKind { LOCAL_GRAMMAR, RESOLVED_TARGET, EXPANDED_INCLUDE, INPUT_REGION_ISOLATION, PARTIAL_UNKNOWN }
    public record Target(TargetKind kind, String reference, List<String> proofs) {
        public Target { Objects.requireNonNull(kind); text(reference); proofs=sorted(nonempty(proofs),x->x); }
    }
    public record Occurrence(String statement, String region, List<String> outcomes, List<String> proofs) {
        public Occurrence { text(statement);text(region);outcomes=sorted(nonempty(outcomes),x->x);proofs=sorted(nonempty(proofs),x->x); }
    }
    public record Region(String id, RegionKind kind, String parent, Target entry,
            List<String> members, List<String> regions, String boundary, List<String> proofs) {
        public Region { text(id);Objects.requireNonNull(kind);Objects.requireNonNull(parent);Objects.requireNonNull(entry);
            members=sorted(members,x->x);regions=kind==RegionKind.RANGE?List.copyOf(regions):sorted(regions,x->x);text(boundary);proofs=sorted(nonempty(proofs),x->x); }
    }
    public record Boundary(String id, String region, Target ordinaryDefault, List<String> proofs) {
        public Boundary {text(id);text(region);Objects.requireNonNull(ordinaryDefault);proofs=sorted(nonempty(proofs),x->x);}
    }
    public record Outcome(String id, String statement, OutcomeKind kind, String role, Target target,
            String binding, List<String> proofs) {
        public Outcome {text(id);text(statement);Objects.requireNonNull(kind);text(role);Objects.requireNonNull(target);
            Objects.requireNonNull(binding);proofs=sorted(nonempty(proofs),x->x);}
    }
    public record Binding(String id, String caller, String region, String endpoint, Target resume, String entryPhase, String completionPhase, List<Phase> phases, List<String> proofs) {
        public Binding {text(id);text(caller);text(region);text(endpoint);Objects.requireNonNull(resume);text(entryPhase);text(completionPhase);phases=sorted(phases,Phase::id);proofs=sorted(nonempty(proofs),x->x);}
    }
    public record Proof(String id, ProofKind kind, String rule, Provenance provenance, List<String> dependencies) {
        public Proof {text(id);Objects.requireNonNull(kind);text(rule);Objects.requireNonNull(provenance);dependencies=sorted(dependencies,x->x);}
    }
    public ControlTopology {
        if(!"FRONTEND_CONTROL_TOPOLOGY_R1".equals(authority))throw new IllegalArgumentException("control topology authority");
        occurrences=sorted(occurrences,Occurrence::statement);regions=sorted(regions,Region::id);boundaries=sorted(boundaries,Boundary::id);
        outcomes=sorted(outcomes,Outcome::id);bindings=sorted(bindings,Binding::id);proofs=sorted(proofs,Proof::id);
        var os=index(occurrences,Occurrence::statement);var rs=index(regions,Region::id);var bs=index(boundaries,Boundary::id);
        var es=index(outcomes,Outcome::id);var calls=index(bindings,Binding::id);var ps=index(proofs,Proof::id);
        for(var p:proofs) {refs(p.dependencies(),ps);var pending=new ArrayDeque<String>();pending.add(p.id());var visited=new HashSet<String>();
            while(!pending.isEmpty()){var next=pending.removeFirst();if(!visited.add(next))continue;for(var dependency:ps.get(next).dependencies()){
                require(!dependency.equals(p.id()),"proof dependency cycle");pending.addLast(dependency);}}
        }
        java.util.function.Consumer<Target> target=t->{refs(t.proofs(),ps);switch(t.kind()) {
            case OCCURRENCE -> require(os.containsKey(t.reference()),"target occurrence");
            case REGION_ENTRY, COMPLETE, UNKNOWN_LOCAL, PROGRAM_RETURN -> require(rs.containsKey(t.reference()),"target region");
        }};
        for(var o:occurrences){refs(o.proofs(),ps);require(rs.containsKey(o.region()),"occurrence region");refs(o.outcomes(),es);
            require(rs.get(o.region()).members().contains(o.statement()),"inventoried region member");
            // A role selects one published outcome; opaque IDs never break a tie.
            var roles=new HashSet<String>();
            for(var id:o.outcomes()) {
                var e=es.get(id);require(e.statement().equals(o.statement()),"outcome owner");
                require(roles.add(e.role()),"duplicate outcome role: "+o.statement()+"/"+e.role());
            }}
        for(var r:regions){refs(r.proofs(),ps);refs(r.members(),os);refs(r.regions(),rs);target.accept(r.entry());
            for(var member:r.members())require(os.get(member).region().equals(r.id()),"member owner");
            require(r.parent().isEmpty()||rs.containsKey(r.parent()),"region parent");
            require(bs.containsKey(r.boundary())&&bs.get(r.boundary()).region().equals(r.id()),"region boundary");}
        for(var b:boundaries){refs(b.proofs(),ps);require(rs.containsKey(b.region()),"boundary region");require(rs.get(b.region()).boundary().equals(b.id()),"single region boundary");target.accept(b.ordinaryDefault());}
        for(var e:outcomes){refs(e.proofs(),ps);require(os.containsKey(e.statement())&&os.get(e.statement()).outcomes().contains(e.id()),"inventoried outcome");target.accept(e.target());
            require(e.kind()==OutcomeKind.LOCAL_INVOKE?!e.binding().isEmpty()&&calls.containsKey(e.binding()):e.binding().isEmpty(),"outcome binding");
            if(e.kind()==OutcomeKind.LOCAL_INVOKE)require(calls.get(e.binding()).caller().equals(e.statement())
                &&e.target().kind()==TargetKind.REGION_ENTRY&&e.target().reference().equals(calls.get(e.binding()).region()),"invocation target/binding agreement");
            if(e.kind()==OutcomeKind.PROGRAM_RETURN)require(e.target().kind()==TargetKind.PROGRAM_RETURN,"return target");
            if(e.kind()==OutcomeKind.UNKNOWN_LOCAL)require(e.target().kind()==TargetKind.UNKNOWN_LOCAL,"unknown target");
        }
        for(var b:bindings){refs(b.proofs(),ps);require(os.containsKey(b.caller())&&rs.containsKey(b.region())&&bs.containsKey(b.endpoint()),"binding references");target.accept(b.resume());
            require(rs.get(b.region()).kind()==RegionKind.RANGE,"invoke region is range");
            require(!rs.get(b.region()).regions().isEmpty()&&rs.get(b.region()).regions().get(rs.get(b.region()).regions().size()-1).equals(bs.get(b.endpoint()).region()),"range endpoint");}
        for(var b:bindings) {
            var phaseIds=new HashSet<String>(List.of("BODY","RESUME"));
            for(var phase:b.phases())require(phaseIds.add(phase.id()),"duplicate phase identity");
            require(phaseIds.contains(b.entryPhase())&&phaseIds.contains(b.completionPhase()),"binding phase entry/completion");
            for(var phase:b.phases()) {refs(phase.proofs(),ps);
                require(phase.kind()==PhaseKind.PREDICATE?Set.of("UNTIL_PREDICATE","COUNT_ENTRY","COUNT_REPEAT").contains(phase.operation())
                    :Set.of("VARY_INITIAL","VARY_UPDATE").contains(phase.operation()),"phase operation payload kind");var roles=new HashSet<String>();
                for(var edge:phase.edges())require(phaseIds.contains(edge.target())&&roles.add(edge.role()),"phase target/role");
                require(phase.kind()==PhaseKind.PREDICATE?roles.equals(Set.of("true","false")):roles.equals(Set.of("next")),"phase shape");
            }
        }
        // Symbolic entry/completion aliases terminate before an executable occurrence.
        // Executable loops and invocation recursion are not rejected by this check.
        var aliases=new HashMap<String,String>();
        for(var r:regions)aliases.put("entry:"+r.id(),alias(r.entry()));
        for(var b:boundaries)aliases.put("complete:"+b.region(),alias(b.ordinaryDefault()));
        var finished=new HashSet<String>();
        for(var start:aliases.keySet()) {
            var chain=new HashSet<String>();String current=start;
            while(current!=null&&!finished.contains(current)) {
                require(chain.add(current),"symbolic topology reference cycle");current=aliases.get(current);
            }
            finished.addAll(chain);
        }
        // Parent/completion composition must terminate. Executable control may cycle.
        for(var r:regions){var seen=new HashSet<String>();String parent=r.id();while(!parent.isEmpty()){
            require(seen.add(parent),"region parent cycle");parent=rs.get(parent).parent();}}
    }
    private static String alias(Target target) {
        return switch(target.kind()) {
            case REGION_ENTRY -> "entry:"+target.reference();
            case COMPLETE -> "complete:"+target.reference();
            default -> null;
        };
    }
    private static <T> List<T> sorted(List<T> values,java.util.function.Function<T,String> key) {return values.stream().sorted(Comparator.comparing(key)).toList();}
    private static <T> Map<String,T> index(List<T> values, java.util.function.Function<T,String> key) {
        var m=new HashMap<String,T>();for(var v:values)require(m.put(key.apply(v),v)==null,"duplicate topology identity");return m;
    }
    private static void refs(List<String> ids,Map<String,?> map){var unique=new HashSet<String>();for(var id:ids)require(map.containsKey(id)&&unique.add(id),"unresolved/duplicate topology reference: "+id);}
    private static <T> List<T> nonempty(List<T> values){var copy=List.copyOf(values);require(!copy.isEmpty(),"required topology evidence/inventory");return copy;}
    private static void text(String value){require(value!=null&&!value.isBlank(),"required topology identity");}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
