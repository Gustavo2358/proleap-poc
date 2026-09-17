package io.github.gustavo2358.cobolexplorer.semanticproduct;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
/** Compilation inventory and selected-unit publications; execution remains intraprogram. */
public record CompilationSemanticProduct(InventoryStatus inventoryStatus,List<UnitId> unitInventory,List<UnitProduct> units){
    public CompilationSemanticProduct{Objects.requireNonNull(inventoryStatus);unitInventory=List.copyOf(unitInventory);units=List.copyOf(units);}
    public record DataCapture(DataItemId localData,DataItemId sourceData){}
    public record UnitProduct(CobolSemanticPort product,Optional<UnitId> parent,List<DataItemId> ownedData,List<DataItemId> globalData,List<DataCapture> dataCaptures,List<FileId> fileCaptures){
        public UnitProduct{Objects.requireNonNull(product);Objects.requireNonNull(parent);ownedData=List.copyOf(ownedData);globalData=List.copyOf(globalData);dataCaptures=List.copyOf(dataCaptures);fileCaptures=List.copyOf(fileCaptures);}
    }
}
