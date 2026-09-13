package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

class StorageLayoutTest {
    record Fixture(AstBoundaryTestSupport.Analysis source,StorageLayoutSemantics product,Layout layout) {
        View view(String name) {
            var symbols=source.tables().forProgramUnit(source.model().programUnits().get(0).id()).orElseThrow().symbolTable().symbols();
            int node=symbols.stream().filter(s->s.canonicalName().equals(name)).findFirst().orElseThrow().declarationAstNodeId();
            return layout.views().stream().filter(v->v.node().node()==node).findFirst().orElseThrow();
        }
    }
    static Fixture fixture(String data,Profile profile) {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data,"GOBACK."),"layout.cbl");
        var product=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),profile);
        return new Fixture(a,product,product.layout(a.model().programUnits().get(0).id()));
    }
    static Fixture fixture(String data){return fixture(data,Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);}
    static void known(long value,Measure measure){assertEquals(Optional.of(BigInteger.valueOf(value)),measure.value());assertEquals(List.of(),measure.reasons());}
    @Test void nestedGroupIncludesFillerAndUsesPhysicalOffsets() {
        var f=fixture("01 WS-AREA.\n05 LEFT-PART PIC X(4).\n05 FILLER PIC X(2).\n05 TAIL-PART.\n10 RIGHT-PART PIC X(2).");
        assertEquals(5,f.layout.nodes().size());assertEquals(1,f.layout.bases().size());known(8,f.layout.bases().get(0).extent());
        known(0,f.view("WS-AREA").offset());known(8,f.view("WS-AREA").extent());known(0,f.view("LEFT-PART").offset());
        known(6,f.view("TAIL-PART").offset());known(2,f.view("TAIL-PART").extent());known(6,f.view("RIGHT-PART").offset());
        assertTrue(f.layout.views().stream().allMatch(v->v.base().equals(f.view("WS-AREA").base())));
        var filler=f.layout.nodes().stream().filter(Node::filler).findFirst().orElseThrow();
        assertTrue(filler.entity().isEmpty());known(2,filler.extent());assertEquals(Optional.of(f.view("WS-AREA").node()),filler.parent());
        known(4,f.layout.views().stream().filter(v->v.node().equals(filler.id())).findFirst().orElseThrow().offset());
        assertTrue(filler.origin().exact());assertTrue(f.layout.bases().get(0).independent());
    }
    @Test void opaquePrefixNeverBecomesZeroAndKnownPrefixSurvives() {
        var f=fixture("01 WS-AREA.\n05 PREFIX-PART PIC X(2).\n05 UNKNOWN-PART PIC 9(3).\n05 TARGET-PART PIC X(8).\n01 SAFE-AREA PIC X(8).");
        known(0,f.view("PREFIX-PART").offset());known(2,f.view("PREFIX-PART").extent());
        assertTrue(f.view("UNKNOWN-PART").extent().value().isEmpty());
        assertTrue(f.view("TARGET-PART").offset().value().isEmpty());assertFalse(f.view("TARGET-PART").offset().reasons().isEmpty());
        known(8,f.view("TARGET-PART").extent());assertTrue(f.view("WS-AREA").extent().value().isEmpty());
        known(0,f.view("SAFE-AREA").offset());known(8,f.view("SAFE-AREA").extent());
        assertNotEquals(f.view("WS-AREA").base(),f.view("SAFE-AREA").base());
    }
    @Test void explicitEnvironmentIsRequiredAndExcludedClausesStayUnknown() {
        var absent=fixture("01 WS-AREA.\n05 TARGET-PART PIC X(8).",Profile.UNSPECIFIED);
        assertTrue(absent.layout.reasons().contains(Reason.PROFILE_NOT_SELECTED));
        assertTrue(absent.layout.views().stream().allMatch(v->v.extent().value().isEmpty()));
        for(var clause:List.of("OCCURS 2 TIMES","JUSTIFIED RIGHT","VALUE 'ABCDEFGH'","USAGE NATIONAL","EXTERNAL")) {
            var f=fixture("01 WS-AREA PIC X(8) "+clause+".");
            assertTrue(f.view("WS-AREA").extent().value().isEmpty(),clause);
        }
        var overlay=fixture("01 WS-AREA PIC X(8).\n01 OTHER-AREA REDEFINES WS-AREA PIC X(8).");
        assertTrue(overlay.layout.reasons().contains(Reason.OVERLAY_NOT_PROVEN));
        assertTrue(overlay.layout.bases().stream().noneMatch(Base::independent));
    }
    @Test void oneTwoFiveAndManyChildrenHaveUncappedLinearPreparationAndRenameInvariant() {
        for(int n:new int[]{1,2,5,40,256}) {
            var data=new StringBuilder("01 WS-AREA.\n");for(int i=0;i<n;i++)data.append("05 ITEM-").append(i).append(" PIC X.\n");
            var f=fixture(data.toString());var renamed=fixture(data.toString().replace("WS-AREA","RENAMED").replace("ITEM-","OTHER-"));
            known(n,f.view("WS-AREA").extent());assertEquals(n+1,f.layout.views().size());
            for(int i=0;i<n;i++){known(i,f.view("ITEM-"+i).offset());assertEquals(f.view("ITEM-"+i).offset(),renamed.view("OTHER-"+i).offset());}
            assertEquals((long)n+1,f.product.metrics().get("declarations"));
            assertEquals(0L,f.product.metrics().get("objectPairs"));
            assertTrue(f.product.metrics().get("layoutVisits")<=4L*(n+1));
        }
    }
    @Test void missingInputAndNonordinaryProgramsNeverClaimPhysicalCertainty() {
        var a=fixture("01 WS-AREA PIC X(8).").source();
        var missing=new ResolutionAnalysisReport.FrontendState(0,0,0,List.of(new Diagnostic("COBOL",Diagnostic.Phase.PREPROCESSOR,
            Diagnostic.Code.UNRESOLVED_COPY,"layout.cbl",3,0,"COPY unavailable","MISSING","")));
        var report=ResolutionAnalysisReport.compose(a.build(),missing,a.occurrences(),a.resolution());
        var product=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),report,Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        var layout=product.layout(a.model().programUnits().get(0).id());assertTrue(layout.reasons().contains(Reason.INPUT_MISSING));
        assertTrue(layout.bases().stream().allMatch(b->!b.independent()&&b.extent().value().isEmpty()));
        for(var attribute:List.of("INITIAL","RECURSIVE")) {
            String source=ScalarMoveCheckpoint4ATest.program("01 WS-AREA PIC X(8).","GOBACK.").replace("PROGRAM-ID. SAMPLE.","PROGRAM-ID. SAMPLE IS "+attribute+".");
            var restricted=AstBoundaryTestSupport.analyze(source,"layout.cbl");
            var p=StorageLayoutSemantics.analyze(restricted.build(),restricted.tables(),restricted.resolution(),restricted.report(),Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
            assertTrue(p.layout(restricted.model().programUnits().get(0).id()).reasons().contains(Reason.NONORDINARY_PROGRAM));
        }
    }
    @Test void copybookOriginsAndInheritedDisplaySurvivePhysicalProjection() {
        var f=fixture("COPY STORAGE-FIRST.");
        assertEquals(Optional.of(BigInteger.valueOf(2)),f.view("COPIED-GROUP").extent().value(),f.layout.toString());known(1,f.view("SECOND-FIELD").offset());
        var origin=f.view("SECOND-FIELD").origin();assertEquals("SECOND.cpy",origin.original().file());
        assertEquals(2,origin.includeChain().size());
        var canonical=AstBoundaryTestSupport.nodes(f.source(),Ast.DataEntry.class).stream().filter(d->d.name().equals("SECOND-FIELD")).findFirst().orElseThrow();
        assertEquals(canonical.meta().provenance(),origin);
        var display=fixture("01 WS-AREA USAGE DISPLAY.\n05 FIRST-PART PIC X(3).\n05 SECOND-PART PIC X(2).");
        known(5,display.view("WS-AREA").extent());known(3,display.view("SECOND-PART").offset());
        // REPLACING changes source mapping, while its fully parsed typed layout is known.
        var transformed=fixture("COPY FIRST REPLACING ==:TAG:== BY ==ACTUAL==.");
        known(2,transformed.view("ACTUAL-GROUP").extent());known(1,transformed.view("SECOND-FIELD").offset());
        assertFalse(transformed.view("ACTUAL-GROUP").origin().exact());
    }
    @Test void nonlocalChildCannotProvePrivateRootIndependence() {
        var f=fixture("01 WS-AREA.\n05 SHARED-PART PIC X(8) GLOBAL.\n01 LOCAL-PART PIC X(8).");
        var root=f.layout.bases().stream().filter(b->b.id().equals(f.view("WS-AREA").base())).findFirst().orElseThrow();
        assertFalse(root.independent());
    }
    @Test void illegalRootLevelDoesNotBecomeOrdinaryAllocation() {
        var f=fixture("05 WS-AREA PIC X(8).");
        assertFalse(f.layout.bases().get(0).independent());
        assertTrue(f.layout.bases().get(0).extent().value().isEmpty());
    }
}
