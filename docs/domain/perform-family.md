# PERFORM family — SP 2.5 range, repetition and implicit effects

Goal: THRU/THROUGH, UNTIL, TIMES and single-variable VARYING end-to-end.
The wave continues through cumulative qualification; review occurs only at its end.

Source authority: IBM Enterprise COBOL for z/OS 6.4 Language Reference,
[PERFORM, printed pp. 413–424](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf),
consulted directly 2026-09-13. Paragraph entry, completion of the final paragraph
and return to the particular PERFORM continuation are language rules. Ordinary
entry into the same paragraphs does not acquire that return. Recursion is outside
the supported profile. THRU and THROUGH select the same grammar relation.

Phase 1 uses the typed Procedure Division paragraph list to establish an ordered
range between resolved local procedure identities. Each paragraph publishes its
executable entry, statement membership and normal completion frontier. Existing
statement facts supply all internal edges. The paragraph list is language order;
statement inventories and ProgramPoints never determine successors.

Precision requires exact input/provenance, nonempty proved boundaries, a closed
primary graph, disjoint primary/body membership, no external incoming edge, no
escape, recursive activation or cycle, and no unequal overlapping ranges. Local
MOVE, CALL without handlers, IF, EVALUATE, GO TO and GOBACK retain their existing
semantics when the graph proof succeeds. A terminal GOBACK never gains a return.
Unproved endpoints/membership/activation retain typed partial facts and open control.

Algorithm: index nodes, symbols and resolved references; join endpoints by typed
identity; collect direct paragraph structure and branch membership; validate each
finite graph with an active/closed worklist. The conservative indexed implementation costs O(P * (N + M)), where P is the
number of range activations, N the unit graph and M the total range membership; specialization is per callsite, never per iteration count. The profile
is conservative and deliberately incomplete; no runtime predicate solver is used.

Independent oracles: last strong update across three paragraphs; two distinct
callsite resumes; structured branch joins; external incoming/escape/overlap/cycle
refusal; 1/2/5/40 occurrences; byte determinism and physical-order permutations.
SP 1.x/2.0/2.1 semantics remain historical. SP 2.2 adds PERFORM_PROCEDURE
with typed start/end, ordered paragraphs and completion frontiers; the historical
PERFORM variant remains unchanged. Causally relevant open primary ranges prevent whole-profile isolation claims; unrelated peers do not erase facts.

SP 2.3 adds an optional typed UNTIL loop to the same procedure-range fact. A
single procedure is the one-member case of that range. The grammar supplies
TEST BEFORE/AFTER; absent TEST means BEFORE (IBM printed pp. 417–418).
The condition retains nominal reads, whole-item access, provenance and the
existing IF predicate guarantee. The initial profile is a proved pure, total
scalar-text equality with unknown truth; unsupported/unresolved conditions keep
the loop occurrence and a partial proof. No constant-condition pruning occurs.

BEFORE enters a decision, whose true edge resumes and false edge enters the
range; completion returns to the decision. AFTER enters the range first and
then uses that same decision/back edge. Primary/body isolation and finite body
proofs are unchanged; the explicit loop is allowed to iterate without a bound.
The existing fixed point must preserve OLDPROG/NEWPROG for BEFORE and only
NEWPROG after the mandatory first strong update in AFTER. Branching bodies,
THRU composition, condition reads, partial peers and multiplicity are cumulative
oracles. SP 2.2 retains its historical loop-free meaning.


SP 2.4 TIMES profile (IBM printed p. 417): a positive integer literal proves at
least one execution; a resolved integer count may be zero/negative and therefore
permits immediate resume. The count is evaluated once on activation entry.
One body and an unknown exhaustion decision conservatively represent repetition;
no count-dependent cloning or artificial count ceiling. The back edge does not
reread the source count. This permits overapproximation of iteration cardinality,
without claiming an exact finite count. Numeric storage facts prove only a local
standalone DISPLAY integer item (PIC 9/S9); they do not give its runtime value or
reinterpret COBOL arithmetic as unbounded integer arithmetic. Existing text
MOVE/CALL guarantees remain separate. Unknown/noninteger counts and unproved
ranges stay typed partial. Oracles: unknown count preserves OLDPROG+NEWPROG;
positive count preserves NEWPROG after a strong update; a million iterations
has the same static body size as five; 1/2/5/40 callsites and prior families regress.


SP 2.5 VARYING design: single elementary DISPLAY integer control item, integer
FROM literal or resolved integer item, nonzero integer BY literal, and a proved
pure total condition with unknown truth. Numeric relations retain each typed
read; no comparison is evaluated. Initialization and each increment perform a
mandatory whole-item write with an open numeric value. AIR Opaque with exact
localized memory/control envelopes preserves the implicit control-item read and
FROM/BY provenance; this avoids inventing bounded COBOL arithmetic in AIR int.
No other memory is written by these implicit effects.

The IBM printed p. 420 diagrams were inspected directly. BEFORE is init → test,
false → body → increment → test, true → resume. AFTER is init → body → test,
false → increment → body, true → resume: the exiting AFTER path does not increment.
The UNTIL decision/activation machinery is reused. Multi-level AFTER is preserved
as typed levels and operands but remains conservative: correct inner-variable
resets and nested condition ordering are outside this slice. Unknown BY cannot
prove the required nonzero increment; unsupported numeric/storage/condition,
open ranges, incoming/escaping control and recursion retain partial facts.

Oracles: BEFORE old/new candidates, AFTER new only, THRU composition, initialization
and iteration must-writes to the control item, count-independent multiplicity,
FROM reads, negative increments, AFTER exit bypassing the increment, and cumulative
byte/order/control regressions. SP 2.4 and older must reject the new wire fields
and predicate profile; their historical meaning is unchanged.

## Positive Memory Topology W5

Whole-profile qualification is separate from positive structural facts. Partial
BASIC now publishes target, independent entry, range membership, conditional normal
completion frontiers and callsite resume under SP 2.36. Historical range facts keep
their prior meaning and consumer admission. Nested nonrecursive facts are in scope;
compositional return execution is handed off to W6.
See [partial structural facts](partial-structural-facts.md).
