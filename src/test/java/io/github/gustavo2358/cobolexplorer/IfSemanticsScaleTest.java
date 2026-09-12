package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IfSemanticsScaleTest {
    record Counts(int n, long nodes, long declarations, long references, long armMembers, long lookups, long completionVisits) { }
    private static Counts measure(int n) throws Exception {
        StringBuilder source = new StringBuilder("IDENTIFICATION DIVISION.\nPROGRAM-ID. SCALE-IF.\nDATA DIVISION.\nWORKING-STORAGE SECTION.\n01 FLAG PIC X.\n");
        for (int i = 0; i < n; i++) source.append("01 WS-").append(i).append(" PIC X(8).\n");
        source.append("PROCEDURE DIVISION.\n");
        for (int i = 0; i < n; i++) source.append("IF FLAG = 'Y'\n MOVE 'PROGA' TO WS-").append(i)
                .append("\nELSE\n MOVE 'PROGB' TO WS-").append(i).append("\nEND-IF\n");
        source.append("CALL WS-0.\nGOBACK.\n");
        var a = AstBoundaryTestSupport.analyze(source.toString(), "scale-if.cbl");
        var product = ScalarMoveCheckpoint4ATest.products(a);
        var canonical = product.scalarMoves().ifs(); var m = canonical.metrics();
        assertEquals(n + 1, canonical.storage(a.model().programUnits().get(0).id()).members().size());
        var port = io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector.open(product, a.model().programUnits().get(0).id());
        assertEquals(n, port.ifs().size()); assertEquals(2 * n, port.moves().size());
        assertTrue(port.ifs().stream().allMatch(f -> f.profile() == io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.IfProfile.SIMPLE_TEXT_EQUALITY));
        // Measure the actual typed parser-region algorithm, separately from output cardinalities.
        var normalized = SourceNormalizer.normalize(source.toString().lines().map(l -> "       " + l).collect(java.util.stream.Collectors.joining("\n")), "scale-if.cbl", SourceNormalizer.SourceFormat.FIXED);
        var binding = Bindings.cobol(); var parser = binding.cobolParser(new CommonTokenStream(binding.cobolLexer(CharStreams.fromString(normalized.text()))));
        ParseTree tree = binding.cobolStart(parser); assertEquals(0, parser.getNumberOfSyntaxErrors());
        var builder = new AstBuilder(parser, normalized.text(), normalized.sourceMap(), new IdentityHashMap<>(), new IdentityHashMap<>());
        builder.buildCompilationUnit(tree, "scale-if.cbl");
        assertEquals(3L * n + 2, builder.completionStatementVisits());
        return new Counts(n, m.nodeVisits(), m.declarationVisits(), m.referenceVisits(), m.armMemberVisits(), m.scalarLookups(), builder.completionStatementVisits());
    }
    @Test void nAnd2nUseLinearWorkNotTimingThresholds() throws Exception {
        var n = measure(64); var twice = measure(128);
        assertEquals(2 * n.armMembers(), twice.armMembers());
        assertTrue(twice.nodes() <= 2 * n.nodes()); assertTrue(twice.declarations() <= 2 * n.declarations());
        assertTrue(twice.references() <= 2 * n.references()); assertTrue(twice.lookups() <= 2 * n.lookups());
        assertTrue(twice.completionVisits() <= 2 * n.completionVisits());
        var out = Path.of("target/cp6-w2a/work-counts.json"); Files.createDirectories(out.getParent());
        new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out.toFile(), List.of(n, twice));
        System.out.println("W2A work counts " + n + " / " + twice);
    }
}
