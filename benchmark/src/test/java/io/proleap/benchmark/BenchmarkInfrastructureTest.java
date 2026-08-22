package io.proleap.benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkInfrastructureTest {
    @TempDir Path temp;

    @Test void discoveryIsCaseInsensitive() throws Exception {
        Files.writeString(temp.resolve("a.cbl"), ""); Files.writeString(temp.resolve("b.CBL"), "");
        Files.writeString(temp.resolve("c.cpy"), ""); Files.writeString(temp.resolve("d.CPY"), "");
        assertEquals(2, BenchmarkMain.discover(temp, Set.of(".cbl")).size());
        assertEquals(2, BenchmarkMain.discover(temp, Set.of(".cpy")).size());
    }

    @Test void syntaxErrorsAreCapturedAndMeanFailure() throws Exception {
        Path cpy = Files.createDirectory(temp.resolve("cpy"));
        Path source = temp.resolve("bad.cbl");
        Files.writeString(source, "       THIS IS NOT A COBOL PROGRAM.\n");
        FrontendResult result = new Cobol85Frontend(cpy).parse(source);
        assertFalse(result.parsingSuccess());
        assertTrue(result.lexerErrors() + result.parserErrors() > 0);
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.phase() == Diagnostic.Phase.PARSER || d.phase() == Diagnostic.Phase.LEXER));
    }

    @Test void bothFrontendsUseSameInterfaceAndProduceMetrics() throws Exception {
        Path cpy = Files.createDirectory(temp.resolve("cpy"));
        Path source = temp.resolve("ok.CBL");
        Files.writeString(source, "       IDENTIFICATION DIVISION.\n" +
                "       PROGRAM-ID. HELLO.\n" +
                "       PROCEDURE DIVISION.\n" +
                "           STOP RUN.\n");
        List<CobolFrontend> frontends = List.of(new Cobol85Frontend(cpy), new ProLeapGrammarFrontend(cpy));
        for (CobolFrontend frontend : frontends) {
            FrontendResult result = frontend.parse(source);
            assertTrue(result.parsingSuccess(), () -> result.diagnostics().toString());
            assertTrue(result.tokenCount() > 0);
            assertTrue(result.parseTreeNodeCount() > 0);
            assertTrue(result.parseTreeMaxDepth() > 0);
        }
    }

    @Test void fixedNormalizerHandlesCommentEntriesAndLiteralContinuation() {
        String normalized = SourceNormalizer.fixed("       AUTHOR. AWS.\n" +
                "           01 X VALUE 'ABC\n" +
                "      -        'DEF'.\n");
        assertTrue(normalized.contains("AUTHOR. \n*>CE AWS."));
        assertTrue(normalized.contains("'ABCDEF'"));
    }

    @Test void csvEscapesCommasQuotesAndNewlines() {
        String row = Csv.row("a,b", "x\"y", "line1\nline2");
        assertEquals("\"a,b\",\"x\"\"y\",\"line1\nline2\"\n", row);
    }
}
