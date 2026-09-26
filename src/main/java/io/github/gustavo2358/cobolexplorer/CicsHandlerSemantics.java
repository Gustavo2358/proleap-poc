package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Positive operation facts. Does not execute registrations or propagate handler state. */
public final class CicsHandlerSemantics {
    public enum Action { ACTIVATE, CANCEL, RESET, UNAVAILABLE }
    public enum TargetKind { LABEL, PROGRAM, NONE, UNAVAILABLE }
    public record LabelTarget(ResolutionContracts.SemanticEntityId identity,Ast.SourceProvenance declarationOrigin,
                              Optional<Integer> entry,Optional<Ast.SourceProvenance> entryOrigin) { }
    public record Fact(String raw,Action action,TargetKind targetKind,Optional<String> targetSyntax,
                       Optional<ResolutionContracts.ResolutionStatus> labelBindingStatus,Optional<LabelTarget> labelTarget,
                       Optional<Ast.SourceProvenance> targetOrigin,Optional<String> programLiteral,
                       List<CicsProgramControlAnalyzer.Option> options,List<String> gaps) {
        public Fact { options=List.copyOf(options);gaps=List.copyOf(gaps); }
        public boolean registrationPreservesApplicationMemory() {
            return gaps.isEmpty() && (action==Action.CANCEL || action==Action.RESET
                || action==Action.ACTIVATE && targetKind==TargetKind.LABEL && labelTarget.flatMap(LabelTarget::entry).isPresent())
                && options.stream().allMatch(o->Set.of("ABEND","LABEL","CANCEL","RESET","NOHANDLE").contains(o.name()));
        }
    }
    private final CompilationUnitBuildResult owner;
    private final Map<CicsProgramControlAnalyzer.Key,Fact> facts;
    private CicsHandlerSemantics(CompilationUnitBuildResult owner,Map<CicsProgramControlAnalyzer.Key,Fact> facts) {
        this.owner=owner;this.facts=Map.copyOf(facts);
    }
    public boolean belongsTo(CompilationUnitBuildResult frontend){return owner==frontend;}
    public Optional<Fact> fact(ResolutionContracts.ProgramUnitId unit,int statement){return Optional.ofNullable(facts.get(new CicsProgramControlAnalyzer.Key(unit,statement)));}
    static CicsHandlerSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,ReferenceResolution resolution) {
        var refs=new HashMap<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry>();
        for(var ref:resolution.entries())refs.put(new ScalarMoveSemantics.NodeKey(ref.occurrence().programUnitId(),ref.occurrence().referenceAstNodeId()),ref);
        var result=new LinkedHashMap<CicsProgramControlAnalyzer.Key,Fact>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var nodes=new LinkedHashMap<Integer,Ast.Node>();var todo=new ArrayDeque<Ast.Node>();todo.push(unit.program());
            while(!todo.isEmpty()) {var n=todo.pop();if(n instanceof Ast.Program&&n!=unit.program())continue;nodes.put(n.meta().id(),n);var children=Ast.children(n);for(int i=children.size()-1;i>=0;i--)todo.push(children.get(i));}
            var symbols=new HashMap<Integer,SymbolTable.Symbol>();
            for(var symbol:tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols())symbols.put(symbol.id(),symbol);
            for(var node:nodes.values())if(node instanceof Ast.EmbeddedLanguageStatement statement&&statement.language()==Ast.EmbeddedLanguage.CICS) {
                var parsed=CicsHandlerSyntax.parse(statement.rawText());if(parsed.isEmpty())continue;var operation=parsed.get();
                var gaps=new LinkedHashSet<>(operation.gaps());Optional<LabelTarget> target=Optional.empty();
                Optional<ResolutionContracts.ResolutionStatus> status=Optional.empty();
                Optional<Ast.SourceProvenance> origin=statement.operandAnchors().stream().map(Ast.EmbeddedOperandAnchor::provenance)
                        .reduce((a,b)->{throw new IllegalArgumentException("handler has one target operand anchor");});
                Optional<String> literal=Optional.empty();
                if(operation.targetKind()==CicsHandlerSyntax.TargetKind.LABEL) {
                    var reference=statement.procedureOperands().size()==1?statement.procedureOperands().get(0):null;
                    var resolved=reference==null?null:refs.get(new ScalarMoveSemantics.NodeKey(unit.id(),reference.meta().id()));
                    if(reference!=null)origin=Optional.of(reference.meta().provenance());
                    status=Optional.of(resolved==null?ResolutionContracts.ResolutionStatus.UNRESOLVED:resolved.status());
                    if(resolved!=null&&resolved.occurrence().role()==ResolutionContracts.ReferenceRole.CICS_HANDLER_TARGET
                            &&resolved.status()==ResolutionContracts.ResolutionStatus.RESOLVED&&resolved.candidates().size()==1&&resolved.selectedCandidate().isPresent()) {
                        var id=resolved.selectedCandidate().get().entityId();var symbol=symbols.get(id.localId());
                        if(id.programUnitId().equals(unit.id())&&id.domain()==ResolutionContracts.SemanticEntityDomain.PROCEDURE_SYMBOL&&symbol!=null&&symbol.namespace()==SymbolTable.Namespace.PROCEDURE) {
                            var declaration=nodes.get(symbol.declarationAstNodeId());
                            if(declaration instanceof Ast.Paragraph||declaration instanceof Ast.Section) {
                                var entry=declaration instanceof Ast.Paragraph p?p.executableEntry():Optional.<Integer>empty();
                                var executable=entry.map(nodes::get).filter(Ast.Statement.class::isInstance);
                                target=Optional.of(new LabelTarget(id,declaration.meta().provenance(),executable.map(n->n.meta().id()),executable.map(n->n.meta().provenance())));
                            }
                        }
                    }
                    if(target.isEmpty()) {gaps.add("CICS_HANDLER_LABEL_BINDING_UNAVAILABLE");if(status.get()==ResolutionContracts.ResolutionStatus.RESOLVED)status=Optional.of(ResolutionContracts.ResolutionStatus.UNRESOLVED);}
                    else if(target.get().entry().isEmpty())gaps.add("CICS_HANDLER_LABEL_ENTRY_UNAVAILABLE");
                }
                if(operation.targetKind()==CicsHandlerSyntax.TargetKind.PROGRAM) {
                    var host=statement.hostOperands().stream().filter(h->h.option().equals("PROGRAM"))
                            .reduce((a,b)->{throw new IllegalArgumentException("handler PROGRAM has one operand identity");});
                    if(host.isPresent())origin=Optional.of(host.get().reference().meta().provenance());
                    var syntax=operation.targetSyntax().orElseThrow().strip();
                    if(syntax.startsWith("'")||syntax.startsWith("\"")) {literal=CicsCommandSyntax.literal(syntax);if(literal.isEmpty())gaps.add("CICS_HANDLER_INVALID_PROGRAM_LITERAL");}
                }
                result.put(new CicsProgramControlAnalyzer.Key(unit.id(),node.meta().id()),new Fact(operation.raw(),Action.valueOf(operation.action().name()),TargetKind.valueOf(operation.targetKind().name()),operation.targetSyntax(),status,target,origin,literal,
                    operation.options().stream().map(o->new CicsProgramControlAnalyzer.Option(o.name(),o.operand(),o.start(),o.end())).toList(),List.copyOf(gaps)));
            }
        }
        return new CicsHandlerSemantics(frontend,result);
    }
}
