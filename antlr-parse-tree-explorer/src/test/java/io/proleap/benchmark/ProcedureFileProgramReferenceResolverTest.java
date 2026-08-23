package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProcedureFileProgramReferenceResolverTest {
    private static final Path PROCEDURE = Path.of("src/test/resources/cobol/resolution/procedure-binding.cbl");
    private static final Path FILE = Path.of("src/test/resources/cobol/resolution/file-binding.cbl");
    private static final Path PROGRAM = Path.of("src/test/resources/cobol/resolution/program-binding.cbl");

    @Test
    void resolvesProcedureTargetsQualificationDependingOnPerformAndPreservedAlter() throws Exception {
        Analysis analysis = analyze(PROCEDURE, Optional.empty());
        ResolutionContracts.ProgramUnitId outer = analysis.model().programUnits().get(0).id();

        assertEntry(analysis.resolution(), outer, "UNIQUE-PARA", ResolutionContracts.ReferenceRole.GO_TO_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), outer, "DUPLICATE-PARA", ResolutionContracts.ReferenceRole.GO_TO_TARGET,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
        assertEntry(analysis.resolution(), outer, "DUPLICATE-PARA OF SECTION-A",
                ResolutionContracts.ReferenceRole.GO_TO_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        assertEntry(analysis.resolution(), outer, "FIRST-PARA", ResolutionContracts.ReferenceRole.GO_TO_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), outer, "SECOND-PARA", ResolutionContracts.ReferenceRole.GO_TO_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), outer, "FIRST-PARA", ResolutionContracts.ReferenceRole.PERFORM_FROM,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), outer, "SECOND-PARA", ResolutionContracts.ReferenceRole.PERFORM_THROUGH,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), outer, "CHILD-ONLY", ResolutionContracts.ReferenceRole.GO_TO_TARGET,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0);

        assertTrue(analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().kind() == ResolutionContracts.ReferenceKind.PROCEDURE)
                .filter(entry -> entry.occurrence().preservation() == ReferenceOccurrences.Preservation.PRESERVED_CONTAINER)
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED),
                "ALTER procedure operands retained by a preserved container must still bind");
    }

    @Test
    void resolvesProcedureReferencesInsidePreservedSortAndMergeContainers() throws Exception {
        Analysis analysis = analyze(Path.of("src/test/resources/cobol/semantic/nominal-references.cbl"),
                Optional.empty());
        assertTrue(analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().kind() == ResolutionContracts.ReferenceKind.PROCEDURE)
                .filter(entry -> entry.occurrence().preservation() == ReferenceOccurrences.Preservation.PRESERVED_CONTAINER)
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED));
        assertTrue(analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().kind() == ResolutionContracts.ReferenceKind.FILE)
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.UNRESOLVED
                        && entry.reason() == ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND),
                "SORT/MERGE files without SELECT or FD remain explicit unknowns");
    }

    @Test
    void resolvesFileEntitiesWithoutArtificialSelectFdAmbiguity() throws Exception {
        Analysis analysis = analyze(FILE, Optional.empty());
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();

        ReferenceResolution.Entry both = assertEntry(analysis.resolution(), unit, "BOTH-FILE",
                ResolutionContracts.ReferenceRole.FILE_OPERATION,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals(2, both.candidates().get(0).declarationSymbolIds().size());
        assertEquals("ASSIGN TO 'BOTHDD'", both.candidates().get(0).attributes().get("assignments"));
        assertEntry(analysis.resolution(), unit, "SELECT-ONLY", ResolutionContracts.ReferenceRole.FILE_OPERATION,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), unit, "FD-ONLY", ResolutionContracts.ReferenceRole.FILE_OPERATION,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), unit, "MISSING-FILE", ResolutionContracts.ReferenceRole.FILE_OPERATION,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0);
        ReferenceResolution.Entry fileArgument = assertEntry(analysis.resolution(), unit, "BOTH-FILE",
                ResolutionContracts.ReferenceRole.CALL_ARGUMENT,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA, ResolutionContracts.ReferenceKind.FILE),
                fileArgument.occurrence().admissibleKinds());
        assertEquals(ResolutionContracts.ReferenceKind.FILE, fileArgument.candidates().get(0).kind());
    }

    @Test
    void resolvesNestedAndCommonProgramsButPreservesVisibilityAndAmbiguity() throws Exception {
        Analysis analysis = analyze(PROGRAM, Optional.empty());
        ResolutionContracts.ProgramUnitId outer = unit(analysis, "PROGRAM-OUTER");
        ResolutionContracts.ProgramUnitId privateChild = unit(analysis, "PRIVATE-CHILD");

        assertEntry(analysis.resolution(), outer, "'PRIVATE-CHILD'", ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), privateChild, "'COMMON-CHILD'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), privateChild, "'PRIVATE-SIBLING'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.EXTERNAL_CATALOG_NOT_PROVIDED, 0);
        assertEntry(analysis.resolution(), outer, "'DUPLICATE-CHILD'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
    }

    @Test
    void usesOptionalExternalCatalogAndPreservesAllReturnedCandidates() throws Exception {
        ExternalProgramCatalog catalog = canonicalName -> switch (canonicalName) {
            case "EXTERNAL-ONE" -> List.of(new ExternalProgramCatalog.Program(10, "fake", "EXTERNAL-ONE", Map.of()));
            case "EXTERNAL-MANY" -> List.of(
                    new ExternalProgramCatalog.Program(20, "fake", "EXTERNAL-MANY", Map.of("library", "A")),
                    new ExternalProgramCatalog.Program(21, "fake", "EXTERNAL-MANY", Map.of("library", "B")));
            default -> List.of();
        };
        Analysis absent = analyze(PROGRAM, Optional.empty());
        Analysis empty = analyze(PROGRAM, Optional.of(ExternalProgramCatalog.empty()));
        Analysis present = analyze(PROGRAM, Optional.of(catalog));
        ResolutionContracts.ProgramUnitId outer = unit(present, "PROGRAM-OUTER");

        assertEntry(absent.resolution(), unit(absent, "PROGRAM-OUTER"), "'EXTERNAL-ONE'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.EXTERNAL_CATALOG_NOT_PROVIDED, 0);
        assertEntry(empty.resolution(), unit(empty, "PROGRAM-OUTER"), "'EXTERNAL-ONE'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0);
        ReferenceResolution.Entry one = assertEntry(present.resolution(), outer, "'EXTERNAL-ONE'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals(ResolutionContracts.SemanticEntityDomain.EXTERNAL_PROGRAM,
                one.candidates().get(0).entityId().domain());
        assertEntry(present.resolution(), outer, "'EXTERNAL-MANY'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
    }

    @Test
    void dynamicCallBindsOnlyItsDataVariable() throws Exception {
        Analysis analysis = analyze(PROGRAM, Optional.empty());
        ResolutionContracts.ProgramUnitId outer = unit(analysis, "PROGRAM-OUTER");
        ReferenceResolution.Entry dynamic = assertEntry(analysis.resolution(), outer, "WS-CALL-TARGET",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals(ResolutionContracts.ReferenceKind.DATA, dynamic.occurrence().kind());
        assertEquals(ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                dynamic.candidates().get(0).entityId().domain());
        assertTrue(analysis.resolution().find(outer, "'WS-CALL-TARGET'",
                ResolutionContracts.ReferenceRole.CALL_TARGET).isEmpty());
    }

    @Test
    void keepsCbstm03RegressionFactsWithoutPerformingValueResolution() throws Exception {
        Analysis dynamic = analyzeCorpus("CBSTM03D.CBL", Optional.empty());
        List<ReferenceResolution.Entry> dynamicCalls = dynamic.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role() == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .toList();
        assertEquals(14, dynamicCalls.size());
        assertTrue(dynamicCalls.stream().allMatch(entry -> entry.occurrence().kind()
                == ResolutionContracts.ReferenceKind.DATA));
        assertTrue(dynamicCalls.stream().allMatch(entry -> entry.status()
                == ResolutionContracts.ResolutionStatus.RESOLVED));
        assertEquals(1, dynamicCalls.stream().map(entry -> entry.candidates().get(0).entityId()).distinct().count());
        assertTrue(dynamicCalls.stream().allMatch(entry -> entry.occurrence().writtenText()
                .equals("WS-CALL-TARGET")));

        Analysis literal = analyzeCorpus("CBSTM03A.CBL", Optional.empty());
        List<ReferenceResolution.Entry> literalCalls = literal.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role() == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .toList();
        assertEquals(14, literalCalls.size());
        assertTrue(literalCalls.stream().allMatch(entry -> entry.occurrence().kind()
                == ResolutionContracts.ReferenceKind.PROGRAM));
        assertTrue(literalCalls.stream().allMatch(entry -> entry.status()
                == ResolutionContracts.ResolutionStatus.UNRESOLVED
                && entry.reason() == ResolutionContracts.ResolutionReason.EXTERNAL_CATALOG_NOT_PROVIDED));
    }

    private static ResolutionContracts.ProgramUnitId unit(Analysis analysis, String name) {
        return analysis.model().programUnits().stream().filter(unit -> unit.program().name().equals(name))
                .findFirst().orElseThrow().id();
    }

    private static ReferenceResolution.Entry assertEntry(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId unit,
            String writtenText, ResolutionContracts.ReferenceRole role,
            ResolutionContracts.ResolutionStatus status, ResolutionContracts.ResolutionReason reason,
            int candidates) {
        ReferenceResolution.Entry entry = resolution.find(unit, writtenText, role).stream()
                .findFirst().orElseThrow(() -> new AssertionError("missing entry " + role + " " + writtenText));
        assertEquals(status, entry.status(), entry.toString());
        assertEquals(reason, entry.reason(), entry.toString());
        assertEquals(candidates, entry.candidates().size(), entry.toString());
        return entry;
    }

    private static Analysis analyze(Path sourcePath, Optional<ExternalProgramCatalog> catalog) throws Exception {
        Path file = sourcePath.toAbsolutePath();
        String source = SourceNormalizer.fixed(Files.readString(file, StandardCharsets.UTF_8));
        return analyzeSource(file, source, catalog);
    }

    private static Analysis analyzeCorpus(String fileName, Optional<ExternalProgramCatalog> catalog) throws Exception {
        Path project = Path.of("").toAbsolutePath().normalize();
        Path internal = project.resolve("corpus/cbl").resolve(fileName);
        Path file = Files.exists(internal) ? internal : project.getParent().resolve("cbl").resolve(fileName);
        Path copybooks = file.startsWith(project.resolve("corpus"))
                ? project.resolve("corpus/cpy") : project.getParent().resolve("cpy");
        GrammarBinding binding = Bindings.proleap();
        String fixed = SourceNormalizer.fixed(Files.readString(file, StandardCharsets.UTF_8));
        String source = new PreprocessorEngine(binding, new CopybookLibrary(copybooks))
                .process(fixed, file.getFileName().toString()).text();
        return analyzeSource(file, source, catalog);
    }

    private static Analysis analyzeSource(Path file, String source,
                                          Optional<ExternalProgramCatalog> catalog) throws Exception {
        GrammarBinding binding = Bindings.proleap();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, file.getFileName().toString()))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitModel model = new AstBuilder(parser, source,
                SourceMap.identity(source, file.getFileName().toString()), ids, sizes)
                .buildCompilationUnit(tree, file.getFileName().toString()).compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector().collect(unit.id(), unit.program(),
                    AstScopeIndex.build(unit.program(), table)));
        }
        ResolutionContracts.CobolResolutionPolicy policy = new ResolutionContracts.CobolResolutionPolicy(
                "test-policy", "1", ResolutionContracts.QualifyMode.STANDARD);
        ReferenceResolution resolution = new CobolReferenceResolver(policy, catalog)
                .resolve(model, tables, occurrences);
        return new Analysis(model, resolution);
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private record Analysis(CompilationUnitModel model, ReferenceResolution resolution) { }
}
