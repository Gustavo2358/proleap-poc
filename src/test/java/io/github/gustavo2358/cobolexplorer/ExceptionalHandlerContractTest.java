package io.github.gustavo2358.cobolexplorer;

import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Frontend-owned event premises, independent of handler selection and storage. */
class ExceptionalHandlerContractTest {
    static JsonNode publish(Path path) throws Exception {
        var a=AstBoundaryTestSupport.analyze(Files.readString(path),path.getFileName().toString());
        return CicsAbendContractTest.json(EofUnitBoundaryTest.publish(a,0,StorageLayoutSemantics.Profile.UNSPECIFIED));
    }
    @Test void realFrontendFixturesAndClosedEventAuthority() throws Exception {
        var dir=Path.of("src/test/resources/cobol/semantic/exceptional-handler");
        var export=System.getProperty("r7.exceptional.export");
        try(var stream=Files.list(dir)) { for(var path:stream.sorted().toList()) {
            var json=publish(path);assertEquals(json,publish(path),"deterministic source product");
            var events=json.path("controlTopology").path("exceptionalEvents");
            for(var event:events) {
                assertFalse(event.has("target"));assertFalse(event.has("handlerTarget"));
                assertEquals("CURRENT_EXECUTION_LOGICAL_LEVEL",event.path("scope").asText());
                assertEquals("UNAVAILABLE",event.path("runtimeIdentity").asText());
                if(event.path("origin").asText().equals("XCTL_PGMIDERR")) {
                    assertEquals(List.of("CONDITION_RAISED","DEFAULT_DISPOSITION_APPLIES"),new ObjectMapper().convertValue(event.path("premises"),List.class));
                } else assertTrue(event.path("premises").isEmpty());
            }
            String name=path.getFileName().toString().replace(".cbl","");
            long defaults=0;for(var event:events)if(event.path("origin").asText().equals("XCTL_PGMIDERR"))defaults++;
            assertEquals(Set.of("default","resp2","renamed","storage","default-dead","copy").contains(name)?1:0,defaults,name);
            if(name.equals("copy"))for(var event:events)if(event.path("origin").asText().equals("XCTL_PGMIDERR")) {
                var proofId=event.path("proofs").get(0);boolean found=false;
                for(var proof:json.path("controlTopology").path("proofs"))if(proof.path("id").equals(proofId)) {
                    assertEquals("R7EXEVENT.cpy",proof.path("provenance").path("original").path("file").asText());
                    assertFalse(proof.path("provenance").path("includeChain").isEmpty());found=true;
                }assertTrue(found);
            }
            if(defaults>0)assertEquals("2.46.0",json.path("contractVersion").asText());
            for(var fact:json.path("statements"))if(fact.path("variant").asText().equals("CICS_PROGRAM_CONTROL")&&fact.path("command").asText().equals("XCTL")&&defaults>0)
                for(var outcome:json.path("controlTopology").path("outcomes"))if(outcome.path("statement").equals(fact.path("header").path("id")))
                    assertNotEquals("NORMAL",outcome.path("kind").asText(),"exceptional event never creates XCTL ordinary return");
            if(export!=null) {var target=Path.of(export);Files.createDirectories(target);Files.writeString(target.resolve(name+".json"),json.toString());Files.copy(path,target.resolve(name+".cbl"),StandardCopyOption.REPLACE_EXISTING);}
        }}
    }
}
