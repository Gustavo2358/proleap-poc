# Estado

## Onde estamos

Checkpoint de Discovery concluído na branch `implementation/work-cond-005-contextual-occurrences`, sobre `main` com o PR #18 mergeado. Implementação não autorizada; zero arquivo de produção alterado.

## Verde conhecido

- Surface AST distingue standalone, contextual tail, relation operand, distribuição, NOT e boundaries.
- Teste FACT prova uma occurrence por C, child policies independentes e o falso gap atual.
- Teste what-if prova que o resolver atual aceita primary CONDITION com `{DATA, INDEX, CONDITION}` e seleciona DATA/INDEX/CONDITION/RENAMES-as-DATA; missing permanece `DECLARATION_NOT_FOUND`.
- `ContextualConditionOccurrenceDiscoveryTest`: passou.
- Gate `fast`: passou.
- Gate `semantic`: passou.
- Gate `full`: passou, incluindo regressão E2E e naming.

## Restante

Review humano e autorização explícita para o Checkpoint 2. SEARCH WHEN permanece Slice 6; `BACKLOG-RES-004` permanece separado.

## Descobertas que afetam o plano

- `ReferenceOccurrenceCollector` e `ReferenceResolutionManifest` contêm os couplings por `conditionNameReference`.
- IF e nodes condition tipados bastam para helpers estruturais; PERFORM mistura VALUE e UNTIL CONDITION em lista sem tag. Por isso `Ast.java`/`AstBuilder.java` entram no futuro scope apenas para metadata não-node de controls, sem mudar pre-order.
- Resolver, symbol model, CICS e `ResolutionContracts` não precisam mudar.

## Semantic challenge pass — Discovery

1. Trocar apenas o if por outra sintaxe incidental falha CO-01/02/11/15.
2. Permissividade global `{DATA, INDEX, CONDITION}` falha standalone CO-01.
3. Tail `{CONDITION}` reproduz WAUX em CO-03/05/06.
4. Relation contaminada por CONDITION falha CO-02/12.
5. Uma occurrence por kind viola CO-08 e a unicidade por AST node.
6. Qualifier herdando root falha CO-09.
7. Subscript herdando root falha CO-10.
8. Contexto único de IF não distingue os boundaries de CO-11.
9. Heurística OR não cobre AND e connectors mistos.
10. Lookup no collector viola phase ownership e architecture boundary.
11. Materializar predicate herdado antecipa ConditionSemantics e cria identidade/provenance falsos.
12. Primary CONDITION foi atacado: resolver agrupa DATA/CONDITION/INDEX, candidate final governa semântica, reports o tratam como syntactic hint e CICS o ignora; novo enum não acrescentaria binding e ampliaria switches.

## Decisão final

`READY_FOR_IMPLEMENTATION`. Usar helper de traversal por containers tipados + control PERFORM VALUE/CONDITION não-node; contextual root `CONDITION/{DATA, INDEX, CONDITION}`; standalone `{CONDITION}`; relation/distribution `{DATA, INDEX}`; qualifier/subscript independentes; resolver inalterado. A implementação aguarda review humano.
