# R7-R2 — typed CICS ABEND event contract

- id: TYPED_CICS_ABEND_EVENT_CONTRACT
- status: IN_PROGRESS (human review pending; no merge)
- scope: frontend/SP 2.42.0 only; [contract](../domain/cics-abend-events.md)
- authority: explicit R7-R2 authorization after the completion contract-expansion stop
- first loss: explicit ABEND/CANCEL previously shared OBSERVED/OPAQUE_CICS
- must_not_change: CICS_HANDLER meaning/provenance, lower, AIR, CFG, state,
  replacement, dispatch, PROGRAM/FILE producers, ALTER, R8/R9
- gates: RED oracles; ten new mutations each RED/restore/GREEN; full FAST and full
  local including historical suites; CardDemo73 producer twice and fixtures twice;
  lower boundary probe; exact-head CI and draft PR #58 publication
- evidence: aggregate `.positive-memory-topology/positive-cics-handler-r7/event-contract-r2/`

R7 and R7-R1 remain historical contract qualifications. The 14 handler assertions
and 17 provenance assertions remain intact. The historical handler test composes
the former 2.41 contribution set explicitly; all other historical assertions and
composition roots stay unchanged. The new family joins the existing compositional
multiplicity oracle. The State envelope and state-derived port remain unchanged.
Only products with new event facts or registration-completion proofs require
2.42; unchanged products retain their feature-based version. The initially
proposed metadata marker for zero-event products was removed after the frozen
State/port architecture tests exposed its incompatibility.

Handler state, replacement and dispatch remain NOT_STARTED. R7 completion is
not resumed. All downstream repositories and PR #34 remain untouched. PR #58
remains DRAFT / OPEN / NO MERGE.
