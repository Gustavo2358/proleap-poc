package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Conditional source-memory effects, separate from the feasible outcome/control
 * proof. Normal completion of AIR is not SUCCESS here. FileIoMemory is the union
 * used for lifetime evidence; these cases retain execution order for lowering. */
public final class FileIoEffects {
    public enum Outcome { SUCCESS, END, INVALID_KEY, OTHER_ERROR }
    public enum Kind { MAY_UNKNOWN, MUST_UNKNOWN, COPY_BYTES, FIT_TEXT }
    public record Step(FileIoMemory.Role role,Kind kind,FileIoMemory.Target destination,
            Optional<FileIoMemory.Target> source,List<String> gaps,Ast.SourceProvenance origin) {
        public Step {Objects.requireNonNull(role);Objects.requireNonNull(kind);Objects.requireNonNull(destination);source=Objects.requireNonNull(source);gaps=List.copyOf(gaps);Objects.requireNonNull(origin);}
    }
    public record OutcomeCase(Outcome outcome,List<Step> steps) {
        public OutcomeCase {steps=List.copyOf(steps);}
    }
    public record Operation(int ordinal,List<FileIoMemory.Target> ioReads,List<Step> before,List<OutcomeCase> outcomes,
            boolean unknownReadBound,boolean unknownWriteBound,List<String> gaps) {
        public Operation {ioReads=List.copyOf(ioReads);before=List.copyOf(before);outcomes=List.copyOf(outcomes);gaps=List.copyOf(gaps);}
    }
    private final Map<Key,List<Operation>> operations;
    private FileIoEffects(Map<Key,List<Operation>> operations){this.operations=Map.copyOf(operations);}
    public Optional<Operation> operation(Key statement,int ordinal){return operations.getOrDefault(statement,List.of()).stream().filter(p->p.ordinal()==ordinal).findFirst();}
    static FileIoEffects analyze(FileIoMemory memory,StorageLayoutSemantics storage) {
        var bases=new HashMap<Key,Base>();var result=new HashMap<Key,List<Operation>>();var preparedUnits=new HashSet<ResolutionContracts.ProgramUnitId>();
        for(var statement:memory.statements()) {
            var unit=statement.statement().unit();
            if(preparedUnits.add(unit)) {
                storage.layout(unit).bases().forEach(b->bases.put(b.id(),b));
            }
            var plans=new ArrayList<Operation>();var surface=statement.surface();
            for(var op:statement.operations()) {
                var before=new ArrayList<Step>();var outcomes=new ArrayList<OutcomeCase>();var reads=new ArrayList<FileIoMemory.Target>();
                var gaps=new LinkedHashSet<>(op.gaps());boolean unknownRead=false;
                var file=op.file().orElse(null);
                boolean sortRecord=FileIoMemory.expectedKind(surface,op.ordinal())==Ast.FileKind.SD;
                boolean nativeProfile=surface.profile()==Ast.FileSyntaxProfile.N_LR&&file!=null&&file.description().kind()==(sortRecord?Ast.FileKind.SD:Ast.FileKind.FD)
                    &&(sortRecord||file.binding().control().assignment().form()==Ast.AssignmentForm.IBM_NAME);
                if(file!=null&&(surface.command()==Ast.FileCommand.WRITE||surface.command()==Ast.FileCommand.REWRITE||surface.command()==Ast.FileCommand.RELEASE))reads.addAll(file.records());
                if(file!=null){var auxiliary=new ArrayList<Ast.FileAuxiliary>(file.binding().control().auxiliary());auxiliary.addAll(file.description().auxiliary());
                    for(var clause:auxiliary){boolean read=clause.kind()==Ast.FileAuxKind.RECORD&&(surface.command()==Ast.FileCommand.WRITE||surface.command()==Ast.FileCommand.REWRITE||surface.command()==Ast.FileCommand.RELEASE)
                        ||clause.kind()==Ast.FileAuxKind.PASSWORD&&surface.command()==Ast.FileCommand.OPEN
                        ||clause.kind()==Ast.FileAuxKind.LINAGE&&file.description().kind()==Ast.FileKind.FD&&(surface.command()==Ast.FileCommand.WRITE||surface.command()==Ast.FileCommand.OPEN&&(surface.files().get(op.ordinal()).mode()==Ast.FileOpenMode.OUTPUT||surface.files().get(op.ordinal()).mode()==Ast.FileOpenMode.EXTEND));
                        if(read)for(var reference:clause.data()){var target=memory.target(new Key(file.entity().programUnitId(),reference.reference().meta().id())).map(t->FileIoMemory.inUnit(t,unit));if(target.isPresent())reads.add(target.orElseThrow());else unknownRead=true;}
                    }
                }
                for(var operand:surface.operands())if(operand.role()==Ast.FileOperandRole.KEY||operand.role()==Ast.FileOperandRole.ADVANCING) {
                    var target=memory.target(new Key(unit,operand.value().meta().id()));if(target.isPresent()){reads.add(target.orElseThrow());if(target.orElseThrow().wholeBase())unknownRead=true;}
                    else if(!(operand.value() instanceof Ast.LiteralExpression))unknownRead=true;
                }
                if(file!=null&&surface.command()==Ast.FileCommand.READ)for(var reference:file.binding().control().references())
                    if(reference.role()==Ast.FileReferenceRole.RECORD_KEY||reference.role()==Ast.FileReferenceRole.RELATIVE_KEY) {
                        var target=memory.target(new Key(file.entity().programUnitId(),reference.reference().meta().id())).map(t->FileIoMemory.inUnit(t,unit));if(target.isPresent())reads.add(target.orElseThrow());else unknownRead=true;
                    }
                for(var write:op.writes())if(write.role()==FileIoMemory.Role.FROM_RECORD) {
                    var source=surface.operands().stream().filter(o->o.role()==Ast.FileOperandRole.FROM).findFirst()
                        .flatMap(o->memory.target(new Key(unit,o.value().meta().id())));
                    if(source.isEmpty()||source.orElseThrow().wholeBase())unknownRead=true;
                    var step=transfer(write.target(),source,statement.origin(),bases);before.add(nativeProfile?step:outsideProfile(step));
                }
                // This table describes effects IF the semantic outcome occurs;
                // handler selection and which cases are feasible are independent W4 facts.
                for(var outcome:Outcome.values()) {
                    var steps=new ArrayList<Step>();
                    for(var write:op.writes()) {
                        var role=write.role();if(role==FileIoMemory.Role.FROM_RECORD)continue;
                        if(role==FileIoMemory.Role.INTO||role==FileIoMemory.Role.RELATIVE_KEY||role==FileIoMemory.Role.RECORD_LENGTH) {
                            if(outcome!=Outcome.SUCCESS)continue;
                        }
                        if(role==FileIoMemory.Role.RECORD&&surface.command()==Ast.FileCommand.REWRITE&&outcome==Outcome.INVALID_KEY)continue;
                        var reasons=new ArrayList<String>();var kind=Kind.MAY_UNKNOWN;
                        if(role==FileIoMemory.Role.FILE_STATUS) {
                            if(exactText(write.target(),bases)&&write.target().view().orElseThrow().extent().value().orElseThrow().equals(java.math.BigInteger.TWO))kind=Kind.MUST_UNKNOWN;
                            else reasons.add("FILE_STATUS_STORAGE_NOT_PROVEN");
                        } else if(role==FileIoMemory.Role.INTO) {
                            boolean address=exactText(write.target(),bases);boolean separate=file!=null&&!file.records().isEmpty()
                                &&file.records().stream().allMatch(r->separate(r,write.target(),bases));
                            boolean sender=file!=null&&!file.records().isEmpty()&&file.records().stream().allMatch(r->r.view().filter(View::textual).isPresent());
                            if(!address)reasons.add("FILE_RECEIVER_ADDRESS_NOT_PROVEN");
                            if(!separate)reasons.add("FILE_TRANSFER_ALIAS_NOT_PROVEN");
                            if(!sender)reasons.add("FILE_RECORD_MOVE_CLASS_NOT_PROVEN");
                            if(address&&separate&&sender)kind=Kind.MUST_UNKNOWN;
                        }
                        var step=new Step(role,kind,write.target(),Optional.empty(),reasons,statement.origin());steps.add(nativeProfile?step:outsideProfile(step));
                    }
                    // READ-without-INTO completes (including status) before the
                    // implied MOVE; dependent addressing therefore happens last.
                    steps.sort(Comparator.comparingInt(s->order(s.role())));
                    outcomes.add(new OutcomeCase(outcome,steps));
                }
                if(file!=null&&!file.entity().programUnitId().equals(unit))gaps.add("FILE_CAPTURE_PHYSICAL_VIEW_UNAVAILABLE");
                if(unknownRead)gaps.add("FILE_READ_BOUND_NOT_PROVEN");
                if(!nativeProfile)gaps.add("FILE_EFFECT_PROFILE_NOT_PROVEN");
                plans.add(new Operation(op.ordinal(),reads,before,outcomes,unknownRead||!nativeProfile,!op.bounded()||!nativeProfile,List.copyOf(gaps)));
            }
            result.put(statement.statement(),List.copyOf(plans));
        }
        return new FileIoEffects(result);
    }
    private static Step outsideProfile(Step step) {
        var gaps=new LinkedHashSet<>(step.gaps());gaps.add("FILE_EFFECT_PROFILE_NOT_PROVEN");
        return new Step(step.role(),Kind.MAY_UNKNOWN,step.destination(),step.source(),List.copyOf(gaps),step.origin());
    }
    private static int order(FileIoMemory.Role role){return switch(role){case RECORD,FROM_RECORD->0;case RELATIVE_KEY->1;case RECORD_LENGTH->2;case FILE_STATUS->3;case ADDITIONAL_STATUS->4;case INTO->5;};}
    private static Step transfer(FileIoMemory.Target destination,Optional<FileIoMemory.Target> source,Ast.SourceProvenance origin,Map<Key,Base> bases) {
        var gaps=new ArrayList<String>();Kind kind=Kind.MAY_UNKNOWN;
        if(!exactText(destination,bases))gaps.add("FILE_RECEIVER_ADDRESS_NOT_PROVEN");
        if(source.isEmpty()||!exactText(source.orElseThrow(),bases))gaps.add("FILE_SOURCE_ADDRESS_NOT_PROVEN");
        if(source.isEmpty()||!separate(source.orElseThrow(),destination,bases))gaps.add("FILE_TRANSFER_ALIAS_NOT_PROVEN");
        if(gaps.isEmpty())kind=switch(StorageAccessSemantics.copyKind(source.orElseThrow().view().orElseThrow(),destination.view().orElseThrow(),bases)) {
            case COPY_BYTES->Kind.COPY_BYTES;case FIT_TEXT->Kind.FIT_TEXT;default->throw new IllegalStateException("proved source transfer lost its exact MOVE contract");
        };
        return new Step(FileIoMemory.Role.FROM_RECORD,kind,destination,source,gaps,origin);
    }
    private static boolean exactText(FileIoMemory.Target target,Map<Key,Base> bases) {
        var view=target.view().orElse(null);var base=view==null?null:bases.get(view.base());
        return !target.wholeBase()&&view!=null&&view.textual()&&view.offset().value().isPresent()
            &&view.extent().value().filter(n->n.signum()>0).isPresent()&&base!=null&&base.extent().value().isPresent();
    }
    private static boolean separate(FileIoMemory.Target a,FileIoMemory.Target b,Map<Key,Base> bases) {
        var av=a.view().orElse(null);var bv=b.view().orElse(null);if(av==null||bv==null)return false;
        if(av.base().equals(bv.base())&&(a.wholeBase()||b.wholeBase()))return false;
        if(av.base().equals(bv.base())&&(av.offset().value().isEmpty()||bv.offset().value().isEmpty()||av.extent().value().isEmpty()||bv.extent().value().isEmpty()))return false;
        return StorageAccessSemantics.disjoint(av,bv,bases);
    }
}
