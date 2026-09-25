package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static org.junit.jupiter.api.Assertions.*;

class CicsHandlerContractTest {
    static JsonNode publish(String code) throws Exception {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("01 PGM PIC X(8).",code),"handlers.cbl");
        return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(HistoricalCicsHandler241TestSupport.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED)));
    }
    @Test void registrationCancelAndResetAreSeparateOperations() throws Exception {
        var json=publish("EXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC.\nEXEC CICS HANDLE ABEND CANCEL END-EXEC.\nEXEC CICS HANDLE ABEND RESET END-EXEC.\nGOBACK.\nERR.\nGOBACK.");
        assertEquals("2.41.0",json.path("contractVersion").asText());
        var statements=json.path("statements");
        for(int i=0;i<3;i++)assertEquals("CICS_HANDLER",statements.get(i).path("variant").asText());
        assertEquals("ACTIVATE",statements.get(0).path("action").asText());
        assertEquals("CANCEL",statements.get(1).path("action").asText());
        assertEquals("RESET",statements.get(2).path("action").asText());
        assertEquals("LABEL",statements.get(0).path("targetKind").asText());
        assertFalse(statements.get(0).path("labelTarget").isNull());
    }

    static CobolSemanticPort port(String code) {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("01 PGM PIC X(8).\n01 RC PIC S9(8) COMP.\n01 RC2 PIC S9(8) COMP.",code),"handlers.cbl");
        return HistoricalCicsHandler241TestSupport.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
    }
    static List<CicsHandlerFact> handlers(CobolSemanticPort p) {return p.statements().stream().filter(CicsHandlerFact.class::isInstance).map(CicsHandlerFact.class::cast).toList();}
    @Test void overwritePreservesTwoOperationsAndCanonicalTargets() {
        var p=port("EXEC CICS HANDLE ABEND LABEL(ERR1) END-EXEC.\nEXEC CICS HANDLE ABEND LABEL(ERR2) END-EXEC.\nGOBACK.\nERR1.\nGOBACK.\nERR2.\nGOBACK.");
        var hs=handlers(p);assertEquals(2,hs.size());
        assertNotEquals(hs.get(0).header().id(),hs.get(1).header().id());assertNotEquals(hs.get(0).labelTarget(),hs.get(1).labelTarget());
        for(var h:hs) {
            assertEquals(CicsHandlerAction.ACTIVATE,h.action());assertEquals(ResolutionStatus.RESOLVED,h.labelBindingStatus().orElseThrow());
            assertEquals(p.unit(),h.labelTarget().orElseThrow().id().unit());assertTrue(h.targetEntry().isPresent());
            assertEquals(h.entryOrigin().orElseThrow(),p.statement(h.targetEntry().orElseThrow()).orElseThrow().header().provenance());
            assertEquals(CicsHandlerScopeKind.CURRENT_EXECUTION_LOGICAL_LEVEL,h.scope().kind());assertEquals(Availability.UNAVAILABLE,h.scope().runtimeIdentity());
            assertFalse(h.targetOrigin().orElseThrow().exact(),"embedded source provenance is not promoted to exact");
        }
    }
    @Test void defaultCancelAndResetHaveNoNewTargetOrRuntimeState() {
        var hs=handlers(port("EXEC CICS HANDLE ABEND END-EXEC.\nEXEC CICS HANDLE ABEND RESET END-EXEC.\nGOBACK."));
        assertEquals(List.of(CicsHandlerAction.CANCEL,CicsHandlerAction.RESET),hs.stream().map(CicsHandlerFact::action).toList());
        for(var h:hs){assertEquals(CicsHandlerTargetKind.NONE,h.targetKind());assertTrue(h.labelTarget().isEmpty());assertTrue(h.programTarget().isEmpty());assertTrue(h.targetSyntax().isEmpty());}
    }
    @Test void programLiteralAndHostKeepDifferentBindingKinds() {
        var hs=handlers(port("EXEC CICS HANDLE ABEND PROGRAM('ExitPgm') END-EXEC.\nEXEC CICS HANDLE ABEND PROGRAM(PGM) END-EXEC.\nGOBACK."));
        assertEquals(2,hs.size());assertEquals("ExitPgm",((LiteralCallTarget)hs.get(0).programTarget().orElseThrow()).text());
        var data=(DataReference)hs.get(1).programTarget().orElseThrow();assertTrue(data.binding().selected().isPresent());
        assertTrue(hs.stream().allMatch(h->h.labelBindingStatus().isEmpty()&&h.labelTarget().isEmpty()));
    }
    @Test void unknownAndAmbiguousLabelNeverPickNearbyDeclaration() {
        var missing=handlers(port("EXEC CICS HANDLE ABEND LABEL(ABSENT) END-EXEC.\nGOBACK.\nERR.\nGOBACK.")).get(0);
        assertEquals(CicsHandlerAction.ACTIVATE,missing.action());assertEquals("ABSENT",missing.targetSyntax().orElseThrow());assertTrue(missing.labelTarget().isEmpty());assertTrue(missing.targetEntry().isEmpty());
        var ambiguous=handlers(port("EXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC.\nGOBACK.\nSEC1 SECTION.\nERR.\nGOBACK.\nSEC2 SECTION.\nERR.\nGOBACK.")).get(0);
        assertEquals(ResolutionStatus.AMBIGUOUS,ambiguous.labelBindingStatus().orElseThrow());assertTrue(ambiguous.labelTarget().isEmpty());
        var qualified=handlers(port("EXEC CICS HANDLE ABEND LABEL(ERR IN SEC2) END-EXEC.\nGOBACK.\nSEC1 SECTION.\nERR.\nGOBACK.\nSEC2 SECTION.\nERR.\nGOBACK.")).get(0);
        assertEquals(ResolutionStatus.RESOLVED,qualified.labelBindingStatus().orElseThrow());
    }
    @Test void invalidSelectorsPreserveOccurrenceWithoutInventingAction() {
        for(var code:List.of("LABEL(ERR) CANCEL","CANCEL RESET","LABEL(ERR) LABEL(ERR)","CANCEL(X)","LABEL()","MYSTERY")) {
            var h=handlers(port("EXEC CICS HANDLE ABEND "+code+" END-EXEC.\nGOBACK.\nERR.\nGOBACK.")).get(0);
            assertEquals(CicsHandlerAction.UNAVAILABLE,h.action(),code);assertEquals(CicsHandlerTargetKind.UNAVAILABLE,h.targetKind());assertTrue(h.labelTarget().isEmpty());assertTrue(h.programTarget().isEmpty());
        }
        assertTrue(CicsHandlerSyntax.parse("EXEC CICS HANDLE ABEND LABEL(ERR)").orElseThrow().action()==CicsHandlerSyntax.Action.UNAVAILABLE);
    }
    @Test void conditionOptionsDoNotTurnIntoHandlerActions() {
        var p=port("EXEC CICS HANDLE ABEND LABEL(ERR)\nRESP(RC) RESP2(RC2) NOHANDLE END-EXEC.\nEXEC CICS LINK PROGRAM(PGM) RESP(RC) END-EXEC.\nEXEC CICS XCTL PROGRAM(PGM) NOHANDLE END-EXEC.\nGOBACK.\nERR.\nGOBACK.");
        var hs=handlers(p);assertEquals(1,hs.size());assertEquals(CicsHandlerAction.ACTIVATE,hs.get(0).action());
        assertEquals(2,hs.get(0).options().stream().filter(o->o.reference().isPresent()).count());
        assertEquals(2,p.statements().stream().filter(CicsFact.class::isInstance).count());
        assertTrue(hs.get(0).options().stream().filter(o->Set.of("RESP","RESP2").contains(o.name())).allMatch(o->o.reference().orElseThrow().role()==OperandRole.WRITE));
    }
    @Test void handlerConditionAndAbendDispatchRemainOutsideThisVariant() {
        var p=port("EXEC CICS HANDLE CONDITION ERROR(ERR) END-EXEC.\nEXEC CICS ABEND ABCODE('9999') CANCEL END-EXEC.\nGOBACK.\nERR.\nGOBACK.");
        assertTrue(handlers(p).isEmpty());assertEquals(2,p.observedStatements().size());
    }
    @Test void duplicateTextAcrossUnitsBindsByOwnedOccurrence() {
        String body="EXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC.\nGOBACK.\nERR.\nGOBACK.\n";
        String source="IDENTIFICATION DIVISION.\nPROGRAM-ID. A.\nPROCEDURE DIVISION.\n"+body+"END PROGRAM A.\nIDENTIFICATION DIVISION.\nPROGRAM-ID. B.\nPROCEDURE DIVISION.\n"+body+"END PROGRAM B.\n";
        var a=AstBoundaryTestSupport.analyze(source,"two.cbl");assertEquals(2,a.model().programUnits().size());
        var first=handlers(HistoricalCicsHandler241TestSupport.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED)).get(0);
        var second=handlers(HistoricalCicsHandler241TestSupport.publish(a,1,StorageLayoutSemantics.Profile.UNSPECIFIED)).get(0);
        assertNotEquals(first.labelTarget().orElseThrow().declarationOrigin(),second.labelTarget().orElseThrow().declarationOrigin());
        assertNotEquals(first.header().id(),second.header().id());assertNotEquals(first.labelTarget().orElseThrow().id(),second.labelTarget().orElseThrow().id());
        assertEquals(first.header().id().unit(),first.labelTarget().orElseThrow().id().unit());assertEquals(second.header().id().unit(),second.labelTarget().orElseThrow().id().unit());
    }
    @Test void copyExpandedHandlersKeepEachOccurrenceOrigin() throws Exception {
        var p=port("COPY CICSHAND.\nCOPY CICSHAND.\nGOBACK.\nERR.\nGOBACK.");var hs=handlers(p);assertEquals(2,hs.size());
        assertNotEquals(hs.get(0).header().id(),hs.get(1).header().id());
        assertNotEquals(hs.get(0).header().provenance(),hs.get(1).header().provenance());
        for(var h:hs){assertTrue(h.labelTarget().isPresent());assertTrue(new String(SemanticProductJsonWriter.serialize(p),java.nio.charset.StandardCharsets.UTF_8).contains("CICSHAND.cpy"));}
    }
    @Test void typedOperationDoesNotAddDispatchOrContinuation() throws Exception {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("01 PGM PIC X(8).","EXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC.\nEXEC CICS XCTL PROGRAM(PGM) NOHANDLE END-EXEC.\nGOBACK.\nERR.\nGOBACK."),"handlers.cbl");
        var p=HistoricalCicsHandler241TestSupport.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        var legacy=ScalarMoveCheckpoint4ATest.products(a);
        var cics=new CicsProgramControlAnalyzer().analyze(a.build(),a.report());
        var without=io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.open(new io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.FrontendProducts(
            legacy.frontend(),legacy.symbolTables(),legacy.occurrencesByUnit(),legacy.resolution(),legacy.report(),legacy.scalarMoves(),legacy.storage(),Optional.of(cics)),a.model().programUnits().get(0).id());
        assertEquals(without.controlTopology().orElseThrow(),p.controlTopology().orElseThrow());
        assertEquals(without.ordinaryContinuations(),p.ordinaryContinuations());
        String id="statement:"+handlers(p).get(0).header().id().localId();
        var outcomes=p.controlTopology().orElseThrow().outcomes().stream().filter(o->o.statement().equals(id)).toList();
        assertEquals(1,outcomes.size());assertEquals(ControlTopology.OutcomeKind.UNKNOWN_LOCAL,outcomes.get(0).kind());
        assertEquals(ControlTopology.TargetKind.UNKNOWN_LOCAL,outcomes.get(0).target().kind());
        assertTrue(handlers(p).get(0).gapCodes().contains("CICS_HANDLER_EXECUTION_STATE_NOT_MODELED"));
    }
    @Test void deterministicPublicationAndMissingProgramStayBounded() throws Exception {
        String code="EXEC CICS HANDLE ABEND PROGRAM(ABSENT) END-EXEC.\nEXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC.\nGOBACK.\nERR.\nGOBACK.";
        assertArrayEquals(SemanticProductJsonWriter.serialize(port(code)),SemanticProductJsonWriter.serialize(port(code)));
        var first=handlers(port(code)).get(0);assertEquals(CicsHandlerAction.ACTIVATE,first.action());
        first.programTarget().ifPresent(t->assertTrue(((DataReference)t).binding().selected().isEmpty()));
    }

    static CicsHandlerFact altered(CicsHandlerFact h,CicsHandlerAction action,Optional<ResolutionStatus> status,Optional<CicsHandlerLabelTarget> target) {
        return new CicsHandlerFact(h.header(),h.handlerKind(),action,h.targetKind(),h.targetSyntax(),status,target,h.targetEntry(),h.entryOrigin(),h.targetOrigin(),h.programTarget(),h.scope(),h.rawText(),h.options(),h.gapCodes());
    }
    @Test void contractRejectsForeignIdentityAndContradictoryOperation() {
        var h=handlers(port("EXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC.\nGOBACK.\nERR.\nGOBACK.")).get(0);
        assertThrows(IllegalArgumentException.class,()->altered(h,CicsHandlerAction.CANCEL,h.labelBindingStatus(),h.labelTarget()));
        assertThrows(IllegalArgumentException.class,()->altered(h,h.action(),Optional.of(ResolutionStatus.UNRESOLVED),h.labelTarget()));
        var target=h.labelTarget().orElseThrow();
        var foreign=new CicsHandlerLabelTarget(new ProcedureId(new UnitId("foreign",List.of(99),"FOREIGN"),target.id().localId()),target.declarationOrigin());
        assertThrows(IllegalArgumentException.class,()->altered(h,h.action(),h.labelBindingStatus(),Optional.of(foreign)));
        assertThrows(IllegalArgumentException.class,()->new CicsHandlerScope(CicsHandlerScopeKind.CURRENT_EXECUTION_LOGICAL_LEVEL,Availability.KNOWN,h.scope().provenance()));
    }
    @Test void missingCopyDoesNotEraseIndependentOccurrenceAndLiteral() {
        var p=port("MOVE 'KNOWN' TO PGM.\nCALL 'STATIC'.\nCOPY ABSENT-MEMBER.\nEXEC CICS HANDLE ABEND LABEL(UNKNOWN-EXIT) END-EXEC.\nGOBACK.");
        assertEquals(1,p.moves().size());assertEquals(1,p.calls().size());assertEquals(1,handlers(p).size());
        assertEquals("STATIC",((LiteralCallTarget)p.calls().get(0).target()).text());assertTrue(handlers(p).get(0).labelTarget().isEmpty());
        assertEquals("UNKNOWN-EXIT",handlers(p).get(0).targetSyntax().orElseThrow());
    }
}
