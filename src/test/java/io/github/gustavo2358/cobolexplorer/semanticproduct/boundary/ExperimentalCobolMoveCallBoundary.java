package io.github.gustavo2358.cobolexplorer.semanticproduct.boundary;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Test-only A2/B seam for the first MOVE-to-CALL semantic slice.
 *
 * <p>The model is deliberately narrow: it publishes one data item, one literal
 * MOVE, one identifier CALL and their explicit sequencing. It is not a general
 * statement, storage or dataflow model.</p>
 */
public final class ExperimentalCobolMoveCallBoundary {
    private ExperimentalCobolMoveCallBoundary() { }

    public enum BindingStatus { COMPLETE, INCOMPLETE }
    public enum RuntimeTargetKnowledge { UNKNOWN }
    public enum UncertaintyScope { RUNTIME_CALL_TARGET }

    /** Local identity is meaningful only together with its compilation unit. */
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

    /** Typed DATA identity; the local symbol id is never global by itself. */
    public record DataItemId(UnitId unit, int localSymbolId) {
        public DataItemId {
            unit = Objects.requireNonNull(unit, "unit");
            if (localSymbolId < 0)
                throw new IllegalArgumentException("localSymbolId must be non-negative");
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

    /** Localized physical-source provenance, including COPY context. */
    public record Provenance(Location expanded, Location original,
                             List<IncludeFrame> includeChain, boolean exact) {
        public Provenance {
            expanded = Objects.requireNonNull(expanded, "expanded");
            original = Objects.requireNonNull(original, "original");
            includeChain = List.copyOf(includeChain);
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
            value = requireText(value, "value");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** Program point assigned from semantic statement order, not AST node ids. */
    public record ProgramPoint(int ordinal) {
        public ProgramPoint {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal must be non-negative");
        }
    }

    public record MoveFact(ProgramPoint point, LiteralSource source, DataItemId target,
                           BindingStatus targetBinding, Provenance provenance) {
        public MoveFact {
            point = Objects.requireNonNull(point, "point");
            source = Objects.requireNonNull(source, "source");
            target = Objects.requireNonNull(target, "target");
            targetBinding = Objects.requireNonNull(targetBinding, "targetBinding");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record CallFact(ProgramPoint point, DataItemId operand,
                           BindingStatus operandBinding,
                           RuntimeTargetKnowledge runtimeTarget,
                           Provenance provenance) {
        public CallFact {
            point = Objects.requireNonNull(point, "point");
            operand = Objects.requireNonNull(operand, "operand");
            operandBinding = Objects.requireNonNull(operandBinding, "operandBinding");
            runtimeTarget = Objects.requireNonNull(runtimeTarget, "runtimeTarget");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** Explicit relation needed by this slice; it is not a control-flow graph. */
    public record Ordering(ProgramPoint earlier, ProgramPoint later) {
        public Ordering {
            earlier = Objects.requireNonNull(earlier, "earlier");
            later = Objects.requireNonNull(later, "later");
            if (earlier.equals(later) || earlier.ordinal() >= later.ordinal())
                throw new IllegalArgumentException("ordering must be strict and forward");
        }
    }

    public record Uncertainty(ProgramPoint point, UncertaintyScope scope,
                              String code, String detail) {
        public Uncertainty {
            point = Objects.requireNonNull(point, "point");
            scope = Objects.requireNonNull(scope, "scope");
            code = requireText(code, "code");
            detail = requireText(detail, "detail");
        }
    }

    /** Nominal binding and runtime target knowledge remain separate dimensions. */
    public record AnalysisStatus(BindingStatus nominalBinding,
                                  RuntimeTargetKnowledge runtimeTarget,
                                  List<Uncertainty> uncertainties) {
        public AnalysisStatus {
            nominalBinding = Objects.requireNonNull(nominalBinding, "nominalBinding");
            runtimeTarget = Objects.requireNonNull(runtimeTarget, "runtimeTarget");
            uncertainties = List.copyOf(uncertainties);
            if (runtimeTarget == RuntimeTargetKnowledge.UNKNOWN && uncertainties.isEmpty())
                throw new IllegalArgumentException("unknown runtime target must explain uncertainty");
        }
    }

    /** A2: one immutable, materialized publication for the narrow slice. */
    public record State(UnitId unit, List<DataDeclaration> dataItems,
                        MoveFact move, CallFact call, Ordering ordering,
                        AnalysisStatus analysis) {
        public State {
            unit = Objects.requireNonNull(unit, "unit");
            dataItems = List.copyOf(dataItems);
            move = Objects.requireNonNull(move, "move");
            call = Objects.requireNonNull(call, "call");
            ordering = Objects.requireNonNull(ordering, "ordering");
            analysis = Objects.requireNonNull(analysis, "analysis");
            for (DataDeclaration item : dataItems) {
                if (!item.id().unit().equals(unit))
                    throw new IllegalArgumentException("data item belongs to another unit");
            }
            if (!move.target().unit().equals(unit) || !call.operand().unit().equals(unit))
                throw new IllegalArgumentException("statement binding belongs to another unit");
            if (!ordering.earlier().equals(move.point()) || !ordering.later().equals(call.point()))
                throw new IllegalArgumentException("ordering must publish MOVE before CALL");
            for (Uncertainty uncertainty : analysis.uncertainties()) {
                if (!uncertainty.point().equals(call.point()))
                    throw new IllegalArgumentException("uncertainty must be localized to CALL");
            }
        }
    }

    /** B: read-only queries over the materialized A2 state. */
    public interface Port {
        UnitId unit();
        List<DataDeclaration> dataItems();
        MoveFact move();
        CallFact call();
        Ordering ordering();
        AnalysisStatus analysis();
    }

    public static Port open(State state) {
        return new MaterializedPort(Objects.requireNonNull(state, "state"));
    }

    private record MaterializedPort(State state) implements Port {
        private MaterializedPort {
            state = Objects.requireNonNull(state, "state");
        }

        @Override
        public UnitId unit() { return state.unit(); }

        @Override
        public List<DataDeclaration> dataItems() {
            return Collections.unmodifiableList(state.dataItems());
        }

        @Override
        public MoveFact move() { return state.move(); }

        @Override
        public CallFact call() { return state.call(); }

        @Override
        public Ordering ordering() { return state.ordering(); }

        @Override
        public AnalysisStatus analysis() { return state.analysis(); }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
