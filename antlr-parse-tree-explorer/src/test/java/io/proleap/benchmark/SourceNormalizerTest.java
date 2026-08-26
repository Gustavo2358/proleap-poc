package io.proleap.benchmark;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SourceNormalizerTest {
    private static final Path COMMENT_BEFORE_ENVIRONMENT = Path.of(
            "src/test/resources/cobol/source-format/comment-before-environment.cbl");
    private static final Path INLINE_COMMENT_ENTRY = Path.of(
            "src/test/resources/cobol/source-format/inline-comment-entry.cbl");

    @Test
    void fixedCommentAfterEmptyCommentEntryDoesNotConsumeEnvironmentDivision() throws Exception {
        String raw = Files.readString(COMMENT_BEFORE_ENVIRONMENT, StandardCharsets.UTF_8);
        String normalized = SourceNormalizer.fixed(raw);

        assertFalse(normalized.contains("*>CE ENVIRONMENT DIVISION."), normalized);

        GrammarBinding binding = Bindings.proleap();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(normalized, "comment-before-environment.cbl"))));
        ParseTree tree = binding.cobolStart(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());
        assertNotNull(firstRule(tree, parser, "environmentDivision"));
    }

    @Test
    void preservesSupportedPhysicalLineEndingsWithoutAddingPhantomRecords() {
        assertEquals("", SourceNormalizer.fixed(""));
        assertEquals("DISPLAY 'A'.", SourceNormalizer.fixed("       DISPLAY 'A'."));
        assertEquals("DISPLAY 'A'.\n", SourceNormalizer.fixed("       DISPLAY 'A'.\n"));
        assertEquals("DISPLAY 'A'.\r\nDISPLAY 'B'.\r\n",
                SourceNormalizer.fixed("       DISPLAY 'A'.\r\n       DISPLAY 'B'.\r\n"));
        assertEquals("DISPLAY 'A'.\rDISPLAY 'B'.",
                SourceNormalizer.fixed("       DISPLAY 'A'.\r       DISPLAY 'B'."));
    }

    @Test
    void rejectsUnsupportedUnicodeLineSeparatorsExplicitly() {
        IllegalArgumentException failure = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SourceNormalizer.fixed("       DISPLAY 'A'.\u2028       DISPLAY 'B'."));

        org.junit.jupiter.api.Assertions.assertTrue(
                failure.getMessage().contains("Unsupported line separator"), failure.getMessage());
    }

    @Test
    void mapsNormalizedTextBackToPhysicalLinesAndColumns() throws Exception {
        String raw = Files.readString(INLINE_COMMENT_ENTRY, StandardCharsets.UTF_8);
        SourceNormalizer.Result result = SourceNormalizer.normalize(raw,
                INLINE_COMMENT_ENTRY.getFileName().toString(), SourceNormalizer.SourceFormat.FIXED);

        int environmentStart = result.text().indexOf("ENVIRONMENT");
        Ast.SourceProvenance environment = result.sourceMap().provenance(
                environmentStart, environmentStart + "ENVIRONMENT".length());
        assertEquals(4, environment.original().startLine());
        assertEquals(7, environment.original().startColumn());
        org.junit.jupiter.api.Assertions.assertTrue(environment.exact());

        int moveStart = result.text().indexOf("MOVE");
        Ast.SourceProvenance move = result.sourceMap().provenance(moveStart, moveStart + 4);
        assertEquals(9, move.original().startLine());
        assertEquals(11, move.original().startColumn());
        org.junit.jupiter.api.Assertions.assertTrue(move.exact());

        int commentEntryStart = result.text().indexOf("*>CE ORIGINAL AUTHOR.");
        Ast.SourceProvenance commentEntry = result.sourceMap().provenance(
                commentEntryStart, commentEntryStart + "*>CE ORIGINAL AUTHOR.".length());
        assertEquals(3, commentEntry.original().startLine());
        org.junit.jupiter.api.Assertions.assertFalse(commentEntry.exact());
    }

    @Test
    void fixedFormatHasExplicitMarginsAndRejectsAmbiguousColumns() {
        String code = "000100 DISPLAY 'A'." + " ".repeat(72 - "000100 DISPLAY 'A'.".length())
                + "IDENTIFICATION-AREA\n";
        SourceNormalizer.Result result = SourceNormalizer.normalize(
                code, "margins.cbl", SourceNormalizer.SourceFormat.FIXED);

        assertEquals("DISPLAY 'A'." + " ".repeat(72 - "000100 DISPLAY 'A'.".length()) + "\n",
                result.text());
        assertEquals("", SourceNormalizer.fixed("123456"),
                "a short record containing only sequence-area columns has no program text");

        IllegalArgumentException tab = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SourceNormalizer.normalize("      \tDISPLAY 'A'.", "tab.cbl",
                        SourceNormalizer.SourceFormat.FIXED));
        org.junit.jupiter.api.Assertions.assertTrue(tab.getMessage().contains("tab"), tab.getMessage());

        IllegalArgumentException nonAscii = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SourceNormalizer.normalize("       DISPLAY 'Á'.", "non-ascii.cbl",
                        SourceNormalizer.SourceFormat.FIXED));
        org.junit.jupiter.api.Assertions.assertTrue(
                nonAscii.getMessage().contains("non-ASCII"), nonAscii.getMessage());
    }

    private static ParserRuleContext firstRule(ParseTree tree, Parser parser, String expected) {
        if (tree instanceof ParserRuleContext context
                && parser.getRuleNames()[context.getRuleIndex()].equals(expected)) return context;
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParserRuleContext found = firstRule(tree.getChild(i), parser, expected);
            if (found != null) return found;
        }
        return null;
    }
}
