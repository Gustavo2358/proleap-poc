package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Fixed RENAMES proof over canonical binding and an already prepared physical record. */
final class StorageRenames {
    private StorageRenames() { }
    private record Range(Key root, java.math.BigInteger start, java.math.BigInteger length) { }
    private record Proven(StorageComponents.Position position, Key owner, Integer from, Integer through, Ast.RenamesClause clause, Range range, Kind kind) { }
    static List<LogicalView> logical(ResolutionContracts.ProgramUnitId unit, StorageComponents.Unit structure,
            ReferenceResolution resolution, Map<Integer,ResolutionContracts.SemanticEntityId> entities,
            Map<Integer,SemanticCoverage.Finding> coverage,List<Node> nodes,List<LogicalView> views) {
        var ranges=new HashMap<Integer,Range>();for(var v:views)ranges.put(v.node().node(),new Range(v.root(),v.start(),v.length()));
        var kinds=new HashMap<Integer,Kind>();for(var n:nodes)kinds.put(n.id().node(),n.kind());
        return proveRanges(unit,structure,resolution,entities,coverage,ranges,kinds).stream().filter(p->p.range()!=null)
            .map(p->new LogicalView(p.owner(),p.range().root(),p.range().start(),p.range().length())).toList();
    }
    static List<Renaming> prove(ResolutionContracts.ProgramUnitId unit, StorageComponents.Unit physical,
            ReferenceResolution resolution, Map<Integer,ResolutionContracts.SemanticEntityId> entities,
            Map<Integer,SemanticCoverage.Finding> coverage, List<Node> nodes, List<View> views) {
        var ranges=new HashMap<Integer,Range>();var original=new HashMap<Integer,View>();var kinds=new HashMap<Integer,Kind>();
        for(var v:views){original.put(v.node().node(),v);if(v.textual()&&v.offset().value().isPresent()&&v.extent().value().isPresent())ranges.put(v.node().node(),new Range(v.base(),v.offset().value().orElseThrow(),v.extent().value().orElseThrow()));}
        for(var n:nodes)kinds.put(n.id().node(),n.kind());
        var result=new ArrayList<Renaming>();
        for(var p:proveRanges(unit,physical,resolution,entities,coverage,ranges,kinds)) {
            boolean proved=p.range()!=null;var root=original.get(p.position().root());
            var offset=proved?Measure.known(p.range().start()):Measure.unknown(Reason.RENAMES_NOT_PROVEN);
            var extent=proved?Measure.known(p.range().length()):Measure.unknown(Reason.RENAMES_NOT_PROVEN);
            var data=p.position().data();
            nodes.add(new Node(p.owner(),p.position().parent().map(id->new Key(unit,id)),p.position().order(),data.filler(),p.kind(),Optional.ofNullable(entities.get(data.meta().id())),extent,data.meta().provenance()));
            views.add(new View(p.owner(),root.base(),offset,extent,proved,data.meta().provenance()));
            if(p.clause()!=null)result.add(new Renaming(p.owner(),Optional.ofNullable(p.from()).map(id->new Key(unit,id)),Optional.ofNullable(p.through()).map(id->new Key(unit,id)),proved,p.clause()));
        }
        return List.copyOf(result);
    }
    private static List<Proven> proveRanges(ResolutionContracts.ProgramUnitId unit, StorageComponents.Unit physical,
            ReferenceResolution resolution,Map<Integer,ResolutionContracts.SemanticEntityId> entities,Map<Integer,SemanticCoverage.Finding> coverage,
            Map<Integer,Range> ranges,Map<Integer,Kind> kinds) {
        var byEntity=new HashMap<ResolutionContracts.SemanticEntityId,Integer>();
        entities.forEach((node,id)->byEntity.put(id,node));
        var selected=new HashMap<Integer,Integer>();
        for(var entry:resolution.entries())if(entry.occurrence().programUnitId().equals(unit)
                &&entry.status()==ResolutionContracts.ResolutionStatus.RESOLVED&&entry.candidates().size()==1)
            entry.selectedCandidate().ifPresent(c->{var node=byEntity.get(c.entityId());if(node!=null)selected.put(entry.occurrence().referenceAstNodeId(),node);});
        var positions=new HashMap<Integer,StorageComponents.Position>();

        var lastToken=new HashMap<Integer,Integer>();
        for(var p:physical.positions()) {
            positions.put(p.data().meta().id(),p);
            lastToken.merge(p.root(),p.data().meta().span().endToken(),Math::max);
        }
        var result=new ArrayList<Proven>();
        for(var p:physical.renames()) {
            var data=p.data();var key=new Key(unit,data.meta().id());
            var clauses=data.clauses().stream().filter(Ast.RenamesClause.class::isInstance).map(Ast.RenamesClause.class::cast).toList();
            var clause=clauses.size()==1?clauses.get(0):null;
            Integer from=clause==null?null:selected.get(clause.from().meta().id());
            Integer through=clause==null||clause.through()==null?null:selected.get(clause.through().meta().id());
            var root=ranges.get(p.root());var start=ranges.get(from);var end=clause!=null&&clause.through()==null?start:ranges.get(through);
            boolean proved=clause!=null&&data.clauses().size()==1&&data.children().isEmpty()
                &&data.visibility()==Ast.DeclarationVisibility.LOCAL&&modeled(data,coverage)&&modeled(clause,coverage)
                &&p.parent().equals(Optional.of(p.root()))&&positions.get(p.root()).data().level().equals("01")
                &&data.meta().span().startToken()>lastToken.get(p.root())
                &&root!=null
                &&endpoint(from,p.root(),positions)&&endpoint(clause.through()==null?from:through,p.root(),positions)
                &&start!=null&&end!=null
                &&start.root().equals(root.root())&&end.root().equals(root.root());
            if(proved&&clause.through()!=null) {
                proved=!from.equals(through)&&!descendant(through,from,positions)
                    &&end.start().compareTo(start.start())>=0
                    &&end.start().add(end.length()).compareTo(
                        start.start().add(start.length()))>=0;
            }
            var range=proved?new Range(root.root(),start.start(),end.start().add(end.length()).subtract(start.start())):null;
            var kind=proved?(clause.through()==null?kinds.get(from):Kind.GROUP):Kind.OPAQUE;
            result.add(new Proven(p,key,from,through,clause,range,kind));
        }
        return List.copyOf(result);
    }
    private static boolean endpoint(Integer node,int root,Map<Integer,StorageComponents.Position> positions) {
        var p=positions.get(node);
        return p!=null&&p.root()==root&&p.parent().isPresent()
            &&p.data().levelKind()==Ast.DataLevelKind.GROUP_OR_ELEMENTARY;
    }
    private static boolean descendant(int node,int ancestor,Map<Integer,StorageComponents.Position> positions) {
        var parent=positions.get(node).parent();
        while(parent.isPresent()) {if(parent.get()==ancestor)return true;parent=positions.get(parent.get()).parent();}
        return false;
    }
    private static boolean modeled(Ast.Node node,Map<Integer,SemanticCoverage.Finding> coverage) {
        var finding=coverage.get(node.meta().id());
        return finding!=null&&finding.coverage()==SemanticCoverage.ConstructionCoverage.MODELED;
    }
}
