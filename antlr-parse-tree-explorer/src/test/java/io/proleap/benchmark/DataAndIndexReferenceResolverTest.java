package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DataAndIndexReferenceResolverTest {
    private static final Path DATA = Path.of("src/test/resources/cobol/resolution/data-binding.cbl");
    private static final Path NESTED = Path.of("src/test/resources/cobol/resolution/nested-data-visibility.cbl");
    private static final Path GLOBAL_SUBORDINATES = Path.of(
            "src/test/resources/cobol/resolution/global-subordinate-visibility.cbl");
    private static final Path GLOBAL_FD_RECORDS = Path.of(
            "src/test/resources/cobol/resolution/global-fd-record-visibility.cbl");
    private static final Path NAMESPACE_SHADOWING = Path.of(
            "src/test/resources/cobol/resolution/nested-namespace-shadowing.cbl");
    private static final Path QUALIFIED_GLOBAL = Path.of(
            "src/test/resources/cobol/resolution/nested-qualified-global.cbl");
    private static final Path GLOBAL_THROUGH_LOCAL = Path.of(
            "src/test/resources/cobol/resolution/nested-global-through-local-data.cbl");
    private static final Path REDEFINES_STRUCTURAL = Path.of(
            "src/test/resources/cobol/resolution/redefines-structural-binding.cbl");
    private static final Path REDEFINES_DIFFERENT_LEVEL = Path.of(
            "src/test/resources/cobol/resolution/redefines-different-level-number.cbl");
    private static final Path RENAMES_STRUCTURAL = Path.of(
            "src/test/resources/cobol/resolution/renames-structural-binding.cbl");
    private static final Path SUBSCRIPT_SEMANTIC_KIND = Path.of(
            "src/test/resources/cobol/resolution/subscript-semantic-kind.cbl");

    @Test
    void resolvesSimpleDuplicateMissingAndIncompatibleNames() throws Exception {
        Analysis analysis = analyze(DATA, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();

        assertEntry(analysis.resolution(), unit, "WS-UNIQUE", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), unit, "DUPLICATE-ITEM", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
        assertEntry(analysis.resolution(), unit, "MISSING-ITEM", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0);
        assertEntry(analysis.resolution(), unit, "NORMAL-ITEM", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, 0);
    }

    @Test
    void resolvesOrderedQualificationConditionIndexAndRelations() throws Exception {
        Analysis analysis = analyze(DATA, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();

        assertEntry(analysis.resolution(), unit, "DUPLICATE-ITEM OF GROUP-A",
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        assertEntry(analysis.resolution(), unit, "DUPLICATE-ITEM IN GROUP-B",
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        assertEntry(analysis.resolution(), unit,
                "LEAF-ITEM OF MIDDLE-GROUP OF OUTER-GROUP",
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
        assertEntry(analysis.resolution(), unit,
                "LEAF-ITEM OF OUTER-GROUP OF MIDDLE-GROUP",
                ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0);
        assertEntry(analysis.resolution(), unit, "FLAG-ON", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEntry(analysis.resolution(), unit, "TABLE-IDX", ResolutionContracts.ReferenceRole.OCCURS_INDEX,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);

        DeclarationRelationResolution relations = analysis.resolution().declarationRelations();
        assertTrue(relations.entries().stream().filter(entry -> entry.kind() == SymbolTable.RelationKind.REDEFINES)
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED));
        assertTrue(relations.entries().stream().filter(entry -> entry.kind() == SymbolTable.RelationKind.OCCURS_DEPENDING_ON
                        || entry.kind() == SymbolTable.RelationKind.OCCURS_INDEX)
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.RESOLVED));
        assertTrue(relations.entries().stream().filter(entry -> entry.kind() == SymbolTable.RelationKind.RENAMES_FROM
                        || entry.kind() == SymbolTable.RelationKind.RENAMES_THROUGH)
                .allMatch(entry -> entry.status() == ResolutionContracts.ResolutionStatus.UNRESOLVED
                        && entry.reason() == ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT));
    }

    @Test
    void resolvesRedefinesWithinItsStructuralLevelAndRejectsInvalidHierarchyTargets() throws Exception {
        Analysis analysis = analyze(REDEFINES_STRUCTURAL, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();
        SymbolTable table = analysis.tables().forProgramUnit(unit).orElseThrow().symbolTable();
        SymbolTable.Symbol groupAX = symbolUnder(table, "X", "GROUP-A");
        SymbolTable.Symbol groupBX = symbolUnder(table, "X", "GROUP-B");
        SymbolTable.Symbol y = symbolUnder(table, "Y", "GROUP-A");
        SymbolTable.Symbol deepX = symbolUnder(table, "DEEP-X", "SUBGROUP-C");
        SymbolTable.Symbol badY = symbolUnder(table, "BAD-Y", "GROUP-C");

        assertAll("adversarial REDEFINES declarations",
                () -> assertNotEquals(groupAX.id(), groupBX.id()),
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM, groupAX.kind()),
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM, groupBX.kind()),
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM, y.kind()),
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM, deepX.kind()),
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM, badY.kind()));

        DeclarationRelationResolution.Entry valid = relationForOwner(
                analysis, table, unit, y.id(), SymbolTable.RelationKind.REDEFINES);
        DeclarationRelationResolution.Entry invalid = relationForOwner(
                analysis, table, unit, badY.id(), SymbolTable.RelationKind.REDEFINES);

        assertAll("REDEFINES selection is constrained by the declaring item's structural level",
                () -> assertRelationCandidate(valid, groupAX.id()),
                () -> assertAll("different-level target is rejected instead of nominally bound",
                        () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED,
                                invalid.status(), invalid.toString()),
                        () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                                invalid.reason(), invalid.toString()),
                        () -> assertEquals(0, invalid.candidates().size(), invalid.toString())));
    }

    @Test
    void usesStructuralSiblingScopeInsteadOfTextualLevelNumberForRedefines() throws Exception {
        Analysis analysis = analyze(REDEFINES_DIFFERENT_LEVEL, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();
        SymbolTable table = analysis.tables().forProgramUnit(unit).orElseThrow().symbolTable();
        SymbolTable.Symbol target = symbolUnder(table, "ITEM-A", "GROUP-A");
        SymbolTable.Symbol owner = symbolUnder(table, "ITEM-B", "GROUP-A");
        SymbolTable.Symbol other = symbolUnder(table, "OTHER-X", "GROUP-B");
        SymbolTable.Symbol badOwner = symbolUnder(table, "BAD-X", "GROUP-C");

        assertAll("written levels differ while the valid declarations are structural siblings",
                () -> assertEquals("05", target.attributes().get("level")),
                () -> assertEquals("04", owner.attributes().get("level")),
                () -> assertEquals(owner.scopeId(), target.scopeId()),
                () -> assertEquals(badOwner.attributes().get("level"), other.attributes().get("level")),
                () -> assertNotEquals(badOwner.scopeId(), other.scopeId()));

        DeclarationRelationResolution.Entry valid = relationForOwner(
                analysis, table, unit, owner.id(), SymbolTable.RelationKind.REDEFINES);
        DeclarationRelationResolution.Entry invalid = relationForOwner(
                analysis, table, unit, badOwner.id(), SymbolTable.RelationKind.REDEFINES);
        assertAll("scope hierarchy is the source of truth for REDEFINES",
                () -> assertRelationCandidate(valid, target.id()),
                () -> assertInvalidRelation(invalid));
    }

    @Test
    void resolvesRenamesWithinItsLogicalRecordAndRejectsCrossRecordRanges() throws Exception {
        Analysis analysis = analyze(RENAMES_STRUCTURAL, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();
        SymbolTable table = analysis.tables().forProgramUnit(unit).orElseThrow().symbolTable();
        SymbolTable.Symbol recordAStart = symbolUnder(table, "START-X", "RECORD-A");
        SymbolTable.Symbol recordAEnd = symbolUnder(table, "END-X", "RECORD-A");
        SymbolTable.Symbol recordBStart = symbolUnder(table, "START-X", "RECORD-B");
        SymbolTable.Symbol recordBEnd = symbolUnder(table, "END-X", "RECORD-B");
        SymbolTable.Symbol rangeA = symbolUnder(table, "RANGE-A", "RECORD-A");
        SymbolTable.Symbol crossStart = symbolUnder(table, "CROSS-START", "RECORD-C");
        SymbolTable.Symbol crossEnd = symbolUnder(table, "CROSS-END", "RECORD-D");
        SymbolTable.Symbol crossRange = symbolUnder(table, "CROSS-RANGE", "RECORD-D");

        assertAll("adversarial RENAMES declarations and logical-record ownership",
                () -> assertNotEquals(recordAStart.id(), recordBStart.id()),
                () -> assertNotEquals(recordAEnd.id(), recordBEnd.id()),
                () -> assertEquals(SymbolTable.SymbolKind.RENAMES, rangeA.kind()),
                () -> assertEquals(SymbolTable.SymbolKind.RENAMES, crossRange.kind()),
                () -> assertNotEquals(table.scopes().get(crossStart.scopeId()).name(),
                        table.scopes().get(crossEnd.scopeId()).name()));

        DeclarationRelationResolution.Entry validFrom = relationForOwner(
                analysis, table, unit, rangeA.id(), SymbolTable.RelationKind.RENAMES_FROM);
        DeclarationRelationResolution.Entry validThrough = relationForOwner(
                analysis, table, unit, rangeA.id(), SymbolTable.RelationKind.RENAMES_THROUGH);
        DeclarationRelationResolution.Entry invalidFrom = relationForOwner(
                analysis, table, unit, crossRange.id(), SymbolTable.RelationKind.RENAMES_FROM);
        DeclarationRelationResolution.Entry invalidThrough = relationForOwner(
                analysis, table, unit, crossRange.id(), SymbolTable.RelationKind.RENAMES_THROUGH);

        assertAll("RENAMES endpoints are selected and validated within the owner logical record",
                () -> assertRelationCandidate(validFrom, recordAStart.id()),
                () -> assertRelationCandidate(validThrough, recordAEnd.id()),
                () -> assertInvalidRelation(invalidFrom),
                () -> assertInvalidRelation(invalidThrough));
    }

    @Test
    void appliesQualifyModeWithoutGuessingAnUnspecifiedCompilerOption() throws Exception {
        Analysis standard = analyze(DATA, ResolutionContracts.QualifyMode.STANDARD);
        Analysis extend = analyze(DATA, ResolutionContracts.QualifyMode.EXTEND);
        Analysis unspecified = analyze(DATA, ResolutionContracts.QualifyMode.UNSPECIFIED);
        ResolutionContracts.ProgramUnitId unit = standard.model().programUnits().get(0).id();
        String text = "LEAF-ITEM OF MIDDLE-GROUP OF OUTER-GROUP";

        assertEntry(standard.resolution(), unit, text, ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.AMBIGUOUS,
                ResolutionContracts.ResolutionReason.MULTIPLE_VALID_CANDIDATES, 2);
        assertEntry(extend.resolution(), unit, text, ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        assertEntry(unspecified.resolution(), unit, text, ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                ResolutionContracts.ResolutionReason.UNSUPPORTED_DIALECT_OPTION, 2);
    }

    @Test
    void respectsNestedShadowingAndOnlyImportsTypedGlobalData() throws Exception {
        Analysis analysis = analyze(NESTED, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId child = analysis.model().programUnits().stream()
                .filter(unit -> unit.program().name().equals("DATA-CHILD")).findFirst().orElseThrow().id();

        ReferenceResolution.Entry shadow = assertEntry(analysis.resolution(), child, "OUTER-LOCAL",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals(child, shadow.candidates().get(0).entityId().programUnitId());
        ReferenceResolution.Entry global = assertEntry(analysis.resolution(), child, "GLOBAL-DATA",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertNotEquals(child, global.candidates().get(0).entityId().programUnitId());
        ReferenceResolution.Entry external = assertEntry(analysis.resolution(), child, "EXTERNAL-DATA",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals("EXTERNAL", external.candidates().get(0).attributes().get("visibility"));
        assertEntry(analysis.resolution(), child, "NOT-VISIBLE", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.UNRESOLVED,
                ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0);
    }

    @Test
    void skipsInvisibleLocalDataInIntermediateProgramsWhenLookingForGlobalData() throws Exception {
        Analysis analysis = analyze(GLOBAL_THROUGH_LOCAL, ResolutionContracts.QualifyMode.STANDARD);
        CompilationUnitModel.ProgramUnit outerA = program(analysis, "OUTER-A");
        CompilationUnitModel.ProgramUnit middleA = program(analysis, "MIDDLE-A");
        CompilationUnitModel.ProgramUnit innerA = program(analysis, "INNER-A");
        CompilationUnitModel.ProgramUnit middleB = program(analysis, "MIDDLE-B");
        CompilationUnitModel.ProgramUnit innerB = program(analysis, "INNER-B");
        CompilationUnitModel.ProgramUnit innerC = program(analysis, "INNER-C");
        CompilationUnitModel.ProgramUnit innerD = program(analysis, "INNER-D");
        SymbolTable outerTable = analysis.tables().forProgramUnit(outerA.id()).orElseThrow().symbolTable();
        SymbolTable middleTable = analysis.tables().forProgramUnit(middleA.id()).orElseThrow().symbolTable();

        assertAll("adversarial visibility topology",
                () -> assertEquals("GLOBAL", symbol(outerTable, "X").attributes().get("visibility")),
                () -> assertEquals("LOCAL", symbol(middleTable, "X").attributes().get("visibility")),
                () -> assertTrue(analysis.tables().forProgramUnit(innerA.id()).orElseThrow().symbolTable()
                        .lookupAll(SymbolTable.Namespace.DATA, "X").isEmpty()));

        ReferenceResolution.Entry throughLocal = assertEntry(analysis.resolution(), innerA.id(), "X",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        ReferenceResolution.Entry nearestGlobal = assertEntry(analysis.resolution(), innerB.id(), "X",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        ReferenceResolution.Entry local = assertEntry(analysis.resolution(), innerC.id(), "X",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);

        assertAll("nominal nested DATA selection",
                () -> assertEquals(outerA.id(), throughLocal.candidates().get(0).entityId().programUnitId()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA, throughLocal.candidates().get(0).kind()),
                () -> assertNotEquals(middleA.id(), throughLocal.candidates().get(0).entityId().programUnitId()),
                () -> assertEquals(middleB.id(), nearestGlobal.candidates().get(0).entityId().programUnitId()),
                () -> assertEquals(innerC.id(), local.candidates().get(0).entityId().programUnitId()),
                () -> assertEntry(analysis.resolution(), innerD.id(), "X",
                        ResolutionContracts.ReferenceRole.VALUE_READ,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0));
    }

    @Test
    void inheritsGlobalGroupVisibilityForDataConditionAndIndexSubordinates() throws Exception {
        Analysis analysis = analyze(GLOBAL_SUBORDINATES, ResolutionContracts.QualifyMode.STANDARD);
        CompilationUnitModel.ProgramUnit outer = analysis.model().programUnits().stream()
                .filter(unit -> unit.program().name().equals("GLOBAL-OUTER")).findFirst().orElseThrow();
        CompilationUnitModel.ProgramUnit inner = analysis.model().programUnits().stream()
                .filter(unit -> unit.program().name().equals("GLOBAL-INNER")).findFirst().orElseThrow();
        SymbolTable outerTable = analysis.tables().forProgramUnit(outer.id()).orElseThrow().symbolTable();
        SymbolTable innerTable = analysis.tables().forProgramUnit(inner.id()).orElseThrow().symbolTable();

        assertEquals(SymbolTable.SymbolKind.DATA_ITEM, symbol(outerTable, "GLOBAL-CHILD").kind());
        assertEquals(SymbolTable.SymbolKind.CONDITION_NAME, symbol(outerTable, "STATUS-OK").kind());
        assertEquals(SymbolTable.SymbolKind.INDEX_NAME, symbol(outerTable, "GLOBAL-IDX").kind());
        assertTrue(innerTable.lookupAll(SymbolTable.Namespace.DATA, "GLOBAL-CHILD").isEmpty(),
                "binding must not invent a local DATA declaration");
        assertTrue(innerTable.lookupAll(SymbolTable.Namespace.DATA, "STATUS-OK").isEmpty(),
                "binding must not invent a local CONDITION declaration");
        assertTrue(innerTable.lookupAll(SymbolTable.Namespace.DATA, "GLOBAL-IDX").isEmpty(),
                "binding must not invent a local INDEX declaration");

        assertAll("GLOBAL visibility inherited by subordinate declarations",
                () -> assertInheritedCandidate(analysis.resolution(), inner.id(), outer.id(),
                        "GLOBAL-CHILD", ResolutionContracts.ReferenceRole.VALUE_READ,
                        ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, "DATA_ITEM"),
                () -> assertInheritedCandidate(analysis.resolution(), inner.id(), outer.id(),
                        "STATUS-OK", ResolutionContracts.ReferenceRole.VALUE_READ,
                        ResolutionContracts.ReferenceKind.CONDITION,
                        ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, "CONDITION_NAME"),
                () -> assertInheritedCandidate(analysis.resolution(), inner.id(), outer.id(),
                        "GLOBAL-IDX", ResolutionContracts.ReferenceRole.SUBSCRIPT,
                        ResolutionContracts.ReferenceKind.INDEX,
                        ResolutionContracts.SemanticEntityDomain.INDEX_SYMBOL, "INDEX_NAME"));
    }

    @Test
    void propagatesGlobalFileDescriptionVisibilityToItsRecordHierarchy() throws Exception {
        Analysis analysis = analyze(GLOBAL_FD_RECORDS, ResolutionContracts.QualifyMode.STANDARD);
        CompilationUnitModel.ProgramUnit outer = program(analysis, "GLOBAL-FD-OUTER");
        CompilationUnitModel.ProgramUnit inner = program(analysis, "GLOBAL-FD-INNER");
        CompilationUnitModel.ProgramUnit localInner = program(analysis, "LOCAL-FD-INNER");
        SymbolTable outerTable = analysis.tables().forProgramUnit(outer.id()).orElseThrow().symbolTable();
        SymbolTable innerTable = analysis.tables().forProgramUnit(inner.id()).orElseThrow().symbolTable();
        SymbolTable.Entity file = outerTable.entities().stream()
                .filter(entity -> entity.canonicalName().equals("CUSTOMER-FILE"))
                .findFirst().orElseThrow();

        assertAll("FD GLOBAL effective visibility reaches the complete record hierarchy",
                () -> assertEquals("GLOBAL", file.attributes().get("visibility")),
                () -> assertEquals("GLOBAL", symbol(outerTable, "CUSTOMER-RECORD")
                        .attributes().get("visibility")),
                () -> assertEquals("GLOBAL", symbol(outerTable, "CUSTOMER-ID")
                        .attributes().get("visibility")),
                () -> assertEquals("GLOBAL", symbol(outerTable, "CUSTOMER-OK")
                        .attributes().get("visibility")),
                () -> assertEquals("GLOBAL", symbol(outerTable, "CUSTOMER-IDX")
                        .attributes().get("visibility")),
                () -> assertTrue(innerTable.lookupAll(SymbolTable.Namespace.DATA, "CUSTOMER-ID").isEmpty()));

        assertAll("contained program binds inherited FD record declarations to the outer unit",
                () -> assertInheritedCandidate(analysis.resolution(), inner.id(), outer.id(),
                        "CUSTOMER-RECORD", ResolutionContracts.ReferenceRole.VALUE_READ,
                        ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, "DATA_ITEM"),
                () -> assertInheritedCandidate(analysis.resolution(), inner.id(), outer.id(),
                        "CUSTOMER-ID", ResolutionContracts.ReferenceRole.VALUE_READ,
                        ResolutionContracts.ReferenceKind.DATA,
                        ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, "DATA_ITEM"),
                () -> assertInheritedCandidate(analysis.resolution(), inner.id(), outer.id(),
                        "CUSTOMER-OK", ResolutionContracts.ReferenceRole.VALUE_READ,
                        ResolutionContracts.ReferenceKind.CONDITION,
                        ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, "CONDITION_NAME"),
                () -> assertInheritedCandidate(analysis.resolution(), inner.id(), outer.id(),
                        "CUSTOMER-IDX", ResolutionContracts.ReferenceRole.SUBSCRIPT,
                        ResolutionContracts.ReferenceKind.INDEX,
                        ResolutionContracts.SemanticEntityDomain.INDEX_SYMBOL, "INDEX_NAME"),
                () -> assertEntry(analysis.resolution(), localInner.id(), "LOCAL-ID",
                        ResolutionContracts.ReferenceRole.VALUE_READ,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionReason.DECLARATION_NOT_FOUND, 0));
    }

    @Test
    void stopsAtIncompatibleLocalNamesBeforeFilteringByReferenceKind() throws Exception {
        Analysis analysis = analyze(NAMESPACE_SHADOWING, ResolutionContracts.QualifyMode.STANDARD);
        CompilationUnitModel.ProgramUnit outer = analysis.model().programUnits().stream()
                .filter(unit -> unit.program().name().equals("SHADOW-OUTER")).findFirst().orElseThrow();
        CompilationUnitModel.ProgramUnit inner = analysis.model().programUnits().stream()
                .filter(unit -> unit.program().name().equals("SHADOW-INNER")).findFirst().orElseThrow();
        SymbolTable outerTable = analysis.tables().forProgramUnit(outer.id()).orElseThrow().symbolTable();
        SymbolTable innerTable = analysis.tables().forProgramUnit(inner.id()).orElseThrow().symbolTable();

        assertAll("fixture declaration categories",
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM,
                        symbol(outerTable, "COLLIDE-CONDITION").kind()),
                () -> assertEquals(SymbolTable.SymbolKind.CONDITION_NAME,
                        symbol(innerTable, "COLLIDE-CONDITION").kind()),
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM,
                        symbol(outerTable, "COLLIDE-INDEX").kind()),
                () -> assertEquals(SymbolTable.SymbolKind.INDEX_NAME,
                        symbol(innerTable, "COLLIDE-INDEX").kind()));

        ReferenceResolution.Entry control = assertEntry(analysis.resolution(), inner.id(), "CONTROL-GLOBAL",
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        assertEquals(outer.id(), control.candidates().get(0).entityId().programUnitId());

        assertAll("an incompatible local declaration shadows an otherwise compatible outer GLOBAL",
                () -> assertEntry(analysis.resolution(), inner.id(), "COLLIDE-CONDITION",
                        ResolutionContracts.ReferenceRole.VALUE_READ,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, 0),
                () -> assertEntry(analysis.resolution(), inner.id(), "COLLIDE-INDEX",
                        ResolutionContracts.ReferenceRole.VALUE_READ,
                        ResolutionContracts.ResolutionStatus.UNRESOLVED,
                        ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT, 0));
    }

    @Test
    void appliesQualificationAcrossLocalAndInheritedGlobalCandidatesBeforeSelectingAProgramUnit() throws Exception {
        Analysis analysis = analyze(QUALIFIED_GLOBAL, ResolutionContracts.QualifyMode.STANDARD);
        CompilationUnitModel.ProgramUnit outer = analysis.model().programUnits().stream()
                .filter(unit -> unit.program().name().equals("QUALIFY-OUTER")).findFirst().orElseThrow();
        CompilationUnitModel.ProgramUnit inner = analysis.model().programUnits().stream()
                .filter(unit -> unit.program().name().equals("QUALIFY-INNER")).findFirst().orElseThrow();
        SymbolTable outerTable = analysis.tables().forProgramUnit(outer.id()).orElseThrow().symbolTable();
        SymbolTable innerTable = analysis.tables().forProgramUnit(inner.id()).orElseThrow().symbolTable();

        assertAll("same base name exists in both ProgramUnits before candidate selection",
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM, symbol(outerTable, "VALUE-X").kind()),
                () -> assertEquals(SymbolTable.SymbolKind.DATA_ITEM, symbol(innerTable, "VALUE-X").kind()),
                () -> assertEquals("GLOBAL", symbol(outerTable, "OUTER-GROUP").attributes().get("visibility")),
                () -> assertEquals("LOCAL", symbol(innerTable, "INNER-GROUP").attributes().get("visibility")));

        assertAll("unqualified, locally qualified and externally qualified references are distinct",
                () -> assertResolvedInUnit(analysis.resolution(), inner.id(), "VALUE-X",
                        ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, inner.id()),
                () -> assertResolvedInUnit(analysis.resolution(), inner.id(), "VALUE-X OF INNER-GROUP",
                        ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, inner.id()),
                () -> assertResolvedInUnit(analysis.resolution(), inner.id(), "VALUE-X OF OUTER-GROUP",
                        ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, outer.id()));
    }

    @Test
    void resolvesADataItemQualifiedThroughItsFileHierarchy() throws Exception {
        Analysis analysis = analyze(Path.of("src/test/resources/cobol/resolution/entities-and-occurrences.cbl"),
                ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();
        ReferenceResolution.Entry entry = assertEntry(analysis.resolution(), unit,
                "FILE-VALUE OF BOTH-RECORD IN BOTH-FILE", ResolutionContracts.ReferenceRole.VALUE_READ,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.QUALIFIED_HIERARCHY_MATCH, 1);
        assertEquals("FILE-VALUE", entry.candidates().get(0).canonicalName());
    }

    @Test
    void keepsNamespacesOutsideThisPhaseExplicitlyUnsupported() throws Exception {
        Analysis analysis = analyze(Path.of("src/test/resources/cobol/resolution/baseline-compilation-unit.cbl"),
                ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();
        assertEntry(analysis.resolution(), unit, "'EXTERNAL-PROGRAM'",
                ResolutionContracts.ReferenceRole.CALL_TARGET,
                ResolutionContracts.ResolutionStatus.UNSUPPORTED,
                ResolutionContracts.ResolutionReason.UNSUPPORTED_GRAMMAR_FORM, 0);
    }

    @Test
    void usesPrebuiltNameIndexesInsteadOfScanningAllSymbolsPerReference() throws Exception {
        Analysis analysis = analyze(DATA, ResolutionContracts.QualifyMode.STANDARD);
        assertTrue(analysis.resolution().metrics().nominalLookups() > 0);
        assertTrue(analysis.resolution().metrics().candidateInspections()
                < analysis.resolution().metrics().nominalLookups() * 5,
                "inspection must be proportional to same-name candidates, not all symbols");
        assertTrue(analysis.resolution().metrics().indexedDeclarations()
                >= analysis.tables().units().get(0).symbolTable().symbols().size());
    }

    @Test
    void exposesTheResolvedSemanticKindForPolymorphicSubscripts() throws Exception {
        Analysis analysis = analyze(SUBSCRIPT_SEMANTIC_KIND, ResolutionContracts.QualifyMode.STANDARD);
        ResolutionContracts.ProgramUnitId unit = analysis.model().programUnits().get(0).id();
        ReferenceResolution.Entry dataSubscript = assertEntry(analysis.resolution(), unit,
                "SUBSCRIPT-NUM", ResolutionContracts.ReferenceRole.SUBSCRIPT,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);
        ReferenceResolution.Entry indexSubscript = assertEntry(analysis.resolution(), unit,
                "TABLE-IDX", ResolutionContracts.ReferenceRole.SUBSCRIPT,
                ResolutionContracts.ResolutionStatus.RESOLVED,
                ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, 1);

        assertAll("occurrence kind is a syntactic hint and admissibleKinds preserves polymorphism",
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        dataSubscript.occurrence().kind()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        dataSubscript.occurrence().admissibleKinds()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        indexSubscript.occurrence().kind()),
                () -> assertEquals(EnumSet.of(ResolutionContracts.ReferenceKind.DATA,
                                ResolutionContracts.ReferenceKind.INDEX),
                        indexSubscript.occurrence().admissibleKinds()));
        assertAll("selected candidate kind is the final semantic category",
                () -> assertEquals(ResolutionContracts.ReferenceKind.DATA,
                        dataSubscript.selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                        dataSubscript.selectedCandidate().orElseThrow().entityId().domain()),
                () -> assertEquals(ResolutionContracts.ReferenceKind.INDEX,
                        indexSubscript.selectedCandidate().orElseThrow().kind()),
                () -> assertEquals(ResolutionContracts.SemanticEntityDomain.INDEX_SYMBOL,
                        indexSubscript.selectedCandidate().orElseThrow().entityId().domain()));
    }

    private static ReferenceResolution.Entry assertEntry(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId unit,
            String writtenText, ResolutionContracts.ReferenceRole role,
            ResolutionContracts.ResolutionStatus status, ResolutionContracts.ResolutionReason reason,
            int candidates) {
        ReferenceResolution.Entry entry = resolution.find(unit, writtenText, role).stream()
                .findFirst().orElseThrow(() -> new AssertionError("missing resolution entry " + role + " " + writtenText));
        assertEquals(status, entry.status(), entry.toString());
        assertEquals(reason, entry.reason(), entry.toString());
        assertEquals(candidates, entry.candidates().size(), entry.toString());
        return entry;
    }

    private static void assertInheritedCandidate(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId useUnit,
            ResolutionContracts.ProgramUnitId declarationUnit, String writtenText,
            ResolutionContracts.ReferenceRole role, ResolutionContracts.ReferenceKind kind,
            ResolutionContracts.SemanticEntityDomain domain, String symbolKind) {
        ReferenceResolution.Entry entry = resolution.find(useUnit, writtenText, role).stream()
                .findFirst().orElseThrow(() -> new AssertionError("missing resolution entry " + role + " " + writtenText));
        assertAll(writtenText,
                () -> assertEquals(kind, entry.occurrence().kind(), entry.toString()),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status(), entry.toString()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION,
                        entry.reason(), entry.toString()),
                () -> assertEquals(1, entry.candidates().size(), entry.toString()),
                () -> {
                    assertFalse(entry.candidates().isEmpty(), entry.toString());
                    ReferenceResolution.Candidate candidate = entry.candidates().get(0);
                    assertEquals(kind, candidate.kind());
                    assertEquals(declarationUnit, candidate.entityId().programUnitId());
                    assertEquals(domain, candidate.entityId().domain());
                    assertEquals(symbolKind, candidate.attributes().get("symbolKind"));
                });
    }

    private static void assertResolvedInUnit(
            ReferenceResolution resolution, ResolutionContracts.ProgramUnitId useUnit,
            String writtenText, ResolutionContracts.ResolutionReason reason,
            ResolutionContracts.ProgramUnitId expectedDeclarationUnit) {
        ReferenceResolution.Entry entry = assertEntry(resolution, useUnit, writtenText,
                ResolutionContracts.ReferenceRole.VALUE_READ, ResolutionContracts.ResolutionStatus.RESOLVED,
                reason, 1);
        ReferenceResolution.Candidate candidate = entry.candidates().get(0);
        assertEquals(ResolutionContracts.ReferenceKind.DATA, candidate.kind());
        assertEquals(ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL, candidate.entityId().domain());
        assertEquals(expectedDeclarationUnit, candidate.entityId().programUnitId());
    }

    private static SymbolTable.Symbol symbol(SymbolTable table, String name) {
        return table.lookupAll(SymbolTable.Namespace.DATA, name).stream().findFirst()
                .orElseThrow(() -> new AssertionError("missing declaration " + name));
    }

    private static CompilationUnitModel.ProgramUnit program(Analysis analysis, String name) {
        return analysis.model().programUnits().stream()
                .filter(unit -> unit.program().name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("missing program " + name));
    }

    private static SymbolTable.Symbol symbolUnder(SymbolTable table, String name, String owner) {
        return table.lookupAll(SymbolTable.Namespace.DATA, name).stream()
                .filter(symbol -> table.scopes().get(symbol.scopeId()).name().equals(owner))
                .findFirst().orElseThrow(() -> new AssertionError("missing declaration " + owner + "." + name));
    }

    private static DeclarationRelationResolution.Entry relationForOwner(
            Analysis analysis, SymbolTable table, ResolutionContracts.ProgramUnitId unit,
            int ownerSymbolId, SymbolTable.RelationKind kind) {
        SymbolTable.DeclarationRelation declaration = table.declarationRelations().stream()
                .filter(relation -> relation.kind() == kind && relation.ownerSymbolId() == ownerSymbolId)
                .findFirst().orElseThrow(() -> new AssertionError("missing " + kind + " relation for " + ownerSymbolId));
        return analysis.resolution().declarationRelations().entries().stream()
                .filter(entry -> entry.programUnitId().equals(unit))
                .filter(entry -> entry.relationId() == declaration.id())
                .findFirst().orElseThrow(() -> new AssertionError("missing resolution for relation " + declaration.id()));
    }

    private static void assertRelationCandidate(DeclarationRelationResolution.Entry entry, int expectedSymbolId) {
        assertAll(entry.toString(),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.RESOLVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.UNIQUE_VISIBLE_DECLARATION, entry.reason()),
                () -> assertEquals(1, entry.candidates().size()),
                () -> {
                    assertFalse(entry.candidates().isEmpty());
                    ReferenceResolution.Candidate candidate = entry.candidates().get(0);
                    assertEquals(ResolutionContracts.SemanticEntityDomain.DATA_SYMBOL,
                            candidate.entityId().domain());
                    assertEquals(expectedSymbolId, candidate.entityId().localId());
                });
    }

    private static void assertInvalidRelation(DeclarationRelationResolution.Entry entry) {
        assertAll(entry.toString(),
                () -> assertEquals(ResolutionContracts.ResolutionStatus.UNRESOLVED, entry.status()),
                () -> assertEquals(ResolutionContracts.ResolutionReason.INVALID_NAMESPACE_FOR_CONTEXT,
                        entry.reason()),
                () -> assertEquals(0, entry.candidates().size()));
    }

    private static Analysis analyze(Path sourcePath, ResolutionContracts.QualifyMode mode) throws Exception {
        Path file = sourcePath.toAbsolutePath();
        String source = SourceNormalizer.fixed(Files.readString(file, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.proleap();
        Parser parser = binding.cobolParser(new CommonTokenStream(
                binding.cobolLexer(CharStreams.fromString(source, file.getFileName().toString()))));
        ParseTree tree = binding.cobolStart(parser);
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        CompilationUnitBuildResult build = new AstBuilder(parser, source,
                SourceMap.identity(source, file.getFileName().toString()), ids, sizes)
                .buildCompilationUnit(tree, file.getFileName().toString());
        CompilationUnitModel model = build.compilationUnit();
        CompilationUnitSymbolTables tables = new CompilationUnitSymbolTableBuilder().build(model);
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : model.programUnits()) {
            SymbolTable table = tables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector().collect(unit.id(), unit.program(),
                    AstScopeIndex.build(unit.program(), table)));
        }
        ResolutionContracts.CobolResolutionPolicy policy = new ResolutionContracts.CobolResolutionPolicy(
                "test-policy", "1", mode);
        ReferenceResolution resolution = new DataAndIndexReferenceResolver(policy)
                .resolve(model, tables, occurrences);
        return new Analysis(model, tables, resolution);
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int i = 0; i < tree.getChildCount(); i++) size += index(tree.getChild(i), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private record Analysis(CompilationUnitModel model, CompilationUnitSymbolTables tables,
                            ReferenceResolution resolution) { }
}
