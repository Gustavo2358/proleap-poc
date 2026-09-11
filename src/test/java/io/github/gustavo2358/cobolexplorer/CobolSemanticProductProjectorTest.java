package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Production projection coverage for WORK-SEMANTIC-PRODUCT-002 Checkpoints 3 to 5. */
class CobolSemanticProductProjectorTest {
    private static final String SOURCE_NAME = "semantic-product-projection.cbl";
    private static final String MULTIPLE_SOURCE = String.join("\n",
            "       IDENTIFICATION DIVISION.",
            "       PROGRAM-ID. SEMANTIC-PROJECTION.",
            "       DATA DIVISION.",
            "       WORKING-STORAGE SECTION.",
            "       01 WS-A PIC X(8).",
            "       01 WS-B PIC X(8).",
            "       01 WS-N PIC 9.",
            "       01 UNUSED PIC X.",
            "       PROCEDURE DIVISION.",
            "           MOVE 'A' TO WS-A.",
            "           CALL WS-B.",
            "           MOVE 'B' TO WS-B.",
            "           CALL WS-A.",
            "           MOVE 1 TO WS-N.",
            "           CALL WS-B.",
            "           GOBACK.",
            "       END PROGRAM SEMANTIC-PROJECTION.", "");
    private static final String SINGLE_SOURCE = String.join("\n",
            "       IDENTIFICATION DIVISION.",
            "       PROGRAM-ID. SEMANTIC-SINGLE.",
            "       DATA DIVISION.",
            "       WORKING-STORAGE SECTION.",
            "       01 WS-PGM PIC X(8).",
            "       PROCEDURE DIVISION.",
            "           MOVE 'PGMA' TO WS-PGM.",
            "           CALL WS-PGM.",
            "           GOBACK.",
            "       END PROGRAM SEMANTIC-SINGLE.", "");

    @Test
    void projectsEverySupportedDataMoveAndCallWithoutPairing() {
        AstBoundaryTestSupport.Analysis frontend = analyze(MULTIPLE_SOURCE, SOURCE_NAME);
        CobolSemanticProduct.State state = project(frontend);
        CobolSemanticPort port = CobolSemanticProductProjector.open(
                products(frontend), unit(frontend));

        assertEquals(List.of("WS-A", "WS-B", "WS-N", "UNUSED"),
                port.dataDeclarations().stream()
                .map(CobolSemanticProduct.DataDeclaration::canonicalName).toList());
        assertEquals(List.of(Optional.of("X(8)"), Optional.of("X(8)"), Optional.of("9"),
                        Optional.of("X")),
                port.dataDeclarations().stream()
                        .map(CobolSemanticProduct.DataDeclaration::picture).toList());
        assertEquals(7, port.statements().size());
        assertEquals(3, port.moves().size());
        assertEquals(3, port.calls().size());
        assertTrue(port.observedStatements().isEmpty());
        assertInstanceOf(CobolSemanticProduct.GobackFact.class, port.statements().get(6));

        Map<CobolSemanticProduct.DataItemId, String> names = new LinkedHashMap<>();
        port.dataDeclarations().forEach(declaration ->
                names.put(declaration.id(), declaration.canonicalName()));
        assertEquals(List.of("WS-A", "WS-B", "WS-N"), port.moves().stream()
                .map(move -> names.get(move.target().binding().selected().orElseThrow()))
                .toList());
        assertEquals(List.of("WS-B", "WS-A", "WS-B"), port.calls().stream()
                .map(call -> names.get(((CobolSemanticProduct.DataReference) call.target()).binding().selected().orElseThrow()))
                .toList(), "CALL facts must not depend on a MOVE pair or source proximity");
        assertEquals(List.of("A", "B", "1"), port.moves().stream()
                .map(move -> move.source().value()).toList());

        assertEquals(7, new HashSet<>(port.statements().stream()
                .map(statement -> statement.header().id()).toList()).size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), port.statements().stream()
                .map(statement -> statement.header().point().ordinal()).toList());
        assertTrue(port.statements().stream().allMatch(statement ->
                statement.header().id().unit().equals(port.unit())));
        assertTrue(port.moves().stream().allMatch(move ->
                move.target().role() == CobolSemanticProduct.OperandRole.WRITE
                        && move.target().binding().status()
                        == CobolSemanticProduct.ResolutionStatus.RESOLVED));
        assertTrue(port.calls().stream().allMatch(call ->
                ((CobolSemanticProduct.DataReference) call.target()).role() == CobolSemanticProduct.OperandRole.CALL_TARGET
                        && ((CobolSemanticProduct.DataReference) call.target()).binding().status()
                        == CobolSemanticProduct.ResolutionStatus.RESOLVED
                        && call.runtimeTarget()
                        == CobolSemanticProduct.RuntimeTargetKnowledge.UNKNOWN));

        assertEquals(CobolSemanticProduct.InventoryStatus.COMPLETE,
                state.coverage().inventoryStatus());
        assertEquals(7, state.coverage().observedStatements());
        assertEquals(6, state.coverage().modeledStatements());
        assertEquals(1, state.coverage().partialStatements());
        assertEquals(0, state.coverage().unsupportedStatements());
        assertEquals(CobolSemanticProduct.ReadinessStatus.PARTIAL,
                state.coverage().readiness().lowering().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                state.coverage().readiness().cfg().status());
        assertEquals(CobolSemanticProduct.ReadinessStatus.BLOCKED,
                state.coverage().readiness().effectsDataflow().status());
        assertEquals(state, portState(port));

        assertTrue(port.dataDeclarations().stream().allMatch(declaration ->
                declaration.provenance().original().file().equals(SOURCE_NAME)
                        && declaration.provenance().exact()));
        assertTrue(port.statements().stream().allMatch(statement ->
                statement.header().provenance().original().file().equals(SOURCE_NAME)
                        && statement.header().provenance().exact()));
    }

    @Test
    void historicalCardinalityOneCaseRemainsARegression() {
        AstBoundaryTestSupport.Analysis frontend = analyze(
                SINGLE_SOURCE, "semantic-product-single.cbl");
        CobolSemanticPort port = CobolSemanticPort.open(project(frontend));

        assertEquals(1, port.dataDeclarations().size());
        assertEquals(1, port.moves().size());
        assertEquals(1, port.calls().size());
        assertEquals("PGMA", port.moves().get(0).source().value());
        assertEquals(port.moves().get(0).target().binding().selected(),
                ((CobolSemanticProduct.DataReference) port.calls().get(0).target()).binding().selected());
    }

    @Test
    void targetFixtureProjectsEveryIfAndRefinesOnlyCanonicalBranchContainment()
            throws IOException {
        Path fixture = Path.of(
                "src/test/resources/cobol/semantic/semantic-product-lowering-readiness.cbl");
        AstBoundaryTestSupport.Analysis frontend = analyze(
                Files.readString(fixture, StandardCharsets.UTF_8), fixture.getFileName().toString());
        CobolSemanticPort port = CobolSemanticPort.open(project(frontend));

        assertEquals(7, port.moves().size());
        assertEquals(3, port.calls().size());
        assertEquals(3, port.ifs().size());
        assertEquals(14, port.statements().size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                port.statements().stream()
                        .map(statement -> statement.header().point().ordinal()).toList());
        assertEquals(List.of(0, 1, 2, 4, 5, 6, 9), port.moves().stream()
                .map(move -> move.header().id().localId()).toList(),
                "adding IF facts must retain the CP3 MOVE identities");
        assertEquals(List.of(3, 7, 8), port.calls().stream()
                .map(call -> call.header().id().localId()).toList(),
                "adding IF facts must retain the CP3 CALL identities");
        CobolSemanticProduct.StatementId outer = statementId(port, 10);
        CobolSemanticProduct.StatementId nested = statementId(port, 11);
        CobolSemanticProduct.StatementId emptyFalseBranch = statementId(port, 12);
        assertEquals(List.of(outer, nested, emptyFalseBranch), port.ifs().stream()
                .map(branch -> branch.header().id()).toList());
        CobolSemanticProduct.StatementId display = statementId(port, 13);
        assertEquals(List.of(statementId(port, 0), statementId(port, 1), outer,
                        statementId(port, 7), statementId(port, 8), emptyFalseBranch,
                        display),
                port.rootStatements());

        assertEquals(List.of(statementId(port, 2), nested, statementId(port, 5)),
                ids(port.children(outer, CobolSemanticProduct.Branch.THEN)));
        assertEquals(List.of(statementId(port, 6)),
                ids(port.children(outer, CobolSemanticProduct.Branch.ELSE)));
        assertEquals(List.of(statementId(port, 3)),
                ids(port.children(nested, CobolSemanticProduct.Branch.THEN)));
        assertEquals(List.of(statementId(port, 4)),
                ids(port.children(nested, CobolSemanticProduct.Branch.ELSE)));
        assertEquals(List.of(statementId(port, 9)),
                ids(port.children(emptyFalseBranch, CobolSemanticProduct.Branch.THEN)));
        assertTrue(port.children(emptyFalseBranch, CobolSemanticProduct.Branch.ELSE).isEmpty());

        assertEquals(Optional.of(statementId(port, 7)), port.ifs().get(0).continuation());
        assertEquals(Optional.of(statementId(port, 5)), port.ifs().get(1).continuation());
        assertEquals(Optional.of(display), port.ifs().get(2).continuation());
        assertTrue(port.ifs().stream().allMatch(CobolSemanticProduct.IfFact::explicitlyTerminated));
        assertTrue(port.ifs().stream().allMatch(branch ->
                branch.condition().shape().equals("RELATION")
                        && branch.condition().references().size() == 1
                        && branch.condition().references().get(0).role()
                        == CobolSemanticProduct.OperandRole.READ));

        Map<CobolSemanticProduct.DataItemId, String> names = new LinkedHashMap<>();
        port.dataDeclarations().forEach(declaration ->
                names.put(declaration.id(), declaration.canonicalName()));
        assertTrue(port.ifs().stream().allMatch(branch -> names.get(
                branch.condition().references().get(0).binding().selected().orElseThrow())
                .equals("FLAG")));

        List<CobolSemanticProduct.StatementFact> exactChildren = port.statements().stream()
                .filter(statement -> statement.header().containment().branch()
                        == CobolSemanticProduct.Branch.THEN
                        || statement.header().containment().branch()
                        == CobolSemanticProduct.Branch.ELSE)
                .toList();
        assertEquals(7, exactChildren.size());
        assertTrue(port.statements().stream().noneMatch(statement ->
                statement.header().containment().branch()
                        == CobolSemanticProduct.Branch.UNKNOWN));
        assertEquals(0, port.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.STRUCTURE
                        && gap.code().equals("CONTAINMENT_NOT_PROJECTED")).count());
        assertFalse(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(emptyFalseBranch)
                        && gap.scope() == CobolSemanticProduct.GapScope.STRUCTURE
                        && gap.code().equals("CONTINUATION_NOT_PROJECTED")),
                "the observed DISPLAY is now the exact structural continuation");
        assertFalse(port.gaps().stream().anyMatch(gap ->
                gap.code().equals("BRANCH_CONTENT_NOT_PROJECTED")),
                "the fixture has no unprojected direct IF child; its empty ELSE is genuine");
        assertEquals(3, port.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.CONDITION_SEMANTICS
                        && gap.code().equals("CONDITION_SEMANTICS_NOT_AVAILABLE")).count());

        Set<String> ifComponents = new HashSet<>(Arrays.stream(
                CobolSemanticProduct.IfFact.class.getRecordComponents())
                .map(component -> component.getName()).toList());
        assertTrue(ifComponents.contains("explicitlyTerminated"));
        assertFalse(ifComponents.contains("elsePresent"));
        assertFalse(ifComponents.contains("hasElse"));
        assertTrue(ifComponents.stream().noneMatch(Set.of(
                "cfgEdges", "reachability", "truthValue", "branchSelection")::contains));
    }

    @Test
    void ambiguousConditionBindingPreservesCandidatesBranchesAndContinuation() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-IF-AMBIGUOUS.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 FLAG PIC 9.",
                "       01 FLAG PIC 9.",
                "       01 WS-PGM PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           IF FLAG = 1",
                "               MOVE 'PGMA' TO WS-PGM",
                "           END-IF.",
                "           CALL WS-PGM.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-IF-AMBIGUOUS.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-if-ambiguous.cbl")));
        CobolSemanticProduct.IfFact branch = port.ifs().get(0);
        CobolSemanticProduct.NominalBinding binding =
                branch.condition().references().get(0).binding();

        assertEquals(CobolSemanticProduct.ResolutionStatus.AMBIGUOUS, binding.status());
        assertEquals(CobolSemanticProduct.ResolutionReason.MULTIPLE_VALID_CANDIDATES,
                binding.reason());
        assertEquals(2, binding.candidates().size());
        assertTrue(binding.selected().isEmpty());
        assertEquals(List.of(statementId(port, 0)), ids(port.children(
                branch.header().id(), CobolSemanticProduct.Branch.THEN)));
        assertTrue(port.children(branch.header().id(),
                CobolSemanticProduct.Branch.ELSE).isEmpty());
        assertEquals(Optional.of(statementId(port, 1)), branch.continuation());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                branch.header().readiness().cfg().status(),
                "partial predicate binding must not erase reconstructible control structure");
        assertTrue(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(branch.header().id())
                        && gap.scope() == CobolSemanticProduct.GapScope.NOMINAL_BINDING));
        assertFalse(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(branch.header().id())
                        && gap.code().equals("BRANCH_CONTENT_NOT_PROJECTED")),
                "a genuinely empty false branch must not acquire an incompleteness gap");
    }

    @Test
    void directDisplayChildCannotLookLikeAnExactEmptyBranch() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-IF-DISPLAY.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 FLAG PIC 9.",
                "       01 WS-PGM PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           IF FLAG = 1",
                "               DISPLAY 'X'",
                "           END-IF.",
                "           CALL WS-PGM.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-IF-DISPLAY.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-if-display.cbl")));
        CobolSemanticProduct.IfFact branch = port.ifs().get(0);

        assertEquals(1, port.children(branch.header().id(),
                CobolSemanticProduct.Branch.THEN).size());
        assertInstanceOf(CobolSemanticProduct.ObservedStatement.class,
                port.children(branch.header().id(),
                        CobolSemanticProduct.Branch.THEN).get(0));
        assertTrue(port.children(branch.header().id(),
                CobolSemanticProduct.Branch.ELSE).isEmpty());
        assertEquals(Optional.of(port.calls().get(0).header().id()), branch.continuation());
        assertEquals(CobolSemanticProduct.CoverageStatus.PARTIAL,
                branch.header().coverage());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                branch.header().readiness().cfg().status());
        assertFalse(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(branch.header().id())
                        && gap.scope() == CobolSemanticProduct.GapScope.STRUCTURE
                        && gap.code().equals("BRANCH_CONTENT_NOT_PROJECTED")));
    }

    @Test
    void directPerformChildKeepsSupportedBranchFactsButDowngradesCfgReadiness() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-IF-PERFORM.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 FLAG PIC 9.",
                "       01 WS-PGM PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           IF FLAG = 1",
                "               MOVE 'PGMA' TO WS-PGM",
                "               PERFORM WORK-PARA",
                "           END-IF.",
                "           CALL WS-PGM.",
                "           GOBACK.",
                "       WORK-PARA.",
                "           CONTINUE.",
                "       END PROGRAM SEMANTIC-IF-PERFORM.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-if-perform.cbl")));
        CobolSemanticProduct.IfFact branch = port.ifs().get(0);

        assertEquals(List.of(port.moves().get(0).header().id(),
                        observedByKind(port, "PERFORM").header().id()),
                ids(port.children(branch.header().id(), CobolSemanticProduct.Branch.THEN)));
        assertTrue(port.children(branch.header().id(),
                CobolSemanticProduct.Branch.ELSE).isEmpty());
        assertEquals(Optional.of(port.calls().get(0).header().id()), branch.continuation());
        assertEquals(CobolSemanticProduct.ReadinessStatus.SUFFICIENT,
                branch.header().readiness().cfg().status());
        assertFalse(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(branch.header().id())
                        && gap.scope() == CobolSemanticProduct.GapScope.STRUCTURE
                        && gap.code().equals("BRANCH_CONTENT_NOT_PROJECTED")));
    }

    @Test
    void qualifiedConditionPublishesTheValueReadWithoutInventingQualifierReads() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-IF-QUALIFIED.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 GROUP-A.",
                "          05 ITEM PIC X.",
                "       01 WS-PGM PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           IF ITEM OF GROUP-A = 'Y'",
                "               MOVE 'PGMA' TO WS-PGM",
                "           END-IF.",
                "           CALL WS-PGM.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-IF-QUALIFIED.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-if-qualified.cbl")));
        CobolSemanticProduct.IfFact branch = port.ifs().get(0);

        assertEquals(1, branch.condition().references().size());
        assertEquals(CobolSemanticProduct.ResolutionReason.QUALIFIED_HIERARCHY_MATCH,
                branch.condition().references().get(0).binding().reason());
        assertFalse(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(branch.header().id())
                        && gap.code().equals("CONDITION_REFERENCE_KIND_NOT_PROJECTED")));
    }

    @Test
    void ifUnderPerformIsPublishedWithoutRefiningTheUnprojectedParent() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-IF-IN-PERFORM.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 FLAG PIC X.",
                "          88 FLAG-ON VALUE 'Y'.",
                "       01 WS-PGM PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           PERFORM UNTIL FLAG = 'Y'",
                "               IF FLAG-ON",
                "                   MOVE 'PGMA' TO WS-PGM",
                "               END-IF",
                "               CALL WS-PGM",
                "           END-PERFORM.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-IF-IN-PERFORM.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-if-in-perform.cbl")));
        CobolSemanticProduct.IfFact branch = port.ifs().get(0);
        CobolSemanticProduct.StatementId move = port.moves().get(0).header().id();
        CobolSemanticProduct.StatementId call = port.calls().get(0).header().id();

        assertEquals(CobolSemanticProduct.Containment.unknown(),
                branch.header().containment());
        assertEquals(CobolSemanticProduct.Containment.childOf(
                        branch.header().id(), CobolSemanticProduct.Branch.THEN),
                port.statement(move).orElseThrow().header().containment());
        assertEquals(CobolSemanticProduct.Containment.unknown(),
                port.statement(call).orElseThrow().header().containment());
        assertEquals(List.of(move), ids(port.children(
                branch.header().id(), CobolSemanticProduct.Branch.THEN)));
        assertTrue(port.children(branch.header().id(),
                CobolSemanticProduct.Branch.ELSE).isEmpty());
        assertEquals(Optional.of(call), branch.continuation());
        assertTrue(branch.condition().references().isEmpty(),
                "the DATA-only ConditionSurface must not fabricate a condition-name identity");
        assertTrue(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(branch.header().id())
                        && gap.code().equals("CONDITION_REFERENCE_KIND_NOT_PROJECTED")));
        assertTrue(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(branch.header().id())
                        && gap.code().equals("CONTAINMENT_NOT_PROJECTED")));
        assertTrue(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(call)
                        && gap.code().equals("CONTAINMENT_NOT_PROJECTED")));
        assertFalse(port.gaps().stream().anyMatch(gap ->
                gap.statement().equals(move)
                        && gap.code().equals("CONTAINMENT_NOT_PROJECTED")));
    }

    @Test
    void moveAndCallNestedUnderPerformAlsoRemainExplicit() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-PERFORM-NESTED.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-PGM PIC X(8).",
                "       01 DONE PIC X.",
                "       PROCEDURE DIVISION.",
                "           PERFORM UNTIL DONE = 'Y'",
                "               MOVE 'PGMA' TO WS-PGM",
                "               CALL WS-PGM",
                "               MOVE 'Y' TO DONE",
                "           END-PERFORM.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-PERFORM-NESTED.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-perform-nested.cbl")));

        assertEquals(2, port.moves().size());
        assertEquals(1, port.calls().size());
        assertEquals(5, port.statements().size());
        assertEquals(2, port.rootStatements().size());
        assertTrue(port.moves().stream().allMatch(statement ->
                statement.header().containment().equals(
                        CobolSemanticProduct.Containment.unknown())));
        assertTrue(port.calls().stream().allMatch(statement ->
                statement.header().containment().equals(
                        CobolSemanticProduct.Containment.unknown())));
        assertEquals(3, port.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.STRUCTURE
                        && gap.code().equals("CONTAINMENT_NOT_PROJECTED")).count());
    }

    @Test
    void qualifiedHierarchyReasonCrossesTheBoundaryWithoutNormalization() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-QUALIFIED.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 GROUP-A.",
                "          05 ITEM PIC X.",
                "       01 CTRL-GROUP.",
                "          05 PGM PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           MOVE 'X' TO ITEM OF GROUP-A.",
                "           CALL PGM OF CTRL-GROUP.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-QUALIFIED.", "");
        AstBoundaryTestSupport.Analysis frontend = analyze(
                source, "semantic-product-qualified.cbl");
        List<ReferenceResolution.Entry> projectedEntries = frontend.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role()
                        == ResolutionContracts.ReferenceRole.VALUE_WRITE
                        || entry.occurrence().role()
                        == ResolutionContracts.ReferenceRole.CALL_TARGET)
                .toList();
        CobolSemanticPort port = CobolSemanticPort.open(project(frontend));

        assertEquals(2, projectedEntries.size());
        assertTrue(projectedEntries.stream().allMatch(entry -> entry.reason()
                == ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH));
        assertEquals(CobolSemanticProduct.ResolutionReason.QUALIFIED_HIERARCHY_MATCH,
                port.moves().get(0).target().binding().reason());
        assertEquals(CobolSemanticProduct.ResolutionReason.QUALIFIED_HIERARCHY_MATCH,
                ((CobolSemanticProduct.DataReference) port.calls().get(0).target()).binding().reason());
    }

    @Test
    void onlyCanonicalBasicTextLiteralsGainCategory() {
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(MULTIPLE_SOURCE, SOURCE_NAME)));

        assertEquals(List.of(CobolSemanticProduct.LiteralKind.ALPHANUMERIC,
                CobolSemanticProduct.LiteralKind.ALPHANUMERIC, CobolSemanticProduct.LiteralKind.UNKNOWN),
                port.moves().stream().map(move -> move.source().kind()).toList());
        assertEquals(1, port.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.LITERAL_KIND
                        && gap.code().equals("LITERAL_KIND_NOT_PUBLISHED")).count());
    }

    @Test
    void shapesOutsideTheCapabilityRemainObservedInsteadOfDisappearing() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-UNSUPPORTED.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-A PIC X(8).",
                "       01 WS-B PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           MOVE WS-A TO WS-B.",
                "           CALL 'PGMA'.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-UNSUPPORTED.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-unsupported.cbl")));

        assertTrue(port.moves().isEmpty());
        assertEquals(1, port.calls().size());
        assertEquals("PGMA", ((CobolSemanticProduct.LiteralCallTarget) port.calls().get(0).target()).text());
        assertEquals(List.of("MOVE_NON_LITERAL_SOURCE"),
                port.observedStatements().stream()
                        .map(CobolSemanticProduct.ObservedStatement::observedShape).toList());
        assertEquals(List.of("MOVE_NON_LITERAL_SOURCE_OUTSIDE_CAPABILITY"),
                port.observedStatements().stream()
                        .map(CobolSemanticProduct.ObservedStatement::gapCode).toList());
        assertTrue(port.observedStatements().stream().allMatch(observed ->
                observed.header().coverage() == CobolSemanticProduct.CoverageStatus.UNSUPPORTED));
        assertEquals(1, port.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.CAPABILITY
                        && gap.code().endsWith("OUTSIDE_CAPABILITY")).count());
    }

    @Test
    void callUncertaintiesComeFromTheCanonicalReport() {
        AstBoundaryTestSupport.Analysis frontend = analyze(MULTIPLE_SOURCE, SOURCE_NAME);
        CobolSemanticProduct.State state = project(frontend);
        List<String> reportDetails = frontend.report().gaps().stream()
                .filter(gap -> gap.category() == ResolutionAnalysisReport.GapCategory.CALL_SEMANTICS)
                .filter(gap -> gap.code().equals("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"))
                .map(ResolutionAnalysisReport.Gap::message).toList();
        List<String> projectedDetails = state.gaps().stream()
                .filter(gap -> gap.scope() == CobolSemanticProduct.GapScope.RUNTIME_CALL_TARGET)
                .map(CobolSemanticProduct.Gap::detail).toList();

        assertEquals(3, reportDetails.size());
        assertEquals(reportDetails, projectedDetails);
        assertEquals(frontend.report().policy().policyId(), state.policy().policyId());
        assertEquals(frontend.report().policy().version(), state.policy().version());
    }

    @Test
    void ambiguousBindingsPreserveEveryCanonicalCandidateWithoutSelection() {
        String source = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-AMBIGUOUS.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-PGM PIC X(8).",
                "       01 WS-PGM PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           MOVE 'A' TO WS-PGM.",
                "           CALL WS-PGM.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-AMBIGUOUS.", "");
        CobolSemanticPort port = CobolSemanticPort.open(project(
                analyze(source, "semantic-product-ambiguous.cbl")));

        assertEquals(2, port.dataDeclarations().size());
        assertEquals(CobolSemanticProduct.ResolutionStatus.AMBIGUOUS,
                port.moves().get(0).target().binding().status());
        assertEquals(CobolSemanticProduct.ResolutionStatus.AMBIGUOUS,
                ((CobolSemanticProduct.DataReference) port.calls().get(0).target()).binding().status());
        assertEquals(2, port.moves().get(0).target().binding().candidates().size());
        assertEquals(2, ((CobolSemanticProduct.DataReference) port.calls().get(0).target()).binding().candidates().size());
        assertTrue(port.moves().get(0).target().binding().selected().isEmpty());
        assertTrue(((CobolSemanticProduct.DataReference) port.calls().get(0).target()).binding().selected().isEmpty());
        assertEquals(2, port.gaps().stream().filter(gap ->
                gap.scope() == CobolSemanticProduct.GapScope.NOMINAL_BINDING).count());
    }

    @Test
    void mismatchedReportFailsClosedInsteadOfReclassifyingFacts() {
        AstBoundaryTestSupport.Analysis frontend = analyze(MULTIPLE_SOURCE, SOURCE_NAME);
        AstBoundaryTestSupport.Analysis other = analyze(
                MULTIPLE_SOURCE.replace("SEMANTIC-PROJECTION", "SEMANTIC-OTHER"),
                "semantic-product-other.cbl");
        CobolSemanticProductProjector.FrontendProducts mismatched =
                new CobolSemanticProductProjector.FrontendProducts(
                        frontend.build(), frontend.tables(), frontend.occurrences(),
                        frontend.resolution(), other.report(),
                ScalarMoveSemantics.analyze(frontend.build(), frontend.tables(), frontend.resolution(), other.report()));

        assertThrows(IllegalArgumentException.class,
                () -> CobolSemanticProductProjector.project(mismatched, unit(frontend)));
    }

    @Test
    void canonicalNamespaceMismatchFailsInsteadOfJoiningByName() {
        AstBoundaryTestSupport.Analysis frontend = analyze(MULTIPLE_SOURCE, SOURCE_NAME);
        ReferenceResolution.Entry originalCall = null;
        for (ReferenceResolution.Entry entry : frontend.resolution().entries()) {
            if (entry.occurrence().role() == ResolutionContracts.ReferenceRole.CALL_TARGET) {
                originalCall = entry;
                break;
            }
        }
        if (originalCall == null) throw new AssertionError("fixture must contain a CALL target");
        ReferenceResolution.Candidate originalCandidate =
                originalCall.selectedCandidate().orElseThrow();
        ResolutionContracts.ProgramUnitId foreignUnit = new ResolutionContracts.ProgramUnitId(
                "OTHER.CBL", List.of(0), originalCandidate.canonicalName());
        ReferenceResolution.Candidate foreignCandidate = new ReferenceResolution.Candidate(
                new ResolutionContracts.SemanticEntityId(foreignUnit,
                        ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                        originalCandidate.entityId().localId()),
                originalCandidate.kind(), originalCandidate.writtenName(),
                originalCandidate.canonicalName(), originalCandidate.declarationSymbolIds(),
                originalCandidate.attributes());
        ReferenceResolution.Entry forgedCall = new ReferenceResolution.Entry(
                originalCall.id(), originalCall.occurrence(), originalCall.status(),
                originalCall.reason(), List.of(foreignCandidate), originalCall.diagnosticIds(),
                originalCall.callSemantics());
        List<ReferenceResolution.Entry> entries = new ArrayList<>(frontend.resolution().entries());
        entries.set(originalCall.id(), forgedCall);
        ReferenceResolution forgedResolution = new ReferenceResolution(
                frontend.resolution().policy(), entries, frontend.resolution().diagnostics(),
                frontend.resolution().metrics(), frontend.resolution().declarationRelations());
        CobolSemanticProductProjector.FrontendProducts forged =
                new CobolSemanticProductProjector.FrontendProducts(
                        frontend.build(), frontend.tables(), frontend.occurrences(),
                        forgedResolution, frontend.report(),
                ScalarMoveSemantics.analyze(frontend.build(), frontend.tables(), forgedResolution, frontend.report()));

        assertThrows(IllegalArgumentException.class,
                () -> CobolSemanticProductProjector.project(forged, unit(frontend)));
    }

    @Test
    void projectionDoesNotReparseOrDependOnPresentationOrAnalysisEngines()
            throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/projection/"
                        + "CobolSemanticProductProjector.java"), StandardCharsets.UTF_8);

        assertFalse(source.contains("findFirst"));
        assertFalse(source.contains("findLast"));
        assertFalse(source.contains("single("));
        // Written spelling is forwarded as metadata only, never inspected for semantics.
        assertFalse(source.replace("literal.writtenText(),", "").contains("writtenText"));
        assertFalse(source.contains("rawLexeme"));
        assertFalse(source.contains("grammarRule"));
        assertFalse(source.contains("SourceMap"));
        assertFalse(source.contains("Snapshot"));
        assertFalse(source.contains("ExplorerMain"));
        assertFalse(source.contains("AstBuilder"));
        assertFalse(source.contains("CobolReferenceResolver"));
        assertFalse(source.contains("ReferenceOccurrenceCollector"));
        assertFalse(source.contains("org.antlr"));
    }

    private static AstBoundaryTestSupport.Analysis analyze(String source, String sourceName) {
        return AstBoundaryTestSupport.analyze(source, sourceName);
    }

    private static ResolutionContracts.ProgramUnitId unit(
            AstBoundaryTestSupport.Analysis frontend) {
        return frontend.model().programUnits().get(0).id();
    }

    private static CobolSemanticProduct.State project(
            AstBoundaryTestSupport.Analysis frontend) {
        return CobolSemanticProductProjector.project(products(frontend), unit(frontend));
    }

    private static CobolSemanticProductProjector.FrontendProducts products(
            AstBoundaryTestSupport.Analysis frontend) {
        return new CobolSemanticProductProjector.FrontendProducts(
                frontend.build(), frontend.tables(), frontend.occurrences(),
                frontend.resolution(), frontend.report(),
                ScalarMoveSemantics.analyze(frontend.build(), frontend.tables(), frontend.resolution(), frontend.report()));
    }

    private static CobolSemanticProduct.State portState(CobolSemanticPort port) {
        return new CobolSemanticProduct.State(port.unit(), port.policy(),
                port.dataDeclarations(), port.statements(), port.gaps(), port.coverage(), port.entryInventory());
    }

    private static CobolSemanticProduct.StatementId statementId(
            CobolSemanticPort port, int localId) {
        return new CobolSemanticProduct.StatementId(port.unit(), localId);
    }

    private static List<CobolSemanticProduct.StatementId> ids(
            List<CobolSemanticProduct.StatementFact> facts) {
        return facts.stream().map(fact -> fact.header().id()).toList();
    }

    private static CobolSemanticProduct.ObservedStatement observedByKind(
            CobolSemanticPort port, String kind) {
        CobolSemanticProduct.ObservedStatement result = null;
        for (CobolSemanticProduct.ObservedStatement observed : port.observedStatements()) {
            if (!observed.observedKind().equals(kind)) continue;
            if (result != null)
                throw new AssertionError("fixture has multiple observed statements of kind " + kind);
            result = observed;
        }
        if (result == null)
            throw new AssertionError("fixture has no observed statement of kind " + kind);
        return result;
    }
}
