package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Source-observed CALL possibilities. This inventory is never a reachability graph
 * and never interprets a computed target, callee contract or missing COPY content. */
public record ObservedDependencyInventory(String schema,String version,List<Site> sites,
                                          List<ResolutionAnalysisReport.Gap> inputGaps) {
    public enum Knowledge { OBSERVED_ONLY }
    public enum Reachability { UNKNOWN }
    public record LiteralCandidate(String value,Ast.SourceProvenance provenance) { }
    public record Site(ResolutionContracts.ProgramUnitId unit,int statement,String command,
                       Knowledge knowledge,Reachability reachability,List<LiteralCandidate> literalCandidates,
                       boolean unknownRemainder,Ast.SourceProvenance provenance) {
        public Site { literalCandidates=List.copyOf(literalCandidates); }
    }
    public ObservedDependencyInventory { sites=List.copyOf(sites);inputGaps=List.copyOf(inputGaps); }
    public static ObservedDependencyInventory from(CompilationUnitBuildResult frontend,ResolutionAnalysisReport report) {
        var sites=new ArrayList<Site>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var pending=new ArrayDeque<Ast.Node>();pending.push(unit.program());
            while(!pending.isEmpty()) {
                var node=pending.pop();
                if(node instanceof Ast.CallStatement call) {
                    var candidates=new ArrayList<LiteralCandidate>();
                    if(call.targetSyntax()==Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME)
                        call.literalText().ifPresent(t->candidates.add(new LiteralCandidate(t.value(),call.target().meta().provenance())));
                    sites.add(new Site(unit.id(),call.meta().id(),"CALL",Knowledge.OBSERVED_ONLY,Reachability.UNKNOWN,
                        candidates,true,call.meta().provenance()));
                }
                var children=Ast.children(node);
                for(int i=children.size()-1;i>=0;i--)pending.push(children.get(i));
            }
        }
        return new ObservedDependencyInventory("cobol-observed-dependencies","1.0.0",sites,
            report.gaps().stream().filter(g->g.category()==ResolutionAnalysisReport.GapCategory.INPUT).toList());
    }
}
