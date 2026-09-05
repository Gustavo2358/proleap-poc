# Estado

## Onde estamos

Work item permanece `active`, risco `high`. CP1 está concluído. CP2 foi aprovado
e mergeado no PR #24 (`a64c3b3`). Checkpoint 3A foi aprovado e mergeado no PR
#25. Checkpoint 3B foi autorizado e executado no PR #26; aguarda review humano.
Qualquer trabalho posterior continua não autorizado.

## Verde conhecido

- `main` foi atualizado ao merge do CP2, PR #24 (`a64c3b3`), antes da abertura da branch deste checkpoint.
- O relatório do Checkpoint 1 foi atualizado na segunda remediation adversarial, incluindo F-SP-006/F-SP-007, a matriz refinada e a self-validation; os gates docs/fast/architecture/semantic/full passaram e estão registrados no relatório.
- Não houve alteração em `src/main/**`, gramática, AST, símbolos, occurrences, resolução ou fixtures.
- O Checkpoint 2 aprovou, após o strongest-opponent test, uma boundary híbrida: estado semântico COBOL-specific, próprio, imutável e materializado, exposto ao lowerer por facade/port tipado e somente leitura; o relatório compara A2, B e envelope, explicita a decisão de analysis context, a matriz de suficiência e a reavaliação dos oito findings.
- Gates `docs`, `architecture`, `fast`, `semantic`, `performance` e `full` passaram; `git diff --check` passou.
- O slice experimental do Checkpoint 3A, restrito a `CALL` literal e aos testes
  adversariais de closure, no leakage e ausência de semantic reparsing, foi
  aprovado e mergeado no PR #25.
- O Checkpoint 3B executa, em código test-only, a falsificação da boundary para
  o slice `MOVE` → `CALL`; o PR #26 aguarda review.

## Restante

- Review humano do Checkpoint 3B.
- Incorporar findings adicionais caso existam, classificando findings semânticos
  novos pela taxonomia downstream existente.
- Não iniciar interchange, snapshot/round-trip, outros slices ou implementação
  de produção sem nova autorização; todo trabalho posterior permanece não
  autorizado.

## Descobertas que afetam o plano

O frontend não possui um objeto único que represente todos os produtos semânticos. O Checkpoint 2 recomenda um modelo materializado próprio, fechado e imutável, acessado por um port/facade de views e handles; não promove aggregate de internals ou envelope a contrato público. `ExternalClassification` já existe pós-binding; `ConditionSemantics` e `ConditionValidation` não existem em produção. Identidade composta e provenance localizada estão materializadas; persistência cross-run/cross-version continua fora do contrato provado e requer decisão posterior.
