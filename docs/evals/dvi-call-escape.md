# DVI-E1/E2 — CALL USING physical exposure

**READY FOR HUMAN REVIEW. E18: PENDING_SOURCE.** No claim about the four production
sites; their `.cbl` has not been provided. No merge or auto-merge authorized.

## Change and proof

Previously any CALL USING inserted a global FOREIGN_MUTATION_OR_ESCAPE gap.
Even `77 PGM PIC X(8) VALUE 'PROGA'` with `CALL PGM USING ARG-AREA` lost PROGA when
ARG-AREA had a proved independent allocation. E0 reproduced UNKNOWN/NONE and an
empty dependency candidate set with the production CLIs. CALL without USING on
the same declaration retained PROGA. Five focal tests were expected RED.

The inventory now separates global blockers, indexed physical writes, indexed
physical exposures and unknown exposures. Only typed REFERENCE + VALUE-kind +
DataReference arguments can supply exact exposure. Both allocations must be
independent for distinct bases to prove disjunction. Same-base intervals use
`[offset, offset + extent)`, through canonical REDEFINES/RENAMES/group/ref-mod views.
The candidate query accumulates independent blockers instead of returning early.

Production changes are confined to:

- `StorageAccessSemantics.java`: materialize the canonical CALL_ARGUMENT access role.
- `StorageMutationInventory.java`: build exposure indices and query per candidate.

The [domain rule](../domain/declarative-value-inference.md#candidate-specific-call-exposure)
states the authority, premises, algorithm and limits. SP **2.15.0**, storage
**1.4.0**, DECLARATIVE_INVARIANT and LITERAL_BYTES are unchanged. No production,
contract or pin changes in lower, AIR, CFG, analysis-ir or Regional Values; no
new dataflow, text scan or synthetic MOVE.

## E01–E18

All focal assertions below are in `DeclarativeValueInferenceTest`. `invariant`
checks exact IBM1047 bytes; negatives retain UNKNOWN with explicit gaps.

| ID | Case | Final evidence |
| --- | --- | --- |
| E01 | CALL without USING | PASS, PROGA retained through dependency |
| E02 | independent argument base, default reference | PASS, RED→GREEN; PROGA recovered in dependency |
| E03 | target passed BY REFERENCE | PASS, UNKNOWN/NONE; dependency empty |
| E04 | overlapping REDEFINES | PASS, FOREIGN_MUTATION_OR_ESCAPE; dependency empty |
| E05 | overlapping RENAMES, single item and THROUGH | PASS, FOREIGN_MUTATION_OR_ESCAPE; THROUGH dependency empty |
| E06 | group containing target | PASS, FOREIGN_MUTATION_OR_ESCAPE |
| E07 | disjoint sibling in same base | PASS, RED→GREEN; PROGA recovered in dependency |
| E08 | independent base, explicit BY REFERENCE | PASS, RED→GREEN |
| E09 | disjoint constant ref-mod | PASS, PROGA recovered in dependency |
| E10 | overlapping constant ref-mod | PASS, UNKNOWN |
| E11 | dynamic ref-mod | PASS, FOREIGN_MUTATION_OR_ESCAPE; dependency empty |
| E12 | missing binding/access/storage | PASS, UNKNOWN; removing only canonical argument access invalidates proof |
| E13 | ADDRESS OF and other forms outside slice | PASS, UNKNOWN across REFERENCE/CONTENT/VALUE address forms |
| E14 | irrelevant other CALL and per-candidate exposures | PASS, unrelated candidate remains invariant; overlapping candidate blocked |
| E15 | real write | PASS, still blocked; OVERLAPPING_WRITE coexists with foreign gap |
| E16 | independent unknown foreign effect | PASS, still blocked; no unknown effect becomes empty |
| E17 | known literal plus runtime alternative | PASS, RED→GREEN; PROGA plus model/source/interpretation remainder |
| E18 | four real sites | PENDING_SOURCE; no measured gain asserted |

Additional adversaries cover argument order/multiplicity, mode inheritance,
subscripts, nonlocal allocation, RETURNING, handlers and unreachable overlapping
CALLs. Inventory covers the whole lifetime, not only reachable sites.

## Newly executed

- Focused disjoint tests: **2/2 PASS** after implementation.
- Original E0 focal set: **21/21 PASS**, from **16 PASS + 5 expected RED**.
- Final focal/adversarial set: **23/23 PASS**, zero errors/skips.
- **One local frontend FAST: 293 tests PASS**, zero failures/errors/skips;
  policy checks also PASS. No harness/profile changes.
- **Nine selected source → SP → lower → AIR → AnalysisDependencies verticals PASS**.
  Assertions check exact candidates, proof, bytes, BEFORE observation, support,
  source-proof origin, immutable stage inputs, open remainder and no synthetic MOVE.

| Selected vertical | Before candidate | After candidate | SP after |
| --- | --- | --- | --- |
| E01 no USING | PROGA | PROGA | DECLARATIVE_INVARIANT |
| E02 independent | empty | PROGA | DECLARATIVE_INVARIANT |
| E03 self | empty | empty | NONE |
| E04 REDEFINES | empty | empty | NONE |
| E05 RENAMES | E0 SP UNKNOWN; dependency not run | empty | NONE |
| E07 sibling | empty | PROGA | DECLARATIVE_INVARIANT |
| E09 constant slice | E0 SP UNKNOWN; dependency not run | PROGA | DECLARATIVE_INVARIANT |
| E11 dynamic slice | E0 SP UNKNOWN; dependency not run | empty | NONE |
| E17 mixed runtime | E0 SP UNKNOWN; dependency not run | PROGA | DECLARATIVE_INVARIANT |

All nine sites retain modelValueRemainder, sourceValueRemainder and
interpretationUnknownRemainder = true. A known target does not close the CALL
signature/effects/control or the runtime alternative in E17.

Commands from the dedicated worktree (Maven cache reused):

```sh
mvn -o -B -ntp -Dmaven.repo.local=../.dvi/m2 -Dtest=DeclarativeValueInferenceTest test
FRONTEND_MAVEN_REPO=../.dvi/m2 python3 -B scripts/harness/lean.py fast
```

Actual commands with absolute paths, source, SP, AIR, dependencies, logs and hashes
are retained locally under `artefatos-e2e/dvi-escape-20260914/`, with baseline in
`e0/` and final executions in `e2/selected/`. The E0 record is unchanged.

## Reuse, limits and omitted gates

Reused evidence: E0's RED result and five baseline dependency executions. Base
production content is identical after the required fetch. Reused **builds**:
lower `46828657c02f6c01cd4cea258b1f4cc1cab4352c`, analysis-cfg
`b5c98d01589c82b769abe1be83da0e9b5ed0a4e4`, air-java
`eaf83c6233d347348a3927b5983de03cde62554a`. All 310 downstream build files match
E0 hashes. The nine final vertical executions are new, not reused test results.

BY CONTENT, BY VALUE, ADDRESS OF, OMITTED, literals, arbitrary expressions,
dynamic slices/subscripts, unresolved binding, unproved layout/allocation and
missing argument inventory stay conservative. CALL RETURNING remains subject to
the existing physical write proof. Unknown downstream interaction effects can
still reduce value knowledge later in a program; this does not close those effects.
No SET, INITIALIZE, file semantics or COMMAREA capability was added.

Not run: frontend/lower/CFG full, CardDemo 73, historical mutations, all 25 DVI
E2Es, entire Storage/CICS campaigns, downstream FAST. The change is confined to
the frontend lifetime escape proof; focal adversaries, one frontend FAST and
selected unchanged-contract boundaries cover its impact. No evidence required
escalation. E18 remains PENDING_SOURCE per user authorization.

## Git handoff

- Original checkout: `/home/gustavo/workspace/teste-e2e/proleap-poc`, clean and
  unchanged; fetch updated refs only.
- Fetched base: `a5a06ce6d5eb416b40cc35ce6b6b7e58ee8f72f5` (`origin/main`).
- Branch: `fix/dvi-alias-aware-call-escape`.
- Worktree: `/home/gustavo/workspace/teste-e2e/proleap-poc-dvi-escape`.
- E0 test commit replayed from approved evidence: `7e63803`.
- Production/tested commit: `071a8b84141e6b4b9be9c569ba6975ec7af3bef1`.
- Subsequent handoff commit changes documentation only; final head is recorded
  in the Draft PR and local handoff. No downstream repin in this slice.
