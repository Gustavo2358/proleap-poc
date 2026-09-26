# W7-R1 directed boundary validation

Scope: review P2 R01/R02 only. Architecture and SP2.37 meaning stay unchanged; no W8, new COBOL semantics, oracle change or merge. Five exact W7 HEADs and clean campaign branches verified. Both edited-campaign PRs OPEN/DRAFT; w4-b.2 verified by both frozen verifiers.

Review package `revisao_w7_sp237.zip`: SHA-256 `2de8cb6f4b4d23cf6bb38fc168ad0dc1a25038534e078c2c5ac6e73330e2b4df`. Its included SHA256SUMS was checked. The original generator and Java probe were read, then run unmodified against the immutable W7 lower runtime. Package files and generated proposal manifest remain historical; execution outcomes are recorded separately.

Observed RED at lower be2acf93696e3e87802ac97576bc668269278832:
- Control: decodes and produces AIR.
- R01 appended null: NullPointerException in SpJsonDecoder.decodePayload:96.
- R02 self destination on MOVE with no intrinsic successor: SUCCESS, publication present.
- Original probe exits1 with both failures, recorded in red-2.stdout/stderr and baseline-commands.json.

Root cause R01: the new subtree is removed before the existing recursive requirePhysical visitor sees it. Only top-level relation fields were checked. Existing required DTO/record/list visitor already rejects null elements and malformed nested provenance with PhysicalShape -> Rejected(INPUT_ERROR). Reuse it on this subtree before any dereference or Materialize; no broad exception catch.

Root cause R02: producer CobolSemanticProduct.State at f62a4140, lines1597–1601 forbids statement=destination. Lower PartialProgramAdmission validates closed/exact/typed/consistent ordinary facts but omits this inequality. Add a STRUCTURE admission rule on ordinary relations only. JSON transports the relation; core admission enforces semantic validity for both JSON and in-memory inputs. Do not prohibit cycles in GO TO or PERFORM.

Other new relation guards audited: source family/existence, same-unit closed destination, exact/valid provenance, KNOWN target, consistency with intrinsic successor and duplicate source. Existing checks remain; new tests cover malformed physical provenance and semantic self-edge independently from intrinsic conflict.

Validation impact C3: focused physical decoder/admission tests and CLI rejection/output preservation; normal W7/W6/R1 control suite; lower FAST; mandatory local full for contract/admission changes per lower lean-harness policy. Producer production unchanged; its published invariant is authority. Selected E2E covers no-ELSE/no-OTHER, ordinary facts, legitimate backward GO TO and context/deadness32/33/34/39. Frozen verifier before/after, corpus replay required by campaign retained. Previous Chaos/performance observations stay explicitly historical, not claimed as fresh R1 measurements.
