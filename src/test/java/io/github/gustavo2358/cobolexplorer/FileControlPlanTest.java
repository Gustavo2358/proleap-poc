package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.FileMemoryEffectsTest.fixture;

/** Independent IBM SC27-8713-03 (2026-04-28) pp264,299,303-304,432,480,714-715. */
class FileControlPlanTest {
    static FileMemoryEffectsTest.Fixture source(String declarations,String body) {
        return fixture("SELECT F ASSIGN TO INDD FILE STATUS FS.","FD F.\n01 REC PIC X(8).","01 FS PIC XX.\n01 DEST PIC X(8).",
            declarations.isEmpty()?body:"DECLARATIVES.\n"+declarations+"\nEND DECLARATIVES.\nMAIN-SECTION SECTION.\nMAIN-PARA.\n"+body);
    }
    static FileIoControl control(FileMemoryEffectsTest.Fixture f) {
        return FileIoControl.analyze(f.source().build(),f.source().resolution(),f.storage().files());
    }
    static FileIoControl.Operation read(FileIoControl c) {return c.operations().stream().filter(o->o.command()==Ast.FileCommand.READ).findFirst().orElseThrow();}
    static FileIoControl.Route route(FileIoControl.Operation p,FileIoControl.Event e){return p.routes().stream().filter(r->r.event()==e).findFirst().orElseThrow();}
    static List<FileIoControl.DestinationKind> kinds(FileIoControl.Route r){return r.destinations().stream().map(FileIoControl.Destination::kind).toList();}
    static final String FILE_USE="FILE-ERROR SECTION.\nUSE AFTER STANDARD ERROR PROCEDURE ON F.\nERROR-PARA.\nCALL 'USEFILE'.";
    @Test void declarativesAreInventoriedOnceAndNeverBecomePrimaryEntry() {
        var f=source(FILE_USE,"READ F.\nCALL 'AFTER'.");var c=control(f);
        assertEquals(1,c.declaratives().size());var d=c.declaratives().get(0);
        assertEquals(Ast.UseKind.AFTER_EXCEPTION,d.kind());assertFalse(d.global());assertEquals(1,d.files().size());assertEquals(1,d.roots().size());
        var procedure=f.source().model().programUnits().get(0).program().divisions().stream().filter(x->x.divisionKind()==Ast.DivisionKind.PROCEDURE).findFirst().orElseThrow();
        assertTrue(procedure.procedureEntry().orElseThrow().startStatementId().isPresent());
        assertNotEquals(d.entry(),procedure.procedureEntry().orElseThrow().startStatementId());
        assertEquals(List.of(FileIoControl.DestinationKind.USE),kinds(route(read(c),FileIoControl.Event.END)));
        assertEquals(List.of(FileIoControl.DestinationKind.CONTINUE),kinds(route(read(c),FileIoControl.Event.SUCCESS)));
    }
    @Test void explicitEofOverridesUseAndErrorsDoNotExecuteNotAtEndWithUse() {
        var c=control(source(FILE_USE,"READ F\n AT END CALL 'EOFPGM'\n NOT AT END CALL 'GOODPGM'\nEND-READ.\nCALL 'AFTER'."));var p=read(c);
        assertEquals(List.of(FileIoControl.DestinationKind.HANDLER),kinds(route(p,FileIoControl.Event.END)));
        assertEquals(Ast.FileHandlerKind.AT_END,route(p,FileIoControl.Event.END).destinations().get(0).handler().orElseThrow());
        assertEquals(List.of(FileIoControl.DestinationKind.USE),kinds(route(p,FileIoControl.Event.OTHER_ERROR)));
        assertEquals(Ast.FileHandlerKind.NOT_AT_END,route(p,FileIoControl.Event.SUCCESS).destinations().get(0).handler().orElseThrow());
        assertTrue(p.continuation().isPresent());
    }
    @Test void notAtEndWithoutUseCanReceiveOtherErrorButNeverExecutesIntoOnThatRoute() {
        var p=read(control(source("","READ F INTO DEST\n AT END CONTINUE\n NOT AT END CALL 'NOTEOF' END-READ.")));
        var r=route(p,FileIoControl.Event.OTHER_ERROR);assertEquals(FileIoEffects.Outcome.OTHER_ERROR,r.effects());
        assertEquals(Ast.FileHandlerKind.NOT_AT_END,r.destinations().get(0).handler().orElseThrow());
    }
    @Test void namedUseWinsOverModeAndOpenModeIsKnownOnlyAtOpen() {
        var c=control(source("MODE-ERROR SECTION.\nUSE AFTER ERROR PROCEDURE ON INPUT.\nMODE-PARA.\nCALL 'MODEPGM'.\n"+FILE_USE,"OPEN INPUT F.\nREAD F."));
        var named=c.declaratives().stream().filter(d->!d.files().isEmpty()).findFirst().orElseThrow();
        for(var p:c.operations())assertEquals(named.section(),route(p,FileIoControl.Event.OTHER_ERROR).destinations().get(0).declarative().orElseThrow());
        var onlyMode=control(source("MODE-ERROR SECTION.\nUSE AFTER ERROR PROCEDURE ON INPUT.\nMODE-PARA.\nCALL 'MODEPGM'.","OPEN INPUT F.\nREAD F."));
        var open=onlyMode.operations().stream().filter(o->o.command()==Ast.FileCommand.OPEN).findFirst().orElseThrow();
        assertEquals(List.of(FileIoControl.DestinationKind.USE),kinds(route(open,FileIoControl.Event.OTHER_ERROR)));
        assertFalse(read(onlyMode).gaps().isEmpty()); // mode at later I/O is not guessed from lexical order
    }
    @Test void duplicateUseAndDebuggingAreExplicitlyIncomplete() {
        var c=control(source(FILE_USE+"\nSECOND-ERROR SECTION.\nUSE AFTER ERROR PROCEDURE ON F.\nP2.\nCALL 'DUP'.","READ F."));
        assertTrue(read(c).gaps().contains("FILE_USE_SELECTION_NOT_PROVEN"));
        assertEquals(2,route(read(c),FileIoControl.Event.END).destinations().stream().filter(d->d.kind()==FileIoControl.DestinationKind.USE).count());
        var debugging=control(source("DEBUG-SECTION SECTION.\nUSE FOR DEBUGGING ON ALL PROCEDURES.\nDBG-PARA.\nCALL 'DEBUGPGM'.","READ F."));
        assertEquals(Ast.UseKind.DEBUGGING,debugging.declaratives().get(0).kind());assertFalse(debugging.declaratives().get(0).gaps().isEmpty());
        assertTrue(route(read(debugging),FileIoControl.Event.END).destinations().stream().noneMatch(d->d.kind()==FileIoControl.DestinationKind.USE));
    }
    @Test void eopIsASuccessfulWriteConditionNotReadEof() {
        var f=fixture("SELECT F ASSIGN TO PRINTDD.","FD F LINAGE IS 60 LINES.\n01 REC PIC X(8).","01 SAFE PIC X(8).",
            "WRITE REC AT EOP CALL 'PAGEPGM'\n NOT AT EOP CALL 'NOTPAGE' END-WRITE.");
        var p=control(f).operations().get(0);var r=route(p,FileIoControl.Event.END_OF_PAGE);
        assertEquals(FileIoEffects.Outcome.SUCCESS,r.effects());assertEquals(Ast.FileHandlerKind.AT_END_OF_PAGE,r.destinations().get(0).handler().orElseThrow());
        assertTrue(p.routes().stream().noneMatch(x->x.event()==FileIoControl.Event.END));
    }
    @Test void unprovedFileOrUseBindingNeverClosesAwayAnErrorBody() {
        var c=control(source(FILE_USE,"READ MISSING.\nCALL 'AFTER'."));var p=read(c);
        assertTrue(p.routes().stream().anyMatch(r->r.event()==FileIoControl.Event.END));
        assertTrue(p.routes().stream().anyMatch(r->r.event()==FileIoControl.Event.INVALID_KEY));
        assertTrue(route(p,FileIoControl.Event.OTHER_ERROR).destinations().stream().anyMatch(d->d.kind()==FileIoControl.DestinationKind.USE));
        assertTrue(route(p,FileIoControl.Event.OTHER_ERROR).destinations().stream().anyMatch(d->d.kind()==FileIoControl.DestinationKind.CONTINUE));
        var unknown=control(source(FILE_USE.replace("ON F.","ON MISSING."),"READ F."));
        assertFalse(read(unknown).gaps().isEmpty());
        assertTrue(route(read(unknown),FileIoControl.Event.END).destinations().stream().anyMatch(d->d.kind()==FileIoControl.DestinationKind.USE));
        assertTrue(route(read(unknown),FileIoControl.Event.END).destinations().stream().anyMatch(d->d.kind()==FileIoControl.DestinationKind.CONTINUE));
    }
}
