# R7-R7B — terminal SEND typed contract (SP2.44)

Status: BLOCKED for real executable qualification; typed contract ready for review.
Scope: SEND source operands and independent lower-owned transport. No state algorithm, AIR schema, CFG, dependency policy or physical profile change.

## Source contract

`CICS_COMMAND.commandKind = SEND_TERMINAL` represents the basic terminal-control SEND form, distinct from SEND MAP/TEXT/CONTROL and explicit CONVID/SESSION forms. It does not assert a physical device or runtime principal facility identity. The supported syntax subset preserves FROM, LENGTH, explicit NOHANDLE, ERASE, RESP and RESP2. Duplicate, truncated, malformed, missing FROM and unsupported options remain UNAVAILABLE. The shared scanner is source-owned; no downstream source parsing.

Optional `length` is a source expression: `kind = INTEGER | DATA_REFERENCE | LENGTH_OF`, optional `integer` decimal string, optional canonical `reference`, and operand `provenance`. INTEGER has only its integer; reference forms have only their reference. LENGTH_OF reuses the COBOL SpecialRegisterExpression through a small generic embedded-expression bridge. Its reference denotes declaration extent, not a runtime load or physical byte-count proof. The integer/target has no value-inference consumer in this wave.

FROM and LENGTH references use independent occurrence identities and SourceMap/include provenance. The entire statement is not substituted for operand origin. Unmodified whole-item identity is retained without inventing physical storage. LENGTH reference role in the AST is DECLARATION_RELATION for LENGTH_OF; a generic SP READ operand does not turn that declaration relation into an executable load.

SP2.44 is emitted only when the new family occurs. Existing products retain their historical version/shape; no empty length field is added to historical wire. Lower profiles explicitly admit2.44 and reject SEND_TERMINAL/length under2.43. Unknown2.45 stops at UNSUPPORTED_CONTRACT before feature validation. Nullable absent length is normalized inside the wire adapter only; a supported LENGTH without its structural expression is rejected.

## Language oracle and executable boundary

IBM [terminal control commands](https://www.ibm.com/docs/en/cics-ts/6.x?topic=control-terminal-commands) and [SEND logical](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-send-3270-logical) establish the source family and operand roles. Without WAIT, return can precede I/O completion. [Condition handling](https://www.ibm.com/docs/en/cics-ts/5.6.0?topic=conditions-using-push-handle-pop-handle-commands) distinguishes command-local NOHANDLE return from SEND MAP OVERFLOW and handler registration. No general NOHANDLE→fallthrough rule is introduced.

A scalar executable experiment used AIR2.0's existing Opaque envelope, then failed on the real group FROM area: data:770/storage-node:2631 in COACTUPC has no published logical group view or physical extent and no materialized nominal AIR object. Creating a new group object or treating absent effects as a NOP would not qualify the real command. The experiment was withdrawn; it is not production behavior.

Final disposition: every SEND_TERMINAL remains ExecutableLowering.NOT_READY. ControlTopology remains UNKNOWN_LOCAL for this family. BOUNDED_POSITIVE retains real independent AIR publication and explicit NonExecutableCapability; EXECUTABLE_ONLY retains IMPLEMENTATION_LIMIT. CANCEL/ABEND after the real SEND are not traversed. Next first-loss owner is storage/effect contract/materialization, not the state engine; no AIR expansion was proved necessary.

## Gates and superseded expectations

TerminalSendContractTest and TerminalSendSuite cover typed probes, SourceMap/COPY, expression shape, closed version admission, real frontend bytes, deterministic publication and forbidden continuation. Historical CicsCommandContractTest's exclusion of SEND FROM from the typed inventory is superseded by2.44 only; its no-fallthrough rule survives. Future-version sentinel2.44 becomes2.45 in consumer tests. These are version/availability changes, not weakened control or state oracles.

R7-R4, R7-R3-R1, R7-R3, R7-R6 and R7-R7A gates remain required. No R8/R9/ALTER. Existing PRs remain draft; no merge.
