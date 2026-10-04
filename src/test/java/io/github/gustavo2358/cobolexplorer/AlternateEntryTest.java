package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.ControlTopology.*;
class AlternateEntryTest {
    static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort publish(String code) {
        return CicsMemoryLocalityTest.publish("01 ARG PIC X(8).",code,false);
    }
    static StatementId call(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort p,String name) {
        return p.statements().stream().filter(s->s instanceof CallFact c&&c.target() instanceof LiteralCallTarget t&&t.text().equals(name)).findFirst().orElseThrow().header().id();
    }
    @Test void alternateRootStartsAfterDeclarationAndNotAtPrimary()throws Exception {
        var p=publish("CALL 'PRIMARY'.\nGOBACK.\nENTRY 'SECOND'.\nCALL 'ALT'.\nGOBACK.");
        assertEquals(EntryInventoryScope.SOURCE_DECLARED,p.entryInventory().scope());
        assertEquals(2,p.entryInventory().entries().size());var e=p.entryInventory().entries().get(1);
        assertEquals(Optional.of("SECOND"),e.externalName());assertEquals(call(p,"ALT"),e.start().statement().orElseThrow());
        var point=p.controlTopology().orElseThrow().entryPoints().get(0);
        assertEquals("statement:"+e.declaration().orElseThrow().localId(),point.declaration());assertEquals("statement:"+call(p,"ALT").localId(),point.target().reference());
        assertEquals("2.65.0",CicsAbendContractTest.json(p).path("contractVersion").asText());
    }
    @Test void consecutiveDeclarationsShareExecutableStartAndKeepOwnNames() {
        var p=publish("GOBACK.\nENTRY 'SECOND'.\nENTRY 'THIRD'.\nNEXT-PARA.\nCALL 'ALT'.\nGOBACK.");
        assertEquals(3,p.entryInventory().entries().size());
        assertEquals(1,p.controlTopology().orElseThrow().entryPoints().stream().map(e->e.target().reference()).distinct().count());
        assertEquals(Set.of("SECOND","THIRD"),p.entryInventory().entries().stream().flatMap(e->e.externalName().stream()).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void usingPreservesUnknownRuntimeSignature() {
        var p=CicsMemoryLocalityTest.publish("LINKAGE SECTION.\n01 ARG PIC X(8).","GOBACK.\nENTRY 'SECOND' USING ARG.\nCALL ARG.\nGOBACK.",false);
        var e=p.entryInventory().entries().get(1);assertEquals(Availability.KNOWN,e.start().availability());
        assertEquals(Availability.PARTIAL,e.signature().availability());assertEquals(Optional.of(1),e.signature().parameterCount());
        assertTrue(e.gaps().stream().anyMatch(g->g.scope()==GapScope.ENTRY_SIGNATURE));
    }
    @Test void conflictingNamesRetainUnavailableDeclarationsWithoutRoots() {
        for(var code:List.of("ENTRY 'SAMPLE'.\nGOBACK.","ENTRY 'SECOND'.\nGOBACK.\nENTRY 'SECOND'.\nGOBACK.")) {
            var p=publish(code);assertTrue(p.controlTopology().orElseThrow().entryPoints().isEmpty());
            assertTrue(p.entryInventory().entries().stream().filter(e->e.role()==EntryRole.ALTERNATE).allMatch(e->e.availability()==Availability.UNAVAILABLE&&!e.gaps().isEmpty()));
        }
    }
    @Test void sequentialEntryRemainsNeutral() {
        var p=publish("CALL 'BEFORE'.\nENTRY 'SECOND'.\nCALL 'AFTER'.\nGOBACK.");
        var t=p.controlTopology().orElseThrow();var e=p.entryInventory().entries().get(1);
        assertTrue(t.outcomes().stream().anyMatch(o->o.statement().equals("statement:"+e.declaration().orElseThrow().localId())&&o.kind()==OutcomeKind.NORMAL&&o.target().kind()==TargetKind.OCCURRENCE&&o.target().reference().equals("statement:"+call(p,"AFTER").localId())));
    }
    @Test void returningAndNestedProgramsKeepEntryRestrictions() {
        var source="IDENTIFICATION DIVISION.\nPROGRAM-ID. OUTER-P.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n01 RESULT-N PIC S9(9) COMP.\nPROCEDURE DIVISION RETURNING RESULT-N.\nGOBACK.\nENTRY 'ALTPOINT'.\nCALL 'ALT'.\nGOBACK.\n";
        var analysis=AstBoundaryTestSupport.analyze(source,"entry-returning.cbl");
        var p=EofUnitBoundaryTest.publish(analysis,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertTrue(p.controlTopology().orElseThrow().entryPoints().isEmpty());
        assertTrue(p.entryInventory().entries().get(1).gaps().stream().anyMatch(g->g.code().equals("ALTERNATE_ENTRY_WITH_RETURNING")));
        var nested=source.replace(" RETURNING RESULT-N","").replace("ENTRY 'ALTPOINT'.\nCALL 'ALT'.\nGOBACK.\n", "IDENTIFICATION DIVISION.\nPROGRAM-ID. INNER-P.\nPROCEDURE DIVISION.\nGOBACK.\nENTRY 'ALTPOINT'.\nCALL 'ALT'.\nGOBACK.\nEND PROGRAM INNER-P.\nEND PROGRAM OUTER-P.\n");
        var a=AstBoundaryTestSupport.analyze(nested,"entry-nested.cbl");assertEquals(2,a.model().programUnits().size());
        var inner=EofUnitBoundaryTest.publish(a,1,StorageLayoutSemantics.Profile.UNSPECIFIED);
        assertTrue(inner.controlTopology().orElseThrow().entryPoints().isEmpty());
        assertTrue(inner.entryInventory().entries().get(1).gaps().stream().anyMatch(g->g.code().equals("ALTERNATE_ENTRY_IN_NESTED_PROGRAM")));
    }
}
