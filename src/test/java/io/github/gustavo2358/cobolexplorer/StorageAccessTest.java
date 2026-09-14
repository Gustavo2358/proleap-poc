package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;
import static io.github.gustavo2358.cobolexplorer.StorageAccessSemantics.*;
import io.github.gustavo2358.cobolexplorer.StorageAccessSemantics.Reason;

class StorageAccessTest {
    record Fixture(AstBoundaryTestSupport.Analysis source,StorageAccessSemantics effects) { }
    static Fixture fixture(String data,String code,Profile profile) {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data,code+"\nGOBACK."),"access.cbl");
        var layout=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),profile);
        return new Fixture(a,StorageAccessSemantics.analyze(a.build(),a.resolution(),layout));
    }
    static Fixture fixture(String data,String code){return fixture(data,code,Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);}
    @Test void groupBytesAndQualifiedCallUseResolvedPhysicalViewWithoutChangingLegacyAccess() {
        var f=fixture("01 FIRST-AREA.\n05 PREFIX-PART PIC X(2).\n05 FILLER PIC X(2).\n05 WS-PGM PIC X(4).\n01 SECOND-AREA.\n05 WS-PGM PIC X(4).",
            "MOVE 'ABCDEFGH' TO FIRST-AREA.\nCALL WS-PGM OF FIRST-AREA.\nCALL WS-PGM OF SECOND-AREA.");
        var move=f.effects.moves().iterator().next();assertEquals(MoveKind.LITERAL_BYTES,move.kind());
        assertEquals(List.of(0xc1,0xc2,0xc3,0xc4,0xc5,0xc6,0xc7,0xc8),move.bytes());
        var calls=f.effects.accesses().stream().filter(a->a.role()==Role.CALL_TARGET).sorted(Comparator.comparingInt(a->a.statement().node())).toList();
        assertEquals(2,calls.size());assertEquals(move.destination().orElseThrow().view().base(),calls.get(0).view().base());
        assertEquals(Optional.of(BigInteger.valueOf(4)),calls.get(0).view().offset().value());
        assertNotEquals(calls.get(0).view().base(),calls.get(1).view().base());
        var legacy=ScalarMoveSemantics.analyze(f.source.build(),f.source.tables(),f.source.resolution(),f.source.report());
        var unit=f.source.model().programUnits().get(0).id();
        for(var call:AstBoundaryTestSupport.nodes(f.source,Ast.CallStatement.class))assertTrue(legacy.call(unit,call.meta().id()).wholeItem().isEmpty());
    }
    @Test void copiesRequireEqualExtentsAndProvedDisjointRanges() {
        var f=fixture("01 SOURCE-PART PIC X(4).\n01 TARGET-PART PIC X(4).","MOVE SOURCE-PART TO TARGET-PART.");
        var copy=f.effects.moves().iterator().next();assertEquals(MoveKind.COPY_BYTES,copy.kind());assertTrue(copy.bytes().isEmpty());
        assertTrue(copy.source().isPresent());assertNotEquals(copy.source().get().view().base(),copy.destination().orElseThrow().view().base());
        var same=fixture("01 WS-AREA.\n05 CHILD-PART PIC X(4).","MOVE WS-AREA TO CHILD-PART.").effects.moves().iterator().next();
        assertEquals(MoveKind.MUST_UNKNOWN,same.kind());assertTrue(same.reasons().contains(Reason.OVERLAPPING_COPY));
        var mismatch=fixture("01 SOURCE-PART PIC X(2).\n01 TARGET-PART PIC X(4).","MOVE SOURCE-PART TO TARGET-PART.").effects.moves().iterator().next();
        assertEquals(MoveKind.FIT_TEXT,mismatch.kind());assertTrue(mismatch.reasons().isEmpty());
    }
    @Test void unsupportedLiteralKeepsMandatoryFootprintWithoutInventingBytes() {
        for(var text:List.of("€")) {
            var move=fixture("01 WS-AREA.\n05 CHILD-PART PIC X(4).","MOVE '"+text+"' TO WS-AREA.").effects.moves().iterator().next();
            assertEquals(MoveKind.MUST_UNKNOWN,move.kind());assertTrue(move.destination().isPresent());assertTrue(move.bytes().isEmpty());assertFalse(move.reasons().isEmpty());
        }
    }
    @Test void absentProfileAndUnknownPrefixNeverPublishPreciseAccess() {
        var absent=fixture("01 WS-AREA.\n05 CHILD-PART PIC X(4).","MOVE 'ABCD' TO WS-AREA.\nCALL CHILD-PART.",Profile.UNSPECIFIED);
        assertTrue(absent.effects.accesses().isEmpty());assertEquals(MoveKind.UNAVAILABLE,absent.effects.moves().iterator().next().kind());
        var unknown=fixture("01 WS-AREA.\n05 PREFIX-PART PIC 9.\n05 CHILD-PART PIC X(4).","MOVE 'ABCD' TO CHILD-PART.\nCALL CHILD-PART.");
        assertTrue(unknown.effects.accesses().isEmpty());assertEquals(MoveKind.UNAVAILABLE,unknown.effects.moves().iterator().next().kind());
    }
    @Test void explicitIanaCodecHasNoHostCharsetOrReplacement() {
        assertEquals(Optional.of(List.of(0xd7,0xc7,0xd4,0xf0,0xf0,0xf0,0xf0,0xf1)),StorageAccessSemantics.encode("PGM00001",Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047));
        assertEquals(Optional.of(List.of(0x40,0x25,0x15)),StorageAccessSemantics.encode(" \n\u0085",Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047));
        assertTrue(StorageAccessSemantics.encode("PGM00001",Profile.UNSPECIFIED).isEmpty());
        assertTrue(StorageAccessSemantics.encode("€",Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047).isEmpty());
        assertTrue(StorageAccessSemantics.encode("\ud800",Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047).isEmpty());
    }
    @Test void equalLookingIdsCannotBorrowAnotherLayoutsProof() {
        var a=fixture("01 WS-AREA PIC X(4).","MOVE 'ABCD' TO WS-AREA.").source();
        var b=fixture("01 WS-AREA PIC X(8).","MOVE 'ABCD' TO WS-AREA.").source();
        var foreign=StorageLayoutSemantics.analyze(b.build(),b.tables(),b.resolution(),b.report(),Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        assertEquals(a.model().programUnits().get(0).id(),b.model().programUnits().get(0).id());
        assertThrows(IllegalArgumentException.class,()->StorageAccessSemantics.analyze(a.build(),a.resolution(),foreign));
    }
    @Test void all256CodePointsMatchTheIndependentIanaMapping() throws Exception {
        var doc=new com.fasterxml.jackson.databind.ObjectMapper().readTree(java.nio.file.Path.of("src/test/resources/storage/iana-ibm1047-mapping.json").toFile());
        assertEquals("a4d946d97ebc37ae432f1ec12bd5afec2ac54fda4c8537b9a6f41e6e16e8a44a",doc.path("sha256").asText());
        var mapping=doc.path("mapping");assertEquals(256,mapping.size());
        for(var rows=mapping.fields();rows.hasNext();) {
            var row=rows.next();int octet=Integer.parseInt(row.getKey(),16);int scalar=Integer.parseInt(row.getValue().asText(),16);
            assertEquals(Optional.of(List.of(octet)),StorageAccessSemantics.encode(Character.toString(scalar),Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047));
        }
    }
}
