package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PerformTimesTest {
    @Test void countReadOncePositiveLiteralAndConservativeCounts() throws Exception {
        var partial=Set.of("times-unresolved","times-noninteger","times-zero","times-incoming","times-escape","times-cycle","times-recursive","times-partial-end","times-unknown-body");
        var output=Path.of("target/perform-family");Files.createDirectories(output);
        try(var files=Files.list(Path.of("src/test/resources/cobol/perform-family"))) {
            for(var path:files.filter(p->p.getFileName().toString().startsWith("times-")).sorted().toList()) {
                var name=path.getFileName().toString().replace(".cbl","");
                var source=Files.readAllLines(path).stream().map(line->line.substring(7)).collect(java.util.stream.Collectors.joining("\n"));
                var bytes=SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source));
                var sp=new ObjectMapper().readTree(bytes);var facts=PerformFamilyTest.ranges(sp);
                assertFalse(facts.isEmpty(),name);assertEquals("2.20.0",sp.path("contractVersion").asText());
                for(var p:facts) {
                    assertEquals(!partial.contains(name),p.path("gapCodes").isEmpty(),name+": "+p.path("gapCodes"));
                    assertTrue(p.path("times").isObject(),name);
                    if(!partial.contains(name))assertEquals(name.equals("times-identifier")?"INTEGER_ITEM":"POSITIVE_INTEGER",p.path("times").path("profile").asText(),name);
                    if(name.equals("times-identifier"))assertTrue(p.path("times").path("reference").path("wholeItemAccess").isObject());
                }
                if(name.matches("times-[0-9]+"))assertEquals(Integer.parseInt(name.substring(6)),facts.size());
                assertArrayEquals(bytes,SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)),name);
                Files.write(output.resolve(name+".json"),bytes);
            }
        }
    }
}
