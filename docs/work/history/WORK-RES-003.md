# WORK-RES-003 — Resolver SET de condition-name sem namespace DATA espúrio

Status: concluído em 2026-08-29. Risco: médio.

`SET condition-name TO TRUE/FALSE` agora recebe contexto semântico `SET_CONDITION_TARGET` no lowering da AST e gera occurrence `CONDITION` no coletor. A decisão usa `setToStatement` e `booleanLiteral` da parse tree; não depende de grafia achatada e não introduz binding ou inferência de valor na AST. Os outros operandos de `SET` continuam `DATA/INDEX`.

O oracle adversarial inclui `TRUE`, `FALSE`, múltiplos targets, colisão DATA/CONDITION e SET de dado/índice. O PIT focalizado em `ReferenceOccurrenceCollector` gerou 128 mutantes; 63 foram mortos, 38 não tiveram cobertura e a força de teste foi 70%. Os mutantes das branches novas CONDITION e DATA/INDEX foram mortos pelos oráculos adicionados.

Regressão dos artefatos imediatamente anteriores para os novos:

| Programa | Gaps antes | Gaps depois | Redução | Efeito confirmado |
| --- | ---: | ---: | ---: | --- |
| COACTUPC | 1.470 | 1.245 | 225 | `INVALID_NAMESPACE_FOR_CONTEXT`: 333 → 108 |
| CBSTM03A | 345 | 268 | 77 | `INVALID_NAMESPACE_FOR_CONTEXT`: 77 → 0 |
| CBSTM03D | 345 | 268 | 77 | `INVALID_NAMESPACE_FOR_CONTEXT`: 77 → 0 |

Em CBSTM03A e CBSTM03D restam 37 gaps de frontend coverage, 122 containers preservados, 95 formas de gramática sem suporte e 14 gaps de CALL. Em COACTUPC permanecem 1.118 declarações ausentes, 108 `INVALID_NAMESPACE_FOR_CONTEXT`, 14 CICS opacos, 2 ambiguidades, 1 COPY ausente, 1 relation namespace inválido e 1 CALL linkage desconhecido. Os 108 casos restantes são `WHEN condition-name` (`VALUE_READ`/`qualifiedDataName`), uma classe gramatical diferente que não foi expandida silenciosamente neste work item.

Evidências: `AstBuilderTypedTraversalTest`, `DataAndIndexReferenceResolverTest`, perfil `mutation-adversarial` focalizado, `check-full` e dists regeneradas.
