package io.github.gustavo2358.cobolexplorer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExplorerMainLoggingTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/resolution/coverage-states.cbl");

    @Test
    void logsCorrelatedLifecycleAndPhaseMetricsWithoutChangingProducts() throws Exception {
        Path output = Files.createTempDirectory("explorer-logging-success");
        Path copybooks = Files.createTempDirectory("explorer-logging-copybooks");

        List<ILoggingEvent> events = capture(() -> ExplorerMain.main(new String[]{
                "--source", FIXTURE.toAbsolutePath().toString(),
                "--copybooks", copybooks.toString(), "--output", output.toString()}));

        assertEvent(events, Level.INFO, "event=analysis_started", "phase=ANALYSIS");
        for (String event : List.of("normalization_completed", "preprocessing_completed",
                "lexing_completed", "parsing_completed", "ast_built",
                "symbol_tables_built", "references_collected",
                "external_classification_completed", "resolution_completed")) {
            assertEvent(events, Level.DEBUG, "event=" + event, "elapsedMs=");
        }
        assertEvent(events, Level.INFO, "event=analysis_completed", "elapsedMs=");
        assertEvent(events, Level.WARN, "event=analysis_degraded", "fallback=RESULT_PUBLISHED_WITH_GAPS");

        List<ILoggingEvent> lifecycle = events.stream()
                .filter(event -> event.getFormattedMessage().contains("event=analysis_"))
                .toList();
        assertEquals(3, lifecycle.size());
        String runId = lifecycle.get(0).getMDCPropertyMap().get("runId");
        assertNotNull(runId);
        assertEquals(runId, lifecycle.get(1).getMDCPropertyMap().get("runId"));
        assertEquals(FIXTURE.getFileName().toString(),
                lifecycle.get(0).getMDCPropertyMap().get("source"));
        assertNotNull(lifecycle.get(1).getMDCPropertyMap().get("programUnit"));
        assertNull(MDC.get("runId"), "the operational boundary must clean MDC after success");

        assertTrue(Files.size(output.resolve("ast-data.js")) > 0);
        assertTrue(Files.size(output.resolve("symbol-data.js")) > 0);
        assertTrue(Files.size(output.resolve("resolution-data.js")) > 0);
    }

    @Test
    void logsExecutedPartialFallbackAndStructurallyBlockedClassifier(@TempDir Path directory)
            throws Exception {
        Path copybooks = Files.createDirectory(directory.resolve("copybooks"));
        Path partial = directory.resolve("partial.cbl");
        Files.writeString(partial, source("COPY MISSINGCP.", "DFHRESP(NORMAL)"),
                StandardCharsets.UTF_8);
        List<ILoggingEvent> partialEvents = capture(() -> ExplorerMain.main(new String[]{
                "--source", partial.toString(), "--copybooks", copybooks.toString(),
                "--output", directory.resolve("partial-output").toString()}));

        Path recovered = directory.resolve("recovered.cbl");
        Files.writeString(recovered, source("", "DFHRESP(IDX)(OTHER)"),
                StandardCharsets.UTF_8);
        List<ILoggingEvent> recoveredEvents = capture(() -> ExplorerMain.main(new String[]{
                "--source", recovered.toString(), "--copybooks", copybooks.toString(),
                "--output", directory.resolve("recovered-output").toString()}));

        assertAll("classifier lifecycle explains partial execution and fail-closed skip",
                () -> assertEvent(partialEvents, Level.DEBUG,
                        "event=external_classification_completed", "executed=true",
                        "unresolvedCopies=1", "copyInputCompleteness=INCOMPLETE_UNRESOLVED_COPY",
                        "fallback=CONTINUE_WITH_PARTIAL_ANALYSIS", "impact=ANALYSIS_INCOMPLETE"),
                () -> assertEvent(recoveredEvents, Level.DEBUG,
                        "event=external_classification_completed", "executed=false",
                        "unresolvedCopies=0", "copyInputCompleteness=COMPLETE",
                        "reason=STRUCTURAL_FRONTEND_ERRORS",
                        "fallback=SKIP_CLASSIFIER_FAIL_CLOSED"),
                () -> assertFalse(recoveredEvents.stream().anyMatch(event ->
                        event.getFormattedMessage().contains(" inputCompleteness="))));
    }

    @Test
    void logsEscapingFailureOnceAtTheOperationalBoundaryAndCleansMdc() throws Exception {
        Path missing = Path.of("/tmp", "missing-cobol-" + System.nanoTime(), "PROGA.cbl");

        CapturedFailure failure = captureFailure(() -> ExplorerMain.main(new String[]{
                "--source", missing.toString(), "--output", Files.createTempDirectory("unused").toString()}));

        assertNotNull(failure.thrown());
        List<ILoggingEvent> errors = failure.events().stream()
                .filter(event -> event.getLevel() == Level.ERROR).toList();
        assertEquals(1, errors.size(), "one escaping failure must have one stacktrace event");
        ILoggingEvent error = errors.get(0);
        assertAll(
                () -> assertTrue(error.getFormattedMessage().contains("event=analysis_failed")),
                () -> assertTrue(error.getFormattedMessage().contains("phase=SOURCE_READ")),
                () -> assertTrue(error.getFormattedMessage().contains("impact=NO_RESULT")),
                () -> assertNotNull(error.getThrowableProxy()),
                () -> assertEquals("PROGA.cbl", error.getMDCPropertyMap().get("source")));
        assertNull(MDC.get("runId"), "the operational boundary must clean MDC after failure");
    }

    private static void assertEvent(List<ILoggingEvent> events, Level level, String... fragments) {
        assertTrue(events.stream().anyMatch(event -> event.getLevel() == level
                        && List.of(fragments).stream().allMatch(event.getFormattedMessage()::contains)),
                () -> "missing " + level + " event with " + List.of(fragments)
                        + " in " + events.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    private static String source(String copy, String construct) {
        return String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. LOGPOLICY.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-RESP PIC S9(8) COMP.",
                "       01 IDX PIC 9.",
                copy.isBlank() ? "" : "       " + copy,
                "       PROCEDURE DIVISION.",
                "           IF WS-RESP = " + construct,
                "               CONTINUE",
                "           END-IF.",
                "           GOBACK.",
                "       END PROGRAM LOGPOLICY.", "");
    }

    private static List<ILoggingEvent> capture(ThrowingAction action) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ExplorerMain.class);
        Level previous = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        ListAppender<ILoggingEvent> appender = new SnapshotListAppender();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        try {
            action.run();
            return List.copyOf(appender.list);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
            logger.setAdditive(previousAdditive);
            appender.stop();
        }
    }

    private static CapturedFailure captureFailure(ThrowingAction action) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ExplorerMain.class);
        Level previous = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        ListAppender<ILoggingEvent> appender = new SnapshotListAppender();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        try {
            Exception thrown = assertThrows(Exception.class, action::run);
            return new CapturedFailure(List.copyOf(appender.list), thrown);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
            logger.setAdditive(previousAdditive);
            appender.stop();
        }
    }

    @FunctionalInterface
    private interface ThrowingAction { void run() throws Exception; }
    private record CapturedFailure(List<ILoggingEvent> events, Exception thrown) {}

    private static final class SnapshotListAppender extends ListAppender<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    }
}
