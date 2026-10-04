package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Integer item identity for logical MOVE and repetition controls, without assuming byte encoding. */
public final class IntegerSemantics {
    public record IntegerItem(int digits) { public IntegerItem { if(digits<=0)throw new IllegalArgumentException("positive digits"); } }
    private final Map<ResolutionContracts.SemanticEntityId,IntegerItem> declarations;
    private IntegerSemantics(Map<ResolutionContracts.SemanticEntityId,IntegerItem> declarations) { this.declarations=Map.copyOf(declarations); }
    static IntegerSemantics empty() { return new IntegerSemantics(Map.of()); }
    public Optional<IntegerItem> declaration(ResolutionContracts.SemanticEntityId id) { return Optional.ofNullable(declarations.get(id)); }
    static IntegerSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,boolean complete,StorageComponents components) {
        return analyze(frontend,tables,u->complete,components,Map.of());
    }
    static IntegerSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,
            java.util.function.Predicate<ResolutionContracts.ProgramUnitId> complete,StorageComponents components,
            Map<ResolutionContracts.ProgramUnitId,io.github.gustavo2358.cobolexplorer.semanticproduct.FactDependencies> graphs) {
        if(!components.belongsTo(frontend))throw new IllegalArgumentException("storage components belong to another snapshot");
        var result=new HashMap<ResolutionContracts.SemanticEntityId,IntegerItem>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var attributes=unit.program().attributes();
            if(attributes.initial()||attributes.recursive()||attributes.common()||attributes.library()||attributes.definition())continue;
            var graph=graphs.get(unit.id());var cells=new HashSet<String>();
            if(graph!=null) {
                var known=graph.proofAvailability();
                for(var fact:graph.facts())if(fact.kind()==io.github.gustavo2358.cobolexplorer.semanticproduct.FactDependencies.FactKind.LOCAL_CELL
                    &&fact.dependencies().stream().allMatch(p->Boolean.TRUE.equals(known.get(p))))cells.add(fact.subject());
            }
            var shapes=shapes(components.unit(unit.id()));
            var eligible=new HashMap<Integer,IntegerItem>();
            for(var position:components.unit(unit.id()).positions()) {
                var d=position.data();
                boolean independent=graph!=null?cells.contains("storage-node:"+d.meta().id()):
                    complete.test(unit.id())&&components.unit(unit.id()).standaloneIndependent(d.meta().id())
                    &&(d.level().equals("01")||d.levelKind()==Ast.DataLevelKind.STANDALONE_77);
                if(independent&&shapes.containsKey(d.meta().id()))eligible.put(d.meta().id(),shapes.get(d.meta().id()));
            }
            for(var symbol:tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols())
                if(symbol.namespace()==SymbolTable.Namespace.DATA&&symbol.kind()==SymbolTable.SymbolKind.DATA_ITEM&&eligible.containsKey(symbol.declarationAstNodeId()))
                    result.put(new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,symbol.id()),eligible.get(symbol.declarationAstNodeId()));
        }
        return new IntegerSemantics(result);
    }
    /** An explicit stack memoizes inherited usage once per declaration; no ancestor scan per MOVE. */
    static Map<Integer,IntegerItem> shapes(StorageComponents.Unit unit) {
        var positions=new HashMap<Integer,StorageComponents.Position>();unit.positions().forEach(p->positions.put(p.data().meta().id(),p));
        var display=new HashMap<Integer,Boolean>();var result=new HashMap<Integer,IntegerItem>();
        for(var position:unit.positions()) {
            int id=position.data().meta().id(),current=id;var path=new ArrayDeque<Integer>();
            while(!display.containsKey(current)) {
                path.push(current);var parent=positions.get(current).parent();
                if(parent.isEmpty())break;current=parent.orElseThrow();
            }
            boolean supported=display.getOrDefault(current,true);
            while(!path.isEmpty()) {
                int at=path.pop();var declaration=positions.get(at).data();
                supported&=declaration.clauses().stream().noneMatch(c->c instanceof Ast.UsageClause u&&!u.display()
                    ||c instanceof Ast.PreservedDataClause||c instanceof Ast.ValueClause value&&!knownValues(value));
                display.put(at,supported);
            }
            if(display.get(id))shape(position.data()).ifPresent(value->result.put(id,value));
        }
        return Map.copyOf(result);
    }
    private static boolean knownValues(Ast.ValueClause value) {
        return !value.ranges().isEmpty()&&value.ranges().stream().allMatch(r->r.first().kind()!=Ast.ConditionValueKind.UNAVAILABLE
            &&r.last().filter(v->v.kind()==Ast.ConditionValueKind.UNAVAILABLE).isEmpty());
    }
    static Optional<IntegerItem> shape(Ast.DataEntry d) {
        if(!d.meta().provenance().exact()||d.meta().syntheticModel()||d.filler()
            ||d.visibility()!=Ast.DeclarationVisibility.LOCAL
            ||d.children().stream().anyMatch(c->c.levelKind()!=Ast.DataLevelKind.CONDITION_88))return Optional.empty();
        int pictures=0,usages=0;Optional<Integer> digits=Optional.empty();
        for(var c:d.clauses()) {
            if(!c.meta().provenance().exact())return Optional.empty();
            if(c instanceof Ast.PictureClause p){pictures++;digits=p.integerDigits();}
            else if(c instanceof Ast.UsageClause u&&u.display())usages++;
            else if(!(c instanceof Ast.ValueClause value)||!knownValues(value))return Optional.empty();
        }
        return pictures==1&&usages<=1?digits.filter(n->n<=31).map(IntegerItem::new):Optional.empty();
    }
    public Optional<ResolutionContracts.SemanticEntityId> whole(Ast.Expression expression,ResolutionContracts.ProgramUnitId unit,
            Map<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry> references) {
        if(!(expression instanceof Ast.DataReference r)||!r.meta().provenance().exact()
            ||r.understanding()!=Ast.ReferenceUnderstanding.STRUCTURED||!r.subscriptGroups().isEmpty()||r.referenceModification()!=null)return Optional.empty();
        var binding=references.get(new ScalarMoveSemantics.NodeKey(unit,r.meta().id()));
        if(binding==null||binding.status()!=ResolutionContracts.ResolutionStatus.RESOLVED||binding.candidates().size()!=1)return Optional.empty();
        var id=binding.selectedCandidate().orElseThrow().entityId();return declaration(id).isPresent()?Optional.of(id):Optional.empty();
    }
}
