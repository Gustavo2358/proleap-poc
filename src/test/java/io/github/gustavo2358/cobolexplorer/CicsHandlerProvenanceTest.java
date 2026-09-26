package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import static org.junit.jupiter.api.Assertions.*;

/** R7-R1: operands own their source; registration syntax is never an execution edge. */
class CicsHandlerProvenanceTest {
    static String source(String selector) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. HANDLER.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n01 PGM PIC X(8).\nPROCEDURE DIVISION.\nEXEC CICS HANDLE ABEND\n    "+selector+"\nEND-EXEC.\nGOBACK.\nERR-HANDLER.\nGOBACK.\n";
    }
    static CobolSemanticPort product(String source) {
        return EofUnitBoundaryTest.publish(AstBoundaryTestSupport.analyze(source,"origin.cbl"),0,StorageLayoutSemantics.Profile.UNSPECIFIED);
    }
    static CicsHandlerFact handler(String selector) {return CicsHandlerContractTest.handlers(product(source(selector))).get(0);}
    static void operand(CicsHandlerFact h,String text,int column) {
        var p=h.targetOrigin().orElseThrow();
        assertEquals(new Location("origin.cbl",8,column,8,column+text.codePointCount(0,text.length())-1),p.original());
        assertEquals("<preprocessed>",p.expanded().file());
        assertEquals(7,p.expanded().startLine());assertEquals(7,p.expanded().endLine());
        // Normalized/framed line: *>EXECCICS EXEC CICS HANDLE ABEND followed by the retained operand.
        int expandedColumn=44+(column-17);
        assertEquals(expandedColumn,p.expanded().startColumn());
        assertEquals(expandedColumn+text.codePointCount(0,text.length())-1,p.expanded().endColumn());
        assertEquals(List.of(),p.includeChain());assertFalse(p.exact());
        assertNotEquals(h.header().provenance(),p);
    }
    @Test void labelUsesOperandCoordinatesAndCanonicalProcedureReference() {
        var h=handler("LABEL(ERR-HANDLER)");operand(h,"ERR-HANDLER",17);
        assertNotEquals(h.labelTarget().orElseThrow().declarationOrigin(),h.targetOrigin().orElseThrow());
        var a=AstBoundaryTestSupport.analyze(source("LABEL(ERR-HANDLER)"),"origin.cbl");
        var reference=AstBoundaryTestSupport.nodes(a,Ast.ProcedureReference.class).get(0).meta().provenance();
        assertEquals(reference.original().startColumn(),h.targetOrigin().orElseThrow().original().startColumn());
        assertEquals(reference.expanded().startColumn(),h.targetOrigin().orElseThrow().expanded().startColumn());
    }
    @Test void programLiteralUsesOperandAndIdenticalTargetProvenance() {
        var h=handler("PROGRAM('ExitPgm')");operand(h,"'ExitPgm'",19);
        var target=(LiteralCallTarget)h.programTarget().orElseThrow();
        assertEquals("ExitPgm",target.text());assertEquals(h.targetOrigin().orElseThrow(),target.provenance());
    }
    @Test void programDataUsesCanonicalReferenceProvenance() {
        var h=handler("PROGRAM(PGM)");operand(h,"PGM",19);
        assertEquals(h.targetOrigin().orElseThrow(),((DataReference)h.programTarget().orElseThrow()).provenance());
    }
    static void noTarget(CicsHandlerFact h) {
        assertTrue(h.targetSyntax().isEmpty());assertTrue(h.targetOrigin().isEmpty());
        assertTrue(h.programTarget().isEmpty());assertTrue(h.labelBindingStatus().isEmpty());
        assertTrue(h.labelTarget().isEmpty());assertTrue(h.targetEntry().isEmpty());assertTrue(h.entryOrigin().isEmpty());
    }
    @Test void cancelHasNoTargetOrigin() {var h=handler("CANCEL");assertEquals(CicsHandlerAction.CANCEL,h.action());assertEquals(CicsHandlerTargetKind.NONE,h.targetKind());noTarget(h);}
    @Test void defaultCancelHasNoTargetOrigin() {var h=handler("");assertEquals(CicsHandlerAction.CANCEL,h.action());assertEquals(CicsHandlerTargetKind.NONE,h.targetKind());noTarget(h);}
    @Test void resetHasNoTargetOrigin() {var h=handler("RESET");assertEquals(CicsHandlerAction.RESET,h.action());assertEquals(CicsHandlerTargetKind.NONE,h.targetKind());noTarget(h);}
    @Test void unavailableNeverInventsTargetOrigin() {
        for(var invalid:List.of("LABEL(ERR-HANDLER) CANCEL","CANCEL RESET","LABEL(ERR-HANDLER) LABEL(ERR-HANDLER)","LABEL()","CANCEL(X)","MYSTERY")) {
            var h=handler(invalid);assertEquals(CicsHandlerAction.UNAVAILABLE,h.action(),invalid);assertEquals(CicsHandlerTargetKind.UNAVAILABLE,h.targetKind());noTarget(h);
        }
        var operation=CicsHandlerSyntax.parse("EXEC CICS HANDLE ABEND").orElseThrow();
        assertEquals(CicsHandlerSyntax.Action.UNAVAILABLE,operation.action());
        assertTrue(CicsHandlerSyntax.targetOperand(operation.raw()).isEmpty());
    }
    static List<CicsHandlerFact> copied(String name,String text,int column) {
        var hs=CicsHandlerContractTest.handlers(product(source("CANCEL").replace("EXEC CICS HANDLE ABEND\n    CANCEL\nEND-EXEC.","COPY "+name+".\nCOPY "+name+".")));
        assertEquals(2,hs.size());assertNotEquals(hs.get(0).header().id(),hs.get(1).header().id());
        assertNotEquals(hs.get(0).targetOrigin(),hs.get(1).targetOrigin());
        String file=name+".cpy";
        for(int i=0;i<2;i++) {
            var p=hs.get(i).targetOrigin().orElseThrow();
            assertEquals(new Location(file,2,column,2,column+text.length()-1),p.original());
            assertEquals(List.of(new IncludeFrame("origin.cbl",name,file,7+i)),p.includeChain());
            assertEquals("<preprocessed>",p.expanded().file());assertFalse(p.exact());
            hs.get(i).programTarget().ifPresent(t->assertEquals(p,t.provenance()));
        }
        return hs;
    }
    @Test void copyLabelRetainsOriginalFileChainAndOccurrence() {copied("R7LABEL","ERR-HANDLER",17);}
    @Test void copyProgramLiteralRetainsOriginalFileChainAndOccurrence() {copied("R7LITERAL","'ExitPgm'",19);}
    @Test void copyProgramDataRetainsOriginalFileChainAndOccurrence() {copied("R7DATA","PGM",19);}
    @Test void duplicateProgramTextInDifferentUnitsKeepsOwnedIdentity() {
        String body=source("PROGRAM(PGM)");String two=body+"END PROGRAM HANDLER.\n"+body.replace("PROGRAM-ID. HANDLER.","PROGRAM-ID. SECOND.")+"END PROGRAM SECOND.\n";
        var a=AstBoundaryTestSupport.analyze(two,"origin.cbl");assertEquals(2,a.model().programUnits().size());
        var first=CicsHandlerContractTest.handlers(EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED)).get(0);
        var second=CicsHandlerContractTest.handlers(EofUnitBoundaryTest.publish(a,1,StorageLayoutSemantics.Profile.UNSPECIFIED)).get(0);
        assertNotEquals(first.header().id(),second.header().id());assertNotEquals(first.targetOrigin(),second.targetOrigin());
        assertNotEquals(first.programTarget().orElseThrow().id(),second.programTarget().orElseThrow().id());
    }
    static CicsHandlerFact changed(CicsHandlerFact h,Optional<Provenance> origin,Optional<CallTarget> program) {
        return new CicsHandlerFact(h.header(),h.handlerKind(),h.action(),h.targetKind(),h.targetSyntax(),h.labelBindingStatus(),h.labelTarget(),h.targetEntry(),h.entryOrigin(),origin,program,h.scope(),h.rawText(),h.options(),h.gapCodes());
    }
    @Test void constructorRejectsMissingOriginAndOriginWithoutTarget() {
        for(var selector:List.of("LABEL(ERR-HANDLER)","PROGRAM('ExitPgm')","PROGRAM(PGM)","PROGRAM(ABSENT)")) {
            var h=handler(selector);assertThrows(IllegalArgumentException.class,()->changed(h,Optional.empty(),h.programTarget()));
        }
        for(var selector:List.of("CANCEL","RESET","MYSTERY")) {
            var h=handler(selector);assertThrows(IllegalArgumentException.class,()->changed(h,Optional.of(h.header().provenance()),h.programTarget()));
        }
    }
    @Test void constructorRejectsProgramTargetOriginMismatch() {
        for(var selector:List.of("PROGRAM('ExitPgm')","PROGRAM(PGM)")) {
            var h=handler(selector);assertThrows(IllegalArgumentException.class,()->changed(h,Optional.of(h.header().provenance()),h.programTarget()));
        }
    }
    @Test void incompleteProgramBindingStillHasOperandOrigin() {
        for(var syntax:List.of("ABSENT","FUNCTION UNKNOWN(PGM)")) {
            var h=handler("PROGRAM("+syntax+")");operand(h,syntax,19);
            assertEquals(CicsHandlerAction.ACTIVATE,h.action());assertEquals(CicsHandlerTargetKind.PROGRAM,h.targetKind());
            h.programTarget().ifPresent(t->assertEquals(h.targetOrigin().orElseThrow(),t.provenance()));
        }
    }
    @Test void fixturesReplayByteForByteIncludingUnknownBindings() throws Exception {
        for(var selector:List.of("LABEL(ERR-HANDLER)","PROGRAM('ExitPgm')","PROGRAM(PGM)","CANCEL","RESET","LABEL(ABSENT)","PROGRAM(ABSENT)"))
            assertArrayEquals(SemanticProductJsonWriter.serialize(product(source(selector))),SemanticProductJsonWriter.serialize(product(source(selector))));
        for(var name:List.of("R7LABEL","R7LITERAL","R7DATA")) {
            var s=source("CANCEL").replace("EXEC CICS HANDLE ABEND\n    CANCEL\nEND-EXEC.","COPY "+name+".");
            assertArrayEquals(SemanticProductJsonWriter.serialize(product(s)),SemanticProductJsonWriter.serialize(product(s)));
        }
    }
    @Test void refinementDoesNotChangeStatementProvenanceOrTopology() throws Exception {
        new CicsHandlerContractTest().typedOperationDoesNotAddDispatchOrContinuation();
        var p=product(source("PROGRAM('ExitPgm')"));var h=CicsHandlerContractTest.handlers(p).get(0);
        assertEquals(new Location("origin.cbl",7,7,9,15),h.header().provenance().original());
        var t=p.controlTopology().orElseThrow();
        assertTrue(t.outcomes().stream().filter(o->o.statement().equals("statement:"+h.header().id().localId())).allMatch(o->o.kind()==ControlTopology.OutcomeKind.UNKNOWN_LOCAL));
    }
    @Test void retainedMappingHandlesCrLfAndUnicodeWithoutPromotingExactness() {
        var map=SourceMap.identity("EXEC\r\nPROGRAM('😀') END-EXEC","unicode.cbl");
        var framed=map.framedEmbeddedSlice(0,map.length(),map.length(),"*>EXECCICS ","\n");
        var p=framed.embeddedOperandProvenance(24,27);
        assertEquals(new Ast.SourceLocation("unicode.cbl",2,8,2,10),p.original());
        assertEquals(new Ast.SourceLocation("<preprocessed>",1,24,1,26),p.expanded());assertFalse(p.exact());
    }
}
