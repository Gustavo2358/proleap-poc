# POSITIVE_MEMORY_TOPOLOGY_W5_READY_FOR_REVIEW

Qualification lock: **w4-b.2 VERIFIED**. Production changed: YES. Repositories changed: proleap-poc, cobol-lower. No merge; W6 not started.

## Exact final HEADs

- proleap-poc: `aa059545e6eeb2fdee3751d0b0f1bf33b24b3937`
- cobol-lower: `00354bbc48ffc9990494bff983b658edddaac714`
- air-java: `980d4989a18f876996390cc61af41419a595eb7b`
- analysis-ir: `b628e4c1a62de61a157cac85ae71ba4cd111052e`
- analysis-cfg: `7a4a2104d3c284e8efc1700413839f33551b474f`

Both changed repositories remain on `feat/positive-memory-topology`, based on the existing `feat/source-dependencies-w3` stack. All five worktrees are clean. The lower SRC-SP lock pins the producer commit, tree, paths, immutable URLs and SHA-256 values; AIR/IR pins and all untouched repository HEADs are unchanged.

## Frozen authority

- `qualification-lock.json`: `13ac4bc8bda87e7327562e730ceece46ab2db5af8bfbc7070c797d5dfa1e9fab`
- `manifest.json`: `1175ac8c99451ec7328f784ee9dd8a1754a862de879cc9216c8873241bc65930`
- `oracle-metadata.json`: `3c751b134dbb8ac3b00787c6f2fe808b3f553ecf3faf66da7f0b7d1c28f8b04f`

Read-only checks before implementation and after final E2E: `QUALIFICATION_LOCK_OK w4-b.2 39 3` and `W4_B_R1_VERIFY_OK w4-b.2 39 originals, 3 derived, 45 sites`. See `freeze-before.log`, `freeze-after.log`, `freeze-final.log`. Qualified corpus modified: NO. Oracles modified: NO. Historical fixture files modified: NO (only six new lower fixture files added).

## Validation

- Frontend full unit/SP suite: 999 tests reported, 0 failures, 0 errors, 1 existing skip; `frontend-unit-summary.json`, `frontend-unit-verified.log`.
- Frontend FAST: 401 tests, no failures/errors; `frontend-fast-with-w5.log`. W5 test class is now a permanent FAST/CI selection. Final strengthened M5 assertions also pass in `metamorphic-final.log`.
- Lower FAST at the W5 source pin: PASS (155.251 seconds); structural wire suite, historical decoder suites, malformed facts, round-trip, physical order, identity checks, architecture and 21 harness tests; `lower-fast-pinned.log`. Final source-link metadata validated by `lower-docs-pinned.log`.
- Qualified final corpus: 42 actual pipelines, 20 PASS / 10 FAIL / 1 PIPELINE_FAILED / 11 COVERAGE_PASS_COMPLETE. Primary nine regressions: 1 PASS / 8 FAIL / 0 PIPELINE_FAILED. All nine required SP structural fact checks pass; no old-green-site regression.
- Determinism: 20/20 artifact hashes match for repeated 06/08/24/32/39; `determinism.json`. JDK 21.0.12+1.1-tem.

## CI and PRs

- proleap-poc pull_request FAST: PASS at `aa059545e6eeb2fdee3751d0b0f1bf33b24b3937` — [run 35858673074](https://github.com/Gustavo2358/proleap-poc/actions/runs/35858673074).
- proleap-poc push FAST: PASS at `aa059545e6eeb2fdee3751d0b0f1bf33b24b3937` — [run 35858669075](https://github.com/Gustavo2358/proleap-poc/actions/runs/35858669075).
- [PR #58](https://github.com/Gustavo2358/proleap-poc/pull/58): DRAFT, OPEN, NOT MERGED; base unchanged.
- cobol-lower pull_request FAST: PASS at `00354bbc48ffc9990494bff983b658edddaac714` — [run 35859067504](https://github.com/Gustavo2358/cobol-lower/actions/runs/35859067504).
- cobol-lower push FAST: PASS at `00354bbc48ffc9990494bff983b658edddaac714` — [run 35859060574](https://github.com/Gustavo2358/cobol-lower/actions/runs/35859060574).
- [PR #34](https://github.com/Gustavo2358/cobol-lower/pull/34): DRAFT, OPEN, NOT MERGED; base unchanged.

## Reproduction / commands

```sh
python3 -B positive_memory_perform_e2e/qualified-w4/run_qualified.py --verify-only
python3 -B .positive-memory-topology/continuacao-w4-w9/evidence/w4-b-r1/verify_review.py
# In proleap-poc, JAVA_HOME/PATH set to JDK 21:
mvn -q test
mvn -q -Dtest=PartialStructuralFactsTest test
python3 -B scripts/harness/lean.py fast
# In cobol-lower, JAVA_HOME/PATH set to JDK 21:
python3 -B scripts/harness/lean.py fast
# At workspace root:
python3 -B .positive-memory-topology/continuacao-w4-w9/evidence/w5/prepare_runtime.py
python3 -B .positive-memory-topology/continuacao-w4-w9/evidence/w5/run_w5_qualified.py --runtime .positive-memory-topology/continuacao-w4-w9/evidence/w5/runtime-w5.json --out <new-output> --java /home/gustavo/.sdkman/candidates/java/21.0.12+1.1-tem/bin/java
python3 -B .positive-memory-topology/continuacao-w4-w9/evidence/w5/summarize_w5.py
```

The saved runtime snapshot already exists; prepare_runtime refuses to overwrite it. The adapter changes only runtime baseline pins, leaving strict clean-HEAD/classpath pre/postflight and all frozen oracles active. `run-final/results.json` records actual stage commands, timings and artifact hashes. Tool execution required escalation because the default local bubblewrap helper failed before execution; no approval rejection or unavailable verification was hidden.

## Evidence index

- [W5_DISCOVERY.md](W5_DISCOVERY.md)
- [W5_CONTRACT.md](W5_CONTRACT.md)
- [W5_IMPLEMENTATION.md](W5_IMPLEMENTATION.md)
- [W5_WITNESS_MATRIX.csv](W5_WITNESS_MATRIX.csv)
- [W5_NON_PERFORM_AUDIT.md](W5_NON_PERFORM_AUDIT.md)
- [W5_METAMORPHIC.md](W5_METAMORPHIC.md)
- [W5_E2E.md](W5_E2E.md)
- `run-final/`: every source copy and source→SP→lower→AIR→CFG→dependencies boundary, or explicit failure.
- `sp-witness-facts.json`, `boundary-checks.json`: positive facts and nine-witness assertions.
- `air-boundary-observations.json`: source-correlated AIR operations prove that remaining activations lack their known target edges in the lower; 33-C1 keeps its Jump.
- `heads.json`, `baseline-w5.json`, `runtime-w5.json`, `runtime-final/`: exact pins and immutable executable classes; external jar/AIR/CFG paths retain their prior frozen hashes.
- `artifact-sha256.json`: hash inventory of retained evidence (excludes itself).

## Open work / stop

W6: consume structural-only PERFORM entry/resume facts; compose activation-specific and nested nonrecursive returns; connect the published EXIT./CONTINUE completion frontiers through existing partial body facts. Keep unavailable concrete inner resume distinct from conditional caller completion.

W7/W8: producer MOVE mixed-transfer availability needs a separate transfer contract; ordinary cross-paragraph versus activation continuation needs distinct context; EVALUATE unsupported arm grouping and downstream IF/EVALUATE/GO TO composition remain separate investigations. See the non-PERFORM audit for what is confirmed versus not established.

Preexisting 09 OUTPUT_INVALID remains. No broad fallback, AllControl, AllMemory, havoc compensation, source scanning or ProgramPoint reconstruction was introduced. Dependency PASS is **not** required for W5 closure; producer/SP preservation **is**. W5 is ready for human/Astra review. Stop here.
