package io.proleap.benchmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class ResultWriter {
    private ResultWriter() {}

    static void write(BenchmarkData data) throws IOException {
        Path directory = data.root().resolve("benchmark/results"); Files.createDirectories(directory);
        StringBuilder files = new StringBuilder(Csv.row("frontend","file","source_lines","source_bytes","preprocessing_success",
                "parsing_success","status","preprocessing_errors","lexer_errors","parser_errors","unresolved_copies",
                "preprocessing_ms","parsing_ms","total_ms","peak_heap_bytes","token_count","tree_node_count","tree_max_depth"));
        StringBuilder diagnostics = new StringBuilder(Csv.row("frontend","phase","file","line","column","message","offending_token","exception_class"));
        for (List<FrontendResult> results : data.aggregateFiles().values()) for (FrontendResult r : results) {
            String status = !r.parsingSuccess() || !r.preprocessingSuccess() ? "FAILED" : r.diagnostics().isEmpty() ? "SUCCESS" : "SUCCESS_WITH_WARNINGS";
            files.append(Csv.row(r.frontend(),r.file(),r.sourceLines(),r.sourceBytes(),r.preprocessingSuccess(),r.parsingSuccess(),status,
                    r.preprocessingErrors(),r.lexerErrors(),r.parserErrors(),r.unresolvedCopies(),ms(r.preprocessingTimeNanos()),
                    ms(r.parsingTimeNanos()),ms(r.totalTimeNanos()),r.approximatePeakUsedHeapBytes(),r.tokenCount(),r.parseTreeNodeCount(),r.parseTreeMaxDepth()));
            for (Diagnostic d : r.diagnostics()) diagnostics.append(Csv.row(d.frontend(),d.phase(),d.file(),d.line(),d.column(),
                    d.message(),d.offendingToken(),d.exceptionClass()));
        }
        Files.writeString(directory.resolve("files.csv"), files, StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("diagnostics.csv"), diagnostics, StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("summary.csv"), summary(data), StandardCharsets.UTF_8);
    }

    private static String summary(BenchmarkData data) {
        StringBuilder out = new StringBuilder(Csv.row("frontend","programs","preprocessed","parsed_without_errors","preprocessing_errors",
                "lexer_errors","parser_errors","unresolved_copies","errors_per_kloc","corpus_prep_median_ms","corpus_prep_p95_ms",
                "corpus_prep_mean_ms","corpus_parse_median_ms","corpus_parse_p95_ms","corpus_parse_mean_ms","corpus_total_median_ms",
                "corpus_total_p95_ms","corpus_total_mean_ms","loc_per_second","mb_per_second","max_observed_heap_bytes",
                "total_tokens","total_tree_nodes","max_tree_depth"));
        data.aggregateFiles().forEach((frontend, files) -> {
            List<List<FrontendResult>> runs = data.measuredRuns().get(frontend);
            List<Long> prep = corpusTotals(runs, 0), parse = corpusTotals(runs, 1), total = corpusTotals(runs, 2);
            long loc = files.stream().mapToLong(FrontendResult::sourceLines).sum();
            long bytes = files.stream().mapToLong(FrontendResult::sourceBytes).sum();
            int errors = files.stream().mapToInt(r -> r.preprocessingErrors()+r.lexerErrors()+r.parserErrors()).sum();
            double seconds = Statistics.median(total) / 1_000_000_000.0;
            out.append(Csv.row(frontend,files.size(),files.stream().filter(FrontendResult::preprocessingSuccess).count(),
                    files.stream().filter(FrontendResult::parsingSuccess).count(),files.stream().mapToInt(FrontendResult::preprocessingErrors).sum(),
                    files.stream().mapToInt(FrontendResult::lexerErrors).sum(),files.stream().mapToInt(FrontendResult::parserErrors).sum(),
                    files.stream().mapToInt(FrontendResult::unresolvedCopies).sum(),fmt(errors/(loc/1000.0)),ms(Statistics.median(prep)),
                    ms(Statistics.p95(prep)),ms(Statistics.mean(prep)),ms(Statistics.median(parse)),ms(Statistics.p95(parse)),
                    ms(Statistics.mean(parse)),ms(Statistics.median(total)),ms(Statistics.p95(total)),ms(Statistics.mean(total)),
                    fmt(seconds == 0 ? 0 : loc/seconds),fmt(seconds == 0 ? 0 : bytes/1048576.0/seconds),
                    files.stream().mapToLong(FrontendResult::approximatePeakUsedHeapBytes).max().orElse(0),
                    files.stream().mapToLong(FrontendResult::tokenCount).sum(),files.stream().mapToLong(FrontendResult::parseTreeNodeCount).sum(),
                    files.stream().mapToInt(FrontendResult::parseTreeMaxDepth).max().orElse(0)));
        });
        return out.toString();
    }

    static List<Long> corpusTotals(List<List<FrontendResult>> runs, int metric) {
        return runs.stream().map(run -> run.stream().mapToLong(r -> metric == 0 ? r.preprocessingTimeNanos() : metric == 1 ? r.parsingTimeNanos() : r.totalTimeNanos()).sum()).toList();
    }
    static String ms(long nanos) { return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0); }
    static String ms(double nanos) { return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0); }
    static String fmt(double value) { return String.format(Locale.ROOT, "%.3f", value); }
}
