package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DatabaseControlTest {
    static final List<String> SQL=List.of("SELECT COUNT(1) INTO :OUT-X FROM S.T WHERE ((:FLAG-X = '1' AND C = :IN-X) OR :FLAG-X <> '1') AND D LIKE TRIM(:IN-X)","UPDATE S.T SET C = :IN-X, D = CURRENT DATE WHERE C = :IN-X", "INSERT INTO S.T (C,D) VALUES (:IN-X,TIMESTAMP_FORMAT(:IN-X, 'YY-MM-DD'))", "DELETE FROM S.T WHERE C = :IN-X", "OPEN CURSOR-X", "FETCH CURSOR-X INTO :OUT-X", "CLOSE CURSOR-X");
    @Test void sqlClosedFormsRetainNormalAndFailure()throws Exception {
        for(var sql:SQL) {
            assertTrue(SqlCommandSyntax.parse("EXEC SQL "+sql+" END-EXEC").isPresent(),sql);
            var p=CicsMemoryLocalityTest.publish("01 IN-X PIC X(20).\n01 OUT-X PIC X(20).\n01 FLAG-X PIC X.","EXEC SQL\n"+sql.replace(" WHERE ","\nWHERE ").replace(" INTO ","\nINTO ").replace(" SET ","\nSET ").replace(" VALUES ","\nVALUES ").replace(" AND ","\nAND ").replace(" OR ","\nOR ")+"\nEND-EXEC.\nCALL 'AFTERIO'.\nGOBACK.",false);
            var j=CicsAbendContractTest.json(p);assertEquals("2.65.0",j.path("contractVersion").asText());
            var statement=j.path("statements").get(0);var o=CicsCommandContractTest.outcomes(j,statement);
            assertTrue(o.stream().anyMatch(x->x.path("kind").asText().equals("NORMAL")),sql);
            assertTrue(o.stream().anyMatch(x->x.path("kind").asText().equals("UNKNOWN_LOCAL")),sql);
            var e=j.path("statementEffects").get(0);assertEquals("SQL_HOST_OPERANDS",e.path("proof").asText());assertEquals("ALL",e.path("unknownWriteBound").asText());assertTrue(e.path("mustOverwrite").isEmpty());
        }
    }
    @Test void dliUpdatesAndCheckpointAreBoundedSyntax() {
        for(var dli:List.of("CHKP ID(AREA-X)","CHKP ID('CP000001')","REPL USING PCB(PCB-N) SEGMENT(SEG1) FROM(AREA-X)","DLET USING PCB(PCB-N) SEGMENT(SEG1) FROM(AREA-X)","ISRT USING PCB(PCB-N) SEGMENT(PARENT) WHERE(KEY1 = KEY-X) SEGMENT(CHILD) FROM(AREA-X) SEGLENGTH(LENGTH OF AREA-X)"))assertTrue(DliCommandSyntax.parse("EXEC DLI "+dli+" END-EXEC").isPresent(),dli);
        for(var dli:List.of("CHKP", "CHKP ID()", "CHKP ID(AREA-X) MYSTERY", "REPL SEGMENT(SEG1)","ISRT SEGMENT(SEG1) FROM(AREA-X) WHERE(KEY1 = KEY-X)","DLET SEGMENT(SEG1) FROM(AREA-X) FROM(OTHER-X)"))assertTrue(DliCommandSyntax.parse("EXEC DLI "+dli+" END-EXEC").isEmpty(),dli);
    }
    @Test void sqlDeclarationDoesNotAllocateHostStorage() {
        String decl="EXEC SQL DECLARE CURSOR-X CURSOR FOR\n SELECT C FROM T WHERE C = :TARGET ORDER BY C DESC\n END-EXEC.\n";
        for(var data:List.of(decl+"01 TARGET PIC X(8).","01 TARGET PIC X(8).\n"+decl))assertTrue(FactDependencyLocalityTest.known(CicsMemoryLocalityTest.publish(data,"CALL TARGET.\nGOBACK.",false),"TARGET",io.github.gustavo2358.cobolexplorer.semanticproduct.FactDependencies.FactKind.LOCAL_CELL));
    }
}
