package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    List<CobolSemanticProduct.StatementId> rootStatements();

    Optional<CobolSemanticProduct.StatementFact> statement(
            CobolSemanticProduct.StatementId id);

    List<CobolSemanticProduct.StatementFact> children(
            CobolSemanticProduct.StatementId parent, CobolSemanticProduct.Branch branch);

    List<CobolSemanticProduct.MoveFact> moves();

    List<CobolSemanticProduct.CallFact> calls();

    List<CobolSemanticProduct.IfFact> ifs();

    List<CobolSemanticProduct.ObservedStatement> observedStatements();

    static CobolSemanticPort open(CobolSemanticProduct.State state) {
        return new MaterializedCobolSemanticPort(Objects.requireNonNull(state, "state"));
    }
}

final class MaterializedCobolSemanticPort implements CobolSemanticPort {
    private final CobolSemanticProduct.State state;
    private final Map<CobolSemanticProduct.StatementId,
            CobolSemanticProduct.StatementFact> statementById;
    private final Map<CobolSemanticProduct.Containment,
            List<CobolSemanticProduct.StatementFact>> childrenByContainment;
    private final List<CobolSemanticProduct.StatementId> rootStatements;
    private final List<CobolSemanticProduct.MoveFact> moves;
    private final List<CobolSemanticProduct.CallFact> calls;
    private final List<CobolSemanticProduct.IfFact> ifs;
    private final List<CobolSemanticProduct.ObservedStatement> observedStatements;

    MaterializedCobolSemanticPort(CobolSemanticProduct.State state) {
        this.state = Objects.requireNonNull(state, "state");

        Map<CobolSemanticProduct.StatementId, CobolSemanticProduct.StatementFact>
                statementsById = new LinkedHashMap<>();
        Map<CobolSemanticProduct.Containment, List<CobolSemanticProduct.StatementFact>>
                children = new LinkedHashMap<>();
        List<CobolSemanticProduct.StatementId> roots = new ArrayList<>();
        List<CobolSemanticProduct.MoveFact> indexedMoves = new ArrayList<>();
        List<CobolSemanticProduct.CallFact> indexedCalls = new ArrayList<>();
        List<CobolSemanticProduct.IfFact> indexedIfs = new ArrayList<>();
        List<CobolSemanticProduct.ObservedStatement> indexedObserved = new ArrayList<>();

        for (CobolSemanticProduct.StatementFact statement : state.statements()) {
            CobolSemanticProduct.StatementHeader header = statement.header();
            statementsById.put(header.id(), statement);
            if (header.containment().branch() == CobolSemanticProduct.Branch.ROOT) {
                roots.add(header.id());
            } else {
                children.computeIfAbsent(header.containment(), ignored -> new ArrayList<>())
                        .add(statement);
            }
            if (statement instanceof CobolSemanticProduct.MoveFact move) {
                indexedMoves.add(move);
            } else if (statement instanceof CobolSemanticProduct.CallFact call) {
                indexedCalls.add(call);
            } else if (statement instanceof CobolSemanticProduct.IfFact branch) {
                indexedIfs.add(branch);
            } else if (statement instanceof CobolSemanticProduct.ObservedStatement observed) {
                indexedObserved.add(observed);
            }
        }

        Map<CobolSemanticProduct.Containment, List<CobolSemanticProduct.StatementFact>>
                immutableChildren = new LinkedHashMap<>();
        children.forEach((containment, facts) ->
                immutableChildren.put(containment, List.copyOf(facts)));
        this.statementById = Collections.unmodifiableMap(statementsById);
        this.childrenByContainment = Collections.unmodifiableMap(immutableChildren);
        this.rootStatements = List.copyOf(roots);
        this.moves = List.copyOf(indexedMoves);
        this.calls = List.copyOf(indexedCalls);
        this.ifs = List.copyOf(indexedIfs);
        this.observedStatements = List.copyOf(indexedObserved);
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

    @Override
    public List<CobolSemanticProduct.StatementId> rootStatements() {
        return rootStatements;
    }

    @Override
    public Optional<CobolSemanticProduct.StatementFact> statement(
            CobolSemanticProduct.StatementId id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(statementById.get(id));
    }

    @Override
    public List<CobolSemanticProduct.StatementFact> children(
            CobolSemanticProduct.StatementId parent, CobolSemanticProduct.Branch branch) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(branch, "branch");
        if (branch == CobolSemanticProduct.Branch.ROOT)
            throw new IllegalArgumentException("children belong to THEN or ELSE");
        CobolSemanticProduct.Containment containment =
                CobolSemanticProduct.Containment.childOf(parent, branch);
        return childrenByContainment.getOrDefault(containment, List.of());
    }

    @Override
    public List<CobolSemanticProduct.MoveFact> moves() {
        return moves;
    }

    @Override
    public List<CobolSemanticProduct.CallFact> calls() {
        return calls;
    }

    @Override
    public List<CobolSemanticProduct.IfFact> ifs() {
        return ifs;
    }

    @Override
    public List<CobolSemanticProduct.ObservedStatement> observedStatements() {
        return observedStatements;
    }
}
