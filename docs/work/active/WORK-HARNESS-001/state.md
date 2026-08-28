# Estado — WORK-HARNESS-001

## Onde estamos

Fases 0–11 concluídas. O protocolo e o work item ativo estão instalados; `AGENTS.md` agora roteia contexto, trabalho e gates. O README foi reduzido a uma porta de entrada de uso e navegação.

## Verde conhecido

Os gates `fast`, `semantic`, `performance` e `full` estão disponíveis. O full gate passou após a Fase 8, incluindo documentação, bytecode, suíte Maven, regressão COACTUPC e naming. O check documental valida o protocolo e o roteador.

## Restante

Fechar a matriz de migração e remover/arquivar o legado elegível.

## Descobertas que afetam o plano

O check de bytecode revelou e corrigiu a dependência direta de `AstBuilder` para `SymbolTable`. O naming gate foi limitado a arquivos rastreados ou novos não ignorados, preservando atribuições e fontes históricas/transitórias fora do escopo de identidade atual.
