package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessDocsTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path DOCS = ROOT.resolve("docs");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("(?<!!)\\[[^]]+]\\(([^)]+)\\)");
    private static final Pattern ADR_HEADING = Pattern.compile("(?m)^# (ADR-\\d{4}) — ");
    private static final Pattern ADR_INDEX_ROW = Pattern.compile("(?m)^\\| (ADR-\\d{4}) \\|");
    private static final Pattern INVARIANT_HEADING = Pattern.compile("(?m)^### (INV-[A-Z]+-\\d{3}) — ");
    private static final Pattern EVAL_ROW = Pattern.compile("(?m)^\\| (EVAL-[A-Z0-9-]+-\\d{3}) \\|");
    private static final Pattern BACKLOG_HEADING = Pattern.compile("(?m)^### (BACKLOG-[A-Z]+-\\d{3}) — ");
    private static final Pattern CANONICAL_ID = Pattern.compile(
            "\\b(?:ADR-\\d{4}|INV-[A-Z]+-\\d{3}|EVAL-[A-Z0-9-]+-\\d{3}|BACKLOG-[A-Z]+-\\d{3})\\b");

    @Test
    void stableGateEntrypointsExistAndAreExecutable() {
        for (String gate : List.of("check-docs.sh", "check-fast.sh", "check-semantic.sh",
                "check-performance.sh", "check-architecture.sh", "check-full.sh")) {
            Path entrypoint = ROOT.resolve("scripts/harness").resolve(gate);
            assertTrue(Files.isRegularFile(entrypoint), "entrypoint ausente: " + ROOT.relativize(entrypoint));
            assertTrue(Files.isExecutable(entrypoint), "entrypoint não executável: " + ROOT.relativize(entrypoint));
        }
    }

    @Test
    void internalMarkdownLinksResolveToExistingPaths() throws Exception {
        List<String> failures = new ArrayList<>();
        for (Path document : markdownDocuments()) {
            String content = read(document);
            Matcher matcher = MARKDOWN_LINK.matcher(content);
            while (matcher.find()) {
                String target = matcher.group(1).trim();
                if (target.startsWith("<") && target.endsWith(">")) {
                    target = target.substring(1, target.length() - 1);
                }
                int title = target.indexOf(" \"");
                if (title >= 0) {
                    target = target.substring(0, title);
                }
                if (target.startsWith("#") || target.matches("[A-Za-z][A-Za-z0-9+.-]*:.*")) {
                    continue;
                }
                String pathPart = target.split("#", 2)[0];
                Path resolved = document.getParent().resolve(pathPart).normalize();
                if (!Files.exists(resolved)) {
                    failures.add(ROOT.relativize(document) + " -> " + target);
                }
            }
        }
        assertTrue(failures.isEmpty(), "links internos quebrados:\n" + String.join("\n", failures));
    }

    @Test
    void canonicalIdsAreUniqueIndexedAndResolvable() throws Exception {
        Path decisions = DOCS.resolve("architecture/decisions");
        Set<String> adrIds = new HashSet<>();
        try (Stream<Path> files = Files.list(decisions)) {
            for (Path adr : files.filter(path -> path.getFileName().toString().matches("\\d{4}-.*\\.md"))
                    .sorted().toList()) {
                Set<String> ids = definitions(adr, ADR_HEADING);
                assertEquals(1, ids.size(), "ADR deve declarar exatamente um ID: " + ROOT.relativize(adr));
                String id = ids.iterator().next();
                assertTrue(adr.getFileName().toString().startsWith(id.substring(4) + "-"),
                        "ID e nome do ADR divergem: " + ROOT.relativize(adr));
                assertTrue(adrIds.add(id), "ID de ADR duplicado: " + id);
            }
        }
        Set<String> indexedAdrs = definitions(decisions.resolve("index.md"), ADR_INDEX_ROW);
        assertEquals(adrIds, indexedAdrs, "índice de ADRs deve listar exatamente os ADRs existentes");

        Set<String> invariantIds = definitions(DOCS.resolve("architecture/invariants.md"), INVARIANT_HEADING);
        Set<String> evalIds = definitions(DOCS.resolve("evals/semantic-eval-catalog.md"), EVAL_ROW);
        Set<String> backlogIds = definitions(DOCS.resolve("work/backlog.md"), BACKLOG_HEADING);
        assertFalse(invariantIds.isEmpty(), "nenhum invariant canônico encontrado");
        assertFalse(evalIds.isEmpty(), "nenhum eval canônico encontrado");
        assertFalse(backlogIds.isEmpty(), "nenhum item de backlog canônico encontrado");

        Map<String, String> owners = new HashMap<>();
        registerUnique(owners, adrIds, "ADRs");
        registerUnique(owners, invariantIds, "invariants");
        registerUnique(owners, evalIds, "evals");
        registerUnique(owners, backlogIds, "backlog");

        List<String> unresolved = new ArrayList<>();
        for (Path document : canonicalMarkdownDocuments()) {
            Matcher references = CANONICAL_ID.matcher(read(document));
            while (references.find()) {
                String id = references.group();
                if (!owners.containsKey(id)) {
                    unresolved.add(ROOT.relativize(document) + " -> " + id);
                }
            }
        }
        assertTrue(unresolved.isEmpty(), "IDs canônicos sem definição:\n" + String.join("\n", unresolved));
    }

    private static List<Path> markdownDocuments() throws IOException {
        List<Path> documents = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(DOCS)) {
            documents.addAll(paths.filter(path -> path.toString().endsWith(".md")).sorted().toList());
        }
        documents.add(ROOT.resolve("README.md"));
        documents.add(ROOT.resolve("ARCHITECTURE.md"));
        return documents;
    }

    private static List<Path> canonicalMarkdownDocuments() throws IOException {
        return markdownDocuments().stream()
                .filter(path -> !path.startsWith(DOCS.resolve("history")))
                .filter(path -> !path.startsWith(DOCS.resolve("_migration")))
                .toList();
    }

    private static Set<String> definitions(Path document, Pattern pattern) throws IOException {
        Set<String> ids = new HashSet<>();
        Matcher matcher = pattern.matcher(read(document));
        while (matcher.find()) {
            assertTrue(ids.add(matcher.group(1)), "ID duplicado em " + ROOT.relativize(document) + ": " + matcher.group(1));
        }
        return ids;
    }

    private static void registerUnique(Map<String, String> owners, Set<String> ids, String owner) {
        for (String id : ids) {
            String previous = owners.putIfAbsent(id, owner);
            assertTrue(previous == null, "ID " + id + " definido em " + previous + " e " + owner);
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
