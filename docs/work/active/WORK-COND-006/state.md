# Estado

## Onde estamos

Checkpoint 1 — Discovery Round 2 concluído na branch `implementation/work-cond-006-search-when`, partindo do head revisado `1e938ae16414bf19acae5d430ecd7fd73c21e8bc` do Round 1. PR #19 e o commit `5f41bc1` foram confirmados como ancestrais. `WORK-COND-005` não existe em `active/` e seu resumo existe em `history/`.

## Verde conhecido

- working tree limpa antes da criação da branch;
- `./scripts/harness/check-fast.sh` verde no baseline pós-merge;
- `SearchWhenConditionDiscoveryTest` verde com 13 testes, incluindo F1, NEXT SENTENCE e VARYING DATA/INDEX;
- regressões focais `ContextualConditionOccurrenceTest`, `ConditionSurfaceAstTest` e `SemanticConditionContextDiscoveryTest` verdes;
- Round 2: `check-fast.sh`, `check-semantic.sh` e `check-full.sh` verdes;
- nenhuma alteração em `src/main`, grammar, resolver, snapshots ou baselines;
- S1–S6, SEARCH ALL, controle negativo e challenges documentados.

## Restante

Review humano do Round 2 e autorização explícita para Implementation. A implementação continua proibida neste PR. Slice 7 e `BACKLOG-RES-004` permanecem separados.

## Descobertas que afetam o plano

`searchStatement` usa `SEARCH ALL? qualifiedDataName searchVarying? atEndPhrase? searchWhen+ END_SEARCH?`; `searchWhen` usa `WHEN condition (NEXT SENTENCE | statement*)`. O ponto exato de perda da condition é `AstBuilder.visitSearchStatement → preserved → buildStructuredStatement → buildStatementClause`, que não chama o lowering de condition e deixa `recognizedNodes` vazio. O collector atual exige routing tipado: `Ast.children` garante reachability estrutural, mas não converte uma child em position `CONDITION`; o futuro `SearchWhen.condition` deve chamar explicitamente `visitConditionSurface`. `NEXT SENTENCE` é uma alternativa de token fora de `statement()`, e o varying exige policy própria DATA/INDEX. Não há necessidade demonstrada de alterar grammar ou resolver.

SEARCH ALL compartilha a shape local, mas IBM exige key/preceding-key/equal-to/AND-only e compatibilidade própria; o bit `all` deve ser preservado e sua validação não deve ser absorvida silenciosamente pelo materialization slice.

## Round 2 self-review

- **SR-01 PASS** — os documentos do work item dizem que `Ast.children` não substitui `visitConditionSurface`; a busca de consistência não encontrou afirmação contrária.
- **SR-02 PASS** — o path futuro está explícito: `SearchWhen.condition → typed CONDITION position → ReferenceOccurrenceCollector.visitConditionSurface(...)`.
- **SR-03 PASS** — NEXT SENTENCE tem representação futura lossless fechada como `Ast.NextSentenceStatement` na action estrutural da branch.
- **SR-04 PASS** — o oracle verifica `when.statement().size() == 0` e tokens diretos `NEXT`/`SENTENCE`.
- **SR-05 PASS** — o oracle INDEX isola `VARYING SEARCH-IDX`, registra a falha atual e a seleção futura INDEX exigida.
- **SR-06 PASS** — o oracle DATA isola `VARYING SEARCH-COUNTER PIC 9(4)` e registra selectedCandidate DATA.
- **SR-07 PASS** — `SEARCH_VARYING` está fechado como `primary kind DATA`, `admissibleKinds {DATA, INDEX}`, role `CONTEXT_DEPENDENT`.
- **SR-08 PASS** — searched table, varying, condition, qualification e subscript têm posições/policies distintas nos findings e testes.
- **SR-09 PASS** — `SearchWhen` Node é justificado por branch identity, provenance, condition→statements ownership e posição determinística, com precedente `EvaluateBranch`.
- **SR-10 PASS** — SEARCH ALL permanece no mesmo shape estrutural com `all=true`, mas sua validação semântica fica futura/separada.
- **SR-11 PASS** — não foi criada nova condition policy de SEARCH; a condição reutiliza a surface e helpers do Slice 5.
- **SR-12 PASS** — resolver, contratos de resolução, símbolos e filtering permanecem fora do scope.
- **SR-13 PASS** — nenhum arquivo `src/main` foi alterado.
- **SR-14 PASS** — CH-01–CH-15 têm evidência executável ou argumento verificável documentado em `eval.md`.
- **SR-15 PASS** — F1–F3, representation de NEXT SENTENCE, policy VARYING, source scope e must-not-change estão fechados; não resta decisão semântica nova para a implementação autorizada.
