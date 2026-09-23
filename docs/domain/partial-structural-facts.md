# W5 — partial structural facts (SP 2.36)

Current compositional control and SP2.37 ordinary continuations: [W7 contract](control-composition.md). Historical profile qualification below does not gate independent branch entries or predicate coverage.

Invariant: **Qualificação integral autoriza especialização integral; ela não autoriza a existência dos fatos positivos.**

## Facts and availability

| Fact | Positive representation | Absence / limits |
| --- | --- | --- |
| Nominal target | `start` / `end`: local procedure identity and reference/paragraph provenance | absent means unavailable, never any label |
| Executable target entry | `targetEntry`: local published root StatementId, independent of whole range | absent means unavailable; if a complete range exists it agrees with its first entry |
| Activation identity | PERFORM statement header identity | each callsite has its own identity, even with the same target |
| Callsite resume | `normalContinuation.availability=KNOWN` and statement/provenance | UNAVAILABLE supplies no destination; resume is conditional on completion, never a bypass edge |
| Ordered range | nonempty `procedures`, endpoints agree; each paragraph has entry and membership | empty means unavailable as a whole; it does not erase an independently known target/entry |
| Normal completion frontier | each `procedures[].completions` ID is a positive conditional frontier | the list does not prove that every path returns, or that CALL terminates; empty is no positive frontier claim |
| Nested terminal callsite | inner PERFORM is an outer paragraph completion frontier, with its own target/entry | a concrete inner resume may be unavailable because continuation is caller completion; no ordinary fallthrough is invented |
| IF arms / local continuation | existing typed arm entries, membership and successor | predicate/body readiness is independent; missing predicate never deletes a proved arm |
| Body/effect specialization | existing legacy profile qualification remains separate | `STRUCTURAL_FACTS` does not assert whole-body precision or effects completeness |

A known statement entry has its own provenance through its published header. A
frontier means: **if this statement completes normally within this activation**, it
completes this paragraph. It is neither a termination claim nor an unconditional
edge. GOBACK and GO TO are not normal completion frontiers. Plain EXIT and CONTINUE
are neutral. EXIT PROGRAM/PARAGRAPH/SECTION/PERFORM are recognized distinctly and
receive no neutral completion from this change. Inline and SECTION execution,
recursive returns and multilevel VARYING execution remain unsupported.

## Wire and consumers

SP **2.36.0** adds `publicationKind: STRUCTURAL_FACTS` and optional independent
`targetEntry` to `PERFORM_PROCEDURE`. Partial BASIC is the existing one-paragraph,
ONCE range shape, with optional loop/count/varying absent. There is no new ad hoc
Observed variant. Optional target/entry values are KNOWN when present and UNAVAILABLE
when absent; membership/frontier items are positive facts; other body/effect aspects
remain PARTIAL/unclaimed, with coverage diagnostics retained.

The in-memory kind `LEGACY_PROFILE` preserves all historical PERFORM_PROCEDURE
contracts (including their already partial facts). It is not serialized as a new
field and does not assert that all old facts are precise. Old BASIC normalization
also keeps its original variant/profile. Existing consumers may continue qualifying
those historical facts independently. A partially known *new* BASIC or independent
entry with no whole range uses STRUCTURAL_FACTS and requires 2.36. This distinction
prevents a migration from disabling an already supported range, such as 33-C1.

The lower accepts 2.36 into `SpInput`, checks identity/membership/entry/continuation
consistency, includes new facts in deterministic identity material, and retains the
kind. Its existing executable range admission does not specialize STRUCTURAL_FACTS.
This is an explicit consumer capability limit to hand off to W6. Legacy decoders
remain closed; 2.28/2.33/2.35 reject the new fields. Re-encoding the wire tree and
decoding preserves all typed facts. Historical fixture bytes are unchanged.

`gapCodes` report missing coverage or failed whole-profile proof. They do not decide
the existence of a source-positive target, entry, resume, arm or frontier. Removing a
diagnostic does not upgrade STRUCTURAL_FACTS. Existing legacy constructor constraints
remain versioned; new structural facts are not required to satisfy legacy integral
specialization constraints.

## Algorithm and limits

Canonical frontend analysis resolves endpoints by typed identity, extracts the
parser-owned executable entry and range membership, and collects normal completion
from the grammar-owned authority. Finite graph checks still control whole-profile
qualification. Only an open range on a BASIC specialization's own primary path may
invalidate that primary closure; an unrelated unreachable partial peer may not.
Overlap, recursive execution, incoming transfers, missing identities and provenance
remain real constraints. No target is reconstructed from ProgramPoint order or text.
The existing bound O(P * (N + M)) remains; multiplicity is tested at 1/2/5/40.

Source authority: IBM Enterprise COBOL 6.4, [PERFORM](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-perform-statement)
and [Language Reference](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf), plain EXIT / special EXIT formats and CONTINUE.
AIR producer/control contracts are unchanged. W5 ends at source → SP plus wire
acceptance; no new AIR/CFG executable semantic capability is introduced.
