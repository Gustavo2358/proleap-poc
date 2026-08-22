package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.*;

/** Builds one immutable semantic AST from the ANTLR parse tree. */
final class AstBuilder {
    private final Parser parser;
    private final String source;
    private final IdentityHashMap<ParseTree, Integer> parseIds;
    private final IdentityHashMap<ParseTree, Integer> parseSubtreeSizes;
    private int nextId;

    AstBuilder(Parser parser, String source, IdentityHashMap<ParseTree, Integer> parseIds,
               IdentityHashMap<ParseTree, Integer> parseSubtreeSizes) {
        this.parser = parser;
        this.source = source;
        this.parseIds = parseIds;
        this.parseSubtreeSizes = parseSubtreeSizes;
    }

    Ast.Program build(ParseTree tree) {
        ParserRuleContext unit = firstDescendant(tree, "programUnit");
        if (unit == null) throw new IllegalStateException("programUnit not found");
        Ast.Meta meta = meta(unit);
        ParserRuleContext programName = firstDescendant(firstDescendant(unit, "programIdParagraph"), "programName");
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
        return new Ast.Program(meta, programName == null ? "<anonymous>" : clean(programName.getText()), divisions);
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
                    fileName == null ? "<unknown>" : clean(fileName.getText()),
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
                    for (ParserRuleContext data : directChildrenNamed(fd, "dataDescriptionEntry")) dataEntries.add(buildDataEntry(data));
                    entries.add(new Ast.FileDescription(fdMeta,
                            fileName == null ? "<unknown>" : clean(fileName.getText()), dataEntries));
                }
            } else {
                for (ParserRuleContext data : nearestDescendants(sectionContext, "dataDescriptionEntry")) entries.add(buildDataEntry(data));
            }
            sections.add(new Ast.Section(sectionMeta, displayRule(rule(sectionContext)), entries));
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
        return new Ast.DataEntry(meta, level, name == null ? "FILLER" : clean(name.getText()), raw);
    }

    private Ast.Division buildProcedure(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        List<Ast.Node> children = new ArrayList<>();
        ParserRuleContext body = firstDescendant(context, "procedureDivisionBody");
        if (body != null) {
            for (ParserRuleContext child : directRuleChildren(body)) {
                if (rule(child).equals("paragraphs")) children.addAll(buildParagraphGroup(child));
                else if (rule(child).equals("procedureSection")) children.add(buildProcedureSection(child));
            }
        }
        return new Ast.Division(meta, Ast.DivisionKind.PROCEDURE, children);
    }

    private Ast.Section buildProcedureSection(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext name = firstDescendant(context, "sectionName");
        ParserRuleContext paragraphs = firstDescendant(context, "paragraphs");
        return new Ast.Section(meta, name == null ? "<section>" : clean(name.getText()),
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
        return new Ast.Paragraph(meta, name == null ? "<paragraph>" : clean(name.getText()), sentences);
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
        return switch (rule(concrete)) {
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
            default -> buildUnsupported(concrete);
        };
    }

    private Ast.CallStatement buildCall(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext targetContext = firstDirectOrNearest(context, Set.of("identifier", "literal"));
        Ast.Expression target = expression(targetContext, "call target");
        Ast.CallTargetKind kind = target instanceof Ast.LiteralExpression
                ? Ast.CallTargetKind.STATIC_LITERAL : Ast.CallTargetKind.DYNAMIC_EXPRESSION;
        List<Ast.CallArgument> arguments = new ArrayList<>();
        for (ParserRuleContext arg : nearestDescendants(context, Set.of("callByReference", "callByValue", "callByContent"))) {
            Ast.PassingMode mode = switch (rule(arg)) {
                case "callByValue" -> Ast.PassingMode.VALUE;
                case "callByContent" -> Ast.PassingMode.CONTENT;
                default -> Ast.PassingMode.REFERENCE;
            };
            Ast.Meta argMeta = meta(arg);
            arguments.add(new Ast.CallArgument(argMeta, mode, expression(firstDirectOrNearest(arg,
                    Set.of("identifier", "literal", "fileName")), "argument")));
        }
        return new Ast.CallStatement(meta, kind, target, arguments, directNestedStatements(context));
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
        List<Ast.Expression> subjects = directChildrenNamed(context, "evaluateSelect").stream()
                .map(c -> expression(c, "subject")).toList();
        List<Ast.EvaluateBranch> branches = new ArrayList<>();
        for (ParserRuleContext branch : directChildrenNamed(context, "evaluateWhenPhrase")) {
            Ast.Meta branchMeta = meta(branch);
            List<String> selectors = directChildrenNamed(branch, "evaluateWhen").stream().map(this::sourceText).map(AstBuilder::compact).toList();
            branches.add(new Ast.EvaluateBranch(branchMeta, String.join(" ALSO ", selectors), false,
                    directChildrenNamed(branch, "statement").stream().map(this::buildStatement).toList()));
        }
        ParserRuleContext other = firstDescendant(context, "evaluateWhenOther");
        if (other != null) branches.add(new Ast.EvaluateBranch(meta(other), "OTHER", true, statementsInside(other)));
        return new Ast.EvaluateStatement(meta, subjects, branches, containsToken(context, "END-EVALUATE"));
    }

    private Ast.PerformStatement buildPerform(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext inline = firstDescendant(context, "performInlineStatement");
        ParserRuleContext procedure = firstDescendant(context, "performProcedureStatement");
        ParserRuleContext type = firstDescendant(context, "performType");
        if (inline != null) return new Ast.PerformStatement(meta, Ast.PerformKind.INLINE, "", "",
                type == null ? "once" : compact(sourceText(type)), statementsInside(inline));
        List<ParserRuleContext> names = procedure == null ? List.of() : nearestDescendants(procedure, "procedureName");
        return new Ast.PerformStatement(meta, Ast.PerformKind.PROCEDURE,
                names.isEmpty() ? "<unknown>" : clean(names.get(0).getText()),
                names.size() < 2 ? "" : clean(names.get(1).getText()),
                type == null ? "once" : compact(sourceText(type)), List.of());
    }

    private Ast.GoToStatement buildGoTo(ParserRuleContext context) {
        Ast.Meta meta = meta(context);
        ParserRuleContext depending = firstDescendant(context, "goToDependingOnStatement");
        List<String> targets = nearestDescendants(context, "procedureName").stream().map(ParseTree::getText).map(AstBuilder::clean).toList();
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
        return new Ast.UnsupportedStatement(meta, rule(context), compact(sourceText(context)), directNestedStatements(context));
    }

    private Ast.Expression expression(ParserRuleContext context, String role) {
        if (context == null) return new Ast.RawExpression(syntheticMeta(role), role, "<missing>");
        String contextRule = rule(context);
        ParserRuleContext literal = contextRule.equals("literal") ? context : firstDescendant(context, "literal");
        if (literal != null && compact(sourceText(context)).equals(compact(sourceText(literal)))) {
            String raw = literal.getText();
            return new Ast.LiteralExpression(meta(literal), unquote(raw), raw);
        }
        if (contextRule.equals("identifier") || contextRule.equals("fileName"))
            return new Ast.DataReference(meta(context), clean(context.getText()));
        ParserRuleContext identifier = firstDescendant(context, "identifier");
        if (identifier != null && compact(sourceText(context)).equals(compact(sourceText(identifier))))
            return new Ast.DataReference(meta(identifier), clean(identifier.getText()));
        return new Ast.RawExpression(meta(context), role, compact(sourceText(context)));
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
        return new Ast.Meta(id, new Ast.SourceSpan(startLine, startColumn, endLine, endColumn, startToken, endToken),
                new Ast.ParseTreeOrigin(parseIds.getOrDefault(context, -1), rule(context),
                        parseSubtreeSizes.getOrDefault(context, 1)));
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
        return start >= end ? context.getText() : source.substring(start, end);
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

    private static String displayRule(String rule) {
        if (rule == null || rule.isBlank()) return "Section";
        return Character.toUpperCase(rule.charAt(0)) + rule.substring(1).replaceAll("([a-z])([A-Z])", "$1 $2");
    }
}
