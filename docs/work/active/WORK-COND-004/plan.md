# Plano — WORK-COND-004

## Fatiamento

Dois checkpoints no MESMO work item, branch e PR:

1. **Discovery (Commit 1 — este commit):** investigação da grammar e da fonte IBM, matriz de shapes, caracterização da AST atual, consumer impact analysis, decisão de modelagem (contrato nominal compartilhado) e oracles. Zero produção. Entregáveis: lifecycle documental, teste de caracterização `ConditionNameSurfaceDiscoveryTest` (13 testes, somente FATOS), arquivamento de `WORK-COND-003`, atualização de índices/AGENTS/backlog. STOP para review humano.
2. **Implementação (Commit 2 — após aprovação humana):** contrato abaixo, delta revisável `Commit 1 → Commit 2`, na mesma branch/PR.

## Dependências

- Merge do PR #17 (Slice 3, `WORK-COND-003`) — já em `main` (`4cd95d6`).
- ADR-0012 `Accepted` e INV-COND-001/002 vigentes — nenhuma decisão arquitetural nova é necessária; o node tipado + contrato nominal compartilhado são evolução estrutural prevista pelo contrato de surface lossless, sem ADR novo.
- Aprovação humana explícita do Discovery antes do Commit 2 (obrigatória).

## Superfície arquitetural provável

Plano arquivo por arquivo do Commit 2 (decisão revisada: **Alternativa C — contrato nominal estrutural compartilhado**; ver `spec.md` "Decisão de modelagem" e `state.md` "Semantic challenge pass"):

1. `src/main/java/.../Ast.java`
   - Novo `sealed interface NominalReference extends Node permits DataReference, ConditionNameReference` com `String baseName()`, `String writtenText()`, `List<DataQualifier> qualifiers()`, `List<SubscriptGroup> subscriptGroups()`. `Node` ganha `NominalReference` no `permits`; `DataReference` passa a `implements Expression, NominalReference` (nenhum campo muda; os accessors já existem).
   - Novo record `ConditionNameReference(Meta meta, String baseName, String writtenText, List<DataQualifier> qualifiers, List<SubscriptGroup> subscriptGroups) implements Expression, NominalReference`, com listas `List.copyOf` e baseName não-nulo. Javadoc: o node preserva a shape escrita `conditionNameReference`; NÃO afirma a classe semântica (DATA/INDEX/CONDITION é binding-dependent); `IF TBL(I)`/`IF DATA-X OF GROUP-A` produzem o mesmo node sem alegação de validade (COND-N05). Sem campo de kind/lookup/declaration; sem `understanding` (a surface é sempre STRUCTURED — o fallback preservado continua `PreservedExpression`).
   - `Ast.children(ConditionNameReference)` = qualifiers + subscriptGroups (ordem escrita; idêntica à ordem de `DataReference` sem o modifier).
   - `ContextualConditionTail.nominalReference` passa de `DataReference` para `NominalReference` (javadoc: a interpretação final permanece aberta; o tipo do campo é o contrato estrutural neutro).
   - Nenhum outro node/field muda; nenhum campo de binding entra.
2. `src/main/java/.../AstBuilder.java`
   - Novo helper `conditionNameReference(ConditionNameReferenceContext ctx)` que constrói o node a partir dos **children diretos** do context: `conditionName()` para `baseName`; `inData()` na ordem para qualifiers, com target por posição — `i < inData.size()-1 ? DATA : DATA_OR_FILE` (grammar-faithful; NUNCA "DATA porque o branch é inData"); cada `conditionNameSubscriptReference()` vira um `SubscriptGroup` com meta do group context, subscripts via `expression(subscript, "condition-name subscript")` (o `qualifiedDataName` do subscript preserva os próprios qualifiers) e writtenText do grupo.
   - **Proibido:** `nearestDescendants(ctx, qualifier)` ou `firstDescendant(qualifiedDataName)` a partir da referência (rouba qualifiers do subscript interno — Erro H); nenhum reparse de `writtenText`.
   - `buildBareNominal` passa a usar o helper nos dois ramos (standalone e tail interno).
   - `visitConditionNameReference` passa a retornar o novo node (uniformidade do visitor; hoje só é alcançável como fallback, sem mudança observável).
   - `dataReference`, `tableReference`, `buildQualifiers` (paths de identifier) e os paths de SET/EVALUATE permanecem intocados.
3. `src/main/java/.../DataAndIndexReferenceResolver.java` — **STRUCTURAL ADAPTATION ONLY — NO RESOLUTION POLICY CHANGE**
   - `baseName(Ast.Node, Occurrence)`: `instanceof NominalReference → baseName()` (substitui o branch `DataReference`).
   - `qualifiedReference`: `node instanceof NominalReference ref && !ref.qualifiers().isEmpty()`.
   - `applyQualification`/`qualifyExtend`/`qualifierConstraints`: o gate de tipo e a extração passam a usar `NominalReference` (mesmos campos, mesmos algoritmos `orderedSubsequence`/`exactQualification`, mesmo mapeamento `DATA_OR_FILE → {DATA, FILE}` pré-existente).
   - Nenhuma mudança em candidate selection, namespace policy, scope walk, ambiguity, reason codes ou `admissibleKinds`. Delta de input documentado: último qualifier `DATA_OR_FILE` → root reference de condition-name qualificada por file-name passa a resolver (antes `DECLARATION_NOT_FOUND`); nenhum caso que já resolvia muda.
4. `src/main/java/.../ReferenceOccurrenceCollector.java`
   - Apenas traversal estrutural: branch para `ConditionNameReference` que emite a occurrence do node com a MESMA política atual (incluindo a regra `grammarRule == "conditionNameReference"` — o falso gap permanece observável) e percorre qualifiers (`QUALIFIER_COMPONENT`, mesma classificação de target de sempre) e subscripts (`SUBSCRIPT`, kind INDEX / {DATA, INDEX}), reusando a lógica extraída de `addDataReference` sem alterar comportamento. Resultado esperado e aceito: subscripts de condition-name, hoje descartados, passam a produzir occurrence `SUBSCRIPT` — recuperação de perda estrutural, não política semântica nova.
   - `ContextualConditionTail` continua percorrido por `Ast.children`; nenhuma mudança de `admissibleKinds`.
5. `src/main/java/.../AstSnapshot.java`
   - `label` e `attributes` para o novo node (baseName, writtenText); `ContextualConditionTail` label passa a usar `nominalReference().writtenText()` do contrato.
6. `src/main/java/.../CoverageSnapshot.java` — sem mudança de código obrigatória: a métrica `dataReferences` deixa de contar condition surfaces (elas não são data references). Baselines/diffs documentados no Commit 2 (nenhum finding novo de coverage; `grammar-rule-manifest.tsv` intocado).
7. Testes (ver `eval.md`): novo `ConditionNameSurfaceAstTest` (oracles CN-01..CN-12 e erros A..I); migrações mecânicas em `ConditionSurfaceAstTest` (filters `instanceof DataReference` sobre tail interno e `referenceName`), `SemanticConditionContextDiscoveryTest` (accessors continuam compilando via contrato; o assert `characterizesConditionNameSubscriptCorruption` migra porque o subscript `IDX` passa a gerar occurrence `SUBSCRIPT`) e `ConditionNameSurfaceDiscoveryTest` (os asserts que caracterizam a corrupção atual passam a caracterizar a nova surface). `AstPreorderInvariantTest`, `AstBoundaryTestSupport.assertActualProductsJoin` e snapshots continuam verdes com o node novo.
8. `docs/domain/semantic-ast.md` e `docs/domain/conditional-expressions.md`: parágrafo curto registrando o node, o contrato `NominalReference` e a fronteira de superfície (atualização documental canônica no Commit 2, junto com a produção correspondente).

## Migrações requeridas

- Nenhuma migração de dados; produtos são gerados por execução. Snapshot HTML passa a mostrar o type novo; cardinalidades mudam apenas onde a estrutura legítima foi materializada (qualifiers/subscripts que hoje não existiam como children).
- `planned:` do `work-item.yaml` para `ConditionNameSurfaceAstTest.java` é removido no mesmo checkpoint que cria o arquivo.
- Migração de asserts de caracterização: os discovery tests do estado pré-fix não são oracles permanentes; eles migram no Commit 2 e os oracles permanentes são CN-01..CN-12 + erros A..I.

## Artefatos esperados

- Commit 1 (docs+testes de caracterização) e Commit 2 (produção+oracles) na branch `implementation/work-cond-004-condition-name-surface`; PR único com os dois SHAs.
- `ConditionNameSurfaceDiscoveryTest` (FACT, 13 testes; migra no Commit 2) e `ConditionNameSurfaceAstTest` (oracles CN-01..CN-12, implementado no Commit 2).
- Gates verdes: `fast`, `semantic`, `performance`, `full` em ambos os checkpoints (o Commit 1 prova que produção não mudou).
- `state.md` e `eval.md` atualizados no Commit 2; `WORK-COND-004` permanece `active/` até review humano final (merge não é automático).
