package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import io.github.gustavo2358.cobolexplorer.antlr.*;
import org.antlr.v4.runtime.*;

/** Explicit ABEND command syntax, never a classification of arbitrary CICS failures. */
final class CicsAbendSyntax {
    enum Eligibility { HANDLER_ELIGIBLE, HANDLERS_BYPASSED, UNAVAILABLE }
    record Event(String raw,Eligibility eligibility,List<CicsCommandSyntax.Option> options,List<String> gaps) {
        Event {options=List.copyOf(options);gaps=List.copyOf(gaps);}
    }
    static Optional<Event> parse(String raw) {
        var parsed=CicsCommandSyntax.parse(raw);
        if(parsed.isEmpty()||!parsed.get().name().equals("ABEND"))return Optional.empty();
        var command=parsed.get();var gaps=new LinkedHashSet<>(command.gaps());var names=new HashSet<String>();
        for(var option:command.options()) {
            if(!Set.of("CANCEL","NODUMP","ABCODE").contains(option.name()))gaps.add("CICS_ABEND_UNMODELED_OPTION");
            if(!names.add(option.name()))gaps.add("CICS_ABEND_DUPLICATE_OPTION");
            if(option.name().equals("ABCODE")) {
                if(option.operand().isEmpty()||!dumpOperand(option.operand().orElseThrow().strip()))gaps.add("CICS_ABEND_OPERAND_SHAPE");
            } else if(option.operand().isPresent())gaps.add("CICS_ABEND_OPERAND_SHAPE");
        }
        var eligibility=!command.ended()||!gaps.isEmpty()?Eligibility.UNAVAILABLE:
                names.contains("CANCEL")?Eligibility.HANDLERS_BYPASSED:Eligibility.HANDLER_ELIGIBLE;
        return Optional.of(new Event(raw,eligibility,command.options(),List.copyOf(gaps)));
    }
    private static boolean dumpOperand(String text) {
        if(CicsCommandSyntax.literal(text).filter(s->!s.isEmpty()).isPresent())return true;
        // Deliberately bounded DATA syntax. Qualification/subscripts/functions remain unavailable.
        if(text.isEmpty()||!text.codePoints().allMatch(c->c>='A'&&c<='Z'||c>='a'&&c<='z'||c>='0'&&c<='9'||c=='-'))return false;
        var failed=new boolean[1];var listener=new BaseErrorListener(){@Override public void syntaxError(Recognizer<?,?> r,Object s,int l,int c,String m,RecognitionException e){failed[0]=true;}};
        var lexer=new CobolLexer(CharStreams.fromString(text));lexer.removeErrorListeners();lexer.addErrorListener(listener);
        var parser=new CobolParser(new CommonTokenStream(lexer));parser.removeErrorListeners();parser.setErrorHandler(new BailErrorStrategy());
        try {var id=parser.identifier();return !failed[0]&&id.qualifiedDataName()!=null&&parser.getCurrentToken().getType()==Token.EOF;}
        catch(org.antlr.v4.runtime.misc.ParseCancellationException e){return false;}
    }
}
