package io.proleap.benchmark;

import java.util.*;

/** Semantic COBOL AST used by the explorer and by future analysis phases. */
public final class Ast {
    private Ast() {}

    public record SourceSpan(int startLine, int startColumn, int endLine, int endColumn,
                             int startToken, int endToken) {}

    public record ParseTreeOrigin(int rootNodeId, String grammarRule, int subtreeNodeCount) {}

    public record SourceLocation(String file, int startLine, int startColumn,
                                 int endLine, int endColumn) {}

    public record CopyFrame(String includingFile, String requestedName, String includedFile,
                            int includeLine) {}

    public record SourceProvenance(SourceLocation expanded, SourceLocation original,
                                   List<CopyFrame> includeChain, boolean exact) {
        public SourceProvenance { includeChain = List.copyOf(includeChain); }
    }

    public record Meta(int id, SourceSpan span, ParseTreeOrigin origin,
                       SourceProvenance provenance) {
        public Meta(int id, SourceSpan span, ParseTreeOrigin origin) {
            this(id, span, origin, new SourceProvenance(
                    new SourceLocation("<unknown>", span.startLine(), span.startColumn(), span.endLine(), span.endColumn()),
                    new SourceLocation("<unknown>", span.startLine(), span.startColumn(), span.endLine(), span.endColumn()),
                    List.of(), false));
        }
    }

    public sealed interface Node permits Program, Division, Section, FileBinding, FileDescription,
            DataEntry, Paragraph, Sentence, CallArgument, EvaluateBranch, Statement, Expression {
        Meta meta();
    }

    public enum DivisionKind { IDENTIFICATION, ENVIRONMENT, DATA, PROCEDURE }
    public enum SentenceTerminator { PERIOD, END_OF_PROCEDURE }
    public enum PassingMode { REFERENCE, VALUE, CONTENT }
    public enum EmbeddedLanguage { SQL, CICS, SQLIMS, UNKNOWN }
    public enum CallTargetKind { STATIC_LITERAL, DYNAMIC_EXPRESSION }
    public enum PerformKind { INLINE, PROCEDURE }
    public enum GoToKind { SIMPLE, DEPENDING_ON }

    public record Program(Meta meta, String name, List<Division> divisions) implements Node {
        public Program { divisions = List.copyOf(divisions); }
    }

    public record Division(Meta meta, DivisionKind divisionKind, List<Node> children) implements Node {
        public Division { children = List.copyOf(children); }
    }

    public record Section(Meta meta, String name, List<Node> children) implements Node {
        public Section { children = List.copyOf(children); }
    }

    public record FileBinding(Meta meta, String logicalName, String assignment) implements Node {}

    public record FileDescription(Meta meta, String fileName, List<DataEntry> entries) implements Node {
        public FileDescription { entries = List.copyOf(entries); }
    }

    public record DataEntry(Meta meta, String level, String name, String declaration) implements Node {}

    public record Paragraph(Meta meta, String name, List<Sentence> sentences) implements Node {
        public Paragraph { sentences = List.copyOf(sentences); }
    }

    public record Sentence(Meta meta, List<Statement> statements, SentenceTerminator terminator,
                           SourceSpan terminatorSpan) implements Node {
        public Sentence { statements = List.copyOf(statements); }
    }

    public sealed interface Statement extends Node permits CallStatement, IfStatement, EvaluateStatement,
            PerformStatement, GoToStatement, MoveStatement, EmbeddedLanguageStatement,
            NextSentenceStatement, UnsupportedStatement {}

    public sealed interface Expression extends Node permits LiteralExpression, DataReference, RawExpression {}

    public record CallStatement(Meta meta, CallTargetKind targetKind, Expression target,
                                List<CallArgument> arguments, List<Statement> exceptionFlow) implements Statement {
        public CallStatement {
            arguments = List.copyOf(arguments);
            exceptionFlow = List.copyOf(exceptionFlow);
        }
    }

    public record CallArgument(Meta meta, PassingMode passingMode, Expression value) implements Node {}

    public record IfStatement(Meta meta, Expression condition, List<Statement> thenBranch,
                              List<Statement> elseBranch, boolean explicitlyTerminated) implements Statement {
        public IfStatement {
            thenBranch = List.copyOf(thenBranch);
            elseBranch = List.copyOf(elseBranch);
        }
    }

    public record EvaluateStatement(Meta meta, List<Expression> subjects, List<EvaluateBranch> branches,
                                    boolean explicitlyTerminated) implements Statement {
        public EvaluateStatement {
            subjects = List.copyOf(subjects);
            branches = List.copyOf(branches);
        }
    }

    public record EvaluateBranch(Meta meta, String selector, boolean other,
                                 List<Statement> statements) implements Node {
        public EvaluateBranch { statements = List.copyOf(statements); }
    }

    public record PerformStatement(Meta meta, PerformKind performKind, String fromProcedure,
                                   String throughProcedure, String control,
                                   List<Statement> inlineBody) implements Statement {
        public PerformStatement { inlineBody = List.copyOf(inlineBody); }
    }

    public record GoToStatement(Meta meta, GoToKind goToKind, List<String> targets,
                                Expression dependingOn) implements Statement {
        public GoToStatement { targets = List.copyOf(targets); }
    }

    public record MoveStatement(Meta meta, Expression source, List<Expression> targets,
                                boolean corresponding) implements Statement {
        public MoveStatement { targets = List.copyOf(targets); }
    }

    /** Raw embedded-language payload; parsedContent is intentionally deferred to a future plugin. */
    public record EmbeddedLanguageStatement(Meta meta, EmbeddedLanguage language,
                                            String rawText) implements Statement {}

    public record NextSentenceStatement(Meta meta) implements Statement {}

    /** Keeps unsupported syntax visible and retains any directly nested statements. */
    public record UnsupportedStatement(Meta meta, String grammarRule, String rawText,
                                       List<Statement> nestedStatements) implements Statement {
        public UnsupportedStatement { nestedStatements = List.copyOf(nestedStatements); }
    }

    public record LiteralExpression(Meta meta, String value, String rawLexeme) implements Expression {}
    public record DataReference(Meta meta, String writtenName) implements Expression {}
    public record RawExpression(Meta meta, String role, String rawText) implements Expression {}

    public static List<? extends Node> children(Node node) {
        if (node instanceof Program n) return n.divisions();
        if (node instanceof Division n) return n.children();
        if (node instanceof Section n) return n.children();
        if (node instanceof FileDescription n) return n.entries();
        if (node instanceof Paragraph n) return n.sentences();
        if (node instanceof Sentence n) return n.statements();
        if (node instanceof CallStatement n) {
            List<Node> result = new ArrayList<>();
            result.add(n.target()); result.addAll(n.arguments()); result.addAll(n.exceptionFlow());
            return result;
        }
        if (node instanceof CallArgument n) return List.of(n.value());
        if (node instanceof IfStatement n) {
            List<Node> result = new ArrayList<>();
            result.add(n.condition()); result.addAll(n.thenBranch()); result.addAll(n.elseBranch());
            return result;
        }
        if (node instanceof EvaluateStatement n) {
            List<Node> result = new ArrayList<>(n.subjects()); result.addAll(n.branches()); return result;
        }
        if (node instanceof EvaluateBranch n) return n.statements();
        if (node instanceof PerformStatement n) return n.inlineBody();
        if (node instanceof GoToStatement n) return n.dependingOn() == null ? List.of() : List.of(n.dependingOn());
        if (node instanceof MoveStatement n) {
            List<Node> result = new ArrayList<>(); result.add(n.source()); result.addAll(n.targets()); return result;
        }
        if (node instanceof UnsupportedStatement n) return n.nestedStatements();
        return List.of();
    }
}
