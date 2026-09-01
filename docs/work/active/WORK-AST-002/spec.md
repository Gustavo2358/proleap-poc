# WORK-AST-002 — Hardening da fronteira AST

## Problema

O discovery revisado no PR #9 provou que `BACKLOG-AST-001` exige duas mudanças de produção localizadas: coverage concreto/taxonomia nas fronteiras AST (F-01/F-03) e integridade cross-product fail-closed (F-02). O Slice 1 fechou F-01/F-03 no PR #10. O PR #12 corrigiu separadamente `BUG-AST-PREORDER-001`, removendo o bloqueio de identidade/traversal levantado por WORK-AST-003. Resta decidir e implementar F-02 sem misturar produtos, report ou análises futuras.

## Objetivo

Implementar as lacunas comprovadas em PRs independentes. A autorização vigente cobre exclusivamente o Discovery arquitetural do Slice 2: fechar contrato, ownership, ponto de integração, modelo de erro e matriz de regressão da integridade cross-product. Nenhuma alteração de produção ou promoção dos oráculos F-02 está autorizada nesta sessão.

## Domínio de entrada suportado

Fonte aceita pela gramática COBOL configurada após normalização e preprocessing, para todos os program units top-level e nested reconhecidos. A matriz focal cobre statements, data description entries, clauses DATA, expressões estruturadas ou preservadas, FILLER, VALUE, REDEFINES, RENAMES, grupos, CALL literal e por identifier/expression, referências DATA, scopes, símbolos, occurrences, resolution entries, coverage, readiness e provenance.

Parser errors, COPYs ausentes e falhas de preprocessing permanecem input incompleto e não fazem parte da garantia de representação exata. Containers SQL, CICS e SQLIMS continuam opacos.

## Classes semânticas

- Fronteiras materializadas: statement, data description entry, data clause e fallback/preserved expression dependency-bearing.
- Declarações: grupos e itens elementares, níveis 66, 77 e 88, FILLER e entrada SQL opaca.
- Relações declarativas: REDEFINES, RENAMES e endpoints nominais, sem alegação de layout ou alias.
- Referências: DATA estruturada/preservada, qualifier, subscript, reference modification, PROGRAM literal e target de CALL por identifier/expression.
- Produtos namespaced: AST/local node ID, scope, symbol/relation, occurrence, resolution entry e candidate sob `ProgramUnitId`.
- Incerteza: `ConstructionCoverage` e `DependencyKnowledge` como dimensões independentes; sintaxe válida desconhecida produz finding/gap, não ausência.

## Premissas

- `LANGUAGE_GUARANTEED`: FILLER não declara um nome utilizável; REDEFINES e RENAMES preservam relações declarativas cujas restrições COBOL serão verificadas contra fonte oficial do dialeto quando necessárias.
- `ARCHITECTURE_GUARANTEED`: produtos permanecem separados; binding nominal não infere valores; identidade entre units inclui `ProgramUnitId`; incompletude bloqueia claims incompatíveis.
- `SPECIFICATION_GUARANTEED`: findings representam fronteiras semânticas materializadas, não cada wrapper gramatical.
- `ARCHITECTURE_GUARANTEED`: o ponto de reconciliação deve preceder o primeiro consumidor pós-resolution. No pipeline atual, todos os produtos necessários coexistem imediatamente após `CobolReferenceResolver.resolve(...)` e antes de `CicsIntrinsicClassifier`; report e snapshots consomem somente produtos já validados.
- `SPECIFICATION_GUARANTEED`: checks autocontidos continuam nos próprios produtos; joins entre AST, tables, scopes, occurrences, resolution, relation resolution e candidates pertencem a um integrador dedicado no ponto de orquestração.
- `UNCERTAIN`: entries tipadas são containers não dependency-bearing; `PICTURE`/`USAGE` não adicionam dependência nominal e não alegam layout, enquanto `VALUE`, `OCCURS`, `REDEFINES` e `RENAMES` preservam `DEPENDENCY_UNKNOWN` para valores, cardinalidade e aliases ainda desconhecidos. Essas decisões do Slice 1 não são reabertas por F-02.
- `OBSERVED_IN_CURRENT_CORPUS_ONLY`: nenhuma cardinalidade ou forma encontrada apenas no corpus será promovida a regra.

## Comportamento esperado

1. Cada statement e declaração DATA relevante tem exatamente um AST node determinístico e alcançável.
2. Cada fronteira aprovada tem exatamente um finding com `astNodeId` válido; wrappers não duplicam findings.
3. `MODELED` descreve construção estrutural, independentemente de `DEPENDENCY_UNKNOWN`.
4. Cada referência DATA coletada junta, sem texto, AST node, scope, resolution entry e candidate/declaration quando resolvida.
5. FILLER permanece `DataEntry` sem símbolo nominal; grupos e endpoints REDEFINES/RENAMES permanecem alcançáveis.
6. CALL literal produz `ProgramReference`; CALL por identifier/expression produz referência DATA ou expressão preservada e nunca target PROGRAM inferido.
7. Joins usam `(ProgramUnitId, astNodeId)` ou `(ProgramUnitId, domain, localId)`; IDs locais podem repetir entre units.
8. Provenance da occurrence é a do AST node correspondente e preserva exatidão.
9. Produtos estruturalmente incoerentes falham fechados; sintaxe válida ainda não interpretada produz incompletude observável.
10. `dependencyAnalysisReady` significa apenas readiness das capacidades implementadas/versionadas e não prova CFG, dataflow ou CALL dinâmico final.

## Comportamento diante de incerteza

Uma construção válida porém não interpretada deve permanecer como node preservado e finding/gap com `DEPENDENCY_UNKNOWN` ou coverage conservadora. Hipóteses não demonstradas permanecem registradas como abertas. Oráculos de requisito podem ficar vermelhos, isolados por ativação explícita, enquanto os testes de comportamento observado e os gates normais continuam verdes.

## Fora de escopo

- Implementação do validator, alteração de `ResolutionAnalysisReport.compose`, alteração de `ExplorerMain` e promoção dos dois oráculos de F-02; todos permanecem fora desta sessão de Discovery.
- Gramática, parser behavior, shape/cardinalidade da AST, symbols, occurrences, binding nominal ou baselines não explicados.
- CFG, statement effects, reaching definitions, possible/runtime values, storage regions/layout/aliases, kills e targets dinâmicos finais.
- Parsers ou interpretação semântica de SQL, CICS e SQLIMS.
- IDs persistentes resistentes a edição do fonte.

## Regras de domínio relacionadas

`docs/domain/semantic-ast.md`, `docs/domain/compilation-units.md`, `docs/domain/symbol-model.md`, `docs/domain/reference-resolution.md` e `docs/domain/provenance.md`.

## ADRs/invariantes relacionados

ADRs 0002, 0003, 0004, 0005, 0007, 0008, 0009 e 0010. INV-AST-001, INV-AST-002, INV-SYM-001, INV-PROV-002, INV-COV-001, INV-COV-002, INV-RES-001, INV-RES-002, INV-DET-001 e INV-PERF-001.

## Decisões de desenho da promoção

| Decisão | Estado | Recomendação derivada da evidência |
| --- | --- | --- |
| Unidade de coverage | FECHADA | Findings concretos representam somente fronteiras semânticas materializadas: statement, data description entry, data clause e fallback/preserved expression dependency-bearing. Wrappers que encaminham integralmente um filho não recebem finding. |
| Cardinalidade | FECHADA | Exatamente um finding por fronteira concreta e por program unit, com `astNodeId` do node materializado. A chave de auditoria é `(ProgramUnitId, astNodeId)`, nunca o ID local isolado. |
| Coverage versus dependency knowledge | FECHADA no modelo; ABERTA por regra | As dimensões permanecem independentes. `MODELED` não implica readiness. A classificação exata de `PICTURE`, `USAGE`, entry/container e clauses semânticas deve ser aprovada regra a regra; `VALUE`, clause preservada e expressão preservada dependency-bearing já possuem evidência para `DEPENDENCY_UNKNOWN`. |
| Falha interna versus incompletude | FECHADA | Join impossível ou ID órfão deve lançar exception/invariant com produto, unit e ID. Sintaxe válida preservada/unknown deve produzir finding/gap e análise parcial, não exception. |
| Identidade | FECHADA | Usar `(ProgramUnitId, astNodeId)`, `(ProgramUnitId, occurrenceId)` e `SemanticEntityId(ProgramUnitId, domain, localId)`. Containers por unit podem manter IDs locais, desde que a unit seja obrigatória em toda reconciliação. |
| Escopo da claim | FECHADA conceitualmente | `dependencyAnalysisReady` é apenas a claim versionada das capacidades implementadas; não prova CFG, storage, possible values ou call graph dinâmico. A apresentação final deve nomear esse escopo sem usar `COMPLETE` como alegação irrestrita. |

O Discovery do Slice 2 fechou a decisão (a): usar estratégia híbrida, preservando invariants locais existentes e adicionando um `SemanticProductIntegrityValidator` dedicado no ponto de orquestração. O validator deve ser chamado imediatamente após a resolução nominal e antes da classificação externa, report, snapshots ou futuras análises. `ResolutionAnalysisReport` não recebe symbol tables nem scope indexes, já é responsável por composição de coverage/readiness e ocorre depois do primeiro consumidor pós-resolution; portanto não deve adquirir ownership de integridade cross-product.

Continuam abertas e fora de F-02: (b) se `DataEntry(level=SQL, levelKind=OPAQUE, filler=true)` é contrato intencional ou conflation a remover; (c) se a relação de `FILLER REDEFINES` deve continuar ligada pelo par AST containment + occurrence/resolution ou ganhar uma relação declarativa cujo owner não seja símbolo.

## Fonte COBOL consultada

O dialeto configurado é tratado como IBM Enterprise COBOL. A documentação IBM confirma que `FILLER` pode ser sujeito de REDEFINES e que o objeto permanece um data-name; isso sustenta manter o `DataEntry` sem símbolo nominal e preservar o endpoint estruturado ([REDEFINES clause](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-redefines-clause)). A RENAMES level 66 define agrupamento alternativo e endpoints dentro do registro, sem por si só fornecer layout calculado ([RENAMES clause](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=entry-renames-clause)). VALUE especifica conteúdo inicial ou valores de condition-name; preservar lexemas na AST não equivale a calcular valor em program points ([VALUE clause](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-value-clause)).
