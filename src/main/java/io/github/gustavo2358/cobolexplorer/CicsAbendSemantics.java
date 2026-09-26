package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Immutable source event evidence. Does not read handler state or select a destination. */
public final class CicsAbendSemantics {
    public enum Eligibility { HANDLER_ELIGIBLE, HANDLERS_BYPASSED, UNAVAILABLE }
    public record Fact(String raw,Eligibility eligibility,List<CicsProgramControlAnalyzer.Option> options,List<String> gaps) {
        public Fact {Objects.requireNonNull(raw);Objects.requireNonNull(eligibility);options=List.copyOf(options);gaps=List.copyOf(gaps);}
    }
    private final CompilationUnitBuildResult owner;
    private final Map<CicsProgramControlAnalyzer.Key,Fact> facts;
    CicsAbendSemantics(CompilationUnitBuildResult owner,Map<CicsProgramControlAnalyzer.Key,Fact> facts) {
        this.owner=Objects.requireNonNull(owner);this.facts=Map.copyOf(facts);
    }
    public boolean belongsTo(CompilationUnitBuildResult frontend){return owner==frontend;}
    public Optional<Fact> fact(ResolutionContracts.ProgramUnitId unit,int statement){return Optional.ofNullable(facts.get(new CicsProgramControlAnalyzer.Key(unit,statement)));}
    public static CicsAbendSemantics analyze(CompilationUnitBuildResult frontend) {
        var result=new LinkedHashMap<CicsProgramControlAnalyzer.Key,Fact>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var todo=new ArrayDeque<Ast.Node>();todo.push(unit.program());
            while(!todo.isEmpty()) {
                var node=todo.pop();if(node instanceof Ast.Program&&node!=unit.program())continue;
                if(node instanceof Ast.EmbeddedLanguageStatement statement&&statement.language()==Ast.EmbeddedLanguage.CICS)
                    CicsAbendSyntax.parse(statement.rawText()).ifPresent(event->result.put(new CicsProgramControlAnalyzer.Key(unit.id(),node.meta().id()),
                        new Fact(event.raw(),Eligibility.valueOf(event.eligibility().name()),event.options().stream()
                            .map(o->new CicsProgramControlAnalyzer.Option(o.name(),o.operand(),o.start(),o.end())).toList(),event.gaps())));
                var children=Ast.children(node);for(int i=children.size()-1;i>=0;i--)todo.push(children.get(i));
            }
        }
        return new CicsAbendSemantics(frontend,result);
    }
}
