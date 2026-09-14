# Declarative Value Inference — SP 2.15.0 / storage 1.4.0

The configured `ibm-enterprise-6.4-fixed-display-1047@1` profile is still required.
Ordinary WORKING-STORAGE persists; the default CLI remains `unknown`. A VALUE
declaration alone never changes the global entry policy to INITIAL.

Source authority (read 2026-09-14): IBM Enterprise COBOL 6.4
[WS lifetime and INITIAL](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=data-comparison-working-storage-local-storage),
[VALUE format 1](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=vc-format-1).
The existing AST `ProgramAttributes.initial` proves fresh entry for supported WS;
the fixed layout now admits that attribute. Other nonordinary attributes remain
excluded. Explicit INITIAL/PRESERVED continue to express the user's premise.

## Proof contract

Every `storage.entryState.conditions[]` adds required enum `proof`:

| proof | kind | global mode | meaning |
| --- | --- | --- | --- |
| NONE | UNKNOWN | any | missing proof, with gaps and no bytes |
| EXPLICIT_INITIAL | LITERAL_BYTES | INITIAL | existing user invocation premise |
| EXPLICIT_PRESERVED | PRESERVE | PRESERVED | existing preservation premise |
| PROGRAM_INITIAL | LITERAL_BYTES | UNKNOWN | source INITIAL attribute guarantees fresh entry |
| DECLARATIVE_INVARIANT | LITERAL_BYTES | UNKNOWN | lifetime inventory proves unchanged bytes |

All literal conditions retain the existing supported VALUE encoding, bounds,
provenance, and nested VALUE/REDEFINES exclusions. Their byte count equals view
extent. Invariant conditions additionally require independent local WS allocation.
Constructors and publication validation reject contradictory kind/proof/mode.
SP 2.15.0/storage 1.4.0 is the single current writer. Older consumer rejection is
expected until its coordinated reader/pin update; no draft downgrade writer.

## Lifetime proof

`StorageMutationInventory` consumes the same canonical AST/coverage and completed
`StorageAccessSemantics` access/MOVE-sequence maps. It visits every procedure
statement, including unreachable statements and handler bodies. It does not use
reachability to infer lifetime invariance. Exact physical writes are grouped by
base, sorted and coalesced. A binary search tests overlap with the VALUE view.
The declaration can be known only when all relevant inventory gaps are absent.

LANGUAGE_GUARANTEED: VALUE initialization; persistence; physical alias overlap;
no direct access to unexposed local bytes by an argument-free callee.
ARCHITECTURE_GUARANTEED: complete typed input, canonical binding, existing fixed
layout/allocation proofs and selected codec. These are required, not heuristics.
GROUP/REDEFINES/RENAMES use the same component/view infrastructure. A constant
ref-mod preserves the exact slice; dynamic/unproved destinations block inference.

The bounded statement slice is MOVE (including proven correspondence sequences),
IF/EVALUATE, non-VARYING PERFORM, GO TO, NEXT SENTENCE, CONTINUE and GOBACK.
CALL without arguments may read a target; RETURNING must have a proved physical
write. Calls with arguments conservatively block the lifetime proof. Function,
special-register/address and uninterpreted expressions block it, even within a
modeled statement. VARYING/unknown repetition, unsupported statements, incomplete
coverage/input, diagnostics, nested programs, signatures/declaratives, unknown
allocation or foreign effects also block it. No recognized MOVE is not a proof.

The existing CICS contribution alone can certify LINK/XCTL with only PROGRAM and
optional NOHANDLE as input-only for private local storage. The dedicated parser
and canonical host operands supply this fact; disabled contributions supply none.
[IBM LINK PROGRAM/COMMAREA](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-link)
and [argument direction](https://www.ibm.com/docs/en/cics-ts/6.x?topic=values-data-areas-data)
establish the distinction. All other options/commands remain foreign blockers in
this slice. There is no CICS logic in a value solver.

Soundness: local lifetime bytes start at the declared literal and none of the
complete admitted operations can change/expose them. Any missing premise keeps
UNKNOWN. Complexity is O(n+w log w+c log w), finite AST n, exact writes w and VALUE
conditions c; there is no iteration over runtime values or second propagation
engine. Gaps distinguish storage, inventory, overlap, unproved writes, unknown
effects and foreign exposure. This deliberately sacrifices completeness.

## Downstream and evidence

AIR §3.8 already expresses literal entry knowledge with source origin/premise,
open absent conditions and no reseeding on a label/backedge. Lower can use its
existing `LiteralInitial` translator; AIR production/normative changes are not
needed. Existing Regional Values and CALL/LINK/XCTL consumers compute candidates.
Value paths never close runtime/input alternatives merely because a literal exists.

W0 on the integrated CICS baseline: UNKNOWN VALUE→CALL gave no candidate;
explicit INITIAL gave PROGA plus existing model remainder; PROGRAM-ID INITIAL
had NONORDINARY_PROGRAM. All were production CLI runs. The new focused tests
include independent octet goldens, alias/group/slice blockers, mutation pairs,
mixed entry knowledge, INITIAL, preserved policy, unknown effects and coverage.
`DeclarativeValueInferenceTest` is in frontend FAST. No corpus/full gate is added.
Real COACTUPC/COCRDSLC menu paths remain subject to preexisting INPUT_MISSING and
unmodeled effects; candidate gain must be measured, not inferred from VALUE counts.

This implements ADR-0013 and INV-SP-001–010: the projector only transports the
canonical proof, preserving VALUE origin and gaps. No synthetic MOVE or targets
are published by the frontend.
