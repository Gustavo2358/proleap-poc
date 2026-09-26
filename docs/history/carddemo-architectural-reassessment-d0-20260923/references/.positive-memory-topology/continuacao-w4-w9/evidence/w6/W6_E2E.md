# W6 qualified E2E

Freeze `w4-b.2`: both read-only verifiers PASS before and after. 39 historical + 3 derived sources, 45 qualified sites. All qualified manifests, metadata, locks, original/derived sources and historical product fixtures unchanged.

Final result: **31 PASS + 11 COVERAGE_PASS_COMPLETE; 0 FAIL; 0 PIPELINE_FAILED**. Nine primary regression gates: **9 PASS / 0 FAIL / 0 PIPELINE_FAILED**. W5 was 20 PASS / 10 FAIL / 1 PIPELINE_FAILED / 11 coverage PASS. The ten failing programs included two derived dependency witnesses; all now pass. 09 OUTPUT_INVALID was the pipeline failure and is fixed as a byproduct.

| Qualified case | Mode | W6 result | Candidates by oracle site |
|---|---|---|---|
| 01_straight_strong_update | DEPENDENCY | PASS | C1=PROGB001 |
| 02_if_join | DEPENDENCY | PASS | C1=PROGA001,PROGB001 |
| 03_evaluate_join | DEPENDENCY | PASS | C1=PROGA001,PROGB001,PROGC001 |
| 04_basic_move | DEPENDENCY | PASS | C1=PROGB001 |
| 05_self_thru_move | DEPENDENCY | PASS | C1=PROGB001 |
| 06_basic_call_inside | DEPENDENCY | PASS | C1=PROGA001 |
| 07_self_thru_call_inside | DEPENDENCY | PASS | C1=PROGA001 |
| 08_basic_two_activations | DEPENDENCY | PASS | C1=PROGA001,PROGB001 |
| 09_self_thru_two_activations | DEPENDENCY | PASS | C1=PROGA001,PROGB001 |
| 10_three_paragraph_range | DEPENDENCY | PASS | C1=PROGC001 |
| 11_thru_exit_endpoint | DEPENDENCY | PASS | C1=PROGB001 |
| 12_thru_dummy_move_endpoint | DEPENDENCY | PASS | C1=PROGB001 |
| 13_out_of_line_until_before | DEPENDENCY | PASS | C1=PROGA001,PROGB001 |
| 14_out_of_line_until_after | DEPENDENCY | PASS | C1=PROGB001 |
| 15_out_of_line_times_literal | DEPENDENCY | PASS | C1=PROGB001 |
| 16_out_of_line_times_item | DEPENDENCY | PASS | C1=PROGA001,PROGB001 |
| 17_out_of_line_varying_before | DEPENDENCY | PASS | C1=PROGA001,PROGB001 |
| 18_out_of_line_varying_after | DEPENDENCY | PASS | C1=PROGB001 |
| 19_inline_once | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 20_inline_times | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 21_inline_until_before | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 22_inline_until_after | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 23_inline_varying | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 24_nested_basic | DEPENDENCY | PASS | C1=PROGB001 |
| 25_nested_thru | DEPENDENCY | PASS | C1=PROGB001 |
| 26_if_with_performs | DEPENDENCY | PASS | C1=PROGA001,PROGB001 |
| 27_evaluate_with_performs | DEPENDENCY | PASS | C1=PROGA001,PROGB001 |
| 28_goto_inside_range | DEPENDENCY | PASS | C1=PROGB001,PROGC001 |
| 29_exit_paragraph | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 30_exit_perform | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 31_range_unreachable_open_peer | DEPENDENCY | PASS | C1=PROGB001 |
| 32_basic_unreachable_open_peer | DEPENDENCY | PASS | C1=PROGB001 |
| 33_mixed_peer_contamination | DEPENDENCY | PASS | C1=PROGA001; C2=PROGB001 |
| 34_return_contexts | DEPENDENCY | PASS | C1=PROGA001; C2=PROGB001 |
| 35_inline_unrelated_effect | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 36_thru_continue_endpoint | DEPENDENCY | PASS | C1=PROGB001 |
| 37_section_perform | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 38_varying_after_level | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |
| 39_goto_dead_call_control | DEPENDENCY | PASS | DEAD=∅ (unreachable); C1=PROGA001 |
| 24_nested_entry_call | DEPENDENCY | PASS | C1=PROGB001 |
| 29_exit_paragraph_entry_call | DEPENDENCY | PASS | C1=PROGB001 |
| 38_multilevel_body_call | COVERAGE_INTEGRITY | COVERAGE_PASS_COMPLETE | coverage only |

All 23 previously green oracle sites were rechecked individually (`boundary-checks.json`), including 33-C1, both 34 sites and 39-DEAD. All 11 coverage-only oracles still pass. No source failure was relabeled to obtain this result.

For each of the nine regression witnesses, SOURCE/SP/LOWER/AIR/CFG/DATAFLOW/DEPENDENCY = PASS, first loss NONE for the qualified expectation. The 17-row `W6_WITNESS_MATRIX.csv` includes these sites, 09, 33-C1, 34-C1/C2, 39-DEAD/C1 and the two affected derived witnesses. PASS is scoped to the declared positive facts and oracle, not complete COBOL semantics; gap/coverage and source/interpretation remainders remain in the artifacts.

Each `run-final/<case>/` holds `source.cbl`, `sp/cobol-semantic-product.json`, `program.air.json`, `cfg.json`, `dependencies.json`, `result.json` and stage stdout/stderr. Result records retain exact commands, source/output hashes and stage times. AIR acceptance uses the shared validator; existing CFG and value analysis consume the resulting core control unchanged.

The local runner adapter substitutes only the execution baseline pins/classes, because the historical runner is intentionally pinned to P0. It imports the frozen qualified runner without modifying it. Lock/oracle loading, source checks, exact clean HEAD/classpath preflight/postflight and all expected candidates remain authoritative. `preflight == postflight` is asserted.

Determinism: cases06/08/25/33/34/39 repeated under identical final pins; all 24 SP/AIR/CFG/dependency SHA-256 outputs match exactly (`determinism.json`). Permanent AIR physical-inventory permutation tests and full local capacity determinism also pass.

No W6 PERFORM first loss remains among the qualified dependency gates. General IF/EVALUATE/GO TO/ordinary continuation issues from W5 discovery remain W7 work; MOVE per-transfer producer availability remains W8. These are not claimed resolved merely because this corpus is green. W7 was not started.
