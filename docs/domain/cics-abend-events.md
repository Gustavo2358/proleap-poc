# Explicit CICS ABEND events — SP 2.42.0

R7-R2 extends normal producer composition with `CICS_ABEND`, retaining the
[SP 2.41 handler operation contract](cics-handler-operations.md) unchanged.
Registration and an explicit abnormal termination request are distinct facts.
Handler state, replacement and dispatch are not implemented.

## Language authority and abstraction

IBM CICS TS documents [ABEND](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-abend)
as an abnormal termination request. Its CANCEL option bypasses exits established
by HANDLE ABEND. It does not assert that no exit exists. ABCODE and NODUMP govern
dump behavior; this producer does not calculate their runtime values. The
[EXEC interface](https://www.ibm.com/docs/en/cics-ts/6.x?topic=control-dfhepc)
and [HANDLE ABEND](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-handle-abend)
separate registration's ordinary return from ABEND and later runtime dispatch.
Runtime retry exits and enclosing logical levels are outside this contract.

The supported syntax is a complete `EXEC CICS ABEND ... END-EXEC`, with at most
one each of CANCEL, NODUMP and ABCODE, in any order. CANCEL/NODUMP have no operand.
ABCODE accepts a nonempty quoted literal or one simple COBOL data-name. Complex
operands, condition options, unknown options, duplicates and malformed payloads
remain UNAVAILABLE. Unterminated EXEC can fail earlier at the frontend input
boundary; no valid AST statement is synthesized to hide that failure.

## Typed event

`CicsAbendSyntax` reads embedded syntax once per CICS payload in the dedicated
`CicsAbendSemantics` AST traversal. Its immutable contribution is keyed by owned
ProgramUnit/AST statement identity. Projectors translate the contribution;
they never reinterpret rawText or resolve an event target.

The new statement variant contains:

- canonical header (StatementId, ordinal, containment, provenance, readiness);
- eventKind = ABEND;
- dispatchEligibility = HANDLER_ELIGIBLE, HANDLERS_BYPASSED or UNAVAILABLE;
- rawText, ordered options with existing payload offsets, and gapCodes.

Plain qualified ABEND is HANDLER_ELIGIBLE. Qualified ABEND CANCEL is
HANDLERS_BYPASSED. Unqualified syntax is UNAVAILABLE, never eligible plus a gap.
The product rejects contradictory eligibility/CANCEL evidence, known eligibility
with syntax gaps, unavailable events without a reason, duplicate/unsupported
qualified options, invalid ordered offsets and published dump operand bindings.
Option syntax is diagnostic/source evidence, not a runtime value or target.
No labelTarget, programTarget, targetEntry, active state or dispatch exists in
this record. Canonical statement provenance preserves COPY/include ownership.

Every event remains PARTIAL with CICS_ABEND_DISPATCH_NOT_MODELED. Eligibility
is positive source knowledge; the gap concerns a separate execution dimension.
No storage/value/dependency fact is produced by this family.

## ControlTopology

ABEND retains UNKNOWN_LOCAL, without normal continuation or an exceptional
destination. The event inventory qualifies handler eligibility independently
of a future control consumer. No implicit CICS failure is treated as an event.

In the new composition only, valid HANDLE ABEND CANCEL/default CANCEL/RESET
and ACTIVATE LABEL with a canonical entry and no local gaps have ordinary
completion. The topology uses the existing grammar-owned next statement or
region completion, with LOCAL_GRAMMAR proof `cics-handle-abend-ordinary-return`.
This does not change CICS_HANDLER, its target or scope. targetEntry is never a
successor. PROGRAM, unresolved and partial registration stay UNKNOWN_LOCAL.
Neither successful XCTL nor ABEND acquires normal fallthrough. The legacy
ordinaryContinuations inventory and storage completion proofs are unchanged;
ControlTopology remains the sole control authority.

## Version and cost

The normal production composition includes event analysis. Following the existing
feature-based writer, a typed event or a registration-completion proof requires
2.42. This version floor also applies to manually materialized ports, preventing
publication of new semantics as 2.41. Products with neither new feature retain
their previous contract and bytes. Historical manual compositions retain their
previous meaning. No publication is downgraded or emitted under two versions.
The State envelope and state-derived immutable port remain unchanged.

The additional traversal is O(AST + CICS payload), with an identity-indexed map.
No per-event label scan, path enumeration or handler state product is created.
Lower/AIR/CFG remain frozen. The pinned lower does not support SP 2.42; its known
FactDependencies diagnostic precedes version admission and yields INPUT_ERROR.

## Executable qualification

CicsAbendContractTest and CicsAbendInvariantTest cover real frontend composition,
negative syntax, event/registration separation, source ownership, COPY,
cardinality, constructor guards, projector authority, topology and byte replay.
CompositionalityContractTest adds this family at 1/2/5/40 occurrences. Historical
R7 and R7-R1 tests retain their assertions and a frozen 2.41 composition. CardDemo
and mutation evidence is indexed by the [work item](../work/positive-cics-abend-r7-r2.md).
