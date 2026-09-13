package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic transport oracle for WORK-SEMANTIC-PRODUCT-002 CP7. */
class SemanticProductCheckpoint7JsonTest {
    private static final Path FIXTURE = Path.of(
            "src/test/resources/cobol/semantic/semantic-product-lowering-readiness.cbl");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> ROOT_FIELD_ORDER = List.of(
            "schema", "contractVersion", "unit", "policy", "dataDeclarations",
            "statements", "structure", "gaps", "coverage", "entryInventory", "storageIndependence");
    private static final Set<String> ROOT_FIELDS = Set.of(
            "schema", "contractVersion", "unit", "policy", "dataDeclarations",
            "statements", "structure", "gaps", "coverage", "entryInventory", "storageIndependence");
    private static final Set<String> VOLATILE_FIELDS = Set.of(
            "timestamp", "elapsedms", "thread", "objectid", "memoryaddress",
            "generatedat", "nonce");

    @Test
    void equivalentIndependentPublicationsAndRepeatedSerializationAreByteIdentical()
            throws IOException {
        CobolSemanticPort firstPublication = publishFixture();
        CobolSemanticPort secondPublication = publishFixture();

        byte[] first = SemanticProductJsonWriter.serialize(firstPublication);
        byte[] repeated = SemanticProductJsonWriter.serialize(firstPublication);
        byte[] independent = SemanticProductJsonWriter.serialize(secondPublication);

        assertArrayEquals(first, repeated,
                "serializing the same closed publication twice must reproduce every byte");
        assertArrayEquals(first, independent,
                "independent equivalent analyses must reproduce every byte");
    }

    @Test
    void v1CarriesTheCompleteControlledFixtureAndKeepsReferencesReconciliable()
            throws IOException {
        JsonNode document = parse(SemanticProductJsonWriter.serialize(publishFixture()));

        assertEquals(ROOT_FIELDS, fieldSet(document));
        assertEquals(ROOT_FIELD_ORDER, fieldList(document));
        assertEquals("cobol-semantic-product", document.path("schema").asText());
        assertEquals("2.0.0", document.path("contractVersion").asText());
        assertEquals("SEMANTIC-TARGET",
                document.path("unit").path("canonicalProgramName").asText());
        assertEquals(List.of(0), integerValues(document.path("unit").path("structuralPath")));
        assertEquals("cobol-explorer/explicit-options",
                document.path("policy").path("policyId").asText());
        assertEquals("UNSPECIFIED", document.path("policy").path("qualifyMode").asText());
        assertEquals("UNSPECIFIED", document.path("policy").path("pgmnameMode").asText());
        assertEquals("UNSPECIFIED", document.path("policy").path("dynamMode").asText());
        assertEquals("UNSPECIFIED", document.path("policy").path("dllMode").asText());

        JsonNode data = document.path("dataDeclarations");
        JsonNode statements = document.path("statements");
        assertEquals(List.of("WS-X", "FLAG", "AUX-PGM"), elements(data).stream()
                .map(entry -> entry.path("canonicalName").asText()).toList());
        assertEquals(List.of("X(8)", "9", "X(8)"), elements(data).stream()
                .map(entry -> entry.path("picture").asText()).toList());
        assertEquals(3, data.size());
        assertEquals(14, statements.size());
        assertEquals(7, countVariant(statements, "MOVE"));
        assertEquals(3, countVariant(statements, "CALL"));
        assertEquals(3, countVariant(statements, "IF"));
        assertEquals(1, countVariant(statements, "OBSERVED"));
        assertEquals(IntStream.range(0, 14).boxed().toList(), elements(statements).stream()
                .map(statement -> statement.path("header").path("programPoint").asInt())
                .toList());

        assertEquals(List.of("statement:0", "statement:1", "statement:10", "statement:7",
                        "statement:8", "statement:12", "statement:13"),
                textValues(document.path("structure").path("roots")));
        assertBranch(document, "statement:10", "THEN",
                List.of("statement:2", "statement:11", "statement:5"));
        assertBranch(document, "statement:10", "ELSE", List.of("statement:6"));
        assertBranch(document, "statement:11", "THEN", List.of("statement:3"));
        assertBranch(document, "statement:11", "ELSE", List.of("statement:4"));
        assertBranch(document, "statement:12", "THEN", List.of("statement:9"));
        assertBranch(document, "statement:12", "ELSE", List.of());

        assertEquals("statement:7", statement(document, "statement:10")
                .path("continuation").asText());
        assertEquals("statement:5", statement(document, "statement:11")
                .path("continuation").asText());
        assertEquals("statement:13", statement(document, "statement:12")
                .path("continuation").asText());
        assertEquals("statement:10", statement(document, "statement:11")
                .path("header").path("containment").path("parent").asText());
        assertEquals("THEN", statement(document, "statement:11")
                .path("header").path("containment").path("branch").asText());

        List<JsonNode> moves = elements(statements).stream()
                .filter(value -> value.path("variant").asText().equals("MOVE")).toList();
        assertEquals(List.of("A", "AUXPGM", "B", "NEST", "AFTER", "C", "D"),
                moves.stream().map(value -> value.path("source").path("value").asText())
                        .toList());
        assertTrue(moves.stream().allMatch(value ->
                value.path("source").path("kind").asText().equals("ALPHANUMERIC")
                        && value.path("target").path("role").asText().equals("WRITE")));
        List<JsonNode> calls = elements(statements).stream()
                .filter(value -> value.path("variant").asText().equals("CALL")).toList();
        assertTrue(calls.stream()
                .allMatch(value -> value.path("runtimeTarget").asText().equals("UNKNOWN")
                        && value.path("runtimeUncertaintyCode").asText()
                        .equals("DYNAMIC_CALL_TARGET_VALUE_UNKNOWN")
                        && value.path("target").path("reference").path("role").asText()
                        .equals("CALL_TARGET")));
        JsonNode outerIf = statement(document, "statement:10");
        assertEquals("RELATION", outerIf.path("condition").path("shape").asText());
        assertEquals(1, outerIf.path("condition").path("references").size());
        JsonNode conditionReference = outerIf.path("condition").path("references").get(0);
        assertEquals("READ", conditionReference.path("role").asText());
        assertEquals(dataHandle(document, "FLAG"), conditionReference.path("binding")
                .path("selected").asText());
        JsonNode observed = elements(statements).stream()
                .filter(value -> value.path("variant").asText().equals("OBSERVED"))
                .findFirst().orElseThrow();
        assertEquals("PRESERVED_STATEMENT", observed.path("observedKind").asText());
        assertEquals("GENERIC_PRESERVED_STATEMENT", observed.path("observedShape").asText());
        assertEquals("OBSERVED_STATEMENT_PARTIAL", observed.path("gapCode").asText());

        JsonNode coverage = document.path("coverage");
        assertEquals("COMPLETE", coverage.path("inventoryStatus").asText());
        assertEquals(14, coverage.path("observedStatements").asInt());
        // SP1.8 also proves the contained CALL normal completion.
        assertEquals(10, coverage.path("modeledStatements").asInt());
        assertEquals(4, coverage.path("partialStatements").asInt());
        assertEquals(0, coverage.path("unsupportedStatements").asInt());
        assertEquals(0, coverage.path("inputMissingStatements").asInt());
        assertEquals("BLOCKED", coverage.path("readiness").path("lowering")
                .path("status").asText());
        assertEquals("BLOCKED", coverage.path("readiness").path("cfg")
                .path("status").asText());
        assertEquals("BLOCKED", coverage.path("readiness").path("effectsDataflow")
                .path("status").asText());
        assertEquals("SUFFICIENT", moves.get(0).path("header").path("readiness")
                .path("lowering").path("status").asText());
        assertEquals("SUFFICIENT", moves.get(0).path("header").path("readiness")
                .path("cfg").path("status").asText());
        assertEquals("SUFFICIENT", calls.get(0).path("header").path("readiness")
                .path("lowering").path("status").asText());
        assertEquals("KNOWN", calls.get(0).path("normalContinuation").path("availability").asText());
        assertTrue(elements(statements).stream().anyMatch(f -> f.path("header").path("id").equals(calls.get(0).path("normalContinuation").path("statement"))));
        assertEquals("PARTIAL", outerIf.path("header").path("readiness")
                .path("lowering").path("status").asText());
        assertEquals("BLOCKED", observed.path("header").path("readiness")
                .path("lowering").path("status").asText());

        assertEquals(3, countGap(document, "RUNTIME_CALL_TARGET",
                "DYNAMIC_CALL_TARGET_VALUE_UNKNOWN"));
        assertEquals(3, countGap(document, "CONDITION_SEMANTICS",
                "CONDITION_SEMANTICS_NOT_AVAILABLE"));
        assertEquals(1, countGap(document, "CAPABILITY",
                "OBSERVED_STATEMENT_PARTIAL"));

        assertTrue(elements(data).stream()
                .allMatch(entry -> entry.path("provenance").path("exact").asBoolean()));
        assertTrue(elements(statements).stream().allMatch(entry -> entry.path("header")
                .path("provenance").path("exact").asBoolean()));
        assertInternalReferences(document);
        assertNoVolatileFields(document);
    }

    @Test
    void bindingReasonsAndPublishedIncludeChainOrderCrossWithoutNormalization()
            throws IOException {
        String qualifiedSource = String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEMANTIC-QUALIFIED.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 GROUP-A.",
                "          05 ITEM PIC X.",
                "       01 CTRL-GROUP.",
                "          05 PGM PIC X(8).",
                "       PROCEDURE DIVISION.",
                "           MOVE 'X' TO ITEM OF GROUP-A.",
                "           CALL PGM OF CTRL-GROUP.",
                "           GOBACK.",
                "       END PROGRAM SEMANTIC-QUALIFIED.", "");
        JsonNode qualified = parse(SemanticProductJsonWriter.serialize(publish(
                qualifiedSource, "semantic-product-qualified-json.cbl")));

        JsonNode moveBinding = elements(qualified.path("statements")).stream()
                .filter(statement -> statement.path("variant").asText().equals("MOVE"))
                .findFirst().orElseThrow().path("target").path("binding");
        JsonNode callBinding = elements(qualified.path("statements")).stream()
                .filter(statement -> statement.path("variant").asText().equals("CALL"))
                .findFirst().orElseThrow().path("target").path("reference").path("binding");
        assertEquals("QUALIFIED_HIERARCHY_MATCH", moveBinding.path("reason").asText());
        assertEquals("QUALIFIED_HIERARCHY_MATCH", callBinding.path("reason").asText());

        CobolSemanticProduct.IncludeFrame outer = new CobolSemanticProduct.IncludeFrame(
                "main.cbl", "OUTER", "outer.cpy", 7);
        CobolSemanticProduct.IncludeFrame inner = new CobolSemanticProduct.IncludeFrame(
                "outer.cpy", "INNER", "inner.cpy", 3);
        CobolSemanticProduct.Provenance provenance = new CobolSemanticProduct.Provenance(
                new CobolSemanticProduct.Location("expanded.cbl", 1, 0, 1, 8),
                new CobolSemanticProduct.Location("inner.cpy", 2, 0, 2, 8),
                List.of(outer, inner), false);
        CobolSemanticProduct.UnitId unit = new CobolSemanticProduct.UnitId(
                "main.cbl", List.of(0), "INCLUDE-ORDER");
        CobolSemanticProduct.Readiness blocked = readiness(
                CobolSemanticProduct.ReadinessStatus.BLOCKED);
        CobolSemanticProduct.DataItemId alphaId =
                new CobolSemanticProduct.DataItemId(unit, 0);
        CobolSemanticProduct.DataItemId zetaId =
                new CobolSemanticProduct.DataItemId(unit, 1);
        CobolSemanticProduct.StatementId ifId =
                new CobolSemanticProduct.StatementId(unit, 0);
        CobolSemanticProduct.DataReference ambiguousReference =
                new CobolSemanticProduct.DataReference(
                        new CobolSemanticProduct.OperandId(ifId, 0),
                        CobolSemanticProduct.OperandRole.READ,
                        CobolSemanticProduct.NominalBinding.incomplete(
                                CobolSemanticProduct.ResolutionStatus.AMBIGUOUS,
                                CobolSemanticProduct.ResolutionReason.MULTIPLE_VALID_CANDIDATES,
                                List.of(new CobolSemanticProduct.DataCandidate(zetaId, "ZETA"),
                                        new CobolSemanticProduct.DataCandidate(alphaId, "ALPHA"))),
                        provenance);
        CobolSemanticProduct.IfFact terminalIf = new CobolSemanticProduct.IfFact(
                new CobolSemanticProduct.StatementHeader(ifId,
                        new CobolSemanticProduct.ProgramPoint(0),
                        CobolSemanticProduct.Containment.root(), provenance,
                        CobolSemanticProduct.CoverageStatus.PARTIAL, blocked),
                new CobolSemanticProduct.ConditionSurface(
                        "RELATION", List.of(ambiguousReference), provenance),
                true, java.util.Optional.empty());
        CobolSemanticProduct.State state = new CobolSemanticProduct.State(unit,
                CobolSemanticProduct.Policy.unspecified(),
                List.of(new CobolSemanticProduct.DataDeclaration(
                        alphaId, "ALPHA",
                        java.util.Optional.empty(), provenance,
                        CobolSemanticProduct.CoverageStatus.PARTIAL, blocked),
                        new CobolSemanticProduct.DataDeclaration(
                                zetaId, "ZETA",
                                java.util.Optional.empty(), provenance,
                                CobolSemanticProduct.CoverageStatus.PARTIAL, blocked)),
                List.of(terminalIf), List.of(new CobolSemanticProduct.Gap(
                        ifId, CobolSemanticProduct.GapScope.NOMINAL_BINDING,
                        "REFERENCE_AMBIGUOUS_MULTIPLE_VALID_CANDIDATES",
                        "binding is deliberately incomplete", provenance)),
                new CobolSemanticProduct.CoverageSummary(
                        CobolSemanticProduct.InventoryStatus.COMPLETE,
                        1, 0, 1, 0, 0, blocked));
        JsonNode includeDocument = parse(SemanticProductJsonWriter.serialize(
                CobolSemanticPort.open(state)));
        JsonNode declaration = includeDocument.path("dataDeclarations").get(0);

        assertTrue(declaration.has("picture"));
        assertTrue(declaration.get("picture").isNull(),
                "an unavailable optional value must be explicit, not silently omitted");
        assertEquals(List.of("OUTER", "INNER"), elements(declaration.path("provenance")
                        .path("includeChain")).stream()
                .map(frame -> frame.path("requestedName").asText()).toList());
        JsonNode serializedIf = includeDocument.path("statements").get(0);
        assertTrue(serializedIf.has("continuation"));
        assertTrue(serializedIf.get("continuation").isNull());
        JsonNode serializedBinding = serializedIf.path("condition").path("references")
                .get(0).path("binding");
        assertEquals(List.of("ZETA", "ALPHA"),
                elements(serializedBinding.path("candidates")).stream()
                        .map(candidate -> candidate.path("canonicalName").asText()).toList(),
                "candidate authority order must cross the transport unchanged");
        assertTrue(serializedBinding.has("selected"));
        assertTrue(serializedBinding.get("selected").isNull());
    }

    @Test
    void structuralEditProducesAConsistentNewPublicationWithoutLongitudinalIdPromise()
            throws IOException {
        String originalSource = Files.readString(FIXTURE, StandardCharsets.UTF_8);
        String editedSource = originalSource.replace(
                "           DISPLAY 'UNMODELED'.",
                "           MOVE 'EDIT' TO WS-X.\n           DISPLAY 'UNMODELED'.");

        JsonNode original = parse(SemanticProductJsonWriter.serialize(publish(
                originalSource, FIXTURE.getFileName().toString())));
        JsonNode edited = parse(SemanticProductJsonWriter.serialize(publish(
                editedSource, FIXTURE.getFileName().toString())));

        assertEquals(original.path("statements").size() + 1,
                edited.path("statements").size());
        assertInternalReferences(edited);
        assertNoVolatileFields(edited);
        // INV-SP-006 deliberately makes no assertion that pre-edit handles stay unchanged.
    }

    @Test
    void compositionRootWritesExactlyTheAdapterBytes(@TempDir Path directory)
            throws Exception {
        Path copybooks = Files.createDirectory(directory.resolve("copybooks"));
        Path output = directory.resolve("output");

        ExplorerMain.main(new String[]{
                "--source", FIXTURE.toAbsolutePath().toString(),
                "--copybooks", copybooks.toString(),
                "--output", output.toString()});

        Path artifact = output.resolve("semantic-product.json");
        assertTrue(Files.isRegularFile(artifact));
        byte[] actual = Files.readAllBytes(artifact);
        assertArrayEquals(SemanticProductJsonWriter.serialize(publishFixture()), actual);
        assertInternalReferences(parse(actual));
    }

    private static CobolSemanticPort publishFixture() throws IOException {
        return publish(Files.readString(FIXTURE, StandardCharsets.UTF_8),
                FIXTURE.getFileName().toString());
    }

    private static CobolSemanticPort publish(String source, String sourceName) {
        AstBoundaryTestSupport.Analysis frontend = AstBoundaryTestSupport.analyze(source, sourceName);
        ResolutionContracts.ProgramUnitId unitId = frontend.model().programUnits().get(0).id();
        return ExplorerMain.publishSemanticProduct(unitId, frontend.build(), frontend.tables(),
                frontend.occurrences(), frontend.resolution(), frontend.report());
    }

    private static CobolSemanticProduct.Readiness readiness(
            CobolSemanticProduct.ReadinessStatus status) {
        return new CobolSemanticProduct.Readiness(
                new CobolSemanticProduct.ReadinessClaim(status, "test lowering"),
                new CobolSemanticProduct.ReadinessClaim(status, "test cfg"),
                new CobolSemanticProduct.ReadinessClaim(status, "test effects"));
    }

    private static JsonNode parse(byte[] bytes) throws IOException {
        JsonNode document = JSON.readTree(bytes);
        assertTrue(document != null && document.isObject(), "transport must be one JSON object");
        return document;
    }

    private static void assertInternalReferences(JsonNode document) {
        Set<String> dataIds = new LinkedHashSet<>();
        for (JsonNode declaration : document.path("dataDeclarations")) {
            assertEquals(Set.of("id", "canonicalName", "picture", "provenance", "coverage",
                    "readiness", "scalarText"), fieldSet(declaration));
            assertTrue(dataIds.add(declaration.path("id").asText()), "duplicate DATA handle");
        }

        Set<String> statementIds = new LinkedHashSet<>();
        for (JsonNode statement : document.path("statements")) {
            String variant = statement.path("variant").asText();
            assertEquals(expectedStatementFields(variant), fieldSet(statement));
            JsonNode header = statement.path("header");
            assertEquals(Set.of("id", "programPoint", "containment", "provenance",
                    "coverage", "readiness"), fieldSet(header));
            assertTrue(statementIds.add(header.path("id").asText()),
                    "duplicate statement handle");
        }

        List<String> canonicalRoots = elements(document.path("statements")).stream()
                .filter(statement -> statement.path("header").path("containment")
                        .path("branch").asText().equals("ROOT"))
                .map(statement -> statement.path("header").path("id").asText()).toList();
        assertEquals(canonicalRoots, textValues(document.path("structure").path("roots")));
        assertTrue(textValues(document.path("structure").path("roots")).stream()
                .allMatch(statementIds::contains));

        for (JsonNode statement : document.path("statements")) {
            String statementId = statement.path("header").path("id").asText();
            JsonNode containment = statement.path("header").path("containment");
            assertEquals(Set.of("parent", "branch"), fieldSet(containment));
            JsonNode parent = containment.get("parent");
            if (!parent.isNull()) {
                assertTrue(statementIds.contains(parent.asText()));
                assertEquals("IF", statement(document, parent.asText())
                        .path("variant").asText());
                assertTrue(Set.of("THEN", "ELSE").contains(
                        containment.path("branch").asText()));
            } else {
                assertTrue(Set.of("ROOT", "UNKNOWN").contains(
                        containment.path("branch").asText()));
            }

            switch (statement.path("variant").asText()) {
                case "MOVE" -> {
                    assertOperandOwner(statement.path("source"), statementId);
                    assertOperandOwner(statement.path("target"), statementId);
                    assertBindingReferences(statement.path("target").path("binding"), dataIds);
                }
                case "CALL" -> {
                    assertOperandOwner(statement.path("target").path("reference"), statementId);
                    assertBindingReferences(statement.path("target").path("reference").path("binding"), dataIds);
                }
                case "IF" -> {
                    for (JsonNode reference : statement.path("condition").path("references")) {
                        assertOperandOwner(reference, statementId);
                        assertBindingReferences(reference.path("binding"), dataIds);
                    }
                    JsonNode continuation = statement.get("continuation");
                    assertTrue(continuation.isNull()
                                    || statementIds.contains(continuation.asText()),
                            "IF continuation must resolve to a published statement");
                }
                case "OBSERVED" -> assertTrue(elements(document.path("gaps")).stream()
                        .anyMatch(gap -> gap.path("statement").asText().equals(statementId)
                                && gap.path("code").asText()
                                .equals(statement.path("gapCode").asText())));
                case "GOBACK" -> {
                    assertEquals("CURRENT_PROGRAM_INVOCATION", statement.path("exit").asText());
                    assertEquals("NONE", statement.path("localContinuation").asText());
                }
                default -> throw new AssertionError("unexpected statement variant "
                        + statement.path("variant").asText());
            }
        }

        for (JsonNode branch : document.path("structure").path("branches")) {
            String parent = branch.path("parent").asText();
            String branchKind = branch.path("branch").asText();
            assertTrue(statementIds.contains(parent));
            assertEquals("IF", statement(document, parent).path("variant").asText());
            assertTrue(Set.of("THEN", "ELSE").contains(branchKind));
            for (JsonNode child : branch.path("children")) {
                assertTrue(statementIds.contains(child.asText()));
                JsonNode childContainment = statement(document, child.asText())
                        .path("header").path("containment");
                assertEquals(parent, childContainment.path("parent").asText());
                assertEquals(branchKind, childContainment.path("branch").asText());
            }
        }
        for (JsonNode gap : document.path("gaps"))
            assertTrue(statementIds.contains(gap.path("statement").asText()),
                    "gap must resolve to a published statement");
    }

    private static Set<String> expectedStatementFields(String variant) {
        return switch (variant) {
            case "MOVE" -> Set.of("variant", "header", "source", "target", "copySemantics", "normalContinuation", "textAdjustment");
            case "CALL" -> Set.of("variant", "header", "syntax", "target",
                    "runtimeTarget", "runtimeUncertaintyCode", "normalContinuation", "surface", "effects", "outcomes");
            case "IF" -> Set.of("variant", "header", "condition",
                    "explicitlyTerminated", "continuation", "normalContinuation", "thenArm", "elseArm", "profile");
            case "OBSERVED" -> Set.of("variant", "header", "observedKind",
                    "observedShape", "gapCode", "knownReferences", "normalContinuation");
            default -> throw new AssertionError("unexpected statement variant " + variant);
        };
    }

    private static void assertBindingReferences(JsonNode binding, Set<String> dataIds) {
        assertEquals(Set.of("status", "reason", "candidates", "selected"), fieldSet(binding));
        List<String> candidates = elements(binding.path("candidates")).stream()
                .map(candidate -> candidate.path("id").asText()).toList();
        assertTrue(candidates.stream().allMatch(dataIds::contains));
        JsonNode selected = binding.get("selected");
        if (selected.isNull()) {
            assertNotEquals("RESOLVED", binding.path("status").asText());
        } else {
            assertEquals("RESOLVED", binding.path("status").asText());
            assertEquals(List.of(selected.asText()), candidates);
        }
    }

    private static void assertOperandOwner(JsonNode operand, String statementId) {
        String localStatementId = statementId.substring("statement:".length());
        assertTrue(operand.path("id").asText().startsWith("operand:"
                + localStatementId + ":"));
    }

    private static void assertBranch(JsonNode document, String parent, String branch,
                                     List<String> expectedChildren) {
        JsonNode relation = elements(document.path("structure").path("branches")).stream()
                .filter(candidate -> candidate.path("parent").asText().equals(parent)
                        && candidate.path("branch").asText().equals(branch))
                .findFirst().orElseThrow();
        assertEquals(expectedChildren, textValues(relation.path("children")));
    }

    private static JsonNode statement(JsonNode document, String id) {
        return elements(document.path("statements")).stream()
                .filter(statement -> statement.path("header").path("id").asText().equals(id))
                .findFirst().orElseThrow();
    }

    private static long countVariant(JsonNode statements, String variant) {
        return elements(statements).stream()
                .filter(statement -> statement.path("variant").asText().equals(variant)).count();
    }

    private static long countGap(JsonNode document, String scope, String code) {
        return elements(document.path("gaps")).stream()
                .filter(gap -> gap.path("scope").asText().equals(scope)
                        && gap.path("code").asText().equals(code)).count();
    }

    private static String dataHandle(JsonNode document, String canonicalName) {
        return elements(document.path("dataDeclarations")).stream()
                .filter(data -> data.path("canonicalName").asText().equals(canonicalName))
                .map(data -> data.path("id").asText()).findFirst().orElseThrow();
    }

    private static void assertNoVolatileFields(JsonNode node) {
        if (node.isObject()) {
            node.properties().forEach(field -> {
                assertFalse(VOLATILE_FIELDS.contains(field.getKey().toLowerCase()),
                        () -> "volatile execution field leaked into semantic payload: "
                                + field.getKey());
                assertNoVolatileFields(field.getValue());
            });
        } else if (node.isArray()) {
            node.forEach(SemanticProductCheckpoint7JsonTest::assertNoVolatileFields);
        }
    }

    private static Set<String> fieldSet(JsonNode object) {
        Set<String> fields = new LinkedHashSet<>();
        object.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private static List<String> fieldList(JsonNode object) {
        List<String> fields = new ArrayList<>();
        object.fieldNames().forEachRemaining(fields::add);
        return List.copyOf(fields);
    }

    private static List<JsonNode> elements(JsonNode array) {
        List<JsonNode> values = new ArrayList<>();
        array.forEach(values::add);
        return List.copyOf(values);
    }

    private static List<String> textValues(JsonNode array) {
        return elements(array).stream().map(JsonNode::asText).toList();
    }

    private static List<Integer> integerValues(JsonNode array) {
        return elements(array).stream().map(JsonNode::asInt).toList();
    }
}
