package io.proleap.benchmark;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Freezes the post-hardening behavior before reference resolution starts. */
class ReferenceResolutionBaselineCharacterizationTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/resolution/baseline-compilation-unit.cbl");

    @Test
    void currentlyBuildsOnlyTheFirstProgramUnit() throws Exception {
        Parsed parsed = parseFixture();

        assertEquals(2, directProgramUnits(parsed.tree()).size(),
                "the grammar recognizes both top-level program units");
        assertEquals("FIRST-PROGRAM", parsed.ast().name(),
                "the current AST builder deliberately freezes its first-program behavior");
        assertFalse(nodes(parsed.ast(), Ast.DataEntry.class).stream()
                .anyMatch(entry -> entry.name().equals("SECOND-VALUE")),
                "the second unit is not represented yet; phase 2 must change this fact");
    }

    @Test
    void currentlyIndexesSelectAndFdAsTwoFileDeclarations() throws Exception {
        SymbolTable table = new SymbolTableBuilder().build(parseFixture().ast());
        List<SymbolTable.Symbol> files = table.lookupAll(SymbolTable.Namespace.FILE, "TEST-FILE");

        assertEquals(2, files.size());
        assertEquals(Set.of(SymbolTable.SymbolKind.FILE_CONTROL, SymbolTable.SymbolKind.FILE_DESCRIPTION),
                files.stream().map(SymbolTable.Symbol::kind).collect(Collectors.toSet()));
    }

    @Test
    void exposesNominalReferencesButNoBindingInsideTheAst() throws Exception {
        Ast.Program ast = parseFixture().ast();

        assertEquals(1, nodes(ast, Ast.FileReference.class).size());
        assertTrue(nodes(ast, Ast.DataReference.class).stream()
                .anyMatch(reference -> reference.baseName().equals("WS-VALUE")));
        assertEquals(1, nodes(ast, Ast.ProgramReference.class).size());
        assertEquals(1, nodes(ast, Ast.ProcedureReference.class).size());

        Set<String> dataReferenceFields = List.of(Ast.DataReference.class.getRecordComponents()).stream()
                .map(RecordComponent::getName).collect(Collectors.toSet());
        Set<String> procedureReferenceFields = List.of(Ast.ProcedureReference.class.getRecordComponents()).stream()
                .map(RecordComponent::getName).collect(Collectors.toSet());
        assertTrue(Set.of("symbolId", "candidateIds", "resolutionStatus").stream()
                .noneMatch(dataReferenceFields::contains));
        assertTrue(Set.of("symbolId", "candidateIds", "resolutionStatus").stream()
                .noneMatch(procedureReferenceFields::contains));
    }

    private static Parsed parseFixture() throws Exception {
        Path file = FIXTURE.toAbsolutePath();
        String source = SourceNormalizer.fixed(Files.readString(file, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.proleap();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, file.getFileName().toString()))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());

        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        Ast.Program ast = new AstBuilder(parser, source,
                SourceMap.identity(source, file.getFileName().toString()), ids, sizes).build(tree).program();
        return new Parsed(tree, ast);
    }

    private static List<ParseTree> directProgramUnits(ParseTree tree) {
        ParseTree compilationUnit = tree.getChild(0);
        List<ParseTree> units = new ArrayList<>();
        for (int index = 0; index < compilationUnit.getChildCount(); index++) {
            ParseTree child = compilationUnit.getChild(index);
            if (child.getClass().getSimpleName().equals("ProgramUnitContext")) units.add(child);
        }
        return units;
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int child = 0; child < tree.getChildCount(); child++)
            size += index(tree.getChild(child), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return result;
    }

    private record Parsed(ParseTree tree, Ast.Program ast) { }
}
