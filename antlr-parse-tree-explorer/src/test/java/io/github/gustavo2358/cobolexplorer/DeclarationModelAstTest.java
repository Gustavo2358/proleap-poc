package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DeclarationModelAstTest {
    @Test void modelsHierarchySpecialLevelsAndTypedClauses() throws Exception {
        Ast.Program ast = parse();
        Ast.Section working = sections(ast).stream().filter(s -> s.dataSectionKind() == Ast.DataSectionKind.WORKING_STORAGE).findFirst().orElseThrow();
        Ast.DataEntry root = (Ast.DataEntry) working.children().get(0);
        assertEquals(Ast.DataLevelKind.GROUP_OR_ELEMENTARY, root.levelKind());
        assertTrue(root.children().stream().anyMatch(Ast.DataEntry::filler));
        Ast.DataEntry base = child(root, "BASE-ITEM");
        assertInstanceOf(Ast.PictureClause.class, clause(base, Ast.PictureClause.class));
        assertInstanceOf(Ast.UsageClause.class, clause(base, Ast.UsageClause.class));
        assertEquals(1, ((Ast.ValueClause) clause(base, Ast.ValueClause.class)).values().size());
        Ast.RedefinesClause redefines = (Ast.RedefinesClause) clause(child(root, "REDEF-ITEM"), Ast.RedefinesClause.class);
        assertEquals("BASE-ITEM", redefines.target().baseName());
        Ast.OccursClause occurs = (Ast.OccursClause) clause(child(root, "TABLE-ITEM"), Ast.OccursClause.class);
        assertNotNull(occurs.dependingOn()); assertEquals("TABLE-IDX", occurs.indexes().get(0).indexName());
        Ast.DataEntry rename = child(root, "RANGE-ALIAS");
        assertEquals(Ast.DataLevelKind.RENAMES_66, rename.levelKind());
        assertNotNull(((Ast.RenamesClause) clause(rename, Ast.RenamesClause.class)).through());
        Ast.DataEntry tableCount = child(root, "TABLE-COUNT");
        assertEquals(Ast.DataLevelKind.CONDITION_88, child(tableCount, "VALID-COUNT").levelKind());
        Ast.PreservedDataClause preserved = (Ast.PreservedDataClause)
                clause(tableCount, Ast.PreservedDataClause.class);
        assertEquals("dataBlankWhenZeroClause", preserved.grammarRule());
        assertEquals("BLANK WHEN ZERO", preserved.writtenText());
        assertEquals(Ast.DataLevelKind.STANDALONE_77,
                working.children().stream().map(Ast.DataEntry.class::cast)
                        .filter(e -> e.name().equals("STANDALONE")).findFirst().orElseThrow().levelKind());
        assertTrue(sections(ast).stream().anyMatch(s -> s.dataSectionKind() == Ast.DataSectionKind.LINKAGE));
    }

    @Test void modelsProcedureSignatureWithoutCreatingParameterDeclarations() throws Exception {
        Ast.Program ast = parse();
        Ast.ProcedureSignature signature = nodes(ast, Ast.ProcedureSignature.class).get(0);
        assertEquals(List.of(Ast.PassingMode.REFERENCE, Ast.PassingMode.VALUE, Ast.PassingMode.VALUE),
                signature.parameters().stream().map(Ast.ProcedureParameter::passingMode).toList());
        assertTrue(signature.parameters().get(0).optional());
        assertTrue(signature.parameters().get(2).any());
        assertEquals("TABLE-COUNT", signature.returning().baseName());
        SymbolTable table = new SymbolTableBuilder().build(ast);
        assertEquals(1, table.lookupAll(SymbolTable.Namespace.DATA, "LINK-PARAM").size());
        assertEquals(1, table.symbols().stream().filter(s -> s.kind() == SymbolTable.SymbolKind.RENAMES).count());
        assertEquals(1, table.symbols().stream().filter(s -> s.kind() == SymbolTable.SymbolKind.INDEX_NAME).count());
        SymbolTable.Symbol rename = table.lookupAll(SymbolTable.Namespace.DATA, "RANGE-ALIAS").get(0);
        assertEquals("BASE-ITEM", rename.attributes().get("renamesFrom"));
        assertEquals("TABLE-COUNT", rename.attributes().get("renamesThrough"));
        assertEquals("NOT_PERFORMED", rename.attributes().get("relationBinding"));
        AstSnapshot snapshot = AstSnapshot.from(ast);
        assertTrue(snapshot.nodes().stream().anyMatch(n -> n.type().equals("ProcedureSignature")
                && n.attributes().get("writtenText").contains("USING")));
        assertTrue(snapshot.nodes().stream().anyMatch(n -> n.type().equals("OccursClause")
                && n.attributes().get("writtenText").contains("INDEXED BY")));
    }

    private static Ast.DataEntry child(Ast.DataEntry parent, String name) { return parent.children().stream().filter(e -> e.name().equals(name)).findFirst().orElseThrow(); }
    private static Ast.DataClause clause(Ast.DataEntry entry, Class<? extends Ast.DataClause> type) { return entry.clauses().stream().filter(type::isInstance).findFirst().orElseThrow(); }
    private static List<Ast.Section> sections(Ast.Program ast) { return nodes(ast, Ast.Section.class).stream().filter(s -> s.dataSectionKind() != null).toList(); }
    private static Ast.Program parse() throws Exception {
        Path file=Path.of("src/test/resources/cobol/semantic/declarations.cbl").toAbsolutePath(); String source=SourceNormalizerTestSupport.fixed(Files.readString(file,StandardCharsets.UTF_8));
        GrammarBinding b=Bindings.cobol(); Parser p=b.cobolParser(new CommonTokenStream(b.cobolLexer(CharStreams.fromString(source)))); ParseTree t=b.cobolStart(p); assertEquals(0,p.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree,Integer> ids=new IdentityHashMap<>(), sizes=new IdentityHashMap<>(); index(t,ids,sizes,new int[]{0}); return new AstBuilder(p,source,ids,sizes).build(t).program();
    }
    private static int index(ParseTree t,IdentityHashMap<ParseTree,Integer> i,IdentityHashMap<ParseTree,Integer>s,int[]n){i.put(t,n[0]++);int z=1;for(int x=0;x<t.getChildCount();x++)z+=index(t.getChild(x),i,s,n);s.put(t,z);return z;}
    private static <T extends Ast.Node> List<T> nodes(Ast.Node r,Class<T>t){List<T>o=new ArrayList<>();if(t.isInstance(r))o.add(t.cast(r));for(Ast.Node c:Ast.children(r))o.addAll(nodes(c,t));return o;}
}
