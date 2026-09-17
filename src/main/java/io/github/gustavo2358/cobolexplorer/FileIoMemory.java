package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Canonical native-I/O mutation inventory. This is the union of possible writes,
 * not an unconditional write sequence or a MUST proof. Uses nominal identities;
 * implicit record/status destinations never manufacture AST occurrences. */
public final class FileIoMemory {
    public enum Role { RECORD, INTO, FROM_RECORD, FILE_STATUS, ADDITIONAL_STATUS, RELATIVE_KEY, RECORD_LENGTH }
    public record Target(Key declaration,Optional<Key> reference,Optional<View> view,boolean wholeBase,Ast.SourceProvenance origin) {
        public Target {Objects.requireNonNull(declaration);Objects.requireNonNull(reference);Objects.requireNonNull(view);Objects.requireNonNull(origin);}
    }
    public record Write(Role role,Target target) { }
    public record File(ResolutionContracts.SemanticEntityId entity,Ast.FileDescription description,
            Ast.FileBinding binding,List<Target> records,boolean sameRecordArea) {
        public File {records=List.copyOf(records);}
    }
    public record Operation(int ordinal,Optional<File> file,List<Write> writes,List<String> gaps) {
        public Operation {file=Objects.requireNonNull(file);writes=List.copyOf(writes);gaps=List.copyOf(gaps);}
        public boolean bounded(){return gaps.isEmpty();}
    }
    public record Statement(Key statement,Ast.FileIoSurface surface,Ast.SourceProvenance origin,List<Operation> operations) {
        public Statement {operations=List.copyOf(operations);}
        public boolean bounded(){return !operations.isEmpty()&&operations.stream().allMatch(Operation::bounded);}
    }
    private final Map<Key,Statement> statements;
    private final Map<Key,Target> targets;
    private FileIoMemory(Map<Key,Statement> statements,Map<Key,Target> targets){this.statements=Map.copyOf(statements);this.targets=Map.copyOf(targets);}
    public Optional<Statement> statement(Key key){return Optional.ofNullable(statements.get(key));}
    public Collection<Statement> statements(){return statements.values();}
    public Optional<Target> target(Key reference){return Optional.ofNullable(targets.get(reference));}

    static FileIoMemory analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,StorageLayoutSemantics storage,
            Map<Key,StorageAccessSemantics.Access> accesses) {
        var bindings=new HashMap<Key,ReferenceResolution.Entry>();
        for(var entry:resolution.entries())bindings.put(new Key(entry.occurrence().programUnitId(),entry.occurrence().referenceAstNodeId()),entry);
        var result=new LinkedHashMap<Key,Statement>();var targets=new HashMap<Key,Target>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var table=storage.symbolTables().forProgramUnit(unit.id()).orElseThrow().symbolTable();
            var nodes=new HashMap<Integer,Ast.Node>();var pending=new ArrayDeque<Ast.Node>();pending.push(unit.program());
            var statementNodes=new ArrayList<Ast.Statement>();
            while(!pending.isEmpty()) {
                var node=pending.pop();if(node instanceof Ast.Program&&node!=unit.program())continue;
                nodes.put(node.meta().id(),node);if(node instanceof Ast.Statement s)statementNodes.add(s);
                var children=Ast.children(node);for(int i=children.size()-1;i>=0;i--)pending.push(children.get(i));
            }
            var views=new HashMap<Integer,View>();var byEntity=new HashMap<ResolutionContracts.SemanticEntityId,Integer>();
            storage.layout(unit.id()).views().forEach(v->views.put(v.node().node(),v));
            for(var s:table.symbols())if(s.namespace()==SymbolTable.Namespace.DATA)
                byEntity.put(new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,s.id()),s.declarationAstNodeId());
            for(var node:nodes.values())if(node instanceof Ast.DataReference)
                target(unit.id(),node,bindings,byEntity,views,accesses).ifPresent(t->targets.put(new Key(unit.id(),node.meta().id()),t));
            var files=new HashMap<ResolutionContracts.SemanticEntityId,File>();var recordOwners=new HashMap<Integer,File>();
            var baseOwners=new HashMap<Key,Set<Integer>>();
            for(var node:nodes.values())if(node instanceof Ast.FileDescription d)for(var record:d.entries()) {
                var view=views.get(record.meta().id());if(view!=null)baseOwners.computeIfAbsent(view.base(),ignored->new HashSet<>()).add(d.meta().id());
            }
            for(var entity:table.entities())if(entity.kind()==SymbolTable.EntityKind.FILE) {
                var descriptions=new ArrayList<Ast.FileDescription>();var controls=new ArrayList<Ast.FileBinding>();
                for(var symbol:entity.declarationSymbolIds()) {
                    var node=nodes.get(table.symbols().get(symbol).declarationAstNodeId());
                    if(node instanceof Ast.FileDescription d)descriptions.add(d);if(node instanceof Ast.FileBinding c)controls.add(c);
                }
                if(descriptions.size()!=1||controls.size()!=1||controls.get(0).control()==null)continue;
                var description=descriptions.get(0);var records=new ArrayList<Target>();boolean shared=false;
                for(var record:description.entries())if(StorageComponents.level(record)==1) {
                    var view=views.get(record.meta().id());
                    records.add(new Target(new Key(unit.id(),record.meta().id()),Optional.empty(),Optional.ofNullable(view),true,record.meta().provenance()));
                    shared|=view!=null&&baseOwners.getOrDefault(view.base(),Set.of()).size()>1;
                }
                var id=new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.FILE_ENTITY,entity.id());
                var file=new File(id,description,controls.get(0),records,shared);files.put(id,file);
                for(var record:records)recordOwners.put(record.declaration().node(),file);
            }
            for(var node:statementNodes) {
                var surface=node instanceof Ast.ModeledStatement m?m.fileIo():node instanceof Ast.PreservedStatement p?p.fileIo():Optional.<Ast.FileIoSurface>empty();
                if(surface.isEmpty())continue;var io=surface.orElseThrow();var operations=new ArrayList<Operation>();int ordinal=0;
                for(var operand:io.files()) {
                    var gaps=new LinkedHashSet<String>();var writes=new ArrayList<Write>();File file=null;
                    var binding=bindings.get(new Key(unit.id(),operand.reference().meta().id()));
                    if(binding!=null&&binding.status()==ResolutionContracts.ResolutionStatus.RESOLVED&&binding.candidates().size()==1) {
                        var selected=binding.candidates().get(0).entityId();
                        file=selected.domain()==ResolutionContracts.SemanticEntityDomain.FILE_ENTITY?files.get(selected):recordOwners.get(byEntity.get(selected));
                    }
                    if(io.profile()!=Ast.FileSyntaxProfile.N_LR)gaps.add("FILE_SYNTAX_OUTSIDE_N_LR");
                    if(file==null||file.description().kind()!=Ast.FileKind.FD)gaps.add("FILE_MEMORY_BINDING_NOT_PROVEN");
                    else {
                        if(file.binding().control().assignment().form()!=Ast.AssignmentForm.IBM_NAME)gaps.add("FILE_ASSIGN_OUTSIDE_N_LR");
                        if(file.records().isEmpty())gaps.add("FILE_RECORD_AREA_NOT_PROVEN");
                        if(io.command()==Ast.FileCommand.READ||io.command()==Ast.FileCommand.CLOSE
                                ||(io.command()==Ast.FileCommand.WRITE||io.command()==Ast.FileCommand.REWRITE)&&!file.sameRecordArea())
                            for(var record:file.records())writes.add(new Write(Role.RECORD,record));
                        for(var reference:file.binding().control().references()) {
                            Role role=switch(reference.role()) {
                                case FILE_STATUS->Role.FILE_STATUS;case ADDITIONAL_STATUS->Role.ADDITIONAL_STATUS;
                                case RELATIVE_KEY->io.command()==Ast.FileCommand.READ&&(file.binding().control().accessMode()!=Ast.FileAccessMode.RANDOM
                                    &&(file.binding().control().accessMode()!=Ast.FileAccessMode.DYNAMIC||io.options().contains(Ast.FileOption.NEXT)))?Role.RELATIVE_KEY:null;
                                default->null;
                            };
                            if(role!=null)add(writes,gaps,role,target(unit.id(),reference.reference(),bindings,byEntity,views,accesses));
                        }
                        if(io.command()==Ast.FileCommand.READ)for(var clause:file.description().recordClauses())
                            clause.dependingOn().ifPresent(ref->add(writes,gaps,Role.RECORD_LENGTH,target(unit.id(),ref,bindings,byEntity,views,accesses)));
                        for(var data:io.operands())if(data.role()==Ast.FileOperandRole.INTO)
                            add(writes,gaps,Role.INTO,target(unit.id(),data.value(),bindings,byEntity,views,accesses));
                        if(io.operands().stream().anyMatch(d->d.role()==Ast.FileOperandRole.FROM))
                            add(writes,gaps,Role.FROM_RECORD,target(unit.id(),operand.reference(),bindings,byEntity,views,accesses));
                    }
                    for(var write:writes)if(write.target().view().isEmpty())gaps.add("FILE_MEMORY_REGION_NOT_PROVEN");
                    operations.add(new Operation(ordinal++,Optional.ofNullable(file),writes,List.copyOf(gaps)));
                }
                var key=new Key(unit.id(),node.meta().id());result.put(key,new Statement(key,io,node.meta().provenance(),operations));
            }
        }
        return new FileIoMemory(result,targets);
    }
    private static void add(List<Write> writes,Set<String> gaps,Role role,Optional<Target> target) {
        if(target.isPresent())writes.add(new Write(role,target.orElseThrow()));else gaps.add("FILE_"+role+"_TARGET_NOT_PROVEN");
    }
    private static Optional<Target> target(ResolutionContracts.ProgramUnitId unit,Ast.Node node,
            Map<Key,ReferenceResolution.Entry> bindings,Map<ResolutionContracts.SemanticEntityId,Integer> byEntity,
            Map<Integer,View> views,Map<Key,StorageAccessSemantics.Access> accesses) {
        var key=new Key(unit,node.meta().id());var binding=bindings.get(key);
        if(binding==null||binding.status()!=ResolutionContracts.ResolutionStatus.RESOLVED||binding.candidates().size()!=1)return Optional.empty();
        var declaration=byEntity.get(binding.candidates().get(0).entityId());if(declaration==null)return Optional.empty();
        var access=accesses.get(key);var view=access==null?views.get(declaration):access.view();
        // Dynamic indexing/refmod may address any part of the owning base. The
        // exact address is evaluated by the later operation, never captured here.
        boolean broad=node instanceof Ast.DataReference r&&(!r.subscriptGroups().isEmpty()||r.referenceModification()!=null)&&access==null;
        return Optional.of(new Target(new Key(unit,declaration),Optional.of(key),Optional.ofNullable(view),broad,node.meta().provenance()));
    }
}
