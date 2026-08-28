# Conflitos e tensões identificados — migração Harness v1

Status: transitório. Um conflito permanece visível até receber resolução
baseada em evidência. A ausência de conflito aberto não autoriza exclusões antes
da matriz estar completa.

| ID | Fontes | Tensão observada | Evidência atual | Tratamento inicial | Estado |
| --- | --- | --- | --- | --- | --- |
| CONFLICT-001 | `specs/semantic-model-hardening-tasklist.md` §Estado; `specs/reference-resolution-tasklist.md`; plano Harness v1 | O plano de hardening dizia que a tasklist de resolução deveria ficar byte a byte inalterada até retomada explícita; o Harness v1 determina que tasklists concluídas sejam migradas e removidas após matriz completa. | A tasklist de resolução está marcada concluída e há commits/relatórios posteriores de hardening. O pedido atual aprova explicitamente o Harness v1. | Tratar a proteção como regra de escopo da execução histórica, não como contrato atual. Manter a fonte intacta até o gate de remoção; então migrar por matriz. | RESOLVED_BY_SUPERSEDING_WORK |
| CONFLICT-002 | `README.md`; `specs/cbstm03d-dynamic-calls-tasklist.md` | O README descreve quatro artefatos HTML; um critério histórico da variante menciona “três HTMLs”. | O script de regressão atual exige quatro páginas: `index`, `ast`, `symbols` e `resolution`. | Usar o script e o README atual como evidência operacional; classificar a formulação histórica como evidência desatualizada. | RESOLVED_BY_CURRENT_EXECUTABLE_EVIDENCE |
| CONFLICT-003 | `specs/AGENTS.md — antlr-parse-tree-explorer.md`; código, specs e reports | O documento importado contém recomendações abrangentes, mas não foi instrução operacional histórica. | As Fases 2–3 confrontaram cada família material com código, contratos, testes e fontes de domínio. | Conteúdo de engenharia foi migrado para políticas, pipeline, domain docs, invariantes e ADRs sem atribuir autoridade histórica ao documento importado. | RESOLVED_BY_VALIDATION |
| CONFLICT-004 | `specs/antlr-parse-tree-explorer-logging-tasklist.md` fases 6–8 | Há checkboxes de commit sem hash, condicionados à existência de mudança de código/configuração. | As fases registram explicitamente que não houve ajuste em alguns casos; o histórico contém commit documental final. | Não criar backlog artificial. Classificar cada checkbox como trabalho transitório concluído ou evidência histórica após validação. | OPEN_VALIDATION_REQUIRED |
| CONFLICT-005 | `README.md`; relatórios históricos | Métricas do corpus aparecem em vários documentos e em momentos diferentes. | O corpus e artefatos são evidência; o próprio README afirma que números não definem semântica. | Não consolidar métricas como regra normativa. Manter só baselines que tenham oracle executável ou valor histórico. | OPEN_CLASSIFICATION_REQUIRED |

## Decisões da grill já aplicáveis

- `WORK-HARNESS-001` será criado somente na Fase 9.
- Gates terão pirâmide incremental: fast estrutural, semantic Maven/semântico,
  full agregando cenário E2E e nomenclatura.
- Enforcement arquitetural usará teste Java próprio baseado em dependências de
  bytecode; há backlog futuro para fronteiras por pacotes/Clean Architecture.
- Catálogo de evals será Markdown canônico nesta versão.
- Performance será validada por propriedades algorítmicas/determinismo, sem
  threshold dependente de hardware.
- O plano-base será arquivado ao final em `docs/history/harness-v1-migration/`.
- `check-docs` será um script fino que delega a teste Java.
