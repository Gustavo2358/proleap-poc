package io.github.gustavo2358.cobolexplorer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticModelLoggingTest {
    @Test
    void astBuildLogsProgramSummaryAndStatementDecisionsWithoutChangingTheAst() throws Exception {
        AstBuildResult baseline = parse("statements.cbl");
        Captured<AstBuildResult> captured = capture(AstBuilder.class, Level.TRACE,
                () -> parse("statements.cbl"));

        assertEquals(AstSnapshot.from(baseline.program()).metrics(),
                AstSnapshot.from(captured.result().program()).metrics());
        assertEquals(baseline.coverage(), captured.result().coverage());
        assertEvent(captured.events(), "event=ast_built", "scope=PROGRAM_UNIT", "programUnit=STMTTEST",
                "nodes=", "semanticDiagnostics=", "unsupportedStatements=", "preservedStatements=", "elapsedMs=");
        assertEvent(captured.events(), "event=ast_statement_modeled", "grammarRule=acceptStatement");
        assertEvent(captured.events(), "event=ast_statement_preserved", "grammarRule=alterStatement");
    }

    @Test
    void symbolTableLogsProgramSummaryAndDeclarationTraceWithoutChangingTables() throws Exception {
        Ast.Program program = parse("declarations.cbl").program();
        SymbolTable baseline = new SymbolTableBuilder().build(program);
        Captured<SymbolTable> captured = capture(SymbolTableBuilder.class, Level.TRACE,
                () -> new SymbolTableBuilder().build(program));

        assertEquals(baseline.scopes(), captured.result().scopes());
        assertEquals(baseline.symbols(), captured.result().symbols());
        assertEquals(baseline.declarationRelations(), captured.result().declarationRelations());
        assertEvent(captured.events(), "event=symbol_table_built", "scope=PROGRAM_UNIT", "programUnit=DECLTEST",
                "scopes=", "symbols=", "entities=", "declarationRelations=", "diagnostics=", "elapsedMs=");
        assertEvent(captured.events(), "event=scope_created");
        assertEvent(captured.events(), "event=symbol_declared");
        assertEvent(captured.events(), "event=declaration_relation_registered");
    }

    private static AstBuildResult parse(String fixture) throws Exception {
        Path file = Path.of("src/test/resources/cobol/semantic", fixture).toAbsolutePath();
        String source = SourceNormalizerTestSupport.fixed(Files.readString(file, StandardCharsets.UTF_8));
        GrammarBinding binding = Bindings.cobol();
        Parser parser = binding.cobolParser(new CommonTokenStream(binding.cobolLexer(CharStreams.fromString(source))));
        ParseTree tree = binding.cobolStart(parser);
        IdentityHashMap<ParseTree, Integer> ids = new IdentityHashMap<>();
        IdentityHashMap<ParseTree, Integer> sizes = new IdentityHashMap<>();
        index(tree, ids, sizes, new int[]{0});
        return new AstBuilder(parser, source, SourceMap.identity(source, fixture), ids, sizes).build(tree);
    }

    private static int index(ParseTree tree, IdentityHashMap<ParseTree, Integer> ids,
                             IdentityHashMap<ParseTree, Integer> sizes, int[] next) {
        ids.put(tree, next[0]++);
        int size = 1;
        for (int index = 0; index < tree.getChildCount(); index++)
            size += index(tree.getChild(index), ids, sizes, next);
        sizes.put(tree, size);
        return size;
    }

    private static void assertEvent(List<ILoggingEvent> events, String... fragments) {
        assertTrue(events.stream().map(ILoggingEvent::getFormattedMessage)
                        .anyMatch(message -> java.util.Arrays.stream(fragments).allMatch(message::contains)),
                () -> "missing event " + List.of(fragments) + " in "
                        + events.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    private static <T> Captured<T> capture(Class<?> owner, Level level, ThrowingSupplier<T> action)
            throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(owner);
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        SnapshotListAppender appender = new SnapshotListAppender();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(level);
        logger.setAdditive(false);
        try {
            T result = action.get();
            return new Captured<>(List.copyOf(appender.list), result);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
            appender.stop();
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> { T get() throws Exception; }
    private record Captured<T>(List<ILoggingEvent> events, T result) { }

    private static final class SnapshotListAppender extends ListAppender<ILoggingEvent> {
        @Override protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    }
}
