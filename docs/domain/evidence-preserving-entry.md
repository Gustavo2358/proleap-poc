# EP-W1 — source evidence and precision proof

Current writer: SP **2.19.0**, storage **1.6.0**. This changes the admission
meaning of `POSSIBLE_LITERAL_BYTES`; older consumers must not silently accept
the new document as storage 1.5.0.

## Source authority

`DeclarativeValueEvidence` recognizes one modeled textual VALUE and its logical
width (modeled elementary text PIC, or a previously supported textual group).
The selected IBM1047 profile encodes that value, including declared padding.
This supports a logical value of the identified declaration. It does not prove
allocation, address, alias separation, lifetime, or a simultaneous physical
memory image. An unrelated preserved clause has no authority to erase support.
Literal validity, encoding and whole-item multiplicity remain source obligations:
a repeated element's literal does not describe its whole aggregate. No new array
semantics is introduced.

`POSSIBLE_LITERAL_BYTES` may have an unknown view offset, unknown extent, unknown
containing base, or unknown allocation. Its bytes encode the supported logical
candidate; consumers must not infer a physical extent from their count.
Provenance, selected profile, nonempty evidence and `DECLARATIVE_POSSIBILITY`
are required. `ENTRY_STATE_NOT_PROVEN` remains required; layout and mutation gaps
remain available and cannot be cleared to claim exactness.

## Precision authority

SP 2.19 also requires the nullable `logicalWholeItem` field on data references.
It identifies the complete logical declaration accessed by this occurrence,
independently of a physical view or the older `scalarText` storage profile.
The producer requires exact source shape, no subscripts/reference modification,
and a resolved binding to that declaration. It currently emits the fact for
CALL targets. Consumers cannot derive it from a name or nominal binding alone.
It proves neither the value, type, allocation nor alias separation of that item.

`StorageInitialSemantics` separately checks a bounded textual view and lifecycle
or invariant proof. Explicit INITIAL, PROGRAM INITIAL and a closed mutation
inventory can upgrade supported evidence to `LITERAL_BYTES` under their existing
obligations. DVI failure produces a possibility with remainder. PRESERVED remains
a distinct explicit entry profile and never claims an initial literal.

Lowering must use a bounded physical place only when supported. Otherwise it
must retain the typed logical declaration with an unknown binding and publish
an AIR `entry.possibilities@2` ObjectPlace. It must not invent a Cell, derive
disjointness from identity, or strengthen possible facts to exact entry values.

## Algorithm and limits

Recognition visits declarations and clauses once plus encoded value size.
Existing layout and mutation analyses provide precision independently; there is
no new dataflow. Entry evidence is seeded at invocation boundaries only.
Numeric representations, unsupported encodings, invalid literals, nested VALUE
conflicts and redefinition initial rules retain explicit limits. This wave does
not claim to implement every COBOL declaration or padding/alignment rule.

## Qualification

W0 source RED: four semantic failures among seven tests. W1 source tests cover
unknown clauses (including synthetic JOHNDOE), unknown offset/base, statement
effects, source-invalid controls, and a later MUST assignment. These prove source
transport, not downstream survival. W1 remains open until lower/analysis
boundaries and selected E2Es are qualified. Confidential real case: NOT AVAILABLE.
