package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;

class CicsAbendInvariantTest {
    static CicsAbendFact fact(String options) {
        return CicsAbendContractTest.port("EXEC CICS ABEND "+options+" END-EXEC.").statements().stream()
            .filter(CicsAbendFact.class::isInstance).map(CicsAbendFact.class::cast).findFirst().orElseThrow();
    }
    static CicsAbendFact altered(CicsAbendFact f,CicsAbendEligibility eligibility,List<CicsOption> options,List<String> gaps) {
        return new CicsAbendFact(f.header(),f.eventKind(),eligibility,f.rawText(),options,gaps);
    }
    @Test void rejectsContradictoryCancelEvidence() {
        var plain=fact("");var cancel=fact("CANCEL");
        assertThrows(IllegalArgumentException.class,()->altered(plain,CicsAbendEligibility.HANDLERS_BYPASSED,plain.options(),plain.gapCodes()));
        assertThrows(IllegalArgumentException.class,()->altered(cancel,CicsAbendEligibility.HANDLER_ELIGIBLE,cancel.options(),cancel.gapCodes()));
        assertThrows(IllegalArgumentException.class,()->altered(cancel,cancel.dispatchEligibility(),List.of(),cancel.gapCodes()));
    }
    @Test void rejectsPartialEligibilityAndMissingUnknownReason() {
        for(var options:List.of("MYSTERY","CANCEL CANCEL","ABCODE()","CANCEL(X)")) {
            var unknown=fact(options);
            assertThrows(IllegalArgumentException.class,()->altered(unknown,CicsAbendEligibility.HANDLER_ELIGIBLE,unknown.options(),unknown.gapCodes()));
            assertThrows(IllegalArgumentException.class,()->altered(unknown,CicsAbendEligibility.HANDLER_ELIGIBLE,unknown.options(),List.of()));
            assertThrows(IllegalArgumentException.class,()->altered(unknown,CicsAbendEligibility.UNAVAILABLE,unknown.options(),List.of()));
        }
        var plain=fact("");assertThrows(NullPointerException.class,()->new CicsAbendFact(plain.header(),null,plain.dispatchEligibility(),plain.rawText(),plain.options(),plain.gapCodes()));
    }
    @Test void rejectsOptionCoordinateCorruption() {
        var f=fact("NODUMP CANCEL");var reversed=new ArrayList<>(f.options());Collections.reverse(reversed);
        assertThrows(IllegalArgumentException.class,()->altered(f,f.dispatchEligibility(),reversed,f.gapCodes()));
        assertThrows(IllegalArgumentException.class,()->altered(f,f.dispatchEligibility(),List.of(new CicsOption("CANCEL",Optional.empty(),0,f.rawText().length()+1,Optional.empty())),f.gapCodes()));
    }
    @Test void truncatedSyntaxNeverQualifies() {
        for(var raw:List.of("EXEC CICS ABEND","EXEC CICS ABEND CANCEL","EXEC CICS ABEND ABCODE('T001'","EXEC CICS ABEND ABCODE('T001) END-EXEC")) {
            var event=CicsAbendSyntax.parse(raw).orElseThrow();assertEquals(CicsAbendSyntax.Eligibility.UNAVAILABLE,event.eligibility());assertFalse(event.gaps().isEmpty());
        }
    }
    @Test void projectorTranslatesPreparedEligibilityWithoutParsingDiagnosticRawText() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("","EXEC CICS ABEND END-EXEC."),"prepared.cbl");
        var unit=a.model().programUnits().get(0).id();var legacy=ScalarMoveCheckpoint4ATest.products(a);
        // Alter only diagnostic text, not the prepared event/options proof. A projector reparse would bypass.
        var pending=new ArrayDeque<Ast.Node>();pending.push(a.model().programUnits().get(0).program());
        Ast.EmbeddedLanguageStatement statement=null;
        while(!pending.isEmpty()){var node=pending.pop();if(node instanceof Ast.EmbeddedLanguageStatement e){statement=e;break;}pending.addAll(Ast.children(node));}
        assertNotNull(statement);
        var evidence=new CicsAbendSemantics.Fact("EXEC CICS ABEND CANCEL END-EXEC",CicsAbendSemantics.Eligibility.HANDLER_ELIGIBLE,List.of(),List.of());
        var prepared=new CicsAbendSemantics(a.build(),Map.of(new CicsProgramControlAnalyzer.Key(unit,statement.meta().id()),evidence));
        var cics=new CicsProgramControlAnalyzer().analyze(a.build(),a.report()).withAbendEvents(prepared);
        var p=CobolSemanticProductProjector.open(new CobolSemanticProductProjector.FrontendProducts(
            legacy.frontend(),legacy.symbolTables(),legacy.occurrencesByUnit(),legacy.resolution(),legacy.report(),legacy.scalarMoves(),legacy.storage(),Optional.of(cics)),unit);
        var event=(CicsAbendFact)p.statements().get(0);assertEquals(CicsAbendEligibility.HANDLER_ELIGIBLE,event.dispatchEligibility());assertEquals(evidence.raw(),event.rawText());
    }
    @Test void foreignContributionIsRejected() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("","EXEC CICS ABEND END-EXEC."),"a.cbl");
        var b=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("","EXEC CICS ABEND END-EXEC."),"b.cbl");
        assertThrows(IllegalArgumentException.class,()->new CicsProgramControlAnalyzer().analyze(a.build(),a.report()).withAbendEvents(CicsAbendSemantics.analyze(b.build())));
    }
    @Test void capabilityDoesNotLimitOccurrenceCardinality() throws Exception {
        for(int n:List.of(0,1,2,8)) {
            var p=CicsAbendContractTest.publish("EXEC CICS ABEND END-EXEC.\n".repeat(n)+"GOBACK.");
            assertEquals(n,CicsAbendContractTest.events(p).size());assertEquals(n==0?"2.41.0":"2.42.0",p.path("contractVersion").asText());
        }
    }
}
