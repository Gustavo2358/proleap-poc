package io.proleap.benchmark;

import java.util.*;

/** Completes nominal binding for PROCEDURE, FILE and PROGRAM over DATA/INDEX results. */
final class CobolReferenceResolver {
    private record UnitIndex(CompilationUnitModel.ProgramUnit unit, SymbolTable table,
                             Map<String, List<SymbolTable.Symbol>> procedures,
                             Map<String, List<SymbolTable.Symbol>> nominalNames,
                             Map<String, List<SymbolTable.Entity>> files,
                             Map<Integer, Ast.Node> astNodes) { }
    private record Decision(ResolutionContracts.ResolutionStatus status,
                            ResolutionContracts.ResolutionReason reason,
                            List<ReferenceResolution.Candidate> candidates) { }

    private final ResolutionContracts.CobolResolutionPolicy policy;
    private final Optional<ExternalProgramCatalog> externalCatalog;
    private final Map<ResolutionContracts.ProgramUnitId, UnitIndex> units = new LinkedHashMap<>();
    private final Map<String, List<CompilationUnitModel.ProgramUnit>> programsByName = new LinkedHashMap<>();
    private final Map<ResolutionContracts.ProgramUnitId, Integer> programLocalIds = new LinkedHashMap<>();
    private int additionalLookups;
    private long additionalInspections;
    private int additionalIndexed;
    private int additionalMaximum;

    CobolReferenceResolver(ResolutionContracts.CobolResolutionPolicy policy,
                           Optional<ExternalProgramCatalog> externalCatalog) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.externalCatalog = Objects.requireNonNull(externalCatalog, "externalCatalog");
    }

    ReferenceResolution resolve(CompilationUnitModel model, CompilationUnitSymbolTables symbolTables,
                                Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences) {
        ReferenceResolution data = new DataAndIndexReferenceResolver(policy)
                .resolve(model, symbolTables, occurrences);
        buildIndexes(model, symbolTables);
        List<ReferenceResolution.Entry> entries = new ArrayList<>();
        List<ReferenceResolution.Diagnostic> diagnostics = new ArrayList<>();
        for (ReferenceResolution.Entry base : data.entries()) {
            ReferenceOccurrences.Occurrence occurrence = base.occurrence();
            Decision decision = switch (occurrence.kind()) {
                case DATA, CONDITION, INDEX -> resolveDataAlternatives(base);
                case PROCEDURE -> resolveProcedure(occurrence);
                case FILE -> resolveFile(occurrence);
                case PROGRAM -> resolveProgram(occurrence);
                case PRESERVED_NAMED -> new Decision(ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                        ResolutionContracts.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM, List.of());
            };
            List<Integer> diagnosticIds = List.of();
            if (decision.status() != ResolutionContracts.ResolutionStatus.RESOLVED) {
                int id = diagnostics.size();
                diagnostics.add(new ReferenceResolution.Diagnostic(id,
                        "REFERENCE_" + decision.status() + "_" + decision.reason(),
                        decision.status() + " " + occurrence.kind() + " reference '"
                                + occurrence.writtenText() + "': " + decision.reason(),
                        occurrence.programUnitId(), occurrence.id()));
                diagnosticIds = List.of(id);
            }
            entries.add(new ReferenceResolution.Entry(entries.size(), occurrence, decision.status(),
                    decision.reason(), decision.candidates(), diagnosticIds));
        }
        ReferenceResolution.Metrics baseMetrics = data.metrics();
        return new ReferenceResolution(policy, entries, diagnostics,
                new ReferenceResolution.Metrics(baseMetrics.indexedDeclarations() + additionalIndexed,
                        baseMetrics.nominalLookups() + additionalLookups,
                        baseMetrics.candidateInspections() + additionalInspections,
                        Math.max(baseMetrics.maximumCandidates(), additionalMaximum)),
                data.declarationRelations());
    }

    private Decision resolveDataAlternatives(ReferenceResolution.Entry base) {
        if (!base.occurrence().admissibleKinds().contains(ResolutionContracts.ReferenceKind.FILE))
            return fromBase(base);
        Decision file = resolveFile(base.occurrence());
        List<ReferenceResolution.Candidate> combined = new ArrayList<>();
        combined.addAll(base.candidates());
        combined.addAll(file.candidates());
        if (combined.size() == 1)
            return new Decision(ResolutionContracts.ResolutionStatus.RESOLVED,
                    ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, combined);
        if (combined.size() > 1)
            return new Decision(ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                    ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, combined);
        return fromBase(base);
    }

    private void buildIndexes(CompilationUnitModel model, CompilationUnitSymbolTables symbolTables) {
        units.clear(); programsByName.clear(); programLocalIds.clear();
        additionalLookups = 0; additionalInspections = 0; additionalIndexed = 0; additionalMaximum = 0;
        int programIndex = 0;
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = symbolTables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            Map<String, List<SymbolTable.Symbol>> procedures = groupProcedures(table);
            Map<String, List<SymbolTable.Symbol>> nominalNames = groupSymbols(table);
            Map<String, List<SymbolTable.Entity>> files = groupFiles(table);
            Map<Integer, Ast.Node> astNodes = new HashMap<>();
            indexAst(unit.program(), astNodes);
            units.put(unit.id(), new UnitIndex(unit, table, procedures, nominalNames,
                    files, Map.copyOf(astNodes)));
            programsByName.computeIfAbsent(unit.id().canonicalProgramName(), ignored -> new ArrayList<>()).add(unit);
            programLocalIds.put(unit.id(), programIndex++);
            additionalIndexed += procedures.values().stream().mapToInt(List::size).sum()
                    + files.values().stream().mapToInt(List::size).sum() + 1;
        }
    }

    private Decision resolveProcedure(ReferenceOccurrences.Occurrence occurrence) {
        UnitIndex unit = units.get(occurrence.programUnitId());
        Ast.Node node = unit.astNodes().get(occurrence.referenceAstNodeId());
        String name = node instanceof Ast.ProcedureReference reference ? reference.baseName()
                : node instanceof Ast.ProcedureQualifier qualifier ? qualifier.sectionName()
                : occurrence.writtenText();
        String canonical = SymbolTable.canonical(name);
        additionalLookups++;
        List<SymbolTable.Symbol> candidates = unit.procedures().getOrDefault(canonical, List.of());
        additionalInspections += candidates.size();

        if (node instanceof Ast.ProcedureReference reference && reference.qualifier() != null) {
            String section = SymbolTable.canonical(reference.qualifier().sectionName());
            candidates = candidates.stream()
                    .filter(symbol -> symbol.kind() == SymbolTable.SymbolKind.PARAGRAPH)
                    .filter(symbol -> declaringSection(unit.table(), symbol)
                            .map(scope -> SymbolTable.canonical(scope.name()).equals(section)).orElse(false))
                    .toList();
        } else if (node instanceof Ast.ProcedureQualifier) {
            candidates = candidates.stream()
                    .filter(symbol -> symbol.kind() == SymbolTable.SymbolKind.PROCEDURE_SECTION).toList();
        } else if (candidates.size() > 1) {
            Optional<SymbolTable.Scope> currentSection = containingSection(unit.table(), occurrence.scopeId());
            if (currentSection.isPresent()) {
                int sectionScopeId = currentSection.get().id();
                List<SymbolTable.Symbol> contextual = candidates.stream()
                        .filter(symbol -> symbol.scopeId() == sectionScopeId).toList();
                if (!contextual.isEmpty()) candidates = contextual;
            }
        }
        List<ReferenceResolution.Candidate> mapped = candidates.stream()
                .map(symbol -> procedureCandidate(occurrence.programUnitId(), symbol)).toList();
        return nominalDecision(mapped, node instanceof Ast.ProcedureReference reference
                && reference.qualifier() != null);
    }

    private Decision resolveFile(ReferenceOccurrences.Occurrence occurrence) {
        UnitIndex startingUnit = units.get(occurrence.programUnitId());
        Ast.Node node = startingUnit.astNodes().get(occurrence.referenceAstNodeId());
        String name = node instanceof Ast.FileReference reference ? reference.baseName()
                : node instanceof Ast.DataReference reference ? reference.baseName()
                : occurrence.writtenText();
        String canonical = SymbolTable.canonical(name);
        ResolutionContracts.ProgramUnitId current = occurrence.programUnitId();
        while (current != null) {
            UnitIndex unit = units.get(current);
            additionalLookups++;
            List<SymbolTable.Symbol> nominal = unit.nominalNames().getOrDefault(canonical, List.of());
            List<SymbolTable.Entity> named = unit.files().getOrDefault(canonical, List.of());
            additionalInspections += nominal.size() + named.size();
            boolean localUnit = current.equals(occurrence.programUnitId());
            List<SymbolTable.Symbol> visibleNominal = localUnit ? nominal : nominal.stream()
                    .filter(CobolReferenceResolver::isGlobal).toList();
            if (!visibleNominal.isEmpty()) {
                ResolutionContracts.ProgramUnitId owner = current;
                List<SymbolTable.Entity> visibleFiles = localUnit ? named : named.stream()
                        .filter(entity -> "GLOBAL".equals(entity.attributes().get("visibility"))).toList();
                if (visibleFiles.isEmpty())
                    return new Decision(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                            ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, List.of());
                List<ReferenceResolution.Candidate> candidates = visibleFiles.stream()
                        .map(entity -> fileCandidate(owner, entity)).toList();
                return nominalDecision(candidates, false);
            }
            current = unit.unit().parentId();
        }
        return nominalDecision(List.of(), false);
    }

    private Decision resolveProgram(ReferenceOccurrences.Occurrence occurrence) {
        UnitIndex caller = units.get(occurrence.programUnitId());
        Ast.Node node = caller.astNodes().get(occurrence.referenceAstNodeId());
        if (!(node instanceof Ast.ProgramReference reference))
            return new Decision(ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                    ResolutionContracts.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM, List.of());
        String canonical = SymbolTable.canonical(reference.programName());
        additionalLookups++;
        List<CompilationUnitModel.ProgramUnit> named = programsByName.getOrDefault(canonical, List.of());
        additionalInspections += named.size();
        List<CompilationUnitModel.ProgramUnit> visible = named.stream()
                .filter(target -> visibleInternalProgram(caller.unit(), target)).toList();
        if (!visible.isEmpty()) {
            List<ReferenceResolution.Candidate> candidates = visible.stream().map(this::programCandidate).toList();
            return nominalDecision(candidates, false);
        }
        if (externalCatalog.isEmpty())
            return new Decision(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                    ResolutionContracts.ResolutionReason.EXTERNAL_CATALOG_NOT_PROVIDED, List.of());
        if (policy.pgmnameMode() == ResolutionContracts.PgmnameMode.UNSPECIFIED
                && dependsOnPgmname(reference.programName()))
            return new Decision(ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                    ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, List.of());
        String externalCanonical = ProgramNameCanonicalizer.external(
                reference.programName(), policy.pgmnameMode());
        List<ExternalProgramCatalog.Program> external = List.copyOf(
                externalCatalog.get().lookup(externalCanonical));
        additionalInspections += external.size();
        List<ReferenceResolution.Candidate> candidates = external.stream()
                .map(program -> externalCandidate(program, externalCanonical)).toList();
        return nominalDecision(candidates, false);
    }

    private static boolean dependsOnPgmname(String writtenName) {
        String uppercase = SymbolTable.canonical(writtenName);
        if (!writtenName.equals(uppercase) || uppercase.length() > 8) return true;
        return uppercase.chars().anyMatch(character ->
                !(character >= 'A' && character <= 'Z')
                        && !(character >= '0' && character <= '9'));
    }

    private boolean visibleInternalProgram(CompilationUnitModel.ProgramUnit caller,
                                           CompilationUnitModel.ProgramUnit target) {
        if (caller.id().equals(target.id())) return true;
        if (Objects.equals(target.parentId(), caller.id())) return true;
        if (isDescendantOf(caller.id(), target.id())) return false;
        ResolutionContracts.ProgramUnitId ancestorParent = caller.parentId();
        while (ancestorParent != null) {
            if (Objects.equals(target.parentId(), ancestorParent) && target.program().attributes().common())
                return true;
            ancestorParent = units.get(ancestorParent).unit().parentId();
        }
        return false;
    }

    private boolean isDescendantOf(ResolutionContracts.ProgramUnitId candidate,
                                   ResolutionContracts.ProgramUnitId ancestor) {
        ResolutionContracts.ProgramUnitId parent = units.get(candidate).unit().parentId();
        while (parent != null) {
            if (parent.equals(ancestor)) return true;
            parent = units.get(parent).unit().parentId();
        }
        return false;
    }

    private Decision nominalDecision(List<ReferenceResolution.Candidate> candidates, boolean qualified) {
        additionalMaximum = Math.max(additionalMaximum, candidates.size());
        if (candidates.isEmpty()) return new Decision(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, List.of());
        if (candidates.size() == 1) return new Decision(ResolutionContracts.ResolutionStatus.RESOLVED,
                qualified ? ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH
                        : ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, candidates);
        return new Decision(ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, candidates);
    }

    private ReferenceResolution.Candidate procedureCandidate(ResolutionContracts.ProgramUnitId unitId,
                                                              SymbolTable.Symbol symbol) {
        return new ReferenceResolution.Candidate(new ResolutionContracts.SemanticEntityId(unitId,
                ResolutionContracts.SemanticEntityDomain.PROCEDURE_SYMBOL, symbol.id()),
                ResolutionContracts.ReferenceKind.PROCEDURE, symbol.writtenName(), symbol.canonicalName(),
                List.of(symbol.id()), Map.of("symbolKind", symbol.kind().name()));
    }

    private ReferenceResolution.Candidate fileCandidate(ResolutionContracts.ProgramUnitId unitId,
                                                         SymbolTable.Entity entity) {
        return new ReferenceResolution.Candidate(new ResolutionContracts.SemanticEntityId(unitId,
                ResolutionContracts.SemanticEntityDomain.FILE_ENTITY, entity.id()),
                ResolutionContracts.ReferenceKind.FILE, entity.writtenName(), entity.canonicalName(),
                entity.declarationSymbolIds(), entity.attributes());
    }

    private ReferenceResolution.Candidate programCandidate(CompilationUnitModel.ProgramUnit unit) {
        return new ReferenceResolution.Candidate(new ResolutionContracts.SemanticEntityId(unit.id(),
                ResolutionContracts.SemanticEntityDomain.PROGRAM_UNIT, programLocalIds.get(unit.id())),
                ResolutionContracts.ReferenceKind.PROGRAM, unit.program().name(),
                unit.id().canonicalProgramName(), List.of(),
                Map.of("common", Boolean.toString(unit.program().attributes().common()), "source", "COMPILATION_UNIT"));
    }

    private static ReferenceResolution.Candidate externalCandidate(ExternalProgramCatalog.Program program,
                                                                    String canonicalName) {
        ResolutionContracts.ProgramUnitId catalogOwner = new ResolutionContracts.ProgramUnitId(
                "EXTERNAL-CATALOG:" + program.catalogId().toUpperCase(Locale.ROOT),
                List.of(program.id()), canonicalName);
        Map<String, String> attributes = new LinkedHashMap<>(program.attributes());
        attributes.put("catalogId", program.catalogId());
        attributes.put("source", "EXTERNAL_CATALOG");
        return new ReferenceResolution.Candidate(new ResolutionContracts.SemanticEntityId(catalogOwner,
                ResolutionContracts.SemanticEntityDomain.EXTERNAL_PROGRAM, program.id()),
                ResolutionContracts.ReferenceKind.PROGRAM, program.writtenName(), canonicalName,
                List.of(), attributes);
    }

    private static Decision fromBase(ReferenceResolution.Entry entry) {
        return new Decision(entry.status(), entry.reason(), entry.candidates());
    }

    private static Map<String, List<SymbolTable.Symbol>> groupProcedures(SymbolTable table) {
        LinkedHashMap<String, List<SymbolTable.Symbol>> mutable = new LinkedHashMap<>();
        for (SymbolTable.Symbol symbol : table.symbols()) {
            if (symbol.namespace() == SymbolTable.Namespace.PROCEDURE)
                mutable.computeIfAbsent(symbol.canonicalName(), ignored -> new ArrayList<>()).add(symbol);
        }
        return immutableLists(mutable);
    }

    private static Map<String, List<SymbolTable.Symbol>> groupSymbols(SymbolTable table) {
        LinkedHashMap<String, List<SymbolTable.Symbol>> mutable = new LinkedHashMap<>();
        for (SymbolTable.Symbol symbol : table.symbols())
            mutable.computeIfAbsent(symbol.canonicalName(), ignored -> new ArrayList<>()).add(symbol);
        return immutableLists(mutable);
    }

    private static boolean isGlobal(SymbolTable.Symbol symbol) {
        return "GLOBAL".equals(symbol.attributes().get("visibility"));
    }

    private static Map<String, List<SymbolTable.Entity>> groupFiles(SymbolTable table) {
        LinkedHashMap<String, List<SymbolTable.Entity>> mutable = new LinkedHashMap<>();
        for (SymbolTable.Entity entity : table.entities())
            if (entity.kind() == SymbolTable.EntityKind.FILE)
                mutable.computeIfAbsent(entity.canonicalName(), ignored -> new ArrayList<>()).add(entity);
        return immutableLists(mutable);
    }

    private static <T> Map<String, List<T>> immutableLists(Map<String, List<T>> source) {
        LinkedHashMap<String, List<T>> result = new LinkedHashMap<>();
        source.forEach((name, values) -> result.put(name, List.copyOf(values)));
        return Collections.unmodifiableMap(result);
    }

    private static Optional<SymbolTable.Scope> declaringSection(SymbolTable table, SymbolTable.Symbol symbol) {
        SymbolTable.Scope scope = table.scopes().get(symbol.scopeId());
        return scope.kind() == SymbolTable.ScopeKind.SECTION ? Optional.of(scope) : Optional.empty();
    }

    private static Optional<SymbolTable.Scope> containingSection(SymbolTable table, int startingScopeId) {
        int scopeId = startingScopeId;
        while (scopeId >= 0) {
            SymbolTable.Scope scope = table.scopes().get(scopeId);
            if (scope.kind() == SymbolTable.ScopeKind.SECTION) return Optional.of(scope);
            scopeId = scope.parentId();
        }
        return Optional.empty();
    }

    private static void indexAst(Ast.Node node, Map<Integer, Ast.Node> output) {
        if (output.put(node.meta().id(), node) != null)
            throw new IllegalArgumentException("duplicate AST node id " + node.meta().id());
        for (Ast.Node child : Ast.children(node)) indexAst(child, output);
    }
}
