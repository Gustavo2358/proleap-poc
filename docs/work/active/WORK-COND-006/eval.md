# Evaluation — WORK-COND-006 Discovery

## O que prova corretude

O Discovery precisa demonstrar a localização da perda, não fazer a implementação passar. A suite `SearchWhenConditionDiscoveryTest` prova que a grammar reconhece `searchStatement`/`searchWhen`, que `condition` contém os nominais esperados, que o AST atual é um `PreservedStatement`, que operands reconhecidos têm entries/resolution atuais e que as conditions não materializadas não têm occurrence. `AstBoundaryTestSupport.assertActualProductsJoin` protege IDs, pre-order, scope, provenance, cardinalidade e bijection nos produtos observados.

## Classes positivas

| Caso | Forma | Resultado atual caracterizado | Oracle futuro |
| --- | --- | --- | --- |
| S1 | `WHEN FLAG-ON` com nível 88 fora da tabela | `conditionNameReference` na parse tree; nenhum AST condition/occurrence/resolution para `FLAG-ON`; `TABLE-ITEM` preservado e resolvido | `CONDITION/{CONDITION}` para o root standalone |
| S2 | `WHEN SEARCH-A = SEARCH-B` | `SEARCH-A` e `SEARCH-B` são operands do preserved statement, occurrences DATA e resolvidos; nenhum `RelationCondition` AST | ambos permanecem DATA relation operands |
| S3 | `WHEN SEARCH-A = SEARCH-B OR SEARCH-C` | A/B preservados; `SEARCH-C` é `conditionNameReference` na parse tree e desaparece; não há `ContextualConditionTail` | A/B relation operands; C usa `CONDITION/{DATA,INDEX,CONDITION}` na shape bare |
| S4 | dois `WHEN` standalone | ordem e cardinalidade dos `searchWhen`/branch statements preservadas em clauses; FLAG-A/B desaparecem | dois `SearchWhen`, cada condition ligada somente aos seus statements |
| S5 | `WHEN NOT FLAG-ON` | `NOT` e condition estão na parse tree; nenhum `NegatedCondition`/occurrence | `NegatedCondition` sobre a surface standalone |
| S6 | `WHEN FLAG-ON OF GROUP-X` | root `FLAG-ON` desaparece; qualifier `GROUP-X` sobrevive como `NamedReference`/preserved nominal | `DataReference` lossless com qualifier e policy do root; qualifier occurrence independente |

`SEARCH ALL` também foi caracterizado: a mesma `searchWhen` e condition grammar preserva relations completas A/B/C/D no generic path, mas o bit `ALL` é observável apenas na parse tree e a validade normativa exige contrato próprio.

## Classes negativas

- `SEARCH` serial e `SEARCH ALL` não são promovidos como semanticamente equivalentes.
- `TABLE-ITEM`, `VARYING SEARCH-IDX`, `TABLE-VALUE(SEARCH-IDX)` e `SEARCH-KEY` não são automaticamente `CONDITION`; o controle negativo prova suas policies atuais separadas.
- Condition-name subscript/qualification não autoriza descartar o root nem propagar `CONDITION` para qualifier/subscript.
- Grammar acceptance de uma relation em SEARCH ALL não prova key/order/equal-to/AND-only validity IBM.

## Classes ambíguas

O caso contextual `SEARCH-A = SEARCH-B OR SEARCH-C` permanece semanticamente aberto até binding, exatamente como `IF`/`PERFORM`: C pode ser DATA, INDEX ou CONDITION conforme declaration kind, shape, qualification e scope. O Discovery não introduz candidate ou selected kind artificial. Formas SEARCH ALL aceitas pela grammar mas sem prova das preconditions IBM ficam incompletas/unsupported para validação futura.

## Casos adversariais

| Challenge | Resultado | Evidência |
| --- | --- | --- |
| CH-1 identifier em SEARCH não é sempre CONDITION | PASS | controle negativo e S2 preservam table/index/relation fora de CONDITION |
| CH-2 `grammarRule` não é autoridade | PASS | S3 observa `conditionNameReference` mas o oracle futuro exige policy contextual do Slice 5 |
| CH-3 sem WHEN/regex para reconstruir condition | PASS | asserts usam contexts e products; nenhum texto é reparsed |
| CH-4 collector não lê parse tree | PASS | resultado atual mostra ausência após AST preserved path |
| CH-5 não materializar só o primeiro WHEN | PASS | S4 verifica dois contexts, duas clauses e dois branch owners |
| CH-6 não achatar `A = B OR C` | PASS | S3 mantém relation completa e tail separado como contrato futuro |
| CH-7 não duplicar occurrence | PASS | join invariant e uma occurrence por AST node em S4 |
| CH-8 não propagar CONDITION para table/index/subscript | PASS | controle negativo exige roles/policies próprias |
| CH-9 não misturar SEARCH/SEARCH ALL | PASS | teste ALL e seção IBM separada; `all` deve ser preservado |
| CH-10 não mudar resolver para compensar AST | PASS | código e diff não alteram resolver; A resolve na fronteira AST |

## Casos de regressão

`SemanticConditionContextDiscoveryTest.characterizesSearchConditionReferencesThatDisappearAtThePreservedBoundary` já registrava a perda histórica de C e FLAG-ON. A nova suite isola S1–S6, adiciona resolução dos operands preservados, branch ownership, NOT, qualification e SEARCH ALL. O corpus WAUX/COACTUPC não é rebaselined no Discovery.

## Propriedades/relações metamórficas

- O número de `searchWhen` parseados é igual ao número de branch clauses atuais e deverá ser igual ao número de `SearchWhen` futuros.
- Alterar a ordem dos WHEN deve alterar a ordem de contexts/clauses/branches, não colapsar branches.
- Adicionar `OR C` deve acrescentar uma nominal surface/occurrence futura sem alterar occurrences A/B.
- Trocar `SEARCH` por `SEARCH ALL` deve alterar `all`/contrato normativo, não autorizar policy global de CONDITION.
- O join `(ProgramUnitId, astNodeId)` → occurrence → resolution deve permanecer bijetivo; qualifiers/subscripts continuam children com occurrence própria.

## Expectativas de escala

O futuro lowering deve percorrer os contexts de SEARCH e sua condition surface uma vez, em `O(search nodes + condition nodes + relevant occurrences)`, sem scan textual/global e sem `O(references × declarations)`. O Discovery não adiciona threshold de hardware.

## Gaps observados e status

- `searchStatement`: `PRESERVED_UNINTERPRETED` atual — confirmado.
- `searchWhen`: `PRESERVED_UNINTERPRETED` atual — confirmado.
- `condition` em `searchWhen`: parse tree presente, AST ausente — confirmado.
- occurrences/resolution para condition-only names: ausentes — confirmado.
- relation operands: preservados pelo generic path e resolvidos como DATA quando declarados — confirmado.
- resolver/candidate filtering: nenhum defeito observado; recebe somente occurrences sobreviventes — confirmado.

## Status do Discovery

`READY_FOR_IMPLEMENTATION`. A implementação continua proibida neste PR e requer novo review humano.
