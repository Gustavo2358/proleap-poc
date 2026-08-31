package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CallSemanticsTest {
    private static final Path DYNAM = Path.of(
            "src/test/resources/cobol/resolution/call-linkage-dynam.cbl");
    private static final Path NODYNAM = Path.of(
            "src/test/resources/cobol/resolution/call-linkage-nodynam.cbl");
    private static final Path UNSPECIFIED = Path.of(
            "src/test/resources/cobol/resolution/call-linkage-unspecified.cbl");
    private static final Path DYNAMIC_EXTERNAL = Path.of(
            "src/test/resources/cobol/resolution/dynamic-external-canonicalization.cbl");
    private static final Path STATIC_EXTERNAL = Path.of(
            "src/test/resources/cobol/resolution/static-external-canonicalization.cbl");
    private static final Path UNKNOWN_LINKAGE_EXTERNAL = Path.of(
            "src/test/resources/cobol/resolution/unknown-linkage-external-canonicalization.cbl");
    private static final Path UNKNOWN_LINKAGE_SAME_TARGET = Path.of(
            "src/test/resources/cobol/resolution/unknown-linkage-same-external-target.cbl");
    private static final Path CALL_IDENTIFIER_RUNTIME_TARGET = Path.of(
            "src/test/resources/cobol/resolution/call-identifier-runtime-target.cbl");
    private static final Path DLL = Path.of(
            "src/test/resources/cobol/resolution/call-linkage-dll.cbl");
    private static final Path INVALID_DYNAM_DLL = Path.of(
            "src/test/resources/cobol/resolution/invalid-dynam-dll-call.cbl");

    @Test
    void separatesLiteralTargetSyntaxFromCompilerSelectedLinkage() throws Exception {
        GrammarBinding grammar = Bindings.cobol();
        String source = SourceNormalizerTestSupport.fixed(Files.readString(DYNAM, StandardCharsets.UTF_8));
        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(
                grammar, new CopybookLibrary(Path.of("src/test/resources")))
                .process(SourceMap.identity(source, DYNAM.getFileName().toString()),
                        DYNAM.getFileName().toString());
        Set<String> outcomeComponents = components(PreprocessorEngine.Outcome.class);
        Set<String> policyComponents = components(ResolutionContracts.CobolResolutionPolicy.class);
        Set<String> entryComponents = components(ReferenceResolution.Entry.class);
        Set<String> targetKinds = Arrays.stream(Ast.CallTargetSyntax.values())
                .map(Enum::name).collect(Collectors.toSet());

        assertAll("CALL syntax and linkage must be independent contracts",
                () -> assertTrue(outcome.compilerOptions().stream().anyMatch(option ->
                        option.name().equals("DYNAM")), outcome.compilerOptions().toString()),
                () -> assertTrue(outcomeComponents.contains("dynamMode"), outcomeComponents.toString()),
                () -> assertTrue(policyComponents.contains("dynamMode"), policyComponents.toString()),
                () -> assertEquals(Set.of("LITERAL_PROGRAM_NAME", "IDENTIFIER_OR_EXPRESSION"),
                        targetKinds),
                () -> assertTrue(entryComponents.contains("callSemantics"), entryComponents.toString()));
    }

    @Test
    void transportsDynamModeAndAssignsLinkageWithoutChangingTargetSyntax() throws Exception {
        Analysis dynam = analyze(DYNAM);
        Analysis nodynam = analyze(NODYNAM);
        Analysis unspecified = analyze(UNSPECIFIED);

        assertAll("compiler options are transported structurally",
                () -> assertEquals(ResolutionContracts.DynamMode.DYNAM, dynam.outcome().dynamMode()),
                () -> assertEquals(ResolutionContracts.DynamMode.NODYNAM, nodynam.outcome().dynamMode()),
                () -> assertEquals(ResolutionContracts.DynamMode.UNSPECIFIED,
                        unspecified.outcome().dynamMode()),
                () -> assertEquals(ResolutionContracts.DynamMode.DYNAM,
                        dynam.resolution().policy().dynamMode()),
                () -> assertEquals(ResolutionContracts.DynamMode.NODYNAM,
                        nodynam.resolution().policy().dynamMode()),
                () -> assertEquals(ResolutionContracts.DllMode.NODLL, dynam.outcome().dllMode()),
                () -> assertEquals(ResolutionContracts.DllMode.NODLL, nodynam.outcome().dllMode()),
                () -> assertEquals(ResolutionContracts.DllMode.UNSPECIFIED,
                        unspecified.outcome().dllMode()));

        assertCallSyntax(dynam.model().programUnits().get(0).program());
        assertCallSyntax(nodynam.model().programUnits().get(0).program());
        assertCallSyntax(unspecified.model().programUnits().get(0).program());

        assertLinkage(dynam, "'TARGET-A'", ResolutionContracts.CallLinkage.DYNAMIC);
        assertLinkage(nodynam, "'TARGET-A'", ResolutionContracts.CallLinkage.STATIC);
        assertLinkage(unspecified, "'TARGET-A'", ResolutionContracts.CallLinkage.UNKNOWN);
        assertLinkage(dynam, "CALL-NAME", ResolutionContracts.CallLinkage.DYNAMIC);
        assertLinkage(nodynam, "CALL-NAME", ResolutionContracts.CallLinkage.DYNAMIC);
        assertLinkage(unspecified, "CALL-NAME", ResolutionContracts.CallLinkage.DYNAMIC);
    }

    @Test
    void observesExternalLiteralCallsWithoutCatalogLookup() throws Exception {
        Analysis dynamic = analyze(DYNAMIC_EXTERNAL);
        Analysis staticAnalysis = analyze(STATIC_EXTERNAL);
        Analysis unknown = analyze(UNKNOWN_LINKAGE_EXTERNAL);

        assertExternal(dynamic, "'LONG-NAME-ABC'", ResolutionContracts.CallLinkage.DYNAMIC);
        assertExternal(dynamic, "'1PROG'", ResolutionContracts.CallLinkage.DYNAMIC);
        assertExternal(dynamic, "'mixed-Child'", ResolutionContracts.CallLinkage.DYNAMIC);

        assertExternal(staticAnalysis, "'LONG-NAME-ABC'", ResolutionContracts.CallLinkage.STATIC);
        assertExternal(staticAnalysis, "'1PROG'", ResolutionContracts.CallLinkage.STATIC);
        assertExternal(staticAnalysis, "'mixed-Child'", ResolutionContracts.CallLinkage.STATIC);

        assertExternal(unknown, "'LONG-NAME-ABC'", ResolutionContracts.CallLinkage.UNKNOWN);
        assertExternal(unknown, "'1PROG'", ResolutionContracts.CallLinkage.UNKNOWN);
        assertExternal(unknown, "'mixed-Child'", ResolutionContracts.CallLinkage.UNKNOWN);

        ResolutionAnalysisReport dynamicReport = ResolutionAnalysisReport.compose(dynamic.build(),
                ResolutionAnalysisReport.FrontendState.complete(), dynamic.occurrences(), dynamic.resolution());
        ResolutionAnalysisReport staticReport = ResolutionAnalysisReport.compose(staticAnalysis.build(),
                ResolutionAnalysisReport.FrontendState.complete(), staticAnalysis.occurrences(), staticAnalysis.resolution());
        ResolutionAnalysisReport unknownReport = ResolutionAnalysisReport.compose(unknown.build(),
                ResolutionAnalysisReport.FrontendState.complete(), unknown.occurrences(), unknown.resolution());
        assertAll("external observation is complete while unknown linkage remains separate",
                () -> assertTrue(dynamicReport.completeness().dependencyAnalysisReady()),
                () -> assertTrue(staticReport.completeness().dependencyAnalysisReady()),
                () -> assertFalse(unknownReport.completeness().dependencyAnalysisReady()),
                () -> assertTrue(unknownReport.gaps().stream().anyMatch(gap ->
                        gap.code().equals("CALL_LINKAGE_UNKNOWN"))));
    }

    @Test
    void unknownCallSemanticsBlockDependencyReadinessWithoutErasingCertainNameBinding()
            throws Exception {
        Analysis analysis = analyze(UNKNOWN_LINKAGE_SAME_TARGET);
        ReferenceResolution.Entry call = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role()
                        == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .findFirst().orElseThrow();
        assertEquals(ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED, call.status());
        assertEquals(ResolutionContracts.CallLinkage.UNKNOWN,
                call.callSemantics().orElseThrow().linkage());

        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(),
                analysis.occurrences(), analysis.resolution());
        assertAll("certain nominal binding and uncertain call dependency are separate",
                () -> assertTrue(report.completeness().referenceBindingComplete()),
                () -> assertFalse(report.completeness().dependencyAnalysisReady()),
                () -> assertTrue(report.gaps().stream().anyMatch(gap ->
                        gap.code().equals("CALL_LINKAGE_UNKNOWN"))));
    }

    @Test
    void callIdentifierBindingDoesNotClaimItsRuntimeProgramTargetIsKnown() throws Exception {
        Analysis analysis = analyze(CALL_IDENTIFIER_RUNTIME_TARGET);
        ReferenceResolution.Entry call = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role()
                        == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .findFirst().orElseThrow();
        assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, call.status());
        assertEquals(ResolutionContracts.ReferenceKind.DATA,
                call.selectedCandidate().orElseThrow().kind());
        assertEquals(ResolutionContracts.CallLinkage.DYNAMIC,
                call.callSemantics().orElseThrow().linkage());

        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(
                analysis.build(), ResolutionAnalysisReport.FrontendState.complete(),
                analysis.occurrences(), analysis.resolution());
        assertAll("the data variable is bound while VALUE and runtime call target remain unknown",
                () -> assertTrue(analysis.resolution().entries().stream().allMatch(entry ->
                        entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED)),
                () -> assertFalse(report.completeness().referenceBindingComplete()),
                () -> assertFalse(report.completeness().dependencyAnalysisReady()),
                () -> assertTrue(report.gaps().stream().anyMatch(gap ->
                        gap.grammarRule().equals("dataValueClause"))),
                () -> assertTrue(report.gaps().stream().anyMatch(gap ->
                        gap.code().equals("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"))));
    }

    @Test
    void doesNotClassifyNodynamDllCallsAsStatic() throws Exception {
        Analysis analysis = analyze(DLL);
        ReferenceResolution.Entry call = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role()
                        == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .findFirst().orElseThrow();
        assertAll("NODYNAM alone is insufficient to prove static linkage",
                () -> assertEquals(ResolutionContracts.DynamMode.NODYNAM,
                        analysis.outcome().dynamMode()),
                () -> assertEquals(ResolutionContracts.DllMode.DLL, analysis.outcome().dllMode()),
                () -> assertEquals(ResolutionContracts.DllMode.DLL,
                        analysis.resolution().policy().dllMode()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED, call.status()),
                () -> assertEquals(ResolutionContracts.CallLinkage.DLL,
                        call.callSemantics().orElseThrow().linkage()));
    }

    @Test
    void rejectsTheInvalidDynamDllCompilerOptionCombination() throws Exception {
        Analysis analysis = analyze(INVALID_DYNAM_DLL);
        ReferenceResolution.Entry call = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role()
                        == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .findFirst().orElseThrow();
        assertAll("DYNAM and DLL are not a supported certain call configuration",
                () -> assertEquals(ResolutionContracts.DynamMode.DYNAM,
                        analysis.resolution().policy().dynamMode()),
                () -> assertEquals(ResolutionContracts.DllMode.DLL,
                        analysis.resolution().policy().dllMode()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNSUPPORTED, call.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION,
                        call.reason()),
                () -> assertEquals(ResolutionContracts.CallLinkage.UNKNOWN,
                        call.callSemantics().orElseThrow().linkage()));
    }

    private static void assertExternal(Analysis analysis, String writtenText,
                                       ResolutionContracts.CallLinkage linkage) {
        ReferenceResolution.Entry entry = analysis.resolution().entries().stream()
                .filter(candidate -> candidate.occurrence().writtenText().equals(writtenText))
                .findFirst().orElseThrow();
        assertAll(entry.toString(),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.LITERAL_EXTERNAL_PROGRAM, entry.reason()),
                () -> assertEquals(linkage, entry.callSemantics().orElseThrow().linkage()),
                () -> assertTrue(entry.candidates().isEmpty()),
                () -> assertTrue(entry.diagnosticIds().isEmpty()));
    }

    private static void assertCallSyntax(Ast.Program program) {
        List<Ast.CallStatement> calls = nodes(program, Ast.CallStatement.class);
        assertEquals(2, calls.size());
        assertEquals(Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME, calls.get(0).targetSyntax());
        assertEquals(Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION, calls.get(1).targetSyntax());
        AstSnapshot.Metrics metrics = AstSnapshot.from(program).metrics();
        assertEquals(1, metrics.literalTargetCalls());
        assertEquals(1, metrics.identifierTargetCalls());
    }

    private static void assertLinkage(Analysis analysis, String writtenText,
                                      ResolutionContracts.CallLinkage expected) {
        ReferenceResolution.Entry entry = analysis.resolution().entries().stream()
                .filter(candidate -> candidate.occurrence().role()
                        == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .filter(candidate -> candidate.occurrence().writtenText().equals(writtenText))
                .findFirst().orElseThrow();
        assertEquals(expected, entry.callSemantics().orElseThrow().linkage(), entry.toString());
    }

    private static Analysis analyze(Path sourcePath) throws Exception {
        GrammarBinding grammar = Bindings.cobol();
        String normalized = SourceNormalizerTestSupport.fixed(Files.readString(sourcePath, StandardCharsets.UTF_8));
        PreprocessorEngine.Outcome outcome = new PreprocessorEngine(
                grammar, new CopybookLibrary(Path.of("src/test/resources")))
                .process(SourceMap.identity(normalized, sourcePath.getFileName().toString()),
                        sourcePath.getFileName().toString());
        Parser parser = grammar.cobolParser(new CommonTokenStream(grammar.cobolLexer(
                CharStreams.fromString(outcome.text(), sourcePath.getFileName().toString()))));
        ParseTree tree = grammar.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(
                parser, outcome.text(), outcome.sourceMap(), ids, sizes)
                .buildCompilationUnit(tree, sourcePath.getFileName().toString());
        CompilationUnitModel model = build.compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector().collect(
                    unit.id(), unit.program(), AstScopeIndex.build(unit.program(), table)));
        }
        ResolutionContracts.CobolResolutionPolicy policy = ResolutionContracts.CobolResolutionPolicy.initial()
                .withPgmnameMode(outcome.pgmnameMode()).withDynamMode(outcome.dynamMode())
                .withDllMode(outcome.dllMode());
        ReferenceResolution resolution = new CobolReferenceResolver(policy)
                .resolve(model, tables, occurrences);
        return new Analysis(outcome, build, model, occurrences, resolution);
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

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        java.util.ArrayList<T> result = new java.util.ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return List.copyOf(result);
    }

    private static Set<String> components(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName)
                .map(name -> name.toLowerCase(Locale.ROOT).equals("dynammode") ? "dynamMode" : name)
                .collect(Collectors.toSet());
    }

    private record Analysis(PreprocessorEngine.Outcome outcome, CompilationUnitBuildResult build,
                            CompilationUnitModel model,
                            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences,
                            ReferenceResolution resolution) { }
}
