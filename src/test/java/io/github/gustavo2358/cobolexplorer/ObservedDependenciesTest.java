package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ObservedDependenciesTest {
    @TempDir Path directory;
    @Test void cliPublishesObservedNamesEvenWhenProcedureEntryIsUnavailable() throws Exception {
        for(String code:new String[]{"COPY PRIVATE-COPY.\nCALL 'PROGA'.","CALL 'PROGA'.\nCOPY PRIVATE-COPY."}) {
            Path source=directory.resolve("input.cbl"),output=directory.resolve("output");
            Files.writeString(source,ScopedInputTest.unit("OBSERVED-PGM","01 ARG PIC X.",code));
            ExplorerMain.main(new String[]{"--source",source.toString(),"--source-format","free","--output",output.toString()});
            var json=new ObjectMapper();var observed=json.readTree(output.resolve("observed-dependencies.json").toFile());
            assertEquals("OBSERVED_ONLY",observed.path("sites").get(0).path("knowledge").asText());
            assertEquals("UNKNOWN",observed.path("sites").get(0).path("reachability").asText());
            assertEquals("PROGA",observed.path("sites").get(0).path("literalCandidates").get(0).path("value").asText());
            assertTrue(observed.path("sites").get(0).path("unknownRemainder").asBoolean());
            assertEquals(1,observed.path("inputGaps").size());assertFalse(observed.has("edges"));
            var sp=json.readTree(output.resolve("cobol-semantic-product.json").toFile());
            assertEquals("INPUT_MISSING",sp.path("entryInventory").path("entries").get(0).path("start").path("availability").asText());
        }
    }
}
