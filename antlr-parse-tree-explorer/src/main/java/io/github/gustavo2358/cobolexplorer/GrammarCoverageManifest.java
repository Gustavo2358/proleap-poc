package io.github.gustavo2358.cobolexplorer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Explicit, versioned classification of every parser rule in the two frontend grammars. */
final class GrammarCoverageManifest {
    enum Grammar { COBOL, PREPROCESSOR }

    enum RuleFamily {
        GRAMMAR_INFRASTRUCTURE,
        PROGRAM_STRUCTURE,
        ENVIRONMENT_DECLARATION,
        DATA_DECLARATION,
        PROCEDURE_STRUCTURE,
        STATEMENT,
        STATEMENT_COMPONENT,
        EXPRESSION_REFERENCE,
        NAME_LITERAL,
        PREPROCESSOR
    }

    record RuleKey(Grammar grammar, String rule) {
        RuleKey {
            Objects.requireNonNull(grammar, "grammar");
            if (rule == null || rule.isBlank()) throw new IllegalArgumentException("rule must not be blank");
        }
    }

    record Entry(RuleKey key, RuleFamily family, SemanticCoverage.ConstructionCoverage coverage,
                 SemanticCoverage.DependencyKnowledge dependencyKnowledge, String rationale) {
        Entry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(family, "family");
            Objects.requireNonNull(coverage, "coverage");
            Objects.requireNonNull(dependencyKnowledge, "dependencyKnowledge");
            if (rationale == null || rationale.isBlank())
                throw new IllegalArgumentException("rationale must not be blank");
        }
    }

    private static final String RESOURCE = "/semantic-coverage/grammar-rule-manifest.tsv";
    private static final List<Entry> ENTRIES = load();
    private static final Map<RuleKey, Entry> INDEX = index(ENTRIES);

    private GrammarCoverageManifest() { }

    static List<Entry> entries() { return ENTRIES; }

    static Entry entry(Grammar grammar, String rule) {
        Entry result = INDEX.get(new RuleKey(grammar, rule));
        if (result == null) throw new IllegalArgumentException("unclassified grammar rule: " + grammar + ":" + rule);
        return result;
    }

    private static List<Entry> load() {
        InputStream stream = GrammarCoverageManifest.class.getResourceAsStream(RESOURCE);
        if (stream == null) throw new ExceptionInInitializerError("missing " + RESOURCE);
        List<Entry> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] columns = line.split("\\t", -1);
                if (columns.length != 6)
                    throw new IllegalStateException("invalid manifest row " + lineNumber + ": expected 6 columns");
                result.add(new Entry(new RuleKey(Grammar.valueOf(columns[0]), columns[1]),
                        RuleFamily.valueOf(columns[2]),
                        SemanticCoverage.ConstructionCoverage.valueOf(columns[3]),
                        SemanticCoverage.DependencyKnowledge.valueOf(columns[4]), columns[5]));
            }
        } catch (IOException | RuntimeException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        return List.copyOf(result);
    }

    private static Map<RuleKey, Entry> index(List<Entry> entries) {
        Map<RuleKey, Entry> result = new LinkedHashMap<>();
        for (Entry entry : entries) {
            if (result.put(entry.key(), entry) != null)
                throw new ExceptionInInitializerError("duplicate manifest key " + entry.key());
        }
        return Map.copyOf(result);
    }
}
