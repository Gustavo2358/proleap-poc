package io.github.gustavo2358.cobolexplorer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Versioned resolution classification expanded over every grammar rule.
 *
 * <p>The semantic grammar manifest is the exhaustive source of rule keys. Exact
 * overrides identify occurrence origins and qualifier/relation forms. All other
 * rules inherit a conservative container/boundary classification; none becomes
 * supported merely because it appears in the corpus.</p>
 */
final class ReferenceResolutionManifest {
    static final String VERSION = "1.1.0";

    enum RuleClass {
        REFERENCE_ORIGIN,
        CONTEXTUAL_REFERENCE_ORIGIN,
        QUALIFIER_COMPONENT,
        DECLARATION_RELATION,
        REFERENCE_CONTAINER,
        BUILTIN_NO_BINDING,
        PRESERVED_UNKNOWN,
        INPUT_BOUNDARY,
        NOT_REFERENCE
    }

    record Entry(GrammarCoverageManifest.RuleKey key, RuleClass ruleClass,
                 ResolutionContracts.ReferenceKind referenceKind,
                 String policySection, String rationale) {
        Entry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(ruleClass, "ruleClass");
            policySection = requireText(policySection, "policySection");
            rationale = requireText(rationale, "rationale");
            boolean kindRequired = ruleClass == RuleClass.REFERENCE_ORIGIN
                    || ruleClass == RuleClass.QUALIFIER_COMPONENT
                    || ruleClass == RuleClass.DECLARATION_RELATION;
            if (kindRequired && referenceKind == null)
                throw new IllegalArgumentException(ruleClass + " requires referenceKind for " + key);
            if (ruleClass == RuleClass.CONTEXTUAL_REFERENCE_ORIGIN && referenceKind != null)
                throw new IllegalArgumentException(ruleClass + " must not carry referenceKind for " + key);
        }
    }

    private record Override(RuleClass ruleClass, ResolutionContracts.ReferenceKind kind,
                            String policySection, String rationale) { }

    private static final Map<String, Override> COBOL_OVERRIDES = overrides();
    private static final List<Entry> ENTRIES = build();
    private static final Map<GrammarCoverageManifest.RuleKey, Entry> INDEX = index(ENTRIES);

    private ReferenceResolutionManifest() { }

    static List<Entry> entries() { return ENTRIES; }

    static Entry entry(GrammarCoverageManifest.Grammar grammar, String rule) {
        Entry result = INDEX.get(new GrammarCoverageManifest.RuleKey(grammar, rule));
        if (result == null) throw new IllegalArgumentException("unclassified rule " + grammar + ':' + rule);
        return result;
    }

    private static List<Entry> build() {
        List<Entry> result = GrammarCoverageManifest.entries().stream()
                .map(ReferenceResolutionManifest::classify).toList();
        Set<String> grammarRules = result.stream()
                .filter(entry -> entry.key().grammar() == GrammarCoverageManifest.Grammar.COBOL)
                .map(entry -> entry.key().rule()).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!grammarRules.containsAll(COBOL_OVERRIDES.keySet())) {
            Set<String> missing = new LinkedHashSet<>(COBOL_OVERRIDES.keySet());
            missing.removeAll(grammarRules);
            throw new ExceptionInInitializerError("resolution overrides absent from grammar: " + missing);
        }
        return result;
    }

    private static Map<GrammarCoverageManifest.RuleKey, Entry> index(List<Entry> entries) {
        Map<GrammarCoverageManifest.RuleKey, Entry> result = new LinkedHashMap<>();
        for (Entry entry : entries) {
            if (result.put(entry.key(), entry) != null)
                throw new ExceptionInInitializerError("duplicate resolution entry " + entry.key());
        }
        return Map.copyOf(result);
    }

    private static Entry classify(GrammarCoverageManifest.Entry semantic) {
        GrammarCoverageManifest.RuleKey key = semantic.key();
        if (key.grammar() == GrammarCoverageManifest.Grammar.PREPROCESSOR) {
            return new Entry(key, RuleClass.INPUT_BOUNDARY, null, "preprocessing-input",
                    "Preprocessor syntax controls input/provenance and can block complete binding; "
                            + semantic.rationale());
        }

        Override override = COBOL_OVERRIDES.get(key.rule());
        if (override != null) {
            return new Entry(key, override.ruleClass(), override.kind(),
                    override.policySection(), override.rationale());
        }

        if (semantic.dependencyKnowledge() == SemanticCoverage.DependencyKnowledge.REFERENCE_READY) {
            return new Entry(key, RuleClass.REFERENCE_CONTAINER, null, "ast-reference-containers",
                    "Grammar container may carry structured child occurrences; children, not source text, are inspected. "
                            + semantic.rationale());
        }
        if (semantic.dependencyKnowledge() == SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN
                || semantic.coverage() == SemanticCoverage.ConstructionCoverage.UNSUPPORTED
                || semantic.coverage() == SemanticCoverage.ConstructionCoverage.INPUT_MISSING) {
            return new Entry(key, RuleClass.PRESERVED_UNKNOWN, null, "conservative-coverage",
                    "Rule is not claimed as bindable until a typed occurrence contract exists. "
                            + semantic.rationale());
        }
        return new Entry(key, RuleClass.NOT_REFERENCE, null, "non-reference-grammar",
                "Rule is not itself an occurrence origin; structured descendants remain independently classified. "
                        + semantic.rationale());
    }

    private static Map<String, Override> overrides() {
        Map<String, Override> result = new LinkedHashMap<>();
        origin(result, "qualifiedDataName", ResolutionContracts.ReferenceKind.DATA,
                "data-qualification", "Structured DATA/condition reference origin; exact format is interpreted later.");
        contextual(result, "conditionNameReference", "condition-names",
                "Condition-name surface origin may be standalone or a contextual condition tail; occurrence policy is decided by typed AST position and nominal shape.");
        origin(result, "procedureName", ResolutionContracts.ReferenceKind.PROCEDURE,
                "procedure-names", "Paragraph or section occurrence, optionally section-qualified.");
        origin(result, "fileName", ResolutionContracts.ReferenceKind.FILE,
                "file-entities", "File connector occurrence; SELECT and FD/SD entity relations are handled later.");
        origin(result, "programName", ResolutionContracts.ReferenceKind.PROGRAM,
                "program-visibility", "Program occurrence for internal visibility or external literal observation.");
        origin(result, "indexName", ResolutionContracts.ReferenceKind.INDEX,
                "index-names", "OCCURS index occurrence with DATA-scope visibility.");

        qualifier(result, "inData", ResolutionContracts.ReferenceKind.DATA, "data-qualification");
        qualifier(result, "inTable", ResolutionContracts.ReferenceKind.DATA, "data-qualification");
        qualifier(result, "qualifiedInData", ResolutionContracts.ReferenceKind.DATA, "data-qualification");
        qualifier(result, "inFile", ResolutionContracts.ReferenceKind.FILE, "file-qualification");
        qualifier(result, "inSection", ResolutionContracts.ReferenceKind.PROCEDURE, "procedure-names");
        qualifier(result, "inLibrary", ResolutionContracts.ReferenceKind.PRESERVED_NAMED, "library-names");

        relation(result, "dataRedefinesClause", ResolutionContracts.ReferenceKind.DATA, "data-relations");
        relation(result, "dataRenamesClause", ResolutionContracts.ReferenceKind.DATA, "data-relations");
        relation(result, "dataOccursDepending", ResolutionContracts.ReferenceKind.DATA, "occurs-relations");
        relation(result, "dataOccursSort", ResolutionContracts.ReferenceKind.DATA, "occurs-relations");
        relation(result, "dataOccursIndexed", ResolutionContracts.ReferenceKind.INDEX, "occurs-relations");

        result.put("specialRegister", new Override(RuleClass.BUILTIN_NO_BINDING, null,
                "builtins", "COBOL special register is intrinsic and does not bind to a user declaration."));
        return Map.copyOf(result);
    }

    private static void origin(Map<String, Override> result, String rule,
                               ResolutionContracts.ReferenceKind kind, String section, String rationale) {
        putUnique(result, rule, new Override(RuleClass.REFERENCE_ORIGIN, kind, section, rationale));
    }

    private static void contextual(Map<String, Override> result, String rule,
                                   String section, String rationale) {
        putUnique(result, rule, new Override(RuleClass.CONTEXTUAL_REFERENCE_ORIGIN, null, section, rationale));
    }

    private static void qualifier(Map<String, Override> result, String rule,
                                  ResolutionContracts.ReferenceKind kind, String section) {
        putUnique(result, rule, new Override(RuleClass.QUALIFIER_COMPONENT, kind, section,
                "Qualifier component narrows a containing occurrence and is not a value read."));
    }

    private static void relation(Map<String, Override> result, String rule,
                                 ResolutionContracts.ReferenceKind kind, String section) {
        putUnique(result, rule, new Override(RuleClass.DECLARATION_RELATION, kind, section,
                "Declaration relation has a typed binding role but remains separate from the AST."));
    }

    private static void putUnique(Map<String, Override> result, String rule, Override override) {
        if (result.put(rule, override) != null) throw new IllegalStateException("duplicate override " + rule);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
