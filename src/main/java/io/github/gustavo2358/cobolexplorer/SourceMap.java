package io.github.gustavo2358.cobolexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, segment-based mapping from preprocessed text back to COBOL source files. */
final class SourceMap {
    record Segment(int start, int end, String sourceFile, int originalStart, int originalEnd,
                   List<Ast.CopyFrame> includeChain, boolean exact, Diagnostic inputGap) {
        Segment {
            includeChain = List.copyOf(includeChain);
            if (start < 0 || end < start) throw new IllegalArgumentException("invalid expanded range");
        }

        Segment(int start, int end, String sourceFile, int originalStart, int originalEnd,
                List<Ast.CopyFrame> includeChain, boolean exact) {
            this(start, end, sourceFile, originalStart, originalEnd, includeChain, exact, null);
        }

        Segment shifted(int delta) {
            return new Segment(start + delta, end + delta, sourceFile, originalStart, originalEnd,
                    includeChain, exact, inputGap);
        }

        Segment clipped(int from, int to, int destinationStart) {
            int clippedStart = Math.max(start, from);
            int clippedEnd = Math.min(end, to);
            int originalClippedStart = exact ? originalStart + clippedStart - start : originalStart;
            int originalClippedEnd = exact ? originalStart + clippedEnd - start : originalEnd;
            return new Segment(destinationStart + clippedStart - from,
                    destinationStart + clippedEnd - from, sourceFile,
                    originalClippedStart, originalClippedEnd, includeChain, exact, inputGap);
        }
    }

    record Replacement(int start, int end, SourceMap sourceMap) {
        Replacement {
            Objects.requireNonNull(sourceMap, "sourceMap");
            if (start < 0 || end < start) throw new IllegalArgumentException("invalid replacement range");
        }
    }

    /** Physical source boundary, not a token or an exact textual provenance. */
    record PhysicalBoundary(String sourceFile, int originalOffset) { }

    private final PhysicalBoundary physicalBoundary;
    private final String text;
    private final UnicodeText indexedText;
    private final List<Segment> segments;
    private final Map<String, UnicodeText> sources;
    // Refinement of retained text inside an opaque envelope; ordinary provenance is unchanged.
    private final List<Segment> retainedSegments;

    private SourceMap(String text, List<Segment> segments, Map<String, UnicodeText> sources,
                      PhysicalBoundary physicalBoundary) {
        this(text, segments, sources, physicalBoundary, segments);
    }

    private SourceMap(String text, List<Segment> segments, Map<String, UnicodeText> sources,
                      PhysicalBoundary physicalBoundary, List<Segment> retainedSegments) {
        this.retainedSegments = List.copyOf(retainedSegments);
        this.physicalBoundary = physicalBoundary;
        this.text = Objects.requireNonNull(text);
        this.indexedText = new UnicodeText(text);
        this.segments = List.copyOf(segments);
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
    }

    static SourceMap identity(String text, String file) {
        UnicodeText indexed = new UnicodeText(text);
        List<Segment> segments = text.isEmpty() ? List.of()
                : List.of(new Segment(0, indexed.length(), file, 0, indexed.length(), List.of(), true));
        return new SourceMap(text, segments, Map.of(file, indexed), new PhysicalBoundary(file, indexed.length()));
    }

    static SourceMap mapped(String text, String file, String original, List<Segment> segments) {
        Objects.requireNonNull(file, "file");
        UnicodeText indexedText = new UnicodeText(text);
        UnicodeText indexedOriginal = new UnicodeText(Objects.requireNonNull(original, "original"));
        int previousEnd = 0;
        for (Segment segment : segments) {
            if (segment.start() < previousEnd || segment.end() > indexedText.length()) {
                throw new IllegalArgumentException("invalid or overlapping mapped segment");
            }
            if (!segment.sourceFile().equals(file)) {
                throw new IllegalArgumentException("mapped segment belongs to another source file");
            }
            if (segment.originalStart() < 0 || segment.originalEnd() < segment.originalStart()
                    || segment.originalEnd() > indexedOriginal.length()) {
                throw new IllegalArgumentException("invalid original segment range");
            }
            if (segment.exact()
                    && segment.end() - segment.start() != segment.originalEnd() - segment.originalStart()) {
                throw new IllegalArgumentException("exact mapped segment changes length");
            }
            previousEnd = segment.end();
        }
        return new SourceMap(text, mergeAdjacent(segments), Map.of(file, indexedOriginal),
                new PhysicalBoundary(file, indexedOriginal.length()));
    }

    String text() { return text; }

    int length() { return indexedText.length(); }

    Optional<PhysicalBoundary> physicalBoundary() { return Optional.ofNullable(physicalBoundary); }

    SourceMap replace(int start, int end, SourceMap replacement) {
        return replaceAll(List.of(new Replacement(start, end, replacement)));
    }

    SourceMap replaceAll(List<Replacement> replacements) {
        if (replacements.isEmpty()) return this;
        List<Replacement> ordered = replacements.stream()
                .sorted(Comparator.comparingInt(Replacement::start)).toList();
        StringBuilder nextText = new StringBuilder(text.length());
        List<Segment> next = new ArrayList<>();
        List<Segment> nextRetained = new ArrayList<>();
        Map<String, UnicodeText> nextSources = new LinkedHashMap<>(sources);
        int cursor = 0;
        int destination = 0;
        for (Replacement replacement : ordered) {
            if (replacement.start() < cursor || replacement.end() > indexedText.length()) {
                throw new IllegalArgumentException("invalid or overlapping replacement range");
            }
            nextText.append(indexedText.substring(cursor, replacement.start()));
            addSlice(next, cursor, replacement.start(), destination);
            addRetainedSlice(nextRetained, cursor, replacement.start(), destination);
            destination += replacement.start() - cursor;
            nextText.append(replacement.sourceMap().text);
            for (Segment segment : replacement.sourceMap().segments) next.add(segment.shifted(destination));
            for (Segment segment : replacement.sourceMap().retainedSegments) nextRetained.add(segment.shifted(destination));
            destination += replacement.sourceMap().indexedText.length();
            replacement.sourceMap().sources.forEach(nextSources::putIfAbsent);
            cursor = replacement.end();
        }
        nextText.append(indexedText.substring(cursor, indexedText.length()));
        addSlice(next, cursor, indexedText.length(), destination);
        addRetainedSlice(nextRetained, cursor, indexedText.length(), destination);
        return new SourceMap(nextText.toString(), mergeAdjacent(next), nextSources, physicalBoundary,
                mergeAdjacent(nextRetained));
    }

    SourceMap transformedSlice(int start, int end, String replacementText) {
        Ast.SourceProvenance origin = provenance(start, end);
        UnicodeText replacement = new UnicodeText(replacementText);
        Segment segment = replacementText.isEmpty() ? null : new Segment(0, replacement.length(),
                origin.original().file(), offset(origin.original().file(), origin.original().startLine(),
                origin.original().startColumn()), offsetAfter(origin.original()), origin.includeChain(), false);
        return new SourceMap(replacementText, segment == null ? List.of() : List.of(segment), sources, null);
    }

    /** Preserve the coordinates of retained runs while framing/flattening an opaque payload.
     * The ordinary map still describes the complete transformation as approximate. */
    SourceMap framedEmbeddedSlice(int start, int end, int payloadEnd, String prefix, String suffix) {
        int from = start, to = payloadEnd;
        var points = indexedText.substring(start, payloadEnd).codePoints().toArray();
        int left = 0, right = points.length;
        while (left < right && Character.isWhitespace(points[left])) left++;
        while (right > left && Character.isWhitespace(points[right - 1])) right--;
        from += left; to = start + right;
        var text = new StringBuilder(prefix);
        var retained = new ArrayList<Segment>();
        int destination = prefix.codePointCount(0, prefix.length());
        for (int cursor = from; cursor < to;) {
            int run = cursor;
            boolean lineBreak = points[cursor - start] == '\r' || points[cursor - start] == '\n';
            while (cursor < to && (points[cursor - start] == '\r' || points[cursor - start] == '\n') == lineBreak) cursor++;
            if (lineBreak) {
                text.append(' ');
                for (var segment : transformedSlice(run, cursor, " ").segments)
                    retained.add(segment.shifted(destination));
                destination++;
            } else {
                text.append(indexedText.substring(run, cursor));
                addRetainedSlice(retained, run, cursor, destination);
                destination += cursor - run;
            }
        }
        text.append(suffix);
        var coarse = transformedSlice(start, end, text.toString());
        var complete = new ArrayList<Segment>();
        coarse.addSlice(complete, 0, prefix.codePointCount(0, prefix.length()), 0);
        complete.addAll(retained);
        coarse.addSlice(complete, destination, coarse.length(), destination);
        return new SourceMap(coarse.text, coarse.segments, sources, null, mergeAdjacent(complete));
    }

    /** Operand coordinates from retained source; an opaque envelope stays approximate. */
    Ast.SourceProvenance embeddedOperandProvenance(int start, int end) {
        var p = provenance(start, end, retainedSegments);
        return new Ast.SourceProvenance(p.expanded(), p.original(), p.includeChain(), false);
    }

    private void addRetainedSlice(List<Segment> target, int from, int to, int destination) {
        addSlice(retainedSegments, target, from, to, destination);
    }

    SourceMap withInputGap(Diagnostic diagnostic) {
        if (diagnostic.code() != Diagnostic.Code.UNRESOLVED_COPY)
            throw new IllegalArgumentException("only missing COPY regions are qualified");
        return new SourceMap(text, segments.stream().map(s -> new Segment(s.start(), s.end(),
                s.sourceFile(), s.originalStart(), s.originalEnd(), s.includeChain(), s.exact(), diagnostic)).toList(), sources, physicalBoundary);
    }

    List<Segment> inputGapRegions() {
        return segments.stream().filter(s -> s.inputGap() != null).toList();
    }

    SourceMap replaceLiteral(String from, String to) {
        if (from.isEmpty()) return this;
        int fromLength = from.codePointCount(0, from.length());
        int searchFrom = 0;
        List<Replacement> replacements = new ArrayList<>();
        while (true) {
            int index = indexedText.indexOf(from, searchFrom);
            if (index < 0) break;
            replacements.add(new Replacement(index, index + fromLength,
                    transformedSlice(index, index + fromLength, to)));
            searchFrom = index + fromLength;
        }
        return replaceAll(replacements);
    }

    SourceMap withCopyFrame(Ast.CopyFrame frame) {
        List<Segment> framed = segments.stream().map(segment -> {
            List<Ast.CopyFrame> chain = new ArrayList<>();
            chain.add(frame);
            chain.addAll(segment.includeChain());
            return new Segment(segment.start(), segment.end(), segment.sourceFile(), segment.originalStart(),
                    segment.originalEnd(), chain, segment.exact(), segment.inputGap());
        }).toList();
        List<Segment> retained = retainedSegments.stream().map(segment -> {
            List<Ast.CopyFrame> chain = new ArrayList<>();
            chain.add(frame); chain.addAll(segment.includeChain());
            return new Segment(segment.start(), segment.end(), segment.sourceFile(), segment.originalStart(),
                    segment.originalEnd(), chain, segment.exact(), segment.inputGap());
        }).toList();
        return new SourceMap(text, framed, sources, null, retained);
    }

    Ast.SourceProvenance provenance(int start, int end) {
        return provenance(start, end, segments);
    }

    private Ast.SourceProvenance provenance(int start, int end, List<Segment> selected) {
        int safeStart = Math.max(0, Math.min(start, indexedText.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, indexedText.length()));
        Segment first = segmentAt(selected, safeStart, safeEnd == safeStart);
        Segment last = segmentAt(selected, Math.max(safeStart, safeEnd - 1), safeEnd == safeStart);
        Ast.SourceLocation expanded = location("<preprocessed>", indexedText, safeStart, safeEnd);
        if (first == null) return new Ast.SourceProvenance(expanded,
                new Ast.SourceLocation("<unknown>", 0, 0, 0, 0), List.of(), false);
        boolean compatible = last != null && first.sourceFile().equals(last.sourceFile())
                && first.includeChain().equals(last.includeChain());
        int originalStart = first.originalStart() + (first.exact() ? safeStart - first.start() : 0);
        int originalEnd = compatible
                ? last.originalStart() + (last.exact() ? safeEnd - last.start() : last.originalEnd() - last.originalStart())
                : first.originalEnd();
        UnicodeText originalText = sources.get(first.sourceFile());
        if (originalText == null) originalText = new UnicodeText("");
        Ast.SourceLocation original = location(first.sourceFile(), originalText, originalStart, originalEnd);
        boolean exact = compatible && allOverlappingExact(selected, safeStart, safeEnd);
        return new Ast.SourceProvenance(expanded, original, first.includeChain(), exact);
    }

    private static boolean allOverlappingExact(List<Segment> segments, int start, int end) {
        int index = firstOverlapping(segments, start);
        while (index < segments.size() && segments.get(index).start() < end) {
            if (!segments.get(index).exact()) return false;
            index++;
        }
        return true;
    }

    private static Segment segmentAt(List<Segment> segments, int offset, boolean empty) {
        int insertion = firstStartingAtOrAfter(segments, offset);
        if (empty && insertion > 0) {
            Segment previous = segments.get(insertion - 1);
            if (previous.end() == offset) return previous;
        }
        int index = insertion < segments.size() && segments.get(insertion).start() == offset
                ? insertion : insertion - 1;
        if (index < 0 || index >= segments.size()) return null;
        Segment segment = segments.get(index);
        return offset >= segment.start() && offset < segment.end() ? segment : null;
    }

    private static int firstStartingAtOrAfter(List<Segment> segments, int offset) {
        int low = 0, high = segments.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (segments.get(middle).start() < offset) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static int firstOverlapping(List<Segment> segments, int offset) {
        int low = 0, high = segments.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (segments.get(middle).end() <= offset) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private void addSlice(List<Segment> target, int from, int to, int destinationStart) {
        addSlice(segments, target, from, to, destinationStart);
    }

    private static void addSlice(List<Segment> segments, List<Segment> target, int from, int to, int destinationStart) {
        if (from >= to) return;
        for (int index = firstOverlapping(segments, from); index < segments.size(); index++) {
            Segment segment = segments.get(index);
            if (segment.start() >= to) break;
            target.add(segment.clipped(from, to, destinationStart));
        }
    }

    private static List<Segment> mergeAdjacent(List<Segment> input) {
        List<Segment> result = new ArrayList<>();
        for (Segment current : input) {
            if (current.start() == current.end()) continue;
            if (!result.isEmpty()) {
                Segment previous = result.get(result.size() - 1);
                boolean linear = previous.exact() && current.exact()
                        && previous.originalEnd() == current.originalStart();
                if (previous.end() == current.start() && previous.sourceFile().equals(current.sourceFile())
                        && previous.includeChain().equals(current.includeChain())
                        && previous.exact() == current.exact() && previous.inputGap() == current.inputGap()
                        && (linear || !previous.exact())) {
                    result.set(result.size() - 1, new Segment(previous.start(), current.end(),
                            previous.sourceFile(), previous.originalStart(), current.originalEnd(),
                            previous.includeChain(), previous.exact(), previous.inputGap()));
                    continue;
                }
            }
            result.add(current);
        }
        return result;
    }

    private int offset(String file, int line, int column) {
        UnicodeText source = sources.get(file);
        return source == null ? 0 : source.offset(line, column);
    }

    private int offsetAfter(Ast.SourceLocation location) {
        return offset(location.file(), location.endLine(), location.endColumn() + 1);
    }

    private static Ast.SourceLocation location(String file, UnicodeText source, int start, int end) {
        int safeStart = Math.max(0, Math.min(start, source.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, source.length()));
        int[] startPosition = source.lineColumn(safeStart);
        int endOffset = safeEnd == safeStart ? safeStart : safeEnd - 1;
        int[] endPosition = source.lineColumn(endOffset);
        return new Ast.SourceLocation(file, startPosition[0], startPosition[1], endPosition[0], endPosition[1]);
    }
}
