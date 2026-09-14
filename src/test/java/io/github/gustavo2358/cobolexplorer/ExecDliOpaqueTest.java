package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** EXEC DLI boundary witnesses: lexical preservation, never IMS interpretation. */
class ExecDliOpaqueTest {
    static String source(String body) {
        return "       IDENTIFICATION DIVISION.\n       PROGRAM-ID. DLI-TEST.\n"
                + "       DATA DIVISION.\n       WORKING-STORAGE SECTION.\n"
                + "       01 WS-NAME PIC X(8).\n       PROCEDURE DIVISION.\n" + body;
    }

    @ParameterizedTest
    @ValueSource(strings = {"TERM", "GU PCB(1) SEGMENT(NAME)", "GNP PCB(1)", "REPL PCB(1)", "SCHD PSB((PSB-NAME))"})
    void basicFormsAndSentenceBoundaries(String payload) {
        for (String suffix : List.of(".", "\n           GOBACK.", ".\n           MOVE 'A' TO WS-NAME.", "\n           MOVE 'A' TO WS-NAME.")) {
            var analysis = analyze("           EXEC DLI " + payload + " END-EXEC" + suffix);
            var dli = embedded(analysis);
            assertEquals(1, dli.size());
            assertEquals("DLI", dli.get(0).language().name());
            assertEquals("EXEC DLI " + payload + " END-EXEC", dli.get(0).rawText());
            assertTrue(dli.get(0).hostOperands().isEmpty());
            assertEquals(suffix.contains("GOBACK") ? 1 : 0, AstBoundaryTestSupport.nodes(analysis, Ast.GobackStatement.class).size());
            assertEquals(suffix.contains("MOVE") ? 1 : 0, AstBoundaryTestSupport.nodes(analysis, Ast.MoveStatement.class).size());
            assertEquals(suffix.startsWith(".") && suffix.contains("MOVE") ? 2 : 1,
                    AstBoundaryTestSupport.nodes(analysis, Ast.Sentence.class).size());
        }
        // Preprocessing must accept a real delimiter at EOF; sentence syntax is a separate contract.
        assertDoesNotThrow(() -> preprocess("EXEC DLI " + payload + " END-EXEC"));
    }

    @Test void adjacentBlocksAndFamiliesRemainBijective() {
        var analysis = analyze("           IF WS-NAME = 'A'\n"
                + "           EXEC DLI TERM END-EXEC\n"
                + "           EXEC DLI SCHD PSB((PSB-NAME)) END-EXEC\n"
                + "           END-IF\n"
                + "           EXEC CICS SYNCPOINT END-EXEC\n"
                + "           EXEC DLI GU END-EXEC EXEC DLI GNP END-EXEC\n"
                + "           EXEC SQL SELECT 1 END-EXEC\n"
                + "           EXEC DLI REPL END-EXEC\n"
                + "           EXEC SQLIMS SELECT 1 END-EXEC\n"
                + "           EXEC DLI TERM END-EXEC\n           GOBACK.");
        assertEquals(List.of("DLI", "DLI", "CICS", "DLI", "DLI", "SQL", "DLI", "SQLIMS", "DLI"),
                embedded(analysis).stream().map(e -> e.language().name()).toList());
        var state = project(analysis);
        assertEquals(6, state.statements().stream().filter(s -> s instanceof CobolSemanticProduct.ObservedStatement o && o.observedShape().equals("OPAQUE_DLI")).count());
        assertEquals(AstBoundaryTestSupport.nodes(analysis, Ast.Statement.class).size(), state.statements().size());
        assertEquals("EXEC DLI TERM END-EXEC", embedded(analysis).get(0).rawText());
        assertEquals("EXEC DLI SCHD PSB((PSB-NAME)) END-EXEC", embedded(analysis).get(1).rawText());
    }

    @ParameterizedTest @ValueSource(strings = {"\n", "\r\n", "\r"})
    void normalizedSliceIsLossless(String newline) {
        String body = "           EXEC DLI GU  PCB(1)\n"
                + "      * END-EXEC EXEC DLI false delimiter 😀\n"
                + "           TEXT('END-EXEC '' } *>EXECDLI 中文 😀')\n"
                + "           TEXT(\"END-EXEC \"\" }\") *> END-EXEC EXEC CICS\n"
                + "           NAME(X-END-EXEC END-EXEC-X)\n"
                + "           END-EXEC.\n           GOBACK.\n";
        String raw = source(body).replace("\n", newline);
        String normalized = SourceNormalizer.normalize(raw, "dli.cbl", SourceNormalizer.SourceFormat.FIXED).text();
        String expected = normalized.substring(normalized.indexOf("EXEC DLI"), normalized.lastIndexOf("END-EXEC") + 8);
        var analysis = AstBoundaryTestSupport.analyze(raw, "dli.cbl");
        assertEquals(expected, embedded(analysis).get(0).rawText());
        assertEquals(1, AstBoundaryTestSupport.nodes(analysis, Ast.GobackStatement.class).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXEC DLI TERM", "EXEC DLI TERM\nGOBACK.", "EXEC DLI GU\nMOVE 'A' TO WS-NAME.",
            "EXEC DLI TERM\nEXEC CICS RETURN END-EXEC", "EXEC DLI TERM EXEC DLI GU END-EXEC",
            "EXEC DLI GU TEXT('open END-EXEC", "EXEC DLI END-EXEC", "EXEC DLI *> only comment\nEND-EXEC"})
    void malformedDliFailsLocallyBeforeAnyPublication(String malformed) {
        var error = assertThrows(IllegalStateException.class, () -> preprocess(malformed));
        assertTrue(error.getMessage().contains("DLI"), error.getMessage());
    }

    @Test void unknownExecHasNoFallbackPolicy() {
        assertThrows(IllegalStateException.class, () -> preprocess("EXEC MYSTERY FOO END-EXEC"));
        assertThrows(IllegalStateException.class, () -> PreprocessorEngine.policyFor("EXEC"));
    }

    @Test void opaquePayloadCannotInventReferencesOrControl() {
        for (String command : List.of("TERM", "SCHD", "GU", "GNP", "REPL")) {
            var analysis = analyze("           EXEC DLI " + command + "\n"
                    + "           PROGRAM(WS-NAME) LINK XCTL SQL SQLIMS TABLE(PROGRAM)\n"
                    + "           END-EXEC\n           GOBACK.");
            AstBoundaryTestSupport.assertActualProductsJoin(analysis);
            assertTrue(analysis.occurrences().values().stream().allMatch(o -> o.occurrences().isEmpty()));
            var dli = embedded(analysis).get(0);
            assertTrue(dli.hostOperands().isEmpty());
            var state = project(analysis);
            var fact = assertInstanceOf(CobolSemanticProduct.ObservedStatement.class, state.statements().get(0));
            assertEquals("OPAQUE_DLI", fact.observedShape());
            assertEquals("EMBEDDED_LANGUAGE", fact.observedKind());
            assertEquals(CobolSemanticProduct.CoverageStatus.PARTIAL, fact.header().coverage());
            assertEquals("OBSERVED_STATEMENT_PARTIAL", fact.gapCode());
            assertTrue(fact.knownReferences().isEmpty());
            assertEquals(CobolSemanticProduct.ContinuationAvailability.UNAVAILABLE, fact.normalContinuation().availability());
            assertTrue(fact.normalContinuation().statement().isEmpty());
            assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED, fact.header().readiness().cfg().status());
            assertTrue(state.gaps().stream().anyMatch(g -> g.statement().equals(fact.header().id()) && g.code().equals(fact.gapCode())));
            for (var division : analysis.model().programUnits().get(0).program().divisions()) {
                assertFalse(division.normalContinuations().containsKey(dli.meta().id()));
            }
        }
    }

    static PreprocessorEngine.Outcome preprocess(String normalized) throws Exception {
        return new PreprocessorEngine(Bindings.cobol(), new CopybookLibrary(Path.of("src/test/resources/cobol/provenance/cpy")))
                .process(SourceMap.identity(normalized, "dli.cbl"), "dli.cbl");
    }
    static AstBoundaryTestSupport.Analysis analyze(String body) {
        return AstBoundaryTestSupport.analyze(source(body), "dli.cbl");
    }
    static List<Ast.EmbeddedLanguageStatement> embedded(AstBoundaryTestSupport.Analysis analysis) {
        return AstBoundaryTestSupport.nodes(analysis, Ast.EmbeddedLanguageStatement.class);
    }
    static CobolSemanticProduct.State project(AstBoundaryTestSupport.Analysis analysis) {
        return CobolSemanticProductProjector.project(new CobolSemanticProductProjector.FrontendProducts(
                analysis.build(), analysis.tables(), analysis.occurrences(), analysis.resolution(), analysis.report(),
                ScalarMoveSemantics.analyze(analysis.build(), analysis.tables(), analysis.resolution(), analysis.report())),
                analysis.model().programUnits().get(0).id());
    }
}
