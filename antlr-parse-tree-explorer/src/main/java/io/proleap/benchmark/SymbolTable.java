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

    private record LookupKey(int scopeId, Namespace namespace, String canonicalName) {}
    private record GlobalKey(Namespace namespace, String canonicalName) {}

    private final List<Scope> scopes;
    private final List<Symbol> symbols;
    private final List<Diagnostic> diagnostics;
    private final Map<LookupKey, List<Symbol>> localIndex;
    private final Map<GlobalKey, List<Symbol>> globalIndex;

    SymbolTable(List<Scope> scopes, List<Symbol> symbols, List<Diagnostic> diagnostics) {
        this.scopes = List.copyOf(scopes);
        this.symbols = List.copyOf(symbols);
        this.diagnostics = List.copyOf(diagnostics);
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

    /** Lexical lookup only. This is infrastructure, not reference binding. */
    public List<Symbol> lookupVisible(int startingScopeId, Namespace namespace, String writtenName) {
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
    }
}
