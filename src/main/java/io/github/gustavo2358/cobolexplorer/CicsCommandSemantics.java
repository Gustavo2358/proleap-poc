package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import io.github.gustavo2358.cobolexplorer.antlr.*;
import org.antlr.v4.runtime.*;

/** Source command/option qualification. Does not compute control, runtime values or handlers. */
public final class CicsCommandSemantics {
    public enum Kind { SYNCPOINT, RECEIVE_MAP, SEND_MAP }
    public enum SyntaxStatus { SUPPORTED, UNAVAILABLE }
    public record Fact(Kind command,SyntaxStatus syntaxStatus,String raw,List<CicsCommandSyntax.Option> options,List<String> gaps) {
        public Fact { Objects.requireNonNull(command);Objects.requireNonNull(syntaxStatus);options=List.copyOf(options);gaps=List.copyOf(gaps); }
        public boolean supported(){return syntaxStatus==SyntaxStatus.SUPPORTED;}
    }
    private final CompilationUnitBuildResult owner;
    private final Map<CicsProgramControlAnalyzer.Key,Fact> facts;
    private CicsCommandSemantics(CompilationUnitBuildResult owner,Map<CicsProgramControlAnalyzer.Key,Fact> facts) {
        this.owner=owner;this.facts=Map.copyOf(facts);
    }
    public boolean belongsTo(CompilationUnitBuildResult frontend){return frontend==owner;}
    public Optional<Fact> fact(ResolutionContracts.ProgramUnitId unit,int statement){return Optional.ofNullable(facts.get(new CicsProgramControlAnalyzer.Key(unit,statement)));}
    public static CicsCommandSemantics analyze(CompilationUnitBuildResult frontend) {
        var facts=new LinkedHashMap<CicsProgramControlAnalyzer.Key,Fact>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var todo=new ArrayDeque<Ast.Node>();todo.push(unit.program());
            while(!todo.isEmpty()) {
                var node=todo.pop();if(node instanceof Ast.Program&&node!=unit.program())continue;
                if(node instanceof Ast.EmbeddedLanguageStatement s&&s.language()==Ast.EmbeddedLanguage.CICS)
                    parse(s.rawText()).ifPresent(f->facts.put(new CicsProgramControlAnalyzer.Key(unit.id(),s.meta().id()),f));
                var children=Ast.children(node);for(int i=children.size()-1;i>=0;i--)todo.push(children.get(i));
            }
        }
        return new CicsCommandSemantics(frontend,facts);
    }
    static Optional<Fact> parse(String raw) {
        var syntax=CicsCommandSyntax.parse(raw);if(syntax.isEmpty())return Optional.empty();var s=syntax.get();
        Kind kind;
        if(s.name().equals("SYNCPOINT"))kind=Kind.SYNCPOINT;
        else if(Set.of("SEND","RECEIVE").contains(s.name())&&s.options().stream().anyMatch(o->o.name().equals("MAP")))
            kind=s.name().equals("SEND")?Kind.SEND_MAP:Kind.RECEIVE_MAP;
        else return Optional.empty();
        var gaps=new LinkedHashSet<>(s.gaps());boolean supported=s.gaps().isEmpty();var names=new HashSet<String>();
        var allowed=new HashSet<>(Set.of("RESP","RESP2","NOHANDLE"));
        if(kind!=Kind.SYNCPOINT)allowed.addAll(Set.of("MAP","MAPSET",kind==Kind.SEND_MAP?"FROM":"INTO"));
        if(kind==Kind.SEND_MAP)allowed.addAll(Set.of("CURSOR","ERASE","FREEKB"));
        for(var o:s.options()) {
            if(!allowed.contains(o.name())){supported=false;gaps.add("CICS_COMMAND_UNMODELED_OPTION");}
            if(!names.add(o.name())){supported=false;gaps.add("CICS_COMMAND_DUPLICATE_OPTION");}
            boolean flag=Set.of("NOHANDLE","CURSOR","ERASE","FREEKB").contains(o.name());
            if(flag?o.operand().isPresent():o.operand().filter(v->!v.isBlank()).isEmpty()){supported=false;gaps.add("CICS_COMMAND_OPERAND_SHAPE");}
            if(!flag&&allowed.contains(o.name())&&o.operand().isPresent()) {
                String v=o.operand().orElseThrow().strip();boolean name=Set.of("MAP","MAPSET").contains(o.name());
                if(!(name&&CicsCommandSyntax.literal(v).filter(x->!x.isEmpty()).isPresent())&&!dataSyntax(v)){supported=false;gaps.add("CICS_COMMAND_OPERAND_SHAPE");}
            }
        }
        if(kind!=Kind.SYNCPOINT) {
            var map=s.options().stream().filter(o->o.name().equals("MAP")).findFirst().orElseThrow();
            if(map.operand().flatMap(v->CicsCommandSyntax.literal(v.strip())).isEmpty()&&!names.contains(kind==Kind.SEND_MAP?"FROM":"INTO"))
                {supported=false;gaps.add("CICS_COMMAND_IMPLICIT_AREA_UNAVAILABLE");}
        }
        return Optional.of(new Fact(kind,supported?SyntaxStatus.SUPPORTED:SyntaxStatus.UNAVAILABLE,raw,s.options(),List.copyOf(gaps)));
    }
    private static boolean dataSyntax(String text) {
        var failed=new boolean[1];var listener=new BaseErrorListener(){@Override public void syntaxError(Recognizer<?,?> r,Object s,int l,int c,String m,RecognitionException e){failed[0]=true;}};
        var lexer=new CobolLexer(CharStreams.fromString(text));lexer.removeErrorListeners();lexer.addErrorListener(listener);
        var parser=new CobolParser(new CommonTokenStream(lexer));parser.removeErrorListeners();parser.setErrorHandler(new BailErrorStrategy());
        try {var ref=parser.identifier();return !failed[0]&&parser.getCurrentToken().getType()==Token.EOF&&(ref.qualifiedDataName()!=null||ref.tableCall()!=null);}
        catch(org.antlr.v4.runtime.misc.ParseCancellationException e){return false;}
    }
}
