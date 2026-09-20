package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Canonical procedure transfers; ordinal occurrence order is never sorted or deduplicated. */
public final class GoToSemantics {
    public record Target(ResolutionContracts.SemanticEntityId identity, Ast.SourceProvenance paragraphOrigin) { }
    public record Facts(Optional<Target> target, Ast.SourceProvenance referenceOrigin,
                        Optional<Integer> entry, Optional<Ast.SourceProvenance> entryOrigin, List<String> gaps) {
        public Facts { gaps=List.copyOf(gaps); }
    }
    public record Destination(int ordinal, Facts proof) { }
    public record ConditionalFacts(List<Destination> destinations,
            Optional<ResolutionContracts.SemanticEntityId> integerSelector,
            Ast.SourceProvenance selectorOrigin, Optional<Integer> continuation,
            Ast.SourceProvenance continuationOrigin, List<String> gaps) {
        public ConditionalFacts { destinations=List.copyOf(destinations);gaps=List.copyOf(gaps); }
        public boolean closed() { return !destinations.isEmpty() && continuation.isPresent()
            && destinations.stream().allMatch(d->d.proof().entry().isPresent()); }
    }
    private final Map<ScalarMoveSemantics.NodeKey,Facts> facts;
    private final Map<ScalarMoveSemantics.NodeKey,ConditionalFacts> conditional;
    private final long resolutionAttempts;
    private GoToSemantics(Map<ScalarMoveSemantics.NodeKey,Facts> facts,
            Map<ScalarMoveSemantics.NodeKey,ConditionalFacts> conditional,long resolutionAttempts) {
        this.facts=Map.copyOf(facts);this.conditional=Map.copyOf(conditional);this.resolutionAttempts=resolutionAttempts;
    }
    public long resolutionAttempts() { return resolutionAttempts; }
    public Set<ResolutionContracts.ProgramUnitId> conditionalUnits() {
        var units=new HashSet<ResolutionContracts.ProgramUnitId>();
        for(var key:conditional.keySet())units.add(key.unit());
        return Set.copyOf(units);
    }
    public Facts fact(ResolutionContracts.ProgramUnitId unit,int statement) {
        return Objects.requireNonNull(facts.get(new ScalarMoveSemantics.NodeKey(unit,statement)));
    }
    public ConditionalFacts conditionalFact(ResolutionContracts.ProgramUnitId unit,int statement) {
        return Objects.requireNonNull(conditional.get(new ScalarMoveSemantics.NodeKey(unit,statement)));
    }
    public static boolean simple(Ast.GoToStatement g) {
        return g.goToKind()==Ast.GoToKind.SIMPLE && g.targets().size()==1 && g.dependingOn()==null;
    }
    public static boolean depending(Ast.GoToStatement g) { return g.goToKind()==Ast.GoToKind.DEPENDING_ON; }
    /** Known candidates survive an open peer. Closure is tested separately. */
    public List<Integer> entries(ResolutionContracts.ProgramUnitId unit,Ast.GoToStatement g) {
        if(simple(g))return fact(unit,g.meta().id()).entry().stream().toList();
        if(depending(g))return conditionalFact(unit,g.meta().id()).destinations().stream().flatMap(d->d.proof().entry().stream()).toList();
        return List.of();
    }
    public boolean closed(ResolutionContracts.ProgramUnitId unit,Ast.GoToStatement g) {
        return simple(g)?fact(unit,g.meta().id()).entry().isPresent():depending(g)&&conditionalFact(unit,g.meta().id()).closed();
    }
    static GoToSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,
            ReferenceResolution resolution,ResolutionAnalysisReport report,NumericControlSemantics numbers) {
        var refs=new HashMap<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry>();
        for(var ref:resolution.entries()) refs.put(new ScalarMoveSemantics.NodeKey(ref.occurrence().programUnitId(),ref.occurrence().referenceAstNodeId()),ref);
        var result=new HashMap<ScalarMoveSemantics.NodeKey,Facts>();
        var conditional=new HashMap<ScalarMoveSemantics.NodeKey,ConditionalFacts>();long attempts=0;
        for(var unit:frontend.compilationUnit().programUnits()) {
            var nodes=new HashMap<Integer,Ast.Node>(); var transfers=new ArrayList<Ast.GoToStatement>();
            var pending=new ArrayDeque<Ast.Node>(); pending.push(unit.program()); boolean altered=false;
            while(!pending.isEmpty()) {
                var node=pending.pop(); nodes.put(node.meta().id(),node);
                if(node instanceof Ast.GoToStatement g)transfers.add(g);
                if(node instanceof Ast.Statement && node.meta().origin().grammarRule().equals("alterStatement"))altered=true;
                for(var child:Ast.children(node))pending.push(child);
            }
            var symbols=new HashMap<Integer,SymbolTable.Symbol>();
            for(var symbol:tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols())symbols.put(symbol.id(),symbol);
            var procedures=unit.program().divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).toList();
            boolean input=procedures.size()==1 && procedures.get(0).procedureEntry()
                .filter(e->!e.declarativesPresent() && e.inputProof().unaffectedBy(report.frontendState())).isPresent();
            for(var g:transfers) {
                if(!simple(g)&&!depending(g))continue;
                var global=new ArrayList<String>();
                if(!input)global.add("GO_TO_PROCEDURE_INPUT_NOT_PROVEN");
                if(altered)global.add("GO_TO_ALTER_IN_UNIT");
                var destinations=new ArrayList<Destination>();
                for(var source:g.targets()) {
                    attempts++;
                    destinations.add(new Destination(destinations.size(),resolve(unit.id(),g,source,refs,symbols,nodes,global,depending(g))));
                }
                var key=new ScalarMoveSemantics.NodeKey(unit.id(),g.meta().id());
                if(simple(g)){result.put(key,destinations.get(0).proof());continue;}
                // IBM profile qualification only: retain every parser-observed occurrence above it.
                if(g.targets().size()>255)global.add("GO_TO_OUTSIDE_IBM_6_4_PROFILE");
                if(g.targets().isEmpty())global.add("GO_TO_DESTINATIONS_UNAVAILABLE");
                var selector=numbers.whole(g.dependingOn(),unit.id(),refs);
                if(selector.isEmpty())global.add("GO_TO_SELECTOR_INTEGER_NOT_PROVEN");
                var next=input?Optional.ofNullable(procedures.get(0).ordinaryContinuations().get(g.meta().id())):Optional.<Integer>empty();
                var nextNode=next.map(nodes::get).filter(Ast.Statement.class::isInstance).filter(n->n.meta().provenance().exact());
                if(nextNode.isEmpty()){next=Optional.empty();global.add("GO_TO_NORMAL_CONTINUATION_NOT_PROVEN");}
                conditional.put(key,new ConditionalFacts(destinations,selector,g.dependingOn()==null?g.meta().provenance():g.dependingOn().meta().provenance(),
                    next,nextNode.map(n->n.meta().provenance()).orElse(g.meta().provenance()),global));
            }
        }
        return new GoToSemantics(result,conditional,attempts);
    }
    private static Facts resolve(ResolutionContracts.ProgramUnitId unit,Ast.GoToStatement g,Ast.ProcedureReference source,
            Map<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry> refs,Map<Integer,SymbolTable.Symbol> symbols,
            Map<Integer,Ast.Node> nodes,List<String> global,boolean retainSection) {
        var gaps=new ArrayList<>(global);var ref=refs.get(new ScalarMoveSemantics.NodeKey(unit,source.meta().id()));
        Ast.Paragraph paragraph=null;Target target=null;
        if(ref!=null && ref.occurrence().role()==ResolutionContracts.ReferenceRole.GO_TO_TARGET
                && ref.status()==ResolutionContracts.ResolutionStatus.RESOLVED && ref.candidates().size()==1 && ref.selectedCandidate().isPresent()) {
            var id=ref.selectedCandidate().orElseThrow().entityId();var symbol=symbols.get(id.localId());
            if(id.programUnitId().equals(unit)&&id.domain()==ResolutionContracts.SemanticEntityDomain.PROCEDURE_SYMBOL
                    &&symbol!=null&&symbol.namespace()==SymbolTable.Namespace.PROCEDURE) {
                var node=nodes.get(symbol.declarationAstNodeId());
                if(symbol.kind()==SymbolTable.SymbolKind.PARAGRAPH&&node instanceof Ast.Paragraph p){paragraph=p;target=new Target(id,p.meta().provenance());}
                else if(retainSection&&node instanceof Ast.Section s){target=new Target(id,s.meta().provenance());gaps.add("GO_TO_SECTION_ENTRY_NOT_PROVEN");}
            }
        }
        if(target==null)gaps.add("GO_TO_TARGET_NOT_UNIQUE_LOCAL_PARAGRAPH");
        var entry=paragraph==null?Optional.<Integer>empty():paragraph.executableEntry();
        var executable=entry.map(nodes::get).filter(Ast.Statement.class::isInstance);
        if(executable.isEmpty())gaps.add("GO_TO_TARGET_ENTRY_UNAVAILABLE");
        if(!g.meta().provenance().exact()||!source.meta().provenance().exact()
                ||target!=null&&!target.paragraphOrigin().exact()
                ||executable.filter(n->!n.meta().provenance().exact()).isPresent())gaps.add("GO_TO_PROVENANCE_INCOMPLETE");
        boolean entryExact=executable.isPresent()&&source.meta().provenance().exact()
            &&g.meta().provenance().exact()&&target!=null&&target.paragraphOrigin().exact()
            &&executable.orElseThrow().meta().provenance().exact();
        var knownEntry=entryExact?entry:Optional.<Integer>empty();
        return new Facts(Optional.ofNullable(target),source.meta().provenance(),knownEntry,
            knownEntry.flatMap(id->executable.map(n->n.meta().provenance())),gaps);
    }
}
