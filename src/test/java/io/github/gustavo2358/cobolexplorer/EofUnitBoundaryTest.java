package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.FactDependencies.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;

/** R4: EOF establishes ownership only; the unchanged R2 graph decides availability. */
class EofUnitBoundaryTest {
    @TempDir Path directory;
    static final String CLOSED="01 OWN-RECORD.\n 05 TARGET PIC X(8).\n01 SENTINEL PIC X.\nCOPY ABSENT-MEMBER.";
    static String source(String name,String data,boolean end) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. "+name+".\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n"+data
            +"\nPROCEDURE DIVISION.\nMOVE 'BEFORE01' TO TARGET.\nCALL TARGET.\nGOBACK.\n"+(end?"END PROGRAM "+name+".\n":"");
    }
    static AstBoundaryTestSupport.Analysis analyze(String source) {return AstBoundaryTestSupport.analyze(source,"boundary.cbl");}
    static CobolSemanticPort publish(AstBoundaryTestSupport.Analysis a,int i,StorageLayoutSemantics.Profile profile) {
        return ExplorerMain.publishSemanticProduct(a.model().programUnits().get(i).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),profile);
    }
    static CobolSemanticPort publish(String data,boolean end) {return publish(analyze(source("BOUNDARY",data,end)),0,StorageLayoutSemantics.Profile.UNSPECIFIED);}
    static boolean known(CobolSemanticPort p,FactKind kind) {return FactDependencyLocalityTest.known(p,"TARGET",kind);}
    static Map<String,Boolean> availability(FactDependencies g) {
        var out=new TreeMap<String,Boolean>();g.facts().forEach(f->out.put(f.id(),g.available(f)));return out;
    }
    @Test void A_B_eofAndEndEquivalent() {
        var a=analyze(source("BOUNDARY",CLOSED,false));var b=analyze(source("BOUNDARY",CLOSED,true));
        var pa=publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);var pb=publish(b,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertEquals(1,a.model().programUnits().get(0).program().inputProof().copies().size());
        assertEquals(b.model().programUnits().get(0).program().inputProof(),a.model().programUnits().get(0).program().inputProof());
        assertEquals(availability(pb.factDependencies().orElseThrow()),availability(pa.factDependencies().orElseThrow()));
        assertEquals(pb.factDependencies().orElseThrow().proofAvailability(),pa.factDependencies().orElseThrow().proofAvailability());
        assertTrue(known(pa,FactKind.LOCAL_CELL));assertTrue(known(pa,FactKind.LOGICAL_TEXT));assertFalse(known(pa,FactKind.PHYSICAL_VIEW));
        assertTrue(pa.factDependencies().orElseThrow().inputs().stream().anyMatch(i->i.kind()==InputKind.MISSING_COPY&&!i.available()));
        assertTrue(pa.factDependencies().orElseThrow().inputs().stream().noneMatch(i->i.kind()==InputKind.UNLOCATED_INPUT));
        assertEquals(pb.controlTopology().orElseThrow().outcomes().stream().map(o->o.kind()+":"+o.role()+":"+o.target().kind()+":"+o.target().reference()).toList(),pa.controlTopology().orElseThrow().outcomes().stream().map(o->o.kind()+":"+o.role()+":"+o.target().kind()+":"+o.target().reference()).toList());
    }
    @Test void C_D_M5_headersRemainUnknown() {
        for(String data:List.of("01 TARGET\nCOPY ABSENT-MEMBER.\n PIC X(8).\n01 SENTINEL PIC X.",
                "01 OWN-RECORD.\n 05 TARGET\nCOPY ABSENT-MEMBER.\n PIC X(8).\n01 SENTINEL PIC X.")) {
            var p=publish(data,false);assertTrue(known(p,FactKind.SOURCE_IDENTITY));
            assertFalse(known(p,FactKind.LOGICAL_TEXT));assertFalse(known(p,FactKind.LOCAL_CELL));
            assertEquals(availability(publish(data,true).factDependencies().orElseThrow()),availability(p.factDependencies().orElseThrow()));
        }
    }
    @Test void E_M6_openGroupRemainsOpen() {
        var p=publish("01 OWN-RECORD.\n 05 TARGET PIC X(8).\nCOPY ABSENT-MEMBER.",false);var g=p.factDependencies().orElseThrow();
        assertFalse(known(p,FactKind.LOCAL_CELL));
        assertTrue(g.proofs().stream().filter(x->x.kind()==ProofKind.REGION_CLOSURE).noneMatch(x->g.proofAvailability().get(x.id())));
    }
    @Test void F_M2_nestedAndContainingCannotInheritEof() {
        var a=analyze(source("OUTER",CLOSED,false)+source("INNER",CLOSED,false));assertEquals(2,a.model().programUnits().size());
        assertNotNull(a.model().programUnits().get(1).parentId());
        a.model().programUnits().forEach(u->assertTrue(u.program().inputProof().copies().isEmpty()));
    }
    @Test void G_M3_previousUnitsDoNotInheritFinalEof() {
        var a=analyze(source("UNIT-A",CLOSED,true)+source("UNIT-B",CLOSED,false));assertEquals(2,a.model().programUnits().size());
        var first=a.model().programUnits().get(0);var last=a.model().programUnits().get(1);
        assertNull(first.parentId());assertNull(last.parentId());
        assertEquals(1,first.program().inputProof().copies().size());assertEquals(1,last.program().inputProof().copies().size());
        assertNotEquals(first.program().inputProof().regions(),last.program().inputProof().regions());
        assertTrue(a.report().gaps().stream().filter(g->g.code().equals("UNRESOLVED_COPY")).allMatch(g->g.programUnitId()!=null));
        for(int i=0;i<2;i++)assertTrue(known(publish(a,i,StorageLayoutSemantics.Profile.UNSPECIFIED),FactKind.LOCAL_CELL));
        var moved=analyze(source("UNIT-A",CLOSED+"\nCOPY ABSENT-MEMBER.",true)+source("UNIT-B",CLOSED.replace("COPY ABSENT-MEMBER.",""),false));
        assertEquals(2,moved.model().programUnits().get(0).program().inputProof().copies().size());
        assertEquals(0,moved.model().programUnits().get(1).program().inputProof().copies().size());
    }
    @Test void I_M4_sameSpellingKeepsOccurrenceIdentity() {
        var a=analyze(source("BOUNDARY",CLOSED+"\n01 LATER.\n 05 OTHER-TEXT PIC X.\nCOPY ABSENT-MEMBER.",false));
        var proof=a.model().programUnits().get(0).program().inputProof();assertEquals(2,proof.copies().size());assertEquals(2,new HashSet<>(proof.regions()).size());
        var g=publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED).factDependencies().orElseThrow();
        assertEquals(2,g.inputs().stream().filter(i->i.kind()==InputKind.MISSING_COPY).count());
        var used=g.inputs().stream().filter(i->i.kind()==InputKind.MISSING_COPY).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,()->new FactDependencies(g.authority(),g.inputs().stream().filter(i->!i.equals(used)).toList(),g.proofs(),g.regions(),g.facts(),g.bindings()));
    }
    @Test void M7_inventoryPermutation() {
        var g=publish(CLOSED,false).factDependencies().orElseThrow();
        var inputs=new ArrayList<>(g.inputs());var proofs=new ArrayList<>(g.proofs());var facts=new ArrayList<>(g.facts());var bindings=new ArrayList<>(g.bindings());var regions=new ArrayList<>(g.regions());
        Collections.reverse(inputs);Collections.reverse(proofs);Collections.reverse(facts);Collections.reverse(bindings);Collections.reverse(regions);
        var shuffled=new FactDependencies(g.authority(),inputs,proofs,regions,facts,bindings);assertEquals(g,shuffled);assertEquals(availability(g),availability(shuffled));
    }
    @Test void M8_profileAndAliasSeparation() {
        var a=analyze(source("BOUNDARY",CLOSED,false));var absent=publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        var present=publish(a,0,StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        for(var kind:List.of(FactKind.SOURCE_IDENTITY,FactKind.LOGICAL_TEXT,FactKind.LOCAL_CELL))assertEquals(known(absent,kind),known(present,kind));
        assertFalse(known(absent,FactKind.PHYSICAL_VIEW));
        var explicit=publish(analyze(source("BOUNDARY",CLOSED,true)),0,StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        assertEquals(known(explicit,FactKind.PHYSICAL_VIEW),known(present,FactKind.PHYSICAL_VIEW),"EOF cannot strengthen the existing physical contract");
        var complete=analyze(source("BOUNDARY",CLOSED.replace("COPY ABSENT-MEMBER.",""),false));
        assertTrue(known(publish(complete,0,StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047),FactKind.PHYSICAL_VIEW));
        for(String target:List.of("TARGET","MISSING-TARGET")) {
            var alias=publish(CLOSED.replace("01 SENTINEL"," 05 ALIAS REDEFINES "+target+" PIC X(4).\n01 SENTINEL"),false);
            assertFalse(known(alias,FactKind.LOCAL_CELL));
        }
    }
    @Test void M9_laterDeclarationRespectsPrefixCausality() {
        var before=publish(CLOSED,false);var after=publish(CLOSED+"\n01 LATER PIC X(8).",false);
        assertTrue(known(before,FactKind.LOCAL_CELL));assertTrue(known(after,FactKind.LOCAL_CELL));
        assertFalse(FactDependencyLocalityTest.known(after,"LATER",FactKind.LOCAL_CELL));
    }
    @Test void M10_diagnosticRemovalDoesNotSupplyContent() {
        var a=analyze(source("BOUNDARY","01 OWN-RECORD.\n 05 TARGET PIC X(8).\nCOPY ABSENT-MEMBER.",false));
        var stripped=ResolutionAnalysisReport.compose(a.build(),new ResolutionAnalysisReport.FrontendState(0,0,0,List.of()),a.occurrences(),a.resolution());
        var id=a.model().programUnits().get(0).id();var layout=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.UNSPECIFIED);
        var before=FactLocalitySemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),id,layout);
        var after=FactLocalitySemantics.analyze(a.build(),a.tables(),a.resolution(),stripped,id,layout);
        assertEquals(before,after);assertTrue(after.inputs().stream().anyMatch(i->i.kind()==InputKind.MISSING_COPY&&!i.available()));
    }
    @Test void physicalBoundarySurvivesCompositionButNotFragmentsOrIncludes() {
        String raw="       IDENTIFICATION DIVISION.\n       PROGRAM-ID. BOUNDARY.\n";
        var map=SourceNormalizer.normalize(raw,"physical.cbl",SourceNormalizer.SourceFormat.FIXED).sourceMap();
        var boundary=map.physicalBoundary().orElseThrow();
        assertEquals("physical.cbl",boundary.sourceFile());
        assertEquals(raw.codePointCount(0,raw.length()),boundary.originalOffset());
        var fragment=map.transformedSlice(0,1," ");assertTrue(fragment.physicalBoundary().isEmpty());
        assertEquals(boundary,map.replace(0,1,fragment).physicalBoundary().orElseThrow());
        var included=SourceMap.identity("X","copy.cpy");
        assertEquals(boundary,map.replace(0,1,included).physicalBoundary().orElseThrow());
        assertFalse(fragment.provenance(0,1).exact(),"transformed text never gains exact provenance from EOF");
    }
    @Test void H_parserAndLexerCorruptionRejectEofOwnership() throws Exception {
        for(String suffix:List.of("MOVE .\n","~\n")) {
            var input=directory.resolve("input.cbl");Files.writeString(input,(source("BOUNDARY",CLOSED,false)+suffix).lines().map(l->"       "+l+"\n").collect(java.util.stream.Collectors.joining()));
            var copies=directory.resolve("copies");Files.createDirectories(copies);var out=directory.resolve("out");
            ExplorerMain.main(new String[]{"--source",input.toString(),"--copybooks",copies.toString(),"--output",out.toString()});
            var json=new ObjectMapper().readTree(out.resolve("cobol-semantic-product.json").toFile());
            assertTrue(json.path("factDependencies").path("inputs").toString().contains("UNLOCATED_INPUT"));
            assertFalse(json.path("factDependencies").path("inputs").toString().contains("MISSING_COPY"),
                    "corruption must reject ownership itself, not just append UNLOCATED_INPUT");
            assertTrue(Files.readString(out.resolve("resolution-data.js")).contains("UNRESOLVED_COPY"));
        }
    }
}
