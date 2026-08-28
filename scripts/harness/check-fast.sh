#!/usr/bin/env bash

harness_gate_name="fast"
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"
harness_gate_start

"$harness_script_dir/check-docs.sh"
