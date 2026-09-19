# W3 — source dependencies

Status: IN_PROGRESS, stacked on W2 PR #55 at `84845762c58ba0f64199bf01db9afce4c97a939b`.

## Semantics and authority

A COPYBOOK dependency is the nominal member and optional library explicitly named
by a proved preprocessor `copyStatement/copySource`. Capture precedes expansion,
including an empty or unavailable member. REPLACING operands are not dependency
names. COPY names and library qualifiers are canonicalized with Locale.ROOT upper
case. Comments and string literals do not establish COPY occurrences.

A DCLGEN dependency is a proved `EXEC SQL INCLUDE member END-EXEC` occurrence plus
positive `DCLGEN` classification in configured artifact inventory. SQLCA and SQLDA
are builtin SQL includes, never DCLGENs. Generic/unclassified includes remain
`SQL_INCLUDE`; unknown classification opens remainder. SQL INCLUDE payloads other
than the supported single unquoted member form are not guessed. An INCLUDE-shaped
but unproved form opens `SQL_INCLUDE_FORM_UNPROVED`, without inventing a name.
Runtime SQL remains opaque. Static table dependencies are extracted separately as described below.

Primary references checked for this change:
[IBM COBOL COPY](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=statements-copy-statement),
[Db2 INCLUDE](https://www.ibm.com/docs/en/db2-for-zos/12.0.0?topic=statements-include),
[DCLGEN inclusion](https://www.ibm.com/docs/en/db2-for-zos/13.0.0?topic=definitions-including-declarations-from-dclgen-in-your-program).
COPY is source library insertion; INCLUDE may insert generated declarations but
also SQLCA/SQLDA or other application code. Syntax alone cannot prove DCLGEN.

## Configured authority

Optional CLI argument `--source-inventory inventory.json`:

```json
{"version":"1.0.0","artifacts":[
  {"name":"DCLCLI","kind":"DCLGEN","artifact":"copybooks/DCLCLI.cpy"},
  {"name":"GENERIC","kind":"SQL_INCLUDE","artifact":"copybooks/GENERIC.cpy"}
]}
```

The inventory is an explicit assertion by the input provider, not inferred from
path spelling or content. Names are unique after normalization. Unknown fields,
versions/kinds, duplicate names, absolute/parent-traversing paths, or attempts to
classify SQLCA/SQLDA as DCLGEN are rejected. Artifact paths are relative to the
inventory directory. Existence controls resolution independently of classification:
a missing configured DCLGEN is still nominal DCLGEN with UNRESOLVED artifact.
Without inventory, no user SQL INCLUDE becomes DCLGEN. DCLGEN contents are not
expanded or interpreted by this feature.

## Producer and wire

`PreprocessorEngine.Outcome` carries immutable `SourceDependencyFact` occurrences
and unproved-form gaps. Each occurrence has kind/name/qualification, resolution,
artifact, authority, original provenance, include chain and root inclusion site.
`SourceDependencySemantics` associates root sites with structurally enclosing
program source intervals, retaining the deepest enclosing owner. An unprovable
program association is explicitly rejected instead of assigning facts to another
program or discarding a nominal dependency.

SP `2.30.0` adds required `sourceDependencies`:

* `availability`: KNOWN or PARTIAL for the CLI producer;
* `occurrences`: unique occurrence id, COPYBOOK/DCLGEN/SQL_INCLUDE, canonical name
  and qualification, RESOLVED/UNRESOLVED/CYCLIC/IO_ERROR, artifact, authority and
  the existing typed provenance;
* `gapCodes`: explicit resolution/classification/shape remainder.

The enclosing SP version also supports the existing storage 1.8/1.9 contracts;
logical W2 semantics and default are unchanged. Legacy in-memory SP constructors
retain UNAVAILABLE source inventory and their old wire version. Old closed readers
reject 2.30.0 explicitly. The compilation envelope remains 1.0.0 and embeds each
versioned program document.

Nested COPY is program-centric with original source ownership: PROGRAM -> A is
DIRECT; A -> B is represented by B's occurrence in A, with include chain A and
TRANSITIVE program relationship downstream. Repeated expansions retain distinct
occurrence identities and supports; nominal aggregation happens downstream.
The expanded field names a local normalized snapshot `preprocessing:<source>`,
not a position in the final expanded compilation where the directive no longer
exists. Snapshots from different files have distinct artifact identities.
Original line/span is captured before replacing the COPY directive, not recovered
from expanded content. Provenance exactness has its existing SourceMap meaning.

## Limits and complexity

Existing COPY dialect, source-format, expansion and resolver rules remain in
force. OF/IN qualification is preserved, but the existing flat library resolver
cannot prove qualified resolution; these occurrences remain UNRESOLVED even if
legacy expansion found an unqualified member. SQL INCLUDE resolution uses only
explicit inventory and does not load its content. No dependency is inferred from
program names, runtime values or copybook business logic.

Cyclic COPY nominal facts survive preprocessing/SP, but existing runtime-entry
admission can block lower/AIR publication after a preprocessing diagnostic. This
is an explicit E2E limit, not a successful cycle qualification. Source dependencies
in programs whose original source interval cannot establish ownership reject.

Lookup uses indexed library/inventory and program ranges; occurrence traversal is
linear plus O(log U) program lookup and canonical O(N log N) output ordering.
Existing preprocessor IO/expansion cost is proportional to expanded source.
No pairwise artifact comparisons, new CFG/dataflow solver or physical propagation.

## Evidence

`SourceDependencyProducerTest` covers missing/repeated/replacing/empty/nested,
qualification, casing, comments/literals, explicit inventory, SQLCA rejection,
malformed/unproved forms and generic/unknown SQL includes.
`src/test/resources/cobol/source-dependencies-w3/` contains synthetic source E2E
fixtures; expected names/support counts are independent oracles. Corporate source
NOT USED; corporate execution NOT AN ACCEPTANCE GATE. DB2 continuation stays within this W3 campaign.

## DB2 TABLE continuation

The previous COPYBOOK/DCLGEN/SQL_INCLUDE qualification remains a completed checkpoint.
DB2 TABLE qualification is now required before the campaign returns to READY_FOR_REVIEW.
Same branch and PR; no new administrative W4, no AIR/runtime/physical engine changes.

## DB2 TABLE source dependencies

Static SQL is extracted before embedded-language framing, from the normalized original EXEC SQL region and its existing SourceMap span. A lightweight tokenizer neutralizes SQL strings, host variables, `--` and `/* */` comments; paired parentheses are indexed once. A deterministic structural scanner recognizes table positions and scoped CTEs without constructing SQL grammar/AST/IR or evaluating expressions. Each region has independent state. Unsupported/malformed supported structure rejects all tentative table facts for that region and opens a gap.

Supported: SELECT FROM/JOIN (including multiple and comma joins), schema qualification, INSERT target and INSERT SELECT, UPDATE/DELETE targets and nested SELECT, MERGE target and nominal/derived USING, CTE definitions, derived SELECT, UNION/EXCEPT/INTERSECT branches, and simple DECLARE name CURSOR FOR SELECT. Aliases are consumed at relation boundaries. Local CTE names are indexed before traversing definitions; recursive/forward CTE references conservatively open DB2_RECURSIVE_CTE_UNSUPPORTED. SQL expression validity, column binding and catalog object kinds are not certified: DB2_TABLE denotes a syntactic relation reference, which a catalog could resolve to a table/view/alias. No catalog resolution is attempted.

Identity: uppercase ordinary identifier name plus explicit qualification; CLIENTE and DBPROD.CLIENTE remain distinct. Delimited identifiers are tokenized but conservatively rejected with DB2_DELIMITED_IDENTIFIER_UNSUPPORTED because the current source identity contract folds case. Table functions, VALUES-derived relations, DDL, stored procedures, unfamiliar relation constructs, and unsupported cursor options open explicit gaps. Nesting beyond 128 levels opens a gap. No inference from DCLGEN or INCLUDE names/content.

SP 2.31.0 adds DB2_TABLE and typed operation/access per occurrence. Non-DB2 occurrences use NONE/NONE. SELECT uses READ; INSERT/UPDATE/DELETE use WRITE; MERGE target uses MERGE/READ_WRITE and nominal USING uses MERGE/READ (derived SELECT uses SELECT/READ). Authority STATIC_SQL_TABLE_POSITION is required. Resolution NOT_APPLICABLE is exclusive to DB2_TABLE: catalog lookup is outside this product and no physical artifact identity is fabricated. This nominal completeness is separate from source artifact resolution.

PREPARE and EXECUTE, including EXECUTE IMMEDIATE literals, emit DYNAMIC_SQL_NOT_ANALYZED with remainder=true and no invented table. PossibleValues is never invoked. Other unsupported SQL shapes remain open. Existing source gap transport is conservative at compilation scope; no statement-level SQL gap provenance type is introduced in this continuation.

Each support retains original EXEC SQL span, program association, sourceOwner and include chain. SQL in A.cpy is TRANSITIVE to the program and points into A.cpy. Repeated SELECT/UPDATE of the same qualified table aggregate into one dependency with separate usage-bearing supports. Source aggregation uses maps and canonical sorting, no CFG/reachability/RD/values/physical inputs. Runtime SQL stays opaque in its existing path.

AIR stays unchanged at 646ca3ab1687d43f7d2063fc2a8f3837ab3cf9fa. Existing LiteralTarget category source-db2_table and ResourceDeclaration classification source.NOT_APPLICABLE carry nominal references; nameSource source.STATIC_SQL_<operation>_<access>@1 carries a closed usage profile. No runtime uses, objects or operations are added. The dependency wire is 2.5.0, with operation/access on every source support; 2.4.0 readers reject it. The new reader retains explicit support for older wires; lower upgrades legacy SP2.30 NONE usage only after rejecting DB2/new fields in that old envelope.

Scope remains source-only, physical default OFF with NO AUTOMATIC FALLBACK. Existing cyclic COPY primary-entry admission limitation remains unchanged. Corporate NOT EXECUTED / NOT AN ACCEPTANCE GATE / NO CORPORATE SOURCE USED.

Primary language references: [IBM CTE](https://www.ibm.com/docs/en/db2-for-zos/12.0.0?topic=statement-common-table-expression), [identifiers](https://www.ibm.com/docs/en/db2/12.1.x?topic=elements-identifiers), [tokens/comments](https://www.ibm.com/docs/en/db2-as-a-service?topic=elements-tokens). Scope is deliberately smaller than the SQL language.

Producer qualification: focused extractor/provenance tests and full FAST PASS (382 tests).
31 source DB2 fixtures traversed the real pipeline successfully; final repeated integrated qualification is recorded in the stacked PR.
Extraction median at 10/100/1000 statements: 0.133/0.364/1.933 ms; 1000 occurrences/100 unique: 1.923 ms.
