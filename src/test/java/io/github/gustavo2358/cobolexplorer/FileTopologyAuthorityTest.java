package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.ControlTopology.*;

/** Executable FILE targets are published once; domain metadata is producer input. */
class FileTopologyAuthorityTest {
    private static CobolSemanticPort product() throws Exception {
        var fixture=FileMemoryEffectsTest.fixture(
            "SELECT F ASSIGN TO INDD ORGANIZATION IS INDEXED\n ACCESS IS DYNAMIC RECORD KEY REC FILE STATUS FS.",
            "FD F.\n01 REC PIC X(8).", "01 FS PIC XX.",
            "MAIN.\nPERFORM P.\nGOBACK.\nP.\nDELETE F RECORD\n"
                +" INVALID KEY DISPLAY 'BAD-FIRST' DISPLAY 'BAD-SECOND'\n"
                +" NOT INVALID KEY DISPLAY 'GOOD-FIRST' DISPLAY 'GOOD-SECOND'\nEND-DELETE.\nCONTINUE.");
        return ControlTopologyAuthorityTest.publishFile(fixture);
    }
    @Test void everyFileDestinationHasAnAuthoritativeOutcome() throws Exception {
        var product=product();
        var topology=product.controlTopology().orElseThrow();
        int destinations=0,handlers=0;
        for(var use:product.fileInventory().operations().uses()) {
            var statement="statement:"+use.statement().localId();
            for(var route:use.control().routes())for(int i=0;i<route.destinations().size();i++) {
                var role="file/"+use.ordinal()+"/"+route.event()+"/"+i;
                var matches=topology.outcomes().stream().filter(o->o.statement().equals(statement)&&o.role().equals(role)).toList();
                assertEquals(1,matches.size(),role);var outcome=matches.get(0);destinations++;
                assertTrue(topology.occurrences().stream().anyMatch(o->o.statement().equals(statement)&&o.outcomes().contains(outcome.id())));
                var destination=route.destinations().get(i);
                if(destination.kind()==CobolSemanticProduct.FileDestinationKind.HANDLER) {
                    handlers++;
                    var handler=use.handlers().stream().filter(h->h.kind()==destination.handler().orElseThrow()).findFirst().orElseThrow();
                    var region=topology.regions().stream().filter(r->r.id().equals(outcome.target().reference())).findFirst().orElseThrow();
                    assertEquals(TargetKind.REGION_ENTRY,outcome.target().kind());assertEquals(RegionKind.FILE_HANDLER,region.kind());
                    assertEquals(TargetKind.OCCURRENCE,region.entry().kind());
                    assertEquals("statement:"+handler.statements().get(0).localId(),region.entry().reference());
                    assertEquals(2,handler.statements().size());
                }
            }
        }
        assertEquals(2,handlers);assertTrue(destinations>handlers);
        var directory=Path.of("target/control-topology-r1-r1");Files.createDirectories(directory);
        Files.write(directory.resolve("file-authority.json"),SemanticProductJsonWriter.serialize(product));
    }

    @Test void outcomeRoleIsUniqueWithinItsSourceRegardlessOfIdentityOrTarget() throws Exception {
        var topology=product().controlTopology().orElseThrow();
        assertTrue(topology.outcomes().stream().map(Outcome::role).distinct().count()<topology.outcomes().size(),
            "valid repeated roles on distinct source occurrences remain accepted");
        for(var original:topology.outcomes())for(var identity:List.of("outcome:000-role-shadow","outcome:zzz-role-shadow"))
            for(boolean conflict:List.of(false,true))for(boolean reverse:List.of(false,true)) {
                var target=conflict?topology.outcomes().stream().map(Outcome::target)
                    .filter(t->!t.equals(original.target())).findFirst().orElseThrow():original.target();
                var duplicate=new Outcome(identity,original.statement(),original.kind(),original.role(),target,original.binding(),original.proofs());
                var outcomes=new ArrayList<>(topology.outcomes());outcomes.add(duplicate);
                var occurrences=new ArrayList<>(topology.occurrences().stream().map(o->{
                    var ids=new ArrayList<>(o.outcomes());if(o.statement().equals(original.statement()))ids.add(identity);
                    if(reverse)Collections.reverse(ids);
                    return new Occurrence(o.statement(),o.region(),ids,o.proofs());}).toList());
                if(reverse){Collections.reverse(outcomes);Collections.reverse(occurrences);}
                var failure=assertThrows(IllegalArgumentException.class,()->new ControlTopology(topology.authority(),occurrences,
                    topology.regions(),topology.boundaries(),outcomes,topology.bindings(),topology.proofs()),
                    original.role()+" / "+identity+" / conflict="+conflict+" / reverse="+reverse);
                assertTrue(failure.getMessage().contains("duplicate outcome role"),failure.getMessage());
            }
    }
}
