# Plano

## Fatiamento

1. **Checkpoint 1 — Current semantic state and boundary requirements.** Reconstruir a pipeline pelo código, inventariar produtos, ownership, lifetime, identities, joins, provenance, incompletude, apresentação, produtos pós-binding e requisitos R1–R7. Concluído e usado como baseline factual.
2. **Review humano obrigatório.** O resultado do Checkpoint 1 deve ser revisado antes de qualquer desenho de boundary.
3. **Checkpoint 2 — Boundary design and sufficiency.** Após review e autorização: definir e comparar candidatas de boundary, incluindo modelo materializado próprio, facade e envelope; definir o que cruza e o que não cruza; responsabilidades de evolução/versionamento; relação com produtos pós-binding; matriz de suficiência semântica; reavaliar impactos downstream; localizar a seam de Clean Architecture; propor ADRs e invariantes. Concluído, aprovado e mergeado pelo PR #24.
4. **Review humano obrigatório.** Review do Checkpoint 2 concluído no PR #24; o Checkpoint 3A foi autorizado depois para o slice test-only de CALL literal.
5. **Checkpoint 3 — Executable falsification / interchange proof.** Continua não autorizado. O slice 3A autorizado tenta falsificar a candidata somente para CALL literal, com consumer independente, ausência de dependência de ANTLR/parse tree/presentation, preservação de joins, provenance e análise parcial; round-trip, interchange, outros constructs e qualquer trabalho posterior permanecem fora da autorização.
6. **Review humano obrigatório.** O Checkpoint 3 termina com recomendação revisável; não autoriza implementação automaticamente.
7. **Outro work item — implementação futura.** Qualquer slice de produto estável, adapter de inspeção, lowering, IR ou consumidor deverá ser explicitamente autorizado depois dos três checkpoints.

## Dependências

- `AGENTS.md`, índice de conhecimento, pipeline, invariantes, ADRs e regras de domínio listados em `work-item.yaml`.
- Evals semânticos e gates do harness listados no contrato.
- `WORK-AST-002` é contexto de boundary: Slice 2 está em Discovery e F-02 exige review/merge e autorização posterior. Este work item não altera seu escopo nem incorpora suas tarefas.
- `ExternalClassification` e as decisões de incompletude/provenance são evidência atual; `ConditionSemantics`, `ConditionValidation`, CFG, dataflow e dependency facts são referências futuras, não dependências implementadas.

## Superfície arquitetural provável

O código atual sugere uma composição distribuída no composition root: `ExplorerMain` retém produtos separados e chama resolução, classificação e report; snapshots fazem projeções posteriores. O Checkpoint 2 aprovou a seam de estado materializado próprio exposto por facade/port tipado, sem importar internals do parser ou da apresentação; o 3A apenas a falsifica em código test-only para CALL literal.

## Migrações requeridas

Nenhuma migração ou alteração de produção é autorizada no Checkpoint 1. Migrações documentais, compatibilidade, versionamento de identidades e adapters só poderão ser definidas no Checkpoint 2 após decisão humana.

O Checkpoint 2 também deverá receber como inputs obrigatórios a responsabilidade de boundary
do analysis context (evidence bruta, policy normalizada e facts derivados/incerteza) e a
suficiência de papéis semânticos dos constructs de controle, sem depender de ordem implícita
da grammar ou de `writtenText`.

## Artefatos esperados

- Este work item com contrato, plano em três checkpoints, eval e estado factual.
- `docs/history/evidence/semantic-product-boundary-checkpoint-1.md` com o relatório de evidência do Checkpoint 1.
- Evidência referenciada por código, testes e fixtures existentes; probes descartáveis removidos.
- Nenhuma implementação, serializer, schema, fixture corretiva ou alteração em `src/main/**`.
