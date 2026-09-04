package io.github.gustavo2358.cobolexplorer.semanticproduct.consumer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.boundary.ExperimentalCobolCallBoundary;

import java.util.List;
import java.util.Objects;

/**
 * Deliberately independent test consumer. Its only semantic input is the
 * boundary port, never the frontend or the adapter that built it.
 */
public final class ExperimentalCobolCallConsumer {
    private ExperimentalCobolCallConsumer() { }

    public enum ObservedCallKind { EXTERNAL_LITERAL }
    public enum Linkage { STATIC, DYNAMIC, DLL, UNKNOWN }
    public enum RuntimeTargetKnowledge { UNKNOWN }
    public enum ReferenceBindingCompleteness { COMPLETE, INCOMPLETE }
    public enum DependencyReadiness { READY, INCOMPLETE }
    public enum UncertaintyScope { CALL_LINKAGE }

    public record Uncertainty(String unitName, int callSiteLocalId,
                              UncertaintyScope scope, String code, String detail) {
        public Uncertainty {
            unitName = requireText(unitName, "unitName");
            if (callSiteLocalId < 0) throw new IllegalArgumentException("callSiteLocalId must be non-negative");
            scope = Objects.requireNonNull(scope, "scope");
            code = requireText(code, "code");
            detail = requireText(detail, "detail");
        }
    }

    public record ConsumedCall(String unitName, String observedTarget,
                               ObservedCallKind kind, Linkage linkage,
                               RuntimeTargetKnowledge runtimeTarget,
                               String sourceFile, int sourceLine, boolean exactSource) {
        public ConsumedCall {
            unitName = requireText(unitName, "unitName");
            observedTarget = requireText(observedTarget, "observedTarget");
            kind = Objects.requireNonNull(kind, "kind");
            linkage = Objects.requireNonNull(linkage, "linkage");
            runtimeTarget = Objects.requireNonNull(runtimeTarget, "runtimeTarget");
            sourceFile = requireText(sourceFile, "sourceFile");
        }
    }

    public record Consumption(ReferenceBindingCompleteness referenceBindingCompleteness,
                              DependencyReadiness dependencyReadiness,
                              List<Uncertainty> uncertainties,
                              List<ConsumedCall> calls) {
        public Consumption {
            referenceBindingCompleteness = Objects.requireNonNull(
                    referenceBindingCompleteness, "referenceBindingCompleteness");
            dependencyReadiness = Objects.requireNonNull(dependencyReadiness,
                    "dependencyReadiness");
            uncertainties = List.copyOf(uncertainties);
            calls = List.copyOf(calls);
        }
    }

    public static Consumption consume(ExperimentalCobolCallBoundary.Port port) {
        Objects.requireNonNull(port, "port");
        List<ConsumedCall> calls = port.literalCalls().stream()
                .map(ExperimentalCobolCallConsumer::consumeCall)
                .toList();
        List<Uncertainty> uncertainties = port.callAnalysis().uncertainties().stream()
                .map(ExperimentalCobolCallConsumer::consumeUncertainty)
                .toList();
        return new Consumption(
                port.callAnalysis().referenceBindingCompleteness()
                        == ExperimentalCobolCallBoundary.ReferenceBindingCompleteness.COMPLETE
                        ? ReferenceBindingCompleteness.COMPLETE : ReferenceBindingCompleteness.INCOMPLETE,
                port.callAnalysis().dependencyReadiness()
                        == ExperimentalCobolCallBoundary.DependencyReadiness.READY
                        ? DependencyReadiness.READY : DependencyReadiness.INCOMPLETE,
                uncertainties, calls);
    }

    private static Uncertainty consumeUncertainty(
            ExperimentalCobolCallBoundary.Uncertainty uncertainty) {
        return new Uncertainty(uncertainty.site().unit().canonicalProgramName(),
                uncertainty.site().localId(), mapScope(uncertainty.scope()),
                uncertainty.code(), uncertainty.detail());
    }

    private static UncertaintyScope mapScope(
            ExperimentalCobolCallBoundary.UncertaintyScope scope) {
        return switch (scope) {
            case CALL_LINKAGE -> UncertaintyScope.CALL_LINKAGE;
        };
    }

    private static ConsumedCall consumeCall(ExperimentalCobolCallBoundary.CallFact call) {
        if (call.targetSyntax() != ExperimentalCobolCallBoundary.TargetSyntax.LITERAL_PROGRAM_NAME)
            throw new IllegalArgumentException("consumer received a non-literal CALL");
        if (call.status() != ExperimentalCobolCallBoundary.ResolutionStatus.EXTERNAL_OBSERVED)
            throw new IllegalArgumentException("literal CALL must preserve external observation");
        if (call.reason() != ExperimentalCobolCallBoundary.ResolutionReason.LITERAL_EXTERNAL_PROGRAM)
            throw new IllegalArgumentException("literal CALL has an unexpected resolution reason");
        ExperimentalCobolCallBoundary.Provenance provenance = call.provenance();
        return new ConsumedCall(call.site().unit().canonicalProgramName(), call.observedTarget(),
                ObservedCallKind.EXTERNAL_LITERAL, mapLinkage(call.linkage()),
                RuntimeTargetKnowledge.UNKNOWN, provenance.original().file(),
                provenance.original().startLine(), provenance.exact());
    }

    private static Linkage mapLinkage(ExperimentalCobolCallBoundary.Linkage linkage) {
        return switch (linkage) {
            case STATIC -> Linkage.STATIC;
            case DYNAMIC -> Linkage.DYNAMIC;
            case DLL -> Linkage.DLL;
            case UNKNOWN -> Linkage.UNKNOWN;
        };
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
