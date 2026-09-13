package io.github.gustavo2358.cobolexplorer;

import java.math.BigInteger;
import java.util.*;

/** Canonical physical layout facts; no SP, AIR, downstream analysis or runtime inference. */
public final class StorageLayoutSemantics {
    public enum Profile { UNSPECIFIED, IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047 }
    public static final String PROFILE_ID="ibm-enterprise-6.4-fixed-display-1047@1";
    public enum Reason { PROFILE_NOT_SELECTED, INPUT_MISSING, NONORDINARY_PROGRAM, SECTION_NOT_PROVEN,
        UNSUPPORTED_DECLARATION, UNKNOWN_EXTENT, UNKNOWN_OFFSET, OVERLAY_NOT_PROVEN }
    public record Key(ResolutionContracts.ProgramUnitId unit,int node) { }
    public record Measure(Optional<BigInteger> value,List<Reason> reasons) {
        public Measure { Objects.requireNonNull(value);reasons=List.copyOf(reasons);
            if(value.isPresent()?(value.get().signum()<0||!reasons.isEmpty()):reasons.isEmpty())throw new IllegalArgumentException("known nonnegative measure or explicit unknown reason"); }
        public static Measure known(BigInteger value){return new Measure(Optional.of(value),List.of());}
        public static Measure unknown(Reason reason){return new Measure(Optional.empty(),List.of(reason));}
    }
    public enum Kind { GROUP, ELEMENTARY, OPAQUE }
    public record Node(Key id,Optional<Key> parent,int order,boolean filler,Kind kind,
                       Optional<ResolutionContracts.SemanticEntityId> entity,Measure extent,Ast.SourceProvenance origin) { }
    public record Base(Key id,Measure extent,boolean independent,Ast.SourceProvenance origin) { }
    public record View(Key node,Key base,Measure offset,Measure extent,boolean textual,Ast.SourceProvenance origin) { }
    public record Layout(Profile profile,List<Node> nodes,List<Base> bases,List<View> views,List<Reason> reasons) {
        public Layout { nodes=List.copyOf(nodes);bases=List.copyOf(bases);views=List.copyOf(views);reasons=List.copyOf(reasons); }
    }
    private final Map<ResolutionContracts.ProgramUnitId,Layout> layouts;
    private final Map<String,Long> metrics;
    private final CompilationUnitBuildResult owner;
    private final ReferenceResolution bindings;
    private StorageLayoutSemantics(Map<ResolutionContracts.ProgramUnitId,Layout> layouts,Map<String,Long> metrics,
            CompilationUnitBuildResult owner,ReferenceResolution bindings) {
        this.layouts=Map.copyOf(layouts);this.metrics=Map.copyOf(metrics);this.owner=owner;this.bindings=bindings;
    }
    boolean belongsTo(CompilationUnitBuildResult owner,ReferenceResolution bindings){return this.owner==owner&&this.bindings==bindings;}
    public Layout layout(ResolutionContracts.ProgramUnitId unit){return Objects.requireNonNull(layouts.get(unit),"foreign unit");}
    public Map<String,Long> metrics(){return metrics;}
    public static StorageLayoutSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,
            ReferenceResolution resolution,ResolutionAnalysisReport report,Profile profile) {
        Objects.requireNonNull(profile);Objects.requireNonNull(resolution);
        boolean input=report.gaps().stream().noneMatch(g->g.category()==ResolutionAnalysisReport.GapCategory.INPUT);
        var layouts=new LinkedHashMap<ResolutionContracts.ProgramUnitId,Layout>();long declarations=0,visits=0;
        for(var unit:frontend.compilationUnit().programUnits()) {
            var reasons=new LinkedHashSet<Reason>();
            if(profile==Profile.UNSPECIFIED)reasons.add(Reason.PROFILE_NOT_SELECTED);
            if(!input)reasons.add(Reason.INPUT_MISSING);
            var attributes=unit.program().attributes();
            if(attributes.initial()||attributes.recursive()||attributes.common()||attributes.library()||attributes.definition())reasons.add(Reason.NONORDINARY_PROGRAM);
            var sections=new ArrayList<Ast.Section>();
            for(var division:unit.program().divisions())if(division.divisionKind()==Ast.DivisionKind.DATA)
                for(var child:division.children())if(child instanceof Ast.Section section&&section.dataSectionKind()==Ast.DataSectionKind.WORKING_STORAGE)sections.add(section);
            if(sections.size()!=1)reasons.add(Reason.SECTION_NOT_PROVEN);
            var coverage=new HashMap<Integer,SemanticCoverage.Finding>();
            var reportUnit=frontend.coverageByProgramUnit().get(unit.id());
            if(reportUnit==null)reasons.add(Reason.INPUT_MISSING);else for(var f:reportUnit.findings())coverage.put(f.astNodeId(),f);
            var entities=new HashMap<Integer,ResolutionContracts.SemanticEntityId>();var duplicates=new HashSet<Integer>();
            for(var symbol:tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols())
                if(symbol.namespace()==SymbolTable.Namespace.DATA&&symbol.kind()==SymbolTable.SymbolKind.DATA_ITEM) {
                    var entity=new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,symbol.id());
                    if(entities.putIfAbsent(symbol.declarationAstNodeId(),entity)!=null)duplicates.add(symbol.declarationAstNodeId());
                }
            var ordered=new ArrayList<Position>();var roots=new ArrayList<Ast.DataEntry>();
            for(var section:sections)for(var child:section.children()) {
                if(child instanceof Ast.DataEntry data)roots.add(data);else reasons.add(Reason.SECTION_NOT_PROVEN);
            }
            if(roots.stream().anyMatch(root->level(root)!=1&&level(root)!=77))reasons.add(Reason.SECTION_NOT_PROVEN);
            var pending=new ArrayDeque<Position>();
            for(int i=roots.size()-1;i>=0;i--)pending.push(new Position(roots.get(i),Optional.empty(),i,roots.get(i).meta().id()));
            var unique=new HashSet<Integer>();boolean opaqueClause=false,overlay=false;
            while(!pending.isEmpty()) {
                var position=pending.pop();var data=position.data();visits++;declarations++;
                if(!unique.add(data.meta().id()))throw new IllegalArgumentException("duplicate physical declaration identity");
                ordered.add(position);
                for(var clause:data.clauses()) {
                    overlay|=clause instanceof Ast.RedefinesClause||clause instanceof Ast.RenamesClause;
                    opaqueClause|=clause instanceof Ast.PreservedDataClause;
                }
                for(int i=data.children().size()-1;i>=0;i--)pending.push(new Position(data.children().get(i),Optional.of(data.meta().id()),i,position.root()));
            }
            // W3 deliberately retains the conservative overlay guard; W4 replaces it with components.
            if(overlay)reasons.add(Reason.OVERLAY_NOT_PROVEN);
            boolean environment=reasons.isEmpty();
            var extents=new HashMap<Integer,Measure>();var shapes=new HashMap<Integer,Shape>();
            for(int i=ordered.size()-1;i>=0;i--) {
                var data=ordered.get(i).data();visits++;var shape=shape(data,coverage,duplicates);shapes.put(data.meta().id(),shape);
                Measure extent;
                if(!environment)extent=Measure.unknown(reasons.iterator().next());
                else if(!shape.supported())extent=Measure.unknown(Reason.UNSUPPORTED_DECLARATION);
                else if(shape.kind()==Kind.ELEMENTARY)extent=Measure.known(shape.leafExtent().orElseThrow());
                else {
                    extent=Measure.known(BigInteger.ZERO);
                    for(var child:data.children())extent=plus(extent,extents.get(child.meta().id()),Reason.UNKNOWN_EXTENT);
                }
                extents.put(data.meta().id(),extent);
            }
            var offsets=new HashMap<Integer,Measure>();var permitted=new HashMap<Integer,Boolean>();
            for(var root:roots){offsets.put(root.meta().id(),Measure.known(BigInteger.ZERO));permitted.put(root.meta().id(),environment);}
            var nodes=new ArrayList<Node>();var views=new ArrayList<View>();var bases=new ArrayList<Base>();
            for(var position:ordered) {
                var data=position.data();var shape=shapes.get(data.meta().id());var key=new Key(unit.id(),data.meta().id());visits++;
                var extent=extents.get(data.meta().id());var offset=offsets.get(data.meta().id());
                boolean allowed=permitted.get(data.meta().id())&&shape.supported();
                nodes.add(new Node(key,position.parent().map(p->new Key(unit.id(),p)),position.order(),data.filler(),shape.kind(),
                    data.filler()||duplicates.contains(data.meta().id())?Optional.empty():Optional.ofNullable(entities.get(data.meta().id())),extent,data.meta().provenance()));
                views.add(new View(key,new Key(unit.id(),position.root()),offset,extent,allowed&&extent.value().isPresent(),data.meta().provenance()));
                if(position.parent().isEmpty())bases.add(new Base(key,extent,environment&&!opaqueClause&&data.visibility()==Ast.DeclarationVisibility.LOCAL,data.meta().provenance()));
                var cursor=allowed?offset:Measure.unknown(Reason.UNKNOWN_OFFSET);
                for(var child:data.children()) {
                    offsets.put(child.meta().id(),cursor);permitted.put(child.meta().id(),allowed);
                    cursor=plus(cursor,extents.get(child.meta().id()),Reason.UNKNOWN_OFFSET);
                }
            }
            layouts.put(unit.id(),new Layout(profile,nodes,bases,views,List.copyOf(reasons)));
        }
        return new StorageLayoutSemantics(layouts,Map.of("declarations",declarations,"layoutVisits",visits,"objectPairs",0L),frontend,resolution);
    }
    private record Position(Ast.DataEntry data,Optional<Integer> parent,int order,int root) { }
    private record Shape(Kind kind,boolean supported,Optional<BigInteger> leafExtent) { }
    private static Shape shape(Ast.DataEntry data,Map<Integer,SemanticCoverage.Finding> coverage,Set<Integer> duplicates) {
        boolean known=!duplicates.contains(data.meta().id())
            &&data.visibility()==Ast.DeclarationVisibility.LOCAL
            &&(data.levelKind()==Ast.DataLevelKind.GROUP_OR_ELEMENTARY||data.levelKind()==Ast.DataLevelKind.STANDALONE_77)
            &&((level(data)>=1&&level(data)<=49)||level(data)==77)
            &&modeled(data,coverage);
        int pictures=0,usages=0;Optional<BigInteger> extent=Optional.empty();
        for(var clause:data.clauses()) {
            known&=modeled(clause,coverage);
            if(clause instanceof Ast.PictureClause picture){pictures++;extent=picture.textExtent().map(BigInteger::valueOf);}
            else if(clause instanceof Ast.UsageClause usage&&usage.display())usages++;
            else known=false;
        }
        var kind=data.children().isEmpty()?Kind.ELEMENTARY:Kind.GROUP;
        known&=usages<=1&&(kind==Kind.GROUP?pictures==0:pictures==1&&extent.isPresent());
        return new Shape(known?kind:Kind.OPAQUE,known,extent);
    }
    private static boolean modeled(Ast.Node node,Map<Integer,SemanticCoverage.Finding> coverage) {
        var finding=coverage.get(node.meta().id());
        // Exact is a source mapping claim. Typed semantics survive complete COPY expansion.
        return finding!=null&&finding.coverage()==SemanticCoverage.ConstructionCoverage.MODELED;
    }
    private static int level(Ast.DataEntry data) {
        try{return new BigInteger(data.level()).intValueExact();}catch(NumberFormatException|ArithmeticException invalid){return -1;}
    }
    private static Measure plus(Measure a,Measure b,Reason reason) {
        return a.value().isPresent()&&b.value().isPresent()?Measure.known(a.value().get().add(b.value().get())):Measure.unknown(reason);
    }
}
