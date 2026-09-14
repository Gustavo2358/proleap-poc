package io.github.gustavo2358.cobolexplorer;

import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Lifetime write/escape proof over canonical syntax and physical accesses; not a value solver. */
final class StorageMutationInventory {
    private record Interval(BigInteger start,BigInteger end) { }
    private final List<StorageInitialSemantics.Reason> gaps;
    private final Map<Key,List<Interval>> writes;

    private StorageMutationInventory(Set<StorageInitialSemantics.Reason> gaps,Map<Key,List<Interval>> writes) {
        this.gaps=List.copyOf(gaps);
        var index=new HashMap<Key,List<Interval>>();
        writes.forEach((base,intervals)->{
            intervals.sort(Comparator.comparing(Interval::start));
            var merged=new ArrayList<Interval>();
            for(var interval:intervals) {
                if(merged.isEmpty()||merged.get(merged.size()-1).end().compareTo(interval.start())<0)merged.add(interval);
                else {var last=merged.remove(merged.size()-1);merged.add(new Interval(last.start(),last.end().max(interval.end())));}
            }
            index.put(base,List.copyOf(merged));
        });
        this.writes=Map.copyOf(index);
    }

    List<StorageInitialSemantics.Reason> blockers(View candidate) {
        if(!gaps.isEmpty())return gaps;
        var intervals=writes.getOrDefault(candidate.base(),List.of());
        var start=candidate.offset().value().orElseThrow();var end=start.add(candidate.extent().value().orElseThrow());
        int lo=0,hi=intervals.size();
        while(lo<hi){int mid=(lo+hi)>>>1;if(intervals.get(mid).end().compareTo(start)<=0)lo=mid+1;else hi=mid;}
        return lo<intervals.size()&&intervals.get(lo).start().compareTo(end)<0
            ?List.of(StorageInitialSemantics.Reason.OVERLAPPING_WRITE):List.of();
    }

    static StorageMutationInventory analyze(CompilationUnitBuildResult frontend,CompilationUnitModel.ProgramUnit unit,
            Layout layout,Map<Key,StorageAccessSemantics.Access> accesses,
            Map<Key,List<StorageAccessSemantics.Move>> moves,CicsProgramControlAnalyzer.Contribution cics) {
        var gaps=new LinkedHashSet<StorageInitialSemantics.Reason>();var writes=new HashMap<Key,List<Interval>>();
        var bases=new HashMap<Key,Base>();layout.bases().forEach(b->bases.put(b.id(),b));
        var coverage=new HashMap<Integer,SemanticCoverage.Finding>();
        frontend.coverageByProgramUnit().get(unit.id()).findings().forEach(f->coverage.put(f.astNodeId(),f));
        if(coverage.values().stream().anyMatch(f->f.coverage()==SemanticCoverage.ConstructionCoverage.INPUT_MISSING
                ||f.astNodeId()<0&&f.coverage()!=SemanticCoverage.ConstructionCoverage.MODELED)
                ||!frontend.diagnosticsByProgramUnit().get(unit.id()).isEmpty())
            gaps.add(StorageInitialSemantics.Reason.INCOMPLETE_WRITE_INVENTORY);
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
                var finding=coverage.get(node.meta().id());
                boolean embeddedInputOnly=cics.localStorageInputOnly(unit.id(),node);
                if(finding==null||(!embeddedInputOnly&&finding.coverage()!=SemanticCoverage.ConstructionCoverage.MODELED))
                    gaps.add(StorageInitialSemantics.Reason.INCOMPLETE_WRITE_INVENTORY);
                if(statement instanceof Ast.MoveStatement move) {
                    if(move.corresponding()) {
                        var sequence=moves.getOrDefault(new Key(unit.id(),move.meta().id()),List.of());
                        if(sequence.isEmpty())gaps.add(StorageInitialSemantics.Reason.WRITE_NOT_PROVEN);
                        for(var effect:sequence)addWrite(effect.destination().orElse(null),bases,writes,gaps);
                    } else {
                        if(move.targets().isEmpty())gaps.add(StorageInitialSemantics.Reason.WRITE_NOT_PROVEN);
                        for(var target:move.targets())addWrite(accesses.get(new Key(unit.id(),target.meta().id())),bases,writes,gaps);
                    }
                } else if(statement instanceof Ast.CallStatement call) {
                    // Local, non-GLOBAL/non-EXTERNAL bytes are inaccessible to an argument-free callee.
                    // Any argument can expose storage in this first slice, including unresolved modes.
                    if(call.surface().using()||!call.arguments().isEmpty())gaps.add(StorageInitialSemantics.Reason.FOREIGN_MUTATION_OR_ESCAPE);
                    if(call.returning()!=null)addWrite(accesses.get(new Key(unit.id(),call.returning().meta().id())),bases,writes,gaps);
                    else if(call.surface().returning())gaps.add(StorageInitialSemantics.Reason.WRITE_NOT_PROVEN);
                } else if(statement instanceof Ast.EmbeddedLanguageStatement) {
                    if(!embeddedInputOnly)gaps.add(StorageInitialSemantics.Reason.FOREIGN_MUTATION_OR_ESCAPE);
                } else if(statement instanceof Ast.PerformStatement perform) {
                    if(perform.repetition()==Ast.PerformRepetition.VARYING||perform.repetition()==Ast.PerformRepetition.UNKNOWN)
                        gaps.add(StorageInitialSemantics.Reason.WRITE_NOT_PROVEN);
                } else if(!(statement instanceof Ast.IfStatement||statement instanceof Ast.EvaluateStatement
                        ||statement instanceof Ast.GoToStatement||statement instanceof Ast.NextSentenceStatement
                        ||statement instanceof Ast.GobackStatement
                        ||statement instanceof Ast.ModeledStatement m&&m.grammarRule().equals("continueStatement")&&m.operands().isEmpty()&&m.clauses().isEmpty()))
                    gaps.add(StorageInitialSemantics.Reason.UNKNOWN_STORAGE_EFFECT);
            }
            // An expression may invoke foreign code or expose an address even inside a modeled MOVE/IF.
            if(node instanceof Ast.FunctionExpression||node instanceof Ast.SpecialRegisterExpression
                    ||node instanceof Ast.PreservedExpression||node instanceof Ast.RawExpression
                    ||node instanceof Ast.OperationExpression operation&&operation.category()==Ast.OperationCategory.OTHER)
                gaps.add(StorageInitialSemantics.Reason.UNKNOWN_STORAGE_EFFECT);
            Ast.children(node).forEach(pending::push);
        }
        return new StorageMutationInventory(gaps,writes);
    }

    private static void addWrite(StorageAccessSemantics.Access access,Map<Key,Base> bases,Map<Key,List<Interval>> writes,
            Set<StorageInitialSemantics.Reason> gaps) {
        if(access==null||access.role()!=StorageAccessSemantics.Role.WRITE) {
            gaps.add(StorageInitialSemantics.Reason.WRITE_NOT_PROVEN);return;
        }
        var view=access.view();
        if(!bases.get(view.base()).independent()) {gaps.add(StorageInitialSemantics.Reason.STORAGE_NOT_LOCAL);return;}
        var start=view.offset().value().orElseThrow();
        writes.computeIfAbsent(view.base(),k->new ArrayList<>()).add(new Interval(start,start.add(view.extent().value().orElseThrow())));
    }
}
