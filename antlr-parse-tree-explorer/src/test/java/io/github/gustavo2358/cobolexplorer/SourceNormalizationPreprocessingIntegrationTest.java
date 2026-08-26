package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.IdentityHashMap;

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
        GrammarBinding binding = Bindings.cobol();
        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(
                binding, new CopybookLibrary(FIXTURE.resolve("cpy")))
                .process(normalized.text(), "main.cbl");

        assertEquals(0, outcome.errors(), outcome.diagnostics().toString());
        assertEquals(0, outcome.unresolved(), outcome.diagnostics().toString());
        assertTrue(outcome.text().contains("AUTHOR. *>CE ENTRY INSIDE COPY. WITH PERIODS."));
        assertTrue(outcome.text().contains("*>CE SECOND ENTRY RECORD WITHOUT PERIOD"));
        assertTrue(outcome.text().contains("01 LONG-NAME PIC X."));
        assertTrue(outcome.text().contains("DISPLAY 'HELLO FROM COPY'."));

        assertEquals(List.of("UNIT.cpy", "FIELDS.cpy"), outcome.copyDependencies().stream()
                .map(PreprocessorEngine.CopyDependency::includedFile).toList());

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
        PreprocessorEngine engine = new PreprocessorEngine(Bindings.cobol(), library);

        PreprocessorEngine.Outcome missing = engine.process(
                "COPY MISSING.\n", "missing.cbl");
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
                "COPY CYCLE.\n", "cycle-main.cbl");
        assertTrue(cyclic.text().contains("CYCLIC COPY CYCLE"));
        assertEquals(0, cyclic.errors());
        assertTrue(cyclic.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.message().contains("cyclic COPY: CYCLE")));
        Diagnostic cycleDiagnostic = cyclic.diagnostics().stream()
                .filter(diagnostic -> diagnostic.message().contains("cyclic COPY: CYCLE"))
                .findFirst().orElseThrow();
        assertEquals("CYCLE.cpy", cycleDiagnostic.file());
        assertEquals(1, cycleDiagnostic.line());
        assertEquals(0, cycleDiagnostic.column());

        PreprocessorEngine.Outcome unreadable = engine.process(
                "COPY UNREADABLE.\n", "io-main.cbl");
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
        GrammarBinding binding = Bindings.cobol();
        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(
                binding, new CopybookLibrary(FIXTURE.resolve("cpy")))
                .process(normalized.text(), "policy-integration.cbl");

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

    @Test
    void copyExpansionKeepsCopyDependenciesCallsAndDataBindingsSemantic(@TempDir Path directory)
            throws Exception {
        Files.writeString(directory.resolve("DATA.cpy"), "       01 CUSTOMER-DATA.\n"
                + "          05 CUSTOMER-NAME PIC X(30).\n");
        Files.writeString(directory.resolve("CALLS.cpy"), "       CALL 'PROGB'.\n");
        String main = "       IDENTIFICATION DIVISION.\n"
                + "       PROGRAM-ID. PROGA.\n"
                + "       DATA DIVISION.\n"
                + "       WORKING-STORAGE SECTION.\n"
                + "       COPY DATA.\n"
                + "       PROCEDURE DIVISION.\n"
                + "       COPY CALLS.\n"
                + "       DISPLAY CUSTOMER-NAME.\n";
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(main, "PROGA.cbl",
                SourceNormalizer.SourceFormat.FIXED);
        GrammarBinding binding = Bindings.cobol();
        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(binding, new CopybookLibrary(directory))
                .process(normalized.text(), "PROGA.cbl");
        assertEquals(List.of("DATA.cpy", "CALLS.cpy"), outcome.copyDependencies().stream()
                .map(PreprocessorEngine.CopyDependency::includedFile).toList());

        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(outcome.text(), "PROGA.cbl"))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors(), outcome.text());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>(), sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[] {0});
        Ast.Program program = new AstBuilder(parser, outcome.text(), ids, sizes).build(tree).program();
        assertTrue(Ast.children(program).stream().anyMatch(Ast.Division.class::isInstance));
        assertTrue(nodes(program, Ast.CallStatement.class).stream().anyMatch(call ->
                call.target() instanceof Ast.ProgramReference target && target.programName().equals("PROGB")));
        SymbolTable table = new SymbolTableBuilder().build(program);
        assertTrue(table.symbols().stream().anyMatch(symbol -> symbol.writtenName().equals("CUSTOMER-NAME")));
    }

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        java.util.ArrayList<T> result = new java.util.ArrayList<>();
        java.util.ArrayDeque<Ast.Node> pending = new java.util.ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            Ast.Node node = pending.pop();
            if (type.isInstance(node)) result.add(type.cast(node));
            Ast.children(node).forEach(pending::push);
        }
        return result;
    }

    private static int index(ParseTree node, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        int id = next[0]++; ids.put(node, id); int size = 1;
        for (int child = 0; child < node.getChildCount(); child++)
            size += index(node.getChild(child), ids, sizes, next);
        sizes.put(node, size); return size;
    }

}
