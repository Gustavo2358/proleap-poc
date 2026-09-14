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
            Optional.of(new CicsProgramControlAnalyzer().analyze(a.build())));
        var state=io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.project(enriched,unit);
        var fact=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)state.statements().get(0);
        assertEquals("LINK",fact.command().name());assertEquals(2,fact.options().size());assertFalse(fact.gapCodes().isEmpty());
        assertEquals("aBc",((io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.LiteralCallTarget)fact.target().orElseThrow()).text());
        var bytes=io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(
            io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state));
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(bytes);
        assertEquals("2.13.0",json.path("contractVersion").asText());assertEquals("CICS_PROGRAM_CONTROL",json.path("statements").get(0).path("variant").asText());
        var disabled=io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.project(source,unit);
        assertTrue(disabled.statements().get(0) instanceof io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.ObservedStatement);
        if(System.getProperty("cics.fixture.output")!=null)java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("cics.fixture.output")),bytes);
    }

    static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.State regional(String data,String code) {
        var a=StorageAccessTest.fixture(data,code.replace(" COMMAREA(","\nCOMMAREA(").replace(" NOHANDLE","\nNOHANDLE").replace(" ELSE ","\nELSE ").replace(" END-IF","\nEND-IF"));var f=a.source();
        return io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.project(new io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.FrontendProducts(
            f.build(),f.tables(),f.occurrences(),f.resolution(),f.report(),ScalarMoveSemantics.analyze(f.build(),f.tables(),f.resolution(),f.report()),Optional.of(a.effects()),Optional.of(new CicsProgramControlAnalyzer().analyze(f.build()))),f.model().programUnits().get(0).id());
    }
    @Test void canonicalHostBindingKeepsQualificationAliasesAndEightByteViews() throws Exception {
        var s=regional("01 FIRST-AREA.\n05 WS-PGM PIC X(8).\n01 SECOND-AREA.\n05 WS-PGM PIC X(8).",
            "MOVE 'PROGA' TO WS-PGM OF FIRST-AREA.\nEXEC CICS LINK PROGRAM(WS-PGM OF FIRST-AREA) COMMAREA(FIRST-AREA) NOHANDLE END-EXEC.");
        var fact=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact)s.statements().get(1);
        var reference=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.DataReference)fact.target().orElseThrow();
        assertTrue(reference.regionalAccess().isPresent());assertTrue(reference.binding().selected().isPresent());
        var move=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.MoveFact)s.statements().get(0);
        assertEquals(move.target().binding().selected(),reference.binding().selected());
        assertEquals(move.target().regionalAccess(),reference.regionalAccess());
        assertTrue(fact.options().stream().filter(o->o.name().equals("COMMAREA")).findFirst().orElseThrow().reference().isPresent());
        emit("qualified",s);
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

}
