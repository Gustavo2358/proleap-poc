#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

legacy_vendor="pro""leap"
legacy_purpose="bench""mark"

paths=()
while IFS= read -r -d '' path; do
  case "$path" in
    src/main/antlr4/Cobol.g4|src/main/antlr4/CobolPreprocessor.g4|THIRD_PARTY_NOTICES.md|specs/*|docs/history/*)
      continue
      ;;
  esac
  path_lower="${path,,}"
  if [[ "$path_lower" == *"$legacy_vendor"* || "$path_lower" == *"$legacy_purpose"* ]]; then
    paths+=("$path")
  fi
done < <(git ls-files --cached --others --exclude-standard -z)

if ((${#paths[@]})); then
  printf 'Legacy identifier found in path:\n' >&2
  printf '  %s\n' "${paths[@]}" >&2
  exit 1
fi

mapfile -t contents < <(
  rg -l -i --text "${legacy_vendor}|${legacy_purpose}" . \
    --glob '!src/main/antlr4/Cobol.g4' \
    --glob '!src/main/antlr4/CobolPreprocessor.g4' \
    --glob '!THIRD_PARTY_NOTICES.md' \
    --glob '!specs/**' \
    --glob '!docs/history/**'
)

if ((${#contents[@]})); then
  printf 'Legacy identifier found in content:\n' >&2
  printf '  %s\n' "${contents[@]}" >&2
  exit 1
fi

printf 'Naming verification passed.\n'
