package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.boundary.ExperimentalCobolCallBoundary;
import io.github.gustavo2358.cobolexplorer.semanticproduct.consumer.ExperimentalCobolCallConsumer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executable falsification of the Checkpoint 2 A2+B seam for literal CALL only.
 * The adapter is intentionally in this frontend package; the consumer is not.
 */
class SemanticProductBoundaryCheckpoint3ATest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/resolution/call-linkage-unspecified.cbl");
    private static final String PROJECT_PREFIX = "io/github/gustavo2358/cobolexplorer/";
    private static final String EXPERIMENT_PREFIX =
            "io/github/gustavo2358/cobolexplorer/semanticproduct/";

    @Test
    void closureSurvivesAfterFrontendAnalysisIsReleased() throws Exception {
        FrontendAnalysis frontend = analyze(FIXTURE);
        ExperimentalCobolCallBoundary.State state = adapt(frontend);
        ExperimentalCobolCallBoundary.Port port = ExperimentalCobolCallBoundary.open(state);

        frontend = null;
        ExperimentalCobolCallConsumer.Consumption consumed =
                ExperimentalCobolCallConsumer.consume(port);

        assertAllCallFacts(consumed);
        assertTrue(Arrays.stream(port.getClass().getDeclaredFields())
                        .allMatch(field -> field.getType().getName()
                                .startsWith("io.github.gustavo2358.cobolexplorer.semanticproduct")),
                "port must retain only boundary state, never a frontend provider");
    }

    @Test
    void consumerHasNoFrontendTypeLeakage() throws Exception {
        assertNoFrontendDependenciesRecursively(ExperimentalCobolCallBoundary.class);
        assertNoFrontendDependenciesRecursively(ExperimentalCobolCallConsumer.class);
    }

    @Test
    void consumerDoesNotNeedWrittenTextOrGrammarMetadata() throws Exception {
        ExperimentalCobolCallBoundary.UnitId unit = new ExperimentalCobolCallBoundary.UnitId(
                "standalone.cbl", List.of(0), "CALLER");
        ExperimentalCobolCallBoundary.Provenance provenance = provenance("standalone.cbl");
        ExperimentalCobolCallBoundary.CallFact fact = new ExperimentalCobolCallBoundary.CallFact(
                new ExperimentalCobolCallBoundary.CallSiteId(unit, 0), "TARGET-A",
                ExperimentalCobolCallBoundary.TargetSyntax.LITERAL_PROGRAM_NAME,
                ExperimentalCobolCallBoundary.ResolutionStatus.EXTERNAL_OBSERVED,
                ExperimentalCobolCallBoundary.ResolutionReason.LITERAL_EXTERNAL_PROGRAM,
                ExperimentalCobolCallBoundary.Linkage.UNKNOWN, provenance);
        ExperimentalCobolCallBoundary.State state = new ExperimentalCobolCallBoundary.State(
                "standalone-generation", unit,
                new ExperimentalCobolCallBoundary.Policy("test-policy", "1",
                        ExperimentalCobolCallBoundary.DynamMode.UNSPECIFIED,
                        ExperimentalCobolCallBoundary.DllMode.UNSPECIFIED),
                new ExperimentalCobolCallBoundary.AnalysisState(
                        ExperimentalCobolCallBoundary.Claim.INCOMPLETE,
                        ExperimentalCobolCallBoundary.Availability.AVAILABLE,
                        List.of(new ExperimentalCobolCallBoundary.Uncertainty(
                                "CALL_LINKAGE_UNKNOWN", "compiler options were not supplied"))),
                List.of(fact));

        ExperimentalCobolCallConsumer.Consumption consumed =
                ExperimentalCobolCallConsumer.consume(ExperimentalCobolCallBoundary.open(state));

        assertEquals("TARGET-A", consumed.calls().get(0).observedTarget());
        assertEquals(ExperimentalCobolCallConsumer.Linkage.UNKNOWN,
                consumed.calls().get(0).linkage());
        assertEquals(ExperimentalCobolCallConsumer.RuntimeTargetKnowledge.UNKNOWN,
                consumed.calls().get(0).runtimeTarget());
        assertEquals("CALL_LINKAGE_UNKNOWN", consumed.uncertainties().get(0).code());

        for (Class<?> type : List.of(ExperimentalCobolCallBoundary.class,
                ExperimentalCobolCallConsumer.class)) {
            String source = Files.readString(sourcePath(type), StandardCharsets.UTF_8);
            assertFalse(source.contains("writtenText"), type.getName());
            assertFalse(source.contains("grammarRule"), type.getName());
            assertFalse(source.contains("ParseTree"), type.getName());
            assertFalse(source.contains("org.antlr"), type.getName());
        }
    }

    @Test
    void adapterPublishesKnownLiteralAndPreservesUnknownLinkage() throws Exception {
        ExperimentalCobolCallBoundary.State first = adapt(analyze(FIXTURE));
        ExperimentalCobolCallBoundary.State second = adapt(analyze(FIXTURE));

        assertEquals(first, second, "same input must publish the same boundary state");
        assertEquals(1, first.literalCalls().size(), "dynamic CALL is outside this literal slice");
        ExperimentalCobolCallBoundary.CallFact fact = first.literalCalls().get(0);
        assertEquals("TARGET-A", fact.observedTarget());
        assertEquals(ExperimentalCobolCallBoundary.TargetSyntax.LITERAL_PROGRAM_NAME,
                fact.targetSyntax());
        assertEquals(ExperimentalCobolCallBoundary.ResolutionStatus.EXTERNAL_OBSERVED,
                fact.status());
        assertEquals(ExperimentalCobolCallBoundary.ResolutionReason.LITERAL_EXTERNAL_PROGRAM,
                fact.reason());
        assertEquals(ExperimentalCobolCallBoundary.Linkage.UNKNOWN, fact.linkage());
        assertEquals(ExperimentalCobolCallBoundary.DynamMode.UNSPECIFIED,
                first.policy().dynamMode());
        assertEquals(ExperimentalCobolCallBoundary.DllMode.UNSPECIFIED,
                first.policy().dllMode());
        assertEquals(ExperimentalCobolCallBoundary.Claim.INCOMPLETE, first.analysis().claim());
        assertEquals("CALL_LINKAGE_UNKNOWN", first.analysis().uncertainties().get(0).code());
        assertEquals(FIXTURE.getFileName().toString(), fact.provenance().original().file());
        assertTrue(fact.provenance().exact());
    }

    @Test
    void stateAndPortAreReadOnlyBoundaryValues() throws Exception {
        ExperimentalCobolCallBoundary.State state = adapt(analyze(FIXTURE));
        ExperimentalCobolCallBoundary.Port port = ExperimentalCobolCallBoundary.open(state);

        assertThrows(UnsupportedOperationException.class,
                () -> state.literalCalls().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> port.literalCalls().clear());
        assertEquals(state.analysisGeneration(), port.analysisGeneration());
        assertEquals(state.unit(), port.unit());
        assertEquals(state.policy(), port.policy());
    }

    private static void assertAllCallFacts(ExperimentalCobolCallConsumer.Consumption consumed) {
        assertEquals("call-linkage-unspecified.cbl", consumed.calls().get(0).sourceFile());
        assertEquals("TARGET-A", consumed.calls().get(0).observedTarget());
        assertEquals(ExperimentalCobolCallConsumer.ObservedCallKind.EXTERNAL_LITERAL,
                consumed.calls().get(0).kind());
        assertEquals(ExperimentalCobolCallConsumer.Linkage.UNKNOWN,
                consumed.calls().get(0).linkage());
        assertEquals(ExperimentalCobolCallConsumer.RuntimeTargetKnowledge.UNKNOWN,
                consumed.calls().get(0).runtimeTarget());
        assertEquals(ExperimentalCobolCallConsumer.Claim.INCOMPLETE, consumed.claim());
        assertEquals("CALL_LINKAGE_UNKNOWN", consumed.uncertainties().get(0).code());
    }

    private static ExperimentalCobolCallBoundary.State adapt(FrontendAnalysis frontend) {
        CompilationUnitModel.ProgramUnit unit = frontend.model().programUnits().get(0);
        ExperimentalCobolCallBoundary.UnitId boundaryUnit = new ExperimentalCobolCallBoundary.UnitId(
                unit.id().compilationUnitId(), unit.id().structuralPath(),
                unit.id().canonicalProgramName());
        List<ExperimentalCobolCallBoundary.CallFact> calls = new ArrayList<>();
        for (Ast.Node node : nodes(unit.program())) {
            if (!(node instanceof Ast.CallStatement call)
                    || call.targetSyntax() != Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME)
                continue;
            if (!(call.target() instanceof Ast.ProgramReference target))
                throw new IllegalStateException("literal CALL must have a program reference");

            ReferenceResolution.Entry entry = frontend.resolution().entries().stream()
                    .filter(candidate -> candidate.occurrence().programUnitId().equals(unit.id()))
                    .filter(candidate -> candidate.occurrence().referenceAstNodeId() == target.meta().id())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "CALL target occurrence was not published by the frontend"));
            calls.add(new ExperimentalCobolCallBoundary.CallFact(
                    new ExperimentalCobolCallBoundary.CallSiteId(boundaryUnit,
                            entry.occurrence().id()),
                    target.programName(), mapTargetSyntax(call.targetSyntax()),
                    mapStatus(entry.status()), mapReason(entry.reason()),
                    mapLinkage(entry.callSemantics().orElseThrow().linkage()),
                    mapProvenance(target.meta().provenance())));
        }

        List<ExperimentalCobolCallBoundary.Uncertainty> uncertainties = calls.stream()
                .filter(call -> call.linkage() == ExperimentalCobolCallBoundary.Linkage.UNKNOWN)
                .map(call -> new ExperimentalCobolCallBoundary.Uncertainty(
                        "CALL_LINKAGE_UNKNOWN", "linkage depends on unavailable compiler options"))
                .distinct()
                .toList();
        ExperimentalCobolCallBoundary.AnalysisState analysis = new ExperimentalCobolCallBoundary.AnalysisState(
                uncertainties.isEmpty() ? ExperimentalCobolCallBoundary.Claim.COMPLETE
                        : ExperimentalCobolCallBoundary.Claim.INCOMPLETE,
                ExperimentalCobolCallBoundary.Availability.AVAILABLE, uncertainties);
        ResolutionContracts.CobolResolutionPolicy policy = frontend.resolution().policy();
        return new ExperimentalCobolCallBoundary.State(
                "test-generation/" + frontend.model().compilationUnitId(), boundaryUnit,
                new ExperimentalCobolCallBoundary.Policy(policy.policyId(), policy.version(),
                        mapDynamMode(policy.dynamMode()), mapDllMode(policy.dllMode())),
                analysis, calls);
    }

    private static ExperimentalCobolCallBoundary.TargetSyntax mapTargetSyntax(
            Ast.CallTargetSyntax syntax) {
        if (syntax != Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME)
            throw new IllegalArgumentException("only literal CALL is in this experiment");
        return ExperimentalCobolCallBoundary.TargetSyntax.LITERAL_PROGRAM_NAME;
    }

    private static ExperimentalCobolCallBoundary.ResolutionStatus mapStatus(
            ResolutionContracts.ResolutionStatus status) {
        return ExperimentalCobolCallBoundary.ResolutionStatus.valueOf(status.name());
    }

    private static ExperimentalCobolCallBoundary.ResolutionReason mapReason(
            ResolutionContracts.ResolutionReason reason) {
        return ExperimentalCobolCallBoundary.ResolutionReason.valueOf(reason.name());
    }

    private static ExperimentalCobolCallBoundary.Linkage mapLinkage(
            ResolutionContracts.CallLinkage linkage) {
        return ExperimentalCobolCallBoundary.Linkage.valueOf(linkage.name());
    }

    private static ExperimentalCobolCallBoundary.DynamMode mapDynamMode(
            ResolutionContracts.DynamMode mode) {
        return ExperimentalCobolCallBoundary.DynamMode.valueOf(mode.name());
    }

    private static ExperimentalCobolCallBoundary.DllMode mapDllMode(
            ResolutionContracts.DllMode mode) {
        return ExperimentalCobolCallBoundary.DllMode.valueOf(mode.name());
    }

    private static ExperimentalCobolCallBoundary.Provenance mapProvenance(
            Ast.SourceProvenance provenance) {
        return new ExperimentalCobolCallBoundary.Provenance(
                mapLocation(provenance.expanded()), mapLocation(provenance.original()),
                provenance.includeChain().stream()
                        .map(frame -> new ExperimentalCobolCallBoundary.IncludeFrame(
                                frame.includingFile(), frame.requestedName(),
                                frame.includedFile(), frame.includeLine()))
                        .toList(), provenance.exact());
    }

    private static ExperimentalCobolCallBoundary.Location mapLocation(Ast.SourceLocation location) {
        return new ExperimentalCobolCallBoundary.Location(location.file(), location.startLine(),
                location.startColumn(), location.endLine(), location.endColumn());
    }

    private static ExperimentalCobolCallBoundary.Provenance provenance(String file) {
        ExperimentalCobolCallBoundary.Location location =
                new ExperimentalCobolCallBoundary.Location(file, 5, 13, 5, 29);
        return new ExperimentalCobolCallBoundary.Provenance(location, location, List.of(), true);
    }

    private static FrontendAnalysis analyze(Path sourcePath) throws Exception {
        GrammarBinding grammar = Bindings.cobol();
        String normalized = SourceNormalizerTestSupport.fixed(
                Files.readString(sourcePath, StandardCharsets.UTF_8));
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
        return new FrontendAnalysis(outcome, model, resolution);
    }

    private static List<Ast.Node> nodes(Ast.Node root) {
        List<Ast.Node> result = new ArrayList<>();
        result.add(root);
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child));
        return result;
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

    private static void assertNoFrontendDependenciesRecursively(Class<?> type) throws IOException {
        assertNoFrontendDependencies(type);
        for (Class<?> nested : type.getDeclaredClasses())
            assertNoFrontendDependenciesRecursively(nested);
    }

    private static void assertNoFrontendDependencies(Class<?> type) throws IOException {
        for (String reference : classReferences(type)) {
            assertFalse(reference.startsWith(PROJECT_PREFIX)
                            && !reference.startsWith(EXPERIMENT_PREFIX),
                    () -> type.getName() + " leaks frontend type " + reference);
            assertFalse(reference.startsWith("org/antlr/v4/"),
                    () -> type.getName() + " leaks ANTLR type " + reference);
        }
    }

    private static Path sourcePath(Class<?> type) {
        return Path.of("src/test/java").resolve(type.getName().replace('.', '/') + ".java");
    }

    private static Set<String> classReferences(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream raw = type.getResourceAsStream(resource)) {
            assertTrue(raw != null, "bytecode não encontrado para " + type.getName());
            return classReferences(new DataInputStream(raw));
        }
    }

    private static Set<String> classReferences(DataInputStream input) throws IOException {
        assertEquals(0xCAFEBABE, input.readInt(), "classfile inválido");
        input.readUnsignedShort();
        input.readUnsignedShort();
        int count = input.readUnsignedShort();
        Map<Integer, String> utf8 = new HashMap<>();
        Map<Integer, Integer> classNameIndexes = new HashMap<>();
        for (int index = 1; index < count; index++) {
            switch (input.readUnsignedByte()) {
                case 1 -> utf8.put(index, input.readUTF());
                case 3, 4 -> input.readInt();
                case 5, 6 -> {
                    input.readLong();
                    index++;
                }
                case 7 -> classNameIndexes.put(index, input.readUnsignedShort());
                case 8, 16, 19, 20 -> input.readUnsignedShort();
                case 9, 10, 11, 12, 17, 18 -> {
                    input.readUnsignedShort();
                    input.readUnsignedShort();
                }
                case 15 -> {
                    input.readUnsignedByte();
                    input.readUnsignedShort();
                }
                default -> throw new IOException("tag de constant pool não suportada");
            }
        }
        Set<String> result = new LinkedHashSet<>();
        for (int nameIndex : classNameIndexes.values()) {
            String name = utf8.get(nameIndex);
            if (name != null) result.add(name);
        }
        return result;
    }

    private record FrontendAnalysis(PreprocessorEngine.Outcome outcome,
                                    CompilationUnitModel model,
                                    ReferenceResolution resolution) { }
}
