package io.github.gustavo2358.cobolexplorer.semanticproduct.consumer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Production-like consumer that audits whether one closed Semantic Product
 * publication contains the inputs declared for future lowering.
 *
 * <p>The consumer receives only {@link CobolSemanticPort}. It copies facts and
 * readiness claims into an immutable audit; it does not parse COBOL, resolve
 * names, infer runtime values, build IR/CFG, or improve an incomplete claim.</p>
 */
public final class CobolLoweringReadinessConsumer {
    private CobolLoweringReadinessConsumer() { }

    public enum StatementFamily { MOVE, CALL, IF, OBSERVED, GOBACK }

    public record GobackAudit(StatementHeaderAudit header, CobolSemanticProduct.GobackExit exit,
                              CobolSemanticProduct.LocalContinuation localContinuation) implements StatementAudit {
        @Override public StatementFamily family() { return StatementFamily.GOBACK; }
    }

    public record PolicyAudit(String policyId, String version,
                              CobolSemanticProduct.QualifyMode qualifyMode,
                              CobolSemanticProduct.PgmnameMode pgmnameMode,
                              CobolSemanticProduct.DynamMode dynamMode,
                              CobolSemanticProduct.DllMode dllMode) {
        public PolicyAudit {
            policyId = requireText(policyId, "policyId");
            version = requireText(version, "version");
            qualifyMode = Objects.requireNonNull(qualifyMode, "qualifyMode");
            pgmnameMode = Objects.requireNonNull(pgmnameMode, "pgmnameMode");
            dynamMode = Objects.requireNonNull(dynamMode, "dynamMode");
            dllMode = Objects.requireNonNull(dllMode, "dllMode");
        }
    }

    /** Localized source evidence copied from boundary-owned provenance. */
    public record ProvenanceAudit(CobolSemanticProduct.Location expanded,
                                  CobolSemanticProduct.Location original,
                                  List<CobolSemanticProduct.IncludeFrame> includeChain,
                                  boolean exact) {
        public ProvenanceAudit {
            expanded = Objects.requireNonNull(expanded, "expanded");
            original = Objects.requireNonNull(original, "original");
            includeChain = List.copyOf(includeChain);
        }
    }

    /** One readiness dimension, copied without promotion or normalization. */
    public record DimensionAudit(CobolSemanticProduct.ReadinessStatus status,
                                 String scope) {
        public DimensionAudit {
            status = Objects.requireNonNull(status, "status");
            scope = requireText(scope, "scope");
        }
    }

    /** Lowering, CFG and effects/dataflow remain three independent claims. */
    public record ReadinessAudit(DimensionAudit lowering, DimensionAudit cfg,
                                 DimensionAudit effectsDataflow) {
        public ReadinessAudit {
            lowering = Objects.requireNonNull(lowering, "lowering");
            cfg = Objects.requireNonNull(cfg, "cfg");
            effectsDataflow = Objects.requireNonNull(effectsDataflow,
                    "effectsDataflow");
        }
    }

    public record CandidateAudit(CobolSemanticProduct.DataItemId id,
                                 String canonicalName) {
        public CandidateAudit {
            id = Objects.requireNonNull(id, "id");
            canonicalName = requireText(canonicalName, "canonicalName");
        }
    }

    public record BindingAudit(CobolSemanticProduct.ResolutionStatus status,
                               CobolSemanticProduct.ResolutionReason reason,
                               List<CandidateAudit> candidates,
                               Optional<CobolSemanticProduct.DataItemId> selected) {
        public BindingAudit {
            status = Objects.requireNonNull(status, "status");
            reason = Objects.requireNonNull(reason, "reason");
            candidates = List.copyOf(candidates);
            selected = Objects.requireNonNull(selected, "selected");
        }
    }

    public record DataAudit(CobolSemanticProduct.DataItemId id,
                            String canonicalName, Optional<String> picture,
                            ProvenanceAudit provenance,
                            CobolSemanticProduct.CoverageStatus coverage,
                            ReadinessAudit readiness) {
        public DataAudit {
            id = Objects.requireNonNull(id, "id");
            canonicalName = requireText(canonicalName, "canonicalName");
            picture = Objects.requireNonNull(picture, "picture");
            provenance = Objects.requireNonNull(provenance, "provenance");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
        }
    }

    public sealed interface MoveSourceAudit permits LiteralAudit, DataReferenceAudit { }

    public record LiteralAudit(CobolSemanticProduct.OperandId id,
                               CobolSemanticProduct.LiteralKind kind,
                               String value, ProvenanceAudit provenance) implements MoveSourceAudit {
        public LiteralAudit {
            id = Objects.requireNonNull(id, "id");
            kind = Objects.requireNonNull(kind, "kind");
            value = Objects.requireNonNull(value, "value");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record DataReferenceAudit(CobolSemanticProduct.OperandId id,
                                     CobolSemanticProduct.OperandRole role,
                                     BindingAudit binding,
                                     ProvenanceAudit provenance) implements MoveSourceAudit {
        public DataReferenceAudit {
            id = Objects.requireNonNull(id, "id");
            role = Objects.requireNonNull(role, "role");
            binding = Objects.requireNonNull(binding, "binding");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record GapAudit(CobolSemanticProduct.StatementId statement,
                           CobolSemanticProduct.GapScope scope,
                           String code, String detail,
                           ProvenanceAudit provenance) {
        public GapAudit {
            statement = Objects.requireNonNull(statement, "statement");
            scope = Objects.requireNonNull(scope, "scope");
            code = requireText(code, "code");
            detail = requireText(detail, "detail");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record ContainmentAudit(Optional<CobolSemanticProduct.StatementId> parent,
                                   CobolSemanticProduct.Branch branch) {
        public ContainmentAudit {
            parent = Objects.requireNonNull(parent, "parent");
            branch = Objects.requireNonNull(branch, "branch");
        }
    }

    public record StatementHeaderAudit(CobolSemanticProduct.StatementId id,
                                       int programPoint,
                                       ContainmentAudit containment,
                                       ProvenanceAudit provenance,
                                       CobolSemanticProduct.CoverageStatus coverage,
                                       ReadinessAudit readiness,
                                       List<GapAudit> gaps) {
        public StatementHeaderAudit {
            id = Objects.requireNonNull(id, "id");
            if (programPoint < 0)
                throw new IllegalArgumentException("programPoint must be non-negative");
            containment = Objects.requireNonNull(containment, "containment");
            provenance = Objects.requireNonNull(provenance, "provenance");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
            gaps = List.copyOf(gaps);
        }
    }

    /**
     * Extensible audit envelope. The mapper fails clearly for a new production
     * fact family until its dedicated audit record is added here.
     */
    public interface StatementAudit {
        StatementHeaderAudit header();
        StatementFamily family();
    }

    public record MoveAudit(StatementHeaderAudit header, MoveSourceAudit source,
                            DataReferenceAudit target) implements StatementAudit {
        public MoveAudit {
            header = Objects.requireNonNull(header, "header");
            source = Objects.requireNonNull(source, "source");
            target = Objects.requireNonNull(target, "target");
        }

        @Override public StatementFamily family() { return StatementFamily.MOVE; }
    }

    public record CallAudit(StatementHeaderAudit header,
                            CobolSemanticProduct.CallSyntax syntax,
                            CobolSemanticProduct.CallTarget target,
                            CobolSemanticProduct.RuntimeTargetKnowledge runtimeTarget,
                            String runtimeUncertaintyCode) implements StatementAudit {
        public CallAudit {
            header = Objects.requireNonNull(header, "header");
            syntax = Objects.requireNonNull(syntax, "syntax");
            target = Objects.requireNonNull(target, "target");
            runtimeTarget = Objects.requireNonNull(runtimeTarget, "runtimeTarget");
            runtimeUncertaintyCode = requireText(runtimeUncertaintyCode,
                    "runtimeUncertaintyCode");
        }

        @Override public StatementFamily family() { return StatementFamily.CALL; }
    }

    public record ConditionAudit(String shape, List<DataReferenceAudit> references,
                                 ProvenanceAudit provenance) {
        public ConditionAudit {
            shape = requireText(shape, "shape");
            references = List.copyOf(references);
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record IfAudit(StatementHeaderAudit header, ConditionAudit condition,
                          boolean explicitlyTerminated,
                          List<CobolSemanticProduct.StatementId> thenMembers,
                          List<CobolSemanticProduct.StatementId> elseMembers,
                          Optional<CobolSemanticProduct.StatementId> continuation)
            implements StatementAudit {
        public IfAudit {
            header = Objects.requireNonNull(header, "header");
            condition = Objects.requireNonNull(condition, "condition");
            thenMembers = List.copyOf(thenMembers);
            elseMembers = List.copyOf(elseMembers);
            continuation = Objects.requireNonNull(continuation, "continuation");
        }

        @Override public StatementFamily family() { return StatementFamily.IF; }
    }

    public record ObservedAudit(StatementHeaderAudit header, String observedKind,
                                String observedShape, String gapCode)
            implements StatementAudit {
        public ObservedAudit {
            header = Objects.requireNonNull(header, "header");
            observedKind = requireText(observedKind, "observedKind");
            observedShape = requireText(observedShape, "observedShape");
            gapCode = requireText(gapCode, "gapCode");
        }

        @Override public StatementFamily family() { return StatementFamily.OBSERVED; }
    }

    public record CoverageAudit(CobolSemanticProduct.InventoryStatus inventoryStatus,
                                int observedStatements, int modeledStatements,
                                int partialStatements, int unsupportedStatements,
                                int inputMissingStatements, ReadinessAudit readiness) {
        public CoverageAudit {
            inventoryStatus = Objects.requireNonNull(inventoryStatus, "inventoryStatus");
            if (observedStatements < 0 || modeledStatements < 0 || partialStatements < 0
                    || unsupportedStatements < 0 || inputMissingStatements < 0)
                throw new IllegalArgumentException("coverage counts must be non-negative");
            readiness = Objects.requireNonNull(readiness, "readiness");
        }
    }

    /** Immutable boundary-only reconstruction; it retains no port or frontend provider. */
    public record Audit(CobolSemanticProduct.UnitId unit, PolicyAudit policy,
                        List<DataAudit> dataDeclarations,
                        List<CobolSemanticProduct.StatementId> rootStatements,
                        List<StatementAudit> statements,
                        List<GapAudit> gaps, CoverageAudit coverage) {
        public Audit {
            unit = Objects.requireNonNull(unit, "unit");
            policy = Objects.requireNonNull(policy, "policy");
            dataDeclarations = List.copyOf(dataDeclarations);
            rootStatements = List.copyOf(rootStatements);
            statements = List.copyOf(statements);
            gaps = List.copyOf(gaps);
            coverage = Objects.requireNonNull(coverage, "coverage");
        }
    }

    /** Audits one already-materialized publication using the port as its sole input. */
    public static Audit audit(CobolSemanticPort port) {
        Objects.requireNonNull(port, "port");

        List<GapAudit> gaps = port.gaps().stream()
                .map(CobolLoweringReadinessConsumer::gap).toList();
        Map<CobolSemanticProduct.StatementId, List<GapAudit>> gapsByStatement =
                indexGaps(gaps);
        List<DataAudit> declarations = port.dataDeclarations().stream()
                .map(CobolLoweringReadinessConsumer::data).toList();
        List<StatementAudit> statements = new ArrayList<>(port.statements().size());
        for (CobolSemanticProduct.StatementFact fact : port.statements())
            statements.add(statement(port, fact, gapsByStatement));

        return new Audit(port.unit(), policy(port.policy()), declarations,
                port.rootStatements(), statements, gaps, coverage(port.coverage()));
    }

    private static StatementAudit statement(
            CobolSemanticPort port,
            CobolSemanticProduct.StatementFact fact,
            Map<CobolSemanticProduct.StatementId, List<GapAudit>> gapsByStatement) {
        StatementHeaderAudit header = header(fact.header(), gapsByStatement.getOrDefault(
                fact.header().id(), List.of()));
        if (fact instanceof CobolSemanticProduct.GobackFact goback)
            return new GobackAudit(header, goback.exit(), goback.localContinuation());
        if (fact instanceof CobolSemanticProduct.MoveFact move) {
            return new MoveAudit(header, move.source() instanceof CobolSemanticProduct.LiteralSource literal
                    ? literal(literal) : reference((CobolSemanticProduct.DataReference) move.source()), reference(move.target()));
        }
        if (fact instanceof CobolSemanticProduct.CallFact call) {
            return new CallAudit(header, call.syntax(), call.target(),
                    call.runtimeTarget(), call.runtimeUncertaintyCode());
        }
        if (fact instanceof CobolSemanticProduct.IfFact branch) {
            return new IfAudit(header, condition(branch.condition()),
                    branch.explicitlyTerminated(),
                    memberIds(port.children(header.id(), CobolSemanticProduct.Branch.THEN)),
                    memberIds(port.children(header.id(), CobolSemanticProduct.Branch.ELSE)),
                    branch.continuation());
        }
        if (fact instanceof CobolSemanticProduct.ObservedStatement observed) {
            return new ObservedAudit(header, observed.observedKind(), observed.observedShape(),
                    observed.gapCode());
        }
        throw new UnsupportedOperationException(
                "No lowering-readiness audit projection for Semantic Product statement family: "
                        + fact.getClass().getName());
    }

    private static Map<CobolSemanticProduct.StatementId, List<GapAudit>> indexGaps(
            List<GapAudit> gaps) {
        Map<CobolSemanticProduct.StatementId, List<GapAudit>> mutable =
                new LinkedHashMap<>();
        for (GapAudit gap : gaps)
            mutable.computeIfAbsent(gap.statement(), ignored -> new ArrayList<>()).add(gap);
        Map<CobolSemanticProduct.StatementId, List<GapAudit>> immutable =
                new LinkedHashMap<>();
        mutable.forEach((statement, localized) ->
                immutable.put(statement, List.copyOf(localized)));
        return Collections.unmodifiableMap(immutable);
    }

    private static DataAudit data(CobolSemanticProduct.DataDeclaration declaration) {
        return new DataAudit(declaration.id(), declaration.canonicalName(),
                declaration.picture(), provenance(declaration.provenance()),
                declaration.coverage(), readiness(declaration.readiness()));
    }

    private static StatementHeaderAudit header(
            CobolSemanticProduct.StatementHeader header, List<GapAudit> gaps) {
        CobolSemanticProduct.Containment containment = header.containment();
        return new StatementHeaderAudit(header.id(), header.point().ordinal(),
                new ContainmentAudit(containment.parent(), containment.branch()),
                provenance(header.provenance()), header.coverage(),
                readiness(header.readiness()), gaps);
    }

    private static LiteralAudit literal(CobolSemanticProduct.LiteralSource literal) {
        return new LiteralAudit(literal.id(), literal.kind(), literal.value(),
                provenance(literal.provenance()));
    }

    private static DataReferenceAudit reference(CobolSemanticProduct.DataReference reference) {
        return new DataReferenceAudit(reference.id(), reference.role(),
                binding(reference.binding()), provenance(reference.provenance()));
    }

    private static ConditionAudit condition(CobolSemanticProduct.ConditionSurface condition) {
        return new ConditionAudit(condition.shape(), condition.references().stream()
                .map(CobolLoweringReadinessConsumer::reference).toList(),
                provenance(condition.provenance()));
    }

    private static BindingAudit binding(CobolSemanticProduct.NominalBinding binding) {
        return new BindingAudit(binding.status(), binding.reason(), binding.candidates().stream()
                .map(candidate -> new CandidateAudit(candidate.id(), candidate.canonicalName()))
                .toList(), binding.selected());
    }

    private static List<CobolSemanticProduct.StatementId> memberIds(
            List<CobolSemanticProduct.StatementFact> facts) {
        return facts.stream().map(fact -> fact.header().id()).toList();
    }

    private static GapAudit gap(CobolSemanticProduct.Gap gap) {
        return new GapAudit(gap.statement(), gap.scope(), gap.code(), gap.detail(),
                provenance(gap.provenance()));
    }

    private static CoverageAudit coverage(CobolSemanticProduct.CoverageSummary coverage) {
        return new CoverageAudit(coverage.inventoryStatus(), coverage.observedStatements(),
                coverage.modeledStatements(), coverage.partialStatements(),
                coverage.unsupportedStatements(), coverage.inputMissingStatements(),
                readiness(coverage.readiness()));
    }

    private static PolicyAudit policy(CobolSemanticProduct.Policy policy) {
        return new PolicyAudit(policy.policyId(), policy.version(), policy.qualifyMode(),
                policy.pgmnameMode(), policy.dynamMode(), policy.dllMode());
    }

    private static ProvenanceAudit provenance(CobolSemanticProduct.Provenance provenance) {
        return new ProvenanceAudit(provenance.expanded(), provenance.original(),
                provenance.includeChain(), provenance.exact());
    }

    private static ReadinessAudit readiness(CobolSemanticProduct.Readiness readiness) {
        return new ReadinessAudit(dimension(readiness.lowering()), dimension(readiness.cfg()),
                dimension(readiness.effectsDataflow()));
    }

    private static DimensionAudit dimension(CobolSemanticProduct.ReadinessClaim claim) {
        return new DimensionAudit(claim.status(), claim.scope());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
