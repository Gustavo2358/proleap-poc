# R7-R7B1 — source CONTROL independent of executable storage

Status: IN_REVIEW. Scope: positive source-control availability only; executable SEND, buffer storage/effects, AIR2.0, handler-state transfers and dependency policy unchanged.

`CicsCommandControl` now qualifies successful ordinary completion of the already-supported SEND_TERMINAL subset. `ControlTopologySemantics` supplies its canonical grammar-owned destination (including COMPLETE at region boundaries). Explicit NOHANDLE/RESP returns modeled command conditions locally; without either retain NORMAL plus handler/default UNKNOWN_LOCAL. RESP2 alone does not dispose conditions. SEND_MAP retains its distinct OVERFLOW remainder; unsupported forms/options never gain qualification. No physical device is inferred.

Authority: IBM [SEND](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-send-3270-logical), [default exception handling](https://www.ibm.com/docs/en/cics-ts/6.x?topic=conditions-default-cics-exception-handling), and [condition disposition](https://www.ibm.com/docs/en/cics-ts/5.6.0?topic=conditions-using-push-handle-pop-handle-commands). ADR-0013 separates CFG readiness from effects/dataflow readiness; source control does not require successful AIR materialization.

SP remains2.44: no shape/version change, new positive availability in existing ControlTopology. CicsCommandFact executable lowering remains NOT_READY. The lower consumes source topology for analysis, but TopologyProgramAssembler emits an empty bounded executable frontier before interpreting those outcomes. No operation, read effect, storage object, dispatch edge or dependency policy is added.

Two explicit test dimensions: identical source with logical group views enabled/disabled or an explicitly selected test physical profile must preserve byte-identical ControlTopology and identical source state; all executable SEND projections remain blocked. This does not introduce a default physical profile. Existing frozen R7-R7B lower fixtures remain byte-identical.

Superseded only: frontend R7-R7B assertions that a supported terminal SEND must have UNKNOWN_LOCAL source control. Their wire/provenance and malformed-form oracles survive. Historical lower R7-R7B fixture oracles stay frozen; new frontend-generated B1 fixtures exercise new source availability.

COACTUPC now has NORMAL1313→1314, existing CANCEL1314→1315. Local ACTIVE/UNKNOWN predecessors become CANCELED/CANCELED_UNKNOWN. The eligible ABEND has no active local candidate, no unknown local active-target remainder, and an outer-level remainder. Outer logical levels are not modeled; INACTIVE is not ABSENT. AIR execution still cannot cross SEND, nor any other NOT_READY capability. Real FROM ABEND-DATA still lacks a qualified group read place.

Tests: ControlStorageDecouplingTest; ControlStorageDecouplingSuite543 checks/10 metamorphics; historical suites unchanged. Evidence: workspace .positive-memory-topology/positive-cics-handler-r7/control-effects-r7b1. R7 overall remains incomplete; storage work deferred; R8/R9/ALTER not started. No merge.
