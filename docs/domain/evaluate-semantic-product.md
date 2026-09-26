# EVALUATE first slice — SP 2.0

Current compositional control and SP2.37 ordinary continuations: [W7 contract](control-composition.md). Historical profile qualification below does not gate independent branch entries or predicate coverage.

Rule: [IBM Enterprise COBOL 6.4 EVALUATE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-evaluate-statement).
The first matching WHEN executes; normal completion leaves EVALUATE. OTHER is
the default; without OTHER, no match continues after EVALUATE. Roadmap authority
is HUMAN, independent of CardDemo frequency.

Canonical grammar/AST relations prove a single simple DATA subject and one basic
text literal per WHEN. Ordered arm identities, direct members, entries, OTHER
presence and normal continuation cross SP explicitly. Binding failure is PARTIAL
and never removes an arm. Body precision is independent of structural selection.
Unsupported variants remain observed with open control; empty arms have no invented
entry. No next statement is inferred downstream from JSON or ProgramPoint order.

The algorithm visits AST nodes, references and direct arm members finitely, using
indexed binding and continuation lookups. Time and storage are linear in those
inputs. SP 2.0 changes the sealed statement family and containment vocabulary;
historical wire decoders keep their original meaning.

AIR Branch with Unknown BOOL preserves all possible choices, as current IF does.
No path pruning or comparison/conversion engine is added. Numeric selections,
ALSO, TRUE, ANY, THRU, NOT, arithmetic/refmod/subscripts and general nested
EVALUATE stay outside this slice. Oracles: three-way join, incoming value on
no-match, strong updates, per-arm CALL sites, nested IF and 1/2/5/40 occurrences.

## Positive topology W2 — SP 2.33.0 extension

When the grammar supplies an ordered WHEN arm but its condition is outside the
evaluator, the producer retains that arm, its body, source origin, known read
operands and ordinary continuation. Such an arm has no `selection`; it instead
publishes `conditionReads` and `conditionOrigin`. Literal arms keep their SP 2.0
shape. SP 2.33.0 is emitted only when this extension is used. The lower uses the
existing bool Unknown and ordered Branch chain, including the no-OTHER path.
Uninterpreted condition details remain coverage; they add no global control or
memory effects. This does not implement the excluded COBOL condition families.
