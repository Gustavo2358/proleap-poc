package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.transport.JsonFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

final class ZstdArtifactTest {
    @TempDir Path directory;
    @Test void interoperableLosslessDeterministicAndStrict() throws Exception {
        byte[] json = "{\"text\":\"ação 日本語\",\"unknown\":true}".repeat(200).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Path plain=directory.resolve("plain.json"), zipped=directory.resolve("value.json.zst");
        Files.write(plain,json);
        try(var out=JsonFiles.output(Files.newOutputStream(zipped),zipped)){out.write(json);}
        byte[] frame=Files.readAllBytes(zipped);
        assertArrayEquals(new byte[]{0x28,(byte)0xb5,0x2f,(byte)0xfd},Arrays.copyOf(frame,4));
        assertTrue(frame.length < json.length);
        assertArrayEquals(json,JsonFiles.read(zipped));
        assertArrayEquals(json,JsonFiles.read(plain));
        try(var out=JsonFiles.output(Files.newOutputStream(zipped),zipped)){out.write(json);}
        assertArrayEquals(frame,Files.readAllBytes(zipped));
        Path renamed=directory.resolve("renamed.json");Files.write(renamed,frame);
        assertArrayEquals(json,JsonFiles.read(renamed));
        Files.write(zipped,Arrays.copyOf(frame,frame.length-1));
        assertThrows(java.io.IOException.class,()->JsonFiles.read(zipped));
        frame[frame.length-1]^=1;Files.write(zipped,frame);
        assertThrows(java.io.IOException.class,()->JsonFiles.read(zipped));
        Files.write(zipped,json);
        assertThrows(java.io.IOException.class,()->JsonFiles.read(zipped));
    }
    @Test void defaultCliCompressesEveryJsonWithIdenticalContent() throws Exception {
        Path plain=directory.resolve("plain"), zipped=directory.resolve("zipped");
        String fixture=Path.of("src/test/resources/cobol/semantic/semantic-product-entry-goback.cbl").toAbsolutePath().toString();
        ExplorerMain.main(new String[]{"--source",fixture,"--copybooks",directory.toString(),"--output",plain.toString(),"--json-compression","none"});
        ExplorerMain.main(new String[]{"--source",fixture,"--copybooks",directory.toString(),"--output",zipped.toString()});
        assertFalse(Files.exists(zipped.resolve("semantic-gap-assessment.json.zst")));
        assertFalse(Files.exists(zipped.resolve("gap-assessment-data.js")));
        var mapper = new com.fasterxml.jackson.databind.json.JsonMapper();
        String js = Files.readString(zipped.resolve("gaps-data.js"));
        var snapshot = mapper.readTree(js.substring("window.SEMANTIC_GAPS=".length(), js.length()-2));
        var product = mapper.readTree(JsonFiles.read(zipped.resolve("cobol-semantic-product.json.zst")));
        assertEquals(product.get("gaps"), snapshot.path("units").get(0).get("gaps"));
        for(String name:java.util.List.of("cobol-semantic-product","cobol-semantic-compilation","observed-dependencies","semantic-product")) {
            assertFalse(Files.exists(zipped.resolve(name+".json")));
            assertArrayEquals(Files.readAllBytes(plain.resolve(name+".json")),JsonFiles.read(zipped.resolve(name+".json.zst")),name);
        }
    }
}
