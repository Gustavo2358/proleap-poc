package io.github.gustavo2358.cobolexplorer.semanticproduct.boundary;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Test-only A2/B seam for the smallest executable semantic slice: literal CALLs.
 *
 * <p>Every type in this class belongs to the experimental boundary. The state is
 * materialized once and the port only reads that state; no frontend lifecycle is
 * represented here.</p>
 */
public final class ExperimentalCobolCallBoundary {
    private ExperimentalCobolCallBoundary() { }

    public enum TargetSyntax { LITERAL_PROGRAM_NAME }

    public enum ResolutionStatus {
        RESOLVED,
        EXTERNAL_OBSERVED,
        AMBIGUOUS,
        UNRESOLVED,
        UNSUPPORTED
    }

    public enum ResolutionReason {
        UNIQUE_VISIBLE_DECLARATION,
        LITERAL_EXTERNAL_PROGRAM,
        MULTIPLE_VALID_CANDIDATES,
        DECLARATION_NOT_FOUND,
        INPUT_INCOMPLETE,
        UNSUPPORTED_GRAMMAR_FORM,
        UNSUPPORTED_DIALECT_OPTION,
        INVALID_NAMESPACE_FOR_CONTEXT
    }

    public enum Linkage { STATIC, DYNAMIC, DLL, UNKNOWN }
    public enum DynamMode { DYNAM, NODYNAM, UNSPECIFIED }
    public enum DllMode { DLL, NODLL, UNSPECIFIED }
    public enum Claim { COMPLETE, INCOMPLETE }
    public enum Availability { AVAILABLE, INPUT_MISSING, UNSUPPORTED, NOT_PRODUCED }

    /** Namespace required to interpret a local identity within one generation. */
    public record UnitId(String compilationUnitId, List<Integer> structuralPath,
                         String canonicalProgramName) {
        public UnitId {
            compilationUnitId = requireText(compilationUnitId, "compilationUnitId");
            structuralPath = List.copyOf(structuralPath);
            if (structuralPath.stream().anyMatch(index -> index == null || index < 0))
                throw new IllegalArgumentException("structuralPath must contain non-negative indexes");
            canonicalProgramName = requireText(canonicalProgramName, "canonicalProgramName");
        }
    }

    /** Boundary-local call-site identity; the local number is never global by itself. */
    public record CallSiteId(UnitId unit, int localId) {
        public CallSiteId {
            unit = Objects.requireNonNull(unit, "unit");
            if (localId < 0) throw new IllegalArgumentException("localId must be non-negative");
        }
    }

    public record Location(String file, int startLine, int startColumn,
                           int endLine, int endColumn) {
        public Location {
            file = requireText(file, "file");
        }
    }

    public record IncludeFrame(String includingFile, String requestedName,
                               String includedFile, int includeLine) {
        public IncludeFrame {
            includingFile = requireText(includingFile, "includingFile");
            requestedName = requireText(requestedName, "requestedName");
            includedFile = requireText(includedFile, "includedFile");
        }
    }

    /** Localized provenance, deliberately smaller than a source-map implementation. */
    public record Provenance(Location expanded, Location original,
                             List<IncludeFrame> includeChain, boolean exact) {
        public Provenance {
            expanded = Objects.requireNonNull(expanded, "expanded");
            original = Objects.requireNonNull(original, "original");
            includeChain = List.copyOf(includeChain);
        }
    }

    /** Normalized policy observed by this slice; absent options remain explicit. */
    public record Policy(String policyId, String version, DynamMode dynamMode,
                         DllMode dllMode) {
        public Policy {
            policyId = requireText(policyId, "policyId");
            version = requireText(version, "version");
            dynamMode = Objects.requireNonNull(dynamMode, "dynamMode");
            dllMode = Objects.requireNonNull(dllMode, "dllMode");
        }
    }

    public record Uncertainty(String code, String detail) {
        public Uncertainty {
            code = requireText(code, "code");
            detail = requireText(detail, "detail");
        }
    }

    /** Completeness is a fact of the publication, not an empty-list convention. */
    public record AnalysisState(Claim claim, Availability availability,
                                List<Uncertainty> uncertainties) {
        public AnalysisState {
            claim = Objects.requireNonNull(claim, "claim");
            availability = Objects.requireNonNull(availability, "availability");
            uncertainties = List.copyOf(uncertainties);
            if (claim == Claim.COMPLETE && !uncertainties.isEmpty())
                throw new IllegalArgumentException("complete analysis cannot carry uncertainties");
            if (claim == Claim.INCOMPLETE && uncertainties.isEmpty())
                throw new IllegalArgumentException("incomplete analysis must explain uncertainty");
        }
    }

    /**
     * Materialized semantic fact for one literal CALL. It contains no source text
     * or grammar metadata as an input to interpretation.
     */
    public record CallFact(CallSiteId site, String observedTarget,
                           TargetSyntax targetSyntax, ResolutionStatus status,
                           ResolutionReason reason, Linkage linkage,
                           Provenance provenance) {
        public CallFact {
            site = Objects.requireNonNull(site, "site");
            observedTarget = requireText(observedTarget, "observedTarget");
            targetSyntax = Objects.requireNonNull(targetSyntax, "targetSyntax");
            status = Objects.requireNonNull(status, "status");
            reason = Objects.requireNonNull(reason, "reason");
            linkage = Objects.requireNonNull(linkage, "linkage");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** A2: one immutable publication for one analysis generation. */
    public record State(String analysisGeneration, UnitId unit, Policy policy,
                        AnalysisState analysis, List<CallFact> literalCalls) {
        public State {
            analysisGeneration = requireText(analysisGeneration, "analysisGeneration");
            unit = Objects.requireNonNull(unit, "unit");
            policy = Objects.requireNonNull(policy, "policy");
            analysis = Objects.requireNonNull(analysis, "analysis");
            literalCalls = List.copyOf(literalCalls);
            for (CallFact call : literalCalls) {
                if (!call.site().unit().equals(unit))
                    throw new IllegalArgumentException("call site belongs to another unit");
                if (call.targetSyntax() != TargetSyntax.LITERAL_PROGRAM_NAME)
                    throw new IllegalArgumentException("state accepts literal calls only");
            }
        }
    }

    /** B: read-only queries over the already materialized A2 state. */
    public interface Port {
        String analysisGeneration();
        UnitId unit();
        Policy policy();
        AnalysisState analysis();
        List<CallFact> literalCalls();
    }

    public static Port open(State state) {
        return new MaterializedPort(Objects.requireNonNull(state, "state"));
    }

    private record MaterializedPort(State state) implements Port {
        private MaterializedPort {
            state = Objects.requireNonNull(state, "state");
        }

        @Override
        public String analysisGeneration() { return state.analysisGeneration(); }

        @Override
        public UnitId unit() { return state.unit(); }

        @Override
        public Policy policy() { return state.policy(); }

        @Override
        public AnalysisState analysis() { return state.analysis(); }

        @Override
        public List<CallFact> literalCalls() {
            return Collections.unmodifiableList(state.literalCalls());
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
