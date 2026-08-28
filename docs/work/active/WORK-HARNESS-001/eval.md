# Eval — WORK-HARNESS-001

## O que prova corretude

`HarnessDocsTest`, `check-fast`, `check-semantic` e `check-full` provam a integridade documental, as fronteiras já automatizadas e a preservação dos oráculos existentes.

## Classes positivas

Rota a partir do work item chega a documentos existentes; IDs relacionados existem; gates declarados são executáveis; fontes migradas têm destino na matriz.

## Classes negativas

Link interno quebrado, ID inexistente, campo obrigatório ausente, gate desconhecido ou work item incompleto falham no check documental.

## Classes ambíguas

Uma fonte legada com classificação `UNCERTAIN` não é removida; ela permanece no escopo até receber destino explícito.

## Casos adversariais

Remover prematuramente fonte ainda referenciada, usar tarefa histórica como contrato atual ou criar um work item sem oracle/gate devem ser rejeitados pelo protocolo ou pelo check documental.

## Casos de regressão

EVAL-ARCH-001 preserva a fronteira AST/símbolos; EVAL-PROV-002 e o full gate preservam o cenário E2E já existente enquanto a documentação é reorganizada.

## Propriedades/relações metamórficas

Reorganizar rotas e textos explicativos não altera os produtos semânticos, fixtures ou baselines executáveis; somente referências canônicas e checks documentais mudam.

## Expectativas de escala

O check documental percorre os documentos e work items do repositório de forma determinística. Não há threshold temporal dependente de hardware.
