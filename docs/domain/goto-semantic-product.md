# GO TO first slice — SP 2.1

Rule: [IBM Enterprise COBOL unconditional GO TO](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=statement-unconditional-go).
A local unconditional transfer enters the named procedure's first statement;
statements after the transfer are not executed through normal continuation.
ALTER can change that transfer, so this slice refuses precision in units containing
ALTER. DEPENDING ON, sections and unresolved/ambiguous targets remain conservative.

Reuse typed GO TO AST and GO_TO_TARGET nominal resolution. The paragraph's
executable entry is a direct grammar relation recorded by AstBuilder; an empty
paragraph or an unmaterialized first executable has no entry. Index references,
local symbols and AST nodes once, then join by identities. No name matching,
ProgramPoint sorting or inventory position decides the destination. Complexity
is linear in nodes, symbols and references, with finite indexed lookups per GO TO.

SP 2.1 adds GoToFact with optional paragraph identity, reference/paragraph origins,
optional executable entry, entry origin and explicit gaps. A precise fact requires
local unique resolution, unaffected procedure input, exact origins and known entry.
Missing DATA COPY alone does not invalidate control. Unknown containment retains
partial control. There is no normalContinuation field on GoToFact.

Lower consumes only these published facts and emits existing AIR Jump. Incomplete
facts retain Opaque unit control with no explicit fallthrough. IF/EVALUATE branches
may jump outside their containment without acquiring their owner's continuation.
BASIC specialization must still prove a closed primary region disjoint from its
intrinsic body; cycles or a jump into that body cannot qualify it.

Oracles: G1 skips PROGB; G2/G4 join values and skip BADPROG; G3 preserves sites;
G5 strong update; backward/cyclic targets; 1/2/5/40 independent occurrences;
empty, unknown, ambiguous, section, ALTER and DEPENDING ON targets stay open.
