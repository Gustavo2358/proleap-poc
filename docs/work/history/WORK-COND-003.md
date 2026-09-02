# WORK-COND-003 — Surface AST lossless para condições combinadas e abreviadas

Status: concluído em 2026-09-02. Risco: alto.

O Slice 3 de `BACKLOG-COND-001` foi concluído com o merge do PR #17 em `main` (`4cd95d6`). A surface AST passou a materializar, com nodes tipados e sem binding, a estrutura escrita de condições combinadas e abreviadas conforme ADR-0012: `LogicalCondition` com precedência estrutural, `GroupedCondition` com boundary de parênteses, `RelationCondition` com subject/operator omitidos sem nodes sintéticos, `NegatedCondition`, `ContextualConditionTail`, `DistributedOperandGroup` e `ClassCondition`. Zero diff de grammar; collector/resolver mantidos — o falso gap `grammarRule == conditionNameReference` permaneceu observável e reservado ao Slice 5.

O review humano pediu ajustes aplicados antes do merge: escopo do span de `RelationCondition` sob logical NOT restrito ao fragmento escrito e migração dos asserts do discovery para a nova surface.

O conhecimento durável foi promovido para `docs/domain/semantic-ast.md`, `docs/domain/conditional-expressions.md` e `docs/architecture/invariants.md` (nota do INV-COND-001). A estrutura completa de condition-name references (qualification/subscripts) permaneceu explícita para o Slice 4 (`WORK-COND-004`); este resumo não autoriza implementação.
