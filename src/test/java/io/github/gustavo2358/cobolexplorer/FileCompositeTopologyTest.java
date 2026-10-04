package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source oracle: normal execution must finish each operand before the next statement. */
class FileCompositeTopologyTest {
    static JsonNode publish(String procedure) throws Exception {
        var source=FileMemoryEffectsTest.fixture(
            "SELECT F ASSIGN TO CLIENTDD.\nSELECT G ASSIGN TO OTHERDD.\nSELECT H ASSIGN TO THIRDDD.",
            "FD F.\n01 R PIC X(8).\nFD G.\n01 S PIC X(8).\nFD H.\n01 T PIC X(8).", "", procedure);
        return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(ControlTopologyAuthorityTest.publishFile(source)));
    }
    @Test void multipleOperandsHaveExplicitInternalTargets() throws Exception {
        for(var procedure:List.of("OPEN INPUT F OUTPUT G.\nCLOSE F G.\nGOBACK.",
                "OPEN INPUT G F EXTEND H.\nCLOSE H F G.\nGOBACK.")) {
            var json=publish(procedure);var topology=json.path("controlTopology");var flows=topology.path("fileFlows");
            assertEquals(2,flows.size(),"OPEN and CLOSE each publish an internal flow");
            assertEquals("2.65.0",json.path("contractVersion").asText());
            for(var flow:flows) {
                var points=new HashMap<String,JsonNode>();for(var point:flow.path("points"))points.put(point.path("id").asText(),point);
                var target=flow.path("entry");int ordinal=0;
                while(target.path("kind").asText().equals("FILE_POINT")) {
                    var point=points.get(target.path("reference").asText());assertNotNull(point);assertEquals("USE",point.path("kind").asText());
                    assertEquals(ordinal++,point.path("ordinal").asInt());assertEquals(1,point.path("targets").size());
                    var normal=point.path("targets").get(0);int matches=0;
                    for(var outcome:topology.path("outcomes"))if(outcome.path("statement").equals(flow.path("statement"))
                            &&outcome.path("role").asText().equals("file/"+point.path("ordinal").asInt()+"/SUCCESS/0")) {
                        assertEquals(normal,outcome.path("target"));matches++;
                    }
                    assertEquals(1,matches);target=normal;assertTrue(ordinal<=points.size(),"no success loop");
                }
                assertEquals(points.size(),ordinal);assertEquals("COMPLETE",target.path("kind").asText());
            }
            var out=Path.of("target/file-composite-topology");Files.createDirectories(out);
            Files.writeString(out.resolve(flows.get(0).path("points").size()==2?"two.json":"three.json"),json.toPrettyString());
        }
    }
    @Test void singleFileRetainsItsExistingContract() throws Exception {
        assertTrue(publish("OPEN INPUT F.\nCLOSE F.\nGOBACK.").path("controlTopology").path("fileFlows").isMissingNode()
            ||publish("OPEN INPUT F.\nCLOSE F.\nGOBACK.").path("controlTopology").path("fileFlows").isEmpty());
    }
    @Test void sortPublishesPhasesAndKeepsCallbacksUnavailable() throws Exception {
        for(boolean callback:List.of(false,true)) {
            var source=FileMemoryEffectsTest.fixture(
                "SELECT A ASSIGN TO INA.\nSELECT B ASSIGN TO INB.\nSELECT C ASSIGN TO OUTC.\nSELECT S ASSIGN TO WORKDD.",
                "FD A.\n01 AR PIC X(8).\nFD B.\n01 BR PIC X(8).\nFD C.\n01 CR PIC X(8).\nSD S.\n01 SR.\n02 K PIC X(8).", "",
                "SORT S ON ASCENDING KEY K\n"+(callback?"INPUT PROCEDURE FILLER-P\n":"USING A B\n")+"GIVING C.\nGOBACK.\nFILLER-P.\nRELEASE SR.");
            var port=ControlTopologyAuthorityTest.publishFile(source);var topology=port.controlTopology().orElseThrow();
            if(callback) {
                assertTrue(topology.fileFlows().isEmpty());
                var statement="statement:"+port.fileInventory().sortPlans().get(0).statement().localId();
                assertTrue(topology.outcomes().stream().filter(o->o.statement().equals(statement))
                    .allMatch(o->o.kind()==io.github.gustavo2358.cobolexplorer.semanticproduct.ControlTopology.OutcomeKind.UNKNOWN_LOCAL));
            } else {
                var flow=topology.fileFlows().get(0);var points=new HashMap<String,io.github.gustavo2358.cobolexplorer.semanticproduct.ControlTopology.FilePoint>();
                flow.points().forEach(point->points.put(point.id(),point));
                var input=points.get(flow.entry().reference());assertEquals(-1,input.ordinal());assertEquals(3,input.targets().size());
                var work=points.values().stream().filter(point->point.ordinal()==0).findFirst().orElseThrow();
                assertTrue(input.targets().stream().anyMatch(t->t.reference().equals(work.id())));
                for(int ordinal:List.of(1,2)) {
                    var participant=points.values().stream().filter(point->point.ordinal()==ordinal).findFirst().orElseThrow();
                    assertEquals(input.id(),participant.targets().get(0).reference());
                }
                var output=points.get(work.targets().get(0).reference());assertEquals(-1,output.ordinal());assertEquals(2,output.targets().size());
                assertEquals(6,points.size());
                // All routing/proof identity remains stable when inventories are physically permuted.
                var flows=new ArrayList<>(topology.fileFlows());Collections.reverse(flows);
                var peer=new io.github.gustavo2358.cobolexplorer.semanticproduct.ControlTopology(topology.authority(),topology.occurrences(),topology.regions(),topology.boundaries(),topology.outcomes(),topology.bindings(),topology.proofs(),topology.exceptionalEvents(),flows);
                assertEquals(topology,peer);
            }
        }
    }

    @Test void compilationEnvelopeGatesTheNewInventory() throws Exception {
        for(boolean composite:List.of(false,true)) {
            var source=FileMemoryEffectsTest.fixture("SELECT A ASSIGN TO INA.\nSELECT B ASSIGN TO OUTB.",
                "FD A.\n01 RA PIC X.\nFD B.\n01 RB PIC X.","",composite?"OPEN INPUT A B.\nGOBACK.":"OPEN INPUT A.\nGOBACK.");
            var port=ControlTopologyAuthorityTest.publishFile(source);
            var unit=new io.github.gustavo2358.cobolexplorer.semanticproduct.CompilationSemanticProduct.UnitProduct(port,Optional.empty(),List.of(),List.of(),List.of(),List.of());
            var compilation=new io.github.gustavo2358.cobolexplorer.semanticproduct.CompilationSemanticProduct(
                io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.InventoryStatus.COMPLETE,List.of(port.unit()),List.of(unit));
            var json=new ObjectMapper().readTree(io.github.gustavo2358.cobolexplorer.semanticproduct.transport.CompilationSemanticProductJsonWriter.serialize(compilation));
            assertEquals(composite,json.path("units").get(0).path("product").path("controlTopology").has("fileFlows"));
        }
    }

    @Test void structuralFlowDoesNotRequireMemoryEventAdmission() throws Exception {
        var json=FileDeclarationContractTest.publish("SELECT A ASSIGN TO INA.\nSELECT B ASSIGN TO OUTB.",
            "FD A.\n01 RA PIC X.\nFD B.\n01 RB PIC X.","","OPEN INPUT A OUTPUT B.\nCLOSE A B.\nGOBACK.");
        assertEquals(2,json.path("controlTopology").path("fileFlows").size());
        assertEquals("2.65.0",json.path("contractVersion").asText());
        assertEquals(4,json.path("fileInventory").path("operations").path("uses").size());
        assertTrue(json.path("factDependencies").path("facts").isEmpty());
        assertTrue(json.path("factDependencies").path("bindings").isEmpty());
        var out=Path.of("target/file-composite-topology");Files.createDirectories(out);Files.writeString(out.resolve("no-event-model.json"),json.toPrettyString());
    }

}
