package io.proleap.benchmark;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceNormalizationPreprocessingIntegrationTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/source-format-integration");

    @Test
    void normalizesCommentEntriesAndContinuationsAcrossNestedCopybooks() throws Exception {
        Path main = FIXTURE.resolve("main.cbl");
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(
                Files.readString(main, StandardCharsets.UTF_8), "main.cbl",
                SourceNormalizer.SourceFormat.FIXED);
        GrammarBinding binding = Bindings.proleap();
        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(
                binding, new CopybookLibrary(FIXTURE.resolve("cpy")))
                .process(normalized.sourceMap(), "main.cbl");

        assertEquals(0, outcome.errors(), outcome.diagnostics().toString());
        assertEquals(0, outcome.unresolved(), outcome.diagnostics().toString());
        assertTrue(outcome.text().contains("AUTHOR. *>CE ENTRY INSIDE COPY. WITH PERIODS."));
        assertTrue(outcome.text().contains("*>CE SECOND ENTRY RECORD WITHOUT PERIOD"));
        assertTrue(outcome.text().contains("01 LONG-NAME PIC X."));
        assertTrue(outcome.text().contains("DISPLAY 'HELLO FROM COPY'."));

        Ast.SourceProvenance field = provenanceOf(outcome, "LONG-NAME");
        assertEquals("FIELDS.cpy", field.original().file());
        assertEquals(1, field.original().startLine());
        assertEquals(List.of("UNIT.cpy", "FIELDS.cpy"), includedFiles(field));
        assertFalse(field.exact(), "a word assembled from two physical records is transformed");

        Ast.SourceProvenance literal = provenanceOf(outcome, "DISPLAY 'HELLO FROM COPY'.");
        assertEquals("UNIT.cpy", literal.original().file());
        assertEquals(10, literal.original().startLine());
        assertEquals(List.of("UNIT.cpy"), includedFiles(literal));
        assertFalse(literal.exact(), "a literal assembled from two physical records is transformed");

        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(outcome.text(), "main.cbl"))));
        binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors(), outcome.text());
    }

    @Test
    void copyFailuresAreLocalizedForMissingCyclicAndUnreadableSources(@TempDir Path directory)
            throws Exception {
        Files.writeString(directory.resolve("CYCLE.cpy"), "       COPY CYCLE.\n");
        Files.writeString(directory.resolve("UNREADABLE.cpy"), "       01 VALUE-A PIC X.\n");
        CopybookLibrary library = new CopybookLibrary(directory);
        Files.delete(directory.resolve("UNREADABLE.cpy"));
        PreprocessorEngine engine = new PreprocessorEngine(Bindings.proleap(), library);

        PreprocessorEngine.Outcome missing = engine.process(
                SourceMap.identity("COPY MISSING.\n", "missing.cbl"), "missing.cbl");
        assertEquals(1, missing.unresolved());
        assertEquals(0, missing.errors());
        assertTrue(missing.text().contains("UNRESOLVED COPY MISSING"));
        Diagnostic missingDiagnostic = missing.diagnostics().stream()
                .filter(diagnostic -> diagnostic.message().startsWith("unresolved_copy"))
                .findFirst().orElseThrow();
        assertEquals("missing.cbl", missingDiagnostic.file());
        assertEquals(1, missingDiagnostic.line());
        assertEquals(0, missingDiagnostic.column());

        PreprocessorEngine.Outcome cyclic = engine.process(
                SourceMap.identity("COPY CYCLE.\n", "cycle-main.cbl"), "cycle-main.cbl");
        assertTrue(cyclic.text().contains("CYCLIC COPY CYCLE"));
        assertEquals(0, cyclic.errors());
        assertTrue(cyclic.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.message().contains("cyclic COPY: CYCLE")));
        Diagnostic cycleDiagnostic = cyclic.diagnostics().stream()
                .filter(diagnostic -> diagnostic.message().contains("cyclic COPY: CYCLE"))
                .findFirst().orElseThrow();
        assertEquals("CYCLE.cpy", cycleDiagnostic.file());
        assertEquals(1, cycleDiagnostic.line());
        assertEquals(7, cycleDiagnostic.column());

        PreprocessorEngine.Outcome unreadable = engine.process(
                SourceMap.identity("COPY UNREADABLE.\n", "io-main.cbl"), "io-main.cbl");
        assertTrue(unreadable.text().contains("COPY IO ERROR UNREADABLE"));
        assertTrue(unreadable.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.phase() == Diagnostic.Phase.IO));
        Diagnostic ioDiagnostic = unreadable.diagnostics().stream()
                .filter(diagnostic -> diagnostic.phase() == Diagnostic.Phase.IO)
                .findFirst().orElseThrow();
        assertEquals("io-main.cbl", ioDiagnostic.file());
        assertEquals(1, ioDiagnostic.line());
        assertEquals(0, ioDiagnostic.column());
    }

    @Test
    void normalizationPreservesEverySupportedPreprocessorPolicy() throws Exception {
        String raw = "       CBL DYNAM,NODLL,PGMNAME(LONGMIXED)\n"
                + "       IDENTIFICATION DIVISION.\n"
                + "       PROGRAM-ID. POLICY-INTEGRATION.\n"
                + "       DATA DIVISION.\n"
                + "       PROCEDURE DIVISION.\n"
                + "       EJECT\n"
                + "       SKIP1\n"
                + "       TITLE 'IGNORED'\n"
                + "           EXEC CICS RETURN END-EXEC.\n"
                + "           EXEC SQL SELECT 'A  B' END-EXEC.\n"
                + "           EXEC SQLIMS SELECT 1 END-EXEC.\n"
                + "           GOBACK.\n";
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(
                raw, "policy-integration.cbl", SourceNormalizer.SourceFormat.FIXED);
        GrammarBinding binding = Bindings.proleap();
        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(
                binding, new CopybookLibrary(FIXTURE.resolve("cpy")))
                .process(normalized.sourceMap(), "policy-integration.cbl");

        assertEquals(ResolutionContracts.DynamMode.DYNAM, outcome.dynamMode());
        assertEquals(ResolutionContracts.DllMode.NODLL, outcome.dllMode());
        assertEquals(ResolutionContracts.PgmnameMode.LONGMIXED, outcome.pgmnameMode());
        assertTrue(outcome.text().contains("*>EXECCICS EXEC CICS RETURN END-EXEC"));
        assertTrue(outcome.text().contains("*>EXECSQL EXEC SQL SELECT 'A  B' END-EXEC"),
                "opaque embedded-language whitespace must be preserved");
        assertTrue(outcome.text().contains("*>EXECSQLIMS EXEC SQLIMS SELECT 1 END-EXEC"));
        for (String removed : List.of("EJECT", "SKIP1", "TITLE 'IGNORED'")) {
            assertFalse(outcome.text().contains(removed), removed + " leaked through preprocessing");
        }

        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(
                        outcome.text(), "policy-integration.cbl"))));
        binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors(), outcome.text());
    }

    private static Ast.SourceProvenance provenanceOf(PreprocessorEngine.Outcome outcome,
                                                      String text) {
        int start = outcome.text().indexOf(text);
        return outcome.sourceMap().provenance(start, start + text.length());
    }

    private static List<String> includedFiles(Ast.SourceProvenance provenance) {
        return provenance.includeChain().stream().map(Ast.CopyFrame::includedFile).toList();
    }
}
