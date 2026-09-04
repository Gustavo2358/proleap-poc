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

/** Functional implementation expectations for the typed SEARCH WHEN boundary. */
class SearchWhenConditionDiscoveryTest {
    @Test
    void S1_standaloneConditionNameIsMaterializedAsCondition() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S1", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);

        Ast.SearchStatement search = search(analysis);
        CobolParser.SearchWhenContext when = onlyWhen(analysis);
        Ast.SearchWhen branch = search.whens().get(0);
        Ast.DataReference flag = assertInstanceOf(Ast.DataReference.class, branch.condition());
        assertAll("S1 standalone condition-name",
                () -> assertEquals("FLAG-ON", when.condition().getText()),
                () -> assertEquals("TABLE-ITEM", search.searchedReference().baseName()),
                () -> assertEquals("FLAG-ON", flag.baseName()),
                () -> assertEquals(1, branch.statements().size()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        entryForRole(analysis, "FLAG-ON", ResolutionContracts.ReferenceRole.VALUE_READ)
                                .occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                        entry(analysis, "FLAG-ON").occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                        entry(analysis, "FLAG-ON").status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        entry(analysis, "FLAG-ON").selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        entry(analysis, "TABLE-ITEM").selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                        entry(analysis, "TABLE-ITEM").status()),
                () -> assertEquals(1, AstBoundaryTestSupport.contexts(
                        analysis.tree(), CobolParser.ConditionNameReferenceContext.class).size()));
    }

    @Test
    void S2_completeRelationUsesTheExistingRelationSurface() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S2", """
                SEARCH TABLE-ITEM
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.SearchStatement search = search(analysis);
        Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class,
                search.whens().get(0).condition());
        assertAll("S2 complete relation",
                () -> assertEquals("TABLE-ITEM", search.searchedReference().baseName()),
                () -> assertEquals("SEARCH-A", ((Ast.DataReference) relation.subject()).baseName()),
                () -> assertEquals("SEARCH-B", ((Ast.DataReference) relation.object()).baseName()),
                () -> assertEquals(List.of(ResolutionContracts.ResolutionStatus.RESOLVED,
                                ResolutionContracts.ResolutionStatus.RESOLVED),
                        List.of(entry(analysis, "SEARCH-A").status(), entry(analysis, "SEARCH-B").status())),
                () -> assertTrue(List.of(entry(analysis, "SEARCH-A"), entry(analysis, "SEARCH-B")).stream()
                        .allMatch(entry -> entry.selectedCandidate().orElseThrow().kind()
                                == ResolutionContracts.ReferenceKind.DATA)),
                () -> assertEquals(1, AstBoundaryTestSupport.nodes(analysis, Ast.RelationCondition.class).size()));
    }

    @Test
    void S3_abbreviatedTailUsesContextualConditionLowering() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S3", """
                SEARCH TABLE-ITEM
                   WHEN SEARCH-A = SEARCH-B OR SEARCH-C
                      CONTINUE
                END-SEARCH.
                """);

        Ast.SearchStatement search = search(analysis);
        Ast.LogicalCondition logical = assertInstanceOf(Ast.LogicalCondition.class,
                search.whens().get(0).condition());
        Ast.RelationCondition relation = assertInstanceOf(Ast.RelationCondition.class, logical.operands().get(0));
        Ast.ContextualConditionTail tail = assertInstanceOf(Ast.ContextualConditionTail.class,
                logical.operands().get(1));
        assertAll("S3 abbreviated relation",
                () -> assertEquals("TABLE-ITEM", search.searchedReference().baseName()),
                () -> assertEquals("SEARCH-C", tail.nominalReference().baseName()),
                () -> assertEquals("SEARCH-A", ((Ast.DataReference) relation.subject()).baseName()),
                () -> assertEquals("SEARCH-B", ((Ast.DataReference) relation.object()).baseName()),
                () -> assertTrue(writtenNames(analysis).contains("SEARCH-C")),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX,
                                ResolutionContracts.ReferenceKind.CONDITION),
                        entry(analysis, "SEARCH-C").occurrence().admissibleKinds()),
                () -> assertEquals(List.of(ResolutionContracts.ResolutionStatus.RESOLVED,
                                ResolutionContracts.ResolutionStatus.RESOLVED),
                        List.of(entry(analysis, "SEARCH-A").status(), entry(analysis, "SEARCH-B").status())),
                () -> assertEquals(1, AstBoundaryTestSupport.nodes(analysis, Ast.ContextualConditionTail.class).size()));
    }

    @Test
    void S4_multipleWhenPreserveOrderAndBranchOwnership() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S4", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-A
                      DISPLAY 'A'
                   WHEN FLAG-B
                      DISPLAY 'B'
                END-SEARCH.
                """);

        Ast.SearchStatement search = search(analysis);
        List<CobolParser.SearchWhenContext> whens = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.SearchWhenContext.class);
        assertAll("S4 multiple WHEN",
                () -> assertEquals(List.of("FLAG-A", "FLAG-B"), whens.stream()
                        .map(context -> context.condition().getText()).toList()),
                () -> assertEquals(List.of("FLAG-A", "FLAG-B"), search.whens().stream()
                        .map(branch -> ((Ast.DataReference) branch.condition()).baseName()).toList()),
                () -> assertEquals(List.of(1, 1), search.whens().stream()
                        .map(branch -> branch.statements().size()).toList()),
                () -> assertEquals("DISPLAY 'A'", ((Ast.PreservedStatement) search.whens().get(0).statements().get(0)).writtenText()),
                () -> assertEquals("DISPLAY 'B'", ((Ast.PreservedStatement) search.whens().get(1).statements().get(0)).writtenText()),
                () -> assertTrue(writtenNames(analysis).contains("FLAG-A")),
                () -> assertTrue(writtenNames(analysis).contains("FLAG-B")),
                () -> { AstBoundaryTestSupport.assertActualProductsJoin(analysis); });
    }

    @Test
    void S5_notConditionNameReusesNegatedConditionLowering() {
        AstBoundaryTestSupport.Analysis analysis = analyze("S5", """
                SEARCH TABLE-ITEM
                   WHEN NOT FLAG-ON
                      CONTINUE
                END-SEARCH.
                """);

        CobolParser.SearchWhenContext when = onlyWhen(analysis);
        Ast.NegatedCondition negated = assertInstanceOf(Ast.NegatedCondition.class, search(analysis).whens().get(0).condition());
        assertAll("S5 NOT",
                () -> assertEquals(1, when.condition().combinableCondition().NOT().getSymbol().getTokenIndex()
                        >= 0 ? 1 : 0),
                () -> assertEquals("NOTFLAG-ON", when.condition().getText()),
                () -> assertEquals("FLAG-ON", ((Ast.DataReference) negated.operand()).baseName()),
                () -> assertTrue(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        entry(analysis, "FLAG-ON").selectedCandidate().orElseThrow().kind()));
    }

    @Test
    void S6_qualifiedConditionNamePreservesRootAndQualifier() {
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
        Ast.DataReference root = assertInstanceOf(Ast.DataReference.class, search(analysis).whens().get(0).condition());
        assertAll("S6 qualified condition-name",
                () -> assertEquals("FLAG-ONOFGROUP-X", condition.getText()),
                () -> assertEquals(1, condition.inData().size()),
                () -> assertEquals("GROUP-X", condition.inData(0).getText().replaceFirst("(?i)^OF", "").trim()),
                () -> assertEquals("FLAG-ON", root.baseName()),
                () -> assertEquals(1, root.qualifiers().size()),
                () -> assertTrue(writtenNames(analysis).stream().anyMatch(name -> name.startsWith("FLAG-ON"))),
                () -> assertTrue(writtenNames(analysis).contains("GROUP-X")),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        resolutionEntry(analysis.resolution(), root.meta().id())
                                .selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT,
                        entryForRole(analysis, "GROUP-X", ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT)
                                .occurrence().role()));
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
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.SearchStatement.class).stream()
                        .anyMatch(node -> node.all())));
    }

    @Test
    void controlNegativeKeepsTableVaryingAndRelationOperandsOutOfConditionPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyze("CONTROL", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX
                   WHEN TABLE-VALUE (SEARCH-IDX) = SEARCH-KEY
                      CONTINUE
                END-SEARCH.
                """);

        Ast.SearchStatement search = search(analysis);
        Map<String, List<ReferenceOccurrences.Occurrence>> occurrences = analysis.occurrences().values().stream()
                .flatMap(product -> product.occurrences().stream())
                .collect(Collectors.groupingBy(ReferenceOccurrences.Occurrence::writtenText));
        assertAll("control negative",
                () -> assertEquals("TABLE-ITEM", search.searchedReference().baseName()),
                () -> assertEquals("SEARCH-IDX", search.varying().baseName()),
                () -> assertEquals(1, search.whens().size()),
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
    void F1_typedConditionRoutingIsRequiredBeyondGenericTraversal() {
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
                () -> assertInstanceOf(Ast.DataReference.class, search(searchAnalysis).whens().get(0).condition()),
                () -> assertTrue(writtenNames(searchAnalysis).contains("FLAG-ON")),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        entry(searchAnalysis, "FLAG-ON").occurrence().kind()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION, ifFlag.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                        ifFlag.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, ifFlag.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        ifFlag.selectedCandidate().orElseThrow().kind()));
    }

    @Test
    void F2_nextSentenceIsMaterializedAsTheBranchAction() {
        AstBoundaryTestSupport.Analysis analysis = analyze("F2-NEXT", """
                SEARCH TABLE-ITEM
                   WHEN FLAG-ON
                      NEXT SENTENCE
                END-SEARCH.
                """);

        CobolParser.SearchWhenContext when = onlyWhen(analysis);
        Ast.SearchWhen branch = search(analysis).whens().get(0);
        assertAll("F2 NEXT SENTENCE",
                () -> assertEquals("FLAG-ON", when.condition().getText()),
                () -> assertEquals(0, when.statement().size()),
                () -> assertEquals("NEXT", when.NEXT().getText()),
                () -> assertEquals("SENTENCE", when.SENTENCE().getText()),
                () -> assertEquals(1, branch.statements().size()),
                () -> assertInstanceOf(Ast.NextSentenceStatement.class, branch.statements().get(0)),
                () -> assertTrue(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.NextSentenceStatement.class).size() == 1));
    }

    @Test
    void F3_varyingIndexUsesTheProductionShapePolicy() {
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
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        varying.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, varying.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        varying.selectedCandidate().orElseThrow().kind()),
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
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
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
                () -> assertTrue(writtenNames(analysis).contains("FLAG-ON")),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        entry(analysis, "FLAG-ON").occurrence().kind()),
                () -> assertTrue(analysis.occurrences().values().stream()
                        .flatMap(product -> product.occurrences().stream())
                        .filter(occurrence -> occurrence.writtenText().equals("SEARCH-IDX"))
                        .allMatch(occurrence -> occurrence.kind() != ResolutionContracts.ReferenceKind.CONDITION)));
    }

    @Test
    void R1_bareVaryingIndexResolvesWithTheProductionPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyze("R1-BARE-INDEX", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.DataReference varying = varyingReference(analysis, "SEARCH-IDX");
        ReferenceResolution.Entry entry = entryForRole(analysis, "SEARCH-IDX",
                ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT);
        assertAll("R1 bare INDEX",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, entry.occurrence().kind()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX), entry.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        entry.selectedCandidate().orElseThrow().kind()));
    }

    @Test
    void R2_bareVaryingDataResolvesWithTheProductionPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyzeWithDeclarations("R2-BARE-DATA", """
                01  SEARCH-COUNTER PIC 9(4).
                """, """
                SEARCH TABLE-ITEM VARYING SEARCH-COUNTER
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.DataReference varying = varyingReference(analysis, "SEARCH-COUNTER");
        ReferenceResolution.Entry entry = entryForRole(analysis, "SEARCH-COUNTER",
                ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT);
        assertAll("R2 bare DATA",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, entry.occurrence().kind()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX), entry.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        entry.selectedCandidate().orElseThrow().kind()));
    }

    @Test
    void R3_qualifiedVaryingExcludesIndexFromTheProductionPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyzeQualifiedVarying("R3-QUALIFIED", """
                SEARCH TABLE-ITEM VARYING SEARCH-IDX OF SOME-GROUP
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.DataReference varying = varyingReference(analysis, "SEARCH-IDX");
        assertEquals(1, varying.qualifiers().size(), "R3 must be a qualified nominal what-if");
        ReferenceResolution.Entry proposed = resolutionEntry(analysis.resolution(), varying.meta().id());
        assertAll("R3 qualified INDEX exclusion",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, proposed.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        proposed.occurrence().admissibleKinds()),
                () -> assertTrue(proposed.selectedCandidate().isEmpty()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, proposed.status()),
                () -> assertTrue(proposed.selectedCandidate().isEmpty()));
    }

    @Test
    void R5_qualifiedVaryingDataReResolvesWithTheQualifiedDataPolicy() {
        AstBoundaryTestSupport.Analysis analysis = analyzeWithDeclarations("R5-QUALIFIED-DATA", """
                01  SOME-GROUP.
                    05  SEARCH-COUNTER PIC 9(4).
                """, """
                SEARCH TABLE-ITEM VARYING SEARCH-COUNTER OF SOME-GROUP
                   WHEN SEARCH-A = SEARCH-B
                      CONTINUE
                END-SEARCH.
                """);

        Ast.DataReference varying = varyingReference(analysis, "SEARCH-COUNTER");
        ReferenceResolution.Entry entry = resolutionEntry(analysis.resolution(), varying.meta().id());
        assertAll("R5 qualified DATA",
                () -> assertEquals("SEARCH-COUNTER", varying.baseName()),
                () -> assertEquals(1, varying.qualifiers().size()),
                () -> assertEquals("SOME-GROUP", varying.qualifiers().get(0).name()),
                () -> assertEquals("OF SOME-GROUP", varying.qualifiers().get(0).writtenText()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, entry.occurrence().kind()),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.DATA),
                        entry.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ReferenceRole.CONTEXT_DEPENDENT,
                        entry.occurrence().role()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        entry.selectedCandidate().orElseThrow().kind()),
                () -> assertTrue(writtenNames(analysis).contains("SOME-GROUP")));
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

    private static Ast.SearchStatement search(AstBoundaryTestSupport.Analysis analysis) {
        return AstBoundaryTestSupport.nodes(analysis, Ast.SearchStatement.class).stream()
                .findFirst().orElseThrow();
    }

    private static CobolParser.SearchWhenContext onlyWhen(AstBoundaryTestSupport.Analysis analysis) {
        return AstBoundaryTestSupport.contexts(analysis.tree(), CobolParser.SearchWhenContext.class)
                .stream().findFirst().orElseThrow();
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
        Ast.DataReference varying = search(analysis).varying();
        if (varying != null && varying.baseName().equals(baseName)) return varying;
        throw new AssertionError("missing varying reference: " + baseName);
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
