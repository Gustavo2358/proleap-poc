# Estado

## Onde estamos

Discovery concluído na branch `codex/discovery-next-sentence-coverage`, base
main/origin/main limpa `2815e805fd3a9ef4762a39ab9435260fc76da0e8`.
**DISCOVERY COMPLETE — HUMAN REVIEW REQUIRED BEFORE IMPLEMENTATION**.
Implementação não autorizada; status blocked representa esse gate humano.

## Verde conhecido

Fast e full passaram (full inclui semantic, E2E e naming). Suíte: 566 testes,
0 failures/errors e 2 skips: um preexistente e o oracle requerido deste discovery.
Caracterização: 15 testes, 0 failures/errors, um skip; oracle requerido executado
separadamente: RED com nove contracasos. CLI NEXT: exit 1 na fase SEMANTIC_PRODUCT;
controle CONTINUE: exit 0 e JSON 1.2.0. A primeira falha de naming foi corrigida
somente no resumo documental e o full repetido passou; detalhes no eval.
Diff integral revisado, diff check limpo, source/gramática/manifestos intocados.
Self-validation local de active/index/history, links, IDs, contratos e escopo
aprovada pelo harness; lifecycle remoto confirmado para os PRs #31/#32.

## Restante

Review e decisão humanas do discovery. Handoff em PR Draft associado à branch
acima; implementação e promoção dos oracles exigem aprovação explícita e
continuarão na mesma branch, no mesmo work item e no mesmo PR. Não fazer merge
ou retirar Draft. Este checkpoint não conclui o item de implementação.

## Descobertas que afetam o plano

Causa confirmada em ifThen/ifElse/searchWhen. INV-COV-001 já existe; finalização
comum reutiliza o registro canônico. Policies por origem permanecem conservadoras.
AST preserva períodos; SP ainda não publica destino/boundary de sentence, portanto
CFG/effects/dataflow continuam bloqueados. PR #32 mergeado confirmado no GitHub;
archive de WORK-SEMANTIC-PRODUCT-005 é higiene obrigatória, sem iniciar trabalho adjacente.
