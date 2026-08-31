package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExternalCicsCharacterizationTest {
    private static final Path POSSIBLE = Path.of(
            "src/test/resources/cobol/semantic/external-cics-possible.cbl");
    private static final Path TABLES = Path.of(
            "src/test/resources/cobol/semantic/external-cics-cobol-precedence.cbl");
    private static final Path DATA_ITEMS = Path.of(
            "src/test/resources/cobol/semantic/external-cics-cobol-data-items.cbl");

    @Test
    void preservesPossibleCicsHostsAsStructuredTableCallsWithRootAndChildBindingEntries() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(POSSIBLE);
        CompilationUnitModel.ProgramUnit unit = analysis.model().programUnits().get(0);

        for (String name : List.of("DFHRESP", "DFHVALUE")) {
            Ast.DataReference root = references(unit).stream()
                    .filter(reference -> reference.baseName().equals(name))
                    .findFirst().orElseThrow();
            assertAll(name,
                    () -> assertEquals("tableCall", root.meta().origin().grammarRule()),
                    () -> assertTrue(root.qualifiers().isEmpty()),
                    () -> assertEquals(1, root.subscriptGroups().size()),
                    () -> assertEquals(1, root.subscriptGroups().get(0).subscripts().size()),
                    () -> assertNull(root.referenceModification()),
                    () -> assertInstanceOf(Ast.DataReference.class,
                            root.subscriptGroups().get(0).subscripts().get(0)));

            Ast.DataReference argument = (Ast.DataReference)
                    root.subscriptGroups().get(0).subscripts().get(0);
            ReferenceResolution.Entry rootEntry = entryForAstNode(analysis, unit.id(), root.meta().id());
            ReferenceResolution.Entry argumentEntry = entryForAstNode(analysis, unit.id(), argument.meta().id());
            assertAll("root and child occurrences remain distinct and traceable",
                    () -> assertNotEquals(rootEntry.occurrence().id(), argumentEntry.occurrence().id()),
                    () -> assertEquals(ResolutionContracts.ReferenceRole.SUBSCRIPT,
                            argumentEntry.occurrence().role()),
                    () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, rootEntry.status()),
                    () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, argumentEntry.status()),
                    () -> assertEquals(root.meta(), rootEntry.occurrence().meta()),
                    () -> assertEquals(argument.meta(), argumentEntry.occurrence().meta()));
        }
    }

    @Test
    void resolvesDeclaredDfhrespAndDfhvalueAsOrdinaryCobolDataItems() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(DATA_ITEMS);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();

        for (String name : List.of("DFHRESP", "DFHVALUE")) {
            ReferenceResolution.Entry entry = analysis.resolution().find(
                    unit, name, ResolutionContracts.ReferenceRole.VALUE_READ).get(0);
            assertAll(name,
                    () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                    () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                            entry.selectedCandidate().orElseThrow().kind()));
        }
    }

    @Test
    void resolvesDeclaredDfhrespAndDfhvalueTableCallsAndTheirSubscriptsAsCobol() throws Exception {
        ExternalClassificationTestSupport.Analysis analysis =
                ExternalClassificationTestSupport.analyze(TABLES);
        CompilationUnitModel.ProgramUnit unit = analysis.model().programUnits().get(0);

        for (String name : List.of("DFHRESP", "DFHVALUE")) {
            Ast.DataReference root = references(unit).stream()
                    .filter(reference -> reference.baseName().equals(name))
                    .findFirst().orElseThrow();
            Ast.DataReference subscript = (Ast.DataReference)
                    root.subscriptGroups().get(0).subscripts().get(0);
            assertAll(name,
                    () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                            entryForAstNode(analysis, unit.id(), root.meta().id()).status()),
                    () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                            entryForAstNode(analysis, unit.id(), subscript.meta().id()).status()));
        }
    }

    private static List<Ast.DataReference> references(CompilationUnitModel.ProgramUnit unit) {
        return ExternalClassificationTestSupport.nodes(unit.program()).stream()
                .filter(Ast.DataReference.class::isInstance)
                .map(Ast.DataReference.class::cast)
                .toList();
    }

    private static ReferenceResolution.Entry entryForAstNode(
            ExternalClassificationTestSupport.Analysis analysis,
            ResolutionContracts.ProgramUnitId unitId, int astNodeId) {
        return analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().programUnitId().equals(unitId)
                        && entry.occurrence().referenceAstNodeId() == astNodeId)
                .findFirst().orElseThrow();
    }
}
