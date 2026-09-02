# Avaliação — WORK-COND-004

## O que prova corretude

O oracle central é a distinção entre **FATO de descoberta** (Commit 1, `ConditionNameSurfaceDiscoveryTest` — 13 testes verdes, incluindo os três novos fatos desta rodada: nested qualifier dentro de subscript, file declaration atrás do branch `inData` e granularidade de provenance sem node de nome-base) e **ORACLE PROPOSTO** (Commit 2, `ConditionNameSurfaceAstTest`). A corretude da implementação é provada quando a surface AST responde estruturalmente "o que foi escrito" para uma condition-name reference — baseName, qualification em ordem, subscripts como children, writtenText e spans por token — sem lookup, sem `grammarRule` como autoridade, sem inventar DATA a partir do branch ANTLR e sem transformar `ContextualConditionTail` em CONDITION. A âncora normativa é COND-P08, com a tabela "Linguagem versus grammar" da `spec.md` (IBM Format 1: data-name/file-name; Format 2: mnemonic-name; "data-name subscript pode ser qualificado").

## Classes positivas

- **CN-01 — simple condition-name:** `IF FLAG-OK` produz `ConditionNameReference{baseName="FLAG-OK"}` sem qualifiers/subscripts; nenhum `DataReference` substituto.
- **CN-02 — qualification:** `IF FLAG-88 OF CUSTOMER` mantém `DataQualifier(OF, CUSTOMER)` estrutural com span próprio; a qualification não é string flat; único qualifier = última posição → target `DATA_OR_FILE` (grammar não distingue `inData`/`inFile` ali; nunca DATA por branch).
- **CN-03 — nested qualification:** `IF FLAG-OK OF SUB-GRP OF GROUP-A` preserva os dois qualifiers na ordem escrita; não-final → `DATA` (grammar-provado por posição), final → `DATA_OR_FILE`.
- **CN-04 — subscript:** `IF FLAG-OK(I)` preserva `SubscriptGroup[(I)]` com o subscript como expression child própria (e sua própria occurrence nominal).
- **CN-05 — qualification + subscript:** `IF FLAG-88 OF CUSTOMER(I, J)` mantém as duas dimensões sem perda; o grupo de subscripts cobre o trecho escrito.
- **CN-06 — contextual contrast:** `A = B OR C` continua `ContextualConditionTail`; `A = B OR FLAG-ON(IDX)` continua tail, agora com `nominalReference` estruturalmente completo (campo tipado `NominalReference`).
- **CN-07 — no clone/no synthetic reference:** cada nome escrito produz exatamente um node/occurrence correspondente (baseName, cada qualifier, cada subscript); nenhum node sintético. Para `FLAG-88 OF CUSTOMER(I)` as ocorrências escritas são exatamente: `FLAG-88` (CONDITION), `CUSTOMER` (QUALIFIER_COMPONENT), `I` (SUBSCRIPT) — sem duplicar `FLAG-88`, sem occurrence para container, sem perder `I`, sem contar `CUSTOMER` duas vezes.
- **CN-08 — provenance (Contrato A):** o node da referência cobre exatamente os tokens escritos; cada `DataQualifier` possui Meta próprio com span da qualification escrita (conector + alvo); cada `SubscriptGroup` cobre o grupo escrito e cada subscript expression tem span próprio. **O baseName NÃO possui Meta independente** — não existe node cujo span seja exatamente o token do nome-base; essa granularidade não possui consumidor e não justifica node sintético (INV-AST-003, CN-07, precedente `DataReference.baseName` String). INV-PROV-002: todo span exposto é exato.
- **CN-09 — case metamorphic:** variação de caixa não altera topology.
- **CN-10 — alpha rename metamorphic:** renomear consistentemente nomes preserva topology.
- **CN-11 — qualifier namespace nunca por branch:** o target de cada qualifier deriva exclusivamente da posição na estrutura da rule (`inData* inFile?`): não-final `DATA`, final `DATA_OR_FILE`. Nenhuma implementação pode fechar `DATA` "porque ANTLR escolheu `inData`". MNEMONIC permanece lacuna explícita (SPECIAL-NAMES não modelado): o qualifier escrito é preservado como `DATA_OR_FILE` e a resolução permanece não suportada — sem invenção de namespace.
- **CN-12 — nested qualification dentro do subscript:** `IF FLAG-88 OF CUSTOMER(SUB OF SUB-GROUP)` produz root qualifiers = `[CUSTOMER]` SOMENTE; o subscript é `DataReference(baseName="SUB", qualifiers=[OF SUB-GROUP])` próprio. `SUB-GROUP` pertence ao subscript interno e NUNCA vira qualifier da referência raiz.
- **S4-BOUNDARY-01:** paths fora da superfície de condição (SET, `EVALUATE WHEN` selector, operands de relation via `tableCall`) continuam com as shapes atuais e não são afetados.

## Classes negativas

- `grammarRule == "conditionNameReference"` não governa categoria semântica em nenhum ponto novo (permanece somente provenance; o falso gap do collector continua como era, reservado ao Slice 5).
- qualification não fica armazenada somente em `writtenText`; subscripts não são descartados; qualifiers não são flattenados em `"A OF B OF C"`; qualifiers de subscripts internos não são promovidos à raiz.
- `ContextualConditionTail` não vira condition-name definitivo por causa da grammar; nenhum reparse textual recupera estrutura; nenhum clone do associated data reference representa a condition-name.
- nenhum lookup, symbol ID, declaration link ou `admissibleKinds` entra na AST; nenhuma occurrence sintética é criada; nenhum `QualifierTarget.DATA` é atribuído por branch da grammar.
- nenhum node sintético para o token do nome-base (Contrato A): a precisão contratual é node da referência + qualifiers + grupos/subscripts.

## Classes ambíguas

1. Bare nominal com estado de abbreviation aberto: permanece `ContextualConditionTail` (interpretação DATA/INDEX/CONDITION aberta ao Slice 5); o campo interno é o contrato neutro `NominalReference`.
2. Shapes grammar-only fora do formato IBM (`FLAG-OK(I)(J)` com dois grupos de subscripts; `FLAG-OK(ALL)`): preservadas estruturalmente, sem alegação de validade (classe COND-N05).
3. DATA/FILE no último qualifier: a parse tree não distingue (`inFile` sombreado por `inData` para tokens `cobolWord`) — modelado como `DATA_OR_FILE` (enum pré-existente). MNEMONIC (IBM Format 2): não distinguível e não modelado — boundary documentada, resolução não suportada.
4. Qual dos qualifiers é a conditional variable e a validade da hierarquia/dimensões: post-binding / `ConditionValidation`, fora deste slice.

## Casos adversariais

- **COND-A08 — qualification:** condition-name repetido e tornado único com `OF`/subscripts não pode depender de texto; exige estrutura (CN-02/03/05).
- **COND-A08b — qualifier FILE atrás do branch inData:** programa com `FD CUSTOMER-FILE` + `88 FLAG-88` e `IF FLAG-88 OF CUSTOMER-FILE`: o branch é `inData` e a declaração é `Namespace.FILE` (fato caracterizado em `dataQualifierBranchCanCarryAFileDeclaration`). Toda solução `inData ⇒ DATA` é rejeitada; o último qualifier é `DATA_OR_FILE`.
- **Erro A — `conditionNameReference ⇒ CONDITION` definitivo:** oracles observam a shape da AST (node tipado em posição standalone; `ContextualConditionTail` em posição aberta), nunca o `grammarRule`; a implementação que fecha o kind pelo rule name continua falhando no tail contextual.
- **Erro B — qualification só em `writtenText`:** falha em CN-02/03 (exige `DataQualifier` children).
- **Erro C — subscript descartado:** falha em CN-04/05 (exige `SubscriptGroup` com expression child).
- **Erro D — qualifier flattenado:** falha em CN-03 (dois qualifiers como children distintos na ordem).
- **Erro E — `ContextualConditionTail` convertido:** falha em CN-06 (tail permanece tail com node nominal interno estruturado).
- **Erro F — reparse textual:** nenhum consumidor reparseia `writtenText`; a estrutura vem de contexts/tokens (INV-AST-002).
- **Erro G — clone do associated data reference:** falha em CN-01/07 (node único por nome escrito; identidade/pre-order de INV-AST-003).
- **Erro H — traversal por descendente rouba qualifier de subscript:** uma implementação `nearestDescendants(conditionNameReference, qualifier)` produz root qualifiers `[CUSTOMER, SUB-GROUP]` para `FLAG-88 OF CUSTOMER(SUB OF SUB-GROUP)` — falha em CN-12. O builder deve navegar pelos children diretos (`conditionName()`, `inData()`, `conditionNameSubscriptReference()`) e nunca descer para dentro de `conditionNameSubscriptReference` em busca de qualifiers da raiz.
- **Erro I — mudança de política no resolver:** a adaptação do `DataAndIndexReferenceResolver` é permitida SOMENTE como consumo estrutural do contrato `NominalReference` (baseName/qualifiers). Falha qualquer implementação que altere candidate selection, namespace policy, scope walk, ambiguity handling, `orderedSubsequence`/`exactQualification` ou introduza `admissibleKind` novo. O único delta aceito e documentado é o input fidelity do último qualifier (`DATA_OR_FILE`), que faz root references qualificadas por file-name resolverem quando inequívocas.

## Casos de regressão

- `SemanticConditionContextDiscoveryTest.characterizesDeclarationKindQualificationAndHomonymMatrix`: o variant `qualified-condition` (`A = B OR C OF G` com `88 C` sob `01 G`) é o baseline de invariância do resolver — deve continuar `RESOLVED/QUALIFIED_HIERARCHY_MATCH` após a adaptação estrutural (Challenge 7: qualified condition-name que já funcionava sem subscripts não pode regredir). O variant `qualified-data` continua `INVALID_NAMESPACE_FOR_CONTEXT` (o falso gap do collector permanece). Accessors `.baseName()`/`.qualifiers()` continuam compilando via `NominalReference`.
- `ConditionSurfaceAstTest` (Slice 3) permanece verde com migração mecânica dos filters `instanceof DataReference` sobre tail interno; `A = B OR C` e toda a superfície de conectores/NOT/parênteses inalterados.
- `SemanticConditionContextDiscoveryTest.characterizesConditionNameSubscriptCorruption` migra: `IDX` passa a gerar occurrence `SUBSCRIPT` (recuperação de perda estrutural, política inalterada).
- `AstPreorderInvariantTest`, `AstBoundaryTestSupport.assertActualProductsJoin`, `ArchitectureBoundaryTest` e `AstSnapshot`/coverage continuam verdes; métrica `dataReferences` do coverage deixa de contar condition surfaces (elas não são data references) com diff de baseline explicado; `grammar-rule-manifest.tsv` byte-identical.
- Grammar, `CobolReferenceResolver`, `ReferenceOccurrences`, `ReferenceResolution` permanecem byte-identical no delta.
- Resolver file-qualified: `IF FLAG-88 OF CUSTOMER-FILE` passa de `DECLARATION_NOT_FOUND` para `RESOLVED` quando inequívoco (correção de input fidelity, comportamento IBM); a occurrence `QUALIFIER_COMPONENT` do file-name permanece sem candidate (boundary fora do slice).

## Propriedades/relações metamórficas

1. **M1 — case:** variar caixa de nomes não altera topology nem ordem estrutural.
2. **M2 — alpha rename:** renomear consistentemente condição/qualifiers/subscripts preserva topology.
3. **M3 — IN/OF equivalentes:** trocar `OF` por `IN` preserva a posição/ordem dos qualifiers, mudando somente o connector.
4. Determinismo e pre-order: IDs `0..N-1` sem duplicação (INV-AST-003, INV-DET-001).
5. Tail contextual metamórfico: substituir a declaração do nome (DATA vs CONDITION vs INDEX vs RENAMES vs ausente) não muda a surface (a decisão permanece aberta; Challenge 3).
6. **M4 — declaration substitution no qualifier:** a MESMA shape escrita `FLAG-88 OF CUSTOMER-FILE` com a declaração do qualifier trocada (data-name vs file-name vs mnemonic-name) não muda a surface AST — o target continua `DATA_OR_FILE` (grammar-position) e a diferença só aparece no binding.

## Expectativas de escala

A construção do node percorre cada `conditionNameReference` uma única vez: `conditionName()`, `inData()` diretos e `conditionNameSubscriptReference()` são visitados em ordem linear no tamanho do context. Nenhum scan global, lookup, reparse ou passagem quadrática; a complexidade permanece `O(condition surface nodes)` em tempo e memória, e a ocorrência de subscripts recuperados é linear no número de subscripts escritos. O gate `performance` continua sem threshold de hardware. A adaptação do resolver não altera a complexidade de `resolveDataOccurrence` (mesmos passos, mesma indexação).
