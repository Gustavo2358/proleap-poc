package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.FactDependencies.*;

/** Grammar/source-map owned proof locality. No dataflow, runtime values or physical layout inference. */
public final class FactLocalitySemantics {
    private FactLocalitySemantics() { }
    public static Map<ResolutionContracts.ProgramUnitId,FactDependencies> prepare(CompilationUnitBuildResult frontend,
            CompilationUnitSymbolTables symbols,ReferenceResolution resolution,ResolutionAnalysisReport report,
            Optional<StorageAccessSemantics> storage) {
        if(storage.isEmpty())return Map.of();
        if(!storage.orElseThrow().belongsTo(frontend,resolution))throw new IllegalArgumentException("foreign storage snapshot");
        var result=new LinkedHashMap<ResolutionContracts.ProgramUnitId,FactDependencies>();
        for(var unit:frontend.compilationUnit().programUnits())result.put(unit.id(),analyze(frontend,symbols,resolution,report,unit.id(),storage.orElseThrow().layout()));
        return Map.copyOf(result);
    }
    private record Gap(InputKind kind,Ast.SourceProvenance origin,boolean located) { }
    public static FactDependencies analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables symbols,
            ReferenceResolution resolution,ResolutionAnalysisReport report,ResolutionContracts.ProgramUnitId id,
            StorageLayoutSemantics storage) {
        var unit=frontend.compilationUnit().find(id).orElseThrow();var program=unit.program();
        var structure=StorageComponents.analyze(frontend,symbols,resolution).unit(id);var layout=storage.layout(id);
        var positions=new HashMap<Integer,StorageComponents.Position>();structure.positions().forEach(p->positions.put(p.data().meta().id(),p));
        var allDeclarations=new HashMap<Integer,Ast.DataEntry>();var pending=new ArrayDeque<Ast.Node>();pending.add(program);
        while(!pending.isEmpty()){var n=pending.remove();if(n instanceof Ast.DataEntry d)allDeclarations.put(d.meta().id(),d);pending.addAll(Ast.children(n));}
        var rootForNode=new HashMap<Integer,Integer>();layout.views().forEach(v->rootForNode.put(v.node().node(),v.base().node()));
        for(var n:layout.nodes())if(!positions.containsKey(n.id().node()))positions.put(n.id().node(),new StorageComponents.Position(allDeclarations.get(n.id().node()),n.parent().map(StorageLayoutSemantics.Key::node),n.order(),rootForNode.get(n.id().node())));
        var coverage=new HashMap<Integer,SemanticCoverage.Finding>();frontend.coverageByProgramUnit().get(id).findings().forEach(f->coverage.put(f.astNodeId(),f));
        var shapes=new HashMap<Integer,StorageLayoutSemantics.Shape>();for(var p:positions.values())shapes.put(p.data().meta().id(),StorageLayoutSemantics.shape(p.data(),coverage,Set.of()));
        var sections=new HashMap<Integer,Ast.Section>();
        var sectionEnds=new HashMap<Integer,Long>();var boundaryOrigins=new HashMap<Integer,Ast.SourceProvenance>();
        long procedureStart=program.divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).mapToLong(d->start(d.meta().provenance())).min().orElse(end(program.meta().provenance().expanded()));
        for(var division:program.divisions())if(division.divisionKind()==Ast.DivisionKind.DATA) {
            var ordered=division.children().stream().filter(Ast.Section.class::isInstance).map(Ast.Section.class::cast).toList();
            for(int i=0;i<ordered.size();i++) {
                sectionEnds.put(ordered.get(i).meta().id(),i+1<ordered.size()?start(ordered.get(i+1).meta().provenance()):procedureStart);
                boundaryOrigins.put(ordered.get(i).meta().id(),i+1<ordered.size()?ordered.get(i+1).meta().provenance():program.divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).map(d->d.meta().provenance()).findFirst().orElse(program.meta().provenance()));
            }
        }
        for(var division:program.divisions())if(division.divisionKind()==Ast.DivisionKind.DATA)
            for(var child:division.children())if(child instanceof Ast.Section section)
                for(var root:section.children()) {
                    if(root instanceof Ast.DataEntry d)sections.put(d.meta().id(),section);
                    if(root instanceof Ast.FileDescription fd)for(var d:fd.entries())sections.put(d.meta().id(),section);
                }
        var gaps=new ArrayList<Gap>();for(var origin:program.inputProof().regions())gaps.add(new Gap(InputKind.MISSING_COPY,origin,true));
        // A located COPY never accounts for an unrelated parser, I/O, or ancestor input gap.
        boolean unlocated=report.gaps().stream().anyMatch(g->g.category()==ResolutionAnalysisReport.GapCategory.INPUT
            &&ResolutionAnalysisReport.appliesTo(g,id)
            &&!(g.code().equals("UNRESOLVED_COPY")&&id.equals(g.programUnitId())&&!program.inputProof().regions().isEmpty()));
        if(unlocated)gaps.add(new Gap(InputKind.UNLOCATED_INPUT,program.meta().provenance(),false));
        for(var p:structure.positions())if(StorageComponents.level(p.data())<0)gaps.add(new Gap(InputKind.OPAQUE_INCLUDE,p.data().meta().provenance(),true));
        var members=new TreeMap<String,List<StorageLayoutSemantics.Node>>();var baseByNode=new HashMap<Integer,String>();
        for(var v:layout.views())baseByNode.put(v.node().node(),base(v.base().node()));
        for(var n:layout.nodes())members.computeIfAbsent(baseByNode.get(n.id().node()),k->new ArrayList<>()).add(n);
        var bases=new HashMap<String,StorageLayoutSemantics.Base>();layout.bases().forEach(b->bases.put(base(b.id().node()),b));
        var closeAt=new HashMap<String,Long>();var closeOrigin=new HashMap<String,Ast.SourceProvenance>();
        for(var section:new HashSet<>(sections.values())) {
            long next=sectionEnds.get(section.meta().id());var nextOrigin=boundaryOrigins.get(section.meta().id());String previous="";
            var roots=section.children().stream().filter(Ast.DataEntry.class::isInstance).map(Ast.DataEntry.class::cast).filter(d->StorageComponents.level(d)>0).toList();
            for(int i=roots.size()-1;i>=0;i--) {var d=roots.get(i);var region=baseByNode.get(d.meta().id());
                if(region==null)continue; // Non-storage entries do not create allocation components.
                if(!region.equals(previous)){closeAt.put(region,next);closeOrigin.put(region,nextOrigin);previous=region;}
                next=start(d.meta().provenance());nextOrigin=d.meta().provenance();}
        }
        for(var entry:members.entrySet())if(!closeAt.containsKey(entry.getKey())) {var section=sections.get(bases.get(entry.getKey()).id().node());
            closeAt.put(entry.getKey(),section==null?procedureStart:sectionEnds.get(section.meta().id()));
            closeOrigin.put(entry.getKey(),section==null?program.meta().provenance():boundaryOrigins.get(section.meta().id()));}
        var declarationInputs=new HashMap<String,List<String>>();var context=new HashMap<String,List<String>>();var closure=new HashMap<String,List<String>>();
        var inputs=new ArrayList<Input>();int ordinal=0;
        for(var gap:gaps) {
            var contexts=new ArrayList<String>();var closures=new ArrayList<String>();var declarations=new ArrayList<String>();var input="input:"+ordinal++;
            for(var entry:members.entrySet()) {
                var b=bases.get(entry.getKey());var root=positions.get(b.id().node()).data();var section=sections.get(root.meta().id());
                // Prefix proof is intentionally conservative after an unknown insertion.
                // Allocation/visibility also depend on the entire declaration header.
                // A COPY inside its clauses may change EXTERNAL/GLOBAL or identity.
                boolean affectsContext=!gap.located()||start(gap.origin())<end(root.meta().provenance().expanded());
                long close=closeAt.get(entry.getKey());
                boolean affectsClosure=affectsContext||!gap.located()||start(gap.origin())<close;
                if(affectsContext){contexts.add(entry.getKey());context.computeIfAbsent(entry.getKey(),k->new ArrayList<>()).add(input);}
                if(affectsClosure){closures.add(entry.getKey());closure.computeIfAbsent(entry.getKey(),k->new ArrayList<>()).add(input);}
            }
            for(var entry:members.values())for(var n:entry) {
                var ast=positions.get(n.id().node()).data();
                if(!gap.located()||start(gap.origin())<end(ast.meta().provenance().expanded())) {
                    var subject=node(n.id().node());declarations.add(subject);declarationInputs.computeIfAbsent(subject,k->new ArrayList<>()).add(input);
                }
            }
            inputs.add(new Input(input,gap.kind(),false,contexts,closures,declarations,origin(gap.origin())));
        }
        inputs.add(new Input("input:profile",InputKind.PHYSICAL_PROFILE,layout.profile()!=StorageLayoutSemantics.Profile.UNSPECIFIED,List.of(),List.of(),List.of(),origin(program.meta().provenance())));
        var proofs=new ArrayList<Proof>();var regions=new ArrayList<Region>();var facts=new ArrayList<Fact>();var bindings=new ArrayList<Binding>();
        var attrs=program.attributes();boolean ordinary=!attrs.recursive()&&!attrs.common()&&!attrs.library()&&!attrs.definition()&&!attrs.initial();
        var exacts=new HashMap<Integer,Integer>();for(var e:storage.logicalExactViews())if(e.node().unit().equals(id))exacts.put(e.node().node(),e.representative().node());
        var views=new HashMap<Integer,StorageLayoutSemantics.View>();layout.views().forEach(v->views.put(v.node().node(),v));
        for(var entry:members.entrySet()) {
            var region=entry.getKey();var b=bases.get(region);int rootId=b.id().node();var root=positions.get(rootId).data();var section=sections.get(rootId);
            var ns=entry.getValue();regions.add(new Region(region,ns.stream().map(n->node(n.id().node())).toList(),origin(b.origin())));
            var syntax=proof(proofs,ProofKind.SOURCE_SYNTAX,region,region,StorageComponents.level(root)>0,List.of(),List.of(),root.meta().provenance());
            var ctx=proof(proofs,ProofKind.REGION_CONTEXT,region,region,true,List.of(syntax),context.getOrDefault(region,List.of()),root.meta().provenance());
            var boundary=proof(proofs,ProofKind.REGION_BOUNDARY,region,region,section!=null,List.of(syntax),List.of(),closeOrigin.get(region));
            var closed=proof(proofs,ProofKind.REGION_CLOSURE,region,region,section!=null,List.of(syntax,boundary),closure.getOrDefault(region,List.of()),root.meta().provenance());
            boolean local=section!=null&&ordinary
                &&(section.dataSectionKind()==Ast.DataSectionKind.WORKING_STORAGE
                    ||section.dataSectionKind()==Ast.DataSectionKind.FILE&&structure.allocation(rootId).proved())
                &&ns.stream().allMatch(n->positions.get(n.id().node()).data().visibility()==Ast.DeclarationVisibility.LOCAL)
                &&structure.rootRelationsProven();
            var allocation=proof(proofs,ProofKind.LOCAL_ALLOCATION,region,region,local||b.independent(),List.of(syntax,ctx),List.of(),b.origin());
            fact(facts,FactKind.STORAGE_IDENTITY,region,region,List.of(allocation));
            boolean aliases=ns.stream().noneMatch(n->(structure.componentOf().get(n.id().node())==null||structure.componentOf().get(n.id().node()).members().size()>1)
                ||positions.get(n.id().node()).data().clauses().stream().anyMatch(c->c instanceof Ast.OccursClause||c instanceof Ast.RedefinesClause))
                &&structure.renames().stream().noneMatch(p->baseByNode.getOrDefault(p.root(),"").equals(region))
                &&!structure.uncertainRoots().contains(rootId);
            boolean wholeExact=structure.renames().stream().noneMatch(p->baseByNode.getOrDefault(p.root(),"").equals(region))&&ns.stream().allMatch(n->exacts.containsKey(n.id().node()))&&ns.stream().map(n->exacts.get(n.id().node())).distinct().count()==1;
            var aliasOrigin=structure.relations().stream().filter(r->baseByNode.getOrDefault(r.owner(),"").equals(region)).map(r->r.clause().meta().provenance()).findFirst().orElse(b.origin());
            var aliasInventory=proof(proofs,ProofKind.ALIAS_INVENTORY,region,region,aliases||wholeExact,List.of(syntax),List.of(),aliasOrigin);
            var alias=proof(proofs,ProofKind.ALIAS_CLOSURE,region,region,true,List.of(closed,aliasInventory),List.of(),b.origin());
            var profile=proof(proofs,ProofKind.PROFILE,region,region,true,List.of(),List.of("input:profile"),b.origin());
            boolean allocated=(local||b.independent())&&context.getOrDefault(region,List.of()).isEmpty()&&StorageComponents.level(root)>0;
            boolean isolated=allocated&&closure.getOrDefault(region,List.of()).isEmpty()&&(aliases||wholeExact)&&section!=null;
            var cells=new HashMap<Integer,String>();
            var independentPath=new HashMap<Integer,Boolean>();
            boolean localAliases=structure.renames().stream().noneMatch(p->baseByNode.getOrDefault(p.root(),"").equals(region))
                &&!structure.uncertainRoots().contains(rootId)
                &&ns.stream().noneMatch(n->positions.get(n.id().node()).data().clauses().stream()
                    .anyMatch(c->c instanceof Ast.OccursClause o&&o.dependingOn()!=null));
            for(var n:ns) {
                int nid=n.id().node();var subject=node(nid);var ast=positions.get(nid).data();var shape=shapes.get(nid);
                var s=proof(proofs,ProofKind.SOURCE_SYNTAX,subject,region,StorageComponents.level(ast)>0,List.of(),List.of(),n.origin());
                boolean text=wholeExact||shape.supported()&&shape.kind()==StorageLayoutSemantics.Kind.ELEMENTARY&&shape.leafExtent().filter(v->v.signum()>0).isPresent();
                var header=proof(proofs,ProofKind.DECLARATION_CONTEXT,subject,region,true,List.of(s),declarationInputs.getOrDefault(subject,List.of()),n.origin());
                var logical=proof(proofs,ProofKind.LOGICAL_TYPE,subject,region,text,List.of(s,header),List.of(),n.origin());
                var v=views.get(nid);
                var physical=proof(proofs,ProofKind.PHYSICAL_VIEW,subject,region,v.offset().value().isPresent()&&v.extent().value().isPresent(),List.of(s,profile,ctx,closed),List.of(),n.origin());
                fact(facts,FactKind.SOURCE_IDENTITY,subject,region,List.of(s));fact(facts,FactKind.LOGICAL_TEXT,subject,region,List.of(logical));
                fact(facts,FactKind.PHYSICAL_VIEW,subject,region,List.of(physical));
                String itemAlias=alias;
                boolean itemIsolated=isolated;
                if(!aliases&&!wholeExact) {
                    var path=new ArrayDeque<Integer>();int current=nid;
                    while(!independentPath.containsKey(current)) {
                        path.push(current);var parent=positions.get(current).parent();
                        if(parent.isEmpty())break;current=parent.orElseThrow();
                    }
                    boolean separate=independentPath.getOrDefault(current,true);
                    while(!path.isEmpty()) {
                        int at=path.pop();var component=structure.componentOf().get(at);
                        separate &= component!=null&&component.members().size()==1
                            &&positions.get(at).data().clauses().stream().noneMatch(c->c instanceof Ast.OccursClause||c instanceof Ast.RedefinesClause);
                        independentPath.put(at,separate);
                    }
                    boolean itemInventory=localAliases&&independentPath.get(nid);
                    var inventory=proof(proofs,ProofKind.ALIAS_INVENTORY,subject,region,itemInventory,List.of(s),List.of(),n.origin());
                    itemAlias=proof(proofs,ProofKind.ALIAS_CLOSURE,subject,region,true,List.of(closed,inventory),List.of(),n.origin());
                    itemIsolated=allocated&&closure.getOrDefault(region,List.of()).isEmpty()&&itemInventory&&section!=null;
                }
                fact(facts,FactKind.LOCAL_CELL,subject,region,List.of(logical,itemAlias,allocation));
                if(itemIsolated&&text&&declarationInputs.getOrDefault(subject,List.of()).isEmpty()&&!n.filler()&&n.entity().isPresent())cells.put(nid,node(wholeExact?exacts.get(nid):nid));
            }
            var descendants=new HashMap<Integer,Set<String>>();
            if(!wholeExact)for(var cell:cells.entrySet()) {int current=cell.getKey();var visited=new HashSet<Integer>();
                while(visited.add(current)) {descendants.computeIfAbsent(current,k->new TreeSet<>()).add(cell.getValue());
                    var parent=positions.get(current).parent();if(parent.isEmpty())break;current=parent.orElseThrow();}}
            for(var n:ns) {
                int nid=n.id().node();var exact=cells.getOrDefault(nid,"");
                var targets=!exact.isEmpty()?List.of(exact):List.copyOf(descendants.getOrDefault(nid,Set.of()));
                bindings.add(new Binding(node(nid),region,exact,targets,allocated&&exact.isEmpty()?List.of(region):List.of(),List.of(allocation)));
            }
        }
        return new FactDependencies("FRONTEND_FACT_DEPENDENCY_LOCALITY_R2",inputs,proofs,regions,facts,bindings);
    }
    private static String proof(List<Proof> out,ProofKind kind,String subject,String scope,boolean premise,List<String> deps,List<String> inputs,Ast.SourceProvenance p) {
        var id=kind+"/"+subject;out.add(new Proof(id,kind,scope,subject,premise,deps,inputs,"IBM6.4/R2/"+kind,origin(p)));return id;
    }
    private static void fact(List<Fact> out,FactKind kind,String subject,String region,List<String> deps){out.add(new Fact(kind+"/"+subject,kind,subject,region,deps));}
    private static String node(int id){return "storage-node:"+id;}
    private static String base(int id){return "storage-base:"+id;}
    private static long start(Ast.SourceProvenance p){return ((long)p.expanded().startLine()<<32)+p.expanded().startColumn();}
    private static long end(Ast.SourceLocation p){return ((long)p.endLine()<<32)+p.endColumn();}
    private static CobolSemanticProduct.Provenance origin(Ast.SourceProvenance p){return new CobolSemanticProduct.Provenance(location(p.expanded()),location(p.original()),p.includeChain().stream().map(c->new CobolSemanticProduct.IncludeFrame(c.includingFile(),c.requestedName(),c.includedFile(),c.includeLine())).toList(),p.exact());}
    private static CobolSemanticProduct.Location location(Ast.SourceLocation p){return new CobolSemanticProduct.Location(p.file(),p.startLine(),p.startColumn(),p.endLine(),p.endColumn());}
}
