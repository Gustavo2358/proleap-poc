package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Immutable post-binding proofs for the narrow W2A IF profile. No runtime evaluation. */
public final class IfSemantics {
    public enum Availability { KNOWN, PARTIAL, UNAVAILABLE, INPUT_MISSING }
    public record Predicate(Availability availability, Optional<Integer> readNode,
                            Optional<ResolutionContracts.SemanticEntityId> wholeItem,
                            Ast.SourceProvenance provenance) { }
    public record Arm(Ast.BranchPresence presence, Availability contentAvailability,
                      Optional<Integer> entry, Ast.SourceProvenance provenance) { }
    public record Facts(Predicate predicate, Arm thenArm, Arm elseArm,
                        Optional<Integer> nextStatement, boolean simpleProfile) { }
    public record IndependentStorageSet(Availability availability,
            List<ResolutionContracts.SemanticEntityId> members, Ast.SourceProvenance provenance) {
        public IndependentStorageSet { members = List.copyOf(members); }
    }
    public record Metrics(long nodeVisits, long declarationVisits, long referenceVisits,
                          long armMemberVisits, long scalarLookups) { }
    private final Map<ScalarMoveSemantics.NodeKey, Facts> facts;
    private final Map<ResolutionContracts.ProgramUnitId, IndependentStorageSet> storage;
    private final Metrics metrics;
    private IfSemantics(Map<ScalarMoveSemantics.NodeKey, Facts> facts,
            Map<ResolutionContracts.ProgramUnitId, IndependentStorageSet> storage, long[] work) {
        this.facts = Map.copyOf(facts); this.storage = Map.copyOf(storage);
        this.metrics = new Metrics(work[0], work[1], work[2], work[3], work[4]);
    }
    public Facts fact(ResolutionContracts.ProgramUnitId unit, int node) {
        return Objects.requireNonNull(facts.get(new ScalarMoveSemantics.NodeKey(unit, node)), "IF proof inventory missing");
    }
    public IndependentStorageSet storage(ResolutionContracts.ProgramUnitId unit) { return storage.get(unit); }
    public Metrics metrics() { return metrics; }

    /** Finite indexed passes O(nodes + references + declarations + direct arm members). */
    static IfSemantics analyze(CompilationUnitBuildResult frontend, CompilationUnitSymbolTables tables,
            ReferenceResolution resolution, ResolutionAnalysisReport report,
            Map<ResolutionContracts.SemanticEntityId, ScalarMoveSemantics.ScalarText> scalars,
            Map<ScalarMoveSemantics.NodeKey, ScalarMoveSemantics.Move> moves) {
        long[] work = new long[5];
        boolean input = report.gaps().stream().noneMatch(g -> g.category() == ResolutionAnalysisReport.GapCategory.INPUT);
        Map<ScalarMoveSemantics.NodeKey, ReferenceResolution.Entry> reads = new HashMap<>();
        for (var entry : resolution.entries()) {
            work[2]++;
            reads.put(new ScalarMoveSemantics.NodeKey(entry.occurrence().programUnitId(),
                    entry.occurrence().referenceAstNodeId()), entry);
        }
        Map<ScalarMoveSemantics.NodeKey, Facts> facts = new HashMap<>();
        Map<ResolutionContracts.ProgramUnitId, IndependentStorageSet> storage = new HashMap<>();
        for (var unit : frontend.compilationUnit().programUnits()) {
            Map<Integer, SemanticCoverage.Finding> coverage = new HashMap<>();
            for (var f : frontend.coverageByProgramUnit().get(unit.id()).findings()) {
                work[0]++; coverage.put(f.astNodeId(), f);
            }
            Map<Integer, ResolutionContracts.SemanticEntityId> entities = new HashMap<>();
            Set<Integer> duplicateDeclarations = new HashSet<>();
            for (var symbol : tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols()) {
                work[1]++;
                if (symbol.namespace() == SymbolTable.Namespace.DATA && symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM) {
                    var id = new ResolutionContracts.SemanticEntityId(unit.id(), ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, symbol.id());
                    if (entities.put(symbol.declarationAstNodeId(), id) != null) duplicateDeclarations.add(symbol.declarationAstNodeId());
                }
            }
            Map<Integer, Integer> next = Map.of();
            for (var division : unit.program().divisions())
                if (division.divisionKind() == Ast.DivisionKind.PROCEDURE) next = division.normalContinuations();
            List<Ast.Section> workingStorage = new ArrayList<>();
            List<Visit> branches = new ArrayList<>();
            Map<Integer, Ast.DataEntry> dataNodes = new HashMap<>();
            Deque<Visit> pending = new ArrayDeque<>(); pending.push(new Visit(unit.program(), false));
            while (!pending.isEmpty()) {
                Visit visit = pending.pop(); Ast.Node node = visit.node(); work[0]++;
                if (node instanceof Ast.Section section && section.dataSectionKind() == Ast.DataSectionKind.WORKING_STORAGE)
                    workingStorage.add(section);
                if (node instanceof Ast.IfStatement) branches.add(visit);
                if (node instanceof Ast.DataEntry data) dataNodes.put(data.meta().id(), data);
                for (Ast.Node child : Ast.children(node))
                    pending.push(new Visit(child, visit.insideStatement() || node instanceof Ast.Statement));
            }
            Map<ResolutionContracts.SemanticEntityId, ScalarMoveSemantics.ScalarText> completeScalars = new HashMap<>();
            for (var entity : entities.entrySet()) {
                var finding = coverage.get(entity.getKey()); work[1]++;
                var scalar = scalars.get(entity.getValue()); work[4]++;
                var data = dataNodes.get(entity.getKey());
                boolean complete = data != null && modeled(data, coverage);
                if (data != null) for (var clause : data.clauses()) { work[0]++; complete &= modeled(clause, coverage); }
                if (complete && scalar != null && !duplicateDeclarations.contains(entity.getKey()) && finding != null
                        && finding.coverage() == SemanticCoverage.ConstructionCoverage.MODELED && finding.meta().provenance().exact())
                    completeScalars.put(entity.getValue(), scalar);
            }
            for (Visit visit : branches) {
                    Ast.IfStatement branch = (Ast.IfStatement) visit.node();
                    boolean intact = input && modeled(branch, coverage);
                    Predicate predicate = predicate(branch, unit.id(), intact, input, reads, completeScalars, coverage, work);
                    Arm thenArm = arm(branch.thenBranch(), Ast.BranchPresence.PRESENT, branch.thenProvenance(),
                            unit.id(), intact, input, moves, coverage, work);
                    Arm elseArm = arm(branch.elseBranch(), branch.elsePresence(), branch.elseProvenance(),
                            unit.id(), intact, input, moves, coverage, work);
                    Optional<Integer> successor = intact ? Optional.ofNullable(next.get(branch.meta().id())) : Optional.empty();
                    boolean simple = !visit.insideStatement() && branch.explicitlyTerminated()
                            && predicate.availability() == Availability.KNOWN
                            && thenArm.contentAvailability() == Availability.KNOWN
                            && elseArm.contentAvailability() == Availability.KNOWN && successor.isPresent();
                    facts.put(new ScalarMoveSemantics.NodeKey(unit.id(), branch.meta().id()),
                            new Facts(predicate, thenArm, elseArm, successor, simple));
                }
            // One section and its complete declaration inventory: no pair enumeration.
            List<ResolutionContracts.SemanticEntityId> members = new ArrayList<>();
            Ast.SourceProvenance origin = unit.program().meta().provenance();
            boolean independent = input && workingStorage.size() == 1;
            if (workingStorage.size() == 1) {
                Ast.Section section = workingStorage.get(0); origin = section.meta().provenance();
                independent &= exactSurface(section, coverage);
                for (Ast.Node child : section.children()) {
                    work[1]++;
                    if (!(child instanceof Ast.DataEntry data)) { independent = false; continue; }
                    var entity = entities.get(data.meta().id()); work[4]++;
                    boolean eligible = entity != null && !duplicateDeclarations.contains(data.meta().id())
                            && scalars.containsKey(entity) && modeled(data, coverage);
                    for (var clause : data.clauses()) { work[0]++; eligible &= modeled(clause, coverage); }
                    independent &= eligible;
                    if (eligible) members.add(entity);
                }
            }
            independent &= members.size() >= 2;
            storage.put(unit.id(), new IndependentStorageSet(independent ? Availability.KNOWN
                    : input ? Availability.UNAVAILABLE : Availability.INPUT_MISSING,
                    independent ? members : List.of(), origin));
        }
        return new IfSemantics(facts, storage, work);
    }
    private record Visit(Ast.Node node, boolean insideStatement) { }
    private static boolean modeled(Ast.Node node, Map<Integer, SemanticCoverage.Finding> coverage) {
        var f = coverage.get(node.meta().id());
        return node.meta().provenance().exact() && f != null
                && f.coverage() == SemanticCoverage.ConstructionCoverage.MODELED;
    }
    /** Coverage findings are intentionally sparse: containers and ordinary typed
     * expression nodes have no individual finding. Statement/data coverage and
     * exact typed surface plus complete frontend input jointly supply the proof. */
    private static boolean exactSurface(Ast.Node node, Map<Integer, SemanticCoverage.Finding> coverage) {
        return node.meta().provenance().exact() && (!coverage.containsKey(node.meta().id()) || modeled(node, coverage));
    }
    private static Predicate predicate(Ast.IfStatement branch, ResolutionContracts.ProgramUnitId unit,
            boolean intact, boolean input, Map<ScalarMoveSemantics.NodeKey, ReferenceResolution.Entry> reads,
            Map<ResolutionContracts.SemanticEntityId, ScalarMoveSemantics.ScalarText> scalars,
            Map<Integer, SemanticCoverage.Finding> coverage, long[] work) {
        var condition = branch.condition();
        if (intact && condition instanceof Ast.RelationCondition relation
                && relation.operatorKind() == Ast.RelationOperator.EQUAL && exactSurface(relation, coverage)
                && relation.subject() instanceof Ast.DataReference reference
                && reference.understanding() == Ast.ReferenceUnderstanding.STRUCTURED
                && reference.qualifiers().isEmpty() && reference.subscriptGroups().isEmpty()
                && reference.referenceModification() == null && exactSurface(reference, coverage)
                && relation.object() instanceof Ast.LiteralExpression literal && literal.logicalText().isPresent()
                && exactSurface(literal, coverage)) {
            var entry = reads.get(new ScalarMoveSemantics.NodeKey(unit, reference.meta().id()));
            if (entry != null && entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED
                    && entry.candidates().size() == 1 && entry.selectedCandidate().isPresent()
                    && entry.occurrence().role() == ResolutionContracts.ReferenceRole.VALUE_READ
                    && entry.occurrence().meta().provenance().exact()) {
                var selected = entry.selectedCandidate().orElseThrow().entityId(); work[4]++;
                if (scalars.containsKey(selected))
                    return new Predicate(Availability.KNOWN, Optional.of(reference.meta().id()), Optional.of(selected), condition.meta().provenance());
            }
        }
        return new Predicate(input ? Availability.PARTIAL : Availability.INPUT_MISSING,
                Optional.empty(), Optional.empty(), condition.meta().provenance());
    }
    private static Arm arm(List<Ast.Statement> children, Ast.BranchPresence presence, Ast.SourceProvenance origin,
            ResolutionContracts.ProgramUnitId unit, boolean intact, boolean input,
            Map<ScalarMoveSemantics.NodeKey, ScalarMoveSemantics.Move> moves,
            Map<Integer, SemanticCoverage.Finding> coverage, long[] work) {
        boolean complete = intact && origin.exact() && presence != Ast.BranchPresence.UNKNOWN;
        if (presence == Ast.BranchPresence.PRESENT && children.isEmpty()) complete = false;
        if (presence == Ast.BranchPresence.ABSENT && !children.isEmpty()) complete = false;
        for (var child : children) {
            work[3]++;
            var move = moves.get(new ScalarMoveSemantics.NodeKey(unit, child.meta().id()));
            complete &= child instanceof Ast.MoveStatement && modeled(child, coverage)
                    && move != null && move.copy() != ScalarMoveSemantics.Copy.UNAVAILABLE && move.nextStatement().isPresent();
        }
        return new Arm(input ? presence : Ast.BranchPresence.UNKNOWN,
                complete ? Availability.KNOWN : input ? Availability.PARTIAL : Availability.INPUT_MISSING,
                intact && origin.exact() && !children.isEmpty() ? Optional.of(children.get(0).meta().id()) : Optional.empty(), origin);
    }
}
