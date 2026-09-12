package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

class PartialProgramFactsTest {
    @Test void realSourceRegionsRemainExplicitAndDeterministic() throws Exception {
        var root=Path.of("src/test/resources/partial-program");
        try(var files=Files.list(root)) {
            for(var file:files.sorted().toList()) {
                var source=Files.readString(file); var p=ScalarMoveCheckpoint4ATest.publish(source);
                assertFalse(p.statements().isEmpty(),file.toString());
                assertFalse(p.calls().isEmpty(),file.toString());
                var name=file.getFileName().toString().replace(".cbl","");
                if(List.of("p1","p2","p3","read").contains(name)) {
                    var unknown=p.observedStatements(); assertEquals(1,unknown.size(),name);
                    assertEquals(ContinuationAvailability.KNOWN,unknown.get(0).normalContinuation().availability(),name);
                    assertTrue(p.gaps().stream().anyMatch(g->g.statement().equals(unknown.get(0).header().id())),name);
                }
                if(name.startsWith("perform-")) {
                    assertEquals(2,p.performs().size(),name); assertTrue(p.observedStatements().isEmpty(),name);
                    assertTrue(p.performs().stream().allMatch(f->f.profile()==PerformProfile.BASIC_PROCEDURE_PERFORM),name);
                }
                var bytes=SemanticProductJsonWriter.serialize(p);
                assertArrayEquals(bytes,SemanticProductJsonWriter.serialize(ScalarMoveCheckpoint4ATest.publish(source)),name);
                var out=Path.of("target/partial-program"); Files.createDirectories(out);Files.write(out.resolve(name+".json"),bytes);
            }
        }
    }
    @Test void compositionalSnapshotsAreGeneratedFromRealSource() throws Exception {
        var out=Path.of("target/partial-program"); Files.createDirectories(out);
        for(int n:List.of(1,2,5,40)) {
            var source=CompositionalityContractTest.program(n,n,n);var p=ScalarMoveCheckpoint4ATest.publish(source);
            assertEquals(2*n,p.calls().size()); assertEquals(n,p.ifs().size()); assertEquals(n,p.performs().size());
            Files.write(out.resolve("compose-"+n+".json"),SemanticProductJsonWriter.serialize(p));
        }
    }
}
