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
    public enum Claim { COMPLETE, INCOMPLETE }

    public record Uncertainty(String code, String detail) {
        public Uncertainty {
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

    public record Consumption(String analysisGeneration, Claim claim,
                              List<Uncertainty> uncertainties,
                              List<ConsumedCall> calls) {
        public Consumption {
            analysisGeneration = requireText(analysisGeneration, "analysisGeneration");
            claim = Objects.requireNonNull(claim, "claim");
            uncertainties = List.copyOf(uncertainties);
            calls = List.copyOf(calls);
        }
    }

    public static Consumption consume(ExperimentalCobolCallBoundary.Port port) {
        Objects.requireNonNull(port, "port");
        List<ConsumedCall> calls = port.literalCalls().stream()
                .map(ExperimentalCobolCallConsumer::consumeCall)
                .toList();
        List<Uncertainty> uncertainties = port.analysis().uncertainties().stream()
                .map(uncertainty -> new Uncertainty(uncertainty.code(), uncertainty.detail()))
                .toList();
        return new Consumption(port.analysisGeneration(),
                port.analysis().claim() == ExperimentalCobolCallBoundary.Claim.COMPLETE
                        ? Claim.COMPLETE : Claim.INCOMPLETE,
                uncertainties, calls);
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
