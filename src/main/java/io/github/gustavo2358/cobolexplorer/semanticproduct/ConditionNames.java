package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.util.*;

/** Level-88 source semantics. Names refer to their associated variable, never boolean storage. */
public record ConditionNames(List<Definition> definitions,List<Use> uses,List<Assignment> assignments,List<Predicate> predicates) {
    public enum Access { READ, WRITE }
    public enum VariableDomain { TEXT, INTEGER, UNKNOWN }
    public enum ValueKind { TEXT, NUMBER, HEX, SPACES, LOW_VALUES, HIGH_VALUES, ZERO, QUOTE, ALL_TEXT }
    public record Value(ValueKind kind,String value) {
        public Value { Objects.requireNonNull(kind);Objects.requireNonNull(value);
            if(kind==ValueKind.NUMBER)new java.math.BigDecimal(value);
            if(kind==ValueKind.HEX)require(value.length()%2==0&&value.chars().allMatch(c->Character.digit(c,16)>=0),"hex literal");
        }
    }
    public record Range(Value first,Optional<Value> last) {public Range{Objects.requireNonNull(first);Objects.requireNonNull(last);}}
    public record Definition(String id,String parent,boolean anonymous,VariableDomain domain,List<Range> ranges,Optional<Value> falseValue,
            CobolSemanticProduct.Provenance variableProvenance,CobolSemanticProduct.Provenance provenance) {
        public Definition {text(id);text(parent);require(anonymous==parent.startsWith("anonymous:"),"anonymous variable identity");Objects.requireNonNull(variableProvenance);Objects.requireNonNull(domain);ranges=List.copyOf(ranges);require(!ranges.isEmpty(),"condition values");Objects.requireNonNull(falseValue);Objects.requireNonNull(provenance);}
    }
    public record Index(String kind,String value,List<Index> arguments) {
        public Index {Objects.requireNonNull(value);arguments=List.copyOf(arguments);
            require(switch(kind){case "INTEGER","READ","UNKNOWN"->arguments.isEmpty();case "ADD","SUBTRACT","MULTIPLY","DIVIDE"->arguments.size()>=2;default->false;},"condition index shape");}
    }
    public record Use(String id,String statement,String definition,String operand,Access access,List<Index> indices,CobolSemanticProduct.Provenance provenance) {
        public Use {text(id);text(statement);text(definition);Objects.requireNonNull(operand);Objects.requireNonNull(access);indices=List.copyOf(indices);Objects.requireNonNull(provenance);}
    }
    /** One assignment per destination, in source order. */
    public record Assignment(String statement,int ordinal,String use,boolean truth,Value value) {
        public Assignment {text(statement);text(use);require(ordinal>=0,"assignment ordinal");Objects.requireNonNull(value);}
    }
    public record Tree(String kind,String use,List<Tree> children) {
        public Tree {Objects.requireNonNull(use);children=List.copyOf(children);
            require(switch(kind){case "TEST"->!use.isBlank()&&children.isEmpty();case "UNKNOWN"->use.isEmpty()&&children.isEmpty();case "NOT"->use.isEmpty()&&children.size()==1;case "AND","OR"->use.isEmpty()&&children.size()>=2;default->false;},"condition tree shape");}
        public boolean complete(){var todo=new ArrayDeque<Tree>();todo.add(this);while(!todo.isEmpty()){var t=todo.removeFirst();if(t.kind().equals("UNKNOWN"))return false;todo.addAll(t.children());}return true;}
    }
    public record Predicate(String statement,String role,Tree tree) {
        public Predicate {
            text(statement); text(role); Objects.requireNonNull(tree);
            require(role.equals("IF") || role.matches("EVALUATE_(SUBJECT|WHEN)/[0-9]+")
                    || role.matches("EVALUATE_SELECTOR/[0-9]+/[0-9]+")
                    || role.matches("PERFORM_UNTIL/[0-9]+"), "condition predicate role");
        }
    }
    public ConditionNames {
        definitions=List.copyOf(definitions);uses=List.copyOf(uses);assignments=List.copyOf(assignments);predicates=List.copyOf(predicates);
        var defs=new HashMap<String,Definition>();for(var d:definitions)require(defs.put(d.id(),d)==null,"duplicate condition definition");
        var refs=new HashMap<String,Use>();for(var u:uses)require(refs.put(u.id(),u)==null&&defs.containsKey(u.definition()),"condition use identity");
        var ordinals=new HashMap<String,Integer>();
        var assignedUses=new HashSet<String>();
        for(var a:assignments){var u=refs.get(a.use());require(assignedUses.add(a.use()),"SET use assigned once");require(u!=null&&u.access()==Access.WRITE&&u.statement().equals(a.statement()),"SET use belongs to statement");
            require(a.ordinal()==ordinals.getOrDefault(a.statement(),0),"SET destination order");ordinals.put(a.statement(),a.ordinal()+1);
            var d=defs.get(u.definition());require((a.truth()?Optional.of(d.ranges().get(0).first()):d.falseValue()).filter(a.value()::equals).isPresent(),"SET uses declared value");}
        var owners=new HashSet<String>();
        for(var p:predicates){require(owners.add(p.statement()+"/"+p.role()),"duplicate predicate owner");var todo=new ArrayDeque<Tree>();todo.add(p.tree());
            while(!todo.isEmpty()){var t=todo.removeFirst();if(t.kind().equals("TEST")){var u=refs.get(t.use());require(u!=null&&u.access()==Access.READ&&u.statement().equals(p.statement()),"predicate use belongs to statement");}todo.addAll(t.children());}}
    }
    public record OperandProof(String statement,String parent,boolean write) { }
    public Set<String> completeSets(Map<String,List<String>> destinations) {
        var byUse=new HashMap<String,Use>();uses.forEach(u->byUse.put(u.id(),u));
        var assigned=new HashMap<String,List<String>>();var counts=new HashMap<String,Integer>();var writes=new HashMap<String,Integer>();
        uses.stream().filter(u->u.access()==Access.WRITE).forEach(u->writes.merge(u.statement(),1,Integer::sum));
        for(var a:assignments){counts.merge(a.statement(),1,Integer::sum);var operands=assigned.computeIfAbsent(a.statement(),k->new ArrayList<>());var operand=byUse.get(a.use()).operand();if(!operand.isEmpty())operands.add(operand);}
        var result=new HashSet<String>();
        assigned.forEach((id,operands)->{if(counts.get(id).equals(writes.get(id))&&operands.equals(destinations.get(id)))result.add(id);});
        return Set.copyOf(result);
    }
    public void validate(Set<String> data,Set<String> nodes,Set<String> statements,Map<String,OperandProof> operands) {
        for(var d:definitions)require(d.anonymous()||data.contains(d.parent()),"condition variable is a published declaration");
        var defs=new HashMap<String,Definition>();definitions.forEach(d->defs.put(d.id(),d));
        var refs=new HashMap<String,Use>();uses.forEach(u->refs.put(u.id(),u));
        for(var u:uses){require(statements.contains(u.statement()),"condition statement owner");
            var operand=operands.get(u.operand());var definition=defs.get(u.definition());
            require(definition.anonymous()?u.operand().isEmpty():operand!=null&&operand.statement().equals(u.statement())&&operand.parent().equals(definition.parent())&&operand.write()==(u.access()==Access.WRITE),"condition operand must bind its declared parent");var todo=new ArrayDeque<Index>(u.indices());while(!todo.isEmpty()){var i=todo.removeFirst();if(i.kind().equals("READ"))require(nodes.contains(i.value()),"condition index identity");todo.addAll(i.arguments());}}
        for(var a:assignments)require(refs.get(a.use()).operand().isEmpty()||operands.get(refs.get(a.use()).operand()).write(),"SET operand must write");
        for(var p:predicates){var todo=new ArrayDeque<Tree>();todo.add(p.tree());while(!todo.isEmpty()){var t=todo.removeFirst();if(t.kind().equals("TEST"))require(refs.get(t.use()).operand().isEmpty()||!operands.get(refs.get(t.use()).operand()).write(),"condition predicate must read");todo.addAll(t.children());}}
    }
    private static void text(String s){require(s!=null&&!s.isBlank(),"condition identity");}
    private static void require(boolean yes,String message){if(!yes)throw new IllegalArgumentException(message);}
}
