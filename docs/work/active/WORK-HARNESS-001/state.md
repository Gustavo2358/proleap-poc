# Estado — WORK-HARNESS-001

## Onde estamos

Fases 0–8 concluídas. Esta Fase 9 instala o protocolo e formaliza o work item ativo que cobre as Fases 9–12.

## Verde conhecido

Os gates `fast`, `semantic`, `performance` e `full` estão disponíveis. O full gate passou após a Fase 8, incluindo documentação, bytecode, suíte Maven, regressão COACTUPC e naming.

## Restante

Criar o novo roteador `AGENTS.md`, reduzir duplicação do README, fechar a matriz de migração e remover/arquivar o legado elegível.

## Descobertas que afetam o plano

O check de bytecode revelou e corrigiu a dependência direta de `AstBuilder` para `SymbolTable`. O naming gate foi limitado a arquivos rastreados ou novos não ignorados, preservando atribuições e fontes históricas/transitórias fora do escopo de identidade atual.
