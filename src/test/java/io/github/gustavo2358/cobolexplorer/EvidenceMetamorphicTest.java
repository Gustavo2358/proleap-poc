package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.DeclarativeValueInferenceTest.*;

/** Arbitrary preserved construction × layout × arguments/results, independent of keyword semantics. */
class EvidenceMetamorphicTest {
    @Test void genericPartialConstructionPreservesSourceSupportAcrossLayoutAndCallOperands() throws Exception {
        var out=Path.of("target/ep-w5-generic");Files.createDirectories(out);
        int count=0;
        for(boolean physical:List.of(false,true))for(boolean shifted:List.of(false,true))for(String operands:List.of(""," USING ARG"," RETURNING ARG")) {
            String data=(shifted?"01 WS-AREA.\n05 PREFIX-UNKNOWN PIC S9(9) COMP.\n05 LIT-PGM PIC X(8) VALUE 'PROGA'.\n":VALUE)
                +"77 ARG PIC X(8).\n77 PARTIAL-AREA PIC X(3) USAGE DISPLAY.\n";
            var source=source(data,(physical&&!shifted&&operands.isEmpty()?"":"EXHIBIT ARG.\n")+"CALL LIT-PGM"+operands+".");
            var original=AstBoundaryTestSupport.analyze(source,"generic-"+count+".cbl");
            int replaced=AstBoundaryTestSupport.nodes(original,Ast.UsageClause.class).get(0).meta().id();
            var model=new CompilationUnitModel(original.model().compilationUnitId(),original.model().programUnits().stream()
                .map(u->new CompilationUnitModel.ProgramUnit(u.id(),u.parentId(),(Ast.Program)EvidencePreservationTest.replace(u.program(),replaced))).toList());
            var coverage=new LinkedHashMap<ResolutionContracts.ProgramUnitId,SemanticCoverage.Report>();
            original.build().coverageByProgramUnit().forEach((unit,report)->coverage.put(unit,new SemanticCoverage.Report(report.findings().stream()
                .map(f->f.astNodeId()==replaced?new SemanticCoverage.Finding(f.id(),"futureArbitraryClause",f.meta(),"JOHNDOE",SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED,f.dependencyKnowledge(),"generic addition law",f.astNodeId()):f).toList())));
            var build=new CompilationUnitBuildResult(model,coverage,original.build().diagnosticsByProgramUnit());
            var tables=new CompilationUnitSymbolTableBuilder().build(model);
            var profile=physical?StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047:StorageLayoutSemantics.Profile.UNSPECIFIED;
            var baseline=ExplorerMain.publishSemanticProduct(model.programUnits().get(0).id(),original.build(),original.tables(),original.occurrences(),original.resolution(),original.report(),profile,StorageInitialSemantics.EntryMode.UNKNOWN);
            var partial=ExplorerMain.publishSemanticProduct(model.programUnits().get(0).id(),build,tables,original.occurrences(),original.resolution(),original.report(),profile,StorageInitialSemantics.EntryMode.UNKNOWN);
            var before=condition(baseline);var after=condition(partial);
            if(!physical)assertEquals(before.logicalText(),after.logicalText());assertEquals(before.bytes(),after.bytes());
            assertTrue(after.logicalText().filter("PROGA   "::equals).isPresent()||!after.bytes().isEmpty());
            assertEquals(before.proof(),after.proof(),"coverage-only addition cannot weaken entry authority");
            assertEquals(before.gapCodes(),after.gapCodes(),"coverage belongs to the omitted clause, not the entry value");
            assertEquals(1,partial.calls().size());assertTrue(after.provenance().exact());
            var folder=out.resolve(String.format("case-%02d",count++));Files.createDirectories(folder);
            Files.writeString(folder.resolve("source.cbl"),source);
            Files.write(folder.resolve("baseline.sp.json"),SemanticProductJsonWriter.serialize(baseline));
            Files.write(folder.resolve("partial.sp.json"),SemanticProductJsonWriter.serialize(partial));
            Files.writeString(folder.resolve("transformation.txt"),"Typed AST substitution only: USAGE clause occurrence "+replaced+" becomes PreservedDataClause(futureArbitraryClause, JOHNDOE). No grammar change. physical="+physical+", shifted="+shifted+", operands="+operands+"\n");
        }
        assertEquals(12,count,"full pairwise/triple layout × encoding × operands coverage");
    }
}
