#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

const ARTIFACTS = Object.freeze({
  tree: ["tree-data.js", "PARSE_TREE_DATA"],
  ast: ["ast-data.js", "AST_DATA"],
  coverage: ["coverage-data.js", "SEMANTIC_COVERAGE_DATA"],
  symbols: ["symbol-data.js", "SYMBOL_TABLE_DATA"],
  resolution: ["resolution-data.js", "RESOLUTION_DATA"],
});

function fail(location, message) {
  throw new Error(`${location}: ${message}`);
}

function invariant(condition, location, message) {
  if (!condition) fail(location, message);
}

function array(value, location) {
  invariant(Array.isArray(value), location, "expected an array");
  return value;
}

function object(value, location) {
  invariant(value !== null && typeof value === "object" && !Array.isArray(value),
      location, "expected an object");
  return value;
}

export function loadWindowData(file, globalName) {
  const text = fs.readFileSync(file, "utf8").trim();
  const prefix = `window.${globalName}=`;
  invariant(text.startsWith(prefix), file, `expected exact prefix ${prefix}`);
  invariant(text.endsWith(";"), file, "expected a single trailing semicolon");
  const payload = text.slice(prefix.length, -1);
  invariant(!payload.trimEnd().endsWith(";"), file, "unexpected executable content after JSON payload");
  try {
    return JSON.parse(payload);
  } catch (error) {
    fail(file, `invalid JSON payload: ${error.message}`);
  }
}

export function assertDeclaredCount(location, declared, values) {
  invariant(Number.isInteger(declared) && declared >= 0, location,
      `invalid declared count ${String(declared)}`);
  invariant(Array.isArray(values), location, "inventory is not an array");
  invariant(declared === values.length, location,
      `declared ${declared}, inventory contains ${values.length}`);
}

function countBy(values, classifier) {
  const counts = {};
  for (const value of values) {
    const key = classifier(value);
    counts[key] = (counts[key] ?? 0) + 1;
  }
  return counts;
}

export function assertCountMap(location, declaredValue, values, classifier) {
  const declared = object(declaredValue, location);
  const actual = countBy(values, classifier);
  for (const key of new Set([...Object.keys(declared), ...Object.keys(actual)])) {
    invariant(Number.isInteger(declared[key] ?? 0), `${location}.${key}`,
        "declared count must be an integer");
    invariant((declared[key] ?? 0) === (actual[key] ?? 0), `${location}.${key}`,
        `declared ${declared[key] ?? 0}, derived ${actual[key] ?? 0}`);
  }
}

export function assertCandidateCardinality(location, entry) {
  const candidates = array(entry.candidates, `${location}.candidates`);
  const pair = `${entry.status}/${entry.reason}`;
  switch (pair) {
    case "RESOLVED/UNIQUE_VISIBLE_DECLARATION":
    case "RESOLVED/QUALIFIED_HIERARCHY_MATCH":
      invariant(candidates.length === 1, `${location}.candidates`,
          "resolved entry must select exactly one candidate");
      return;
    case "AMBIGUOUS/MULTIPLE_VALID_CANDIDATES":
      invariant(candidates.length > 1, `${location}.candidates`,
          "ambiguous entry must preserve multiple candidates");
      return;
    case "EXTERNAL_OBSERVED/LITERAL_EXTERNAL_PROGRAM":
    case "UNRESOLVED/DECLARATION_NOT_FOUND":
    case "UNRESOLVED/INPUT_INCOMPLETE":
    case "UNRESOLVED/INVALID_NAMESPACE_FOR_CONTEXT":
    case "UNSUPPORTED/UNSUPPORTED_GRAMMAR_FORM":
      invariant(candidates.length === 0, `${location}.candidates`,
          `${pair} must not contain a candidate`);
      return;
    case "UNSUPPORTED/UNSUPPORTED_DIALECT_OPTION":
      // An unavailable compiler option prevents selection, not preservation of possibilities.
      return;
    default:
      fail(`${location}.status`, `unsupported status/reason combination ${pair}`);
  }
}

function occurrenceKey(unitId, occurrenceId) {
  return `${unitId}\u0000${occurrenceId}`;
}

export function assertOccurrenceIdentity(location, entries, diagnostics, gaps, unitIds) {
  const occurrences = new Set();
  const localIdsByUnit = new Map();
  entries.forEach((entry, index) => {
    invariant(unitIds.has(entry.unitId), `${location}.entries[${index}].unitId`,
        `unknown unit ${String(entry.unitId)}`);
    invariant(Number.isInteger(entry.occurrenceId) && entry.occurrenceId >= 0,
        `${location}.entries[${index}].occurrenceId`, "expected a non-negative integer");
    const key = occurrenceKey(entry.unitId, entry.occurrenceId);
    invariant(!occurrences.has(key), `${location}.entries[${index}].occurrenceId`,
        `duplicate occurrence ${entry.occurrenceId} in unit ${entry.unitId}`);
    occurrences.add(key);
    const localIds = localIdsByUnit.get(entry.unitId) ?? new Set();
    localIds.add(entry.occurrenceId);
    localIdsByUnit.set(entry.unitId, localIds);
  });
  for (const [unitId, localIds] of localIdsByUnit) {
    for (let occurrenceId = 0; occurrenceId < localIds.size; occurrenceId++)
      invariant(localIds.has(occurrenceId), `${location}.entries`,
          `unit ${unitId} is missing local occurrence ${occurrenceId}`);
  }

  diagnostics.forEach((diagnostic, index) => {
    invariant(unitIds.has(diagnostic.unitId), `${location}.diagnostics[${index}].unitId`,
        `unknown unit ${String(diagnostic.unitId)}`);
    invariant(Number.isInteger(diagnostic.occurrenceId) && diagnostic.occurrenceId >= 0,
        `${location}.diagnostics[${index}].occurrenceId`, "expected a non-negative integer");
    invariant(occurrences.has(occurrenceKey(diagnostic.unitId, diagnostic.occurrenceId)),
        `${location}.diagnostics[${index}].occurrenceId`,
        `unknown occurrence ${diagnostic.occurrenceId} in unit ${diagnostic.unitId}`);
  });
  gaps.forEach((gap, index) => {
    invariant(Number.isInteger(gap.occurrenceId) && gap.occurrenceId >= -1,
        `${location}.gaps[${index}].occurrenceId`, "expected -1 or a non-negative integer");
    if (gap.unitId === null) {
      invariant(gap.occurrenceId === -1, `${location}.gaps[${index}].occurrenceId`,
          "global gap must use occurrenceId -1");
      return;
    }
    invariant(unitIds.has(gap.unitId), `${location}.gaps[${index}].unitId`,
        `unknown unit ${String(gap.unitId)}`);
    if (gap.occurrenceId === -1) return;
    invariant(occurrences.has(occurrenceKey(gap.unitId, gap.occurrenceId)),
        `${location}.gaps[${index}].occurrenceId`,
        `unknown occurrence ${gap.occurrenceId} in unit ${gap.unitId}`);
  });
}

function assertIndexed(location, values) {
  values.forEach((value, index) => {
    object(value, `${location}[${index}]`);
    invariant(value.id === index, `${location}[${index}].id`,
        `expected deterministic id ${index}, found ${String(value.id)}`);
  });
}

function assertParentTree(location, values, parentField) {
  const children = new Array(values.length).fill(0);
  values.forEach((value, index) => {
    const parent = value[parentField];
    invariant(Number.isInteger(parent), `${location}[${index}].${parentField}`, "expected an integer");
    invariant(parent === -1 || (parent >= 0 && parent < index),
        `${location}[${index}].${parentField}`, `invalid parent ${parent}`);
    if (parent >= 0) children[parent]++;
  });
  return children;
}

function assertSource(location, data, expectedSource) {
  invariant(data.meta?.source === expectedSource, `${location}.meta.source`,
      `expected ${expectedSource}, found ${String(data.meta?.source)}`);
}

function assertTree(tree, expectedSource) {
  const location = "tree-data.js";
  object(tree.meta, `${location}.meta`);
  assertSource(location, tree, expectedSource);
  const nodes = array(tree.nodes, `${location}.nodes`);
  assertDeclaredCount(`${location}.meta.nodes`, tree.meta.nodes, nodes);
  assertIndexed(`${location}.nodes`, nodes);
  const children = assertParentTree(`${location}.nodes`, nodes, "p");
  nodes.forEach((node, index) => invariant(node.q === children[index],
      `${location}.nodes[${index}].q`, `declared ${String(node.q)}, derived ${children[index]}`));
  assertCountMap(`${location}.ruleCounts`, tree.ruleCounts,
      nodes.filter(node => node.k === "rule"), node => node.n);
  array(tree.diagnostics, `${location}.diagnostics`);
  invariant(tree.meta.lexerErrors === 0, `${location}.meta.lexerErrors`, "expected zero lexer errors");
  invariant(tree.meta.parserErrors === 0, `${location}.meta.parserErrors`, "expected zero parser errors");
}

function assertAst(ast, tree, expectedSource) {
  const location = "ast-data.js";
  object(ast.meta, `${location}.meta`);
  assertSource(location, ast, expectedSource);
  const nodes = array(ast.nodes, `${location}.nodes`);
  assertDeclaredCount(`${location}.meta.nodes`, ast.meta.nodes, nodes);
  invariant(ast.meta.parseTreeNodes === tree.nodes.length, `${location}.meta.parseTreeNodes`,
      `declared ${String(ast.meta.parseTreeNodes)}, parse tree contains ${tree.nodes.length}`);
  assertIndexed(`${location}.nodes`, nodes);
  const children = assertParentTree(`${location}.nodes`, nodes, "p");
  nodes.forEach((node, index) => {
    invariant(node.q === children[index], `${location}.nodes[${index}].q`,
        `declared ${String(node.q)}, derived ${children[index]}`);
    invariant(Number.isInteger(node.r) && (node.r === -1 || (node.r >= 0 && node.r < tree.nodes.length)),
        `${location}.nodes[${index}].r`, `invalid parse-tree id ${String(node.r)}`);
  });
  assertCountMap(`${location}.typeCounts`, ast.typeCounts, nodes, node => node.t);
  const maxDepth = nodes.reduce((maximum, node) => Math.max(maximum, node.d), 0);
  invariant(ast.meta.maxDepth === maxDepth, `${location}.meta.maxDepth`,
      `declared ${String(ast.meta.maxDepth)}, derived ${maxDepth}`);

  const calls = nodes.filter(node => node.t === "CallStatement");
  const literalCalls = calls.filter(node => node.a?.targetSyntax === "LITERAL_PROGRAM_NAME");
  assertDeclaredCount(`${location}.meta.literalTargetCalls`, ast.meta.literalTargetCalls, literalCalls);
  assertDeclaredCount(`${location}.meta.identifierTargetCalls`, ast.meta.identifierTargetCalls,
      calls.filter(node => node.a?.targetSyntax !== "LITERAL_PROGRAM_NAME"));
  assertDeclaredCount(`${location}.meta.embeddedLanguages`, ast.meta.embeddedLanguages,
      nodes.filter(node => node.t === "EmbeddedLanguageStatement"));
  assertDeclaredCount(`${location}.meta.unsupportedStatements`, ast.meta.unsupportedStatements,
      nodes.filter(node => node.t === "UnsupportedStatement"));
  assertDeclaredCount(`${location}.meta.preservedStatements`, ast.meta.preservedStatements,
      nodes.filter(node => node.t === "PreservedStatement"));

  const derivedParseToAst = {};
  for (const node of nodes) {
    if (node.r < 0) continue;
    (derivedParseToAst[node.r] ??= []).push(node.id);
  }
  invariant(JSON.stringify(ast.parseToAst) === JSON.stringify(derivedParseToAst),
      `${location}.parseToAst`, "index does not match AST node parse origins");
}

function astTypeCount(ast, type) {
  return ast.nodes.filter(node => node.t === type).length;
}

function dataReferencesContaining(ast, childType) {
  const references = new Set();
  for (const child of ast.nodes.filter(node => node.t === childType)) {
    let parent = child.p;
    while (parent >= 0) {
      const candidate = ast.nodes[parent];
      if (candidate.t === "DataReference") {
        references.add(candidate.id);
        break;
      }
      parent = candidate.p;
    }
    invariant(parent >= 0, `ast-data.js ${childType} node ${child.id}`,
        "must be contained by a DataReference");
  }
  return references.size;
}

function assertCoverage(coverage, ast, tree, expectedSource) {
  const location = "coverage-data.js";
  object(coverage.meta, `${location}.meta`);
  assertSource(location, coverage, expectedSource);
  invariant(coverage.meta.unresolvedCopies === tree.meta.unresolvedCopies,
      `${location}.meta.unresolvedCopies`, "does not match frontend snapshot");
  invariant(coverage.meta.lexerErrors === tree.meta.lexerErrors,
      `${location}.meta.lexerErrors`, "does not match frontend snapshot");
  invariant(coverage.meta.parserErrors === tree.meta.parserErrors,
      `${location}.meta.parserErrors`, "does not match frontend snapshot");

  const findings = array(coverage.findings, `${location}.findings`);
  assertIndexed(`${location}.findings`, findings);
  assertCountMap(`${location}.constructionCounts`, coverage.constructionCounts,
      findings, finding => finding.coverage);
  assertCountMap(`${location}.dependencyCounts`, coverage.dependencyCounts,
      findings, finding => finding.dependency);
  findings.forEach((finding, index) => {
    invariant(Number.isInteger(finding.ast) && finding.ast >= 0 && finding.ast < ast.nodes.length,
        `${location}.findings[${index}].ast`, `invalid AST id ${String(finding.ast)}`);
    invariant(Number.isInteger(finding.parse) && finding.parse >= 0 && finding.parse < tree.nodes.length,
        `${location}.findings[${index}].parse`, `invalid parse-tree id ${String(finding.parse)}`);
    invariant(typeof finding.sourceFile === "string" && finding.sourceFile.length > 0,
        `${location}.findings[${index}].sourceFile`, "missing provenance source file");
  });

  const directMetrics = {
    dataReferences: "DataReference",
    procedureReferences: "ProcedureReference",
    fileReferences: "FileReference",
    programReferences: "ProgramReference",
    modeledStatements: "ModeledStatement",
    preservedStatements: "PreservedStatement",
    preservedDataClauses: "PreservedDataClause",
    opaqueExpressions: "PreservedExpression",
    embeddedLanguages: "EmbeddedLanguageStatement",
  };
  for (const [metric, type] of Object.entries(directMetrics)) {
    const derived = astTypeCount(ast, type);
    invariant(coverage.metrics?.[metric] === derived, `${location}.metrics.${metric}`,
        `declared ${String(coverage.metrics?.[metric])}, AST contains ${derived} ${type} nodes`);
  }
  const preservedDataReferences = ast.nodes.filter(node =>
    node.t === "DataReference" && node.a?.understanding === "PRESERVED").length;
  invariant(coverage.metrics?.preservedDataReferences === preservedDataReferences,
      `${location}.metrics.preservedDataReferences`,
      `declared ${String(coverage.metrics?.preservedDataReferences)}, derived ${preservedDataReferences}`);
  for (const [metric, childType] of [["qualifiedDataReferences", "DataQualifier"],
    ["subscriptedDataReferences", "SubscriptGroup"],
    ["modifiedDataReferences", "ReferenceModification"]]) {
    const derived = dataReferencesContaining(ast, childType);
    invariant(coverage.metrics?.[metric] === derived, `${location}.metrics.${metric}`,
        `declared ${String(coverage.metrics?.[metric])}, derived ${derived}`);
  }

  const blockers = array(coverage.blockingReasons, `${location}.blockingReasons`);
  invariant(coverage.meta.complete === (blockers.length === 0), `${location}.meta.complete`,
      "must be true exactly when blockingReasons is empty");
  if (coverage.meta.unresolvedCopies > 0) {
    invariant(!coverage.meta.complete, `${location}.meta.complete`,
        "cannot be complete while COPY inputs are unresolved");
  }

  const embeddedNodes = ast.nodes.filter(node => node.t === "EmbeddedLanguageStatement");
  for (const embedded of embeddedNodes) {
    const matches = findings.filter(finding => finding.ast === embedded.id);
    invariant(matches.length === 1, `${location}.findings`,
        `embedded AST node ${embedded.id} must have exactly one coverage finding`);
    invariant(matches[0].coverage === "PRESERVED_UNINTERPRETED"
        && matches[0].dependency === "DEPENDENCY_UNKNOWN",
    `${location}.findings[${matches[0].id}]`,
    "embedded language must remain preserved with unknown dependency knowledge");
  }
}

function assertSymbols(symbols, ast, expectedSource) {
  const location = "symbol-data.js";
  object(symbols.meta, `${location}.meta`);
  assertSource(location, symbols, expectedSource);
  const scopes = array(symbols.scopes, `${location}.scopes`);
  const inventory = array(symbols.symbols, `${location}.symbols`);
  const diagnostics = array(symbols.diagnostics, `${location}.diagnostics`);
  assertDeclaredCount(`${location}.meta.scopes`, symbols.meta.scopes, scopes);
  assertDeclaredCount(`${location}.meta.symbols`, symbols.meta.symbols, inventory);
  assertDeclaredCount(`${location}.meta.diagnostics`, symbols.meta.diagnostics, diagnostics);
  assertIndexed(`${location}.scopes`, scopes);
  assertIndexed(`${location}.symbols`, inventory);
  scopes.forEach((scope, index) => {
    invariant(scope.p === -1 || (scope.p >= 0 && scope.p < index),
        `${location}.scopes[${index}].p`, `invalid parent scope ${String(scope.p)}`);
    invariant(scope.o === -1 || (scope.o >= 0 && scope.o < inventory.length),
        `${location}.scopes[${index}].o`, `invalid owner symbol ${String(scope.o)}`);
    invariant(scope.a === -1 || (scope.a >= 0 && scope.a < ast.nodes.length),
        `${location}.scopes[${index}].a`, `invalid AST id ${String(scope.a)}`);
  });
  inventory.forEach((symbol, index) => {
    invariant(symbol.s >= 0 && symbol.s < scopes.length, `${location}.symbols[${index}].s`,
        `invalid scope id ${String(symbol.s)}`);
    invariant(symbol.a >= 0 && symbol.a < ast.nodes.length, `${location}.symbols[${index}].a`,
        `invalid AST id ${String(symbol.a)}`);
  });
  assertCountMap(`${location}.kindCounts`, symbols.kindCounts, inventory, symbol => symbol.k);
  const namespaceCounts = countBy(inventory, symbol => symbol.ns);
  for (const [field, namespace] of [["dataSymbols", "DATA"], ["procedureSymbols", "PROCEDURE"],
    ["fileSymbols", "FILE"]]) {
    invariant(symbols.meta[field] === (namespaceCounts[namespace] ?? 0), `${location}.meta.${field}`,
        `declared ${String(symbols.meta[field])}, derived ${namespaceCounts[namespace] ?? 0}`);
  }
  diagnostics.forEach((diagnostic, index) => array(diagnostic.symbols,
      `${location}.diagnostics[${index}].symbols`).forEach(symbolId => invariant(
      Number.isInteger(symbolId) && symbolId >= 0 && symbolId < inventory.length,
      `${location}.diagnostics[${index}].symbols`, `invalid symbol id ${String(symbolId)}`)));
}

function assertResolution(resolution, ast, tree, symbols, expectedSource) {
  const location = "resolution-data.js";
  object(resolution.meta, `${location}.meta`);
  assertSource(location, resolution, expectedSource);
  const units = array(resolution.units, `${location}.units`);
  const entries = array(resolution.entries, `${location}.entries`);
  const gaps = array(resolution.gaps, `${location}.gaps`);
  const diagnostics = array(resolution.diagnostics, `${location}.diagnostics`);
  const relations = array(resolution.relations, `${location}.relations`);
  assertDeclaredCount(`${location}.meta.programUnits`, resolution.meta.programUnits, units);
  assertDeclaredCount(`${location}.meta.references`, resolution.meta.references, entries);
  assertDeclaredCount(`${location}.metrics.collectedReferences`,
      resolution.metrics?.collectedReferences, entries);
  assertDeclaredCount(`${location}.meta.gaps`, resolution.meta.gaps, gaps);
  assertIndexed(`${location}.entries`, entries);
  assertIndexed(`${location}.gaps`, gaps);
  assertIndexed(`${location}.diagnostics`, diagnostics);
  assertIndexed(`${location}.relations`, relations);

  const unitIds = new Set(units.map(unit => unit.id));
  invariant(unitIds.size === units.length, `${location}.units`, "unit ids must be unique");
  entries.forEach((entry, index) => {
    invariant(entry.astNodeId >= 0 && entry.astNodeId < ast.nodes.length,
        `${location}.entries[${index}].astNodeId`, `invalid AST id ${String(entry.astNodeId)}`);
    invariant(entry.parseNodeId >= 0 && entry.parseNodeId < tree.nodes.length,
        `${location}.entries[${index}].parseNodeId`, `invalid parse-tree id ${String(entry.parseNodeId)}`);
    invariant(entry.scopeId >= 0 && entry.scopeId < symbols.scopes.length,
        `${location}.entries[${index}].scopeId`, `invalid scope id ${String(entry.scopeId)}`);
    array(entry.diagnosticIds, `${location}.entries[${index}].diagnosticIds`);
    array(entry.candidates, `${location}.entries[${index}].candidates`);
  });

  assertCountMap(`${location}.counts.status`, resolution.counts?.status, entries, entry => entry.status);
  assertCountMap(`${location}.counts.reason`, resolution.counts?.reason, entries, entry => entry.reason);
  assertCountMap(`${location}.counts.syntacticKind`, resolution.counts?.syntacticKind,
      entries, entry => entry.kind);
  assertCountMap(`${location}.counts.role`, resolution.counts?.role, entries, entry => entry.role);
  const resolvedEntries = entries.filter(entry => entry.status === "RESOLVED");
  entries.forEach(entry => assertCandidateCardinality(`${location}.entries[${entry.id}]`, entry));
  assertCountMap(`${location}.counts.resolvedSemanticKind`, resolution.counts?.resolvedSemanticKind,
      resolvedEntries, entry => entry.candidates[0].kind);

  assertOccurrenceIdentity(location, entries, diagnostics, gaps, unitIds);

  const diagnosticIds = new Set(diagnostics.map(diagnostic => diagnostic.id));
  entries.forEach((entry, index) => entry.diagnosticIds.forEach(diagnosticId => invariant(
      diagnosticIds.has(diagnosticId), `${location}.entries[${index}].diagnosticIds`,
      `unknown diagnostic ${String(diagnosticId)}`)));
  relations.forEach((relation, index) => {
    invariant(unitIds.has(relation.unitId), `${location}.relations[${index}].unitId`,
        `unknown unit ${String(relation.unitId)}`);
    invariant(relation.referenceAstNodeId >= 0 && relation.referenceAstNodeId < ast.nodes.length,
        `${location}.relations[${index}].referenceAstNodeId`,
        `invalid AST id ${String(relation.referenceAstNodeId)}`);
  });

  const hasGlobalInputGap = gaps.some(gap => gap.unitId === null);
  for (const unit of units) {
    const unitEntries = entries.filter(entry => entry.unitId === unit.id);
    const unitGaps = gaps.filter(gap => gap.unitId === unit.id);
    invariant(unit.references === unitEntries.length, `${location}.units[${unit.id}].references`,
        `declared ${String(unit.references)}, derived ${unitEntries.length}`);
    for (const [field, status] of [["resolved", "RESOLVED"],
      ["externalObserved", "EXTERNAL_OBSERVED"], ["ambiguous", "AMBIGUOUS"],
      ["unresolved", "UNRESOLVED"], ["unsupported", "UNSUPPORTED"]]) {
      const derived = unitEntries.filter(entry => entry.status === status).length;
      invariant(unit[field] === derived, `${location}.units[${unit.id}].${field}`,
          `declared ${String(unit[field])}, derived ${derived}`);
    }
    const derivedGaps = unitGaps.length + (hasGlobalInputGap ? 1 : 0);
    invariant(unit.gaps === derivedGaps, `${location}.units[${unit.id}].gaps`,
        `declared ${String(unit.gaps)}, derived ${derivedGaps}`);
    const bindingComplete = !hasGlobalInputGap
        && unitGaps.every(gap => gap.category === "CALL_SEMANTICS");
    invariant(unit.complete === bindingComplete, `${location}.units[${unit.id}].complete`,
        `declared ${String(unit.complete)}, derived ${bindingComplete}`);
  }

  const bindingComplete = gaps.every(gap => gap.category === "CALL_SEMANTICS");
  const dependencyReady = gaps.length === 0;
  invariant(resolution.meta.referenceBindingComplete === bindingComplete,
      `${location}.meta.referenceBindingComplete`, "does not match gap categories");
  invariant(resolution.meta.dependencyAnalysisReady === dependencyReady,
      `${location}.meta.dependencyAnalysisReady`, "must be true exactly when gaps are empty");
  invariant(resolution.meta.claim === (dependencyReady ? "COMPLETE" : "INCOMPLETE"),
      `${location}.meta.claim`, "does not match dependency readiness");
  const blockers = array(resolution.completeness?.blockingReasons,
      `${location}.completeness.blockingReasons`);
  invariant((blockers.length === 0) === dependencyReady,
      `${location}.completeness.blockingReasons`, "must be empty exactly when analysis is ready");
}

function assertCrossArtifactSources(bundle) {
  const sources = [bundle.tree.meta.source, bundle.ast.meta.source, bundle.coverage.meta.source,
    bundle.symbols.meta.source, bundle.resolution.meta.source];
  invariant(new Set(sources).size === 1, "artifacts.meta.source",
      `snapshots disagree: ${sources.join(", ")}`);
  invariant(JSON.stringify(bundle.tree.sourceLines) === JSON.stringify(bundle.ast.sourceLines),
      "ast-data.js.sourceLines", "does not match parse-tree source lines");
  invariant(JSON.stringify(bundle.tree.sourceLines) === JSON.stringify(bundle.symbols.sourceLines),
      "symbol-data.js.sourceLines", "does not match parse-tree source lines");
  invariant(JSON.stringify(bundle.tree.sourceLines) === JSON.stringify(bundle.resolution.sourceLines),
      "resolution-data.js.sourceLines", "does not match parse-tree source lines");
}

function assertCoactupc(bundle) {
  invariant(bundle.tree.meta.unresolvedCopies === 3, "tree-data.js.meta.unresolvedCopies",
      "COACTUPC evidence requires the three known unavailable COPY inputs");
  invariant(bundle.coverage.meta.complete === false, "coverage-data.js.meta.complete",
      "COACTUPC coverage must remain conservatively incomplete");
  invariant(bundle.resolution.meta.programUnits === 1, "resolution-data.js.meta.programUnits",
      "COACTUPC source declares one program unit");
  invariant(bundle.resolution.meta.referenceBindingComplete === false,
      "resolution-data.js.meta.referenceBindingComplete", "COACTUPC binding is known incomplete");
  invariant(bundle.resolution.meta.dependencyAnalysisReady === false,
      "resolution-data.js.meta.dependencyAnalysisReady", "COACTUPC dependency analysis is not ready");

  const program = bundle.ast.nodes.filter(node =>
    node.t === "Program" && node.a?.programName === "COACTUPC");
  assertDeclaredCount("ast-data.js COACTUPC program sentinel", 1, program);
  const calls = bundle.ast.nodes.filter(node => node.t === "CallStatement");
  assertDeclaredCount("ast-data.js COACTUPC CALL inventory", 1, calls);
  invariant(calls[0].a?.targetSyntax === "LITERAL_PROGRAM_NAME" && calls[0].n === "'CSUTLDTC'",
      `ast-data.js.nodes[${calls[0].id}]`, "expected the known literal CSUTLDTC CALL");

  const callEntries = bundle.resolution.entries.filter(entry => entry.role === "CALL_TARGET");
  assertDeclaredCount("resolution-data.js COACTUPC CALL_TARGET inventory", 1, callEntries);
  const call = callEntries[0];
  invariant(call.writtenText === "'CSUTLDTC'" && call.kind === "PROGRAM"
      && call.status === "EXTERNAL_OBSERVED" && call.reason === "LITERAL_EXTERNAL_PROGRAM"
      && call.callSemantics?.targetSyntax === "LITERAL_PROGRAM_NAME"
      && call.callSemantics?.linkage === "UNKNOWN",
  `resolution-data.js.entries[${call.id}]`, "literal external CALL semantics changed");
  invariant(call.provenance?.original?.file === "CSUTLDPY.cpy"
      && call.provenance?.includeChain?.some(frame => frame.requestedName === "CSUTLDPY"),
  `resolution-data.js.entries[${call.id}].provenance`, "CALL provenance no longer reaches its COPY");

  const wsRespSymbols = bundle.symbols.symbols.filter(symbol =>
    symbol.ns === "DATA" && symbol.n === "WS-RESP-CD" && symbol.k === "DATA_ITEM");
  assertDeclaredCount("symbol-data.js WS-RESP-CD declaration sentinel", 1, wsRespSymbols);
  const wsRespReads = bundle.resolution.entries.filter(entry => entry.writtenText === "WS-RESP-CD"
    && entry.role === "VALUE_READ" && entry.status === "RESOLVED"
    && entry.candidates.some(candidate => candidate.kind === "DATA"
      && candidate.writtenName === "WS-RESP-CD"));
  invariant(wsRespReads.length > 0, "resolution-data.js WS-RESP-CD sentinel",
      "known DATA read no longer resolves to the declared DATA candidate");

  for (const writtenText of ["ACCTSIDI OF CACTUPAI",
    "DFHCOMMAREA (1:LENGTH OF CARDDEMO-COMMAREA)"]) {
    invariant(bundle.ast.nodes.some(node => node.t === "DataReference" && node.n === writtenText),
        "ast-data.js written-form sentinel", `missing ${writtenText}`);
  }
  const embedded = bundle.ast.nodes.filter(node => node.t === "EmbeddedLanguageStatement");
  invariant(embedded.length > 0 && embedded.every(node => node.a?.language === "CICS"),
      "ast-data.js embedded-language sentinel", "known embedded statements must remain classified as CICS");
}

function assertCommentEntry(bundle) {
  invariant(bundle.ast.nodes.some(node => node.t === "Program" && node.n === "COMMENTBUG"),
      "ast-data.js COMMENTBUG sentinel", "program was not modeled");
  for (const division of ["IDENTIFICATION", "ENVIRONMENT", "DATA", "PROCEDURE"]) {
    invariant(bundle.ast.nodes.some(node => node.t === "Division" && node.a?.divisionKind === division),
        "ast-data.js division sentinel", `missing ${division} division`);
  }
  invariant(bundle.tree.nodes.some(node => node.k === "rule" && node.n === "environmentDivision"),
      "tree-data.js environmentDivision sentinel", "environment division was not parsed");
  invariant(bundle.ast.nodes.some(node => node.sf === "comment-before-environment.cbl" && node.sl === 5),
      "ast-data.js provenance sentinel", "expected original line 5 provenance");
}

function assertCopybookNormalization(bundle) {
  invariant(bundle.ast.nodes.some(node => node.n === "COPY-NORMALIZED"),
      "ast-data.js COPY-NORMALIZED sentinel", "program from normalized COPY input is missing");
  invariant(bundle.ast.nodes.some(node => node.n === "01 LONG-NAME"),
      "ast-data.js LONG-NAME sentinel", "copied data declaration is missing");
  invariant(bundle.ast.nodes.some(node => node.sf === "FIELDS.cpy" && node.sl === 1),
      "ast-data.js FIELDS.cpy provenance sentinel", "missing FIELDS.cpy line 1 provenance");
  invariant(bundle.ast.nodes.some(node => node.sf === "UNIT.cpy" && node.sl === 10),
      "ast-data.js UNIT.cpy provenance sentinel", "missing UNIT.cpy line 10 provenance");
  invariant(!bundle.ast.nodes.some(node => node.g === "commentEntry"),
      "ast-data.js commentEntry exclusion", "comment entry leaked into semantic AST");
}

function loadBundle(outputDirectory) {
  const bundle = {};
  for (const [key, [file, globalName]] of Object.entries(ARTIFACTS)) {
    bundle[key] = loadWindowData(path.join(outputDirectory, file), globalName);
  }
  return bundle;
}

export function assertArtifactDirectory(outputDirectory, expectedSource, fixture) {
  const bundle = loadBundle(outputDirectory);
  assertTree(bundle.tree, expectedSource);
  assertAst(bundle.ast, bundle.tree, expectedSource);
  assertCoverage(bundle.coverage, bundle.ast, bundle.tree, expectedSource);
  assertSymbols(bundle.symbols, bundle.ast, expectedSource);
  assertResolution(bundle.resolution, bundle.ast, bundle.tree, bundle.symbols, expectedSource);
  assertCrossArtifactSources(bundle);
  if (fixture === "coactupc") assertCoactupc(bundle);
  else if (fixture === "comment-entry") assertCommentEntry(bundle);
  else if (fixture === "copybook-normalization") assertCopybookNormalization(bundle);
  else fail("fixture", `unsupported fixture ${fixture}`);
}

function main(args) {
  invariant(args.length === 3, "usage",
      "assert-semantic-artifacts.mjs <coactupc-dir> <comment-entry-dir> <copybook-normalization-dir>");
  assertArtifactDirectory(args[0], "COACTUPC.cbl", "coactupc");
  assertArtifactDirectory(args[1], "comment-before-environment.cbl", "comment-entry");
  assertArtifactDirectory(args[2], "main.cbl", "copybook-normalization");
  console.log("Structured semantic artifact invariants passed");
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    main(process.argv.slice(2));
  } catch (error) {
    console.error(`Semantic artifact invariant failed: ${error.message}`);
    process.exitCode = 1;
  }
}
