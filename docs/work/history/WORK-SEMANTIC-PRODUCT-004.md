# WORK-SEMANTIC-PRODUCT-004 — Entry primária e saída local GOBACK

Implementado e mergeado pelo PR #31 em 2026-09-06, confirmado no GitHub durante
lifecycle hygiene do 4A. Contrato 1.1.0: entry primária/start/signature e
GOBACK CURRENT_PROGRAM_INVOCATION/NONE; entry inventory permanece PARTIAL.

Conhecimento durável: [contrato](../../domain/cobol-semantic-product.md),
INV-SP-007 e EVAL-SP-004. Oracles: SemanticProductEntryGobackTest e probe público.
Fast, semantic, performance e full passaram no fechamento: 532 testes,
zero falhas/erros, um skip preexistente; E2E/naming verdes. O restante de
SP-003/SP-005 e efeitos/lifecycle continuou fora do slice.
