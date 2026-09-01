# Plano

## Fatiamento

1. Fase 0 concluída e revisada no PR #9: lowering, cardinalidades, joins, provenance e quatro oráculos foram caracterizados sem alteração de produção.
2. Slice 1 concluído no PR #10: coverage concreto, manifesto coerente e promoção somente dos dois oráculos de F-01.
3. WORK-AST-003 e `BUG-AST-PREORDER-001` concluídos nos PRs #11 e #12; `main` de origem deste Discovery é `9aba9a897cc7f45ba7da3a25079d66aee838ba55`.
4. Discovery do Slice 2, nesta sessão: reproduzir F-02, auditar invariants e consumidores, comparar ownerships e versionar o contrato recomendado sem alterar produção.
5. Futura Fase 2 de implementação, somente após review e autorização: validator linear dedicado, integração antes da classificação externa e promoção/refino dos dois oráculos F-02.
6. Regressão documental final permanece posterior aos slices de produção revisados.

## Dependências

- Autoridades e evals listados em `work-item.yaml`, revisão independente do discovery no PR #9, Slice 1 mergeado no PR #10 e correção de pre-order mergeada no PR #12.
- Fonte oficial IBM Enterprise COBOL quando a semântica COBOL não estiver suficientemente fechada pelo contrato canônico.
- `BACKLOG-CFG-001` e `BACKLOG-DF-001` permanecem consumidores futuros bloqueados por lacunas comprovadas relevantes.

## Superfície arquitetural provável

Para a futura implementação do Slice 2, a incisão recomendada é um novo `SemanticProductIntegrityValidator`, seu teste focal e a retenção dos `AstScopeIndex` já construídos na orquestração para uma única chamada em `ExplorerMain` após `CobolReferenceResolver.resolve(...)`. `ResolutionAnalysisReport`, classifier, snapshots e modelos semânticos permanecem consumidores ou produtos separados, sem absorver ownership do validator. A inclusão desses arquivos no `source_scope` depende da autorização da Fase 2.

## Migrações requeridas

Não há migração de AST, símbolos, occurrences, resolução, report, classifier, snapshot ou baseline de corpus no Discovery. A futura implementação deve preservar as shapes e apenas rejeitar combinações internamente impossíveis antes do primeiro consumo pós-resolution.

## Artefatos esperados

- Relatório versionado de F-02 em `eval.md`, com reprodução, inventário, matriz de joins, call sites, alternativas, API, erro, complexidade, riscos e aceite futuro.
- `spec.md`, `plan.md`, `eval.md` e `state.md` atualizados somente com conhecimento do Discovery.
- Dois oráculos F-02 ainda opt-in/vermelhos e nenhum teste de produção promovido.
- Commit e PR exclusivos do Discovery, sem merge.

## Slices de produção recomendados após revisão independente

### Slice 1 — Coverage concreto e taxonomia coerente

- **Problema:** F-01/F-03; entries, clauses e preserved expressions existem na AST, mas não geram findings, e o manifesto descreve várias estruturas tipadas como apenas preservadas.
- **Oracle do discovery, promovido no Slice 1:** `everyMaterializedSemanticBoundaryHasExactlyOneFinding` e `unknownDataClauseWithoutNominalReferenceBlocksReadiness` em `AstSemanticBoundaryRequiredOracleTest` agora integram o gate normal.
- **Provável superfície:** `AstBuilder` (registro das fronteiras sem wrappers), `SemanticCoverage` somente se invariants locais precisarem ser reforçados, `grammar-rule-manifest.tsv`; `ResolutionAnalysisReport` já bloqueia um finding unknown e não deve mudar sem necessidade demonstrada.
- **Invariants/evals:** INV-AST-002, INV-COV-001, INV-COV-002, INV-DET-001; EVAL-AST-001/003/004, EVAL-COV-001/002, EVAL-RES-COV-001 e EVAL-RES-REPORT-001.
- **must_not_change:** shapes/cardinalidades da AST, símbolos, occurrences e binding; gramática; parser; valores de runtime/storage/effects; baselines não explicados.
- **Dependências:** aprovação da classificação rule-by-rule, começando por VALUE, preserved clause/expression e entry/container; nenhuma dependência de CFG.
- **Risco:** médio — duplicar findings por wrapper, transformar modelagem estrutural em falsa readiness ou bloquear tudo por taxonomia excessivamente ampla.

### Slice 2 — Integridade linear cross-product

- **Problema:** F-02; produtos normais são coerentes, mas combinações corrompidas podem atravessar a composição sem fail-closed.
- **Oracle atualmente falho:** `reportFailsClosedWhenResolutionContainsOccurrenceMissingFromCollectorProduct` e `crossProductValidationRejectsSymbolWhoseDeclarationAstNodeDoesNotExist`.
- **Ownership recomendado:** estratégia híbrida. Invariants autocontidos permanecem nos construtores atuais; joins que exigem dois ou mais produtos ficam em um validator dedicado, testável isoladamente e chamado pela orquestração.
- **API proposta:** `SemanticProductIntegrityValidator.validate(model, symbolTables, scopeIndexesByUnit, occurrencesByUnit, resolution)`; `ReferenceResolution` já contém candidates e `DeclarationRelationResolution`.
- **Ponto exato:** depois de `CobolReferenceResolver.resolve(...)` e antes de `CicsIntrinsicClassifier.classify(...)`. Essa ordem protege classificação externa, report, snapshot e futuras análises com uma única validação.
- **Provável superfície:** novo `src/main/.../SemanticProductIntegrityValidator.java`, novo teste focal, `ExplorerMain` somente para reter o map de scopes e chamar o validator, além do refino/promoção dos oráculos atuais. Não alterar `ResolutionAnalysisReport`.
- **Invariants/evals:** ADR-0003/0005; INV-SYM-001, INV-PROV-002, INV-DET-001, INV-PERF-001; EVAL-SYM-001/002, EVAL-RES-REL-001, EVAL-RES-DET-001 e EVAL-RES-PERF-001.
- **must_not_change:** status/candidates válidos, ambiguidade, IDs locais, resolução nominal, snapshots como fonte de verdade, complexidade maior que linear.
- **Dependências:** contrato aprovado para inputs e ownership do validator; independente do Slice 1, mas deve usar as mesmas identidades compostas.
- **Risco:** médio — chamada de orquestração esquecida por um consumidor alternativo, validação duplicada com classifier/report, rejeição indevida de candidate cross-unit válido ou join por ID local sem unit. Mitigar com teste do ponto de integração, identidade composta e validator sem lookup textual.
- **Complexidade:** `O(units + nodes + scopes + symbols + entities + relations + occurrences + resolutionEntries + candidates + candidateDeclarationSymbolIds)` em tempo e espaço auxiliar linear, usando maps por unit/node/occurrence/relation e acesso direto às listas contíguas de scopes/symbols/entities.

### Slice 3 — Regressão e migração documental

- **Problema:** consolidar os Slices 1–2 sem ampliar a claim para CFG/dataflow.
- **Oracle:** converter os quatro oráculos ativáveis em testes normais verdes; manter a matriz 1:1 e o reconciliador positivo.
- **Provável superfície:** testes/evals, `semantic-ast.md`, `symbol-model.md`, `reference-resolution.md`, invariants de coverage e apresentação da claim se necessário.
- **Invariants/evals:** todos os relacionados em `work-item.yaml`.
- **must_not_change:** nenhum CFG, storage model, statement effect, reaching definition, possible value ou dynamic target.
- **Dependências:** Slices 1 e 2 verdes.
- **Risco:** baixo a médio — snapshot/baseline só pode mudar por findings novos explicados; cardinalidades semânticas anteriores permanecem estáveis.

A ambiguidade SQL/FILLER e a eventual relação específica para `FILLER REDEFINES` não entram nesses slices sem decisão humana: a evidência atual prova observabilidade e uma possível conflation, não a necessidade de uma shape específica.
