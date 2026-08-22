package io.proleap.benchmark;

final class Csv {
    private Csv() {}
    static String row(Object... values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            String value = String.valueOf(values[i] == null ? "" : values[i]);
            if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)
                out.append('"').append(value.replace("\"", "\"\"")).append('"');
            else out.append(value);
        }
        return out.append('\n').toString();
    }
}
