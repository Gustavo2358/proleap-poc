package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ExternalClassificationTestSupport {
    private ExternalClassificationTestSupport() { }

    static Analysis analyze(Path sourcePath) throws Exception {
        Path file = sourcePath.toAbsolutePath();
        return analyze(Files.readString(file, StandardCharsets.UTF_8), file.getFileName().toString());
    }

    static Analysis analyze(String rawSource, String sourceName) throws Exception {
        String source = SourceNormalizerTestSupport.fixed(rawSource);
        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, sourceName))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors(), "fixture must parse without recovery");
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, source,
                SourceMap.identity(source, sourceName), ids, sizes)
                .buildCompilationUnit(tree, sourceName);
        CompilationUnitModel model = build.compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector().collect(unit.id(), unit.program(),
                    AstScopeIndex.build(unit.program(), table)));
        }
        ReferenceResolution resolution = new CobolReferenceResolver(
                ResolutionContracts.CobolResolutionPolicy.initial())
                .resolve(model, tables, occurrences);
        return new Analysis(source, build, model, tables, occurrences, resolution);
    }

    static java.util.List<Ast.Node> nodes(Ast.Node root) {
        java.util.List<Ast.Node> result = new java.util.ArrayList<>();
        result.add(root);
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child));
        return java.util.List.copyOf(result);
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

    record Analysis(String source, CompilationUnitBuildResult build, CompilationUnitModel model,
                    CompilationUnitSymbolTables tables,
                    Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences,
                    ReferenceResolution resolution) { }
}
