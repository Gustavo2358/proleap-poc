package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Structural admission is global; semantic precision is selected per occurrence. */
final class PartialProgramAdmission {
    record Plan(Admission admission, List<DataFact> data, List<StatementFact> statements,
                Set<StatementId> precise, Map<StatementId,List<MoveFact>> bodies, Map<StatementId,List<StatementFact>> ranges, Map<StatementId,List<StatementFact>> compositions, RegionalStorageAdmission.Index storage, Set<StatementId> fitted) { }
    Plan plan(SpInput input, AdmitInput.Limits limits) {
        var c = new EntryGobackAdmission.Context(input, limits, true);
        try {
            if (input == null) { c.require(false,Rule.INPUT_REQUIRED,"input",null,"SP input required"); return rejected(c,Status.INVALID_INPUT); }
            EntryGobackAdmission.validate(input, c);
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            for(var relation:input.ordinaryContinuations().entrySet()) {
                var source=c.lookup(relation.getKey());var destination=relation.getValue();
                c.require(source instanceof MoveFact || source instanceof IfFact || source instanceof EvaluateFact
                    || source instanceof PerformFact || source instanceof ProcedurePerformFact,Rule.STRUCTURE,
                    relation.getKey().handle(),destination.provenance(),"ordinary continuation requires completing construction");
                if(source!=null) {
                    CallAdmission.continuation(destination,source.header(),c);
                    c.require(destination.availability()==ContinuationAvailability.KNOWN && destination.statement().isPresent()
                        && destination.provenance().exact(),Rule.STRUCTURE,relation.getKey().handle(),destination.provenance(),"positive ordinary relation required");
                    c.require(!destination.statement().equals(Optional.of(relation.getKey())), Rule.STRUCTURE,
                        relation.getKey().handle(), destination.provenance(), "ordinary continuation cannot target its source");
                    var intrinsic=next(source);
                    c.require(intrinsic==null || intrinsic.statement().isEmpty() || intrinsic.statement().equals(destination.statement()),
                        Rule.STRUCTURE,relation.getKey().handle(),destination.provenance(),"ordinary and intrinsic successors agree when both known");
                }
            }
            CallAdmission.validateFacts(input,c);
            var goToTargets=new HashMap<ProcedureId,GoToAdmission.CanonicalTarget>();
            var evaluateMembers=new HashMap<StatementId,Set<StatementId>>();
            for(var s:input.statements()) if(s.header().containment().branch()==Branch.EVALUATE_ARM)
                s.header().containment().parent().ifPresent(id -> evaluateMembers.computeIfAbsent(id,k->new HashSet<>()).add(s.header().id()));
            for (var s : input.statements()) {
                var n = next(s); if (n != null) CallAdmission.continuation(n,s.header(),c);
                if(s instanceof CicsFact x) CicsInvokeHandler.validate(x,c);
                if(s instanceof CicsFileFact x) CicsFileAdmission.validate(x,c);
                if(s instanceof OtherStatement o) {
                    var operands=new HashSet<OperandId>();
                    for(var ref:o.knownReferences())CallAdmission.reference(ref,o.header(),operands,c);
                    o.effects().ifPresent(e->{
                        var refs=new HashMap<OperandId,DataReference>();o.knownReferences().forEach(r->refs.put(r.id(),r));
                        for(var ids:List.of(e.knownReads(),e.mayWrites(),e.mustOverwrite(),e.exposedRegions())) {
                            c.require(new HashSet<>(ids).size()==ids.size(),Rule.PROFILE_FACT,o.header().id().handle(),null,"effect operand inventory is distinct");
                            for(var id:ids)c.require(refs.containsKey(id),Rule.PROFILE_FACT,o.header().id().handle(),null,"effect operand belongs to statement");
                        }
                        for(var id:e.mayWrites())c.require(refs.containsKey(id)&&refs.get(id).role()==OperandRole.WRITE,Rule.PROFILE_FACT,o.header().id().handle(),null,"write role required");
                        c.require(e.mustOverwrite().isEmpty()||(e.proof()==EffectProof.INITIALIZE_TARGETS||e.proof()==EffectProof.ACCEPT_TARGET)
                            &&e.unknownWriteBound()==EffectBound.NONE&&e.unknownExposureBound()==EffectBound.NONE,
                            Rule.PROFILE_FACT,o.header().id().handle(),null,"MUST requires bounded supported receiver proof");
                        for(var id:e.mustOverwrite())c.require(refs.containsKey(id)&&
                            (e.proof()==EffectProof.ACCEPT_TARGET?refs.get(id).wholeItemAccess().isPresent():refs.get(id).regionalAccess().isPresent()),
                            Rule.PROFILE_FACT,o.header().id().handle(),null,"MUST requires exact whole item or physical access");
                        c.require(e.mayWrites().containsAll(e.mustOverwrite()),Rule.PROFILE_FACT,o.header().id().handle(),null,"MUST is a known write");
                        if(e.proof()!=EffectProof.DISPLAY_SIMPLE&&e.proof()!=EffectProof.NO_OP)c.require(e.values()==EffectValueTransform.UNKNOWN,
                            Rule.PROFILE_FACT,o.header().id().handle(),null,"receiver value transform remains uninterpreted");
                        for(var id:e.knownReads())c.require(refs.containsKey(id)&&refs.get(id).role()==OperandRole.READ,Rule.PROFILE_FACT,o.header().id().handle(),null,"read role required");
                        if(e.proof()==EffectProof.NO_OP)c.require(e.knownReads().isEmpty()&&e.mayWrites().isEmpty()&&e.mustOverwrite().isEmpty()&&e.exposedRegions().isEmpty()&&e.unknownReadBound()==EffectBound.NONE&&e.unknownWriteBound()==EffectBound.NONE&&e.unknownExposureBound()==EffectBound.NONE&&e.environment()==EnvironmentEffect.NONE&&e.values()==EffectValueTransform.NONE,Rule.PROFILE_FACT,o.header().id().handle(),null,"NO_OP proof has no effects");
                        if(e.proof()==EffectProof.DISPLAY_SIMPLE)c.require(e.mayWrites().isEmpty()&&e.mustOverwrite().isEmpty()&&e.exposedRegions().isEmpty()
                            &&e.unknownWriteBound()==EffectBound.NONE&&e.unknownExposureBound()==EffectBound.NONE
                            &&e.environment()==EnvironmentEffect.OUTPUT&&e.values()==EffectValueTransform.NONE,Rule.PROFILE_FACT,o.header().id().handle(),null,"DISPLAY proof is read-only storage with output environment");
                    });
                }
                if (s instanceof ProcedurePerformFact p) ProcedurePerformAdmission.validate(p,c);
                if (s instanceof GoToFact g) GoToAdmission.validate(g,c,goToTargets);
                if (s instanceof ConditionalGoToFact g) GoToAdmission.validate(g,c,goToTargets);
                if (s instanceof EvaluateFact e) EvaluateAdmission.validate(e,evaluateMembers.getOrDefault(e.header().id(),Set.of()),c);
                if (s instanceof IfFact f) {
                    var operands = new HashSet<OperandId>();
                    for (var ref : f.conditionReads()) CallAdmission.reference(ref,f.header(),operands,c);
                    c.provenance(f.conditionProvenance()); c.provenance(f.predicateGuarantee().provenance());
                    c.provenance(f.thenArm().provenance()); c.provenance(f.elseArm().provenance());
                    validateArm(f,f.thenArm(),Branch.THEN,c); validateArm(f,f.elseArm(),Branch.ELSE,c);
                }
                if (s instanceof PerformFact p) {
                    p.target().ifPresent(t -> { c.identity(t.id().unit(),t.id().handle(),"procedure",t.paragraphOrigin()); c.provenance(t.referenceOrigin()); c.provenance(t.paragraphOrigin()); });
                    for (var list : List.of(p.primaryStatements(),p.targetStatements())) {
                        var unique = new HashSet<StatementId>();
                        for (var id : list) { c.touch(); c.require(id.unit().equals(input.unit()) && c.lookup(id)!=null && unique.add(id),Rule.STRUCTURE,id.handle(),null,"PERFORM members are distinct published statements"); }
                    }
                }
            }
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            input.storageIndependence().filter(p -> p.availability()==Availability.KNOWN).ifPresent(p -> {
                var members=new HashSet<DataId>();
                c.require(p.members().size()>=2 && p.authority().equals("IBM_ENTERPRISE_COBOL_6_4_WORKING_STORAGE")
                    && p.gapCodes().isEmpty() && p.provenance().filter(Provenance::exact).isPresent(),Rule.PROFILE_FACT,"storage-independence",null,"complete source-derived storage proof required");
                for(var id:p.members())c.require(id.unit().equals(input.unit()) && c.data(id)!=null && members.add(id),Rule.STRUCTURE,id.handle(),null,"storage proof has distinct published members");
            });
            if (!c.diagnostics.isEmpty())return rejected(c,Status.INVALID_INPUT);
            c.phase=Phase.ADMISSION;
            for(var id:RegionalDataTranslator.sourceText(c.regionalStorage)) {
                var view=c.regionalStorage.byData().get(id);if(view==null)continue;
                var base=c.regionalStorage.bases().get(view.base());
                if(base!=null&&!base.allocation().proved()&&base.extent().value().isEmpty()
                        &&!c.regionalStorage.logical().byData.containsKey(id)&&!c.regionalStorage.localCellSafe(id)
                        &&input.dataDeclarations().stream().filter(d->d.id().equals(id)).noneMatch(d->CallAdmission.scalar(d)))
                    c.require(false,Rule.PROFILE_FACT,id.handle(),null,"supported logical value has an unrepresentable partial storage relation");
            }
            if(!c.diagnostics.isEmpty())return rejected(c,Status.BLOCKED_LOWERING);
            c.require(input.entryInventory().entries().size()==1 && input.entryInventory().entries().getFirst().start().statement().isPresent(),
                Rule.ENTRY_START,"entry",null,"usable explicit primary entry required");
            if (!c.diagnostics.isEmpty()) return rejected(c,Status.BLOCKED_LOWERING);
            var data=ScalarDataOrder.canonical(input.dataDeclarations().stream().filter(d->CallAdmission.scalar(d)||PerformCountAdmission.integer(d)||RegionalDataTranslator.textual(c.regionalStorage,d.id())||c.regionalStorage.logical().byData.containsKey(d.id())).toList());
            var mapped=new HashSet<DataId>(); data.forEach(d->mapped.add(d.id()));
            var precise=new HashSet<StatementId>(); var fitted=new HashSet<StatementId>();
            for(var s:input.statements()) {
                int before=c.diagnostics.size();
                boolean eligible=false;
                if(s instanceof MoveFact m && mapped.contains(m.target().wholeItemAccess().map(WholeItemAccess::data).orElse(null))
                    && (!(m.source() instanceof DataReference r)||mapped.contains(r.wholeItemAccess().map(WholeItemAccess::data).orElse(null)))) {
                    CallAdmission.admitMove(m,c); eligible=before==c.diagnostics.size()&&RegionalDataTranslator.encodableLiteral(c.regionalStorage,m);
                    if(eligible&&m.copySemantics()==CopySemantics.FITTED_TEXT)fitted.add(m.header().id());
                    if(m.source() instanceof DataReference && (RegionalDataTranslator.textual(c.regionalStorage,m.target().wholeItemAccess().orElseThrow().data())
                        ||RegionalDataTranslator.textual(c.regionalStorage,((DataReference)m.source()).wholeItemAccess().orElseThrow().data())))eligible=false;
                } else if(s instanceof CallFact || s instanceof CicsFact || s instanceof CicsFileFact) {
                    eligible=true; // The dependency site survives unavailable target values and CALL surface gaps.
                } else if(s instanceof IfFact f && f.header().provenance().exact()) {
                    // Predicate evaluation and arm content coverage do not gate known control entries.
                    eligible=true;
                } else if(s instanceof EvaluateFact e) eligible=EvaluateAdmission.structured(e);
                else if(s instanceof GoToFact g) eligible=GoToAdmission.precise(g);
                else if(s instanceof ConditionalGoToFact g) eligible=GoToAdmission.precise(g);
                else if(s instanceof GobackFact) eligible=true;
                if(s instanceof MoveFact m&&(!m.logicalTransfers().isEmpty()||m.regionalMove().isPresent()
                    &&m.transfers().stream().anyMatch(t->t.effect().kind()!=io.github.gustavo2358.lower.domain.StorageFacts.MoveKind.UNAVAILABLE))) {
                    c.diagnostics.subList(before,c.diagnostics.size()).clear();eligible=true;
                }
                if(s instanceof MoveFact m&&(c.regionalStorage.logical().literalMove(m)||m.copySemantics()==CopySemantics.POSSIBLE_TEXT))eligible=true;
                if(eligible&&before==c.diagnostics.size())precise.add(s.header().id());
                c.diagnostics.subList(before,c.diagnostics.size()).clear();
            }
            var bodies=new LinkedHashMap<StatementId,List<MoveFact>>(); var bodyMembers=new HashSet<StatementId>();
            var targets=new HashMap<ProcedureId,List<StatementId>>();
            var owners=new HashMap<StatementId,ProcedureId>();
            var primary=Set.copyOf(primary(input,c,precise));
            for(var s:input.statements()) if(s instanceof PerformFact p && p.profile()==PerformProfile.BASIC_PROCEDURE_PERFORM) {
                boolean valid=p.target().isPresent()&&!p.targetStatements().isEmpty()&&p.normalContinuation().statement().isPresent();
                valid &= p.primaryStatements().isEmpty() && primary.contains(p.header().id())
                    && p.normalContinuation().statement().filter(primary::contains).isPresent()
                    && p.target().filter(t -> t.referenceOrigin().exact() && t.paragraphOrigin().exact()).isPresent();
                var target=p.target().map(PerformTarget::id).orElse(null);
                if(target!=null) {
                    var previous=targets.putIfAbsent(target,p.targetStatements());
                    valid &= previous==null || previous.equals(p.targetStatements());
                }
                var body=new ArrayList<MoveFact>(); boolean preciseBody=true;
                for(int i=0;i<p.targetStatements().size();i++) {
                    var member=c.lookup(p.targetStatements().get(i));
                    if(!(member instanceof MoveFact m)) {valid=false;continue;}
                    preciseBody &= precise.contains(m.header().id());
                    var expected=i+1<p.targetStatements().size()?Optional.of(p.targetStatements().get(i+1)):Optional.<StatementId>empty();
                    var owner=owners.putIfAbsent(m.header().id(),target);
                    valid &= (owner==null || owner.equals(target)) && !primary.contains(m.header().id())
                        && m.header().containment().equals(new Containment(Optional.empty(),Branch.ROOT))
                        && m.normalContinuation().statement().equals(expected); body.add(m);
                }
                valid &= p.targetEntry().equals(p.targetStatements().stream().findFirst())
                    &&p.targetExit().equals(p.targetStatements().isEmpty()?Optional.empty():Optional.of(p.targetStatements().getLast()));
                if(!valid) {c.require(false,Rule.STRUCTURE,p.header().id().handle(),p.header().provenance(),"BASIC activation contradicts intrinsic body facts");continue;}
                if(!preciseBody)continue; // A semantic body gap is partial, not a contradictory structural proof.
                precise.add(p.header().id());bodies.put(p.header().id(),List.copyOf(body));bodyMembers.addAll(p.targetStatements());
            }
            var ranges=new HashMap<StatementId,List<StatementFact>>();
            for(var s:input.statements())if(s instanceof ProcedurePerformFact p) {
                var body=ProcedurePerformAdmission.qualify(p,c,precise,primary,input.statements());
                if(!body.isEmpty()){ranges.put(p.header().id(),body);precise.add(p.header().id());bodyMembers.addAll(ProcedurePerformAdmission.members(p));}
            }
            for (var s : input.statements()) {
                if(s instanceof GoToFact g && !bodyMembers.contains(s.header().id()))c.require(g.targetEntry().filter(bodyMembers::contains).isEmpty(),Rule.STRUCTURE,
                    s.header().id().handle(),s.header().provenance(),"intrinsic BASIC body has no ordinary GO TO incoming edge");
                if(s instanceof ConditionalGoToFact g && !bodyMembers.contains(s.header().id()))c.require(g.destinations().stream().noneMatch(d->d.targetEntry().filter(bodyMembers::contains).isPresent()),Rule.STRUCTURE,
                    s.header().id().handle(),s.header().provenance(),"intrinsic body has no ordinary conditional incoming edge");
                var successor=ordinaryNext(s);
                if (!bodyMembers.contains(s.header().id()) && successor!=null)
                    c.require(successor.statement().filter(bodyMembers::contains).isEmpty(),Rule.STRUCTURE,s.header().id().handle(),s.header().provenance(),"intrinsic BASIC body has no ordinary incoming continuation");
            }
            if(!c.diagnostics.isEmpty())return rejected(c,Status.INVALID_INPUT);
            // Legacy specialization can coexist with an explicitly published ordinary incoming path.
            // Keep the required ordinary occurrences so positive relations never point at removed labels.
            var ordinaryInventory=ordinaryInventory(input,bodyMembers,c);
            var statements=input.statements().stream().filter(s->ordinaryInventory.contains(s.header().id()))
                .sorted(Comparator.comparingInt(s->s.header().programPoint())).toList();
            return new Plan(c.result(Status.ADMITTED),data,statements,Set.copyOf(precise),Map.copyOf(bodies),Map.copyOf(ranges),CompositionalPerformAdmission.plan(input,c,ranges),c.regionalStorage,Set.copyOf(fitted));
        } catch(EntryGobackAdmission.LimitReached ex) {return rejected(c,Status.IMPLEMENTATION_LIMIT);}
    }
    private static Set<StatementId> ordinaryInventory(SpInput input,Set<StatementId> specialized,EntryGobackAdmission.Context c) {
        var retained=new HashSet<StatementId>();var pending=new ArrayDeque<StatementId>();
        input.statements().stream().map(s->s.header().id()).filter(id->!specialized.contains(id)).forEach(pending::addLast);
        while(!pending.isEmpty()) {
            var id=pending.removeFirst();if(!retained.add(id))continue;
            c.touch();var fact=c.lookup(id);var successor=ordinaryNext(input,fact);
            if(successor!=null)successor.statement().ifPresent(pending::addLast);
            if(fact instanceof GoToFact g)g.targetEntry().ifPresent(pending::addLast);
            if(fact instanceof ConditionalGoToFact g)g.destinations().forEach(d->d.targetEntry().ifPresent(pending::addLast));
            if(fact instanceof IfFact f) {f.thenArm().entry().statement().ifPresent(pending::addLast);f.elseArm().entry().statement().ifPresent(pending::addLast);}
            if(fact instanceof EvaluateFact e) {e.arms().forEach(a->a.control().entry().statement().ifPresent(pending::addLast));e.otherArm().entry().statement().ifPresent(pending::addLast);}
        }
        return retained;
    }
    private static List<StatementId> primary(SpInput input,EntryGobackAdmission.Context c,Set<StatementId> precise) {
        record Visit(StatementId id,boolean complete) { }
        var closed=new LinkedHashSet<StatementId>(); var active=new HashSet<StatementId>(); var pending=new ArrayDeque<Visit>();
        input.entryInventory().entries().getFirst().start().statement().ifPresent(id->pending.push(new Visit(id,false)));
        while(!pending.isEmpty()) {
            var visit=pending.pop(); var id=visit.id();
            if(visit.complete()) { active.remove(id); closed.add(id); continue; }
            if(closed.contains(id))continue; // A shared join was already proved closed.
            if(!active.add(id))return List.of(); // A cycle does not prove a returning primary region.
            c.touch(); var s=c.lookup(id); if(s==null)return List.of();
            if(s instanceof GobackFact) { active.remove(id); closed.add(id); continue; }
            if(s instanceof ConditionalGoToFact g) {
                if(!precise.contains(id))return List.of();
                pending.push(new Visit(id,true));
                for(var d:g.destinations())pending.push(new Visit(d.targetEntry().orElseThrow(),false));
                pending.push(new Visit(g.normalContinuation().statement().orElseThrow(),false));continue;
            }
            if(s instanceof GoToFact g) {
                if(!precise.contains(id))return List.of();
                pending.push(new Visit(id,true)); pending.push(new Visit(g.targetEntry().orElseThrow(),false));
                continue;
            }
            if(!precise.contains(id) && !(s instanceof PerformFact p && p.profile()==PerformProfile.BASIC_PROCEDURE_PERFORM)
                    && !(s instanceof ProcedurePerformFact p && p.start().isPresent()&&p.end().isPresent()
                        &&p.normalContinuation().statement().isPresent()))return List.of();
            var continuation=ordinaryNext(s);
            if(continuation==null || continuation.availability()!=ContinuationAvailability.KNOWN
                    || continuation.statement().isEmpty() || !continuation.provenance().exact())return List.of();
            pending.push(new Visit(id,true));
            pending.push(new Visit(continuation.statement().orElseThrow(),false));
            if(s instanceof EvaluateFact e) {
                if(!precise.contains(id) || e.arms().stream().anyMatch(a->a.control().entry().statement().isEmpty()))return List.of();
                for(var arm:e.arms())pending.push(new Visit(arm.control().entry().statement().orElseThrow(),false));
                e.otherArm().entry().statement().ifPresent(entry->pending.push(new Visit(entry,false)));
            }
            if(s instanceof IfFact f) {
                if(!precise.contains(id) || !primaryArm(f.thenArm(),false) || !primaryArm(f.elseArm(),true))return List.of();
                pending.push(new Visit(f.thenArm().entry().statement().orElseThrow(),false));
                f.elseArm().entry().statement().ifPresent(entry->pending.push(new Visit(entry,false)));
            }
        }
        return List.copyOf(closed);
    }
    private static boolean primaryArm(IfArm arm,boolean mayBeAbsent) {
        return arm.contentAvailability()==Availability.KNOWN && arm.provenance().exact()
            && (arm.presence()==ClausePresence.PRESENT && arm.entry().availability()==Availability.KNOWN && arm.entry().statement().isPresent()
                || mayBeAbsent && arm.presence()==ClausePresence.ABSENT && arm.entry().statement().isEmpty());
    }

    private static void validateArm(IfFact f,IfArm arm,Branch branch,EntryGobackAdmission.Context c) {
        if(arm.entry().statement().isPresent()) {
            var id=arm.entry().statement().orElseThrow();var child=c.lookup(id);
            c.require(child!=null && child.header().containment().equals(new Containment(Optional.of(f.header().id()),branch)),
                Rule.STRUCTURE,id.handle(),arm.provenance(),"IF arm entry belongs to that direct arm");
        }
        c.require(arm.presence()!=ClausePresence.ABSENT || arm.entry().statement().isEmpty(),Rule.STRUCTURE,f.header().id().handle(),arm.provenance(),"absent IF arm has no entry");
    }
    static NormalContinuation ordinaryNext(SpInput input,StatementFact s) {
        return input.ordinaryContinuations().getOrDefault(s.header().id(),ordinaryNext(s));
    }
    static NormalContinuation ordinaryNext(StatementFact s) {
        return s instanceof CicsFact c?c.ordinaryContinuation():s instanceof CicsFileFact c?c.ordinaryContinuation():next(s);
    }
    static NormalContinuation next(StatementFact s) {
        if(s instanceof CicsFact c)return c.localContinuation();
        if(s instanceof CicsFileFact c)return c.localContinuation();
        if(s instanceof ConditionalGoToFact g)return g.normalContinuation();
        if(s instanceof ProcedurePerformFact p)return p.normalContinuation();
        if(s instanceof EvaluateFact e)return e.normalContinuation();
        if(s instanceof OtherStatement o)return o.normalContinuation();
        return SupportedProgramAdmission.next(s);
    }
    private static Plan rejected(EntryGobackAdmission.Context c,Status s) {return new Plan(c.result(s),List.of(),List.of(),Set.of(),Map.of(),Map.of(),Map.of(),c.regionalStorage,Set.of());}
}
