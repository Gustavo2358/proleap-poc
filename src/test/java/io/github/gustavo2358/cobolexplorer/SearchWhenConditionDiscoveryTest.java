package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checkpoint 1 characterization for SEARCH WHEN.  This class intentionally
 * records the current preserved boundary; it does not authorize or exercise a
 * production implementation.
 */
class SearchWhenConditionDiscoveryTest {
    @Test
    void S1_standaloneConditionNameIsInParseTreeButLostBeforeOccurrences() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S1", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        CobolParser.SearchWhenContext when = onlyWhen(analysis);
        assertAll("S1 standalone condition-name",
                () -> assertEquals("FLAG-ON", when.condition().getText()),
                () -> assertEquals(List.of("TABLE-ITEM"), operandNames(search)),
                () -> assertEquals(List.of("searchWhen"), search.clauses().stream()
                        .map(Ast.StatementClause::grammarRule).toList()),
                () -> assertTrue(search.clauses().get(0).recognizedNodes().isEmpty()),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertFalse(AstBoundaryTestSupport.nodes(analysis).stream()
                        .anyMatch(node -> node instanceof Ast.ContextualConditionTail)),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        entry(analysis, "TABLE-ITEM").selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                        entry(analysis, "TABLE-ITEM").status()),
                () -> assertEquals(1, AstBoundaryTestSupport.contexts(
                        analysis.tree(), CobolParser.ConditionNameReferenceContext.class).size()));
    }

    @Test
    void S2_completeRelationPreservesBothOperandsThroughThePreservedPath() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S2", """
                SEARCH TABLE-ITEM
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        assertAll("S2 complete relation",
                () -> assertEquals(List.of("TABLE-ITEM", "SEARCH-A", "SEARCH-B"), operandNames(search)),
                () -> assertEquals(List.of("SEARCH-IDX", "TABLE-ITEM", "SEARCH-A", "SEARCH-B"),
                        writtenNames(analysis).stream().filter(name -> name.startsWith("SEARCH-")
                                || name.equals("TABLE-ITEM")).toList()),
                () -> assertEquals(List.of("SEARCH-A", "SEARCH-B"),
                        writtenNames(analysis).stream().filter(name -> name.equals("SEARCH-A")
                                || name.equals("SEARCH-B")).toList()),
                () -> assertEquals(List.of(ResolutionContracts.ResolutionStatus.RESOLVED,
                                ResolutionContracts.ResolutionStatus.RESOLVED),
                        List.of(entry(analysis, "SEARCH-A").status(), entry(analysis, "SEARCH-B").status())),
                () -> assertTrue(List.of(entry(analysis, "SEARCH-A"), entry(analysis, "SEARCH-B")).stream()
                        .allMatch(entry -> entry.selectedCandidate().orElseThrow().kind()
                                == ResolutionContracts.ReferenceKind.DATA)),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.RelationCondition.class).isEmpty()));
    }

    @Test
    void S3_abbreviatedTailDisappearsWhileCompleteOperandsRemain() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3", """
                SEARCH TABLE-ITEM
                   WHEN SEARCH-A = SEARCH-B OR SEARCH-C
                      CONTINUE
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        assertAll("S3 abbreviated relation",
                () -> assertEquals(List.of("TABLE-ITEM", "SEARCH-A", "SEARCH-B"), operandNames(search)),
                () -> assertFalse(writtenNames(analysis).contains("SEARCH-C")),
                () -> assertEquals(0, AstBoundaryTestSupport.nodes(analysis, Ast.ContextualConditionTail.class).size()),
                () -> assertEquals(List.of(ResolutionContracts.ResolutionStatus.RESOLVED,
                                ResolutionContracts.ResolutionStatus.RESOLVED),
                        List.of(entry(analysis, "SEARCH-A").status(), entry(analysis, "SEARCH-B").status())),
                () -> assertEquals(0, AstBoundaryTestSupport.contexts(
                        analysis.tree(), CobolParser.AbbreviationContext.class).size()),
                () -> assertTrue(AstBoundaryTestSupport.contexts(
                        analysis.tree(), CobolParser.ConditionNameReferenceContext.class).stream()
                        .anyMatch(context -> context.getText().equals("SEARCH-C"))));
    }

    @Test
    void S4_multipleWhenKeepParseOrderAndBranchStatementsButLoseBothConditions() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S4", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-A
                      DISPLAY 'A'
                   WHEN FLAG-B
                      DISPLAY 'B'
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        List<CobolParser.SearchWhenContext> whens = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.SearchWhenContext.class);
        assertAll("S4 multiple WHEN",
                () -> assertEquals(List.of("FLAG-A", "FLAG-B"), whens.stream()
                        .map(context -> context.condition().getText()).toList()),
                () -> assertEquals(List.of("searchWhen", "searchWhen"), search.clauses().stream()
                        .map(Ast.StatementClause::grammarRule).toList()),
                () -> assertEquals(List.of(1, 1), search.clauses().stream()
                        .map(clause -> clause.nestedStatements().size()).toList()),
                () -> assertEquals(List.of("DISPLAY 'A'", "DISPLAY 'B'"), search.clauses().stream()
                        .flatMap(clause -> clause.nestedStatements().stream())
                        .map(Ast.Statement::toString)
                        .map(text -> text.substring(text.indexOf("writtenText=") + 12,
                                text.indexOf(", operands=")))
                        .toList()),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-A")),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-B")),
                () -> { AstBoundaryTestSupport.assertActualProductsJoin(analysis); });
    }

    @Test
    void S5_notConditionNameHasLogicalParseShapeButNoConditionOccurrence() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S5", """
                SEARCH TABLE-ITEM
                   WHEN NOT FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);

        CobolParser.SearchWhenContext when = onlyWhen(analysis);
        assertAll("S5 NOT",
                () -> assertEquals(1, when.condition().combinableCondition().NOT().getSymbol().getTokenIndex()
                        >= 0 ? 1 : 0),
                () -> assertEquals("NOTFLAG-ON", when.condition().getText()),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.NegatedCondition.class).isEmpty()));
    }

    @Test
    void S6_qualifiedConditionNameIsParsedButItsQualificationAndRootAreLost() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S6", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON OF GROUP-X
                      CONTINUE
                END-SEARCH.
                """);

        CobolParser.ConditionNameReferenceContext condition = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.ConditionNameReferenceContext.class).stream()
                .filter(context -> context.getText().contains("FLAG-ON"))
                .findFirst().orElseThrow();
        assertAll("S6 qualified condition-name",
                () -> assertEquals("FLAG-ONOFGROUP-X", condition.getText()),
                () -> assertEquals(1, condition.inData().size()),
                () -> assertEquals("GROUP-X", condition.inData(0).getText().replaceFirst("(?i)^OF", "").trim()),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertTrue(writtenNames(analysis).contains("GROUP-X")),
                () -> assertEquals(List.of("TABLE-ITEM", "GROUP-X"), operandNames(search(analysis))),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.DataReference.class).stream()
                        .noneMatch(reference -> reference.baseName().equals("FLAG-ON"))));
    }

    @Test
    void searchAllSharesTheGrammarBoundaryButHasDistinctNormativeRestrictions() {
        AstBoundaryTestSupport.Analysis analysis = analyze("ALL", """
                SEARCH ALL TABLE-ITEM
                   WHEN SEARCH-A = SEARCH-B AND SEARCH-C = SEARCH-D
                      CONTINUE
                END-SEARCH.
                """);

        CobolParser.SearchStatementContext statement = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.SearchStatementContext.class).get(0);
        assertAll("SEARCH ALL",
                () -> assertEquals("ALL", statement.ALL().getText()),
                () -> assertEquals(1, statement.searchWhen().size()),
                () -> assertEquals("SEARCH-A=SEARCH-BANDSEARCH-C=SEARCH-D",
                        statement.searchWhen(0).condition().getText()),
                () -> assertEquals(List.of("SEARCH-IDX", "SEARCH-A", "SEARCH-B", "SEARCH-C", "SEARCH-D"),
                        writtenNames(analysis).stream().filter(name -> name.startsWith("SEARCH-")).toList()),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.PreservedStatement.class).stream()
                        .anyMatch(node -> node.grammarRule().equals("searchStatement"))));
    }

    @Test
    void controlNegativeKeepsTableVaryingAndRelationOperandsOutOfConditionPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyze("CONTROL", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX
                   WHEN TABLE-VALUE (SEARCH-IDX) = SEARCH-KEY
                      CONTINUE
                END-SEARCH.
                """);

        Ast.PreservedStatement search = search(analysis);
        Map<String, List<ReferenceOccurrences.Occurrence>> occurrences = analysis.occurrences().values().stream()
                .flatMap(product -> product.occurrences().stream())
                .collect(Collectors.groupingBy(ReferenceOccurrences.Occurrence::writtenText));
        assertAll("control negative",
                () -> assertEquals(List.of("TABLE-ITEM", "SEARCH-IDX", "TABLE-VALUE (SEARCH-IDX)", "SEARCH-KEY"),
                        operandNames(search)),
                () -> assertTrue(occurrences.containsKey("TABLE-ITEM")),
                () -> assertTrue(occurrences.containsKey("SEARCH-IDX")),
                () -> assertTrue(occurrences.containsKey("TABLE-VALUE (SEARCH-IDX)")),
                () -> assertTrue(occurrences.containsKey("SEARCH-KEY")),
                () -> assertTrue(occurrences.get("SEARCH-IDX").stream()
                        .allMatch(occurrence -> occurrence.kind() != ResolutionContracts.ReferenceKind.CONDITION)),
                () -> assertFalse(occurrences.get("TABLE-VALUE (SEARCH-IDX)").stream()
                        .anyMatch(occurrence -> occurrence.kind() == ResolutionContracts.ReferenceKind.CONDITION)));
    }

    @Test
    void F1_genericAstChildrenTraversalCannotSupplyConditionPositionRouting() {
        AstBoundaryTestSupport.Analysis searchAnalysis = analyze("F1-SEARCH", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);
        AstBoundaryTestSupport.Analysis ifAnalysis = analyze("F1-IF", """
                IF FLAG-ON
                   CONTINUE
                END-IF.
                """);

        ReferenceResolution.Entry ifFlag = entryForRole(ifAnalysis, "FLAG-ON",
                ResolutionContracts.ReferenceRole.VALUE_READ);
        assertAll("F1 typed boundary versus generic fallback",
                () -> assertTrue(AstBoundaryTestSupport.nodes(searchAnalysis, Ast.ContextualConditionTail.class)
                        .isEmpty()),
                () -> assertFalse(writtenNames(searchAnalysis).contains("FLAG-ON")),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, ifFlag.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                        ifFlag.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, ifFlag.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        ifFlag.selectedCandidate().orElseThrow().kind()));
    }

    @Test
    void F2_nextSentenceIsAnAlternativeTokenPathAndCurrentPreservedClauseRetainsIt() {
        AstBoundaryTestSupport.Analysis analysis = analyze("F2-NEXT", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON
                      NEXT SENTENCE
                END-SEARCH.
                """);

        CobolParser.SearchWhenContext when = onlyWhen(analysis);
        Ast.StatementClause clause = search(analysis).clauses().get(0);
        assertAll("F2 NEXT SENTENCE",
                () -> assertEquals("FLAG-ON", when.condition().getText()),
                () -> assertEquals(0, when.statement().size()),
                () -> assertEquals("NEXT", when.NEXT().getText()),
                () -> assertEquals("SENTENCE", when.SENTENCE().getText()),
                () -> assertEquals("searchWhen", clause.grammarRule()),
                () -> assertTrue(clause.recognizedNodes().isEmpty()),
                () -> assertEquals(1, clause.nestedStatements().size()),
                () -> assertInstanceOf(Ast.NextSentenceStatement.class, clause.nestedStatements().get(0)),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.NextSentenceStatement.class).size() == 1));
    }

    @Test
    void F3_varyingIndexIsCurrentlyDefaultDataButFuturePolicyMustAdmitIndex() {
        AstBoundaryTestSupport.Analysis analysis = analyze("F3-INDEX", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        ReferenceResolution.Entry varying = entryForRole(analysis, "SEARCH-IDX",
                ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT);
        assertAll("F3 VARYING index",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, varying.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        varying.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, varying.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        varying.reason()),
                () -> assertTrue(analysis.resolution().entries().stream()
                        .filter(entry -> entry.occurrence().writtenText().equals("SEARCH-IDX"))
                        .anyMatch(entry -> entry.occurrence().role() == ResolutionContracts.ReferenceRole.OCCURS_INDEX
                                && entry.selectedCandidate().orElseThrow().kind()
                                == ResolutionContracts.ReferenceKind.INDEX)));
    }

    @Test
    void F3_varyingElementaryIntegerIsDataAndMustRemainAdmissibleAsData() {
        AstBoundaryTestSupport.Analysis analysis = analyzeWithDeclarations("F3-DATA", """
                01  SEARCH-COUNTER PIC 9(4).
                """, """
                SEARCH TABLE-ITEM VARYING SEARCH-COUNTER
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        ReferenceResolution.Entry varying = entryForRole(analysis, "SEARCH-COUNTER",
                ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT);
        assertAll("F3 VARYING data",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, varying.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        varying.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, varying.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        varying.selectedCandidate().orElseThrow().kind()));
    }

    @Test
    void F3_varyingAndConditionUseIndependentSemanticPositions() {
        AstBoundaryTestSupport.Analysis analysis = analyze("F3-SEPARATE", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX
                   WHEN FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);

        ReferenceResolution.Entry varying = entryForRole(analysis, "SEARCH-IDX",
                ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT);
        assertAll("F3 separate VARYING and condition positions",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, varying.occurrence().kind()),
                () -> assertFalse(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertTrue(analysis.occurrences().values().stream()
                        .flatMap(product -> product.occurrences().stream())
                        .filter(occurrence -> occurrence.writtenText().equals("SEARCH-IDX"))
                        .allMatch(occurrence -> occurrence.kind() != ResolutionContracts.ReferenceKind.CONDITION)));
    }

    @Test
    void R1_bareVaryingIndexReResolvesWithTheHypotheticalSharedPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyze("R1-BARE-INDEX", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.DataReference varying = varyingReference(analysis, "SEARCH-IDX");
        ReferenceResolution resolution = reResolveWithVaryingPolicy(analysis, varying,
                EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.ReferenceKind.INDEX));
        ReferenceResolution.Entry entry = resolutionEntry(resolution, varying.meta().id());
        assertAll("R1 bare INDEX",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, entry.occurrence().kind()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX), entry.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        entry.selectedCandidate().orElseThrow().kind()));
    }

    @Test
    void R2_bareVaryingDataReResolvesWithTheHypotheticalSharedPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyzeWithDeclarations("R2-BARE-DATA", """
                01  SEARCH-COUNTER PIC 9(4).
                """, """
                SEARCH TABLE-ITEM VARYING SEARCH-COUNTER
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.DataReference varying = varyingReference(analysis, "SEARCH-COUNTER");
        ReferenceResolution resolution = reResolveWithVaryingPolicy(analysis, varying,
                EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.ReferenceKind.INDEX));
        ReferenceResolution.Entry entry = resolutionEntry(resolution, varying.meta().id());
        assertAll("R2 bare DATA",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, entry.occurrence().kind()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX), entry.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        entry.selectedCandidate().orElseThrow().kind()));
    }

    @Test
    void R3_qualifiedVaryingExcludesIndexEvenWhenWrongPolicyWouldAdmitIt() {
        AstBoundaryTestSupport.Analysis analysis = analyzeQualifiedVarying("R3-QUALIFIED", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX OF SOME-GROUP
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.DataReference varying = varyingReference(analysis, "SEARCH-IDX");
        assertEquals(1, varying.qualifiers().size(), "R3 must be a qualified nominal what-if");
        ReferenceResolution.Entry proposed = resolutionEntry(
                reResolveWithVaryingPolicy(analysis, varying, Set.of(ResolutionContracts.ReferenceKind.DATA)),
                varying.meta().id());
        ReferenceResolution.Entry wrong = resolutionEntry(
                reResolveWithVaryingPolicy(analysis, varying,
                        EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX)), varying.meta().id());
        assertAll("R3 qualified INDEX exclusion",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, proposed.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        proposed.occurrence().admissibleKinds()),
                () -> assertTrue(proposed.selectedCandidate().isEmpty()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX), wrong.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, wrong.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        wrong.selectedCandidate().orElseThrow().kind()));
    }

    @Test
    void R4_grammarShapeAuditShowsQualifiedDataNameWithoutRootSubscript() {
        AstBoundaryTestSupport.Analysis analysis = analyzeWithDeclarations("R4-SHAPE", """
                01  SOME-GROUP.
                    05  QUALIFIED-ITEM PIC 9(4).
                """, """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX OF SOME-GROUP
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        CobolParser.SearchVaryingContext varyingContext =
                AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.SearchVaryingContext.class)
                        .get(0);
        Ast.DataReference varying = varyingReference(analysis, "SEARCH-IDX");
        assertAll("R4 searchVarying grammar shape",
                () -> assertEquals(1, varyingContext.qualifiedDataName().getChildCount()),
                () -> assertEquals("SEARCH-IDXOFSOME-GROUP", varyingContext.qualifiedDataName().getText()),
                () -> assertEquals(1, varying.qualifiers().size()),
                () -> assertTrue(varying.subscriptGroups().isEmpty()),
                () -> assertTrue(AstBoundaryTestSupport.contexts(varyingContext,
                        CobolParser.TableCallContext.class).isEmpty()));
    }

    @Test
    void declarationSubstitutionChangesOnlyBindingForBareVarying() {
        AstBoundaryTestSupport.Analysis data = analyzeWithDeclarations("R5-DATA", """
                01  VARYING-NAME PIC 9(4).
                """, varyingProgram("VARYING-NAME"));
        AstBoundaryTestSupport.Analysis index = analyzeWithDeclarations("R5-INDEX", """
                01  VARYING-TABLE OCCURS 10 TIMES INDEXED BY VARYING-NAME.
                    05  VARYING-VALUE PIC 9(4).
                """, varyingProgram("VARYING-NAME"));
        AstBoundaryTestSupport.Analysis missing = analyze("R5-MISSING", varyingProgram("VARYING-NAME"));

        assertAll("declaration substitution",
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        projectedVaryingOccurrence(data, "VARYING-NAME").admissibleKinds()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        projectedVaryingOccurrence(index, "VARYING-NAME").admissibleKinds()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        projectedVaryingOccurrence(missing, "VARYING-NAME").admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        reResolveWithVaryingPolicy(data, varyingReference(data, "VARYING-NAME"),
                                EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                        ResolutionContracts.ReferenceKind.INDEX))
                                .entries().stream().filter(entry -> entry.occurrence().writtenText()
                                        .equals("VARYING-NAME") && entry.occurrence().role()
                                        == ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT)
                                .findFirst().orElseThrow().selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        reResolveWithVaryingPolicy(index, varyingReference(index, "VARYING-NAME"),
                                EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                        ResolutionContracts.ReferenceKind.INDEX))
                                .entries().stream().filter(entry -> entry.occurrence().writtenText()
                                        .equals("VARYING-NAME") && entry.occurrence().role()
                                        == ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT)
                                .findFirst().orElseThrow().selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        reResolveWithVaryingPolicy(missing, varyingReference(missing, "VARYING-NAME"),
                                EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                        ResolutionContracts.ReferenceKind.INDEX))
                                .entries().stream().filter(entry -> entry.occurrence().writtenText()
                                        .equals("VARYING-NAME") && entry.occurrence().role()
                                        == ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT)
                                .findFirst().orElseThrow().status()));
    }

    @Test
    void shapeSubstitutionChangesOnlyAdmissibilityForVarying() {
        AstBoundaryTestSupport.Analysis bare = analyzeWithDeclarations("R6-BARE", """
                01  VARYING-NAME PIC 9(4).
                """, varyingProgram("VARYING-NAME"));
        AstBoundaryTestSupport.Analysis qualified = analyzeWithDeclarations("R6-QUALIFIED", """
                01  SOME-GROUP.
                    05  QUALIFIED-ITEM PIC 9(4).
                """, """
                SEARCH TABLE-ITEM VARYING VARYING-NAME OF SOME-GROUP
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.DataReference bareReference = varyingReference(bare, "VARYING-NAME");
        Ast.DataReference qualifiedReference = varyingReference(qualified, "VARYING-NAME");
        assertAll("shape substitution",
                () -> assertTrue(bareReference.qualifiers().isEmpty()),
                () -> assertEquals(1, qualifiedReference.qualifiers().size()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        projectedVaryingOccurrence(bare, "VARYING-NAME").admissibleKinds()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        reResolveWithVaryingPolicy(qualified, qualifiedReference,
                                Set.of(ResolutionContracts.ReferenceKind.DATA))
                                .entries().stream().filter(entry -> entry.occurrence().referenceAstNodeId()
                                        == qualifiedReference.meta().id()).findFirst().orElseThrow()
                                .occurrence().admissibleKinds()));
    }

    private static AstBoundaryTestSupport.Analysis analyze(String id, String search) {
        return AstBoundaryTestSupport.analyze(program(id, search), "search-when-" + id + ".cbl");
    }

    private static AstBoundaryTestSupport.Analysis analyzeWithDeclarations(String id, String declarations,
                                                                            String search) {
        return AstBoundaryTestSupport.analyze(program(id, declarations, search), "search-when-" + id + ".cbl");
    }

    private static String program(String id, String search) {
        return program(id, "", search);
    }

    private static String program(String id, String extraDeclarations, String search) {
        String program = "SEARCH-WHEN-" + id;
        return """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01  SEARCH-A PIC 9(4).
                01  SEARCH-B PIC 9(4).
                01  SEARCH-C PIC 9(4).
                01  SEARCH-D PIC 9(4).
                01  SEARCH-KEY PIC 9(4).
                %s
                01  GROUP-X.
                    05  FLAG PIC X.
                        88  FLAG-ON VALUE 'Y'.
                        88  FLAG-A VALUE 'A'.
                        88  FLAG-B VALUE 'B'.
                01  TABLE-ITEM OCCURS 2 TIMES INDEXED BY SEARCH-IDX.
                    05  TABLE-VALUE PIC 9(4).
                PROCEDURE DIVISION.
                %s
                END PROGRAM %s.
                """.formatted(program, extraDeclarations, search, program);
    }

    private static AstBoundaryTestSupport.Analysis analyzeQualifiedVarying(String id, String search) {
        String program = "SEARCH-WHEN-" + id;
        String source = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01  SEARCH-A PIC 9(4).
                01  SEARCH-B PIC 9(4).
                01  TABLE-ITEM OCCURS 2 TIMES INDEXED BY TABLE-IDX.
                    05  TABLE-VALUE PIC 9(4).
                01  SOME-GROUP.
                    05  QUALIFIED-TABLE OCCURS 10 TIMES INDEXED BY SEARCH-IDX.
                        10  QUALIFIED-VALUE PIC 9(4).
                PROCEDURE DIVISION.
                %s
                END PROGRAM %s.
                """.formatted(program, search, program);
        return AstBoundaryTestSupport.analyze(source, "search-when-" + id + ".cbl");
    }

    private static String varyingProgram(String varyingName) {
        return """
                SEARCH TABLE-ITEM VARYING %s
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """.formatted(varyingName);
    }

    private static Ast.PreservedStatement search(AstBoundaryTestSupport.Analysis analysis) {
        return AstBoundaryTestSupport.nodes(analysis, Ast.PreservedStatement.class).stream()
                .filter(statement -> statement.grammarRule().equals("searchStatement"))
                .findFirst().orElseThrow();
    }

    private static CobolParser.SearchWhenContext onlyWhen(AstBoundaryTestSupport.Analysis analysis) {
        return AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.SearchWhenContext.class)
                .stream().findFirst().orElseThrow();
    }

    private static List<String> operandNames(Ast.PreservedStatement statement) {
        return statement.operands().stream().map(operand -> {
            if (operand.value() instanceof Ast.DataReference reference) return reference.writtenText();
            if (operand.value() instanceof Ast.NamedReference reference) return reference.writtenText();
            return operand.value().toString();
        }).toList();
    }

    private static Set<String> writtenNames(AstBoundaryTestSupport.Analysis analysis) {
        return analysis.occurrences().values().stream()
                .flatMap(product -> product.occurrences().stream())
                .map(ReferenceOccurrences.Occurrence::writtenText)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static ReferenceResolution.Entry entry(AstBoundaryTestSupport.Analysis analysis, String writtenText) {
        return analysis.resolution().entries().stream()
                .filter(candidate -> candidate.occurrence().writtenText().equals(writtenText))
                .findFirst().orElseThrow(() -> new AssertionError("missing resolution entry: " + writtenText));
    }

    private static ReferenceResolution.Entry entryForRole(AstBoundaryTestSupport.Analysis analysis,
                                                          String writtenText,
                                                          ResolutionContracts.ReferenceRole role) {
        return analysis.resolution().entries().stream()
                .filter(candidate -> candidate.occurrence().writtenText().equals(writtenText)
                        && candidate.occurrence().role() == role)
                .findFirst().orElseThrow(() -> new AssertionError(
                        "missing resolution entry: " + writtenText + " / " + role));
    }

    private static Ast.DataReference varyingReference(AstBoundaryTestSupport.Analysis analysis,
                                                      String baseName) {
        return search(analysis).operands().stream()
                .map(Ast.StatementOperand::value)
                .filter(Ast.DataReference.class::isInstance)
                .map(Ast.DataReference.class::cast)
                .filter(reference -> reference.baseName().equals(baseName))
                .findFirst().orElseThrow(() -> new AssertionError("missing varying reference: " + baseName));
    }

    private static ReferenceOccurrences.Occurrence projectedVaryingOccurrence(
            AstBoundaryTestSupport.Analysis analysis, String baseName) {
        Ast.DataReference varying = varyingReference(analysis, baseName);
        return reResolveWithVaryingPolicy(analysis, varying,
                EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.ReferenceKind.INDEX))
                .entries().stream()
                .filter(entry -> entry.occurrence().referenceAstNodeId() == varying.meta().id())
                .findFirst().orElseThrow().occurrence();
    }

    private static ReferenceResolution reResolveWithVaryingPolicy(
            AstBoundaryTestSupport.Analysis analysis, Ast.DataReference varying,
            Set<ResolutionContracts.ReferenceKind> admissibleKinds) {
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> projected = new LinkedHashMap<>();
        analysis.occurrences().forEach((unitId, product) -> projected.put(unitId,
                new ReferenceOccurrences(product.occurrences().stream().map(occurrence -> {
                    if (occurrence.referenceAstNodeId() != varying.meta().id()) return occurrence;
                    return new ReferenceOccurrences.Occurrence(occurrence.id(), occurrence.programUnitId(),
                            occurrence.referenceAstNodeId(), occurrence.scopeId(),
                            ResolutionContracts.ReferenceKind.DATA, admissibleKinds, occurrence.role(),
                            occurrence.grammarRule(), occurrence.writtenText(), occurrence.meta(),
                            occurrence.preservation());
                }).toList())));
        return new CobolReferenceResolver(ResolutionContracts.CobolResolutionPolicy.initial())
                .resolve(analysis.model(), analysis.tables(), Map.copyOf(projected));
    }

    private static ReferenceResolution.Entry resolutionEntry(ReferenceResolution resolution, int astNodeId) {
        return resolution.entries().stream()
                .filter(entry -> entry.occurrence().referenceAstNodeId() == astNodeId)
                .findFirst().orElseThrow(() -> new AssertionError("missing resolution entry: " + astNodeId));
    }
}
