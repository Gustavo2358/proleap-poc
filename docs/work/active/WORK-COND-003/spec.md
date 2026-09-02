# Spec — WORK-COND-003

## Problema

A AST atual não preserva integralmente a superfície escrita de condições combinadas e abreviadas. Em `A = B OR C OR D`, o lowering fecha cada bare nominal pelo ramo sintático (`conditionNameReference`) e perde a alternativa contextual; connectors AND/OR mistos são achatados em `MIXED_LOGICAL` plano; `abbreviation+` seleciona somente `abbreviation(0)`; `relationCombinedComparison` vira `PreservedExpression` sem operador distribuído; logical `NOT` e relational `NOT` não possuem shapes distintas; parênteses são colapsados em um operator `GROUP` sem boundary observável. Tudo isso acontece antes de qualquer fase possuir declaration kind/scope — a informação necessária para a decisão final.

## Objetivo

Tornar a condition surface lossless e contextual na AST, representando explicitamente o que foi escrito: conectores e precedência, grupos parentéticos, relations completas e abreviadas (com subject/operator omitidos sem nodes sintéticos), distribuição de operador, logical/relational `NOT` e tails nominais binding-dependent. Nenhuma especialização semântica, binding, expansão ou validação type-sensitive é executada neste slice.

## Domínio de entrada suportado

- `condition` com `andOrCondition*` (AND/OR), `combinableCondition` com `NOT?`, `simpleCondition` com parênteses, `relationCondition`, `classCondition` e `conditionNameReference`;
- `relationArithmeticComparison` (subject e operator escritos);
- `relationCombinedComparison` + `relationCombinedCondition` (operador distribuído com grupo de operands);
- `abbreviation` flat: `NOT? relationalOperator? arithmeticExpression` — inclui subject omitido com operator explícito, subject+operator omitidos e `NOT` lógico sobre fragmento;
- `classCondition` como simple condition estrutural distinta (terminador da sequência abreviada).

Fora do domínio modelado, o fallback `PreservedExpression` continua válido (ex.: a alternativa recursiva de `abbreviation` com parênteses internos, `relationSignCondition` permanece no formato atual).

## Classes semânticas

1. **LogicalCondition** (`AND`/`OR`): n-ário por connector único; precedência estrutural (AND aninha sob OR). Proibido `MIXED_LOGICAL` plano para condições.
2. **GroupedCondition**: parênteses explícitos com span de abertura/fechamento e conteúdo; boundary observável.
3. **RelationCondition**: relation de superfície com `subject` (nulo = OMITTED), `relationalOperator` (nulo = OMITTED; grafia canônica incluindo `NOT` relacional) e `object` (expressão ou `DistributedOperandGroup`).
4. **AbbreviatedRelation**: coberta por `RelationCondition` com `subject == null`; nunca materializa subject/operator herdados.
5. **NegatedCondition**: logical `NOT` aplicado somente ao fragmento seguinte; não vira relational operator.
6. **ContextualConditionTail**: uso nominal escrito em posição cuja interpretação final (DATA/INDEX como object abreviado ou CONDITION como nova simple condition) depende do binding.
7. **DistributedOperandGroup**: grupo de operands sob operator distribuído (`A = (B OR C)`), distinto de `GroupedCondition`.
8. **ClassCondition**: `C IS NUMERIC` etc. como simple condition estrutural, nunca rebaixada a tail contextual.

## Premissas

- `ARCHITECTURE_GUARANTEED` (ADR-0012): a AST é produto de superfície anterior ao binding; a especialização pertence a produto pós-binding futuro.
- `LANGUAGE_GUARANTEED` (contrato IBM em `conditional-expressions.md`): precedência `NOT` > `AND` > `OR`; `NOT` relacional integra o operator; `)` de grupo à esquerda do subject encerra a inserção; grupo distribuído mantém subject/operator correntes.
- `ARCHITECTURE_GUARANTEED` (INV-AST-003): todo `Ast.Node` é alcançado exatamente uma vez no pre-order canônico com IDs `0..N-1`; nenhuma instância é compartilhada entre pais nem clonada.
- `ARCHITECTURE_GUARANTEED` (INV-PROV-002): provenance normal para todo elemento escrito; nenhum span para elementos omitidos.

## Comportamento esperado

- `A = B OR C` → `LogicalCondition(OR)` com `RelationCondition(A,=,B)` e `ContextualConditionTail(C)`; nenhum `A = C` sintético.
- `A = B OR C OR D` → ambos `C` e `D` preservados como tails; nenhum truncamento em `abbreviation(0)`.
- `A = B OR < C` → `RelationCondition(subject=OMITTED, operator="<", object=C)`.
- `A NOT = B OR C` → relational operator canônico `NOT =` na relation; nenhum logical NOT artificial.
- `A = B OR NOT C OR D` → `NegatedCondition(ContextualConditionTail(C))`; `D` é irmão posterior do `NOT`.
- `A = B OR C AND D` → `OR[A = B, AND[C, D]]`; nunca lista plana.
- `(A = B OR C) AND D` → `GroupedCondition(OR[A = B, Tail(C)])` fora do qual `D` inicia condition própria.
- `A = (B OR C) AND D` → `RelationCondition(A, =, DistributedOperandGroup[B, OR, C])`, distinguível do grupo anterior.
- `A = B OR C = D OR E` → relations completas `A = B` e `C = D`; `E` como tail contextual.
- `A = B OR C IS NUMERIC OR D` → `ClassCondition(C, NUMERIC)` distinta; `D` preservado após ela.
- Caso de baixa ambígua não muda com a caixa; alpha-rename preserva a topology; grouping explícito só adiciona o node de grupo correspondente.

## Comportamento diante de incerteza

- Bare nominal em posição com estado de abbreviation estruturalmente aberto: sempre `ContextualConditionTail`; a AST não decide CONDITION/DATA/INDEX.
- Bare nominal em posição sem estado aberto (início de condição, após `)` de grupo, após class condition): mantém a shape de referência nominal existente (uso estrutural de condition-name), sem nova categoria neste slice; a estrutura completa de condition-name pertence ao Slice 4.
- Forma aceita pela grammar sem suporte modelado neste slice: `PreservedExpression` fail-closed, com texto e referências reconhecidas.

## Fora de escopo

Binding nominal, lookup, scope resolution, `ConditionSemantics`, `ConditionValidation`, type checking, occurrences contextuais (`admissibleKinds`), estrutura completa de condition-name (qualification/subscripts específicos), `SEARCH WHEN`, CFG/dataflow, alteração de grammar, alteração semântica de `ReferenceOccurrenceCollector`/`ReferenceOccurrences`/resolvers.

## Regras de domínio relacionadas

`docs/domain/conditional-expressions.md` (autoridade IBM registrada), `docs/domain/semantic-ast.md`, `docs/domain/provenance.md`, `docs/evals/conditional-expression-oracles.md` (COND-P01/P02/P03/P04/P05/P09/P11, COND-N01/N02/N06/N07/N08/N09, COND-A01 a COND-A05, COND-A11 a COND-A13).

## ADRs/invariantes relacionados

ADR-0012 (Accepted), ADR-0003, ADR-0005, ADR-0009; INV-AST-001, INV-AST-002, INV-AST-003, INV-PROV-002, INV-COND-001, INV-COND-002, INV-DET-001, INV-PERF-001.
