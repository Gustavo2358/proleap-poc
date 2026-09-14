package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExecDliProvenanceTest {
    @Test void nestedCopyReplacingContinuationAndUnicode(@TempDir Path copy) throws Exception {
        Files.writeString(copy.resolve("OUTER.cpy"), "       COPY INNER.\n");
        Files.writeString(copy.resolve("INNER.cpy"), "           EXEC DLI SCHD PSB((OLD-NAME))\n"
                + "           TEXT('HELLO\n      -    ' WORLD')\n           END-EXEC\n"
                + "           MOVE 'A' TO WS-NAME\n");
        String source = ExecDliOpaqueTest.source("      * 😀 before COPY\n"
                + "           COPY OUTER REPLACING ==OLD-NAME== BY ==NEW-NAME==.\n"
                + "           GOBACK.\n");
        var normalized = SourceNormalizer.normalize(source, "main.cbl", SourceNormalizer.SourceFormat.FIXED);
        var preprocessing = new PreprocessorEngine(Bindings.cobol(), new CopybookLibrary(copy)).process(normalized.sourceMap(), "main.cbl");
        assertEquals(0, preprocessing.unresolved());
        var analysis = AstBoundaryTestSupport.analyze(preprocessing, "main.cbl");
        var dli = ExecDliOpaqueTest.embedded(analysis).get(0);
        String expected = SourceNormalizer.normalize(Files.readString(copy.resolve("INNER.cpy")), "INNER.cpy", SourceNormalizer.SourceFormat.FIXED).text();
        expected = expected.substring(expected.indexOf("EXEC DLI"), expected.indexOf("END-EXEC") + 8).replace("OLD-NAME", "NEW-NAME");
        assertEquals(expected, dli.rawText());
        var origin = dli.meta().provenance();
        assertEquals(new Ast.SourceLocation("INNER.cpy", 1, 11, 4, 18), origin.original());
        assertFalse(origin.exact());
        assertEquals(List.of("OUTER.cpy", "INNER.cpy"), origin.includeChain().stream().map(Ast.CopyFrame::includedFile).toList());
        assertEquals(List.of(8, 1), origin.includeChain().stream().map(Ast.CopyFrame::includeLine).toList());
        var move = AstBoundaryTestSupport.nodes(analysis, Ast.MoveStatement.class).get(0);
        assertEquals("INNER.cpy", move.meta().provenance().original().file());
        assertEquals(5, move.meta().provenance().original().startLine());
        assertEquals(11, move.meta().provenance().original().startColumn());
        assertEquals(origin.includeChain(), move.meta().provenance().includeChain());
        var goback = AstBoundaryTestSupport.nodes(analysis, Ast.GobackStatement.class).get(0);
        assertEquals(new Ast.SourceLocation("main.cbl", 9, 11, 9, 16), goback.meta().provenance().original());
        assertTrue(goback.meta().provenance().includeChain().isEmpty());
        var indexed = new UnicodeText(preprocessing.text());
        int offset = indexed.indexOf("GOBACK", 0);
        assertEquals(goback.meta().provenance().original(), preprocessing.sourceMap().provenance(offset, offset + 6).original());
        AstBoundaryTestSupport.assertActualProductsJoin(analysis);
    }

    @Test void realCorpusRegionsAndFollowingSentinels() throws Exception {
        Path path = Path.of("corpus/carddemo/cbl/COPAUS1C.cbl");
        String raw = Files.readString(path);
        // Interval witnesses are tied to this unchanged upstream corpus, not an eternal count.
        assertEquals("27a969cbee69426fa1056053e676041430e99399912f0e27ee1f1a454093c21e",
                java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
        var normalized = SourceNormalizer.normalize(raw, "COPAUS1C.cbl", SourceNormalizer.SourceFormat.FIXED);
        var preprocessing = new PreprocessorEngine(Bindings.cobol(), new CopybookLibrary(List.of(Path.of("corpus/carddemo/cpy"), Path.of("corpus/carddemo/cpy-bms"))))
                .process(normalized.sourceMap(), "COPAUS1C.cbl");
        assertEquals(0, preprocessing.errors());
        var analysis = AstBoundaryTestSupport.analyze(preprocessing, "COPAUS1C.cbl");
        var dli = ExecDliOpaqueTest.embedded(analysis).stream().filter(e -> e.language().name().equals("DLI")).toList();
        int[][] intervals = {{439,443},{465,469},{495,498},{525,528},{575,578},{581,582},{584,587}};
        assertEquals(intervals.length, dli.size());
        String[] lines = normalized.text().split("\n", -1);
        for (int i = 0; i < intervals.length; i++) {
            int start = intervals[i][0], end = intervals[i][1];
            String slice = String.join("\n", java.util.Arrays.copyOfRange(lines, start-1, end));
            slice = slice.substring(slice.indexOf("EXEC DLI"), slice.lastIndexOf("END-EXEC") + 8);
            assertEquals(slice, dli.get(i).rawText());
            assertEquals(start, dli.get(i).meta().provenance().original().startLine());
            assertEquals(end, dli.get(i).meta().provenance().original().endLine());
            assertEquals("COPAUS1C.cbl", dli.get(i).meta().provenance().original().file());
            assertTrue(dli.get(i).hostOperands().isEmpty());
        }
        var moves = AstBoundaryTestSupport.nodes(analysis, Ast.MoveStatement.class);
        for (int line : List.of(445,471,500,530,579,588))
            assertEquals(1, moves.stream().filter(m -> m.meta().provenance().original().file().equals("COPAUS1C.cbl") && m.meta().provenance().original().startLine() == line).count());
        for (int line : List.of(558,566))
            assertEquals(1, ExecDliOpaqueTest.embedded(analysis).stream().filter(e -> e.language() == Ast.EmbeddedLanguage.CICS && e.meta().provenance().original().startLine() == line).count());
        AstBoundaryTestSupport.assertActualProductsJoin(analysis);
        var state = ExecDliOpaqueTest.project(analysis);
        var observed = state.statements().stream().filter(s -> s instanceof io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.ObservedStatement o && o.observedShape().equals("OPAQUE_DLI")).toList();
        assertEquals(dli.size(), observed.size());
    }
}
