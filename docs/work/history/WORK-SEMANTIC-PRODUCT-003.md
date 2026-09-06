# WORK-SEMANTIC-PRODUCT-003 — Discovery bilateral do Semantic Product para Analysis IR 2.0.0

Status: concluído em 2026-09-06. Risco: alto.

## Resultado

O Discovery/Audit exclusivamente documental confrontou bilateralmente o
`CobolSemanticPort` com a Analysis IR 2.0.0. O PR #29 encerrou o trabalho e foi
mergeado na main em `2026-09-06T10:17:03Z`, merge commit
`001547f35cb9c658af2832e490d719194401ad45`. A implementação de integridade
cross-product F-02 já havia sido integrada pelo PR #28 em
`2026-09-06T10:05:04Z`, merge commit
`6d3400ed6247f20effb49c4547378437096a1457`, e foi revalidada como prerequisite
do audit.

A autoridade externa auditada foi AIR 2.0.0, fixada no commit
`0b2fbce7046010b22b32efa8cbc3e75ccba09442`. O port atual permite publicação
AIR válida e conservadora de parte da estrutura e do inventário, mas não contém
fatos suficientes para declarar `assign`, predicate ou `invoke` precisos nem um
primeiro CFG executável fechado.

## Conhecimento promovido

- A [matriz bilateral e os findings](../../architecture/semantic-product-air-v2-audit.md)
  permanecem documentação arquitetural canônica.
- Os [oracles AIR futuros](../../evals/semantic-product-air-v2-oracles.md)
  preservam as falsificações por checkpoint; não representam implementação.
- Pipeline, invariantes, contrato do Semantic Product, referência nominal,
  índices de arquitetura/evals e [backlog](../backlog.md) foram alinhados aos
  findings no PR #29.

## Encerramento e trabalho futuro

O checkpoint final partiu de `main`/`origin/main` em
`001547f35cb9c658af2832e490d719194401ad45`, confirmou remotamente os merges dos
PRs #28 e #29 e executou novamente os gates `fast`, `semantic` e `full`, todos
verdes. O diretório ativo foi removido conforme o protocolo do harness.

Nenhuma classe/schema AIR, `CobolLower`, enrichment do Semantic Product, CFG,
storage, effects ou dataflow foi implementado neste Discovery ou em seu
arquivamento.

No momento do audit, `BACKLOG-IR-001` e `BACKLOG-LOWER-001` representavam os
próximos handoffs conceituais da progressão IR → lowering → CFG. A ownership
física foi consolidada posteriormente: o repositório separado `air-java` já
implementa o modelo/validator Java da AIR 2.0.0; `cobol-lower` será o tradutor
separado de `cobol-semantic-product.json` para uma AIR Publication; e o
repositório separado `analysis-cfg` consome essa Publication para construir o
CFG. Esses IDs permanecem como rastreabilidade histórica/cross-repo, não como
implementação futura deste repositório.

Neste frontend, o próximo trabalho local recomendado é promover, sob nova
autorização, o enrichment mínimo de `BACKLOG-SP-005` coordenado com
`BACKLOG-SP-003`: Entry, início executável e semântica de saída/terminal para o
primeiro caso GOBACK. O objetivo downstream é permitir que o futuro
`cobol-lower` produza `Publication → Unit → Entry → Sequence → Return` a partir
desse programa mínimo. Nenhum desses itens foi iniciado neste checkpoint.
