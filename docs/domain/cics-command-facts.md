# Bounded CICS source commands — SP 2.43.0

R7-R6 adds `CICS_COMMAND` for SYNCPOINT (commit form), RECEIVE MAP and SEND MAP.
It inherits SP 2.42, including FactDependencies, ControlTopology, CICS_HANDLER and
CICS_ABEND. The writer selects 2.43 when a command fact is present; older feature
profiles remain unchanged. Consumers must explicitly admit 2.43.

The fact has canonical `header`, `commandKind` (SYNCPOINT / RECEIVE_MAP / SEND_MAP),
`syntaxStatus` (SUPPORTED / UNAVAILABLE), `rawText`, ordered `options`, and
`gapCodes`. Each option uses the existing name, optional operand, source offsets
and optional canonical DataReference. These describe syntax, ownership and nominal
binding, not runtime values, effects, reachability, fallthrough or dispatch.

Supported options:

| Family | Options |
| --- | --- |
| SYNCPOINT | RESP, RESP2, NOHANDLE |
| RECEIVE_MAP | MAP, MAPSET, INTO, RESP, RESP2, NOHANDLE |
| SEND_MAP | MAP, MAPSET, FROM, CURSOR (flag), ERASE, FREEKB, RESP, RESP2, NOHANDLE |

MAP/MAPSET accept nonempty literals or canonical COBOL data references. Other
operand options accept canonical data references. Missing FROM/INTO with a
nonliteral MAP is outside this subset; no implicit area binding is invented.
Duplicate, malformed, truncated or unmodeled options yield UNAVAILABLE with a
cause, never ordinary control qualification. Other command families remain as
before, including SEND FROM/TEXT, RETURN, XCTL and ABEND.

The dedicated syntax/semantic layer uses the source AST and SourceMap. Projectors
transport its immutable contribution. MAP/MAPSET/FROM references are READ;
INTO/RESP/RESP2 references are WRITE. Original/expanded locations, COPY chains and
exactness are canonical; a coarse source map remains inexact. There is no late
spelling search, source reopening or downstream COBOL parser.

## Control authority and primary language rules

The separate command control qualifier combines a supported typed command with
ControlTopology's grammar-owned continuation. It adds the positive NORMAL outcome
for successful ordinary completion. It does not choose a destination by ordinal.

IBM [SYNCPOINT](https://www.ibm.com/docs/en/cics-ts/5.6.0?topic=summary-syncpoint)
and [RECEIVE MAP](https://www.ibm.com/docs/en/cics-ts/5.5.0?topic=summary-receive-map)
have command-condition alternatives. Without RESP or NOHANDLE, the abstraction
retains UNKNOWN_LOCAL for unresolved HANDLE CONDITION/default disposition,
including possible abnormal termination. With RESP or NOHANDLE, those command
conditions return locally. RESP2 alone does not supply this proof.

IBM's [command format](https://www.ibm.com/docs/en/cics-ts/6.x?topic=reference-exec-cics-command-format-programming-considerations)
makes RESP imply NOHANDLE behavior. The source fact still contains only explicit
options; no synthetic NOHANDLE token is emitted.

[SEND MAP](https://www.ibm.com/docs/en/cics-ts/5.6.0?topic=summary-send-map)
explicitly exempts OVERFLOW from RESP/NOHANDLE handling. Its separate UNKNOWN_LOCAL
remainder is retained even with RESP. Neither active HANDLE CONDITION OVERFLOW
nor physical map placement is available here. No exceptional destination is
invented, and the ordinary outcome does not erase that remainder. No NOFLUSH,
ACCUM or paging model is added. Unrelated runtime failures remain outside this
bounded command-condition abstraction, as in R6.

Rules `cics-command-{syncpoint,receive-map,send-map}-ordinary-return` identify the
positive proof; condition proofs/roles preserve the distinct remainders. The
wire control algebra itself is unchanged. No handler target becomes an edge.

All facts remain PARTIAL with CICS_COMMAND_EFFECTS_NOT_MODELED. The lower preserves
them and uses the published topology for nonexecutable state assessment; their
AIR lowering remains NOT_READY. No value producer, dependency admission or
publication rule is added. Complexity is linear in source statements/options,
plus the existing finite topology/state algorithms.
