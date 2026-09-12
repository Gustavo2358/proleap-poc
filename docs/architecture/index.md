# Arquitetura

Esta área descreve o mapa estável de componentes, suas fronteiras e as decisões que explicam alternativas arquiteturais relevantes.

O [pipeline atual](pipeline.md) registra as fronteiras confirmadas. O mapa curto de componentes está em [`ARCHITECTURE.md`](../../ARCHITECTURE.md), as regras normativas em [invariantes](invariants.md) e o racional no [índice de ADRs](decisions/index.md).

## Rotas

- [Decisões arquiteturais](decisions/index.md): ADRs aceitos, retrospectivos ou futuros quando houver evidência suficiente.
- [Invariantes](invariants.md): fronteiras atuais, enforcement e exceções conhecidas.
- [Pipeline e readiness downstream](pipeline.md): fronteira local no Semantic
  Product JSON e ownership cross-repo de AIR, lowering e CFG.
- [Audit bilateral Semantic Product / AIR 2.0.0](semantic-product-air-v2-audit.md):
  evidências, matriz de suficiência, blockers e próximos slices.

Um ADR registra por que uma alternativa arquitetural foi escolhida. Um invariante registra o que não pode ser violado, seu enforcement e exceções conhecidas. Regras COBOL pertencem a `../domain/`.

- [Compositionality and conservative partial lowering](compositional-partial-lowering.md): permanent invariants and future construction completion contract.
