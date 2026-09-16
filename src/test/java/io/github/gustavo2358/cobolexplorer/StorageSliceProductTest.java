package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageProductTest.state;

class StorageSliceProductTest {
    @Test void writeAndCallCarryOccurrenceSliceWithoutChangingTheDeclaredView() throws Exception {
        var s=state(StorageReferenceModificationTest.DATA,"MOVE 'ABC' TO TEXT-PART(2:3).\nCALL TEXT-ALIAS(2:3).");
        var bytes=SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s));var doc=new ObjectMapper().readTree(bytes);
        assertEquals("2.20.0",doc.path("contractVersion").asText());
        var write=doc.path("statements").get(0).path("target").path("regionalAccess");
        var call=doc.path("statements").get(1).path("target").path("reference").path("regionalAccess");
        for(var access:java.util.List.of(write,call)) {
            assertEquals("3",access.path("slice").path("offset").asText());assertEquals("3",access.path("slice").path("extent").asText());
            var view=java.util.stream.StreamSupport.stream(doc.path("storage").path("views").spliterator(),false).filter(v->v.path("node").equals(access.path("view"))).findFirst().orElseThrow();
            assertEquals("2",view.path("offset").path("value").asText());assertEquals("6",view.path("extent").path("value").asText());
        }
        if(System.getProperty("storage.fixture.output")!=null)java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("storage.fixture.output")),bytes);
    }
    @Test void dynamicPositionRetainsFallbackAndNeverPublishesWholeAccess() throws Exception {
        var s=state(StorageReferenceModificationTest.DATA+"\n01 POSITION-VAR PIC 9.","MOVE 'A' TO TEXT-PART(POSITION-VAR:1).");
        var doc=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s)));
        assertFalse(doc.path("gaps").isEmpty());
        for(var statement:doc.path("statements"))if(statement.path("variant").asText().equals("MOVE")) {
            assertTrue(statement.path("target").path("regionalAccess").isNull());
            assertTrue(statement.path("target").path("wholeItemAccess").isNull());
        }
    }
}
