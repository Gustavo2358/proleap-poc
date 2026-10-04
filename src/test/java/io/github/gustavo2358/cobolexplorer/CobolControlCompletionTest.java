package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CobolControlCompletionTest {
    static com.fasterxml.jackson.databind.JsonNode publish(String data,String body)throws Exception{return CicsAbendContractTest.json(CicsMemoryLocalityTest.publish(data,body,false));}
    @Test void nextSentenceHasItsOwnScopeEscape()throws Exception {
        var j=publish("01 FLAG-X PIC X.","PERFORM P\nCALL 'AFTERP'\nGOBACK.\nP.\nPERFORM 2 TIMES\nIF FLAG-X = 'Y' NEXT SENTENCE END-IF\nCALL 'SKIPPED'\nEND-PERFORM.\nCALL 'AFTERDOT'.");
        assertEquals("2.65.0",j.path("contractVersion").asText());
        var t=j.path("controlTopology");assertTrue(t.path("regions").findValuesAsText("kind").contains("SENTENCE"));
        assertTrue(java.util.stream.StreamSupport.stream(t.path("outcomes").spliterator(),false).anyMatch(o->o.path("role").asText().equals("next-sentence")&&o.path("target").path("kind").asText().equals("ESCAPE")));
    }
    @Test void binarySearchPublishesDistinctBodies()throws Exception {
        var j=publish("01 T.\n 05 ROW-X OCCURS 3 ASCENDING KEY KEY-X\n INDEXED BY IX.\n  10 KEY-X PIC X.","SEARCH ALL ROW-X\nAT END CALL 'MISS'\nWHEN KEY-X(IX) = 'A' CALL 'MATCHED'\nEND-SEARCH\nCALL 'AFTERIO'\nGOBACK.");
        var t=j.path("controlTopology");assertTrue(t.path("regions").findValuesAsText("kind").contains("SEARCH"));
        assertEquals(2,t.path("regions").findValuesAsText("kind").stream().filter("SEARCH_ARM"::equals).count());
    }
    @Test void haltDiffersFromContextDependentExit()throws Exception {
        var stop=publish("","STOP RUN.\nCALL 'DEAD'.");assertTrue(stop.path("controlTopology").path("outcomes").findValuesAsText("kind").contains("PROGRAM_HALT"));
        var exit=publish("","EXIT PROGRAM.\nCALL 'MAINONLY'.\nGOBACK.");
        var outcomes=CicsCommandContractTest.outcomes(exit,exit.path("statements").get(0));
        assertTrue(outcomes.stream().anyMatch(o->o.path("kind").asText().equals("NORMAL")));
        assertTrue(outcomes.stream().anyMatch(o->o.path("kind").asText().equals("PROGRAM_RETURN")));
    }
    @Test void containedProgramExitHasNoMainAlternative()throws Exception {
        var source="IDENTIFICATION DIVISION.\nPROGRAM-ID. OUTER-P.\nPROCEDURE DIVISION.\nGOBACK.\nIDENTIFICATION DIVISION.\nPROGRAM-ID. INNER-P.\nPROCEDURE DIVISION.\nEXIT PROGRAM.\nCALL 'DEAD'.\nEND PROGRAM INNER-P.\nEND PROGRAM OUTER-P.";
        var a=AstBoundaryTestSupport.analyze(source,"nested-exit.cbl");
        var j=CicsAbendContractTest.json(EofUnitBoundaryTest.publish(a,1,StorageLayoutSemantics.Profile.UNSPECIFIED));
        var outcomes=CicsCommandContractTest.outcomes(j,j.path("statements").get(0));
        assertEquals(1,outcomes.size());assertEquals("PROGRAM_RETURN",outcomes.get(0).path("kind").asText());
    }
}
