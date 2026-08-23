package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DataAndIndexReferenceResolverTest {
    private static final Path DATA = Path.of("src/test/resources/cobol/resolution/data-binding.cbl");
    private static final Path NESTED = Path.of("src/test/resources/cobol/resolution/nested-data-visibility.cbl");

    @Test
    void resolvesSimpleDuplicateMissingAndIncompatibleNames() throws Exception {
        Analysis analysis = analyze(DATA, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();

        assertEntry(analysis.resolution(), unit, "WS-UNIQUE", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), unit, "DUPLICATE-ITEM", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
        assertEntry(analysis.resolution(), unit, "MISSING-ITEM", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0);
        assertEntry(analysis.resolution(), unit, "NORMAL-ITEM", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, 0);
    }

    @Test
    void resolvesOrderedQualificationConditionIndexAndRelations() throws Exception {
        Analysis analysis = analyze(DATA, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();

        assertEntry(analysis.resolution(), unit, "DUPLICATE-ITEM OF GROUP-A",
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        assertEntry(analysis.resolution(), unit, "DUPLICATE-ITEM IN GROUP-B",
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        assertEntry(analysis.resolution(), unit,
                "LEAF-ITEM OF MIDDLE-GROUP OF OUTER-GROUP",
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
        assertEntry(analysis.resolution(), unit,
                "LEAF-ITEM OF OUTER-GROUP OF MIDDLE-GROUP",
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0);
        assertEntry(analysis.resolution(), unit, "FLAG-ON", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), unit, "TABLE-IDX", ResolutionContracts.ReferenceRole.OCCURS_INDEX,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);

        DeclarationRelationResolution relations = analysis.resolution().declarationRelations();
        assertTrue(relations.entries().stream().filter(entry -> entry.kind() == SymbolTable.RelationKind.REDEFINES)
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED));
        assertTrue(relations.entries().stream().filter(entry -> entry.kind() == SymbolTable.RelationKind.RENAMES_FROM
                        || entry.kind() == SymbolTable.RelationKind.RENAMES_THROUGH
                        || entry.kind() == SymbolTable.RelationKind.OCCURS_DEPENDING_ON
                        || entry.kind() == SymbolTable.RelationKind.OCCURS_INDEX)
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED));
    }

    @Test
    void appliesQualifyModeWithoutGuessingAnUnspecifiedCompilerOption() throws Exception {
        Analysis standard = analyze(DATA, ResolutionContracts.QualifyMode.STANDARD);
        Analysis extend = analyze(DATA, ResolutionContracts.QualifyMode.EXTEND);
        Analysis unspecified = analyze(DATA, ResolutionContracts.QualifyMode.UNSPECIFIED);
        ResolutionContracts.ProgramUnitId unit = standard.model().programUnits().get(0).id();
        String text = "LEAF-ITEM OF MIDDLE-GROUP OF OUTER-GROUP";

        assertEntry(standard.resolution(), unit, text, ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
        assertEntry(extend.resolution(), unit, text, ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        assertEntry(unspecified.resolution(), unit, text, ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, 2);
    }

    @Test
    void respectsNestedShadowingAndOnlyImportsTypedGlobalData() throws Exception {
        Analysis analysis = analyze(NESTED, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId child = analysis.model().programUnits().stream()
                .filter(unit -> unit.program().name().equals("DATA-CHILD")).findFirst().orElseThrow().id();

        ReferenceResolution.Entry shadow = assertEntry(analysis.resolution(), child, "OUTER-LOCAL",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals(child, shadow.candidates().get(0).entityId().programUnitId());
        ReferenceResolution.Entry global = assertEntry(analysis.resolution(), child, "GLOBAL-DATA",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertNotEquals(child, global.candidates().get(0).entityId().programUnitId());
        ReferenceResolution.Entry external = assertEntry(analysis.resolution(), child, "EXTERNAL-DATA",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals("EXTERNAL", external.candidates().get(0).attributes().get("visibility"));
        assertEntry(analysis.resolution(), child, "NOT-VISIBLE", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0);
    }

    @Test
    void resolvesADataItemQualifiedThroughItsFileHierarchy() throws Exception {
        Analysis analysis = analyze(Path.of("src/test/resources/cobol/resolution/entities-and-occurrences.cbl"),
                ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();
        ReferenceResolution.Entry entry = assertEntry(analysis.resolution(), unit,
                "FILE-VALUE OF BOTH-RECORD IN BOTH-FILE", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        assertEquals("FILE-VALUE", entry.candidates().get(0).canonicalName());
    }

    @Test
    void keepsNamespacesOutsideThisPhaseExplicitlyUnsupported() throws Exception {
        Analysis analysis = analyze(Path.of("src/test/resources/cobol/resolution/baseline-compilation-unit.cbl"),
                ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();
        assertEntry(analysis.resolution(), unit, "'EXTERNAL-PROGRAM'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                ResolutionContracts.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM, 0);
    }

    @Test
    void usesPrebuiltNameIndexesInsteadOfScanningAllSymbolsPerReference() throws Exception {
        Analysis analysis = analyze(DATA, ResolutionContracts.QualifyMode.STANDARD);
        assertTrue(analysis.resolution().metrics().nominalLookups() > 0);
        assertTrue(analysis.resolution().metrics().candidateInspections()
                < analysis.resolution().metrics().nominalLookups() * 5,
                "inspection must be proportional to same-name candidates, not all symbols");
        assertTrue(analysis.resolution().metrics().indexedDeclarations()
                >= analysis.tables().units().get(0).symbolTable().symbols().size());
    }

    private static ReferenceResolution.Entry assertEntry(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId unit,
            String writtenText, ResolutionContracts.ReferenceRole role,
            ResolutionContracts.ResolutionStatus status, ResolutionContracts.ResolutionReason reason,
            int candidates) {
        ReferenceResolution.Entry entry = resolution.find(unit, writtenText, role).stream()
                .findFirst().orElseThrow(() -> new AssertionError("missing resolution entry " + role + " " + writtenText));
        assertEquals(status, entry.status(), entry.toString());
        assertEquals(reason, entry.reason(), entry.toString());
        assertEquals(candidates, entry.candidates().size(), entry.toString());
        return entry;
    }

    private static Analysis analyze(Path sourcePath, ResolutionContracts.QualifyMode mode) throws Exception {
        Path file = sourcePath.toAbsolutePath();
        String source = SourceNormalizer.fixed(Files.readString(file, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.proleap();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, file.getFileName().toString()))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, source,
                SourceMap.identity(source, file.getFileName().toString()), ids, sizes)
                .buildCompilationUnit(tree, file.getFileName().toString());
        CompilationUnitModel model = build.compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector().collect(unit.id(), unit.program(),
                    AstScopeIndex.build(unit.program(), table)));
        }
        ResolutionContracts.CobolResolutionPolicy policy = new ResolutionContracts.CobolResolutionPolicy(
                "test-policy", "1", mode);
        ReferenceResolution resolution = new DataAndIndexReferenceResolver(policy)
                .resolve(model, tables, occurrences);
        return new Analysis(model, tables, resolution);
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private record Analysis(CompilationUnitModel model, CompilationUnitSymbolTables tables,
                            ReferenceResolution resolution) { }
}
