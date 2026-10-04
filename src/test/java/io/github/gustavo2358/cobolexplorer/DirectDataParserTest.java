package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DirectDataParserTest {
    record Parsed(CompilationUnitBuildResult ast, List<ExplorerMain.Node> nodes,
                  List<String> errors, DirectDataParser.Session session) { }
    private static Parsed parse(SourceMap source, boolean direct) {
        var binding = Bindings.cobol(); var errors = new ArrayList<String>();
        var listener = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?,?> r,Object symbol,int line,int column,String message,RecognitionException e) {
                errors.add(line + ":" + column + ":" + message);
            }
        };
        var lexer = binding.cobolLexer(CharStreams.fromString(source.text())); lexer.removeErrorListeners(); lexer.addErrorListener(listener);
        var tokens = new CommonTokenStream(lexer); tokens.fill();
        var parser = (CobolParser) binding.cobolParser(tokens); parser.removeErrorListeners(); parser.addErrorListener(listener);
        if (direct) parser.directDataSession = new DirectDataParser.Session();
        ParseTree tree = parser.startRule();
        var nodes = new ArrayList<ExplorerMain.Node>(); var ids = new IdentityHashMap<ParseTree,Integer>(); var sizes = new IdentityHashMap<ParseTree,Integer>();
        ExplorerMain.walk(tree,-1,0,parser,nodes,new TreeMap<>(),ids,sizes);
        var ast = new AstBuilder(parser,source.text(),source,ids,sizes,errors.isEmpty()).buildCompilationUnit(tree,"test.cbl");
        return new Parsed(ast,nodes,errors,parser.directDataSession);
    }
    private static String program(String data) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. LAB.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n" + data
                + "\nPROCEDURE DIVISION.\nMOVE 'X' TO ITEM.\nGOBACK.\nEND PROGRAM LAB.\n";
    }
    private static Parsed equal(String source, boolean accepted) {
        return equal(SourceMap.identity(source,"test.cbl"),accepted);
    }
    private static Parsed equal(SourceMap source, boolean accepted) {
        var base = parse(source,false); var direct = parse(source,true);
        assertEquals(base.errors(),direct.errors(),source.text());
        assertEquals(base.nodes(),direct.nodes(),"syntax origins differ: " + source.text());
        assertEquals(base.ast().compilationUnit().programUnits(),direct.ast().compilationUnit().programUnits(),"semantic AST/provenance differ");
        assertEquals(base.ast().coverageByProgramUnit(),direct.ast().coverageByProgramUnit(),"coverage differs");
        assertEquals(base.ast().diagnosticsByProgramUnit(),direct.ast().diagnosticsByProgramUnit(),"semantic diagnostics differ");
        if (accepted) assertTrue(direct.session().acceptedDivisions()>0,direct.session().fallbackReasons().toString());
        else assertEquals(0,direct.session().acceptedDivisions());
        return direct;
    }
    @Test void directlyBuildsHierarchyClausesVisibilityAndReferences() {
        var parsed = equal(program("01 WS-ROOT.\n05 ITEM PIC X(8) VALUE 'AB''C'.\n88 READY VALUE 'Y' 'N'.\n"
                + "05 WS-NUMBER PIC S9(8) COMP-3 VALUE ZERO.\n05 WS-OTHER REDEFINES ITEM PIC X(8).\n"
                + "77 WS-SINGLE GLOBAL EXTERNAL PIC X.\nLINKAGE SECTION.\n01 ARG USAGE IS POINTER."),true);
        assertEquals(7,parsed.session().entries());
        var program = parsed.ast().compilationUnit().programUnits().get(0).program();
        assertTrue(Ast.children(program).stream().anyMatch(n -> n instanceof Ast.Division d && d.divisionKind()==Ast.DivisionKind.DATA));
        assertTrue(parsed.ast().diagnosticsByProgramUnit().values().stream().flatMap(List::stream).anyMatch(d->d.code().equals("CONFLICTING_DECLARATION_VISIBILITY")));
    }
    @Test void pictureMatrixPreservesStructureAndRecovery() {
        String[] pictures={"X","X(10)","9","9(4)","S9(4)","9(3)V99","ZZZ9.99","+999","***9","$999","99/99","N(2)","X(2)X(3)","9 ( 2 )","9(0)","9()","(3)","9((3))","9(3","9)","X\nX","9,99","9:99"};
        for(String pic:pictures)for(String tail:List.of("", " VALUE ZERO", " USAGE DISPLAY", " COMP-3", " JUSTIFIED RIGHT"))
            equal(program("01 ITEM PIC " + pic + tail + "."),true);
        equal(program("01 ITEM PIC ."),false);
    }
    @Test void unsupportedFormsRollBackEntireDivision() {
        for (String data : List.of("01 ITEM OCCURS TABLE-SIZE(1) PIC X.",
                "01 ITEM PIC X.\n66 ALIAS RENAMES ITEM.", "01 ITEM PIC X.\nLOCAL-STORAGE SECTION.\nLD L."))
            equal(program(data), false);
    }
    @Test void namesAndValueClausesFollowTheGrammarAlternatives() {
        for (String data : List.of("01 BINARY PIC X.", "01 ITEM PIC X VALUE 'A' BINARY.",
                "01 ITEM PIC X VALUE NAME.", "01 ITEM VALUE 'A' USAGE DISPLAY.",
                "01 ITEM VALUE ALL 'A'.", "01 PIC X.", "01 ITEM PIC X.\n88 READY 'Y'."))
            equal(program(data), true);
    }
    @Test void occursBuildsBoundsQualifiedReferencesSortKeysAndIndexes() {
        for (String clause : List.of("OCCURS 3 TIMES", "OCCURS 0 TO 12 TIMES DEPENDING ON WS-COUNT OF WS-ROOT",
                "OCCURS WS-COUNT", "OCCURS 5 ASCENDING KEY IS WS-KEY INDEXED BY WS-IDX WS-IDY"))
            equal(program("01 WS-ROOT.\n05 WS-COUNT PIC 9.\n05 ITEM " + clause + " PIC X."), true);
    }
    @Test void fileDescriptionsPreserveRecordFactsAuxiliaryNodesAndDeclarationOrder() {
        for (String clause : List.of("", "RECORDING MODE IS F", "RECORD CONTAINS 80 CHARACTERS",
                "RECORD CONTAINS 10 TO 80 CHARACTERS", "RECORD IS VARYING IN SIZE FROM 10 TO 80 DEPENDING ON WS-LEN",
                "RECORD IS VARYING DEPENDING ON WS-LEN", "IS EXTERNAL GLOBAL")) {
            String source = program("01 WS-LEN PIC 99.").replace("WORKING-STORAGE SECTION.",
                    "FILE SECTION.\nFD DATA-FILE " + clause + ".\n01 FILE-REC PIC X(80).\nSD SORT-FILE.\n01 SORT-REC PIC X.\nWORKING-STORAGE SECTION.");
            equal(source, true);
        }
        equal(program("01 ITEM PIC X.").replace("WORKING-STORAGE SECTION.",
                "FILE SECTION.\nFD DATA-FILE.\nRECORDING MODE F.\nRECORD CONTAINS 80.\n01 FILE-REC PIC X(80).\nWORKING-STORAGE SECTION."), true);
    }
    @Test void sqlDeclarationsRemainOpaqueAndPreserveHierarchy() {
        equal(program("01 ITEM PIC X.\n*>EXECSQL EXEC SQL INCLUDE SQLCA END-EXEC\n.\n01 WS-NEXT PIC X."), true);
    }
    @Test void cobolWordTokenSetMatchesTheGrammar() throws Exception {
        String grammar = Files.readString(Path.of("src/main/antlr4/Cobol.g4"));
        String alternatives = grammar.split("\\ncobolWord\\n", 2)[1].split(";", 2)[0].replace(":", "");
        Set<Integer> expected = new HashSet<>();
        for (String word : alternatives.split("\\|")) expected.add(CobolParser.class.getField(word.strip()).getInt(null));
        var method = DirectDataParser.class.getDeclaredMethod("wordToken", int.class); method.setAccessible(true);
        for (int token = 1; token <= CobolParser.VOCABULARY.getMaxTokenType(); token++)
            assertEquals(expected.contains(token), method.invoke(null, token), CobolParser.VOCABULARY.getSymbolicName(token));
    }
    @Test void literalsRangesAndUnicodeKeepSourceCoordinates() {
        equal(program("01 ITEM PIC X(3) VALUE 'á😀'.\n88 READY VALUE 'A' THRU 'Z' ZERO 12 TRUE X'FF'."),true);
        equal(program("01 ITEM PIC X VALUE LOW-VALUES.\n01 WS-OTHER PIC 9 VALUE -12."),true);
        equal(program("01 ITEM PIC X." ).replace("\n","\r\n"),true);
    }
    @Test void malformedSuffixHasSameDiagnosticsAndPartialAst() {
        String source = program("01 ITEM PIC X.").replace("MOVE 'X' TO ITEM.","MOVE TO ITEM.");
        equal(source,true);
    }
    @Test void multipleProgramsAndSessionsDoNotShareResults() {
        equal(program("01 ITEM PIC X.") + program("01 ITEM PIC 9.").replace("LAB","OTHER"),true);
        equal(program("01 ITEM PIC X."),true);
        equal(program("01 ITEM OCCURS 2 TIMES PIC X."),true);
    }
    @Test void existingFixturesPreserveAstOrTheSameFailure() throws Exception {
        int directFiles = 0, baselineFailures = 0, compared = 0;
        var rows = new ArrayList<String>();
        try (var files = Files.walk(Path.of("src/test/resources"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".cbl")).sorted().toList()) {
                SourceNormalizer.Result normalized;
                try {
                    normalized = SourceNormalizer.normalize(Files.readString(file), file.toString(),
                            new SourceNormalizer.Options(SourceNormalizer.SourceFormat.FIXED, SourceNormalizer.DebugLinePolicy.EXCLUDE));
                } catch (IllegalArgumentException normalizationRejected) {
                    // Some fixtures deliberately test invalid indicators or another source format.
                    // They never reach either parser in this fixed-format corpus pass.
                    rows.add(file + "\tNORMALIZATION_REJECTED\t" + normalizationRejected.getMessage());
                    continue;
                }
                Parsed base = null, direct = null; RuntimeException baseFailure = null, directFailure = null;
                try { base = parse(normalized.sourceMap(), false); } catch (RuntimeException failure) { baseFailure = failure; }
                try { direct = parse(normalized.sourceMap(), true); } catch (RuntimeException failure) { directFailure = failure; }
                if (baseFailure != null || directFailure != null) {
                    assertNotNull(baseFailure, "new direct failure: " + file + ": " + directFailure);
                    assertNotNull(directFailure, "baseline failure disappeared: " + file);
                    assertEquals(baseFailure.getClass(), directFailure.getClass(), file.toString());
                    assertEquals(baseFailure.getMessage(), directFailure.getMessage(), file.toString());
                    baselineFailures++; rows.add(file + "\tBASELINE_FAILURE\t" + baseFailure); continue;
                }
                assertEquals(base.errors(), direct.errors(), file.toString());
                assertEquals(base.nodes(), direct.nodes(), "origins: " + file);
                assertEquals(base.ast().compilationUnit().programUnits(), direct.ast().compilationUnit().programUnits(), "AST: " + file);
                assertEquals(base.ast().coverageByProgramUnit(), direct.ast().coverageByProgramUnit(), "coverage: " + file);
                assertEquals(base.ast().diagnosticsByProgramUnit(), direct.ast().diagnosticsByProgramUnit(), "diagnostics: " + file);
                if (direct.session().acceptedDivisions() > 0) directFiles++;
                compared++; rows.add(file + "\tEQUAL\tdirectDivisions=" + direct.session().acceptedDivisions()
                        + "\terrors=" + direct.errors().size() + "\tfallback=" + direct.session().fallbackReasons());
            }
        }
        Files.createDirectories(Path.of("target/direct-data-lab"));
        Files.write(Path.of("target/direct-data-lab/fixtures.tsv"), rows);
        assertTrue(compared > 100, "differential corpus unexpectedly empty");
        assertTrue(directFiles > 30, "direct path must be exercised, not only fallback");
        System.out.println("DIRECT_DATA_CORPUS compared=" + compared + " directFiles=" + directFiles + " baselineFailures=" + baselineFailures);
    }

    @Test void adversarialLevelsAndClauseOrdersPreserveFallback() {
        for (String level : List.of("+01", "-1", "0", "50", "999999999999999999999999999"))
            equal(program(level + " ITEM PIC X."), false);
        for (String clause : List.of("VALUE 'A',", "PIC X EXTERNAL BY 'N'"))
            equal(program("01 ITEM " + clause + "."), false);
        equal(program("01 ITEM IS GLOBAL PIC X."), true);
        equal(program("01 ITEM BLANK WHEN ZERO PIC 9 SYNC RIGHT."), true);
    }

    @Test void sourceMapIncludesCopyAndSyntheticAuthority() throws Exception {
        Path source=Path.of("corpus/carddemo/cbl/COACCT01.cbl");
        var normalized=SourceNormalizer.normalize(Files.readString(source),source.getFileName().toString(),new SourceNormalizer.Options(SourceNormalizer.SourceFormat.FIXED,SourceNormalizer.DebugLinePolicy.EXCLUDE));
        var pre=new PreprocessorEngine(Bindings.cobol(),new CopybookLibrary(List.of(Path.of("corpus/carddemo/cpy"),Path.of("corpus/carddemo/cpy-bms"),Path.of("corpus/cpy"),Path.of("corpus/cpy-bms")))).process(normalized.sourceMap(),source.getFileName().toString());
        var result=equal(pre.sourceMap(),true);assertTrue(result.session().entries()>2000,"real MQ declarations must use the direct path");
    }
    @Test void conditionValuesAndOptionalFalseWordsHaveIdenticalDirectAst() {
        for(int mask=0;mask<16;mask++) {
            String clause=((mask&1)!=0?"WHEN ":"")+((mask&2)!=0?"SET ":"")+((mask&4)!=0?"TO ":"")+"FALSE "+((mask&8)!=0?"IS ":"")+"0";
            equal(program("01 ITEM PIC 9(9).\n88 READY VALUE 7 20 THRU 999999999 "+clause+"."),true);
        }
    }
}
