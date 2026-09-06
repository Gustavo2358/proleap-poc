# Estado

## Onde estamos

Slice 1 mergeado no PR #10; pré-requisito de IDs/traversal concluído nos PRs #11/#12; Discovery de F-02 mergeado no PR #13. O PR #28 implementa o Slice 2 na branch `feat/semantic-product-integrity-validator`. A revisão de 2026-09-06 pediu dois refinamentos localizados: admissibilidade dos candidates e kind/owner dos scopes. As correções estão implementadas e validadas localmente; PR aberto, aguardando nova revisão, sem merge automático.

## Verde conhecido

- Validator/exception dedicados implementam os joins do Discovery e a chamada precede classifier/report/Semantic Product, reutilizando os scope indexes.
- Os dois oracles F-02 foram promovidos para execução normal. Os 78 casos focais passam; os testes da revisão reproduziram 14 aceitações indevidas antes da correção e preservaram o positivo contextual DATA/INDEX/CONDITION.
- Gates locais fast, semantic, performance e full verdes: 523 testes, zero failures/errors e um skip preexistente de `semantic.condition.required`, fora de F-02; E2E estruturado e naming passaram. O PR não tinha checks remotos registrados na conferência desta revisão; não há claim de CI remoto verificado.
- Candidates usam unit/domain/localId e declaration IDs exatos; nomes não governam joins. Estados incompletos e candidates ancestrais permanecem aceitos.
- Contratos de produção e baselines anteriores permanecem preservados; nenhuma análise posterior foi introduzida.
- Diff revisado e self-validation do harness: diretório ativo, índices, histórico, contratos documentais e escopo de source/tests coerentes. O gate performance inclui o probe de integridade até 512 units, sem threshold de hardware.

## Restante

- Nova revisão do Slice 2; merge depende de ação posterior e não integra esta autorização.
- Regressão documental final/Slice 3 continua posterior à revisão dos slices de produção; nenhum slice/backlog adjacente foi iniciado.

## Descobertas que afetam o plano

API, ownership, ponto de integração e modelo de falha seguem o Discovery. ScopeKind/owner e nome de procedure section influenciam qualification; agora são reconciliados com seus anchors, sem nova análise. O índice de declarations por AST ID é construído uma vez por unit. SQL/FILLER e relação de FILLER REDEFINES permanecem fora do checkpoint.
