package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PerformVaryingTest {
    @Test void typedSingleVariableEffectsTestModeAndAfterBoundary() throws Exception {
        var precise=Set.of("varying-before","varying-default","varying-after","varying-thru","varying-from-read","varying-decrement","varying-branches","varying-1","varying-2","varying-5","varying-40");
        var output=Path.of("target/perform-family");Files.createDirectories(output);
        try(var files=Files.list(Path.of("src/test/resources/cobol/perform-family"))) {
            for(var path:files.filter(p->p.getFileName().toString().startsWith("varying-")).sorted().toList()) {
                var name=path.getFileName().toString().replace(".cbl","");
                var source=Files.readAllLines(path).stream().map(line->line.substring(7)).collect(java.util.stream.Collectors.joining("\n"));
                var bytes=SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source));
                var sp=new ObjectMapper().readTree(bytes);var facts=PerformFamilyTest.ranges(sp);
                assertFalse(facts.isEmpty(),name);assertEquals("2.10.0",sp.path("contractVersion").asText());
                for(var p:facts) {
                    assertEquals(precise.contains(name),p.path("gapCodes").isEmpty(),name+": "+p.path("gapCodes"));
                    assertTrue(p.path("varying").isObject(),name);assertTrue(p.path("loop").isObject(),name);
                    assertEquals(name.equals("varying-after-level")?2:1,p.path("varying").path("levels").asInt(),name);
                    if(name.equals("varying-subscript-control")) {
                        var refs=p.path("varying").path("controls").get(0).path("references");
                        assertEquals(2,refs.size());assertEquals("WRITE",refs.get(0).path("role").asText());
                        assertEquals("READ",refs.get(1).path("role").asText(),"subscript remains a read");
                    }
                    if(precise.contains(name)) {
                        var controls=p.path("varying").path("controls");assertEquals(3,controls.size());
                        var variable=controls.get(0);assertEquals("CONTROL_VARIABLE",variable.path("role").asText());
                        assertEquals("WRITE",variable.path("references").get(0).path("role").asText());
                        assertTrue(variable.path("references").get(0).path("wholeItemAccess").isObject());
                        assertEquals("KNOWN",p.path("loop").path("condition").path("predicate").path("availability").asText());
                        assertEquals("NUMERIC_RELATION",p.path("loop").path("condition").path("predicate").path("profile").asText());
                    }
                }
                if(name.matches("varying-[0-9]+"))assertEquals(Integer.parseInt(name.substring(8)),facts.size());
                assertArrayEquals(bytes,SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)),name);
                Files.write(output.resolve(name+".json"),bytes);
            }
        }
    }
}
