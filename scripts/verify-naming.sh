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

# Python is already required by the harness. A missing optional search binary
# must never become success through a Bash process substitution.
python3 - "$legacy_vendor" "$legacy_purpose" <<'PY'
from pathlib import Path
import re
import subprocess
import sys

vendor, purpose = sys.argv[1:]
forbidden = re.compile(re.escape(vendor) + '|' + re.escape(purpose), re.IGNORECASE)
repository = re.compile(r'(?<!\w)' + re.escape(vendor) + r'-poc(?!\w)')
excluded = {'src/main/antlr4/Cobol.g4', 'src/main/antlr4/CobolPreprocessor.g4', 'THIRD_PARTY_NOTICES.md'}
paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'])
contents = []
for name in sorted(set(paths.decode().split('\0')) - {''}):
    if name in excluded or name.startswith(('specs/', 'docs/history/')):
        continue
    path = Path(name)
    if path.is_symlink() or not path.is_file():
        continue  # Match the original search: no symlink following or deleted files.
    content = path.read_text(errors='replace')
    if name.startswith('docs/') and name.endswith('.md'):
        # Exact repository identity in documentary evidence only.
        content = repository.sub('', content)
    if forbidden.search(content):
        contents.append(name)
if contents:
    print('Legacy identifier found in content:', *contents, sep='\n  ', file=sys.stderr)
    raise SystemExit(1)
PY

printf 'Naming verification passed.\n'
