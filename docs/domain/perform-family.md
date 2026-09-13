# PERFORM family — SP 2.3 range and UNTIL contract

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
PERFORM variant remains unchanged. Open peer ranges prevent isolation claims.

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
