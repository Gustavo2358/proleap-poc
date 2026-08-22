package io.proleap.benchmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class CopybookLibrary {
    private final Map<String, Path> entries = new HashMap<>();

    CopybookLibrary(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            files.filter(Files::isRegularFile).forEach(p -> {
                String file = p.getFileName().toString().toLowerCase(Locale.ROOT);
                entries.put(file, p);
                int dot = file.lastIndexOf('.');
                if (dot > 0) entries.putIfAbsent(file.substring(0, dot), p);
            });
        }
    }

    Optional<Path> resolve(String requested) {
        String key = requested.replace("'", "").replace("\"", "").trim().toLowerCase(Locale.ROOT);
        Path found = entries.get(key);
        if (found == null && !key.contains(".")) found = entries.get(key + ".cpy");
        return Optional.ofNullable(found);
    }

    String readNormalized(Path path) throws IOException {
        return SourceNormalizer.fixed(Files.readString(path, StandardCharsets.UTF_8));
    }
}
