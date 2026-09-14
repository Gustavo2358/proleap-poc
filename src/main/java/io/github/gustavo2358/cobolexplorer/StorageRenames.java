package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Fixed RENAMES proof over canonical binding and an already prepared physical record. */
final class StorageRenames {
    private StorageRenames() { }
    static List<Renaming> prove(ResolutionContracts.ProgramUnitId unit, StorageComponents.Unit physical,
            ReferenceResolution resolution, Map<Integer,ResolutionContracts.SemanticEntityId> entities,
            Map<Integer,SemanticCoverage.Finding> coverage, List<Node> nodes, List<View> views) {
        var byEntity=new HashMap<ResolutionContracts.SemanticEntityId,Integer>();
        entities.forEach((node,id)->byEntity.put(id,node));
        var selected=new HashMap<Integer,Integer>();
        for(var entry:resolution.entries())if(entry.occurrence().programUnitId().equals(unit)
                &&entry.status()==ResolutionContracts.ResolutionStatus.RESOLVED&&entry.candidates().size()==1)
            entry.selectedCandidate().ifPresent(c->{var node=byEntity.get(c.entityId());if(node!=null)selected.put(entry.occurrence().referenceAstNodeId(),node);});
        var positions=new HashMap<Integer,StorageComponents.Position>();
        var byNode=new HashMap<Integer,View>();var kinds=new HashMap<Integer,Kind>();
        var lastToken=new HashMap<Integer,Integer>();
        for(var p:physical.positions()) {
            positions.put(p.data().meta().id(),p);
            lastToken.merge(p.root(),p.data().meta().span().endToken(),Math::max);
        }
        for(var v:views)byNode.put(v.node().node(),v);
        for(var n:nodes)kinds.put(n.id().node(),n.kind());
        var result=new ArrayList<Renaming>();
        for(var p:physical.renames()) {
            var data=p.data();var key=new Key(unit,data.meta().id());
            var clauses=data.clauses().stream().filter(Ast.RenamesClause.class::isInstance).map(Ast.RenamesClause.class::cast).toList();
            var clause=clauses.size()==1?clauses.get(0):null;
            Integer from=clause==null?null:selected.get(clause.from().meta().id());
            Integer through=clause==null||clause.through()==null?null:selected.get(clause.through().meta().id());
            var root=byNode.get(p.root());var start=byNode.get(from);var end=clause!=null&&clause.through()==null?start:byNode.get(through);
            boolean proved=clause!=null&&data.clauses().size()==1&&data.children().isEmpty()
                &&data.visibility()==Ast.DeclarationVisibility.LOCAL&&modeled(data,coverage)&&modeled(clause,coverage)
                &&p.parent().equals(Optional.of(p.root()))&&positions.get(p.root()).data().level().equals("01")
                &&data.meta().span().startToken()>lastToken.get(p.root())
                &&root!=null&&root.textual()&&root.extent().value().isPresent()
                &&endpoint(from,p.root(),positions)&&endpoint(clause.through()==null?from:through,p.root(),positions)
                &&start!=null&&end!=null&&start.textual()&&end.textual()
                &&start.base().equals(root.base())&&end.base().equals(root.base())
                &&start.offset().value().isPresent()&&end.offset().value().isPresent();
            if(proved&&clause.through()!=null) {
                proved=!from.equals(through)&&!descendant(through,from,positions)
                    &&end.offset().value().get().compareTo(start.offset().value().get())>=0
                    &&end.offset().value().get().add(end.extent().value().orElseThrow()).compareTo(
                        start.offset().value().get().add(start.extent().value().orElseThrow()))>=0;
            }
            var offset=proved?start.offset():Measure.unknown(Reason.RENAMES_NOT_PROVEN);
            var extent=proved?Measure.known(end.offset().value().orElseThrow().add(end.extent().value().orElseThrow()).subtract(start.offset().value().orElseThrow()))
                :Measure.unknown(Reason.RENAMES_NOT_PROVEN);
            var kind=proved?(clause.through()==null?kinds.get(from):Kind.GROUP):Kind.OPAQUE;
            nodes.add(new Node(key,p.parent().map(id->new Key(unit,id)),p.order(),data.filler(),kind,
                Optional.ofNullable(entities.get(data.meta().id())),extent,data.meta().provenance()));
            views.add(new View(key,root.base(),offset,extent,proved,data.meta().provenance()));
            if(clause!=null)result.add(new Renaming(key,Optional.ofNullable(from).map(id->new Key(unit,id)),
                Optional.ofNullable(through).map(id->new Key(unit,id)),proved,clause));
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
