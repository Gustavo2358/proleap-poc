package io.proleap.benchmark;

final class SourceNormalizerTestSupport {
    private SourceNormalizerTestSupport() {}

    static String fixed(String raw) {
        return SourceNormalizer.normalize(raw, "<test-source>",
                SourceNormalizer.SourceFormat.FIXED).text();
    }
}
