# Primary entry and localized input completeness — SP 1.9.0

Missing source dependencies degrade only the semantic dimensions and regions
they can affect. They must not invalidate an independently proven executable entry.

IBM Enterprise COBOL 6.4 [Procedures, pp. 265–266; ENTRY, pp. 338–339](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf)
defines ordinary execution in the nondeclarative procedure body and ENTRY's
destination as the following executable statement. AstBuilder traverses the
typed body/section/paragraph/sentence relations, passing leading ENTRY declarations
until the first other direct statement. It stops even if that statement cannot be
materialized. Consecutive leading ENTRYs compose; later ENTRYs cannot replace the
primary start. Empty bodies, altered GO TO without a node, and declaratives remain
unqualified. Alternate entry interfaces/runtime behavior remain unmodeled.

COPY is textual inclusion, not a runtime no-op (IBM, COPY, pp. 688–694). Missing
COPY placeholders retain a typed diagnostic anchor through the preprocessing
source map, including nested includes. The AST qualifies a data input gap only
when all occurrences of that anchor are within the selected unit's typed DATA
DIVISION, after its complete header and before its explicit PROCEDURE header.
This is a proof about the independently parsed procedure of that unit and valid
data-region completions; it does not guess missing text or certify arbitrary
textual substitutions which change program/division ownership. Unlocated gaps,
other input diagnostics and parser/lexer/preprocessor errors fail closed.

The small canonical entry proof records the qualified diagnostic identities.
Projection checks that this proof covers every input gap, then translates the
canonical statement identity. It performs no COBOL interpretation. SP 1.9 reuses
the typed entry fields: KNOWN entry/start may coexist with INPUT_MISSING inventory,
coverage and signature. INPUT gaps and unknown declarations remain present.
No binding, value, type or independent-storage proof follows from localization.
SP 1.8 and earlier retain their original consumer decoding semantics.

Missing PROCEDURE DIVISION COPY requires conservative executable-region
representation. Until that exists, any such gap blocks entry admission even
when the first visible executable precedes it. A placeholder must never authorize
MOVE → CALL across unknown code. Missing inputs in other units or outside DATA
are also conservatively unqualified in this wave.

Traversal terminates over finite typed children and source-map segments; it is
linear in statements plus mapped input regions per unit. Diagnostic-set matching
is linear in diagnostics. Oracles: leading ENTRY → MOVE/PERFORM/other statement;
later ENTRY preserves the earlier start; no following executable has no start;
one/many/nested DATA gaps retain start and input/signature/storage partiality;
unknown target has no binding; PROCEDURE gap stays blocked. These are permanent
FAST tests in `LocalizedInputCompletenessContractTest`, alongside existing
declaratives, unlocated-input and compositionality regressions.
