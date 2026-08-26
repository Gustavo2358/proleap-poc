package io.proleap.benchmark;

import java.util.*;

final class SourceNormalizer {
    enum SourceFormat { FIXED }
    enum DebugLinePolicy { INCLUDE, EXCLUDE }

    record Options(SourceFormat format, DebugLinePolicy debugLinePolicy) {
        Options {
            format = Objects.requireNonNull(format, "format");
            debugLinePolicy = Objects.requireNonNull(debugLinePolicy, "debugLinePolicy");
        }
    }

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

    private enum Indicator { NORMAL, COMMENT, PAGE_EJECT_COMMENT, CONTINUATION, DEBUG }
    private enum LineRole { PROGRAM_TEXT, NON_PROGRAM, CONTINUATION_PLACEHOLDER }
    private enum ContinuationKind { SINGLE_QUOTED_LITERAL, DOUBLE_QUOTED_LITERAL, WORD }

    private record PhysicalLine(String content, String terminator, int start, int contentEnd,
                                int end, int lineNumber) {}
    private record NormalizedLine(String content, String terminator,
                                  int contentOriginalStart, int contentOriginalEnd,
                                  int terminatorOriginalStart, int terminatorOriginalEnd,
                                  boolean contentExact, boolean terminatorExact,
                                  LineRole role) {}

    private SourceNormalizer() {}

    static String fixed(String raw) {
        return normalize(raw, "<source>", new Options(
                SourceFormat.FIXED, DebugLinePolicy.EXCLUDE)).text();
    }

    static Result normalize(String raw, String file, SourceFormat format) {
        return normalize(raw, file, new Options(format, DebugLinePolicy.EXCLUDE));
    }

    static Result normalize(String raw, String file, Options options) {
        Objects.requireNonNull(options, "options");
        return switch (options.format()) {
            case FIXED -> normalizeFixed(raw, file, options.debugLinePolicy());
        };
    }

    private static Result normalizeFixed(String raw, String file, DebugLinePolicy debugLinePolicy) {
        List<NormalizedLine> output = new ArrayList<>();
        for (PhysicalLine physical : physicalLines(raw)) {
            String line = physical.content();
            validateFixedCharacters(line, physical.start());
            String padded = line.length() < 7 ? line + "       ".substring(Math.min(7, line.length())) : line;
            char indicator = padded.charAt(6);
            Indicator kind = indicator(indicator, physical);
            int end = Math.min(padded.length(), 72);
            String area = padded.substring(7, end);
            switch (kind) {
                case COMMENT, PAGE_EJECT_COMMENT ->
                        output.add(transformedLine("*> " + area, physical));
                case CONTINUATION -> appendContinuation(output, area, physical);
                case DEBUG -> {
                    if (debugLinePolicy == DebugLinePolicy.INCLUDE) {
                        output.add(programTextLine(area, end, physical));
                    } else {
                        output.add(transformedLine("*> DEBUG " + area, physical));
                    }
                }
                case NORMAL -> output.add(programTextLine(area, end, physical));
            }
        }
        return mappedResult(markCommentEntries(output), raw, file);
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
        int lineNumber = 1;
        for (int index = 0; index < raw.length();) {
            char character = raw.charAt(index);
            if (character == '\n' || character == '\r') {
                int terminatorEnd = character == '\r' && index + 1 < raw.length()
                        && raw.charAt(index + 1) == '\n' ? index + 2 : index + 1;
                result.add(new PhysicalLine(raw.substring(start, index),
                        raw.substring(index, terminatorEnd), start, index, terminatorEnd, lineNumber++));
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
            result.add(new PhysicalLine(raw.substring(start), "", start, raw.length(), raw.length(),
                    lineNumber));
        }
        return result;
    }

    private static void validateFixedCharacters(String line, int rawStart) {
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '\t' && index >= 6 && hasNonWhitespaceAfter(line, index)) {
                throw new IllegalArgumentException("Unsupported tab in fixed-format source at offset "
                        + (rawStart + index));
            }
            if (character > 0x7f) {
                throw new IllegalArgumentException("Unsupported non-ASCII character U+"
                        + String.format(Locale.ROOT, "%04X", (int) character)
                        + " in fixed-format source at offset " + (rawStart + index));
            }
        }
    }

    private static boolean hasNonWhitespaceAfter(String line, int index) {
        for (int following = index + 1; following < line.length(); following++) {
            if (!Character.isWhitespace(line.charAt(following))) return true;
        }
        return false;
    }

    private static NormalizedLine transformedLine(String content, PhysicalLine physical) {
        return new NormalizedLine(content, physical.terminator(), physical.start(), physical.contentEnd(),
                physical.contentEnd(), physical.end(), false, true, LineRole.NON_PROGRAM);
    }

    private static NormalizedLine programTextLine(String area, int areaEnd, PhysicalLine physical) {
        int contentStart = Math.min(physical.start() + 7, physical.contentEnd());
        int contentEnd = Math.min(physical.start() + areaEnd, physical.contentEnd());
        return new NormalizedLine(area, physical.terminator(), contentStart, contentEnd,
                physical.contentEnd(), physical.end(), true, true, LineRole.PROGRAM_TEXT);
    }

    private static Indicator indicator(char indicator, PhysicalLine physical) {
        return switch (indicator) {
            case ' ' -> Indicator.NORMAL;
            case '*' -> Indicator.COMMENT;
            case '/' -> Indicator.PAGE_EJECT_COMMENT;
            case '-' -> Indicator.CONTINUATION;
            case 'D', 'd' -> Indicator.DEBUG;
            default -> throw new IllegalArgumentException("Unsupported fixed-format indicator '"
                    + indicator + "' at line " + physical.lineNumber() + ", column 7");
        };
    }

    private static void appendContinuation(List<NormalizedLine> output, String area,
                                           PhysicalLine physical) {
        int target = output.size() - 1;
        while (target >= 0 && output.get(target).role() == LineRole.CONTINUATION_PLACEHOLDER) {
            target--;
        }
        if (target < 0) {
            throw continuationFailure(physical, "orphan continuation has no preceding source record");
        }
        NormalizedLine previousLine = output.get(target);
        if (previousLine.role() != LineRole.PROGRAM_TEXT) {
            throw continuationFailure(physical,
                    "continuation cannot follow a comment or excluded source record");
        }
        String previous = previousLine.content().stripTrailing();
        String continuation = area.stripLeading();
        ContinuationKind kind = continuationKind(previous, physical);
        switch (kind) {
            case SINGLE_QUOTED_LITERAL -> continuation = literalContinuation(
                    continuation, '\'', physical);
            case DOUBLE_QUOTED_LITERAL -> continuation = literalContinuation(
                    continuation, '"', physical);
            case WORD -> {
                if (continuation.isEmpty() || !isCobolWordCharacter(continuation.charAt(0))) {
                    throw continuationFailure(physical,
                            "word continuation must begin with a COBOL word character");
                }
            }
        }
        output.set(target, new NormalizedLine(previous + continuation,
                previousLine.terminator(), previousLine.contentOriginalStart(), physical.contentEnd(),
                previousLine.terminatorOriginalStart(), previousLine.terminatorOriginalEnd(),
                false, previousLine.terminatorExact(), LineRole.PROGRAM_TEXT));
        output.add(new NormalizedLine("", physical.terminator(), physical.start(), physical.contentEnd(),
                physical.contentEnd(), physical.end(), false, true,
                LineRole.CONTINUATION_PLACEHOLDER));
    }

    private static ContinuationKind continuationKind(String previous, PhysicalLine physical) {
        char openQuote = 0;
        for (int index = 0; index < previous.length(); index++) {
            char character = previous.charAt(index);
            if (openQuote == 0) {
                if (character == '\'' || character == '"') openQuote = character;
            } else if (character == openQuote) {
                if (index + 1 < previous.length() && previous.charAt(index + 1) == openQuote) {
                    index++;
                } else {
                    openQuote = 0;
                }
            }
        }
        if (openQuote == '\'') return ContinuationKind.SINGLE_QUOTED_LITERAL;
        if (openQuote == '"') return ContinuationKind.DOUBLE_QUOTED_LITERAL;
        if (!previous.isEmpty() && isCobolWordCharacter(previous.charAt(previous.length() - 1))) {
            return ContinuationKind.WORD;
        }
        throw continuationFailure(physical,
                "continuation requires an open literal or a split COBOL word");
    }

    private static String literalContinuation(String continuation, char expectedQuote,
                                              PhysicalLine physical) {
        if (continuation.isEmpty() || continuation.charAt(0) != expectedQuote) {
            throw continuationFailure(physical,
                    "literal continuation must begin with matching quote '" + expectedQuote + "'");
        }
        return continuation.substring(1);
    }

    private static boolean isCobolWordCharacter(char character) {
        return character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9'
                || character == '-'
                || character == '_';
    }

    private static IllegalArgumentException continuationFailure(PhysicalLine physical,
                                                                String reason) {
        return new IllegalArgumentException("Invalid fixed-format continuation at line "
                + physical.lineNumber() + ", column 7: " + reason);
    }

    private static NormalizedLine transformedFrom(NormalizedLine original, String content,
                                                  String terminator, boolean originalTerminator) {
        return new NormalizedLine(content, terminator,
                original.contentOriginalStart(), original.contentOriginalEnd(),
                originalTerminator ? original.terminatorOriginalStart() : original.contentOriginalStart(),
                originalTerminator ? original.terminatorOriginalEnd() : original.contentOriginalEnd(),
                false, originalTerminator && original.terminatorExact(), original.role());
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
