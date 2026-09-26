package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutTest.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** IBM SC27-8713-03 (2026-04-28), READ p431 and SAME RECORD AREA p157. */
class FileStorageLayoutTest {
    @Test void recordTopologyIgnoresStatementsAndOrthogonalInputDiagnostics() {
        String control="SELECT F ASSIGN TO CLIENTDD.";
        String records="FD F.\n01 R.\n 02 K PIC X(8).";
        var declarationOnly=fixture(control,records,"01 BUF PIC X(8).","GOBACK.");
        var withEffects=fixture(control,records,"01 BUF PIC X(8).","READ F INTO BUF.\nWRITE R FROM BUF.\nGOBACK.");
        assertEquals(declarationOnly.product().logicalExactViews(),withEffects.product().logicalExactViews());
        for(int diagnostics:new int[]{1,50}) {
            var withGaps=fixture(control,records,"COPY MISSINGDP.\n".repeat(diagnostics)+"01 BUF PIC X(8).","GOBACK.");
            var record=withGaps.product().logicalExactViews().stream()
                .filter(v->v.node().equals(withGaps.view("R").node())||v.node().equals(withGaps.view("K").node())).toList();
            assertEquals(2,record.size());
            assertEquals(record.get(0).representative(),record.get(1).representative());
            assertTrue(record.stream().allMatch(v->v.length().equals(java.math.BigInteger.valueOf(8))));
            assertTrue(withGaps.layout().reasons().contains(Reason.INPUT_MISSING));
        }
    }
    @Test void completeTextRecordAndOnlyChildPublishLocalLogicalIdentity() {
        var f=fixture("SELECT F ASSIGN TO CLIENTDD.","FD F.\n01 R.\n 02 K PIC X(8).","01 BUF PIC X(8).");
        var exact=f.product().logicalExactViews().stream()
            .filter(v->v.node().equals(f.view("R").node())||v.node().equals(f.view("K").node())).toList();
        assertEquals(2,exact.size());
        assertEquals(exact.get(0).representative(),exact.get(1).representative());
        assertTrue(exact.stream().allMatch(v->v.length().equals(java.math.BigInteger.valueOf(8))));
        assertNotEquals(exact.get(0).node(),exact.get(1).node());
    }
    @Test void partialChildrenAndIndependentRecordsDoNotBecomeCompleteAliases() {
        var split=fixture("SELECT F ASSIGN TO CLIENTDD.","FD F.\n01 R.\n 02 A PIC X(4).\n 02 B PIC X(4).","");
        assertTrue(split.product().logicalExactViews().isEmpty());
        var partial=fixture("SELECT F ASSIGN TO CLIENTDD.",
            "FD F.\n01 R.\n 02 A PIC X(8).\n 02 B REDEFINES A PIC X(4).","");
        assertTrue(partial.product().logicalExactViews().isEmpty());
        var separate=fixture("SELECT F ASSIGN TO CLIENTDD.\nSELECT G ASSIGN TO OTHERDD.",
            "FD F.\n01 R.\n 02 K PIC X(8).\nFD G.\n01 S.\n 02 L PIC X(8).","01 BUF PIC X(8).");
        var exact=separate.product().logicalExactViews().stream()
            .filter(v->v.node().equals(separate.view("R").node())||v.node().equals(separate.view("K").node())
                ||v.node().equals(separate.view("S").node())||v.node().equals(separate.view("L").node())).toList();
        assertEquals(4,exact.size());
        assertEquals(2,exact.stream().map(LogicalExactView::representative).distinct().count());
        assertNotEquals(separate.view("R").base(),separate.view("S").base());
    }
    static Fixture fixture(String control,String records,String working) {
        return fixture(control,records,working,"GOBACK.");
    }
    static Fixture fixture(String control,String records,String working,String body) {
        var source="IDENTIFICATION DIVISION.\nPROGRAM-ID. FILEMEM.\nENVIRONMENT DIVISION.\nINPUT-OUTPUT SECTION.\nFILE-CONTROL.\n"+control+"\nDATA DIVISION.\nFILE SECTION.\n"+records+"\nWORKING-STORAGE SECTION.\n"+working+"\nPROCEDURE DIVISION.\n"+body+"\n";
        var a=AstBoundaryTestSupport.analyze(source,"file-storage.cbl");
        var product=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        return new Fixture(a,product,product.layout(a.model().programUnits().get(0).id()));
    }
    @Test void twoDescriptionsOfOneFdShareBaseWithMaximumFootprint() {
        var f=fixture("SELECT F ASSIGN TO INDD.","FD F.\n01 LONG-REC PIC X(12).\n01 SHORT-REC PIC X(4).","01 SAFE PIC X(8).");
        assertEquals(f.view("LONG-REC").base(),f.view("SHORT-REC").base());assertNotEquals(f.view("LONG-REC").base(),f.view("SAFE").base());
        known(0,f.view("SHORT-REC").offset());known(4,f.view("SHORT-REC").extent());known(12,f.layout().bases().stream().filter(b->b.id().equals(f.view("LONG-REC").base())).findFirst().orElseThrow().extent());
        assertEquals(2,f.layout().bases().size());assertTrue(f.layout().bases().stream().allMatch(Base::independent));
    }
    @Test void subordinateRedefinesAndRenamesStayInTheFileBuffer() {
        var f=fixture("SELECT F ASSIGN TO INDD.","FD F.\n01 REC.\n 05 RAW-PART PIC X(4).\n 05 VIEW-PART REDEFINES RAW-PART PIC X(6).\n 05 TAIL-PART PIC X(2).\n66 WHOLE RENAMES RAW-PART THRU TAIL-PART.","01 SAFE PIC X(8).");
        known(0,f.view("RAW-PART").offset());known(0,f.view("VIEW-PART").offset());known(6,f.view("TAIL-PART").offset());known(8,f.view("REC").extent());
        assertTrue(f.layout().renames().stream().anyMatch(Renaming::proved));assertNotEquals(f.view("REC").base(),f.view("SAFE").base());
    }
    @Test void differentFilesAreIndependentUntilSameRecordAreaProvesAlias() {
        var controls="SELECT F ASSIGN TO INDD.\nSELECT G ASSIGN TO OUTDD.";
        var records="FD F.\n01 F-REC PIC X(8).\nFD G.\n01 G-REC PIC X(12).";
        var separate=fixture(controls,records,"01 SAFE PIC X(8).");assertNotEquals(separate.view("F-REC").base(),separate.view("G-REC").base());
        var shared=fixture(controls+"\nI-O-CONTROL.\n SAME RECORD AREA FOR F G.",records,"01 SAFE PIC X(8).");
        assertEquals(shared.view("F-REC").base(),shared.view("G-REC").base());assertNotEquals(shared.view("F-REC").base(),shared.view("SAFE").base());
        known(12,shared.layout().bases().stream().filter(b->b.id().equals(shared.view("F-REC").base())).findFirst().orElseThrow().extent());
    }
    @Test void sameSortAreaDoesNotPretendToBeSameRecordArea() {
        var f=fixture("SELECT F ASSIGN TO INDD.\nSELECT G ASSIGN TO OUTDD.\nI-O-CONTROL.\n SAME SORT AREA FOR F G.","SD F.\n01 F-REC PIC X(8).\nFD G.\n01 G-REC PIC X(8).","01 SAFE PIC X(8).");
        assertNotEquals(f.view("F-REC").base(),f.view("G-REC").base());
    }
    @Test void unknownRecordExtentCannotEraseDisjointWorkingStorage() {
        var f=fixture("SELECT F ASSIGN TO INDD.","FD F.\n01 REC.\n 05 PREFIX-PART PIC X(2).\n 05 OPAQUE-PART PIC 9(4).\n 05 TAIL-PART PIC X(4).","01 SAFE PIC X(8).");
        assertTrue(f.view("REC").extent().value().isEmpty());known(0,f.view("PREFIX-PART").offset());known(2,f.view("PREFIX-PART").extent());
        known(8,f.view("SAFE").extent());assertTrue(f.layout().bases().stream().filter(b->b.id().equals(f.view("SAFE").base())).findFirst().orElseThrow().independent());
    }

    @Test void unresolvedOrRepeatedSharingCannotProveAnAllocation() {
        for(var clause:List.of("SAME RECORD AREA FOR F MISSING-F.","SAME RECORD AREA FOR F G\nSAME RECORD AREA FOR F G.")) {
            var f=fixture("SELECT F ASSIGN TO INDD.\nSELECT G ASSIGN TO OUTDD.\nI-O-CONTROL.\n"+clause,"FD F.\n01 F-REC PIC X(8).\nFD G.\n01 G-REC PIC X(8).","01 SAFE PIC X(8).");
            assertTrue(f.layout().bases().stream().noneMatch(Base::independent));
        }
    }
    @Test void globalFileCannotCertifySameRecordAreaRelation() {
        var f=fixture("SELECT F ASSIGN TO INDD.\nSELECT G ASSIGN TO OUTDD.\nI-O-CONTROL.\nSAME RECORD AREA FOR F G.","FD F IS GLOBAL.\n01 F-REC PIC X(8).\nFD G.\n01 G-REC PIC X(8).","01 SAFE PIC X(8).");
        assertNotEquals(f.view("F-REC").base(),f.view("G-REC").base());assertTrue(f.layout().bases().stream().noneMatch(Base::independent));
    }
    @Test void recordContainsZeroCannotCertifySameRecordAreaRelation() {
        var f=fixture("SELECT F ASSIGN TO INDD.\nSELECT G ASSIGN TO OUTDD.\nI-O-CONTROL.\nSAME RECORD AREA FOR F G.","FD F RECORD CONTAINS 0 CHARACTERS.\n01 F-REC PIC X(8).\nFD G.\n01 G-REC PIC X(8).","01 SAFE PIC X(8).");
        assertNotEquals(f.view("F-REC").base(),f.view("G-REC").base());assertTrue(f.layout().bases().stream().noneMatch(Base::independent));
    }
    @Test void recordDependingIsAResolvedDeclarationReference() {
        var f=fixture("SELECT F ASSIGN TO INDD.","FD F RECORD IS VARYING FROM 2 TO 8 CHARACTERS\n DEPENDING ON REC-LENGTH.\n01 REC PIC X(8).","01 REC-LENGTH PIC 9(4).\n01 SAFE PIC X(8).");
        assertTrue(f.source().resolution().entries().stream().anyMatch(e->e.occurrence().writtenText().equals("REC-LENGTH")
            &&e.occurrence().role()==ResolutionContracts.ReferenceRole.DECLARATION_RELATION&&e.status()==ResolutionContracts.ResolutionStatus.RESOLVED));
    }
    @Test void sameAreaOnIndexedFilesHasVsamRecordAliasSemantics() {
        var f=fixture("SELECT F ASSIGN TO INDD ORGANIZATION INDEXED\n RECORD KEY FK.\nSELECT G ASSIGN TO OUTDD\n ORGANIZATION INDEXED RECORD KEY GK.\nI-O-CONTROL.\n SAME AREA FOR F G.","FD F.\n01 F-REC.\n 02 FK PIC X(8).\nFD G.\n01 G-REC.\n 02 GK PIC X(8).","01 SAFE PIC X(8).");
        assertEquals(f.view("F-REC").base(),f.view("G-REC").base());
        assertNotEquals(f.view("F-REC").base(),f.view("SAFE").base());
    }
    @Test void sameAreaVsamSequentialSharesRecordArea() {
        var f=fixture("SELECT F ASSIGN TO AS-INDD.\nSELECT G ASSIGN TO AS-OUTDD.\nI-O-CONTROL.\n SAME AREA FOR F G.","FD F.\n01 F-REC PIC X(8).\nFD G.\n01 G-REC PIC X(8).","01 SAFE PIC X(8).");
        assertEquals(f.view("F-REC").base(),f.view("G-REC").base());assertNotEquals(f.view("SAFE").base(),f.view("F-REC").base());
    }
    @Test void sameAreaQsamIsDocumentaryAndPreservesSeparation() {
        var f=fixture("SELECT F ASSIGN TO INDD.\nSELECT G ASSIGN TO OUTDD.\nI-O-CONTROL.\n SAME AREA FOR F G.","FD F.\n01 F-REC PIC X(8).\nFD G.\n01 G-REC PIC X(8).","01 SAFE PIC X(8).");
        assertNotEquals(f.view("F-REC").base(),f.view("G-REC").base());
        assertTrue(f.layout().bases().stream().allMatch(Base::independent));
    }

}
