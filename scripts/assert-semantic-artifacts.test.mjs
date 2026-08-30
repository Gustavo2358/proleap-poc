import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import assert from "node:assert/strict";

import {
  assertCountMap,
  assertDeclaredCount,
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
