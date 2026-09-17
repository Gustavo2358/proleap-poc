package io.github.gustavo2358.cobolexplorer;
import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
class StorageMixedInitialTest {
    static final String DATA="01 WS-LEGACY PIC 9.\n01 WS-GROUP.\n05 WS-PGM PIC X(8) VALUE 'PGM00001'.\n01 WS-ALIAS REDEFINES WS-GROUP PIC X(8).\n01 WS-TABLE.\n05 WS-ELEM OCCURS 2 PIC X.\n01 WS-NATIONAL PIC N(4).";
    @Test void knownInitialRegionCoexistsWithLegacyAndUnprovedPhysicalComponents() throws Exception {
        var a=StorageAccessTest.fixture(DATA,"CALL WS-PGM.\nMOVE 'OTHERPGM' TO WS-ALIAS.\nCALL WS-PGM.\nCALL 'LITERAL1'.\nPERFORM WORK-PARA WS-LEGACY TIMES.\nGOBACK.\nWORK-PARA.\nCONTINUE.").source();
        var p=ExplorerMain.publishSemanticProduct(a.model().programUnits().get(0).id(),a.build(),a.tables(),a.occurrences(),a.resolution(),a.report(),StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,StorageInitialSemantics.EntryMode.INITIAL);
        assertEquals(4,p.storage().bases().size());assertEquals(7,p.storage().nodes().size());assertEquals(7,p.storage().views().size());
        var condition=p.storage().entryState().conditions().get(0);assertEquals(InitialStorageKind.LITERAL_BYTES,condition.kind());
        assertEquals(1,p.dataDeclarations().stream().filter(d->d.scalarInteger().isPresent()).count());
        assertTrue(p.storage().bases().stream().anyMatch(b->b.extent().value().isEmpty()));
        assertTrue(p.storage().views().stream().anyMatch(v->v.codec().isEmpty()));
        assertTrue(p.storage().bases().stream().allMatch(b->b.allocation()==AllocationProof.INDEPENDENT_LOCAL_STORAGE));
        assertTrue(p.calls().get(2).target() instanceof LiteralCallTarget);
        if(System.getProperty("storage.fixture.output")!=null)java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("storage.fixture.output")),SemanticProductJsonWriter.serialize(p));
    }
    @Test void unprovedAllocationDoesNotAcquireIndependentScalarProof() {
        var s=StorageProductTest.state(DATA+"\n01 EXTERNAL-DATA PIC X EXTERNAL.","MOVE 'PGM00001' TO WS-PGM.\nCALL 'LITERAL1'.");
        assertTrue(s.storage().bases().stream().allMatch(b->b.allocation()==AllocationProof.UNPROVEN));
        assertTrue(s.dataDeclarations().stream().allMatch(d->d.scalarInteger().isEmpty()&&d.scalarText().isEmpty()));
        assertTrue(((CallFact)s.statements().get(1)).target() instanceof LiteralCallTarget);
    }
}
