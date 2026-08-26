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
