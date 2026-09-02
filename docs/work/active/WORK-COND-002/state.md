# Estado — WORK-COND-002

## Onde estamos

Slice 2 de `BACKLOG-COND-001` promovido para work item ativo de risco `high`, limitado ao checkpoint arquitetural do PR #16. Base: `main` em `e30f2d0` (merge do PR #15). Branch: `discovery/work-cond-002-architectural-decision`.

A evidência sustenta a alternativa 3: AST contextual/lossless de superfície e `ConditionSemantics` separado pós-binding. ADR-0012 permanece `Proposed` para review humano; nenhuma implementação foi iniciada.

## Verde conhecido

- `main` foi sincronizada com `origin/main`; o merge do PR #15 está presente e não houve push desnecessário da `main`.
- WORK-COND-001 foi promovido para domínio/evals canônicos, resumido em histórico e removido de `active/`.
- O contrato IBM e os IDs `COND-*` continuam disponíveis fora da tasklist concluída.
- As três alternativas foram comparadas contra separação de produtos, binding, losslessness, IDs/pre-order, provenance, occurrences, scope/qualification, ambiguity, consumidores futuros, incisão e Open/Closed.
- Challenge pass rejeita normalização pré-binding e AST contextual como único produto; os custos do produto pós-binding possuem contratos explícitos.
- ADR-0012, INV-COND-001 e INV-COND-002 registram a decisão proposta e a fronteira de uncertainty.
- Nenhum arquivo em `src/`, grammar, fixture, teste, script ou `pom.xml` foi alterado.
- `./scripts/harness/check-fast.sh`, `./scripts/harness/check-semantic.sh` e `./scripts/harness/check-full.sh` passaram em 2026-09-01; o `full` incluiu fast, semantic, regressão E2E estruturada e naming.
- Commit `fcb0496` foi publicado e o PR #16 foi aberto contra `main`.

## Restante

- obter review/merge humano para aceitar ou revisar ADR-0012;
- somente depois de merge e nova autorização, promover Slice 3 em escopo próprio.

Incertezas não bloqueantes: `cob2` não está disponível para wording/código de diagnostics; nomes Java/schema/snapshot exatos de `ConditionSemantics` pertencem ao slice implementador; occurrences multi-kind e joins precisam de oracles executáveis no slice correspondente; shapes aceitas pela grammar sem suporte IBM permanecem negativas/unsupported.

## Descobertas que afetam o plano

1. AST contextual é necessária, mas insuficiente como única interface de consumers: sem produto pós-binding, cada consumer duplicaria a semântica IBM.
2. Normalização no lowering é inexata justamente no caso central, porque declaration kind/scope ainda não existem; clones/sharing também conflitam com INV-AST-003 e provenance.
3. Source occurrence e semantic use não são a mesma cardinalidade. A expansão pode reutilizar um binding em vários predicate operands sem inventar ocorrências textuais.
4. A projeção pós-binding não amplia a responsabilidade do resolver: ele fornece candidate/status; o projector interpreta condition structure.
5. Ambiguous/unresolved precisa sobreviver como condição contextual parcial. Post-binding não significa “sempre decidido”.
6. O Slice 3 pode implementar somente a surface AST lossless após aceitação do ADR; collector, resolver e `ConditionSemantics` continuam slices posteriores.
