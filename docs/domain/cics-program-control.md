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

The generic AST records an embedded positional continuation independently of
normal completion. SP `localContinuation` is this boundary; the lower assigns
its meaning from the command and condition profile. The current CLI option
`--cics-entry-mode unknown|new-logical-level|disabled` defaults to unknown.
`new-logical-level` is an explicit environment premise that CICS handlers start
at defaults; it is not inferred from a source file. Only complete input with a
canonical entry prefix consisting of MOVE statements can prove DEFAULT_ENTRY_PREFIX.
RESP2 alone, prior calls/opaque commands, ENTRY/declaratives and input gaps exclude
this proof. Local error tests use separate RESP/NOHANDLE fixtures.

Bounded RESP/NOHANDLE commands can participate in the existing PERFORM paragraph
range profile. A complete parsed embedded transform proves its structural span
without changing `provenance.exact=false`. Error continuation may reach the
activation resume; XCTL success never receives a normal return edge.

CICS target references carry the READ role for a data-area. The elementary-only
CALL target restriction therefore remains exclusive to COBOL CALL. The readiness
consumer exposes a separate CICS_PROGRAM_CONTROL audit family. Host operand AST
nodes have their own identities/spans; parse-tree navigation points to the real
EXEC container because the host grammar runs in a separate syntax tree.

W4: full Maven selected 741 tests (one pre-existing opt-in oracle skipped); the
new-family projector guard failure was corrected with unique-position lookup
and focused regression. CICS CLI/readiness, group storage and source navigation
regressions have reduced tests. Full normalizer artifact checks passed after the
source navigation correction, reusing unaffected outputs and the full test run.
The naming check still rejects the pre-existing product name in
`docs/engineering/storage-w8-qualification.md`; that historical file is unchanged.
See the campaign handoff for the exact pins and final composed evidence.
