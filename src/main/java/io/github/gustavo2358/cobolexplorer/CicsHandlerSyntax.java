package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.*;
import org.antlr.v4.runtime.*;
import java.util.*;

/** HANDLE ABEND operation syntax, independent of execution, state and dispatch. */
final class CicsHandlerSyntax {
    enum Action { ACTIVATE, CANCEL, RESET, UNAVAILABLE }
    enum TargetKind { LABEL, PROGRAM, NONE, UNAVAILABLE }
    record Operation(String raw, Action action, TargetKind targetKind, Optional<String> targetSyntax,
                     List<CicsCommandSyntax.Option> options, List<String> gaps) {
        Operation { options=List.copyOf(options);gaps=List.copyOf(gaps); }
    }
    static Optional<Operation> parse(String raw) {
        var command=CicsCommandSyntax.parse(raw);
        if(command.isEmpty()||!command.get().name().equals("HANDLE")||command.get().options().isEmpty()
                ||!command.get().options().get(0).name().equals("ABEND"))return Optional.empty();
        var options=command.get().options();var gaps=new LinkedHashSet<>(command.get().gaps());
        var names=new HashSet<String>();
        for(var option:options) {
            if(!Set.of("ABEND","LABEL","PROGRAM","CANCEL","RESET","NOHANDLE","RESP","RESP2").contains(option.name()))gaps.add("CICS_HANDLER_UNMODELED_OPTION");
            if(!names.add(option.name()))gaps.add("CICS_HANDLER_DUPLICATE_OPTION");
            boolean operand=Set.of("LABEL","PROGRAM","RESP","RESP2").contains(option.name());
            if(operand!=option.operand().isPresent()||option.operand().filter(String::isBlank).isPresent())gaps.add("CICS_HANDLER_OPERAND_SHAPE");
        }
        var selectors=options.stream().filter(o->Set.of("LABEL","PROGRAM","CANCEL","RESET").contains(o.name())).toList();
        if(selectors.size()>1)gaps.add("CICS_HANDLER_CONFLICTING_ACTIONS");
        Action action=Action.CANCEL;TargetKind kind=TargetKind.NONE;Optional<String> target=Optional.empty();
        if(selectors.size()==1) {
            var selected=selectors.get(0);target=selected.operand();
            if(Set.of("LABEL","PROGRAM").contains(selected.name())) {action=Action.ACTIVATE;kind=TargetKind.valueOf(selected.name());}
            else action=Action.valueOf(selected.name());
        }
        if(!gaps.isEmpty()||!command.get().ended()) {action=Action.UNAVAILABLE;kind=TargetKind.UNAVAILABLE;target=Optional.empty();}
        return Optional.of(new Operation(raw,action,kind,target,options,List.copyOf(gaps)));
    }
    /** Syntax bridge to the COBOL procedure grammar. No spelling-based binding here. */
    static Optional<CobolParser.ProcedureNameContext> label(String raw,int offset,int line,int column,int anchorToken) {
        var operation=parse(raw);
        if(operation.isEmpty()||operation.get().targetKind()!=TargetKind.LABEL)return Optional.empty();
        var option=operation.get().options().stream().filter(o->o.name().equals("LABEL")).findFirst().orElseThrow();
        int begin=raw.indexOf('(',option.start())+1;
        var lexer=new CobolLexer(CharStreams.fromString(option.operand().orElseThrow()));lexer.removeErrorListeners();
        var failed=new boolean[1];lexer.addErrorListener(new BaseErrorListener(){@Override public void syntaxError(Recognizer<?,?> r,Object symbol,int l,int c,String msg,RecognitionException e){failed[0]=true;}});
        var tokens=new CommonTokenStream(lexer);tokens.fill();var parser=new CobolParser(tokens);
        parser.removeErrorListeners();parser.setErrorHandler(new BailErrorStrategy());
        CobolParser.ProcedureNameContext tree;
        try {tree=parser.procedureName();if(failed[0]||parser.getCurrentToken().getType()!=Token.EOF)return Optional.empty();}
        catch(org.antlr.v4.runtime.misc.ParseCancellationException e){return Optional.empty();}
        for(var token:tokens.getTokens())if(token instanceof CommonToken t) {
            t.setText(t.getText());int l=line,c=column;
            for(int i=0;i<begin+Math.max(t.getStartIndex(),0);i++){if(raw.charAt(i)=='\n'){l++;c=0;}else c++;}
            t.setLine(l);t.setCharPositionInLine(c);t.setStartIndex(offset+begin+t.getStartIndex());t.setStopIndex(offset+begin+t.getStopIndex());t.setTokenIndex(anchorToken);
        }
        return Optional.of(tree);
    }
}
