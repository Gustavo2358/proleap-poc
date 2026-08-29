#!/usr/bin/env bash

set -euo pipefail

harness_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
harness_project_dir="$(cd "$harness_script_dir/../.." && pwd)"
maven_bin="${MAVEN_BIN:-mvn}"

harness_gate_start() {
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
