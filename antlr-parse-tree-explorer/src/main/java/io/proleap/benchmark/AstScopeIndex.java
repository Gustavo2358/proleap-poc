package io.proleap.benchmark;

import java.util.*;

/** Deterministic O(1) mapping from every AST node to its innermost owning scope. */
public final class AstScopeIndex {
    private final Map<Integer, Integer> scopeByAstNodeId;

    private AstScopeIndex(Map<Integer, Integer> scopeByAstNodeId) {
        this.scopeByAstNodeId = Collections.unmodifiableMap(new LinkedHashMap<>(scopeByAstNodeId));
    }

    public static AstScopeIndex build(Ast.Program program, SymbolTable symbolTable) {
        Map<Integer, Integer> ownerScopes = new HashMap<>();
        for (SymbolTable.Scope scope : symbolTable.scopes()) {
            if (scope.astNodeId() >= 0) ownerScopes.put(scope.astNodeId(), scope.id());
        }
        LinkedHashMap<Integer, Integer> result = new LinkedHashMap<>();
        index(program, symbolTable.rootScope().id(), ownerScopes, result);
        return new AstScopeIndex(result);
    }

    public int scopeId(Ast.Node node) { return scopeIdForAstNodeId(node.meta().id()); }

    public int scopeIdForAstNodeId(int astNodeId) {
        Integer result = scopeByAstNodeId.get(astNodeId);
        if (result == null) throw new IllegalArgumentException("AST node is not indexed: " + astNodeId);
        return result;
    }

    public int mappedNodeCount() { return scopeByAstNodeId.size(); }

    private static void index(Ast.Node node, int inheritedScope, Map<Integer, Integer> ownerScopes,
                              Map<Integer, Integer> output) {
        int scope = ownerScopes.getOrDefault(node.meta().id(), inheritedScope);
        if (output.put(node.meta().id(), scope) != null)
            throw new IllegalArgumentException("AST node id occurs more than once: " + node.meta().id());
        for (Ast.Node child : Ast.children(node)) index(child, scope, ownerScopes, output);
    }
}
