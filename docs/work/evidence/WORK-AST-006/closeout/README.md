# CP6 W2A — fechamento formal

**WORK-AST-006: completed / APPROVED / MERGED / CLOSED**.
**W2A BASELINE — FROZEN**. [Registro do baseline](baseline.json).
[Histórico do work item](../../../history/WORK-AST-006.md).

## Merge e autoridade

Aprovação humana explícita nesta tarefa, sem blockers. Conferência remota não
observou novos commits, reviews, comentários ou findings. PR #35 estava OPEN /
DRAFT, base main, mergeable true, auto-merge null e CI SUCCESS no source aprovado.
Foi marcado Ready sem alterar o corpo e mergeado por `gh pr merge --merge
--match-head-commit a86be664bf099ab7d8b9fc02794369391702f000`. Sem rebase, squash, force-push ou auto-merge.

| Identidade | Valor |
| --- | --- |
| Source branch histórica | feat/cp6-w2a-if-semantic-facts |
| Source HEAD qualificado | `a86be664bf099ab7d8b9fc02794369391702f000` |
| Source tree qualificada | `5880e174b33c85ba3f3cdbc70bd2d8dc7b1d567b` |
| PR | #35 — MERGED |
| Método | merge commit |
| Merge SHA / main na integração produtiva | `4ffabded1aad39316b8a6f337f732976fdb3ca3e` |
| Merge timestamp UTC | 2026-09-12T12:39:11Z |
| main tree na integração produtiva | `5880e174b33c85ba3f3cdbc70bd2d8dc7b1d567b` |
| Comparação | **QUALIFIED_TREE_PRESERVED** |
| SP final | **1.4.0** |
| CI da qualificação | run 34692974921 — SUCCESS no source HEAD acima |

A main local foi atualizada por fast-forward. O merge tem o source aprovado como
segundo pai e preserva byte a byte a tree qualificada. O novo pin upstream para
uma futura W2C é `4ffabded1aad39316b8a6f337f732976fdb3ca3e`; W1A continua histórico.
A source branch fica preservada como referência histórica, sem novos commits;
a configuração remota `delete_branch_on_merge=false` não manda removê-la.

O commit administrativo posterior contém somente lifecycle/documentação e seus
recibos. Pode mudar a tree global de main por esses arquivos, sem alterar produto,
testes, harness ou conteúdo normativo. A única mudança no contrato IF é o link
para o work item agora arquivado. A identidade final desse commit é registrada
no handoff pós-commit, evitando SHA autorreferente. A autoridade produtiva permanece
a integração imutável acima; a revisão/Full/challenges não migram de source SHA.

## Verificação e preservação

`./scripts/harness/check-fast.sh` pós-merge: PASS, exit 0, docs/architecture e
compilação Maven básica. Somente fast e naming são usados para o closeout
administrativo. Nenhuma Full Qualification local foi repetida; a autorização de
fechamento manda reutilizar a qualificação da tree preservada. O workflow remoto
existente pode executar automaticamente em pushes de main; nenhuma execução
pesada foi solicitada manualmente e nenhum workflow foi alterado.

RED A–D, primeiro GREEN, tentativas, mutações, restored GREEN, CI e W1 histórico
permanecem intactos. Os cinco arquivos do work item revisado estão copiados byte
a byte em `work-item-reviewed/` e foram retirados de active conforme o protocolo.
Os antigos receipts são snapshots anteriores ao merge; suas identidades/claims
não foram reescritas. No README anterior, somente o link active→history mudou;
seus bytes originais estão em `reviewed-handoff.md.txt`. O primeiro fast
administrativo detectou esse link quebrado; log/exit 1 foram preservados e a
correção é apenas navegação documental. O estado atual é este closeout.
Fast administrativo após correção: PASS, exit 0; docs e architecture verdes.
Logs/metadata deste fechamento são novos, com hashes e gzip sem perda, em `raw/`
e no [manifesto](raw-manifest.json).

Os siblings air-java, cobol-lower, analysis-cfg e analysis-ir estavam limpos;
artefatos-e2e já tinha checkpoint-4e/ e cp5/ não rastreados. Todos os estados
preexistentes foram preservados. Nenhuma branch W2C criada nem pin downstream alterado.

## Entrega congelada e limites

Completion: MOVE interno → próximo MOVE do braço; último MOVE → successor do IF;
IF → successor externo. Derivação por estrutura canônica e successor herdado,
sem StatementId, line number, programPoint ou posição física global como ordem.

Predicate admitido: resultDomain BOOLEAN, evaluation PURE, normalCompletion TOTAL,
readsCompleteness COMPLETE, truthValue UNKNOWN; leitura whole-item resolvida.
**predicate truth value is NOT evaluated**. A expressão `FLAG = 'Y'` não foi avaliada.

ELSE ABSENT permanece distinto de ELSE PRESENT vazio/parcial.
**Option A — IndependentStorageSet**: **storage independence is source-derived
evidence, never inferred from distinct IDs.** Conjunto conhecido somente na slice
conservadora publicada de WORKING-STORAGE. Nested IF mantém ownership/completion,
mas continua fora da admissão produtiva completa.

## Handoff documental, sem início

W2C ainda precisará, sob autorização própria, do transporte AIR no air-java:
Operations.Branch, Operations.Jump, Expressions.Unknown, Types.Known(BOOL), known
predicate reads/origin/reason, Premise e DisjointStorage. Nada disso foi implementado,
nenhum RED/branch/PR W2C foi criado e nenhum codec/lower/pin downstream foi alterado.

**CP6 W2A — APPROVED / MERGED / CLOSED**.
**W2A BASELINE — FROZEN**.
**W2C — NOT_STARTED / NOT_AUTHORIZED**.
**W2B — NOT_STARTED / NOT_AUTHORIZED**.
**W2D — NOT_STARTED / NOT_AUTHORIZED**.
