package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.FileMemoryEffectsTest.fixture;

/** Expected phases come from IBM rules, not serialized output. */
class FileIoEffectPlanTest {
    static FileIoEffects.Operation plan(FileMemoryEffectsTest.Fixture f,Ast.FileCommand command) {
        var statement=f.storage().files().statements().stream().filter(s->s.surface().command()==command).findFirst().orElseThrow();
        return f.storage().fileEffects().operation(statement.statement(),0).orElseThrow();
    }
    static List<FileIoEffects.Step> outcome(FileIoEffects.Operation p,FileIoEffects.Outcome outcome){return p.outcomes().stream().filter(c->c.outcome()==outcome).findFirst().orElseThrow().steps();}
    @Test void readSeparatesSuccessAndEofAndDoesNotInferBufferMust() {
        var f=fixture("SELECT F ASSIGN TO INDD FILE STATUS IO-STATUS.","FD F.\n01 REC PIC X(8).","01 DEST PIC X(8).\n01 IO-STATUS PIC XX.","READ F INTO DEST.");
        var p=plan(f,Ast.FileCommand.READ);assertTrue(p.before().isEmpty());
        var success=outcome(p,FileIoEffects.Outcome.SUCCESS);
        assertEquals(List.of(FileIoMemory.Role.RECORD,FileIoMemory.Role.FILE_STATUS,FileIoMemory.Role.INTO),success.stream().map(FileIoEffects.Step::role).toList());
        assertEquals(FileIoEffects.Kind.MAY_UNKNOWN,success.get(0).kind());assertEquals(FileIoEffects.Kind.MUST_UNKNOWN,success.get(1).kind());assertEquals(FileIoEffects.Kind.MUST_UNKNOWN,success.get(2).kind());
        for(var c:p.outcomes())if(c.outcome()!=FileIoEffects.Outcome.SUCCESS)assertTrue(c.steps().stream().noneMatch(s->s.role()==FileIoMemory.Role.INTO));
    }
    @Test void rewriteFromPrecedesEveryOutcomeButInvalidKeyDoesNotInvalidateRecord() {
        var f=fixture("SELECT F ASSIGN TO OUTDD.","FD F.\n01 REC PIC X(8).","01 SOURCE-PGM PIC X(8).","REWRITE REC FROM SOURCE-PGM.");
        var p=plan(f,Ast.FileCommand.REWRITE);assertEquals(1,p.before().size());assertEquals(FileIoEffects.Kind.COPY_BYTES,p.before().get(0).kind());
        assertTrue(p.before().get(0).source().isPresent());assertTrue(outcome(p,FileIoEffects.Outcome.INVALID_KEY).stream().noneMatch(s->s.role()==FileIoMemory.Role.RECORD));
        assertTrue(outcome(p,FileIoEffects.Outcome.SUCCESS).stream().anyMatch(s->s.role()==FileIoMemory.Role.RECORD&&s.kind()==FileIoEffects.Kind.MAY_UNKNOWN));
    }
    @Test void overlappingIntoDoesNotGetAnIllegalStrongWrite() {
        var f=fixture("SELECT F ASSIGN TO INDD.","FD F.\n01 REC PIC X(8).","01 SAFE PIC X(8).","READ F INTO REC(2:4).");
        var p=plan(f,Ast.FileCommand.READ);var into=outcome(p,FileIoEffects.Outcome.SUCCESS).stream().filter(s->s.role()==FileIoMemory.Role.INTO).findFirst().orElseThrow();
        assertEquals(FileIoEffects.Kind.MAY_UNKNOWN,into.kind());assertTrue(into.gaps().contains("FILE_TRANSFER_ALIAS_NOT_PROVEN"));
    }
    @Test void indexAffectedByReadIsNotCapturedBeforeIo() {
        var f=fixture("SELECT F ASSIGN TO INDD.","FD F.\n01 REC.\n 05 IDX PIC 9.\n 05 CONTENT-FIELD PIC X(8).",
            "01 DEST-TABLE.\n 05 DEST PIC X(8) OCCURS 4 TIMES.\n01 SAFE PIC X(8).","READ F INTO DEST(IDX).");
        var p=plan(f,Ast.FileCommand.READ);assertTrue(p.before().isEmpty());
        var into=outcome(p,FileIoEffects.Outcome.SUCCESS).stream().filter(s->s.role()==FileIoMemory.Role.INTO).findFirst().orElseThrow();
        assertEquals(FileIoEffects.Kind.MAY_UNKNOWN,into.kind());assertTrue(into.destination().wholeBase());assertTrue(into.gaps().contains("FILE_RECEIVER_ADDRESS_NOT_PROVEN"));
        assertTrue(outcome(p,FileIoEffects.Outcome.END).stream().noneMatch(s->s.role()==FileIoMemory.Role.INTO));
    }
    @Test void variableTailNeverAcquiresAnUnprovedMustAndLengthPrecedesInto() {
        var f=fixture("SELECT F ASSIGN TO INDD.","FD F RECORD VARYING FROM 2 TO 8\n DEPENDING ON REC-LENGTH.\n01 REC PIC X(8).",
            "01 REC-LENGTH PIC 9(4).\n01 DEST PIC X(8).","READ F INTO DEST.");
        var steps=outcome(plan(f,Ast.FileCommand.READ),FileIoEffects.Outcome.SUCCESS);
        assertTrue(steps.stream().filter(s->s.role()==FileIoMemory.Role.RECORD).allMatch(s->s.kind()==FileIoEffects.Kind.MAY_UNKNOWN));
        assertEquals(List.of(FileIoMemory.Role.RECORD,FileIoMemory.Role.RECORD_LENGTH,FileIoMemory.Role.INTO),steps.stream().map(FileIoEffects.Step::role).toList());
    }
    @Test void foreignDialectSyntaxCannotAcquireIbmStrongEffects() {
        var f=fixture("SELECT F ASSIGN TO INDD FILE STATUS IO-STATUS.","FD F.\n01 REC PIC X(8).","01 DEST PIC X(8).\n01 IO-STATUS PIC XX.","READ F INTO DEST WITH KEPT LOCK.");
        var p=plan(f,Ast.FileCommand.READ);assertFalse(p.unknownWriteBound());
        assertTrue(p.outcomes().stream().flatMap(c->c.steps().stream()).allMatch(s->s.kind()==FileIoEffects.Kind.MAY_UNKNOWN));
    }
}
