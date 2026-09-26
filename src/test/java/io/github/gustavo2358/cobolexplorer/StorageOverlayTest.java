package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutTest.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Goldens from IBM z/OS 6.4 allocation rules, independent of the layout algorithm. */
class StorageOverlayTest {
    @Test void rootChainUsesOneAllocationAndMaximumExtent() {
        var f=fixture("01 RAW-AREA PIC X(4).\n01 WIDE-AREA REDEFINES RAW-AREA PIC X(12).\n01 LAST-AREA REDEFINES WIDE-AREA PIC X(8).\n01 SAFE-AREA PIC X(3).");
        assertEquals(2,f.layout().bases().size());
        for(var name:List.of("RAW-AREA","WIDE-AREA","LAST-AREA")) {
            assertEquals(f.view("RAW-AREA").base(),f.view(name).base());known(0,f.view(name).offset());assertTrue(f.view(name).textual());
        }
        known(4,f.view("RAW-AREA").extent());known(12,f.view("WIDE-AREA").extent());known(8,f.view("LAST-AREA").extent());
        known(12,f.layout().bases().get(0).extent());assertTrue(f.layout().bases().stream().allMatch(Base::independent));
        assertNotEquals(f.view("RAW-AREA").base(),f.view("SAFE-AREA").base());
    }
    @Test void nestedLargerAlternativeSetsFollowingOffsetWithoutAddingAnotherArea() {
        var f=fixture("01 WS-AREA.\n05 PREFIX-PART PIC X(2).\n05 RAW-PART PIC X(4).\n05 VIEW-PART REDEFINES RAW-PART.\n10 FIRST-PART PIC X(3).\n10 SECOND-PART PIC X(5).\n05 TAIL-PART PIC X(3).");
        known(13,f.view("WS-AREA").extent());known(2,f.view("RAW-PART").offset());known(2,f.view("VIEW-PART").offset());
        known(2,f.view("FIRST-PART").offset());known(5,f.view("SECOND-PART").offset());known(10,f.view("TAIL-PART").offset());
        assertTrue(f.layout().views().stream().allMatch(v->v.base().equals(f.view("WS-AREA").base())));
    }
    @Test void fillerRedefinerHasPhysicalIdentityAndCanContainNamedViews() {
        var f=fixture("01 WS-AREA.\n05 PREFIX-PART PIC X(2).\n05 RAW-PART PIC X(4).\n05 FILLER REDEFINES RAW-PART.\n10 CHILD-PART PIC X(8).\n05 TAIL-PART PIC X.");
        var filler=f.layout().nodes().stream().filter(Node::filler).findFirst().orElseThrow();assertTrue(filler.entity().isEmpty());
        known(8,filler.extent());known(2,f.view("CHILD-PART").offset());known(10,f.view("TAIL-PART").offset());known(11,f.view("WS-AREA").extent());
        var view=f.layout().views().stream().filter(v->v.node().equals(filler.id())).findFirst().orElseThrow();
        known(2,view.offset());assertEquals(f.view("RAW-PART").base(),view.base());
    }
    @Test void smallerRedefinitionAndLateAlternativeNeverCreateFalseIndependence() {
        var f=fixture("01 RAW-AREA PIC X(12).\n01 SHORT-AREA REDEFINES RAW-AREA PIC X(4).\n01 OTHER-AREA REDEFINES RAW-AREA PIC X(8).");
        assertEquals(1,f.layout().bases().size());known(12,f.layout().bases().get(0).extent());
        assertTrue(f.layout().views().stream().allMatch(v->v.base().equals(f.view("RAW-AREA").base())));
    }
    @Test void unknownAlternativeExtentDoesNotBecomeZeroOrKnownFollowingOffset() {
        var f=fixture("01 WS-AREA.\n05 PREFIX-PART PIC X(2).\n05 RAW-PART PIC X(4).\n05 OPAQUE-PART REDEFINES RAW-PART PIC 9(8).\n05 TAIL-PART PIC X(3).\n01 SAFE-AREA PIC X(8).");
        known(0,f.view("PREFIX-PART").offset());known(2,f.view("RAW-PART").offset());known(2,f.view("OPAQUE-PART").offset());
        assertTrue(f.view("WS-AREA").extent().value().isEmpty());assertTrue(f.view("TAIL-PART").offset().value().isEmpty());
        known(8,f.view("SAFE-AREA").extent());assertTrue(f.layout().bases().stream().allMatch(Base::independent));
    }
    @Test void invalidOrUnprovedRelationsDoNotPublishIndependentAllocations() {
        for(var data:List.of(
                "01 RAW-AREA PIC X(8).\n01 OTHER-AREA PIC X(8).\n01 VIEW-AREA REDEFINES RAW-AREA PIC X(8).",
                "01 VIEW-AREA REDEFINES RAW-AREA PIC X(8).\n01 RAW-AREA PIC X(8).",
                "01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES MISSING-AREA PIC X(8).",
                "01 RAW-AREA.\n05 CHILD-PART PIC X(8).\n01 VIEW-AREA REDEFINES CHILD-PART PIC X(8).")) {
            var f=fixture(data);assertTrue(f.layout().reasons().contains(Reason.OVERLAY_NOT_PROVEN),data);
            assertTrue(f.layout().bases().stream().noneMatch(Base::independent),data);
            assertTrue(f.layout().views().stream().noneMatch(View::textual),data);
        }
    }
    @Test void structuralScopeKeepsRepeatedNamesInSeparateComponents() {
        var f=fixture("01 FIRST-AREA.\n05 RAW-PART PIC X(3).\n05 VIEW-PART REDEFINES RAW-PART PIC X(5).\n01 SECOND-AREA.\n05 RAW-PART PIC X(7).\n05 VIEW-PART REDEFINES RAW-PART PIC X(9).");
        known(5,f.view("FIRST-AREA").extent());known(9,f.view("SECOND-AREA").extent());assertEquals(2,f.layout().bases().size());
    }
    @Test void arbitraryMultiplicityAndRenamingPreserveMaxFootprint() {
        for(int n:new int[]{1,2,5,40}) {
            var source=new StringBuilder("01 WS-AREA.\n05 PREFIX-PART PIC X(2).\n05 ITEM-0 PIC X.\n");
            for(int i=1;i<=n;i++)source.append("05 ITEM-").append(i).append(" REDEFINES ITEM-").append(i-1).append(" PIC X(").append(i+1).append(").\n");
            source.append("05 TAIL-PART PIC X.");
            var f=fixture(source.toString());var renamed=fixture(source.toString().replace("ITEM-","RENAMED-"));
            known(n+4,f.view("WS-AREA").extent());known(n+3,f.view("TAIL-PART").offset());
            for(int i=0;i<=n;i++){known(2,f.view("ITEM-"+i).offset());assertEquals(f.view("ITEM-"+i).offset(),renamed.view("RENAMED-"+i).offset());}
            assertEquals(0L,f.product().metrics().get("objectPairs"));
        }
    }
    @Test void relationOriginAndOwnerArePreservedWithoutNominalOwner() {
        var f=fixture("01 WS-AREA.\n05 RAW-PART PIC X(4).\n05 FILLER REDEFINES RAW-PART PIC X(8).");
        var product=StorageComponents.analyze(f.source().build());var physical=product.unit(f.source().model().programUnits().get(0).id());
        assertEquals(1,physical.relations().size());var relation=physical.relations().get(0);assertTrue(relation.proved());
        assertEquals(f.view("RAW-PART").node().node(),relation.target().orElseThrow());
        var owner=f.layout().nodes().stream().filter(Node::filler).findFirst().orElseThrow();assertEquals(owner.id().node(),relation.owner());
        var clause=AstBoundaryTestSupport.nodes(f.source(),Ast.RedefinesClause.class).get(0);
        assertSame(clause,relation.clause());assertEquals(clause.meta().provenance(),relation.clause().meta().provenance());
        assertThrows(UnsupportedOperationException.class,()->physical.relations().clear());
        assertThrows(UnsupportedOperationException.class,()->physical.componentOf().clear());
    }
    @Test void distinctNumericLevelsUseProvedSiblingHierarchy() {
        var f=fixture("01 WS-AREA.\n05 RAW-PART PIC X(4).\n04 OTHER-PART REDEFINES RAW-PART PIC X(8).\n04 TAIL-PART PIC X(2).");
        known(10,f.view("WS-AREA").extent());known(0,f.view("RAW-PART").offset());known(0,f.view("OTHER-PART").offset());known(8,f.view("TAIL-PART").offset());
        assertTrue(f.layout().reasons().isEmpty());assertTrue(f.layout().bases().get(0).independent());
    }
    @Test void descendingImmediateChainUsesTheSamePhysicalComponent() {
        var f=fixture("01 WS-AREA.\n05 RAW-PART PIC X(4).\n04 OTHER-PART REDEFINES RAW-PART PIC X(8).\n03 LAST-PART REDEFINES OTHER-PART PIC X(12).\n03 TAIL-PART PIC X(2).");
        known(14,f.view("WS-AREA").extent());known(0,f.view("LAST-PART").offset());known(12,f.view("TAIL-PART").offset());
        assertTrue(f.layout().reasons().isEmpty());
    }
    @Test void interveningLowerNumberAndDifferentParentDoNotBecomeProofByName() {
        for(var data:List.of("01 WS-AREA.\n05 RAW-PART PIC X(4).\n04 OTHER-PART REDEFINES RAW-PART PIC X(8).\n04 LAST-PART REDEFINES RAW-PART PIC X(12).",
                "01 WS-AREA.\n05 RAW-PART PIC X(4).\n06 OTHER-PART REDEFINES RAW-PART PIC X(8).")) {
            var f=fixture(data);assertTrue(f.layout().reasons().contains(Reason.OVERLAY_NOT_PROVEN));
            assertTrue(f.layout().relations().stream().anyMatch(r->!r.proved()));
            assertTrue(f.layout().bases().stream().allMatch(b->b.extent().value().isEmpty()));
            assertTrue(f.layout().views().stream().allMatch(v->v.extent().value().isEmpty()&&!v.textual()),
                "unknown subordinate relation never proves component offsets, extents or codecs");
        }
    }
    @Test void explicitProfileAndOrdinaryAllocationRemainIndependentRequirements() {
        var absent=fixture("01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES RAW-AREA PIC X(8).",Profile.UNSPECIFIED);
        assertEquals(1,absent.layout().bases().size());assertTrue(absent.layout().bases().get(0).extent().value().isEmpty());
        assertFalse(absent.layout().bases().get(0).independent());assertTrue(absent.layout().views().stream().noneMatch(View::textual));
        for(var clause:List.of("GLOBAL","EXTERNAL")) {
            var f=fixture("01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES RAW-AREA PIC X(8) "+clause+".\n01 SAFE-AREA PIC X(8).");
            assertTrue(f.layout().bases().stream().noneMatch(Base::independent),clause);
        }
    }
}
