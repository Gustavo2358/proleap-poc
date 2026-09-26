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
        // A Program provenance may collapse to its PROGRAM-ID line when its body
        // crosses COPY expansions. Bound each owner by proved original AST nodes.
        var extents=new ArrayList<SourceExtent>();
        for(var unit:units) {
            collected.put(unit.id(),new ArrayList<>());
            var start=unit.program().meta().provenance().original();
            extents.add(new SourceExtent(unit,start.file(),position(start.startLine(),start.startColumn()),
                maxOriginalEnd(unit.program(),start.file()),unit.id().structuralPath().size()));
        }
        for(var fact:facts) {
            var source=fact.rootSite();var first=position(source.startLine(),source.startColumn());
            var last=position(source.endLine(),source.endColumn());
            CompilationUnitModel.ProgramUnit owner=null;int depth=-1;boolean ambiguous=false;
            for(var extent:extents) {
                if(!extent.file().equals(source.file())||first<extent.start()||last>extent.end())continue;
                if(extent.depth()>depth) {owner=extent.unit();depth=extent.depth();ambiguous=false;}
                else if(extent.depth()==depth) ambiguous=true;
            }
            // A root COPY can contain the complete program, so no program node
            // has a provenance location in the including file. Match the proved
            // include frame to the captured root occurrence.
            if(owner==null&&!ambiguous) {
                for(var unit:units) {
                    if(!matchesRootCopy(fact,unit))continue;
                    if(owner!=null) {ambiguous=true;break;}
                    owner=unit;
                }
            }
            // A trailing COPY may be the last source construct and leave no AST node
            // after it. With exactly one root program in that physical source, its
            // ownership is unique even when the directive itself was preprocessed away.
            if(owner==null&&!ambiguous&&extents.size()==1) {
                var only=extents.get(0);
                if(only.file().equals(source.file())&&first>=only.start())owner=only.unit();
            }
            if(owner==null||ambiguous)throw new IllegalArgumentException("SOURCE_DEPENDENCY_OWNER_UNPROVED: nominal occurrence cannot be assigned safely");
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
    private static boolean matchesRootCopy(SourceDependencyFact fact,CompilationUnitModel.ProgramUnit unit) {
        var chain=unit.program().meta().provenance().includeChain();
        if(chain.isEmpty())return false;
        var root=chain.get(0);
        var site=fact.rootSite();
        if(!root.includingFile().equals(site.file())||root.includeLine()!=site.startLine())return false;
        var factChain=fact.provenance().includeChain();
        return factChain.isEmpty()
            ? fact.kind()==SourceDependencyFact.Kind.COPYBOOK&&root.requestedName().equals(fact.name())
            : root.equals(factChain.get(0));
    }
    private record SourceExtent(CompilationUnitModel.ProgramUnit unit,String file,long start,long end,int depth) {}
    private static long maxOriginalEnd(Ast.Node node,String file) {
        var place=node.meta().provenance().original();
        long end=place.file().equals(file)?position(place.endLine(),place.endColumn()):Long.MIN_VALUE;
        for(var child:Ast.children(node))end=Math.max(end,maxOriginalEnd(child,file));
        return end;
    }
    private static long position(int line,int column){return ((long)line<<32)+(column&0xffffffffL);}
    private static Location location(Ast.SourceLocation p){return new Location(p.file(),p.startLine(),p.startColumn(),p.endLine(),p.endColumn());}
}
