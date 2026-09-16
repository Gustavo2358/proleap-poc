package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.DeclarativeValueInferenceTest.*;

/** EP laws: source support and proof of precision are separate obligations. */
class EvidencePreservationTest {
    private static final List<Integer> PROGA = List.of(215,217,214,199,193,64,64,64);

    @Test void missingPhysicalEncodingDoesNotEraseLogicalSourceSupport() {
        var a=AstBoundaryTestSupport.analyze(source(VALUE,"CALL LIT-PGM."),"no-layout-profile.cbl");
        var tables=new CompilationUnitSymbolTableBuilder().build(a.model());
        var layout=StorageLayoutSemantics.analyze(a.build(),tables,a.resolution(),a.report(),StorageLayoutSemantics.Profile.UNSPECIFIED);
        var condition=StorageAccessSemantics.analyze(a.build(),a.resolution(),layout).initial().facts(a.model().programUnits().get(0).id()).conditions().get(0);
        assertEquals("POSSIBLE_LOGICAL_TEXT",condition.kind().name(),"a logical VALUE does not require inventing a byte codec");
        assertEquals(Optional.of("PROGA   "),condition.logicalText());
        assertTrue(condition.bytes().isEmpty(),"no invented encoding");
        assertFalse(condition.reasons().isEmpty());
    }

    @Test void preservedDeclarationCannotEraseIndependentSourceEvidence() {
        for (var declaration : List.of(
                "77 PARTIAL-AREA PIC S9(9) COMP SYNC.",
                "77 PARTIAL-AREA PIC S9(9) COMP SYNCHRONIZED.",
                "77 PARTIAL-AREA PIC X(8) JUSTIFIED.",
                "77 PARTIAL-AREA PIC 9(8) BLANK WHEN ZERO.",
                "77 PARTIAL-AREA PIC S9(8) SIGN LEADING SEPARATE.")) {
            var p = product(VALUE + declaration + "\n01 ARG PIC X(20).\n", "CALL LIT-PGM USING ARG.");
            var c = condition(p);
            assertEquals(PROGA, c.bytes(), declaration + ": " + c.gapCodes());
            assertEquals("POSSIBLE_LITERAL_BYTES", c.kind().name());
            assertEquals("DECLARATIVE_POSSIBILITY", c.proof().name());
            assertFalse(c.gapCodes().isEmpty());
        }
    }

    @Test void unknownBaseExtentCannotEraseKnownDeclarationValue() {
        var c = condition(product("01 WS-AREA.\n05 LIT-PGM PIC X(8) VALUE 'PROGA'.\n05 PARTIAL-AREA PIC S9(9) COMP.\n", "CALL LIT-PGM."));
        assertEquals(PROGA, c.bytes(), c.gapCodes().toString());
        assertEquals("POSSIBLE_LITERAL_BYTES", c.kind().name());
        assertFalse(c.gapCodes().isEmpty());
    }

    @Test void unknownOffsetCannotEraseKnownDeclarationValue() {
        var p = product("01 WS-AREA.\n05 PARTIAL-AREA PIC S9(9) COMP.\n05 LIT-PGM PIC X(8) VALUE 'PROGA'.\n", "CALL LIT-PGM.");
        var c = condition(p);
        assertEquals(PROGA, c.bytes(), c.gapCodes().toString());
        assertEquals("POSSIBLE_LITERAL_BYTES", c.kind().name());
        assertFalse(c.gapCodes().isEmpty());
        var call=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.DataReference)p.calls().get(0).target();
        assertEquals(call.binding().selected(),call.logicalWholeItem(),"whole logical access is independent of physical layout");
        assertTrue(call.regionalAccess().isEmpty());
    }

    @Test void logicalWholeItemDoesNotInventASliceOrArrayRead() {
        for(var target:List.of("LIT-PGM(1:3)","LIT-PGM(1)")) {
            var p=product(VALUE,"CALL "+target+".");
            var ref=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.DataReference)p.calls().get(0).target();
            assertTrue(ref.logicalWholeItem().isEmpty(),target);
        }
    }

    @Test void unknownStatementAndCallOperandsPreserveSourceEvidence() {
        for (var code : List.of("EXHIBIT LIT-PGM.\nCALL LIT-PGM.",
                "DISPLAY LIT-PGM.\nCALL LIT-PGM USING MISSING-ARG.",
                "CALL 'OTHER' RETURNING LIT-PGM.\nCALL LIT-PGM.")) {
            var c = condition(product(VALUE, code));
            assertEquals(PROGA, c.bytes(), code);
            assertEquals("POSSIBLE_LITERAL_BYTES", c.kind().name());
        }
    }

    @Test void sourceEvidenceRequiresAnActualValidLiteral() {
        assertTrue(product(VALUE.replace(" VALUE 'PROGA'", ""), "CALL LIT-PGM.").storage().entryState().conditions().isEmpty());
        assertTrue(condition(product("01 LIT-PGM PIC X(2) VALUE 'TOO-LONG'.\n", "CALL LIT-PGM.")).bytes().isEmpty());
    }

    @Test void repeatedParentDoesNotTurnAnElementValueIntoAWholeObjectValue() {
        var c=condition(product("01 TABLE-AREA OCCURS 2.\n05 LIT-PGM PIC X(8) VALUE 'PROGA'.\n","CALL LIT-PGM."));
        assertEquals("UNKNOWN",c.kind().name(),"positive multiplicity does not support a scalar whole-object entry fact");
        assertTrue(c.bytes().isEmpty());assertTrue(c.logicalText().isEmpty());
    }

    @Test void lifecycleProfileCannotDecideWhetherSourcePossibilityExists() {
        var c=StorageInitialTest.initial(VALUE,StorageInitialSemantics.EntryMode.PRESERVED).conditions().get(0);
        assertEquals(PROGA,c.bytes(),"preserving previous storage does not prove the declared value impossible");
        assertEquals(StorageInitialSemantics.Kind.POSSIBLE_LITERAL_BYTES,c.kind());
        assertEquals(StorageInitialSemantics.Proof.DECLARATIVE_POSSIBILITY,c.proof());
    }

    @Test void initialLifecycleDoesNotUpgradeUnprovedAllocation() {
        var c=StorageInitialTest.initial(VALUE+"77 PARTIAL-AREA PIC X(8) JUSTIFIED.\n",StorageInitialSemantics.EntryMode.INITIAL).conditions().get(0);
        assertEquals(PROGA,c.bytes());
        assertEquals(StorageInitialSemantics.Kind.POSSIBLE_LITERAL_BYTES,c.kind(),"entry lifecycle alone does not close layout/storage proof");
    }

    @Test void sourceEvidenceDoesNotClaimLifetimeInvarianceAcrossMustWrite() {
        var p = product(VALUE, "MOVE 'PROGB' TO LIT-PGM.\nCALL LIT-PGM.");
        assertEquals(PROGA, condition(p).bytes());
        assertEquals("DECLARATIVE_POSSIBILITY", condition(p).proof().name());
        assertEquals(1, p.moves().size());
    }

    @Test void futureArbitraryClauseHasNoAuthorityToEraseSupportedValue() {
        // Build a typed partial node at the AST boundary. JOHNDOE has no grammar or keyword semantics.
        var original = AstBoundaryTestSupport.analyze(source(VALUE + "77 PARTIAL-AREA PIC X(3) USAGE DISPLAY.\n", "CALL LIT-PGM."), "future-construct.cbl");
        int replaced = AstBoundaryTestSupport.nodes(original, Ast.UsageClause.class).get(0).meta().id();
        var model = new CompilationUnitModel(original.model().compilationUnitId(), original.model().programUnits().stream()
                .map(u -> new CompilationUnitModel.ProgramUnit(u.id(), u.parentId(), (Ast.Program) replace(u.program(), replaced))).toList());
        var coverage = new LinkedHashMap<ResolutionContracts.ProgramUnitId, SemanticCoverage.Report>();
        original.build().coverageByProgramUnit().forEach((unit, report) -> coverage.put(unit, new SemanticCoverage.Report(report.findings().stream()
                .map(f -> f.astNodeId() == replaced ? new SemanticCoverage.Finding(f.id(), "futureArbitraryClause", f.meta(), "JOHNDOE", SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED, f.dependencyKnowledge(), "EP synthetic unknown construction", f.astNodeId()) : f).toList())));
        var build = new CompilationUnitBuildResult(model, coverage, original.build().diagnosticsByProgramUnit());
        var tables = new CompilationUnitSymbolTableBuilder().build(model);
        var components=StorageComponents.analyze(build).unit(model.programUnits().get(0).id());
        var clause=components.uncertainties().stream().filter(u->u.reason()==StorageComponents.Reason.UNINTERPRETED_DATA_CLAUSE).toList();
        assertEquals(2,clause.size(),"generic clause retains both its layout owner and unbounded allocation remainder");
        assertTrue(clause.stream().allMatch(u->u.origin().exact()));
        assertEquals(Set.of(StorageComponents.UncertaintyScope.DECLARATION,StorageComponents.UncertaintyScope.UNIT),
            clause.stream().map(StorageComponents.Uncertainty::scope).collect(java.util.stream.Collectors.toSet()));
        var layout = StorageLayoutSemantics.analyze(build, tables, original.resolution(), original.report(), StorageLayoutSemantics.Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);
        var facts = StorageAccessSemantics.analyze(build, original.resolution(), layout).initial().facts(model.programUnits().get(0).id());
        var c = facts.conditions().get(0);
        assertEquals(PROGA, c.bytes(), "JOHNDOE is not a MUST write: " + c.reasons());
        assertEquals(StorageInitialSemantics.Kind.POSSIBLE_LITERAL_BYTES, c.kind());
        assertTrue(layout.layout(model.programUnits().get(0).id()).nodes().stream().anyMatch(n -> n.extent().value().isEmpty()), "unknown must remain explicit");
    }

    private static Ast.Node replace(Ast.Node node, int replaced) {
        if (node.meta().id() == replaced) return new Ast.PreservedDataClause(node.meta(), "futureArbitraryClause", "JOHNDOE", List.of());
        if (node instanceof Ast.Program p) return new Ast.Program(p.meta(), p.name(), p.attributes(), p.divisions().stream().map(d -> (Ast.Division) replace(d, replaced)).toList(), p.inputProof());
        if (node instanceof Ast.Division d) return new Ast.Division(d.meta(), d.divisionKind(), d.children().stream().map(n -> replace(n, replaced)).toList(), d.procedureEntry(), d.normalContinuations(), d.ordinaryContinuations(), d.embeddedContinuations(), d.embeddedOrdinaryContinuations());
        if (node instanceof Ast.Section s) return new Ast.Section(s.meta(), s.name(), s.dataSectionKind(), s.children().stream().map(n -> replace(n, replaced)).toList());
        if (node instanceof Ast.DataEntry d) return new Ast.DataEntry(d.meta(), d.level(), d.levelKind(), d.name(), d.filler(), d.visibility(), d.declaration(), d.clauses().stream().map(n -> (Ast.DataClause) replace(n, replaced)).toList(), d.children().stream().map(n -> (Ast.DataEntry) replace(n, replaced)).toList());
        return node;
    }
}
