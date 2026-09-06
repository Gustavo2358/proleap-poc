# Estado

## Onde estamos

Slice 1 mergeado no PR #10; pré-requisito de IDs/traversal concluído nos PRs #11/#12; Discovery de F-02 mergeado no PR #13. Implementação do Slice 2 concluída neste checkpoint autorizado em 2026-09-05, na branch `feat/semantic-product-integrity-validator`, baseada na `main` atualizada `107ce08` (PR #27). O work item permanece ativo para revisão do slice, sem merge automático.

## Verde conhecido

- Validator/exception dedicados implementam os joins do Discovery e a chamada precede classifier/report/Semantic Product, reutilizando os scope indexes.
- Os dois oracles F-02 foram promovidos para execução normal. O validator tem 59 casos focais; os quatro required oracles e dez testes de caracterização passam sem skips.
- Gates fast, semantic, performance e full verdes. A suíte Maven registra 504 testes, zero failures/errors e um skip preexistente de `semantic.condition.required`, fora de F-02; E2E estruturado e naming passaram.
- Candidates usam unit/domain/localId e declaration IDs exatos; nomes não governam joins. Estados incompletos e candidates ancestrais permanecem aceitos.
- Contratos de produção e baselines anteriores permanecem preservados; nenhuma análise posterior foi introduzida.
- Diff revisado e self-validation do harness: diretório ativo, índices, histórico, contratos documentais e escopo de source/tests coerentes. O gate performance inclui o probe de integridade até 512 units, sem threshold de hardware.

## Restante

- Revisão do checkpoint do Slice 2; merge depende de ação posterior e não integra esta autorização.
- Regressão documental final/Slice 3 continua posterior à revisão dos slices de produção; nenhum slice/backlog adjacente foi iniciado.

## Descobertas que afetam o plano

As premissas do PR #13 continuam válidas após o Semantic Product do PR #27. API, ownership, ponto de integração e modelo de falha seguem o Discovery. A reconciliação estrutural também verifica kind/namespace/scope de declarations e o pre-order já contratado por INV-AST-003; não reinterpreta COBOL. SQL/FILLER e relação de FILLER REDEFINES permanecem fora do checkpoint.
