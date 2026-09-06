package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;

import static io.github.gustavo2358.cobolexplorer.ResolutionContracts.*;
import static org.junit.jupiter.api.Assertions.*;

/** F-02 oracles: controlled product corruption, independent of nominal lookup. */
class SemanticProductIntegrityValidatorTest {
    private static AstBoundaryTestSupport.Analysis baseline;

    @BeforeAll
    static void buildProducts() {
        baseline = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. INTEGRITY.
                ENVIRONMENT DIVISION.
                INPUT-OUTPUT SECTION.
                FILE-CONTROL.
                    SELECT INPUT-FILE ASSIGN TO 'INPUTDD'.
                DATA DIVISION.
                FILE SECTION.
                FD INPUT-FILE.
                01 INPUT-RECORD PIC X.
                WORKING-STORAGE SECTION.
                01 GROUP-A.
                   05 ITEM-A PIC X.
                   05 ITEM-B REDEFINES ITEM-A PIC X.
                   66 ALIAS-A RENAMES ITEM-A THRU ITEM-B.
                01 TABLE-A OCCURS 2 TIMES INDEXED BY INDEX-A PIC X.
                01 FLAG-A GLOBAL PIC X.
                   88 READY-A VALUE 'Y'.
                PROCEDURE DIVISION.
                    READ INPUT-FILE INTO FLAG-A.
                    MOVE ITEM-A TO FLAG-A.
                    SET INDEX-A TO 1.
                    SET READY-A TO TRUE.
                    PERFORM FINISH-A.
                    CALL 'CHILD-A'.
                    CALL 'EXTERNAL-A'.
                    DISPLAY MISSING-A.
                FINISH-A.
                    GOBACK.
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CHILD-A.
                PROCEDURE DIVISION.
                    DISPLAY FLAG-A.
                    GOBACK.
                END PROGRAM CHILD-A.
                END PROGRAM INTEGRITY.
                """, "integrity.cbl");
    }

    @Test
    void acceptsEveryCandidateDomainAndAncestralIdentityWithoutMutatingProducts() {
        Products products = new Products(baseline);
        Set<SemanticEntityDomain> domains = EnumSet.noneOf(SemanticEntityDomain.class);
        Set<ReferenceKind> kinds = EnumSet.noneOf(ReferenceKind.class);
        for (var entry : products.resolution.entries()) {
            for (var candidate : entry.candidates()) {
                domains.add(candidate.entityId().domain());
                kinds.add(candidate.kind());
            }
        }
        assertEquals(EnumSet.allOf(SemanticEntityDomain.class), domains);
        assertTrue(kinds.contains(ReferenceKind.CONDITION));
        assertTrue(products.resolution.entries().stream().anyMatch(entry -> entry.candidates().stream()
                .anyMatch(candidate -> candidate.entityId().domain() == SemanticEntityDomain.DATA_SYMBOL
                        && !candidate.entityId().programUnitId().equals(entry.occurrence().programUnitId()))));
        var before = List.copyOf(products.resolution.entries());
        products.validate();
        products.validate();
        assertEquals(before, products.resolution.entries());
        assertEquals(baseline.tables(), products.tables);
        assertEquals(baseline.scopes(), products.scopes);
        assertEquals(baseline.occurrences(), products.occurrences);
    }

    @ParameterizedTest
    @ValueSource(strings = {"coverage-states.cbl", "entities-and-occurrences.cbl",
            "nested-global-file.cbl", "program-binding.cbl", "nested-namespace-shadowing.cbl",
            "filler-redefines-owner.cbl", "renames-structural-binding.cbl"})
    void acceptsExistingSemanticClasses(String fixture) throws Exception {
        var analysis = AstBoundaryTestSupport.analyze(Files.readString(
                Path.of("src/test/resources/cobol/resolution", fixture)), fixture);
        new Products(analysis).validate();
        AstBoundaryTestSupport.assertActualProductsJoin(analysis);
    }

    @ParameterizedTest
    @EnumSource(ResolutionStatus.class)
    void legitimateIncompleteStatusesAreNotIntegrityFailures(ResolutionStatus status) {
        Products products = new Products(baseline);
        var original = products.resolution.entries().stream().filter(entry ->
                status == ResolutionStatus.EXTERNAL_OBSERVED
                        ? entry.status() == ResolutionStatus.EXTERNAL_OBSERVED
                        : entry.candidates().stream().anyMatch(c -> c.kind() == ReferenceKind.DATA))
                .findFirst().orElseThrow();
        var data = candidate(SemanticEntityDomain.DATA_SYMBOL);
        var other = baseline.tables().units().get(0).symbolTable().symbols().stream()
                .filter(symbol -> symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM
                        && symbol.id() != data.entityId().localId()).findFirst().orElseThrow();
        var second = new ReferenceResolution.Candidate(new SemanticEntityId(unitId(),
                SemanticEntityDomain.DATA_SYMBOL, other.id()), ReferenceKind.DATA,
                data.writtenName(), data.canonicalName(), List.of(other.id()), Map.of());
        List<ReferenceResolution.Candidate> candidates = switch (status) {
            case EXTERNAL_OBSERVED, UNRESOLVED -> List.of();
            case AMBIGUOUS -> List.of(data, second);
            case RESOLVED, UNSUPPORTED -> List.of(data);
        };
        // Equal names with different identities are valid ambiguity; membership
        // is checked structurally, without re-running nominal lookup.
        ResolutionReason reason = switch (status) {
            case EXTERNAL_OBSERVED -> ResolutionReason.LITERAL_EXTERNAL_PROGRAM;
            case UNRESOLVED -> ResolutionReason.DECLARATION_NOT_FOUND;
            case AMBIGUOUS -> ResolutionReason.MULTIPLE_VALID_CANDIDATES;
            case RESOLVED -> ResolutionReason.UNIQUE_VISIBLE_DECLARATION;
            case UNSUPPORTED -> ResolutionReason.UNSUPPORTED_DIALECT_OPTION;
        };
        products.entry(original.id(), new ReferenceResolution.Entry(original.id(), original.occurrence(),
                status, reason, candidates, List.of(), original.callSemantics()));
        products.validate();
    }

    @ParameterizedTest
    @EnumSource(value = SemanticEntityDomain.class, names = {"PROGRAM_UNIT", "FILE_ENTITY"})
    void rejectsValidCandidatesOutsideTheOccurrenceAdmissibleKinds(SemanticEntityDomain domain) {
        var original = baseline.resolution().entries().stream()
                .filter(e -> e.occurrence().admissibleKinds().equals(Set.of(ReferenceKind.DATA))
                        && e.status() == ResolutionStatus.RESOLVED).findFirst().orElseThrow();
        var incompatible = candidate(domain);
        for (var status : List.of(ResolutionStatus.RESOLVED, ResolutionStatus.AMBIGUOUS, ResolutionStatus.UNSUPPORTED)) {
            Products products = new Products(baseline);
            // The second candidate must also be checked, including under incomplete statuses.
            var candidates = status == ResolutionStatus.RESOLVED ? List.of(incompatible)
                    : List.of(original.candidates().get(0), incompatible);
            products.entry(original.id(), new ReferenceResolution.Entry(original.id(), original.occurrence(),
                    status, status == ResolutionStatus.RESOLVED ? ResolutionReason.UNIQUE_VISIBLE_DECLARATION
                    : ResolutionReason.MULTIPLE_VALID_CANDIDATES, candidates, List.of()));
            rejects(products);
        }
    }

    @Test
    void contextualOccurrencesAcceptDataIndexAndConditionCandidates() {
        var analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CONTEXTUAL-INTEGRITY.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 SUBJECT-A PIC 9.
                01 DATA-A PIC 9.
                01 FLAG-A PIC 9.
                   88 CONDITION-A VALUE 1.
                01 TABLE-A OCCURS 2 INDEXED BY INDEX-A PIC 9.
                PROCEDURE DIVISION.
                    IF SUBJECT-A = 1 OR DATA-A OR INDEX-A OR CONDITION-A
                        CONTINUE
                    END-IF.
                END PROGRAM CONTEXTUAL-INTEGRITY.
                """, "contextual-integrity.cbl");
        Set<ReferenceKind> admissible = Set.of(ReferenceKind.DATA, ReferenceKind.INDEX, ReferenceKind.CONDITION);
        var contextual = analysis.resolution().entries().stream()
                .filter(e -> e.occurrence().admissibleKinds().equals(admissible)).toList();
        assertEquals(3, contextual.size());
        assertTrue(contextual.stream().allMatch(e -> e.occurrence().kind() == ReferenceKind.CONDITION
                && e.status() == ResolutionStatus.RESOLVED));
        assertEquals(admissible, contextual.stream().map(e -> e.selectedCandidate().orElseThrow().kind())
                .collect(java.util.stream.Collectors.toSet()));
        new Products(analysis).validate();
    }

    @ParameterizedTest
    @EnumSource(value = SymbolTable.ScopeKind.class, mode = EnumSource.Mode.EXCLUDE, names = "ROOT")
    void rejectsScopeKindCorruptedBeforeResolution(SymbolTable.ScopeKind kind) {
        Products products = new Products(baseline);
        var scope = products.tables.units().get(0).symbolTable().scopes().stream()
                .filter(s -> s.kind() == kind).findFirst().orElseThrow();
        products.scope(new SymbolTable.Scope(scope.id(), scope.parentId(),
                kind == SymbolTable.ScopeKind.SECTION ? SymbolTable.ScopeKind.DATA_ITEM : SymbolTable.ScopeKind.SECTION,
                scope.name(), scope.ownerSymbolId(), scope.astNodeId()));
        products.resolveAgain();
        rejects(products);
    }

    @ParameterizedTest
    @EnumSource(value = SymbolTable.ScopeKind.class, names = {"ROOT", "DIVISION", "SECTION", "PARAGRAPH"})
    void scopesWithoutNominalDeclarationsCannotAcquireAnOwner(SymbolTable.ScopeKind kind) {
        Products products = new Products(baseline);
        var scope = products.tables.units().get(0).symbolTable().scopes().stream()
                .filter(s -> s.kind() == kind && s.ownerSymbolId() == -1).findFirst().orElseThrow();
        products.scope(new SymbolTable.Scope(scope.id(), scope.parentId(), scope.kind(),
                scope.name(), 0, scope.astNodeId()));
        products.resolveAgain();
        rejects(products);
    }

    @ParameterizedTest
    @EnumSource(value = SymbolTable.ScopeKind.class, names = {"PROGRAM", "FILE_DESCRIPTION", "DATA_ITEM", "PARAGRAPH"})
    void nominalScopeOwnerMustBeTheDeclarationAtItsAnchor(SymbolTable.ScopeKind kind) {
        Products products = new Products(baseline);
        var original = products.tables.units().get(0).symbolTable();
        var scope = original.scopes().stream().filter(s -> s.kind() == kind && s.ownerSymbolId() >= 0)
                .findFirst().orElseThrow();
        var symbols = new ArrayList<>(original.symbols());
        var owner = symbols.get(scope.ownerSymbolId());
        // Keep the old declaration-scope check coherent with the false owner, so
        // only the new scope -> exact declaration identity join rejects this state.
        symbols.set(owner.id(), new SymbolTable.Symbol(owner.id(), owner.kind(), owner.namespace(),
                owner.writtenName(), owner.canonicalName(), scope.id(), owner.declarationAstNodeId(),
                owner.span(), owner.attributes()));
        products.table(new SymbolTable(original.scopes(), symbols, original.diagnostics(), original.entities(),
                original.declarationRelations()));
        int falseOwner = owner.id() == 0 ? 1 : 0;
        products.scope(new SymbolTable.Scope(scope.id(), scope.parentId(), scope.kind(), scope.name(),
                falseOwner, scope.astNodeId()));
        rejects(products);
    }

    @Test
    void fillerOwnerCannotInventQualificationEvenWhenAllCandidatesExist() {
        var analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. FILLER-OWNER.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 NAMED-GROUP.
                   05 VALUE-A PIC X.
                01 FILLER.
                   05 VALUE-A PIC X.
                01 RESULT-A PIC X.
                PROCEDURE DIVISION.
                    MOVE VALUE-A OF NAMED-GROUP TO RESULT-A.
                END PROGRAM FILLER-OWNER.
                """, "filler-owner.cbl");
        Products products = new Products(analysis);
        var table = products.tables.units().get(0).symbolTable();
        var filler = AstBoundaryTestSupport.nodes(analysis, Ast.DataEntry.class).stream()
                .filter(Ast.DataEntry::filler).findFirst().orElseThrow();
        var scope = table.scopes().get(products.scopes.get(products.model.programUnits().get(0).id()).scopeId(filler));
        var nominalOwner = table.symbols().stream().filter(s -> s.writtenName().equals("NAMED-GROUP"))
                .findFirst().orElseThrow();
        var before = products.resolution.entries().stream()
                .filter(e -> e.occurrence().writtenText().equals("VALUE-A OF NAMED-GROUP")).findFirst().orElseThrow();
        assertEquals(ResolutionStatus.RESOLVED, before.status());
        products.scope(new SymbolTable.Scope(scope.id(), scope.parentId(), scope.kind(), scope.name(),
                nominalOwner.id(), scope.astNodeId()));
        products.resolveAgain();
        var after = products.resolution.entries().get(before.id());
        assertEquals(ResolutionStatus.AMBIGUOUS, after.status(), "the false owner changes qualification");
        assertEquals(2, after.candidates().size());
        rejects(products);
    }

    @Test
    void procedureSectionPayloadCannotChangeQualifiedBinding() throws Exception {
        var analysis = AstBoundaryTestSupport.analyze(Files.readString(
                Path.of("src/test/resources/cobol/resolution/procedure-binding.cbl")), "procedure-binding.cbl");
        Products products = new Products(analysis);
        var scope = products.tables.units().get(0).symbolTable().scopes().stream()
                .filter(s -> s.kind() == SymbolTable.ScopeKind.SECTION && s.name().equals("SECTION-A"))
                .findFirst().orElseThrow();
        var before = products.resolution.entries().stream()
                .filter(e -> e.occurrence().writtenText().equals("DUPLICATE-PARA OF SECTION-A")).findFirst().orElseThrow();
        assertEquals(ResolutionStatus.RESOLVED, before.status());
        products.scope(new SymbolTable.Scope(scope.id(), scope.parentId(), scope.kind(),
                "CORRUPTED-SECTION", scope.ownerSymbolId(), scope.astNodeId()));
        products.resolveAgain();
        assertEquals(ResolutionStatus.UNRESOLVED, products.resolution.entries().get(before.id()).status());
        rejects(products);
    }

    enum InventoryCorruption { MISSING_TABLE, EXTRA_TABLE, PARENT, MISSING_SCOPES, EXTRA_SCOPES,
        MISSING_OCCURRENCES, EXTRA_OCCURRENCES, NULL_SCOPES, NULL_OCCURRENCES, CONTAINER_UNIT }

    @ParameterizedTest
    @EnumSource(InventoryCorruption.class)
    void rejectsMismatchedUnitInventories(InventoryCorruption corruption) {
        Products products = new Products(baseline);
        var first = products.tables.units().get(0);
        switch (corruption) {
            case MISSING_TABLE -> products.tables = new CompilationUnitSymbolTables(List.of(first));
            case EXTRA_TABLE -> {
                var units = new ArrayList<>(products.tables.units());
                units.add(new CompilationUnitSymbolTables.UnitSymbols(unknownUnit(), null, first.symbolTable()));
                products.tables = new CompilationUnitSymbolTables(units);
            }
            case PARENT -> products.tables = new CompilationUnitSymbolTables(List.of(
                    new CompilationUnitSymbolTables.UnitSymbols(first.id(), unknownUnit(), first.symbolTable()),
                    products.tables.units().get(1)));
            case MISSING_SCOPES -> products.scopes.remove(unitId());
            case EXTRA_SCOPES -> products.scopes.put(unknownUnit(), products.scopes.get(unitId()));
            case MISSING_OCCURRENCES -> products.occurrences.remove(unitId());
            case EXTRA_OCCURRENCES -> products.occurrences.put(unknownUnit(), new ReferenceOccurrences(List.of()));
            case NULL_SCOPES -> products.scopes.put(unitId(), null);
            case NULL_OCCURRENCES -> products.occurrences.put(unitId(), null);
            case CONTAINER_UNIT -> products.occurrence(o -> occurrence(o, unknownUnit(), o.referenceAstNodeId(),
                    o.scopeId(), o.meta(), o.writtenText()));
        }
        rejects(products);
    }

    enum AnchorCorruption { SYMBOL_NODE, SYMBOL_NAMESPACE, SYMBOL_SCOPE, SYMBOL_NODE_KIND,
        RELATION_NODE, SCOPE_ANCHOR, DUPLICATE_AST, NON_PREORDER_AST,
        MISSING_MAPPING, EXTRA_MAPPING, WRONG_SCOPE_MAPPING, OCCURRENCE_NODE,
        OCCURRENCE_SCOPE, OCCURRENCE_PROVENANCE }

    @ParameterizedTest
    @EnumSource(AnchorCorruption.class)
    void rejectsOrphanAnchorsAndProvenance(AnchorCorruption corruption) {
        Products products = new Products(baseline);
        SymbolTable table = products.tables.units().get(0).symbolTable();
        Ast.Program program = products.model.programUnits().get(0).program();
        switch (corruption) {
            case SYMBOL_NODE -> {
                var symbols = new ArrayList<>(table.symbols());
                var s = symbols.get(0);
                symbols.set(0, new SymbolTable.Symbol(s.id(), s.kind(), s.namespace(), s.writtenName(),
                        s.canonicalName(), s.scopeId(), Integer.MAX_VALUE, s.span(), s.attributes()));
                products.table(new SymbolTable(table.scopes(), symbols, table.diagnostics(), table.entities(),
                        table.declarationRelations()));
            }
            case SYMBOL_NAMESPACE, SYMBOL_SCOPE, SYMBOL_NODE_KIND -> {
                var symbols = new ArrayList<>(table.symbols());
                var s = symbols.stream().filter(symbol -> symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM)
                        .findFirst().orElseThrow();
                symbols.set(s.id(), new SymbolTable.Symbol(s.id(), s.kind(),
                        corruption == AnchorCorruption.SYMBOL_NAMESPACE ? SymbolTable.Namespace.FILE : s.namespace(),
                        s.writtenName(), s.canonicalName(), corruption == AnchorCorruption.SYMBOL_SCOPE ? 0 : s.scopeId(),
                        corruption == AnchorCorruption.SYMBOL_NODE_KIND ? program.meta().id() : s.declarationAstNodeId(),
                        s.span(), s.attributes()));
                products.table(new SymbolTable(table.scopes(), symbols, table.diagnostics(), table.entities(), table.declarationRelations()));
            }
            case RELATION_NODE -> {
                var relations = new ArrayList<>(table.declarationRelations());
                var r = relations.get(0);
                relations.set(0, new SymbolTable.DeclarationRelation(r.id(), r.kind(), r.ownerSymbolId(),
                        Integer.MAX_VALUE, r.writtenTarget(), r.bindingStatus(), r.attributes()));
                products.table(new SymbolTable(table.scopes(), table.symbols(), table.diagnostics(), table.entities(), relations));
            }
            case SCOPE_ANCHOR -> {
                var scopes = new ArrayList<>(table.scopes());
                // An extra orphan anchor leaves every existing mapping intact.
                scopes.add(new SymbolTable.Scope(scopes.size(), 0, SymbolTable.ScopeKind.DIVISION,
                        "orphan", -1, Integer.MAX_VALUE));
                products.table(new SymbolTable(scopes, table.symbols(), table.diagnostics(), table.entities(), table.declarationRelations()));
            }
            case DUPLICATE_AST -> {
                var divisions = new ArrayList<>(program.divisions());
                divisions.add(divisions.get(0));
                products.program(new Ast.Program(program.meta(), program.name(), program.attributes(), divisions));
            }
            case NON_PREORDER_AST -> products.program(new Ast.Program(meta(Integer.MAX_VALUE), program.name(),
                    program.attributes(), program.divisions()));
            case MISSING_MAPPING -> products.scopes.put(unitId(), AstScopeIndex.build(
                    new Ast.Program(program.meta(), program.name(), List.of()), table));
            case EXTRA_MAPPING -> {
                var divisions = new ArrayList<>(program.divisions());
                divisions.add(new Ast.Division(meta(Integer.MAX_VALUE), Ast.DivisionKind.DATA, List.of()));
                products.scopes.put(unitId(), AstScopeIndex.build(new Ast.Program(program.meta(), program.name(), divisions), table));
            }
            case WRONG_SCOPE_MAPPING -> products.scopes.put(unitId(), AstScopeIndex.build(program,
                    new SymbolTable(List.of(table.rootScope()), List.of(), List.of(), List.of(), List.of())));
            case OCCURRENCE_NODE -> products.occurrence(o -> occurrence(o, o.programUnitId(), Integer.MAX_VALUE,
                    o.scopeId(), o.meta(), o.writtenText()));
            case OCCURRENCE_SCOPE -> products.occurrence(o -> occurrence(o, o.programUnitId(), o.referenceAstNodeId(),
                    0, o.meta(), o.writtenText()));
            case OCCURRENCE_PROVENANCE -> products.occurrence(o -> {
                var m = o.meta();
                var p = m.provenance();
                return occurrence(o, o.programUnitId(), o.referenceAstNodeId(), o.scopeId(),
                        new Ast.Meta(m.id(), m.span(), m.origin(), new Ast.SourceProvenance(p.expanded(),
                                p.original(), p.includeChain(), !p.exact())), o.writtenText());
            });
        }
        rejects(products);
    }

    @Test
    void relationOwnerIntegrityRemainsWithSymbolTable() {
        var table = baseline.tables().units().get(0).symbolTable();
        var r = table.declarationRelations().get(0);
        var corrupt = new SymbolTable.DeclarationRelation(0, r.kind(), Integer.MAX_VALUE,
                r.referenceAstNodeId(), r.writtenTarget(), r.bindingStatus(), r.attributes());
        assertThrows(IllegalArgumentException.class, () -> new SymbolTable(table.scopes(), table.symbols(),
                table.diagnostics(), table.entities(), List.of(corrupt)));
    }

    enum ResolutionCorruption { MISSING_ENTRY, EXTRA_ENTRY, DUPLICATE_IDENTITY, PAYLOAD,
        MISSING_RELATION_ENTRY, EXTRA_RELATION_ENTRY, DUPLICATE_RELATION_IDENTITY, RELATION_KIND, RELATION_NODE }

    @ParameterizedTest
    @EnumSource(ResolutionCorruption.class)
    void rejectsBrokenBijections(ResolutionCorruption corruption) {
        Products products = new Products(baseline);
        var entries = new ArrayList<>(products.resolution.entries());
        var relations = new ArrayList<>(products.resolution.declarationRelations().entries());
        var first = entries.get(0);
        var relation = relations.get(0);
        switch (corruption) {
            case MISSING_ENTRY -> entries.remove(entries.size() - 1);
            case EXTRA_ENTRY -> products.occurrences.put(unitId(), new ReferenceOccurrences(List.of()));
            case DUPLICATE_IDENTITY -> entries.add(first);
            case PAYLOAD -> entries.set(0, new ReferenceResolution.Entry(0,
                    occurrence(first.occurrence(), unitId(), first.occurrence().referenceAstNodeId(),
                            first.occurrence().scopeId(), first.occurrence().meta(), "different payload"),
                    first.status(), first.reason(), first.candidates(), first.diagnosticIds()));
            case MISSING_RELATION_ENTRY -> relations.remove(relations.size() - 1);
            case EXTRA_RELATION_ENTRY -> relations.add(relation(relation, Integer.MAX_VALUE, relation.kind(), relation.referenceAstNodeId()));
            case DUPLICATE_RELATION_IDENTITY -> relations.add(relation);
            case RELATION_KIND -> relations.set(0, relation(relation, relation.relationId(),
                    SymbolTable.RelationKind.OCCURS_KEY, relation.referenceAstNodeId()));
            case RELATION_NODE -> relations.set(0, relation(relation, relation.relationId(), relation.kind(), Integer.MAX_VALUE));
        }
        products.resolution(entries, relations);
        rejects(products);
    }

    @ParameterizedTest
    @EnumSource(SemanticEntityDomain.class)
    void candidateIdentitySelectsTheExactDeclarationPayload(SemanticEntityDomain domain) {
        var candidate = candidate(domain);
        List<ReferenceResolution.Candidate> corruptions = List.of(
                candidate(candidate, new SemanticEntityId(unknownUnit(), domain, candidate.entityId().localId()), candidate.kind(), candidate.declarationSymbolIds()),
                candidate(candidate, new SemanticEntityId(candidate.entityId().programUnitId(), domain, Integer.MAX_VALUE), candidate.kind(), candidate.declarationSymbolIds()),
                candidate(candidate, candidate.entityId(), candidate.kind() == ReferenceKind.PROGRAM ? ReferenceKind.DATA : ReferenceKind.PROGRAM, candidate.declarationSymbolIds()),
                candidate(candidate, candidate.entityId(), candidate.kind(), List.of(Integer.MAX_VALUE)),
                candidate(candidate, candidate.entityId(), candidate.kind(), List.of(0)));
        for (var corrupt : corruptions) {
            Products products = new Products(baseline);
            products.candidates(List.of(corrupt));
            rejects(products);
        }
        // Names and attributes are deliberately unrelated to any declaration.
        Products renamed = new Products(baseline);
        renamed.candidates(List.of(new ReferenceResolution.Candidate(candidate.entityId(), candidate.kind(),
                "no matching written name", "no matching canonical name", candidate.declarationSymbolIds(), Map.of())));
        renamed.validate();
    }

    @Test
    void rejectsWrongSymbolDomainsAndContradictoryOrDuplicateCandidatesInBothProducts() {
        var data = candidate(SemanticEntityDomain.DATA_SYMBOL);
        var file = candidate(SemanticEntityDomain.FILE_ENTITY);
        assertTrue(file.declarationSymbolIds().size() > 1, "SELECT and FD form one entity");
        List<List<ReferenceResolution.Candidate>> corruptions = List.of(
                List.of(data, data),
                List.of(data, candidate(data, data.entityId(), ReferenceKind.CONDITION, data.declarationSymbolIds())),
                List.of(candidate(data, new SemanticEntityId(unitId(), SemanticEntityDomain.INDEX_SYMBOL,
                        data.entityId().localId()), ReferenceKind.INDEX, data.declarationSymbolIds())),
                List.of(candidate(data, new SemanticEntityId(unitId(), SemanticEntityDomain.PROCEDURE_SYMBOL,
                        data.entityId().localId()), ReferenceKind.PROCEDURE, data.declarationSymbolIds())),
                List.of(candidate(file, file.entityId(), file.kind(), List.of(file.declarationSymbolIds().get(0)))),
                List.of(candidate(data, data.entityId(), data.kind(), List.of(data.entityId().localId(), data.entityId().localId()))));
        for (var candidates : corruptions) {
            Products occurrenceProducts = new Products(baseline);
            occurrenceProducts.candidates(candidates);
            rejects(occurrenceProducts);
            Products relationProducts = new Products(baseline);
            var relations = new ArrayList<>(relationProducts.resolution.declarationRelations().entries());
            var r = relations.get(0);
            relations.set(0, new DeclarationRelationResolution.Entry(0, r.programUnitId(), r.relationId(),
                    r.kind(), r.referenceAstNodeId(), ResolutionStatus.UNSUPPORTED,
                    ResolutionReason.UNSUPPORTED_GRAMMAR_FORM, candidates));
            relationProducts.resolution(relationProducts.resolution.entries(), relations);
            rejects(relationProducts);
        }
    }

    @Test
    void validButDifferentDeclarationIdsCannotMasqueradeAsTheSelectedEntity() {
        var data = candidate(SemanticEntityDomain.DATA_SYMBOL);
        var other = baseline.tables().units().get(0).symbolTable().symbols().stream()
                .filter(s -> s.kind() == SymbolTable.SymbolKind.DATA_ITEM && s.id() != data.entityId().localId())
                .findFirst().orElseThrow();
        Products products = new Products(baseline);
        products.candidates(List.of(candidate(data, data.entityId(), data.kind(), List.of(other.id()))));
        rejects(products);
        var program = candidate(SemanticEntityDomain.PROGRAM_UNIT);
        products = new Products(baseline);
        products.candidates(List.of(candidate(program, new SemanticEntityId(program.entityId().programUnitId(),
                SemanticEntityDomain.PROGRAM_UNIT, 0), program.kind(), List.of())));
        rejects(products);
    }

    @Test
    void namespacesCannotBeSubstitutedEvenWhenLocalIdsRepeat() {
        Products products = new Products(baseline);
        var child = products.model.programUnits().get(1).id();
        var parentOccurrence = products.occurrences.get(unitId()).occurrences().get(0);
        var childOccurrence = products.occurrences.get(child).occurrences().get(0);
        assertEquals(parentOccurrence.id(), childOccurrence.id());
        var first = products.resolution.entries().get(0);
        products.entry(0, new ReferenceResolution.Entry(0, childOccurrence, first.status(), first.reason(),
                first.candidates(), List.of()));
        rejects(products);

        products = new Products(baseline);
        var data = candidate(SemanticEntityDomain.DATA_SYMBOL);
        products.candidates(List.of(candidate(data, new SemanticEntityId(child, SemanticEntityDomain.DATA_SYMBOL,
                data.entityId().localId()), data.kind(), data.declarationSymbolIds())));
        rejects(products);
    }

    @Test
    void diagnosticsDoNotDependOnInputMapIterationOrder() {
        Products forward = new Products(baseline);
        Products reverse = new Products(baseline);
        var other = new ProgramUnitId("another-compilation", List.of(0), "INTEGRITY");
        forward.scopes.put(unknownUnit(), baseline.scopes().get(unitId()));
        forward.scopes.put(other, baseline.scopes().get(unitId()));
        reverse.scopes.put(other, baseline.scopes().get(unitId()));
        reverse.scopes.put(unknownUnit(), baseline.scopes().get(unitId()));
        assertEquals(rejects(forward).getMessage(), rejects(reverse).getMessage());
    }

    @Test
    void absentProductsFailThroughTheDedicatedException() {
        assertThrows(SemanticProductIntegrityException.class, () -> SemanticProductIntegrityValidator.validate(
                null, baseline.tables(), baseline.scopes(), baseline.occurrences(), baseline.resolution()));
        assertThrows(SemanticProductIntegrityException.class, () -> SemanticProductIntegrityValidator.validate(
                baseline.model(), null, baseline.scopes(), baseline.occurrences(), baseline.resolution()));
        assertThrows(SemanticProductIntegrityException.class, () -> SemanticProductIntegrityValidator.validate(
                baseline.model(), baseline.tables(), null, baseline.occurrences(), baseline.resolution()));
        assertThrows(SemanticProductIntegrityException.class, () -> SemanticProductIntegrityValidator.validate(
                baseline.model(), baseline.tables(), baseline.scopes(), null, baseline.resolution()));
        assertThrows(SemanticProductIntegrityException.class, () -> SemanticProductIntegrityValidator.validate(
                baseline.model(), baseline.tables(), baseline.scopes(), baseline.occurrences(), null));
    }

    @Test
    void compositionRootValidatesImmediatelyBeforeFirstPostResolutionConsumer() throws Exception {
        String source = Files.readString(Path.of("src/main/java/io/github/gustavo2358/cobolexplorer/ExplorerMain.java"));
        int resolver = source.indexOf(".resolve(compilationUnit, symbolTables, occurrences);");
        int validator = source.indexOf("SemanticProductIntegrityValidator.validate(", resolver);
        int classifier = source.indexOf("new CicsIntrinsicClassifier().classify(", resolver);
        int report = source.indexOf("ResolutionAnalysisReport.compose(", resolver);
        int projection = source.indexOf("publishSemanticProduct(primaryUnit.id()", resolver);
        assertTrue(resolver >= 0 && validator > resolver && classifier > validator && report > classifier && projection > report);
        // The production call is unconditional and has no recovery before classification.
        assertEquals(".resolve(compilationUnit, symbolTables, occurrences);", source.substring(resolver, validator).trim());
        assertEquals("SemanticProductIntegrityValidator.validate(compilationUnit, symbolTables,\n"
                        + "                scopeIndexesByUnit, occurrences, resolution);",
                source.substring(validator, source.indexOf("progress.phase", validator)).trim());
    }

    @Test
    void scalesAcrossUnitsNodesDeclarationsAndCandidatesWithoutTextualJoins() {
        // Product-level scale probe: equal local IDs and names in every unit; no
        // parser/resolver work or wall-clock threshold obscures validator behavior.
        for (int size : List.of(32, 128, 512)) {
            var units = new ArrayList<CompilationUnitModel.ProgramUnit>();
            var tables = new ArrayList<CompilationUnitSymbolTables.UnitSymbols>();
            var scopes = new HashMap<ProgramUnitId, AstScopeIndex>();
            var occurrences = new HashMap<ProgramUnitId, ReferenceOccurrences>();
            var entries = new ArrayList<ReferenceResolution.Entry>();
            for (int u = 0; u < size; u++) {
                ProgramUnitId id = new ProgramUnitId("scale", List.of(u), "SAME");
                var declarations = new ArrayList<Ast.Node>();
                for (int d = 0; d < 8; d++) declarations.add(new Ast.DataEntry(meta(3 + d), "01",
                        Ast.DataLevelKind.GROUP_OR_ELEMENTARY, "SAME", false, "", List.of(), List.of()));
                var program = new Ast.Program(meta(0), "SAME", List.of(new Ast.Division(meta(1),
                        Ast.DivisionKind.DATA, List.of(new Ast.Section(meta(2), "WORKING-STORAGE", declarations))),
                        new Ast.Division(meta(11), Ast.DivisionKind.PROCEDURE,
                                List.of(new Ast.NamedReference(meta(12), "dataName", "SAME")))));
                var table = new SymbolTableBuilder().build(program);
                var index = AstScopeIndex.build(program, table);
                var occurrence = new ReferenceOccurrences.Occurrence(0, id, 12, index.scopeIdForAstNodeId(12),
                        ReferenceKind.DATA, Set.of(ReferenceKind.DATA), ReferenceRole.VALUE_READ,
                        "diagnostic", "no lookup match", meta(12), ReferenceOccurrences.Preservation.STRUCTURED);
                var candidates = table.symbols().stream().filter(s -> s.kind() == SymbolTable.SymbolKind.DATA_ITEM)
                        .map(s -> new ReferenceResolution.Candidate(new SemanticEntityId(id, SemanticEntityDomain.DATA_SYMBOL, s.id()),
                                ReferenceKind.DATA, "diagnostic only", "diagnostic only", List.of(s.id()), Map.of())).toList();
                units.add(new CompilationUnitModel.ProgramUnit(id, null, program));
                tables.add(new CompilationUnitSymbolTables.UnitSymbols(id, null, table));
                scopes.put(id, index);
                occurrences.put(id, new ReferenceOccurrences(List.of(occurrence)));
                entries.add(new ReferenceResolution.Entry(u, occurrence, ResolutionStatus.AMBIGUOUS,
                        ResolutionReason.MULTIPLE_VALID_CANDIDATES, candidates, List.of()));
            }
            var model = new CompilationUnitModel("scale", units);
            var symbolTables = new CompilationUnitSymbolTables(tables);
            var resolution = new ReferenceResolution(CobolResolutionPolicy.initial(), entries, List.of(),
                    new ReferenceResolution.Metrics(0, 0, 0, 0), new DeclarationRelationResolution(List.of()));
            SemanticProductIntegrityValidator.validate(model, symbolTables, scopes, occurrences, resolution);
            // Corruption at the last unit cannot disappear as the inventories grow.
            occurrences.put(units.get(size - 1).id(), new ReferenceOccurrences(List.of()));
            var failure = assertThrows(SemanticProductIntegrityException.class, () ->
                    SemanticProductIntegrityValidator.validate(model, symbolTables, scopes, occurrences, resolution));
            assertTrue(failure.getMessage().contains(units.get(size - 1).id().toString()));
        }
    }

    private static SemanticProductIntegrityException rejects(Products products) {
        var first = assertThrows(SemanticProductIntegrityException.class, products::validate);
        assertTrue(first.getMessage().startsWith("INTERNAL PRODUCT INTEGRITY FAILURE product="));
        assertTrue(first.getMessage().contains(" unit="));
        assertTrue(first.getMessage().contains(" domain="));
        assertEquals(first.getMessage(), assertThrows(SemanticProductIntegrityException.class, products::validate).getMessage());
        return first;
    }

    private static ProgramUnitId unitId() { return baseline.model().programUnits().get(0).id(); }
    private static ProgramUnitId unknownUnit() { return new ProgramUnitId("different-compilation", List.of(0), "INTEGRITY"); }
    private static Ast.Meta meta(int id) { return new Ast.Meta(id, new Ast.SourceSpan(1, 0, 1, 1, 0, 0), new Ast.ParseTreeOrigin(0, "typed", 1)); }

    private static ReferenceResolution.Candidate candidate(SemanticEntityDomain domain) {
        return baseline.resolution().entries().stream().flatMap(e -> e.candidates().stream())
                .filter(c -> c.entityId().domain() == domain).findFirst().orElseThrow();
    }

    private static ReferenceResolution.Candidate candidate(ReferenceResolution.Candidate c, SemanticEntityId id,
                                                           ReferenceKind kind, List<Integer> declarations) {
        return new ReferenceResolution.Candidate(id, kind, c.writtenName(), c.canonicalName(), declarations, c.attributes());
    }

    private static ReferenceOccurrences.Occurrence occurrence(ReferenceOccurrences.Occurrence o, ProgramUnitId unit,
                                                              int node, int scope, Ast.Meta meta, String text) {
        return new ReferenceOccurrences.Occurrence(o.id(), unit, node, scope, o.kind(), o.admissibleKinds(), o.role(),
                o.grammarRule(), text, meta, o.preservation());
    }

    private static DeclarationRelationResolution.Entry relation(DeclarationRelationResolution.Entry r, int id,
                                                                 SymbolTable.RelationKind kind, int node) {
        return new DeclarationRelationResolution.Entry(r.id(), r.programUnitId(), id, kind, node,
                r.status(), r.reason(), r.candidates());
    }

    private static final class Products {
        CompilationUnitModel model;
        CompilationUnitSymbolTables tables;
        final Map<ProgramUnitId, AstScopeIndex> scopes;
        final Map<ProgramUnitId, ReferenceOccurrences> occurrences;
        ReferenceResolution resolution;

        Products(AstBoundaryTestSupport.Analysis analysis) {
            model = analysis.model(); tables = analysis.tables(); resolution = analysis.resolution();
            scopes = new LinkedHashMap<>(analysis.scopes()); occurrences = new LinkedHashMap<>(analysis.occurrences());
        }

        void validate() { SemanticProductIntegrityValidator.validate(model, tables, scopes, occurrences, resolution); }

        void table(SymbolTable table) {
            var units = new ArrayList<>(tables.units());
            var first = units.get(0);
            units.set(0, new CompilationUnitSymbolTables.UnitSymbols(first.id(), first.parentId(), table));
            tables = new CompilationUnitSymbolTables(units);
        }

        void scope(SymbolTable.Scope replacement) {
            var original = tables.units().get(0).symbolTable();
            var changed = new ArrayList<>(original.scopes());
            changed.set(replacement.id(), replacement);
            table(new SymbolTable(changed, original.symbols(), original.diagnostics(), original.entities(),
                    original.declarationRelations()));
        }

        void resolveAgain() {
            resolution = new CobolReferenceResolver(resolution.policy()).resolve(model, tables, occurrences);
        }

        void program(Ast.Program program) {
            var units = new ArrayList<>(model.programUnits());
            var first = units.get(0);
            units.set(0, new CompilationUnitModel.ProgramUnit(first.id(), first.parentId(), program));
            model = new CompilationUnitModel(model.compilationUnitId(), units);
        }

        void occurrence(UnaryOperator<ReferenceOccurrences.Occurrence> change) {
            var list = new ArrayList<>(occurrences.get(unitId()).occurrences());
            list.set(0, change.apply(list.get(0)));
            occurrences.put(unitId(), new ReferenceOccurrences(list));
        }

        void candidates(List<ReferenceResolution.Candidate> candidates) {
            var e = resolution.entries().stream().filter(entry -> entry.candidates().stream().anyMatch(c ->
                    c.entityId().domain() == candidates.get(0).entityId().domain())).findFirst().orElseThrow();
            entry(e.id(), new ReferenceResolution.Entry(e.id(), e.occurrence(), ResolutionStatus.UNSUPPORTED,
                    ResolutionReason.UNSUPPORTED_DIALECT_OPTION, candidates, List.of(), e.callSemantics()));
        }

        void entry(int index, ReferenceResolution.Entry entry) {
            var entries = new ArrayList<>(resolution.entries());
            entries.set(index, entry);
            resolution(entries, resolution.declarationRelations().entries());
        }

        void resolution(List<ReferenceResolution.Entry> entries, List<DeclarationRelationResolution.Entry> relations) {
            var numbered = new ArrayList<ReferenceResolution.Entry>();
            for (var e : entries) numbered.add(new ReferenceResolution.Entry(numbered.size(), e.occurrence(),
                    e.status(), e.reason(), e.candidates(), e.diagnosticIds(), e.callSemantics()));
            var numberedRelations = new ArrayList<DeclarationRelationResolution.Entry>();
            for (var r : relations) numberedRelations.add(new DeclarationRelationResolution.Entry(numberedRelations.size(),
                    r.programUnitId(), r.relationId(), r.kind(), r.referenceAstNodeId(), r.status(), r.reason(), r.candidates()));
            resolution = new ReferenceResolution(resolution.policy(), numbered, resolution.diagnostics(), resolution.metrics(),
                    new DeclarationRelationResolution(numberedRelations));
        }
    }
}
