# W6-R1 directed design — before production changes

Input: W6 lower 8bb58b7e13451db736646d20660e268f32530b48, all four other approved W5 pins unchanged. Exact clean HEADs recorded in entry-heads.json. Both read-only freeze verifiers PASS w4-b.2. W6 evidence remains immutable; R1 evidence is separate. Existing Draft PR #34 only, no merge/W7.

Authority: canonical continuation/decisions and W5/W6 discovery/contracts already read during this campaign; pinned SP perform-family contract 2.3 UNTIL,2.4 TIMES,2.5 VARYING reread for this revision, AIR control/producer obligations and coverage I-29 unchanged. No new language interpretation or capability is required. Scope classified C4 (shared occurrence identity and activation materialization); focused RED/GREEN, neighbor handlers, full lower qualification, FAST, all frozen corpus and source counterexamples are required because prior W6 evidence did not cover these compositions.

## F01 occurrence coverage

Confirmed real source pipeline RED: computed CICS LINK and UNLOCK fail OUTPUT_INVALID even with one activation, because the ordinary occurrence coexists; two activations also fail. Literal LINK is a positive control. IF with a referenced predicate also fails, exposing the same gap in IfPredicate. Source provenance and memory remain shared; operand coverage keys must include LocalIds.sourceKey for CicsInvokeHandler target/options, CicsFileInvokeHandler.Context.finish, and IfPredicate reads. Existing CALL/Move/RegionalMove already qualify occurrence keys. Data/storage/entry inventories are publication-level and must not be specialized. Other assembler source keys are legacy top-level-only; PartialProgramAssembler's statement keys already contain operation ID. No deduplication or validator weakening.

Options require a source-produced regional storage profile to materialize the option places; test that separately from nominal default mode. Preserve both variants and first-loss, never claim an unmaterialized option tested its handler branch.

## F02 supported repetition as an independent wrapper

Actual fixed frontend publishes positive TIMES profile and known loop predicates independently of failed whole-body/primary/resume qualification. All 12 inner/outer TIMES/UNTIL/VARYING combinations lose execution at lower, and TIMES + omitted INITIALIZE also fails. The unavailable-count negative remains a gap.

Admit the new composition path only for targetEntry known and independently executable repetition: count profile POSITIVE_INTEGER/INTEGER_ITEM; known validated loop predicate; single-level executable VARYING. No gap-code gate. Unknown/noninteger count, unavailable predicate and multilevel VARYING are not ONCE. Use the same loop construction for legacy and compositional routes: positive TIMES body first; integer item entry zero/body branch, count read once; UNTIL BEFORE decision first, AFTER body first; VARYING initialization and correctly placed increment, no numeric evaluation/unrolling. Body completion enters repeat/decision, exit goes to this activation's explicit resume. Nested resume can be supplied by a parent frontier even without a concrete source successor. Independent INITIALIZE effects stay a local omission.

## F03 inventory versus expansion

Measured baseline small: cold shared DAG depth2/4 emitted44/216 AIR blocks and15/83 structural resumes; live depth2/4 emitted62/294 blocks. Live chain depth16 emitted362 blocks, including many unnecessary ordinary-root expansions. Medium measurements continue under40s/stage,768MiB heap; stop escalation on any budget failure.

Use a finite per-occurrence demand proof to avoid expanding dead invocation trees, while keeping every ordinary source occurrence in AIR. Demand follows only explicit SP successors, IF/EVALUATE arms, GO TO targets and this context's published frontier routing, rooted at declared entry/activation entry. PERFORM's normal successor is a may-return upper bound for scheduling only: it cannot add an AIR edge. Unknown outcomes never manufacture demand. Undemanded PERFORM remains an inventoried local opaque construction with an explicit not-materialized-in-entry-projection reason, not an executable jump/return or source deletion. This optimization is conservative: when FILE declarative/SORT auxiliary routing can add edges outside the statement graph, retain eager scheduling (no semantic fallback or control widening). Document that limit rather than silently missing a FILE callback.

Replace Java recursive append calls with an explicit task deque. A task describes one finite context, its body and completion routing; child tasks extend LocalIds. This removes JVM-stack dependence from lower activation assembly. Recursive callsite detection stays local and explicit; source loops use visited sets. Demand never changes memory effects or modifies already admitted legacy profiles.

Flat expansion is O(A*B), scheduling is output-sensitive plus per-context explicit graph traversal. Shared live DAGs still have many distinct contexts, as specialization requires. Measure cold/live2/4/6/8 then10/12 only after safe smaller completion; chain16/64/128/256 likewise. Context descriptor lengths and emitted occurrences are measured. Operational time/heap budgets belong to the external runner; no semantic depth/cardinality cutoff is introduced. A compact arbitrary shared-context product requirement would need a separately reviewed representation, not an unmeasured rewrite here.

## Falsifiers / gates

Any missing known resume/body path, TIMES zero bypass for positive count, lost zero path for integer count/BEFORE, VARYING AFTER increment on exit, CICS duplicate coverage, lost operand origin/output, context contamination, dead expansion activated as code, omitted source inventory, recursion bypass, legacy regression or negative39 failure blocks closure. Test correlation/valid AIR first, real candidates second. Keep baseline/new results and all sources/SP/AIR/CFG/dependencies separately, with per-counterexample first loss.
