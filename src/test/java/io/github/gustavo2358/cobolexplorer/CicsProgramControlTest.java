package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CicsProgramControlTest {
    @Test void dedicatedParserKeepsOptionsOffsetsEscapesAndCommand() {
        var parser=new CicsProgramControlAnalyzer();
        for(String command:List.of("LINK","XCTL"))for(String quote:List.of("'","\"")) {
            String raw="*>EXECCICS EXEC CICS "+command.toLowerCase()+" NOHANDLE PROGRAM("+quote+"aB"+quote+quote+"C"+quote+") COMMAREA(AREA(1:8)) RESP(RC) END-EXEC";
            var fact=parser.parse(raw).orElseThrow();assertEquals(command,fact.command().name());assertEquals("aB"+quote+"C",fact.literal().orElseThrow());
            assertEquals(List.of("NOHANDLE","PROGRAM","COMMAREA","RESP"),fact.options().stream().map(CicsProgramControlAnalyzer.Option::name).toList());
            assertTrue(fact.gaps().isEmpty(),fact.gaps().toString());assertEquals(raw,fact.raw());assertTrue(raw.substring(fact.targetStart(),fact.targetEnd()).startsWith("PROGRAM("));
        }
    }
    @Test void conflictTruncationAndUnknownOptionsRemainExplicit() {
        var parser=new CicsProgramControlAnalyzer();
        var duplicate=parser.parse("EXEC CICS LINK PROGRAM('A') PROGRAM('B') END-EXEC").orElseThrow();
        assertTrue(duplicate.literal().isEmpty());assertTrue(duplicate.gaps().contains("CICS_PROGRAM_DUPLICATED"));
        var partial=parser.parse("EXEC CICS XCTL PROGRAM('A') MYSTERY(X) END-EXEC").orElseThrow();assertEquals("A",partial.literal().orElseThrow());assertFalse(partial.gaps().isEmpty());assertEquals(2,partial.options().size());
        var truncated=parser.parse("EXEC CICS LINK PROGRAM('A' END-EXEC").orElseThrow();assertTrue(truncated.literal().isEmpty());assertFalse(truncated.gaps().isEmpty());
    }
    @Test void realPreprocessorPublishesEachCommandWithPreservedPayload() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("01 WS-AREA PIC X(8).",
            "EXEC CICS LINK PROGRAM('PROGA') END-EXEC\nEXEC CICS XCTL PROGRAM(WS-AREA) NOHANDLE END-EXEC\nGOBACK."),"cics.cbl");
        var nodes=AstBoundaryTestSupport.nodes(a,Ast.EmbeddedLanguageStatement.class);assertEquals(2,nodes.size());
        var host=nodes.get(1).hostOperands().get(0).reference();
        assertEquals(nodes.get(1).meta().origin(),host.meta().origin());assertTrue(host.meta().origin().rootNodeId()>=0);
        assertFalse(host.meta().provenance().exact(),"embedded syntax must not invent exact physical provenance");
        var snapshot=new CicsProgramControlAnalyzer().analyze(a.build());var unit=a.model().programUnits().get(0).id();
        assertEquals("PROGA",snapshot.fact(unit,nodes.get(0).meta().id()).orElseThrow().literal().orElseThrow());
        assertEquals("WS-AREA",snapshot.fact(unit,nodes.get(1).meta().id()).orElseThrow().host().orElseThrow());
    }
    @Test void typedSnapshotPublishesLiteralWithExplicitPartialSignatureAndDisabledFallback() throws Exception {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("01 WS-AREA PIC X(8).",
            "EXEC CICS LINK PROGRAM('aBc') COMMAREA(WS-AREA) END-EXEC.\nGOBACK."),"cics.cbl");
        var source=ScalarMoveCheckpoint4ATest.products(a);var unit=a.model().programUnits().get(0).id();
        var enriched=new io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.FrontendProducts(
            source.frontend(),source.symbolTables(),source.occurrencesByUnit(),source.resolution(),source.report(),source.scalarMoves(),source.storage(),
            Optional.of(new CicsProgramControlAnalyzer().analyze(a.build(),a.report())));
        var state=io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.project(enriched,unit);
        var fact=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)state.statements().get(0);
        assertEquals("LINK",fact.command().name());assertEquals(2,fact.options().size());assertFalse(fact.gapCodes().isEmpty());
        assertEquals("aBc",((io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.LiteralCallTarget)fact.target().orElseThrow()).text());
        var bytes=io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(
            io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state));
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(bytes);
        assertEquals("2.25.0",json.path("contractVersion").asText());assertEquals("CICS_PROGRAM_CONTROL",json.path("statements").get(0).path("variant").asText());
        var disabled=io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.project(source,unit);
        assertTrue(disabled.statements().get(0) instanceof io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.ObservedStatement);
        if(System.getProperty("cics.fixture.output")!=null)java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("cics.fixture.output")),bytes);
    }

    static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.State regional(String data,String code) {
        return regional(data,code,CicsProgramControlAnalyzer.EntryMode.UNKNOWN);
    }
    static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.State regional(String data,String code,CicsProgramControlAnalyzer.EntryMode mode) {
        var a=StorageAccessTest.fixture(data,code.replace(" COMMAREA(","\nCOMMAREA(").replace(" NOHANDLE","\nNOHANDLE").replace(" ELSE ","\nELSE ").replace(" END-IF","\nEND-IF"));var f=a.source();
        var cics=new CicsProgramControlAnalyzer().analyze(f.build(),f.report(),mode);
        return io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.project(new io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.FrontendProducts(
            f.build(),f.tables(),f.occurrences(),f.resolution(),f.report(),ScalarMoveSemantics.analyze(f.build(),f.tables(),f.resolution(),f.report(),StorageComponents.analyze(f.build()),Optional.of(a.effects()),cics),Optional.of(a.effects()),Optional.of(cics)),f.model().programUnits().get(0).id());
    }
    @Test void canonicalHostBindingKeepsQualificationAliasesAndEightByteViews() throws Exception {
        var s=regional("01 FIRST-AREA.\n05 WS-PGM PIC X(8).\n01 SECOND-AREA.\n05 WS-PGM PIC X(8).",
            "MOVE 'PROGA' TO WS-PGM OF FIRST-AREA.\nEXEC CICS LINK PROGRAM(WS-PGM OF FIRST-AREA) COMMAREA(FIRST-AREA) NOHANDLE END-EXEC.");
        var fact=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)s.statements().get(1);
        var reference=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.DataReference)fact.target().orElseThrow();
        assertEquals("KNOWN",fact.localContinuation().availability().name());
        assertTrue(reference.regionalAccess().isPresent());assertTrue(reference.binding().selected().isPresent());
        var move=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.MoveFact)s.statements().get(0);
        assertEquals(move.target().binding().selected(),reference.binding().selected());
        assertEquals(move.target().regionalAccess(),reference.regionalAccess());
        assertTrue(fact.options().stream().filter(o->o.name().equals("COMMAREA")).findFirst().orElseThrow().reference().isPresent());
        emit("qualified",s);
        var group=regional("01 WS-GROUP.\n05 P1 PIC X(4).\n05 P2 PIC X(4).","MOVE 'PROGA' TO WS-GROUP.\nEXEC CICS LINK PROGRAM(WS-GROUP) NOHANDLE END-EXEC.");
        var groupTarget=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.DataReference)((io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)group.statements().get(1)).target().orElseThrow();
        assertEquals("READ",groupTarget.role().name());assertTrue(groupTarget.regionalAccess().isPresent());emit("group",group);
        emit("variable",regional("01 WS-PGM PIC X(8).","MOVE 'PROGA' TO WS-PGM.\nEXEC CICS LINK PROGRAM(WS-PGM) COMMAREA(WS-PGM) NOHANDLE END-EXEC.\nCALL WS-PGM."));
        emit("short",regional("01 WS-PGM PIC X(5).","MOVE 'PROGA' TO WS-PGM.\nEXEC CICS LINK PROGRAM(WS-PGM) END-EXEC."));
        emit("if",regional("01 FLAG PIC X.\n01 WS-PGM PIC X(8).","IF FLAG = 'Y' MOVE 'PROGA' TO WS-PGM ELSE MOVE 'PROGB' TO WS-PGM END-IF.\nEXEC CICS LINK PROGRAM(WS-PGM) NOHANDLE END-EXEC."));
        emit("overlay",regional("01 WS-GROUP.\n05 PREFIX-PART PIC X(4).\n05 SUFFIX-PART PIC X(4).\n01 WS-ALIAS REDEFINES WS-GROUP PIC X(8).","MOVE 'XXXXXXXX' TO WS-GROUP.\nMOVE 'PROG' TO PREFIX-PART.\nMOVE 'A   ' TO SUFFIX-PART.\nEXEC CICS LINK PROGRAM(WS-ALIAS) NOHANDLE END-EXEC."));
        emit("slice",regional("01 WS-GROUP PIC X(10).","MOVE 'XXPROGA   ' TO WS-GROUP.\nEXEC CICS LINK PROGRAM(WS-GROUP(3:8)) NOHANDLE END-EXEC."));
        emit("dynamic",regional("01 WS-PGM PIC X(10).\n01 IDX PIC 9.","MOVE 'PROGA' TO WS-PGM.\nEXEC CICS LINK PROGRAM(WS-PGM(IDX:8)) END-EXEC."));
    }
    static void emit(String name,io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.State state) throws Exception {
        if(System.getProperty("cics.fixtures.dir")==null)return;
        var path=java.nio.file.Path.of(System.getProperty("cics.fixtures.dir"));java.nio.file.Files.createDirectories(path);
        java.nio.file.Files.write(path.resolve(name+".sp.json"),io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state)));
    }

    @Test void defaultEntryPrefixAndLocalErrorsStayDistinct() throws Exception {
        for(String option:List.of("","NOHANDLE","RESP(RC)","RESP2(RC)")) {
            var state=regional("01 RC PIC 9.","EXEC CICS XCTL PROGRAM('PROGA')\n"+option+" END-EXEC.\nCALL 'AFTER'.",CicsProgramControlAnalyzer.EntryMode.NEW_LOGICAL_LEVEL);
            var fact=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)state.statements().get(0);
            String expected=option.isEmpty()?"DEFAULT_ENTRY_PREFIX":option.startsWith("RESP2")?"UNKNOWN":"LOCAL_CONDITION";
            assertEquals(expected,fact.conditions().name());emit(option.isEmpty()?"xctl-default":option.startsWith("RESP2")?"xctl-resp2":option.startsWith("RESP(")?"xctl-resp":"xctl-nohandle",state);
        }
        var noPremise=regional("01 RC PIC 9.","EXEC CICS XCTL PROGRAM('PROGA') END-EXEC.\nCALL 'AFTER'.");
        assertEquals("UNKNOWN",((io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)noPremise.statements().get(0)).conditions().name());emit("xctl-entry-unknown",noPremise);
        var unknown=regional("01 RC PIC 9.","EXEC CICS HANDLE CONDITION ERROR(ERR-PARA) END-EXEC.\nEXEC CICS XCTL PROGRAM('PROGA') END-EXEC.\nCALL 'AFTER'.");
        assertEquals("UNKNOWN",((io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)unknown.statements().get(1)).conditions().name());emit("xctl-handler",unknown);
    }

    @Test void boundedCicsInsidePerformedParagraphKeepsActivationBoundary() throws Exception {
        for(String command:List.of("LINK","XCTL")) {
            var source=PerformFamilyTest.source("PERFORM A THRU A.\nCALL 'AFTER'.\n","A.\nEXEC CICS "+command+" PROGRAM('PROGA')\nNOHANDLE END-EXEC.\nMOVE 'PROGB' TO WS-PGM.\n");
            var a=AstBoundaryTestSupport.analyze(source,"cics-perform.cbl");
            var layout=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
            var storage=StorageAccessSemantics.analyze(a.build(),a.resolution(),layout);
            var cics=new CicsProgramControlAnalyzer().analyze(a.build(),a.report());
            var state=io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.project(new io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.FrontendProducts(a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),ScalarMoveSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),StorageComponents.analyze(a.build()),Optional.of(storage),cics),Optional.of(storage),Optional.of(cics)),a.model().programUnits().get(0).id());
            var perform=state.statements().stream().filter(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.ProcedurePerformFact.class::isInstance).map(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.ProcedurePerformFact.class::cast).findFirst().orElseThrow();
            assertTrue(perform.gapCodes().isEmpty(),perform.gapCodes().toString());emit("perform-"+command.toLowerCase(Locale.ROOT),state);
        }
    }

    @Test void ordinaryLinkCrossesParagraphAndTypedConditionsRejectContradictions() {
        var state=regional("01 WS-PGM PIC X(8).","MAIN-PARA.\nEXEC CICS LINK PROGRAM('PROGA') END-EXEC.\nAFTER-PARA.\nCALL 'AFTER'.",CicsProgramControlAnalyzer.EntryMode.NEW_LOGICAL_LEVEL);
        var c=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)state.statements().get(0);
        assertEquals("UNAVAILABLE",c.localContinuation().availability().name());
        assertEquals(Optional.of(state.statements().get(1).header().id()),c.ordinaryContinuation().statement());
        var local=regional("01 WS-PGM PIC X(8).","EXEC CICS XCTL PROGRAM('PROGA') NOHANDLE END-EXEC.");
        var f=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)local.statements().get(0);
        assertThrows(IllegalArgumentException.class,()->new io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact(f.header(),f.command(),f.rawText(),f.target(),f.options(),c.conditions(),f.localContinuation(),f.ordinaryContinuation(),f.nameProfile(),f.gapCodes()));
        assertThrows(IllegalArgumentException.class,()->new io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact(c.header(),c.command(),c.rawText(),c.target(),c.options(),f.conditions(),c.localContinuation(),c.ordinaryContinuation(),c.nameProfile(),f.gapCodes()));
        var invalidGaps=new ArrayList<>(c.gapCodes());invalidGaps.add("CICS_UNMODELED_OPTION");
        assertThrows(IllegalArgumentException.class,()->new io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact(c.header(),c.command(),c.rawText(),c.target(),c.options(),c.conditions(),c.localContinuation(),c.ordinaryContinuation(),c.nameProfile(),invalidGaps));
    }

    @Test void cliCompositionDisablesCicsControlProofs() {
        for(String command:List.of("LINK","XCTL"))for(var mode:List.of(CicsProgramControlAnalyzer.EntryMode.UNKNOWN,CicsProgramControlAnalyzer.EntryMode.DISABLED)) {
            var a=AstBoundaryTestSupport.analyze(PerformFamilyTest.source("PERFORM A THRU A.\nCALL 'AFTER'.\n","A.\nEXEC CICS "+command+" PROGRAM('PROGA')\nNOHANDLE END-EXEC.\nB.\nCALL 'OUTSIDE'.\nGOBACK.\n"),"disabled-perform.cbl");
            var port=ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,StorageInitialSemantics.EntryMode.UNKNOWN,mode);
            var p=port.statements().stream().filter(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.ProcedurePerformFact.class::isInstance).map(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.ProcedurePerformFact.class::cast).findFirst().orElseThrow();
            if(mode==CicsProgramControlAnalyzer.EntryMode.DISABLED) {
                assertTrue(p.gapCodes().contains("PERFORM_PARAGRAPH_BOUNDARY_NOT_PROVEN"),p.gapCodes().toString());assertTrue(p.procedures().isEmpty());
                assertTrue(port.statements().stream().noneMatch(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact.class::isInstance));
            } else {assertTrue(p.gapCodes().isEmpty(),p.gapCodes().toString());assertEquals(1,p.procedures().get(0).completions().size());}
        }
    }

    @Test void completeCliPublishesCicsAndReadiness(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var source=directory.resolve("cli.cbl");
        java.nio.file.Files.writeString(source,ScalarMoveCheckpoint4ATest.program("01 WS-PGM PIC X(8).",
            "EXEC CICS LINK PROGRAM('PROGA') END-EXEC.\nEXEC CICS XCTL PROGRAM(WS-PGM) NOHANDLE END-EXEC.\nGOBACK.").lines().map(line->"       "+line).collect(java.util.stream.Collectors.joining("\n","","\n")));
        var output=directory.resolve("out");
        ExplorerMain.main(new String[]{"--source",source.toString(),"--copybooks",directory.toString(),"--output",output.toString(),"--storage-profile","ibm-enterprise-6.4-fixed-display-1047@1"});
        var doc=new com.fasterxml.jackson.databind.ObjectMapper().readTree(output.resolve("cobol-semantic-product.json").toFile());
        int sites=0;for(var statement:doc.path("statements"))if(statement.path("variant").asText().equals("CICS_PROGRAM_CONTROL"))sites++;
        assertEquals(2,sites);assertTrue(java.nio.file.Files.size(output.resolve("resolution-data.js"))>0);
        var port=io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(regional("01 WS-PGM PIC X(8).","EXEC CICS LINK PROGRAM('PROGA') END-EXEC."));
        var audit=io.github.gustavo2358.cobolexplorer.semanticproduct.consumer.CobolLoweringReadinessConsumer.audit(port);
        assertEquals("CICS_PROGRAM_CONTROL",audit.statements().get(0).family().name());
        assertEquals(port.statements().get(0),((io.github.gustavo2358.cobolexplorer.semanticproduct.consumer.CobolLoweringReadinessConsumer.CicsAudit)audit.statements().get(0)).fact());
    }

}
