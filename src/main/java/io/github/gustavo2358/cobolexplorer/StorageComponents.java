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
    public enum UncertaintyScope { DECLARATION, RECORD, UNIT }
    public enum Dimension { LAYOUT, ALLOCATION, ALIAS, LIFETIME }
    public enum Reason { NONLOCAL_VISIBILITY, UNINTERPRETED_DATA_CLAUSE, UNPROVED_OVERLAY }
    public record Uncertainty(int owner,int root,UncertaintyScope scope,Set<Dimension> dimensions,
                              Reason reason,Ast.SourceProvenance origin) {
        public Uncertainty { dimensions=Set.copyOf(dimensions);Objects.requireNonNull(scope);Objects.requireNonNull(reason);Objects.requireNonNull(origin); }
    }
    public record AllocationAssessment(List<Uncertainty> unitRemainder,List<Uncertainty> rootRemainder) {
        public AllocationAssessment { unitRemainder=List.copyOf(unitRemainder);rootRemainder=List.copyOf(rootRemainder); }
        public boolean proved() { return unitRemainder.isEmpty()&&rootRemainder.isEmpty(); }
    }
    public record UncertaintyIndex(List<Uncertainty> records,List<Uncertainty> unitAllocation,Map<Integer,List<Uncertainty>> rootAllocation) {
        public UncertaintyIndex {
            records=List.copyOf(records);unitAllocation=List.copyOf(unitAllocation);
            var roots=new HashMap<Integer,List<Uncertainty>>();rootAllocation.forEach((key,value)->roots.put(key,List.copyOf(value)));rootAllocation=Map.copyOf(roots);
        }
        static UncertaintyIndex of(List<Uncertainty> records) {
            var broad=new ArrayList<Uncertainty>();var roots=new HashMap<Integer,List<Uncertainty>>();
            for(var u:records)if(u.dimensions().contains(Dimension.ALLOCATION)) {
                if(u.scope()==UncertaintyScope.UNIT)broad.add(u);else roots.computeIfAbsent(u.root(),ignored->new ArrayList<>()).add(u);
            }
            roots.replaceAll((key,value)->List.copyOf(value));return new UncertaintyIndex(records,broad,roots);
        }
        AllocationAssessment allocation(int root) { return new AllocationAssessment(unitAllocation,rootAllocation.getOrDefault(root,List.of())); }
    }
    public record Unit(List<Position> positions, List<Ast.DataEntry> roots,
                       Map<Integer,Component> componentOf, Map<Integer,List<Component>> children,
                       List<Component> rootComponents, List<Relation> relations, List<Position> renames,
                       boolean structureProven, UncertaintyIndex uncertaintyIndex, boolean relationsProven,
                       boolean rootRelationsProven,Set<Integer> uncertainRoots) {
        public Unit {
            uncertainRoots=Set.copyOf(uncertainRoots);positions=List.copyOf(positions);roots=List.copyOf(roots);componentOf=Map.copyOf(componentOf);
            var copy=new HashMap<Integer,List<Component>>();children.forEach((k,v)->copy.put(k,List.copyOf(v)));children=Map.copyOf(copy);
            rootComponents=List.copyOf(rootComponents);relations=List.copyOf(relations);renames=List.copyOf(renames);
        }
        public List<Uncertainty> uncertainties() { return uncertaintyIndex.records(); }
        public AllocationAssessment allocation(int root) {
            var component=componentOf.get(root);
            if(component==null||component.parent().isPresent())throw new IllegalArgumentException("allocation requires a declared root");
            return uncertaintyIndex.allocation(root);
        }
        /** No separate Cell is safe for any root participating in an overlay, including a later declaration. */
        public boolean standaloneIndependent(int node) {
            var c=componentOf.get(node);
            return structureProven&&c!=null&&c.parent().isEmpty()&&allocation(node).proved()&&rootRelationsProven&&!uncertainRoots.contains(node)&&c.members().size()==1
                &&renames.stream().noneMatch(p->p.root()==node);
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
            var positions=new ArrayList<Position>();var renames=new ArrayList<Position>();var pending=new ArrayDeque<Position>();
            for(int i=roots.size()-1;i>=0;i--)pending.push(new Position(roots.get(i),Optional.empty(),i,roots.get(i).meta().id()));
            var identities=new HashSet<Integer>();var uncertainties=new ArrayList<Uncertainty>();
            while(!pending.isEmpty()) {
                var p=pending.pop();var data=p.data();
                if(data.levelKind()==Ast.DataLevelKind.RENAMES_66&&p.parent().isPresent()){renames.add(p);continue;}
                positions.add(p);
                if(!identities.add(data.meta().id()))throw new IllegalArgumentException("duplicate physical declaration identity");
                if(data.visibility()!=Ast.DeclarationVisibility.LOCAL)uncertainties.add(new Uncertainty(data.meta().id(),p.root(),UncertaintyScope.UNIT,
                    Set.of(Dimension.ALLOCATION,Dimension.ALIAS,Dimension.LIFETIME),Reason.NONLOCAL_VISIBILITY,data.meta().provenance()));
                for(var clause:data.clauses())if(clause instanceof Ast.RenamesClause||clause instanceof Ast.PreservedDataClause) {
                    // The declaration owns the layout gap; absent an alias bound, separation
                    // remains open for the unit. Neither record controls source VALUE support.
                    uncertainties.add(new Uncertainty(data.meta().id(),p.root(),UncertaintyScope.DECLARATION,
                        Set.of(Dimension.LAYOUT),Reason.UNINTERPRETED_DATA_CLAUSE,clause.meta().provenance()));
                    uncertainties.add(new Uncertainty(data.meta().id(),p.root(),UncertaintyScope.UNIT,
                        Set.of(Dimension.ALLOCATION,Dimension.ALIAS,Dimension.LIFETIME),Reason.UNINTERPRETED_DATA_CLAUSE,clause.meta().provenance()));
                }
                for(int i=data.children().size()-1;i>=0;i--)pending.push(new Position(data.children().get(i),Optional.of(data.meta().id()),i,p.root()));
            }
            var byNode=new HashMap<Integer,Component>();var children=new HashMap<Integer,List<Component>>();var relations=new ArrayList<Relation>();
            var rootComponents=components(roots,Optional.empty(),coverage,byNode,relations);
            for(var p:positions)children.put(p.data().meta().id(),components(p.data().children(),Optional.of(p.data().meta().id()),coverage,byNode,relations));
            boolean proved=relations.stream().allMatch(Relation::proved);
            var positionsByNode=new HashMap<Integer,Position>();positions.forEach(p->positionsByNode.put(p.data().meta().id(),p));
            var uncertainRoots=new HashSet<Integer>();boolean rootRelations=true;
            for(var relation:relations)if(!relation.proved()) {
                var owner=positionsByNode.get(relation.owner());
                if(owner.parent().isEmpty()) {
                    rootRelations=false;
                    uncertainties.add(new Uncertainty(relation.owner(),owner.root(),UncertaintyScope.UNIT,
                        Set.of(Dimension.LAYOUT,Dimension.ALLOCATION,Dimension.ALIAS),Reason.UNPROVED_OVERLAY,relation.clause().meta().provenance()));
                } else {
                    uncertainRoots.add(owner.root());
                    uncertainties.add(new Uncertainty(relation.owner(),owner.root(),UncertaintyScope.RECORD,
                        Set.of(Dimension.LAYOUT,Dimension.ALIAS),Reason.UNPROVED_OVERLAY,relation.clause().meta().provenance()));
                }
            }
            units.put(unit.id(),new Unit(positions,roots,byNode,children,rootComponents,relations,renames,structure,UncertaintyIndex.of(uncertainties),proved,rootRelations,uncertainRoots));
        }
        return new StorageComponents(frontend,units);
    }
    private static List<Component> components(List<Ast.DataEntry> siblings,Optional<Integer> parent,
            Map<Integer,SemanticCoverage.Finding> coverage,Map<Integer,Component> byNode,List<Relation> relations) {
        var result=new ArrayList<Component>();var members=new ArrayList<Integer>();
        var names=new HashMap<String,Ast.DataEntry>();var ambiguous=new HashSet<String>();
        Ast.DataEntry previous=null;
        for(var data:siblings) {
            if(data.levelKind()==Ast.DataLevelKind.RENAMES_66&&parent.isPresent())continue;
            var redefines=data.clauses().stream().filter(Ast.RedefinesClause.class::isInstance).map(Ast.RedefinesClause.class::cast).toList();
            Ast.DataEntry selected=null;boolean proved=false;
            if(redefines.size()==1) {
                var clause=redefines.get(0);var ref=clause.target();var name=SymbolTable.canonical(ref.baseName());
                selected=ambiguous.contains(name)?null:names.get(name);
                proved=selected!=null&&levelsProven(data,selected,previous,parent)
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
            previous=data;
        }
        if(!members.isEmpty())finish(members,parent,byNode,result);
        return List.copyOf(result);
    }
    private static boolean levelsProven(Ast.DataEntry owner,Ast.DataEntry target,Ast.DataEntry previous,Optional<Integer> parent) {
        int o=level(owner),t=level(target),p=level(previous);
        if(parent.isEmpty())return o==t&&(o==1||o==77);
        // AST siblings can have different written numbers. In a proved nonincreasing
        // component, the previous member is its minimum; no scan per target is needed.
        return o>=2&&o<=49&&t>=2&&t<=49&&p>=o&&(previous==target||p>=t);
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
