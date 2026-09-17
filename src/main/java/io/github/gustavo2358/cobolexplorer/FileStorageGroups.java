package io.github.gustavo2358.cobolexplorer;
import java.util.*;

/** Record allocation relations. Names are resolved upstream; this pass only joins published identities. */
final class FileStorageGroups {
    record Result(List<StorageComponents.Component> components,boolean allocationProved) {
        Result {components=List.copyOf(components);}
    }
    static Result build(CompilationUnitModel.ProgramUnit unit,List<Ast.FileDescription> descriptions,
            CompilationUnitSymbolTables tables,Map<Integer,ReferenceResolution.Entry> bindings) {
        var parent=new HashMap<Integer,Integer>();var records=new LinkedHashMap<Integer,List<Integer>>();
        var localOwners=new HashSet<Integer>();
        boolean proved=true;
        for(var fd:descriptions) {
            var ids=fd.entries().stream().filter(e->StorageComponents.level(e)==1).map(e->e.meta().id()).toList();
            if(ids.isEmpty())continue;records.put(fd.meta().id(),ids);parent.put(fd.meta().id(),fd.meta().id());
            proved&=fd.visibility()==Ast.DeclarationVisibility.LOCAL;
            if(fd.visibility()==Ast.DeclarationVisibility.LOCAL&&fd.recordClauses().stream().noneMatch(c->
                    c.form()==Ast.FileRecordForm.FIXED&&c.minimum().filter(n->n.signum()==0).isPresent()))localOwners.add(fd.meta().id());
        }
        var byEntity=new HashMap<ResolutionContracts.SemanticEntityId,List<Integer>>();
        var vsam=new HashSet<ResolutionContracts.SemanticEntityId>();
        var controls=new HashMap<Integer,Ast.FileBinding>();for(var division:unit.program().divisions())for(var child:division.children())if(child instanceof Ast.FileBinding f)controls.put(f.meta().id(),f);
        if(tables!=null) {
            var table=tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            for(var entity:table.entities())if(entity.kind()==SymbolTable.EntityKind.FILE) {
                var fds=new ArrayList<Integer>();
                var entityId=new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.FILE_ENTITY,entity.id());
                for(var symbol:entity.declarationSymbolIds()) {var id=table.symbols().get(symbol).declarationAstNodeId();if(records.containsKey(id))fds.add(id);if(FileAuxiliarySemantics.vsam(controls.get(id)))vsam.add(entityId);}
                byEntity.put(new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.FILE_ENTITY,entity.id()),fds);
            }
        }
        var named=new HashSet<Integer>();
        for(var division:unit.program().divisions())for(var child:division.children())if(child instanceof Ast.FileAreaSharing sharing) {
            // IBM pp156–158: SORT variants are documentary. AREA equals RECORD for
            // source-proven VSAM; sequential organization alone cannot distinguish access methods.
            if(sharing.kind()==Ast.FileAreaKind.SORT||sharing.kind()==Ast.FileAreaKind.SORT_MERGE)continue;
            var group=new ArrayList<Integer>();boolean valid=sharing.files().size()>=2;
            for(var reference:sharing.files()) {
                var binding=bindings.get(reference.meta().id());
                var owners=binding!=null&&binding.status()==ResolutionContracts.ResolutionStatus.RESOLVED
                    ?byEntity.getOrDefault(binding.candidates().get(0).entityId(),List.of()):List.<Integer>of();
                if(owners.size()!=1){valid=false;continue;}
                var owner=owners.get(0);if(!named.add(owner)||!localOwners.contains(owner))valid=false;
                if(sharing.kind()==Ast.FileAreaKind.AREA&&!vsam.contains(binding.candidates().get(0).entityId()))valid=false;
                group.add(owner);
            }
            if(valid)for(int i=1;i<group.size();i++)union(parent,group.get(0),group.get(i));else proved=false;
        }
        var groups=new LinkedHashMap<Integer,List<Integer>>();
        for(var record:records.entrySet())groups.computeIfAbsent(find(parent,record.getKey()),ignored->new ArrayList<>()).addAll(record.getValue());
        var components=new ArrayList<StorageComponents.Component>();
        for(var group:groups.values())components.add(new StorageComponents.Component(group.get(0),Optional.empty(),group));
        return new Result(components,proved);
    }
    private static int find(Map<Integer,Integer> parent,int node) {
        int root=node;while(parent.get(root)!=root)root=parent.get(root);
        while(node!=root){int next=parent.get(node);parent.put(node,root);node=next;}return root;
    }
    private static void union(Map<Integer,Integer> parent,int a,int b){parent.put(find(parent,b),find(parent,a));}
}
