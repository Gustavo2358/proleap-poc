package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.*;
import org.antlr.v4.runtime.*;
import java.util.*;

/** Syntax bridge only: CICS selects operands; the canonical COBOL grammar parses host references. */
final class CicsHostSyntax {
    record Host(String option,int optionStart,Ast.EmbeddedHostRole role,CobolParser.IdentifierContext identifier) { }
    static List<Host> parse(String raw,int sourceOffset,int sourceLine,int sourceColumn,int anchorToken) {
        var parsed=new CicsProgramControlAnalyzer().parse(raw);if(parsed.isEmpty())return List.of();
        var result=new ArrayList<Host>();
        for(var option:parsed.orElseThrow().options()) {
            if(!Set.of("PROGRAM","COMMAREA","CHANNEL","LENGTH","RESP","RESP2","INPUTMSG","INPUTMSGLEN","DATALENGTH","SYSID","TRANSID").contains(option.name())||option.operand().isEmpty())continue;
            int begin=raw.indexOf('(',option.start())+1;String operand=option.operand().orElseThrow();
            var lexer=new CobolLexer(CharStreams.fromString(operand));lexer.removeErrorListeners();
            var failed=new boolean[1];lexer.addErrorListener(new BaseErrorListener(){@Override public void syntaxError(Recognizer<?,?> r,Object symbol,int line,int col,String msg,RecognitionException e){failed[0]=true;}});
            var tokens=new CommonTokenStream(lexer);tokens.fill();
            var parser=new CobolParser(tokens);parser.removeErrorListeners();parser.setErrorHandler(new BailErrorStrategy());
            CobolParser.IdentifierContext identifier;
            try {identifier=parser.identifier();if(failed[0]||parser.getCurrentToken().getType()!=Token.EOF)continue;}
            catch(org.antlr.v4.runtime.misc.ParseCancellationException e){continue;}
            // Re-anchor the grammar tree in the original flattened payload. COPY provenance comes from SourceMap.
            for(var token:tokens.getTokens())if(token instanceof CommonToken t) {
                t.setText(t.getText());int relative=t.getStartIndex();int line=sourceLine,col=sourceColumn;
                for(int i=0;i<begin+Math.max(relative,0);i++){if(raw.charAt(i)=='\n'){line++;col=0;}else col++;}
                t.setLine(line);t.setCharPositionInLine(col);t.setStartIndex(sourceOffset+begin+t.getStartIndex());t.setStopIndex(sourceOffset+begin+t.getStopIndex());t.setTokenIndex(anchorToken);
            }
            var role=Set.of("RESP","RESP2").contains(option.name())?Ast.EmbeddedHostRole.WRITE:option.name().equals("COMMAREA")?Ast.EmbeddedHostRole.READ_WRITE:Ast.EmbeddedHostRole.READ;
            result.add(new Host(option.name(),option.start(),role,identifier));
        }
        return List.copyOf(result);
    }
}
