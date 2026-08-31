package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Conservative composition of frontend, collection and name-binding coverage. */
public final class ResolutionAnalysisReport {
    public enum GapCategory {
        INPUT,
        FRONTEND_COVERAGE,
        SEMANTIC_DIAGNOSTIC,
        COLLECTOR_INTEGRITY,
        PRESERVED_CONTAINER,
        REFERENCE_BINDING,
        CALL_SEMANTICS,
        EXTERNAL_CLASSIFICATION
    }

    public enum AnalysisClaim { COMPLETE, INCOMPLETE }

    public record FrontendState(int unresolvedCopies, int preprocessorErrors,
                                int lexerErrors, int parserErrors,
                                List<Diagnostic> diagnostics) {
        public FrontendState {
            if (unresolvedCopies < 0 || preprocessorErrors < 0 || lexerErrors < 0 || parserErrors < 0)
                throw new IllegalArgumentException("frontend counts must be non-negative");
            diagnostics = List.copyOf(diagnostics);
        }
        public static FrontendState complete() { return new FrontendState(0, 0, 0, 0, List.of()); }
    }

    public record Gap(int id, GapCategory category, String code, String message,
                      ResolutionContracts.ProgramUnitId programUnitId,
                      String grammarRule, int line, int occurrenceId) { }

    public record ProgramUnitSummary(ResolutionContracts.ProgramUnitId programUnitId,
                                     int references, int resolved, int externalObserved, int ambiguous,
                                     int unresolved, int unsupported, int gaps,
                                     boolean referenceBindingComplete,
                                     boolean dependencyAnalysisReady) { }

    public record OperationalMetrics(int indexedDeclarations, int nominalLookups,
                                     long candidateInspections, int maximumCandidates,
                                     long collectedReferences, int programUnits) { }

    private final ResolutionContracts.CobolResolutionPolicy policy;
    private final ExternalClassification externalClassifications;
    private final ResolutionContracts.Completeness completeness;
    private final AnalysisClaim analysisClaim;
    private final List<Gap> gaps;
    private final List<ProgramUnitSummary> programUnits;
    private final Map<ResolutionContracts.ResolutionStatus, Long> statusCounts;
    private final Map<ResolutionContracts.ResolutionReason, Long> reasonCounts;
    private final Map<ResolutionContracts.ReferenceKind, Long> syntacticKindCounts;
    private final Map<ResolutionContracts.ReferenceKind, Long> resolvedSemanticKindCounts;
    private final Map<ResolutionContracts.ReferenceRole, Long> roleCounts;
    private final OperationalMetrics operationalMetrics;

    private ResolutionAnalysisReport(ResolutionContracts.CobolResolutionPolicy policy,
                                     ExternalClassification externalClassifications,
                                     ResolutionContracts.Completeness completeness,
                                     List<Gap> gaps, List<ProgramUnitSummary> programUnits,
                                     Map<ResolutionContracts.ResolutionStatus, Long> statusCounts,
                                     Map<ResolutionContracts.ResolutionReason, Long> reasonCounts,
                                     Map<ResolutionContracts.ReferenceKind, Long> syntacticKindCounts,
                                     Map<ResolutionContracts.ReferenceKind, Long> resolvedSemanticKindCounts,
                                     Map<ResolutionContracts.ReferenceRole, Long> roleCounts,
                                     OperationalMetrics operationalMetrics) {
        this.policy = policy;
        this.externalClassifications = Objects.requireNonNull(
                externalClassifications, "externalClassifications");
        this.completeness = completeness;
        this.analysisClaim = completeness.dependencyAnalysisReady()
                ? AnalysisClaim.COMPLETE : AnalysisClaim.INCOMPLETE;
        this.gaps = List.copyOf(gaps);
        this.programUnits = List.copyOf(programUnits);
        this.statusCounts = Map.copyOf(statusCounts);
        this.reasonCounts = Map.copyOf(reasonCounts);
        this.syntacticKindCounts = Map.copyOf(syntacticKindCounts);
        this.resolvedSemanticKindCounts = Map.copyOf(resolvedSemanticKindCounts);
        this.roleCounts = Map.copyOf(roleCounts);
        this.operationalMetrics = operationalMetrics;
    }

    public static ResolutionAnalysisReport compose(
            CompilationUnitBuildResult frontend,
            FrontendState frontendState,
            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrencesByUnit,
            ReferenceResolution resolution) {
        return compose(frontend, frontendState, occurrencesByUnit, resolution,
                ExternalClassification.empty());
    }

    public static ResolutionAnalysisReport compose(
            CompilationUnitBuildResult frontend,
            FrontendState frontendState,
            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrencesByUnit,
            ReferenceResolution resolution,
            ExternalClassification externalClassifications) {
        Objects.requireNonNull(frontend, "frontend");
        Objects.requireNonNull(frontendState, "frontendState");
        Objects.requireNonNull(occurrencesByUnit, "occurrencesByUnit");
        Objects.requireNonNull(resolution, "resolution");
        Objects.requireNonNull(externalClassifications, "externalClassifications");

        List<Gap> gaps = new ArrayList<>();
        addInputGaps(frontendState, gaps);
        addFrontendGaps(frontend, gaps);
        addCollectorGaps(frontend.compilationUnit(), occurrencesByUnit, resolution, gaps);
        ExternalProjection projection = validateExternalClassifications(
                resolution, externalClassifications, gaps);
        addResolutionGaps(resolution, projection.coveredOccurrences(), gaps);
        addCallSemanticsGaps(resolution, gaps);
        addExternalClassificationGaps(projection.classifications(), gaps);

        EnumMap<ResolutionContracts.ResolutionStatus, Long> statuses = zeroed(
                ResolutionContracts.ResolutionStatus.class);
        EnumMap<ResolutionContracts.ResolutionReason, Long> reasons = zeroed(
                ResolutionContracts.ResolutionReason.class);
        EnumMap<ResolutionContracts.ReferenceKind, Long> syntacticKinds = zeroed(
                ResolutionContracts.ReferenceKind.class);
        EnumMap<ResolutionContracts.ReferenceKind, Long> resolvedSemanticKinds = zeroed(
                ResolutionContracts.ReferenceKind.class);
        EnumMap<ResolutionContracts.ReferenceRole, Long> roles = zeroed(
                ResolutionContracts.ReferenceRole.class);
        for (ReferenceResolution.Entry entry : resolution.entries()) {
            statuses.merge(entry.status(), 1L, Long::sum);
            reasons.merge(entry.reason(), 1L, Long::sum);
            syntacticKinds.merge(entry.occurrence().kind(), 1L, Long::sum);
            entry.selectedCandidate().ifPresent(candidate ->
                    resolvedSemanticKinds.merge(candidate.kind(), 1L, Long::sum));
            roles.merge(entry.occurrence().role(), 1L, Long::sum);
        }

        List<String> blockingReasons = gaps.stream().map(Gap::code).distinct().toList();
        boolean bindingComplete = gaps.stream().noneMatch(gap ->
                gap.category() != GapCategory.CALL_SEMANTICS);
        boolean dependencyReady = gaps.isEmpty();
        ResolutionContracts.Completeness completeness = new ResolutionContracts.Completeness(
                bindingComplete, dependencyReady, dependencyReady ? List.of() : blockingReasons);
        List<ProgramUnitSummary> summaries = programSummaries(
                frontend.compilationUnit(), resolution, gaps, frontendState);
        ReferenceResolution.Metrics metrics = resolution.metrics();
        long referenceCount = occurrencesByUnit.values().stream()
                .mapToLong(value -> value.occurrences().size()).sum();
        OperationalMetrics operational = new OperationalMetrics(metrics.indexedDeclarations(),
                metrics.nominalLookups(), metrics.candidateInspections(), metrics.maximumCandidates(),
                referenceCount, frontend.compilationUnit().programUnits().size());
        return new ResolutionAnalysisReport(resolution.policy(), projection.classifications(),
                completeness, gaps, summaries,
                statuses, reasons, syntacticKinds, resolvedSemanticKinds, roles, operational);
    }

    public ResolutionContracts.CobolResolutionPolicy policy() { return policy; }
    public ExternalClassification externalClassifications() { return externalClassifications; }
    public ResolutionContracts.Completeness completeness() { return completeness; }
    public AnalysisClaim analysisClaim() { return analysisClaim; }
    public List<Gap> gaps() { return gaps; }
    public List<ProgramUnitSummary> programUnits() { return programUnits; }
    public Map<ResolutionContracts.ResolutionStatus, Long> statusCounts() { return statusCounts; }
    public Map<ResolutionContracts.ResolutionReason, Long> reasonCounts() { return reasonCounts; }
    public Map<ResolutionContracts.ReferenceKind, Long> syntacticKindCounts() {
        return syntacticKindCounts;
    }
    public Map<ResolutionContracts.ReferenceKind, Long> resolvedSemanticKindCounts() {
        return resolvedSemanticKindCounts;
    }
    public Map<ResolutionContracts.ReferenceRole, Long> roleCounts() { return roleCounts; }
    public OperationalMetrics operationalMetrics() { return operationalMetrics; }
    public long referenceCount() { return operationalMetrics.collectedReferences(); }
    public long unknownDependencyCount() { return gaps.size(); }

    private static void addInputGaps(FrontendState state, List<Gap> gaps) {
        if (state.unresolvedCopies() > 0)
            addGap(gaps, GapCategory.INPUT, "UNRESOLVED_COPY",
                    state.unresolvedCopies() + " COPY statement(s) could not be expanded",
                    null, "copyStatement", 0, -1);
        if (state.preprocessorErrors() > 0)
            addGap(gaps, GapCategory.INPUT, "PREPROCESSOR_ERROR",
                    state.preprocessorErrors() + " preprocessing error(s)", null, "", 0, -1);
        if (state.lexerErrors() > 0)
            addGap(gaps, GapCategory.INPUT, "LEXER_ERROR",
                    state.lexerErrors() + " lexer error(s)", null, "", 0, -1);
        if (state.parserErrors() > 0)
            addGap(gaps, GapCategory.INPUT, "PARSER_ERROR",
                    state.parserErrors() + " parser error(s)", null, "", 0, -1);
        for (Diagnostic diagnostic : state.diagnostics()) {
            if (diagnostic.message().startsWith("unresolved_copy")) continue;
            if (diagnostic.phase() == Diagnostic.Phase.LEXER
                    || diagnostic.phase() == Diagnostic.Phase.PARSER
                    || diagnostic.phase() == Diagnostic.Phase.PREPROCESSOR
                    || diagnostic.phase() == Diagnostic.Phase.IO) {
                addGap(gaps, GapCategory.INPUT, "FRONTEND_DIAGNOSTIC_" + diagnostic.phase(),
                        diagnostic.message(), null, "", diagnostic.line(), -1);
            }
        }
    }

    private static void addFrontendGaps(CompilationUnitBuildResult frontend, List<Gap> gaps) {
        for (CompilationUnitModel.ProgramUnit unit : frontend.compilationUnit().programUnits()) {
            SemanticCoverage.Report report = frontend.coverageByProgramUnit().get(unit.id());
            for (SemanticCoverage.Finding finding : report.findings()) {
                boolean blocks = finding.coverage() == SemanticCoverage.ConstructionCoverage.UNSUPPORTED
                        || finding.coverage() == SemanticCoverage.ConstructionCoverage.INPUT_MISSING
                        || finding.dependencyKnowledge() == SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN;
                if (blocks) addGap(gaps, GapCategory.FRONTEND_COVERAGE,
                        "FRONTEND_" + finding.coverage() + "_" + finding.grammarRule(),
                        finding.reason(), unit.id(), finding.grammarRule(),
                        finding.meta().span().startLine(), -1);
            }
            for (SemanticCoverage.Diagnostic diagnostic : frontend.diagnosticsByProgramUnit().get(unit.id()))
                addGap(gaps, GapCategory.SEMANTIC_DIAGNOSTIC, diagnostic.code(), diagnostic.message(),
                        unit.id(), diagnostic.meta().origin().grammarRule(),
                        diagnostic.meta().span().startLine(), -1);
        }
    }

    private static void addCollectorGaps(CompilationUnitModel model,
                                         Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences,
                                         ReferenceResolution resolution, List<Gap> gaps) {
        Set<String> resolvedKeys = new HashSet<>();
        for (ReferenceResolution.Entry entry : resolution.entries()) {
            ReferenceOccurrences.Occurrence occurrence = entry.occurrence();
            resolvedKeys.add(occurrence.programUnitId() + "#" + occurrence.id());
            if (occurrence.preservation() != ReferenceOccurrences.Preservation.STRUCTURED)
                addGap(gaps, GapCategory.PRESERVED_CONTAINER, "PRESERVED_REFERENCE_CONTAINER",
                        "Reference is preserved inside syntax that is not fully interpreted",
                        occurrence.programUnitId(), occurrence.grammarRule(),
                        occurrence.meta().span().startLine(), occurrence.id());
        }
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            ReferenceOccurrences collected = occurrences.get(unit.id());
            if (collected == null) {
                addGap(gaps, GapCategory.COLLECTOR_INTEGRITY, "MISSING_OCCURRENCE_PRODUCT",
                        "No occurrence product exists for program unit", unit.id(), "", 0, -1);
                continue;
            }
            for (ReferenceOccurrences.Occurrence occurrence : collected.occurrences()) {
                if (!resolvedKeys.contains(unit.id() + "#" + occurrence.id()))
                    addGap(gaps, GapCategory.COLLECTOR_INTEGRITY, "OCCURRENCE_WITHOUT_RESOLUTION",
                            "Collected reference has no resolution entry", unit.id(), occurrence.grammarRule(),
                            occurrence.meta().span().startLine(), occurrence.id());
            }
        }
    }

    private static void addResolutionGaps(ReferenceResolution resolution,
                                          Set<OccurrenceKey> externallyCovered,
                                          List<Gap> gaps) {
        Map<String, ReferenceResolution.Entry> entriesByReferenceNode = new HashMap<>();
        for (ReferenceResolution.Entry entry : resolution.entries()) {
            entriesByReferenceNode.put(entry.occurrence().programUnitId() + "#"
                    + entry.occurrence().referenceAstNodeId(), entry);
            if (entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED
                    || entry.status() == ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED) continue;
            ReferenceOccurrences.Occurrence occurrence = entry.occurrence();
            if (externallyCovered.contains(new OccurrenceKey(
                    occurrence.programUnitId(), occurrence.id()))) continue;
            addGap(gaps, GapCategory.REFERENCE_BINDING,
                    "REFERENCE_" + entry.status() + "_" + entry.reason(),
                    entry.status() + " " + occurrence.kind() + " reference '"
                            + occurrence.writtenText() + "': " + entry.reason(),
                    occurrence.programUnitId(), occurrence.grammarRule(),
                    occurrence.meta().span().startLine(), occurrence.id());
        }
        for (DeclarationRelationResolution.Entry relation :
                resolution.declarationRelations().entries()) {
            if (relation.status() == ResolutionContracts.ResolutionStatus.RESOLVED) continue;
            ReferenceResolution.Entry nominal = entriesByReferenceNode.get(
                    relation.programUnitId() + "#" + relation.referenceAstNodeId());
            ReferenceOccurrences.Occurrence occurrence = nominal == null ? null : nominal.occurrence();
            addGap(gaps, GapCategory.REFERENCE_BINDING,
                    "DECLARATION_RELATION_" + relation.status() + "_" + relation.reason(),
                    relation.status() + " " + relation.kind() + " declaration relation: "
                            + relation.reason(), relation.programUnitId(),
                    occurrence == null ? "" : occurrence.grammarRule(),
                    occurrence == null ? 0 : occurrence.meta().span().startLine(),
                    occurrence == null ? -1 : occurrence.id());
        }
    }

    private static ExternalProjection validateExternalClassifications(
            ReferenceResolution resolution, ExternalClassification classifications, List<Gap> gaps) {
        Map<OccurrenceKey, ReferenceResolution.Entry> entries = new HashMap<>();
        for (ReferenceResolution.Entry entry : resolution.entries()) {
            ReferenceOccurrences.Occurrence occurrence = entry.occurrence();
            entries.put(new OccurrenceKey(occurrence.programUnitId(), occurrence.id()), entry);
        }
        Set<OccurrenceKey> covered = new HashSet<>();
        for (ExternalClassification.Entry classification : classifications.entries()) {
            OccurrenceKey rootKey = new OccurrenceKey(
                    classification.programUnitId(), classification.rootOccurrenceId());
            ReferenceResolution.Entry root = entries.get(rootKey);
            boolean coherent = root != null
                    && root.occurrence().referenceAstNodeId() == classification.rootAstNodeId()
                    && root.occurrence().meta().equals(classification.meta())
                    && root.occurrence().writtenText().equals(classification.constructWrittenText())
                    && root.status() == ResolutionContracts.ResolutionStatus.UNRESOLVED;
            for (int occurrenceId : classification.coveredOccurrenceIds()) {
                OccurrenceKey key = new OccurrenceKey(classification.programUnitId(), occurrenceId);
                coherent &= entries.containsKey(key) && covered.add(key);
            }
            if (!coherent) {
                addGap(gaps, GapCategory.COLLECTOR_INTEGRITY,
                        "INCONSISTENT_EXTERNAL_CLASSIFICATION",
                        "External classification does not match the immutable occurrence/resolution products",
                        classification.programUnitId(), classification.meta().origin().grammarRule(),
                        classification.meta().span().startLine(), classification.rootOccurrenceId());
                return new ExternalProjection(ExternalClassification.empty(), Set.of());
            }
        }
        return new ExternalProjection(classifications, Set.copyOf(covered));
    }

    private static void addExternalClassificationGaps(
            ExternalClassification classifications, List<Gap> gaps) {
        for (ExternalClassification.Entry classification : classifications.entries()) {
            addGap(gaps, GapCategory.EXTERNAL_CLASSIFICATION,
                    "EXTERNAL_" + classification.certainty() + "_"
                            + classification.technology() + "_" + classification.kind(),
                    classification.certainty() + " " + classification.technology() + " "
                            + classification.kind() + " construct '"
                            + classification.constructWrittenText() + "': " + classification.reason(),
                    classification.programUnitId(), classification.meta().origin().grammarRule(),
                    classification.meta().span().startLine(), classification.rootOccurrenceId());
        }
    }

    private static void addCallSemanticsGaps(ReferenceResolution resolution, List<Gap> gaps) {
        for (ReferenceResolution.Entry entry : resolution.entries()) {
            if (entry.callSemantics().isEmpty()) continue;
            ReferenceResolution.CallSemantics semantics = entry.callSemantics().orElseThrow();
            ReferenceOccurrences.Occurrence occurrence = entry.occurrence();
            if (semantics.linkage() == ResolutionContracts.CallLinkage.UNKNOWN) {
                addGap(gaps, GapCategory.CALL_SEMANTICS, "CALL_LINKAGE_UNKNOWN",
                        "CALL target is nominally bound but linkage depends on missing compiler options",
                        occurrence.programUnitId(), occurrence.grammarRule(),
                        occurrence.meta().span().startLine(), occurrence.id());
            }
            if (semantics.targetSyntax() == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION) {
                addGap(gaps, GapCategory.CALL_SEMANTICS, "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN",
                        "CALL identifier/expression binds its data reference, not the runtime program target",
                        occurrence.programUnitId(), occurrence.grammarRule(),
                        occurrence.meta().span().startLine(), occurrence.id());
            }
        }
    }

    private static List<ProgramUnitSummary> programSummaries(
            CompilationUnitModel model, ReferenceResolution resolution, List<Gap> gaps,
            FrontendState frontendState) {
        boolean globalInputGap = frontendState.unresolvedCopies() > 0
                || frontendState.preprocessorErrors() > 0 || frontendState.lexerErrors() > 0
                || frontendState.parserErrors() > 0 || !frontendState.diagnostics().isEmpty();
        List<ProgramUnitSummary> result = new ArrayList<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            List<ReferenceResolution.Entry> entries = resolution.entries().stream()
                    .filter(entry -> entry.occurrence().programUnitId().equals(unit.id())).toList();
            List<Gap> unitGapEntries = gaps.stream().filter(gap -> gap.programUnitId() != null
                    && gap.programUnitId().equals(unit.id())).toList();
            int unitGaps = unitGapEntries.size();
            boolean bindingComplete = !globalInputGap && unitGapEntries.stream().noneMatch(gap ->
                    gap.category() != GapCategory.CALL_SEMANTICS);
            boolean dependencyReady = !globalInputGap && unitGaps == 0;
            result.add(new ProgramUnitSummary(unit.id(), entries.size(),
                    count(entries, ResolutionContracts.ResolutionStatus.RESOLVED),
                    count(entries, ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED),
                    count(entries, ResolutionContracts.ResolutionStatus.AMBIGUOUS),
                    count(entries, ResolutionContracts.ResolutionStatus.UNRESOLVED),
                    count(entries, ResolutionContracts.ResolutionStatus.UNSUPPORTED),
                    unitGaps + (globalInputGap ? 1 : 0), bindingComplete, dependencyReady));
        }
        return List.copyOf(result);
    }

    private static int count(List<ReferenceResolution.Entry> entries,
                             ResolutionContracts.ResolutionStatus status) {
        return (int) entries.stream().filter(entry -> entry.status() == status).count();
    }

    private static void addGap(List<Gap> gaps, GapCategory category, String code, String message,
                               ResolutionContracts.ProgramUnitId unitId, String grammarRule,
                               int line, int occurrenceId) {
        gaps.add(new Gap(gaps.size(), category, code, message, unitId,
                Objects.requireNonNullElse(grammarRule, ""), line, occurrenceId));
    }

    private static <E extends Enum<E>> EnumMap<E, Long> zeroed(Class<E> type) {
        EnumMap<E, Long> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) result.put(value, 0L);
        return result;
    }

    private record OccurrenceKey(ResolutionContracts.ProgramUnitId programUnitId, int occurrenceId) { }

    private record ExternalProjection(ExternalClassification classifications,
                                      Set<OccurrenceKey> coveredOccurrences) { }
}
