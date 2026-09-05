package io.github.gustavo2358.cobolexplorer.semanticproduct.targetmodel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Independent test-only consumer that checks lowering sufficiency using only
 * the target port. It reconstructs an outline; it does not build IR, CFG or
 * dataflow facts.
 */
public final class SemanticProductTargetConsumer {
    private SemanticProductTargetConsumer() { }

    public record SourceAnchor(String file, int line, boolean exact) {
        public SourceAnchor {
            file = requireText(file, "file");
            if (line < 0) throw new IllegalArgumentException("line must be non-negative");
        }
    }

    public record DataInput(SemanticProductTargetModel.DataItemId identity,
                            String canonicalName, String picture,
                            SemanticProductTargetModel.CoverageStatus coverage,
                            SemanticProductTargetModel.Readiness readiness,
                            SourceAnchor declaration) {
        public DataInput {
            identity = Objects.requireNonNull(identity, "identity");
            canonicalName = requireText(canonicalName, "canonicalName");
            picture = requireText(picture, "picture");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
            declaration = Objects.requireNonNull(declaration, "declaration");
        }
    }

    public record StatementInput(SemanticProductTargetModel.StatementId identity,
                                 SemanticProductTargetModel.StatementKind kind,
                                 int programPoint,
                                 SemanticProductTargetModel.Containment containment,
                                 SemanticProductTargetModel.CoverageStatus coverage,
                                 SemanticProductTargetModel.Readiness readiness,
                                 SourceAnchor source) {
        public StatementInput {
            identity = Objects.requireNonNull(identity, "identity");
            kind = Objects.requireNonNull(kind, "kind");
            if (programPoint < 0)
                throw new IllegalArgumentException("programPoint must be non-negative");
            containment = Objects.requireNonNull(containment, "containment");
            coverage = Objects.requireNonNull(coverage, "coverage");
            readiness = Objects.requireNonNull(readiness, "readiness");
            source = Objects.requireNonNull(source, "source");
        }
    }

    public record BindingInput(SemanticProductTargetModel.ResolutionStatus status,
                               String reason,
                               List<SemanticProductTargetModel.DataCandidate> candidates,
                               Optional<SemanticProductTargetModel.DataItemId> selected) {
        public BindingInput {
            status = Objects.requireNonNull(status, "status");
            reason = requireText(reason, "reason");
            candidates = List.copyOf(candidates);
            selected = Objects.requireNonNull(selected, "selected");
        }
    }

    public record MoveInput(SemanticProductTargetModel.StatementId statement,
                            SemanticProductTargetModel.OperandId sourceOperand,
                            SemanticProductTargetModel.LiteralKind literalKind,
                            String literal,
                            SemanticProductTargetModel.OperandId targetOperand,
                            SemanticProductTargetModel.OperandRole role,
                            BindingInput targetBinding,
                            SourceAnchor literalSource) {
        public MoveInput {
            statement = Objects.requireNonNull(statement, "statement");
            sourceOperand = Objects.requireNonNull(sourceOperand, "sourceOperand");
            literalKind = Objects.requireNonNull(literalKind, "literalKind");
            literal = requireText(literal, "literal");
            targetOperand = Objects.requireNonNull(targetOperand, "targetOperand");
            role = Objects.requireNonNull(role, "role");
            targetBinding = Objects.requireNonNull(targetBinding, "targetBinding");
            literalSource = Objects.requireNonNull(literalSource, "literalSource");
        }
    }

    public record CallInput(SemanticProductTargetModel.StatementId statement,
                            SemanticProductTargetModel.CallSyntax syntax,
                            SemanticProductTargetModel.OperandId operandIdentity,
                            SemanticProductTargetModel.OperandRole role,
                            BindingInput operandBinding,
                            SemanticProductTargetModel.RuntimeTargetKnowledge runtimeTarget) {
        public CallInput {
            statement = Objects.requireNonNull(statement, "statement");
            syntax = Objects.requireNonNull(syntax, "syntax");
            operandIdentity = Objects.requireNonNull(operandIdentity, "operandIdentity");
            role = Objects.requireNonNull(role, "role");
            operandBinding = Objects.requireNonNull(operandBinding, "operandBinding");
            runtimeTarget = Objects.requireNonNull(runtimeTarget, "runtimeTarget");
        }
    }

    public record ConditionInput(
                          String surface,
                          SemanticProductTargetModel.OperandId subjectIdentity,
                          SemanticProductTargetModel.OperandRole subjectRole,
                          BindingInput subjectBinding,
                          SemanticProductTargetModel.RelationalOperator operator,
                          SemanticProductTargetModel.OperandId objectIdentity,
                          SemanticProductTargetModel.LiteralKind objectKind,
                          String objectValue,
                          SourceAnchor source) {
        public ConditionInput {
            surface = requireText(surface, "surface");
            subjectIdentity = Objects.requireNonNull(subjectIdentity, "subjectIdentity");
            subjectRole = Objects.requireNonNull(subjectRole, "subjectRole");
            subjectBinding = Objects.requireNonNull(subjectBinding, "subjectBinding");
            operator = Objects.requireNonNull(operator, "operator");
            objectIdentity = Objects.requireNonNull(objectIdentity, "objectIdentity");
            objectKind = Objects.requireNonNull(objectKind, "objectKind");
            objectValue = requireText(objectValue, "objectValue");
            source = Objects.requireNonNull(source, "source");
        }
    }

    public record ObservedStatementInput(
            SemanticProductTargetModel.StatementId statement,
            String observedKind,
            String observedShape,
            SemanticProductTargetModel.CoverageStatus coverage,
            String gapCode) {
        public ObservedStatementInput {
            statement = Objects.requireNonNull(statement, "statement");
            observedKind = requireText(observedKind, "observedKind");
            observedShape = requireText(observedShape, "observedShape");
            coverage = Objects.requireNonNull(coverage, "coverage");
            gapCode = requireText(gapCode, "gapCode");
        }
    }

    public record IfInput(SemanticProductTargetModel.StatementId statement,
                          ConditionInput condition,
                          List<SemanticProductTargetModel.StatementId> thenChildren,
                          List<SemanticProductTargetModel.StatementId> elseChildren,
                          SemanticProductTargetModel.StatementId continuation) {
        public IfInput {
            statement = Objects.requireNonNull(statement, "statement");
            condition = Objects.requireNonNull(condition, "condition");
            thenChildren = List.copyOf(thenChildren);
            elseChildren = List.copyOf(elseChildren);
            continuation = Objects.requireNonNull(continuation, "continuation");
        }
    }

    public record GapInput(SemanticProductTargetModel.StatementId statement,
                           SemanticProductTargetModel.GapScope scope,
                           String code, SourceAnchor source) {
        public GapInput {
            statement = Objects.requireNonNull(statement, "statement");
            scope = Objects.requireNonNull(scope, "scope");
            code = requireText(code, "code");
            source = Objects.requireNonNull(source, "source");
        }
    }

    public record LoweringReadinessOutline(
            SemanticProductTargetModel.UnitId unit,
            List<DataInput> data,
            List<SemanticProductTargetModel.StatementId> rootStatements,
            List<StatementInput> statements,
            List<MoveInput> moves,
            List<CallInput> calls,
            List<IfInput> branches,
            List<ObservedStatementInput> unmodeledStatements,
            List<GapInput> gaps,
            SemanticProductTargetModel.CoverageSummary coverage) {
        public LoweringReadinessOutline {
            unit = Objects.requireNonNull(unit, "unit");
            data = List.copyOf(data);
            rootStatements = List.copyOf(rootStatements);
            statements = List.copyOf(statements);
            moves = List.copyOf(moves);
            calls = List.copyOf(calls);
            branches = List.copyOf(branches);
            unmodeledStatements = List.copyOf(unmodeledStatements);
            gaps = List.copyOf(gaps);
            coverage = Objects.requireNonNull(coverage, "coverage");
        }
    }

    public static LoweringReadinessOutline consume(SemanticProductTargetModel.Port port) {
        Objects.requireNonNull(port, "port");
        List<DataInput> data = port.dataDeclarations().stream().map(declaration ->
                new DataInput(declaration.id(), declaration.canonicalName(),
                        declaration.picture(), declaration.coverage(), declaration.readiness(),
                        anchor(declaration.provenance()))).toList();
        List<StatementInput> statements = port.statements().stream().map(fact -> {
            SemanticProductTargetModel.StatementHeader header = fact.header();
            return new StatementInput(header.id(), header.kind(), header.point().ordinal(),
                    header.containment(), header.coverage(), header.readiness(),
                    anchor(header.provenance()));
        }).toList();
        List<MoveInput> moves = port.moves().stream().map(move ->
                new MoveInput(move.header().id(), move.source().id(), move.source().kind(),
                        move.source().value(), move.target().id(), move.target().role(),
                        binding(move.target().binding()), anchor(move.source().provenance())))
                .toList();
        List<CallInput> calls = port.calls().stream().map(call ->
                new CallInput(call.header().id(), call.syntax(), call.operand().id(),
                        call.operand().role(), binding(call.operand().binding()),
                        call.runtimeTarget())).toList();
        List<IfInput> branches = port.ifs().stream().map(branch ->
                new IfInput(branch.header().id(), condition(branch.condition()),
                        branch.thenChildren(), branch.elseChildren(), branch.continuation()))
                .toList();
        List<ObservedStatementInput> unmodeledStatements = port.statements().stream()
                .filter(SemanticProductTargetModel.ObservedStatementFact.class::isInstance)
                .map(SemanticProductTargetModel.ObservedStatementFact.class::cast)
                .map(observed -> new ObservedStatementInput(observed.header().id(),
                        observed.observedKind(), observed.observedShape(),
                        observed.header().coverage(), observed.gapCode()))
                .toList();
        List<GapInput> gaps = port.gaps().stream().map(gap ->
                new GapInput(gap.statement(), gap.scope(), gap.code(),
                        anchor(gap.provenance()))).toList();

        return new LoweringReadinessOutline(port.unit(), data, port.rootStatements(),
                statements, moves, calls, branches, unmodeledStatements, gaps,
                port.coverage());
    }

    private static SourceAnchor anchor(SemanticProductTargetModel.Provenance provenance) {
        SemanticProductTargetModel.Location location = provenance.original();
        return new SourceAnchor(location.file(), location.startLine(), provenance.exact());
    }

    private static ConditionInput condition(
            SemanticProductTargetModel.ConditionFact condition) {
        return new ConditionInput(condition.surface(), condition.subject().id(),
                condition.subject().role(), binding(condition.subject().binding()),
                condition.operator(),
                condition.object().id(), condition.object().kind(),
                condition.object().value(), anchor(condition.provenance()));
    }

    private static BindingInput binding(SemanticProductTargetModel.NominalBinding binding) {
        return new BindingInput(binding.status(), binding.reason(), binding.candidates(),
                binding.selected());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
