package io.proleap.benchmark;

import java.util.*;

final class SourceNormalizer {
    private SourceNormalizer() {}

    static String fixed(String raw) {
        List<String> output = new ArrayList<>();
        for (String line : raw.split("\\R", -1)) {
            String padded = line.length() < 7 ? line + "       ".substring(Math.min(7, line.length())) : line;
            char indicator = padded.charAt(6);
            int end = Math.min(padded.length(), 72);
            String area = padded.substring(7, end);
            if (indicator == '*' || indicator == '/') {
                output.add("*> " + area);
            } else if (indicator == '-') {
                if (output.isEmpty()) output.add(area.stripLeading());
                else {
                    String previous = output.get(output.size() - 1).stripTrailing();
                    String continuation = area.stripLeading();
                    char quote = oddQuote(previous, '\'') ? '\'' : oddQuote(previous, '"') ? '"' : 0;
                    if (quote != 0 && !continuation.isEmpty() && continuation.charAt(0) == quote)
                        continuation = continuation.substring(1);
                    output.set(output.size() - 1, previous + continuation);
                }
            } else if (indicator == 'D' || indicator == 'd') {
                output.add("*> DEBUG " + area);
            } else {
                output.add(area);
            }
        }
        return markCommentEntries(output);
    }

    private static boolean oddQuote(String value, char quote) {
        return value.chars().filter(c -> c == quote).count() % 2 == 1;
    }

    private static String markCommentEntries(List<String> input) {
        java.util.regex.Pattern header = java.util.regex.Pattern.compile(
                "(?i)^(\\s*)(AUTHOR|INSTALLATION|DATE-WRITTEN|DATE-COMPILED|SECURITY|REMARKS)\\s*\\.\\s*(.*?)\\s*$");
        List<String> output = new ArrayList<>();
        boolean inEntry = false;
        for (String line : input) {
            java.util.regex.Matcher matcher = header.matcher(line);
            if (matcher.matches()) {
                output.add(matcher.group(1) + matcher.group(2) + ". ");
                String value = matcher.group(3);
                if (!value.isBlank()) {
                    output.add("*>CE " + value);
                    inEntry = !value.stripTrailing().endsWith(".");
                } else inEntry = true;
            } else if (inEntry && !line.isBlank() && !line.stripLeading().startsWith("*>")) {
                if (startsInAreaA(line)) {
                    inEntry = false;
                    output.add(line);
                } else {
                    output.add("*>CE " + line.strip());
                }
            } else output.add(line);
        }
        return String.join("\n", output) + "\n";
    }

    private static boolean startsInAreaA(String line) {
        int areaAWidth = Math.min(4, line.length());
        for (int i = 0; i < areaAWidth; i++) {
            if (!Character.isWhitespace(line.charAt(i))) return true;
        }
        return false;
    }
}
