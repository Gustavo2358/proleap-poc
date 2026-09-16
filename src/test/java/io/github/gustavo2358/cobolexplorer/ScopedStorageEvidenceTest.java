package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutTest.*;
import static io.github.gustavo2358.cobolexplorer.DeclarativeValueInferenceTest.*;

class ScopedStorageEvidenceTest {
    @Test void unknownAllocationInAnotherUnitCannotEscapeItsDeclaredScope() {
        var source=ScopedInputTest.unit("KNOWN-UNIT",VALUE,"CALL LIT-PGM.")+
            ScopedInputTest.unit("PARTIAL-UNIT","77 PARTIAL-AREA PIC X(3) JUSTIFIED.","CALL 'DIRECT'.");
        var a=AstBoundaryTestSupport.analyze(source,"separate-units.cbl");
        var components=StorageComponents.analyze(a.build());
        assertTrue(components.unit(a.model().programUnits().get(0).id()).uncertainties().isEmpty());
        assertFalse(components.unit(a.model().programUnits().get(1).id()).uncertainties().isEmpty());
        assertEquals("LITERAL_BYTES",ScopedInputTest.product(a,0).storage().entryState().conditions().get(0).kind().name());
        assertEquals(1,ScopedInputTest.product(a,1).calls().size());
    }
    @Test void broadAliasRemainderRetainsItsOwnerAndDoesNotPoisonKnownBounds() {
        var f=fixture(VALUE+"77 PARTIAL-AREA PIC X(3) JUSTIFIED.\n");
        var unit=StorageComponents.analyze(f.source().build()).unit(f.source().model().programUnits().get(0).id());
        int owner=f.view("PARTIAL-AREA").node().node(),target=f.view("LIT-PGM").node().node();
        var assessment=unit.allocation(target);
        assertFalse(assessment.proved());assertTrue(assessment.rootRemainder().isEmpty());
        assertTrue(assessment.unitRemainder().stream().allMatch(u->u.owner()==owner&&u.root()==owner&&u.origin().exact()));
        assertTrue(assessment.unitRemainder().stream().allMatch(u->u.scope()==StorageComponents.UncertaintyScope.UNIT&&u.dimensions().contains(StorageComponents.Dimension.ALIAS)));
        assertTrue(unit.uncertainties().stream().anyMatch(u->u.scope()==StorageComponents.UncertaintyScope.DECLARATION&&u.dimensions().equals(Set.of(StorageComponents.Dimension.LAYOUT))));
        known(0,f.view("LIT-PGM").offset());known(8,f.view("LIT-PGM").extent());
        assertTrue(f.view("PARTIAL-AREA").extent().value().isEmpty());
        var value=condition(product(VALUE+"77 PARTIAL-AREA PIC X(3) JUSTIFIED.\n","CALL LIT-PGM."));
        assertEquals("POSSIBLE_LITERAL_BYTES",value.kind().name());assertFalse(value.bytes().isEmpty());
    }
    @Test void nestedUnknownRelationIsRecordScopedAndDoesNotInventEndpoints() {
        var f=fixture(VALUE+"01 PARTIAL-AREA.\n05 KNOWN-PART PIC X(3).\n05 UNKNOWN-PART REDEFINES MISSING PIC X(3).\n");
        var unit=StorageComponents.analyze(f.source().build()).unit(f.source().model().programUnits().get(0).id());
        int root=f.view("PARTIAL-AREA").node().node();
        assertTrue(unit.uncertainties().stream().allMatch(u->u.scope()==StorageComponents.UncertaintyScope.RECORD&&u.root()==root));
        assertTrue(unit.allocation(f.view("LIT-PGM").node().node()).proved());
        assertTrue(f.view("UNKNOWN-PART").offset().value().isEmpty());
        known(8,f.view("LIT-PGM").extent());
    }
    @Test void declarationNamesAndOrderDoNotCreateSeparationProof() {
        for(String partial:List.of("A-FIRST","Z-LAST"))for(boolean before:List.of(false,true)) {
            String declaration="77 "+partial+" PIC X(3) JUSTIFIED.\n";
            var f=fixture(before?declaration+VALUE:VALUE+declaration);
            var unit=StorageComponents.analyze(f.source().build()).unit(f.source().model().programUnits().get(0).id());
            assertFalse(unit.allocation(f.view("LIT-PGM").node().node()).proved());
            assertFalse(f.layout().bases().stream().anyMatch(StorageLayoutSemantics.Base::independent));
            assertFalse(condition(product(before?declaration+VALUE:VALUE+declaration,"CALL LIT-PGM.")).bytes().isEmpty());
        }
    }
}
