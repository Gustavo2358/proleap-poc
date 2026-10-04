package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CicsCatalogueTest {
    static final List<String> FORMS=List.of("ASKTIME ABSTIME(CLOCK-X)","ASKTIME", "FORMATTIME ABSTIME(CLOCK-X) YYYYMMDD(AREA-X) DATESEP('-') TIME(AREA-X) TIMESEP", "FORMATTIME ABSTIME(CLOCK-X) YYDDD(AREA-X) MILLISECONDS(RESP-CD)", "ASSIGN APPLID(AREA-X)","ASSIGN SYSID(AREA-X)","INQUIRE PROGRAM(AREA-X)","SEND TEXT FROM(AREA-X) LENGTH(LENGTH OF AREA-X) ERASE FREEKB","WRITEQ TD QUEUE('JOBS') FROM(AREA-X) LENGTH(80)");
    static CicsCommandSemantics.Fact parse(String form){return CicsCommandSemantics.parse("EXEC CICS "+form+" END-EXEC").orElseThrow();}
    @Test void commandsHaveTheirOwnClosedSyntaxAndControl() {
        for(var form:FORMS)for(var response:List.of(""," NOHANDLE"," RESP(RESP-CD)"," NOHANDLE NOHANDLE")) {
            var c=parse(form+response);assertTrue(c.supported(),c.toString());
            var control=CicsCommandControl.qualify(c).orElseThrow();assertFalse(control.programReturn());
            assertEquals(response.isEmpty(),!control.unresolvedConditions().isEmpty());
            assertFalse(parse(form+" INVALID-X(AREA-X)").supported());
        }
        for(var form:List.of("FORMATTIME YYYYMMDD(AREA-X)","ASSIGN", "INQUIRE PROGRAM", "SEND TEXT", "WRITEQ TD QUEUE('JOBS')", "WRITEQ TD FROM(AREA-X)", "ASKTIME ABSTIME(CLOCK-X) ABSTIME(AREA-X)"))assertFalse(parse(form).supported(),form);
        assertTrue(CicsCommandSemantics.parse("EXEC CICS WRITEQ TS QUEUE('Q') FROM(B) END-EXEC").isEmpty());
    }
    @Test void producerPublishesDirectionAndOpenEffects()throws Exception {
        for(var form:FORMS) {
            var p=CicsMemoryLocalityTest.publish("01 CLOCK-X PIC S9(15) COMP-3.\n01 AREA-X PIC X(80).\n01 RESP-CD PIC S9(9) COMP.","EXEC CICS "+form.replace(") ",")\n")+"\nNOHANDLE END-EXEC.\nCALL 'AFTERIO'.\nGOBACK.",false);
            var j=CicsAbendContractTest.json(p);var c=CicsCommandContractTest.command(j);assertEquals("2.65.0",j.path("contractVersion").asText());
            assertEquals("SUPPORTED",c.path("syntaxStatus").asText());assertFalse(c.hasNonNull("hostEffects"),"implicit environment writes remain open");
            for(var o:c.path("options"))if(o.hasNonNull("reference")) {
                var name=o.path("name").asText();boolean write=Set.of("RESP","RESP2","APPLID").contains(name)||form.startsWith("ASSIGN")||form.startsWith("ASKTIME")||form.startsWith("FORMATTIME")&&!Set.of("ABSTIME","DATESEP","TIMESEP").contains(name);
                assertEquals(write?"WRITE":"READ",o.path("reference").path("role").asText(),form+name);
            }
        }
    }
    @Test void isolatedHostMustParseTheCompleteIdentifier()throws Exception {
        for(var ref:List.of("ITEM-X(IDX)","ITEM-X (IDX)","ITEM-X(1:2)","ITEM-X(IDX)(1:2)")) {
            assertTrue(parse("INQUIRE PROGRAM("+ref+") NOHANDLE").supported(),ref);
            var hosts=CicsHostSyntax.parse("EXEC CICS INQUIRE PROGRAM("+ref+") END-EXEC",0,1,0,0);
            assertEquals(1,hosts.size(),ref);assertNotNull(hosts.get(0).identifier().tableCall(),ref);
        }
        assertFalse(parse("INQUIRE PROGRAM(ITEM-X(IDX) GARBAGE) NOHANDLE").supported());
        var p=CicsMemoryLocalityTest.publish("01 G.\n 05 ITEM-X PIC X(8) OCCURS 3.\n01 IDX PIC 9.","EXEC CICS INQUIRE PROGRAM(ITEM-X(IDX)) NOHANDLE\nEND-EXEC.\nGOBACK.",false);
        var c=CicsCommandContractTest.command(CicsAbendContractTest.json(p));var reference=c.path("options").get(0).path("reference");
        assertTrue(reference.isObject());assertTrue(reference.path("logicalWholeItem").isNull(),"element is not a whole table");
    }

}
