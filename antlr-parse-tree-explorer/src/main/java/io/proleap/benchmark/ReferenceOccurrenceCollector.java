package io.proleap.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Collects typed nominal occurrences exclusively from AST contracts.
 * It performs no lookup, candidate selection, source-text parsing or binding.
 */
final class ReferenceOccurrenceCollector {
    private static final Logger LOG = LoggerFactory.getLogger(ReferenceOccurrenceCollector.class);
    private final List<ReferenceOccurrences.Occurrence> occurrences = new ArrayList<>();
    private final Set<Integer> visitedReferenceNodeIds = new HashSet<>();
    private ResolutionContracts.ProgramUnitId programUnitId;
    private AstScopeIndex scopes;
    private String source;

    ReferenceOccurrences collect(ResolutionContracts.ProgramUnitId programUnitId,
                                 Ast.Program program, AstScopeIndex scopes) {
        this.programUnitId = Objects.requireNonNull(programUnitId, "programUnitId");
        this.scopes = Objects.requireNonNull(scopes, "scopes");
        this.source = program.meta().provenance().original().file();
        occurrences.clear();
        visitedReferenceNodeIds.clear();
        visit(program, ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT,
                ReferenceOccurrences.Preservation.STRUCTURED);
        ReferenceOccurrences result = new ReferenceOccurrences(occurrences);
        if (LOG.isDebugEnabled()) {
            Map<ResolutionContracts.ReferenceKind, Long> byKind = result.occurrences().stream()
                    .collect(java.util.stream.Collectors.groupingBy(ReferenceOccurrences.Occurrence::kind,
                            () -> new EnumMap<>(ResolutionContracts.ReferenceKind.class),
                            java.util.stream.Collectors.counting()));
            Map<ResolutionContracts.ReferenceRole, Long> byRole = result.occurrences().stream()
                    .collect(java.util.stream.Collectors.groupingBy(ReferenceOccurrences.Occurrence::role,
                            () -> new EnumMap<>(ResolutionContracts.ReferenceRole.class),
                            java.util.stream.Collectors.counting()));
            LOG.debug("event=references_collected scope=PROGRAM_UNIT source={} programUnit={} phase=REFERENCE_COLLECTION total={} byKind={} byRole={}",
                    source, programUnitId.canonicalProgramName(), result.occurrences().size(), byKind, byRole);
        }
        return result;
    }

    private void visit(Ast.Node node, ResolutionContracts.ReferenceRole role,
                       ReferenceOccurrences.Preservation preservation) {
        if (node instanceof Ast.DataReference reference) {
            if (role == ResolutionContracts.ReferenceRole.SUBSCRIPT) {
                addDataReference(reference, role, preservation, ResolutionContracts.ReferenceKind.INDEX,
                        EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX));
            } else {
                addDataReference(reference, role, preservation, null);
            }
            return;
        }
        if (node instanceof Ast.ProcedureReference reference) {
            add(reference, ResolutionContracts.ReferenceKind.PROCEDURE, role,
                    reference.writtenText(), preservation);
            if (reference.qualifier() != null) {
                Ast.ProcedureQualifier qualifier = reference.qualifier();
                add(qualifier, ResolutionContracts.ReferenceKind.PROCEDURE,
                        ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT,
                        qualifier.sectionName(), preservation);
            }
            return;
        }
        if (node instanceof Ast.FileReference reference) {
            add(reference, ResolutionContracts.ReferenceKind.FILE, role, reference.writtenText(), preservation);
            return;
        }
        if (node instanceof Ast.ProgramReference reference) {
            add(reference, ResolutionContracts.ReferenceKind.PROGRAM, role, reference.writtenText(), preservation);
            return;
        }
        if (node instanceof Ast.IndexReference reference) {
            add(reference, ResolutionContracts.ReferenceKind.INDEX, role, reference.writtenText(), preservation);
            return;
        }
        if (node instanceof Ast.NamedReference reference) {
            add(reference, ResolutionContracts.ReferenceKind.PRESERVED_NAMED, role,
                    reference.writtenText(), ReferenceOccurrences.Preservation.PRESERVED_NODE);
            return;
        }
        if (node instanceof Ast.CallStatement statement) {
            visit(statement.target(), ResolutionContracts.ReferenceRole.CALL_TARGET, preservation);
            for (Ast.CallArgument argument : statement.arguments())
                if (argument.value() != null) {
                    if (argument.passingMode() == Ast.PassingMode.REFERENCE
                            && argument.value() instanceof Ast.DataReference dataReference) {
                        addDataReference(dataReference, ResolutionContracts.ReferenceRole.CALL_ARGUMENT,
                                preservation, null, EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                        ResolutionContracts.ReferenceKind.FILE));
                    } else {
                        visit(argument.value(), ResolutionContracts.ReferenceRole.CALL_ARGUMENT, preservation);
                    }
                }
            if (statement.returning() != null)
                visit(statement.returning(), ResolutionContracts.ReferenceRole.CALL_RETURNING, preservation);
            for (Ast.Statement nested : statement.exceptionFlow()) visit(nested, role, preservation);
            return;
        }
        if (node instanceof Ast.MoveStatement statement) {
            visit(statement.source(), ResolutionContracts.ReferenceRole.VALUE_READ, preservation);
            for (Ast.Expression target : statement.targets())
                visit(target, ResolutionContracts.ReferenceRole.VALUE_WRITE, preservation);
            return;
        }
        if (node instanceof Ast.IfStatement statement) {
            visit(statement.condition(), ResolutionContracts.ReferenceRole.VALUE_READ, preservation);
            for (Ast.Statement nested : statement.thenBranch()) visit(nested, role, preservation);
            for (Ast.Statement nested : statement.elseBranch()) visit(nested, role, preservation);
            return;
        }
        if (node instanceof Ast.EvaluateStatement statement) {
            for (Ast.Expression subject : statement.subjects())
                visit(subject, ResolutionContracts.ReferenceRole.VALUE_READ, preservation);
            for (Ast.EvaluateBranch branch : statement.branches()) {
                for (Ast.Expression selector : branch.selectorExpressions())
                    visit(selector, ResolutionContracts.ReferenceRole.VALUE_READ, preservation);
                for (Ast.Statement nested : branch.statements()) visit(nested, role, preservation);
            }
            return;
        }
        if (node instanceof Ast.GoToStatement statement) {
            for (Ast.ProcedureReference target : statement.targets())
                visit(target, ResolutionContracts.ReferenceRole.GO_TO_TARGET, preservation);
            if (statement.dependingOn() != null)
                visit(statement.dependingOn(), ResolutionContracts.ReferenceRole.GO_TO_SELECTOR, preservation);
            return;
        }
        if (node instanceof Ast.PerformStatement statement) {
            if (statement.fromReference() != null)
                visit(statement.fromReference(), ResolutionContracts.ReferenceRole.PERFORM_FROM, preservation);
            if (statement.throughReference() != null)
                visit(statement.throughReference(), ResolutionContracts.ReferenceRole.PERFORM_THROUGH, preservation);
            for (Ast.Expression control : statement.controlExpressions())
                visit(control, ResolutionContracts.ReferenceRole.VALUE_READ, preservation);
            for (Ast.Statement nested : statement.inlineBody()) visit(nested, role, preservation);
            return;
        }
        if (node instanceof Ast.RedefinesClause clause) {
            visit(clause.target(), ResolutionContracts.ReferenceRole.REDEFINES_TARGET, preservation);
            return;
        }
        if (node instanceof Ast.RenamesClause clause) {
            visit(clause.from(), ResolutionContracts.ReferenceRole.RENAMES_FROM, preservation);
            if (clause.through() != null)
                visit(clause.through(), ResolutionContracts.ReferenceRole.RENAMES_THROUGH, preservation);
            return;
        }
        if (node instanceof Ast.OccursClause clause) {
            if (clause.minimum() != null)
                visit(clause.minimum(), ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT, preservation);
            if (clause.maximum() != null)
                visit(clause.maximum(), ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT, preservation);
            if (clause.dependingOn() != null)
                visit(clause.dependingOn(), ResolutionContracts.ReferenceRole.OCCURS_DEPENDING_ON, preservation);
            for (Ast.DataReference key : clause.keys())
                visit(key, ResolutionContracts.ReferenceRole.OCCURS_KEY, preservation);
            for (Ast.IndexReference index : clause.indexes())
                visit(index, ResolutionContracts.ReferenceRole.OCCURS_INDEX, preservation);
            return;
        }
        if (node instanceof Ast.ProcedureSignature signature) {
            for (Ast.ProcedureParameter parameter : signature.parameters())
                if (parameter.reference() != null)
                    visit(parameter.reference(), ResolutionContracts.ReferenceRole.PROCEDURE_PARAMETER, preservation);
            if (signature.returning() != null)
                visit(signature.returning(), ResolutionContracts.ReferenceRole.PROCEDURE_RETURNING, preservation);
            return;
        }
        if (node instanceof Ast.ModeledStatement statement) {
            visitStatementOperands(statement.operands(), statement.clauses(), preservation);
            return;
        }
        if (node instanceof Ast.PreservedStatement statement) {
            visitStatementOperands(statement.operands(), statement.clauses(),
                    ReferenceOccurrences.Preservation.PRESERVED_CONTAINER);
            return;
        }
        if (node instanceof Ast.UnsupportedStatement statement) {
            for (Ast.Node reference : statement.recognizedReferences())
                visit(reference, ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT,
                        ReferenceOccurrences.Preservation.PRESERVED_CONTAINER);
            for (Ast.Statement nested : statement.nestedStatements())
                visit(nested, role, ReferenceOccurrences.Preservation.PRESERVED_CONTAINER);
            return;
        }
        if (node instanceof Ast.PreservedDataClause clause) {
            for (Ast.Node reference : clause.recognizedReferences())
                visit(reference, ResolutionContracts.ReferenceRole.DECLARATION_RELATION,
                        ReferenceOccurrences.Preservation.PRESERVED_CONTAINER);
            return;
        }
        if (node instanceof Ast.PreservedExpression expression) {
            for (Ast.Expression operand : expression.recognizedOperands())
                visit(operand, role, ReferenceOccurrences.Preservation.PRESERVED_NODE);
            return;
        }
        if (node instanceof Ast.ReferenceModification modification) {
            visit(modification.offset(), ResolutionContracts.ReferenceRole.REFERENCE_MODIFICATION_OFFSET, preservation);
            if (modification.length() != null)
                visit(modification.length(), ResolutionContracts.ReferenceRole.REFERENCE_MODIFICATION_LENGTH, preservation);
            return;
        }
        if (node instanceof Ast.FunctionExpression expression) {
            for (Ast.Expression argument : expression.arguments())
                visit(argument, ResolutionContracts.ReferenceRole.VALUE_READ, preservation);
            if (expression.referenceModification() != null)
                visit(expression.referenceModification(), role, preservation);
            return;
        }
        if (node instanceof Ast.StatementClause clause) {
            for (Ast.Node recognized : clause.recognizedNodes()) visit(recognized, role, preservation);
            for (Ast.Statement nested : clause.nestedStatements()) visit(nested, role, preservation);
            return;
        }
        for (Ast.Node child : Ast.children(node)) visit(child, role, preservation);
    }

    private void visitStatementOperands(List<Ast.StatementOperand> operands,
                                        List<Ast.StatementClause> clauses,
                                        ReferenceOccurrences.Preservation preservation) {
        for (Ast.StatementOperand operand : operands) {
            ResolutionContracts.ReferenceRole role = operand.value() instanceof Ast.FileReference
                    ? ResolutionContracts.ReferenceRole.FILE_OPERATION
                    : ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT;
            visit(operand.value(), role, preservation);
        }
        for (Ast.StatementClause clause : clauses) visit(clause,
                ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT, preservation);
    }

    private void addDataReference(Ast.DataReference reference, ResolutionContracts.ReferenceRole role,
                                  ReferenceOccurrences.Preservation preservation,
                                  ResolutionContracts.ReferenceKind kindOverride) {
        addDataReference(reference, role, preservation, kindOverride, null);
    }

    private void addDataReference(Ast.DataReference reference, ResolutionContracts.ReferenceRole role,
                                  ReferenceOccurrences.Preservation preservation,
                                  ResolutionContracts.ReferenceKind kindOverride,
                                  Set<ResolutionContracts.ReferenceKind> admissibleKindsOverride) {
        ResolutionContracts.ReferenceKind kind = kindOverride != null ? kindOverride
                : "conditionNameReference".equals(reference.meta().origin().grammarRule())
                ? ResolutionContracts.ReferenceKind.CONDITION : ResolutionContracts.ReferenceKind.DATA;
        ReferenceOccurrences.Preservation effective = reference.understanding() == Ast.ReferenceUnderstanding.PRESERVED
                ? ReferenceOccurrences.Preservation.PRESERVED_NODE : preservation;
        add(reference, kind, role, reference.writtenText(), effective,
                admissibleKindsOverride == null ? Set.of(kind) : admissibleKindsOverride);
        for (Ast.DataQualifier qualifier : reference.qualifiers()) {
            ResolutionContracts.ReferenceKind qualifierKind = qualifier.target() == Ast.QualifierTarget.FILE
                    ? ResolutionContracts.ReferenceKind.FILE : ResolutionContracts.ReferenceKind.DATA;
            Set<ResolutionContracts.ReferenceKind> qualifierKinds = qualifier.target()
                    == Ast.QualifierTarget.DATA_OR_FILE
                    ? EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                            ResolutionContracts.ReferenceKind.FILE)
                    : Set.of(qualifierKind);
            addDataReference(qualifier.reference(), ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT,
                    effective, qualifierKind, qualifierKinds);
        }
        for (Ast.SubscriptGroup group : reference.subscriptGroups())
            for (Ast.Expression subscript : group.subscripts())
                visit(subscript, ResolutionContracts.ReferenceRole.SUBSCRIPT, effective);
        if (reference.referenceModification() != null)
            visit(reference.referenceModification(), role, effective);
    }

    private void add(Ast.Node reference, ResolutionContracts.ReferenceKind kind,
                     ResolutionContracts.ReferenceRole role, String writtenText,
                     ReferenceOccurrences.Preservation preservation) {
        add(reference, kind, role, writtenText, preservation, Set.of(kind));
    }

    private void add(Ast.Node reference, ResolutionContracts.ReferenceKind kind,
                     ResolutionContracts.ReferenceRole role, String writtenText,
                     ReferenceOccurrences.Preservation preservation,
                     Set<ResolutionContracts.ReferenceKind> admissibleKinds) {
        if (!visitedReferenceNodeIds.add(reference.meta().id()))
            throw new IllegalStateException("reference AST node reached twice: " + reference.meta().id());
        int id = occurrences.size();
        occurrences.add(new ReferenceOccurrences.Occurrence(id, programUnitId,
                reference.meta().id(), scopes.scopeId(reference), kind, admissibleKinds, role,
                reference.meta().origin().grammarRule(), writtenText, reference.meta(), preservation));
        if (LOG.isTraceEnabled()) {
            LOG.trace("event=reference_collected source={} programUnit={} phase=REFERENCE_COLLECTION occurrenceId={} kind={} role={} writtenName={} line={} grammarRule={} preservation={}",
                    source, programUnitId.canonicalProgramName(), id, kind, role, writtenText,
                    reference.meta().span().startLine(), reference.meta().origin().grammarRule(), preservation);
        }
    }
}
