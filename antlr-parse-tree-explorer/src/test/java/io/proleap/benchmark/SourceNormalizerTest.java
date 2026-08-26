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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void indicatorAreaHasAnExplicitPolicyForEverySupportedKind() {
        assertEquals("*>  ordinary comment\n", SourceNormalizer.fixed(
                "      * ordinary comment\n"));
        assertEquals("*>  page eject comment\n", SourceNormalizer.fixed(
                "      / page eject comment\n"));

        SourceNormalizer.Options excludeDebug = new SourceNormalizer.Options(
                SourceNormalizer.SourceFormat.FIXED,
                SourceNormalizer.DebugLinePolicy.EXCLUDE);
        SourceNormalizer.Options includeDebug = new SourceNormalizer.Options(
                SourceNormalizer.SourceFormat.FIXED,
                SourceNormalizer.DebugLinePolicy.INCLUDE);
        assertEquals("*> DEBUG DISPLAY 'D'.\n", SourceNormalizer.normalize(
                "      DDISPLAY 'D'.\n", "debug.cbl", excludeDebug).text());
        assertEquals("DISPLAY 'D'.\n", SourceNormalizer.normalize(
                "      dDISPLAY 'D'.\n", "debug.cbl", includeDebug).text());

        IllegalArgumentException invalid = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SourceNormalizer.fixed("      ?DISPLAY 'A'.\n"));
        org.junit.jupiter.api.Assertions.assertTrue(invalid.getMessage().contains("line 1"),
                invalid.getMessage());
        org.junit.jupiter.api.Assertions.assertTrue(invalid.getMessage().contains("column 7"),
                invalid.getMessage());
    }

    @Test
    void continuationUsesLexicalLiteralStateAndPreservesPhysicalRecords() {
        assertEquals("DISPLAY 'DON\"T AND MORE'.\n\nGOBACK.\n", SourceNormalizer.fixed(
                "       DISPLAY 'DON\"T AND\n"
                        + "      -' MORE'.\n"
                        + "       GOBACK.\n"));
        assertEquals("DISPLAY \"DON'T AND MORE\".\n\n", SourceNormalizer.fixed(
                "       DISPLAY \"DON'T AND\n"
                        + "      -\" MORE\".\n"));
        assertEquals("DISPLAY 'DON''T AND MORE'.\n\n", SourceNormalizer.fixed(
                "       DISPLAY 'DON''T AND\n"
                        + "      -' MORE'.\n"));
        assertEquals("MOVE LONG-NAME TO TARGET.\n\n", SourceNormalizer.fixed(
                "       MOVE LONG-\n"
                        + "      -NAME TO TARGET.\n"));
        assertEquals("DISPLAY X'ABCD', N\"EFGH\", Z'IJKL'.\n\n\n\n",
                SourceNormalizer.fixed(
                        "       DISPLAY X'AB\n"
                                + "      -'CD', N\"EF\n"
                                + "      -\"GH\", Z'IJ\n"
                                + "      -'KL'.\n"));
        assertEquals("MOVE LONG_NAME TO TARGET.\n\n", SourceNormalizer.fixed(
                "       MOVE LONG_\n"
                        + "      -NAME TO TARGET.\n"));
    }

    @Test
    void continuationRejectsOrphanIncompatibleAndMismatchedRecordsLocally() {
        IllegalArgumentException orphan = assertThrows(IllegalArgumentException.class,
                () -> SourceNormalizer.fixed("      -ORPHAN\n"));
        assertTrue(orphan.getMessage().contains("line 1"), orphan.getMessage());
        assertTrue(orphan.getMessage().contains("orphan"), orphan.getMessage());

        IllegalArgumentException afterComment = assertThrows(IllegalArgumentException.class,
                () -> SourceNormalizer.fixed("      * COMMENT\n      -TEXT\n"));
        assertTrue(afterComment.getMessage().contains("line 2"), afterComment.getMessage());
        assertTrue(afterComment.getMessage().contains("comment"), afterComment.getMessage());

        IllegalArgumentException mismatchedQuote = assertThrows(IllegalArgumentException.class,
                () -> SourceNormalizer.fixed("       DISPLAY 'OPEN\n      -\"CLOSE'.\n"));
        assertTrue(mismatchedQuote.getMessage().contains("line 2"), mismatchedQuote.getMessage());
        assertTrue(mismatchedQuote.getMessage().contains("quote"), mismatchedQuote.getMessage());
    }

    @Test
    void continuationSourceMapKeepsRawLineCoordinates() {
        String raw = "       DISPLAY 'OPEN\n"
                + "      -' CONTINUED'.\n"
                + "       GOBACK.\n";
        SourceNormalizer.Result result = SourceNormalizer.normalize(
                raw, "continuation.cbl", SourceNormalizer.SourceFormat.FIXED);

        int displayStart = result.text().indexOf("DISPLAY");
        Ast.SourceProvenance combined = result.sourceMap().provenance(
                displayStart, result.text().indexOf(".\n") + 1);
        assertEquals(1, combined.original().startLine());
        assertFalse(combined.exact());

        int gobackStart = result.text().indexOf("GOBACK");
        Ast.SourceProvenance goback = result.sourceMap().provenance(gobackStart, gobackStart + 6);
        assertEquals(3, goback.original().startLine());
        assertEquals(7, goback.original().startColumn());
        assertTrue(goback.exact());
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
