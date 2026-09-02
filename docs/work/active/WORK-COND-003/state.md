# Estado — WORK-COND-003

## Onde estamos

Correção dos REQUEST CHANGES do review humano do PR #17 concluída na branch `implementation/work-cond-003-lossless-condition-surface` (commit do head `9c6c3ec` + a correção localizada de provenance do NOT lógico). Os cinco findings anteriores e o P2 de provenance do NOT foram resolvidos; ADR-0012 e slices posteriores permanecem intocados. Nenhum work item novo foi aberto.

## Verde conhecido

- Suíte Maven completa verde no head: **257 testes, 0 falhas** (30 em `ConditionSurfaceAstTest`).
- Estado do builder é `ConditionState(abbreviationOpen, currentSubjectStartToken)` — privado ao `AstBuilder`; relation completa escrita ancora o token inicial do subject; relation abreviada conserva o anchor; o `GroupedCondition` fecha o estado somente quando `openParen.startToken < currentSubjectStartToken`.
- Double NOT: `NOT NOT = C` → `NegatedCondition(RelationCondition(subject=OMITTED, operator="NOT ="))`; um único `NOT` antes de operator não-NOT integra o operator escrito. **Provenance do NOT lógico corrigida:** o `Meta`/writtenText da `RelationCondition` interna cobre somente o fragmento após o NOT (double NOT: `NOT = C`; NOT sem operator: só o object), e o pai `NegatedCondition` cobre o fragmento inteiro (`NOT NOT = C` / `NOT 5`).
- `abbreviation+` com múltiplas children sob um único connector permanece `PreservedExpression` fail-closed e lossless, sem AND/OR inventado (grammar acceptance ≠ COBOL válido).
- `ReferenceOccurrenceCollector.visitRelationalOperand` reutiliza a política existente para `DistributedOperandGroup`: A/B/C em `A = (B OR C)`/`A = (B AND C)` ficam `{DATA, INDEX}` com uma única occurrence por referência.
- AND sintético de precedência usa `tailOperandStart(...)`: `AND.writtenText == "C AND D"` com `startToken` no token de C.

## Restante

- Nada pendente: commit e push já realizados na branch do PR #17; gates registrados abaixo; aguardando novo review humano.

## Descobertas que afetam o plano

1. O boundary de parênteses é relativo: o `)` só encerra a inserção quando o `(` correspondente está à esquerda do subject corrente; decisão puramente estrutural por token.
2. Na grammar atual, `NOT = C` parseia como `abbreviation.NOT` + `relationalOperator "="`; por isso o single `NOT` diante de operator não-NOT integra o relational operator escrito, enquanto `NOT NOT =` produz logical NOT sobre o operator `NOT =` já contido no context.
3. `abbreviation+` grammar-only com múltiplas children não tem conector escrito entre as abbreviations; permanece fail-closed e lossless.
4. Distribuição reutiliza a classificação nominal já existente de relation operands; occurrences contextuais continuam para o Slice 5.
5. Nodes sintéticos de precedência e o `RelationCondition` sob logical NOT têm span somente do subtree/fragmento que representam, sem incluir o connector do pai nem o `NOT` lógico externo.
6. O falso gap CONDITION (`grammarRule == conditionNameReference` no collector) permanece observável e intocado, conforme o escopo do Slice 3.

## Gates (head atual, após a última alteração de produção)

- `./scripts/harness/check-fast.sh` — **passed**
- `./scripts/harness/check-performance.sh` — **passed**
- `./scripts/harness/check-semantic.sh` — **passed**
- `./scripts/harness/check-full.sh` — **passed**
