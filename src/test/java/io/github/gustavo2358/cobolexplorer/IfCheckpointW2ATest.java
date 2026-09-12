package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Behavioral public JSON oracles: missing capabilities fail at runtime, not compilation. */
class IfCheckpointW2ATest {
    static String fixture(String name) throws Exception {
        return Files.readString(Path.of("src/test/resources/cobol/semantic/cp6-w2a-" + name + ".cbl"))
                .lines().map(line -> line.substring(7)).collect(java.util.stream.Collectors.joining("\n")) + "\n";
    }
    static JsonNode publish(String source, String name) throws Exception {
        byte[] bytes = SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source));
        Path out = Path.of("target/cp6-w2a/products"); Files.createDirectories(out);
        Files.write(out.resolve(name + ".json"), bytes);
        return new ObjectMapper().readTree(bytes);
    }
    static JsonNode doc(String name) throws Exception { return publish(fixture(name), name); }
    static List<JsonNode> statements(JsonNode doc, String kind) {
        List<JsonNode> result = new ArrayList<>();
        doc.path("statements").forEach(s -> { if (s.path("variant").asText().equals(kind)) result.add(s); });
        return result;
    }
    static JsonNode one(JsonNode doc, String kind) { return statements(doc, kind).get(0); }
    static JsonNode id(JsonNode statement) { return statement.path("header").path("id"); }
    static void completion(JsonNode statement, JsonNode next) {
        assertEquals("KNOWN", statement.path("normalContinuation").path("availability").asText(), statement.toString());
        assertEquals(id(next), statement.path("normalContinuation").path("statement"));
    }
    @Test void redAClosedCompletion() throws Exception {
        var d = doc("closed"); var call = one(d, "CALL");
        var moves = statements(d, "MOVE");
        assertAll(() -> completion(moves.get(0), call), () -> completion(moves.get(1), call),
                () -> completion(one(d, "IF"), call), () -> completion(call, one(d, "GOBACK")));
    }
    @Test void redBPredicateGuarantee() throws Exception {
        var d = doc("closed"); var condition = one(d, "IF").path("condition");
        var p = condition.path("predicate");
        assertAll(() -> assertEquals("KNOWN", p.path("availability").asText()),
                () -> assertEquals("BOOLEAN", p.path("resultDomain").asText()),
                () -> assertEquals("PURE", p.path("evaluation").asText()),
                () -> assertEquals("TOTAL", p.path("normalCompletion").asText()),
                () -> assertEquals("COMPLETE", p.path("readsCompleteness").asText()),
                () -> assertEquals("UNKNOWN", p.path("truthValue").asText()),
                () -> assertEquals(1, p.path("knownReads").size()));
        var ref = condition.path("references").get(0);
        assertEquals(ref.path("id"), p.path("knownReads").get(0));
        assertEquals(ref.path("binding").path("selected"), ref.path("wholeItemAccess").path("data"));
        assertTrue(p.path("provenance").path("exact").asBoolean());
        assertTrue(condition.path("provenance").path("exact").asBoolean());
        assertTrue(ref.path("provenance").path("exact").asBoolean());
        assertEquals("FLAG", d.path("dataDeclarations").get(0).path("canonicalName").asText());
        assertEquals(d.path("dataDeclarations").get(0).path("id"), ref.path("binding").path("selected"));
    }
    @Test void redCElsePresenceIsNotEmptyChildren() throws Exception {
        var open = one(doc("open"), "IF"); var empty = one(doc("empty"), "IF");
        assertAll(() -> assertEquals("ABSENT", open.path("elseArm").path("presence").asText()),
                () -> assertEquals("PRESENT", empty.path("elseArm").path("presence").asText()),
                () -> assertEquals("KNOWN", open.path("elseArm").path("contentAvailability").asText()),
                () -> assertEquals("PARTIAL", empty.path("elseArm").path("contentAvailability").asText()));
    }
    @Test void redDStorageIndependence() throws Exception {
        var d = doc("closed"); var proof = d.path("storageIndependence");
        assertEquals("KNOWN", proof.path("availability").asText());
        assertEquals("INDEPENDENT_WORKING_STORAGE_ROOTS", proof.path("rule").asText());
        var ids = new HashSet<JsonNode>(); d.path("dataDeclarations").forEach(x -> ids.add(x.path("id")));
        var members = new HashSet<JsonNode>(); proof.path("members").forEach(members::add);
        assertEquals(2, members.size()); assertEquals(ids, members);
        assertTrue(proof.path("provenance").path("exact").asBoolean());
    }
    @Test void canonicalInputs() throws Exception {
        var a = AstBoundaryTestSupport.analyze(fixture("closed"), "closed.cbl");
        var relation = AstBoundaryTestSupport.nodes(a, Ast.RelationCondition.class).get(0);
        assertEquals(Ast.RelationOperator.EQUAL, relation.operatorKind());
        assertInstanceOf(Ast.DataReference.class, relation.subject());
        assertTrue(((Ast.LiteralExpression) relation.object()).logicalText().isPresent());
        assertEquals(1, a.resolution().entries().stream().filter(e -> e.occurrence().role() == ResolutionContracts.ReferenceRole.VALUE_READ).count());
    }

    @Test void closedAndOpenAreUsableByBoundaryOnlyOracle() throws Exception {
        for (var name : List.of("closed", "open")) {
            var port = ScalarMoveCheckpoint4ATest.publish(fixture(name));
            var fact = port.ifs().get(0);
            var result = io.github.gustavo2358.cobolexplorer.semanticproduct.ifprofile.IfFactsOracle.read(port, fact.header().id());
            assertEquals(port.calls().get(0).header().id(), result.completion());
            assertEquals(port.moves().get(0).header().id(), result.thenEntry());
            assertEquals(name.equals("closed"), result.elseEntry().isPresent());
            assertEquals("PROGA   ", port.moves().get(0).textAdjustment().orElseThrow().result().value());
            if (name.equals("closed")) {
                assertEquals("PROGB   ", port.moves().get(1).textAdjustment().orElseThrow().result().value());
                assertEquals(port.moves().get(1).header().id(), result.elseEntry().orElseThrow());
            }
            assertEquals(2, result.independent().size());
            var bytes = SemanticProductJsonWriter.serialize(port);
            assertArrayEquals(bytes, SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(fixture(name))));
            assertEquals("1.8.0", new ObjectMapper().readTree(bytes).path("contractVersion").asText());
        }
    }
    @Test void multipleStatementsInEachArmFollowDirectRelations() throws Exception {
        var source = fixture("closed").replace("MOVE 'PROGA' TO WS-PGM", "MOVE 'FIRST' TO WS-PGM\n MOVE 'PROGA' TO WS-PGM")
                .replace("MOVE 'PROGB' TO WS-PGM", "MOVE 'OTHER' TO WS-PGM\n MOVE 'PROGB' TO WS-PGM");
        var d = publish(source, "multiple"); var moves = statements(d, "MOVE"); var call = one(d, "CALL");
        completion(moves.get(0), moves.get(1)); completion(moves.get(1), call);
        completion(moves.get(2), moves.get(3)); completion(moves.get(3), call); completion(one(d, "IF"), call);
        assertEquals(id(moves.get(0)), one(d, "IF").path("thenArm").path("entry").path("statement"));
        assertEquals(id(moves.get(2)), one(d, "IF").path("elseArm").path("entry").path("statement"));
        for (String arm : List.of("thenArm", "elseArm")) {
            var origin = one(d, "IF").path(arm).path("provenance");
            assertTrue(origin.path("exact").asBoolean());
            var span = origin.path("expanded");
            assertEquals(span.path("startLine"), span.path("endLine"), "arm provenance anchors one written token");
        }
        // Existing namespaces deliberately put IF after MOVE/CALL IDs, although IF executes first.
        assertNotEquals(d.path("structure").path("roots").get(0), id(moves.get(0)));
    }
    static void noPredicate(JsonNode d) {
        var branch = one(d, "IF"); var p = branch.path("condition").path("predicate");
        assertNotEquals("KNOWN", p.path("availability").asText());
        assertEquals("UNKNOWN", p.path("resultDomain").asText());
        assertEquals("UNKNOWN", p.path("evaluation").asText());
        assertEquals("UNKNOWN", p.path("normalCompletion").asText());
        assertEquals("PARTIAL", p.path("readsCompleteness").asText());
        assertEquals("UNKNOWN", p.path("truthValue").asText());
        assertEquals("OUTSIDE_SLICE", branch.path("profile").asText());
        assertFalse(p.path("gapCodes").isEmpty());
        assertEquals(branch.path("condition").path("references").size(), p.path("knownReads").size());
    }
    @Test void conditionsOutsideSliceNeverGainGuarantees() throws Exception {
        for (String condition : List.of("FLAG(1:1) = 'Y'", "FLAG(1) = 'Y'", "MISSING = 'Y'",
                "FLAG > 'Y'", "FLAG NOT = 'Y'", "FLAG = N'Y'", "FLAG = 1", "FLAG = WS-PGM",
                "FUNCTION UPPER-CASE(FLAG) = 'Y'", "FLAG + 1 = 2", "FLAG = 'Y' OR FLAG = 'N'", "NOT FLAG = 'Y'")) {
            var d = publish(fixture("closed").replace("FLAG = 'Y'", condition), "condition-negative-" + Integer.toUnsignedString(condition.hashCode()));
            noPredicate(d);
            for (var reference : one(d, "IF").path("condition").path("references")) assertTrue(reference.path("wholeItemAccess").isNull());
        }
    }
    @Test void ambiguityAndUnresolvedKeepBindingsWithoutWholeAccess() throws Exception {
        var ambiguous = publish(fixture("closed").replace("01 FLAG PIC X.", "01 FLAG PIC X.\n01 FLAG PIC X."), "ambiguous");
        var binding = one(ambiguous, "IF").path("condition").path("references").get(0).path("binding");
        assertEquals("AMBIGUOUS", binding.path("status").asText()); assertEquals(2, binding.path("candidates").size());
        assertTrue(binding.path("selected").isNull()); noPredicate(ambiguous);
        var unresolved = publish(fixture("closed").replace("IF FLAG", "IF MISSING"), "unresolved");
        // Unresolved condition names admit DATA/INDEX: the historical DATA-only
        // surface retains the kind gap, never an invented DATA identity/read.
        assertTrue(one(unresolved, "IF").path("condition").path("references").isEmpty());
        assertTrue(java.util.stream.StreamSupport.stream(unresolved.path("gaps").spliterator(), false)
                .anyMatch(g -> g.path("code").asText().equals("CONDITION_REFERENCE_KIND_NOT_PROJECTED")));
        noPredicate(unresolved);
    }
    @Test void unsupportedStorageCannotGainIndependenceFromDistinctIds() throws Exception {
        for (String declaration : List.of("01 FLAG PIC X.\n01 OVERLAY REDEFINES FLAG PIC X.",
                "01 FLAG PIC X EXTERNAL.", "01 FLAG PIC X OCCURS 2.", "01 FLAG PIC 9.",
                "01 GROUP-ITEM.\n 05 FLAG PIC X.", "01 FLAG PIC X VALUE 'N'.",
                "01 FLAG PIC X.\n66 ALIAS-FLAG RENAMES FLAG.")) {
            var d = publish(fixture("closed").replace("01 FLAG PIC X.", declaration), "storage-negative-" + Integer.toUnsignedString(declaration.hashCode()));
            assertTrue(d.path("dataDeclarations").size() >= 2);
            assertEquals("UNAVAILABLE", d.path("storageIndependence").path("availability").asText());
            assertTrue(d.path("storageIndependence").path("members").isEmpty());
            assertFalse(d.path("storageIndependence").path("gapCodes").isEmpty());
        }
        var linkage = publish(fixture("closed").replace("WORKING-STORAGE", "LINKAGE"), "linkage");
        assertEquals("UNAVAILABLE", linkage.path("storageIndependence").path("availability").asText());
    }
    @Test void unrelatedUnmodeledDeclarationDoesNotEraseIndependentRoots() throws Exception {
        var d=publish(fixture("closed").replace("01 FLAG PIC X.","01 FLAG PIC X.\n01 WS-OTHER PIC 9."),"partial-storage");
        assertEquals("KNOWN",d.path("storageIndependence").path("availability").asText());
        var unsupported=java.util.stream.StreamSupport.stream(d.path("dataDeclarations").spliterator(),false)
            .filter(x->x.path("canonicalName").asText().equals("WS-OTHER")).findFirst().orElseThrow().path("id");
        assertTrue(java.util.stream.StreamSupport.stream(d.path("storageIndependence").path("members").spliterator(),false).noneMatch(unsupported::equals));
    }
    @Test void inputMissingAndRecoveryDoNotCertifyAbsenceOrReads() throws Exception {
        var a = AstBoundaryTestSupport.analyze(fixture("open"), "input.cbl");
        for (var state : List.of(new ResolutionAnalysisReport.FrontendState(0, 0, 1, List.of()),
                new ResolutionAnalysisReport.FrontendState(0, 0, 0, List.of(new Diagnostic("COBOL", Diagnostic.Phase.PREPROCESSOR,
                        Diagnostic.Code.UNRESOLVED_COPY, "input.cbl", 4, 0, "COPY unavailable", "MISSING", ""))))) {
            var report = ResolutionAnalysisReport.compose(a.build(), state, a.occurrences(), a.resolution());
            var proof = ScalarMoveSemantics.analyze(a.build(), a.tables(), a.resolution(), report);
            var port = io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.open(
                    new io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.FrontendProducts(
                            a.build(), a.tables(), a.occurrences(), a.resolution(), report, proof), a.model().programUnits().get(0).id());
            var d = new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(port)); noPredicate(d);
            assertEquals("INPUT_MISSING", d.path("storageIndependence").path("availability").asText());
            assertEquals("UNKNOWN", one(d, "IF").path("elseArm").path("presence").asText());
            assertEquals("INPUT_MISSING", one(d, "IF").path("elseArm").path("contentAvailability").asText());
            assertEquals("UNAVAILABLE", one(d, "IF").path("normalContinuation").path("availability").asText());
            assertEquals("UNAVAILABLE", one(d, "MOVE").path("normalContinuation").path("availability").asText());
        }
    }
    @Test void nestedOwnershipAndCompletionAreNeverFlattened() throws Exception {
        String source = fixture("closed").replace("MOVE 'PROGA' TO WS-PGM", "IF FLAG = 'N'\n MOVE 'INNER' TO WS-PGM\n ELSE\n MOVE 'OTHER' TO WS-PGM\n END-IF\n MOVE 'PROGA' TO WS-PGM");
        var d = publish(source, "nested"); var branches = statements(d, "IF"); var moves = statements(d, "MOVE");
        var outer = branches.get(0); var inner = branches.get(1); var call = one(d, "CALL");
        assertEquals(id(outer), inner.path("header").path("containment").path("parent"));
        assertEquals("THEN", inner.path("header").path("containment").path("branch").asText());
        assertEquals(id(inner), moves.get(0).path("header").path("containment").path("parent"));
        assertEquals("THEN", moves.get(0).path("header").path("containment").path("branch").asText());
        assertEquals(id(inner), moves.get(1).path("header").path("containment").path("parent"));
        assertEquals("ELSE", moves.get(1).path("header").path("containment").path("branch").asText());
        completion(inner, moves.get(2)); completion(moves.get(0), moves.get(2)); completion(moves.get(1), moves.get(2));
        completion(outer, call); completion(moves.get(2), call); completion(moves.get(3), call);
        assertEquals("OUTSIDE_SLICE", outer.path("profile").asText()); assertEquals("OUTSIDE_SLICE", inner.path("profile").asText());
        // Also exercise inherited completion when the inner IF is the last THEN member.
        var last = publish(source.replace(" MOVE 'PROGA' TO WS-PGM", ""), "nested-last");
        completion(statements(last, "IF").get(1), one(last, "CALL"));
    }
    @Test void unsupportedArmAndRegionBoundaryRemainPartial() throws Exception {
        for (String source : List.of(fixture("empty"), fixture("closed").replace("MOVE 'PROGA' TO WS-PGM", "DISPLAY FLAG"),
                fixture("closed").replace("MOVE 'PROGA' TO WS-PGM", "NEXT SENTENCE"),
                fixture("closed").replace("END-IF", "END-IF.").replace("    CALL WS-PGM.", "NEXT-PARA.\n CALL WS-PGM."))) {
            var d = publish(source, "unsupported-arm-" + Integer.toUnsignedString(source.hashCode()));
            assertEquals("OUTSIDE_SLICE", one(d, "IF").path("profile").asText());
            assertNotEquals("SUFFICIENT", one(d, "IF").path("header").path("readiness").path("lowering").path("status").asText());
        }
    }
}
