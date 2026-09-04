# Plano

## Fatiamento

1. **Checkpoint 1 — Discovery (Round 1):** validar baseline pós-PR #19, caracterizar grammar → parse tree → AST → occurrences → resolution, confirmar IBM normal/ALL, criar oracles S1–S6 e controle negativo, fechar migration contract e parar.
2. **Checkpoint 1 — Discovery Rounds 2–4 (este PR):** fechar o routing tipado do collector, caracterizar a alternativa `NEXT SENTENCE`, refutar e confirmar a policy shape-sensitive de `VARYING` para `INDEX` e `DATA`, fechar a matriz bare/qualified, executar CH-11–CH-15 e as self-reviews SR-01–SR-15/RF-01–RF-15/RF4-01–RF4-15. Nenhuma produção é autorizada.
3. **Implementation futura, após review/autorização:** substituir somente o lowering preservado de `searchStatement` por `SearchStatement` e materializar `SearchWhen` com condition surface existente; adicionar routing explícito `SearchWhen.condition → typed CONDITION position → visitConditionSurface`, manter IDs pre-order e ownership de clauses, e representar `NEXT SENTENCE` com `Ast.NextSentenceStatement`.
4. **Verification futura:** promover cobertura de `searchStatement`/`searchWhen`, atualizar snapshots/cardinalidades justificadas e provar que cada nominal escrito produz uma occurrence/resolution entry única.
5. **Slice posterior separado:** validar restrições type-sensitive e específicas de SEARCH ALL, se o produto passar a declarar validade semântica.
6. Slice 7 de regressão de corpus permanece posterior e separado.

## Dependências

- Slice 5 mergeado no PR #19: policy contextual shape-sensitive, `conditionNameReference` sem autoridade semântica e resolver nominal inalterado.
- `docs/domain/conditional-expressions.md`, ADR-0012, INV-COND-001/002 e oracles `COND-*`.
- IBM Enterprise COBOL for z/OS 6.4 Language Reference e documentação IBM de SEARCH serial/binary.
- `Ast.children`, `AstScopeIndex` e joins de provenance/IDs existentes; não há evidência de necessidade de alterar scope ou resolver.

## Superfície arquitetural provável

Confirmada como mínima: `Ast.java`, `AstBuilder.java` e `ReferenceOccurrenceCollector.java`. O collector terá duas responsabilidades localizadas: (1) typed boundary routing para chamar `visitConditionSurface` na condition de cada `SearchWhen`; (2) routing de `SearchStatement.varying` para uma posição `SEARCH_VARYING` com helper puro shape-sensitive `searchVaryingKinds(ref)`. Não deve receber nova condition policy de SEARCH, duplicação de `relationOperandKinds`/`contextualKinds` ou lógica de resolver. O manifest de coverage precisa reclassificar `searchStatement`/`searchWhen` quando a implementação for autorizada. Testes focalizados e fixtures podem crescer em `src/test`; grammar não entra no escopo porque `searchStatement` já reconhece ambas as formas e `searchWhen.condition` já é o nó correto da parse tree.

`AstScopeIndex`, `ReferenceResolution`, `ResolutionContracts`, symbol tables e snapshots não precisam de alteração estrutural demonstrada neste Discovery; só entram em implementação se um oracle futuro revelar uma incompatibilidade concreta.

## Migrações requeridas

- Remover a duplicação do caminho `PreservedStatement` quando SEARCH virar typed statement.
- Preservar operands reconhecidos hoje e acrescentar apenas condition/branch nodes alcançáveis.
- Rotear a condition pelo boundary semântico tipado; `Ast.children` permanece responsável por reachability, IDs, scope, provenance e pre-order, não por escolher `CONDITION`.
- Preservar `NEXT SENTENCE` fora de `statement()` como `Ast.NextSentenceStatement` na action da branch.
- Centralizar `searchVaryingKinds(ref)`: bare → primary DATA/admissible `{DATA, INDEX}`; qualified → primary DATA/admissible `{DATA}`. A shape vem da surface AST, não da declaração.
- Atualizar coverage de `PRESERVED_UNINTERPRETED` para a classificação da boundary tipada, sem afirmar que validation/ConditionSemantics existem.
- Rebaselinar IDs/snapshots somente com evidência aprovada e sem esconder diferenças de cardinalidade.

## Artefatos esperados

- `SearchWhenConditionDiscoveryTest.java` como caracterização executável sem produção.
- Este work item ativo com contrato, plano, eval e estado factual.
- PR explicitamente `PHASE 1 — DISCOVERY`, `IMPLEMENTATION NOT AUTHORIZED`, `NOT READY FOR MERGE`.
- Commits append-only; nenhum merge ou implementação neste checkpoint.
