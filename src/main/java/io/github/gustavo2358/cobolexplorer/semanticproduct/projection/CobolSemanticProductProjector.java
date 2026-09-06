package io.github.gustavo2358.cobolexplorer.semanticproduct.projection;

import io.github.gustavo2358.cobolexplorer.Ast;
import io.github.gustavo2358.cobolexplorer.CompilationUnitBuildResult;
import io.github.gustavo2358.cobolexplorer.CompilationUnitModel;
import io.github.gustavo2358.cobolexplorer.CompilationUnitSymbolTables;
import io.github.gustavo2358.cobolexplorer.ExternalClassification;
import io.github.gustavo2358.cobolexplorer.ReferenceOccurrences;
import io.github.gustavo2358.cobolexplorer.ReferenceResolution;
import io.github.gustavo2358.cobolexplorer.ResolutionAnalysisReport;
import io.github.gustavo2358.cobolexplorer.ResolutionContracts;
import io.github.gustavo2358.cobolexplorer.SemanticCoverage;
import io.github.gustavo2358.cobolexplorer.SymbolTable;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.*;

/**
 * Production projection seam from closed canonical frontend products to the
 * boundary-owned Semantic Product.
 *
 * <p>The current projection capability covers DATA declarations in the selected
 * unit, typed MOVE/CALL/IF facts and one explicit observed fact for every other
 * statement in the canonical AST inventory. Direct IF branch membership and
 * safe structural continuations come from the typed AST; statements nested
 * under structural families not yet projected retain unknown containment. The
 * projector only translates and reconciles facts; it performs no parsing,
 * nominal lookup, resolution or runtime inference.</p>
 */
public final class CobolSemanticProductProjector {
    private static final String DYNAMIC_TARGET_GAP =
            "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN";
    private static final String LITERAL_KIND_GAP = "LITERAL_KIND_NOT_PUBLISHED";
    private static final String CONTAINMENT_GAP = "CONTAINMENT_NOT_PROJECTED";
    private static final String CONDITION_SEMANTICS_GAP =
            "CONDITION_SEMANTICS_NOT_AVAILABLE";
    private static final String CONDITION_REFERENCE_GAP =
            "CONDITION_REFERENCE_KIND_NOT_PROJECTED";
    private static final String CONTINUATION_GAP = "CONTINUATION_NOT_PROJECTED";
    private static final String BRANCH_CONTENT_GAP = "BRANCH_CONTENT_NOT_PROJECTED";
    private static final String OBSERVED_PARTIAL_GAP = "OBSERVED_STATEMENT_PARTIAL";
    private static final String OBSERVED_UNSUPPORTED_GAP =
            "OBSERVED_STATEMENT_UNSUPPORTED";
    private static final String OBSERVED_INPUT_MISSING_GAP =
            "OBSERVED_STATEMENT_INPUT_MISSING";

    private CobolSemanticProductProjector() { }

    /** Immutable frontend publications from one completed analysis. */
    public record FrontendProducts(
            CompilationUnitBuildResult frontend,
            CompilationUnitSymbolTables symbolTables,
            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrencesByUnit,
            ReferenceResolution resolution,
            ResolutionAnalysisReport report) {
        public FrontendProducts {
            frontend = Objects.requireNonNull(frontend, "frontend");
            symbolTables = Objects.requireNonNull(symbolTables, "symbolTables");
            occurrencesByUnit = immutableOccurrences(occurrencesByUnit);
            resolution = Objects.requireNonNull(resolution, "resolution");
            report = Objects.requireNonNull(report, "report");
        }

        private static Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences>
        immutableOccurrences(Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> source) {
            Objects.requireNonNull(source, "occurrencesByUnit");
            return Collections.unmodifiableMap(new LinkedHashMap<>(source));
        }
    }

    /** Materializes the complete observed statement inventory for one selected unit. */
    public static CobolSemanticProduct.State project(
            FrontendProducts products, ResolutionContracts.ProgramUnitId unitId) {
        Objects.requireNonNull(products, "products");
        Objects.requireNonNull(unitId, "unitId");
        ProjectionInputs inputs = ProjectionInputs.index(products, unitId);

        List<StatementPlan> plans = new ArrayList<>(inputs.statementPositions().size());
        LinkedHashSet<ResolutionContracts.SemanticEntityId> referencedData =
                new LinkedHashSet<>();
        for (StatementPosition position : inputs.statementPositions()) {
            StatementPlan plan = plan(position, inputs);
            plans.add(plan);
            if (plan.capability().supported()) {
                for (ReferenceResolution.Entry entry : plan.entries()) {
                    if (!projectableDataBinding(entry, inputs)) continue;
                    for (ReferenceResolution.Candidate candidate : entry.candidates())
                        referencedData.add(requireDataCandidate(candidate).entityId());
                }
            }
        }

        DeclarationProjection declarations = declarations(inputs, referencedData);
        List<CobolSemanticProduct.StatementFact> statements = new ArrayList<>(plans.size());
        List<CobolSemanticProduct.Gap> gaps = new ArrayList<>();
        Map<Ast.Statement, CobolSemanticProduct.StatementId> statementIds =
                new IdentityHashMap<>();
        // Additive fact families receive handles after the previously published
        // MOVE/CALL and IF namespaces. Structural order remains the independent
        // ProgramPoint and now covers the complete observed statement inventory.
        int nextStatementId = 0;
        for (StatementPlan plan : plans) {
            if (!(plan.position().statement() instanceof Ast.MoveStatement)
                    && !(plan.position().statement() instanceof Ast.CallStatement))
                continue;
            statementIds.put(plan.position().statement(),
                    new CobolSemanticProduct.StatementId(
                            inputs.boundaryUnit(), nextStatementId++));
        }
        for (StatementPlan plan : plans) {
            if (!(plan.position().statement() instanceof Ast.IfStatement)) continue;
            statementIds.put(plan.position().statement(),
                    new CobolSemanticProduct.StatementId(
                            inputs.boundaryUnit(), nextStatementId++));
        }
        for (StatementPlan plan : plans) {
            if (plan.position().statement() instanceof Ast.MoveStatement
                    || plan.position().statement() instanceof Ast.CallStatement
                    || plan.position().statement() instanceof Ast.IfStatement)
                continue;
            statementIds.put(plan.position().statement(),
                    new CobolSemanticProduct.StatementId(
                            inputs.boundaryUnit(), nextStatementId++));
        }
        Map<Ast.Statement, StatementPosition> positionsByStatement = new IdentityHashMap<>();
        for (StatementPosition position : inputs.statementPositions())
            positionsByStatement.put(position.statement(), position);
        Map<Ast.Statement, ContinuationProjection> continuations = new IdentityHashMap<>();
        for (StatementPlan plan : plans)
            projectStatement(plan, inputs, declarations.ids(), statementIds,
                    positionsByStatement, continuations, statements, gaps);

        CobolSemanticProduct.InventoryStatus inventoryStatus =
                inputs.report().gaps().stream().anyMatch(gap ->
                        gap.category() == ResolutionAnalysisReport.GapCategory.INPUT)
                        ? CobolSemanticProduct.InventoryStatus.INPUT_MISSING
                        : CobolSemanticProduct.InventoryStatus.COMPLETE;
        EntryInventory entries = entries(inputs, statementIds, inventoryStatus);
        if (entries.gapCodes().contains("DECLARATIVES_NOT_PROJECTED")
                && inventoryStatus == InventoryStatus.COMPLETE)
            inventoryStatus = InventoryStatus.PARTIAL;
        CobolSemanticProduct.CoverageSummary coverage = coverage(
                inventoryStatus, statements, inputs.unitSummary());
        return new CobolSemanticProduct.State(inputs.boundaryUnit(),
                policy(inputs.report().policy()), declarations.facts(),
                statements, gaps, coverage, entries);
    }

    private static EntryInventory entries(ProjectionInputs inputs,
            Map<Ast.Statement, StatementId> statementIds, InventoryStatus inventoryStatus) {
        Ast.Program program = inputs.selectedSource().unit().program();
        Ast.Division procedure = null;
        for (Ast.Division division : program.divisions()) {
            if (division.divisionKind() != Ast.DivisionKind.PROCEDURE) continue;
            require(procedure == null, "unit has duplicate PROCEDURE DIVISION");
            procedure = division;
        }
        Ast.ProcedureEntry origin = procedure == null ? null : procedure.procedureEntry().orElse(null);
        Provenance provenance = provenance(procedure == null
                ? program.meta().provenance() : procedure.meta().provenance());
        boolean inputMissing = inventoryStatus == InventoryStatus.INPUT_MISSING;
        boolean declaratives = origin != null && origin.declarativesPresent();
        Availability unavailable = inputMissing ? Availability.INPUT_MISSING : Availability.UNAVAILABLE;
        Optional<StatementId> start = Optional.empty();
        if (origin != null && !inputMissing && !declaratives && origin.startStatementId().isPresent()) {
            Ast.Node target = inputs.selectedSource().nodes().get(origin.startStatementId().get());
            require(target instanceof Ast.Statement, "canonical entry target is not a unit statement");
            StatementId id = statementIds.get((Ast.Statement) target);
            require(id != null, "canonical entry target must be published");
            start = Optional.of(id);
        }
        EntrySignature signature;
        if (origin == null || inputMissing) {
            signature = new EntrySignature(unavailable, Optional.empty(), ReturningClause.UNKNOWN);
        } else if (!origin.signatureClausesPresent()) {
            signature = new EntrySignature(Availability.KNOWN, Optional.of(0), ReturningClause.ABSENT);
        } else {
            Ast.ProcedureSignature surface = null;
            for (Ast.Node child : procedure.children()) {
                if (!(child instanceof Ast.ProcedureSignature candidate)) continue;
                require(surface == null, "procedure has duplicate signature surface");
                surface = candidate;
            }
            signature = surface == null
                    ? new EntrySignature(Availability.UNAVAILABLE, Optional.empty(), ReturningClause.UNKNOWN)
                    : new EntrySignature(Availability.PARTIAL, Optional.of(surface.parameters().size()),
                            surface.returning() == null ? ReturningClause.ABSENT : ReturningClause.PRESENT);
        }
        List<EntryGap> gaps = new ArrayList<>();
        if (start.isEmpty())
            gaps.add(new EntryGap(GapScope.ENTRY_START, "EXECUTABLE_START_NOT_AVAILABLE",
                    "primary nondeclarative start is unavailable for this input/surface", provenance));
        if (signature.availability() != Availability.KNOWN)
            gaps.add(new EntryGap(GapScope.ENTRY_SIGNATURE, "ENTRY_SIGNATURE_NOT_PROJECTED",
                    "parameter contracts and explicit result contract are not fully available", provenance));
        if (inputMissing)
            gaps.add(new EntryGap(GapScope.ANALYSIS_INPUT, "ENTRY_INPUT_INCOMPLETE",
                    "canonical input gaps prevent proving the executable entry", provenance));
        ReadinessStatus lowering = start.isEmpty() ? ReadinessStatus.BLOCKED
                : signature.availability() == Availability.KNOWN
                ? ReadinessStatus.SUFFICIENT : ReadinessStatus.PARTIAL;
        EntryFact entry = new EntryFact(new EntryId(inputs.boundaryUnit(), 0), EntryRole.PRIMARY,
                origin == null || inputMissing ? unavailable : Availability.KNOWN,
                new ExecutableStart(start.isPresent() ? Availability.KNOWN : unavailable, start),
                signature, provenance,
                inputMissing ? CoverageStatus.INPUT_MISSING : gaps.isEmpty()
                        ? CoverageStatus.MODELED : CoverageStatus.PARTIAL,
                readiness(lowering, "primary entry, canonical executable start and published signature availability",
                        start.isPresent() ? ReadinessStatus.SUFFICIENT : ReadinessStatus.BLOCKED,
                        "primary entry start only; no sequence or reachability claim",
                        ReadinessStatus.BLOCKED, "entry state, storage and effects are not published"), gaps);
        List<String> inventoryGaps = new ArrayList<>(List.of("ALTERNATE_ENTRIES_NOT_PROJECTED"));
        if (declaratives) inventoryGaps.add("DECLARATIVES_NOT_PROJECTED");
        if (inputMissing) inventoryGaps.add("ENTRY_INPUT_INCOMPLETE");
        return new EntryInventory(inputMissing ? InventoryStatus.INPUT_MISSING : InventoryStatus.PARTIAL,
                List.of(entry), inventoryGaps);
    }

    /** Opens the read-only port over the materialized projection. */
    public static CobolSemanticPort open(
            FrontendProducts products, ResolutionContracts.ProgramUnitId unitId) {
        return CobolSemanticPort.open(project(products, unitId));
    }

    private static StatementPlan plan(StatementPosition position, ProjectionInputs inputs) {
        if (position.statement() instanceof Ast.GobackStatement)
            return new StatementPlan(position, Capability.supported("GOBACK", "GOBACK_LOCAL_EXIT"), List.of());
        if (position.statement() instanceof Ast.MoveStatement move) {
            Capability capability = moveCapability(move);
            ReferenceResolution.Entry entry = null;
            if (capability.supported()) {
                Ast.DataReference target = (Ast.DataReference) move.targets().get(0);
                entry = inputs.entryFor(target);
                require(entry.occurrence().role() == ResolutionContracts.ReferenceRole.VALUE_WRITE,
                        "MOVE target role must come from the canonical occurrence");
                capability = bindingCapability(capability, entry, "MOVE");
            }
            return new StatementPlan(position, capability,
                    entry == null ? List.of() : List.of(entry));
        }

        if (position.statement() instanceof Ast.IfStatement branch)
            return new StatementPlan(position, Capability.supported("IF", "IF_STRUCTURAL"),
                    conditionEntries(branch.condition(), inputs));

        if (position.statement() instanceof Ast.CallStatement call) {
            Capability capability = callCapability(call);
            ReferenceResolution.Entry entry = null;
            if (capability.supported()) {
                Ast.DataReference operand = (Ast.DataReference) call.target();
                entry = inputs.entryFor(operand);
                require(entry.occurrence().role()
                                == ResolutionContracts.ReferenceRole.CALL_TARGET,
                        "CALL operand role must come from the canonical occurrence");
                ReferenceResolution.CallSemantics semantics = entry.callSemantics()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "CALL resolution must publish typed call semantics"));
                require(semantics.targetSyntax()
                                == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION,
                        "CALL resolution must preserve identifier/expression syntax");
                capability = bindingCapability(capability, entry, "CALL");
            } else if (call.target() != null) {
                entry = inputs.optionalEntryFor(call.target());
            }
            return new StatementPlan(position, capability,
                    entry == null ? List.of() : List.of(entry));
        }

        ObservedDescriptor observed = observedDescriptor(position.statement());
        CobolSemanticProduct.CoverageStatus observedCoverage = observedCoverage(
                inputs.finding(position.statement().meta().id()));
        return new StatementPlan(position,
                Capability.unmodeled(observed.kind(), observed.shape(),
                        observedGapCode(observedCoverage)), List.of());
    }

    private static Capability moveCapability(Ast.MoveStatement move) {
        if (move.corresponding())
            return Capability.unsupported("MOVE", "MOVE_CORRESPONDING",
                    "MOVE_CORRESPONDING_OUTSIDE_CAPABILITY");
        if (!(move.source() instanceof Ast.LiteralExpression))
            return Capability.unsupported("MOVE", "MOVE_NON_LITERAL_SOURCE",
                    "MOVE_NON_LITERAL_SOURCE_OUTSIDE_CAPABILITY");
        if (move.targets().size() != 1)
            return Capability.unsupported("MOVE", "MOVE_TARGET_CARDINALITY",
                    "MOVE_TARGET_CARDINALITY_OUTSIDE_CAPABILITY");
        if (!(move.targets().get(0) instanceof Ast.DataReference))
            return Capability.unsupported("MOVE", "MOVE_NON_DATA_TARGET",
                    "MOVE_NON_DATA_TARGET_OUTSIDE_CAPABILITY");
        return Capability.supported("MOVE", "MOVE_LITERAL_TO_DATA");
    }

    private static Capability callCapability(Ast.CallStatement call) {
        if (call.targetSyntax() != Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION)
            return Capability.unsupported("CALL", "CALL_LITERAL_TARGET",
                    "CALL_LITERAL_TARGET_OUTSIDE_CAPABILITY");
        if (!(call.target() instanceof Ast.DataReference))
            return Capability.unsupported("CALL", "CALL_NON_DATA_TARGET",
                    "CALL_NON_DATA_TARGET_OUTSIDE_CAPABILITY");
        return Capability.supported("CALL", "CALL_DATA_TARGET");
    }

    /**
     * Carries only distinctions already represented by typed AST variants and
     * fields. Generic statement metadata and source text are deliberately not
     * interpreted to recover a more specific family.
     */
    private static ObservedDescriptor observedDescriptor(Ast.Statement statement) {
        if (statement instanceof Ast.EvaluateStatement)
            return new ObservedDescriptor("EVALUATE", "TYPED_EVALUATE_STATEMENT");
        if (statement instanceof Ast.PerformStatement perform)
            return new ObservedDescriptor("PERFORM",
                    "TYPED_PERFORM_" + perform.performKind().name());
        if (statement instanceof Ast.GoToStatement goTo)
            return new ObservedDescriptor("GO_TO",
                    "TYPED_GO_TO_" + goTo.goToKind().name());
        if (statement instanceof Ast.EmbeddedLanguageStatement embedded)
            return new ObservedDescriptor("EMBEDDED_LANGUAGE",
                    "OPAQUE_" + embedded.language().name());
        if (statement instanceof Ast.NextSentenceStatement)
            return new ObservedDescriptor("NEXT_SENTENCE", "TYPED_NEXT_SENTENCE");
        if (statement instanceof Ast.SearchStatement search)
            return new ObservedDescriptor("SEARCH",
                    search.all() ? "TYPED_SEARCH_ALL" : "TYPED_SEARCH_SEQUENTIAL");
        if (statement instanceof Ast.ModeledStatement)
            return new ObservedDescriptor("MODELED_STATEMENT",
                    "GENERIC_MODELED_STATEMENT");
        if (statement instanceof Ast.PreservedStatement)
            return new ObservedDescriptor("PRESERVED_STATEMENT",
                    "GENERIC_PRESERVED_STATEMENT");
        if (statement instanceof Ast.UnsupportedStatement)
            return new ObservedDescriptor("UNSUPPORTED_STATEMENT",
                    "GENERIC_UNSUPPORTED_STATEMENT");
        throw new IllegalArgumentException(
                "canonical AST statement family is not classified for inventory: "
                        + statement.getClass().getName());
    }

    private static CobolSemanticProduct.CoverageStatus observedCoverage(
            SemanticCoverage.Finding finding) {
        CobolSemanticProduct.CoverageStatus frontendCoverage = coverage(finding);
        if (frontendCoverage == CobolSemanticProduct.CoverageStatus.INPUT_MISSING)
            return CobolSemanticProduct.CoverageStatus.INPUT_MISSING;
        if (frontendCoverage == CobolSemanticProduct.CoverageStatus.PARTIAL)
            return CobolSemanticProduct.CoverageStatus.PARTIAL;
        return CobolSemanticProduct.CoverageStatus.UNSUPPORTED;
    }

    private static String observedGapCode(CobolSemanticProduct.CoverageStatus coverage) {
        return switch (coverage) {
            case PARTIAL -> OBSERVED_PARTIAL_GAP;
            case INPUT_MISSING -> OBSERVED_INPUT_MISSING_GAP;
            case UNSUPPORTED, MODELED -> OBSERVED_UNSUPPORTED_GAP;
        };
    }

    private static Capability bindingCapability(Capability capability,
                                                ReferenceResolution.Entry entry,
                                                String statementKind) {
        if (entry.status() == ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED)
            return Capability.unsupported(statementKind,
                    statementKind + "_EXTERNAL_BINDING",
                    statementKind + "_BINDING_OUTSIDE_CAPABILITY");
        for (ReferenceResolution.Candidate candidate : entry.candidates()) {
            if (candidate.kind() != ResolutionContracts.ReferenceKind.DATA
                    || candidate.entityId().domain()
                    != ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL)
                return Capability.unsupported(statementKind,
                        statementKind + "_NON_DATA_BINDING",
                        statementKind + "_BINDING_OUTSIDE_CAPABILITY");
        }
        return capability;
    }

    private static List<ReferenceResolution.Entry> conditionEntries(
            Ast.Expression condition, ProjectionInputs inputs) {
        List<ReferenceResolution.Entry> entries = new ArrayList<>();
        collectConditionEntries(Objects.requireNonNull(condition, "IF condition"), inputs,
                entries);
        return List.copyOf(entries);
    }

    private static void collectConditionEntries(
            Ast.Node node, ProjectionInputs inputs, List<ReferenceResolution.Entry> entries) {
        ReferenceResolution.Entry entry = inputs.optionalEntryFor(node);
        if (entry != null && isConditionRead(entry.occurrence().role())) entries.add(entry);
        for (Ast.Node child : Ast.children(node))
            collectConditionEntries(child, inputs, entries);
    }

    private static boolean isConditionRead(ResolutionContracts.ReferenceRole role) {
        return role == ResolutionContracts.ReferenceRole.VALUE_READ
                || role == ResolutionContracts.ReferenceRole.SUBSCRIPT
                || role == ResolutionContracts.ReferenceRole.REFERENCE_MODIFICATION_OFFSET
                || role == ResolutionContracts.ReferenceRole.REFERENCE_MODIFICATION_LENGTH;
    }

    private static boolean projectableDataBinding(
            ReferenceResolution.Entry entry, ProjectionInputs inputs) {
        if (inputs.nominalGapSuppressedByExternalClassification(entry.occurrence())) return false;
        if (entry.status() == ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED)
            return false;
        if (entry.candidates().isEmpty())
            return entry.occurrence().admissibleKinds().equals(
                    Set.of(ResolutionContracts.ReferenceKind.DATA));
        for (ReferenceResolution.Candidate candidate : entry.candidates()) {
            if (candidate.kind() != ResolutionContracts.ReferenceKind.DATA
                    || candidate.entityId().domain()
                    != ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL)
                return false;
        }
        return true;
    }

    private static DeclarationProjection declarations(
            ProjectionInputs inputs,
            Set<ResolutionContracts.SemanticEntityId> referencedData) {
        LinkedHashMap<ResolutionContracts.SemanticEntityId, DeclarationSource> sources =
                new LinkedHashMap<>();
        for (SymbolTable.Symbol symbol : inputs.selectedSource().table().symbols()) {
            if (symbol.namespace() != SymbolTable.Namespace.DATA
                    || symbol.kind() != SymbolTable.SymbolKind.DATA_ITEM)
                continue;
            ResolutionContracts.SemanticEntityId entityId =
                    new ResolutionContracts.SemanticEntityId(inputs.unitId(),
                            ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, symbol.id());
            sources.put(entityId, inputs.declarationSource(entityId, symbol.canonicalName()));
        }
        for (ResolutionContracts.SemanticEntityId entityId : referencedData)
            sources.putIfAbsent(entityId, inputs.declarationSource(entityId, null));

        LinkedHashMap<ResolutionContracts.SemanticEntityId, CobolSemanticProduct.DataItemId> ids =
                new LinkedHashMap<>();
        List<CobolSemanticProduct.DataDeclaration> facts = new ArrayList<>(sources.size());
        int localId = 0;
        for (Map.Entry<ResolutionContracts.SemanticEntityId, DeclarationSource> item
                : sources.entrySet()) {
            CobolSemanticProduct.DataItemId id = new CobolSemanticProduct.DataItemId(
                    inputs.boundaryUnit(), localId++);
            ids.put(item.getKey(), id);
            facts.add(dataDeclaration(id, item.getValue()));
        }
        return new DeclarationProjection(Collections.unmodifiableMap(ids), List.copyOf(facts));
    }

    private static CobolSemanticProduct.DataDeclaration dataDeclaration(
            CobolSemanticProduct.DataItemId id, DeclarationSource source) {
        Optional<String> picture = Optional.empty();
        int pictureCount = 0;
        for (Ast.DataClause clause : source.entry().clauses()) {
            if (!(clause instanceof Ast.PictureClause candidate)) continue;
            pictureCount++;
            if (pictureCount == 1 && !candidate.picture().isBlank())
                picture = Optional.of(candidate.picture());
        }
        if (pictureCount != 1) picture = Optional.empty();
        CobolSemanticProduct.CoverageStatus coverage = coverage(source.finding());
        if (pictureCount > 1 && coverage == CobolSemanticProduct.CoverageStatus.MODELED)
            coverage = CobolSemanticProduct.CoverageStatus.PARTIAL;
        CobolSemanticProduct.ReadinessStatus lowering =
                coverage == CobolSemanticProduct.CoverageStatus.MODELED
                        ? CobolSemanticProduct.ReadinessStatus.SUFFICIENT
                        : coverage == CobolSemanticProduct.CoverageStatus.INPUT_MISSING
                        || coverage == CobolSemanticProduct.CoverageStatus.UNSUPPORTED
                        ? CobolSemanticProduct.ReadinessStatus.BLOCKED
                        : CobolSemanticProduct.ReadinessStatus.PARTIAL;
        return new CobolSemanticProduct.DataDeclaration(id, source.symbol().canonicalName(), picture,
                provenance(source.entry().meta().provenance()), coverage,
                readiness(lowering, "declaration surface and nominal identity available",
                        CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE,
                        "declaration alone has no control successor",
                        CobolSemanticProduct.ReadinessStatus.PARTIAL,
                        "nominal identity available; storage layout and aliases unknown"));
    }

    private static void projectStatement(
            StatementPlan plan, ProjectionInputs inputs,
            Map<ResolutionContracts.SemanticEntityId, CobolSemanticProduct.DataItemId> dataIds,
            Map<Ast.Statement, CobolSemanticProduct.StatementId> statementIds,
            Map<Ast.Statement, StatementPosition> positionsByStatement,
            Map<Ast.Statement, ContinuationProjection> continuations,
            List<CobolSemanticProduct.StatementFact> statements,
            List<CobolSemanticProduct.Gap> gaps) {
        CobolSemanticProduct.StatementId statementId = statementIds.get(
                plan.position().statement());
        require(statementId != null, "projected statement has no boundary identity");
        CobolSemanticProduct.Provenance statementProvenance =
                provenance(plan.position().statement().meta().provenance());
        CobolSemanticProduct.Containment containment = containment(
                plan.position(), statementIds);

        if (plan.position().statement() instanceof Ast.GobackStatement goback) {
            statements.add(new GobackFact(header(statementId, plan.position().ordinal(), containment,
                    statementProvenance, weakest(coverage(inputs.finding(goback.meta().id())),
                            containmentCoverage(containment)),
                    containmentReadiness(containment, readiness(
                            ReadinessStatus.SUFFICIENT, "GOBACK concludes the current program invocation",
                            ReadinessStatus.SUFFICIENT, "GOBACK has no local successor",
                            ReadinessStatus.BLOCKED, "GOBACK runtime result and lifecycle effects are not published")))));
            addContainmentGap(containment, statementId, statementProvenance, gaps);
            return;
        }

        if (!plan.capability().supported()) {
            boolean typedCapabilityFamily = plan.position().statement() instanceof Ast.MoveStatement
                    || plan.position().statement() instanceof Ast.CallStatement;
            SemanticCoverage.Finding finding = inputs.finding(
                    plan.position().statement().meta().id());
            CobolSemanticProduct.CoverageStatus statementCoverage = typedCapabilityFamily
                    ? CobolSemanticProduct.CoverageStatus.UNSUPPORTED
                    : observedCoverage(finding);
            statementCoverage = weakest(statementCoverage,
                    containmentCoverage(containment));
            CobolSemanticProduct.StatementHeader header = header(statementId,
                    plan.position().ordinal(), containment, statementProvenance,
                    statementCoverage,
                    blockedReadiness("statement shape is outside the current projection capability"));
            statements.add(new CobolSemanticProduct.ObservedStatement(header,
                    plan.capability().kind(),
                    plan.capability().shape(), plan.capability().gapCode()));
            gaps.add(new CobolSemanticProduct.Gap(statementId,
                    CobolSemanticProduct.GapScope.CAPABILITY,
                    plan.capability().gapCode(),
                    typedCapabilityFamily
                            ? "typed statement shape is outside the current MOVE/CALL capability"
                            : finding.reason(),
                    statementProvenance));
            if (statementCoverage == CobolSemanticProduct.CoverageStatus.INPUT_MISSING)
                gaps.add(new CobolSemanticProduct.Gap(statementId,
                        CobolSemanticProduct.GapScope.ANALYSIS_INPUT,
                        OBSERVED_INPUT_MISSING_GAP,
                        finding.reason(), statementProvenance));
            addContainmentGap(containment, statementId, statementProvenance, gaps);
            if (!plan.entries().isEmpty())
                addReportGaps(statementId, plan.entries().get(0).occurrence(), inputs,
                        statementProvenance, gaps);
            return;
        }

        if (plan.position().statement() instanceof Ast.MoveStatement move) {
            ReferenceResolution.Entry entry = onlyEntry(plan);
            CobolSemanticProduct.NominalBinding binding = nominalBinding(entry, dataIds);
            CobolSemanticProduct.CoverageStatus bindingCoverage = bindingCoverage(entry);
            Ast.LiteralExpression literal = (Ast.LiteralExpression) move.source();
            CobolSemanticProduct.CoverageStatus coverage =
                    weakest(CobolSemanticProduct.CoverageStatus.PARTIAL,
                            containmentCoverage(containment),
                            coverage(inputs.finding(move.meta().id())), bindingCoverage);
            CobolSemanticProduct.MoveFact fact = new CobolSemanticProduct.MoveFact(
                    header(statementId, plan.position().ordinal(), containment,
                            statementProvenance, coverage,
                            containmentReadiness(containment, moveReadiness(entry))),
                    new CobolSemanticProduct.LiteralSource(
                            new CobolSemanticProduct.OperandId(statementId, 0),
                            CobolSemanticProduct.LiteralKind.UNKNOWN, literal.value(),
                            provenance(literal.meta().provenance())),
                    new CobolSemanticProduct.DataReference(
                            new CobolSemanticProduct.OperandId(statementId, 1),
                            CobolSemanticProduct.OperandRole.WRITE, binding,
                            provenance(((Ast.DataReference) move.targets().get(0))
                                    .meta().provenance())));
            statements.add(fact);
            gaps.add(new CobolSemanticProduct.Gap(statementId,
                    CobolSemanticProduct.GapScope.LITERAL_KIND, LITERAL_KIND_GAP,
                    "the canonical frontend AST does not publish a typed literal kind",
                    provenance(literal.meta().provenance())));
            addContainmentGap(containment, statementId, statementProvenance, gaps);
            addReportGaps(statementId, entry.occurrence(), inputs,
                    provenance(entry.occurrence().meta().provenance()), gaps);
            requireBindingGapWhenNeeded(fact.header(), entry, gaps);
            return;
        }

        if (plan.position().statement() instanceof Ast.CallStatement call) {
            ReferenceResolution.Entry entry = onlyEntry(plan);
            CobolSemanticProduct.NominalBinding binding = nominalBinding(entry, dataIds);
            CobolSemanticProduct.CoverageStatus bindingCoverage = bindingCoverage(entry);
            ResolutionAnalysisReport.Gap runtimeGap = inputs.requiredReportGap(
                    entry.occurrence(), ResolutionAnalysisReport.GapCategory.CALL_SEMANTICS,
                    DYNAMIC_TARGET_GAP);
            CobolSemanticProduct.CoverageStatus coverage = weakest(
                    containmentCoverage(containment),
                    coverage(inputs.finding(call.meta().id())), bindingCoverage);
            if (!call.arguments().isEmpty() || call.returning() != null
                    || !call.exceptionFlow().isEmpty())
                coverage = weakest(coverage, CobolSemanticProduct.CoverageStatus.PARTIAL);
            CobolSemanticProduct.CallFact fact = new CobolSemanticProduct.CallFact(
                    header(statementId, plan.position().ordinal(), containment,
                            statementProvenance, coverage,
                            containmentReadiness(containment, callReadiness(entry, call))),
                    CobolSemanticProduct.CallSyntax.IDENTIFIER_OR_EXPRESSION,
                    new CobolSemanticProduct.DataReference(
                            new CobolSemanticProduct.OperandId(statementId, 0),
                            CobolSemanticProduct.OperandRole.CALL_TARGET, binding,
                            provenance(((Ast.DataReference) call.target()).meta().provenance())),
                    CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN,
                    runtimeGap.code());
            statements.add(fact);
            addUnprojectedCallSurfaceGaps(statementId, call, statementProvenance, gaps);
            addContainmentGap(containment, statementId, statementProvenance, gaps);
            addReportGaps(statementId, entry.occurrence(), inputs,
                    provenance(entry.occurrence().meta().provenance()), gaps);
            requireBindingGapWhenNeeded(fact.header(), entry, gaps);
            return;
        }

        Ast.IfStatement branch = (Ast.IfStatement) plan.position().statement();
        ContinuationProjection continuation = continuation(plan.position(), statementIds,
                positionsByStatement, continuations);
        BranchContentProjection branchContent = branchContent(branch, statementIds);
        List<CobolSemanticProduct.DataReference> references = new ArrayList<>();
        CobolSemanticProduct.CoverageStatus ifCoverage = weakest(
                CobolSemanticProduct.CoverageStatus.PARTIAL,
                containmentCoverage(containment),
                coverage(inputs.finding(branch.meta().id())));
        for (ReferenceResolution.Entry entry : plan.entries()) {
            CobolSemanticProduct.Provenance referenceProvenance =
                    provenance(entry.occurrence().meta().provenance());
            addReportGaps(statementId, entry.occurrence(), inputs, referenceProvenance, gaps);
            if (!projectableDataBinding(entry, inputs)) {
                ifCoverage = weakest(ifCoverage, CobolSemanticProduct.CoverageStatus.PARTIAL);
                gaps.add(new CobolSemanticProduct.Gap(statementId,
                        CobolSemanticProduct.GapScope.CONDITION_SEMANTICS,
                        CONDITION_REFERENCE_GAP,
                        "condition reference resolves outside the DATA identity capability",
                        referenceProvenance));
                continue;
            }
            CobolSemanticProduct.NominalBinding binding = nominalBinding(entry, dataIds);
            ifCoverage = weakest(ifCoverage, bindingCoverage(entry));
            references.add(new CobolSemanticProduct.DataReference(
                    new CobolSemanticProduct.OperandId(statementId, references.size()),
                    CobolSemanticProduct.OperandRole.READ, binding, referenceProvenance));
        }
        if (!continuation.exact())
            ifCoverage = weakest(ifCoverage, CobolSemanticProduct.CoverageStatus.PARTIAL);
        if (!branchContent.exact())
            ifCoverage = weakest(ifCoverage, CobolSemanticProduct.CoverageStatus.PARTIAL);
        CobolSemanticProduct.StatementHeader ifHeader = header(statementId,
                plan.position().ordinal(), containment, statementProvenance, ifCoverage,
                containmentReadiness(containment,
                        ifReadiness(plan.entries(), continuation, branchContent, inputs)));
        CobolSemanticProduct.IfFact fact = new CobolSemanticProduct.IfFact(
                ifHeader,
                new CobolSemanticProduct.ConditionSurface(conditionShape(branch.condition()),
                        references, provenance(branch.condition().meta().provenance())),
                branch.explicitlyTerminated(), continuation.statement());
        statements.add(fact);
        gaps.add(new CobolSemanticProduct.Gap(statementId,
                CobolSemanticProduct.GapScope.CONDITION_SEMANTICS,
                CONDITION_SEMANTICS_GAP,
                "predicate normalization and type-sensitive validation are not published",
                provenance(branch.condition().meta().provenance())));
        if (!continuation.exact())
            gaps.add(new CobolSemanticProduct.Gap(statementId,
                    CobolSemanticProduct.GapScope.STRUCTURE, CONTINUATION_GAP,
                    continuation.detail(), statementProvenance));
        if (!branchContent.exact())
            gaps.add(new CobolSemanticProduct.Gap(statementId,
                    CobolSemanticProduct.GapScope.STRUCTURE, BRANCH_CONTENT_GAP,
                    branchContent.detail(), statementProvenance));
        addContainmentGap(containment, statementId, statementProvenance, gaps);
        for (ReferenceResolution.Entry entry : plan.entries()) {
            if (projectableDataBinding(entry, inputs))
                requireBindingGapWhenNeeded(ifHeader, entry, gaps);
        }
    }

    private static CobolSemanticProduct.CoverageStatus containmentCoverage(
            CobolSemanticProduct.Containment containment) {
        return containment.branch() == CobolSemanticProduct.Branch.UNKNOWN
                ? CobolSemanticProduct.CoverageStatus.PARTIAL
                : CobolSemanticProduct.CoverageStatus.MODELED;
    }

    private static CobolSemanticProduct.Readiness containmentReadiness(
            CobolSemanticProduct.Containment containment,
            CobolSemanticProduct.Readiness readiness) {
        if (containment.branch() != CobolSemanticProduct.Branch.UNKNOWN) return readiness;
        CobolSemanticProduct.ReadinessStatus cfg = readiness.cfg().status()
                == CobolSemanticProduct.ReadinessStatus.BLOCKED
                ? CobolSemanticProduct.ReadinessStatus.BLOCKED
                : CobolSemanticProduct.ReadinessStatus.PARTIAL;
        return new CobolSemanticProduct.Readiness(readiness.lowering(),
                new CobolSemanticProduct.ReadinessClaim(cfg,
                        "statement containment awaits structural parent projection"),
                readiness.effectsDataflow());
    }

    private static void addContainmentGap(
            CobolSemanticProduct.Containment containment,
            CobolSemanticProduct.StatementId statementId,
            CobolSemanticProduct.Provenance provenance,
            List<CobolSemanticProduct.Gap> gaps) {
        if (containment.branch() != CobolSemanticProduct.Branch.UNKNOWN) return;
        gaps.add(new CobolSemanticProduct.Gap(statementId,
                CobolSemanticProduct.GapScope.STRUCTURE, CONTAINMENT_GAP,
                "statement is nested under a structural parent not projected in this checkpoint",
                provenance));
    }

    private static ReferenceResolution.Entry onlyEntry(StatementPlan plan) {
        require(plan.entries().size() == 1,
                "supported MOVE/CALL statement must have one canonical binding entry");
        return plan.entries().get(0);
    }

    private static CobolSemanticProduct.Containment containment(
            StatementPosition position,
            Map<Ast.Statement, CobolSemanticProduct.StatementId> statementIds) {
        if (position.parent() == null)
            return CobolSemanticProduct.Containment.root();
        if (position.parent() instanceof Ast.IfStatement) {
            CobolSemanticProduct.StatementId parent = statementIds.get(position.parent());
            require(parent != null, "direct IF parent must be part of the projected inventory");
            require(position.branch() == CobolSemanticProduct.Branch.THEN
                            || position.branch() == CobolSemanticProduct.Branch.ELSE,
                    "direct IF child must retain its canonical branch");
            return CobolSemanticProduct.Containment.childOf(parent, position.branch());
        }
        return CobolSemanticProduct.Containment.unknown();
    }

    private static ContinuationProjection continuation(
            StatementPosition position,
            Map<Ast.Statement, CobolSemanticProduct.StatementId> statementIds,
            Map<Ast.Statement, StatementPosition> positionsByStatement,
            Map<Ast.Statement, ContinuationProjection> continuations) {
        ContinuationProjection cached = continuations.get(position.statement());
        if (cached != null) return cached;

        ContinuationProjection result;
        if (position.nextSibling() != null) {
            CobolSemanticProduct.StatementId next = statementIds.get(position.nextSibling());
            result = next == null
                    ? ContinuationProjection.incomplete(
                    "the immediate structural continuation is outside the current statement capability")
                    : ContinuationProjection.statement(next);
        } else if (position.parent() == null) {
            result = ContinuationProjection.end();
        } else if (position.parent() instanceof Ast.IfStatement) {
            StatementPosition parent = positionsByStatement.get(position.parent());
            require(parent != null, "IF parent has no canonical structural position");
            result = continuation(parent, statementIds, positionsByStatement, continuations);
        } else {
            result = ContinuationProjection.incomplete(
                    "IF ends inside a structural parent whose continuation is not projected");
        }
        continuations.put(position.statement(), result);
        return result;
    }

    private static BranchContentProjection branchContent(
            Ast.IfStatement branch,
            Map<Ast.Statement, CobolSemanticProduct.StatementId> statementIds) {
        List<CobolSemanticProduct.Branch> incomplete = new ArrayList<>(2);
        if (hasUnprojectedDirectChild(branch.thenBranch(), statementIds))
            incomplete.add(CobolSemanticProduct.Branch.THEN);
        if (hasUnprojectedDirectChild(branch.elseBranch(), statementIds))
            incomplete.add(CobolSemanticProduct.Branch.ELSE);
        if (incomplete.isEmpty()) return BranchContentProjection.complete();
        return BranchContentProjection.incomplete(
                "direct statement content is outside the current capability in "
                        + String.join(" and ", incomplete.stream().map(Enum::name).toList()));
    }

    private static boolean hasUnprojectedDirectChild(
            List<Ast.Statement> branch,
            Map<Ast.Statement, CobolSemanticProduct.StatementId> statementIds) {
        return branch.stream().anyMatch(statement -> !statementIds.containsKey(statement));
    }

    private static String conditionShape(Ast.Expression condition) {
        if (condition instanceof Ast.DataReference) return "DATA_REFERENCE";
        if (condition instanceof Ast.LogicalCondition logical)
            return logical.connector() == Ast.LogicalConnector.AND
                    ? "LOGICAL_AND" : "LOGICAL_OR";
        if (condition instanceof Ast.GroupedCondition) return "GROUPED";
        if (condition instanceof Ast.RelationCondition) return "RELATION";
        if (condition instanceof Ast.NegatedCondition) return "NEGATED";
        if (condition instanceof Ast.ContextualConditionTail) return "CONTEXTUAL_TAIL";
        if (condition instanceof Ast.DistributedOperandGroup) return "DISTRIBUTED_OPERANDS";
        if (condition instanceof Ast.ClassCondition) return "CLASS_CONDITION";
        if (condition instanceof Ast.OperationExpression) return "OPERATION";
        if (condition instanceof Ast.LiteralExpression) return "LITERAL";
        if (condition instanceof Ast.PreservedExpression) return "PRESERVED_EXPRESSION";
        if (condition instanceof Ast.RawExpression) return "RAW_EXPRESSION";
        return "OTHER_EXPRESSION";
    }

    private static void addUnprojectedCallSurfaceGaps(
            CobolSemanticProduct.StatementId statementId,
            Ast.CallStatement call,
            CobolSemanticProduct.Provenance provenance,
            List<CobolSemanticProduct.Gap> gaps) {
        if (!call.arguments().isEmpty())
            gaps.add(capabilityGap(statementId, "CALL_ARGUMENTS_NOT_PROJECTED",
                    "CALL argument roles are not published by the current boundary fact",
                    provenance));
        if (call.returning() != null)
            gaps.add(capabilityGap(statementId, "CALL_RETURNING_NOT_PROJECTED",
                    "CALL returning operand is not published by the current boundary fact",
                    provenance));
        if (!call.exceptionFlow().isEmpty())
            gaps.add(capabilityGap(statementId, "CALL_EXCEPTION_FLOW_NOT_PROJECTED",
                    "CALL exception flow awaits structural statement projection",
                    provenance));
    }

    private static CobolSemanticProduct.Gap capabilityGap(
            CobolSemanticProduct.StatementId statementId, String code, String detail,
            CobolSemanticProduct.Provenance provenance) {
        return new CobolSemanticProduct.Gap(statementId,
                CobolSemanticProduct.GapScope.CAPABILITY, code, detail, provenance);
    }

    private static void addReportGaps(
            CobolSemanticProduct.StatementId statementId,
            ReferenceOccurrences.Occurrence occurrence,
            ProjectionInputs inputs,
            CobolSemanticProduct.Provenance provenance,
            List<CobolSemanticProduct.Gap> output) {
        for (ResolutionAnalysisReport.Gap gap : inputs.reportGaps(occurrence)) {
            output.add(new CobolSemanticProduct.Gap(statementId, gapScope(gap),
                    gap.code(), gap.message(), provenance));
        }
    }

    private static CobolSemanticProduct.GapScope gapScope(ResolutionAnalysisReport.Gap gap) {
        if (gap.category() == ResolutionAnalysisReport.GapCategory.CALL_SEMANTICS
                && gap.code().equals(DYNAMIC_TARGET_GAP))
            return CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET;
        if (gap.category() == ResolutionAnalysisReport.GapCategory.REFERENCE_BINDING)
            return CobolSemanticProduct.GapScope.NOMINAL_BINDING;
        if (gap.category() == ResolutionAnalysisReport.GapCategory.INPUT
                || gap.code().equals("CALL_LINKAGE_UNKNOWN"))
            return CobolSemanticProduct.GapScope.ANALYSIS_INPUT;
        return CobolSemanticProduct.GapScope.CAPABILITY;
    }

    private static void requireBindingGapWhenNeeded(
            CobolSemanticProduct.StatementHeader header,
            ReferenceResolution.Entry entry,
            List<CobolSemanticProduct.Gap> gaps) {
        if (entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED) return;
        require(gaps.stream().anyMatch(gap -> gap.statement().equals(header.id())
                        && gap.scope() == CobolSemanticProduct.GapScope.NOMINAL_BINDING),
                "incomplete canonical binding must have a report-owned localized gap");
    }

    private static CobolSemanticProduct.NominalBinding nominalBinding(
            ReferenceResolution.Entry entry,
            Map<ResolutionContracts.SemanticEntityId, CobolSemanticProduct.DataItemId> dataIds) {
        List<CobolSemanticProduct.DataCandidate> candidates = new ArrayList<>();
        for (ReferenceResolution.Candidate candidate : entry.candidates()) {
            requireDataCandidate(candidate);
            CobolSemanticProduct.DataItemId id = dataIds.get(candidate.entityId());
            require(id != null, "canonical DATA candidate has no projected declaration");
            candidates.add(new CobolSemanticProduct.DataCandidate(id, candidate.canonicalName()));
        }
        CobolSemanticProduct.ResolutionReason reason = reason(entry.reason());
        if (entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED) {
            ReferenceResolution.Candidate selected = entry.selectedCandidate().orElseThrow();
            CobolSemanticProduct.DataItemId selectedId = dataIds.get(selected.entityId());
            return new CobolSemanticProduct.NominalBinding(
                    CobolSemanticProduct.ResolutionStatus.RESOLVED,
                    reason,
                    candidates, Optional.of(selectedId));
        }
        return CobolSemanticProduct.NominalBinding.incomplete(
                resolutionStatus(entry.status(), entry.reason()), reason, candidates);
    }

    private static ReferenceResolution.Candidate requireDataCandidate(
            ReferenceResolution.Candidate candidate) {
        require(candidate.kind() == ResolutionContracts.ReferenceKind.DATA,
                "projected candidate must have canonical DATA kind");
        require(candidate.entityId().domain()
                        == ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                "projected DATA candidate must identify a DATA symbol");
        return candidate;
    }

    private static CobolSemanticProduct.ResolutionStatus resolutionStatus(
            ResolutionContracts.ResolutionStatus status,
            ResolutionContracts.ResolutionReason reason) {
        if (reason == ResolutionContracts.ResolutionReason.INPUT_INCOMPLETE)
            return CobolSemanticProduct.ResolutionStatus.INPUT_MISSING;
        return switch (status) {
            case AMBIGUOUS -> CobolSemanticProduct.ResolutionStatus.AMBIGUOUS;
            case UNRESOLVED, UNSUPPORTED -> CobolSemanticProduct.ResolutionStatus.UNRESOLVED;
            case RESOLVED, EXTERNAL_OBSERVED -> throw new IllegalArgumentException(
                    "canonical status is not an incomplete DATA binding: " + status);
        };
    }

    private static CobolSemanticProduct.ResolutionReason reason(
            ResolutionContracts.ResolutionReason reason) {
        return switch (reason) {
            case UNIQUE_VISIBLE_DECLARATION ->
                    CobolSemanticProduct.ResolutionReason.UNIQUE_VISIBLE_DECLARATION;
            case QUALIFIED_HIERARCHY_MATCH ->
                    CobolSemanticProduct.ResolutionReason.QUALIFIED_HIERARCHY_MATCH;
            case MULTIPLE_VALID_CANDIDATES ->
                    CobolSemanticProduct.ResolutionReason.MULTIPLE_VALID_CANDIDATES;
            case DECLARATION_NOT_FOUND ->
                    CobolSemanticProduct.ResolutionReason.DECLARATION_NOT_FOUND;
            case INPUT_INCOMPLETE -> CobolSemanticProduct.ResolutionReason.INPUT_INCOMPLETE;
            case UNSUPPORTED_GRAMMAR_FORM ->
                    CobolSemanticProduct.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM;
            case UNSUPPORTED_DIALECT_OPTION ->
                    CobolSemanticProduct.ResolutionReason.UNSUPPORTED_DIALECT_OPTION;
            case INVALID_NAMESPACE_FOR_CONTEXT ->
                    CobolSemanticProduct.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT;
            case LITERAL_EXTERNAL_PROGRAM -> throw new IllegalArgumentException(
                    "literal external program is not a DATA binding reason");
        };
    }

    private static CobolSemanticProduct.CoverageStatus bindingCoverage(
            ReferenceResolution.Entry entry) {
        if (entry.reason() == ResolutionContracts.ResolutionReason.INPUT_INCOMPLETE)
            return CobolSemanticProduct.CoverageStatus.INPUT_MISSING;
        return switch (entry.status()) {
            case RESOLVED -> CobolSemanticProduct.CoverageStatus.MODELED;
            case AMBIGUOUS, UNRESOLVED -> CobolSemanticProduct.CoverageStatus.PARTIAL;
            case UNSUPPORTED, EXTERNAL_OBSERVED -> CobolSemanticProduct.CoverageStatus.UNSUPPORTED;
        };
    }

    private static CobolSemanticProduct.CoverageStatus coverage(
            SemanticCoverage.Finding finding) {
        if (finding.coverage() == SemanticCoverage.ConstructionCoverage.INPUT_MISSING)
            return CobolSemanticProduct.CoverageStatus.INPUT_MISSING;
        if (finding.coverage() == SemanticCoverage.ConstructionCoverage.UNSUPPORTED)
            return CobolSemanticProduct.CoverageStatus.UNSUPPORTED;
        if (finding.coverage()
                == SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED
                || finding.dependencyKnowledge()
                == SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN)
            return CobolSemanticProduct.CoverageStatus.PARTIAL;
        return CobolSemanticProduct.CoverageStatus.MODELED;
    }

    private static CobolSemanticProduct.CoverageStatus weakest(
            CobolSemanticProduct.CoverageStatus... statuses) {
        CobolSemanticProduct.CoverageStatus result = CobolSemanticProduct.CoverageStatus.MODELED;
        for (CobolSemanticProduct.CoverageStatus status : statuses) {
            if (coverageRank(status) < coverageRank(result)) result = status;
        }
        return result;
    }

    private static int coverageRank(CobolSemanticProduct.CoverageStatus status) {
        return switch (status) {
            case INPUT_MISSING -> 0;
            case UNSUPPORTED -> 1;
            case PARTIAL -> 2;
            case MODELED -> 3;
        };
    }

    private static CobolSemanticProduct.StatementHeader header(
            CobolSemanticProduct.StatementId id, int ordinal,
            CobolSemanticProduct.Containment containment,
            CobolSemanticProduct.Provenance provenance,
            CobolSemanticProduct.CoverageStatus coverage,
            CobolSemanticProduct.Readiness readiness) {
        return new CobolSemanticProduct.StatementHeader(id,
                new CobolSemanticProduct.ProgramPoint(ordinal),
                containment, provenance, coverage, readiness);
    }

    private static CobolSemanticProduct.Readiness moveReadiness(
            ReferenceResolution.Entry entry) {
        CobolSemanticProduct.ReadinessStatus lowering =
                entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED
                        ? CobolSemanticProduct.ReadinessStatus.PARTIAL
                        : entry.status() == ResolutionContracts.ResolutionStatus.UNSUPPORTED
                        ? CobolSemanticProduct.ReadinessStatus.BLOCKED
                        : CobolSemanticProduct.ReadinessStatus.PARTIAL;
        return readiness(lowering,
                "literal value and nominal target available; canonical literal kind missing",
                CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                "structural fallthrough available",
                CobolSemanticProduct.ReadinessStatus.PARTIAL,
                "nominal DEF available only to binding precision; literal kind and storage unknown");
    }

    private static CobolSemanticProduct.Readiness callReadiness(
            ReferenceResolution.Entry entry, Ast.CallStatement call) {
        CobolSemanticProduct.ReadinessStatus lowering =
                entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED
                        ? CobolSemanticProduct.ReadinessStatus.SUFFICIENT
                        : entry.status() == ResolutionContracts.ResolutionStatus.UNSUPPORTED
                        ? CobolSemanticProduct.ReadinessStatus.BLOCKED
                        : CobolSemanticProduct.ReadinessStatus.PARTIAL;
        if (!call.arguments().isEmpty() || call.returning() != null)
            lowering = CobolSemanticProduct.ReadinessStatus.PARTIAL;
        CobolSemanticProduct.ReadinessStatus cfg = call.exceptionFlow().isEmpty()
                ? CobolSemanticProduct.ReadinessStatus.SUFFICIENT
                : CobolSemanticProduct.ReadinessStatus.PARTIAL;
        return readiness(lowering,
                "variable CALL surface and canonical nominal binding projected",
                cfg,
                call.exceptionFlow().isEmpty()
                        ? "local structural fallthrough available"
                        : "exception flow is not yet structurally projected",
                CobolSemanticProduct.ReadinessStatus.PARTIAL,
                "nominal USE available only to binding precision; report keeps call uncertainty");
    }

    private static CobolSemanticProduct.Readiness ifReadiness(
            List<ReferenceResolution.Entry> entries,
            ContinuationProjection continuation,
            BranchContentProjection branchContent,
            ProjectionInputs inputs) {
        CobolSemanticProduct.ReadinessStatus lowering =
                CobolSemanticProduct.ReadinessStatus.PARTIAL;
        CobolSemanticProduct.ReadinessStatus effects =
                CobolSemanticProduct.ReadinessStatus.PARTIAL;
        if (entries.stream().anyMatch(entry ->
                entry.status() == ResolutionContracts.ResolutionStatus.UNSUPPORTED
                        || !projectableDataBinding(entry, inputs))) {
            lowering = CobolSemanticProduct.ReadinessStatus.BLOCKED;
            effects = CobolSemanticProduct.ReadinessStatus.BLOCKED;
        }
        boolean structureExact = continuation.exact() && branchContent.exact();
        CobolSemanticProduct.ReadinessStatus cfg = structureExact
                ? CobolSemanticProduct.ReadinessStatus.SUFFICIENT
                : CobolSemanticProduct.ReadinessStatus.PARTIAL;
        return readiness(lowering,
                "condition surface and branches are projected; predicate semantics remain partial",
                cfg,
                structureExact
                        ? "two conservative branches and structural continuation are reconstructible"
                        : !branchContent.exact()
                        ? "direct branch content is incomplete in the current statement capability"
                        : "branches are known but the structural continuation is incomplete",
                effects,
                "condition reads are available only to canonical binding precision");
    }

    private static CobolSemanticProduct.Readiness blockedReadiness(String scope) {
        return readiness(CobolSemanticProduct.ReadinessStatus.BLOCKED, scope,
                CobolSemanticProduct.ReadinessStatus.BLOCKED, scope,
                CobolSemanticProduct.ReadinessStatus.BLOCKED, scope);
    }

    private static CobolSemanticProduct.Readiness readiness(
            CobolSemanticProduct.ReadinessStatus lowering, String loweringScope,
            CobolSemanticProduct.ReadinessStatus cfg, String cfgScope,
            CobolSemanticProduct.ReadinessStatus effects, String effectsScope) {
        return new CobolSemanticProduct.Readiness(
                new CobolSemanticProduct.ReadinessClaim(lowering, loweringScope),
                new CobolSemanticProduct.ReadinessClaim(cfg, cfgScope),
                new CobolSemanticProduct.ReadinessClaim(effects, effectsScope));
    }

    private static CobolSemanticProduct.CoverageSummary coverage(
            CobolSemanticProduct.InventoryStatus inventoryStatus,
            List<CobolSemanticProduct.StatementFact> statements,
            ResolutionAnalysisReport.ProgramUnitSummary reportSummary) {
        int modeled = 0;
        int partial = 0;
        int unsupported = 0;
        int inputMissing = 0;
        for (CobolSemanticProduct.StatementFact statement : statements) {
            switch (statement.header().coverage()) {
                case MODELED -> modeled++;
                case PARTIAL -> partial++;
                case UNSUPPORTED -> unsupported++;
                case INPUT_MISSING -> inputMissing++;
            }
        }
        if (statements.isEmpty()) {
            CobolSemanticProduct.ReadinessStatus emptyStatus =
                    inventoryStatus == CobolSemanticProduct.InventoryStatus.COMPLETE
                            ? CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE
                            : CobolSemanticProduct.ReadinessStatus.BLOCKED;
            return new CobolSemanticProduct.CoverageSummary(inventoryStatus, 0,
                    0, 0, 0, 0,
                    readiness(emptyStatus,
                            inventoryStatus == CobolSemanticProduct.InventoryStatus.COMPLETE
                                    ? "complete inventory proves that the unit has no statements"
                                    : "statement inventory is unavailable because analysis input is missing",
                            emptyStatus,
                            inventoryStatus == CobolSemanticProduct.InventoryStatus.COMPLETE
                                    ? "no statement requires a control successor"
                                    : "control readiness is blocked by unavailable statement inventory",
                            emptyStatus,
                            inventoryStatus == CobolSemanticProduct.InventoryStatus.COMPLETE
                                    ? "no statement requires effects or dataflow input"
                                    : "effects readiness is blocked by unavailable statement inventory"));
        }
        CobolSemanticProduct.ReadinessStatus lowering = reportSummary.referenceBindingComplete()
                ? CobolSemanticProduct.ReadinessStatus.SUFFICIENT
                : CobolSemanticProduct.ReadinessStatus.BLOCKED;
        CobolSemanticProduct.ReadinessStatus effects = reportSummary.dependencyAnalysisReady()
                ? CobolSemanticProduct.ReadinessStatus.SUFFICIENT
                : CobolSemanticProduct.ReadinessStatus.BLOCKED;
        CobolSemanticProduct.ReadinessStatus inventoryCeiling = switch (inventoryStatus) {
            case COMPLETE -> CobolSemanticProduct.ReadinessStatus.SUFFICIENT;
            case PARTIAL -> CobolSemanticProduct.ReadinessStatus.PARTIAL;
            case INPUT_MISSING -> CobolSemanticProduct.ReadinessStatus.BLOCKED;
        };
        lowering = weakerReadiness(lowering, inventoryCeiling);
        effects = weakerReadiness(effects, inventoryCeiling);
        lowering = summaryStatus(lowering, statements,
                readiness -> readiness.lowering().status());
        CobolSemanticProduct.ReadinessStatus cfg = summaryStatus(
                inventoryCeiling, statements,
                readiness -> readiness.cfg().status());
        effects = summaryStatus(effects, statements,
                readiness -> readiness.effectsDataflow().status());
        return new CobolSemanticProduct.CoverageSummary(inventoryStatus, statements.size(),
                modeled, partial, unsupported, inputMissing,
                readiness(lowering,
                        "report binding claim combined with every observed statement fact",
                        cfg,
                        "complete observed inventory bounded by each statement's structural readiness",
                        effects,
                        "report dependency claim bounded by each statement's effects readiness"));
    }

    private static CobolSemanticProduct.ReadinessStatus summaryStatus(
            CobolSemanticProduct.ReadinessStatus proposed,
            List<CobolSemanticProduct.StatementFact> statements,
            java.util.function.Function<CobolSemanticProduct.Readiness,
                    CobolSemanticProduct.ReadinessStatus> dimension) {
        CobolSemanticProduct.ReadinessStatus result = proposed;
        for (CobolSemanticProduct.StatementFact statement : statements) {
            CobolSemanticProduct.ReadinessStatus candidate =
                    dimension.apply(statement.header().readiness());
            if (candidate != CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE
                    && readinessRank(candidate) < readinessRank(result))
                result = candidate;
        }
        return result;
    }

    private static int readinessRank(CobolSemanticProduct.ReadinessStatus status) {
        return switch (status) {
            case BLOCKED -> 0;
            case PARTIAL -> 1;
            case SUFFICIENT -> 2;
            case NOT_APPLICABLE -> 3;
        };
    }

    private static CobolSemanticProduct.ReadinessStatus weakerReadiness(
            CobolSemanticProduct.ReadinessStatus first,
            CobolSemanticProduct.ReadinessStatus second) {
        return readinessRank(first) <= readinessRank(second) ? first : second;
    }

    private static CobolSemanticProduct.Policy policy(
            ResolutionContracts.CobolResolutionPolicy policy) {
        return new CobolSemanticProduct.Policy(policy.policyId(), policy.version(),
                map(policy.qualifyMode()), map(policy.pgmnameMode()),
                map(policy.dynamMode()), map(policy.dllMode()));
    }

    private static CobolSemanticProduct.QualifyMode map(
            ResolutionContracts.QualifyMode mode) {
        return switch (mode) {
            case STANDARD -> CobolSemanticProduct.QualifyMode.STANDARD;
            case EXTEND -> CobolSemanticProduct.QualifyMode.EXTEND;
            case UNSPECIFIED -> CobolSemanticProduct.QualifyMode.UNSPECIFIED;
        };
    }

    private static CobolSemanticProduct.PgmnameMode map(
            ResolutionContracts.PgmnameMode mode) {
        return switch (mode) {
            case COMPAT -> CobolSemanticProduct.PgmnameMode.COMPAT;
            case LONGUPPER -> CobolSemanticProduct.PgmnameMode.LONGUPPER;
            case LONGMIXED -> CobolSemanticProduct.PgmnameMode.LONGMIXED;
            case UNSPECIFIED -> CobolSemanticProduct.PgmnameMode.UNSPECIFIED;
        };
    }

    private static CobolSemanticProduct.DynamMode map(ResolutionContracts.DynamMode mode) {
        return switch (mode) {
            case DYNAM -> CobolSemanticProduct.DynamMode.DYNAM;
            case NODYNAM -> CobolSemanticProduct.DynamMode.NODYNAM;
            case UNSPECIFIED -> CobolSemanticProduct.DynamMode.UNSPECIFIED;
        };
    }

    private static CobolSemanticProduct.DllMode map(ResolutionContracts.DllMode mode) {
        return switch (mode) {
            case DLL -> CobolSemanticProduct.DllMode.DLL;
            case NODLL -> CobolSemanticProduct.DllMode.NODLL;
            case UNSPECIFIED -> CobolSemanticProduct.DllMode.UNSPECIFIED;
        };
    }

    private static CobolSemanticProduct.Provenance provenance(
            Ast.SourceProvenance provenance) {
        return new CobolSemanticProduct.Provenance(
                location(provenance.expanded()), location(provenance.original()),
                provenance.includeChain().stream().map(frame ->
                        new CobolSemanticProduct.IncludeFrame(frame.includingFile(),
                                frame.requestedName(), frame.includedFile(), frame.includeLine()))
                        .toList(),
                provenance.exact());
    }

    private static CobolSemanticProduct.Location location(Ast.SourceLocation location) {
        return new CobolSemanticProduct.Location(location.file(), location.startLine(),
                location.startColumn(), location.endLine(), location.endColumn());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private record StatementPosition(
            Ast.Statement statement,
            Ast.Statement parent,
            CobolSemanticProduct.Branch branch,
            Ast.Statement nextSibling,
            int ordinal) { }

    private record Capability(boolean supported, String kind, String shape, String gapCode) {
        private Capability {
            kind = Objects.requireNonNull(kind, "kind");
            shape = Objects.requireNonNull(shape, "shape");
            gapCode = Objects.requireNonNull(gapCode, "gapCode");
        }

        private static Capability supported(String kind, String shape) {
            return new Capability(true, kind, shape, "");
        }

        private static Capability unsupported(String kind, String shape, String gapCode) {
            return new Capability(false, kind, shape, gapCode);
        }

        private static Capability unmodeled(String kind, String shape, String gapCode) {
            return new Capability(false, kind, shape, gapCode);
        }
    }

    private record ObservedDescriptor(String kind, String shape) { }

    private record StatementPlan(StatementPosition position, Capability capability,
                                 List<ReferenceResolution.Entry> entries) {
        private StatementPlan {
            entries = List.copyOf(entries);
        }
    }

    private record ContinuationProjection(
            Optional<CobolSemanticProduct.StatementId> statement,
            boolean exact,
            String detail) {
        private ContinuationProjection {
            statement = Objects.requireNonNull(statement, "statement");
            detail = Objects.requireNonNullElse(detail, "");
            if (exact && !detail.isEmpty())
                throw new IllegalArgumentException(
                        "exact continuation must not carry an incompleteness detail");
            if (!exact && detail.isBlank())
                throw new IllegalArgumentException(
                        "incomplete continuation must explain its structural gap");
        }

        private static ContinuationProjection statement(
                CobolSemanticProduct.StatementId statement) {
            return new ContinuationProjection(Optional.of(statement), true, "");
        }

        private static ContinuationProjection end() {
            return new ContinuationProjection(Optional.empty(), true, "");
        }

        private static ContinuationProjection incomplete(String detail) {
            return new ContinuationProjection(Optional.empty(), false, detail);
        }
    }

    private record BranchContentProjection(boolean exact, String detail) {
        private BranchContentProjection {
            detail = Objects.requireNonNullElse(detail, "");
            if (exact && !detail.isEmpty())
                throw new IllegalArgumentException(
                        "exact branch content must not carry an incompleteness detail");
            if (!exact && detail.isBlank())
                throw new IllegalArgumentException(
                        "incomplete branch content must explain its structural gap");
        }

        private static BranchContentProjection complete() {
            return new BranchContentProjection(true, "");
        }

        private static BranchContentProjection incomplete(String detail) {
            return new BranchContentProjection(false, detail);
        }
    }

    private record DeclarationSource(SymbolTable.Symbol symbol, Ast.DataEntry entry,
                                     SemanticCoverage.Finding finding) { }

    private record DeclarationProjection(
            Map<ResolutionContracts.SemanticEntityId, CobolSemanticProduct.DataItemId> ids,
            List<CobolSemanticProduct.DataDeclaration> facts) { }

    private record OccurrenceAstKey(ResolutionContracts.ProgramUnitId unitId,
                                    int referenceAstNodeId) { }

    private record OccurrenceIdKey(ResolutionContracts.ProgramUnitId unitId,
                                   int occurrenceId) { }

    private record UnitSource(CompilationUnitModel.ProgramUnit unit, SymbolTable table,
                              Map<Integer, Ast.Node> nodes,
                              Map<Integer, SemanticCoverage.Finding> findings) { }

    private record ProjectionInputs(
            FrontendProducts products,
            ResolutionContracts.ProgramUnitId unitId,
            CobolSemanticProduct.UnitId boundaryUnit,
            UnitSource selectedSource,
            Map<ResolutionContracts.ProgramUnitId, UnitSource> units,
            List<StatementPosition> statementPositions,
            Map<OccurrenceAstKey, ReferenceOccurrences.Occurrence> occurrencesByAst,
            Map<OccurrenceIdKey, ReferenceOccurrences.Occurrence> occurrencesById,
            Map<OccurrenceAstKey, ReferenceResolution.Entry> resolutionsByAst,
            Set<OccurrenceIdKey> externallySuppressedNominalOccurrences,
            Map<OccurrenceIdKey, List<ResolutionAnalysisReport.Gap>> reportGaps,
            ResolutionAnalysisReport.ProgramUnitSummary unitSummary,
            ResolutionAnalysisReport report) {

        private static ProjectionInputs index(
                FrontendProducts products, ResolutionContracts.ProgramUnitId unitId) {
            CompilationUnitModel.ProgramUnit selected = products.frontend().compilationUnit()
                    .find(unitId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "selected program unit is not present in the frontend product"));
            require(unitId.equals(selected.id()), "selected unit identity must be canonical");
            validateReport(products);

            Map<ResolutionContracts.ProgramUnitId, UnitSource> units = indexUnits(products);
            UnitSource selectedSource = units.get(unitId);
            require(selectedSource != null, "selected program unit products are incomplete");
            Map<OccurrenceAstKey, ReferenceOccurrences.Occurrence> occurrencesByAst =
                    new LinkedHashMap<>();
            Map<OccurrenceIdKey, ReferenceOccurrences.Occurrence> occurrencesById =
                    new LinkedHashMap<>();
            indexOccurrences(products.occurrencesByUnit(), occurrencesByAst, occurrencesById);
            Map<OccurrenceAstKey, ReferenceResolution.Entry> resolutionsByAst =
                    indexResolutions(products.resolution(), occurrencesByAst);
            Set<OccurrenceIdKey> externallySuppressedNominalOccurrences =
                    indexExternallySuppressedNominalOccurrences(
                            products.report(), occurrencesById);
            Map<OccurrenceIdKey, List<ResolutionAnalysisReport.Gap>> reportGaps =
                    indexReportGaps(products.report(), occurrencesById);
            ResolutionAnalysisReport.ProgramUnitSummary unitSummary =
                    reportSummary(products.report(), unitId);
            CobolSemanticProduct.UnitId boundaryUnit = new CobolSemanticProduct.UnitId(
                    unitId.compilationUnitId(), unitId.structuralPath(),
                    unitId.canonicalProgramName());
            return new ProjectionInputs(products, unitId, boundaryUnit, selectedSource,
                    Collections.unmodifiableMap(units), statements(selected.program()),
                    Collections.unmodifiableMap(occurrencesByAst),
                    Collections.unmodifiableMap(occurrencesById),
                    Collections.unmodifiableMap(resolutionsByAst),
                    externallySuppressedNominalOccurrences, reportGaps,
                    unitSummary, products.report());
        }

        private ReferenceResolution.Entry entryFor(Ast.Node reference) {
            ReferenceResolution.Entry entry = resolutionsByAst.get(
                    new OccurrenceAstKey(unitId, reference.meta().id()));
            require(entry != null, "typed reference has no canonical resolution entry");
            require(entry.occurrence().meta().equals(reference.meta()),
                    "canonical occurrence must preserve typed reference metadata");
            return entry;
        }

        private ReferenceResolution.Entry optionalEntryFor(Ast.Node reference) {
            return resolutionsByAst.get(new OccurrenceAstKey(unitId, reference.meta().id()));
        }

        private SemanticCoverage.Finding finding(int astNodeId) {
            SemanticCoverage.Finding finding = selectedSource.findings().get(astNodeId);
            require(finding != null, "typed AST node has no canonical coverage finding");
            return finding;
        }

        private List<ResolutionAnalysisReport.Gap> reportGaps(
                ReferenceOccurrences.Occurrence occurrence) {
            return reportGaps.getOrDefault(new OccurrenceIdKey(
                    occurrence.programUnitId(), occurrence.id()), List.of());
        }

        private boolean nominalGapSuppressedByExternalClassification(
                ReferenceOccurrences.Occurrence occurrence) {
            return externallySuppressedNominalOccurrences.contains(new OccurrenceIdKey(
                    occurrence.programUnitId(), occurrence.id()));
        }

        private ResolutionAnalysisReport.Gap requiredReportGap(
                ReferenceOccurrences.Occurrence occurrence,
                ResolutionAnalysisReport.GapCategory category,
                String code) {
            ResolutionAnalysisReport.Gap match = null;
            for (ResolutionAnalysisReport.Gap gap : reportGaps(occurrence)) {
                if (gap.category() != category || !gap.code().equals(code)) continue;
                require(match == null, "canonical report published a duplicate localized gap");
                match = gap;
            }
            require(match != null, "canonical report omitted a required localized CALL gap");
            return match;
        }

        private DeclarationSource declarationSource(
                ResolutionContracts.SemanticEntityId entityId, String expectedName) {
            UnitSource source = units.get(entityId.programUnitId());
            require(source != null, "DATA candidate unit has no canonical frontend products");
            require(entityId.localId() < source.table().symbols().size(),
                    "DATA candidate symbol identity is not published");
            SymbolTable.Symbol symbol = source.table().symbols().get(entityId.localId());
            require(symbol.id() == entityId.localId()
                            && symbol.namespace() == SymbolTable.Namespace.DATA
                            && (symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM
                            || symbol.kind() == SymbolTable.SymbolKind.RENAMES),
                    "DATA entity must identify a canonical DATA declaration");
            if (expectedName != null)
                require(symbol.canonicalName().equals(expectedName),
                        "DATA candidate name contradicts its canonical declaration");
            Ast.Node node = source.nodes().get(symbol.declarationAstNodeId());
            require(node instanceof Ast.DataEntry,
                    "DATA symbol must point to a typed DATA declaration");
            SemanticCoverage.Finding finding = source.findings().get(node.meta().id());
            require(finding != null && finding.meta().equals(node.meta()),
                    "DATA declaration must retain its canonical coverage/provenance");
            return new DeclarationSource(symbol, (Ast.DataEntry) node, finding);
        }
    }

    private static Map<ResolutionContracts.ProgramUnitId, UnitSource> indexUnits(
            FrontendProducts products) {
        LinkedHashMap<ResolutionContracts.ProgramUnitId, UnitSource> result =
                new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit
                : products.frontend().compilationUnit().programUnits()) {
            CompilationUnitSymbolTables.UnitSymbols unitSymbols = products.symbolTables()
                    .forProgramUnit(unit.id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "program unit has no canonical symbol-table product"));
            Map<Integer, Ast.Node> nodes = new LinkedHashMap<>();
            collectNodes(unit.program(), nodes);
            SemanticCoverage.Report coverage = products.frontend().coverageByProgramUnit()
                    .get(unit.id());
            require(coverage != null, "program unit has no canonical coverage product");
            Map<Integer, SemanticCoverage.Finding> findings = new LinkedHashMap<>();
            for (SemanticCoverage.Finding finding : coverage.findings()) {
                if (finding.astNodeId() < 0) continue;
                require(findings.put(finding.astNodeId(), finding) == null,
                        "multiple coverage findings share one AST identity");
            }
            result.put(unit.id(), new UnitSource(unit, unitSymbols.symbolTable(),
                    Collections.unmodifiableMap(nodes), Collections.unmodifiableMap(findings)));
        }
        return result;
    }

    private static void collectNodes(Ast.Node node, Map<Integer, Ast.Node> output) {
        require(output.put(node.meta().id(), node) == null,
                "duplicate AST node identity in program unit");
        for (Ast.Node child : Ast.children(node)) collectNodes(child, output);
    }

    private static List<StatementPosition> statements(Ast.Program program) {
        List<Ast.Statement> roots = new ArrayList<>();
        collectDirectStatements(program, roots);
        List<StatementPosition> result = new ArrayList<>();
        collectStatementGroup(roots, null, CobolSemanticProduct.Branch.ROOT, result);
        return List.copyOf(result);
    }

    private static void collectStatementGroup(
            List<Ast.Statement> group,
            Ast.Statement parent,
            CobolSemanticProduct.Branch branch,
            List<StatementPosition> output) {
        for (int index = 0; index < group.size(); index++) {
            Ast.Statement statement = group.get(index);
            Ast.Statement nextSibling = index + 1 < group.size() ? group.get(index + 1) : null;
            output.add(new StatementPosition(statement, parent, branch, nextSibling,
                    output.size()));
            if (statement instanceof Ast.IfStatement conditional) {
                collectStatementGroup(conditional.thenBranch(), conditional,
                        CobolSemanticProduct.Branch.THEN, output);
                collectStatementGroup(conditional.elseBranch(), conditional,
                        CobolSemanticProduct.Branch.ELSE, output);
            } else {
                List<Ast.Statement> nested = new ArrayList<>();
                for (Ast.Node child : Ast.children(statement))
                    collectDirectStatements(child, nested);
                collectStatementGroup(nested, statement,
                        CobolSemanticProduct.Branch.UNKNOWN, output);
            }
        }
    }

    private static void collectDirectStatements(
            Ast.Node node, List<Ast.Statement> output) {
        if (node instanceof Ast.Statement statement) {
            output.add(statement);
            return;
        }
        for (Ast.Node child : Ast.children(node)) collectDirectStatements(child, output);
    }

    private static void indexOccurrences(
            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> products,
            Map<OccurrenceAstKey, ReferenceOccurrences.Occurrence> byAst,
            Map<OccurrenceIdKey, ReferenceOccurrences.Occurrence> byId) {
        for (Map.Entry<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> product
                : products.entrySet()) {
            for (ReferenceOccurrences.Occurrence occurrence : product.getValue().occurrences()) {
                require(product.getKey().equals(occurrence.programUnitId()),
                        "occurrence product crossed program-unit namespace");
                require(byAst.put(new OccurrenceAstKey(product.getKey(),
                                occurrence.referenceAstNodeId()), occurrence) == null,
                        "multiple occurrences share one canonical reference AST identity");
                require(byId.put(new OccurrenceIdKey(product.getKey(), occurrence.id()),
                                occurrence) == null,
                        "multiple occurrences share one canonical occurrence identity");
            }
        }
    }

    private static Map<OccurrenceAstKey, ReferenceResolution.Entry> indexResolutions(
            ReferenceResolution resolution,
            Map<OccurrenceAstKey, ReferenceOccurrences.Occurrence> occurrences) {
        LinkedHashMap<OccurrenceAstKey, ReferenceResolution.Entry> result =
                new LinkedHashMap<>();
        for (ReferenceResolution.Entry entry : resolution.entries()) {
            ReferenceOccurrences.Occurrence occurrence = entry.occurrence();
            OccurrenceAstKey key = new OccurrenceAstKey(
                    occurrence.programUnitId(), occurrence.referenceAstNodeId());
            require(Objects.equals(occurrences.get(key), occurrence),
                    "resolution entry must carry the canonical occurrence product");
            require(result.put(key, entry) == null,
                    "multiple resolutions share one canonical reference AST identity");
        }
        return result;
    }

    private static Map<OccurrenceIdKey, List<ResolutionAnalysisReport.Gap>> indexReportGaps(
            ResolutionAnalysisReport report,
            Map<OccurrenceIdKey, ReferenceOccurrences.Occurrence> occurrences) {
        LinkedHashMap<OccurrenceIdKey, List<ResolutionAnalysisReport.Gap>> mutable =
                new LinkedHashMap<>();
        for (ResolutionAnalysisReport.Gap gap : report.gaps()) {
            if (gap.occurrenceId() < 0) continue;
            require(gap.programUnitId() != null,
                    "localized report gap must retain its program-unit namespace");
            OccurrenceIdKey key = new OccurrenceIdKey(gap.programUnitId(), gap.occurrenceId());
            require(occurrences.containsKey(key),
                    "report gap references no canonical occurrence");
            mutable.computeIfAbsent(key, ignored -> new ArrayList<>()).add(gap);
        }
        LinkedHashMap<OccurrenceIdKey, List<ResolutionAnalysisReport.Gap>> result =
                new LinkedHashMap<>();
        mutable.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(result);
    }

    private static Set<OccurrenceIdKey> indexExternallySuppressedNominalOccurrences(
            ResolutionAnalysisReport report,
            Map<OccurrenceIdKey, ReferenceOccurrences.Occurrence> occurrences) {
        LinkedHashSet<OccurrenceIdKey> result = new LinkedHashSet<>();
        for (ExternalClassification.Entry classification
                : report.externalClassifications().entries()) {
            if (classification.copyInputCompleteness()
                    != ExternalClassification.CopyInputCompleteness.COMPLETE)
                continue;
            for (int occurrenceId : classification.coveredOccurrenceIds()) {
                OccurrenceIdKey key = new OccurrenceIdKey(
                        classification.programUnitId(), occurrenceId);
                require(occurrences.containsKey(key),
                        "external classification references no canonical occurrence");
                require(result.add(key),
                        "canonical occurrence is covered by multiple external classifications");
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static ResolutionAnalysisReport.ProgramUnitSummary reportSummary(
            ResolutionAnalysisReport report, ResolutionContracts.ProgramUnitId unitId) {
        ResolutionAnalysisReport.ProgramUnitSummary result = null;
        for (ResolutionAnalysisReport.ProgramUnitSummary summary : report.programUnits()) {
            if (!summary.programUnitId().equals(unitId)) continue;
            require(result == null, "report contains duplicate program-unit summaries");
            result = summary;
        }
        require(result != null, "report omitted the selected program-unit summary");
        return result;
    }

    private static void validateReport(FrontendProducts products) {
        require(products.report().policy().equals(products.resolution().policy()),
                "report policy contradicts the canonical resolution product");
        EnumMap<ResolutionContracts.ResolutionStatus, Long> statuses =
                zeroed(ResolutionContracts.ResolutionStatus.class);
        EnumMap<ResolutionContracts.ResolutionReason, Long> reasons =
                zeroed(ResolutionContracts.ResolutionReason.class);
        EnumMap<ResolutionContracts.ReferenceKind, Long> syntacticKinds =
                zeroed(ResolutionContracts.ReferenceKind.class);
        EnumMap<ResolutionContracts.ReferenceKind, Long> semanticKinds =
                zeroed(ResolutionContracts.ReferenceKind.class);
        EnumMap<ResolutionContracts.ReferenceRole, Long> roles =
                zeroed(ResolutionContracts.ReferenceRole.class);
        Map<ResolutionContracts.ProgramUnitId, int[]> unitCounts = new LinkedHashMap<>();
        for (ReferenceResolution.Entry entry : products.resolution().entries()) {
            statuses.merge(entry.status(), 1L, Long::sum);
            reasons.merge(entry.reason(), 1L, Long::sum);
            syntacticKinds.merge(entry.occurrence().kind(), 1L, Long::sum);
            entry.selectedCandidate().ifPresent(candidate ->
                    semanticKinds.merge(candidate.kind(), 1L, Long::sum));
            roles.merge(entry.occurrence().role(), 1L, Long::sum);
            int[] counts = unitCounts.computeIfAbsent(
                    entry.occurrence().programUnitId(), ignored -> new int[6]);
            counts[0]++;
            switch (entry.status()) {
                case RESOLVED -> counts[1]++;
                case EXTERNAL_OBSERVED -> counts[2]++;
                case AMBIGUOUS -> counts[3]++;
                case UNRESOLVED -> counts[4]++;
                case UNSUPPORTED -> counts[5]++;
            }
        }
        require(products.report().statusCounts().equals(statuses)
                        && products.report().reasonCounts().equals(reasons)
                        && products.report().syntacticKindCounts().equals(syntacticKinds)
                        && products.report().resolvedSemanticKindCounts().equals(semanticKinds)
                        && products.report().roleCounts().equals(roles),
                "report aggregates contradict the canonical resolution product");
        long occurrenceCount = 0;
        for (ReferenceOccurrences occurrences : products.occurrencesByUnit().values())
            occurrenceCount += occurrences.occurrences().size();
        require(products.report().referenceCount() == occurrenceCount,
                "report reference count contradicts the canonical occurrence products");
        require(products.report().programUnits().size()
                        == products.frontend().compilationUnit().programUnits().size(),
                "report program-unit inventory contradicts the frontend product");
        for (ResolutionAnalysisReport.ProgramUnitSummary summary
                : products.report().programUnits()) {
            int[] counts = unitCounts.getOrDefault(summary.programUnitId(), new int[6]);
            require(products.frontend().compilationUnit().find(summary.programUnitId()).isPresent()
                            && summary.references() == counts[0]
                            && summary.resolved() == counts[1]
                            && summary.externalObserved() == counts[2]
                            && summary.ambiguous() == counts[3]
                            && summary.unresolved() == counts[4]
                            && summary.unsupported() == counts[5],
                    "report program-unit summary contradicts canonical products");
        }
    }

    private static <E extends Enum<E>> EnumMap<E, Long> zeroed(Class<E> type) {
        EnumMap<E, Long> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) result.put(value, 0L);
        return result;
    }
}
