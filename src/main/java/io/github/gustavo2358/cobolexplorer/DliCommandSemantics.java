package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Canonical bounded source effects, independent of nominal/storage admission. */
final class DliCommandSemantics {
    static Optional<StatementEffectSummary> effects(Ast.Statement statement) {
        if(!(statement instanceof Ast.EmbeddedLanguageStatement s)||s.language()!=Ast.EmbeddedLanguage.DLI)return Optional.empty();
        var parsed=DliCommandSyntax.parse(s.rawText());if(parsed.isEmpty())return Optional.empty();
        var byOffset=new HashMap<Integer,Ast.EmbeddedHostOperand>();
        for(var h:s.hostOperands())if(byOffset.put(h.optionStart(),h)!=null)return Optional.empty();
        if(byOffset.size()!=parsed.get().hosts().size())return Optional.empty();
        var reads=new ArrayList<Ast.DataReference>();var writes=new ArrayList<Ast.DataReference>();
        for(var h:parsed.get().hosts()) {
            var operand=byOffset.get(h.optionStart());if(operand==null||operand.role()!=h.role()||!operand.option().equals(h.option()))return Optional.empty();
            if(h.role()==Ast.EmbeddedHostRole.WRITE)writes.add(operand.reference());else reads.add(operand.reference());
        }
        return Optional.of(new StatementEffectSummary(reads,writes,List.of(),List.of(),
            StatementEffectSummary.Bound.NONE,StatementEffectSummary.Bound.NONE,StatementEffectSummary.Bound.NONE,
            StatementEffectSummary.Environment.UNKNOWN,StatementEffectSummary.ValueTransform.UNKNOWN,StatementEffectSummary.Proof.DLI_HOST_OPERANDS));
    }
}
