#!/usr/bin/env bash

harness_gate_name="semantic"
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"
harness_gate_start

cd "$harness_project_dir"
"$maven_bin" -q test
