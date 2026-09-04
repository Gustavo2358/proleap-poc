# Estado — WORK-COND-007

## Onde estamos

Checkpoint 1 de Discovery + Corpus Characterization concluído na branch `discovery/work-cond-007-broad-corpus-regression`, no escopo do PR #21. A seleção do CardDemo está congelada e a proveniência, inventory, baseline, findings, bug-refutation e self-refutation estão concluídos. O checkpoint está pronto para human review/closure.

## Verde conhecido

- A fonte externa está fixada e licenciada em `corpus/carddemo/provenance.md`; a seleção versionada contém dez programas e a closure de COPY correspondente.
- Os gates aplicáveis `check-fast.sh`, `check-semantic.sh` e `check-full.sh` passaram após a expansão; `verify-naming.sh` e a integridade documental também passaram.
- Sete programas selecionados completam a pipeline; dois falham no preprocessing por `EXEC DLI`; um falha na normalização por TAB em fonte fixed-format. Esses resultados estão sendo tratados como dados de corpus, não mascarados.
- Nenhuma alteração em `src/main/**`, gramática, resolver ou semantic manifest foi feita.
- F-01 é `CONFIRMED_KNOWN_BUG`: 34 occurrences em `COACTUPC` (5), `COCRDSLC` (1) e `COTRTUPC` (28) chegam como `DATA/{DATA}` e terminam `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`; a remediação requer decisão arquitetural e não foi autorizada.
- F-09 é `TEST_GAP`: não há oracle ativo para `EVALUATE TRUE WHEN FLAG-ON AND OTHER-ON`; o caso combinado permanece requisito do futuro work item e não foi materializado.
- `performance` foi removido dos gates deste Discovery. `check-performance.sh` não foi executado porque nenhuma implementação ou propriedade algorítmica foi alterada; os timings no `eval.md` são apenas observacionais.

## Restante

- Review humano e encerramento formal do work item; até lá, o diretório permanece ativo.
- Em trabalho futuro autorizado, descobrir primeiro a fronteira do produto semântico downstream e então priorizar F-01 por impacto; nenhuma implementação de produção, Semantic Product, Cobol Lower ou IR está autorizada neste checkpoint.

## Descobertas que afetam o plano

- O CardDemo não oferece `SEARCH WHEN` ordinário em quantidade; há um `SEARCH ALL` em `COPAUS1C`. A cobertura desse construct será complementada pelo oracle sintético durável existente.
- `EXEC DLI` é rejeitado pelo preprocessing atual e não deve ser convertido em stub.
- `COTRTLIC.cbl` contém um TAB literal que viola a entrada fixed-format configurada; o source upstream não será alterado.
- F-01 foi submetido a BR-01..BR-15 e confirmado como bug conhecido, não como mero unresolved esperado. A informação futura está preservada em `BACKLOG-RES-003`, incluindo reproducer, evidência CardDemo, camada provável, contraexemplos e regressão combinada necessária.
- O gap de teste combinado é deliberado: não congelar o comportamento incorreto atual. A futura correção deverá preservar contexto estrutural de `EVALUATE TRUE/FALSE`, branches e `ALSO`, além de manter selectors DATA/value fora dessa classe.

## Status do checkpoint

`DISCOVERY_REVIEW_READY`: Discovery concluído, pronto para human review/closure. Nenhuma implementação de produção foi autorizada.
