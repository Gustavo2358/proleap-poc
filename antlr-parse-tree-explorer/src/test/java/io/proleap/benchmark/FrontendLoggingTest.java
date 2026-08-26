package io.proleap.benchmark;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.antlr.v4.runtime.CommonToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FrontendLoggingTest {
    @Test
    void normalizerTracesExceptionalDecisionsButNotOrdinaryLines() throws Exception {
        String raw = "       DISPLAY 'A'.\n"
                + "      / PAGE EJECT\n"
                + "      DDISPLAY 'DEBUG'.\n"
                + "       MOVE LONG-\n"
                + "      -NAME TO TARGET.\n";

        List<ILoggingEvent> events = capture(SourceNormalizer.class, Level.TRACE,
                () -> SourceNormalizer.normalize(raw, "decisions.cbl",
                        new SourceNormalizer.Options(SourceNormalizer.SourceFormat.FIXED,
                                SourceNormalizer.DebugLinePolicy.EXCLUDE)));

        assertEquals(1, count(events, "event=normalization_completed"));
        assertEquals(1, count(events, "event=page_eject_normalized"));
        assertEquals(1, count(events, "event=debug_line_policy_applied"));
        assertEquals(1, count(events, "event=continuation_resolved"));
        assertEquals(4, events.size(), "ordinary physical lines must not generate trace events");
    }

    @Test
    void repeatedMissingCopiesProduceDiagnosticsButOneAggregatedWarning(@TempDir Path copybooks)
            throws Exception {
        StringBuilder source = new StringBuilder();
        for (int index = 0; index < 100; index++) {
            source.append("COPY MISSING").append(index).append(".\n");
        }
        PreprocessorEngine engine = new PreprocessorEngine(
                Bindings.proleap(), new CopybookLibrary(copybooks));

        Captured<PreprocessorEngine.Outcome> captured = captureResult(
                PreprocessorEngine.class, Level.WARN,
                () -> engine.process(SourceMap.identity(source.toString(), "many-copies.cbl"),
                        "many-copies.cbl"));

        assertEquals(100, captured.result().unresolved());
        assertEquals(100, captured.result().diagnostics().stream()
                .filter(diagnostic -> diagnostic.message().startsWith("unresolved_copy")).count());
        assertEquals(1, captured.events().size());
        String warning = captured.events().get(0).getFormattedMessage();
        assertAll(
                () -> assertTrue(warning.contains("event=copy_unresolved")),
                () -> assertTrue(warning.contains("count=100")),
                () -> assertTrue(warning.contains("fallback=KEEP_UNRESOLVED_PLACEHOLDER")),
                () -> assertTrue(warning.contains("impact=ANALYSIS_INCOMPLETE")));
    }

    @Test
    void cycleAndIoFallbacksEachExposeReasonFallbackAndImpact(@TempDir Path copybooks)
            throws Exception {
        Files.writeString(copybooks.resolve("CYCLE.cpy"), "       COPY CYCLE.\n");
        Files.writeString(copybooks.resolve("UNREADABLE.cpy"), "       01 VALUE-A PIC X.\n");
        CopybookLibrary library = new CopybookLibrary(copybooks);
        Files.delete(copybooks.resolve("UNREADABLE.cpy"));
        PreprocessorEngine engine = new PreprocessorEngine(Bindings.proleap(), library);

        List<ILoggingEvent> cycle = capture(PreprocessorEngine.class, Level.WARN,
                () -> engine.process(SourceMap.identity("COPY CYCLE.\n", "cycle-main.cbl"),
                        "cycle-main.cbl"));
        List<ILoggingEvent> io = capture(PreprocessorEngine.class, Level.WARN,
                () -> engine.process(SourceMap.identity("COPY UNREADABLE.\n", "io-main.cbl"),
                        "io-main.cbl"));

        assertEquals(1, cycle.size());
        assertTrue(cycle.get(0).getFormattedMessage().contains(
                "event=copy_cycle source=cycle-main.cbl phase=PREPROCESSING count=1 reason=EXPANSION_CYCLE fallback=KEEP_CYCLIC_PLACEHOLDER impact=ANALYSIS_INCOMPLETE"));
        assertEquals(1, io.size());
        assertTrue(io.get(0).getFormattedMessage().contains(
                "event=copy_io_failure source=io-main.cbl phase=PREPROCESSING count=1 reason=IO_EXCEPTION fallback=KEEP_IO_ERROR_PLACEHOLDER impact=ANALYSIS_INCOMPLETE"));
    }

    @Test
    void preprocessorTraceExplainsPoliciesCopyReplacementOptionsAndOpaqueLanguage(
            @TempDir Path copybooks) throws Exception {
        Files.writeString(copybooks.resolve("ITEM.cpy"), "       01 OLD PIC X.\n");
        PreprocessorEngine engine = new PreprocessorEngine(
                Bindings.proleap(), new CopybookLibrary(copybooks));
        String source = "CBL DYNAM\n"
                + "COPY ITEM REPLACING ==OLD== BY ==NEW==.\n"
                + "EXEC CICS RETURN END-EXEC.\n";

        List<ILoggingEvent> events = capture(PreprocessorEngine.class, Level.TRACE,
                () -> engine.process(SourceMap.identity(source, "trace.cbl"), "trace.cbl"));

        for (String event : List.of("preprocess_policy_selected", "compiler_option_detected",
                "copy_resolved", "copy_replacing_applied", "embedded_language_preserved")) {
            assertTrue(events.stream().anyMatch(log -> log.getFormattedMessage().contains("event=" + event)),
                    () -> "missing trace event " + event + " in "
                            + events.stream().map(ILoggingEvent::getFormattedMessage).toList());
        }
    }

    @Test
    void individualAntlrDiagnosticsNeverProduceWarnings() throws Exception {
        List<Diagnostic> diagnostics = new ArrayList<>();
        AntlrDiagnosticListener listener = new AntlrDiagnosticListener(
                "test", Diagnostic.Phase.PARSER, "broken.cbl", diagnostics);

        List<ILoggingEvent> events = capture(AntlrDiagnosticListener.class, Level.WARN, () -> {
            for (int index = 0; index < 250; index++) {
                listener.syntaxError(null, new CommonToken(1, "BAD"), index + 1, 7,
                        "synthetic parser error", null);
            }
        });

        assertEquals(250, diagnostics.size());
        assertTrue(events.isEmpty(), "Diagnostic is the detail product; WARN must be aggregated elsewhere");
    }

    @Test
    void parserDegradationIsSummarizedOnceAfterRecovery(@TempDir Path directory) throws Exception {
        Path source = directory.resolve("broken.cbl");
        Path copybooks = Files.createDirectory(directory.resolve("cpy"));
        Path output = directory.resolve("output");
        StringBuilder cobol = new StringBuilder("       IDENTIFICATION DIVISION.\n"
                + "       PROGRAM-ID. BROKEN.\n"
                + "       PROCEDURE DIVISION.\n");
        for (int index = 0; index < 25; index++) cobol.append("       @@@@@\n");
        cobol.append("       GOBACK.\n");
        Files.writeString(source, cobol);

        List<ILoggingEvent> events = capture(ExplorerMain.class, Level.WARN,
                () -> ExplorerMain.main(new String[]{"--source", source.toString(),
                        "--copybooks", copybooks.toString(), "--output", output.toString()}));

        List<ILoggingEvent> warnings = events.stream()
                .filter(event -> event.getLevel() == Level.WARN).toList();
        assertEquals(2, warnings.size());
        String message = warnings.stream().filter(event -> event.getFormattedMessage()
                .contains("event=parse_degraded")).findFirst().orElseThrow().getFormattedMessage();
        assertAll(
                () -> assertTrue(message.contains("event=parse_degraded")),
                () -> assertTrue(message.contains("result=PARSE_TREE_PRODUCED")),
                () -> assertTrue(message.contains("fallback=CONTINUE_WITH_PARTIAL_PARSE_TREE")),
                () -> assertTrue(message.contains("impact=ANALYSIS_INCOMPLETE")));
        assertEquals(1, count(warnings, "event=analysis_degraded"));
        assertTrue(Files.size(output.resolve("ast-data.js")) > 0);
    }

    private static long count(List<ILoggingEvent> events, String fragment) {
        return events.stream().filter(event -> event.getFormattedMessage().contains(fragment)).count();
    }

    private static List<ILoggingEvent> capture(Class<?> owner, Level level, ThrowingAction action)
            throws Exception {
        return captureResult(owner, level, () -> {
            action.run();
            return null;
        }).events();
    }

    private static <T> Captured<T> captureResult(Class<?> owner, Level level,
                                                  ThrowingSupplier<T> action) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(owner);
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        SnapshotListAppender appender = new SnapshotListAppender();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(level);
        logger.setAdditive(false);
        try {
            T result = action.get();
            return new Captured<>(List.copyOf(appender.list), result);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
            appender.stop();
        }
    }

    @FunctionalInterface
    private interface ThrowingAction { void run() throws Exception; }
    @FunctionalInterface
    private interface ThrowingSupplier<T> { T get() throws Exception; }
    private record Captured<T>(List<ILoggingEvent> events, T result) {}

    private static final class SnapshotListAppender extends ListAppender<ILoggingEvent> {
        @Override protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    }
}
