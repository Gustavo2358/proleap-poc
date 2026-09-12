# Trabalho ativo e backlog

Work items delimitam a mudança em execução; o [backlog](backlog.md) registra trabalho futuro ainda sem autorização de início. Não use tasklists históricas como contexto padrão.

## Ativo

- [WORK-SEMANTIC-PRODUCT-006 — scalar MOVE data source](active/WORK-SEMANTIC-PRODUCT-006.yaml) — IN_PROGRESS; [SP 1.5.0 contract](../domain/move-data-source.md).

- [WORK-AST-002 — Hardening da fronteira AST para CFG e dataflow](active/WORK-AST-002/spec.md) — Slice 1 mergeado no PR #10; Discovery do Slice 2 no PR #13 e implementação de F-02 integrada pelo PR #28. Nenhum slice adjacente foi iniciado.


O restante de SP-005/SP-003 não foi promovido. `BACKLOG-IR-001`,
`BACKLOG-LOWER-001` e `BACKLOG-CFG-001` permanecem
como registros/handoffs cross-repo para `air-java`, `cobol-lower` e
`analysis-cfg`, não como implementação futura deste repositório.

## Histórico

- [WORK-AST-006 — CP6 W2A IF semantic facts](history/WORK-AST-006.md) — APPROVED / MERGED / CLOSED; PR #35; [baseline W2A congelado](evidence/WORK-AST-006/closeout/baseline.json). W2C/B/D NOT_STARTED / NOT_AUTHORIZED.

- [WORK-AST-005 — CP6 W1A CALL Semantic Product](history/WORK-AST-005.md) — PR #34 mergeado.

- [WORK-AST-004 — NEXT SENTENCE canonical coverage](history/WORK-AST-004.md) — PR #33 mergeado.

`history/` recebe somente resumos de work items concluídos que ainda ajudem a explicar uma decisão ou migração.

- [WORK-HARNESS-001 — Concluir Harness Engineering v1](history/WORK-HARNESS-001.md)
- [WORK-AST-001 — Construção da AST dirigida por contextos tipados](history/WORK-AST-001.md)
- [WORK-AST-003 — Corrigir a consistência global entre IDs e traversal da AST](history/WORK-AST-003.md) — Discovery no PR #11 e implementação no PR #12; não bloqueia mais WORK-AST-002.
- [WORK-COND-001 — Contrato normativo de condições combinadas e abreviadas](history/WORK-COND-001.md) — Slice 1 concluído pelo PR #15; regra durável e oracles foram promovidos para domínio/evals.
- [WORK-COND-002 — Decisão arquitetural para condições contextuais](history/WORK-COND-002.md) — Slice 2 concluído pelo PR #16; ADR-0012 `Accepted` e INV-COND-001/002 promovidos; diff exclusivamente documental.
- [WORK-COND-003 — Surface AST lossless para condições combinadas e abreviadas](history/WORK-COND-003.md) — Slice 3 concluído pelo PR #17; nodes tipados da condition surface sem binding; collector/resolver intocados.
- [WORK-COND-004 — Preservar estrutura nominal completa de condition-name references](history/WORK-COND-004.md) — Slice 4 de `BACKLOG-COND-001` concluído pelo PR #18; `DataReference` lossless para condition-name surface, `UNSPECIFIED` preserva a incerteza do qualifier, `BACKLOG-RES-004` mantém a resolução DATA/FILE futura.
- [WORK-COND-005 — Contextualizar occurrences de condições](history/WORK-COND-005.md) — Slice 5 de `BACKLOG-COND-001` concluído pelo PR #19; occurrences contextuais shape-sensitive, `PerformControl` tipado e manifesto `1.1.0`.
- [WORK-COND-006 — Materializar conditions de SEARCH WHEN](history/WORK-COND-006.md) — Slice 6 de `BACKLOG-COND-001` concluído pelo PR #20; boundary `SearchStatement`/`SearchWhen`, routing contextual, VARYING shape-sensitive e `NEXT SENTENCE` preservados; validação normativa de SEARCH ALL permanece futura.
- [WORK-COND-007 — Broad corpus regression for contextual conditions](history/WORK-COND-007.md) — Checkpoint de Discovery + caracterização do corpus CardDemo concluído e encerrado pelo PR #21; F-01 permanece em `BACKLOG-RES-003` e nenhuma correção de produção foi feita.
- [WORK-HARNESS-IMPACT-001 — Downstream semantic impact classification](history/WORK-HARNESS-IMPACT-001.md) — Taxonomia downstream de oito classes, validator documental e lifecycle hygiene concluídos e aprovados para closure no PR #22.
- [WORK-RES-001 — Observar CALLs literais externos por artefato](history/WORK-RES-001.md)
- [WORK-RES-002 — Veredito sobre W3D-AUX e categorias de resolução](history/WORK-RES-002.md)
- [WORK-RES-003 — Resolver SET de condition-name sem namespace DATA espúrio](history/WORK-RES-003.md)
- [WORK-RES-004 — Classificar condition-names em EVALUATE TRUE/FALSE](history/WORK-RES-004.md)
- [WORK-TEST-001 — Restaurar relatório PIT focalizado](history/WORK-TEST-001.md)
- [WORK-TEST-002 — Substituir cardinalidades globais por oráculos semânticos](history/WORK-TEST-002.md)
- [WORK-EXT-001 — Classificar `DFHRESP` e `DFHVALUE` unresolved como possíveis intrínsecos CICS](history/WORK-EXT-001.md)
- [WORK-COV-001 — Preservar análise parcial diante de COPY ausente](history/WORK-COV-001.md)
- [WORK-SEMANTIC-PRODUCT-001 — Semantic Product Boundary Discovery](history/WORK-SEMANTIC-PRODUCT-001.md) — Discovery concluído pelo PR #26; evidências dos Checkpoints 2, 3A e 3B preservadas.
- [WORK-SEMANTIC-PRODUCT-002 — Semantic Product extensível e readiness final](history/WORK-SEMANTIC-PRODUCT-002.md) — CP1–CP8 concluídos no PR #27; boundary materializada, consumer/JSON independentes e handoff lowering/IR falsificado.
- [WORK-SEMANTIC-PRODUCT-003 — Discovery bilateral Semantic Product / AIR V2](history/WORK-SEMANTIC-PRODUCT-003.md) — audit exclusivamente documental concluído pelo PR #29; matriz, findings e oracles futuros permanecem canônicos, sem início de produção.

- [WORK-SEMANTIC-PRODUCT-004 — Entry primária e saída local GOBACK](history/WORK-SEMANTIC-PRODUCT-004.md) — concluído pelo PR #31.

- [WORK-SEMANTIC-PRODUCT-005 — Checkpoint 4A scalar textual MOVE](history/WORK-SEMANTIC-PRODUCT-005.md) — concluído pelo PR #32; E1–E4 e remediação de escrita preservados nos evals canônicos.
