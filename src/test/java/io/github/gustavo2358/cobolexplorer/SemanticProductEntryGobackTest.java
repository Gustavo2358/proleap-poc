package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.loweringreadiness.SemanticPortLoweringProbe;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static org.junit.jupiter.api.Assertions.*;

/** EVAL-SP-004: executable entry/local GOBACK exit, never inferred by a consumer. */
class SemanticProductEntryGobackTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/semantic/semantic-product-entry-goback.cbl");

    @Test
    void minimalFixturePublishesPrimaryEntryAndDedicatedLocalExit() throws Exception {
        var port = publish(Files.readString(FIXTURE));
        assertEquals("AIR-FIRST", port.unit().canonicalProgramName());
        assertEquals(1, port.entries().size());
        var entry = port.entries().get(0);
        var goback = assertInstanceOf(GobackFact.class, port.statements().get(0));
        assertEquals(EntryRole.PRIMARY, entry.role());
        assertEquals(port.unit(), entry.id().unit());
        assertEquals(port.unit(), goback.header().id().unit());
        assertEquals(Availability.KNOWN, entry.availability());
        assertEquals(Availability.KNOWN, entry.start().availability());
        assertEquals(Optional.of(goback.header().id()), entry.start().statement());
        assertEquals(Availability.KNOWN, entry.signature().availability());
        assertEquals(Optional.of(0), entry.signature().parameterCount());
        assertEquals(ReturningClause.ABSENT, entry.signature().returningClause());
        assertEquals(CoverageStatus.MODELED, entry.coverage());
        assertEquals(CoverageStatus.MODELED, goback.header().coverage());
        assertTrue(entry.provenance().exact());
        assertTrue(goback.header().provenance().exact());
        assertEquals(3, entry.provenance().original().startLine());
        assertEquals(4, goback.header().provenance().original().startLine());
        assertEquals(new ProgramPoint(0), goback.header().point());
        assertEquals(LocalContinuation.NONE, goback.localContinuation());
        assertEquals(GobackExit.CURRENT_PROGRAM_INVOCATION, goback.exit());
        assertEquals(ReadinessStatus.SUFFICIENT, entry.readiness().lowering().status());
        assertEquals(ReadinessStatus.SUFFICIENT, goback.header().readiness().lowering().status());
        assertEquals(ReadinessStatus.SUFFICIENT, goback.header().readiness().cfg().status());
        assertEquals(ReadinessStatus.BLOCKED, goback.header().readiness().effectsDataflow().status());
        assertTrue(port.observedStatements().isEmpty());
        assertEquals(InventoryStatus.PARTIAL, port.entryInventory().status());
        assertTrue(port.entryInventory().gapCodes().contains("ALTERNATE_ENTRIES_NOT_PROJECTED"));
        var reconstruction = SemanticPortLoweringProbe.reconstruct(port);
        assertTrue(reconstruction.violations().isEmpty(), reconstruction.violations().toString());
        assertEquals(entry.start().statement(), reconstruction.entries().get(0).start().statement());
        assertInstanceOf(SemanticPortLoweringProbe.GobackNode.class,
                reconstruction.statements().get(0));
    }

    @Test
    void jsonAloneAnswersTheSliceAndIsByteDeterministic() throws Exception {
        String source = Files.readString(FIXTURE);
        var port = publish(source);
        byte[] bytes = SemanticProductJsonWriter.serialize(port);
        assertArrayEquals(bytes, SemanticProductJsonWriter.serialize(port));
        assertArrayEquals(bytes, SemanticProductJsonWriter.serialize(publish(source)));
        JsonNode json = new ObjectMapper().readTree(bytes);
        assertEquals("cobol-semantic-product", json.path("schema").asText());
        assertEquals("1.7.0", json.path("contractVersion").asText());
        assertEquals("AIR-FIRST", json.path("unit").path("canonicalProgramName").asText());
        var entry = json.path("entryInventory").path("entries").get(0);
        var terminal = json.path("statements").get(0);
        assertEquals("PRIMARY", entry.path("role").asText());
        assertEquals("KNOWN", entry.path("availability").asText());
        assertEquals(terminal.path("header").path("id"), entry.path("start").path("statement"));
        assertEquals("GOBACK", terminal.path("variant").asText());
        assertEquals("NONE", terminal.path("localContinuation").asText());
        assertEquals("CURRENT_PROGRAM_INVOCATION", terminal.path("exit").asText());
        assertEquals("KNOWN", entry.path("signature").path("availability").asText());
        assertEquals(0, entry.path("signature").path("parameterCount").asInt(-1));
        assertEquals("MODELED", terminal.path("header").path("coverage").asText());
        assertFalse(entry.path("provenance").isMissingNode());
        assertFalse(terminal.path("header").path("provenance").isMissingNode());
        assertEquals("PARTIAL", json.path("entryInventory").path("status").asText());
        assertFalse(json.path("entryInventory").path("gapCodes").isEmpty());
    }

    @Test
    void physicalStatementAfterGobackDoesNotBecomeItsContinuation() {
        var port = publish(program("GOBACK.\nCONTINUE."));
        assertEquals(2, port.statements().size());
        var terminal = assertInstanceOf(GobackFact.class, port.statements().get(0));
        assertInstanceOf(ObservedStatement.class, port.statements().get(1));
        assertEquals(Optional.of(terminal.header().id()), port.entries().get(0).start().statement());
        assertEquals(LocalContinuation.NONE, terminal.localContinuation());
        assertEquals(1, port.coverage().modeledStatements());
        assertEquals(ReadinessStatus.BLOCKED, port.coverage().readiness().cfg().status());
    }

    @Test
    void otherTerminalsAreNotGoback() {
        var port = publish(program("STOP RUN.\nEXIT PROGRAM.\nGOBACK."));
        assertEquals(3, port.statements().size());
        assertInstanceOf(ObservedStatement.class, port.statements().get(0));
        assertInstanceOf(ObservedStatement.class, port.statements().get(1));
        assertInstanceOf(GobackFact.class, port.statements().get(2));
        assertTrue(port.observedStatements().stream().allMatch(f ->
                f.header().readiness().cfg().status() == ReadinessStatus.BLOCKED));
    }

    @Test
    void missingEntryTargetAndCrossUnitTargetAreRejected() {
        var state = state(program("GOBACK."));
        var entry = state.entryInventory().entries().get(0);
        for (var target : List.of(new StatementId(state.unit(), 999),
                new StatementId(new UnitId("OTHER", List.of(0), "OTHER"), 0))) {
            var broken = new EntryFact(entry.id(), entry.role(), entry.availability(),
                    new ExecutableStart(Availability.KNOWN, Optional.of(target)),
                    entry.signature(), entry.provenance(), entry.coverage(), entry.readiness(), entry.gaps());
            var inventory = new EntryInventory(state.entryInventory().status(), List.of(broken),
                    state.entryInventory().gapCodes());
            assertThrows(IllegalArgumentException.class, () -> new State(state.unit(), state.policy(),
                    state.dataDeclarations(), state.statements(), state.gaps(), state.coverage(), inventory));
        }
    }

    @Test
    void removedGobackCannotRemainCountedAsModeled() {
        var state = state(program("GOBACK."));
        assertThrows(IllegalArgumentException.class, () -> new State(state.unit(), state.policy(),
                state.dataDeclarations(), List.of(), List.of(), state.coverage()));
    }

    @Test
    void unknownSignatureAndStartAreExplicitWithoutInventingZero() throws Exception {
        var port = publish("IDENTIFICATION DIVISION.\nPROGRAM-ID. AIR-FIRST.\nEND PROGRAM AIR-FIRST.");
        var entry = port.entries().get(0);
        assertEquals(Availability.UNAVAILABLE, entry.availability());
        assertEquals(Availability.UNAVAILABLE, entry.start().availability());
        assertTrue(entry.start().statement().isEmpty());
        assertEquals(Availability.UNAVAILABLE, entry.signature().availability());
        assertTrue(entry.signature().parameterCount().isEmpty());
        assertEquals(ReturningClause.UNKNOWN, entry.signature().returningClause());
        assertEquals(ReadinessStatus.BLOCKED, entry.readiness().lowering().status());
        assertFalse(entry.gaps().isEmpty());
        var json = new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(port));
        assertTrue(json.path("entryInventory").path("entries").get(0)
                .path("signature").path("parameterCount").isNull());
    }

    @Test
    void signatureWithParametersRetainsKnownCountAndPartialAvailability() {
        var port = publish("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. AIR-FIRST.
                DATA DIVISION.
                LINKAGE SECTION.
                01 ARG PIC X.
                PROCEDURE DIVISION USING ARG.
                    GOBACK.
                END PROGRAM AIR-FIRST.
                """);
        var entry = port.entries().get(0);
        assertEquals(Availability.PARTIAL, entry.signature().availability());
        assertEquals(Optional.of(1), entry.signature().parameterCount());
        assertEquals(ReadinessStatus.PARTIAL, entry.readiness().lowering().status());
        assertTrue(entry.gaps().stream().anyMatch(g -> g.code().equals("ENTRY_SIGNATURE_NOT_PROJECTED")));
    }

    @Test
    void alternateEntryRemainsObservedAndDoesNotCloseEntryInventory() {
        var port = publish(program("GOBACK.\nENTRY 'ALT'.\nGOBACK."));
        assertEquals(3, port.statements().size());
        assertEquals(1, port.entries().size());
        assertEquals(InventoryStatus.PARTIAL, port.entryInventory().status());
        assertInstanceOf(ObservedStatement.class, port.statements().get(1));
        assertEquals(2, port.statements().stream().filter(GobackFact.class::isInstance).count());
    }

    @Test
    void primaryStartIsCanonicalAcrossParagraphsAndNamespaceIndependentUnits() {
        String source = program("EMPTY-PARAGRAPH.\nBODY-SECTION SECTION.\nBODY-PARAGRAPH.\nGOBACK.")
                + "\n" + program("GOBACK.").replace("AIR-FIRST", "SECOND");
        var analysis = AstBoundaryTestSupport.analyze(source, "entry-goback.cbl");
        assertEquals(2, analysis.model().programUnits().size());
        var products = products(analysis);
        for (var unit : analysis.model().programUnits()) {
            var port = CobolSemanticProductProjector.open(products, unit.id());
            var entry = port.entries().get(0);
            assertEquals(port.unit(), entry.id().unit());
            var target = entry.start().statement().orElseThrow();
            assertEquals(port.unit(), target.unit());
            assertInstanceOf(GobackFact.class, port.statement(target).orElseThrow());
        }
    }

    @Test
    void allGobacksSurvivePluralInventory() {
        var port = publish(program("GOBACK.\n".repeat(128)));
        assertEquals(128, port.statements().size());
        assertTrue(port.statements().stream().allMatch(GobackFact.class::isInstance));
        assertEquals(128, port.coverage().modeledStatements());
        assertEquals(LocalContinuation.NONE, ((GobackFact) port.statements().get(127)).localContinuation());
    }

    @Test
    void compositionPublishesCanonicalBoundaryFilename(@TempDir Path directory) throws Exception {
        Path copybooks = Files.createDirectory(directory.resolve("copybooks"));
        Path output = directory.resolve("output");
        ExplorerMain.main(new String[]{"--source", FIXTURE.toAbsolutePath().toString(),
                "--copybooks", copybooks.toString(), "--output", output.toString()});
        byte[] canonical = Files.readAllBytes(output.resolve("cobol-semantic-product.json"));
        assertArrayEquals(canonical, Files.readAllBytes(output.resolve("semantic-product.json")));
        JsonNode json = new ObjectMapper().readTree(canonical);
        assertEquals("GOBACK", json.path("statements").get(0).path("variant").asText());
        assertEquals(json.path("statements").get(0).path("header").path("id"),
                json.path("entryInventory").path("entries").get(0).path("start").path("statement"));
    }

    @Test
    void entryDoesNotUseLowestStatementHandle() {
        var port = publish("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. AIR-FIRST.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 X PIC X.
                PROCEDURE DIVISION.
                    GOBACK.
                    MOVE 'A' TO X.
                END PROGRAM AIR-FIRST.
                """);
        assertEquals(0, port.moves().get(0).header().id().localId());
        var target = port.entries().get(0).start().statement().orElseThrow();
        assertNotEquals(0, target.localId());
        assertInstanceOf(GobackFact.class, port.statement(target).orElseThrow());
    }

    @Test
    void missingCanonicalEntryMetadataDoesNotFallBackToRootsOrEmptySignature() {
        var analysis = AstBoundaryTestSupport.analyze(program("GOBACK."), "entry-goback.cbl");
        var unit = analysis.model().programUnits().get(0);
        var program = new Ast.Program(unit.program().meta(), unit.program().name(),
                unit.program().attributes(), unit.program().divisions().stream().map(d ->
                new Ast.Division(d.meta(), d.divisionKind(), d.children())).toList());
        var model = new CompilationUnitModel(analysis.model().compilationUnitId(),
                List.of(new CompilationUnitModel.ProgramUnit(unit.id(), unit.parentId(), program)));
        var build = new CompilationUnitBuildResult(model, analysis.build().coverageByProgramUnit(),
                analysis.build().diagnosticsByProgramUnit());
        var port = CobolSemanticProductProjector.open(new CobolSemanticProductProjector.FrontendProducts(
                build, analysis.tables(), analysis.occurrences(), analysis.resolution(), analysis.report(),
                ScalarMoveSemantics.analyze(build, analysis.tables(), analysis.resolution(), analysis.report())), unit.id());
        assertEquals(1, port.rootStatements().size());
        assertTrue(port.entries().get(0).start().statement().isEmpty());
        assertTrue(port.entries().get(0).signature().parameterCount().isEmpty());
        assertEquals(Availability.UNAVAILABLE, port.entries().get(0).signature().availability());
    }

    @Test
    void incompleteInputDoesNotProveEntryOrZeroParameters() {
        var analysis = AstBoundaryTestSupport.analyze(program("GOBACK."), "entry-goback.cbl");
        var incomplete = new ResolutionAnalysisReport.FrontendState(0, 0, 0,
                List.of(new Diagnostic("COBOL", Diagnostic.Phase.PREPROCESSOR,
                        Diagnostic.Code.UNRESOLVED_COPY, "entry-goback.cbl", 3, 0,
                        "COPY unavailable", "MISSING", "")));
        var report = ResolutionAnalysisReport.compose(analysis.build(), incomplete,
                analysis.occurrences(), analysis.resolution());
        var port = CobolSemanticProductProjector.open(new CobolSemanticProductProjector.FrontendProducts(
                analysis.build(), analysis.tables(), analysis.occurrences(), analysis.resolution(), report,
                ScalarMoveSemantics.analyze(analysis.build(), analysis.tables(), analysis.resolution(), report)),
                analysis.model().programUnits().get(0).id());
        assertInstanceOf(GobackFact.class, port.statements().get(0));
        assertEquals(InventoryStatus.INPUT_MISSING, port.coverage().inventoryStatus());
        assertEquals(InventoryStatus.INPUT_MISSING, port.entryInventory().status());
        var entry = port.entries().get(0);
        assertEquals(Availability.INPUT_MISSING, entry.availability());
        assertTrue(entry.start().statement().isEmpty());
        assertEquals(Availability.INPUT_MISSING, entry.signature().availability());
        assertTrue(entry.signature().parameterCount().isEmpty());
        assertEquals(CoverageStatus.INPUT_MISSING, entry.coverage());
        assertEquals(ReadinessStatus.BLOCKED, entry.readiness().lowering().status());
    }

    @Test
    void nestedUnitKeepsItsOwnEntryAndGoback() {
        var analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. OUTER-PROGRAM.
                PROCEDURE DIVISION.
                    GOBACK.
                IDENTIFICATION DIVISION.
                PROGRAM-ID. INNER-PROGRAM.
                PROCEDURE DIVISION.
                    GOBACK.
                END PROGRAM INNER-PROGRAM.
                END PROGRAM OUTER-PROGRAM.
                """, "nested-entry.cbl");
        assertEquals(2, analysis.model().programUnits().size());
        var ports = analysis.model().programUnits().stream()
                .map(u -> CobolSemanticProductProjector.open(products(analysis), u.id())).toList();
        assertNotEquals(ports.get(0).entries().get(0).id(), ports.get(1).entries().get(0).id());
        assertEquals(List.of(0, 0), ports.get(1).unit().structuralPath());
        for (var port : ports) {
            assertEquals(1, port.statements().size());
            assertEquals(port.unit(), port.entries().get(0).id().unit());
            assertEquals(Optional.of(port.statements().get(0).header().id()),
                    port.entries().get(0).start().statement());
        }
    }

    @Test
    void emptyBodyAndLeadingAlternateEntryDoNotFabricateStart() {
        for (String body : List.of("", "ENTRY 'ALT'.\nGOBACK.")) {
            var port = publish(program(body));
            assertTrue(port.entries().get(0).start().statement().isEmpty());
            assertEquals(ReadinessStatus.BLOCKED, port.entries().get(0).readiness().cfg().status());
        }
    }

    @Test
    void declarativesKeepInventoryAndStartOpen() {
        var port = publish(program("""
                DECLARATIVES.
                DEBUG-SECTION SECTION.
                    USE FOR DEBUGGING ON ALL PROCEDURES.
                DEBUG-PARAGRAPH.
                    CONTINUE.
                END DECLARATIVES.
                    GOBACK.
                """));
        assertTrue(port.entryInventory().gapCodes().contains("DECLARATIVES_NOT_PROJECTED"));
        assertEquals(InventoryStatus.PARTIAL, port.coverage().inventoryStatus());
        assertTrue(port.entries().get(0).start().statement().isEmpty());
        assertEquals(ReadinessStatus.BLOCKED, port.entries().get(0).readiness().cfg().status());
    }

    @Test
    void unknownSignatureCannotBeRepresentedAsZeroOrLoseItsGap() {
        assertThrows(IllegalArgumentException.class, () -> new EntrySignature(
                Availability.UNAVAILABLE, Optional.of(0), ReturningClause.ABSENT));
        var entry = publish("IDENTIFICATION DIVISION.\nPROGRAM-ID. EMPTY-UNIT.")
                .entries().get(0);
        assertThrows(IllegalArgumentException.class, () -> new EntryFact(entry.id(), entry.role(),
                entry.availability(), entry.start(), entry.signature(), entry.provenance(),
                entry.coverage(), entry.readiness(), List.of()));
    }

    @Test
    void independentProbeRejectsDanglingEntryWithoutCoreValidation() {
        var port = publish(program("GOBACK."));
        var entry = port.entries().get(0);
        var dangling = new EntryFact(entry.id(), entry.role(), entry.availability(),
                new ExecutableStart(Availability.KNOWN, Optional.of(new StatementId(port.unit(), 999))),
                entry.signature(), entry.provenance(), entry.coverage(), entry.readiness(), entry.gaps());
        var forged = (CobolSemanticPort) java.lang.reflect.Proxy.newProxyInstance(
                CobolSemanticPort.class.getClassLoader(), new Class<?>[]{CobolSemanticPort.class},
                (proxy, method, args) -> method.getName().equals("entries")
                        ? List.of(dangling) : method.invoke(port, args));
        var result = SemanticPortLoweringProbe.reconstruct(forged);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.code().equals("ENTRY_START_TARGET_MISSING")));
    }

    private static String program(String statements) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. AIR-FIRST.\nPROCEDURE DIVISION.\n"
                + statements + "\nEND PROGRAM AIR-FIRST.\n";
    }

    private static CobolSemanticPort publish(String source) {
        return CobolSemanticPort.open(state(source));
    }

    private static CobolSemanticProduct.State state(String source) {
        var analysis = AstBoundaryTestSupport.analyze(source, "entry-goback.cbl");
        return CobolSemanticProductProjector.project(products(analysis),
                analysis.model().programUnits().get(0).id());
    }

    private static CobolSemanticProductProjector.FrontendProducts products(
            AstBoundaryTestSupport.Analysis analysis) {
        return new CobolSemanticProductProjector.FrontendProducts(analysis.build(),
                analysis.tables(), analysis.occurrences(), analysis.resolution(), analysis.report(),
                ScalarMoveSemantics.analyze(analysis.build(), analysis.tables(), analysis.resolution(), analysis.report()));
    }
}
