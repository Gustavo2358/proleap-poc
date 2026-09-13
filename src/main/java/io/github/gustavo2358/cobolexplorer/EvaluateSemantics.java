package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Canonical EVALUATE proofs. No runtime truth evaluation or downstream types. */
public final class EvaluateSemantics {
    public record Facts(boolean supportedShape, boolean structureKnown,
                        Optional<ResolutionContracts.SemanticEntityId> wholeItem,
                        Optional<Integer> nextStatement) { }
    private final Map<ScalarMoveSemantics.NodeKey, Facts> facts;
    private EvaluateSemantics(Map<ScalarMoveSemantics.NodeKey, Facts> facts) { this.facts = Map.copyOf(facts); }
    public Facts fact(ResolutionContracts.ProgramUnitId unit, int node) {
        return Objects.requireNonNull(facts.get(new ScalarMoveSemantics.NodeKey(unit, node)));
    }
    public static boolean supportedShape(Ast.EvaluateStatement e) {
        return e.simpleSubject() && e.subjects().size() == 1
                && e.subjects().get(0) instanceof Ast.DataReference r
                && r.understanding() == Ast.ReferenceUnderstanding.STRUCTURED
                && r.qualifiers().isEmpty() && r.subscriptGroups().isEmpty() && r.referenceModification() == null
                && e.explicitlyTerminated() && !e.branches().isEmpty()
                && e.branches().stream().anyMatch(a -> !a.other())
                && e.branches().stream().allMatch(a -> a.other() || a.selectors().size() == 1
                    && a.selectors().get(0).subjectIndex() == 0
                    && a.selectors().get(0).context() == Ast.EvaluateSelectorContext.SIMPLE_LITERAL);
    }
    static EvaluateSemantics analyze(CompilationUnitBuildResult frontend, ReferenceResolution resolution,
            ResolutionAnalysisReport report, Map<ResolutionContracts.SemanticEntityId, ScalarMoveSemantics.ScalarText> scalars) {
        var refs = new HashMap<ScalarMoveSemantics.NodeKey, ReferenceResolution.Entry>();
        for (var ref : resolution.entries()) refs.put(new ScalarMoveSemantics.NodeKey(
                ref.occurrence().programUnitId(), ref.occurrence().referenceAstNodeId()), ref);
        boolean input = report.gaps().stream().noneMatch(g -> g.category() == ResolutionAnalysisReport.GapCategory.INPUT);
        var result = new HashMap<ScalarMoveSemantics.NodeKey, Facts>();
        for (var unit : frontend.compilationUnit().programUnits()) {
            Map<Integer,Integer> next = Map.of();
            for (var d : unit.program().divisions()) if (d.divisionKind() == Ast.DivisionKind.PROCEDURE) next = d.normalContinuations();
            var pending = new ArrayDeque<Ast.Node>(); pending.push(unit.program());
            while (!pending.isEmpty()) {
                var node = pending.pop();
                if (node instanceof Ast.EvaluateStatement e) {
                    boolean shape = supportedShape(e);
                    boolean structure = shape && input && e.meta().provenance().exact()
                            && e.branches().stream().allMatch(a -> a.meta().provenance().exact());
                    Optional<ResolutionContracts.SemanticEntityId> whole = Optional.empty();
                    if (shape && structure) {
                        var ref = refs.get(new ScalarMoveSemantics.NodeKey(unit.id(), e.subjects().get(0).meta().id()));
                        if (ref != null && ref.status() == ResolutionContracts.ResolutionStatus.RESOLVED
                                && ref.candidates().size() == 1 && ref.selectedCandidate().isPresent()
                                && ref.occurrence().role() == ResolutionContracts.ReferenceRole.VALUE_READ
                                && ref.occurrence().meta().provenance().exact())
                            whole = ref.selectedCandidate().map(c -> c.entityId()).filter(scalars::containsKey);
                    }
                    result.put(new ScalarMoveSemantics.NodeKey(unit.id(), e.meta().id()), new Facts(shape, structure,
                            whole, structure ? Optional.ofNullable(next.get(e.meta().id())) : Optional.empty()));
                }
                for (var child : Ast.children(node)) pending.push(child);
            }
        }
        return new EvaluateSemantics(result);
    }
}
