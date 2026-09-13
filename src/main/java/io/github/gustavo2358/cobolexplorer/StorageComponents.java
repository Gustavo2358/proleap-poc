package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Source allocation proof shared by physical layout and legacy scalar eligibility.
 * Relations resolve only inside the preceding contiguous sibling component.
 * Nominal visibility outside that component cannot prove a REDEFINES location. */
public final class StorageComponents {
    public record Position(Ast.DataEntry data, Optional<Integer> parent, int order, int root) { }
    public record Component(int representative, Optional<Integer> parent, List<Integer> members) {
        public Component { members=List.copyOf(members); }
    }
    public record Relation(int owner, Optional<Integer> target, Ast.RedefinesClause clause, boolean proved) { }
    public record Unit(List<Position> positions, List<Ast.DataEntry> roots,
                       Map<Integer,Component> componentOf, Map<Integer,List<Component>> children,
                       List<Component> rootComponents, List<Relation> relations,
                       boolean structureProven, boolean allocationProven, boolean relationsProven) {
        public Unit {
            positions=List.copyOf(positions);roots=List.copyOf(roots);componentOf=Map.copyOf(componentOf);
            var copy=new HashMap<Integer,List<Component>>();children.forEach((k,v)->copy.put(k,List.copyOf(v)));children=Map.copyOf(copy);
            rootComponents=List.copyOf(rootComponents);relations=List.copyOf(relations);
        }
        /** No separate Cell is safe for any root participating in an overlay, including a later declaration. */
        public boolean standaloneIndependent(int node) {
            var c=componentOf.get(node);
            return structureProven&&allocationProven&&relationsProven&&c!=null&&c.parent().isEmpty()&&c.members().size()==1;
        }
    }
    private final CompilationUnitBuildResult owner;
    private final Map<ResolutionContracts.ProgramUnitId,Unit> units;
    private StorageComponents(CompilationUnitBuildResult owner,Map<ResolutionContracts.ProgramUnitId,Unit> units) { this.owner=owner;this.units=Map.copyOf(units); }
    boolean belongsTo(CompilationUnitBuildResult frontend) { return owner==frontend; }
    public Unit unit(ResolutionContracts.ProgramUnitId unit) { return Objects.requireNonNull(units.get(unit),"foreign unit"); }
    public static StorageComponents analyze(CompilationUnitBuildResult frontend) {
        var units=new LinkedHashMap<ResolutionContracts.ProgramUnitId,Unit>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var sections=new ArrayList<Ast.Section>();
            for(var division:unit.program().divisions())if(division.divisionKind()==Ast.DivisionKind.DATA)
                for(var child:division.children())if(child instanceof Ast.Section s&&s.dataSectionKind()==Ast.DataSectionKind.WORKING_STORAGE)sections.add(s);
            var roots=new ArrayList<Ast.DataEntry>();boolean structure=sections.size()==1;
            for(var section:sections)for(var child:section.children()) {
                if(child instanceof Ast.DataEntry d){roots.add(d);structure&=level(d)==1||level(d)==77;}
                else structure=false;
            }
            var coverage=new HashMap<Integer,SemanticCoverage.Finding>();
            var report=frontend.coverageByProgramUnit().get(unit.id());
            if(report==null)structure=false;else for(var f:report.findings())coverage.put(f.astNodeId(),f);
            var positions=new ArrayList<Position>();var pending=new ArrayDeque<Position>();
            for(int i=roots.size()-1;i>=0;i--)pending.push(new Position(roots.get(i),Optional.empty(),i,roots.get(i).meta().id()));
            var identities=new HashSet<Integer>();boolean allocation=true;
            while(!pending.isEmpty()) {
                var p=pending.pop();var data=p.data();positions.add(p);
                if(!identities.add(data.meta().id()))throw new IllegalArgumentException("duplicate physical declaration identity");
                allocation&=data.visibility()==Ast.DeclarationVisibility.LOCAL;
                for(var clause:data.clauses())if(clause instanceof Ast.RenamesClause||clause instanceof Ast.PreservedDataClause)allocation=false;
                for(int i=data.children().size()-1;i>=0;i--)pending.push(new Position(data.children().get(i),Optional.of(data.meta().id()),i,p.root()));
            }
            var byNode=new HashMap<Integer,Component>();var children=new HashMap<Integer,List<Component>>();var relations=new ArrayList<Relation>();
            var rootComponents=components(roots,Optional.empty(),coverage,byNode,relations);
            for(var p:positions)children.put(p.data().meta().id(),components(p.data().children(),Optional.of(p.data().meta().id()),coverage,byNode,relations));
            boolean proved=relations.stream().allMatch(Relation::proved);
            units.put(unit.id(),new Unit(positions,roots,byNode,children,rootComponents,relations,structure,allocation,proved));
        }
        return new StorageComponents(frontend,units);
    }
    private static List<Component> components(List<Ast.DataEntry> siblings,Optional<Integer> parent,
            Map<Integer,SemanticCoverage.Finding> coverage,Map<Integer,Component> byNode,List<Relation> relations) {
        var result=new ArrayList<Component>();var members=new ArrayList<Integer>();
        var names=new HashMap<String,Ast.DataEntry>();var ambiguous=new HashSet<String>();
        for(var data:siblings) {
            var redefines=data.clauses().stream().filter(Ast.RedefinesClause.class::isInstance).map(Ast.RedefinesClause.class::cast).toList();
            Ast.DataEntry selected=null;boolean proved=false;
            if(redefines.size()==1) {
                var clause=redefines.get(0);var ref=clause.target();var name=SymbolTable.canonical(ref.baseName());
                selected=ambiguous.contains(name)?null:names.get(name);
                proved=selected!=null&&level(data)==level(selected)&&level(data)!=66&&level(data)!=88
                    &&data.clauses().get(0)==clause&&ref.understanding()==Ast.ReferenceUnderstanding.STRUCTURED
                    &&ref.qualifiers().isEmpty()&&ref.subscriptGroups().isEmpty()&&ref.referenceModification()==null
                    &&modeled(data,coverage)&&modeled(clause,coverage)&&modeled(selected,coverage);
            }
            for(var clause:redefines)relations.add(new Relation(data.meta().id(),proved?Optional.of(selected.meta().id()):Optional.empty(),clause,proved));
            if(!proved&&!members.isEmpty()) { finish(members,parent,byNode,result);members.clear();names.clear();ambiguous.clear(); }
            members.add(data.meta().id());
            if(!data.filler()&&!data.name().isBlank()) {
                var name=SymbolTable.canonical(data.name());if(names.putIfAbsent(name,data)!=null)ambiguous.add(name);
            }
        }
        if(!members.isEmpty())finish(members,parent,byNode,result);
        return List.copyOf(result);
    }
    private static void finish(List<Integer> members,Optional<Integer> parent,Map<Integer,Component> byNode,List<Component> result) {
        var component=new Component(members.get(0),parent,members);result.add(component);
        for(var member:members)byNode.put(member,component);
    }
    private static boolean modeled(Ast.Node node,Map<Integer,SemanticCoverage.Finding> coverage) {
        var f=coverage.get(node.meta().id());return f!=null&&f.coverage()==SemanticCoverage.ConstructionCoverage.MODELED;
    }
    static int level(Ast.DataEntry data) { try{return Integer.parseInt(data.level());}catch(NumberFormatException invalid){return -1;} }
}
