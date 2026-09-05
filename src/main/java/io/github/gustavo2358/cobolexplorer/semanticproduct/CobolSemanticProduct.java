package io.github.gustavo2358.cobolexplorer.semanticproduct;

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
 * Boundary-owned, materialized COBOL semantic state.
 *
 * <p>The state is a closed publication, not a view of frontend products. Its
 * statement inventory is cardinality-independent: a vertical slice controls
 * which fact shapes are modeled, never how many occurrences may be published.
 * Program points and containment are structural anchors, not execution order
 * or CFG edges.</p>
 */
public final class CobolSemanticProduct {
    private CobolSemanticProduct() { }

    public enum ResolutionStatus { RESOLVED, AMBIGUOUS, UNRESOLVED, INPUT_MISSING }

    public enum ResolutionReason {
        UNIQUE_VISIBLE_DECLARATION,
        MULTIPLE_VALID_CANDIDATES,
        DECLARATION_NOT_FOUND,
        INPUT_INCOMPLETE,
        UNSUPPORTED_GRAMMAR_FORM,
        UNSUPPORTED_DIALECT_OPTION,
        INVALID_NAMESPACE_FOR_CONTEXT
    }

    public enum CoverageStatus { MODELED, PARTIAL, UNSUPPORTED, INPUT_MISSING }

    /** Whether the statement inventory itself could be produced. */
    public enum InventoryStatus { COMPLETE, PARTIAL, INPUT_MISSING }

    public enum ReadinessStatus { SUFFICIENT, PARTIAL, BLOCKED, NOT_APPLICABLE }

    public enum Branch { ROOT, THEN, ELSE }

    public enum OperandRole { READ, WRITE, CALL_TARGET }

    public enum CallSyntax { IDENTIFIER_OR_EXPRESSION }

    /** Runtime values are outside nominal binding and this checkpoint. */
    public enum RuntimeTargetKnowledge { UNKNOWN }

    public enum GapScope {
        RUNTIME_CALL_TARGET,
        NOMINAL_BINDING,
        CONDITION_SEMANTICS,
        CAPABILITY,
        ANALYSIS_INPUT
    }

    public enum QualifyMode { STANDARD, EXTEND, UNSPECIFIED }

    public enum PgmnameMode { COMPAT, LONGUPPER, LONGMIXED, UNSPECIFIED }

    public enum DynamMode { DYNAM, NODYNAM, UNSPECIFIED }

    public enum DllMode { DLL, NODLL, UNSPECIFIED }

    /**
     * A unit namespace. Local identities have meaning only with this value and
     * are not persistent identities across edits or contract versions.
     */
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

    /** Nominal DATA identity; it is not a storage identity. */
    public record DataItemId(UnitId unit, int localId) {
        public DataItemId {
            unit = Objects.requireNonNull(unit, "unit");
            if (localId < 0)
                throw new IllegalArgumentException("localId must be non-negative");
        }
    }

    /** Boundary-owned statement occurrence identity. */
    public record StatementId(UnitId unit, int localId) {
        public StatementId {
            unit = Objects.requireNonNull(unit, "unit");
            if (localId < 0)
                throw new IllegalArgumentException("localId must be non-negative");
        }
    }

    /** Operand occurrence identity remains distinct from a selected DATA id. */
    public record OperandId(StatementId statement, int localId) {
        public OperandId {
            statement = Objects.requireNonNull(statement, "statement");
            if (localId < 0)
                throw new IllegalArgumentException("localId must be non-negative");
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

    /** Localized provenance; the frontend SourceMap never crosses the boundary. */
    public record Provenance(Location expanded, Location original,
                             List<IncludeFrame> includeChain, boolean exact) {
        public Provenance {
            expanded = Objects.requireNonNull(expanded, "expanded");
            original = Objects.requireNonNull(original, "original");
            includeChain = List.copyOf(includeChain);
        }
    }

    public record Policy(String policyId, String version, QualifyMode qualifyMode,
                         PgmnameMode pgmnameMode, DynamMode dynamMode, DllMode dllMode) {
        public Policy {
            policyId = requireText(policyId, "policyId");
            version = requireText(version, "version");
            qualifyMode = Objects.requireNonNull(qualifyMode, "qualifyMode");
            pgmnameMode = Objects.requireNonNull(pgmnameMode, "pgmnameMode");
            dynamMode = Objects.requireNonNull(dynamMode, "dynamMode");
            dllMode = Objects.requireNonNull(dllMode, "dllMode");
        }

        public static Policy unspecified() {
            return new Policy("cobol-explorer/explicit-options", "3.0.0",
                    QualifyMode.UNSPECIFIED, PgmnameMode.UNSPECIFIED,
                    DynamMode.UNSPECIFIED, DllMode.UNSPECIFIED);
        }
    }

    public record ReadinessClaim(ReadinessStatus status, String scope) {
        public ReadinessClaim {
            status = Objects.requireNonNull(status, "status");
            scope = requireText(scope, "scope");
        }
    }

    /** Lowering, CFG and effects/dataflow are independent claims. */
    public record Readiness(ReadinessClaim lowering, ReadinessClaim cfg,
                            ReadinessClaim effectsDataflow) {
        public Readiness {
            lowering = Objects.requireNonNull(lowering, "lowering");
            cfg = Objects.requireNonNull(cfg, "cfg");
            effectsDataflow = Objects.requireNonNull(effectsDataflow,
                    "effectsDataflow");
        }
    }

    public record DataDeclaration(DataItemId id, String canonicalName,
                                  Optional<String> picture, Provenance provenance,
                                  CoverageStatus coverage, Readiness readiness) {
        public DataDeclaration {
            id = Objects.requireNonNull(id, "id");
            canonicalName = requireText(canonicalName, "canonicalName");
            picture = Objects.requireNonNull(picture, "picture");
            picture.ifPresent(value -> requireText(value, "picture value"));
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

    /** Nominal binding never contains a runtime value or a fabricated selection. */
    public record NominalBinding(ResolutionStatus status, ResolutionReason reason,
                                 List<DataCandidate> candidates,
                                 Optional<DataItemId> selected) {
        public NominalBinding {
            status = Objects.requireNonNull(status, "status");
            reason = Objects.requireNonNull(reason, "reason");
            candidates = List.copyOf(candidates);
            selected = Objects.requireNonNull(selected, "selected");
            Set<DataItemId> candidateIds = new HashSet<>();
            if (candidates.stream().anyMatch(candidate -> !candidateIds.add(candidate.id())))
                throw new IllegalArgumentException("binding candidates must have unique identities");
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
                        "ambiguous binding must preserve all valid candidates");
            if (status == ResolutionStatus.RESOLVED
                    && reason != ResolutionReason.UNIQUE_VISIBLE_DECLARATION)
                throw new IllegalArgumentException(
                        "resolved binding must retain its unique-selection reason");
            if (status == ResolutionStatus.AMBIGUOUS
                    && reason != ResolutionReason.MULTIPLE_VALID_CANDIDATES)
                throw new IllegalArgumentException(
                        "ambiguous binding must retain its ambiguity reason");
            if (status == ResolutionStatus.INPUT_MISSING
                    && reason != ResolutionReason.INPUT_INCOMPLETE)
                throw new IllegalArgumentException(
                        "input-missing binding must retain its input reason");
        }

        public static NominalBinding resolved(DataItemId id, String canonicalName) {
            DataCandidate candidate = new DataCandidate(id, canonicalName);
            return new NominalBinding(ResolutionStatus.RESOLVED,
                    ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                    List.of(candidate), Optional.of(id));
        }

        public static NominalBinding incomplete(ResolutionStatus status,
                                                ResolutionReason reason,
                                                List<DataCandidate> candidates) {
            if (status == ResolutionStatus.RESOLVED)
                throw new IllegalArgumentException("resolved binding needs a selection");
            return new NominalBinding(status, reason, candidates, Optional.empty());
        }
    }

    /** Structural order only; this is neither execution order nor a CFG node. */
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
            Objects.requireNonNull(parent, "parent");
            if (branch == Branch.ROOT)
                throw new IllegalArgumentException("a child must belong to THEN or ELSE");
            return new Containment(Optional.of(parent), branch);
        }
    }

    public record StatementHeader(StatementId id, ProgramPoint point,
                                  Containment containment, Provenance provenance,
                                  CoverageStatus coverage, Readiness readiness) {
        public StatementHeader {
            id = Objects.requireNonNull(id, "id");
            point = Objects.requireNonNull(point, "point");
            containment = Objects.requireNonNull(containment, "containment");
            provenance = Objects.requireNonNull(provenance, "provenance");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
        }
    }

    public record LiteralSource(OperandId id, String value, Provenance provenance) {
        public LiteralSource {
            id = Objects.requireNonNull(id, "id");
            value = Objects.requireNonNull(value, "value");
            provenance = Objects.requireNonNull(provenance, "provenance");
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

    /**
     * Surface retained for structural IF facts. Predicate normalization remains
     * a later post-binding product; references here are only those already known.
     */
    public record ConditionSurface(String shape, List<DataReference> references,
                                   Provenance provenance) {
        public ConditionSurface {
            shape = requireText(shape, "shape");
            references = List.copyOf(references);
            provenance = Objects.requireNonNull(provenance, "provenance");
            if (references.stream().anyMatch(reference -> reference.role() != OperandRole.READ))
                throw new IllegalArgumentException("condition references must have READ role");
        }
    }

    /** Adding a fact type extends this inventory without changing the State envelope. */
    public sealed interface StatementFact permits MoveFact, CallFact, IfFact,
            ObservedStatement {
        StatementHeader header();
    }

    public record MoveFact(StatementHeader header, LiteralSource source,
                           DataReference target) implements StatementFact {
        public MoveFact {
            header = Objects.requireNonNull(header, "header");
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
            header = Objects.requireNonNull(header, "header");
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

    /**
     * IF owns its condition surface and optional structural continuation.
     * Branch children are the statements whose containment names this IF.
     */
    public record IfFact(StatementHeader header, ConditionSurface condition,
                         Optional<StatementId> continuation) implements StatementFact {
        public IfFact {
            header = Objects.requireNonNull(header, "header");
            condition = Objects.requireNonNull(condition, "condition");
            continuation = Objects.requireNonNull(continuation, "continuation");
        }
    }

    /** A visible statement whose family or shape is not modeled by this capability. */
    public record ObservedStatement(StatementHeader header, String observedKind,
                                    String observedShape,
                                    String gapCode) implements StatementFact {
        public ObservedStatement {
            header = Objects.requireNonNull(header, "header");
            observedKind = requireText(observedKind, "observedKind");
            observedShape = requireText(observedShape, "observedShape");
            gapCode = requireText(gapCode, "gapCode");
            if (header.coverage() == CoverageStatus.MODELED)
                throw new IllegalArgumentException(
                        "an unmodeled statement cannot publish MODELED coverage");
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

    public record CoverageSummary(InventoryStatus inventoryStatus,
                                  int observedStatements, int modeledStatements,
                                  int partialStatements, int unsupportedStatements,
                                  int inputMissingStatements, Readiness readiness) {
        public CoverageSummary {
            inventoryStatus = Objects.requireNonNull(inventoryStatus, "inventoryStatus");
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

    /** One immutable, closed publication with a cardinality-independent envelope. */
    public record State(UnitId unit, Policy policy,
                        List<DataDeclaration> dataDeclarations,
                        List<StatementFact> statements,
                        List<Gap> gaps, CoverageSummary coverage) {
        public State {
            unit = Objects.requireNonNull(unit, "unit");
            policy = Objects.requireNonNull(policy, "policy");
            dataDeclarations = List.copyOf(dataDeclarations);
            statements = List.copyOf(statements);
            gaps = List.copyOf(gaps);
            coverage = Objects.requireNonNull(coverage, "coverage");
            validateState(unit, dataDeclarations, statements, gaps, coverage);
        }
    }

    private static void validateState(UnitId unit, List<DataDeclaration> declarations,
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
        int previousPoint = -1;
        for (StatementFact statement : statements) {
            StatementHeader header = statement.header();
            require(header.id().unit().equals(unit),
                    "statement crossed the unit namespace");
            require(statementById.put(header.id(), statement) == null,
                    "duplicate statement identity");
            require(points.add(header.point().ordinal()), "duplicate program point");
            require(header.point().ordinal() > previousPoint,
                    "statement inventory must follow structural program points");
            previousPoint = header.point().ordinal();
            validateReferences(statement, dataById);
        }
        validateOperandIdentities(statements);
        validateStructure(statementById);

        Map<StatementId, List<Gap>> gapsByStatement = new HashMap<>();
        for (Gap gap : gaps) {
            require(gap.statement().unit().equals(unit),
                    "gap crossed the unit namespace");
            require(statementById.containsKey(gap.statement()),
                    "gap references an unknown statement");
            gapsByStatement.computeIfAbsent(gap.statement(), ignored -> new java.util.ArrayList<>())
                    .add(gap);
        }
        validateLocalizedIncompleteness(statements, gapsByStatement);
        validateCoverage(statements, coverage);
    }

    private static void validateReferences(StatementFact statement,
                                           Map<DataItemId, DataDeclaration> declarations) {
        for (DataReference reference : references(statement)) {
            for (DataCandidate candidate : reference.binding().candidates()) {
                require(candidate.id().unit().equals(statement.header().id().unit()),
                        "binding candidate crossed the statement unit namespace");
                require(declarations.containsKey(candidate.id()),
                        "binding candidate has no declaration in the publication");
                require(declarations.get(candidate.id()).canonicalName()
                                .equals(candidate.canonicalName()),
                        "binding candidate name contradicts its declaration");
            }
        }
    }

    private static List<DataReference> references(StatementFact statement) {
        if (statement instanceof MoveFact move) return List.of(move.target());
        if (statement instanceof CallFact call) return List.of(call.operand());
        if (statement instanceof IfFact branch) return branch.condition().references();
        return List.of();
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
                operands = branch.condition().references().stream()
                        .map(DataReference::id).toList();
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

    private static void validateStructure(Map<StatementId, StatementFact> statements) {
        for (StatementFact statement : statements.values()) {
            StatementHeader header = statement.header();
            header.containment().parent().ifPresent(parentId -> {
                StatementFact parent = statements.get(parentId);
                require(parent instanceof IfFact,
                        "branch parent must be a published IF fact");
                require(parent.header().point().ordinal() < header.point().ordinal(),
                        "branch parent must precede its structural child");
            });
            if (statement instanceof IfFact branch) {
                branch.continuation().ifPresent(continuationId -> {
                    StatementFact continuation = statements.get(continuationId);
                    require(continuation != null,
                            "IF continuation must reference a published statement");
                    require(continuation.header().point().ordinal()
                                    > branch.header().point().ordinal(),
                            "IF continuation must follow its structural program point");
                    require(!isDescendantOf(continuation, branch.header().id(), statements),
                            "IF continuation cannot be contained by that IF");
                });
            }
        }
    }

    private static boolean isDescendantOf(StatementFact statement, StatementId ancestor,
                                          Map<StatementId, StatementFact> statements) {
        Optional<StatementId> parent = statement.header().containment().parent();
        while (parent.isPresent()) {
            if (parent.get().equals(ancestor)) return true;
            StatementFact parentStatement = statements.get(parent.get());
            if (parentStatement == null) return false;
            parent = parentStatement.header().containment().parent();
        }
        return false;
    }

    private static void validateLocalizedIncompleteness(
            List<StatementFact> statements, Map<StatementId, List<Gap>> gaps) {
        for (StatementFact statement : statements) {
            StatementId id = statement.header().id();
            List<Gap> localized = gaps.getOrDefault(id, List.of());
            if (statement.header().coverage() != CoverageStatus.MODELED)
                require(!localized.isEmpty(),
                        "non-modeled statement must retain a localized gap");
            if (statement instanceof CallFact call)
                require(hasGap(localized, GapScope.RUNTIME_CALL_TARGET,
                                call.runtimeUncertaintyCode()),
                        "unknown runtime CALL target must retain its localized gap");
            if (statement instanceof ObservedStatement observed)
                require(hasGap(localized, GapScope.CAPABILITY, observed.gapCode()),
                        "observed unmodeled statement must retain its capability gap");
            if (references(statement).stream()
                    .anyMatch(reference -> reference.binding().status()
                            != ResolutionStatus.RESOLVED)) {
                require(statement.header().coverage() != CoverageStatus.MODELED,
                        "incomplete binding cannot be hidden by MODELED coverage");
                require(localized.stream().anyMatch(gap -> gap.scope()
                                == GapScope.NOMINAL_BINDING),
                        "incomplete binding must retain a nominal binding gap");
            }
        }
    }

    private static boolean hasGap(List<Gap> gaps, GapScope scope, String code) {
        return gaps.stream().anyMatch(gap -> gap.scope() == scope && gap.code().equals(code));
    }

    private static void validateCoverage(List<StatementFact> statements,
                                         CoverageSummary coverage) {
        Map<CoverageStatus, Long> counts = new HashMap<>();
        for (StatementFact statement : statements)
            counts.merge(statement.header().coverage(), 1L, Long::sum);
        require(coverage.observedStatements() == statements.size(),
                "coverage summary omitted observed statements");
        require(coverage.modeledStatements() == count(counts, CoverageStatus.MODELED)
                        && coverage.partialStatements() == count(counts, CoverageStatus.PARTIAL)
                        && coverage.unsupportedStatements()
                        == count(counts, CoverageStatus.UNSUPPORTED)
                        && coverage.inputMissingStatements()
                        == count(counts, CoverageStatus.INPUT_MISSING),
                "coverage summary contradicts individual statement facts");
        validateSummaryClaim(coverage.readiness().lowering(), statements,
                readiness -> readiness.lowering());
        validateSummaryClaim(coverage.readiness().cfg(), statements,
                readiness -> readiness.cfg());
        validateSummaryClaim(coverage.readiness().effectsDataflow(), statements,
                readiness -> readiness.effectsDataflow());
        if (coverage.inventoryStatus() != InventoryStatus.COMPLETE) {
            require(coverage.readiness().lowering().status() != ReadinessStatus.SUFFICIENT
                            && coverage.readiness().cfg().status()
                            != ReadinessStatus.SUFFICIENT
                            && coverage.readiness().effectsDataflow().status()
                            != ReadinessStatus.SUFFICIENT,
                    "incomplete inventory cannot publish sufficient aggregate readiness");
        }
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
                    "summary readiness cannot exceed its weakest statement fact");
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
