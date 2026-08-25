package io.proleap.benchmark;

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
}
