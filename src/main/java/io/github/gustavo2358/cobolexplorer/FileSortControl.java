package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.Key;

/** Canonical SORT phase/range facts. Lexical procedure order comes from typed AST;
 * nominal binding comes only from ReferenceResolution. No filename/value solver. */
public final class FileSortControl {
    public record Endpoint(ResolutionContracts.SemanticEntityId id,Ast.SourceProvenance referenceOrigin,Ast.SourceProvenance declarationOrigin) { }
    public record Link(int from,int to) { }
    public record Procedure(Ast.FileProcedurePhase phase,Optional<Endpoint> start,Optional<Endpoint> end,
            List<Integer> roots,Optional<Integer> entry,List<Integer> completions,List<Link> links,List<String> gaps) {
        public Procedure {roots=List.copyOf(roots);completions=List.copyOf(completions);links=List.copyOf(links);gaps=List.copyOf(gaps);}
    }
    public record Plan(Key statement,int work,List<Integer> inputs,List<Integer> outputs,List<Procedure> procedures,List<String> gaps) {
        public Plan {inputs=List.copyOf(inputs);outputs=List.copyOf(outputs);procedures=List.copyOf(procedures);gaps=List.copyOf(gaps);}
    }
    private record Span(int first,int last,Ast.Node declaration) { }
    private final List<Plan> plans;
    private FileSortControl(List<Plan> plans){this.plans=List.copyOf(plans);}
    public List<Plan> plans(){return plans;}

    static FileSortControl analyze(CompilationUnitBuildResult frontend,ReferenceResolution resolution,StorageLayoutSemantics storage,FileIoMemory memory) {
        var refs=new HashMap<Key,ReferenceResolution.Entry>();resolution.entries().forEach(e->refs.put(new Key(e.occurrence().programUnitId(),e.occurrence().referenceAstNodeId()),e));
        var result=new ArrayList<Plan>();
        var byUnit=memory.statements().stream().collect(java.util.stream.Collectors.groupingBy(s->s.statement().unit()));
        for(var unit:frontend.compilationUnit().programUnits()) {
            var division=unit.program().divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).findFirst().orElse(null);if(division==null)continue;
            var paragraphs=new ArrayList<Ast.Paragraph>();var spans=new HashMap<Integer,Span>();
            for(var node:division.children()) {
                if(node instanceof Ast.Paragraph p){int i=paragraphs.size();paragraphs.add(p);spans.put(p.meta().id(),new Span(i,i,p));}
                else if(node instanceof Ast.Section s&&!s.children().stream().anyMatch(Ast.UseClause.class::isInstance)) {
                    int first=paragraphs.size();
                    for(var child:s.children())if(child instanceof Ast.Paragraph p){int i=paragraphs.size();paragraphs.add(p);spans.put(p.meta().id(),new Span(i,i,p));}
                    spans.put(s.meta().id(),new Span(first,paragraphs.size()-1,s));
                }
            }
            var byIdentity=new HashMap<ResolutionContracts.SemanticEntityId,Span>();
            for(var symbol:storage.symbolTables().forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols())
                if(symbol.kind()==SymbolTable.SymbolKind.PARAGRAPH||symbol.kind()==SymbolTable.SymbolKind.PROCEDURE_SECTION) {
                    var span=spans.get(symbol.declarationAstNodeId());if(span!=null)byIdentity.put(new ResolutionContracts.SemanticEntityId(unit.id(),ResolutionContracts.SemanticEntityDomain.PROCEDURE_SYMBOL,symbol.id()),span);
                }
            var statements=byUnit.getOrDefault(unit.id(),List.of()).stream().sorted(Comparator.comparingInt(s->s.statement().node())).toList();
            for(var statement:statements) {
                var surface=statement.surface();if(surface.command()!=Ast.FileCommand.SORT&&surface.command()!=Ast.FileCommand.MERGE)continue;
                var inputs=new ArrayList<Integer>();var outputs=new ArrayList<Integer>();int work=-1;
                var gaps=new LinkedHashSet<>(surface.gapCodes());var identities=new HashSet<ResolutionContracts.SemanticEntityId>();
                for(var op:statement.operations()) {
                    switch(surface.files().get(op.ordinal()).role()){case INPUT->inputs.add(op.ordinal());case OUTPUT->outputs.add(op.ordinal());case WORK->work=op.ordinal();default->gaps.add("FILE_SORT_ROLE_NOT_PROVEN");}
                    if(op.file().isEmpty())gaps.add("FILE_SORT_BINDING_NOT_PROVEN");
                    else {
                        var file=op.file().orElseThrow();
                        if(file.description().kind()!=FileIoMemory.expectedKind(surface,op.ordinal()))gaps.add("FILE_KIND_NOT_PROVEN");
                        if(!identities.add(file.entity())&&surface.command()==Ast.FileCommand.MERGE)gaps.add("FILE_MERGE_DUPLICATE_PARTICIPANT");
                        if(surface.command()==Ast.FileCommand.MERGE&&surface.files().get(op.ordinal()).role()!=Ast.FileRole.WORK
                                &&file.binding().control().accessMode()==Ast.FileAccessMode.RANDOM)gaps.add("FILE_MERGE_ACCESS_OUTSIDE_N_LR");
                    }
                }
                if(work<0)gaps.add("FILE_SORT_WORK_NOT_PROVEN");
                var procedures=new ArrayList<Procedure>();
                for(var p:surface.procedures()) {
                    var procedure=procedure(p,unit.id(),refs,byIdentity,paragraphs,division);procedures.add(procedure);gaps.addAll(procedure.gaps());
                }
                result.add(new Plan(statement.statement(),work,inputs,outputs,procedures,List.copyOf(gaps)));
            }
        }
        return new FileSortControl(result);
    }
    private static Procedure procedure(Ast.FileProcedureSurface surface,ResolutionContracts.ProgramUnitId unit,
            Map<Key,ReferenceResolution.Entry> refs,Map<ResolutionContracts.SemanticEntityId,Span> spans,List<Ast.Paragraph> paragraphs,Ast.Division division) {
        var start=endpoint(surface.start(),unit,refs,spans);var end=endpoint(surface.end().orElse(surface.start()),unit,refs,spans);
        var gaps=new LinkedHashSet<String>();var roots=new ArrayList<Integer>();var completions=new ArrayList<Integer>();var links=new ArrayList<Link>();
        if(start.isEmpty()||end.isEmpty())gaps.add("FILE_PROCEDURE_ENDPOINT_NOT_PROVEN");
        else {
            var first=spans.get(start.orElseThrow().id());var last=spans.get(end.orElseThrow().id());
            if(first.first()>last.last()||first.declaration().getClass()!=last.declaration().getClass())gaps.add("FILE_PROCEDURE_RANGE_NOT_PROVEN");
            else {
                var regions=new ArrayList<List<Ast.Statement>>();
                for(int i=first.first();i<=last.last();i++) {
                    var direct=paragraphs.get(i).sentences().stream().flatMap(s->s.statements().stream()).toList();
                    if(!direct.isEmpty())regions.add(direct);
                }
                for(int i=0;i<regions.size();i++) {
                    var region=regions.get(i);region.forEach(s->roots.add(s.meta().id()));
                    var members=new LinkedHashSet<Integer>();var pending=new ArrayDeque<Ast.Node>(region);
                    while(!pending.isEmpty()){var n=pending.removeFirst();if(n instanceof Ast.Statement)members.add(n.meta().id());pending.addAll(Ast.children(n));}
                    for(var member:members)if(division.normalCompletionStatements().contains(member)
                            &&!Optional.ofNullable(division.normalContinuations().get(member)).filter(members::contains).isPresent()) {
                        if(i+1<regions.size())links.add(new Link(member,regions.get(i+1).get(0).meta().id()));else completions.add(member);
                    }
                }
            }
        }
        return new Procedure(surface.phase(),start,end,roots,roots.isEmpty()?Optional.empty():Optional.of(roots.get(0)),completions,links,List.copyOf(gaps));
    }
    private static Optional<Endpoint> endpoint(Ast.ProcedureReference reference,ResolutionContracts.ProgramUnitId unit,
            Map<Key,ReferenceResolution.Entry> refs,Map<ResolutionContracts.SemanticEntityId,Span> spans) {
        var binding=refs.get(new Key(unit,reference.meta().id()));
        if(binding==null||binding.status()!=ResolutionContracts.ResolutionStatus.RESOLVED||binding.candidates().size()!=1)return Optional.empty();
        var id=binding.candidates().get(0).entityId();var span=spans.get(id);if(span==null)return Optional.empty();
        return Optional.of(new Endpoint(id,reference.meta().provenance(),span.declaration().meta().provenance()));
    }
}
