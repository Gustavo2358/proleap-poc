package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Closed, read-only facade over one already-materialized publication. Queries
 * only select boundary facts; they perform no parsing, resolution or analysis.
 */
public interface CobolSemanticPort {
    CobolSemanticProduct.UnitId unit();

    CobolSemanticProduct.Policy policy();

    List<CobolSemanticProduct.DataDeclaration> dataDeclarations();

    List<CobolSemanticProduct.StatementFact> statements();

    List<CobolSemanticProduct.Gap> gaps();

    CobolSemanticProduct.CoverageSummary coverage();

    default List<CobolSemanticProduct.StatementId> rootStatements() {
        return statements().stream()
                .filter(statement -> statement.header().containment()
                        .equals(CobolSemanticProduct.Containment.root()))
                .map(statement -> statement.header().id()).toList();
    }

    default Optional<CobolSemanticProduct.StatementFact> statement(
            CobolSemanticProduct.StatementId id) {
        Objects.requireNonNull(id, "id");
        return statements().stream().filter(fact -> fact.header().id().equals(id)).findFirst();
    }

    default List<CobolSemanticProduct.StatementFact> children(
            CobolSemanticProduct.StatementId parent, CobolSemanticProduct.Branch branch) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(branch, "branch");
        if (branch == CobolSemanticProduct.Branch.ROOT)
            throw new IllegalArgumentException("children belong to THEN or ELSE");
        CobolSemanticProduct.Containment containment =
                CobolSemanticProduct.Containment.childOf(parent, branch);
        return statements().stream()
                .filter(statement -> statement.header().containment().equals(containment))
                .toList();
    }

    default List<CobolSemanticProduct.MoveFact> moves() {
        return statements().stream().filter(CobolSemanticProduct.MoveFact.class::isInstance)
                .map(CobolSemanticProduct.MoveFact.class::cast).toList();
    }

    default List<CobolSemanticProduct.CallFact> calls() {
        return statements().stream().filter(CobolSemanticProduct.CallFact.class::isInstance)
                .map(CobolSemanticProduct.CallFact.class::cast).toList();
    }

    default List<CobolSemanticProduct.IfFact> ifs() {
        return statements().stream().filter(CobolSemanticProduct.IfFact.class::isInstance)
                .map(CobolSemanticProduct.IfFact.class::cast).toList();
    }

    default List<CobolSemanticProduct.ObservedStatement> observedStatements() {
        return statements().stream()
                .filter(CobolSemanticProduct.ObservedStatement.class::isInstance)
                .map(CobolSemanticProduct.ObservedStatement.class::cast).toList();
    }

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
    public CobolSemanticProduct.Policy policy() {
        return state.policy();
    }

    @Override
    public List<CobolSemanticProduct.DataDeclaration> dataDeclarations() {
        return state.dataDeclarations();
    }

    @Override
    public List<CobolSemanticProduct.StatementFact> statements() {
        return state.statements();
    }

    @Override
    public List<CobolSemanticProduct.Gap> gaps() {
        return state.gaps();
    }

    @Override
    public CobolSemanticProduct.CoverageSummary coverage() {
        return state.coverage();
    }
}
