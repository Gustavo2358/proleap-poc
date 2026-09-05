package io.github.gustavo2358.cobolexplorer.semanticproduct.consumer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.boundary.ExperimentalCobolMoveCallBoundary;

import java.util.List;
import java.util.Objects;

/**
 * Independent test consumer for the first MOVE-to-CALL slice. It receives only
 * the boundary port and reconstructs the facts needed by a future downstream
 * analysis; it does not retain or inspect frontend objects.
 */
public final class ExperimentalCobolMoveCallConsumer {
    private ExperimentalCobolMoveCallConsumer() { }

    public enum RuntimeTargetKnowledge { UNKNOWN }

    public record SourceAnchor(String file, int line, boolean exact) {
        public SourceAnchor {
            file = requireText(file, "file");
        }
    }

    public record DataItem(ExperimentalCobolMoveCallBoundary.DataItemId identity,
                           String name, String picture, SourceAnchor declaration) {
        public DataItem {
            identity = Objects.requireNonNull(identity, "identity");
            name = requireText(name, "name");
            picture = requireText(picture, "picture");
            declaration = Objects.requireNonNull(declaration, "declaration");
        }
    }

    public record Move(String literal,
                       ExperimentalCobolMoveCallBoundary.DataItemId target,
                       SourceAnchor literalSource, SourceAnchor statement) {
        public Move {
            literal = requireText(literal, "literal");
            target = Objects.requireNonNull(target, "target");
            literalSource = Objects.requireNonNull(literalSource, "literalSource");
            statement = Objects.requireNonNull(statement, "statement");
        }
    }

    public record Call(ExperimentalCobolMoveCallBoundary.DataItemId operand,
                       RuntimeTargetKnowledge runtimeTarget, SourceAnchor statement) {
        public Call {
            operand = Objects.requireNonNull(operand, "operand");
            runtimeTarget = Objects.requireNonNull(runtimeTarget, "runtimeTarget");
            statement = Objects.requireNonNull(statement, "statement");
        }
    }

    public record Reconstruction(DataItem data, Move move, Call call,
                                 boolean movePrecedesCall, List<String> uncertaintyCodes) {
        public Reconstruction {
            data = Objects.requireNonNull(data, "data");
            move = Objects.requireNonNull(move, "move");
            call = Objects.requireNonNull(call, "call");
            uncertaintyCodes = List.copyOf(uncertaintyCodes);
        }
    }

    public static Reconstruction consume(ExperimentalCobolMoveCallBoundary.Port port) {
        Objects.requireNonNull(port, "port");
        ExperimentalCobolMoveCallBoundary.MoveFact move = port.move();
        ExperimentalCobolMoveCallBoundary.CallFact call = port.call();
        if (move.targetBinding()
                != ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE)
            throw new IllegalArgumentException("MOVE target binding is incomplete");
        if (call.operandBinding()
                != ExperimentalCobolMoveCallBoundary.BindingStatus.COMPLETE)
            throw new IllegalArgumentException("CALL operand binding is incomplete");
        if (!move.target().equals(call.operand()))
            throw new IllegalArgumentException("MOVE target and CALL operand differ");
        if (call.runtimeTarget()
                != ExperimentalCobolMoveCallBoundary.RuntimeTargetKnowledge.UNKNOWN)
            throw new IllegalArgumentException("runtime CALL target must remain unknown");

        ExperimentalCobolMoveCallBoundary.DataDeclaration declaration = port.dataItems().stream()
                .filter(item -> item.id().equals(move.target()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "bound data item declaration is not published"));
        boolean ordered = port.ordering().earlier().equals(move.point())
                && port.ordering().later().equals(call.point());
        if (!ordered)
            throw new IllegalArgumentException("MOVE/CALL ordering is not published");

        return new Reconstruction(
                new DataItem(declaration.id(), declaration.name(), declaration.picture(),
                        anchor(declaration.provenance())),
                new Move(move.source().value(), move.target(), anchor(move.source().provenance()),
                        anchor(move.provenance())),
                new Call(call.operand(), mapRuntimeTarget(call.runtimeTarget()),
                        anchor(call.provenance())),
                ordered,
                port.analysis().uncertainties().stream()
                        .map(ExperimentalCobolMoveCallBoundary.Uncertainty::code).toList());
    }

    private static RuntimeTargetKnowledge mapRuntimeTarget(
            ExperimentalCobolMoveCallBoundary.RuntimeTargetKnowledge runtimeTarget) {
        return switch (runtimeTarget) {
            case UNKNOWN -> RuntimeTargetKnowledge.UNKNOWN;
        };
    }

    private static SourceAnchor anchor(ExperimentalCobolMoveCallBoundary.Provenance provenance) {
        ExperimentalCobolMoveCallBoundary.Location location = provenance.original();
        return new SourceAnchor(location.file(), location.startLine(), provenance.exact());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
