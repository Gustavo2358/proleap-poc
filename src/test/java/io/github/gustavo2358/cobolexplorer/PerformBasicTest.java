package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

class PerformBasicTest {
    @Test void typedCanonicalTargetBodyResumeAndProvenance() throws Exception {
        for (String name : List.of("literal", "copy", "overwrite")) {
            String body = name.equals("copy") ? "MOVE 'PROGA' TO WS-A.\nMOVE WS-A TO WS-PGM." : "MOVE 'PROGA' TO WS-PGM.";
            String source = PerformDiscoveryTest.source(name.equals("overwrite") ? "MOVE 'OLDPROG' TO WS-PGM.\n" : "", "PERFORM DEFINE-PGM.", body);
            if (name.equals("literal") || name.equals("overwrite")) source = source.replace("01 WS-A PIC X(8).\n", "");
            var port = ScalarMoveCheckpoint4ATest.publish(source);
            assertEquals(1, port.performs().size()); var perform = port.performs().get(0);
            assertEquals(PerformProfile.BASIC_PROCEDURE_PERFORM, perform.profile());
            assertEquals(port.calls().get(0).header().id(), perform.normalContinuation().statement().orElseThrow());
            var bodyFacts = perform.targetStatements().stream().map(id -> (MoveFact) port.statement(id).orElseThrow()).toList();
            assertEquals(name.equals("copy") ? 2 : 1, bodyFacts.size());
            assertEquals(Optional.of(bodyFacts.get(0).header().id()), perform.targetEntry());
            assertEquals(Optional.of(bodyFacts.get(bodyFacts.size()-1).header().id()), perform.targetExit());
            assertEquals(Optional.empty(), bodyFacts.get(bodyFacts.size()-1).normalContinuation().statement());
            assertTrue(perform.primaryStatements().isEmpty(), "SP1.8 does not duplicate the primary inventory per activation");
            assertFalse(perform.targetStatements().contains(port.entries().get(0).start().statement().orElseThrow()));
            assertEquals(perform.header().provenance().original().startLine(), perform.target().orElseThrow().referenceOrigin().original().startLine());
            assertTrue(perform.target().orElseThrow().referenceOrigin().exact()); assertTrue(perform.target().orElseThrow().paragraphOrigin().exact());
            assertEquals(port.calls().get(0).header().provenance(), perform.normalContinuation().provenance());
            assertTrue(bodyFacts.stream().allMatch(m -> m.header().provenance().exact() && m.header().coverage() == CoverageStatus.MODELED));
            assertEquals("PROGA   ", bodyFacts.get(0).textAdjustment().orElseThrow().result().value());
            if (name.equals("copy")) { assertInstanceOf(DataReference.class, bodyFacts.get(1).source()); assertEquals(Availability.KNOWN, port.storageIndependence().availability()); }
            var json = SemanticProductJsonWriter.serialize(port);
            assertArrayEquals(json, SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)));
            var tree = new ObjectMapper().readTree(json); assertEquals("1.8.0", tree.path("contractVersion").asText());
            Path out = Path.of("target/perform-basic"); Files.createDirectories(out);
            Files.write(out.resolve(name + ".json"), json); Files.writeString(out.resolve(name + ".cbl"), source);
        }
    }
    @Test void unsupportedFormsAndOtherActivationsNeverPublishNormalization() {
        var base = PerformDiscoveryTest.simple();
        var sources = new LinkedHashMap<String,String>();
        for (String form : List.of("DEFINE-PGM THRU DEFINE-PGM", "DEFINE-PGM 5 TIMES", "DEFINE-PGM UNTIL WS-A = 'X'",
                "DEFINE-PGM WITH TEST AFTER UNTIL WS-A = 'X'", "DEFINE-PGM VARYING WS-A FROM 1 BY 1 UNTIL WS-A = 5",
                "MOVE 'PROGA' TO WS-PGM END-PERFORM", "MISSING"))
            sources.put(form, base.replace("PERFORM DEFINE-PGM.", "PERFORM " + form + "."));
        sources.put("ordinary fallthrough", base.replace("GOBACK.\nDEFINE-PGM.", "DEFINE-PGM."));
        sources.put("GO TO target", base.replace("GOBACK.", "GO TO DEFINE-PGM."));
        sources.put("ENTRY", base.replace("CALL WS-PGM.", "ENTRY 'ALT'.\nCALL WS-PGM."));
        sources.put("empty target", base.replace("MOVE 'PROGA' TO WS-PGM.", ""));
        sources.put("ambiguous target", base + "DEFINE-PGM.\nMOVE 'OTHER' TO WS-PGM.\n");
        for (String body : List.of("IF WS-A = 'X' MOVE 'PROGA' TO WS-PGM END-IF.", "PERFORM DEFINE-PGM.", "CALL WS-A.",
                "NEXT SENTENCE.", "EXIT.", "GO TO MAIN."))
            sources.put(body, base.replace("MOVE 'PROGA' TO WS-PGM.", body));
        sources.forEach((name, source) -> {
            var port = ScalarMoveCheckpoint4ATest.publish(source);
            assertTrue(port.performs().isEmpty(), name);
            assertTrue(port.observedStatements().stream().anyMatch(s -> s.observedKind().equals("PERFORM")), name);
        });
    }
    @Test void namesDoNotSelectPrimaryOrTarget() {
        var source = PerformDiscoveryTest.simple().replace("MAIN", "ANY-NAME").replace("DEFINE-PGM", "ANOTHER-NAME");
        var port = ScalarMoveCheckpoint4ATest.publish(source);
        assertEquals(1, port.performs().size());
        assertEquals(port.calls().get(0).header().id(), port.performs().get(0).normalContinuation().statement().orElseThrow());
    }
    @Test void incompleteInputCannotProveIsolationOrParagraphReturn() {
        var a = AstBoundaryTestSupport.analyze(PerformDiscoveryTest.simple(), "incomplete.cbl");
        var report = ResolutionAnalysisReport.compose(a.build(), new ResolutionAnalysisReport.FrontendState(0, 0, 1, List.of()), a.occurrences(), a.resolution());
        var semantic = ScalarMoveSemantics.analyze(a.build(), a.tables(), a.resolution(), report);
        var unit = a.model().programUnits().get(0).id();
        var perform = AstBoundaryTestSupport.nodes(a, Ast.PerformStatement.class).get(0);
        var move = AstBoundaryTestSupport.nodes(a, Ast.MoveStatement.class).get(0);
        assertFalse(semantic.performs().fact(unit, perform.meta().id()).simpleProfile());
        assertTrue(semantic.move(unit, move.meta().id()).nextStatement().isEmpty());
    }
}
