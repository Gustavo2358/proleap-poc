package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static org.junit.jupiter.api.Assertions.*;

class LogicalInitialInvariantTest {
    static JsonNode publish(String data,String code)throws Exception {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data,code),"logical-invariant.cbl");
        return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED)));
    }
    static JsonNode constant(JsonNode j) {
        for(var c:j.path("storage").path("entryState").path("conditions"))
            if(c.path("logicalText").asText().equals("PROGA001"))return c;
        throw new AssertionError("constant source VALUE missing");
    }
    @Test void closedConstantDoesNotRequireAByteCodecOrFirstEntryAssumption()throws Exception {
        var j=publish("01 REC-A.\n 05 NAME-A PIC X(8) VALUE 'PROGA001'.\n 05 OTHER-A PIC X(8).", "MOVE 'OTHER001' TO OTHER-A.\nCALL NAME-A.\nGOBACK.");
        var c=constant(j);assertEquals("LOGICAL_TEXT",c.path("kind").asText());assertEquals("DECLARATIVE_INVARIANT",c.path("proof").asText());
        assertEquals("UNKNOWN",j.path("storage").path("entryState").path("mode").asText());
        assertEquals("2.65.0",j.path("contractVersion").asText());
        var out=java.nio.file.Path.of("target/recall-cics");java.nio.file.Files.createDirectories(out);
        java.nio.file.Files.writeString(out.resolve("logical-invariant.json"),j.toPrettyString());
    }
    @Test void taskReturnDoesNotMutateAnUnexposedSibling()throws Exception {
        var data="01 NAME-A PIC X(8) VALUE 'PROGA001'.\n01 AREA-A PIC X(8).";
        var j=publish(data,"EXEC CICS RETURN TRANSID('ABCD')\n COMMAREA(AREA-A) LENGTH(8) END-EXEC.");
        assertEquals("LOGICAL_TEXT",constant(j).path("kind").asText());
    }
    @Test void implicitLengthRegisterDoesNotAliasTheMeasuredItem()throws Exception {
        var data="01 NAME-A PIC X(8) VALUE 'PROGA001'.\n01 AREA-A PIC X(16).\n01 KEY-A PIC X(8).\n01 RESP-A PIC S9(8) COMP.";
        var j=publish(data,"EXEC CICS READ FILE('FILEA') RIDFLD(KEY-A)\n INTO(AREA-A) LENGTH(LENGTH OF NAME-A) RESP(RESP-A)\n END-EXEC.\nGOBACK.");
        assertEquals("LOGICAL_TEXT",constant(j).path("kind").asText());
        j=publish(data,"EXEC CICS READ FILE('FILEA') RIDFLD(KEY-A)\n INTO(NAME-A) LENGTH(LENGTH OF NAME-A) RESP(RESP-A)\n END-EXEC.\nGOBACK.");
        assertEquals("POSSIBLE_LOGICAL_TEXT",constant(j).path("kind").asText());
    }
    @Test void writesAliasesEscapesAndMissingCodeRetainLifecycleUncertainty()throws Exception {
        var data="01 REC-A.\n 05 NAME-A PIC X(8) VALUE 'PROGA001'.\n 05 OTHER-A PIC X(8).";
        for(var code:java.util.List.of("MOVE SPACES TO NAME-A.","MOVE SPACES TO REC-A.","CALL 'FOREIGN' USING REC-A.","MOVE FUNCTION CUSTOM-FUNC(NAME-A) TO OTHER-A.","COPY MISSING-CODE.","EXEC CICS UNKNOWN END-EXEC.")) {
            var c=constant(publish(data,code+"\nGOBACK."));assertEquals("POSSIBLE_LOGICAL_TEXT",c.path("kind").asText(),code);
        }
        var c=constant(publish(data+"\n01 ALT-A REDEFINES REC-A PIC X(16).","MOVE SPACES TO ALT-A.\nGOBACK."));
        assertEquals("POSSIBLE_LOGICAL_TEXT",c.path("kind").asText());
    }

    private static State state(CobolSemanticPort port,StorageInventory storage,Optional<FactDependencies> graph) {
        return new State(port.unit(),port.policy(),port.dataDeclarations(),port.statements(),port.gaps(),
            port.coverage(),port.entryInventory(),port.storageIndependence(),storage,port.fileInventory(),
            port.sourceDependencies(),port.ordinaryContinuations(),port.controlTopology(),graph,port.nominalValues());
    }
    @Test void sharedProofEvaluationStillRejectsOneUnprovedCellAmongValidConstants() {
        var data=new StringBuilder();
        for(int i=0;i<40;i++)data.append("01 CONST-").append(i).append(" PIC X(8) VALUE 'PROGA001'.\n");
        data.append("01 BAD-CELL PIC X(8) VALUE 'PROGB001'.\n01 ALIAS-CELL REDEFINES BAD-CELL PIC X(4).");
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data.toString(),"GOBACK."),"proof-cells.cbl");
        var port=EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        var storage=port.storage();var conditions=storage.entryState().conditions();
        assertEquals(40,conditions.stream().filter(c->c.kind()==InitialStorageKind.LOGICAL_TEXT).count());
        var bad=conditions.stream().filter(c->c.logicalText().equals(Optional.of("PROGB001"))).findFirst().orElseThrow();
        assertEquals(InitialStorageKind.POSSIBLE_LOGICAL_TEXT,bad.kind());
        assertDoesNotThrow(()->state(port,storage,port.factDependencies()));
        var forged=new StorageInitialCondition(bad.node(),InitialStorageKind.LOGICAL_TEXT,List.of(),List.of(),
            bad.provenance(),InitialStorageProof.DECLARATIVE_INVARIANT,bad.logicalText());
        for(boolean first:List.of(false,true)) {
            var changed=new ArrayList<>(conditions);changed.remove(bad);changed.add(first?0:changed.size(),forged);
            var invalid=new StorageInventory(storage.profile(),storage.nodes(),storage.bases(),storage.views(),
                storage.gapCodes(),storage.relations(),storage.renames(),new StorageEntryState(storage.entryState().mode(),changed),
                storage.logicalTextViews(),storage.logicalExactViews());
            var error=assertThrows(IllegalArgumentException.class,()->state(port,invalid,port.factDependencies()));
            assertEquals("logical invariant has a closed local cell",error.getMessage());
        }
        var absent=assertThrows(IllegalArgumentException.class,()->state(port,storage,Optional.empty()));
        assertEquals("logical invariant requires local-cell proof",absent.getMessage());
    }
}
