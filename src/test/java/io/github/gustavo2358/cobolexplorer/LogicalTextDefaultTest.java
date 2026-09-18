package io.github.gustavo2358.cobolexplorer;

import java.nio.file.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LogicalTextDefaultTest {
    @TempDir Path work;
    private com.fasterxml.jackson.databind.JsonNode run(String name, String... options) throws Exception {
        var source=work.resolve(name+".cbl");
        Files.writeString(source, ScalarMoveCheckpoint4ATest.program("01 REC-A.\n05 PGM-A PIC X(8).\n05 CODE-A PIC X(4).", "MOVE 'PROGA   1234' TO REC-A.\nCALL PGM-A.\nGOBACK.").lines().map(line->"       "+line).collect(java.util.stream.Collectors.joining("\n","","\n")));
        var out=work.resolve(name);
        var args=new java.util.ArrayList<String>(java.util.List.of("--source",source.toString(),"--copybooks",work.toString(),"--output",out.toString()));
        args.addAll(java.util.List.of(options));
        ExplorerMain.main(args.toArray(String[]::new));
        return new ObjectMapper().readTree(out.resolve("cobol-semantic-product.json").toFile());
    }
    @Test void normalCommandPublishesLogicalProof() throws Exception {
        var sp=run("default");
        assertEquals("UNSPECIFIED",sp.path("storage").path("profile").asText());
        assertEquals(3,sp.path("storage").path("logicalTextViews").size());
    }
    @Test void explicitLegacyOptOut() throws Exception {
        assertEquals(0,run("disabled","--logical-text","disabled").path("storage").path("logicalTextViews").size());
    }
    @Test void explicitPhysicalProfileStillWorksWithAuto() throws Exception {
        assertEquals(0,run("physical","--storage-profile",StorageLayoutSemantics.PROFILE_ID).path("storage").path("logicalTextViews").size());
        assertThrows(IllegalArgumentException.class,()->run("conflict","--storage-profile",StorageLayoutSemantics.PROFILE_ID,"--logical-text","enabled"));
    }
    @Test void explicitAutoAndInvalidMode() throws Exception {
        assertEquals(3,run("auto","--logical-text","auto").path("storage").path("logicalTextViews").size());
        assertThrows(IllegalArgumentException.class,()->run("bad","--logical-text","typo"));
    }
}
