package io.proleap.benchmark;

import java.util.*;

final class SourceNormalizer {
    private record PhysicalLine(String content, String terminator, int start, int contentEnd, int end) {}
    private record NormalizedLine(String content, String terminator) {}

    private SourceNormalizer() {}

    static String fixed(String raw) {
        List<NormalizedLine> output = new ArrayList<>();
        for (PhysicalLine physical : physicalLines(raw)) {
            String line = physical.content();
            String padded = line.length() < 7 ? line + "       ".substring(Math.min(7, line.length())) : line;
            char indicator = padded.charAt(6);
            int end = Math.min(padded.length(), 72);
            String area = padded.substring(7, end);
            if (indicator == '*' || indicator == '/') {
                output.add(new NormalizedLine("*> " + area, physical.terminator()));
            } else if (indicator == '-') {
                if (output.isEmpty()) output.add(new NormalizedLine(
                        area.stripLeading(), physical.terminator()));
                else {
                    NormalizedLine previousLine = output.get(output.size() - 1);
                    String previous = previousLine.content().stripTrailing();
                    String continuation = area.stripLeading();
                    char quote = oddQuote(previous, '\'') ? '\'' : oddQuote(previous, '"') ? '"' : 0;
                    if (quote != 0 && !continuation.isEmpty() && continuation.charAt(0) == quote)
                        continuation = continuation.substring(1);
                    output.set(output.size() - 1, new NormalizedLine(
                            previous + continuation, physical.terminator()));
                }
            } else if (indicator == 'D' || indicator == 'd') {
                output.add(new NormalizedLine("*> DEBUG " + area, physical.terminator()));
            } else {
                output.add(new NormalizedLine(area, physical.terminator()));
            }
        }
        return markCommentEntries(output);
    }

    private static boolean oddQuote(String value, char quote) {
        return value.chars().filter(c -> c == quote).count() % 2 == 1;
    }

    private static String markCommentEntries(List<NormalizedLine> input) {
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
                    output.add(new NormalizedLine(
                            matcher.group(1) + matcher.group(2) + ". ", insertedTerminator));
                    output.add(new NormalizedLine("*>CE " + value, normalizedLine.terminator()));
                    inEntry = !value.stripTrailing().endsWith(".");
                } else {
                    output.add(new NormalizedLine(
                            matcher.group(1) + matcher.group(2) + ". ", normalizedLine.terminator()));
                    inEntry = true;
                }
            } else if (inEntry && !line.isBlank() && !line.stripLeading().startsWith("*>")) {
                if (startsInAreaA(line)) {
                    inEntry = false;
                    output.add(normalizedLine);
                } else {
                    output.add(new NormalizedLine("*>CE " + line.strip(), normalizedLine.terminator()));
                }
            } else output.add(normalizedLine);
        }
        StringBuilder result = new StringBuilder();
        for (NormalizedLine line : output) result.append(line.content()).append(line.terminator());
        return result.toString();
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
}
