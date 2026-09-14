package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;

/** Lexical boundary only: no IMS opcode, operand, effect or control interpretation. */
public final class DliRegion {
    public static final String PREFIX = "*>EXECDLI{";
    public static final String SUFFIX = "}*>ENDDLI";

    private DliRegion() { }

    /** Used by the two dedicated DLI lexer rules after their EXEC DLI prefix. */
    public static void consumeBody(Lexer lexer, boolean transported) {
        boolean payload = false;
        while (true) {
            int c = lexer.getInputStream().LA(1);
            if (c == IntStream.EOF) throw failure(lexer, "missing real END-EXEC before EOF");
            if (Character.isWhitespace(c)) {
                consume(lexer);
            } else if (c == '*' && lexer.getInputStream().LA(2) == '>') {
                while (lexer.getInputStream().LA(1) != IntStream.EOF
                        && lexer.getInputStream().LA(1) != '\n' && lexer.getInputStream().LA(1) != '\r') consume(lexer);
            } else if (c == '\'' || c == '"') {
                payload = true;
                consumeLiteral(lexer, c);
            } else if (wordPart(c)) {
                StringBuilder word = new StringBuilder();
                do {
                    word.appendCodePoint(lexer.getInputStream().LA(1));
                    consume(lexer);
                } while (wordPart(lexer.getInputStream().LA(1)));
                if (word.toString().equalsIgnoreCase("EXEC"))
                    throw failure(lexer, "another EXEC before real END-EXEC");
                if (word.toString().equalsIgnoreCase("END-EXEC")) {
                    if (!payload) throw failure(lexer, "empty payload");
                    if (transported) {
                        for (int expected : SUFFIX.codePoints().toArray()) {
                            if (lexer.getInputStream().LA(1) != expected)
                                throw failure(lexer, "invalid opaque transport boundary");
                            consume(lexer);
                        }
                    }
                    return;
                }
                payload = true;
            } else {
                payload = true;
                consume(lexer);
            }
        }
    }

    public static boolean wordPart(int c) {
        return c != IntStream.EOF && (Character.isLetterOrDigit(c) || c == '-' || c == '_');
    }

    private static void consumeLiteral(Lexer lexer, int quote) {
        consume(lexer);
        while (true) {
            int c = lexer.getInputStream().LA(1);
            // Fixed-format continuation has already been resolved by SourceNormalizer.
            if (c == IntStream.EOF || c == '\n' || c == '\r')
                throw failure(lexer, "unclosed literal");
            consume(lexer);
            if (c == quote) {
                if (lexer.getInputStream().LA(1) != quote) return;
                consume(lexer); // doubled quote is literal content
            }
        }
    }

    private static void consume(Lexer lexer) {
        lexer.getInterpreter().consume(lexer.getInputStream());
    }

    private static IllegalStateException failure(Lexer lexer, String reason) {
        return new IllegalStateException("EXEC DLI at " + lexer.getInputStream().getSourceName()
                + ":" + lexer.getLine() + ":" + lexer.getCharPositionInLine() + ": " + reason);
    }

    static String frame(String raw) { return PREFIX + raw + SUFFIX; }

    static String payload(String framed) {
        if (!framed.startsWith(PREFIX) || !framed.endsWith(SUFFIX))
            throw new IllegalStateException("EXEC DLI has no validated opaque framing");
        return framed.substring(PREFIX.length(), framed.length() - SUFFIX.length());
    }
}
