package io.github.gustavo2358.cobolexplorer.semanticproduct.loweringreadiness;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Small boundary consumer used only to falsify downstream-readiness claims.
 * It copies the minimum observable structure and performs no language analysis.
 */
public final class SemanticPortLoweringProbe {
    private SemanticPortLoweringProbe() { }

    public static Reconstruction reconstruct(CobolSemanticPort port) {
        Objects.requireNonNull(port, "port");
        List<Violation> violations = new ArrayList<>();
        CobolSemanticProduct.UnitId unit = port.unit();

        Map<CobolSemanticProduct.DataItemId, CobolSemanticProduct.DataDeclaration>
                declarations = declarations(port, unit, violations);
        Map<CobolSemanticProduct.StatementId, CobolSemanticProduct.StatementFact> statements =
                statements(port, unit, violations);
        Map<CobolSemanticProduct.StatementId, List<CobolSemanticProduct.Gap>> gaps =
                gaps(port, statements, violations);

        validateStatementIndex(port, statements, violations);
        validateRoots(port, statements, violations);
        validateStructure(port, statements, gaps, violations);
        validateCoverage(port.coverage(), statements.values(), violations);
        for (var entry : port.entries()) {
            if (!entry.id().unit().equals(unit))
                global(violations, "ENTRY_CROSSES_UNIT", "entry does not belong to the publication");
            entry.start().statement().ifPresent(target -> {
                if (!target.unit().equals(unit) || !statements.containsKey(target))
                    global(violations, "ENTRY_START_TARGET_MISSING", "entry start has no published statement in its unit");
            });
        }

        List<DataNode> dataNodes = port.dataDeclarations().stream()
                .map(data -> new DataNode(data.id(), data.canonicalName(), data.picture(),
                        data.provenance(), data.coverage(), data.readiness()))
                .toList();
        List<StatementNode> statementNodes = new ArrayList<>();
        for (CobolSemanticProduct.StatementFact fact : statements.values()) {
            List<CobolSemanticProduct.Gap> localized =
                    gaps.getOrDefault(fact.header().id(), List.of());
            statementNodes.add(reconstruct(port, fact, declarations, localized, violations));
        }

        return new Reconstruction(unit, dataNodes, statementNodes, port.rootStatements(),
                port.gaps(), port.coverage(), violations, port.entries().stream().map(entry ->
                        new EntryNode(entry.id(), entry.role(), entry.availability(), entry.start(),
                                entry.signature(), entry.provenance(), entry.coverage(), entry.readiness(),
                                entry.gaps())).toList(),
                new EntryInventoryNode(port.entryInventory().scope(), port.entryInventory().status(),
                        port.entryInventory().gapCodes()));
    }

    private static Map<CobolSemanticProduct.DataItemId,
            CobolSemanticProduct.DataDeclaration> declarations(
            CobolSemanticPort port,
            CobolSemanticProduct.UnitId unit,
            List<Violation> violations) {
        Map<CobolSemanticProduct.DataItemId, CobolSemanticProduct.DataDeclaration> result =
                new LinkedHashMap<>();
        for (CobolSemanticProduct.DataDeclaration declaration : port.dataDeclarations()) {
            if (!declaration.id().unit().equals(unit))
                global(violations, "DATA_CROSSES_UNIT",
                        "DATA identity is outside the port unit");
            if (result.put(declaration.id(), declaration) != null)
                global(violations, "DUPLICATE_DATA_ID", "DATA identity is duplicated");
            if (declaration.readiness().lowering().status()
                    == CobolSemanticProduct.ReadinessStatus.SUFFICIENT
                    && declaration.coverage()
                    != CobolSemanticProduct.CoverageStatus.MODELED)
                global(violations, "DATA_READY_WITH_INCOMPLETE_COVERAGE",
                        "lowering-ready DATA must have modeled declaration coverage");
        }
        return result;
    }

    private static Map<CobolSemanticProduct.StatementId,
            CobolSemanticProduct.StatementFact> statements(
            CobolSemanticPort port,
            CobolSemanticProduct.UnitId unit,
            List<Violation> violations) {
        Map<CobolSemanticProduct.StatementId, CobolSemanticProduct.StatementFact> result =
                new LinkedHashMap<>();
        Set<Integer> points = new LinkedHashSet<>();
        int previous = -1;
        for (CobolSemanticProduct.StatementFact fact : port.statements()) {
            CobolSemanticProduct.StatementHeader header = fact.header();
            if (!header.id().unit().equals(unit))
                at(violations, "STATEMENT_CROSSES_UNIT", header.id(),
                        "statement identity is outside the port unit");
            if (result.put(header.id(), fact) != null)
                at(violations, "DUPLICATE_STATEMENT_ID", header.id(),
                        "statement identity is duplicated");
            if (!points.add(header.point().ordinal()))
                at(violations, "DUPLICATE_PROGRAM_POINT", header.id(),
                        "structural program point is duplicated");
            if (header.point().ordinal() <= previous)
                at(violations, "PROGRAM_POINTS_OUT_OF_ORDER", header.id(),
                        "statement inventory is not in structural program-point order");
            previous = header.point().ordinal();
        }
        return result;
    }

    private static Map<CobolSemanticProduct.StatementId,
            List<CobolSemanticProduct.Gap>> gaps(
            CobolSemanticPort port,
            Map<CobolSemanticProduct.StatementId, CobolSemanticProduct.StatementFact> statements,
            List<Violation> violations) {
        Map<CobolSemanticProduct.StatementId, List<CobolSemanticProduct.Gap>> result =
                new LinkedHashMap<>();
        for (CobolSemanticProduct.Gap gap : port.gaps()) {
            if (!statements.containsKey(gap.statement()))
                at(violations, "GAP_REFERENCES_UNKNOWN_STATEMENT", gap.statement(),
                        "gap cannot be attached to a visible fact");
            result.computeIfAbsent(gap.statement(), ignored -> new ArrayList<>()).add(gap);
        }
        result.replaceAll((ignored, values) -> List.copyOf(values));
        return result;
    }

    private static void validateStatementIndex(
            CobolSemanticPort port,
            Map<CobolSemanticProduct.StatementId, CobolSemanticProduct.StatementFact> statements,
            List<Violation> violations) {
        for (Map.Entry<CobolSemanticProduct.StatementId,
                CobolSemanticProduct.StatementFact> entry : statements.entrySet()) {
            if (!port.statement(entry.getKey()).filter(entry.getValue()::equals).isPresent())
                at(violations, "STATEMENT_LOOKUP_MISMATCH", entry.getKey(),
                        "statement(id) contradicts statements()");
        }
    }

    private static void validateRoots(
            CobolSemanticPort port,
            Map<CobolSemanticProduct.StatementId, CobolSemanticProduct.StatementFact> statements,
            List<Violation> violations) {
        List<CobolSemanticProduct.StatementId> expected = statements.values().stream()
                .filter(fact -> fact.header().containment().branch()
                        == CobolSemanticProduct.Branch.ROOT)
                .map(fact -> fact.header().id()).toList();
        if (!expected.equals(port.rootStatements()))
            global(violations, "ROOT_INDEX_MISMATCH",
                    "rootStatements() contradicts statement containment");
    }

    private static void validateStructure(
            CobolSemanticPort port,
            Map<CobolSemanticProduct.StatementId, CobolSemanticProduct.StatementFact> statements,
            Map<CobolSemanticProduct.StatementId, List<CobolSemanticProduct.Gap>> gaps,
            List<Violation> violations) {
        for (CobolSemanticProduct.StatementFact fact : statements.values()) {
            CobolSemanticProduct.StatementHeader header = fact.header();
            CobolSemanticProduct.StatementId id = header.id();
            List<CobolSemanticProduct.Gap> localized = gaps.getOrDefault(id, List.of());
            if (header.coverage() != CobolSemanticProduct.CoverageStatus.MODELED
                    && localized.isEmpty())
                at(violations, "INCOMPLETE_FACT_WITHOUT_GAP", id,
                        "non-modeled fact lost its localized uncertainty");
            if (header.containment().branch() == CobolSemanticProduct.Branch.UNKNOWN) {
                if (!hasScope(localized, CobolSemanticProduct.GapScope.STRUCTURE))
                    at(violations, "UNKNOWN_CONTAINMENT_WITHOUT_GAP", id,
                            "unknown containment has no structural gap");
                if (header.readiness().cfg().status()
                        == CobolSemanticProduct.ReadinessStatus.SUFFICIENT)
                    at(violations, "CFG_READY_WITH_UNKNOWN_CONTAINMENT", id,
                            "CFG readiness cannot hide unknown containment");
            }
            header.containment().parent().ifPresent(parent -> {
                if (!(statements.get(parent) instanceof CobolSemanticProduct.IfFact))
                    at(violations, "UNKNOWN_BRANCH_PARENT", id,
                            "branch containment does not name a visible IF");
            });

            if (fact instanceof CobolSemanticProduct.IfFact branch)
                validateIfStructure(port, branch, statements, localized, violations);
        }
    }

    private static void validateIfStructure(
            CobolSemanticPort port,
            CobolSemanticProduct.IfFact branch,
            Map<CobolSemanticProduct.StatementId, CobolSemanticProduct.StatementFact> statements,
            List<CobolSemanticProduct.Gap> localized,
            List<Violation> violations) {
        CobolSemanticProduct.StatementId id = branch.header().id();
        for (CobolSemanticProduct.Branch side : List.of(
                CobolSemanticProduct.Branch.THEN, CobolSemanticProduct.Branch.ELSE)) {
            List<CobolSemanticProduct.StatementFact> expected = statements.values().stream()
                    .filter(fact -> fact.header().containment().parent()
                            .filter(id::equals).isPresent())
                    .filter(fact -> fact.header().containment().branch() == side)
                    .toList();
            List<CobolSemanticProduct.StatementFact> actual = port.children(id, side);
            if (!expected.equals(actual))
                at(violations, "BRANCH_MEMBERSHIP_MISMATCH", id,
                        side + " membership contradicts child containment");
            for (CobolSemanticProduct.StatementFact child : actual) {
                if (!statements.containsKey(child.header().id())
                        || !child.header().containment().equals(
                        CobolSemanticProduct.Containment.childOf(id, side)))
                    at(violations, "BRANCH_MEMBER_CONTRADICTS_CONTAINMENT", id,
                            side + " query returned a non-member");
            }
        }
        branch.continuation().ifPresent(continuation -> {
            if (!statements.containsKey(continuation))
                at(violations, "CONTINUATION_TARGET_MISSING", id,
                        "IF continuation does not name a visible statement");
        });
        if (branch.header().readiness().cfg().status()
                == CobolSemanticProduct.ReadinessStatus.SUFFICIENT
                && hasScope(localized, CobolSemanticProduct.GapScope.STRUCTURE))
            at(violations, "CFG_READY_WITH_STRUCTURE_GAP", id,
                    "CFG-ready IF still carries missing structure");
        if (branch.header().readiness().lowering().status()
                == CobolSemanticProduct.ReadinessStatus.SUFFICIENT)
            at(violations, "IF_LOWERING_READY_WITHOUT_PREDICATE_SEMANTICS", id,
                    "ConditionSurface does not publish a normalized predicate");
    }

    private static void validateCoverage(
            CobolSemanticProduct.CoverageSummary coverage,
            Iterable<CobolSemanticProduct.StatementFact> statements,
            List<Violation> violations) {
        int observed = 0;
        Map<CobolSemanticProduct.CoverageStatus, Integer> counts = new LinkedHashMap<>();
        for (CobolSemanticProduct.StatementFact fact : statements) {
            observed++;
            counts.merge(fact.header().coverage(), 1, Integer::sum);
        }
        if (coverage.observedStatements() != observed)
            global(violations, "COVERAGE_STATEMENT_COUNT_MISMATCH",
                    "coverage does not match the positive statement inventory");
        if (coverage.modeledStatements()
                != counts.getOrDefault(CobolSemanticProduct.CoverageStatus.MODELED, 0)
                || coverage.partialStatements()
                != counts.getOrDefault(CobolSemanticProduct.CoverageStatus.PARTIAL, 0)
                || coverage.unsupportedStatements()
                != counts.getOrDefault(CobolSemanticProduct.CoverageStatus.UNSUPPORTED, 0)
                || coverage.inputMissingStatements()
                != counts.getOrDefault(CobolSemanticProduct.CoverageStatus.INPUT_MISSING, 0))
            global(violations, "COVERAGE_CLASSIFICATION_MISMATCH",
                    "coverage buckets contradict statement facts");
        validateAggregateReadiness(coverage.readiness().lowering(), statements,
                readiness -> readiness.lowering(), "LOWERING", violations);
        validateAggregateReadiness(coverage.readiness().cfg(), statements,
                readiness -> readiness.cfg(), "CFG", violations);
        validateAggregateReadiness(coverage.readiness().effectsDataflow(), statements,
                readiness -> readiness.effectsDataflow(), "EFFECTS_DATAFLOW", violations);
        if (coverage.inventoryStatus() != CobolSemanticProduct.InventoryStatus.COMPLETE
                && List.of(coverage.readiness().lowering(), coverage.readiness().cfg(),
                        coverage.readiness().effectsDataflow()).stream().anyMatch(claim ->
                        claim.status() == CobolSemanticProduct.ReadinessStatus.SUFFICIENT))
            global(violations, "INCOMPLETE_INVENTORY_CLAIMS_SUFFICIENT",
                    "incomplete inventory cannot make a sufficient aggregate claim");
    }

    private static void validateAggregateReadiness(
            CobolSemanticProduct.ReadinessClaim aggregate,
            Iterable<CobolSemanticProduct.StatementFact> statements,
            Function<CobolSemanticProduct.Readiness,
                    CobolSemanticProduct.ReadinessClaim> dimension,
            String name,
            List<Violation> violations) {
        int weakest = Integer.MAX_VALUE;
        for (CobolSemanticProduct.StatementFact fact : statements) {
            CobolSemanticProduct.ReadinessStatus status =
                    dimension.apply(fact.header().readiness()).status();
            if (status == CobolSemanticProduct.ReadinessStatus.NOT_APPLICABLE) continue;
            weakest = Math.min(weakest, readinessRank(status));
        }
        if (weakest != Integer.MAX_VALUE
                && readinessRank(aggregate.status()) > weakest)
            global(violations, "AGGREGATE_" + name + "_EXCEEDS_FACTS",
                    "aggregate readiness exceeds its weakest visible fact");
    }

    private static int readinessRank(CobolSemanticProduct.ReadinessStatus status) {
        return switch (status) {
            case BLOCKED -> 0;
            case PARTIAL -> 1;
            case SUFFICIENT -> 2;
            case NOT_APPLICABLE -> -1;
        };
    }

    private static StatementNode reconstruct(
            CobolSemanticPort port,
            CobolSemanticProduct.StatementFact fact,
            Map<CobolSemanticProduct.DataItemId, CobolSemanticProduct.DataDeclaration>
                    declarations,
            List<CobolSemanticProduct.Gap> gaps,
            List<Violation> violations) {
        Anchor anchor = anchor(fact.header());
        if (fact instanceof CobolSemanticProduct.GobackFact goback)
            return new GobackNode(anchor, goback.exit(), goback.localContinuation());
        if (fact instanceof CobolSemanticProduct.MoveFact move) {
            ReferenceNode target = reference(move.target(), declarations);
            validateOperandOwner(anchor, move.source().id(), violations);
            validateOperandOwner(anchor, target.id(), violations);
            if (move.source().kind() == CobolSemanticProduct.LiteralKind.UNKNOWN) {
                if (!hasScope(gaps, CobolSemanticProduct.GapScope.LITERAL_KIND))
                    at(violations, "UNKNOWN_LITERAL_KIND_WITHOUT_GAP", anchor.id(),
                            "literal kind uncertainty disappeared");
                if (anchor.readiness().lowering().status()
                        == CobolSemanticProduct.ReadinessStatus.SUFFICIENT)
                    at(violations, "MOVE_READY_WITH_UNKNOWN_LITERAL_KIND", anchor.id(),
                            "typed literal semantics are absent");
            }
            validateReadyBinding(anchor, target, violations);
            return new MoveNode(anchor,
                    new LiteralNode(move.source().id(), move.source().kind(),
                            move.source().value(), move.source().provenance()), target);
        }
        if (fact instanceof CobolSemanticProduct.CallFact call) {
            ReferenceNode operand = reference(((CobolSemanticProduct.DataReference) call.target()), declarations);
            validateOperandOwner(anchor, operand.id(), violations);
            validateReadyBinding(anchor, operand, violations);
            if (!hasGap(gaps, CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET,
                    call.runtimeUncertaintyCode()))
                at(violations, "CALL_RUNTIME_UNKNOWN_WITHOUT_GAP", anchor.id(),
                        "runtime target uncertainty disappeared");
            return new CallNode(anchor, call.syntax(), operand, call.runtimeTarget(),
                    call.runtimeUncertaintyCode());
        }
        if (fact instanceof CobolSemanticProduct.IfFact branch) {
            List<ReferenceNode> references = branch.condition().references().stream()
                    .map(reference -> reference(reference, declarations)).toList();
            for (ReferenceNode reference : references) {
                validateOperandOwner(anchor, reference.id(), violations);
                if (reference.role() != CobolSemanticProduct.OperandRole.READ)
                    at(violations, "CONDITION_REFERENCE_ROLE_MISMATCH", anchor.id(),
                            "condition reference is not a READ");
                if (anchor.readiness().effectsDataflow().status()
                        == CobolSemanticProduct.ReadinessStatus.SUFFICIENT)
                    validateReadyBinding(anchor, reference, violations);
            }
            return new IfNode(anchor,
                    new ConditionNode(branch.condition().shape(), references,
                            PredicateKnowledge.NOT_PUBLISHED,
                            branch.condition().provenance()),
                    childIds(port, branch.header().id(), CobolSemanticProduct.Branch.THEN),
                    childIds(port, branch.header().id(), CobolSemanticProduct.Branch.ELSE),
                    branch.explicitlyTerminated(), branch.continuation());
        }
        CobolSemanticProduct.ObservedStatement observed =
                (CobolSemanticProduct.ObservedStatement) fact;
        if (!hasGap(gaps, CobolSemanticProduct.GapScope.CAPABILITY,
                observed.gapCode()))
            at(violations, "OBSERVED_WITHOUT_CAPABILITY_GAP", anchor.id(),
                    "positive observed fact lost its capability gap");
        if (List.of(anchor.readiness().lowering().status(),
                        anchor.readiness().cfg().status(),
                        anchor.readiness().effectsDataflow().status()).stream()
                .anyMatch(status -> status != CobolSemanticProduct.ReadinessStatus.BLOCKED))
            at(violations, "OBSERVED_SEMANTICS_CLAIMED_READY", anchor.id(),
                    "inventory-only statement cannot claim semantic readiness");
        return new ObservedNode(anchor, observed.observedKind(), observed.observedShape(),
                observed.gapCode());
    }

    private static List<CobolSemanticProduct.StatementId> childIds(
            CobolSemanticPort port,
            CobolSemanticProduct.StatementId parent,
            CobolSemanticProduct.Branch branch) {
        return port.children(parent, branch).stream()
                .map(fact -> fact.header().id()).toList();
    }

    private static Anchor anchor(CobolSemanticProduct.StatementHeader header) {
        return new Anchor(header.id(), header.point().ordinal(), header.containment(),
                header.provenance(), header.coverage(), header.readiness());
    }

    private static ReferenceNode reference(
            CobolSemanticProduct.DataReference reference,
            Map<CobolSemanticProduct.DataItemId, CobolSemanticProduct.DataDeclaration>
                    declarations) {
        Optional<String> selectedName = reference.binding().selected()
                .map(declarations::get)
                .map(CobolSemanticProduct.DataDeclaration::canonicalName);
        return new ReferenceNode(reference.id(), reference.role(), reference.binding().status(),
                reference.binding().reason(), reference.binding().candidates(),
                reference.binding().selected(), selectedName, reference.provenance());
    }

    private static void validateReadyBinding(
            Anchor anchor,
            ReferenceNode reference,
            List<Violation> violations) {
        if (anchor.readiness().lowering().status()
                != CobolSemanticProduct.ReadinessStatus.SUFFICIENT)
            return;
        boolean complete = reference.status() == CobolSemanticProduct.ResolutionStatus.RESOLVED
                && reference.candidates().size() == 1
                && reference.selected().isPresent()
                && reference.selectedName().isPresent()
                && reference.candidates().get(0).id().equals(reference.selected().get());
        if (!complete)
            at(violations, "LOWERING_READY_BINDING_INCOMPLETE", anchor.id(),
                    "lowering-ready nominal operand has no coherent selected declaration");
    }

    private static void validateOperandOwner(
            Anchor anchor,
            CobolSemanticProduct.OperandId operand,
            List<Violation> violations) {
        if (!operand.statement().equals(anchor.id()))
            at(violations, "OPERAND_IDENTITY_CROSSES_STATEMENT", anchor.id(),
                    "operand identity belongs to another statement");
    }

    private static boolean hasScope(
            List<CobolSemanticProduct.Gap> gaps,
            CobolSemanticProduct.GapScope scope) {
        return gaps.stream().anyMatch(gap -> gap.scope() == scope);
    }

    private static boolean hasGap(
            List<CobolSemanticProduct.Gap> gaps,
            CobolSemanticProduct.GapScope scope,
            String code) {
        return gaps.stream().anyMatch(gap -> gap.scope() == scope && gap.code().equals(code));
    }

    private static void global(List<Violation> violations, String code, String detail) {
        violations.add(new Violation(code, Optional.empty(), detail));
    }

    private static void at(List<Violation> violations, String code,
                           CobolSemanticProduct.StatementId statement, String detail) {
        violations.add(new Violation(code, Optional.of(statement), detail));
    }

    public enum PredicateKnowledge { NOT_PUBLISHED }

    public record Reconstruction(
            CobolSemanticProduct.UnitId unit,
            List<DataNode> data,
            List<StatementNode> statements,
            List<CobolSemanticProduct.StatementId> roots,
            List<CobolSemanticProduct.Gap> gaps,
            CobolSemanticProduct.CoverageSummary coverage,
            List<Violation> violations,
            List<EntryNode> entries,
            EntryInventoryNode entryInventory) {
        public Reconstruction {
            data = List.copyOf(data);
            statements = List.copyOf(statements);
            roots = List.copyOf(roots);
            gaps = List.copyOf(gaps);
            violations = List.copyOf(violations);
            entries = List.copyOf(entries);
        }

        public boolean valid() {
            return violations.isEmpty();
        }

        public <T extends StatementNode> List<T> statements(Class<T> type) {
            return statements.stream().filter(type::isInstance).map(type::cast).toList();
        }
    }

    public record EntryNode(CobolSemanticProduct.EntryId id, CobolSemanticProduct.EntryRole role,
                             CobolSemanticProduct.Availability availability,
                             CobolSemanticProduct.ExecutableStart start,
                             CobolSemanticProduct.EntrySignature signature,
                             CobolSemanticProduct.Provenance provenance,
                             CobolSemanticProduct.CoverageStatus coverage,
                             CobolSemanticProduct.Readiness readiness,
                             List<CobolSemanticProduct.EntryGap> gaps) {
        public EntryNode { gaps = List.copyOf(gaps); }
    }

    public record EntryInventoryNode(CobolSemanticProduct.EntryInventoryScope scope,
                                     CobolSemanticProduct.InventoryStatus status, List<String> gapCodes) {
        public EntryInventoryNode { gapCodes = List.copyOf(gapCodes); }
    }

    public record DataNode(
            CobolSemanticProduct.DataItemId id,
            String canonicalName,
            Optional<String> picture,
            CobolSemanticProduct.Provenance provenance,
            CobolSemanticProduct.CoverageStatus coverage,
            CobolSemanticProduct.Readiness readiness) { }

    public record Anchor(
            CobolSemanticProduct.StatementId id,
            int programPoint,
            CobolSemanticProduct.Containment containment,
            CobolSemanticProduct.Provenance provenance,
            CobolSemanticProduct.CoverageStatus coverage,
            CobolSemanticProduct.Readiness readiness) { }

    public record LiteralNode(
            CobolSemanticProduct.OperandId id,
            CobolSemanticProduct.LiteralKind kind,
            String value,
            CobolSemanticProduct.Provenance provenance) { }

    public record ReferenceNode(
            CobolSemanticProduct.OperandId id,
            CobolSemanticProduct.OperandRole role,
            CobolSemanticProduct.ResolutionStatus status,
            CobolSemanticProduct.ResolutionReason reason,
            List<CobolSemanticProduct.DataCandidate> candidates,
            Optional<CobolSemanticProduct.DataItemId> selected,
            Optional<String> selectedName,
            CobolSemanticProduct.Provenance provenance) {
        public ReferenceNode {
            candidates = List.copyOf(candidates);
        }
    }

    public record ConditionNode(
            String shape,
            List<ReferenceNode> references,
            PredicateKnowledge predicateKnowledge,
            CobolSemanticProduct.Provenance provenance) {
        public ConditionNode {
            references = List.copyOf(references);
        }
    }

    public sealed interface StatementNode permits MoveNode, CallNode, IfNode, ObservedNode, GobackNode {
        Anchor anchor();
    }

    public record GobackNode(Anchor anchor, CobolSemanticProduct.GobackExit exit,
                             CobolSemanticProduct.LocalContinuation localContinuation) implements StatementNode { }

    public record MoveNode(Anchor anchor, LiteralNode source,
                           ReferenceNode target) implements StatementNode { }

    public record CallNode(Anchor anchor, CobolSemanticProduct.CallSyntax syntax,
                           ReferenceNode operand,
                           CobolSemanticProduct.RuntimeTargetKnowledge runtimeTarget,
                           String runtimeUncertaintyCode) implements StatementNode { }

    public record IfNode(Anchor anchor, ConditionNode condition,
                         List<CobolSemanticProduct.StatementId> thenMembers,
                         List<CobolSemanticProduct.StatementId> elseMembers,
                         boolean explicitlyTerminated,
                         Optional<CobolSemanticProduct.StatementId> continuation)
            implements StatementNode {
        public IfNode {
            thenMembers = List.copyOf(thenMembers);
            elseMembers = List.copyOf(elseMembers);
        }
    }

    public record ObservedNode(Anchor anchor, String observedKind,
                               String observedShape,
                               String gapCode) implements StatementNode { }

    public record Violation(String code,
                            Optional<CobolSemanticProduct.StatementId> statement,
                            String detail) { }
}
