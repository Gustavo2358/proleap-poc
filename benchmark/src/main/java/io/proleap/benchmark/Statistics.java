package io.proleap.benchmark;

import java.util.*;

final class Statistics {
    private Statistics() {}
    static double mean(List<Long> values) { return values.stream().mapToLong(Long::longValue).average().orElse(0); }
    static long median(List<Long> values) { return percentile(values, 0.50); }
    static long p95(List<Long> values) { return percentile(values, 0.95); }
    static long percentile(List<Long> values, double p) {
        if (values.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(values); Collections.sort(sorted);
        int index = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
