package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Source-language access and exact MOVE facts over the prepared physical layout. */
public final class StorageAccessSemantics {
    public enum Role { READ, WRITE, CALL_TARGET, CALL_ARGUMENT }
    public record Access(Key reference,Key statement,ResolutionContracts.SemanticEntityId entity,
                         View view,Role role,boolean sliced,Ast.SourceProvenance origin) { }
    public enum MoveKind { LITERAL_BYTES, FITTED_LITERAL_BYTES, COPY_BYTES, FIT_TEXT, LOGICAL_FIT_TEXT, MUST_UNKNOWN, UNAVAILABLE }
    public enum Reason { ACCESS_NOT_PROVEN, SOURCE_NOT_PROVEN, EXTENT_MISMATCH, UNREPRESENTABLE_TEXT, OVERLAPPING_COPY, MOVE_FORM_NOT_SUPPORTED }
    public record Move(Key statement,Optional<Access> destination,Optional<Access> source,MoveKind kind,
                       List<Integer> bytes,List<Reason> reasons,Ast.SourceProvenance origin) {
        public Move { bytes=List.copyOf(bytes);reasons=List.copyOf(reasons); }
    }
    private final StorageLayoutSemantics layout;
    private final StorageInitialSemantics initial;
    private final FileIoMemory files;
    private final FileIoEffects fileEffects;
    private final FileIoControl fileControl;
    private final FileSortControl fileSort;
    public FileSortControl fileSort(){return fileSort;}
    public FileIoControl fileControl(){return fileControl;}
    public FileIoMemory files(){return files;}
    public FileIoEffects fileEffects(){return fileEffects;}
    public StorageInitialSemantics initial() {return initial;}
    public StorageLayoutSemantics layout() { return layout; }
    public boolean belongsTo(CompilationUnitBuildResult frontend, ReferenceResolution resolution) { return layout.belongsTo(frontend, resolution); }
    private final Map<Key,List<Access>> alternatives;
    public List<Access> alternatives(Key reference){return alternatives.getOrDefault(reference,List.of());}
    private final Map<Key,Access> accesses;
    private final Map<Key,Move> moves;
    private final Map<Key,List<Move>> sequences;
    private final Map<Key,StatementEffectSummary> effects;
    public Optional<StatementEffectSummary> effects(Key statement){return Optional.ofNullable(effects.get(statement));}
    public List<Move> sequence(Key statement) { return sequences.getOrDefault(statement,List.of()); }
    private StorageAccessSemantics(StorageLayoutSemantics layout,Map<Key,List<Access>> alternatives,Map<Key,Access> accesses,Map<Key,Move> moves,Map<Key,List<Move>> sequences,Map<Key,StatementEffectSummary> effects,StorageInitialSemantics initial,FileIoMemory files,FileIoControl fileControl,FileSortControl fileSort){this.fileSort=fileSort;this.fileControl=fileControl;this.files=files;this.fileEffects=FileIoEffects.analyze(files,layout);this.alternatives=Map.copyOf(alternatives);this.initial=initial;this.layout=layout;this.accesses=Map.copyOf(accesses);this.moves=Map.copyOf(moves);this.sequences=Map.copyOf(sequences);this.effects=Map.copyOf(effects);}
    public Optional<Access> access(Key reference){return Optional.ofNullable(accesses.get(reference));}
    public Collection<Access> accesses(){return accesses.values();}
    public Collection<Move> moves(){return moves.values();}
    public Move move(Key statement){return Objects.requireNonNull(moves.get(statement),"unknown MOVE");}
    public static StorageAccessSemantics analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,StorageLayoutSemantics layout) {
        return analyze(frontend,resolution,layout,StorageInitialSemantics.EntryMode.UNKNOWN);
    }
    public static StorageAccessSemantics analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,StorageLayoutSemantics layout,StorageInitialSemantics.EntryMode mode) {
        return analyze(frontend,resolution,layout,mode,new CicsProgramControlAnalyzer().analyze(frontend));
    }
    public static StorageAccessSemantics analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,StorageLayoutSemantics layout,
            StorageInitialSemantics.EntryMode mode,CicsProgramControlAnalyzer.Contribution cics) {
        if(!cics.belongsTo(frontend))throw new IllegalArgumentException("foreign platform contribution");
        if(!layout.belongsTo(frontend,resolution))throw new IllegalArgumentException("layout and binding proof belong to another snapshot");
        var bindings=new HashMap<Key,ReferenceResolution.Entry>();
        for(var entry:resolution.entries())bindings.put(new Key(entry.occurrence().programUnitId(),entry.occurrence().referenceAstNodeId()),entry);
        var logicalReads=new HashMap<Key,Access>();
        var accesses=new LinkedHashMap<Key,Access>();var moves=new LinkedHashMap<Key,Move>();var sequences=new LinkedHashMap<Key,List<Move>>();
        var summaries=new LinkedHashMap<Key,StatementEffectSummary>();
        var alternatives=new LinkedHashMap<Key,List<Access>>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var physical=layout.layout(unit.id());var nodes=new HashMap<Key,Node>();var bases=new HashMap<Key,Base>();
            for(var node:physical.nodes())nodes.put(node.id(),node);
            for(var base:physical.bases())bases.put(base.id(),base);
            var byEntity=new HashMap<ResolutionContracts.SemanticEntityId,View>();
            for(var view:physical.views())nodes.get(view.node()).entity().ifPresent(id->byEntity.put(id,view));
            var pending=new ArrayDeque<Visit>();pending.push(new Visit(unit.program(),null));
            var moveNodes=new ArrayList<Ast.MoveStatement>();var declarations=new HashMap<Key,Ast.DataEntry>();
            var statementNodes=new ArrayList<Ast.Statement>();
            while(!pending.isEmpty()) {
                var visit=pending.pop();var node=visit.node();
                if(node instanceof Ast.Program&&node!=unit.program())continue;
                var owner=node instanceof Ast.Statement s?s:visit.owner();
                if(node instanceof Ast.Statement s)statementNodes.add(s);
                if(node instanceof Ast.MoveStatement move)moveNodes.add(move);
                if(node instanceof Ast.DataEntry data)declarations.put(new Key(unit.id(),data.meta().id()),data);
                if(owner!=null&&node instanceof Ast.DataReference reference) {
                    var key=new Key(unit.id(),reference.meta().id());var binding=bindings.get(key);
                    if(binding!=null&&binding.status()==ResolutionContracts.ResolutionStatus.AMBIGUOUS
                            &&binding.occurrence().role()==ResolutionContracts.ReferenceRole.CALL_TARGET
                            &&reference.understanding()==Ast.ReferenceUnderstanding.STRUCTURED
                            &&reference.subscriptGroups().isEmpty()&&reference.referenceModification()==null) {
                        var choices=new ArrayList<Access>();
                        for(var candidate:binding.candidates()) {
                            var entity=candidate.entityId();var view=byEntity.get(entity);
                            if(view!=null&&view.textual()&&view.offset().value().isPresent()&&view.extent().value().isPresent()
                                    &&bases.get(view.base()).extent().value().isPresent()&&nodes.get(view.node()).kind()==Kind.ELEMENTARY)
                                choices.add(new Access(key,new Key(unit.id(),owner.meta().id()),entity,view,Role.CALL_TARGET,false,reference.meta().provenance()));
                        }
                        if(!choices.isEmpty())alternatives.put(key,List.copyOf(choices));
                    }
                    if(binding!=null&&binding.status()==ResolutionContracts.ResolutionStatus.RESOLVED&&binding.candidates().size()==1
                            &&binding.selectedCandidate().isPresent()&&reference.understanding()==Ast.ReferenceUnderstanding.STRUCTURED
                            &&reference.subscriptGroups().isEmpty()) {
                        var entity=binding.selectedCandidate().orElseThrow().entityId();var view=byEntity.get(entity);
                        var role=role(binding.occurrence().role());
                        boolean sliced=reference.referenceModification()!=null;
                        if(sliced)view=slice(view,reference.referenceModification(),physical.profile());
                        if(!sliced&&role==Role.READ&&view!=null&&view.textual()&&view.extent().value().filter(n->n.signum()>0).isPresent()
                                &&nodes.get(view.node()).kind()==Kind.ELEMENTARY&&reference.meta().provenance().exact())
                            logicalReads.put(key,new Access(key,new Key(unit.id(),owner.meta().id()),entity,view,role,false,reference.meta().provenance()));
                        if(role!=null&&view!=null&&view.textual()&&view.offset().value().isPresent()&&view.extent().value().isPresent()
                                &&bases.get(view.base()).extent().value().isPresent()
                                &&(role!=Role.CALL_TARGET||sliced||nodes.get(view.node()).kind()==Kind.ELEMENTARY))
                            accesses.put(key,new Access(key,new Key(unit.id(),owner.meta().id()),entity,view,role,sliced,reference.meta().provenance()));
                    }
                }
                var children=Ast.children(node);for(int i=children.size()-1;i>=0;i--)pending.push(new Visit(children.get(i),owner));
            }
            var coverage=new HashMap<Integer,SemanticCoverage.Finding>();
            for(var statement:statementNodes)StatementEffectSummary.of(statement).ifPresent(e->{
                var must=new ArrayList<Ast.DataReference>();
                if(e.proof()==StatementEffectSummary.Proof.INITIALIZE_TARGETS&&e.completeMutationBound())for(var ref:e.mayWrites()) {
                    var access=accesses.get(new Key(unit.id(),ref.meta().id()));
                    if(access==null||access.sliced())continue;
                    var physicalNode=nodes.get(access.view().node());var declaration=declarations.get(access.view().node());
                    // A group may contain excluded bytes (FILLER, REDEFINES).
                    // Only an ordinary exact elementary text receiver proves full overwrite.
                    if(physicalNode!=null&&physicalNode.kind()==Kind.ELEMENTARY&&!physicalNode.filler()&&declaration!=null
                            &&declaration.clauses().stream().noneMatch(c->c instanceof Ast.RedefinesClause||c instanceof Ast.OccursClause))must.add(ref);
                }
                summaries.put(new Key(unit.id(),statement.meta().id()),new StatementEffectSummary(e.knownReads(),e.mayWrites(),must,e.exposedRegions(),
                    e.unknownReadBound(),e.unknownWriteBound(),e.unknownExposureBound(),e.environment(),e.values(),e.proof()));
            });
            for(var finding:frontend.coverageByProgramUnit().get(unit.id()).findings())coverage.put(finding.astNodeId(),finding);
            var correspondence=new StorageCorrespondence(declarations,nodes,physical,bases);
            for(var node:moveNodes) {
                var statement=new Key(unit.id(),node.meta().id());var finding=coverage.get(node.meta().id());
                if(node.targets().isEmpty()||node.targets().stream().anyMatch(t->!(t instanceof Ast.DataReference))
                        ||finding==null||finding.coverage()!=SemanticCoverage.ConstructionCoverage.MODELED) {
                    moves.put(statement,new Move(statement,Optional.empty(),Optional.empty(),MoveKind.UNAVAILABLE,List.of(),List.of(Reason.MOVE_FORM_NOT_SUPPORTED),node.meta().provenance()));continue;
                }
                if(node.corresponding()) {
                    var pairs=correspondence.sequence(statement,node,accesses);
                    if(pairs.isEmpty())moves.put(statement,new Move(statement,Optional.empty(),Optional.empty(),MoveKind.UNAVAILABLE,List.of(),List.of(Reason.MOVE_FORM_NOT_SUPPORTED),node.meta().provenance()));
                    else {moves.put(statement,pairs.get(0));sequences.put(statement,pairs);}
                    continue;
                }
                var source=Optional.ofNullable(accesses.get(new Key(unit.id(),node.source().meta().id()))).filter(a->a.role()==Role.READ);
                var effects=new ArrayList<Move>();
                for(var target:node.targets()) {
                    var destination=Optional.ofNullable(accesses.get(new Key(unit.id(),target.meta().id()))).filter(a->a.role()==Role.WRITE);
                    var effect=effect(statement,destination,source,node.source(),physical.profile(),bases,node.meta().provenance());
                    var logical=logicalReads.get(new Key(unit.id(),node.source().meta().id()));
                    if(node.targets().size()==1&&effect.reasons().equals(List.of(Reason.SOURCE_NOT_PROVEN))&&logical!=null&&destination.isPresent()
                            &&!logical.view().base().equals(destination.get().view().base())&&bases.get(logical.view().base()).independent()
                            &&bases.get(destination.get().view().base()).independent())
                        effect=new Move(statement,destination,Optional.of(logical),MoveKind.LOGICAL_FIT_TEXT,List.of(),List.of(),node.meta().provenance());
                    effects.add(effect);
                }
                // A receiver may invalidate all later source reads. Never publish concrete
                // values for the remaining receivers of an overlapping source statement.
                boolean overlap=effects.stream().anyMatch(e->e.reasons().contains(Reason.OVERLAPPING_COPY));
                if(overlap)effects.replaceAll(e->e.destination().isEmpty()?e:new Move(statement,e.destination(),e.source(),MoveKind.MUST_UNKNOWN,List.of(),List.of(Reason.OVERLAPPING_COPY),e.origin()));
                moves.put(statement,effects.get(0));sequences.put(statement,List.copyOf(effects));
            }
        }
        var files=FileIoMemory.analyze(frontend,resolution,layout,accesses);
        return new StorageAccessSemantics(layout,alternatives,accesses,moves,sequences,summaries,StorageInitialSemantics.analyze(frontend,resolution,layout,mode,accesses,sequences,summaries,cics,files),files,FileIoControl.analyze(frontend,resolution,files),FileSortControl.analyze(frontend,resolution,layout,files));
    }
    private static Move effect(Key statement,Optional<Access> destination,Optional<Access> source,Ast.Expression expression,
            Profile profile,Map<Key,Base> bases,Ast.SourceProvenance origin) {
        MoveKind kind=MoveKind.MUST_UNKNOWN;var reasons=new ArrayList<Reason>();List<Integer> bytes=List.of();
        if(destination.isEmpty()){kind=MoveKind.UNAVAILABLE;reasons.add(Reason.ACCESS_NOT_PROVEN);}
        else if(expression instanceof Ast.LiteralExpression literal&&literal.logicalText().isPresent()) {
            var encoded=encode(literal.logicalText().get().value(),profile);
            if(encoded.isEmpty())reasons.add(Reason.UNREPRESENTABLE_TEXT);
            else {
                var size=destination.get().view().extent().value().orElseThrow();
                // The emitted byte vector has the same intrinsic JVM collection-size
                // representability as the source inventory, with no configurable cap.
                var fitted=new ArrayList<Integer>();int length=size.intValueExact();
                for(int i=0;i<length;i++)fitted.add(i<encoded.get().size()?encoded.get().get(i):0x40);
                kind=length==encoded.get().size()?MoveKind.LITERAL_BYTES:MoveKind.FITTED_LITERAL_BYTES;bytes=List.copyOf(fitted);
            }
        } else if(source.isPresent()) {
            if(!disjoint(source.get().view(),destination.get().view(),bases))reasons.add(Reason.OVERLAPPING_COPY);
            else kind=copyKind(source.get().view(),destination.get().view(),bases);
        } else reasons.add(Reason.SOURCE_NOT_PROVEN);
        return new Move(statement,destination,source,kind,bytes,reasons,origin);
    }
    static MoveKind copyKind(View source,View destination,Map<Key,Base> bases) {
        return !disjoint(source,destination,bases)?MoveKind.MUST_UNKNOWN:source.extent().equals(destination.extent())?MoveKind.COPY_BYTES:MoveKind.FIT_TEXT;
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
            :role==ResolutionContracts.ReferenceRole.CALL_TARGET?Role.CALL_TARGET
            :role==ResolutionContracts.ReferenceRole.CALL_ARGUMENT?Role.CALL_ARGUMENT:null;
    }
    static boolean disjoint(View a,View b,Map<Key,Base> bases) {
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
