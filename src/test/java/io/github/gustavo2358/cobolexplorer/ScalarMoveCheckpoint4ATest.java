package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.LiteralSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import io.github.gustavo2358.cobolexplorer.semanticproduct.scalar.ScalarMoveOracle;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScalarMoveCheckpoint4ATest {
    static final Path FIXTURE = Path.of("src/test/resources/cobol/semantic/AIR-MOVE.cbl");
    static String program(String data, String statements) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. SAMPLE.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n"
                + data + "\nPROCEDURE DIVISION.\n" + statements + "\nEND PROGRAM SAMPLE.\n";
    }
    static CobolSemanticProductProjector.FrontendProducts products(AstBoundaryTestSupport.Analysis a) {
        return new CobolSemanticProductProjector.FrontendProducts(a.build(), a.tables(), a.occurrences(), a.resolution(), a.report(),
                ScalarMoveSemantics.analyze(a.build(), a.tables(), a.resolution(), a.report()));
    }
    static CobolSemanticPort publish(String source) {
        var a = AstBoundaryTestSupport.analyze(source, "scalar.cbl");
        return CobolSemanticProductProjector.open(products(a), a.model().programUnits().get(0).id());
    }
    @Test void positivePublicOracleAndJson() throws Exception {
        String source = Files.readString(FIXTURE);
        var port = publish(source);
        assertEquals(1, port.dataDeclarations().size());
        assertEquals(2, port.statements().size());
        var move = port.moves().get(0);
        var definition = ScalarMoveOracle.read(port, move.header().id());
        assertEquals("PROGA", definition.value());
        assertEquals(5, definition.extent());
        assertEquals(port.dataDeclarations().get(0).id(), definition.destination());
        assertEquals(CoverageStatus.MODELED, move.header().coverage());
        assertFalse(port.gaps().stream().anyMatch(g -> g.code().equals("LITERAL_KIND_NOT_PUBLISHED")));
        assertEquals(ReadinessStatus.BLOCKED, port.coverage().readiness().effectsDataflow().status());
        assertTrue(port.entryInventory().gapCodes().contains("ALTERNATE_ENTRIES_NOT_PROJECTED"));
        assertEquals(6, port.dataDeclarations().get(0).provenance().original().startLine());
        assertEquals(9, move.source().provenance().original().startLine());
        assertNotEquals(move.source().provenance(), move.target().provenance());
        byte[] json = SemanticProductJsonWriter.serialize(port);
        assertArrayEquals(json, SemanticProductJsonWriter.serialize(publish(source)));
        assertJson(new ObjectMapper().readTree(json));
        Path output = Path.of("target/checkpoint-4a"); Files.createDirectories(output);
        Files.write(output.resolve("AIR-MOVE.semantic-product.json"), json);
        System.out.println("CP4A small JSON bytes=" + json.length);
    }
    // JSON-only assertions have no frontend joins and deliberately erase textual readiness.
    static void assertJson(JsonNode doc) {
        assertEquals("1.6.0", doc.path("contractVersion").asText());
        var data = doc.path("dataDeclarations").get(0);
        var statements = doc.path("statements");
        JsonNode move = null, goback = null;
        for (var s : statements) {
            if (s.path("variant").asText().equals("MOVE")) move = s;
            if (s.path("variant").asText().equals("GOBACK")) goback = s;
        }
        assertNotNull(move); assertNotNull(goback);
        assertEquals("TEXT", data.path("scalarText").path("logicalDomain").asText());
        assertEquals(5, data.path("scalarText").path("logicalExtent").asInt());
        assertEquals("WORKING_STORAGE", data.path("scalarText").path("storageClass").asText());
        assertEquals("ALPHANUMERIC", move.path("source").path("kind").asText());
        assertEquals("TEXT", move.path("source").path("logicalValue").path("logicalDomain").asText());
        assertEquals("PROGA", move.path("source").path("logicalValue").path("value").asText());
        assertEquals(data.path("id"), move.path("target").path("wholeItemAccess").path("data"));
        assertEquals(data.path("id"), move.path("target").path("binding").path("selected"));
        assertEquals("FULL_IDENTITY", move.path("copySemantics").asText());
        assertEquals("KNOWN", move.path("normalContinuation").path("availability").asText());
        assertEquals(goback.path("header").path("id"), move.path("normalContinuation").path("statement"));
        assertEquals("NONE", goback.path("localContinuation").asText());
        assertEquals("CURRENT_PROGRAM_INVOCATION", goback.path("exit").asText());
        assertTrue(move.path("source").path("provenance").isObject());
        assertTrue(move.path("target").path("provenance").isObject());
    }
    @Test void mismatchesArePreservedWithoutIdentity() {
        for (var pair : List.of(List.of("5", "ABC"), List.of("3", "ABCDE"))) {
            var port = publish(program("01 WS-X PIC X(" + pair.get(0) + ").", "MOVE '" + pair.get(1) + "' TO WS-X.\nGOBACK."));
            assertEquals(1, port.moves().size());
            var move = port.moves().get(0);
            assertTrue(move.target().wholeItemAccess().isPresent());
            assertNotEquals(CopySemantics.FULL_IDENTITY, move.copySemantics());
            if (pair.get(0).equals("5")) {
                assertEquals(CopySemantics.FITTED_TEXT, move.copySemantics());
                assertEquals("ABC  ", move.textAdjustment().orElseThrow().result().value());
                assertEquals(CoverageStatus.MODELED, move.header().coverage());
            } else {
                assertEquals(CopySemantics.UNAVAILABLE, move.copySemantics());
                assertEquals(CoverageStatus.PARTIAL, move.header().coverage());
                assertTrue(port.gaps().stream().anyMatch(g -> g.code().equals("MOVE_IDENTITY_NOT_PROVEN")));
            }
        }
    }
    @Test void categoriesDoNotFollowJavaStringOrQuotedPrefixes() {
        for (String literal : List.of("12345", "SPACE", "ZERO", "N'PROGA'", "X'4142434445'", "Z'PROGA'")) {
            var move = publish(program("01 WS-X PIC X(5).", "MOVE " + literal + " TO WS-X.\nGOBACK.")).moves().get(0);
            assertEquals(LiteralKind.UNKNOWN, ((LiteralSource) move.source()).kind(), literal);
            assertTrue(((LiteralSource) move.source()).logicalValue().isEmpty(), literal);
            assertEquals(CopySemantics.UNAVAILABLE, move.copySemantics(), literal);
        }
        var port = publish(program("01 WS-X PIC 9(5).", "MOVE 'PROGA' TO WS-X.\nGOBACK."));
        assertTrue(port.dataDeclarations().get(0).scalarText().isEmpty());
        assertEquals(CopySemantics.UNAVAILABLE, port.moves().get(0).copySemantics());
    }
    @Test void excludedStorageAndAccessNeverBecomeScalarWhole() {
        List<String[]> cases = List.of(
                new String[]{"01 WS-X PIC X(5) OCCURS 3.", "WS-X"},
                new String[]{"01 TAB OCCURS 3.\n 05 WS-X PIC X(5).", "WS-X(1)"},
                new String[]{"01 WS-X PIC X(5).\n01 WS-OTHER REDEFINES WS-X PIC X(5).", "WS-X"},
                new String[]{"01 WS-OTHER PIC X(5).\n01 WS-X REDEFINES WS-OTHER PIC X(5).", "WS-X"},
                new String[]{"01 GROUP-X.\n05 WS-X PIC X(5).\n66 ALIAS-X RENAMES WS-X.", "WS-X"},
                new String[]{"01 WS-X.\n05 CHILD-X PIC X(5).", "WS-X"},
                new String[]{"01 WS-X PIC X(5).", "WS-X(1:5)"},
                new String[]{"01 WS-X PIC X(5).", "WS-X(1)"},
                new String[]{"01 WS-X PIC X(5) EXTERNAL.", "WS-X"},
                new String[]{"01 WS-X PIC X(5) GLOBAL.", "WS-X"},
                new String[]{"01 WS-X PIC X(5) JUSTIFIED RIGHT.", "WS-X"});
        for (var c : cases) {
            var port = publish(program(c[0], "MOVE 'PROGA' TO " + c[1] + ".\nGOBACK."));
            assertEquals(1, port.moves().size(), Arrays.toString(c));
            assertTrue(port.moves().get(0).target().wholeItemAccess().isEmpty(), Arrays.toString(c));
            assertEquals(CopySemantics.UNAVAILABLE, port.moves().get(0).copySemantics(), Arrays.toString(c));
        }
        for (var storage : List.of("LINKAGE", "LOCAL-STORAGE")) {
            var port = publish(program("01 WS-X PIC X(5).", "MOVE 'PROGA' TO WS-X.\nGOBACK.").replace("WORKING-STORAGE", storage));
            assertTrue(port.dataDeclarations().get(0).scalarText().isEmpty());
        }
    }
    @Test void ambiguousAndMissingBindingCannotSelect() {
        for (String data : List.of("01 WS-X PIC X(5).\n01 WS-X PIC X(5).", "01 WS-OTHER PIC X(5).")) {
            var move = publish(program(data, "MOVE 'PROGA' TO WS-X.\nGOBACK.")).moves().get(0);
            assertTrue(move.target().binding().selected().isEmpty());
            assertTrue(move.target().wholeItemAccess().isEmpty());
            assertEquals(CopySemantics.UNAVAILABLE, move.copySemantics());
        }
    }
    @Test void basicLiteralNormalizationAndRenamingAreSemantic() {
        for (String name : List.of("WS-PGM", "WS-TARGET", "RENAMED-ITEM")) {
            var port = publish(program("01 " + name + " PIC X(5) USAGE DISPLAY.", "MOVE 'ABCDE' TO " + name + ".\nGOBACK."));
            assertEquals("ABCDE", ScalarMoveOracle.read(port, port.moves().get(0).header().id()).value());
        }
        for (String literal : List.of("'AB''CD'", "\"AB\"\"CD\"")) {
            var port = publish(program("01 WS-X PIC XXXXX.", "MOVE " + literal + " TO WS-X.\nGOBACK."));
            assertEquals(5, ScalarMoveOracle.read(port, port.moves().get(0).header().id()).extent());
        }
    }
    @Test void unavailableContinuationNeverUsesPhysicalOrArrayOrder() {
        for (String body : List.of("MOVE 'PROGA' TO WS-X.",
                "MOVE 'PROGA' TO WS-X.\nNEXT-PARAGRAPH.\nGOBACK.")) {
            var port = publish(program("01 WS-X PIC X(5).", body));
            var next = port.moves().get(0).normalContinuation();
            assertEquals(ContinuationAvailability.UNAVAILABLE, next.availability());
            assertTrue(next.statement().isEmpty());
        }
        // SP 1.4 explicitly closes the formerly unavailable direct IF arm completion.
        var nested = publish(program("01 WS-X PIC X(5).", "IF WS-X = 'PROGA' MOVE 'PROGA' TO WS-X END-IF.\nGOBACK."));
        assertEquals(nested.statements().get(2).header().id(), nested.moves().get(0).normalContinuation().statement().orElseThrow());
        var port = publish(program("01 WS-X PIC X(5).", "GOBACK.\nMOVE 'PROGA' TO WS-X.\nGOBACK."));
        assertEquals(LocalContinuation.NONE, ((GobackFact) port.statements().get(0)).localContinuation());
        assertEquals(port.statements().get(2).header().id(), port.moves().get(0).normalContinuation().statement().orElseThrow());
    }
    @Test void absentCanonicalMetadataCannotBeRebuiltByProjector() {
        var a = AstBoundaryTestSupport.analyze(program("01 WS-X PIC X(5).", "MOVE 'PROGA' TO WS-X.\nGOBACK."), "scalar.cbl");
        var u = a.model().programUnits().get(0);
        var altered = new Ast.Program(u.program().meta(), u.program().name(), u.program().attributes(),
                u.program().divisions().stream().map(d -> new Ast.Division(d.meta(), d.divisionKind(), d.children(), d.procedureEntry())).toList());
        var model = new CompilationUnitModel(a.model().compilationUnitId(), List.of(new CompilationUnitModel.ProgramUnit(u.id(), u.parentId(), altered)));
        var build = new CompilationUnitBuildResult(model, a.build().coverageByProgramUnit(), a.build().diagnosticsByProgramUnit());
        var port = CobolSemanticProductProjector.open(new CobolSemanticProductProjector.FrontendProducts(build, a.tables(), a.occurrences(), a.resolution(), a.report(),
                ScalarMoveSemantics.analyze(build, a.tables(), a.resolution(), a.report())), u.id());
        assertEquals(2, port.statements().size());
        assertEquals(ContinuationAvailability.UNAVAILABLE, port.moves().get(0).normalContinuation().availability());
    }
    @Test void noReadinessOrCollectionOrderingIsNeededByPublicOracle() throws Exception {
        var port = publish(Files.readString(FIXTURE));
        var move = port.moves().get(0);
        var stripped = new Readiness(new ReadinessClaim(ReadinessStatus.PARTIAL, "omitted"),
                new ReadinessClaim(ReadinessStatus.PARTIAL, "omitted"),
                new ReadinessClaim(ReadinessStatus.PARTIAL, "omitted"));
        var h = move.header();
        var withoutDescriptions = new MoveFact(new StatementHeader(h.id(), h.point(), h.containment(),
                h.provenance(), h.coverage(), stripped), move.source(), move.target(), move.copySemantics(), move.normalContinuation());
        var reversed = new ArrayList<>(port.statements()); Collections.reverse(reversed);
        var proxy = (CobolSemanticPort) java.lang.reflect.Proxy.newProxyInstance(
                CobolSemanticPort.class.getClassLoader(), new Class<?>[]{CobolSemanticPort.class},
                (self, method, args) -> {
                    if (method.getName().equals("statements")) return reversed;
                    if (method.getName().equals("statement") && args[0].equals(h.id())) return Optional.of(withoutDescriptions);
                    return method.invoke(port, args);
                });
        assertEquals("PROGA", ScalarMoveOracle.read(proxy, h.id()).value());
        var json = new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(port));
        eraseDescriptions(json);
        assertJson(json);
    }
    private static void eraseDescriptions(JsonNode node) {
        if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {
            object.remove(List.of("readiness", "scope", "detail"));
        }
        for (var child : node) eraseDescriptions(child);
    }
    @Test void legacyConstructorsDoNotCreateScalarOrExecutableProofs() throws Exception {
        var port = publish(Files.readString(FIXTURE));
        var d = port.dataDeclarations().get(0);
        assertTrue(new DataDeclaration(d.id(), d.canonicalName(), d.picture(), d.provenance(), d.coverage(), d.readiness()).scalarText().isEmpty());
        var move = port.moves().get(0);
        var source = new LiteralSource(move.source().id(), LiteralKind.UNKNOWN, "PROGA", move.source().provenance());
        var target = new DataReference(move.target().id(), OperandRole.WRITE, move.target().binding(), move.target().provenance());
        var legacy = new MoveFact(move.header(), source, target);
        assertTrue(source.logicalValue().isEmpty());
        assertTrue(target.wholeItemAccess().isEmpty());
        assertEquals(CopySemantics.UNAVAILABLE, legacy.copySemantics());
        assertEquals(ContinuationAvailability.UNAVAILABLE, legacy.normalContinuation().availability());
    }
    @Test void publicInvariantsRejectContradictoryProofs() throws Exception {
        var port = publish(Files.readString(FIXTURE));
        var move = port.moves().get(0);
        assertThrows(IllegalArgumentException.class, () -> new ScalarText(0));
        assertThrows(IllegalArgumentException.class, () -> new LiteralSource(move.source().id(), LiteralKind.UNKNOWN,
                "PROGA", move.source().provenance(), Optional.of(new TextValue("PROGA"))));
        assertThrows(IllegalArgumentException.class, () -> new DataReference(move.target().id(), OperandRole.WRITE,
                move.target().binding(), move.target().provenance(), Optional.of(new WholeItemAccess(new DataItemId(port.unit(), 99)))));
        assertThrows(IllegalArgumentException.class, () -> new NormalContinuation(ContinuationAvailability.KNOWN,
                Optional.empty(), move.header().provenance()));
        var shortened = new LiteralSource(move.source().id(), LiteralKind.ALPHANUMERIC, "ABC", move.source().provenance(),
                Optional.of(new TextValue("ABC")));
        var invalid = new MoveFact(move.header(), shortened, move.target(), CopySemantics.FULL_IDENTITY, move.normalContinuation());
        var statements = List.<StatementFact>of(invalid, port.statements().get(1));
        assertThrows(IllegalArgumentException.class, () -> new State(port.unit(), port.policy(), port.dataDeclarations(),
                statements, port.gaps(), port.coverage(), port.entryInventory()));
        var none = new NormalContinuation(ContinuationAvailability.NONE, Optional.empty(), move.header().provenance());
        assertTrue(none.statement().isEmpty()); // Representable; never inferred from physical end by this profile.
    }
    @Test void inputIncompleteDoesNotProveWholeCopyOrContinuation() {
        var a = AstBoundaryTestSupport.analyze(program("01 WS-X PIC X(5).", "MOVE 'PROGA' TO WS-X.\nGOBACK."), "scalar.cbl");
        var incomplete = new ResolutionAnalysisReport.FrontendState(0, 0, 0, List.of(new Diagnostic("COBOL",
                Diagnostic.Phase.PREPROCESSOR, Diagnostic.Code.UNRESOLVED_COPY, "scalar.cbl", 3, 0,
                "COPY unavailable", "MISSING", "")));
        var report = ResolutionAnalysisReport.compose(a.build(), incomplete, a.occurrences(), a.resolution());
        var semantics = ScalarMoveSemantics.analyze(a.build(), a.tables(), a.resolution(), report);
        var port = CobolSemanticProductProjector.open(new CobolSemanticProductProjector.FrontendProducts(
                a.build(), a.tables(), a.occurrences(), a.resolution(), report, semantics), a.model().programUnits().get(0).id());
        assertTrue(port.dataDeclarations().get(0).scalarText().isEmpty());
        assertTrue(port.moves().get(0).target().wholeItemAccess().isEmpty());
        assertEquals(CopySemantics.UNAVAILABLE, port.moves().get(0).copySemantics());
        assertEquals(ContinuationAvailability.UNAVAILABLE, port.moves().get(0).normalContinuation().availability());
    }

    @Test void baselineCp3PayloadRemainsCompatibleAcrossMinorVersion() throws Exception {
        var mapper = new ObjectMapper();
        var previous = mapper.readTree(Path.of("src/test/resources/cobol/semantic/entry-goback-sp-1.1.0.json").toFile());
        assertEquals("1.1.0", previous.path("contractVersion").asText());
        var fixture = Path.of("src/test/resources/cobol/semantic/semantic-product-entry-goback.cbl");
        var a = AstBoundaryTestSupport.analyze(Files.readString(fixture), fixture.getFileName().toString());
        var port = CobolSemanticProductProjector.open(products(a), a.model().programUnits().get(0).id());
        var current = mapper.readTree(SemanticProductJsonWriter.serialize(port));
        assertEquals("1.6.0", current.path("contractVersion").asText());
        assertEquals("GOBACK", previous.path("statements").get(0).path("variant").asText());
        assertEquals("NONE", previous.path("statements").get(0).path("localContinuation").asText());
        ((com.fasterxml.jackson.databind.node.ObjectNode) previous).remove("contractVersion");
        assertEquals("UNAVAILABLE", current.path("storageIndependence").path("availability").asText());
        ((com.fasterxml.jackson.databind.node.ObjectNode) current).remove("storageIndependence");
        ((com.fasterxml.jackson.databind.node.ObjectNode) current).remove("contractVersion");
        assertEquals(previous, current, "all CP3 facts, provenance, coverage and gaps must be unchanged");
    }

}
