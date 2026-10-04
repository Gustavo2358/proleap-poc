package io.github.gustavo2358.cobolexplorer;
import java.math.BigInteger;
import java.util.*;
/** Source-owned numeric MOVE proofs. No interval enumeration or physical representation inference. */
public final class IntegerMoveSemantics {
    public record Transfer(int target,ResolutionContracts.SemanticEntityId receiver,
        Optional<ResolutionContracts.SemanticEntityId> source,Optional<BigInteger> value) { }
    private final Map<ScalarMoveSemantics.NodeKey,List<Transfer>> moves;
    private IntegerMoveSemantics(Map<ScalarMoveSemantics.NodeKey,List<Transfer>> moves){this.moves=Map.copyOf(moves);}
    public List<Transfer> transfers(ResolutionContracts.ProgramUnitId unit,int statement){return moves.getOrDefault(new ScalarMoveSemantics.NodeKey(unit,statement),List.of());}
    public Optional<ResolutionContracts.SemanticEntityId> source(ResolutionContracts.ProgramUnitId unit,int statement) {
        var facts=transfers(unit,statement);return facts.isEmpty()?Optional.empty():facts.get(0).source();
    }
    private static Optional<BigInteger> integral(Ast.LiteralExpression literal) {
        if(literal.integerValue().isPresent())return literal.integerValue();
        // Fixed-point spelling only; conversion allocates at most its source digit count.
        return literal.numericValue().filter(v->v.scale()>=0&&v.stripTrailingZeros().scale()<=0).map(java.math.BigDecimal::toBigIntegerExact);
    }
    static IntegerMoveSemantics analyze(CompilationUnitBuildResult frontend,IntegerSemantics numbers,
            Map<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry> references) {
        var result=new HashMap<ScalarMoveSemantics.NodeKey,List<Transfer>>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var pending=new ArrayDeque<Ast.Node>();pending.push(unit.program());
            while(!pending.isEmpty()) {
                var node=pending.pop();Ast.children(node).forEach(pending::push);
                if(!(node instanceof Ast.MoveStatement move)||move.corresponding()||!move.meta().provenance().exact())continue;
                var source=numbers.whole(move.source(),unit.id(),references);
                Optional<BigInteger> literal=move.source() instanceof Ast.LiteralExpression l&&l.meta().provenance().exact()?integral(l):Optional.empty();
                if(source.isEmpty()&&literal.isEmpty())continue;
                if(literal.filter(v->v.signum()<0).isPresent())continue;
                int digits=source.map(s->numbers.declaration(s).orElseThrow().digits()).orElseGet(()->literal.orElseThrow().toString().length());
                var transfers=new ArrayList<Transfer>();boolean sourcePreserved=true;
                for(var target:move.targets()) {
                    var receiver=numbers.whole(target,unit.id(),references);
                    if(sourcePreserved&&receiver.isPresent()&&numbers.declaration(receiver.orElseThrow()).orElseThrow().digits()>=digits)
                        transfers.add(new Transfer(target.meta().id(),receiver.orElseThrow(),source,literal));
                    else if(source.isPresent())sourcePreserved=false;
                }
                // DATA reads are exact through the proved prefix. An unknown peer may change the sending cell;
                // subsequent receivers retain uncertainty without inventing a snapshot of its old value.
                if(!transfers.isEmpty())result.put(new ScalarMoveSemantics.NodeKey(unit.id(),move.meta().id()),List.copyOf(transfers));
            }
        }
        return new IntegerMoveSemantics(result);
    }
}
