package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Optional port for external program identities; it does not load or index source code. */
@FunctionalInterface
public interface ExternalProgramCatalog {
    record Program(int id, String catalogId, String writtenName, Map<String, String> attributes) {
        public Program {
            if (id < 0) throw new IllegalArgumentException("external program id must be non-negative");
            if (catalogId == null || catalogId.isBlank())
                throw new IllegalArgumentException("catalogId must not be blank");
            if (writtenName == null || writtenName.isBlank())
                throw new IllegalArgumentException("writtenName must not be blank");
            attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        }
    }

    List<Program> lookup(String canonicalProgramName);

    static ExternalProgramCatalog empty() { return ignored -> List.of(); }
}
