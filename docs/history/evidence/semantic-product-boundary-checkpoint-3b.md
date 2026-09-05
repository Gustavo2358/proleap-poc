# Semantic Product Boundary — Checkpoint 3B

## Hipótese

Um consumer independente consegue reconstruir o slice `01 WS-PGM PIC X(8)` /
`MOVE 'PGMA' TO WS-PGM` / `CALL WS-PGM` somente pela boundary A2+B, sem
conhecer ANTLR, AST, símbolos, occurrences, resolução ou presentation snapshots.

## Slice usado

O adapter test-only analisou uma compilation unit sintética e reutilizou os
produtos canônicos do frontend: `DataEntry`/`PictureClause`, `MoveStatement`,
`CallStatement`/`DataReference`, occurrence roles, `ReferenceResolution` e
`ResolutionAnalysisReport`. A boundary publicou estado imutável próprio com
`DataItemId` namespaced, declaração, literal, MOVE, CALL, program points,
ordering, provenance e uncertainty. O consumer recebeu somente o port
read-only.

## Oracles

| Oracle | Resultado |
| --- | --- |
| Identidade tipada/namespaced de `WS-PGM` | PASS |
| Declaração `DATA WS-PGM` e `PIC X(8)` atravessam | PASS |
| Literal escrito pelo MOVE reconstruído como `PGMA` | PASS |
| Target do MOVE e operando do CALL apontam para a mesma identidade | PASS |
| Binding nominal conhecido separado do target de runtime | PASS |
| Target de runtime do CALL permanece `UNKNOWN` | PASS |
| Incerteza `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN` localizada no CALL | PASS |
| MOVE precede CALL por ordering/program points explícitos | PASS |
| Provenance localizada para declaração, literal, MOVE e CALL | PASS |
| Consumer independente opera sem frontend, texto semântico ou grammar metadata | PASS |
| Estado/port imutáveis e fechamento após liberar o frontend | PASS |
| Nenhum lowering, CFG, dataflow ou serializer implementado | PASS |
| Teste focalizado (`SemanticProductBoundaryCheckpoint3BTest`, 5 testes) | PASS |

## Informação necessária não atravessou a boundary

Nenhuma informação necessária para este slice ficou ausente. O target final de
runtime não atravessa deliberadamente: a boundary publica `UNKNOWN` e a
incerteza localizada, preservando a separação entre binding nominal e análise de
valores futuros.

## Veredito

**PASS.** Há evidência suficiente para encerrar o discovery bloqueante do
Semantic Product para este slice e seguir para um work item separado de
implementação do primeiro vertical slice. Isso não autoriza implementação de
produção, lowering, CFG ou dataflow neste checkpoint.

## Recomendação

A boundary está suficiente para iniciar o vertical slice de produção em um novo
work item explicitamente autorizado; o próximo trabalho deve manter a mesma
separação frontend → adapter → A2/estado imutável → port → consumer e
preservar `UNKNOWN`/uncertainty até existir análise de valores autorizada.
