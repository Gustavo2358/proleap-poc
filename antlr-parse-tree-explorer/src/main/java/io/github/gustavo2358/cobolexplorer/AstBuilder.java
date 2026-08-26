package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/** Builds one immutable semantic AST from the ANTLR parse tree. */
final class AstBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(AstBuilder.class);
    private static final Set<String> MODELED_GENERIC_STATEMENTS = Set.of(
            "acceptStatement", "addStatement", "closeStatement", "computeStatement",
            "continueStatement", "deleteStatement", "divideStatement", "exitStatement",
            "gobackStatement", "initializeStatement", "inspectStatement", "multiplyStatement",
            "openStatement", "readStatement", "releaseStatement", "returnStatement",
            "rewriteStatement", "setStatement", "startStatement", "stopStatement",
            "stringStatement", "subtractStatement", "unstringStatement", "writeStatement");
    private static final Set<String> PRESERVED_STATEMENTS = Set.of(
            "alterStatement", "cancelStatement", "disableStatement", "displayStatement",
            "enableStatement", "entryStatement", "exhibitStatement", "generateStatement",
            "initiateStatement", "mergeStatement", "purgeStatement", "receiveStatement",
            "searchStatement", "sendStatement", "sortStatement", "terminateStatement");
    private static final Set<String> OPERAND_RULES = Set.of(
            "identifier", "qualifiedDataName", "procedureName", "fileName", "indexName",
            "literal", "integerLiteral", "numericLiteral", "dataName", "recordName",
            "reportName", "cdName", "libraryName", "mnemonicName", "environmentName", "alphabetName");
    private static final Set<String> FLOW_CLAUSE_RULES = Set.of(
            "onExceptionClause", "notOnExceptionClause", "onOverflowPhrase", "notOnOverflowPhrase",
            "onSizeErrorPhrase", "notOnSizeErrorPhrase", "invalidKeyPhrase", "notInvalidKeyPhrase",
            "atEndPhrase", "notAtEndPhrase", "writeAtEndOfPagePhrase", "writeNotAtEndOfPagePhrase",
            "receiveNoData", "receiveWithData", "searchWhen");
    private final Parser parser;
    private final String source;
    private final SourceMap sourceMap;
    private final IdentityHashMap<ParseTree, Integer> parseIds;
    private final IdentityHashMap<ParseTree, Integer> parseSubtreeSizes;
    private final List<CoverageDraft> coverageDrafts = new ArrayList<>();
    private final List<SemanticCoverage.Diagnostic> semanticDiagnostics = new ArrayList<>();
    private int nextId;

    private record CoverageDraft(String grammarRule, Ast.Meta meta, String writtenText,
                                 int astNodeId) { }
    private record LogMetrics(int nodes, int unsupportedStatements, int preservedStatements) { }

    AstBuilder(Parser parser, String source, SourceMap sourceMap,
               IdentityHashMap<ParseTree, Integer> parseIds,
               IdentityHashMap<ParseTree, Integer> parseSubtreeSizes) {
        this.parser = parser;
        this.source = source;
        this.sourceMap = sourceMap;
        this.parseIds = parseIds;
        this.parseSubtreeSizes = parseSubtreeSizes;
    }

    AstBuildResult build(ParseTree tree) {
        ParserRuleContext unit = firstDescendant(tree, "programUnit");
        if (unit == null) throw new IllegalStateException("programUnit not found");
        return buildProgramUnit(unit);
    }

    CompilationUnitBuildResult buildCompilationUnit(ParseTree tree, String writtenCompilationUnitId) {
        String compilationUnitId = SymbolTable.canonical(writtenCompilationUnitId);
        ParserRuleContext compilation = firstDescendant(tree, "compilationUnit");
        if (compilation == null) throw new IllegalStateException("compilationUnit not found");
        List<CompilationUnitModel.ProgramUnit> units = new ArrayList<>();
        Map<ResolutionContracts.ProgramUnitId, SemanticCoverage.Report> coverage = new LinkedHashMap<>();
        Map<ResolutionContracts.ProgramUnitId, List<SemanticCoverage.Diagnostic>> diagnostics = new LinkedHashMap<>();
        List<ParserRuleContext> topLevel = directChildrenNamed(compilation, "programUnit");
        for (int index = 0; index < topLevel.size(); index++) {
            collectProgramUnits(topLevel.get(index), List.of(index), null, compilationUnitId,
                    units, coverage, diagnostics);
        }
        CompilationUnitModel model = new CompilationUnitModel(compilationUnitId, units);
        return new CompilationUnitBuildResult(model, coverage, diagnostics);
    }

    private void collectProgramUnits(ParserRuleContext context, List<Integer> structuralPath,
                                     ResolutionContracts.ProgramUnitId parentId, String compilationUnitId,
                                     List<CompilationUnitModel.ProgramUnit> units,
                                     Map<ResolutionContracts.ProgramUnitId, SemanticCoverage.Report> coverage,
                                     Map<ResolutionContracts.ProgramUnitId, List<SemanticCoverage.Diagnostic>> diagnostics) {
        AstBuildResult built = buildProgramUnit(context);
        ResolutionContracts.ProgramUnitId id = new ResolutionContracts.ProgramUnitId(
                compilationUnitId, structuralPath,
                SymbolTable.canonical(unquote(built.program().name())));
        units.add(new CompilationUnitModel.ProgramUnit(id, parentId, built.program()));
        coverage.put(id, built.coverage());
        diagnostics.put(id, built.diagnostics());
        List<ParserRuleContext> nested = directChildrenNamed(context, "programUnit");
        for (int index = 0; index < nested.size(); index++) {
            List<Integer> childPath = new ArrayList<>(structuralPath);
            childPath.add(index);
            collectProgramUnits(nested.get(index), childPath, id, compilationUnitId,
                    units, coverage, diagnostics);
        }
    }

    private AstBuildResult buildProgramUnit(ParserRuleContext unit) {
        long started = System.nanoTime();
        nextId = 0;
        coverageDrafts.clear();
        semanticDiagnostics.clear();
        Ast.Meta meta = meta(unit);
        ParserRuleContext programId = firstDescendant(unit, "programIdParagraph");
        ParserRuleContext programName = firstDescendant(programId, "programName");
        List<Ast.Division> divisions = new ArrayList<>();
        for (ParserRuleContext child : directRuleChildren(unit)) {
            switch (rule(child)) {
                case "identificationDivision" -> divisions.add(buildIdentification(child));
                case "environmentDivision" -> divisions.add(buildEnvironment(child));
                case "dataDivision" -> divisions.add(buildData(child));
                case "procedureDivision" -> divisions.add(buildProcedure(child));
                default -> { }
            }
        }
        Ast.Program program = new Ast.Program(meta,
                programName == null ? "<anonymous>" : clean(sourceText(programName)),
                programAttributes(programId), divisions);
        AstBuildResult result = new AstBuildResult(program, buildCoverageReport(), semanticDiagnostics);
        if (LOG.isDebugEnabled()) {
            LogMetrics metrics = logMetrics(program);
            LOG.debug("event=ast_built scope=PROGRAM_UNIT source={} programUnit={} phase=AST_BUILD elapsedMs={} nodes={} semanticDiagnostics={} unsupportedStatements={} preservedStatements={}",
                    program.meta().provenance().original().file(), program.name(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), metrics.nodes(),
                    result.diagnostics().size(), metrics.unsupportedStatements(), metrics.preservedStatements());
        }
        return result;
    }

    private static LogMetrics logMetrics(Ast.Node root) {
        int nodes = 0;
        int unsupported = 0;
        int preserved = 0;
        Deque<Ast.Node> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            Ast.Node node = pending.pop();
            nodes++;
            if (node instanceof Ast.UnsupportedStatement) unsupported++;
            if (node instanceof Ast.PreservedStatement) preserved++;
            for (Ast.Node child : Ast.children(node)) pending.push(child);
        }
        return new LogMetrics(nodes, unsupported, preserved);
    }

    private Ast.ProgramAttributes programAttributes(ParserRuleContext programId) {
        if (programId == null) return Ast.ProgramAttributes.none();
        return new Ast.ProgramAttributes(containsToken(programId, "COMMON"),
                containsToken(programId, "INITIAL"), containsToken(programId, "RECURSIVE"),
                containsToken(programId, "LIBRARY"), containsToken(programId, "DEFINITION"),
                sourceText(programId).strip());
    }

    private Ast.Division buildIdentification(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        return new Ast.Division(meta, Ast.DivisionKind.IDENTIFICATION, List.of());
    }

    private Ast.Division buildEnvironment(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Node> children = new ArrayList<>();
        for (ParserRuleContext entry : nearestDescendants(context, "fileControlEntry")) {
            Ast.Meta entryMeta = meta(entry);
            ParserRuleContext select = firstDescendant(entry, "selectClause");
            ParserRuleContext fileName = firstDescendant(select, "fileName");
            ParserRuleContext assign = firstDescendant(entry, "assignClause");
            children.add(new Ast.FileBinding(entryMeta,
                    fileName == null ? "<unknown>" : clean(sourceText(fileName)),
                    assign == null ? "" : compact(sourceText(assign))));
        }
        return new Ast.Division(meta, Ast.DivisionKind.ENVIRONMENT, children);
    }

    private Ast.Division buildData(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Node> sections = new ArrayList<>();
        for (ParserRuleContext wrapper : directChildrenNamed(context, "dataDivisionSection")) {
            ParserRuleContext sectionContext = directRuleChildren(wrapper).stream().findFirst().orElse(wrapper);
            Ast.Meta sectionMeta = meta(sectionContext);
            List<Ast.Node> entries = new ArrayList<>();
            if (rule(sectionContext).equals("fileSection")) {
                for (ParserRuleContext fd : directChildrenNamed(sectionContext, "fileDescriptionEntry")) {
                    Ast.Meta fdMeta = meta(fd);
                    ParserRuleContext fileName = firstDescendant(fd, "fileName");
                    List<Ast.DataEntry> dataEntries = new ArrayList<>();
                    dataEntries.addAll(buildDataHierarchy(directChildrenNamed(fd, "dataDescriptionEntry")));
                    entries.add(new Ast.FileDescription(fdMeta,
                            fileName == null ? "<unknown>" : clean(sourceText(fileName)),
                            declarationVisibility(fd, "externalClause", "globalClause"), dataEntries));
                }
            } else {
                entries.addAll(buildDataHierarchy(nearestDescendants(sectionContext, "dataDescriptionEntry")));
            }
            sections.add(new Ast.Section(sectionMeta, displayRule(rule(sectionContext)), dataSectionKind(rule(sectionContext)), entries));
        }
        return new Ast.Division(meta, Ast.DivisionKind.DATA, sections);
    }

    private Ast.DataEntry buildDataEntry(ParserRuleContext wrapper) {
        ParserRuleContext format = directRuleChildren(wrapper).stream().findFirst().orElse(wrapper);
        Ast.Meta meta = meta(format);
        String raw = compact(sourceText(format));
        String level = raw.matches("^\\d+.*") ? raw.replaceFirst("^(\\d+).*", "$1") : rule(format).contains("ExecSql") ? "SQL" : "?";
        ParserRuleContext name = firstDescendant(format, "dataName");
        if (name == null) name = firstDescendant(format, "conditionName");
        boolean filler = name == null;
        List<Ast.DataClause> clauses = directRuleChildren(format).stream()
                .filter(child -> rule(child).startsWith("data") && rule(child).endsWith("Clause"))
                .map(this::buildDataClause).toList();
        return new Ast.DataEntry(meta, level, levelKind(level), filler ? "FILLER" : clean(sourceText(name)),
                filler, declarationVisibility(format, "dataExternalClause", "dataGlobalClause"),
                raw, clauses, List.of());
    }

    private static final class DataDraft {
        private final Ast.DataEntry entry;
        private final List<DataDraft> children = new ArrayList<>();

        private DataDraft(Ast.DataEntry entry) {
            this.entry = entry;
        }
    }

    private List<Ast.DataEntry> buildDataHierarchy(List<ParserRuleContext> contexts) {
        List<DataDraft> roots = new ArrayList<>();
        Deque<Map.Entry<Integer, DataDraft>> stack = new ArrayDeque<>();
        DataDraft previous = null;
        for (ParserRuleContext context : contexts) {
            Ast.DataEntry entry = buildDataEntry(context);
            int level = parseLevel(entry.level());
            DataDraft draft = new DataDraft(entry);
            if (level == 88 && previous != null) {
                previous.children.add(draft);
                continue;
            }

            while (!stack.isEmpty() && stack.peek().getKey() >= level) stack.pop();
            if (level == 66) {
                DataDraft owner = stack.stream().reduce((a, b) -> b)
                        .map(Map.Entry::getValue).orElse(null);
                if (owner == null) roots.add(draft); else owner.children.add(draft);
                continue;
            }

            if (stack.isEmpty() || level == 77) roots.add(draft);
            else stack.peek().getValue().children.add(draft);
            if (level >= 1 && level <= 49) stack.push(Map.entry(level, draft));
            previous = draft;
        }
        return roots.stream().map(this::freezeDataDraft).toList();
    }

    private Ast.DataEntry freezeDataDraft(DataDraft draft) {
        Ast.DataEntry entry = draft.entry;
        return new Ast.DataEntry(entry.meta(), entry.level(), entry.levelKind(), entry.name(), entry.filler(),
                entry.visibility(), entry.declaration(), entry.clauses(),
                draft.children.stream().map(this::freezeDataDraft).toList());
    }

    private Ast.DeclarationVisibility declarationVisibility(ParserRuleContext context,
                                                             String externalRule, String globalRule) {
        boolean external = firstDescendant(context, externalRule) != null;
        boolean global = firstDescendant(context, globalRule) != null;
        if (external && global) {
            semanticDiagnostics.add(new SemanticCoverage.Diagnostic(
                    "CONFLICTING_DECLARATION_VISIBILITY",
                    "Declaration contains both GLOBAL and EXTERNAL visibility", meta(context)));
            return Ast.DeclarationVisibility.CONFLICTING;
        }
        if (external) return Ast.DeclarationVisibility.EXTERNAL;
        if (global) return Ast.DeclarationVisibility.GLOBAL;
        return Ast.DeclarationVisibility.LOCAL;
    }

    private Ast.DataClause buildDataClause(ParserRuleContext context) {
        String grammarRule = rule(context);
        String writtenText = sourceText(context).strip();
        Ast.Meta meta = meta(context);
        if (grammarRule.equals("dataPictureClause")) {
            ParserRuleContext picture = firstDescendant(context, "pictureString");
            return new Ast.PictureClause(meta, picture == null ? "" : sourceText(picture).strip(), writtenText);
        }
        if (grammarRule.equals("dataUsageClause")) {
            String usage = writtenText.replaceFirst("(?i)^USAGE\\s+(IS\\s+)?", "");
            return new Ast.UsageClause(meta, usage, writtenText);
        }
        if (grammarRule.equals("dataValueClause")) {
            List<String> values = directChildrenNamed(context, "dataValueInterval").stream()
                    .map(this::sourceText).map(String::strip).toList();
            return new Ast.ValueClause(meta, values, writtenText);
        }
        if (grammarRule.equals("dataRedefinesClause")) {
            return new Ast.RedefinesClause(meta,
                    simpleDataReference(firstDescendant(context, "dataName")), writtenText);
        }
        if (grammarRule.equals("dataRenamesClause")) {
            List<ParserRuleContext> names = nearestDescendants(context, "qualifiedDataName");
            Ast.DataReference from = (Ast.DataReference) expression(names.get(0), "renames");
            Ast.DataReference through = names.size() > 1
                    ? (Ast.DataReference) expression(names.get(1), "renames through") : null;
            return new Ast.RenamesClause(meta, from, through, writtenText);
        }
        if (grammarRule.equals("dataOccursClause")) {
            ParserRuleContext first = firstDirectOrNearest(context, Set.of("identifier", "integerLiteral"));
            ParserRuleContext to = firstDescendant(context, "dataOccursTo");
            ParserRuleContext depending = firstDescendant(context, "dataOccursDepending");
            Ast.Expression minimum = expression(first, "occurs minimum");
            Ast.Expression maximum = to == null ? null
                    : expression(firstDescendant(to, "integerLiteral"), "occurs maximum");
            Ast.DataReference dependingOn = depending == null ? null
                    : (Ast.DataReference) expression(firstDescendant(depending, "qualifiedDataName"),
                    "occurs depending");
            List<Ast.DataReference> keys = nearestDescendants(context, "dataOccursSort").stream()
                    .flatMap(x -> nearestDescendants(x, "qualifiedDataName").stream())
                    .map(x -> (Ast.DataReference) expression(x, "occurs key"))
                    .toList();
            List<Ast.IndexReference> indexes = nearestDescendants(context, "dataOccursIndexed").stream()
                    .flatMap(x -> nearestDescendants(x, "indexName").stream())
                    .map(x -> new Ast.IndexReference(meta(x), clean(sourceText(x)), sourceText(x).strip()))
                    .toList();
            return new Ast.OccursClause(meta, minimum, maximum, dependingOn, keys, indexes, writtenText);
        }
        List<Ast.Node> references = nearestDescendants(context,
                Set.of("qualifiedDataName", "identifier", "fileName", "indexName")).stream()
                .map(node -> rule(node).equals("qualifiedDataName") || rule(node).equals("identifier")
                        ? expression(node, "data clause") : nominalReference(node))
                .map(Ast.Node.class::cast).toList();
        return new Ast.PreservedDataClause(meta, grammarRule, writtenText, references);
    }

    private Ast.DataReference simpleDataReference(ParserRuleContext context) {
        return new Ast.DataReference(meta(context), clean(sourceText(context)), sourceText(context).strip(),
                List.of(), List.of(), null, Ast.ReferenceUnderstanding.STRUCTURED);
    }

    private static Ast.DataLevelKind levelKind(String level) {
        return switch (level) {
            case "66" -> Ast.DataLevelKind.RENAMES_66;
            case "77" -> Ast.DataLevelKind.STANDALONE_77;
            case "88" -> Ast.DataLevelKind.CONDITION_88;
            default -> level.matches("\\d+")
                    ? Ast.DataLevelKind.GROUP_OR_ELEMENTARY : Ast.DataLevelKind.OPAQUE;
        };
    }

    private static int parseLevel(String level) {
        try { return Integer.parseInt(level); }
        catch (NumberFormatException ignored) { return -1; }
    }

    private static Ast.DataSectionKind dataSectionKind(String grammarRule) {
        return switch (grammarRule) {
            case "fileSection" -> Ast.DataSectionKind.FILE;
            case "dataBaseSection" -> Ast.DataSectionKind.DATABASE;
            case "workingStorageSection" -> Ast.DataSectionKind.WORKING_STORAGE;
            case "linkageSection" -> Ast.DataSectionKind.LINKAGE;
            case "communicationSection" -> Ast.DataSectionKind.COMMUNICATION;
            case "localStorageSection" -> Ast.DataSectionKind.LOCAL_STORAGE;
            case "screenSection" -> Ast.DataSectionKind.SCREEN;
            case "reportSection" -> Ast.DataSectionKind.REPORT;
            default -> Ast.DataSectionKind.PROGRAM_LIBRARY;
        };
    }

    private Ast.Division buildProcedure(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Node> children = new ArrayList<>();
        Ast.ProcedureSignature signature = buildProcedureSignature(context);
        if (signature != null) children.add(signature);
        ParserRuleContext body = firstDescendant(context, "procedureDivisionBody");
        if (body != null) {
            for (ParserRuleContext child : directRuleChildren(body)) {
                if (rule(child).equals("paragraphs")) children.addAll(buildParagraphGroup(child));
                else if (rule(child).equals("procedureSection")) children.add(buildProcedureSection(child));
            }
        }
        return new Ast.Division(meta, Ast.DivisionKind.PROCEDURE, children);
    }

    private Ast.ProcedureSignature buildProcedureSignature(ParserRuleContext context) {
        ParserRuleContext using = firstDescendant(context, "procedureDivisionUsingClause");
        ParserRuleContext giving = firstDescendant(context, "procedureDivisionGivingClause");
        if (using == null && giving == null) return null;

        ParserRuleContext anchor = using != null ? using : giving;
        Ast.Meta meta = meta(anchor);
        List<Ast.ProcedureParameter> parameters = new ArrayList<>();
        if (using != null) {
            for (ParserRuleContext parameter : nearestDescendants(using,
                    Set.of("procedureDivisionByReference", "procedureDivisionByValue"))) {
                Ast.Meta parameterMeta = meta(parameter);
                boolean any = containsToken(parameter, "ANY");
                boolean optional = containsToken(parameter, "OPTIONAL");
                ParserRuleContext value = firstDirectOrNearest(parameter,
                        Set.of("identifier", "fileName", "literal"));
                Ast.PassingMode mode = rule(parameter).contains("ByValue")
                        ? Ast.PassingMode.VALUE : Ast.PassingMode.REFERENCE;
                parameters.add(new Ast.ProcedureParameter(parameterMeta, mode,
                        any ? null : expression(value, "procedure parameter"), optional, any,
                        sourceText(parameter).strip()));
            }
        }
        Ast.DataReference returning = giving == null ? null
                : simpleDataReference(firstDescendant(giving, "dataName"));
        String writtenText = sourceText(anchor).strip()
                + (giving == null ? "" : " " + sourceText(giving).strip());
        return new Ast.ProcedureSignature(meta, using != null && containsToken(using, "CHAINING"),
                parameters, returning, writtenText);
    }

    private Ast.Section buildProcedureSection(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext name = firstDescendant(context, "sectionName");
        ParserRuleContext paragraphs = firstDescendant(context, "paragraphs");
        return new Ast.Section(meta, name == null ? "<section>" : clean(sourceText(name)),
                paragraphs == null ? List.of() : buildParagraphGroup(paragraphs));
    }

    private List<Ast.Node> buildParagraphGroup(ParserRuleContext context) {
        List<Ast.Node> result = new ArrayList<>();
        List<ParserRuleContext> leading = directChildrenNamed(context, "sentence");
        if (!leading.isEmpty()) {
            Ast.Meta syntheticMeta = meta(context);
            List<Ast.Sentence> sentences = leading.stream().map(this::buildSentence).toList();
            result.add(new Ast.Paragraph(syntheticMeta, "<entry>", sentences));
        }
        for (ParserRuleContext paragraph : directChildrenNamed(context, "paragraph")) result.add(buildParagraph(paragraph));
        return result;
    }

    private Ast.Paragraph buildParagraph(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext name = firstDescendant(context, "paragraphName");
        List<Ast.Sentence> sentences = directChildrenNamed(context, "sentence").stream().map(this::buildSentence).toList();
        return new Ast.Paragraph(meta, name == null ? "<paragraph>" : clean(sourceText(name)), sentences);
    }

    private Ast.Sentence buildSentence(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Statement> statements = directChildrenNamed(context, "statement").stream().map(this::buildStatement).toList();
        Token stop = context.getStop();
        Ast.SourceSpan terminator = stop == null ? meta.span() : new Ast.SourceSpan(stop.getLine(), stop.getCharPositionInLine(),
                stop.getLine(), stop.getCharPositionInLine() + Math.max(0, stop.getText().length() - 1),
                stop.getTokenIndex(), stop.getTokenIndex());
        return new Ast.Sentence(meta, statements, Ast.SentenceTerminator.PERIOD, terminator);
    }

    private Ast.Statement buildStatement(ParserRuleContext wrapper) {
        ParserRuleContext concrete = directRuleChildren(wrapper).stream().findFirst().orElse(wrapper);
        String grammarRule = rule(concrete);
        Ast.Statement statement = switch (grammarRule) {
            case "callStatement" -> buildCall(concrete);
            case "ifStatement" -> buildIf(concrete);
            case "evaluateStatement" -> buildEvaluate(concrete);
            case "performStatement" -> buildPerform(concrete);
            case "goToStatement" -> buildGoTo(concrete);
            case "moveStatement" -> buildMove(concrete);
            case "execSqlStatement" -> buildEmbedded(concrete, Ast.EmbeddedLanguage.SQL);
            case "execCicsStatement" -> buildEmbedded(concrete, Ast.EmbeddedLanguage.CICS);
            case "execSqlImsStatement" -> buildEmbedded(concrete, Ast.EmbeddedLanguage.SQLIMS);
            case "nextSentenceStatement" -> new Ast.NextSentenceStatement(meta(concrete));
            default -> {
                if (MODELED_GENERIC_STATEMENTS.contains(grammarRule)) yield buildStructuredStatement(concrete, false);
                if (PRESERVED_STATEMENTS.contains(grammarRule)) yield buildStructuredStatement(concrete, true);
                throw new IllegalStateException("Unclassified statement alternative: " + grammarRule);
            }
        };
        coverageDrafts.add(new CoverageDraft(rule(concrete), statement.meta(), sourceText(concrete),
                statement.meta().id()));
        if (LOG.isTraceEnabled()) {
            String sourceFile = statement.meta().provenance().original().file();
            if (statement instanceof Ast.PreservedStatement) {
                LOG.trace("event=ast_statement_preserved source={} phase=AST_BUILD grammarRule={} line={}",
                        sourceFile, grammarRule, statement.meta().span().startLine());
            } else {
                LOG.trace("event=ast_statement_modeled source={} phase=AST_BUILD grammarRule={} line={}",
                        sourceFile, grammarRule, statement.meta().span().startLine());
            }
        }
        return statement;
    }

    private SemanticCoverage.Report buildCoverageReport() {
        Comparator<CoverageDraft> bySourceOrder = Comparator
                .comparingInt((CoverageDraft draft) -> draft.meta().span().startToken())
                .thenComparingInt(CoverageDraft::astNodeId);
        List<CoverageDraft> ordered = coverageDrafts.stream().sorted(bySourceOrder).toList();
        List<SemanticCoverage.Finding> findings = new ArrayList<>();
        for (CoverageDraft draft : ordered) {
            GrammarCoverageManifest.Entry policy = GrammarCoverageManifest.entry(
                    GrammarCoverageManifest.Grammar.COBOL, draft.grammarRule());
            findings.add(new SemanticCoverage.Finding(findings.size(), draft.grammarRule(), draft.meta(),
                    draft.writtenText(), policy.coverage(), policy.dependencyKnowledge(),
                    policy.rationale(), draft.astNodeId()));
        }
        return new SemanticCoverage.Report(findings);
    }

    private Ast.CallStatement buildCall(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext targetContext = firstDirectOrNearest(context, Set.of("identifier", "literal"));
        Ast.Expression target = targetContext != null && rule(targetContext).equals("literal")
                ? new Ast.ProgramReference(meta(targetContext), unquote(sourceText(targetContext).strip()), sourceText(targetContext).strip())
                : expression(targetContext, "call target");
        Ast.CallTargetSyntax kind = target instanceof Ast.ProgramReference
                ? Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME : Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION;
        List<Ast.CallArgument> arguments = new ArrayList<>();
        for (ParserRuleContext arg : nearestDescendants(context, Set.of("callByReference", "callByValue", "callByContent"))) {
            Ast.PassingMode mode = switch (rule(arg)) {
                case "callByValue" -> Ast.PassingMode.VALUE;
                case "callByContent" -> Ast.PassingMode.CONTENT;
                default -> Ast.PassingMode.REFERENCE;
            };
            Ast.Meta argMeta = meta(arg);
            Ast.CallArgumentKind argumentKind = containsToken(arg, "OMITTED")
                    ? Ast.CallArgumentKind.OMITTED : containsToken(arg, "ADDRESS")
                    ? Ast.CallArgumentKind.ADDRESS_OF : containsToken(arg, "LENGTH")
                    ? Ast.CallArgumentKind.LENGTH_OF : Ast.CallArgumentKind.VALUE;
            ParserRuleContext value = firstDirectOrNearest(arg, Set.of("identifier", "literal", "fileName"));
            arguments.add(new Ast.CallArgument(argMeta, mode, argumentKind,
                    argumentKind == Ast.CallArgumentKind.OMITTED ? null : expression(value, "argument"),
                    sourceText(arg).strip()));
        }
        ParserRuleContext giving = firstDescendant(context, "callGivingPhrase");
        Ast.Expression returning = giving == null ? null
                : expression(firstDescendant(giving, "identifier"), "call returning");
        return new Ast.CallStatement(meta, kind, target, arguments, returning, directNestedStatements(context));
    }

    private Ast.Statement buildStructuredStatement(ParserRuleContext context, boolean preserved) {
        Ast.Meta meta = meta(context);
        List<Ast.StatementOperand> operands = new ArrayList<>();
        collectStatementOperands(context, context, operands);
        List<Ast.StatementClause> clauses = nearestDescendants(context, FLOW_CLAUSE_RULES).stream()
                .map(this::buildStatementClause).toList();
        return preserved
                ? new Ast.PreservedStatement(meta, rule(context), sourceText(context).strip(), operands, clauses)
                : new Ast.ModeledStatement(meta, rule(context), sourceText(context).strip(), operands, clauses);
    }

    private void collectStatementOperands(ParserRuleContext root, ParserRuleContext context,
                                          List<Ast.StatementOperand> output) {
        for (ParserRuleContext child : directRuleChildren(context)) {
            if (child != root && rule(child).equals("statement")) continue;
            if (OPERAND_RULES.contains(rule(child))) {
                Ast.Meta operandMeta = meta(child);
                output.add(new Ast.StatementOperand(operandMeta, rule(context), statementOperand(child)));
            } else {
                collectStatementOperands(root, child, output);
            }
        }
    }

    private Ast.Node statementOperand(ParserRuleContext context) {
        return switch (rule(context)) {
            case "identifier", "qualifiedDataName" -> expression(context, "statement operand");
            case "procedureName" -> procedureReference(context);
            case "fileName" -> new Ast.FileReference(meta(context), clean(sourceText(context)), sourceText(context).strip());
            case "indexName" -> new Ast.IndexReference(meta(context), clean(sourceText(context)), sourceText(context).strip());
            case "literal", "integerLiteral", "numericLiteral" -> literalExpression(context);
            default -> new Ast.NamedReference(meta(context), rule(context), sourceText(context).strip());
        };
    }

    private Ast.StatementClause buildStatementClause(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        return new Ast.StatementClause(meta, rule(context), sourceText(context).strip(), List.of(),
                statementsInside(context));
    }

    static Set<String> supportedStatementRules() {
        Set<String> result = new LinkedHashSet<>(MODELED_GENERIC_STATEMENTS);
        result.addAll(PRESERVED_STATEMENTS);
        result.addAll(Set.of("callStatement", "ifStatement", "evaluateStatement", "performStatement",
                "goToStatement", "moveStatement", "execSqlStatement", "execCicsStatement",
                "execSqlImsStatement", "nextSentenceStatement"));
        return Set.copyOf(result);
    }

    private Ast.IfStatement buildIf(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext condition = firstDescendant(context, "condition");
        ParserRuleContext thenContext = firstDescendant(context, "ifThen");
        ParserRuleContext elseContext = firstDescendant(context, "ifElse");
        return new Ast.IfStatement(meta, expression(condition, "condition"),
                statementsInside(thenContext), statementsInside(elseContext), containsToken(context, "END-IF"));
    }

    private Ast.EvaluateStatement buildEvaluate(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Expression> subjects = new ArrayList<>();
        for (ParserRuleContext child : directRuleChildren(context)) {
            if (rule(child).equals("evaluateSelect")) subjects.add(expression(child, "subject"));
            else if (rule(child).equals("evaluateAlsoSelect")) {
                ParserRuleContext select = firstDescendant(child, "evaluateSelect");
                if (select != null) subjects.add(expression(select, "subject"));
            }
        }
        List<Ast.EvaluateBranch> branches = new ArrayList<>();
        for (ParserRuleContext branch : directChildrenNamed(context, "evaluateWhenPhrase")) {
            Ast.Meta branchMeta = meta(branch);
            List<Ast.Expression> selectors = new ArrayList<>();
            for (ParserRuleContext when : directChildrenNamed(branch, "evaluateWhen")) {
                ParserRuleContext condition = firstDescendant(when, "evaluateCondition");
                if (condition != null) selectors.add(expression(condition, "evaluate selector"));
                for (ParserRuleContext also : directChildrenNamed(when, "evaluateAlsoCondition")) {
                    ParserRuleContext alsoCondition = firstDescendant(also, "evaluateCondition");
                    if (alsoCondition != null) selectors.add(expression(alsoCondition, "evaluate selector"));
                }
            }
            branches.add(new Ast.EvaluateBranch(branchMeta, selectors,
                    directChildrenNamed(branch, "evaluateWhen").stream()
                            .map(this::sourceText).map(AstBuilder::compact).reduce((a, b) -> a + " " + b).orElse(""), false,
                    directChildrenNamed(branch, "statement").stream().map(this::buildStatement).toList()));
        }
        ParserRuleContext other = firstDescendant(context, "evaluateWhenOther");
        if (other != null) branches.add(new Ast.EvaluateBranch(meta(other), List.of(), "OTHER", true, statementsInside(other)));
        return new Ast.EvaluateStatement(meta, subjects, branches, containsToken(context, "END-EVALUATE"));
    }

    private Ast.PerformStatement buildPerform(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext inline = firstDescendant(context, "performInlineStatement");
        ParserRuleContext procedure = firstDescendant(context, "performProcedureStatement");
        ParserRuleContext type = firstDescendant(context, "performType");
        List<Ast.Expression> controls = type == null ? List.of() : controlExpressions(type);
        if (inline != null) return new Ast.PerformStatement(meta, Ast.PerformKind.INLINE, null, null,
                type == null ? "once" : compact(sourceText(type)), controls, statementsInside(inline));
        List<ParserRuleContext> names = procedure == null ? List.of() : nearestDescendants(procedure, "procedureName");
        return new Ast.PerformStatement(meta, Ast.PerformKind.PROCEDURE,
                names.isEmpty() ? null : procedureReference(names.get(0)),
                names.size() < 2 ? null : procedureReference(names.get(1)),
                type == null ? "once" : compact(sourceText(type)), controls, List.of());
    }

    private Ast.GoToStatement buildGoTo(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext depending = firstDescendant(context, "goToDependingOnStatement");
        List<Ast.ProcedureReference> targets = nearestDescendants(context, "procedureName").stream()
                .map(this::procedureReference).toList();
        ParserRuleContext selector = depending == null ? null : firstDescendant(depending, "identifier");
        return new Ast.GoToStatement(meta, depending == null ? Ast.GoToKind.SIMPLE : Ast.GoToKind.DEPENDING_ON,
                targets, selector == null ? null : expression(selector, "selector"));
    }

    private Ast.MoveStatement buildMove(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext sending = firstDirectOrNearest(context,
                Set.of("moveToSendingArea", "moveCorrespondingToSendingArea"));
        List<ParserRuleContext> identifiers = nearestDescendants(context, "identifier");
        Ast.Expression sourceExpression = expression(firstDirectOrNearest(sending, Set.of("identifier", "literal")), "source");
        int sourceToken = sourceExpression.meta().span().startToken();
        List<Ast.Expression> targets = identifiers.stream().filter(i -> i.getStart().getTokenIndex() != sourceToken)
                .map(i -> expression(i, "target")).toList();
        return new Ast.MoveStatement(meta, sourceExpression, targets, containsToken(context, "CORRESPONDING") || containsToken(context, "CORR"));
    }

    private Ast.EmbeddedLanguageStatement buildEmbedded(ParserRuleContext context, Ast.EmbeddedLanguage language) {
        return new Ast.EmbeddedLanguageStatement(meta(context), language, sourceText(context).strip());
    }

    private Ast.UnsupportedStatement buildUnsupported(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        LOG.trace("event=ast_construct_unsupported source={} phase=AST_BUILD grammarRule={} line={}",
                meta.provenance().original().file(), rule(context), meta.span().startLine());
        List<Ast.Node> references = nearestDescendants(context,
                Set.of("procedureName", "fileName", "indexName")).stream().map(this::nominalReference).toList();
        return new Ast.UnsupportedStatement(meta, rule(context), compact(sourceText(context)), references,
                directNestedStatements(context));
    }

    private Ast.Expression expression(ParserRuleContext context, String role) {
        if (context == null) return new Ast.RawExpression(syntheticMeta(role), role, "<missing>");
        String contextRule = rule(context);
        if (contextRule.equals("identifier")) return identifierExpression(context);
        if (contextRule.equals("fileName")) return new Ast.FileReference(meta(context), clean(sourceText(context)),
                sourceText(context).strip());
        if (contextRule.equals("indexName")) return new Ast.IndexReference(meta(context), clean(sourceText(context)),
                sourceText(context).strip());
        if (contextRule.equals("qualifiedDataName") || contextRule.equals("conditionNameReference"))
            return dataReference(context);
        if (contextRule.equals("tableCall")) return tableReference(context);
        if (contextRule.equals("functionCall")) return functionExpression(context);
        if (contextRule.equals("specialRegister")) return specialRegisterExpression(context);
        if (contextRule.equals("arithmeticExpression") || contextRule.equals("multDivs")
                || contextRule.equals("powers") || contextRule.equals("basis"))
            return arithmeticExpression(context);
        if (isConditionRule(contextRule)) return conditionExpression(context);
        if (contextRule.equals("subscript")) return subscriptExpression(context);
        if (Set.of("evaluateSelect", "evaluateValue", "evaluateCondition", "argument", "characterPosition", "length",
                "performFrom", "performBy").contains(contextRule)) {
            ParserRuleContext child = firstDirectOrNearest(context, Set.of("condition", "arithmeticExpression",
                    "identifier", "qualifiedDataName", "literal", "integerLiteral"));
            if (child != null) return expression(child, role);
        }
        if (contextRule.equals("literal") || contextRule.endsWith("Literal")) return literalExpression(context);
        ParserRuleContext exact = firstDirectOrNearest(context, Set.of("condition", "arithmeticExpression",
                "identifier", "literal"));
        if (exact != null && compact(sourceText(context)).equals(compact(sourceText(exact))))
            return expression(exact, role);
        Ast.Meta meta = meta(context);
        List<Ast.Expression> recognized = nearestDescendants(context,
                Set.of("condition", "arithmeticExpression", "identifier", "literal")).stream()
                .map(child -> expression(child, role)).toList();
        return new Ast.PreservedExpression(meta, contextRule, sourceText(context).strip(), recognized,
                Ast.ReferenceUnderstanding.PRESERVED);
    }

    private Ast.Expression subscriptExpression(ParserRuleContext context) {
        List<ParserRuleContext> parts = directRuleChildren(context);
        if (parts.size() > 1) {
            Ast.Meta meta = meta(context);
            return new Ast.OperationExpression(meta, "RELATIVE_SUBSCRIPT",
                    parts.stream().map(part -> expression(part, "relative subscript")).toList(),
                    sourceText(context).strip());
        }
        if (parts.size() == 1) return expression(parts.get(0), "subscript");
        return preservedExpression(context, "subscript");
    }

    private List<Ast.Expression> controlExpressions(ParserRuleContext performType) {
        return nearestDescendants(performType,
                Set.of("condition", "arithmeticExpression", "identifier", "literal")).stream()
                .map(context -> expression(context, "perform control")).toList();
    }

    private Ast.Expression identifierExpression(ParserRuleContext identifier) {
        ParserRuleContext child = directRuleChildren(identifier).stream().findFirst().orElse(null);
        if (child == null) return preservedExpression(identifier, "identifier");
        return switch (rule(child)) {
            case "qualifiedDataName" -> dataReference(child);
            case "tableCall" -> tableReference(child);
            case "functionCall" -> functionExpression(child);
            case "specialRegister" -> specialRegisterExpression(child);
            default -> preservedExpression(identifier, "identifier");
        };
    }

    private Ast.DataReference dataReference(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext qualified = rule(context).equals("qualifiedDataName") ? context
                : firstDescendant(context, "qualifiedDataName");
        ParserRuleContext base = qualified == null ? firstDirectOrNearest(context,
                Set.of("dataName", "conditionName", "paragraphName", "textName"))
                : firstDirectOrNearest(qualified, Set.of("dataName", "conditionName", "paragraphName", "textName"));
        String baseName = base == null ? firstSemanticWord(context) : clean(sourceText(base));
        List<Ast.DataQualifier> qualifiers = buildQualifiers(qualified == null ? context : qualified);
        return new Ast.DataReference(meta, baseName, sourceText(context).strip(), qualifiers, List.of(), null,
                Ast.ReferenceUnderstanding.STRUCTURED);
    }

    private Ast.DataReference tableReference(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext qualified = firstDescendant(context, "qualifiedDataName");
        ParserRuleContext base = firstDirectOrNearest(qualified,
                Set.of("dataName", "conditionName", "paragraphName", "textName"));
        List<Ast.DataQualifier> qualifiers = qualified == null ? List.of() : buildQualifiers(qualified);
        List<Ast.SubscriptGroup> groups = new ArrayList<>();
        List<ParserRuleContext> current = null;
        for (int i = 0; i < context.getChildCount(); i++) {
            ParseTree child = context.getChild(i);
            if (child instanceof ParserRuleContext ruleContext && rule(ruleContext).equals("referenceModifier")) break;
            if (child instanceof TerminalNode terminal && terminal.getText().equals("(")) current = new ArrayList<>();
            else if (child instanceof ParserRuleContext ruleContext && rule(ruleContext).equals("subscript") && current != null)
                current.add(ruleContext);
            else if (child instanceof TerminalNode terminal && terminal.getText().equals(")") && current != null) {
                if (!current.isEmpty()) {
                    Ast.Meta groupMeta = meta(current.get(0));
                    List<Ast.Expression> subscripts = current.stream().map(subscript -> expression(subscript, "subscript")).toList();
                    groups.add(new Ast.SubscriptGroup(groupMeta, subscripts,
                            "(" + sourceBetween(current.get(0), current.get(current.size() - 1)) + ")"));
                }
                current = null;
            }
        }
        ParserRuleContext modifier = firstDescendant(context, "referenceModifier");
        Ast.ReferenceModification referenceModification = modifier == null ? null : referenceModification(modifier);
        return new Ast.DataReference(meta, base == null ? firstSemanticWord(context) : clean(sourceText(base)),
                sourceText(context).strip(), qualifiers, groups, referenceModification,
                Ast.ReferenceUnderstanding.STRUCTURED);
    }

    private List<Ast.DataQualifier> buildQualifiers(ParserRuleContext qualified) {
        List<ParserRuleContext> contexts = nearestDescendants(qualified, Set.of("inData", "inTable", "inFile"));
        List<Ast.DataQualifier> result = new ArrayList<>();
        boolean generalFormat = rule(qualified).equals("qualifiedDataNameFormat1")
                || firstDescendant(qualified, "qualifiedDataNameFormat1") != null;
        for (int i = 0; i < contexts.size(); i++) {
            ParserRuleContext qualifier = contexts.get(i);
            String written = sourceText(qualifier).strip();
            Ast.QualifierConnector connector = containsToken(qualifier, "IN")
                    ? Ast.QualifierConnector.IN : Ast.QualifierConnector.OF;
            String qualifierRule = rule(qualifier);
            Ast.QualifierTarget target = qualifierRule.equals("inFile") ? Ast.QualifierTarget.FILE
                    : qualifierRule.equals("inData") && generalFormat && i == contexts.size() - 1
                    ? Ast.QualifierTarget.DATA_OR_FILE : Ast.QualifierTarget.DATA;
            Ast.Meta qualifierMeta = meta(qualifier);
            ParserRuleContext value = directRuleChildren(qualifier).stream().reduce((first, second) -> second).orElse(null);
            Ast.DataReference reference;
            if (value != null && rule(value).equals("tableCall")) reference = tableReference(value);
            else {
                String name = value == null ? written.replaceFirst("(?i)^(IN|OF)\\s+", "") : clean(sourceText(value));
                reference = new Ast.DataReference(value == null ? qualifierMeta : meta(value), name,
                        value == null ? name : sourceText(value).strip(), List.of(), List.of(), null,
                        Ast.ReferenceUnderstanding.STRUCTURED);
            }
            result.add(new Ast.DataQualifier(qualifierMeta, connector, target, reference, written));
        }
        return result;
    }

    private Ast.ReferenceModification referenceModification(ParserRuleContext modifier) {
        Ast.Meta meta = meta(modifier);
        ParserRuleContext offset = firstDescendant(modifier, "characterPosition");
        ParserRuleContext length = firstDescendant(modifier, "length");
        return new Ast.ReferenceModification(meta, expression(offset, "reference offset"),
                length == null ? null : expression(length, "reference length"), sourceText(modifier).strip());
    }

    private Ast.FunctionExpression functionExpression(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext name = firstDescendant(context, "functionName");
        List<Ast.Expression> arguments = directChildrenNamed(context, "argument").stream()
                .map(argument -> expression(argument, "function argument")).toList();
        ParserRuleContext modifier = firstDescendant(context, "referenceModifier");
        return new Ast.FunctionExpression(meta, name == null ? "<unknown>" : clean(sourceText(name)), arguments,
                modifier == null ? null : referenceModification(modifier), sourceText(context).strip());
    }

    private Ast.SpecialRegisterExpression specialRegisterExpression(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Expression> operands = nearestDescendants(context, "identifier").stream()
                .map(this::identifierExpression).toList();
        return new Ast.SpecialRegisterExpression(meta, specialRegisterName(context), operands,
                sourceText(context).strip());
    }

    private String specialRegisterName(ParserRuleContext context) {
        String terminal = firstTerminal(context);
        return terminal.isBlank() ? firstSemanticWord(context).toUpperCase(Locale.ROOT) : terminal.toUpperCase(Locale.ROOT);
    }

    private Ast.LiteralExpression literalExpression(ParserRuleContext context) {
        String raw = sourceText(context).strip();
        return new Ast.LiteralExpression(meta(context), unquote(raw), raw);
    }

    private Ast.Expression arithmeticExpression(ParserRuleContext context) {
        String contextRule = rule(context);
        if (contextRule.equals("arithmeticExpression"))
            return sequenceOperation(context, "multDivs", "plusMinus", Set.of("+", "-"));
        if (contextRule.equals("multDivs"))
            return sequenceOperation(context, "powers", "multDiv", Set.of("*", "/"));
        if (contextRule.equals("powers")) {
            List<ParserRuleContext> powers = directChildrenNamed(context, "power");
            ParserRuleContext basis = firstDescendant(context, "basis");
            String text = sourceText(context).strip();
            if (!powers.isEmpty()) {
                Ast.Meta meta = meta(context);
                List<Ast.Expression> operands = new ArrayList<>();
                operands.add(expression(basis, "power base"));
                for (ParserRuleContext power : powers) operands.add(expression(firstDescendant(power, "basis"), "exponent"));
                return new Ast.OperationExpression(meta, "**", operands, text);
            }
            if (text.startsWith("+") || text.startsWith("-"))
                return new Ast.OperationExpression(meta(context), text.substring(0, 1),
                        List.of(expression(basis, "unary operand")), text);
            return expression(basis, "arithmetic basis");
        }
        if (contextRule.equals("basis")) {
            ParserRuleContext grouped = firstDescendant(context, "arithmeticExpression");
            if (sourceText(context).strip().startsWith("(") && grouped != null)
                return new Ast.OperationExpression(meta(context), "GROUP",
                        List.of(expression(grouped, "grouped arithmetic")), sourceText(context).strip());
            ParserRuleContext child = firstDirectOrNearest(context, Set.of("identifier", "literal"));
            return expression(child, "arithmetic value");
        }
        return preservedExpression(context, "arithmetic");
    }

    private Ast.Expression sequenceOperation(ParserRuleContext context, String firstRule,
                                               String remainderRule, Set<String> operators) {
        List<ParserRuleContext> remainder = directChildrenNamed(context, remainderRule);
        ParserRuleContext first = directChildrenNamed(context, firstRule).stream().findFirst().orElse(null);
        if (remainder.isEmpty()) return expression(first, "arithmetic operand");
        Ast.Meta meta = meta(context);
        List<Ast.Expression> operands = new ArrayList<>();
        operands.add(expression(first, "arithmetic operand"));
        String operator = null;
        for (ParserRuleContext item : remainder) {
            String itemText = sourceText(item).strip();
            String itemOperator = operators.stream().filter(itemText::startsWith).findFirst().orElse(remainderRule);
            if (operator == null) operator = itemOperator;
            else if (!operator.equals(itemOperator)) operator = "MIXED_ARITHMETIC";
            ParserRuleContext operand = firstDescendant(item, firstRule);
            if (operand == null) operand = firstDescendant(item, "basis");
            operands.add(expression(operand, "arithmetic operand"));
        }
        return new Ast.OperationExpression(meta, operator, operands, sourceText(context).strip());
    }

    private Ast.Expression conditionExpression(ParserRuleContext context) {
        String contextRule = rule(context);
        if (contextRule.equals("condition")) {
            List<ParserRuleContext> combinables = new ArrayList<>(directChildrenNamed(context, "combinableCondition"));
            List<ParserRuleContext> tails = directChildrenNamed(context, "andOrCondition");
            if (tails.isEmpty()) return expression(combinables.get(0), "condition");
            Ast.Meta meta = meta(context);
            List<Ast.Expression> operands = new ArrayList<>();
            operands.add(expression(combinables.get(0), "condition"));
            String operator = null;
            for (ParserRuleContext tail : tails) {
                String next = firstKeyword(tail, Set.of("AND", "OR"));
                operator = operator == null ? next : operator.equals(next) ? operator : "MIXED_LOGICAL";
                ParserRuleContext operand = firstDirectOrNearest(tail, Set.of("combinableCondition", "abbreviation"));
                operands.add(expression(operand, "condition"));
            }
            return new Ast.OperationExpression(meta, operator, operands, sourceText(context).strip());
        }
        if (contextRule.equals("combinableCondition")) {
            ParserRuleContext simple = firstDescendant(context, "simpleCondition");
            if (containsToken(context, "NOT")) return new Ast.OperationExpression(meta(context), "NOT",
                    List.of(expression(simple, "negated condition")), sourceText(context).strip());
            return expression(simple, "condition");
        }
        if (contextRule.equals("simpleCondition")) {
            ParserRuleContext child = directRuleChildren(context).stream().findFirst().orElse(null);
            if (sourceText(context).strip().startsWith("(") && child != null)
                return new Ast.OperationExpression(meta(context), "GROUP", List.of(expression(child, "condition")),
                        sourceText(context).strip());
            return expression(child, "condition");
        }
        if (contextRule.equals("relationArithmeticComparison")) {
            Ast.Meta meta = meta(context);
            List<ParserRuleContext> values = directChildrenNamed(context, "arithmeticExpression");
            ParserRuleContext operator = firstDescendant(context, "relationalOperator");
            return new Ast.OperationExpression(meta, operator == null ? "RELATION" : compact(sourceText(operator)).toUpperCase(Locale.ROOT),
                    values.stream().map(value -> expression(value, "comparison operand")).toList(), sourceText(context).strip());
        }
        if (contextRule.equals("classCondition") || contextRule.equals("relationSignCondition")) {
            Ast.Meta meta = meta(context);
            List<Ast.Expression> operands = nearestDescendants(context,
                    Set.of("arithmeticExpression", "identifier")).stream()
                    .map(value -> expression(value, "predicate operand")).toList();
            return new Ast.OperationExpression(meta, contextRule, operands, sourceText(context).strip());
        }
        if (contextRule.equals("conditionNameReference")) return dataReference(context);
        ParserRuleContext child = directRuleChildren(context).stream().findFirst().orElse(null);
        if (child != null && isConditionRule(rule(child))) return expression(child, "condition");
        return preservedExpression(context, "condition");
    }

    private Ast.PreservedExpression preservedExpression(ParserRuleContext context, String role) {
        Ast.Meta meta = meta(context);
        List<Ast.Expression> recognized = nearestDescendants(context,
                Set.of("condition", "arithmeticExpression", "identifier", "literal")).stream()
                .map(child -> expression(child, role)).toList();
        return new Ast.PreservedExpression(meta, rule(context), sourceText(context).strip(), recognized,
                Ast.ReferenceUnderstanding.PRESERVED);
    }

    private boolean isConditionRule(String rule) {
        return Set.of("condition", "combinableCondition", "simpleCondition", "relationCondition",
                "relationSignCondition", "relationArithmeticComparison", "relationCombinedComparison",
                "relationCombinedCondition", "classCondition", "conditionNameReference", "abbreviation").contains(rule);
    }

    private String firstKeyword(ParseTree tree, Set<String> keywords) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            String cleaned = clean(child.getText()).toUpperCase(Locale.ROOT);
            if (child instanceof TerminalNode && keywords.contains(cleaned)) return cleaned;
            String nested = firstKeyword(child, keywords);
            if (nested != null) return nested;
        }
        return "UNKNOWN";
    }

    private String firstSemanticWord(ParserRuleContext context) {
        return sourceText(context).strip().split("[\\s(]", 2)[0];
    }

    private String sourceBetween(ParserRuleContext startContext, ParserRuleContext endContext) {
        int start = Math.max(0, startContext.getStart().getStartIndex());
        int end = Math.min(source.length(), endContext.getStop().getStopIndex() + 1);
        return source.substring(start, end).strip();
    }

    private Ast.Node nominalReference(ParserRuleContext context) {
        return switch (rule(context)) {
            case "procedureName" -> procedureReference(context);
            case "fileName" -> new Ast.FileReference(meta(context), clean(sourceText(context)), sourceText(context).strip());
            case "indexName" -> new Ast.IndexReference(meta(context), clean(sourceText(context)), sourceText(context).strip());
            default -> throw new IllegalArgumentException("unsupported nominal reference: " + rule(context));
        };
    }

    private Ast.ProcedureReference procedureReference(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext paragraph = directChildrenNamed(context, "paragraphName").stream().findFirst().orElse(null);
        ParserRuleContext section = directChildrenNamed(context, "sectionName").stream().findFirst().orElse(null);
        ParserRuleContext qualification = directChildrenNamed(context, "inSection").stream().findFirst().orElse(null);
        String baseName = paragraph != null ? clean(sourceText(paragraph))
                : section != null ? clean(sourceText(section)) : firstSemanticWord(context);
        Ast.ProcedureQualifier qualifier = null;
        if (qualification != null) {
            ParserRuleContext qualifiedSection = firstDescendant(qualification, "sectionName");
            qualifier = new Ast.ProcedureQualifier(meta(qualification),
                    containsToken(qualification, "IN") ? Ast.QualifierConnector.IN : Ast.QualifierConnector.OF,
                    qualifiedSection == null ? "<unknown>" : clean(sourceText(qualifiedSection)),
                    sourceText(qualification).strip());
        }
        return new Ast.ProcedureReference(meta, baseName, sourceText(context).strip(), qualifier);
    }

    private Ast.Meta syntheticMeta(String label) {
        int id = nextId++;
        return new Ast.Meta(id, new Ast.SourceSpan(0, 0, 0, 0, -1, -1),
                new Ast.ParseTreeOrigin(-1, label, 0));
    }

    private Ast.Meta meta(ParserRuleContext context) {
        int id = nextId++;
        Token start = context.getStart(), stop = context.getStop();
        int startLine = start == null ? 0 : start.getLine();
        int startColumn = start == null ? 0 : start.getCharPositionInLine();
        int endLine = stop == null ? startLine : stop.getLine();
        int endColumn = stop == null ? startColumn : stop.getCharPositionInLine() + Math.max(0, stop.getText().length() - 1);
        int startToken = start == null ? -1 : start.getTokenIndex();
        int endToken = stop == null ? startToken : stop.getTokenIndex();
        Ast.SourceSpan span = new Ast.SourceSpan(startLine, startColumn, endLine, endColumn, startToken, endToken);
        int startOffset = start == null ? 0 : Math.max(0, start.getStartIndex());
        int endOffset = stop == null ? startOffset : Math.min(source.length(), stop.getStopIndex() + 1);
        return new Ast.Meta(id, span,
                new Ast.ParseTreeOrigin(parseIds.getOrDefault(context, -1), rule(context),
                        parseSubtreeSizes.getOrDefault(context, 1)),
                sourceMap.provenance(startOffset, endOffset));
    }

    private List<Ast.Statement> statementsInside(ParserRuleContext context) {
        if (context == null) return List.of();
        List<ParserRuleContext> statements = directChildrenNamed(context, "statement");
        if (statements.isEmpty() && containsToken(context, "NEXT") && containsToken(context, "SENTENCE"))
            return List.of(new Ast.NextSentenceStatement(meta(context)));
        return statements.stream().map(this::buildStatement).toList();
    }

    private List<Ast.Statement> directNestedStatements(ParserRuleContext context) {
        List<ParserRuleContext> result = new ArrayList<>();
        collectNearest(context, Set.of("statement"), result, true);
        return result.stream().map(this::buildStatement).toList();
    }

    private ParserRuleContext firstDirectOrNearest(ParserRuleContext context, Set<String> names) {
        if (context == null) return null;
        for (ParserRuleContext child : directRuleChildren(context)) if (names.contains(rule(child))) return child;
        List<ParserRuleContext> found = nearestDescendants(context, names);
        return found.isEmpty() ? null : found.get(0);
    }

    private ParserRuleContext firstDescendant(ParseTree tree, String name) {
        if (tree == null) return null;
        if (tree instanceof ParserRuleContext context && rule(context).equals(name)) return context;
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParserRuleContext found = firstDescendant(tree.getChild(i), name);
            if (found != null) return found;
        }
        return null;
    }

    private List<ParserRuleContext> nearestDescendants(ParseTree tree, String name) {
        return nearestDescendants(tree, Set.of(name));
    }

    private List<ParserRuleContext> nearestDescendants(ParseTree tree, Set<String> names) {
        List<ParserRuleContext> result = new ArrayList<>();
        collectNearest(tree, names, result, false);
        return result;
    }

    private void collectNearest(ParseTree tree, Set<String> names, List<ParserRuleContext> result, boolean skipRoot) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof ParserRuleContext context && names.contains(rule(context))) result.add(context);
            else collectNearest(child, names, result, false);
        }
    }

    private List<ParserRuleContext> directChildrenNamed(ParserRuleContext context, String name) {
        if (context == null) return List.of();
        return directRuleChildren(context).stream().filter(c -> rule(c).equals(name)).toList();
    }

    private List<ParserRuleContext> directRuleChildren(ParseTree context) {
        List<ParserRuleContext> result = new ArrayList<>();
        for (int i = 0; i < context.getChildCount(); i++)
            if (context.getChild(i) instanceof ParserRuleContext child) result.add(child);
        return result;
    }

    private String rule(ParserRuleContext context) {
        return parser.getRuleNames()[context.getRuleIndex()];
    }

    private String sourceText(ParserRuleContext context) {
        if (context == null || context.getStart() == null || context.getStop() == null) return "";
        int start = Math.max(0, context.getStart().getStartIndex());
        int end = Math.min(source.length(), context.getStop().getStopIndex() + 1);
        return start >= end ? "" : source.substring(start, end);
    }

    private boolean containsToken(ParseTree tree, String text) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof TerminalNode && clean(child.getText()).equalsIgnoreCase(text)) return true;
            if (containsToken(child, text)) return true;
        }
        return false;
    }

    private static String compact(String text) { return text == null ? "" : text.replaceAll("\\s+", " ").trim(); }
    private static String clean(String text) { return compact(text); }
    private static String unquote(String text) {
        String value = clean(text);
        if (value.length() >= 2 && ((value.startsWith("'") && value.endsWith("'")) ||
                (value.startsWith("\"") && value.endsWith("\"")))) return value.substring(1, value.length() - 1);
        return value;
    }

    private String firstTerminal(ParseTree tree) {
        if (tree instanceof TerminalNode terminal) return clean(terminal.getText());
        for (int i = 0; i < tree.getChildCount(); i++) {
            String value = firstTerminal(tree.getChild(i));
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static String displayRule(String rule) {
        if (rule == null || rule.isBlank()) return "Section";
        return Character.toUpperCase(rule.charAt(0)) + rule.substring(1).replaceAll("([a-z])([A-Z])", "$1 $2");
    }
}
