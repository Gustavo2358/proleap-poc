package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Evaluation properties of loop conditions; never evaluates their truth. */
public final class PerformPredicateSemantics {
    public enum Profile { SCALAR_TEXT_EQUALITY, NUMERIC_RELATION, UNAVAILABLE }
    public record Predicate(IfSemantics.Availability availability,Profile profile,
            Map<Integer,ResolutionContracts.SemanticEntityId> wholeItems,Ast.SourceProvenance provenance) {
        public Predicate {wholeItems=Map.copyOf(wholeItems);}
    }
    static Predicate unavailable(Ast.SourceProvenance origin) {return new Predicate(IfSemantics.Availability.PARTIAL,Profile.UNAVAILABLE,Map.of(),origin);}
    static Predicate analyze(Ast.Expression expression,ResolutionContracts.ProgramUnitId unit,boolean complete,boolean numeric,
            Map<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry> reads,Map<ResolutionContracts.SemanticEntityId,ScalarMoveSemantics.ScalarText> scalars,
            NumericControlSemantics numbers,Map<Integer,SemanticCoverage.Finding> coverage) {
        var text=IfSemantics.predicate(expression,unit,complete,complete,reads,scalars,coverage,new long[5]);
        if(text.availability()==IfSemantics.Availability.KNOWN)return new Predicate(text.availability(),Profile.SCALAR_TEXT_EQUALITY,
            Map.of(text.readNode().orElseThrow(),text.wholeItem().orElseThrow()),text.provenance());
        if(complete&&numeric&&expression instanceof Ast.RelationCondition relation&&relation.meta().provenance().exact()
                &&relation.operatorKind()!=Ast.RelationOperator.UNAVAILABLE) {
            var items=new LinkedHashMap<Integer,ResolutionContracts.SemanticEntityId>();boolean known=true;
            for(var operand:List.of(relation.subject(),relation.object())) {
                if(!operand.meta().provenance().exact()){known=false;continue;}
                if(operand instanceof Ast.LiteralExpression l&&l.integerValue().isPresent())continue;
                var whole=numbers.whole(operand,unit,reads);var read=reads.get(new ScalarMoveSemantics.NodeKey(unit,operand.meta().id()));
                if(whole.isEmpty()||read==null||read.occurrence().role()!=ResolutionContracts.ReferenceRole.VALUE_READ){known=false;continue;}
                items.put(operand.meta().id(),whole.get());
            }
            if(known)return new Predicate(IfSemantics.Availability.KNOWN,Profile.NUMERIC_RELATION,items,expression.meta().provenance());
        }
        return new Predicate(text.availability(),Profile.UNAVAILABLE,Map.of(),expression.meta().provenance());
    }
}
