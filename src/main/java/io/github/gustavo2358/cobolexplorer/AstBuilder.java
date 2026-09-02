package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolBaseVisitor;
import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/** Builds one immutable semantic AST from the ANTLR parse tree. */
final class AstBuilder extends CobolBaseVisitor<Ast.Node> {
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
    private final CobolParser parser;
    private final UnicodeText indexedSource;
    private final SourceMap sourceMap;
    private final IdentityHashMap<ParseTree, Integer> parseIds;
    private final IdentityHashMap<ParseTree, Integer> parseSubtreeSizes;
    private final List<CoverageDraft> coverageDrafts = new ArrayList<>();
    private final List<SemanticCoverage.Diagnostic> semanticDiagnostics = new ArrayList<>();
    private int nextId;

    private record CoverageDraft(String grammarRule, Ast.Meta meta, String writtenText,
                                 int astNodeId) { }
    private record LogMetrics(int nodes, int unsupportedStatements, int preservedStatements) { }

    @Override
    protected Ast.Node defaultResult() {
        return null;
    }

    @Override
    protected Ast.Node aggregateResult(Ast.Node aggregate, Ast.Node nextResult) {
        if (aggregate != null && nextResult != null) {
            throw new IllegalStateException("Grammar wrapper produced multiple semantic AST results");
        }
        return nextResult != null ? nextResult : aggregate;
    }

    AstBuilder(Parser parser, String source, SourceMap sourceMap,
               IdentityHashMap<ParseTree, Integer> parseIds,
               IdentityHashMap<ParseTree, Integer> parseSubtreeSizes) {
        if (!(parser instanceof CobolParser cobolParser)) {
            throw new IllegalArgumentException("AstBuilder requires the versioned COBOL parser");
        }
        this.parser = cobolParser;
        this.indexedSource = new UnicodeText(source);
        this.sourceMap = sourceMap;
        this.parseIds = parseIds;
        this.parseSubtreeSizes = parseSubtreeSizes;
    }

    AstBuildResult build(ParseTree tree) {
        CobolParser.ProgramUnitContext unit = firstProgramUnit(tree);
        if (unit == null) throw new IllegalStateException("programUnit not found");
        return buildProgramUnit(unit);
    }

    CompilationUnitBuildResult buildCompilationUnit(ParseTree tree, String writtenCompilationUnitId) {
        String compilationUnitId = canonicalName(writtenCompilationUnitId);
        CobolParser.CompilationUnitContext compilation = compilationUnit(tree);
        if (compilation == null) throw new IllegalStateException("compilationUnit not found");
        List<CompilationUnitModel.ProgramUnit> units = new ArrayList<>();
        Map<ResolutionContracts.ProgramUnitId, SemanticCoverage.Report> coverage = new LinkedHashMap<>();
        Map<ResolutionContracts.ProgramUnitId, List<SemanticCoverage.Diagnostic>> diagnostics = new LinkedHashMap<>();
        List<CobolParser.ProgramUnitContext> topLevel = compilation.programUnit();
        for (int index = 0; index < topLevel.size(); index++) {
            collectProgramUnits(topLevel.get(index), List.of(index), null, compilationUnitId,
                    units, coverage, diagnostics);
        }
        CompilationUnitModel model = new CompilationUnitModel(compilationUnitId, units);
        return new CompilationUnitBuildResult(model, coverage, diagnostics);
    }

    private void collectProgramUnits(CobolParser.ProgramUnitContext context, List<Integer> structuralPath,
                                     ResolutionContracts.ProgramUnitId parentId, String compilationUnitId,
                                     List<CompilationUnitModel.ProgramUnit> units,
                                     Map<ResolutionContracts.ProgramUnitId, SemanticCoverage.Report> coverage,
                                     Map<ResolutionContracts.ProgramUnitId, List<SemanticCoverage.Diagnostic>> diagnostics) {
        AstBuildResult built = buildProgramUnit(context);
        ResolutionContracts.ProgramUnitId id = new ResolutionContracts.ProgramUnitId(
                compilationUnitId, structuralPath,
                canonicalName(unquote(built.program().name())));
        units.add(new CompilationUnitModel.ProgramUnit(id, parentId, built.program()));
        coverage.put(id, built.coverage());
        diagnostics.put(id, built.diagnostics());
        List<CobolParser.ProgramUnitContext> nested = context.programUnit();
        for (int index = 0; index < nested.size(); index++) {
            List<Integer> childPath = new ArrayList<>(structuralPath);
            childPath.add(index);
            collectProgramUnits(nested.get(index), childPath, id, compilationUnitId,
                    units, coverage, diagnostics);
        }
    }

    private AstBuildResult buildProgramUnit(CobolParser.ProgramUnitContext unit) {
        long started = System.nanoTime();
        nextId = 0;
        coverageDrafts.clear();
        semanticDiagnostics.clear();
        Ast.Program program = (Ast.Program) visit(unit);
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

    @Override
    public Ast.Node visitProgramUnit(CobolParser.ProgramUnitContext context) {
        Ast.Meta meta = meta(context);
        CobolParser.ProgramIdParagraphContext programId = context.identificationDivision().programIdParagraph();
        List<Ast.Division> divisions = new ArrayList<>();
        divisions.add((Ast.Division) visit(context.identificationDivision()));
        if (context.environmentDivision() != null) divisions.add((Ast.Division) visit(context.environmentDivision()));
        if (context.dataDivision() != null) divisions.add((Ast.Division) visit(context.dataDivision()));
        if (context.procedureDivision() != null) divisions.add((Ast.Division) visit(context.procedureDivision()));
        return new Ast.Program(meta, clean(sourceText(programId.programName())), programAttributes(programId), divisions);
    }

    @Override public Ast.Node visitIdentificationDivision(CobolParser.IdentificationDivisionContext ctx) { return buildIdentification(ctx); }
    @Override public Ast.Node visitEnvironmentDivision(CobolParser.EnvironmentDivisionContext ctx) { return buildEnvironment(ctx); }
    @Override public Ast.Node visitDataDivision(CobolParser.DataDivisionContext ctx) { return buildData(ctx); }
    @Override public Ast.Node visitProcedureDivision(CobolParser.ProcedureDivisionContext ctx) { return buildProcedure(ctx); }

    private static String canonicalName(String writtenName) {
        return writtenName == null ? "" : writtenName.trim().toUpperCase(Locale.ROOT);
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

    private Ast.ProgramAttributes programAttributes(CobolParser.ProgramIdParagraphContext programId) {
        if (programId == null) return Ast.ProgramAttributes.none();
        return new Ast.ProgramAttributes(programId.COMMON() != null,
                programId.INITIAL() != null, programId.RECURSIVE() != null,
                programId.LIBRARY() != null, programId.DEFINITION() != null,
                sourceText(programId).strip());
    }

    private Ast.Division buildIdentification(CobolParser.IdentificationDivisionContext context) {
        Ast.Meta meta = meta(context);
        return new Ast.Division(meta, Ast.DivisionKind.IDENTIFICATION, List.of());
    }

    private Ast.Division buildEnvironment(CobolParser.EnvironmentDivisionContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Node> children = new ArrayList<>();
        for (CobolParser.FileControlEntryContext entry : nearestDescendants(context, CobolParser.FileControlEntryContext.class)) {
            Ast.Meta entryMeta = meta(entry);
            CobolParser.SelectClauseContext select = entry.selectClause();
            ParserRuleContext fileName = select.fileName();
            ParserRuleContext assign = firstDescendant(entry, CobolParser.AssignClauseContext.class);
            children.add(new Ast.FileBinding(entryMeta,
                    fileName == null ? "<unknown>" : clean(sourceText(fileName)),
                    assign == null ? "" : compact(sourceText(assign))));
        }
        return new Ast.Division(meta, Ast.DivisionKind.ENVIRONMENT, children);
    }

    private Ast.Division buildData(CobolParser.DataDivisionContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Node> sections = new ArrayList<>();
        for (CobolParser.DataDivisionSectionContext wrapper : context.dataDivisionSection()) {
            ParserRuleContext sectionContext = dataSectionContext(wrapper);
            Ast.Meta sectionMeta = meta(sectionContext);
            List<Ast.Node> entries = new ArrayList<>();
            if (sectionContext instanceof CobolParser.FileSectionContext fileSection) {
                for (CobolParser.FileDescriptionEntryContext fd : fileSection.fileDescriptionEntry()) {
                    Ast.Meta fdMeta = meta(fd);
                    ParserRuleContext fileName = fd.fileName();
                    List<Ast.DataEntry> dataEntries = new ArrayList<>();
                    dataEntries.addAll(buildDataHierarchy(fd.dataDescriptionEntry()));
                    entries.add(new Ast.FileDescription(fdMeta,
                            fileName == null ? "<unknown>" : clean(sourceText(fileName)),
                            declarationVisibility(fdMeta,
                                    firstDescendant(fd, CobolParser.ExternalClauseContext.class) != null,
                                    firstDescendant(fd, CobolParser.GlobalClauseContext.class) != null), dataEntries));
                }
            } else {
                entries.addAll(buildDataHierarchy(nearestDescendants(sectionContext,
                        CobolParser.DataDescriptionEntryContext.class)));
            }
            sections.add(new Ast.Section(sectionMeta, displayRule(rule(sectionContext)), dataSectionKind(sectionContext), entries));
        }
        return new Ast.Division(meta, Ast.DivisionKind.DATA, sections);
    }

    private static ParserRuleContext dataSectionContext(CobolParser.DataDivisionSectionContext context) {
        if (context.fileSection() != null) return context.fileSection();
        if (context.dataBaseSection() != null) return context.dataBaseSection();
        if (context.workingStorageSection() != null) return context.workingStorageSection();
        if (context.linkageSection() != null) return context.linkageSection();
        if (context.communicationSection() != null) return context.communicationSection();
        if (context.localStorageSection() != null) return context.localStorageSection();
        if (context.screenSection() != null) return context.screenSection();
        if (context.reportSection() != null) return context.reportSection();
        if (context.programLibrarySection() != null) return context.programLibrarySection();
        throw new IllegalStateException("Data division section has no recognized grammar alternative");
    }

    private Ast.DataEntry buildDataEntry(ParserRuleContext wrapper) {
        if (!(wrapper instanceof CobolParser.DataDescriptionEntryContext entryContext)) {
            throw new IllegalArgumentException("Expected data description entry, got " + rule(wrapper));
        }
        ParserRuleContext format = dataEntryFormat(entryContext);
        Ast.Meta meta = meta(format);
        String raw = compact(sourceText(format));
        String level;
        ParserRuleContext name;
        boolean external = false;
        boolean global = false;
        if (format instanceof CobolParser.DataDescriptionEntryFormat1Context value) {
            level = value.INTEGERLITERAL() != null ? value.INTEGERLITERAL().getText() : "77";
            name = value.dataName();
            external = !value.dataExternalClause().isEmpty();
            global = !value.dataGlobalClause().isEmpty();
        } else if (format instanceof CobolParser.DataDescriptionEntryFormat2Context value) {
            level = "66";
            name = value.dataName();
        } else if (format instanceof CobolParser.DataDescriptionEntryFormat3Context value) {
            level = "88";
            name = value.conditionName();
        } else {
            level = "SQL";
            name = null;
        }
        boolean filler = name == null;
        List<Ast.DataClause> clauses = directRuleChildren(format).stream()
                .filter(AstBuilder::isDataClauseContext)
                .map(this::buildDataClause).toList();
        Ast.DataEntry entry = new Ast.DataEntry(meta, level, levelKind(level),
                filler ? "FILLER" : clean(sourceText(name)), filler,
                declarationVisibility(meta, external, global), raw, clauses, List.of());
        recordCoverage(entry, sourceText(format));
        return entry;
    }

    private static ParserRuleContext dataEntryFormat(CobolParser.DataDescriptionEntryContext context) {
        if (context.dataDescriptionEntryFormat1() != null) return context.dataDescriptionEntryFormat1();
        if (context.dataDescriptionEntryFormat2() != null) return context.dataDescriptionEntryFormat2();
        if (context.dataDescriptionEntryFormat3() != null) return context.dataDescriptionEntryFormat3();
        if (context.dataDescriptionEntryExecSql() != null) return context.dataDescriptionEntryExecSql();
        throw new IllegalStateException("Data description entry has no recognized grammar alternative");
    }

    private static final class DataDraft {
        private final Ast.DataEntry entry;
        private final List<DataDraft> children = new ArrayList<>();

        private DataDraft(Ast.DataEntry entry) {
            this.entry = entry;
        }
    }

    private List<Ast.DataEntry> buildDataHierarchy(List<? extends ParserRuleContext> contexts) {
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

    private Ast.DeclarationVisibility declarationVisibility(Ast.Meta declarationMeta,
                                                             boolean external, boolean global) {
        if (external && global) {
            semanticDiagnostics.add(new SemanticCoverage.Diagnostic(
                    "CONFLICTING_DECLARATION_VISIBILITY",
                    "Declaration contains both GLOBAL and EXTERNAL visibility", declarationMeta));
            return Ast.DeclarationVisibility.CONFLICTING;
        }
        if (external) return Ast.DeclarationVisibility.EXTERNAL;
        if (global) return Ast.DeclarationVisibility.GLOBAL;
        return Ast.DeclarationVisibility.LOCAL;
    }

    private Ast.DataClause buildDataClause(ParserRuleContext context) {
        Ast.Node visited = visit(context);
        if (visited instanceof Ast.DataClause dataClause) {
            recordCoverage(dataClause, sourceText(context));
            return dataClause;
        }
        throw new IllegalStateException("Data clause visitor produced no AST node for " + rule(context));
    }

    private Ast.DataClause mapDataClause(ParserRuleContext context) {
        String grammarRule = rule(context);
        String writtenText = sourceText(context).strip();
        Ast.Meta meta = meta(context);
        if (context instanceof CobolParser.DataPictureClauseContext) {
            ParserRuleContext picture = ((CobolParser.DataPictureClauseContext) context).pictureString();
            return new Ast.PictureClause(meta, picture == null ? "" : sourceText(picture).strip(), writtenText);
        }
        if (context instanceof CobolParser.DataUsageClauseContext) {
            String usage = writtenText.replaceFirst("(?i)^USAGE\\s+(IS\\s+)?", "");
            return new Ast.UsageClause(meta, usage, writtenText);
        }
        if (context instanceof CobolParser.DataValueClauseContext) {
            List<String> values = ((CobolParser.DataValueClauseContext) context).dataValueInterval().stream()
                    .map(this::sourceText).map(String::strip).toList();
            return new Ast.ValueClause(meta, values, writtenText);
        }
        if (context instanceof CobolParser.DataRedefinesClauseContext) {
            return new Ast.RedefinesClause(meta,
                    simpleDataReference(((CobolParser.DataRedefinesClauseContext) context).dataName()), writtenText);
        }
        if (context instanceof CobolParser.DataRenamesClauseContext) {
            List<CobolParser.QualifiedDataNameContext> names = nearestDescendants(context,
                    CobolParser.QualifiedDataNameContext.class);
            Ast.DataReference from = (Ast.DataReference) expression(names.get(0), "renames");
            Ast.DataReference through = names.size() > 1
                    ? (Ast.DataReference) expression(names.get(1), "renames through") : null;
            return new Ast.RenamesClause(meta, from, through, writtenText);
        }
        if (context instanceof CobolParser.DataOccursClauseContext) {
            ParserRuleContext first = firstDirectOrNearest(context,
                    child -> child instanceof CobolParser.IdentifierContext
                            || child instanceof CobolParser.IntegerLiteralContext);
            CobolParser.DataOccursClauseContext occurs = (CobolParser.DataOccursClauseContext) context;
            CobolParser.DataOccursToContext to = occurs.dataOccursTo();
            CobolParser.DataOccursDependingContext depending = occurs.dataOccursDepending();
            Ast.Expression minimum = expression(first, "occurs minimum");
            Ast.Expression maximum = to == null ? null
                    : expression(to.integerLiteral(), "occurs maximum");
            Ast.DataReference dependingOn = depending == null ? null
                    : (Ast.DataReference) expression(depending.qualifiedDataName(),
                    "occurs depending");
            List<Ast.DataReference> keys = occurs.dataOccursSort().stream()
                    .flatMap(x -> x.qualifiedDataName().stream())
                    .map(x -> (Ast.DataReference) expression(x, "occurs key"))
                    .toList();
            List<Ast.IndexReference> indexes = occurs.dataOccursIndexed().stream()
                    .flatMap(x -> x.indexName().stream())
                    .map(x -> new Ast.IndexReference(meta(x), clean(sourceText(x)), sourceText(x).strip()))
                    .toList();
            return new Ast.OccursClause(meta, minimum, maximum, dependingOn, keys, indexes, writtenText);
        }
        List<Ast.Node> references = nearestDescendants(context, AstBuilder::isDataClauseReferenceContext).stream()
                .map(node -> node instanceof CobolParser.QualifiedDataNameContext
                        || node instanceof CobolParser.IdentifierContext
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

    private static Ast.DataSectionKind dataSectionKind(ParserRuleContext context) {
        if (context instanceof CobolParser.FileSectionContext) return Ast.DataSectionKind.FILE;
        if (context instanceof CobolParser.DataBaseSectionContext) return Ast.DataSectionKind.DATABASE;
        if (context instanceof CobolParser.WorkingStorageSectionContext) return Ast.DataSectionKind.WORKING_STORAGE;
        if (context instanceof CobolParser.LinkageSectionContext) return Ast.DataSectionKind.LINKAGE;
        if (context instanceof CobolParser.CommunicationSectionContext) return Ast.DataSectionKind.COMMUNICATION;
        if (context instanceof CobolParser.LocalStorageSectionContext) return Ast.DataSectionKind.LOCAL_STORAGE;
        if (context instanceof CobolParser.ScreenSectionContext) return Ast.DataSectionKind.SCREEN;
        if (context instanceof CobolParser.ReportSectionContext) return Ast.DataSectionKind.REPORT;
        if (context instanceof CobolParser.ProgramLibrarySectionContext) return Ast.DataSectionKind.PROGRAM_LIBRARY;
        throw new IllegalArgumentException("Unknown data section context: " + context.getClass().getSimpleName());
    }

    private Ast.Division buildProcedure(CobolParser.ProcedureDivisionContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Node> children = new ArrayList<>();
        Ast.ProcedureSignature signature = buildProcedureSignature(context);
        if (signature != null) children.add(signature);
        CobolParser.ProcedureDivisionBodyContext body = context.procedureDivisionBody();
        if (body != null) {
            children.addAll(buildParagraphGroup(body.paragraphs()));
            for (CobolParser.ProcedureSectionContext section : body.procedureSection()) {
                children.add(buildProcedureSection(section));
            }
        }
        return new Ast.Division(meta, Ast.DivisionKind.PROCEDURE, children);
    }

    private Ast.ProcedureSignature buildProcedureSignature(CobolParser.ProcedureDivisionContext context) {
        CobolParser.ProcedureDivisionUsingClauseContext using = context.procedureDivisionUsingClause();
        CobolParser.ProcedureDivisionGivingClauseContext giving = context.procedureDivisionGivingClause();
        if (using == null && giving == null) return null;

        ParserRuleContext anchor = using != null ? using : giving;
        Ast.Meta meta = meta(anchor);
        List<Ast.ProcedureParameter> parameters = new ArrayList<>();
        if (using != null) {
            for (ParserRuleContext parameter : nearestDescendants(using,
                    AstBuilder::isProcedureParameterContext)) {
                Ast.Meta parameterMeta = meta(parameter);
                boolean any = parameter.getToken(CobolParser.ANY, 0) != null;
                boolean optional = parameter.getToken(CobolParser.OPTIONAL, 0) != null;
                ParserRuleContext value = firstDirectOrNearest(parameter, AstBuilder::isParameterValueContext);
                Ast.PassingMode mode = parameter instanceof CobolParser.ProcedureDivisionByValueContext
                        ? Ast.PassingMode.VALUE : Ast.PassingMode.REFERENCE;
                parameters.add(new Ast.ProcedureParameter(parameterMeta, mode,
                        any ? null : expression(value, "procedure parameter"), optional, any,
                        sourceText(parameter).strip()));
            }
        }
        Ast.DataReference returning = giving == null ? null
                : simpleDataReference(giving.dataName());
        String writtenText = sourceText(anchor).strip()
                + (giving == null ? "" : " " + sourceText(giving).strip());
        return new Ast.ProcedureSignature(meta, using != null && using.CHAINING() != null,
                parameters, returning, writtenText);
    }

    private Ast.Section buildProcedureSection(CobolParser.ProcedureSectionContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext name = context.procedureSectionHeader().sectionName();
        CobolParser.ParagraphsContext paragraphs = context.paragraphs();
        return new Ast.Section(meta, name == null ? "<section>" : clean(sourceText(name)),
                paragraphs == null ? List.of() : buildParagraphGroup(paragraphs));
    }

    private List<Ast.Node> buildParagraphGroup(CobolParser.ParagraphsContext context) {
        List<Ast.Node> result = new ArrayList<>();
        List<CobolParser.SentenceContext> leading = context.sentence();
        if (!leading.isEmpty()) {
            Ast.Meta syntheticMeta = meta(context);
            List<Ast.Sentence> sentences = leading.stream().map(this::buildSentence).toList();
            result.add(new Ast.Paragraph(syntheticMeta, "<entry>", sentences));
        }
        for (CobolParser.ParagraphContext paragraph : context.paragraph()) result.add(buildParagraph(paragraph));
        return result;
    }

    private Ast.Paragraph buildParagraph(CobolParser.ParagraphContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext name = context.paragraphName();
        List<Ast.Sentence> sentences = context.sentence().stream().map(this::buildSentence).toList();
        return new Ast.Paragraph(meta, name == null ? "<paragraph>" : clean(sourceText(name)), sentences);
    }

    private Ast.Sentence buildSentence(CobolParser.SentenceContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Statement> statements = context.statement().stream().map(this::buildStatement).toList();
        Token stop = context.getStop();
        Ast.SourceSpan terminator = stop == null ? meta.span() : new Ast.SourceSpan(stop.getLine(), stop.getCharPositionInLine(),
                stop.getLine(), stop.getCharPositionInLine() + Math.max(0,
                        stop.getText().codePointCount(0, stop.getText().length()) - 1),
                stop.getTokenIndex(), stop.getTokenIndex());
        return new Ast.Sentence(meta, statements, Ast.SentenceTerminator.PERIOD, terminator);
    }

    private Ast.Statement buildStatement(ParserRuleContext wrapper) {
        if (!(wrapper instanceof CobolParser.StatementContext statementContext)) {
            throw new IllegalArgumentException("Expected statement context, got " + rule(wrapper));
        }
        Ast.Node visited = visit(statementContext);
        if (!(visited instanceof Ast.Statement statement)) {
            throw new IllegalStateException("Statement visitor produced no AST node for " + sourceText(wrapper));
        }
        String grammarRule = statement.meta().origin().grammarRule();
        recordCoverage(statement, sourceText(wrapper));
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

    private Ast.Node modeled(ParserRuleContext context) {
        return buildStructuredStatement(context, false);
    }

    private Ast.Node preserved(ParserRuleContext context) {
        return buildStructuredStatement(context, true);
    }

    @Override public Ast.Node visitAcceptStatement(CobolParser.AcceptStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitAddStatement(CobolParser.AddStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitAlterStatement(CobolParser.AlterStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitCallStatement(CobolParser.CallStatementContext ctx) { return buildCall(ctx); }
    @Override public Ast.Node visitCancelStatement(CobolParser.CancelStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitCloseStatement(CobolParser.CloseStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitComputeStatement(CobolParser.ComputeStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitContinueStatement(CobolParser.ContinueStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitDeleteStatement(CobolParser.DeleteStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitDisableStatement(CobolParser.DisableStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitDisplayStatement(CobolParser.DisplayStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitDivideStatement(CobolParser.DivideStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitEnableStatement(CobolParser.EnableStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitEntryStatement(CobolParser.EntryStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitEvaluateStatement(CobolParser.EvaluateStatementContext ctx) { return buildEvaluate(ctx); }
    @Override public Ast.Node visitExhibitStatement(CobolParser.ExhibitStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitExecCicsStatement(CobolParser.ExecCicsStatementContext ctx) { return buildEmbedded(ctx, Ast.EmbeddedLanguage.CICS); }
    @Override public Ast.Node visitExecSqlStatement(CobolParser.ExecSqlStatementContext ctx) { return buildEmbedded(ctx, Ast.EmbeddedLanguage.SQL); }
    @Override public Ast.Node visitExecSqlImsStatement(CobolParser.ExecSqlImsStatementContext ctx) { return buildEmbedded(ctx, Ast.EmbeddedLanguage.SQLIMS); }
    @Override public Ast.Node visitExitStatement(CobolParser.ExitStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitGenerateStatement(CobolParser.GenerateStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitGobackStatement(CobolParser.GobackStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitGoToStatement(CobolParser.GoToStatementContext ctx) { return buildGoTo(ctx); }
    @Override public Ast.Node visitIfStatement(CobolParser.IfStatementContext ctx) { return buildIf(ctx); }
    @Override public Ast.Node visitInitializeStatement(CobolParser.InitializeStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitInitiateStatement(CobolParser.InitiateStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitInspectStatement(CobolParser.InspectStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitMergeStatement(CobolParser.MergeStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitMoveStatement(CobolParser.MoveStatementContext ctx) { return buildMove(ctx); }
    @Override public Ast.Node visitMultiplyStatement(CobolParser.MultiplyStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitNextSentenceStatement(CobolParser.NextSentenceStatementContext ctx) { return new Ast.NextSentenceStatement(meta(ctx)); }
    @Override public Ast.Node visitOpenStatement(CobolParser.OpenStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitPerformStatement(CobolParser.PerformStatementContext ctx) { return buildPerform(ctx); }
    @Override public Ast.Node visitPurgeStatement(CobolParser.PurgeStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitReadStatement(CobolParser.ReadStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitReceiveStatement(CobolParser.ReceiveStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitReleaseStatement(CobolParser.ReleaseStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitReturnStatement(CobolParser.ReturnStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitRewriteStatement(CobolParser.RewriteStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitSearchStatement(CobolParser.SearchStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitSendStatement(CobolParser.SendStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitSetStatement(CobolParser.SetStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitSortStatement(CobolParser.SortStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitStartStatement(CobolParser.StartStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitStopStatement(CobolParser.StopStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitStringStatement(CobolParser.StringStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitSubtractStatement(CobolParser.SubtractStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitTerminateStatement(CobolParser.TerminateStatementContext ctx) { return preserved(ctx); }
    @Override public Ast.Node visitUnstringStatement(CobolParser.UnstringStatementContext ctx) { return modeled(ctx); }
    @Override public Ast.Node visitWriteStatement(CobolParser.WriteStatementContext ctx) { return modeled(ctx); }

    @Override public Ast.Node visitIdentifier(CobolParser.IdentifierContext ctx) { return identifierExpression(ctx); }
    @Override public Ast.Node visitFileName(CobolParser.FileNameContext ctx) { return new Ast.FileReference(meta(ctx), clean(sourceText(ctx)), sourceText(ctx).strip()); }
    @Override public Ast.Node visitIndexName(CobolParser.IndexNameContext ctx) { return new Ast.IndexReference(meta(ctx), clean(sourceText(ctx)), sourceText(ctx).strip()); }
    @Override public Ast.Node visitQualifiedDataName(CobolParser.QualifiedDataNameContext ctx) { return dataReference(ctx); }
    @Override public Ast.Node visitConditionNameReference(CobolParser.ConditionNameReferenceContext ctx) { return dataReference(ctx); }
    @Override public Ast.Node visitTableCall(CobolParser.TableCallContext ctx) { return tableReference(ctx); }
    @Override public Ast.Node visitFunctionCall(CobolParser.FunctionCallContext ctx) { return functionExpression(ctx); }
    @Override public Ast.Node visitSpecialRegister(CobolParser.SpecialRegisterContext ctx) { return specialRegisterExpression(ctx); }
    @Override public Ast.Node visitArithmeticExpression(CobolParser.ArithmeticExpressionContext ctx) { return arithmeticExpression(ctx); }
    @Override public Ast.Node visitMultDivs(CobolParser.MultDivsContext ctx) { return arithmeticExpression(ctx); }
    @Override public Ast.Node visitPowers(CobolParser.PowersContext ctx) { return arithmeticExpression(ctx); }
    @Override public Ast.Node visitBasis(CobolParser.BasisContext ctx) { return arithmeticExpression(ctx); }
    @Override public Ast.Node visitCondition(CobolParser.ConditionContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitCombinableCondition(CobolParser.CombinableConditionContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitSimpleCondition(CobolParser.SimpleConditionContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitRelationCondition(CobolParser.RelationConditionContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitRelationSignCondition(CobolParser.RelationSignConditionContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitRelationArithmeticComparison(CobolParser.RelationArithmeticComparisonContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitRelationCombinedComparison(CobolParser.RelationCombinedComparisonContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitRelationCombinedCondition(CobolParser.RelationCombinedConditionContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitClassCondition(CobolParser.ClassConditionContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitAbbreviation(CobolParser.AbbreviationContext ctx) { return conditionExpression(ctx); }
    @Override public Ast.Node visitSubscript(CobolParser.SubscriptContext ctx) { return subscriptExpression(ctx); }
    @Override public Ast.Node visitEvaluateSelect(CobolParser.EvaluateSelectContext ctx) { return expressionWrapper(ctx, "evaluate select"); }
    @Override public Ast.Node visitEvaluateValue(CobolParser.EvaluateValueContext ctx) { return expressionWrapper(ctx, "evaluate value"); }
    @Override public Ast.Node visitEvaluateCondition(CobolParser.EvaluateConditionContext ctx) { return expressionWrapper(ctx, "evaluate condition"); }
    @Override public Ast.Node visitArgument(CobolParser.ArgumentContext ctx) { return expressionWrapper(ctx, "argument"); }
    @Override public Ast.Node visitCharacterPosition(CobolParser.CharacterPositionContext ctx) { return expressionWrapper(ctx, "character position"); }
    @Override public Ast.Node visitLength(CobolParser.LengthContext ctx) { return expressionWrapper(ctx, "length"); }
    @Override public Ast.Node visitPerformFrom(CobolParser.PerformFromContext ctx) { return expressionWrapper(ctx, "perform from"); }
    @Override public Ast.Node visitPerformBy(CobolParser.PerformByContext ctx) { return expressionWrapper(ctx, "perform by"); }
    @Override public Ast.Node visitLiteral(CobolParser.LiteralContext ctx) { return literalExpression(ctx); }
    @Override public Ast.Node visitIntegerLiteral(CobolParser.IntegerLiteralContext ctx) { return literalExpression(ctx); }
    @Override public Ast.Node visitNumericLiteral(CobolParser.NumericLiteralContext ctx) { return literalExpression(ctx); }
    @Override public Ast.Node visitBooleanLiteral(CobolParser.BooleanLiteralContext ctx) { return literalExpression(ctx); }
    @Override public Ast.Node visitCicsDfhRespLiteral(CobolParser.CicsDfhRespLiteralContext ctx) { return literalExpression(ctx); }
    @Override public Ast.Node visitCicsDfhValueLiteral(CobolParser.CicsDfhValueLiteralContext ctx) { return literalExpression(ctx); }
    @Override public Ast.Node visitDataAlignedClause(CobolParser.DataAlignedClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataBlankWhenZeroClause(CobolParser.DataBlankWhenZeroClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataCommonOwnLocalClause(CobolParser.DataCommonOwnLocalClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataExternalClause(CobolParser.DataExternalClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataGlobalClause(CobolParser.DataGlobalClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataIntegerStringClause(CobolParser.DataIntegerStringClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataJustifiedClause(CobolParser.DataJustifiedClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataOccursClause(CobolParser.DataOccursClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataPictureClause(CobolParser.DataPictureClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataReceivedByClause(CobolParser.DataReceivedByClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataRecordAreaClause(CobolParser.DataRecordAreaClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataRedefinesClause(CobolParser.DataRedefinesClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataRenamesClause(CobolParser.DataRenamesClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataSignClause(CobolParser.DataSignClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataSynchronizedClause(CobolParser.DataSynchronizedClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataThreadLocalClause(CobolParser.DataThreadLocalClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataTypeClause(CobolParser.DataTypeClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataTypeDefClause(CobolParser.DataTypeDefClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataUsageClause(CobolParser.DataUsageClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataUsingClause(CobolParser.DataUsingClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataValueClause(CobolParser.DataValueClauseContext ctx) { return mapDataClause(ctx); }
    @Override public Ast.Node visitDataWithLowerBoundsClause(CobolParser.DataWithLowerBoundsClauseContext ctx) { return mapDataClause(ctx); }

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

    private void recordCoverage(Ast.Node node, String writtenText) {
        Ast.Meta meta = node.meta();
        coverageDrafts.add(new CoverageDraft(meta.origin().grammarRule(), meta, writtenText, meta.id()));
    }

    private Ast.CallStatement buildCall(CobolParser.CallStatementContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext targetContext = context.identifier() != null ? context.identifier() : context.literal();
        Ast.Expression target = targetContext instanceof CobolParser.LiteralContext
                ? new Ast.ProgramReference(meta(targetContext), unquote(sourceText(targetContext).strip()), sourceText(targetContext).strip())
                : expression(targetContext, "call target");
        Ast.CallTargetSyntax kind = target instanceof Ast.ProgramReference
                ? Ast.CallTargetSyntax.LITERAL_PROGRAM_NAME : Ast.CallTargetSyntax.IDENTIFIER_OR_EXPRESSION;
        List<Ast.CallArgument> arguments = new ArrayList<>();
        for (ParserRuleContext arg : nearestDescendants(context, AstBuilder::isCallArgumentContext)) {
            Ast.PassingMode mode = arg instanceof CobolParser.CallByValueContext ? Ast.PassingMode.VALUE
                    : arg instanceof CobolParser.CallByContentContext ? Ast.PassingMode.CONTENT
                    : Ast.PassingMode.REFERENCE;
            Ast.Meta argMeta = meta(arg);
            Ast.CallArgumentKind argumentKind = arg.getToken(CobolParser.OMITTED, 0) != null
                    ? Ast.CallArgumentKind.OMITTED : arg.getToken(CobolParser.ADDRESS, 0) != null
                    ? Ast.CallArgumentKind.ADDRESS_OF : arg.getToken(CobolParser.LENGTH, 0) != null
                    ? Ast.CallArgumentKind.LENGTH_OF : Ast.CallArgumentKind.VALUE;
            ParserRuleContext value = firstDirectOrNearest(arg, AstBuilder::isParameterValueContext);
            arguments.add(new Ast.CallArgument(argMeta, mode, argumentKind,
                    argumentKind == Ast.CallArgumentKind.OMITTED ? null : expression(value, "argument"),
                    sourceText(arg).strip()));
        }
        CobolParser.CallGivingPhraseContext giving = context.callGivingPhrase();
        Ast.Expression returning = giving == null ? null
                : expression(giving.identifier(), "call returning");
        return new Ast.CallStatement(meta, kind, target, arguments, returning, directNestedStatements(context));
    }

    private Ast.Statement buildStructuredStatement(ParserRuleContext context, boolean preserved) {
        Ast.Meta meta = meta(context);
        List<Ast.StatementOperand> operands = new ArrayList<>();
        collectStatementOperands(context, context, operands);
        List<Ast.StatementClause> clauses = nearestDescendants(context, AstBuilder::isFlowClauseContext).stream()
                .map(this::buildStatementClause).toList();
        return preserved
                ? new Ast.PreservedStatement(meta, rule(context), sourceText(context).strip(), operands, clauses)
                : new Ast.ModeledStatement(meta, rule(context), sourceText(context).strip(), operands, clauses);
    }

    private void collectStatementOperands(ParserRuleContext root, ParserRuleContext context,
                                          List<Ast.StatementOperand> output) {
        for (ParserRuleContext child : directRuleChildren(context)) {
            if (child != root && child instanceof CobolParser.StatementContext) continue;
            if (isStatementOperandContext(child)) {
                Ast.Meta operandMeta = meta(child);
                output.add(new Ast.StatementOperand(operandMeta, rule(context),
                        statementOperandContext(root, context), statementOperand(child)));
            } else {
                collectStatementOperands(root, child, output);
            }
        }
    }

    private Ast.Node statementOperand(ParserRuleContext context) {
        if (context instanceof CobolParser.IdentifierContext
                || context instanceof CobolParser.QualifiedDataNameContext)
            return expression(context, "statement operand");
        if (context instanceof CobolParser.ProcedureNameContext) return procedureReference(context);
        if (context instanceof CobolParser.FileNameContext)
            return new Ast.FileReference(meta(context), clean(sourceText(context)), sourceText(context).strip());
        if (context instanceof CobolParser.IndexNameContext)
            return new Ast.IndexReference(meta(context), clean(sourceText(context)), sourceText(context).strip());
        if (isLiteralContext(context)) return literalExpression(context);
        return new Ast.NamedReference(meta(context), rule(context), sourceText(context).strip());
    }

    private static Ast.StatementOperandContext statementOperandContext(ParserRuleContext root,
                                                                        ParserRuleContext parent) {
        if (!(root instanceof CobolParser.SetStatementContext)) return Ast.StatementOperandContext.DEFAULT;
        if (parent instanceof CobolParser.SetToContext setTo) {
            ParserRuleContext statement = setTo.getParent();
            if (statement instanceof CobolParser.SetToStatementContext setToStatement
                    && hasBooleanSetValue(setToStatement))
                return Ast.StatementOperandContext.SET_CONDITION_TARGET;
            return Ast.StatementOperandContext.SET_DATA_OR_INDEX;
        }
        return parent instanceof CobolParser.SetToValueContext
                ? Ast.StatementOperandContext.SET_DATA_OR_INDEX : Ast.StatementOperandContext.DEFAULT;
    }

    private static boolean hasBooleanSetValue(CobolParser.SetToStatementContext context) {
        return context.setToValue().size() == 1
                && context.setToValue(0).literal() != null
                && context.setToValue(0).literal().booleanLiteral() != null;
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

    private Ast.IfStatement buildIf(CobolParser.IfStatementContext context) {
        Ast.Meta meta = meta(context);
        return new Ast.IfStatement(meta, expression(context.condition(), "condition"),
                statementsInside(context.ifThen()), statementsInside(context.ifElse()), context.END_IF() != null);
    }

    private Ast.EvaluateStatement buildEvaluate(CobolParser.EvaluateStatementContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Expression> subjects = new ArrayList<>();
        subjects.add(expression(context.evaluateSelect(), "subject"));
        for (CobolParser.EvaluateAlsoSelectContext also : context.evaluateAlsoSelect())
            subjects.add(expression(also.evaluateSelect(), "subject"));
        List<CobolParser.EvaluateSelectContext> subjectContexts = new ArrayList<>();
        subjectContexts.add(context.evaluateSelect());
        subjectContexts.addAll(context.evaluateAlsoSelect().stream().map(CobolParser.EvaluateAlsoSelectContext::evaluateSelect).toList());
        List<Ast.EvaluateBranch> branches = new ArrayList<>();
        for (CobolParser.EvaluateWhenPhraseContext branch : context.evaluateWhenPhrase()) {
            Ast.Meta branchMeta = meta(branch);
            List<Ast.EvaluateSelector> selectors = new ArrayList<>();
            for (CobolParser.EvaluateWhenContext when : branch.evaluateWhen()) {
                CobolParser.EvaluateConditionContext condition = when.evaluateCondition();
                if (condition != null) selectors.add(evaluateSelector(condition, 0, subjectContexts));
                int subjectIndex = 1;
                for (CobolParser.EvaluateAlsoConditionContext also : when.evaluateAlsoCondition()) {
                    CobolParser.EvaluateConditionContext alsoCondition = also.evaluateCondition();
                    if (alsoCondition != null) selectors.add(evaluateSelector(alsoCondition, subjectIndex, subjectContexts));
                    subjectIndex++;
                }
            }
            branches.add(new Ast.EvaluateBranch(branchMeta, selectors,
                    branch.evaluateWhen().stream()
                            .map(this::sourceText).map(AstBuilder::compact).reduce((a, b) -> a + " " + b).orElse(""), false,
                    branch.statement().stream().map(this::buildStatement).toList()));
        }
        CobolParser.EvaluateWhenOtherContext other = context.evaluateWhenOther();
        if (other != null) branches.add(new Ast.EvaluateBranch(meta(other), List.of(), "OTHER", true, statementsInside(other)));
        return new Ast.EvaluateStatement(meta, subjects, branches, context.END_EVALUATE() != null);
    }

    private Ast.EvaluateSelector evaluateSelector(CobolParser.EvaluateConditionContext condition,
                                                   int subjectIndex,
                                                   List<CobolParser.EvaluateSelectContext> subjects) {
        Ast.Expression expression = expression(condition, "evaluate selector");
        boolean correspondingBooleanSubject = subjectIndex < subjects.size()
                && subjects.get(subjectIndex).literal() != null
                && subjects.get(subjectIndex).literal().booleanLiteral() != null;
        boolean directNominalValue = condition.evaluateValue() != null
                && condition.evaluateValue().identifier() != null
                && condition.evaluateThrough() == null;
        Ast.EvaluateSelectorContext selectorContext = !directNominalValue || subjectIndex >= subjects.size()
                ? Ast.EvaluateSelectorContext.OTHER
                : correspondingBooleanSubject ? Ast.EvaluateSelectorContext.BOOLEAN_SUBJECT_NOMINAL
                : Ast.EvaluateSelectorContext.VALUE_COMPARISON;
        return new Ast.EvaluateSelector(expression, subjectIndex, selectorContext);
    }

    private Ast.PerformStatement buildPerform(CobolParser.PerformStatementContext context) {
        Ast.Meta meta = meta(context);
        CobolParser.PerformInlineStatementContext inline = context.performInlineStatement();
        CobolParser.PerformProcedureStatementContext procedure = context.performProcedureStatement();
        CobolParser.PerformTypeContext type = inline != null ? inline.performType() : procedure.performType();
        if (inline != null) {
            List<Ast.Expression> controls = type == null ? List.of() : controlExpressions(type);
            return new Ast.PerformStatement(meta, Ast.PerformKind.INLINE, null, null,
                    type == null ? "once" : compact(sourceText(type)), controls, statementsInside(inline));
        }
        List<CobolParser.ProcedureNameContext> names = procedure == null ? List.of()
                : nearestDescendants(procedure, CobolParser.ProcedureNameContext.class);
        Ast.ProcedureReference fromReference = names.isEmpty() ? null : procedureReference(names.get(0));
        Ast.ProcedureReference throughReference = names.size() < 2 ? null : procedureReference(names.get(1));
        List<Ast.Expression> controls = type == null ? List.of() : controlExpressions(type);
        return new Ast.PerformStatement(meta, Ast.PerformKind.PROCEDURE,
                fromReference, throughReference,
                type == null ? "once" : compact(sourceText(type)), controls, List.of());
    }

    private Ast.GoToStatement buildGoTo(CobolParser.GoToStatementContext context) {
        Ast.Meta meta = meta(context);
        CobolParser.GoToDependingOnStatementContext depending = context.goToDependingOnStatement();
        List<Ast.ProcedureReference> targets = nearestDescendants(context, CobolParser.ProcedureNameContext.class).stream()
                .map(this::procedureReference).toList();
        ParserRuleContext selector = depending == null ? null : depending.identifier();
        return new Ast.GoToStatement(meta, depending == null ? Ast.GoToKind.SIMPLE : Ast.GoToKind.DEPENDING_ON,
                targets, selector == null ? null : expression(selector, "selector"));
    }

    private Ast.MoveStatement buildMove(CobolParser.MoveStatementContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext sending = firstDirectOrNearest(context,
                child -> child instanceof CobolParser.MoveToSendingAreaContext
                        || child instanceof CobolParser.MoveCorrespondingToSendingAreaContext);
        List<CobolParser.IdentifierContext> identifiers = nearestDescendants(context, CobolParser.IdentifierContext.class);
        Ast.Expression sourceExpression = expression(firstDirectOrNearest(sending,
                child -> child instanceof CobolParser.IdentifierContext
                        || child instanceof CobolParser.LiteralContext), "source");
        int sourceToken = sourceExpression.meta().span().startToken();
        List<Ast.Expression> targets = identifiers.stream().filter(i -> i.getStart().getTokenIndex() != sourceToken)
                .map(i -> expression(i, "target")).toList();
        return new Ast.MoveStatement(meta, sourceExpression, targets,
                context.moveCorrespondingToStatement() != null);
    }

    private Ast.EmbeddedLanguageStatement buildEmbedded(ParserRuleContext context, Ast.EmbeddedLanguage language) {
        return new Ast.EmbeddedLanguageStatement(meta(context), language, sourceText(context).strip());
    }

    private Ast.Expression expression(ParserRuleContext context, String role) {
        if (context == null) return new Ast.RawExpression(syntheticMeta(role), role, "<missing>");
        if (isVisitorExpressionContext(context)) {
            Ast.Node visited = visit(context);
            if (visited instanceof Ast.Expression result) return result;
            throw new IllegalStateException("Expression visitor produced no AST node for " + rule(context));
        }
        ParserRuleContext exact = firstDirectOrNearest(context, AstBuilder::isRecognizedExpressionContext);
        if (exact != null && compact(sourceText(context)).equals(compact(sourceText(exact))))
            return expression(exact, role);
        Ast.Meta meta = meta(context);
        List<Ast.Expression> recognized = nearestDescendants(context, AstBuilder::isRecognizedExpressionContext).stream()
                .map(child -> expression(child, role)).toList();
        Ast.PreservedExpression expression = new Ast.PreservedExpression(meta, rule(context),
                sourceText(context).strip(), recognized, Ast.ReferenceUnderstanding.PRESERVED);
        recordCoverage(expression, sourceText(context));
        return expression;
    }

    private Ast.Expression expressionWrapper(ParserRuleContext context, String role) {
        ParserRuleContext child = firstDirectOrNearest(context, AstBuilder::isExpressionWrapperValueContext);
        return child == null ? preservedExpression(context, role) : expression(child, role);
    }

    private Ast.Expression subscriptExpression(CobolParser.SubscriptContext context) {
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
        return nearestDescendants(performType, AstBuilder::isRecognizedExpressionContext).stream()
                .map(context -> expression(context, "perform control")).toList();
    }

    private Ast.Expression identifierExpression(CobolParser.IdentifierContext identifier) {
        if (identifier.qualifiedDataName() != null) return dataReference(identifier.qualifiedDataName());
        if (identifier.tableCall() != null) return tableReference(identifier.tableCall());
        if (identifier.functionCall() != null) return functionExpression(identifier.functionCall());
        if (identifier.specialRegister() != null) return specialRegisterExpression(identifier.specialRegister());
        return preservedExpression(identifier, "identifier");
    }

    private Ast.DataReference dataReference(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext qualified = context instanceof CobolParser.QualifiedDataNameContext ? context
                : firstDescendant(context, CobolParser.QualifiedDataNameContext.class);
        ParserRuleContext base = qualified == null ? firstDirectOrNearest(context, AstBuilder::isReferenceBaseContext)
                : firstDirectOrNearest(qualified, AstBuilder::isReferenceBaseContext);
        String baseName = base == null ? firstSemanticWord(context) : clean(sourceText(base));
        List<Ast.DataQualifier> qualifiers = buildQualifiers(qualified == null ? context : qualified);
        return new Ast.DataReference(meta, baseName, sourceText(context).strip(), qualifiers, List.of(), null,
                Ast.ReferenceUnderstanding.STRUCTURED);
    }

    private Ast.DataReference tableReference(CobolParser.TableCallContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext qualified = context.qualifiedDataName();
        ParserRuleContext base = firstDirectOrNearest(qualified, AstBuilder::isReferenceBaseContext);
        List<Ast.DataQualifier> qualifiers = qualified == null ? List.of() : buildQualifiers(qualified);
        List<Ast.SubscriptGroup> groups = new ArrayList<>();
        List<ParserRuleContext> current = null;
        for (int i = 0; i < context.getChildCount(); i++) {
            ParseTree child = context.getChild(i);
            if (child instanceof CobolParser.ReferenceModifierContext) break;
            if (child instanceof TerminalNode terminal && terminal.getText().equals("(")) current = new ArrayList<>();
            else if (child instanceof CobolParser.SubscriptContext ruleContext && current != null)
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
        ParserRuleContext modifier = context.referenceModifier();
        Ast.ReferenceModification referenceModification = modifier == null ? null : referenceModification(modifier);
        return new Ast.DataReference(meta, base == null ? firstSemanticWord(context) : clean(sourceText(base)),
                sourceText(context).strip(), qualifiers, groups, referenceModification,
                Ast.ReferenceUnderstanding.STRUCTURED);
    }

    private List<Ast.DataQualifier> buildQualifiers(ParserRuleContext qualified) {
        List<ParserRuleContext> contexts = nearestDescendants(qualified, AstBuilder::isQualifierContext);
        List<Ast.DataQualifier> result = new ArrayList<>();
        boolean generalFormat = qualified instanceof CobolParser.QualifiedDataNameFormat1Context
                || qualified instanceof CobolParser.QualifiedDataNameContext name
                && name.qualifiedDataNameFormat1() != null;
        for (int i = 0; i < contexts.size(); i++) {
            ParserRuleContext qualifier = contexts.get(i);
            String written = sourceText(qualifier).strip();
            Ast.QualifierConnector connector = qualifier.getToken(CobolParser.IN, 0) != null
                    ? Ast.QualifierConnector.IN : Ast.QualifierConnector.OF;
            Ast.QualifierTarget target = qualifier instanceof CobolParser.InFileContext ? Ast.QualifierTarget.FILE
                    : qualifier instanceof CobolParser.InDataContext && generalFormat && i == contexts.size() - 1
                    ? Ast.QualifierTarget.DATA_OR_FILE : Ast.QualifierTarget.DATA;
            Ast.Meta qualifierMeta = meta(qualifier);
            ParserRuleContext value = directRuleChildren(qualifier).stream().reduce((first, second) -> second).orElse(null);
            Ast.DataReference reference;
            if (value instanceof CobolParser.TableCallContext tableCall) reference = tableReference(tableCall);
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
        CobolParser.ReferenceModifierContext referenceModifier = (CobolParser.ReferenceModifierContext) modifier;
        ParserRuleContext offset = referenceModifier.characterPosition();
        ParserRuleContext length = referenceModifier.length();
        return new Ast.ReferenceModification(meta, expression(offset, "reference offset"),
                length == null ? null : expression(length, "reference length"), sourceText(modifier).strip());
    }

    private Ast.FunctionExpression functionExpression(CobolParser.FunctionCallContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext name = context.functionName();
        List<Ast.Expression> arguments = context.argument().stream()
                .map(argument -> expression(argument, "function argument")).toList();
        ParserRuleContext modifier = context.referenceModifier();
        return new Ast.FunctionExpression(meta, name == null ? "<unknown>" : clean(sourceText(name)), arguments,
                modifier == null ? null : referenceModification(modifier), sourceText(context).strip());
    }

    private Ast.SpecialRegisterExpression specialRegisterExpression(CobolParser.SpecialRegisterContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Expression> operands = context.identifier() == null
                ? List.of() : List.of(identifierExpression(context.identifier()));
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
        if (context instanceof CobolParser.ArithmeticExpressionContext arithmetic) {
            if (arithmetic.plusMinus().isEmpty()) return expression(arithmetic.multDivs(), "arithmetic operand");
            Ast.Meta meta = meta(context);
            List<Ast.Expression> operands = new ArrayList<>();
            operands.add(expression(arithmetic.multDivs(), "arithmetic operand"));
            String operator = null;
            for (CobolParser.PlusMinusContext item : arithmetic.plusMinus()) {
                String itemOperator = item.PLUSCHAR() != null ? "+" : "-";
                operator = operator == null ? itemOperator : operator.equals(itemOperator) ? operator : "MIXED_ARITHMETIC";
                operands.add(expression(item.multDivs(), "arithmetic operand"));
            }
            return new Ast.OperationExpression(meta, operator, operands, sourceText(context).strip());
        }
        if (context instanceof CobolParser.MultDivsContext multDivs) {
            if (multDivs.multDiv().isEmpty()) return expression(multDivs.powers(), "arithmetic operand");
            Ast.Meta meta = meta(context);
            List<Ast.Expression> operands = new ArrayList<>();
            operands.add(expression(multDivs.powers(), "arithmetic operand"));
            String operator = null;
            for (CobolParser.MultDivContext item : multDivs.multDiv()) {
                String itemOperator = item.ASTERISKCHAR() != null ? "*" : "/";
                operator = operator == null ? itemOperator : operator.equals(itemOperator) ? operator : "MIXED_ARITHMETIC";
                operands.add(expression(item.powers(), "arithmetic operand"));
            }
            return new Ast.OperationExpression(meta, operator, operands, sourceText(context).strip());
        }
        if (context instanceof CobolParser.PowersContext powersContext) {
            List<CobolParser.PowerContext> powers = powersContext.power();
            ParserRuleContext basis = powersContext.basis();
            String text = sourceText(context).strip();
            if (!powers.isEmpty()) {
                Ast.Meta meta = meta(context);
                List<Ast.Expression> operands = new ArrayList<>();
                operands.add(expression(basis, "power base"));
                for (CobolParser.PowerContext power : powers) operands.add(expression(power.basis(), "exponent"));
                return new Ast.OperationExpression(meta, "**", operands, text);
            }
            if (powersContext.PLUSCHAR() != null || powersContext.MINUSCHAR() != null)
                return new Ast.OperationExpression(meta(context), powersContext.PLUSCHAR() != null ? "+" : "-",
                        List.of(expression(basis, "unary operand")), text);
            return expression(basis, "arithmetic basis");
        }
        if (context instanceof CobolParser.BasisContext basis) {
            if (basis.arithmeticExpression() != null)
                return new Ast.OperationExpression(meta(context), "GROUP",
                        List.of(expression(basis.arithmeticExpression(), "grouped arithmetic")), sourceText(context).strip());
            ParserRuleContext child = basis.identifier() != null ? basis.identifier() : basis.literal();
            return expression(child, "arithmetic value");
        }
        return preservedExpression(context, "arithmetic");
    }

    private Ast.Expression conditionExpression(ParserRuleContext context) {
        return buildConditionSurface(context, false).node();
    }

    /**
     * Surface result of one condition fragment. {@code abbreviationOpen} records only
     * whether a structurally open abbreviation state exists at the end of the fragment;
     * it is used exclusively to choose the surface shape of bare nominals and is never
     * materialized as inherited subject/operator (that belongs to the future
     * post-binding projector).
     */
    private record ConditionBuild(Ast.Expression node, boolean abbreviationOpen) { }

    private ConditionBuild buildConditionSurface(ParserRuleContext context, boolean abbreviationOpen) {
        if (context instanceof CobolParser.ConditionContext condition)
            return buildCondition(condition, abbreviationOpen);
        if (context instanceof CobolParser.CombinableConditionContext combinable)
            return buildCombinable(combinable, abbreviationOpen);
        if (context instanceof CobolParser.SimpleConditionContext simple)
            return buildSimple(simple, abbreviationOpen);
        if (context instanceof CobolParser.RelationConditionContext relation)
            return buildRelationCondition(relation);
        if (context instanceof CobolParser.RelationArithmeticComparisonContext comparison)
            return buildArithmeticComparison(comparison);
        if (context instanceof CobolParser.RelationCombinedComparisonContext combined)
            return buildCombinedComparison(combined);
        if (context instanceof CobolParser.RelationSignConditionContext sign)
            return buildSignCondition(sign);
        if (context instanceof CobolParser.ClassConditionContext classCondition)
            return buildClassCondition(classCondition);
        if (context instanceof CobolParser.ConditionNameReferenceContext name)
            return buildBareNominal(name, abbreviationOpen);
        if (context instanceof CobolParser.AbbreviationContext abbreviation)
            return buildAbbreviation(abbreviation);
        return new ConditionBuild(preservedExpression(context, "condition"), false);
    }

    private ConditionBuild buildCondition(CobolParser.ConditionContext condition, boolean abbreviationOpen) {
        List<CobolParser.AndOrConditionContext> tails = condition.andOrCondition();
        if (tails.isEmpty()) return buildCombinable(condition.combinableCondition(), abbreviationOpen);
        List<Ast.LogicalConnector> connectors = tails.stream()
                .map(tail -> tail.AND() != null ? Ast.LogicalConnector.AND : Ast.LogicalConnector.OR)
                .toList();
        // AND binds tighter than OR: plan the fold before allocating any node ID.
        record ChainPlan(int firstElement, int lastElement) { }
        List<ChainPlan> chains = new ArrayList<>();
        int chainStart = 0;
        for (int i = 0; i < connectors.size(); i++) {
            if (connectors.get(i) == Ast.LogicalConnector.OR) {
                chains.add(new ChainPlan(chainStart, i));
                chainStart = i + 1;
            }
        }
        chains.add(new ChainPlan(chainStart, tails.size()));

        Ast.Meta rootMeta = meta(condition);
        boolean state = abbreviationOpen;
        if (chains.size() == 1) {
            // Only AND connectors: the AND node is the root and covers the whole condition.
            List<Ast.Expression> operands = new ArrayList<>();
            ConditionBuild first = buildCombinable(condition.combinableCondition(), state);
            operands.add(first.node());
            state = first.abbreviationOpen();
            for (CobolParser.AndOrConditionContext tail : tails) {
                ConditionBuild built = buildTail(tail, state);
                operands.add(built.node());
                state = built.abbreviationOpen();
            }
            return new ConditionBuild(new Ast.LogicalCondition(rootMeta, Ast.LogicalConnector.AND,
                    operands, sourceText(condition).strip()), state);
        }
        List<Ast.Expression> orOperands = new ArrayList<>();
        for (ChainPlan chain : chains) {
            int size = chain.lastElement() - chain.firstElement() + 1;
            if (size == 1) {
                ConditionBuild single = chain.firstElement() == 0
                        ? buildCombinable(condition.combinableCondition(), state)
                        : buildTail(tails.get(chain.firstElement() - 1), state);
                orOperands.add(single.node());
                state = single.abbreviationOpen();
            } else {
                ParserRuleContext start = chain.firstElement() == 0
                        ? condition.combinableCondition() : tails.get(chain.firstElement() - 1);
                ParserRuleContext end = tails.get(chain.lastElement() - 1);
                Ast.Meta chainMeta = metaForRange(start, end, "condition");
                List<Ast.Expression> operands = new ArrayList<>();
                for (int element = chain.firstElement(); element <= chain.lastElement(); element++) {
                    ConditionBuild built = element == 0
                            ? buildCombinable(condition.combinableCondition(), state)
                            : buildTail(tails.get(element - 1), state);
                    operands.add(built.node());
                    state = built.abbreviationOpen();
                }
                orOperands.add(new Ast.LogicalCondition(chainMeta, Ast.LogicalConnector.AND,
                        operands, sourceBetween(start, end)));
            }
        }
        return new ConditionBuild(new Ast.LogicalCondition(rootMeta, Ast.LogicalConnector.OR,
                orOperands, sourceText(condition).strip()), state);
    }

    private ConditionBuild buildTail(CobolParser.AndOrConditionContext tail, boolean abbreviationOpen) {
        if (tail.combinableCondition() != null)
            return buildCombinable(tail.combinableCondition(), abbreviationOpen);
        List<CobolParser.AbbreviationContext> abbreviations = tail.abbreviation();
        boolean state = abbreviationOpen;
        if (abbreviations.size() == 1) return buildAbbreviation(abbreviations.get(0));
        // The grammar groups all abbreviations under a single written connector.
        Ast.Meta meta = metaForRange(abbreviations.get(0),
                abbreviations.get(abbreviations.size() - 1), rule(tail));
        List<Ast.Expression> parts = new ArrayList<>();
        for (CobolParser.AbbreviationContext abbreviation : abbreviations) {
            ConditionBuild built = buildAbbreviation(abbreviation);
            parts.add(built.node());
            state = built.abbreviationOpen();
        }
        Ast.LogicalConnector connector = tail.AND() != null
                ? Ast.LogicalConnector.AND : Ast.LogicalConnector.OR;
        return new ConditionBuild(new Ast.LogicalCondition(meta, connector, parts,
                sourceText(tail).strip()), state);
    }

    private ConditionBuild buildCombinable(CobolParser.CombinableConditionContext combinable,
                                           boolean abbreviationOpen) {
        if (combinable.NOT() == null)
            return buildSimple(combinable.simpleCondition(), abbreviationOpen);
        Ast.Meta meta = meta(combinable);
        ConditionBuild inner = buildSimple(combinable.simpleCondition(), abbreviationOpen);
        return new ConditionBuild(new Ast.NegatedCondition(meta, inner.node(),
                sourceText(combinable).strip()), inner.abbreviationOpen());
    }

    private ConditionBuild buildSimple(CobolParser.SimpleConditionContext simple,
                                       boolean abbreviationOpen) {
        if (simple.condition() != null) {
            Ast.Meta meta = meta(simple);
            ConditionBuild inner = buildCondition(simple.condition(), abbreviationOpen);
            Ast.SourceSpan open = simple.LPARENCHAR() == null ? meta.span() : spanOf(simple.LPARENCHAR());
            Ast.SourceSpan close = simple.RPARENCHAR() == null ? meta.span() : spanOf(simple.RPARENCHAR());
            // A parenthesized group is a written boundary: it closes the abbreviation state.
            return new ConditionBuild(new Ast.GroupedCondition(meta, inner.node(), open, close,
                    sourceText(simple).strip()), false);
        }
        if (simple.relationCondition() != null) return buildRelationCondition(simple.relationCondition());
        if (simple.classCondition() != null) return buildClassCondition(simple.classCondition());
        return buildBareNominal(simple.conditionNameReference(), abbreviationOpen);
    }

    private ConditionBuild buildRelationCondition(CobolParser.RelationConditionContext relation) {
        if (relation.relationArithmeticComparison() != null)
            return buildArithmeticComparison(relation.relationArithmeticComparison());
        if (relation.relationCombinedComparison() != null)
            return buildCombinedComparison(relation.relationCombinedComparison());
        return buildSignCondition(relation.relationSignCondition());
    }

    private ConditionBuild buildArithmeticComparison(CobolParser.RelationArithmeticComparisonContext comparison) {
        Ast.Meta meta = meta(comparison);
        List<CobolParser.ArithmeticExpressionContext> values = comparison.arithmeticExpression();
        String operator = compact(sourceText(comparison.relationalOperator())).toUpperCase(Locale.ROOT);
        Ast.Expression subject = expression(values.get(0), "comparison subject");
        Ast.Expression object = expression(values.get(1), "comparison object");
        return new ConditionBuild(new Ast.RelationCondition(meta, subject, operator, object,
                sourceText(comparison).strip()), true);
    }

    private ConditionBuild buildCombinedComparison(CobolParser.RelationCombinedComparisonContext combined) {
        Ast.Meta meta = meta(combined);
        Ast.Expression subject = expression(combined.arithmeticExpression(), "distributed subject");
        CobolParser.RelationCombinedConditionContext group = combined.relationCombinedCondition();
        Ast.Meta groupMeta = meta(group);
        List<Ast.Expression> operands = group.arithmeticExpression().stream()
                .map(operand -> expression(operand, "distributed operand")).toList();
        List<Ast.LogicalConnector> connectors = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            ParseTree child = group.getChild(i);
            if (child instanceof TerminalNode terminal) {
                if (terminal.getText().equalsIgnoreCase("AND")) connectors.add(Ast.LogicalConnector.AND);
                else if (terminal.getText().equalsIgnoreCase("OR")) connectors.add(Ast.LogicalConnector.OR);
            }
        }
        Ast.DistributedOperandGroup distributed = new Ast.DistributedOperandGroup(groupMeta,
                operands, connectors, sourceText(group).strip());
        String operator = compact(sourceText(combined.relationalOperator())).toUpperCase(Locale.ROOT);
        return new ConditionBuild(new Ast.RelationCondition(meta, subject, operator, distributed,
                sourceText(combined).strip()), true);
    }

    private ConditionBuild buildSignCondition(CobolParser.RelationSignConditionContext sign) {
        Ast.Meta meta = meta(sign);
        List<Ast.Expression> operands = nearestDescendants(sign, AstBuilder::isPredicateOperandContext).stream()
                .map(value -> expression(value, "predicate operand")).toList();
        return new ConditionBuild(new Ast.OperationExpression(meta, rule(sign), operands,
                sourceText(sign).strip()), false);
    }

    private ConditionBuild buildClassCondition(CobolParser.ClassConditionContext classCondition) {
        Ast.Meta meta = meta(classCondition);
        Ast.Expression subject = expression(classCondition.identifier(), "class condition subject");
        String className = classCondition.className() != null
                ? clean(sourceText(classCondition.className()))
                : classCondition.NUMERIC() != null ? "NUMERIC"
                : classCondition.ALPHABETIC() != null ? "ALPHABETIC"
                : classCondition.ALPHABETIC_LOWER() != null ? "ALPHABETIC-LOWER"
                : classCondition.ALPHABETIC_UPPER() != null ? "ALPHABETIC-UPPER"
                : classCondition.DBCS() != null ? "DBCS"
                : classCondition.KANJI() != null ? "KANJI" : "<unknown>";
        return new ConditionBuild(new Ast.ClassCondition(meta, subject, className,
                classCondition.NOT() != null, sourceText(classCondition).strip()), false);
    }

    private ConditionBuild buildBareNominal(CobolParser.ConditionNameReferenceContext name,
                                            boolean abbreviationOpen) {
        // Standalone simple condition (no open abbreviation state): the structural
        // condition-name shape is kept; complete condition-name structure is Slice 4 work.
        if (!abbreviationOpen) return new ConditionBuild(dataReference(name), false);
        // Binding-dependent bare tail: the surface keeps the alternative open.
        Ast.Meta meta = meta(name);
        Ast.DataReference reference = dataReference(name);
        return new ConditionBuild(new Ast.ContextualConditionTail(meta, reference,
                sourceText(name).strip()), true);
    }

    private ConditionBuild buildAbbreviation(CobolParser.AbbreviationContext abbreviation) {
        CobolParser.RelationalOperatorContext relationalOperator = abbreviation.relationalOperator();
        if (relationalOperator != null) {
            Ast.Meta meta = meta(abbreviation);
            String operator = compact(sourceText(relationalOperator)).toUpperCase(Locale.ROOT);
            if (abbreviation.NOT() != null) operator = "NOT " + operator;
            Ast.Expression object = expression(abbreviation.arithmeticExpression(), "abbreviated object");
            return new ConditionBuild(new Ast.RelationCondition(meta, null, operator, object,
                    sourceText(abbreviation).strip()), true);
        }
        if (abbreviation.LPARENCHAR() == null) {
            // Subject and operator both omitted; a written NOT is a logical NOT
            // over the immediately following abbreviated relation fragment.
            Ast.Meta notMeta = abbreviation.NOT() == null ? null : meta(abbreviation);
            Ast.Meta meta = meta(abbreviation);
            Ast.Expression object = expression(abbreviation.arithmeticExpression(), "abbreviated object");
            Ast.RelationCondition omitted = new Ast.RelationCondition(meta, null, null, object,
                    sourceText(abbreviation).strip());
            if (notMeta == null) return new ConditionBuild(omitted, true);
            return new ConditionBuild(new Ast.NegatedCondition(notMeta, omitted,
                    sourceText(abbreviation).strip()), true);
        }
        // Recursive parenthesized abbreviation form: fail-closed preservation.
        return new ConditionBuild(preservedExpression(abbreviation, "condition"), false);
    }

    /** Span covering the written range between two parse contexts, for folded structure. */
    private Ast.Meta metaForRange(ParserRuleContext first, ParserRuleContext last, String grammarRule) {
        int id = nextId++;
        Token start = first.getStart(), stop = last.getStop();
        int startLine = start == null ? 0 : start.getLine();
        int startColumn = start == null ? 0 : start.getCharPositionInLine();
        int endLine = stop == null ? startLine : stop.getLine();
        int endColumn = stop == null ? startColumn : stop.getCharPositionInLine() + Math.max(0,
                stop.getText().codePointCount(0, stop.getText().length()) - 1);
        int startToken = start == null ? -1 : start.getTokenIndex();
        int endToken = stop == null ? startToken : stop.getTokenIndex();
        Ast.SourceSpan span = new Ast.SourceSpan(startLine, startColumn, endLine, endColumn,
                startToken, endToken);
        int startOffset = start == null ? 0 : Math.max(0, start.getStartIndex());
        int endOffset = stop == null ? startOffset : Math.min(indexedSource.length(), stop.getStopIndex() + 1);
        return new Ast.Meta(id, span, new Ast.ParseTreeOrigin(-1, grammarRule, 0),
                sourceMap.provenance(startOffset, endOffset));
    }

    private static Ast.SourceSpan spanOf(TerminalNode terminal) {
        Token token = terminal.getSymbol();
        return new Ast.SourceSpan(token.getLine(), token.getCharPositionInLine(), token.getLine(),
                token.getCharPositionInLine() + Math.max(0,
                        token.getText().codePointCount(0, token.getText().length()) - 1),
                token.getTokenIndex(), token.getTokenIndex());
    }

    private Ast.PreservedExpression preservedExpression(ParserRuleContext context, String role) {
        Ast.Meta meta = meta(context);
        List<Ast.Expression> recognized = nearestDescendants(context, AstBuilder::isRecognizedExpressionContext).stream()
                .map(child -> expression(child, role)).toList();
        Ast.PreservedExpression expression = new Ast.PreservedExpression(meta, rule(context),
                sourceText(context).strip(), recognized, Ast.ReferenceUnderstanding.PRESERVED);
        recordCoverage(expression, sourceText(context));
        return expression;
    }

    private static boolean isArithmeticContext(ParserRuleContext context) {
        return context instanceof CobolParser.ArithmeticExpressionContext
                || context instanceof CobolParser.MultDivsContext
                || context instanceof CobolParser.PowersContext
                || context instanceof CobolParser.BasisContext;
    }

    private static boolean isConditionContext(ParserRuleContext context) {
        return context instanceof CobolParser.ConditionContext
                || context instanceof CobolParser.CombinableConditionContext
                || context instanceof CobolParser.SimpleConditionContext
                || context instanceof CobolParser.RelationConditionContext
                || context instanceof CobolParser.RelationSignConditionContext
                || context instanceof CobolParser.RelationArithmeticComparisonContext
                || context instanceof CobolParser.RelationCombinedComparisonContext
                || context instanceof CobolParser.RelationCombinedConditionContext
                || context instanceof CobolParser.ClassConditionContext
                || context instanceof CobolParser.ConditionNameReferenceContext
                || context instanceof CobolParser.AbbreviationContext;
    }

    private static boolean isExpressionWrapper(ParserRuleContext context) {
        return context instanceof CobolParser.EvaluateSelectContext
                || context instanceof CobolParser.EvaluateValueContext
                || context instanceof CobolParser.EvaluateConditionContext
                || context instanceof CobolParser.ArgumentContext
                || context instanceof CobolParser.CharacterPositionContext
                || context instanceof CobolParser.LengthContext
                || context instanceof CobolParser.PerformFromContext
                || context instanceof CobolParser.PerformByContext;
    }

    private static boolean isLiteralContext(ParserRuleContext context) {
        return context instanceof CobolParser.LiteralContext
                || context instanceof CobolParser.IntegerLiteralContext
                || context instanceof CobolParser.NumericLiteralContext
                || context instanceof CobolParser.BooleanLiteralContext
                || context instanceof CobolParser.CicsDfhRespLiteralContext
                || context instanceof CobolParser.CicsDfhValueLiteralContext;
    }

    private static boolean isVisitorExpressionContext(ParserRuleContext context) {
        return context instanceof CobolParser.IdentifierContext
                || context instanceof CobolParser.FileNameContext
                || context instanceof CobolParser.IndexNameContext
                || context instanceof CobolParser.QualifiedDataNameContext
                || context instanceof CobolParser.ConditionNameReferenceContext
                || context instanceof CobolParser.TableCallContext
                || context instanceof CobolParser.FunctionCallContext
                || context instanceof CobolParser.SpecialRegisterContext
                || context instanceof CobolParser.SubscriptContext
                || isArithmeticContext(context)
                || isConditionContext(context)
                || isExpressionWrapper(context)
                || isLiteralContext(context);
    }

    private static boolean isDataClauseContext(ParserRuleContext context) {
        return context instanceof CobolParser.DataAlignedClauseContext
                || context instanceof CobolParser.DataBlankWhenZeroClauseContext
                || context instanceof CobolParser.DataCommonOwnLocalClauseContext
                || context instanceof CobolParser.DataExternalClauseContext
                || context instanceof CobolParser.DataGlobalClauseContext
                || context instanceof CobolParser.DataIntegerStringClauseContext
                || context instanceof CobolParser.DataJustifiedClauseContext
                || context instanceof CobolParser.DataOccursClauseContext
                || context instanceof CobolParser.DataPictureClauseContext
                || context instanceof CobolParser.DataReceivedByClauseContext
                || context instanceof CobolParser.DataRecordAreaClauseContext
                || context instanceof CobolParser.DataRedefinesClauseContext
                || context instanceof CobolParser.DataRenamesClauseContext
                || context instanceof CobolParser.DataSignClauseContext
                || context instanceof CobolParser.DataSynchronizedClauseContext
                || context instanceof CobolParser.DataThreadLocalClauseContext
                || context instanceof CobolParser.DataTypeClauseContext
                || context instanceof CobolParser.DataTypeDefClauseContext
                || context instanceof CobolParser.DataUsageClauseContext
                || context instanceof CobolParser.DataUsingClauseContext
                || context instanceof CobolParser.DataValueClauseContext
                || context instanceof CobolParser.DataWithLowerBoundsClauseContext;
    }

    private static boolean isStatementOperandContext(ParserRuleContext context) {
        return context instanceof CobolParser.IdentifierContext
                || context instanceof CobolParser.QualifiedDataNameContext
                || context instanceof CobolParser.ProcedureNameContext
                || context instanceof CobolParser.FileNameContext
                || context instanceof CobolParser.IndexNameContext
                || isLiteralContext(context)
                || context instanceof CobolParser.DataNameContext
                || context instanceof CobolParser.RecordNameContext
                || context instanceof CobolParser.ReportNameContext
                || context instanceof CobolParser.CdNameContext
                || context instanceof CobolParser.LibraryNameContext
                || context instanceof CobolParser.MnemonicNameContext
                || context instanceof CobolParser.EnvironmentNameContext
                || context instanceof CobolParser.AlphabetNameContext;
    }

    private static boolean isFlowClauseContext(ParserRuleContext context) {
        return context instanceof CobolParser.OnExceptionClauseContext
                || context instanceof CobolParser.NotOnExceptionClauseContext
                || context instanceof CobolParser.OnOverflowPhraseContext
                || context instanceof CobolParser.NotOnOverflowPhraseContext
                || context instanceof CobolParser.OnSizeErrorPhraseContext
                || context instanceof CobolParser.NotOnSizeErrorPhraseContext
                || context instanceof CobolParser.InvalidKeyPhraseContext
                || context instanceof CobolParser.NotInvalidKeyPhraseContext
                || context instanceof CobolParser.AtEndPhraseContext
                || context instanceof CobolParser.NotAtEndPhraseContext
                || context instanceof CobolParser.WriteAtEndOfPagePhraseContext
                || context instanceof CobolParser.WriteNotAtEndOfPagePhraseContext
                || context instanceof CobolParser.ReceiveNoDataContext
                || context instanceof CobolParser.ReceiveWithDataContext
                || context instanceof CobolParser.SearchWhenContext;
    }

    private static boolean isDataClauseReferenceContext(ParserRuleContext context) {
        return context instanceof CobolParser.QualifiedDataNameContext
                || context instanceof CobolParser.IdentifierContext
                || context instanceof CobolParser.FileNameContext
                || context instanceof CobolParser.IndexNameContext;
    }

    private static boolean isProcedureParameterContext(ParserRuleContext context) {
        return context instanceof CobolParser.ProcedureDivisionByReferenceContext
                || context instanceof CobolParser.ProcedureDivisionByValueContext;
    }

    private static boolean isCallArgumentContext(ParserRuleContext context) {
        return context instanceof CobolParser.CallByReferenceContext
                || context instanceof CobolParser.CallByValueContext
                || context instanceof CobolParser.CallByContentContext;
    }

    private static boolean isParameterValueContext(ParserRuleContext context) {
        return context instanceof CobolParser.IdentifierContext
                || context instanceof CobolParser.FileNameContext
                || context instanceof CobolParser.LiteralContext;
    }

    private static boolean isRecognizedExpressionContext(ParserRuleContext context) {
        return context instanceof CobolParser.ConditionContext
                || context instanceof CobolParser.ArithmeticExpressionContext
                || context instanceof CobolParser.IdentifierContext
                || context instanceof CobolParser.LiteralContext;
    }

    private static boolean isExpressionWrapperValueContext(ParserRuleContext context) {
        return isRecognizedExpressionContext(context)
                || context instanceof CobolParser.QualifiedDataNameContext
                || context instanceof CobolParser.IntegerLiteralContext;
    }

    private static boolean isReferenceBaseContext(ParserRuleContext context) {
        return context instanceof CobolParser.DataNameContext
                || context instanceof CobolParser.ConditionNameContext
                || context instanceof CobolParser.ParagraphNameContext
                || context instanceof CobolParser.TextNameContext;
    }

    private static boolean isQualifierContext(ParserRuleContext context) {
        return context instanceof CobolParser.InDataContext
                || context instanceof CobolParser.InTableContext
                || context instanceof CobolParser.InFileContext;
    }

    private static boolean isPredicateOperandContext(ParserRuleContext context) {
        return context instanceof CobolParser.ArithmeticExpressionContext
                || context instanceof CobolParser.IdentifierContext;
    }

    private String firstSemanticWord(ParserRuleContext context) {
        return sourceText(context).strip().split("[\\s(]", 2)[0];
    }

    private String sourceBetween(ParserRuleContext startContext, ParserRuleContext endContext) {
        int start = Math.max(0, startContext.getStart().getStartIndex());
        int end = Math.min(indexedSource.length(), endContext.getStop().getStopIndex() + 1);
        return indexedSource.substring(start, end).strip();
    }

    private Ast.Node nominalReference(ParserRuleContext context) {
        if (context instanceof CobolParser.ProcedureNameContext) return procedureReference(context);
        if (context instanceof CobolParser.FileNameContext)
            return new Ast.FileReference(meta(context), clean(sourceText(context)), sourceText(context).strip());
        if (context instanceof CobolParser.IndexNameContext)
            return new Ast.IndexReference(meta(context), clean(sourceText(context)), sourceText(context).strip());
        throw new IllegalArgumentException("unsupported nominal reference: " + rule(context));
    }

    private Ast.ProcedureReference procedureReference(ParserRuleContext context) {
        if (!(context instanceof CobolParser.ProcedureNameContext procedureName)) {
            throw new IllegalArgumentException("Expected procedureName, got " + rule(context));
        }
        Ast.Meta meta = meta(context);
        ParserRuleContext paragraph = procedureName.paragraphName();
        ParserRuleContext section = procedureName.sectionName();
        CobolParser.InSectionContext qualification = procedureName.inSection();
        String baseName = paragraph != null ? clean(sourceText(paragraph))
                : section != null ? clean(sourceText(section)) : firstSemanticWord(context);
        Ast.ProcedureQualifier qualifier = null;
        if (qualification != null) {
            ParserRuleContext qualifiedSection = qualification.sectionName();
            qualifier = new Ast.ProcedureQualifier(meta(qualification),
                    qualification.getToken(CobolParser.IN, 0) != null
                            ? Ast.QualifierConnector.IN : Ast.QualifierConnector.OF,
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
        int endColumn = stop == null ? startColumn : stop.getCharPositionInLine() + Math.max(0,
                stop.getText().codePointCount(0, stop.getText().length()) - 1);
        int startToken = start == null ? -1 : start.getTokenIndex();
        int endToken = stop == null ? startToken : stop.getTokenIndex();
        Ast.SourceSpan span = new Ast.SourceSpan(startLine, startColumn, endLine, endColumn, startToken, endToken);
        int startOffset = start == null ? 0 : Math.max(0, start.getStartIndex());
        int endOffset = stop == null ? startOffset : Math.min(indexedSource.length(), stop.getStopIndex() + 1);
        return new Ast.Meta(id, span,
                new Ast.ParseTreeOrigin(parseIds.getOrDefault(context, -1), rule(context),
                        parseSubtreeSizes.getOrDefault(context, 1)),
                sourceMap.provenance(startOffset, endOffset));
    }

    private List<Ast.Statement> statementsInside(ParserRuleContext context) {
        if (context == null) return List.of();
        List<CobolParser.StatementContext> statements = directChildren(context, CobolParser.StatementContext.class);
        if (statements.isEmpty() && context.getToken(CobolParser.NEXT, 0) != null
                && context.getToken(CobolParser.SENTENCE, 0) != null)
            return List.of(new Ast.NextSentenceStatement(meta(context)));
        return statements.stream().map(this::buildStatement).toList();
    }

    private List<Ast.Statement> directNestedStatements(ParserRuleContext context) {
        List<CobolParser.StatementContext> result = nearestDescendants(context,
                CobolParser.StatementContext.class);
        return result.stream().map(this::buildStatement).toList();
    }

    private ParserRuleContext firstDirectOrNearest(ParserRuleContext context,
                                                   Predicate<ParserRuleContext> predicate) {
        if (context == null) return null;
        for (ParserRuleContext child : directRuleChildren(context)) if (predicate.test(child)) return child;
        List<ParserRuleContext> found = nearestDescendants(context, predicate);
        return found.isEmpty() ? null : found.get(0);
    }

    private static CobolParser.CompilationUnitContext compilationUnit(ParseTree tree) {
        if (tree instanceof CobolParser.CompilationUnitContext context) return context;
        if (tree instanceof CobolParser.StartRuleContext context) return context.compilationUnit();
        if (tree instanceof CobolParser.ProgramUnitContext context
                && context.getParent() instanceof CobolParser.CompilationUnitContext compilation) {
            return compilation;
        }
        return null;
    }

    private static CobolParser.ProgramUnitContext firstProgramUnit(ParseTree tree) {
        if (tree instanceof CobolParser.ProgramUnitContext context) return context;
        CobolParser.CompilationUnitContext compilation = compilationUnit(tree);
        return compilation == null || compilation.programUnit().isEmpty()
                ? null : compilation.programUnit(0);
    }

    private static <T extends ParserRuleContext> T firstDescendant(ParseTree tree, Class<T> type) {
        if (tree == null) return null;
        if (type.isInstance(tree)) return type.cast(tree);
        for (int i = 0; i < tree.getChildCount(); i++) {
            T found = firstDescendant(tree.getChild(i), type);
            if (found != null) return found;
        }
        return null;
    }

    private static <T extends ParserRuleContext> List<T> nearestDescendants(ParseTree tree, Class<T> type) {
        List<T> result = new ArrayList<>();
        collectNearest(tree, type::isInstance, result, type);
        return result;
    }

    private static List<ParserRuleContext> nearestDescendants(ParseTree tree,
                                                               Predicate<ParserRuleContext> predicate) {
        List<ParserRuleContext> result = new ArrayList<>();
        collectNearest(tree, predicate, result);
        return result;
    }

    private static void collectNearest(ParseTree tree, Predicate<ParserRuleContext> predicate,
                                       List<ParserRuleContext> result) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof ParserRuleContext context && predicate.test(context)) result.add(context);
            else collectNearest(child, predicate, result);
        }
    }

    private static <T extends ParserRuleContext> void collectNearest(
            ParseTree tree, Predicate<ParserRuleContext> predicate, List<T> result, Class<T> type) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof ParserRuleContext context && predicate.test(context)) result.add(type.cast(context));
            else collectNearest(child, predicate, result, type);
        }
    }

    private static <T extends ParserRuleContext> List<T> directChildren(ParserRuleContext context, Class<T> type) {
        if (context == null) return List.of();
        return directRuleChildren(context).stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static List<ParserRuleContext> directRuleChildren(ParseTree context) {
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
        int end = Math.min(indexedSource.length(), context.getStop().getStopIndex() + 1);
        return start >= end ? "" : indexedSource.substring(start, end);
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
