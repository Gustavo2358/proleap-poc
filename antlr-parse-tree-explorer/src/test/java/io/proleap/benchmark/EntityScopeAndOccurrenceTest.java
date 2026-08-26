package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EntityScopeAndOccurrenceTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/resolution/entities-and-occurrences.cbl");

    @Test
    void unifiesSelectAndFileDescriptionIntoEntitiesWithoutBindingRelations() throws Exception {
        Parsed parsed = parseFixture();
        SymbolTable table = parsed.symbolTable();
        Map<String, SymbolTable.Entity> files = table.entities().stream()
                .filter(entity -> entity.kind() == SymbolTable.EntityKind.FILE)
                .collect(Collectors.toMap(SymbolTable.Entity::canonicalName, entity -> entity));

        assertEquals(Set.of("BOTH-FILE", "SELECT-ONLY", "FD-ONLY", "SORT-FILE"), files.keySet());
        assertEquals(2, files.get("BOTH-FILE").declarationSymbolIds().size());
        assertEquals(1, files.get("SELECT-ONLY").declarationSymbolIds().size());
        assertEquals(1, files.get("FD-ONLY").declarationSymbolIds().size());
        assertEquals(1, files.get("SORT-FILE").declarationSymbolIds().size());

        Map<SymbolTable.RelationKind, Long> relations = table.declarationRelations().stream()
                .collect(Collectors.groupingBy(SymbolTable.DeclarationRelation::kind, Collectors.counting()));
        assertEquals(1L, relations.get(SymbolTable.RelationKind.REDEFINES));
        assertEquals(1L, relations.get(SymbolTable.RelationKind.RENAMES_FROM));
        assertEquals(1L, relations.get(SymbolTable.RelationKind.RENAMES_THROUGH));
        assertEquals(1L, relations.get(SymbolTable.RelationKind.OCCURS_DEPENDING_ON));
        assertEquals(1L, relations.get(SymbolTable.RelationKind.OCCURS_INDEX));
        assertTrue(table.declarationRelations().stream().allMatch(relation -> relation.bindingStatus().equals("NOT_PERFORMED")));
        assertTrue(table.declarationRelations().stream().allMatch(relation -> relation.referenceAstNodeId() >= 0));
    }

    @Test
    void mapsEveryAstNodeToOneDeterministicOwningScope() throws Exception {
        Parsed parsed = parseFixture();
        AstScopeIndex index = AstScopeIndex.build(parsed.program(), parsed.symbolTable());
        List<Ast.Node> nodes = nodes(parsed.program(), Ast.Node.class);

        assertEquals(nodes.size(), index.mappedNodeCount());
        for (Ast.Node node : nodes) {
            int scopeId = index.scopeId(node);
            assertTrue(scopeId >= 0 && scopeId < parsed.symbolTable().scopes().size());
            assertEquals(scopeId, index.scopeIdForAstNodeId(node.meta().id()));
        }
        assertEquals(index.scopeId(nodes.get(10)),
                AstScopeIndex.build(parsed.program(), parsed.symbolTable()).scopeId(nodes.get(10)));
    }

    @Test
    void collectsExactTypedOccurrencesWithStructuralRolesAndNoDuplicates() throws Exception {
        Parsed parsed = parseFixture();
        AstScopeIndex scopes = AstScopeIndex.build(parsed.program(), parsed.symbolTable());
        ReferenceOccurrences result = new ReferenceOccurrenceCollector().collect(
                parsed.programUnitId(), parsed.program(), scopes);
        List<ReferenceOccurrences.Occurrence> occurrences = result.occurrences();

        assertEquals(List.of(
                        "REDEFINES_TARGET:BASE-ITEM",
                        "OCCURS_DEPENDING_ON:TABLE-COUNT",
                        "OCCURS_INDEX:TABLE-IDX",
                        "RENAMES_FROM:BASE-ITEM",
                        "RENAMES_THROUGH:TABLE-COUNT",
                        "VALUE_READ:FILE-VALUE OF BOTH-RECORD IN BOTH-FILE",
                        "QUALIFIER_COMPONENT:BOTH-RECORD",
                        "QUALIFIER_COMPONENT:BOTH-FILE",
                        "VALUE_WRITE:WS-TARGET(WS-IDX)(WS-OFFSET:WS-LENGTH)",
                        "SUBSCRIPT:WS-IDX",
                        "REFERENCE_MODIFICATION_OFFSET:WS-OFFSET",
                        "REFERENCE_MODIFICATION_LENGTH:WS-LENGTH",
                        "VALUE_READ:LINAGE-COUNTER IN BOTH-FILE",
                        "QUALIFIER_COMPONENT:BOTH-FILE",
                        "VALUE_WRITE:WS-TARGET",
                        "CONTEXT_DEPENDENT:WS-TARGET"),
                occurrences.stream().map(occurrence -> occurrence.role() + ":" + occurrence.writtenText()).toList(),
                "the fixture is an exact loss/duplication oracle, not a minimum-count assertion");
        assertEquals(occurrences.size(), occurrences.stream()
                .map(ReferenceOccurrences.Occurrence::referenceAstNodeId).distinct().count(),
                "one AST reference node must produce exactly one occurrence");
        assertEquals(List.copyOf(occurrences), occurrences.stream()
                .sorted(Comparator.comparingInt(ReferenceOccurrences.Occurrence::id)).toList());
        for (int index = 0; index < occurrences.size(); index++) assertEquals(index, occurrences.get(index).id());

        assertOccurrence(occurrences, "BASE-ITEM", ResolutionContracts.ReferenceRole.REDEFINES_TARGET);
        assertOccurrence(occurrences, "BASE-ITEM", ResolutionContracts.ReferenceRole.RENAMES_FROM);
        assertOccurrence(occurrences, "TABLE-COUNT", ResolutionContracts.ReferenceRole.RENAMES_THROUGH);
        assertOccurrence(occurrences, "TABLE-COUNT", ResolutionContracts.ReferenceRole.OCCURS_DEPENDING_ON);
        assertOccurrence(occurrences, "TABLE-IDX", ResolutionContracts.ReferenceRole.OCCURS_INDEX);
        assertOccurrence(occurrences, "FILE-VALUE OF BOTH-RECORD IN BOTH-FILE",
                ResolutionContracts.ReferenceRole.VALUE_READ);
        assertOccurrence(occurrences, "BOTH-RECORD", ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT);
        ReferenceOccurrences.Occurrence fileQualifier = occurrences.stream()
                .filter(occurrence -> occurrence.writtenText().equals("BOTH-FILE"))
                .filter(occurrence -> occurrence.role() == ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT)
                .filter(occurrence -> occurrence.kind() == ResolutionContracts.ReferenceKind.FILE)
                .findFirst().orElseThrow(() -> new AssertionError("missing grammar-proven FILE qualifier"));
        assertEquals("fileName", fileQualifier.grammarRule());
        assertOccurrence(occurrences, "WS-TARGET(WS-IDX)(WS-OFFSET:WS-LENGTH)",
                ResolutionContracts.ReferenceRole.VALUE_WRITE);
        assertOccurrence(occurrences, "WS-IDX", ResolutionContracts.ReferenceRole.SUBSCRIPT);
        assertOccurrence(occurrences, "WS-OFFSET",
                ResolutionContracts.ReferenceRole.REFERENCE_MODIFICATION_OFFSET);
        assertOccurrence(occurrences, "WS-LENGTH",
                ResolutionContracts.ReferenceRole.REFERENCE_MODIFICATION_LENGTH);

        assertTrue(occurrences.stream().filter(occurrence -> occurrence.writtenText().equals("BOTH-RECORD"))
                .noneMatch(occurrence -> occurrence.role() == ResolutionContracts.ReferenceRole.VALUE_READ),
                "qualifiers narrow a reference; they are not independent reads");
        assertTrue(occurrences.stream().anyMatch(occurrence -> occurrence.writtenText().equals("WS-TARGET")
                        && occurrence.role() == ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT
                        && occurrence.preservation() == ReferenceOccurrences.Preservation.PRESERVED_CONTAINER),
                "a reference retained by a preserved statement remains explicit and conservative");
        assertTrue(occurrences.stream().allMatch(occurrence -> occurrence.scopeId() == scopes.scopeIdForAstNodeId(
                occurrence.referenceAstNodeId())));
        assertEquals("NOT_PERFORMED", result.bindingStatus());
    }

    @Test
    void collectsAllNominalNamespacesWithoutTurningThemIntoBindings() throws Exception {
        Parsed parsed = parse(Path.of("src/test/resources/cobol/resolution/baseline-compilation-unit.cbl"));
        AstScopeIndex scopes = AstScopeIndex.build(parsed.program(), parsed.symbolTable());
        ReferenceOccurrences result = new ReferenceOccurrenceCollector().collect(
                parsed.programUnitId(), parsed.program(), scopes);

        assertOccurrence(result.occurrences(), "TEST-FILE", ResolutionContracts.ReferenceRole.FILE_OPERATION);
        ReferenceOccurrences.Occurrence call = assertOccurrence(result.occurrences(), "'EXTERNAL-PROGRAM'",
                ResolutionContracts.ReferenceRole.CALL_TARGET);
        assertEquals(ResolutionContracts.ReferenceKind.PROGRAM, call.kind());
        ReferenceOccurrences.Occurrence goTo = assertOccurrence(result.occurrences(), "FINISH-FIRST",
                ResolutionContracts.ReferenceRole.GO_TO_TARGET);
        assertEquals(ResolutionContracts.ReferenceKind.PROCEDURE, goTo.kind());
        assertOccurrence(result.occurrences(), "WS-VALUE", ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT);
        assertEquals("NOT_PERFORMED", result.bindingStatus());
    }

    private static ReferenceOccurrences.Occurrence assertOccurrence(
            List<ReferenceOccurrences.Occurrence> occurrences, String text,
            ResolutionContracts.ReferenceRole role) {
        return occurrences.stream().filter(occurrence -> occurrence.writtenText().equals(text))
                .filter(occurrence -> occurrence.role() == role).findFirst()
                .orElseThrow(() -> new AssertionError("missing occurrence " + role + " " + text
                        + "; actual=" + occurrences.stream()
                        .map(occurrence -> occurrence.role() + ":" + occurrence.writtenText()).toList()));
    }

    private static Parsed parseFixture() throws Exception {
        return parse(FIXTURE);
    }

    private static Parsed parse(Path sourcePath) throws Exception {
        Path file = sourcePath.toAbsolutePath();
        String source = SourceNormalizerTestSupport.fixed(Files.readString(file, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.proleap();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, file.getFileName().toString()))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        Ast.Program program = new AstBuilder(parser, source,
                SourceMap.identity(source, file.getFileName().toString()), ids, sizes).build(tree).program();
        SymbolTable table = new SymbolTableBuilder().build(program);
        ResolutionContracts.ProgramUnitId unitId = new ResolutionContracts.ProgramUnitId(
                file.getFileName().toString().toUpperCase(Locale.ROOT), List.of(0),
                SymbolTable.canonical(program.name()));
        return new Parsed(program, table, unitId);
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private static <T extends Ast.Node> List<T> nodes(Ast.Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Ast.Node child : Ast.children(root)) result.addAll(nodes(child, type));
        return result;
    }

    private record Parsed(Ast.Program program, SymbolTable symbolTable,
                          ResolutionContracts.ProgramUnitId programUnitId) { }
}
