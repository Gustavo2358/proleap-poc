package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.*;
import org.antlr.v4.runtime.*;
import java.util.*;

/** Syntax bridge only: CICS selects operands; the canonical COBOL grammar parses host references. */
final class CicsHostSyntax {
    record Host(String option,int optionStart,Ast.EmbeddedHostRole role,CobolParser.IdentifierContext identifier) { }
    static List<Host> parse(String raw,int sourceOffset,int sourceLine,int sourceColumn,int anchorToken) {
        var parsed=new CicsProgramControlAnalyzer().parse(raw);
        var file=new CicsFileControlAnalyzer().parse(raw);
        var options=new ArrayList<CicsCommandSyntax.Option>();var roles=new HashMap<Integer,Ast.EmbeddedHostRole>();
        if(parsed.isPresent())for(var option:parsed.get().options()) {
            if(!Set.of("PROGRAM","COMMAREA","CHANNEL","LENGTH","RESP","RESP2","INPUTMSG","INPUTMSGLEN","DATALENGTH","SYSID","TRANSID").contains(option.name()))continue;
            options.add(new CicsCommandSyntax.Option(option.name(),option.operand(),option.start(),option.end()));
            roles.put(option.start(),Set.of("RESP","RESP2").contains(option.name())?Ast.EmbeddedHostRole.WRITE:option.name().equals("COMMAREA")?Ast.EmbeddedHostRole.READ_WRITE:Ast.EmbeddedHostRole.READ);
        }
        if(file.isPresent())for(var option:file.get().options()) {
            if(option.role()==CicsFileControlAnalyzer.Role.NONE)continue;
            options.add(option.syntax());roles.put(option.syntax().start(),switch(option.role()) {
                case WRITE -> Ast.EmbeddedHostRole.WRITE;case READ_WRITE -> Ast.EmbeddedHostRole.READ_WRITE;default -> Ast.EmbeddedHostRole.READ;
            });
        }
        CicsHandlerSyntax.parse(raw).ifPresent(handler->{
            for(var option:handler.options())if(Set.of("PROGRAM","RESP","RESP2").contains(option.name())) {
                options.add(option);roles.put(option.start(),option.name().equals("PROGRAM")?Ast.EmbeddedHostRole.READ:Ast.EmbeddedHostRole.WRITE);
            }
        });
        CicsCommandSemantics.parse(raw).ifPresent(command->{
            for(var option:command.options())if(Set.of("MAP","MAPSET","FROM","INTO","RESP","RESP2").contains(option.name())) {
                options.add(option);roles.put(option.start(),Set.of("INTO","RESP","RESP2").contains(option.name())?Ast.EmbeddedHostRole.WRITE:Ast.EmbeddedHostRole.READ);
            }
        });
        CicsCommandSyntax.parse(raw).filter(c->c.name().equals("RETURN")).ifPresent(command->{
            for(var option:command.options())if(Set.of("TRANSID","COMMAREA","LENGTH","RESP","RESP2").contains(option.name())) {
                options.add(option);roles.put(option.start(),Set.of("RESP","RESP2").contains(option.name())?Ast.EmbeddedHostRole.WRITE:Ast.EmbeddedHostRole.READ);
            }
        });
        var result=new ArrayList<Host>();
        for(var option:options) {
            if(option.operand().isEmpty())continue;
            int begin=raw.indexOf('(',option.start())+1;String operand=option.operand().orElseThrow();
            var identifier=parseReference(operand,raw,begin,sourceOffset,sourceLine,sourceColumn,anchorToken).orElse(null);
            if(identifier==null)continue;
            var role=roles.get(option.start());
            result.add(new Host(option.name(),option.start(),role,identifier));
        }
        return List.copyOf(result);
    }
    static Optional<CobolParser.IdentifierContext> parseReference(String operand,String raw,int begin,
            int sourceOffset,int sourceLine,int sourceColumn,int anchorToken) {
        var lexer=new CobolLexer(CharStreams.fromString(operand));lexer.removeErrorListeners();
        var failed=new boolean[1];lexer.addErrorListener(new BaseErrorListener(){@Override public void syntaxError(Recognizer<?,?> r,Object symbol,int line,int col,String msg,RecognitionException e){failed[0]=true;}});
        var tokens=new CommonTokenStream(lexer);tokens.fill();
        var parser=new CobolParser(tokens);parser.removeErrorListeners();parser.setErrorHandler(new BailErrorStrategy());
        CobolParser.IdentifierContext identifier;
        try {identifier=parser.identifier();if(failed[0]||parser.getCurrentToken().getType()!=Token.EOF)return Optional.empty();}
        catch(org.antlr.v4.runtime.misc.ParseCancellationException e){return Optional.empty();}
        // Only reference-shaped hosts have an EmbeddedHostOperand today.
        // Do not allocate then discard LENGTH OF / function AST nodes: that breaks preorder identity.
        if(identifier.qualifiedDataName()==null&&identifier.tableCall()==null)return Optional.empty();
        // Re-anchor the grammar tree in the original flattened payload. COPY provenance comes from SourceMap.
        for(var token:tokens.getTokens())if(token instanceof CommonToken t) {
            t.setText(t.getText());int relative=t.getStartIndex();int line=sourceLine,col=sourceColumn;
            for(int i=0;i<begin+Math.max(relative,0);i++){if(raw.charAt(i)=='\n'){line++;col=0;}else col++;}
            t.setLine(line);t.setCharPositionInLine(col);t.setStartIndex(sourceOffset+begin+t.getStartIndex());t.setStopIndex(sourceOffset+begin+t.getStopIndex());t.setTokenIndex(anchorToken);
        }
        return Optional.of(identifier);
    }
}
