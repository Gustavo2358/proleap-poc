package io.github.gustavo2358.cobolexplorer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LoggingInfrastructureTest {
    @AfterEach
    void cleanMdc() {
        MDC.clear();
        System.clearProperty("ANALYZER_LOG_LEVEL");
        System.clearProperty("PREPROCESSOR_LOG_LEVEL");
    }

    @Test
    void defaultConfigurationIsConservativeButExposesLifecycle() throws Exception {
        LoggerContext context = configuredContext();
        try {
            assertEquals(Level.WARN, context.getLogger(Logger.ROOT_LOGGER_NAME).getLevel());
            assertEquals(Level.INFO, context.getLogger(ExplorerMain.class).getLevel());
            assertEquals(Level.WARN, context.getLogger(PreprocessorEngine.class).getEffectiveLevel());
        } finally {
            context.stop();
        }
    }

    @Test
    void analyzerLevelCanBeOverriddenWithoutRecompilation() throws Exception {
        System.setProperty("ANALYZER_LOG_LEVEL", "DEBUG");

        LoggerContext context = configuredContext();
        try {
            assertEquals(Level.DEBUG, context.getLogger(ExplorerMain.class).getLevel());
        } finally {
            context.stop();
        }
    }

    @Test
    void decisionTracingCanBeEnabledForOneClass() throws Exception {
        System.setProperty("PREPROCESSOR_LOG_LEVEL", "TRACE");

        LoggerContext context = configuredContext();
        try {
            assertEquals(Level.TRACE, context.getLogger(PreprocessorEngine.class).getLevel());
            assertEquals(Level.WARN, context.getLogger(CobolReferenceResolver.class).getEffectiveLevel());
        } finally {
            context.stop();
        }
    }

    @Test
    void analysisContextCorrelatesRunAndRestoresExistingMdc() {
        MDC.put("callerContext", "preserved");

        String runId;
        try (AnalysisLogContext context = AnalysisLogContext.open(Path.of("/private/input/PROGA.cbl"))) {
            runId = context.runId();
            assertEquals(runId, MDC.get("runId"));
            assertEquals("PROGA.cbl", MDC.get("source"));
            assertNull(MDC.get("programUnit"));

            context.setProgramUnit("PROGA");
            assertEquals("PROGA", MDC.get("programUnit"));
        }

        assertNotNull(runId);
        assertTrue(runId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertNull(MDC.get("runId"));
        assertNull(MDC.get("source"));
        assertNull(MDC.get("programUnit"));
        assertEquals("preserved", MDC.get("callerContext"));
    }

    private static LoggerContext configuredContext() throws Exception {
        URL configuration = LoggerFactory.class.getResource("/logback.xml");
        assertNotNull(configuration, "logback.xml must be available on the runtime classpath");
        LoggerContext context = new LoggerContext();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(configuration);
        return context;
    }
}
