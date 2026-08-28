#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

legacy_vendor="pro""leap"
legacy_purpose="bench""mark"

mapfile -d '' paths < <(
  find . \
    ! -path './src/main/antlr4/Cobol.g4' \
    ! -path './src/main/antlr4/CobolPreprocessor.g4' \
    \( -iname "*${legacy_vendor}*" -o -iname "*${legacy_purpose}*" \) \
    -print0
)

if ((${#paths[@]})); then
  printf 'Legacy identifier found in path:\n' >&2
  printf '  %s\n' "${paths[@]}" >&2
  exit 1
fi

mapfile -t contents < <(
  rg -l -i --text "${legacy_vendor}|${legacy_purpose}" . \
    --glob '!src/main/antlr4/Cobol.g4' \
    --glob '!src/main/antlr4/CobolPreprocessor.g4'
)

if ((${#contents[@]})); then
  printf 'Legacy identifier found in content:\n' >&2
  printf '  %s\n' "${contents[@]}" >&2
  exit 1
fi

printf 'Naming verification passed.\n'
