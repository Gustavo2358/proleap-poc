package io.proleap.benchmark;

import java.util.*;

final class SourceNormalizer {
    enum SourceFormat { FIXED }

    record NormalizationDiagnostic(int line, int column, String message) {}

    record Result(SourceMap sourceMap, List<NormalizationDiagnostic> diagnostics,
                  SourceFormat format) {
        Result {
            sourceMap = Objects.requireNonNull(sourceMap, "sourceMap");
            diagnostics = List.copyOf(diagnostics);
            format = Objects.requireNonNull(format, "format");
        }

        String text() { return sourceMap.text(); }
    }

    private record PhysicalLine(String content, String terminator, int start, int contentEnd, int end) {}
    private record NormalizedLine(String content, String terminator,
                                  int contentOriginalStart, int contentOriginalEnd,
                                  int terminatorOriginalStart, int terminatorOriginalEnd,
                                  boolean contentExact, boolean terminatorExact) {}

    private SourceNormalizer() {}

    static String fixed(String raw) {
        return normalize(raw, "<source>").text();
    }

    static Result normalize(String raw, String file) {
        List<NormalizedLine> output = new ArrayList<>();
        for (PhysicalLine physical : physicalLines(raw)) {
            String line = physical.content();
            String padded = line.length() < 7 ? line + "       ".substring(Math.min(7, line.length())) : line;
            char indicator = padded.charAt(6);
            int end = Math.min(padded.length(), 72);
            String area = padded.substring(7, end);
            if (indicator == '*' || indicator == '/') {
                output.add(transformedLine("*> " + area, physical));
            } else if (indicator == '-') {
                if (output.isEmpty()) output.add(transformedLine(area.stripLeading(), physical));
                else {
                    NormalizedLine previousLine = output.get(output.size() - 1);
                    String previous = previousLine.content().stripTrailing();
                    String continuation = area.stripLeading();
                    char quote = oddQuote(previous, '\'') ? '\'' : oddQuote(previous, '"') ? '"' : 0;
                    if (quote != 0 && !continuation.isEmpty() && continuation.charAt(0) == quote)
                        continuation = continuation.substring(1);
                    output.set(output.size() - 1, new NormalizedLine(previous + continuation,
                            physical.terminator(), previousLine.contentOriginalStart(), physical.contentEnd(),
                            physical.contentEnd(), physical.end(), false, true));
                }
            } else if (indicator == 'D' || indicator == 'd') {
                output.add(transformedLine("*> DEBUG " + area, physical));
            } else {
                int contentStart = Math.min(physical.start() + 7, physical.contentEnd());
                int contentEnd = Math.min(physical.start() + end, physical.contentEnd());
                output.add(new NormalizedLine(area, physical.terminator(), contentStart, contentEnd,
                        physical.contentEnd(), physical.end(), true, true));
            }
        }
        return mappedResult(markCommentEntries(output), raw, file);
    }

    private static boolean oddQuote(String value, char quote) {
        return value.chars().filter(c -> c == quote).count() % 2 == 1;
    }

    private static List<NormalizedLine> markCommentEntries(List<NormalizedLine> input) {
        java.util.regex.Pattern header = java.util.regex.Pattern.compile(
                "(?i)^(\\s*)(AUTHOR|INSTALLATION|DATE-WRITTEN|DATE-COMPILED|SECURITY|REMARKS)\\s*\\.\\s*(.*?)\\s*$");
        List<NormalizedLine> output = new ArrayList<>();
        boolean inEntry = false;
        for (NormalizedLine normalizedLine : input) {
            String line = normalizedLine.content();
            java.util.regex.Matcher matcher = header.matcher(line);
            if (matcher.matches()) {
                String value = matcher.group(3);
                if (!value.isBlank()) {
                    String insertedTerminator = normalizedLine.terminator().isEmpty()
                            ? "\n" : normalizedLine.terminator();
                    output.add(transformedFrom(normalizedLine,
                            matcher.group(1) + matcher.group(2) + ". ", insertedTerminator, false));
                    output.add(transformedFrom(normalizedLine,
                            "*>CE " + value, normalizedLine.terminator(), true));
                    inEntry = !value.stripTrailing().endsWith(".");
                } else {
                    output.add(transformedFrom(normalizedLine,
                            matcher.group(1) + matcher.group(2) + ". ",
                            normalizedLine.terminator(), true));
                    inEntry = true;
                }
            } else if (inEntry && !line.isBlank() && !line.stripLeading().startsWith("*>")) {
                if (startsInAreaA(line)) {
                    inEntry = false;
                    output.add(normalizedLine);
                } else {
                    output.add(transformedFrom(normalizedLine,
                            "*>CE " + line.strip(), normalizedLine.terminator(), true));
                }
            } else output.add(normalizedLine);
        }
        return output;
    }

    private static boolean startsInAreaA(String line) {
        int areaAWidth = Math.min(4, line.length());
        for (int i = 0; i < areaAWidth; i++) {
            if (!Character.isWhitespace(line.charAt(i))) return true;
        }
        return false;
    }

    private static List<PhysicalLine> physicalLines(String raw) {
        List<PhysicalLine> result = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < raw.length();) {
            char character = raw.charAt(index);
            if (character == '\n' || character == '\r') {
                int terminatorEnd = character == '\r' && index + 1 < raw.length()
                        && raw.charAt(index + 1) == '\n' ? index + 2 : index + 1;
                result.add(new PhysicalLine(raw.substring(start, index),
                        raw.substring(index, terminatorEnd), start, index, terminatorEnd));
                start = terminatorEnd;
                index = terminatorEnd;
            } else if (character == '\u0085' || character == '\u2028' || character == '\u2029') {
                throw new IllegalArgumentException("Unsupported line separator U+"
                        + String.format(Locale.ROOT, "%04X", (int) character)
                        + " at offset " + index);
            } else {
                index++;
            }
        }
        if (start < raw.length()) {
            result.add(new PhysicalLine(raw.substring(start), "", start, raw.length(), raw.length()));
        }
        return result;
    }

    private static NormalizedLine transformedLine(String content, PhysicalLine physical) {
        return new NormalizedLine(content, physical.terminator(), physical.start(), physical.contentEnd(),
                physical.contentEnd(), physical.end(), false, true);
    }

    private static NormalizedLine transformedFrom(NormalizedLine original, String content,
                                                  String terminator, boolean originalTerminator) {
        return new NormalizedLine(content, terminator,
                original.contentOriginalStart(), original.contentOriginalEnd(),
                originalTerminator ? original.terminatorOriginalStart() : original.contentOriginalStart(),
                originalTerminator ? original.terminatorOriginalEnd() : original.contentOriginalEnd(),
                false, originalTerminator && original.terminatorExact());
    }

    private static Result mappedResult(List<NormalizedLine> lines, String raw, String file) {
        StringBuilder text = new StringBuilder();
        List<SourceMap.Segment> segments = new ArrayList<>();
        for (NormalizedLine line : lines) {
            appendMapped(text, segments, file, line.content(), line.contentOriginalStart(),
                    line.contentOriginalEnd(), line.contentExact());
            appendMapped(text, segments, file, line.terminator(), line.terminatorOriginalStart(),
                    line.terminatorOriginalEnd(), line.terminatorExact());
        }
        SourceMap sourceMap = SourceMap.mapped(text.toString(), file, raw, segments);
        return new Result(sourceMap, List.of(), SourceFormat.FIXED);
    }

    private static void appendMapped(StringBuilder text, List<SourceMap.Segment> segments,
                                     String file, String value, int originalStart,
                                     int originalEnd, boolean exact) {
        if (value.isEmpty()) return;
        int start = text.length();
        text.append(value);
        segments.add(new SourceMap.Segment(start, text.length(), file, originalStart, originalEnd,
                List.of(), exact));
    }
}
