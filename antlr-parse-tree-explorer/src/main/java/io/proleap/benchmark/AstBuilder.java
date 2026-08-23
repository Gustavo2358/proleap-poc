package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.*;

/** Builds one immutable semantic AST from the ANTLR parse tree. */
final class AstBuilder {
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

    AstBuilder(Parser parser, String source, IdentityHashMap<ParseTree, Integer> parseIds,
               IdentityHashMap<ParseTree, Integer> parseSubtreeSizes) {
        this(parser, source, SourceMap.identity(source, "<preprocessed>"), parseIds, parseSubtreeSizes);
    }

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
        nextId = 0;
        coverageDrafts.clear();
        semanticDiagnostics.clear();
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
        Ast.Program program = new Ast.Program(meta,
                programName == null ? "<anonymous>" : clean(sourceText(programName)), divisions);
        return new AstBuildResult(program, buildCoverageReport(), semanticDiagnostics);
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
                    for (ParserRuleContext data : directChildrenNamed(fd, "dataDescriptionEntry")) dataEntries.add(buildDataEntry(data));
                    entries.add(new Ast.FileDescription(fdMeta,
                            fileName == null ? "<unknown>" : clean(sourceText(fileName)), dataEntries));
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
        return new Ast.DataEntry(meta, level, name == null ? "FILLER" : clean(sourceText(name)), raw);
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
        Ast.Statement statement = switch (rule(concrete)) {
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
        coverageDrafts.add(new CoverageDraft(rule(concrete), statement.meta(), sourceText(concrete),
                statement.meta().id()));
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
        Ast.CallTargetKind kind = target instanceof Ast.ProgramReference
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
        for (ParserRuleContext qualifier : contexts) {
            String written = sourceText(qualifier).strip();
            Ast.QualifierConnector connector = containsToken(qualifier, "IN")
                    ? Ast.QualifierConnector.IN : Ast.QualifierConnector.OF;
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
            result.add(new Ast.DataQualifier(qualifierMeta, connector, reference, written));
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
