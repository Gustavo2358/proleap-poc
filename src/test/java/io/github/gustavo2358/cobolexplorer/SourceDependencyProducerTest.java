package io.github.gustavo2358.cobolexplorer;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SourceDependencyProducerTest {
    @TempDir Path directory;
    private PreprocessorEngine.Outcome process(String text) throws Exception {
        return new PreprocessorEngine(Bindings.cobol(), new CopybookLibrary(directory))
            .process(SourceMap.identity(text, "program.cbl"), "program.cbl");
    }
    @Test void preservesMissingRepeatedAndReplacingOccurrences() throws Exception {
        var facts=process("COPY cpy404.\nCOPY CPY404 REPLACING ==OLD== BY ==NEW==.\n").sourceDependencies();
        assertEquals(List.of("CPY404","CPY404"),facts.stream().map(SourceDependencyFact::name).toList());
        assertTrue(facts.stream().allMatch(f->f.resolution()==SourceDependencyFact.Resolution.UNRESOLVED));
        assertEquals(List.of(1,2),facts.stream().map(f->f.provenance().original().startLine()).toList());
    }
    @Test void preservesEmptyAndNestedOwner() throws Exception {
        Files.writeString(directory.resolve("A.cpy"),"       COPY B.\n");
        Files.writeString(directory.resolve("B.cpy"),"");
        var facts=process("COPY A.\n").sourceDependencies();
        assertEquals(List.of("A","B"),facts.stream().map(SourceDependencyFact::name).toList());
        assertEquals("program.cbl",facts.get(0).provenance().original().file());
        assertEquals("A.cpy",facts.get(1).provenance().original().file());
        assertEquals("A",facts.get(1).provenance().includeChain().get(0).requestedName());
        assertTrue(facts.stream().allMatch(f->f.resolution()==SourceDependencyFact.Resolution.RESOLVED));
    }
    @Test void commentsAndLiteralsAreNotCopyDirectives() throws Exception {
        assertTrue(process("*> COPY FAKE.\nDISPLAY 'COPY FALSE.'.\n").sourceDependencies().isEmpty());
    }
    @Test void preservesLibraryQualification() throws Exception {
        var facts=process("COPY X OF LIB.\nCOPY X IN LIB.\n").sourceDependencies();
        assertEquals(List.of("LIB","LIB"),facts.stream().map(SourceDependencyFact::qualification).toList());
    }
    @Test void sqlIncludesDoNotInventDclgens() throws Exception {
        var facts=process("EXEC SQL INCLUDE SQLCA END-EXEC\nEXEC SQL INCLUDE SQLDA END-EXEC\nEXEC SQL INCLUDE MISSING END-EXEC\n").sourceDependencies();
        assertEquals(3,facts.size());
        assertTrue(facts.stream().allMatch(f->f.kind()==SourceDependencyFact.Kind.SQL_INCLUDE));
        assertEquals("BUILTIN_SQL_INCLUDE",facts.get(0).authority());
        assertEquals("UNKNOWN",facts.get(2).authority());
    }
    @Test void onlyConfiguredInventoryProvesDclgenIncludingMissingArtifact() throws Exception {
        var inventory=directory.resolve("inventory.json");
        Files.writeString(inventory,"""
            {"version":"1.0.0","artifacts":[
              {"name":"DCLCLI","kind":"DCLGEN","artifact":"DCLCLI.cpy"},
              {"name":"GENERIC","kind":"SQL_INCLUDE","artifact":"GENERIC.cpy"}]}
            """);
        Files.writeString(directory.resolve("DCLCLI.cpy"),"");
        var producer=new PreprocessorEngine(Bindings.cobol(),new CopybookLibrary(directory),SourceArtifactInventory.read(inventory));
        var source="EXEC SQL INCLUDE dclcli END-EXEC\nEXEC SQL INCLUDE GENERIC END-EXEC\n";
        var facts=producer.process(SourceMap.identity(source,"program.cbl"),"program.cbl").sourceDependencies();
        assertEquals(SourceDependencyFact.Kind.DCLGEN,facts.get(0).kind());
        assertEquals("CONFIGURED_DCLGEN",facts.get(0).authority());
        assertEquals(SourceDependencyFact.Resolution.RESOLVED,facts.get(0).resolution());
        assertEquals(SourceDependencyFact.Kind.SQL_INCLUDE,facts.get(1).kind());
        assertEquals(SourceDependencyFact.Resolution.UNRESOLVED,facts.get(1).resolution());
        Files.delete(directory.resolve("DCLCLI.cpy"));
        var missing=producer.process(SourceMap.identity(source,"program.cbl"),"program.cbl").sourceDependencies().get(0);
        assertEquals(SourceDependencyFact.Kind.DCLGEN,missing.kind());
        assertEquals(SourceDependencyFact.Resolution.UNRESOLVED,missing.resolution());
    }
    @Test void rejectsInventoryThatWouldMisclassifySqlca() throws Exception {
        var inventory=directory.resolve("inventory.json");
        Files.writeString(inventory,"""
            {"version":"1.0.0","artifacts":[{"name":"SQLCA","kind":"DCLGEN","artifact":"SQLCA.cpy"}]}
            """);
        assertThrows(IllegalArgumentException.class,()->SourceArtifactInventory.read(inventory));
    }
    @Test void recoveredCopyIsNeverAProvedOccurrence() {
        assertThrows(IllegalArgumentException.class,()->process("COPY .\n"));
    }
    @Test void sqlLiteralsAndMalformedIncludeDoNotProveNames() throws Exception {
        var outcome=process("EXEC SQL INCLUDE 'FAKE' END-EXEC\nEXEC SQL INCLUDE A B END-EXEC\n");
        assertTrue(outcome.sourceDependencies().isEmpty());
        assertEquals(List.of("SQL_INCLUDE_FORM_UNPROVED"),outcome.sourceDependencyGaps());
    }
}
