package io.proleap.benchmark;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Browser DTO for the symbol-table learning page. */
final class SymbolTableSnapshot {
    record Metrics(int scopes, int symbols, int dataSymbols, int procedureSymbols,
                   int fileSymbols, int diagnostics) {}

    private final SymbolTable table;
    private final Metrics metrics;
    private final Map<String, Integer> kindCounts;

    private SymbolTableSnapshot(SymbolTable table, Metrics metrics, Map<String, Integer> kindCounts) {
        this.table = table;
        this.metrics = metrics;
        this.kindCounts = Collections.unmodifiableMap(new TreeMap<>(kindCounts));
    }

    static SymbolTableSnapshot from(SymbolTable table) {
        Map<String, Integer> kinds = new TreeMap<>();
        int data = 0, procedure = 0, files = 0;
        for (SymbolTable.Symbol symbol : table.symbols()) {
            kinds.merge(symbol.kind().name(), 1, Integer::sum);
            switch (symbol.namespace()) {
                case DATA -> data++;
                case PROCEDURE -> procedure++;
                case FILE -> files++;
                case PROGRAM -> { }
            }
        }
        return new SymbolTableSnapshot(table, new Metrics(table.scopes().size(), table.symbols().size(),
                data, procedure, files, table.diagnostics().size()), kinds);
    }

    Metrics metrics() { return metrics; }

    void write(Path path, String sourceName, List<String> sourceLines) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            out.write("window.SYMBOL_TABLE_DATA={\n\"meta\":{");
            field(out, "source", sourceName); out.write(',');
            out.write("\"scopes\":" + metrics.scopes + ",\"symbols\":" + metrics.symbols + ',');
            out.write("\"dataSymbols\":" + metrics.dataSymbols + ",\"procedureSymbols\":" + metrics.procedureSymbols + ',');
            out.write("\"fileSymbols\":" + metrics.fileSymbols + ",\"diagnostics\":" + metrics.diagnostics + "},\n");
            out.write("\"sourceLines\":[");
            for (int i = 0; i < sourceLines.size(); i++) { if (i > 0) out.write(','); string(out, sourceLines.get(i)); }
            out.write("],\n\"scopes\":[");
            for (int i = 0; i < table.scopes().size(); i++) {
                if (i > 0) out.write(',');
                SymbolTable.Scope scope = table.scopes().get(i);
                out.write("{\"id\":" + scope.id() + ",\"p\":" + scope.parentId() + ",\"k\":"); string(out, scope.kind().name());
                out.write(",\"n\":"); string(out, scope.name());
                out.write(",\"o\":" + scope.ownerSymbolId() + ",\"a\":" + scope.astNodeId() + '}');
            }
            out.write("],\n\"symbols\":[");
            for (int i = 0; i < table.symbols().size(); i++) {
                if (i > 0) out.write(',');
                SymbolTable.Symbol symbol = table.symbols().get(i);
                out.write("{\"id\":" + symbol.id() + ",\"k\":"); string(out, symbol.kind().name());
                out.write(",\"ns\":"); string(out, symbol.namespace().name());
                out.write(",\"n\":"); string(out, symbol.writtenName());
                out.write(",\"c\":"); string(out, symbol.canonicalName());
                out.write(",\"s\":" + symbol.scopeId() + ",\"a\":" + symbol.declarationAstNodeId());
                out.write(",\"l\":" + symbol.span().startLine() + ",\"e\":" + symbol.span().endLine() + ",\"x\":{");
                boolean first = true;
                for (var attribute : symbol.attributes().entrySet()) {
                    if (!first) out.write(','); first = false;
                    string(out, attribute.getKey()); out.write(':'); string(out, attribute.getValue());
                }
                out.write("}}");
            }
            out.write("],\n\"kindCounts\":{");
            boolean first = true;
            for (var entry : kindCounts.entrySet()) {
                if (!first) out.write(','); first = false;
                string(out, entry.getKey()); out.write(':' + String.valueOf(entry.getValue()));
            }
            out.write("},\n\"diagnostics\":[");
            for (int i = 0; i < table.diagnostics().size(); i++) {
                if (i > 0) out.write(',');
                SymbolTable.Diagnostic diagnostic = table.diagnostics().get(i);
                out.write("{\"code\":"); string(out, diagnostic.code());
                out.write(",\"message\":"); string(out, diagnostic.message());
                out.write(",\"scope\":" + diagnostic.scopeId() + ",\"symbols\":[");
                for (int j = 0; j < diagnostic.symbolIds().size(); j++) {
                    if (j > 0) out.write(','); out.write(String.valueOf(diagnostic.symbolIds().get(j)));
                }
                out.write("]}");
            }
            out.write("]};\n");
        }
    }

    private static void field(Writer out, String name, String value) throws IOException {
        string(out, name); out.write(':'); string(out, value);
    }

    private static void string(Writer out, String value) throws IOException {
        out.write('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.write("\\\""); case '\\' -> out.write("\\\\");
                case '\b' -> out.write("\\b"); case '\f' -> out.write("\\f");
                case '\n' -> out.write("\\n"); case '\r' -> out.write("\\r"); case '\t' -> out.write("\\t");
                default -> { if (ch < 0x20 || ch == '\u2028' || ch == '\u2029') out.write(String.format("\\u%04x", (int) ch)); else out.write(ch); }
            }
        }
        out.write('"');
    }
}
