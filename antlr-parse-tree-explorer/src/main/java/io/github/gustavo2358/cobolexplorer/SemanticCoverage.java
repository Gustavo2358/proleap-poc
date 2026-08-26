package io.github.gustavo2358.cobolexplorer;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Conservative, immutable observability for Parse Tree to AST transformation. */
public final class SemanticCoverage {
    private SemanticCoverage() { }

    public enum ConstructionCoverage {
        MODELED,
        PRESERVED_UNINTERPRETED,
        UNSUPPORTED,
        INPUT_MISSING
    }

    public enum DependencyKnowledge {
        REFERENCE_READY,
        DEPENDENCY_UNKNOWN,
        NOT_DEPENDENCY_BEARING
    }

    public record Finding(int id, String grammarRule, Ast.Meta meta, String writtenText,
                          ConstructionCoverage coverage, DependencyKnowledge dependencyKnowledge,
                          String reason, int astNodeId) {
        public Finding {
            if (id < 0) throw new IllegalArgumentException("finding id must be non-negative");
            grammarRule = requireText(grammarRule, "grammarRule");
            meta = Objects.requireNonNull(meta, "meta");
            writtenText = Objects.requireNonNullElse(writtenText, "");
            coverage = Objects.requireNonNull(coverage, "coverage");
            dependencyKnowledge = Objects.requireNonNull(dependencyKnowledge, "dependencyKnowledge");
            reason = requireText(reason, "reason");
            if (astNodeId < -1) throw new IllegalArgumentException("astNodeId must be -1 or non-negative");
        }
    }

    public record Diagnostic(String code, String message, Ast.Meta meta) {
        public Diagnostic {
            code = requireText(code, "code");
            message = requireText(message, "message");
            meta = Objects.requireNonNull(meta, "meta");
        }
    }

    public record Report(List<Finding> findings) {
        public Report {
            findings = List.copyOf(findings);
            for (int index = 0; index < findings.size(); index++) {
                if (findings.get(index).id() != index)
                    throw new IllegalArgumentException("finding ids must be deterministic and contiguous");
            }
        }

        public boolean dependencyCoverageComplete() {
            return findings.stream().noneMatch(finding ->
                    finding.coverage() == ConstructionCoverage.UNSUPPORTED
                            || finding.coverage() == ConstructionCoverage.INPUT_MISSING
                            || finding.dependencyKnowledge() == DependencyKnowledge.DEPENDENCY_UNKNOWN);
        }

        public Map<ConstructionCoverage, Long> constructionCounts() {
            EnumMap<ConstructionCoverage, Long> result = new EnumMap<>(ConstructionCoverage.class);
            for (ConstructionCoverage value : ConstructionCoverage.values()) result.put(value, 0L);
            for (Finding finding : findings) result.merge(finding.coverage(), 1L, Long::sum);
            return Map.copyOf(result);
        }

        public Map<DependencyKnowledge, Long> dependencyCounts() {
            EnumMap<DependencyKnowledge, Long> result = new EnumMap<>(DependencyKnowledge.class);
            for (DependencyKnowledge value : DependencyKnowledge.values()) result.put(value, 0L);
            for (Finding finding : findings) result.merge(finding.dependencyKnowledge(), 1L, Long::sum);
            return Map.copyOf(result);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
