# Compositionality and conservative partial lowering

The default pipeline consumes structurally usable programs. Each supported region
has precise AIR; each semantic gap has a conservative operation and local
uncertainty. A semantic gap does not remove its program or dependency sites.

## Permanent invariants

1. **No Artificial Cardinality.** Capability limits semantic forms, never a finite
   number of occurrences. New features must pass 1, 2, 5 and a larger N, plus
   mixtures. A unique selected candidate in a RESOLVED nominal reference is a
   legitimate identity invariant, not a program/profile cardinality gate.
2. **No Silent Elision.** Unknown statements retain identity, provenance and an
   explicit operation/gap. They cannot disappear or become Nop without a proof
   of absence of effects.
3. **Conservative Fallback.** Reuse Assign/Branch/Jump/Invoke/Return for exact facts,
   HavocMust for a proved mandatory write, HavocMay for a possible scoped write,
   and Opaque with declared envelopes for insufficient effects/control facts.
4. **Local Degradation.** Apply effects at their ProgramPoint. A later unknown
   memory effect does not retroactively change an earlier observation. Unknown
   control may revisit an earlier region only when its envelope permits that.
5. **Known Facts Survive Uncertainty.** A possible write retains surviving known
   candidates and opens the remainder; a mandatory overwrite kills the old value.
   Strong later assignments can restore a closed model value.
6. **Completeness != Usefulness.** COMPLETE, PARTIAL and unknown knowledge are
   independent of publication success. Coverage, uncertainties and per-site
   remainders remain separate; no dependency-result wire change is needed.
7. **Whole-program rejection is exceptional.** Reject contradictory/invalid
   structure, unusable frontend output, identity/validation failure or an explicit
   resource limit. An unsupported statement/surface alone is not such a failure.

## Completion contract for future constructions

A construction is complete only with multiplicity, mixed composition, unsupported
neighbors, localized uncertainty and deterministic identity/control tests. This
applies to future EVALUATE, GO TO, I/O, SQL, CICS and PERFORM variants. Test numbers
are examples, never productive limits. No N-sized program gate may implement a
semantic profile. Physical inventory order does not create control edges.

The next product activity after this wave is CARDDEMO BASELINE: run the corpus,
count unsupported constructs and affected programs, measure CALL-site impact,
rank blockers by expected coverage gain, and rerun CardDemo after each vertical.
No precise EVALUATE/GO TO/READ/WRITE, SQL/CICS, general PERFORM, RD, Def-Use or SSA
is implemented by this wave. Git/PR/tests/merge are the record; remote FAST only.

## SP 1.8 boundary

BASIC_PROCEDURE_PERFORM describes target entry, ordered intrinsic MOVE body,
target exit and the resume of this activation. The final body MOVE has NONE
normal continuation; it never owns one global resume. The shared primary graph
is not repeated in each wire PerformFact. Each activation is qualified independently;
multiple targets and repeated targets are accepted. Main/body proof checks use
indices and shared body results, proportional to input and published body facts.

ObservedStatement publishes observed kind/shape, statement identity, containment,
provenance, gap, known nominal references and an explicit normal continuation when
proved by typed canonical frontend structure. Nominal references do not prove
whole-item access or effect scope. A known observed continuation is the unique
normal in-unit successor; abnormal exit/nonreturning behavior is not certified.
Typed CONTINUE/DISPLAY and handler-free READ can publish that continuation. Other
control regions retain an unavailable continuation. The lower does not inspect AST
or parse observedKind. READ values/file semantics remain unimplemented.

Authority: IBM Enterprise COBOL 6.4
[Basic PERFORM](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=statement-basic-perform)
and [READ sequential access](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statement-sequential-access-mode).
The frontend transports only the structural facts qualified here, without runtime
value evaluation. SP1.7 is not reinterpreted; consumers retain its decoder/meaning.

`CompositionalityContractTest` and `PartialProgramFactsTest` run in remote FAST.
The synthetic stress has 20 CALLs, 10 IFs, 5 BASIC PERFORMs and 36 MOVEs.

Independent-storage facts cover qualified elementary WORKING-STORAGE roots even
when another declaration has no scalar model. The section must still be exact;
any overlay suppresses scalar qualification before the proof is formed. No
disjointness is inferred from distinct IDs. `mixed-data` protects this distinction.
