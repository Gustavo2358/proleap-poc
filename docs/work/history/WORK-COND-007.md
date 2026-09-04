# WORK-COND-007 — Broad corpus regression for contextual conditions

## Resultado

Concluído pelo PR #21, mergeado em `main` no commit `6ed3035`. O checkpoint
entregou Discovery e
caracterização de corpus, sem autorizar correção de produção.

## Escopo e evidência promovida

O objetivo foi verificar se os contratos dos Slices 1–6 de
`BACKLOG-COND-001` permaneciam observáveis em um corpus COBOL amplo, mantendo
provenance, análise parcial e a cadeia source → AST → occurrences → resolution.
O subconjunto CardDemo selecionado incluiu `COACTUPC`, `COCRDSLC`, `COPAUS1C`,
`COTRTUPC`, `COTRTLIC`, `COACCT01`, `CBSTM03A`, `CBPAUP0C`, `CBACT01C` e
`COUSR01C`, com suas dependências de COPY autorizadas.

F-01 foi confirmado como `CONFIRMED_KNOWN_BUG`: condition-names em selectors
combinados de `EVALUATE TRUE` podem chegar como `DATA/{DATA}` e terminar em
`INVALID_NAMESPACE_FOR_CONTEXT`. Ele permanece preservado em
[`BACKLOG-RES-003`](../backlog.md#backlog-res-003--classificar-condition-names-em-evaluate-true-when),
com remediação dependente de decisão arquitetural. F-09 foi registrado como
`TEST_GAP` para a ausência de oracle durável do selector combinado.

Nenhuma correção em produção foi feita. A seleção, os oracles existentes e as
limitações de parsing/preprocessing permanecem evidência de caracterização, não
especificação completa da linguagem. O impacto downstream de F-01 deve ser
reavaliado somente depois que o contrato de Semantic Product e as fronteiras
downstream necessárias forem definidos; nenhuma classe de impacto foi inferida
no checkpoint.

## Encerramento

O PR #21 fechou o checkpoint após os gates aplicáveis e review humano. O
diretório ativo foi removido conforme o protocolo; este resumo preserva apenas
as decisões necessárias para entender o fechamento e o handoff futuro.
