# Trabalho ativo e backlog

Work items delimitam a mudança em execução; o [backlog](backlog.md) registra trabalho futuro ainda sem autorização de início. Não use tasklists históricas como contexto padrão.

## Ativo

- [WORK-AST-002 — Hardening da fronteira AST para CFG e dataflow](active/WORK-AST-002/spec.md) — Slice 1 mergeado no PR #10; Slice 2 no checkpoint de Discovery do PR #13. Implementar F-02 depende de merge/review e autorização explícita posterior.
- [WORK-COND-002 — Decisão arquitetural para condições contextuais](active/WORK-COND-002/spec.md) — Slice 2 de BACKLOG-COND-001 no checkpoint arquitetural do PR #16; propõe AST de superfície + produto pós-binding e proíbe implementação antes de review/merge e nova autorização.

## Histórico

`history/` recebe somente resumos de work items concluídos que ainda ajudem a explicar uma decisão ou migração.

- [WORK-HARNESS-001 — Concluir Harness Engineering v1](history/WORK-HARNESS-001.md)
- [WORK-AST-001 — Construção da AST dirigida por contextos tipados](history/WORK-AST-001.md)
- [WORK-AST-003 — Corrigir a consistência global entre IDs e traversal da AST](history/WORK-AST-003.md) — Discovery no PR #11 e implementação no PR #12; não bloqueia mais WORK-AST-002.
- [WORK-COND-001 — Contrato normativo de condições combinadas e abreviadas](history/WORK-COND-001.md) — Slice 1 concluído pelo PR #15; regra durável e oracles foram promovidos para domínio/evals.
- [WORK-RES-001 — Observar CALLs literais externos por artefato](history/WORK-RES-001.md)
- [WORK-RES-002 — Veredito sobre W3D-AUX e categorias de resolução](history/WORK-RES-002.md)
- [WORK-RES-003 — Resolver SET de condition-name sem namespace DATA espúrio](history/WORK-RES-003.md)
- [WORK-RES-004 — Classificar condition-names em EVALUATE TRUE/FALSE](history/WORK-RES-004.md)
- [WORK-TEST-001 — Restaurar relatório PIT focalizado](history/WORK-TEST-001.md)
- [WORK-TEST-002 — Substituir cardinalidades globais por oráculos semânticos](history/WORK-TEST-002.md)
- [WORK-EXT-001 — Classificar `DFHRESP` e `DFHVALUE` unresolved como possíveis intrínsecos CICS](history/WORK-EXT-001.md)
- [WORK-COV-001 — Preservar análise parcial diante de COPY ausente](history/WORK-COV-001.md)
