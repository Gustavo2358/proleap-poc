package io.github.gustavo2358.cobolexplorer;

import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutTest.*;

/** W1: omitted representation aspects retain the supported textual projection. */
class PositiveMemoryTopologyTest {
    @Test void representationDiagnosticsDoNotRemoveIndependentOrFamilyViews() {
        for(var clause:List.of("SYNC", "JUSTIFIED RIGHT")) {
            String data="01 SRC.\n05 PREFIX-A PIC X(3).\n05 SUFFIX-A PIC X(5) "+clause+".\n01 DST.\n05 FIRST-A PIC X(4).\n05 LAST-A PIC X(4).\n01 OTHER-A PIC X(2) "+clause+".";
            var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data,"MOVE 'ABCDEFGH' TO SRC.\nMOVE SRC TO DST.\nMOVE 'ZZZ' TO PREFIX-A.\nCALL LAST-A."),"positive.cbl");
            var components=StorageComponents.analyze(a.build(),a.tables(),a.resolution());
            var unit=a.model().programUnits().get(0).id();
            assertTrue(components.unit(unit).roots().stream().allMatch(r->components.unit(unit).allocation(r.meta().id()).proved()));
            assertEquals(2,components.unit(unit).uncertainties().size());
            assertTrue(components.unit(unit).uncertainties().stream().allMatch(u->u.scope()==StorageComponents.UncertaintyScope.DECLARATION&&u.dimensions().equals(Set.of(StorageComponents.Dimension.LAYOUT))));
            var logical=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.UNSPECIFIED,components,true);
            assertEquals(7,logical.logicalViews().size());
            assertEquals(List.of(8,3,5,8,4,4,2),logical.logicalViews().stream().map(v->v.length().intValueExact()).toList());
            assertEquals(List.of(0,0,3,0,0,4,0),logical.logicalViews().stream().map(v->v.start().intValueExact()).toList());
            var physical=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,components);
            assertEquals(3,physical.layout(unit).bases().size());
            assertTrue(physical.layout(unit).views().stream().allMatch(StorageLayoutSemantics.View::textual));
            var effects=StorageAccessSemantics.analyze(a.build(),a.resolution(),physical);
            assertEquals(3,effects.moves().size());
            assertEquals(1,effects.moves().stream().filter(m->m.kind()==StorageAccessSemantics.MoveKind.COPY_BYTES).count());
            var copy=effects.moves().stream().filter(m->m.kind()==StorageAccessSemantics.MoveKind.COPY_BYTES).findFirst().orElseThrow();
            assertEquals(BigInteger.valueOf(8),copy.source().orElseThrow().view().extent().value().orElseThrow());
            assertEquals(BigInteger.valueOf(8),copy.destination().orElseThrow().view().extent().value().orElseThrow());
            assertTrue(a.build().coverageByProgramUnit().get(unit).findings().stream().anyMatch(f->f.coverage()==SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED));
        }
    }
    @Test void partialRepresentationKeepsSupportedOverlaysAndRenames() {
        var f=fixture("01 RAW-A PIC X(8) SYNC.\n01 VIEW-A REDEFINES RAW-A.\n05 PREFIX-A PIC X(3).\n05 TAIL-A PIC X(5) JUSTIFIED.\n66 RANGE-A RENAMES PREFIX-A THRU TAIL-A.");
        assertEquals(1,f.layout().bases().size());known(8,f.layout().bases().get(0).extent());
        assertEquals(f.view("RAW-A").base(),f.view("TAIL-A").base());known(3,f.view("TAIL-A").offset());
        assertTrue(f.layout().renames().stream().allMatch(StorageLayoutSemantics.Renaming::proved));
    }
    @Test void omittedAspectKeepsScalarTransferAndNominalCall() {
        var product=ScalarMoveCheckpoint4ATest.publish(ScalarMoveCheckpoint4ATest.program(
            "01 PGM-A PIC X(5) JUSTIFIED RIGHT.", "MOVE 'PROGA' TO PGM-A.\nCALL PGM-A."));
        assertTrue(product.dataDeclarations().get(0).scalarText().isPresent());
        assertTrue(product.moves().get(0).target().wholeItemAccess().isPresent());
        assertEquals("FULL_IDENTITY",product.moves().get(0).copySemantics().name());
        assertEquals(1,product.calls().size());
    }
    @Test void unknownNumericRepresentationDoesNotInventAWidth() {
        var f=fixture("01 UNKNOWN-A PIC 9(8) COMP SYNC.\n01 TEXT-A PIC X(8).");
        assertTrue(f.view("UNKNOWN-A").extent().value().isEmpty());known(8,f.view("TEXT-A").extent());
        assertTrue(f.layout().bases().stream().allMatch(StorageLayoutSemantics.Base::independent));
    }
}
