package io.github.gustavo2358.lower.application;

import java.util.HashMap;
import java.util.Map;

/** One publication's exact collision registry; descriptors retain original strings, never encoded copies. */
final class LocalIds {
    static final String POLICY = "local-xxh3-128-v1";
    private record Identity(String namespace, String role, String owner, String key) { }
    private final Map<String, Identity> registered;
    private final String context;
    private final java.util.Set<String> activations;
    LocalIds() { this(new HashMap<>(), "", java.util.Set.of()); }
    private LocalIds(Map<String, Identity> registered, String context, java.util.Set<String> activations) {
        this.registered = registered; this.context = context; this.activations = activations;
    }
    LocalIds activation(String callsite) {
        var nested = new java.util.HashSet<>(activations); nested.add(callsite);
        return new LocalIds(registered, (context.isEmpty() ? "" : context + "/") + callsite.length() + ":" + callsite, java.util.Set.copyOf(nested));
    }
    boolean containsActivation(String callsite) { return activations.contains(callsite); }

    String sourceKey(String key) { return context.isEmpty() ? key : context + "/" + key; }

    String id(String namespace, String role, String owner, String key) {
        if (!context.isEmpty()) key = context + ":" + key;
        var identity = new Identity(namespace, role, owner, key);
        String digest = CanonicalRevision.local(namespace, role, owner, key);
        var previous = registered.putIfAbsent(digest, identity);
        if (previous != null && !previous.equals(identity))
            throw new IllegalStateException("LOCAL_ID_COLLISION: distinct identities share " + digest);
        return digest;
    }
}
