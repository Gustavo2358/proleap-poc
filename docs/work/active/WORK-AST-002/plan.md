# Plano

## Fatiamento

1. Fase 0 concluída e revisada no PR #9: lowering, cardinalidades, joins, provenance e quatro oráculos foram caracterizados sem alteração de produção.
2. Slice 1 autorizado: registrar coverage concreto para statements, entries, clauses e preserved expressions; alinhar manifesto rule-by-rule e promover somente os dois oráculos de F-01.
3. Abrir PR independente do Slice 1 e parar para revisão externa.
4. Slice 2 permanece futuro e sem autorização nesta execução: integridade linear cross-product e dois oráculos de F-02.
5. Regressão documental final permanece posterior aos slices de produção revisados.

## Dependências

- Autoridades e evals listados em `work-item.yaml`, mais a revisão independente do discovery no PR #9.
- Fonte oficial IBM Enterprise COBOL quando a semântica COBOL não estiver suficientemente fechada pelo contrato canônico.
- `BACKLOG-CFG-001` e `BACKLOG-DF-001` permanecem consumidores futuros bloqueados por lacunas comprovadas relevantes.

## Superfície arquitetural provável

No Slice 1, a incisão aprovada fica em `AstBuilder`, no invariant local de `SemanticCoverage` e em `grammar-rule-manifest.tsv`, além dos testes/evals e contratos documentais correspondentes. `Ast`, `AstScopeIndex`, symbol model, occurrences, resolução e `ResolutionAnalysisReport` permanecem inalterados. A superfície cross-product continua reservada ao Slice 2.

## Migrações requeridas

Não há migração de AST, símbolos, occurrences, resolução ou baseline de corpus. O Slice 1 migra somente a taxonomia do manifesto e a cardinalidade esperada dos findings de coverage; snapshots gerados podem ganhar exclusivamente esses findings explicáveis.

## Artefatos esperados

- Registro comum de coverage nas quatro fronteiras materializadas, sem finding de wrapper.
- Manifesto coerente com entries/clauses e referências já tipadas, preservando dependency unknown.
- Matriz versionada com cardinalidade exata, provenance e determinismo.
- Dois oráculos de F-01 verdes no gate normal e dois oráculos de F-02 ainda opt-in/vermelhos.
- Commit e PR exclusivos do Slice 1.

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
- **Provável superfície:** validador interno novo mais um ponto de composição explícito; possivelmente checks locais somente onde o produto já disponha do contexto necessário e inputs adicionais na orquestração. A escolha final permanece aberta para review.
- **Invariants/evals:** ADR-0003/0005; INV-SYM-001, INV-PROV-002, INV-DET-001, INV-PERF-001; EVAL-SYM-001/002, EVAL-RES-REL-001, EVAL-RES-DET-001 e EVAL-RES-PERF-001.
- **must_not_change:** status/candidates válidos, ambiguidade, IDs locais, resolução nominal, snapshots como fonte de verdade, complexidade maior que linear.
- **Dependências:** contrato aprovado para inputs e ownership do validator; independente do Slice 1, mas deve usar as mesmas identidades compostas.
- **Risco:** médio — validação incompleta distribuída entre construtores ou assinatura transversal desnecessária; custo deve permanecer `O(nodes + scopes + symbols + relations + occurrences + entries + candidates)`.

### Slice 3 — Regressão e migração documental

- **Problema:** consolidar os Slices 1–2 sem ampliar a claim para CFG/dataflow.
- **Oracle:** converter os quatro oráculos ativáveis em testes normais verdes; manter a matriz 1:1 e o reconciliador positivo.
- **Provável superfície:** testes/evals, `semantic-ast.md`, `symbol-model.md`, `reference-resolution.md`, invariants de coverage e apresentação da claim se necessário.
- **Invariants/evals:** todos os relacionados em `work-item.yaml`.
- **must_not_change:** nenhum CFG, storage model, statement effect, reaching definition, possible value ou dynamic target.
- **Dependências:** Slices 1 e 2 verdes.
- **Risco:** baixo a médio — snapshot/baseline só pode mudar por findings novos explicados; cardinalidades semânticas anteriores permanecem estáveis.

A ambiguidade SQL/FILLER e a eventual relação específica para `FILLER REDEFINES` não entram nesses slices sem decisão humana: a evidência atual prova observabilidade e uma possível conflation, não a necessidade de uma shape específica.
