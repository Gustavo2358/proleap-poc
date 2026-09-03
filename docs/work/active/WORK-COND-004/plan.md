# Plano — WORK-COND-004

## Fatiamento

Checkpoints LÓGICOS no MESMO work item, branch e PR — sem cardinalidade física de commits:

1. **Checkpoint 1 — Discovery (concluído e aprovado no head `69e715e`):** investigação da grammar e da fonte IBM, contracasos de resolver, consumer impact analysis, decisão de modelagem (round 2: **D** — `DataReference` corrigido) e oracles. Zero produção. Entregáveis: lifecycle documental, teste de caracterização `ConditionNameSurfaceDiscoveryTest` (14 testes, somente FATOS), arquivamento de `WORK-COND-003`, atualização de índices/AGENTS/backlog (incluindo `BACKLOG-RES-004`). Commits do checkpoint: `5ed7e14` (discovery inicial), `43fdeab` (semantic challenge round 1), `0d4851d` (facts do contracaso) e `69e715e` (docs do round 2).
2. **Checkpoint 2 — Implementação (concluído, aguardando review humano final):** contrato abaixo implementado com commits revisáveis na mesma branch/PR (SHAs em `state.md` e no corpo do PR).

## Dependências

- Merge do PR #17 (Slice 3, `WORK-COND-003`) — já em `main` (`4cd95d6`).
- ADR-0012 `Accepted` e INV-COND-001/002 vigentes — sem ADR novo: a decisão D é o menor contrato surface-lossless.
- **`BACKLOG-RES-004` (IBM resolution-of-names step 3)** — dependência registrada NESTA rodada; bloqueia APENAS a ampliação `UNSPECIFIED → {DATA, FILE}` no resolver (resolução de condition-names qualificadas por file-name). Não bloqueia o Slice 4 estrutural.
- Aprovação humana explícita do Discovery antes do Checkpoint 2 (obrigatória).

## Superfície arquitetural provável

Plano consolidado do Checkpoint 2 (decisão revisada do round 2: **Alternativa D — `DataReference` corrigido, sem node novo, sem contrato novo**; a comparação C vs D e os challenges estão em `spec.md` "Decisão de modelagem — C vs D revisitado" e `state.md` "Semantic challenge pass — round 2"):

1. `src/main/java/.../Ast.java`
   - Único delta: novo valor `QualifierTarget.UNSPECIFIED` no enum (javadoc: a parse tree não classifica o namespace do qualifier — DATA/FILE/MNEMONIC possíveis).
   - Nenhum record novo, nenhuma interface nova, nenhum `permits` novo, `ContextualConditionTail` intocado.
2. `src/main/java/.../AstBuilder.java`
   - Novo helper `conditionNameReference(ConditionNameReferenceContext ctx)` que constrói `Ast.DataReference` a partir dos **children diretos** do context: `conditionName()` → `baseName`; `inData()` na ordem → qualifiers com target por posição (`i < inData.size()-1 ? DATA : UNSPECIFIED`); `conditionNameSubscriptReference()` → `SubscriptGroup` por grupo (meta do group context, subscripts via `expression(subscript, "condition-name subscript")`, writtenText do grupo).
   - **Proibido:** `nearestDescendants`/`firstDescendant` a partir da referência (Erro H); nenhum reparse de `writtenText`.
   - `buildBareNominal` usa o helper nos dois ramos (standalone e tail interno); `visitConditionNameReference` retorna o helper (uniformidade do visitor).
   - `dataReference`, `tableReference`, `buildQualifiers` (identifier paths) e SET/EVALUATE permanecem intocados.
3. `src/main/java/.../DataAndIndexReferenceResolver.java` — **mínimo e policy-preserving**
   - Único delta: case `UNSPECIFIED → Set.of(ReferenceKind.DATA)` em `qualifierConstraints` (a constraint efetiva é idêntica à atual; nenhuma seleção de candidates muda). Javadoc: ampliação `{DATA, FILE}` bloqueada por `BACKLOG-RES-004`.
   - Nenhuma outra linha muda: sem re-tipo, sem passo novo, sem `admissibleKind` novo.
4. **Byte-identical (voltaram a `must_not_change`):** `ReferenceOccurrenceCollector` (o `addDataReference` existente já emite a occurrence raiz com o falso gap preservado e percorre qualifiers/subscripts do `DataReference` corrigido — subscripts de condition-name passam a gerar occurrence `SUBSCRIPT` pela política pré-existente, sem mudança de código), `AstSnapshot`, `CobolReferenceResolver`, `ReferenceOccurrences`, `ReferenceResolution`, grammar, manifesto de coverage.
5. Testes (ver `eval.md`): novo `ConditionNameSurfaceAstTest` (oracles CN-01..CN-12 e erros A..M); migração dos asserts de caracterização do `ConditionNameSurfaceDiscoveryTest` (a corrupção atual deixa de existir); `ConditionSurfaceAstTest` e `SemanticConditionContextDiscoveryTest` permanecem verdes sem migração de tipo (o tail interno continua `DataReference`); `AstPreorderInvariantTest` e `assertActualProductsJoin` inalterados.
6. `docs/domain/semantic-ast.md` e `docs/domain/conditional-expressions.md`: parágrafo curto registrando o lowering corrigido, o alvo `UNSPECIFIED` e a dependência `BACKLOG-RES-004` (atualização documental canônica no Checkpoint 2).

## Migrações requeridas

- Nenhuma migração de dados; produtos são gerados por execução. Baselines de corpus mudam apenas onde a estrutura legítima foi materializada (qualifiers/subscripts que hoje não existiam como children); métricas `dataReferences` do coverage permanecem as mesmas.
- `planned:` do `work-item.yaml` para `ConditionNameSurfaceAstTest.java` é removido no mesmo checkpoint que cria o arquivo.
- Os discovery tests do estado pré-fix não são oracles permanentes; migram no Checkpoint 2.

## Artefatos esperados

- Checkpoint 1 (Discovery, múltiplos commits revisáveis) e Checkpoint 2 (produção+oracles) na branch `implementation/work-cond-004-condition-name-surface`; PR único com os SHAs por checkpoint.
- `ConditionNameSurfaceDiscoveryTest` (FACT, 14 testes; migra no Checkpoint 2) e `ConditionNameSurfaceAstTest` (oracles CN-01..CN-12, implementado no Checkpoint 2).
- Gates verdes: `fast`, `semantic`, `performance`, `full` em ambos os checkpoints.
- `state.md` e `eval.md` atualizados no Checkpoint 2; `WORK-COND-004` permanece `active/` até review humano final (merge não é automático).
