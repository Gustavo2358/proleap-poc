package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceResolutionManifestTest {
    @Test
    void classifiesAllVersionedGrammarRulesWithoutUsingTheCorpus() {
        List<ReferenceResolutionManifest.Entry> entries = ReferenceResolutionManifest.entries();
        Map<GrammarCoverageManifest.RuleKey, ReferenceResolutionManifest.Entry> indexed = entries.stream()
                .collect(Collectors.toMap(ReferenceResolutionManifest.Entry::key, Function.identity()));

        assertEquals(628, entries.size());
        assertEquals(entries.size(), indexed.size());
        assertEquals(GrammarCoverageManifest.entries().stream().map(GrammarCoverageManifest.Entry::key).toList(),
                entries.stream().map(ReferenceResolutionManifest.Entry::key).toList());
        assertTrue(entries.stream().allMatch(entry -> !entry.rationale().isBlank()));
        assertTrue(entries.stream().allMatch(entry -> !entry.policySection().isBlank()));

        assertEntry(indexed, "qualifiedDataName", ReferenceResolutionManifest.RuleClass.REFERENCE_ORIGIN,
                ResolutionContracts.ReferenceKind.DATA);
        assertEntry(indexed, "procedureName", ReferenceResolutionManifest.RuleClass.REFERENCE_ORIGIN,
                ResolutionContracts.ReferenceKind.PROCEDURE);
        assertEntry(indexed, "fileName", ReferenceResolutionManifest.RuleClass.REFERENCE_ORIGIN,
                ResolutionContracts.ReferenceKind.FILE);
        assertEntry(indexed, "programName", ReferenceResolutionManifest.RuleClass.REFERENCE_ORIGIN,
                ResolutionContracts.ReferenceKind.PROGRAM);
        assertEntry(indexed, "indexName", ReferenceResolutionManifest.RuleClass.REFERENCE_ORIGIN,
                ResolutionContracts.ReferenceKind.INDEX);
        assertEntry(indexed, "inFile", ReferenceResolutionManifest.RuleClass.QUALIFIER_COMPONENT,
                ResolutionContracts.ReferenceKind.FILE);
        assertEntry(indexed, "specialRegister", ReferenceResolutionManifest.RuleClass.BUILTIN_NO_BINDING,
                null);
        assertEntry(indexed, "execSqlStatement", ReferenceResolutionManifest.RuleClass.PRESERVED_UNKNOWN,
                null);
        assertEntry(indexed, GrammarCoverageManifest.Grammar.PREPROCESSOR, "copyStatement",
                ReferenceResolutionManifest.RuleClass.INPUT_BOUNDARY, null);
    }

    @Test
    void exposesImmutableVersionedContractsBeforeAnyResolverExists() {
        ResolutionContracts.ProgramUnitId unit = new ResolutionContracts.ProgramUnitId(
                "bank/source/ACCT.cbl", List.of(0, 2), "ACCT01");
        ResolutionContracts.CobolResolutionPolicy policy = ResolutionContracts.CobolResolutionPolicy.initial();
        ResolutionContracts.Completeness incomplete = new ResolutionContracts.Completeness(
                false, false, List.of("CALL_LINKAGE_UNKNOWN"));

        assertEquals(List.of(0, 2), unit.structuralPath());
        assertThrows(UnsupportedOperationException.class, () -> unit.structuralPath().add(3));
        assertFalse(policy.policyId().isBlank());
        assertFalse(policy.version().isBlank());
        assertEquals(ResolutionContracts.QualifyMode.UNSPECIFIED, policy.qualifyMode());
        assertEquals(List.of("CALL_LINKAGE_UNKNOWN"), incomplete.blockingReasons());
        assertThrows(UnsupportedOperationException.class,
                () -> incomplete.blockingReasons().add("MUTATION"));

        assertEquals(EnumSet.of(ResolutionContracts.ResolutionStatus.RESOLVED,
                        ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED,
                        ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionStatus.UNSUPPORTED),
                EnumSet.allOf(ResolutionContracts.ResolutionStatus.class));
        assertTrue(Set.of(ResolutionContracts.ReferenceRole.values()).containsAll(Set.of(
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ReferenceRole.VALUE_WRITE,
                ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT,
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ReferenceRole.GO_TO_TARGET,
                ResolutionContracts.ReferenceRole.PERFORM_THROUGH)));
        assertTrue(Set.of(ResolutionContracts.ResolutionReason.values()).containsAll(Set.of(
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                ResolutionContracts.ResolutionReason.LITERAL_EXTERNAL_PROGRAM,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND,
                ResolutionContracts.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM)));
    }

    private static void assertEntry(Map<GrammarCoverageManifest.RuleKey, ReferenceResolutionManifest.Entry> entries,
                                    String rule, ReferenceResolutionManifest.RuleClass ruleClass,
                                    ResolutionContracts.ReferenceKind kind) {
        assertEntry(entries, GrammarCoverageManifest.Grammar.COBOL, rule, ruleClass, kind);
    }

    private static void assertEntry(Map<GrammarCoverageManifest.RuleKey, ReferenceResolutionManifest.Entry> entries,
                                    GrammarCoverageManifest.Grammar grammar, String rule,
                                    ReferenceResolutionManifest.RuleClass ruleClass,
                                    ResolutionContracts.ReferenceKind kind) {
        ReferenceResolutionManifest.Entry entry = entries.get(new GrammarCoverageManifest.RuleKey(grammar, rule));
        assertEquals(ruleClass, entry.ruleClass(), grammar + ":" + rule);
        assertEquals(kind, entry.referenceKind(), grammar + ":" + rule);
    }
}
