package io.github.gustavo2358.cobolexplorer;

import java.util.Objects;

/** Policy-dependent program-name lookup keys; it does not decide visibility or binding. */
final class ProgramNameCanonicalizer {
    private ProgramNameCanonicalizer() { }

    static String external(String writtenName, ResolutionContracts.PgmnameMode mode) {
        Objects.requireNonNull(writtenName, "writtenName");
        Objects.requireNonNull(mode, "mode");
        return switch (mode) {
            case COMPAT -> translatedUpper(writtenName, true);
            case LONGUPPER -> translatedUpper(writtenName, false);
            case LONGMIXED -> writtenName;
            case UNSPECIFIED -> SymbolTable.canonical(writtenName);
        };
    }

    static String nested(String writtenName, ResolutionContracts.PgmnameMode mode) {
        Objects.requireNonNull(writtenName, "writtenName");
        Objects.requireNonNull(mode, "mode");
        String semanticName = unquote(writtenName.strip());
        return mode == ResolutionContracts.PgmnameMode.LONGMIXED
                ? semanticName : SymbolTable.canonical(semanticName);
    }

    /** IBM dynamic CALL identity is COMPAT-like and is not controlled by PGMNAME. */
    static String dynamicExternal(String writtenName) {
        Objects.requireNonNull(writtenName, "writtenName");
        return translatedUpper(writtenName, true);
    }

    private static String translatedUpper(String writtenName, boolean truncate) {
        String canonical = SymbolTable.canonical(writtenName);
        if (truncate) canonical = canonical.substring(0, Math.min(8, canonical.length()));
        canonical = canonical.replace('-', '0');
        if (canonical.isEmpty()) return canonical;
        char first = canonical.charAt(0);
        if ((first >= 'A' && first <= 'Z') || first == '_') return canonical;
        char translated = first >= '1' && first <= '9'
                ? (char) ('A' + first - '1') : 'J';
        return translated + canonical.substring(1);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\""))))
            return value.substring(1, value.length() - 1);
        return value;
    }
}
