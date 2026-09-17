package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** CICS TS for z/OS 5.6 File Control/API and selected FILE SPI. No external lookup. */
public final class CicsFileControlAnalyzer {
    public enum Command { READ, WRITE, REWRITE, DELETE, STARTBR, READNEXT, READPREV, RESETBR, ENDBR, UNLOCK, INQUIRE, SET }
    public enum TargetMode { INPUT, OUTPUT, BROWSE_START, BROWSE_END }
    public enum Role { READ, WRITE, READ_WRITE, NONE, UNKNOWN }
    public record Option(CicsCommandSyntax.Option syntax,String canonicalName,Role role,
                         Optional<String> literal,Optional<java.math.BigInteger> integer) { }
    public record Fact(Command command,String raw,TargetMode targetMode,List<Option> options,
                       Optional<String> literal,Optional<String> host,List<String> gaps) {
        public Fact {options=List.copyOf(options);gaps=List.copyOf(gaps);}
        public boolean boundedLocal() {return gaps.isEmpty()&&options.stream().anyMatch(o->Set.of("RESP","NOHANDLE").contains(o.canonicalName()));}
    }
    private static final Set<String> READS=words("READ READNEXT READPREV");
    private static final Set<String> FLAGS=words("NOHANDLE UNCOMMITTED CONSISTENT REPEATABLE UPDATE GENERIC EQUAL GTEQ DEBKEY DEBREC RBA RRN XRBA NOSUSPEND MASSINSERT START NEXT END ADDABLE NOTADDABLE BROWSABLE NOTBROWSABLE DELETABLE NOTDELETABLE OLD SHARE EMPTY EMPTYREQ NOEMPTYREQ DISABLED ENABLED CLOSED OPEN EXCTL NOEXCTL LOAD NOLOAD NOTREADABLE READABLE RLS NOTRLS CFTABLE CICSTABLE NOTTABLE USERTABLE NOTUPDATABLE UPDATABLE CONTENTION LOCKING WAIT FORCE NOWAIT");
    private static final Map<Command,Set<String>> CATALOG=catalog();
    private static Set<String> words(String text){return Set.of(text.split(" "));}
    private static Map<Command,Set<String>> catalog() {
        var m=new EnumMap<Command,Set<String>>(Command.class);
        m.put(Command.READ,words("INTO SET RIDFLD KEYLENGTH SYSID LENGTH TOKEN UNCOMMITTED CONSISTENT REPEATABLE UPDATE GENERIC EQUAL GTEQ DEBKEY DEBREC RBA RRN XRBA NOSUSPEND"));
        var browse=words("INTO SET RIDFLD KEYLENGTH SYSID LENGTH REQID TOKEN UNCOMMITTED CONSISTENT REPEATABLE UPDATE RBA RRN XRBA NOSUSPEND");
        m.put(Command.READNEXT,browse);m.put(Command.READPREV,browse);
        m.put(Command.WRITE,words("FROM RIDFLD KEYLENGTH SYSID LENGTH MASSINSERT RBA RRN XRBA NOSUSPEND"));
        m.put(Command.REWRITE,words("FROM LENGTH SYSID TOKEN NOSUSPEND"));
        m.put(Command.DELETE,words("RIDFLD KEYLENGTH SYSID TOKEN GENERIC NOSUSPEND NUMREC"));
        m.put(Command.STARTBR,words("RIDFLD KEYLENGTH SYSID REQID GENERIC DEBKEY DEBREC GTEQ EQUAL RBA RRN XRBA"));
        m.put(Command.RESETBR,words("RIDFLD KEYLENGTH SYSID REQID GENERIC GTEQ EQUAL RBA RRN XRBA"));
        m.put(Command.ENDBR,words("SYSID REQID"));m.put(Command.UNLOCK,words("SYSID TOKEN"));
        m.put(Command.INQUIRE,words("START NEXT END ACCESSMETHOD ADD BASEDSNAME BLOCKFORMAT BLOCKKEYLEN BLOCKSIZE BROWSE CFDTPOOL CHANGEAGENT CHANGEAGREL CHANGETIME CHANGEUSRID DEFINESOURCE DEFINETIME DELETE DISPOSITION DSNAME EMPTYSTATUS ENABLESTATUS EXCLUSIVE FWDRECSTATUS INSTALLAGENT INSTALLTIME INSTALLUSRID JOURNALNUM KEYLENGTH KEYPOSITION LOADTYPE LSRPOOLNUM MAXNUMRECS OBJECT OPENSTATUS RBATYPE READ READINTEG RECORDFORMAT RECORDSIZE RECOVSTATUS RELTYPE REMOTENAME REMOTESYSTEM REMOTETABLE RLSACCESS STRINGS TABLE TABLENAME TYPE UPDATE UPDATEMODEL"));
        m.put(Command.SET,words("DATASET OBJECTNAME ADD ADDABLE NOTADDABLE BROWSE BROWSABLE NOTBROWSABLE BUSY WAIT FORCE NOWAIT CFDTPOOL DELETE DELETABLE NOTDELETABLE DISPOSITION OLD SHARE DSNAME EMPTYSTATUS EMPTY EMPTYREQ NOEMPTYREQ ENABLESTATUS DISABLED ENABLED OPENSTATUS CLOSED OPEN EXCLUSIVE EXCTL NOEXCTL KEYLENGTH LOADTYPE LOAD NOLOAD LSRPOOLNUM MAXNUMRECS READ NOTREADABLE READABLE RECORDSIZE READINTEG UNCOMMITTED CONSISTENT REPEATABLE RLSACCESS RLS NOTRLS STRINGS TABLE CFTABLE CICSTABLE NOTTABLE USERTABLE TABLENAME UPDATE NOTUPDATABLE UPDATABLE UPDATEMODEL CONTENTION LOCKING"));
        return Collections.unmodifiableMap(m);
    }
    public Optional<Fact> parse(String raw) {
        var parsed=CicsCommandSyntax.parse(raw);if(parsed.isEmpty())return Optional.empty();var syntax=parsed.get();
        Command command;try{command=Command.valueOf(syntax.name());}catch(IllegalArgumentException ex){return Optional.empty();}
        // Other resource variants of WRITE/DELETE/INQUIRE/SET are independent capabilities.
        if(syntax.options().stream().noneMatch(o->o.name().equals("FILE")||command==Command.SET&&o.name().equals("DATASET")))return Optional.empty();
        var gaps=new LinkedHashSet<>(syntax.gaps());var names=new HashSet<String>();
        for(var o:syntax.options())names.add(canonical(command,o.name()));
        var mode=command!=Command.INQUIRE?TargetMode.INPUT:names.contains("NEXT")?TargetMode.OUTPUT:
            names.contains("START")?TargetMode.BROWSE_START:names.contains("END")?TargetMode.BROWSE_END:TargetMode.INPUT;
        var options=new ArrayList<Option>();var seen=new HashSet<String>();
        for(var o:syntax.options()) {
            String name=canonical(command,o.name());boolean known=Set.of("FILE","RESP","RESP2","NOHANDLE").contains(name)||CATALOG.get(command).contains(o.name());
            if(!known)gaps.add("CICS_FILE_UNMODELED_OPTION");
            if(!seen.add(name))gaps.add("CICS_FILE_DUPLICATED_OPTION");
            boolean bareFile=name.equals("FILE")&&(mode==TargetMode.BROWSE_START||mode==TargetMode.BROWSE_END);
            // SET/INQUIRE UPDATE is a CVDA operand; API UPDATE is a flag.
            boolean flag=FLAGS.contains(name)&&!(command==Command.INQUIRE&&name.equals("UPDATE"))&&!(command==Command.SET&&name.equals("UPDATE"));
            if((bareFile||flag)==o.operand().isPresent())gaps.add("CICS_FILE_OPTION_OPERAND_SHAPE");
            var role=!known?Role.UNKNOWN:bareFile||flag?Role.NONE:role(command,mode,name,names);
            var text=o.operand().flatMap(v->CicsCommandSyntax.literal(v.strip()));
            Optional<java.math.BigInteger> integer=Optional.empty();
            if(o.operand().isPresent()&&text.isEmpty())try{integer=Optional.of(new java.math.BigInteger(o.operand().get().strip()));}catch(NumberFormatException ignored) { }
            if((role==Role.WRITE||role==Role.READ_WRITE)&&(text.isPresent()||integer.isPresent()))gaps.add("CICS_FILE_OUTPUT_REQUIRES_HOST");
            options.add(new Option(o,name,role,text,integer));
        }
        var targets=options.stream().filter(o->o.canonicalName().equals("FILE")).toList();
        Optional<String> literal=Optional.empty(),host=Optional.empty();
        if(targets.size()!=1)gaps.add("CICS_FILE_TARGET_CARDINALITY");
        else if(mode==TargetMode.INPUT&&targets.get(0).syntax().operand().isPresent()) {
            var value=targets.get(0).syntax().operand().orElseThrow().strip();
            if(value.startsWith("'")||value.startsWith("\"")){literal=CicsCommandSyntax.literal(value);if(literal.isEmpty())gaps.add("CICS_FILE_INVALID_LITERAL");}
            else if(!value.isEmpty())host=Optional.of(value);else gaps.add("CICS_FILE_EMPTY_TARGET");
        } else if(mode==TargetMode.OUTPUT&&targets.get(0).syntax().operand().map(String::strip).filter(v->v.isEmpty()||v.startsWith("'")||v.startsWith("\"")).isPresent())gaps.add("CICS_FILE_OUTPUT_REQUIRES_HOST");
        if(READS.contains(command.name())) {
            if(names.contains("INTO")==names.contains("SET"))gaps.add("CICS_FILE_BUFFER_CHOICE");
            if(!names.contains("RIDFLD"))gaps.add("CICS_FILE_RIDFLD_REQUIRED");
        }
        if(Set.of(Command.WRITE,Command.REWRITE).contains(command)&&!names.contains("FROM"))gaps.add("CICS_FILE_FROM_REQUIRED");
        if(Set.of(Command.WRITE,Command.STARTBR,Command.RESETBR).contains(command)&&!names.contains("RIDFLD"))gaps.add("CICS_FILE_RIDFLD_REQUIRED");
        for(var exclusive:List.of(words("START NEXT END"),words("RBA RRN XRBA"),words("GTEQ EQUAL"),words("UNCOMMITTED CONSISTENT REPEATABLE")))
            if(exclusive.stream().filter(names::contains).count()>1)gaps.add("CICS_FILE_CONFLICTING_OPTIONS");
        if(!syntax.ended()||gaps.contains("CICS_TRUNCATED_OPERAND")){literal=Optional.empty();host=Optional.empty();}
        return Optional.of(new Fact(command,raw,mode,options,literal,host,List.copyOf(gaps)));
    }
    public record Key(ResolutionContracts.ProgramUnitId unit,int statement) { }
    public static final class Contribution {
        private final CompilationUnitBuildResult owner;private final Map<Key,Fact> facts;
        private Contribution(CompilationUnitBuildResult owner,Map<Key,Fact> facts){this.owner=owner;this.facts=Map.copyOf(facts);}
        public boolean belongsTo(CompilationUnitBuildResult frontend){return owner==frontend;}
        public Optional<Fact> fact(ResolutionContracts.ProgramUnitId unit,int statement){return Optional.ofNullable(facts.get(new Key(unit,statement)));}
    }
    public Contribution analyze(CompilationUnitBuildResult frontend) {
        var facts=new LinkedHashMap<Key,Fact>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var pending=new ArrayDeque<Ast.Node>();pending.push(unit.program());
            while(!pending.isEmpty()) {
                var node=pending.pop();if(node instanceof Ast.Program&&node!=unit.program())continue;
                if(node instanceof Ast.EmbeddedLanguageStatement embedded&&embedded.language()==Ast.EmbeddedLanguage.CICS)
                    parse(embedded.rawText()).ifPresent(f->facts.put(new Key(unit.id(),node.meta().id()),f));
                var children=Ast.children(node);for(int i=children.size()-1;i>=0;i--)pending.push(children.get(i));
            }
        }
        return new Contribution(frontend,facts);
    }
    private static String canonical(Command command,String option) {
        if(command==Command.SET){if(option.equals("DATASET"))return "FILE";if(option.equals("OBJECTNAME"))return "DSNAME";}return option;
    }
    private static Role role(Command command,TargetMode mode,String option,Set<String> names) {
        if(option.equals("FILE"))return mode==TargetMode.OUTPUT?Role.WRITE:Role.READ;
        if(Set.of("RESP","RESP2").contains(option))return Role.WRITE;
        if(command==Command.INQUIRE)return Role.WRITE;
        if(command==Command.SET)return Role.READ;
        if(option.equals("INTO")||option.equals("SET")||option.equals("NUMREC"))return Role.WRITE;
        if(option.equals("TOKEN"))return READS.contains(command.name())?Role.WRITE:Role.READ;
        if(option.equals("LENGTH")&&READS.contains(command.name()))return Role.READ_WRITE;
        if(option.equals("RIDFLD")) {
            if(command==Command.READNEXT||command==Command.READPREV||command==Command.READ&&names.contains("GENERIC"))return Role.READ_WRITE;
            if(command==Command.WRITE&&(names.contains("RBA")||names.contains("XRBA")))return Role.WRITE;
        }
        return Role.READ;
    }
}
