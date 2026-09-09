# Índice de conhecimento

Este diretório é a memória canônica do projeto. Ele separa o contrato atual do subsistema, as decisões arquiteturais, a prática de engenharia, o trabalho em andamento e a evidência histórica.

## Modelo de autoridade

Quando fontes divergem, aplicar a seguinte ordem para a pergunta em questão:

1. Para uma regra COBOL, a fonte oficial do dialeto configurado; os documentos de domínio registram como o projeto representa essa regra.
2. Para uma escolha arquitetural interna, um ADR aceito.
3. Para uma fronteira arquitetural, `architecture/invariants.md`.
4. Para o contrato atual de um subsistema, o documento em `domain/`.
5. Para comprovação executável, o teste, fixture e gate relacionados.
6. Para uma mudança proposta, o work item ativo.
7. Para investigação, a evidência em `history/`.

Testes, corpus e artefatos gerados são evidência executável; não definem a semântica isoladamente. Uma regra normativa possui um único texto canônico. Outros documentos devem apontar para ele em vez de repeti-lo.

## Fronteira do repositório

Este repositório é o frontend COBOL e termina no COBOL Semantic Product,
materializado também como `cobol-semantic-product.json`. Downstream,
`cobol-lower` será um repositório separado que consumirá esse JSON e dependerá
de `air-java`, já responsável pelo modelo/validator AIR 2.0.0. A AIR publicada
será consumida pelo repositório separado `analysis-cfg`, responsável pelo CFG.
Nenhum desses componentes downstream é implementação local deste repositório.

## Roteamento

| Pergunta ou tarefa | Contexto canônico |
| --- | --- |
| mapa do pipeline, responsabilidades e dependências | [architecture/](architecture/index.md) |
| decisão e racional arquitetural | [architecture/decisions/](architecture/decisions/index.md) |
| fronteira que não pode regredir | [architecture/invariants.md](architecture/invariants.md) |
| formato de fonte, preprocessing e provenance | [domain/](domain/index.md) |
| AST, unidades, símbolos e resolução nominal | [domain/](domain/index.md) |
| Semantic Product, lowering e readiness downstream | [contrato e matriz atuais](domain/cobol-semantic-product.md), [pipeline arquitetural](architecture/pipeline.md), [ADR-0013](architecture/decisions/0013-cobol-semantic-product-precedes-language-neutral-lowering.md) e invariantes `INV-SP-*` |
| política para mudança semântica, testes, desempenho ou observabilidade | [engineering/](engineering/index.md) |
| oracle, teste, fixture ou cenário de regressão | [evals/](evals/index.md) |
| trabalho ativo, histórico curto ou backlog | [work/](work/index.md) |
| impacto downstream de finding semântico | [classificação de impacto downstream](engineering/downstream-impact-classification.md) |
| decisão antiga, relatório, baseline ou matriz de migração | [history/](history/index.md), somente quando necessário |

Os work items ativos estão no [índice de trabalho](work/index.md). `WORK-AST-002` possui implementação de F-02 integrada pelo PR #28. `WORK-SEMANTIC-PRODUCT-003` concluiu no PR #29 o audit da boundary contra AIR 2.0.0 e possui apenas um [resumo histórico](work/history/WORK-SEMANTIC-PRODUCT-003.md); a [matriz bilateral](architecture/semantic-product-air-v2-audit.md) e os [oracles futuros](evals/semantic-product-air-v2-oracles.md) continuam canônicos. `WORK-SEMANTIC-PRODUCT-004` e `WORK-SEMANTIC-PRODUCT-005` foram concluídos pelos PRs #31/#32 e arquivados. `WORK-AST-004` implementou o fix focal aprovado de NEXT SENTENCE, com gates verdes e novo review humano antes de merge na mesma branch/work item/PR #33 Draft; os demais escopos permanecem backlog. AIR, lowering e CFG são handoffs cross-repo, não próximos trabalhos locais. `WORK-SEMANTIC-PRODUCT-002` foi concluído no PR #27 e promoveu o [contrato atual](domain/cobol-semantic-product.md). `WORK-COND-001` foi concluído pelo PR #15, `WORK-COND-002` pelo PR #16, `WORK-COND-003` pelo PR #17, `WORK-COND-004` pelo PR #18 e `WORK-COND-005` pelo PR #19; todos possuem apenas [resumos históricos](work/history/WORK-COND-001.md). `WORK-AST-003` foi concluído pelos PRs #11/#12 e possui apenas um [resumo histórico](work/history/WORK-AST-003.md).

## Fronteira entre famílias documentais

| Família | Responde a | Não deve conter |
| --- | --- | --- |
| `architecture/` | por que e como os principais componentes se relacionam | diário de implementação ou regra COBOL completa |
| `domain/` | como um subsistema funciona hoje, seus limites e incertezas | plano futuro ou narrativa de tasklist |
| `engineering/` | como implementar e avaliar trabalho com rigor | contrato detalhado de cada domínio |
| `evals/` | qual teste/fixture prova qual capacidade | cópia integral de asserts |
| `history/` | qual evidência levou a uma decisão ou baseline | norma atual por padrão |

Não carregar `history/` por padrão. Use-o apenas quando uma fonte canônica, um work item ou uma investigação exigir evidência histórica.
