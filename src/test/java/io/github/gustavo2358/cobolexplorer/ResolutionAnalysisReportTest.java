package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ResolutionAnalysisReportTest {
    private static final Path STATES = Path.of("src/test/resources/cobol/resolution/coverage-states.cbl");

    @Test
    void composesAllFourStatesReasonsAndConservativeCompleteness() throws Exception {
        Analysis analysis = analyze(Files.readString(STATES, StandardCharsets.UTF_8), "coverage-states.cbl");
        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(analysis.build(),
                ResolutionAnalysisReport.FrontendState.complete(), analysis.occurrences(), analysis.resolution());

        assertTrue(report.statusCounts().get(ResolutionContracts.ResolutionStatus.RESOLVED) > 0);
        assertTrue(report.statusCounts().get(ResolutionContracts.ResolutionStatus.AMBIGUOUS) > 0);
        assertTrue(report.statusCounts().get(ResolutionContracts.ResolutionStatus.UNRESOLVED) > 0);
        assertTrue(report.statusCounts().get(ResolutionContracts.ResolutionStatus.UNSUPPORTED) > 0);
        assertTrue(report.reasonCounts().containsKey(ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES));
        assertTrue(report.reasonCounts().containsKey(ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND));
        assertTrue(report.reasonCounts().containsKey(ResolutionContracts.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM));
        assertFalse(report.completeness().referenceBindingComplete());
        assertFalse(report.completeness().dependencyAnalysisReady());
        assertEquals(ResolutionAnalysisReport.AnalysisClaim.INCOMPLETE, report.analysisClaim());
        assertTrue(report.gaps().stream().anyMatch(gap -> gap.category()
                == ResolutionAnalysisReport.GapCategory.REFERENCE_BINDING));
        assertThrows(UnsupportedOperationException.class, () -> report.gaps().clear());
    }

    @Test
    void missingCopyAndOpaqueContainersCanNeverBecomeZeroDependencies() throws Exception {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. INPUTGAP.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 VALUE-A PIC X.",
                "       PROCEDURE DIVISION.",
                "           DISPLAY VALUE-A.",
                "           GOBACK.",
                "       END PROGRAM INPUTGAP.", "");
        Analysis analysis = analyze(source, "input-gap.cbl");
        ResolutionAnalysisReport.FrontendState incomplete = new ResolutionAnalysisReport.FrontendState(
                1, 0, 0, 0, List.of(new Diagnostic("COBOL", Diagnostic.Phase.PREPROCESSOR,
                "input-gap.cbl", 1, 0, "unresolved_copy: MISSING", "MISSING", "")));
        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(
                analysis.build(), incomplete, analysis.occurrences(), analysis.resolution());

        assertFalse(report.completeness().referenceBindingComplete());
        assertFalse(report.completeness().dependencyAnalysisReady());
        assertTrue(report.gaps().stream().anyMatch(gap -> gap.code().equals("UNRESOLVED_COPY")));
        assertTrue(report.gaps().stream().anyMatch(gap -> gap.code().equals("PRESERVED_REFERENCE_CONTAINER")));
        assertTrue(report.unknownDependencyCount() > 0);
        assertNotEquals(ResolutionAnalysisReport.AnalysisClaim.COMPLETE, report.analysisClaim());
    }

    @Test
    void missingCopyCountAndDetailedDiagnosticsMustRemainReconciled() {
        Diagnostic missing = new Diagnostic("COBOL", Diagnostic.Phase.PREPROCESSOR,
                "input-gap.cbl", 4, 7, "unresolved_copy: MISSING", "MISSING", "");

        assertAll("missing inputs remain enumerable rather than aggregate-only",
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new ResolutionAnalysisReport.FrontendState(1, 0, 0, 0, List.of())),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new ResolutionAnalysisReport.FrontendState(0, 0, 0, 0,
                                List.of(missing))));
    }

    @Test
    void scalesByIndexedCandidatesAndProducesDeterministicResults() throws Exception {
        int declarations = 1_200;
        Analysis analysis = analyze(scaleSource(declarations), "scale.cbl");
        ResolutionAnalysisReport first = ResolutionAnalysisReport.compose(analysis.build(),
                ResolutionAnalysisReport.FrontendState.complete(), analysis.occurrences(), analysis.resolution());
        ReferenceResolution secondResolution = resolveAgain(analysis);
        ResolutionAnalysisReport second = ResolutionAnalysisReport.compose(analysis.build(),
                ResolutionAnalysisReport.FrontendState.complete(), analysis.occurrences(), secondResolution);

        assertEquals(declarations * 2L, first.referenceCount());
        assertTrue(first.operationalMetrics().indexedDeclarations() >= declarations);
        assertTrue(first.operationalMetrics().candidateInspections() <= first.referenceCount() * 2,
                "same-name index lookup must not scan all declarations for each reference");
        assertEquals(signature(analysis.resolution()), signature(secondResolution));
        assertEquals(first.statusCounts(), second.statusCounts());
        assertEquals(first.reasonCounts(), second.reasonCounts());
        assertTrue(first.completeness().referenceBindingComplete());
        assertTrue(first.completeness().dependencyAnalysisReady());
    }

    @Test
    void keepsProgramUnitsIsolatedAndResolverInstancesParallelSafe() throws Exception {
        String source = Files.readString(Path.of(
                "src/test/resources/cobol/resolution/nested-data-visibility.cbl"), StandardCharsets.UTF_8);
        Analysis analysis = analyze(source, "nested-data-visibility.cbl");
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> signature(resolveAgain(analysis))));
        }
        List<String> expected = signature(analysis.resolution());
        for (CompletableFuture<List<String>> future : futures) assertEquals(expected, future.get());

        assertTrue(analysis.model().programUnits().size() > 1);
        assertTrue(analysis.resolution().entries().stream().allMatch(entry ->
                analysis.model().find(entry.occurrence().programUnitId()).isPresent()));
    }

    @Test
    void unresolvedDeclarationRelationsBlockDependencyReadiness() throws Exception {
        String source = Files.readString(Path.of(
                "src/test/resources/cobol/resolution/redefines-different-level-number.cbl"),
                StandardCharsets.UTF_8);
        Analysis analysis = analyze(source, "redefines-different-level-number.cbl");
        assertTrue(analysis.resolution().declarationRelations().entries().stream().anyMatch(entry ->
                entry.status() != ResolutionContracts.ResolutionStatus.RESOLVED));
        assertTrue(analysis.resolution().entries().stream().allMatch(entry ->
                entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED),
                "generic nominal occurrences intentionally isolate the relation-specific readiness gap");

        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(analysis.build(),
                ResolutionAnalysisReport.FrontendState.complete(), analysis.occurrences(),
                analysis.resolution());
        assertAll("an invalid declaration relation must block downstream readiness",
                () -> assertFalse(report.completeness().referenceBindingComplete()),
                () -> assertFalse(report.completeness().dependencyAnalysisReady()),
                () -> assertEquals(ResolutionAnalysisReport.AnalysisClaim.INCOMPLETE,
                        report.analysisClaim()),
                () -> assertTrue(report.gaps().stream().anyMatch(gap ->
                        gap.code().startsWith("DECLARATION_RELATION_UNRESOLVED_"))));
    }

    @Test
    void distinguishesSyntacticHintsFromResolvedSemanticKindMetrics() throws Exception {
        Path fixture = Path.of("src/test/resources/cobol/resolution/subscript-semantic-kind.cbl");
        Analysis analysis = analyze(Files.readString(fixture, StandardCharsets.UTF_8),
                fixture.getFileName().toString());
        List<ReferenceResolution.Entry> subscripts = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role() == ResolutionContracts.ReferenceRole.SUBSCRIPT)
                .toList();
        assertTrue(subscripts.stream().anyMatch(entry ->
                entry.occurrence().kind() == ResolutionContracts.ReferenceKind.INDEX
                        && entry.selectedCandidate().orElseThrow().kind()
                        == ResolutionContracts.ReferenceKind.DATA));

        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(analysis.build(),
                ResolutionAnalysisReport.FrontendState.complete(), analysis.occurrences(),
                analysis.resolution());
        long syntacticIndexSubscripts = subscripts.stream().filter(entry ->
                entry.occurrence().kind() == ResolutionContracts.ReferenceKind.INDEX).count();
        long resolvedDataSubscripts = subscripts.stream().filter(entry ->
                entry.selectedCandidate().orElseThrow().kind()
                        == ResolutionContracts.ReferenceKind.DATA).count();
        assertAll("metrics must name their semantic level",
                () -> assertTrue(syntacticIndexSubscripts > 0),
                () -> assertTrue(resolvedDataSubscripts > 0),
                () -> assertTrue(report.syntacticKindCounts()
                        .get(ResolutionContracts.ReferenceKind.INDEX) >= syntacticIndexSubscripts),
                () -> assertTrue(report.resolvedSemanticKindCounts()
                        .get(ResolutionContracts.ReferenceKind.DATA) >= resolvedDataSubscripts),
                () -> assertEquals(analysis.resolution().entries().size(),
                        report.syntacticKindCounts().values().stream().mapToLong(Long::longValue).sum()),
                () -> assertEquals(analysis.resolution().entries().stream()
                                .filter(entry -> entry.status()
                                        == ResolutionContracts.ResolutionStatus.RESOLVED).count(),
                        report.resolvedSemanticKindCounts().values().stream()
                                .mapToLong(Long::longValue).sum()));
    }

    private static ReferenceResolution resolveAgain(Analysis analysis) {
        return new CobolReferenceResolver(ResolutionContracts.CobolResolutionPolicy.initial())
                .resolve(analysis.model(), analysis.tables(), analysis.occurrences());
    }

    private static List<String> signature(ReferenceResolution resolution) {
        return resolution.entries().stream().map(entry -> entry.occurrence().programUnitId() + "|"
                + entry.occurrence().id() + "|" + entry.status() + "|" + entry.reason() + "|"
                + entry.candidates().stream().map(candidate -> candidate.entityId().toString()).toList()).toList();
    }

    private static String scaleSource(int declarations) {
        StringBuilder source = new StringBuilder(String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SCALETEST.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 SINK-ITEM PIC X.", ""));
        for (int i = 0; i < declarations; i++)
            source.append("       01 ITEM-%04d PIC X.%n".formatted(i));
        source.append("       PROCEDURE DIVISION.\n");
        for (int i = 0; i < declarations; i++)
            source.append("           MOVE ITEM-%04d TO SINK-ITEM.%n".formatted(i));
        source.append("           GOBACK.\n       END PROGRAM SCALETEST.\n");
        return source.toString();
    }

    private static Analysis analyze(String rawSource, String sourceName) throws Exception {
        String source = SourceNormalizerTestSupport.fixed(rawSource);
        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, sourceName))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, source,
                SourceMap.identity(source, sourceName), ids, sizes).buildCompilationUnit(tree, sourceName);
        CompilationUnitModel model = build.compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector().collect(unit.id(), unit.program(),
                    AstScopeIndex.build(unit.program(), table)));
        }
        ReferenceResolution resolution = new CobolReferenceResolver(
                ResolutionContracts.CobolResolutionPolicy.initial())
                .resolve(model, tables, occurrences);
        return new Analysis(build, model, tables, occurrences, resolution);
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private record Analysis(CompilationUnitBuildResult build, CompilationUnitModel model,
                            CompilationUnitSymbolTables tables,
                            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences,
                            ReferenceResolution resolution) { }
}
