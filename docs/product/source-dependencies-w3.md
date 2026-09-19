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
Other SQL statements stay opaque: no DB2 table extraction.

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
NOT USED; corporate execution NOT AN ACCEPTANCE GATE. No W4 implementation.
