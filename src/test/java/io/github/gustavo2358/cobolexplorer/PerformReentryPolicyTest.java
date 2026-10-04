package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PerformReentryPolicyTest {
    @Test void producerPublishesSourceReentryPrecondition()throws Exception {
        var j=CobolControlCompletionTest.publish("01 FLAG-X PIC X.","PERFORM P\nGOBACK.\nP.\nIF FLAG-X = 'Y' PERFORM P END-IF.");
        assertEquals("2.65.0",j.path("contractVersion").asText());
        assertEquals(2,j.path("controlTopology").path("bindings").size());
        for(var b:j.path("controlTopology").path("bindings"))assertEquals("SOURCE_UNDEFINED",b.path("reentryPolicy").asText());
    }
    @Test void sequentialCallsKeepSeparateBindingsWithSameTarget()throws Exception {
        var j=CobolControlCompletionTest.publish("","PERFORM P\nPERFORM P\nGOBACK.\nP.\nCONTINUE.");
        var bs=j.path("controlTopology").path("bindings");assertEquals(2,bs.size());
        assertNotEquals(bs.get(0).path("id"),bs.get(1).path("id"));assertEquals(bs.get(0).path("endpoint"),bs.get(1).path("endpoint"));
        for(var b:bs)assertEquals("SOURCE_UNDEFINED",b.path("reentryPolicy").asText());
    }
}
