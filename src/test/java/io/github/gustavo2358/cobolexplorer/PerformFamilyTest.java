package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class PerformFamilyTest {
    static String source(String main, String body) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. FAMILY.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n"
            + "01 WS-PGM PIC X(8).\n01 FLAG PIC X.\nPROCEDURE DIVISION.\nMAIN.\n"
            + main + "\nCALL WS-PGM.\nGOBACK.\n" + body;
    }
    static JsonNode publish(String source) throws Exception {
        return new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)));
    }
    static List<JsonNode> ranges(JsonNode sp) {
        var result=new ArrayList<JsonNode>();
        for(var s:sp.path("statements"))if(s.path("variant").asText().equals("PERFORM_PROCEDURE"))result.add(s);
        return result;
    }
    @Test void thruRangeIdentitiesCompletionAndIndependentResumes() throws Exception {
        var sp=publish(source("PERFORM A THRU C.\nPERFORM A THROUGH C.",
            "A.\nMOVE 'PROGA' TO WS-PGM.\nB.\nMOVE 'PROGB' TO WS-PGM.\nC.\nMOVE 'PROGC' TO WS-PGM.\n"));
        var facts=ranges(sp);assertEquals(2,facts.size());assertEquals("2.65.0",sp.path("contractVersion").asText());
        for(var p:facts) {
            assertTrue(p.path("gapCodes").isEmpty(),p.toString());assertEquals(3,p.path("procedures").size());
            assertEquals(p.path("start").path("id"),p.path("procedures").get(0).path("id"));
            assertEquals(p.path("end").path("id"),p.path("procedures").get(2).path("id"));
            for(var procedure:p.path("procedures")) {
                assertEquals(1,procedure.path("statements").size());assertEquals(1,procedure.path("completions").size());
                assertEquals(procedure.path("entry"),procedure.path("statements").get(0));
            }
        }
        assertNotEquals(facts.get(0).path("normalContinuation").path("statement"),facts.get(1).path("normalContinuation").path("statement"));
    }
    @Test void allThruFocalsAdversarialAndMultiplicity() throws Exception {
        var partial=Set.of("unknown-body","incoming","escape","overlap","recursive","cycle","partial-end","partial-start","reverse","empty");
        var root=Path.of("src/test/resources/cobol/perform-family");var output=Path.of("target/perform-family");Files.createDirectories(output);
        try(var files=Files.list(root)) {
            for(var path:files.filter(p->p.toString().endsWith(".cbl")&&!p.getFileName().toString().startsWith("until-")&&!p.getFileName().toString().startsWith("times-")&&!p.getFileName().toString().startsWith("varying-")).sorted().toList()) {
                var name=path.getFileName().toString().replace(".cbl","");
                var source=Files.readAllLines(path).stream().map(line->line.substring(7)).collect(java.util.stream.Collectors.joining("\n"));
                var port=ScalarMoveCheckpoint4ATest.publish(source);var bytes=SemanticProductJsonWriter.serialize(port);
                var sp=new ObjectMapper().readTree(bytes);var facts=ranges(sp);assertFalse(facts.isEmpty(),name);
                for(var p:facts)assertEquals(!partial.contains(name),p.path("header").path("readiness").path("lowering").path("status").asText().equals("SUFFICIENT"),name+": "+p.path("gapCodes"));
                if(name.startsWith("thru-"))assertEquals(Integer.parseInt(name.substring(5)),facts.size());
                assertArrayEquals(bytes,SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)),name);
                Files.write(output.resolve(name+".json"),bytes);
            }
        }
    }
    @Test void openThruCannotCertifyNeighborBasicActivation() {
        var source=source("PERFORM A THRU MISSING.\nPERFORM B.","A.\nMOVE 'A' TO WS-PGM.\nB.\nMOVE 'B' TO WS-PGM.\n");
        var port=ScalarMoveCheckpoint4ATest.publish(source);
        assertTrue(port.performs().isEmpty(),"open range cannot certify isolated BASIC body");
    }
    @Test void unprovedRangesRemainTypedAndPartial() throws Exception {
        for(var target:List.of("C THRU A","A THRU MISSING","MISSING THRU C")) {
            var facts=ranges(publish(source("PERFORM "+target+".","A.\nMOVE 'A' TO WS-PGM.\nC.\nMOVE 'C' TO WS-PGM.\n")));
            assertEquals(1,facts.size());assertFalse(facts.get(0).path("gapCodes").isEmpty());
        }
    }

    private static List<ProcedurePerformSemantics.Facts> analyzedRanges(String source) {
        var a=AstBoundaryTestSupport.analyze(source,"shared-perform-proofs.cbl");
        var semantics=ScalarMoveSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report());
        var unit=a.model().programUnits().get(0).id();
        return AstBoundaryTestSupport.nodes(a,Ast.PerformStatement.class).stream()
            .sorted(Comparator.comparingInt(p->p.meta().id()))
            .map(p->semantics.procedurePerforms().fact(unit,p.meta().id()).orElseThrow()).toList();
    }
    @Test void identicalBodiesKeepCallerSpecificPrimaryMembershipAndResume() {
        var facts=analyzedRanges(source("PERFORM A THRU A.",
            "DEAD.\nPERFORM A THRU A.\nGOBACK.\nA.\nMOVE 'PROGA' TO WS-PGM."));
        assertEquals(2,facts.size());
        assertTrue(facts.get(0).precise(),facts.get(0).gaps().toString());
        assertFalse(facts.get(1).precise());
        assertEquals(facts.get(0).procedures(),facts.get(1).procedures());
        assertNotEquals(facts.get(0).resume(),facts.get(1).resume());
    }
    @Test void sharedBodyProofsKeepUnreachableIncomingAndUnequalOverlapRefusals() {
        for(String transfer:List.of("GO TO A.","GO TO MISSING.")) {
            var facts=analyzedRanges(source("PERFORM A THRU A.\nPERFORM A THRU A.",
                "DEAD.\n"+transfer+"\nA.\nMOVE 'PROGA' TO WS-PGM."));
            assertEquals(2,facts.size());
            for(var fact:facts)assertFalse(fact.precise(),fact.gaps().toString());
        }
        var facts=analyzedRanges(source("PERFORM A THRU B.\nPERFORM A THRU B.\nPERFORM B THRU C.",
            "A.\nMOVE 'PROGA' TO WS-PGM.\nB.\nMOVE 'PROGB' TO WS-PGM.\nC.\nMOVE 'PROGC' TO WS-PGM."));
        assertEquals(3,facts.size());
        for(var fact:facts)assertTrue(fact.gaps().contains("PERFORM_OVERLAPPING_RANGES"),fact.gaps().toString());
    }

    @Test void proofsAreIsolatedBetweenProgramUnitsAndExecutions() {
        String main="PERFORM A THRU A.\nPERFORM A THRU A.";
        String closed=source(main,"A.\nMOVE 'PROGA' TO WS-PGM.").replace("PROGRAM-ID. FAMILY.","PROGRAM-ID. CLOSED-UNIT.")+"\nEND PROGRAM CLOSED-UNIT.\n";
        String open=source(main,"DEAD.\nGO TO A.\nA.\nMOVE 'PROGA' TO WS-PGM.").replace("PROGRAM-ID. FAMILY.","PROGRAM-ID. OPEN-UNIT.")+"\nEND PROGRAM OPEN-UNIT.\n";
        for(var sources:List.of(List.of(closed,open),List.of(open,closed))) {
            var a=AstBoundaryTestSupport.analyze(String.join("",sources),"unit-proof-scope.cbl");
            var semantics=ScalarMoveSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report());
            assertEquals(2,a.model().programUnits().size());
            for(int i=0;i<2;i++) {
                var program=a.model().programUnits().get(i);var unit=program.id();
                var facts=AstBoundaryTestSupport.nodes(program.program()).stream()
                    .filter(Ast.PerformStatement.class::isInstance)
                    .map(p->semantics.procedurePerforms().fact(unit,p.meta().id()).orElseThrow()).toList();
                assertEquals(2,facts.size());
                for(var fact:facts)assertEquals(sources.get(i).equals(open),
                    !fact.precise(),fact.gaps().toString());
            }
        }
    }
}
