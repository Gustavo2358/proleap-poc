package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.NominalValues;

/** Source text/copy facts independent of executable storage admission. No value propagation. */
public final class NominalValueSemantics {
    public record Assignment(int statement,String target,NominalValues.Term source) { }
    public record Condition(int statement,NominalValues.Predicate predicate) { }
    public record Query(int statement,String node) { }
    public record Facts(List<NominalValues.Symbol> symbols,List<Assignment> assignments,List<Condition> conditions,List<Query> queries) {
        public Facts {symbols=List.copyOf(symbols);assignments=assignments.stream().distinct().toList();conditions=List.copyOf(conditions);queries=List.copyOf(queries);}
    }
    private final Map<ResolutionContracts.ProgramUnitId,Facts> units;
    private NominalValueSemantics(Map<ResolutionContracts.ProgramUnitId,Facts> units){this.units=Map.copyOf(units);}
    public Optional<Facts> facts(ResolutionContracts.ProgramUnitId unit){return Optional.ofNullable(units.get(unit));}
    static NominalValueSemantics analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,
            Map<ResolutionContracts.SemanticEntityId,ScalarMoveSemantics.ScalarText> shapes,Optional<StorageAccessSemantics> storage) {
        var result=new HashMap<ResolutionContracts.ProgramUnitId,Facts>();
        if(storage.isEmpty())return new NominalValueSemantics(result);
        var predicates=TextConditionSemantics.analyze(frontend,resolution,shapes,true);
        for(var unit:frontend.compilationUnit().programUnits()) {
            var nodes=new HashMap<ResolutionContracts.SemanticEntityId,String>();var symbols=new ArrayList<NominalValues.Symbol>();
            for(var n:storage.get().layout().layout(unit.id()).nodes())n.entity().ifPresent(e->{
                var shape=shapes.get(e);if(shape!=null){String id="storage-node:"+n.id().node();nodes.put(e,id);symbols.add(new NominalValues.Symbol(id,shape.extent()));}});
            var references=new HashMap<Integer,String>();
            for(var r:resolution.entries())if(r.occurrence().programUnitId().equals(unit.id())&&r.status()==ResolutionContracts.ResolutionStatus.RESOLVED)
                r.selectedCandidate().map(c->nodes.get(c.entityId())).ifPresent(node->references.put(r.occurrence().referenceAstNodeId(),node));
            var assignments=new ArrayList<Assignment>();var conditions=new ArrayList<Condition>();var queries=new ArrayList<Query>();
            var todo=new ArrayDeque<Ast.Node>();todo.add(unit.program());
            while(!todo.isEmpty()) {
                var n=todo.removeFirst();if(n instanceof Ast.Program&&n!=unit.program())continue;
                if(n instanceof Ast.MoveStatement m&&!m.corresponding()) {
                    var source=term(m.source(),references);
                    for(var receiver:m.targets()) {
                        var target=term(receiver,references);
                        if(target.kind().equals("READ"))assignments.add(new Assignment(m.meta().id(),target.value(),source));
                    }
                }
                if(n instanceof Ast.IfStatement branch) {
                    var p=predicates.get(new ScalarMoveSemantics.NodeKey(unit.id(),branch.meta().id()));
                    if(p!=null)conditions.add(new Condition(branch.meta().id(),predicate(p,references)));
                }
                if(n instanceof Ast.CallStatement call){var t=term(call.target(),references);if(t.kind().equals("READ"))queries.add(new Query(call.meta().id(),t.value()));}
                if(n instanceof Ast.EmbeddedLanguageStatement embedded&&embedded.language()==Ast.EmbeddedLanguage.CICS)
                    for(var host:embedded.hostOperands())if(host.option().equals("PROGRAM")) {
                        var t=term(host.reference(),references);if(t.kind().equals("READ"))queries.add(new Query(embedded.meta().id(),t.value()));
                    }
                todo.addAll(Ast.children(n));
            }
            symbols.sort(Comparator.comparing(NominalValues.Symbol::node));
            assignments.sort(Comparator.comparingInt(Assignment::statement).thenComparing(Assignment::target));
            conditions.sort(Comparator.comparingInt(Condition::statement));queries.sort(Comparator.comparingInt(Query::statement));
            result.put(unit.id(),new Facts(symbols,assignments,conditions,queries));
        }
        return new NominalValueSemantics(result);
    }
    private static NominalValues.Term term(Ast.Expression e,Map<Integer,String> references) {
        { // Source coordinates may be approximate even when the parsed operand is structured.
            if(e instanceof Ast.DataReference ref&&ref.understanding()==Ast.ReferenceUnderstanding.STRUCTURED
                &&ref.subscriptGroups().isEmpty()&&ref.referenceModification()==null&&references.containsKey(ref.meta().id()))
                return new NominalValues.Term("READ",references.get(ref.meta().id()));
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
