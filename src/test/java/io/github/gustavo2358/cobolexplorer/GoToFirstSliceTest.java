package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static org.junit.jupiter.api.Assertions.*;

class GoToFirstSliceTest {
    @Test void focalTargetsOriginsAndMultiplicity() throws Exception {
        var out=Path.of("target/goto"); Files.createDirectories(out);
        try(var files=Files.list(Path.of("src/test/resources/cobol/goto"))) {
            for(var file:files.sorted().toList()) {
                var source=Files.readString(file); var p=ScalarMoveCheckpoint4ATest.publish(source);
                var name=file.getFileName().toString().replace(".cbl","");
                if(name.equals("depending")) {
                    assertTrue(p.goTos().isEmpty());
                    assertTrue(p.observedStatements().stream().anyMatch(o->o.observedKind().equals("GO_TO") && o.normalContinuation().statement().isEmpty()));
                } else {
                    assertFalse(p.goTos().isEmpty(),name);
                    if(name.startsWith("compose-"))assertEquals(Integer.parseInt(name.substring(8)),p.goTos().size());
                    for(var g:p.goTos()) {
                        if(Set.of("empty","unknown","alter","section","ambiguous").contains(name)) {
                            assertTrue(g.targetEntry().isEmpty(),name); assertFalse(g.gapCodes().isEmpty(),name);
                            if(name.equals("empty"))assertTrue(g.target().isPresent());
                        } else {
                            assertTrue(g.gapCodes().isEmpty(),name+g.gapCodes());
                            var target=p.statement(g.targetEntry().orElseThrow()).orElseThrow();
                            assertEquals(g.entryOrigin().orElseThrow(),target.header().provenance());
                            assertEquals(p.unit(),g.target().orElseThrow().id().unit());
                            assertTrue(g.referenceOrigin().exact());
                        }
                    }
                }
                if(name.equals("g1"))assertEquals(p.calls().get(0).header().id(),p.goTos().get(0).targetEntry().orElseThrow());
                if(name.equals("backward"))assertTrue(p.goTos().stream().anyMatch(g -> p.statement(g.targetEntry().orElseThrow()).orElseThrow().header().point().ordinal()<g.header().point().ordinal()));
                if(name.equals("perform-adjacent"))assertEquals(1,p.performs().size(),"closed primary may follow explicit GO TO to a disjoint paragraph");
                if(name.equals("perform-overlap"))assertTrue(p.performs().isEmpty(),"primary target cannot masquerade as isolated body");
                var bytes=SemanticProductJsonWriter.serialize(p);
                assertArrayEquals(bytes,SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)),name);
                Files.write(out.resolve(name+".json"),bytes);
            }
        }
    }
    @Test void unsupportedContainmentKeepsStructuralGap() {
        var p=ScalarMoveCheckpoint4ATest.publish(EvaluateFirstSliceTest.source("EVALUATE TRUE\nWHEN FLAG = 'Y' GO TO TARGET\nEND-EVALUATE.\nTARGET.\nCALL 'PROGA'."));
        var g=p.goTos().get(0); assertEquals(Branch.UNKNOWN,g.header().containment().branch());
        assertTrue(g.targetEntry().isEmpty());
        assertTrue(p.gaps().stream().anyMatch(gap->gap.statement().equals(g.header().id()) && gap.scope()==GapScope.STRUCTURE));
    }
    @Test void targetInAnotherProgramUnitRemainsUnresolved() {
        var source=EvaluateFirstSliceTest.source("GO TO CHILD-TARGET.\nCALL 'PROGA'.")
            + "IDENTIFICATION DIVISION.\nPROGRAM-ID. CHILD.\nPROCEDURE DIVISION.\nCHILD-TARGET.\nGOBACK.\nEND PROGRAM CHILD.\nEND PROGRAM EVALTEST.\n";
        var p=ScalarMoveCheckpoint4ATest.publish(source);
        assertEquals(1,p.goTos().size()); assertTrue(p.goTos().get(0).target().isEmpty());
        assertTrue(p.goTos().get(0).targetEntry().isEmpty());
    }
    @Test void publishesExplicitTargetWithoutFallthrough() throws Exception {
        var p=ScalarMoveCheckpoint4ATest.publish(EvaluateFirstSliceTest.source(
            "MOVE 'PROGA' TO WS-PGM.\nGO TO TARGET.\nMOVE 'PROGB' TO WS-PGM.\nTARGET.\nCALL WS-PGM."));
        var wire=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(p));
        var gotos=wire.get("statements").findValues("variant").stream().filter(v->v.asText().equals("GO_TO")).toList();
        assertEquals(1,gotos.size(),"GO TO must publish a typed transfer, not an observed unknown");
    }
}
