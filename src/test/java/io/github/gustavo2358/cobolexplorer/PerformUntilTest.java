package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PerformUntilTest {
    @Test void typedUntilTestModeReadsPartialAndMultiplicity() throws Exception {
        var partial=Set.of("until-unresolved","until-unsupported","until-incoming","until-escape","until-cycle","until-recursive","until-partial-end","until-unknown-body");
        var output=Path.of("target/perform-family");Files.createDirectories(output);
        try(var files=Files.list(Path.of("src/test/resources/cobol/perform-family"))) {
            for(var path:files.filter(p->p.getFileName().toString().startsWith("until-")).sorted().toList()) {
                var name=path.getFileName().toString().replace(".cbl","");
                var source=Files.readAllLines(path).stream().map(line->line.substring(7)).collect(java.util.stream.Collectors.joining("\n"));
                var bytes=SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source));
                var sp=new ObjectMapper().readTree(bytes);var facts=PerformFamilyTest.ranges(sp);
                assertFalse(facts.isEmpty(),name);assertEquals("2.26.0",sp.path("contractVersion").asText());
                for(var p:facts) {
                    assertEquals(!partial.contains(name),p.path("gapCodes").isEmpty(),name+": "+p.path("gapCodes"));
                    if(name.equals("until-mixed")&&p.path("loop").isNull())continue;
                    assertFalse(p.path("loop").isMissingNode(),name);
                    assertEquals(name.equals("until-after")||name.equals("until-branches")||name.equals("until-thru")||name.equals("until-mixed")||name.matches("until-[0-9]+")?"AFTER":"BEFORE",p.path("loop").path("testMode").asText());
                    if(!partial.contains(name)) {
                        var condition=p.path("loop").path("condition");
                        assertEquals("KNOWN",condition.path("predicate").path("availability").asText());
                        assertEquals("UNKNOWN",condition.path("predicate").path("truthValue").asText());
                        assertEquals(1,condition.path("references").size());
                        assertFalse(condition.path("references").get(0).path("wholeItemAccess").isNull());
                    }
                }
                if(name.matches("until-[0-9]+"))assertEquals(Integer.parseInt(name.substring(6)),facts.size());
                assertArrayEquals(bytes,SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)),name);
                Files.write(output.resolve(name+".json"),bytes);
            }
        }
    }
}
