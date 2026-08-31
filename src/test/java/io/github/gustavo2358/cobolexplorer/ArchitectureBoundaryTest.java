package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces architectural dependencies while the project remains in one Java package.
 * It reads constant-pool class references, so imports alone cannot bypass the check.
 */
class ArchitectureBoundaryTest {
    private static final String PROJECT_PREFIX = "io.github.gustavo2358.cobolexplorer.";

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
        for (int index = 1; index < count; index++) {
            switch (input.readUnsignedByte()) {
                case 1 -> utf8.put(index, input.readUTF());
                case 3, 4 -> input.readInt();
                case 5, 6 -> {
                    input.readLong();
                    index++;
                }
                case 7 -> classNameIndexes.put(index, input.readUnsignedShort());
                case 8, 16, 19, 20 -> input.readUnsignedShort();
                case 9, 10, 11, 12, 17, 18 -> {
                    input.readUnsignedShort();
                    input.readUnsignedShort();
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
            String name = utf8.get(nameIndex);
            if (name != null && name.startsWith(PROJECT_PREFIX.replace('.', '/'))) result.add(name);
            if (name != null && name.startsWith("org/antlr/v4/")) result.add(name);
        }
        return result;
    }
}
