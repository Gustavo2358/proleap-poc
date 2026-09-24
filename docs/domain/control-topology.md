# Frontend control topology — SP 2.39.0

The frontend owns COBOL control structure. The closed Semantic Product transports
that structure; lower binds and materializes it without discovering paragraph,
arm or range completion from legacy statement facts.

## Authority and migration

`controlTopology` is required in 2.39.0. Its authority is
`FRONTEND_CONTROL_TOPOLOGY_R1`. Versions through 2.38 retain their historical
interpretation and reject the new field. New wire without topology is rejected.
The producer's branch already contained 2.38 before this wave. Historical State
constructors and typed ports remain available and do not acquire topology.
New and legacy assemblers are selected once. Legacy control fields remain useful
for compatibility and operation payloads, never as an alternative authority for
completion in the new assembler.

## Closed algebra

Occurrence identity reuses `statement:N`. Region identities use frontend AST
identity plus role; they do not depend on transport ordering or program names.
Every region owns an explicit boundary. Root procedure, paragraph, IF/EVALUATE
parent and arms, FILE parent and handlers, inline body and invocation range are
first-class records. Declarative sections are isolated from ordinary flow.
A RANGE references ordered existing regions and its binding names the last
boundary as endpoint. The range does not duplicate source occurrences.

Targets are OCCURRENCE, REGION_ENTRY, COMPLETE(region), PROGRAM_RETURN or
UNKNOWN_LOCAL(region). Outcomes distinguish NORMAL, BRANCH, EXPLICIT_TRANSFER,
LOCAL_INVOKE, PROGRAM_RETURN and UNKNOWN_LOCAL. COMPLETE is symbolic: the same
paragraph has an ordinary default and can complete the matching active binding.
A THRU intermediate boundary follows its ordinary default; only the final active
endpoint resumes the caller. Arm completion composes through the parent boundary.
An explicit transfer/return does not acquire a completion edge.

Bindings carry their own resume and finite phase graph. BODY and RESUME are
endpoints; predicate/effect phases publish routing explicitly. ONCE, BEFORE/AFTER,
TIMES and single-level VARYING use this graph. Value/effect precision is separate
from routing. Inline repetition may retain unknown predicate/effect values.

## Proof and integrity

Every region, boundary, outcome, target and binding cites proof identities.
Proofs distinguish local grammar, resolved targets, expanded includes, input
isolation and partial unknown knowledge; provenance retains expanded/original
locations and include chains. Missing DATA input does not erase independently
closed procedure syntax. Missing procedure structure stays unavailable.

The same occurrence/outcome inventory drives reference closure and emission,
including event-specific FILE handlers. Targets are bound in the same activation
as the emitting operation. Invalid identities, missing targets, inconsistent
ownership/endpoints, malformed phases and symbolic alias/proof cycles reject
before AIR. Executable control loops remain representable.

## Partiality and scope

UNKNOWN_LOCAL identifies the region whose source control knowledge is unavailable.
It is not a guessed set of destinations and does not claim source impossibility.
The current AIR backend preserves an explicit unavailable projection frontier:
UNSUPPORTED coverage, control UNAVAILABLE, proof/context origins, and an open
control envelope with no licensed labels. No return, divergence, fallthrough,
all-label scope or synthetic caller bypass is inferred. CFG exposes the open
frontier; dependencies distinguish completed computation in the known model from
incomplete source coverage. See AIR 00 §5, 05 §6 and 06 §§1,3.2,8.

Source occurrences outside the entry projection remain explicit coverage items;
unreachable activation trees are not expanded. Recursion retains its existing
unsupported local frontier, without a return bypass. Section-target PERFORM,
special EXIT, multilevel VARYING and unresolved callbacks remain explicit limits.
USE/SORT callback bindings are not implemented in this slice: source occurrences
remain inventoried, unavailable routes are explicit, and SORT cannot bypass a
required callback. The pinned CardDemo 73 has no USE/SORT local callback plans.

## Validation and review

Contract tests cover old/new separation, malformed wire and in-memory parity,
roundtrip, physical permutation and reference mutation. Region tests cover terminal
verb substitution, ordinary versus performed entry, THRU endpoints, two callers,
nesting, predicate/input gaps and FILE handler inventory/event separation.
Real source witnesses, external full73 redistribution and backend scale remain
mandatory campaign gates; unit tests alone do not qualify the wave.
