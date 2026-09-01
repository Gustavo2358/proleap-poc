package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final String PLANNED_PATH_PREFIX = "planned:";
    private static final Pattern MARKDOWN_LINK = Pattern.compile("(?<!!)\\[[^]]+]\\(([^)]+)\\)");
    private static final Pattern ADR_HEADING = Pattern.compile("(?m)^# (ADR-\\d{4}) — ");
    private static final Pattern ADR_INDEX_ROW = Pattern.compile("(?m)^\\| (ADR-\\d{4}) \\|");
    private static final Pattern INVARIANT_HEADING = Pattern.compile("(?m)^### (INV-[A-Z]+-\\d{3}) — ");
    private static final Pattern EVAL_ROW = Pattern.compile("(?m)^\\| (EVAL-[A-Z0-9-]+-\\d{3}) \\|");
    private static final Pattern BACKLOG_HEADING = Pattern.compile("(?m)^### (BACKLOG-[A-Z]+-\\d{3}) — ");
    private static final Pattern CANONICAL_ID = Pattern.compile(
            "\\b(?:ADR-\\d{4}|INV-[A-Z]+-\\d{3}|EVAL-[A-Z0-9-]+-\\d{3}|BACKLOG-[A-Z]+-\\d{3})\\b");
    private static final Pattern YAML_KEY = Pattern.compile("^([a-z_]+):(?:\\s*(.*))?$");
    private static final Pattern YAML_LIST_ITEM = Pattern.compile("^  - (.+)$");
    private static final Set<String> WORK_ITEM_FIELDS = Set.of("id", "title", "status", "risk", "goal",
            "must_read", "related_domain_rules", "related_decisions", "related_invariants", "evals",
            "source_scope", "test_scope", "must_not_change", "gates");
    private static final Set<String> HARNESS_GATES = Set.of("docs", "fast", "architecture", "semantic",
            "performance", "full");

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
    void agentsFileRoutesWithoutLoadingHistoryByDefault() throws Exception {
        Path agents = ROOT.resolve("AGENTS.md");
        assertTrue(Files.isRegularFile(agents), "AGENTS.md raiz ausente");
        String content = read(agents);
        for (String section : List.of("Escopo", "Propósito", "Pipeline", "Regras universais",
                "Roteamento inicial", "Trabalho ativo", "Contexto histórico", "Verificação")) {
            assertTrue(content.contains("## " + section + "\n"), "seção ausente em AGENTS.md: " + section);
        }
        assertTrue(content.contains("docs/work/index.md"), "AGENTS.md não roteia para o índice de trabalho");
        assertFalse(content.contains("docs/history/evidence/"), "AGENTS.md não deve rotear diretamente para reports");
    }

    @Test
    void readmeRemainsAHumanFacingBridgeToCanonicalDocumentation() throws Exception {
        String content = read(ROOT.resolve("README.md"));
        for (String section : List.of("Requisitos", "Gerar e abrir", "Exemplos", "Verificação", "Documentação")) {
            assertTrue(content.contains("## " + section + "\n"), "seção ausente no README: " + section);
        }
        for (String destination : List.of("docs/index.md", "docs/architecture/index.md",
                "docs/domain/index.md", "docs/engineering/index.md", "docs/evals/index.md")) {
            assertTrue(content.contains(destination), "README não aponta para documentação canônica: " + destination);
        }
        assertFalse(content.contains("## Contrato da AST"),
                "contratos semânticos detalhados devem morar nos documentos de domínio");
        assertFalse(content.contains("## Contrato da tabela de símbolos"),
                "contratos semânticos detalhados devem morar nos documentos de domínio");
    }

    @Test
    void activeWorkItemsFollowTheRoutingProtocol() throws Exception {
        Path active = DOCS.resolve("work/active");
        assertTrue(Files.isRegularFile(DOCS.resolve("engineering/work-item-protocol.md")),
                "protocolo de work items ausente");
        List<Path> workItems;
        if (Files.isDirectory(active)) {
            try (Stream<Path> directories = Files.list(active)) {
                workItems = directories.filter(Files::isDirectory).sorted().toList();
            }
        } else {
            workItems = List.of();
        }
        Set<String> knownIds = knownCanonicalIds();
        for (Path workItem : workItems) {
            assertTrue(workItem.getFileName().toString().matches("WORK-[A-Z0-9-]+-\\d{3}"),
                    "diretório de work item inválido: " + ROOT.relativize(workItem));
            assertEquals(Set.of("work-item.yaml", "spec.md", "plan.md", "eval.md", "state.md"),
                    directFileNames(workItem), "estrutura inválida: " + ROOT.relativize(workItem));

            Map<String, List<String>> yaml = simpleYaml(workItem.resolve("work-item.yaml"));
            assertTrue(yaml.keySet().containsAll(WORK_ITEM_FIELDS),
                    "campos obrigatórios ausentes em " + ROOT.relativize(workItem));
            for (String field : WORK_ITEM_FIELDS) {
                assertFalse(yaml.get(field).isEmpty(), "campo vazio " + field + " em " + ROOT.relativize(workItem));
            }
            assertEquals(workItem.getFileName().toString(), scalar(yaml, "id"), "ID e diretório divergem");
            assertTrue(Set.of("active", "blocked").contains(scalar(yaml, "status")), "status ativo inválido");
            assertTrue(Set.of("low", "medium", "high").contains(scalar(yaml, "risk")), "risk inválido");

            assertPathsExist(yaml.get("must_read"), "must_read");
            assertPathsExist(yaml.get("related_domain_rules"), "related_domain_rules");
            assertPathsExist(yaml.get("source_scope"), "source_scope");
            assertPathsExist(yaml.get("test_scope"), "test_scope");
            assertKnownIds(yaml.get("related_decisions"), knownIds, "related_decisions");
            assertKnownIds(yaml.get("related_invariants"), knownIds, "related_invariants");
            assertKnownIds(yaml.get("evals"), knownIds, "evals");
            for (String gate : yaml.get("gates")) {
                assertTrue(HARNESS_GATES.contains(gate), "gate desconhecido: " + gate);
            }

            assertSections(workItem.resolve("spec.md"), List.of("Problema", "Objetivo", "Domínio de entrada suportado",
                    "Classes semânticas", "Premissas", "Comportamento esperado", "Comportamento diante de incerteza",
                    "Fora de escopo", "Regras de domínio relacionadas", "ADRs/invariantes relacionados"));
            assertSections(workItem.resolve("plan.md"), List.of("Fatiamento", "Dependências",
                    "Superfície arquitetural provável", "Migrações requeridas", "Artefatos esperados"));
            assertSections(workItem.resolve("eval.md"), List.of("O que prova corretude", "Classes positivas",
                    "Classes negativas", "Classes ambíguas", "Casos adversariais", "Casos de regressão",
                    "Propriedades/relações metamórficas", "Expectativas de escala"));
            assertSections(workItem.resolve("state.md"), List.of("Onde estamos", "Verde conhecido", "Restante",
                    "Descobertas que afetam o plano"));
        }
    }

    @Test
    void completedHarnessMigrationIsArchivedOutsideDefaultRouting() throws Exception {
        Path archive = DOCS.resolve("history/harness-v1-migration");
        for (String document : List.of("README.md", "HARNESS_ENGINEERING_IMPLEMENTATION_PLAN.md",
                "knowledge-migration-matrix.md", "source-inventory.md", "open-conflicts.md")) {
            assertTrue(Files.isRegularFile(archive.resolve(document)), "arquivo histórico ausente: " + document);
        }
        String matrix = read(archive.resolve("knowledge-migration-matrix.md"));
        assertFalse(matrix.contains("| UNMAPPED |"), "a matriz final não pode conter conhecimento sem destino");
        assertFalse(matrix.contains("| UNCERTAIN |"), "a matriz final não pode conter conhecimento incerto");
        assertFalse(Files.exists(ROOT.resolve("specs/HARNESS_ENGINEERING_IMPLEMENTATION_PLAN.md")),
                "o plano-base concluído não deve continuar em specs/");
        assertTrue(Files.isRegularFile(DOCS.resolve("work/history/WORK-HARNESS-001.md")),
                "resumo do work item concluído ausente");
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
            documents.addAll(paths.filter(path -> path.toString().endsWith(".md"))
                    .filter(path -> !path.startsWith(DOCS.resolve("history"))).sorted().toList());
        }
        documents.add(ROOT.resolve("README.md"));
        documents.add(ROOT.resolve("ARCHITECTURE.md"));
        documents.add(ROOT.resolve("AGENTS.md"));
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

    private static Set<String> knownCanonicalIds() throws IOException {
        Set<String> ids = new HashSet<>();
        Path decisions = DOCS.resolve("architecture/decisions");
        try (Stream<Path> files = Files.list(decisions)) {
            for (Path adr : files.filter(path -> path.getFileName().toString().matches("\\d{4}-.*\\.md")).toList()) {
                ids.addAll(definitions(adr, ADR_HEADING));
            }
        }
        ids.addAll(definitions(DOCS.resolve("architecture/invariants.md"), INVARIANT_HEADING));
        ids.addAll(definitions(DOCS.resolve("evals/semantic-eval-catalog.md"), EVAL_ROW));
        ids.addAll(definitions(DOCS.resolve("work/backlog.md"), BACKLOG_HEADING));
        return ids;
    }

    private static Set<String> directFileNames(Path directory) throws IOException {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(java.util.stream.Collectors.toSet());
        }
    }

    private static Map<String, List<String>> simpleYaml(Path yaml) throws IOException {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        String current = null;
        for (String line : read(yaml).split("\\R")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            Matcher key = YAML_KEY.matcher(line);
            if (key.matches()) {
                current = key.group(1);
                assertTrue(fields.putIfAbsent(current, new ArrayList<>()) == null,
                        "campo YAML duplicado em " + ROOT.relativize(yaml) + ": " + current);
                if (!key.group(2).isBlank()) fields.get(current).add(key.group(2));
                continue;
            }
            Matcher item = YAML_LIST_ITEM.matcher(line);
            assertTrue(item.matches() && current != null, "YAML fora do subconjunto permitido em "
                    + ROOT.relativize(yaml) + ": " + line);
            fields.get(current).add(item.group(1));
        }
        return fields;
    }

    private static String scalar(Map<String, List<String>> yaml, String field) {
        assertEquals(1, yaml.get(field).size(), "campo escalar inválido: " + field);
        return yaml.get(field).get(0);
    }

    private static void assertPathsExist(List<String> paths, String field) {
        for (String value : paths) {
            if (value.startsWith(PLANNED_PATH_PREFIX)) {
                assertTrue(field.equals("source_scope") || field.equals("test_scope"),
                        "caminho planejado permitido somente em source_scope/test_scope: " + value);
                String path = value.substring(PLANNED_PATH_PREFIX.length());
                assertFalse(path.isBlank() || path.contains("#"), "caminho planejado inválido: " + value);
                Path resolved = ROOT.resolve(path).normalize();
                Path expectedRoot = ROOT.resolve(field.equals("source_scope") ? "src/main" : "src/test");
                assertTrue(resolved.startsWith(expectedRoot),
                        field + " planejado fora da árvore esperada: " + value);
                assertTrue(Files.isDirectory(resolved.getParent()),
                        field + " planejado sem diretório pai existente: " + value);
                assertFalse(Files.exists(resolved), field + " planejado já existe: " + value);
                continue;
            }
            String path = value.split("#", 2)[0];
            assertTrue(Files.exists(ROOT.resolve(path)), field + " inexistente: " + value);
        }
    }

    private static void assertKnownIds(List<String> ids, Set<String> knownIds, String field) {
        for (String id : ids) assertTrue(knownIds.contains(id), field + " desconhecido: " + id);
    }

    private static void assertSections(Path document, List<String> sections) throws IOException {
        String content = read(document);
        for (String section : sections) {
            assertTrue(content.contains("## " + section + "\n"), "seção ausente em "
                    + ROOT.relativize(document) + ": " + section);
        }
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
