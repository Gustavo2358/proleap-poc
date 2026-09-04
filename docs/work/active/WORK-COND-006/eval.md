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
| CH-11 generic traversal fallacy: child sem typed condition routing | PASS | F1 contrasta `visitConditionSurface` no IF com fallback DATA no SEARCH |
| CH-12 armazenar apenas `statement()` e perder NEXT SENTENCE | PASS | F2 prova tokens diretos e `statement().size() == 0` |
| CH-13 VARYING sempre DATA | PASS | F3 INDEX registra atual DATA/{DATA} e futuro selectedCandidate INDEX |
| CH-14 VARYING sempre INDEX | PASS | F3 DATA resolve selectedCandidate DATA |
| CH-15 um role compartilhado para table/varying/condition/subscript | PASS | controle negativo + F3 separam as posições |

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

## Discovery Round 2 — findings fechados

### F1-COLLECTOR-ROUTING

- **Current fact:** `ReferenceOccurrenceCollector.visit(Ast.Node, ...)` possui routing semântico explícito para `IfStatement`, `EvaluateStatement` e `PerformStatement`; somente esses boundaries chamam `visitConditionSurface`. O fallback `for (Ast.Node child : Ast.children(node)) visit(child, role, preservation)` propaga o role recebido e, para um `Ast.DataReference` standalone, cai em DATA/{DATA}. O SEARCH atual nem sequer coloca `condition` em `Ast.children`: `PreservedStatement → StatementClause(searchWhen)`, com `recognizedNodes` vazio.
- **Future contract:** materializar `SearchWhen.condition` como condition surface e declarar explicitamente `SearchWhen.condition → typed CONDITION position → visitConditionSurface(condition)`. `Ast.children` continua necessário para reachability, IDs, scope, provenance e pre-order, mas não substitui routing semântico.
- **Negative implementation killed:** `SearchWhen.condition` apenas como child estrutural, sem chamada explícita a `visitConditionSurface`, não pode produzir `CONDITION/{CONDITION}` para `WHEN FLAG-ON`.
- **Evidence/test:** `F1_genericAstChildrenTraversalCannotSupplyConditionPositionRouting` contrasta SEARCH preservado com IF tipado: o IF resolve `FLAG-ON` como CONDITION/{CONDITION}; o SEARCH atual não tem condition node nem occurrence.

### F2-SEARCH-NEXT-SENTENCE

- **Current fact:** a grammar local é `searchWhen : WHEN condition (NEXT SENTENCE | statement*)`. Para `WHEN FLAG-ON NEXT SENTENCE`, `when.condition()` existe, `when.statement()` é vazio e os tokens `NEXT`/`SENTENCE` são filhos diretos da alternativa. O lowering preservado atual cria um `StatementClause(searchWhen)` e `AstBuilder.statementsInside` injeta um `Ast.NextSentenceStatement` quando encontra os dois tokens; a condition continua perdida.
- **Future contract:** `SearchWhen` preserva a action como `statements = [Ast.NextSentenceStatement]` quando a alternativa `NEXT SENTENCE` estiver escrita. O builder deve reconhecer explicitamente os tokens, e não depender de `statement()`. O nó existente preserva a forma estrutural e evita boolean/action paralelo; provenance/span da ação escrita deve ser mantido.
- **Negative implementation killed:** um modelo que armazene somente `searchWhen.statement()` perde NEXT SENTENCE e não é lossless.
- **Evidence/test:** `F2_nextSentenceIsAnAlternativeTokenPathAndCurrentPreservedClauseRetainsIt` verifica parse path, ausência de `statement()`, clause preservada e nó AST atual.

### F3-SEARCH-VARYING

- **Current fact:** IBM 6.4 distingue `VARYING` do searched table e da condition: o varying pode ser index-name ou identificador que seja item índice/item elementar inteiro. A grammar local representa ambos por `searchVarying : VARYING qualifiedDataName`. Hoje o operand cai na policy default DATA/{DATA}: `SEARCH-IDX` (de `INDEXED BY`) fica UNRESOLVED/INVALID_NAMESPACE_FOR_CONTEXT, enquanto `SEARCH-COUNTER PIC 9(4)` resolve DATA.
- **Future contract:** posição independente `SEARCH_VARYING`, sem novo `ReferenceKind`, com `role = CONTEXT_DEPENDENT`, `primary kind = DATA` e `admissibleKinds = {DATA, INDEX}`. Assim a seleção final pode ser INDEX para `SEARCH-IDX` e DATA para `SEARCH-COUNTER`; `searchedReference`, varying, condition e subscripts nunca compartilham uma policy global.
- **Negative implementation killed:** `VARYING → DATA` sempre falha no oracle INDEX; `VARYING → INDEX` sempre falha no oracle DATA; aplicar CONDITION ao varying/table/subscript também falha.
- **Evidence/test:** `F3_varyingIndexIsCurrentlyDefaultDataButFuturePolicyMustAdmitIndex`, `F3_varyingElementaryIntegerIsDataAndMustRemainAdmissibleAsData` e `F3_varyingAndConditionUseIndependentSemanticPositions` registram kind, admissibleKinds, status, selectedCandidate e separação de posições.

IBM authority: [SEARCH statement, Enterprise COBOL 6.4](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-search-statement), [binary SEARCH/SEARCH ALL, Enterprise COBOL 6.4](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=statement-binary-search) e [comparisons of index-names and index data items](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=conditions-comparison-index-names-index-data-items).

## Status do Discovery

`READY_FOR_IMPLEMENTATION` quanto ao contrato de Discovery Round 2. A implementação continua proibida neste PR e requer novo review humano.
