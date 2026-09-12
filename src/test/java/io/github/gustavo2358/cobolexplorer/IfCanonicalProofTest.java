package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.function.UnaryOperator;
import static org.junit.jupiter.api.Assertions.*;

class IfCanonicalProofTest {
    private static Object rewrite(Object value, UnaryOperator<Ast.Meta> meta) throws Exception {
        if (value instanceof Ast.Meta m) return meta.apply(m);
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>(); for (var item : list) result.add(rewrite(item, meta)); return result;
        }
        if (value instanceof Optional<?> optional) return optional.isEmpty() ? optional : Optional.of(rewrite(optional.get(), meta));
        if (!(value instanceof Record record) || record.getClass().getEnclosingClass() != Ast.class) return value;
        var components = record.getClass().getRecordComponents(); var arguments = new Object[components.length]; var types = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) { types[i] = components[i].getType(); arguments[i] = rewrite(components[i].getAccessor().invoke(record), meta); }
        return record.getClass().getDeclaredConstructor(types).newInstance(arguments);
    }
    private static CompilationUnitBuildResult build(AstBoundaryTestSupport.Analysis a, Ast.Program program,
            Map<ResolutionContracts.ProgramUnitId, SemanticCoverage.Report> coverage) {
        var u = a.model().programUnits().get(0);
        var model = new CompilationUnitModel(a.model().compilationUnitId(), List.of(new CompilationUnitModel.ProgramUnit(u.id(), u.parentId(), program)));
        return new CompilationUnitBuildResult(model, coverage, a.build().diagnosticsByProgramUnit());
    }
    @Test void missingProvenanceAtEachProofInputFailsClosed() throws Exception {
        var a = AstBoundaryTestSupport.analyze(IfCheckpointW2ATest.fixture("closed"), "origin.cbl");
        var unit = a.model().programUnits().get(0); var branch = AstBoundaryTestSupport.nodes(a, Ast.IfStatement.class).get(0);
        var condition = (Ast.RelationCondition) branch.condition();
        for (var node : List.of(branch, condition, condition.subject(), condition.object(),
                AstBoundaryTestSupport.nodes(a, Ast.DataEntry.class).get(0), AstBoundaryTestSupport.nodes(a, Ast.PictureClause.class).get(0))) {
            var modified = (Ast.Program) rewrite(unit.program(), m -> {
                if (m.id() != node.meta().id()) return m;
                var origin = m.provenance();
                return new Ast.Meta(m.id(), m.span(), m.origin(), new Ast.SourceProvenance(origin.expanded(), origin.original(), origin.includeChain(), false));
            });
            var b = build(a, modified, a.build().coverageByProgramUnit());
            var proof = ScalarMoveSemantics.analyze(b, a.tables(), a.resolution(), a.report()).ifs();
            assertNotEquals(IfSemantics.Availability.KNOWN, proof.fact(unit.id(), branch.meta().id()).predicate().availability(), node.getClass().getSimpleName());
            if (node instanceof Ast.DataEntry || node instanceof Ast.PictureClause)
                assertNotEquals(IfSemantics.Availability.KNOWN, proof.storage(unit.id()).availability());
        }
    }
    @Test void partialDeclarationAndStatementCoverageDoNotBecomeProof() throws Exception {
        var a = AstBoundaryTestSupport.analyze(IfCheckpointW2ATest.fixture("closed"), "partial.cbl");
        var unit = a.model().programUnits().get(0); var branch = AstBoundaryTestSupport.nodes(a, Ast.IfStatement.class).get(0);
        for (var node : List.of(branch, AstBoundaryTestSupport.nodes(a, Ast.DataEntry.class).get(0),
                AstBoundaryTestSupport.nodes(a, Ast.PictureClause.class).get(0), AstBoundaryTestSupport.nodes(a, Ast.MoveStatement.class).get(0))) {
            var findings = a.build().coverageByProgramUnit().get(unit.id()).findings().stream().map(f -> f.astNodeId() != node.meta().id() ? f
                    : new SemanticCoverage.Finding(f.id(), f.grammarRule(), f.meta(), f.writtenText(), SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED,
                            SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN, "test partial canonical input", f.astNodeId())).toList();
            var b = build(a, unit.program(), Map.of(unit.id(), new SemanticCoverage.Report(findings)));
            var proof = ScalarMoveSemantics.analyze(b, a.tables(), a.resolution(), a.report()).ifs();
            assertFalse(proof.fact(unit.id(), branch.meta().id()).simpleProfile());
            if (!(node instanceof Ast.MoveStatement)) assertNotEquals(IfSemantics.Availability.KNOWN, proof.fact(unit.id(), branch.meta().id()).predicate().availability());
            if (node instanceof Ast.DataEntry || node instanceof Ast.PictureClause) assertNotEquals(IfSemantics.Availability.KNOWN, proof.storage(unit.id()).availability());
        }
    }
    @Test void missingCanonicalCompletionIsNotReconstructed() throws Exception {
        var a = AstBoundaryTestSupport.analyze(IfCheckpointW2ATest.fixture("closed"), "no-completion.cbl");
        var unit = a.model().programUnits().get(0); var p = unit.program();
        var program = new Ast.Program(p.meta(), p.name(), p.attributes(), p.divisions().stream()
                .map(d -> new Ast.Division(d.meta(), d.divisionKind(), d.children(), d.procedureEntry())).toList());
        var b = build(a, program, a.build().coverageByProgramUnit());
        var canonical = ScalarMoveSemantics.analyze(b, a.tables(), a.resolution(), a.report());
        var port = io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.open(
                new io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.FrontendProducts(
                        b, a.tables(), a.occurrences(), a.resolution(), a.report(), canonical), unit.id());
        assertTrue(port.ifs().get(0).normalContinuation().statement().isEmpty());
        assertTrue(port.moves().stream().allMatch(m -> m.normalContinuation().statement().isEmpty()));
    }
}
