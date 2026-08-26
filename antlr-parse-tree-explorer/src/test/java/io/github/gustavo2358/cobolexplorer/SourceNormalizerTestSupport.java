package io.github.gustavo2358.cobolexplorer;

final class SourceNormalizerTestSupport {
    private SourceNormalizerTestSupport() {}

    static String fixed(String raw) {
        return SourceNormalizer.normalize(raw, "<test-source>",
                SourceNormalizer.SourceFormat.FIXED).text();
    }
}
