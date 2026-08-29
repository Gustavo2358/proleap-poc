package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopybookDirectoriesTest {
    @Test
    void resolvesCopybooksAcrossDirectoriesWithFirstDirectoryPrecedence() throws Exception {
        Path first = Files.createTempDirectory("copybooks-first");
        Path second = Files.createTempDirectory("copybooks-second");
        Path firstCopybook = first.resolve("FIRST.cpy");
        Path secondCopybook = second.resolve("SECOND.cpy");
        Files.writeString(firstCopybook, "       01 FROM-FIRST PIC X.\n", StandardCharsets.UTF_8);
        Files.writeString(secondCopybook, "       01 FROM-SECOND PIC X.\n", StandardCharsets.UTF_8);
        Files.writeString(second.resolve("FIRST.cpy"), "       01 SHADOWED PIC X.\n", StandardCharsets.UTF_8);

        CopybookLibrary library = new CopybookLibrary(List.of(first, second));

        assertEquals(firstCopybook, library.resolve("FIRST").orElseThrow());
        assertEquals(secondCopybook, library.resolve("SECOND").orElseThrow());
    }

    @Test
    void explorerMainExpandsCopybookFromSecondCommandLineDirectory() throws Exception {
        Path source = Files.createTempFile("multiple-copybooks", ".cbl");
        Path first = Files.createTempDirectory("copybooks-cli-first");
        Path second = Files.createTempDirectory("copybooks-cli-second");
        Path output = Files.createTempDirectory("multiple-copybooks-output");
        Files.writeString(source, "       IDENTIFICATION DIVISION.\n"
                + "       PROGRAM-ID. MAIN.\n"
                + "       DATA DIVISION.\n"
                + "       WORKING-STORAGE SECTION.\n"
                + "       COPY SECOND.\n"
                + "       PROCEDURE DIVISION.\n"
                + "           GOBACK.\n"
                + "       END PROGRAM MAIN.\n", StandardCharsets.UTF_8);
        Files.writeString(second.resolve("SECOND.cpy"), "       01 SECOND-FIELD PIC X.\n", StandardCharsets.UTF_8);

        ExplorerMain.main(new String[]{
                "--source", source.toString(),
                "--copybooks", first + "," + second,
                "--output", output.toString()});

        String preprocessed = Files.readString(output.resolve("preprocessed.cbl"), StandardCharsets.UTF_8);
        String treeData = Files.readString(output.resolve("tree-data.js"), StandardCharsets.UTF_8);
        assertTrue(preprocessed.contains("SECOND-FIELD"));
        assertTrue(treeData.contains("\"unresolvedCopies\":0"));
    }
}
