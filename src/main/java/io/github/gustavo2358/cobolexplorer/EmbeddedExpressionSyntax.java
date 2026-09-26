package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.*;
import org.antlr.v4.runtime.*;
import java.util.*;

/** Small host-expression bridge. COBOL grammar owns structure; SourceMap owns source identity. */
final class EmbeddedExpressionSyntax {
    record Parsed(ParserRuleContext tree,CommonTokenStream tokens) { }
    record Host(String option,int optionStart,ParserRuleContext tree) { }
    static boolean supported(String text){return parse(text).isPresent();}
    private static Optional<Parsed> parse(String text) {
        var failed=new boolean[1];var listener=new BaseErrorListener(){@Override public void syntaxError(Recognizer<?,?> r,Object s,int l,int c,String m,RecognitionException e){failed[0]=true;}};
        var lexer=new CobolLexer(CharStreams.fromString(text));lexer.removeErrorListeners();lexer.addErrorListener(listener);
        var tokens=new CommonTokenStream(lexer);tokens.fill();
        var parser=new CobolParser(tokens);parser.removeErrorListeners();parser.setErrorHandler(new BailErrorStrategy());
        try {
            ParserRuleContext tree;
            if(tokens.LA(1)==CobolParser.INTEGERLITERAL) tree=parser.literal();
            else tree=parser.identifier();
            if(failed[0]||parser.getCurrentToken().getType()!=Token.EOF)return Optional.empty();
            boolean valid=tree instanceof CobolParser.LiteralContext l&&l.numericLiteral()!=null&&l.numericLiteral().integerLiteral()!=null;
            if(tree instanceof CobolParser.IdentifierContext i)valid=i.qualifiedDataName()!=null||i.tableCall()!=null
                ||i.specialRegister()!=null&&i.specialRegister().LENGTH()!=null&&i.specialRegister().identifier()!=null
                  &&(i.specialRegister().identifier().qualifiedDataName()!=null||i.specialRegister().identifier().tableCall()!=null);
            return valid?Optional.of(new Parsed(tree,tokens)):Optional.empty();
        }catch(org.antlr.v4.runtime.misc.ParseCancellationException e){return Optional.empty();}
    }
    static List<Host> parse(String raw,int offset,int line,int column,int tokenIndex) {
        var command=CicsCommandSemantics.parse(raw);
        var options=new ArrayList<CicsCommandSyntax.Option>();
        if(command.filter(c->c.command()==CicsCommandSemantics.Kind.SEND_TERMINAL).isPresent())options.addAll(command.get().options());
        // Non-reference LENGTH operands have their own identity; they are not a write to the measured item.
        new CicsFileControlAnalyzer().parse(raw).ifPresent(file->{
            for(var o:file.options())if(o.syntax().name().equals("LENGTH")&&o.syntax().operand().isPresent()) {
                var parsed=parse(o.syntax().operand().orElseThrow());
                if(parsed.isPresent()&&parsed.get().tree() instanceof CobolParser.IdentifierContext i&&i.specialRegister()!=null)options.add(o.syntax());
            }
        });
        var out=new ArrayList<Host>();
        for(var option:options)if(option.name().equals("LENGTH")&&option.operand().isPresent()) {
            var parsed=parse(option.operand().orElseThrow());if(parsed.isEmpty())continue;
            int begin=raw.indexOf('(',option.start())+1;
            for(var token:parsed.get().tokens().getTokens())if(token instanceof CommonToken t) {
                t.setText(t.getText());int l=line,c=column;
                for(int n=0;n<begin+Math.max(0,t.getStartIndex());n++){if(raw.charAt(n)=='\n'){l++;c=0;}else c++;}
                t.setLine(l);t.setCharPositionInLine(c);t.setStartIndex(offset+begin+t.getStartIndex());t.setStopIndex(offset+begin+t.getStopIndex());t.setTokenIndex(tokenIndex);
            }
            out.add(new Host(option.name(),option.start(),parsed.get().tree()));
        }
        return List.copyOf(out);
    }
}
