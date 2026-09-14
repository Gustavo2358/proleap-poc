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
    static void notInvariant(String data,String code) {
        var c=condition(product(data,code));
        assertTrue(Set.of(InitialStorageKind.UNKNOWN,InitialStorageKind.POSSIBLE_LITERAL_BYTES).contains(c.kind()),code);
        assertNotEquals(InitialStorageProof.DECLARATIVE_INVARIANT,c.proof());assertFalse(c.gapCodes().isEmpty());
        if(c.kind()==InitialStorageKind.POSSIBLE_LITERAL_BYTES)assertEquals(List.of(215,217,214,199,193,64,64,64),c.bytes());
        else assertTrue(c.bytes().isEmpty());
    }
    @Test void ordinaryLiteralAndDisjointWritesKeepGloballyUnknownEntry() {
        invariant(VALUE,"CALL LIT-PGM.");
        invariant(VALUE+"01 OTHER-TEXT PIC X(8).\n","MOVE 'OTHER' TO OTHER-TEXT.\nCALL LIT-PGM.");
        invariant(GROUP,"MOVE 'OTHER' TO TAIL-PART.\nCALL LIT-PGM.");
    }
    @Test void directGroupRedefinesAndRenamesWritesBlockByPhysicalOverlap() {
        notInvariant(VALUE,"CALL LIT-PGM.\nMOVE 'OTHER' TO LIT-PGM.");
        notInvariant(GROUP,"MOVE 'OTHER' TO WS-AREA.\nCALL LIT-PGM.");
        notInvariant(GROUP+"01 ALIAS-AREA REDEFINES WS-AREA PIC X(16).\n","MOVE 'OTHER' TO ALIAS-AREA.\nCALL LIT-PGM.");
        notInvariant(GROUP+"66 ALIAS-PGM RENAMES LIT-PGM.\n","MOVE 'OTHER' TO ALIAS-PGM.\nCALL LIT-PGM.");
        notInvariant(GROUP+"66 ALIAS-PGM RENAMES LIT-PGM THROUGH TAIL-PART.\n","MOVE 'OTHER' TO ALIAS-PGM.\nCALL LIT-PGM.");
    }
    @Test void constantSlicesArePhysicalAndDynamicSlicesFailOpen() {
        invariant(GROUP,"MOVE 'X' TO WS-AREA(9:1).\nCALL LIT-PGM.");
        notInvariant(GROUP,"MOVE 'X' TO WS-AREA(8:1).\nCALL LIT-PGM.");
        notInvariant(GROUP+"01 POS PIC 9.\n","MOVE 'X' TO WS-AREA(POS:1).\nCALL LIT-PGM.");
        notInvariant(GROUP,"MOVE 'X' TO WS-AREA(17:1).\nCALL LIT-PGM.");
    }
    @Test void completeInventoryIncludesUnknownEffectsUnreachableCodeAndEscapes() {
        for(var code:List.of("ACCEPT LIT-PGM.","INITIALIZE WS-AREA.","CALL 'OTHER' USING WS-AREA.",
                "GOBACK.\nMOVE 'X' TO LIT-PGM.","MOVE FUNCTION CURRENT-DATE TO TAIL-PART.",
                "EXEC CICS READ FILE('A') INTO(WS-AREA) END-EXEC.","MOVE 'X' TO MISSING-TARGET."))notInvariant(GROUP,code);
        notInvariant(GROUP,"EXEC CICS LINK PROGRAM(LIT-PGM) COMMAREA(WS-AREA) END-EXEC.");
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
        assertEquals(InitialStorageKind.POSSIBLE_LITERAL_BYTES,p.storage().entryState().conditions().get(1).kind());
    }
    @Test void formattingAndRenamePreserveProofAndBlockedInputRemainsUnknown() {
        var code=source(GROUP,"MOVE 'X' TO WS-AREA(9:1).\nCALL LIT-PGM.");
        var a=StorageInitialProductTest.initialSource(code,StorageInitialSemantics.EntryMode.UNKNOWN);
        var b=StorageInitialProductTest.initialSource(code.replace("LIT-PGM","RENAMED-PROGRAM").replace("AREA","RENAMED-AREA").replace("MOVE", "move").replace("CALL", "call"),StorageInitialSemantics.EntryMode.UNKNOWN);
        assertEquals(InitialStorageKind.LITERAL_BYTES,condition(a).kind());assertEquals(condition(a).bytes(),condition(b).bytes());
        notInvariant(VALUE+"01 FOREIGN-AREA PIC X(8) EXTERNAL.\n","CALL LIT-PGM.");
        notInvariant(VALUE+"01 FOREIGN-AREA PIC X(8) GLOBAL.\n","CALL LIT-PGM.");
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
        notInvariant(alias,"ACCEPT TAIL-PART.\n"+overlap+"CALL LIT-PGM.");
        notInvariant(alias,overlap+"CALL LIT-PGM.");
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
        assertEquals(StorageInitialSemantics.Kind.POSSIBLE_LITERAL_BYTES,c.kind());
        assertTrue(c.reasons().contains(StorageInitialSemantics.Reason.INCOMPLETE_WRITE_INVENTORY));
    }
    @Test void disabledPlatformContributionCannotProveNoForeignMutation() {
        var a=AstBoundaryTestSupport.analyze(source(VALUE,"EXEC CICS LINK PROGRAM(LIT-PGM) NOHANDLE END-EXEC."),"disabled.cbl");
        var p=ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),
            StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,StorageInitialSemantics.EntryMode.UNKNOWN,CicsProgramControlAnalyzer.EntryMode.DISABLED);
        assertEquals(InitialStorageKind.POSSIBLE_LITERAL_BYTES,condition(p).kind());
        assertTrue(condition(p).gapCodes().contains("FOREIGN_MUTATION_OR_ESCAPE"));
    }
    @Test void proofKindCannotBeForgedIndependentlyOfConditionShape() throws Exception {
        var p=product(VALUE,"CALL LIT-PGM.");var c=condition(p);
        assertEquals(InitialStorageProof.DECLARATIVE_INVARIANT,c.proof());
        assertThrows(IllegalArgumentException.class,()->new StorageInitialCondition(c.node(),c.kind(),c.bytes(),c.gapCodes(),c.provenance(),InitialStorageProof.NONE));
        var wire=new ObjectMapper().readTree(SemanticProductJsonWriter.serialize(p));
        assertEquals("2.17.0",wire.path("contractVersion").asText());
        assertEquals("1.5.0",wire.path("storage").path("version").asText());
        assertEquals("DECLARATIVE_INVARIANT",wire.path("storage").path("entryState").path("conditions").get(0).path("proof").asText());
    }

    @Test void e02e08ReferenceArgumentsExposeOnlyTheirPhysicalRegion() {
        for(var mode:List.of("", "BY REFERENCE "))
            invariant(VALUE+"01 ARG-AREA PIC X(20).\n","CALL LIT-PGM USING "+mode+"ARG-AREA.");
    }
    @Test void e03e06OverlappingReferenceArgumentsBlockWithForeignReason() {
        for(var pair:List.of(
                new String[]{VALUE,"LIT-PGM"},
                new String[]{GROUP,"WS-AREA"},
                new String[]{GROUP+"01 ALIAS-AREA REDEFINES WS-AREA PIC X(16).\n","ALIAS-AREA"},
                new String[]{GROUP+"66 ALIAS-PGM RENAMES LIT-PGM.\n","ALIAS-PGM"},
                new String[]{GROUP+"66 ALIAS-PGM RENAMES LIT-PGM THROUGH TAIL-PART.\n","ALIAS-PGM"})) {
            var c=condition(product(pair[0],"CALL LIT-PGM USING BY REFERENCE "+pair[1]+"."));
            assertEquals(InitialStorageKind.POSSIBLE_LITERAL_BYTES,c.kind());
            assertEquals(InitialStorageProof.DECLARATIVE_POSSIBILITY,c.proof());
            assertTrue(c.gapCodes().contains("FOREIGN_MUTATION_OR_ESCAPE"),c.gapCodes().toString());
        }
    }
    @Test void e07e09e10PhysicalSlicesWithinSameBaseDecideOverlap() {
        invariant(GROUP,"CALL LIT-PGM USING TAIL-PART.");
        invariant(GROUP,"CALL LIT-PGM USING WS-AREA(9:8).");
        notInvariant(GROUP,"CALL LIT-PGM USING WS-AREA(8:8).");
        notInvariant(GROUP,"CALL LIT-PGM USING WS-AREA(1:1).");
        invariant(GROUP+"66 ALIAS-TAIL RENAMES TAIL-PART.\n","CALL LIT-PGM USING ALIAS-TAIL.");
    }
    @Test void e11e13UnknownExposureNeverMeansNoMutation() {
        for(var arg:List.of("WS-AREA(POS:1)","WS-AREA(17:1)","MISSING-AREA",
                "BY REFERENCE ADDRESS OF WS-AREA","BY CONTENT ADDRESS OF WS-AREA",
                "BY VALUE ADDRESS OF WS-AREA","BY REFERENCE OMITTED",
                "BY CONTENT TAIL-PART","BY VALUE POS","BY REFERENCE 'X'")) {
            var c=condition(product(GROUP+"01 POS PIC 9.\n","CALL LIT-PGM USING "+arg+"."));
            assertEquals(InitialStorageKind.POSSIBLE_LITERAL_BYTES,c.kind(),arg);
            assertTrue(c.gapCodes().contains("FOREIGN_MUTATION_OR_ESCAPE"),arg+": "+c.gapCodes());
        }
    }
    @Test void e14AllCallsAndCandidatesUsePhysicalExposureNotStatementFamily() {
        invariant(VALUE+"01 ARG-AREA PIC X(20).\n","CALL LIT-PGM.\nCALL 'OTHER' USING ARG-AREA.");
        var p=product(VALUE+"01 OTHER-PGM PIC X(8) VALUE 'PROGB'.\n01 ARG-AREA PIC X(20).\n",
            "CALL LIT-PGM USING ARG-AREA.\nCALL OTHER-PGM USING LIT-PGM.");
        assertEquals(InitialStorageKind.POSSIBLE_LITERAL_BYTES,p.storage().entryState().conditions().get(0).kind());
        assertEquals(InitialStorageProof.DECLARATIVE_INVARIANT,p.storage().entryState().conditions().get(1).proof());
        notInvariant(GROUP,"CALL LIT-PGM USING TAIL-PART.\nGOBACK.\nCALL 'OTHER' USING LIT-PGM.");
        notInvariant(GROUP,"CALL LIT-PGM USING TAIL-PART\nON EXCEPTION CALL 'OTHER' USING LIT-PGM\nEND-CALL.");
    }
    @Test void e15e16DisjointExposureCannotHideIndependentWritesOrForeignEffects() {
        for(var effect:List.of("MOVE 'OTHER' TO LIT-PGM.","ACCEPT TAIL-PART.",
                "EXEC CICS READ FILE('A') INTO(TAIL-PART) END-EXEC.",
                "MOVE FUNCTION CURRENT-DATE TO TAIL-PART."))
            notInvariant(GROUP,"CALL LIT-PGM USING TAIL-PART.\n"+effect);
        var c=condition(product(GROUP,"CALL LIT-PGM USING LIT-PGM.\nMOVE 'OTHER' TO LIT-PGM."));
        assertTrue(c.gapCodes().contains("OVERLAPPING_WRITE"));
        assertTrue(c.gapCodes().contains("FOREIGN_MUTATION_OR_ESCAPE"));
    }
    @Test void e17DisjointExposurePreservesKnownProducerAndRuntimeAlternative() {
        var p=product(VALUE+"01 ARG-AREA PIC X(20).\n01 TARGET-PGM PIC X(8) VALUE 'BEFORE'.\n01 INPUT-PGM PIC X(8).\n01 FLAG PIC X.\n",
            "IF FLAG = 'Y'\nMOVE LIT-PGM TO TARGET-PGM\nELSE\nMOVE INPUT-PGM TO TARGET-PGM\nEND-IF.\nCALL TARGET-PGM USING ARG-AREA.");
        assertEquals(InitialStorageProof.DECLARATIVE_INVARIANT,p.storage().entryState().conditions().get(0).proof());
        assertEquals(InitialStorageKind.POSSIBLE_LITERAL_BYTES,p.storage().entryState().conditions().get(1).kind());
    }
    @Test void typedArgumentModesIncludeDefaultAndPhraseInheritance() {
        var a=AstBoundaryTestSupport.analyze(source(GROUP,"CALL LIT-PGM USING TAIL-PART LIT-PGM\nBY CONTENT TAIL-PART LIT-PGM\nBY VALUE 1 2\nBY REFERENCE TAIL-PART LIT-PGM."),"modes.cbl");
        var call=AstBoundaryTestSupport.nodes(a,Ast.CallStatement.class).get(0);
        assertEquals(List.of(Ast.PassingMode.REFERENCE,Ast.PassingMode.REFERENCE,
            Ast.PassingMode.CONTENT,Ast.PassingMode.CONTENT,Ast.PassingMode.VALUE,Ast.PassingMode.VALUE,
            Ast.PassingMode.REFERENCE,Ast.PassingMode.REFERENCE),call.arguments().stream().map(Ast.CallArgument::passingMode).toList());
        assertTrue(call.arguments().stream().allMatch(arg->arg.argumentKind()==Ast.CallArgumentKind.VALUE));
    }
    @Test void missingCanonicalArgumentAccessCannotCertifyDisjunction() {
        var f=StorageAccessTest.fixture(GROUP,"CALL LIT-PGM USING WS-AREA(9:8).");
        var a=f.source();var unit=a.model().programUnits().get(0);
        var condition=f.effects().initial().facts(unit.id()).conditions().get(0);
        assertEquals(StorageInitialSemantics.Proof.DECLARATIVE_INVARIANT,condition.proof());
        var argument=f.effects().accesses().stream().filter(x->x.role()==StorageAccessSemantics.Role.CALL_ARGUMENT).findFirst().orElseThrow();
        assertEquals(condition.view().orElseThrow().base(),argument.view().base());
        assertEquals(java.math.BigInteger.valueOf(8),argument.view().offset().value().orElseThrow());
        assertEquals(java.math.BigInteger.valueOf(8),argument.view().extent().value().orElseThrow());
        var retained=new HashMap<StorageLayoutSemantics.Key,StorageAccessSemantics.Access>();
        for(var access:f.effects().accesses())if(access!=argument)retained.put(access.reference(),access);
        var inventory=StorageMutationInventory.analyze(a.build(),unit,f.effects().layout().layout(unit.id()),retained,Map.of(),
            new CicsProgramControlAnalyzer().analyze(a.build()));
        assertTrue(inventory.blockers(condition.view().orElseThrow()).contains(StorageInitialSemantics.Reason.FOREIGN_MUTATION_OR_ESCAPE));
    }
    @Test void everyArgumentMustProveDisjunctionRegardlessOfOrderOrOtherModes() {
        invariant(GROUP,"CALL LIT-PGM USING TAIL-PART WS-AREA(9:4) WS-AREA(13:4).");
        for(var args:List.of("TAIL-PART LIT-PGM","LIT-PGM TAIL-PART","TAIL-PART MISSING-AREA",
                "MISSING-AREA TAIL-PART","BY CONTENT TAIL-PART BY REFERENCE TAIL-PART",
                "BY REFERENCE TAIL-PART BY VALUE 1"))notInvariant(GROUP,"CALL LIT-PGM USING "+args+".");
        notInvariant(VALUE+"01 TABLE-AREA.\n05 ITEM-AREA PIC X(8) OCCURS 2 TIMES.\n01 IDX PIC 9.\n",
            "CALL LIT-PGM USING ITEM-AREA(IDX).");
        notInvariant(VALUE+"01 ARG-AREA PIC X(8) EXTERNAL.\n","CALL LIT-PGM USING ARG-AREA.");
        notInvariant(GROUP,"CALL LIT-PGM USING TAIL-PART RETURNING LIT-PGM.");
    }
}
