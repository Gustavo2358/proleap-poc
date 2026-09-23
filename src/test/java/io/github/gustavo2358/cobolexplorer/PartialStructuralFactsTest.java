package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PartialStructuralFactsTest {
    @Test void callBodyRetainsTargetEntryAndCallsiteResume() throws Exception {
        var sp=PerformFamilyTest.publish(PerformFamilyTest.source("PERFORM A.","A.\nCALL WS-PGM.\n"));
        var facts=PerformFamilyTest.ranges(sp);
        assertEquals(1,facts.size(),"a partial BASIC must publish typed structure");
        var p=facts.get(0);
        assertFalse(p.path("start").isNull());
        assertEquals("KNOWN",p.path("normalContinuation").path("availability").asText());
        assertEquals(1,p.path("procedures").size());
        var paragraph=p.path("procedures").get(0);
        assertEquals(paragraph.path("entry"),paragraph.path("statements").get(0));
        assertEquals(1,paragraph.path("completions").size());
        assertEquals("STRUCTURAL_FACTS",p.path("publicationKind").asText());
    }
    @Test void nestedActivationsKeepBothTargetsAndConditionalFrontiers() throws Exception {
        var sp=PerformFamilyTest.publish(PerformFamilyTest.source("PERFORM A.","A.\nPERFORM B.\nB.\nMOVE 'B' TO WS-PGM.\n"));
        var facts=PerformFamilyTest.ranges(sp);assertEquals(2,facts.size());
        for(var p:facts) {
            assertTrue(p.path("start").isObject());assertEquals(1,p.path("procedures").size());
            assertEquals(1,p.path("procedures").get(0).path("completions").size());
        }
    }
    @Test void neutralEndpointsHaveNormalCompletion() throws Exception {
        for(var endpoint:List.of("EXIT.","CONTINUE.")) {
            var sp=PerformFamilyTest.publish(PerformFamilyTest.source("PERFORM A THRU Z.","A.\nMOVE 'B' TO WS-PGM.\nZ.\n"+endpoint));
            var range=PerformFamilyTest.ranges(sp).get(0);
            assertEquals(1,range.path("procedures").get(1).path("completions").size(),endpoint);
        }
    }

    @Test void unrelatedPeerDoesNotEraseBasicAndMultiplicityPreservesResumes() throws Exception {
        var base=PerformFamilyTest.source("PERFORM A.","A.\nMOVE 'B' TO WS-PGM.\n");
        var without=ScalarMoveCheckpoint4ATest.publish(base).performs().get(0);
        var with=ScalarMoveCheckpoint4ATest.publish(base+"UNUSED.\nPERFORM U 0 TIMES.\nGOBACK.\nU.\nMOVE 'U' TO WS-PGM.\n").performs().get(0);
        assertEquals(without.target(),with.target());assertEquals(without.targetEntry(),with.targetEntry());
        assertEquals(without.normalContinuation(),with.normalContinuation());assertEquals(without.targetExit(),with.targetExit());
        for(int n:List.of(1,2,5,40)) {
            var sp=PerformFamilyTest.publish(PerformFamilyTest.source("PERFORM A.\n".repeat(n),"A.\nCALL WS-PGM.\n"));
            var ps=PerformFamilyTest.ranges(sp);assertEquals(n,ps.size());
            assertEquals(n,ps.stream().map(x->x.path("header").path("id")).distinct().count());
            assertEquals(n,ps.stream().map(x->x.path("normalContinuation").path("statement")).distinct().count());
            assertEquals(1,ps.stream().map(x->x.path("start").path("id")).distinct().count());
        }
    }
    @Test void moveToCallChangesSpecializationButNotPositiveEntryAndResume() throws Exception {
        var move=ScalarMoveCheckpoint4ATest.publish(PerformFamilyTest.source("PERFORM A.","A.\nMOVE 'B' TO WS-PGM.\n"));
        var call=ScalarMoveCheckpoint4ATest.publish(PerformFamilyTest.source("PERFORM A.","A.\nCALL WS-PGM.\n"));
        var a=move.performs().get(0);
        var b=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.ProcedurePerformFact)call.statements().stream()
            .filter(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.ProcedurePerformFact.class::isInstance).findFirst().orElseThrow();
        assertEquals(a.target().orElseThrow().id(),b.start().orElseThrow().id());
        assertEquals(a.target().orElseThrow().referenceOrigin(),b.start().orElseThrow().referenceOrigin());
        assertEquals(a.normalContinuation(),b.normalContinuation());
        assertEquals(a.targetEntry().orElseThrow(),b.procedures().get(0).entry());
        assertEquals(a.targetExit().orElseThrow(),b.procedures().get(0).completions().get(0));
    }
    @Test void independentCoverageDoesNotEraseIfArmsOrPerformFacts() throws Exception {
        var source=PerformFamilyTest.source("IF FLAG = 'X' PERFORM A ELSE PERFORM B END-IF.",
            "A.\nCALL WS-PGM.\nB.\nCALL WS-PGM.\n");
        var a=AstBoundaryTestSupport.analyze(source,"scalar.cbl");var unit=a.model().programUnits().get(0).id();
        var findings=a.build().coverageByProgramUnit().get(unit).findings();
        var changed=findings.stream().map(f->new SemanticCoverage.Finding(f.id(),f.grammarRule(),f.meta(),f.writtenText(),
            f.grammarRule().equals("ifStatement")?SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED:f.coverage(),
            f.dependencyKnowledge(),"independent coverage annotation",f.astNodeId())).toList();
        var reports=new LinkedHashMap<>(a.build().coverageByProgramUnit());reports.put(unit,new SemanticCoverage.Report(changed));
        var build=new CompilationUnitBuildResult(a.model(),reports,a.build().diagnosticsByProgramUnit());
        var before=ScalarMoveSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report());
        var after=ScalarMoveSemantics.analyze(build,a.tables(),a.resolution(),a.report());
        for(var node:AstBoundaryTestSupport.nodes(a,Ast.PerformStatement.class)) {
            var x=before.procedurePerforms().fact(unit,node.meta().id()).orElseThrow();
            var y=after.procedurePerforms().fact(unit,node.meta().id()).orElseThrow();
            assertEquals(x.start(),y.start());assertEquals(x.end(),y.end());assertEquals(x.procedures(),y.procedures());assertEquals(x.resume(),y.resume());
            // Changing diagnostics on an already materialized proof cannot change structural readiness.
            var diagnosticOnly=new ProcedurePerformSemantics.Facts(x.start(),x.end(),x.procedures(),x.resume(),x.resumeOrigin(),x.loop(),x.times(),x.varying(),x.structureKnown(),List.of("UNRELATED_GAP"));
            assertEquals(x.structureKnown(),diagnosticOnly.structureKnown());
        }
        var branch=AstBoundaryTestSupport.nodes(a,Ast.IfStatement.class).get(0);
        assertEquals(before.ifs().fact(unit,branch.meta().id()).thenArm().entry(),after.ifs().fact(unit,branch.meta().id()).thenArm().entry());
        assertEquals(before.ifs().fact(unit,branch.meta().id()).elseArm().entry(),after.ifs().fact(unit,branch.meta().id()).elseArm().entry());
        var sp=PerformFamilyTest.publish(source);assertEquals(2,PerformFamilyTest.ranges(sp).size());
        for(var fact:sp.path("statements"))if(fact.path("variant").asText().equals("IF")) {
            assertEquals("KNOWN",fact.path("thenArm").path("entry").path("availability").asText());
            assertEquals("KNOWN",fact.path("elseArm").path("entry").path("availability").asText());
        }
    }
    @Test void terminalAndSpecialExitsDoNotAcquireNormalCompletion() throws Exception {
        for(var end:List.of("GOBACK.","EXIT PROGRAM.","EXIT PARAGRAPH.","EXIT SECTION.","EXIT PERFORM.")) {
            var sp=PerformFamilyTest.publish(PerformFamilyTest.source("PERFORM A THRU A.","A.\n"+end));
            var p=PerformFamilyTest.ranges(sp).get(0);
            assertTrue(p.path("procedures").get(0).path("completions").isEmpty(),end);
        }
        var sp=PerformFamilyTest.publish(PerformFamilyTest.source("GO TO LIVE.","DEAD.\nCALL 'DEAD0001'.\nLIVE.\nGOBACK."));
        var byId=new HashMap<String,JsonNode>();
        for(var fact:sp.path("statements"))byId.put(fact.path("header").path("id").asText(),fact);
        for(var fact:sp.path("statements")) {
            if(fact.path("variant").asText().equals("GOBACK"))assertEquals("NONE",fact.path("localContinuation").asText());
            if(fact.path("variant").asText().equals("GO_TO")) {
                var destination=byId.get(fact.path("targetEntry").asText());
                assertNotNull(destination);assertEquals("GOBACK",destination.path("variant").asText(),"explicit transfer reaches LIVE, never DEAD CALL");
            }
        }
    }
    @Test void knownEntrySurvivesAnUnavailableWholeRange() throws Exception {
        var sp=PerformFamilyTest.publish(PerformFamilyTest.source("PERFORM A THRU EMPTY.","A.\nCALL WS-PGM.\nEMPTY.\n"));
        var p=PerformFamilyTest.ranges(sp).get(0);
        assertTrue(p.path("start").isObject());assertTrue(p.path("end").isObject());
        assertTrue(p.path("targetEntry").isTextual());assertTrue(p.path("procedures").isEmpty());
        assertEquals("KNOWN",p.path("normalContinuation").path("availability").asText());
    }
    @Test void structuralTransportIsDeterministicAndExportsProducerFixtures() throws Exception {
        var samples=Map.of("call-basic",PerformFamilyTest.source("PERFORM A.","A.\nCALL WS-PGM.\n"),
            "nested",PerformFamilyTest.source("PERFORM A.","A.\nPERFORM B.\nB.\nMOVE 'B' TO WS-PGM.\n"),
            "neutral",PerformFamilyTest.source("PERFORM A.","A.\nCONTINUE.\n"));
        var out=java.nio.file.Path.of("target/partial-structural-facts");java.nio.file.Files.createDirectories(out);
        for(var sample:samples.entrySet()) {
            var bytes=io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(sample.getValue()));
            assertArrayEquals(bytes,io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(sample.getValue())));
            java.nio.file.Files.write(out.resolve(sample.getKey()+".json"),bytes);
            java.nio.file.Files.writeString(out.resolve(sample.getKey()+".cbl"),sample.getValue());
        }
    }
}
