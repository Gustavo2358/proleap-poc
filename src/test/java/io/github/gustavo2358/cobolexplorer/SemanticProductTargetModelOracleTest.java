package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolMoveCallAdapter;
import io.github.gustavo2358.cobolexplorer.semanticproduct.targetmodel.SemanticProductTargetConsumer;
import io.github.gustavo2358.cobolexplorer.semanticproduct.targetmodel.SemanticProductTargetModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executable, test-only oracle for WORK-SEMANTIC-PRODUCT-002 Checkpoint 1. */
class SemanticProductTargetModelOracleTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/semantic/semantic-product-lowering-readiness.cbl");
    private static final String SOURCE_NAME = FIXTURE.getFileName().toString();
    private static final SemanticProductTargetModel.UnitId UNIT =
            new SemanticProductTargetModel.UnitId(
                    SOURCE_NAME.toUpperCase(Locale.ROOT), List.of(0), "SEMANTIC-TARGET");
    private static final SemanticProductTargetModel.DataItemId WS_X = dataId(10);
    private static final SemanticProductTargetModel.DataItemId FLAG = dataId(11);
    private static final SemanticProductTargetModel.DataItemId AUX_PGM = dataId(12);

    private static final SemanticProductTargetModel.StatementId MOVE_A = statementId(0);
    private static final SemanticProductTargetModel.StatementId MOVE_AUX = statementId(1);
    private static final SemanticProductTargetModel.StatementId OUTER_IF = statementId(2);
    private static final SemanticProductTargetModel.StatementId MOVE_B = statementId(3);
    private static final SemanticProductTargetModel.StatementId INNER_IF = statementId(4);
    private static final SemanticProductTargetModel.StatementId NESTED_CALL_AUX = statementId(5);
    private static final SemanticProductTargetModel.StatementId MOVE_NEST = statementId(6);
    private static final SemanticProductTargetModel.StatementId MOVE_AFTER = statementId(7);
    private static final SemanticProductTargetModel.StatementId MOVE_C = statementId(8);
    private static final SemanticProductTargetModel.StatementId CALL_X = statementId(9);
    private static final SemanticProductTargetModel.StatementId CALL_AUX = statementId(10);
    private static final SemanticProductTargetModel.StatementId LITERAL_CALL = statementId(11);

    @Test
    void controlledFixtureHasTheTargetEvidenceWhileProductionRemainsSingleton() throws Exception {
        AstBoundaryTestSupport.Analysis frontend = analyzeFixture();
        List<Ast.Statement> statements = AstBoundaryTestSupport.nodes(frontend).stream()
                .filter(Ast.Statement.class::isInstance).map(Ast.Statement.class::cast).toList();
        List<Ast.CallStatement> calls = statements.stream()
                .filter(Ast.CallStatement.class::isInstance)
                .map(Ast.CallStatement.class::cast).toList();
        List<Ast.IfStatement> branches = statements.stream()
                .filter(Ast.IfStatement.class::isInstance)
                .map(Ast.IfStatement.class::cast).toList();

        assertEquals(12, statements.size());
        assertEquals(6, statements.stream().filter(Ast.MoveStatement.class::isInstance).count());
        assertEquals(4, calls.size());
        assertEquals(3, calls.stream().filter(call -> call.targetSyntax()
                == Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION).count());
        assertEquals(1, calls.stream().filter(call -> call.targetSyntax()
                == Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME).count());
        assertEquals(2, branches.size());
        assertEquals(3, branches.get(0).thenBranch().size());
        assertEquals(1, branches.get(0).elseBranch().size());
        assertEquals(1, branches.get(1).thenBranch().size());
        assertEquals(1, branches.get(1).elseBranch().size());
        assertTrue(branches.stream().allMatch(branch ->
                branch.condition() instanceof Ast.RelationCondition relation
                        && relation.subject() instanceof Ast.DataReference
                        && relation.object() instanceof Ast.LiteralExpression
                        && relation.relationalOperator().equals("=")));

        SymbolTable table = frontend.tables().units().get(0).symbolTable();
        assertEquals(3, table.symbols().stream()
                .filter(symbol -> symbol.namespace() == SymbolTable.Namespace.DATA)
                .filter(symbol -> symbol.kind() == SymbolTable.SymbolKind.DATA_ITEM).count());
        assertEquals(6, frontend.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role()
                        == ResolutionContracts.ReferenceRole.VALUE_WRITE).count());
        assertEquals(3, frontend.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role()
                        == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .filter(entry -> entry.occurrence().kind()
                        == ResolutionContracts.ReferenceKind.DATA).count());

        SemanticProductTargetModel.State target = targetState();
        assertEquals(statements.size(), target.coverage().observedStatements(),
                "the target inventory must not silently omit a fixture statement");
        IllegalArgumentException currentGap = assertThrows(IllegalArgumentException.class,
                () -> CobolMoveCallAdapter.project(
                        new CobolMoveCallAdapter.FrontendProducts(frontend.build(),
                                frontend.tables(), frontend.occurrences(), frontend.resolution()),
                        frontend.model().programUnits().get(0).id()));
        assertEquals("the selected unit must contain exactly one MOVE", currentGap.getMessage());
    }

    @Test
    void boundaryOnlyConsumerObservesEverySupportedOccurrenceAndReadinessDimension() {
        SemanticProductTargetModel.Port port = SemanticProductTargetModel.open(targetState());
        SemanticProductTargetConsumer.LoweringReadinessOutline outline =
                SemanticProductTargetConsumer.consume(port);

        assertEquals(UNIT, outline.unit());
        assertEquals(List.of("WS-X", "FLAG", "AUX-PGM"), outline.data().stream()
                .map(SemanticProductTargetConsumer.DataInput::canonicalName).toList());
        assertEquals(List.of("X(8)", "9", "X(8)"), outline.data().stream()
                .map(SemanticProductTargetConsumer.DataInput::picture).toList());
        assertEquals(3, new HashSet<>(outline.data().stream()
                .map(SemanticProductTargetConsumer.DataInput::identity).toList()).size());
        assertTrue(outline.data().stream().allMatch(data -> data.identity().unit().equals(UNIT)));
        assertTrue(outline.data().stream().allMatch(data -> data.declaration().exact()));

        assertEquals(12, outline.statements().size());
        assertEquals(6, outline.moves().size());
        assertEquals(3, outline.calls().size());
        assertEquals(2, outline.branches().size());
        assertEquals(12, new HashSet<>(outline.statements().stream()
                .map(SemanticProductTargetConsumer.StatementInput::identity).toList()).size());
        assertEquals(12, new HashSet<>(outline.statements().stream()
                .map(SemanticProductTargetConsumer.StatementInput::programPoint).toList()).size());
        assertTrue(outline.statements().stream()
                .allMatch(statement -> statement.identity().unit().equals(UNIT)));
        assertTrue(outline.statements().stream()
                .allMatch(statement -> statement.source().file().equals(SOURCE_NAME)));

        assertTrue(outline.moves().stream().allMatch(move ->
                move.literalKind() == SemanticProductTargetModel.LiteralKind.ALPHANUMERIC
                        && move.role() == SemanticProductTargetModel.OperandRole.WRITE
                        && move.binding()
                        == SemanticProductTargetModel.ResolutionStatus.RESOLVED));
        assertTrue(port.moves().stream().allMatch(move ->
                move.target().binding().selected().equals(java.util.Optional.of(
                        move.target().dataItem()))
                        && move.target().binding().candidates().size() == 1
                        && move.target().provenance().exact()
                        && move.source().provenance().exact()));
        assertTrue(outline.calls().stream().allMatch(call ->
                call.syntax() == SemanticProductTargetModel.CallSyntax.IDENTIFIER_OR_EXPRESSION
                        && call.role() == SemanticProductTargetModel.OperandRole.CALL_TARGET
                        && call.binding()
                        == SemanticProductTargetModel.ResolutionStatus.RESOLVED
                        && call.runtimeTarget()
                        == SemanticProductTargetModel.RuntimeTargetKnowledge.UNKNOWN));
        assertTrue(port.calls().stream().allMatch(call ->
                call.operand().binding().selected().equals(java.util.Optional.of(
                        call.operand().dataItem()))
                        && call.operand().provenance().exact()
                        && call.header().provenance().exact()));
        assertTrue(port.ifs().stream().flatMap(branch ->
                        branch.condition().references().stream())
                .allMatch(reference ->
                        reference.role() == SemanticProductTargetModel.OperandRole.READ
                                && reference.binding().status()
                                == SemanticProductTargetModel.ResolutionStatus.RESOLVED
                                && reference.provenance().exact()));
        Set<SemanticProductTargetModel.OperandId> operands = new HashSet<>();
        port.moves().forEach(move -> {
            operands.add(move.source().id());
            operands.add(move.target().id());
        });
        port.calls().forEach(call -> operands.add(call.operand().id()));
        port.ifs().forEach(branch -> {
            operands.add(branch.condition().subject().id());
            operands.add(branch.condition().object().id());
        });
        assertEquals(19, operands.size());
        assertTrue(operands.stream().allMatch(operand -> operand.statement().unit().equals(UNIT)));
        assertEquals(List.of(AUX_PGM, WS_X, AUX_PGM), outline.calls().stream()
                .map(SemanticProductTargetConsumer.CallInput::operand).toList(),
                "CALL facts are independent from MOVE pairing and source proximity");

        assertTrue(outline.statements().stream().allMatch(statement ->
                statement.readiness().lowering() != null
                        && statement.readiness().cfg() != null
                        && statement.readiness().effectsDataflow() != null));
        assertEquals(SemanticProductTargetModel.ReadinessStatus.BLOCKED,
                outline.coverage().readiness().lowering().status());
        assertEquals(SemanticProductTargetModel.ReadinessStatus.BLOCKED,
                outline.coverage().readiness().cfg().status());
        assertEquals(SemanticProductTargetModel.ReadinessStatus.BLOCKED,
                outline.coverage().readiness().effectsDataflow().status());
    }

    @Test
    void structuralOraclePreservesTheFutureJoinInputsWithoutComputingDataflow() {
        SemanticProductTargetConsumer.LoweringReadinessOutline outline =
                SemanticProductTargetConsumer.consume(
                        SemanticProductTargetModel.open(targetState()));
        SemanticProductTargetConsumer.IfInput outer = outline.branches().stream()
                .filter(branch -> branch.statement().equals(OUTER_IF)).findFirst().orElseThrow();
        SemanticProductTargetConsumer.IfInput inner = outline.branches().stream()
                .filter(branch -> branch.statement().equals(INNER_IF)).findFirst().orElseThrow();
        SemanticProductTargetConsumer.MoveInput moveB = move(outline, "B");
        SemanticProductTargetConsumer.MoveInput moveC = move(outline, "C");
        SemanticProductTargetConsumer.CallInput callX = outline.calls().stream()
                .filter(call -> call.statement().equals(CALL_X)).findFirst().orElseThrow();

        assertEquals("FLAG = 1", outer.condition().surface());
        assertEquals(FLAG, outer.condition().subject());
        assertEquals(SemanticProductTargetModel.OperandRole.READ,
                outer.condition().subjectRole());
        assertEquals(SemanticProductTargetModel.ResolutionStatus.RESOLVED,
                outer.condition().subjectBinding());
        assertEquals(SemanticProductTargetModel.RelationalOperator.EQUALS,
                outer.condition().operator());
        assertEquals(SemanticProductTargetModel.LiteralKind.NUMERIC,
                outer.condition().objectKind());
        assertEquals("1", outer.condition().objectValue());
        assertEquals(List.of(MOVE_B, INNER_IF, MOVE_AFTER), outer.thenChildren());
        assertEquals(List.of(MOVE_C), outer.elseChildren());
        assertEquals(CALL_X, outer.continuation());
        assertEquals(List.of(NESTED_CALL_AUX), inner.thenChildren());
        assertEquals(List.of(MOVE_NEST), inner.elseChildren());
        assertEquals(MOVE_AFTER, inner.continuation());

        assertEquals(WS_X, moveB.target());
        assertEquals(WS_X, moveC.target());
        assertEquals(WS_X, callX.operand());
        assertTrue(outline.rootStatements().indexOf(MOVE_A)
                < outline.rootStatements().indexOf(OUTER_IF));
        assertTrue(outline.rootStatements().indexOf(OUTER_IF)
                < outline.rootStatements().indexOf(CALL_X));

        Set<String> forbiddenResults = Set.of("truthValue", "reachability", "cfgEdges",
                "branchProbability", "reachingDefinitions", "possibleValues");
        assertTrue(Arrays.stream(SemanticProductTargetConsumer.LoweringReadinessOutline.class
                        .getRecordComponents()).map(RecordComponent::getName)
                .noneMatch(forbiddenResults::contains));
        assertTrue(Arrays.stream(SemanticProductTargetModel.IfFact.class.getRecordComponents())
                .map(RecordComponent::getName)
                .noneMatch(name -> name.equals("elsePresent") || name.equals("hasElse")),
                "the current frontend does not distinguish absent from syntactically empty ELSE");
    }

    @Test
    void unsupportedAndUnknownRemainVisibleWithoutSuppressingIndependentFacts() {
        SemanticProductTargetModel.Port port = SemanticProductTargetModel.open(targetState());

        assertEquals(12, port.coverage().observedStatements());
        assertEquals(9, port.coverage().modeledStatements());
        assertEquals(2, port.coverage().partialStatements());
        assertEquals(1, port.coverage().unsupportedStatements());
        assertEquals(0, port.coverage().inputMissingStatements());
        assertEquals(List.of("CALL_LITERAL_OUTSIDE_INITIAL_CAPABILITY"), port.statements().stream()
                .filter(SemanticProductTargetModel.UnsupportedStatementFact.class::isInstance)
                .map(SemanticProductTargetModel.UnsupportedStatementFact.class::cast)
                .map(SemanticProductTargetModel.UnsupportedStatementFact::gapCode).toList());
        assertEquals(3, port.gaps().stream()
                .filter(gap -> gap.scope()
                        == SemanticProductTargetModel.GapScope.RUNTIME_CALL_TARGET).count());
        assertEquals(2, port.gaps().stream()
                .filter(gap -> gap.scope()
                        == SemanticProductTargetModel.GapScope.CONDITION_SEMANTICS).count());
        assertEquals(1, port.gaps().stream()
                .filter(gap -> gap.scope()
                        == SemanticProductTargetModel.GapScope.CAPABILITY).count());
        assertEquals(6, port.moves().size(),
                "unsupported CALL must not erase independently supported MOVE facts");
        assertEquals(3, port.calls().size(),
                "unsupported CALL shape must not erase supported variable CALL facts");
    }

    @Test
    void invalidNamespaceCoverageOrSummaryClaimsFailClosed() {
        SemanticProductTargetModel.State target = targetState();
        SemanticProductTargetModel.UnitId otherUnit = new SemanticProductTargetModel.UnitId(
                UNIT.compilationUnitId(), List.of(1), UNIT.canonicalProgramName());
        assertNotEquals(WS_X, new SemanticProductTargetModel.DataItemId(otherUnit, WS_X.localId()),
                "a local id or name cannot join facts across unit namespaces");

        SemanticProductTargetModel.Readiness falselyComplete = readiness(
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT, "all lowering ready",
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT, "all CFG ready",
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT, "all effects ready");
        assertThrows(IllegalArgumentException.class, () -> new SemanticProductTargetModel.State(
                target.unit(), target.dataDeclarations(), target.rootStatements(),
                target.statements(), target.gaps(),
                new SemanticProductTargetModel.CoverageSummary(
                        12, 9, 2, 1, 0, falselyComplete)));
        assertThrows(IllegalArgumentException.class, () -> new SemanticProductTargetModel.State(
                target.unit(), target.dataDeclarations(), target.rootStatements(),
                target.statements().subList(0, 11), target.gaps(),
                new SemanticProductTargetModel.CoverageSummary(
                        12, 9, 2, 1, 0, target.coverage().readiness())));
    }

    @Test
    void targetContractIsPluralImmutableAndFreeOfFrontendDependencies() throws IOException {
        SemanticProductTargetModel.Port port = SemanticProductTargetModel.open(targetState());
        Set<String> methods = new HashSet<>();
        Arrays.stream(SemanticProductTargetModel.Port.class.getDeclaredMethods())
                .map(method -> method.getName()).forEach(methods::add);
        assertTrue(methods.containsAll(Set.of("statements", "moves", "calls", "ifs")));
        assertFalse(methods.contains("move"));
        assertFalse(methods.contains("call"));
        assertThrows(UnsupportedOperationException.class, () -> port.statements().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.dataDeclarations().clear());
        assertThrows(UnsupportedOperationException.class, () -> port.rootStatements().clear());

        for (Path source : List.of(
                Path.of("src/test/java/io/github/gustavo2358/cobolexplorer/semanticproduct/"
                        + "targetmodel/SemanticProductTargetModel.java"),
                Path.of("src/test/java/io/github/gustavo2358/cobolexplorer/semanticproduct/"
                        + "targetmodel/SemanticProductTargetConsumer.java"))) {
            String code = Files.readString(source, StandardCharsets.UTF_8);
            assertFalse(code.contains("import io.github.gustavo2358.cobolexplorer."));
            assertFalse(code.contains("io.github.gustavo2358.cobolexplorer.Ast"));
            assertFalse(code.contains("CobolSemanticProduct"));
            assertFalse(code.contains("CobolSemanticPort"));
            assertFalse(code.contains("CobolMoveCallAdapter"));
            assertFalse(code.contains("org.antlr"));
            assertFalse(code.contains("writtenText"));
            assertFalse(code.contains("grammarRule"));
            assertFalse(code.contains("SourceMap"));
        }
    }

    private static AstBoundaryTestSupport.Analysis analyzeFixture() throws IOException {
        return AstBoundaryTestSupport.analyze(
                Files.readString(FIXTURE, StandardCharsets.UTF_8), SOURCE_NAME);
    }

    private static SemanticProductTargetModel.State targetState() {
        List<SemanticProductTargetModel.DataDeclaration> data = List.of(
                declaration(WS_X, "WS-X", "X(8)", 5),
                declaration(FLAG, "FLAG", "9", 6),
                declaration(AUX_PGM, "AUX-PGM", "X(8)", 7));

        List<SemanticProductTargetModel.StatementFact> statements = List.of(
                move(MOVE_A, 0, "A", WS_X, 9,
                        SemanticProductTargetModel.Containment.root()),
                move(MOVE_AUX, 1, "AUXPGM", AUX_PGM, 10,
                        SemanticProductTargetModel.Containment.root()),
                branch(OUTER_IF, 2, "FLAG = 1", "1", 11,
                        SemanticProductTargetModel.Containment.root(),
                        List.of(MOVE_B, INNER_IF, MOVE_AFTER), List.of(MOVE_C), CALL_X),
                move(MOVE_B, 3, "B", WS_X, 12,
                        childOf(OUTER_IF, SemanticProductTargetModel.Branch.THEN)),
                branch(INNER_IF, 4, "FLAG = 2", "2", 13,
                        childOf(OUTER_IF, SemanticProductTargetModel.Branch.THEN),
                        List.of(NESTED_CALL_AUX), List.of(MOVE_NEST), MOVE_AFTER),
                call(NESTED_CALL_AUX, 5, AUX_PGM, 14,
                        childOf(INNER_IF, SemanticProductTargetModel.Branch.THEN)),
                move(MOVE_NEST, 6, "NEST", AUX_PGM, 16,
                        childOf(INNER_IF, SemanticProductTargetModel.Branch.ELSE)),
                move(MOVE_AFTER, 7, "AFTER", AUX_PGM, 18,
                        childOf(OUTER_IF, SemanticProductTargetModel.Branch.THEN)),
                move(MOVE_C, 8, "C", WS_X, 20,
                        childOf(OUTER_IF, SemanticProductTargetModel.Branch.ELSE)),
                call(CALL_X, 9, WS_X, 22, SemanticProductTargetModel.Containment.root()),
                call(CALL_AUX, 10, AUX_PGM, 23,
                        SemanticProductTargetModel.Containment.root()),
                unsupportedLiteralCall());

        List<SemanticProductTargetModel.Gap> gaps = List.of(
                gap(OUTER_IF, SemanticProductTargetModel.GapScope.CONDITION_SEMANTICS,
                        "CONDITION_SEMANTICS_NOT_AVAILABLE", 11),
                gap(INNER_IF, SemanticProductTargetModel.GapScope.CONDITION_SEMANTICS,
                        "CONDITION_SEMANTICS_NOT_AVAILABLE", 13),
                gap(NESTED_CALL_AUX, SemanticProductTargetModel.GapScope.RUNTIME_CALL_TARGET,
                        "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN", 14),
                gap(CALL_X, SemanticProductTargetModel.GapScope.RUNTIME_CALL_TARGET,
                        "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN", 22),
                gap(CALL_AUX, SemanticProductTargetModel.GapScope.RUNTIME_CALL_TARGET,
                        "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN", 23),
                gap(LITERAL_CALL, SemanticProductTargetModel.GapScope.CAPABILITY,
                        "CALL_LITERAL_OUTSIDE_INITIAL_CAPABILITY", 24));

        SemanticProductTargetModel.Readiness summary = readiness(
                SemanticProductTargetModel.ReadinessStatus.BLOCKED,
                "literal CALL has no lowering contract in this capability",
                SemanticProductTargetModel.ReadinessStatus.BLOCKED,
                "literal CALL control semantics are not claimed",
                SemanticProductTargetModel.ReadinessStatus.BLOCKED,
                "literal CALL effects are not claimed");
        return new SemanticProductTargetModel.State(UNIT, data,
                List.of(MOVE_A, MOVE_AUX, OUTER_IF, CALL_X, CALL_AUX, LITERAL_CALL),
                statements, gaps,
                new SemanticProductTargetModel.CoverageSummary(12, 9, 2, 1, 0, summary));
    }

    private static SemanticProductTargetModel.DataDeclaration declaration(
            SemanticProductTargetModel.DataItemId id, String name, String picture, int line) {
        SemanticProductTargetModel.Readiness readiness = readiness(
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT,
                "declaration surface and nominal identity available",
                SemanticProductTargetModel.ReadinessStatus.NOT_APPLICABLE,
                "declaration alone has no control successor",
                SemanticProductTargetModel.ReadinessStatus.PARTIAL,
                "nominal identity available; storage layout and aliases unknown");
        return new SemanticProductTargetModel.DataDeclaration(id, name, picture,
                provenance(line), SemanticProductTargetModel.CoverageStatus.MODELED, readiness);
    }

    private static SemanticProductTargetModel.MoveFact move(
            SemanticProductTargetModel.StatementId id, int point, String literal,
            SemanticProductTargetModel.DataItemId target, int line,
            SemanticProductTargetModel.Containment containment) {
        SemanticProductTargetModel.Readiness readiness = readiness(
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT,
                "literal source, nominal target and structure available",
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT,
                "structural fallthrough available",
                SemanticProductTargetModel.ReadinessStatus.PARTIAL,
                "nominal DEF available; storage region and alias effects unknown");
        return new SemanticProductTargetModel.MoveFact(
                header(id, SemanticProductTargetModel.StatementKind.MOVE, point, line,
                        containment, SemanticProductTargetModel.CoverageStatus.MODELED, readiness),
                new SemanticProductTargetModel.LiteralSource(operandId(id, 0),
                        SemanticProductTargetModel.LiteralKind.ALPHANUMERIC,
                        literal, provenance(line)),
                reference(id, 1, target, SemanticProductTargetModel.OperandRole.WRITE, line));
    }

    private static SemanticProductTargetModel.CallFact call(
            SemanticProductTargetModel.StatementId id, int point,
            SemanticProductTargetModel.DataItemId operand, int line,
            SemanticProductTargetModel.Containment containment) {
        SemanticProductTargetModel.Readiness readiness = readiness(
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT,
                "variable CALL syntax and nominal operand available",
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT,
                "local structural continuation available",
                SemanticProductTargetModel.ReadinessStatus.PARTIAL,
                "nominal USE available; runtime target and call effects unknown");
        return new SemanticProductTargetModel.CallFact(
                header(id, SemanticProductTargetModel.StatementKind.CALL, point, line,
                        containment, SemanticProductTargetModel.CoverageStatus.MODELED, readiness),
                SemanticProductTargetModel.CallSyntax.IDENTIFIER_OR_EXPRESSION,
                reference(id, 0, operand,
                        SemanticProductTargetModel.OperandRole.CALL_TARGET, line),
                SemanticProductTargetModel.RuntimeTargetKnowledge.UNKNOWN,
                "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN");
    }

    private static SemanticProductTargetModel.IfFact branch(
            SemanticProductTargetModel.StatementId id, int point, String surface,
            String objectValue, int line,
            SemanticProductTargetModel.Containment containment,
            List<SemanticProductTargetModel.StatementId> thenChildren,
            List<SemanticProductTargetModel.StatementId> elseChildren,
            SemanticProductTargetModel.StatementId continuation) {
        SemanticProductTargetModel.Readiness readiness = readiness(
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT,
                "condition surface, branches and continuation available",
                SemanticProductTargetModel.ReadinessStatus.SUFFICIENT,
                "two conservative successors and structural join are reconstructible",
                SemanticProductTargetModel.ReadinessStatus.PARTIAL,
                "condition read available; predicate semantics remain partial");
        return new SemanticProductTargetModel.IfFact(
                header(id, SemanticProductTargetModel.StatementKind.IF, point, line,
                        containment, SemanticProductTargetModel.CoverageStatus.PARTIAL, readiness),
                new SemanticProductTargetModel.ConditionFact(surface,
                        reference(id, 0, FLAG, SemanticProductTargetModel.OperandRole.READ, line),
                        SemanticProductTargetModel.RelationalOperator.EQUALS,
                        new SemanticProductTargetModel.LiteralSource(operandId(id, 1),
                                SemanticProductTargetModel.LiteralKind.NUMERIC,
                                objectValue, provenance(line)),
                        provenance(line)),
                thenChildren, elseChildren, continuation);
    }

    private static SemanticProductTargetModel.UnsupportedStatementFact unsupportedLiteralCall() {
        SemanticProductTargetModel.Readiness blocked = readiness(
                SemanticProductTargetModel.ReadinessStatus.BLOCKED,
                "literal CALL is outside the initial CALL capability",
                SemanticProductTargetModel.ReadinessStatus.BLOCKED,
                "no control claim for the unsupported shape",
                SemanticProductTargetModel.ReadinessStatus.BLOCKED,
                "no effect claim for the unsupported shape");
        return new SemanticProductTargetModel.UnsupportedStatementFact(
                header(LITERAL_CALL, SemanticProductTargetModel.StatementKind.CALL, 11, 24,
                        SemanticProductTargetModel.Containment.root(),
                        SemanticProductTargetModel.CoverageStatus.UNSUPPORTED, blocked),
                "CALL_LITERAL_PROGRAM_NAME", "CALL_LITERAL_OUTSIDE_INITIAL_CAPABILITY");
    }

    private static SemanticProductTargetModel.StatementHeader header(
            SemanticProductTargetModel.StatementId id,
            SemanticProductTargetModel.StatementKind kind,
            int point, int line, SemanticProductTargetModel.Containment containment,
            SemanticProductTargetModel.CoverageStatus coverage,
            SemanticProductTargetModel.Readiness readiness) {
        return new SemanticProductTargetModel.StatementHeader(id, kind,
                new SemanticProductTargetModel.ProgramPoint(point), containment,
                provenance(line), coverage, readiness);
    }

    private static SemanticProductTargetModel.DataReference reference(
            SemanticProductTargetModel.StatementId statement, int localOperandId,
            SemanticProductTargetModel.DataItemId data,
            SemanticProductTargetModel.OperandRole role, int line) {
        String name = data.equals(WS_X) ? "WS-X" : data.equals(FLAG) ? "FLAG" : "AUX-PGM";
        return new SemanticProductTargetModel.DataReference(
                operandId(statement, localOperandId), data, role,
                SemanticProductTargetModel.NominalBinding.resolved(data, name),
                provenance(line));
    }

    private static SemanticProductTargetModel.Gap gap(
            SemanticProductTargetModel.StatementId statement,
            SemanticProductTargetModel.GapScope scope, String code, int line) {
        return new SemanticProductTargetModel.Gap(statement, scope, code,
                "target oracle preserves this unavailable semantic dimension",
                provenance(line));
    }

    private static SemanticProductTargetModel.Readiness readiness(
            SemanticProductTargetModel.ReadinessStatus lowering, String loweringScope,
            SemanticProductTargetModel.ReadinessStatus cfg, String cfgScope,
            SemanticProductTargetModel.ReadinessStatus effects, String effectsScope) {
        return new SemanticProductTargetModel.Readiness(
                new SemanticProductTargetModel.ReadinessClaim(lowering, loweringScope),
                new SemanticProductTargetModel.ReadinessClaim(cfg, cfgScope),
                new SemanticProductTargetModel.ReadinessClaim(effects, effectsScope));
    }

    private static SemanticProductTargetConsumer.MoveInput move(
            SemanticProductTargetConsumer.LoweringReadinessOutline outline, String literal) {
        return outline.moves().stream().filter(move -> move.literal().equals(literal))
                .findFirst().orElseThrow();
    }

    private static SemanticProductTargetModel.Containment childOf(
            SemanticProductTargetModel.StatementId parent,
            SemanticProductTargetModel.Branch branch) {
        return SemanticProductTargetModel.Containment.childOf(parent, branch);
    }

    private static SemanticProductTargetModel.Provenance provenance(int line) {
        SemanticProductTargetModel.Location expanded =
                new SemanticProductTargetModel.Location("<preprocessed>", line, 0, line, 40);
        SemanticProductTargetModel.Location original =
                new SemanticProductTargetModel.Location(SOURCE_NAME, line, 7, line, 40);
        return new SemanticProductTargetModel.Provenance(
                expanded, original, List.of(), true);
    }

    private static SemanticProductTargetModel.DataItemId dataId(int localId) {
        return new SemanticProductTargetModel.DataItemId(UNIT, localId);
    }

    private static SemanticProductTargetModel.StatementId statementId(int localId) {
        return new SemanticProductTargetModel.StatementId(UNIT, localId);
    }

    private static SemanticProductTargetModel.OperandId operandId(
            SemanticProductTargetModel.StatementId statement, int localId) {
        return new SemanticProductTargetModel.OperandId(statement, localId);
    }
}
