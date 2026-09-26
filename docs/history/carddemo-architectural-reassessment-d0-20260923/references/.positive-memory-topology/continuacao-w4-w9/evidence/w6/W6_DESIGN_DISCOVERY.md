# W6 design discovery — before production changes

Entry: all five approved W5 HEADs match, worktrees clean, campaign branches and Draft PRs verified in entry-heads.json/entry-prs.json. Freeze w4-b.2 passes both read-only verifiers (freeze-before.log). Canonical master and decisions, W4 qualification/revisions/site matrix, qualified manifest/metadata, all eight W5 deliverables and producer SP2.36 contract were read. No oracle changes authorized.

## Twelve design questions

1. Legacy lower validates an isolated primary and range graph in ProcedurePerformAdmission.qualify; every visited body member must belong to `precise`. PartialProgramAssembler clones each admitted range using activation-specific LocalIds and maps paragraph completions to the next published paragraph entry or resume. BASIC MOVE-only uses its existing separate route.
2. Sibling callsites obtain distinct labels/operations using their StatementId. Memory/data identities are translated once and shared. Source coverage correlates cloned occurrences back to the written site.
3. The representation can express the W6 witnesses, but current admission rejects structural-only/nested/neutral members. LocalIds.activation currently replaces, rather than extends, parent context: nesting under two callers would conflate inner operations. This must change before nesting is enabled.
4. SP completion lists identify conditional statement normal completion. The assembler supplies a normal destination to the statement handler; it must not replace an explicit transfer/return with an unconditional jump.
5. Each activation will own a synthetic completion/resume block. A body frontier targets that block; it transfers only to that activation's supplied resume. CALL uses its Normal outcome. GOBACK retains Return; GO TO retains its explicit destination.
6. A nested call gets context(parent, callsite). Its resume block returns to a block in the parent context. If the nested occurrence is an outer frontier, the parent continuation is the outer completion block (or next paragraph). This keeps both composition steps visible.
7. Nonrecursive finite activation contexts can be specialized statically. Dynamic recursion requires a stack or summaries; it remains unsupported. Recursive expansion is detected from typed activation membership, never from gap names. Preserve a bounded partial boundary, never silently truncate an allegedly exact return.
8. CFG CoreCfgProjection handles explicit Jump, Branch, Invoke Normal and Return. ContextView indexes by unit Entry, not local frames; ReachabilityProvider performs BFS over those published edges. Distinct static labels already encode the local contexts needed by unchanged CFG/dataflow.
9. Flat multiplicity is O(A*B) emitted body size. A chain of nested activations has finite expansion. General acyclic shared call DAGs can yield many distinct contexts: specialization does not claim a polynomial bound in source size. Measure actual contexts/blocks and depth, avoid iteration unrolling and any arbitrary cardinality cutoff. This output-sensitive bound is an explicit limitation, not a hidden semantic limit.
10. Principal risks: replacing parent context; duplicate coverage identities for a CALL translated multiple times; accidentally using an intrinsic body end as ordinary fallthrough; pairing a nested completion directly with an unrelated caller; aliasing memory clones. Tests target each risk.
11. Select **ACTIVATION_SPECIALIZATION**. No missing AIR operation is required for finite nonrecursive witnesses. control.local@1 exists in air-java model/codec/validator but CFG declares it unsupported; choosing it would require a new contextual projection/analysis contract without a witness requiring that redesign here.
12. Falsifiers: cross-return in 08/34 or nested repeated outer; body GOBACK reaching resume; 39 DEAD becoming live; recursive infinite expansion; entry-only facts acquiring invented range/return; legacy loop topology changing; growth beyond justified emitted contexts. Such failures block delivery, not oracle updates.

## Strategy comparison and integration agreement

A reuses AIR core operations and all existing per-statement effect handlers. Add a separate source-structural ONCE composition route only where the legacy route has not admitted the activation; keep admitted LEGACY_PROFILE/BASIC loops and ranges intact. Full effect precision does not gate entry, membership or conditional completion. Preserve ordinary occurrences separately when their isolation is not proven. Unknown membership permits an entry edge but no manufactured activation return. New route does not reinterpret unsupported TIMES/UNTIL/VARYING as ONCE.

B would share body operations and use semantic frames/ports; it naturally represents recursion and large shared-context graphs. However, the current CFG cannot enforce its matching rules, and fallback to all resumes violates the exact context controls. Implementing B is not the minimum required for this wave. No WHY_EXISTING_SPECIALIZATION_IS_INSUFFICIENT counterexample was found within the qualified nonrecursive W6 cases.

Authority: pinned analysis-ir 05 §§1–5,7; 07 PROD-01/02/03/04/07/09/10 and §§3,8; conformity O-56/57/58; SP2.36 partial-structural-facts contract. These fixed normative rules permit decompositions and source-correlated distinct clones. No source-language rediscovery is needed. Gap diagnostics remain coverage; no execution branch may test gapCodes.

## Validation and scope

RED then GREEN permanent lower tests; input version/rejection and AIR validator/codec; M1–M7; 1/2/5/40 and depths1/2/3; real source→SP→lower→AIR→CFG→dependencies qualified corpus and context/negative gates; determinism; lower FAST and local qualification/full relevant suites; remote FAST at final HEAD. Reuse unchanged producer/AIR/CFG component evidence, but reexecute integrated boundaries. Record any genuine W7 first loss. Stop after W6; no merges, probes/oracles/fixtures untouched.

## Implementation refinement: partial graph closure

A target with unavailable range still needs its own occurrence: jumping to a global original could lose that label when a legacy isolated activation owns it. The new route therefore clones the finite closure of **explicit SP edges**, starting at targetEntry and published members. This is not new paragraph membership: only the published completion IDs gain contextual return destinations. Known GO TO edges retain the same activation, GOBACK remains terminal, and an unknown edge ends at the existing local gap. No source/physical order is consulted. A visited set makes source graph cycles finite; activation recursion is separately bounded by the typed callsite context and published as unsupported. Ordinary source occurrences stay separate from activation clones.

Exploratory 09 now validates after CALL operand coverage keys became activation-specific; this is the same multiplicity defect exposed by 08, not an independent 09 expansion.
