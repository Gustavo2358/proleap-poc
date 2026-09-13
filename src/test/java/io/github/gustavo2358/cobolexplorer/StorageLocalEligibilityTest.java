package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageProductTest.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

/** Separate controls prove locality; no removal of the related-overlay refusal. */
class StorageLocalEligibilityTest {
    static final String OVERLAY="01 RAW-AREA PIC X(8).\n01 VIEW-AREA REDEFINES RAW-AREA PIC X(8).\n";
    @Test void unrelatedOverlayPreservesLegacyScalarWritesAndCallInBothProfiles() {
        for(var profile:StorageLayoutSemantics.Profile.values()) {
            var s=state(OVERLAY+"01 WS-PGM PIC X(8).","MOVE 'PGM00001' TO WS-PGM.\nCALL WS-PGM.",profile);
            var data=s.dataDeclarations().stream().filter(d->d.canonicalName().equals("WS-PGM")).findFirst().orElseThrow();assertTrue(data.scalarText().isPresent());
            var move=(MoveFact)s.statements().get(0);assertEquals(CopySemantics.FULL_IDENTITY,move.copySemantics());
            assertEquals(data.id(),move.target().wholeItemAccess().orElseThrow().data());
            var call=(CallFact)s.statements().get(1);assertEquals(data.id(),((DataReference)call.target()).wholeItemAccess().orElseThrow().data());
            assertTrue(s.dataDeclarations().stream().filter(d->!d.id().equals(data.id())).allMatch(d->d.scalarText().isEmpty()));
        }
    }
    @Test void localIndependenceAndIfPredicateUseTheSameStorageProof() {
        var s=state(OVERLAY+"01 FLAG PIC X.\n01 WS-PGM PIC X(8).","IF FLAG = 'Y'\nMOVE 'PGM00001' TO WS-PGM\nELSE\nMOVE 'PGM00002' TO WS-PGM\nEND-IF.\nCALL WS-PGM.");
        var predicate=(IfFact)s.statements().get(0);assertEquals(Availability.KNOWN,predicate.condition().predicate().availability());
        assertEquals(Availability.KNOWN,s.storageIndependence().availability());
        var expected=s.dataDeclarations().stream().filter(d->Set.of("FLAG","WS-PGM").contains(d.canonicalName())).map(DataDeclaration::id).collect(java.util.stream.Collectors.toSet());
        assertEquals(expected,new HashSet<>(s.storageIndependence().members()));
    }
    @Test void numericSelectorOutsideAnOverlayKeepsItsExistingWholeItemProof() throws Exception {
        var source=ConditionalGoToTest.source("GO TO ONE-PARA TWO-PARA DEPENDING ON WS-IDX.\nGOBACK.\nONE-PARA.\nGOBACK.\nTWO-PARA.\nGOBACK.")
            .replace("PROCEDURE DIVISION.",OVERLAY+"PROCEDURE DIVISION.");
        var wire=ConditionalGoToTest.wire(source);var control=ConditionalGoToTest.conditional(wire);
        assertTrue(control.path("selectorInteger").asBoolean());assertTrue(control.path("selector").path("wholeItemAccess").isObject());
        assertTrue(control.path("gapCodes").isEmpty());
    }
    @Test void relatedAndLateFillerOverlaysStillDenySeparateScalarCells() {
        for(var data:List.of(OVERLAY,"01 RAW-AREA PIC X(8).\n01 FILLER REDEFINES RAW-AREA PIC X(8).")) {
            var s=state(data,"MOVE 'PGM00001' TO RAW-AREA.\nCALL RAW-AREA.");
            assertTrue(s.dataDeclarations().stream().allMatch(d->d.scalarText().isEmpty()));
            assertTrue(((MoveFact)s.statements().get(0)).target().wholeItemAccess().isEmpty());
            assertTrue(((DataReference)((CallFact)s.statements().get(1)).target()).wholeItemAccess().isEmpty());
            assertEquals(1,s.storage().bases().size());
        }
    }
    @Test void unresolvedRelationsAndOpaqueAllocationRemainConservative() {
        for(var extra:List.of("01 BAD-AREA REDEFINES MISSING-AREA PIC X(8).","01 BAD-AREA PIC X(8) EXTERNAL.",
                "01 BAD-AREA PIC X(8) GLOBAL.","01 BAD-AREA PIC X(8) JUSTIFIED RIGHT.")) {
            var s=state("01 WS-PGM PIC X(8).\n"+extra,"MOVE 'PGM00001' TO WS-PGM.\nCALL WS-PGM.");
            assertTrue(s.dataDeclarations().stream().filter(d->d.canonicalName().equals("WS-PGM")).findFirst().orElseThrow().scalarText().isEmpty(),extra);
            assertTrue(((MoveFact)s.statements().get(0)).target().wholeItemAccess().isEmpty(),extra);
        }
    }
    @Test void timesAndVaryingProfilesKeepTheirNumericProofOutsideOverlayComponents() throws Exception {
        for(var name:List.of("times-identifier","varying-before","varying-after")) {
            var path=java.nio.file.Path.of("src/test/resources/cobol/perform-family/"+name+".cbl");
            var source=java.nio.file.Files.readAllLines(path).stream().map(line->line.substring(7)).collect(java.util.stream.Collectors.joining("\n"));
            var wire=PerformFamilyTest.publish(source.replace("PROCEDURE DIVISION.",OVERLAY+"PROCEDURE DIVISION."));
            var ranges=PerformFamilyTest.ranges(wire);assertFalse(ranges.isEmpty());
            for(var range:ranges) {
                assertTrue(range.path("gapCodes").isEmpty(),name+": "+range.path("gapCodes"));
                if(name.startsWith("times"))assertTrue(range.path("times").path("reference").path("wholeItemAccess").isObject());
                else assertTrue(range.path("varying").path("controls").get(0).path("references").get(0).path("wholeItemAccess").isObject());
            }
        }
    }
    @Test void componentCountAndNamingDoNotAffectAnIndependentScalar() {
        for(int n:new int[]{1,2,5,40}) {
            var data=new StringBuilder();for(int i=0;i<n;i++)data.append(OVERLAY.replace("RAW-AREA","RAW-"+i).replace("VIEW-AREA","VIEW-"+i));
            data.append("01 WS-PGM PIC X(8).");
            var s=state(data.toString(),"MOVE 'PGM00001' TO WS-PGM.\nCALL WS-PGM.");
            assertEquals(1,s.dataDeclarations().stream().filter(d->d.scalarText().isPresent()).count());
            assertEquals(CopySemantics.FULL_IDENTITY,((MoveFact)s.statements().get(0)).copySemantics());
            assertEquals(n+1,s.storage().bases().size());
        }
    }
    @Test void sharedProofRejectsAnIndexFromAnotherSnapshot() {
        var a=StorageAccessTest.fixture(OVERLAY+"01 WS-PGM PIC X(8).","CALL WS-PGM.").source();
        var b=StorageAccessTest.fixture(OVERLAY+"01 WS-PGM PIC X(8).","CALL WS-PGM.").source();
        var index=StorageComponents.analyze(b.build());
        assertThrows(IllegalArgumentException.class,()->ScalarMoveSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),index));
        assertThrows(IllegalArgumentException.class,()->NumericControlSemantics.analyze(a.build(),a.tables(),true,index));
        assertThrows(IllegalArgumentException.class,()->StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.UNSPECIFIED,index));
    }
}
