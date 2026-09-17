package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.ResolutionContracts.*;
/** Canonical declaration relationships. No spelling-based lookup or runtime sharing. */
public final class FileScopeSemantics {
    private final Map<SemanticEntityId,SemanticEntityId> recordOwners;
    private final Set<SemanticEntityId> globalData;
    private FileScopeSemantics(Map<SemanticEntityId,SemanticEntityId> owners,Set<SemanticEntityId> globals){recordOwners=Map.copyOf(owners);globalData=Set.copyOf(globals);}
    public Optional<SemanticEntityId> recordOwner(SemanticEntityId data){return Optional.ofNullable(recordOwners.get(data));}
    public boolean globalData(SemanticEntityId data){return globalData.contains(data);}
    public static FileScopeSemantics analyze(CompilationUnitSymbolTables tables){
        var owners=new LinkedHashMap<SemanticEntityId,SemanticEntityId>();var globals=new LinkedHashSet<SemanticEntityId>();
        for(var unit:tables.units()){
            var table=unit.symbolTable();var files=new HashMap<Integer,SemanticEntityId>();
            for(var entity:table.entities())if(entity.kind()==SymbolTable.EntityKind.FILE)
                for(var symbol:entity.declarationSymbolIds())files.put(symbol,new SemanticEntityId(unit.id(),SemanticEntityDomain.FILE_ENTITY,entity.id()));
            for(var symbol:table.symbols())if(symbol.namespace()==SymbolTable.Namespace.DATA){
                var id=new SemanticEntityId(unit.id(),SemanticEntityDomain.DATA_SYMBOL,symbol.id());
                if("GLOBAL".equals(symbol.attributes().get("visibility")))globals.add(id);
                var scope=table.scopes().get(symbol.scopeId());
                if("01".equals(symbol.attributes().get("level"))&&scope.kind()==SymbolTable.ScopeKind.FILE_DESCRIPTION){
                    var file=files.get(scope.ownerSymbolId());if(file!=null)owners.put(id,file);
                }
            }
        }
        return new FileScopeSemantics(owners,globals);
    }
}
