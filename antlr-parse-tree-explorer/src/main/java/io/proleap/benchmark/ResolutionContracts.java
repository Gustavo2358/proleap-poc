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

    public enum PgmnameMode {
        COMPAT,
        LONGUPPER,
        LONGMIXED,
        UNSPECIFIED;

        public static PgmnameMode fromCompilerValue(String value) {
            return switch (value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT)) {
                case "CO", "COMPAT" -> COMPAT;
                case "LU", "U", "UPPER", "LONGUPPER" -> LONGUPPER;
                case "LM", "M", "MIXED", "LONGMIXED" -> LONGMIXED;
                default -> UNSPECIFIED;
            };
        }
    }

    public enum DynamMode {
        DYNAM,
        NODYNAM,
        UNSPECIFIED;

        public static DynamMode fromCompilerOption(String name, String value) {
            String option = name == null ? "" : name.trim().toUpperCase(java.util.Locale.ROOT);
            String setting = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
            if (option.equals("NODYNAM") || option.equals("NODYN")) return NODYNAM;
            if (option.equals("DYNAM") || option.equals("DYN"))
                return setting.equals("NO") ? NODYNAM : DYNAM;
            return UNSPECIFIED;
        }
    }

    public enum CallLinkage {
        STATIC,
        DYNAMIC,
        UNKNOWN
    }

    public enum SemanticEntityDomain {
        DATA_SYMBOL,
        INDEX_SYMBOL,
        PROCEDURE_SYMBOL,
        FILE_ENTITY,
        PROGRAM_UNIT,
        EXTERNAL_PROGRAM
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

    /** Stable identity inside one deterministic semantic-analysis generation. */
    public record SemanticEntityId(ProgramUnitId programUnitId, SemanticEntityDomain domain,
                                   int localId) {
        public SemanticEntityId {
            programUnitId = Objects.requireNonNull(programUnitId, "programUnitId");
            domain = Objects.requireNonNull(domain, "domain");
            if (localId < 0) throw new IllegalArgumentException("localId must be non-negative");
        }
    }

    /** Versioned dialect/options contract; absence of an option remains explicit. */
    public record CobolResolutionPolicy(String policyId, String version, QualifyMode qualifyMode,
                                        PgmnameMode pgmnameMode, DynamMode dynamMode) {
        public CobolResolutionPolicy {
            policyId = requireText(policyId, "policyId");
            version = requireText(version, "version");
            qualifyMode = Objects.requireNonNull(qualifyMode, "qualifyMode");
            pgmnameMode = Objects.requireNonNull(pgmnameMode, "pgmnameMode");
            dynamMode = Objects.requireNonNull(dynamMode, "dynamMode");
        }

        public CobolResolutionPolicy(String policyId, String version, QualifyMode qualifyMode,
                                     PgmnameMode pgmnameMode) {
            this(policyId, version, qualifyMode, pgmnameMode, DynamMode.UNSPECIFIED);
        }

        public CobolResolutionPolicy(String policyId, String version, QualifyMode qualifyMode) {
            this(policyId, version, qualifyMode, PgmnameMode.UNSPECIFIED);
        }

        public static CobolResolutionPolicy initial() {
            return new CobolResolutionPolicy(
                    "proleap-cobol/explicit-options", "3.0.0", QualifyMode.UNSPECIFIED,
                    PgmnameMode.UNSPECIFIED, DynamMode.UNSPECIFIED);
        }

        public CobolResolutionPolicy withPgmnameMode(PgmnameMode mode) {
            return new CobolResolutionPolicy(policyId, version, qualifyMode, mode, dynamMode);
        }

        public CobolResolutionPolicy withDynamMode(DynamMode mode) {
            return new CobolResolutionPolicy(policyId, version, qualifyMode, pgmnameMode, mode);
        }
    }

    /** Conservative completion flags composed later with frontend coverage. */
    public record Completeness(boolean referenceBindingComplete, boolean dependencyAnalysisReady,
                               List<String> blockingReasons) {
        public Completeness {
            blockingReasons = List.copyOf(blockingReasons);
            if (dependencyAnalysisReady && !blockingReasons.isEmpty())
                throw new IllegalArgumentException("dependency-ready result cannot have blocking reasons");
            if (dependencyAnalysisReady && !referenceBindingComplete)
                throw new IllegalArgumentException("dependency readiness requires complete reference binding");
            if (!dependencyAnalysisReady && blockingReasons.isEmpty())
                throw new IllegalArgumentException("a dependency-incomplete result must explain why");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
