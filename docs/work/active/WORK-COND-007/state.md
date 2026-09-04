# Estado — WORK-COND-007

## Onde estamos

Checkpoint 1 em Discovery + Corpus Characterization. O PR #20 foi confirmado no histórico de `main` em `8c6f449` (`Merge pull request #20 from Gustavo2358/implementation/work-cond-006-search-when`). Branch atual: `discovery/work-cond-007-broad-corpus-regression`.

O CardDemo foi clonado externamente em `/tmp/work-cond-007/external-corpora/aws-mainframe-modernization-carddemo`, com SHA fixado `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`. A licença root é Apache-2.0; `NOTICE` contém a atribuição Amazon. A seleção preliminar e a closure foram copiadas para `corpus/carddemo/**`, ainda sem commit.

## Verde conhecido

- `main` limpo e atualizado por `git pull --ff-only`; merge do PR #20 confirmado.
- Gates pré-expansão `check-fast.sh` e `check-semantic.sh` passaram em `8c6f449`.
- Há 44 candidatos COBOL no CardDemo; a seleção preliminar contém dez programas e 27 COPYs disponíveis.
- Sete programas selecionados completam a pipeline; dois falham no preprocessing por `EXEC DLI`; um falha na normalização por TAB em fonte fixed-format. Esses resultados estão sendo tratados como dados de corpus, não mascarados.
- Nenhuma alteração em `src/main/**`, gramática, resolver ou semantic manifest foi feita.

## Restante

- Congelar a seleção após CS-01..CS-10.
- Finalizar provenance, inventory, baseline detalhado, findings e bug-refutation.
- Adicionar somente testes/harness de characterization necessários.
- Executar gates finais fast, semantic e full; registrar SHAs, commits, head e PR.

## Descobertas que afetam o plano

- O CardDemo não oferece `SEARCH WHEN` ordinário em quantidade; há um `SEARCH ALL` em `COPAUS1C`. A cobertura desse construct será complementada pelo oracle sintético durável existente.
- `EXEC DLI` é rejeitado pelo preprocessing atual e não deve ser convertido em stub.
- `COTRTLIC.cbl` contém um TAB literal que viola a entrada fixed-format configurada; o source upstream não será alterado.
- Há uma hipótese recorrente em `EVALUATE TRUE WHEN condition-name` que precisa ser submetida integralmente a BR-01..BR-15. O backlog já registra a lacuna conhecida de contexto de occurrence (`BACKLOG-RES-003`); não será corrigida neste checkpoint.
