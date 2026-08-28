#!/usr/bin/env bash

harness_gate_name="full"
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"
harness_gate_start

export MAVEN_BIN="$maven_bin"
"$harness_script_dir/check-fast.sh"
"$harness_script_dir/check-semantic.sh"
"$harness_project_dir/scripts/source-normalizer-regression.sh" full
"$harness_project_dir/scripts/verify-naming.sh"
