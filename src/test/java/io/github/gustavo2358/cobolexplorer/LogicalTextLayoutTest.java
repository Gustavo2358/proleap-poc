package io.github.gustavo2358.cobolexplorer;

import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogicalTextLayoutTest {
    private static StorageLayoutSemantics analyze(String data) {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data,"GOBACK."),"logical.cbl");
        return StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),
            StorageLayoutSemantics.Profile.UNSPECIFIED,StorageComponents.analyze(a.build(),a.tables(),a.resolution()),true);
    }
    @Test void logicalCoordinatesDoNotGrantPhysicalCoordinates() {
        var p=analyze("01 AREA-A.\n05 FIRST-A PIC X(8).\n05 LAST-A PIC X(4).");
        var views=p.logicalViews();
        assertEquals(List.of(BigInteger.ZERO,BigInteger.ZERO,BigInteger.valueOf(8)),views.stream().map(StorageLayoutSemantics.LogicalView::start).toList());
        assertEquals(List.of(BigInteger.valueOf(12),BigInteger.valueOf(8),BigInteger.valueOf(4)),views.stream().map(StorageLayoutSemantics.LogicalView::length).toList());
        var physical=p.layout(views.get(0).node().unit());
        assertEquals(StorageLayoutSemantics.Profile.UNSPECIFIED,physical.profile());
        assertTrue(physical.nodes().stream().allMatch(n->n.extent().value().isEmpty()));
        assertTrue(physical.views().stream().noneMatch(StorageLayoutSemantics.View::textual));
        assertEquals(1,views.stream().map(StorageLayoutSemantics.LogicalView::root).distinct().count());
    }
    @Test void unsupportedRepresentationRefusesAffectedRoot() {
        for(var data:List.of("01 AREA-A.\n05 A PIC X(8).\n05 B PIC 9(4) COMP.",
            "01 AREA-A.\n05 A PIC X(8) OCCURS 2 TIMES."))assertTrue(analyze(data).logicalViews().isEmpty(),data);
    }
    @Test void overlaysShareCharacterRangesWithoutPhysicalAuthority() {
        var p=analyze("01 RAW-A PIC X(12).\n01 VIEW-A REDEFINES RAW-A.\n05 PREFIX-A PIC X(4).\n05 PGM-A PIC X(8).");
        assertEquals(4,p.logicalViews().size());assertEquals(1,p.logicalViews().stream().map(StorageLayoutSemantics.LogicalView::root).distinct().count());
        assertEquals(BigInteger.valueOf(4),p.logicalViews().get(3).start());
        assertEquals(BigInteger.valueOf(8),p.logicalViews().get(3).length());
        assertTrue(p.layout(p.logicalViews().get(0).node().unit()).views().stream().noneMatch(StorageLayoutSemantics.View::textual));
    }
    @Test void renamesIncludesFillerCoordinates() {
        var p=analyze("01 REC-A.\n05 PART-A PIC X(2).\n05 FILLER PIC X(2).\n05 PART-B PIC X(4).\n66 RANGE-A RENAMES PART-A THRU PART-B.");
        assertEquals(5,p.logicalViews().size());var range=p.logicalViews().get(p.logicalViews().size()-1);
        assertEquals(BigInteger.ZERO,range.start());assertEquals(BigInteger.valueOf(8),range.length());
    }
    @Test void invalidRenamesDoesNotPublishRange() {
        var p=analyze("01 REC-A.\n05 PART-A PIC X(4).\n05 PART-B PIC X(4).\n66 RANGE-A RENAMES PART-B THRU PART-A.");
        assertEquals(3,p.logicalViews().size());
    }
    @Test void valueAndFillerKeepStructuralPositions() {
        var p=analyze("01 AREA-A.\n05 FILLER PIC X(2).\n05 A PIC X(8) VALUE 'PROGA'.");
        assertEquals(3,p.logicalViews().size());
        assertEquals(BigInteger.valueOf(2),p.logicalViews().get(p.logicalViews().size()-1).start());
    }
}
