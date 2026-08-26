package io.github.gustavo2358.cobolexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, segment-based mapping from preprocessed text back to COBOL source files. */
final class SourceMap {
    record Segment(int start, int end, String sourceFile, int originalStart, int originalEnd,
                   List<Ast.CopyFrame> includeChain, boolean exact) {
        Segment {
            includeChain = List.copyOf(includeChain);
            if (start < 0 || end < start) throw new IllegalArgumentException("invalid expanded range");
        }

        Segment shifted(int delta) {
            return new Segment(start + delta, end + delta, sourceFile, originalStart, originalEnd,
                    includeChain, exact);
        }

        Segment clipped(int from, int to, int destinationStart) {
            int clippedStart = Math.max(start, from);
            int clippedEnd = Math.min(end, to);
            int originalClippedStart = exact ? originalStart + clippedStart - start : originalStart;
            int originalClippedEnd = exact ? originalStart + clippedEnd - start : originalEnd;
            return new Segment(destinationStart + clippedStart - from,
                    destinationStart + clippedEnd - from, sourceFile,
                    originalClippedStart, originalClippedEnd, includeChain, exact);
        }
    }

    private final String text;
    private final List<Segment> segments;
    private final Map<String, String> sources;

    private SourceMap(String text, List<Segment> segments, Map<String, String> sources) {
        this.text = Objects.requireNonNull(text);
        this.segments = List.copyOf(segments);
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
    }

    static SourceMap identity(String text, String file) {
        List<Segment> segments = text.isEmpty() ? List.of()
                : List.of(new Segment(0, text.length(), file, 0, text.length(), List.of(), true));
        return new SourceMap(text, segments, Map.of(file, text));
    }

    static SourceMap mapped(String text, String file, String original, List<Segment> segments) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(original, "original");
        int previousEnd = 0;
        for (Segment segment : segments) {
            if (segment.start() < previousEnd || segment.end() > text.length()) {
                throw new IllegalArgumentException("invalid or overlapping mapped segment");
            }
            if (!segment.sourceFile().equals(file)) {
                throw new IllegalArgumentException("mapped segment belongs to another source file");
            }
            if (segment.originalStart() < 0 || segment.originalEnd() < segment.originalStart()
                    || segment.originalEnd() > original.length()) {
                throw new IllegalArgumentException("invalid original segment range");
            }
            if (segment.exact()
                    && segment.end() - segment.start() != segment.originalEnd() - segment.originalStart()) {
                throw new IllegalArgumentException("exact mapped segment changes length");
            }
            previousEnd = segment.end();
        }
        return new SourceMap(text, mergeAdjacent(segments), Map.of(file, original));
    }

    String text() { return text; }

    SourceMap replace(int start, int end, SourceMap replacement) {
        if (start < 0 || end < start || end > text.length()) throw new IllegalArgumentException("invalid replacement range");
        String nextText = text.substring(0, start) + replacement.text + text.substring(end);
        List<Segment> next = new ArrayList<>();
        addSlice(next, 0, start, 0);
        for (Segment segment : replacement.segments) next.add(segment.shifted(start));
        int trailingStart = start + replacement.text.length();
        addSlice(next, end, text.length(), trailingStart);
        Map<String, String> nextSources = new LinkedHashMap<>(sources);
        replacement.sources.forEach(nextSources::putIfAbsent);
        return new SourceMap(nextText, mergeAdjacent(next), nextSources);
    }

    SourceMap transformedSlice(int start, int end, String replacementText) {
        Ast.SourceProvenance origin = provenance(start, end);
        Segment segment = replacementText.isEmpty() ? null : new Segment(0, replacementText.length(),
                origin.original().file(), offset(origin.original().file(), origin.original().startLine(),
                origin.original().startColumn()), offsetAfter(origin.original()), origin.includeChain(), false);
        return new SourceMap(replacementText, segment == null ? List.of() : List.of(segment), sources);
    }

    SourceMap replaceLiteral(String from, String to) {
        if (from.isEmpty()) return this;
        SourceMap result = this;
        int searchFrom = 0;
        while (true) {
            int index = result.text.indexOf(from, searchFrom);
            if (index < 0) return result;
            result = result.replace(index, index + from.length(), result.transformedSlice(index, index + from.length(), to));
            searchFrom = index + to.length();
        }
    }

    SourceMap withCopyFrame(Ast.CopyFrame frame) {
        List<Segment> framed = segments.stream().map(segment -> {
            List<Ast.CopyFrame> chain = new ArrayList<>();
            chain.add(frame);
            chain.addAll(segment.includeChain());
            return new Segment(segment.start(), segment.end(), segment.sourceFile(), segment.originalStart(),
                    segment.originalEnd(), chain, segment.exact());
        }).toList();
        return new SourceMap(text, framed, sources);
    }

    Ast.SourceProvenance provenance(int start, int end) {
        int safeStart = Math.max(0, Math.min(start, text.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, text.length()));
        Segment first = segmentAt(safeStart, safeEnd == safeStart);
        Segment last = segmentAt(Math.max(safeStart, safeEnd - 1), safeEnd == safeStart);
        Ast.SourceLocation expanded = location("<preprocessed>", text, safeStart, safeEnd);
        if (first == null) return new Ast.SourceProvenance(expanded,
                new Ast.SourceLocation("<unknown>", 0, 0, 0, 0), List.of(), false);
        boolean compatible = last != null && first.sourceFile().equals(last.sourceFile())
                && first.includeChain().equals(last.includeChain());
        int originalStart = first.originalStart() + (first.exact() ? safeStart - first.start() : 0);
        int originalEnd = compatible
                ? last.originalStart() + (last.exact() ? safeEnd - last.start() : last.originalEnd() - last.originalStart())
                : first.originalEnd();
        String originalText = sources.getOrDefault(first.sourceFile(), "");
        Ast.SourceLocation original = location(first.sourceFile(), originalText, originalStart, originalEnd);
        boolean exact = compatible && overlapping(safeStart, safeEnd).stream().allMatch(Segment::exact);
        return new Ast.SourceProvenance(expanded, original, first.includeChain(), exact);
    }

    private List<Segment> overlapping(int start, int end) {
        return segments.stream().filter(segment -> segment.end() > start && segment.start() < end).toList();
    }

    private Segment segmentAt(int offset, boolean empty) {
        for (Segment segment : segments)
            if ((offset >= segment.start() && offset < segment.end())
                    || (empty && offset == segment.end())) return segment;
        return null;
    }

    private void addSlice(List<Segment> target, int from, int to, int destinationStart) {
        if (from >= to) return;
        for (Segment segment : segments) {
            if (segment.end() <= from || segment.start() >= to) continue;
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
                        && previous.exact() == current.exact() && (linear || !previous.exact())) {
                    result.set(result.size() - 1, new Segment(previous.start(), current.end(),
                            previous.sourceFile(), previous.originalStart(), current.originalEnd(),
                            previous.includeChain(), previous.exact()));
                    continue;
                }
            }
            result.add(current);
        }
        return result;
    }

    private int offset(String file, int line, int column) {
        String source = sources.getOrDefault(file, "");
        int offset = 0;
        for (int current = 1; current < line && offset < source.length(); current++) {
            int newline = source.indexOf('\n', offset);
            offset = newline < 0 ? source.length() : newline + 1;
        }
        return Math.min(source.length(), offset + Math.max(0, column));
    }

    private int offsetAfter(Ast.SourceLocation location) {
        return offset(location.file(), location.endLine(), location.endColumn() + 1);
    }

    private static Ast.SourceLocation location(String file, String source, int start, int end) {
        int[] startPosition = lineColumn(source, Math.max(0, Math.min(start, source.length())));
        int endOffset = Math.max(start, end) == start ? start : Math.max(start, end) - 1;
        int[] endPosition = lineColumn(source, Math.max(0, Math.min(endOffset, source.length())));
        return new Ast.SourceLocation(file, startPosition[0], startPosition[1], endPosition[0], endPosition[1]);
    }

    private static int[] lineColumn(String source, int offset) {
        int line = 1;
        int column = 0;
        for (int i = 0; i < offset && i < source.length(); i++) {
            if (source.charAt(i) == '\n') { line++; column = 0; }
            else column++;
        }
        return new int[]{line, column};
    }
}
