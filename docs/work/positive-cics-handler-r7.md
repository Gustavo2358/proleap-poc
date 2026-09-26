# R7 — typed CICS handler operation contract

- id: POSITIVE_CICS_HANDLER_R7_CONTRACT
- status: IN_PROGRESS (review required; no merge authorized)
- scope: frontend/SP 2.41.0 only; [contract](../domain/cics-handler-operations.md)
- first loss: HANDLE ABEND previously OBSERVED/OPAQUE_CICS with no typed action or procedure binding
- authority: explicit user authorization to version the SP after R7 contract-expansion stop
- must_not_change: AIR, downstream state engine, value providers, R8/R9, ALTER, frozen historical semantic expectations
- qualification: CicsHandlerContractTest, frontend FAST/full local, real synthetic and CardDemo73 producer runs, determinism, pinned consumer admission probe

## Original R7 qualification (historical)

SP 2.41.0 publishes 18 HANDLE ABEND operations in the frozen 73-input CardDemo
inventory: nine ACTIVATE/LABEL with canonical binding and nine CANCEL. Fourteen
real frontend fixtures cover 15 units and 27 operations, including PROGRAM,
RESET, ambiguity, COPY and distinct owners. Both corpus runs preserve all old
non-handler statements and independent facts; normalized ControlTopology is
equal in all 73 inputs. No state propagation or new transitions are implemented.

FAST: 484 tests pass. Full local: 1059 tests, zero failures/errors, one existing
skip; normalizer/provenance E2E and naming pass. Eight operation-contract mutants
are killed, then 16 focused/compositionality tests pass after restoration.
Current-version test assertions advance to 2.41.0; frozen semantic expectations
are unchanged. 219 corpus transports and 56 fixture transports are deterministic.

The pinned R6 Lower rejects authentic 2.41 products at SP admission (INPUT_ERROR,
FactDependencies requires 2.40), before constructing AIR. This contract review
does not claim a newly qualified downstream pipeline or an AIR contract limitation.
The next consumer migration and handler-state implementation remain pending;
AIR, PROGRAM/FILE providers, R8/R9 and ALTER are unchanged.

## R7-R1 current review scope

- id: CICS_HANDLER_CONTRACT_CLOSURE
- status: IN_PROGRESS (draft contract; human review pending)
- findings: R7-C01 operand provenance; R7-C02 PR body reviewability
- scope: correct Optional targetOrigin and literal/DATA parity in SP 2.41.0;
  retain canonical source/include ownership and all existing control semantics
- gates: final-head frontend FAST/full and focal historical suites; eight new
  provenance mutants separately from eight R7 mutants; CardDemo73 SP replay twice;
  frozen lower 4044877321ab9fef3deee3314e92249872f96fb9 admission probe; exact-head CI
- evidence: aggregate `.positive-memory-topology/positive-cics-handler-r7/contract-r1/`

Handler state, replacement and dispatch remain NOT IMPLEMENTED. AIR is unchanged.
Lower 2.41 is NOT IMPLEMENTED: rejection is the expected version boundary. The
frozen decoder actually emits INPUT_ERROR for FactDependencies requiring 2.40
before its UNSUPPORTED_CONTRACT branch. This diagnostic ordering is recorded as
a limit; no lower source change is authorized in R7-R1. R8/R9 remain NOT STARTED.
PR #58 remains DRAFT / OPEN / NO MERGE.
