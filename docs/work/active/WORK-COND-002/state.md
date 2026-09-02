# Estado — WORK-COND-002

## Onde estamos

Slice 2 de `BACKLOG-COND-001` promovido para work item ativo de risco `high`, limitado ao checkpoint arquitetural do PR #16. Base: `main` em `e30f2d0` (merge do PR #15). Branch: `discovery/work-cond-002-architectural-decision`.

O review humano do PR #16 aceitou a decisão arquitetural central e pediu dois ajustes: concluir o lifecycle de ADR-0012 e separar normalização de condition semantics de validação type-sensitive. Ambos foram aplicados: ADR-0012 está `Accepted`; a divisão `ReferenceResolution`/`ConditionSemantics`/`ConditionValidation` está explícita; nenhuma implementação foi iniciada.

## Verde conhecido

- ADR-0012 foi movido para a tabela de decisões aceitas; a seção de decisões propostas foi removida do índice por ter ficado vazia.
- `ReferenceResolution` permanece exclusivamente nominal e não valida tipos; `ConditionSemantics` (conceitual) materializa a relation normalizada sem afirmar validade type-sensitive; `ConditionValidation` (conceitual, futura) verifica a admissibilidade com declaração/tipo e contratos IBM. Nenhuma dessas duas etapas existe em produção nem recebeu API/schema.
- `COND-P06`, `COND-N04` e `COND-A06` possuem owner por fase: binding identifica INDEX; `ConditionSemantics` materializa a relation; `ConditionValidation` verifica a admissibilidade type-sensitive.
- `eval.md` ganhou o challenge arquitetural INDEX válido versus INDEX incompatível com binding nominal idêntico, proibindo implementação futura equivalente a `candidate.kind() == INDEX ⇒ relation válida`.
- INV-COND-001 e INV-COND-002 são invariantes legítimos derivados de decisão aceita.
- Aceitar o ADR não autoriza implementação: Slice 3 continua proibido sem autorização explícita posterior.
- Nenhum arquivo em `src/`, grammar, fixture, teste, script ou `pom.xml` foi alterado; somente documentação mudou.
- `./scripts/harness/check-fast.sh`, `./scripts/harness/check-semantic.sh` e `./scripts/harness/check-full.sh` passaram em 2026-09-02; o `full` incluiu fast, semantic, regressão E2E estruturada e naming.
- O commit deste ajuste foi publicado na branch do PR #16; nenhum PR novo foi aberto.

## Restante

- obter review/merge humano final do PR #16;
- somente depois de merge e nova autorização, promover Slice 3 em escopo próprio.

Incertezas não bloqueantes: `cob2` não está disponível para wording/código de diagnostics; nomes Java/schema/snapshot exatos de `ConditionSemantics` e de `ConditionValidation` pertencem ao slice implementador; occurrences multi-kind e joins precisam de oracles executáveis no slice correspondente; shapes aceitas pela grammar sem suporte IBM permanecem negativas/unsupported.

## Descobertas que afetam o plano

1. AST contextual é necessária, mas insuficiente como única interface de consumers: sem produto pós-binding, cada consumer duplicaria a semântica IBM.
2. Normalização no lowering é inexata justamente no caso central, porque declaration kind/scope ainda não existem; clones/sharing também conflitam com INV-AST-003 e provenance.
3. Source occurrence e semantic use não são a mesma cardinalidade. A expansão pode reutilizar um binding em vários predicate operands sem inventar ocorrências textuais.
4. A projeção pós-binding não amplia a responsabilidade do resolver: ele fornece candidate/status; o projector interpreta condition structure.
5. Ambiguous/unresolved precisa sobreviver como condição contextual parcial. Post-binding não significa “sempre decidido”.
6. Com o ADR `Accepted`, o Slice 3 ainda pode implementar somente a surface AST lossless quando autorizado; collector, resolver e `ConditionSemantics` continuam slices posteriores.
7. Normalização e validação type-sensitive são fases distintas: binding nominal idêntico pode exigir veredito de validade diferente, que somente a futura `ConditionValidation` produz.
