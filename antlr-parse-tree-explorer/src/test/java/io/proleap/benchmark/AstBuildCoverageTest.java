package io.proleap.benchmark;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstBuildCoverageTest {

    @Test
    void returnsAstCoverageAndDiagnosticsAsSeparateProducts() {
        String source = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 FLAG PIC X.
                PROCEDURE DIVISION.
                    MOVE 'X' TO FLAG.
                    SET FLAG TO TRUE.
                    GOBACK.
                """;
        GrammarBinding binding = Bindings.proleap();
        CommonTokenStream tokens = new CommonTokenStream(binding.cobolLexer(CharStreams.fromString(source)));
        Parser parser = binding.cobolParser(tokens);
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());

        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});

        AstBuilder builder = new AstBuilder(parser, source, ids, sizes);
        AstBuildResult result = builder.build(tree);
        assertEquals("SAMPLE", result.program().name());
        assertTrue(result.diagnostics().isEmpty());
        assertEquals(List.of("moveStatement", "setStatement", "gobackStatement"),
                result.coverage().findings().stream().map(SemanticCoverage.Finding::grammarRule).toList());
        assertEquals(SemanticCoverage.ConstructionCoverage.MODELED,
                result.coverage().findings().get(0).coverage());
        assertEquals(SemanticCoverage.ConstructionCoverage.MODELED,
                result.coverage().findings().get(1).coverage());
        assertEquals(SemanticCoverage.ConstructionCoverage.MODELED,
                result.coverage().findings().get(2).coverage());
        assertTrue(result.coverage().dependencyCoverageComplete());

        AstBuildResult repeated = builder.build(tree);
        assertEquals(result.program(), repeated.program());
        assertEquals(result.coverage(), repeated.coverage());
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }
}
