package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** IBM SC27-8713-03 (2026-04-28), pp400-404/434-437/452-459.
 * Expected participants, phases and effects are source-derived, never snapshots. */
class FileSortContractTest {
    static final String SELECT="SELECT S ASSIGN TO SORTWK.\nSELECT A ASSIGN TO INA.\nSELECT B ASSIGN TO INB.\nSELECT C ASSIGN TO OUTC.";
    static final String FILES="SD S.\n01 SR.\n 02 SK PIC X(4).\n 02 SK2 PIC X(4).\nFD A.\n01 AR PIC X(8).\nFD B.\n01 BR PIC X(8).\nFD C.\n01 CR PIC X(8).";
    static JsonNode publish(String body)throws Exception {
        var a=AstBoundaryTestSupport.analyze("IDENTIFICATION DIVISION.\nPROGRAM-ID. SORTIO.\nENVIRONMENT DIVISION.\nINPUT-OUTPUT SECTION.\nFILE-CONTROL.\n"+SELECT+"\nDATA DIVISION.\nFILE SECTION.\n"+FILES+"\nWORKING-STORAGE SECTION.\n01 BUF PIC X(8).\nPROCEDURE DIVISION.\n"+body+"\nEND PROGRAM SORTIO.\n","sort-io.cbl");
        var port=ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(port));
    }
    static JsonNode uses(JsonNode p){return p.path("fileInventory").path("operations").path("uses");}
    static JsonNode use(JsonNode p,String command){for(var u:uses(p))if(u.path("command").asText().equals(command))return u;throw new AssertionError("Missing "+command);}
    static String id(JsonNode p,String name){for(var d:p.path("fileInventory").path("declarations"))if(d.path("logicalFile").asText().equals(name))return d.path("id").asText();throw new AssertionError(name);}
    static JsonNode outcome(JsonNode u,String event){for(var o:u.path("effects").path("outcomes"))if(o.path("outcome").asText().equals(event))return o;throw new AssertionError(event);}
    static void save(String name,JsonNode p)throws Exception{Files.createDirectories(Path.of("target/fd-w5"));Files.writeString(Path.of("target/fd-w5/"+name+".json"),p.toPrettyString());}

    @Test void releaseFromUsesSdAndTransfersBeforeConsumption()throws Exception {
        var p=publish("SORT S ON ASCENDING KEY SK\n INPUT PROCEDURE IN-P GIVING C.\nGOBACK.\nIN-P.\nRELEASE SR FROM AR.");
        var r=use(p,"RELEASE");assertEquals(id(p,"S"),r.path("candidates").get(0).path("id").asText());
        assertEquals(1,r.path("effects").path("before").size());assertEquals("FROM_RECORD",r.path("effects").path("before").get(0).path("role").asText());
        assertTrue(r.path("effects").path("before").get(0).path("kind").asText().matches("COPY_BYTES|FIT_TEXT"));
        assertTrue(outcome(r,"SUCCESS").path("steps").toString().contains("RECORD"));
        assertFalse(r.path("effects").path("unknownWriteBound").asBoolean());
        assertTrue(java.util.stream.StreamSupport.stream(uses(p).spliterator(),false).noneMatch(u->u.path("candidates").toString().contains(id(p,"A"))),"FROM buffer is not file input");
        save("release-from",p);
    }
    @Test void returnIntoEndAndSuccessHaveSeparateEffectsAndHandlers()throws Exception {
        var p=publish("SORT S ON ASCENDING KEY SK USING A\n OUTPUT PROCEDURE OUT-P.\nGOBACK.\nOUT-P.\nRETURN S INTO BUF AT END CALL 'EMPTY'\n NOT AT END CALL 'RECORD' END-RETURN.");
        var r=use(p,"RETURN");assertEquals(id(p,"S"),r.path("candidates").get(0).path("id").asText());
        assertFalse(outcome(r,"END").path("steps").toString().contains("INTO"));
        assertTrue(outcome(r,"SUCCESS").path("steps").toString().contains("INTO"));
        var routes=r.path("control").path("routes");assertEquals(Set.of("SUCCESS","END"),values(routes,"event"));
        assertEquals(Set.of("AT_END","NOT_AT_END"),values(r.path("handlers"),"kind"));
        assertFalse(r.path("effects").path("unknownWriteBound").asBoolean());
        save("return-into",p);
    }
    @Test void sortAndMergeKeepAllParticipantsAndDistinctRoles()throws Exception {
        for(var verb:List.of("SORT","MERGE")) {
            var p=publish(verb+" S ON ASCENDING KEY SK USING A B GIVING C.\nGOBACK.");
            assertEquals(4,uses(p).size());var roles=new HashMap<String,String>();
            for(var u:uses(p))roles.put(u.path("candidates").get(0).path("id").asText(),u.path("role").asText());
            assertEquals(Map.of(id(p,"S"),"WORK",id(p,"A"),"INPUT",id(p,"B"),"INPUT",id(p,"C"),"OUTPUT"),roles);
            assertEquals(1,java.util.stream.StreamSupport.stream(uses(p).spliterator(),false).map(u->u.path("statement").asText()).distinct().count());
            for(var d:p.path("fileInventory").path("declarations"))if(d.path("logicalFile").asText().equals("S"))assertTrue(d.path("assignment").path("externalFileName").isNull());
            save(verb.toLowerCase()+"-multiparty",p);
        }
    }
    @Test void tableSortIsNotFileEvenWithoutWrittenKey()throws Exception {
        for(var body:List.of("SORT T ASCENDING KEY K.","SORT T.")) {
            var p=FileDeclarationContractTest.publish("","","WORKING-STORAGE SECTION.\n01 TAB.\n 02 T OCCURS 3 TIMES ASCENDING KEY K.\n  03 K PIC X.",body+"\nGOBACK.");
            assertTrue(uses(p).isEmpty());
            var table=AstBoundaryTestSupport.analyze("IDENTIFICATION DIVISION.\nPROGRAM-ID. TABTEST.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n01 TAB.\n 02 T OCCURS 3 TIMES ASCENDING KEY K.\n  03 K PIC X.\nPROCEDURE DIVISION.\n"+body+"\nGOBACK.\nEND PROGRAM TABTEST.","table.cbl");
            assertTrue(AstBoundaryTestSupport.nodes(table,Ast.FileReference.class).isEmpty());
            assertTrue(AstBoundaryTestSupport.nodes(table,Ast.DataReference.class).stream().anyMatch(r->r.baseName().equals("T")),"table operand stays DATA; SP table memory capability remains partial");save(body.contains("KEY")?"table-key":"table-no-key",p);
        }
    }
    @Test void fdOperandsCannotMasqueradeAsSd()throws Exception {
        var p=publish("RELEASE AR.\nRETURN A AT END CONTINUE END-RETURN.\nGOBACK.");
        assertEquals(2,uses(p).size());for(var u:uses(p))assertTrue(u.path("gapCodes").toString().contains("FILE_KIND_NOT_PROVEN"));
    }
    @Test void paragraphRangesPublishLocalEntriesAndReturnsWithoutInventedCalls()throws Exception {
        var p=publish("MAIN.\nSORT S ON ASCENDING KEY SK\n INPUT PROCEDURE IN-FIRST THRU IN-LAST\n OUTPUT PROCEDURE OUT-FIRST THRU OUT-LAST.\nCALL 'AFTER'.\nGOBACK.\nIN-FIRST.\nCALL 'INPGM'.\nRELEASE SR FROM BUF.\nIN-LAST.\nCALL 'INTAIL'.\nOUT-FIRST.\nRETURN S INTO BUF AT END CALL 'EOFPGM' END-RETURN.\nOUT-LAST.\nCALL 'OUTTAIL'.");
        var plans=p.path("fileInventory").path("sortPlans");assertEquals(1,plans.size());var plan=plans.get(0);
        assertEquals("KNOWN",plan.path("availability").asText());assertTrue(plan.path("inputs").isEmpty());assertTrue(plan.path("outputs").isEmpty());
        assertEquals(2,plan.path("procedures").size());assertEquals(Set.of("INPUT","OUTPUT"),values(plan.path("procedures"),"phase"));
        for(var procedure:plan.path("procedures")){assertFalse(procedure.path("start").isNull());assertFalse(procedure.path("end").isNull());assertFalse(procedure.path("entry").isNull());assertFalse(procedure.path("completions").isEmpty());assertTrue(procedure.path("gapCodes").isEmpty());}
        assertEquals(5,java.util.stream.StreamSupport.stream(p.path("statements").spliterator(),false).filter(s->s.path("variant").asText().equals("CALL")).count());
        save("procedure-ranges",p);
    }
    @Test void sectionProcedureIncludesItsParagraphsAndPlainExitReturns()throws Exception {
        var p=publish("MAIN SECTION.\nSTART-P.\nSORT S ON ASCENDING KEY SK\n INPUT PROCEDURE FEED GIVING C.\nGOBACK.\nFEED SECTION.\nFEED-ONE.\nCALL 'INPUT'.\nRELEASE SR FROM BUF.\nFEED-TWO.\nEXIT.");
        var procedure=p.path("fileInventory").path("sortPlans").get(0).path("procedures").get(0);
        assertEquals(3,procedure.path("roots").size());assertEquals(1,procedure.path("completions").size());assertTrue(procedure.path("gapCodes").isEmpty());save("section-procedure",p);
    }
    @Test void duplicateMergeAndMissingProcedureRetainLocalizedGaps()throws Exception {
        var merge=publish("MERGE S ON ASCENDING KEY SK USING A A GIVING C.\nGOBACK.");
        assertEquals(4,uses(merge).size());assertTrue(merge.path("fileInventory").path("sortPlans").get(0).path("gapCodes").toString().contains("FILE_MERGE_DUPLICATE_PARTICIPANT"));
        var missing=publish("SORT S ON ASCENDING KEY SK\n INPUT PROCEDURE MISSING-P GIVING C.\nGOBACK.");
        assertEquals("PARTIAL",missing.path("fileInventory").path("sortPlans").get(0).path("availability").asText());
        assertFalse(missing.path("fileInventory").path("sortPlans").get(0).path("procedures").get(0).path("gapCodes").isEmpty());save("missing-procedure",missing);
    }
    @Test void plainExitAndContinueHaveNoMemoryOrEnvironmentEffects()throws Exception {
        var p=publish("CONTINUE.\nEXIT.\nGOBACK.");
        var effects=p.path("statementEffects");assertEquals(2,effects.size());
        for(var e:effects){assertEquals("NO_OP",e.path("proof").asText());assertEquals("NONE",e.path("environment").asText());assertEquals("NONE",e.path("unknownWriteBound").asText());assertEquals("NONE",e.path("unknownReadBound").asText());assertTrue(e.path("knownReads").isEmpty());assertTrue(e.path("mayWrites").isEmpty());}
        assertTrue(publish("EXIT PROGRAM.").path("statementEffects").isEmpty(),"EXIT PROGRAM is a transfer, not NO_OP");
    }
    @Test void sameFdMayHaveBothSortRolesAndMultipleKeysSurvive()throws Exception {
        var p=publish("SORT S ON ASCENDING KEY SK SK2 USING A GIVING A.\nGOBACK.");
        assertEquals(3,uses(p).size());assertEquals(Set.of("WORK","INPUT","OUTPUT"),values(uses(p),"role"));
        assertEquals(2,java.util.stream.StreamSupport.stream(uses(p).spliterator(),false).filter(u->u.path("candidates").get(0).path("id").asText().equals(id(p,"A"))).count());
        for(var u:uses(p))assertEquals(2,u.path("operands").size());
        assertTrue(p.path("fileInventory").path("sortPlans").get(0).path("gapCodes").isEmpty());save("same-file",p);
    }
    @Test void implicitIoUsesTheKnownInputAndOutputModes()throws Exception {
        var p=publish("DECLARATIVES.\nIN-ERROR SECTION.\nUSE AFTER ERROR PROCEDURE ON INPUT.\nIN-PARA.\nCALL 'INERR'.\nOUT-ERROR SECTION.\nUSE AFTER ERROR PROCEDURE ON OUTPUT.\nOUT-PARA.\nCALL 'OUTERR'.\nEND DECLARATIVES.\nMAIN SECTION.\nSTART-P.\nSORT S ON ASCENDING KEY SK USING A B GIVING C.\nGOBACK.");
        assertEquals(2,p.path("fileInventory").path("declaratives").size());
        for(var u:uses(p)) {
            var control=u.path("control");assertEquals("KNOWN",control.path("availability").asText());
            if(u.path("role").asText().equals("WORK"))assertEquals(1,control.path("routes").size());
            else {assertEquals(2,control.path("routes").size());assertEquals("USE",control.path("routes").get(1).path("destinations").get(0).path("kind").asText());}
        }
        save("implicit-use",p);
    }
    @Test void performInsideInputProcedureRetainsTheLocalRange()throws Exception {
        var p=publish("MAIN.\nSORT S ON ASCENDING KEY SK\n INPUT PROCEDURE FEED\n OUTPUT PROCEDURE DRAIN.\nCALL 'AFTER'.\nGOBACK.\nFEED.\nPERFORM FILL THRU FILL-END.\nRELEASE SR.\nDRAIN.\nRETURN S AT END CONTINUE END-RETURN.\nFILL.\nMOVE 'SAFE0001' TO SR.\nFILL-END.\nCALL 'FILLPGM'.\nEXIT.");
        var plan=p.path("fileInventory").path("sortPlans").get(0);assertEquals("KNOWN",plan.path("availability").asText());
        var output=plan.path("procedures").get(1);assertEquals("OUTPUT",output.path("phase").asText());assertEquals(1,output.path("roots").size());assertFalse(output.path("entry").isNull());
        assertEquals(1,java.util.stream.StreamSupport.stream(p.path("statements").spliterator(),false).filter(n->n.path("variant").asText().equals("PERFORM_PROCEDURE")).count());save("perform-procedure",p);
    }
    @Test void emptyInputIsPermittedButEmptyOutputCannotSatisfyRequiredReturn()throws Exception {
        var input=publish("SORT S ON ASCENDING KEY SK\n INPUT PROCEDURE EMPTY-P GIVING C.\nGOBACK.\nEMPTY-P.");
        var valid=input.path("fileInventory").path("sortPlans").get(0);assertEquals("KNOWN",valid.path("availability").asText());assertTrue(valid.path("procedures").get(0).path("roots").isEmpty());
        var output=publish("SORT S ON ASCENDING KEY SK USING A\n OUTPUT PROCEDURE EMPTY-P.\nGOBACK.\nEMPTY-P.");
        var invalid=output.path("fileInventory").path("sortPlans").get(0);assertEquals("PARTIAL",invalid.path("availability").asText());assertTrue(invalid.path("gapCodes").toString().contains("FILE_OUTPUT_PROCEDURE_EMPTY"));
    }
    private static Set<String> values(JsonNode nodes,String key){var values=new HashSet<String>();for(var n:nodes)values.add(n.path(key).asText());return values;}
}
