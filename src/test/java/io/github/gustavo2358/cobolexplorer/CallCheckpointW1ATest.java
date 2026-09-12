package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Source-derived oracles, independent of the projector and canonical proof engine. */
class CallCheckpointW1ATest {
    static final String DYNAMIC = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. CALLER.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 WS-PGM PIC X(8).
            PROCEDURE DIVISION.
                MOVE 'PROGA' TO WS-PGM.
                CALL WS-PGM.
                GOBACK.
            """;
    static final String LITERAL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. CALLER.
            PROCEDURE DIVISION.
                CALL 'PROGA'.
                GOBACK.
            """;
    static JsonNode publish(String source, String label) throws Exception {
        var port = ScalarMoveCheckpoint4ATest.publish(source);
        byte[] bytes = SemanticProductJsonWriter.serialize(port);
        Path dir = Path.of("target/cp6-w1a/products");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(label + ".cbl"), source);
        Files.write(dir.resolve(label + ".json"), bytes);
        assertArrayEquals(bytes, SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)));
        return new ObjectMapper().readTree(bytes);
    }
    static JsonNode statement(JsonNode doc, String variant) {
        for (var s : doc.path("statements"))
            if (s.path("variant").asText().equals(variant)) return s;
        fail("missing typed " + variant);
        return null;
    }
    static void simpleCall(JsonNode call, JsonNode goback) {
        assertAll("normal return is conditional; clauses absent and effects/outcomes remain open",
            () -> assertEquals("KNOWN", call.path("normalContinuation").path("availability").asText()),
            () -> assertEquals(goback.path("header").path("id"), call.path("normalContinuation").path("statement")),
            () -> assertEquals("ABSENT", call.path("surface").path("using").asText()),
            () -> assertEquals(0, call.path("surface").path("argumentCount").asInt(-1)),
            () -> assertEquals("ABSENT", call.path("surface").path("returning").asText()),
            () -> assertEquals("ABSENT", call.path("surface").path("onException").asText()),
            () -> assertEquals("ABSENT", call.path("surface").path("notOnException").asText()),
            () -> assertEquals("ABSENT", call.path("surface").path("onOverflow").asText()),
            () -> assertEquals("UNKNOWN", call.path("effects").asText()),
            () -> assertEquals("OPEN", call.path("outcomes").asText()),
            () -> assertEquals("SUFFICIENT", call.path("header").path("readiness").path("lowering").path("status").asText()));
    }
    @Test void dynamicX8() throws Exception {
        var doc = publish(DYNAMIC, "dynamic-x8");
        var call = statement(doc, "CALL");
        var move = statement(doc, "MOVE");
        var dataId = doc.path("dataDeclarations").get(0).path("id");
        assertAll("X8 independent source oracle",
            () -> simpleCall(call, statement(doc, "GOBACK")),
            () -> assertEquals("DATA", call.path("target").path("kind").asText()),
            () -> assertEquals(dataId, call.path("target").path("reference").path("binding").path("selected")),
            () -> assertEquals(dataId, call.path("target").path("reference").path("wholeItemAccess").path("data")),
            () -> assertEquals("UNKNOWN", call.path("runtimeTarget").asText()),
            () -> assertEquals("PROGA", move.path("source").path("value").asText()),
            () -> assertEquals("RIGHT_PAD_SPACE", move.path("textAdjustment").path("rule").asText()),
            () -> assertEquals(8, move.path("textAdjustment").path("receiverExtent").asInt()),
            () -> assertEquals("PROGA   ", move.path("textAdjustment").path("result").path("value").asText()),
            () -> assertTrue(move.path("textAdjustment").path("provenance").isObject()),
            () -> assertTrue(call.path("target").path("reference").path("provenance").isObject()));
    }
    @Test void literalCall() throws Exception {
        var doc = publish(LITERAL, "literal");
        var call = statement(doc, "CALL");
        simpleCall(call, statement(doc, "GOBACK"));
        assertEquals("LITERAL", call.path("target").path("kind").asText());
        assertEquals("PROGA", call.path("target").path("text").asText());
        assertEquals("'PROGA'", call.path("target").path("writtenText").asText());
        assertTrue(call.path("target").path("provenance").path("exact").asBoolean());
        assertNotEquals(call.path("header").path("provenance"), call.path("target").path("provenance"));
    }
    static void outsideSlice(JsonNode call) {
        assertNotEquals("SUFFICIENT", call.path("header").path("readiness").path("lowering").path("status").asText());
        assertEquals("UNKNOWN", call.path("effects").asText());
        assertEquals("OPEN", call.path("outcomes").asText());
    }
    @Test void clausesRemainDistinct() throws Exception {
        for (var c : List.of(new String[]{"USING WS-PGM", "using"},
                new String[]{"RETURNING WS-PGM", "returning"},
                new String[]{"GIVING WS-PGM", "returning"},
                new String[]{"ON EXCEPTION CONTINUE END-CALL", "onException"},
                new String[]{"NOT ON EXCEPTION CONTINUE END-CALL", "notOnException"},
                new String[]{"ON OVERFLOW CONTINUE END-CALL", "onOverflow"})) {
            var doc = publish(DYNAMIC.replace("CALL WS-PGM.", "CALL WS-PGM " + c[0] + "."), c[1]);
            var call = statement(doc, "CALL");
            assertEquals("PRESENT", call.path("surface").path(c[1]).asText(), c[0]);
            if (c[1].equals("using")) assertEquals(1, call.path("surface").path("argumentCount").asInt());
            outsideSlice(call);
        }
    }
    @Test void accessAndBindingsAreNeverInvented() throws Exception {
        for (String target : List.of("WS-PGM(1:5)", "WS-PGM(1)", "MISSING")) {
            var call = statement(publish(DYNAMIC.replace("CALL WS-PGM", "CALL " + target),
                    "nonwhole-" + target.replaceAll("[^A-Z]", "")), "CALL");
            assertEquals("DATA", call.path("target").path("kind").asText());
            assertTrue(call.path("target").path("reference").path("wholeItemAccess").isNull());
            outsideSlice(call);
        }
        var call = statement(publish(DYNAMIC.replace("01 WS-PGM PIC X(8).",
                "01 WS-PGM PIC X(8).\n01 WS-PGM PIC X(8)."), "ambiguous"), "CALL");
        var binding = call.path("target").path("reference").path("binding");
        assertEquals("AMBIGUOUS", binding.path("status").asText());
        assertEquals("MULTIPLE_VALID_CANDIDATES", binding.path("reason").asText());
        assertEquals(2, binding.path("candidates").size());
        assertTrue(binding.path("selected").isNull());
        outsideSlice(call);
    }
    @Test void callValueDoesNotFollowPreviousMoves() throws Exception {
        for (String source : List.of(DYNAMIC, DYNAMIC.replace("'PROGA'", "'OTHER'"),
                DYNAMIC.replace("    MOVE 'PROGA' TO WS-PGM.\n", ""))) {
            var call = statement(publish(source, "runtime-unknown"), "CALL");
            assertEquals("UNKNOWN", call.path("runtimeTarget").asText());
            assertFalse(call.path("target").has("text"));
            assertFalse(call.has("programReferenceName"));
        }
    }
    @Test void identityAndFittingHaveDifferentProofs() throws Exception {
        var x5 = statement(publish(DYNAMIC.replace("X(8)", "X(5)"), "dynamic-x5"), "MOVE");
        assertEquals("FULL_IDENTITY", x5.path("copySemantics").asText());
        assertTrue(x5.path("textAdjustment").isNull());
        assertEquals("PROGA", x5.path("source").path("logicalValue").path("value").asText());
        var x8 = statement(publish(DYNAMIC, "dynamic-x8"), "MOVE");
        assertEquals("FITTED_TEXT", x8.path("copySemantics").asText());
        var shorter = statement(publish(DYNAMIC.replace("X(8)", "X(3)"), "truncation-excluded"), "MOVE");
        assertEquals("UNAVAILABLE", shorter.path("copySemantics").asText());
        assertTrue(shorter.path("textAdjustment").isNull());
    }
    @Test void continuationIsCanonicalAndDoesNotCrossRegions() throws Exception {
        for (String source : List.of(DYNAMIC.replace("    GOBACK.", "NEXT-PARA.\n    GOBACK."),
                DYNAMIC.replace("    GOBACK.\n", ""))) {
            var call = statement(publish(source, "continuation-unavailable"), "CALL");
            assertEquals("UNAVAILABLE", call.path("normalContinuation").path("availability").asText());
            assertTrue(call.path("normalContinuation").path("statement").isNull());
            outsideSlice(call);
        }
    }
    @Test void nestedCallHasItsProvenArmCompletion() throws Exception {
        var sp=publish(DYNAMIC.replace("    CALL WS-PGM.", "    IF WS-PGM = 'PROGA' CALL WS-PGM END-IF."),"nested-call");
        var call=statement(sp,"CALL");var end=statement(sp,"GOBACK");
        assertEquals("KNOWN",call.path("normalContinuation").path("availability").asText());
        assertEquals(end.path("header").path("id"),call.path("normalContinuation").path("statement"));
    }
    @Test void literalValuePreservesCaseSpacesAndEscapes() throws Exception {
        for (var c : List.of(new String[]{"'proGa   '", "proGa   "}, new String[]{"'AB''CD'", "AB'CD"})) {
            var call = statement(publish(LITERAL.replace("'PROGA'", c[0]), "literal-spelling"), "CALL");
            assertEquals(c[1], call.path("target").path("text").asText());
            assertEquals(c[0], call.path("target").path("writtenText").asText());
        }
    }
    @Test void incompleteInputPublishesUnknownSurface() throws Exception {
        var a = AstBoundaryTestSupport.analyze(DYNAMIC, "scalar.cbl");
        var missing = new ResolutionAnalysisReport.FrontendState(0, 0, 0, List.of(new Diagnostic("COBOL",
                Diagnostic.Phase.PREPROCESSOR, Diagnostic.Code.UNRESOLVED_COPY, "scalar.cbl", 3, 0,
                "COPY unavailable", "MISSING", "")));
        var report = ResolutionAnalysisReport.compose(a.build(), missing, a.occurrences(), a.resolution());
        var semantics = ScalarMoveSemantics.analyze(a.build(), a.tables(), a.resolution(), report);
        var port = io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.open(
                new io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.FrontendProducts(
                        a.build(), a.tables(), a.occurrences(), a.resolution(), report, semantics),
                a.model().programUnits().get(0).id());
        var doc = new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(port));
        var call = statement(doc, "CALL");
        for (String field : List.of("using", "returning", "onException", "notOnException", "onOverflow"))
            assertEquals("UNKNOWN", call.path("surface").path(field).asText());
        assertTrue(call.path("surface").path("argumentCount").isNull());
        assertTrue(call.path("target").path("reference").path("wholeItemAccess").isNull());
        assertEquals("UNAVAILABLE", call.path("normalContinuation").path("availability").asText());
        outsideSlice(call);
    }
    @Test void storageAndNonbasicLiteralsFailClosed() throws Exception {
        for (String declaration : List.of("01 WS-PGM PIC X(8) EXTERNAL.", "01 WS-PGM PIC X(8) OCCURS 2.",
                "01 WS-PGM PIC X(8).\n01 ALIAS-PGM REDEFINES WS-PGM PIC X(8).")) {
            var call = statement(publish(DYNAMIC.replace("01 WS-PGM PIC X(8).", declaration), "excluded-storage"), "CALL");
            assertTrue(call.path("target").path("reference").path("wholeItemAccess").isNull());
            outsideSlice(call);
        }
        for (String literal : List.of("N'PROGA'", "X'414243'")) {
            var call = statement(publish(LITERAL.replace("'PROGA'", literal), "nonbasic-literal"), "CALL");
            assertEquals("LITERAL", call.path("target").path("kind").asText());
            assertTrue(call.path("target").path("logicalValue").isNull());
            outsideSlice(call);
        }
    }
    @Test void nestedHandlerCallDoesNotDonateArgumentsToItsOwner() throws Exception {
        var doc = publish(DYNAMIC.replace("CALL WS-PGM.",
                "CALL WS-PGM ON EXCEPTION\n CALL WS-PGM USING WS-PGM END-CALL\n END-CALL."), "nested-handler");
        var calls = new java.util.ArrayList<JsonNode>();
        for (var s : doc.path("statements")) if (s.path("variant").asText().equals("CALL")) calls.add(s);
        assertEquals(2, calls.size());
        var outer = calls.get(0);
        var inner = calls.get(1);
        assertEquals("ABSENT", outer.path("surface").path("using").asText());
        assertEquals(0, outer.path("surface").path("argumentCount").asInt(-1));
        assertEquals("PRESENT", inner.path("surface").path("using").asText());
        assertEquals(1, inner.path("surface").path("argumentCount").asInt());
        assertEquals("PRESENT", outer.path("surface").path("onException").asText());
        assertEquals("ABSENT", inner.path("surface").path("onException").asText());
        outsideSlice(outer);
        outsideSlice(inner);
        assertNotEquals(outer.path("target").path("reference").path("id"), inner.path("target").path("reference").path("id"));
    }
}
