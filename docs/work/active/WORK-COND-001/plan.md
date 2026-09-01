# Plano — WORK-COND-001

## Fatiamento

Este work item promove somente o Slice 1 de `BACKLOG-COND-001` e termina no checkpoint de Discovery.

1. **S1.1 — Autoridade normativa mínima**: consolidar IBM Enterprise COBOL para abbreviated combined relation conditions, complex conditions, user-defined words, condition-name/index-name, qualification e scope.
2. **S1.2 — Matriz semântica**: transformar as regras em classes positivas, negativas, ambíguas e adversariais independentes da implementação atual.
3. **S1.3 — Reconciliação histórica**: comparar os oracles com `semantic-condition-context-discovery-report.md`, marcando divergências entre regra IBM e comportamento parser/AST sem corrigir produção.
4. **S1.4 — Checkpoint**: executar os gates declarados, registrar fatos/incertezas em `state.md`, submeter a revisão humana e parar.

Slices posteriores permanecem backlog: decisão arquitetural, lowering lossless, condition-name/subscripts, occurrences, SEARCH e regressão de produção.

## Dependências

- merge do PR #14 na `main` como evidência histórica reproduzível;
- IBM Enterprise COBOL Language Reference como autoridade para linguagem;
- `semantic-analysis-policy.md` e `semantic-testing.md` para classificação de premissas/oracles;
- `reference-resolution.md`, INV-AST-001/002, INV-SYM-001 e INV-RES-001 apenas para manter fronteiras internas, não para redefinir COBOL.

Não há dependência de uma escolha arquitetural do Slice 2.

## Superfície arquitetural provável

O Discovery do PR #14 indica que uma futura implementação poderá tocar a fronteira parse tree → AST → occurrences → resolution. Este Slice 1 não escolhe onde o contexto deve morar e não autoriza modificação nessa superfície.

As entradas de `source_scope` em `work-item.yaml` são pontos de observação da produção atual, em modo somente leitura. `must_not_change: src/main` prevalece durante todo o Slice 1.

As três famílias candidatas continuam abertas e devem ser comparadas no Slice 2 contra os mesmos oracles normativos:

- AST contextual;
- normalização semântica no lowering;
- produto semântico pós-binding.

Critérios futuros mínimos: losslessness, separação entre sintaxe/binding, preservação de ambiguity, qualification/scope, provenance, determinismo e capacidade de representar precedence/boundaries sem heurística textual.

## Migrações requeridas

Nenhuma migração de produção, snapshot, manifesto, grammar ou arquitetura é autorizada neste slice.

Se os oracles documentais revelarem que uma fixture histórica representa COBOL inválido — em especial DATA + CONDITION com a mesma user-defined word no mesmo programa — a fixture permanece evidência histórica até uma implementação autorizada decidir se deve ser substituída ou convertida em teste negativo de frontend. Não alterar baseline apenas para alinhar contagem.

## Artefatos esperados

- `work-item.yaml`, `spec.md`, `plan.md`, `eval.md` e `state.md` em `docs/work/active/WORK-COND-001/`;
- índice de trabalho atualizado para tornar o item visível;
- matriz de oracles normativa no `eval.md`;
- registro explícito de que o sentinel DATA+CONDITION do relatório histórico é input IBM inválido no mesmo programa;
- nenhum arquivo de produção/grammar/arquitetura alterado;
- evidência dos gates `fast`, `semantic` e `full` antes de sair do checkpoint de Discovery.
