#!/usr/bin/env bash
set -euo pipefail

phase="${1:-manual}"
if [[ ! "$phase" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Invalid phase name: $phase" >&2
  exit 2
fi

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
maven_bin="${MAVEN_BIN:-mvn}"
run_root="$(mktemp -d "/tmp/cobol-source-normalizer-${phase}.XXXXXX")"
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
  tree-data.js ast-data.js coverage-data.js symbol-data.js resolution-data.js
)

for output in "$canonical_output" "$fixture_output" "$integration_output"; do
  for file in "${required_files[@]}"; do
    if [[ ! -s "$output/$file" ]]; then
      echo "Missing or empty regression artifact: $output/$file" >&2
      exit 1
    fi
  done
done

node --test scripts/assert-semantic-artifacts.test.mjs
node scripts/assert-semantic-artifacts.mjs \
  "$canonical_output" "$fixture_output" "$integration_output"

grep -Fqx 'ENVIRONMENT DIVISION.' "$fixture_output/preprocessed.cbl"
if grep -Fq '*>CE ENVIRONMENT DIVISION.' "$fixture_output/preprocessed.cbl"; then
  echo "ENVIRONMENT DIVISION leaked into a comment entry" >&2
  exit 1
fi
grep -Fqx 'AUTHOR. *>CE ENTRY INSIDE COPY. WITH PERIODS.' \
  "$integration_output/preprocessed.cbl"

echo "Source-normalizer regression passed: $run_root"
sed -n '1,8p' "$run_root/coactupc.log"
sed -n '1,8p' "$run_root/comment-entry.log"
sed -n '1,8p' "$run_root/copybook-normalization.log"
