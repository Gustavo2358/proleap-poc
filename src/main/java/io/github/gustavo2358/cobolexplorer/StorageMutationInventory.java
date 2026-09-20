package io.github.gustavo2358.cobolexplorer;

import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Lifetime write/escape proof over canonical syntax and physical accesses; not a value solver. */
final class StorageMutationInventory {
    private record Interval(BigInteger start,BigInteger end) { }
    private final List<StorageInitialSemantics.Reason> globalBlockers;
    private final Map<Key,List<Interval>> writes;
    private final Set<Key> wholeBaseWrites;
    private final Map<Key,List<Interval>> exposedRegions;
    private final Set<Key> unknownExposures;
    private final Set<Key> independentBases;

    private StorageMutationInventory(Set<StorageInitialSemantics.Reason> gaps,Map<Key,List<Interval>> writes,
            Map<Key,List<Interval>> exposedRegions,Set<Key> unknownExposures,Map<Key,Base> bases,Set<Key> wholeBaseWrites) {
        this.globalBlockers=List.copyOf(gaps);
        this.writes=index(writes);
        this.wholeBaseWrites=Set.copyOf(wholeBaseWrites);
        this.exposedRegions=index(exposedRegions);
        this.unknownExposures=Set.copyOf(unknownExposures);
        var independent=new HashSet<Key>();
        bases.forEach((key,base)->{if(base.independent())independent.add(key);});
        this.independentBases=Set.copyOf(independent);
    }

    private static Map<Key,List<Interval>> index(Map<Key,List<Interval>> regions) {
        var index=new HashMap<Key,List<Interval>>();
        regions.forEach((base,intervals)->{
            intervals.sort(Comparator.comparing(Interval::start));
            var merged=new ArrayList<Interval>();
            for(var interval:intervals) {
                if(merged.isEmpty()||merged.get(merged.size()-1).end().compareTo(interval.start())<0)merged.add(interval);
                else {var last=merged.remove(merged.size()-1);merged.add(new Interval(last.start(),last.end().max(interval.end())));}
            }
            index.put(base,List.copyOf(merged));
        });
        return Map.copyOf(index);
    }

    List<StorageInitialSemantics.Reason> blockers(View candidate) {
        var blockers=new LinkedHashSet<>(globalBlockers);
        if(wholeBaseWrites.contains(candidate.base())||overlaps(writes,candidate))blockers.add(StorageInitialSemantics.Reason.OVERLAPPING_WRITE);
        // All indexed exposures have independent allocation. A different base is
        // disjoint only if the candidate also carries that allocation proof.
        if(!unknownExposures.isEmpty()||overlaps(exposedRegions,candidate)
                ||!independentBases.contains(candidate.base())&&!exposedRegions.isEmpty())
            blockers.add(StorageInitialSemantics.Reason.FOREIGN_MUTATION_OR_ESCAPE);
        return List.copyOf(blockers);
    }

    private static boolean overlaps(Map<Key,List<Interval>> regions,View candidate) {
        var intervals=regions.getOrDefault(candidate.base(),List.of());
        var start=candidate.offset().value().orElseThrow();var end=start.add(candidate.extent().value().orElseThrow());
        int lo=0,hi=intervals.size();
        while(lo<hi){int mid=(lo+hi)>>>1;if(intervals.get(mid).end().compareTo(start)<=0)lo=mid+1;else hi=mid;}
        return lo<intervals.size()&&intervals.get(lo).start().compareTo(end)<0;
    }

    static StorageMutationInventory analyze(CompilationUnitBuildResult frontend,CompilationUnitModel.ProgramUnit unit,
            Layout layout,Map<Key,StorageAccessSemantics.Access> accesses,
            Map<Key,List<StorageAccessSemantics.Move>> moves,Map<Key,StatementEffectSummary> effects,CicsProgramControlAnalyzer.Contribution cics,FileIoMemory files) {
        var gaps=new LinkedHashSet<StorageInitialSemantics.Reason>();var writes=new HashMap<Key,List<Interval>>();
        var wholeBaseWrites=new HashSet<Key>();
        var exposedRegions=new HashMap<Key,List<Interval>>();var unknownExposures=new LinkedHashSet<Key>();
        var bases=new HashMap<Key,Base>();layout.bases().forEach(b->bases.put(b.id(),b));
        // Diagnostic count and source feature coverage do not change the
        // executable mutation inventory. Only published effects enter it.
        if(layout.reasons().contains(Reason.INPUT_MISSING))gaps.add(StorageInitialSemantics.Reason.INCOMPLETE_WRITE_INVENTORY);
        if(unit.parentId()!=null||frontend.compilationUnit().programUnits().stream().anyMatch(u->unit.id().equals(u.parentId())))
            gaps.add(StorageInitialSemantics.Reason.FOREIGN_MUTATION_OR_ESCAPE);
        var divisions=unit.program().divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).toList();
        if(divisions.size()!=1||divisions.get(0).procedureEntry().isEmpty())gaps.add(StorageInitialSemantics.Reason.INCOMPLETE_WRITE_INVENTORY);
        else {
            var entry=divisions.get(0).procedureEntry().orElseThrow();
            if(entry.signatureClausesPresent()||entry.declarativesPresent())gaps.add(StorageInitialSemantics.Reason.FOREIGN_MUTATION_OR_ESCAPE);
        }
        var pending=new ArrayDeque<Ast.Node>();divisions.forEach(pending::push);
        while(!pending.isEmpty()) {
            var node=pending.pop();
            if(node instanceof Ast.Program) {gaps.add(StorageInitialSemantics.Reason.FOREIGN_MUTATION_OR_ESCAPE);continue;}
            if(node instanceof Ast.Statement statement) {
                var summary=Optional.ofNullable(effects.get(new Key(unit.id(),node.meta().id())));
                var file=files.statement(new Key(unit.id(),node.meta().id()));
                boolean bounded=file.map(FileIoMemory.Statement::bounded).orElseGet(()->summary.filter(StatementEffectSummary::completeMutationBound).isPresent());
                boolean embeddedInputOnly=cics.localStorageInputOnly(unit.id(),node);
                if(file.isPresent()) {
                    for(var operation:file.orElseThrow().operations())for(var write:operation.writes()) {
                        var view=write.target().view().orElse(null);var base=view==null?null:bases.get(view.base());
                        if(base==null||!base.independent()){gaps.add(StorageInitialSemantics.Reason.STORAGE_NOT_LOCAL);continue;}
                        if(write.target().wholeBase()||view.offset().value().isEmpty()||view.extent().value().isEmpty())wholeBaseWrites.add(view.base());
                        else {var start=view.offset().value().orElseThrow();writes.computeIfAbsent(view.base(),ignored->new ArrayList<>()).add(new Interval(start,start.add(view.extent().value().orElseThrow())));}
                    }
                } else if(bounded) {
                    var effect=summary.orElseThrow();
                    for(var target:effect.mayWrites())addWrite(accesses.get(new Key(unit.id(),target.meta().id())),bases,writes,gaps);
                    if(!effect.exposedRegions().isEmpty())unknownExposures.add(new Key(unit.id(),node.meta().id()));
                } else if(statement instanceof Ast.MoveStatement move) {
                    if(move.corresponding()) {
                        var sequence=moves.getOrDefault(new Key(unit.id(),move.meta().id()),List.of());
                        for(var effect:sequence)addWrite(effect.destination().orElse(null),bases,writes,gaps);
                    } else {
                        for(var target:move.targets()) {
                            var access=accesses.get(new Key(unit.id(),target.meta().id()));
                            if(access==null&&target instanceof Ast.DataReference r&&r.referenceModification()!=null)
                                gaps.add(StorageInitialSemantics.Reason.WRITE_NOT_PROVEN);
                            else addWrite(access,bases,writes,gaps);
                        }
                    }
                } else if(statement instanceof Ast.CallStatement call) {
                    for(var argument:call.arguments())
                        addExposure(unit,argument,accesses,bases,exposedRegions,unknownExposures);
                    if(call.returning()!=null)addWrite(accesses.get(new Key(unit.id(),call.returning().meta().id())),bases,writes,gaps);
                } else if(statement instanceof Ast.EmbeddedLanguageStatement embedded) {
                    cics.fileFact(unit.id(),embedded.meta().id()).ifPresent(fact->{
                        for(var host:embedded.hostOperands()) {
                            boolean writesHost=fact.options().stream().anyMatch(option->
                                option.syntax().start()==host.optionStart()
                                    &&(option.role()==CicsFileControlAnalyzer.Role.WRITE
                                        ||option.role()==CicsFileControlAnalyzer.Role.READ_WRITE));
                            if(writesHost&&(host.role()==Ast.EmbeddedHostRole.WRITE||host.role()==Ast.EmbeddedHostRole.READ_WRITE))
                                addWrite(accesses.get(new Key(unit.id(),host.reference().meta().id())),bases,writes,gaps);
                        }
                    });
                }
            }
            Ast.children(node).forEach(pending::push);
        }
        return new StorageMutationInventory(gaps,writes,exposedRegions,unknownExposures,bases,wholeBaseWrites);
    }

    private static void addExposure(CompilationUnitModel.ProgramUnit unit,Ast.CallArgument argument,
            Map<Key,StorageAccessSemantics.Access> accesses,Map<Key,Base> bases,
            Map<Key,List<Interval>> exposedRegions,Set<Key> unknownExposures) {
        // VALUE here is the argument form, not BY VALUE. Only ordinary data items
        // passed BY REFERENCE (including the typed default) are admitted.
        if(argument.passingMode()==Ast.PassingMode.REFERENCE&&argument.argumentKind()==Ast.CallArgumentKind.VALUE
                &&argument.value() instanceof Ast.DataReference reference) {
            var access=accesses.get(new Key(unit.id(),reference.meta().id()));
            if(access!=null&&access.role()==StorageAccessSemantics.Role.CALL_ARGUMENT) {
                var view=access.view();var base=bases.get(view.base());
                if(base!=null&&base.independent()) {
                    var start=view.offset().value().orElseThrow();
                    exposedRegions.computeIfAbsent(view.base(),k->new ArrayList<>())
                        .add(new Interval(start,start.add(view.extent().value().orElseThrow())));
                    return;
                }
            }
        }
        // Unresolved binding/layout, dynamic slices, addresses, other modes and
        // expressions cannot prove disjunction from any candidate's bytes.
        unknownExposures.add(new Key(unit.id(),argument.meta().id()));
    }

    private static void addWrite(StorageAccessSemantics.Access access,Map<Key,Base> bases,Map<Key,List<Interval>> writes,
            Set<StorageInitialSemantics.Reason> gaps) {
        if(access==null||access.role()!=StorageAccessSemantics.Role.WRITE)return;
        var view=access.view();
        if(!bases.get(view.base()).independent()) {gaps.add(StorageInitialSemantics.Reason.STORAGE_NOT_LOCAL);return;}
        var start=view.offset().value().orElseThrow();
        writes.computeIfAbsent(view.base(),k->new ArrayList<>()).add(new Interval(start,start.add(view.extent().value().orElseThrow())));
    }
}
