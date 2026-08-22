package io.proleap.benchmark;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToLongFunction;

public final class BenchmarkMain {
    private BenchmarkMain() {}
    public static void main(String[] args) throws Exception {
        int warmups = argument(args, "--warmups", 2), runs = argument(args, "--runs", 5);
        Path benchmark = Path.of("").toAbsolutePath().normalize();
        Path root = benchmark.getFileName().toString().equals("benchmark") ? benchmark.getParent() : benchmark;
        List<Path> programs = discover(root.resolve("cbl"), Set.of(".cbl"));
        List<Path> copybooks = discover(root.resolve("cpy"), Set.of(".cpy"));
        List<CobolFrontend> frontends = List.of(new Cobol85Frontend(root.resolve("cpy")), new ProLeapGrammarFrontend(root.resolve("cpy")));

        for (int i = 0; i < warmups; i++) for (CobolFrontend frontend : frontends) {
            System.out.printf(Locale.ROOT, "Warmup %d/%d: %s%n", i+1, warmups, frontend.name());
            for (Path program : programs) frontend.parse(program);
        }
        Map<String, List<List<FrontendResult>>> measured = new LinkedHashMap<>();
        for (CobolFrontend frontend : frontends) {
            List<List<FrontendResult>> frontendRuns = new ArrayList<>();
            for (int i = 0; i < runs; i++) {
                System.out.printf(Locale.ROOT, "Measured %d/%d: %s%n", i+1, runs, frontend.name());
                List<FrontendResult> oneRun = new ArrayList<>();
                for (Path program : programs) oneRun.add(frontend.parse(program));
                frontendRuns.add(oneRun);
            }
            measured.put(frontend.name(), frontendRuns);
        }
        Map<String, List<FrontendResult>> aggregate = new LinkedHashMap<>();
        measured.forEach((frontend, frontendRuns) -> aggregate.put(frontend, aggregate(frontendRuns)));
        BenchmarkData data = new BenchmarkData(root, programs, copybooks, measured, aggregate, warmups, runs);
        ResultWriter.write(data); ReportWriter.write(data);
        System.out.println("Generated benchmark/results/*.csv and benchmark/REPORT.md");
    }

    static List<Path> discover(Path directory, Set<String> extensions) throws IOException {
        try (var paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile).filter(p -> {
                String lower = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return extensions.stream().anyMatch(lower::endsWith);
            }).sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))).toList();
        }
    }

    private static List<FrontendResult> aggregate(List<List<FrontendResult>> runs) {
        List<FrontendResult> output = new ArrayList<>();
        for (int i = 0; i < runs.get(0).size(); i++) {
            FrontendResult base = runs.get(0).get(i); final int index = i;
            long prep = median(runs,index,FrontendResult::preprocessingTimeNanos), parse = median(runs,index,FrontendResult::parsingTimeNanos);
            long peak = runs.stream().map(run -> run.get(index)).mapToLong(FrontendResult::approximatePeakUsedHeapBytes).max().orElse(0);
            output.add(new FrontendResult(base.frontend(),base.file(),base.sourceBytes(),base.sourceLines(),base.preprocessingSuccess(),
                    base.parsingSuccess(),base.preprocessingErrors(),base.lexerErrors(),base.parserErrors(),base.unresolvedCopies(),prep,parse,
                    prep+parse,peak,base.tokenCount(),base.parseTreeNodeCount(),base.parseTreeMaxDepth(),base.diagnostics(),base.normalizedSource(),base.parseTreeSample()));
        }
        return output;
    }
    private static long median(List<List<FrontendResult>> runs, int index, ToLongFunction<FrontendResult> metric) {
        return Statistics.median(runs.stream().map(run -> metric.applyAsLong(run.get(index))).toList());
    }
    private static int argument(String[] args, String name, int fallback) {
        for (int i=0;i<args.length-1;i++) if (args[i].equals(name)) return Integer.parseInt(args[i+1]);
        return fallback;
    }
}
