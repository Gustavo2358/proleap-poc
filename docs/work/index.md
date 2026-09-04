# Trabalho ativo e backlog

Work items delimitam a mudança em execução; o [backlog](backlog.md) registra trabalho futuro ainda sem autorização de início. Não use tasklists históricas como contexto padrão.

## Ativo

- [WORK-AST-002 — Hardening da fronteira AST para CFG e dataflow](active/WORK-AST-002/spec.md) — Slice 1 mergeado no PR #10; Slice 2 no checkpoint de Discovery do PR #13. Implementar F-02 depende de merge/review e autorização explícita posterior.
- [WORK-SEMANTIC-PRODUCT-001 — Semantic Product Boundary Discovery](active/WORK-SEMANTIC-PRODUCT-001/spec.md) — CP1 concluído; CP2 aprovado e mergeado no PR #24; 3A executado no PR #25 e aguardando review; trabalho posterior não autorizado.

## Histórico

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
