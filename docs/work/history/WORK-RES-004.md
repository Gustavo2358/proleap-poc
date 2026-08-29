# WORK-RES-004 — Classificar condition-names em EVALUATE TRUE/FALSE

## Resultado

`EVALUATE TRUE/FALSE` agora preserva, por selector de `WHEN`, o índice do subject correspondente por `ALSO` e um contexto derivado dos accessors tipados da gramática. Somente `evaluateValue.identifier` direto, sem `evaluateThrough`, na posição de subject `booleanLiteral` recebe `BOOLEAN_SUBJECT_NOMINAL` e é coletado/resolvido como `CONDITION`.

`WHEN NOT condition-name` pertence à mesma classe. Selectors nominais sob subject de valor são `VALUE_COMPARISON` e admitem DATA/INDEX. Literals, intervalos `THRU` e outras formas permanecem fora da promoção. Não houve mudança de gramática, CFG, dataflow, CALL dinâmico ou linguagens embarcadas.

## Evidência

Os testes adversariais foram escritos antes da produção: no baseline `STATUS-OPEN`, `STATUS-CLOSED` e o homônimo `FLAG-DATA` eram occurrences DATA, e os condition-names terminavam em `INVALID_NAMESPACE_FOR_CONTEXT`. A fixture cobre TRUE, FALSE, NOT, múltiplos WHEN, ALSO posicional, homônimo DATA/CONDITION, ausente, comparison DATA/INDEX, literal e THRU.

`AstBuilderTypedTraversalTest` verifica a classificação e o índice `ALSO`; `DataAndIndexReferenceResolverTest` verifica kind, admissibleKinds, status e não regressão de DATA/INDEX. A tentativa inicial de expor selector como `Ast.Node` quebrou a ordem pre-order de IDs; o selector ficou como metadado imutável de `EvaluateBranch`, preservando a árvore navegável, IDs e provenance da expressão.

## Regressão de corpus

| Programa | Antes | Depois | Diferença |
| --- | ---: | ---: | ---: |
| COACTUPC | 1.245 gaps; 108 `INVALID_NAMESPACE_FOR_CONTEXT` | 1.137 gaps; 0 nessa categoria de occurrence | -108 gaps, todos resolvidos como CONDITION |
| CBSTM03A | 268 gaps | 268 gaps | sem diferença de dist |
| CBSTM03D | 268 gaps | 268 gaps | sem diferença de dist |

Em COACTUPC permanecem 1.118 `DECLARATION_NOT_FOUND`, 3 COPYs ausentes, 14 CICS opacos, 2 ambiguidades, uma relation `REDEFINES` com namespace inválido e um CALL linkage desconhecido. A análise continua `INCOMPLETE`.

Somente `dist/resolution-data.js` mudou; os baselines semântico e de regressão de COACTUPC foram atualizados para 1.137 gaps, 1.921 resolved e 1.118 unresolved. As duas dists CBSTM foram byte-a-byte idênticas às anteriores.

## Verificação

- `check-fast`: passou.
- `check-semantic`: passou.
- suíte limpa: `mvn clean test`, 158 testes verdes.
- componentes de `check-full`: fast, regressão E2E/baseline, e naming foram verificados verdes após a compilação limpa.
- PIT focalizado (`mutation-adversarial`) passou sob Temurin 25.0.4/PIT 1.21.0 quando executado em sessão persistente: 931 mutantes, 537 mortos (58%), 235 sem cobertura, força de testes 77%, sem timeout ou erro. Os mutantes das decisões novas de classificação, posição `ALSO`, selector genérico e travessia foram mortos; os sobreviventes restantes estão fora desse slice.
