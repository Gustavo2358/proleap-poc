package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Discovery-only characterization for WORK-COND-004 (Slice 4): these tests prove FACTS about
 * the current grammar shape, the qualifier-namespace ambiguity of the inData branch, the
 * current information loss in the surface AST, the current provenance granularity and the
 * local/GLOBAL qualified-reference collision across program units (IBM resolution-of-names
 * step 3 gap, BACKLOG-RES-004). They deliberately do not codify the proposed implementation
 * as if it already existed.
 */
class ConditionNameSurfaceDiscoveryTest {

    @Test
    void grammarPlacesSubscriptsAfterQualifiersAndRejectsOtherOrders() {
        assertParse("FLAG-OK OF CUSTOMER(I)", 0);
        ParseRecord record = parse(program("SUB-AFTER-QUAL", "FLAG-88 OF CUSTOMER(I)"), "sub-after-qual.cbl");
        assertEquals(0, record.syntaxErrors());
        List<CobolParser.InDataContext> inData = AstBoundaryTestSupport.contexts(
                record.tree(), CobolParser.InDataContext.class);
        List<CobolParser.ConditionNameSubscriptReferenceContext> subscripts = AstBoundaryTestSupport.contexts(
                record.tree(), CobolParser.ConditionNameSubscriptReferenceContext.class);
        assertEquals(1, inData.size(), "one IN/OF qualifier before the subscript");
        assertEquals(1, subscripts.size(), "the trailing subscript list belongs to the reference");
        assertTrue(inData.get(0).getToken(CobolParser.OF, 0) != null, "written connector is OF");
        assertEquals("CUSTOMER", inData.get(0).dataName().getText());
        assertTrue(inData.get(0).getStop().getTokenIndex() < subscripts.get(0).getStart().getTokenIndex(),
                "qualification is written before the subscript list");

        // Subscript written before the qualifier, and qualifier after the subscript, are
        // grammar-rejected: the reference is conditionName (inData* inFile? subscriptRef*).
        assertParse("FLAG-OK(I) OF CUSTOMER", true);
        assertParse("FLAG-OK OF CUSTOMER(I) OF MASTER", true);

        // Multiple subscripts inside one parenthesized list; comma is optional in the grammar.
        assertParse("FLAG-88(I, J)", 0);
        ParseRecord two = parse(program("TWO-SUBSCRIPTS", "FLAG-88(I J)"), "two-subscripts.cbl");
        assertEquals(0, two.syntaxErrors());
        List<CobolParser.SubscriptContext> subscriptContexts = AstBoundaryTestSupport.contexts(
                two.tree(), CobolParser.SubscriptContext.class);
        assertEquals(2, subscriptContexts.size());

        // Grammar-only acceptance: two separate subscript lists, and subscript ALL. Neither is
        // claimed as valid COBOL; both remain surface-preserved shapes (COND-N05 class).
        assertParse("FLAG-88(I)(J)", 0);
        assertParse("FLAG-88(ALL)", 0);
    }

    @Test
    void subscriptNameParsesAsQualifiedDataNameInsideConditionNameReference() {
        ParseRecord record = parse(program("SUBSCRIPT-RULE", "FLAG-OK(IDX)"), "subscript-rule.cbl");
        assertEquals(0, record.syntaxErrors());
        List<CobolParser.QualifiedDataNameContext> qdn = AstBoundaryTestSupport.contexts(
                record.tree(), CobolParser.QualifiedDataNameContext.class);
        assertEquals(1, qdn.size(), "the subscript name parses as qualifiedDataName");
        assertEquals("IDX", qdn.get(0).getText());
        // This parse fact is what makes the current lowering pick the subscript's name as the
        // reference base: the first qualifiedDataName DESCENDANT of conditionNameReference.
    }

    @Test
    void anyBareNominalInSimpleConditionPositionParsesAsConditionNameReference() {
        ParseRecord table = parse(program("SIMPLE-TABLE", "TBL(I)"), "simple-table.cbl");
        ParseRecord qualifiedTable = parse(program("SIMPLE-QUALIFIED-TABLE", "ELEM OF GRP-TBL(I)"), "simple-qualified-table.cbl");
        ParseRecord plainQualified = parse(program("SIMPLE-QUALIFIED", "DATA-X OF GROUP-A"), "simple-qualified.cbl");
        assertEquals(0, table.syntaxErrors());
        assertEquals(0, qualifiedTable.syntaxErrors());
        assertEquals(0, plainQualified.syntaxErrors());
        for (ParseRecord record : List.of(table, qualifiedTable, plainQualified)) {
            assertEquals(1, AstBoundaryTestSupport.contexts(
                    record.tree(), CobolParser.ConditionNameReferenceContext.class).size(),
                    "simple condition swallows the bare nominal via conditionNameReference");
        }
        assertTrue(AstBoundaryTestSupport.contexts(table.tree(), CobolParser.TableCallContext.class).isEmpty(),
                "no tableCall branch in simple-condition position");
        assertTrue(AstBoundaryTestSupport.contexts(
                plainQualified.tree(), CobolParser.IdentifierContext.class).isEmpty(),
                "no identifier branch in simple-condition position");

        // The same written shapes in relation-operand position use the data-name branches.
        ParseRecord subject = parse(program("REL-SUBJECT", "TBL(I) = X"), "rel-subject.cbl");
        ParseRecord qualifiedSubject = parse(program("REL-QUALIFIED", "ELEM OF GRP-TBL(I) = X"), "rel-qualified.cbl");
        assertEquals(0, subject.syntaxErrors());
        assertEquals(0, qualifiedSubject.syntaxErrors());
        assertTrue(AstBoundaryTestSupport.contexts(subject.tree(), CobolParser.TableCallContext.class).size() >= 1);
        assertTrue(AstBoundaryTestSupport.contexts(subject.tree(), CobolParser.ConditionNameReferenceContext.class)
                .isEmpty());
        assertEquals(1, AstBoundaryTestSupport.contexts(
                qualifiedSubject.tree(), CobolParser.InTableContext.class).size(),
                "the subscript attaches to the QUALIFIER (inTable) in the data branch");
    }

    @Test
    void fileAndMnemonicQualifierBranchesAreShadowedByInData() {
        ParseRecord file = parse(program("FILE-QUAL", "FLAG-OK IN FILE-A"), "file-qual.cbl");
        ParseRecord mnemonic = parse(program("MNEMONIC-QUAL", "FLAG-OK IN MNEMONIC-A"), "mnemonic-qual.cbl");
        assertEquals(0, file.syntaxErrors());
        assertEquals(0, mnemonic.syntaxErrors());
        for (ParseRecord record : List.of(file, mnemonic)) {
            assertEquals(1, AstBoundaryTestSupport.contexts(record.tree(), CobolParser.InDataContext.class).size(),
                    "the qualifier parses via inData");
            assertTrue(AstBoundaryTestSupport.contexts(record.tree(), CobolParser.InFileContext.class).isEmpty(),
                    "inFile branch is unreachable for cobolWord tokens");
            assertTrue(AstBoundaryTestSupport.contexts(record.tree(), CobolParser.InMnemonicContext.class).isEmpty(),
                    "inMnemonic branch is unreachable for cobolWord tokens");
        }
    }

    @Test
    void currentAstPreservesQualifiersOnlyWhileSubscriptsAreAbsent() {
        AstBoundaryTestSupport.Analysis nested = AstBoundaryTestSupport.analyze(
                program("AST-NESTED-QUAL", "FLAG-OK OF SUB-GRP OF GROUP-A"), "ast-nested-qual.cbl");
        Ast.IfStatement statement = AstBoundaryTestSupport.nodes(nested, Ast.IfStatement.class).get(0);
        Ast.DataReference reference = assertInstanceOf(Ast.DataReference.class, statement.condition());
        assertEquals("FLAG-OK", reference.baseName());
        assertEquals(List.of("SUB-GRP", "GROUP-A"), reference.qualifiers().stream()
                .map(Ast.DataQualifier::name).toList(), "written qualifier order preserved");
        assertEquals(List.of(Ast.QualifierConnector.OF, Ast.QualifierConnector.OF),
                reference.qualifiers().stream().map(Ast.DataQualifier::connector).toList());
        assertEquals("FLAG-OK OF SUB-GRP OF GROUP-A", reference.writtenText());
    }

    @Test
    void currentAstLosesNameQualifiersAndSubscriptsWhenSubscriptsAreWritten() {
        AstBoundaryTestSupport.Analysis simple = AstBoundaryTestSupport.analyze(
                program("AST-SUBSCRIPT", "FLAG-OK(I)"), "ast-subscript.cbl");
        Ast.DataReference simpleReference = assertInstanceOf(Ast.DataReference.class,
                AstBoundaryTestSupport.nodes(simple, Ast.IfStatement.class).get(0).condition());
        assertAll("FLAG-OK(I) is structurally corrupted today",
                () -> assertEquals("I", simpleReference.baseName(),
                        "the reference base is hijacked by the subscript's name"),
                () -> assertTrue(simpleReference.subscriptGroups().isEmpty(),
                        "the written subscript does not survive structurally"),
                () -> assertEquals("FLAG-OK(I)", simpleReference.writtenText(),
                        "only writtenText retains the written surface"));

        AstBoundaryTestSupport.Analysis qualified = AstBoundaryTestSupport.analyze(
                program("AST-QUALIFIED-SUBSCRIPT", "FLAG-OK OF CUSTOMER(I, J)"), "ast-qualified-subscript.cbl");
        Ast.DataReference qualifiedReference = assertInstanceOf(Ast.DataReference.class,
                AstBoundaryTestSupport.nodes(qualified, Ast.IfStatement.class).get(0).condition());
        assertAll("FLAG-OK OF CUSTOMER(I, J) loses name, qualification and subscripts",
                () -> assertEquals("I", qualifiedReference.baseName()),
                () -> assertTrue(qualifiedReference.qualifiers().isEmpty()),
                () -> assertTrue(qualifiedReference.subscriptGroups().isEmpty()),
                () -> assertEquals("FLAG-OK OF CUSTOMER(I, J)", qualifiedReference.writtenText()));
    }

    @Test
    void contextualTailCarriesTheSameStructuralCorruption() {
        AstBoundaryTestSupport.Analysis qualifiedTail = AstBoundaryTestSupport.analyze(
                program("TAIL-QUALIFIED", "A = B OR C OF GROUP-A"), "tail-qualified.cbl");
        Ast.ContextualConditionTail tail = (Ast.ContextualConditionTail)
                AstBoundaryTestSupport.nodes(qualifiedTail, Ast.ContextualConditionTail.class).get(0);
        assertEquals("C", tail.nominalReference().baseName());
        assertEquals(List.of("GROUP-A"), tail.nominalReference().qualifiers().stream()
                .map(Ast.DataQualifier::name).toList());

        AstBoundaryTestSupport.Analysis subscriptTail = AstBoundaryTestSupport.analyze(
                program("TAIL-SUBSCRIPT", "A = B OR FLAG-ON(IDX)"), "tail-subscript.cbl");
        Ast.ContextualConditionTail corrupt = (Ast.ContextualConditionTail)
                AstBoundaryTestSupport.nodes(subscriptTail, Ast.ContextualConditionTail.class).get(0);
        assertAll("tail inner reference shares the subscript corruption",
                () -> assertEquals("IDX", corrupt.nominalReference().baseName()),
                () -> assertTrue(corrupt.nominalReference().subscriptGroups().isEmpty()),
                () -> assertEquals("FLAG-ON(IDX)", corrupt.writtenText()));
    }

    @Test
    void standaloneConditionNameSurfaceIsTypedAsDataReferenceToday() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("AST-PLAIN", "FLAG-OK"), "ast-plain.cbl");
        Ast.Expression condition = AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).get(0).condition();
        assertInstanceOf(Ast.DataReference.class, condition);
        assertEquals("conditionNameReference", condition.meta().origin().grammarRule(),
                "today the ONLY structural evidence of the condition-name surface is the grammar rule");
    }

    @Test
    void collectorKeysConditionKindOnGrammarRuleToday() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("COLLECTOR-PLAIN", "C"), "collector-plain.cbl");
        ReferenceResolution.Entry entry = analysis.resolution().entries().stream()
                .filter(item -> item.occurrence().writtenText().equals("C")).findFirst().orElseThrow();
        assertAll("boundary: the collector still closes the kind by grammarRule (false gap, Slice 5)",
                () -> assertEquals("conditionNameReference", entry.occurrence().grammarRule()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, entry.occurrence().kind()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        entry.reason()),
                () -> assertTrue(entry.candidates().isEmpty()));
    }

    @Test
    void evaluateAndSetContextsAlreadyBuildStructurallyCompleteReferences() {
        AstBoundaryTestSupport.Analysis evaluate = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. EVAL-BOUNDARY.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 T OCCURS 2 TIMES.
                   05 FLAG PIC X.
                      88 FLAG-OK VALUE 'Y'.
                01 I PIC 99.
                PROCEDURE DIVISION.
                    EVALUATE TRUE
                       WHEN FLAG-OK(I) CONTINUE
                    END-EVALUATE.
                END PROGRAM EVAL-BOUNDARY.
                """, "eval-boundary.cbl");
        Ast.DataReference selector = (Ast.DataReference) AstBoundaryTestSupport.nodes(evaluate, Ast.EvaluateStatement.class)
                .get(0).branches().get(0).selectors().get(0).expression();
        assertAll("EVALUATE selector path (identifier/tableCall) is already structurally complete",
                () -> assertEquals("FLAG-OK", selector.baseName()),
                () -> assertEquals(1, selector.subscriptGroups().size()));

        AstBoundaryTestSupport.Analysis set = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SET-BOUNDARY.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 GROUP-A.
                   05 FLAG PIC X.
                      88 FLAG-OK VALUE 'Y'.
                PROCEDURE DIVISION.
                    SET FLAG-OK OF GROUP-A TO TRUE.
                END PROGRAM SET-BOUNDARY.
                """, "set-boundary.cbl");
        Ast.DataReference target = AstBoundaryTestSupport.nodes(set, Ast.DataReference.class).stream()
                .filter(reference -> reference.baseName().equals("FLAG-OK")).findFirst().orElseThrow();
        assertAll("SET target path (qualifiedDataName) is already structurally complete",
                () -> assertEquals(1, target.qualifiers().size()),
                () -> assertEquals("GROUP-A", target.qualifiers().get(0).name()));
    }

    @Test
    void nestedQualifierInsideSubscriptBelongsToTheSubscriptNotTheReference() {
        ParseRecord record = parse(program("SUBSCRIPT-QUALIFIED", "FLAG-OK OF CUSTOMER(SUB OF SUB-GROUP)"),
                "subscript-qualified.cbl");
        assertEquals(0, record.syntaxErrors());
        List<CobolParser.InDataContext> inData = AstBoundaryTestSupport.contexts(
                record.tree(), CobolParser.InDataContext.class);
        assertEquals(2, inData.size(), "one IN/OF qualifier at the reference root and one inside the subscript");
        List<CobolParser.ConditionNameSubscriptReferenceContext> subscripts = AstBoundaryTestSupport.contexts(
                record.tree(), CobolParser.ConditionNameSubscriptReferenceContext.class);
        assertEquals(1, subscripts.size());
        assertEquals(1, inData.stream().filter(qualifier -> hasAncestor(qualifier,
                CobolParser.ConditionNameSubscriptReferenceContext.class)).count(),
                "exactly one qualifier lives inside the subscript subtree");
        assertEquals(1, inData.stream().filter(qualifier -> !hasAncestor(qualifier,
                CobolParser.ConditionNameSubscriptReferenceContext.class)).count(),
                "the reference root keeps exactly one qualifier");
        assertEquals(1, AstBoundaryTestSupport.contexts(
                subscripts.get(0), CobolParser.QualifiedDataNameContext.class).size(),
                "the subscript is itself a qualified data name");

        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("AST-SUBSCRIPT-QUALIFIED", "FLAG-OK OF CUSTOMER(SUB OF SUB-GROUP)"),
                "ast-subscript-qualified.cbl");
        Ast.DataReference reference = assertInstanceOf(Ast.DataReference.class,
                AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).get(0).condition());
        assertAll("today the nested qualifier is stolen into the root reference",
                () -> assertEquals("SUB", reference.baseName(),
                        "the root base is hijacked by the subscript's name"),
                () -> assertEquals(List.of("SUB-GROUP"), reference.qualifiers().stream()
                        .map(Ast.DataQualifier::name).toList(),
                        "the subscript's own qualifier is stolen and the root qualifier CUSTOMER is lost"),
                () -> assertTrue(reference.subscriptGroups().isEmpty()),
                () -> assertEquals("FLAG-OK OF CUSTOMER(SUB OF SUB-GROUP)", reference.writtenText()));
    }

    @Test
    void dataQualifierBranchCanCarryAFileDeclaration() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. D-FILE-QUAL.
                ENVIRONMENT DIVISION.
                INPUT-OUTPUT SECTION.
                FILE-CONTROL.
                    SELECT CUSTOMER-FILE ASSIGN TO 'CUSTDD'.
                DATA DIVISION.
                FILE SECTION.
                FD CUSTOMER-FILE.
                01 CUSTOMER-REC.
                   05 CUST-STATUS PIC X.
                      88 FLAG-OK VALUE 'A'.
                PROCEDURE DIVISION.
                    IF FLAG-OK OF CUSTOMER-FILE CONTINUE END-IF.
                END PROGRAM D-FILE-QUAL.
                """, "file-qual.cbl");
        assertTrue(AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.InDataContext.class).stream()
                        .anyMatch(context -> context.dataName().getText().equals("CUSTOMER-FILE")),
                "the file-name qualifier parses through the inData branch");
        assertTrue(AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.InFileContext.class).isEmpty(),
                "inFile is shadowed even when the qualifier is a declared file-name");
        assertTrue(AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.InMnemonicContext.class).isEmpty(),
                "inMnemonic is shadowed for cobolWord qualifiers");
        assertTrue(analysis.tables().units().stream()
                        .flatMap(unit -> unit.symbolTable().symbols().stream())
                        .anyMatch(symbol -> symbol.canonicalName().equals("CUSTOMER-FILE")
                                && symbol.namespace() == SymbolTable.Namespace.FILE),
                "the same written token category denotes a FILE declaration");

        Ast.DataReference reference = assertInstanceOf(Ast.DataReference.class,
                AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).get(0).condition());
        assertAll("today the surface closes DATA where the parse tree cannot classify",
                () -> assertEquals("FLAG-OK", reference.baseName()),
                () -> assertEquals(List.of(Ast.QualifierTarget.DATA),
                        reference.qualifiers().stream().map(Ast.DataQualifier::target).toList(),
                        "inData is closed as DATA even though the declaration is a file-name"),
                () -> assertEquals("CUSTOMER-FILE", reference.qualifiers().get(0).name()));
        ReferenceResolution.Entry condition = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().writtenText().equals("FLAG-OK OF CUSTOMER-FILE"))
                .findFirst().orElseThrow();
        assertAll("today's resolver cannot qualify through a file-name because the surface invented DATA",
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, condition.occurrence().kind()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, condition.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, condition.reason()));
    }

    @Test
    void provenanceGranularityHasNoIndependentBaseNameNode() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("AST-PROVENANCE", "FLAG-OK OF SUB-GRP OF GROUP-A"), "ast-provenance.cbl");
        Ast.DataReference reference = assertInstanceOf(Ast.DataReference.class,
                AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).get(0).condition());
        CobolParser.ConditionNameContext name = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.ConditionNameContext.class).get(0);
        int nameStart = name.getStart().getTokenIndex();
        int nameStop = name.getStop().getTokenIndex();
        assertAll("current surface granularity matches contract A (no independent base-name node)",
                () -> assertEquals(2, reference.qualifiers().size()),
                () -> assertTrue(reference.qualifiers().stream().allMatch(qualifier ->
                        qualifier.meta().id() != reference.meta().id()
                                && qualifier.meta().span().startToken() > nameStop),
                        "each written qualification owns a distinct meta after the base name"),
                () -> assertTrue(reference.qualifiers().stream().allMatch(qualifier ->
                        qualifier.reference().meta().span().startToken()
                                > qualifier.meta().span().startToken()),
                        "the qualifier payload span starts after the written connector"),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis).stream()
                                .noneMatch(node -> node.meta().span().startToken() == nameStart
                                        && node.meta().span().endToken() == nameStop),
                        "no AST node owns exactly the base-name token range: the name has no Meta of its own"));
    }

    @Test
    void qualifiedLocalDataNameCollidesWithOuterGlobalFileNameAcrossPrograms() {
        // FACT-R2-01..05: local condition-name qualified by DATA in the contained program; outer
        // GLOBAL condition-name qualified by FILE; the same qualifier spelling across units; the
        // current resolver behavior; and the structural facts that decide whether widening
        // DATA -> DATA_OR_FILE would add a candidate.
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. D-OUTER-COLLISION.
                ENVIRONMENT DIVISION.
                INPUT-OUTPUT SECTION.
                FILE-CONTROL.
                    SELECT Q ASSIGN TO 'QDD'.
                DATA DIVISION.
                FILE SECTION.
                FD Q IS GLOBAL.
                01 OUTER-REC.
                   05 CUST-STATUS PIC X.
                      88 C VALUE 'Y'.
                PROCEDURE DIVISION.
                    GOBACK.
                IDENTIFICATION DIVISION.
                PROGRAM-ID. D-INNER-COLLISION.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 Q.
                   05 CUST-STATUS PIC X.
                      88 C VALUE 'Y'.
                01 X PIC X.
                PROCEDURE DIVISION.
                    IF C OF Q CONTINUE END-IF.
                    MOVE CUST-STATUS OF Q TO X.
                END PROGRAM D-INNER-COLLISION.
                END PROGRAM D-OUTER-COLLISION.
                """, "qualified-collision.cbl");
        CompilationUnitSymbolTables.UnitSymbols outer = unit(analysis, "D-OUTER-COLLISION");
        CompilationUnitSymbolTables.UnitSymbols inner = unit(analysis, "D-INNER-COLLISION");
        SymbolTable.Symbol outerQ = symbolOfKind(outer, "Q", SymbolTable.SymbolKind.FILE_DESCRIPTION);
        SymbolTable.Symbol outerC = symbolOfKind(outer, "C", SymbolTable.SymbolKind.CONDITION_NAME);
        SymbolTable.Symbol innerQ = symbolOfKind(inner, "Q", SymbolTable.SymbolKind.DATA_ITEM);
        SymbolTable.Symbol innerC = symbolOfKind(inner, "C", SymbolTable.SymbolKind.CONDITION_NAME);
        assertAll("FACT-R2-01/02/03: declarations, namespaces and visibility across program units",
                () -> assertEquals(SymbolTable.Namespace.FILE, outerQ.namespace()),
                () -> assertEquals("GLOBAL", outerQ.attributes().get("visibility")),
                () -> assertEquals("GLOBAL", outerC.attributes().get("visibility"),
                        "a condition-name subordinate to a GLOBAL file description is global"),
                () -> assertEquals(SymbolTable.Namespace.DATA, innerQ.namespace()),
                () -> assertEquals("LOCAL", innerQ.attributes().getOrDefault("visibility", "LOCAL")),
                () -> assertEquals("LOCAL", innerC.attributes().getOrDefault("visibility", "LOCAL")));

        // FACT-R2-05: the ancestry namespaces prove that widening the qualifier constraint from
        // {DATA} to {DATA, FILE} would admit the outer candidate too (both ancestries contain Q).
        assertTrue(ancestryNames(outer, outerC).contains(new AncestryName("Q", SymbolTable.Namespace.FILE)),
                "the outer C is qualified, in its ancestry, by the FILE named Q");
        assertTrue(ancestryNames(inner, innerC).contains(new AncestryName("Q", SymbolTable.Namespace.DATA)),
                "the inner C is qualified, in its ancestry, by the DATA named Q");

        Ast.DataReference reference = assertInstanceOf(Ast.DataReference.class,
                AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).get(0).condition());
        assertAll("the condition surface still closes DATA today",
                () -> assertEquals("C", reference.baseName()),
                () -> assertEquals(List.of(Ast.QualifierTarget.DATA),
                        reference.qualifiers().stream().map(Ast.DataQualifier::target).toList(),
                        "today the qualifier target invents DATA; DATA_OR_FILE would widen the constraint"));

        // FACT-R2-04: current resolver behavior for the qualified condition-name reference.
        // After-widening model (documental, no production change): with DATA_OR_FILE the constraint
        // becomes {DATA, FILE}, both C candidates survive applyQualification (both ancestries contain Q),
        // qualifyExtend keeps only the fully-qualified local C and the UNSPECIFIED policy would return
        // UNSUPPORTED_DIALECT_OPTION — a regression from today's RESOLVED. IBM resolution-of-names
        // step 3 would instead select the local C in every qualify mode.
        ReferenceResolution.Entry condition = resolutionEntry(analysis, "C OF Q");
        assertAll("qualified condition-name across units",
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, condition.occurrence().kind()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, condition.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH,
                        condition.reason()),
                () -> assertEquals(innerC.id(), condition.selectedCandidate().orElseThrow()
                                .entityId().localId(),
                        "the local C wins today only because the DATA constraint excludes the FILE ancestry"));

        ReferenceResolution.Entry qualifier = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().writtenText().equals("Q")
                        && entry.occurrence().role() == ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT)
                .filter(entry -> entry.occurrence().programUnitId().equals(inner.id()))
                .reduce((first, second) -> second).orElseThrow();
        assertAll("the qualifier occurrence itself resolves to the local DATA Q",
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, qualifier.status()),
                () -> assertEquals(innerQ.id(),
                        qualifier.selectedCandidate().orElseThrow().entityId().localId()));

        ReferenceResolution.Entry dataReference = resolutionEntry(analysis, "CUST-STATUS OF Q");
        assertAll("pre-existing gap: the same collision on a qualified DATA reference",
                () -> assertEquals(2, dataReference.candidates().size(),
                        "both the local DATA and the outer GLOBAL DATA survive qualification"),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNSUPPORTED, dataReference.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION,
                        dataReference.reason(),
                        "qualifyExtend finds only the fully-qualified local candidate and the UNSPECIFIED policy "
                                + "treats the STANDARD/EXTEND divergence as unsupported; IBM resolution-of-names "
                                + "step 3 would select the local CUST-STATUS in every qualify mode"));
    }

    private static CompilationUnitSymbolTables.UnitSymbols unit(
            AstBoundaryTestSupport.Analysis analysis, String canonicalProgramName) {
        return analysis.tables().units().stream()
                .filter(unit -> unit.id().canonicalProgramName().equals(canonicalProgramName))
                .findFirst().orElseThrow();
    }

    private static SymbolTable.Symbol symbolOfKind(CompilationUnitSymbolTables.UnitSymbols unit,
                                                   String canonicalName, SymbolTable.SymbolKind kind) {
        return unit.symbolTable().symbols().stream()
                .filter(symbol -> symbol.kind() == kind && symbol.canonicalName().equals(canonicalName))
                .findFirst().orElseThrow();
    }

    private static List<AncestryName> ancestryNames(CompilationUnitSymbolTables.UnitSymbols unit,
                                                    SymbolTable.Symbol symbol) {
        List<AncestryName> result = new ArrayList<>();
        int scopeId = symbol.scopeId();
        while (scopeId >= 0) {
            SymbolTable.Scope scope = unit.symbolTable().scopes().get(scopeId);
            if (scope.ownerSymbolId() >= 0) {
                SymbolTable.Symbol owner = unit.symbolTable().symbols().get(scope.ownerSymbolId());
                result.add(new AncestryName(owner.canonicalName(), owner.namespace()));
            }
            scopeId = scope.parentId();
        }
        return result;
    }

    private record AncestryName(String canonicalName, SymbolTable.Namespace namespace) { }

    private static ReferenceResolution.Entry resolutionEntry(AstBoundaryTestSupport.Analysis analysis,
                                                             String writtenText) {
        return analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().writtenText().equals(writtenText))
                .reduce((first, second) -> second).orElseThrow();
    }

    private static boolean hasAncestor(ParseTree node, Class<?> type) {
        for (ParseTree current = node.getParent(); current != null; current = current.getParent())
            if (type.isInstance(current)) return true;
        return false;
    }

    private static void assertParse(String condition, boolean expectErrors) {
        ParseRecord record = parse(program("P-" + Integer.toHexString(condition.hashCode()), condition),
                "parse-shape.cbl");
        if (expectErrors) {
            assertTrue(record.syntaxErrors() > 0, "expected grammar rejection: " + condition);
        } else {
            assertEquals(0, record.syntaxErrors(), "expected grammar acceptance: " + condition);
        }
    }

    private static void assertParse(String condition, int expectedErrors) {
        ParseRecord record = parse(program("P-" + Integer.toHexString(condition.hashCode()), condition),
                "parse-shape.cbl");
        if (expectedErrors == 0) {
            assertEquals(0, record.syntaxErrors(), "expected grammar acceptance: " + condition);
        } else {
            assertTrue(record.syntaxErrors() > 0, "expected grammar rejection: " + condition);
        }
    }

    private static ParseRecord parse(String rawSource, String sourceName) {
        GrammarBinding binding = Bindings.cobol();
        String first = rawSource.lines().filter(line -> !line.isBlank()).findFirst().orElse("");
        String fixed = first.startsWith("       ") ? rawSource
                : rawSource.lines().map(line -> line.isBlank() ? line : "       " + line)
                .collect(java.util.stream.Collectors.joining("\n")) + "\n";
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(fixed, sourceName,
                SourceNormalizer.SourceFormat.FIXED);
        PreprocessorEngine.Outcome preprocessing;
        try {
            preprocessing = new PreprocessorEngine(binding,
                    new CopybookLibrary(java.nio.file.Path.of("src/test/resources/cobol/provenance/cpy")))
                    .process(normalized.sourceMap(), sourceName);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("test copybook library must be readable", exception);
        }
        assertEquals(0, preprocessing.errors(), "fixture must preprocess without errors");
        Parser parser = binding.cobolParser(new CommonTokenStream(binding.cobolLexer(
                CharStreams.fromString(preprocessing.text(), sourceName))));
        ParseTree tree = binding.cobolStart(parser);
        return new ParseRecord(parser.getNumberOfSyntaxErrors(), tree);
    }

    private static String program(String suffix, String condition) {
        String programId = "D-" + suffix.toUpperCase().replace('_', '-');
        return """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC X.
                01 B PIC X.
                01 C PIC X.
                01 GROUP-A.
                   05 SUB-GRP.
                      10 CUSTOMER PIC X.
                         88 FLAG-OK VALUE 'Y'.
                      10 CUST-TBL OCCURS 2 TIMES INDEXED BY IDX.
                         15 ELEM PIC X.
                   05 GRP-TBL OCCURS 2 TIMES.
                      10 ELEM PIC X.
                01 SUB PIC 99.
                01 I PIC 99.
                01 J PIC 99.
                01 X PIC X.
                PROCEDURE DIVISION.
                    IF %s CONTINUE END-IF.
                END PROGRAM %s.
                """.formatted(programId, condition, programId);
    }

    private record ParseRecord(int syntaxErrors, ParseTree tree) { }
}
