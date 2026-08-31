package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Requirement oracles promoted as their production slices become implemented. */
class AstSemanticBoundaryRequiredOracleTest {

    @Test
    void everyMaterializedSemanticBoundaryHasExactlyOneFinding() throws Exception {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyzeFixture();
        Map<String, Class<? extends Ast.Node>> boundaries = new LinkedHashMap<>();
        boundaries.put("statement", Ast.Statement.class);
        boundaries.put("data description entry", Ast.DataEntry.class);
        boundaries.put("data clause", Ast.DataClause.class);
        boundaries.put("preserved expression", Ast.PreservedExpression.class);

        for (CompilationUnitModel.ProgramUnit unit : analysis.model().programUnits()) {
            SemanticCoverage.Report report = analysis.build().coverageByProgramUnit().get(unit.id());
            boundaries.forEach((name, type) -> {
                List<Ast.Node> nodes = AstBoundaryTestSupport.nodes(unit.program()).stream()
                        .filter(type::isInstance).toList();
                for (Ast.Node node : nodes) {
                    List<SemanticCoverage.Finding> findings = report.findings().stream()
                            .filter(finding -> finding.astNodeId() == node.meta().id()).toList();
                    assertEquals(1, findings.size(),
                            name + " must have exactly one finding for node "
                                    + node.meta().id() + " in " + unit.id());
                    assertEquals(node.meta(), findings.get(0).meta(),
                            name + " finding must preserve node metadata and provenance");
                }
            });
        }
    }

    @Test
    void unknownDataClauseWithoutNominalReferenceBlocksReadiness() {
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

        assertFalse(analysis.report().completeness().dependencyAnalysisReady(),
                "DEPENDENCY_UNKNOWN data clauses must block the implemented readiness claim");
        assertTrue(analysis.report().gaps().stream().anyMatch(gap ->
                gap.grammarRule().equals("dataValueClause")
                        || gap.grammarRule().equals("dataBlankWhenZeroClause")));
    }

    @Test
    @EnabledIfSystemProperty(named = "ast.boundary.required", matches = "true")
    void reportFailsClosedWhenResolutionContainsOccurrenceMissingFromCollectorProduct() {
        String source = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CORRUPT-JOIN.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 SOURCE-ITEM PIC X.
                01 TARGET-ITEM PIC X.
                PROCEDURE DIVISION.
                    MOVE SOURCE-ITEM TO TARGET-ITEM.
                    GOBACK.
                END PROGRAM CORRUPT-JOIN.
                """;
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(source,
                "corrupt-join.cbl");
        ResolutionContracts.ProgramUnitId unitId = analysis.model().programUnits().get(0).id();
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> corrupted =
                Map.of(unitId, new ReferenceOccurrences(List.of()));

        assertThrows(IllegalStateException.class, () -> ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(), corrupted,
                analysis.resolution()),
                "an extra resolution entry without its occurrence product must be rejected");
    }

    @Test
    @EnabledIfSystemProperty(named = "ast.boundary.required", matches = "true")
    void crossProductValidationRejectsSymbolWhoseDeclarationAstNodeDoesNotExist() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. ORPHAN-SYMBOL.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 ITEM-A PIC X.
                PROCEDURE DIVISION.
                    GOBACK.
                END PROGRAM ORPHAN-SYMBOL.
                """, "orphan-symbol.cbl");
        SymbolTable original = analysis.tables().units().get(0).symbolTable();
        List<SymbolTable.Symbol> symbols = new java.util.ArrayList<>(original.symbols());
        SymbolTable.Symbol first = symbols.get(0);
        symbols.set(0, new SymbolTable.Symbol(first.id(), first.kind(), first.namespace(),
                first.writtenName(), first.canonicalName(), first.scopeId(), Integer.MAX_VALUE,
                first.span(), first.attributes()));

        SymbolTable corruptedTable = new SymbolTable(original.scopes(), symbols,
                original.diagnostics(), original.entities(), original.declarationRelations());
        CompilationUnitSymbolTables.UnitSymbols originalUnit = analysis.tables().units().get(0);
        CompilationUnitSymbolTables corruptedTables = new CompilationUnitSymbolTables(List.of(
                new CompilationUnitSymbolTables.UnitSymbols(originalUnit.id(), originalUnit.parentId(),
                        corruptedTable)));

        assertThrows(RuntimeException.class,
                () -> AstBoundaryTestSupport.composePostAstProducts(analysis, corruptedTables),
                "post-AST product composition must reject a symbol whose declaration node is absent");
    }
}
