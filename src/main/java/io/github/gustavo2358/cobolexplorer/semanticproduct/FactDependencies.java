package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.Provenance;

/** Causal source proof graph. Physical layout and logical value identity are distinct facets. */
public record FactDependencies(String authority,List<Input> inputs,List<Proof> proofs,
        List<Region> regions,List<Fact> facts,List<Binding> bindings) {
    public enum InputKind { MISSING_COPY, OPAQUE_INCLUDE, UNLOCATED_INPUT, PHYSICAL_PROFILE }
    public enum ProofKind { SOURCE_SYNTAX, REGION_CONTEXT, DECLARATION_CONTEXT, REGION_BOUNDARY, REGION_CLOSURE, ALIAS_INVENTORY, ALIAS_CLOSURE, LOCAL_ALLOCATION, LOGICAL_TYPE, PROFILE, PHYSICAL_VIEW }
    public enum FactKind { SOURCE_IDENTITY, LOGICAL_TEXT, STORAGE_IDENTITY, LOCAL_CELL, PHYSICAL_VIEW }
    public record Input(String id,InputKind kind,boolean available,List<String> contextScopes,List<String> closureScopes,List<String> declarationScopes,Provenance provenance) {
        public Input {text(id);Objects.requireNonNull(kind);contextScopes=sorted(contextScopes);closureScopes=sorted(closureScopes);declarationScopes=sorted(declarationScopes);Objects.requireNonNull(provenance);}
    }
    public record Proof(String id,ProofKind kind,String scope,String subject,boolean localPremise,List<String> dependencies,List<String> inputs,String rule,Provenance provenance) {
        public Proof {text(id);Objects.requireNonNull(kind);text(scope);text(subject);dependencies=sorted(dependencies);inputs=sorted(inputs);text(rule);Objects.requireNonNull(provenance);}
    }
    public record Region(String id,List<String> members,Provenance provenance) {
        public Region {text(id);members=sorted(members);require(!members.isEmpty(),"region members required");Objects.requireNonNull(provenance);}
    }
    public record Fact(String id,FactKind kind,String subject,String region,List<String> dependencies) {
        public Fact {text(id);Objects.requireNonNull(kind);text(subject);text(region);dependencies=sorted(dependencies);require(!dependencies.isEmpty(),"fact dependencies required");}
    }
    /** Empty bounds mean unavailable, not an empty storage region. */
    public record Binding(String node,String region,String exactCell,List<String> cells,List<String> regions,List<String> dependencies) {
        public Binding {text(node);text(region);Objects.requireNonNull(exactCell);cells=sorted(cells);regions=sorted(regions);dependencies=sorted(dependencies);require(!dependencies.isEmpty(),"binding dependencies required");}
    }
    public FactDependencies {
        require("FRONTEND_FACT_DEPENDENCY_LOCALITY_R2".equals(authority),"fact dependency authority");
        inputs=order(inputs,Input::id);proofs=order(proofs,Proof::id);regions=order(regions,Region::id);facts=order(facts,Fact::id);bindings=order(bindings,Binding::node);
        var ins=index(inputs,Input::id);var ps=index(proofs,Proof::id);var rs=index(regions,Region::id);var fs=index(facts,Fact::id);var bs=index(bindings,Binding::node);
        var owners=new HashMap<String,String>();
        for(var r:regions)for(var n:r.members())require(owners.put(n,r.id())==null,"node has one nominal region");
        for(var i:inputs){refs(i.contextScopes(),rs);refs(i.closureScopes(),rs);refs(i.declarationScopes(),owners);}
        var inputDeclarations=new HashMap<String,Set<String>>();var inputContext=new HashMap<String,Set<String>>();var inputClosure=new HashMap<String,Set<String>>();var profileInputs=new TreeSet<String>();
        for(var i:inputs) {for(var scope:i.contextScopes())inputContext.computeIfAbsent(scope,k->new TreeSet<>()).add(i.id());
            for(var scope:i.closureScopes())inputClosure.computeIfAbsent(scope,k->new TreeSet<>()).add(i.id());
            for(var node:i.declarationScopes())inputDeclarations.computeIfAbsent(node,k->new TreeSet<>()).add(i.id());
            if(i.kind()==InputKind.PHYSICAL_PROFILE)profileInputs.add(i.id());}
        require(profileInputs.size()==1,"one explicit physical profile premise");
        for(var p:proofs) {
            require(rs.containsKey(p.scope()),"proof region exists");refs(p.dependencies(),ps);refs(p.inputs(),ins);
            require(p.subject().equals(p.scope())||p.scope().equals(owners.get(p.subject())),"proof subject belongs to scope");
            require(switch(p.kind()) {
                case SOURCE_SYNTAX -> true;
                case DECLARATION_CONTEXT,LOGICAL_TYPE,PHYSICAL_VIEW -> owners.containsKey(p.subject());
                default -> p.subject().equals(p.scope());
            },"proof kind has the required region/declaration subject");
            kinds(p.dependencies(),ps,switch(p.kind()) {
                case SOURCE_SYNTAX,PROFILE -> Set.of();
                case REGION_CONTEXT,DECLARATION_CONTEXT,REGION_BOUNDARY,ALIAS_INVENTORY -> Set.of(ProofKind.SOURCE_SYNTAX);
                case LOGICAL_TYPE -> Set.of(ProofKind.SOURCE_SYNTAX,ProofKind.DECLARATION_CONTEXT);
                case REGION_CLOSURE -> Set.of(ProofKind.SOURCE_SYNTAX,ProofKind.REGION_BOUNDARY);
                case ALIAS_CLOSURE -> Set.of(ProofKind.REGION_CLOSURE,ProofKind.ALIAS_INVENTORY);
                case LOCAL_ALLOCATION -> Set.of(ProofKind.SOURCE_SYNTAX,ProofKind.REGION_CONTEXT);
                case PHYSICAL_VIEW -> Set.of(ProofKind.SOURCE_SYNTAX,ProofKind.PROFILE,ProofKind.REGION_CONTEXT,ProofKind.REGION_CLOSURE);
            });
            var expected=switch(p.kind()) {
                case DECLARATION_CONTEXT -> inputDeclarations.getOrDefault(p.subject(),Set.of());
                case REGION_CONTEXT -> inputContext.getOrDefault(p.scope(),Set.of());
                case REGION_CLOSURE -> inputClosure.getOrDefault(p.scope(),Set.of());
                case PROFILE -> profileInputs;
                default -> Set.<String>of();
            };
            require(expected.equals(new TreeSet<>(p.inputs())),"proof input relation is complete");
            for(var dependency:p.dependencies()) {
                var premise=ps.get(dependency);require(premise.scope().equals(p.scope()),"proof scope agreement");
                if(premise.kind()==ProofKind.SOURCE_SYNTAX||premise.kind()==ProofKind.DECLARATION_CONTEXT)
                    require(premise.subject().equals(p.subject()),"proof declaration subject agreement");
            }
        }
        var known=evaluate(proofs,ins); // also rejects cycles
        var cells=new HashMap<String,Fact>();var allocations=new HashMap<String,Fact>();
        var facets=new HashSet<String>();
        for(var f:facts) {
            require(rs.containsKey(f.region()),"fact region exists");refs(f.dependencies(),ps);
            require(facets.add(f.kind()+"/"+f.subject()),"unique fact facet");
            require(f.kind()==FactKind.STORAGE_IDENTITY?f.subject().equals(f.region()):f.region().equals(owners.get(f.subject())),"fact subject belongs to region");
            kinds(f.dependencies(),ps,switch(f.kind()) {
                case SOURCE_IDENTITY -> Set.of(ProofKind.SOURCE_SYNTAX);
                case LOGICAL_TEXT -> Set.of(ProofKind.LOGICAL_TYPE);
                case STORAGE_IDENTITY -> Set.of(ProofKind.LOCAL_ALLOCATION);
                case LOCAL_CELL -> Set.of(ProofKind.LOGICAL_TYPE,ProofKind.ALIAS_CLOSURE,ProofKind.LOCAL_ALLOCATION);
                case PHYSICAL_VIEW -> Set.of(ProofKind.PHYSICAL_VIEW);
            });
            for(var p:f.dependencies()) {
                var proof=ps.get(p);require(proof.scope().equals(f.region()),"fact proof scope");
                boolean local=proof.kind()==ProofKind.SOURCE_SYNTAX||proof.kind()==ProofKind.LOGICAL_TYPE||proof.kind()==ProofKind.PHYSICAL_VIEW;
                require(proof.subject().equals(local?f.subject():f.region()),"fact proof subject agreement");
            }
            if(f.kind()==FactKind.LOCAL_CELL)cells.put(f.subject(),f);
            if(f.kind()==FactKind.STORAGE_IDENTITY)allocations.put(f.subject(),f);
        }
        require(allocations.keySet().equals(rs.keySet()),"every region has an allocation facet");
        require(bs.keySet().equals(owners.keySet()),"binding inventory equals region nodes");
        for(var b:bindings) {
            require(b.region().equals(owners.get(b.node())),"binding region membership");refs(b.dependencies(),ps);
            kinds(b.dependencies(),ps,Set.of(ProofKind.LOCAL_ALLOCATION));
            for(var p:b.dependencies())require(ps.get(p).scope().equals(b.region()),"binding proof scope");
            for(var cell:b.cells())require(cells.containsKey(cell)&&cells.get(cell).region().equals(b.region())&&all(cells.get(cell).dependencies(),known),"binding cell requires available facet in same region");
            for(var region:b.regions())require(region.equals(b.region())&&all(allocations.get(region).dependencies(),known),"binding region requires allocation proof");
            require(b.exactCell().isEmpty()||b.cells().equals(List.of(b.exactCell()))&&b.regions().isEmpty(),"exact cell binding shape");
            if(!all(b.dependencies(),known))require(b.cells().isEmpty()&&b.regions().isEmpty()&&b.exactCell().isEmpty(),"unavailable binding has no executable precision");
            else require(!b.cells().isEmpty()||!b.regions().isEmpty(),"available binding requires grounded bound");
        }
    }
    public Map<String,Boolean> proofAvailability(){return evaluate(proofs,index(inputs,Input::id));}
    public boolean available(Fact fact){return all(fact.dependencies(),proofAvailability());}
    private static Map<String,Boolean> evaluate(List<Proof> proofs,Map<String,Input> inputs) {
        var counts=new HashMap<String,Integer>();var users=new HashMap<String,List<String>>();var ps=index(proofs,Proof::id);var queue=new ArrayDeque<String>();
        for(var p:proofs){counts.put(p.id(),p.dependencies().size());if(p.dependencies().isEmpty())queue.add(p.id());for(var d:p.dependencies())users.computeIfAbsent(d,k->new ArrayList<>()).add(p.id());}
        var known=new HashMap<String,Boolean>();
        while(!queue.isEmpty()){var id=queue.remove();var p=ps.get(id);known.put(id,p.localPremise()&&all(p.dependencies(),known)&&p.inputs().stream().allMatch(i->inputs.get(i).available()));for(var u:users.getOrDefault(id,List.of()))if(counts.compute(u,(k,v)->v-1)==0)queue.add(u);}
        require(known.size()==proofs.size(),"proof dependency cycle");return Map.copyOf(known);
    }
    private static boolean all(List<String> ids,Map<String,Boolean> known){return ids.stream().allMatch(id->Boolean.TRUE.equals(known.get(id)));}
    private static void kinds(List<String> ids,Map<String,Proof> proofs,Set<ProofKind> required){var actual=new HashSet<ProofKind>();for(var id:ids)actual.add(proofs.get(id).kind());require(actual.equals(required),"required causal proof kinds: "+required);}
    private static List<String> sorted(List<String> xs){var copy=xs.stream().sorted().toList();require(new HashSet<>(copy).size()==copy.size(),"duplicate dependency/reference");return copy;}
    private static <T> List<T> order(List<T> xs,java.util.function.Function<T,String> key){return xs.stream().sorted(Comparator.comparing(key)).toList();}
    private static <T> Map<String,T> index(List<T> xs,java.util.function.Function<T,String> key){var m=new HashMap<String,T>();for(var x:xs)require(m.put(key.apply(x),x)==null,"duplicate fact graph identity");return m;}
    private static void refs(List<String> xs,Map<String,?> index){for(var x:xs)require(index.containsKey(x),"missing fact graph reference: "+x);}
    private static void text(String x){require(x!=null&&!x.isBlank(),"fact graph identity required");}
    private static void require(boolean x,String message){if(!x)throw new IllegalArgumentException(message);}
}
