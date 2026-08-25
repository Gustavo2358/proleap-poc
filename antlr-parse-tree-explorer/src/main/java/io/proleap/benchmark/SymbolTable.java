package io.proleap.benchmark;

import java.util.*;

/**
 * Immutable declaration index built from the semantic AST.
 *
 * <p>This model deliberately contains no resolved references. It answers only
 * "what is declared, in which namespace and scope?" so name resolution can be
 * implemented as a separate analysis step.</p>
 */
public final class SymbolTable {
    public enum Namespace { PROGRAM, DATA, PROCEDURE, FILE }

    public enum SymbolKind {
        PROGRAM,
        FILE_CONTROL,
        FILE_DESCRIPTION,
        DATA_ITEM,
        CONDITION_NAME,
        RENAMES,
        INDEX_NAME,
        PROCEDURE_SECTION,
        PARAGRAPH
    }

    public enum ScopeKind {
        ROOT,
        PROGRAM,
        DIVISION,
        SECTION,
        FILE_DESCRIPTION,
        DATA_ITEM,
        PARAGRAPH
    }

    public enum EntityKind { FILE }

    public enum RelationKind {
        REDEFINES,
        RENAMES_FROM,
        RENAMES_THROUGH,
        OCCURS_DEPENDING_ON,
        OCCURS_KEY,
        OCCURS_INDEX
    }

    /**
     * Structural/declaration context for ownership, qualification and ancestry.
     *
     * <p>The {@code parentId} chain models structural ownership; it is not the
     * complete COBOL name-visibility model. COBOL visibility and binding belong
     * to the dedicated reference resolvers.</p>
     */
    public record Scope(int id, int parentId, ScopeKind kind, String name,
                        int ownerSymbolId, int astNodeId) {}

    public record Symbol(int id, SymbolKind kind, Namespace namespace,
                         String writtenName, String canonicalName, int scopeId,
                         int declarationAstNodeId, Ast.SourceSpan span,
                         Map<String, String> attributes) {
        public Symbol { attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes)); }
    }

    public record Diagnostic(String code, String message, int scopeId,
                             List<Integer> symbolIds) {
        public Diagnostic { symbolIds = List.copyOf(symbolIds); }
    }

    /** One semantic declaration entity may be represented by multiple declarations (for example SELECT + FD). */
    public record Entity(int id, EntityKind kind, String writtenName, String canonicalName,
                         List<Integer> declarationSymbolIds, Map<String, String> attributes) {
        public Entity {
            declarationSymbolIds = List.copyOf(declarationSymbolIds);
            attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        }
    }

    /** Nominal declaration relation. Its target is intentionally not bound to a symbol here. */
    public record DeclarationRelation(int id, RelationKind kind, int ownerSymbolId,
                                      int referenceAstNodeId, String writtenTarget,
                                      String bindingStatus, Map<String, String> attributes) {
        public DeclarationRelation {
            attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
            if (!"NOT_PERFORMED".equals(bindingStatus))
                throw new IllegalArgumentException("declaration relation cannot contain binding results");
        }
    }

    private record LookupKey(int scopeId, Namespace namespace, String canonicalName) {}
    private record GlobalKey(Namespace namespace, String canonicalName) {}

    private final List<Scope> scopes;
    private final List<Symbol> symbols;
    private final List<Diagnostic> diagnostics;
    private final List<Entity> entities;
    private final List<DeclarationRelation> declarationRelations;
    private final Map<LookupKey, List<Symbol>> localIndex;
    private final Map<GlobalKey, List<Symbol>> globalIndex;

    SymbolTable(List<Scope> scopes, List<Symbol> symbols, List<Diagnostic> diagnostics,
                List<Entity> entities, List<DeclarationRelation> declarationRelations) {
        this.scopes = List.copyOf(scopes);
        this.symbols = List.copyOf(symbols);
        this.diagnostics = List.copyOf(diagnostics);
        this.entities = List.copyOf(entities);
        this.declarationRelations = List.copyOf(declarationRelations);
        validateIds();
        Map<LookupKey, List<Symbol>> mutable = new HashMap<>();
        Map<GlobalKey, List<Symbol>> global = new HashMap<>();
        for (Symbol symbol : this.symbols) {
            LookupKey key = new LookupKey(symbol.scopeId(), symbol.namespace(), symbol.canonicalName());
            mutable.computeIfAbsent(key, ignored -> new ArrayList<>()).add(symbol);
            GlobalKey globalKey = new GlobalKey(symbol.namespace(), symbol.canonicalName());
            global.computeIfAbsent(globalKey, ignored -> new ArrayList<>()).add(symbol);
        }
        Map<LookupKey, List<Symbol>> immutable = new HashMap<>();
        mutable.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        this.localIndex = Map.copyOf(immutable);
        Map<GlobalKey, List<Symbol>> immutableGlobal = new HashMap<>();
        global.forEach((key, value) -> immutableGlobal.put(key, List.copyOf(value)));
        this.globalIndex = Map.copyOf(immutableGlobal);
    }

    public List<Scope> scopes() { return scopes; }
    public List<Symbol> symbols() { return symbols; }
    public List<Diagnostic> diagnostics() { return diagnostics; }
    public List<Entity> entities() { return entities; }
    public List<DeclarationRelation> declarationRelations() { return declarationRelations; }
    public Scope rootScope() { return scopes.get(0); }

    public List<Symbol> symbolsInScope(int scopeId) {
        return symbols.stream().filter(symbol -> symbol.scopeId() == scopeId).toList();
    }

    /** Case-insensitive COBOL lookup in one scope; ambiguity remains visible as a list. */
    public List<Symbol> lookupLocal(int scopeId, Namespace namespace, String writtenName) {
        return localIndex.getOrDefault(new LookupKey(scopeId, namespace, canonical(writtenName)), List.of());
    }

    /** All declarations with this name; a future resolver can then apply COBOL qualification rules. */
    public List<Symbol> lookupAll(Namespace namespace, String writtenName) {
        return globalIndex.getOrDefault(new GlobalKey(namespace, canonical(writtenName)), List.of());
    }

    /**
     * Searches only the structural scope ancestor chain.
     *
     * <p>This operation queries the starting scope, ascends sequentially through
     * {@code parentId} while no declaration is found, and returns the first
     * non-empty result. It does not perform COBOL reference binding and must not
     * be used as a substitute for the dedicated COBOL reference resolvers.</p>
     */
    public List<Symbol> lookupInAncestorScopes(int startingScopeId, Namespace namespace, String writtenName) {
        int scopeId = startingScopeId;
        while (scopeId >= 0) {
            List<Symbol> found = lookupLocal(scopeId, namespace, writtenName);
            if (!found.isEmpty()) return found;
            scopeId = scopes.get(scopeId).parentId();
        }
        return List.of();
    }

    public static String canonical(String writtenName) {
        return writtenName == null ? "" : writtenName.trim().toUpperCase(Locale.ROOT);
    }

    private void validateIds() {
        if (scopes.isEmpty() || scopes.get(0).kind() != ScopeKind.ROOT)
            throw new IllegalArgumentException("symbol table must start with the root scope");
        for (int i = 0; i < scopes.size(); i++) {
            Scope scope = scopes.get(i);
            if (scope.id() != i) throw new IllegalArgumentException("scope ids must be contiguous");
            if (scope.parentId() >= i) throw new IllegalArgumentException("scope parent must precede child");
        }
        for (int i = 0; i < symbols.size(); i++) {
            Symbol symbol = symbols.get(i);
            if (symbol.id() != i) throw new IllegalArgumentException("symbol ids must be contiguous");
            if (symbol.scopeId() < 0 || symbol.scopeId() >= scopes.size())
                throw new IllegalArgumentException("unknown declaring scope " + symbol.scopeId());
        }
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if (entity.id() != i) throw new IllegalArgumentException("entity ids must be contiguous");
            if (entity.declarationSymbolIds().isEmpty())
                throw new IllegalArgumentException("entity must have at least one declaration");
            if (entity.declarationSymbolIds().stream().anyMatch(id -> id < 0 || id >= symbols.size()))
                throw new IllegalArgumentException("entity contains unknown declaration symbol");
        }
        for (int i = 0; i < declarationRelations.size(); i++) {
            DeclarationRelation relation = declarationRelations.get(i);
            if (relation.id() != i) throw new IllegalArgumentException("relation ids must be contiguous");
            if (relation.ownerSymbolId() < 0 || relation.ownerSymbolId() >= symbols.size())
                throw new IllegalArgumentException("relation contains unknown owner symbol");
            if (relation.referenceAstNodeId() < 0)
                throw new IllegalArgumentException("relation must preserve its nominal reference node");
        }
    }
}
