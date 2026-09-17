package io.github.gustavo2358.cobolexplorer;
import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.CompilationSemanticProductJsonWriter;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** IBM6.4 pp63–66: declaration owner, use owner and imported DATA identity are independent. */
class FileScopeContractTest {
    static String program(String name,String select,String fd,String ws,String body,boolean end) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. "+name+".\nENVIRONMENT DIVISION.\nINPUT-OUTPUT SECTION.\nFILE-CONTROL.\n"+select+"\nDATA DIVISION.\nFILE SECTION.\n"+fd+"\nWORKING-STORAGE SECTION.\n"+ws+"\nPROCEDURE DIVISION.\n"+body+"\nGOBACK.\n"+(end?"END PROGRAM "+name+".\n":"");
    }
    static String nested(){return program("PARENT","SELECT SHARED-F ASSIGN TO PARENTDD.\nSELECT PRIVATE-F ASSIGN TO PRIVDD.","FD SHARED-F IS GLOBAL.\n01 GLOBAL-REC PIC X(8).\nFD PRIVATE-F.\n01 PRIVATE-REC PIC X(8).","01 GLOBAL-NAME IS GLOBAL PIC X(8).","CALL 'PARENTPG'.",false)
        +program("CHILD","","","","OPEN INPUT SHARED-F.\nWRITE GLOBAL-REC.\nCALL GLOBAL-NAME.\nEXEC CICS LINK PROGRAM('CHILDLNK') NOHANDLE END-EXEC.",true)
        +program("SHADOW","SELECT SHARED-F ASSIGN TO CHILDDD.","FD SHARED-F.\n01 CHILD-REC PIC X(8).","","OPEN OUTPUT SHARED-F.\nCALL 'SHADOWPG'.",true)+"END PROGRAM PARENT.\n";}
    static JsonNode publish(String name,String source)throws Exception {
        var a=AstBoundaryTestSupport.analyze(source,name+".cbl");
        var p=ExplorerMain.publishCompilationSemanticProduct(a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,StorageInitialSemantics.EntryMode.UNKNOWN,CicsProgramControlAnalyzer.EntryMode.UNKNOWN);
        var bytes=CompilationSemanticProductJsonWriter.serialize(p);assertArrayEquals(bytes,CompilationSemanticProductJsonWriter.serialize(p));
        var out=Path.of(".harness-results/fd-w9/exports");Files.createDirectories(out);Files.write(out.resolve(name+".json"),bytes);Files.writeString(out.resolve(name+".cbl"),source);
        return new ObjectMapper().readTree(bytes);
    }
    static JsonNode unit(JsonNode root,String name){for(var u:root.path("units"))if(u.path("product").path("unit").path("canonicalProgramName").asText().equals(name))return u;throw new AssertionError(name);}
    @Test void globalRecordAndDataCaptureKeepOriginalOwnerAndShadowStaysLocal()throws Exception {
        var root=publish("nested",nested());assertEquals("cobol-semantic-compilation",root.path("schema").asText());assertEquals("1.0.0",root.path("contractVersion").asText());assertEquals("COMPLETE",root.path("inventoryStatus").asText());assertEquals(3,root.path("unitInventory").size());assertEquals(3,root.path("units").size());
        var parent=unit(root,"PARENT");var child=unit(root,"CHILD");var shadow=unit(root,"SHADOW");var owner=parent.path("product").path("unit");
        assertTrue(parent.path("product").path("fileInventory").path("operations").path("uses").isEmpty(),"child FILE never belongs to parent");
        assertEquals(owner,child.path("parent"));assertEquals(owner,shadow.path("parent"));
        var uses=child.path("product").path("fileInventory").path("operations").path("uses");assertEquals(2,uses.size());
        for(var use:uses){assertEquals("RESOLVED",use.path("bindingStatus").asText());assertEquals(owner,use.path("candidates").get(0).path("owner"));}
        assertEquals(1,child.path("fileCaptures").size());assertEquals(owner,child.path("fileCaptures").get(0).path("owner"));
        assertFalse(child.path("dataCaptures").isEmpty());for(var capture:child.path("dataCaptures"))assertEquals(owner,capture.path("sourceUnit"));
        assertEquals(shadow.path("product").path("unit"),shadow.path("product").path("fileInventory").path("operations").path("uses").get(0).path("candidates").get(0).path("owner"));
        assertTrue(shadow.path("fileCaptures").isEmpty());
    }
    @Test void externalHomonymsAndPartialSiblingNeverFuseByName()throws Exception {
        var source=program("ONE","SELECT F ASSIGN TO COMMONDD.","FD F IS EXTERNAL.\n01 R PIC X(8).","","OPEN INPUT F.\nCALL 'ONECALL'.",true)
            +program("TWO","SELECT F ASSIGN TO COMMONDD.","FD F IS EXTERNAL.\n01 R PIC X(8).","","DISPLAY 'PARTIAL'.\nOPEN INPUT F.\nCALL 'TWOCALL'.",true);
        var root=publish("external",source);assertEquals(2,root.path("units").size());var a=unit(root,"ONE");var b=unit(root,"TWO");assertNotEquals(a.path("product").path("unit"),b.path("product").path("unit"));
        for(var u:root.path("units")){assertTrue(u.path("parent").isNull());assertTrue(u.path("dataCaptures").isEmpty());assertTrue(u.path("fileCaptures").isEmpty());var d=u.path("product").path("fileInventory").path("declarations").get(0);assertEquals("EXTERNAL",d.path("visibility").asText());assertEquals("COMMONDD",d.path("assignment").path("externalFileName").asText());assertEquals(1,u.path("product").path("fileInventory").path("operations").path("uses").size());}
    }
    @Test void cicsProgramContributionCannotEscapeItsProgramUnit() {
        var a=AstBoundaryTestSupport.analyze(nested(),"nested-cics.cbl");var c=new CicsProgramControlAnalyzer().analyze(a.build(),a.report(),CicsProgramControlAnalyzer.EntryMode.UNKNOWN);var child=a.model().programUnits().get(1);var queue=new ArrayDeque<Ast.Node>();queue.add(child.program());int statement=-1;
        while(!queue.isEmpty()){var n=queue.removeFirst();if(n instanceof Ast.EmbeddedLanguageStatement)statement=n.meta().id();queue.addAll(Ast.children(n));}
        assertTrue(statement>=0);assertTrue(c.fact(child.id(),statement).isPresent());assertTrue(c.fact(a.model().programUnits().get(0).id(),statement).isEmpty(),"parent must not own contained LINK");
    }
    @Test void repeatedCopyReplacingPreservesSeparateOwnersAndOrigins()throws Exception {
        var a=program("COPY-A","SELECT A ASSIGN TO SAMEEXT.","COPY FDW0 REPLACING FILE-TEMPLATE BY A\n RECORD-TEMPLATE BY RA.","","WRITE RA.\nCALL 'COPYA'.",true);
        var b=program("COPY-B","SELECT B ASSIGN TO SAMEEXT.","COPY FDW0 REPLACING FILE-TEMPLATE BY B\n RECORD-TEMPLATE BY RB.","","WRITE RB.\nCALL 'COPYB'.",true);
        var root=publish("copy",a+b);assertEquals(2,root.path("units").size());
        for(var u:root.path("units")){var d=u.path("product").path("fileInventory").path("declarations").get(0);assertEquals("SAMEEXT",d.path("assignment").path("externalFileName").asText());assertEquals(1,d.path("origins").get(1).path("includeChain").size());assertEquals(u.path("product").path("unit"),d.path("owner"));assertTrue(u.path("fileCaptures").isEmpty());assertTrue(u.path("dataCaptures").isEmpty(),"COPY repeated AST handles must be qualified by unit");}
    }
    @Test void qualificationAndAmbiguityAreLocalAndDoNotFuseRecords()throws Exception {
        var root=publish("qualified",program("QUALIFIED","SELECT F ASSIGN TO FIRSTDD.\nSELECT G ASSIGN TO SECONDDD.","FD F.\n01 R PIC X(8).\nFD G.\n01 R PIC X(8).","","WRITE R OF F.\nWRITE R.\nCALL 'KEEP'.",true));
        var uses=root.path("units").get(0).path("product").path("fileInventory").path("operations").path("uses");assertEquals(2,uses.size());assertEquals("RESOLVED",uses.get(0).path("bindingStatus").asText());assertEquals(1,uses.get(0).path("candidates").size());assertEquals("AMBIGUOUS",uses.get(1).path("bindingStatus").asText());assertEquals(2,uses.get(1).path("candidates").size());
    }
    @Test void missingCopyKeepsObservedUnitsAndMakesCompilationInventoryExplicit()throws Exception {
        var a=program("GOOD","SELECT F ASSIGN TO GOODDD.","FD F.\n01 R PIC X(8).","","OPEN INPUT F.\nCALL 'GOODCALL'.",true);
        var b=program("GAP","SELECT F ASSIGN TO GAPDD.","FD F.\n01 R PIC X(8).","COPY SECRET.","OPEN INPUT F.\nCALL 'GAPCALL'.",true);
        var root=publish("missing-copy",a+b);assertEquals("INPUT_MISSING",root.path("inventoryStatus").asText());assertEquals(2,root.path("units").size());
        assertEquals(1,unit(root,"GOOD").path("product").path("fileInventory").path("operations").path("uses").size());assertEquals("INPUT_MISSING",unit(root,"GAP").path("product").path("entryInventory").path("status").asText());
    }

    @Test void globalReadKeepsItsImplicitRecordBoundAndLocalCallDataDisjoint()throws Exception {
        var source=program("OWNER","SELECT F ASSIGN TO GLOBALDD.","FD F IS GLOBAL.\n01 R PIC X(8).","","CONTINUE.",false)
            +program("USER","","","01 PGM PIC X(8).","MOVE 'KEEP' TO PGM.\nREAD F.\nCALL PGM.",true)+"END PROGRAM OWNER.\n";
        var root=publish("global-read",source);var u=unit(root,"USER");var effect=u.path("product").path("fileInventory").path("operations").path("uses").get(0).path("effects");
        assertFalse(effect.path("unknownWriteBound").asBoolean(),"known GLOBAL record ownership bounds writes without claiming a physical extent");
        assertEquals(1,u.path("dataCaptures").size());for(var outcome:effect.path("outcomes")){assertEquals(1,outcome.path("steps").size());assertEquals("MAY_UNKNOWN",outcome.path("steps").get(0).path("kind").asText());assertEquals("data:1",outcome.path("steps").get(0).path("destination").path("data").asText());}
    }

}
