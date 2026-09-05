package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolMoveCallAdapter;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Production adapter coverage for the narrow MOVE literal to CALL variable slice. */
class SemanticProductMoveCallAdapterTest {
    private static final String SOURCE_NAME = "semantic-product-adapter.cbl";
    private static final String COMPILATION_UNIT_ID = SOURCE_NAME.toUpperCase(Locale.ROOT);
    private static final String SOURCE = String.join("\n",
            "       IDENTIFICATION DIVISION.",
            "       PROGRAM-ID. SEMANTIC-ADAPTER.",
            "       DATA DIVISION.",
            "       WORKING-STORAGE SECTION.",
            "       01 WS-PGM PIC X(8).",
            "       PROCEDURE DIVISION.",
            "           MOVE 'PGMA' TO WS-PGM.",
            "           CALL WS-PGM.",
            "           GOBACK.",
            "       END PROGRAM SEMANTIC-ADAPTER.", "");

    @Test
    void projectsCanonicalFrontendFactsIntoTheApprovedState() {
        FrontendAnalysis frontend = analyze();
        CobolSemanticProduct.State state = project(frontend);
        CobolSemanticPort port = CobolMoveCallAdapter.open(products(frontend), frontend.unit().id());

        CobolSemanticProduct.DataItemId dataItem = state.dataItems().get(0).id();
        assertEquals(COMPILATION_UNIT_ID, state.unit().compilationUnitId());
        assertEquals(List.of(0), state.unit().structuralPath());
        assertEquals("SEMANTIC-ADAPTER", state.unit().canonicalProgramName());
        assertEquals("WS-PGM", state.dataItems().get(0).name());
        assertEquals("X(8)", state.dataItems().get(0).picture());
        assertEquals(Optional.of(dataItem), state.move().target());
        assertEquals(Optional.of(dataItem), state.call().operand());
        assertEquals(Optional.of(dataItem), state.move().targetBinding().selected());
        assertEquals(Optional.of(dataItem), state.call().operandBinding().selected());
        assertEquals(CobolSemanticProduct.BindingStatus.COMPLETE,
                state.move().targetBinding().status());
        assertEquals(CobolSemanticProduct.BindingStatus.COMPLETE,
                state.call().operandBinding().status());
        assertEquals("PGMA", state.move().source().value());
        assertEquals(CobolSemanticProduct.CallSyntax.IDENTIFIER_OR_EXPRESSION,
                state.call().syntax());
        assertEquals(CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                state.call().runtimeTarget());
        assertEquals(CobolSemanticProduct.BindingStatus.COMPLETE,
                state.analysis().nominalBinding());
        assertEquals(CobolSemanticProduct.AnalysisClaim.PARTIAL,
                state.analysis().claim());
        assertEquals(CobolSemanticProduct.DependencyReadiness.INCOMPLETE,
                state.analysis().dependencyReadiness());
        assertEquals(CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                state.analysis().runtimeTarget());
        assertEquals(List.of("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"),
                state.analysis().uncertainties().stream()
                        .map(CobolSemanticProduct.Uncertainty::code).toList());
        assertEquals(state.call().point(), state.analysis().uncertainties().get(0).point());
        assertTrue(state.move().point().ordinal() < state.call().point().ordinal());
        assertEquals(state.move().point(), state.ordering().earlier());
        assertEquals(state.call().point(), state.ordering().later());

        assertEquals(CobolSemanticProduct.QualifyMode.UNSPECIFIED,
                port.policy().qualifyMode());
        assertEquals(CobolSemanticProduct.PgmnameMode.UNSPECIFIED,
                port.policy().pgmnameMode());
        assertEquals(CobolSemanticProduct.DynamMode.UNSPECIFIED,
                port.policy().dynamMode());
        assertEquals(CobolSemanticProduct.DllMode.UNSPECIFIED,
                port.policy().dllMode());
        assertEquals(frontend.resolution().policy().policyId(), state.policy().policyId());
        assertEquals(frontend.resolution().policy().version(), state.policy().version());
        assertEquals(ResolutionContracts.CallLinkage.DYNAMIC,
                frontend.resolution().entries().stream()
                        .filter(entry -> entry.occurrence().role()
                                == ResolutionContracts.ReferenceRole.CALL_TARGET)
                        .findFirst().orElseThrow().callSemantics().orElseThrow().linkage());
        assertEquals(state, portState(port));

        assertProvenance(SOURCE_NAME, state.dataItems().get(0).provenance());
        assertProvenance(SOURCE_NAME, state.move().source().provenance());
        assertProvenance(SOURCE_NAME, state.move().provenance());
        assertProvenance(SOURCE_NAME, state.call().provenance());
        assertTrue(state.dataItems().get(0).provenance().original().startLine()
                < state.move().provenance().original().startLine());
        assertTrue(state.move().provenance().original().startLine()
                < state.call().provenance().original().startLine());
        assertTrue(state.analysis().uncertainties().get(0).provenance().exact());
    }

    @Test
    void equivalentFrontendExecutionsReproduceHandlesAndProjection() {
        FrontendAnalysis firstFrontend = analyze();
        FrontendAnalysis secondFrontend = analyze();

        CobolSemanticProduct.State first = project(firstFrontend);
        CobolSemanticProduct.State second = project(secondFrontend);

        assertEquals(first, second);
        assertEquals(first.move().target(), first.call().operand());
        assertEquals(first.move().target(), second.move().target());
        assertEquals(first.dataItems().get(0).id(), second.dataItems().get(0).id());
        assertEquals(first.analysis().uncertainties(), second.analysis().uncertainties());
    }

    @Test
    void canonicalNamespaceMismatchFailsInsteadOfJoiningBySameName() {
        FrontendAnalysis frontend = analyze();
        ReferenceResolution.Entry originalCall = frontend.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role() == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .findFirst().orElseThrow();
        ReferenceResolution.Candidate originalCandidate = originalCall.selectedCandidate().orElseThrow();
        ResolutionContracts.ProgramUnitId foreignUnit = new ResolutionContracts.ProgramUnitId(
                "OTHER.CBL", List.of(0), originalCandidate.canonicalName());
        ReferenceResolution.Candidate foreignCandidate = new ReferenceResolution.Candidate(
                new ResolutionContracts.SemanticEntityId(foreignUnit,
                        ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                        originalCandidate.entityId().localId()),
                originalCandidate.kind(), originalCandidate.writtenName(),
                originalCandidate.canonicalName(), originalCandidate.declarationSymbolIds(),
                originalCandidate.attributes());
        ReferenceResolution.Entry forgedCall = new ReferenceResolution.Entry(
                originalCall.id(), originalCall.occurrence(), originalCall.status(),
                originalCall.reason(), List.of(foreignCandidate), originalCall.diagnosticIds(),
                originalCall.callSemantics());
        List<ReferenceResolution.Entry> entries = new ArrayList<>(frontend.resolution().entries());
        entries.set(originalCall.id(), forgedCall);
        ReferenceResolution forgedResolution = new ReferenceResolution(
                frontend.resolution().policy(), entries, frontend.resolution().diagnostics(),
                frontend.resolution().metrics(), frontend.resolution().declarationRelations());
        FrontendAnalysis forged = frontend.withResolution(forgedResolution);

        assertThrows(IllegalArgumentException.class, () -> project(forged));
    }

    @Test
    void adapterDoesNotReparseTextOrDependOnPresentationArtifacts() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/CobolMoveCallAdapter.java"),
                StandardCharsets.UTF_8);
        assertFalse(source.contains("writtenText"));
        assertFalse(source.contains("grammarRule"));
        assertFalse(source.contains("SourceMap"));
        assertFalse(source.contains("Snapshot"));
        assertFalse(source.contains("ExplorerMain"));
        assertFalse(source.contains("org.antlr"));
    }

    private static CobolSemanticProduct.State project(FrontendAnalysis frontend) {
        return CobolMoveCallAdapter.project(products(frontend), frontend.unit().id());
    }

    private static CobolMoveCallAdapter.FrontendProducts products(FrontendAnalysis frontend) {
        return new CobolMoveCallAdapter.FrontendProducts(frontend.build(), frontend.tables(),
                frontend.occurrences(), frontend.resolution());
    }

    private static CobolSemanticProduct.State portState(CobolSemanticPort port) {
        return new CobolSemanticProduct.State(port.unit(), port.dataItems(), port.policy(),
                port.move(), port.call(), port.ordering(), port.analysis());
    }

    private static void assertProvenance(String sourceName,
                                         CobolSemanticProduct.Provenance provenance) {
        assertEquals(sourceName, provenance.original().file());
        assertEquals("<preprocessed>", provenance.expanded().file());
        assertTrue(provenance.exact());
    }

    private static FrontendAnalysis analyze() {
        GrammarBinding grammar = Bindings.cobol();
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(
                SOURCE, SOURCE_NAME, SourceNormalizer.SourceFormat.FIXED);
        PreprocessorEngine.Outcome preprocessed;
        try {
            preprocessed = new PreprocessorEngine(grammar, new CopybookLibrary(Path.of(
                    "src/test/resources"))).process(normalized.sourceMap(), SOURCE_NAME);
        } catch (IOException exception) {
            throw new IllegalStateException("test copybook library must be readable", exception);
        }
        Parser parser = grammar.cobolParser(new CommonTokenStream(grammar.cobolLexer(
                CharStreams.fromString(preprocessed.text(), SOURCE_NAME))));
        ParseTree tree = grammar.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, preprocessed.text(),
                preprocessed.sourceMap(), ids, sizes).buildCompilationUnit(tree, SOURCE_NAME);
        CompilationUnitModel model = build.compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector().collect(
                    unit.id(), unit.program(), AstScopeIndex.build(unit.program(), table)));
        }
        ResolutionContracts.CobolResolutionPolicy policy =
                ResolutionContracts.CobolResolutionPolicy.initial()
                        .withPgmnameMode(preprocessed.pgmnameMode())
                        .withDynamMode(preprocessed.dynamMode())
                        .withDllMode(preprocessed.dllMode());
        ReferenceResolution resolution = new CobolReferenceResolver(policy)
                .resolve(model, tables, occurrences);
        return new FrontendAnalysis(build, model.programUnits().get(0), tables,
                occurrences, resolution);
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int child = 0; child < tree.getChildCount(); child++)
            size += index(tree.getChild(child), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private record FrontendAnalysis(CompilationUnitBuildResult build,
                                    CompilationUnitModel.ProgramUnit unit,
                                    CompilationUnitSymbolTables tables,
                                    Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences,
                                    ReferenceResolution resolution) {
        private FrontendAnalysis withResolution(ReferenceResolution replacement) {
            return new FrontendAnalysis(build, unit, tables, occurrences, replacement);
        }
    }
}
