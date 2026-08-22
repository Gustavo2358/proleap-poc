package io.proleap.benchmark;

import java.nio.file.Path;
import java.util.*;

record BenchmarkData(Path root, List<Path> programs, List<Path> copybooks,
                     Map<String, List<List<FrontendResult>>> measuredRuns,
                     Map<String, List<FrontendResult>> aggregateFiles, int warmups, int runs) {
}
