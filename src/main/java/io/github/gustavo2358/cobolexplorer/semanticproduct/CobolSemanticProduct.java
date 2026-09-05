package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Boundary-owned semantic state for the first narrow COBOL slice.
 *
 * <p>This is a materialized in-memory product, not a view of frontend
 * products.  It deliberately models only one DATA item, one literal MOVE and
 * one variable CALL.  Values of the MOVE and nominal DATA binding are facts;
 * they do not constitute runtime-value analysis.</p>
 */
public final class CobolSemanticProduct {
    private CobolSemanticProduct() { }

    public enum BindingStatus { COMPLETE, INCOMPLETE }

    public enum ResolutionStatus {
        RESOLVED,
        AMBIGUOUS,
        UNRESOLVED,
        UNSUPPORTED,
        INPUT_MISSING
    }

    public enum ResolutionReason {
        UNIQUE_VISIBLE_DECLARATION,
        MULTIPLE_VALID_CANDIDATES,
        DECLARATION_NOT_FOUND,
        INPUT_INCOMPLETE,
        UNSUPPORTED_GRAMMAR_FORM,
        UNSUPPORTED_DIALECT_OPTION,
        INVALID_NAMESPACE_FOR_CONTEXT
    }

    public enum CallSyntax { IDENTIFIER_OR_EXPRESSION }

    /** Runtime values are outside nominal binding and this checkpoint. */
    public enum RuntimeTargetKnowledge { UNKNOWN }

    public enum AnalysisClaim { COMPLETE, PARTIAL, UNKNOWN }

    public enum DependencyReadiness { READY, INCOMPLETE, UNKNOWN }

    public enum UncertaintyScope {
        RUNTIME_CALL_TARGET,
        NOMINAL_BINDING,
        ANALYSIS_INPUT
    }

    public enum QualifyMode { STANDARD, EXTEND, UNSPECIFIED }

    public enum PgmnameMode { COMPAT, LONGUPPER, LONGMIXED, UNSPECIFIED }

    public enum DynamMode { DYNAM, NODYNAM, UNSPECIFIED }

    public enum DllMode { DLL, NODLL, UNSPECIFIED }

    /**
     * A unit namespace.  Structural/local identities are meaningful only
     * together with this value and are not persistent identities across edits
     * or analyzer/contract versions.
     */
    public record UnitId(String compilationUnitId, List<Integer> structuralPath,
                         String canonicalProgramName) {
        public UnitId {
            compilationUnitId = requireText(compilationUnitId, "compilationUnitId");
            structuralPath = List.copyOf(structuralPath);
            if (structuralPath.stream().anyMatch(index -> index == null || index < 0))
                throw new IllegalArgumentException(
                        "structuralPath must contain non-negative indexes");
            canonicalProgramName = requireText(canonicalProgramName, "canonicalProgramName");
        }
    }

    /**
     * Boundary-local DATA identity.  The local id is never a global or
     * persistent identity by itself.
     */
    public record DataItemId(UnitId unit, int localId) {
        public DataItemId {
            unit = Objects.requireNonNull(unit, "unit");
            if (localId < 0)
                throw new IllegalArgumentException("localId must be non-negative");
        }
    }

    public record Location(String file, int startLine, int startColumn,
                           int endLine, int endColumn) {
        public Location {
            file = requireText(file, "file");
            if (startLine < 0 || startColumn < 0 || endLine < 0 || endColumn < 0)
                throw new IllegalArgumentException("location coordinates must be non-negative");
        }
    }

    public record IncludeFrame(String includingFile, String requestedName,
                               String includedFile, int includeLine) {
        public IncludeFrame {
            includingFile = requireText(includingFile, "includingFile");
            requestedName = requireText(requestedName, "requestedName");
            includedFile = requireText(includedFile, "includedFile");
            if (includeLine < 0)
                throw new IllegalArgumentException("includeLine must be non-negative");
        }
    }

    /** Localized provenance; the full frontend SourceMap does not cross here. */
    public record Provenance(Location expanded, Location original,
                             List<IncludeFrame> includeChain, boolean exact) {
        public Provenance {
            expanded = Objects.requireNonNull(expanded, "expanded");
            original = Objects.requireNonNull(original, "original");
            includeChain = List.copyOf(includeChain);
        }
    }

    public record Policy(String policyId, String version, QualifyMode qualifyMode,
                         PgmnameMode pgmnameMode, DynamMode dynamMode, DllMode dllMode) {
        public Policy {
            policyId = requireText(policyId, "policyId");
            version = requireText(version, "version");
            qualifyMode = Objects.requireNonNull(qualifyMode, "qualifyMode");
            pgmnameMode = Objects.requireNonNull(pgmnameMode, "pgmnameMode");
            dynamMode = Objects.requireNonNull(dynamMode, "dynamMode");
            dllMode = Objects.requireNonNull(dllMode, "dllMode");
        }

        /** The absence of compiler options remains explicit in every mode. */
        public static Policy unspecified() {
            return new Policy("cobol-explorer/explicit-options", "3.0.0",
                    QualifyMode.UNSPECIFIED, PgmnameMode.UNSPECIFIED,
                    DynamMode.UNSPECIFIED, DllMode.UNSPECIFIED);
        }
    }

    public record DataDeclaration(DataItemId id, String name, String picture,
                                  Provenance provenance) {
        public DataDeclaration {
            id = Objects.requireNonNull(id, "id");
            name = requireText(name, "name");
            picture = requireText(picture, "picture");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record LiteralSource(String value, Provenance provenance) {
        public LiteralSource {
            value = Objects.requireNonNull(value, "value");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** Source/program order only; this is not CFG, reachability or execution order. */
    public record ProgramPoint(int ordinal) {
        public ProgramPoint {
            if (ordinal < 0)
                throw new IllegalArgumentException("ordinal must be non-negative");
        }
    }

    /** A DATA candidate is a boundary fact, not a frontend symbol reference. */
    public record DataCandidate(DataItemId id, String canonicalName) {
        public DataCandidate {
            id = Objects.requireNonNull(id, "id");
            canonicalName = requireText(canonicalName, "canonicalName");
        }
    }

    /**
     * Nominal resolution preserves status, reason and every candidate.  A
     * selected identity exists only for a unique resolved DATA binding.
     */
    public record NominalBinding(BindingStatus status, ResolutionStatus resolution,
                                 ResolutionReason reason, List<DataCandidate> candidates,
                                 Optional<DataItemId> selected) {
        public NominalBinding {
            status = Objects.requireNonNull(status, "status");
            resolution = Objects.requireNonNull(resolution, "resolution");
            reason = Objects.requireNonNull(reason, "reason");
            candidates = List.copyOf(candidates);
            selected = Objects.requireNonNull(selected, "selected");

            if (resolution == ResolutionStatus.RESOLVED) {
                if (status != BindingStatus.COMPLETE || candidates.size() != 1
                        || selected.isEmpty() || !selected.get().equals(candidates.get(0).id()))
                    throw new IllegalArgumentException(
                            "resolved binding must have exactly one selected candidate");
            } else if (status != BindingStatus.INCOMPLETE || selected.isPresent()) {
                throw new IllegalArgumentException(
                        "non-resolved binding must be incomplete and unselected");
            }
            if (resolution == ResolutionStatus.AMBIGUOUS && candidates.size() < 2)
                throw new IllegalArgumentException(
                        "ambiguous binding must preserve all valid candidates");
        }

        public static NominalBinding resolved(DataItemId id, String canonicalName) {
            DataCandidate candidate = new DataCandidate(id, canonicalName);
            return new NominalBinding(BindingStatus.COMPLETE, ResolutionStatus.RESOLVED,
                    ResolutionReason.UNIQUE_VISIBLE_DECLARATION, List.of(candidate),
                    Optional.of(id));
        }

        public static NominalBinding incomplete(ResolutionStatus resolution,
                                                ResolutionReason reason,
                                                List<DataCandidate> candidates) {
            if (resolution == ResolutionStatus.RESOLVED)
                throw new IllegalArgumentException("resolved binding needs a selected candidate");
            return new NominalBinding(BindingStatus.INCOMPLETE, resolution, reason,
                    candidates, Optional.empty());
        }
    }

    public record MoveFact(ProgramPoint point, LiteralSource source,
                           Optional<DataItemId> target, NominalBinding targetBinding,
                           Provenance provenance) {
        public MoveFact {
            point = Objects.requireNonNull(point, "point");
            source = Objects.requireNonNull(source, "source");
            target = Objects.requireNonNull(target, "target");
            targetBinding = Objects.requireNonNull(targetBinding, "targetBinding");
            provenance = Objects.requireNonNull(provenance, "provenance");
            if (!target.equals(targetBinding.selected()))
                throw new IllegalArgumentException(
                        "MOVE target must equal its selected nominal binding");
        }
    }

    public record CallFact(ProgramPoint point, Optional<DataItemId> operand,
                           CallSyntax syntax, NominalBinding operandBinding,
                           RuntimeTargetKnowledge runtimeTarget,
                           Provenance provenance) {
        public CallFact {
            point = Objects.requireNonNull(point, "point");
            operand = Objects.requireNonNull(operand, "operand");
            syntax = Objects.requireNonNull(syntax, "syntax");
            operandBinding = Objects.requireNonNull(operandBinding, "operandBinding");
            runtimeTarget = Objects.requireNonNull(runtimeTarget, "runtimeTarget");
            provenance = Objects.requireNonNull(provenance, "provenance");
            if (syntax != CallSyntax.IDENTIFIER_OR_EXPRESSION)
                throw new IllegalArgumentException("the slice accepts variable CALL syntax only");
            if (!operand.equals(operandBinding.selected()))
                throw new IllegalArgumentException(
                        "CALL operand must equal its selected nominal binding");
        }
    }

    public record Ordering(ProgramPoint earlier, ProgramPoint later) {
        public Ordering {
            earlier = Objects.requireNonNull(earlier, "earlier");
            later = Objects.requireNonNull(later, "later");
            if (earlier.ordinal() >= later.ordinal())
                throw new IllegalArgumentException("ordering must be strict and forward");
        }
    }

    public record Uncertainty(ProgramPoint point, UncertaintyScope scope,
                              String code, String detail, Provenance provenance) {
        public Uncertainty {
            point = Objects.requireNonNull(point, "point");
            scope = Objects.requireNonNull(scope, "scope");
            code = requireText(code, "code");
            detail = requireText(detail, "detail");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    /**
     * Completeness and runtime knowledge are separate dimensions.  In
     * particular, a known nominal DATA binding may coexist with an unknown
     * runtime CALL target.
     */
    public record AnalysisStatus(BindingStatus nominalBinding,
                                 AnalysisClaim claim,
                                 DependencyReadiness dependencyReadiness,
                                 RuntimeTargetKnowledge runtimeTarget,
                                 List<Uncertainty> uncertainties) {
        public AnalysisStatus {
            nominalBinding = Objects.requireNonNull(nominalBinding, "nominalBinding");
            claim = Objects.requireNonNull(claim, "claim");
            dependencyReadiness = Objects.requireNonNull(
                    dependencyReadiness, "dependencyReadiness");
            runtimeTarget = Objects.requireNonNull(runtimeTarget, "runtimeTarget");
            uncertainties = List.copyOf(uncertainties);

            if (runtimeTarget == RuntimeTargetKnowledge.UNKNOWN
                    && uncertainties.stream().noneMatch(uncertainty ->
                    uncertainty.scope() == UncertaintyScope.RUNTIME_CALL_TARGET))
                throw new IllegalArgumentException(
                        "unknown runtime target must have localized uncertainty");
            if (nominalBinding == BindingStatus.COMPLETE
                    && uncertainties.stream().anyMatch(uncertainty ->
                    uncertainty.scope() == UncertaintyScope.NOMINAL_BINDING))
                throw new IllegalArgumentException(
                        "complete nominal binding cannot carry nominal binding uncertainty");
            if (dependencyReadiness != DependencyReadiness.READY && uncertainties.isEmpty())
                throw new IllegalArgumentException(
                        "non-ready dependency state must explain uncertainty");
            if (runtimeTarget == RuntimeTargetKnowledge.UNKNOWN
                    && dependencyReadiness == DependencyReadiness.READY)
                throw new IllegalArgumentException(
                        "unknown runtime target cannot be dependency-ready");
            if (claim == AnalysisClaim.COMPLETE
                    && (nominalBinding != BindingStatus.COMPLETE
                    || dependencyReadiness != DependencyReadiness.READY
                    || !uncertainties.isEmpty()))
                throw new IllegalArgumentException(
                        "complete claim cannot hide incomplete or unknown facts");
            if (claim == AnalysisClaim.UNKNOWN && uncertainties.isEmpty())
                throw new IllegalArgumentException("unknown claim must explain uncertainty");
        }

        public static AnalysisStatus partial(BindingStatus nominalBinding,
                                             List<Uncertainty> uncertainties) {
            return new AnalysisStatus(nominalBinding, AnalysisClaim.PARTIAL,
                    DependencyReadiness.INCOMPLETE, RuntimeTargetKnowledge.UNKNOWN,
                    uncertainties);
        }
    }

    /** One coherent, immutable publication for the narrow MOVE/CALL slice. */
    public record State(UnitId unit, List<DataDeclaration> dataItems, Policy policy,
                        MoveFact move, CallFact call, Ordering ordering,
                        AnalysisStatus analysis) {
        public State {
            unit = Objects.requireNonNull(unit, "unit");
            dataItems = List.copyOf(dataItems);
            policy = Objects.requireNonNull(policy, "policy");
            move = Objects.requireNonNull(move, "move");
            call = Objects.requireNonNull(call, "call");
            ordering = Objects.requireNonNull(ordering, "ordering");
            analysis = Objects.requireNonNull(analysis, "analysis");

            Set<DataItemId> declarations = new HashSet<>();
            for (DataDeclaration item : dataItems) {
                if (!item.id().unit().equals(unit))
                    throw new IllegalArgumentException("data item belongs to another unit");
                if (!declarations.add(item.id()))
                    throw new IllegalArgumentException("duplicate DATA item identity");
            }
            requireUnit(move.target(), unit, "MOVE target");
            requireUnit(call.operand(), unit, "CALL operand");
            requireCandidateUnits(move.targetBinding(), unit);
            requireCandidateUnits(call.operandBinding(), unit);
            if (!ordering.earlier().equals(move.point())
                    || !ordering.later().equals(call.point()))
                throw new IllegalArgumentException("ordering must publish MOVE before CALL");
            for (Uncertainty uncertainty : analysis.uncertainties()) {
                if (!uncertainty.point().equals(move.point())
                        && !uncertainty.point().equals(call.point()))
                    throw new IllegalArgumentException(
                            "uncertainty must be localized to a published fact");
                if (uncertainty.scope() == UncertaintyScope.RUNTIME_CALL_TARGET
                        && !uncertainty.point().equals(call.point()))
                    throw new IllegalArgumentException(
                            "runtime target uncertainty must be localized to CALL");
            }
            if (move.targetBinding().status() == BindingStatus.COMPLETE
                    && call.operandBinding().status() == BindingStatus.COMPLETE
                    && (!move.target().isPresent() || !call.operand().isPresent()
                    || !move.target().equals(call.operand())))
                throw new IllegalArgumentException(
                        "resolved MOVE and CALL must share the same DATA identity");
            if (analysis.nominalBinding() == BindingStatus.COMPLETE
                    && (move.targetBinding().status() != BindingStatus.COMPLETE
                    || call.operandBinding().status() != BindingStatus.COMPLETE))
                throw new IllegalArgumentException(
                        "analysis cannot claim complete nominal binding for incomplete facts");
            if (move.targetBinding().status() == BindingStatus.COMPLETE
                    && call.operandBinding().status() == BindingStatus.COMPLETE
                    && analysis.nominalBinding() != BindingStatus.COMPLETE)
                throw new IllegalArgumentException(
                        "complete nominal facts must be published as complete binding");
            if (analysis.runtimeTarget() != call.runtimeTarget())
                throw new IllegalArgumentException(
                        "analysis runtime knowledge must match the CALL fact");
            for (DataItemId selected : List.of(move.target(), call.operand()).stream()
                    .flatMap(Optional::stream).toList()) {
                if (!declarations.contains(selected))
                    throw new IllegalArgumentException(
                            "selected DATA identity has no published declaration");
            }
        }

        private static void requireUnit(Optional<DataItemId> identity, UnitId unit,
                                        String name) {
            if (identity.isPresent() && !identity.get().unit().equals(unit))
                throw new IllegalArgumentException(name + " belongs to another unit");
        }

        private static void requireCandidateUnits(NominalBinding binding, UnitId unit) {
            if (binding.candidates().stream().anyMatch(candidate ->
                    !candidate.id().unit().equals(unit)))
                throw new IllegalArgumentException("binding candidate belongs to another unit");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
