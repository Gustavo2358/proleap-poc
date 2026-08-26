package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        String normalized = SourceNormalizerTestSupport.fixed(raw);

        assertFalse(normalized.contains("*>CE ENVIRONMENT DIVISION."), normalized);

        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(normalized, "comment-before-environment.cbl"))));
        ParseTree tree = binding.cobolStart(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());
        assertNotNull(firstRule(tree, parser, "environmentDivision"));
    }

    @Test
    void preservesSupportedPhysicalLineEndingsWithoutAddingPhantomRecords() {
        assertEquals("", SourceNormalizerTestSupport.fixed(""));
        assertEquals("DISPLAY 'A'.", SourceNormalizerTestSupport.fixed("       DISPLAY 'A'."));
        assertEquals("DISPLAY 'A'.\n", SourceNormalizerTestSupport.fixed("       DISPLAY 'A'.\n"));
        assertEquals("DISPLAY 'A'.\r\nDISPLAY 'B'.\r\n",
                SourceNormalizerTestSupport.fixed("       DISPLAY 'A'.\r\n       DISPLAY 'B'.\r\n"));
        assertEquals("DISPLAY 'A'.\rDISPLAY 'B'.",
                SourceNormalizerTestSupport.fixed("       DISPLAY 'A'.\r       DISPLAY 'B'."));
    }

    @Test
    void rejectsUnsupportedUnicodeLineSeparatorsExplicitly() {
        IllegalArgumentException failure = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SourceNormalizerTestSupport.fixed("       DISPLAY 'A'.\u2028       DISPLAY 'B'."));

        org.junit.jupiter.api.Assertions.assertTrue(
                failure.getMessage().contains("Unsupported line separator"), failure.getMessage());
    }

    @Test
    void normalizesInlineCommentEntriesWithoutChangingCobolText() throws Exception {
        String raw = Files.readString(INLINE_COMMENT_ENTRY, StandardCharsets.UTF_8);
        SourceNormalizer.Result result = SourceNormalizer.normalize(raw,
                INLINE_COMMENT_ENTRY.getFileName().toString(), SourceNormalizer.SourceFormat.FIXED);

        assertTrue(result.text().contains("ENVIRONMENT"));
        assertTrue(result.text().contains("MOVE"));
        assertTrue(result.text().contains("*>CE ORIGINAL AUTHOR."));
    }

    @Test
    void fixedFormatHasExplicitMarginsAndRejectsAmbiguousColumns() {
        String code = "000100 DISPLAY 'A'." + " ".repeat(72 - "000100 DISPLAY 'A'.".length())
                + "IDENTIFICATION-AREA\n";
        SourceNormalizer.Result result = SourceNormalizer.normalize(
                code, "margins.cbl", SourceNormalizer.SourceFormat.FIXED);

        assertEquals("DISPLAY 'A'." + " ".repeat(72 - "000100 DISPLAY 'A'.".length()) + "\n",
                result.text());
        assertEquals("", SourceNormalizerTestSupport.fixed("123456"),
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
        assertEquals("*>  ordinary comment\n", SourceNormalizerTestSupport.fixed(
                "      * ordinary comment\n"));
        assertEquals("*>  page eject comment\n", SourceNormalizerTestSupport.fixed(
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
                () -> SourceNormalizerTestSupport.fixed("      ?DISPLAY 'A'.\n"));
        org.junit.jupiter.api.Assertions.assertTrue(invalid.getMessage().contains("line 1"),
                invalid.getMessage());
        org.junit.jupiter.api.Assertions.assertTrue(invalid.getMessage().contains("column 7"),
                invalid.getMessage());
    }

    @Test
    void continuationUsesLexicalLiteralStateAndPreservesPhysicalRecords() {
        assertEquals("DISPLAY 'DON\"T AND MORE'.\n\nGOBACK.\n", SourceNormalizerTestSupport.fixed(
                "       DISPLAY 'DON\"T AND\n"
                        + "      -' MORE'.\n"
                        + "       GOBACK.\n"));
        assertEquals("DISPLAY \"DON'T AND MORE\".\n\n", SourceNormalizerTestSupport.fixed(
                "       DISPLAY \"DON'T AND\n"
                        + "      -\" MORE\".\n"));
        assertEquals("DISPLAY 'DON''T AND MORE'.\n\n", SourceNormalizerTestSupport.fixed(
                "       DISPLAY 'DON''T AND\n"
                        + "      -' MORE'.\n"));
        assertEquals("MOVE LONG-NAME TO TARGET.\n\n", SourceNormalizerTestSupport.fixed(
                "       MOVE LONG-\n"
                        + "      -NAME TO TARGET.\n"));
        assertEquals("DISPLAY X'ABCD', N\"EFGH\", Z'IJKL'.\n\n\n\n",
                SourceNormalizerTestSupport.fixed(
                        "       DISPLAY X'AB\n"
                                + "      -'CD', N\"EF\n"
                                + "      -\"GH\", Z'IJ\n"
                                + "      -'KL'.\n"));
        assertEquals("MOVE LONG_NAME TO TARGET.\n\n", SourceNormalizerTestSupport.fixed(
                "       MOVE LONG_\n"
                        + "      -NAME TO TARGET.\n"));
    }

    @Test
    void continuationRejectsOrphanIncompatibleAndMismatchedRecordsLocally() {
        IllegalArgumentException orphan = assertThrows(IllegalArgumentException.class,
                () -> SourceNormalizerTestSupport.fixed("      -ORPHAN\n"));
        assertTrue(orphan.getMessage().contains("line 1"), orphan.getMessage());
        assertTrue(orphan.getMessage().contains("orphan"), orphan.getMessage());

        IllegalArgumentException afterComment = assertThrows(IllegalArgumentException.class,
                () -> SourceNormalizerTestSupport.fixed("      * COMMENT\n      -TEXT\n"));
        assertTrue(afterComment.getMessage().contains("line 2"), afterComment.getMessage());
        assertTrue(afterComment.getMessage().contains("comment"), afterComment.getMessage());

        IllegalArgumentException afterInlineComment = assertThrows(IllegalArgumentException.class,
                () -> SourceNormalizerTestSupport.fixed(
                        "       DISPLAY 'DONE'. *> INLINE COMMENT\n      -TEXT\n"));
        assertTrue(afterInlineComment.getMessage().contains("line 2"),
                afterInlineComment.getMessage());
        assertTrue(afterInlineComment.getMessage().contains("inline comment"),
                afterInlineComment.getMessage());

        IllegalArgumentException mismatchedQuote = assertThrows(IllegalArgumentException.class,
                () -> SourceNormalizerTestSupport.fixed("       DISPLAY 'OPEN\n      -\"CLOSE'.\n"));
        assertTrue(mismatchedQuote.getMessage().contains("line 2"), mismatchedQuote.getMessage());
        assertTrue(mismatchedQuote.getMessage().contains("quote"), mismatchedQuote.getMessage());
    }

    @Test
    void continuationProducesSemanticallyContinuousText() {
        String raw = "       DISPLAY 'OPEN\n"
                + "      -' CONTINUED'.\n"
                + "       GOBACK.\n";
        SourceNormalizer.Result result = SourceNormalizer.normalize(
                raw, "continuation.cbl", SourceNormalizer.SourceFormat.FIXED);

        assertTrue(result.text().contains("DISPLAY 'OPEN CONTINUED'."));
        assertTrue(result.text().contains("GOBACK."));
    }

    @Test
    void commentEntryOwnersHaveAnExhaustiveNormalizationPolicy() {
        Set<String> grammarOwners = Arrays.stream(CobolParser.class.getDeclaredClasses())
                .filter(context -> Arrays.stream(context.getDeclaredMethods())
                        .anyMatch(method -> method.getParameterCount() == 0
                                && method.getReturnType() == CobolParser.CommentEntryContext.class))
                .map(Class::getSimpleName)
                .map(name -> Character.toLowerCase(name.charAt(0))
                        + name.substring(1, name.length() - "Context".length()))
                .collect(Collectors.toSet());

        assertEquals(grammarOwners, SourceNormalizer.commentEntryOwnerRules());

        Set<String> grammarQualifiers = Arrays.stream(
                        CobolParser.ProgramIdParagraphContext.class.getDeclaredMethods())
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> method.getReturnType()
                        == org.antlr.v4.runtime.tree.TerminalNode.class)
                .map(java.lang.reflect.Method::getName)
                .filter(token -> !Set.of("PROGRAM_ID", "IS", "PROGRAM").contains(token))
                .collect(Collectors.toSet());
        assertEquals(grammarQualifiers, SourceNormalizer.programIdQualifierTokens());
    }

    @Test
    void commentEntriesFollowFixedFormatParagraphBoundariesNotPeriods() {
        String raw = "       IDENTIFICATION DIVISION.\n"
                + "       PROGRAM-ID. COMMENTS. PROGRAM NOTE.\n"
                + "       AUTHOR. INLINE HAS. INTERNAL PERIODS.\n"
                + "           MULTILINE HAS. INTERNAL PERIODS.\n"
                + "\n"
                + "      * ORDINARY COMMENT INSIDE ENTRY\n"
                + "           FINAL WITHOUT PERIOD\n"
                + "       ENVIRONMENT DIVISION.\n";
        SourceNormalizer.Result result = SourceNormalizer.normalize(
                raw, "comment-entries.cbl", SourceNormalizer.SourceFormat.FIXED);

        assertEquals("IDENTIFICATION DIVISION.\n"
                        + "PROGRAM-ID. COMMENTS. *>CE PROGRAM NOTE.\n"
                        + "AUTHOR. *>CE INLINE HAS. INTERNAL PERIODS.\n"
                        + "*>CE MULTILINE HAS. INTERNAL PERIODS.\n"
                        + "\n"
                        + "*>  ORDINARY COMMENT INSIDE ENTRY\n"
                        + "*>CE FINAL WITHOUT PERIOD\n"
                        + "ENVIRONMENT DIVISION.\n",
                result.text());
        assertFalse(result.text().contains("*>CE ENVIRONMENT"), result.text());

        assertTrue(result.text().contains("ENVIRONMENT DIVISION."));
    }

    @Test
    void programIdCommentEntryHandlesSplitNameOptionalClauseAndOptionalPeriod() {
        assertEquals("PROGRAM-ID.\n    SPLIT.\n*>CE NOTE AFTER NAME.\nENVIRONMENT DIVISION.\n",
                SourceNormalizerTestSupport.fixed(
                        "       PROGRAM-ID.\n"
                                + "           SPLIT.\n"
                                + "           NOTE AFTER NAME.\n"
                                + "       ENVIRONMENT DIVISION.\n"));
        assertEquals("PROGRAM-ID. QUALIFIED IS INITIAL PROGRAM.\n"
                        + "*>CE QUALIFIED NOTE.\nENVIRONMENT DIVISION.\n",
                SourceNormalizerTestSupport.fixed(
                        "       PROGRAM-ID. QUALIFIED IS INITIAL PROGRAM.\n"
                                + "           QUALIFIED NOTE.\n"
                                + "       ENVIRONMENT DIVISION.\n"));
        assertEquals("PROGRAM-ID. NODOT\n*>CE NOTE WITHOUT PROGRAM PERIOD\n"
                        + "ENVIRONMENT DIVISION.\n",
                SourceNormalizerTestSupport.fixed(
                        "       PROGRAM-ID. NODOT\n"
                                + "           NOTE WITHOUT PROGRAM PERIOD\n"
                                + "       ENVIRONMENT DIVISION.\n"));
        assertEquals("PROGRAM-ID. 'Quoted''Name'. *>CE LITERAL NAME NOTE.\n",
                SourceNormalizerTestSupport.fixed(
                        "       PROGRAM-ID. 'Quoted''Name'. LITERAL NAME NOTE.\n"));
        assertEquals("PROGRAM-ID. N\"National\". *>CE NATIONAL NAME NOTE.\n",
                SourceNormalizerTestSupport.fixed(
                        "       PROGRAM-ID. N\"National\". NATIONAL NAME NOTE.\n"));
        assertEquals("PROGRAM-ID. MULTILINE\n"
                        + "    IS\n"
                        + "    INITIAL\n"
                        + "    PROGRAM\n"
                        + "    .\n"
                        + "*>CE COMMENT AFTER MULTILINE CLAUSE\n"
                        + "ENVIRONMENT DIVISION.\n",
                SourceNormalizerTestSupport.fixed(
                        "       PROGRAM-ID. MULTILINE\n"
                                + "           IS\n"
                                + "           INITIAL\n"
                                + "           PROGRAM\n"
                                + "           .\n"
                                + "           COMMENT AFTER MULTILINE CLAUSE\n"
                                + "       ENVIRONMENT DIVISION.\n"));
        assertEquals("PROGRAM-ID. QUALIFIER-SPLIT\n"
                        + "    RECURSIVE\n"
                        + "*>CE COMMENT AFTER QUALIFIER\n",
                SourceNormalizerTestSupport.fixed(
                        "       PROGRAM-ID. QUALIFIER-SPLIT\n"
                                + "           RECURSIVE\n"
                                + "           COMMENT AFTER QUALIFIER\n"));

        String complete = SourceNormalizerTestSupport.fixed(
                "       IDENTIFICATION DIVISION.\n"
                        + "       PROGRAM-ID. PARSE-SPLIT\n"
                        + "           IS\n"
                        + "           INITIAL\n"
                        + "           PROGRAM.\n"
                        + "           COMMENT ENTRY\n"
                        + "       ENVIRONMENT DIVISION.\n"
                        + "       DATA DIVISION.\n"
                        + "       PROCEDURE DIVISION.\n"
                        + "           GOBACK.\n");
        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(complete, "program-id-split.cbl"))));
        binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors(), complete);

        IllegalArgumentException missingName = assertThrows(IllegalArgumentException.class,
                () -> SourceNormalizerTestSupport.fixed("       PROGRAM-ID.\n"));
        assertTrue(missingName.getMessage().contains("PROGRAM_NAME_PENDING"),
                missingName.getMessage());
        IllegalArgumentException missingQualifier = assertThrows(IllegalArgumentException.class,
                () -> SourceNormalizerTestSupport.fixed("       PROGRAM-ID. FOO IS\n"));
        assertTrue(missingQualifier.getMessage().contains("PROGRAM_QUALIFIER_PENDING"),
                missingQualifier.getMessage());
    }

    @Test
    void normalizedCommentEntryMatrixParsesWithoutSyntaxErrors() {
        String raw = "       IDENTIFICATION DIVISION.\n"
                + "       PROGRAM-ID. COMMENT-MATRIX. PROGRAM COMMENT.\n"
                + "       AUTHOR. INLINE. AUTHOR TEXT.\n"
                + "           AUTHOR CONTINUATION WITHOUT FINAL PERIOD\n"
                + "       INSTALLATION.\n"
                + "\n"
                + "           INSTALLATION TEXT. WITH PERIOD.\n"
                + "       DATE-WRITTEN. 2026.08.25.\n"
                + "       DATE-COMPILED.\n"
                + "           NEVER\n"
                + "       SECURITY. NONE.\n"
                + "       REMARKS. REMARK ONE. REMARK TWO.\n"
                + "       ENVIRONMENT DIVISION.\n"
                + "       DATA DIVISION.\n"
                + "       PROCEDURE DIVISION.\n"
                + "           GOBACK.\n";
        String normalized = SourceNormalizerTestSupport.fixed(raw);

        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(normalized, "comment-matrix.cbl"))));
        binding.cobolStart(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors(), normalized);
    }

    @Test
    void remarksHasAnExplicitEndRemarksBoundaryAndHeadersRequireDotFs() {
        assertEquals("REMARKS.\n*>CE TEXT WITH PERIODS.\n    END-REMARKS.\n"
                        + "ENVIRONMENT DIVISION.\n",
                SourceNormalizerTestSupport.fixed(
                        "       REMARKS.\n"
                                + "           TEXT WITH PERIODS.\n"
                                + "           END-REMARKS.\n"
                                + "       ENVIRONMENT DIVISION.\n"));
        assertEquals("AUTHOR.X\n", SourceNormalizerTestSupport.fixed("       AUTHOR.X\n"),
                "a period without a following separator is not DOT_FS and cannot open an entry");
        assertEquals("AUTHOR.\n*>CE FINAL ENTRY AT EOF",
                SourceNormalizerTestSupport.fixed("       AUTHOR.\n           FINAL ENTRY AT EOF"));
    }

    @Test
    void commentEntriesDoNotCreateSemanticAstNodes() {
        String programPrefix = "       IDENTIFICATION DIVISION.\n"
                + "       PROGRAM-ID. NO-COMMENT-NODES.\n";
        String programSuffix = "       ENVIRONMENT DIVISION.\n"
                + "       DATA DIVISION.\n"
                + "       PROCEDURE DIVISION.\n"
                + "           GOBACK.\n";
        Ast.Program withoutEntries = buildNormalizedAst(programPrefix + programSuffix,
                "without-comment-entries.cbl");
        Ast.Program withEntries = buildNormalizedAst(programPrefix
                        + "       AUTHOR. INLINE AUTHOR.\n"
                        + "           SECOND AUTHOR RECORD.\n"
                        + "       REMARKS. TEXT. WITH. PERIODS.\n"
                        + programSuffix,
                "with-comment-entries.cbl");

        assertEquals(semanticNodeTypes(withoutEntries), semanticNodeTypes(withEntries));
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

    private static Ast.Program buildNormalizedAst(String raw, String file) {
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(
                raw, file, SourceNormalizer.SourceFormat.FIXED);
        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(normalized.text(), file))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors(), normalized.text());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        return new AstBuilder(parser, normalized.text(), ids, sizes)
                .build(tree).program();
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int child = 0; child < tree.getChildCount(); child++) {
            size += index(tree.getChild(child), ids, sizes, next);
        }
        sizes.put(tree, size);
        return size;
    }

    private static List<String> semanticNodeTypes(Ast.Node root) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        result.add(root.getClass().getSimpleName());
        for (Ast.Node child : Ast.children(root)) result.addAll(semanticNodeTypes(child));
        return result;
    }
}
