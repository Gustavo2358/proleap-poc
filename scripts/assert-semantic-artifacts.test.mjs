import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import assert from "node:assert/strict";

import {
  assertCandidateCardinality,
  assertCountMap,
  assertDeclaredCount,
  assertOccurrenceIdentity,
  assertUnitGapSummary,
  loadWindowData,
} from "./assert-semantic-artifacts.mjs";

function temporaryFile(contents) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "semantic-artifact-runner-"));
  const file = path.join(directory, "data.js");
  fs.writeFileSync(file, contents, "utf8");
  return file;
}

test("loads fields structurally regardless of JSON property order", () => {
  const first = loadWindowData(temporaryFile('window.TEST_DATA={"a":1,"b":{"x":2}};'), "TEST_DATA");
  const reordered = loadWindowData(temporaryFile('window.TEST_DATA={"b":{"x":2},"a":1};'), "TEST_DATA");
  assert.deepEqual(first, reordered);
});

test("rejects the wrong wrapper, invalid JSON, and trailing executable content", () => {
  assert.throws(() => loadWindowData(temporaryFile('window.OTHER={"a":1};'), "TEST_DATA"),
      /expected exact prefix/);
  assert.throws(() => loadWindowData(temporaryFile('window.TEST_DATA={"a":};'), "TEST_DATA"),
      /invalid JSON payload/);
  assert.throws(() => loadWindowData(
      temporaryFile('window.TEST_DATA={"a":1};console.log("unexpected")'), "TEST_DATA"),
  /expected a single trailing semicolon|invalid JSON payload/);
});

test("rejects permissive 1-versus-10 count matches", () => {
  assert.throws(() => assertDeclaredCount("literal-target CALLs", 10, [{ id: 0 }]),
      /declared 10, inventory contains 1/);
  assert.throws(() => assertCountMap("status", { RESOLVED: 10 },
      [{ status: "RESOLVED" }], entry => entry.status), /declared 10, derived 1/);
});

test("preserves candidates when a dialect option prevents selection", () => {
  assert.doesNotThrow(() => assertCandidateCardinality("resolution-data.js.entries[0]", {
    status: "UNSUPPORTED",
    reason: "UNSUPPORTED_DIALECT_OPTION",
    candidates: [{ id: "candidate-a" }, { id: "candidate-b" }],
  }));
  assert.throws(() => assertCandidateCardinality("resolution-data.js.entries[0]", {
    status: "UNSUPPORTED",
    reason: "LITERAL_EXTERNAL_PROGRAM",
    candidates: [],
  }), /unsupported status\/reason combination/);
});

test("names occurrences by unit and local id", () => {
  const unitIds = new Set(["OUTER", "INNER"]);
  const entries = [
    { unitId: "OUTER", occurrenceId: 0 },
    { unitId: "INNER", occurrenceId: 0 },
  ];
  assert.doesNotThrow(() => assertOccurrenceIdentity("resolution-data.js", entries,
      [{ unitId: "INNER", occurrenceId: 0 }],
      [{ unitId: "OUTER", occurrenceId: 0 },
        { unitId: "OUTER", occurrenceId: -1, category: "FRONTEND_COVERAGE" },
        { unitId: null, occurrenceId: -1 }], unitIds));
  assert.throws(() => assertOccurrenceIdentity("resolution-data.js", [
    { unitId: "OUTER", occurrenceId: 0 }, { unitId: "OUTER", occurrenceId: 0 },
  ], [], [], unitIds), /duplicate occurrence 0 in unit OUTER/);
  assert.throws(() => assertOccurrenceIdentity("resolution-data.js", entries,
      [{ unitId: "OUTER", occurrenceId: 1 }], [], unitIds), /unknown occurrence 1 in unit OUTER/);
  assert.throws(() => assertOccurrenceIdentity("resolution-data.js", entries, [],
      [{ unitId: "INNER", occurrenceId: 1 }], unitIds), /unknown occurrence 1 in unit INNER/);
  assert.throws(() => assertOccurrenceIdentity("resolution-data.js", entries, [],
      [{ unitId: "OUTER", occurrenceId: -2 }], unitIds), /expected -1 or a non-negative integer/);
});

// R4 exposes input ownership already supported by ResolutionAnalysisReport.
// Summary counts exact-unit inventory plus one applicable-input indicator.
test("input summary follows ownership and ancestry without granting completeness", () => {
  const units = [{ id: "outer", parentId: null }, { id: "child", parentId: "outer" },
    { id: "peer", parentId: null }];
  const scoped = [{ unitId: "outer", category: "INPUT" }, { unitId: "outer", category: "INPUT" }];
  assert.doesNotThrow(() => assertUnitGapSummary("outer", { ...units[0], gaps: 3, complete: false }, units, scoped));
  assert.doesNotThrow(() => assertUnitGapSummary("child", { ...units[1], gaps: 1, complete: false }, units, scoped));
  assert.doesNotThrow(() => assertUnitGapSummary("peer", { ...units[2], gaps: 0, complete: true }, units, scoped));
  assert.throws(() => assertUnitGapSummary("outer", { ...units[0], gaps: 2, complete: false }, units, scoped), /declared 2, derived 3/);
  assert.throws(() => assertUnitGapSummary("child", { ...units[1], gaps: 1, complete: true }, units, scoped), /complete/);
  const global = [{ unitId: null, category: "INPUT" }];
  assert.doesNotThrow(() => assertUnitGapSummary("peer", { ...units[2], gaps: 1, complete: false }, units, global));
});
