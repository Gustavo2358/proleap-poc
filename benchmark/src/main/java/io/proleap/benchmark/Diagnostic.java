package io.proleap.benchmark;

public record Diagnostic(String frontend, Phase phase, String file, int line, int column,
                         String message, String offendingToken, String exceptionClass) {
    public enum Phase { PREPROCESSOR, LEXER, PARSER, IO, OTHER }
}
