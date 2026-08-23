package io.proleap.benchmark;

import java.util.*;

/** Resolves DATA/CONDITION/INDEX names without performing value or control-flow analysis. */
final class DataAndIndexReferenceResolver {
    private record IndexedUnit(CompilationUnitModel.ProgramUnit unit, SymbolTable table,
                               Map<String, List<SymbolTable.Symbol>> byName,
                               Map<Integer, Ast.Node> astNodes) { }
    private record Decision(ResolutionContracts.ResolutionStatus status,
                            ResolutionContracts.ResolutionReason reason,
                            List<ReferenceResolution.Candidate> candidates) { }

    private final ResolutionContracts.CobolResolutionPolicy policy;
    private final Map<ResolutionContracts.ProgramUnitId, IndexedUnit> units = new LinkedHashMap<>();
    private int indexedDeclarations;
    private int nominalLookups;
    private long candidateInspections;
    private int maximumCandidates;

    DataAndIndexReferenceResolver(ResolutionContracts.CobolResolutionPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    ReferenceResolution resolve(CompilationUnitModel model, CompilationUnitSymbolTables symbolTables,
                                Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrencesByUnit) {
        buildIndexes(model, symbolTables);
        List<ReferenceResolution.Entry> entries = new ArrayList<>();
        List<ReferenceResolution.Diagnostic> diagnostics = new ArrayList<>();
        Map<String, ReferenceResolution.Entry> entryByReference = new HashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            ReferenceOccurrences occurrences = Objects.requireNonNull(occurrencesByUnit.get(unit.id()),
                    "missing occurrences for " + unit.id());
            for (ReferenceOccurrences.Occurrence occurrence : occurrences.occurrences()) {
                Decision decision = switch (occurrence.kind()) {
                    case DATA, CONDITION, INDEX -> resolveDataOccurrence(unit.id(), occurrence);
                    default -> new Decision(ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                            ResolutionContracts.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM, List.of());
                };
                List<Integer> diagnosticIds = List.of();
                if (decision.status() != ResolutionContracts.ResolutionStatus.RESOLVED) {
                    int diagnosticId = diagnostics.size();
                    diagnostics.add(new ReferenceResolution.Diagnostic(diagnosticId,
                            "REFERENCE_" + decision.status() + "_" + decision.reason(),
                            diagnosticMessage(occurrence, decision), unit.id(), occurrence.id()));
                    diagnosticIds = List.of(diagnosticId);
                }
                ReferenceResolution.Entry entry = new ReferenceResolution.Entry(entries.size(), occurrence,
                        decision.status(), decision.reason(), decision.candidates(), diagnosticIds);
                entries.add(entry);
                entryByReference.put(referenceKey(unit.id(), occurrence.referenceAstNodeId()), entry);
            }
        }
        DeclarationRelationResolution relations = resolveRelations(symbolTables, entryByReference);
        return new ReferenceResolution(policy, entries, diagnostics,
                new ReferenceResolution.Metrics(indexedDeclarations, nominalLookups,
                        candidateInspections, maximumCandidates), relations);
    }

    private void buildIndexes(CompilationUnitModel model, CompilationUnitSymbolTables symbolTables) {
        units.clear(); indexedDeclarations = 0; nominalLookups = 0; candidateInspections = 0; maximumCandidates = 0;
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = symbolTables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            LinkedHashMap<String, List<SymbolTable.Symbol>> mutable = new LinkedHashMap<>();
            for (SymbolTable.Symbol symbol : table.symbols())
                mutable.computeIfAbsent(symbol.canonicalName(), ignored -> new ArrayList<>()).add(symbol);
            LinkedHashMap<String, List<SymbolTable.Symbol>> byName = new LinkedHashMap<>();
            mutable.forEach((name, symbols) -> byName.put(name, List.copyOf(symbols)));
            Map<Integer, Ast.Node> astNodes = new HashMap<>();
            indexAst(unit.program(), astNodes);
            units.put(unit.id(), new IndexedUnit(unit, table, Collections.unmodifiableMap(byName), Map.copyOf(astNodes)));
            indexedDeclarations += table.symbols().size();
        }
    }

    private Decision resolveDataOccurrence(ResolutionContracts.ProgramUnitId startingUnitId,
                                           ReferenceOccurrences.Occurrence occurrence) {
        IndexedUnit startingUnit = units.get(startingUnitId);
        Ast.Node node = startingUnit.astNodes().get(occurrence.referenceAstNodeId());
        String baseName = baseName(node, occurrence);
        String canonical = SymbolTable.canonical(baseName);
        List<SymbolOwner> compatible = compatibleCandidates(startingUnitId, canonical, occurrence.kind());
        List<SymbolOwner> visible = localOrInheritedGlobal(startingUnitId, compatible);
        List<SymbolOwner> qualified = applyQualification(visible, node);
        List<ReferenceResolution.Candidate> candidates = qualified.stream()
                .map(owner -> candidate(owner, occurrence.kind())).toList();
        maximumCandidates = Math.max(maximumCandidates, candidates.size());

        if (candidates.isEmpty()) {
            boolean anySameName = hasSameNameInSearchPath(startingUnitId, canonical);
            ResolutionContracts.ResolutionReason reason = anySameName && compatible.isEmpty()
                    ? ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT
                    : ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND;
            return new Decision(ResolutionContracts.ResolutionStatus.UNRESOLVED, reason, List.of());
        }
        if (candidates.size() == 1) {
            boolean qualifiedReference = node instanceof Ast.DataReference data && !data.qualifiers().isEmpty();
            return new Decision(ResolutionContracts.ResolutionStatus.RESOLVED,
                    qualifiedReference ? ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH
                            : ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                    candidates);
        }

        List<SymbolOwner> extended = qualifyExtend(qualified, node);
        if (policy.qualifyMode() == ResolutionContracts.QualifyMode.EXTEND && extended.size() == 1)
            return new Decision(ResolutionContracts.ResolutionStatus.RESOLVED,
                    ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH,
                    List.of(candidate(extended.get(0), occurrence.kind())));
        if (policy.qualifyMode() == ResolutionContracts.QualifyMode.UNSPECIFIED && extended.size() == 1)
            return new Decision(ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                    ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, candidates);
        return new Decision(ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, candidates);
    }

    private record SymbolOwner(ResolutionContracts.ProgramUnitId unitId, SymbolTable table,
                               SymbolTable.Symbol symbol) { }

    private List<SymbolOwner> compatibleCandidates(ResolutionContracts.ProgramUnitId startingUnitId,
                                                   String canonical,
                                                   ResolutionContracts.ReferenceKind kind) {
        List<SymbolOwner> result = new ArrayList<>();
        ResolutionContracts.ProgramUnitId current = startingUnitId;
        while (current != null) {
            IndexedUnit unit = units.get(current);
            nominalLookups++;
            List<SymbolTable.Symbol> sameName = unit.byName().getOrDefault(canonical, List.of());
            candidateInspections += sameName.size();
            for (SymbolTable.Symbol symbol : sameName)
                if (compatible(symbol, kind)) result.add(new SymbolOwner(current, unit.table(), symbol));
            current = unit.unit().parentId();
        }
        return result;
    }

    private List<SymbolOwner> localOrInheritedGlobal(ResolutionContracts.ProgramUnitId startingUnitId,
                                                     List<SymbolOwner> candidates) {
        List<SymbolOwner> local = candidates.stream().filter(owner -> owner.unitId().equals(startingUnitId)).toList();
        if (!local.isEmpty()) return local;
        ResolutionContracts.ProgramUnitId current = units.get(startingUnitId).unit().parentId();
        while (current != null) {
            ResolutionContracts.ProgramUnitId ancestor = current;
            List<SymbolOwner> global = candidates.stream()
                    .filter(owner -> owner.unitId().equals(ancestor))
                    .filter(owner -> "GLOBAL".equals(owner.symbol().attributes().get("visibility"))).toList();
            if (!global.isEmpty()) return global;
            current = units.get(current).unit().parentId();
        }
        return List.of();
    }

    private List<SymbolOwner> applyQualification(List<SymbolOwner> candidates, Ast.Node node) {
        if (!(node instanceof Ast.DataReference reference) || reference.qualifiers().isEmpty()) return candidates;
        List<String> qualifiers = reference.qualifiers().stream()
                .map(Ast.DataQualifier::name).map(SymbolTable::canonical).toList();
        return candidates.stream().filter(candidate -> orderedSubsequence(qualifiers, ancestry(candidate))).toList();
    }

    private List<SymbolOwner> qualifyExtend(List<SymbolOwner> candidates, Ast.Node node) {
        if (!(node instanceof Ast.DataReference reference)) return List.of();
        List<String> qualifiers = reference.qualifiers().stream()
                .map(Ast.DataQualifier::name).map(SymbolTable::canonical).toList();
        if (qualifiers.isEmpty()) {
            List<SymbolOwner> level01 = candidates.stream()
                    .filter(candidate -> ancestry(candidate).isEmpty()).toList();
            return level01.size() == 1 ? level01 : List.of();
        }
        List<SymbolOwner> fullyQualified = candidates.stream()
                .filter(candidate -> qualifiers.equals(ancestry(candidate))).toList();
        return fullyQualified.size() == 1 ? fullyQualified : List.of();
    }

    private List<String> ancestry(SymbolOwner owner) {
        List<String> result = new ArrayList<>();
        int scopeId = owner.symbol().scopeId();
        while (scopeId >= 0) {
            SymbolTable.Scope scope = owner.table().scopes().get(scopeId);
            if (scope.kind() == SymbolTable.ScopeKind.DATA_ITEM && scope.ownerSymbolId() >= 0)
                result.add(owner.table().symbols().get(scope.ownerSymbolId()).canonicalName());
            else if (scope.kind() == SymbolTable.ScopeKind.FILE_DESCRIPTION && scope.ownerSymbolId() >= 0)
                result.add(owner.table().symbols().get(scope.ownerSymbolId()).canonicalName());
            scopeId = scope.parentId();
        }
        return List.copyOf(result);
    }

    private boolean hasSameNameInSearchPath(ResolutionContracts.ProgramUnitId startingUnitId, String canonical) {
        ResolutionContracts.ProgramUnitId current = startingUnitId;
        while (current != null) {
            if (!units.get(current).byName().getOrDefault(canonical, List.of()).isEmpty()) return true;
            current = units.get(current).unit().parentId();
        }
        return false;
    }

    private static boolean compatible(SymbolTable.Symbol symbol, ResolutionContracts.ReferenceKind kind) {
        return switch (kind) {
            case INDEX -> symbol.kind() == SymbolTable.SymbolKind.INDEX_NAME;
            case CONDITION -> symbol.kind() == SymbolTable.SymbolKind.CONDITION_NAME;
            case DATA -> symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM
                    || symbol.kind() == SymbolTable.SymbolKind.RENAMES;
            default -> false;
        };
    }

    private ReferenceResolution.Candidate candidate(SymbolOwner owner,
                                                     ResolutionContracts.ReferenceKind kind) {
        ResolutionContracts.SemanticEntityDomain domain = kind == ResolutionContracts.ReferenceKind.INDEX
                ? ResolutionContracts.SemanticEntityDomain.INDEX_SYMBOL
                : ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL;
        return new ReferenceResolution.Candidate(new ResolutionContracts.SemanticEntityId(
                owner.unitId(), domain, owner.symbol().id()), kind, owner.symbol().writtenName(),
                owner.symbol().canonicalName(), List.of(owner.symbol().id()),
                Map.of("symbolKind", owner.symbol().kind().name(),
                        "visibility", owner.symbol().attributes().getOrDefault("visibility", "LOCAL")));
    }

    private DeclarationRelationResolution resolveRelations(CompilationUnitSymbolTables symbolTables,
                                                            Map<String, ReferenceResolution.Entry> entriesByReference) {
        List<DeclarationRelationResolution.Entry> result = new ArrayList<>();
        for (CompilationUnitSymbolTables.UnitSymbols unit : symbolTables.units()) {
            for (SymbolTable.DeclarationRelation relation : unit.symbolTable().declarationRelations()) {
                ReferenceResolution.Entry binding = entriesByReference.get(
                        referenceKey(unit.id(), relation.referenceAstNodeId()));
                ResolutionContracts.ResolutionStatus status = binding == null
                        ? ResolutionContracts.ResolutionStatus.UNSUPPORTED : binding.status();
                ResolutionContracts.ResolutionReason reason = binding == null
                        ? ResolutionContracts.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM : binding.reason();
                List<ReferenceResolution.Candidate> candidates = binding == null ? List.of() : binding.candidates();
                result.add(new DeclarationRelationResolution.Entry(result.size(), unit.id(), relation.id(),
                        relation.kind(), relation.referenceAstNodeId(), status, reason, candidates));
            }
        }
        return new DeclarationRelationResolution(result);
    }

    private static String baseName(Ast.Node node, ReferenceOccurrences.Occurrence occurrence) {
        if (node instanceof Ast.DataReference reference) return reference.baseName();
        if (node instanceof Ast.IndexReference reference) return reference.indexName();
        return occurrence.writtenText();
    }

    private static boolean orderedSubsequence(List<String> qualifiers, List<String> ancestry) {
        int position = 0;
        for (String ancestor : ancestry)
            if (position < qualifiers.size() && qualifiers.get(position).equals(ancestor)) position++;
        return position == qualifiers.size();
    }

    private static void indexAst(Ast.Node node, Map<Integer, Ast.Node> output) {
        if (output.put(node.meta().id(), node) != null)
            throw new IllegalArgumentException("duplicate AST node id " + node.meta().id());
        for (Ast.Node child : Ast.children(node)) indexAst(child, output);
    }

    private static String referenceKey(ResolutionContracts.ProgramUnitId unitId, int astNodeId) {
        return unitId + "#" + astNodeId;
    }

    private static String diagnosticMessage(ReferenceOccurrences.Occurrence occurrence, Decision decision) {
        return decision.status() + " " + occurrence.kind() + " reference '" + occurrence.writtenText()
                + "': " + decision.reason();
    }
}
