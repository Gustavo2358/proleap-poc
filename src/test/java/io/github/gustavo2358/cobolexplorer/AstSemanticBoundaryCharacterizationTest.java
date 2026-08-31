package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Green characterization of the production baseline observed by WORK-AST-002. */
class AstSemanticBoundaryCharacterizationTest {
    private static final String MATRIX_VERSION = "WORK-AST-002/F0/v1";

    @Test
    void matrixCountsParseContextsAstNodesAndCurrentCoverageExactly() throws Exception {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyzeFixture();
        List<BoundaryRow> matrix = boundaryMatrix(analysis);

        assertEquals("WORK-AST-002/F0/v1", MATRIX_VERSION);
        assertEquals(List.of(
                        new BoundaryRow("statement", "statement", "Ast.Statement", 10, 10,
                                "manifest per concrete statement", "manifest per concrete statement",
                                "only nominal children", "(ProgramUnitId, astNodeId)", 10),
                        new BoundaryRow("data-description", "dataDescriptionEntry", "Ast.DataEntry", 14, 14,
                                "PRESERVED_UNINTERPRETED", "DEPENDENCY_UNKNOWN",
                                "no; clauses may contain occurrences", "(ProgramUnitId, astNodeId)", 0),
                        new BoundaryRow("data-clause", "direct data clause", "Ast.DataClause", 20, 20,
                                "typed or PRESERVED_UNINTERPRETED", "clause-specific",
                                "only nominal endpoints", "(ProgramUnitId, astNodeId)", 0),
                        new BoundaryRow("preserved-expression", "abbreviation", "Ast.PreservedExpression", 1, 1,
                                "PRESERVED_UNINTERPRETED", "DEPENDENCY_UNKNOWN",
                                "recognized operands only", "(ProgramUnitId, astNodeId)", 0)),
                matrix);
    }

    @Test
    void allFiftyStatementAlternativesRemainOneToOneFromContextToReachableAst() throws Exception {
        Path fixture = Path.of("src/test/resources/cobol/semantic/statements.cbl");
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                Files.readString(fixture, StandardCharsets.UTF_8), fixture.getFileName().toString());
        int parseStatements = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.StatementContext.class).size();
        List<Ast.Statement> astStatements = AstBoundaryTestSupport.nodes(analysis, Ast.Statement.class);

        assertAll("every recognized statement context is materialized exactly once",
                () -> assertEquals(parseStatements, astStatements.size()),
                () -> assertEquals(50, astStatements.stream()
                        .map(statement -> statement.meta().origin().grammarRule()).distinct().count()),
                () -> assertEquals(astStatements.size(), analysis.build().coverageByProgramUnit().values()
                        .stream().mapToInt(report -> report.findings().size()).sum()));
    }

    @Test
    void preservesDataHierarchyFillersValuesAndRelationEndpointsWithoutInventingStorage() throws Exception {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyzeFixture();
        List<Ast.DataEntry> entries = AstBoundaryTestSupport.nodes(analysis, Ast.DataEntry.class);
        Ast.DataEntry root = entries.stream().filter(entry -> entry.name().equals("STORAGE-GROUP"))
                .findFirst().orElseThrow();
        List<Ast.DataEntry> fillers = entries.stream().filter(Ast.DataEntry::filler)
                .filter(entry -> entry.levelKind() != Ast.DataLevelKind.OPAQUE).toList();
        Ast.DataEntry opaqueSql = entries.stream()
                .filter(entry -> entry.levelKind() == Ast.DataLevelKind.OPAQUE)
                .findFirst().orElseThrow();
        Ast.DataEntry fillerRedefines = fillers.stream().filter(entry -> entry.clauses().stream()
                .anyMatch(Ast.RedefinesClause.class::isInstance)).findFirst().orElseThrow();
        Ast.RedefinesClause redefines = fillerRedefines.clauses().stream()
                .filter(Ast.RedefinesClause.class::isInstance).map(Ast.RedefinesClause.class::cast)
                .findFirst().orElseThrow();
        Ast.RenamesClause renames = AstBoundaryTestSupport.nodes(analysis, Ast.RenamesClause.class)
                .get(0);
        Ast.OccursClause occurs = AstBoundaryTestSupport.nodes(analysis, Ast.OccursClause.class).get(0);
        Ast.ValueClause conditionValues = entries.stream()
                .filter(entry -> entry.name().equals("VALID-COUNT"))
                .flatMap(entry -> entry.clauses().stream())
                .filter(Ast.ValueClause.class::isInstance).map(Ast.ValueClause.class::cast)
                .findFirst().orElseThrow();

        CompilationUnitModel.ProgramUnit parent = analysis.model().programUnits().get(0);
        SymbolTable table = analysis.tables().forProgramUnit(parent.id()).orElseThrow().symbolTable();
        ReferenceResolution.Entry fillerTarget = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().programUnitId().equals(parent.id()))
                .filter(entry -> entry.occurrence().referenceAstNodeId() == redefines.target().meta().id())
                .findFirst().orElseThrow();

        assertAll("structural facts remain distinct from future storage semantics",
                () -> assertEquals(9, root.children().size()),
                () -> assertEquals(3, fillers.size()),
                () -> assertEquals("SQL", opaqueSql.level()),
                () -> assertTrue(opaqueSql.declaration().contains("EXEC SQL")),
                () -> assertTrue(opaqueSql.filler(),
                        "current AST conflates an absent SQL declarator with the FILLER flag"),
                () -> assertEquals(6, AstBoundaryTestSupport.nodes(analysis, Ast.ValueClause.class).size()),
                () -> assertEquals(2, conditionValues.values().size()),
                () -> assertEquals("BASE-ITEM", redefines.target().baseName()),
                () -> assertEquals("RANGE-START", renames.from().baseName()),
                () -> assertEquals("RANGE-END", renames.through().baseName()),
                () -> assertEquals("TABLE-COUNT", occurs.dependingOn().baseName()),
                () -> assertEquals("TABLE-IDX", occurs.indexes().get(0).indexName()),
                () -> assertTrue(table.lookupAll(SymbolTable.Namespace.DATA, "FILLER").isEmpty()),
                () -> assertTrue(table.scopes().stream()
                        .anyMatch(scope -> scope.astNodeId() == fillerRedefines.meta().id())),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                        fillerTarget.status()),
                () -> assertTrue(fillerTarget.selectedCandidate().isPresent()),
                () -> assertTrue(root.meta().provenance().exact()),
                () -> assertEquals("ast-cfg-boundary.cbl",
                        root.meta().provenance().original().file()),
                () -> assertTrue(table.declarationRelations().stream().noneMatch(relation ->
                        relation.referenceAstNodeId() == redefines.target().meta().id()),
                        "FILLER has no owner symbol, so its endpoint is bound only as a nominal occurrence"));
    }

    @Test
    void keepsLiteralAndIdentifierCallTargetsSemanticallySeparate() throws Exception {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyzeFixture();
        List<Ast.CallStatement> calls = AstBoundaryTestSupport.nodes(analysis, Ast.CallStatement.class);
        Ast.CallStatement literal = calls.stream()
                .filter(call -> call.targetSyntax() == Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME)
                .findFirst().orElseThrow();
        Ast.CallStatement dynamic = calls.stream()
                .filter(call -> call.targetSyntax() == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION)
                .findFirst().orElseThrow();
        ReferenceResolution.Entry literalEntry = callEntry(analysis, literal);
        ReferenceResolution.Entry dynamicEntry = callEntry(analysis, dynamic);

        assertAll("syntax, nominal binding and runtime target remain separate",
                () -> assertEquals(2, calls.size()),
                () -> assertInstanceOf(Ast.ProgramReference.class, literal.target()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED,
                        literalEntry.status()),
                () -> assertTrue(literalEntry.candidates().isEmpty()),
                () -> assertInstanceOf(Ast.DataReference.class, dynamic.target()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                        dynamicEntry.status()),
                () -> assertEquals(ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                        dynamicEntry.selectedCandidate().orElseThrow().entityId().domain()),
                () -> assertEquals(Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION,
                        dynamicEntry.callSemantics().orElseThrow().targetSyntax()),
                () -> assertTrue(analysis.report().gaps().stream()
                        .anyMatch(gap -> gap.code().equals("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"))));
    }

    @Test
    void currentPipelineProductsJoinExactlyWhenProducedNormally() throws Exception {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyzeFixture();
        AstBoundaryTestSupport.assertActualProductsJoin(analysis);

        assertEquals(DeclarationRelationResolution.SemanticScope.NOMINAL_STRUCTURAL_TARGET_BINDING,
                analysis.resolution().declarationRelations().semanticScope());
        assertFalse(analysis.report().completeness().dependencyAnalysisReady());
        assertTrue(analysis.report().gaps().stream().anyMatch(gap ->
                        gap.grammarRule().equals("execCicsStatement")),
                () -> "observed gap codes=" + analysis.report().gaps().stream()
                        .map(ResolutionAnalysisReport.Gap::code).toList());
        Ast.EmbeddedLanguageStatement embedded = AstBoundaryTestSupport.nodes(
                analysis, Ast.EmbeddedLanguageStatement.class).get(0);
        assertAll("preprocessing provenance survives opaque embedded preservation",
                () -> assertFalse(embedded.meta().provenance().exact()),
                () -> assertEquals("ast-cfg-boundary.cbl",
                        embedded.meta().provenance().original().file()),
                () -> assertTrue(embedded.rawText().contains("EXEC CICS")),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.RawExpression.class).isEmpty()));
    }

    @Test
    void everyReachableDataOrIndexReferenceHasOneOccurrenceInTheSameUnit() throws Exception {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyzeFixture();
        for (CompilationUnitModel.ProgramUnit unit : analysis.model().programUnits()) {
            Set<Integer> dataReferenceNodeIds = AstBoundaryTestSupport.nodes(unit.program()).stream()
                    .filter(node -> node instanceof Ast.DataReference
                            || node instanceof Ast.IndexReference)
                    .map(node -> node.meta().id()).collect(java.util.stream.Collectors.toSet());
            Set<Integer> dataOccurrenceNodeIds = analysis.occurrences().get(unit.id()).occurrences().stream()
                    .filter(occurrence -> occurrence.kind() == ResolutionContracts.ReferenceKind.DATA
                            || occurrence.kind() == ResolutionContracts.ReferenceKind.INDEX
                            || occurrence.kind() == ResolutionContracts.ReferenceKind.CONDITION)
                    .map(ReferenceOccurrences.Occurrence::referenceAstNodeId)
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(dataReferenceNodeIds, dataOccurrenceNodeIds,
                    "DATA references and occurrences must join bijectively in " + unit.id());
        }
    }

    @Test
    void localIdsRepeatAcrossUnitsButCompositeIdentitiesRemainDistinct() throws Exception {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyzeFixture();
        assertEquals(2, analysis.model().programUnits().size());
        CompilationUnitModel.ProgramUnit parent = analysis.model().programUnits().get(0);
        CompilationUnitModel.ProgramUnit child = analysis.model().programUnits().get(1);

        assertAll("local counters deliberately restart at each program unit",
                () -> assertEquals(0, parent.program().meta().id()),
                () -> assertEquals(0, child.program().meta().id()),
                () -> assertNotEquals(parent.id(), child.id()),
                () -> assertEquals(0, analysis.tables().forProgramUnit(parent.id()).orElseThrow()
                        .symbolTable().symbols().get(0).id()),
                () -> assertEquals(0, analysis.tables().forProgramUnit(child.id()).orElseThrow()
                        .symbolTable().symbols().get(0).id()),
                () -> assertEquals(0, analysis.occurrences().get(parent.id()).occurrences().get(0).id()),
                () -> assertEquals(0, analysis.occurrences().get(child.id()).occurrences().get(0).id()));

        Set<String> compositeOccurrences = new HashSet<>();
        analysis.occurrences().forEach((unitId, occurrences) -> occurrences.occurrences().forEach(
                occurrence -> assertTrue(compositeOccurrences.add(unitId + "#" + occurrence.id()))));
    }

    @Test
    void characterizesCoverageBlindSpotForUnknownClauseWithoutNominalReference() {
        String source = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. VALUE-ONLY.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 FLAG PIC X VALUE 'Y' BLANK WHEN ZERO.
                PROCEDURE DIVISION.
                    GOBACK.
                END PROGRAM VALUE-ONLY.
                """;
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source,
                "value-only.cbl");
        SemanticCoverage.Report coverage = analysis.build().coverageByProgramUnit().values()
                .iterator().next();

        assertAll("current production observes only the statement boundary",
                () -> assertEquals(List.of("gobackStatement"), coverage.findings().stream()
                        .map(SemanticCoverage.Finding::grammarRule).toList()),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.ValueClause.class).size() == 1),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.PreservedDataClause.class)
                        .stream().anyMatch(clause -> clause.grammarRule()
                                .equals("dataBlankWhenZeroClause"))),
                () -> assertTrue(coverage.dependencyCoverageComplete()),
                () -> assertTrue(analysis.report().completeness().dependencyAnalysisReady()),
                () -> assertTrue(analysis.report().gaps().isEmpty()));
    }

    private static List<BoundaryRow> boundaryMatrix(AstBoundaryTestSupport.Analysis analysis) {
        int statements = AstBoundaryTestSupport.nodes(analysis, Ast.Statement.class).size();
        int entries = AstBoundaryTestSupport.nodes(analysis, Ast.DataEntry.class).size();
        int clauses = AstBoundaryTestSupport.nodes(analysis, Ast.DataClause.class).size();
        int preservedExpressions = AstBoundaryTestSupport.nodes(analysis, Ast.PreservedExpression.class)
                .size();
        return List.of(
                new BoundaryRow("statement", "statement", "Ast.Statement",
                        AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.StatementContext.class).size(),
                        statements, "manifest per concrete statement", "manifest per concrete statement",
                        "only nominal children", "(ProgramUnitId, astNodeId)",
                        countFindings(analysis, Ast.Statement.class)),
                new BoundaryRow("data-description", "dataDescriptionEntry", "Ast.DataEntry",
                        AstBoundaryTestSupport.contexts(analysis.tree(),
                                CobolParser.DataDescriptionEntryContext.class).size(),
                        entries, "PRESERVED_UNINTERPRETED", "DEPENDENCY_UNKNOWN",
                        "no; clauses may contain occurrences", "(ProgramUnitId, astNodeId)",
                        countFindings(analysis, Ast.DataEntry.class)),
                new BoundaryRow("data-clause", "direct data clause", "Ast.DataClause",
                        AstBoundaryTestSupport.directDataClauseContexts(analysis.tree()).size(),
                        clauses, "typed or PRESERVED_UNINTERPRETED", "clause-specific",
                        "only nominal endpoints", "(ProgramUnitId, astNodeId)",
                        countFindings(analysis, Ast.DataClause.class)),
                new BoundaryRow("preserved-expression", "abbreviation", "Ast.PreservedExpression",
                        AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.AbbreviationContext.class)
                                .size(),
                        preservedExpressions, "PRESERVED_UNINTERPRETED", "DEPENDENCY_UNKNOWN",
                        "recognized operands only", "(ProgramUnitId, astNodeId)",
                        countFindings(analysis, Ast.PreservedExpression.class)));
    }

    private static int countFindings(AstBoundaryTestSupport.Analysis analysis, Class<?> type) {
        int count = 0;
        for (CompilationUnitModel.ProgramUnit unit : analysis.model().programUnits()) {
            Set<Integer> nodeIds = AstBoundaryTestSupport.nodes(unit.program()).stream()
                    .filter(type::isInstance).map(node -> node.meta().id())
                    .collect(java.util.stream.Collectors.toSet());
            count += (int) analysis.build().coverageByProgramUnit().get(unit.id()).findings().stream()
                    .filter(finding -> nodeIds.contains(finding.astNodeId())).count();
        }
        return count;
    }

    private static ReferenceResolution.Entry callEntry(AstBoundaryTestSupport.Analysis analysis,
                                                        Ast.CallStatement call) {
        return analysis.resolution().entries().stream().filter(entry ->
                entry.occurrence().referenceAstNodeId() == call.target().meta().id()
                        && entry.occurrence().role() == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .findFirst().orElseThrow();
    }

    private record BoundaryRow(String semanticBoundary, String antlrContext, String astNode,
                               int contextCardinality, int astCardinality,
                               String expectedCoverage, String expectedDependencyKnowledge,
                               String occurrenceExpectation, String joinIdentity,
                               int observedFindingCardinality) { }
}
