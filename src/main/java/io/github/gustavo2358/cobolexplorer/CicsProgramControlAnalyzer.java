package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Dedicated parser for the preserved CICS command surface. No runtime value analysis. */
public final class CicsProgramControlAnalyzer {
    public enum Command { LINK, XCTL }
    public record Option(String name, Optional<String> operand, int start, int end) {
        public Option { Objects.requireNonNull(name); Objects.requireNonNull(operand); }
    }
    public record Fact(Command command, String raw, List<Option> options, Optional<String> literal,
                       Optional<String> host, int targetStart, int targetEnd, List<String> gaps) {
        public Fact { options=List.copyOf(options); gaps=List.copyOf(gaps); }
    }
    public record Key(ResolutionContracts.ProgramUnitId unit, int statement) { }
    public static final class Snapshot {
        private final CompilationUnitBuildResult owner;
        private final Map<Key,Fact> facts;
        private Snapshot(CompilationUnitBuildResult owner, Map<Key,Fact> facts) { this.owner=owner;this.facts=Map.copyOf(facts); }
        public boolean belongsTo(CompilationUnitBuildResult frontend) { return owner==frontend; }
        public Optional<Fact> fact(ResolutionContracts.ProgramUnitId unit,int statement) { return Optional.ofNullable(facts.get(new Key(unit,statement))); }
    }
    public Snapshot analyze(CompilationUnitBuildResult frontend) {
        var facts=new LinkedHashMap<Key,Fact>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var pending=new ArrayDeque<Ast.Node>();pending.push(unit.program());
            while(!pending.isEmpty()) {
                var node=pending.pop();
                if(node instanceof Ast.EmbeddedLanguageStatement embedded && embedded.language()==Ast.EmbeddedLanguage.CICS)
                    parse(embedded.rawText()).ifPresent(f->facts.put(new Key(unit.id(),node.meta().id()),f));
                var children=Ast.children(node);for(int i=children.size()-1;i>=0;i--)pending.push(children.get(i));
            }
        }
        return new Snapshot(frontend,facts);
    }
    public Optional<Fact> parse(String raw) {
        var cursor=new Cursor(raw);cursor.space();
        if(cursor.at("*>EXECCICS")){cursor.position+=10;cursor.space();}
        if(!cursor.word().equalsIgnoreCase("EXEC")||!cursor.word().equalsIgnoreCase("CICS"))return Optional.empty();
        String name=cursor.word().toUpperCase(Locale.ROOT);
        if(!name.equals("LINK")&&!name.equals("XCTL"))return Optional.empty();
        var command=Command.valueOf(name);var options=new ArrayList<Option>();var gaps=new LinkedHashSet<String>();
        boolean ended=false;
        while(cursor.position<raw.length()) {
            cursor.space();int start=cursor.position;String option=cursor.word().toUpperCase(Locale.ROOT);
            if(option.equals("END-EXEC")){ended=true;break;}
            if(option.isEmpty()){gaps.add("CICS_INVALID_OPTION_SYNTAX");break;}
            cursor.space();Optional<String> operand=Optional.empty();
            if(cursor.at("(")) {
                int begin=++cursor.position;int depth=1;char quote=0;
                while(cursor.position<raw.length()&&depth>0) {
                    char c=raw.charAt(cursor.position++);
                    if(quote!=0) { if(c==quote) { if(cursor.position<raw.length()&&raw.charAt(cursor.position)==quote)cursor.position++;else quote=0; } }
                    else if(c=='\''||c=='"')quote=c;
                    else if(c=='(')depth++;else if(c==')')depth--;
                }
                if(depth!=0||quote!=0){gaps.add("CICS_TRUNCATED_OPERAND");break;}
                operand=Optional.of(raw.substring(begin,cursor.position-1));
            }
            options.add(new Option(option,operand,start,cursor.position));
            if(!Set.of("PROGRAM","COMMAREA","LENGTH","CHANNEL","RESP","RESP2","NOHANDLE","INPUTMSG","INPUTMSGLEN","SYSID","SYNCONRETURN","TRANSID","DATALENGTH").contains(option))gaps.add("CICS_UNMODELED_OPTION");
            if(command==Command.XCTL&&Set.of("SYSID","SYNCONRETURN","TRANSID","DATALENGTH").contains(option))gaps.add("CICS_OPTION_INVALID_FOR_COMMAND");
            if((option.equals("NOHANDLE")||option.equals("SYNCONRETURN"))==operand.isPresent())gaps.add("CICS_OPTION_OPERAND_SHAPE");
        }
        cursor.space();if(cursor.at(".")){cursor.position++;cursor.space();}
        if(!ended||cursor.position!=raw.length())gaps.add("CICS_INCOMPLETE_PAYLOAD");
        var targets=options.stream().filter(o->o.name().equals("PROGRAM")).toList();
        Optional<String> literal=Optional.empty(),host=Optional.empty();int start=0,end=0;
        if(targets.size()!=1)gaps.add(targets.isEmpty()?"CICS_PROGRAM_MISSING":"CICS_PROGRAM_DUPLICATED");
        else if(targets.get(0).operand().isPresent()) {
            var option=targets.get(0);start=option.start();end=option.end();String value=option.operand().orElseThrow().strip();
            if(value.startsWith("'")||value.startsWith("\"")) {
                var decoded=literal(value);if(decoded.isPresent())literal=decoded;else gaps.add("CICS_INVALID_PROGRAM_LITERAL");
            } else if(!value.isEmpty())host=Optional.of(value);else gaps.add("CICS_EMPTY_PROGRAM");
        } else gaps.add("CICS_PROGRAM_OPERAND_MISSING");
        if(!ended||gaps.contains("CICS_TRUNCATED_OPERAND")){literal=Optional.empty();host=Optional.empty();}
        return Optional.of(new Fact(command,raw,options,literal,host,start,end,List.copyOf(gaps)));
    }
    private static Optional<String> literal(String value) {
        char quote=value.charAt(0);var decoded=new StringBuilder();
        for(int i=1;i<value.length();i++) {
            char c=value.charAt(i);
            if(c!=quote){decoded.append(c);continue;}
            if(i+1<value.length()&&value.charAt(i+1)==quote){decoded.append(c);i++;continue;}
            return i==value.length()-1?Optional.of(decoded.toString()):Optional.empty();
        }
        return Optional.empty();
    }
    private static final class Cursor {
        final String raw;int position;
        Cursor(String raw){this.raw=Objects.requireNonNull(raw);}
        void space(){while(position<raw.length()&&Character.isWhitespace(raw.charAt(position)))position++;}
        boolean at(String text){return raw.regionMatches(true,position,text,0,text.length());}
        String word(){space();int begin=position;while(position<raw.length()) {char c=raw.charAt(position);if(!Character.isLetterOrDigit(c)&&c!='-'&&c!='_')break;position++;}return raw.substring(begin,position);}
    }
}
