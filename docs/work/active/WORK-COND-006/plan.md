# Plano

## Fatiamento

1. **Checkpoint 1 — Discovery (este PR):** validar baseline pós-PR #19, caracterizar grammar → parse tree → AST → occurrences → resolution, confirmar IBM normal/ALL, criar oracles S1–S6 e controle negativo, fechar migration contract e parar.
2. **Implementation futura, após review/autorização:** substituir somente o lowering preservado de `searchStatement` por `SearchStatement` e materializar `SearchWhen` com condition surface existente; manter IDs pre-order e ownership de clauses.
3. **Verification futura:** promover cobertura de `searchStatement`/`searchWhen`, atualizar snapshots/cardinalidades justificadas e provar que cada nominal escrito produz uma occurrence/resolution entry única.
4. **Slice posterior separado:** validar restrições type-sensitive e específicas de SEARCH ALL, se o produto passar a declarar validade semântica.
5. Slice 7 de regressão de corpus permanece posterior e separado.

## Dependências

- Slice 5 mergeado no PR #19: policy contextual shape-sensitive, `conditionNameReference` sem autoridade semântica e resolver nominal inalterado.
- `docs/domain/conditional-expressions.md`, ADR-0012, INV-COND-001/002 e oracles `COND-*`.
- IBM Enterprise COBOL for z/OS 6.4 Language Reference e documentação IBM de SEARCH serial/binary.
- `Ast.children`, `AstScopeIndex` e joins de provenance/IDs existentes; não há evidência de necessidade de alterar scope ou resolver.

## Superfície arquitetural provável

Confirmada como mínima: `Ast.java`, `AstBuilder.java` e `ReferenceOccurrenceCollector.java`. O manifest de coverage precisa reclassificar `searchStatement`/`searchWhen` quando a implementação for autorizada. Testes focalizados e fixtures podem crescer em `src/test`; grammar não entra no escopo porque `searchStatement` já reconhece ambas as formas e `searchWhen.condition` já é o nó correto da parse tree.

`AstScopeIndex`, `ReferenceResolution`, `ResolutionContracts`, symbol tables e snapshots não precisam de alteração estrutural demonstrada neste Discovery; só entram em implementação se um oracle futuro revelar uma incompatibilidade concreta.

## Migrações requeridas

- Remover a duplicação do caminho `PreservedStatement` quando SEARCH virar typed statement.
- Preservar operands reconhecidos hoje e acrescentar apenas condition/branch nodes alcançáveis.
- Atualizar coverage de `PRESERVED_UNINTERPRETED` para a classificação da boundary tipada, sem afirmar que validation/ConditionSemantics existem.
- Rebaselinar IDs/snapshots somente com evidência aprovada e sem esconder diferenças de cardinalidade.

## Artefatos esperados

- `SearchWhenConditionDiscoveryTest.java` como caracterização executável sem produção.
- Este work item ativo com contrato, plano, eval e estado factual.
- PR explicitamente `PHASE 1 — DISCOVERY`, `IMPLEMENTATION NOT AUTHORIZED`, `NOT READY FOR MERGE`.
- Commits append-only; nenhum merge ou implementação neste checkpoint.
