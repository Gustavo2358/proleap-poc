package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.boundary.ExperimentalCobolMoveCallBoundary;
import io.github.gustavo2358.cobolexplorer.semanticproduct.consumer.ExperimentalCobolMoveCallConsumer;
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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executable falsification of the A2+B seam for the first MOVE-to-CALL slice.
 * Frontend knowledge is confined to the adapter in this test package.
 */
class SemanticProductBoundaryCheckpoint3BTest {
    private static final String SOURCE_NAME = "semantic-product-checkpoint-3b.cbl";
    private static final String COMPILATION_UNIT_ID = SOURCE_NAME.toUpperCase(Locale.ROOT);
    private static final String SOURCE = String.join("\n",
            "       IDENTIFICATION DIVISION.",
            "       PROGRAM-ID. SEMANTIC-3B.",
            "       DATA DIVISION.",
            "       WORKING-STORAGE SECTION.",
            "       01 WS-PGM PIC X(8).",
            "       PROCEDURE DIVISION.",
            "           MOVE 'PGMA' TO WS-PGM.",
            "           CALL WS-PGM.",
            "           GOBACK.",
            "       END PROGRAM SEMANTIC-3B.", "");
    private static final String PROJECT_PREFIX = "io/github/gustavo2358/cobolexplorer/";
    private static final String EXPERIMENT_PREFIX =
            "io/github/gustavo2358/cobolexplorer/semanticproduct/";

    @Test
    void independentConsumerReconstructsTheSliceAfterFrontendIsReleased() {
        FrontendAnalysis frontend = analyze(SOURCE, SOURCE_NAME);
        ExperimentalCobolMoveCallBoundary.State state = adapt(frontend);
        ExperimentalCobolMoveCallBoundary.Port port =
                ExperimentalCobolMoveCallBoundary.open(state);

        frontend = null;
        ExperimentalCobolMoveCallConsumer.Reconstruction reconstruction =
                ExperimentalCobolMoveCallConsumer.consume(port);

        ExperimentalCobolMoveCallBoundary.DataItemId dataId = reconstruction.data().identity();
        assertEquals("WS-PGM", reconstruction.data().name());
        assertEquals("X(8)", reconstruction.data().picture());
        assertEquals(dataId, reconstruction.move().target());
        assertEquals(dataId, reconstruction.call().operand());
        assertEquals("PGMA", reconstruction.move().literal());
        assertEquals(ExperimentalCobolMoveCallConsumer.RuntimeTargetKnowledge.UNKNOWN,
                reconstruction.call().runtimeTarget());
        assertTrue(reconstruction.movePrecedesCall());
        assertEquals(List.of("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"),
                reconstruction.uncertaintyCodes());

        assertEquals(SOURCE_NAME, reconstruction.data().declaration().file());
        assertEquals(SOURCE_NAME, reconstruction.move().literalSource().file());
        assertEquals(SOURCE_NAME, reconstruction.move().statement().file());
        assertEquals(SOURCE_NAME, reconstruction.call().statement().file());
        assertTrue(reconstruction.data().declaration().line()
                        < reconstruction.move().statement().line());
        assertTrue(reconstruction.move().statement().line()
                        < reconstruction.call().statement().line());
        assertTrue(reconstruction.data().declaration().exact());
        assertTrue(reconstruction.move().statement().exact());
        assertTrue(reconstruction.call().statement().exact());

        assertEquals(ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE,
                state.move().targetBinding());
        assertEquals(ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE,
                state.call().operandBinding());
        assertEquals(ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE,
                state.analysis().nominalBinding());
        assertEquals(ExperimentalCobolMoveCallBoundary.RuntimeTargetKnowledge.UNKNOWN,
                state.analysis().runtimeTarget());
        assertEquals(List.of(ExperimentalCobolMoveCallBoundary.UncertaintyScope.RUNTIME_CALL_TARGET),
                state.analysis().uncertainties().stream()
                        .map(ExperimentalCobolMoveCallBoundary.Uncertainty::scope).toList());

        // The identity is typed and namespaced, not a free-standing local integer.
        assertEquals(COMPILATION_UNIT_ID, dataId.unit().compilationUnitId());
        assertEquals(List.of(0), dataId.unit().structuralPath());
        assertEquals("SEMANTIC-3B", dataId.unit().canonicalProgramName());
    }

    @Test
    void adapterPublishesCanonicalFactsDeterministically() {
        ExperimentalCobolMoveCallBoundary.State first = adapt(analyze(SOURCE, SOURCE_NAME));
        ExperimentalCobolMoveCallBoundary.State second = adapt(analyze(SOURCE, SOURCE_NAME));

        assertEquals(first, second);
        assertEquals(1, first.dataItems().size());
        assertEquals(ExperimentalCobolMoveCallBoundary.UncertaintyScope.RUNTIME_CALL_TARGET,
                first.analysis().uncertainties().get(0).scope());
        assertEquals(first.move().target(), first.call().operand());
        assertEquals(first.move().point(), first.ordering().earlier());
        assertEquals(first.call().point(), first.ordering().later());
    }

    @Test
    void consumerNeedsNoFrontendOrSemanticTextMetadata() throws Exception {
        assertNoFrontendDependenciesRecursively(ExperimentalCobolMoveCallBoundary.class);
        assertNoFrontendDependenciesRecursively(ExperimentalCobolMoveCallConsumer.class);
        for (Class<?> type : List.of(ExperimentalCobolMoveCallBoundary.class,
                ExperimentalCobolMoveCallConsumer.class)) {
            String source = Files.readString(sourcePath(type), StandardCharsets.UTF_8);
            assertFalse(source.contains("writtenText"), type.getName());
            assertFalse(source.contains("grammarRule"), type.getName());
            assertFalse(source.contains("ParseTree"), type.getName());
            assertFalse(source.contains("org.antlr"), type.getName());
        }
    }

    @Test
    void consumerCanOperateOnBoundaryStateConstructedWithoutFrontend() {
        ExperimentalCobolMoveCallBoundary.UnitId unit =
                new ExperimentalCobolMoveCallBoundary.UnitId(
                        "standalone.cbl", List.of(0), "STANDALONE");
        ExperimentalCobolMoveCallBoundary.DataItemId data =
                new ExperimentalCobolMoveCallBoundary.DataItemId(unit, 4);
        ExperimentalCobolMoveCallBoundary.Provenance declarationProvenance = provenance(
                "standalone.cbl", 5);
        ExperimentalCobolMoveCallBoundary.Provenance moveProvenance = provenance(
                "standalone.cbl", 7);
        ExperimentalCobolMoveCallBoundary.Provenance literalProvenance = provenance(
                "standalone.cbl", 7);
        ExperimentalCobolMoveCallBoundary.Provenance callProvenance = provenance(
                "standalone.cbl", 8);
        ExperimentalCobolMoveCallBoundary.ProgramPoint movePoint =
                new ExperimentalCobolMoveCallBoundary.ProgramPoint(2);
        ExperimentalCobolMoveCallBoundary.ProgramPoint callPoint =
                new ExperimentalCobolMoveCallBoundary.ProgramPoint(3);
        ExperimentalCobolMoveCallBoundary.State state = new ExperimentalCobolMoveCallBoundary.State(
                unit,
                List.of(new ExperimentalCobolMoveCallBoundary.DataDeclaration(
                        data, "WS-PGM", "X(8)", declarationProvenance)),
                new ExperimentalCobolMoveCallBoundary.MoveFact(
                        movePoint,
                        new ExperimentalCobolMoveCallBoundary.LiteralSource(
                                "PGMA", literalProvenance),
                        data, ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE,
                        moveProvenance),
                new ExperimentalCobolMoveCallBoundary.CallFact(
                        callPoint, data,
                        ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE,
                        ExperimentalCobolMoveCallBoundary.RuntimeTargetKnowledge.UNKNOWN,
                        callProvenance),
                new ExperimentalCobolMoveCallBoundary.Ordering(movePoint, callPoint),
                new ExperimentalCobolMoveCallBoundary.AnalysisStatus(
                        ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE,
                        ExperimentalCobolMoveCallBoundary.RuntimeTargetKnowledge.UNKNOWN,
                        List.of(new ExperimentalCobolMoveCallBoundary.Uncertainty(
                                callPoint,
                                ExperimentalCobolMoveCallBoundary.UncertaintyScope.RUNTIME_CALL_TARGET,
                                "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN",
                                "CALL operand binding does not determine its runtime program target"))));

        ExperimentalCobolMoveCallConsumer.Reconstruction reconstruction =
                ExperimentalCobolMoveCallConsumer.consume(
                        ExperimentalCobolMoveCallBoundary.open(state));
        assertEquals("PGMA", reconstruction.move().literal());
        assertEquals(data, reconstruction.call().operand());
        assertTrue(reconstruction.movePrecedesCall());
        assertEquals(List.of("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"),
                reconstruction.uncertaintyCodes());
    }

    @Test
    void stateAndPortAreReadOnlyBoundaryValues() {
        ExperimentalCobolMoveCallBoundary.State state = adapt(analyze(SOURCE, SOURCE_NAME));
        ExperimentalCobolMoveCallBoundary.Port port =
                ExperimentalCobolMoveCallBoundary.open(state);

        assertThrows(UnsupportedOperationException.class,
                () -> state.dataItems().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> port.dataItems().clear());
        assertEquals(state.move(), port.move());
        assertEquals(state.call(), port.call());
        assertEquals(state.ordering(), port.ordering());
    }

    private static ExperimentalCobolMoveCallBoundary.State adapt(FrontendAnalysis frontend) {
        CompilationUnitModel.ProgramUnit unit = frontend.model().programUnits().get(0);
        ExperimentalCobolMoveCallBoundary.UnitId boundaryUnit =
                new ExperimentalCobolMoveCallBoundary.UnitId(
                        unit.id().compilationUnitId(), unit.id().structuralPath(),
                        unit.id().canonicalProgramName());
        Map<Integer, Ast.Node> nodesById = new LinkedHashMap<>();
        for (Ast.Node node : nodes(unit.program()))
            require(nodesById.put(node.meta().id(), node) == null,
                    "duplicate AST node id");

        SymbolTable table = frontend.tables().forProgramUnit(unit.id())
                .orElseThrow().symbolTable();
        List<ExperimentalCobolMoveCallBoundary.DataDeclaration> dataItems = table.symbols()
                .stream()
                .filter(symbol -> symbol.namespace() == SymbolTable.Namespace.DATA)
                .filter(symbol -> symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM)
                .map(symbol -> dataDeclaration(boundaryUnit, symbol, nodesById))
                .toList();

        List<Ast.Statement> statements = nodes(unit.program()).stream()
                .filter(Ast.Statement.class::isInstance)
                .map(Ast.Statement.class::cast).toList();
        require(statements.stream().filter(Ast.MoveStatement.class::isInstance).count() == 1,
                "slice must contain one MOVE");
        require(statements.stream().filter(Ast.CallStatement.class::isInstance).count() == 1,
                "slice must contain one CALL");
        Ast.MoveStatement move = statements.stream().filter(Ast.MoveStatement.class::isInstance)
                .map(Ast.MoveStatement.class::cast).findFirst().orElseThrow();
        Ast.CallStatement call = statements.stream().filter(Ast.CallStatement.class::isInstance)
                .map(Ast.CallStatement.class::cast).findFirst().orElseThrow();
        IdentityHashMap<Ast.Statement, ExperimentalCobolMoveCallBoundary.ProgramPoint> points =
                new IdentityHashMap<>();
        for (int index = 0; index < statements.size(); index++)
            points.put(statements.get(index),
                    new ExperimentalCobolMoveCallBoundary.ProgramPoint(index));

        Ast.LiteralExpression literal = requireInstance(Ast.LiteralExpression.class, move.source(),
                "MOVE source must be a literal expression");
        require(move.targets().size() == 1, "slice must contain one MOVE target");
        Ast.DataReference moveTarget = requireInstance(Ast.DataReference.class,
                move.targets().get(0), "MOVE target must be a data reference");
        Ast.DataReference callOperand = requireInstance(Ast.DataReference.class, call.target(),
                "CALL operand must be a data reference");
        require(call.targetSyntax() == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION,
                "slice CALL must use identifier/expression syntax");

        ReferenceResolution.Entry moveEntry = entryFor(frontend.resolution(), unit.id(),
                moveTarget.meta().id());
        ReferenceResolution.Entry callEntry = entryFor(frontend.resolution(), unit.id(),
                callOperand.meta().id());
        require(moveEntry.occurrence().role() == ResolutionContracts.ReferenceRole.VALUE_WRITE,
                "MOVE target role must come from canonical occurrence");
        require(callEntry.occurrence().role() == ResolutionContracts.ReferenceRole.CALL_TARGET,
                "CALL operand role must come from canonical occurrence");
        ExperimentalCobolMoveCallBoundary.DataItemId moveData =
                resolvedDataItem(moveEntry, boundaryUnit);
        ExperimentalCobolMoveCallBoundary.DataItemId callData =
                resolvedDataItem(callEntry, boundaryUnit);
        require(moveData.equals(callData), "frontend bindings must agree on data identity");

        List<ResolutionAnalysisReport.Gap> callGaps = frontend.report().gaps().stream()
                .filter(gap -> gap.programUnitId() != null && gap.programUnitId().equals(unit.id()))
                .filter(gap -> gap.occurrenceId() == callEntry.occurrence().id())
                .toList();
        require(callGaps.size() == 1
                        && callGaps.get(0).code().equals("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"),
                "runtime target uncertainty must be published by the frontend report");
        ResolutionAnalysisReport.Gap runtimeGap = callGaps.get(0);

        ExperimentalCobolMoveCallBoundary.ProgramPoint movePoint = points.get(move);
        ExperimentalCobolMoveCallBoundary.ProgramPoint callPoint = points.get(call);
        require(movePoint != null && callPoint != null,
                "semantic statement order must assign both program points");
        ExperimentalCobolMoveCallBoundary.RuntimeTargetKnowledge runtimeTarget =
                ExperimentalCobolMoveCallBoundary.RuntimeTargetKnowledge.UNKNOWN;
        require(callEntry.callSemantics().orElseThrow().targetSyntax()
                        == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION,
                "CALL semantics must preserve dynamic target syntax");

        return new ExperimentalCobolMoveCallBoundary.State(
                boundaryUnit,
                dataItems,
                new ExperimentalCobolMoveCallBoundary.MoveFact(
                        movePoint,
                        new ExperimentalCobolMoveCallBoundary.LiteralSource(
                                literal.value(), mapProvenance(literal.meta().provenance())),
                        moveData, mapBinding(moveEntry),
                        mapProvenance(move.meta().provenance())),
                new ExperimentalCobolMoveCallBoundary.CallFact(
                        callPoint, callData, mapBinding(callEntry), runtimeTarget,
                        mapProvenance(call.meta().provenance())),
                new ExperimentalCobolMoveCallBoundary.Ordering(movePoint, callPoint),
                new ExperimentalCobolMoveCallBoundary.AnalysisStatus(
                        nominalBinding(moveEntry, callEntry), runtimeTarget,
                        List.of(new ExperimentalCobolMoveCallBoundary.Uncertainty(
                                callPoint,
                                ExperimentalCobolMoveCallBoundary.UncertaintyScope.RUNTIME_CALL_TARGET,
                                runtimeGap.code(), runtimeGap.message()))));
    }

    private static ExperimentalCobolMoveCallBoundary.DataDeclaration dataDeclaration(
            ExperimentalCobolMoveCallBoundary.UnitId unit, SymbolTable.Symbol symbol,
            Map<Integer, Ast.Node> nodesById) {
        Ast.DataEntry entry = requireInstance(Ast.DataEntry.class,
                nodesById.get(symbol.declarationAstNodeId()),
                "DATA symbol must point to a typed data declaration");
        Ast.PictureClause picture = entry.clauses().stream()
                .filter(Ast.PictureClause.class::isInstance)
                .map(Ast.PictureClause.class::cast).findFirst()
                .orElseThrow(() -> new IllegalStateException("WS-PGM must publish its PIC clause"));
        return new ExperimentalCobolMoveCallBoundary.DataDeclaration(
                new ExperimentalCobolMoveCallBoundary.DataItemId(unit, symbol.id()),
                symbol.canonicalName(), picture.picture(),
                mapProvenance(entry.meta().provenance()));
    }

    private static ExperimentalCobolMoveCallBoundary.DataItemId resolvedDataItem(
            ReferenceResolution.Entry entry,
            ExperimentalCobolMoveCallBoundary.UnitId unit) {
        require(entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED,
                "slice reference must be nominally resolved");
        ReferenceResolution.Candidate candidate = entry.selectedCandidate().orElseThrow();
        require(candidate.kind() == ResolutionContracts.ReferenceKind.DATA,
                "slice reference must resolve as DATA");
        require(candidate.entityId().domain()
                        == ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                "slice reference must resolve to a DATA symbol identity");
        require(candidate.entityId().programUnitId().equals(
                        new ResolutionContracts.ProgramUnitId(unit.compilationUnitId(),
                                unit.structuralPath(), unit.canonicalProgramName())),
                "DATA identity must remain in its namespaced unit");
        return new ExperimentalCobolMoveCallBoundary.DataItemId(unit,
                candidate.entityId().localId());
    }

    private static ExperimentalCobolMoveCallBoundary.BindingStatus nominalBinding(
            ReferenceResolution.Entry move, ReferenceResolution.Entry call) {
        return mapBinding(move) == ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE
                && mapBinding(call) == ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE
                ? ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE
                : ExperimentalCobolMoveCallBoundary.BindingStatus.INCOMPLETE;
    }

    private static ExperimentalCobolMoveCallBoundary.BindingStatus mapBinding(
            ReferenceResolution.Entry entry) {
        return entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED
                ? ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE
                : ExperimentalCobolMoveCallBoundary.BindingStatus.INCOMPLETE;
    }

    private static ReferenceResolution.Entry entryFor(ReferenceResolution resolution,
                                                      ResolutionContracts.ProgramUnitId unit,
                                                      int astNodeId) {
        return resolution.entries().stream()
                .filter(entry -> entry.occurrence().programUnitId().equals(unit))
                .filter(entry -> entry.occurrence().referenceAstNodeId() == astNodeId)
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "typed reference has no published resolution entry"));
    }

    private static ExperimentalCobolMoveCallBoundary.Provenance mapProvenance(
            Ast.SourceProvenance provenance) {
        return new ExperimentalCobolMoveCallBoundary.Provenance(
                mapLocation(provenance.expanded()), mapLocation(provenance.original()),
                provenance.includeChain().stream()
                        .map(frame -> new ExperimentalCobolMoveCallBoundary.IncludeFrame(
                                frame.includingFile(), frame.requestedName(),
                                frame.includedFile(), frame.includeLine())).toList(),
                provenance.exact());
    }

    private static ExperimentalCobolMoveCallBoundary.Location mapLocation(
            Ast.SourceLocation location) {
        return new ExperimentalCobolMoveCallBoundary.Location(location.file(),
                location.startLine(), location.startColumn(), location.endLine(),
                location.endColumn());
    }

    private static ExperimentalCobolMoveCallBoundary.Provenance provenance(
            String file, int line) {
        ExperimentalCobolMoveCallBoundary.Location location =
                new ExperimentalCobolMoveCallBoundary.Location(file, line, 7, line, 20);
        return new ExperimentalCobolMoveCallBoundary.Provenance(location, location,
                List.of(), true);
    }

    private static FrontendAnalysis analyze(String rawSource, String sourceName) {
        GrammarBinding grammar = Bindings.cobol();
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(rawSource, sourceName,
                SourceNormalizer.SourceFormat.FIXED);
        PreprocessorEngine.Outcome outcome;
        try {
            outcome = new PreprocessorEngine(grammar, new CopybookLibrary(Path.of(
                    "src/test/resources"))).process(normalized.sourceMap(), sourceName);
        } catch (IOException exception) {
            throw new IllegalStateException("test copybook library must be readable", exception);
        }
        Parser parser = grammar.cobolParser(new CommonTokenStream(grammar.cobolLexer(
                CharStreams.fromString(outcome.text(), sourceName))));
        ParseTree tree = grammar.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, outcome.text(),
                outcome.sourceMap(), ids, sizes).buildCompilationUnit(tree, sourceName);
        CompilationUnitModel model = build.compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences =
                new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector().collect(
                    unit.id(), unit.program(), AstScopeIndex.build(unit.program(), table)));
        }
        ResolutionContracts.CobolResolutionPolicy policy =
                ResolutionContracts.CobolResolutionPolicy.initial()
                        .withPgmnameMode(outcome.pgmnameMode())
                        .withDynamMode(outcome.dynamMode()).withDllMode(outcome.dllMode());
        ReferenceResolution resolution = new CobolReferenceResolver(policy)
                .resolve(model, tables, occurrences);
        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(build,
                new ResolutionAnalysisReport.FrontendState(outcome.errors(), 0,
                        parser.getNumberOfSyntaxErrors(), outcome.diagnostics()),
                occurrences, resolution);
        return new FrontendAnalysis(model, tables, resolution, report);
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static <T> T requireInstance(Class<T> type, Object value, String message) {
        require(type.isInstance(value), message);
        return type.cast(value);
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
            assertTrue(raw != null, "classfile not found for " + type.getName());
            return classReferences(new DataInputStream(raw));
        }
    }

    private static Set<String> classReferences(DataInputStream input) throws IOException {
        assertEquals(0xCAFEBABE, input.readInt(), "invalid classfile");
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
                default -> throw new IOException("unsupported constant-pool tag");
            }
        }
        Set<String> result = new LinkedHashSet<>();
        for (int nameIndex : classNameIndexes.values()) {
            String name = utf8.get(nameIndex);
            if (name != null) result.add(name);
        }
        return result;
    }

    private record FrontendAnalysis(CompilationUnitModel model,
                                    CompilationUnitSymbolTables tables,
                                    ReferenceResolution resolution,
                                    ResolutionAnalysisReport report) { }
}
