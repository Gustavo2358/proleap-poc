package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Source-language access and exact MOVE facts over the prepared physical layout. */
public final class StorageAccessSemantics {
    public enum Role { READ, WRITE, CALL_TARGET }
    public record Access(Key reference,Key statement,ResolutionContracts.SemanticEntityId entity,
                         View view,Role role,boolean sliced,Ast.SourceProvenance origin) { }
    public enum MoveKind { LITERAL_BYTES, COPY_BYTES, MUST_UNKNOWN, UNAVAILABLE }
    public enum Reason { ACCESS_NOT_PROVEN, SOURCE_NOT_PROVEN, EXTENT_MISMATCH, UNREPRESENTABLE_TEXT, OVERLAPPING_COPY, MOVE_FORM_NOT_SUPPORTED }
    public record Move(Key statement,Optional<Access> destination,Optional<Access> source,MoveKind kind,
                       List<Integer> bytes,List<Reason> reasons,Ast.SourceProvenance origin) {
        public Move { bytes=List.copyOf(bytes);reasons=List.copyOf(reasons); }
    }
    private final StorageLayoutSemantics layout;
    public StorageLayoutSemantics layout() { return layout; }
    public boolean belongsTo(CompilationUnitBuildResult frontend, ReferenceResolution resolution) { return layout.belongsTo(frontend, resolution); }
    private final Map<Key,Access> accesses;
    private final Map<Key,Move> moves;
    private StorageAccessSemantics(StorageLayoutSemantics layout,Map<Key,Access> accesses,Map<Key,Move> moves){this.layout=layout;this.accesses=Map.copyOf(accesses);this.moves=Map.copyOf(moves);}
    public Optional<Access> access(Key reference){return Optional.ofNullable(accesses.get(reference));}
    public Collection<Access> accesses(){return accesses.values();}
    public Collection<Move> moves(){return moves.values();}
    public Move move(Key statement){return Objects.requireNonNull(moves.get(statement),"unknown MOVE");}
    public static StorageAccessSemantics analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,StorageLayoutSemantics layout) {
        if(!layout.belongsTo(frontend,resolution))throw new IllegalArgumentException("layout and binding proof belong to another snapshot");
        var bindings=new HashMap<Key,ReferenceResolution.Entry>();
        for(var entry:resolution.entries())bindings.put(new Key(entry.occurrence().programUnitId(),entry.occurrence().referenceAstNodeId()),entry);
        var accesses=new LinkedHashMap<Key,Access>();var moves=new LinkedHashMap<Key,Move>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var physical=layout.layout(unit.id());var nodes=new HashMap<Key,Node>();var bases=new HashMap<Key,Base>();
            for(var node:physical.nodes())nodes.put(node.id(),node);
            for(var base:physical.bases())bases.put(base.id(),base);
            var byEntity=new HashMap<ResolutionContracts.SemanticEntityId,View>();
            for(var view:physical.views())nodes.get(view.node()).entity().ifPresent(id->byEntity.put(id,view));
            var pending=new ArrayDeque<Visit>();pending.push(new Visit(unit.program(),null));
            var moveNodes=new ArrayList<Ast.MoveStatement>();
            while(!pending.isEmpty()) {
                var visit=pending.pop();var node=visit.node();
                if(node instanceof Ast.Program&&node!=unit.program())continue;
                var owner=node instanceof Ast.Statement s?s:visit.owner();
                if(node instanceof Ast.MoveStatement move)moveNodes.add(move);
                if(owner!=null&&node instanceof Ast.DataReference reference) {
                    var key=new Key(unit.id(),reference.meta().id());var binding=bindings.get(key);
                    if(binding!=null&&binding.status()==ResolutionContracts.ResolutionStatus.RESOLVED&&binding.candidates().size()==1
                            &&binding.selectedCandidate().isPresent()&&reference.understanding()==Ast.ReferenceUnderstanding.STRUCTURED
                            &&reference.subscriptGroups().isEmpty()) {
                        var entity=binding.selectedCandidate().orElseThrow().entityId();var view=byEntity.get(entity);
                        var role=role(binding.occurrence().role());
                        boolean sliced=reference.referenceModification()!=null;
                        if(sliced)view=slice(view,reference.referenceModification(),physical.profile());
                        if(role!=null&&view!=null&&view.textual()&&view.offset().value().isPresent()&&view.extent().value().isPresent()
                                &&bases.get(view.base()).extent().value().isPresent()
                                &&(role!=Role.CALL_TARGET||sliced||nodes.get(view.node()).kind()==Kind.ELEMENTARY))
                            accesses.put(key,new Access(key,new Key(unit.id(),owner.meta().id()),entity,view,role,sliced,reference.meta().provenance()));
                    }
                }
                var children=Ast.children(node);for(int i=children.size()-1;i>=0;i--)pending.push(new Visit(children.get(i),owner));
            }
            var coverage=new HashMap<Integer,SemanticCoverage.Finding>();
            for(var finding:frontend.coverageByProgramUnit().get(unit.id()).findings())coverage.put(finding.astNodeId(),finding);
            for(var node:moveNodes) {
                var statement=new Key(unit.id(),node.meta().id());var finding=coverage.get(node.meta().id());
                if(node.corresponding()||node.targets().size()!=1||!(node.targets().get(0) instanceof Ast.DataReference)
                        ||finding==null||finding.coverage()!=SemanticCoverage.ConstructionCoverage.MODELED) {
                    moves.put(statement,new Move(statement,Optional.empty(),Optional.empty(),MoveKind.UNAVAILABLE,List.of(),List.of(Reason.MOVE_FORM_NOT_SUPPORTED),node.meta().provenance()));continue;
                }
                var destination=Optional.ofNullable(accesses.get(new Key(unit.id(),node.targets().get(0).meta().id()))).filter(a->a.role()==Role.WRITE);
                var source=Optional.ofNullable(accesses.get(new Key(unit.id(),node.source().meta().id()))).filter(a->a.role()==Role.READ);
                MoveKind kind=MoveKind.MUST_UNKNOWN;var reasons=new ArrayList<Reason>();List<Integer> bytes=List.of();
                if(destination.isEmpty()){kind=MoveKind.UNAVAILABLE;reasons.add(Reason.ACCESS_NOT_PROVEN);}
                else if(node.source() instanceof Ast.LiteralExpression literal&&literal.logicalText().isPresent()) {
                    var encoded=encode(literal.logicalText().get().value(),physical.profile());
                    if(encoded.isEmpty())reasons.add(Reason.UNREPRESENTABLE_TEXT);
                    else if(!destination.get().view().extent().value().orElseThrow().equals(java.math.BigInteger.valueOf(encoded.get().size())))reasons.add(Reason.EXTENT_MISMATCH);
                    else {kind=MoveKind.LITERAL_BYTES;bytes=encoded.get();}
                } else if(source.isPresent()) {
                    if(!source.get().view().extent().equals(destination.get().view().extent()))reasons.add(Reason.EXTENT_MISMATCH);
                    else if(!disjoint(source.get().view(),destination.get().view(),bases))reasons.add(Reason.OVERLAPPING_COPY);
                    else kind=MoveKind.COPY_BYTES;
                } else reasons.add(Reason.SOURCE_NOT_PROVEN);
                moves.put(statement,new Move(statement,destination,source,kind,bytes,reasons,node.meta().provenance()));
            }
        }
        return new StorageAccessSemantics(layout,accesses,moves);
    }
    private static View slice(View view,Ast.ReferenceModification modification,Profile profile) {
        if(profile!=Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047||view==null||!view.textual()
                ||view.offset().value().isEmpty()||view.extent().value().isEmpty()
                ||!(modification.offset() instanceof Ast.LiteralExpression position)
                ||!(modification.length() instanceof Ast.LiteralExpression length)
                ||position.integerValue().isEmpty()||length.integerValue().isEmpty())return null;
        var p=position.integerValue().get();var n=length.integerValue().get();
        if(p.signum()<=0||n.signum()<=0||p.subtract(java.math.BigInteger.ONE).add(n).compareTo(view.extent().value().get())>0)return null;
        return new View(view.node(),view.base(),Measure.known(view.offset().value().get().add(p).subtract(java.math.BigInteger.ONE)),Measure.known(n),true,view.origin());
    }
    private record Visit(Ast.Node node,Ast.Statement owner) { }
    private static Role role(ResolutionContracts.ReferenceRole role) {
        return role==ResolutionContracts.ReferenceRole.VALUE_READ?Role.READ:role==ResolutionContracts.ReferenceRole.VALUE_WRITE?Role.WRITE
            :role==ResolutionContracts.ReferenceRole.CALL_TARGET?Role.CALL_TARGET:null;
    }
    private static boolean disjoint(View a,View b,Map<Key,Base> bases) {
        if(!a.base().equals(b.base()))return a.base().unit().equals(b.base().unit())&&bases.get(a.base()).independent()&&bases.get(b.base()).independent();
        var left=a.offset().value().orElseThrow();var right=b.offset().value().orElseThrow();
        return left.add(a.extent().value().orElseThrow()).compareTo(right)<=0||right.add(b.extent().value().orElseThrow()).compareTo(left)<=0;
    }
    // Explicit IBM/IANA CP1047 -> ISO10646 table 1.00 (2002-09-24), Unicode U+00xx.
    // Authority: https://data.iana.org/archive/ietf-charsets/msg01226.html . No JVM charset.
    private static final String TABLE=
        "000102039c09867f978d8e0b0c0d0e0f101112139d8508871819928f1c1d1e1f"+
        "80818283840a171b88898a8b8c050607909116939495960498999a9b14159e1a"+
        "20a0e2e4e0e1e3e5e7f1a22e3c282b7c26e9eaebe8edeeefecdf21242a293b5e"+
        "2d2fc2c4c0c1c3c5c7d1a62c255f3e3ff8c9cacbc8cdcecfcc603a2340273d22"+
        "d8616263646566676869abbbf0fdfeb1b06a6b6c6d6e6f707172aabae6b8c6a4"+
        "b57e737475767778797aa1bfd05bdeaeaca3a5b7a9a7b6bcbdbedda8af5db4d7"+
        "7b414243444546474849adf4f6f2f3f57d4a4b4c4d4e4f505152b9fbfcf9faff"+
        "5cf7535455565758595ab2d4d6d2d3d530313233343536373839b3dbdcd9da9f";
    private static final int[] ENCODE=encodingTable();
    private static int[] encodingTable() {
        var result=new int[256];Arrays.fill(result,-1);
        if(TABLE.length()!=512)throw new IllegalStateException("incomplete codec authority");
        for(int octet=0;octet<256;octet++){int scalar=Integer.parseInt(TABLE.substring(octet*2,octet*2+2),16);if(result[scalar]!=-1)throw new IllegalStateException("codec is not bijective");result[scalar]=octet;}
        return result;
    }
    static Optional<List<Integer>> encode(String text,Profile profile) {
        Objects.requireNonNull(text);Objects.requireNonNull(profile);
        if(profile!=Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047)return Optional.empty();
        var bytes=new ArrayList<Integer>();
        for(int i=0;i<text.length();i++){int scalar=text.charAt(i);if(scalar>255)return Optional.empty();bytes.add(ENCODE[scalar]);}
        return Optional.of(List.copyOf(bytes));
    }
}
