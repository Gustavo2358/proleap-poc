package io.github.gustavo2358.cobolexplorer;
import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.FileMemoryEffectsTest.fixture;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

class FileEffectsContractTest {
    @Test void memoryContractRejectsMissingCasesAndStrongBufferEffects() {
        var a=fixture("SELECT F ASSIGN TO INDD FILE STATUS FS.","FD F.\n01 REC PIC X(8).","01 FS PIC XX.\n01 DEST PIC X(8).","READ F INTO DEST.").source();
        var port=ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        var e=port.fileInventory().operations().uses().get(0).effects();
        assertThrows(IllegalArgumentException.class,()->new FileEffectPlan(e.availability(),e.ioReads(),e.before(),e.outcomes().subList(0,3),e.unknownReadBound(),e.unknownWriteBound(),e.gapCodes()));
        var record=e.outcomes().get(0).steps().get(0);var t=record.destination();
        var exact=new FileMemoryTarget(t.data(),t.regional(),false,t.reference(),t.provenance());
        assertThrows(IllegalArgumentException.class,()->new FileMemoryStep(FileMemoryRole.RECORD,FileMemoryKind.MUST_UNKNOWN,exact,Optional.empty(),List.of(),record.provenance()));
        var into=e.outcomes().get(0).steps().get(2);
        assertThrows(IllegalArgumentException.class,()->new FileOutcomeEffects(FileEffectOutcome.END,List.of(into)));
    }
    static JsonNode publish(FileMemoryEffectsTest.Fixture f)throws Exception {
        var a=f.source();var port=ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(port));
    }
    static JsonNode effect(JsonNode p){return p.path("fileInventory").path("operations").path("uses").get(0).path("effects");}
    @Test void nativeReadPublishesConditionalEffectsAndGeneralAllocationProof()throws Exception {
        var f=fixture("SELECT F ASSIGN TO INDD FILE STATUS IO-STATUS.","FD F.\n01 REC PIC X(8).",
            "01 DEST PIC X(8).\n01 IO-STATUS PIC XX.\n01 SAFE-PGM PIC X(8) VALUE 'SAFE'.",
            "MOVE 'OLD' TO REC.\nREAD F INTO DEST AT END CALL 'EOFHAND' END-READ.\nCALL REC.\nCALL SAFE-PGM.");
        var p=publish(f);assertEquals("2.24.0",p.path("contractVersion").asText());assertEquals("1.8.0",p.path("storage").path("version").asText());
        assertEquals("1.3.0",p.path("fileInventory").path("version").asText());var e=effect(p);assertEquals("KNOWN",e.path("availability").asText());
        assertFalse(e.path("unknownWriteBound").asBoolean());assertTrue(e.path("before").isEmpty());
        var cases=new HashMap<String,JsonNode>();for(var c:e.path("outcomes"))cases.put(c.path("outcome").asText(),c.path("steps"));
        assertEquals(Set.of("SUCCESS","END","INVALID_KEY","OTHER_ERROR"),cases.keySet());
        var success=cases.get("SUCCESS");assertEquals(3,success.size());assertEquals("MAY_UNKNOWN",success.get(0).path("kind").asText());
        assertTrue(success.get(0).path("destination").path("wholeBase").asBoolean());
        assertEquals("MUST_UNKNOWN",success.get(2).path("kind").asText());assertEquals("INTO",success.get(2).path("role").asText());
        for(var c:List.of("END","INVALID_KEY","OTHER_ERROR"))for(var step:cases.get(c))assertNotEquals("INTO",step.path("role").asText());
        for(var base:p.path("storage").path("bases"))assertEquals("INDEPENDENT_LOCAL_STORAGE",base.path("allocation").asText(),"FILE allocation is not WORKING-STORAGE");
        Files.createDirectories(Path.of("target/fd-w3"));Files.writeString(Path.of("target/fd-w3/read-effects.json"),p.toPrettyString());
        assertEquals(p,publish(f));
    }
    @Test void fromMoveIsBeforeIoAndKeepsTheActualSource()throws Exception {
        var f=fixture("SELECT F ASSIGN TO OUTDD.\nSELECT G ASSIGN TO INDD.","FD F.\n01 REC PIC X(8).\nFD G.\n01 OTHER-REC PIC X(8).","01 SAFE PIC X(8).","REWRITE REC FROM OTHER-REC.");
        var p=publish(f);var e=effect(p);assertEquals(1,e.path("before").size());var transfer=e.path("before").get(0);
        assertEquals("COPY_BYTES",transfer.path("kind").asText());assertNotEquals(transfer.path("source").path("data"),transfer.path("destination").path("data"));
        assertEquals(1,p.path("fileInventory").path("operations").path("uses").size());
        Files.createDirectories(Path.of("target/fd-w3"));Files.writeString(Path.of("target/fd-w3/from-effects.json"),p.toPrettyString());
    }
    @Test void relativeNumericReceiversKeepLocalBoundsWithoutTextMust()throws Exception {
        var f=fixture("SELECT F ASSIGN TO INDD ORGANIZATION RELATIVE\n ACCESS SEQUENTIAL RELATIVE KEY RK FILE STATUS FS.",
            "FD F RECORD IS VARYING FROM 2 TO 8 CHARACTERS\n DEPENDING ON REC-LEN.\n01 REC PIC X(8).",
            "01 RK PIC 9(4).\n01 FS PIC 99.\n01 REC-LEN PIC 9(4) COMP.\n01 SAFE PIC X(8) VALUE 'SAFE'.","READ F NEXT RECORD.\nCALL SAFE.");
        var p=publish(f);var e=effect(p);assertFalse(e.path("unknownWriteBound").asBoolean());
        assertEquals(List.of("RECORD","RELATIVE_KEY","RECORD_LENGTH","FILE_STATUS"),
            java.util.stream.StreamSupport.stream(e.path("outcomes").get(0).path("steps").spliterator(),false).map(s->s.path("role").asText()).toList());
        for(var s:e.path("outcomes").get(0).path("steps"))assertEquals("MAY_UNKNOWN",s.path("kind").asText());
        Files.createDirectories(Path.of("target/fd-w3"));Files.writeString(Path.of("target/fd-w3/relative-effects.json"),p.toPrettyString());
    }
    @Test void unprovedFromStillReadsItsSourceBeforeTheIo()throws Exception {
        Files.createDirectories(Path.of("target/fd-w3"));
        var alias=publish(fixture("SELECT F ASSIGN TO OUTDD.","FD F.\n01 REC PIC X(8).","01 SAFE PIC X(8).","WRITE REC FROM REC."));
        var transfer=effect(alias).path("before").get(0);assertEquals("MAY_UNKNOWN",transfer.path("kind").asText());
        assertFalse(transfer.path("source").isNull());Files.writeString(Path.of("target/fd-w3/alias-from-effects.json"),alias.toPrettyString());
        var missing=publish(fixture("SELECT F ASSIGN TO OUTDD.","FD F.\n01 REC PIC X(8).","01 SAFE PIC X(8).","WRITE REC FROM MISSING."));
        Files.writeString(Path.of("target/fd-w3/missing-from-effects.json"),missing.toPrettyString());
        assertTrue(effect(missing).path("unknownReadBound").asBoolean(),"unresolved FROM cannot prove an empty source read bound");
    }
}
