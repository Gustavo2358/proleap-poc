package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

class AntlrCobolFrontend implements CobolFrontend {
    private final GrammarBinding binding;
    private final PreprocessorEngine preprocessor;

    AntlrCobolFrontend(GrammarBinding binding, Path copybooks) throws IOException {
        this.binding = binding;
        this.preprocessor = new PreprocessorEngine(binding, new CopybookLibrary(copybooks));
    }
    public String name() { return binding.name(); }

    @Override public FrontendResult parse(Path source) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        String file = source.getFileName().toString();
        long bytes = 0, lines = 0, prepNanos = 0, parseNanos = 0, peak = 0, tokensCount = 0, nodes = 0;
        int depth = 0, prepErrors = 0, unresolved = 0, lexerErrors = 0, parserErrors = 0;
        String normalized = "", sample = "";
        try (HeapSampler sampler = new HeapSampler()) {
            byte[] rawBytes = Files.readAllBytes(source); bytes = rawBytes.length;
            String raw = new String(rawBytes, StandardCharsets.UTF_8); lines = raw.lines().count();
            long prepStart = System.nanoTime();
            normalized = SourceNormalizer.fixed(raw);
            PreprocessorEngine.Outcome outcome = preprocessor.process(normalized, file);
            normalized = outcome.text(); diagnostics.addAll(outcome.diagnostics());
            prepErrors = outcome.errors(); unresolved = outcome.unresolved();
            prepNanos = System.nanoTime() - prepStart;

            int beforeLex = diagnostics.size();
            long parseStart = System.nanoTime();
            Lexer lexer = binding.cobolLexer(CharStreams.fromString(normalized, file));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new AntlrDiagnosticListener(name(), Diagnostic.Phase.LEXER, file, diagnostics));
            CommonTokenStream tokenStream = new CommonTokenStream(lexer); tokenStream.fill();
            tokensCount = tokenStream.getTokens().stream().filter(t -> t.getType() != Token.EOF).count();
            lexerErrors = (int) diagnostics.subList(beforeLex, diagnostics.size()).stream().filter(d -> d.phase() == Diagnostic.Phase.LEXER).count();
            tokenStream.seek(0);
            int beforeParser = diagnostics.size();
            Parser parser = binding.cobolParser(tokenStream);
            parser.removeErrorListeners();
            parser.addErrorListener(new AntlrDiagnosticListener(name(), Diagnostic.Phase.PARSER, file, diagnostics));
            ParseTree tree = binding.cobolStart(parser);
            parserErrors = (int) diagnostics.subList(beforeParser, diagnostics.size()).stream().filter(d -> d.phase() == Diagnostic.Phase.PARSER).count();
            TreeMetrics.Metrics metrics = TreeMetrics.measure(tree, parser);
            nodes = metrics.nodes(); depth = metrics.depth(); sample = metrics.sample();
            parseNanos = System.nanoTime() - parseStart;
            peak = sampler.peak();
        } catch (Exception e) {
            diagnostics.add(new Diagnostic(name(), e instanceof IOException ? Diagnostic.Phase.IO : Diagnostic.Phase.OTHER,
                    file, 0, 0, String.valueOf(e.getMessage()), "", e.getClass().getName()));
        }
        boolean prepSuccess = prepErrors == 0 && unresolved == 0 && diagnostics.stream().noneMatch(d -> d.phase() == Diagnostic.Phase.IO);
        boolean parseSuccess = lexerErrors == 0 && parserErrors == 0 && diagnostics.stream().noneMatch(d -> d.phase() == Diagnostic.Phase.OTHER);
        return new FrontendResult(name(), file, bytes, lines, prepSuccess, parseSuccess, prepErrors, lexerErrors,
                parserErrors, unresolved, prepNanos, parseNanos, prepNanos + parseNanos, peak, tokensCount, nodes,
                depth, List.copyOf(diagnostics), normalized, sample);
    }
}
