#!/usr/bin/env bash

set -euo pipefail

harness_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
harness_project_dir="$(cd "$harness_script_dir/../.." && pwd)"
maven_bin="${MAVEN_BIN:-mvn}"

harness_gate_start() {
  if [[ "$harness_gate_name" == semantic || "$harness_gate_name" == performance || "$harness_gate_name" == full ]]; then
    python3 -c 'import sys; sys.path.insert(0,sys.argv[1]); from lean import require_local; require_local()' "$harness_script_dir"
  fi
  printf '[harness] gate=%s status=running\n' "$harness_gate_name"
}

harness_gate_exit() {
  local status=$?
  if ((status == 0)); then
    printf '[harness] gate=%s status=passed\n' "$harness_gate_name"
  else
    printf '[harness] gate=%s status=failed exitCode=%d\n' "$harness_gate_name" "$status" >&2
  fi
  exit "$status"
}

trap harness_gate_exit EXIT
