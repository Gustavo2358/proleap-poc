package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SqlNormalCompletionTest {
    private static final String DATA="01 NAME-A PIC X(8) VALUE 'PROGA001'.\n01 RESULT-A PIC 9(4).";
    @Test void successfulSelectHasConditionalOrdinaryCompletionAndOpenFailures() throws Exception {
        var j=LogicalInitialInvariantTest.publish(DATA,"EXEC SQL SELECT 7 INTO :RESULT-A FROM APP.TABLE_A\n FETCH FIRST 2 ROWS ONLY END-EXEC.\nCALL NAME-A.\nGOBACK.");
        assertTrue(j.path("controlTopology").toString().contains("db2-select-into-successful-return"));
        assertTrue(j.path("controlTopology").toString().contains("db2-select-into-other-outcomes"));
        var out=java.nio.file.Path.of("target/recall-cics");java.nio.file.Files.createDirectories(out);
        java.nio.file.Files.writeString(out.resolve("sql-normal-completion.json"),j.toPrettyString());
    }
    @Test void missingCodeAndUnsupportedSqlDoNotAcquireOrdinaryCompletion() throws Exception {
        for(var code:java.util.List.of("EXEC SQL SELECT 7 INTO :RESULT-A FROM APP.TABLE_A\n WHERE MYSTERY() END-EXEC.","COPY MISSING-CODE.\nEXEC SQL SELECT 7 INTO :RESULT-A FROM TABLE_A END-EXEC.")) {
            var j=LogicalInitialInvariantTest.publish(DATA,code+"\nCALL NAME-A.\nGOBACK.");
            assertFalse(j.path("controlTopology").toString().contains("db2-select-into-successful-return"));
        }
    }
    @Test void fullPayloadAndCardinalityAreRequired() {
        for(var sql:java.util.List.of("SELECT A, 23, 'x''y' INTO :OUT-A, :OUT-B, :OUT-C FROM S.T", "SELECT C INTO :OUT-A FROM T FETCH FIRST 1 ROW ONLY"))
            assertTrue(SqlNormalCompletion.selectInto("EXEC SQL "+sql+" END-EXEC."),sql);
        for(var sql:java.util.List.of("SELECT A INTO :OUT-A, :OUT-B FROM T", "SELECT X() INTO :OUT-A FROM T", "SELECT A INTO :OUT-A FROM T FETCH FIRST 0 ROWS ONLY", "SELECT A INTO :OUT-A FROM T END-EXEC EXEC SQL DROP TABLE T", "SELECT 'unfinished INTO :OUT-A FROM T", "SELECT A INTO :OUT-A FROM T; DELETE FROM T", "SELECT A INTO :OUT-A FROM T WHERE A = 2"))
            assertFalse(SqlNormalCompletion.selectInto("EXEC SQL "+sql+" END-EXEC"),sql);
    }
}
