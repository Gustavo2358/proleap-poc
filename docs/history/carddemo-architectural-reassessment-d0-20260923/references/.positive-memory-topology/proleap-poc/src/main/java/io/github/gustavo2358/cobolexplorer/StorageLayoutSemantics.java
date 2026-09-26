package io.github.gustavo2358.cobolexplorer;

import java.math.BigInteger;
import java.util.*;

/** Canonical layout of the supported projection; no source-completeness or runtime inference. */
public final class StorageLayoutSemantics {
    public enum Profile { UNSPECIFIED, IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047 }
    public static final String PROFILE_ID="ibm-enterprise-6.4-fixed-display-1047@1";
    public enum Reason { PROFILE_NOT_SELECTED, INPUT_MISSING, NONORDINARY_PROGRAM, SECTION_NOT_PROVEN,
        UNSUPPORTED_DECLARATION, UNKNOWN_EXTENT, UNKNOWN_OFFSET, OVERLAY_NOT_PROVEN, RENAMES_NOT_PROVEN }
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
    public record Renaming(Key owner, Optional<Key> from, Optional<Key> through, boolean proved, Ast.RenamesClause clause) { }
    public record Layout(Profile profile,List<Node> nodes,List<Base> bases,List<View> views,List<Reason> reasons,List<StorageComponents.Relation> relations,List<Renaming> renames) {
        public Layout { nodes=List.copyOf(nodes);bases=List.copyOf(bases);views=List.copyOf(views);reasons=List.copyOf(reasons);relations=List.copyOf(relations);renames=List.copyOf(renames); }
    }
    /** Character positions only. This fact grants no physical extent, allocation or codec. */
    public record LogicalView(Key node,Key root,BigInteger start,BigInteger length) { }
    /** A complete local TEXT view; unlike LogicalView, it does not close a whole layout family. */
    public record LogicalExactView(Key node,Key representative,BigInteger length) { }
    private final List<LogicalView> logicalViews;
    public List<LogicalView> logicalViews(){return logicalViews;}
    private final List<LogicalExactView> logicalExactViews;
    public List<LogicalExactView> logicalExactViews(){return logicalExactViews;}
    private final Map<ResolutionContracts.ProgramUnitId,Layout> layouts;
    private final Map<String,Long> metrics;
    private final CompilationUnitBuildResult owner;
    private final ReferenceResolution bindings;
    private final CompilationUnitSymbolTables symbolTables;
    private StorageLayoutSemantics(Map<ResolutionContracts.ProgramUnitId,Layout> layouts,Map<String,Long> metrics,
            CompilationUnitBuildResult owner,ReferenceResolution bindings,CompilationUnitSymbolTables symbolTables,List<LogicalView> logicalViews,List<LogicalExactView> logicalExactViews) {
        this.logicalViews=List.copyOf(logicalViews);
        this.logicalExactViews=List.copyOf(logicalExactViews);
        this.layouts=Map.copyOf(layouts);this.metrics=Map.copyOf(metrics);this.owner=owner;this.bindings=bindings;this.symbolTables=symbolTables;
    }
    CompilationUnitSymbolTables symbolTables(){return symbolTables;}
    boolean belongsTo(CompilationUnitBuildResult owner,ReferenceResolution bindings){return this.owner==owner&&this.bindings==bindings;}
    public Layout layout(ResolutionContracts.ProgramUnitId unit){return Objects.requireNonNull(layouts.get(unit),"foreign unit");}
    public Map<String,Long> metrics(){return metrics;}
    public static StorageLayoutSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,
            ReferenceResolution resolution,ResolutionAnalysisReport report,Profile profile) {
        return analyze(frontend,tables,resolution,report,profile,StorageComponents.analyze(frontend,tables,resolution));
    }
    public static StorageLayoutSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,
            ReferenceResolution resolution,ResolutionAnalysisReport report,Profile profile,StorageComponents components) {
        return analyze(frontend,tables,resolution,report,profile,components,false);
    }
    public static StorageLayoutSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,
            ReferenceResolution resolution,ResolutionAnalysisReport report,Profile profile,StorageComponents components,boolean logicalText) {
        Objects.requireNonNull(profile);Objects.requireNonNull(resolution);
        if(logicalText&&profile!=Profile.UNSPECIFIED)throw new IllegalArgumentException("logical text W1 requires unspecified physical profile");
        if(!components.belongsTo(frontend))throw new IllegalArgumentException("storage components belong to another snapshot");
        var layouts=new LinkedHashMap<ResolutionContracts.ProgramUnitId,Layout>();var logicalViews=new ArrayList<LogicalView>();var logicalExactViews=new ArrayList<LogicalExactView>();long declarations=0,visits=0;
        for(var unit:frontend.compilationUnit().programUnits()) {
            boolean input=report.inputComplete(unit.id());
            var reasons=new LinkedHashSet<Reason>();
            if(profile==Profile.UNSPECIFIED)reasons.add(Reason.PROFILE_NOT_SELECTED);
            if(!input)reasons.add(Reason.INPUT_MISSING);
            var attributes=unit.program().attributes();
            if(attributes.recursive()||attributes.common()||attributes.library()||attributes.definition())reasons.add(Reason.NONORDINARY_PROGRAM);
            var physical=components.unit(unit.id());
            if(!physical.structureProven())reasons.add(Reason.SECTION_NOT_PROVEN);
            if(!physical.relationsProven())reasons.add(Reason.OVERLAY_NOT_PROVEN);
            var coverage=new HashMap<Integer,SemanticCoverage.Finding>();
            var reportUnit=frontend.coverageByProgramUnit().get(unit.id());
            if(reportUnit==null)reasons.add(Reason.INPUT_MISSING);else for(var f:reportUnit.findings())coverage.put(f.astNodeId(),f);
            var entities=new HashMap<Integer,ResolutionContracts.SemanticEntityId>();var duplicates=new HashSet<Integer>();
            for(var symbol:tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols())
                if(symbol.namespace()==SymbolTable.Namespace.DATA&&(symbol.kind()==SymbolTable.SymbolKind.DATA_ITEM||symbol.kind()==SymbolTable.SymbolKind.RENAMES)) {
                    var entity=new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,symbol.id());
                    if(entities.putIfAbsent(symbol.declarationAstNodeId(),entity)!=null)duplicates.add(symbol.declarationAstNodeId());
                }
            var ordered=physical.positions();var roots=physical.roots();
            visits+=ordered.size();declarations+=ordered.size();
            // An unproved subordinate REDEFINES stays inside its enclosing record.
            // Its root extent/views become unknown, but root allocation is a separate proof.
            boolean environment=reasons.stream().allMatch(r->r==Reason.OVERLAY_NOT_PROVEN&&physical.rootRelationsProven());
            var extents=new HashMap<Integer,Measure>();var shapes=new HashMap<Integer,Shape>();var footprints=new HashMap<Integer,Measure>();
            for(int i=ordered.size()-1;i>=0;i--) {
                var data=ordered.get(i).data();visits++;var shape=shape(data,coverage,duplicates);shapes.put(data.meta().id(),shape);
                Measure extent;
                if(!environment)extent=Measure.unknown(reasons.iterator().next());
                else if(physical.uncertainRoots().contains(ordered.get(i).root()))extent=Measure.unknown(Reason.OVERLAY_NOT_PROVEN);
                else if(!shape.supported())extent=Measure.unknown(Reason.UNSUPPORTED_DECLARATION);
                else if(shape.kind()==Kind.ELEMENTARY)extent=Measure.known(shape.leafExtent().orElseThrow());
                else {
                    extent=Measure.known(BigInteger.ZERO);
                    for(var component:physical.children().get(data.meta().id())) {
                        var footprint=footprint(component,extents);footprints.put(component.representative(),footprint);
                        extent=plus(extent,footprint,Reason.UNKNOWN_EXTENT);
                    }
                }
                extents.put(data.meta().id(),extent);
                // Even an opaque parent must preserve explicit unknown offsets for its children.
                for(var component:physical.children().get(data.meta().id()))
                    footprints.putIfAbsent(component.representative(),footprint(component,extents));
            }
            for(var component:physical.rootComponents())footprints.put(component.representative(),footprint(component,extents));
            var offsets=new HashMap<Integer,Measure>();var permitted=new HashMap<Integer,Boolean>();
            for(var root:roots){offsets.put(root.meta().id(),Measure.known(BigInteger.ZERO));permitted.put(root.meta().id(),environment&&!physical.uncertainRoots().contains(root.meta().id()));}
            var nodes=new ArrayList<Node>();var views=new ArrayList<View>();var bases=new ArrayList<Base>();
            for(var position:ordered) {
                var data=position.data();var shape=shapes.get(data.meta().id());var key=new Key(unit.id(),data.meta().id());visits++;
                var extent=extents.get(data.meta().id());var offset=offsets.get(data.meta().id());
                boolean allowed=permitted.get(data.meta().id())&&shape.supported();
                nodes.add(new Node(key,position.parent().map(p->new Key(unit.id(),p)),position.order(),data.filler(),shape.kind(),
                    data.filler()||duplicates.contains(data.meta().id())?Optional.empty():Optional.ofNullable(entities.get(data.meta().id())),extent,data.meta().provenance()));
                int base=physical.componentOf().get(position.root()).representative();
                views.add(new View(key,new Key(unit.id(),base),offset,extent,allowed&&extent.value().isPresent(),data.meta().provenance()));
                if(position.parent().isEmpty()&&base==data.meta().id())bases.add(new Base(key,footprints.get(base),environment&&physical.allocation(base).proved(),data.meta().provenance()));
                var cursor=allowed?offset:Measure.unknown(Reason.UNKNOWN_OFFSET);
                for(var component:physical.children().get(data.meta().id())) {
                    for(var child:component.members()){offsets.put(child,cursor);permitted.put(child,allowed);}
                    cursor=plus(cursor,footprints.get(component.representative()),Reason.UNKNOWN_OFFSET);
                }
            }
            if(logicalText&&input&&!attributes.initial()&&!attributes.recursive()&&!attributes.common()&&!attributes.library()&&!attributes.definition())
                {
                var textViews=logical(unit.id(),physical,shapes);
                logicalViews.addAll(textViews);
                logicalViews.addAll(StorageRenames.logical(unit.id(),physical,resolution,entities,coverage,nodes,textViews));
            }
            if(!attributes.initial()&&!attributes.recursive()&&!attributes.common()&&!attributes.library()&&!attributes.definition())
                logicalExactViews.addAll(exactLocalText(unit,physical,shapes));
            var renames=StorageRenames.prove(unit.id(),physical,resolution,entities,coverage,nodes,views);
            if(renames.stream().anyMatch(r->!r.proved())) {
                reasons.add(Reason.RENAMES_NOT_PROVEN);
                // RENAMES has no allocation. Its unknown view retains the owning record
                // base; this does not prove any particular endpoint, offset or disjunction
                // inside the record. Writes/escapes through that view remain conservative.
            }
            layouts.put(unit.id(),new Layout(profile,nodes,bases,views,List.copyOf(reasons),physical.relations(),renames));
        }
        return new StorageLayoutSemantics(layouts,Map.of("declarations",declarations,"layoutVisits",visits,"objectPairs",0L),frontend,resolution,tables,logicalViews,logicalExactViews);
    }
    private static List<LogicalExactView> exactLocalText(CompilationUnitModel.ProgramUnit unit,StorageComponents.Unit structure,Map<Integer,Shape> shapes) {
        var working=new HashSet<Integer>();
        var declarations=new HashMap<Integer,Ast.DataEntry>();
        for(var position:structure.positions())declarations.put(position.data().meta().id(),position.data());
        for(var division:unit.program().divisions())if(division.divisionKind()==Ast.DivisionKind.DATA)
            for(var child:division.children())if(child instanceof Ast.Section section&&section.dataSectionKind()==Ast.DataSectionKind.WORKING_STORAGE)
                for(var entry:section.children())if(entry instanceof Ast.DataEntry data)working.add(data.meta().id());
        var result=new ArrayList<LogicalExactView>();
        var relations=new HashMap<Integer,StorageComponents.Relation>();
        for(var relation:structure.relations())relations.put(relation.owner(),relation);
        for(var component:structure.rootComponents()) {
            if(component.members().size()<2||!working.containsAll(component.members()))continue;
            var members=component.members();var first=shapes.get(members.get(0));
            if(first==null||!first.supported()||first.kind()!=Kind.ELEMENTARY||first.leafExtent().isEmpty())continue;
            boolean exact=true;
            for(int i=0;i<members.size();i++) {
                int id=members.get(i);var shape=shapes.get(id);
                exact&=shape!=null&&shape.supported()&&shape.kind()==Kind.ELEMENTARY&&shape.leafExtent().equals(first.leafExtent())
                    &&structure.children().getOrDefault(id,List.of()).isEmpty();
                if(i>0) {var relation=relations.get(id);exact&=relation!=null&&relation.proved()&&relation.target().isPresent()&&members.contains(relation.target().get());}
            }
            if(!exact)continue;
            for(int id:members)result.add(new LogicalExactView(new Key(unit.id(),id),new Key(unit.id(),component.representative()),first.leafExtent().orElseThrow()));
        }
        // A chain of complete group/child views has one logical TEXT value even
        // when no physical byte layout or global input completeness is proved.
        // A second child component or an overlay member would make the relation
        // partial, so neither may be silently collapsed into the same Cell.
        for(var component:structure.rootComponents()) {
            if(component.members().size()!=1)continue;
            var chain=new ArrayList<Integer>();int current=component.representative();
            while(true) {
                var shape=shapes.get(current);
                if(shape==null||!shape.supported()||declarations.get(current).filler())break;
                chain.add(current);
                if(shape.kind()==Kind.ELEMENTARY) {
                    if(shape.leafExtent().filter(n->n.signum()>0).isPresent())
                        for(int id:chain)result.add(new LogicalExactView(new Key(unit.id(),id),
                            new Key(unit.id(),component.representative()),shape.leafExtent().orElseThrow()));
                    break;
                }
                if(shape.kind()!=Kind.GROUP)break;
                var children=structure.children().getOrDefault(current,List.of());
                if(children.size()!=1||children.get(0).members().size()!=1)break;
                current=children.get(0).representative();
            }
        }
        return result;
    }
    private static List<LogicalView> logical(ResolutionContracts.ProgramUnitId unit,StorageComponents.Unit structure,Map<Integer,Shape> shapes) {
        // The same source components/shape/footprint algorithm, in character coordinates only.
        var excluded=new HashSet<Integer>();
        for(var p:structure.positions())if(!shapes.get(p.data().meta().id()).supported()||structure.uncertainRoots().contains(p.root()))excluded.add(p.root());
        var accepted=new HashSet<Integer>();
        for(var c:structure.rootComponents())if(structure.structureProven()&&structure.rootRelationsProven()
                &&c.members().stream().noneMatch(excluded::contains)&&c.members().stream().allMatch(id->structure.allocation(id).proved()))accepted.addAll(c.members());
        var extents=new HashMap<Integer,Measure>();var ordered=structure.positions();
        for(int i=ordered.size()-1;i>=0;i--) {
            var p=ordered.get(i);if(!accepted.contains(p.root()))continue;
            int id=p.data().meta().id();var shape=shapes.get(id);var length=shape.leafExtent().orElse(BigInteger.ZERO);
            if(shape.kind()==Kind.GROUP)for(var c:structure.children().get(id))length=length.add(footprint(c,extents).value().orElseThrow());
            extents.put(id,Measure.known(length));
        }
        var families=new HashMap<Integer,Integer>();
        for(var c:structure.rootComponents())if(accepted.contains(c.representative())) {
            int root=c.members().stream().max(Comparator.<Integer,BigInteger>comparing(id->extents.get(id).value().orElseThrow()).thenComparing(Comparator.reverseOrder())).orElseThrow();
            for(int member:c.members())families.put(member,root);
        }
        var starts=new HashMap<Integer,BigInteger>();for(int root:accepted)starts.put(root,BigInteger.ZERO);
        var result=new ArrayList<LogicalView>();
        for(var p:ordered) {
            if(!accepted.contains(p.root()))continue;int id=p.data().meta().id();var start=starts.get(id);
            result.add(new LogicalView(new Key(unit,id),new Key(unit,families.get(p.root())),start,extents.get(id).value().orElseThrow()));
            var cursor=start;for(var c:structure.children().get(id)) {
                for(var child:c.members())starts.put(child,cursor);
                cursor=cursor.add(footprint(c,extents).value().orElseThrow());
            }
        }
        return result;
    }
    private static Measure footprint(StorageComponents.Component component,Map<Integer,Measure> extents) {
        BigInteger max=BigInteger.ZERO;
        for(var member:component.members()) {
            var extent=extents.get(member);
            if(extent.value().isEmpty())return Measure.unknown(Reason.UNKNOWN_EXTENT);
            max=max.max(extent.value().get());
        }
        return Measure.known(max);
    }
    private record Shape(Kind kind,boolean supported,Optional<BigInteger> leafExtent) { }
    private static Shape shape(Ast.DataEntry data,Map<Integer,SemanticCoverage.Finding> coverage,Set<Integer> duplicates) {
        boolean known=!duplicates.contains(data.meta().id())
            &&data.visibility()==Ast.DeclarationVisibility.LOCAL
            &&(data.levelKind()==Ast.DataLevelKind.GROUP_OR_ELEMENTARY||data.levelKind()==Ast.DataLevelKind.STANDALONE_77)
            &&((level(data)>=1&&level(data)<=49)||level(data)==77)
            &&modeled(data,coverage);
        int pictures=0,usages=0;Optional<BigInteger> extent=Optional.empty();
        for(var clause:data.clauses()) {
            // Preserved clauses have no implemented contribution to the projected
            // layout. Their canonical coverage finding survives independently.
            if(clause instanceof Ast.PreservedDataClause)continue;
            known&=modeled(clause,coverage);
            if(clause instanceof Ast.PictureClause picture){pictures++;extent=picture.textExtent().map(BigInteger::valueOf);}
            else if(clause instanceof Ast.UsageClause usage&&usage.display())usages++;
            else if(clause instanceof Ast.ValueClause) { /* Initial content does not change physical extent. */ }
            else if(clause instanceof Ast.RedefinesClause) { /* Physical relation is proved by StorageComponents. */ }
            else known=false;
        }
        var kind=data.children().stream().allMatch(c->c.levelKind()==Ast.DataLevelKind.RENAMES_66)?Kind.ELEMENTARY:Kind.GROUP;
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
