package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Source-derived proof for a single isolated paragraph invocation, not general PERFORM. */
public final class PerformSemantics {
    public record Target(ResolutionContracts.SemanticEntityId identity, int paragraph,
                         List<Integer> statements, Ast.SourceProvenance referenceOrigin,
                         Ast.SourceProvenance paragraphOrigin) {
        public Target { statements = List.copyOf(statements); }
    }
    public record Facts(Optional<Target> target, Optional<Integer> resume,
                        Ast.SourceProvenance resumeOrigin, List<Integer> primaryStatements,
                        List<String> gaps) {
        public Facts { primaryStatements = List.copyOf(primaryStatements); gaps = List.copyOf(gaps); }
        public boolean simpleProfile() { return gaps.isEmpty(); }
    }
    private final Map<ScalarMoveSemantics.NodeKey, Facts> facts;
    private PerformSemantics(Map<ScalarMoveSemantics.NodeKey, Facts> facts) { this.facts = Map.copyOf(facts); }
    public Facts fact(ResolutionContracts.ProgramUnitId unit, int statement) {
        return Objects.requireNonNull(facts.get(new ScalarMoveSemantics.NodeKey(unit, statement)), "PERFORM fact missing");
    }
    /** Only a proved isolated activation may give its final MOVE this return relation. */
    void applyCompletions(Map<ScalarMoveSemantics.NodeKey, ScalarMoveSemantics.Move> moves) {
        facts.forEach((key, fact) -> {
            if (!fact.simpleProfile()) return;
            var body = fact.target().orElseThrow().statements();
            var last = new ScalarMoveSemantics.NodeKey(key.unit(), body.get(body.size() - 1));
            var move = Objects.requireNonNull(moves.get(last));
            moves.put(last, new ScalarMoveSemantics.Move(move.wholeItem(), move.copy(), fact.resume(),
                move.gaps().stream().filter(g -> g != ScalarMoveSemantics.Gap.NORMAL_CONTINUATION_NOT_AVAILABLE).toList(),
                move.adjustment(), move.sourceWholeItem()));
        });
    }
    static PerformSemantics analyze(CompilationUnitBuildResult frontend, CompilationUnitSymbolTables tables,
            ReferenceResolution resolution, ResolutionAnalysisReport report,
            Map<ScalarMoveSemantics.NodeKey, ScalarMoveSemantics.Move> moves, IfSemantics ifs) {
        boolean complete = report.gaps().stream().noneMatch(g -> g.category() == ResolutionAnalysisReport.GapCategory.INPUT);
        var references = new HashMap<ScalarMoveSemantics.NodeKey, ReferenceResolution.Entry>();
        for (var entry : resolution.entries()) references.put(new ScalarMoveSemantics.NodeKey(
            entry.occurrence().programUnitId(), entry.occurrence().referenceAstNodeId()), entry);
        var result = new HashMap<ScalarMoveSemantics.NodeKey, Facts>();
        for (var unit : frontend.compilationUnit().programUnits()) {
            var nodes = new HashMap<Integer, Ast.Node>(); var performs = new ArrayList<Ast.PerformStatement>();
            var pending = new ArrayDeque<Ast.Node>(); pending.push(unit.program());
            while (!pending.isEmpty()) {
                var node = pending.pop(); nodes.put(node.meta().id(), node);
                if (node instanceof Ast.PerformStatement p) performs.add(p);
                for (var child : Ast.children(node)) pending.push(child);
            }
            var symbols = new HashMap<Integer, SymbolTable.Symbol>();
            for (var symbol : tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols()) symbols.put(symbol.id(), symbol);
            var findings = new HashMap<Integer, SemanticCoverage.Finding>();
            for (var finding : frontend.coverageByProgramUnit().get(unit.id()).findings()) findings.put(finding.astNodeId(), finding);
            var procedures = unit.program().divisions().stream().filter(d -> d.divisionKind() == Ast.DivisionKind.PROCEDURE).toList();
            for (var perform : performs) {
                var gaps = new LinkedHashSet<String>();
                if (performs.size() != 1) {
                    result.put(new ScalarMoveSemantics.NodeKey(unit.id(), perform.meta().id()), new Facts(
                        Optional.empty(), Optional.empty(), perform.meta().provenance(), List.of(), List.of("PERFORM_MULTIPLE_CALLSITES")));
                    continue;
                }
                if (!complete) gaps.add("PERFORM_INPUT_INCOMPLETE");
                if (perform.performKind() != Ast.PerformKind.PROCEDURE || perform.fromReference() == null
                        || perform.throughReference() != null || !perform.controls().isEmpty()
                        || !perform.controlExpressions().isEmpty() || !perform.inlineBody().isEmpty())
                    gaps.add("PERFORM_FORM_OUTSIDE_PROFILE");
                if (performs.size() != 1) gaps.add("PERFORM_MULTIPLE_CALLSITES");
                Ast.Paragraph target = null; ResolutionContracts.SemanticEntityId identity = null;
                var ref = perform.fromReference() == null ? null : references.get(new ScalarMoveSemantics.NodeKey(unit.id(), perform.fromReference().meta().id()));
                if (ref != null && ref.occurrence().role() == ResolutionContracts.ReferenceRole.PERFORM_FROM
                        && ref.status() == ResolutionContracts.ResolutionStatus.RESOLVED
                        && ref.occurrence().meta().provenance().exact()) {
                    var candidate = ref.selectedCandidate().orElseThrow();
                    var symbol = symbols.get(candidate.entityId().localId());
                    if (candidate.entityId().programUnitId().equals(unit.id())
                            && candidate.entityId().domain() == ResolutionContracts.SemanticEntityDomain.PROCEDURE_SYMBOL
                            && symbol != null && symbol.namespace() == SymbolTable.Namespace.PROCEDURE
                            && symbol.kind() == SymbolTable.SymbolKind.PARAGRAPH
                            && nodes.get(symbol.declarationAstNodeId()) instanceof Ast.Paragraph paragraph) {
                        target = paragraph; identity = candidate.entityId();
                    }
                }
                if (target == null) gaps.add("PERFORM_TARGET_NOT_UNIQUE_LOCAL_PARAGRAPH");
                Ast.Paragraph primary = null; Ast.Division procedure = procedures.size() == 1 ? procedures.get(0) : null;
                if (procedure != null && procedure.procedureEntry().isPresent()) {
                    var entry = procedure.procedureEntry().orElseThrow();
                    if (!entry.declarativesPresent() && !entry.signatureClausesPresent() && entry.startStatementId().isPresent()
                            && procedure.children().size() == 2 && procedure.children().stream().allMatch(Ast.Paragraph.class::isInstance)) {
                        for (var child : procedure.children()) {
                            var paragraph = (Ast.Paragraph) child;
                            var body = direct(paragraph);
                            if (!body.isEmpty() && body.get(0).meta().id() == entry.startStatementId().orElseThrow()) primary = paragraph;
                        }
                    }
                }
                var main = primary == null ? List.<Ast.Statement>of() : direct(primary);
                var body = target == null ? List.<Ast.Statement>of() : direct(target);
                boolean isolated = primary != null && target != null && primary != target && procedure.children().contains(target);
                int index = main.indexOf(perform);
                // SP1.7: direct supported primary statements with one constant PERFORM resume.
                isolated &= index >= 0 && index + 1 < main.size()
                    && main.get(main.size() - 1) instanceof Ast.GobackStatement;
                if (isolated) {
                    for (int i = 0; i < main.size(); i++) {
                        var statement = main.get(i);
                        isolated &= modeled(statement, findings);
                        if (i == main.size() - 1) continue;
                        if (statement == perform) continue;
                        boolean supported = supportedMove(statement, unit.id(), moves)
                            || statement instanceof Ast.CallStatement call && !call.surface().hasHandlers()
                            || statement instanceof Ast.IfStatement branch && ifs.fact(unit.id(), branch.meta().id()).simpleProfile();
                        isolated &= supported && Objects.equals(procedure.normalContinuations().get(statement.meta().id()), main.get(i + 1).meta().id());
                    }
                }
                if (!isolated) gaps.add("PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN");
                boolean linear = !body.isEmpty();
                for (int i = 0; i < body.size(); i++) {
                    var statement = body.get(i);
                    linear &= supportedMove(statement, unit.id(), moves) && modeled(statement, findings);
                    if (i + 1 < body.size()) linear &= procedure != null && Objects.equals(
                        procedure.normalContinuations().get(statement.meta().id()), body.get(i + 1).meta().id());
                }
                if (!linear) gaps.add("PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN");
                boolean exact = perform.meta().provenance().exact() && primary != null && primary.meta().provenance().exact()
                    && target != null && target.meta().provenance().exact();
                if (!exact) gaps.add("PERFORM_PROVENANCE_INCOMPLETE");
                // Refused forms publish no control guarantees. Their observed inventory remains intact.
                if (gaps.isEmpty()) {
                    var resume = main.get(index + 1);
                    result.put(new ScalarMoveSemantics.NodeKey(unit.id(), perform.meta().id()), new Facts(
                        Optional.of(new Target(identity, target.meta().id(), body.stream().map(s -> s.meta().id()).toList(),
                            perform.fromReference().meta().provenance(), target.meta().provenance())),
                        Optional.of(resume.meta().id()), resume.meta().provenance(), main.stream().map(s -> s.meta().id()).toList(), List.of()));
                } else result.put(new ScalarMoveSemantics.NodeKey(unit.id(), perform.meta().id()), new Facts(
                    Optional.empty(), Optional.empty(), perform.meta().provenance(), List.of(), List.copyOf(gaps)));
            }
        }
        return new PerformSemantics(result);
    }
    private static List<Ast.Statement> direct(Ast.Paragraph paragraph) {
        return paragraph.sentences().stream().flatMap(s -> s.statements().stream()).toList();
    }
    private static boolean supportedMove(Ast.Statement statement, ResolutionContracts.ProgramUnitId unit,
            Map<ScalarMoveSemantics.NodeKey, ScalarMoveSemantics.Move> moves) {
        var move = moves.get(new ScalarMoveSemantics.NodeKey(unit, statement.meta().id()));
        return statement instanceof Ast.MoveStatement && move != null && move.copy() != ScalarMoveSemantics.Copy.UNAVAILABLE;
    }
    private static boolean modeled(Ast.Statement statement, Map<Integer, SemanticCoverage.Finding> findings) {
        var finding = findings.get(statement.meta().id());
        return finding != null && finding.coverage() == SemanticCoverage.ConstructionCoverage.MODELED && statement.meta().provenance().exact();
    }
}
