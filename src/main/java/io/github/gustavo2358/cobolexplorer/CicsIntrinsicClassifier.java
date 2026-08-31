package io.github.gustavo2358.cobolexplorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Conservative CICS classification over canonical AST and completed COBOL binding products.
 * Occurrences and resolution entries are indexed once; the AST is visited once and every
 * supported construct has a constant-size nominal subtree (root plus one simple argument).
 */
final class CicsIntrinsicClassifier {
    private static final Set<String> SUPPORTED_BASE_NAMES = Set.of("DFHRESP", "DFHVALUE");

    ExternalClassification classify(
            CompilationUnitModel model,
            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrencesByUnit,
            ReferenceResolution resolution) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(occurrencesByUnit, "occurrencesByUnit");
        Objects.requireNonNull(resolution, "resolution");

        Map<ResolutionContracts.ProgramUnitId, Map<Integer, ReferenceOccurrences.Occurrence>>
                occurrencesByAstNode = indexOccurrences(occurrencesByUnit);
        Map<OccurrenceKey, ReferenceResolution.Entry> entriesByOccurrence = indexResolution(resolution);
        List<ExternalClassification.Entry> classifications = new ArrayList<>();

        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            Map<Integer, ReferenceOccurrences.Occurrence> unitOccurrences =
                    occurrencesByAstNode.getOrDefault(unit.id(), Map.of());
            for (Ast.Node node : nodes(unit.program())) {
                if (!(node instanceof Ast.DataReference root) || !hasSupportedShape(root)) continue;
                ReferenceOccurrences.Occurrence rootOccurrence = unitOccurrences.get(root.meta().id());
                if (rootOccurrence == null) continue;
                ReferenceResolution.Entry rootEntry = entriesByOccurrence.get(
                        new OccurrenceKey(unit.id(), rootOccurrence.id()));
                if (!coherent(rootEntry, rootOccurrence)
                        || rootEntry.status() != ResolutionContracts.ResolutionStatus.UNRESOLVED) continue;

                List<Integer> coveredOccurrenceIds = coveredOccurrences(
                        unit.id(), root, unitOccurrences, entriesByOccurrence);
                if (coveredOccurrenceIds.isEmpty()
                        || !coveredOccurrenceIds.contains(rootOccurrence.id())) continue;
                classifications.add(new ExternalClassification.Entry(
                        classifications.size(), unit.id(), root.meta().id(), rootOccurrence.id(), root.writtenText(),
                        ExternalClassification.Technology.CICS,
                        ExternalClassification.Kind.POSSIBLE_INTRINSIC,
                        ExternalClassification.Certainty.INFERRED,
                        ExternalClassification.Reason.COBOL_REFERENCE_UNRESOLVED_WITH_KNOWN_CICS_SHAPE,
                        root.meta(), coveredOccurrenceIds));
            }
        }
        return new ExternalClassification(classifications);
    }

    private static boolean hasSupportedShape(Ast.DataReference root) {
        if (!SUPPORTED_BASE_NAMES.contains(SymbolTable.canonical(root.baseName()))
                || root.understanding() != Ast.ReferenceUnderstanding.STRUCTURED
                || !root.qualifiers().isEmpty()
                || root.subscriptGroups().size() != 1
                || root.referenceModification() != null) return false;
        List<Ast.Expression> arguments = root.subscriptGroups().get(0).subscripts();
        return arguments.size() == 1 && isSupportedArgument(arguments.get(0));
    }

    private static boolean isSupportedArgument(Ast.Expression argument) {
        if (argument instanceof Ast.LiteralExpression) return true;
        if (!(argument instanceof Ast.DataReference reference)) return false;
        return reference.understanding() == Ast.ReferenceUnderstanding.STRUCTURED
                && reference.qualifiers().isEmpty()
                && reference.subscriptGroups().isEmpty()
                && reference.referenceModification() == null;
    }

    private static Map<ResolutionContracts.ProgramUnitId, Map<Integer, ReferenceOccurrences.Occurrence>>
            indexOccurrences(Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrencesByUnit) {
        Map<ResolutionContracts.ProgramUnitId, Map<Integer, ReferenceOccurrences.Occurrence>> result =
                new LinkedHashMap<>();
        for (Map.Entry<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> unitEntry
                : occurrencesByUnit.entrySet()) {
            Map<Integer, ReferenceOccurrences.Occurrence> byAstNode = new HashMap<>();
            for (ReferenceOccurrences.Occurrence occurrence : unitEntry.getValue().occurrences()) {
                if (!occurrence.programUnitId().equals(unitEntry.getKey())) continue;
                ReferenceOccurrences.Occurrence previous =
                        byAstNode.put(occurrence.referenceAstNodeId(), occurrence);
                if (previous != null) return Map.of();
            }
            result.put(unitEntry.getKey(), Map.copyOf(byAstNode));
        }
        return Map.copyOf(result);
    }

    private static Map<OccurrenceKey, ReferenceResolution.Entry> indexResolution(
            ReferenceResolution resolution) {
        Map<OccurrenceKey, ReferenceResolution.Entry> result = new HashMap<>();
        for (ReferenceResolution.Entry entry : resolution.entries()) {
            ReferenceOccurrences.Occurrence occurrence = entry.occurrence();
            OccurrenceKey key = new OccurrenceKey(occurrence.programUnitId(), occurrence.id());
            if (result.put(key, entry) != null) return Map.of();
        }
        return Map.copyOf(result);
    }

    private static List<Integer> coveredOccurrences(
            ResolutionContracts.ProgramUnitId unitId, Ast.DataReference root,
            Map<Integer, ReferenceOccurrences.Occurrence> occurrencesByAstNode,
            Map<OccurrenceKey, ReferenceResolution.Entry> entriesByOccurrence) {
        List<Integer> result = new ArrayList<>();
        Ast.Expression argument = root.subscriptGroups().get(0).subscripts().get(0);
        for (Ast.Node node : List.of(root, argument)) {
            ReferenceOccurrences.Occurrence occurrence = occurrencesByAstNode.get(node.meta().id());
            if (occurrence == null) continue;
            ReferenceResolution.Entry entry = entriesByOccurrence.get(
                    new OccurrenceKey(unitId, occurrence.id()));
            if (!coherent(entry, occurrence)) return List.of();
            result.add(occurrence.id());
        }
        return result.stream().sorted().toList();
    }

    private static boolean coherent(ReferenceResolution.Entry entry,
                                    ReferenceOccurrences.Occurrence occurrence) {
        return entry != null
                && entry.occurrence().programUnitId().equals(occurrence.programUnitId())
                && entry.occurrence().id() == occurrence.id()
                && entry.occurrence().referenceAstNodeId() == occurrence.referenceAstNodeId();
    }

    private static List<Ast.Node> nodes(Ast.Node root) {
        List<Ast.Node> result = new ArrayList<>();
        addNodes(root, result);
        return result;
    }

    private static void addNodes(Ast.Node node, List<Ast.Node> result) {
        result.add(node);
        for (Ast.Node child : Ast.children(node)) addNodes(child, result);
    }

    private record OccurrenceKey(ResolutionContracts.ProgramUnitId programUnitId, int occurrenceId) { }
}
