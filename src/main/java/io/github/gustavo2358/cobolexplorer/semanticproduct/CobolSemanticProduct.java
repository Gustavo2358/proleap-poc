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
        QUALIFIED_HIERARCHY_MATCH,
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

    public enum Branch { ROOT, THEN, ELSE, UNKNOWN }

    public enum OperandRole { READ, WRITE, CALL_TARGET }

    /** Literal category is semantic input; consumers must not recover it from value text. */
    public enum LiteralKind { ALPHANUMERIC, NUMERIC, UNKNOWN }

    public enum CallSyntax { IDENTIFIER_OR_EXPRESSION, LITERAL_PROGRAM_NAME }

    /** Runtime values are outside nominal binding and this checkpoint. */
    public enum RuntimeTargetKnowledge { UNKNOWN }

    public enum Availability { KNOWN, PARTIAL, UNAVAILABLE, INPUT_MISSING }
    public enum EntryRole { PRIMARY }
    public enum EntryInventoryScope { PRIMARY_ONLY }
    /** Availability of the explicit PROCEDURE DIVISION RETURNING/GIVING clause only. */
    public enum ReturningClause { ABSENT, PRESENT, UNKNOWN }
    public enum LocalContinuation { NONE }
    /** Does not classify a top-level unit as the runtime main program. */
    public enum GobackExit { CURRENT_PROGRAM_INVOCATION }

    public enum GapScope {
        RUNTIME_CALL_TARGET,
        LITERAL_KIND,
        NOMINAL_BINDING,
        CONDITION_SEMANTICS,
        STRUCTURE,
        CAPABILITY,
        ANALYSIS_INPUT,
        ENTRY_START,
        ENTRY_SIGNATURE
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

    public record EntryId(UnitId unit, int localId) {
        public EntryId {
            unit = Objects.requireNonNull(unit, "unit");
            require(localId >= 0, "entry localId must be non-negative");
        }
    }

    public record ExecutableStart(Availability availability, Optional<StatementId> statement) {
        public ExecutableStart {
            availability = Objects.requireNonNull(availability, "availability");
            statement = Objects.requireNonNull(statement, "statement");
            require((availability == Availability.KNOWN) == statement.isPresent(),
                    "known executable start requires exactly one statement identity");
        }
    }

    /** Known count is not a complete parameter contract; unknown count is never zero.
     * RETURNING describes the written clause, not runtime RETURN-CODE or effects. */
    public record EntrySignature(Availability availability, Optional<Integer> parameterCount,
                                 ReturningClause returningClause) {
        public EntrySignature {
            availability = Objects.requireNonNull(availability, "availability");
            parameterCount = Objects.requireNonNull(parameterCount, "parameterCount");
            returningClause = Objects.requireNonNull(returningClause, "returningClause");
            parameterCount.ifPresent(count -> require(count >= 0, "negative parameter count"));
            if (availability == Availability.UNAVAILABLE || availability == Availability.INPUT_MISSING)
                require(parameterCount.isEmpty() && returningClause == ReturningClause.UNKNOWN,
                        "unavailable signature cannot claim zero parameters or absent RETURNING");
            if (availability == Availability.KNOWN)
                require(parameterCount.equals(Optional.of(0)) && returningClause == ReturningClause.ABSENT,
                        "known signature capability covers only a header without clauses");
        }
    }

    public record EntryGap(GapScope scope, String code, String detail, Provenance provenance) {
        public EntryGap {
            scope = Objects.requireNonNull(scope, "scope");
            code = requireText(code, "code");
            detail = requireText(detail, "detail");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record EntryFact(EntryId id, EntryRole role, Availability availability,
                            ExecutableStart start, EntrySignature signature,
                            Provenance provenance, CoverageStatus coverage,
                            Readiness readiness, List<EntryGap> gaps) {
        public EntryFact {
            id = Objects.requireNonNull(id, "id");
            role = Objects.requireNonNull(role, "role");
            availability = Objects.requireNonNull(availability, "availability");
            start = Objects.requireNonNull(start, "start");
            signature = Objects.requireNonNull(signature, "signature");
            provenance = Objects.requireNonNull(provenance, "provenance");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
            gaps = List.copyOf(gaps);
            if (availability != Availability.KNOWN)
                require(start.statement().isEmpty(), "unavailable entry cannot have a known start");
            boolean complete = availability == Availability.KNOWN
                    && start.availability() == Availability.KNOWN
                    && signature.availability() == Availability.KNOWN;
            if (!complete) {
                require(coverage != CoverageStatus.MODELED && !gaps.isEmpty(),
                        "incomplete entry requires coverage and localized gaps");
                require(readiness.lowering().status() != ReadinessStatus.SUFFICIENT,
                        "incomplete entry cannot claim sufficient lowering readiness");
            }
            if (start.availability() != Availability.KNOWN) {
                require(gaps.stream().anyMatch(g -> g.scope() == GapScope.ENTRY_START),
                        "unknown entry start needs its gap");
                require(readiness.cfg().status() != ReadinessStatus.SUFFICIENT,
                        "unknown entry start cannot claim sufficient CFG readiness");
            }
            if (signature.availability() != Availability.KNOWN)
                require(gaps.stream().anyMatch(g -> g.scope() == GapScope.ENTRY_SIGNATURE),
                        "incomplete signature needs its gap");
            require(readiness.effectsDataflow().status() != ReadinessStatus.SUFFICIENT,
                    "entry capability does not publish effects/dataflow");
        }
    }

    /** Inventory scope is primary entries only; alternatives are explicitly open. */
    public record EntryInventory(InventoryStatus status, List<EntryFact> entries,
                                 List<String> gapCodes) {
        public EntryInventory {
            status = Objects.requireNonNull(status, "status");
            entries = List.copyOf(entries);
            gapCodes = List.copyOf(gapCodes);
            require(status != InventoryStatus.COMPLETE && !gapCodes.isEmpty(),
                    "primary-only capability cannot close the entry inventory");
            gapCodes.forEach(code -> requireText(code, "entry inventory gap"));
        }

        public static EntryInventory unavailable() {
            return new EntryInventory(InventoryStatus.PARTIAL, List.of(),
                    List.of("PRIMARY_ENTRY_NOT_AVAILABLE", "ALTERNATE_ENTRIES_NOT_PROJECTED"));
        }

        public EntryInventoryScope scope() { return EntryInventoryScope.PRIMARY_ONLY; }
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

    public enum LogicalDomain { TEXT }
    public enum StorageClass { WORKING_STORAGE }
    public enum DeclarationScope { LOCAL }
    /** Positive COBOL guarantee: standalone elementary textual DISPLAY value,
     * local ordinary WORKING-STORAGE, no relevant table/overlay/unknown clause. */
    public record ScalarText(int logicalExtent) {
        public ScalarText { require(logicalExtent > 0, "scalar extent must be positive"); }
        public LogicalDomain logicalDomain() { return LogicalDomain.TEXT; }
        public StorageClass storageClass() { return StorageClass.WORKING_STORAGE; }
        public DeclarationScope declarationScope() { return DeclarationScope.LOCAL; }
    }
    public record TextValue(String value) {
        public TextValue { value = Objects.requireNonNull(value, "logical text value"); }
        public LogicalDomain logicalDomain() { return LogicalDomain.TEXT; }
        public int logicalExtent() { return value.codePointCount(0, value.length()); }
    }
    /** This occurrence directly accesses the complete scalar value of data. */
    public record WholeItemAccess(DataItemId data) {
        public WholeItemAccess { data = Objects.requireNonNull(data, "whole item"); }
    }
    /** FULL_IDENTITY guarantees mandatory whole receiving-item overwrite without
     * conversion, padding or truncation. UNAVAILABLE makes no copy claim. */
    public enum CopySemantics { FULL_IDENTITY, FITTED_TEXT, UNAVAILABLE }
    public enum TextAdjustmentRule { RIGHT_PAD_SPACE }
    public record TextAdjustment(TextAdjustmentRule rule, int receiverExtent,
                                 TextValue result, Provenance provenance) {
        public TextAdjustment {
            Objects.requireNonNull(rule); Objects.requireNonNull(result); Objects.requireNonNull(provenance);
            require(receiverExtent > 0 && result.logicalExtent() == receiverExtent,
                    "adjusted text must fill receiver extent");
        }
    }
    public enum ContinuationAvailability { KNOWN, UNAVAILABLE, NONE }
    public record NormalContinuation(ContinuationAvailability availability,
                                     Optional<StatementId> statement, Provenance provenance) {
        public NormalContinuation {
            availability = Objects.requireNonNull(availability);
            statement = Objects.requireNonNull(statement);
            provenance = Objects.requireNonNull(provenance);
            require((availability == ContinuationAvailability.KNOWN) == statement.isPresent(),
                    "only known continuation has a statement");
        }
        public static NormalContinuation unavailable(Provenance provenance) {
            return new NormalContinuation(ContinuationAvailability.UNAVAILABLE, Optional.empty(), provenance);
        }
    }

    public record DataDeclaration(DataItemId id, String canonicalName,
                                  Optional<String> picture, Provenance provenance,
                                  CoverageStatus coverage, Readiness readiness, Optional<ScalarText> scalarText) {
        public DataDeclaration {
            scalarText = Objects.requireNonNull(scalarText);
            id = Objects.requireNonNull(id, "id");
            canonicalName = requireText(canonicalName, "canonicalName");
            picture = Objects.requireNonNull(picture, "picture");
            picture.ifPresent(value -> requireText(value, "picture value"));
            provenance = Objects.requireNonNull(provenance, "provenance");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
        }
        public DataDeclaration(DataItemId id, String canonicalName, Optional<String> picture,
                               Provenance provenance, CoverageStatus coverage, Readiness readiness) {
            this(id, canonicalName, picture, provenance, coverage, readiness, Optional.empty());
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
                    && reason != ResolutionReason.UNIQUE_VISIBLE_DECLARATION
                    && reason != ResolutionReason.QUALIFIED_HIERARCHY_MATCH)
                throw new IllegalArgumentException(
                        "resolved binding must retain its successful-selection reason");
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
            boolean requiresParent = branch == Branch.THEN || branch == Branch.ELSE;
            if (requiresParent != parent.isPresent())
                throw new IllegalArgumentException(
                        "THEN/ELSE require a parent; ROOT/UNKNOWN must omit it");
        }

        public static Containment root() {
            return new Containment(Optional.empty(), Branch.ROOT);
        }

        /** Statement is known to be nested, but its parent relation is not projected yet. */
        public static Containment unknown() {
            return new Containment(Optional.empty(), Branch.UNKNOWN);
        }

        public static Containment childOf(StatementId parent, Branch branch) {
            Objects.requireNonNull(parent, "parent");
            if (branch == Branch.ROOT || branch == Branch.UNKNOWN)
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

    public sealed interface MoveSource permits LiteralSource, DataReference {
        OperandId id();
        Provenance provenance();
    }

    public record LiteralSource(OperandId id, LiteralKind kind, String value,
                                Provenance provenance, Optional<TextValue> logicalValue) implements MoveSource {
        public LiteralSource {
            logicalValue = Objects.requireNonNull(logicalValue);
            if (logicalValue.isPresent()) require(kind == LiteralKind.ALPHANUMERIC && logicalValue.get().value().equals(value),
                    "logical text requires alphanumeric kind and normalized value");
            id = Objects.requireNonNull(id, "id");
            kind = Objects.requireNonNull(kind, "kind");
            value = Objects.requireNonNull(value, "value");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
        public LiteralSource(OperandId id, LiteralKind kind, String value, Provenance provenance) {
            this(id, kind, value, provenance, Optional.empty());
        }
    }

    public record DataReference(OperandId id, OperandRole role,
                                NominalBinding binding, Provenance provenance, Optional<WholeItemAccess> wholeItemAccess) implements CallTarget, MoveSource {
        public DataReference {
            wholeItemAccess = Objects.requireNonNull(wholeItemAccess);
            if (wholeItemAccess.isPresent()) require(binding.selected().equals(Optional.of(wholeItemAccess.get().data())),
                    "whole item access must agree with nominal selection");
            id = Objects.requireNonNull(id, "id");
            role = Objects.requireNonNull(role, "role");
            binding = Objects.requireNonNull(binding, "binding");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
        public DataReference(OperandId id, OperandRole role, NominalBinding binding, Provenance provenance) {
            this(id, role, binding, provenance, Optional.empty());
        }
    }

    public enum PredicateProfile { SCALAR_TEXT_EQUALITY, UNAVAILABLE }
    public enum PredicateDomain { BOOLEAN, UNKNOWN }
    public enum PredicateEvaluation { PURE, UNKNOWN }
    public enum PredicateCompletion { TOTAL, UNKNOWN }
    public enum ReadsCompleteness { COMPLETE, PARTIAL }
    public enum PredicateTruth { UNKNOWN }
    public enum IfProfile { SIMPLE_TEXT_EQUALITY, OUTSIDE_SLICE }
    public enum StorageIndependenceRule { INDEPENDENT_WORKING_STORAGE_ROOTS }

    /** Guarantees evaluation properties, never evaluates the predicate truth value. */
    public record PredicateGuarantee(Availability availability, PredicateProfile profile,
            List<OperandId> knownReads, Provenance provenance, List<String> gapCodes) {
        public PredicateGuarantee {
            Objects.requireNonNull(availability); Objects.requireNonNull(profile); Objects.requireNonNull(provenance);
            knownReads = List.copyOf(knownReads); gapCodes = List.copyOf(gapCodes);
            require(new HashSet<>(knownReads).size() == knownReads.size(), "duplicate predicate read");
            if (availability == Availability.KNOWN)
                require(profile == PredicateProfile.SCALAR_TEXT_EQUALITY && knownReads.size() == 1
                        && provenance.exact() && gapCodes.isEmpty(), "predicate proof requires one read and exact provenance");
            else require(profile == PredicateProfile.UNAVAILABLE && !gapCodes.isEmpty(), "unproven predicate requires gap");
        }
        public PredicateDomain resultDomain() { return availability == Availability.KNOWN ? PredicateDomain.BOOLEAN : PredicateDomain.UNKNOWN; }
        public PredicateEvaluation evaluation() { return availability == Availability.KNOWN ? PredicateEvaluation.PURE : PredicateEvaluation.UNKNOWN; }
        public PredicateCompletion normalCompletion() { return availability == Availability.KNOWN ? PredicateCompletion.TOTAL : PredicateCompletion.UNKNOWN; }
        public ReadsCompleteness readsCompleteness() { return availability == Availability.KNOWN ? ReadsCompleteness.COMPLETE : ReadsCompleteness.PARTIAL; }
        public PredicateTruth truthValue() { return PredicateTruth.UNKNOWN; }
        public static PredicateGuarantee unavailable(List<DataReference> references, Provenance provenance) {
            return new PredicateGuarantee(Availability.UNAVAILABLE, PredicateProfile.UNAVAILABLE,
                    references.stream().map(DataReference::id).toList(), provenance, List.of("PREDICATE_NOT_PROVEN"));
        }
    }

    public record IfArm(ClausePresence presence, Availability contentAvailability,
                        ExecutableStart entry, Provenance provenance, List<String> gapCodes) {
        public IfArm {
            Objects.requireNonNull(presence); Objects.requireNonNull(contentAvailability);
            Objects.requireNonNull(entry); Objects.requireNonNull(provenance); gapCodes = List.copyOf(gapCodes);
            if (presence == ClausePresence.ABSENT) require(entry.statement().isEmpty(), "absent arm cannot have entry");
            if (contentAvailability == Availability.KNOWN)
                require(presence != ClausePresence.UNKNOWN && provenance.exact() && gapCodes.isEmpty()
                        && (presence == ClausePresence.ABSENT || entry.availability() == Availability.KNOWN),
                        "complete arm needs presence, origin and executable entry or proven absence");
            else require(!gapCodes.isEmpty(), "incomplete arm requires gap");
        }
        public static IfArm unavailable(Provenance provenance) {
            return new IfArm(ClausePresence.UNKNOWN, Availability.UNAVAILABLE,
                    new ExecutableStart(Availability.UNAVAILABLE, Optional.empty()), provenance, List.of("IF_ARM_NOT_PROVEN"));
        }
    }

    /** Source-derived declaration evidence. Distinct IDs alone never create this fact. */
    public record IndependentStorageSet(Availability availability, List<DataItemId> members,
            Optional<Provenance> provenance, List<String> gapCodes) {
        public IndependentStorageSet {
            Objects.requireNonNull(availability); members = List.copyOf(members);
            Objects.requireNonNull(provenance); gapCodes = List.copyOf(gapCodes);
            if (availability == Availability.KNOWN)
                require(members.size() >= 2 && new HashSet<>(members).size() == members.size()
                        && provenance.isPresent() && provenance.get().exact() && gapCodes.isEmpty(),
                        "independent storage proof requires members and exact source authority");
            else require(members.isEmpty() && !gapCodes.isEmpty(), "unproven storage cannot publish independent members");
        }
        public StorageIndependenceRule rule() { return StorageIndependenceRule.INDEPENDENT_WORKING_STORAGE_ROOTS; }
        public String authority() { return "IBM_ENTERPRISE_COBOL_6_4_WORKING_STORAGE"; }
        public static IndependentStorageSet unavailable() {
            return new IndependentStorageSet(Availability.UNAVAILABLE, List.of(), Optional.empty(), List.of("STORAGE_INDEPENDENCE_NOT_PROVEN"));
        }
    }

    /**
     * Surface retained for structural IF facts. Predicate normalization remains
     * a later post-binding product; references here are only those already known.
     */
    public record ConditionSurface(String shape, List<DataReference> references,
                                   Provenance provenance, PredicateGuarantee predicate) {
        public ConditionSurface {
            shape = requireText(shape, "shape");
            references = List.copyOf(references);
            provenance = Objects.requireNonNull(provenance, "provenance");
            if (references.stream().anyMatch(reference -> reference.role() != OperandRole.READ))
                throw new IllegalArgumentException("condition references must have READ role");
            Objects.requireNonNull(predicate);
            require(predicate.knownReads().equals(references.stream().map(DataReference::id).toList()),
                    "predicate must preserve every known read occurrence");
            if (predicate.availability() == Availability.KNOWN)
                require(shape.equals("RELATION") && references.size() == 1 && provenance.exact()
                        && predicate.provenance().equals(provenance)
                        && references.get(0).wholeItemAccess().isPresent() && references.get(0).provenance().exact()
                        && references.get(0).binding().status() == ResolutionStatus.RESOLVED,
                        "predicate proof requires complete resolved whole-item read and origin");
        }
        public ConditionSurface(String shape, List<DataReference> references, Provenance provenance) {
            this(shape, references, provenance, PredicateGuarantee.unavailable(references, provenance));
        }
    }

    /** Adding a fact type extends this inventory without changing the State envelope. */
    public sealed interface StatementFact permits MoveFact, CallFact, IfFact,
            ObservedStatement, GobackFact, PerformFact {
        StatementHeader header();
    }

    public enum PerformProfile { SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM, OUTSIDE_SLICE }
    /** Canonical local PROCEDURE symbol identity; display spelling is not control. */
    public record ProcedureId(UnitId unit, int localId) {
        public ProcedureId { Objects.requireNonNull(unit); require(localId >= 0, "procedure localId must be non-negative"); }
    }
    public record PerformTarget(ProcedureId id, Provenance referenceOrigin, Provenance paragraphOrigin) {
        public PerformTarget { Objects.requireNonNull(id); Objects.requireNonNull(referenceOrigin); Objects.requireNonNull(paragraphOrigin); }
    }
    public record PerformFact(StatementHeader header, PerformProfile profile, Optional<PerformTarget> target,
            Optional<StatementId> targetEntry, List<StatementId> targetStatements, Optional<StatementId> targetExit,
            NormalContinuation normalContinuation, List<StatementId> primaryStatements, List<String> gapCodes) implements StatementFact {
        public PerformFact {
            Objects.requireNonNull(header); Objects.requireNonNull(profile); Objects.requireNonNull(target);
            Objects.requireNonNull(targetEntry); Objects.requireNonNull(targetExit); Objects.requireNonNull(normalContinuation);
            targetStatements = List.copyOf(targetStatements); primaryStatements = List.copyOf(primaryStatements); gapCodes = List.copyOf(gapCodes);
            boolean simple = profile == PerformProfile.SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM;
            require(simple == gapCodes.isEmpty(), "PERFORM profile must preserve gaps");
            if (simple) {
                require(target.isPresent() && !targetStatements.isEmpty() && !primaryStatements.isEmpty(), "PERFORM needs target and closed bodies");
                require(targetEntry.equals(Optional.of(targetStatements.get(0))) && targetExit.equals(Optional.of(targetStatements.get(targetStatements.size()-1))), "PERFORM body endpoints disagree");
                require(normalContinuation.availability() == ContinuationAvailability.KNOWN, "PERFORM requires unique resume");
                require(target.get().id().unit().equals(header.id().unit()), "PERFORM target must be local");
                require(header.provenance().exact() && target.get().referenceOrigin().exact() && target.get().paragraphOrigin().exact()
                    && normalContinuation.provenance().exact(), "PERFORM requires exact origins");
            } else require(target.isEmpty() && targetStatements.isEmpty() && targetEntry.isEmpty() && targetExit.isEmpty()
                    && primaryStatements.isEmpty() && normalContinuation.statement().isEmpty(), "refused PERFORM cannot publish control guarantees");
        }
    }

    /** GOBACK concludes this program invocation. On a called invocation it returns
     * to the caller; otherwise the runtime handles completion. Neither runtime
     * role, return value nor INITIAL/lifecycle effects are inferred here. */
    public record GobackFact(StatementHeader header) implements StatementFact {
        public GobackFact {
            header = Objects.requireNonNull(header, "header");
            require(header.readiness().effectsDataflow().status() != ReadinessStatus.SUFFICIENT,
                    "GOBACK local-exit capability does not publish effects/dataflow");
        }
        public GobackExit exit() { return GobackExit.CURRENT_PROGRAM_INVOCATION; }
        public LocalContinuation localContinuation() { return LocalContinuation.NONE; }
    }

    public record MoveFact(StatementHeader header, MoveSource source,
                           DataReference target, CopySemantics copySemantics,
                           NormalContinuation normalContinuation, Optional<TextAdjustment> textAdjustment) implements StatementFact {
        public MoveFact {
            copySemantics = Objects.requireNonNull(copySemantics);
            textAdjustment = Objects.requireNonNull(textAdjustment);
            require((copySemantics == CopySemantics.FITTED_TEXT) == textAdjustment.isPresent(),
                    "fitted copy requires adjustment; identity/unavailable omit it");
            if (copySemantics == CopySemantics.FITTED_TEXT)
                require(source instanceof LiteralSource literal && literal.logicalValue().isPresent() && target.wholeItemAccess().isPresent(),
                        "fitting requires logical source and whole scalar target");
            normalContinuation = Objects.requireNonNull(normalContinuation);
            if (copySemantics == CopySemantics.FULL_IDENTITY)
                require((source instanceof LiteralSource literal && literal.logicalValue().isPresent()
                        || source instanceof DataReference data && data.wholeItemAccess().isPresent())
                        && target.wholeItemAccess().isPresent(),
                        "full identity copy requires logical source and whole scalar target");
            header = Objects.requireNonNull(header, "header");
            source = Objects.requireNonNull(source, "source");
            target = Objects.requireNonNull(target, "target");
            if (source instanceof DataReference data) require(data.role() == OperandRole.READ, "MOVE data source requires READ");
            if (target.role() != OperandRole.WRITE)
                throw new IllegalArgumentException("MOVE target must have WRITE role");
        }
        public MoveFact(StatementHeader header, MoveSource source, DataReference target,
                        CopySemantics copySemantics, NormalContinuation normalContinuation) {
            this(header, source, target, copySemantics, normalContinuation, Optional.empty());
        }
        public MoveFact(StatementHeader header, MoveSource source, DataReference target) {
            this(header, source, target, CopySemantics.UNAVAILABLE,
                    NormalContinuation.unavailable(header.provenance()), Optional.empty());
        }
    }

    public sealed interface CallTarget permits DataReference, LiteralCallTarget {
        OperandId id();
        Provenance provenance();
    }
    public record LiteralCallTarget(OperandId id, String text, String writtenText,
                                    Optional<TextValue> logicalValue, Provenance provenance) implements CallTarget {
        public LiteralCallTarget {
            Objects.requireNonNull(id); Objects.requireNonNull(text); Objects.requireNonNull(writtenText);
            Objects.requireNonNull(logicalValue); Objects.requireNonNull(provenance);
            require(logicalValue.isEmpty() || logicalValue.get().value().equals(text),
                    "literal target logical value must agree with semantic text");
        }
    }
    public enum ClausePresence { ABSENT, PRESENT, UNKNOWN }
    public enum CallEffects { UNKNOWN }
    public enum CallOutcomes { OPEN }
    public record CallSurface(ClausePresence using, Optional<Integer> argumentCount,
                              ClausePresence returning, ClausePresence onException,
                              ClausePresence notOnException, ClausePresence onOverflow) {
        public CallSurface {
            Objects.requireNonNull(using); Objects.requireNonNull(argumentCount); Objects.requireNonNull(returning);
            Objects.requireNonNull(onException); Objects.requireNonNull(notOnException); Objects.requireNonNull(onOverflow);
            require(using != ClausePresence.ABSENT || argumentCount.equals(Optional.of(0)),
                    "absent USING requires known empty inventory");
            require(using != ClausePresence.UNKNOWN || argumentCount.isEmpty(),
                    "unknown USING must not manufacture inventory");
            require(using != ClausePresence.PRESENT || argumentCount.isPresent() && argumentCount.get() > 0,
                    "present USING requires arguments");
        }
        public boolean firstSlice() {
            return using == ClausePresence.ABSENT && returning == ClausePresence.ABSENT
                    && onException == ClausePresence.ABSENT && notOnException == ClausePresence.ABSENT
                    && onOverflow == ClausePresence.ABSENT;
        }
        public static CallSurface unknown() {
            return new CallSurface(ClausePresence.UNKNOWN, Optional.empty(), ClausePresence.UNKNOWN,
                    ClausePresence.UNKNOWN, ClausePresence.UNKNOWN, ClausePresence.UNKNOWN);
        }
    }
    /** Continuation only describes normal return; effects and other outcomes remain open. */
    public record CallFact(StatementHeader header, CallTarget target,
                           RuntimeTargetKnowledge runtimeTarget, String runtimeUncertaintyCode,
                           NormalContinuation normalContinuation, CallSurface surface) implements StatementFact {
        public CallFact {
            Objects.requireNonNull(header); Objects.requireNonNull(target); Objects.requireNonNull(runtimeTarget);
            runtimeUncertaintyCode = requireText(runtimeUncertaintyCode, "runtimeUncertaintyCode");
            Objects.requireNonNull(normalContinuation); Objects.requireNonNull(surface);
            if (target instanceof DataReference data)
                require(data.role() == OperandRole.CALL_TARGET, "CALL data target requires CALL_TARGET role");
            require(normalContinuation.availability() != ContinuationAvailability.NONE,
                    "CALL does not prove absence of normal return");
            require(header.readiness().effectsDataflow().status() != ReadinessStatus.SUFFICIENT,
                    "CALL effects and outcomes are not proven");
            if (header.readiness().lowering().status() == ReadinessStatus.SUFFICIENT)
                require(surface.firstSlice() && normalContinuation.availability() == ContinuationAvailability.KNOWN
                        && target.provenance().exact() && header.provenance().exact()
                        && (target instanceof DataReference data && data.wholeItemAccess().isPresent()
                            || target instanceof LiteralCallTarget literal && literal.logicalValue().isPresent()),
                        "CALL readiness requires proven target, continuation, origin and clause absence");
        }
        public CallSyntax syntax() {
            return target instanceof LiteralCallTarget ? CallSyntax.LITERAL_PROGRAM_NAME : CallSyntax.IDENTIFIER_OR_EXPRESSION;
        }
        public CallEffects effects() { return CallEffects.UNKNOWN; }
        public CallOutcomes outcomes() { return CallOutcomes.OPEN; }
    }

    /**
     * IF owns its condition surface and optional structural continuation.
     * Branch children are the statements whose containment names this IF.
     */
    public record IfFact(StatementHeader header, ConditionSurface condition,
                         boolean explicitlyTerminated,
                         Optional<StatementId> continuation, NormalContinuation normalContinuation,
                         IfArm thenArm, IfArm elseArm, IfProfile profile) implements StatementFact {
        public IfFact {
            header = Objects.requireNonNull(header, "header");
            condition = Objects.requireNonNull(condition, "condition");
            continuation = Objects.requireNonNull(continuation, "continuation");
            Objects.requireNonNull(normalContinuation); Objects.requireNonNull(thenArm);
            Objects.requireNonNull(elseArm); Objects.requireNonNull(profile);
            require(normalContinuation.availability() != ContinuationAvailability.NONE, "IF does not prove local exit");
            require(thenArm.presence() != ClausePresence.ABSENT, "THEN cannot be absent");
            if (profile == IfProfile.SIMPLE_TEXT_EQUALITY)
                require(explicitlyTerminated && condition.predicate().availability() == Availability.KNOWN
                        && normalContinuation.availability() == ContinuationAvailability.KNOWN
                        && thenArm.contentAvailability() == Availability.KNOWN && elseArm.contentAvailability() == Availability.KNOWN
                        && header.containment().branch() == Branch.ROOT, "simple IF requires all source proofs and root ownership");
        }
        public IfFact(StatementHeader header, ConditionSurface condition, boolean explicitlyTerminated,
                      Optional<StatementId> continuation) {
            this(header, condition, explicitlyTerminated, continuation, NormalContinuation.unavailable(header.provenance()),
                    IfArm.unavailable(header.provenance()), IfArm.unavailable(header.provenance()), IfProfile.OUTSIDE_SLICE);
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
                        List<Gap> gaps, CoverageSummary coverage, EntryInventory entryInventory, IndependentStorageSet storageIndependence) {
        public State {
            unit = Objects.requireNonNull(unit, "unit");
            policy = Objects.requireNonNull(policy, "policy");
            dataDeclarations = List.copyOf(dataDeclarations);
            statements = List.copyOf(statements);
            gaps = List.copyOf(gaps);
            coverage = Objects.requireNonNull(coverage, "coverage");
            entryInventory = Objects.requireNonNull(entryInventory, "entryInventory");
            validateState(unit, dataDeclarations, statements, gaps, coverage);
            validateEntries(unit, statements, entryInventory);
            Objects.requireNonNull(storageIndependence);
            Map<DataItemId, DataDeclaration> storageDeclarations = new HashMap<>();
            for (var declaration : dataDeclarations) storageDeclarations.put(declaration.id(), declaration);
            for (var member : storageIndependence.members()) {
                var declaration = storageDeclarations.get(member);
                require(declaration != null && member.unit().equals(unit) && declaration.scalarText().isPresent()
                        && declaration.provenance().exact() && declaration.coverage() == CoverageStatus.MODELED,
                        "independent member requires published, complete scalar declaration and origin");
            }
        }
        public State(UnitId unit, Policy policy, List<DataDeclaration> dataDeclarations,
                     List<StatementFact> statements, List<Gap> gaps, CoverageSummary coverage, EntryInventory entryInventory) {
            this(unit, policy, dataDeclarations, statements, gaps, coverage, entryInventory, IndependentStorageSet.unavailable());
        }

        /** Older manual publications explicitly lack entry knowledge. */
        public State(UnitId unit, Policy policy, List<DataDeclaration> dataDeclarations,
                     List<StatementFact> statements, List<Gap> gaps, CoverageSummary coverage) {
            this(unit, policy, dataDeclarations, statements, gaps, coverage, EntryInventory.unavailable());
        }
    }

    private static void validateEntries(UnitId unit, List<StatementFact> statements,
                                        EntryInventory inventory) {
        Set<StatementId> statementIds = new HashSet<>();
        for (StatementFact fact : statements) statementIds.add(fact.header().id());
        Set<EntryId> entries = new HashSet<>();
        Set<EntryRole> roles = new HashSet<>();
        for (EntryFact entry : inventory.entries()) {
            require(entry.id().unit().equals(unit), "entry crossed the unit namespace");
            require(entries.add(entry.id()), "duplicate entry identity");
            require(roles.add(entry.role()), "duplicate primary entry");
            entry.start().statement().ifPresent(target -> {
                require(target.unit().equals(unit), "entry start crossed the unit namespace");
                require(statementIds.contains(target), "entry start must reference a published statement");
            });
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
        if (statement instanceof MoveFact move && move.copySemantics() == CopySemantics.FULL_IDENTITY) {
            var declaration = declarations.get(move.target().wholeItemAccess().orElseThrow().data());
            require(declaration != null && declaration.scalarText().isPresent(), "copy requires scalar declaration");
            int extent;
            if (move.source() instanceof LiteralSource literal) extent = literal.logicalValue().orElseThrow().logicalExtent();
            else {
                var source = declarations.get(((DataReference) move.source()).wholeItemAccess().orElseThrow().data());
                require(source != null && source.scalarText().isPresent(), "copy requires scalar source declaration");
                extent = source.scalarText().orElseThrow().logicalExtent();
            }
            require(declaration.scalarText().orElseThrow().logicalExtent() == extent, "identity copy requires equal extents");
        }
        if (statement instanceof MoveFact move && move.textAdjustment().isPresent()) {
            var adjustment = move.textAdjustment().orElseThrow();
            var declaration = declarations.get(move.target().wholeItemAccess().orElseThrow().data());
            require(declaration != null && declaration.scalarText().isPresent(), "fitting requires scalar declaration");
            var source = ((LiteralSource) move.source()).logicalValue().orElseThrow();
            require(adjustment.receiverExtent() == declaration.scalarText().orElseThrow().logicalExtent()
                    && source.logicalExtent() < adjustment.receiverExtent(), "padding requires larger scalar receiver");
            require(adjustment.result().value().equals(source.value()
                    + " ".repeat(adjustment.receiverExtent() - source.logicalExtent())), "padding must preserve source and append spaces");
        }
        for (DataReference reference : references(statement)) {
            reference.wholeItemAccess().ifPresent(access -> {
                var declaration = declarations.get(access.data());
                require(declaration != null && declaration.scalarText().isPresent(), "whole item requires scalar declaration");
            });
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
        if (statement instanceof MoveFact move) return move.source() instanceof DataReference data
                ? List.of(data, move.target()) : List.of(move.target());
        if (statement instanceof CallFact call) return call.target() instanceof DataReference data ? List.of(data) : List.of();
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
                operands = List.of(call.target().id());
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
        Map<Containment, List<StatementFact>> armChildren = new HashMap<>();
        for (var statement : statements.values())
            armChildren.computeIfAbsent(statement.header().containment(), ignored -> new java.util.ArrayList<>()).add(statement);
        Map<StatementId, StructuralInterval> intervals = structuralIntervals(statements);
        for (StatementFact statement : statements.values()) {
            if (statement instanceof PerformFact perform && perform.profile() == PerformProfile.SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM) {
                var members = new HashSet<StatementId>();
                for (var id : perform.primaryStatements()) require(members.add(id) && statements.containsKey(id), "PERFORM primary members must be unique and published");
                for (int i = 0; i < perform.targetStatements().size(); i++) {
                    var id = perform.targetStatements().get(i);
                    require(members.add(id) && statements.get(id) instanceof MoveFact, "PERFORM target is a disjoint linear MOVE body");
                    var move = (MoveFact) statements.get(id);
                    var next = i + 1 < perform.targetStatements().size() ? Optional.of(perform.targetStatements().get(i+1)) : perform.normalContinuation().statement();
                    require(move.normalContinuation().statement().equals(next), "PERFORM body completion disagrees with published relation");
                }
                require(members.equals(statements.keySet()), "PERFORM proof covers the complete statement inventory");
                var main = perform.primaryStatements(); int callsite = main.indexOf(perform.header().id());
                require(callsite >= 0 && main.size() == callsite + 3 && statements.get(main.get(callsite+1)) instanceof CallFact
                    && statements.get(main.get(callsite+2)) instanceof GobackFact
                    && perform.normalContinuation().statement().equals(Optional.of(main.get(callsite+1))), "PERFORM primary shape/resume mismatch");
                for (int i = 0; i < callsite; i++) require(statements.get(main.get(i)) instanceof MoveFact move
                    && move.normalContinuation().statement().equals(Optional.of(main.get(i+1))), "PERFORM prefix relation mismatch");
            }
            if (statement instanceof MoveFact move) move.normalContinuation().statement().ifPresent(next -> {
                require(next.unit().equals(move.header().id().unit()) && statements.containsKey(next),
                        "MOVE continuation must reference a published statement in the same unit");
                require(!next.equals(move.header().id()), "MOVE cannot continue to itself");
            });
            if (statement instanceof CallFact call) call.normalContinuation().statement().ifPresent(next -> {
                require(next.unit().equals(call.header().id().unit()) && statements.containsKey(next),
                        "CALL continuation must reference a published statement in the same unit");
                require(!next.equals(call.header().id()), "CALL cannot continue to itself");
            });
            StatementHeader header = statement.header();
            header.containment().parent().ifPresent(parentId -> {
                StatementFact parent = statements.get(parentId);
                require(parent instanceof IfFact,
                        "branch parent must be a published IF fact");
                require(parent.header().point().ordinal() < header.point().ordinal(),
                        "branch parent must precede its structural child");
            });
            if (statement instanceof IfFact branch) {
                validateArm(branch, branch.thenArm(), Branch.THEN, armChildren);
                validateArm(branch, branch.elseArm(), Branch.ELSE, armChildren);
                branch.normalContinuation().statement().ifPresent(next -> {
                    require(statements.containsKey(next) && next.unit().equals(branch.header().id().unit())
                            && !next.equals(branch.header().id()), "IF completion requires published successor");
                    require(branch.continuation().equals(Optional.of(next)), "executable IF completion contradicts structural continuation");
                });
                branch.continuation().ifPresent(continuationId -> {
                    StatementFact continuation = statements.get(continuationId);
                    require(continuation != null,
                            "IF continuation must reference a published statement");
                    require(continuation.header().point().ordinal()
                                    > branch.header().point().ordinal(),
                            "IF continuation must follow its structural program point");
                    require(!intervals.get(branch.header().id()).contains(intervals.get(continuationId)),
                            "IF continuation cannot be contained by that IF");
                });
            }
        }
    }

    private static void validateArm(IfFact owner, IfArm arm, Branch side,
            Map<Containment, List<StatementFact>> children) {
        var members = children.getOrDefault(new Containment(Optional.of(owner.header().id()), side), List.of());
        if (arm.presence() == ClausePresence.ABSENT) require(members.isEmpty(), "absent arm has children");
        arm.entry().statement().ifPresent(entry -> require(!members.isEmpty() && members.get(0).header().id().equals(entry),
                "arm entry must identify its first direct child"));
        if (arm.contentAvailability() == Availability.KNOWN && arm.presence() == ClausePresence.PRESENT) {
            for (int i = 0; i < members.size(); i++) {
                var member = members.get(i);
                require(member instanceof MoveFact, "complete W2 arm admits only direct MOVE facts");
                var move = (MoveFact) member;
                require(move.copySemantics() != CopySemantics.UNAVAILABLE && move.header().provenance().exact(),
                        "complete W2 arm requires MOVE proof and origin");
                var expected = i + 1 < members.size() ? Optional.of(members.get(i + 1).header().id())
                        : owner.normalContinuation().statement();
                require(expected.isPresent() && move.normalContinuation().statement().equals(expected),
                        "arm MOVE completion must follow direct sibling or IF completion");
            }
        }
    }

    private record StructuralInterval(int begin, int end) {
        boolean contains(StructuralInterval other) { return begin < other.begin && other.end < end; }
    }
    private record StructuralVisit(StatementId id, boolean exit) { }

    /** Containment intervals validate ancestry once, without a parent-chain scan per IF. */
    private static Map<StatementId, StructuralInterval> structuralIntervals(Map<StatementId, StatementFact> statements) {
        Map<StatementId, List<StatementId>> children = new HashMap<>();
        java.util.Deque<StructuralVisit> pending = new java.util.ArrayDeque<>();
        for (var statement : statements.values()) {
            var parent = statement.header().containment().parent();
            if (parent.isEmpty()) pending.push(new StructuralVisit(statement.header().id(), false));
            else children.computeIfAbsent(parent.get(), ignored -> new java.util.ArrayList<>()).add(statement.header().id());
        }
        Map<StatementId, Integer> begins = new HashMap<>();
        Map<StatementId, StructuralInterval> intervals = new HashMap<>();
        int ordinal = 0;
        while (!pending.isEmpty()) {
            var visit = pending.pop();
            if (visit.exit()) intervals.put(visit.id(), new StructuralInterval(begins.get(visit.id()), ordinal++));
            else {
                require(begins.put(visit.id(), ordinal++) == null, "cyclic or duplicate containment");
                pending.push(new StructuralVisit(visit.id(), true));
                for (var child : children.getOrDefault(visit.id(), List.of())) pending.push(new StructuralVisit(child, false));
            }
        }
        require(intervals.size() == statements.size(), "containment must be a closed forest");
        return intervals;
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
            if (statement instanceof MoveFact move
                    && move.source() instanceof LiteralSource literal && literal.kind() == LiteralKind.UNKNOWN) {
                require(statement.header().coverage() != CoverageStatus.MODELED,
                        "unknown literal kind cannot be hidden by MODELED coverage");
                require(localized.stream().anyMatch(gap -> gap.scope()
                                == GapScope.LITERAL_KIND),
                        "unknown literal kind must retain a literal-kind gap");
            }
            if (statement instanceof ObservedStatement observed)
                require(hasGap(localized, GapScope.CAPABILITY, observed.gapCode()),
                        "observed unmodeled statement must retain its capability gap");
            if (statement.header().containment().branch() == Branch.UNKNOWN) {
                require(statement.header().coverage() != CoverageStatus.MODELED,
                        "unknown containment cannot be hidden by MODELED coverage");
                require(localized.stream().anyMatch(gap -> gap.scope() == GapScope.STRUCTURE),
                        "unknown containment must retain a structural gap");
            }
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
