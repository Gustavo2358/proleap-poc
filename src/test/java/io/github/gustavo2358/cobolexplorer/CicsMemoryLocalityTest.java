package io.github.gustavo2358.cobolexplorer;
import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CicsMemoryLocalityTest {
    static CobolSemanticPort publish(String data,String body,boolean initial) {
        var text=ScalarMoveCheckpoint4ATest.program(data,body);if(initial)text=text.replace("PROGRAM-ID. SAMPLE.","PROGRAM-ID. SAMPLE IS INITIAL.");
        var a=AstBoundaryTestSupport.analyze(text,"memory-locality.cbl");return EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED);
    }
    @Test void initialDoesNotRemoveAllocation() {
        var p=publish("01 TARGET PIC X(8).","MOVE 'PGMA' TO TARGET.\nCALL TARGET.\nGOBACK.",true);
        assertTrue(FactDependencyLocalityTest.known(p,"TARGET",FactDependencies.FactKind.LOCAL_CELL));
        assertFalse(FactDependencyLocalityTest.known(p,"TARGET",FactDependencies.FactKind.PHYSICAL_VIEW));
    }
    @Test void sqlTableMetadataIsNotAnUnknownStorageInsertion() {
        String decl="EXEC SQL DECLARE SCHEMA.T TABLE\n (C CHAR(8) NOT NULL, N DECIMAL(9,2)) END-EXEC.\n";
        for(var data:List.of(decl+"01 TARGET PIC X(8).","01 TARGET PIC X(8).\n"+decl+"01 END-MARK PIC X.")) {
            var p=publish(data,"CALL TARGET.\nGOBACK.",false);
            assertTrue(FactDependencyLocalityTest.known(p,"TARGET",FactDependencies.FactKind.LOCAL_CELL));
            assertTrue(p.factDependencies().orElseThrow().inputs().stream().noneMatch(i->i.kind()==FactDependencies.InputKind.OPAQUE_INCLUDE));
        }
        var missing=publish("EXEC SQL INCLUDE MISSING END-EXEC.\n01 TARGET PIC X(8).","CALL TARGET.\nGOBACK.",false);
        assertFalse(FactDependencyLocalityTest.known(missing,"TARGET",FactDependencies.FactKind.LOCAL_CELL));
    }
    @Test void implicitBmsUsesCanonicalResolverAndSeparateSourceOrigin()throws Exception {
        for(var command:List.of("RECEIVE","SEND")) {
            String name="MAPA"+(command.equals("RECEIVE")?"I":"O");
            var p=publish("01 "+name+" PIC X(80).","EXEC CICS "+command+" MAP('MAPA') NOHANDLE END-EXEC.\nGOBACK.",false);
            var j=CicsAbendContractTest.json(p);var c=CicsCommandContractTest.command(j);
            assertEquals("2.65.0",j.path("contractVersion").asText());
            assertTrue(c.hasNonNull("implicitArea"));assertTrue(c.hasNonNull("hostEffects"));
            assertEquals(command.equals("RECEIVE")?"WRITE":"READ",c.path("implicitArea").path("role").asText());
            assertFalse(c.path("implicitArea").path("provenance").path("exact").asBoolean());
            assertEquals(2,c.path("options").size(),"do not invent written INTO/FROM");
        }
        for(var data:List.of("01 UNRELATED PIC X.","01 G1.\n 05 MAPAI PIC X.\n01 G2.\n 05 MAPAI PIC X.")) {
            var c=CicsCommandContractTest.command(CicsAbendContractTest.json(publish(data,"EXEC CICS RECEIVE MAP('MAPA') NOHANDLE END-EXEC.\nGOBACK.",false)));
            assertFalse(c.hasNonNull("implicitArea"));assertFalse(c.hasNonNull("hostEffects"));
        }
        var explicit=CicsCommandContractTest.command(CicsAbendContractTest.json(publish("01 G1.\n 05 MAPAI PIC X(80).","EXEC CICS RECEIVE MAP('MAPA')\n INTO(MAPAI OF G1) NOHANDLE END-EXEC.\nGOBACK.",false)));
        assertFalse(explicit.hasNonNull("implicitArea"));assertTrue(explicit.hasNonNull("hostEffects"));
    }
}
