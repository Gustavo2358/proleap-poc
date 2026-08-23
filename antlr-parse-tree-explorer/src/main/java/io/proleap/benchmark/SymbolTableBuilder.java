package io.proleap.benchmark;

import java.util.*;

/** Collects declarations and scopes from an AST. It never visits statement references. */
final class SymbolTableBuilder {
    private record DataLevel(int level, int scopeId) {}

    private final List<SymbolTable.Scope> scopes = new ArrayList<>();
    private final List<SymbolTable.Symbol> symbols = new ArrayList<>();
    private final List<SymbolTable.Diagnostic> diagnostics = new ArrayList<>();

    SymbolTable build(Ast.Program program) {
        scopes.clear(); symbols.clear(); diagnostics.clear();
        int root = addScope(-1, SymbolTable.ScopeKind.ROOT, "<root>", -1, -1);
        int programSymbol = addSymbol(SymbolTable.SymbolKind.PROGRAM, SymbolTable.Namespace.PROGRAM,
                program.name(), root, program, programAttributes(program));
        int programScope = addScope(root, SymbolTable.ScopeKind.PROGRAM, program.name(),
                programSymbol, program.meta().id());

        for (Ast.Division division : program.divisions()) {
            int divisionScope = addScope(programScope, SymbolTable.ScopeKind.DIVISION,
                    division.divisionKind().name(), -1, division.meta().id());
            switch (division.divisionKind()) {
                case ENVIRONMENT -> collectEnvironment(division, divisionScope);
                case DATA -> collectDataDivision(division, divisionScope);
                case PROCEDURE -> collectProcedureDivision(division, divisionScope);
                case IDENTIFICATION -> { }
            }
        }
        detectDuplicateDeclarations();
        return new SymbolTable(scopes, symbols, diagnostics);
    }

    private void collectEnvironment(Ast.Division division, int divisionScope) {
        for (Ast.Node node : division.children()) {
            if (node instanceof Ast.FileBinding binding) {
                addSymbol(SymbolTable.SymbolKind.FILE_CONTROL, SymbolTable.Namespace.FILE,
                        binding.logicalName(), divisionScope, binding,
                        Map.of("assignment", binding.assignment()));
            }
        }
    }

    private void collectDataDivision(Ast.Division division, int divisionScope) {
        for (Ast.Node node : division.children()) {
            if (!(node instanceof Ast.Section section)) continue;
            int sectionScope = addScope(divisionScope, SymbolTable.ScopeKind.SECTION,
                    section.name(), -1, section.meta().id());
            for (Ast.Node child : section.children()) {
                if (child instanceof Ast.FileDescription file) {
                    int fileSymbol = addSymbol(SymbolTable.SymbolKind.FILE_DESCRIPTION,
                            SymbolTable.Namespace.FILE, file.fileName(), sectionScope, file,
                            Map.of("visibility", file.visibility().name()));
                    int fileScope = addScope(sectionScope, SymbolTable.ScopeKind.FILE_DESCRIPTION,
                            file.fileName(), fileSymbol, file.meta().id());
                    collectDataEntries(file.entries(), fileScope);
                } else if (child instanceof Ast.DataEntry entry) {
                    collectDataEntries(section.children().stream()
                            .filter(Ast.DataEntry.class::isInstance)
                            .map(Ast.DataEntry.class::cast).toList(), sectionScope);
                    break;
                }
            }
        }
    }

    private void collectDataEntries(List<Ast.DataEntry> entries, int sectionScope) {
        for (Ast.DataEntry entry : entries) {
            int level = parseLevel(entry.level());
            if (level == 88) {
                if (!isFiller(entry.name())) {
                    addSymbol(SymbolTable.SymbolKind.CONDITION_NAME, SymbolTable.Namespace.DATA,
                            entry.name(), sectionScope, entry, dataAttributes(entry));
                }
                continue;
            }
            SymbolTable.SymbolKind kind = level == 66
                    ? SymbolTable.SymbolKind.RENAMES
                    : SymbolTable.SymbolKind.DATA_ITEM;
            int symbolId = isFiller(entry.name()) ? -1 : addSymbol(kind, SymbolTable.Namespace.DATA,
                            entry.name(), sectionScope, entry, dataAttributes(entry));
            String scopeName = symbolId < 0 ? "<FILLER@" + entry.meta().span().startLine() + ">" : entry.name();
            int itemScope = addScope(sectionScope, SymbolTable.ScopeKind.DATA_ITEM,
                    scopeName, symbolId, entry.meta().id());
            for (Ast.DataClause clause : entry.clauses()) {
                if (clause instanceof Ast.OccursClause occurs) {
                    for (Ast.IndexReference index : occurs.indexes()) {
                        addSymbol(SymbolTable.SymbolKind.INDEX_NAME, SymbolTable.Namespace.DATA,
                                index.indexName(), itemScope, index, Map.of("relation", "OCCURS_INDEX"));
                    }
                }
            }
            collectDataEntries(entry.children(), itemScope);
        }
    }

    private void collectProcedureDivision(Ast.Division division, int divisionScope) {
        for (Ast.Node node : division.children()) {
            if (node instanceof Ast.Section section) {
                int sectionSymbol = addSymbol(SymbolTable.SymbolKind.PROCEDURE_SECTION,
                        SymbolTable.Namespace.PROCEDURE, section.name(), divisionScope, section, Map.of());
                int sectionScope = addScope(divisionScope, SymbolTable.ScopeKind.SECTION,
                        section.name(), sectionSymbol, section.meta().id());
                for (Ast.Node child : section.children()) {
                    if (child instanceof Ast.Paragraph paragraph) collectParagraph(paragraph, sectionScope);
                }
            } else if (node instanceof Ast.Paragraph paragraph) {
                collectParagraph(paragraph, divisionScope);
            }
        }
    }

    private void collectParagraph(Ast.Paragraph paragraph, int declaringScope) {
        int symbolId = paragraph.name().startsWith("<") ? -1
                : addSymbol(SymbolTable.SymbolKind.PARAGRAPH, SymbolTable.Namespace.PROCEDURE,
                        paragraph.name(), declaringScope, paragraph, Map.of());
        addScope(declaringScope, SymbolTable.ScopeKind.PARAGRAPH, paragraph.name(),
                symbolId, paragraph.meta().id());
    }

    private int addScope(int parentId, SymbolTable.ScopeKind kind, String name,
                         int ownerSymbolId, int astNodeId) {
        int id = scopes.size();
        scopes.add(new SymbolTable.Scope(id, parentId, kind, name, ownerSymbolId, astNodeId));
        return id;
    }

    private int addSymbol(SymbolTable.SymbolKind kind, SymbolTable.Namespace namespace,
                          String name, int scopeId, Ast.Node declaration,
                          Map<String, String> attributes) {
        int id = symbols.size();
        symbols.add(new SymbolTable.Symbol(id, kind, namespace, name,
                SymbolTable.canonical(name), scopeId, declaration.meta().id(),
                declaration.meta().span(), attributes));
        return id;
    }

    private void detectDuplicateDeclarations() {
        record Key(int scopeId, SymbolTable.Namespace namespace, String canonicalName) {}
        Map<Key, List<Integer>> grouped = new LinkedHashMap<>();
        for (SymbolTable.Symbol symbol : symbols) {
            Key key = new Key(symbol.scopeId(), symbol.namespace(), symbol.canonicalName());
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(symbol.id());
        }
        for (var entry : grouped.entrySet()) {
            if (entry.getValue().size() < 2) continue;
            Key key = entry.getKey();
            diagnostics.add(new SymbolTable.Diagnostic("DUPLICATE_DECLARATION",
                    "Multiple " + key.namespace() + " declarations named " + key.canonicalName()
                            + " in the same scope", key.scopeId(), entry.getValue()));
        }
    }

    private static Map<String, String> dataAttributes(Ast.DataEntry entry) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("level", entry.level());
        attributes.put("levelKind", entry.levelKind().name());
        attributes.put("visibility", entry.visibility().name());
        attributes.put("declaration", entry.declaration());
        for (Ast.DataClause clause : entry.clauses()) {
            if (clause instanceof Ast.RedefinesClause redefines) {
                attributes.put("redefinesTarget", redefines.target().writtenText());
                attributes.put("relationBinding", "NOT_PERFORMED");
            } else if (clause instanceof Ast.RenamesClause renames) {
                attributes.put("renamesFrom", renames.from().writtenText());
                if (renames.through() != null) attributes.put("renamesThrough", renames.through().writtenText());
                attributes.put("relationBinding", "NOT_PERFORMED");
            } else if (clause instanceof Ast.OccursClause occurs && occurs.dependingOn() != null) {
                attributes.put("occursDependingOn", occurs.dependingOn().writtenText());
                attributes.put("relationBinding", "NOT_PERFORMED");
            }
        }
        return attributes;
    }

    private static Map<String, String> programAttributes(Ast.Program program) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("common", Boolean.toString(program.attributes().common()));
        attributes.put("initial", Boolean.toString(program.attributes().initial()));
        attributes.put("recursive", Boolean.toString(program.attributes().recursive()));
        attributes.put("library", Boolean.toString(program.attributes().library()));
        attributes.put("definition", Boolean.toString(program.attributes().definition()));
        return attributes;
    }

    private static int parseLevel(String level) {
        try { return Integer.parseInt(level); }
        catch (NumberFormatException ignored) { return -1; }
    }

    private static boolean isFiller(String name) {
        return "FILLER".equalsIgnoreCase(name);
    }
}
