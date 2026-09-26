# W7 control contract

Authority: canonical campaign steering/decisions; IBM Enterprise COBOL for z/OS 6.4 Language Reference chapter 9, IF and EVALUATE execution (official PDF: https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf); AIR 05, 07 and O-03/04/09/18/27/56–60 at the pinned analysis-ir HEAD. No new AIR capability or language completeness claim.

| Concept | Producer authority / wire | Lower interpretation | AIR / CFG consequence |
|---|---|---|---|
| Branch entry | Typed IF thenArm/elseArm or ordered EVALUATE arm entry, exact origin and closed StatementId | Independent of predicate evaluation and body coverage | Branch or ordered first-match chain reaches only published arm entries |
| Normal completion | Grammar-owned Ast.Division.normalCompletionStatements plus typed structure | Conditional fact about finishing normally; effects NO_OP does not prove it | Only a normal outcome follows the supplied continuation |
| Intrinsic normalContinuation | Historical family field: MOVE/IF/EVALUATE/PERFORM successor within a structured/paragraph region. CALL historically carries ordinary flow; CICS/FILE have explicit contracts | Preserve family/version meaning; missing is unavailable, NONE is not unit termination | Known successor becomes Jump/Normal outcome only when the handler completes normally |
| Ordinary continuation | SP **2.37.0** optional `ordinaryContinuations` list of `{statement,destination,provenance}`. Projection translates canonical grammar relations for MOVE/IF/EVALUATE/typed PERFORM when the intrinsic field does not carry them | Closed positive relation; never derived from array order/ProgramPoint/name | Supplies normal destination; never bypasses an invocation, unknown repetition, GO TO or GOBACK |
| Contextual completion | SP2.36 PERFORM range `completions` identifies conditional frontiers; activation and callsite resume remain separate | Occurrence-local override precedes ordinary relation. Nested return passes through inner resume then parent completion | Specialized Jump/Branch/Invoke normal outcome reaches next range paragraph or this activation's resume |
| Explicit transfer | GO TO targetEntry; conditional GO TO retains its own destination alternatives/out-of-range continuation | Replaces normal flow for that occurrence | Jump only, no physical fallthrough |
| Unit termination/return | GOBACK states current-program invocation return | Distinct from local completion | AIR Return has no join/fallthrough successor |
| Local invocation return | W6/R1 target, membership, frontiers, supported repetition and contextual resume | Activation specialization with shared COBOL memory | Inner completion -> inner resume -> parent continuation, never another caller's resume |
| Join | Successor may be shared by positively published normally completing paths | Optional result of composition, never a prerequisite | All-transfer branches need no join; Return/GO TO paths are not redirected there |
| No-match alternative | Ordered EVALUATE WHENs and OTHER presence | Last failed decision reaches OTHER entry; absent OTHER uses occurrence normal destination | Ordered Branch chain preserves final false outcome without fabricating OTHER |
| Absent ELSE | `elseArm.presence=ABSENT`, no entry | False uses occurrence normal/contextual destination | False path exists without written ELSE |
| Absent OTHER | `otherArm.presence=ABSENT`, no entry | No-match uses occurrence normal/contextual destination | Final false path exists without written OTHER |
| Missing outcome destination | Neither entry nor applicable normal/contextual destination is established; UNKNOWN/PRESENT-without-entry is not ABSENT | Derived per-occurrence/per-outcome boundary, explicit control uncertainty, no outgoing labels | Branch reaches localized Opaque boundary. It is not a join, Return, Halt or global opening |

## Resolution and independence

Resolve a supplied contextual frontier first; otherwise explicit ordinary relation; otherwise existing family continuation. Handlers use this destination **only on normal completion**. Branch entries are independent. An absent clause may use normal completion; a present/unknown arm with missing entry cannot bypass to completion. Its outcome remains a local gap. GO TO/GOBACK ignore normal destinations and cannot be sources of the new relation.

The producer's grammar completion walk is the positive authority. Legacy MOVE-only/simple-diamond/range qualification remains a specialization predicate, not a branch-admission requirement. EXIT. and CONTINUE complete normally; special EXITs do not inherit that fact. Gaps are diagnostics and cannot select edges. Unknown predicates are pure BOOL abstractions of the modeled decision; known reads survive without invented numeric/scalar proofs. Publication validates available accesses before claiming WholeItemAccess.

## Wire, absence and consistency

Additional ordinary relations select SP2.37.0. Historical fields and SP2.36 PERFORM facts retain their meanings. Missing relation means no additional fact, not NONE/termination. Each source is unique, endpoints are in the same published unit, provenance is exact, and a known intrinsic successor must agree. GO TO, GOBACK and arbitrary OBSERVED/special EXIT cannot acquire an ordinary relation through this extension. No completeness claim is made about the relation inventory.

The decoder rejects this field in old versions, preserves historical decoding, and materializes it before admission. In-memory admission enforces the same closure/source-kind/consistency rules. Canonical AIR revision includes the sorted relation inventory; JSON statement/relation order is not semantic. No source rediscovery is permitted downstream. AIR validator/codec and CFG continue using core operations.

## Complexity and limits

Relation translation/indexing is linear in the statement/control inventory; Branch normalization is linear in arm count. Each missing outcome adds at most one local boundary per occurrence, with no effects/reads/return claim. No branch-path enumeration or new activation cloning is introduced.

No predicate truth evaluation, inline/SECTION/recursive PERFORM execution, special EXIT semantics, ALTER retargeting or INITIALIZE transformation. Live shared activation DAGs may still expand by context. **Any published FILE operation use** currently selects eager activation scheduling, not just proven callbacks. This is an operational scheduling limit, not an executable edge.

Ordinary inventory is closed under published successors/arms/transfers before removing legacy specialized body copies. A positive ordinary incoming relation therefore keeps its destination occurrence. This is reference closure, not entry reachability: dead source regions stay dead, and legacy activation wrappers/return pairing are unchanged.
