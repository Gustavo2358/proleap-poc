package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ResolutionSnapshotTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/resolution/coverage-states.cbl");

    @Test
    void writesDeterministicTraceableAndConservativeBrowserProjection() throws Exception {
        String raw = Files.readString(FIXTURE, StandardCharsets.UTF_8);
        Analysis analysis = analyze(raw, FIXTURE.getFileName().toString());
        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(analysis.build(),
                ResolutionAnalysisReport.FrontendState.complete(), analysis.occurrences(),
                analysis.resolution());
        ResolutionSnapshot snapshot = ResolutionSnapshot.from(FIXTURE.getFileName().toString(),
                Arrays.asList(analysis.source().split("\\R", -1)), analysis.model(),
                analysis.resolution(), report);

        Path first = Files.createTempFile("resolution-one", ".js");
        Path second = Files.createTempFile("resolution-two", ".js");
        snapshot.write(first);
        snapshot.write(second);
        String text = Files.readString(first, StandardCharsets.UTF_8);

        assertEquals(text, Files.readString(second, StandardCharsets.UTF_8));
        assertTrue(text.startsWith("window.RESOLUTION_DATA={"));
        assertFalse(text.contains("\"catalog\":"));
        assertTrue(text.contains("\"policyId\":\"cobol-explorer/explicit-options\""));
        assertTrue(text.contains("\"pgmnameMode\":\"UNSPECIFIED\""));
        assertTrue(text.contains("\"status\":\"RESOLVED\""));
        assertTrue(text.contains("\"status\":\"AMBIGUOUS\""));
        assertTrue(text.contains("\"status\":\"UNRESOLVED\""));
        assertTrue(text.contains("\"status\":\"UNSUPPORTED\""));
        assertTrue(text.contains("\"candidates\":["));
        assertTrue(text.contains("\"astNodeId\":"));
        assertTrue(text.contains("\"parseNodeId\":"));
        assertTrue(text.contains("\"original\":{"));
        assertTrue(text.contains("\"gaps\":["));
        assertTrue(text.contains("\"dependencyAnalysisReady\":false"));
        assertEquals(1, occurrences(text, "\"sourceLines\":["),
                "the source must be stored once, never repeated for every occurrence");
    }

    @Test
    void webPageDeclaresAllRequiredFiltersAndNoExternalDependency() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/web/resolution.html"));
        String script = Files.readString(Path.of("src/main/resources/web/resolution-app.js"));

        for (String filter : List.of("unit-filter", "kind-filter", "role-filter",
                "status-filter", "reason-filter", "resolution-search"))
            assertTrue(html.contains("id=\"" + filter + "\""), filter);
        assertTrue(html.contains("id=\"candidate-inspector\""));
        assertTrue(html.contains("id=\"coverage-panel\""));
        assertTrue(script.contains("RESOLUTION_DATA"));
        assertFalse(html.contains("http://"));
        assertFalse(html.contains("https://"));
        assertFalse(script.contains("fetch("));
    }

    @Test
    void explorerPipelineEmitsResolutionAsASeparateFourthProduct() throws Exception {
        Path output = Files.createTempDirectory("resolution-explorer-output");
        Path copybooks = Files.createTempDirectory("resolution-empty-copybooks");

        ExplorerMain.main(new String[]{"--source", FIXTURE.toAbsolutePath().toString(),
                "--copybooks", copybooks.toString(), "--output", output.toString()});

        for (String artifact : List.of("index.html", "ast.html", "symbols.html",
                "resolution.html", "tree-data.js", "ast-data.js", "symbol-data.js",
                "resolution-data.js"))
            assertTrue(Files.isRegularFile(output.resolve(artifact)), artifact);
        String resolution = Files.readString(output.resolve("resolution-data.js"));
        assertFalse(resolution.contains("\"catalog\":"));
        assertTrue(resolution.contains("\"qualifyMode\":\"UNSPECIFIED\""));
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int from = 0; (from = value.indexOf(needle, from)) >= 0; from += needle.length()) count++;
        return count;
    }

    private static Analysis analyze(String rawSource, String sourceName) throws Exception {
        String source = SourceNormalizerTestSupport.fixed(rawSource);
        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, sourceName))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, source,
                SourceMap.identity(source, sourceName), ids, sizes).buildCompilationUnit(tree, sourceName);
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
        return new Analysis(source, build, model, occurrences, resolution);
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

    private record Analysis(String source, CompilationUnitBuildResult build,
                            CompilationUnitModel model,
                            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences,
                            ReferenceResolution resolution) { }
}
