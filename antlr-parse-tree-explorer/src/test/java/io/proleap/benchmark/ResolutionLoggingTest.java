package io.proleap.benchmark;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ResolutionLoggingTest {
    private static final Path FIXTURE = Path.of("src/test/resources/cobol/resolution/coverage-states.cbl");

    @Test
    void collectorAndResolverExposeSummariesAndDecisionsWithoutChangingProducts() throws Exception {
        Analysis baseline = analyze();
        Captured<ReferenceOccurrences> collected = capture(ReferenceOccurrenceCollector.class, Level.TRACE,
                () -> collect(baseline.model(), baseline.tables()));
        Captured<ReferenceResolution> resolved = capture(CobolReferenceResolver.class, Level.TRACE,
                () -> new CobolReferenceResolver(ResolutionContracts.CobolResolutionPolicy.initial(), Optional.empty())
                        .resolve(baseline.model(), baseline.tables(), Map.of(
                                baseline.model().programUnits().get(0).id(), collected.result())));

        assertEquals(baseline.occurrences().occurrences(), collected.result().occurrences());
        assertEquals(signature(baseline.resolution()), signature(resolved.result()));
        assertEvent(collected.events(), "event=references_collected", "programUnit=COVERAGESTATES", "total=");
        assertEvent(collected.events(), "event=reference_collected", "occurrenceId=", "kind=", "role=", "line=");
        assertEvent(resolved.events(), "event=resolution_completed", "references=", "resolved=", "unresolved=", "ambiguous=", "unsupported=", "candidateInspections=");
        assertEvent(resolved.events(), "event=reference_resolution", "candidateCount=", "status=", "reason=");
    }

    @Test
    void incompleteReportProducesOneAggregatedDegradationWarning(@TempDir Path directory) throws Exception {
        Path output = directory.resolve("output");
        List<ILoggingEvent> events = capture(ExplorerMain.class, Level.WARN,
                () -> { ExplorerMain.main(new String[]{"--source", FIXTURE.toAbsolutePath().toString(),
                        "--copybooks", Files.createDirectory(directory.resolve("cpy")).toString(),
                        "--output", output.toString()}); return null; }).events();

        List<ILoggingEvent> warnings = events.stream().filter(event -> event.getLevel() == Level.WARN).toList();
        assertEquals(1, warnings.size());
        assertEvent(warnings, "event=analysis_degraded", "phase=REFERENCE_RESOLUTION", "gaps=", "blockingReasons=", "fallback=RESULT_PUBLISHED_WITH_GAPS", "impact=DEPENDENCY_ANALYSIS_NOT_READY");
        assertTrue(Files.readString(output.resolve("resolution-data.js")).contains("\"dependencyAnalysisReady\":false"));
    }

    private static ReferenceOccurrences collect(CompilationUnitModel model, CompilationUnitSymbolTables tables) {
        CompilationUnitModel.ProgramUnit unit = model.programUnits().get(0);
        SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
        return new ReferenceOccurrenceCollector().collect(unit.id(), unit.program(), AstScopeIndex.build(unit.program(), table));
    }

    private static Analysis analyze() throws Exception {
        String source = SourceNormalizerTestSupport.fixed(Files.readString(FIXTURE, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.proleap();
        Parser parser = binding.cobolParser(new CommonTokenStream(binding.cobolLexer(CharStreams.fromString(source))));
        ParseTree tree = binding.cobolStart(parser);
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>(), sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, source, SourceMap.identity(source, "coverage-states.cbl"), ids, sizes).buildCompilationUnit(tree, "coverage-states.cbl");
        CompilationUnitModel model = build.compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        ReferenceOccurrences occurrences = collect(model, tables);
        ReferenceResolution resolution = new CobolReferenceResolver(ResolutionContracts.CobolResolutionPolicy.initial(), Optional.empty()).resolve(model, tables, Map.of(model.programUnits().get(0).id(), occurrences));
        return new Analysis(model, tables, occurrences, resolution);
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids, IdentityHashMap<ParseTree, Integer> sizes, int[] next) { ids.put(tree, next[0]++); int size = 1; for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next); sizes.put(tree, size); return size; }
    private static List<String> signature(ReferenceResolution resolution) { return resolution.entries().stream().map(entry -> entry.occurrence().id() + "|" + entry.status() + "|" + entry.reason() + "|" + entry.candidates().stream().map(candidate -> candidate.entityId().toString()).toList()).toList(); }
    private static void assertEvent(List<ILoggingEvent> events, String... fragments) { assertTrue(events.stream().map(ILoggingEvent::getFormattedMessage).anyMatch(message -> Arrays.stream(fragments).allMatch(message::contains)), () -> "missing " + List.of(fragments) + " in " + events.stream().map(ILoggingEvent::getFormattedMessage).toList()); }
    private static <T> Captured<T> capture(Class<?> owner, Level level, ThrowingSupplier<T> action) throws Exception { Logger logger = (Logger) LoggerFactory.getLogger(owner); Level previous = logger.getLevel(); boolean additive = logger.isAdditive(); SnapshotAppender appender = new SnapshotAppender(); appender.start(); logger.addAppender(appender); logger.setLevel(level); logger.setAdditive(false); try { T result = action.get(); return new Captured<>(List.copyOf(appender.list), result); } finally { logger.detachAppender(appender); logger.setLevel(previous); logger.setAdditive(additive); appender.stop(); } }
    @FunctionalInterface private interface ThrowingSupplier<T> { T get() throws Exception; }
    private record Captured<T>(List<ILoggingEvent> events, T result) { }
    private record Analysis(CompilationUnitModel model, CompilationUnitSymbolTables tables, ReferenceOccurrences occurrences, ReferenceResolution resolution) { }
    private static final class SnapshotAppender extends ListAppender<ILoggingEvent> { @Override protected void append(ILoggingEvent event) { event.prepareForDeferredProcessing(); super.append(event); } }
}
