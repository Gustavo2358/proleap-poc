# Estado

## Onde estamos

Checkpoint 1 — Discovery concluído na branch `implementation/work-cond-006-search-when`, partindo de `main` em `f02344a`. PR #19 e o commit `5f41bc1` foram confirmados como ancestrais. `WORK-COND-005` não existe em `active/` e seu resumo existe em `history/`.

## Verde conhecido

- working tree limpa antes da criação da branch;
- `./scripts/harness/check-fast.sh` verde no baseline pós-merge;
- `SearchWhenConditionDiscoveryTest` verde com 8 testes;
- nenhuma alteração em `src/main`, grammar, resolver, snapshots ou baselines;
- S1–S6, SEARCH ALL, controle negativo e challenges documentados.

## Restante

Review humano do contrato e autorização explícita para Implementation. Depois disso, a implementação deverá permanecer na mesma branch, com novo checkpoint/PR conforme o protocolo. Slice 7 e `BACKLOG-RES-004` permanecem separados.

## Descobertas que afetam o plano

`searchStatement` usa `SEARCH ALL? qualifiedDataName searchVarying? atEndPhrase? searchWhen+ END_SEARCH?`; `searchWhen` usa `WHEN condition (NEXT SENTENCE | statement*)`. O ponto exato de perda é `AstBuilder.visitSearchStatement → preserved → buildStructuredStatement → buildStatementClause`, que não chama o lowering de condition e deixa `recognizedNodes` vazio. `Ast.children` e o collector são corretos para os produtos que recebem; não há necessidade demonstrada de alterar grammar ou resolver.

SEARCH ALL compartilha a shape local, mas IBM exige key/preceding-key/equal-to/AND-only e compatibilidade própria; o bit `all` deve ser preservado e sua validação não deve ser absorvida silenciosamente pelo materialization slice.
