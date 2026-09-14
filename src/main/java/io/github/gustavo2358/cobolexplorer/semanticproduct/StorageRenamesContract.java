package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

/** Closure of source-proved aliases. Does not resolve or interpret COBOL names. */
final class StorageRenamesContract {
    private StorageRenamesContract() { }
    static void validate(UnitId unit,StorageInventory inventory,Map<StorageNodeId,PhysicalNode> nodes,
            Map<StorageNodeId,StorageView> views,Set<StorageRelationId> relationIds) {
        var owners=new HashSet<StorageNodeId>();
        for(var r:inventory.renames()) {
            require(r.id().unit().equals(unit)&&relationIds.add(r.id())&&owners.add(r.owner()),"duplicate or foreign RENAMES identity");
            require(nodes.containsKey(r.owner()),"RENAMES owner must exist");
            r.from().ifPresent(id->require(nodes.containsKey(id),"RENAMES from must exist"));
            r.through().ifPresent(id->require(nodes.containsKey(id),"RENAMES through must exist"));
        }
        var roots=new HashMap<StorageNodeId,StorageNodeId>();
        for(var node:nodes.keySet()) {
            var path=new ArrayList<StorageNodeId>();var cursor=node;
            while(!roots.containsKey(cursor)&&nodes.get(cursor).parent().isPresent()) {path.add(cursor);cursor=nodes.get(cursor).parent().get();}
            var root=roots.getOrDefault(cursor,cursor);roots.put(cursor,root);for(var p:path)roots.put(p,root);
        }
        var open=new HashSet<StorageNodeId>();
        for(var node:nodes.values())if(!owners.contains(node.id())) {
            var v=views.get(node.id());
            if(v.codec().isEmpty()||v.offset().value().isEmpty()||v.extent().value().isEmpty())open.add(roots.get(node.id()));
        }
        for(var r:inventory.renames()) {
            var owner=nodes.get(r.owner());var view=views.get(r.owner());
            if(r.status()==StorageRelationStatus.UNPROVEN) {
                require(view.codec().isEmpty()&&view.offset().value().isEmpty()&&view.extent().value().isEmpty(),"unproved RENAMES cannot publish precise storage");continue;
            }
            var first=nodes.get(r.from().orElseThrow());var last=nodes.get(r.through().orElse(r.from().get()));
            require(!owners.contains(first.id())&&!owners.contains(last.id())&&first.parent().isPresent()&&last.parent().isPresent(),"RENAMES endpoints require physical children");
            var root=roots.get(owner.id());
            require(owner.parent().equals(Optional.of(root))&&root.equals(roots.get(first.id()))&&root.equals(roots.get(last.id()))&&!open.contains(root),"RENAMES requires one fully proved record");
            var a=views.get(first.id());var b=views.get(last.id());
            require(view.base().equals(a.base())&&view.base().equals(b.base())&&view.offset().equals(a.offset()),"RENAMES must reuse endpoint base and start");
            require(view.extent().value().isPresent()&&view.extent().value().get().equals(b.offset().value().orElseThrow().add(b.extent().value().orElseThrow()).subtract(a.offset().value().orElseThrow())),"RENAMES extent must end at final endpoint");
            require(view.codec().equals(a.codec())&&view.codec().equals(b.codec()),"RENAMES codec must agree with proved endpoints");
            if(r.through().isPresent()) {
                require(!first.id().equals(last.id())&&owner.kind()==PhysicalKind.GROUP,"THROUGH requires distinct endpoints and group interpretation");
                require(b.offset().value().get().compareTo(a.offset().value().get())>=0&&b.offset().value().get().add(b.extent().value().get()).compareTo(a.offset().value().get().add(a.extent().value().get()))>=0,"RENAMES endpoints must be ordered");
                var parent=last.parent();while(parent.isPresent()) {require(!parent.get().equals(first.id()),"RENAMES final endpoint cannot be subordinate to start");parent=nodes.get(parent.get()).parent();}
            } else require(owner.kind()==first.kind(),"single RENAMES inherits source category");
        }
    }
    private static void require(boolean condition,String reason) {if(!condition)throw new IllegalArgumentException(reason);}
}
