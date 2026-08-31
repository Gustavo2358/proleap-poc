package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PartialAnalysisMissingCopyTest {
    @Test
    void missingCopiesKeepExternalFactsAndEachInputGapObservable(@TempDir Path directory)
            throws Exception {
        Path copybooks = Files.createDirectory(directory.resolve("configured-copybooks"));
        String snapshot = analyze(directory, copybooks, "two-missing.cbl", source(
                "COPY MISSINGA.", "COPY MISSINGB.", "DFHRESP(NORMAL)"));

        assertAll("partial analysis remains useful and explicitly incomplete",
                () -> assertTrue(snapshot.contains("\"externalClassifications\":1")),
                () -> assertEquals(2, occurrences(snapshot,
                        "\"inputCompleteness\":\"INCOMPLETE_UNRESOLVED_COPY\"")),
                () -> assertTrue(snapshot.contains("\"claim\":\"INCOMPLETE\"")),
                () -> assertTrue(snapshot.contains("\"dependencyAnalysisReady\":false")),
                () -> assertTrue(snapshot.contains("\"unresolvedCopies\":2")),
                () -> assertEquals(2, occurrences(snapshot, "\"code\":\"UNRESOLVED_COPY\"")),
                () -> assertTrue(snapshot.contains("MISSINGA")),
                () -> assertTrue(snapshot.contains("MISSINGB")),
                () -> assertTrue(snapshot.contains("\"category\":\"EXTERNAL_CLASSIFICATION\"")),
                () -> assertTrue(snapshot.contains("\"category\":\"REFERENCE_BINDING\"")),
                () -> assertTrue(snapshot.contains("\"status\":\"UNRESOLVED\"")));
    }

    @Test
    void resolvingCopyWithCobolDeclarationMakesCobolWin(@TempDir Path directory)
            throws Exception {
        Path copybooks = Files.createDirectory(directory.resolve("copybooks"));
        String missing = analyze(directory, copybooks, "missing-declaration.cbl", source(
                "COPY TESTCP.", "", "DFHRESP(IDX)"));

        Files.writeString(copybooks.resolve("TESTCP.cpy"),
                "       01 DFHRESP OCCURS 10 TIMES PIC 9.\n", StandardCharsets.UTF_8);
        String resolved = analyze(directory, copybooks, "resolved-declaration.cbl", source(
                "COPY TESTCP.", "", "DFHRESP(IDX)"));

        assertAll("MR2 missing to resolved declaration refines the nominal universe",
                () -> assertTrue(missing.contains("\"externalClassifications\":1")),
                () -> assertTrue(missing.contains("\"inputCompleteness\":\"INCOMPLETE_UNRESOLVED_COPY\"")),
                () -> assertTrue(resolved.contains("\"externalClassifications\":0")),
                () -> assertTrue(resolved.contains("\"unresolvedCopies\":0")),
                () -> assertFalse(resolved.contains("\"code\":\"UNRESOLVED_COPY\"")),
                () -> assertTrue(resolved.contains("\"writtenName\":\"DFHRESP\"")),
                () -> assertTrue(resolved.contains("\"status\":\"RESOLVED\"")));
    }

    @Test
    void resolvingIrrelevantCopyPreservesIndependentClassification(@TempDir Path directory)
            throws Exception {
        Path copybooks = Files.createDirectory(directory.resolve("copybooks"));
        String missing = analyze(directory, copybooks, "irrelevant-missing.cbl", source(
                "COPY IRRELEVANT.", "", "DFHRESP(NORMAL)"));

        Files.writeString(copybooks.resolve("IRRELEVANT.cpy"),
                "       01 UNUSED-FIELD PIC X.\n", StandardCharsets.UTF_8);
        String resolved = analyze(directory, copybooks, "irrelevant-resolved.cbl", source(
                "COPY IRRELEVANT.", "", "DFHRESP(NORMAL)"));

        assertAll("MR1 irrelevant input changes completeness but not the independent fact",
                () -> assertTrue(missing.contains("\"externalClassifications\":1")),
                () -> assertTrue(resolved.contains("\"externalClassifications\":1")),
                () -> assertTrue(missing.contains("\"technology\":\"CICS\"")),
                () -> assertTrue(resolved.contains("\"technology\":\"CICS\"")),
                () -> assertTrue(missing.contains("\"inputCompleteness\":\"INCOMPLETE_UNRESOLVED_COPY\"")),
                () -> assertTrue(resolved.contains("\"inputCompleteness\":\"COMPLETE\"")),
                () -> assertTrue(missing.contains("\"code\":\"UNRESOLVED_COPY\"")),
                () -> assertFalse(resolved.contains("\"code\":\"UNRESOLVED_COPY\"")));
    }

    @Test
    void addingAnotherMissingCopyDoesNotEraseExistingFacts(@TempDir Path directory)
            throws Exception {
        Path copybooks = Files.createDirectory(directory.resolve("copybooks"));
        String one = analyze(directory, copybooks, "one-missing.cbl", source(
                "COPY FIRSTMISS.", "", "DFHRESP(NORMAL)"));
        String two = analyze(directory, copybooks, "two-missing.cbl", source(
                "COPY FIRSTMISS.", "COPY SECONDMISS.", "DFHRESP(NORMAL)"));

        assertAll("MR3 adds uncertainty monotonically",
                () -> assertTrue(one.contains("\"externalClassifications\":1")),
                () -> assertTrue(two.contains("\"externalClassifications\":1")),
                () -> assertTrue(one.contains("\"unresolvedCopies\":1")),
                () -> assertTrue(two.contains("\"unresolvedCopies\":2")),
                () -> assertEquals(1, occurrences(one, "\"code\":\"UNRESOLVED_COPY\"")),
                () -> assertEquals(2, occurrences(two, "\"code\":\"UNRESOLVED_COPY\"")));
    }

    @Test
    void copyOutsideConfiguredSearchPathRemainsMissing(@TempDir Path directory)
            throws Exception {
        Path configured = Files.createDirectory(directory.resolve("configured"));
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Files.writeString(outside.resolve("OUTSIDE.cpy"),
                "       01 DFHRESP OCCURS 10 TIMES PIC 9.\n", StandardCharsets.UTF_8);

        String snapshot = analyze(directory, configured, "outside-path.cbl", source(
                "COPY OUTSIDE.", "", "DFHRESP(IDX)"));

        assertAll("H search paths remain an explicit boundary",
                () -> assertTrue(snapshot.contains("\"unresolvedCopies\":1")),
                () -> assertTrue(snapshot.contains("OUTSIDE")),
                () -> assertTrue(snapshot.contains("\"externalClassifications\":1")),
                () -> assertTrue(snapshot.contains("\"inputCompleteness\":\"INCOMPLETE_UNRESOLVED_COPY\"")));
    }

    @Test
    void sameInputAndSearchPathsProduceDeterministicPartialSnapshot(@TempDir Path directory)
            throws Exception {
        Path copybooks = Files.createDirectory(directory.resolve("copybooks"));
        String source = source("COPY MISSINGA.", "COPY MISSINGB.", "DFHVALUE(SOME-NAME)");

        String first = analyze(directory, copybooks, "deterministic.cbl", source);
        String second = analyze(directory, copybooks, "deterministic.cbl", source);

        assertEquals(first, second, "MR4 partial-analysis products and ordering must be deterministic");
    }

    private static String analyze(Path directory, Path copybooks, String fileName, String source)
            throws Exception {
        Path sourceFile = directory.resolve(fileName);
        Path output = directory.resolve(fileName + "-output");
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);
        ExplorerMain.main(new String[]{"--source", sourceFile.toString(),
                "--copybooks", copybooks.toString(), "--output", output.toString()});
        return Files.readString(output.resolve("resolution-data.js"), StandardCharsets.UTF_8);
    }

    private static String source(String firstCopy, String secondCopy, String construct) {
        return String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. PARTIALCOPY.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-RESP PIC S9(8) COMP.",
                "       01 IDX PIC 9.",
                firstCopy.isBlank() ? "" : "       " + firstCopy,
                secondCopy.isBlank() ? "" : "       " + secondCopy,
                "       PROCEDURE DIVISION.",
                "           IF WS-RESP = " + construct,
                "               CONTINUE",
                "           END-IF.",
                "           GOBACK.",
                "       END PROGRAM PARTIALCOPY.", "");
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int from = 0; (from = value.indexOf(needle, from)) >= 0; from += needle.length()) {
            count++;
        }
        return count;
    }
}
