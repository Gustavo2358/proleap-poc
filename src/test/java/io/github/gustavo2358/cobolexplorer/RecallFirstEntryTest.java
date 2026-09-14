package io.github.gustavo2358.cobolexplorer;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.DeclarativeValueInferenceTest.*;

/** RF-W0 oracle: entry possibilities are independent of lifetime constancy. */
class RecallFirstEntryTest {
    private static void possible(String code) {
        var p=product(VALUE+"01 FLAG PIC X.\n01 INPUT-PGM PIC X(8).\n",code);
        var c=condition(p);
        // Wire vocabulary allows this oracle to compile against the pre-capability producer.
        assertEquals("POSSIBLE_LITERAL_BYTES",c.kind().name(),c.gapCodes().toString());
        assertEquals("DECLARATIVE_POSSIBILITY",c.proof().name());
        assertEquals(List.of(215,217,214,199,193,64,64,64),c.bytes());
        assertTrue(c.gapCodes().contains("ENTRY_STATE_NOT_PROVEN"));
    }
    @Test void unknownStatementDoesNotEraseSourceSupportedEntryBytes() {
        // DISPLAY gains a typed no-write proof in RF-W2; EXHIBIT remains unknown.
        possible("EXHIBIT LIT-PGM.\nCALL LIT-PGM.");
    }
    @Test void futureMustWriteDoesNotEraseEntryButIsStillAnExecutableWrite() {
        possible("MOVE 'PROGB' TO LIT-PGM.\nCALL LIT-PGM.");
        assertEquals(1,product(VALUE,"MOVE 'PROGB' TO LIT-PGM.\nCALL LIT-PGM.").moves().size());
    }
    @Test void conditionalWriteDoesNotRequireLifetimeInvariance() {
        possible("IF FLAG = 'Y'\nMOVE 'PROGB' TO LIT-PGM\nEND-IF.\nCALL LIT-PGM.");
    }
    @Test void unknownForeignExposureOpensRemainderInsteadOfErasingEntryCandidate() {
        possible("CALL 'OTHER' USING LIT-PGM.\nCALL LIT-PGM.");
        possible("CALL 'OTHER' USING MISSING-AREA.\nCALL LIT-PGM.");
    }
    @Test void disjointInformationAndStrongProfilesPreserveExistingPrecision() {
        invariant(VALUE,"CALL LIT-PGM.");
        invariant(VALUE+"01 ARG-AREA PIC X(20).\n","CALL LIT-PGM USING ARG-AREA.");
        for(var mode:List.of(StorageInitialSemantics.EntryMode.INITIAL,StorageInitialSemantics.EntryMode.UNKNOWN)) {
            String s=source(VALUE,"DISPLAY 'TRACE'.\nCALL LIT-PGM.");
            if(mode==StorageInitialSemantics.EntryMode.UNKNOWN)s=s.replace("DVI-PROGRAM.","DVI-PROGRAM IS INITIAL.");
            var p=StorageInitialProductTest.initialSource(s,mode);
            assertEquals("LITERAL_BYTES",condition(p).kind().name());
            assertTrue(condition(p).gapCodes().isEmpty());assertTrue(p.moves().isEmpty());
        }
    }
    @Test void unknownValueOrLayoutNeverInventsBytesAndPreservedProfileStaysExplicit() {
        for(var data:List.of("01 LIT-PGM PIC X(2) VALUE 'TOO-LONG'.\n",
                "01 WS-AREA PIC X(8).\n01 LIT-PGM REDEFINES WS-AREA PIC X(8) VALUE 'PROGA'.\n")) {
            var c=condition(product(data,"CALL LIT-PGM."));
            assertEquals("UNKNOWN",c.kind().name());assertTrue(c.bytes().isEmpty());
        }
        var p=StorageInitialProductTest.initialSource(source(VALUE,"CALL LIT-PGM."),StorageInitialSemantics.EntryMode.PRESERVED);
        assertEquals("PRESERVE",condition(p).kind().name());assertTrue(condition(p).bytes().isEmpty());
    }
}
