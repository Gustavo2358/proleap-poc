#!/usr/bin/env bash
set -euo pipefail

phase="${1:-manual}"
if [[ ! "$phase" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Invalid phase name: $phase" >&2
  exit 2
fi

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
maven_bin="${MAVEN_BIN:-mvn}"
run_root="$(mktemp -d "/tmp/proleap-source-normalizer-${phase}.XXXXXX")"
canonical_output="$run_root/coactupc"
fixture_output="$run_root/comment-entry"
integration_output="$run_root/copybook-normalization"

cd "$project_dir"
"$maven_bin" -q test

"$maven_bin" -q compile exec:java \
  -Dexec.args="--source corpus/cbl/COACTUPC.cbl --copybooks corpus/cpy --output $canonical_output" \
  >"$run_root/coactupc.log"

"$maven_bin" -q compile exec:java \
  -Dexec.args="--source src/test/resources/cobol/source-format/comment-before-environment.cbl --copybooks src/test/resources/cobol/provenance/cpy --output $fixture_output" \
  >"$run_root/comment-entry.log"

"$maven_bin" -q compile exec:java \
  -Dexec.args="--source src/test/resources/cobol/source-format-integration/main.cbl --copybooks src/test/resources/cobol/source-format-integration/cpy --output $integration_output" \
  >"$run_root/copybook-normalization.log"

required_files=(
  index.html ast.html symbols.html resolution.html
  tree-data.js ast-data.js symbol-data.js resolution-data.js
)

for output in "$canonical_output" "$fixture_output" "$integration_output"; do
  for file in "${required_files[@]}"; do
    if [[ ! -s "$output/$file" ]]; then
      echo "Missing or empty regression artifact: $output/$file" >&2
      exit 1
    fi
  done
  grep -Fq '"lexerErrors":0' "$output/tree-data.js"
  grep -Fq '"parserErrors":0' "$output/tree-data.js"
done

while IFS= read -r expected; do
  [[ -z "$expected" ]] && continue
  grep -Fqx "$expected" "$run_root/coactupc.log"
done <"src/test/resources/cobol/source-format/coactupc-regression-baseline.txt"

grep -Fq '"n":"COMMENTBUG"' "$fixture_output/ast-data.js"
for division in IDENTIFICATION ENVIRONMENT DATA PROCEDURE; do
  grep -Fq "\"divisionKind\":\"$division\"" "$fixture_output/ast-data.js"
done
grep -Fq '"n":"environmentDivision"' "$fixture_output/tree-data.js"
grep -Fq 'ENVIRONMENT DIVISION.' "$fixture_output/preprocessed.cbl"
if grep -Fq '*>CE ENVIRONMENT DIVISION.' "$fixture_output/preprocessed.cbl"; then
  echo "ENVIRONMENT DIVISION leaked into a comment entry" >&2
  exit 1
fi
grep -Fq '"sf":"comment-before-environment.cbl","sl":5' "$fixture_output/ast-data.js"

grep -Fq '"n":"COPY-NORMALIZED"' "$integration_output/ast-data.js"
grep -Fq '"n":"01 LONG-NAME"' "$integration_output/ast-data.js"
grep -Fq '"sf":"FIELDS.cpy","sl":1' "$integration_output/ast-data.js"
grep -Fq '"sf":"UNIT.cpy","sl":10' "$integration_output/ast-data.js"
grep -Fq 'AUTHOR. *>CE ENTRY INSIDE COPY. WITH PERIODS.' \
  "$integration_output/preprocessed.cbl"
if grep -Fq '"g":"commentEntry"' "$integration_output/ast-data.js"; then
  echo "Comment entry leaked into the semantic AST" >&2
  exit 1
fi

echo "Source-normalizer regression passed: $run_root"
sed -n '1,8p' "$run_root/coactupc.log"
sed -n '1,8p' "$run_root/comment-entry.log"
sed -n '1,8p' "$run_root/copybook-normalization.log"
