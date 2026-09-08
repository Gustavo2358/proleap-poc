package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.consumer.CobolLoweringReadinessConsumer;
import io.github.gustavo2358.cobolexplorer.semanticproduct.loweringreadiness.SemanticPortLoweringProbe;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces architectural dependencies while the project remains in one Java package.
 * It reads type references from classfile metadata and the constant pool, so
 * imports alone cannot bypass the check.
 */
class ArchitectureBoundaryTest {
    private static final String PROJECT_PREFIX = "io.github.gustavo2358.cobolexplorer.";
    private static final String PROJECT_PREFIX_INTERNAL = PROJECT_PREFIX.replace('.', '/');
    private static final String SEMANTIC_PRODUCT_PREFIX =
            (PROJECT_PREFIX + "semanticproduct.").replace('.', '/');
    private static final String ANTLR_PREFIX = "org/antlr/v4/";
    private static final String SEMANTIC_PORT_INTERNAL =
            CobolSemanticPort.class.getName().replace('.', '/');
    private static final String SEMANTIC_PRODUCT_INTERNAL =
            CobolSemanticProduct.class.getName().replace('.', '/');
    private static final String LOWERING_CONSUMER_INTERNAL =
            CobolLoweringReadinessConsumer.class.getName().replace('.', '/');
    private static final String JSON_WRITER_INTERNAL =
            SemanticProductJsonWriter.class.getName().replace('.', '/');
    private static final String LOWERING_PROBE_INTERNAL =
            SemanticPortLoweringProbe.class.getName().replace('.', '/');
    private static final Pattern DESCRIPTOR_CLASS =
            Pattern.compile("L([A-Za-z0-9_$/]+)(?=[;<])");
    private static final Pattern JAVA_IMPORT =
            Pattern.compile("(?m)^import\\s+(?:static\\s+)?([^;]+);");

    @Test
    void frontendPublicationHasNoAirJavaDependency() throws Exception {
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var pom = factory.newDocumentBuilder().parse(Path.of("pom.xml").toFile());
        var dependencies = pom.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            var dependency = (org.w3c.dom.Element) dependencies.item(index);
            String artifact = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
            assertTrue(!artifact.equals("air-java"), "ADR-0013: air-java belongs to the external lowerer");
        }
        List<Class<?>> components = semanticProductTypes();
        addNestedTypes(CobolSemanticProductProjector.class, components);
        addNestedTypes(SemanticProductJsonWriter.class, components);
        for (Class<?> component : components) {
            assertTrue(directDependencies(component).stream().allMatch(reference ->
                            reference.startsWith("java/") || reference.startsWith("javax/")
                                    || reference.startsWith(PROJECT_PREFIX_INTERNAL)
                                    || reference.startsWith("com/fasterxml/jackson/")),
                    () -> "Semantic Product depends on an external implementation: " + component.getName());
        }
    }

    @Test
    void astConstructionDoesNotDependOnLaterSemanticProductsOrPresentation() throws Exception {
        assertNoDirectDependencies("INV-AST-001", List.of(Ast.class, AstBuildResult.class, AstBuilder.class),
                names(SymbolTable.class, SymbolTableBuilder.class, CompilationUnitSymbolTables.class,
                        ReferenceOccurrences.class, ReferenceOccurrenceCollector.class, ReferenceResolution.class,
                        CobolReferenceResolver.class, DataAndIndexReferenceResolver.class,
                        ExternalClassification.class, CicsIntrinsicClassifier.class,
                        ResolutionAnalysisReport.class, AstSnapshot.class, SymbolTableSnapshot.class,
                        ResolutionSnapshot.class, CoverageSnapshot.class, ExplorerMain.class));
    }

    @Test
    void symbolConstructionDoesNotDependOnParserOrReferenceResolution() throws Exception {
        List<Class<?>> symbolComponents = List.of(SymbolTable.class, SymbolTableBuilder.class,
                CompilationUnitSymbolTableBuilder.class, CompilationUnitSymbolTables.class);
        assertNoDirectDependencies("INV-SYM-001", symbolComponents,
                names(ReferenceOccurrences.class, ReferenceOccurrenceCollector.class, ReferenceResolution.class,
                        CobolReferenceResolver.class, DataAndIndexReferenceResolver.class,
                        ExternalClassification.class, CicsIntrinsicClassifier.class,
                        ResolutionAnalysisReport.class));
        for (Class<?> component : symbolComponents) {
            assertTrue(directDependencies(component).stream().noneMatch(name -> name.startsWith("org/antlr/v4/")),
                    () -> "INV-SYM-001: " + component.getSimpleName() + " depende diretamente de ANTLR");
        }
    }

    @Test
    void semanticProductsDoNotDependOnSnapshotsOrTheApplicationEntrypoint() throws Exception {
        assertNoDirectDependencies("ADR-0003", List.of(Ast.class, AstBuildResult.class,
                        CompilationUnitModel.class, CompilationUnitSymbolTables.class, SymbolTable.class,
                        ReferenceOccurrences.class, ReferenceResolution.class, ExternalClassification.class,
                        ResolutionAnalysisReport.class),
                names(AstSnapshot.class, SymbolTableSnapshot.class, CoverageSnapshot.class,
                        ResolutionSnapshot.class, ExplorerMain.class));
    }

    @Test
    void canonicalCobolProductsDoNotDependOnConcretePlatformClassification() throws Exception {
        assertNoDirectDependencies("INV-EXT-001", List.of(
                        Ast.class, AstBuilder.class, SymbolTable.class, SymbolTableBuilder.class,
                        CompilationUnitSymbolTableBuilder.class, ReferenceOccurrences.class,
                        ReferenceOccurrenceCollector.class, ReferenceResolution.class,
                        CobolReferenceResolver.class, DataAndIndexReferenceResolver.class),
                names(ExternalClassification.class, CicsIntrinsicClassifier.class));
    }

    @Test
    void semanticProductBoundaryDoesNotDependOnFrontendOrProjectionBytecode() throws Exception {
        for (Class<?> component : semanticProductTypes()) {
            Set<String> violations = new LinkedHashSet<>();
            for (String reference : directDependencies(component)) {
                if ((reference.startsWith(PROJECT_PREFIX_INTERNAL)
                        && !reference.startsWith(SEMANTIC_PRODUCT_PREFIX))
                        || reference.startsWith(ANTLR_PREFIX)
                        || isSemanticProductProjection(reference))
                    violations.add(reference);
            }
            assertTrue(violations.isEmpty(), () -> "EVAL-ARCH-001: "
                    + component.getName()
                    + " depende diretamente de frontend/projection: " + violations);
        }
    }

    @Test
    void semanticProductProjectionDoesNotDependOnAnalysisEnginesOrPresentation() throws Exception {
        List<Class<?>> projectionTypes = List.of(CobolSemanticProductProjector.class,
                CobolSemanticProductProjector.FrontendProducts.class);
        assertNoDirectDependencies("INV-SP-004", projectionTypes,
                names(AstBuilder.class, ReferenceOccurrenceCollector.class,
                        CobolReferenceResolver.class, DataAndIndexReferenceResolver.class,
                        SourceMap.class, AstSnapshot.class, SymbolTableSnapshot.class,
                        CoverageSnapshot.class, ResolutionSnapshot.class, ExplorerMain.class));
        for (Class<?> component : projectionTypes) {
            assertTrue(directDependencies(component).stream()
                            .noneMatch(name -> name.startsWith(ANTLR_PREFIX)),
                    () -> "INV-SP-004: " + component.getSimpleName()
                            + " depende diretamente de ANTLR");
        }
    }

    @Test
    void loweringReadinessConsumerDependsOnlyOnTheSemanticPortBoundary() throws Exception {
        List<Class<?>> consumerTypes = new ArrayList<>();
        addNestedTypes(CobolLoweringReadinessConsumer.class, consumerTypes);
        for (Class<?> component : consumerTypes) {
            Set<String> violations = new LinkedHashSet<>();
            for (String reference : directDependencies(component)) {
                if ((reference.startsWith(PROJECT_PREFIX_INTERNAL)
                        && !isConsumerBoundaryType(reference))
                        || reference.startsWith(ANTLR_PREFIX))
                    violations.add(reference);
            }
            assertTrue(violations.isEmpty(), () -> "INV-SP-003/EVAL-ARCH-001: "
                    + component.getName()
                    + " depende de frontend, projection, presentation ou composition root: "
                    + violations);
        }

        Path sourcePath = Path.of("src/main/java/io/github/gustavo2358/cobolexplorer/"
                + "semanticproduct/consumer/CobolLoweringReadinessConsumer.java");
        String source = Files.readString(sourcePath);
        Set<String> forbiddenImports = new LinkedHashSet<>();
        Matcher imports = JAVA_IMPORT.matcher(source);
        while (imports.find()) {
            String imported = imports.group(1).replace('.', '/');
            if (imported.startsWith(PROJECT_PREFIX_INTERNAL)
                    && !isConsumerBoundaryType(imported))
                forbiddenImports.add(imported);
        }
        assertTrue(forbiddenImports.isEmpty(), () -> "INV-SP-003/EVAL-ARCH-001: "
                + "consumer importa implementação fora do port: " + forbiddenImports);
        assertTrue(List.of("writtenText(", "grammarRule(", "org.antlr.v4").stream()
                        .noneMatch(source::contains),
                "INV-SP-003/INV-SP-004: consumer reinterpreta metadata do frontend");
    }

    @Test
    void semanticProductJsonAdapterDependsOnlyOnTheClosedBoundaryAndJsonLibrary()
            throws Exception {
        List<Class<?>> adapterTypes = new ArrayList<>();
        addNestedTypes(SemanticProductJsonWriter.class, adapterTypes);
        for (Class<?> component : adapterTypes) {
            Set<String> violations = new LinkedHashSet<>();
            for (String reference : directDependencies(component)) {
                if ((reference.startsWith(PROJECT_PREFIX_INTERNAL)
                        && !isJsonAdapterBoundaryType(reference))
                        || reference.startsWith(ANTLR_PREFIX))
                    violations.add(reference);
            }
            assertTrue(violations.isEmpty(), () -> "INV-SP-004/INV-SP-006: "
                    + component.getName()
                    + " depende de frontend, projection, consumer ou composition root: "
                    + violations);
        }

        Path sourcePath = Path.of("src/main/java/io/github/gustavo2358/cobolexplorer/"
                + "semanticproduct/transport/SemanticProductJsonWriter.java");
        String source = Files.readString(sourcePath);
        Set<String> forbiddenImports = new LinkedHashSet<>();
        Matcher imports = JAVA_IMPORT.matcher(source);
        while (imports.find()) {
            String imported = imports.group(1).replace('.', '/');
            if (imported.startsWith(PROJECT_PREFIX_INTERNAL)
                    && !isJsonAdapterBoundaryType(imported))
                forbiddenImports.add(imported);
        }
        assertTrue(forbiddenImports.isEmpty(), () -> "INV-SP-004/INV-SP-006: "
                + "JSON adapter importa implementação fora da boundary: " + forbiddenImports);
        assertTrue(List.of("writtenText(", "grammarRule(", "Map<String, Object>",
                        "Map<String,Object>", "org.antlr.v4").stream().noneMatch(source::contains),
                "INV-SP-004: JSON adapter reinterpreta frontend ou usa bag semântico genérico");
    }

    @Test
    void scalarOracleUsesOnlyPublicFactsAndProjectorDoesNotAnalyze() throws Exception {
        Class<?> oracle = io.github.gustavo2358.cobolexplorer.semanticproduct.scalar.ScalarMoveOracle.class;
        List<Class<?>> types = new ArrayList<>(); addNestedTypes(oracle, types);
        String own = oracle.getName().replace('.', '/');
        for (var type : types) for (String reference : directDependencies(type)) {
            assertTrue(!reference.startsWith(ANTLR_PREFIX), reference);
            if (reference.startsWith(PROJECT_PREFIX_INTERNAL)) assertTrue(
                    reference.startsWith(own) || reference.startsWith(SEMANTIC_PRODUCT_INTERNAL)
                            || reference.equals(SEMANTIC_PORT_INTERNAL), reference);
        }
        String oracleSource = Files.readString(Path.of("src/test/java/io/github/gustavo2358/cobolexplorer/semanticproduct/scalar/ScalarMoveOracle.java"));
        for (String forbidden : List.of("readiness()", "picture()", "canonicalName()", "rawLexeme", "programPoint"))
            assertTrue(!oracleSource.contains(forbidden), forbidden);
        String projection = Files.readString(Path.of("src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/projection/CobolSemanticProductProjector.java"));
        for (String forbidden : List.of("ScalarMoveSemantics.analyze", "rawLexeme()", "logicalExtent()", "extent() =="))
            assertTrue(!projection.contains(forbidden), forbidden);
    }

    @Test
    void checkpoint8ProbeDependsOnlyOnTheSemanticPortBoundary() throws Exception {
        List<Class<?>> probeTypes = new ArrayList<>();
        addNestedTypes(SemanticPortLoweringProbe.class, probeTypes);
        for (Class<?> component : probeTypes) {
            Set<String> violations = new LinkedHashSet<>();
            for (String reference : directDependencies(component)) {
                if ((reference.startsWith(PROJECT_PREFIX_INTERNAL)
                        && !isLoweringProbeBoundaryType(reference))
                        || reference.startsWith(ANTLR_PREFIX))
                    violations.add(reference);
            }
            assertTrue(violations.isEmpty(), () -> "INV-SP-003/EVAL-SP-002: "
                    + component.getName()
                    + " depende de frontend, projector, adapter ou consumer auxiliar: "
                    + violations);
        }

        Path sourcePath = Path.of("src/test/java/io/github/gustavo2358/cobolexplorer/"
                + "semanticproduct/loweringreadiness/SemanticPortLoweringProbe.java");
        String source = Files.readString(sourcePath);
        Set<String> forbiddenImports = new LinkedHashSet<>();
        Matcher imports = JAVA_IMPORT.matcher(source);
        while (imports.find()) {
            String imported = imports.group(1).replace('.', '/');
            if (imported.startsWith(PROJECT_PREFIX_INTERNAL)
                    && !isLoweringProbeBoundaryType(imported))
                forbiddenImports.add(imported);
        }
        assertTrue(forbiddenImports.isEmpty(), () -> "INV-SP-003/EVAL-SP-002: "
                + "probe importa implementação fora do port: " + forbiddenImports);
        assertTrue(List.of("Ast", "SymbolTable", "ReferenceOccurrences",
                        "ReferenceResolution", "ResolutionAnalysisReport", "SourceMap",
                        "CobolSemanticProductProjector", "SemanticProductJsonWriter",
                        "CobolLoweringReadinessConsumer", "ExplorerMain", "writtenText(",
                        "grammarRule(", "org.antlr.v4").stream().noneMatch(source::contains),
                "INV-SP-003/INV-SP-004: probe usa frontend, adapter ou consumer auxiliar");
    }

    @Test
    void bytecodeScannerSeesGenericAndRecordComponentTypeReferences() throws Exception {
        Set<String> references = directDependencies(BytecodeLeakageProbe.class);

        assertTrue(references.contains("io/github/gustavo2358/cobolexplorer/Ast"));
        assertTrue(references.contains("io/github/gustavo2358/cobolexplorer/ReferenceResolution"));
    }

    private static List<Class<?>> semanticProductTypes() throws ClassNotFoundException {
        List<Class<?>> types = new ArrayList<>();
        addNestedTypes(CobolSemanticProduct.class, types);
        types.add(CobolSemanticPort.class);
        types.add(Class.forName(
                "io.github.gustavo2358.cobolexplorer.semanticproduct.MaterializedCobolSemanticPort"));
        return types;
    }

    private static void addNestedTypes(Class<?> type, List<Class<?>> types) {
        if (types.contains(type)) return;
        types.add(type);
        for (Class<?> nested : type.getDeclaredClasses()) addNestedTypes(nested, types);
    }

    private static boolean isSemanticProductProjection(String reference) {
        return reference.startsWith(SEMANTIC_PRODUCT_PREFIX + "projection/")
                || reference.startsWith(SEMANTIC_PRODUCT_PREFIX + "adapter/")
                || reference.startsWith(SEMANTIC_PRODUCT_PREFIX + "transport/");
    }

    private static boolean isConsumerBoundaryType(String reference) {
        return reference.equals(SEMANTIC_PORT_INTERNAL)
                || reference.equals(SEMANTIC_PRODUCT_INTERNAL)
                || reference.startsWith(SEMANTIC_PRODUCT_INTERNAL + '$')
                || reference.equals(LOWERING_CONSUMER_INTERNAL)
                || reference.startsWith(LOWERING_CONSUMER_INTERNAL + '$');
    }

    private static boolean isJsonAdapterBoundaryType(String reference) {
        return reference.equals(SEMANTIC_PORT_INTERNAL)
                || reference.equals(SEMANTIC_PRODUCT_INTERNAL)
                || reference.startsWith(SEMANTIC_PRODUCT_INTERNAL + '$')
                || reference.equals(JSON_WRITER_INTERNAL)
                || reference.startsWith(JSON_WRITER_INTERNAL + '$');
    }

    private static boolean isLoweringProbeBoundaryType(String reference) {
        return reference.equals(SEMANTIC_PORT_INTERNAL)
                || reference.equals(SEMANTIC_PRODUCT_INTERNAL)
                || reference.startsWith(SEMANTIC_PRODUCT_INTERNAL + '$')
                || reference.equals(LOWERING_PROBE_INTERNAL)
                || reference.startsWith(LOWERING_PROBE_INTERNAL + '$');
    }

    private static void assertNoDirectDependencies(String boundary, List<Class<?>> components,
                                                   Set<String> forbidden) throws IOException {
        for (Class<?> component : components) {
            Set<String> violations = new LinkedHashSet<>(directDependencies(component));
            violations.retainAll(forbidden);
            assertTrue(violations.isEmpty(), () -> boundary + ": " + component.getSimpleName()
                    + " depende de produto posterior ou apresentação: " + violations);
        }
    }

    private static Set<String> names(Class<?>... types) {
        Set<String> names = new LinkedHashSet<>();
        for (Class<?> type : types) names.add(type.getName().replace('.', '/'));
        return names;
    }

    private static Set<String> directDependencies(Class<?> type) throws IOException {
        String resource = '/' + type.getName().replace('.', '/') + ".class";
        try (InputStream raw = type.getResourceAsStream(resource)) {
            assertTrue(raw != null, "bytecode não encontrado para " + type.getName());
            return classReferences(new DataInputStream(raw));
        }
    }

    private static Set<String> classReferences(DataInputStream input) throws IOException {
        assertEquals(0xCAFEBABE, input.readInt(), "classfile inválido");
        input.readUnsignedShort();
        input.readUnsignedShort();
        int count = input.readUnsignedShort();
        Map<Integer, String> utf8 = new HashMap<>();
        Map<Integer, Integer> classNameIndexes = new HashMap<>();
        Set<Integer> descriptorIndexes = new LinkedHashSet<>();
        for (int index = 1; index < count; index++) {
            switch (input.readUnsignedByte()) {
                case 1 -> utf8.put(index, input.readUTF());
                case 3, 4 -> input.readInt();
                case 5, 6 -> {
                    input.readLong();
                    index++;
                }
                case 7 -> classNameIndexes.put(index, input.readUnsignedShort());
                case 8, 19, 20 -> input.readUnsignedShort();
                case 16 -> descriptorIndexes.add(input.readUnsignedShort());
                case 9, 10, 11, 17, 18 -> {
                    input.readUnsignedShort();
                    input.readUnsignedShort();
                }
                case 12 -> {
                    input.readUnsignedShort();
                    descriptorIndexes.add(input.readUnsignedShort());
                }
                case 15 -> {
                    input.readUnsignedByte();
                    input.readUnsignedShort();
                }
                default -> throw new IOException("tag de constant pool não suportada");
            }
        }
        Set<String> result = new LinkedHashSet<>();
        for (int nameIndex : classNameIndexes.values()) {
            addClassReference(utf8.get(nameIndex), result);
        }
        for (int descriptorIndex : descriptorIndexes)
            addDescriptorReferences(utf8.get(descriptorIndex), result);

        readClassStructure(input, utf8, result);
        return result;
    }

    private static void readClassStructure(DataInputStream input, Map<Integer, String> utf8,
                                           Set<String> result) throws IOException {
        input.readUnsignedShort();
        input.readUnsignedShort();
        input.readUnsignedShort();
        skipIndexes(input, input.readUnsignedShort());
        readMembers(input, utf8, result);
        readMembers(input, utf8, result);
        readAttributes(input, utf8, result);
    }

    private static void readMembers(DataInputStream input, Map<Integer, String> utf8,
                                    Set<String> result) throws IOException {
        int count = input.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            input.readUnsignedShort();
            input.readUnsignedShort();
            addDescriptorReferences(utf8.get(input.readUnsignedShort()), result);
            readAttributes(input, utf8, result);
        }
    }

    private static void readAttributes(DataInputStream input, Map<Integer, String> utf8,
                                       Set<String> result) throws IOException {
        int count = input.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            String name = utf8.get(input.readUnsignedShort());
            int length = input.readInt();
            if (length < 0) throw new IOException("tamanho de atributo inválido");
            byte[] bytes = new byte[length];
            input.readFully(bytes);
            readAttribute(name, new DataInputStream(new ByteArrayInputStream(bytes)), utf8, result);
        }
    }

    private static void readAttribute(String name, DataInputStream input,
                                      Map<Integer, String> utf8, Set<String> result)
            throws IOException {
        if ("Signature".equals(name)) {
            addDescriptorReferences(utf8.get(input.readUnsignedShort()), result);
        } else if ("Record".equals(name)) {
            int count = input.readUnsignedShort();
            for (int index = 0; index < count; index++) {
                input.readUnsignedShort();
                addDescriptorReferences(utf8.get(input.readUnsignedShort()), result);
                readAttributes(input, utf8, result);
            }
        } else if ("Code".equals(name)) {
            input.readUnsignedShort();
            input.readUnsignedShort();
            int codeLength = input.readInt();
            if (codeLength < 0) throw new IOException("tamanho de bytecode inválido");
            input.skipNBytes(codeLength);
            skipIndexes(input, input.readUnsignedShort() * 4);
            readAttributes(input, utf8, result);
        } else if ("LocalVariableTable".equals(name)) {
            readLocalVariables(input, utf8, result);
        } else if ("LocalVariableTypeTable".equals(name)) {
            readLocalVariables(input, utf8, result);
        }
    }

    private static void readLocalVariables(DataInputStream input, Map<Integer, String> utf8,
                                           Set<String> result) throws IOException {
        int count = input.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            input.readUnsignedShort();
            input.readUnsignedShort();
            input.readUnsignedShort();
            addDescriptorReferences(utf8.get(input.readUnsignedShort()), result);
            input.readUnsignedShort();
        }
    }

    private static void skipIndexes(DataInputStream input, int count) throws IOException {
        for (int index = 0; index < count; index++) input.readUnsignedShort();
    }

    private static void addClassReference(String name, Set<String> result) {
        if (name == null) return;
        if (name.startsWith("[")) {
            addDescriptorReferences(name, result);
        } else if (name.startsWith(PROJECT_PREFIX_INTERNAL) || name.startsWith(ANTLR_PREFIX)) {
            result.add(name);
        }
    }

    private static void addDescriptorReferences(String descriptor, Set<String> result) {
        if (descriptor == null) return;
        Matcher matcher = DESCRIPTOR_CLASS.matcher(descriptor);
        while (matcher.find()) addClassReference(matcher.group(1), result);
    }

    private record BytecodeLeakageProbe<T extends Ast>(List<ReferenceResolution> references) { }
}
