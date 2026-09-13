package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Permanent dimensional contract, through real preprocessing and the public SP. */
class LocalizedInputCompletenessContractTest {
    @TempDir Path directory;

    @Test void primaryStartSkipsOnlyLeadingEntryDeclarations() throws Exception {
        for (String prefix : List.of("ENTRY 'ALT'.", "ENTRY 'A'.\nENTRY 'B'.")) {
            var sp = publish("", "MAIN.\n" + prefix + "\nMOVE 'PROGA' TO WS-PGM.\nCALL WS-PGM.\nGOBACK.");
            export("leading-entry", sp);
            assertEquals("MOVE", startFact(sp).path("variant").asText());
            assertEquals("PARTIAL", sp.path("entryInventory").path("status").asText());
            assertTrue(sp.path("entryInventory").path("gapCodes").toString().contains("ALTERNATE_ENTRIES_NOT_PROJECTED"));
        }
        var perform = publish("", "ENTRY 'ALT'.\nPERFORM DEFINE-PGM.\nCALL WS-PGM.\nGOBACK.\nDEFINE-PGM.\nMOVE 'PROGA' TO WS-PGM.");
        assertEquals(8, startFact(perform).path("header").path("provenance").path("original").path("startLine").asInt(), "PERFORM line");
        var display = publish("", "ENTRY 'ALT'.\nDISPLAY 'HELLO'.\nGOBACK.");
        assertEquals(8, startFact(display).path("header").path("provenance").path("original").path("startLine").asInt(), "DISPLAY line");
    }

    @Test void laterEntryDoesNotMovePrimaryStartAndEmptyEntryDoesNotInventOne() throws Exception {
        var sp = publish("", "MOVE 'PROGA' TO WS-PGM.\nENTRY 'ALT'.\nMOVE 'PROGB' TO WS-PGM.\nGOBACK.");
        assertEquals(7, startFact(sp).path("header").path("provenance").path("original").path("startLine").asInt());
        assertTrue(entry(publish("", "MAIN.\nENTRY 'ALT'.")).path("start").path("statement").isNull());
    }

    @Test void missingDataCopiesPreserveStartButNotInputSignatureOrStorageCompleteness() throws Exception {
        for (String copies : List.of("COPY UNKNOWN.", "COPY UNKNOWN.\nCOPY SECOND.")) {
            var sp = publish(copies, "MOVE 'PROGA' TO WS-PGM.\nCALL WS-PGM.\nGOBACK.");
            assertEquals("KNOWN", entry(sp).path("availability").asText());
            assertEquals("KNOWN", entry(sp).path("start").path("availability").asText());
            assertEquals("MOVE", startFact(sp).path("variant").asText());
            assertEquals("INPUT_MISSING", sp.path("coverage").path("inventoryStatus").asText());
            assertEquals("INPUT_MISSING", sp.path("entryInventory").path("status").asText());
            assertEquals("INPUT_MISSING", entry(sp).path("signature").path("availability").asText());
            assertTrue(entry(sp).path("signature").path("parameterCount").isNull());
            assertEquals("INPUT_MISSING", sp.path("storageIndependence").path("availability").asText());
            assertTrue(Files.readString(directory.resolve("out/resolution-data.js")).contains("UNRESOLVED_COPY"));
            assertTrue(sp.toString().contains("UNRESOLVED_COPY"));
            export("data-independent", sp);
        }
    }

    @Test void missingDeclarationDoesNotAcquireBinding() throws Exception {
        var sp = publish("COPY UNKNOWN.", "CALL UNKNOWN-PGM.\nGOBACK.");
        assertEquals("CALL", startFact(sp).path("variant").asText());
        export("data-unknown", sp);
        assertFalse(sp.path("dataDeclarations").toString().contains("UNKNOWN-PGM"));
        assertTrue(startFact(sp).path("target").toString().contains("UNRESOLVED"));
    }

    @Test void missingExecutableCopyBeforeOrAfterStartRemainsBlocked() throws Exception {
        for (String body : List.of("COPY UNKNOWN.\nCALL WS-PGM.",
                "MOVE 'PROGA' TO WS-PGM.\nCOPY UNKNOWN.\nCALL WS-PGM.")) {
            var sp = publish("COPY DATA-GAP.", body);
            export("procedure-unsafe", sp);
            assertTrue(entry(sp).path("start").path("statement").isNull());
            assertEquals("BLOCKED", entry(sp).path("readiness").path("lowering").path("status").asText());
            assertTrue(sp.toString().contains("UNRESOLVED_COPY"));
        }
    }

    @Test void nestedCopyProvenanceQualifiesDataButRejectsSameCopyInProcedure() throws Exception {
        Files.createDirectories(directory.resolve("copybooks"));
        Files.writeString(directory.resolve("copybooks/OUTER.cpy"), "       COPY MISSING.\n");
        assertEquals("KNOWN", entry(publish("COPY OUTER.", "GOBACK.")).path("start").path("availability").asText());
        assertTrue(entry(publish("COPY OUTER.", "COPY OUTER.\nGOBACK.")).path("start").path("statement").isNull());
    }

    @Test void unqualifiedInputDiagnosticsCannotReuseDataProof() {
        var d = new Diagnostic("COBOL", Diagnostic.Phase.PREPROCESSOR, Diagnostic.Code.UNRESOLVED_COPY,
                "data.cbl", 3, 7, "missing", "UNKNOWN", "");
        var proof = new EntryInputProof(List.of(d));
        assertTrue(proof.unaffectedBy(new ResolutionAnalysisReport.FrontendState(0, 0, 0, List.of(d))));
        assertFalse(proof.unaffectedBy(new ResolutionAnalysisReport.FrontendState(0, 0, 0, List.of(d, d))), "each missing occurrence needs a region");
        for (var input : List.of(new ResolutionAnalysisReport.FrontendState(1, 0, 0, List.of(d)),
                new ResolutionAnalysisReport.FrontendState(0, 1, 0, List.of(d)),
                new ResolutionAnalysisReport.FrontendState(0, 0, 1, List.of(d)),
                new ResolutionAnalysisReport.FrontendState(0, 0, 0, List.of(d,
                        new Diagnostic("COBOL", Diagnostic.Phase.IO, "missing file", 1, 0, "unavailable", "", "")))))
            assertFalse(proof.unaffectedBy(input));
        assertFalse(new EntryInputProof(List.of()).unaffectedBy(
                new ResolutionAnalysisReport.FrontendState(0, 0, 0, List.of(d))));
    }

    private static void export(String name, JsonNode sp) throws Exception {
        var out = Path.of("target/entry-localization");
        Files.createDirectories(out);
        Files.write(out.resolve(name + ".json"), new ObjectMapper().writeValueAsBytes(sp));
    }

    private JsonNode publish(String copies, String body) throws Exception {
        String source = "IDENTIFICATION DIVISION.\nPROGRAM-ID. LOCAL-ENTRY.\nDATA DIVISION.\n"
                + (copies.isEmpty() ? "" : copies + "\n")
                + "WORKING-STORAGE SECTION.\n01 WS-PGM PIC X(8).\nPROCEDURE DIVISION.\n" + body + "\n";
        Files.createDirectories(directory.resolve("copybooks"));
        Path input = directory.resolve("input.cbl");
        Files.writeString(input, source.lines().map(line -> "       " + line + "\n").collect(java.util.stream.Collectors.joining()));
        ExplorerMain.main(new String[]{"--source", input.toString(), "--copybooks", directory.resolve("copybooks").toString(),
                "--output", directory.resolve("out").toString()});
        return new ObjectMapper().readTree(directory.resolve("out/cobol-semantic-product.json").toFile());
    }

    private static JsonNode entry(JsonNode sp) { return sp.path("entryInventory").path("entries").get(0); }
    private static JsonNode startFact(JsonNode sp) {
        var start = entry(sp).path("start").path("statement");
        assertFalse(start.isNull(), "canonical primary start required");
        for (var statement : sp.path("statements")) if (statement.path("header").path("id").equals(start)) return statement;
        throw new AssertionError("start must reference a published statement");
    }
}
