# Qualified CICS host effects and ordinary control

Status: IN_PROGRESS. Scope: SP 2.46, source-qualified SYNCPOINT, RECEIVE MAP,
SEND MAP and terminal SEND. The existing ControlTopology remains authoritative.

## Cause and authority

The producer already publishes successful command completion separately from
handler/default/overflow uncertainty. The consumer nevertheless turns every typed
command into a closed frontier, including commands with completely known explicit
application data operands. This loses later definitions and calls.

IBM CICS [RECEIVE MAP](https://www.ibm.com/docs/en/cics-ts/5.5.0?topic=summary-receive-map),
[SEND MAP](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-send-map) and
[SYNCPOINT](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-syncpoint) define
input/output host areas and condition handling. INTO, RESP and RESP2 may change;
FROM and data-valued command parameters are read. LENGTH OF refers to declaration
extent rather than reading the current value. Ordinary control and exceptional
control remain the already published language facts.

## Proof and limits

An optional hostEffects proof states that all explicit application host operands
are classified. It identifies literal MAP/MAPSET option occurrences positively;
absence of a reference alone never proves a literal. Other operands require their
canonical whole data reference. RECEIVE/SEND MAP require explicit INTO/FROM;
implicit symbolic map areas remain unqualified. Syntax gaps or incomplete operand
structure cannot produce this proof. Transport/projection only translate it.

The proof does not assert runtime values, mandatory overwrite, physical widths,
mapset availability, terminal behavior, EIB contents or handler dispatch. As in the
existing CICS FILE contract, task/runtime effects are outside the application host
memory footprint and retain open environment/dependency coverage. Runtime-owned
LINKAGE data is not promoted to a private logical cell by this proof.

The consumer requires all host references to have independently published,
grounded memory bindings before translating the command to AIR Opaque. Group
operands use their published region/descendant-cell bound, without synthesizing
extent, offsets or alias disjunction. Reads/writes are bounded MAY effects; no kill
is inferred from a complete list of operands. Only ControlTopology destinations
are used, including its explicit unknown frontiers. Missing proof or unmaterialized
host storage keeps the execution frontier. Earlier SP versions remain unchanged.

Preparation and qualification are linear in command operands, using indexed
canonical occurrences and bindings. No COBOL reparsing in the consumer, path
inference, target-name heuristic or new solver is introduced.

Oracles: a computed target after a qualified RECEIVE/PERFORM must remain reachable;
INTO/RESP scopes must retain their published bounds, aliasing writes stay visible;
implicit maps, unknown operands and unsupported options remain blocked; SEND MAP
OVERFLOW remains a separate frontier; payload display-text mutations cannot alter
execution; old SP fixtures preserve NOT_READY.

RETRIEVE with explicit INTO joins this source command family in SP 2.46.
[IBM RETRIEVE](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-retrieve)
defines an ordinary return, with RESP/NOHANDLE handling as above. The initial
subset admits INTO and the common response options. SET, WAIT, LENGTH and other
forms remain unqualified until their host roles are published. A missing INTO
host declaration prevents hostEffects and AIR execution, but does not erase the
source command's qualified ordinary outcome. Source-qualified literal calls may
use that positive control evidence through the existing R9 source path.

## HANDLE ABEND registration

IBM CICS TS [HANDLE ABEND](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-handle-abend) installs, cancels or restores a handler. LABEL is a procedure identity, not an application data operand. Registration does not execute the handler. The closed subset ABEND/LABEL/CANCEL/RESET/NOHANDLE has no application memory operands; RESP/RESP2 and PROGRAM remain outside this proof. A resolved LABEL with entry, or CANCEL/RESET, already carries a positive ordinary-completion proof.

SP 2.46 adds optional `registrationEffects: NO_APPLICATION_MEMORY` only to positively qualified operations. Historical publications omit it. Lowering translates this fact to an AIR opaque operation with empty application memory, open runtime/dependency effects, and exactly the published control destinations. It neither adds a handler-dispatch edge nor changes handler analysis. Missing/ambiguous labels and unsupported options remain barriers. Qualification is linear in options; no names or corpus identity participate. Tests cover registration followed by a call, absent/invalid proof and historical-version rejection.
