package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.Key;

/** IBM native I/O dispatch from canonical AST and binding. Routes are conditional
 * on an event, never a claim that normal AIR return means successful COBOL I/O. */
public final class FileIoControl {
    public enum Event { SUCCESS, END, INVALID_KEY, OTHER_ERROR, END_OF_PAGE }
    public enum DestinationKind { CONTINUE, HANDLER, USE }
    public record Declarative(Key section,Ast.UseKind kind,boolean global,Ast.FileOpenMode mode,
            List<ResolutionContracts.SemanticEntityId> files,List<Integer> roots,Optional<Integer> entry,
            List<Integer> completions,List<String> gaps,Ast.SourceProvenance origin) {
        public Declarative {files=List.copyOf(files);roots=List.copyOf(roots);completions=List.copyOf(completions);gaps=List.copyOf(gaps);}
    }
    public record Destination(DestinationKind kind,Optional<Ast.FileHandlerKind> handler,Optional<Key> declarative) { }
    public record Route(Event event,FileIoEffects.Outcome effects,List<Destination> destinations,boolean criticalExit) {
        public Route {destinations=List.copyOf(destinations);}
    }
    public record Operation(Key statement,int ordinal,Ast.FileCommand command,Optional<Integer> continuation,
            List<Route> routes,List<String> gaps) {
        public Operation {routes=List.copyOf(routes);gaps=List.copyOf(gaps);}
    }
    private final List<Declarative> declaratives;
    private final List<Operation> operations;
    private FileIoControl(List<Declarative> declarations,List<Operation> operations) {this.declaratives=List.copyOf(declarations);this.operations=List.copyOf(operations);}
    public List<Declarative> declaratives(){return declaratives;}
    public List<Operation> operations(){return operations;}
    public static FileIoControl analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,FileIoMemory memory) {
        var bindings=new HashMap<Key,ReferenceResolution.Entry>();
        resolution.entries().forEach(e->bindings.put(new Key(e.occurrence().programUnitId(),e.occurrence().referenceAstNodeId()),e));
        var declarations=new ArrayList<Declarative>();var byUnit=new HashMap<ResolutionContracts.ProgramUnitId,List<Declarative>>();
        var continuations=new HashMap<Key,Integer>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var procedure=unit.program().divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).findFirst().orElse(null);
            if(procedure==null)continue;
            procedure.ordinaryContinuations().forEach((from,to)->continuations.put(new Key(unit.id(),from),to));
            for(var child:procedure.children())if(child instanceof Ast.Section section) {
                var clause=section.children().stream().filter(Ast.UseClause.class::isInstance).map(Ast.UseClause.class::cast).findFirst().orElse(null);
                if(clause==null)continue;
                var files=new LinkedHashSet<ResolutionContracts.SemanticEntityId>();var gaps=new LinkedHashSet<String>();
                if(clause.kind()==Ast.UseKind.DEBUGGING)gaps.add("USE_DEBUGGING_CONTROL_NOT_PROVEN");
                for(var reference:clause.files()) {
                    var binding=bindings.get(new Key(unit.id(),reference.meta().id()));
                    if(binding==null||binding.status()!=ResolutionContracts.ResolutionStatus.RESOLVED)gaps.add("FILE_USE_BINDING_NOT_PROVEN");
                    if(binding!=null)for(var candidate:binding.candidates())if(candidate.entityId().domain()==ResolutionContracts.SemanticEntityDomain.FILE_ENTITY)files.add(candidate.entityId());
                }
                var roots=new ArrayList<Integer>();var members=new ArrayList<Ast.Statement>();
                for(var node:section.children())if(node instanceof Ast.Paragraph paragraph)for(var sentence:paragraph.sentences())
                    for(var statement:sentence.statements()){roots.add(statement.meta().id());collectStatements(statement,members);}
                var completions=new ArrayList<Integer>();
                for(var statement:members)if(!procedure.ordinaryContinuations().containsKey(statement.meta().id())&&procedure.normalCompletionStatements().contains(statement.meta().id()))completions.add(statement.meta().id());
                var declaration=new Declarative(new Key(unit.id(),section.meta().id()),clause.kind(),clause.global(),clause.mode(),List.copyOf(files),roots,
                    roots.isEmpty()?Optional.empty():Optional.of(roots.get(0)),completions,List.copyOf(gaps),clause.meta().provenance());
                declarations.add(declaration);byUnit.computeIfAbsent(unit.id(),k->new ArrayList<>()).add(declaration);
            }
        }
        var result=new ArrayList<Operation>();
        for(var statement:memory.statements())for(var op:statement.operations()) {
            var surface=statement.surface();var gaps=new LinkedHashSet<String>();
            var available=byUnit.getOrDefault(statement.statement().unit(),List.of());var selected=new ArrayList<Declarative>();boolean modeOpen=false;
            var file=op.file().orElse(null);
            if(file==null)gaps.add("FILE_CONTROL_BINDING_NOT_PROVEN");
            for(var d:available)if(d.kind()==Ast.UseKind.AFTER_EXCEPTION&&file!=null&&d.files().contains(file.entity()))selected.add(d);
            if(selected.size()>1)gaps.add("FILE_USE_SELECTION_NOT_PROVEN");
            if(selected.isEmpty()) {
                var mode=surface.files().get(op.ordinal()).mode();
                for(var d:available)if(d.kind()==Ast.UseKind.AFTER_EXCEPTION&&d.mode()!=Ast.FileOpenMode.UNSPECIFIED
                        &&(surface.command()!=Ast.FileCommand.OPEN||d.mode()==mode))selected.add(d);
                modeOpen=!selected.isEmpty()&&surface.command()!=Ast.FileCommand.OPEN;
                if(modeOpen)gaps.add("FILE_CURRENT_OPEN_MODE_NOT_PROVEN");
                if(surface.command()==Ast.FileCommand.OPEN&&selected.size()>1)gaps.add("FILE_USE_SELECTION_NOT_PROVEN");
            }
            for(var d:available)if(d.kind()==Ast.UseKind.AFTER_EXCEPTION
                    &&(file==null||d.gaps().contains("FILE_USE_BINDING_NOT_PROVEN"))) {
                if(!selected.contains(d))selected.add(d);
                modeOpen=true;
                gaps.add("FILE_USE_SELECTION_NOT_PROVEN");
            }
            if(available.stream().anyMatch(d->d.kind()==Ast.UseKind.AFTER_EXCEPTION&&!d.gaps().isEmpty()))gaps.add("FILE_USE_SELECTION_NOT_PROVEN");
            var handlers=new EnumMap<Ast.FileHandlerKind,Ast.FileHandler>(Ast.FileHandlerKind.class);
            for(var h:surface.handlers())if(handlers.put(h.kind(),h)!=null)gaps.add("FILE_HANDLER_SELECTION_NOT_PROVEN");
            var events=new ArrayList<Event>(List.of(Event.SUCCESS));
            if(surface.command()==Ast.FileCommand.READ) {
                var access=file==null?Ast.FileAccessMode.UNSPECIFIED:file.binding().control().accessMode();
                if(file==null||access==Ast.FileAccessMode.UNSUPPORTED) {
                    events.add(Event.END);events.add(Event.INVALID_KEY);gaps.add("FILE_ACCESS_MODE_NOT_PROVEN");
                } else events.add(access==Ast.FileAccessMode.RANDOM||access==Ast.FileAccessMode.DYNAMIC&&!surface.options().contains(Ast.FileOption.NEXT)?Event.INVALID_KEY:Event.END);
            } else if(surface.command()==Ast.FileCommand.WRITE||surface.command()==Ast.FileCommand.REWRITE||surface.command()==Ast.FileCommand.DELETE_RECORD||surface.command()==Ast.FileCommand.START)events.add(Event.INVALID_KEY);
            if(surface.command()==Ast.FileCommand.WRITE&&(handlers.containsKey(Ast.FileHandlerKind.AT_END_OF_PAGE)||handlers.containsKey(Ast.FileHandlerKind.NOT_AT_END_OF_PAGE)))events.add(Event.END_OF_PAGE);
            events.add(Event.OTHER_ERROR);var routes=new ArrayList<Route>();
            for(var event:events) {
                Ast.FileHandlerKind handler=switch(event) {
                    case END->Ast.FileHandlerKind.AT_END;case INVALID_KEY->Ast.FileHandlerKind.INVALID_KEY;case END_OF_PAGE->Ast.FileHandlerKind.AT_END_OF_PAGE;
                    case SUCCESS->surface.command()==Ast.FileCommand.READ&&events.contains(Event.END)?Ast.FileHandlerKind.NOT_AT_END:
                        events.contains(Event.END_OF_PAGE)?Ast.FileHandlerKind.NOT_AT_END_OF_PAGE:Ast.FileHandlerKind.NOT_INVALID_KEY;
                    case OTHER_ERROR->null;
                };
                var destinations=new ArrayList<Destination>();
                if(handler!=null&&handlers.containsKey(handler))destinations.add(handler(handler));
                else if(event==Event.END||event==Event.INVALID_KEY||event==Event.OTHER_ERROR) {
                    for(var d:selected)destinations.add(new Destination(DestinationKind.USE,Optional.empty(),Optional.of(d.section())));
                    if(selected.isEmpty()||modeOpen) {
                        // READ p432: without applicable USE, NOT AT END also receives non-EOF errors.
                        if(event==Event.OTHER_ERROR&&surface.command()==Ast.FileCommand.READ&&events.contains(Event.END)&&handlers.containsKey(Ast.FileHandlerKind.NOT_AT_END))destinations.add(handler(Ast.FileHandlerKind.NOT_AT_END));
                        else destinations.add(continuing());
                    }
                } else destinations.add(continuing());
                var effect=switch(event){case SUCCESS,END_OF_PAGE->FileIoEffects.Outcome.SUCCESS;case END->FileIoEffects.Outcome.END;case INVALID_KEY->FileIoEffects.Outcome.INVALID_KEY;case OTHER_ERROR->FileIoEffects.Outcome.OTHER_ERROR;};
                routes.add(new Route(event,effect,destinations,event==Event.OTHER_ERROR));
            }
            if(surface.profile()!=Ast.FileSyntaxProfile.N_LR)gaps.add("FILE_SYNTAX_OUTSIDE_N_LR");
            result.add(new Operation(statement.statement(),op.ordinal(),surface.command(),Optional.ofNullable(continuations.get(statement.statement())),routes,List.copyOf(gaps)));
        }
        result.sort(Comparator.comparing((Operation o)->o.statement().unit().structuralPath().toString()).thenComparingInt(o->o.statement().node()).thenComparingInt(Operation::ordinal));
        return new FileIoControl(declarations,result);
    }
    private static Destination handler(Ast.FileHandlerKind h){return new Destination(DestinationKind.HANDLER,Optional.of(h),Optional.empty());}
    private static Destination continuing(){return new Destination(DestinationKind.CONTINUE,Optional.empty(),Optional.empty());}
    private static void collectStatements(Ast.Node root,List<Ast.Statement> result){var pending=new ArrayDeque<Ast.Node>();pending.push(root);while(!pending.isEmpty()){var n=pending.pop();if(n instanceof Ast.Statement s)result.add(s);for(var child:Ast.children(n))pending.push(child);}}
}
