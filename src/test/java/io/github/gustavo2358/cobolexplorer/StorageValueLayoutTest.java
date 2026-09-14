package io.github.gustavo2358.cobolexplorer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageAccessTest.fixture;
class StorageValueLayoutTest {
    @Test void valueDoesNotMakeFixedStorageExtentOrSubsequentWritesUnknown() {
        var f=fixture("01 WS-AREA.\n05 PGM-TEXT PIC X(8) VALUE 'PGM00001'.\n05 UNKNOWN-TEXT PIC X(2).","MOVE 'OTHERPGM' TO PGM-TEXT.\nCALL PGM-TEXT.");
        var layout=f.effects().layout().layout(f.source().model().programUnits().get(0).id());
        assertEquals(java.math.BigInteger.TEN,layout.bases().get(0).extent().value().orElseThrow());
        assertEquals(StorageAccessSemantics.MoveKind.LITERAL_BYTES,f.effects().moves().iterator().next().kind());
    }
}
