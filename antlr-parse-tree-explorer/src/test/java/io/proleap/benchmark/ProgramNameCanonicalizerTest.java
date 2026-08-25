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
        assertAll("nested lookup keys are separate from external transformations",
                () -> assertEquals("mixed-Child", ProgramNameCanonicalizer.nested(
                        "'mixed-Child'", ResolutionContracts.PgmnameMode.LONGMIXED)),
                () -> assertEquals("MIXED-CHILD", ProgramNameCanonicalizer.nested(
                        "'mixed-Child'", ResolutionContracts.PgmnameMode.LONGUPPER)),
                () -> assertEquals("MIXED-CHILD", ProgramNameCanonicalizer.nested(
                        "'mixed-Child'", ResolutionContracts.PgmnameMode.COMPAT)));
    }

    private static String canonical(String name, ResolutionContracts.PgmnameMode mode) {
        return ProgramNameCanonicalizer.external(name, mode);
    }
}
