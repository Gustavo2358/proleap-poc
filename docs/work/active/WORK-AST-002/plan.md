# Plano

## Fatiamento

1. Promover o backlog e congelar restrições, autoridades, decisões e oráculos da Fase 0.
2. Inventariar lowering e cardinalidade parse context → AST para statements, entries, clauses e expressões preservadas.
3. Caracterizar containment DATA, FILLER, VALUE, REDEFINES, RENAMES e separação sintática de CALL.
4. Reconciliar AST → scopes → symbols/relations → occurrences → resolution/candidates por identidade composta e provenance.
5. Caracterizar coverage, dependency knowledge e readiness, incluindo unknown sem referência nominal.
6. Registrar findings, falsos alarmes, decisões abertas e slices recomendados; não executar slices de produção.

## Dependências

- Autoridades e evals listados em `work-item.yaml`.
- Fonte oficial IBM Enterprise COBOL quando a semântica COBOL não estiver suficientemente fechada pelo contrato canônico.
- `BACKLOG-CFG-001` e `BACKLOG-DF-001` permanecem consumidores futuros bloqueados por lacunas comprovadas relevantes.

## Superfície arquitetural provável

Superfície apenas investigada nesta fase: `AstBuilder`, `Ast`, `AstScopeIndex`, `SemanticCoverage`, `GrammarCoverageManifest`, symbol model, occurrences, resolution products e `ResolutionAnalysisReport`. Nenhum desses arquivos está autorizado para edição. A provável incisão de produção será reduzida após os oráculos distinguirem coverage ausente, integridade de joins e capacidade já existente.

## Migrações requeridas

Nenhuma migração de produção ou baseline nesta fase. A promoção atualiza somente o índice de trabalho. Eventuais migrações canônicas serão propostas para slices posteriores e exigirão revisão independente.

## Artefatos esperados

- Os cinco artefatos obrigatórios do work item.
- Fixture integrada focal e helper de teste não produtivo.
- Matriz versionada de fronteiras com cardinalidades e identidades exatas.
- Testes verdes do comportamento observado e oráculos de requisito ativáveis que demonstrem lacunas.
- Relatório de discovery com guarantee matrix, findings, false alarms, decisões, slices e readiness.
- Commit e PR exclusivos de discovery.

## Slices de produção recomendados após revisão independente

### Slice 1 — Coverage concreto e taxonomia coerente

- **Problema:** F-01/F-03; entries, clauses e preserved expressions existem na AST, mas não geram findings, e o manifesto descreve várias estruturas tipadas como apenas preservadas.
- **Oracle atualmente falho:** `everyMaterializedSemanticBoundaryHasExactlyOneFinding` e `unknownDataClauseWithoutNominalReferenceBlocksReadiness` em `AstSemanticBoundaryRequiredOracleTest`.
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
