# Plano — WORK-COND-002

## Fatiamento

Este work item contém somente o Slice 2 arquitetural e termina em review humano:

1. **S2.1 — Lifecycle:** promover o contrato/oracles de WORK-COND-001 para domínio/evals, criar resumo histórico e remover o diretório ativo anterior.
2. **S2.2 — Constraints:** auditar ADRs, invariants e contratos atuais de AST, symbols, occurrences, resolution, IDs, provenance, ambiguity e performance.
3. **S2.3 — Alternatives/challenge:** comparar AST contextual, normalization durante lowering e surface AST + produto pós-binding contra cada critério e contraexemplo.
4. **S2.4 — Decision record:** registrar a alternativa escolhida em ADR-0012 e INV-COND-001/002, mantendo o ADR `Proposed` até review/merge.
5. **S2.5 — Checkpoint:** executar gates, atualizar `state.md`, abrir PR e parar.

Slice 3 e todos os slices executáveis permanecem fora deste plano. Mesmo após merge, seu início exige autorização explícita e novo/atualizado work item.

## Dependências

- merge do PR #15 em `main`;
- contrato IBM e oracles `COND-*` promovidos por WORK-COND-001;
- Discovery histórico do PR #14 para a evidência de implementação;
- ADR-0002/0003/0005/0008/0009 e invariants de AST/symbols/resolution;
- INV-AST-003 para identidade/pre-order e política de performance para a futura projeção linear/indexada.

Não há dependência de alteração de grammar, código ou fixture neste slice.

## Superfície arquitetural provável

A futura implementação da decisão possui quatro responsabilidades separadas:

- `Ast`/`AstBuilder`: condition surface lossless e contextual, sem binding;
- `ReferenceOccurrences`/collector: usos escritos e admissible kinds, sem lookup nem synthetic expansion;
- `ReferenceResolution`: binding nominal existente, sem predicate reconstruction;
- novo `ConditionSemantics`: projeção por unit pós-binding com identidade/provenance próprias e uncertainty explícita.

`SymbolTable` não precisa interpretar conditions; ele continua fornecendo declarations/scopes. CFG/dataflow futuros dependem do novo produto em vez de modificar AST/resolution. Grammar só entra em slice próprio se os contexts/tokens atuais forem comprovadamente insuficientes.

Os caminhos de `source_scope` são pontos de auditoria somente leitura. `must_not_change: src/main` prevalece neste Discovery.

## Migrações requeridas

Neste slice, somente migração documental:

- promover a regra normativa para `docs/domain/conditional-expressions.md`;
- promover os `COND-*` para `docs/evals/conditional-expression-oracles.md`;
- arquivar WORK-COND-001 sem manter sua tasklist ativa;
- indexar ADR-0012 proposto, invariants e WORK-COND-002.

Migrações futuras de AST IDs, snapshots, manifests, occurrences ou report schema não são executadas. O slice implementador deverá declarar cada uma antes de alterar produção.

## Artefatos esperados

- work item completo em `docs/work/active/WORK-COND-002/`;
- resumo `docs/work/history/WORK-COND-001.md` e ausência do diretório ativo anterior;
- contrato canônico e catálogo `COND-*` fora da tasklist;
- ADR-0012 `Proposed`, INV-COND-001 e INV-COND-002;
- índices/roteamento coerentes;
- challenge pass e trade-offs em `eval.md`/ADR;
- gates `fast`, `semantic` e `full` verdes;
- nenhum diff em `src/`, grammar, fixtures, tests, scripts ou `pom.xml`.
