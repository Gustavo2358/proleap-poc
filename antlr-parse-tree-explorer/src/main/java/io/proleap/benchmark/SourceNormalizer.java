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
    private enum ProgramIdQualifier { COMMON, INITIAL, LIBRARY, DEFINITION, RECURSIVE }
    private enum CommentEntryState {
        NONE,
        PROGRAM_NAME_PENDING,
        PROGRAM_OPTIONAL_CLAUSE,
        PROGRAM_QUALIFIER_PENDING,
        PROGRAM_OPTIONAL_PROGRAM,
        PROGRAM_OPTIONAL_PERIOD,
        COMMENT_ENTRY,
        REMARKS_COMMENT_ENTRY
    }

    private enum CommentEntryOwner {
        PROGRAM_ID("programIdParagraph", "PROGRAM-ID", true),
        AUTHOR("authorParagraph", "AUTHOR", false),
        INSTALLATION("installationParagraph", "INSTALLATION", false),
        DATE_WRITTEN("dateWrittenParagraph", "DATE-WRITTEN", false),
        DATE_COMPILED("dateCompiledParagraph", "DATE-COMPILED", false),
        SECURITY("securityParagraph", "SECURITY", false),
        REMARKS("remarksParagraph", "REMARKS", false);

        private final String ruleName;
        private final String keyword;
        private final boolean programId;

        CommentEntryOwner(String ruleName, String keyword, boolean programId) {
            this.ruleName = ruleName;
            this.keyword = keyword;
            this.programId = programId;
        }
    }

    private record CommentEntryHeader(CommentEntryOwner owner, int period) {}
    private record CommentEntryDecision(NormalizedLine line, CommentEntryState state) {}

    private record PhysicalLine(String content, String terminator, int start, int contentEnd,
                                int end, int lineNumber) {}
    private record NormalizedLine(String content, String terminator,
                                  int contentOriginalStart, int contentOriginalEnd,
                                  int terminatorOriginalStart, int terminatorOriginalEnd,
                                  boolean contentExact, boolean terminatorExact,
                                  LineRole role) {}

    private SourceNormalizer() {}

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
                case DEBUG -> output.add(switch (debugLinePolicy) {
                    case INCLUDE -> programTextLine(area, end, physical);
                    case EXCLUDE -> transformedLine("*> DEBUG " + area, physical);
                });
                case NORMAL -> output.add(programTextLine(area, end, physical));
            }
        }
        return mappedResult(normalizeCommentEntries(output), raw, file);
    }

    static Set<String> commentEntryOwnerRules() {
        Set<String> result = new LinkedHashSet<>();
        for (CommentEntryOwner owner : CommentEntryOwner.values()) result.add(owner.ruleName);
        return Collections.unmodifiableSet(result);
    }

    static Set<String> programIdQualifierTokens() {
        Set<String> result = new LinkedHashSet<>();
        for (ProgramIdQualifier qualifier : ProgramIdQualifier.values()) {
            result.add(qualifier.name());
        }
        return Collections.unmodifiableSet(result);
    }

    private static List<NormalizedLine> normalizeCommentEntries(List<NormalizedLine> input) {
        List<NormalizedLine> output = new ArrayList<>();
        CommentEntryState state = CommentEntryState.NONE;
        NormalizedLine lastLine = null;
        for (NormalizedLine normalizedLine : input) {
            lastLine = normalizedLine;
            String line = normalizedLine.content();
            switch (normalizedLine.role()) {
                case NON_PROGRAM, CONTINUATION_PLACEHOLDER -> {
                    output.add(normalizedLine);
                    continue;
                }
                case PROGRAM_TEXT -> { }
            }
            if (line.isBlank()) {
                output.add(normalizedLine);
                continue;
            }

            boolean areaA = startsInAreaA(line);
            if (areaA) {
                if (state == CommentEntryState.PROGRAM_NAME_PENDING
                        || state == CommentEntryState.PROGRAM_QUALIFIER_PENDING) {
                    throw incompleteProgramId(line, normalizedLine, state);
                }
                state = CommentEntryState.NONE;
            }
            CommentEntryHeader header = areaA ? commentEntryHeader(line) : null;
            if (header != null) {
                CommentEntryDecision decision = header.owner().programId
                        ? normalizeProgramIdHeader(normalizedLine, header.period() + 1)
                        : normalizeOrdinaryCommentEntryHeader(
                                normalizedLine, header.owner(), header.period() + 1);
                output.add(decision.line());
                state = decision.state();
            } else if (!areaA) {
                switch (state) {
                    case NONE -> output.add(normalizedLine);
                    case PROGRAM_NAME_PENDING, PROGRAM_OPTIONAL_CLAUSE,
                            PROGRAM_QUALIFIER_PENDING, PROGRAM_OPTIONAL_PROGRAM,
                            PROGRAM_OPTIONAL_PERIOD -> {
                        CommentEntryDecision decision = normalizeProgramIdSyntax(
                                normalizedLine, firstNonSpace(line, 0), state);
                        output.add(decision.line());
                        state = decision.state();
                    }
                    case COMMENT_ENTRY -> output.add(asCommentEntry(normalizedLine, line));
                    case REMARKS_COMMENT_ENTRY -> {
                        if (isEndRemarks(line)) {
                            output.add(normalizedLine);
                            state = CommentEntryState.NONE;
                        } else {
                            output.add(asCommentEntry(normalizedLine, line));
                        }
                    }
                }
            } else {
                output.add(normalizedLine);
            }
        }
        switch (state) {
            case PROGRAM_NAME_PENDING, PROGRAM_QUALIFIER_PENDING ->
                    throw incompleteProgramId("end of file", Objects.requireNonNull(lastLine), state);
            case NONE, PROGRAM_OPTIONAL_CLAUSE, PROGRAM_OPTIONAL_PROGRAM,
                    PROGRAM_OPTIONAL_PERIOD, COMMENT_ENTRY, REMARKS_COMMENT_ENTRY -> { }
        }
        return output;
    }

    private static CommentEntryHeader commentEntryHeader(String line) {
        int start = firstNonSpace(line, 0);
        if (start < 0 || start >= 4) return null;
        for (CommentEntryOwner owner : CommentEntryOwner.values()) {
            int afterKeyword = start + owner.keyword.length();
            if (afterKeyword <= line.length()
                    && line.regionMatches(true, start, owner.keyword, 0, owner.keyword.length())) {
                int period = firstNonSpace(line, afterKeyword);
                if (period >= 0 && isSeparatorPeriod(line, period)) {
                    return new CommentEntryHeader(owner, period);
                }
            }
        }
        return null;
    }

    private static CommentEntryDecision normalizeOrdinaryCommentEntryHeader(
            NormalizedLine line, CommentEntryOwner owner, int tailStart) {
        CommentEntryState state = owner == CommentEntryOwner.REMARKS
                ? CommentEntryState.REMARKS_COMMENT_ENTRY : CommentEntryState.COMMENT_ENTRY;
        int commentStart = firstNonSpace(line.content(), tailStart);
        if (commentStart < 0) return new CommentEntryDecision(line, state);
        return new CommentEntryDecision(tagTail(line, commentStart), state);
    }

    private static CommentEntryDecision normalizeProgramIdHeader(NormalizedLine line,
                                                                  int tailStart) {
        int nameStart = firstNonSpace(line.content(), tailStart);
        if (nameStart < 0) {
            return new CommentEntryDecision(line, CommentEntryState.PROGRAM_NAME_PENDING);
        }
        return normalizeProgramIdSyntax(line, nameStart, CommentEntryState.PROGRAM_NAME_PENDING);
    }

    private static CommentEntryDecision normalizeProgramIdSyntax(
            NormalizedLine line, int start, CommentEntryState initialState) {
        int cursor = start;
        CommentEntryState state = initialState;
        while (cursor >= 0) {
            switch (state) {
                case PROGRAM_NAME_PENDING -> {
                    int afterName = endOfProgramName(line.content(), cursor);
                    if (afterName == cursor) throw incompleteProgramId(line.content(), line, state);
                    cursor = firstNonSpace(line.content(), afterName);
                    state = CommentEntryState.PROGRAM_OPTIONAL_CLAUSE;
                }
                case PROGRAM_OPTIONAL_CLAUSE -> {
                    if (isSeparatorPeriod(line.content(), cursor)) {
                        cursor = firstNonSpace(line.content(), cursor + 1);
                        state = CommentEntryState.COMMENT_ENTRY;
                    } else {
                        int wordEnd = endOfCobolWord(line.content(), cursor);
                        if (wordEquals(line.content(), cursor, wordEnd, "IS")) {
                            cursor = firstNonSpace(line.content(), wordEnd);
                            state = CommentEntryState.PROGRAM_QUALIFIER_PENDING;
                        } else if (isProgramIdQualifier(line.content(), cursor, wordEnd)) {
                            cursor = firstNonSpace(line.content(), wordEnd);
                            state = CommentEntryState.PROGRAM_OPTIONAL_PROGRAM;
                        } else {
                            return commentTail(line, cursor);
                        }
                    }
                }
                case PROGRAM_QUALIFIER_PENDING -> {
                    int wordEnd = endOfCobolWord(line.content(), cursor);
                    if (!isProgramIdQualifier(line.content(), cursor, wordEnd)) {
                        throw incompleteProgramId(line.content(), line, state);
                    }
                    cursor = firstNonSpace(line.content(), wordEnd);
                    state = CommentEntryState.PROGRAM_OPTIONAL_PROGRAM;
                }
                case PROGRAM_OPTIONAL_PROGRAM -> {
                    if (isSeparatorPeriod(line.content(), cursor)) {
                        cursor = firstNonSpace(line.content(), cursor + 1);
                        state = CommentEntryState.COMMENT_ENTRY;
                    } else {
                        int wordEnd = endOfCobolWord(line.content(), cursor);
                        if (wordEquals(line.content(), cursor, wordEnd, "PROGRAM")) {
                            cursor = firstNonSpace(line.content(), wordEnd);
                            state = CommentEntryState.PROGRAM_OPTIONAL_PERIOD;
                        } else {
                            return commentTail(line, cursor);
                        }
                    }
                }
                case PROGRAM_OPTIONAL_PERIOD -> {
                    if (isSeparatorPeriod(line.content(), cursor)) {
                        cursor = firstNonSpace(line.content(), cursor + 1);
                    }
                    state = CommentEntryState.COMMENT_ENTRY;
                }
                case COMMENT_ENTRY -> {
                    return commentTail(line, cursor);
                }
                case NONE, REMARKS_COMMENT_ENTRY -> throw new IllegalStateException(
                        "Invalid PROGRAM-ID normalization state: " + state);
            }
        }
        return new CommentEntryDecision(line, state);
    }

    private static CommentEntryDecision commentTail(NormalizedLine line, int start) {
        return new CommentEntryDecision(tagTail(line, start), CommentEntryState.COMMENT_ENTRY);
    }

    private static IllegalArgumentException incompleteProgramId(
            String content, NormalizedLine line, CommentEntryState state) {
        return new IllegalArgumentException("Incomplete PROGRAM-ID syntax in state " + state
                + " near '" + content.strip() + "' at source offset "
                + line.contentOriginalStart());
    }

    private static boolean isProgramIdQualifier(String line, int start, int end) {
        for (ProgramIdQualifier qualifier : ProgramIdQualifier.values()) {
            if (wordEquals(line, start, end, qualifier.name())) return true;
        }
        return false;
    }

    private static boolean wordEquals(String line, int start, int end, String expected) {
        return end - start == expected.length()
                && line.regionMatches(true, start, expected, 0, expected.length());
    }

    private static int endOfCobolWord(String line, int start) {
        int cursor = start;
        while (cursor < line.length() && isCobolWordCharacter(line.charAt(cursor))) cursor++;
        return cursor;
    }

    private static int endOfProgramName(String line, int start) {
        if (start >= line.length()) return start;
        int quoteIndex = start;
        char first = line.charAt(start);
        if (isLiteralPrefix(first) && start + 1 < line.length()
                && isQuote(line.charAt(start + 1))) {
            quoteIndex = start + 1;
        } else if (!isQuote(first)) {
            return endOfCobolWord(line, start);
        }
        char quote = line.charAt(quoteIndex);
        for (int index = quoteIndex + 1; index < line.length(); index++) {
            if (line.charAt(index) == quote) {
                if (index + 1 < line.length() && line.charAt(index + 1) == quote) {
                    index++;
                } else {
                    return index + 1;
                }
            }
        }
        return start;
    }

    private static boolean isLiteralPrefix(char character) {
        char upper = Character.toUpperCase(character);
        return upper == 'X' || upper == 'G' || upper == 'N' || upper == 'Z';
    }

    private static boolean isQuote(char character) {
        return character == '\'' || character == '"';
    }

    private static int firstNonSpace(String line, int start) {
        for (int index = start; index < line.length(); index++) {
            if (line.charAt(index) != ' ') return index;
        }
        return -1;
    }

    private static boolean isSeparatorPeriod(String line, int index) {
        return index < line.length() && line.charAt(index) == '.'
                && (index + 1 == line.length() || line.charAt(index + 1) == ' ');
    }

    private static boolean isEndRemarks(String line) {
        int start = firstNonSpace(line, 0);
        if (start < 0) return false;
        String keyword = "END-REMARKS";
        int end = start + keyword.length();
        if (end > line.length() || !line.regionMatches(true, start, keyword, 0, keyword.length())) {
            return false;
        }
        int following = firstNonSpace(line, end);
        if (following < 0) return true;
        if (!isSeparatorPeriod(line, following)) return false;
        return firstNonSpace(line, following + 1) < 0;
    }

    private static NormalizedLine tagTail(NormalizedLine line, int commentStart) {
        String prefix = line.content().substring(0, commentStart).stripTrailing();
        String content = prefix + (prefix.isEmpty() ? "" : " ")
                + "*>CE " + line.content().substring(commentStart).strip();
        return transformedFrom(line, content, line.terminator(), true);
    }

    private static NormalizedLine asCommentEntry(NormalizedLine line, String content) {
        return transformedFrom(line, "*>CE " + content.strip(), line.terminator(), true);
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
                if (character == '*' && index + 1 < previous.length()
                        && previous.charAt(index + 1) == '>') {
                    throw continuationFailure(physical,
                            "continuation cannot follow an inline comment");
                }
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
