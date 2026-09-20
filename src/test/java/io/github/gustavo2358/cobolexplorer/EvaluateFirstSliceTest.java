package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static org.junit.jupiter.api.Assertions.*;

class EvaluateFirstSliceTest {
    static String source(String body) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. EVALTEST.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n01 WS-X PIC X.\n01 FLAG PIC X.\n01 WS-PGM PIC X(8).\nPROCEDURE DIVISION.\n" + body + "\nGOBACK.\n";
    }
    @Test void publishesTypedEvaluate() throws Exception {
        var p = ScalarMoveCheckpoint4ATest.publish(source("EVALUATE WS-X\nWHEN 'A' MOVE 'PROGA' TO WS-PGM\nWHEN 'B' MOVE 'PROGB' TO WS-PGM\nWHEN OTHER MOVE 'PROGC' TO WS-PGM\nEND-EVALUATE\nCALL WS-PGM."));
        var json = new String(SemanticProductJsonWriter.serialize(p), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(json.contains("\"variant\":\"EVALUATE\""), "EVALUATE must cross SP as a typed family");
    }
    @Test void focalFactsAndDeterministicSnapshots() throws Exception {
        var out=Path.of("target/evaluate"); Files.createDirectories(out);
        try(var files=Files.list(Path.of("src/test/resources/cobol/evaluate"))) {
            for(var file:files.sorted().toList()) {
                var source=Files.readString(file); var p=ScalarMoveCheckpoint4ATest.publish(source);
                var name=file.getFileName().toString().replace(".cbl","");
                {
                    assertFalse(p.evaluates().isEmpty(),name);
                    int count=name.startsWith("compose-")?Integer.parseInt(name.substring(8)):name.equals("e5")?2:1;
                    assertEquals(count,p.evaluates().size(),name);
                    for(var e:p.evaluates()) {
                        assertEquals(ContinuationAvailability.KNOWN,e.normalContinuation().availability(),name);
                        assertTrue(e.header().provenance().exact(),name);
                        var members=new HashSet<StatementId>();
                        for(int i=0;i<e.arms().size();i++) {
                            var arm=e.arms().get(i); assertEquals(i,arm.ordinal());
                            if(name.equals("also")) assertTrue(arm.selection().isEmpty());
                            else assertEquals(LiteralKind.ALPHANUMERIC,arm.selection().orElseThrow().kind());
                            assertTrue(arm.conditionOrigin().exact());
                            if(!name.equals("empty")) assertEquals(arm.statements().get(0),arm.control().entry().statement().orElseThrow());
                            for(var id:arm.statements()) { assertTrue(members.add(id));
                                assertEquals(new Containment(Optional.of(e.header().id()),Branch.EVALUATE_ARM),p.statement(id).orElseThrow().header().containment()); }
                        }
                        assertFalse(members.contains(e.normalContinuation().statement().orElseThrow()));
                        if(name.equals("unknown")) { assertEquals(CoverageStatus.PARTIAL,e.header().coverage()); assertFalse(e.gapCodes().isEmpty()); }
                        if(name.equals("e2") || name.equals("closed")) assertEquals(ClausePresence.ABSENT,e.otherArm().presence());
                        if(name.equals("empty")) { assertTrue(e.arms().get(0).control().entry().statement().isEmpty()); assertEquals(CoverageStatus.PARTIAL,e.header().coverage()); }
                    }
                    if(name.equals("e1")) {
                        var e=p.evaluates().get(0); assertEquals(List.of("A","B"),e.arms().stream().map(a->a.selection().orElseThrow().value()).toList());
                        var call=p.calls().get(0).header().id(); assertEquals(call,e.normalContinuation().statement().orElseThrow());
                        for(var m:p.moves()) assertEquals(call,m.normalContinuation().statement().orElseThrow());
                    }
                }
                var bytes=SemanticProductJsonWriter.serialize(p);
                assertArrayEquals(bytes,SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)),name);
                Files.write(out.resolve(name+".json"),bytes);
            }
        }
    }
    @Test void unsupportedSelectorsKeepOrderedArmsWithoutLiteralSemantics() {
        for(var selection:List.of("ANY","NOT 'A'","'A' THRU 'Z'","1","FLAG")) {
            var p=ScalarMoveCheckpoint4ATest.publish(source("EVALUATE WS-X\nWHEN "+selection+"\nCALL 'PROGA'\nEND-EVALUATE."));
            assertEquals(1,p.evaluates().size(),selection); assertEquals(1,p.calls().size());
            var arm=p.evaluates().get(0).arms().get(0);
            assertTrue(arm.selection().isEmpty(),selection);
            assertTrue(arm.conditionOrigin().exact(),selection);
            assertEquals(CoverageStatus.PARTIAL,p.evaluates().get(0).header().coverage(),selection);
        }
    }
    @Test void generalNestedEvaluateRemainsObserved() {
        var p=ScalarMoveCheckpoint4ATest.publish(source("EVALUATE WS-X\nWHEN 'A'\nEVALUATE FLAG\nWHEN 'Y' CALL 'PROGA'\nEND-EVALUATE\nWHEN OTHER CALL 'PROGB'\nEND-EVALUATE."));
        assertEquals(2,p.evaluates().size());
        assertEquals(2,p.calls().size());
        assertTrue(p.evaluates().stream().anyMatch(e -> e.arms().stream().anyMatch(a -> a.selection().isEmpty())));
    }
}
