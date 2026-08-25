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
    private static final Path NESTED_GLOBAL_FILE = Path.of(
            "src/test/resources/cobol/resolution/nested-global-file.cbl");
    private static final Path GLOBAL_FILE_THROUGH_LOCAL = Path.of(
            "src/test/resources/cobol/resolution/nested-global-through-local-file.cbl");
    private static final Path DATA_FILE_QUALIFIER_COLLISION = Path.of(
            "src/test/resources/cobol/resolution/data-file-qualifier-collision.cbl");
    private static final Path FILE_NAMESPACE_SHADOWING = Path.of(
            "src/test/resources/cobol/resolution/file-namespace-shadowing.cbl");
    private static final Path COMMON_PROGRAM_VISIBILITY = Path.of(
            "src/test/resources/cobol/resolution/common-program-visibility.cbl");
    private static final Path LITERAL_PROGRAM_NAME = Path.of(
            "src/test/resources/cobol/resolution/literal-program-name.cbl");
    private static final Path PROGRAM_NAME_POLICY = Path.of(
            "src/test/resources/cobol/resolution/program-name-policy.cbl");
    private static final Path EXTERNAL_PROGRAM_NAMES = Path.of(
            "src/test/resources/cobol/resolution/external-program-name-canonicalization.cbl");
    private static final Path LONGMIXED_NESTED_PROGRAM = Path.of(
            "src/test/resources/cobol/resolution/longmixed-nested-program.cbl");
    private static final Path UNSPECIFIED_EXTERNAL_PROGRAM = Path.of(
            "src/test/resources/cobol/resolution/unspecified-external-program-name.cbl");

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
    void resolvesOnlyGlobalAncestorFilesAndPreservesFileEntityOwnershipAndDeclarations() throws Exception {
        Analysis analysis = analyze(NESTED_GLOBAL_FILE, Optional.empty());
        ResolutionContracts.ProgramUnitId outer = unit(analysis, "FILE-OUTER");
        ResolutionContracts.ProgramUnitId inner = unit(analysis, "FILE-INNER");
        SymbolTable outerTable = analysis.tables().forProgramUnit(outer).orElseThrow().symbolTable();
        SymbolTable innerTable = analysis.tables().forProgramUnit(inner).orElseThrow().symbolTable();

        assertAll("GLOBAL FILE semantic product",
                () -> assertAll("FILE entities preserve association and effective visibility before binding",
                        () -> assertFileEntity(outerTable, "GLOBAL-BOTH", 2, "GLOBAL"),
                        () -> assertFileEntity(outerTable, "OUTER-LOCAL", 2, "LOCAL"),
                        () -> assertFileEntity(outerTable, "GLOBAL-FD-ONLY", 1, "GLOBAL"),
                        () -> assertFileEntity(outerTable, "SHADOW-FILE", 2, "GLOBAL"),
                        () -> assertFileEntity(innerTable, "SHADOW-FILE", 2, "LOCAL")),
                () -> assertAll("nested FILE lookup applies GLOBAL visibility and local shadowing",
                        () -> assertFileCandidate(analysis.resolution(), inner,
                                "GLOBAL-BOTH", outer, 2, "GLOBAL"),
                        () -> assertEntry(analysis.resolution(), inner, "OUTER-LOCAL",
                                ResolutionContracts.ReferenceRole.FILE_OPERATION,
                                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0),
                        () -> assertFileCandidate(analysis.resolution(), inner,
                                "GLOBAL-FD-ONLY", outer, 1, "GLOBAL"),
                        () -> assertFileCandidate(analysis.resolution(), inner,
                                "SHADOW-FILE", inner, 2, "LOCAL")));
    }

    @Test
    void skipsInvisibleLocalFilesInIntermediateProgramsWhenLookingForGlobalFiles() throws Exception {
        Analysis analysis = analyze(GLOBAL_FILE_THROUGH_LOCAL, Optional.empty());
        ResolutionContracts.ProgramUnitId outerA = unit(analysis, "FILE-OUTER-A");
        ResolutionContracts.ProgramUnitId middleA = unit(analysis, "FILE-MIDDLE-A");
        ResolutionContracts.ProgramUnitId innerA = unit(analysis, "FILE-INNER-A");
        ResolutionContracts.ProgramUnitId middleB = unit(analysis, "FILE-MIDDLE-B");
        ResolutionContracts.ProgramUnitId innerB = unit(analysis, "FILE-INNER-B");
        ResolutionContracts.ProgramUnitId innerC = unit(analysis, "FILE-INNER-C");
        ResolutionContracts.ProgramUnitId innerD = unit(analysis, "FILE-INNER-D");

        assertAll("adversarial FILE visibility topology",
                () -> assertFileEntity(analysis.tables().forProgramUnit(outerA).orElseThrow().symbolTable(),
                        "FILE-X", 1, "GLOBAL"),
                () -> assertFileEntity(analysis.tables().forProgramUnit(middleA).orElseThrow().symbolTable(),
                        "FILE-X", 1, "LOCAL"),
                () -> assertTrue(analysis.tables().forProgramUnit(innerA).orElseThrow().symbolTable()
                        .entities().stream().noneMatch(entity -> entity.canonicalName().equals("FILE-X"))));

        assertAll("nested FILE lookup ignores only invisible ancestor declarations",
                () -> assertFileCandidate(analysis.resolution(), innerA, "FILE-X", outerA, 1, "GLOBAL"),
                () -> assertFileCandidate(analysis.resolution(), innerB, "FILE-X", middleB, 1, "GLOBAL"),
                () -> assertFileCandidate(analysis.resolution(), innerC, "FILE-X", innerC, 1, "LOCAL"),
                () -> assertEntry(analysis.resolution(), innerD, "FILE-X",
                        ResolutionContracts.ReferenceRole.FILE_OPERATION,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0));
    }

    @Test
    void stopsFileLookupAtIncompatibleLocalNominalDeclarations() throws Exception {
        Analysis analysis = analyze(FILE_NAMESPACE_SHADOWING, Optional.empty());
        ResolutionContracts.ProgramUnitId outer = unit(analysis, "FILE-SHADOW-OUTER");
        ResolutionContracts.ProgramUnitId inner = unit(analysis, "FILE-SHADOW-INNER");
        SymbolTable outerTable = analysis.tables().forProgramUnit(outer).orElseThrow().symbolTable();
        SymbolTable innerTable = analysis.tables().forProgramUnit(inner).orElseThrow().symbolTable();

        assertAll("fixture contains incompatible local names and homonymous outer GLOBAL FILE entities",
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM,
                        innerTable.lookupAll(SymbolTable.Namespace.DATA, "DATA-X").get(0).kind()),
                () -> assertEquals(SymbolTable.SymbolKind.CONDITION_NAME,
                        innerTable.lookupAll(SymbolTable.Namespace.DATA, "CONDITION-X").get(0).kind()),
                () -> assertEquals(SymbolTable.SymbolKind.INDEX_NAME,
                        innerTable.lookupAll(SymbolTable.Namespace.DATA, "INDEX-X").get(0).kind()),
                () -> assertFileEntity(outerTable, "DATA-X", 1, "GLOBAL"),
                () -> assertFileEntity(outerTable, "CONDITION-X", 1, "GLOBAL"),
                () -> assertFileEntity(outerTable, "INDEX-X", 1, "GLOBAL"));

        assertAll("FILE context respects the first nominal declaration before namespace filtering",
                () -> assertEntry(analysis.resolution(), inner, "DATA-X",
                        ResolutionContracts.ReferenceRole.FILE_OPERATION,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, 0),
                () -> assertEntry(analysis.resolution(), inner, "CONDITION-X",
                        ResolutionContracts.ReferenceRole.FILE_OPERATION,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, 0),
                () -> assertEntry(analysis.resolution(), inner, "INDEX-X",
                        ResolutionContracts.ReferenceRole.FILE_OPERATION,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, 0),
                () -> assertFileCandidate(analysis.resolution(), inner, "FILE-X", inner, 1, "LOCAL"),
                () -> assertFileCandidate(analysis.resolution(), inner, "CONTROL-FILE", outer, 1, "GLOBAL"));
    }

    @Test
    void preservesQualifierKindWhenDataAndFileHierarchiesHaveTheSameNames() throws Exception {
        Analysis analysis = analyze(DATA_FILE_QUALIFIER_COLLISION, Optional.empty());
        ResolutionContracts.ProgramUnitId unit = unit(analysis, "QUALIFIER-COLLISION");
        SymbolTable table = analysis.tables().forProgramUnit(unit).orElseThrow().symbolTable();
        List<SymbolTable.Symbol> items = table.lookupAll(SymbolTable.Namespace.DATA, "ITEM");
        assertEquals(2, items.size());
        SymbolTable.Symbol fileItem = items.stream().filter(symbol -> hasFileAncestor(table, symbol))
                .findFirst().orElseThrow();
        SymbolTable.Symbol dataItem = items.stream().filter(symbol -> !hasFileAncestor(table, symbol))
                .findFirst().orElseThrow();

        List<ReferenceResolution.Entry> homonymousQualifiers = analysis.resolution().find(
                unit, "QUALIFIER-X", ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT);
        List<ReferenceResolution.Entry> generalQualifiers = homonymousQualifiers.stream()
                .filter(entry -> entry.occurrence().grammarRule().equals("dataName")).toList();
        ReferenceResolution.Entry explicitFileQualifier = homonymousQualifiers.stream()
                .filter(entry -> entry.occurrence().grammarRule().equals("fileName"))
                .findFirst().orElseThrow();
        assertAll("typed DATA/FILE qualifier collision",
                () -> assertAll("grammar-derived qualifier constraints retain semantic admissibility",
                        () -> assertEquals(3, homonymousQualifiers.size()),
                        () -> assertEquals(2, generalQualifiers.size()),
                        () -> generalQualifiers.forEach(entry -> assertEquals(
                                EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                        ResolutionContracts.ReferenceKind.FILE),
                                entry.occurrence().admissibleKinds(), entry.toString())),
                        () -> assertEquals(ResolutionContracts.ReferenceKind.FILE,
                                explicitFileQualifier.occurrence().kind()),
                        () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.FILE),
                                explicitFileQualifier.occurrence().admissibleKinds()),
                        () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                                explicitFileQualifier.status(), explicitFileQualifier.toString()),
                        () -> assertEquals(ResolutionContracts.SemanticEntityDomain.FILE_ENTITY,
                                explicitFileQualifier.candidates().get(0).entityId().domain())),
                () -> assertAll("ambiguous generic qualifiers never collapse DATA and FILE to strings",
                        () -> assertAmbiguousDataCandidates(analysis.resolution(), unit,
                                "ITEM OF QUALIFIER-X", dataItem.id(), fileItem.id()),
                        () -> assertAmbiguousDataCandidates(analysis.resolution(), unit,
                                "ITEM OF FILE-RECORD IN QUALIFIER-X", dataItem.id(), fileItem.id())));
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
    void excludesCommonProgramAndItsDescendantsFromTheCommonProgramsCallingRegion() throws Exception {
        Analysis analysis = analyze(COMMON_PROGRAM_VISIBILITY, Optional.empty());
        ResolutionContracts.ProgramUnitId root = unit(analysis, "COMMON-ROOT");
        ResolutionContracts.ProgramUnitId sibling = unit(analysis, "SIBLING");
        ResolutionContracts.ProgramUnitId common = unit(analysis, "COMMON-C");
        ResolutionContracts.ProgramUnitId descendant = unit(analysis, "DESCENDANT");
        ResolutionContracts.ProgramUnitId privateSibling = unit(analysis, "PRIVATE-SIBLING");
        ResolutionContracts.ProgramUnitId a = unit(analysis, "A");
        ResolutionContracts.ProgramUnitId b = unit(analysis, "B");
        ResolutionContracts.ProgramUnitId deepCommon = unit(analysis, "DEEP-C");
        ResolutionContracts.ProgramUnitId d = unit(analysis, "D");

        assertAll("fixture topology and declaration categories",
                () -> assertNull(analysis.model().find(root).orElseThrow().parentId()),
                () -> assertEquals(root, analysis.model().find(sibling).orElseThrow().parentId()),
                () -> assertEquals(root, analysis.model().find(common).orElseThrow().parentId()),
                () -> assertEquals(common, analysis.model().find(descendant).orElseThrow().parentId()),
                () -> assertEquals(root, analysis.model().find(privateSibling).orElseThrow().parentId()),
                () -> assertEquals(root, analysis.model().find(a).orElseThrow().parentId()),
                () -> assertEquals(a, analysis.model().find(b).orElseThrow().parentId()),
                () -> assertEquals(a, analysis.model().find(deepCommon).orElseThrow().parentId()),
                () -> assertEquals(deepCommon, analysis.model().find(d).orElseThrow().parentId()),
                () -> assertTrue(analysis.model().find(common).orElseThrow().program().attributes().common()),
                () -> assertTrue(analysis.model().find(deepCommon).orElseThrow().program().attributes().common()),
                () -> assertFalse(analysis.model().find(privateSibling).orElseThrow().program().attributes().common()));

        assertAll("COMMON scope excludes the COMMON program subtree",
                () -> assertProgramCandidate(analysis.resolution(), root, "'COMMON-C'", common),
                () -> assertProgramCandidate(analysis.resolution(), sibling, "'COMMON-C'", common),
                () -> assertInvisibleInternalProgram(analysis.resolution(), descendant, "'COMMON-C'"),
                () -> assertInvisibleInternalProgram(analysis.resolution(), sibling, "'PRIVATE-SIBLING'"),
                () -> assertProgramCandidate(analysis.resolution(), a, "'DEEP-C'", deepCommon),
                () -> assertProgramCandidate(analysis.resolution(), b, "'DEEP-C'", deepCommon),
                () -> assertInvisibleInternalProgram(analysis.resolution(), d, "'DEEP-C'"),
                () -> assertInvisibleInternalProgram(analysis.resolution(), root, "'DEEP-C'"));
    }

    @Test
    void separatesLiteralProgramNameSpellingFromCanonicalProgramIdentity() throws Exception {
        Analysis analysis = analyze(LITERAL_PROGRAM_NAME, Optional.empty());
        assertEquals(2, analysis.model().programUnits().size());
        CompilationUnitModel.ProgramUnit outer = analysis.model().programUnits().get(0);
        CompilationUnitModel.ProgramUnit child = analysis.model().programUnits().get(1);
        assertEquals(outer.id(), child.parentId());
        Ast.ProgramReference target = nodes(outer.program(), Ast.ProgramReference.class).stream()
                .findFirst().orElseThrow();
        ReferenceResolution.Entry binding = analysis.resolution().find(
                outer.id(), "'CHILD'", ResolutionContracts.ReferenceRole.CALL_TARGET)
                .stream().findFirst().orElseThrow();

        assertAll("literal program name representation and binding",
                () -> assertAll("written spelling is distinct from semantic identity",
                        () -> assertEquals("'CHILD'", child.program().name()),
                        () -> assertTrue(child.program().attributes().writtenText().contains("'CHILD'")),
                        () -> assertEquals("CHILD", child.id().canonicalProgramName()),
                        () -> assertEquals("CHILD", target.programName()),
                        () -> assertEquals("'CHILD'", target.writtenText()),
                        () -> assertEquals("'CHILD'", binding.occurrence().writtenText()),
                        () -> assertEquals(ResolutionContracts.ReferenceKind.PROGRAM,
                                binding.occurrence().kind())),
                () -> assertAll("CALL literal binds to the literal PROGRAM-ID semantic identity",
                        () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                                binding.status(), binding.toString()),
                        () -> assertEquals(ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                                binding.reason(), binding.toString()),
                        () -> assertEquals(1, binding.candidates().size(), binding.toString()),
                        () -> {
                            assertFalse(binding.candidates().isEmpty(), binding.toString());
                            ReferenceResolution.Candidate candidate = binding.candidates().get(0);
                            assertEquals(ResolutionContracts.SemanticEntityDomain.PROGRAM_UNIT,
                                    candidate.entityId().domain());
                            assertEquals(child.id(), candidate.entityId().programUnitId());
                        }));
    }

    @Test
    void requiresExplicitPgmnamePolicyWhenProgramIdentityDependsOnCompilerOption() throws Exception {
        GrammarBinding grammar = Bindings.proleap();
        Parser preprocessor = grammar.preprocessorParser(new CommonTokenStream(
                grammar.preprocessorLexer(CharStreams.fromString("CBL PGMNAME(LONGMIXED)\n"))));
        ParseTree optionTree = grammar.preprocessorStart(preprocessor);
        String optionTreeText = optionTree.toStringTree(preprocessor);
        PreprocessorEngine.Outcome transported = new PreprocessorEngine(
                grammar, new CopybookLibrary(Path.of("src/test/resources")))
                .process("CBL PGMNAME(LONGMIXED)\n", "options.cbl");
        Set<String> outcomeComponents = Arrays.stream(PreprocessorEngine.Outcome.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> policyComponents = Arrays.stream(
                        ResolutionContracts.CobolResolutionPolicy.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        ExternalProgramCatalog catalog = canonicalName -> switch (canonicalName) {
            case "SIMPLE" -> List.of(new ExternalProgramCatalog.Program(
                    1, "policy-catalog", canonicalName, Map.of()));
            case "mixed-Child" -> List.of(new ExternalProgramCatalog.Program(
                    2, "policy-catalog", canonicalName, Map.of()));
            case "LONG-NAME-ABC" -> List.of(new ExternalProgramCatalog.Program(
                    3, "policy-catalog", canonicalName, Map.of()));
            case "LONG0NAM" -> List.of(new ExternalProgramCatalog.Program(
                    4, "policy-catalog", canonicalName, Map.of()));
            default -> List.of();
        };
        Analysis analysis = analyze(PROGRAM_NAME_POLICY, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.UNSPECIFIED);
        ResolutionContracts.ProgramUnitId caller = unit(analysis, "POLICY-CALLER");
        Analysis longMixed = analyze(PROGRAM_NAME_POLICY, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.LONGMIXED);
        Analysis compat = analyze(PROGRAM_NAME_POLICY, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.COMPAT);

        assertAll("PGMNAME is an explicit input to program identity",
                () -> assertAll("frontend recognizes and transports the compiler option",
                        () -> assertEquals(0, preprocessor.getNumberOfSyntaxErrors()),
                        () -> assertTrue(optionTreeText.contains("PGMNAME"), optionTreeText),
                        () -> assertTrue(optionTreeText.contains("LONGMIXED"), optionTreeText),
                        () -> assertEquals(ResolutionContracts.PgmnameMode.LONGMIXED,
                                transported.pgmnameMode()),
                        () -> assertTrue(transported.compilerOptions().stream().anyMatch(option ->
                                option.name().equals("PGMNAME") && option.value().equals("LONGMIXED"))),
                        () -> assertTrue(outcomeComponents.stream().anyMatch(
                                        component -> component.toLowerCase(Locale.ROOT).contains("compileroption")
                                                || component.toLowerCase(Locale.ROOT).contains("pgmname")),
                                "PreprocessorEngine.Outcome must transport parsed compiler options")),
                () -> assertTrue(policyComponents.stream().anyMatch(
                                component -> component.toLowerCase(Locale.ROOT).contains("pgmname")),
                        "CobolResolutionPolicy must expose an explicit PGMNAME mode"),
                () -> assertAll("only option-independent identity may resolve without PGMNAME",
                        () -> assertEntry(analysis.resolution(), caller, "'SIMPLE'",
                                ResolutionContracts.ReferenceRole.CALL_TARGET,
                                ResolutionContracts.ResolutionStatus.RESOLVED,
                                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1),
                        () -> assertEntry(analysis.resolution(), caller, "'mixed-Child'",
                                ResolutionContracts.ReferenceRole.CALL_TARGET,
                                ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                                ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, 1),
                        () -> assertEntry(analysis.resolution(), caller, "'LONG-NAME-ABC'",
                                ResolutionContracts.ReferenceRole.CALL_TARGET,
                                ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                                ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, 2)),
                () -> assertAll("explicit modes canonicalize external identities",
                        () -> assertEquals("mixed-Child", assertEntry(longMixed.resolution(),
                                unit(longMixed, "POLICY-CALLER"), "'mixed-Child'",
                                ResolutionContracts.ReferenceRole.CALL_TARGET,
                                ResolutionContracts.ResolutionStatus.RESOLVED,
                                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1)
                                .candidates().get(0).canonicalName()),
                        () -> assertEquals("LONG0NAM", assertEntry(compat.resolution(),
                                unit(compat, "POLICY-CALLER"), "'LONG-NAME-ABC'",
                                ResolutionContracts.ReferenceRole.CALL_TARGET,
                                ResolutionContracts.ResolutionStatus.RESOLVED,
                                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1)
                                .candidates().get(0).canonicalName())));
    }

    @Test
    void appliesPgmnameCanonicalizationBeforeExternalCatalogLookup() throws Exception {
        ExternalProgramCatalog catalog = canonicalName -> List.of(new ExternalProgramCatalog.Program(
                Math.abs(canonicalName.hashCode()), "canonicalization-catalog", canonicalName, Map.of()));
        Analysis compat = analyze(EXTERNAL_PROGRAM_NAMES, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.COMPAT);
        Analysis upper = analyze(EXTERNAL_PROGRAM_NAMES, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.LONGUPPER);
        Analysis mixed = analyze(EXTERNAL_PROGRAM_NAMES, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.LONGMIXED);

        assertAll("external catalog receives the policy-dependent canonical name",
                () -> assertExternalCanonical(compat, "'LONG-NAME-ABC'", "LONG0NAM"),
                () -> assertExternalCanonical(compat, "'1PROG'", "APROG"),
                () -> assertExternalCanonical(compat, "'$PROG'", "JPROG"),
                () -> assertExternalCanonical(upper, "'PROG-A'", "PROG0A"),
                () -> assertExternalCanonical(upper, "'-PROG'", "JPROG"),
                () -> assertExternalCanonical(mixed, "'mixed-Child'", "mixed-Child"));
    }

    @Test
    void keepsUnspecifiedExternalProgramIdentityConservativeForLeadingDigits() throws Exception {
        ExternalProgramCatalog catalog = canonicalName -> switch (canonicalName) {
            case "SIMPLE" -> List.of(new ExternalProgramCatalog.Program(
                    1, "unspecified-pgmname", "SIMPLE", Map.of()));
            case "APROG" -> List.of(new ExternalProgramCatalog.Program(
                    2, "unspecified-pgmname", "COMPAT-1PROG", Map.of("mode", "FOLDED")));
            case "1PROG" -> List.of(new ExternalProgramCatalog.Program(
                    3, "unspecified-pgmname", "LONGMIXED-1PROG", Map.of("mode", "MIXED")));
            default -> List.of();
        };
        Analysis unspecified = analyze(UNSPECIFIED_EXTERNAL_PROGRAM, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.UNSPECIFIED);
        Analysis compat = analyze(UNSPECIFIED_EXTERNAL_PROGRAM, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.COMPAT);
        Analysis upper = analyze(UNSPECIFIED_EXTERNAL_PROGRAM, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.LONGUPPER);
        Analysis mixed = analyze(UNSPECIFIED_EXTERNAL_PROGRAM, Optional.of(catalog),
                ResolutionContracts.PgmnameMode.LONGMIXED);

        ReferenceResolution.Entry simple = assertEntry(unspecified.resolution(),
                unit(unspecified, "UNSPECIFIED-EXTERNAL-CALLER"), "'SIMPLE'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals(1, simple.candidates().get(0).entityId().localId());

        ReferenceResolution.Entry dependent = assertEntry(unspecified.resolution(),
                unit(unspecified, "UNSPECIFIED-EXTERNAL-CALLER"), "'1PROG'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, 2);
        assertEquals(Set.of(2, 3), dependent.candidates().stream()
                .map(candidate -> candidate.entityId().localId()).collect(
                        java.util.stream.Collectors.toSet()));

        assertEquals(2, assertEntry(compat.resolution(), unit(compat, "UNSPECIFIED-EXTERNAL-CALLER"),
                "'1PROG'", ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1)
                .candidates().get(0).entityId().localId());
        assertEquals(2, assertEntry(upper.resolution(), unit(upper, "UNSPECIFIED-EXTERNAL-CALLER"),
                "'1PROG'", ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1)
                .candidates().get(0).entityId().localId());
        assertEquals(3, assertEntry(mixed.resolution(), unit(mixed, "UNSPECIFIED-EXTERNAL-CALLER"),
                "'1PROG'", ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1)
                .candidates().get(0).entityId().localId());
    }

    @Test
    void honorsLongmixedCaseWhenBindingNestedPrograms() throws Exception {
        Analysis mixed = analyze(LONGMIXED_NESTED_PROGRAM, Optional.empty(),
                ResolutionContracts.PgmnameMode.LONGMIXED);
        Analysis upper = analyze(LONGMIXED_NESTED_PROGRAM, Optional.empty(),
                ResolutionContracts.PgmnameMode.LONGUPPER);
        Analysis compat = analyze(LONGMIXED_NESTED_PROGRAM, Optional.empty(),
                ResolutionContracts.PgmnameMode.COMPAT);
        ResolutionContracts.ProgramUnitId mixedCaller = unit(mixed, "'Outer'");
        ResolutionContracts.ProgramUnitId mixedChild = unit(mixed, "'mixed-Child'");

        assertAll("ProgramUnitId remains structural and independent from the lookup policy",
                () -> assertEquals("MIXED-CHILD", mixedChild.canonicalProgramName()),
                () -> assertEquals(mixedChild, unit(upper, "'mixed-Child'")),
                () -> assertEquals(mixedChild, unit(compat, "'mixed-Child'")));
        assertAll("LONGMIXED nested lookup preserves written case",
                () -> assertProgramCandidate(mixed.resolution(), mixedCaller,
                        "'mixed-Child'", mixedChild),
                () -> assertEntry(mixed.resolution(), mixedCaller, "'MIXED-CHILD'",
                        ResolutionContracts.ReferenceRole.CALL_TARGET,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionReason.EXTERNAL_CATALOG_NOT_PROVIDED, 0));
        assertAll("case-folding modes retain case-insensitive nested lookup",
                () -> assertProgramCandidate(upper.resolution(), unit(upper, "'Outer'"),
                        "'MIXED-CHILD'", unit(upper, "'mixed-Child'")),
                () -> assertProgramCandidate(compat.resolution(), unit(compat, "'Outer'"),
                        "'MIXED-CHILD'", unit(compat, "'mixed-Child'")));
    }

    @Test
    void keepsUnspecifiedNestedProgramIdentityConservativeWhenModesDisagree() throws Exception {
        Analysis unspecified = analyze(LONGMIXED_NESTED_PROGRAM, Optional.empty(),
                ResolutionContracts.PgmnameMode.UNSPECIFIED);
        ResolutionContracts.ProgramUnitId caller = unit(unspecified, "'Outer'");
        ResolutionContracts.ProgramUnitId child = unit(unspecified, "'mixed-Child'");

        assertAll("option-independent exact spelling may bind",
                () -> assertProgramCandidate(unspecified.resolution(), caller,
                        "'mixed-Child'", child));
        ReferenceResolution.Entry dependent = assertEntry(unspecified.resolution(), caller,
                "'MIXED-CHILD'", ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, 1);
        assertEquals(child, dependent.candidates().get(0).entityId().programUnitId());
    }

    @Test
    void usesOptionalExternalCatalogAndPreservesAllReturnedCandidates() throws Exception {
        ExternalProgramCatalog catalog = canonicalName -> switch (canonicalName) {
            case "EXTERNAL0ONE" -> List.of(new ExternalProgramCatalog.Program(10, "fake", "EXTERNAL-ONE", Map.of()));
            case "EXTERNAL0MANY" -> List.of(
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

    private static void assertFileEntity(SymbolTable table, String name, int declarations, String visibility) {
        SymbolTable.Entity entity = table.entities().stream()
                .filter(candidate -> candidate.kind() == SymbolTable.EntityKind.FILE)
                .filter(candidate -> candidate.canonicalName().equals(SymbolTable.canonical(name)))
                .findFirst().orElseThrow(() -> new AssertionError("missing FILE entity " + name));
        assertEquals(declarations, entity.declarationSymbolIds().size());
        assertEquals(visibility, entity.attributes().get("visibility"));
    }

    private static void assertFileCandidate(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId useUnit, String name,
            ResolutionContracts.ProgramUnitId declarationUnit, int declarations, String visibility) {
        ReferenceResolution.Entry entry = assertEntry(resolution, useUnit, name,
                ResolutionContracts.ReferenceRole.FILE_OPERATION,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        ReferenceResolution.Candidate candidate = entry.candidates().get(0);
        assertEquals(ResolutionContracts.ReferenceKind.FILE, candidate.kind());
        assertEquals(ResolutionContracts.SemanticEntityDomain.FILE_ENTITY, candidate.entityId().domain());
        assertEquals(declarationUnit, candidate.entityId().programUnitId());
        assertEquals(declarations, candidate.declarationSymbolIds().size());
        assertEquals(visibility, candidate.attributes().get("visibility"));
    }

    private static void assertDataCandidate(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId unit,
            String writtenText, int expectedSymbolId) {
        ReferenceResolution.Entry entry = assertEntry(resolution, unit, writtenText,
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        ReferenceResolution.Candidate candidate = entry.candidates().get(0);
        assertEquals(ResolutionContracts.ReferenceKind.DATA, candidate.kind());
        assertEquals(ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, candidate.entityId().domain());
        assertEquals(expectedSymbolId, candidate.entityId().localId());
    }

    private static void assertAmbiguousDataCandidates(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId unit,
            String writtenText, int firstSymbolId, int secondSymbolId) {
        ReferenceResolution.Entry entry = assertEntry(resolution, unit, writtenText,
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
        assertEquals(Set.of(firstSymbolId, secondSymbolId), entry.candidates().stream()
                .map(candidate -> candidate.entityId().localId())
                .collect(java.util.stream.Collectors.toSet()));
        assertTrue(entry.candidates().stream().allMatch(candidate ->
                candidate.kind() == ResolutionContracts.ReferenceKind.DATA));
    }

    private static void assertProgramCandidate(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId caller,
            String writtenText, ResolutionContracts.ProgramUnitId declarationUnit) {
        ReferenceResolution.Entry entry = assertEntry(resolution, caller, writtenText,
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        ReferenceResolution.Candidate candidate = entry.candidates().get(0);
        assertEquals(ResolutionContracts.ReferenceKind.PROGRAM, candidate.kind());
        assertEquals(ResolutionContracts.SemanticEntityDomain.PROGRAM_UNIT, candidate.entityId().domain());
        assertEquals(declarationUnit, candidate.entityId().programUnitId());
    }

    private static void assertExternalCanonical(Analysis analysis, String writtenText,
                                                String expectedCanonical) {
        ResolutionContracts.ProgramUnitId caller = unit(analysis, "EXTERNAL-NAME-CALLER");
        ReferenceResolution.Entry entry = assertEntry(analysis.resolution(), caller, writtenText,
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals(ResolutionContracts.SemanticEntityDomain.EXTERNAL_PROGRAM,
                entry.candidates().get(0).entityId().domain());
        assertEquals(expectedCanonical, entry.candidates().get(0).canonicalName());
    }

    private static void assertInvisibleInternalProgram(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId caller, String writtenText) {
        assertEntry(resolution, caller, writtenText,
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.EXTERNAL_CATALOG_NOT_PROVIDED, 0);
    }

    private static boolean hasFileAncestor(SymbolTable table, SymbolTable.Symbol symbol) {
        int scopeId = symbol.scopeId();
        while (scopeId >= 0) {
            SymbolTable.Scope scope = table.scopes().get(scopeId);
            if (scope.kind() == SymbolTable.ScopeKind.FILE_DESCRIPTION) return true;
            scopeId = scope.parentId();
        }
        return false;
    }

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return List.copyOf(result);
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
        return analyze(sourcePath, catalog, ResolutionContracts.PgmnameMode.LONGUPPER);
    }

    private static Analysis analyze(Path sourcePath, Optional<ExternalProgramCatalog> catalog,
                                    ResolutionContracts.PgmnameMode pgmnameMode) throws Exception {
        Path file = sourcePath.toAbsolutePath();
        String source = SourceNormalizer.fixed(Files.readString(file, StandardCharsets.UTF_8));
        return analyzeSource(file, source, catalog, pgmnameMode);
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
        return analyzeSource(file, source, catalog, ResolutionContracts.PgmnameMode.LONGUPPER);
    }

    private static Analysis analyzeSource(Path file, String source,
                                          Optional<ExternalProgramCatalog> catalog,
                                          ResolutionContracts.PgmnameMode pgmnameMode) throws Exception {
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
                "test-policy", "1", ResolutionContracts.QualifyMode.STANDARD, pgmnameMode);
        ReferenceResolution resolution = new CobolReferenceResolver(policy, catalog)
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
