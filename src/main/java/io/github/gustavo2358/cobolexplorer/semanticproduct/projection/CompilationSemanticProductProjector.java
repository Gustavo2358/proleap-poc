package io.github.gustavo2358.cobolexplorer.semanticproduct.projection;
import java.util.*;
import io.github.gustavo2358.cobolexplorer.ResolutionContracts.ProgramUnitId;
import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
/** Translates canonical ownership and already resolved captures; never resolves a name. */
public final class CompilationSemanticProductProjector {
    private CompilationSemanticProductProjector(){}
    private static UnitId unit(ProgramUnitId id){return new UnitId(id.compilationUnitId(),id.structuralPath(),id.canonicalProgramName());}
    public static CompilationSemanticProduct project(CobolSemanticProductProjector.FrontendProducts products){
        var projected=new LinkedHashMap<ProgramUnitId,CobolSemanticProductProjector.ScopedProjection>();
        for(var u:products.frontend().compilationUnit().programUnits())projected.put(u.id(),CobolSemanticProductProjector.projectScoped(products,u.id()));
        var result=new ArrayList<CompilationSemanticProduct.UnitProduct>();
        for(var u:products.frontend().compilationUnit().programUnits()){
            var projection=projected.get(u.id());var port=CobolSemanticPort.open(projection.state());
            var captures=new ArrayList<CompilationSemanticProduct.DataCapture>();var globals=new ArrayList<DataItemId>();var owned=new ArrayList<DataItemId>();
            for(var entry:projection.dataIds().entrySet()){
                var entity=entry.getKey();
                if(entity.programUnitId().equals(u.id())){owned.add(entry.getValue());if(products.fileScope().globalData(entity))globals.add(entry.getValue());}
                else {var source=projected.get(entity.programUnitId()).dataIds().get(entity);if(source==null)throw new IllegalStateException("capture source not projected");captures.add(new CompilationSemanticProduct.DataCapture(entry.getValue(),source));}
            }
            var files=new LinkedHashSet<FileId>();
            for(var use:port.fileInventory().operations().uses())for(var candidate:use.candidates())if(!candidate.unit().equals(port.unit()))files.add(candidate);
            result.add(new CompilationSemanticProduct.UnitProduct(port,Optional.ofNullable(u.parentId()).map(CompilationSemanticProductProjector::unit),owned,globals,captures,List.copyOf(files)));
        }
        return new CompilationSemanticProduct(products.report().frontendState().supportsExternalClassification()&&products.report().frontendState().unresolvedCopies()==0?InventoryStatus.COMPLETE:InventoryStatus.INPUT_MISSING,projected.keySet().stream().map(CompilationSemanticProductProjector::unit).toList(),result);
    }
}
