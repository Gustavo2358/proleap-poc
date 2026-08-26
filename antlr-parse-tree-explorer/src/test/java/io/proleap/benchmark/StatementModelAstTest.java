package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StatementModelAstTest {
    @Test
    void everyGrammarStatementAlternativeHasAnExplicitBuilderPath() throws Exception {
        String grammar = Files.readString(Path.of("src/main/antlr4/Cobol.g4"), StandardCharsets.UTF_8);
        String alternatives = grammar.substring(grammar.indexOf("statement\n   :") + 14,
                grammar.indexOf("\n   ;", grammar.indexOf("statement\n   :")));
        Set<String> grammarRules = new LinkedHashSet<>(List.of(alternatives
                .replace(":", " ").replace("|", " ").trim().split("\\s+")));
        assertEquals(50, grammarRules.size());
        assertEquals(grammarRules, AstBuilder.supportedStatementRules());
    }

    @Test
    void parsesAllAlternativesWithoutUnsupportedFallbackAndPreservesOperands() throws Exception {
        AstBuildResult result = parse();
        Set<String> findings = new LinkedHashSet<>();
        result.coverage().findings().forEach(f -> findings.add(f.grammarRule()));
        Set<String> missing = new LinkedHashSet<>(AstBuilder.supportedStatementRules());
        missing.removeAll(findings);
        assertTrue(missing.isEmpty(), "missing fixture alternatives: " + missing);
        assertTrue(nodes(result.program(), Ast.UnsupportedStatement.class).isEmpty());
        assertFalse(nodes(result.program(), Ast.ModeledStatement.class).isEmpty());
        assertEquals(16, nodes(result.program(), Ast.PreservedStatement.class).stream()
                .map(Ast.PreservedStatement::grammarRule).distinct().count());
        assertTrue(nodes(result.program(), Ast.StatementOperand.class).stream()
                .anyMatch(o -> o.value() instanceof Ast.FileReference));
        assertTrue(nodes(result.program(), Ast.StatementOperand.class).stream()
                .anyMatch(o -> o.value() instanceof Ast.ProcedureReference));
    }

    @Test
    void callPreservesModesOmittedAddressLengthAndReturning() throws Exception {
        Ast.CallStatement call = nodes(parse().program(), Ast.CallStatement.class).get(0);
        assertEquals(List.of(Ast.PassingMode.REFERENCE, Ast.PassingMode.REFERENCE,
                        Ast.PassingMode.VALUE, Ast.PassingMode.CONTENT),
                call.arguments().stream().map(Ast.CallArgument::passingMode).toList());
        assertEquals(Ast.CallArgumentKind.OMITTED, call.arguments().get(1).argumentKind());
        assertNull(call.arguments().get(1).value());
        assertEquals(Ast.CallArgumentKind.LENGTH_OF, call.arguments().get(2).argumentKind());
        assertEquals("B", ((Ast.DataReference) call.returning()).baseName());
    }

    private static AstBuildResult parse() throws Exception {
        Path file = Path.of("src/test/resources/cobol/semantic/statements.cbl").toAbsolutePath();
        String source = SourceNormalizerTestSupport.fixed(Files.readString(file, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.proleap();
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = binding.cobolParser(tokens);
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>(), sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        return new AstBuilder(parser, source, SourceMap.identity(source, "statements.cbl"), ids, sizes).build(tree);
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++); int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size); return size;
    }

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return result;
    }
}
