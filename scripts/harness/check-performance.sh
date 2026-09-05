#!/usr/bin/env bash

harness_gate_name="performance"
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"
harness_gate_start

cd "$harness_project_dir"
"$maven_bin" -q \
  -Dtest=ResolutionAnalysisReportTest#scalesByIndexedCandidatesAndProducesDeterministicResults,\
SemanticProductMoveCallContractTest#materializedPortIndexesScaleLinearly \
  test
