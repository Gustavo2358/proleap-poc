package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;

/** HANDLE CONDITION syntax; registration never implies handler execution. */
final class CicsConditionSyntax {
    private CicsConditionSyntax() { }
    private static final Set<String> CONDITIONS=Set.of(
        "ALLOCERR","CBIDERR","CCSIDERR","CHANNELERR","CONTAINERERR","DISABLED","DSIDERR","DSSTAT","DUPKEY","DUPREC",
        "END","ENDDATA","ENDFILE","ENDINPT","ENQBUSY","ENVDEFERR","EOC","EODS","EOF","ERROR","EXPIRED","FILENOTFOUND",
        "FUNCERR","IGREQID","IGREQCD","ILLOGIC","INBFMH","INVERRTERM","INVEXITREQ","INVLDC","INVMPSZ","INVPARTNSET",
        "INVPARTN","INVREQ","INVTSREQ","IOERR","ISCINVREQ","ITEMERR","JIDERR","LENGERR","MAPERROR","MAPFAIL","NAMEERROR",
        "NODEIDERR","NOJBUFSP","NONVAL","NOPASSBKRD","NOPASSBKWR","NOSPACE","NOSPOOL","NOSTART","NOSTG","NOTALLOC",
        "NOTAUTH","NOTFINISHED","NOTFND","NOTOPEN","OPENERR","OVERFLOW","PARTNFAIL","PGMIDERR","QBUSY","QIDERR","QZERO",
        "RDATT","RETPAGE","ROLLEDBACK","RTEFAIL","RTESOME","SELNERR","SESSBUSY","SESSIONERR","SIGNAL","SPOLBUSY","SPOLERR",
        "STRELERR","SUPPRESSED","SYMBOLERR","SYSBUSY","SYSIDERR","TASKIDERR","TCIDERR","TEMPLATERR","TERMERR","TERMIDERR",
        "TOKENERR","TRANSIDERR","TSIOERR","UNEXPIN","USERIDERR","WRBRK","WRONGSTAT");
    static Optional<List<CicsCommandSyntax.Option>> parse(String raw) {
        var command=CicsCommandSyntax.parse(raw).orElse(null);
        if(command==null||!command.name().equals("HANDLE")||!command.ended()||!command.gaps().isEmpty()
                ||command.options().size()<2||command.options().size()>17)return Optional.empty();
        var first=command.options().get(0);
        if(!first.name().equals("CONDITION")||first.operand().isPresent())return Optional.empty();
        var options=command.options().subList(1,command.options().size());var seen=new HashSet<String>();
        for(var option:options) {
            if(!CONDITIONS.contains(option.name())||!seen.add(option.name()))return Optional.empty();
            if(option.operand().isPresent()&&label(raw,option,0,1,0,0).isEmpty())return Optional.empty();
        }
        return Optional.of(List.copyOf(options));
    }
    static Optional<CobolParser.ProcedureNameContext> label(String raw,CicsCommandSyntax.Option option,int offset,int line,int column,int token) {
        return option.operand().flatMap(s->CicsHandlerSyntax.labelOperand(raw,s,raw.indexOf('(',option.start())+1,offset,line,column,token));
    }
    static Optional<StatementEffectSummary> effects(Ast.Statement statement) {
        if(!(statement instanceof Ast.EmbeddedLanguageStatement s)||s.language()!=Ast.EmbeddedLanguage.CICS)return Optional.empty();
        var options=parse(s.rawText()).orElse(null);
        if(options==null||!s.hostOperands().isEmpty()||!s.expressionOperands().isEmpty()
                ||s.procedureOperands().size()!=options.stream().filter(o->o.operand().isPresent()).count())return Optional.empty();
        return Optional.of(new StatementEffectSummary(List.of(),List.of(),List.of(),List.of(),
            StatementEffectSummary.Bound.NONE,StatementEffectSummary.Bound.NONE,StatementEffectSummary.Bound.NONE,
            StatementEffectSummary.Environment.UNKNOWN,StatementEffectSummary.ValueTransform.UNKNOWN,StatementEffectSummary.Proof.CICS_CONDITION_REGISTRATION));
    }
}
