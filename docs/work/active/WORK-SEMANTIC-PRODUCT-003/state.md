# Estado

## Onde estamos

Checkpoint documental rebaseado sobre a main `6d3400e`; revisão/merge do PR
#29 permanecem pendentes. F-02 foi integrado pelo PR #28 em
`2026-09-06T10:05:04Z`, com head `52ee4eb` e merge commit `6d3400e`. AIR 2.0.0
continua fixada pelo ZIP e commit verificado.

## Verde conhecido

Após o rebase, os gates fast, semantic e full passaram; o full incluiu semantic,
E2E estruturado e naming. O snapshot Surefire final registrou 511 testes, zero
failures/errors e um skip preexistente de conditions; os dois oracles F-02
integram a execução normal. O probe descartável original continua evidenciando
perda de SUBSCRIPT e shapes genéricas de terminais. Diff restrito a
documentação; ZIP/extração/experimento preservados em /tmp.

## Restante

Review/merge deste Discovery no PR #29. Próxima implementação requer
autorização própria: contrato/validator AIR, depois entrada/terminal do
Semantic Product, lowerer e CFG. Nenhum slice foi iniciado.

## Descobertas que afetam o plano

Opaque máximo é válido, mas não fecha CFG. Entradas, sequência semântica,
terminais, endereçamento e contratos de avaliação/interação precedem precisão;
literal kind sozinho não satisfaz assign. Nenhuma implementação autorizada.
