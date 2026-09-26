package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.FactDependencies;
import static io.github.gustavo2358.cobolexplorer.StorageInitialSemantics.*;

/** Whole-lifetime mutation proof over canonical syntax and independently closed local cells. */
final class LogicalInitialSemantics {
    record Result(Facts facts,List<String> blockers) { Result { blockers=List.copyOf(blockers); } }
    static Map<ResolutionContracts.ProgramUnitId,Result> analyze(CompilationUnitBuildResult frontend,
            ReferenceResolution resolution,ResolutionAnalysisReport report,StorageAccessSemantics storage,
            Map<ResolutionContracts.ProgramUnitId,FactDependencies> graphs,CicsProgramControlAnalyzer.Contribution cics) {
        var result=new LinkedHashMap<ResolutionContracts.ProgramUnitId,Result>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var original=storage.initial().facts(unit.id());var graph=graphs.get(unit.id());
            if(graph==null||original.mode()!=EntryMode.UNKNOWN){result.put(unit.id(),new Result(original,List.of()));continue;}
            var inventory=new Inventory(frontend,unit,resolution,report,storage,cics);
            var availability=graph.proofAvailability();var cells=new HashSet<String>();
            for(var fact:graph.facts())if(fact.kind()==FactDependencies.FactKind.LOCAL_CELL
                &&fact.dependencies().stream().allMatch(p->Boolean.TRUE.equals(availability.get(p))))cells.add(fact.subject());
            var conditions=new ArrayList<Condition>();
            for(var condition:original.conditions()) {
                int node=condition.declaration().node();
                if(condition.kind()==Kind.POSSIBLE_LOGICAL_TEXT&&cells.contains("storage-node:"+node)&&inventory.unchanged(node))
                    conditions.add(new Condition(condition.declaration(),Optional.empty(),Kind.LOGICAL_TEXT,List.of(),List.of(),condition.origin(),Proof.DECLARATIVE_INVARIANT,condition.logicalText()));
                else conditions.add(condition);
            }
            result.put(unit.id(),new Result(new Facts(original.mode(),conditions),List.copyOf(inventory.blockers)));
        }
        return Map.copyOf(result);
    }
    private static final class Inventory {
        final Set<String> blockers=new LinkedHashSet<>();final Set<Integer> mutations=new HashSet<>();
        final Map<Integer,Integer> parent=new HashMap<>();final Map<ResolutionContracts.SemanticEntityId,Integer> nodes=new HashMap<>();
        final Map<Integer,ReferenceResolution.Entry> references=new HashMap<>();
        Inventory(CompilationUnitBuildResult frontend,CompilationUnitModel.ProgramUnit unit,ReferenceResolution resolution,
                ResolutionAnalysisReport report,StorageAccessSemantics storage,CicsProgramControlAnalyzer.Contribution cics) {
            var layout=storage.layout().layout(unit.id());
            for(var n:layout.nodes()){n.parent().ifPresent(p->parent.put(n.id().node(),p.node()));n.entity().ifPresent(e->nodes.put(e,n.id().node()));}
            for(var r:resolution.entries())if(r.occurrence().programUnitId().equals(unit.id()))references.put(r.occurrence().referenceAstNodeId(),r);
            if(unit.parentId()!=null||frontend.compilationUnit().programUnits().stream().anyMatch(u->unit.id().equals(u.parentId())))blockers.add("NESTED_VISIBILITY");
            var procedures=unit.program().divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).toList();
            if(procedures.size()!=1||procedures.get(0).procedureEntry().isEmpty()){blockers.add("PROCEDURE_INVENTORY_UNAVAILABLE");return;}
            var procedure=procedures.get(0);var entry=procedure.procedureEntry().orElseThrow();
            if(entry.signatureClausesPresent()||entry.declarativesPresent())blockers.add("FOREIGN_ENTRY_STORAGE");
            if(unit.program().inputProof().regions().stream().anyMatch(p->p.expanded().endLine()>=procedure.meta().provenance().expanded().startLine()))blockers.add("PROCEDURE_INPUT_MISSING");
            if(report.gaps().stream().anyMatch(g->g.category()==ResolutionAnalysisReport.GapCategory.INPUT&&ResolutionAnalysisReport.appliesTo(g,unit.id())
                &&!(g.code().equals("UNRESOLVED_COPY")&&!unit.program().inputProof().regions().isEmpty())))blockers.add("UNLOCATED_INPUT_MISSING");
            var pending=new ArrayDeque<Ast.Node>();pending.push(procedure);
            while(!pending.isEmpty()) {
                var n=pending.pop();
                // Taking any address escapes the inventory's ordinary declaration model.
                if(n instanceof Ast.SpecialRegisterExpression r&&r.registerName().equals("ADDRESS"))blockers.add("ADDRESS_ESCAPE");
                // Function identity/passing policy is outside this proof: arguments may escape.
                if(n instanceof Ast.FunctionExpression f)for(var argument:f.arguments())
                    if(!(argument instanceof Ast.LiteralExpression))write(argument);
                if(n instanceof Ast.RawExpression||n instanceof Ast.PreservedExpression)blockers.add("EXPRESSION_EFFECTS_UNAVAILABLE:"+n.meta().id());
                if(n instanceof Ast.Program){blockers.add("NESTED_VISIBILITY");continue;}
                if(n instanceof Ast.Statement s)statement(s,unit,storage,cics);
                for(var child:Ast.children(n))pending.push(child);
            }
        }
        boolean unchanged(int node) {
            if(!blockers.isEmpty())return false;
            for(Integer n=node;n!=null;n=parent.get(n))if(mutations.contains(n))return false;
            return true;
        }
        void write(Ast.Expression expression) {
            if(!(expression instanceof Ast.DataReference ref)){blockers.add("WRITE_OPERAND_UNAVAILABLE");return;}
            var binding=references.get(ref.meta().id());
            if(binding==null||binding.status()!=ResolutionContracts.ResolutionStatus.RESOLVED||binding.candidates().size()!=1||binding.selectedCandidate().isEmpty()) {
                blockers.add("WRITE_BINDING_UNAVAILABLE");return;
            }
            var node=nodes.get(binding.selectedCandidate().orElseThrow().entityId());
            if(node==null){blockers.add("WRITE_STORAGE_UNAVAILABLE");return;}
            // A slice or occurrence may modify any part of this declaration. No byte approximation.
            mutations.add(node);
        }
        /** RETURN transfers input task data; this proof grants no continuation. */
        boolean returnFootprint(Ast.EmbeddedLanguageStatement statement) {
            var parsed=CicsCommandSyntax.parse(statement.rawText());
            if(parsed.isEmpty()||!parsed.get().name().equals("RETURN")||!parsed.get().ended()||!parsed.get().gaps().isEmpty())return false;
            var options=parsed.get().options();var names=new HashSet<String>();
            var hosts=new HashMap<Integer,Ast.EmbeddedHostOperand>();statement.hostOperands().forEach(h->hosts.put(h.optionStart(),h));
            for(var o:options) {
                if(!Set.of("TRANSID","COMMAREA","LENGTH","IMMEDIATE","RESP","RESP2","NOHANDLE").contains(o.name())||!names.add(o.name()))return false;
                boolean flag=Set.of("IMMEDIATE","NOHANDLE").contains(o.name());
                if(flag==o.operand().isPresent())return false;
                if(flag)continue;
                if(o.name().equals("TRANSID")&&o.operand().flatMap(CicsCommandSyntax::literal).isPresent())continue;
                if(o.name().equals("LENGTH")&&o.operand().filter(EmbeddedExpressionSyntax::supported).isPresent())continue;
                var host=hosts.get(o.start());if(host==null)return false;
                if(Set.of("COMMAREA","RESP","RESP2").contains(o.name()))write(host.reference());
            }
            return !names.contains("LENGTH")||names.contains("COMMAREA");
        }
        void statement(Ast.Statement s,CompilationUnitModel.ProgramUnit unit,StorageAccessSemantics storage,CicsProgramControlAnalyzer.Contribution cics) {
            var summary=storage.effects(new StorageLayoutSemantics.Key(unit.id(),s.meta().id())).or(()->StatementEffectSummary.of(s));
            if(summary.isPresent()) {
                var effect=summary.get();if(!effect.completeMutationBound())blockers.add("OPEN_STATEMENT_EFFECTS:"+s.meta().id());
                effect.mayWrites().forEach(this::write);effect.exposedRegions().forEach(this::write);return;
            }
            if(s instanceof Ast.MoveStatement move){move.targets().forEach(this::write);return;}
            if(s instanceof Ast.CallStatement call) {
                for(var a:call.arguments()) {
                    if(a.argumentKind()==Ast.CallArgumentKind.OMITTED)continue;
                    if(a.argumentKind()!=Ast.CallArgumentKind.VALUE){blockers.add("CALL_ARGUMENT_FORM_UNAVAILABLE");continue;}
                    if(a.passingMode()==Ast.PassingMode.REFERENCE)write(a.value());
                    else if(a.passingMode()!=Ast.PassingMode.CONTENT&&a.passingMode()!=Ast.PassingMode.VALUE)blockers.add("CALL_PASSING_UNAVAILABLE");
                }
                if(call.returning()!=null)write(call.returning());return;
            }
            if(s instanceof Ast.PerformStatement p) {
                if(p.repetition()==Ast.PerformRepetition.UNKNOWN)blockers.add("PERFORM_CONTROL_UNAVAILABLE");
                p.controls().stream().filter(c->c.context()==Ast.PerformControlContext.CONTROL_VARIABLE).forEach(c->write(c.expression()));return;
            }
            if(s instanceof Ast.IfStatement||s instanceof Ast.EvaluateStatement||s instanceof Ast.GoToStatement||s instanceof Ast.GobackStatement||s instanceof Ast.NextSentenceStatement)return;
            if(s instanceof Ast.EmbeddedLanguageStatement e&&e.language()==Ast.EmbeddedLanguage.CICS) {
                var command=cics.commandFact(unit.id(),s.meta().id());
                if(command.flatMap(CicsCommandSemantics.Fact::hostEffects).isPresent()) {
                    for(var host:e.hostOperands())if(host.role()==Ast.EmbeddedHostRole.WRITE||host.role()==Ast.EmbeddedHostRole.READ_WRITE)write(host.reference());return;
                }
                if(cics.handlerFact(unit.id(),s.meta().id()).filter(CicsHandlerSemantics.Fact::registrationPreservesApplicationMemory).isPresent())return;
                var file=cics.fileFact(unit.id(),s.meta().id());
                if(file.filter(f->f.gaps().isEmpty()).isPresent()) {
                    var hosts=new HashMap<Integer,Ast.EmbeddedHostOperand>();e.hostOperands().forEach(h->hosts.put(h.optionStart(),h));
                    for(var option:file.get().options())if(option.role()==CicsFileControlAnalyzer.Role.WRITE||option.role()==CicsFileControlAnalyzer.Role.READ_WRITE) {
                        var host=hosts.get(option.syntax().start());
                        var expression=e.expressionOperands().stream().filter(x->x.optionStart()==option.syntax().start()).findFirst();
                        // LENGTH OF denotes a separate implicit register, never the contents of its operand.
                        // This memory invariant grants no successful command/normal-control claim.
                        boolean separateRegister=option.syntax().name().equals("LENGTH")&&expression.isPresent()
                            &&expression.get().expression() instanceof Ast.SpecialRegisterExpression r&&r.registerName().equals("LENGTH");
                        if(host!=null)write(host.reference());
                        else if(!separateRegister)blockers.add("CICS_FILE_HOST_UNAVAILABLE_"+option.syntax().name()+":"+s.meta().id());
                    }
                    return;
                }
                var transfer=cics.fact(unit.id(),s.meta().id());
                if(transfer.filter(f->f.gaps().isEmpty()).isPresent()) {
                    // Input PROGRAM/LENGTH names do not escape. COMMAREA and all response outputs do.
                    var hosts=new HashMap<Integer,Ast.EmbeddedHostOperand>();e.hostOperands().forEach(h->hosts.put(h.optionStart(),h));
                    for(var o:transfer.get().options())if(Set.of("COMMAREA","RESP","RESP2","INPUTMSG").contains(o.name())) {
                        var host=hosts.get(o.start());if(host==null)blockers.add("CICS_TRANSFER_HOST_UNAVAILABLE");else write(host.reference());
                    }
                    return;
                }
                if(returnFootprint(e))return;
                blockers.add("CICS_EFFECTS_UNAVAILABLE:"+s.meta().id());return;
            }
            blockers.add("STATEMENT_EFFECTS_UNAVAILABLE:"+s.meta().id());
        }
    }
}
