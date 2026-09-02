# Estado — WORK-COND-003

## Onde estamos

Correção dos REQUEST CHANGES do review humano do PR #17 na branch `implementation/work-cond-003-lossless-condition-surface`. Os cinco findings (boundary de grupos relativo ao subject corrente, double NOT, `abbreviation+` fail-closed, política de relation operand para distribuição, span dos AND sintéticos) foram corrigidos no lowering/collector/testes; ADR-0012 e slices posteriores permanecem intocados. Resta somente gates finais, commit e push para o mesmo PR.

## Verde conhecido

- Suíte Maven completa verde após as correções: 256 testes, 0 falhas (29 em `ConditionSurfaceAstTest`, incluindo PAREN-01/02/03, NOT-DOUBLE-01/02, SPAN-01/02/03, distribuição e a suíte adversarial da máquina de estado com os 15 shapes).
- Estado do builder agora é `ConditionState(abbreviationOpen, currentSubjectStartToken)` — privado ao `AstBuilder`, nunca em `Ast`, snapshot ou produto público; relation completa escrita ancora o token inicial do subject; relation abreviada conserva o anchor.
- Boundary de grupo: o `GroupedCondition` fecha o estado somente quando `openParen.startToken < currentSubjectStartToken`; caso contrário o estado sobrevive ao `)` (PAREN-01/02/03).
- Double NOT: `NOT NOT = C` vira `NegatedCondition(RelationCondition(subject=OMITTED, operator="NOT ="))`; um único `NOT` imediatamente antes do relational operator integra o operator escrito (`NOT =`), nunca `NOT NOT =`.
- `abbreviation+` com múltiplas children sob um único connector permanece `PreservedExpression` fail-closed e lossless (grammarRule `andOrCondition`, writtenText `< C > D`, operands C/D reconhecidos), sem AND/OR inventado entre as abbreviations.
- `ReferenceOccurrenceCollector.visitRelationalOperand` ganhou o branch estrutural de `DistributedOperandGroup` reutilizando a política existente: A, B e C em `A = (B OR C)`/`A = (B AND C)` ficam `{DATA, INDEX}` com uma única occurrence por referência escrita.
- AND sintético de precedência usa `tailOperandStart(...)` (primeiro context do operand, excluindo o connector do pai): `A = B OR C AND D` → `AND.writtenText == "C AND D"` e `startToken` no token de C (SPAN-01/02/03).

## Restante

- gates `fast`, `performance`, `semantic`, `full`;
- revisão do diff completo; commit; push na MESMA branch do PR #17; parar para novo review humano.

## Descobertas que afetam o plano

1. O boundary de parênteses é **relativo**: o `)` só encerra a inserção herdada quando o `(` correspondente está à esquerda do subject corrente; a decisão é puramente estrutural por token, sem texto, regex ou lookup.
2. Na grammar atual, `NOT = C` parseia como `abbreviation.NOT` + `relationalOperator "="` (o `NOT?` externo é greedy); por isso o single `NOT` diante de operator não-NOT integra o relational operator escrito, enquanto o double `NOT NOT =` produz logical NOT sobre o operator `NOT =` já contido no context — os testes NOT-DOUBLE-01/02 fixam esse contrato.
3. `abbreviation+` grammar-only com múltiplas children não tem conector escrito entre as abbreviations; atribuir AND/OR seria inventar estrutura. O caso permanece fail-closed e lossless; grammar acceptance não prova validade COBOL.
4. Distribuição reutiliza a classificação nominal já existente de relation operands; o acoplamento `conditionNameReference` do collector e as occurrences contextuais continuam para o Slice 5.
5. Nodes sintéticos de precedência têm span somente do subtree semântico que representam; `metaForRange` sobre contexts do operand, nunca sobre o tail inteiro (o connector pertence ao pai).
6. O falso gap CONDITION (`grammarRule == conditionNameReference` no collector) permanece observável e intocado, conforme o escopo do Slice 3.
