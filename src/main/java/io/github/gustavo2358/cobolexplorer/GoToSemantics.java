package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Canonical local paragraph transfers; no source-text or downstream control inference. */
public final class GoToSemantics {
    public record Target(ResolutionContracts.SemanticEntityId identity, Ast.SourceProvenance paragraphOrigin) { }
    public record Facts(Optional<Target> target, Ast.SourceProvenance referenceOrigin,
                        Optional<Integer> entry, Optional<Ast.SourceProvenance> entryOrigin, List<String> gaps) {
        public Facts { gaps=List.copyOf(gaps); }
    }
    private final Map<ScalarMoveSemantics.NodeKey,Facts> facts;
    private GoToSemantics(Map<ScalarMoveSemantics.NodeKey,Facts> facts) { this.facts=Map.copyOf(facts); }
    public Facts fact(ResolutionContracts.ProgramUnitId unit,int statement) {
        return Objects.requireNonNull(facts.get(new ScalarMoveSemantics.NodeKey(unit,statement)));
    }
    public static boolean simple(Ast.GoToStatement g) {
        return g.goToKind()==Ast.GoToKind.SIMPLE && g.targets().size()==1 && g.dependingOn()==null;
    }
    static GoToSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,
            ReferenceResolution resolution,ResolutionAnalysisReport report) {
        var refs=new HashMap<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry>();
        for(var ref:resolution.entries()) refs.put(new ScalarMoveSemantics.NodeKey(ref.occurrence().programUnitId(),ref.occurrence().referenceAstNodeId()),ref);
        var result=new HashMap<ScalarMoveSemantics.NodeKey,Facts>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var nodes=new HashMap<Integer,Ast.Node>(); var transfers=new ArrayList<Ast.GoToStatement>();
            var pending=new ArrayDeque<Ast.Node>(); pending.push(unit.program()); boolean altered=false;
            while(!pending.isEmpty()) {
                var node=pending.pop(); nodes.put(node.meta().id(),node);
                if(node instanceof Ast.GoToStatement g)transfers.add(g);
                if(node instanceof Ast.Statement && node.meta().origin().grammarRule().equals("alterStatement"))altered=true;
                for(var child:Ast.children(node))pending.push(child);
            }
            var symbols=new HashMap<Integer,SymbolTable.Symbol>();
            for(var symbol:tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols())symbols.put(symbol.id(),symbol);
            var procedures=unit.program().divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).toList();
            boolean input=procedures.size()==1 && procedures.get(0).procedureEntry()
                .filter(e->!e.declarativesPresent() && e.inputProof().unaffectedBy(report.frontendState())).isPresent();
            for(var g:transfers) {
                if(!simple(g))continue;
                var gaps=new ArrayList<String>(); var source=g.targets().get(0);
                if(!input)gaps.add("GO_TO_PROCEDURE_INPUT_NOT_PROVEN");
                if(altered)gaps.add("GO_TO_ALTER_IN_UNIT");
                var ref=refs.get(new ScalarMoveSemantics.NodeKey(unit.id(),source.meta().id()));
                Ast.Paragraph paragraph=null; Target target=null;
                if(ref!=null && ref.occurrence().role()==ResolutionContracts.ReferenceRole.GO_TO_TARGET
                        && ref.status()==ResolutionContracts.ResolutionStatus.RESOLVED && ref.candidates().size()==1
                        && ref.selectedCandidate().isPresent()) {
                    var id=ref.selectedCandidate().orElseThrow().entityId(); var symbol=symbols.get(id.localId());
                    if(id.programUnitId().equals(unit.id()) && id.domain()==ResolutionContracts.SemanticEntityDomain.PROCEDURE_SYMBOL
                            && symbol!=null && symbol.namespace()==SymbolTable.Namespace.PROCEDURE && symbol.kind()==SymbolTable.SymbolKind.PARAGRAPH
                            && nodes.get(symbol.declarationAstNodeId()) instanceof Ast.Paragraph p) {
                        paragraph=p; target=new Target(id,p.meta().provenance());
                    }
                }
                if(target==null)gaps.add("GO_TO_TARGET_NOT_UNIQUE_LOCAL_PARAGRAPH");
                var entry=paragraph==null?Optional.<Integer>empty():paragraph.executableEntry();
                var executable=entry.map(nodes::get).filter(Ast.Statement.class::isInstance);
                if(executable.isEmpty())gaps.add("GO_TO_TARGET_ENTRY_UNAVAILABLE");
                if(!g.meta().provenance().exact() || !source.meta().provenance().exact()
                        || paragraph!=null && !paragraph.meta().provenance().exact()
                        || executable.filter(n->!n.meta().provenance().exact()).isPresent())gaps.add("GO_TO_PROVENANCE_INCOMPLETE");
                // Keep known identity, but never a precise destination when control may be altered.
                boolean precise=gaps.isEmpty();
                result.put(new ScalarMoveSemantics.NodeKey(unit.id(),g.meta().id()),new Facts(Optional.ofNullable(target),source.meta().provenance(),
                    precise?entry:Optional.empty(),precise?executable.map(n->n.meta().provenance()):Optional.empty(),gaps));
            }
        }
        return new GoToSemantics(result);
    }
}
