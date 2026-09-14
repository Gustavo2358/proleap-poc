package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageProductTest.state;

class StorageRenamesProductTest {
    static final String DATA="01 RECORD-AREA.\n05 FIRST-PART PIC X(2).\n05 FILLER PIC X.\n05 LAST-PART PIC X(3).\n66 RANGE-ALIAS RENAMES FIRST-PART THRU LAST-PART.\n66 TAIL-ALIAS RENAMES LAST-PART.";
    @Test void unusedAliasesEndpointsAndOriginsArePublishedWithoutNewAllocation() throws Exception {
        var s=state(DATA,"GOBACK.");
        var doc=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s)));
        assertEquals("2.12.0",doc.path("contractVersion").asText());var st=doc.path("storage");
        assertEquals("1.2.0",st.path("version").asText());assertEquals(1,st.path("bases").size());
        assertEquals(2,st.path("renames").size());assertEquals(5,doc.path("dataDeclarations").size());
        var range=st.path("renames").get(0);assertEquals("PROVEN",range.path("status").asText());
        assertFalse(range.path("from").isNull());assertFalse(range.path("through").isNull());
        assertTrue(st.path("renames").get(1).path("through").isNull());
        assertEquals("access.cbl",range.path("provenance").path("original").path("file").asText());
        if(System.getProperty("storage.fixture.output")!=null)
            java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("storage.fixture.output")),
                SemanticProductJsonWriter.serialize(CobolSemanticPort.open(state(DATA,"MOVE 'ABCDEF' TO RANGE-ALIAS.\nCALL TAIL-ALIAS."))));
    }
    @Test void invalidRangeRemainsInInventoryWithGap() throws Exception {
        var s=state(DATA.replace("FIRST-PART THRU LAST-PART","LAST-PART THRU FIRST-PART"),"GOBACK.");
        var doc=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(CobolSemanticPort.open(s)));
        var r=doc.path("storage").path("renames").get(0);
        assertEquals("UNPROVEN",r.path("status").asText());assertFalse(r.path("gapCodes").isEmpty());
        assertEquals(1,doc.path("storage").path("bases").size());
    }
}
