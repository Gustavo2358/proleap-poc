package io.proleap.benchmark;

import org.slf4j.MDC;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class AnalysisLogContext implements AutoCloseable {
    private final Map<String, String> previousContext;
    private final String runId;
    private boolean closed;

    private AnalysisLogContext(Path source) {
        previousContext = MDC.getCopyOfContextMap();
        runId = UUID.randomUUID().toString();
        Path fileName = Objects.requireNonNull(source, "source").getFileName();
        MDC.put("runId", runId);
        MDC.put("source", fileName == null ? source.toString() : fileName.toString());
        MDC.remove("programUnit");
    }

    static AnalysisLogContext open(Path source) {
        return new AnalysisLogContext(source);
    }

    String runId() {
        return runId;
    }

    void setProgramUnit(String programUnit) {
        if (programUnit == null || programUnit.isBlank()) {
            MDC.remove("programUnit");
        } else {
            MDC.put("programUnit", programUnit);
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        MDC.clear();
        if (previousContext != null) MDC.setContextMap(previousContext);
    }
}
