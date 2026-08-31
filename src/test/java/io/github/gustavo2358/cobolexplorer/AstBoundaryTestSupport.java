package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Independent test-only construction and reconciliation helpers for WORK-AST-002. */
final class AstBoundaryTestSupport {
    static final Path FIXTURE = Path.of("src/test/resources/cobol/semantic/ast-cfg-boundary.cbl");

    private AstBoundaryTestSupport() { }

    static Analysis analyzeFixture() throws IOException {
        return analyze(Files.readString(FIXTURE, StandardCharsets.UTF_8),
                FIXTURE.getFileName().toString());
    }

    static Analysis analyze(String rawSource, String sourceName) {
        GrammarBinding binding = Bindings.cobol();
        SourceNormalizer.Result normalized = SourceNormalizer.normalize(
                ensureFixedColumns(rawSource), sourceName, SourceNormalizer.SourceFormat.FIXED);
        PreprocessorEngine.Outcome preprocessing;
        try {
            preprocessing = new PreprocessorEngine(binding, new CopybookLibrary(
                    Path.of("src/test/resources/cobol/provenance/cpy")))
                    .process(normalized.sourceMap(), sourceName);
        } catch (IOException exception) {
            throw new IllegalStateException("test copybook library must be readable", exception);
        }
        assertEquals(0, preprocessing.errors(), "fixture must preprocess without errors");
        String source = preprocessing.text();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, sourceName))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors(), "fixture must be valid for the configured grammar");

        IdentityHashMap<ParseTree, Integer> parseIds = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> parseSizes = new IdentityHashMap<>();
        index(tree, parseIds, parseSizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, source,
                preprocessing.sourceMap(), parseIds, parseSizes)
                .buildCompilationUnit(tree, sourceName);
        CompilationUnitModel model = build.compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        Map<ResolutionContracts.ProgramUnitId, AstScopeIndex> scopes = new LinkedHashMap<>();
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            AstScopeIndex scopeIndex = AstScopeIndex.build(unit.program(), table);
            scopes.put(unit.id(), scopeIndex);
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector()
                    .collect(unit.id(), unit.program(), scopeIndex));
        }
        ReferenceResolution resolution = new CobolReferenceResolver(
                ResolutionContracts.CobolResolutionPolicy.initial())
                .resolve(model, tables, occurrences);
        ResolutionAnalysisReport report = ResolutionAnalysisReport.compose(build,
                ResolutionAnalysisReport.FrontendState.complete(), occurrences, resolution);
        return new Analysis(tree, build, model, tables, scopes, occurrences, resolution, report);
    }

    static List<Ast.Node> nodes(Ast.Node root) {
        List<Ast.Node> result = new ArrayList<>();
        result.add(root);
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child));
        return result;
    }

    static List<Ast.Node> nodes(Analysis analysis) {
        return analysis.model().programUnits().stream()
                .flatMap(unit -> nodes(unit.program()).stream()).toList();
    }

    static <T> List<T> nodes(Analysis analysis, Class<T> type) {
        return nodes(analysis).stream().filter(type::isInstance).map(type::cast).toList();
    }

    static <T extends ParseTree> List<T> contexts(ParseTree root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collectContexts(root, type, result);
        return result;
    }

    static List<ParserRuleContext> directDataClauseContexts(ParseTree root) {
        return contexts(root, ParserRuleContext.class).stream()
                .filter(AstBoundaryTestSupport::isDataClauseContext).toList();
    }

    static void assertActualProductsJoin(Analysis analysis) {
        Map<ResolutionContracts.ProgramUnitId, Map<Integer, Ast.Node>> astByUnit = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : analysis.model().programUnits()) {
            Map<Integer, Ast.Node> nodes = new LinkedHashMap<>();
            for (Ast.Node node : nodes(unit.program()))
                assertTrue(nodes.put(node.meta().id(), node) == null,
                        () -> "duplicate AST node ID in " + unit.id() + ": " + node.meta().id());
            astByUnit.put(unit.id(), nodes);

            AstScopeIndex scopeIndex = analysis.scopes().get(unit.id());
            assertEquals(nodes.size(), scopeIndex.mappedNodeCount(),
                    "AstScopeIndex must cover every reachable node exactly once");
            SymbolTable table = analysis.tables().forProgramUnit(unit.id()).orElseThrow().symbolTable();
            for (Ast.Node node : nodes.values()) {
                int scopeId = scopeIndex.scopeId(node);
                assertTrue(scopeId >= 0 && scopeId < table.scopes().size());
            }
            for (SymbolTable.Symbol symbol : table.symbols()) {
                assertTrue(nodes.containsKey(symbol.declarationAstNodeId()),
                        () -> "orphan declaration node in " + unit.id() + ": " + symbol.id());
                assertTrue(symbol.scopeId() >= 0 && symbol.scopeId() < table.scopes().size());
            }
            for (SymbolTable.DeclarationRelation relation : table.declarationRelations()) {
                assertTrue(relation.ownerSymbolId() >= 0
                        && relation.ownerSymbolId() < table.symbols().size());
                assertTrue(nodes.containsKey(relation.referenceAstNodeId()),
                        () -> "orphan relation reference in " + unit.id() + ": " + relation.id());
            }
            SemanticCoverage.Report coverage = analysis.build().coverageByProgramUnit().get(unit.id());
            for (SemanticCoverage.Finding finding : coverage.findings()) {
                if (finding.astNodeId() < 0) continue;
                Ast.Node node = nodes.get(finding.astNodeId());
                assertNotNull(node, "finding must reference a reachable AST node");
                assertEquals(node.meta(), finding.meta(), "finding provenance/meta must match its node");
            }
        }

        Map<OccurrenceKey, ReferenceOccurrences.Occurrence> collected = new LinkedHashMap<>();
        for (Map.Entry<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> product
                : analysis.occurrences().entrySet()) {
            Map<Integer, Ast.Node> nodes = astByUnit.get(product.getKey());
            AstScopeIndex scopes = analysis.scopes().get(product.getKey());
            for (ReferenceOccurrences.Occurrence occurrence : product.getValue().occurrences()) {
                assertEquals(product.getKey(), occurrence.programUnitId());
                Ast.Node node = nodes.get(occurrence.referenceAstNodeId());
                assertNotNull(node, "occurrence must reference an AST node in the same unit");
                assertEquals(scopes.scopeId(node), occurrence.scopeId());
                assertEquals(node.meta(), occurrence.meta(),
                        "occurrence must preserve exact AST metadata/provenance");
                assertTrue(collected.put(new OccurrenceKey(occurrence.programUnitId(), occurrence.id()),
                        occurrence) == null, "composite occurrence identity must be unique");
            }
        }

        Set<OccurrenceKey> resolved = new HashSet<>();
        for (ReferenceResolution.Entry entry : analysis.resolution().entries()) {
            ReferenceOccurrences.Occurrence occurrence = entry.occurrence();
            OccurrenceKey key = new OccurrenceKey(occurrence.programUnitId(), occurrence.id());
            assertEquals(collected.get(key), occurrence,
                    "resolution entry must carry the collected occurrence with the composite identity");
            assertTrue(resolved.add(key), "each occurrence must have exactly one resolution entry");
            for (ReferenceResolution.Candidate candidate : entry.candidates())
                assertCandidateExists(analysis, candidate);
        }
        assertEquals(collected.keySet(), resolved,
                "collected occurrences and resolution entries must be a bijection");

        Map<RelationKey, SymbolTable.DeclarationRelation> relations = new LinkedHashMap<>();
        for (CompilationUnitSymbolTables.UnitSymbols unit : analysis.tables().units()) {
            for (SymbolTable.DeclarationRelation relation : unit.symbolTable().declarationRelations())
                relations.put(new RelationKey(unit.id(), relation.id()), relation);
        }
        Set<RelationKey> relationEntries = new HashSet<>();
        for (DeclarationRelationResolution.Entry entry
                : analysis.resolution().declarationRelations().entries()) {
            RelationKey key = new RelationKey(entry.programUnitId(), entry.relationId());
            SymbolTable.DeclarationRelation relation = relations.get(key);
            assertNotNull(relation, "relation resolution must reference a declaration relation");
            assertEquals(relation.kind(), entry.kind());
            assertEquals(relation.referenceAstNodeId(), entry.referenceAstNodeId());
            assertTrue(relationEntries.add(key), "relation resolution identity must be unique");
            for (ReferenceResolution.Candidate candidate : entry.candidates())
                assertCandidateExists(analysis, candidate);
        }
        assertEquals(relations.keySet(), relationEntries,
                "declaration relations and their resolution entries must be a bijection");
    }

    /**
     * Exercises the current post-AST product composition without assigning validation ownership
     * to a constructor, resolver, report or future dedicated validator.
     */
    static ResolutionAnalysisReport composePostAstProducts(
            Analysis analysis, CompilationUnitSymbolTables tables) {
        ReferenceResolution resolution = new CobolReferenceResolver(
                ResolutionContracts.CobolResolutionPolicy.initial())
                .resolve(analysis.model(), tables, analysis.occurrences());
        return ResolutionAnalysisReport.compose(analysis.build(),
                ResolutionAnalysisReport.FrontendState.complete(), analysis.occurrences(), resolution);
    }

    private static void assertCandidateExists(Analysis analysis, ReferenceResolution.Candidate candidate) {
        ResolutionContracts.SemanticEntityId id = candidate.entityId();
        CompilationUnitModel.ProgramUnit unit = analysis.model().find(id.programUnitId()).orElseThrow();
        if (id.domain() == ResolutionContracts.SemanticEntityDomain.PROGRAM_UNIT) {
            int expected = analysis.model().programUnits().indexOf(unit);
            assertEquals(expected, id.localId(), "PROGRAM candidate local ID must be namespaced by its unit");
            return;
        }
        SymbolTable table = analysis.tables().forProgramUnit(id.programUnitId()).orElseThrow().symbolTable();
        if (id.domain() == ResolutionContracts.SemanticEntityDomain.FILE_ENTITY) {
            assertTrue(id.localId() >= 0 && id.localId() < table.entities().size(),
                    "candidate must reference an existing FILE entity");
        } else {
            assertTrue(id.localId() >= 0 && id.localId() < table.symbols().size(),
                    "candidate must reference an existing symbol");
        }
        for (int symbolId : candidate.declarationSymbolIds())
            assertTrue(symbolId >= 0 && symbolId < table.symbols().size(),
                    "candidate declaration symbol must exist in the candidate unit");
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++)
            size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private static String ensureFixedColumns(String source) {
        String first = source.lines().filter(line -> !line.isBlank()).findFirst().orElse("");
        if (first.startsWith("       ")) return source;
        return source.lines().map(line -> line.isBlank() ? line : "       " + line)
                .collect(java.util.stream.Collectors.joining("\n")) + "\n";
    }

    private static <T extends ParseTree> void collectContexts(ParseTree tree, Class<T> type,
                                                               List<T> output) {
        if (type.isInstance(tree)) output.add(type.cast(tree));
        for (int i = 0; i < tree.getChildCount(); i++)
            collectContexts(tree.getChild(i), type, output);
    }

    private static boolean isDataClauseContext(ParserRuleContext context) {
        return context instanceof CobolParser.DataAlignedClauseContext
                || context instanceof CobolParser.DataBlankWhenZeroClauseContext
                || context instanceof CobolParser.DataCommonOwnLocalClauseContext
                || context instanceof CobolParser.DataExternalClauseContext
                || context instanceof CobolParser.DataGlobalClauseContext
                || context instanceof CobolParser.DataIntegerStringClauseContext
                || context instanceof CobolParser.DataJustifiedClauseContext
                || context instanceof CobolParser.DataOccursClauseContext
                || context instanceof CobolParser.DataPictureClauseContext
                || context instanceof CobolParser.DataReceivedByClauseContext
                || context instanceof CobolParser.DataRecordAreaClauseContext
                || context instanceof CobolParser.DataRedefinesClauseContext
                || context instanceof CobolParser.DataRenamesClauseContext
                || context instanceof CobolParser.DataSignClauseContext
                || context instanceof CobolParser.DataSynchronizedClauseContext
                || context instanceof CobolParser.DataThreadLocalClauseContext
                || context instanceof CobolParser.DataTypeClauseContext
                || context instanceof CobolParser.DataTypeDefClauseContext
                || context instanceof CobolParser.DataUsageClauseContext
                || context instanceof CobolParser.DataUsingClauseContext
                || context instanceof CobolParser.DataValueClauseContext
                || context instanceof CobolParser.DataWithLowerBoundsClauseContext;
    }

    record Analysis(ParseTree tree, CompilationUnitBuildResult build, CompilationUnitModel model,
                    CompilationUnitSymbolTables tables,
                    Map<ResolutionContracts.ProgramUnitId, AstScopeIndex> scopes,
                    Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences,
                    ReferenceResolution resolution, ResolutionAnalysisReport report) { }

    private record OccurrenceKey(ResolutionContracts.ProgramUnitId unitId, int occurrenceId) { }
    private record RelationKey(ResolutionContracts.ProgramUnitId unitId, int relationId) { }
}
