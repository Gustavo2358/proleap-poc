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
    private record ExternalProgramId(String catalogId, int programId) {
        private ExternalProgramId(ExternalProgramCatalog.Program program) {
            this(program.catalogId().toUpperCase(Locale.ROOT), program.id());
        }
    }

    private final ResolutionContracts.CobolResolutionPolicy policy;
    private final Optional<ExternalProgramCatalog> externalCatalog;
    private final Map<ResolutionContracts.ProgramUnitId, UnitIndex> units = new LinkedHashMap<>();
    private final Map<String, List<CompilationUnitModel.ProgramUnit>> programsByName = new LinkedHashMap<>();
    private final Map<String, List<CompilationUnitModel.ProgramUnit>> longMixedProgramsByName =
            new LinkedHashMap<>();
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
                    decision.reason(), decision.candidates(), diagnosticIds,
                    callSemantics(occurrence, decision)));
        }
        ReferenceResolution.Metrics baseMetrics = data.metrics();
        return new ReferenceResolution(policy, entries, diagnostics,
                new ReferenceResolution.Metrics(baseMetrics.indexedDeclarations() + additionalIndexed,
                        baseMetrics.nominalLookups() + additionalLookups,
                        baseMetrics.candidateInspections() + additionalInspections,
                        Math.max(baseMetrics.maximumCandidates(), additionalMaximum)),
                data.declarationRelations());
    }

    private Optional<ReferenceResolution.CallSemantics> callSemantics(
            ReferenceOccurrences.Occurrence occurrence, Decision decision) {
        if (occurrence.role() != ResolutionContracts.ReferenceRole.CALL_TARGET)
            return Optional.empty();
        Ast.Node target = units.get(occurrence.programUnitId()).astNodes()
                .get(occurrence.referenceAstNodeId());
        Ast.CallTargetSyntax syntax = target instanceof Ast.ProgramReference
                ? Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME
                : Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION;
        ResolutionContracts.CallLinkage linkage;
        if (syntax == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION) {
            linkage = ResolutionContracts.CallLinkage.DYNAMIC;
        } else if (!decision.candidates().isEmpty() && decision.candidates().stream().allMatch(candidate ->
                candidate.entityId().domain() == ResolutionContracts.SemanticEntityDomain.PROGRAM_UNIT)) {
            linkage = ResolutionContracts.CallLinkage.STATIC;
        } else {
            linkage = switch (policy.dynamMode()) {
                case DYNAM -> policy.dllMode() == ResolutionContracts.DllMode.NODLL
                        ? ResolutionContracts.CallLinkage.DYNAMIC
                        : ResolutionContracts.CallLinkage.UNKNOWN;
                case NODYNAM -> switch (policy.dllMode()) {
                    case DLL -> ResolutionContracts.CallLinkage.DLL;
                    case NODLL -> ResolutionContracts.CallLinkage.STATIC;
                    case UNSPECIFIED -> ResolutionContracts.CallLinkage.UNKNOWN;
                };
                case UNSPECIFIED -> ResolutionContracts.CallLinkage.UNKNOWN;
            };
        }
        return Optional.of(new ReferenceResolution.CallSemantics(syntax, linkage));
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
        units.clear(); programsByName.clear(); longMixedProgramsByName.clear(); programLocalIds.clear();
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
            String programLookupKey = ProgramNameCanonicalizer.nested(
                    unit.program().name(), policy.pgmnameMode());
            programsByName.computeIfAbsent(programLookupKey, ignored -> new ArrayList<>()).add(unit);
            String longMixedLookupKey = ProgramNameCanonicalizer.nested(
                    unit.program().name(), ResolutionContracts.PgmnameMode.LONGMIXED);
            longMixedProgramsByName.computeIfAbsent(
                    longMixedLookupKey, ignored -> new ArrayList<>()).add(unit);
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
        List<CompilationUnitModel.ProgramUnit> visible;
        if (policy.pgmnameMode() == ResolutionContracts.PgmnameMode.UNSPECIFIED) {
            String foldedKey = ProgramNameCanonicalizer.nested(
                    reference.programName(), ResolutionContracts.PgmnameMode.LONGUPPER);
            String mixedKey = ProgramNameCanonicalizer.nested(
                    reference.programName(), ResolutionContracts.PgmnameMode.LONGMIXED);
            additionalLookups += 2;
            List<CompilationUnitModel.ProgramUnit> folded = visiblePrograms(
                    caller.unit(), programsByName.getOrDefault(foldedKey, List.of()));
            List<CompilationUnitModel.ProgramUnit> mixed = visiblePrograms(
                    caller.unit(), longMixedProgramsByName.getOrDefault(mixedKey, List.of()));
            additionalInspections += folded.size() + mixed.size();
            if (!programIds(folded).equals(programIds(mixed))) {
                LinkedHashMap<ResolutionContracts.ProgramUnitId, CompilationUnitModel.ProgramUnit> possible =
                        new LinkedHashMap<>();
                folded.forEach(unit -> possible.put(unit.id(), unit));
                mixed.forEach(unit -> possible.put(unit.id(), unit));
                List<ReferenceResolution.Candidate> candidates = possible.values().stream()
                        .map(this::programCandidate).toList();
                return new Decision(ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                        ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, candidates);
            }
            visible = folded;
        } else {
            String canonical = ProgramNameCanonicalizer.nested(
                    reference.programName(), policy.pgmnameMode());
            additionalLookups++;
            List<CompilationUnitModel.ProgramUnit> named = programsByName.getOrDefault(canonical, List.of());
            additionalInspections += named.size();
            visible = visiblePrograms(caller.unit(), named);
        }
        if (!visible.isEmpty()) {
            List<ReferenceResolution.Candidate> candidates = visible.stream().map(this::programCandidate).toList();
            return nominalDecision(candidates, false);
        }
        if (invalidExternalCallOptions())
            return new Decision(ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                    ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, List.of());
        if (externalCatalog.isEmpty())
            return new Decision(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                    ResolutionContracts.ResolutionReason.EXTERNAL_CATALOG_NOT_PROVIDED, List.of());
        return resolveExternalProgram(reference.programName());
    }

    private boolean invalidExternalCallOptions() {
        return policy.dynamMode() == ResolutionContracts.DynamMode.DYNAM
                && policy.dllMode() == ResolutionContracts.DllMode.DLL;
    }

    private Decision resolveExternalProgram(String writtenName) {
        LinkedHashSet<String> keys = possibleExternalKeys(writtenName);
        List<Set<ExternalProgramId>> outcomes = new ArrayList<>();
        LinkedHashMap<ExternalProgramId, ReferenceResolution.Candidate> possible = new LinkedHashMap<>();
        for (String key : keys) {
            additionalLookups++;
            List<ExternalProgramCatalog.Program> programs = lookupExternal(key);
            additionalInspections += programs.size();
            LinkedHashSet<ExternalProgramId> ids = programs.stream().map(ExternalProgramId::new)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            outcomes.add(Set.copyOf(ids));
            for (ExternalProgramCatalog.Program program : programs)
                possible.putIfAbsent(new ExternalProgramId(program), externalCandidate(program, key));
        }
        if (outcomes.stream().distinct().count() > 1)
            return new Decision(ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                    ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION,
                    List.copyOf(possible.values()));
        return nominalDecision(List.copyOf(possible.values()), false);
    }

    private LinkedHashSet<String> possibleExternalKeys(String writtenName) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (policy.dynamMode() != ResolutionContracts.DynamMode.NODYNAM)
            keys.add(ProgramNameCanonicalizer.dynamicExternal(writtenName));
        if (policy.dynamMode() != ResolutionContracts.DynamMode.DYNAM) {
            if (policy.pgmnameMode() == ResolutionContracts.PgmnameMode.UNSPECIFIED) {
                EnumSet.of(ResolutionContracts.PgmnameMode.COMPAT,
                                ResolutionContracts.PgmnameMode.LONGUPPER,
                                ResolutionContracts.PgmnameMode.LONGMIXED).stream()
                        .map(mode -> ProgramNameCanonicalizer.external(writtenName, mode))
                        .forEach(keys::add);
            } else {
                keys.add(ProgramNameCanonicalizer.external(writtenName, policy.pgmnameMode()));
            }
        }
        return keys;
    }

    private List<ExternalProgramCatalog.Program> lookupExternal(String canonicalName) {
        return List.copyOf(externalCatalog.orElseThrow().lookup(canonicalName));
    }

    private List<CompilationUnitModel.ProgramUnit> visiblePrograms(
            CompilationUnitModel.ProgramUnit caller, List<CompilationUnitModel.ProgramUnit> named) {
        return named.stream().filter(target -> visibleInternalProgram(caller, target)).toList();
    }

    private static Set<ResolutionContracts.ProgramUnitId> programIds(
            List<CompilationUnitModel.ProgramUnit> programs) {
        return programs.stream().map(CompilationUnitModel.ProgramUnit::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
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
                ProgramNameCanonicalizer.nested(unit.program().name(), policy.pgmnameMode()), List.of(),
                Map.of("common", Boolean.toString(unit.program().attributes().common()), "source", "COMPILATION_UNIT"));
    }

    private static ReferenceResolution.Candidate externalCandidate(ExternalProgramCatalog.Program program,
                                                                    String canonicalName) {
        ResolutionContracts.ProgramUnitId catalogOwner = new ResolutionContracts.ProgramUnitId(
                "EXTERNAL-CATALOG:" + program.catalogId().toUpperCase(Locale.ROOT),
                List.of(program.id()), SymbolTable.canonical(program.writtenName()));
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
