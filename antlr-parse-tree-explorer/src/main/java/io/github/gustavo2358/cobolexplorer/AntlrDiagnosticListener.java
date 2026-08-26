package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

final class AntlrDiagnosticListener extends BaseErrorListener {
    private static final Logger LOG = LoggerFactory.getLogger(AntlrDiagnosticListener.class);

    private final String frontend;
    private final Diagnostic.Phase phase;
    private final String file;
    private final List<Diagnostic> sink;

    AntlrDiagnosticListener(String frontend, Diagnostic.Phase phase, String file, List<Diagnostic> sink) {
        this.frontend = frontend; this.phase = phase; this.file = file; this.sink = sink;
    }

    @Override public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                      int charPositionInLine, String msg, RecognitionException e) {
        String token = offendingSymbol instanceof Token t ? t.getText() : String.valueOf(offendingSymbol);
        sink.add(new Diagnostic(frontend, phase, file, line, charPositionInLine, msg, token,
                e == null ? "" : e.getClass().getName()));
        if (LOG.isTraceEnabled()) {
            LOG.trace("event=antlr_diagnostic source={} phase={} line={} column={} exceptionClass={}",
                    file, phase, line, charPositionInLine,
                    e == null ? "none" : e.getClass().getSimpleName());
        }
    }
}
