package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Dedicated parser for the preserved CICS command surface. No runtime value analysis. */
public final class CicsProgramControlAnalyzer {
    public enum Command { LINK, XCTL }
    public enum EntryMode { UNKNOWN, NEW_LOGICAL_LEVEL, DISABLED }
    public record Option(String name, Optional<String> operand, int start, int end) {
        public Option { Objects.requireNonNull(name); Objects.requireNonNull(operand); }
    }
    public record Fact(Command command, String raw, List<Option> options, Optional<String> literal,
                       Optional<String> host, int targetStart, int targetEnd, List<String> gaps) {
        public Fact { options=List.copyOf(options); gaps=List.copyOf(gaps); }
    }
    public record Key(ResolutionContracts.ProgramUnitId unit, int statement) { }
    public static final class Contribution {
        private final CompilationUnitBuildResult owner;
        private final Map<Key,Fact> facts;
        private final Set<Key> defaultHandlers;
        private final Optional<CicsFileControlAnalyzer.Contribution> files;
        private Contribution(CompilationUnitBuildResult owner, Map<Key,Fact> facts,Set<Key> defaultHandlers,boolean enabled) { this.owner=owner;this.facts=Map.copyOf(facts);this.defaultHandlers=Set.copyOf(defaultHandlers);this.files=enabled?Optional.of(new CicsFileControlAnalyzer().analyze(owner)):Optional.empty();this.handlers=Optional.empty();this.abends=Optional.empty();this.commands=Optional.empty(); }
        private final Optional<CicsHandlerSemantics> handlers;
        private final Optional<CicsAbendSemantics> abends;
        private final Optional<CicsCommandSemantics> commands;
        public Contribution withCommands(CicsCommandSemantics contribution) {
            if(!contribution.belongsTo(owner))throw new IllegalArgumentException("command contribution belongs to another frontend");
            return new Contribution(owner,facts,defaultHandlers,files,handlers,abends,Optional.of(contribution));
        }
        public Optional<CicsCommandSemantics.Fact> commandFact(ResolutionContracts.ProgramUnitId unit,int statement){return commands.flatMap(c->c.fact(unit,statement));}
        public Contribution withAbendEvents(CicsAbendSemantics contribution) {
            if(!contribution.belongsTo(owner))throw new IllegalArgumentException("ABEND contribution belongs to another frontend");
            return new Contribution(owner,facts,defaultHandlers,files,handlers,Optional.of(contribution),commands);
        }
        public Optional<CicsAbendSemantics.Fact> abendFact(ResolutionContracts.ProgramUnitId unit,int statement){return abends.flatMap(a->a.fact(unit,statement));}
        /** Qualified ordinary return only; never dispatch or a proof about current handler state. */
        boolean handlerOrdinaryCompletion(ResolutionContracts.ProgramUnitId unit,Ast.Node statement) {
            if(abends.isEmpty())return false; // The frozen 2.41 capability made no completion claim.
            return handlerFact(unit,statement.meta().id()).filter(h->h.gaps().isEmpty()
                &&(h.action()==CicsHandlerSemantics.Action.CANCEL||h.action()==CicsHandlerSemantics.Action.RESET
                    ||h.action()==CicsHandlerSemantics.Action.ACTIVATE&&h.targetKind()==CicsHandlerSemantics.TargetKind.LABEL
                        &&h.labelTarget().flatMap(CicsHandlerSemantics.LabelTarget::entry).isPresent())).isPresent();
        }
        public boolean handlerRegistrationEffects(ResolutionContracts.ProgramUnitId unit,int statement) {
            return commands.isPresent() && handlerFact(unit,statement).filter(CicsHandlerSemantics.Fact::registrationPreservesApplicationMemory).isPresent();
        }
        public Contribution withHandlers(CicsHandlerSemantics contribution) {
            if(!contribution.belongsTo(owner))throw new IllegalArgumentException("handler contribution belongs to another frontend");
            return new Contribution(owner,facts,defaultHandlers,files,Optional.of(contribution),abends,commands);
        }
        private Contribution(CompilationUnitBuildResult owner,Map<Key,Fact> facts,Set<Key> defaults,Optional<CicsFileControlAnalyzer.Contribution> files,Optional<CicsHandlerSemantics> handlers,Optional<CicsAbendSemantics> abends,Optional<CicsCommandSemantics> commands) {
            this.owner=owner;this.facts=facts;this.defaultHandlers=defaults;this.files=files;this.handlers=handlers;this.abends=abends;this.commands=commands;
        }
        public Optional<CicsHandlerSemantics.Fact> handlerFact(ResolutionContracts.ProgramUnitId unit,int statement){return handlers.flatMap(h->h.fact(unit,statement));}
        public Optional<CicsFileControlAnalyzer.Fact> fileFact(ResolutionContracts.ProgramUnitId unit,int statement){return files.flatMap(f->f.fact(unit,statement));}
        public boolean defaultHandlers(ResolutionContracts.ProgramUnitId unit,int statement){return defaultHandlers.contains(new Key(unit,statement));}
        public boolean belongsTo(CompilationUnitBuildResult frontend) { return owner==frontend; }
        public Optional<Fact> fact(ResolutionContracts.ProgramUnitId unit,int statement) { return Optional.ofNullable(facts.get(new Key(unit,statement))); }
        /** PROGRAM is an input name, not a mutable/escaping data area (IBM CICS LINK/XCTL).
         * All other data options remain foreign mutation blockers for the lifetime proof. */
        boolean localStorageInputOnly(ResolutionContracts.ProgramUnitId unit,Ast.Node node) {
            if(!(node instanceof Ast.EmbeddedLanguageStatement embedded))return false;
            var fact=fact(unit,node.meta().id()).orElse(null);
            if(fact==null||!fact.gaps().isEmpty()||fact.options().stream().anyMatch(o->!Set.of("PROGRAM","NOHANDLE").contains(o.name())))return false;
            if(fact.options().stream().map(Option::name).distinct().count()!=fact.options().size())return false;
            return fact.literal().isPresent()?embedded.hostOperands().isEmpty():fact.host().isPresent()
                &&embedded.hostOperands().size()==1&&embedded.hostOperands().get(0).option().equals("PROGRAM")
                &&embedded.hostOperands().get(0).role()==Ast.EmbeddedHostRole.READ;
        }
        boolean boundedLocal(ResolutionContracts.ProgramUnitId unit,Ast.Node node) {
            return node!=null&&(fileFact(unit,node.meta().id()).filter(CicsFileControlAnalyzer.Fact::boundedLocal).isPresent()||fact(unit,node.meta().id()).filter(f->f.gaps().isEmpty()&&f.options().stream().anyMatch(o->o.name().equals("RESP")||o.name().equals("NOHANDLE"))).isPresent());
        }
        /** Physical provenance remains inexact; only the active typed contribution bounds CICS control. */
        boolean boundedRegion(ResolutionContracts.ProgramUnitId unit,Ast.Node node) {
            if(node instanceof Ast.EmbeddedLanguageStatement)return boundedLocal(unit,node);
            if(node.meta().provenance().exact())return true;
            var children=Ast.children(node);return !children.isEmpty()&&children.stream().allMatch(child->boundedRegion(unit,child));
        }
    }
    public Contribution analyze(CompilationUnitBuildResult frontend) {return analyze(frontend,null);}
    public Contribution analyze(CompilationUnitBuildResult frontend,ResolutionAnalysisReport report) {return analyze(frontend,report,EntryMode.UNKNOWN);}
    public Contribution analyze(CompilationUnitBuildResult frontend,ResolutionAnalysisReport report,EntryMode mode) {
        if(mode==EntryMode.DISABLED)return new Contribution(frontend,Map.of(),Set.of(),false);
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
        var defaults=new HashSet<Key>();
        if(mode==EntryMode.NEW_LOGICAL_LEVEL&&report!=null)
            for(var unit:frontend.compilationUnit().programUnits()) {
                if(!report.inputComplete(unit.id()))continue;
                var nodes=new HashMap<Integer,Ast.Node>();var todo=new ArrayDeque<Ast.Node>();todo.push(unit.program());
                while(!todo.isEmpty()){var n=todo.pop();if(n instanceof Ast.Program&&n!=unit.program())continue;nodes.put(n.meta().id(),n);Ast.children(n).forEach(todo::push);}
                if(nodes.values().stream().anyMatch(n->n.meta().origin().grammarRule().equals("entryStatement")))continue;
                var division=unit.program().divisions().stream().filter(d->d.divisionKind()==Ast.DivisionKind.PROCEDURE).findFirst();
                if(division.isEmpty()||division.get().procedureEntry().isEmpty()||division.get().procedureEntry().get().declarativesPresent())continue;
                // Narrow entry-prefix proof: only canonical MOVE edges before the first CICS command.
                var current=division.get().procedureEntry().get().startStatementId().orElse(null);var seen=new HashSet<Integer>();
                while(current!=null&&seen.add(current)) {
                    var n=nodes.get(current);var fact=facts.get(new Key(unit.id(),current));
                    if(fact!=null) {if(fact.gaps().isEmpty()&&fact.options().stream().noneMatch(o->o.name().equals("RESP2")))defaults.add(new Key(unit.id(),current));break;}
                    if(!(n instanceof Ast.MoveStatement)||!n.meta().provenance().exact())break;
                    current=division.get().normalContinuations().get(current);
                }
            }
        return new Contribution(frontend,facts,defaults,true);
    }
    public Optional<Fact> parse(String raw) {
        var syntax=CicsCommandSyntax.parse(raw);
        if(syntax.isEmpty()||!Set.of("LINK","XCTL").contains(syntax.get().name()))return Optional.empty();
        var command=Command.valueOf(syntax.get().name());var options=new ArrayList<Option>();
        var gaps=new LinkedHashSet<>(syntax.get().gaps());boolean ended=syntax.get().ended();
        for(var source:syntax.get().options()) {
            var option=source.name();var operand=source.operand();
            options.add(new Option(option,operand,source.start(),source.end()));
            if(!Set.of("PROGRAM","COMMAREA","LENGTH","CHANNEL","RESP","RESP2","NOHANDLE","INPUTMSG","INPUTMSGLEN","SYSID","SYNCONRETURN","TRANSID","DATALENGTH").contains(option))gaps.add("CICS_UNMODELED_OPTION");
            if(command==Command.XCTL&&Set.of("SYSID","SYNCONRETURN","TRANSID","DATALENGTH").contains(option))gaps.add("CICS_OPTION_INVALID_FOR_COMMAND");
            if((option.equals("NOHANDLE")||option.equals("SYNCONRETURN"))==operand.isPresent())gaps.add("CICS_OPTION_OPERAND_SHAPE");
        }
        var targets=options.stream().filter(o->o.name().equals("PROGRAM")).toList();
        Optional<String> literal=Optional.empty(),host=Optional.empty();int start=0,end=0;
        if(targets.size()!=1)gaps.add(targets.isEmpty()?"CICS_PROGRAM_MISSING":"CICS_PROGRAM_DUPLICATED");
        else if(targets.get(0).operand().isPresent()) {
            var option=targets.get(0);start=option.start();end=option.end();String value=option.operand().orElseThrow().strip();
            if(value.startsWith("'")||value.startsWith("\"")) {
                var decoded=CicsCommandSyntax.literal(value);if(decoded.isPresent())literal=decoded;else gaps.add("CICS_INVALID_PROGRAM_LITERAL");
            } else if(!value.isEmpty())host=Optional.of(value);else gaps.add("CICS_EMPTY_PROGRAM");
        } else gaps.add("CICS_PROGRAM_OPERAND_MISSING");
        if(!ended||gaps.contains("CICS_TRUNCATED_OPERAND")){literal=Optional.empty();host=Optional.empty();}
        return Optional.of(new Fact(command,raw,options,literal,host,start,end,List.copyOf(gaps)));
    }
}
