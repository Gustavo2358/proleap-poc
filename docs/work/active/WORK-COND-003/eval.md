# Avaliação — WORK-COND-003

## O que prova corretude

A AST de superfície preserva, com nodes tipados, exatamente a estrutura escrita da condição — conectores, precedência, parênteses, NOT, relations completas/abreviadas, distribuição e tails contextuais — sem executar binding, expansão ou validação. Os oracles S3-01 a S3-10 derivam dos `COND-*` normativos; a identidade/pre-order reusa o enforcement de INV-AST-003; provenance reusa INV-PROV-002. O oracle negativo observa a shape semântica da AST (presença de `ContextualConditionTail`), nunca `grammarRule`.

## Classes positivas

- `A = B OR C`: relation explícita + connector OR + tail contextual `C`; nenhum `A = C` sintético (S3-01).
- `A = B OR C OR D`: ambos os tails preservados e alcançáveis por `Ast.children`; nenhum subject/operator sintético (S3-02).
- `A = B OR < C`: operator `<` escrito preservado; subject OMITTED sem node; `A` não clonado (S3-03).
- `A NOT = B OR C`: `NOT =` no relational operator; ausência de logical NOT artificial (S3-04).
- `A = B OR NOT C OR D`: logical NOT envolve somente `C`; `D` é irmão; NOT não vira operator (S3-05).
- `A = B OR C AND D`: `OR[A = B, AND[C, D]]`; sem `MIXED_LOGICAL` (S3-06).
- `(A = B OR C) AND D`: grupo explícito com boundary observável; `D` fora do grupo (S3-07).
- `A = (B OR C) AND D`: `DistributedOperandGroup` distinguível de grouping; sem `A = B OR A = C` (S3-08).
- `A = B OR C = D OR E`: duas relations completas + tail `E`; nenhuma expansão (S3-09).
- `A = B OR C IS NUMERIC OR D`: `ClassCondition` distinta; `D` preservado (S3-10).
- `a = b or c` → `A = B OR C` preserva shape (M1); alpha-rename preserva topology (M2); grouping explícito neutro só adiciona o node de grupo, sem apagar connectors/operands (M3).

## Classes negativas

- `grammarRule == conditionNameReference` não governa categoria semântica; o tail permanece contextual independentemente do ramo interno.
- subject/operator omitidos não viram `Ast.Node`, não ganham span e não geram occurrences sintéticas.
- nenhum node escrito é clonado ou compartilhado entre pais; IDs continuam `0..N-1` em pre-order.
- bare tail não vira DATA, INDEX ou CONDITION definitivo.
- logical NOT não é incorporado ao relational operator nem propagado a elementos posteriores.
- todo parêntese não é tratado igual: grupo explícito (boundary) e distribuição (operands sob operator) têm shapes distintas.
- `writtenText` não é reparsed; o builder opera sobre contexts/tokens.
- nenhum lookup nominal ou declaração é consultado no `AstBuilder`.
- resolver/collector não ganham nova responsabilidade; o falso gap CONDITION pode permanecer neste slice.

## Classes ambíguas

1. Bare nominal com estado de abbreviation aberto: `ContextualConditionTail`; a especialização permanece aberta para o produto pós-binding.
2. Bare nominal após boundary de grupo/class condition: permanece referência nominal de simple condition (shape atual); a estrutura completa de condition-name pertence ao Slice 4.
3. `abbreviation` recursiva parentética sem modelagem neste slice: `PreservedExpression` fail-closed com referências reconhecidas.
4. `relationSignCondition`: permanece no formato atual (OperationExpression por rule); nenhum oracle novo exige categoria própria neste slice.

## Casos adversariais

- **COND-A01 — branch ANTLR enganoso:** `A = B OR C` com `C` DATA prova que o tail é `ContextualConditionTail`, nunca fechado como CONDITION pela grammar.
- **COND-A03 — boundary versus distribuição:** `(A = B OR C) AND D` versus `A = (B OR C) AND D` exigem node types distintos.
- **COND-A04 — NOT relacional versus lógico:** `A NOT = B OR C`, `A = B OR NOT C OR D` e `NOT A = B OR C` exigem posições distintas para `NOT`.
- **COND-A05 — precedência:** `A = B OR C AND D` e variantes agrupadas rejeitam flattening.
- **COND-A11 — restrições da distribuição:** `A = (B OR C = D)`, `A = (B OR CONDITION-88)` e `A = (NOT B OR C)` continuam rejeitáveis pela surface (group modelado apenas como operands; decisão de validade permanece futura).
- **COND-A12 — atualização do estado:** `A = B OR C OR D` versus `A = B OR C = D OR E` provam que a relation completa posterior é relation própria e `E` permanece tail.
- **COND-A13 — término por qualquer simple condition:** `C IS NUMERIC` preservado como simple condition distinta e `D` não rebaixado.
- **Oracle anti-atalho:** teste que falharia sob implementação `if (ctx.conditionNameReference() != null) return CONDITION;` — observa a shape da AST, não o `grammarRule`.

## Casos de regressão

- `SemanticConditionContextDiscoveryTest` continua caracterizando o falso gap CONDITION (collector/resolver intocados), com asserts migrados para a nova surface.
- `AstPreorderInvariantTest` e `AstBoundaryTestSupport.assertActualProductsJoin` continuam verdes sobre os nodes novos.
- `ArchitectureBoundaryTest` continua bloqueando dependências novas da AST para symbols/resolution.
- Snapshots/baselines de corpus só mudam onde a estrutura legítima mudou; cada diff de cardinalidade é explicado.

## Propriedades/relações metamórficas

1. **M1 — case:** variar caixa de `a = b or c` não altera a shape estrutural da condição.
2. **M2 — alpha rename:** renomear consistentemente A/B/C para X/Y/Z preserva a topology da condition surface.
3. **M3 — grouping explícito:** adicionar parênteses semanticamente neutros só altera explicitamente o node/group correspondente; não apaga connectors nem operands.
4. Determinismo: mesma entrada e policy produzem os mesmos IDs e tree (INV-DET-001).
5. Nenhum node é alcançado duas vezes e nenhum ID repete (INV-AST-003).

## Expectativas de escala

A construção percorre cada condition subtree uma vez: cada context é visitado uma única vez e o fold de precedência é linear no número de tails (`O(condition nodes)` em tempo e memória). Não há scan global, lookup, reparse ou passagem quadrática. O gate `performance` existente continua sem threshold de hardware; a propriedade algorítmica desta construção é verificável por inspeção do código e pelo teste de construção em fixtures de escala modesta.
