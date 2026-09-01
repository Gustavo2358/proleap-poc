# Plano

## Fatiamento

1. Confirmar `main` no merge do PR #11, registrar o SHA e criar branch exclusiva de implementação.
2. Corrigir a ordem do ramo procedure em `buildPerform` e ancorar diagnostics de visibility à metadata declarativa existente.
3. Converter caracterizações em regressões estruturais e promover o oracle para execução normal.
4. Promover o contrato para domínio, invariant e catálogo de evals.
5. Executar testes focais, `fast`, `semantic`, `full` e `git diff --check`; investigar qualquer churn.
6. Criar commit e PR de implementação separado. Não fazer merge e aguardar review.

## Dependências

- Base `47484979b666a61758539630dcf4425249c68340`, merge do PR #11.
- Contratos, policies e gates listados em `work-item.yaml`.
- Evidência histórica de WORK-RES-004 somente porque registra uma decisão anterior explicitamente motivada por pre-order.
- Review independente concluído no PR #11 e autorização explícita da Fase 2.

## Superfície arquitetural provável

A implementação é localizada em `AstBuilder`, regressões e documentação canônica. `Ast`, `AstSnapshot`, grammar e consumidores posteriores permanecem inalterados.

## Migrações requeridas

Nenhuma. A correção preserva IDs e snapshots para entradas anteriormente válidas; os três triggers antes rejeitados passam a receber a sequência canônica. Qualquer churn fora deles exige investigação.

## Artefatos esperados

- Quatro fixtures focais cobrindo dois triggers de `PERFORM`, os dois call sites diagnósticos e controles negativos.
- Regressões sem IDs hardcoded e oracle estrutural normal sobre a superfície representativa.
- INV-AST-003 e EVAL-AST-005 como contrato e proteção duráveis.
- Commit e PR exclusivos de implementação, baseados no merge do Discovery.
