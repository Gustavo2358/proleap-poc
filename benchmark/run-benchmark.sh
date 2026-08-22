#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if command -v mvn >/dev/null 2>&1; then
  MAVEN=mvn
elif [[ -x /home/gustavo/.sdkman/candidates/maven/current/bin/mvn ]]; then
  MAVEN=/home/gustavo/.sdkman/candidates/maven/current/bin/mvn
else
  echo "Maven not found in PATH or SDKMAN current candidate" >&2
  exit 1
fi
"$MAVEN" clean verify
"$MAVEN" -q exec:java -Dexec.args="--warmups 2 --runs 5"
