package io.github.gustavo2358.cobolexplorer.semanticproduct;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

/** Cross-field closure for canonical condition uses and ordered SET destinations. */
public final class ConditionNameContract {
    private ConditionNameContract() { }
    private static String statement(StatementId id){return "statement:"+id.localId();}
    private static String operand(OperandId id){return "operand:"+id.statement().localId()+":"+id.localId();}
    public static List<DataReference> references(StatementFact fact) {
        if(fact instanceof ObservedStatement o)return o.knownReferences();
        if(fact instanceof IfFact f)return f.condition().references();
        if(fact instanceof EvaluateFact e)return java.util.stream.Stream.concat(e.subject().stream(),e.arms().stream().flatMap(a->a.conditionReads().stream())).toList();
        if(fact instanceof ProcedurePerformFact p){var refs=new ArrayList<DataReference>();p.loop().ifPresent(l->refs.addAll(l.condition().references()));p.varying().ifPresent(v->{v.controls().forEach(c->refs.addAll(c.references()));v.afterLoops().forEach(l->refs.addAll(l.condition().references()));});return refs;}
        return List.of();
    }
    public static Set<String> completeSets(ConditionNames names,List<StatementFact> statements) {
        var destinations=new HashMap<String,List<String>>();
        for(var fact:statements)if(fact instanceof ObservedStatement o)
            destinations.put(statement(fact.header().id()),o.knownReferences().stream().filter(x->x.role()==OperandRole.WRITE).map(x->operand(x.id())).toList());
        return names.completeSets(destinations);
    }
    public static void validate(ConditionNames names,List<DataDeclaration> declarations,StorageInventory storage,List<StatementFact> statements) {

        var owners = new HashMap<String,StatementFact>();
        statements.forEach(f -> owners.put(statement(f.header().id()), f));
        for (var predicate : names.predicates()) {
            var owner = owners.get(predicate.statement());
            boolean valid = predicate.role().equals("IF") ? owner instanceof IfFact
                    : predicate.role().startsWith("EVALUATE_") ? owner instanceof EvaluateFact
                    : owner instanceof ProcedurePerformFact;
            if (!valid) throw new IllegalArgumentException("condition predicate owner kind");
        }
        var origins = new HashMap<String,Provenance>();
        declarations.forEach(d -> origins.put("data:" + d.id().localId(), d.provenance()));
        for (var definition : names.definitions())
            if (!definition.anonymous() && !definition.variableProvenance().equals(origins.get(definition.parent())))
                throw new IllegalArgumentException("condition variable provenance must match its declaration");
        var operands=new HashMap<String,ConditionNames.OperandProof>();
        for(var fact:statements)for(var ref:references(fact))ref.binding().selected().ifPresent(d->operands.put(operand(ref.id()),new ConditionNames.OperandProof(statement(fact.header().id()),"data:"+d.localId(),ref.role()==OperandRole.WRITE)));
        names.validate(declarations.stream().map(d->"data:"+d.id().localId()).collect(java.util.stream.Collectors.toSet()),storage.nodes().stream().map(n->"storage-node:"+n.id().localId()).collect(java.util.stream.Collectors.toSet()),statements.stream().map(x->statement(x.header().id())).collect(java.util.stream.Collectors.toSet()),operands);
    }
}
