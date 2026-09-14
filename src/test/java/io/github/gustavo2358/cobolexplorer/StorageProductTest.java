package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static org.junit.jupiter.api.Assertions.*;

class StorageProductTest {
    static State state(String data, String code, StorageLayoutSemantics.Profile profile) {
        var a=StorageAccessTest.fixture(data,code,profile);
        var f=a.source();
        return CobolSemanticProductProjector.project(new CobolSemanticProductProjector.FrontendProducts(
            f.build(),f.tables(),f.occurrences(),f.resolution(),f.report(),
            ScalarMoveSemantics.analyze(f.build(),f.tables(),f.resolution(),f.report()),Optional.of(a.effects())),f.model().programUnits().get(0).id());
    }
    static State state(String data,String code) { return state(data,code,StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047); }
    static State group() { return state("01 WS-AREA.\n05 PREFIX-PART PIC X(2).\n05 FILLER PIC X(2).\n05 WS-PGM PIC X(4).", "MOVE 'ABCDEFGH' TO WS-AREA.\nCALL WS-PGM."); }
    static StorageMeasure known(long n) { return new StorageMeasure(Optional.of(BigInteger.valueOf(n)),List.of()); }
    static State withStorage(State s,StorageInventory storage) { return new State(s.unit(),s.policy(),s.dataDeclarations(),s.statements(),s.gaps(),s.coverage(),s.entryInventory(),s.storageIndependence(),storage); }
    @Test void groupFillerAndCallCrossTheClosedPortWithSeparatePhysicalIds() {
        var s=group();var p=CobolSemanticPort.open(s);var storage=p.storage();
        assertEquals(StorageProfile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,storage.profile());
        assertEquals(4,storage.nodes().size());assertEquals(1,storage.bases().size());assertEquals(4,storage.views().size());
        var filler=storage.nodes().stream().filter(PhysicalNode::filler).findFirst().orElseThrow();assertTrue(filler.data().isEmpty());
        assertEquals(known(8),storage.bases().get(0).extent());
        var move=p.moves().get(0);assertEquals(RegionalMoveKind.LITERAL_BYTES,move.regionalMove().orElseThrow().kind());
        assertEquals(List.of(193,194,195,196,197,198,199,200),move.regionalMove().orElseThrow().bytes());
        assertTrue(move.target().wholeItemAccess().isEmpty());assertTrue(move.target().regionalAccess().isPresent());
        var target=(DataReference)p.calls().get(0).target();assertTrue(target.wholeItemAccess().isEmpty());
        var view=storage.views().stream().filter(v->v.node().equals(target.regionalAccess().orElseThrow().view())).findFirst().orElseThrow();
        assertEquals(known(4),view.offset());assertEquals(known(4),view.extent());
        assertEquals(Optional.of("text.ebcdic.ibm1047@1"),view.codec());
        assertTrue(p.gaps().stream().anyMatch(g->g.scope()==GapScope.RUNTIME_CALL_TARGET));
    }
    @Test void sourceCopyAndLiteralTruncationAreExplicitAndPlural() {
        for(int count:List.of(1,2,5,40)) {
            var code=new StringBuilder();for(int i=0;i<count;i++)code.append("MOVE SOURCE-PART TO TARGET-PART.\n");
            code.append("MOVE 'TOO-LONG' TO TARGET-PART.");
            var p=CobolSemanticPort.open(state("01 SOURCE-PART PIC X(4).\n01 TARGET-PART PIC X(4).",code.toString()));
            assertEquals(count+1,p.moves().size());
            for(int i=0;i<count;i++) { var m=p.moves().get(i);assertEquals(RegionalMoveKind.COPY_BYTES,m.regionalMove().orElseThrow().kind());assertTrue(((DataReference)m.source()).regionalAccess().isPresent()); }
            assertEquals(RegionalMoveKind.FITTED_LITERAL_BYTES,p.moves().get(count).regionalMove().orElseThrow().kind());
            assertEquals(List.of(227,214,214,96),p.moves().get(count).regionalMove().orElseThrow().bytes());
        }
    }
    @Test void absentProfileAndUnknownExtentNeverBecomeZeroOrExactAccess() {
        var s=state("01 WS-AREA.\n05 PREFIX-PART PIC 9.\n05 WS-PGM PIC X(4).","CALL WS-PGM.");
        var p=CobolSemanticPort.open(s);assertFalse(p.storage().nodes().isEmpty());
        assertTrue(p.storage().bases().get(0).extent().value().isEmpty());
        assertTrue(((DataReference)p.calls().get(0).target()).regionalAccess().isEmpty());
        var absent=state("01 WS-PGM PIC X(4).","MOVE 'ABCD' TO WS-PGM.",StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertEquals(StorageProfile.UNSPECIFIED,absent.storage().profile());assertFalse(absent.storage().gapCodes().isEmpty());
        assertTrue(CobolSemanticPort.open(absent).moves().get(0).target().regionalAccess().isEmpty());
    }
    @Test void json28HasDeterministicClosedStorageTransport() throws Exception {
        var p=CobolSemanticPort.open(group());var bytes=SemanticProductJsonWriter.serialize(p);
        assertArrayEquals(bytes,SemanticProductJsonWriter.serialize(CobolSemanticPort.open(group())));
        var doc=new ObjectMapper().readTree(bytes);assertEquals("2.11.0",doc.path("contractVersion").asText());
        assertEquals("ibm-enterprise-6.4-fixed-display-1047@1",doc.path("storage").path("profileId").asText());
        assertEquals("8",doc.path("storage").path("bases").get(0).path("extent").path("value").asText());
        assertTrue(doc.path("storage").path("bases").get(0).path("extent").path("value").isTextual());
        assertEquals("LITERAL_BYTES",doc.path("statements").get(0).path("regionalMove").path("kind").asText());
        assertTrue(doc.path("statements").get(0).path("target").path("regionalAccess").path("view").asText().startsWith("storage-node:"));
    }
    @Test void knownStorageWithoutAnExplicitEnvironmentIsRejectedInMemory() {
        var s=state("01 WS-PGM PIC X(4).","CALL WS-PGM.",StorageLayoutSemantics.Profile.UNSPECIFIED);
        var origin=s.dataDeclarations().get(0).provenance();
        var base=new StorageBase(new StorageBaseId(s.unit(),0),known(4),AllocationProof.INDEPENDENT_LOCAL_WORKING_STORAGE,origin);
        assertThrows(IllegalArgumentException.class,()->withStorage(s,new StorageInventory(StorageProfile.UNSPECIFIED,List.of(),List.of(base),List.of(),List.of("PROFILE_NOT_SELECTED"))));
    }
    static StorageInventory inventory(State s,List<PhysicalNode> nodes,List<StorageBase> bases,List<StorageView> views) {
        return new StorageInventory(s.storage().profile(),nodes,bases,views,s.storage().gapCodes());
    }
    @Test void danglingDuplicateAndCyclicPhysicalInventoriesAreRejected() {
        var s=group();var storage=s.storage();
        assertThrows(IllegalArgumentException.class,()->withStorage(s,inventory(s,storage.nodes(),List.of(),storage.views())));
        var duplicate=new ArrayList<>(storage.nodes());duplicate.add(storage.nodes().get(0));
        assertThrows(IllegalArgumentException.class,()->withStorage(s,inventory(s,duplicate,storage.bases(),storage.views())));
        var nodes=new ArrayList<>(storage.nodes());var root=nodes.get(0);var child=nodes.get(1);
        nodes.set(0,new PhysicalNode(root.id(),Optional.of(child.id()),root.order(),root.filler(),root.kind(),root.data(),root.extent(),root.provenance()));
        assertThrows(IllegalArgumentException.class,()->withStorage(s,inventory(s,nodes,storage.bases(),storage.views())));
        var views=new ArrayList<>(storage.views());views.remove(1);
        assertThrows(IllegalArgumentException.class,()->withStorage(s,inventory(s,storage.nodes(),storage.bases(),views)));
    }
    @Test void outOfBoundsAndWrongNominalViewCannotAuthorizeAnAccess() {
        var s=group();var storage=s.storage();var views=new ArrayList<>(storage.views());var v=views.get(3);
        views.set(3,new StorageView(v.node(),v.base(),known(5),v.extent(),v.codec(),v.provenance()));
        assertThrows(IllegalArgumentException.class,()->withStorage(s,inventory(s,storage.nodes(),storage.bases(),views)));
        var move=(MoveFact)s.statements().get(0);var target=move.target();
        var changed=new DataReference(target.id(),target.role(),target.binding(),target.provenance(),target.wholeItemAccess(),Optional.of(new RegionalAccess(v.node())));
        var facts=new ArrayList<>(s.statements());facts.set(0,new MoveFact(move.header(),move.source(),changed,move.copySemantics(),move.normalContinuation(),move.textAdjustment(),move.regionalMove()));
        assertThrows(IllegalArgumentException.class,()->new State(s.unit(),s.policy(),s.dataDeclarations(),facts,s.gaps(),s.coverage(),s.entryInventory(),s.storageIndependence(),storage));
    }
    @Test void malformedLiteralAndUnprovedOrOverlappingCopyAreRejected() {
        var s=group();var move=(MoveFact)s.statements().get(0);var facts=new ArrayList<>(s.statements());
        facts.set(0,new MoveFact(move.header(),move.source(),move.target(),move.copySemantics(),move.normalContinuation(),move.textAdjustment(),
            Optional.of(new RegionalMove(RegionalMoveKind.LITERAL_BYTES,List.of(193),List.of()))));
        assertThrows(IllegalArgumentException.class,()->new State(s.unit(),s.policy(),s.dataDeclarations(),facts,s.gaps(),s.coverage(),s.entryInventory(),s.storageIndependence(),s.storage()));
        var copy=state("01 SOURCE-PART PIC X(4).\n01 TARGET-PART PIC X(4).","MOVE SOURCE-PART TO TARGET-PART.");
        var bases=copy.storage().bases().stream().map(b->new StorageBase(b.id(),b.extent(),AllocationProof.UNPROVEN,b.provenance())).toList();
        assertThrows(IllegalArgumentException.class,()->withStorage(copy,inventory(copy,copy.storage().nodes(),bases,copy.storage().views())));
        var sibling=state("01 WS-AREA.\n05 SOURCE-PART PIC X(4).\n05 TARGET-PART PIC X(4).","MOVE SOURCE-PART TO TARGET-PART.");
        var views=new ArrayList<>(sibling.storage().views());var target=views.get(2);
        views.set(2,new StorageView(target.node(),target.base(),known(0),target.extent(),target.codec(),target.provenance()));
        assertThrows(IllegalArgumentException.class,()->withStorage(sibling,inventory(sibling,sibling.storage().nodes(),sibling.storage().bases(),views)));
    }
    @Test void inventoryOrderDoesNotRecomputePhysicalOffsets() {
        var s=group();var nodes=new ArrayList<>(s.storage().nodes());var views=new ArrayList<>(s.storage().views());
        Collections.reverse(nodes);Collections.rotate(views,2);
        var permuted=withStorage(s,inventory(s,nodes,s.storage().bases(),views));
        assertEquals(new HashSet<>(s.storage().views()),new HashSet<>(permuted.storage().views()));
        assertEquals(s.statements(),permuted.statements());
    }
    @Test void compositionRootRequiresExactProfileAndSnapshotOwnership() {
        assertEquals(StorageLayoutSemantics.Profile.UNSPECIFIED,ExplorerMain.storageProfile("unspecified"));
        assertEquals(StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,ExplorerMain.storageProfile(StorageLayoutSemantics.PROFILE_ID));
        for(var value:List.of("IBM1047","ascii","ibm-enterprise-6.4-fixed-display-1047@2"))
            assertThrows(IllegalArgumentException.class,()->ExplorerMain.storageProfile(value));
        var a=StorageAccessTest.fixture("01 WS-PGM PIC X(4).","CALL WS-PGM.");
        var b=StorageAccessTest.fixture("01 WS-PGM PIC X(8).","CALL WS-PGM.");var f=a.source();
        assertThrows(IllegalArgumentException.class,()->new CobolSemanticProductProjector.FrontendProducts(f.build(),f.tables(),f.occurrences(),f.resolution(),f.report(),
            ScalarMoveSemantics.analyze(f.build(),f.tables(),f.resolution(),f.report()),Optional.of(b.effects())));
        var p=ExplorerMain.publishSemanticProduct(f.model().programUnits().get(0).id(),f.build(),f.tables(),f.occurrences(),f.resolution(),f.report(),
            StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        assertTrue(((DataReference)p.calls().get(0).target()).regionalAccess().isPresent());
    }
    @Test void baseIdentityIsOpaqueAndUnusedBasesCannotEnterTheInventory() {
        var s=group();var base=s.storage().bases().get(0);var renamed=new StorageBaseId(s.unit(),999);
        var b=new StorageBase(renamed,base.extent(),base.allocation(),base.provenance());
        var views=s.storage().views().stream().map(v->new StorageView(v.node(),renamed,v.offset(),v.extent(),v.codec(),v.provenance())).toList();
        assertEquals(s.statements(),withStorage(s,inventory(s,s.storage().nodes(),List.of(b),views)).statements());
        var unused=new StorageBase(new StorageBaseId(s.unit(),s.storage().nodes().get(1).id().localId()),known(2),base.allocation(),base.provenance());
        assertThrows(IllegalArgumentException.class,()->withStorage(s,inventory(s,s.storage().nodes(),List.of(base,unused),s.storage().views())));
    }
}
