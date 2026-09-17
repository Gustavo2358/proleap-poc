package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.ResolutionContracts.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.Key;

/** N-LR auxiliary declaration semantics; joins canonical nominal identities, never runtime resources. */
public final class FileAuxiliarySemantics {
    public enum Effect { DOCUMENTARY, RECORD_ALIAS, CONDITIONAL_RECORD_ALIAS, BUFFER_ALLOCATION, RECORD_LAYOUT,
        PAGE_CONTROL, ACCESS_CHECK, CHECKPOINT, OUTSIDE_N_LR }
    public record FileRef(ResolutionStatus status,List<SemanticEntityId> candidates,FileDeclarationSemantics.AccessMethod accessMethod,Ast.SourceProvenance origin){public FileRef{candidates=List.copyOf(candidates);}}
    public record Clause(Key id,Ast.FileAuxKind kind,Effect effect,List<FileRef> files,List<Ast.FileAuxData> data,
            List<Ast.FileAuxParameter> parameters,Optional<Ast.FileAssignment> checkpoint,Ast.FileTrigger trigger,List<String> gaps,Ast.SourceProvenance origin){
        public Clause{files=List.copyOf(files);data=List.copyOf(data);parameters=List.copyOf(parameters);gaps=List.copyOf(gaps);}}
    private final List<Clause> clauses;
    private FileAuxiliarySemantics(List<Clause> clauses){this.clauses=List.copyOf(clauses);}
    public List<Clause> clauses(){return clauses;}
    static FileAuxiliarySemantics analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,CompilationUnitSymbolTables tables){
        var bindings=new HashMap<Key,ReferenceResolution.Entry>();for(var b:resolution.entries())bindings.put(new Key(b.occurrence().programUnitId(),b.occurrence().referenceAstNodeId()),b);
        var names=new HashSet<String>();var pendingClauses=new ArrayList<Clause>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var nodes=new LinkedHashMap<Integer,Ast.Node>();collect(unit.program(),unit.program(),nodes);
            var table=tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            var byAst=new HashMap<Integer,SemanticEntityId>();var controls=new HashMap<SemanticEntityId,Ast.FileBinding>();var descriptions=new HashMap<SemanticEntityId,Ast.FileDescription>();
            for(var entity:table.entities())if(entity.kind()==SymbolTable.EntityKind.FILE){var id=new SemanticEntityId(unit.id(),SemanticEntityDomain.FILE_ENTITY,entity.id());
                for(var symbol:entity.declarationSymbolIds()){int ast=table.symbols().get(symbol).declarationAstNodeId();byAst.put(ast,id);var node=nodes.get(ast);
                    if(node instanceof Ast.FileBinding f){controls.put(id,f);if(f.control()!=null&&f.control().assignment().externalFileName()!=null)names.add(f.control().assignment().externalFileName());}
                    if(node instanceof Ast.FileDescription f)descriptions.put(id,f);}}
            var logicalNames=new HashSet<String>();controls.values().forEach(f->logicalNames.add(f.logicalName().toUpperCase(Locale.ROOT)));
            var subjects=new HashMap<Integer,SemanticEntityId>();
            for(var node:nodes.values()){
                List<Ast.FileAuxiliary> aux=node instanceof Ast.FileBinding f&&f.control()!=null?f.control().auxiliary():node instanceof Ast.FileDescription f?f.auxiliary():List.of();
                for(var c:aux)subjects.put(c.meta().id(),byAst.get(node.meta().id()));}
            int sortReruns=(int)nodes.values().stream().filter(n->n instanceof Ast.FileAuxiliary c&&c.kind()==Ast.FileAuxKind.RERUN&&c.trigger()==Ast.FileTrigger.SORT_MERGE).count();
            for(var node:nodes.values()){
                Ast.FileAuxiliary c;
                if(node instanceof Ast.FileAuxiliary value)c=value;
                else if(node instanceof Ast.FileAreaSharing s)c=new Ast.FileAuxiliary(s.meta(),switch(s.kind()){case AREA->Ast.FileAuxKind.SAME_AREA;case RECORD->Ast.FileAuxKind.SAME_RECORD_AREA;case SORT->Ast.FileAuxKind.SAME_SORT_AREA;case SORT_MERGE->Ast.FileAuxKind.SAME_SORT_MERGE_AREA;},s.files(),List.of(),List.of(),Optional.empty(),Ast.FileTrigger.NONE);
                else continue;
                var files=new ArrayList<FileRef>();var gaps=new ArrayList<String>();var subject=subjects.get(c.meta().id());
                if(subject!=null)files.add(new FileRef(ResolutionStatus.RESOLVED,List.of(subject),method(subject,controls,descriptions),c.meta().provenance()));
                for(var ref:c.files()){var b=bindings.get(new Key(unit.id(),ref.meta().id()));files.add(new FileRef(b==null?ResolutionStatus.UNRESOLVED:b.status(),b==null?List.of():b.candidates().stream().map(ReferenceResolution.Candidate::entityId).toList(),b!=null&&b.status()==ResolutionStatus.RESOLVED?method(b.candidates().get(0).entityId(),controls,descriptions):FileDeclarationSemantics.AccessMethod.UNKNOWN,ref.meta().provenance()));}
                var targets=files.stream().filter(f->f.status()==ResolutionStatus.RESOLVED).flatMap(f->f.candidates().stream()).toList();
                if(files.stream().anyMatch(f->f.status()!=ResolutionStatus.RESOLVED))gaps.add("FILE_AUX_BINDING_NOT_PROVEN");
                var effect=effect(c.kind());
                if(c.kind()==Ast.FileAuxKind.SAME_AREA){boolean vsam=!files.isEmpty()&&files.stream().allMatch(f->f.accessMethod()==FileDeclarationSemantics.AccessMethod.VSAM);
                    boolean qsam=!files.isEmpty()&&files.stream().allMatch(f->f.accessMethod()==FileDeclarationSemantics.AccessMethod.QSAM);
                    effect=vsam?Effect.RECORD_ALIAS:qsam?Effect.DOCUMENTARY:Effect.CONDITIONAL_RECORD_ALIAS;if(!vsam&&!qsam)gaps.add("SAME_AREA_ACCESS_METHOD_NOT_PROVEN");}
                if((c.kind()==Ast.FileAuxKind.LINAGE||c.kind()==Ast.FileAuxKind.CODE_SET)&&subject!=null&&descriptions.containsKey(subject)&&descriptions.get(subject).kind()==Ast.FileKind.SD)effect=Effect.DOCUMENTARY;
                if(c.kind()==Ast.FileAuxKind.RECORDING_MODE&&subject!=null&&vsam(controls.get(subject)))effect=Effect.DOCUMENTARY;
                if(c.kind()==Ast.FileAuxKind.LINAGE&&effect!=Effect.DOCUMENTARY){if(subject!=null&&controls.containsKey(subject)&&(controls.get(subject).control().organization()==Ast.FileOrganization.INDEXED||controls.get(subject).control().organization()==Ast.FileOrganization.RELATIVE||controls.get(subject).control().organization()==Ast.FileOrganization.LINE_SEQUENTIAL))gaps.add("LINAGE_REQUIRES_SEQUENTIAL_FILE");}
                if(c.kind()==Ast.FileAuxKind.MULTIPLE_FILE||c.kind()==Ast.FileAuxKind.APPLY_WRITE_ONLY){if(targets.stream().anyMatch(id->vsam(controls.get(id))))gaps.add("FILE_AUX_REQUIRES_QSAM");}
                if(c.kind().name().startsWith("SAME_")){
                    if(targets.size()<2||new HashSet<>(targets).size()!=targets.size())gaps.add("SAME_FILE_SET_INVALID");
                    if(targets.stream().anyMatch(id->!id.programUnitId().equals(unit.id())||!descriptions.containsKey(id)||descriptions.get(id).visibility()==Ast.DeclarationVisibility.EXTERNAL))gaps.add("SAME_FILE_OWNER_RESTRICTION");
                    if(c.kind()==Ast.FileAuxKind.SAME_RECORD_AREA||effect==Effect.RECORD_ALIAS){
                        if(targets.stream().anyMatch(id->descriptions.containsKey(id)&&(descriptions.get(id).visibility()!=Ast.DeclarationVisibility.LOCAL||descriptions.get(id).recordClauses().stream().anyMatch(r->r.form()==Ast.FileRecordForm.FIXED&&r.minimum().filter(n->n.signum()==0).isPresent()))))gaps.add("SAME_RECORD_DECLARATION_RESTRICTION");
                    }
                    if((c.kind()==Ast.FileAuxKind.SAME_SORT_AREA||c.kind()==Ast.FileAuxKind.SAME_SORT_MERGE_AREA)&&targets.stream().noneMatch(id->descriptions.containsKey(id)&&descriptions.get(id).kind()==Ast.FileKind.SD))gaps.add("SAME_SORT_REQUIRES_SD");
                }
                if(c.kind()==Ast.FileAuxKind.RERUN){
                    if(unit.program().attributes().recursive())gaps.add("RERUN_RECURSIVE_PROGRAM");
                    if(c.trigger()==Ast.FileTrigger.RECORD_COUNT&&c.parameters().stream().filter(p->p.role().equals("interval")).anyMatch(p->new java.math.BigInteger(p.value()).signum()<=0))gaps.add("RERUN_INTERVAL_INVALID");
                    if(c.trigger()==Ast.FileTrigger.UNSUPPORTED){effect=Effect.OUTSIDE_N_LR;gaps.add("RERUN_OUTSIDE_N_LR");}
                    if(c.checkpoint().isEmpty()||c.checkpoint().orElseThrow().form()!=Ast.AssignmentForm.IBM_NAME)gaps.add("CHECKPOINT_ASSIGNMENT_NOT_PROVEN");
                    if(c.trigger()==Ast.FileTrigger.SORT_MERGE&&(unit.parentId()!=null||sortReruns>1))gaps.add("RERUN_SORT_PROGRAM_RESTRICTION");
                    if(c.trigger()!=Ast.FileTrigger.SORT_MERGE&&c.trigger()!=Ast.FileTrigger.UNSUPPORTED&&files.size()!=1)gaps.add("RERUN_TRIGGER_FILE_MISSING");
                    for(var id:targets){var control=controls.get(id);var fd=descriptions.get(id);
                        if(!id.programUnitId().equals(unit.id())||control==null||fd==null||fd.visibility()==Ast.DeclarationVisibility.EXTERNAL||vsam(control)&&control.control().organization()!=Ast.FileOrganization.SEQUENTIAL&&control.control().organization()!=Ast.FileOrganization.UNSPECIFIED)gaps.add("RERUN_TRIGGER_FILE_RESTRICTION");}
                }
                if(effect==Effect.OUTSIDE_N_LR&&gaps.isEmpty())gaps.add("FILE_AUX_OUTSIDE_N_LR");
                if(c.kind()==Ast.FileAuxKind.RERUN&&c.trigger()==Ast.FileTrigger.END_VOLUME&&c.checkpoint().isPresent()){
                    var written=c.checkpoint().orElseThrow().original();
                    if(logicalNames.contains(written.toUpperCase(Locale.ROOT)))gaps.add("RERUN_END_VOLUME_TARGET_FORM_NOT_PROVEN");
                }
                pendingClauses.add(new Clause(new Key(unit.id(),c.meta().id()),c.kind(),effect,files,c.data(),c.parameters(),c.checkpoint(),c.trigger(),gaps,c.meta().provenance()));
            }
        }
        var out=new ArrayList<Clause>();for(var c:pendingClauses){var gaps=new ArrayList<>(c.gaps());
            if(c.kind()==Ast.FileAuxKind.RERUN&&c.checkpoint().map(Ast.FileAssignment::externalFileName).filter(names::contains).isPresent())gaps.add("CHECKPOINT_ASSIGNMENT_COLLISION");
            var checkpoint=c.checkpoint();if(gaps.contains("RERUN_END_VOLUME_TARGET_FORM_NOT_PROVEN"))checkpoint=checkpoint.map(n->new Ast.FileAssignment(Ast.AssignmentForm.MISSING,n.original(),null));
            out.add(new Clause(c.id(),c.kind(),c.effect(),c.files(),c.data(),c.parameters(),checkpoint,c.trigger(),gaps,c.origin()));}
        return new FileAuxiliarySemantics(out);
    }
    private static FileDeclarationSemantics.AccessMethod method(SemanticEntityId id,Map<SemanticEntityId,Ast.FileBinding> controls,Map<SemanticEntityId,Ast.FileDescription> descriptions){
        if(descriptions.containsKey(id)&&descriptions.get(id).kind()==Ast.FileKind.SD)return FileDeclarationSemantics.AccessMethod.UNKNOWN;
        return controls.containsKey(id)?FileDeclarationSemantics.accessMethod(controls.get(id).control()):FileDeclarationSemantics.AccessMethod.UNKNOWN;
    }
    static boolean vsam(Ast.FileBinding f){return f!=null&&FileDeclarationSemantics.accessMethod(f.control())==FileDeclarationSemantics.AccessMethod.VSAM;}
    private static Effect effect(Ast.FileAuxKind kind){return switch(kind){
        case RERUN->Effect.CHECKPOINT;case SAME_RECORD_AREA->Effect.RECORD_ALIAS;case SAME_AREA->Effect.CONDITIONAL_RECORD_ALIAS;
        case RESERVE,APPLY_WRITE_ONLY,BLOCK->Effect.BUFFER_ALLOCATION;case RECORD,RECORDING_MODE,CODE_SET->Effect.RECORD_LAYOUT;
        case LINAGE->Effect.PAGE_CONTROL;case PASSWORD->Effect.ACCESS_CHECK;case COMMITMENT_CONTROL,REPORT->Effect.OUTSIDE_N_LR;
        default->Effect.DOCUMENTARY;};}
    private static void collect(Ast.Node node,Ast.Program root,Map<Integer,Ast.Node> out){if(node instanceof Ast.Program&&node!=root)return;out.put(node.meta().id(),node);for(var child:Ast.children(node))collect(child,root,out);}
}
