package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Discovery-only characterization; this class deliberately does not change production behavior. */
class SemanticConditionContextDiscoveryTest {
    private static final Path MINIMAL = Path.of(
            "src/test/resources/cobol/resolution/abbreviated-condition-context.cbl");
    private static final String ALPHANUMERIC_RELATION_OPERANDS = "01 A PIC X.\n01 B PIC X.";
    private static final String NUMERIC_INTEGER_RELATION_OPERANDS = "01 A PIC 9(4).\n01 B PIC 9(4).";

    @Test
    void characterizesMinimalChainFromParseTreeThroughResolution() throws IOException {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                Files.readString(MINIMAL, StandardCharsets.UTF_8), MINIMAL.getFileName().toString());

        List<CobolParser.ConditionNameReferenceContext> parsedTails = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.ConditionNameReferenceContext.class);
        assertEquals(List.of("C", "D"), parsedTails.stream().map(context -> context.getText()).toList());
        assertEquals(0, AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.AbbreviationContext.class).size());

        Ast.IfStatement statement = AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).get(0);
        Ast.LogicalCondition logical = assertInstanceOf(Ast.LogicalCondition.class, statement.condition());
        Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class, logical.operands().get(0));
        assertAll("AST lowering preserves the written condition surface",
                () -> assertEquals(Ast.LogicalConnector.OR, logical.connector()),
                () -> assertEquals(3, logical.operands().size()),
                () -> assertEquals("=", relation.relationalOperator()),
                () -> assertEquals(List.of("C", "D"),
                        logical.operands().subList(1, 3).stream()
                                .map(operand -> assertInstanceOf(Ast.ContextualConditionTail.class, operand)
                                        .nominalReference().baseName()).toList()));

        List<ReferenceResolution.Entry> entries = valueReads(analysis);
        assertEquals(List.of("A", "B", "C", "D"), entries.stream()
                .map(entry -> entry.occurrence().writtenText()).toList());
        for (ReferenceResolution.Entry entry : entries.subList(0, 2)) {
            assertAll(entry.toString(),
                    () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX, entry.occurrence().kind()),
                    () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                            ResolutionContracts.ReferenceKind.INDEX), entry.occurrence().admissibleKinds()),
                    () -> assertEquals(ResolutionContracts.ReferenceRole.VALUE_READ, entry.occurrence().role()),
                    () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                    () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                            entry.selectedCandidate().orElseThrow().kind()));
        }
        for (ReferenceResolution.Entry entry : entries.subList(2, 4)) {
            assertAll(entry.toString(),
                    () -> assertEquals("conditionNameReference", entry.occurrence().grammarRule()),
                    () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, entry.occurrence().kind()),
                    () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                            entry.occurrence().admissibleKinds()),
                    () -> assertEquals(ResolutionContracts.ReferenceRole.VALUE_READ, entry.occurrence().role()),
                    () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, entry.status()),
                    () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                            entry.reason()),
                    () -> assertTrue(entry.candidates().isEmpty()),
                    () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM,
                            sameNameSymbols(analysis, entry.occurrence().writtenText()).get(0).kind()));
        }
    }

    @Test
    void characterizesNearbyFormsAndInformationLoss() {
        Map<String, String> conditions = adversarialConditions();
        Map<String, AstBoundaryTestSupport.Analysis> analyses = new LinkedHashMap<>();
        conditions.forEach((name, condition) -> analyses.put(name,
                AstBoundaryTestSupport.analyze(sourceWithCondition(name, condition), name + ".cbl")));
        assertEquals(19, analyses.size(), "every matrix form must remain accepted by the configured grammar");

        ReferenceResolution.Entry explicitC = entry(analyses.get("explicit-relation"), "C");
        ReferenceResolution.Entry abbreviatedC = entry(analyses.get("one-abbreviation"), "C");
        assertAll("explicit versus abbreviated",
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.ReferenceKind.INDEX), explicitC.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, explicitC.status()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                        abbreviatedC.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        abbreviatedC.reason()));

        Map<String, List<String>> inheritedDataTails = new LinkedHashMap<>();
        inheritedDataTails.put("one-abbreviation", List.of("C"));
        inheritedDataTails.put("two-abbreviations", List.of("C", "D"));
        inheritedDataTails.put("and-abbreviation", List.of("C"));
        inheritedDataTails.put("greater-abbreviation", List.of("C"));
        inheritedDataTails.put("not-equal-abbreviation", List.of("C"));
        inheritedDataTails.put("logical-not-abbreviation", List.of("C"));
        inheritedDataTails.put("mixed-and-or", List.of("C", "D"));
        inheritedDataTails.put("mixed-or-and", List.of("C", "D"));
        inheritedDataTails.put("grouped-whole", List.of("C"));
        inheritedDataTails.put("restart-then-abbreviate", List.of("E"));
        long rigidConditionOccurrences = inheritedDataTails.entrySet().stream()
                .flatMap(item -> item.getValue().stream().map(name -> entry(analyses.get(item.getKey()), name)))
                .peek(entry -> assertEquals("conditionNameReference", entry.occurrence().grammarRule()))
                .peek(entry -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        entry.reason(), entry.toString()))
                .count();
        assertEquals(13, rigidConditionOccurrences);

        for (Map.Entry<String, String> boundary : Map.of(
                "grouped-whole", "D",
                "grouped-left", "C").entrySet()) {
            ReferenceResolution.Entry standalone = entry(analyses.get(boundary.getKey()), boundary.getValue());
            assertAll(boundary.getKey() + " closes inherited relation context before " + boundary.getValue(),
                    () -> assertEquals("conditionNameReference", standalone.occurrence().grammarRule()),
                    () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                            standalone.occurrence().admissibleKinds()),
                    () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                            standalone.reason()));
        }

        for (String name : List.of("distributed-or", "distributed-and")) {
            Ast.Expression expression = AstBoundaryTestSupport.nodes(analyses.get(name), Ast.IfStatement.class)
                    .get(0).condition();
            Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class, expression);
            assertInstanceOf(Ast.DistributedOperandGroup.class, relation.object());
            assertAll(name + " distribution surface and relation-operand classification",
                    () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                    ResolutionContracts.ReferenceKind.INDEX),
                            entry(analyses.get(name), "A").occurrence().admissibleKinds(),
                            "the distributed subject is a relation operand"),
                    () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                    ResolutionContracts.ReferenceKind.INDEX),
                            entry(analyses.get(name), "B").occurrence().admissibleKinds(),
                            "distributed operands reuse the relation-operand policy"),
                    () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                    ResolutionContracts.ReferenceKind.INDEX),
                            entry(analyses.get(name), "C").occurrence().admissibleKinds(),
                            "distributed operands reuse the relation-operand policy"));
        }

        Ast.RelationCondition statedOperator = AstBoundaryTestSupport.nodes(
                analyses.get("stated-new-operator"), Ast.RelationCondition.class).stream()
                .filter(relation -> relation.subject() == null).findFirst().orElseThrow();
        assertAll("explicit operator abbreviation",
                () -> assertEquals("abbreviation", statedOperator.meta().origin().grammarRule()),
                () -> assertNull(statedOperator.subject(), "subject stays omitted on the surface"),
                () -> assertEquals("<", statedOperator.relationalOperator()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        entry(analyses.get("stated-new-operator"), "C").occurrence().admissibleKinds(),
                        "the abbreviated object is a modeled relation operand"));

        AstBoundaryTestSupport.Analysis multiple = analyses.get("multiple-abbreviations-one-tail");
        assertAll("the grammar accepts two abbreviation children and the surface keeps both",
                () -> assertEquals(2, AstBoundaryTestSupport.contexts(
                        multiple.tree(), CobolParser.AbbreviationContext.class).size()),
                () -> assertTrue(valueReads(multiple).stream().anyMatch(entry ->
                        entry.occurrence().writtenText().equals("C"))),
                () -> assertTrue(valueReads(multiple).stream().anyMatch(entry ->
                        entry.occurrence().writtenText().equals("D"))));

        for (String name : List.of("mixed-and-or", "mixed-or-and")) {
            Ast.LogicalCondition mixed = assertInstanceOf(Ast.LogicalCondition.class,
                    AstBoundaryTestSupport.nodes(analyses.get(name), Ast.IfStatement.class)
                            .get(0).condition());
            assertAll(name,
                    () -> assertEquals(Ast.LogicalConnector.OR, mixed.connector(),
                            "OR stays the outer connector; precedence is structural"),
                    () -> assertEquals(2, mixed.operands().size()),
                    () -> assertEquals(1, AstBoundaryTestSupport.nodes(analyses.get(name),
                                    Ast.LogicalCondition.class).stream()
                            .filter(node -> node.connector() == Ast.LogicalConnector.AND).count(),
                            "the AND chain nests under OR instead of flattening"));
        }
    }

    @Test
    void characterizesDeclarationKindQualificationAndHomonymMatrix() {
        record Variant(String declarations, String condition, ResolutionContracts.ResolutionStatus status,
                       ResolutionContracts.ResolutionReason reason, ResolutionContracts.ReferenceKind candidateKind,
                       List<SymbolTable.SymbolKind> declaredKinds) { }
        Map<String, Variant> variants = new LinkedHashMap<>();
        variants.put("data", new Variant("01 C PIC X.", "A = B OR C",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, null,
                List.of(SymbolTable.SymbolKind.DATA_ITEM)));
        variants.put("condition-name", new Variant("01 FLAG PIC X.\n   88 C VALUE 'Y'.", "A = B OR C",
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                ResolutionContracts.ReferenceKind.CONDITION, List.of(SymbolTable.SymbolKind.CONDITION_NAME)));
        variants.put("index-name", new Variant("01 T OCCURS 2 TIMES INDEXED BY C.\n   05 V PIC X.", "A = B OR C",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, null,
                List.of(SymbolTable.SymbolKind.INDEX_NAME)));
        variants.put("renames-66", new Variant(
                "01 G.\n   05 C PIC X.\n   05 D PIC X.\n   66 R RENAMES C THRU D.", "A = B OR R",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, null,
                List.of(SymbolTable.SymbolKind.RENAMES)));
        variants.put("missing", new Variant("01 PRESENT PIC X.", "A = B OR MISSING",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, null, List.of()));
        variants.put("homonym-data-condition", new Variant(
                "01 C PIC X.\n01 FLAG PIC X.\n   88 C VALUE 'Y'.", "A = B OR C",
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                ResolutionContracts.ReferenceKind.CONDITION,
                List.of(SymbolTable.SymbolKind.DATA_ITEM, SymbolTable.SymbolKind.CONDITION_NAME)));
        variants.put("qualified-data", new Variant("01 G.\n   05 C PIC X.", "A = B OR C OF G",
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, null,
                List.of(SymbolTable.SymbolKind.DATA_ITEM)));
        variants.put("qualified-condition", new Variant(
                "01 G.\n   05 FLAG PIC X.\n      88 C VALUE 'Y'.", "A = B OR C OF G",
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH,
                ResolutionContracts.ReferenceKind.CONDITION, List.of(SymbolTable.SymbolKind.CONDITION_NAME)));

        for (Map.Entry<String, Variant> item : variants.entrySet()) {
            Variant expected = item.getValue();
            String relationOperands = expected.declaredKinds().contains(SymbolTable.SymbolKind.INDEX_NAME)
                    ? NUMERIC_INTEGER_RELATION_OPERANDS
                    : ALPHANUMERIC_RELATION_OPERANDS;
            AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                    sourceWithDeclarations(item.getKey(), relationOperands,
                            expected.declarations(), expected.condition()),
                    item.getKey() + ".cbl");
            ReferenceResolution.Entry tail = tailEntry(analysis);
            List<SymbolTable.SymbolKind> declaredKinds = sameNameSymbols(analysis,
                    tail.occurrence().writtenText().split("\\s+")[0]).stream()
                    .map(SymbolTable.Symbol::kind).toList();
            assertAll(item.getKey(),
                    () -> assertEquals("conditionNameReference", tail.occurrence().grammarRule()),
                    () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, tail.occurrence().kind()),
                    () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                            tail.occurrence().admissibleKinds()),
                    () -> assertEquals(expected.status(), tail.status()),
                    () -> assertEquals(expected.reason(), tail.reason()),
                    () -> assertEquals(expected.declaredKinds(), declaredKinds),
                    () -> assertEquals(expected.candidateKind(), tail.selectedCandidate()
                            .map(ReferenceResolution.Candidate::kind).orElse(null)));
        }
    }

    @Test
    void characterizesSharedConditionLoweringAcrossIfEvaluateAndPerform() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SHARED-CONDITION-CONTEXT.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC X.
                01 B PIC X.
                01 C PIC X.
                PROCEDURE DIVISION.
                    IF A = B OR C CONTINUE END-IF.
                    EVALUATE A = B OR C
                       WHEN TRUE CONTINUE
                    END-EVALUATE.
                    PERFORM UNTIL A = B OR C
                       CONTINUE
                    END-PERFORM.
                END PROGRAM SHARED-CONDITION-CONTEXT.
                """, "shared-condition-context.cbl");

        List<ReferenceResolution.Entry> tails = valueReads(analysis).stream()
                .filter(entry -> entry.occurrence().writtenText().equals("C")).toList();
        assertAll("IF, EVALUATE subject, and PERFORM UNTIL share the same lowering defect",
                () -> assertEquals(1, AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).size()),
                () -> assertEquals(1, AstBoundaryTestSupport.nodes(analysis, Ast.EvaluateStatement.class).size()),
                () -> assertEquals(1, AstBoundaryTestSupport.nodes(analysis, Ast.PerformStatement.class).size()),
                () -> assertEquals(3, tails.size()),
                () -> assertTrue(tails.stream().allMatch(entry ->
                        entry.occurrence().admissibleKinds().equals(Set.of(ResolutionContracts.ReferenceKind.CONDITION)))),
                () -> assertTrue(tails.stream().allMatch(entry ->
                        entry.reason() == ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT)));
    }

    @Test
    void characterizesSearchConditionReferencesThatDisappearAtThePreservedBoundary() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SEARCH-CONTEXT.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC X.
                01 B PIC X.
                01 C PIC X.
                01 T OCCURS 2 TIMES INDEXED BY IDX.
                   05 TABLE-VALUE PIC X.
                01 FLAG PIC X.
                   88 FLAG-ON VALUE 'Y'.
                PROCEDURE DIVISION.
                    SEARCH T
                       WHEN A = B OR C CONTINUE
                       WHEN FLAG-ON CONTINUE
                    END-SEARCH.
                END PROGRAM SEARCH-CONTEXT.
                """, "search-context.cbl");

        Ast.PreservedStatement search = AstBoundaryTestSupport.nodes(analysis, Ast.PreservedStatement.class).stream()
                .filter(statement -> statement.grammarRule().equals("searchStatement"))
                .findFirst().orElseThrow();
        List<String> collected = analysis.resolution().entries().stream()
                .map(entry -> entry.occurrence().writtenText()).toList();
        assertAll("SEARCH condition preservation",
                () -> assertEquals(List.of("T", "A", "B"), search.operands().stream()
                        .map(operand -> ((Ast.DataReference) operand.value()).baseName()).toList()),
                () -> assertEquals(List.of("searchWhen", "searchWhen"), search.clauses().stream()
                        .map(Ast.StatementClause::grammarRule).toList()),
                () -> assertTrue(collected.containsAll(List.of("IDX", "T", "A", "B"))),
                () -> assertFalse(collected.contains("C")),
                () -> assertFalse(collected.contains("FLAG-ON")));
    }

    @Test
    void characterizesConditionNameSubscriptCorruption() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CONDITION-SUBSCRIPT.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 T OCCURS 2 TIMES INDEXED BY IDX.
                   05 FLAG PIC X.
                      88 FLAG-ON VALUE 'Y'.
                PROCEDURE DIVISION.
                    IF FLAG-ON(IDX) CONTINUE END-IF.
                END PROGRAM CONDITION-SUBSCRIPT.
                """, "condition-subscript.cbl");

        assertEquals(1, AstBoundaryTestSupport.contexts(analysis.tree(),
                CobolParser.ConditionNameSubscriptReferenceContext.class).size());
        ReferenceResolution.Entry condition = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().writtenText().equals("FLAG-ON(IDX)"))
                .findFirst().orElseThrow();
        Ast.DataReference ast = (Ast.DataReference) AstBoundaryTestSupport.nodes(analysis).stream()
                .filter(node -> node.meta().id() == condition.occurrence().referenceAstNodeId())
                .findFirst().orElseThrow();
        assertAll("condition-name subscript structure is recovered by the existing machinery",
                () -> assertEquals("FLAG-ON", ast.baseName(),
                        "the condition name stays the base; the subscript no longer hijacks it"),
                () -> assertEquals("FLAG-ON(IDX)", ast.writtenText()),
                () -> assertEquals(1, ast.subscriptGroups().size(),
                        "the written subscript materializes as a typed group"),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, condition.occurrence().kind(),
                        "the collector false gap stays reserved for Slice 5"),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, condition.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                        condition.reason(),
                        "with the recovered base name the pre-existing resolution machinery finds the "
                                + "declared 88 level; no resolver policy changed"),
                () -> assertTrue(analysis.resolution().entries().stream().anyMatch(entry ->
                        entry.occurrence().writtenText().equals("IDX")
                                && entry.occurrence().role() == ResolutionContracts.ReferenceRole.SUBSCRIPT),
                        "the subscript participates through the pre-existing SUBSCRIPT policy"));
    }

    @Test
    void characterizesCopyAndNestedScopeWithoutTreatingThemAsRootCause() {
        AstBoundaryTestSupport.Analysis copy = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. COPY-CONTEXT.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC X.
                01 B PIC X.
                COPY SEMCOND.
                PROCEDURE DIVISION.
                    IF A = B OR COPY-C CONTINUE END-IF.
                END PROGRAM COPY-CONTEXT.
                """, "copy-context.cbl");
        ReferenceResolution.Entry copyTail = entry(copy, "COPY-C");
        Ast.DataEntry copiedDeclaration = AstBoundaryTestSupport.nodes(copy, Ast.DataEntry.class).stream()
                .filter(dataEntry -> dataEntry.name().equals("COPY-C")).findFirst().orElseThrow();

        AstBoundaryTestSupport.Analysis nested = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. OUTER-CONTEXT.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 OUTER-FLAG IS GLOBAL PIC X.
                   88 C VALUE 'Y'.
                PROCEDURE DIVISION.
                    GOBACK.
                IDENTIFICATION DIVISION.
                PROGRAM-ID. INNER-CONTEXT.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC X.
                01 B PIC X.
                01 C PIC X.
                PROCEDURE DIVISION.
                    IF A = B OR C CONTINUE END-IF.
                END PROGRAM INNER-CONTEXT.
                END PROGRAM OUTER-CONTEXT.
                """, "nested-context.cbl");
        ReferenceResolution.Entry nestedTail = nested.resolution().entries().stream()
                .filter(entry -> entry.occurrence().programUnitId().canonicalProgramName().equals("INNER-CONTEXT"))
                .filter(entry -> entry.occurrence().writtenText().equals("C"))
                .findFirst().orElseThrow();

        assertAll("COPY provenance and nested lookup remain orthogonal to the classification defect",
                () -> assertEquals("SEMCOND.cpy", copiedDeclaration.meta().provenance().original().file()),
                () -> assertFalse(copiedDeclaration.meta().provenance().includeChain().isEmpty()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        copyTail.reason()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        nestedTail.reason()),
                () -> assertTrue(nestedTail.candidates().isEmpty()));
    }

    @Test
    @EnabledIfSystemProperty(named = "semantic.condition.required", matches = "true")
    void requiredSemanticOraclesForFutureImplementation() {
        List<org.junit.jupiter.api.function.Executable> oracles = new ArrayList<>();
        oracles.add(() -> assertTailResolvesAs("data abbreviation", "01 C PIC X.",
                "A = B OR C", "C", ResolutionContracts.ReferenceKind.DATA));
        oracles.add(() -> assertTailResolvesAs("real condition-name", "01 FLAG PIC X.\n   88 C VALUE 'Y'.",
                "A = B OR C", "C", ResolutionContracts.ReferenceKind.CONDITION));
        oracles.add(() -> assertTailResolvesAs("index abbreviation", NUMERIC_INTEGER_RELATION_OPERANDS,
                "01 T OCCURS 2 TIMES INDEXED BY C.\n   05 V PIC X.",
                "A = B OR C", "C", ResolutionContracts.ReferenceKind.INDEX));
        oracles.add(() -> assertTailResolvesAs("RENAMES abbreviation",
                "01 G.\n   05 C PIC X.\n   05 D PIC X.\n   66 R RENAMES C THRU D.",
                "A = B OR R", "R", ResolutionContracts.ReferenceKind.DATA));
        oracles.add(() -> assertTailResolvesAs("explicit operator abbreviation",
                "01 C PIC X.", "A = B OR < C", "C", ResolutionContracts.ReferenceKind.DATA));
        oracles.add(() -> assertTailResolvesAs("mixed AND/OR", "01 C PIC X.\n01 D PIC X.",
                "A = B OR C AND D", "D", ResolutionContracts.ReferenceKind.DATA));
        oracles.add(() -> assertAll("parenthesized inheritance boundary",
                () -> assertTailResolvesAs("parenthesized inherited DATA", """
                                01 C PIC X.
                                01 FLAG PIC X.
                                   88 D VALUE 'Y'.
                                """, "(A = B OR C) AND D", "C", ResolutionContracts.ReferenceKind.DATA),
                () -> assertTailResolvesAs("condition-name after closing parenthesis", """
                                01 C PIC X.
                                01 FLAG PIC X.
                                   88 D VALUE 'Y'.
                                """, "(A = B OR C) AND D", "D", ResolutionContracts.ReferenceKind.CONDITION)));
        oracles.add(() -> assertTailResolvesAs("condition-name after closed group",
                "01 FLAG PIC X.\n   88 C VALUE 'Y'.",
                "(A = B) OR C", "C", ResolutionContracts.ReferenceKind.CONDITION));
        oracles.add(() -> assertTailResolvesAs("qualified abbreviation", "01 G.\n   05 C PIC X.",
                "A = B OR C OF G", "C OF G", ResolutionContracts.ReferenceKind.DATA));
        oracles.add(() -> {
            AstBoundaryTestSupport.Analysis missing = AstBoundaryTestSupport.analyze(
                    sourceWithDeclarations("required-missing", "01 PRESENT PIC X.", "A = B OR MISSING"),
                    "required-missing.cbl");
            assertEquals(ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND,
                    entry(missing, "MISSING").reason());
        });
        oracles.add(() -> {
            AstBoundaryTestSupport.Analysis multiple = AstBoundaryTestSupport.analyze(
                    sourceWithDeclarations("required-multiple", "01 C PIC X.\n01 D PIC X.",
                            "A = B OR C OR D"), "required-multiple.cbl");
            assertAll("multiple consecutive abbreviations",
                    () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                            entry(multiple, "C").selectedCandidate()
                                    .map(ReferenceResolution.Candidate::kind).orElse(null)),
                    () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                            entry(multiple, "D").selectedCandidate()
                                    .map(ReferenceResolution.Candidate::kind).orElse(null)));
        });
        assertAll("future semantic requirements", oracles.stream());
    }

    private static void assertTailResolvesAs(String label, String declarations, String condition,
                                             String writtenName, ResolutionContracts.ReferenceKind expectedKind) {
        assertTailResolvesAs(label, ALPHANUMERIC_RELATION_OPERANDS,
                declarations, condition, writtenName, expectedKind);
    }

    private static void assertTailResolvesAs(String label, String relationOperands, String declarations,
                                             String condition, String writtenName,
                                             ResolutionContracts.ReferenceKind expectedKind) {
        String slug = label.replaceAll("[^A-Za-z0-9-]", "-");
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                sourceWithDeclarations("required-" + slug, relationOperands, declarations, condition),
                "required-" + slug + ".cbl");
        ReferenceResolution.Entry entry = entry(analysis, writtenName);
        assertAll(label,
                () -> assertTrue(entry.occurrence().admissibleKinds().contains(expectedKind)),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                () -> assertEquals(expectedKind, entry.selectedCandidate()
                        .map(ReferenceResolution.Candidate::kind).orElse(null)));
    }

    private static Map<String, String> adversarialConditions() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("simple", "A = B");
        result.put("one-abbreviation", "A = B OR C");
        result.put("two-abbreviations", "A = B OR C OR D");
        result.put("and-abbreviation", "A = B AND C");
        result.put("explicit-relation", "A = B OR A = C");
        result.put("new-explicit-subject", "A = B OR C = D");
        result.put("greater-abbreviation", "A > B OR C");
        result.put("not-equal-abbreviation", "A NOT = B OR C");
        result.put("logical-not-abbreviation", "A = B OR NOT C");
        result.put("mixed-and-or", "A = B AND C OR D");
        result.put("mixed-or-and", "A = B OR C AND D");
        result.put("grouped-whole", "(A = B OR C) AND D");
        result.put("grouped-left", "(A = B) OR C");
        result.put("distributed-or", "A = (B OR C)");
        result.put("distributed-and", "A = (B AND C)");
        result.put("stated-new-operator", "A = B OR < C");
        result.put("not-relational-operator", "A = B OR NOT = C");
        result.put("multiple-abbreviations-one-tail", "A = B OR < C > D");
        result.put("restart-then-abbreviate", "A = B OR C = D OR E");
        return result;
    }

    private static List<ReferenceResolution.Entry> valueReads(AstBoundaryTestSupport.Analysis analysis) {
        return analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().role() == ResolutionContracts.ReferenceRole.VALUE_READ)
                .sorted(Comparator.comparingInt(entry -> entry.occurrence().meta().span().startToken()))
                .toList();
    }

    private static ReferenceResolution.Entry tailEntry(AstBoundaryTestSupport.Analysis analysis) {
        return valueReads(analysis).stream()
                .filter(entry -> !entry.occurrence().writtenText().equals("A")
                        && !entry.occurrence().writtenText().equals("B"))
                .findFirst().orElseThrow();
    }

    private static ReferenceResolution.Entry entry(AstBoundaryTestSupport.Analysis analysis, String writtenName) {
        return valueReads(analysis).stream()
                .filter(candidate -> candidate.occurrence().writtenText().equals(writtenName))
                .reduce((first, second) -> second).orElseThrow();
    }

    private static List<SymbolTable.Symbol> sameNameSymbols(AstBoundaryTestSupport.Analysis analysis, String name) {
        String canonical = SymbolTable.canonical(name);
        return analysis.tables().units().stream().flatMap(unit -> unit.symbolTable().symbols().stream())
                .filter(symbol -> symbol.canonicalName().equals(canonical)).toList();
    }

    private static String sourceWithCondition(String programSuffix, String condition) {
        return sourceWithDeclarations(programSuffix, "01 C PIC X.\n01 D PIC X.\n01 E PIC X.", condition);
    }

    private static String sourceWithDeclarations(String programSuffix, String declarations, String condition) {
        return sourceWithDeclarations(programSuffix, ALPHANUMERIC_RELATION_OPERANDS, declarations, condition);
    }

    private static String sourceWithDeclarations(String programSuffix, String relationOperands,
                                                 String declarations, String condition) {
        String program = ("D-" + programSuffix).toUpperCase().replace('_', '-');
        return """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                %s
                %s
                PROCEDURE DIVISION.
                    IF %s CONTINUE END-IF.
                END PROGRAM %s.
                """.formatted(program, relationOperands, declarations, condition, program);
    }
}
