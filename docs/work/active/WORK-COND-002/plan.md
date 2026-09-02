# Plano — WORK-COND-002

## Fatiamento

Este work item contém somente o Slice 2 arquitetural e termina em review humano:

1. **S2.1 — Lifecycle:** promover o contrato/oracles de WORK-COND-001 para domínio/evals, criar resumo histórico e remover o diretório ativo anterior.
2. **S2.2 — Constraints:** auditar ADRs, invariants e contratos atuais de AST, symbols, occurrences, resolution, IDs, provenance, ambiguity e performance.
3. **S2.3 — Alternatives/challenge:** comparar AST contextual, normalization durante lowering e surface AST + produto pós-binding contra cada critério e contraexemplo.
4. **S2.4 — Decision record:** registrar a alternativa escolhida em ADR-0012 e INV-COND-001/002; após o review humano do PR #16, promover o ADR a `Accepted`.
5. **S2.5 — Checkpoint:** atender os REQUEST CHANGES do review humano (lifecycle do ADR e separação entre normalização de condition semantics e validação type-sensitive), executar gates, atualizar `state.md`, publicar no PR #16 e parar.

Slice 3 e todos os slices executáveis permanecem fora deste plano. Mesmo com o ADR `Accepted`, seu início exige autorização explícita e novo/atualizado work item.

## Dependências

- merge do PR #15 em `main`;
- contrato IBM e oracles `COND-*` promovidos por WORK-COND-001;
- Discovery histórico do PR #14 para a evidência de implementação;
- ADR-0002/0003/0005/0008/0009 e invariants de AST/symbols/resolution;
- INV-AST-003 para identidade/pre-order e política de performance para a futura projeção linear/indexada.

Não há dependência de alteração de grammar, código ou fixture neste slice.

## Superfície arquitetural provável

A futura implementação da decisão possui cinco responsabilidades separadas:

- `Ast`/`AstBuilder`: condition surface lossless e contextual, sem binding e sem type checking;
- `ReferenceOccurrences`/collector: usos escritos e admissible kinds, sem lookup nem synthetic expansion;
- `ReferenceResolution`: binding nominal existente, sem predicate reconstruction e sem `PIC`/`USAGE` checking;
- novo `ConditionSemantics`: projeção por unit pós-binding com identidade/provenance próprias e uncertainty explícita; materializa a relation normalizada sem afirmar validade type-sensitive;
- futuro `ConditionValidation` (conceitual): validação type-sensitive da relation a partir de `ConditionSemantics`, declaração/tipo e contratos IBM; ainda não existe em produção e terá API/schema decididos em slice futuro.

`SymbolTable` não precisa interpretar conditions; ele continua fornecendo declarations/scopes. CFG/dataflow futuros dependem do predicate normalizado e, quando precisarem, do veredito de `ConditionValidation`, em vez de modificar AST/resolution. Grammar só entra em slice próprio se os contexts/tokens atuais forem comprovadamente insuficientes.

Os caminhos de `source_scope` são pontos de auditoria somente leitura. `must_not_change: src/main` prevalece neste Discovery.

## Migrações requeridas

Neste slice, somente migração documental:

- promover a regra normativa para `docs/domain/conditional-expressions.md`;
- promover os `COND-*` para `docs/evals/conditional-expression-oracles.md`;
- arquivar WORK-COND-001 sem manter sua tasklist ativa;
- indexar ADR-0012 aceito, invariants e WORK-COND-002.

Migrações futuras de AST IDs, snapshots, manifests, occurrences ou report schema não são executadas. O slice implementador deverá declarar cada uma antes de alterar produção.

## Artefatos esperados

- work item completo em `docs/work/active/WORK-COND-002/`;
- resumo `docs/work/history/WORK-COND-001.md` e ausência do diretório ativo anterior;
- contrato canônico e catálogo `COND-*` fora da tasklist;
- ADR-0012 `Accepted`, INV-COND-001 e INV-COND-002;
- fronteira explícita `ReferenceResolution` (nominal) / `ConditionSemantics` (normalização) / `ConditionValidation` (validação type-sensitive conceitual), sem produto novo implementado;
- índices/roteamento coerentes;
- challenge pass e trade-offs em `eval.md`/ADR, incluindo o challenge arquitetural INDEX válido versus INDEX incompatível para separar normalização de validação type-sensitive;
- gates `fast`, `semantic` e `full` verdes;
- nenhum diff em `src/`, grammar, fixtures, tests, scripts ou `pom.xml`.
