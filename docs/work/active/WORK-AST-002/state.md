# Estado

## Onde estamos

Fase 0 concluída. BACKLOG-AST-001 foi promovido para WORK-AST-002; `source_scope` permaneceu somente investigação e nenhum arquivo de produção foi alterado. A avaliação recomenda `READY FOR IMPLEMENTATION` somente após revisão independente e nova instrução explícita.

## Verde conhecido

- Caracterização: 8/8 testes verdes; 10/10 statements na fixture integrada, 50 alternativas 1:1, 14/14 DATA entries, 20/20 clauses e 1/1 preserved expression.
- AST→scope→symbol/relation→occurrence→resolution/candidate reconciliado integralmente nos produtos normais, com IDs locais repetidos sob units distintas e provenance preservada.
- FILLER, VALUE, REDEFINES/RENAMES, groups e CALL literal/identifier já preservam a estrutura necessária sem antecipar storage/dataflow.

## Restante

- Revisar diff e confirmar ausência de produção.
- Criar commit e PR dedicados ao discovery; então parar.

## Descobertas que afetam o plano

- F-01: coverage concreto só existe para statements; VALUE/clause/fallback sem referência pode deixar readiness falsa-positiva.
- F-02: faltam checks cross-product fail-closed; dois produtos corrompidos controlados são aceitos.
- F-03: manifesto ainda chama estruturas AST tipadas de preservadas/flattened.
- F-04 permanece decisão aberta: entrada SQL opaca é distinguível, mas também recebe `filler=true`.
- Oráculos ativáveis: 4 falhas intencionais exatas; detalhes e slices estão em `eval.md` e `plan.md`.

## Verificação final

- `./scripts/harness/check-fast.sh`: verde.
- `./scripts/harness/check-semantic.sh`: verde; 210 testes, 0 falhas, 0 erros e 4 skips correspondentes aos required oracles opt-in.
- `mvn -Dtest=AstSemanticBoundaryCharacterizationTest test`: verde; 8 testes, 0 falhas.
- `mvn -Dast.boundary.required=true -Dtest=AstSemanticBoundaryRequiredOracleTest test`: vermelho intencional; 4 testes, 4 falhas exatas que reproduzem F-01 e F-02.
