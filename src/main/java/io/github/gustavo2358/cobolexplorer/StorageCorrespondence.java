package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;
import static io.github.gustavo2358.cobolexplorer.StorageAccessSemantics.*;

/** Source-owned implicit CORRESPONDING pairs over resolved fixed group operands. */
final class StorageCorrespondence {
    private final Map<Key,Ast.DataEntry> declarations;
    private final Map<Key,Node> nodes;
    private final Map<Key,View> views=new HashMap<>();
    private final Map<Key,Base> bases;
    private final Map<Key,Optional<Map<List<String>,Ast.DataEntry>>> prepared=new HashMap<>();
    StorageCorrespondence(Map<Key,Ast.DataEntry> declarations,Map<Key,Node> nodes,Layout layout,Map<Key,Base> bases) {
        this.declarations=declarations;this.nodes=nodes;this.bases=bases;
        for(var view:layout.views())views.put(view.node(),view);
    }
    List<Move> sequence(Key statement,Ast.MoveStatement move,Map<Key,Access> accesses) {
        if(!(move.source() instanceof Ast.DataReference)||move.targets().size()!=1)return List.of();
        var source=accesses.get(new Key(statement.unit(),move.source().meta().id()));
        var target=accesses.get(new Key(statement.unit(),move.targets().get(0).meta().id()));
        if(!group(source)||!group(target))return List.of();
        var left=members(source.view().node());var right=members(target.view().node());
        if(left.isEmpty()||right.isEmpty())return List.of();
        var result=new ArrayList<Move>();
        boolean separate=StorageAccessSemantics.disjoint(source.view(),target.view(),bases);
        for(var item:left.get().entrySet()) {
            var receiving=right.get().get(item.getKey());if(receiving==null)continue;
            var a=nodes.get(new Key(statement.unit(),item.getValue().meta().id()));
            var b=nodes.get(new Key(statement.unit(),receiving.meta().id()));
            if(a.kind()==Kind.GROUP&&b.kind()==Kind.GROUP)continue;
            // A selected language pair outside this subset invalidates the whole proof.
            if(a.kind()!=Kind.ELEMENTARY||b.kind()!=Kind.ELEMENTARY||a.entity().isEmpty()||b.entity().isEmpty())return List.of();
            var av=views.get(a.id());var bv=views.get(b.id());
            if(!known(av)||!known(bv))return List.of();
            var read=new Access(a.id(),statement,a.entity().orElseThrow(),av,Role.READ,false,a.origin());
            var write=new Access(b.id(),statement,b.entity().orElseThrow(),bv,Role.WRITE,false,b.origin());
            var kind=separate?(av.extent().equals(bv.extent())?MoveKind.COPY_BYTES:MoveKind.FIT_TEXT):MoveKind.MUST_UNKNOWN;
            result.add(new Move(statement,Optional.of(write),Optional.of(read),kind,List.of(),
                    separate?List.of():List.of(StorageAccessSemantics.Reason.OVERLAPPING_COPY),move.meta().provenance()));
        }
        return List.copyOf(result);
    }
    private boolean group(Access access) {
        return access!=null&&!access.sliced()&&nodes.get(access.view().node()).kind()==Kind.GROUP;
    }
    private static boolean known(View view) {
        return view!=null&&view.textual()&&view.offset().value().isPresent()&&view.extent().value().isPresent();
    }
    private Optional<Map<List<String>,Ast.DataEntry>> members(Key root) {
        return prepared.computeIfAbsent(root,key->{
            var result=new LinkedHashMap<List<String>,Ast.DataEntry>();
            var pending=new ArrayDeque<Visit>();var children=declarations.get(key).children();
            for(int i=children.size()-1;i>=0;i--)pending.push(new Visit(children.get(i),List.of()));
            while(!pending.isEmpty()) {
                var visit=pending.pop();var data=visit.data();
                if(data.levelKind()!=Ast.DataLevelKind.GROUP_OR_ELEMENTARY)continue;
                if(data.clauses().stream().anyMatch(c->c instanceof Ast.RedefinesClause||c instanceof Ast.RenamesClause||c instanceof Ast.OccursClause
                        ||c instanceof Ast.UsageClause u&&!u.display()))continue;
                if(data.filler()) {if(!data.children().isEmpty())return Optional.empty();continue;}
                var path=new ArrayList<>(visit.qualifiers());path.add(data.name().toUpperCase(Locale.ROOT));
                var qualified=List.copyOf(path);
                if(result.putIfAbsent(qualified,data)!=null)return Optional.empty();
                for(int i=data.children().size()-1;i>=0;i--)pending.push(new Visit(data.children().get(i),qualified));
            }
            return Optional.of(Collections.unmodifiableMap(result));
        });
    }
    private record Visit(Ast.DataEntry data,List<String> qualifiers) { }
}
