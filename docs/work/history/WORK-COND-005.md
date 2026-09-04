# WORK-COND-005 — Contextualizar occurrences de condições

Status: concluído em 2026-09-03 no PR #19. Risco: alto.

O Slice 5 de `BACKLOG-COND-001` corrigiu o bug em que `A = B OR C` era coletado como `CONDITION/{CONDITION}` por influência de `conditionNameReference`, impedindo o binding de DATA, INDEX e RENAMES admissíveis.

## Decisão e resultado

Discovery Rounds 1–3 fecharam F1–F5: a surface AST permanece pre-binding; occurrences são derivadas de posição tipada e shape nominal; o resolver continua nominal; `ConditionSemantics`/`ConditionValidation` permanecem futuros, conforme ADR-0012. A implementação usa `indexAdmissibleNominalShape(ref)` com qualifiers vazios, subscript groups vazios e `referenceModification == null`; relation/distributed bare admitem `DATA/INDEX`, demais shapes apenas `DATA`; contextual tails unem essa policy a `CONDITION`, mantendo `CONDITION` como primary kind. Uma referência escrita continua gerando uma occurrence.

`conditionNameReference` passou a `CONTEXTUAL_REFERENCE_ORIGIN` com `referenceKind == null`; `ReferenceResolutionManifest` está na versão `1.1.0`. `PerformControl` preserva `VALUE`/`CONDITION` em metadata tipada, sem node/ID adicional ou alteração de pre-order. Diagnostics contextuais usam `CONTEXTUAL_CONDITION`. `grammarRule` permanece somente provenance/coverage/diagnostic metadata.

## Evidência

- TDD RED antes da produção e GREEN após a implementação em `ContextualConditionOccurrenceTest`;
- testes normativos e adversariais para DATA, INDEX, CONDITION, RENAMES, MISSING, shapes, children, boundaries, AND/OR, NOT, distributed operands, manifesto, diagnostics e PERFORM;
- regressão de closure WAUX-like: `IF INPUT-CODE = X1 OR X2 OR X3 OR X4`, com X1–X4 DATA, todos os tails resolvidos como DATA, zero `INVALID_NAMESPACE_FOR_CONTEXT` e cardinalidade um-para-um;
- regressões de AST, resolução, relatório, snapshots e CICS aprovadas; SET/EVALUATE permaneceram inalterados;
- gates `fast`, `semantic`, `performance` e `full` passaram.

## Dependências futuras

O PR #19 permanece sem merge até review humano final. O Slice 6 (`SEARCH WHEN`) e o Slice 7 (regressão ampla de corpus) continuam pendentes. `BACKLOG-RES-004` permanece aberto e separado. `ConditionSemantics` e `ConditionValidation`, além de CFG/dataflow, continuam trabalhos futuros.
