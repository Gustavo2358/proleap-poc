package io.github.gustavo2358.cobolexplorer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageProductTest.state;
class StorageMoveSequenceProductTest {
    @Test void publishesOrderedTransfersAndGenericFits() throws Exception {
        var s=state("01 SRC PIC X(2).\n01 DST-A.\n05 A PIC X(4).\n01 DST-B PIC X(1).","MOVE 'AB' TO SRC.\nMOVE SRC TO DST-A DST-B.\nCALL A.");
        var bytes=SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s));var doc=new ObjectMapper().readTree(bytes);
        assertEquals("2.13.0",doc.path("contractVersion").asText());var move=doc.path("statements").get(1);
        assertEquals("FIT_TEXT",move.path("regionalMove").path("kind").asText());
        assertEquals(1,move.path("additionalTransfers").size());
        assertEquals("FIT_TEXT",move.path("additionalTransfers").get(0).path("effect").path("kind").asText());
        if(System.getProperty("storage.fixture.output")!=null)java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("storage.fixture.output")),bytes);
    }
    @Test void literalAdjustmentsAndOverlappingSequenceKeepExplicitEffects() throws Exception {
        for(String name:java.util.List.of("move-literal-fit","move-overlap")) {
            var s=name.equals("move-literal-fit")?state("01 WS-AREA.\n05 A PIC X(4).\n05 B PIC X(1).","MOVE 'AB' TO A B.\nCALL A."):
                state("01 SRC PIC X(2).\n01 ALIAS-SRC REDEFINES SRC PIC X(2).\n01 OUT-TEXT PIC X(4).","MOVE SRC TO ALIAS-SRC OUT-TEXT.\nCALL OUT-TEXT.");
            var bytes=SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s));var doc=new ObjectMapper().readTree(bytes);var first=doc.path("statements").get(0);
            assertEquals(name.equals("move-literal-fit")?"FITTED_LITERAL_BYTES":"MUST_UNKNOWN",first.path("regionalMove").path("kind").asText());
            assertEquals(1,first.path("additionalTransfers").size());
            if(System.getProperty("storage.fixture.output")!=null)java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("storage.fixture.output")).resolveSibling(name+".sp.json"),bytes);
        }
    }

}
