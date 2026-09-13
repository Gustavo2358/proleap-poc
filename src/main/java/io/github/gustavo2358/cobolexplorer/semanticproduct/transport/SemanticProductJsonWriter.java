package io.github.gustavo2358.cobolexplorer.semanticproduct.transport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic JSON output adapter for one closed Semantic Product publication.
 *
 * <p>Handles are deterministic inside an equivalent, versioned publication.
 * They are deliberately not persistent identities across structural source edits,
 * analyzer changes or contract-version changes.</p>
 */
public final class SemanticProductJsonWriter {
    public static final String SCHEMA = "cobol-semantic-product";
    public static final String CONTRACT_VERSION = "1.8.0";

    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .defaultPropertyInclusion(JsonInclude.Value.construct(
                    JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS))
            .build();

    private SemanticProductJsonWriter() { }

    /** Materializes typed transport DTOs and serializes them as deterministic UTF-8 bytes. */
    public static byte[] serialize(CobolSemanticPort port) throws IOException {
        return JSON.writeValueAsBytes(document(Objects.requireNonNull(port, "port")));
    }

    /** Writes the same deterministic UTF-8 bytes as {@link #serialize(CobolSemanticPort)}
     * directly to the destination, without materializing the complete JSON byte array.
     * The transport DTO graph is still materialized. */
    public static void write(CobolSemanticPort port, Path destination) throws IOException {
        Objects.requireNonNull(destination, "destination");
        var publication = document(Objects.requireNonNull(port, "port"));
        try (var output = Files.newOutputStream(destination)) {
            JSON.writeValue(output, publication);
        }
    }

    private static SemanticProductDocument document(CobolSemanticPort port) {
        List<DataDeclarationDocument> declarations = port.dataDeclarations().stream()
                .map(SemanticProductJsonWriter::dataDeclaration).toList();
        List<StatementDocument> statements = port.statements().stream()
                .map(SemanticProductJsonWriter::statement).toList();
        List<GapDocument> gaps = port.gaps().stream()
                .map(SemanticProductJsonWriter::gap).toList();

        List<BranchChildrenDocument> branches = new ArrayList<>();
        for (CobolSemanticProduct.StatementFact fact : port.statements()) {
            if (!(fact instanceof CobolSemanticProduct.IfFact branch)) continue;
            addBranch(port, branches, branch.header().id(), CobolSemanticProduct.Branch.THEN);
            addBranch(port, branches, branch.header().id(), CobolSemanticProduct.Branch.ELSE);
        }

        return new SemanticProductDocument(SCHEMA, CONTRACT_VERSION,
                unit(port.unit()), policy(port.policy()), declarations, statements,
                new StructureDocument(port.rootStatements().stream()
                        .map(SemanticProductJsonWriter::statementHandle).toList(),
                        List.copyOf(branches)), gaps, coverage(port.coverage()),
                entryInventory(port.entryInventory()), storageIndependence(port.storageIndependence()));
    }

    private static EntryInventoryDocument entryInventory(CobolSemanticProduct.EntryInventory inventory) {
        return new EntryInventoryDocument(inventory.status(), inventory.scope(),
                inventory.entries().stream().map(entry -> new EntryDocument(
                        "entry:" + entry.id().localId(), entry.role(), entry.availability(),
                        new ExecutableStartDocument(entry.start().availability(), entry.start().statement()
                                .map(SemanticProductJsonWriter::statementHandle).orElse(null)),
                        new EntrySignatureDocument(entry.signature().availability(),
                                entry.signature().parameterCount().orElse(null), entry.signature().returningClause()),
                        provenance(entry.provenance()), entry.coverage(), readiness(entry.readiness()),
                        entry.gaps().stream().map(gap -> new EntryGapDocument(gap.scope(), gap.code(),
                                gap.detail(), provenance(gap.provenance()))).toList())).toList(), inventory.gapCodes());
    }

    private static void addBranch(CobolSemanticPort port,
                                  List<BranchChildrenDocument> output,
                                  CobolSemanticProduct.StatementId parent,
                                  CobolSemanticProduct.Branch branch) {
        output.add(new BranchChildrenDocument(statementHandle(parent), branch,
                port.children(parent, branch).stream().map(fact ->
                        statementHandle(fact.header().id())).toList()));
    }

    private static UnitDocument unit(CobolSemanticProduct.UnitId unit) {
        return new UnitDocument(unit.compilationUnitId(), unit.structuralPath(),
                unit.canonicalProgramName());
    }

    private static PolicyDocument policy(CobolSemanticProduct.Policy policy) {
        return new PolicyDocument(policy.policyId(), policy.version(), policy.qualifyMode(),
                policy.pgmnameMode(), policy.dynamMode(), policy.dllMode());
    }

    private static DataDeclarationDocument dataDeclaration(
            CobolSemanticProduct.DataDeclaration declaration) {
        return new DataDeclarationDocument(dataHandle(declaration.id()),
                declaration.canonicalName(), declaration.picture().orElse(null),
                provenance(declaration.provenance()), declaration.coverage(),
                readiness(declaration.readiness()), declaration.scalarText().map(shape -> new ScalarTextDocument(
                        shape.logicalDomain(), shape.logicalExtent(), shape.storageClass(), shape.declarationScope())).orElse(null));
    }

    private static CallTargetDocument callTarget(CobolSemanticProduct.CallTarget target) {
        if (target instanceof CobolSemanticProduct.DataReference data)
            return new DataCallTargetDocument(dataReference(data));
        var literal = (CobolSemanticProduct.LiteralCallTarget) target;
        return new LiteralCallTargetDocument(operandHandle(literal.id()), literal.text(), literal.writtenText(),
                literal.logicalValue().map(t -> new TextValueDocument(t.logicalDomain(), t.value(), t.logicalExtent())).orElse(null),
                provenance(literal.provenance()));
    }

    private static StatementDocument statement(CobolSemanticProduct.StatementFact fact) {
        if (fact instanceof CobolSemanticProduct.PerformFact perform) {
            return new PerformDocument(header(perform.header()), perform.profile(), perform.target().map(t ->
                new PerformTargetDocument("procedure:" + t.id().localId(), provenance(t.referenceOrigin()), provenance(t.paragraphOrigin()))).orElse(null),
                perform.targetEntry().map(SemanticProductJsonWriter::statementHandle).orElse(null),
                perform.targetStatements().stream().map(SemanticProductJsonWriter::statementHandle).toList(),
                perform.targetExit().map(SemanticProductJsonWriter::statementHandle).orElse(null), continuation(perform.normalContinuation()),
                perform.gapCodes());
        }
        if (fact instanceof CobolSemanticProduct.GobackFact goback)
            return new GobackDocument(header(goback.header()), goback.exit(), goback.localContinuation());
        if (fact instanceof CobolSemanticProduct.MoveFact move) {
            return new MoveDocument(header(move.header()), moveSource(move.source()),
                    dataReference(move.target()), move.copySemantics(), new ContinuationDocument(
                            move.normalContinuation().availability(), move.normalContinuation().statement()
                            .map(SemanticProductJsonWriter::statementHandle).orElse(null),
                            provenance(move.normalContinuation().provenance())), move.textAdjustment().map(a ->
                            new TextAdjustmentDocument(a.rule(), a.receiverExtent(),
                                    new TextValueDocument(a.result().logicalDomain(), a.result().value(), a.result().logicalExtent()),
                                    provenance(a.provenance()))).orElse(null));
        }
        if (fact instanceof CobolSemanticProduct.CallFact call) {
            return new CallDocument(header(call.header()), call.syntax(),
                    callTarget(call.target()), call.runtimeTarget(), call.runtimeUncertaintyCode(),
                    new ContinuationDocument(call.normalContinuation().availability(), call.normalContinuation().statement()
                            .map(SemanticProductJsonWriter::statementHandle).orElse(null), provenance(call.normalContinuation().provenance())),
                    new CallSurfaceDocument(call.surface().using(), call.surface().argumentCount().orElse(null),
                            call.surface().returning(), call.surface().onException(), call.surface().notOnException(),
                            call.surface().onOverflow()), call.effects(), call.outcomes());
        }
        if (fact instanceof CobolSemanticProduct.IfFact branch) {
            return new IfDocument(header(branch.header()), condition(branch.condition()),
                    branch.explicitlyTerminated(), branch.continuation()
                    .map(SemanticProductJsonWriter::statementHandle).orElse(null),
                    continuation(branch.normalContinuation()), arm(branch.thenArm()), arm(branch.elseArm()), branch.profile());
        }
        if (fact instanceof CobolSemanticProduct.ObservedStatement observed) {
            return new ObservedDocument(header(observed.header()), observed.observedKind(),
                    observed.observedShape(), observed.gapCode(), continuation(observed.normalContinuation()), observed.knownReferences().stream().map(SemanticProductJsonWriter::dataReference).toList());
        }
        throw new IllegalArgumentException("unsupported Semantic Product statement fact: "
                + fact.getClass().getName());
    }

    private static StatementHeaderDocument header(CobolSemanticProduct.StatementHeader header) {
        return new StatementHeaderDocument(statementHandle(header.id()),
                header.point().ordinal(), containment(header.containment()),
                provenance(header.provenance()), header.coverage(),
                readiness(header.readiness()));
    }

    private static ContainmentDocument containment(
            CobolSemanticProduct.Containment containment) {
        return new ContainmentDocument(containment.parent()
                .map(SemanticProductJsonWriter::statementHandle).orElse(null),
                containment.branch());
    }

    private static MoveSourceDocument moveSource(CobolSemanticProduct.MoveSource source) {
        if (source instanceof CobolSemanticProduct.LiteralSource literal) return literal(literal);
        return new DataMoveSourceDocument(dataReference((CobolSemanticProduct.DataReference) source));
    }

    private static LiteralDocument literal(CobolSemanticProduct.LiteralSource source) {
        return new LiteralDocument(operandHandle(source.id()), source.kind(), source.value(),
                provenance(source.provenance()), source.logicalValue().map(value -> new TextValueDocument(
                        value.logicalDomain(), value.value(), value.logicalExtent())).orElse(null));
    }

    private static DataReferenceDocument dataReference(
            CobolSemanticProduct.DataReference reference) {
        return new DataReferenceDocument(operandHandle(reference.id()), reference.role(),
                binding(reference.binding()), provenance(reference.provenance()), reference.wholeItemAccess()
                        .map(access -> new WholeItemDocument(dataHandle(access.data()))).orElse(null));
    }

    private static BindingDocument binding(CobolSemanticProduct.NominalBinding binding) {
        return new BindingDocument(binding.status(), binding.reason(),
                binding.candidates().stream().map(candidate -> new DataCandidateDocument(
                        dataHandle(candidate.id()), candidate.canonicalName())).toList(),
                binding.selected().map(SemanticProductJsonWriter::dataHandle).orElse(null));
    }

    private static ConditionDocument condition(CobolSemanticProduct.ConditionSurface condition) {
        return new ConditionDocument(condition.shape(), condition.references().stream()
                .map(SemanticProductJsonWriter::dataReference).toList(),
                provenance(condition.provenance()), predicate(condition.predicate()));
    }

    private static PredicateDocument predicate(CobolSemanticProduct.PredicateGuarantee p) {
        return new PredicateDocument(p.availability(), p.profile(), p.resultDomain(), p.evaluation(), p.normalCompletion(),
                p.knownReads().stream().map(SemanticProductJsonWriter::operandHandle).toList(), p.readsCompleteness(), p.truthValue(),
                provenance(p.provenance()), p.gapCodes());
    }
    private static ContinuationDocument continuation(CobolSemanticProduct.NormalContinuation c) {
        return new ContinuationDocument(c.availability(), c.statement().map(SemanticProductJsonWriter::statementHandle).orElse(null), provenance(c.provenance()));
    }
    private static IfArmDocument arm(CobolSemanticProduct.IfArm arm) {
        return new IfArmDocument(arm.presence(), arm.contentAvailability(),
                new ExecutableStartDocument(arm.entry().availability(), arm.entry().statement().map(SemanticProductJsonWriter::statementHandle).orElse(null)),
                provenance(arm.provenance()), arm.gapCodes());
    }
    private static IndependentStorageDocument storageIndependence(CobolSemanticProduct.IndependentStorageSet p) {
        return new IndependentStorageDocument(p.availability(), p.rule(), p.authority(),
                p.members().stream().map(SemanticProductJsonWriter::dataHandle).toList(),
                p.provenance().map(SemanticProductJsonWriter::provenance).orElse(null), p.gapCodes());
    }

    private static GapDocument gap(CobolSemanticProduct.Gap gap) {
        return new GapDocument(statementHandle(gap.statement()), gap.scope(), gap.code(),
                gap.detail(), provenance(gap.provenance()));
    }

    private static CoverageDocument coverage(CobolSemanticProduct.CoverageSummary coverage) {
        return new CoverageDocument(coverage.inventoryStatus(), coverage.observedStatements(),
                coverage.modeledStatements(), coverage.partialStatements(),
                coverage.unsupportedStatements(), coverage.inputMissingStatements(),
                readiness(coverage.readiness()));
    }

    private static ReadinessDocument readiness(CobolSemanticProduct.Readiness readiness) {
        return new ReadinessDocument(readinessClaim(readiness.lowering()),
                readinessClaim(readiness.cfg()), readinessClaim(readiness.effectsDataflow()));
    }

    private static ReadinessClaimDocument readinessClaim(
            CobolSemanticProduct.ReadinessClaim claim) {
        return new ReadinessClaimDocument(claim.status(), claim.scope());
    }

    private static ProvenanceDocument provenance(CobolSemanticProduct.Provenance provenance) {
        return new ProvenanceDocument(location(provenance.expanded()),
                location(provenance.original()), provenance.includeChain().stream()
                .map(SemanticProductJsonWriter::includeFrame).toList(), provenance.exact());
    }

    private static LocationDocument location(CobolSemanticProduct.Location location) {
        return new LocationDocument(location.file(), location.startLine(), location.startColumn(),
                location.endLine(), location.endColumn());
    }

    private static IncludeFrameDocument includeFrame(CobolSemanticProduct.IncludeFrame frame) {
        return new IncludeFrameDocument(frame.includingFile(), frame.requestedName(),
                frame.includedFile(), frame.includeLine());
    }

    private static String dataHandle(CobolSemanticProduct.DataItemId id) {
        return "data:" + id.localId();
    }

    private static String statementHandle(CobolSemanticProduct.StatementId id) {
        return "statement:" + id.localId();
    }

    private static String operandHandle(CobolSemanticProduct.OperandId id) {
        return "operand:" + id.statement().localId() + ':' + id.localId();
    }

    @JsonPropertyOrder({"schema", "contractVersion", "unit", "policy",
            "dataDeclarations", "statements", "structure", "gaps", "coverage", "entryInventory", "storageIndependence"})
    private record SemanticProductDocument(
            String schema,
            String contractVersion,
            UnitDocument unit,
            PolicyDocument policy,
            List<DataDeclarationDocument> dataDeclarations,
            List<StatementDocument> statements,
            StructureDocument structure,
            List<GapDocument> gaps,
            CoverageDocument coverage,
            EntryInventoryDocument entryInventory, IndependentStorageDocument storageIndependence) { }

    private record EntryInventoryDocument(CobolSemanticProduct.InventoryStatus status,
                                          CobolSemanticProduct.EntryInventoryScope scope,
                                          List<EntryDocument> entries, List<String> gapCodes) { }

    private record EntryDocument(String id, CobolSemanticProduct.EntryRole role,
                                  CobolSemanticProduct.Availability availability,
                                  ExecutableStartDocument start, EntrySignatureDocument signature,
                                  ProvenanceDocument provenance, CobolSemanticProduct.CoverageStatus coverage,
                                  ReadinessDocument readiness, List<EntryGapDocument> gaps) { }

    private record ExecutableStartDocument(CobolSemanticProduct.Availability availability,
                                           String statement) { }

    private record EntrySignatureDocument(CobolSemanticProduct.Availability availability,
                                          Integer parameterCount,
                                          CobolSemanticProduct.ReturningClause returningClause) { }

    private record EntryGapDocument(CobolSemanticProduct.GapScope scope, String code,
                                    String detail, ProvenanceDocument provenance) { }

    private record UnitDocument(String compilationUnitId, List<Integer> structuralPath,
                                String canonicalProgramName) { }

    private record PolicyDocument(String policyId, String version,
                                  CobolSemanticProduct.QualifyMode qualifyMode,
                                  CobolSemanticProduct.PgmnameMode pgmnameMode,
                                  CobolSemanticProduct.DynamMode dynamMode,
                                  CobolSemanticProduct.DllMode dllMode) { }

    private record DataDeclarationDocument(
            String id,
            String canonicalName,
            String picture,
            ProvenanceDocument provenance,
            CobolSemanticProduct.CoverageStatus coverage,
            ReadinessDocument readiness, ScalarTextDocument scalarText) { }

    private record StructureDocument(List<String> roots,
                                     List<BranchChildrenDocument> branches) { }

    private record BranchChildrenDocument(String parent, CobolSemanticProduct.Branch branch,
                                          List<String> children) { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "variant")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MoveDocument.class, name = "MOVE"),
            @JsonSubTypes.Type(value = CallDocument.class, name = "CALL"),
            @JsonSubTypes.Type(value = IfDocument.class, name = "IF"),
            @JsonSubTypes.Type(value = GobackDocument.class, name = "GOBACK"),
            @JsonSubTypes.Type(value = PerformDocument.class, name = "PERFORM"),
            @JsonSubTypes.Type(value = ObservedDocument.class, name = "OBSERVED")
    })
    private sealed interface StatementDocument permits MoveDocument, CallDocument,
            IfDocument, ObservedDocument, GobackDocument, PerformDocument { }

    private record PerformTargetDocument(String id, ProvenanceDocument referenceOrigin, ProvenanceDocument paragraphOrigin) { }
    private record PerformDocument(StatementHeaderDocument header, CobolSemanticProduct.PerformProfile profile,
        PerformTargetDocument target, String targetEntry, List<String> targetStatements, String targetExit,
        ContinuationDocument normalContinuation, List<String> gapCodes) implements StatementDocument { }

    @JsonPropertyOrder({"variant", "header", "exit", "localContinuation"})
    private record GobackDocument(StatementHeaderDocument header, CobolSemanticProduct.GobackExit exit,
                                   CobolSemanticProduct.LocalContinuation localContinuation)
            implements StatementDocument { }

    @JsonPropertyOrder({"variant", "header", "source", "target", "copySemantics", "normalContinuation", "textAdjustment"})
    private record MoveDocument(StatementHeaderDocument header, MoveSourceDocument source,
                                DataReferenceDocument target, CobolSemanticProduct.CopySemantics copySemantics,
                                ContinuationDocument normalContinuation, TextAdjustmentDocument textAdjustment) implements StatementDocument { }

    private record TextAdjustmentDocument(CobolSemanticProduct.TextAdjustmentRule rule, int receiverExtent,
                                            TextValueDocument result, ProvenanceDocument provenance) { }
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({@JsonSubTypes.Type(value = DataCallTargetDocument.class, name = "DATA"),
            @JsonSubTypes.Type(value = LiteralCallTargetDocument.class, name = "LITERAL")})
    private sealed interface CallTargetDocument permits DataCallTargetDocument, LiteralCallTargetDocument { }
    private record DataCallTargetDocument(DataReferenceDocument reference) implements CallTargetDocument { }
    private record LiteralCallTargetDocument(String id, String text, String writtenText, TextValueDocument logicalValue,
                                              ProvenanceDocument provenance) implements CallTargetDocument { }
    private record CallSurfaceDocument(CobolSemanticProduct.ClausePresence using, Integer argumentCount,
                                      CobolSemanticProduct.ClausePresence returning, CobolSemanticProduct.ClausePresence onException,
                                      CobolSemanticProduct.ClausePresence notOnException, CobolSemanticProduct.ClausePresence onOverflow) { }
    @JsonPropertyOrder({"variant", "header", "syntax", "target", "runtimeTarget", "runtimeUncertaintyCode",
            "normalContinuation", "surface", "effects", "outcomes"})
    private record CallDocument(StatementHeaderDocument header, CobolSemanticProduct.CallSyntax syntax,
                                CallTargetDocument target, CobolSemanticProduct.RuntimeTargetKnowledge runtimeTarget,
                                String runtimeUncertaintyCode, ContinuationDocument normalContinuation, CallSurfaceDocument surface,
                                CobolSemanticProduct.CallEffects effects, CobolSemanticProduct.CallOutcomes outcomes) implements StatementDocument { }

    @JsonPropertyOrder({"variant", "header", "condition", "explicitlyTerminated",
            "continuation"})
    private record IfDocument(StatementHeaderDocument header, ConditionDocument condition,
                              boolean explicitlyTerminated,
                              String continuation, ContinuationDocument normalContinuation,
                              IfArmDocument thenArm, IfArmDocument elseArm, CobolSemanticProduct.IfProfile profile) implements StatementDocument { }

    @JsonPropertyOrder({"variant", "header", "observedKind", "observedShape", "gapCode"})
    private record ObservedDocument(StatementHeaderDocument header, String observedKind,
                                    String observedShape,
                                    String gapCode, ContinuationDocument normalContinuation, List<DataReferenceDocument> knownReferences) implements StatementDocument { }

    private record StatementHeaderDocument(
            String id,
            int programPoint,
            ContainmentDocument containment,
            ProvenanceDocument provenance,
            CobolSemanticProduct.CoverageStatus coverage,
            ReadinessDocument readiness) { }

    private record ContainmentDocument(String parent, CobolSemanticProduct.Branch branch) { }

    private record ScalarTextDocument(CobolSemanticProduct.LogicalDomain logicalDomain, int logicalExtent,
                                      CobolSemanticProduct.StorageClass storageClass,
                                      CobolSemanticProduct.DeclarationScope declarationScope) { }
    private record TextValueDocument(CobolSemanticProduct.LogicalDomain logicalDomain, String value,
                                     int logicalExtent) { }
    private record WholeItemDocument(String data) { }
    private record ContinuationDocument(CobolSemanticProduct.ContinuationAvailability availability,
                                        String statement, ProvenanceDocument provenance) { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "variant")
    @JsonSubTypes({@JsonSubTypes.Type(value = LiteralDocument.class, name = "LITERAL"),
            @JsonSubTypes.Type(value = DataMoveSourceDocument.class, name = "DATA")})
    private sealed interface MoveSourceDocument permits LiteralDocument, DataMoveSourceDocument { }
    private record DataMoveSourceDocument(DataReferenceDocument reference) implements MoveSourceDocument { }

    private record LiteralDocument(String id, CobolSemanticProduct.LiteralKind kind,
                                   String value, ProvenanceDocument provenance, TextValueDocument logicalValue) implements MoveSourceDocument { }

    private record DataReferenceDocument(String id, CobolSemanticProduct.OperandRole role,
                                         BindingDocument binding,
                                         ProvenanceDocument provenance, WholeItemDocument wholeItemAccess) { }

    private record BindingDocument(
            CobolSemanticProduct.ResolutionStatus status,
            CobolSemanticProduct.ResolutionReason reason,
            List<DataCandidateDocument> candidates,
            String selected) { }

    private record DataCandidateDocument(String id, String canonicalName) { }

    private record ConditionDocument(String shape, List<DataReferenceDocument> references,
                                     ProvenanceDocument provenance, PredicateDocument predicate) { }

    private record PredicateDocument(CobolSemanticProduct.Availability availability, CobolSemanticProduct.PredicateProfile profile,
            CobolSemanticProduct.PredicateDomain resultDomain, CobolSemanticProduct.PredicateEvaluation evaluation,
            CobolSemanticProduct.PredicateCompletion normalCompletion, List<String> knownReads,
            CobolSemanticProduct.ReadsCompleteness readsCompleteness, CobolSemanticProduct.PredicateTruth truthValue,
            ProvenanceDocument provenance, List<String> gapCodes) { }
    private record IfArmDocument(CobolSemanticProduct.ClausePresence presence, CobolSemanticProduct.Availability contentAvailability,
            ExecutableStartDocument entry, ProvenanceDocument provenance, List<String> gapCodes) { }
    private record IndependentStorageDocument(CobolSemanticProduct.Availability availability,
            CobolSemanticProduct.StorageIndependenceRule rule, String authority, List<String> members,
            ProvenanceDocument provenance, List<String> gapCodes) { }

    private record GapDocument(String statement, CobolSemanticProduct.GapScope scope,
                               String code, String detail,
                               ProvenanceDocument provenance) { }

    private record CoverageDocument(
            CobolSemanticProduct.InventoryStatus inventoryStatus,
            int observedStatements,
            int modeledStatements,
            int partialStatements,
            int unsupportedStatements,
            int inputMissingStatements,
            ReadinessDocument readiness) { }

    private record ReadinessDocument(ReadinessClaimDocument lowering,
                                     ReadinessClaimDocument cfg,
                                     ReadinessClaimDocument effectsDataflow) { }

    private record ReadinessClaimDocument(CobolSemanticProduct.ReadinessStatus status,
                                          String scope) { }

    private record ProvenanceDocument(LocationDocument expanded, LocationDocument original,
                                      List<IncludeFrameDocument> includeChain,
                                      boolean exact) { }

    private record LocationDocument(String file, int startLine, int startColumn,
                                    int endLine, int endColumn) { }

    private record IncludeFrameDocument(String includingFile, String requestedName,
                                        String includedFile, int includeLine) { }
}
