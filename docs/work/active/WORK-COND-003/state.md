# Estado — WORK-COND-003

## Onde estamos

Slice 3 de `BACKLOG-COND-001` implementado e verde na branch `implementation/work-cond-003-lossless-condition-surface`, base `main` em `1dd1d87`. Precondições verificadas: ADR-0012 `Accepted`, INV-COND-001/002 presentes, WORK-COND-002 encerrado (resumo histórico + diretório ativo removido + índices atualizados). Resta somente challenge pass final, commit, push e PR.

## Verde conhecido

- Gates `fast`, `performance`, `semantic` e `full` passaram após a implementação; grammar com zero diff.
- Suíte Maven completa verde: 246 testes (19 novos em `ConditionSurfaceAstTest`), 0 falhas.
- Surface AST lossless implementada: `LogicalCondition` (precedência estrutural), `GroupedCondition` (boundary de parênteses), `RelationCondition` (subject/operator `null` = OMITTED; `NOT` relacional no operator), `NegatedCondition`, `ContextualConditionTail`, `DistributedOperandGroup`, `ClassCondition`; `Ast.children` cobre cada um exatamente uma vez; IDs pre-order contíguos.
- `abbreviation+` integralmente materializado (sem truncamento em `abbreviation(0)`); fold de precedência linear com span real para nodes sintetizados (`ParseTreeOrigin.rootNodeId = -1`).
- `ReferenceOccurrenceCollector` ganhou somente o branch estrutural de `RelationCondition` (delega a `visitRelationalOperand`), documentado no `plan.md` antes da edição; classificação DATA/INDEX/CONDITION intacta e o falso gap CONDITION persiste sem mudança.
- `AstSnapshot` com label/attributes dos novos nodes; E2E de snapshots verde no gate `full`.

## Restante

- challenge pass adversarial contra as doze estratégias erradas (revisão concluída; cada uma possui teste bloqueador ou justificativa de review);
- revisão do diff completo; commit; push; PR contra `main`; parar (Slice 4+ não autorizado).

## Descobertas que afetam o plano

1. A grammar fecha bare tails como `conditionNameReference` por adaptive prediction; o lowering agora ignora esse ramo como categoria e o oracle negativo observa a shape semântica.
2. Migrações de baseline aplicadas com justificativa: discovery test (surface nova, gap CONDITION preservado), boundary matrix (`abbreviation` virou boundary modelada `Ast.RelationCondition` sem finding de coverage), `CoverageSnapshotTest` (`abbreviation` saiu do snapshot de preserved boundaries), `AstBuilderTypedTraversalTest`/`StructuredExpressionAstTest` (types novos). Mudanças de cardinalidade de occurrences: subject de relation distribuída passou de `{DATA}` para `{DATA,INDEX}` (agora modelado como relation operand); object de abbreviation explícita passou de `{DATA}` para `{DATA,INDEX}` (mesma regra do collector para relation operands). Nenhum expected de resolution foi alterado para esverdear teste.
3. Fold de precedência exige node AND sintetizado com span real entre o primeiro e o último context escrito do chain; sem span inventado e sem node para conteúdo omitido.
4. A alternativa recursiva de `abbreviation` (parênteses internos) e `relationSignCondition` permanecem no formato anterior (preserved/OperationExpression); sem oracle novo exigindo categoria própria neste slice.
5. Documentos canônicos atualizados: `semantic-ast.md`, `conditional-expressions.md` e a exceção conhecida de INV-COND-001 agora refletem que o lowering parou de fechar tails; o collector continua até o Slice 5.
