# W5 non-PERFORM audit

Scope: producer canonical analysis and projection, not executable consumer changes.

| Family | Result | Evidence / disposition |
| --- | --- | --- |
| IF | CONFIRMED_SAME_MECHANISM — corrected | IfSemantics `intact` previously required aggregate MODELED coverage before exposing arm entries and successor. Exact source/input now governs structure; aggregate coverage may still restrict integral simpleProfile. M1 changes IF coverage and retains arms, PERFORM targets, entries and resumes. |
| Normal completion / continuation | CONFIRMED_SAME_MECHANISM — corrected for PERFORM boundaries | ProcedurePerformSemantics had a separate statement whitelist from AstBuilder normalCompletionStatements. It now uses the canonical positive authority; EXIT./CONTINUE endpoints are retained, special exits are excluded. CALL completion is conditional, with no external-return guarantee. Disabled CICS body interpretation no longer deletes paragraph entry/membership. |
| GO TO | NO_EQUIVALENT_FOUND in examined supported forms | GoToSemantics.resolve retains target and exact executable entry despite global/selector gaps. Conditional destinations are projected separately. Actual missing resolution/provenance is not an independent coverage gap. Existing lower branch routing/ALTER composition is DEFERRED_DOWNSTREAM (W7); no ALTER implementation added. |
| EVALUATE | NO_EQUIVALENT_FOUND for currently established ordered-arm structure | Existing 2.33 publishes ordered arms, reads and entries with unavailable selectors/predicate precision. `structureKnown` still requires typed termination/provenance and a representable arm layout. Non-simple grouped/empty WHEN surfaces require separate source grouping/arm-availability work; W5 does not invent a completion for an unrepresented arm. Existing consumer join/composition questions are DEFERRED_DOWNSTREAM (W7). |
| MOVE projection | CONFIRMED_SAME_MECHANISM — larger producer contract gap deferred to W8 | The regional-sequence gate requires every transfer to be represented. Mixed supported/unavailable receivers can prevent publishing a supported regional transfer. Nominal operand inventory and statement continuation are still preserved by Observed; per-transfer availability/effect composition needs a separate transfer contract, not the paragraph structural abstraction. No MOVE semantic expansion is made here. |
| Ordinary cross-paragraph MOVE continuation | Larger contract/composition issue deferred to W7 | ScalarMoveSemantics has a legacy conditional-GO-TO unit qualification for copying ordinary paragraph edges into MOVE.nextStatement. Intrinsic activation completion and ordinary entry require distinct consumer contexts. Removing this gate blindly would conflate return with ordinary fallthrough. Existing grammar-owned ordinary edges are not generalized in W5. |

No new global fallback, Unknown-everything, source scanning, label inference or
control.local choice. The deferred producer findings are explicitly not claimed as
fixed. They need independent contracts/negative controls beyond the bounded W5
PERFORM/IF abstraction and do not block the nine W5 witness facts.
