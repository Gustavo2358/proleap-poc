# CICS Program Control

CICS TS for z/OS; LINK/XCTL only. IBM authority: [XCTL](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-xctl),
[condition handling](https://www.ibm.com/docs/SSGMCP_5.6.0/applications/designing/dfhp3_exc_pushpophandle.html),
[8-byte area](https://www.ibm.com/support/pages/node/89057).
PROGRAM identifies the target; variables require an actual 8-byte name area.
LINK returns to the following instruction when the linked program returns;
XCTL success releases the issuer. RESP/NOHANDLE can continue on command errors;
RESP2 alone proves no such policy. Unknown handlers keep a local remainder.

The dedicated parser consumes the preprocessor representation: one flattened
`*>EXECCICS` token per command. The COBOL rule must preserve each token as one
EmbeddedLanguageStatement, including consecutive commands without periods.
Parsing uses a cursor with quote/parenthesis state, not semantic regex. Every
option retains original text and payload-relative offsets anchored to the AST
SourceMap provenance; transformed provenance is never promoted to exact physical
columns. Duplicate PROGRAM, malformed/truncated operands keep diagnostics.
Options do not establish effect purity or a complete signature.

Canonical contribution is an immutable snapshot bound to the frontend. The
projector translates facts, never parses embedded text or calculates runtime
values. The extension-disabled composition preserves opaque publication.
The parser terminates in O(payload characters); each cursor step advances.
The source traversal is linear and uses existing AST ownership.

SP follows INTERNAL-CONTRACT-DEV-001: one explicit current version and coordinated
lower admission, with reader rejection of unsupported variants. CICS keeps its
own command and target profile; it is never published as COBOL CALL.

The host-syntax bridge uses the existing COBOL identifier grammar and AstBuilder,
then ReferenceOccurrenceCollector and the canonical resolver. No lookup by raw
PROGRAM text occurs. SourceMap anchors nested operand provenance, including COPY.
StorageAccessSemantics admits existing group/alias/qualified/constant slice reads.
SP options retain bound references when available; malformed, short, dynamic or
unproved accesses remain explicit. The lower requires an IBM1047 physical view of
exactly eight bytes before it emits a computed name read.
