package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.NominalValues;

/** Source text/copy facts independent of executable storage admission. No value propagation. */
public final class NominalValueSemantics {
    public record Assignment(int statement,String target,NominalValues.Term source) { }
    public record Condition(int statement,NominalValues.Predicate predicate) { }
    public record Query(int statement,String node) { }
    public record Facts(List<NominalValues.Symbol> symbols,List<Assignment> assignments,List<Condition> conditions,List<Query> queries,List<NominalValues.TableField> tableFields) {
        public Facts {symbols=List.copyOf(symbols);assignments=assignments.stream().distinct().toList();conditions=List.copyOf(conditions);queries=List.copyOf(queries);tableFields=List.copyOf(tableFields);}
    }
    private final Map<ResolutionContracts.ProgramUnitId,Facts> units;
    private NominalValueSemantics(Map<ResolutionContracts.ProgramUnitId,Facts> units){this.units=Map.copyOf(units);}
    public Optional<Facts> facts(ResolutionContracts.ProgramUnitId unit){return Optional.ofNullable(units.get(unit));}
    static NominalValueSemantics analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,
            Map<ResolutionContracts.SemanticEntityId,ScalarMoveSemantics.ScalarText> shapes,Optional<StorageAccessSemantics> storage,StorageComponents components,ConditionNameSemantics conditionNames) {
        var result=new HashMap<ResolutionContracts.ProgramUnitId,Facts>();
        if(storage.isEmpty())return new NominalValueSemantics(result);
        var predicates=TextConditionSemantics.analyze(frontend,resolution,shapes,true);
        for(var unit:frontend.compilationUnit().programUnits()) {
            var modelNodes=new HashSet<Integer>();
            var inventory=new ArrayDeque<Ast.Node>();inventory.add(unit.program());
            while(!inventory.isEmpty()){var n=inventory.removeFirst();if(n instanceof Ast.Program&&n!=unit.program())continue;
                if(n instanceof Ast.DataEntry d&&d.meta().syntheticModel())modelNodes.add(d.meta().id());inventory.addAll(Ast.children(n));}
            var nodes=new HashMap<ResolutionContracts.SemanticEntityId,String>();var symbols=new ArrayList<NominalValues.Symbol>();
            for(var n:storage.get().layout().layout(unit.id()).nodes())n.entity().ifPresent(e->{
                var shape=shapes.get(e);if(shape!=null){String id="storage-node:"+n.id().node();nodes.put(e,id);symbols.add(new NominalValues.Symbol(id,shape.extent(),modelNodes.contains(n.id().node())));}});
            var entityNodes=new HashMap<ResolutionContracts.SemanticEntityId,Integer>();
            storage.get().layout().layout(unit.id()).nodes().forEach(n->n.entity().ifPresent(e->entityNodes.put(e,n.id().node())));
            var referenceNodes=new HashMap<Integer,Integer>();
            for(var r:resolution.entries())if(r.occurrence().programUnitId().equals(unit.id())&&r.status()==ResolutionContracts.ResolutionStatus.RESOLVED)
                r.selectedCandidate().map(c->entityNodes.get(c.entityId())).ifPresent(node->referenceNodes.put(r.occurrence().referenceAstNodeId(),node));
            var table=new TableTextSemantics(components.unit(unit.id()),referenceNodes);
            var bySymbol=new TreeMap<String,NominalValues.Symbol>();symbols.forEach(s->bySymbol.put(s.node(),s));
            for(var s:table.symbols())bySymbol.merge(s.node(),s,(a,b)->new NominalValues.Symbol(a.node(),a.extent(),a.modelAssumed()||b.modelAssumed()));
            symbols.clear();symbols.addAll(bySymbol.values());
            var references=new HashMap<Integer,String>();
            for(var r:resolution.entries())if(r.occurrence().programUnitId().equals(unit.id())&&r.status()==ResolutionContracts.ResolutionStatus.RESOLVED)
                r.selectedCandidate().map(c->nodes.get(c.entityId())).ifPresent(node->references.put(r.occurrence().referenceAstNodeId(),node));
            var assignments=new ArrayList<Assignment>();var conditions=new ArrayList<Condition>();var queries=new ArrayList<Query>();
            var conditionUses=conditionNames.uses(unit.id());
            var sourceConditions=new HashMap<Integer,io.github.gustavo2358.cobolexplorer.semanticproduct.ConditionNames.Tree>();
            for(var p:conditionNames.predicates(unit.id()))if(p.role().equals("IF"))sourceConditions.put(p.statement(),p.tree());
            var todo=new ArrayDeque<Ast.Node>();todo.add(unit.program());
            while(!todo.isEmpty()) {
                var n=todo.removeFirst();if(n instanceof Ast.Program&&n!=unit.program())continue;
                if(n instanceof Ast.MoveStatement m&&!m.corresponding()) {
                    var source=term(m.source(),references,table);
                    for(var receiver:m.targets()) {
                        var target=term(receiver,references,table);
                        if(target.kind().equals("READ")&&table.fields().stream().noneMatch(f->f.node().equals(target.value())))assignments.add(new Assignment(m.meta().id(),target.value(),source));
                    }
                    assignments.addAll(table.writes(m,source));
                }
                if(n instanceof Ast.IfStatement branch) {
                    var tree=sourceConditions.get(branch.meta().id());
                    var source=tree==null?Optional.<NominalValues.Predicate>empty():conditionPredicate(tree,conditionUses,nodes);
                    if(source.isPresent())conditions.add(new Condition(branch.meta().id(),source.get()));
                    else {var p=predicates.get(new ScalarMoveSemantics.NodeKey(unit.id(),branch.meta().id()));
                        if(p!=null)conditions.add(new Condition(branch.meta().id(),predicate(p,references)));}
                }
                if(n instanceof Ast.ModeledStatement&&conditionNames.sets(unit.id()).containsKey(n.meta().id())) {
                    // Sequential SET destinations may alias. The last write to a whole variable wins.
                    var last=new LinkedHashMap<String,NominalValues.Term>();
                    for(var a:conditionNames.sets(unit.id()).get(n.meta().id())) {
                        var target=nodes.get(a.use().declaration().parent());
                        if(target!=null&&a.use().indices().isEmpty())last.put(target,conditionTerm(a.value()).orElse(new NominalValues.Term("UNKNOWN","")));
                    }
                    last.forEach((target,value)->assignments.add(new Assignment(n.meta().id(),target,value)));
                }
                if(n instanceof Ast.CallStatement call){var t=term(call.target(),references,table);if(t.kind().equals("READ"))queries.add(new Query(call.meta().id(),t.value()));}
                if(n instanceof Ast.EmbeddedLanguageStatement embedded&&embedded.language()==Ast.EmbeddedLanguage.CICS)
                    for(var host:embedded.hostOperands())if(host.option().equals("PROGRAM")) {
                        var t=term(host.reference(),references,table);if(t.kind().equals("READ"))queries.add(new Query(embedded.meta().id(),t.value()));
                    }
                todo.addAll(Ast.children(n));
            }
            symbols.sort(Comparator.comparing(NominalValues.Symbol::node));
            assignments.sort(Comparator.comparingInt(Assignment::statement).thenComparing(Assignment::target));
            conditions.sort(Comparator.comparingInt(Condition::statement));queries.sort(Comparator.comparingInt(Query::statement));
            result.put(unit.id(),new Facts(symbols,assignments,conditions,queries,table.fields()));
        }
        return new NominalValueSemantics(result);
    }
    private static Optional<NominalValues.Term> conditionTerm(Ast.ConditionValue value) {
        return switch(value.kind()) {
            case TEXT->Optional.of(new NominalValues.Term("LITERAL",value.value()));
            case SPACES,LOW_VALUES,HIGH_VALUES->Optional.of(new NominalValues.Term(value.kind().name(),""));
            default->Optional.empty();
        };
    }
    private static Optional<NominalValues.Predicate> conditionPredicate(io.github.gustavo2358.cobolexplorer.semanticproduct.ConditionNames.Tree tree,
            Map<Integer,ConditionNameSemantics.Use> uses,Map<ResolutionContracts.SemanticEntityId,String> nodes) {
        if(tree.kind().equals("UNKNOWN"))return Optional.empty();
        if(tree.kind().equals("TEST")) {
            var use=uses.get(Integer.parseInt(tree.use().substring("condition-use:".length())));
            var node=nodes.get(use.declaration().parent());if(node==null||!use.indices().isEmpty())return Optional.empty();
            var alternatives=new ArrayList<NominalValues.Predicate>();
            for(var range:use.declaration().ranges()) {
                var value=conditionTerm(range.first());if(range.last().isPresent()||value.isEmpty())return Optional.empty();
                alternatives.add(new NominalValues.Predicate("EQ",List.of(new NominalValues.Term("READ",node),value.get()),List.of()));
            }
            return Optional.of(alternatives.size()==1?alternatives.get(0):new NominalValues.Predicate("OR",List.of(),alternatives));
        }
        var children=new ArrayList<NominalValues.Predicate>();
        for(var child:tree.children()){var p=conditionPredicate(child,uses,nodes);if(p.isEmpty())return Optional.empty();children.add(p.get());}
        return Optional.of(new NominalValues.Predicate(tree.kind(),List.of(),children));
    }
    private static NominalValues.Term term(Ast.Expression e,Map<Integer,String> references,TableTextSemantics table) {
        { // Source coordinates may be approximate even when the parsed operand is structured.
            if(e instanceof Ast.DataReference indexed&&table.read(indexed).isPresent())return new NominalValues.Term("READ",table.read(indexed).orElseThrow());
            if(e instanceof Ast.DataReference ref&&ref.understanding()==Ast.ReferenceUnderstanding.STRUCTURED
                &&ref.subscriptGroups().isEmpty()&&ref.referenceModification()==null&&references.containsKey(ref.meta().id()))
                return new NominalValues.Term("READ",references.get(ref.meta().id()));
            if(e instanceof Ast.FunctionExpression function&&function.arguments().size()==1&&function.referenceModification()==null) {
                String operator=switch(function.functionName().toUpperCase(Locale.ROOT)) {
                    case "UPPER-CASE"->function.directions().isEmpty()?"UPPER_ASCII":null;
                    case "TRIM"->function.directions().isEmpty()?"TRIM_SPACES":function.directions().size()!=1?null:
                        function.directions().get(0)==Ast.FunctionDirection.LEADING?"TRIM_LEADING_SPACES":"TRIM_TRAILING_SPACES";
                    default->null;
                };
                if(operator!=null)return new NominalValues.Term(operator,"",List.of(term(function.arguments().get(0),references,table)));
            }
            if(e instanceof Ast.LiteralExpression literal) {
                if(literal.logicalText().isPresent())return new NominalValues.Term("LITERAL",literal.logicalText().get().value());
                if(literal.figurativeText().isPresent())return new NominalValues.Term(literal.figurativeText().get().name(),"");
            }
        }
        return new NominalValues.Term("UNKNOWN","");
    }
    private static NominalValues.Predicate predicate(TextConditionSemantics.Predicate p,Map<Integer,String> refs) {
        if(p.kind()==TextConditionSemantics.Kind.NOT||p.kind()==TextConditionSemantics.Kind.AND||p.kind()==TextConditionSemantics.Kind.OR)
            return new NominalValues.Predicate(p.kind().name(),List.of(),p.children().stream().map(c->predicate(c,refs)).toList());
        var left=new NominalValues.Term("READ",Objects.requireNonNull(refs.get(p.reference().orElseThrow())));
        var right=switch(p.kind()) {
            case EQUAL_TEXT->new NominalValues.Term("LITERAL",p.text().orElseThrow());
            case EQUAL_REFERENCE->new NominalValues.Term("READ",Objects.requireNonNull(refs.get(p.comparedReference().orElseThrow())));
            case EQUAL_SPACES->new NominalValues.Term("SPACES","");
            case EQUAL_LOW_VALUES->new NominalValues.Term("LOW_VALUES","");
            case EQUAL_HIGH_VALUES->new NominalValues.Term("HIGH_VALUES","");
            default->throw new IllegalArgumentException("text relation kind");
        };
        return new NominalValues.Predicate("EQ",List.of(left,right),List.of());
    }
}
