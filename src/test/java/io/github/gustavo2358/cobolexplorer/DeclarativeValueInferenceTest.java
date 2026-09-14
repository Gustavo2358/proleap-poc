package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

class DeclarativeValueInferenceTest {
    static final String VALUE="01 LIT-PGM PIC X(8) VALUE 'PROGA'.\n";
    static final String GROUP="01 WS-AREA.\n05 LIT-PGM PIC X(8) VALUE 'PROGA'.\n05 TAIL-PART PIC X(8).\n";
    static String source(String data,String code) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. DVI-PROGRAM.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n"+data+"PROCEDURE DIVISION.\n"+code+"\nGOBACK.\n";
    }
    static CobolSemanticPort product(String data,String code) {
        return StorageInitialProductTest.initialSource(source(data,code),StorageInitialSemantics.EntryMode.UNKNOWN);
    }
    static StorageInitialCondition condition(CobolSemanticPort p) {
        return p.storage().entryState().conditions().get(0);
    }
    static void invariant(String data,String code) {
        var p=product(data,code);var c=condition(p);
        assertEquals(StorageEntryMode.UNKNOWN,p.storage().entryState().mode());
        assertEquals(InitialStorageKind.LITERAL_BYTES,c.kind(),c.gapCodes().toString());
        assertEquals(List.of(0xd7,0xd9,0xd6,0xc7,0xc1,0x40,0x40,0x40),c.bytes());
    }
    static void blocked(String data,String code) {
        var c=condition(product(data,code));assertEquals(InitialStorageKind.UNKNOWN,c.kind(),code);
        assertTrue(c.bytes().isEmpty());assertFalse(c.gapCodes().isEmpty());
    }
    @Test void ordinaryLiteralAndDisjointWritesKeepGloballyUnknownEntry() {
        invariant(VALUE,"CALL LIT-PGM.");
        invariant(VALUE+"01 OTHER-TEXT PIC X(8).\n","MOVE 'OTHER' TO OTHER-TEXT.\nCALL LIT-PGM.");
        invariant(GROUP,"MOVE 'OTHER' TO TAIL-PART.\nCALL LIT-PGM.");
    }
    @Test void directGroupRedefinesAndRenamesWritesBlockByPhysicalOverlap() {
        blocked(VALUE,"CALL LIT-PGM.\nMOVE 'OTHER' TO LIT-PGM.");
        blocked(GROUP,"MOVE 'OTHER' TO WS-AREA.\nCALL LIT-PGM.");
        blocked(GROUP+"01 ALIAS-AREA REDEFINES WS-AREA PIC X(16).\n","MOVE 'OTHER' TO ALIAS-AREA.\nCALL LIT-PGM.");
        blocked(GROUP+"66 ALIAS-PGM RENAMES LIT-PGM.\n","MOVE 'OTHER' TO ALIAS-PGM.\nCALL LIT-PGM.");
        blocked(GROUP+"66 ALIAS-PGM RENAMES LIT-PGM THROUGH TAIL-PART.\n","MOVE 'OTHER' TO ALIAS-PGM.\nCALL LIT-PGM.");
    }
    @Test void constantSlicesArePhysicalAndDynamicSlicesFailOpen() {
        invariant(GROUP,"MOVE 'X' TO WS-AREA(9:1).\nCALL LIT-PGM.");
        blocked(GROUP,"MOVE 'X' TO WS-AREA(8:1).\nCALL LIT-PGM.");
        blocked(GROUP+"01 POS PIC 9.\n","MOVE 'X' TO WS-AREA(POS:1).\nCALL LIT-PGM.");
        blocked(GROUP,"MOVE 'X' TO WS-AREA(17:1).\nCALL LIT-PGM.");
    }
    @Test void completeInventoryIncludesUnknownEffectsUnreachableCodeAndEscapes() {
        for(var code:List.of("ACCEPT LIT-PGM.","INITIALIZE WS-AREA.","CALL 'OTHER' USING WS-AREA.",
                "GOBACK.\nMOVE 'X' TO LIT-PGM.","MOVE FUNCTION CURRENT-DATE TO TAIL-PART.",
                "EXEC CICS READ FILE('A') INTO(WS-AREA) END-EXEC.","MOVE 'X' TO MISSING-TARGET."))blocked(GROUP,code);
        blocked(GROUP,"EXEC CICS LINK PROGRAM(LIT-PGM) COMMAREA(WS-AREA) END-EXEC.");
    }
    @Test void callsAndCicsInputOnlyTargetsDoNotExposeLocalStorage() {
        for(var command:List.of("LINK","XCTL"))invariant(VALUE,
            "EXEC CICS "+command+" PROGRAM(LIT-PGM) NOHANDLE END-EXEC.");
        invariant(VALUE+"01 TARGET-PGM PIC X(8).\n","MOVE LIT-PGM TO TARGET-PGM.\nCALL TARGET-PGM.");
    }
    @Test void initialAttributeUsesFreshEntryEvenWithLaterWritesAndPreservedIsExplicit() throws Exception {
        var s=source(VALUE,"CALL LIT-PGM.\nMOVE 'OTHER' TO LIT-PGM.").replace("DVI-PROGRAM.","DVI-PROGRAM IS INITIAL.");
        var automatic=StorageInitialProductTest.initialSource(s,StorageInitialSemantics.EntryMode.UNKNOWN);
        assertEquals(InitialStorageKind.LITERAL_BYTES,condition(automatic).kind());
        assertEquals(StorageEntryMode.UNKNOWN,automatic.storage().entryState().mode());
        assertEquals(1,automatic.moves().size(),"entry facts never synthesize a MOVE");
        var wire=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(automatic));
        assertEquals("PROGRAM_INITIAL",wire.path("storage").path("entryState").path("conditions").get(0).path("proof").asText());
        var preserved=StorageInitialProductTest.initialSource(s,StorageInitialSemantics.EntryMode.PRESERVED);
        assertEquals(InitialStorageKind.PRESERVE,condition(preserved).kind());
    }
    @Test void mixedRuntimeBranchDoesNotTurnWritableTargetIntoInvariant() {
        var p=product(VALUE+"01 TARGET-PGM PIC X(8) VALUE 'BEFORE'.\n01 INPUT-PGM PIC X(8).\n01 FLAG PIC X.\n",
            "IF FLAG = 'Y'\nMOVE LIT-PGM TO TARGET-PGM\nELSE\nMOVE INPUT-PGM TO TARGET-PGM\nEND-IF.\nCALL TARGET-PGM.");
        assertEquals(InitialStorageKind.LITERAL_BYTES,p.storage().entryState().conditions().get(0).kind());
        assertEquals(InitialStorageKind.UNKNOWN,p.storage().entryState().conditions().get(1).kind());
    }
    @Test void formattingAndRenamePreserveProofAndBlockedInputRemainsUnknown() {
        var code=source(GROUP,"MOVE 'X' TO WS-AREA(9:1).\nCALL LIT-PGM.");
        var a=StorageInitialProductTest.initialSource(code,StorageInitialSemantics.EntryMode.UNKNOWN);
        var b=StorageInitialProductTest.initialSource(code.replace("LIT-PGM","RENAMED-PROGRAM").replace("AREA","RENAMED-AREA").replace("MOVE", "move").replace("CALL", "call"),StorageInitialSemantics.EntryMode.UNKNOWN);
        assertEquals(InitialStorageKind.LITERAL_BYTES,condition(a).kind());assertEquals(condition(a).bytes(),condition(b).bytes());
        blocked(VALUE+"01 FOREIGN-AREA PIC X(8) EXTERNAL.\n","CALL LIT-PGM.");
        blocked(VALUE+"01 FOREIGN-AREA PIC X(8) GLOBAL.\n","CALL LIT-PGM.");
    }
    @Test void missingStatementCoverageCannotBecomeNoWriteProof() {
        var a=AstBoundaryTestSupport.analyze(source(VALUE,"MOVE 'X' TO LIT-PGM.\nCALL LIT-PGM."),"incomplete.cbl");
        var id=a.model().programUnits().get(0).id();
        var build=new CompilationUnitBuildResult(a.model(),Map.of(id,new SemanticCoverage.Report(List.of())),a.build().diagnosticsByProgramUnit());
        var layout=StorageLayoutSemantics.analyze(build,a.tables(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        var facts=StorageAccessSemantics.analyze(build,a.resolution(),layout).initial().facts(id);
        assertTrue(facts.conditions().stream().allMatch(c->c.kind()==StorageInitialSemantics.Kind.UNKNOWN));
    }
    @Test void removingOneBlockerCannotHideAnotherAndRemovingBothEnablesProof() {
        String alias=GROUP+"01 ALIAS-AREA REDEFINES WS-AREA PIC X(16).\n";
        String overlap="MOVE 'OTHER' TO ALIAS-AREA.\n";
        blocked(alias,"ACCEPT TAIL-PART.\n"+overlap+"CALL LIT-PGM.");
        blocked(alias,overlap+"CALL LIT-PGM.");
        invariant(alias,"CALL LIT-PGM.");
    }
    @Test void missingOnlyTheWriteFindingBlocksEvenWhenLayoutIsExact() {
        var a=AstBoundaryTestSupport.analyze(source(VALUE,"MOVE 'X' TO LIT-PGM.\nCALL LIT-PGM."),"missing-write-coverage.cbl");
        var id=a.model().programUnits().get(0).id();
        int move=AstBoundaryTestSupport.nodes(a,Ast.MoveStatement.class).get(0).meta().id();
        var findings=new ArrayList<SemanticCoverage.Finding>();
        for(var f:a.build().coverageByProgramUnit().get(id).findings())if(f.astNodeId()!=move)
            findings.add(new SemanticCoverage.Finding(findings.size(),f.grammarRule(),f.meta(),f.writtenText(),f.coverage(),f.dependencyKnowledge(),f.reason(),f.astNodeId()));
        var build=new CompilationUnitBuildResult(a.model(),Map.of(id,new SemanticCoverage.Report(findings)),a.build().diagnosticsByProgramUnit());
        var layout=StorageLayoutSemantics.analyze(build,a.tables(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        assertTrue(layout.layout(id).bases().stream().allMatch(b->b.independent()&&b.extent().value().isPresent()));
        var c=StorageAccessSemantics.analyze(build,a.resolution(),layout).initial().facts(id).conditions().get(0);
        assertEquals(StorageInitialSemantics.Kind.UNKNOWN,c.kind());
        assertTrue(c.reasons().contains(StorageInitialSemantics.Reason.INCOMPLETE_WRITE_INVENTORY));
    }
    @Test void disabledPlatformContributionCannotProveNoForeignMutation() {
        var a=AstBoundaryTestSupport.analyze(source(VALUE,"EXEC CICS LINK PROGRAM(LIT-PGM) NOHANDLE END-EXEC."),"disabled.cbl");
        var p=ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),
            StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,StorageInitialSemantics.EntryMode.UNKNOWN,CicsProgramControlAnalyzer.EntryMode.DISABLED);
        assertEquals(InitialStorageKind.UNKNOWN,condition(p).kind());
        assertTrue(condition(p).gapCodes().contains("FOREIGN_MUTATION_OR_ESCAPE"));
    }
    @Test void proofKindCannotBeForgedIndependentlyOfConditionShape() throws Exception {
        var p=product(VALUE,"CALL LIT-PGM.");var c=condition(p);
        assertEquals(InitialStorageProof.DECLARATIVE_INVARIANT,c.proof());
        assertThrows(IllegalArgumentException.class,()->new StorageInitialCondition(c.node(),c.kind(),c.bytes(),c.gapCodes(),c.provenance(),InitialStorageProof.NONE));
        var wire=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(p));
        assertEquals("2.15.0",wire.path("contractVersion").asText());
        assertEquals("1.4.0",wire.path("storage").path("version").asText());
        assertEquals("DECLARATIVE_INVARIANT",wire.path("storage").path("entryState").path("conditions").get(0).path("proof").asText());
    }
}
