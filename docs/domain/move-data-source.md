# Scalar data sources for MOVE — SP 1.5.0

Contract evolution: 1.5.0 under INTERNAL-CONTRACT-DEV-001, a coordinated minor
capability extension with one current writer. MOVE source is a sealed MoveSource:
LiteralSource or DataReference (READ). JSON discriminates `source.variant` as
LITERAL (existing literal fields) or DATA (`reference` with canonical binding,
role, wholeItemAccess and provenance). Literal meaning and fitting are unchanged.
Closed consumers must migrate explicitly; there is no fallback by statement text.

Authority: [IBM Enterprise COBOL 6.4 MOVE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-move-statement),
verified 2026-09-12, and [elementary scalar profile](scalar-text-move.md), INV-SP-008.
LANGUAGE_GUARANTEED: elementary alphanumeric MOVE copies the sending value and
preserves the sender. SPECIFICATION_GUARANTEED: this wave requires one receiver,
non-CORRESPONDING, equal positive scalarText extents, PICTURE X DISPLAY, unique
binding and whole-item unqualified access at both occurrences. No numeric
conversion, data padding/truncation, group, OCCURS, refmod or alias analysis.
FULL_IDENTITY proves copy semantics; it does not claim a known source value.

ARCHITECTURE_GUARANTEED: canonical occurrences/resolution provide VALUE_READ and
VALUE_WRITE identities. ScalarMoveSemantics indexes these facts before projection;
no nominal lookup occurs downstream. Existing IndependentStorageSet publication
already covers linear programs and retains its complete source-derived authority.
The projector and writer only translate. Finite indexed passes cost O(nodes +
references + declarations + literal text); each copy adds constant index work.

Oracle: literal PROGA → A, A → WS-PGM, CALL WS-PGM publishes fitted `PROGA   `,
DATA source READ(A), FULL_IDENTITY and storage independence. Negative sources
with subscript/refmod/unresolved/ambiguous binding or unequal/numeric domains
cannot prove FULL_IDENTITY. W1/W2 facts, provenance and partial coverage persist.
