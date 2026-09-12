package io.github.gustavo2358.cobolexplorer.semanticproduct.ifprofile;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import java.util.*;

/** Boundary-only proof consumer. No AST, names, textual ordering, normalization or runtime analysis. */
public final class IfFactsOracle {
    private IfFactsOracle() { }
    public record SimpleIf(DataItemId read, StatementId thenEntry, Optional<StatementId> elseEntry,
                           StatementId completion, Set<DataItemId> independent) { }
    public static SimpleIf read(CobolSemanticPort port, StatementId id) {
        require(port.statement(id).orElseThrow() instanceof IfFact, "IF required");
        var branch = (IfFact) port.statement(id).orElseThrow();
        require(branch.profile() == IfProfile.SIMPLE_TEXT_EQUALITY, "simple source profile required");
        var predicate = branch.condition().predicate();
        require(predicate.availability() == Availability.KNOWN && predicate.resultDomain() == PredicateDomain.BOOLEAN
                && predicate.evaluation() == PredicateEvaluation.PURE && predicate.normalCompletion() == PredicateCompletion.TOTAL
                && predicate.readsCompleteness() == ReadsCompleteness.COMPLETE && predicate.truthValue() == PredicateTruth.UNKNOWN,
                "predicate guarantees required");
        require(branch.condition().references().size() == 1 && predicate.knownReads().size() == 1, "one complete read required");
        var reference = branch.condition().references().get(0);
        require(predicate.knownReads().equals(List.of(reference.id())) && reference.role() == OperandRole.READ
                && reference.binding().status() == ResolutionStatus.RESOLVED && reference.binding().candidates().size() == 1
                && reference.wholeItemAccess().isPresent() && reference.binding().selected().equals(Optional.of(reference.wholeItemAccess().orElseThrow().data())),
                "resolved whole-item occurrence required");
        require(reference.provenance().exact() && predicate.provenance().exact() && branch.header().provenance().exact(), "exact origin required");
        var next = branch.normalContinuation();
        require(next.availability() == ContinuationAvailability.KNOWN && next.provenance().exact(), "IF completion required");
        require(branch.continuation().equals(next.statement()), "structural/executable completion mismatch");
        var thenEntry = arm(port, branch, Branch.THEN, branch.thenArm(), next.statement().orElseThrow());
        var elseEntry = arm(port, branch, Branch.ELSE, branch.elseArm(), next.statement().orElseThrow());
        require(thenEntry.isPresent(), "THEN entry required");
        var proof = port.storageIndependence();
        require(proof.availability() == Availability.KNOWN && proof.members().size() >= 2 && proof.provenance().orElseThrow().exact(), "independent storage proof required");
        var independent = Set.copyOf(proof.members());
        require(independent.contains(reference.wholeItemAccess().orElseThrow().data()), "predicate storage not covered");
        for (var move : port.moves()) require(independent.contains(move.target().wholeItemAccess().orElseThrow().data()), "MOVE storage not covered");
        return new SimpleIf(reference.wholeItemAccess().orElseThrow().data(), thenEntry.orElseThrow(), elseEntry, next.statement().orElseThrow(), independent);
    }
    private static Optional<StatementId> arm(CobolSemanticPort port, IfFact owner, Branch side, IfArm arm, StatementId next) {
        var children = port.children(owner.header().id(), side);
        require(arm.contentAvailability() == Availability.KNOWN && arm.provenance().exact(), "complete arm required");
        if (arm.presence() == ClausePresence.ABSENT) {
            require(side == Branch.ELSE && children.isEmpty() && arm.entry().statement().isEmpty(), "invalid absence");
            return Optional.empty();
        }
        require(arm.presence() == ClausePresence.PRESENT && !children.isEmpty(), "present empty arm unsupported");
        require(arm.entry().availability() == Availability.KNOWN && arm.entry().statement().equals(Optional.of(children.get(0).header().id())), "wrong arm entry");
        for (int i = 0; i < children.size(); i++) {
            require(children.get(i) instanceof MoveFact, "simple arm must contain only MOVE");
            var move = (MoveFact) children.get(i);
            require(move.header().containment().equals(new Containment(Optional.of(owner.header().id()), side)), "arm ownership mismatch");
            var expected = i + 1 < children.size() ? children.get(i + 1).header().id() : next;
            require(move.normalContinuation().availability() == ContinuationAvailability.KNOWN
                    && move.normalContinuation().statement().equals(Optional.of(expected)) && move.normalContinuation().provenance().exact(), "wrong MOVE completion");
            require(move.copySemantics() != CopySemantics.UNAVAILABLE && move.target().wholeItemAccess().isPresent(), "MOVE proof required");
        }
        return arm.entry().statement();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
