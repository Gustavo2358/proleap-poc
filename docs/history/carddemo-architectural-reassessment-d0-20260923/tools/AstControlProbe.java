package io.github.gustavo2358.cobolexplorer;
import org.antlr.v4.runtime.*;import org.antlr.v4.runtime.tree.*;import java.nio.file.*;import java.util.*;import com.fasterxml.jackson.databind.ObjectMapper;
public class AstControlProbe {
 static int index(ParseTree t,IdentityHashMap<ParseTree,Integer> ids,IdentityHashMap<ParseTree,Integer> sizes){ids.put(t,ids.size());int n=1;for(int i=0;i<t.getChildCount();i++)n+=index(t.getChild(i),ids,sizes);sizes.put(t,n);return n;}
 static void walk(Ast.Node n,Ast.Division d,List<Object> rows){
  if(n instanceof Ast.Statement){var r=new LinkedHashMap<String,Object>();r.put("id",n.meta().id());r.put("type",n.getClass().getSimpleName());r.put("source",n.meta().provenance());r.put("paragraphLocalNext",d.normalContinuations().get(n.meta().id()));r.put("ordinaryNext",d.ordinaryContinuations().get(n.meta().id()));r.put("normalCompletionRecognized",d.normalCompletionStatements().contains(n.meta().id()));rows.add(r);}
  for(var c:Ast.children(n))walk(c,d,rows);
 }
 public static void main(String[] args)throws Exception{
  var source=Path.of(args[0]);var binding=Bindings.cobol();var norm=SourceNormalizer.normalize(Files.readString(source),source.getFileName().toString(),new SourceNormalizer.Options(SourceNormalizer.SourceFormat.FIXED,SourceNormalizer.DebugLinePolicy.EXCLUDE));
  var dirs=Arrays.stream(args[1].split(",")).map(Path::of).toList();var pre=new PreprocessorEngine(binding,new CopybookLibrary(dirs),SourceArtifactInventory.empty()).process(norm.sourceMap(),source.getFileName().toString());var ds=new ArrayList<Diagnostic>();
  var lexer=binding.cobolLexer(CharStreams.fromString(pre.text(),source.getFileName().toString()));lexer.removeErrorListeners();lexer.addErrorListener(new AntlrDiagnosticListener(binding.name(),Diagnostic.Phase.LEXER,source.getFileName().toString(),ds));var tokens=new CommonTokenStream(lexer);tokens.fill();tokens.seek(0);
  var parser=binding.cobolParser(tokens);parser.removeErrorListeners();parser.addErrorListener(new AntlrDiagnosticListener(binding.name(),Diagnostic.Phase.PARSER,source.getFileName().toString(),ds));var tree=binding.cobolStart(parser);var ids=new IdentityHashMap<ParseTree,Integer>();var sizes=new IdentityHashMap<ParseTree,Integer>();index(tree,ids,sizes);
  var build=new AstBuilder(parser,pre.text(),pre.sourceMap(),ids,sizes).buildCompilationUnit(tree,source.getFileName().toString());var out=new LinkedHashMap<String,Object>();out.put("source",source.toString());out.put("unresolvedCopies",pre.unresolved());out.put("preprocessorErrors",pre.errors());out.put("parserDiagnostics",ds.stream().map(Object::toString).toList());var rows=new ArrayList<Object>();
  for(var u:build.compilationUnit().programUnits())for(var d:u.program().divisions())if(d.divisionKind()==Ast.DivisionKind.PROCEDURE)walk(d,d,rows);out.put("statements",rows);
  var storage=new ArrayList<Object>();var components=StorageComponents.analyze(build);for(var u:build.compilationUnit().programUnits()){var c=components.unit(u.id());var roots=new ArrayList<Object>();for(var root:c.roots())roots.add(Map.of("astId",root.meta().id(),"name",root.name(),"level",root.level(),"parsedLevel",StorageComponents.level(root),"source",root.meta().provenance()));storage.add(Map.of("unit",u.id().toString(),"structureProven",c.structureProven(),"roots",roots));}out.put("storageComponents",storage);
  System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(out));
 }
}
