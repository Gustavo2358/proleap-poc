import fs from "node:fs";
import path from "node:path";

const output = process.argv[2];
if (!output) throw new Error("usage: node scripts/assert-cics-artifacts.mjs <application-output>");

function load(name) {
    const source = fs.readFileSync(path.join(output, name), "utf8");
    const start = source.indexOf("=");
    const end = source.lastIndexOf(";");
    if (start < 0 || end <= start) throw new Error(`${name}: unsupported data wrapper`);
    return JSON.parse(source.slice(start + 1, end));
}

function fail(message) {
    throw new Error(`CICS-EXPRESSION-001: ${message}`);
}

function counts(values) {
    return values.reduce((result, value) => {
        result[value] = (result[value] ?? 0) + 1;
        return result;
    }, {});
}

const expected = {"DFHRESP(NORMAL)": 7, "DFHRESP(NOTFND)": 3};
const tree = load("tree-data.js");
const ast = load("ast-data.js");
const resolution = load("resolution-data.js");

const specialized = tree.nodes.filter(node => node.n === "cicsDfhRespLiteral");
// Token intervals are the reliable equivalence key; source text is asserted through the AST below.
if (specialized.length !== 10) fail(`expected 10 specialized parse contexts, found ${specialized.length}`);
if (specialized.some(cics => tree.nodes.some(node =>
        node.n === "tableCall" && node.a === cics.a && node.b === cics.b))) {
    fail("a tableCall covers the same token interval as a specialized CICS literal");
}

const literalCounts = counts(ast.nodes
    .filter(node => node.t === "LiteralExpression" && node.n.startsWith("DFHRESP("))
    .map(node => node.n));
if (Object.keys(expected).some(key => literalCounts[key] !== expected[key])
        || Object.keys(literalCounts).some(key => expected[key] !== literalCounts[key])) {
    fail(`unexpected AST literal distribution: ${JSON.stringify(literalCounts)}`);
}
if (ast.nodes.some(node => node.t === "DataReference" &&
        ["DFHRESP", "NORMAL", "NOTFND"].includes(node.a?.baseName))) {
    fail("a CICS built-in or argument leaked into an AST DataReference");
}

const forbidden = new Set(["DFHRESP", "DFHVALUE", "NORMAL", "NOTFND",
    "DFHRESP(NORMAL)", "DFHRESP(NOTFND)"]);
if (resolution.entries.some(entry => forbidden.has(entry.writtenText))) {
    fail("a CICS built-in or argument leaked into nominal resolution entries");
}
if (resolution.gaps.some(gap => forbidden.has(gap.writtenText)
        || forbidden.has(resolution.entries.find(entry => entry.occurrenceId === gap.occurrenceId)?.writtenText))) {
    fail("a CICS built-in or argument leaked into resolution gaps");
}

if (!tree.nodes.some(node => node.n === "tableCall")) fail("no ordinary tableCall survived in the parse tree");
if (!ast.nodes.some(node => node.t === "DataReference" && node.g === "tableCall")) {
    fail("no ordinary tableCall survived as a structured AST data reference");
}

console.log("CICS application artifacts satisfy CICS-EXPRESSION-001");
