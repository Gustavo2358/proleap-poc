package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExternalClassificationProductTest {
    @Test
    void copiesCollectionsAndRejectsInvalidOrOverlappingIdentities() {
        ResolutionContracts.ProgramUnitId unit = new ResolutionContracts.ProgramUnitId(
                "UNIT", List.of(0), "PROGRAM");
        List<Integer> covered = new ArrayList<>(List.of(3, 4));
        ExternalClassification.Entry entry = entry(0, unit, 10, 3, covered);
        covered.add(5);
        ExternalClassification product = new ExternalClassification(List.of(entry));

        assertAll("immutable identity-bearing product",
                () -> assertEquals(List.of(3, 4), entry.coveredOccurrenceIds()),
                () -> assertEquals(ExternalClassification.InputCompleteness.COMPLETE,
                        entry.inputCompleteness()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> product.entries().add(entry)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> entry(0, unit, 10, 3, List.of(4))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> entry(0, unit, 10, 3, List.of(4, 3))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ExternalClassification(List.of(
                                entry(0, unit, 10, 3, List.of(3)),
                                entry(1, unit, 11, 3, List.of(3))))));
    }

    private static ExternalClassification.Entry entry(
            int id, ResolutionContracts.ProgramUnitId unit, int rootAstNodeId,
            int rootOccurrenceId, List<Integer> covered) {
        Ast.SourceSpan span = new Ast.SourceSpan(1, 0, 1, 10, 0, 1);
        Ast.SourceLocation location = new Ast.SourceLocation("fixture.cbl", 1, 0, 1, 10);
        Ast.Meta meta = new Ast.Meta(rootAstNodeId, span,
                new Ast.ParseTreeOrigin(0, "tableCall", 2),
                new Ast.SourceProvenance(location, location, List.of(), true));
        return new ExternalClassification.Entry(id, unit, rootAstNodeId, rootOccurrenceId,
                "DFHRESP(ARG)", ExternalClassification.Technology.CICS,
                ExternalClassification.Kind.POSSIBLE_INTRINSIC,
                ExternalClassification.Certainty.INFERRED,
                ExternalClassification.Reason.COBOL_REFERENCE_UNRESOLVED_WITH_KNOWN_CICS_SHAPE,
                meta, covered);
    }
}
