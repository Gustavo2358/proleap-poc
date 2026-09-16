# EP-W3 — scoped storage uncertainty

## Decision before implementation

Replace the Unit-wide allocation boolean with explicit uncertainty records. Each
record identifies the source owner/root, provenance, affected dimensions and the
scope for which proof is missing. A root allocation assessment is derived from
those records. Scope is not inferred from names, IDs, order or a keyword list.

An unmodeled data clause is not harmless. Its layout uncertainty starts at its
declaration and propagates through the existing offset/extent/component equations.
Without an alias bound, its allocation uncertainty can cover the unit. That broad
remainder is retained explicitly with its owner/provenance; it cannot erase VALUE
support. We do not claim that every unknown data clause affects only its own root.
Nonlocal storage visibility likewise leaves unit separation unproved.

Known nested REDEFINES and RENAMES scope rules already distinguish a record from
an unbounded root relation. Preserve those positive bounds and counterexamples.
The new assessment keeps layout measures separate: missing allocation proof does
not change a known offset/extent into a fabricated zero or a proved alias relation.
The current SP base allocation enum remains a projection of the assessment. No
normative/wire change is required: it still promises independence only when proved.

## Algorithm and oracles

Collect immutable owner/scope/reason/dimension records while visiting declarations;
index unit-wide and root-scoped uncertainty once. Evaluate a base from its indexed
scope records; no pairwise alias graph, language whitelist or extra dataflow is
introduced. O(declarations + uncertainty records + bases), excluding existing
component/layout work. Logical source evidence remains a separate W1 obligation.

Generic AST JOHNDOE, unknown numeric layout, unknown subordinate/root relations,
and uncertainty moved between declarations/units must preserve independent VALUE
support. Tests inspect both the scope/provenance record and known physical bounds.
Consumer controls remove separation premises and rename/reorder bases: neither
change may turn unproved aliases into disjoint storage or give them kill authority.

Limits: broad alias uncertainty remains broad when no smaller bound is known.
This wave does not implement padding/alignment semantics or claim source-complete
layout. A smaller scope requires positive evidence, not a desired candidate count.
