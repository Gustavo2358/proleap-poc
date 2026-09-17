package io.github.gustavo2358.cobolexplorer.semanticproduct;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
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

    public enum Branch { ROOT, THEN, ELSE, EVALUATE_ARM, FILE_HANDLER, UNKNOWN }

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

    /** Local elementary unedited DISPLAY integer. Value/representation remain unknown. */
    public record ScalarInteger(int digits) { public ScalarInteger { require(digits>0,"positive integer digits"); } }
    public record DataDeclaration(DataItemId id, String canonicalName,
                                  Optional<String> picture, Provenance provenance,
                                  CoverageStatus coverage, Readiness readiness, Optional<ScalarText> scalarText, Optional<ScalarInteger> scalarInteger) {
        public DataDeclaration {
            scalarText = Objects.requireNonNull(scalarText);
            Objects.requireNonNull(scalarInteger); require(scalarText.isEmpty()||scalarInteger.isEmpty(),"distinct scalar domains");
            id = Objects.requireNonNull(id, "id");
            canonicalName = requireText(canonicalName, "canonicalName");
            picture = Objects.requireNonNull(picture, "picture");
            picture.ifPresent(value -> requireText(value, "picture value"));
            provenance = Objects.requireNonNull(provenance, "provenance");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
        }
        public DataDeclaration(DataItemId id,String canonicalName,Optional<String> picture,Provenance provenance,CoverageStatus coverage,Readiness readiness,Optional<ScalarText> scalarText) {
            this(id,canonicalName,picture,provenance,coverage,readiness,scalarText,Optional.empty());
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
            boolean requiresParent = branch == Branch.THEN || branch == Branch.ELSE || branch == Branch.EVALUATE_ARM || branch==Branch.FILE_HANDLER;
            if (requiresParent != parent.isPresent())
                throw new IllegalArgumentException(
                        "structural arms require a parent; ROOT/UNKNOWN must omit it");
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
                                NominalBinding binding, Provenance provenance, Optional<WholeItemAccess> wholeItemAccess, Optional<RegionalAccess> regionalAccess, List<RegionalAccess> regionalAlternatives, Optional<DataItemId> logicalWholeItem) implements CallTarget, MoveSource {
        public DataReference {
            Objects.requireNonNull(logicalWholeItem);
            if(logicalWholeItem.isPresent())require(binding.selected().equals(logicalWholeItem), "logical whole item must agree with nominal selection");
            regionalAlternatives=List.copyOf(regionalAlternatives);
            require(regionalAlternatives.isEmpty()||role==OperandRole.CALL_TARGET&&binding.status()==ResolutionStatus.AMBIGUOUS
                &&regionalAccess.isEmpty()&&wholeItemAccess.isEmpty(),"physical alternatives require an ambiguous CALL target");
            Objects.requireNonNull(regionalAccess);
            wholeItemAccess = Objects.requireNonNull(wholeItemAccess);
            if (wholeItemAccess.isPresent()) require(binding.selected().equals(Optional.of(wholeItemAccess.get().data())),
                    "whole item access must agree with nominal selection");
            id = Objects.requireNonNull(id, "id");
            role = Objects.requireNonNull(role, "role");
            binding = Objects.requireNonNull(binding, "binding");
            provenance = Objects.requireNonNull(provenance, "provenance");
        }
        public DataReference(OperandId id, OperandRole role, NominalBinding binding, Provenance provenance, Optional<WholeItemAccess> wholeItemAccess, Optional<RegionalAccess> regionalAccess, List<RegionalAccess> regionalAlternatives) {
            this(id,role,binding,provenance,wholeItemAccess,regionalAccess,regionalAlternatives,Optional.empty());
        }
        public DataReference(OperandId id, OperandRole role, NominalBinding binding, Provenance provenance, Optional<WholeItemAccess> wholeItemAccess, Optional<RegionalAccess> regionalAccess) {
            this(id,role,binding,provenance,wholeItemAccess,regionalAccess,List.of());
        }
        public DataReference(OperandId id, OperandRole role, NominalBinding binding, Provenance provenance, Optional<WholeItemAccess> wholeItemAccess) {
            this(id, role, binding, provenance, wholeItemAccess, Optional.empty());
        }
        public DataReference(OperandId id, OperandRole role, NominalBinding binding, Provenance provenance) {
            this(id, role, binding, provenance, Optional.empty());
        }
    }

    /** Physical identities are distinct from nominal DATA and from operand occurrences. */
    public record StorageNodeId(UnitId unit, int localId) {
        public StorageNodeId { Objects.requireNonNull(unit); require(localId >= 0, "negative physical node id"); }
    }
    public record StorageBaseId(UnitId unit, int localId) {
        public StorageBaseId { Objects.requireNonNull(unit); require(localId >= 0, "negative storage base id"); }
    }
    public record StorageRelationId(UnitId unit, int localId) {
        public StorageRelationId { Objects.requireNonNull(unit); require(localId >= 0, "negative storage relation id"); }
    }
    public enum StorageRelationStatus { PROVEN, UNPROVEN }
    public record StorageRelation(StorageRelationId id, StorageNodeId owner, Optional<StorageNodeId> target,
            StorageRelationStatus status, Provenance provenance, List<String> gapCodes) {
        public StorageRelation {
            Objects.requireNonNull(id);Objects.requireNonNull(owner);Objects.requireNonNull(target);Objects.requireNonNull(status);Objects.requireNonNull(provenance);
            gapCodes=List.copyOf(gapCodes);gapCodes.forEach(code->requireText(code,"relation gap"));
            require(status==StorageRelationStatus.PROVEN?target.isPresent()&&gapCodes.isEmpty():target.isEmpty()&&!gapCodes.isEmpty(),
                "proved relation requires target; unproved relation requires explicit uncertainty");
        }
    }
    public record StorageRenames(StorageRelationId id, StorageNodeId owner, Optional<StorageNodeId> from,
            Optional<StorageNodeId> through, StorageRelationStatus status, Provenance provenance, List<String> gapCodes) {
        public StorageRenames {
            Objects.requireNonNull(id);Objects.requireNonNull(owner);Objects.requireNonNull(from);Objects.requireNonNull(through);
            Objects.requireNonNull(status);Objects.requireNonNull(provenance);gapCodes=List.copyOf(gapCodes);
            require(status==StorageRelationStatus.PROVEN?from.isPresent()&&gapCodes.isEmpty():!gapCodes.isEmpty(),"RENAMES proof or explicit gap required");
        }
    }
    public enum StorageProfile { UNSPECIFIED, IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047 }
    public enum PhysicalKind { GROUP, ELEMENTARY, OPAQUE }
    public enum AllocationProof { INDEPENDENT_LOCAL_WORKING_STORAGE, INDEPENDENT_LOCAL_STORAGE, UNPROVEN;
        public boolean proved(){return this!=UNPROVEN;}
    }
    public enum RegionalMoveKind { LITERAL_BYTES, FITTED_LITERAL_BYTES, COPY_BYTES, FIT_TEXT, LOGICAL_FIT_TEXT, MUST_UNKNOWN, UNAVAILABLE }
    public record StorageMeasure(Optional<BigInteger> value, List<String> gapCodes) {
        public StorageMeasure {
            Objects.requireNonNull(value); gapCodes = List.copyOf(gapCodes);
            gapCodes.forEach(code -> requireText(code, "measure gap"));
            require(value.isPresent() ? value.get().signum() >= 0 && gapCodes.isEmpty() : !gapCodes.isEmpty(),
                    "known nonnegative measure or explicit unknown reason required");
        }
    }
    public record PhysicalNode(StorageNodeId id, Optional<StorageNodeId> parent, int order,
            boolean filler, PhysicalKind kind, Optional<DataItemId> data, StorageMeasure extent, Provenance provenance) {
        public PhysicalNode {
            Objects.requireNonNull(id); Objects.requireNonNull(parent); require(order >= 0, "negative sibling order");
            Objects.requireNonNull(kind); Objects.requireNonNull(data); Objects.requireNonNull(extent); Objects.requireNonNull(provenance);
            require(!filler || data.isEmpty(), "FILLER cannot require a nominal identity");
        }
    }
    public record StorageBase(StorageBaseId id, StorageMeasure extent, AllocationProof allocation, Provenance provenance) {
        public StorageBase { Objects.requireNonNull(id); Objects.requireNonNull(extent); Objects.requireNonNull(allocation); Objects.requireNonNull(provenance); }
    }
    public record StorageView(StorageNodeId node, StorageBaseId base, StorageMeasure offset,
            StorageMeasure extent, Optional<String> codec, Provenance provenance) {
        public StorageView {
            Objects.requireNonNull(node); Objects.requireNonNull(base); Objects.requireNonNull(offset);
            Objects.requireNonNull(extent); Objects.requireNonNull(codec); Objects.requireNonNull(provenance);
            require(codec.isEmpty() || codec.get().equals("text.ebcdic.ibm1047@1"), "unsupported storage codec");
        }
    }
    /** Absolute byte range within the declared view, proven for this occurrence. */
    public record RegionalSlice(BigInteger offset,BigInteger extent) {
        public RegionalSlice { Objects.requireNonNull(offset);Objects.requireNonNull(extent);require(offset.signum()>=0&&extent.signum()>0,"slice requires nonnegative offset and positive extent"); }
    }
    public record RegionalAccess(StorageNodeId view,Optional<RegionalSlice> slice) {
        public RegionalAccess { Objects.requireNonNull(view);Objects.requireNonNull(slice); }
        public RegionalAccess(StorageNodeId view) { this(view,Optional.empty()); }
    }
    public record RegionalMove(RegionalMoveKind kind, List<Integer> bytes, List<String> gapCodes) {
        public RegionalMove {
            Objects.requireNonNull(kind); bytes = List.copyOf(bytes); gapCodes = List.copyOf(gapCodes);
            require(bytes.stream().allMatch(b -> b >= 0 && b <= 255), "invalid octet");
            require(kind == RegionalMoveKind.LITERAL_BYTES || kind == RegionalMoveKind.FITTED_LITERAL_BYTES || bytes.isEmpty(), "only literal byte writes carry bytes");
            require((kind == RegionalMoveKind.MUST_UNKNOWN || kind == RegionalMoveKind.UNAVAILABLE) == !gapCodes.isEmpty(),
                    "precise write has no gaps; unknown write requires reasons");
            gapCodes.forEach(code -> requireText(code, "regional MOVE gap"));
        }
    }
    public enum StorageEntryMode { UNKNOWN, INITIAL, PRESERVED }
    public enum InitialStorageKind { LITERAL_BYTES, POSSIBLE_LITERAL_BYTES, POSSIBLE_LOGICAL_TEXT, PRESERVE, UNKNOWN }
    /** Source entry proof, versioned by storage 1.6.0. */
    public enum InitialStorageProof { NONE, EXPLICIT_INITIAL, EXPLICIT_PRESERVED, PROGRAM_INITIAL, DECLARATIVE_INVARIANT, DECLARATIVE_POSSIBILITY }
    public record StorageInitialCondition(StorageNodeId node,InitialStorageKind kind,List<Integer> bytes,List<String> gapCodes,Provenance provenance,InitialStorageProof proof,Optional<String> logicalText) {
        public StorageInitialCondition {
            Objects.requireNonNull(logicalText);require((kind==InitialStorageKind.POSSIBLE_LOGICAL_TEXT)==logicalText.isPresent(),"logical source text has its own initial kind");
            Objects.requireNonNull(proof);
            Objects.requireNonNull(node);Objects.requireNonNull(kind);Objects.requireNonNull(provenance);bytes=List.copyOf(bytes);gapCodes=List.copyOf(gapCodes);
            require(bytes.stream().allMatch(b->b>=0&&b<=255),"invalid initial octet");
            require(kind==InitialStorageKind.LITERAL_BYTES||kind==InitialStorageKind.POSSIBLE_LITERAL_BYTES||bytes.isEmpty(),"only literal entry facts carry bytes");
            require(kind!=InitialStorageKind.POSSIBLE_LITERAL_BYTES||!bytes.isEmpty(),"possible literal bytes must not be empty");
            require((kind==InitialStorageKind.UNKNOWN||kind==InitialStorageKind.POSSIBLE_LITERAL_BYTES||kind==InitialStorageKind.POSSIBLE_LOGICAL_TEXT)==!gapCodes.isEmpty(),"unknown or possible initial state requires gaps");
            require((kind!=InitialStorageKind.POSSIBLE_LITERAL_BYTES&&kind!=InitialStorageKind.POSSIBLE_LOGICAL_TEXT)||gapCodes.contains("ENTRY_STATE_NOT_PROVEN"),"possible entry requires lifecycle remainder");
            require(kind==InitialStorageKind.UNKNOWN?proof==InitialStorageProof.NONE:kind==InitialStorageKind.PRESERVE?proof==InitialStorageProof.EXPLICIT_PRESERVED
                :(kind==InitialStorageKind.POSSIBLE_LITERAL_BYTES||kind==InitialStorageKind.POSSIBLE_LOGICAL_TEXT)?proof==InitialStorageProof.DECLARATIVE_POSSIBILITY
                :Set.of(InitialStorageProof.EXPLICIT_INITIAL,InitialStorageProof.PROGRAM_INITIAL,InitialStorageProof.DECLARATIVE_INVARIANT).contains(proof),"initial kind contradicts proof");
            gapCodes.forEach(g->requireText(g,"initial storage gap"));
        }
        public StorageInitialCondition(StorageNodeId node,InitialStorageKind kind,List<Integer> bytes,List<String> gapCodes,Provenance provenance,InitialStorageProof proof) {
            this(node,kind,bytes,gapCodes,provenance,proof,Optional.empty());
        }
        public StorageInitialCondition(StorageNodeId node,InitialStorageKind kind,List<Integer> bytes,List<String> gapCodes,Provenance provenance) {
            this(node,kind,bytes,gapCodes,provenance,kind==InitialStorageKind.UNKNOWN?InitialStorageProof.NONE:kind==InitialStorageKind.PRESERVE?InitialStorageProof.EXPLICIT_PRESERVED:kind==InitialStorageKind.POSSIBLE_LITERAL_BYTES?InitialStorageProof.DECLARATIVE_POSSIBILITY:InitialStorageProof.EXPLICIT_INITIAL);
        }
    }
    public record StorageEntryState(StorageEntryMode mode,List<StorageInitialCondition> conditions) {
        public StorageEntryState {Objects.requireNonNull(mode);conditions=List.copyOf(conditions);}
        public static StorageEntryState unknown() {return new StorageEntryState(StorageEntryMode.UNKNOWN,List.of());}
    }
    public record StorageInventory(StorageProfile profile, List<PhysicalNode> nodes, List<StorageBase> bases,
            List<StorageView> views, List<String> gapCodes, List<StorageRelation> relations, List<StorageRenames> renames,StorageEntryState entryState) {
        public StorageInventory {
            Objects.requireNonNull(entryState);Objects.requireNonNull(profile); nodes = List.copyOf(nodes); bases = List.copyOf(bases);
            views = List.copyOf(views); gapCodes = List.copyOf(gapCodes); relations=List.copyOf(relations);renames=List.copyOf(renames);
            gapCodes.forEach(code -> requireText(code, "storage gap"));
            require(profile != StorageProfile.UNSPECIFIED || !gapCodes.isEmpty(), "absent environment requires a gap");
        }
        public StorageInventory(StorageProfile profile,List<PhysicalNode> nodes,List<StorageBase> bases,List<StorageView> views,List<String> gapCodes,List<StorageRelation> relations,List<StorageRenames> renames) {
            this(profile,nodes,bases,views,gapCodes,relations,renames,StorageEntryState.unknown());
        }
        public StorageInventory(StorageProfile profile,List<PhysicalNode> nodes,List<StorageBase> bases,List<StorageView> views,List<String> gapCodes,List<StorageRelation> relations) {
            this(profile,nodes,bases,views,gapCodes,relations,List.of());
        }
        public StorageInventory(StorageProfile profile,List<PhysicalNode> nodes,List<StorageBase> bases,List<StorageView> views,List<String> gapCodes) {
            this(profile,nodes,bases,views,gapCodes,List.of());
        }
        public Optional<String> profileId() { return profile == StorageProfile.UNSPECIFIED ? Optional.empty()
                : Optional.of("ibm-enterprise-6.4-fixed-display-1047@1"); }
        public Optional<String> runtimeCodec() { return profile == StorageProfile.UNSPECIFIED ? Optional.empty() : Optional.of("text.ebcdic.ibm1047@1"); }
        public static StorageInventory unavailable() { return new StorageInventory(StorageProfile.UNSPECIFIED, List.of(), List.of(), List.of(), List.of("PROFILE_NOT_SELECTED")); }
    }

    public enum PredicateProfile { SCALAR_TEXT_EQUALITY, NUMERIC_RELATION, UNAVAILABLE }
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
                require((profile == PredicateProfile.SCALAR_TEXT_EQUALITY && knownReads.size() == 1||profile==PredicateProfile.NUMERIC_RELATION&&knownReads.size()<=2)
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
                require(shape.equals("RELATION") && provenance.exact()
                        && predicate.provenance().equals(provenance)
                        && references.stream().allMatch(reference -> reference.wholeItemAccess().isPresent()
                            && reference.provenance().exact()
                            && reference.binding().status() == ResolutionStatus.RESOLVED),
                        "predicate proof requires complete resolved whole-item read and origin");
        }
        public ConditionSurface(String shape, List<DataReference> references, Provenance provenance) {
            this(shape, references, provenance, PredicateGuarantee.unavailable(references, provenance));
        }
    }

    /** Adding a fact type extends this inventory without changing the State envelope. */
    public sealed interface StatementFact permits MoveFact, CallFact, CicsFact, IfFact,
            ObservedStatement, GobackFact, PerformFact, EvaluateFact, GoToFact, ConditionalGoToFact, ProcedurePerformFact {
        StatementHeader header();
    }

    public enum CicsCommand { LINK, XCTL }
    public enum CicsConditions { LOCAL_CONDITION, DEFAULT_ENTRY_PREFIX, UNKNOWN }
    public record CicsOption(String name, Optional<String> operand, int start, int end, Optional<DataReference> reference) {
        public CicsOption { Objects.requireNonNull(reference); name=requireText(name,"option name");Objects.requireNonNull(operand);require(start>=0&&end>=start,"option offsets"); }
    }
    /** PROGRAM is the target option. Signature and effects remain independently partial. */
    public record CicsFact(StatementHeader header, CicsCommand command, String rawText,
        Optional<CallTarget> target, List<CicsOption> options, CicsConditions conditions,
        NormalContinuation localContinuation, NormalContinuation ordinaryContinuation, String nameProfile, List<String> gapCodes) implements StatementFact {
        public CicsFact {
            Objects.requireNonNull(header);Objects.requireNonNull(command);Objects.requireNonNull(rawText);
            Objects.requireNonNull(target);options=List.copyOf(options);Objects.requireNonNull(conditions);
            Objects.requireNonNull(localContinuation);Objects.requireNonNull(ordinaryContinuation);nameProfile=requireText(nameProfile,"name profile");gapCodes=List.copyOf(gapCodes);
            require(localContinuation.statement().isEmpty()||localContinuation.statement().equals(ordinaryContinuation.statement()),"CICS local and ordinary continuation agree within a paragraph");
            require(coherentCicsConditions(command,options,conditions,gapCodes),"CICS conditions contradict typed options or gaps");
            require(header.coverage()!=CoverageStatus.MODELED,"CICS effects/signature remain partial");
            for(var option:options)require(option.end()<=rawText.length(),"CICS option outside payload");
            target.ifPresent(t->require(t.id().statement().equals(header.id()),"CICS operand owner"));
        }
    }

    private static boolean coherentCicsConditions(CicsCommand command,List<CicsOption> options,CicsConditions conditions,List<String> gapCodes) {
        if(conditions==CicsConditions.UNKNOWN)return true;
        boolean local=options.stream().anyMatch(o->o.name().equals("RESP")||o.name().equals("NOHANDLE"));
        boolean shape=options.stream().filter(o->o.name().equals("PROGRAM")).count()==1
            &&options.stream().allMatch(o->Set.of("PROGRAM","COMMAREA","LENGTH","CHANNEL","RESP","RESP2","NOHANDLE","INPUTMSG","INPUTMSGLEN","SYSID","SYNCONRETURN","TRANSID","DATALENGTH").contains(o.name())
                &&((o.name().equals("NOHANDLE")||o.name().equals("SYNCONRETURN"))!=o.operand().isPresent())
                &&(command!=CicsCommand.XCTL||!Set.of("SYSID","SYNCONRETURN","TRANSID","DATALENGTH").contains(o.name())));
        var allowed=new HashSet<>(Set.of("CICS_EFFECTS_SIGNATURE_PARTIAL","CICS_HOST_BINDING_UNAVAILABLE","CICS_TARGET_UNKNOWN"));
        if(conditions==CicsConditions.LOCAL_CONDITION)allowed.add("CICS_CONDITION_VALUES_UNKNOWN");
        return shape&&allowed.containsAll(gapCodes)&&(conditions==CicsConditions.LOCAL_CONDITION?local:
            !local&&options.stream().noneMatch(o->o.name().equals("RESP2")));
    }

    /** Arm ordinal is semantic WHEN order, independent of physical statement inventory. */
    public record EvaluateArm(int ordinal, LiteralSource selection, List<StatementId> statements, IfArm control) {
        public EvaluateArm { require(ordinal >= 0, "arm ordinal is non-negative"); Objects.requireNonNull(selection);
            statements = List.copyOf(statements); Objects.requireNonNull(control); }
    }
    public record EvaluateFact(StatementHeader header, Optional<DataReference> subject, List<EvaluateArm> arms,
            IfArm otherArm, List<StatementId> otherStatements, NormalContinuation normalContinuation,
            List<String> gapCodes) implements StatementFact {
        public EvaluateFact { Objects.requireNonNull(header); Objects.requireNonNull(subject); arms = List.copyOf(arms);
            Objects.requireNonNull(otherArm); otherStatements = List.copyOf(otherStatements);
            Objects.requireNonNull(normalContinuation); gapCodes = List.copyOf(gapCodes);
            require(!arms.isEmpty(), "EVALUATE needs a WHEN literal");
            for (int i=0;i<arms.size();i++) require(arms.get(i).ordinal()==i, "WHEN ordinals preserve semantic order");
            require(normalContinuation.availability()!=ContinuationAvailability.NONE, "EVALUATE is not a terminal");
            subject.ifPresent(s -> require(s.role()==OperandRole.READ, "EVALUATE subject is read"));
        }
    }

    public record GoToTarget(ProcedureId id, Provenance paragraphOrigin) {
        public GoToTarget { Objects.requireNonNull(id); Objects.requireNonNull(paragraphOrigin); }
    }
    /** No normal continuation: the sole precise successor is targetEntry. */
    public record GoToFact(StatementHeader header, Optional<GoToTarget> target, Provenance referenceOrigin,
            Optional<StatementId> targetEntry, Optional<Provenance> entryOrigin, List<String> gapCodes) implements StatementFact {
        public GoToFact {
            Objects.requireNonNull(header); Objects.requireNonNull(target); Objects.requireNonNull(referenceOrigin);
            Objects.requireNonNull(targetEntry); Objects.requireNonNull(entryOrigin); gapCodes=List.copyOf(gapCodes);
            target.ifPresent(t -> require(t.id().unit().equals(header.id().unit()), "GO TO target is local"));
            require(targetEntry.isPresent()==entryOrigin.isPresent(), "GO TO entry and origin are paired");
            require(gapCodes.isEmpty()==targetEntry.isPresent(), "GO TO precise entry requires complete proof");
            if(targetEntry.isPresent()) require(target.isPresent() && header.provenance().exact() && referenceOrigin.exact()
                    && target.get().paragraphOrigin().exact() && entryOrigin.get().exact(), "GO TO requires exact origins");
        }
    }

    /** A destination occurrence; duplicate procedure identities retain distinct ordinals. */
    public record GoToDestination(int ordinal, Optional<ProcedureId> target, Optional<Provenance> procedureOrigin,
            Provenance referenceOrigin, Optional<StatementId> targetEntry, Optional<Provenance> entryOrigin,
            List<String> gapCodes) {
        public GoToDestination {
            require(ordinal>=0,"destination ordinal is nonnegative");Objects.requireNonNull(target);Objects.requireNonNull(procedureOrigin);
            Objects.requireNonNull(referenceOrigin);Objects.requireNonNull(targetEntry);Objects.requireNonNull(entryOrigin);gapCodes=List.copyOf(gapCodes);
            require(target.isPresent()==procedureOrigin.isPresent(),"procedure identity and origin are paired");
            require(targetEntry.isPresent()==entryOrigin.isPresent(),"entry and origin are paired");
            require(targetEntry.isEmpty()||target.isPresent(),"entry requires procedure identity");
            require(!gapCodes.isEmpty()||targetEntry.isPresent(),"complete destination requires entry");
        }
    }
    public record ConditionalGoToFact(StatementHeader header, Optional<DataReference> selector, boolean selectorInteger,
            Provenance selectorOrigin, List<GoToDestination> destinations, NormalContinuation normalContinuation,
            List<String> gapCodes) implements StatementFact {
        public ConditionalGoToFact {
            Objects.requireNonNull(header);Objects.requireNonNull(selector);Objects.requireNonNull(selectorOrigin);
            destinations=List.copyOf(destinations);Objects.requireNonNull(normalContinuation);gapCodes=List.copyOf(gapCodes);
            require(normalContinuation.availability()!=ContinuationAvailability.NONE,"conditional transfer has possible fallthrough");
            for(int i=0;i<destinations.size();i++) {
                var d=destinations.get(i);require(d.ordinal()==i,"destination ordinals are contiguous in semantic order");
                d.target().ifPresent(t->require(t.unit().equals(header.id().unit()),"local procedure identity"));
            }
            selector.ifPresent(r->require(r.role()==OperandRole.READ&&r.provenance().equals(selectorOrigin),"one source selector read"));
            require(!selectorInteger||selector.flatMap(DataReference::wholeItemAccess).isPresent(),"integer selector needs whole item");
            require(!gapCodes.isEmpty()||selectorInteger&&!destinations.isEmpty()&&normalContinuation.statement().isPresent()
                &&destinations.stream().allMatch(d->d.gapCodes().isEmpty()),"complete conditional control requires all proofs");
        }
    }

    /** Procedure order is language structure; statement lists are membership only. */
    public record PerformParagraph(ProcedureId id, StatementId entry, List<StatementId> statements,
            List<StatementId> completions, Provenance provenance) {
        public PerformParagraph { Objects.requireNonNull(id); Objects.requireNonNull(entry); Objects.requireNonNull(provenance);
            statements=List.copyOf(statements); completions=List.copyOf(completions); }
    }
    public enum PerformTestMode { BEFORE, AFTER }
    public record PerformLoop(PerformTestMode testMode, ConditionSurface condition) {
        public PerformLoop { Objects.requireNonNull(testMode); Objects.requireNonNull(condition); }
    }
    public enum PerformCountProfile { POSITIVE_INTEGER, INTEGER_ITEM, UNAVAILABLE }
    public record PerformCount(PerformCountProfile profile,Optional<String> integer,Optional<DataReference> reference,Provenance provenance) {
        public PerformCount { Objects.requireNonNull(profile);Objects.requireNonNull(integer);Objects.requireNonNull(reference);Objects.requireNonNull(provenance);
            require(integer.isEmpty()||reference.isEmpty(),"one count operand");
            if(profile==PerformCountProfile.POSITIVE_INTEGER)require(integer.filter(i->new java.math.BigInteger(i).signum()>0).isPresent()&&provenance.exact(),"positive count");
            if(profile==PerformCountProfile.INTEGER_ITEM)require(reference.filter(r->r.wholeItemAccess().isPresent()).isPresent()&&provenance.exact(),"whole integer count");
        }
    }
    public enum VaryingOperandRole { CONTROL_VARIABLE, FROM, BY }
    public record VaryingOperand(int level,VaryingOperandRole role,Optional<String> integer,List<DataReference> references,Provenance provenance) {
        public VaryingOperand { require(level>0,"varying level");Objects.requireNonNull(role);Objects.requireNonNull(integer);references=List.copyOf(references);Objects.requireNonNull(provenance); }
    }
    /** Each level denotes initialization and iteration whole-item writes to its control operand. */
    public record PerformVarying(int levels,List<VaryingOperand> controls) {
        public PerformVarying {require(levels>0,"varying levels");controls=List.copyOf(controls);}
    }
    public record ProcedurePerformFact(StatementHeader header, Optional<PerformTarget> start, Optional<PerformTarget> end,
            List<PerformParagraph> procedures, NormalContinuation normalContinuation, Optional<PerformLoop> loop, Optional<PerformCount> times, Optional<PerformVarying> varying,List<String> gapCodes) implements StatementFact {
        public ProcedurePerformFact { Objects.requireNonNull(header); Objects.requireNonNull(start); Objects.requireNonNull(end);
            Objects.requireNonNull(normalContinuation); Objects.requireNonNull(loop);Objects.requireNonNull(times);Objects.requireNonNull(varying);require(loop.isEmpty()||times.isEmpty(),"one repetition kind");require(varying.isEmpty()||loop.isPresent()&&times.isEmpty(),"VARYING uses condition loop"); procedures=List.copyOf(procedures); gapCodes=List.copyOf(gapCodes);
            if(gapCodes.isEmpty())require(start.isPresent() && end.isPresent() && !procedures.isEmpty()
                && normalContinuation.statement().isPresent(), "PERFORM range needs endpoints, body and resume");
            if(!procedures.isEmpty())require(start.isPresent() && end.isPresent()
                && procedures.get(0).id().equals(start.get().id()) && procedures.get(procedures.size()-1).id().equals(end.get().id()), "PERFORM range endpoints disagree");
            if(gapCodes.isEmpty())times.ifPresent(t->require(t.profile()!=PerformCountProfile.UNAVAILABLE,"count must be proven"));
            if(gapCodes.isEmpty())loop.ifPresent(l->require(l.condition().predicate().availability()==Availability.KNOWN,"loop predicate must be proven"));
            if(gapCodes.isEmpty())varying.ifPresent(v->{
                require(v.levels()==1 && v.controls().size()==3,"single VARYING control profile");
                for(var role:VaryingOperandRole.values()) {
                    var operands=v.controls().stream().filter(o->o.level()==1&&o.role()==role).toList();
                    require(operands.size()==1,"one typed VARYING operand per role");
                    var o=operands.get(0);require(o.provenance().exact(),"exact VARYING operand origin");
                    require(o.integer().isEmpty()||o.references().isEmpty(),"one VARYING operand form");
                    if(role==VaryingOperandRole.CONTROL_VARIABLE)require(o.integer().isEmpty()&&o.references().size()==1
                        &&o.references().get(0).role()==OperandRole.WRITE&&o.references().get(0).wholeItemAccess().isPresent(),"whole control item write");
                    if(role==VaryingOperandRole.FROM)require(o.integer().isPresent()||o.references().size()==1
                        &&o.references().get(0).role()==OperandRole.READ&&o.references().get(0).wholeItemAccess().isPresent(),"integer FROM value or read");
                    if(role==VaryingOperandRole.BY)require(o.integer().filter(i->new java.math.BigInteger(i).signum()!=0).isPresent(),"nonzero BY literal");
                }
            });
        }
    }

    public enum PerformProfile { SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM, BASIC_PROCEDURE_PERFORM, OUTSIDE_SLICE }
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
            boolean simple = profile != PerformProfile.OUTSIDE_SLICE;
            require(simple == gapCodes.isEmpty(), "PERFORM profile must preserve gaps");
            if (simple) {
                require(target.isPresent() && !targetStatements.isEmpty() && (profile == PerformProfile.BASIC_PROCEDURE_PERFORM ? primaryStatements.isEmpty() : !primaryStatements.isEmpty()), "PERFORM needs target and closed bodies");
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

    public record MoveTransfer(MoveSource source,DataReference target,RegionalMove effect) {
        public MoveTransfer { Objects.requireNonNull(source);Objects.requireNonNull(target);Objects.requireNonNull(effect);require(target.role()==OperandRole.WRITE,"transfer target requires WRITE");if(source instanceof DataReference r)require(r.role()==OperandRole.READ,"transfer source requires READ"); }
    }

    public record MoveFact(StatementHeader header, MoveSource source,
                           DataReference target, CopySemantics copySemantics,
                           NormalContinuation normalContinuation, Optional<TextAdjustment> textAdjustment, Optional<RegionalMove> regionalMove, List<MoveTransfer> additionalTransfers) implements StatementFact {
        public MoveFact {
            additionalTransfers=List.copyOf(additionalTransfers);
            require(additionalTransfers.isEmpty()||regionalMove.isPresent()&&copySemantics==CopySemantics.UNAVAILABLE,"additional transfers require regional effects exclusively");
            Objects.requireNonNull(regionalMove);
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
        public MoveFact(StatementHeader header,MoveSource source,DataReference target,CopySemantics copySemantics,
                NormalContinuation normalContinuation,Optional<TextAdjustment> textAdjustment,Optional<RegionalMove> regionalMove) {
            this(header,source,target,copySemantics,normalContinuation,textAdjustment,regionalMove,List.of());
        }
        public List<MoveTransfer> transfers() {
            if(regionalMove.isEmpty())return List.of();
            var result=new ArrayList<MoveTransfer>();result.add(new MoveTransfer(source,target,regionalMove.get()));result.addAll(additionalTransfers);return List.copyOf(result);
        }
        public MoveFact(StatementHeader header, MoveSource source, DataReference target,
                        CopySemantics copySemantics, NormalContinuation normalContinuation, Optional<TextAdjustment> textAdjustment) {
            this(header, source, target, copySemantics, normalContinuation, textAdjustment, Optional.empty());
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

    public enum EffectBound { NONE, ALL }
    public enum EnvironmentEffect { OUTPUT, INPUT, UNKNOWN }
    public enum EffectValueTransform { NONE, UNKNOWN }
    public enum EffectProof { DISPLAY_SIMPLE, INITIALIZE_TARGETS, ACCEPT_TARGET, SET_TARGETS, ARITHMETIC_TARGETS, STRING_TARGETS, UNSTRING_TARGETS, INSPECT_TARGETS }
    public record EffectSummary(List<OperandId> knownReads,List<OperandId> mayWrites,List<OperandId> mustOverwrite,
            List<OperandId> exposedRegions,EffectBound unknownReadBound,EffectBound unknownWriteBound,
            EffectBound unknownExposureBound,EnvironmentEffect environment,EffectValueTransform values,EffectProof proof) {
        public EffectSummary {
            knownReads=List.copyOf(knownReads);mayWrites=List.copyOf(mayWrites);mustOverwrite=List.copyOf(mustOverwrite);
            exposedRegions=List.copyOf(exposedRegions);Objects.requireNonNull(unknownReadBound);Objects.requireNonNull(unknownWriteBound);
            Objects.requireNonNull(unknownExposureBound);Objects.requireNonNull(environment);Objects.requireNonNull(values);Objects.requireNonNull(proof);
            require(mayWrites.containsAll(mustOverwrite),"MUST must be a known write");
        }
    }
    /** A visible statement whose value/control shape is not modeled by this capability. */
    public record ObservedStatement(StatementHeader header, String observedKind,
                                    String observedShape,
                                    String gapCode, NormalContinuation normalContinuation, List<DataReference> knownReferences,
                                    Optional<EffectSummary> effects) implements StatementFact {
        public ObservedStatement(StatementHeader header,String observedKind,String observedShape,String gapCode,
                NormalContinuation normalContinuation,List<DataReference> knownReferences) {
            this(header,observedKind,observedShape,gapCode,normalContinuation,knownReferences,Optional.empty());
        }
        public ObservedStatement(StatementHeader header, String observedKind, String observedShape, String gapCode) {
            this(header, observedKind, observedShape, gapCode, NormalContinuation.unavailable(header.provenance()), List.of());
        }
        public ObservedStatement {
            Objects.requireNonNull(effects);
            header = Objects.requireNonNull(header, "header");
            Objects.requireNonNull(normalContinuation); knownReferences=List.copyOf(knownReferences);
            if(effects.isPresent()) {
                var e=effects.orElseThrow();var refs=new java.util.HashMap<OperandId,DataReference>();knownReferences.forEach(r->refs.put(r.id(),r));
                for(var ids:List.of(e.knownReads(),e.mayWrites(),e.mustOverwrite(),e.exposedRegions())) {
                    require(new java.util.HashSet<>(ids).size()==ids.size(),"duplicate effect operand");
                    require(refs.keySet().containsAll(ids),"effect must reference an owned operand");
                }
                require(e.mayWrites().stream().allMatch(id->refs.get(id).role()==OperandRole.WRITE),"effect write role");
                require(e.mustOverwrite().isEmpty()||e.proof()==EffectProof.INITIALIZE_TARGETS,"only exact INITIALIZE is MUST in this slice");
                require(e.mustOverwrite().stream().allMatch(id->refs.get(id).regionalAccess().isPresent()),"MUST requires a physical access");
                if(e.proof()!=EffectProof.DISPLAY_SIMPLE)require(e.values()==EffectValueTransform.UNKNOWN
                    &&(e.unknownWriteBound()!=EffectBound.NONE||!e.mayWrites().isEmpty()),"receiver effect must retain writes or unknown bound");
                require(e.knownReads().stream().allMatch(id->refs.get(id).role()==OperandRole.READ),"effect read role");
                if(e.proof()==EffectProof.DISPLAY_SIMPLE)require(e.mayWrites().isEmpty()&&e.mustOverwrite().isEmpty()&&e.exposedRegions().isEmpty()
                    &&e.unknownWriteBound()==EffectBound.NONE&&e.unknownExposureBound()==EffectBound.NONE
                    &&e.environment()==EnvironmentEffect.OUTPUT&&e.values()==EffectValueTransform.NONE,"DISPLAY proof shape");
            }
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

    public enum FileKind { FD, SD, UNKNOWN }
    public enum FileOrganization { SEQUENTIAL, INDEXED, RELATIVE, UNSPECIFIED, UNSUPPORTED }
    public enum FileAccessMode { SEQUENTIAL, RANDOM, DYNAMIC, UNSPECIFIED, UNSUPPORTED }
    public enum FileVisibility { LOCAL, GLOBAL, EXTERNAL, CONFLICTING }
    public enum FileReferenceRole { RECORD_KEY, ALTERNATE_RECORD_KEY, RELATIVE_KEY, FILE_STATUS, ADDITIONAL_STATUS }
    public enum FileNameSource { ASSIGNMENT_NAME, SORT_COMMENT, UNSUPPORTED, ABSENT }
    public record FileId(UnitId unit, int localId) {
        public FileId { Objects.requireNonNull(unit); require(localId >= 0, "negative file id"); }
    }
    public record FileAssignment(Availability availability, String profile, String original,
                                 FileNameSource sourceKind, Optional<String> externalFileName, List<String> gapCodes) {
        public FileAssignment {
            Objects.requireNonNull(availability); requireText(profile, "file profile"); Objects.requireNonNull(original);
            Objects.requireNonNull(sourceKind); Objects.requireNonNull(externalFileName); gapCodes = List.copyOf(gapCodes);
            require(externalFileName.isPresent() == (sourceKind == FileNameSource.ASSIGNMENT_NAME), "external name/source kind mismatch");
            externalFileName.ifPresent(n -> requireText(n, "external name"));
            require(availability == Availability.KNOWN ? gapCodes.isEmpty() : !gapCodes.isEmpty(), "assignment availability/gaps mismatch");
            require(!externalFileName.isPresent() || availability == Availability.KNOWN, "unproved exact external name");
        }
    }
    public record FileReference(FileReferenceRole role, NominalBinding binding, boolean duplicates, Provenance provenance) {
        public FileReference { Objects.requireNonNull(role); Objects.requireNonNull(binding); Objects.requireNonNull(provenance); }
    }
    public record FileDeclaration(FileId id, UnitId owner, String logicalFile, FileKind kind,
            Optional<Boolean> optional, FileAssignment assignment, FileOrganization organization, FileAccessMode accessMode,
            FileVisibility visibility, List<DataItemId> records, List<FileReference> references,
            List<Provenance> origins, List<String> gapCodes) {
        public FileDeclaration {
            Objects.requireNonNull(id); Objects.requireNonNull(owner); require(id.unit().equals(owner), "file owner mismatch");
            requireText(logicalFile, "logicalFile"); Objects.requireNonNull(kind); Objects.requireNonNull(optional);
            Objects.requireNonNull(assignment); Objects.requireNonNull(organization); Objects.requireNonNull(accessMode); Objects.requireNonNull(visibility);
            records = List.copyOf(records); references = List.copyOf(references); origins = List.copyOf(origins); gapCodes = List.copyOf(gapCodes);
            require(!origins.isEmpty(), "file declaration needs origin");
            require(kind != FileKind.SD || assignment.externalFileName().isEmpty(), "SD name is a comment");
            require(new HashSet<>(records).size() == records.size(), "duplicate file record");
        }
    }
    public enum FileCommand { OPEN, READ, WRITE, REWRITE, DELETE_RECORD, START, CLOSE }
    public enum FileOpenMode { INPUT, OUTPUT, IO, EXTEND, UNSPECIFIED }
    public enum FileSyntaxProfile { N_LR, UNSUPPORTED }
    public enum FileOption { NEXT, REVERSED, NO_REWIND, LOCK, REEL, UNIT, FOR_REMOVAL, BEFORE_ADVANCING, AFTER_ADVANCING, PAGE }
    public enum FileKeyRelation { UNSPECIFIED, EQUAL, GREATER, GREATER_OR_EQUAL }
    public enum FileOperandRole { RECORD, INTO, FROM, KEY, ADVANCING }
    public enum FileOperandForm { REFERENCE, LITERAL, MNEMONIC, UNSUPPORTED }
    public enum FileHandlerKind { AT_END, NOT_AT_END, INVALID_KEY, NOT_INVALID_KEY, AT_END_OF_PAGE, NOT_AT_END_OF_PAGE }
    public record FileOperand(FileOperandRole role, FileOperandForm form, List<OperandId> references,
            Optional<String> writtenValue, Provenance provenance, List<String> gapCodes) {
        public FileOperand { Objects.requireNonNull(role);Objects.requireNonNull(form);references=List.copyOf(references);
            Objects.requireNonNull(writtenValue);Objects.requireNonNull(provenance);gapCodes=List.copyOf(gapCodes); }
    }
    public record FileHandler(FileHandlerKind kind, List<StatementId> statements, Provenance provenance) {
        public FileHandler { Objects.requireNonNull(kind);statements=List.copyOf(statements);Objects.requireNonNull(provenance); }
    }
    public enum FileEffectOutcome { SUCCESS, END, INVALID_KEY, OTHER_ERROR }
    public enum FileMemoryRole { RECORD, INTO, FROM_RECORD, FILE_STATUS, ADDITIONAL_STATUS, RELATIVE_KEY, RECORD_LENGTH }
    public enum FileMemoryKind { MAY_UNKNOWN, MUST_UNKNOWN, COPY_BYTES, FIT_TEXT }
    public record FileMemoryTarget(Optional<DataItemId> data,Optional<RegionalAccess> regional,boolean wholeBase,
            Optional<OperandId> reference,Provenance provenance) {
        public FileMemoryTarget {Objects.requireNonNull(data);Objects.requireNonNull(regional);Objects.requireNonNull(reference);Objects.requireNonNull(provenance);
            require(data.isPresent()||regional.isPresent(),"file memory target needs canonical DATA or storage");
            require(!wholeBase||regional.isEmpty()||regional.orElseThrow().slice().isEmpty(),"whole base cannot carry a precise slice");}
    }
    public record FileMemoryStep(FileMemoryRole role,FileMemoryKind kind,FileMemoryTarget destination,
            Optional<FileMemoryTarget> source,List<String> gapCodes,Provenance provenance) {
        public FileMemoryStep {Objects.requireNonNull(role);Objects.requireNonNull(kind);Objects.requireNonNull(destination);Objects.requireNonNull(source);gapCodes=List.copyOf(gapCodes);Objects.requireNonNull(provenance);
            require(kind!=FileMemoryKind.COPY_BYTES&&kind!=FileMemoryKind.FIT_TEXT||source.isPresent()&&gapCodes.isEmpty(),"precise transfer requires source and proof");
            require(kind==FileMemoryKind.MAY_UNKNOWN||!destination.wholeBase()&&destination.regional().isPresent()&&gapCodes.isEmpty(),"strong effect requires an exact receiving view and proof");
            require(kind!=FileMemoryKind.MUST_UNKNOWN||role==FileMemoryRole.INTO||role==FileMemoryRole.FILE_STATUS,"MUST is not implied by the I/O verb");
            require(source.isEmpty()||role==FileMemoryRole.FROM_RECORD,"source belongs only to FROM transfer");
            require(kind!=FileMemoryKind.COPY_BYTES&&kind!=FileMemoryKind.FIT_TEXT||role==FileMemoryRole.FROM_RECORD,"precise transfer belongs before I/O");}
    }
    public record FileOutcomeEffects(FileEffectOutcome outcome,List<FileMemoryStep> steps) {
        public FileOutcomeEffects {Objects.requireNonNull(outcome);steps=List.copyOf(steps);
            int prior=-1;var seen=java.util.EnumSet.noneOf(FileMemoryRole.class);
            for(var step:steps) {
                require(step.role()!=FileMemoryRole.FROM_RECORD,"FROM belongs before I/O");
                require(step.role()==FileMemoryRole.RECORD||seen.add(step.role()),"duplicate receiver role");
                require(outcome==FileEffectOutcome.SUCCESS||step.role()!=FileMemoryRole.INTO&&step.role()!=FileMemoryRole.RELATIVE_KEY&&step.role()!=FileMemoryRole.RECORD_LENGTH,"READ receiving effects require success");
                int rank=switch(step.role()){case RECORD,FROM_RECORD->0;case RELATIVE_KEY->1;case RECORD_LENGTH->2;case FILE_STATUS->3;case ADDITIONAL_STATUS->4;case INTO->5;};
                require(rank>=prior,"file memory steps out of order");prior=rank;
            }}
    }
    public record FileEffectPlan(Availability availability,List<FileMemoryTarget> ioReads,List<FileMemoryStep> before,
            List<FileOutcomeEffects> outcomes,boolean unknownReadBound,boolean unknownWriteBound,List<String> gapCodes) {
        public FileEffectPlan {Objects.requireNonNull(availability);ioReads=List.copyOf(ioReads);before=List.copyOf(before);outcomes=List.copyOf(outcomes);gapCodes=List.copyOf(gapCodes);
            require(outcomes.stream().map(FileOutcomeEffects::outcome).distinct().count()==outcomes.size(),"duplicate file effect outcome");
            require(availability!=Availability.KNOWN&&availability!=Availability.PARTIAL||outcomes.size()==FileEffectOutcome.values().length,"conditional effects must preserve every outcome");
            require(before.stream().allMatch(s->s.role()==FileMemoryRole.FROM_RECORD),"only FROM belongs before I/O");
            require(availability!=Availability.KNOWN||!unknownReadBound&&!unknownWriteBound&&gapCodes.isEmpty(),"known effect bounds require no gap");
            require(availability==Availability.KNOWN||!gapCodes.isEmpty(),"unavailable/partial file effects require reasons");
            require(availability!=Availability.UNAVAILABLE||ioReads.isEmpty()&&before.isEmpty()&&outcomes.isEmpty(),"unavailable effects cannot assert a plan");}
        public static FileEffectPlan unavailable(){return new FileEffectPlan(Availability.UNAVAILABLE,List.of(),List.of(),List.of(),true,true,List.of("FILE_MEMORY_EFFECTS_UNAVAILABLE"));}
    }
    public enum FileUseKind { AFTER_EXCEPTION, DEBUGGING }
    public enum FileControlEvent { SUCCESS, END, INVALID_KEY, OTHER_ERROR, END_OF_PAGE }
    public enum FileDestinationKind { CONTINUE, HANDLER, USE }
    public record FileDeclarative(String id,UnitId owner,FileUseKind kind,boolean global,FileOpenMode mode,List<FileId> files,
            List<StatementId> roots,Optional<StatementId> entry,List<StatementId> completions,List<String> gapCodes,Provenance provenance) {
        public FileDeclarative {Objects.requireNonNull(id);Objects.requireNonNull(owner);Objects.requireNonNull(kind);Objects.requireNonNull(mode);files=List.copyOf(files);roots=List.copyOf(roots);Objects.requireNonNull(entry);completions=List.copyOf(completions);gapCodes=List.copyOf(gapCodes);Objects.requireNonNull(provenance);
            require(entry.equals(roots.isEmpty()?Optional.empty():Optional.of(roots.get(0))),"USE entry must be its first published root");}
    }
    public record FileDestination(FileDestinationKind kind,Optional<FileHandlerKind> handler,Optional<String> declarative) {
        public FileDestination {Objects.requireNonNull(kind);Objects.requireNonNull(handler);Objects.requireNonNull(declarative);
            require(handler.isPresent()==(kind==FileDestinationKind.HANDLER)&&declarative.isPresent()==(kind==FileDestinationKind.USE),"file route destination shape");}
    }
    public record FileControlRoute(FileControlEvent event,FileEffectOutcome effects,List<FileDestination> destinations,boolean criticalExit) {
        public FileControlRoute {Objects.requireNonNull(event);Objects.requireNonNull(effects);destinations=List.copyOf(destinations);require(!destinations.isEmpty(),"file event requires destination");
            require(effects==switch(event){case SUCCESS,END_OF_PAGE->FileEffectOutcome.SUCCESS;case END->FileEffectOutcome.END;case INVALID_KEY->FileEffectOutcome.INVALID_KEY;case OTHER_ERROR->FileEffectOutcome.OTHER_ERROR;},"event/effect outcome mismatch");}
    }
    public record FileControlPlan(Availability availability,Optional<StatementId> continuation,List<FileControlRoute> routes,List<String> gapCodes) {
        public FileControlPlan {Objects.requireNonNull(availability);Objects.requireNonNull(continuation);routes=List.copyOf(routes);gapCodes=List.copyOf(gapCodes);
            require(routes.stream().map(FileControlRoute::event).distinct().count()==routes.size(),"duplicate file control event");
            require(availability!=Availability.KNOWN||gapCodes.isEmpty()&&!routes.isEmpty(),"known control requires routes and no gaps");
            require(availability==Availability.KNOWN||!gapCodes.isEmpty(),"partial/unavailable control requires gap");}
        public static FileControlPlan unavailable(){return new FileControlPlan(Availability.UNAVAILABLE,Optional.empty(),List.of(),List.of("FILE_CONTROL_UNAVAILABLE"));}
    }
    public record FileUse(StatementId statement, int ordinal, FileCommand command, FileOpenMode mode,
            FileSyntaxProfile profile, ResolutionStatus bindingStatus, List<FileId> candidates,
            Provenance provenance, List<String> gapCodes, List<FileOperand> operands, List<FileOption> options,
            FileKeyRelation keyRelation, boolean explicitTerminator, List<FileHandler> handlers,FileEffectPlan effects,FileControlPlan control) {
        public FileUse {
            Objects.requireNonNull(statement);require(ordinal>=0,"file use ordinal");Objects.requireNonNull(command);Objects.requireNonNull(mode);
            Objects.requireNonNull(profile);Objects.requireNonNull(bindingStatus);candidates=List.copyOf(candidates);
            Objects.requireNonNull(provenance);gapCodes=List.copyOf(gapCodes);
            operands=List.copyOf(operands);options=List.copyOf(options);Objects.requireNonNull(keyRelation);handlers=List.copyOf(handlers);Objects.requireNonNull(effects);Objects.requireNonNull(control);
            require(bindingStatus!=ResolutionStatus.RESOLVED||candidates.size()==1,"resolved file use needs unique identity");
        }
    }
    public record FileOperations(Availability availability,List<FileUse> uses,List<String> gapCodes) {
        public FileOperations { Objects.requireNonNull(availability);uses=List.copyOf(uses);gapCodes=List.copyOf(gapCodes); }
        public static FileOperations unavailable(){return new FileOperations(Availability.UNAVAILABLE,List.of(),List.of("FILE_OPERATIONS_UNAVAILABLE"));}
    }
    public record FileInventory(Availability availability, List<FileDeclaration> declarations, List<String> gapCodes, FileOperations operations,List<FileDeclarative> declaratives) {
        public FileInventory(Availability availability,List<FileDeclaration> declarations,List<String> gapCodes,FileOperations operations){this(availability,declarations,gapCodes,operations,List.of());}
        public FileInventory(Availability availability,List<FileDeclaration> declarations,List<String> gapCodes){this(availability,declarations,gapCodes,FileOperations.unavailable());}
        public FileInventory {
            Objects.requireNonNull(availability); declarations = List.copyOf(declarations); gapCodes = List.copyOf(gapCodes);Objects.requireNonNull(operations);declaratives=List.copyOf(declaratives);
            require(availability == Availability.KNOWN ? gapCodes.isEmpty() : !gapCodes.isEmpty(), "file inventory availability/gaps mismatch");
            require(availability != Availability.UNAVAILABLE || declarations.isEmpty(), "unavailable file inventory has declarations");
        }
        public static FileInventory unavailable() { return new FileInventory(Availability.UNAVAILABLE, List.of(), List.of("FILE_INVENTORY_UNAVAILABLE")); }
    }
    private static void validateFiles(UnitId unit, List<DataDeclaration> declarations, FileInventory inventory,List<StatementFact> statements,StorageInventory storage) {
        var data = new HashSet<DataItemId>(); for (var d : declarations) data.add(d.id());
        var files = new HashSet<FileId>(); var owned = new HashSet<DataItemId>();
        for (var file : inventory.declarations()) {
            require(file.owner().equals(unit) && files.add(file.id()), "duplicate or foreign file");
            for (var record : file.records()) require(data.contains(record) && owned.add(record), "missing or multiply owned file record");
            for (var ref : file.references()) for (var candidate : ref.binding().candidates())
                require(data.contains(candidate.id()), "file reference has no data declaration");
        }
        var ids=new HashSet<StatementId>();for(var statement:statements)ids.add(statement.header().id());
        var declarativeIds=new HashSet<String>();
        for(var d:inventory.declaratives()) {
            require(d.owner().equals(unit)&&declarativeIds.add(d.id()),"duplicate/foreign USE declaration");
            require(ids.containsAll(d.roots())&&ids.containsAll(d.completions()),"USE body references absent statements");
            require(new HashSet<>(d.roots()).size()==d.roots().size()&&new HashSet<>(d.completions()).size()==d.completions().size(),"duplicate USE members");
        }
        var refs=new HashSet<OperandId>();
        var nodes=new HashMap<StorageNodeId,PhysicalNode>();storage.nodes().forEach(n->nodes.put(n.id(),n));
        var views=new HashMap<StorageNodeId,StorageView>();storage.views().forEach(v->views.put(v.node(),v));
        for(var s:statements)if(s instanceof ObservedStatement o)o.knownReferences().forEach(r->refs.add(r.id()));
        var statementFacts=new HashMap<StatementId,StatementFact>();statements.forEach(s->statementFacts.put(s.header().id(),s));
        var handlerMembers=new HashSet<StatementId>();
        var uses=new HashSet<java.util.Map.Entry<StatementId,Integer>>();
        for(var use:inventory.operations().uses()) {
            require(ids.contains(use.statement())&&use.statement().unit().equals(unit),"file use statement absent or foreign");
            require(uses.add(java.util.Map.entry(use.statement(),use.ordinal())),"duplicate file use ordinal");
            require(new HashSet<>(use.candidates()).size()==use.candidates().size(),"duplicate file candidates");
            for(var candidate:use.candidates())require(!candidate.unit().equals(unit)||files.contains(candidate),"missing local file candidate");
            for(var operand:use.operands())for(var ref:operand.references())require(ref.statement().equals(use.statement())&&refs.contains(ref),"file operand outside observed statement");
            var kinds=new HashSet<FileHandlerKind>();var bodies=new HashSet<StatementId>();
            for(var handler:use.handlers()) {
                require(kinds.add(handler.kind()),"duplicate file handler kind");
                for(var body:handler.statements()) {
                    require(ids.contains(body)&&!body.equals(use.statement())&&bodies.add(body),"invalid file handler body");
                    require(statementFacts.get(body).header().containment().equals(Containment.childOf(use.statement(),Branch.FILE_HANDLER)),"file handler direct body containment mismatch");
                    handlerMembers.add(body);
                }
            }
            use.control().continuation().ifPresent(id->require(ids.contains(id)&&!id.equals(use.statement()),"file continuation absent/self"));
            for(var route:use.control().routes())for(var destination:route.destinations()) {
                destination.handler().ifPresent(h->require(kinds.contains(h),"file route handler absent"));
                destination.declarative().ifPresent(id->require(declarativeIds.contains(id),"file route USE absent"));
            }
            var targets=new ArrayList<>(use.effects().ioReads());var steps=new ArrayList<>(use.effects().before());use.effects().outcomes().forEach(c->steps.addAll(c.steps()));
            for(var step:steps){targets.add(step.destination());step.source().ifPresent(targets::add);}
            for(var target:targets) {
                target.data().ifPresent(id->require(data.contains(id),"file effect DATA missing or foreign"));
                target.reference().ifPresent(id->require(id.statement().equals(use.statement())&&refs.contains(id),"file effect occurrence outside observed statement"));
                target.regional().ifPresent(region->{
                    var node=nodes.get(region.view());var view=views.get(region.view());require(node!=null&&view!=null,"file effect view missing or foreign");
                    require(target.data().isEmpty()||node.data().equals(target.data()),"file effect DATA/view disagree");
                    region.slice().ifPresent(slice->{
                        require(view.offset().value().isPresent()&&view.extent().value().isPresent(),"file slice requires known view");
                        var start=view.offset().value().orElseThrow();require(slice.offset().compareTo(start)>=0&&slice.offset().add(slice.extent()).compareTo(start.add(view.extent().value().orElseThrow()))<=0,"file slice outside declared view");
                    });
                });
            }
        }
        for(var s:statements)if(s.header().containment().branch()==Branch.FILE_HANDLER)
            require(handlerMembers.contains(s.header().id()),"FILE_HANDLER child absent from file surface");
    }

    /** One immutable, closed publication with a cardinality-independent envelope. */
    public record State(UnitId unit, Policy policy,
                        List<DataDeclaration> dataDeclarations,
                        List<StatementFact> statements,
                        List<Gap> gaps, CoverageSummary coverage, EntryInventory entryInventory, IndependentStorageSet storageIndependence, StorageInventory storage, FileInventory fileInventory) {
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
            validateStorage(unit, dataDeclarations, statements, Objects.requireNonNull(storage));
            validateFiles(unit, dataDeclarations, Objects.requireNonNull(fileInventory),statements,storage);
            Objects.requireNonNull(storageIndependence);
            Map<DataItemId, DataDeclaration> storageDeclarations = new HashMap<>();
            for (var declaration : dataDeclarations) storageDeclarations.put(declaration.id(), declaration);
            for (var member : storageIndependence.members()) {
                var declaration = storageDeclarations.get(member);
                require(declaration != null && member.unit().equals(unit) && (declaration.scalarText().isPresent()||declaration.scalarInteger().isPresent())
                        && declaration.provenance().exact() && declaration.coverage() == CoverageStatus.MODELED,
                        "independent member requires published, complete scalar declaration and origin");
            }
        }
        public State(UnitId unit, Policy policy, List<DataDeclaration> dataDeclarations,
                     List<StatementFact> statements, List<Gap> gaps, CoverageSummary coverage, EntryInventory entryInventory,
                     IndependentStorageSet storageIndependence, StorageInventory storage) {
            this(unit, policy, dataDeclarations, statements, gaps, coverage, entryInventory, storageIndependence, storage, FileInventory.unavailable());
        }
        public State(UnitId unit, Policy policy, List<DataDeclaration> dataDeclarations,
                     List<StatementFact> statements, List<Gap> gaps, CoverageSummary coverage, EntryInventory entryInventory, IndependentStorageSet storageIndependence) {
            this(unit, policy, dataDeclarations, statements, gaps, coverage, entryInventory, storageIndependence, StorageInventory.unavailable());
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

    private static StorageView accessedView(Map<StorageNodeId,StorageView> views,RegionalAccess access) {
        var view=views.get(access.view());
        return access.slice().map(slice->new StorageView(view.node(),view.base(),new StorageMeasure(Optional.of(slice.offset()),List.of()),
            new StorageMeasure(Optional.of(slice.extent()),List.of()),view.codec(),view.provenance())).orElse(view);
    }
    private static void validateStorage(UnitId unit, List<DataDeclaration> declarations, List<StatementFact> statements, StorageInventory inventory) {
        var declared = new HashSet<DataItemId>();
        for (var d : declarations) declared.add(d.id());
        var nodes = new HashMap<StorageNodeId, PhysicalNode>();
        var dataNodes = new HashSet<DataItemId>();
        var byData = new HashMap<DataItemId,PhysicalNode>();
        var siblings = new HashMap<Optional<StorageNodeId>, Set<Integer>>();
        for (var n : inventory.nodes()) {
            require(n.id().unit().equals(unit) && nodes.put(n.id(),n) == null, "duplicate or foreign physical node");
            require(siblings.computeIfAbsent(n.parent(),ignored->new HashSet<>()).add(n.order()), "duplicate physical sibling order");
            n.data().ifPresent(id->{require(declared.contains(id) && dataNodes.add(id), "physical DATA must exist and have one node");byData.put(id,n);});
            require(inventory.profile() != StorageProfile.UNSPECIFIED || n.extent().value().isEmpty(), "known layout requires an explicit environment");
        }
        // Iterative forest closure: no recursive ancestry traversal or quadratic pair scan.
        var complete = new HashSet<StorageNodeId>();
        for (var n : inventory.nodes()) {
            var path = new HashSet<StorageNodeId>();
            var current = Optional.of(n.id());
            while (current.isPresent() && !complete.contains(current.get())) {
                var id = current.get(); var node = nodes.get(id);
                require(node != null && path.add(id), "dangling parent or cyclic physical tree");
                current = node.parent();
            }
            complete.addAll(path);
        }
        var bases = new HashMap<StorageBaseId, StorageBase>();
        for (var base : inventory.bases()) {
            require(base.id().unit().equals(unit) && bases.put(base.id(),base) == null, "duplicate or foreign storage base");
            require(inventory.profile() != StorageProfile.UNSPECIFIED || base.extent().value().isEmpty()
                && base.allocation() == AllocationProof.UNPROVEN, "known storage requires an explicit environment");
        }
        var views = new HashMap<StorageNodeId, StorageView>();
        var referencedBases = new HashSet<StorageBaseId>();
        for (var v : inventory.views()) {
            var node = nodes.get(v.node()); var base = bases.get(v.base());
            require(node != null && base != null && views.put(v.node(),v) == null, "view must have unique node and existing base");
            referencedBases.add(v.base());
            require(node.extent().equals(v.extent()), "view extent must agree with physical node");
            require(v.codec().isEmpty() || inventory.runtimeCodec().equals(v.codec()) && v.extent().value().isPresent()
                && node.kind() != PhysicalKind.OPAQUE, "textual view requires explicit supported layout and codec");
            if (v.offset().value().isPresent() && v.extent().value().isPresent() && base.extent().value().isPresent())
                require(v.offset().value().get().add(v.extent().value().get()).compareTo(base.extent().value().get()) <= 0, "view exceeds storage base");
        }
        require(views.size() == nodes.size() && referencedBases.equals(bases.keySet()), "every physical node and base needs explicit view closure, including unknown layout");
        var relationIds=new HashSet<StorageRelationId>();
        for(var relation:inventory.relations()) {
            require(relation.id().unit().equals(unit)&&relationIds.add(relation.id()),"duplicate or foreign storage relation");
            var owner=nodes.get(relation.owner());require(owner!=null,"storage relation owner must exist");
            if(relation.status()==StorageRelationStatus.PROVEN) {
                var target=nodes.get(relation.target().orElseThrow());
                require(target!=null&&target.parent().equals(owner.parent())&&target.order()<owner.order(),"proved relation must select an earlier physical sibling");
                var ov=views.get(owner.id());var tv=views.get(target.id());
                require(ov.base().equals(tv.base())&&ov.offset().equals(tv.offset()),"proved overlay must share base and start");
            }
        }
        var uncertainBases=new HashSet<StorageBaseId>();
        boolean unboundedRelation=false;
        for(var relation:inventory.relations())if(relation.status()==StorageRelationStatus.UNPROVEN) {
            var owner=nodes.get(relation.owner());
            if(owner.parent().isEmpty())unboundedRelation=true;
            else uncertainBases.add(views.get(owner.id()).base());
        }
        if(unboundedRelation) {
            require(inventory.bases().stream().noneMatch(b->b.allocation().proved()),
                "unproved root relation contradicts allocation independence");
            require(declarations.stream().allMatch(d->d.scalarText().isEmpty()&&d.scalarInteger().isEmpty()),
                "unproved root relation contradicts standalone scalar proof");
        }
        // The physical parent chain bounds a subordinate overlay to its record.
        // Do not infer endpoints or precise views inside that uncertain component.
        for(var base:uncertainBases)require(bases.get(base).extent().value().isEmpty(),
            "unproved subordinate relation requires unknown component extent");
        var uncertainData=new HashSet<DataItemId>();
        for(var view:views.values())if(uncertainBases.contains(view.base())) {
            require(view.extent().value().isEmpty()&&view.codec().isEmpty(),
                "unproved subordinate relation cannot certify component views");
            nodes.get(view.node()).data().ifPresent(uncertainData::add);
        }
        require(declarations.stream().filter(d->uncertainData.contains(d.id()))
            .allMatch(d->d.scalarText().isEmpty()&&d.scalarInteger().isEmpty()),
            "uncertain component contradicts scalar proof");
        for (var n : inventory.nodes()) n.parent().ifPresent(parent -> {
            var p = nodes.get(parent); var pv = views.get(parent); var v = views.get(n.id());
            require(p.kind() != PhysicalKind.ELEMENTARY && pv.base().equals(v.base()), "child must share parent storage base");
            if (pv.offset().value().isPresent() && pv.extent().value().isPresent() && v.offset().value().isPresent() && v.extent().value().isPresent())
                require(v.offset().value().get().compareTo(pv.offset().value().get()) >= 0
                    && v.offset().value().get().add(v.extent().value().get()).compareTo(pv.offset().value().get().add(pv.extent().value().get())) <= 0,
                    "child view exceeds physical parent");
        });
        StorageRenamesContract.validate(unit,inventory,nodes,views,relationIds);
        var initialNodes=new HashSet<StorageNodeId>();
        for(var condition:inventory.entryState().conditions()) {
            require(nodes.containsKey(condition.node())&&initialNodes.add(condition.node()),"initial condition needs unique existing physical node");
            if(condition.kind()==InitialStorageKind.POSSIBLE_LOGICAL_TEXT) {
                require(condition.provenance().exact(),"logical source evidence requires exact provenance");
            } else if(condition.kind()==InitialStorageKind.POSSIBLE_LITERAL_BYTES) {
                require(condition.provenance().exact()&&inventory.profile()==StorageProfile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,
                    "possible source bytes require source provenance and explicit representation profile");
            } else if(condition.kind()!=InitialStorageKind.UNKNOWN) {
                var view=views.get(condition.node());
                require(condition.kind()!=InitialStorageKind.LITERAL_BYTES||bases.get(view.base()).allocation().proved(),"strong initial bytes require proved allocation");
                require(condition.provenance().exact()&&view.codec().isPresent()&&view.offset().value().isPresent()&&view.extent().value().isPresent()
                    &&bases.get(view.base()).extent().value().isPresent(),"precise initial condition needs exact provenance and bounded supported view");
                boolean mode=condition.proof()==InitialStorageProof.EXPLICIT_INITIAL?inventory.entryState().mode()==StorageEntryMode.INITIAL
                    :condition.proof()==InitialStorageProof.EXPLICIT_PRESERVED?inventory.entryState().mode()==StorageEntryMode.PRESERVED:inventory.entryState().mode()==StorageEntryMode.UNKNOWN;
                require(mode&&((condition.kind()!=InitialStorageKind.LITERAL_BYTES&&condition.kind()!=InitialStorageKind.POSSIBLE_LITERAL_BYTES)||view.extent().value().get().equals(BigInteger.valueOf(condition.bytes().size()))),
                    "initial condition contradicts entry mode or extent");
                require((condition.proof()!=InitialStorageProof.DECLARATIVE_INVARIANT&&condition.proof()!=InitialStorageProof.DECLARATIVE_POSSIBILITY)||bases.get(view.base()).allocation().proved(),
                    "declarative invariant needs local independent storage");
            }
        }
        for (var statement : statements) {
            for(var ref:references(statement)) {
                var seenAlternatives=new HashSet<StorageNodeId>();
                for(var alternative:ref.regionalAlternatives()) {
                    var view=views.get(alternative.view());var node=nodes.get(alternative.view());
                    require(view!=null&&node!=null&&node.data().isPresent()&&ref.binding().candidates().stream().anyMatch(c->c.id().equals(node.data().get()))
                        &&seenAlternatives.add(node.id()),"physical alternatives must retain distinct nominal candidates");
                    require(alternative.slice().isEmpty()&&node.kind()==PhysicalKind.ELEMENTARY&&view.codec().isPresent()
                        &&view.offset().value().isPresent()&&view.extent().value().isPresent()&&view.extent().value().get().signum()>0
                        &&bases.get(view.base()).extent().value().isPresent(),"alternative requires canonical bounded whole text view");
                }
            }
            for (var ref : references(statement)) ref.regionalAccess().ifPresent(access -> {
                var view = views.get(access.view()); var node = nodes.get(access.view());
                require(view != null && node != null && ref.binding().status() == ResolutionStatus.RESOLVED
                    && ref.binding().selected().isPresent() && ref.binding().selected().equals(node.data()), "regional access must agree with unique nominal selection");
                require(view.codec().isPresent() && view.offset().value().isPresent() && view.extent().value().isPresent()
                    && view.extent().value().get().signum() > 0 && bases.get(view.base()).extent().value().isPresent(), "regional access needs a bounded supported view");
                require(ref.role() != OperandRole.CALL_TARGET || access.slice().isPresent() || node.kind() == PhysicalKind.ELEMENTARY, "regional CALL target must be elementary text");
                access.slice().ifPresent(slice->require(slice.offset().compareTo(view.offset().value().get())>=0
                    &&slice.offset().add(slice.extent()).compareTo(view.offset().value().get().add(view.extent().value().get()))<=0,"access slice must remain inside declared view"));
            });
            if (statement instanceof MoveFact move && move.regionalMove().isPresent()) {
                for(var transfer:move.transfers()) {
                var effect = transfer.effect();
                if (effect.kind() == RegionalMoveKind.UNAVAILABLE) continue;
                require(transfer.target().regionalAccess().isPresent(), "mandatory write requires exact destination access");
                var dest = accessedView(views,transfer.target().regionalAccess().get());
                if (effect.kind() == RegionalMoveKind.LITERAL_BYTES || effect.kind() == RegionalMoveKind.FITTED_LITERAL_BYTES) {
                    require(transfer.source() instanceof LiteralSource literal && literal.logicalValue().isPresent(), "byte literal write requires logical literal source");
                    require(dest.extent().value().get().equals(BigInteger.valueOf(effect.bytes().size())), "literal bytes must fill the exact destination");
                }
                if(effect.kind()==RegionalMoveKind.LOGICAL_FIT_TEXT) {
                    require(move.additionalTransfers().isEmpty()&&transfer.source() instanceof DataReference r&&r.logicalWholeItem().isPresent(),"logical copy needs one whole elementary source");
                    var data=((DataReference)transfer.source()).logicalWholeItem().orElseThrow();
                    var node=Objects.requireNonNull(byData.get(data));var source=views.get(node.id());
                    require(node.kind()==PhysicalKind.ELEMENTARY&&source.codec().equals(dest.codec())&&source.codec().isPresent()&&source.extent().value().filter(n->n.signum()>0).isPresent(),"logical copy needs proved textual shape");
                    require(!source.base().equals(dest.base())&&bases.get(source.base()).allocation().proved()&&bases.get(dest.base()).allocation().proved(),"logical copy requires positive base separation");
                }
                if (effect.kind() == RegionalMoveKind.COPY_BYTES || effect.kind() == RegionalMoveKind.FIT_TEXT) {
                    require(transfer.source() instanceof DataReference source && source.regionalAccess().isPresent(), "byte copy needs exact source access");
                    var source = accessedView(views,((DataReference)transfer.source()).regionalAccess().get());
                    require(effect.kind()!=RegionalMoveKind.COPY_BYTES || source.extent().equals(dest.extent()), "byte copy needs equal source and destination extents");
                    boolean disjoint;
                    if (source.base().equals(dest.base())) {
                        var left = source.offset().value().get(); var right = dest.offset().value().get();
                        disjoint = left.add(source.extent().value().get()).compareTo(right) <= 0
                            || right.add(dest.extent().value().get()).compareTo(left) <= 0;
                    } else disjoint = bases.get(source.base()).allocation().proved()
                            && bases.get(dest.base()).allocation().proved();
                    require(disjoint, "COBOL byte copy requires proved disjoint ranges");
                }
                }
            }
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
        if(statement instanceof ConditionalGoToFact g && g.selectorInteger()) {
            var d=declarations.get(g.selector().orElseThrow().wholeItemAccess().orElseThrow().data());
            require(d!=null&&d.scalarInteger().isPresent(),"integer selector references integer declaration");
        }
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
            reference.logicalWholeItem().ifPresent(data -> require(declarations.containsKey(data), "logical whole item needs a published declaration"));
            reference.wholeItemAccess().ifPresent(access -> {
                var declaration = declarations.get(access.data());
                require(declaration != null && (declaration.scalarText().isPresent()||declaration.scalarInteger().isPresent()), "whole item requires scalar declaration");
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
        if (statement instanceof MoveFact move) {
            var result=new ArrayList<DataReference>();if(move.source() instanceof DataReference r)result.add(r);result.add(move.target());
            for(var t:move.additionalTransfers()){if(t.source() instanceof DataReference r)result.add(r);result.add(t.target());}return List.copyOf(result);
        }
        if (statement instanceof CicsFact cics) return java.util.stream.Stream.concat(cics.target().filter(DataReference.class::isInstance).map(DataReference.class::cast).stream(),cics.options().stream().flatMap(o->o.reference().stream())).toList();
        if (statement instanceof CallFact call) return call.target() instanceof DataReference data ? List.of(data) : List.of();
        if (statement instanceof IfFact branch) return branch.condition().references();
        if (statement instanceof ProcedurePerformFact p) return java.util.stream.Stream.concat(java.util.stream.Stream.concat(p.loop().stream().flatMap(l->l.condition().references().stream()),p.times().stream().flatMap(t->t.reference().stream())),p.varying().stream().flatMap(v->v.controls().stream()).flatMap(v->v.references().stream())).toList();
        if (statement instanceof ConditionalGoToFact g) return g.selector().stream().toList();
        if (statement instanceof EvaluateFact e) return e.subject().stream().toList();
        if (statement instanceof ObservedStatement observed) return observed.knownReferences();
        return List.of();
    }

    private static void validateOperandIdentities(List<StatementFact> statements) {
        Set<OperandId> identities = new HashSet<>();
        for (StatementFact statement : statements) {
            List<OperandId> operands;
            if (statement instanceof MoveFact move) {
                var all=new ArrayList<OperandId>();all.add(move.source().id());all.add(move.target().id());
                for(var t:move.additionalTransfers()){all.add(t.source().id());all.add(t.target().id());}operands=List.copyOf(all);
            } else if (statement instanceof CicsFact cics) {
                operands=java.util.stream.Stream.concat(cics.target().stream().map(CallTarget::id),cics.options().stream().flatMap(o->o.reference().stream()).map(DataReference::id)).toList();
            } else if (statement instanceof CallFact call) {
                operands = List.of(call.target().id());
            } else if (statement instanceof IfFact branch) {
                operands = branch.condition().references().stream()
                        .map(DataReference::id).toList();
            } else if (statement instanceof ConditionalGoToFact g) {
                operands=references(g).stream().map(DataReference::id).toList();
            } else if (statement instanceof ProcedurePerformFact p) {
                operands=references(p).stream().map(DataReference::id).toList();
            } else if (statement instanceof EvaluateFact e) {
                var ids = new ArrayList<OperandId>(); e.subject().ifPresent(s -> ids.add(s.id()));
                e.arms().forEach(a -> ids.add(a.selection().id())); operands = ids;
            } else if(statement instanceof ObservedStatement observed) {
                operands=observed.knownReferences().stream().map(DataReference::id).toList();
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
        var goToTargets=new HashMap<ProcedureId,StatementId>();
        var procedureOrigins=new HashMap<ProcedureId,Provenance>();
        for (StatementFact statement : statements.values()) {
            if (statement instanceof GoToFact g) g.targetEntry().ifPresent(id -> {
                require(id.unit().equals(g.header().id().unit()) && statements.containsKey(id), "GO TO entry is published in same unit");
                var previousOrigin=procedureOrigins.putIfAbsent(g.target().orElseThrow().id(),g.target().orElseThrow().paragraphOrigin());
                require(previousOrigin==null||previousOrigin.equals(g.target().orElseThrow().paragraphOrigin()),"one procedure provenance");
                var previous=goToTargets.putIfAbsent(g.target().orElseThrow().id(),id);
                require(previous==null || previous.equals(id), "GO TO paragraph has one canonical entry");
                require(statements.get(id).header().containment().branch()==Branch.ROOT && statements.get(id).header().provenance().equals(g.entryOrigin().orElseThrow()), "GO TO entry origin agrees with target statement");
            });
            if(statement instanceof ConditionalGoToFact g) {
                for(var d:g.destinations()) {
                    d.target().ifPresent(target->{
                        var previous=procedureOrigins.putIfAbsent(target,d.procedureOrigin().orElseThrow());
                        require(previous==null||previous.equals(d.procedureOrigin().orElseThrow()),"one procedure provenance");
                    });
                    d.targetEntry().ifPresent(id->{
                        var target=statements.get(id);require(id.unit().equals(g.header().id().unit())&&target!=null,"local published conditional destination");
                        require(target.header().containment().branch()==Branch.ROOT&&d.entryOrigin().filter(target.header().provenance()::equals).isPresent(),"conditional entry provenance agreement");
                        var previous=goToTargets.putIfAbsent(d.target().orElseThrow(),id);
                        require(previous==null||previous.equals(id),"one canonical procedure entry");
                    });
                }
                g.normalContinuation().statement().ifPresent(id->{
                    var next=statements.get(id);require(next!=null&&id.unit().equals(g.header().id().unit()),"published local fallthrough");
                    require(g.normalContinuation().provenance().equals(next.header().provenance()),"fallthrough provenance agreement");
                });
            }
            if(statement instanceof ProcedurePerformFact p) {
                var members=new HashSet<StatementId>();var paragraphs=new HashSet<ProcedureId>();
                for(var paragraph:p.procedures()) {
                    require(paragraphs.add(paragraph.id()) && paragraph.id().unit().equals(p.header().id().unit()), "unique local range paragraph");
                    for(var id:paragraph.statements())require(members.add(id) && statements.containsKey(id), "unique published range member");
                    require(paragraph.statements().contains(paragraph.entry()), "paragraph entry belongs to body");
                    require(paragraph.statements().containsAll(paragraph.completions()), "paragraph completion belongs to body");
                }
                p.normalContinuation().statement().ifPresent(id->require(statements.containsKey(id), "published range resume"));
                if(p.gapCodes().isEmpty())require(!members.contains(p.header().id()) && p.normalContinuation().statement().filter(members::contains).isEmpty(), "activation/resume outside range");
            }
            if (statement instanceof PerformFact basic && basic.profile() == PerformProfile.BASIC_PROCEDURE_PERFORM) {
                var seen=new HashSet<StatementId>();
                for(int i=0;i<basic.targetStatements().size();i++) {
                    var id=basic.targetStatements().get(i);
                    require(seen.add(id) && statements.get(id) instanceof MoveFact, "BASIC body contains distinct published MOVEs");
                    var move=(MoveFact)statements.get(id);
                    var expected=i+1<basic.targetStatements().size()?Optional.of(basic.targetStatements().get(i+1)):Optional.<StatementId>empty();
                    require(move.normalContinuation().statement().equals(expected), "intrinsic BASIC body continuation mismatch");
                    require(!id.equals(basic.header().id()) && !basic.normalContinuation().statement().equals(Optional.of(id)), "BASIC activation and resume are outside body");
                }
            }
            if (statement instanceof PerformFact perform && perform.profile() == PerformProfile.SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM) {
                var members = new HashSet<StatementId>();
                for (var id : perform.primaryStatements()) require(members.add(id) && statements.containsKey(id), "PERFORM primary members must be unique and published");
                for (int i = 0; i < perform.targetStatements().size(); i++) {
                    var id = perform.targetStatements().get(i);
                    require(members.add(id) && statements.get(id) instanceof MoveFact, "PERFORM target is a disjoint linear MOVE body");
                    var move = (MoveFact) statements.get(id);
                    var next = i + 1 < perform.targetStatements().size() ? Optional.of(perform.targetStatements().get(i+1)) : (perform.profile() == PerformProfile.BASIC_PROCEDURE_PERFORM ? Optional.<StatementId>empty() : perform.normalContinuation().statement());
                    require(move.normalContinuation().statement().equals(next), "PERFORM body completion disagrees with published relation");
                }
                var main = perform.primaryStatements(); int callsite = main.indexOf(perform.header().id());
                require(callsite >= 0 && callsite + 1 < main.size()
                    && statements.get(main.get(main.size() - 1)) instanceof GobackFact
                    && perform.normalContinuation().statement().equals(Optional.of(main.get(callsite + 1))), "PERFORM primary end/resume mismatch");
                if (perform.profile() == PerformProfile.SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM)
                    require(statements.values().stream().filter(PerformFact.class::isInstance).count() == 1, "legacy SP1.7 one isolated PERFORM callsite");
                for (int i = 0; i < main.size(); i++) {
                    var root = statements.get(main.get(i));
                    require(root.header().containment().equals(new Containment(Optional.empty(), Branch.ROOT)), "PERFORM primary members are direct roots");
                    if (i == main.size() - 1) continue;
                    NormalContinuation next;
                    if (root instanceof MoveFact move) next = move.normalContinuation();
                    else if (root instanceof CallFact call) next = call.normalContinuation();
                    else if (root instanceof PerformFact p) next = p.normalContinuation();
                    else if (root instanceof IfFact branch) {
                        require(branch.profile() == IfProfile.SIMPLE_TEXT_EQUALITY, "PERFORM primary IF must be supported");
                        next = branch.normalContinuation();
                        for (var arm : List.of(Branch.THEN, Branch.ELSE))
                            for (var child : armChildren.getOrDefault(new Containment(Optional.of(root.header().id()), arm), List.of()))
                                require(child instanceof MoveFact && members.add(child.header().id()), "PERFORM primary IF has disjoint MOVE-only arms");
                    } else throw new IllegalArgumentException("unsupported PERFORM primary statement");
                    require(next.availability() == ContinuationAvailability.KNOWN
                        && next.statement().equals(Optional.of(main.get(i + 1))), "PERFORM primary continuation mismatch");
                }
                if (perform.profile() == PerformProfile.SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM)
                    require(members.equals(statements.keySet()), "legacy PERFORM proof covers roots, IF arms and target exactly");
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
            if(statement instanceof CicsFact cics)for(var continuation:List.of(cics.localContinuation(),cics.ordinaryContinuation()))continuation.statement().ifPresent(next -> {
                require(next.unit().equals(statement.header().id().unit()) && statements.containsKey(next) && !next.equals(statement.header().id()),
                    "CICS lexical continuation references a different published statement in the same unit");
            });
            if(statement instanceof ObservedStatement observed)observed.normalContinuation().statement().ifPresent(next -> {
                require(next.unit().equals(statement.header().id().unit()) && statements.containsKey(next) && !next.equals(statement.header().id()),
                    "observed continuation references a different published statement in the same unit");
            });
            StatementHeader header = statement.header();
            header.containment().parent().ifPresent(parentId -> {
                StatementFact parent = statements.get(parentId);
                require(parent instanceof IfFact && (header.containment().branch()==Branch.THEN||header.containment().branch()==Branch.ELSE)
                        || parent instanceof EvaluateFact && header.containment().branch()==Branch.EVALUATE_ARM
                        || parent instanceof ObservedStatement && header.containment().branch()==Branch.FILE_HANDLER,
                        "branch parent must be a published IF fact");
                require(parent.header().point().ordinal() < header.point().ordinal(),
                        "branch parent must precede its structural child");
            });
            if (statement instanceof EvaluateFact e) {
                var members = new HashSet<StatementId>();
                for (var a : e.arms()) validateEvaluateArm(e, a.control(), a.statements(), statements, members);
                validateEvaluateArm(e, e.otherArm(), e.otherStatements(), statements, members);
                var expected = armChildren.getOrDefault(new Containment(Optional.of(e.header().id()), Branch.EVALUATE_ARM), List.of());
                require(members.equals(expected.stream().map(s -> s.header().id()).collect(java.util.stream.Collectors.toSet())), "EVALUATE membership is complete");
                e.normalContinuation().statement().ifPresent(id -> require(statements.containsKey(id)
                        && !id.equals(e.header().id()) && !members.contains(id), "EVALUATE continuation is outside arms"));
            }
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

    private static void validateEvaluateArm(EvaluateFact owner, IfArm arm, List<StatementId> body,
            Map<StatementId, StatementFact> statements, Set<StatementId> members) {
        require(arm.presence()!=ClausePresence.ABSENT || body.isEmpty(), "absent OTHER has no body");
        arm.entry().statement().ifPresent(id -> require(!body.isEmpty() && body.get(0).equals(id), "arm entry is explicit first member"));
        for (var id : body) require(members.add(id) && statements.containsKey(id)
                && statements.get(id).header().containment().equals(new Containment(Optional.of(owner.header().id()), Branch.EVALUATE_ARM)), "EVALUATE direct member belongs to one arm");
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
