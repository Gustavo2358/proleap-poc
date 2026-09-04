# Estado

## Onde estamos

Work item permanece `active`, risco `high`. Checkpoint 2 concluído e aguardando review humano. Checkpoint 3 NÃO autorizado e não iniciado.

## Verde conhecido

- `main` foi atualizado ao merge do PR #23 (`e293a25`) antes da abertura da branch deste checkpoint.
- O relatório do Checkpoint 1 foi atualizado na segunda remediation adversarial, incluindo F-SP-006/F-SP-007, a matriz refinada e a self-validation; os gates docs/fast/architecture/semantic/full passaram e estão registrados no relatório.
- Não houve alteração em `src/main/**`, gramática, AST, símbolos, occurrences, resolução ou fixtures.
- O Checkpoint 2 recomenda, após o strongest-opponent test, uma boundary híbrida: estado semântico COBOL-specific, próprio, imutável e materializado, exposto ao lowerer por facade/port tipado e somente leitura; o relatório compara A2, B e envelope, explicita a decisão de analysis context, a matriz de suficiência e a reavaliação dos oito findings.
- Gates `docs`, `architecture`, `fast`, `semantic`, `performance` e `full` passaram; `git diff --check` passou.

## Restante

- Review humano do Checkpoint 2.
- Incorporar findings adicionais caso existam.
- Obter autorização explícita antes do Checkpoint 3; este checkpoint não o autoriza.

## Descobertas que afetam o plano

O frontend não possui um objeto único que represente todos os produtos semânticos. O Checkpoint 2 recomenda um modelo materializado próprio, fechado e imutável, acessado por um port/facade de views e handles; não promove aggregate de internals ou envelope a contrato público. `ExternalClassification` já existe pós-binding; `ConditionSemantics` e `ConditionValidation` não existem em produção. Identidade composta e provenance localizada estão materializadas; persistência cross-run/cross-version continua fora do contrato provado e requer decisão posterior.
