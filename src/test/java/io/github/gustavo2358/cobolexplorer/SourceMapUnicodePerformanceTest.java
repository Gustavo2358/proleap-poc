package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceMapUnicodePerformanceTest {

    @Test
    void identityAndLocationsUseCodePointOffsetsAcrossPhysicalLineEndings() {
        String text = "A😀\r\nBÁ\rC中\nD";
        SourceMap map = SourceMap.identity(text, "unicode.cbl");
        UnicodeText indexed = new UnicodeText(text);

        int c = indexed.indexOf("C", 0);
        Ast.SourceProvenance provenance = map.provenance(c, c + 2);
        assertEquals(3, provenance.original().startLine());
        assertEquals(0, provenance.original().startColumn());
        assertEquals(1, provenance.original().endColumn());
        assertTrue(provenance.exact());
    }

    @Test
    void batchReplacementMatchesSequentialComposition() {
        SourceMap source = segmentedMap(40);
        SourceMap replacement = SourceMap.identity("YY😀", "replacement.cpy");
        List<SourceMap.Replacement> edits = new ArrayList<>();
        SourceMap sequential = source;
        for (int offset = 38; offset >= 0; offset -= 2) {
            edits.add(new SourceMap.Replacement(offset, offset + 1, replacement));
            sequential = sequential.replace(offset, offset + 1, replacement);
        }

        SourceMap batch = source.replaceAll(edits);
        assertEquals(sequential.text(), batch.text());
        for (int offset : List.of(0, 1, 10, 20, 38, 39, 60, 79)) {
            assertEquals(sequential.provenance(offset, offset + 1),
                    batch.provenance(offset, offset + 1));
        }
    }

    @Test
    void literalReplacementUsesCodePointRangesAndPreservesOrigin() {
        SourceMap source = SourceMap.identity("😀 OLD and OLD", "copy.cpy");
        SourceMap replaced = source.replaceLiteral("OLD", "AÇÃO 😀");

        assertEquals("😀 AÇÃO 😀 and AÇÃO 😀", replaced.text());
        UnicodeText indexed = new UnicodeText(replaced.text());
        int second = indexed.indexOf("AÇÃO", indexed.indexOf("and", 0));
        Ast.SourceProvenance provenance = replaced.provenance(second, second + 4);
        assertEquals(10, provenance.original().startColumn());
        assertFalse(provenance.exact());
    }

    @Test
    void largeSegmentLookupAndBatchCompositionStaySubquadratic() {
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            int size = 20_000;
            SourceMap source = segmentedMap(size);
            for (int query = 0; query < 50_000; query++) {
                Ast.SourceProvenance provenance = source.provenance(size - 1, size);
                assertEquals(size * 2 - 2, provenance.original().startColumn());
            }

            SourceMap replacement = SourceMap.identity("y", "replacement.cpy");
            List<SourceMap.Replacement> edits = new ArrayList<>(size);
            for (int offset = 0; offset < size; offset++) {
                edits.add(new SourceMap.Replacement(offset, offset + 1, replacement));
            }
            assertEquals("y".repeat(size), source.replaceAll(edits).text());
        });
    }

    @Test
    void largeUnicodeNormalizationBuildsItsMapInLinearTime() {
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            int lines = 20_000;
            String raw = "       DISPLAY '😀'.\n".repeat(lines);
            SourceNormalizer.Result result = SourceNormalizer.normalize(
                    raw, "large-unicode.cbl", SourceNormalizer.SourceFormat.FIXED);
            UnicodeText indexed = new UnicodeText(result.text());
            int lastDisplay = indexed.indexOf("DISPLAY", indexed.length() - 20);
            Ast.SourceProvenance provenance = result.sourceMap()
                    .provenance(lastDisplay, lastDisplay + "DISPLAY".length());
            assertEquals(lines, provenance.original().startLine());
            assertEquals(7, provenance.original().startColumn());
        });
    }

    private static SourceMap segmentedMap(int size) {
        String text = "x".repeat(size);
        String original = "x ".repeat(size);
        List<SourceMap.Segment> segments = new ArrayList<>(size);
        for (int offset = 0; offset < size; offset++) {
            segments.add(new SourceMap.Segment(offset, offset + 1, "large.cbl",
                    offset * 2, offset * 2 + 1, List.of(), true));
        }
        return SourceMap.mapped(text, "large.cbl", original, segments);
    }
}
