#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

function fail(message) {
  throw new Error(`semantic-context-mining: ${message}`);
}

function loadWrappedJson(file, expectedGlobal) {
  const text = fs.readFileSync(file, "utf8");
  const prefix = `window.${expectedGlobal}=`;
  if (!text.startsWith(prefix) || !text.trimEnd().endsWith(";"))
    fail(`${file} does not use the expected ${expectedGlobal} wrapper`);
  try {
    return JSON.parse(text.slice(prefix.length, text.lastIndexOf(";")));
  } catch (error) {
    fail(`${file} contains invalid JSON: ${error.message}`);
  }
}

function canonicalBase(entry) {
  if (entry.candidates?.length) return entry.candidates[0].canonicalName;
  return entry.writtenText.trim().split(/[\s(:]/, 1)[0].toUpperCase();
}

function approximateStatement(lines, lineNumber) {
  const keywords = /\b(IF|EVALUATE|SET|SEARCH|PERFORM|MOVE|COMPUTE|ADD|SUBTRACT|MULTIPLY|DIVIDE|CALL|READ|WRITE|REWRITE|DELETE|START|STRING|UNSTRING|INSPECT|ACCEPT|DISPLAY|GO\s+TO)\b/g;
  const start = Math.max(0, lineNumber - 13);
  for (let index = Math.min(lines.length - 1, lineNumber - 1); index >= start; index--) {
    const upper = lines[index].toUpperCase();
    const matches = [...upper.matchAll(keywords)];
    if (matches.length) return matches.at(-1)[1].replace(/\s+/, "_");
    if (/\.\s*$/.test(upper) && index < lineNumber - 1) break;
  }
  return "UNKNOWN";
}

function sourceExcerpt(lines, lineNumber) {
  const start = Math.max(0, lineNumber - 2);
  const end = Math.min(lines.length, lineNumber + 1);
  return lines.slice(start, end).map((line, offset) => `${start + offset + 1}:${line.trim()}`).join(" | ");
}

function increment(map, key) {
  map.set(key, (map.get(key) ?? 0) + 1);
}

function sortedCounts(map) {
  return [...map].sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]));
}

function analyzeSet(resolutionFile, symbolFile, astFile) {
  const resolution = loadWrappedJson(resolutionFile, "RESOLUTION_DATA");
  const symbols = loadWrappedJson(symbolFile, "SYMBOL_TABLE_DATA");
  const ast = loadWrappedJson(astFile, "AST_DATA");
  if (!Array.isArray(resolution.entries) || !Array.isArray(resolution.sourceLines))
    fail(`${resolutionFile} has an unexpected schema`);
  if (!Array.isArray(symbols.symbols)) fail(`${symbolFile} has an unexpected schema`);
  if (!Array.isArray(ast.nodes)) fail(`${astFile} has an unexpected schema`);

  const symbolsByName = new Map();
  for (const symbol of symbols.symbols) {
    const values = symbolsByName.get(symbol.c) ?? [];
    values.push(symbol);
    symbolsByName.set(symbol.c, values);
  }

  const statuses = new Map();
  const unresolvedClusters = new Map();
  const invalidClusters = new Map();
  const invalidRows = [];
  const falseSuccessRows = [];
  const conditionNameReferences = new Map();
  for (const entry of resolution.entries) {
    increment(statuses, `${entry.status}/${entry.reason}`);
    const statement = approximateStatement(resolution.sourceLines, entry.span.startLine);
    const sameName = symbolsByName.get(canonicalBase(entry)) ?? [];
    const sameNameKinds = [...new Set(sameName.map(symbol => symbol.k))].sort();
    if (entry.grammarRule === "conditionNameReference")
      increment(conditionNameReferences, `${entry.status}/${entry.reason}/${entry.candidates.map(candidate => candidate.kind).join("+") || "NONE"}`);
    if (entry.status === "UNRESOLVED") {
      increment(unresolvedClusters, [entry.reason, entry.grammarRule, entry.kind,
        entry.admissibleKinds.join("+"), entry.role, statement, sameNameKinds.join("+") || "NONE"].join("\t"));
    }
    if (entry.reason === "INVALID_NAMESPACE_FOR_CONTEXT") {
      increment(invalidClusters, [entry.grammarRule, entry.kind, entry.admissibleKinds.join("+"),
        entry.role, statement, sameNameKinds.join("+") || "NONE"].join("\t"));
      invalidRows.push({
        name: entry.writtenText,
        grammarRule: entry.grammarRule,
        kind: entry.kind,
        admissibleKinds: entry.admissibleKinds,
        role: entry.role,
        statement,
        sameNameKinds,
        line: entry.span.startLine,
        excerpt: sourceExcerpt(resolution.sourceLines, entry.span.startLine)
      });
    }

    if (entry.status === "RESOLVED" && sameNameKinds.length > 1) {
      const selectedKinds = [...new Set(entry.candidates.map(candidate => candidate.kind))].sort();
      const compatibleDeclaredKinds = sameNameKinds.filter(kind => {
        if (kind === "DATA_ITEM" || kind === "RENAMES") return entry.admissibleKinds.includes("DATA");
        if (kind === "CONDITION_NAME") return entry.admissibleKinds.includes("CONDITION");
        if (kind === "INDEX_NAME") return entry.admissibleKinds.includes("INDEX");
        return false;
      });
      if (compatibleDeclaredKinds.length < sameNameKinds.filter(kind =>
        ["DATA_ITEM", "RENAMES", "CONDITION_NAME", "INDEX_NAME"].includes(kind)).length) {
        falseSuccessRows.push({
          name: entry.writtenText,
          grammarRule: entry.grammarRule,
          kind: entry.kind,
          admissibleKinds: entry.admissibleKinds,
          selectedKinds,
          sameNameKinds,
          role: entry.role,
          statement,
          line: entry.span.startLine,
          excerpt: sourceExcerpt(resolution.sourceLines, entry.span.startLine)
        });
      }
    }
  }

  const astContextClusters = new Map();
  const astContextRows = [];
  for (const node of ast.nodes) {
    const relevant = node.t === "PreservedExpression"
      || node.g === "relationCombinedComparison"
      || node.a?.operator === "MIXED_LOGICAL";
    if (!relevant) continue;
    const key = `${node.t}\t${node.g}\t${node.a?.operator ?? ""}`;
    increment(astContextClusters, key);
    astContextRows.push({
      type: node.t,
      grammarRule: node.g,
      operator: node.a?.operator ?? null,
      writtenText: node.a?.writtenText ?? node.n,
      line: node.l,
      originalFile: node.sf,
      originalLine: node.sl
    });
  }

  return {
    source: resolution.meta?.source ?? path.basename(resolutionFile),
    entries: resolution.entries.length,
    statuses: sortedCounts(statuses),
    conditionNameReferences: sortedCounts(conditionNameReferences),
    unresolvedClusters: sortedCounts(unresolvedClusters),
    invalidClusters: sortedCounts(invalidClusters),
    invalidRows,
    falseSuccessRows,
    astContextClusters: sortedCounts(astContextClusters),
    astContextRows
  };
}

function parseArguments(args) {
  if (!args.length || args.length % 3 !== 0)
    fail("pass one or more resolution-data.js/symbol-data.js/ast-data.js sets");
  const sets = [];
  for (let index = 0; index < args.length; index += 3)
    sets.push([args[index], args[index + 1], args[index + 2]]);
  return sets;
}

const reports = parseArguments(process.argv.slice(2)).map(([resolution, symbols, ast]) =>
  analyzeSet(resolution, symbols, ast));
process.stdout.write(JSON.stringify({schemaVersion: 1, reports}, null, 2) + "\n");
