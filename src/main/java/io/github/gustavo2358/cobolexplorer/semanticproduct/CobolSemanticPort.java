package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.util.List;
import java.util.Objects;

/**
 * Closed, read-only facade over one already-materialized
 * {@link CobolSemanticProduct.State}.  Queries only select facts; they never
 * perform parsing, resolution, lazy analysis or caching.
 */
public interface CobolSemanticPort {
    CobolSemanticProduct.UnitId unit();

    List<CobolSemanticProduct.DataDeclaration> dataItems();

    CobolSemanticProduct.Policy policy();

    CobolSemanticProduct.MoveFact move();

    CobolSemanticProduct.CallFact call();

    CobolSemanticProduct.Ordering ordering();

    CobolSemanticProduct.AnalysisStatus analysis();

    static CobolSemanticPort open(CobolSemanticProduct.State state) {
        return new MaterializedCobolSemanticPort(Objects.requireNonNull(state, "state"));
    }
}

final class MaterializedCobolSemanticPort implements CobolSemanticPort {
    private final CobolSemanticProduct.State state;

    MaterializedCobolSemanticPort(CobolSemanticProduct.State state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    @Override
    public CobolSemanticProduct.UnitId unit() {
        return state.unit();
    }

    @Override
    public List<CobolSemanticProduct.DataDeclaration> dataItems() {
        return state.dataItems();
    }

    @Override
    public CobolSemanticProduct.Policy policy() {
        return state.policy();
    }

    @Override
    public CobolSemanticProduct.MoveFact move() {
        return state.move();
    }

    @Override
    public CobolSemanticProduct.CallFact call() {
        return state.call();
    }

    @Override
    public CobolSemanticProduct.Ordering ordering() {
        return state.ordering();
    }

    @Override
    public CobolSemanticProduct.AnalysisStatus analysis() {
        return state.analysis();
    }
}
