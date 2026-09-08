package io.github.gustavo2358.cobolexplorer.semanticproduct.scalar;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;
import java.util.*;

/** Independent consumer: no source, implementation metadata or readiness input. */
public final class ScalarMoveOracle {
    private ScalarMoveOracle() { }
    public record Definition(StatementId move, DataItemId destination, String value,
                             int extent, StatementId continuation) { }

    public static Definition read(CobolSemanticPort port, StatementId id) {
        Map<DataItemId, DataDeclaration> data = new HashMap<>();
        for (var d : port.dataDeclarations()) check(data.put(d.id(), d) == null, "duplicate DATA");
        var move = (MoveFact) port.statement(id).orElseThrow();
        check(move.copySemantics() == CopySemantics.FULL_IDENTITY, "copy proof missing");
        check(move.source().kind() == LiteralKind.ALPHANUMERIC, "literal category");
        var text = move.source().logicalValue().orElseThrow();
        check(text.logicalDomain() == LogicalDomain.TEXT, "literal domain");
        var access = move.target().wholeItemAccess().orElseThrow();
        check(move.target().role() == OperandRole.WRITE, "write role");
        check(move.target().binding().status() == ResolutionStatus.RESOLVED, "unique binding");
        check(move.target().binding().selected().equals(Optional.of(access.data())), "selected target");
        var declaration = Objects.requireNonNull(data.get(access.data()), "DATA missing");
        var shape = declaration.scalarText().orElseThrow();
        check(shape.logicalDomain() == LogicalDomain.TEXT, "DATA domain");
        check(shape.storageClass() == StorageClass.WORKING_STORAGE, "storage class");
        check(shape.declarationScope() == DeclarationScope.LOCAL, "local declaration");
        check(shape.logicalExtent() == text.logicalExtent(), "length mismatch");
        var next = move.normalContinuation();
        check(next.availability() == ContinuationAvailability.KNOWN, "continuation missing");
        StatementId target = next.statement().orElseThrow();
        var goback = (GobackFact) port.statement(target).orElseThrow();
        check(goback.exit() == GobackExit.CURRENT_PROGRAM_INVOCATION, "exit scope");
        check(goback.localContinuation() == LocalContinuation.NONE, "local successor");
        for (var origin : List.of(declaration.provenance(), move.header().provenance(),
                move.source().provenance(), move.target().provenance(), next.provenance(),
                goback.header().provenance(), port.entries().get(0).provenance())) {
            check(origin.original().startLine() > 0 && origin.expanded().startLine() > 0, "provenance missing");
        }
        check(port.entries().get(0).start().statement().equals(Optional.of(id)), "entry start");
        check(port.entryInventory().status() == InventoryStatus.PARTIAL, "entry inventory must stay open");
        return new Definition(id, declaration.id(), text.value(), shape.logicalExtent(), target);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalArgumentException(message);
    }
}
