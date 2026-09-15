# RF-W4 input ownership

UnitInputProof records only missing COPY occurrences whose mapped SourceMap gap
segments are wholly enclosed by one grammar-owned, explicitly ended top-level
program. An occurrence mapped across boundaries or outside any proved owner stays
global. Diagnostics with lexer/parser/preprocessor/I/O errors remain global.
Nested gaps affect their containing top-level unit and descendants. Unit identity
and containment use typed structural paths, never nominal names or source snippets.

ResolutionAnalysisReport.inputComplete(unit) is the shared per-unit predicate for
layout, MOVE/CALL and control adapters. The global report still reports every input
gap. A selected independent SP may be complete while the compilation remains partial.
EntryInputProof additionally accepts gaps in separate ended units; a local DATA gap
retains the previous narrow entry proof without certifying data/signature completeness.
A PROCEDURE gap never promotes the first visible statement to a proven entry.

ScopedInputTest covers DATA/PROCEDURE gaps in another unit, unowned and nested gaps,
and observed literal CALLs before/after a missing PROCEDURE COPY. The latter remain
observed facts with unknown entry/reachability. W4 qualification and further layout/
metamorphic evidence remain in progress. No missing COPY content is invented.

The CLI additionally writes `observed-dependencies.json` (schema
`cobol-observed-dependencies` 1.0.0) for all parsed units. It preserves typed CALL
literal values, unit/site identity, source provenance and all input gaps. Every site
is OBSERVED_ONLY with UNKNOWN reachability and an open remainder. Computed names
are not evaluated here. There is no edges array and no claim of runtime callee
resolution. This separate inventory survives a blocked lowering/entry admission;
consumers must keep it distinct from `dependencies.json` reachable edges.


A subordinate unresolved REDEFINES has the record bound established by its canonical
physical parent chain. It makes that component's extent and views unknown; allocation
of other independent local WORKING-STORAGE roots survives. An unresolved root relation
has no such bound and retains the global allocation blocker. Producer and lower reader
check this distinction with the existing parent/base/relation fields. No endpoint or
alias disjunction is inferred from an unresolved binding. RENAMES creates no allocation;
an unproved RENAMES view keeps its record base and unknown offset/extent. An access
through an unresolved view still cannot prove exact writes or exposures.

Semantic basis: IBM Enterprise COBOL 6.4 [REDEFINES clause](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-redefines-clause)
(same hierarchy level and enclosing record) and [RENAMES clause](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=entry-renames-clause)
(endpoints in the associated level-01 record; no allocation). This is a refinement of
proof admission using existing SP fields, not a new wire shape. No layout is invented
for missing DATA input or an unbounded root relationship.


## Ambiguous CALL reference — SP 2.18

`DataReference.regionalAlternatives` is a non-exhaustive list of canonical physical
whole-text accesses for a structured, unsubscripted, unmodified ambiguous CALL target.
It never selects a nominal binding. Empty means no materialized alternative; it never
proves that no value exists. Each view must agree with a distinct nominal candidate,
known bounds and a supported text codec. Other forms retain their existing unknown
state. StorageAccessSemantics materializes these facts; the projector only transports
them. The storage schema remains 1.5.0; SP becomes 2.18.0 for the new reference field.

The downstream target is an open Place.Choice. AIR target.possibilities@1 is required
because the remaining memory need not have a known TEXT domain. The extension preserves
TEXT candidates without claiming anything about the unknown remainder. Consumer queries
remain BEFORE Invoke over current regional values, never a union with declared VALUE.
