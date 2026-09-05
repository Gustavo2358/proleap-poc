package io.github.gustavo2358.cobolexplorer.semanticproduct;

import io.github.gustavo2358.cobolexplorer.Ast;
import io.github.gustavo2358.cobolexplorer.CompilationUnitBuildResult;
import io.github.gustavo2358.cobolexplorer.CompilationUnitModel;
import io.github.gustavo2358.cobolexplorer.CompilationUnitSymbolTables;
import io.github.gustavo2358.cobolexplorer.ReferenceOccurrences;
import io.github.gustavo2358.cobolexplorer.ReferenceResolution;
import io.github.gustavo2358.cobolexplorer.ResolutionContracts;
import io.github.gustavo2358.cobolexplorer.SymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Projects the canonical frontend products into the first production
 * Semantic Product slice.
 *
 * <p>This class is deliberately the only frontend-aware part of the
 * semantic-product package. It performs no parsing or resolution: the AST,
 * symbol, occurrence and resolution products are already closed when this
 * adapter is called.</p>
 */
public final class CobolMoveCallAdapter {
    private static final String RUNTIME_TARGET_GAP = "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN";
    private static final String LITERAL_KIND_GAP = "LITERAL_KIND_NOT_PUBLISHED";

    private CobolMoveCallAdapter() { }

    /** The immutable frontend products needed for one projection. */
    public record FrontendProducts(
            CompilationUnitBuildResult frontend,
            CompilationUnitSymbolTables symbolTables,
            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrencesByUnit,
            ReferenceResolution resolution) {
        public FrontendProducts {
            frontend = Objects.requireNonNull(frontend, "frontend");
            symbolTables = Objects.requireNonNull(symbolTables, "symbolTables");
            occurrencesByUnit = immutableOccurrences(occurrencesByUnit);
            resolution = Objects.requireNonNull(resolution, "resolution");
        }

        private static Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences>
        immutableOccurrences(Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> source) {
            Objects.requireNonNull(source, "occurrencesByUnit");
            return Collections.unmodifiableMap(new LinkedHashMap<>(source));
        }
    }

    /** Materializes the state for the selected program unit. */
    public static CobolSemanticProduct.State project(FrontendProducts products,
                                                     ResolutionContracts.ProgramUnitId unitId) {
        Objects.requireNonNull(products, "products");
        Objects.requireNonNull(unitId, "unitId");
        CompilationUnitModel.ProgramUnit unit = products.frontend().compilationUnit()
                .find(unitId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "selected program unit is not present in the frontend product"));
        require(unitId.equals(unit.id()), "selected unit identity must be canonical");

        CobolSemanticProduct.UnitId boundaryUnit = new CobolSemanticProduct.UnitId(
                unit.id().compilationUnitId(), unit.id().structuralPath(),
                unit.id().canonicalProgramName());
        Map<Integer, Ast.Node> nodesById = indexNodes(unit.program());
        List<Ast.Statement> statements = statements(unit.program());
        Ast.MoveStatement move = single(statements, Ast.MoveStatement.class,
                "the selected unit must contain exactly one MOVE");
        Ast.CallStatement call = single(statements, Ast.CallStatement.class,
                "the selected unit must contain exactly one CALL");
        Map<Ast.Statement, CobolSemanticProduct.ProgramPoint> points = programPoints(statements);
        CobolSemanticProduct.ProgramPoint movePoint = requiredPoint(points, move, "MOVE");
        CobolSemanticProduct.ProgramPoint callPoint = requiredPoint(points, call, "CALL");

        Ast.LiteralExpression literal = requireType(Ast.LiteralExpression.class, move.source(),
                "MOVE source must be a typed literal expression");
        require(!move.corresponding(), "MOVE CORRESPONDING is outside this slice");
        require(move.targets().size() == 1, "the slice must contain exactly one MOVE target");
        Ast.DataReference moveTarget = requireType(Ast.DataReference.class, move.targets().get(0),
                "MOVE target must be a typed DATA reference");
        Ast.DataReference callOperand = requireType(Ast.DataReference.class, call.target(),
                "CALL operand must be a typed DATA reference");
        require(call.targetSyntax() == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION,
                "the slice CALL must use identifier/expression syntax");

        SymbolTable table = products.symbolTables().forProgramUnit(unit.id())
                .orElseThrow(() -> new IllegalArgumentException(
                        "selected program unit has no symbol table product"))
                .symbolTable();
        Map<OccurrenceKey, ReferenceOccurrences.Occurrence> occurrences =
                indexOccurrences(products.occurrencesByUnit().get(unit.id()), unit.id());
        Map<OccurrenceKey, ReferenceResolution.Entry> resolutions =
                indexResolutions(products.resolution(), unit.id(), occurrences);
        ReferenceResolution.Entry moveEntry = entryFor(moveTarget, unit.id(), occurrences, resolutions);
        ReferenceResolution.Entry callEntry = entryFor(callOperand, unit.id(), occurrences, resolutions);
        require(moveEntry.occurrence().role() == ResolutionContracts.ReferenceRole.VALUE_WRITE,
                "MOVE target role must come from the canonical occurrence");
        require(callEntry.occurrence().role() == ResolutionContracts.ReferenceRole.CALL_TARGET,
                "CALL operand role must come from the canonical occurrence");
        require(moveEntry.occurrence().meta().equals(moveTarget.meta()),
                "MOVE occurrence must preserve the typed reference metadata");
        require(callEntry.occurrence().meta().equals(callOperand.meta()),
                "CALL occurrence must preserve the typed reference metadata");

        ReferenceResolution.Candidate moveCandidate = resolvedDataCandidate(moveEntry, unit.id());
        ReferenceResolution.Candidate callCandidate = resolvedDataCandidate(callEntry, unit.id());
        require(moveCandidate.entityId().equals(callCandidate.entityId()),
                "MOVE and CALL must reconcile to the same canonical DATA entity");
        CobolSemanticProduct.DataItemId dataItem = new CobolSemanticProduct.DataItemId(
                boundaryUnit, moveCandidate.entityId().localId());
        SymbolTable.Symbol dataSymbol = dataSymbol(table, moveCandidate);
        CobolSemanticProduct.DataDeclaration declaration = dataDeclaration(
                dataSymbol, nodesById, dataItem);

        ReferenceResolution.CallSemantics callSemantics = callEntry.callSemantics()
                .orElseThrow(() -> new IllegalArgumentException(
                        "CALL resolution must publish typed call semantics"));
        require(callSemantics.targetSyntax() == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION,
                "CALL resolution must preserve identifier/expression syntax");

        CobolSemanticProduct.Provenance moveProvenance = provenance(move.meta().provenance());
        CobolSemanticProduct.Provenance callProvenance = provenance(call.meta().provenance());
        CobolSemanticProduct.NominalBinding moveBinding = nominalBinding(
                moveEntry, dataItem);
        CobolSemanticProduct.NominalBinding callBinding = nominalBinding(
                callEntry, dataItem);
        CobolSemanticProduct.RuntimeTargetKnowledge runtimeTarget =
                CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN;
        CobolSemanticProduct.StatementId moveId = new CobolSemanticProduct.StatementId(
                boundaryUnit, movePoint.ordinal());
        CobolSemanticProduct.StatementId callId = new CobolSemanticProduct.StatementId(
                boundaryUnit, callPoint.ordinal());
        CobolSemanticProduct.MoveFact moveFact = new CobolSemanticProduct.MoveFact(
                header(moveId, movePoint, moveProvenance,
                        CobolSemanticProduct.CoverageStatus.PARTIAL, moveReadiness()),
                new CobolSemanticProduct.LiteralSource(
                        new CobolSemanticProduct.OperandId(moveId, 0),
                        CobolSemanticProduct.LiteralKind.UNKNOWN, literal.value(),
                        provenance(literal.meta().provenance())),
                new CobolSemanticProduct.DataReference(
                        new CobolSemanticProduct.OperandId(moveId, 1),
                        CobolSemanticProduct.OperandRole.WRITE, moveBinding, moveProvenance));
        CobolSemanticProduct.CallFact callFact = new CobolSemanticProduct.CallFact(
                header(callId, callPoint, callProvenance, callReadiness()),
                CobolSemanticProduct.CallSyntax.IDENTIFIER_OR_EXPRESSION,
                new CobolSemanticProduct.DataReference(
                        new CobolSemanticProduct.OperandId(callId, 0),
                        CobolSemanticProduct.OperandRole.CALL_TARGET,
                        callBinding, callProvenance),
                runtimeTarget, RUNTIME_TARGET_GAP);
        CobolSemanticProduct.Gap literalKindGap = new CobolSemanticProduct.Gap(
                moveId, CobolSemanticProduct.GapScope.LITERAL_KIND,
                LITERAL_KIND_GAP,
                "the canonical frontend AST does not publish a typed literal kind",
                provenance(literal.meta().provenance()));
        CobolSemanticProduct.Gap runtimeGap = new CobolSemanticProduct.Gap(
                callId, CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET,
                RUNTIME_TARGET_GAP,
                "CALL identifier/expression binds its DATA operand, not its runtime program target",
                callProvenance);
        require(movePoint.ordinal() < callPoint.ordinal(),
                "ordering must be strict and forward");
        List<CobolSemanticProduct.StatementFact> statementFacts = List.of(moveFact, callFact);

        return new CobolSemanticProduct.State(
                boundaryUnit,
                policy(products.resolution()),
                List.of(declaration), statementFacts, List.of(literalKindGap, runtimeGap),
                new CobolSemanticProduct.CoverageSummary(
                        CobolSemanticProduct.InventoryStatus.PARTIAL,
                        2, 1, 1, 0, 0, summaryReadiness()));
    }

    /** Opens the read-only port over one already-materialized projection. */
    public static CobolSemanticPort open(FrontendProducts products,
                                         ResolutionContracts.ProgramUnitId unitId) {
        return CobolSemanticPort.open(project(products, unitId));
    }

    private static Map<Integer, Ast.Node> indexNodes(Ast.Program program) {
        Map<Integer, Ast.Node> result = new LinkedHashMap<>();
        for (Ast.Node node : nodes(program)) {
            if (result.put(node.meta().id(), node) != null)
                throw new IllegalArgumentException("duplicate AST node identity in selected unit");
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<Ast.Node> nodes(Ast.Node root) {
        List<Ast.Node> result = new ArrayList<>();
        collectNodes(root, result);
        return result;
    }

    private static void collectNodes(Ast.Node node, List<Ast.Node> result) {
        result.add(node);
        for (Ast.Node child : Ast.children(node)) collectNodes(child, result);
    }

    private static List<Ast.Statement> statements(Ast.Program program) {
        return nodes(program).stream()
                .filter(Ast.Statement.class::isInstance)
                .map(Ast.Statement.class::cast)
                .toList();
    }

    private static Map<Ast.Statement, CobolSemanticProduct.ProgramPoint> programPoints(
            List<Ast.Statement> statements) {
        Map<Ast.Statement, CobolSemanticProduct.ProgramPoint> result =
                new IdentityHashMap<>();
        for (int index = 0; index < statements.size(); index++)
            result.put(statements.get(index), new CobolSemanticProduct.ProgramPoint(index));
        return result;
    }

    private static CobolSemanticProduct.ProgramPoint requiredPoint(
            Map<Ast.Statement, CobolSemanticProduct.ProgramPoint> points,
            Ast.Statement statement, String kind) {
        CobolSemanticProduct.ProgramPoint point = points.get(statement);
        require(point != null, kind + " must receive a deterministic program point");
        return point;
    }

    private static <T> T single(List<Ast.Statement> statements, Class<T> type, String message) {
        List<T> matches = statements.stream().filter(type::isInstance).map(type::cast).toList();
        require(matches.size() == 1, message);
        return matches.get(0);
    }

    private static Map<OccurrenceKey, ReferenceOccurrences.Occurrence> indexOccurrences(
            ReferenceOccurrences product, ResolutionContracts.ProgramUnitId unitId) {
        require(product != null, "selected program unit has no occurrence product");
        Map<OccurrenceKey, ReferenceOccurrences.Occurrence> result = new LinkedHashMap<>();
        for (ReferenceOccurrences.Occurrence occurrence : product.occurrences()) {
            require(unitId.equals(occurrence.programUnitId()),
                    "occurrence product crossed program-unit namespace");
            OccurrenceKey key = new OccurrenceKey(unitId, occurrence.referenceAstNodeId());
            require(result.put(key, occurrence) == null,
                    "multiple occurrences share one canonical reference AST identity");
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<OccurrenceKey, ReferenceResolution.Entry> indexResolutions(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId unitId,
            Map<OccurrenceKey, ReferenceOccurrences.Occurrence> occurrences) {
        Map<OccurrenceKey, ReferenceResolution.Entry> result = new LinkedHashMap<>();
        for (ReferenceResolution.Entry entry : resolution.entries()) {
            ReferenceOccurrences.Occurrence occurrence = entry.occurrence();
            if (!unitId.equals(occurrence.programUnitId())) continue;
            OccurrenceKey key = new OccurrenceKey(unitId, occurrence.referenceAstNodeId());
            require(Objects.equals(occurrences.get(key), occurrence),
                    "resolution entry must carry the canonical occurrence product");
            require(result.put(key, entry) == null,
                    "multiple resolutions share one canonical reference AST identity");
        }
        return Collections.unmodifiableMap(result);
    }

    private static ReferenceResolution.Entry entryFor(
            Ast.DataReference reference, ResolutionContracts.ProgramUnitId unitId,
            Map<OccurrenceKey, ReferenceOccurrences.Occurrence> occurrences,
            Map<OccurrenceKey, ReferenceResolution.Entry> resolutions) {
        OccurrenceKey key = new OccurrenceKey(unitId, reference.meta().id());
        require(occurrences.containsKey(key), "typed reference has no canonical occurrence");
        ReferenceResolution.Entry entry = resolutions.get(key);
        require(entry != null, "typed reference has no canonical resolution entry");
        return entry;
    }

    private static ReferenceResolution.Candidate resolvedDataCandidate(
            ReferenceResolution.Entry entry, ResolutionContracts.ProgramUnitId unitId) {
        require(entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED,
                "slice reference must be nominally resolved");
        require(entry.candidates().size() == 1,
                "resolved slice reference must preserve exactly one candidate");
        ReferenceResolution.Candidate candidate = entry.selectedCandidate().orElseThrow();
        require(candidate.kind() == ResolutionContracts.ReferenceKind.DATA,
                "slice reference must resolve as DATA");
        require(candidate.entityId().domain() == ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                "slice reference must resolve to a DATA symbol identity");
        require(candidate.entityId().programUnitId().equals(unitId),
                "DATA candidate must remain in the reference unit namespace");
        require(candidate.declarationSymbolIds().contains(candidate.entityId().localId()),
                "DATA candidate must identify its canonical declaration symbol");
        return candidate;
    }

    private static SymbolTable.Symbol dataSymbol(SymbolTable table,
                                                 ReferenceResolution.Candidate candidate) {
        int localId = candidate.entityId().localId();
        require(localId >= 0 && localId < table.symbols().size(),
                "DATA candidate symbol identity is not published");
        SymbolTable.Symbol symbol = table.symbols().get(localId);
        require(symbol.id() == localId
                        && symbol.namespace() == SymbolTable.Namespace.DATA
                        && symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM,
                "DATA candidate must identify an elementary DATA symbol");
        return symbol;
    }

    private static CobolSemanticProduct.DataDeclaration dataDeclaration(
            SymbolTable.Symbol symbol,
            Map<Integer, Ast.Node> nodesById, CobolSemanticProduct.DataItemId dataItem) {
        Ast.DataEntry entry = requireType(Ast.DataEntry.class,
                nodesById.get(symbol.declarationAstNodeId()),
                "DATA symbol must point to a typed DATA declaration");
        List<Ast.PictureClause> pictures = entry.clauses().stream()
                .filter(Ast.PictureClause.class::isInstance)
                .map(Ast.PictureClause.class::cast).toList();
        require(pictures.size() == 1 && !pictures.get(0).picture().isBlank(),
                "slice DATA declaration must publish exactly one PIC");
        return new CobolSemanticProduct.DataDeclaration(
                dataItem, symbol.canonicalName(), Optional.of(pictures.get(0).picture()),
                provenance(entry.meta().provenance()),
                CobolSemanticProduct.CoverageStatus.MODELED, dataReadiness());
    }

    private static CobolSemanticProduct.NominalBinding nominalBinding(
            ReferenceResolution.Entry entry, CobolSemanticProduct.DataItemId dataItem) {
        ReferenceResolution.Candidate candidate = resolvedDataCandidate(
                entry, entry.occurrence().programUnitId());
        require(candidate.entityId().localId() == dataItem.localId(),
                "projected binding must retain the canonical DATA local identity");
        require(entry.reason() == ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                "the narrow slice does not project an unrepresentable resolution reason");
        return CobolSemanticProduct.NominalBinding.resolved(
                dataItem, candidate.canonicalName());
    }

    private static CobolSemanticProduct.StatementHeader header(
            CobolSemanticProduct.StatementId id, CobolSemanticProduct.ProgramPoint point,
            CobolSemanticProduct.Provenance provenance,
            CobolSemanticProduct.Readiness readiness) {
        return header(id, point, provenance,
                CobolSemanticProduct.CoverageStatus.MODELED, readiness);
    }

    private static CobolSemanticProduct.StatementHeader header(
            CobolSemanticProduct.StatementId id, CobolSemanticProduct.ProgramPoint point,
            CobolSemanticProduct.Provenance provenance,
            CobolSemanticProduct.CoverageStatus coverage,
            CobolSemanticProduct.Readiness readiness) {
        return new CobolSemanticProduct.StatementHeader(id, point,
                CobolSemanticProduct.Containment.root(), provenance, coverage, readiness);
    }

    private static CobolSemanticProduct.Readiness dataReadiness() {
        return readiness(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                "declaration surface and nominal identity available",
                CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE,
                "declaration alone has no control successor",
                CobolSemanticProduct.ReadinessStatus.PARTIAL,
                "nominal identity available; storage layout and aliases unknown");
    }

    private static CobolSemanticProduct.Readiness moveReadiness() {
        return readiness(CobolSemanticProduct.ReadinessStatus.PARTIAL,
                "literal value and nominal target available; canonical literal kind missing",
                CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                "structural fallthrough available",
                CobolSemanticProduct.ReadinessStatus.PARTIAL,
                "nominal DEF available; literal kind, storage region and aliases unknown");
    }

    private static CobolSemanticProduct.Readiness callReadiness() {
        return readiness(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                "variable CALL syntax and nominal operand available",
                CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                "local structural continuation available",
                CobolSemanticProduct.ReadinessStatus.PARTIAL,
                "nominal USE available; runtime target and call effects unknown");
    }

    private static CobolSemanticProduct.Readiness summaryReadiness() {
        return readiness(CobolSemanticProduct.ReadinessStatus.PARTIAL,
                "the compatibility bridge publishes only its narrow MOVE/CALL slice",
                CobolSemanticProduct.ReadinessStatus.PARTIAL,
                "control structure outside the compatibility slice is not inventoried",
                CobolSemanticProduct.ReadinessStatus.PARTIAL,
                "storage aliases and runtime call effects remain unknown");
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

    private static CobolSemanticProduct.Policy policy(ReferenceResolution resolution) {
        ResolutionContracts.CobolResolutionPolicy policy = resolution.policy();
        return new CobolSemanticProduct.Policy(policy.policyId(), policy.version(),
                map(policy.qualifyMode()), map(policy.pgmnameMode()),
                map(policy.dynamMode()), map(policy.dllMode()));
    }

    private static CobolSemanticProduct.QualifyMode map(ResolutionContracts.QualifyMode mode) {
        return switch (mode) {
            case STANDARD -> CobolSemanticProduct.QualifyMode.STANDARD;
            case EXTEND -> CobolSemanticProduct.QualifyMode.EXTEND;
            case UNSPECIFIED -> CobolSemanticProduct.QualifyMode.UNSPECIFIED;
        };
    }

    private static CobolSemanticProduct.PgmnameMode map(ResolutionContracts.PgmnameMode mode) {
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

    private static CobolSemanticProduct.Provenance provenance(Ast.SourceProvenance provenance) {
        return new CobolSemanticProduct.Provenance(
                location(provenance.expanded()), location(provenance.original()),
                provenance.includeChain().stream().map(frame ->
                        new CobolSemanticProduct.IncludeFrame(frame.includingFile(),
                                frame.requestedName(), frame.includedFile(), frame.includeLine())).toList(),
                provenance.exact());
    }

    private static CobolSemanticProduct.Location location(Ast.SourceLocation location) {
        return new CobolSemanticProduct.Location(location.file(), location.startLine(),
                location.startColumn(), location.endLine(), location.endColumn());
    }

    private static <T> T requireType(Class<T> type, Object value, String message) {
        require(type.isInstance(value), message);
        return type.cast(value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private record OccurrenceKey(ResolutionContracts.ProgramUnitId unitId, int referenceAstNodeId) { }
}
