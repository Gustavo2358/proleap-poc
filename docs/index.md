# Índice de conhecimento

Este diretório é a memória canônica do projeto. Ele separa o contrato atual do subsistema, as decisões arquiteturais, a prática de engenharia, o trabalho em andamento e a evidência histórica.

## Modelo de autoridade

Quando fontes divergem, aplicar a seguinte ordem para a pergunta em questão:

1. Para uma regra COBOL, a fonte oficial do dialeto configurado; os documentos de domínio registram como o projeto representa essa regra.
2. Para uma escolha arquitetural interna, um ADR aceito.
3. Para uma fronteira arquitetural, `architecture/invariants.md`.
4. Para o contrato atual de um subsistema, o documento em `domain/`.
5. Para comprovação executável, o teste, fixture e gate relacionados.
6. Para uma mudança proposta, o work item ativo quando o protocolo existir.
7. Para investigação, a evidência em `history/`.

Testes, corpus e artefatos gerados são evidência executável; não definem a semântica isoladamente. Uma regra normativa possui um único texto canônico. Outros documentos devem apontar para ele em vez de repeti-lo.

## Roteamento

| Pergunta ou tarefa | Contexto canônico |
| --- | --- |
| mapa do pipeline, responsabilidades e dependências | [architecture/](architecture/index.md) |
| decisão e racional arquitetural | [architecture/decisions/](architecture/decisions/index.md) |
| fronteira que não pode regredir | `architecture/invariants.md` quando criado |
| formato de fonte, preprocessing e provenance | [domain/](domain/index.md) |
| AST, unidades, símbolos e resolução nominal | [domain/](domain/index.md) |
| política para mudança semântica, testes, desempenho ou observabilidade | [engineering/](engineering/index.md) |
| oracle, teste, fixture ou cenário de regressão | [evals/](evals/index.md) |
| decisão antiga, relatório, baseline ou matriz de migração | [history/](history/index.md), somente quando necessário |

Ainda não existe um work item ativo: o protocolo e `WORK-HARNESS-001` serão instalados na Fase 9 do plano. Até lá, `docs/_migration/` registra somente o estado transitório da migração e não é rota normal de contexto.

## Fronteira entre famílias documentais

| Família | Responde a | Não deve conter |
| --- | --- | --- |
| `architecture/` | por que e como os principais componentes se relacionam | diário de implementação ou regra COBOL completa |
| `domain/` | como um subsistema funciona hoje, seus limites e incertezas | plano futuro ou narrativa de tasklist |
| `engineering/` | como implementar e avaliar trabalho com rigor | contrato detalhado de cada domínio |
| `evals/` | qual teste/fixture prova qual capacidade | cópia integral de asserts |
| `history/` | qual evidência levou a uma decisão ou baseline | norma atual por padrão |
| `_migration/` | como o conhecimento legado está sendo classificado | contrato definitivo |

Não carregar `history/` ou `_migration/` por padrão. Use-os apenas quando uma fonte canônica, um work item ou uma investigação exigir evidência histórica.
