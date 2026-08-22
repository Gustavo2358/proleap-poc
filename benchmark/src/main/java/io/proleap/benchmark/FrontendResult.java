package io.proleap.benchmark;

import java.util.List;

public record FrontendResult(
        String frontend, String file, long sourceBytes, long sourceLines,
        boolean preprocessingSuccess, boolean parsingSuccess,
        int preprocessingErrors, int lexerErrors, int parserErrors, int unresolvedCopies,
        long preprocessingTimeNanos, long parsingTimeNanos, long totalTimeNanos,
        long approximatePeakUsedHeapBytes, long tokenCount, long parseTreeNodeCount,
        int parseTreeMaxDepth, List<Diagnostic> diagnostics, String normalizedSource,
        String parseTreeSample) {
}
