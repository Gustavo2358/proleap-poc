package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Source-derived proofs for isolated BASIC paragraph activations, not general PERFORM. */
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
    private final Set<ScalarMoveSemantics.NodeKey> intrinsicExits;
    private PerformSemantics(Map<ScalarMoveSemantics.NodeKey, Facts> facts) {
        this.facts = Map.copyOf(facts);
        var exits=new HashSet<ScalarMoveSemantics.NodeKey>();
        facts.forEach((key,value) -> value.target().ifPresent(t -> exits.add(new ScalarMoveSemantics.NodeKey(key.unit(),t.statements().get(t.statements().size()-1)))));
        intrinsicExits=Set.copyOf(exits);
    }
    public boolean intrinsicExit(ResolutionContracts.ProgramUnitId unit,int node) { return intrinsicExits.contains(new ScalarMoveSemantics.NodeKey(unit,node)); }
    public Facts fact(ResolutionContracts.ProgramUnitId unit, int statement) {
        return Objects.requireNonNull(facts.get(new ScalarMoveSemantics.NodeKey(unit, statement)), "PERFORM fact missing");
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
            var procedure=procedures.size()==1?procedures.get(0):null;
            var bodies=new IdentityHashMap<Ast.Paragraph,List<Ast.Statement>>();
            Ast.Paragraph primary=null;
            if(procedure!=null && procedure.procedureEntry().isPresent()) {
                var entry=procedure.procedureEntry().orElseThrow();
                if(!entry.declarativesPresent() && !entry.signatureClausesPresent() && entry.startStatementId().isPresent()
                    && procedure.children().stream().allMatch(Ast.Paragraph.class::isInstance)) {
                    for(var child:procedure.children()) {
                        var paragraph=(Ast.Paragraph)child;var body=direct(paragraph);bodies.put(paragraph,body);
                        if(!body.isEmpty() && body.get(0).meta().id()==entry.startStatementId().orElseThrow())primary=paragraph;
                    }
                }
            }
            var main=primary==null?List.<Ast.Statement>of():bodies.get(primary);
            var positions=new IdentityHashMap<Ast.Statement,Integer>();
            boolean mainSound=!main.isEmpty() && main.get(main.size()-1) instanceof Ast.GobackStatement;
            for(int i=0;i<main.size();i++) {
                var statement=main.get(i);positions.put(statement,i);mainSound &= modeled(statement,findings);
                if(i==main.size()-1)continue;
                boolean supported=supportedMove(statement,unit.id(),moves)
                    || statement instanceof Ast.CallStatement call && !call.surface().hasHandlers()
                    || statement instanceof Ast.PerformStatement other && basic(other)
                    || statement instanceof Ast.IfStatement branch && ifs.fact(unit.id(),branch.meta().id()).simpleProfile()
                    || statement instanceof Ast.EvaluateStatement e && evaluatePrimary(e, unit.id(), moves, ifs, findings, procedure.normalContinuations(), positions);
                mainSound &= supported && Objects.equals(procedure.normalContinuations().get(statement.meta().id()),main.get(i+1).meta().id());
            }
            var primaryIds=main.stream().map(statement->statement.meta().id()).toList();
            var linearBodies=new IdentityHashMap<Ast.Paragraph,Boolean>();
            for (var perform : performs) {
                var gaps = new LinkedHashSet<String>();
                if (!complete) gaps.add("PERFORM_INPUT_INCOMPLETE");
                if (perform.performKind() != Ast.PerformKind.PROCEDURE || perform.fromReference() == null
                        || perform.throughReference() != null || !perform.controls().isEmpty()
                        || !perform.controlExpressions().isEmpty() || !perform.inlineBody().isEmpty())
                    gaps.add("PERFORM_FORM_OUTSIDE_PROFILE");
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
                var body=target==null?List.<Ast.Statement>of():bodies.getOrDefault(target,List.of());
                var resumeId=procedure==null?null:procedure.normalContinuations().get(perform.meta().id());
                boolean isolated=mainSound && target!=null && target!=primary && bodies.containsKey(target)
                    && positions.containsKey(perform) && resumeId!=null;
                if (!isolated) gaps.add("PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN");
                Boolean linear=linearBodies.get(target);
                if(linear==null) {
                    linear=!body.isEmpty();
                    for(int i=0;i<body.size();i++) {
                        var statement=body.get(i);
                        linear &= supportedMove(statement,unit.id(),moves) && modeled(statement,findings);
                        if(i+1<body.size())linear &= procedure!=null && Objects.equals(procedure.normalContinuations().get(statement.meta().id()),body.get(i+1).meta().id());
                    }
                    linearBodies.put(target,linear);
                }
                if (!linear) gaps.add("PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN");
                boolean exact = perform.meta().provenance().exact() && primary != null && primary.meta().provenance().exact()
                    && target != null && target.meta().provenance().exact();
                if (!exact) gaps.add("PERFORM_PROVENANCE_INCOMPLETE");
                // Refused forms publish no control guarantees. Their observed inventory remains intact.
                if (gaps.isEmpty()) {
                    var resume = nodes.get(resumeId);
                    result.put(new ScalarMoveSemantics.NodeKey(unit.id(), perform.meta().id()), new Facts(
                        Optional.of(new Target(identity, target.meta().id(), body.stream().map(s -> s.meta().id()).toList(),
                            perform.fromReference().meta().provenance(), target.meta().provenance())),
                        Optional.of(resume.meta().id()), resume.meta().provenance(), primaryIds, List.of()));
                } else result.put(new ScalarMoveSemantics.NodeKey(unit.id(), perform.meta().id()), new Facts(
                    Optional.empty(), Optional.empty(), perform.meta().provenance(), List.of(), List.copyOf(gaps)));
            }
        }
        return new PerformSemantics(result);
    }
    /** BASIC activations may be in a bounded EVALUATE arm; each region has its own proved completion. */
    private static boolean evaluatePrimary(Ast.EvaluateStatement evaluate, ResolutionContracts.ProgramUnitId unit,
            Map<ScalarMoveSemantics.NodeKey, ScalarMoveSemantics.Move> moves, IfSemantics ifs,
            Map<Integer, SemanticCoverage.Finding> findings, Map<Integer,Integer> next,
            Map<Ast.Statement,Integer> primaryMembers) {
        if (!EvaluateSemantics.supportedShape(evaluate)) return false;
        record Region(List<Ast.Statement> body, Integer resume) { }
        var pending=new ArrayDeque<Region>();
        for (var arm:evaluate.branches()) pending.push(new Region(arm.statements(),next.get(evaluate.meta().id())));
        boolean sound=true;
        while(!pending.isEmpty()) {
            var region=pending.pop(); sound &= !region.body().isEmpty() && region.resume()!=null;
            for(int i=0;i<region.body().size();i++) {
                var s=region.body().get(i); primaryMembers.put(s,0); sound &= modeled(s,findings);
                var resume=i+1<region.body().size()?region.body().get(i+1).meta().id():region.resume();
                if(s instanceof Ast.GobackStatement) continue;
                sound &= Objects.equals(next.get(s.meta().id()),resume);
                if(s instanceof Ast.IfStatement f) {
                    sound &= f.explicitlyTerminated() && ifs.fact(unit,f.meta().id()).predicate().availability()==IfSemantics.Availability.KNOWN;
                    pending.push(new Region(f.thenBranch(),resume));
                    if(f.elsePresence()==Ast.BranchPresence.PRESENT)pending.push(new Region(f.elseBranch(),resume));
                    else sound &= f.elsePresence()==Ast.BranchPresence.ABSENT;
                } else sound &= supportedMove(s,unit,moves) || s instanceof Ast.CallStatement call && !call.surface().hasHandlers()
                        || s instanceof Ast.PerformStatement p && basic(p);
            }
        }
        return sound;
    }

    private static boolean basic(Ast.PerformStatement p) {
        return p.performKind()==Ast.PerformKind.PROCEDURE && p.fromReference()!=null && p.throughReference()==null
            && p.controls().isEmpty() && p.controlExpressions().isEmpty() && p.inlineBody().isEmpty();
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
