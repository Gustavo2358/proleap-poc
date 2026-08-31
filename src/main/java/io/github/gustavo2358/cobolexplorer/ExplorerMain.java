package io.github.gustavo2358.cobolexplorer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class ExplorerMain {
    private static final Logger LOG = LoggerFactory.getLogger(ExplorerMain.class);

    private record Node(int id, int parent, String kind, String name, String text,
                        int line, int column, int stopLine, int tokenStart, int tokenStop,
                        int depth, int childCount) {}

    private ExplorerMain() {}

    public static void main(String[] args) throws Exception {
        Path project = Path.of("").toAbsolutePath().normalize();
        Path source = project.resolve(argument(args, "--source", "corpus/cbl/COACTUPC.cbl"));
        List<Path> copybooks = copybookDirectories(project,
                argument(args, "--copybooks", "corpus/cpy,corpus/cpy-bms"));
        Path output = project.resolve(argument(args, "--output", "dist"));

        AnalysisProgress progress = new AnalysisProgress();
        long analysisStarted = System.nanoTime();
        try (AnalysisLogContext logContext = AnalysisLogContext.open(source)) {
            LOG.info("event=analysis_started phase=ANALYSIS output={}", output);
            try {
                analyze(source, copybooks, output, logContext, progress, analysisStarted);
            } catch (Exception exception) {
                LOG.error("event=analysis_failed phase={} elapsedMs={} reason={} impact=NO_RESULT",
                        progress.phase, elapsedMs(analysisStarted), exception.getClass().getSimpleName(), exception);
                throw exception;
            }
        }
    }

    private static void analyze(Path source, List<Path> copybooks, Path output,
                                AnalysisLogContext logContext, AnalysisProgress progress,
                                long analysisStarted) throws Exception {

        GrammarBinding binding = Bindings.cobol();
        List<Diagnostic> diagnostics = new ArrayList<>();
        progress.phase = "SOURCE_READ";
        String raw = Files.readString(source, StandardCharsets.UTF_8);

        progress.phase = "NORMALIZATION";
        long phaseStarted = System.nanoTime();
        SourceNormalizer.Result sourceNormalization = SourceNormalizer.normalize(raw,
                source.getFileName().toString(), new SourceNormalizer.Options(
                        SourceNormalizer.SourceFormat.FIXED,
                        SourceNormalizer.DebugLinePolicy.EXCLUDE));
        if (LOG.isDebugEnabled()) {
            LOG.debug("event=normalization_completed phase=NORMALIZATION elapsedMs={} originalLines={} normalizedLines={} diagnostics={}",
                    elapsedMs(phaseStarted), raw.lines().count(), sourceNormalization.text().lines().count(),
                    sourceNormalization.diagnostics().size());
        }

        progress.phase = "PREPROCESSING";
        phaseStarted = System.nanoTime();
        PreprocessorEngine.Outcome preprocessed =
                new PreprocessorEngine(binding, new CopybookLibrary(copybooks))
                        .process(sourceNormalization.sourceMap(), source.getFileName().toString());
        String normalized = preprocessed.text();
        diagnostics.addAll(preprocessed.diagnostics());
        LOG.debug("event=preprocessing_completed phase=PREPROCESSING elapsedMs={} unresolvedCopies={} errors={} compilerOptions={}",
                elapsedMs(phaseStarted), preprocessed.unresolved(), preprocessed.errors(),
                preprocessed.compilerOptions().size());

        progress.phase = "LEXING";
        phaseStarted = System.nanoTime();
        Lexer lexer = binding.cobolLexer(CharStreams.fromString(normalized, source.getFileName().toString()));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new AntlrDiagnosticListener(binding.name(), Diagnostic.Phase.LEXER,
                source.getFileName().toString(), diagnostics));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        tokens.seek(0);
        long tokenCount = tokens.getTokens().stream().filter(t -> t.getType() != Token.EOF).count();
        long lexerErrors = diagnostics.stream().filter(d -> d.phase() == Diagnostic.Phase.LEXER).count();
        LOG.debug("event=lexing_completed phase=LEXING elapsedMs={} tokens={} lexerErrors={}",
                elapsedMs(phaseStarted), tokenCount, lexerErrors);

        progress.phase = "PARSING";
        phaseStarted = System.nanoTime();
        Parser parser = binding.cobolParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new AntlrDiagnosticListener(binding.name(), Diagnostic.Phase.PARSER,
                source.getFileName().toString(), diagnostics));
        ParseTree tree = binding.cobolStart(parser);

        List<Node> nodes = new ArrayList<>();
        Map<String, Integer> ruleCounts = new TreeMap<>();
        IdentityHashMap<ParseTree, Integer> parseIds = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> parseSubtreeSizes = new IdentityHashMap<>();
        walk(tree, -1, 0, parser, nodes, ruleCounts, parseIds, parseSubtreeSizes);
        int maxDepth = nodes.stream().mapToInt(Node::depth).max().orElse(0);
        long parserErrors = diagnostics.stream().filter(d -> d.phase() == Diagnostic.Phase.PARSER).count();
        LOG.debug("event=parsing_completed phase=PARSING elapsedMs={} nodes={} maxDepth={} parserErrors={}",
                elapsedMs(phaseStarted), nodes.size(), maxDepth, parserErrors);
        if (lexerErrors > 0 || parserErrors > 0) {
            LOG.warn("event=parse_degraded phase=PARSING lexerErrors={} parserErrors={} reason=ANTLR_DIAGNOSTICS fallback=CONTINUE_WITH_PARTIAL_PARSE_TREE result=PARSE_TREE_PRODUCED impact=ANALYSIS_INCOMPLETE",
                    lexerErrors, parserErrors);
        }

        progress.phase = "ARTIFACT_EXPORT";
        Files.createDirectories(output);
        copyWebResources(output);
        Files.writeString(output.resolve("preprocessed.cbl"), normalized, StandardCharsets.UTF_8);
        writeData(output.resolve("tree-data.js"), source.getFileName().toString(), raw.lines().count(),
                normalized, preprocessed.unresolved(), tokenCount, maxDepth, lexerErrors, parserErrors,
                nodes, ruleCounts, diagnostics);

        progress.phase = "AST_BUILD";
        phaseStarted = System.nanoTime();
        CompilationUnitBuildResult compilationBuild = new AstBuilder(parser, normalized,
                preprocessed.sourceMap(), parseIds, parseSubtreeSizes)
                .buildCompilationUnit(tree, source.getFileName().toString());
        CompilationUnitModel compilationUnit = compilationBuild.compilationUnit();
        if (compilationUnit.programUnits().isEmpty())
            throw new IllegalStateException("No COBOL program unit was produced by the semantic frontend");
        CompilationUnitModel.ProgramUnit primaryUnit = compilationUnit.programUnits().get(0);
        logContext.setProgramUnit(primaryUnit.id().canonicalProgramName());
        Ast.Program ast = primaryUnit.program();
        AstSnapshot astSnapshot = AstSnapshot.from(ast);
        astSnapshot.write(output.resolve("ast-data.js"), source.getFileName().toString(), nodes.size(),
                Arrays.asList(normalized.split("\\R", -1)));
        CoverageSnapshot coverageSnapshot = CoverageSnapshot.from(source.getFileName().toString(), ast,
                compilationBuild.coverageByProgramUnit().get(primaryUnit.id()), preprocessed.unresolved(),
                (int) lexerErrors, (int) parserErrors);
        coverageSnapshot.write(output.resolve("coverage-data.js"));
        if (LOG.isDebugEnabled()) {
            int semanticDiagnostics = compilationBuild.diagnosticsByProgramUnit().values().stream()
                    .mapToInt(List::size).sum();
            LOG.debug("event=ast_built phase=AST_BUILD elapsedMs={} programUnits={} nodes={} semanticDiagnostics={}",
                    elapsedMs(phaseStarted), compilationUnit.programUnits().size(), astSnapshot.metrics().nodes(),
                    semanticDiagnostics);
        }

        progress.phase = "SYMBOL_TABLE_BUILD";
        phaseStarted = System.nanoTime();
        CompilationUnitSymbolTables symbolTables = new CompilationUnitSymbolTableBuilder().build(compilationUnit);
        SymbolTable symbolTable = symbolTables.forProgramUnit(primaryUnit.id()).orElseThrow().symbolTable();
        SymbolTableSnapshot symbolSnapshot = SymbolTableSnapshot.from(symbolTable);
        symbolSnapshot.write(output.resolve("symbol-data.js"), source.getFileName().toString(),
                Arrays.asList(normalized.split("\\R", -1)));
        LOG.debug("event=symbol_tables_built phase=SYMBOL_TABLE_BUILD elapsedMs={} programUnits={} symbols={} scopes={} diagnostics={}",
                elapsedMs(phaseStarted), symbolTables.units().size(), symbolSnapshot.metrics().symbols(),
                symbolSnapshot.metrics().scopes(), symbolSnapshot.metrics().diagnostics());

        progress.phase = "REFERENCE_COLLECTION";
        phaseStarted = System.nanoTime();
        Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrences = new LinkedHashMap<>();
        for (CompilationUnitModel.ProgramUnit unit : compilationUnit.programUnits()) {
            SymbolTable unitTable = symbolTables.forProgramUnit(unit.id()).orElseThrow().symbolTable();
            occurrences.put(unit.id(), new ReferenceOccurrenceCollector().collect(unit.id(), unit.program(),
                    AstScopeIndex.build(unit.program(), unitTable)));
        }
        if (LOG.isDebugEnabled()) {
            long referenceCount = occurrences.values().stream()
                    .mapToLong(value -> value.occurrences().size()).sum();
            LOG.debug("event=references_collected phase=REFERENCE_COLLECTION elapsedMs={} programUnits={} references={}",
                    elapsedMs(phaseStarted), occurrences.size(), referenceCount);
        }

        progress.phase = "REFERENCE_RESOLUTION";
        phaseStarted = System.nanoTime();
        ResolutionContracts.CobolResolutionPolicy policy = ResolutionContracts.CobolResolutionPolicy.initial()
                .withPgmnameMode(preprocessed.pgmnameMode())
                .withDynamMode(preprocessed.dynamMode())
                .withDllMode(preprocessed.dllMode());
        ReferenceResolution resolution = new CobolReferenceResolver(policy)
                .resolve(compilationUnit, symbolTables, occurrences);

        progress.phase = "EXTERNAL_CLASSIFICATION";
        ResolutionAnalysisReport.FrontendState frontendState =
                new ResolutionAnalysisReport.FrontendState(
                        preprocessed.unresolved(), preprocessed.errors(),
                        (int) lexerErrors, (int) parserErrors, diagnostics);
        boolean classifierExecuted = frontendState.supportsExternalClassification();
        ExternalClassification externalClassifications = classifierExecuted
                ? new CicsIntrinsicClassifier().classify(compilationUnit, occurrences, resolution,
                        frontendState.externalClassificationInputCompleteness())
                : ExternalClassification.empty();
        String classifierReason = classifierExecuted
                ? "STRUCTURAL_PREREQUISITES_AVAILABLE" : "STRUCTURAL_FRONTEND_ERRORS";
        String classifierFallback = !classifierExecuted ? "SKIP_CLASSIFIER_FAIL_CLOSED"
                : preprocessed.unresolved() > 0
                ? "CONTINUE_WITH_PARTIAL_ANALYSIS" : "NONE";
        LOG.debug("event=external_classification_completed phase=EXTERNAL_CLASSIFICATION elapsedMs={} executed={} reason={} unresolvedCopies={} inputCompleteness={} classifications={} fallback={} impact={}",
                elapsedMs(phaseStarted), classifierExecuted, classifierReason, preprocessed.unresolved(),
                frontendState.externalClassificationInputCompleteness(),
                externalClassifications.entries().size(), classifierFallback,
                preprocessed.unresolved() > 0 ? "ANALYSIS_INCOMPLETE" : "NO_ADDITIONAL_IMPACT");
        ResolutionAnalysisReport resolutionReport = ResolutionAnalysisReport.compose(compilationBuild,
                frontendState, occurrences, resolution, externalClassifications);
        ResolutionSnapshot.from(source.getFileName().toString(),
                        Arrays.asList(normalized.split("\\R", -1)), compilationUnit, resolution,
                        resolutionReport)
                .write(output.resolve("resolution-data.js"));
        ReferenceResolution.Metrics resolutionMetrics = resolution.metrics();
        LOG.debug("event=resolution_completed phase=REFERENCE_RESOLUTION elapsedMs={} references={} resolved={} externalObserved={} unresolved={} ambiguous={} unsupported={} externalClassifications={} indexedDeclarations={} nominalLookups={} candidateInspections={} maximumCandidates={}",
                elapsedMs(phaseStarted), resolution.entries().size(),
                resolutionReport.statusCounts().get(ResolutionContracts.ResolutionStatus.RESOLVED),
                resolutionReport.statusCounts().get(ResolutionContracts.ResolutionStatus.EXTERNAL_OBSERVED),
                resolutionReport.statusCounts().get(ResolutionContracts.ResolutionStatus.UNRESOLVED),
                resolutionReport.statusCounts().get(ResolutionContracts.ResolutionStatus.AMBIGUOUS),
                resolutionReport.statusCounts().get(ResolutionContracts.ResolutionStatus.UNSUPPORTED),
                externalClassifications.entries().size(),
                resolutionMetrics.indexedDeclarations(), resolutionMetrics.nominalLookups(),
                resolutionMetrics.candidateInspections(), resolutionMetrics.maximumCandidates());
        if (!resolutionReport.completeness().dependencyAnalysisReady()) {
            List<String> blockingReasons = resolutionReport.completeness().blockingReasons().stream().limit(5).toList();
            LOG.warn("event=analysis_degraded phase=REFERENCE_RESOLUTION gaps={} blockingReasons={} reason=RESOLUTION_GAPS fallback=RESULT_PUBLISHED_WITH_GAPS impact=DEPENDENCY_ANALYSIS_NOT_READY statusCounts={}",
                    resolutionReport.gaps().size(), blockingReasons, resolutionReport.statusCounts());
        }

        System.out.printf(Locale.ROOT,
                "Generated %s, %s, %s and %s%nSource: %s%nParse tree: %,d nodes | %,d tokens | depth %d%n" +
                "AST: %,d nodes | depth %d | literal-target CALLs %d%n" +
                        "Symbols: %,d declarations | %,d scopes | %,d diagnostics | parser errors %d%n" +
                        "Reference binding: %,d entries | %,d gaps | dependency analysis ready: %s%n",
                output.resolve("index.html"), output.resolve("ast.html"), output.resolve("symbols.html"),
                output.resolve("resolution.html"),
                source.getFileName(), nodes.size(),
                tokenCount, maxDepth, astSnapshot.metrics().nodes(), astSnapshot.metrics().maxDepth(),
                astSnapshot.metrics().literalTargetCalls(), symbolSnapshot.metrics().symbols(),
                symbolSnapshot.metrics().scopes(), symbolSnapshot.metrics().diagnostics(), parserErrors,
                resolution.entries().size(), resolutionReport.gaps().size(),
                resolutionReport.completeness().dependencyAnalysisReady());
        progress.phase = "COMPLETED";
        LOG.info("event=analysis_completed phase=ANALYSIS elapsedMs={} programUnits={} references={} gaps={} dependencyAnalysisReady={} output={}",
                elapsedMs(analysisStarted), compilationUnit.programUnits().size(), resolution.entries().size(),
                resolutionReport.gaps().size(), resolutionReport.completeness().dependencyAnalysisReady(), output);
    }

    private static long elapsedMs(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private static final class AnalysisProgress {
        private String phase = "INITIALIZATION";
    }

    private static int walk(ParseTree tree, int parent, int depth, Parser parser,
                            List<Node> nodes, Map<String, Integer> ruleCounts,
                            IdentityHashMap<ParseTree, Integer> parseIds,
                            IdentityHashMap<ParseTree, Integer> parseSubtreeSizes) {
        int id = nodes.size();
        parseIds.put(tree, id);
        String kind;
        String name;
        String text = "";
        int line = 0, column = 0, stopLine = 0, tokenStart = -1, tokenStop = -1;

        if (tree instanceof ParserRuleContext context) {
            kind = "rule";
            name = parser.getRuleNames()[context.getRuleIndex()];
            ruleCounts.merge(name, 1, Integer::sum);
            Token start = context.getStart(), stop = context.getStop();
            if (start != null) {
                line = start.getLine(); column = start.getCharPositionInLine(); tokenStart = start.getTokenIndex();
            }
            if (stop != null) {
                stopLine = stop.getLine(); tokenStop = stop.getTokenIndex();
            }
        } else if (tree instanceof ErrorNode error) {
            kind = "error";
            Token token = error.getSymbol();
            name = tokenName(parser, token);
            text = token.getText();
            line = stopLine = token.getLine(); column = token.getCharPositionInLine();
            tokenStart = tokenStop = token.getTokenIndex();
        } else if (tree instanceof TerminalNode terminal) {
            kind = "terminal";
            Token token = terminal.getSymbol();
            name = tokenName(parser, token);
            text = token.getText();
            line = stopLine = token.getLine(); column = token.getCharPositionInLine();
            tokenStart = tokenStop = token.getTokenIndex();
        } else {
            kind = "unknown";
            name = tree.getClass().getSimpleName();
        }

        nodes.add(new Node(id, parent, kind, name, text, line, column, stopLine,
                tokenStart, tokenStop, depth, tree.getChildCount()));
        int subtreeSize = 1;
        for (int i = 0; i < tree.getChildCount(); i++) {
            subtreeSize += walk(tree.getChild(i), id, depth + 1, parser, nodes, ruleCounts,
                    parseIds, parseSubtreeSizes);
        }
        parseSubtreeSizes.put(tree, subtreeSize);
        return subtreeSize;
    }

    private static String tokenName(Parser parser, Token token) {
        if (token.getType() == Token.EOF) return "EOF";
        String symbolic = parser.getVocabulary().getSymbolicName(token.getType());
        if (symbolic != null) return symbolic;
        String literal = parser.getVocabulary().getLiteralName(token.getType());
        return literal == null ? "TOKEN_" + token.getType() : literal;
    }

    private static void copyWebResources(Path output) throws IOException {
        Path resources = Path.of("src/main/resources/web");
        try (var files = Files.walk(resources)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Files.copy(file, output.resolve(resources.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void writeData(Path path, String sourceName, long originalLines, String source,
                                  int unresolvedCopies, long tokenCount, int maxDepth,
                                  long lexerErrors, long parserErrors, List<Node> nodes,
                                  Map<String, Integer> ruleCounts, List<Diagnostic> diagnostics) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            out.write("window.PARSE_TREE_DATA={\n\"meta\":{");
            field(out, "source", sourceName); out.write(',');
            out.write("\"originalLines\":" + originalLines + ',');
            out.write("\"preprocessedLines\":" + source.lines().count() + ',');
            out.write("\"nodes\":" + nodes.size() + ',');
            out.write("\"tokens\":" + tokenCount + ',');
            out.write("\"maxDepth\":" + maxDepth + ',');
            out.write("\"unresolvedCopies\":" + unresolvedCopies + ',');
            out.write("\"lexerErrors\":" + lexerErrors + ',');
            out.write("\"parserErrors\":" + parserErrors);
            out.write("},\n\"sourceLines\":[");
            String[] lines = source.split("\\R", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) out.write(',');
                string(out, lines[i]);
            }
            out.write("],\n\"nodes\":[");
            for (int i = 0; i < nodes.size(); i++) {
                if (i > 0) out.write(',');
                Node n = nodes.get(i);
                out.write("{\"id\":" + n.id + ",\"p\":" + n.parent + ",\"k\":"); string(out, n.kind);
                out.write(",\"n\":"); string(out, n.name);
                if (!n.text.isEmpty()) { out.write(",\"x\":"); string(out, n.text); }
                out.write(",\"l\":" + n.line + ",\"c\":" + n.column + ",\"e\":" + n.stopLine);
                out.write(",\"a\":" + n.tokenStart + ",\"b\":" + n.tokenStop);
                out.write(",\"d\":" + n.depth + ",\"q\":" + n.childCount + '}');
            }
            out.write("],\n\"ruleCounts\":{");
            boolean first = true;
            for (var entry : ruleCounts.entrySet()) {
                if (!first) out.write(','); first = false;
                string(out, entry.getKey()); out.write(':' + String.valueOf(entry.getValue()));
            }
            out.write("},\n\"diagnostics\":[");
            for (int i = 0; i < diagnostics.size(); i++) {
                if (i > 0) out.write(',');
                Diagnostic d = diagnostics.get(i);
                out.write("{\"phase\":"); string(out, d.phase().name());
                out.write(",\"line\":" + d.line() + ",\"message\":"); string(out, d.message());
                out.write('}');
            }
            out.write("]};\n");
        }
    }

    private static void field(Writer out, String name, String value) throws IOException {
        string(out, name); out.write(':'); string(out, value);
    }

    private static void string(Writer out, String value) throws IOException {
        out.write('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.write("\\\"");
                case '\\' -> out.write("\\\\");
                case '\b' -> out.write("\\b");
                case '\f' -> out.write("\\f");
                case '\n' -> out.write("\\n");
                case '\r' -> out.write("\\r");
                case '\t' -> out.write("\\t");
                default -> {
                    if (ch < 0x20 || ch == '\u2028' || ch == '\u2029') out.write(String.format("\\u%04x", (int) ch));
                    else out.write(ch);
                }
            }
        }
        out.write('"');
    }

    private static String argument(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) if (args[i].equals(name)) return args[i + 1];
        return fallback;
    }

    private static List<Path> copybookDirectories(Path project, String value) {
        String[] parts = value.split(",", -1);
        List<Path> directories = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("--copybooks must contain non-empty directories separated by commas");
            }
            directories.add(project.resolve(trimmed));
        }
        return List.copyOf(directories);
    }
}
