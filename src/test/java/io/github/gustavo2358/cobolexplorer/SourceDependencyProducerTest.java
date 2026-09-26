package io.github.gustavo2358.cobolexplorer;

import java.nio.file.*;
import java.util.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.SourceDependencyInventory;
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
        assertEquals("preprocessing:program.cbl",facts.get(0).provenance().expanded().file());
        assertEquals("preprocessing:A.cpy",facts.get(1).provenance().expanded().file());
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
    @Test void sourceOwnershipDistinguishesProgramsOnTheSamePhysicalLine() {
        var a=new ResolutionContracts.ProgramUnitId("source",List.of(0),"A");
        var b=new ResolutionContracts.ProgramUnitId("source",List.of(1),"B");
        var pa=program("A",0,40);var pb=program("B",41,80);
        var model=new CompilationUnitModel("source",List.of(new CompilationUnitModel.ProgramUnit(a,null,pa),new CompilationUnitModel.ProgramUnit(b,null,pb)));
        var coverage=new HashMap<ResolutionContracts.ProgramUnitId,SemanticCoverage.Report>();coverage.put(a,null);coverage.put(b,null);
        var frontend=new CompilationUnitBuildResult(model,coverage,Map.of(a,List.of(),b,List.of()));
        var la=new Ast.SourceLocation("program.cbl",1,20,1,25);var lb=new Ast.SourceLocation("program.cbl",1,60,1,65);
        var facts=List.of(fact("CA",la),fact("CB",lb));
        var result=SourceDependencySemantics.associate(frontend,facts,List.of());
        assertEquals("CA",result.get(a).occurrences().get(0).name());
        assertEquals("CB",result.get(b).occurrences().get(0).name());
        var outside=new Ast.SourceLocation("outside.cbl",1,0,1,5);
        assertThrows(IllegalArgumentException.class,()->SourceDependencySemantics.associate(frontend,List.of(fact("X",outside)),List.of()));
        var afterBoth=new Ast.SourceLocation("program.cbl",1,81,1,85);
        assertThrows(IllegalArgumentException.class,()->SourceDependencySemantics.associate(frontend,List.of(fact("AFTER-BOTH",afterBoth)),List.of()));
    }
    @Test void sourceOwnershipUsesProvedBodyNodesWhenProgramHeaderProvenanceIsShort() {
        var id=new ResolutionContracts.ProgramUnitId("source",List.of(0),"A");
        var header=program("A",5,10);
        var bodyPlace=new Ast.SourceLocation("program.cbl",10,0,10,20);
        var body=new Ast.Division(new Ast.Meta(1,new Ast.SourceSpan(10,0,10,20,1,1),
            new Ast.ParseTreeOrigin(1,"procedureDivision",1),
            new Ast.SourceProvenance(bodyPlace,bodyPlace,List.of(),true)),Ast.DivisionKind.PROCEDURE,List.of());
        var program=new Ast.Program(header.meta(),"A",List.of(body));
        var model=new CompilationUnitModel("source",List.of(new CompilationUnitModel.ProgramUnit(id,null,program)));
        var coverage=new HashMap<ResolutionContracts.ProgramUnitId,SemanticCoverage.Report>();coverage.put(id,null);
        var frontend=new CompilationUnitBuildResult(model,coverage,Map.of(id,List.of()));
        var inside=new Ast.SourceLocation("program.cbl",5,0,5,10);
        var result=SourceDependencySemantics.associate(frontend,List.of(fact("COPY-IN-BODY",inside)),List.of());
        assertEquals("COPY-IN-BODY",result.get(id).occurrences().get(0).name());
        var trailingCopy=new Ast.SourceLocation("program.cbl",11,0,11,10);
        assertEquals("TRAILING-COPY",SourceDependencySemantics.associate(frontend,
            List.of(fact("TRAILING-COPY",trailingCopy)),List.of()).get(id).occurrences().get(0).name());
        var before=new Ast.SourceLocation("program.cbl",1,0,1,4);
        assertThrows(IllegalArgumentException.class,()->SourceDependencySemantics.associate(frontend,List.of(fact("BEFORE-PROGRAM",before)),List.of()));
    }
    @Test void sourceOwnershipFollowsRootCopyWhenItContainsTheWholeProgram() {
        var id=new ResolutionContracts.ProgramUnitId("source",List.of(0),"A");
        var site=new Ast.SourceLocation("main.cbl",1,7,1,16);
        var copied=new Ast.SourceLocation("UNIT.cpy",1,7,1,31);
        var frame=new Ast.CopyFrame("main.cbl","UNIT","UNIT.cpy",1);
        var meta=new Ast.Meta(0,new Ast.SourceSpan(1,0,10,0,0,10),new Ast.ParseTreeOrigin(0,"programUnit",10),
            new Ast.SourceProvenance(copied,copied,List.of(frame),false));
        var program=new Ast.Program(meta,"A",List.of());
        var model=new CompilationUnitModel("source",List.of(new CompilationUnitModel.ProgramUnit(id,null,program)));
        var coverage=new HashMap<ResolutionContracts.ProgramUnitId,SemanticCoverage.Report>();coverage.put(id,null);
        var frontend=new CompilationUnitBuildResult(model,coverage,Map.of(id,List.of()));
        var root=fact("UNIT",site);
        var nestedPlace=new Ast.SourceLocation("UNIT.cpy",2,0,2,10);
        var nested=new SourceDependencyFact(SourceDependencyFact.Kind.COPYBOOK,"FIELDS","",
            SourceDependencyFact.Resolution.UNRESOLVED,"","COPY_SYNTAX",
            new Ast.SourceProvenance(nestedPlace,nestedPlace,List.of(frame),true),site);
        var result=SourceDependencySemantics.associate(frontend,List.of(root,nested),List.of());
        assertEquals(List.of("UNIT","FIELDS"),result.get(id).occurrences().stream().map(SourceDependencyInventory.Occurrence::name).toList());
        assertThrows(IllegalArgumentException.class,()->SourceDependencySemantics.associate(frontend,List.of(fact("OTHER",site)),List.of()));
    }
    private static SourceDependencyFact fact(String name,Ast.SourceLocation location) {
        return new SourceDependencyFact(SourceDependencyFact.Kind.COPYBOOK,name,"",SourceDependencyFact.Resolution.UNRESOLVED,"","COPY_SYNTAX",new Ast.SourceProvenance(location,location,List.of(),true),location);
    }
    private static Ast.Program program(String name,int start,int end) {
        var location=new Ast.SourceLocation("program.cbl",1,start,1,end);
        return new Ast.Program(new Ast.Meta(start,new Ast.SourceSpan(1,start,1,end,0,0),new Ast.ParseTreeOrigin(0,"programUnit",1),new Ast.SourceProvenance(location,location,List.of(),true)),name,List.of());
    }
}
