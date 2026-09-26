# Locality of logical storage proofs

Status: IN_PROGRESS. Scope: preserve logical values of an elementary item when an
unrelated sibling is repeated or redefined. No physical layout is inferred.

Cause: the R2 producer requires the entire allocation region to have no OCCURS or
REDEFINES before publishing any LOCAL_CELL. That loses a proved whole-item value
identity even when the unsupported sibling cannot overlap it.

Authority: IBM Enterprise COBOL 6.4 [OCCURS](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=tables-defining-table-occurs), [REDEFINES](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-redefines-clause) and [variable location](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=clause-occurs-depending); AIR2.0
03 §3/3.1 separates logical cells from physical layout. The source component index
already identifies the exact sibling overlay component. Allocation and complete
region input remain prerequisites. A logical cell additionally requires every
ancestor (including itself) to have one component member and no OCCURS. Unproved
overlays, RENAMES and variable OCCURS continue to refuse the region. Existing exact whole-region
aliases retain their single representative.

The alias inventory/closure proof can therefore be scoped to a declaration inside
its allocation region. The closure depends on the full region closure and the
matching declaration inventory. A consumer must retain that subject correspondence;
a proof for sibling A cannot qualify B. Historical region proofs remain valid.

The ancestor walk is memoized over the declaration forest, O(N+R) preparation;
existing descendant binding output can be O(N*depth). No source-order, target-name,
value, corpus identity or missing-input default participates. Counterexamples:
item inside OCCURS, target/ancestor REDEFINES, unresolved overlay, missing COPY,
RENAMES and unavailable allocation must remain unqualified. A sibling outside
those subtrees must keep the same logical value domain and independent cell.

## Whole-item value copies

The canonical scalar MOVE capability now consumes available LOCAL_CELL and logical
DISPLAY shape facts. It requires independently qualified source and destination,
unique resolved operands, complete whole-item syntax, equal logical extents and
source provenance. An unavailable unit-wide COPY no longer overrides those positive
local facts. Open regions, missing declaration headers, unlocated input, subscripted
operands, reference modification, aliases without identity and unequal extents remain
outside FULL_IDENTITY. This applies the same R2 proof authority already used to bind
literal writes and CICS reads. A nominal name alone does not authorize a cell or copy.
The existing resolved occurrence contract remains authoritative for selecting DATA;
this change does not select a candidate from ambiguous or unresolved references.

The consumer must choose the admitted logical copy before a missing byte-layout
transfer. A byte-profile gap does not refute a logical whole-item value identity.
The boundary regression uses a closed mixed record, a data-to-data MOVE, and a
missing COPY in a separate open record. Its emitted SP is the lower's test input.


## CALL source syntax and target access

A CALL surface is qualified by the existing EntryInputProof for a complete
procedure inventory plus exact statement provenance. A missing COPY proved to
belong to DATA does not erase USING/RETURNING/handler syntax or the ordinary
continuation already published by the parser. Target storage separately requires
its positive scalar proof, unique binding and exact whole-item reference. No
missing declaration is inferred. Missing procedure content, parser errors and
unlocated input still block the source-surface proof. Foreign effects stay open.
Synthetic tests separate missing DATA after a closed target from missing code.
