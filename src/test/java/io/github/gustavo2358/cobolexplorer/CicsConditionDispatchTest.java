package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.ControlTopology.*;
class CicsConditionDispatchTest {
    static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort publish(String code) {
        return CicsMemoryLocalityTest.publish("01 RESPONSE-CODE PIC S9(9) COMP.",code,false);
    }
    @Test void registrationAndEventRemainSeparateFacts()throws Exception {
        var p=publish("EXEC CICS HANDLE CONDITION PGMIDERR(ERR-P)\n END-EXEC.\nEXEC CICS LINK PROGRAM('TARGET') END-EXEC.\nGOBACK.\nERR-P.\nCALL 'ERRORPGM'.\nGOBACK.");
        var t=p.controlTopology().orElseThrow();assertEquals(1,t.conditionRegistrations().size());assertEquals(1,t.conditionEvents().size());
        var r=t.conditionRegistrations().get(0);var e=t.conditionEvents().get(0);assertNotEquals(r.statement(),e.statement());
        assertEquals(ConditionAction.LABEL,r.action());assertEquals(TargetKind.REGION_ENTRY,r.target().get(0).kind());
        assertTrue(t.outcomes().stream().anyMatch(o->o.statement().equals(e.statement())&&o.kind()==OutcomeKind.NORMAL));
        assertEquals(EventEligibility.HANDLER_ELIGIBLE,e.eligibility());assertTrue(t.exceptionalEvents().stream().anyMatch(a->a.id().equals(e.defaultEvent())&&a.origin()==EventOrigin.LINK_PGMIDERR));
        assertEquals("2.65.0",CicsAbendContractTest.json(p).path("contractVersion").asText());
        assertTrue(publish("EXEC CICS HANDLE CONDITION PGMIDERR(ERR-P)\n END-EXEC.\nGOBACK.\nERR-P.\nGOBACK.").controlTopology().orElseThrow().conditionEvents().isEmpty());
    }
    @Test void omittedLabelAndIgnoreAreDistinctUpdates() {
        for(var code:List.of("HANDLE CONDITION PGMIDERR","IGNORE CONDITION PGMIDERR")) {
            var t=publish("EXEC CICS "+code+" END-EXEC.\nGOBACK.").controlTopology().orElseThrow();
            var r=t.conditionRegistrations().get(0);assertEquals(code.startsWith("IGNORE")?ConditionAction.IGNORE:ConditionAction.DEFAULT,r.action());assertTrue(r.target().isEmpty());
        }
    }
    @Test void respAndNohandleBypassOnlyThatEvent() {
        for(var option:List.of("RESP(RESPONSE-CODE)","NOHANDLE")) {
            var t=publish("EXEC CICS XCTL PROGRAM('TARGET')\n "+option+" END-EXEC.\nGOBACK.").controlTopology().orElseThrow();
            var e=t.conditionEvents().get(0);assertEquals(EventEligibility.HANDLERS_BYPASSED,e.eligibility());assertTrue(e.defaultEvent().isEmpty());
            assertTrue(t.exceptionalEvents().isEmpty());
        }
    }
    @Test void unresolvedRegistrationCannotInventExecutableTarget() {
        var t=publish("EXEC CICS HANDLE CONDITION PGMIDERR(MISSING-P)\n END-EXEC.\nGOBACK.").controlTopology().orElseThrow();
        assertTrue(t.conditionRegistrations().isEmpty());
    }
    @Test void unknownRestorationPublishesSourceAssumptionsOnlyForEligibleEvents() {
        for(var option:List.of(""," NOHANDLE")) {
            var t=publish("EXEC CICS HANDLE CONDITION PGMIDERR(ERR-P)\n END-EXEC.\nEXEC CICS POP HANDLE END-EXEC.\nEXEC CICS XCTL PROGRAM('TARGET')"+option+"\n END-EXEC.\nGOBACK.\nERR-P.\nGOBACK.").controlTopology().orElseThrow();
            var hypotheses=t.sourceContinuations().stream().filter(c->!c.prerequisites().isEmpty()).toList();
            assertEquals(option.isEmpty()?2:0,hypotheses.size());
            for(var h:hypotheses)assertTrue(h.proofs().stream().allMatch(id->t.proofs().stream().anyMatch(p->p.id().equals(id)&&p.kind()==ProofKind.CONTROL_POSSIBILITY)));
        }
    }
}
