# Estado

## Onde estamos

Slice 1 mergeado pelo PR #10. WORK-AST-003 e `BUG-AST-PREORDER-001` foram resolvidos nos PRs #11/#12. O Discovery arquitetural do Slice 2 partiu da `main` `9aba9a897cc7f45ba7da3a25079d66aee838ba55`; nenhuma implementação de F-02 foi iniciada.

## Verde conhecido

- Cardinalidade exata: 10/10 statements, 14/14 DATA entries, 20/20 clauses e 1/1 preserved expression possuem um finding concreto; metadata/provenance e ordem determinística são preservadas.
- `VALUE` e `BLANK WHEN ZERO` sem occurrence nominal produzem gaps e bloqueiam readiness; preserved clause/expression sem referência continuam observáveis.
- Manifesto separa estrutura de dependency knowledge; `MODELED + DEPENDENCY_UNKNOWN` permanece para VALUE/OCCURS/REDEFINES/RENAMES.
- Os dois oráculos de F-01 integram o gate normal; 23 testes focais verdes e gates `fast`, `semantic` e `full` verdes.
- AST→scope→symbols→occurrences→resolution, CALL literal/identifier e ausência de símbolo FILLER permanecem reconciliados e determinísticos.
- A reprodução opt-in executa quatro required oracles: os dois F-01 passam e somente os dois F-02 falham por ausência de exception. A suíte focal sem opt-in fica verde com 14 testes e 2 F-02 skipped.
- Neste checkpoint documental, os gates `fast`, `semantic` e `full` passaram; o `full` incluiu regressão E2E estruturada e naming.

## Restante

- Abrir e revisar o PR exclusivo deste Discovery.
- Implementação do Slice 2/F-02 continua dependente de autorização explícita posterior; os oráculos não foram promovidos.

## Descobertas que afetam o plano

- F-01 e F-03 foram fechados no Slice 1 pela produção, sem busca textual nem alteração de `ResolutionAnalysisReport`.
- A taxonomia final do slice trata PICTURE/USAGE como não dependency-bearing para a capability nominal atual sem alegar layout; VALUE/OCCURS/REDEFINES/RENAMES preservam unknown.
- F-02 permanece reproduzível: com opt-in, somente os dois oráculos cross-product falham. Isso é o limite esperado deste PR.
- F-04 permanece decisão aberta e a shape SQL/FILLER não foi alterada.
- O owner recomendado para F-02 é uma estratégia híbrida: invariants autocontidos permanecem nos produtos e um `SemanticProductIntegrityValidator` reconcilia model/AST, tables, scopes, occurrences, resolution, relation resolution e candidates.
- O ponto exato de integração é imediatamente após `CobolReferenceResolver.resolve(...)` e antes de `CicsIntrinsicClassifier`; report e snapshots não recebem ownership de integridade.
- A API proposta reutiliza os `AstScopeIndex` por unit e falha com uma única `SemanticProductIntegrityException` cujo diagnóstico começa por `INTERNAL PRODUCT INTEGRITY FAILURE`.
- O custo previsto é linear no tamanho agregado dos produtos, com índices por unit/node/occurrence/relation e sem lookup textual.
