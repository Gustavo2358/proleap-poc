# Plano

## Fatiamento

1. **Checkpoint 1 — Current semantic state and boundary requirements.** Reconstruir a pipeline pelo código, inventariar produtos, ownership, lifetime, identities, joins, provenance, incompletude, apresentação, produtos pós-binding e requisitos R1–R7. Esta é a única fase autorizada nesta sessão.
2. **Review humano obrigatório.** O resultado do Checkpoint 1 deve ser revisado antes de qualquer desenho de boundary.
3. **Checkpoint 2 — Boundary design and sufficiency.** Após review e autorização: definir uma ou mais candidatas de boundary; comparar record, facade e envelope como alternativas, sem assumir nenhuma; definir o que cruza e o que não cruza; responsabilidades de evolução/versionamento; relação com produtos pós-binding; matriz de suficiência semântica; reavaliar impactos downstream; localizar a seam de Clean Architecture; propor ADRs e invariantes.
4. **Review humano obrigatório.** O Checkpoint 2 termina antes de qualquer experimento de interchange ou implementação.
5. **Checkpoint 3 — Executable falsification / interchange proof.** Após novo review e autorização: tentar falsificar a candidata com snapshot semântico experimental, determinismo, round-trip, consumer independente, ausência de dependência de ANTLR/parse tree/presentation, preservação de joins, provenance e análise parcial, além de sanity check de tamanho/tempo. Comparar arquivo único e bundle somente se a questão de transporte ainda permanecer relevante.
6. **Review humano obrigatório.** O Checkpoint 3 termina com recomendação revisável; não autoriza implementação automaticamente.
7. **Outro work item — implementação futura.** Qualquer slice de produto estável, adapter de inspeção, lowering, IR ou consumidor deverá ser explicitamente autorizado depois dos três checkpoints.

## Dependências

- `AGENTS.md`, índice de conhecimento, pipeline, invariantes, ADRs e regras de domínio listados em `work-item.yaml`.
- Evals semânticos e gates do harness listados no contrato.
- `WORK-AST-002` é contexto de boundary: Slice 2 está em Discovery e F-02 exige review/merge e autorização posterior. Este work item não altera seu escopo nem incorpora suas tarefas.
- `ExternalClassification` e as decisões de incompletude/provenance são evidência atual; `ConditionSemantics`, `ConditionValidation`, CFG, dataflow e dependency facts são referências futuras, não dependências implementadas.

## Superfície arquitetural provável

O código atual sugere uma composição distribuída no composition root: `ExplorerMain` retém produtos separados e chama resolução, classificação e report; snapshots fazem projeções posteriores. Isso é uma observação para o Checkpoint 1, não uma decisão sobre a forma da boundary. O Checkpoint 2 deverá determinar a seam e o grau de composição necessário sem importar internals do parser ou da apresentação.

## Migrações requeridas

Nenhuma migração ou alteração de produção é autorizada no Checkpoint 1. Migrações documentais, compatibilidade, versionamento de identidades e adapters só poderão ser definidas no Checkpoint 2 após decisão humana.

## Artefatos esperados

- Este work item com contrato, plano em três checkpoints, eval e estado factual.
- `docs/history/evidence/semantic-product-boundary-checkpoint-1.md` com o relatório de evidência do Checkpoint 1.
- Evidência referenciada por código, testes e fixtures existentes; probes descartáveis removidos.
- Nenhuma implementação, serializer, schema, fixture corretiva ou alteração em `src/main/**`.
