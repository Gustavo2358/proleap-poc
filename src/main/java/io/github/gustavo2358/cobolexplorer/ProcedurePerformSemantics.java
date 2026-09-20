package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Typed paragraph-range proof; inventory order never supplies executable edges. */
public final class ProcedurePerformSemantics {
    public record Paragraph(ResolutionContracts.SemanticEntityId identity, int entry, List<Integer> statements,
                            List<Integer> completions, Ast.SourceProvenance origin) {
        public Paragraph { statements=List.copyOf(statements); completions=List.copyOf(completions); }
    }
    public record Endpoint(ResolutionContracts.SemanticEntityId identity, Ast.SourceProvenance referenceOrigin,
                           Ast.SourceProvenance paragraphOrigin) { }
    public record Loop(Ast.PerformTestMode testMode, Optional<Ast.Expression> condition, PerformPredicateSemantics.Predicate predicate) { }
    public record Count(Optional<Ast.Expression> expression,Optional<java.math.BigInteger> integer,
                        Optional<ResolutionContracts.SemanticEntityId> wholeItem,boolean proven) { }
    public record VaryingOperand(Ast.PerformControl control,Optional<java.math.BigInteger> integer,Optional<ResolutionContracts.SemanticEntityId> wholeItem) { }
    public record Varying(int levels,List<VaryingOperand> controls) { public Varying {controls=List.copyOf(controls);} }
    public record Facts(Optional<Endpoint> start, Optional<Endpoint> end, List<Paragraph> procedures,
                        Optional<Integer> resume, Ast.SourceProvenance resumeOrigin, Optional<Loop> loop, Optional<Count> times, Optional<Varying> varying,List<String> gaps) {
        public Facts { procedures=List.copyOf(procedures); gaps=List.copyOf(gaps); }
        public boolean precise() { return gaps.isEmpty(); }
        public boolean structureKnown() { return gaps.stream().allMatch("PERFORM_UNTIL_PREDICATE_NOT_PROVEN"::equals); }
    }
    private final Map<ScalarMoveSemantics.NodeKey,Facts> facts;
    private final Set<ScalarMoveSemantics.NodeKey> completions;
    private final Set<ScalarMoveSemantics.NodeKey> paragraphEnds;
    private ProcedurePerformSemantics(Map<ScalarMoveSemantics.NodeKey,Facts> facts) {
        this.facts=Map.copyOf(facts);var ends=new HashSet<ScalarMoveSemantics.NodeKey>();
        facts.forEach((k,v)->{if(v.structureKnown())for(var p:v.procedures())for(var id:p.completions())ends.add(new ScalarMoveSemantics.NodeKey(k.unit(),id));});
        completions=Set.copyOf(ends);
        var boundaries=new HashSet<ScalarMoveSemantics.NodeKey>();
        facts.forEach((k,v)->v.procedures().forEach(p->p.completions().forEach(id->boundaries.add(new ScalarMoveSemantics.NodeKey(k.unit(),id)))));
        paragraphEnds=Set.copyOf(boundaries);
    }
    boolean hasPartial(ResolutionContracts.ProgramUnitId unit) {
        return facts.entrySet().stream().anyMatch(e->e.getKey().unit().equals(unit)&&!e.getValue().structureKnown());
    }
    public Optional<Facts> fact(ResolutionContracts.ProgramUnitId unit,int id) { return Optional.ofNullable(facts.get(new ScalarMoveSemantics.NodeKey(unit,id))); }
    public boolean paragraphEnd(ResolutionContracts.ProgramUnitId unit,int id) { return paragraphEnds.contains(new ScalarMoveSemantics.NodeKey(unit,id)); }
    public boolean completion(ResolutionContracts.ProgramUnitId unit,int id) { return completions.contains(new ScalarMoveSemantics.NodeKey(unit,id)); }
    public static boolean applicable(Ast.PerformStatement p) {
        return p.performKind()==Ast.PerformKind.PROCEDURE && p.inlineBody().isEmpty()
            && (p.repetition()==Ast.PerformRepetition.UNTIL || p.repetition()==Ast.PerformRepetition.TIMES || p.repetition()==Ast.PerformRepetition.VARYING || p.throughReference()!=null && p.repetition()==Ast.PerformRepetition.ONCE);
    }
    static ProcedurePerformSemantics analyze(CompilationUnitBuildResult frontend,CompilationUnitSymbolTables tables,
            ReferenceResolution resolution,ResolutionAnalysisReport report,
            Map<ResolutionContracts.SemanticEntityId,ScalarMoveSemantics.ScalarText> scalars,
            Map<ScalarMoveSemantics.NodeKey,ScalarMoveSemantics.Move> moves,IfSemantics ifs,
            EvaluateSemantics evaluates,GoToSemantics goTos,PerformSemantics basic,NumericControlSemantics numbers,CicsProgramControlAnalyzer.Contribution cics) {
        var result=new HashMap<ScalarMoveSemantics.NodeKey,Facts>();
        var refs=new HashMap<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry>();
        for(var ref:resolution.entries())refs.put(new ScalarMoveSemantics.NodeKey(ref.occurrence().programUnitId(),ref.occurrence().referenceAstNodeId()),ref);
        for(var unit:frontend.compilationUnit().programUnits()) {
            boolean complete=report.inputComplete(unit.id());
            var coverage=new HashMap<Integer,SemanticCoverage.Finding>();
            for(var finding:frontend.coverageByProgramUnit().get(unit.id()).findings())coverage.put(finding.astNodeId(),finding);
            var nodes=new HashMap<Integer,Ast.Node>();var pending=new ArrayDeque<Ast.Node>();pending.push(unit.program());
            while(!pending.isEmpty()) {var n=pending.pop();nodes.put(n.meta().id(),n);Ast.children(n).forEach(pending::push);}
            var symbols=new HashMap<Integer,SymbolTable.Symbol>();var identities=new HashMap<Integer,ResolutionContracts.SemanticEntityId>();
            for(var s:tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols()) {
                symbols.put(s.id(),s);
                if(s.kind()==SymbolTable.SymbolKind.PARAGRAPH)identities.put(s.declarationAstNodeId(),new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.PROCEDURE_SYMBOL,s.id()));
            }
            var divisions=unit.program().divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).toList();
            var division=divisions.size()==1?divisions.get(0):null;
            var paragraphs=new ArrayList<Ast.Paragraph>();
            boolean structure=division!=null && division.procedureEntry().isPresent()
                && !division.procedureEntry().orElseThrow().declarativesPresent() && !division.procedureEntry().orElseThrow().signatureClausesPresent()
                && division.children().stream().allMatch(Ast.Paragraph.class::isInstance);
            if(structure)for(var child:division.children())paragraphs.add((Ast.Paragraph)child);
            var next=new HashMap<Integer,Integer>();
            if(division!=null){next.putAll(division.normalContinuations());division.embeddedContinuations().forEach((from,to)->{if(cics.boundedLocal(unit.id(),nodes.get(from)))next.put(from,to);});}
            var provisional=new LinkedHashMap<Integer,Facts>();
            var performs=nodes.values().stream().filter(Ast.PerformStatement.class::isInstance).map(Ast.PerformStatement.class::cast)
                .filter(ProcedurePerformSemantics::applicable).sorted(Comparator.comparingInt(p->p.meta().id())).toList();
            for(var p:performs) {
                var gaps=new LinkedHashSet<String>();if(!complete)gaps.add("PERFORM_INPUT_INCOMPLETE");
                if(!structure)gaps.add("PERFORM_PROCEDURE_STRUCTURE_NOT_PROVEN");
                var start=endpoint(p.fromReference(),unit.id(),refs,symbols,nodes);
                var end=endpoint(p.throughReference()==null?p.fromReference():p.throughReference(),unit.id(),refs,symbols,nodes);
                if(start.isEmpty()||end.isEmpty())gaps.add("PERFORM_ENDPOINT_NOT_UNIQUE_LOCAL_PARAGRAPH");
                int first=index(paragraphs,start,identities),last=index(paragraphs,end,identities);
                var range=new ArrayList<Paragraph>();
                if(first<0||last<first)gaps.add("PERFORM_ORDERED_RANGE_NOT_PROVEN");
                else for(int i=first;i<=last;i++) {
                    var paragraph=paragraphs.get(i);var roots=direct(paragraph);var members=members(roots);
                    if(roots.isEmpty()||paragraph.executableEntry().isEmpty()||paragraph.executableEntry().get()!=roots.get(0).meta().id()
                            || !cics.boundedRegion(unit.id(),paragraph)) {gaps.add("PERFORM_PARAGRAPH_BOUNDARY_NOT_PROVEN");break;}
                    var completions=completions(roots,unit.id(),cics,evaluates);
                    range.add(new Paragraph(identities.get(paragraph.meta().id()),roots.get(0).meta().id(),
                        members.stream().map(s->s.meta().id()).toList(),completions,paragraph.meta().provenance()));
                }
                if(range.size()!=Math.max(0,last-first+1))range.clear();
                var resume=Optional.ofNullable(next.get(p.meta().id()));
                if(resume.isEmpty())gaps.add("PERFORM_RESUME_NOT_PROVEN");
                if(!p.meta().provenance().exact())gaps.add("PERFORM_PROVENANCE_INCOMPLETE");
                Optional<Loop> loop=Optional.empty();
                if(p.repetition()==Ast.PerformRepetition.UNTIL||p.repetition()==Ast.PerformRepetition.VARYING) {
                    var conditions=p.controls().stream().filter(c->c.context()==Ast.PerformControlContext.CONDITION).map(Ast.PerformControl::expression).toList();
                    var condition=conditions.size()==1?Optional.of(conditions.get(0)):Optional.<Ast.Expression>empty();
                    var predicate=condition.map(c->PerformPredicateSemantics.analyze(c,unit.id(),complete&&p.meta().provenance().exact(),p.repetition()==Ast.PerformRepetition.VARYING,refs,scalars,numbers,coverage))
                        .orElse(PerformPredicateSemantics.unavailable(p.meta().provenance()));
                    loop=Optional.of(new Loop(p.testMode(),condition,predicate));
                    if(predicate.availability()!=IfSemantics.Availability.KNOWN)gaps.add(p.repetition()==Ast.PerformRepetition.VARYING?"PERFORM_VARYING_PREDICATE_NOT_PROVEN":"PERFORM_UNTIL_PREDICATE_NOT_PROVEN");
                }
                Optional<Count> times=Optional.empty();
                if(p.repetition()==Ast.PerformRepetition.TIMES) {
                    var expressions=p.controls().stream().map(Ast.PerformControl::expression).toList();
                    var expression=expressions.size()==1?Optional.of(expressions.get(0)):Optional.<Ast.Expression>empty();
                    var integer=expression.filter(Ast.LiteralExpression.class::isInstance).map(Ast.LiteralExpression.class::cast).flatMap(Ast.LiteralExpression::integerValue);
                    var whole=expression.flatMap(e->numbers.whole(e,unit.id(),refs));
                    boolean proven=complete&&expression.filter(e->e.meta().provenance().exact()).isPresent()
                        &&(integer.filter(i->i.signum()>0).isPresent()||whole.isPresent());
                    times=Optional.of(new Count(expression,integer,whole,proven));
                    if(!proven)gaps.add("PERFORM_TIMES_COUNT_NOT_PROVEN");
                }
                Optional<Varying> varying=Optional.empty();
                if(p.repetition()==Ast.PerformRepetition.VARYING) {
                    int levels=p.controls().stream().mapToInt(Ast.PerformControl::varyingLevel).max().orElse(0);
                    var operands=new ArrayList<VaryingOperand>();
                    for(var control:p.controls())if(control.context()!=Ast.PerformControlContext.CONDITION) {
                        var e=control.expression();var integer=e instanceof Ast.LiteralExpression l?l.integerValue():Optional.<java.math.BigInteger>empty();
                        operands.add(new VaryingOperand(control,integer,numbers.whole(e,unit.id(),refs)));
                    }
                    varying=Optional.of(new Varying(levels,operands));
                    if(levels!=1||p.controls().size()!=4||p.controls().stream().anyMatch(c->c.varyingLevel()!=1))gaps.add("PERFORM_VARYING_SINGLE_VARIABLE_NOT_PROVEN");
                    if(levels>1)gaps.add("PERFORM_VARYING_AFTER_OUTSIDE_PROFILE");
                    var variables=operands.stream().filter(o->o.control().context()==Ast.PerformControlContext.CONTROL_VARIABLE).toList();
                    var froms=operands.stream().filter(o->o.control().context()==Ast.PerformControlContext.FROM).toList();
                    var steps=operands.stream().filter(o->o.control().context()==Ast.PerformControlContext.BY).toList();
                    if(variables.size()!=1||variables.get(0).wholeItem().isEmpty())gaps.add("PERFORM_VARYING_CONTROL_ITEM_NOT_PROVEN");
                    if(froms.size()!=1||froms.get(0).integer().isEmpty()&&froms.get(0).wholeItem().isEmpty())gaps.add("PERFORM_VARYING_FROM_NOT_PROVEN");
                    if(steps.size()!=1||steps.get(0).integer().filter(n->n.signum()!=0).isEmpty())gaps.add("PERFORM_VARYING_NONZERO_INCREMENT_NOT_PROVEN");
                }
                provisional.put(p.meta().id(),new Facts(start,end,range,resume,resume.map(nodes::get).map(n->n.meta().provenance()).orElse(p.meta().provenance()),loop,times,varying,List.copyOf(gaps)));
            }
            // Validate every callee before primary closure; an unproved sibling activation cannot close primary flow.
            for(var item:new ArrayList<>(provisional.entrySet())) {
                var f=item.getValue();var gaps=new LinkedHashSet<>(f.gaps());var members=new HashSet<Integer>();
                var boundary=new HashMap<Integer,Integer>();
                for(int i=0;i<f.procedures().size();i++) {
                    var r=f.procedures().get(i);members.addAll(r.statements());
                    for(var id:r.completions())boundary.put(id,i+1<f.procedures().size()?f.procedures().get(i+1).entry():-1);
                }
                if(!f.procedures().isEmpty()&&!closed(f.procedures().get(0).entry(),members,boundary,next,nodes,unit.id(),moves,ifs,evaluates,goTos,basic,provisional,false,cics))
                    gaps.add("PERFORM_RANGE_CONTROL_NOT_PROVEN");
                provisional.put(item.getKey(),new Facts(f.start(),f.end(),f.procedures(),f.resume(),f.resumeOrigin(),f.loop(),f.times(),f.varying(),List.copyOf(gaps)));
            }
            // All branches must close; ordinary incoming transfers are checked independently, even if unreachable.
            for(var p:performs) {
                var f=provisional.get(p.meta().id());var gaps=new LinkedHashSet<>(f.gaps());
                var members=new HashSet<Integer>();f.procedures().forEach(r->members.addAll(r.statements()));
                var boundary=new HashMap<Integer,Integer>();
                for(int i=0;i<f.procedures().size();i++)for(var id:f.procedures().get(i).completions())
                    boundary.put(id,i+1<f.procedures().size()?f.procedures().get(i+1).entry():-1);
                if(!f.procedures().isEmpty() && !closed(f.procedures().get(0).entry(),members,boundary,next,nodes,unit.id(),moves,ifs,evaluates,goTos,basic,provisional,false,cics))
                    gaps.add("PERFORM_RANGE_CONTROL_NOT_PROVEN");
                var entry=structure?division.procedureEntry().orElseThrow().startStatementId():Optional.<Integer>empty();
                var primary=new HashSet<Integer>();
                if(entry.isEmpty()||!closed(entry.get(),primary,Map.of(),next,nodes,unit.id(),moves,ifs,evaluates,goTos,basic,provisional,true,cics)
                        ||!primary.contains(p.meta().id())||f.resume().filter(primary::contains).isEmpty()
                        ||members.stream().anyMatch(primary::contains))gaps.add("PERFORM_ISOLATED_PRIMARY_NOT_PROVEN");
                for(var n:nodes.values())if(n instanceof Ast.Statement s && !members.contains(s.meta().id())) {
                    if(s instanceof Ast.GoToStatement g && (!goTos.closed(unit.id(),g) || goTos.entries(unit.id(),g).stream().anyMatch(members::contains)))
                        gaps.add("PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED");
                    if(Optional.ofNullable(next.get(s.meta().id())).filter(members::contains).isPresent())gaps.add("PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED");
                }
                for(var other:provisional.values()) {
                    var otherMembers=new HashSet<Integer>();other.procedures().forEach(r->otherMembers.addAll(r.statements()));
                    if(!members.equals(otherMembers)&&otherMembers.stream().anyMatch(members::contains))gaps.add("PERFORM_OVERLAPPING_RANGES");
                }
                result.put(new ScalarMoveSemantics.NodeKey(unit.id(),p.meta().id()),new Facts(f.start(),f.end(),f.procedures(),f.resume(),f.resumeOrigin(),f.loop(),f.times(),f.varying(),List.copyOf(gaps)));
            }
            if(result.entrySet().stream().anyMatch(e->e.getKey().unit().equals(unit.id())&&!e.getValue().structureKnown())) {
                for(var p:performs) {
                    var key=new ScalarMoveSemantics.NodeKey(unit.id(),p.meta().id());var f=result.get(key);
                    if(f.structureKnown())result.put(key,new Facts(f.start(),f.end(),f.procedures(),f.resume(),f.resumeOrigin(),f.loop(),f.times(),f.varying(),List.of("PERFORM_OPEN_PEER_ACTIVATION")));
                }
            }
        }
        return new ProcedurePerformSemantics(result);
    }
    private static boolean closed(int entry,Set<Integer> members,Map<Integer,Integer> boundary,Map<Integer,Integer> next,
            Map<Integer,Ast.Node> nodes,ResolutionContracts.ProgramUnitId unit,Map<ScalarMoveSemantics.NodeKey,ScalarMoveSemantics.Move> moves,
            IfSemantics ifs,EvaluateSemantics evaluates,GoToSemantics goTos,PerformSemantics basic,Map<Integer,Facts> ranges,boolean primary,CicsProgramControlAnalyzer.Contribution cics) {
        record Visit(int id,boolean finish) { }
        var active=new HashSet<Integer>();var done=new HashSet<Integer>();var todo=new ArrayDeque<Visit>();todo.push(new Visit(entry,false));
        while(!todo.isEmpty()) {
            var v=todo.pop();if(v.id()==-1)continue;
            if(v.finish()){active.remove(v.id());done.add(v.id());continue;}if(done.contains(v.id()))continue;
            if(!active.add(v.id())||!primary&&!members.contains(v.id())||!(nodes.get(v.id()) instanceof Ast.Statement s)||!cics.boundedRegion(unit,s))return false;
            if(primary)members.add(v.id());
            if(s instanceof Ast.GobackStatement){active.remove(v.id());done.add(v.id());continue;}
            todo.push(new Visit(v.id(),true));
            if(s instanceof Ast.GoToStatement g) {
                if(!goTos.closed(unit,g))return false;
                for(var target:goTos.entries(unit,g))todo.push(new Visit(target,false));
                if(GoToSemantics.depending(g))todo.push(new Visit(goTos.conditionalFact(unit,v.id()).continuation().orElseThrow(),false));
                continue;
            }
            var completion=boundary.containsKey(v.id())?boundary.get(v.id()):next.get(v.id());if(completion==null)return false;
            todo.push(new Visit(completion,false));
            if(s instanceof Ast.IfStatement f) {
                if(!f.explicitlyTerminated()||f.thenBranch().isEmpty()||f.elsePresence()==Ast.BranchPresence.UNKNOWN)return false;
                todo.push(new Visit(f.thenBranch().get(0).meta().id(),false));
                if(f.elsePresence()==Ast.BranchPresence.PRESENT){if(f.elseBranch().isEmpty())return false;todo.push(new Visit(f.elseBranch().get(0).meta().id(),false));}
            } else if(s instanceof Ast.EvaluateStatement e) {
                if(!evaluates.fact(unit,v.id()).structureKnown())return false;
                for(var arm:e.branches()){if(arm.statements().isEmpty())return false;todo.push(new Visit(arm.statements().get(0).meta().id(),false));}
            } else if(s instanceof Ast.MoveStatement) {
                var move=moves.get(new ScalarMoveSemantics.NodeKey(unit,v.id()));if(move==null||move.copy()==ScalarMoveSemantics.Copy.UNAVAILABLE)return false;
            } else if(cics.boundedLocal(unit,s)) { /* local error/return and external completion only */ }
            else if(s instanceof Ast.CallStatement c) {if(c.surface().hasHandlers())return false;}
            else if(s instanceof Ast.PerformStatement p && primary) {
                var range=ranges.get(v.id());if(range!=null ? !range.structureKnown() : !basic.fact(unit,v.id()).simpleProfile())return false;
            } else return false;
        }
        return true;
    }
    private static Optional<Endpoint> endpoint(Ast.ProcedureReference reference,ResolutionContracts.ProgramUnitId unit,
            Map<ScalarMoveSemantics.NodeKey,ReferenceResolution.Entry> refs,Map<Integer,SymbolTable.Symbol> symbols,Map<Integer,Ast.Node> nodes) {
        if(reference==null)return Optional.empty();var ref=refs.get(new ScalarMoveSemantics.NodeKey(unit,reference.meta().id()));
        if(ref==null||ref.status()!=ResolutionContracts.ResolutionStatus.RESOLVED||!reference.meta().provenance().exact())return Optional.empty();
        var id=ref.selectedCandidate().orElseThrow().entityId();var s=symbols.get(id.localId());
        if(!id.programUnitId().equals(unit)||id.domain()!=ResolutionContracts.SemanticEntityDomain.PROCEDURE_SYMBOL||s==null
                ||s.kind()!=SymbolTable.SymbolKind.PARAGRAPH||!(nodes.get(s.declarationAstNodeId()) instanceof Ast.Paragraph p))return Optional.empty();
        return Optional.of(new Endpoint(id,reference.meta().provenance(),p.meta().provenance()));
    }
    private static int index(List<Ast.Paragraph> paragraphs,Optional<Endpoint> endpoint,Map<Integer,ResolutionContracts.SemanticEntityId> identities) {
        if(endpoint.isEmpty())return -1;for(int i=0;i<paragraphs.size();i++)if(endpoint.get().identity().equals(identities.get(paragraphs.get(i).meta().id())))return i;return -1;
    }
    /** Only grammar-owned last statements can complete a paragraph; missing edges alone prove nothing. */
    private static List<Integer> completions(List<Ast.Statement> roots,ResolutionContracts.ProgramUnitId unit,CicsProgramControlAnalyzer.Contribution cics,EvaluateSemantics evaluates) {
        var result=new ArrayList<Integer>();var pending=new ArrayDeque<List<Ast.Statement>>();pending.push(roots);
        while(!pending.isEmpty()) {
            var region=pending.pop();if(region.isEmpty())continue;var last=region.get(region.size()-1);
            if(cics.boundedLocal(unit,last)||last instanceof Ast.MoveStatement || last instanceof Ast.CallStatement c && !c.surface().hasHandlers())result.add(last.meta().id());
            else if(last instanceof Ast.IfStatement f && f.explicitlyTerminated()) {
                result.add(last.meta().id());pending.push(f.thenBranch());pending.push(f.elseBranch());
            } else if(last instanceof Ast.EvaluateStatement e && evaluates.fact(unit,e.meta().id()).structureKnown()) {
                result.add(last.meta().id());for(var arm:e.branches())pending.push(arm.statements());
            }
        }
        return List.copyOf(result);
    }
    private static List<Ast.Statement> direct(Ast.Paragraph p){return p.sentences().stream().flatMap(s->s.statements().stream()).toList();}
    private static List<Ast.Statement> members(List<Ast.Statement> roots) {
        var result=new ArrayList<Ast.Statement>();var pending=new ArrayDeque<Ast.Node>();for(int i=roots.size()-1;i>=0;i--)pending.push(roots.get(i));
        while(!pending.isEmpty()){var n=pending.pop();if(n instanceof Ast.Statement s)result.add(s);var children=Ast.children(n);for(int i=children.size()-1;i>=0;i--)pending.push(children.get(i));}
        return List.copyOf(result);
    }
}
