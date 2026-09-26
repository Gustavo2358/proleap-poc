package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Typed source tree for a closed text comparison subset; no evaluation or control selection. */
public final class TextConditionSemantics {
    public enum Kind { EQUAL_TEXT, EQUAL_SPACES, EQUAL_LOW_VALUES, EQUAL_HIGH_VALUES, NOT, AND, OR }
    public record Predicate(Kind kind,Optional<Integer> reference,Optional<String> text,List<Predicate> children) {
        public Predicate {children=List.copyOf(children);}
        public Set<Integer> reads() {
            var out=new LinkedHashSet<Integer>();var todo=new ArrayDeque<Predicate>();todo.push(this);
            while(!todo.isEmpty()){var p=todo.pop();p.reference().ifPresent(out::add);p.children().forEach(todo::push);}
            return Set.copyOf(out);
        }
    }
    private record Result(Predicate predicate,Ast.DataReference subject,Ast.RelationOperator operator) { }
    public static Map<ScalarMoveSemantics.NodeKey,Predicate> analyze(CompilationUnitBuildResult frontend,
            ReferenceResolution resolution,Map<ResolutionContracts.SemanticEntityId,ScalarMoveSemantics.ScalarText> scalars) {
        var out=new HashMap<ScalarMoveSemantics.NodeKey,Predicate>();
        var byUnit=new HashMap<ResolutionContracts.ProgramUnitId,Set<Integer>>();
        for(var r:resolution.entries())if(r.status()==ResolutionContracts.ResolutionStatus.RESOLVED
                &&r.candidates().size()==1&&r.selectedCandidate().filter(c->scalars.containsKey(c.entityId())).isPresent())
            byUnit.computeIfAbsent(r.occurrence().programUnitId(),k->new HashSet<>()).add(r.occurrence().referenceAstNodeId());
        for(var unit:frontend.compilationUnit().programUnits()) {
            var eligible=byUnit.getOrDefault(unit.id(),Set.of());
            var todo=new ArrayDeque<Ast.Node>();todo.push(unit.program());
            while(!todo.isEmpty()) {
                var n=todo.pop();if(n instanceof Ast.Program&&n!=unit.program())continue;
                if(n instanceof Ast.IfStatement branch) {
                    var result=normalize(branch.condition(),null,Ast.RelationOperator.UNAVAILABLE,eligible);
                    if(result!=null)out.put(new ScalarMoveSemantics.NodeKey(unit.id(),branch.meta().id()),result.predicate());
                }
                Ast.children(n).forEach(todo::push);
            }
        }
        return Map.copyOf(out);
    }
    private static Result normalize(Ast.Expression e,Ast.DataReference subject,Ast.RelationOperator operator,Set<Integer> eligible) {
        if(!e.meta().provenance().exact())return null;
        if(e instanceof Ast.GroupedCondition g) {
            var result=normalize(g.inner(),null,Ast.RelationOperator.UNAVAILABLE,eligible);
            return result==null?null:new Result(result.predicate(),subject,operator);
        }
        if(e instanceof Ast.NegatedCondition n) {
            var result=normalize(n.operand(),subject,operator,eligible);
            return result==null?null:new Result(new Predicate(Kind.NOT,Optional.empty(),Optional.empty(),List.of(result.predicate())),result.subject(),result.operator());
        }
        if(e instanceof Ast.LogicalCondition logical) {
            var children=new ArrayList<Predicate>();
            for(var operand:logical.operands()) {
                var result=normalize(operand,subject,operator,eligible);if(result==null)return null;
                children.add(result.predicate());subject=result.subject();operator=result.operator();
            }
            return children.size()<2?null:new Result(new Predicate(Kind.valueOf(logical.connector().name()),Optional.empty(),Optional.empty(),children),subject,operator);
        }
        if(!(e instanceof Ast.RelationCondition relation))return null;
        if(relation.subject()!=null) {
            if(!(relation.subject() instanceof Ast.DataReference ref))return null;
            subject=ref;
        }
        if(relation.relationalOperator()!=null)operator=relation.operatorKind();
        if(operator!=Ast.RelationOperator.EQUAL||subject==null||!eligible.contains(subject.meta().id())
            ||subject.understanding()!=Ast.ReferenceUnderstanding.STRUCTURED||!subject.subscriptGroups().isEmpty()
            ||subject.referenceModification()!=null||!subject.meta().provenance().exact())return null;
        if(!(relation.object() instanceof Ast.LiteralExpression literal)||!literal.meta().provenance().exact())return null;
        Kind kind;Optional<String> text=Optional.empty();
        if(literal.logicalText().isPresent()){kind=Kind.EQUAL_TEXT;text=Optional.of(literal.logicalText().get().value());}
        else if(literal.figurativeText().isPresent())kind=Kind.valueOf("EQUAL_"+literal.figurativeText().get().name());
        else return null;
        return new Result(new Predicate(kind,Optional.of(subject.meta().id()),text,List.of()),subject,operator);
    }
}
