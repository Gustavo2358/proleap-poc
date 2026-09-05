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
    public static final String CONTRACT_VERSION = "1.0.0";

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

    /** Writes exactly the bytes returned by {@link #serialize(CobolSemanticPort)}. */
    public static void write(CobolSemanticPort port, Path destination) throws IOException {
        Files.write(Objects.requireNonNull(destination, "destination"), serialize(port));
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
                        List.copyOf(branches)), gaps, coverage(port.coverage()));
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
                readiness(declaration.readiness()));
    }

    private static StatementDocument statement(CobolSemanticProduct.StatementFact fact) {
        if (fact instanceof CobolSemanticProduct.MoveFact move) {
            return new MoveDocument(header(move.header()), literal(move.source()),
                    dataReference(move.target()));
        }
        if (fact instanceof CobolSemanticProduct.CallFact call) {
            return new CallDocument(header(call.header()), call.syntax(),
                    dataReference(call.operand()), call.runtimeTarget(),
                    call.runtimeUncertaintyCode());
        }
        if (fact instanceof CobolSemanticProduct.IfFact branch) {
            return new IfDocument(header(branch.header()), condition(branch.condition()),
                    branch.explicitlyTerminated(), branch.continuation()
                    .map(SemanticProductJsonWriter::statementHandle).orElse(null));
        }
        if (fact instanceof CobolSemanticProduct.ObservedStatement observed) {
            return new ObservedDocument(header(observed.header()), observed.observedKind(),
                    observed.observedShape(), observed.gapCode());
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

    private static LiteralDocument literal(CobolSemanticProduct.LiteralSource source) {
        return new LiteralDocument(operandHandle(source.id()), source.kind(), source.value(),
                provenance(source.provenance()));
    }

    private static DataReferenceDocument dataReference(
            CobolSemanticProduct.DataReference reference) {
        return new DataReferenceDocument(operandHandle(reference.id()), reference.role(),
                binding(reference.binding()), provenance(reference.provenance()));
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
                provenance(condition.provenance()));
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
            "dataDeclarations", "statements", "structure", "gaps", "coverage"})
    private record SemanticProductDocument(
            String schema,
            String contractVersion,
            UnitDocument unit,
            PolicyDocument policy,
            List<DataDeclarationDocument> dataDeclarations,
            List<StatementDocument> statements,
            StructureDocument structure,
            List<GapDocument> gaps,
            CoverageDocument coverage) { }

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
            ReadinessDocument readiness) { }

    private record StructureDocument(List<String> roots,
                                     List<BranchChildrenDocument> branches) { }

    private record BranchChildrenDocument(String parent, CobolSemanticProduct.Branch branch,
                                          List<String> children) { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "variant")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MoveDocument.class, name = "MOVE"),
            @JsonSubTypes.Type(value = CallDocument.class, name = "CALL"),
            @JsonSubTypes.Type(value = IfDocument.class, name = "IF"),
            @JsonSubTypes.Type(value = ObservedDocument.class, name = "OBSERVED")
    })
    private sealed interface StatementDocument permits MoveDocument, CallDocument,
            IfDocument, ObservedDocument { }

    @JsonPropertyOrder({"variant", "header", "source", "target"})
    private record MoveDocument(StatementHeaderDocument header, LiteralDocument source,
                                DataReferenceDocument target) implements StatementDocument { }

    @JsonPropertyOrder({"variant", "header", "syntax", "operand", "runtimeTarget",
            "runtimeUncertaintyCode"})
    private record CallDocument(StatementHeaderDocument header,
                                CobolSemanticProduct.CallSyntax syntax,
                                DataReferenceDocument operand,
                                CobolSemanticProduct.RuntimeTargetKnowledge runtimeTarget,
                                String runtimeUncertaintyCode) implements StatementDocument { }

    @JsonPropertyOrder({"variant", "header", "condition", "explicitlyTerminated",
            "continuation"})
    private record IfDocument(StatementHeaderDocument header, ConditionDocument condition,
                              boolean explicitlyTerminated,
                              String continuation) implements StatementDocument { }

    @JsonPropertyOrder({"variant", "header", "observedKind", "observedShape", "gapCode"})
    private record ObservedDocument(StatementHeaderDocument header, String observedKind,
                                    String observedShape,
                                    String gapCode) implements StatementDocument { }

    private record StatementHeaderDocument(
            String id,
            int programPoint,
            ContainmentDocument containment,
            ProvenanceDocument provenance,
            CobolSemanticProduct.CoverageStatus coverage,
            ReadinessDocument readiness) { }

    private record ContainmentDocument(String parent, CobolSemanticProduct.Branch branch) { }

    private record LiteralDocument(String id, CobolSemanticProduct.LiteralKind kind,
                                   String value, ProvenanceDocument provenance) { }

    private record DataReferenceDocument(String id, CobolSemanticProduct.OperandRole role,
                                         BindingDocument binding,
                                         ProvenanceDocument provenance) { }

    private record BindingDocument(
            CobolSemanticProduct.ResolutionStatus status,
            CobolSemanticProduct.ResolutionReason reason,
            List<DataCandidateDocument> candidates,
            String selected) { }

    private record DataCandidateDocument(String id, String canonicalName) { }

    private record ConditionDocument(String shape, List<DataReferenceDocument> references,
                                     ProvenanceDocument provenance) { }

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
