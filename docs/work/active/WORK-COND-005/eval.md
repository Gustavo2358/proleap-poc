# Avaliação

## O que prova corretude

Discovery em andamento. Os oracles finais devem distinguir política standalone, relacional e contextual sem grammarRule, preservar uma occurrence por nome escrito e provar o binding nominal com o resolver atual.

## Classes positivas

DATA, INDEX, CONDITION e RENAMES em posições admissíveis; qualification e subscripts estruturados.

## Classes negativas

Bare DATA em standalone condition, CONDITION como relation operand, leakage de contexto para qualifier/subscript e occurrences sintéticas.

## Classes ambíguas

Bindings com múltiplos candidatos válidos preservam `AMBIGUOUS`; homônimo DATA/CONDITION no mesmo programa é source IBM inválido, não oracle de precedência.

## Casos adversariais

WAUX-like tail, boundaries de parênteses, grupo distribuído, AND/OR, NOT lógico/relacional, SET/EVALUATE e SEARCH mantido fora.

## Casos de regressão

Serão detalhados após o consumer audit.

## Propriedades/relações metamórficas

A mesma surface AST de `A = B OR C` não muda quando apenas a declaração de C alterna entre DATA, INDEX, CONDITION, RENAMES e ausente.

## Expectativas de escala

Traversal estrutural linear por AST e lookup nominal indexado; nenhum scan textual ou por todas as declarações.
