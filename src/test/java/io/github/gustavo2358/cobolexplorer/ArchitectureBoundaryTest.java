package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private static final Pattern DESCRIPTOR_CLASS =
            Pattern.compile("L([A-Za-z0-9_$/]+)(?=[;<])");

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
                || reference.startsWith(SEMANTIC_PRODUCT_PREFIX + "adapter/");
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
