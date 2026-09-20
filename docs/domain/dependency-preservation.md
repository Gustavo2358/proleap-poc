# Dependency preservation under incomplete physical evidence

Status: historical dependency-preservation slice, refined by W3-R1 exact logical views.
The [harness principle](../engineering/lean-harness.md#dependency-preservation-principle)
is normative. Evidence hierarchy: exact physical/dataflow proof; semantic nominal/value
support; source-supported possibilities; no usable evidence. Lower levels keep supported
candidates with remainder. They cannot invent storage, aliases, control edges, or values.

The original change reused AIR 2.0.0 ObjectPlace/Read, logical Assign, UnknownBinding and
`target.possibilities@1`; no AIR schema or solver change. Possibilities are attached to
specific definitions and queried BEFORE the dependency site through existing CFG/RD
and values services. A failed physical name-area check opens interpretation remainder.
Runtime paths and missing source keep their existing uncertainty.

## Contract and limits

SP 2.32.0 adds `POSSIBLE_TEXT` for a single literal MOVE to a uniquely resolved whole
elementary DISPLAY PIC X receiver. Exact local syntax establishes receiver text length
and right padding, independently of allocation proof. It excludes truncation, tables,
subscripts, ref-mod, groups, unsupported clauses and representation conversions.
It publishes `logicalWholeItem` and `textAdjustment`, never a fabricated scalar or
physical view. Typed CICS PROGRAM/FILE hosts may carry canonical whole-item evidence
although flattened EXEC provenance is inexact. Physical provenance remains inexact.
Older SP versions cannot silently acquire the new copy semantics.

Strong regional transfers retain precedence. The lower validates the local contract,
preserves value definitions. W3-R1 replaces the old materialization fallback:
supported logical storage uses Cell, true location uncertainty uses a bounded
UnknownBinding, and absent physical representation stays in coverage. No independent
Cell is created for each textual name when a positive exact-view relation exists.
CICS PROGRAM and FILE consumers query readable nominal targets even without physical
8-byte IBM1047 proof. Name spelling/length validation still applies; raw value evidence
and interpretation remainder remain independent. SYSID keeps its separate context policy.

Nonliteral copies across unproved representations, generalized missing-source control,
interprocedural propagation and SQL/IMS typed calls remain outside this bounded change.
No claim of globally complete dependencies is made.

## Semantic authority and algorithm

[IBM elementary MOVE](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=items-assigning-values-elementary-data-move)
provides left alignment/right space padding for the admitted alphanumeric case.
[IBM CICS XCTL](https://www.ibm.com/docs/en/cics-ts/5.5.0?topic=summary-xctl)
requires the computed program data area to be 8 bytes; lack of that physical proof
therefore prevents closure, but does not delete supported logical name possibilities.
AIR pinned specification 14 governs target possibilities and unknown domains.

Producer declaration and resolution indexes are built once; local transfer recognition
is linear in AST/symbols/bindings plus emitted text. Lower type evidence is indexed in
one pass over statements and entry conditions. Existing solver termination/complexity
is unchanged; there is no new dataflow, path enumeration, or global MOVE collection.

## Regression

`DependencyPreservationTest` verifies missing unrelated COPY, local literal/padding,
canonical CICS whole reference, and exclusion of repeated/sliced/JUSTIFIED receivers.
`ScalarMoveCheckpoint4ATest` distinguishes nominal evidence from scalar allocation.
`StorageProductTest` supplies the same physical access facts to scalar analysis and
projection as the production pipeline; strong regional contracts retain their version.
These tests are included in FAST.

## Unknown layout and REDEFINES

The original fixture lacked a logical identity fact and could not justify either
separate Cells or a shared Cell. W3-R1 publishes a local, complete TEXT view proof
for the two elementary X(8) declarations. The lower binds two distinct ObjectIds
to one logical Cell; two mandatory sequential MOVEs therefore leave only PROGB.
The missing COPY remains coverage and does not certify physical extent, codec or
allocation. Partial overlap does not receive this complete-view proof and retains
the supported Region/view treatment when physical representation is known.
