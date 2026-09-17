package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.*;

/** Native N-LR syntax facts. Uses parser identities, never statement text or runtime assumptions. */
final class FileIoSyntax {
    private FileIoSyntax() { }
    static boolean isNativeStatement(CobolParser.StatementContext c) {
        return c.openStatement()!=null||c.closeStatement()!=null||c.readStatement()!=null||c.writeStatement()!=null
            ||c.rewriteStatement()!=null||c.deleteStatement()!=null||c.startStatement()!=null;
    }
    static List<ParserRuleContext> handlerContexts(CobolParser.StatementContext root) {
        var result=new ArrayList<ParserRuleContext>();var pending=new ArrayDeque<org.antlr.v4.runtime.tree.ParseTree>();
        for(int i=0;i<root.getChildCount();i++)pending.addLast(root.getChild(i));
        while(!pending.isEmpty()) {
            var node=pending.removeFirst();if(node instanceof CobolParser.StatementContext)continue;
            if(node instanceof CobolParser.AtEndPhraseContext||node instanceof CobolParser.NotAtEndPhraseContext
                ||node instanceof CobolParser.InvalidKeyPhraseContext||node instanceof CobolParser.NotInvalidKeyPhraseContext
                ||node instanceof CobolParser.WriteAtEndOfPagePhraseContext||node instanceof CobolParser.WriteNotAtEndOfPagePhraseContext) {
                result.add((ParserRuleContext)node);continue;
            }
            for(int i=0;i<node.getChildCount();i++)pending.addLast(node.getChild(i));
        }
        return List.copyOf(result);
    }

    static Ast.FileOperandRole operandRole(ParserRuleContext root, ParserRuleContext operand) {
        if (command(root)==null) return null;
        if (operand instanceof CobolParser.RecordNameContext) return Ast.FileOperandRole.RECORD;
        for(var p=operand.getParent();p!=null&&p!=root;p=p.getParent()) {
            if(p instanceof CobolParser.ReadIntoContext)return Ast.FileOperandRole.INTO;
            if(p instanceof CobolParser.ReadKeyContext || p instanceof CobolParser.StartKeyContext)return Ast.FileOperandRole.KEY;
            if(p instanceof CobolParser.WriteFromPhraseContext || p instanceof CobolParser.RewriteFromContext)return Ast.FileOperandRole.FROM;
            if(p instanceof CobolParser.WriteAdvancingPhraseContext)return Ast.FileOperandRole.ADVANCING;
        }
        return null;
    }
    private static Ast.FileCommand command(ParserRuleContext c) {
        if(c instanceof CobolParser.OpenStatementContext)return Ast.FileCommand.OPEN;
        if(c instanceof CobolParser.ReadStatementContext)return Ast.FileCommand.READ;
        if(c instanceof CobolParser.WriteStatementContext)return Ast.FileCommand.WRITE;
        if(c instanceof CobolParser.RewriteStatementContext)return Ast.FileCommand.REWRITE;
        if(c instanceof CobolParser.DeleteStatementContext)return Ast.FileCommand.DELETE_RECORD;
        if(c instanceof CobolParser.StartStatementContext)return Ast.FileCommand.START;
        if(c instanceof CobolParser.CloseStatementContext)return Ast.FileCommand.CLOSE;
        return null;
    }
    static Optional<Ast.FileIoSurface> project(ParserRuleContext c, Map<ParserRuleContext,Ast.Node> nodes,
            List<ParserRuleContext> contexts,List<Ast.StatementClause> clauses) {
        var command=command(c);if(command==null)return Optional.empty();
        var profile=Ast.FileSyntaxProfile.N_LR;
        if(c instanceof CobolParser.ReadStatementContext r && r.readWith()!=null)profile=Ast.FileSyntaxProfile.UNSUPPORTED;
        if(c instanceof CobolParser.CloseStatementContext x && x.closeFile().stream().anyMatch(f->f.closePortFileIOStatement()!=null))profile=Ast.FileSyntaxProfile.UNSUPPORTED;
        if(c instanceof CobolParser.WriteStatementContext w && w.writeFromPhrase()!=null && w.writeFromPhrase().literal()!=null)profile=Ast.FileSyntaxProfile.UNSUPPORTED;
        var files=new ArrayList<Ast.FileIoOperand>();var operands=new ArrayList<Ast.FileDataOperand>();
        // Identity map iteration is not semantic order. Token positions are unique among these operand roots.
        var entries=new ArrayList<>(nodes.entrySet());entries.sort(Comparator.comparingInt(e->e.getKey().getStart().getTokenIndex()));
        for(var e:entries) {
            var role=operandRole(c,e.getKey());if(role!=null)operands.add(new Ast.FileDataOperand(role,e.getValue()));
            if(e.getKey() instanceof CobolParser.FileNameContext || role==Ast.FileOperandRole.RECORD) {
                var mode=Ast.FileOpenMode.UNSPECIFIED;var options=new ArrayList<Ast.FileOption>();
                for(var p=e.getKey().getParent();p!=null&&p!=c;p=p.getParent()) {
                    if(p instanceof CobolParser.OpenInputStatementContext)mode=Ast.FileOpenMode.INPUT;
                    else if(p instanceof CobolParser.OpenOutputStatementContext)mode=Ast.FileOpenMode.OUTPUT;
                    else if(p instanceof CobolParser.OpenIOStatementContext)mode=Ast.FileOpenMode.IO;
                    else if(p instanceof CobolParser.OpenExtendStatementContext)mode=Ast.FileOpenMode.EXTEND;
                    if(p instanceof CobolParser.OpenInputContext || p instanceof CobolParser.OpenOutputContext) options.addAll(options(p));
                    if(p instanceof CobolParser.CloseFileContext f) {
                        if(f.closeRelativeStatement()!=null)options.addAll(options(f.closeRelativeStatement()));
                        if(f.closeReelUnitStatement()!=null)options.addAll(options(f.closeReelUnitStatement()));
                    }
                }
                files.add(new Ast.FileIoOperand(e.getValue(),mode,options));
            }
        }
        var options=new ArrayList<Ast.FileOption>();var relation=Ast.FileKeyRelation.UNSPECIFIED;
        boolean terminated=false;
        if(c instanceof CobolParser.ReadStatementContext r){if(r.NEXT()!=null)options.add(Ast.FileOption.NEXT);terminated=r.END_READ()!=null;}
        if(c instanceof CobolParser.DeleteStatementContext d)terminated=d.END_DELETE()!=null;
        if(c instanceof CobolParser.RewriteStatementContext r)terminated=r.END_REWRITE()!=null;
        if(c instanceof CobolParser.StartStatementContext s) {
            terminated=s.END_START()!=null;var key=s.startKey();relation=Ast.FileKeyRelation.EQUAL;
            if(key!=null) {
                if(key.NOT()!=null || key.MORETHANOREQUAL()!=null || key.OR()!=null)relation=Ast.FileKeyRelation.GREATER_OR_EQUAL;
                else if(key.GREATER()!=null || key.MORETHANCHAR()!=null)relation=Ast.FileKeyRelation.GREATER;
            }
        }
        if(c instanceof CobolParser.WriteStatementContext w) {
            terminated=w.END_WRITE()!=null;var adv=w.writeAdvancingPhrase();
            if(adv!=null){options.add(adv.BEFORE()!=null?Ast.FileOption.BEFORE_ADVANCING:Ast.FileOption.AFTER_ADVANCING);if(adv.writeAdvancingPage()!=null)options.add(Ast.FileOption.PAGE);}
        }
        var handlers=new ArrayList<Ast.FileHandler>();
        for(int i=0;i<contexts.size();i++) {
            var h=contexts.get(i);Ast.FileHandlerKind kind=null;
            if(h instanceof CobolParser.AtEndPhraseContext)kind=Ast.FileHandlerKind.AT_END;
            else if(h instanceof CobolParser.NotAtEndPhraseContext)kind=Ast.FileHandlerKind.NOT_AT_END;
            else if(h instanceof CobolParser.InvalidKeyPhraseContext)kind=Ast.FileHandlerKind.INVALID_KEY;
            else if(h instanceof CobolParser.NotInvalidKeyPhraseContext)kind=Ast.FileHandlerKind.NOT_INVALID_KEY;
            else if(h instanceof CobolParser.WriteAtEndOfPagePhraseContext)kind=Ast.FileHandlerKind.AT_END_OF_PAGE;
            else if(h instanceof CobolParser.WriteNotAtEndOfPagePhraseContext)kind=Ast.FileHandlerKind.NOT_AT_END_OF_PAGE;
            if(kind!=null)handlers.add(new Ast.FileHandler(kind,clauses.get(i)));
        }
        return Optional.of(new Ast.FileIoSurface(command,files,profile,operands,options,relation,terminated,handlers));
    }
    private static List<Ast.FileOption> options(ParserRuleContext c) {
        var out=new ArrayList<Ast.FileOption>();
        if(c.getToken(CobolParser.REVERSED,0)!=null)out.add(Ast.FileOption.REVERSED);
        if(c.getToken(CobolParser.REWIND,0)!=null)out.add(Ast.FileOption.NO_REWIND);
        if(c.getToken(CobolParser.LOCK,0)!=null)out.add(Ast.FileOption.LOCK);
        if(c.getToken(CobolParser.REEL,0)!=null)out.add(Ast.FileOption.REEL);
        if(c.getToken(CobolParser.UNIT,0)!=null)out.add(Ast.FileOption.UNIT);
        if(c.getToken(CobolParser.REMOVAL,0)!=null)out.add(Ast.FileOption.FOR_REMOVAL);
        return out;
    }
}
