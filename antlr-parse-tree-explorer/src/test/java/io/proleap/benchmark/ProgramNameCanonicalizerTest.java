package io.proleap.benchmark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramNameCanonicalizerTest {
    @Test
    void appliesTheDocumentedIbmExternalProgramNameTransformations() {
        assertAll("PGMNAME(COMPAT)",
                () -> assertEquals("LONG0NAM", canonical("LONG-NAME-ABC",
                        ResolutionContracts.PgmnameMode.COMPAT)),
                () -> assertEquals("PROG0A", canonical("PROG-A",
                        ResolutionContracts.PgmnameMode.COMPAT)),
                () -> assertEquals("APROG", canonical("1PROG",
                        ResolutionContracts.PgmnameMode.COMPAT)),
                () -> assertEquals("JPROG", canonical("$PROG",
                        ResolutionContracts.PgmnameMode.COMPAT)),
                () -> assertEquals("JPROG", canonical("-PROG",
                        ResolutionContracts.PgmnameMode.COMPAT)));
        assertAll("PGMNAME(LONGUPPER)",
                () -> assertEquals("PROG0A", canonical("PROG-A",
                        ResolutionContracts.PgmnameMode.LONGUPPER)),
                () -> assertEquals("APROG", canonical("1PROG",
                        ResolutionContracts.PgmnameMode.LONGUPPER)),
                () -> assertEquals("JPROG", canonical("$PROG",
                        ResolutionContracts.PgmnameMode.LONGUPPER)),
                () -> assertEquals("JPROG", canonical("-PROG",
                        ResolutionContracts.PgmnameMode.LONGUPPER)));
        assertEquals("mixed-Child", canonical("mixed-Child",
                ResolutionContracts.PgmnameMode.LONGMIXED));
    }

    private static String canonical(String name, ResolutionContracts.PgmnameMode mode) {
        return ProgramNameCanonicalizer.external(name, mode);
    }
}
