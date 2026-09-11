package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Canonical, immutable post-binding facts for the elementary textual MOVE profile.
 * This product has no boundary, AIR, CFG or runtime-value dependencies. */
public final class ScalarMoveSemantics {
    public record NodeKey(ResolutionContracts.ProgramUnitId unit, int node) { }
    /** Local standalone elementary DISPLAY item in ordinary WORKING-STORAGE. */
    public record ScalarText(int extent) {
        public ScalarText { if (extent <= 0) throw new IllegalArgumentException("positive extent required"); }
    }
    public enum Copy { FULL_IDENTITY, FITTED_TEXT, UNAVAILABLE }
    public record TextAdjustment(int receiverExtent, String result) { }
    public record Call(Optional<ResolutionContracts.SemanticEntityId> wholeItem,
                       Optional<Integer> nextStatement, boolean inputComplete) { }
    public enum Gap { SCALAR_WHOLE_ITEM_NOT_PROVEN, MOVE_IDENTITY_NOT_PROVEN, NORMAL_CONTINUATION_NOT_AVAILABLE }
    public record Move(Optional<ResolutionContracts.SemanticEntityId> wholeItem,
                       Copy copy, Optional<Integer> nextStatement, List<Gap> gaps, Optional<TextAdjustment> adjustment) {
        public Move {
            wholeItem = Objects.requireNonNull(wholeItem);
            copy = Objects.requireNonNull(copy);
            nextStatement = Objects.requireNonNull(nextStatement);
            gaps = List.copyOf(gaps);
            if (copy == Copy.FULL_IDENTITY && wholeItem.isEmpty())
                throw new IllegalArgumentException("identity copy requires whole item proof");
        }
    }
    public record Metrics(long nodeVisits, long declarationVisits, long referenceVisits,
                          long scalarLookups, long moveVisits) { }
    private final Map<ResolutionContracts.SemanticEntityId, ScalarText> declarations;
    private final Map<NodeKey, Move> moves;
    private final Metrics metrics;
    private final Map<NodeKey, Call> calls;
    public Call call(ResolutionContracts.ProgramUnitId unit, int node) {
        return calls.getOrDefault(new NodeKey(unit, node), new Call(Optional.empty(), Optional.empty(), false));
    }

    private ScalarMoveSemantics(Map<ResolutionContracts.SemanticEntityId, ScalarText> declarations,
                                Map<NodeKey, Move> moves, Map<NodeKey, Call> calls, Metrics metrics) {
        this.declarations = Map.copyOf(declarations);
        this.moves = Map.copyOf(moves);
        this.metrics = metrics;
        this.calls = Map.copyOf(calls);
    }
    public Optional<ScalarText> declaration(ResolutionContracts.SemanticEntityId id) {
        return Optional.ofNullable(declarations.get(id));
    }
    public Move move(ResolutionContracts.ProgramUnitId unit, int node) {
        Move result = moves.get(new NodeKey(unit, node));
        return result != null ? result : fact(Optional.empty(), Copy.UNAVAILABLE, Optional.empty());
    }
    public Metrics metrics() { return metrics; }

    /** One AST walk, one symbol pass and one resolution pass; no MOVE scans DATA.
     * The transient target index holds occurrences, never copied declarations. */
    public static ScalarMoveSemantics analyze(CompilationUnitBuildResult frontend,
            CompilationUnitSymbolTables tables, ReferenceResolution resolution,
            ResolutionAnalysisReport report) {
        Map<ResolutionContracts.SemanticEntityId, ScalarText> declarations = new HashMap<>();
        Map<NodeKey, Ast.MoveStatement> targets = new HashMap<>();
        Map<NodeKey, Move> moves = new HashMap<>();
        Map<NodeKey, Ast.CallStatement> callTargets = new HashMap<>();
        Map<NodeKey, Call> calls = new HashMap<>();
        long[] counts = new long[5];
        boolean inputComplete = report.gaps().stream()
                .noneMatch(g -> g.category() == ResolutionAnalysisReport.GapCategory.INPUT);
        for (var unit : frontend.compilationUnit().programUnits()) {
            Map<Integer, ScalarText> eligible = new HashMap<>();
            // Reuse the already immutable canonical relation index; do not rebuild it.
            Map<Integer, Integer> next = Map.of();
            boolean procedureSeen = false;
            for (var division : unit.program().divisions()) {
                if (division.divisionKind() != Ast.DivisionKind.PROCEDURE) continue;
                if (procedureSeen) throw new IllegalArgumentException("duplicate procedure division");
                procedureSeen = true;
                next = division.normalContinuations();
            }
            var attributes = unit.program().attributes();
            boolean ordinary = inputComplete && !attributes.initial() && !attributes.recursive()
                    && !attributes.common() && !attributes.library() && !attributes.definition();
            Deque<Ast.Node> pending = new ArrayDeque<>();
            pending.push(unit.program());
            while (!pending.isEmpty()) {
                Ast.Node node = pending.pop();
                counts[0]++;
                if (node instanceof Ast.Section section
                        && section.dataSectionKind() == Ast.DataSectionKind.WORKING_STORAGE && ordinary) {
                    // Any overlay in this section denies the profile, including aliases
                    // declared after a candidate. This is conservative, not alias analysis.
                    boolean overlay = hasOverlay(section, counts);
                    if (!overlay) for (Ast.Node child : section.children()) {
                        if (child instanceof Ast.DataEntry entry) scalar(entry, counts)
                                .ifPresent(shape -> eligible.put(entry.meta().id(), shape));
                    }
                }
                if (node instanceof Ast.CallStatement call) {
                    var key = new NodeKey(unit.id(), call.meta().id());
                    calls.put(key, new Call(Optional.empty(), inputComplete
                            ? Optional.ofNullable(next.get(call.meta().id())) : Optional.empty(), inputComplete));
                    if (call.target() instanceof Ast.DataReference target)
                        callTargets.put(new NodeKey(unit.id(), target.meta().id()), call);
                }
                if (node instanceof Ast.MoveStatement move) {
                    counts[4]++;
                    var key = new NodeKey(unit.id(), move.meta().id());
                    moves.put(key, fact(Optional.empty(), Copy.UNAVAILABLE,
                            inputComplete ? Optional.ofNullable(next.get(move.meta().id())) : Optional.empty()));
                    if (!move.corresponding() && move.source() instanceof Ast.LiteralExpression
                            && move.targets().size() == 1 && move.targets().get(0) instanceof Ast.DataReference target)
                        targets.put(new NodeKey(unit.id(), target.meta().id()), move);
                }
                for (var child : Ast.children(node)) pending.push(child);
            }
            for (var symbol : tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols()) {
                counts[1]++;
                ScalarText shape = eligible.get(symbol.declarationAstNodeId());
                if (shape != null && symbol.namespace() == SymbolTable.Namespace.DATA
                        && symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM)
                    declarations.put(new ResolutionContracts.SemanticEntityId(unit.id(),
                            ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, symbol.id()), shape);
            }
        }
        for (var entry : resolution.entries()) {
            counts[2]++;
            var occurrence = entry.occurrence();
            var occurrenceKey = new NodeKey(occurrence.programUnitId(), occurrence.referenceAstNodeId());
            var call = callTargets.get(occurrenceKey);
            if (call != null) {
                var target = (Ast.DataReference) call.target();
                Optional<ResolutionContracts.SemanticEntityId> whole = Optional.empty();
                if (inputComplete && entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED
                        && occurrence.role() == ResolutionContracts.ReferenceRole.CALL_TARGET
                        && target.understanding() == Ast.ReferenceUnderstanding.STRUCTURED
                        && target.subscriptGroups().isEmpty() && target.referenceModification() == null
                        && target.qualifiers().isEmpty()) {
                    var selected = entry.selectedCandidate().orElseThrow();
                    counts[3]++;
                    if (declarations.containsKey(selected.entityId())) whole = Optional.of(selected.entityId());
                }
                var key = new NodeKey(occurrence.programUnitId(), call.meta().id());
                calls.put(key, new Call(whole, calls.get(key).nextStatement(), inputComplete));
            }
            var move = targets.get(occurrenceKey);
            if (move == null) continue;
            var target = (Ast.DataReference) move.targets().get(0);
            Optional<ResolutionContracts.SemanticEntityId> whole = Optional.empty();
            Copy copy = Copy.UNAVAILABLE;
            Optional<TextAdjustment> adjustment = Optional.empty();
            if (inputComplete && entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED
                    && occurrence.role() == ResolutionContracts.ReferenceRole.VALUE_WRITE
                    && target.understanding() == Ast.ReferenceUnderstanding.STRUCTURED
                    && target.subscriptGroups().isEmpty() && target.referenceModification() == null
                    && target.qualifiers().isEmpty()) {
                var selected = entry.selectedCandidate().orElseThrow();
                counts[3]++;
                ScalarText shape = declarations.get(selected.entityId());
                if (shape != null) {
                    whole = Optional.of(selected.entityId());
                    var literal = (Ast.LiteralExpression) move.source();
                    // IBM elementary alphanumeric MOVE with equal logical lengths:
                    // mandatory complete receiving-item write, no conversion or fitting.
                    if (literal.logicalText().isPresent()
                            && literal.logicalText().get().extent() == shape.extent()) copy = Copy.FULL_IDENTITY;
                    else if (literal.logicalText().isPresent()
                            && literal.logicalText().get().extent() < shape.extent()) {
                        // IBM 6.4 elementary alphanumeric MOVE, non-JUSTIFIED DISPLAY receiver:
                        // left alignment fills the remaining logical positions with spaces.
                        var text = literal.logicalText().get();
                        copy = Copy.FITTED_TEXT;
                        adjustment = Optional.of(new TextAdjustment(shape.extent(),
                                text.value() + " ".repeat(shape.extent() - text.extent())));
                    }
                }
            }
            NodeKey key = new NodeKey(occurrence.programUnitId(), move.meta().id());
            var basic = fact(whole, copy, moves.get(key).nextStatement());
            moves.put(key, new Move(whole, copy, basic.nextStatement(), basic.gaps(), adjustment));
        }
        return new ScalarMoveSemantics(declarations, moves, calls,
                new Metrics(counts[0], counts[1], counts[2], counts[3], counts[4]));
    }

    private static Move fact(Optional<ResolutionContracts.SemanticEntityId> whole,
                             Copy copy, Optional<Integer> next) {
        List<Gap> gaps = new ArrayList<>(3);
        if (whole.isEmpty()) gaps.add(Gap.SCALAR_WHOLE_ITEM_NOT_PROVEN);
        if (copy == Copy.UNAVAILABLE) gaps.add(Gap.MOVE_IDENTITY_NOT_PROVEN);
        if (next.isEmpty()) gaps.add(Gap.NORMAL_CONTINUATION_NOT_AVAILABLE);
        return new Move(whole, copy, next, gaps, Optional.empty());
    }

    private static boolean hasOverlay(Ast.Section section, long[] counts) {
        Deque<Ast.Node> pending = new ArrayDeque<>(section.children());
        boolean overlay = false;
        while (!pending.isEmpty()) {
            Ast.Node node = pending.pop();
            counts[0]++;
            if (node instanceof Ast.RedefinesClause || node instanceof Ast.RenamesClause
                    || node instanceof Ast.PreservedDataClause) overlay = true;
            for (var child : Ast.children(node)) pending.push(child);
        }
        return overlay;
    }

    private static Optional<ScalarText> scalar(Ast.DataEntry entry, long[] counts) {
        if (!entry.children().isEmpty() || entry.filler()
                || entry.visibility() != Ast.DeclarationVisibility.LOCAL
                || !(entry.level().equals("01") || entry.levelKind() == Ast.DataLevelKind.STANDALONE_77))
            return Optional.empty();
        Optional<Integer> extent = Optional.empty();
        int pictures = 0, usages = 0;
        for (Ast.DataClause clause : entry.clauses()) {
            counts[0]++;
            if (clause instanceof Ast.PictureClause picture) { pictures++; extent = picture.textExtent(); }
            else if (clause instanceof Ast.UsageClause usage && usage.display()) usages++;
            else return Optional.empty();
        }
        return pictures == 1 && usages <= 1 ? extent.map(ScalarText::new) : Optional.empty();
    }
}
