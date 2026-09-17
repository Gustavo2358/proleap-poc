package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageProductTest.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

class StorageOverlayProductTest {
    @Test void relationIdentityOriginsAndFillerCrossTheVersionedBoundary() throws Exception {
        var s=state("01 RAW-AREA PIC X(4).\n01 VIEW-AREA REDEFINES RAW-AREA PIC X(8).\n01 FILLER REDEFINES VIEW-AREA PIC X(12).","MOVE 'ABCDEFGH' TO VIEW-AREA.\nCALL RAW-AREA.");
        var bytes=SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s));var mapper=new ObjectMapper();var doc=mapper.readTree(bytes);
        assertEquals("2.24.0",doc.path("contractVersion").asText());var storage=doc.path("storage");assertEquals("1.8.0",storage.path("version").asText());
        var relations=storage.path("relations");assertEquals(2,relations.size());assertEquals(1,storage.path("bases").size());
        var nodes=new HashMap<String,com.fasterxml.jackson.databind.JsonNode>();storage.path("nodes").forEach(n->nodes.put(n.path("id").asText(),n));
        var ids=new HashSet<String>();
        for(var relation:relations) {
            assertTrue(relation.path("id").asText().startsWith("storage-relation:"));assertTrue(ids.add(relation.path("id").asText()));
            assertEquals("PROVEN",relation.path("status").asText());assertEquals(0,relation.path("gapCodes").size());
            var owner=nodes.get(relation.path("owner").asText());var target=nodes.get(relation.path("target").asText());
            assertNotNull(owner);assertNotNull(target);assertTrue(owner.path("order").asInt()>target.path("order").asInt());
            assertTrue(relation.path("provenance").isObject());assertEquals("access.cbl",relation.path("provenance").path("original").path("file").asText());
        }
        assertTrue(nodes.get(relations.get(1).path("owner").asText()).path("filler").asBoolean());
        assertEquals(relations.get(0).path("owner"),relations.get(1).path("target"));
        assertEquals(doc,mapper.readTree(mapper.writeValueAsBytes(doc)));
        assertArrayEquals(bytes,SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s)));
    }
    @Test void unresolvedRelationIsRetainedWithUnknownTargetAndReason() throws Exception {
        var s=state("01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES MISSING-AREA PIC X(8).","CALL RAW-AREA.");
        var doc=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s)));
        var relations=doc.path("storage").path("relations");assertEquals(1,relations.size());var relation=relations.get(0);
        assertEquals("UNPROVEN",relation.path("status").asText());assertTrue(relation.path("target").isNull());
        assertEquals("OVERLAY_NOT_PROVEN",relation.path("gapCodes").get(0).asText());
        assertTrue(doc.path("statements").get(0).path("target").path("reference").path("regionalAccess").isNull());
    }
    private static State relations(State s,List<StorageRelation> relations) {
        var st=s.storage();return withStorage(s,new StorageInventory(st.profile(),st.nodes(),st.bases(),st.views(),st.gapCodes(),relations));
    }
    @Test void relationClosureRejectsDanglingSelfForwardAndUnrelatedTargets() {
        var s=state("01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES RAW-AREA PIC X(4).\n01 SAFE-AREA PIC X(8).","GOBACK.");
        var r=s.storage().relations().get(0);var missing=new StorageNodeId(s.unit(),999);
        assertThrows(IllegalArgumentException.class,()->relations(s,List.of(r,r)));
        assertThrows(IllegalArgumentException.class,()->relations(s,List.of(new StorageRelation(r.id(),missing,r.target(),r.status(),r.provenance(),r.gapCodes()))));
        for(var target:List.of(missing,r.owner(),s.storage().nodes().get(2).id()))
            assertThrows(IllegalArgumentException.class,()->relations(s,List.of(new StorageRelation(r.id(),r.owner(),Optional.of(target),r.status(),r.provenance(),r.gapCodes()))));
        assertThrows(IllegalArgumentException.class,()->new StorageRelation(r.id(),r.owner(),Optional.empty(),StorageRelationStatus.PROVEN,r.provenance(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new StorageRelation(r.id(),r.owner(),r.target(),StorageRelationStatus.UNPROVEN,r.provenance(),List.of("UNKNOWN")));
    }
    @Test void relationCannotAssertSharedStorageAcrossDifferentBasesOrOffsets() {
        var s=state("01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES RAW-AREA PIC X(4).\n01 SAFE-AREA PIC X(8).","GOBACK.");
        var st=s.storage();var r=st.relations().get(0);var views=new ArrayList<>(st.views());
        int index=0;while(!views.get(index).node().equals(r.owner()))index++;
        var v=views.get(index);var safe=st.views().get(2);
        views.set(index,new StorageView(v.node(),safe.base(),v.offset(),v.extent(),v.codec(),v.provenance()));
        assertThrows(IllegalArgumentException.class,()->withStorage(s,new StorageInventory(st.profile(),st.nodes(),st.bases(),views,st.gapCodes(),st.relations())));
        views.set(index,new StorageView(v.node(),v.base(),known(1),v.extent(),v.codec(),v.provenance()));
        assertThrows(IllegalArgumentException.class,()->withStorage(s,new StorageInventory(st.profile(),st.nodes(),st.bases(),views,st.gapCodes(),st.relations())));
    }
    @Test void relationInventoryPermutationIsNotAChangeInSemanticsOrWire() throws Exception {
        var s=state("01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES RAW-AREA PIC X(4).\n01 LAST-AREA REDEFINES VIEW-AREA PIC X(6).","GOBACK.");
        var reversed=new ArrayList<>(s.storage().relations());Collections.reverse(reversed);
        assertArrayEquals(SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s)),SemanticProductJsonWriter.serialize(CobolSemanticPort.open(relations(s,reversed))));
        var absent=state("01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES RAW-AREA PIC X(4).","GOBACK.",StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertEquals(StorageRelationStatus.PROVEN,absent.storage().relations().get(0).status());
        assertTrue(absent.storage().bases().get(0).extent().value().isEmpty());assertEquals(AllocationProof.UNPROVEN,absent.storage().bases().get(0).allocation());
    }
    @Test void unknownRelationCannotCoexistWithInventedAllocationIndependence() {
        var s=state("01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES MISSING-AREA PIC X(8).","GOBACK.");var st=s.storage();
        var bases=st.bases().stream().map(b->new StorageBase(b.id(),b.extent(),AllocationProof.INDEPENDENT_LOCAL_WORKING_STORAGE,b.provenance())).toList();
        assertThrows(IllegalArgumentException.class,()->withStorage(s,new StorageInventory(st.profile(),st.nodes(),bases,st.views(),st.gapCodes(),st.relations())));
        var declarations=new ArrayList<>(s.dataDeclarations());var d=declarations.get(0);
        declarations.set(0,new DataDeclaration(d.id(),d.canonicalName(),d.picture(),d.provenance(),d.coverage(),d.readiness(),
            Optional.of(new ScalarText(8)),Optional.empty()));
        assertThrows(IllegalArgumentException.class,()->new State(s.unit(),s.policy(),declarations,s.statements(),s.gaps(),s.coverage(),s.entryInventory(),s.storageIndependence(),st));
    }
}
