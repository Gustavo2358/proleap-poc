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

## Roteamento

| Pergunta ou tarefa | Contexto canônico |
| --- | --- |
| mapa do pipeline, responsabilidades e dependências | [architecture/](architecture/index.md) |
| decisão e racional arquitetural | [architecture/decisions/](architecture/decisions/index.md) |
| fronteira que não pode regredir | [architecture/invariants.md](architecture/invariants.md) |
| formato de fonte, preprocessing e provenance | [domain/](domain/index.md) |
| AST, unidades, símbolos e resolução nominal | [domain/](domain/index.md) |
| política para mudança semântica, testes, desempenho ou observabilidade | [engineering/](engineering/index.md) |
| oracle, teste, fixture ou cenário de regressão | [evals/](evals/index.md) |
| trabalho ativo, histórico curto ou backlog | [work/](work/index.md) |
| decisão antiga, relatório, baseline ou matriz de migração | [history/](history/index.md), somente quando necessário |

Os work items ativos estão no [índice de trabalho](work/index.md). `WORK-AST-002` mantém seu Slice 2 no checkpoint de Discovery do PR #13; implementar F-02 continua dependendo de merge/review e autorização explícita posterior. `WORK-COND-002` promove somente o Slice 2 arquitetural de `BACKLOG-COND-001`: compara representações e registra o checkpoint pós-binding, sem implementação. `WORK-COND-001` foi concluído pelo PR #15 e possui apenas um [resumo histórico](work/history/WORK-COND-001.md). `WORK-AST-003` foi concluído pelos PRs #11/#12 e possui apenas um [resumo histórico](work/history/WORK-AST-003.md).

## Fronteira entre famílias documentais

| Família | Responde a | Não deve conter |
| --- | --- | --- |
| `architecture/` | por que e como os principais componentes se relacionam | diário de implementação ou regra COBOL completa |
| `domain/` | como um subsistema funciona hoje, seus limites e incertezas | plano futuro ou narrativa de tasklist |
| `engineering/` | como implementar e avaliar trabalho com rigor | contrato detalhado de cada domínio |
| `evals/` | qual teste/fixture prova qual capacidade | cópia integral de asserts |
| `history/` | qual evidência levou a uma decisão ou baseline | norma atual por padrão |

Não carregar `history/` por padrão. Use-o apenas quando uma fonte canônica, um work item ou uma investigação exigir evidência histórica.
