package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural oracles for WORK-COND-004 (Slice 4 of BACKLOG-COND-001): a true condition-name
 * reference in simple-condition position must produce an {@link Ast.DataReference} whose
 * written nominal structure is lossless — base name, IN/OF qualification in written order,
 * subscript groups as typed children and provenance per written fragment (Contract A). No
 * binding, no candidate selection, no DATA/INDEX/CONDITION decision and no new node type.
 *
 * <p>Each oracle was derived from the approved Discovery contract before the production
 * lowering was changed: the expectation is the written structure of the source, never the
 * shape the current builder happens to produce. The adversarial shapes kill the known
 * shortcuts (first descendant base stealing, string flattening, subscript dropping,
 * qualifier promotion and DATA_OR_FILE widening).</p>
 */
class ConditionNameSurfaceAstTest {

    // ---- CN-01 — simple condition-name ------------------------------------------------

    @Test
    void cn01SimpleConditionNameProducesASingleTypedDataReference() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("SIMPLE", "FLAG-OK"), "cn-01-simple.cbl");
        Ast.DataReference reference = condition(analysis);
        assertAll("CN-01: IF FLAG-OK must be DataReference(baseName=FLAG-OK) without corruption",
                () -> assertEquals("FLAG-OK", reference.baseName()),
                () -> assertTrue(reference.qualifiers().isEmpty()),
                () -> assertTrue(reference.subscriptGroups().isEmpty()),
                () -> assertEquals("FLAG-OK", reference.writtenText()),
                () -> assertEquals(Ast.ReferenceUnderstanding.STRUCTURED, reference.understanding()),
                () -> assertEquals(1, AstBoundaryTestSupport.nodes(analysis, Ast.DataReference.class).stream()
                        .filter(node -> node.baseName().equals("FLAG-OK")).count(),
                        "exactly one reference node for the written name: no clone, no synthetic node"),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis, Ast.ContextualConditionTail.class)
                        .isEmpty(), "a standalone simple condition is not a contextual tail"));
    }

    // ---- CN-02 — qualification ---------------------------------------------------------

    @Test
    void cn02QualificationBecomesAStructuralQualifierWithUnspecifiedTarget() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("QUAL", "FLAG-88 OF CUSTOMER"), "cn-02-qual.cbl");
        Ast.DataReference reference = condition(analysis);
        assertAll("CN-02: IF FLAG-88 OF CUSTOMER",
                () -> assertEquals("FLAG-88", reference.baseName()),
                () -> assertEquals(1, reference.qualifiers().size()),
                () -> assertEquals("CUSTOMER", reference.qualifiers().get(0).name()),
                () -> assertEquals(Ast.QualifierConnector.OF, reference.qualifiers().get(0).connector()),
                () -> assertEquals(Ast.QualifierTarget.UNSPECIFIED, reference.qualifiers().get(0).target(),
                        "a single qualifier occupies the final grammar slot; the parse tree cannot "
                                + "classify its namespace, so the surface must not invent DATA"),
                () -> assertEquals("CUSTOMER", reference.qualifiers().get(0).reference().baseName(),
                        "the qualifier payload carries its own nominal reference"),
                () -> assertEquals("OF CUSTOMER", reference.qualifiers().get(0).writtenText()),
                () -> assertTrue(reference.subscriptGroups().isEmpty()));
    }

    // ---- CN-03 — nested qualification --------------------------------------------------

    @Test
    void cn03NestedQualificationPreservesOrderConnectorsAndPositionalTargets() {
        AstBoundaryTestSupport.Analysis of = AstBoundaryTestSupport.analyze(
                program("NESTED", "FLAG-88 OF SUB-GRP OF GROUP-A"), "cn-03-nested.cbl");
        Ast.DataReference ofReference = condition(of);
        assertAll("CN-03: FLAG-88 OF SUB-GRP OF GROUP-A keeps both qualifiers in written order",
                () -> assertEquals("FLAG-88", ofReference.baseName()),
                () -> assertEquals(List.of("SUB-GRP", "GROUP-A"), ofReference.qualifiers().stream()
                        .map(Ast.DataQualifier::name).toList()),
                () -> assertEquals(List.of(Ast.QualifierConnector.OF, Ast.QualifierConnector.OF),
                        ofReference.qualifiers().stream().map(Ast.DataQualifier::connector).toList()),
                () -> assertEquals(List.of(Ast.QualifierTarget.DATA, Ast.QualifierTarget.UNSPECIFIED),
                        ofReference.qualifiers().stream().map(Ast.DataQualifier::target).toList(),
                        "non-final slots are grammar-proven DATA; the final slot is UNSPECIFIED"));

        // M3: IN and OF are written-equivalent connectors and must be preserved as written.
        AstBoundaryTestSupport.Analysis mixed = AstBoundaryTestSupport.analyze(
                program("MIXED", "FLAG-88 OF SUB-GRP IN GROUP-A"), "cn-03-mixed.cbl");
        Ast.DataReference mixedReference = condition(mixed);
        assertAll("written connectors are preserved, never normalized away",
                () -> assertEquals(List.of(Ast.QualifierConnector.OF, Ast.QualifierConnector.IN),
                        mixedReference.qualifiers().stream().map(Ast.DataQualifier::connector).toList()),
                () -> assertEquals(List.of("SUB-GRP", "GROUP-A"), mixedReference.qualifiers().stream()
                        .map(Ast.DataQualifier::name).toList()));
    }

    // ---- CN-04 — subscript -------------------------------------------------------------

    @Test
    void cn04SubscriptKeepsTheConditionNameAsBaseAndMaterializesTheGroup() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("SUBSCRIPT", "FLAG-88(I)"), "cn-04-subscript.cbl");
        Ast.DataReference reference = condition(analysis);
        assertAll("CN-04: FLAG-88(I) must not be hijacked by the subscript name",
                () -> assertEquals("FLAG-88", reference.baseName()),
                () -> assertTrue(reference.qualifiers().isEmpty()),
                () -> assertEquals(1, reference.subscriptGroups().size()),
                () -> assertEquals("(I)", reference.subscriptGroups().get(0).writtenText()),
                () -> assertEquals(1, reference.subscriptGroups().get(0).subscripts().size()),
                () -> assertInstanceOf(Ast.DataReference.class,
                        reference.subscriptGroups().get(0).subscripts().get(0)),
                () -> assertEquals("I", ((Ast.DataReference) reference.subscriptGroups().get(0)
                        .subscripts().get(0)).baseName()),
                () -> assertEquals("FLAG-88(I)", reference.writtenText()),
                () -> assertEquals(ResolutionContracts.ReferenceRole.SUBSCRIPT,
                        entry(analysis, "I").occurrence().role(),
                        "the recovered subscript participates through the pre-existing collector policy"));
    }

    // ---- CN-05 — qualification + subscript ----------------------------------------------

    @Test
    void cn05QualificationAndSubscriptsCoexistWithoutLoss() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("QUAL-SUBSCRIPT", "FLAG-88 OF CUSTOMER(I, J)"), "cn-05-qual-subscript.cbl");
        Ast.DataReference reference = condition(analysis);
        Ast.SubscriptGroup group = assertInstanceOf(Ast.SubscriptGroup.class,
                reference.subscriptGroups().stream().findFirst().orElseThrow());
        assertAll("CN-05: FLAG-88 OF CUSTOMER(I, J) keeps both dimensions",
                () -> assertEquals("FLAG-88", reference.baseName()),
                () -> assertEquals(List.of("CUSTOMER"), reference.qualifiers().stream()
                        .map(Ast.DataQualifier::name).toList()),
                () -> assertEquals(Ast.QualifierTarget.UNSPECIFIED, reference.qualifiers().get(0).target()),
                () -> assertEquals(1, reference.subscriptGroups().size()),
                () -> assertEquals(2, group.subscripts().size()),
                () -> assertEquals(List.of("I", "J"), group.subscripts().stream()
                        .map(subscript -> ((Ast.DataReference) subscript).baseName()).toList(),
                        "subscript order inside the written group is preserved"),
                () -> assertEquals("(I, J)", group.writtenText()),
                () -> assertEquals(ResolutionContracts.ReferenceRole.SUBSCRIPT,
                        entry(analysis, "I").occurrence().role()),
                () -> assertEquals(ResolutionContracts.ReferenceRole.SUBSCRIPT,
                        entry(analysis, "J").occurrence().role()));
    }

    // ---- CN-06 — contextual tail ---------------------------------------------------------

    @Test
    void cn06ContextualTailRemainsContextualWithAStructurallyCompleteInnerReference() {
        AstBoundaryTestSupport.Analysis plain = AstBoundaryTestSupport.analyze(
                program("TAIL", "A = B OR C"), "cn-06-tail.cbl");
        Ast.ContextualConditionTail tail = AstBoundaryTestSupport.nodes(plain,
                Ast.ContextualConditionTail.class).get(0);
        assertAll("CN-06a: A = B OR C stays a ContextualConditionTail with a clean inner reference",
                () -> assertEquals("C", tail.nominalReference().baseName()),
                () -> assertTrue(tail.nominalReference().qualifiers().isEmpty()),
                () -> assertTrue(tail.nominalReference().subscriptGroups().isEmpty()),
                () -> assertEquals("C", tail.writtenText()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.CONDITION,
                        entry(plain, "C").occurrence().kind(),
                        "the collector false gap stays reserved for Slice 5; this slice must not "
                                + "convert the tail into a definitive condition"),
                () -> assertEquals(Set.of(ResolutionContracts.ReferenceKind.CONDITION),
                        entry(plain, "C").occurrence().admissibleKinds()));

        AstBoundaryTestSupport.Analysis subscripted = AstBoundaryTestSupport.analyze(
                program("TAIL-SUBSCRIPT", "A = B OR FLAG-ON(IDX)"), "cn-06-tail-subscript.cbl");
        Ast.ContextualConditionTail subscriptedTail = AstBoundaryTestSupport.nodes(subscripted,
                Ast.ContextualConditionTail.class).get(0);
        assertAll("CN-06b: A = B OR FLAG-ON(IDX) keeps the tail wrapper and completes the inner shape",
                () -> assertEquals("FLAG-ON", subscriptedTail.nominalReference().baseName()),
                () -> assertEquals(1, subscriptedTail.nominalReference().subscriptGroups().size()),
                () -> assertEquals("IDX", ((Ast.DataReference) subscriptedTail.nominalReference()
                        .subscriptGroups().get(0).subscripts().get(0)).baseName()),
                () -> assertEquals(ResolutionContracts.ReferenceRole.SUBSCRIPT,
                        entry(subscripted, "IDX").occurrence().role()));
    }

    // ---- CN-07 — cardinalidade ------------------------------------------------------------

    @Test
    void cn07EveryWrittenNameProducesExactlyOneCorrespondingNodeAndOccurrence() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("CARDINALITY", "FLAG-88 OF CUSTOMER(I)"), "cn-07-cardinality.cbl");
        List<Ast.DataReference> roots = AstBoundaryTestSupport.nodes(analysis, Ast.DataReference.class)
                .stream().filter(reference -> reference.baseName().equals("FLAG-88")).toList();
        List<Ast.DataQualifier> qualifiers = AstBoundaryTestSupport.nodes(analysis,
                Ast.DataQualifier.class);
        List<Ast.SubscriptGroup> groups = AstBoundaryTestSupport.nodes(analysis,
                Ast.SubscriptGroup.class);
        List<ReferenceResolution.Entry> rootEntries = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().writtenText().equals("FLAG-88 OF CUSTOMER(I)"))
                .toList();
        List<ReferenceResolution.Entry> qualifierEntries = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().writtenText().equals("CUSTOMER")
                        && entry.occurrence().role() == ResolutionContracts.ReferenceRole.QUALIFIER_COMPONENT)
                .toList();
        List<ReferenceResolution.Entry> subscriptEntries = analysis.resolution().entries().stream()
                .filter(entry -> entry.occurrence().writtenText().equals("I")
                        && entry.occurrence().role() == ResolutionContracts.ReferenceRole.SUBSCRIPT)
                .toList();
        assertAll("CN-07: no clone, no synthetic container, no duplicated qualifier, no lost subscript",
                () -> assertEquals(1, roots.size(), "exactly one root reference for the condition name"),
                () -> assertEquals(1, qualifiers.size()),
                () -> assertEquals(1, groups.size()),
                () -> assertEquals(1, rootEntries.size()),
                () -> assertEquals(1, qualifierEntries.size()),
                () -> assertEquals(1, subscriptEntries.size()),
                () -> assertTrue(analysis.resolution().entries().stream().noneMatch(entry ->
                        entry.occurrence().writtenText().equals("(I)")),
                        "the SubscriptGroup is structural container only: no synthetic occurrence"));
    }

    // ---- CN-08 — provenance Contract A ----------------------------------------------------

    @Test
    void cn08ProvenanceContractACoversTheWholeReferenceAndEachWrittenFragment() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("PROVENANCE", "FLAG-88 OF CUSTOMER(I)"), "cn-08-provenance.cbl");
        Ast.DataReference reference = condition(analysis);
        CobolParser.ConditionNameReferenceContext referenceContext = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.ConditionNameReferenceContext.class).get(0);
        CobolParser.ConditionNameContext name = referenceContext.conditionName();
        CobolParser.InDataContext inData = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.InDataContext.class).get(0);
        CobolParser.ConditionNameSubscriptReferenceContext groupContext = AstBoundaryTestSupport
                .contexts(analysis.tree(), CobolParser.ConditionNameSubscriptReferenceContext.class).get(0);
        CobolParser.SubscriptContext subscriptContext = AstBoundaryTestSupport.contexts(
                analysis.tree(), CobolParser.SubscriptContext.class).get(0);
        Ast.DataQualifier qualifier = reference.qualifiers().get(0);
        Ast.SubscriptGroup group = reference.subscriptGroups().get(0);
        assertAll("CN-08: DataReference.meta = whole reference, DataQualifier.meta = written "
                        + "qualification, SubscriptGroup.meta = written group, subscript meta = written "
                        + "subscript; the base name has no Meta of its own",
                () -> assertEquals(name.getStart().getTokenIndex(),
                        reference.meta().span().startToken()),
                () -> assertEquals(groupContext.getStop().getTokenIndex(),
                        reference.meta().span().endToken()),
                () -> assertEquals(inData.getStart().getTokenIndex(),
                        qualifier.meta().span().startToken(), "qualifier meta covers the OF connector"),
                () -> assertEquals(inData.getStop().getTokenIndex(),
                        qualifier.meta().span().endToken()),
                () -> assertTrue(qualifier.reference().meta().span().startToken()
                        > qualifier.meta().span().startToken(),
                        "the qualifier payload span starts after the written connector"),
                () -> assertEquals(groupContext.getStart().getTokenIndex(),
                        group.meta().span().startToken(), "group meta covers the opening parenthesis"),
                () -> assertEquals(groupContext.getStop().getTokenIndex(),
                        group.meta().span().endToken(), "group meta covers the closing parenthesis"),
                () -> assertEquals(subscriptContext.getStart().getTokenIndex(),
                        group.subscripts().get(0).meta().span().startToken()),
                () -> assertEquals(subscriptContext.getStop().getTokenIndex(),
                        group.subscripts().get(0).meta().span().endToken()),
                () -> assertTrue(AstBoundaryTestSupport.nodes(analysis).stream().noneMatch(node ->
                        node.meta().span().startToken() == name.getStart().getTokenIndex()
                                && node.meta().span().endToken() == name.getStop().getTokenIndex()),
                        "no AST node owns exactly the base-name token range"));
    }

    // ---- CN-09 — case metamorphic -----------------------------------------------------------

    @Test
    void cn09CaseVariationPreservesTopology() {
        AstBoundaryTestSupport.Analysis upper = AstBoundaryTestSupport.analyze(
                program("CASE-UPPER", "FLAG-88 OF SUB-GRP OF GROUP-A"), "cn-09-upper.cbl");
        AstBoundaryTestSupport.Analysis lower = AstBoundaryTestSupport.analyze(
                program("CASE-LOWER", "flag-88 of sub-grp of group-a"), "cn-09-lower.cbl");
        Ast.DataReference upperReference = condition(upper);
        Ast.DataReference lowerReference = condition(lower);
        assertAll("CN-09: case variation changes no structural decision",
                () -> assertEquals(topology(upperReference), topology(lowerReference)),
                () -> assertTrue(upperReference.baseName().equalsIgnoreCase(lowerReference.baseName())),
                () -> assertEquals(upperReference.qualifiers().size(), lowerReference.qualifiers().size()),
                () -> assertEquals(qualifierNamesCanonical(upperReference),
                        qualifierNamesCanonical(lowerReference),
                        "qualifier names are compared case-insensitively, as COBOL names are"));
    }

    // ---- CN-10 — alpha rename metamorphic -------------------------------------------------------

    @Test
    void cn10ConsistentAlphaRenamePreservesTopology() {
        AstBoundaryTestSupport.Analysis original = AstBoundaryTestSupport.analyze(
                program("RENAME-ORIGINAL", "FLAG-88 OF CUSTOMER(I)"), "cn-10-original.cbl");
        AstBoundaryTestSupport.Analysis renamed = AstBoundaryTestSupport.analyze(
                program("RENAME-ALTERED", "FLAG-99 OF CLIENT(K)"), "cn-10-renamed.cbl");
        Ast.DataReference originalReference = condition(original);
        Ast.DataReference renamedReference = condition(renamed);
        assertAll("CN-10: a consistent alpha rename preserves every structural decision",
                () -> assertEquals(topology(originalReference), topology(renamedReference)),
                () -> assertEquals("FLAG-99", renamedReference.baseName()),
                () -> assertEquals(List.of("CLIENT"), qualifierNames(renamedReference)),
                () -> assertEquals(Ast.QualifierTarget.UNSPECIFIED,
                        renamedReference.qualifiers().get(0).target()),
                () -> assertEquals("K", ((Ast.DataReference) renamedReference.subscriptGroups().get(0)
                        .subscripts().get(0)).baseName()));
    }

    // ---- CN-11 — qualifier namespace is positional, mapping stays {DATA} -------------------------

    @Test
    void cn11UnspecifiedQualifiersNeverInventDataAndResolveThroughTheExistingDataMapping() {
        AstBoundaryTestSupport.Analysis dataQualified = AstBoundaryTestSupport.analyze(
                program("NS-DATA", "FLAG-88 OF CUSTOMER"), "cn-11-data.cbl");
        Ast.DataReference reference = condition(dataQualified);
        assertAll("the surface never derives the qualifier namespace from the inData branch",
                () -> assertEquals(Ast.QualifierTarget.UNSPECIFIED, reference.qualifiers().get(0).target()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED,
                        entry(dataQualified, "FLAG-88 OF CUSTOMER").status(),
                        "the compatibility-preserving mapping consumes UNSPECIFIED as {DATA}, exactly "
                                + "like the previous DATA target"),
                () -> assertEquals(ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH,
                        entry(dataQualified, "FLAG-88 OF CUSTOMER").reason()));

        AstBoundaryTestSupport.Analysis fileQualified = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CN-FILE-QUAL.
                ENVIRONMENT DIVISION.
                INPUT-OUTPUT SECTION.
                FILE-CONTROL.
                    SELECT CUSTOMER-FILE ASSIGN TO 'CUSTDD'.
                DATA DIVISION.
                FILE SECTION.
                FD CUSTOMER-FILE.
                01 CUSTOMER-REC.
                   05 CUST-STATUS PIC X.
                      88 FLAG-88 VALUE 'A'.
                PROCEDURE DIVISION.
                    IF FLAG-88 OF CUSTOMER-FILE CONTINUE END-IF.
                END PROGRAM CN-FILE-QUAL.
                """, "cn-11-file.cbl");
        Ast.DataReference fileReference = condition(fileQualified);
        assertAll("file-qualified resolution stays bounded until BACKLOG-RES-004",
                () -> assertEquals(Ast.QualifierTarget.UNSPECIFIED,
                        fileReference.qualifiers().get(0).target(),
                        "the parse tree cannot distinguish DATA from FILE behind the inData branch"),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        entry(fileQualified, "FLAG-88 OF CUSTOMER-FILE").status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND,
                        entry(fileQualified, "FLAG-88 OF CUSTOMER-FILE").reason(),
                        "UNSPECIFIED is consumed as {DATA}; file-qualified resolution is deliberately "
                                + "deferred to BACKLOG-RES-004, never widened inside this slice"));
    }

    // ---- CN-12 — nested qualifier inside the subscript --------------------------------------------

    @Test
    void cn12NestedSubscriptQualifierStaysInsideTheSubscript() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze(
                program("NESTED-SUBSCRIPT", "FLAG-88 OF CUSTOMER(SUB OF SUB-GROUP)"),
                "cn-12-nested-subscript.cbl");
        Ast.DataReference reference = condition(analysis);
        assertAll("CN-12: SUB-GROUP belongs to the subscript reference, never to the root",
                () -> assertEquals("FLAG-88", reference.baseName()),
                () -> assertEquals(List.of("CUSTOMER"), qualifierNames(reference),
                        "the root keeps exactly its own written qualifier"),
                () -> assertEquals(1, reference.subscriptGroups().size()),
                () -> assertInstanceOf(Ast.DataReference.class,
                        reference.subscriptGroups().get(0).subscripts().get(0)));
        Ast.DataReference subscript = (Ast.DataReference) reference.subscriptGroups().get(0)
                .subscripts().get(0);
        assertAll("the subscript is itself a qualified data reference",
                () -> assertEquals("SUB", subscript.baseName()),
                () -> assertEquals(List.of("SUB-GROUP"), qualifierNames(subscript)),
                () -> assertTrue(subscript.subscriptGroups().isEmpty()));
    }

    // ---- S4-BOUNDARY-01 — SET/EVALUATE identifier paths stay untouched ------------------------------

    @Test
    void s4BoundaryIdentifierPathsKeepTheirExistingShapes() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CN-BOUNDARY.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 GROUP-A.
                   05 FLAG PIC X.
                      88 FLAG-OK VALUE 'Y'.
                01 T OCCURS 2 TIMES.
                   05 ELEM PIC X.
                      88 FLAG-88 VALUE 'Y'.
                PROCEDURE DIVISION.
                    SET FLAG-OK OF GROUP-A TO TRUE.
                    EVALUATE TRUE
                       WHEN FLAG-88(1) CONTINUE
                    END-EVALUATE.
                END PROGRAM CN-BOUNDARY.
                """, "cn-boundary.cbl");
        Ast.DataReference setTarget = AstBoundaryTestSupport.nodes(analysis, Ast.DataReference.class)
                .stream().filter(reference -> reference.baseName().equals("FLAG-OK"))
                .findFirst().orElseThrow();
        assertAll("the SET target keeps the pre-existing qualifiedDataName path",
                () -> assertEquals(List.of("GROUP-A"), qualifierNames(setTarget)),
                () -> assertEquals(Ast.QualifierTarget.DATA_OR_FILE,
                        setTarget.qualifiers().get(0).target(),
                        "identifier paths keep their existing target policy; only condition-name "
                                + "surfaces use the positional UNSPECIFIED rule"));
        Ast.DataReference selector = (Ast.DataReference) AstBoundaryTestSupport.nodes(analysis,
                Ast.EvaluateStatement.class).get(0).branches().get(0).selectors().get(0).expression();
        assertAll("the EVALUATE selector keeps the pre-existing structurally complete tableCall path",
                () -> assertEquals("FLAG-88", selector.baseName()),
                () -> assertEquals(1, selector.subscriptGroups().size()));
    }

    // ---- identity / pre-order over adversarial surfaces ---------------------------------------------

    @Test
    void conditionNameSurfacesPreserveCanonicalPreOrderAndProductJoins() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CN-PREORDER.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC X.
                01 B PIC X.
                01 FLAG PIC X.
                   88 FLAG-ON VALUE 'N'.
                01 GROUP-A.
                   05 SUB-GRP.
                      10 CUSTOMER PIC X.
                         88 FLAG-88 VALUE 'Y'.
                01 SUB PIC 99.
                01 I PIC 99.
                PROCEDURE DIVISION.
                    IF FLAG-88 OF CUSTOMER(SUB OF SUB-GROUP) CONTINUE END-IF.
                    IF A = B OR FLAG-ON(I) CONTINUE END-IF.
                END PROGRAM CN-PREORDER.
                """, "cn-preorder.cbl");
        List<Ast.Node> nodes = canonicalNodes(analysis);
        for (int expected = 0; expected < nodes.size(); expected++)
            assertEquals(expected, nodes.get(expected).meta().id(),
                    "every node id must equal its canonical pre-order position");
        AstBoundaryTestSupport.assertActualProductsJoin(analysis);
    }

    // ---- consumer impact — CICS classifier sees the recovered DFHRESP shape ---------------------------

    @Test
    void cicsClassifierConsumerImpactIsBoundedToTheRecoveredSurfaceShape() {
        AstBoundaryTestSupport.Analysis analysis = AstBoundaryTestSupport.analyze("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CN-CICS-SHAPE.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 X PIC S9(8) COMP.
                PROCEDURE DIVISION.
                    IF DFHRESP(X) = DFHVALUE(NORMAL) CONTINUE END-IF.
                    IF DFHRESP(X) CONTINUE END-IF.
                END PROGRAM CN-CICS-SHAPE.
                """, "cn-cics-shape.cbl");

        Ast.DataReference simpleCondition = AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class)
                .stream().map(Ast.IfStatement::condition)
                .filter(Ast.DataReference.class::isInstance)
                .map(Ast.DataReference.class::cast)
                .findFirst().orElseThrow();
        assertAll("the degenerate simple-condition shape is structurally recovered",
                () -> assertEquals("DFHRESP", simpleCondition.baseName(),
                        "before this slice the subscript name hijacked the base; the classifier "
                                + "could never see the DFHRESP shape"),
                () -> assertEquals(1, simpleCondition.subscriptGroups().size()),
                () -> assertEquals("X", ((Ast.DataReference) simpleCondition.subscriptGroups().get(0)
                        .subscripts().get(0)).baseName()));

        ExternalClassification classifications = new CicsIntrinsicClassifier().classify(
                analysis.model(), analysis.occurrences(), analysis.resolution());
        List<ExternalClassification.Entry> dfhresp = classifications.entries().stream()
                .filter(entry -> entry.constructWrittenText().equals("DFHRESP(X)")).toList();
        assertAll("consumer delta: the recovered surface shape is classified exactly like the "
                        + "pre-existing relation-operand shape; the fact stays an inferred hypothesis",
                () -> assertEquals(2, dfhresp.size(),
                        "the relation subject (pre-existing) and the simple condition (recovered) "
                                + "now share the same shape policy"),
                () -> assertTrue(dfhresp.stream().allMatch(entry ->
                        entry.kind() == ExternalClassification.Kind.POSSIBLE_INTRINSIC
                                && entry.certainty() == ExternalClassification.Certainty.INFERRED
                                && entry.reason() == ExternalClassification.Reason
                                .COBOL_REFERENCE_UNRESOLVED_WITH_KNOWN_CICS_SHAPE)),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        entry(analysis, "DFHRESP(X)").status(),
                        "nominal binding is untouched by this slice; the classification stays orthogonal"));
    }

    // ---- helpers ------------------------------------------------------------------------------------

    private static Ast.DataReference condition(AstBoundaryTestSupport.Analysis analysis) {
        return assertInstanceOf(Ast.DataReference.class,
                AstBoundaryTestSupport.nodes(analysis, Ast.IfStatement.class).get(0).condition());
    }

    private static ReferenceResolution.Entry entry(AstBoundaryTestSupport.Analysis analysis,
                                                   String writtenText) {
        return analysis.resolution().entries().stream()
                .filter(item -> item.occurrence().writtenText().equals(writtenText))
                .reduce((first, second) -> second).orElseThrow();
    }

    private static List<String> qualifierNames(Ast.DataReference reference) {
        return reference.qualifiers().stream().map(Ast.DataQualifier::name).toList();
    }

    private static List<String> qualifierNamesCanonical(Ast.DataReference reference) {
        return qualifierNames(reference).stream().map(name -> name.toUpperCase(Locale.ROOT)).toList();
    }

    private static String topology(Ast.DataReference reference) {
        StringBuilder shape = new StringBuilder("ref");
        for (Ast.DataQualifier qualifier : reference.qualifiers())
            shape.append("[q:").append(qualifier.connector()).append(':').append(qualifier.target()).append(']');
        for (Ast.SubscriptGroup group : reference.subscriptGroups())
            shape.append("[g:").append(group.subscripts().size()).append(']');
        return shape.toString();
    }

    private static List<Ast.Node> canonicalNodes(AstBoundaryTestSupport.Analysis analysis) {
        List<Ast.Node> result = new ArrayList<>();
        for (CompilationUnitModel.ProgramUnit unit : analysis.model().programUnits())
            addCanonical(unit.program(), result, new IdentityHashMap<>());
        return result;
    }

    private static void addCanonical(Ast.Node node, List<Ast.Node> result,
                                     IdentityHashMap<Ast.Node, Boolean> reached) {
        assertFalse(reached.containsKey(node), "an AST node instance must be reachable exactly once");
        reached.put(node, Boolean.TRUE);
        result.add(node);
        for (Ast.Node child : Ast.children(node)) addCanonical(child, result, reached);
    }

    private static String program(String suffix, String condition) {
        String programId = "CN-" + suffix.toUpperCase().replace('_', '-');
        return """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A PIC X.
                01 B PIC X.
                01 C PIC X.
                01 D PIC X.
                01 X PIC X.
                01 FLAG PIC X.
                   88 FLAG-OK VALUE 'Y'.
                   88 FLAG-ON VALUE 'N'.
                01 GROUP-A.
                   05 SUB-GRP.
                      10 CUSTOMER PIC X.
                         88 FLAG-88 VALUE 'Y'.
                      10 CUST-TBL OCCURS 2 TIMES INDEXED BY IDX.
                         15 ELEM PIC X.
                01 SUB PIC 99.
                01 I PIC 99.
                01 J PIC 99.
                PROCEDURE DIVISION.
                    IF %s CONTINUE END-IF.
                END PROGRAM %s.
                """.formatted(programId, condition, programId);
    }
}
