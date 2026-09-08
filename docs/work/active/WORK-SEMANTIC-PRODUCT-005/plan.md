# plan

## Fatiamento

1. Lifecycle e baseline. 2. Oracles e fatos canônicos E1–E4. 3. Port/JSON 1.2.0. 4. Contracasos, escala, challenges. 5. Gates, commits, PR/CI sem merge.

Remediação do review no PR #32: retirar o byte[] integral do caminho de escrita
em arquivo; comparar bytes com serialize em fixtures pequenos; exercitar write
nos probes existentes. Preservar modelo, shape 1.2.0 e challenges semânticos.
Quantificar a provenance repetida da continuação e registrar a dívida separada.

## Dependências

AstBuilder para literal/PIC/continuação; produto pós-binding para prova escalar/MOVE; projector somente transporta.

## Superfície arquitetural provável

Ast, AstBuilder, produto canônico escalar, fronteira pública, writer e oracles.

## Migrações requeridas

1.1.0 → 1.2.0 aditiva. Consumers fechados devem reconhecer a versão ou rejeitar explicitamente. Lifecycle do item 004: PR #31 mergeado confirmado no GitHub.

## Artefatos esperados

Fixture AIR-MOVE; oracle público; probes físicos/fatos; logs e hashes; documentação durável e handoff 4C.
