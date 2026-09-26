package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;

/** Published event routing using ordinary AIR branches and bounded return envelopes.
 * USE bodies are shared; recursive invocation never clones source occurrences. */
final class FileControlLowering {
    private final UnitId unit;
    private final LocalIds ids;
    private final SourceOrigins origins;
    private final List<Evidence.Uncertainty> uncertainties;
    private final Map<String,FileFacts.Declarative> declarations=new LinkedHashMap<>();
    private final Map<SpInput.StatementId,LabelId> completions=new HashMap<>();
    private final Map<String,Set<LabelId>> resumes=new HashMap<>();
    FileControlLowering(SpInput input,UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> uncertainties) {
        this.unit=unit;this.ids=ids;this.origins=origins;this.uncertainties=uncertainties;
        for(var d:input.fileInventory().declaratives()) {
            declarations.put(d.id(),d);
            if(d.kind()==FileFacts.UseKind.AFTER_EXCEPTION)for(var s:d.completions())completions.put(s,label(ids,"use/"+d.id()+"/return"));
        }
    }
    LabelId completion(SpInput.StatementId statement,LabelId ordinary){return completions.getOrDefault(statement,ordinary);}
    private LabelId label(LocalIds local,String key){return new LabelId(unit,local.id("label","file-control",unit.localId(),key));}
    private OperationId operation(LocalIds local,String key){return new OperationId(unit,local.id("operation","file-control",unit.localId(),key));}
    private Operations.Header exact(OperationId op,OriginId origin){return new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of());}
    private Operations.Header unknown(OperationId op,OriginId origin,String code) {
        var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","file-control",op.localId(),code));
        uncertainties.add(new Evidence.Uncertainty(reason,code,List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(op)),"Published conditional I/O dispatch",origin));
        var scope=new Scopes.EntityScope(List.of(op));var known=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(reason));
        return new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(open,known,known,open,known),List.of(reason));
    }
    private Operations.Branch choice(LocalIds local,String key,OriginId origin,LabelId yes,LabelId no) {
        var op=operation(local,key);var header=unknown(op,origin,"FILE_EVENT_VALUE_UNKNOWN");
        return new Operations.Branch(header,new Expressions.Unknown(new Operand.Header(new OperandId(new OperationOwner(op),"condition"),Operand.Role.PREDICATE,origin),Types.known(Types.Builtin.BOOL),List.of(),Scopes.NoMemory.INSTANCE,header.uncertainties().getFirst()),yes,no);
    }
    private Terminator continuing(LocalIds local,String key,OriginId origin,LabelId next,boolean critical) {
        var op=operation(local,key);
        if(next!=null&&!critical)return new Operations.Jump(exact(op,origin),next);
        var header=unknown(op,origin,critical?"FILE_CRITICAL_ERROR_EXIT_NOT_PROVEN":"FILE_NORMAL_CONTINUATION_NOT_PROVEN");
        return new Operations.Opaque(header,"file-event-continuation",List.of(),List.of(),
            new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,List.of(),Scopes.NoMemory.INSTANCE,List.of()),
                new Control.ControlEnvelope(next==null?List.of():List.of(new Control.JumpAlternative(next)),critical
                    ?new Scopes.WithinControl(new Scopes.UnitControl(unit,false,true,true,false,false,false))
                    :next==null?new Scopes.WithinControl(new Scopes.LabelsControl(List.of())):Scopes.NoControl.INSTANCE),new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE)));
    }
    List<Sequence> after(String key,FileFacts.Use use,FileFacts.EffectPlan plan,LabelId next,OriginId origin,LocalIds local,FileMemoryLowering memory) {
        var control=use.control().orElseThrow();var routes=control.routes();var result=new ArrayList<Sequence>();
        if(routes.size()==1)result.add(new Sequence(memory.label(key+"/select/0"),List.of(),new Operations.Jump(exact(operation(local,key+"/select/0"),origin),memory.label(key+"/event/"+routes.getFirst().event())),origin));
        var effects=new EnumMap<FileFacts.EffectOutcome,List<FileFacts.MemoryStep>>(FileFacts.EffectOutcome.class);plan.outcomes().forEach(o->effects.put(o.outcome(),o.steps()));
        for(int n=0;n<routes.size();n++) {
            var route=routes.get(n);var eventKey=key+"/event/"+route.event();var entry=memory.label(eventKey);var dispatch=memory.label(eventKey+"/dispatch/0");var resume=memory.label(eventKey+"/resume");
            if(n<routes.size()-1) {
                var alternative=n+2==routes.size()?memory.label(key+"/event/"+routes.get(n+1).event()):memory.label(key+"/select/"+(n+1));
                result.add(new Sequence(memory.label(key+"/select/"+n),List.of(),choice(local,key+"/select/"+n,origin,entry,alternative),origin));
            }
            var steps=effects.get(route.effects());result.addAll(memory.steps(eventKey,steps,entry,dispatch,origin));
            if(steps.isEmpty())result.add(new Sequence(entry,List.of(),new Operations.Jump(exact(operation(local,eventKey+"/enter"),origin),dispatch),origin));
            var destinations=route.destinations();
            for(int i=0;i<destinations.size();i++) {
                var at=memory.label(eventKey+"/dispatch/"+i);var selected=memory.label(eventKey+"/destination/"+i);var d=destinations.get(i);
                if(i+1<destinations.size())result.add(new Sequence(at,List.of(),choice(local,eventKey+"/dispatch/"+i,origin,selected,memory.label(eventKey+"/dispatch/"+(i+1))),origin));
                else selected=at;
                Terminator term;
                if(d.kind()==FileFacts.DestinationKind.USE) {
                    var declaration=declarations.get(d.declarative().orElseThrow());var target=declaration.entry().map(s->PartialProgramAssembler.label(s,unit,ids));
                    if(target.isPresent()) {
                        resumes.computeIfAbsent(declaration.id(),ignored->new LinkedHashSet<>()).add(resume);
                        term=continuing(local,eventKey+"/use/"+i,origin,target.orElseThrow(),false);
                    }
                    else term=continuing(local,eventKey+"/empty-use/"+i,origin,resume,false);
                } else if(d.kind()==FileFacts.DestinationKind.HANDLER) {
                    var handler=use.surface().orElseThrow().handlers().stream().filter(h->h.kind()==d.handler().orElseThrow()).findFirst().orElseThrow();
                    var target=handler.statements().isEmpty()?resume:PartialProgramAssembler.label(handler.statements().getFirst(),unit,local);
                    term=continuing(local,eventKey+"/handler/"+i,origin,target,false);
                } else term=continuing(local,eventKey+"/continue/"+i,origin,resume,false);
                result.add(new Sequence(selected,List.of(),term,origin));
            }
            result.add(new Sequence(resume,List.of(),continuing(local,eventKey+"/resume",origin,next,route.criticalExit()),origin));
        }
        return List.copyOf(result);
    }
    List<Sequence> complete(List<Sequence> source) {
        var result=new ArrayList<>(source);
        for(var d:declarations.values())if(d.kind()==FileFacts.UseKind.AFTER_EXCEPTION&&!d.completions().isEmpty()) {
            var origin=origins.source("file-use-declaration",d.id(),d.provenance());var key="use/"+d.id()+"/return";
            var alternatives=resumes.getOrDefault(d.id(),Set.of()).stream().sorted(Comparator.comparing(LabelId::localId))
                .<Control.ControlAlternative>map(Control.JumpAlternative::new).toList();
            var envelope=new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,List.of(),Scopes.NoMemory.INSTANCE,List.of()),
                new Control.ControlEnvelope(alternatives,alternatives.isEmpty()?new Scopes.WithinControl(new Scopes.LabelsControl(List.of())):Scopes.NoControl.INSTANCE),
                new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
            result.add(new Sequence(label(ids,key),List.of(),new Operations.Opaque(unknown(operation(ids,key),origin,"LOCAL_RETURN_CONTEXT_NOT_PROVEN"),"use-body-return",List.of(),List.of(),envelope),origin));
        }
        return List.copyOf(result);
    }
}
