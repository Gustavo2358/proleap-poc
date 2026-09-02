# WORK-COND-001 — Contrato normativo de condições combinadas e abreviadas

Status: concluído em 2026-09-01. Risco: alto.

O Slice 1 de `BACKLOG-COND-001` foi concluído pelo PR #15. Ele fixou uma única edição do IBM Enterprise COBOL como autoridade, separou comportamento observado no PR #14 de regra da linguagem e fechou herança/atualização de subject e relational operator, terminadores da inserção, distribuição parentética, `NOT`, precedência, classes DATA/INDEX/CONDITION, RENAMES, qualification e scope.

O review acrescentou dois contraexemplos decisivos: uma relation completa posterior redefine o estado herdável, e qualquer nova simple condition — não somente condition-name — encerra a inserção. O antigo caso DATA+CONDITION homônimo no mesmo programa foi reclassificado como source IBM inválido, não ambiguidade de binding.

O conhecimento durável foi promovido para o [contrato canônico de expressões condicionais](../../domain/conditional-expressions.md) e os oracles `COND-*` foram preservados no [catálogo normativo](../../evals/conditional-expression-oracles.md). O relatório do PR #14 continua histórico. Este resumo não é fonte normativa e não autoriza implementação; a representação arquitetural pertence a WORK-COND-002.
