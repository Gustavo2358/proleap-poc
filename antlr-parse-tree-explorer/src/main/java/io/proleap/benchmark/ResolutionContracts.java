package io.proleap.benchmark;

import java.util.List;
import java.util.Objects;

/**
 * Immutable vocabulary shared by future reference-resolution products.
 *
 * <p>This class deliberately contains no resolver, candidates or bindings.</p>
 */
public final class ResolutionContracts {
    private ResolutionContracts() { }

    public enum ReferenceKind {
        DATA,
        CONDITION,
        INDEX,
        PROCEDURE,
        FILE,
        PROGRAM,
        PRESERVED_NAMED
    }

    public enum ReferenceRole {
        VALUE_READ,
        VALUE_WRITE,
        CALL_TARGET,
        CALL_ARGUMENT,
        CALL_RETURNING,
        QUALIFIER_COMPONENT,
        SUBSCRIPT,
        REFERENCE_MODIFICATION_OFFSET,
        REFERENCE_MODIFICATION_LENGTH,
        GO_TO_TARGET,
        GO_TO_SELECTOR,
        PERFORM_FROM,
        PERFORM_THROUGH,
        REDEFINES_TARGET,
        RENAMES_FROM,
        RENAMES_THROUGH,
        OCCURS_DEPENDING_ON,
        OCCURS_KEY,
        OCCURS_INDEX,
        PROCEDURE_PARAMETER,
        PROCEDURE_RETURNING,
        FILE_OPERATION,
        DECLARATION_RELATION,
        CONTEXT_DEPENDENT
    }

    public enum ResolutionStatus {
        RESOLVED,
        AMBIGUOUS,
        UNRESOLVED,
        UNSUPPORTED
    }

    public enum ResolutionReason {
        UNIQUE_VISIBLE_DECLARATION,
        QUALIFIED_HIERARCHY_MATCH,
        MULTIPLE_VALID_CANDIDATES,
        DECLARATION_NOT_FOUND,
        EXTERNAL_CATALOG_NOT_PROVIDED,
        INPUT_INCOMPLETE,
        UNSUPPORTED_GRAMMAR_FORM,
        UNSUPPORTED_DIALECT_OPTION,
        INVALID_NAMESPACE_FOR_CONTEXT
    }

    public enum QualifyMode {
        STANDARD,
        EXTEND,
        UNSPECIFIED
    }

    /** Namespaces local integer IDs by one deterministic program unit. */
    public record ProgramUnitId(String compilationUnitId, List<Integer> structuralPath,
                                String canonicalProgramName) {
        public ProgramUnitId {
            compilationUnitId = requireText(compilationUnitId, "compilationUnitId");
            structuralPath = List.copyOf(structuralPath);
            if (structuralPath.stream().anyMatch(index -> index == null || index < 0))
                throw new IllegalArgumentException("structuralPath must contain non-negative indexes");
            canonicalProgramName = requireText(canonicalProgramName, "canonicalProgramName");
        }
    }

    /** Versioned dialect/options contract; absence of an option remains explicit. */
    public record CobolResolutionPolicy(String policyId, String version, QualifyMode qualifyMode) {
        public CobolResolutionPolicy {
            policyId = requireText(policyId, "policyId");
            version = requireText(version, "version");
            qualifyMode = Objects.requireNonNull(qualifyMode, "qualifyMode");
        }

        public static CobolResolutionPolicy initial() {
            return new CobolResolutionPolicy(
                    "proleap-cobol/ibm-enterprise-compatible", "1.0.0", QualifyMode.UNSPECIFIED);
        }
    }

    /** Conservative completion flags composed later with frontend coverage. */
    public record Completeness(boolean referenceBindingComplete, boolean dependencyAnalysisReady,
                               List<String> blockingReasons) {
        public Completeness {
            blockingReasons = List.copyOf(blockingReasons);
            if ((referenceBindingComplete || dependencyAnalysisReady) && !blockingReasons.isEmpty())
                throw new IllegalArgumentException("a complete result cannot have blocking reasons");
            if (dependencyAnalysisReady && !referenceBindingComplete)
                throw new IllegalArgumentException("dependency readiness requires complete reference binding");
            if (!referenceBindingComplete && blockingReasons.isEmpty())
                throw new IllegalArgumentException("an incomplete result must explain why");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
