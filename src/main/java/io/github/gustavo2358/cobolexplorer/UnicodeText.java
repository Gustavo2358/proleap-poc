package io.github.gustavo2358.cobolexplorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Indexed view of a Java string whose public offsets are Unicode code-point offsets. */
final class UnicodeText {
    private final String value;
    private final int[] utf16Offsets;
    private final int[] lineStarts;

    UnicodeText(String value) {
        this.value = Objects.requireNonNull(value, "value");
        int length = value.codePointCount(0, value.length());
        utf16Offsets = new int[length + 1];
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        int utf16 = 0;
        for (int offset = 0; offset < length; offset++) {
            utf16Offsets[offset] = utf16;
            int codePoint = value.codePointAt(utf16);
            utf16 += Character.charCount(codePoint);
            if (codePoint == '\r') {
                if (offset + 1 < length && value.codePointAt(utf16) == '\n') {
                    offset++;
                    utf16Offsets[offset] = utf16;
                    utf16++;
                }
                starts.add(offset + 1);
            } else if (codePoint == '\n') {
                starts.add(offset + 1);
            }
        }
        utf16Offsets[length] = value.length();
        lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
    }

    int length() { return utf16Offsets.length - 1; }

    int utf16Offset(int codePointOffset) {
        return utf16Offsets[clamp(codePointOffset)];
    }

    int codePointOffset(int utf16Offset) {
        int safe = Math.max(0, Math.min(utf16Offset, value.length()));
        int found = Arrays.binarySearch(utf16Offsets, safe);
        if (found >= 0) return found;
        return -found - 2;
    }

    String substring(int start, int end) {
        int safeStart = clamp(start);
        int safeEnd = Math.max(safeStart, clamp(end));
        return value.substring(utf16Offsets[safeStart], utf16Offsets[safeEnd]);
    }

    int indexOf(String needle, int from) {
        int utf16 = value.indexOf(needle, utf16Offset(from));
        return utf16 < 0 ? -1 : codePointOffset(utf16);
    }

    int offset(int line, int column) {
        if (lineStarts.length == 0) return 0;
        int lineIndex = Math.max(0, Math.min(line - 1, lineStarts.length - 1));
        int start = lineStarts[lineIndex];
        int limit = lineIndex + 1 < lineStarts.length ? lineStarts[lineIndex + 1] : length();
        return Math.min(limit, start + Math.max(0, column));
    }

    int[] lineColumn(int offset) {
        int safe = clamp(offset);
        int found = Arrays.binarySearch(lineStarts, safe);
        int lineIndex = found >= 0 ? found : -found - 2;
        return new int[]{lineIndex + 1, safe - lineStarts[lineIndex]};
    }

    private int clamp(int offset) {
        return Math.max(0, Math.min(offset, length()));
    }
}
