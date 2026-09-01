# WORK-AST-003 — Consistência global entre IDs e traversal da AST

Status: concluído em 2026-09-01. Risco: alto.

O Discovery do PR #11 confirmou duas causas para a divergência entre `Ast.Meta.id` e o pre-order canônico de `Ast.children`: ordem de alocação invertida em `buildPerform` e consumo do contador estrutural por metadata diagnóstica de `declarationVisibility`.

O PR #12 implementou a correção localizada, preservou metadata e produtos posteriores, promoveu o oracle estrutural ao gate normal e consolidou o contrato em `INV-AST-003`, `EVAL-AST-005` e `docs/domain/semantic-ast.md`. Não foi introduzida reindexação pós-build nem um novo passe de produção; o oracle permanece `O(nodes)`.

Com o merge do PR #12, WORK-AST-003 deixou de bloquear WORK-AST-002. Esse encerramento não autoriza implementar F-02, CFG ou dataflow; o Slice 2 de WORK-AST-002 continua submetido ao seu próprio Discovery, review e autorização.
