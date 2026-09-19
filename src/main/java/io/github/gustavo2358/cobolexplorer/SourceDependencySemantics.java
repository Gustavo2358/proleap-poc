package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.*;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

/** Associates captured source facts with structurally enclosing program source ranges. */
public final class SourceDependencySemantics {
    private SourceDependencySemantics() {}
    public static Map<ResolutionContracts.ProgramUnitId,SourceDependencyInventory> associate(
            CompilationUnitBuildResult frontend,List<SourceDependencyFact> facts,List<String> sourceGaps) {
        var collected=new LinkedHashMap<ResolutionContracts.ProgramUnitId,List<SourceDependencyInventory.Occurrence>>();
        var units=frontend.compilationUnit().programUnits();
        var byFile=new HashMap<String,TreeMap<Long,CompilationUnitModel.ProgramUnit>>();
        for(var unit:units) {
            collected.put(unit.id(),new ArrayList<>());
            var location=unit.program().meta().provenance().original();
            byFile.computeIfAbsent(location.file(),unused->new TreeMap<>()).put(position(location.startLine(),location.startColumn()),unit);
        }
        for(var fact:facts) {
            var source=fact.rootSite();var index=byFile.get(source.file());
            var entry=index==null?null:index.floorEntry(position(source.startLine(),source.startColumn()));
            var owner=entry==null?null:entry.getValue();
            while(owner!=null && position(source.endLine(),source.endColumn())>end(owner.program().meta().provenance().original()))
                owner=owner.parentId()==null?null:frontend.compilationUnit().find(owner.parentId()).orElse(null);
            if(owner==null)throw new IllegalArgumentException("SOURCE_DEPENDENCY_OWNER_UNPROVED: nominal occurrence cannot be assigned safely");
            var target=collected.get(owner.id());var p=fact.provenance();
            target.add(new SourceDependencyInventory.Occurrence("source-"+target.size(),
                SourceDependencyInventory.Kind.valueOf(fact.kind().name()),fact.name(),fact.qualification(),
                SourceDependencyInventory.Resolution.valueOf(fact.resolution().name()),fact.artifact(),fact.authority(),
                new Provenance(location(p.expanded()),location(p.original()),p.includeChain().stream()
                    .map(i->new IncludeFrame(i.includingFile(),i.requestedName(),i.includedFile(),i.includeLine())).toList(),p.exact()),SourceDependencyInventory.Operation.valueOf(fact.operation()),SourceDependencyInventory.Access.valueOf(fact.access())));
        }
        var result=new LinkedHashMap<ResolutionContracts.ProgramUnitId,SourceDependencyInventory>();
        for(var entry:collected.entrySet()) {
            var gaps=new TreeSet<String>(sourceGaps);
            for(var f:entry.getValue()) {
                if(f.resolution()!=SourceDependencyInventory.Resolution.RESOLVED&&f.resolution()!=SourceDependencyInventory.Resolution.NOT_APPLICABLE)gaps.add("SOURCE_ARTIFACT_"+f.resolution());
                if(f.authority().equals("UNKNOWN"))gaps.add("SQL_INCLUDE_CLASSIFICATION_UNKNOWN");
            }
            result.put(entry.getKey(),new SourceDependencyInventory(gaps.isEmpty()?Availability.KNOWN:Availability.PARTIAL,entry.getValue(),List.copyOf(gaps)));
        }
        return Map.copyOf(result);
    }
    private static long position(int line,int column){return ((long)line<<32)+(column&0xffffffffL);}
    private static long end(Ast.SourceLocation p){return position(p.endLine(),p.endColumn());}
    private static Location location(Ast.SourceLocation p){return new Location(p.file(),p.startLine(),p.startColumn(),p.endLine(),p.endColumn());}
}
