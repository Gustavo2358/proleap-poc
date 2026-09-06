# Estado

## Onde estamos

Checkpoint documental concluído em discovery/semantic-product-air-v2-audit,
desde main 107ce08; revisão pendente. F-02 permanece no PR #28 aberto, head
52ee4eb, conferido em 2026-09-06. AIR 2.0.0 fixada pelo ZIP e commit verificado.

## Verde conhecido

Fast/self-validation e full passaram, incluindo semantic, E2E e naming.
Maven: 433 testes, zero failures/errors, três skipped opt-in (dois F-02 e um
de conditions); não são prova da implementação F-02. Build limpo e probe
descartável confirmam perda de SUBSCRIPT e shapes genéricas de terminais.
Diff restrito a documentação; ZIP/extração/experimento preservados em /tmp.

## Restante

Review/merge deste Discovery em PR novo; F-02 em fluxo separado. Próxima
implementação requer autorização própria: contrato/validator AIR, depois
entrada/terminal do Semantic Product, lowerer e CFG. Nenhum slice iniciado.

## Descobertas que afetam o plano

Opaque máximo é válido, mas não fecha CFG. Entradas, sequência semântica,
terminais, endereçamento e contratos de avaliação/interação precedem precisão;
literal kind sozinho não satisfaz assign. Nenhuma implementação autorizada.
