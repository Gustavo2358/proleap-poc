# Plano

## Fatiamento

1. **Checkpoint 1 — Current semantic state and boundary requirements.** Reconstruir a pipeline pelo código, inventariar produtos, ownership, lifetime, identities, joins, provenance, incompletude, apresentação, produtos pós-binding e requisitos R1–R7. Concluído e usado como baseline factual.
2. **Review humano obrigatório.** Review do Checkpoint 1 concluído antes do desenho da boundary.
3. **Checkpoint 2 — Boundary design and sufficiency.** Após review e autorização: definir e comparar candidatas de boundary, incluindo modelo materializado próprio, facade e envelope; definir o que cruza e o que não cruza; responsabilidades de evolução/versionamento; relação com produtos pós-binding; matriz de suficiência semântica; reavaliar impactos downstream; localizar a seam de Clean Architecture; propor ADRs e invariantes. Concluído, aprovado e mergeado pelo PR #24.
4. **Review humano obrigatório.** Review do Checkpoint 2 concluído no PR #24; o Checkpoint 3A foi autorizado depois para o slice test-only de `CALL` literal.
5. **Checkpoint 3A — Executable falsification para `CALL` literal.** Slice test-only executado, aprovado e mergeado no PR #25; consumer independente, ausência de dependência de ANTLR/parse tree/presentation, preservação de joins, provenance e análise parcial foram exercitados.
6. **Checkpoint 3B — Executable falsification para `MOVE` → `CALL`.** Slice test-only autorizado e executado no PR #26; o consumer independente exercita a reconstrução após a liberação do frontend, a identidade namespaced, o ordering explícito, a provenance e a incerteza de target de runtime. Aguarda review; interchange, round-trip, outros constructs e trabalho posterior permanecem fora da autorização.
7. **Review humano obrigatório.** O Checkpoint 3B aguarda review e termina com recomendação revisável; não autoriza implementação automaticamente.
8. **Outro work item — implementação futura.** Qualquer slice de produto estável, adapter de inspeção, lowering, IR ou consumidor deverá ser explicitamente autorizado depois deste Discovery; trabalho posterior não está autorizado.

## Dependências

- `AGENTS.md`, índice de conhecimento, pipeline, invariantes, ADRs e regras de domínio listados em `work-item.yaml`.
- Evals semânticos e gates do harness listados no contrato.
- `WORK-AST-002` é contexto de boundary: Slice 2 está em Discovery e F-02 exige review/merge e autorização posterior. Este work item não altera seu escopo nem incorpora suas tarefas.
- `ExternalClassification` e as decisões de incompletude/provenance são evidência atual; `ConditionSemantics`, `ConditionValidation`, CFG, dataflow e dependency facts são referências futuras, não dependências implementadas.

## Superfície arquitetural provável

O código atual sugere uma composição distribuída no composition root: `ExplorerMain` retém produtos separados e chama resolução, classificação e report; snapshots fazem projeções posteriores. O Checkpoint 2 aprovou a seam de estado materializado próprio exposto por facade/port tipado, sem importar internals do parser ou da apresentação; o 3A foi aprovado e mergeado no PR #25 e o 3B foi executado em código test-only no PR #26, aguardando review.

## Migrações requeridas

Nenhuma migração ou alteração de produção é autorizada neste work item. As decisões do Checkpoint 2 estão aprovadas; os slices 3A e 3B permanecem test-only e não autorizam implementação, interchange, serializer ou qualquer trabalho posterior.

O Checkpoint 2 também deverá receber como inputs obrigatórios a responsabilidade de boundary
do analysis context (evidence bruta, policy normalizada e facts derivados/incerteza) e a
suficiência de papéis semânticos dos constructs de controle, sem depender de ordem implícita
da grammar ou de `writtenText`.

## Artefatos esperados

- Este work item com contrato, plano em três checkpoints, eval e estado factual.
- `docs/history/evidence/semantic-product-boundary-checkpoint-1.md` com o relatório de evidência do Checkpoint 1.
- Evidência referenciada por código, testes e fixtures existentes; probes descartáveis removidos.
- Nenhuma implementação, serializer, schema, fixture corretiva ou alteração em `src/main/**`; trabalho posterior permanece não autorizado.
