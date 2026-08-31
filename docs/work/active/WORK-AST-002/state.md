# Estado

## Onde estamos

Slice 1 implementado e validado na branch `work/work-ast-002-slice-1-coverage`, aguardando commit, push e PR independente. Nenhuma implementação do Slice 2 foi iniciada.

## Verde conhecido

- Cardinalidade exata: 10/10 statements, 14/14 DATA entries, 20/20 clauses e 1/1 preserved expression possuem um finding concreto; metadata/provenance e ordem determinística são preservadas.
- `VALUE` e `BLANK WHEN ZERO` sem occurrence nominal produzem gaps e bloqueiam readiness; preserved clause/expression sem referência continuam observáveis.
- Manifesto separa estrutura de dependency knowledge; `MODELED + DEPENDENCY_UNKNOWN` permanece para VALUE/OCCURS/REDEFINES/RENAMES.
- Os dois oráculos de F-01 integram o gate normal; 23 testes focais verdes e gates `fast`, `semantic` e `full` verdes.
- AST→scope→symbols→occurrences→resolution, CALL literal/identifier e ausência de símbolo FILLER permanecem reconciliados e determinísticos.

## Restante

- Revisar o diff completo, commitar, fazer push e abrir o PR do Slice 1; então parar.
- Slice 2/F-02 continua dependente de revisão externa e nova instrução explícita.

## Descobertas que afetam o plano

- F-01 e F-03 foram fechados no Slice 1 pela produção, sem busca textual nem alteração de `ResolutionAnalysisReport`.
- A taxonomia final do slice trata PICTURE/USAGE como não dependency-bearing para a capability nominal atual sem alegar layout; VALUE/OCCURS/REDEFINES/RENAMES preservam unknown.
- F-02 permanece reproduzível: com opt-in, somente os dois oráculos cross-product falham. Isso é o limite esperado deste PR.
- F-04 permanece decisão aberta e a shape SQL/FILLER não foi alterada.
