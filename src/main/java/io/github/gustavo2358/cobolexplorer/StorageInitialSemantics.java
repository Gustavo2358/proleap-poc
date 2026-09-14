package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;
/** Explicit source entry profile and VALUE conditions, separate from executable writes. */
public final class StorageInitialSemantics {
    public enum EntryMode { UNKNOWN, INITIAL, PRESERVED }
    public enum Kind { LITERAL_BYTES, POSSIBLE_LITERAL_BYTES, PRESERVE, UNKNOWN }
    public enum Proof { NONE, EXPLICIT_INITIAL, EXPLICIT_PRESERVED, PROGRAM_INITIAL, DECLARATIVE_INVARIANT, DECLARATIVE_POSSIBILITY }
    public enum Reason { ENTRY_STATE_NOT_PROVEN, STORAGE_NOT_PROVEN, VALUE_NOT_PROVEN, VALUE_ON_REDEFINITION, NESTED_VALUE,
        STORAGE_NOT_LOCAL, INCOMPLETE_WRITE_INVENTORY, OVERLAPPING_WRITE, WRITE_NOT_PROVEN, UNKNOWN_STORAGE_EFFECT, FOREIGN_MUTATION_OR_ESCAPE }
    public record Condition(Key declaration,Optional<View> view,Kind kind,List<Integer> bytes,List<Reason> reasons,Ast.SourceProvenance origin,Proof proof) {
        public Condition {view=Objects.requireNonNull(view);bytes=List.copyOf(bytes);reasons=List.copyOf(reasons);}
    }
    public record Facts(EntryMode mode,List<Condition> conditions) {public Facts {conditions=List.copyOf(conditions);}}
    private final Map<ResolutionContracts.ProgramUnitId,Facts> units;
    private StorageInitialSemantics(Map<ResolutionContracts.ProgramUnitId,Facts> units) {this.units=Map.copyOf(units);}
    public Facts facts(ResolutionContracts.ProgramUnitId unit) {return Objects.requireNonNull(units.get(unit));}
    private record Flags(boolean value,boolean redefined) { }
    private record Visit(Ast.Node node,Flags flags) { }
    public static StorageInitialSemantics analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,StorageLayoutSemantics layout,EntryMode mode) {
        return StorageAccessSemantics.analyze(frontend,resolution,layout,mode).initial();
    }
    static StorageInitialSemantics analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,StorageLayoutSemantics layout,EntryMode mode,
            Map<Key,StorageAccessSemantics.Access> accesses,Map<Key,List<StorageAccessSemantics.Move>> moves,CicsProgramControlAnalyzer.Contribution cics) {
        if(!layout.belongsTo(frontend,resolution))throw new IllegalArgumentException("foreign layout proof");
        var units=new LinkedHashMap<ResolutionContracts.ProgramUnitId,Facts>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var physical=layout.layout(unit.id());var views=new HashMap<Integer,View>();var bases=new HashMap<Key,Base>();
            physical.views().forEach(v->views.put(v.node().node(),v));physical.bases().forEach(b->bases.put(b.id(),b));
            var inventory=mode==EntryMode.UNKNOWN&&!unit.program().attributes().initial()
                ?StorageMutationInventory.analyze(frontend,unit,physical,accesses,moves,cics):null;
            var declarations=new ArrayList<Ast.DataEntry>();var inherited=new HashMap<Integer,Flags>();
            var pending=new ArrayDeque<Visit>();pending.push(new Visit(unit.program(),new Flags(false,false)));
            while(!pending.isEmpty()) {
                var visit=pending.pop();var node=visit.node();if(node instanceof Ast.Program&&node!=unit.program())continue;
                var flags=visit.flags();
                if(node instanceof Ast.DataEntry data) {
                    if(data.levelKind()==Ast.DataLevelKind.CONDITION_88||data.levelKind()==Ast.DataLevelKind.RENAMES_66)continue;
                    declarations.add(data);inherited.put(data.meta().id(),flags);
                    flags=new Flags(flags.value()||data.clauses().stream().anyMatch(Ast.ValueClause.class::isInstance),
                        flags.redefined()||data.clauses().stream().anyMatch(Ast.RedefinesClause.class::isInstance));
                }
                var children=Ast.children(node);for(int i=children.size()-1;i>=0;i--)pending.push(new Visit(children.get(i),flags));
            }
            var descendantValues=new HashMap<Integer,Integer>();
            for(int i=declarations.size()-1;i>=0;i--) {
                var data=declarations.get(i);int count=(int)data.clauses().stream().filter(Ast.ValueClause.class::isInstance).count();
                for(var child:data.children())count=Math.addExact(count,descendantValues.getOrDefault(child.meta().id(),0));
                descendantValues.put(data.meta().id(),count);
            }
            var physicalNodes=new HashSet<Integer>();physical.nodes().forEach(n->physicalNodes.add(n.id().node()));
            var conditions=new ArrayList<Condition>();
            for(var data:declarations) {
                // Nonphysical declarations (for example FILE SECTION) remain outside this entry profile.
                if(!physicalNodes.contains(data.meta().id()))continue;
                var values=data.clauses().stream().filter(Ast.ValueClause.class::isInstance).map(Ast.ValueClause.class::cast).toList();
                if(values.isEmpty())continue;
                var reasons=new ArrayList<Reason>();var candidate=views.get(data.meta().id());
                boolean known=candidate!=null&&candidate.textual()&&candidate.offset().value().isPresent()&&candidate.extent().value().isPresent()
                    &&bases.get(candidate.base()).extent().value().isPresent()&&physical.profile()==Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047;
                var view=known?Optional.of(candidate):Optional.<View>empty();
                if(!known)reasons.add(Reason.STORAGE_NOT_PROVEN);
                var flags=inherited.get(data.meta().id());
                if(flags.redefined()||data.clauses().stream().anyMatch(Ast.RedefinesClause.class::isInstance))reasons.add(Reason.VALUE_ON_REDEFINITION);
                if(flags.value()||descendantValues.get(data.meta().id())>values.size())reasons.add(Reason.NESTED_VALUE);
                // Materialization and lifecycle are separate proofs. Unknown effects can
                // invalidate lifetime constancy, but cannot erase these source-proved bytes.
                if(mode==EntryMode.UNKNOWN&&!unit.program().attributes().initial()&&known&&!bases.get(candidate.base()).independent())
                    reasons.add(Reason.STORAGE_NOT_LOCAL);
                Kind kind=Kind.UNKNOWN;List<Integer> bytes=List.of();Proof proof=Proof.NONE;
                if(reasons.isEmpty()&&mode==EntryMode.PRESERVED) {
                    kind=Kind.PRESERVE;proof=Proof.EXPLICIT_PRESERVED;
                } else if(reasons.isEmpty()) {
                    var encoded=values.size()==1?values.get(0).logicalText().flatMap(t->StorageAccessSemantics.encode(t.value(),physical.profile())):Optional.<List<Integer>>empty();
                    var size=candidate.extent().value().orElseThrow();
                    if(encoded.isEmpty()||java.math.BigInteger.valueOf(encoded.get().size()).compareTo(size)>0)reasons.add(Reason.VALUE_NOT_PROVEN);
                    else {
                        var fitted=new ArrayList<>(encoded.get());int length=size.intValueExact();
                        while(fitted.size()<length)fitted.add(0x40);
                        bytes=List.copyOf(fitted);kind=Kind.LITERAL_BYTES;
                        if(mode==EntryMode.INITIAL)proof=Proof.EXPLICIT_INITIAL;
                        else if(unit.program().attributes().initial())proof=Proof.PROGRAM_INITIAL;
                        else {
                            reasons.addAll(inventory.blockers(candidate));
                            if(reasons.isEmpty())proof=Proof.DECLARATIVE_INVARIANT;
                            else {
                                kind=Kind.POSSIBLE_LITERAL_BYTES;proof=Proof.DECLARATIVE_POSSIBILITY;
                                reasons.add(Reason.ENTRY_STATE_NOT_PROVEN);
                            }
                        }
                    }
                }
                if(kind==Kind.UNKNOWN&&mode==EntryMode.UNKNOWN)reasons.add(Reason.ENTRY_STATE_NOT_PROVEN);
                conditions.add(new Condition(new Key(unit.id(),data.meta().id()),view,kind,bytes,reasons,values.get(0).meta().provenance(),kind==Kind.UNKNOWN?Proof.NONE:proof));
            }
            units.put(unit.id(),new Facts(mode,conditions));
        }
        return new StorageInitialSemantics(units);
    }
}
