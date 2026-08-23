package io.proleap.benchmark;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrammarCoverageManifestTest {
    private static final Pattern PARSER_RULE = Pattern.compile("(?m)^([a-z][A-Za-z0-9_]*)\\s*\\R\\s*:");
    private static final Pattern STATEMENT_RULE = Pattern.compile("(?ms)^statement\\s*\\R\\s*:(.*?);");
    private static final Pattern STATEMENT_ALTERNATIVE = Pattern.compile("[a-z][A-Za-z0-9_]*Statement");
    private static final Set<String> STATEMENTS = Set.of(
            "acceptStatement", "addStatement", "alterStatement", "callStatement", "cancelStatement",
            "closeStatement", "computeStatement", "continueStatement", "deleteStatement", "disableStatement",
            "displayStatement", "divideStatement", "enableStatement", "entryStatement", "evaluateStatement",
            "exhibitStatement", "execCicsStatement", "execSqlStatement", "execSqlImsStatement", "exitStatement",
            "generateStatement", "gobackStatement", "goToStatement", "ifStatement", "initializeStatement",
            "initiateStatement", "inspectStatement", "mergeStatement", "moveStatement", "multiplyStatement",
            "nextSentenceStatement", "openStatement", "performStatement", "purgeStatement", "readStatement",
            "receiveStatement", "releaseStatement", "returnStatement", "rewriteStatement", "searchStatement",
            "sendStatement", "setStatement", "sortStatement", "startStatement", "stopStatement",
            "stringStatement", "subtractStatement", "terminateStatement", "unstringStatement", "writeStatement");

    @Test
    void classifiesEveryParserRuleFromBothVersionedGrammars() throws Exception {
        Path antlr = Path.of("src/main/antlr4");
        Set<GrammarCoverageManifest.RuleKey> expected = new LinkedHashSet<>();
        expected.addAll(ruleKeys(GrammarCoverageManifest.Grammar.COBOL, antlr.resolve("Cobol.g4")));
        expected.addAll(ruleKeys(GrammarCoverageManifest.Grammar.PREPROCESSOR,
                antlr.resolve("CobolPreprocessor.g4")));

        List<GrammarCoverageManifest.Entry> entries = GrammarCoverageManifest.entries();
        Map<GrammarCoverageManifest.RuleKey, GrammarCoverageManifest.Entry> indexed = entries.stream()
                .collect(Collectors.toMap(GrammarCoverageManifest.Entry::key, Function.identity()));

        assertEquals(598, expected.stream().filter(key -> key.grammar() == GrammarCoverageManifest.Grammar.COBOL).count());
        assertEquals(30, expected.stream().filter(key -> key.grammar() == GrammarCoverageManifest.Grammar.PREPROCESSOR).count());
        assertEquals(628, entries.size());
        assertEquals(entries.size(), indexed.size(), "manifest cannot contain duplicate grammar/rule keys");
        assertEquals(expected, indexed.keySet(), "grammar changes must be explicitly classified in the manifest");
        assertFalse(entries.stream().anyMatch(entry -> entry.rationale().isBlank()));
    }

    @Test
    void explicitlyClassifiesAllFiftyStatementAlternativesConservatively() throws Exception {
        String grammar = Files.readString(Path.of("src/main/antlr4/Cobol.g4"), StandardCharsets.UTF_8);
        Matcher statementRule = STATEMENT_RULE.matcher(grammar);
        assertTrue(statementRule.find(), "statement rule must exist");
        Matcher alternatives = STATEMENT_ALTERNATIVE.matcher(statementRule.group(1));
        Set<String> grammarStatements = new LinkedHashSet<>();
        while (alternatives.find()) grammarStatements.add(alternatives.group());

        assertEquals(50, STATEMENTS.size());
        assertEquals(STATEMENTS, grammarStatements,
                "every new or removed statement alternative requires an explicit manifest review");
        Map<String, GrammarCoverageManifest.Entry> statements = GrammarCoverageManifest.entries().stream()
                .filter(entry -> entry.key().grammar() == GrammarCoverageManifest.Grammar.COBOL)
                .filter(entry -> STATEMENTS.contains(entry.key().rule()))
                .collect(Collectors.toMap(entry -> entry.key().rule(), Function.identity()));
        assertEquals(STATEMENTS, statements.keySet());
        assertEquals(Set.of(GrammarCoverageManifest.RuleFamily.STATEMENT),
                statements.values().stream().map(GrammarCoverageManifest.Entry::family).collect(Collectors.toSet()));

        assertEntry("callStatement", SemanticCoverage.ConstructionCoverage.MODELED,
                SemanticCoverage.DependencyKnowledge.REFERENCE_READY);
        assertEntry("setStatement", SemanticCoverage.ConstructionCoverage.UNSUPPORTED,
                SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN);
        assertEntry("execSqlStatement", SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED,
                SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN);
    }

    @Test
    void exposesConservativePoliciesForReferencesDeclarationsAndPreprocessing() {
        assertEntry(GrammarCoverageManifest.Grammar.COBOL, "qualifiedDataName",
                SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED,
                SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN);
        assertEntry(GrammarCoverageManifest.Grammar.COBOL, "dataRedefinesClause",
                SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED,
                SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN);
        assertEntry(GrammarCoverageManifest.Grammar.PREPROCESSOR, "copyStatement",
                SemanticCoverage.ConstructionCoverage.PRESERVED_UNINTERPRETED,
                SemanticCoverage.DependencyKnowledge.DEPENDENCY_UNKNOWN);
    }

    private static void assertEntry(String rule, SemanticCoverage.ConstructionCoverage coverage,
                                    SemanticCoverage.DependencyKnowledge knowledge) {
        assertEntry(GrammarCoverageManifest.Grammar.COBOL, rule, coverage, knowledge);
    }

    private static void assertEntry(GrammarCoverageManifest.Grammar grammar, String rule,
                                    SemanticCoverage.ConstructionCoverage coverage,
                                    SemanticCoverage.DependencyKnowledge knowledge) {
        GrammarCoverageManifest.Entry entry = GrammarCoverageManifest.entry(grammar, rule);
        assertEquals(coverage, entry.coverage());
        assertEquals(knowledge, entry.dependencyKnowledge());
    }

    private static Set<GrammarCoverageManifest.RuleKey> ruleKeys(GrammarCoverageManifest.Grammar grammar,
                                                                 Path grammarFile) throws Exception {
        String source = Files.readString(grammarFile, StandardCharsets.UTF_8);
        Matcher matcher = PARSER_RULE.matcher(source);
        Set<GrammarCoverageManifest.RuleKey> result = new LinkedHashSet<>();
        while (matcher.find()) result.add(new GrammarCoverageManifest.RuleKey(grammar, matcher.group(1)));
        return result;
    }
}
