# plan

## Fatiamento

RED dynamic X8/literal → produção mínima → GREEN → regressão → adversariais → segundo GREEN → gates → commit/push/PR Draft → review humano.

## Dependências

Baseline 8722945cc4cd2052c6091533f6ee6989278aa2f8; discovery analysis-cfg c39a92f930b1c693857a0b30a1f5155f3f81520c. Consumer congelado apenas probe read-only.

## Superfície arquitetural provável

Builder preserva cláusulas e relações estruturais. ScalarMoveSemantics prova acesso CALL e fitting a partir de declarations/occurrences. Projector traduz; modelo valida; writer publica 1.3.0. Índices O(N+R+L+valores produzidos), sem scan por CALL.

## Migrações requeridas

Uma versão corrente 1.3.0, sem dual writer. INTERNAL-CONTRACT-DEV-001 substitui o STOP anterior de incompatibilidade. W1C deverá atualizar consumer e pin; E2E permanece incompatível até lá. Lifecycle WORK-AST-004 arquivado após merge confirmado do PR #33.

## Artefatos esperados

Código, oracles, invariantes/domínio, evidência RED/green/challenges/gates, recibo remoto HEAD/tree, PR Draft.
