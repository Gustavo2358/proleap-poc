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
