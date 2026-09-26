# Logical VALUE invariants

Status: IN_PROGRESS. Scope: source-owned lifetime proof for a closed local text cell.

IBM Enterprise COBOL 6.4 initializes WORKING-STORAGE VALUE on first allocation and retains its last-used state across ordinary calls ([WORKING-STORAGE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=overview-working-storage-section)). Therefore a value that cannot be written or exposed for the whole program lifetime is invariant, without assuming an INITIAL program or choosing a byte encoding.

The existing physical invariant analysis does not establish logical entry invariants when a byte profile is absent. A new canonical analysis will require a positive R2 LOCAL_CELL proof, closed executable input, a complete typed inventory of writes and escapes, and no overlap with the cell or its ancestors. It examines all executable source, including unreachable statements and other paragraphs: it does not infer reachability. Unavailable command footprints, ambiguous writes, addresses, missing procedure input, shared storage and nested program visibility block the proof. Missing data input remains governed by the existing R2 proof dependencies; qualification is not relaxed.

Known writes and by-reference escapes mark source declaration subtrees. A candidate's existing alias-closure proof excludes sibling overlays. A group write or group escape invalidates every candidate beneath it. Complete read-only operations do not invalidate values. The algorithm indexes resolutions and declaration ancestry, then traverses syntax and ancestors; no pairwise comparison of all declarations is needed. Tests must independently cover a constant, sibling write, ancestor write, alias, external escape, unknown command and missing procedure input.

SP 2.46 represents the result as LOGICAL_TEXT with DECLARATIVE_INVARIANT and explicit source proof. Lowering maps it to an AIR literal entry condition only after validating the existing storage-cell proof. Absence of the new fact preserves POSSIBLE_LOGICAL_TEXT and its lifecycle remainder.

The RETURN footprint is qualified from canonical CICS command framing and COBOL host references. The bounded subset TRANSID/COMMAREA/LENGTH/IMMEDIATE/RESP/RESP2/NOHANDLE follows [IBM CICS RETURN](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-return). COMMAREA is conservatively treated as an exposure, RESP/RESP2 as writes. No new return edge is created. Any unsupported option or missing required host reference blocks the lifetime proof.

`LENGTH OF item` é um registrador implícito separado: não é acesso de escrita
a `item`. O inventário de imutabilidade distingue esse registrador de uma
área de dados. Isso não certifica a legalidade de usá-lo como saída CICS, não
publica uma escrita concreta no registrador e não autoriza continuação.
A eventual incompatibilidade de um argumento de saída continua fora desta
prova. Fonte: [IBM COBOL 6.4 LENGTH OF](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=registers-length).
