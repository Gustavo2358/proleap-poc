# IBM language authority (frozen before production patch)

Primary sources queried 2026-09-25; raw search output: campaign evidence archive (finish-r7/ibm-search.json).

- [XCTL](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-xctl): successful transfer replaces the executing program at the same logical level. PGMIDERR has abnormal task termination as its default disposition. Resource/runtime failure itself is not statically known.
- [Default exception handling](https://www.ibm.com/docs/en/cics-ts/6.x?topic=conditions-default-cics-exception-handling): RESP and NOHANDLE suppress default exception processing for the command; HANDLE CONDITION/IGNORE CONDITION can change disposition. RESP2 alone is not a suppression premise.
- [Abend exits](https://www.ibm.com/docs/en/cics-ts/6.x?topic=processing-how-it-works-abend-exit-code): one active exit per logical level; selection searches local then enclosing levels; CICS deactivates the selected exit before executing it. A subsequent ABEND does not reenter that inactive exit. Outer runtime levels are outside this analysis.
- [HANDLE ABEND](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-handle-abend): RESET reactivates an exit canceled by the command or by CICS. COBOL returns to the HANDLE command with registers restored and performs a GO TO. This does not establish a return to the faulting PERFORM callsite.

## Analyzer abstraction, distinct from runtime semantics

A guarded XCTL/PGMIDERR event publishes **condition raised AND default disposition applies**, not actual failure or absence of condition handlers. Unknown condition handling is an explicit premise/remainder, never silently discharged. Default-action conditional ingress is classified BOUNDED, not unconditional runtime reachability. Explicit ABEND is a separate origin with its published eligibility; CANCEL/bypassed and unavailable events never create local entry.

Handler entry deactivates the selected local registration with saved identity (DEACTIVATED). RESET can reactivate it. Semantic entry uses only a resolved local targetEntry and its canonical provenance. Program targets remain selections without a fabricated local destination.

Matched normal PERFORM behavior is unchanged. Handler code runs in a dedicated nonreturning semantic entry context; nested PERFORMs use existing matched summaries. Restoration of an interrupted COBOL PERFORM continuation is not inferred: if a handler path reaches an unbound paragraph completion it stops with HANDLER_COMPLETION_CONTEXT_UNAVAILABLE. Explicit branches and terminal ABEND are analyzable before that bound. This is a local capability boundary, not a false return to a caller.

# SP 2.45 / semantic entry — source contract

T1 census: campaign census plus R7-R7C reachedFrontiers includes all four physical products, typed operations, opaque SEND TEXT and RETURN. The common reached XCTL sites supply a minimal command-specific guarded event. READ/RECEIVE with RESP do not supply default-abend entry. SEND MAP OVERFLOW and opaque families retain their existing remainder; they are not generalized into ABEND. SEND TEXT and RETURN need no implementation to qualify this handler route.

Extend ControlTopology with an optional-on-old-wire exceptionalEvents inventory. Each descriptor has local id, source statement, origin (EXPLICIT_ABEND or XCTL_PGMIDERR), fixed disposition, eligibility, current relative logical-level scope, runtime identity UNAVAILABLE, closed typed premises and proof ids. No handler destination is published by the frontend. Empty inventory is omitted on historical wire. SP2.45 selected only with nonempty inventory. Lower closed profiles reject nonempty exceptional inventories under older versions and unknown future versions before shape checks. Domain and factual validation verify event/source consistency.

The state consumer evaluates descriptors only at reached source nodes. ACTIVE known local LABEL yields a semantic HandlerEntry with event+activation+target proofs, state-before and DEACTIVATED state-after. No ACTIVATE edge, no UNKNOWN_LOCAL dispatch. All other state alternatives preserve unknown/inactive/outer remainder without invented destination. Explicit event and command-condition assessments stay distinct. Conditional ingress remains visible in downstream event classification and its support DAG.

Bounded finite domain: one additional inactive-known atom per target, finite event/activation ingress contexts, two conditionality classes. Existing invocation contexts gain only a finite ingress discriminator. No history stack or path enumeration. Handler paragraph completion requiring unknown interrupted-context restoration is a local frontier.

AIR2.0, executable readiness, NonExecutableCapability and dependency policy unchanged. Exceptional-event inventory is source-only; it is not an executable AIR successor. Existing historical inputs without the feature keep prior semantics.

## Frozen new oracles

1 explicit eligible ABEND + ACTIVE(A) enters A with DEACTIVATED(A).
2 ABEND CANCEL has no local entry; unknown eligibility has no entry.
3 XCTL PGMIDERR publishes conditional/default premises only without RESP/NOHANDLE and supported shape; no ordinary return is added.
4 registration alone, opaque CICS, RETURN, unrelated condition never enter A.
5 CANCELED(A), DEACTIVATED(A), ENTRY_UNKNOWN never invent an entry.
6 handler ABEND does not reenter itself unless RESET or new ACTIVATE restored eligibility.
7 replacement selects only latest causally executed target; dead registration is irrelevant.
8 normal PERFORM completion resumes its own caller, never adjacent handler; nested handler PERFORM returns correctly.
9 unknown interrupted completion remains bounded; outer unknown is not global absence.
10 new source ingress does not add executable AIR handler edges or remove positive publication.
11 copy provenance, duplicate unit identities, worklist/input order and independent storage remain invariant.
12 all 9 real events receive assessed or explicitly guarded/bounded classifications with proof DAGs; four old-unreached cases must not be reported unconditionally reached.

## Review status and verification

IN_PROGRESS — draft review, no merge. New frontend test ExceptionalHandlerContractTest produces real fixtures; lower ExceptionalHandlerSuite checks selection, deactivation, RESET, bypass, replacement, dead code, normal and exceptional invocation contexts, local completion bound, COPY, inventory/worklist determinism and strict bounded AIR. Existing frozen fixture bytes stay immutable. Unknown-future-version vectors move mechanically from 2.45 to 2.46.

Runtime PROGRAM binding is not a premise for a guarded PGMIDERR possibility. The already published CICS_HOST_BINDING_UNAVAILABLE gap remains independent of that control fact. No failing resource or default condition table state is invented.

Historical typed-input roundtrip may explicitly serialize an empty exceptionalEvents list; this absence carries no 2.45 semantics and is accepted in old profiles. Nonempty inventories require 2.45. The frontend omits empty inventories.
