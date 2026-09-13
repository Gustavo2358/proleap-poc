package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Integer item identity for repetition controls, without a runtime numeric value. */
public final class NumericControlSemantics {
    public record IntegerItem(int digits) { public IntegerItem { if(digits<=0)throw new IllegalArgumentException("positive digits"); } }
    private final Map<ResolutionContracts.SemanticEntityId,IntegerItem> declarations;
    private NumericControlSemantics(Map<ResolutionContracts.SemanticEntityId,IntegerItem> declarations) { this.declarations=Map.copyOf(declarations); }
    static NumericControlSemantics empty() { return new NumericControlSemantics(Map.of()); }
    public Optional<IntegerItem> declaration(ResolutionContracts.SemanticEntityId id) { return Optional.ofNullable(declarations.get(id)); }
    static NumericControlSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,boolean complete) {
        var result=new HashMap<ResolutionContracts.SemanticEntityId,IntegerItem>();
        if(!complete)return empty();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var attributes=unit.program().attributes();
            if(attributes.initial()||attributes.recursive()||attributes.common()||attributes.library()||attributes.definition())continue;
            var sections=new ArrayList<Ast.Section>();var pending=new ArrayDeque<Ast.Node>();pending.push(unit.program());
            boolean relevant=false;
            while(!pending.isEmpty()) {
                var n=pending.pop();Ast.children(n).forEach(pending::push);
                if(n instanceof Ast.PerformStatement p && p.performKind()==Ast.PerformKind.PROCEDURE
                    && (p.repetition()==Ast.PerformRepetition.TIMES||p.repetition()==Ast.PerformRepetition.VARYING))relevant=true;
                if(n instanceof Ast.Section s&&s.dataSectionKind()==Ast.DataSectionKind.WORKING_STORAGE)sections.add(s);
            }
            if(!relevant)continue;
            var coverage=new HashMap<Integer,SemanticCoverage.Finding>();
            for(var f:frontend.coverageByProgramUnit().get(unit.id()).findings())coverage.put(f.astNodeId(),f);
            var eligible=new HashMap<Integer,IntegerItem>();
            for(var section:sections) {
                boolean overlay=false;pending.add(section);
                while(!pending.isEmpty()) {
                    var n=pending.pop();Ast.children(n).forEach(pending::push);
                    if(n instanceof Ast.RedefinesClause||n instanceof Ast.RenamesClause||n instanceof Ast.PreservedDataClause)overlay=true;
                }
                if(overlay)continue;
                for(var child:section.children())if(child instanceof Ast.DataEntry d && d.children().isEmpty()&&!d.filler()
                        &&d.visibility()==Ast.DeclarationVisibility.LOCAL&&(d.level().equals("01")||d.levelKind()==Ast.DataLevelKind.STANDALONE_77)) {
                    int pictures=0,usages=0;Optional<Integer> digits=Optional.empty();boolean valid=modeled(d,coverage);
                    for(var clause:d.clauses()) {
                        valid&=modeled(clause,coverage);
                        if(clause instanceof Ast.PictureClause p){pictures++;digits=p.integerDigits();}
                        else if(clause instanceof Ast.UsageClause u&&u.display())usages++;
                        else valid=false;
                    }
                    if(valid&&pictures==1&&usages<=1&&digits.isPresent())eligible.put(d.meta().id(),new IntegerItem(digits.get()));
                }
            }
            for(var symbol:tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols())
                if(symbol.namespace()==SymbolTable.Namespace.DATA&&symbol.kind()==SymbolTable.SymbolKind.DATA_ITEM&&eligible.containsKey(symbol.declarationAstNodeId()))
                    result.put(new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,symbol.id()),eligible.get(symbol.declarationAstNodeId()));
        }
        return new NumericControlSemantics(result);
    }
    private static boolean modeled(Ast.Node n,Map<Integer,SemanticCoverage.Finding> coverage) {
        var f=coverage.get(n.meta().id());return n.meta().provenance().exact()&&f!=null&&f.coverage()==SemanticCoverage.ConstructionCoverage.MODELED;
    }
    public Optional<ResolutionContracts.SemanticEntityId> whole(Ast.Expression expression,ResolutionContracts.ProgramUnitId unit,
            Map<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry> references) {
        if(!(expression instanceof Ast.DataReference r)||!r.meta().provenance().exact()
            ||r.understanding()!=Ast.ReferenceUnderstanding.STRUCTURED||!r.qualifiers().isEmpty()||!r.subscriptGroups().isEmpty()||r.referenceModification()!=null)return Optional.empty();
        var binding=references.get(new ScalarMoveSemantics.NodeKey(unit,r.meta().id()));
        if(binding==null||binding.status()!=ResolutionContracts.ResolutionStatus.RESOLVED||binding.candidates().size()!=1)return Optional.empty();
        var id=binding.selectedCandidate().orElseThrow().entityId();return declaration(id).isPresent()?Optional.of(id):Optional.empty();
    }
}
