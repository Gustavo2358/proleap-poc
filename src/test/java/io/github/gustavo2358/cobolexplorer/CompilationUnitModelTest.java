package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CompilationUnitModelTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/resolution/compilation-unit-visibility.cbl");

    @Test
    void preservesEveryTopLevelAndNestedProgramWithDeterministicIdentity() throws Exception {
        Parsed parsed = parseFixture();
        CompilationUnitBuildResult result = parsed.builder().buildCompilationUnit(
                parsed.tree(), "compilation-unit-visibility.cbl");
        List<CompilationUnitModel.ProgramUnit> units = result.compilationUnit().programUnits();

        assertEquals(countRules(parsed.tree(), "programUnit"), units.size(),
                "no grammar-recognized programUnit may disappear during AST construction");
        assertEquals(List.of("OUTER-PROGRAM", "CHILD-ONE", "GRANDCHILD", "CHILD-TWO", "SECOND-TOP"),
                units.stream().map(unit -> unit.program().name()).toList());
        assertEquals(List.of(List.of(0), List.of(0, 0), List.of(0, 0, 0), List.of(0, 1), List.of(1)),
                units.stream().map(unit -> unit.id().structuralPath()).toList());

        CompilationUnitModel.ProgramUnit outer = units.get(0);
        CompilationUnitModel.ProgramUnit child = units.get(1);
        CompilationUnitModel.ProgramUnit grandchild = units.get(2);
        assertNull(outer.parentId());
        assertEquals(outer.id(), child.parentId());
        assertEquals(child.id(), grandchild.parentId());
        assertEquals("COMPILATION-UNIT-VISIBILITY.CBL", outer.id().compilationUnitId());
        assertEquals("OUTER-PROGRAM", outer.id().canonicalProgramName());

        assertEquals(0, outer.program().meta().id(), "AST ids are local to a ProgramUnitId namespace");
        assertEquals(0, child.program().meta().id(), "each program unit starts its local AST id namespace");
        assertTrue(result.coverageByProgramUnit().keySet().containsAll(
                units.stream().map(CompilationUnitModel.ProgramUnit::id).toList()));
        assertTrue(result.diagnosticsByProgramUnit().keySet().containsAll(
                units.stream().map(CompilationUnitModel.ProgramUnit::id).toList()));
    }

    @Test
    void preservesProgramDataAndFileVisibilityAsTypedSemanticFacts() throws Exception {
        CompilationUnitBuildResult result = parseFixture().build();
        Map<String, Ast.Program> programs = new LinkedHashMap<>();
        result.compilationUnit().programUnits().forEach(unit -> programs.put(unit.program().name(), unit.program()));

        assertTrue(programs.get("OUTER-PROGRAM").attributes().common());
        assertTrue(programs.get("CHILD-ONE").attributes().initial());
        assertTrue(programs.get("CHILD-TWO").attributes().recursive());
        assertFalse(programs.get("SECOND-TOP").attributes().common());

        Ast.Program outer = programs.get("OUTER-PROGRAM");
        Ast.DataEntry global = nodes(outer, Ast.DataEntry.class).stream()
                .filter(entry -> entry.name().equals("SHARED-NAME")).findFirst().orElseThrow();
        Ast.DataEntry external = nodes(outer, Ast.DataEntry.class).stream()
                .filter(entry -> entry.name().equals("EXTERNAL-NAME")).findFirst().orElseThrow();
        Ast.FileDescription file = nodes(outer, Ast.FileDescription.class).get(0);

        assertEquals(Ast.DeclarationVisibility.GLOBAL, global.visibility());
        assertEquals(Ast.DeclarationVisibility.EXTERNAL, external.visibility());
        assertEquals(Ast.DeclarationVisibility.GLOBAL, file.visibility());
        assertTrue(global.clauses().stream().anyMatch(Ast.PreservedDataClause.class::isInstance),
                "typed visibility must not discard the original accepted clause");
    }

    @Test
    void buildsOneDeclarationTablePerProgramAndKeepsAncestryWithoutBindingUses() throws Exception {
        CompilationUnitModel model = parseFixture().build().compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        List<CompilationUnitModel.ProgramUnit> units = model.programUnits();

        assertEquals(units.size(), tables.units().size());
        CompilationUnitSymbolTables.UnitSymbols outer = tables.forProgramUnit(units.get(0).id()).orElseThrow();
        CompilationUnitSymbolTables.UnitSymbols child = tables.forProgramUnit(units.get(1).id()).orElseThrow();
        assertNull(outer.parentId());
        assertEquals(outer.id(), child.parentId());

        SymbolTable.Symbol outerShared = outer.symbolTable().lookupAll(SymbolTable.Namespace.DATA, "SHARED-NAME")
                .stream().findFirst().orElseThrow();
        SymbolTable.Symbol childShared = child.symbolTable().lookupAll(SymbolTable.Namespace.DATA, "SHARED-NAME")
                .stream().findFirst().orElseThrow();
        assertEquals("GLOBAL", outerShared.attributes().get("visibility"));
        assertEquals("LOCAL", childShared.attributes().get("visibility"));
        assertNotEquals(outer.id(), child.id(), "equal local symbol ids remain namespaced by ProgramUnitId");
        assertEquals("NOT_PERFORMED", tables.bindingStatus());
    }

    private static Parsed parseFixture() throws Exception {
        Path file = FIXTURE.toAbsolutePath();
        String source = SourceNormalizerTestSupport.fixed(Files.readString(file, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, file.getFileName().toString()))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        AstBuilder builder = new AstBuilder(parser, source, ids, sizes);
        return new Parsed(tree, builder);
    }

    private static int countRules(ParseTree tree, String suffix) {
        int count = tree.getClass().getSimpleName().equalsIgnoreCase(suffix + "Context") ? 1 : 0;
        for (int i = 0; i < tree.getChildCount(); i++) count += countRules(tree.getChild(i), suffix);
        return count;
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return result;
    }

    private record Parsed(ParseTree tree, AstBuilder builder) {
        CompilationUnitBuildResult build() {
            return builder.buildCompilationUnit(tree, "compilation-unit-visibility.cbl");
        }
    }
}
