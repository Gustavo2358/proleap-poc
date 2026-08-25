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
            DataEntry, Paragraph, Sentence, CallArgument, EvaluateBranch, DataQualifier,
            SubscriptGroup, ReferenceModification, ProcedureQualifier, ProcedureReference,
            ProcedureSignature, ProcedureParameter, StatementOperand, StatementClause,
            Statement, Expression, DataClause {
        Meta meta();
    }

    public enum DivisionKind { IDENTIFICATION, ENVIRONMENT, DATA, PROCEDURE }
    public enum SentenceTerminator { PERIOD, END_OF_PROCEDURE }
    public enum PassingMode { REFERENCE, VALUE, CONTENT }
    public enum EmbeddedLanguage { SQL, CICS, SQLIMS, UNKNOWN }
    /** Syntactic form only; linkage is compiler-option-dependent and belongs to resolution. */
    public enum CallTargetSyntax { LITERAL_PROGRAM_NAME, IDENTIFIER_OR_EXPRESSION }
    public enum PerformKind { INLINE, PROCEDURE }
    public enum GoToKind { SIMPLE, DEPENDING_ON }
    public enum QualifierConnector { OF, IN }
    public enum QualifierTarget { DATA, FILE, DATA_OR_FILE }
    public enum ReferenceUnderstanding { STRUCTURED, PRESERVED }
    public enum DataSectionKind { FILE, DATABASE, WORKING_STORAGE, LINKAGE, COMMUNICATION, LOCAL_STORAGE, SCREEN, REPORT, PROGRAM_LIBRARY }
    public enum DataLevelKind { GROUP_OR_ELEMENTARY, STANDALONE_77, RENAMES_66, CONDITION_88, OPAQUE }
    public enum CallArgumentKind { VALUE, OMITTED, ADDRESS_OF, LENGTH_OF }
    public enum DeclarationVisibility { LOCAL, GLOBAL, EXTERNAL, CONFLICTING }

    public record ProgramAttributes(boolean common, boolean initial, boolean recursive,
                                    boolean library, boolean definition, String writtenText) {
        public ProgramAttributes { writtenText = Objects.requireNonNullElse(writtenText, ""); }
        public static ProgramAttributes none() {
            return new ProgramAttributes(false, false, false, false, false, "");
        }
    }

    public record Program(Meta meta, String name, ProgramAttributes attributes,
                          List<Division> divisions) implements Node {
        public Program {
            attributes = Objects.requireNonNull(attributes, "attributes");
            divisions = List.copyOf(divisions);
        }
        public Program(Meta meta, String name, List<Division> divisions) {
            this(meta, name, ProgramAttributes.none(), divisions);
        }
    }

    public record Division(Meta meta, DivisionKind divisionKind, List<Node> children) implements Node {
        public Division { children = List.copyOf(children); }
    }

    public record Section(Meta meta, String name, DataSectionKind dataSectionKind, List<Node> children) implements Node {
        public Section { children = List.copyOf(children); }
        public Section(Meta meta, String name, List<Node> children) { this(meta, name, null, children); }
    }

    public record FileBinding(Meta meta, String logicalName, String assignment) implements Node {}

    public record FileDescription(Meta meta, String fileName, DeclarationVisibility visibility,
                                  List<DataEntry> entries) implements Node {
        public FileDescription { entries = List.copyOf(entries); }
        public FileDescription(Meta meta, String fileName, List<DataEntry> entries) {
            this(meta, fileName, DeclarationVisibility.LOCAL, entries);
        }
    }

    public record DataEntry(Meta meta, String level, DataLevelKind levelKind, String name, boolean filler,
                            DeclarationVisibility visibility, String declaration,
                            List<DataClause> clauses, List<DataEntry> children) implements Node {
        public DataEntry {
            clauses = List.copyOf(clauses);
            children = List.copyOf(children);
        }
        public DataEntry(Meta meta, String level, DataLevelKind levelKind, String name, boolean filler,
                         String declaration, List<DataClause> clauses, List<DataEntry> children) {
            this(meta, level, levelKind, name, filler, DeclarationVisibility.LOCAL,
                    declaration, clauses, children);
        }
    }
    public sealed interface DataClause extends Node permits PictureClause, UsageClause, ValueClause,
            RedefinesClause, RenamesClause, OccursClause, PreservedDataClause {}
    public record PictureClause(Meta meta, String picture, String writtenText) implements DataClause {}
    public record UsageClause(Meta meta, String usage, String writtenText) implements DataClause {}
    public record ValueClause(Meta meta, List<String> values, String writtenText) implements DataClause {
        public ValueClause { values = List.copyOf(values); }
    }
    public record RedefinesClause(Meta meta, DataReference target, String writtenText) implements DataClause {}
    public record RenamesClause(Meta meta, DataReference from, DataReference through,
                                String writtenText) implements DataClause {}
    public record OccursClause(Meta meta, Expression minimum, Expression maximum,
                               DataReference dependingOn, List<DataReference> keys,
                               List<IndexReference> indexes, String writtenText) implements DataClause {
        public OccursClause {
            keys = List.copyOf(keys);
            indexes = List.copyOf(indexes);
        }
    }
    public record PreservedDataClause(Meta meta, String grammarRule, String writtenText,
                                      List<Node> recognizedReferences) implements DataClause {
        public PreservedDataClause { recognizedReferences = List.copyOf(recognizedReferences); }
    }
    public record ProcedureSignature(Meta meta, boolean chaining, List<ProcedureParameter> parameters,
                                     DataReference returning, String writtenText) implements Node {
        public ProcedureSignature { parameters = List.copyOf(parameters); }
    }
    public record ProcedureParameter(Meta meta, PassingMode passingMode, Expression reference,
                                     boolean optional, boolean any, String writtenText) implements Node {}

    public record Paragraph(Meta meta, String name, List<Sentence> sentences) implements Node {
        public Paragraph { sentences = List.copyOf(sentences); }
    }

    public record Sentence(Meta meta, List<Statement> statements, SentenceTerminator terminator,
                           SourceSpan terminatorSpan) implements Node {
        public Sentence { statements = List.copyOf(statements); }
    }

    public sealed interface Statement extends Node permits CallStatement, IfStatement, EvaluateStatement,
            PerformStatement, GoToStatement, MoveStatement, EmbeddedLanguageStatement,
            NextSentenceStatement, ModeledStatement, PreservedStatement, UnsupportedStatement {}

    public sealed interface Expression extends Node permits LiteralExpression, DataReference,
            OperationExpression, FunctionExpression, SpecialRegisterExpression,
            FileReference, ProgramReference, IndexReference, NamedReference,
            PreservedExpression, RawExpression {}

    public record CallStatement(Meta meta, CallTargetSyntax targetSyntax, Expression target,
                                List<CallArgument> arguments, Expression returning,
                                List<Statement> exceptionFlow) implements Statement {
        public CallStatement {
            arguments = List.copyOf(arguments);
            exceptionFlow = List.copyOf(exceptionFlow);
        }
    }

    public record CallArgument(Meta meta, PassingMode passingMode, CallArgumentKind argumentKind,
                               Expression value, String writtenText) implements Node {}

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

    public record EvaluateBranch(Meta meta, List<Expression> selectorExpressions,
                                 String writtenSelector, boolean other,
                                 List<Statement> statements) implements Node {
        public EvaluateBranch {
            selectorExpressions = List.copyOf(selectorExpressions);
            statements = List.copyOf(statements);
        }
        public String selector() { return writtenSelector; }
    }

    public record PerformStatement(Meta meta, PerformKind performKind, ProcedureReference fromReference,
                                   ProcedureReference throughReference, String writtenControl,
                                   List<Expression> controlExpressions,
                                   List<Statement> inlineBody) implements Statement {
        public PerformStatement {
            controlExpressions = List.copyOf(controlExpressions);
            inlineBody = List.copyOf(inlineBody);
        }
        public String fromProcedure() { return fromReference == null ? "" : fromReference.writtenText(); }
        public String throughProcedure() { return throughReference == null ? "" : throughReference.writtenText(); }
        public String control() { return writtenControl; }
    }

    public record GoToStatement(Meta meta, GoToKind goToKind, List<ProcedureReference> targets,
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

    public record StatementOperand(Meta meta, String grammarRole, Node value) implements Node {}

    public record StatementClause(Meta meta, String grammarRule, String writtenText,
                                  List<Node> recognizedNodes,
                                  List<Statement> nestedStatements) implements Node {
        public StatementClause {
            recognizedNodes = List.copyOf(recognizedNodes);
            nestedStatements = List.copyOf(nestedStatements);
        }
    }

    public record ModeledStatement(Meta meta, String grammarRule, String writtenText,
                                   List<StatementOperand> operands,
                                   List<StatementClause> clauses) implements Statement {
        public ModeledStatement {
            operands = List.copyOf(operands);
            clauses = List.copyOf(clauses);
        }
    }

    public record PreservedStatement(Meta meta, String grammarRule, String writtenText,
                                     List<StatementOperand> operands,
                                     List<StatementClause> clauses) implements Statement {
        public PreservedStatement {
            operands = List.copyOf(operands);
            clauses = List.copyOf(clauses);
        }
    }

    /** Keeps unsupported syntax visible and retains any directly nested statements. */
    public record UnsupportedStatement(Meta meta, String grammarRule, String rawText,
                                       List<Node> recognizedReferences,
                                       List<Statement> nestedStatements) implements Statement {
        public UnsupportedStatement {
            recognizedReferences = List.copyOf(recognizedReferences);
            nestedStatements = List.copyOf(nestedStatements);
        }
    }

    public record LiteralExpression(Meta meta, String value, String rawLexeme) implements Expression {}
    public record DataQualifier(Meta meta, QualifierConnector connector, QualifierTarget target,
                                DataReference reference,
                                String writtenText) implements Node {
        public String name() { return reference.baseName(); }
    }
    public record SubscriptGroup(Meta meta, List<Expression> subscripts,
                                 String writtenText) implements Node {
        public SubscriptGroup { subscripts = List.copyOf(subscripts); }
    }
    public record ReferenceModification(Meta meta, Expression offset, Expression length,
                                        String writtenText) implements Node {}
    public record DataReference(Meta meta, String baseName, String writtenText,
                                List<DataQualifier> qualifiers, List<SubscriptGroup> subscriptGroups,
                                ReferenceModification referenceModification,
                                ReferenceUnderstanding understanding) implements Expression {
        public DataReference {
            qualifiers = List.copyOf(qualifiers);
            subscriptGroups = List.copyOf(subscriptGroups);
        }
        public String writtenName() { return writtenText; }
    }
    public record OperationExpression(Meta meta, String operator, List<Expression> operands,
                                      String writtenText) implements Expression {
        public OperationExpression { operands = List.copyOf(operands); }
    }
    public record FunctionExpression(Meta meta, String functionName, List<Expression> arguments,
                                     ReferenceModification referenceModification,
                                     String writtenText) implements Expression {
        public FunctionExpression { arguments = List.copyOf(arguments); }
    }
    public record SpecialRegisterExpression(Meta meta, String registerName,
                                            List<Expression> operands,
                                            String writtenText) implements Expression {
        public SpecialRegisterExpression { operands = List.copyOf(operands); }
    }
    public record ProcedureQualifier(Meta meta, QualifierConnector connector, String sectionName,
                                     String writtenText) implements Node {}
    public record ProcedureReference(Meta meta, String baseName, String writtenText,
                                     ProcedureQualifier qualifier) implements Node {}
    public record FileReference(Meta meta, String baseName, String writtenText) implements Expression {}
    public record ProgramReference(Meta meta, String programName, String writtenText) implements Expression {}
    public record IndexReference(Meta meta, String indexName, String writtenText) implements Expression {}
    public record NamedReference(Meta meta, String grammarKind, String writtenText) implements Expression {}
    public record PreservedExpression(Meta meta, String grammarRule, String writtenText,
                                      List<Expression> recognizedOperands,
                                      ReferenceUnderstanding understanding) implements Expression {
        public PreservedExpression { recognizedOperands = List.copyOf(recognizedOperands); }
    }
    public record RawExpression(Meta meta, String role, String rawText) implements Expression {}

    public static List<? extends Node> children(Node node) {
        if (node instanceof Program n) return n.divisions();
        if (node instanceof Division n) return n.children();
        if (node instanceof Section n) return n.children();
        if (node instanceof FileDescription n) return n.entries();
        if (node instanceof DataEntry n) {
            List<Node> result = new ArrayList<>(n.clauses());
            result.addAll(n.children());
            return result;
        }
        if (node instanceof Paragraph n) return n.sentences();
        if (node instanceof Sentence n) return n.statements();
        if (node instanceof CallStatement n) {
            List<Node> result = new ArrayList<>();
            result.add(n.target()); result.addAll(n.arguments());
            if (n.returning() != null) result.add(n.returning());
            result.addAll(n.exceptionFlow());
            return result;
        }
        if (node instanceof CallArgument n) return n.value() == null ? List.of() : List.of(n.value());
        if (node instanceof IfStatement n) {
            List<Node> result = new ArrayList<>();
            result.add(n.condition()); result.addAll(n.thenBranch()); result.addAll(n.elseBranch());
            return result;
        }
        if (node instanceof EvaluateStatement n) {
            List<Node> result = new ArrayList<>(n.subjects()); result.addAll(n.branches()); return result;
        }
        if (node instanceof EvaluateBranch n) {
            List<Node> result = new ArrayList<>(n.selectorExpressions()); result.addAll(n.statements()); return result;
        }
        if (node instanceof PerformStatement n) {
            List<Node> result = new ArrayList<>();
            if (n.fromReference() != null) result.add(n.fromReference());
            if (n.throughReference() != null) result.add(n.throughReference());
            result.addAll(n.controlExpressions()); result.addAll(n.inlineBody()); return result;
        }
        if (node instanceof GoToStatement n) {
            List<Node> result = new ArrayList<>(n.targets());
            if (n.dependingOn() != null) result.add(n.dependingOn()); return result;
        }
        if (node instanceof MoveStatement n) {
            List<Node> result = new ArrayList<>(); result.add(n.source()); result.addAll(n.targets()); return result;
        }
        if (node instanceof UnsupportedStatement n) {
            List<Node> result = new ArrayList<>(n.recognizedReferences()); result.addAll(n.nestedStatements()); return result;
        }
        if (node instanceof ModeledStatement n) {
            List<Node> result = new ArrayList<>(n.operands()); result.addAll(n.clauses()); return result;
        }
        if (node instanceof PreservedStatement n) {
            List<Node> result = new ArrayList<>(n.operands()); result.addAll(n.clauses()); return result;
        }
        if (node instanceof StatementOperand n) return List.of(n.value());
        if (node instanceof StatementClause n) {
            List<Node> result = new ArrayList<>(n.recognizedNodes());
            result.addAll(n.nestedStatements());
            return result;
        }
        if (node instanceof DataReference n) {
            List<Node> result = new ArrayList<>(n.qualifiers()); result.addAll(n.subscriptGroups());
            if (n.referenceModification() != null) result.add(n.referenceModification());
            return result;
        }
        if (node instanceof DataQualifier n) return List.of(n.reference());
        if (node instanceof ProcedureReference n) return n.qualifier() == null ? List.of() : List.of(n.qualifier());
        if (node instanceof ProcedureSignature n) {
            List<Node> result = new ArrayList<>(n.parameters());
            if (n.returning() != null) result.add(n.returning());
            return result;
        }
        if (node instanceof ProcedureParameter n) return n.reference() == null ? List.of() : List.of(n.reference());
        if (node instanceof RedefinesClause n) return List.of(n.target());
        if (node instanceof RenamesClause n) return n.through()==null?List.of(n.from()):List.of(n.from(),n.through());
        if (node instanceof OccursClause n) {
            List<Node> result = new ArrayList<>();
            if (n.minimum() != null) result.add(n.minimum());
            if (n.maximum() != null) result.add(n.maximum());
            if (n.dependingOn() != null) result.add(n.dependingOn());
            result.addAll(n.keys());
            result.addAll(n.indexes());
            return result;
        }
        if (node instanceof PreservedDataClause n) return n.recognizedReferences();
        if (node instanceof SubscriptGroup n) return n.subscripts();
        if (node instanceof ReferenceModification n) {
            if (n.length() == null) return List.of(n.offset());
            return List.of(n.offset(), n.length());
        }
        if (node instanceof OperationExpression n) return n.operands();
        if (node instanceof FunctionExpression n) {
            List<Node> result = new ArrayList<>(n.arguments());
            if (n.referenceModification() != null) result.add(n.referenceModification());
            return result;
        }
        if (node instanceof SpecialRegisterExpression n) return n.operands();
        if (node instanceof PreservedExpression n) return n.recognizedOperands();
        return List.of();
    }
}
