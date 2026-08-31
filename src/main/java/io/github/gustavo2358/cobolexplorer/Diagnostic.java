package io.github.gustavo2358.cobolexplorer;

import java.util.Objects;

public record Diagnostic(String frontend, Phase phase, Code code, String file, int line, int column,
                         String message, String offendingToken, String exceptionClass) {
    public enum Phase { PREPROCESSOR, LEXER, PARSER, IO, OTHER }

    public enum Code { GENERAL, UNRESOLVED_COPY }

    public Diagnostic {
        code = Objects.requireNonNull(code, "code");
        if (code == Code.UNRESOLVED_COPY && phase != Phase.PREPROCESSOR)
            throw new IllegalArgumentException(
                    "UNRESOLVED_COPY diagnostics must belong to the preprocessor");
    }

    public Diagnostic(String frontend, Phase phase, String file, int line, int column,
                      String message, String offendingToken, String exceptionClass) {
        this(frontend, phase, Code.GENERAL, file, line, column,
                message, offendingToken, exceptionClass);
    }
}
