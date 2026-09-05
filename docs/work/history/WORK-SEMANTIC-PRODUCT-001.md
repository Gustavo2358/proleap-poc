# WORK-SEMANTIC-PRODUCT-001 — Semantic Product Boundary Discovery

## Resultado

Concluído pelo PR #26, que encerrou o Discovery após o merge do Checkpoint 3B.
O Checkpoint 1 estabeleceu a baseline factual; o Checkpoint 2 aprovou a
boundary H; o Checkpoint 3A aprovou a falsificação test-only para `CALL` literal
no PR #25; e o Checkpoint 3B aprovou a falsificação test-only para `MOVE` literal
→ `CALL` variável no PR #26.

## Conhecimento preservado

- A menor boundary provada é um estado semântico COBOL-specific, próprio,
  materializado, imutável, partial-aware e namespaced (A2), exposto por um
  port/facade tipado, fechado e somente leitura (B).
- O futuro `CobolLower` deve consumir somente o port tipado; AST, symbols,
  occurrences, resolution, ANTLR, snapshots e composition root não atravessam
  essa fronteira.
- Binding nominal, ordering observado, provenance e incompletude permanecem
  separados; `CALL` variável não autoriza inferir target de runtime.
- Não foram autorizados no Discovery Semantic Product de produção, JSON,
  interchange, round-trip, persistência, IR, CFG, dataflow ou generalização
  multi-language.

Os relatórios detalhados dos Checkpoints 2, 3A e 3B permanecem como evidência:

- [Checkpoint 2](../../history/evidence/semantic-product-boundary-checkpoint-2.md);
- [Checkpoint 3A](../../history/evidence/semantic-product-boundary-checkpoint-3a.md);
- [Checkpoint 3B](../../history/evidence/semantic-product-boundary-checkpoint-3b.md).

O diretório ativo foi removido conforme o protocolo. A implementação posterior
é roteada por `WORK-SEMANTIC-PRODUCT-002` e deve respeitar os limites acima.
