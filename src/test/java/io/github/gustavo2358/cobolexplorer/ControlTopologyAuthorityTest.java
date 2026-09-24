package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.ControlTopology.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ControlTopologyAuthorityTest {
    static String source(String body){return "IDENTIFICATION DIVISION.\nPROGRAM-ID. TOPOLOGY.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n01 PGM PIC X(8).\n01 FLAG PIC X.\n88 FLAG-ON VALUE 'Y'.\nPROCEDURE DIVISION.\n"+body;}
    static CobolSemanticPort publish(String body){return ScalarMoveCheckpoint4ATest.publish(source(body));}
    static Region paragraph(ControlTopology t,int line){return t.regions().stream().filter(r->r.kind()==RegionKind.PARAGRAPH)
        .filter(r->t.proofs().stream().anyMatch(p->r.proofs().contains(p.id())&&p.provenance().original().startLine()==line)).findFirst().orElseThrow();}
    static Target outcome(ControlTopology t,String statement){return t.outcomes().stream().filter(o->o.statement().equals(statement)).findFirst().orElseThrow().target();}
    @Test void M1_terminalVerbDoesNotChangeRegionCompletion(){
        for(var verb:List.of("EXIT","CONTINUE","SET FLAG-ON TO TRUE","DISPLAY 'X'")) {
            var t=publish("MAIN.\nPERFORM P.\nGOBACK.\nP.\n"+verb+".\nQ.\nGOBACK.\n").controlTopology().orElseThrow();
            var p=paragraph(t,12);var q=paragraph(t,14);assertEquals(1,p.members().size());
            var end=outcome(t,p.members().get(0));assertEquals(TargetKind.COMPLETE,end.kind());assertEquals(p.id(),end.reference());
            var boundary=t.boundaries().stream().filter(b->b.id().equals(p.boundary())).findFirst().orElseThrow();
            assertEquals(TargetKind.REGION_ENTRY,boundary.ordinaryDefault().kind());assertEquals(q.id(),boundary.ordinaryDefault().reference());
        }
    }
    @Test void M2_M3_M4_sharedSourceRegionHasOrdinaryDefaultAndDistinctContextBindings(){
        var t=publish("MAIN.\nPERFORM P.\nPERFORM P THRU Q.\nGOBACK.\nP.\nCONTINUE.\nQ.\nCONTINUE.\nR.\nGOBACK.\n").controlTopology().orElseThrow();
        assertEquals(2,t.bindings().size());var ranges=t.regions().stream().filter(r->r.kind()==RegionKind.RANGE).toList();
        assertEquals(Set.of(1,2),new HashSet<>(ranges.stream().map(r->r.regions().size()).toList()));
        var p=paragraph(t,13);assertTrue(ranges.stream().allMatch(r->r.regions().get(0).equals(p.id())));
        assertEquals(1,p.members().size());assertNotEquals(t.bindings().get(0).resume(),t.bindings().get(1).resume());
        assertNotEquals(t.bindings().get(0).endpoint(),t.bindings().get(1).endpoint());
    }
    @Test void M6_predicateGapPreservesEvaluateArmComposition(){
        for(var subject:List.of("FLAG","MISSING-VALUE")) {
            var t=publish("MAIN.\nPERFORM P.\nGOBACK.\nP.\nEVALUATE "+subject+"\nWHEN 'Y' CONTINUE\nWHEN OTHER CONTINUE\nEND-EVALUATE.\nQ.\nGOBACK.\n").controlTopology().orElseThrow();
            var arms=t.regions().stream().filter(r->r.kind()==RegionKind.EVALUATE_ARM).toList();assertEquals(2,arms.size());
            for(var arm:arms) {
                assertEquals(TargetKind.COMPLETE,outcome(t,arm.members().get(0)).kind());
                assertEquals(arm.id(),outcome(t,arm.members().get(0)).reference());
                var end=t.boundaries().stream().filter(b->b.id().equals(arm.boundary())).findFirst().orElseThrow();assertEquals(arm.parent(),end.ordinaryDefault().reference());
            }
        }
    }
    @Test void M9_permutedInventoryAndMalformedReference() throws Exception {
        var t=publish("MAIN.\nPERFORM P.\nGOBACK.\nP.\nCONTINUE.\n").controlTopology().orElseThrow();
        var occurrences=new ArrayList<>(t.occurrences());var regions=new ArrayList<>(t.regions());var outcomes=new ArrayList<>(t.outcomes());
        Collections.reverse(occurrences);Collections.reverse(regions);Collections.reverse(outcomes);
        var peer=new ControlTopology(t.authority(),occurrences,regions,t.boundaries(),outcomes,t.bindings(),t.proofs());
        assertArrayEquals(new ObjectMapper().writeValueAsBytes(t),new ObjectMapper().writeValueAsBytes(peer));
        occurrences.remove(0);assertThrows(IllegalArgumentException.class,()->new ControlTopology(t.authority(),occurrences,regions,t.boundaries(),outcomes,t.bindings(),t.proofs()));
    }
    @Test void M5_nestedInvocationCompletesInnerBeforeOuter() {
        var t=publish("MAIN.\nPERFORM P.\nGOBACK.\nP.\nPERFORM Q.\nQ.\nCONTINUE.\n").controlTopology().orElseThrow();
        var inner=t.bindings().stream().filter(b->b.resume().kind()==TargetKind.COMPLETE).findFirst().orElseThrow();
        var outer=t.bindings().stream().filter(b->!b.equals(inner)).findFirst().orElseThrow();
        assertEquals(outer.endpoint(),t.regions().stream().filter(r->r.id().equals(inner.resume().reference())).findFirst().orElseThrow().boundary());
        assertNotEquals(inner.endpoint(),outer.endpoint());
    }
    static CobolSemanticPort publishFile(FileMemoryEffectsTest.Fixture fixture) {
        var a=fixture.source();
        return ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
    }
    @Test void M7_M8_handlersShareInventoryAndCannotLoseTargets() {
        String select="SELECT F ASSIGN TO INDD ORGANIZATION IS INDEXED\n ACCESS IS DYNAMIC RECORD KEY REC FILE STATUS FS.";
        String fd="FD F.\n01 REC PIC X(8).";
        var plain=publishFile(FileMemoryEffectsTest.fixture(select,fd,"01 FS PIC XX.","MAIN.\nDELETE F RECORD.\nGOBACK." )).controlTopology().orElseThrow();
        var t=publishFile(FileMemoryEffectsTest.fixture(select,fd,"01 FS PIC XX.",
                "MAIN.\nDELETE F RECORD\n INVALID KEY CONTINUE\n NOT INVALID KEY CONTINUE\nEND-DELETE.\nGOBACK." )).controlTopology().orElseThrow();
        assertEquals(0,plain.regions().stream().filter(r->r.kind()==RegionKind.FILE_HANDLER).count());
        var handlers=t.regions().stream().filter(r->r.kind()==RegionKind.FILE_HANDLER).toList();assertEquals(2,handlers.size());
        var bad=t.outcomes().stream().filter(o->o.role().contains("/INVALID_KEY/")).findFirst().orElseThrow();
        var good=t.outcomes().stream().filter(o->o.role().contains("/SUCCESS/")).findFirst().orElseThrow();
        assertNotEquals(bad.target().reference(),good.target().reference());
        assertTrue(handlers.stream().anyMatch(r->r.id().equals(bad.target().reference())));
        assertTrue(handlers.stream().anyMatch(r->r.id().equals(good.target().reference())));
        for(var h:handlers) {
            assertEquals(TargetKind.OCCURRENCE,h.entry().kind());
            assertTrue(t.occurrences().stream().anyMatch(o->o.statement().equals(h.entry().reference())&&o.region().equals(h.id())));
            var removed=t.occurrences().stream().filter(o->!o.statement().equals(h.entry().reference())).toList();
            assertThrows(IllegalArgumentException.class,()->new ControlTopology(t.authority(),removed,t.regions(),t.boundaries(),t.outcomes(),t.bindings(),t.proofs()));
        }
    }
    @Test void M10_unrelatedMissingDataCopyPreservesControl() {
        var body="MAIN.\nPERFORM P THRU Q.\nGOBACK.\nP.\nIF FLAG = 'Y' CONTINUE ELSE CONTINUE END-IF.\nQ.\nCONTINUE.\n";
        var a=publish(body).controlTopology().orElseThrow();
        var b=ScalarMoveCheckpoint4ATest.publish(source(body).replace("PROCEDURE DIVISION.","COPY MISSING-DATA.\nPROCEDURE DIVISION.")).controlTopology().orElseThrow();
        assertEquals(a.occurrences().size(),b.occurrences().size());
        assertEquals(a.outcomes().stream().map(o->o.kind()+":"+o.role()+":"+o.target().kind()).toList(),b.outcomes().stream().map(o->o.kind()+":"+o.role()+":"+o.target().kind()).toList());
        assertEquals(a.regions().stream().map(Region::kind).sorted().toList(),b.regions().stream().map(Region::kind).sorted().toList());
        assertTrue(b.proofs().stream().anyMatch(p->p.kind()==ProofKind.INPUT_REGION_ISOLATION));
        assertTrue(b.outcomes().stream().noneMatch(o->o.kind()==OutcomeKind.UNKNOWN_LOCAL));
    }
    @Test void symbolicCyclesAreRejectedButSourceLoopsRemainRepresentable() {
        var t=publish("MAIN.\nGO TO MAIN.\n").controlTopology().orElseThrow();
        var p=t.regions().stream().filter(r->r.kind()==RegionKind.PARAGRAPH).findFirst().orElseThrow();
        var boundary=t.boundaries().stream().filter(b->b.region().equals(p.id())).findFirst().orElseThrow();
        var list=new ArrayList<>(t.boundaries());list.remove(boundary);
        list.add(new Boundary(boundary.id(),p.id(),new Target(TargetKind.COMPLETE,p.id(),boundary.proofs()),boundary.proofs()));
        assertThrows(IllegalArgumentException.class,()->new ControlTopology(t.authority(),t.occurrences(),t.regions(),list,t.outcomes(),t.bindings(),t.proofs()));
    }
    @Test void unavailableCountPreservesRangeWithoutInventingResume() {
        var t=publish("MAIN.\nPERFORM P 0 TIMES.\nCALL 'AFTER'.\nGOBACK.\nP.\nCONTINUE.\n").controlTopology().orElseThrow();
        var range=t.regions().stream().filter(r->r.kind()==RegionKind.RANGE).findFirst().orElseThrow();
        var unknown=t.outcomes().stream().filter(o->o.role().equals("invoke-unavailable")).findFirst().orElseThrow();
        assertEquals(TargetKind.UNKNOWN_LOCAL,unknown.target().kind());assertEquals(range.id(),unknown.target().reference());
        assertTrue(t.bindings().isEmpty());assertEquals(1,range.regions().size());assertEquals(4,t.occurrences().size());
        assertTrue(t.proofs().stream().anyMatch(p->p.kind()==ProofKind.PARTIAL_UNKNOWN&&p.rule().equals("nonpositive-literal-count-outside-qualified-profile")));
    }
    @Test void historicalTypedPortAndNewWireStaySeparate() throws Exception {
        var p=publish("MAIN.\nPERFORM P.\nGOBACK.\nP.\nCONTINUE.\nQ.\nDISPLAY 'DEAD'.\nGOBACK.\n");
        var legacy=CobolSemanticPort.open(new CobolSemanticProduct.State(p.unit(),p.policy(),p.dataDeclarations(),p.statements(),p.gaps(),p.coverage(),p.entryInventory(),p.storageIndependence(),p.storage(),p.fileInventory(),p.sourceDependencies(),p.ordinaryContinuations()));
        var mapper=new ObjectMapper();var old=mapper.readTree(SemanticProductJsonWriter.serialize(legacy));var current=mapper.readTree(SemanticProductJsonWriter.serialize(p));
        assertEquals("2.36.0",old.path("contractVersion").asText());assertFalse(old.has("controlTopology"));
        assertEquals("2.39.0",current.path("contractVersion").asText());assertEquals("FRONTEND_CONTROL_TOPOLOGY_R1",current.path("controlTopology").path("authority").asText());
        var dir=Path.of("target/control-topology-r1");Files.createDirectories(dir);Files.write(dir.resolve("legacy.json"),SemanticProductJsonWriter.serialize(legacy));Files.write(dir.resolve("current.json"),SemanticProductJsonWriter.serialize(p));
    }
}
