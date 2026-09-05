package io.github.gustavo2358.cobolexplorer.semanticproduct.targetmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Test-only executable contract for the Semantic Product target model.
 *
 * <p>This type is deliberately disconnected from the current production
 * singleton boundary. It describes the information that later checkpoints
 * must publish; it is neither an Analysis IR nor a production implementation.</p>
 */
public final class SemanticProductTargetModel {
    private SemanticProductTargetModel() { }

    public enum CoverageStatus { MODELED, PARTIAL, UNSUPPORTED, INPUT_MISSING }

    public enum ReadinessStatus { SUFFICIENT, PARTIAL, BLOCKED, NOT_APPLICABLE }

    public enum Branch { ROOT, THEN, ELSE }

    public enum StatementKind { MOVE, CALL, IF, OBSERVED_UNMODELED }

    public enum LiteralKind { ALPHANUMERIC, NUMERIC }

    public enum OperandRole { READ, WRITE, CALL_TARGET }

    public enum ResolutionStatus { RESOLVED, AMBIGUOUS, UNRESOLVED, INPUT_MISSING }

    public enum CallSyntax { IDENTIFIER_OR_EXPRESSION, LITERAL_PROGRAM_NAME }

    public enum RuntimeTargetKnowledge { UNKNOWN }

    public enum RelationalOperator { EQUALS }

    public enum GapScope { RUNTIME_CALL_TARGET, CONDITION_SEMANTICS, CAPABILITY }

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

    /** A local DATA id has meaning only inside its complete unit namespace. */
    public record DataItemId(UnitId unit, int localId) {
        public DataItemId {
            unit = Objects.requireNonNull(unit, "unit");
            if (localId < 0) throw new IllegalArgumentException("localId must be non-negative");
        }
    }

    /** A local statement id has meaning only inside its complete unit namespace. */
    public record StatementId(UnitId unit, int localId) {
        public StatementId {
            unit = Objects.requireNonNull(unit, "unit");
            if (localId < 0) throw new IllegalArgumentException("localId must be non-negative");
        }
    }

    /** Operand occurrence identity is distinct from its nominal DATA binding. */
    public record OperandId(StatementId statement, int localId) {
        public OperandId {
            statement = Objects.requireNonNull(statement, "statement");
            if (localId < 0) throw new IllegalArgumentException("localId must be non-negative");
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

    public record Provenance(Location expanded, Location original,
                             List<IncludeFrame> includeChain, boolean exact) {
        public Provenance {
            expanded = Objects.requireNonNull(expanded, "expanded");
            original = Objects.requireNonNull(original, "original");
            includeChain = List.copyOf(includeChain);
        }
    }

    public record ReadinessClaim(ReadinessStatus status, String scope) {
        public ReadinessClaim {
            status = Objects.requireNonNull(status, "status");
            scope = requireText(scope, "scope");
        }
    }

    /** The three downstream dimensions are independent claims. */
    public record Readiness(ReadinessClaim lowering, ReadinessClaim cfg,
                            ReadinessClaim effectsDataflow) {
        public Readiness {
            lowering = Objects.requireNonNull(lowering, "lowering");
            cfg = Objects.requireNonNull(cfg, "cfg");
            effectsDataflow = Objects.requireNonNull(effectsDataflow,
                    "effectsDataflow");
        }
    }

    public record DataDeclaration(DataItemId id, String canonicalName, String picture,
                                  Provenance provenance, CoverageStatus coverage,
                                  Readiness readiness) {
        public DataDeclaration {
            id = Objects.requireNonNull(id, "id");
            canonicalName = requireText(canonicalName, "canonicalName");
            picture = requireText(picture, "picture");
            provenance = Objects.requireNonNull(provenance, "provenance");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
        }
    }

    public record DataCandidate(DataItemId id, String canonicalName) {
        public DataCandidate {
            id = Objects.requireNonNull(id, "id");
            canonicalName = requireText(canonicalName, "canonicalName");
        }
    }

    /** Nominal binding never contains a runtime value. */
    public record NominalBinding(ResolutionStatus status, String reason,
                                 List<DataCandidate> candidates,
                                 Optional<DataItemId> selected) {
        public NominalBinding {
            status = Objects.requireNonNull(status, "status");
            reason = requireText(reason, "reason");
            candidates = List.copyOf(candidates);
            selected = Objects.requireNonNull(selected, "selected");
            if (status == ResolutionStatus.RESOLVED) {
                if (candidates.size() != 1 || selected.isEmpty()
                        || !selected.get().equals(candidates.get(0).id()))
                    throw new IllegalArgumentException(
                            "resolved binding must select its only candidate");
            } else if (selected.isPresent()) {
                throw new IllegalArgumentException(
                        "non-resolved binding cannot select a candidate");
            }
            if (status == ResolutionStatus.AMBIGUOUS && candidates.size() < 2)
                throw new IllegalArgumentException(
                        "ambiguous binding must retain every valid candidate");
        }

        public static NominalBinding resolved(DataItemId id, String canonicalName) {
            DataCandidate candidate = new DataCandidate(id, canonicalName);
            return new NominalBinding(ResolutionStatus.RESOLVED,
                    "UNIQUE_VISIBLE_DECLARATION", List.of(candidate), Optional.of(id));
        }
    }

    public record DataReference(OperandId id, OperandRole role,
                                NominalBinding binding, Provenance provenance) {
        public DataReference {
            id = Objects.requireNonNull(id, "id");
            role = Objects.requireNonNull(role, "role");
            binding = Objects.requireNonNull(binding, "binding");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record LiteralSource(OperandId id, LiteralKind kind, String value,
                                Provenance provenance) {
        public LiteralSource {
            id = Objects.requireNonNull(id, "id");
            kind = Objects.requireNonNull(kind, "kind");
            value = requireText(value, "value");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** Structural order only; this is not execution order or a CFG node. */
    public record ProgramPoint(int ordinal) {
        public ProgramPoint {
            if (ordinal < 0)
                throw new IllegalArgumentException("ordinal must be non-negative");
        }
    }

    public record Containment(Optional<StatementId> parent, Branch branch) {
        public Containment {
            parent = Objects.requireNonNull(parent, "parent");
            branch = Objects.requireNonNull(branch, "branch");
            if ((branch == Branch.ROOT) != parent.isEmpty())
                throw new IllegalArgumentException(
                        "only root statements may omit a parent");
        }

        public static Containment root() {
            return new Containment(Optional.empty(), Branch.ROOT);
        }

        public static Containment childOf(StatementId parent, Branch branch) {
            if (branch == Branch.ROOT)
                throw new IllegalArgumentException("a child must belong to THEN or ELSE");
            return new Containment(Optional.of(parent), branch);
        }
    }

    public record StatementHeader(StatementId id, StatementKind kind, ProgramPoint point,
                                  Containment containment, Provenance provenance,
                                  CoverageStatus coverage, Readiness readiness) {
        public StatementHeader {
            id = Objects.requireNonNull(id, "id");
            kind = Objects.requireNonNull(kind, "kind");
            point = Objects.requireNonNull(point, "point");
            containment = Objects.requireNonNull(containment, "containment");
            provenance = Objects.requireNonNull(provenance, "provenance");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
        }
    }

    public sealed interface StatementFact permits MoveFact, CallFact, IfFact,
            ObservedStatementFact {
        StatementHeader header();
    }

    public record MoveFact(StatementHeader header, LiteralSource source,
                           DataReference target) implements StatementFact {
        public MoveFact {
            header = requireKind(header, StatementKind.MOVE);
            source = Objects.requireNonNull(source, "source");
            target = Objects.requireNonNull(target, "target");
            if (target.role() != OperandRole.WRITE)
                throw new IllegalArgumentException("MOVE target must have WRITE role");
        }
    }

    public record CallFact(StatementHeader header, CallSyntax syntax,
                           DataReference operand,
                           RuntimeTargetKnowledge runtimeTarget,
                           String runtimeUncertaintyCode) implements StatementFact {
        public CallFact {
            header = requireKind(header, StatementKind.CALL);
            syntax = Objects.requireNonNull(syntax, "syntax");
            operand = Objects.requireNonNull(operand, "operand");
            runtimeTarget = Objects.requireNonNull(runtimeTarget, "runtimeTarget");
            runtimeUncertaintyCode = requireText(runtimeUncertaintyCode,
                    "runtimeUncertaintyCode");
            if (syntax != CallSyntax.IDENTIFIER_OR_EXPRESSION)
                throw new IllegalArgumentException(
                        "typed CallFact covers identifier/expression syntax only");
            if (operand.role() != OperandRole.CALL_TARGET)
                throw new IllegalArgumentException(
                        "CALL operand must have CALL_TARGET role");
        }
    }

    public record ConditionFact(String surface, DataReference subject,
                                RelationalOperator operator, LiteralSource object,
                                Provenance provenance) {
        public ConditionFact {
            surface = requireText(surface, "surface");
            subject = Objects.requireNonNull(subject, "subject");
            operator = Objects.requireNonNull(operator, "operator");
            object = Objects.requireNonNull(object, "object");
            provenance = Objects.requireNonNull(provenance, "provenance");
            if (subject.role() != OperandRole.READ)
                throw new IllegalArgumentException(
                        "condition subject must have READ role");
        }

        public List<DataReference> references() {
            return List.of(subject);
        }
    }

    /**
     * Branch membership and continuation are structural. No truth,
     * reachability, branch probability or CFG edge is represented here.
     */
    public record IfFact(StatementHeader header, ConditionFact condition,
                         List<StatementId> thenChildren,
                         List<StatementId> elseChildren,
                         StatementId continuation) implements StatementFact {
        public IfFact {
            header = requireKind(header, StatementKind.IF);
            condition = Objects.requireNonNull(condition, "condition");
            thenChildren = List.copyOf(thenChildren);
            elseChildren = List.copyOf(elseChildren);
            continuation = Objects.requireNonNull(continuation, "continuation");
        }
    }

    /** A visible statement whose family or shape is not fully modeled yet. */
    public record ObservedStatementFact(StatementHeader header, String observedKind,
                                        String observedShape,
                                        String gapCode) implements StatementFact {
        public ObservedStatementFact {
            header = requireKind(header, StatementKind.OBSERVED_UNMODELED);
            observedKind = requireText(observedKind, "observedKind");
            observedShape = requireText(observedShape, "observedShape");
            gapCode = requireText(gapCode, "gapCode");
            if (header.coverage() == CoverageStatus.MODELED)
                throw new IllegalArgumentException(
                        "unmodeled fact cannot publish MODELED coverage");
        }
    }

    public record Gap(StatementId statement, GapScope scope, String code,
                      String detail, Provenance provenance) {
        public Gap {
            statement = Objects.requireNonNull(statement, "statement");
            scope = Objects.requireNonNull(scope, "scope");
            code = requireText(code, "code");
            detail = requireText(detail, "detail");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record CoverageSummary(int observedStatements, int modeledStatements,
                                  int partialStatements, int unsupportedStatements,
                                  int inputMissingStatements, Readiness readiness) {
        public CoverageSummary {
            if (observedStatements < 0 || modeledStatements < 0 || partialStatements < 0
                    || unsupportedStatements < 0 || inputMissingStatements < 0)
                throw new IllegalArgumentException("coverage counts must be non-negative");
            if (observedStatements != modeledStatements + partialStatements
                    + unsupportedStatements + inputMissingStatements)
                throw new IllegalArgumentException(
                        "coverage summary must classify every observed statement");
            readiness = Objects.requireNonNull(readiness, "readiness");
        }
    }

    /** One immutable target publication with an extensible statement inventory. */
    public record State(UnitId unit, List<DataDeclaration> dataDeclarations,
                        List<StatementId> rootStatements,
                        List<StatementFact> statements, List<Gap> gaps,
                        CoverageSummary coverage) {
        public State {
            unit = Objects.requireNonNull(unit, "unit");
            dataDeclarations = List.copyOf(dataDeclarations);
            rootStatements = List.copyOf(rootStatements);
            statements = List.copyOf(statements);
            gaps = List.copyOf(gaps);
            coverage = Objects.requireNonNull(coverage, "coverage");
            validateState(unit, dataDeclarations, rootStatements, statements, gaps, coverage);
        }
    }

    /** Closed, read-only target port; all statement families remain plural. */
    public interface Port {
        UnitId unit();

        List<DataDeclaration> dataDeclarations();

        List<StatementId> rootStatements();

        List<StatementFact> statements();

        List<Gap> gaps();

        CoverageSummary coverage();

        default List<MoveFact> moves() {
            return statements().stream().filter(MoveFact.class::isInstance)
                    .map(MoveFact.class::cast).toList();
        }

        default List<CallFact> calls() {
            return statements().stream().filter(CallFact.class::isInstance)
                    .map(CallFact.class::cast).toList();
        }

        default List<IfFact> ifs() {
            return statements().stream().filter(IfFact.class::isInstance)
                    .map(IfFact.class::cast).toList();
        }
    }

    public static Port open(State state) {
        return new MaterializedPort(Objects.requireNonNull(state, "state"));
    }

    private record MaterializedPort(State state) implements Port {
        @Override
        public UnitId unit() { return state.unit(); }

        @Override
        public List<DataDeclaration> dataDeclarations() { return state.dataDeclarations(); }

        @Override
        public List<StatementId> rootStatements() { return state.rootStatements(); }

        @Override
        public List<StatementFact> statements() { return state.statements(); }

        @Override
        public List<Gap> gaps() { return state.gaps(); }

        @Override
        public CoverageSummary coverage() { return state.coverage(); }
    }

    private static void validateState(UnitId unit, List<DataDeclaration> declarations,
                                      List<StatementId> roots,
                                      List<StatementFact> statements, List<Gap> gaps,
                                      CoverageSummary coverage) {
        Map<DataItemId, DataDeclaration> dataById = new LinkedHashMap<>();
        for (DataDeclaration declaration : declarations) {
            require(declaration.id().unit().equals(unit),
                    "DATA declaration crossed the unit namespace");
            require(dataById.put(declaration.id(), declaration) == null,
                    "duplicate DATA identity");
        }

        Map<StatementId, StatementFact> statementById = new LinkedHashMap<>();
        Set<Integer> points = new HashSet<>();
        for (StatementFact statement : statements) {
            StatementHeader header = statement.header();
            require(header.id().unit().equals(unit),
                    "statement crossed the unit namespace");
            require(statementById.put(header.id(), statement) == null,
                    "duplicate statement identity");
            require(points.add(header.point().ordinal()), "duplicate program point");
            validateReferences(statement, dataById);
        }
        validateOperandIdentities(statements);

        Map<StatementId, Integer> memberships = new HashMap<>();
        for (StatementId root : roots) {
            StatementFact statement = requireStatement(statementById, root);
            require(statement.header().containment().equals(Containment.root()),
                    "root inventory disagrees with statement containment");
            memberships.merge(root, 1, Integer::sum);
        }
        for (StatementFact statement : statements) {
            if (!(statement instanceof IfFact branch)) continue;
            validateChildren(statementById, memberships, branch.header().id(),
                    Branch.THEN, branch.thenChildren());
            validateChildren(statementById, memberships, branch.header().id(),
                    Branch.ELSE, branch.elseChildren());
            StatementFact continuation = requireStatement(statementById, branch.continuation());
            require(continuation.header().point().ordinal() > branch.header().point().ordinal(),
                    "IF continuation must follow its structural program point");
        }
        require(memberships.size() == statements.size()
                        && memberships.values().stream().allMatch(count -> count == 1),
                "every observed statement must appear exactly once in the structure");

        Map<StatementId, List<Gap>> gapsByStatement = new HashMap<>();
        for (Gap gap : gaps) {
            require(gap.statement().unit().equals(unit), "gap crossed the unit namespace");
            requireStatement(statementById, gap.statement());
            gapsByStatement.computeIfAbsent(gap.statement(), ignored -> new ArrayList<>())
                    .add(gap);
        }
        for (StatementFact statement : statements) {
            if (statement instanceof CallFact call)
                require(hasGap(gapsByStatement, call.header().id(),
                                call.runtimeUncertaintyCode(), GapScope.RUNTIME_CALL_TARGET),
                        "unknown runtime CALL target must retain its localized gap");
            if (statement instanceof IfFact branch
                    && branch.header().coverage() == CoverageStatus.PARTIAL)
                require(gapsByStatement.getOrDefault(branch.header().id(), List.of()).stream()
                                .anyMatch(gap -> gap.scope() == GapScope.CONDITION_SEMANTICS),
                        "partial IF condition must retain its localized semantics gap");
            if (statement instanceof ObservedStatementFact observed)
                require(hasGap(gapsByStatement, observed.header().id(),
                                observed.gapCode(), GapScope.CAPABILITY),
                        "unmodeled statement must retain its localized capability gap");
        }

        Map<CoverageStatus, Long> actualCoverage = new HashMap<>();
        for (StatementFact statement : statements)
            actualCoverage.merge(statement.header().coverage(), 1L, Long::sum);
        require(coverage.observedStatements() == statements.size(),
                "coverage summary omitted observed statements");
        require(coverage.modeledStatements() == count(actualCoverage, CoverageStatus.MODELED)
                        && coverage.partialStatements()
                        == count(actualCoverage, CoverageStatus.PARTIAL)
                        && coverage.unsupportedStatements()
                        == count(actualCoverage, CoverageStatus.UNSUPPORTED)
                        && coverage.inputMissingStatements()
                        == count(actualCoverage, CoverageStatus.INPUT_MISSING),
                "coverage summary exceeds or contradicts individual facts");
        validateSummaryReadiness(coverage.readiness(), statements);
    }

    private static void validateReferences(StatementFact statement,
                                           Map<DataItemId, DataDeclaration> declarations) {
        List<DataReference> references;
        if (statement instanceof MoveFact move) {
            references = List.of(move.target());
        } else if (statement instanceof CallFact call) {
            references = List.of(call.operand());
        } else if (statement instanceof IfFact branch) {
            references = branch.condition().references();
        } else {
            references = List.of();
        }
        for (DataReference reference : references) {
            require(reference.binding().candidates().stream()
                            .allMatch(candidate -> declarations.containsKey(candidate.id())),
                    "binding candidate has no declaration in the publication");
        }
    }

    private static void validateOperandIdentities(List<StatementFact> statements) {
        Set<OperandId> identities = new HashSet<>();
        for (StatementFact statement : statements) {
            List<OperandId> operands;
            if (statement instanceof MoveFact move) {
                operands = List.of(move.source().id(), move.target().id());
            } else if (statement instanceof CallFact call) {
                operands = List.of(call.operand().id());
            } else if (statement instanceof IfFact branch) {
                operands = List.of(branch.condition().subject().id(),
                        branch.condition().object().id());
            } else {
                operands = List.of();
            }
            for (OperandId operand : operands) {
                require(operand.statement().equals(statement.header().id()),
                        "operand identity belongs to another statement");
                require(identities.add(operand), "duplicate operand identity");
            }
        }
    }

    private static void validateChildren(Map<StatementId, StatementFact> statements,
                                         Map<StatementId, Integer> memberships,
                                         StatementId parent, Branch branch,
                                         List<StatementId> children) {
        Set<StatementId> local = new HashSet<>();
        for (StatementId child : children) {
            require(local.add(child), "branch contains duplicate child identity");
            StatementFact fact = requireStatement(statements, child);
            require(fact.header().containment().equals(Containment.childOf(parent, branch)),
                    "branch membership disagrees with child containment");
            memberships.merge(child, 1, Integer::sum);
        }
    }

    private static void validateSummaryReadiness(Readiness summary,
                                                 List<StatementFact> statements) {
        validateSummaryClaim(summary.lowering(), statements,
                readiness -> readiness.lowering());
        validateSummaryClaim(summary.cfg(), statements, readiness -> readiness.cfg());
        validateSummaryClaim(summary.effectsDataflow(), statements,
                readiness -> readiness.effectsDataflow());
    }

    private static void validateSummaryClaim(ReadinessClaim summary,
                                             List<StatementFact> statements,
                                             Function<Readiness, ReadinessClaim> dimension) {
        int weakest = statements.stream().map(StatementFact::header)
                .map(StatementHeader::readiness).map(dimension)
                .mapToInt(claim -> readinessRank(claim.status()))
                .filter(rank -> rank >= 0).min().orElse(-1);
        if (weakest >= 0)
            require(readinessRank(summary.status()) <= weakest,
                    "summary readiness cannot exceed its weakest individual fact");
    }

    private static int readinessRank(ReadinessStatus status) {
        return switch (status) {
            case BLOCKED -> 0;
            case PARTIAL -> 1;
            case SUFFICIENT -> 2;
            case NOT_APPLICABLE -> -1;
        };
    }

    private static long count(Map<CoverageStatus, Long> counts, CoverageStatus status) {
        return counts.getOrDefault(status, 0L);
    }

    private static boolean hasGap(Map<StatementId, List<Gap>> gaps,
                                  StatementId statement, String code, GapScope scope) {
        return gaps.getOrDefault(statement, List.of()).stream()
                .anyMatch(gap -> gap.code().equals(code) && gap.scope() == scope);
    }

    private static StatementFact requireStatement(Map<StatementId, StatementFact> statements,
                                                  StatementId id) {
        StatementFact statement = statements.get(id);
        if (statement == null)
            throw new IllegalArgumentException("structure references an unknown statement");
        return statement;
    }

    private static StatementHeader requireKind(StatementHeader header, StatementKind kind) {
        header = Objects.requireNonNull(header, "header");
        if (header.kind() != kind)
            throw new IllegalArgumentException("statement header kind does not match fact type");
        return header;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
