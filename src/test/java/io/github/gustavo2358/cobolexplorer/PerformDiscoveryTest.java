package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Discovery against unchanged canonical products; no production PERFORM code. */
class PerformDiscoveryTest {
    static String source(String before, String perform, String body) {
        return "IDENTIFICATION DIVISION.\nPROGRAM-ID. CALLER.\nDATA DIVISION.\n"
            + "WORKING-STORAGE SECTION.\n01 WS-A PIC X(8).\n01 WS-PGM PIC X(8).\n"
            + "PROCEDURE DIVISION.\nMAIN.\n" + before + perform
            + "\nCALL WS-PGM.\nGOBACK.\nDEFINE-PGM.\n" + body + "\n";
    }
    static String simple() { return source("", "PERFORM DEFINE-PGM.", "MOVE 'PROGA' TO WS-PGM."); }
    @Test void canonicalInputsProveIsolatedParagraphAndUniqueResume() {
        var a = AstBoundaryTestSupport.analyze(simple(), "perform.cbl");
        var unit = a.model().programUnits().get(0);
        var division = unit.program().divisions().stream().filter(d -> d.divisionKind() == Ast.DivisionKind.PROCEDURE).findFirst().orElseThrow();
        var perform = AstBoundaryTestSupport.nodes(a, Ast.PerformStatement.class).get(0);
        assertEquals(Ast.PerformKind.PROCEDURE, perform.performKind());
        assertNull(perform.throughReference()); assertTrue(perform.controls().isEmpty());
        assertTrue(perform.inlineBody().isEmpty());
        assertEquals(Optional.of(perform.meta().id()), division.procedureEntry().orElseThrow().startStatementId());
        assertFalse(division.procedureEntry().orElseThrow().declarativesPresent());
        var reference = a.resolution().entries().stream().filter(e -> e.occurrence().role() == ResolutionContracts.ReferenceRole.PERFORM_FROM).findFirst().orElseThrow();
        assertEquals(perform.fromReference().meta().id(), reference.occurrence().referenceAstNodeId());
        assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, reference.status());
        var candidate = reference.selectedCandidate().orElseThrow();
        var symbol = a.tables().forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols().stream()
            .filter(s -> s.id() == candidate.entityId().localId()).findFirst().orElseThrow();
        assertEquals(SymbolTable.SymbolKind.PARAGRAPH, symbol.kind());
        var paragraphs = AstBoundaryTestSupport.nodes(a, Ast.Paragraph.class);
        var target = paragraphs.stream().filter(p -> p.meta().id() == symbol.declarationAstNodeId()).findFirst().orElseThrow();
        var primary = paragraphs.stream().filter(p -> p.sentences().stream().flatMap(s -> s.statements().stream()).anyMatch(s -> s == perform)).findFirst().orElseThrow();
        assertNotSame(primary, target);
        var primaryBody = primary.sentences().stream().flatMap(s -> s.statements().stream()).toList();
        assertEquals(List.of(Ast.PerformStatement.class, Ast.CallStatement.class, Ast.GobackStatement.class), primaryBody.stream().map(Object::getClass).toList());
        assertEquals(1, target.sentences().size());
        assertInstanceOf(Ast.MoveStatement.class, target.sentences().get(0).statements().get(0));
        assertEquals(2, paragraphs.size());
        assertEquals(1, AstBoundaryTestSupport.nodes(a, Ast.PerformStatement.class).size());
        assertTrue(AstBoundaryTestSupport.nodes(a, Ast.GoToStatement.class).isEmpty());
        // Normal return belongs to this activation; body ends remain intrinsic.
        assertEquals(primaryBody.get(1).meta().id(), division.normalContinuations().get(perform.meta().id()));
        assertEquals(primaryBody.get(2).meta().id(), division.normalContinuations().get(primaryBody.get(1).meta().id()));
    }
    @Test void everyGrammarLoopControlHasTypedControlExpressions() {
        for (String control : List.of("5 TIMES", "UNTIL WS-A = 'X'", "WITH TEST AFTER UNTIL WS-A = 'X'",
                "VARYING WS-A FROM 1 BY 1 UNTIL WS-A = 5")) {
            var a = AstBoundaryTestSupport.analyze(source("", "PERFORM DEFINE-PGM " + control + ".", "MOVE 'PROGA' TO WS-PGM."), "control.cbl");
            assertFalse(AstBoundaryTestSupport.nodes(a, Ast.PerformStatement.class).get(0).controls().isEmpty(), control);
        }
    }
}
