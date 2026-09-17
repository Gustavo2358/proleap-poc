package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Independent N-LR oracles: SC27-8713-03, 28 April 2026, SELECT/ASSIGN/File section. */
class FileDeclarationContractTest {
    @Test void selectAndFdAreOneDeclarationWithoutExecution() throws Exception {
        var json = publish("SELECT OPTIONAL CLIENTES ASSIGN TO CLIENTDD.",
                "FD CLIENTES.\n01 REC PIC X(8).", "");
        assertEquals("2.22.0", json.path("contractVersion").asText());
        var inventory = json.path("fileInventory");
        assertEquals("KNOWN", inventory.path("availability").asText());
        assertEquals(1, inventory.path("declarations").size());
        var file = inventory.path("declarations").get(0);
        assertEquals(json.path("unit"), file.path("owner"));
        assertEquals("CLIENTES", file.path("logicalFile").asText());
        assertEquals("FD", file.path("kind").asText());
        assertTrue(file.path("optional").asBoolean());
        assertEquals(2, file.path("origins").size());
        assertEquals("ASSIGNMENT_NAME", file.path("assignment").path("sourceKind").asText());
        assertEquals("CLIENTDD", file.path("assignment").path("externalFileName").asText());
        assertEquals("CLIENTDD", file.path("assignment").path("original").asText());
        assertEquals(1, file.path("records").size());
        assertEquals(1, json.path("statements").size(), "only written GOBACK; declarations never create I/O");
        assertFalse(json.toString().contains("bindingMechanism"));
        assertFalse(json.toString().contains("zos.ddname"));
        Files.createDirectories(Path.of("target/fd-w0"));
        Files.writeString(Path.of("target/fd-w0/declaration.json"), json.toPrettyString());
    }

    @Test void ibmPrefixIsInterpretedAndSortNameIsComment() throws Exception {
        var json = publish("SELECT F ASSIGN TO DISK-S-clientdd.\nSELECT S ASSIGN TO SORTWK.",
                "FD F.\n01 R PIC X.\nSD S.\n01 SR PIC X.", "");
        save(json,"sort");
        var files = json.path("fileInventory").path("declarations");
        assertEquals(2, files.size());
        assertEquals("CLIENTDD", files.get(0).path("assignment").path("externalFileName").asText());
        assertEquals("DISK-S-clientdd", files.get(0).path("assignment").path("original").asText());
        assertEquals("SD", files.get(1).path("kind").asText());
        assertTrue(files.get(1).path("assignment").path("externalFileName").isNull());
        assertEquals("SORT_COMMENT", files.get(1).path("assignment").path("sourceKind").asText());
    }

    @Test void keysStatusAndMultipleRecordsRetainResolvedIdentity() throws Exception {
        var json = publish("""
                SELECT F ASSIGN TO CLIENTDD ORGANIZATION IS INDEXED
                ACCESS MODE IS DYNAMIC RECORD KEY IS K
                ALTERNATE RECORD KEY IS A WITH DUPLICATES FILE STATUS IS FS.
                """, "FD F IS GLOBAL.\n01 R.\n  05 K PIC X.\n  05 A PIC X.\n01 R2 PIC XX.",
                "WORKING-STORAGE SECTION.\n01 FS PIC XX.");
        var file = json.path("fileInventory").path("declarations").get(0);
        save(json,"keys-status");
        assertEquals("INDEXED", file.path("organization").asText());
        assertEquals("DYNAMIC", file.path("accessMode").asText());
        assertEquals("GLOBAL", file.path("visibility").asText());
        assertEquals(2, file.path("records").size());
        assertEquals(3, file.path("references").size());
        for (var reference : file.path("references"))
            assertEquals("RESOLVED", reference.path("binding").path("status").asText());
        assertTrue(file.path("references").get(1).path("duplicates").asBoolean());
    }

    @Test void missingSelectAndUnsupportedDynamicAreLocalized() throws Exception {
        var missing = publish("", "FD F.\n01 R PIC X.", "");
        var file = missing.path("fileInventory").path("declarations").get(0);
        assertTrue(file.path("gapCodes").toString().contains("FILE_SELECT_MISSING"));
        assertTrue(file.path("assignment").path("externalFileName").isNull());
        var dynamic = publish("SELECT F ASSIGN TO DYNAMIC X.", "FD F.\n01 R PIC X.",
                "WORKING-STORAGE SECTION.\n01 X PIC X(8) VALUE 'CLIENTDD'.");
        var assignment = dynamic.path("fileInventory").path("declarations").get(0).path("assignment");
        assertEquals("UNAVAILABLE", assignment.path("availability").asText());
        assertTrue(assignment.path("externalFileName").isNull(), "D/W10 is not core name inference");
        assertTrue(assignment.path("gapCodes").toString().contains("ASSIGN_OUTSIDE_N_LR"));
        assertEquals(0, publish("", "", "").path("fileInventory").path("declarations").size());
    }

    @Test void writeRecordNominalBindingSelectsItsDeclaredOwner() throws Exception {
        var json=publish("SELECT F ASSIGN TO FIRSTDD.\nSELECT G ASSIGN TO SECONDDD.",
                "FD F.\n01 R PIC X.\nFD G.\n01 R PIC X.","","WRITE R OF F.\nGOBACK.");
        var files=json.path("fileInventory").path("declarations");
        var target=files.get(0).path("records").get(0);
        var write=json.path("statements").get(0);
        assertEquals("OBSERVED",write.path("variant").asText());
        // W0 publishes record ownership, while execution operands remain a W2 concern.
        var a=AstBoundaryTestSupport.analyze(source("SELECT F ASSIGN TO FIRSTDD.\nSELECT G ASSIGN TO SECONDDD.",
                "FD F.\n01 R PIC X.\nFD G.\n01 R PIC X.","","WRITE R OF F.\nGOBACK."),"file-declarations.cbl");
        var binding=a.resolution().entries().stream().filter(e->e.occurrence().writtenText().equals("R OF F"))
                .findFirst().orElseThrow();
        assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,binding.status());
        var selected=binding.selectedCandidate().orElseThrow().entityId();
        var table=a.tables().forProgramUnit(selected.programUnitId()).orElseThrow().symbolTable();
        var symbol=table.symbols().get(selected.localId());
        assertEquals("F",table.scopes().get(symbol.scopeId()).name());
        var record=java.util.stream.StreamSupport.stream(json.path("dataDeclarations").spliterator(),false)
                .filter(d->d.path("id").equals(target)).findFirst().orElseThrow();
        assertEquals(11,record.path("provenance").path("original").path("startLine").asInt());
        assertNotEquals(target,files.get(1).path("records").get(0));
    }

    @Test void ibmAlphanumericAssignmentNameIsNotADialectFilename() throws Exception {
        var valid=publish("SELECT F ASSIGN TO 'A#DD'.","FD F.\n01 R PIC X.","");
        var a=valid.path("fileInventory").path("declarations").get(0).path("assignment");
        assertEquals("ASSIGNMENT_NAME",a.path("sourceKind").asText());
        assertEquals("A#DD",a.path("externalFileName").asText());
        assertEquals("'A#DD'",a.path("original").asText());
        var other=publish("SELECT F ASSIGN TO '/tmp/data.txt'.","FD F.\n01 R PIC X.","");
        assertTrue(other.path("fileInventory").path("declarations").get(0).path("assignment").path("externalFileName").isNull());
    }

    @Test void copyReplacingRetainsExpandedNamesAndIncludeOrigins() throws Exception {
        var json = publish("SELECT CLIENTES ASSIGN TO CLIENTDD.",
                "COPY FDW0 REPLACING FILE-TEMPLATE BY CLIENTES\n    RECORD-TEMPLATE BY REC.", "");
        var file = json.path("fileInventory").path("declarations").get(0);
        assertEquals("CLIENTES", file.path("logicalFile").asText());
        assertEquals(1, file.path("records").size());
        save(json,"copy");
        var origin = file.path("origins").get(1);
        assertTrue(origin.path("original").path("file").asText().endsWith("FDW0.cpy"));
        assertEquals(1, origin.path("includeChain").size());
        assertEquals("FDW0", origin.path("includeChain").get(0).path("requestedName").asText());
    }

    @Test void containedProgramsKeepSeparateOwnersAndLocalInventories() throws Exception {
        var source = Files.readString(Path.of("src/test/resources/cobol/resolution/nested-global-file.cbl"));
        var a = AstBoundaryTestSupport.analyze(source, "nested-global-file.cbl");
        var products = new CobolSemanticProductProjector.FrontendProducts(a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),
                ScalarMoveSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report()));
        var outer = CobolSemanticProductProjector.project(products,a.model().programUnits().get(0).id());
        var inner = CobolSemanticProductProjector.project(products,a.model().programUnits().get(1).id());
        assertEquals(4,outer.fileInventory().declarations().size());
        assertEquals(1,inner.fileInventory().declarations().size());
        assertNotEquals(outer.unit(),inner.unit());
        assertTrue(outer.fileInventory().declarations().stream().allMatch(f -> f.owner().equals(outer.unit())));
        assertEquals(inner.unit(),inner.fileInventory().declarations().get(0).owner());
        assertEquals("SHADOW-FILE",inner.fileInventory().declarations().get(0).logicalFile());
        assertEquals(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.FileVisibility.GLOBAL,
                outer.fileInventory().declarations().get(0).visibility());
    }

    @Test void ambiguousKeysAndMissingDescriptionsCannotInventBinding() throws Exception {
        var json = publish("SELECT F ASSIGN TO FIRSTDD RECORD KEY IS K.\nSELECT G ASSIGN TO SECONDDD.",
                "FD F.\n01 R.\n 05 K PIC X.\nFD G.\n01 R2.\n 05 K PIC X.", "");
        var reference=json.path("fileInventory").path("declarations").get(0).path("references").get(0);
        assertEquals("AMBIGUOUS",reference.path("binding").path("status").asText());
        assertTrue(reference.path("binding").path("selected").isNull());
        assertEquals(2,reference.path("binding").path("candidates").size());
        var missing=publish("SELECT F ASSIGN TO FIRSTDD.","","").path("fileInventory").path("declarations").get(0);
        assertEquals("UNKNOWN",missing.path("kind").asText());
        assertTrue(missing.path("gapCodes").toString().contains("FILE_DESCRIPTION_MISSING"));
    }

    private static void save(JsonNode document,String name) throws Exception {
        Files.createDirectories(Path.of("target/fd-w0"));
        Files.writeString(Path.of("target/fd-w0/"+name+".json"),document.toPrettyString());
    }

    static JsonNode publish(String select, String files, String data) throws Exception {
        return publish(select,files,data,"GOBACK.");
    }
    static JsonNode publish(String select, String files, String data, String body) throws Exception {
        var source = source(select,files,data,body);
        var a = AstBoundaryTestSupport.analyze(source, "file-declarations.cbl");
        var products = new CobolSemanticProductProjector.FrontendProducts(a.build(), a.tables(), a.occurrences(),
                a.resolution(), a.report(), ScalarMoveSemantics.analyze(a.build(), a.tables(), a.resolution(), a.report()));
        var state = CobolSemanticProductProjector.project(products, a.model().programUnits().get(0).id());
        return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(CobolSemanticPort.open(state)));
    }
    private static String source(String select,String files,String data,String body) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. FILETEST.\nENVIRONMENT DIVISION.\n"
                + "INPUT-OUTPUT SECTION.\nFILE-CONTROL.\n" + select + "\nDATA DIVISION.\nFILE SECTION.\n"
                + files + "\n" + data + "\nPROCEDURE DIVISION.\n"+body+"\nEND PROGRAM FILETEST.\n";
    }
}
