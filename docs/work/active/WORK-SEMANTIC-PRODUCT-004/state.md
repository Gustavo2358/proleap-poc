# Estado

## Onde estamos

Implementação concluída para review; branch `feat/semantic-product-entry-goback`.
Baseline main/origin main `7a376f33f55127f53c63b86d3228671b9c6a348d` (PR #30),
worktree inicial limpo. Escopo autorizado: sub-slices SP-005/SP-003 descritos na spec.

## Verde conhecido

Fast, architecture, semantic, performance e full passaram. Suíte final: 532
testes, 0 failures, 0 errors, 1 skip preexistente (`semantic.condition.required`
ausente). Novos: 20 testes do slice + 1 guarda arquitetural. E2E: 1 teste Node,
0 falhas/skips, invariantes dos artefatos e naming verdes. REDs e migrações em eval.
Diff integral revisado, sem grammar/manifest changes, AIR/air-java/CFG ou heurística
textual; INV-SP-007/EVAL-SP-004 e JSON 1.1.0 documentados.

## Restante

Revisão humana deste checkpoint; sem merge, auto-merge ou início de slices adjacentes.

## Descobertas que afetam o plano

Ast.GobackStatement e metadata não-node de Division fornecem a autoridade
canônica. Declaratives não materializados mantêm inventário parcial/start
indisponível. Runner agora escreve o nome canônico e preserva alias antigo.
