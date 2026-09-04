# Estado — WORK-HARNESS-IMPACT-001

## Onde estamos

O work item foi criado após inspeção do harness. A fonte canônica, o protocolo,
o roteamento e a validação documental mínima foram ampliados nesta branch para
exatamente oito classes, conteúdo não vazio e lifecycle local coerente. O
`WORK-COND-007` mergeado foi arquivado antes desta revisão; este item continua
ativo e está pronto para review humano.

## Verde conhecido

- O harness possui `HarnessDocsTest` como validator existente para documentação
  e work-item YAML; não existe schema de findings a migrar.
- A taxonomia mantém exatamente oito classes, classe primária única e
  `earliest broken layer wins`.
- `UNASSESSED` é obrigatório diante de evidência insuficiente; F-01 possui um
  registro de impacto `UNASSESSED` sem alterar sua classificação de bug ou
  disposição em `BACKLOG-RES-003`.
- `BLOCKS_DEPENDENCY_FACTS` cobre fatos finais incorretos depois dos produtos
  anteriores realmente necessários; foi separado de `BLOCKS_DATAFLOW` e de
  `REDUCES_PRECISION` por exemplos conservador/unsound.
- `reassess_when` é obrigatório somente para `UNASSESSED`; quando opcional e
  presente, precisa conter uma entrada não vazia.
- `HarnessDocsTest` valida class/rationale/evidence, registros adversariais,
  completude das fronteiras e coerência active/index/history.
- Self-validation A–H passou: lifecycle/stale routing, completude de fronteiras,
  soundness versus precision, obrigatoriedade semântica dos campos, contracasos
  vazios, integridade de escopo e challenge pass.
- `check-docs.sh`, `check-fast.sh`, `check-semantic.sh` e `check-full.sh`
  passaram; `git diff --check` permanece obrigatório no handoff.
- `confidence` foi conscientemente adiada por falta de escala reproduzível.
- Nenhuma camada de produção semântica foi autorizada ou alterada.
- F-01 recebeu o único registro retroativo permitido, com impacto
  `UNASSESSED`; seu tipo `CONFIRMED_KNOWN_BUG` e sua disposição permanecem
  intactos.
- `check-docs.sh`, `check-fast.sh`, `check-semantic.sh` e `check-full.sh`
  passaram; `check-performance.sh` não é aplicável a esta mudança documental.

## Restante

- Review humano do contrato e encerramento formal do work item; não fazer merge
  automaticamente.
- A challenge pass cobriu default indevido para Semantic Product,
  precisão versus unsoundness, `UNASSESSED` versus `NOT_APPLICABLE`, separação
  de prioridade, first broken layer, simplicidade e validade futura.
- Nenhuma decisão de implementação downstream deve ser iniciada por este item.

## Descobertas que afetam o plano

- O harness não tem issue schema nem validator de findings; a extensão mínima
  apropriada é uma validação de vocabulário e de registros no documento
  canônico, sem criar um sistema genérico.
- O harness não tenta inferir merge de todos os work items: a self-validation
  verifica contradições locais observáveis entre active/index/history, enquanto
  o fechamento de WORK-COND-007 foi confirmado explicitamente pelo GitHub.
- A validação continua documental e não cria Semantic Product, Cobol Lower, IR,
  CFG, dataflow ou Dependency Facts.
