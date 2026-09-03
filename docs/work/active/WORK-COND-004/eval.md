# Avaliação — WORK-COND-004

## O que prova corretude

O oracle central é a distinção entre **FATO de descoberta** (Checkpoint 1, `ConditionNameSurfaceDiscoveryTest` — 14 testes verdes, incluindo os fatos do round 2: nested qualifier, file declaration atrás do branch `inData`, granularidade de provenance e o contracaso local-DATA × GLOBAL-FILE) e **ORACLE** (Checkpoint 2, `ConditionNameSurfaceAstTest`). A corretude da implementação é provada quando a surface AST responde estruturalmente "o que foi escrito" para uma condition-name reference — baseName, qualification em ordem, subscripts como children, writtenText e spans por token — **no `DataReference` corrigido**, sem lookup, sem `grammarRule` como autoridade, sem inventar DATA a partir do branch ANTLR, sem ampliar o namespace do qualifier além do que o resolver atual sustenta e sem transformar `ContextualConditionTail` em CONDITION. A âncora normativa é COND-P08 + cap. 7 do IBM LR ("Resolution of names").

**Status dos oracles (Checkpoint 2):** CN-01..CN-12 e S4-BOUNDARY-01 estão **EXECUTABLE** em `ConditionNameSurfaceAstTest` e **PASS** (suíte completa verde; gates `fast`/`performance`/`semantic`/`full` passados). O contracaso de regressão `qualifiedLocalDataNameCollidesWithOuterGlobalFileNameAcrossPrograms` (COND-A08c) permanece **EXECUTABLE / PASS** em `ConditionNameSurfaceDiscoveryTest` com o alvo migrado para `UNSPECIFIED`, e COND-A08b (`dataQualifierBranchCanCarryAFileDeclaration`) idem. O semantic challenge pass da implementação (challenges A–J) está registrado em `state.md`.

## Classes positivas

- **CN-01 — simple condition-name:** `IF FLAG-OK` produz `DataReference{baseName="FLAG-OK"}` corrigido, sem clone e sem corrupção; a posição standalone é a estrutura (condition de `IfStatement`); nenhum `DataReference` substituto sintético.
- **CN-02 — qualification:** `IF FLAG-88 OF CUSTOMER` mantém `DataQualifier(OF, CUSTOMER)` estrutural com span próprio; único qualifier = última posição → target `UNSPECIFIED` (nunca DATA por branch, nunca DATA_OR_FILE que ampliaria a constraint sem o step 3).
- **CN-03 — nested qualification:** `IF FLAG-88 OF SUB-GRP OF GROUP-A` preserva os dois qualifiers na ordem escrita; não-final → `DATA` (grammar-provado por posição), final → `UNSPECIFIED`.
- **CN-04 — subscript:** `IF FLAG-88(I)` preserva `SubscriptGroup[(I)]` com o subscript como expression child própria e occurrence `SUBSCRIPT` própria (política pré-existente do collector, sem mudança de código).
- **CN-05 — qualification + subscript:** `IF FLAG-88 OF CUSTOMER(I, J)` mantém as duas dimensões sem perda; o grupo cobre o trecho escrito.
- **CN-06 — contextual contrast:** `A = B OR C` continua `ContextualConditionTail` com `DataReference` interno corrigido; `A = B OR FLAG-ON(IDX)` continua tail com interno completo.
- **CN-07 — no clone/no synthetic reference:** cada nome escrito produz exatamente um node/occurrence correspondente (baseName, cada qualifier, cada subscript); nenhum node sintético. Para `FLAG-88 OF CUSTOMER(I)`: exatamente FLAG-88 (CONDITION), CUSTOMER (QUALIFIER_COMPONENT), I (SUBSCRIPT) — sem duplicar, sem container, sem perder I.
- **CN-08 — provenance (Contrato A):** o node da referência cobre exatamente os tokens escritos; cada `DataQualifier` possui Meta próprio; cada `SubscriptGroup` cobre o grupo; cada subscript tem span próprio. O baseName NÃO possui Meta independente.
- **CN-09 — case metamorphic:** variação de caixa não altera topology.
- **CN-10 — alpha rename metamorphic:** renomear consistentemente nomes preserva topology.
- **CN-11 — qualifier namespace nunca por branch nem ampliado:** targets derivam exclusivamente da posição na estrutura da rule: não-final `DATA`, final `UNSPECIFIED`; o resolver consome `UNSPECIFIED` como `{DATA}` (constraint idêntica à atual). MNEMONIC permanece lacuna explícita (SPECIAL-NAMES não modelado).
- **CN-12 — nested qualification dentro do subscript:** `IF FLAG-88 OF CUSTOMER(SUB OF SUB-GROUP)` produz root qualifiers = `[CUSTOMER]` SOMENTE; o subscript é `DataReference(baseName="SUB", qualifiers=[OF SUB-GROUP])` próprio.
- **S4-BOUNDARY-01:** paths fora da superfície de condição (SET, `EVALUATE WHEN` selector, operandos de relation via `tableCall`) continuam com as shapes atuais e não são afetados.

## Classes negativas

- `grammarRule == "conditionNameReference"` não governa categoria semântica em nenhum ponto novo (o falso gap do collector continua como era, reservado ao Slice 5).
- qualification não fica armazenada somente em `writtenText`; subscripts não são descartados; qualifiers não são flattenados; qualifiers de subscripts internos não são promovidos à raiz.
- `ContextualConditionTail` não vira condition-name definitivo; nenhum reparse textual; nenhum clone do associated data reference; nenhuma occurrence sintética.
- nenhum `QualifierTarget.DATA` é atribuído por branch da grammar; nenhum node/contrato novo é criado sem informação de surface necessária (Erro L).
- nenhum node sintético para o token do nome-base (Contrato A).

## Classes ambíguas

1. Bare nominal com estado de abbreviation aberto: permanece `ContextualConditionTail` (interpretação DATA/INDEX/CONDITION aberta ao Slice 5).
2. Shapes grammar-only fora do formato IBM (`FLAG-88(I)(J)`, `FLAG-88(ALL)`): preservadas estruturalmente, sem alegação de validade (COND-N05).
3. DATA/FILE no último qualifier: a parse tree não distingue (`inFile` sombreado) — `UNSPECIFIED` na surface; o resolver o consome conservadoramente como `{DATA}` até `BACKLOG-RES-004`. MNEMONIC (IBM Format 2): não distinguível e não modelado — boundary documentada.
4. Qual dos qualifiers é a conditional variable e a validade da hierarquia/dimensões: post-binding / `ConditionValidation`.

## Casos adversariais

- **COND-A08 — qualification:** condition-name repetido e tornado único com `OF`/subscripts não pode depender de texto; exige estrutura (CN-02/03/05).
- **COND-A08b — qualifier FILE atrás do branch inData:** `FD CUSTOMER-FILE` + `88 FLAG-88` e `IF FLAG-88 OF CUSTOMER-FILE`: branch `inData`, declaração `Namespace.FILE` (fato caracterizado). Toda solução `inData ⇒ DATA` é rejeitada; o alvo é `UNSPECIFIED` e a resolução por file-name permanece `DECLARATION_NOT_FOUND` até `BACKLOG-RES-004`.
- **COND-A08c — colisão local-DATA × outer-GLOBAL-FILE (round 2):** OUTER `FD Q IS GLOBAL` com `88 C`; INNER `01 Q` com `88 C`; INNER `IF C OF Q`. Hoje `RESOLVED` (local, por exclusão acidental de namespace); com ampliação `{DATA, FILE}` sem o step 3 do IBM LR seria `UNSUPPORTED_DIALECT_OPTION`/`AMBIGUOUS` — regressão caracterizada em `qualifiedLocalDataNameCollidesWithOuterGlobalFileNameAcrossPrograms`. O correto IBM é o local em qualquer qualify mode (step 3).
- **Erro A — `conditionNameReference ⇒ CONDITION` definitivo:** oracles observam a shape da AST e a posição, nunca o `grammarRule`; o falso gap do collector permanece reservado ao Slice 5.
- **Erro B — qualification só em `writtenText`:** falha em CN-02/03.
- **Erro C — subscript descartado:** falha em CN-04/05.
- **Erro D — qualifier flattenado:** falha em CN-03.
- **Erro E — `ContextualConditionTail` convertido:** falha em CN-06.
- **Erro F — reparse textual:** nenhum consumidor reparseia `writtenText` (INV-AST-002).
- **Erro G — clone do associated data reference:** falha em CN-01/07 (node único por nome escrito; INV-AST-003).
- **Erro H — traversal por descendente rouba qualifier de subscript:** `nearestDescendants(conditionNameReference, qualifier)` produziria root qualifiers `[CUSTOMER, SUB-GROUP]` para `FLAG-88 OF CUSTOMER(SUB OF SUB-GROUP)` — falha em CN-12. O builder usa children diretos.
- **Erro I — mudança de política no resolver:** a única alteração permitida é o case `UNSPECIFIED → {DATA}` (policy-preserving). Falha qualquer implementação que altere candidate selection, namespace policy, scope walk, ambiguity handling, `orderedSubsequence`/`exactQualification` ou introduza `admissibleKind` novo.
- **Erro J — ampliar admissible qualifier introduz AMBIGUOUS/UNSUPPORTED indevido:** mapear `UNSPECIFIED` para `{DATA, FILE}` (ou atribuir `DATA_OR_FILE`) sem o step 3 converte o contracaso COND-A08c de `RESOLVED` em `UNSUPPORTED_DIALECT_OPTION`/`AMBIGUOUS` — falha. A ampliação pertence exclusivamente a `BACKLOG-RES-004`.
- **Erro K — outer GLOBAL vence local indevidamente:** qualquer resultado em que o candidate do programa contendo prevaleça sobre o local no contracaso COND-A08c falha (IBM step 3: local vence).
- **Erro L — criar nova abstraction sem informação semântica necessária:** reintroduzir `ConditionNameReference`/`NominalReference` (ou qualquer node/contrato novo) sem demonstrar uma informação de surface que a árvore corrigida + containers tipados não possuam falha — o node novo só codificaria a posição, já estrutural.
- **Erro M — Slice 5 depende novamente de grammarRule apesar de AST estrutural suficiente:** qualquer hand-off do Slice 5 que exija `grammarRule`, parent inspection ou reparse falha; o hand-off é por contexto estrutural carregado pelo próprio collector (pseudocódigo em `state.md`, round 2, challenge 8).

## Casos de regressão

- `SemanticConditionContextDiscoveryTest.characterizesDeclarationKindQualificationAndHomonymMatrix`: o variant `qualified-condition` (`A = B OR C OF G` com `88 C` sob `01 G`) permanece `RESOLVED/QUALIFIED_HIERARCHY_MATCH` sem nenhuma mudança de resolver além do case `UNSPECIFIED` (a constraint efetiva é a mesma). `qualified-data` permanece `INVALID_NAMESPACE_FOR_CONTEXT` (falso gap do collector preservado).
- `ConditionSurfaceAstTest` (Slice 3) permanece verde SEM migração de tipo (tail interno continua `DataReference`); os asserts de corrupção do discovery migram para a estrutura corrigida.
- `characterizesConditionNameSubscriptCorruption` migra: `IDX` passa a gerar occurrence `SUBSCRIPT` (recuperação estrutural pela política existente).
- `AstPreorderInvariantTest`, `assertActualProductsJoin`, `ArchitectureBoundaryTest` e snapshots permanecem verdes; coverage sem delta de métricas; baselines de corpus mudam apenas pela estrutura materializada.
- Grammar, `CobolReferenceResolver`, `ReferenceOccurrences`, `ReferenceResolution`, `ReferenceOccurrenceCollector` e `AstSnapshot` permanecem byte-identical.
- Resolver file-qualified: `IF FLAG-88 OF CUSTOMER-FILE` permanece `DECLARATION_NOT_FOUND` (como hoje) — a melhoria está bloqueada por `BACKLOG-RES-004`, explicitamente, e não é regressão.

## Propriedades/relações metamórficas

1. **M1 — case:** variar caixa de nomes não altera topology nem ordem estrutural.
2. **M2 — alpha rename:** renomear consistentemente condição/qualifiers/subscripts preserva topology.
3. **M3 — IN/OF equivalentes:** trocar `OF` por `IN` preserva posição/ordem dos qualifiers.
4. Determinismo e pre-order: IDs `0..N-1` sem duplicação (INV-AST-003, INV-DET-001).
5. Tail contextual metamórfico: substituir a declaração do nome (DATA vs CONDITION vs INDEX vs RENAMES vs ausente) não muda a surface.
6. **M4 — declaration substitution no qualifier:** a MESMA shape `FLAG-88 OF CUSTOMER-FILE` com a declaração do qualifier trocada (data-name vs file-name) não muda a surface AST — o target continua `UNSPECIFIED` (grammar-position); a diferença só aparece no binding (e, para file-name, fica bloqueada por `BACKLOG-RES-004`).

## Expectativas de escala

A construção corrigida percorre cada `conditionNameReference` uma única vez: `conditionName()`, `inData()` diretos e `conditionNameSubscriptReference()` em ordem linear no tamanho do context. Nenhum scan global, lookup, reparse ou passagem quadrática; complexidade `O(condition surface nodes)`. O collector não muda: a ocorrência de subscripts recuperados é linear pela política existente. O case novo no resolver é O(1). O gate `performance` continua sem threshold de hardware.
