# Especificação — occurrences contextuais de condições

## Problema

O collector ainda transforma `Meta.origin.grammarRule == conditionNameReference` em `CONDITION/{CONDITION}`. A branch sintática não prova namespace e causa falso gap quando um bare tail de abbreviated combined relation resolve para DATA, INDEX ou RENAMES. O Discovery deve fechar a política por posição na surface AST e não autoriza produção.

## Objetivo

Definir uma projeção estrutural de usos nominais escritos para uma única occurrence por `DataReference`, com `kind` como hint de superfície e `admissibleKinds` como universo nominal pré-binding, mantendo resolução e especialização de predicates em suas fases próprias.

## Domínio de entrada suportado

Condições já tipadas por `LogicalCondition`, `GroupedCondition`, `RelationCondition`, `NegatedCondition`, `ContextualConditionTail`, `DistributedOperandGroup` e `ClassCondition`, nos slots atuais de IF, EVALUATE e PERFORM. SEARCH WHEN permanece fora.

## Classes semânticas

Discovery em andamento. A matriz final distinguirá condição simples standalone, operando relacional, tail contextual, grupo distribuído, boundary de grupo, qualification e subscript.

## Premissas

- `LANGUAGE_GUARANTEED`: uma condition-name é simple condition; bare DATA/INDEX/RENAMES sozinho não é.
- `LANGUAGE_GUARANTEED`: abbreviated relation herda subject/operator até os terminadores IBM.
- `ARCHITECTURE_GUARANTEED`: AST, occurrences, resolution e futura ConditionSemantics são produtos separados.
- `UNCERTAIN`: nenhuma; gaps encontrados durante o checkpoint serão mantidos explícitos.

## Comportamento esperado

Será fechado ao término do Discovery a partir da norma IBM, AST atual, cadeia de consumers e oracles.

## Comportamento diante de incerteza

Binding ausente ou ambíguo permanece `UNRESOLVED`/`AMBIGUOUS`; o collector não consulta símbolos e não duplica occurrences para representar alternativas.

## Fora de escopo

ConditionSemantics, ConditionValidation, CFG, dataflow, materialização de elementos herdados, SEARCH WHEN, BACKLOG-RES-004 e qualquer mudança de resolver.

## Regras de domínio relacionadas

- `docs/domain/conditional-expressions.md`
- `docs/domain/semantic-ast.md`
- `docs/domain/reference-resolution.md`
- `docs/domain/symbol-model.md`
- `docs/domain/provenance.md`

## ADRs/invariantes relacionados

ADR-0012; INV-AST-001/002/003, INV-COND-001/002, INV-SYM-001, INV-PROV-002, INV-RES-001, INV-DET-001 e INV-PERF-001.
