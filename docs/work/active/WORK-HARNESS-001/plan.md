# Plano — WORK-HARNESS-001

## Fatiamento

1. Instalar o protocolo e o exemplo ativo (Fase 9).
2. Instalar o novo `AGENTS.md` como roteador curto (Fase 10).
3. Reduzir o README a uso humano e navegação, sem duplicar contratos (Fase 11).
4. Executar a matriz final, remover ou arquivar fontes legadas e registrar o encerramento (Fase 12).

## Dependências

Taxonomia, políticas, ADRs, domínios, eval catalog, gates e enforcement das Fases 0–8 devem continuar íntegros. A remoção depende de matriz sem itens incertos e do roteador novo existir.

## Superfície arquitetural provável

`AGENTS.md`, `README.md`, `docs/`, `scripts/harness/` e `HarnessDocsTest`; nenhuma fase do pipeline COBOL deve ser redesenhada.

## Migrações requeridas

Atualizar rotas canônicas, retirar duplicação do README e classificar/remover/arquivar fontes em `specs/` somente quando os critérios de saída estiverem satisfeitos.

## Artefatos esperados

Novo `AGENTS.md`, README enxuto, matriz final preservada em histórico, fontes legadas sem referências canônicas e resumo histórico do work item na conclusão.
