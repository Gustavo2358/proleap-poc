package io.github.gustavo2358.cobolexplorer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageProductTest.state;
class StorageCorrespondenceProductTest {
    @Test void publishesImplicitPairsWithPhysicalIdentityAndDeclarationOrigins() throws Exception {
        var s=state("01 SRC.\n05 A PIC X(2).\n05 FILLER PIC X.\n05 B PIC X(2).\n05 ONLY-S PIC X.\n01 DST.\n05 B PIC X.\n05 ONLY-D PIC X(3).\n05 A PIC X(4).","MOVE 'ABXCDZ' TO SRC.\nMOVE 'YYYYYYYY' TO DST.\nMOVE CORR SRC TO DST.\nCALL A OF DST.");
        var bytes=SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s));var doc=new ObjectMapper().readTree(bytes);var move=doc.path("statements").get(2);
        assertEquals("MOVE",move.path("variant").asText());assertEquals("FIT_TEXT",move.path("regionalMove").path("kind").asText());
        assertEquals(1,move.path("additionalTransfers").size());
        var extra=move.path("additionalTransfers").get(0);assertEquals("FIT_TEXT",extra.path("effect").path("kind").asText());
        assertNotEquals(move.path("source").path("reference").path("binding").path("selected"),extra.path("source").path("reference").path("binding").path("selected"));
        assertNotEquals(move.path("target").path("regionalAccess"),extra.path("target").path("regionalAccess"));
        if(System.getProperty("storage.fixture.output")!=null)java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("storage.fixture.output")),bytes);
    }
}
