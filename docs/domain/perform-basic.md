# CP6 PERFORM BASIC — SP 1.6.0

Profile `SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM` publishes an isolated local
paragraph activation as a typed PerformFact. This is not control.local@1.

Authority: IBM Enterprise COBOL for z/OS 6.4 Language Reference, Basic PERFORM,
printed pp. 413–414, [official manual](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf),
read 2026-09-12. Basic paragraph PERFORM executes once, returns after its final
statement, and resumes immediately after the callsite. Ordinary paragraph entry
has different completion. GOBACK terminates the primary program invocation.

The unchanged AST provides ProcedureEntry, exact paragraph/sentence membership,
canonical direct statement order, MOVE/CALL normalContinuations and typed PERFORM
controls. The existing PERFORM_FROM resolution selects a local PARAGRAPH symbol;
no names are resolved again. The grammar requires an expression for each supported
TIMES/UNTIL/VARYING control (WITH TEST belongs to UNTIL/VARYING); an empty typed
control inventory is sufficient only with complete input and modeled coverage.
No decision parses writtenControl or paragraph spelling.

PerformSemantics runs after canonical binding. Admission requires exactly two
nondeclarative direct paragraphs and a complete primary path: MOVE prefix (possibly
empty), one PERFORM, one CALL, final GOBACK. The primary paragraph is selected by
explicit entry membership. Its distinct target has only a nonempty linear sequence
of admitted scalar literal/data MOVEs. Every direct statement is accounted for.
The primary GOBACK excludes ordinary fallthrough, and the closed inventory excludes
GO TO, alternate ENTRY, other PERFORMs and hidden control. All required provenance
must be exact. Section targets, extra paragraphs and more general primary shapes
remain outside this deliberately conservative first vertical.

The source proof uses typed canonical paragraph/sentence containment to publish the
PERFORM completion missing from the old MOVE/IF/CALL continuation index. The target's
last MOVE gains a completion only under this isolated activation proof. It is not a
generic paragraph-end return. Other normal continuations keep their existing authority.

`PerformFact` carries profile, `PerformTarget(ProcedureId, referenceOrigin,
paragraphOrigin)`, targetEntry, ordered targetStatements, targetExit,
normalContinuation (including resume provenance), primaryStatements and gapCodes.
ProcedureId uses the canonical PROCEDURE symbol local ID in its unit namespace;
names are metadata, never executable identities. Body and primary membership cover
the entire statement inventory and are disjoint. The lower must validate these
relations, use explicit entry, and ignore physical SP/AIR array order.

Proof: with one activation and one static resume, every state inside the linear
body has the same pending continuation. Erasing that constant preserves execution
through Jump(target), existing MOVE transfers and Jump(resume). Two callsites require
different returns; ordinary/GO TO entry lacks this return; THRU changes the exit;
loops change traversal count; nested control changes completion. All are refused.
The finite indexed passes use O(nodes + references + declarations + body members)
time/space, with no path enumeration, backward scan, RD or generic stack.

IndependentStorageSet remains the existing source-derived proof. PERFORM adds no
storage premises and emits no external program dependency. The contract remains
PARTIAL in its other source/effects/name-policy dimensions.

Validation: PerformDiscoveryTest verifies the old canonical authorities;
PerformBasicTest covers real literal/copy/overwrite, identity, membership, resume,
provenance, deterministic JSON, alpha-renaming and negative forms/entries. Remote
FAST includes both. EXIT PARAGRAPH is already rejected by the current parser; no
grammar change is included. Full remains local only.
