# CICS handler operations — SP 2.41.0

R7 publishes operations on an ABEND exit, not its runtime state or dispatch.
The version is a minor contract expansion from 2.40.0: `CICS_HANDLER` is a new
statement variant. Consumers must explicitly admit it; no downgraded or dual
publication may erase the new semantics. Existing manually composed earlier
feature subsets retain their explicit historical versions. The production
composition with FactDependencies publishes 2.41.0.

## Language authority

IBM CICS TS [HANDLE ABEND](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-handle-abend)
defines LABEL/PROGRAM activation, default CANCEL, RESET reactivation and common
NOHANDLE/RESP/RESP2 options. [Abnormal termination recovery](https://www.ibm.com/docs/en/cics-ts/6.x?topic=applications-abnormal-termination-recovery)
describes a single active exit per logical level, replacement, automatic
disabling on dispatch and search of enclosing runtime levels. These are language
premises, not corpus-derived rules. ABEND CANCEL is a separate command.

## Analyzer abstraction

| Field | Meaning |
| --- | --- |
| header | Existing owned StatementId, point, containment, coverage/readiness and source provenance |
| handlerKind | ABEND only |
| action | ACTIVATE, CANCEL, RESET, or UNAVAILABLE for unsupported/conflicting/incomplete syntax |
| targetKind / targetSyntax | LABEL / PROGRAM and preserved operand, NONE for CANCEL/RESET, UNAVAILABLE for unproved action |
| labelBindingStatus | Canonical nominal resolution status, present for LABEL only |
| labelTarget | Only a uniquely selected local procedure identity plus declaration provenance; absent for unresolved/ambiguous targets |
| targetEntry / entryOrigin | Optional canonical paragraph entry statement and its provenance; structural binding, **not** an activation edge |
| targetOrigin | Operand SourceMap provenance; transformed source is never promoted to exact |
| programTarget | Literal text or DATA reference with existing nominal binding and access evidence; no inferred runtime program name |
| scope | CURRENT_EXECUTION_LOGICAL_LEVEL, relative to execution of this statement; runtimeIdentity=UNAVAILABLE |
| rawText / options | Preserved command and ordered options/operand offsets; options may retain DATA references |
| gapCodes | Localized unresolved dimensions, including execution/state/dispatch not modeled |

ACTIVATE describes establishing the specified exit **on successful command
execution**. It does not prove the statement is reached, succeeds, or dispatches
an abend. A second activation is a separate operation; a future state consumer
must apply replacement, not accumulate targets. CANCEL disables the exit at the
current logical level, retaining the semantic possibility of RESET. RESET
reactivates the previously canceled exit; it carries no new target. NONE means
this operation specifies no new target, never “no handler currently exists.”

Source ProgramUnit ownership is not a runtime CICS logical level. The relative
scope carries no runtime level identity, stack, initial state or lifetime proof.
A section target can retain nominal binding with entry unavailable. Ambiguous
LABEL resolution retains status but no selected target; no label enumeration,
nearest declaration or textual destination inference is permitted. Missing DATA
binding retains canonical nominal gaps. PROGRAM resolution does not provide a
PROGRAM dependency producer. Condition response options do not mutate ABEND
registration and RESP2 alone proves no continuation.

## Architecture and limits

The dedicated CICS syntax analyzer uses the existing quote-aware scanner. The
host bridge sends LABEL through the COBOL procedure-name grammar; PROGRAM and
response variables use the existing identifier grammar. AstBuilder anchors
operands in SourceMap. ReferenceOccurrenceCollector emits a CICS_HANDLER_TARGET
procedure occurrence. Canonical nominal resolution alone chooses its binding.
CicsHandlerSemantics joins resolution and declaration identities within the
owning unit; the projector only translates that immutable contribution.

ControlTopology is unchanged. No ordinary continuation, dispatch edge, enabled
state, initial registration, saved-target stack or event probability is
published by this slice. All valid facts remain PARTIAL; CFG/effects readiness
is blocked pending an explicitly qualified consumer. HANDLE CONDITION, PUSH/POP,
ABEND dispatch, ALTER, R8/R9 and global CICS modeling remain outside this slice.

The syntax cursor terminates by consuming payload characters. Analysis indexes
resolution once and scans each owned AST once, with per-unit symbol maps:
O(AST + references + payload), excluding existing grammar and nominal resolver
costs. It does not enumerate execution paths, clone CFGs or rescan all labels
for each statement. The soundness boundary is nominal operation availability;
runtime activation and dispatch are deliberately not inferred.

Tests: `CicsHandlerContractTest` exercises the real preprocessor/parser and root
publication, including missing/ambiguous targets, qualified procedures, PROGRAM,
common options, distinct units, COPY, topology equality and determinism. Frontend
FAST and full local qualification preserve the other semantic families. R7
campaign evidence remains under the aggregate `.positive-memory-topology/positive-cics-handler-r7/contract-expansion/`.
