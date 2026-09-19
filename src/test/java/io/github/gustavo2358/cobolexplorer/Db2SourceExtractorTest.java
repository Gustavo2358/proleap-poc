package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class Db2SourceExtractorTest {
    private void tables(String sql,String... expected) {
        var r=Db2SourceExtractor.extract(sql);
        assertEquals(List.of(),r.gaps(),sql);
        assertEquals(List.of(expected),r.tables().stream().map(t->(t.schema().isEmpty()?"":t.schema()+".")+t.name()+":"+t.operation()+":"+t.access()).toList(),sql);
    }
    private void gap(String sql) {var r=Db2SourceExtractor.extract(sql);assertTrue(r.tables().isEmpty(),sql);assertFalse(r.gaps().isEmpty(),sql);}
    @Test void selectJoinQualifiedAliasesAndNeutralText() {
        tables("select c.id from dbprod.cliente c left join conta x on x.id=c.id where c.txt='FROM FAKE' and c.id=:WS-ID -- FROM BAD\n", "DBPROD.CLIENTE:SELECT:READ","CONTA:SELECT:READ");
        tables("SELECT * FROM A JOIN B ON A.X=B.X LEFT JOIN C ON C.X=A.X INNER JOIN D ON D.X=A.X", "A:SELECT:READ","B:SELECT:READ","C:SELECT:READ","D:SELECT:READ");
        tables("SELECT * /* FROM BAD */ FROM A, B", "A:SELECT:READ","B:SELECT:READ");
    }
    @Test void writesAndSubqueries() {
        tables("INSERT INTO DESTINO (ID) SELECT ID FROM ORIGEM","DESTINO:INSERT:WRITE","ORIGEM:SELECT:READ");
        tables("INSERT INTO CLIENTE(ID) VALUES(:ID)","CLIENTE:INSERT:WRITE");
        tables("UPDATE CONTA SET SALDO=:SALDO WHERE ID IN (SELECT ID FROM CLIENTE)","CONTA:UPDATE:WRITE","CLIENTE:SELECT:READ");
        tables("DELETE FROM HISTORICO WHERE ID IN (SELECT ID FROM CLIENTE)","HISTORICO:DELETE:WRITE","CLIENTE:SELECT:READ");
        tables("MERGE INTO CONTA C USING AJUSTE A ON C.ID=A.ID WHEN MATCHED THEN UPDATE SET X=A.X", "CONTA:MERGE:READ_WRITE","AJUSTE:MERGE:READ");
    }
    @Test void scopedCtesDerivedUnionAndCursor() {
        tables("WITH A AS (SELECT * FROM CLIENTE), B AS (SELECT * FROM A) SELECT * FROM B", "CLIENTE:SELECT:READ");
        tables("SELECT * FROM (SELECT * FROM CLIENTE) X JOIN CONTA C ON X.ID=C.ID", "CLIENTE:SELECT:READ","CONTA:SELECT:READ");
        tables("SELECT * FROM A UNION ALL SELECT * FROM B", "A:SELECT:READ","B:SELECT:READ");
        tables("DECLARE C1 CURSOR FOR SELECT * FROM CLIENTE", "CLIENTE:SELECT:READ");
        tables("SELECT * FROM A", "A:SELECT:READ");
    }
    @Test void unsupportedAndMalformedNeverFabricate() {
        for(var sql:List.of("SELECT FROM A","SELECT * FROM", "SELECT * FROM :HOST", "SELECT * FROM TABLE(F()) X", "SELECT * FROM A WHERE", "SELECT * FROM A JOIN", "SELECT * FROM (VALUES 1) X", "SELECT * FROM \"Mixed\"", "SELECT * FROM A; DELETE FROM B", "SELECT * FROM A /*", "WITH A AS (SELECT * FROM A) SELECT * FROM A"))gap(sql);
    }
    @Test void dynamicRemainder() {
        for(var s:List.of("PREPARE S1 FROM :WS-SQL","EXECUTE IMMEDIATE :WS-SQL","EXECUTE IMMEDIATE 'SELECT * FROM FAKE'")) {
            var r=Db2SourceExtractor.extract(s);assertTrue(r.tables().isEmpty());assertEquals(List.of("DYNAMIC_SQL_NOT_ANALYZED"),r.gaps());
        }
    }
    @Test void statementStateDoesNotLeakAndWideInventoryIsIndexed() {
        for(int i=0;i<1000;i++)tables("SELECT * FROM T"+i,"T"+i+":SELECT:READ");
        tables("WITH A AS (SELECT * FROM REALTABLE) SELECT * FROM A","REALTABLE:SELECT:READ");
        tables("SELECT * FROM A","A:SELECT:READ");
        gap("SELECT * FROM REALTABLE /* /* */ JOIN FAKE ON 1=1 */");gap("COMMIT; SELECT * FROM HIDDEN");
        gap("");gap("/* FROM FAKE */");gap("BEGIN ATOMIC SELECT * FROM HIDDEN END");
        tables("BEGIN DECLARE SECTION");tables("END DECLARE SECTION");
        gap("WITH RECURSIVE A AS (SELECT * FROM A) SELECT * FROM A");
        gap("SELECT * FROM A JOIN (SELECT * FROM B) X ON");
    }
}
