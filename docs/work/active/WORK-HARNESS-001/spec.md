# Spec — WORK-HARNESS-001

## Problema

O conhecimento do harness foi migrado por fases, mas ainda faltam o roteador de contexto, a redução do README e a remoção/arquivamento controlado das fontes legadas.

## Objetivo

Concluir o Harness Engineering v1 com rotas curtas para trabalho ativo, documentação canônica e gates verificáveis, sem redefinir a arquitetura ou as regras COBOL já consolidadas.

## Domínio de entrada suportado

Documentação, scripts do harness, checks documentais e arquivos de roteamento do repositório no corte atual.

## Classes semânticas

Conhecimento canônico, evidência executável, trabalho transitório, backlog e evidência histórica permanecem categorias distintas.

## Premissas

O plano em `specs/HARNESS_ENGINEERING_IMPLEMENTATION_PLAN.md` é a baseline da migração até seu arquivamento na conclusão. As Fases 0–8 já estão registradas em `docs/_migration/` e nos commits correspondentes.

## Comportamento esperado

Um agente encontra o work item ativo, os documentos mínimos, os evals e os gates sem carregar tasklists ou relatórios históricos por padrão. Cada fonte legada removida ou arquivada terá classificação e destino verificáveis.

## Comportamento diante de incerteza

Ambiguidade de migração, link pendente ou item não classificado bloqueia a remoção. Nenhuma lacuna é convertida em conhecimento canônico por conveniência.

## Fora de escopo

Refatoração por pacotes/Clean Architecture, CFG, dataflow, análise de linguagens embarcadas, novas regras COBOL e alteração de oráculos semânticos.

## Regras de domínio relacionadas

`docs/architecture/pipeline.md` e `docs/domain/index.md` definem as fronteiras que o harness deve preservar ao roteá-las.

## ADRs/invariantes relacionados

ADR-0003, ADR-0005, INV-AST-001 e INV-DET-001.
