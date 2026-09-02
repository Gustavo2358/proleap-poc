# WORK-COND-002 — Decisão arquitetural para condições contextuais

Status: concluído em 2026-09-02. Risco: alto.

O Slice 2 de `BACKLOG-COND-001` foi concluído com o merge do PR #16 em `main` (`1dd1d87`). O checkpoint arquitetural registrou a decisão de representar condições combinadas e abreviadas como **surface AST lossless + produto pós-binding separado** (`ConditionSemantics`), mantendo `ReferenceResolution` exclusivamente nominal e adiando a validação type-sensitive para a etapa conceitual `ConditionValidation`. ADR-0012 ficou `Accepted`; INV-COND-001 e INV-COND-002 guardam a fronteira.

O review humano do PR #16 pediu dois ajustes, ambos aplicados: concluir o lifecycle do ADR (movê-lo para a tabela de decisões aceitas) e separar normalização de condition semantics de validação type-sensitive. Nenhum arquivo de produção, grammar, fixture, teste ou script mudou neste work item; o diff foi exclusivamente documental.

O conhecimento durável foi promovido para [ADR-0012](../../architecture/decisions/0012-contextual-conditions-use-post-binding-projection.md), [INV-COND-001/002](../../architecture/invariants.md), o [contrato de expressões condicionais](../../domain/conditional-expressions.md) e o [catálogo de oracles `COND-*`](../../evals/conditional-expression-oracles.md). Este resumo não autoriza implementação: o Slice 3 executável pertence a `WORK-COND-003`.
