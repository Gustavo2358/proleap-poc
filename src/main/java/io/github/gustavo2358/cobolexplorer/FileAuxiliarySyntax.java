package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.*;
import java.util.function.Function;

/** Typed grammar extraction only. Text parameters preserve spelling; downstream never parses them. */
final class FileAuxiliarySyntax {
    private final Function<ParserRuleContext,Ast.Meta> meta;
    private final Function<ParserRuleContext,Ast.DataReference> data;
    private final Function<ParserRuleContext,String> text;
    FileAuxiliarySyntax(Function<ParserRuleContext,Ast.Meta> meta,Function<ParserRuleContext,Ast.DataReference> data,Function<ParserRuleContext,String> text){this.meta=meta;this.data=data;this.text=text;}
    List<Ast.FileAuxiliary> clauses(ParserRuleContext parent){var result=new ArrayList<Ast.FileAuxiliary>();walk(parent,result);return List.copyOf(result);}
    private void walk(ParserRuleContext c,List<Ast.FileAuxiliary> out){
        var value=clause(c);if(value!=null){out.add(value);return;}
        if(c.children!=null)for(var child:c.children)if(child instanceof ParserRuleContext p)walk(p,out);
    }
    private Ast.FileAuxiliary clause(ParserRuleContext c){
        if(!(c instanceof CobolParser.RerunClauseContext||c instanceof CobolParser.MultipleFileClauseContext||c instanceof CobolParser.ApplyWriteOnlyClauseContext||c instanceof CobolParser.CommitmentControlClauseContext||c instanceof CobolParser.ReserveClauseContext||c instanceof CobolParser.PaddingCharacterClauseContext||c instanceof CobolParser.RecordDelimiterClauseContext||c instanceof CobolParser.PasswordClauseContext||c instanceof CobolParser.BlockContainsClauseContext||c instanceof CobolParser.RecordContainsClauseContext||c instanceof CobolParser.LabelRecordsClauseContext||c instanceof CobolParser.ValueOfClauseContext||c instanceof CobolParser.DataRecordsClauseContext||c instanceof CobolParser.LinageClauseContext||c instanceof CobolParser.RecordingModeClauseContext||c instanceof CobolParser.CodeSetClauseContext||c instanceof CobolParser.ReportClauseContext))return null;
        var ownMeta=meta.apply(c);
        Ast.FileAuxKind kind;var files=new ArrayList<Ast.FileReference>();var refs=new ArrayList<Ast.FileAuxData>();var params=new ArrayList<Ast.FileAuxParameter>();
        Optional<Ast.FileAssignment> checkpoint=Optional.empty();var trigger=Ast.FileTrigger.NONE;
        if(c instanceof CobolParser.RerunClauseContext r){
            kind=Ast.FileAuxKind.RERUN;checkpoint=Optional.of(FileDeclarationSemantics.assignmentName(text.apply(r.assignmentName())));
            trigger=r.EVERY()==null?Ast.FileTrigger.SORT_MERGE:r.rerunEveryRecords()!=null?Ast.FileTrigger.RECORD_COUNT:r.rerunEveryOf()!=null?Ast.FileTrigger.END_VOLUME:Ast.FileTrigger.UNSUPPORTED;
            if(r.rerunEveryRecords()!=null){var e=r.rerunEveryRecords();param(params,"interval",e.integerLiteral());file(files,e.fileName());}
            if(r.rerunEveryOf()!=null)file(files,r.rerunEveryOf().fileName());
        }else if(c instanceof CobolParser.MultipleFileClauseContext r){kind=Ast.FileAuxKind.MULTIPLE_FILE;for(var f:r.multipleFilePosition()){file(files,f.fileName());param(params,"position",f.integerLiteral());}}
        else if(c instanceof CobolParser.ApplyWriteOnlyClauseContext r){kind=Ast.FileAuxKind.APPLY_WRITE_ONLY;for(var f:r.fileName())file(files,f);}
        else if(c instanceof CobolParser.CommitmentControlClauseContext r){kind=Ast.FileAuxKind.COMMITMENT_CONTROL;file(files,r.fileName());}
        else if(c instanceof CobolParser.ReserveClauseContext r){kind=Ast.FileAuxKind.RESERVE;param(params,"count",r.integerLiteral());if(r.NO()!=null)params.add(new Ast.FileAuxParameter("count","0"));}
        else if(c instanceof CobolParser.PaddingCharacterClauseContext r){kind=Ast.FileAuxKind.PADDING;ref(refs,"character",r.qualifiedDataName());param(params,"character",r.literal());}
        else if(c instanceof CobolParser.RecordDelimiterClauseContext r){kind=Ast.FileAuxKind.RECORD_DELIMITER;params.add(new Ast.FileAuxParameter("delimiter",r.STANDARD_1()!=null?"STANDARD_1":r.IMPLICIT()!=null?"IMPLICIT":text.apply(r.assignmentName())));}
        else if(c instanceof CobolParser.PasswordClauseContext r){kind=Ast.FileAuxKind.PASSWORD;ref(refs,"password",r.dataName());}
        else if(c instanceof CobolParser.BlockContainsClauseContext r){kind=Ast.FileAuxKind.BLOCK;param(params,"minimum",r.integerLiteral());if(r.blockContainsTo()!=null)param(params,"maximum",r.blockContainsTo().integerLiteral());params.add(new Ast.FileAuxParameter("unit",r.CHARACTERS()!=null?"CHARACTERS":"RECORDS"));}
        else if(c instanceof CobolParser.RecordContainsClauseContext r){kind=Ast.FileAuxKind.RECORD;params.add(new Ast.FileAuxParameter("form",r.recordContainsClauseFormat1()!=null?"FIXED":r.recordContainsClauseFormat2()!=null?"VARYING":"RANGE"));
            if(r.recordContainsClauseFormat1()!=null)param(params,"minimum",r.recordContainsClauseFormat1().integerLiteral());
            if(r.recordContainsClauseFormat2()!=null){var f=r.recordContainsClauseFormat2();param(params,"minimum",f.integerLiteral());if(f.recordContainsTo()!=null)param(params,"maximum",f.recordContainsTo().integerLiteral());ref(refs,"depending",f.qualifiedDataName());}
            if(r.recordContainsClauseFormat3()!=null){var f=r.recordContainsClauseFormat3();param(params,"minimum",f.integerLiteral());param(params,"maximum",f.recordContainsTo().integerLiteral());}}
        else if(c instanceof CobolParser.LabelRecordsClauseContext r){kind=Ast.FileAuxKind.LABEL_RECORDS;params.add(new Ast.FileAuxParameter("labels",r.STANDARD()!=null?"STANDARD":r.OMITTED()!=null?"OMITTED":"USER"));for(var n:r.dataName())param(params,"record",n);}
        else if(c instanceof CobolParser.ValueOfClauseContext r){kind=Ast.FileAuxKind.VALUE_OF;for(var v:r.valuePair()){param(params,"item",v.systemName());param(params,"value",v.literal()!=null?v.literal():v.qualifiedDataName());}}
        else if(c instanceof CobolParser.DataRecordsClauseContext r){kind=Ast.FileAuxKind.DATA_RECORDS;for(var n:r.dataName())param(params,"record",n);}
        else if(c instanceof CobolParser.LinageClauseContext r){kind=Ast.FileAuxKind.LINAGE;ref(refs,"page-body",r.dataName());param(params,"page-body",r.integerLiteral());for(var a:r.linageAt()){
            if(a.linageFootingAt()!=null){var v=a.linageFootingAt();ref(refs,"footing",v.dataName());param(params,"footing",v.integerLiteral());}
            if(a.linageLinesAtTop()!=null){var v=a.linageLinesAtTop();ref(refs,"top",v.dataName());param(params,"top",v.integerLiteral());}
            if(a.linageLinesAtBottom()!=null){var v=a.linageLinesAtBottom();ref(refs,"bottom",v.dataName());param(params,"bottom",v.integerLiteral());}}}
        else if(c instanceof CobolParser.RecordingModeClauseContext r){kind=Ast.FileAuxKind.RECORDING_MODE;param(params,"mode",r.modeStatement());}
        else if(c instanceof CobolParser.CodeSetClauseContext r){kind=Ast.FileAuxKind.CODE_SET;param(params,"alphabet",r.alphabetName());}
        else if(c instanceof CobolParser.ReportClauseContext r){kind=Ast.FileAuxKind.REPORT;for(var n:r.reportName())param(params,"report",n);}
        else return null;
        return new Ast.FileAuxiliary(ownMeta,kind,files,refs,params,checkpoint,trigger);
    }
    private void file(List<Ast.FileReference> out,ParserRuleContext c){if(c!=null)out.add(new Ast.FileReference(meta.apply(c),text.apply(c).strip().toUpperCase(Locale.ROOT),text.apply(c).strip()));}
    private void ref(List<Ast.FileAuxData> out,String role,ParserRuleContext c){if(c!=null)out.add(new Ast.FileAuxData(role,data.apply(c)));}
    private void param(List<Ast.FileAuxParameter> out,String role,ParserRuleContext c){if(c!=null)out.add(new Ast.FileAuxParameter(role,text.apply(c).strip()));}
}
