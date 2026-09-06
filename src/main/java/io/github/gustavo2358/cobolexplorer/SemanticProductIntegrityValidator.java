package io.github.gustavo2358.cobolexplorer;

import java.util.*;

import static io.github.gustavo2358.cobolexplorer.ResolutionContracts.*;

/**
 * Reconciles immutable frontend products before their first post-resolution consumer.
 * Joins use unit/domain/local identity only; semantic incompleteness is not corruption.
 *
 * <p>Each inventory is traversed once, with direct indexed joins. Time and auxiliary
 * space are linear in units, nodes, scopes, symbols, relations, occurrences, entries,
 * candidates and candidate declaration IDs. No binding or product repair occurs here.</p>
 */
final class SemanticProductIntegrityValidator {
    private SemanticProductIntegrityValidator() { }

    private record UnitIndex(int ordinal, SymbolTable table, ReferenceOccurrences occurrences,
                             boolean[] resolvedOccurrences, boolean[] resolvedRelations) { }
    private record Visit(Ast.Node node, int inheritedScope) { }

    static void validate(CompilationUnitModel model, CompilationUnitSymbolTables symbolTables,
                         Map<ProgramUnitId, AstScopeIndex> scopeIndexesByUnit,
                         Map<ProgramUnitId, ReferenceOccurrences> occurrencesByUnit,
                         ReferenceResolution resolution) {
        require(model != null, "model", null, "PROGRAM_UNIT", "inventory", "missing product");
        require(symbolTables != null, "tables", null, "PROGRAM_UNIT", "inventory", "missing product");
        require(resolution != null, "resolution", null, "OCCURRENCE", "inventory", "missing product");
        Set<ProgramUnitId> unitIds = new HashSet<>();
        for (var unit : model.programUnits()) unitIds.add(unit.id());
        require(!unitIds.isEmpty(), "model", null, "PROGRAM_UNIT", "inventory", "empty model");
        Set<ProgramUnitId> tableIds = new HashSet<>();
        for (var unit : symbolTables.units()) tableIds.add(unit.id());
        checkUnits(model, unitIds, tableIds, "tables");
        checkUnits(model, unitIds, scopeIndexesByUnit == null ? null : scopeIndexesByUnit.keySet(), "scopes");
        checkUnits(model, unitIds, occurrencesByUnit == null ? null : occurrencesByUnit.keySet(), "occurrences");

        Map<ProgramUnitId, UnitIndex> indexes = new LinkedHashMap<>();
        for (var unit : model.programUnits()) {
            ProgramUnitId id = unit.id();
            var unitSymbols = symbolTables.forProgramUnit(id).orElseThrow();
            require(Objects.equals(unit.parentId(), unitSymbols.parentId()), "tables", id,
                    "PROGRAM_UNIT", "parentId=" + unitSymbols.parentId(), "parent mismatch");
            SymbolTable table = unitSymbols.symbolTable();
            AstScopeIndex scopes = scopeIndexesByUnit.get(id);
            ReferenceOccurrences occurrences = occurrencesByUnit.get(id);
            require(scopes != null, "scopes", id, "SCOPE", "inventory", "missing product");
            require(occurrences != null, "occurrences", id, "OCCURRENCE", "inventory", "missing product");
            Map<Integer, Ast.Node> nodes = indexAst(unit, table, scopes);
            for (var symbol : table.symbols()) {
                Ast.Node declaration = nodes.get(symbol.declarationAstNodeId());
                require(declaration != null, "symbols", id, "SYMBOL", "symbolId=" + symbol.id(),
                        "missing declaration astNodeId=" + symbol.declarationAstNodeId());
                require(declarationMatches(symbol, declaration), "symbols", id, "SYMBOL",
                        "symbolId=" + symbol.id(), "declaration kind/namespace mismatch");
                var owningScope = table.scopes().get(scopes.scopeId(declaration));
                int declaringScope = owningScope.ownerSymbolId() == symbol.id()
                        ? owningScope.parentId() : owningScope.id();
                require(symbol.scopeId() == declaringScope, "symbols", id, "SYMBOL",
                        "symbolId=" + symbol.id(), "declaration scope mismatch");
            }
            for (var relation : table.declarationRelations())
                require(nodes.containsKey(relation.referenceAstNodeId()), "relations", id,
                        "DECLARATION_RELATION", "relationId=" + relation.id(),
                        "missing reference astNodeId=" + relation.referenceAstNodeId());
            for (var occurrence : occurrences.occurrences()) {
                String key = "occurrenceId=" + occurrence.id();
                require(id.equals(occurrence.programUnitId()), "occurrences", id, "OCCURRENCE", key,
                        "container unit mismatch");
                Ast.Node node = nodes.get(occurrence.referenceAstNodeId());
                require(node != null, "occurrences", id, "OCCURRENCE", key,
                        "missing reference astNodeId=" + occurrence.referenceAstNodeId());
                require(occurrence.scopeId() == scopes.scopeId(node), "occurrences", id,
                        "OCCURRENCE", key, "scope mismatch scopeId=" + occurrence.scopeId());
                require(node.meta().equals(occurrence.meta()), "occurrences", id, "OCCURRENCE", key,
                        "reference metadata/provenance mismatch");
            }
            indexes.put(id, new UnitIndex(indexes.size(), table, occurrences,
                    new boolean[occurrences.occurrences().size()],
                    new boolean[table.declarationRelations().size()]));
        }

        for (var entry : resolution.entries()) {
            var occurrence = entry.occurrence();
            ProgramUnitId id = occurrence.programUnitId();
            String key = "occurrenceId=" + occurrence.id();
            UnitIndex unit = indexes.get(id);
            require(unit != null, "resolution", id, "OCCURRENCE", key, "unknown unit");
            require(inRange(occurrence.id(), unit.resolvedOccurrences().length), "resolution", id,
                    "OCCURRENCE", key, "missing collected occurrence");
            require(!unit.resolvedOccurrences()[occurrence.id()], "resolution", id, "OCCURRENCE", key,
                    "duplicate occurrence identity");
            require(unit.occurrences().occurrences().get(occurrence.id()).equals(occurrence),
                    "resolution", id, "OCCURRENCE", key, "collected occurrence payload mismatch");
            unit.resolvedOccurrences()[occurrence.id()] = true;
            validateCandidates(entry.candidates(), indexes, "resolution " + key);
        }
        for (var entry : resolution.declarationRelations().entries()) {
            ProgramUnitId id = entry.programUnitId();
            String key = "relationId=" + entry.relationId();
            UnitIndex unit = indexes.get(id);
            require(unit != null, "relation-resolution", id, "DECLARATION_RELATION", key, "unknown unit");
            require(inRange(entry.relationId(), unit.resolvedRelations().length), "relation-resolution",
                    id, "DECLARATION_RELATION", key, "missing declaration relation");
            require(!unit.resolvedRelations()[entry.relationId()], "relation-resolution", id,
                    "DECLARATION_RELATION", key, "duplicate relation identity");
            var relation = unit.table().declarationRelations().get(entry.relationId());
            require(relation.kind() == entry.kind() && relation.referenceAstNodeId() == entry.referenceAstNodeId(),
                    "relation-resolution", id, "DECLARATION_RELATION", key, "relation payload mismatch");
            unit.resolvedRelations()[entry.relationId()] = true;
            validateCandidates(entry.candidates(), indexes, "relation-resolution " + key);
        }
        for (var unit : indexes.entrySet()) {
            for (var occurrence : unit.getValue().occurrences().occurrences())
                require(unit.getValue().resolvedOccurrences()[occurrence.id()], "resolution", unit.getKey(),
                        "OCCURRENCE", "occurrenceId=" + occurrence.id(), "missing resolution entry");
            for (var relation : unit.getValue().table().declarationRelations())
                require(unit.getValue().resolvedRelations()[relation.id()], "relation-resolution", unit.getKey(),
                        "DECLARATION_RELATION", "relationId=" + relation.id(), "missing resolution entry");
        }
    }

    private static void checkUnits(CompilationUnitModel model, Set<ProgramUnitId> expected,
                                   Set<ProgramUnitId> actual, String product) {
        require(actual != null, product, null, "PROGRAM_UNIT", "inventory", "missing product");
        for (var unit : model.programUnits())
            require(actual.contains(unit.id()), product, unit.id(), "PROGRAM_UNIT", "inventory", "missing unit");
        // Select a stable extra identity without sorting or depending on map iteration order.
        ProgramUnitId extra = null;
        for (var id : actual) {
            require(id != null, product, null, "PROGRAM_UNIT", "inventory", "null unit key");
            if (!expected.contains(id) && (extra == null || id.toString().compareTo(extra.toString()) < 0)) extra = id;
        }
        require(extra == null, product, extra, "PROGRAM_UNIT", "inventory", "unexpected unit");
    }

    private static Map<Integer, Ast.Node> indexAst(CompilationUnitModel.ProgramUnit unit,
                                                  SymbolTable table, AstScopeIndex scopes) {
        ProgramUnitId id = unit.id();
        Map<Integer, SymbolTable.Scope> anchors = new HashMap<>();
        for (var scope : table.scopes()) {
            if (scope.kind() == SymbolTable.ScopeKind.ROOT) {
                require(scope.astNodeId() == -1, "scopes", id, "SCOPE", "scopeId=" + scope.id(),
                        "root must not have an AST anchor");
            } else {
                require(scope.astNodeId() >= 0 && anchors.putIfAbsent(scope.astNodeId(), scope) == null,
                        "scopes", id, "SCOPE", "scopeId=" + scope.id(), "missing or duplicate AST anchor");
            }
        }
        Map<Integer, Ast.Node> nodes = new HashMap<>();
        Deque<Visit> pending = new ArrayDeque<>();
        pending.push(new Visit(unit.program(), table.rootScope().id()));
        while (!pending.isEmpty()) {
            Visit visit = pending.pop();
            Ast.Node node = visit.node();
            require(node != null && node.meta() != null, "AST", id, "AST_NODE", "inventory", "missing node/meta");
            int nodeId = node.meta().id();
            require(nodes.putIfAbsent(nodeId, node) == null, "AST", id, "AST_NODE",
                    "astNodeId=" + nodeId, "duplicate reachable node identity");
            require(nodeId == nodes.size() - 1, "AST", id, "AST_NODE", "astNodeId=" + nodeId,
                    "identity is not canonical preorder");
            SymbolTable.Scope anchor = anchors.get(nodeId);
            int expectedScope = anchor == null ? visit.inheritedScope() : anchor.id();
            if (anchor != null)
                require(anchor.parentId() == visit.inheritedScope(), "scopes", id, "SCOPE",
                        "scopeId=" + anchor.id(), "AST containment/parent mismatch");
            int actualScope;
            try {
                actualScope = scopes.scopeIdForAstNodeId(nodeId);
            } catch (IllegalArgumentException missing) {
                throw failure("scopes", id, "AST_NODE", "astNodeId=" + nodeId, "missing scope mapping");
            }
            require(inRange(actualScope, table.scopes().size()) && actualScope == expectedScope,
                    "scopes", id, "AST_NODE", "astNodeId=" + nodeId, "scope mapping mismatch scopeId=" + actualScope);
            List<? extends Ast.Node> children = Ast.children(node);
            for (int i = children.size() - 1; i >= 0; i--) pending.push(new Visit(children.get(i), expectedScope));
        }
        for (var scope : table.scopes())
            if (scope.kind() != SymbolTable.ScopeKind.ROOT)
                require(nodes.containsKey(scope.astNodeId()), "scopes", id, "SCOPE", "scopeId=" + scope.id(),
                        "missing anchor astNodeId=" + scope.astNodeId());
        require(scopes.mappedNodeCount() == nodes.size(), "scopes", id, "AST_NODE", "inventory",
                "extra scope mappings");
        return nodes;
    }

    /** Checks typed declaration facts, without comparing names or parsing DATA levels. */
    private static boolean declarationMatches(SymbolTable.Symbol symbol, Ast.Node node) {
        return switch (symbol.kind()) {
            case PROGRAM -> symbol.namespace() == SymbolTable.Namespace.PROGRAM && node instanceof Ast.Program;
            case FILE_CONTROL -> symbol.namespace() == SymbolTable.Namespace.FILE && node instanceof Ast.FileBinding;
            case FILE_DESCRIPTION -> symbol.namespace() == SymbolTable.Namespace.FILE && node instanceof Ast.FileDescription;
            case PROCEDURE_SECTION -> symbol.namespace() == SymbolTable.Namespace.PROCEDURE && node instanceof Ast.Section;
            case PARAGRAPH -> symbol.namespace() == SymbolTable.Namespace.PROCEDURE && node instanceof Ast.Paragraph;
            case INDEX_NAME -> symbol.namespace() == SymbolTable.Namespace.DATA && node instanceof Ast.IndexReference;
            case DATA_ITEM, CONDITION_NAME, RENAMES -> symbol.namespace() == SymbolTable.Namespace.DATA
                    && node instanceof Ast.DataEntry entry && !entry.filler()
                    && switch (symbol.kind()) {
                        case CONDITION_NAME -> entry.levelKind() == Ast.DataLevelKind.CONDITION_88;
                        case RENAMES -> entry.levelKind() == Ast.DataLevelKind.RENAMES_66;
                        default -> entry.levelKind() == Ast.DataLevelKind.GROUP_OR_ELEMENTARY
                                || entry.levelKind() == Ast.DataLevelKind.STANDALONE_77;
                    };
        };
    }

    private static void validateCandidates(List<ReferenceResolution.Candidate> candidates,
                                           Map<ProgramUnitId, UnitIndex> indexes, String product) {
        Set<SemanticEntityId> seen = new HashSet<>();
        for (var candidate : candidates) {
            SemanticEntityId entityId = candidate.entityId();
            ProgramUnitId id = entityId.programUnitId();
            String domain = entityId.domain().name();
            String key = "localId=" + entityId.localId();
            require(seen.add(entityId), product, id, domain, key, "duplicate candidate identity");
            UnitIndex unit = indexes.get(id);
            require(unit != null, product, id, domain, key, "unknown candidate unit");
            List<Integer> declarations;
            ReferenceKind kind;
            switch (entityId.domain()) {
                case PROGRAM_UNIT -> {
                    require(entityId.localId() == unit.ordinal(), product, id, domain, key, "program ordinal mismatch");
                    kind = ReferenceKind.PROGRAM;
                    declarations = List.of();
                }
                case FILE_ENTITY -> {
                    require(inRange(entityId.localId(), unit.table().entities().size()), product, id, domain, key,
                            "missing declaration entity");
                    var entity = unit.table().entities().get(entityId.localId());
                    require(entity.kind() == SymbolTable.EntityKind.FILE, product, id, domain, key, "entity kind mismatch");
                    kind = ReferenceKind.FILE;
                    declarations = entity.declarationSymbolIds();
                }
                default -> {
                    require(inRange(entityId.localId(), unit.table().symbols().size()), product, id, domain, key,
                            "missing declaration symbol");
                    var symbol = unit.table().symbols().get(entityId.localId());
                    kind = switch (entityId.domain()) {
                        case DATA_SYMBOL -> switch (symbol.kind()) {
                            case DATA_ITEM, RENAMES -> ReferenceKind.DATA;
                            case CONDITION_NAME -> ReferenceKind.CONDITION;
                            default -> null;
                        };
                        case INDEX_SYMBOL -> symbol.kind() == SymbolTable.SymbolKind.INDEX_NAME ? ReferenceKind.INDEX : null;
                        case PROCEDURE_SYMBOL -> symbol.kind() == SymbolTable.SymbolKind.PROCEDURE_SECTION
                                || symbol.kind() == SymbolTable.SymbolKind.PARAGRAPH ? ReferenceKind.PROCEDURE : null;
                        default -> throw new AssertionError(entityId.domain());
                    };
                    require(kind != null, product, id, domain, key, "symbol domain mismatch");
                    declarations = List.of(symbol.id());
                }
            }
            require(candidate.kind() == kind, product, id, domain, key, "candidate kind mismatch");
            require(declarations.equals(candidate.declarationSymbolIds()), product, id, domain, key,
                    "candidate declarationSymbolIds mismatch");
        }
    }

    private static boolean inRange(int id, int size) { return id >= 0 && id < size; }

    private static void require(boolean valid, String product, ProgramUnitId unit, String domain,
                                String identity, String detail) {
        if (!valid) throw failure(product, unit, domain, identity, detail);
    }

    private static SemanticProductIntegrityException failure(String product, ProgramUnitId unit,
                                                              String domain, String identity, String detail) {
        return new SemanticProductIntegrityException("INTERNAL PRODUCT INTEGRITY FAILURE product=" + product
                + " unit=" + unit + " domain=" + domain + " " + identity + " detail=" + detail);
    }
}

/** An impossible combination of products, never a recoverable semantic gap. */
final class SemanticProductIntegrityException extends IllegalStateException {
    SemanticProductIntegrityException(String message) { super(message); }
}
