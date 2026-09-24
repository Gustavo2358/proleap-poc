package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.FactDependencies.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class FactDependencyLocalityTest {
    static CobolSemanticPort publish(String data,StorageLayoutSemantics.Profile profile) {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data,"MOVE 'PROGA001' TO TARGET.\nCALL TARGET.\nGOBACK."),"locality.cbl");
        return ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),profile);
    }
    static CobolSemanticPort publish(String data){return publish(data,StorageLayoutSemantics.Profile.UNSPECIFIED);}
    static String node(CobolSemanticPort p,String name) {
        var d=p.dataDeclarations().stream().filter(x->x.canonicalName().equals(name)).findFirst().orElseThrow();
        return "storage-node:"+p.storage().nodes().stream().filter(n->n.data().equals(Optional.of(d.id()))).findFirst().orElseThrow().id().localId();
    }
    static boolean known(CobolSemanticPort p,String name,FactKind kind) {
        var graph=p.factDependencies().orElseThrow();var subject=node(p,name);
        return graph.available(graph.facts().stream().filter(f->f.kind()==kind&&f.subject().equals(subject)).findFirst().orElseThrow());
    }
    @Test void projectionReadsPreparedFactsOnly() throws Exception {
        var source=Files.readString(Path.of("src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/projection/CobolSemanticProductProjector.java"));
        assertFalse(source.substring(source.indexOf("public static ScopedProjection projectScoped")).contains("FactLocalitySemantics."));
    }
    @Test void profileIsOnlyAPhysicalDependency() throws Exception {
        var data="01 RECORD-A.\n 05 TARGET PIC X(8).\n 05 COUNTER PIC S9(9) COMP.\n 05 UNUSED-TEXT PIC X(2).\n01 SENTINEL PIC X.";
        var absent=publish(data);var present=publish(data,StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        assertTrue(known(absent,"TARGET",FactKind.SOURCE_IDENTITY));assertTrue(known(absent,"TARGET",FactKind.LOGICAL_TEXT));
        assertTrue(known(absent,"TARGET",FactKind.LOCAL_CELL));assertFalse(known(absent,"TARGET",FactKind.PHYSICAL_VIEW));
        assertFalse(known(absent,"SENTINEL",FactKind.PHYSICAL_VIEW));assertTrue(known(present,"SENTINEL",FactKind.PHYSICAL_VIEW));
        for(var kind:List.of(FactKind.SOURCE_IDENTITY,FactKind.LOGICAL_TEXT,FactKind.LOCAL_CELL))assertEquals(known(absent,"TARGET",kind),known(present,"TARGET",kind));
        var out=Path.of("target/fact-dependency-r2");Files.createDirectories(out);Files.write(out.resolve("mixed-profile-absent.json"),SemanticProductJsonWriter.serialize(absent));
        assertEquals("2.40.0",new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(absent)).path("contractVersion").asText());
    }
    @Test void missingInputNeedsClosedRegionNotLexicalDistance() throws Exception {
        var closed=publish("01 RECORD-A.\n 05 TARGET PIC X(8).\n 05 COUNTER PIC 9.\n01 SENTINEL PIC X.\nCOPY UNKNOWN-DATA.");
        var open=publish("01 RECORD-A.\n 05 TARGET PIC X(8).\n 05 COUNTER PIC 9.\nCOPY UNKNOWN-DATA.");
        assertTrue(known(closed,"TARGET",FactKind.LOCAL_CELL));assertFalse(known(open,"TARGET",FactKind.LOCAL_CELL));
        assertTrue(known(open,"TARGET",FactKind.SOURCE_IDENTITY));
        assertTrue(open.factDependencies().orElseThrow().inputs().stream().anyMatch(i->i.kind()==InputKind.MISSING_COPY&&!i.available()));
        var out=Path.of("target/fact-dependency-r2");Files.createDirectories(out);
        Files.write(out.resolve("closed-copy.json"),SemanticProductJsonWriter.serialize(closed));Files.write(out.resolve("open-copy.json"),SemanticProductJsonWriter.serialize(open));
    }
    @Test void aliasAndOpaqueIncludeDoNotProduceFalseCells() throws Exception {
        var alias=publish("01 RECORD-A.\n 05 TARGET PIC X(8).\n 05 ALIAS REDEFINES TARGET PIC X(4).\n01 SENTINEL PIC X.");
        assertFalse(known(alias,"TARGET",FactKind.LOCAL_CELL));
        var opaque=publish("01 RECORD-A.\n 05 TARGET PIC X(8).\nEXEC SQL INCLUDE SQLCA END-EXEC.");
        assertTrue(known(opaque,"TARGET",FactKind.SOURCE_IDENTITY));assertFalse(known(opaque,"TARGET",FactKind.LOCAL_CELL));
        assertTrue(opaque.factDependencies().orElseThrow().inputs().stream().anyMatch(i->i.kind()==InputKind.OPAQUE_INCLUDE));
        var out=Path.of("target/fact-dependency-r2");Files.createDirectories(out);
        Files.write(out.resolve("alias.json"),SemanticProductJsonWriter.serialize(alias));Files.write(out.resolve("opaque-include.json"),SemanticProductJsonWriter.serialize(opaque));
    }
    @Test void causalMutationsAndPermutation() throws Exception {
        var graph=publish("01 RECORD-A.\n 05 TARGET PIC X(8).\n 05 COUNTER PIC 9.\n01 SENTINEL PIC X.\nCOPY UNKNOWN-DATA.").factDependencies().orElseThrow();
        var shuffled=new ArrayList<>(graph.facts());Collections.reverse(shuffled);
        assertEquals(graph,new FactDependencies(graph.authority(),graph.inputs(),graph.proofs(),graph.regions(),shuffled,graph.bindings()));
        for(var kind:List.of(ProofKind.DECLARATION_CONTEXT,ProofKind.ALIAS_CLOSURE,ProofKind.ALIAS_INVENTORY,ProofKind.REGION_BOUNDARY,ProofKind.REGION_CLOSURE,ProofKind.LOCAL_ALLOCATION)) {
            var victim=graph.proofs().stream().filter(p->p.kind()==kind).findFirst().orElseThrow();
            var proofs=graph.proofs().stream().filter(p->!p.equals(victim)).toList();
            assertThrows(IllegalArgumentException.class,()->new FactDependencies(graph.authority(),graph.inputs(),proofs,graph.regions(),graph.facts(),graph.bindings()));
        }
        var victim=graph.proofs().stream().filter(p->p.kind()==ProofKind.REGION_CLOSURE&&!p.inputs().isEmpty()).findFirst().orElseThrow();
        var proofs=graph.proofs().stream().map(p->p.equals(victim)?new Proof(p.id(),p.kind(),p.scope(),p.subject(),p.localPremise(),p.dependencies(),List.of(),p.rule(),p.provenance()):p).toList();
        assertThrows(IllegalArgumentException.class,()->new FactDependencies(graph.authority(),graph.inputs(),proofs,graph.regions(),graph.facts(),graph.bindings()));
    }
    @Test void M1_M5_M7_independentInputDoesNotChangeClosedFacets() {
        var source="01 RECORD-A.\n 05 TARGET PIC X(8).\n 05 COUNTER PIC 9.\n01 SENTINEL PIC X.";
        var base=publish(source);var a=publish(source+"\nCOPY UNKNOWN-AAAA.");var b=publish(source+"\nCOPY UNKNOWN-BBBB.");
        var node=node(base,"TARGET");var region=base.factDependencies().orElseThrow().facts().stream().filter(f->f.subject().equals(node)).findFirst().orElseThrow().region();
        for(var peer:List.of(a,b)) {
            var first=base.factDependencies().orElseThrow();var second=peer.factDependencies().orElseThrow();
            assertEquals(first.facts().stream().filter(f->f.region().equals(region)).toList(),second.facts().stream().filter(f->f.region().equals(region)).toList());
            for(var f:first.facts())if(f.region().equals(region))assertEquals(first.available(f),second.available(f),f.id());
        }
        assertEquals(a.factDependencies(),b.factDependencies(),"missing input spelling carries no independence semantics");
    }
    @Test void M2_removingDiagnosticCannotSupplyMissingContent() throws Exception {
        var source=ScalarMoveCheckpoint4ATest.program("01 RECORD-A.\n 05 TARGET PIC X(8).\nCOPY UNKNOWN-DATA.","GOBACK.");
        var a=AstBoundaryTestSupport.analyze(source,"locality.cbl");var id=a.model().programUnits().get(0).id();
        var stripped=ResolutionAnalysisReport.compose(a.build(),new ResolutionAnalysisReport.FrontendState(0,0,0,List.of()),a.occurrences(),a.resolution());
        var layout=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.UNSPECIFIED);
        var before=FactLocalitySemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),id,layout);
        var after=FactLocalitySemantics.analyze(a.build(),a.tables(),a.resolution(),stripped,id,layout);
        assertEquals(before,after,"source-map input relation survives diagnostic removal");
        var product=ExplorerMain.publishSemanticProduct(id,a.build(),a.tables(),a.occurrences(),a.resolution(),stripped);
        var out=Path.of("target/fact-dependency-r2");Files.createDirectories(out);
        Files.write(out.resolve("removed-diagnostic.json"),SemanticProductJsonWriter.serialize(product));
        assertFalse(known(product,"TARGET",FactKind.LOCAL_CELL));
        var parserGap=ResolutionAnalysisReport.compose(a.build(),new ResolutionAnalysisReport.FrontendState(0,0,1,List.of()),a.occurrences(),a.resolution());
        var uncertain=FactLocalitySemantics.analyze(a.build(),a.tables(),a.resolution(),parserGap,id,layout);
        assertTrue(uncertain.inputs().stream().anyMatch(i->i.kind()==InputKind.UNLOCATED_INPUT));
        assertTrue(uncertain.facts().stream().filter(f->f.kind()==FactKind.STORAGE_IDENTITY).noneMatch(uncertain::available));
        assertTrue(after.proofs().stream().filter(p->p.kind()==ProofKind.REGION_CLOSURE).anyMatch(p->!after.proofAvailability().get(p.id())));
    }
    @Test void M9_M10_storageGapsAndIrrelevantStatementOrderPreserveControl() {
        String data="01 RECORD-A.\n 05 TARGET PIC X(8).\n 05 COUNTER PIC 9.\n01 SENTINEL PIC X.";
        var a=publish(data);var b=publish(data+"\nCOPY UNKNOWN-DATA.");
        var x=a.controlTopology().orElseThrow();var y=b.controlTopology().orElseThrow();
        assertEquals(x.outcomes().stream().map(o->o.kind()+":"+o.role()+":"+o.target().kind()+":"+o.target().reference()).toList(),y.outcomes().stream().map(o->o.kind()+":"+o.role()+":"+o.target().kind()+":"+o.target().reference()).toList());
        var source=ScalarMoveCheckpoint4ATest.program(data,"DISPLAY 'ONE'.\nDISPLAY 'TWO'.\nGOBACK.");
        var aa=AstBoundaryTestSupport.analyze(source,"order.cbl");var bb=AstBoundaryTestSupport.analyze(source.replace("'ONE'","'TEMP'").replace("'TWO'","'ONE'").replace("'TEMP'","'TWO'"),"order.cbl");
        var left=ExplorerMain.publishSemanticProduct(aa.model().programUnits().get(0).id(),aa.build(),aa.tables(),aa.occurrences(),aa.resolution(),aa.report());
        var right=ExplorerMain.publishSemanticProduct(bb.model().programUnits().get(0).id(),bb.build(),bb.tables(),bb.occurrences(),bb.resolution(),bb.report());
        assertEquals(left.factDependencies(),right.factDependencies(),"irrelevant statement reorder preserves data proof identity");
    }
    @Test void missingInputInsideDeclarationHeaderDoesNotProveAllocation() throws Exception {
        var product=publish("01 TARGET\nCOPY UNKNOWN-CLAUSE.\n PIC X(8).\n01 SENTINEL PIC X.");
        var g=product.factDependencies().orElseThrow();var target=node(product,"TARGET");
        var region=g.regions().stream().filter(r->r.members().contains(target)).findFirst().orElseThrow();
        var allocation=g.facts().stream().filter(f->f.subject().equals(region.id())&&f.kind()==FactKind.STORAGE_IDENTITY).findFirst().orElseThrow();
        assertFalse(g.available(allocation),"unknown header clauses can change allocation/visibility; a name alone is insufficient");
        assertFalse(known(product,"TARGET",FactKind.LOGICAL_TEXT),"unknown header can separate PIC from the observed declaration");
        var child=publish("01 RECORD-A.\n 05 TARGET\nCOPY UNKNOWN-CLAUSE.\n PIC X(8).\n01 SENTINEL PIC X.");
        assertFalse(known(child,"TARGET",FactKind.LOGICAL_TEXT));
        assertFalse(known(child,"TARGET",FactKind.LOCAL_CELL));
        var out=Path.of("target/fact-dependency-r2");Files.createDirectories(out);
        Files.write(out.resolve("header-copy.json"),SemanticProductJsonWriter.serialize(product));
        Files.write(out.resolve("child-header-copy.json"),SemanticProductJsonWriter.serialize(child));
    }
    @Test void graphGrowthIsBoundedByDeclarationsAndDependencies() throws Exception {
        var rows=new ArrayList<Map<String,Object>>();var out=Path.of("target/fact-dependency-r2/scale");Files.createDirectories(out);
        for(int count:List.of(1,10,100,1000)) {
            var source=new StringBuilder();for(int i=0;i<count;i++)source.append("01 RECORD-").append(i).append(".\n 05 ").append(i==0?"TARGET":"TEXT-"+i).append(" PIC X(8).\n 05 NUMBER-").append(i).append(" PIC S9(9) COMP.\n");
            var started=System.nanoTime();var product=publish(source.toString());var graph=product.factDependencies().orElseThrow();var bytes=SemanticProductJsonWriter.serialize(product);
            long links=graph.proofs().stream().mapToLong(p->p.dependencies().size()+p.inputs().size()).sum()+graph.facts().stream().mapToLong(f->f.dependencies().size()).sum();
            assertEquals(count,graph.regions().size());assertEquals(count*13,graph.facts().size());assertTrue(links<=count*60L);
            Files.write(out.resolve(count+".json"),bytes);rows.add(Map.of("records",count,"facts",graph.facts().size(),"proofs",graph.proofs().size(),"dependencies",links,"SPBytes",bytes.length,"frontendMs",(System.nanoTime()-started)/1e6));
        }
        Files.writeString(out.resolve("measurements.json"),new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(rows));
    }

}
