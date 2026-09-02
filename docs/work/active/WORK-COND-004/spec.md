# Spec — WORK-COND-004

## Problema

Uma condition-name reference verdadeira (simple condition escrita como condition-name, com ou sem qualification e subscripts) não possui representação estrutural própria na surface AST. O lowering atual de `conditionNameReference` reutiliza `Ast.DataReference` e, quando subscripts são escritos, corrompe a estrutura:

- o `baseName` passa a ser o nome do primeiro subscript (o lowering captura o primeiro `qualifiedDataName` descendente, que é o subscript), e o nome da condition é perdido da estrutura;
- `subscriptGroups` fica sempre vazio — os subscripts sobrevivem apenas em `writtenText`;
- quando o nome-base é sequestrado pelo subscript, a qualification também é perdida;
- quando o subscript é ele próprio qualificado (`FLAG-OK OF CUSTOMER(SUB OF SUB-GROUP)`), o qualifier do subscript é roubado para a referência raiz e o qualifier raiz some (fato caracterizado por `ConditionNameSurfaceDiscoveryTest.nestedQualifierInsideSubscriptBelongsToTheSubscriptNotTheReference`);
- a única evidência estrutural de que o uso nominal é uma condition-name surface (e não uma data-name reference) é `meta.origin().grammarRule()`, que ADR-0012 e INV-COND-001 proíbem como autoridade semântica.

`ContextualConditionTail` herda a mesma corrupção em seu `nominalReference` interno. Este slice NÃO corrige o bug original de WAUX-371 (a política nominal/contextual do collector e do resolver pertence ao Slice 5).

## Objetivo

Dar à surface AST uma representação estrutural completa, pré-binding e lossless para condition-name references verdadeiras: nome nominal escrito, qualification IN/OF em ordem, subscripts como children tipados, writtenText e provenance por token, em node tipado distinto de `DataReference`. A AST continua respondendo apenas "o que foi escrito e qual é sua estrutura nominal", nunca "a qual declaração isso resolve".

Para cada proposta de modelagem esta spec respondeu, obrigatoriamente, três perguntas por fase (Parse Tree → Surface AST → `Ast.children`/identidade/provenance → `ReferenceOccurrenceCollector` → `ReferenceOccurrences` → `DataAndIndexReferenceResolver` → `ReferenceResolution` → snapshots/coverage/consumidores):

1. **Que informação esta fase realmente conhece?** A surface conhece tokens/contexts (nome, conectores, alvos de qualification escritos, subscripts escritos e seus spans); o collector conhece roles e policies pré-existentes; o resolver conhece baseName, qualifiers e kinds admissíveis.
2. **Que informação ela ainda NÃO conhece?** A surface não conhece declaration kind, namespaces semânticos (DATA/FILE/MNEMONIC), escopos nem a variável condicional; o resolver não conhece subscripts (nunca os consumiu) nem a intenção gramatical do qualifier além do que a AST preserva.
3. **O que os consumidores downstream assumem sobre este produto?** O resolver assume que a occurrence referencia um node do qual consegue extrair baseName + qualifiers (hoje, por `instanceof DataReference`); o collector assume traversal estrutural via `Ast.children`; snapshots assumem pre-order canônico e Meta por node.

A solução foi então falsificada adversarialmente (challenges 1–10, registrados em `state.md`). A conclusão está na seção "Decisão de modelagem".

## Domínio de entrada suportado

- `simpleCondition → conditionNameReference` em posição standalone (estado de abbreviation fechado): `IF FLAG-OK`, operandos de `AND`/`OR`, sob `NOT`, dentro de `GroupedCondition`;
- `conditionNameReference` em posição de bare nominal tail com estado de abbreviation aberto — permanece `ContextualConditionTail`, com o `nominalReference` interno estruturalmente completo;
- shapes gramaticais aceitas pela rule `conditionNameReference`: condition-name simples; qualificado por `inData+` (`OF`/`IN` data-name, repetível em ordem); subscript lists pós-qualificação (`conditionNameSubscriptReference*`, com `COMMACHAR?` entre subscripts);
- forms do `subscript` aceitas pela grammar: integer literal, `qualifiedDataName` (inclusive qualificado — o subscript pode ser ele próprio uma referência qualificada, forma IBM válida), `indexName`, relative subscript (`+`/`-` integer) e aritmética;
- paths fora da superfície de condição (`SET`, selectors de `EVALUATE`, operandos de relation) continuam usando as shapes atuais de `identifier`/`qualifiedDataName`/`tableCall` — já estruturalmente completas — e ficam fora do escopo de mudança.

Fora do domínio modelado: fallbacks `PreservedExpression` e `PreservedStatement` continuam válidos (ex.: `SEARCH WHEN`, `abbreviation` recursiva).

## Classes semânticas

1. **Contrato nominal estrutural compartilhado** (`Ast.NominalReference`, sealed, implementado por `DataReference` e pelo node novo): `baseName`, `writtenText`, `qualifiers`, `subscriptGroups` e `meta`. Não é interface "por elegância": há repetição real de contrato — o resolver extrai exatamente `baseName`+qualifiers, o collector percorre qualifiers+subscripts e o snapshot rotula por `writtenText` para BOTH shapes nominais. O contrato reduz o acoplamento incidental de consumidores nominais-estruturais ao tipo concreto `DataReference`, sem antecipar arquitetura (nenhum consumidor novo é inventado).
2. **Condition-name reference estruturada** (`Ast.ConditionNameReference implements Expression, NominalReference`): `baseName`, qualification estrutural e subscripts estruturais; a mesma classe cobre condition-name simples, qualificado e subscriptado — diferenças de cardinalidade estrutural, não de classe. O nome do node identifica a **shape de superfície** escrita (a rule `conditionNameReference`), nunca a classe semântica: a AST não decide DATA/INDEX/CONDITION e o node não carrega kind, lookup ou declaration link. `IF TBL(I)` e `IF DATA-X OF GROUP-A` (grammar-swallowed, fonte IBM inválido como condição) produzem o MESMO node sem alegação de validade (classe COND-N05).
3. **Qualification** (`DataQualifier` reutilizado): connector `OF`/`IN`, target por posição gramatical (ver "Linguagem versus grammar"), reference nominal do qualifier e writtenText; ordem escrita preservada. A classificação de target NÃO é fechada pelo branch ANTLR escolhido (`inData` não prova DATA).
4. **Subscripting** (`SubscriptGroup` reutilizado): cada `conditionNameSubscriptReference` escrito vira um grupo com seus subscripts como expression children; um subscript `qualifiedDataName` preserva seus próprios qualifiers (nunca promovidos à referência raiz); subscripts são nominais/expressões por si e preservam spans próprios.
5. **ContextualConditionTail**: permanece a alternativa contextual binding-dependent; seu `nominalReference` interno passa ao tipo do contrato compartilhado `NominalReference` (instância concreta: `ConditionNameReference`) — o campo não fecha nada e continua sem alegação DATA/INDEX/CONDITION.
6. **DataReference**: permanece exclusivo dos paths de data-name/identifier (`qualifiedDataName`, `tableCall`, SET, EVALUATE, operandos de relation) e dos qualifier payloads internos; continua o único a carregar `referenceModification` e `understanding`.

## Premissas

- `LANGUAGE_GUARANTEED` (IBM Enterprise COBOL for z/OS 6.4 Language Reference, SC27-8713-03, atualização de 28/06/2024, cap. 8 "Condition-name"/"Subscripting" e cap. 27 "Condition-name condition"): o formato de referência escrito é `condition-name-1 [IN|OF data-name-1]... [IN|OF file-name-1] [(subscript)...]` (Format 1 — DATA DIVISION) ou `condition-name-1 [IN|OF mnemonic-name-1]` (Format 2 — SPECIAL-NAMES). Qualification vem antes dos subscripts; `data-name-1` pode ser record-name; `file-name-1` deve ser identificado por FD/SD, é único no programa e só pode ocupar a última posição da cadeia; a qualification usa a hierarquia da conditional variable; quando a conditional variable exige subscripting, a condition-name exige a mesma combinação; condition-name definido no SPECIAL-NAMES paragraph é global.
- `LANGUAGE_GUARANTEED` (mesma fonte, cap. 8 "Subscripting"): subscript é `integer | ALL | data-name [+|- integer] | index-name [+|- integer]`; `ALL` não pode ser especificado para condition-name; **data-name subscript pode ser qualificado** (fonte do oracle nested-subscript); o número de subscripts deve igualar as dimensões da tabela — validação post-binding/`ConditionValidation`.
- `ARCHITECTURE_GUARANTEED` (ADR-0012): a AST é produto de superfície anterior ao binding; não decide DATA/INDEX/CONDITION, não materializa lookup, candidates, predicates herdados ou expansão.
- `ARCHITECTURE_GUARANTEED` (INV-AST-002/003): estrutura deriva de contexts/tokens; todo node é alcançado exatamente uma vez em pre-order canônico; `grammarRule` permanece somente provenance/debug. O builder do node novo usa SOMENTE children diretos do context (`conditionName()`, `inData()`, `conditionNameSubscriptReference()`) — nunca `nearestDescendants`/primeiro descendente, que roubariam qualifiers de subscripts internos (Erro H).
- `OBSERVED_IN_CURRENT_GRAMMAR` (fato de parse tree, não regra COBOL): `dataName`, `fileName` e `mnemonicName` são todos `cobolWord` (mesmo conjunto de tokens); por isso `inFile` e `inMnemonic` são sombreados por `inData` e inalcançáveis na parse tree — o branch `inData` NÃO distingue DATA de FILE nem de MNEMONIC. A única informação de posição que sobrevive é a estrutura estática da rule: as posições não-finais só têm slots `inData*`; a última posição tem slot `inFile?`. Consequência do modelo: qualifiers `0..n-2` → `QualifierTarget.DATA` (grammar-provado por posição), último qualifier → `QualifierTarget.DATA_OR_FILE` (grammar não distingue `inData`/`inFile` ali) — mesmo precedente já existente em `qualifiedDataNameFormat1`. Nenhum `QualifierTarget.DATA` é atribuído "porque ANTLR escolheu inData".
- `BOUNDED_UNSUPPORTED` (MNEMONIC): qualifier de mnemonic-name (IBM Format 2) chega à parse tree como `inData` e não é distinguível; o modelo não possui namespace MNEMONIC (`SymbolTable.Namespace` = PROGRAM/DATA/PROCEDURE/FILE) nem coleta SPECIAL-NAMES — a surface preserva o qualifier escrito como `DATA_OR_FILE` (última posição) e a resolução de condition-names de SPECIAL-NAMES permanece não suportada (sem candidate, como hoje). Lacuna documentada, não informação inventada.
- `UNCERTAIN`/post-binding: qual qualifier é a conditional variable, se a cadeia respeita a hierarquia e se os subscripts correspondem às dimensões — pertencem a binding/`ConditionValidation`, não à surface.
- `PROVENANCE CONTRACT A` (decisão do Finding 4): `ConditionNameReference.meta` = span da referência completa; `DataQualifier.meta` = span de cada qualification escrita; `SubscriptGroup.meta` = span do grupo; subscript expression tem meta própria; **o nome-base NÃO possui Meta independente** (sem node sintético; precedente: `DataReference.baseName` é `String`). O oracle CN-08 foi reescrito para este contrato; INV-PROV-002 continua satisfeito (todo span exposto é exato; nada aproximado é afirmado).

## Linguagem versus grammar

| Forma COBOL | IBM permite? | Grammar branch observado | Informação semanticamente comprovada |
| --- | --- | --- | --- |
| `condition-name OF data-name` | Sim (Format 1; data-name pode ser record-name) | `inData` | escrita; target DATA somente se o qualifier NÃO é a última posição ou a grammar o prova por posição |
| `condition-name IN data-name` | Sim (IN/OF equivalentes) | `inData` | idem |
| `condition-name OF file-name` | Sim (Format 1; FD/SD, última posição da cadeia) | `inData` (inFile sombreado) | escrita; a parse tree NÃO prova DATA — última posição → DATA_OR_FILE |
| `condition-name IN file-name` | Sim | `inData` (inFile sombreado) | idem |
| `condition-name OF mnemonic-name` | Sim (Format 2, SPECIAL-NAMES) | `inData` (inMnemonic sombreado) | escrita; MNEMONIC não distinguível e não modelado — bounded, resolução não suportada |
| `condition-name OF data-name(subscript)` | Sim (subscripts após a cadeia de qualification) | `inData` + `conditionNameSubscriptReference` | subscripts pertencem à referência (attach estrutural da superfície) |
| `condition-name(subscript) OF data-name` | Não (formato IBM) | grammar-rejeitado | rejeição sintática |
| `condition-name OF data-name(sub OF sub-group)` | Sim ("data-name-3 can be qualified") | `inData` raiz + `qualifiedDataName` dentro do subscript | o qualifier interno pertence ao subscript, NÃO à referência raiz |
| `condition-name OF data-name OF file-name` | Sim (cadeia hierárquica; file-name por último) | dois `inData` | posições: não-final → DATA; final → DATA_OR_FILE |

Challenge do Finding 2 (falsificação de `inData ⇒ DATA`): programa com `FD CUSTOMER-FILE` + `88 FLAG-OK` dentro do record e uso `IF FLAG-OK OF CUSTOMER-FILE` — parse branch `inData`, declaração `Namespace.FILE`. Fato caracterizado em `dataQualifierBranchCanCarryAFileDeclaration`. Consequência: toda modelagem que fecha DATA pelo branch é rejeitada.

## Decisão de modelagem

Alternativas avaliadas (detalhes e challenges em `state.md` "Semantic challenge pass"):

| Alternativa | Surface fidelity | Resolver compatibility | Ambiguity | Incisão | Resultado |
| --- | --- | --- | --- | --- | --- |
| A — node independente `ConditionNameReference` | alta (1 node por surface) | quebra (resolver cai em `writtenText` como baseName; qualification ignorada) — exige adaptação | standalone ok; tail re-tipado com nome que sugere CONDITION | média | rejeitada: duplica o contrato nominal (baseName/qualifiers/subscripts/writtenText/meta) sem compartilhar, e o tail ganha tipo com alegação de nome indesejada |
| B — wrapper estrutural + payload `DataReference` interno | baixa (2 nodes por surface; payload sem context próprio) | perfeita por construção | ok | mínima | rejeitada: o payload interno é node sintético (CN-07/Erro G) com span duplicado (INV-PROV-002 em tensão); a occurrence continuaria rotulada `DataReference` nos produtos — o "node tipado próprio" não seria observável; resolver intacto NÃO compensa a perda de identidade/provenance |
| C — contrato nominal compartilhado + node de superfície | alta (1 node por surface; contrato único) | preservada por adaptação estritamente estrutural comprovada | tail mantém campo `NominalReference` neutro; standalone preserva shape | maior (interface sealed + re-tipo de consumidores nominais) | **escolhida** |
| D — apenas corrigir o lowering de `DataReference` (sem node) | alta | perfeita | ok | mínima | rejeitada: `grammarRule` continuaria sendo a única evidência estrutural da surface; o Slice 5 não conseguiria remover o acoplamento `grammarRule ⇒ CONDITION` sem parent inspection ou reparse — viola o hand-off exigido |

A decisão deriva dos critérios `source fidelity + phase ownership + ambiguity preservation + downstream compatibility + nominal resolution correctness + provenance + identity + minimal semantic surface`:

- **C satisfaz todos**: fidelidade de um node por superfície escrita; ownership de fase preservado (nenhum conhecimento de binding entra); ambiguidade preservada (campo do tail tipado pelo contrato neutro; standalone identifica a shape, não o kind); compatibilidade downstream provada (tabela de consumer impact); corretude nominal mantida por adaptação estrutural com prova de invariância (Erro I); provenance exata por token sem node sintético; identidade/pre-order canônicos; superfície semântica mínima (o contrato evita duplicar os 5 campos em dois records e remove o acoplamento incidental do resolver ao tipo concreto).
- **A falha** por duplicar o contrato e por re-tipar o tail com um nome que afirma mais do que a fase sabe.
- **B falha** por criar node sintético sem correspondência de context, duplicar span/provenance e esconder o node tipado nos produtos.
- **D falha** por não entregar ao Slice 5 uma evidência estrutural substituta do `grammarRule`.

## Comportamento esperado

- `IF FLAG-OK` → `ConditionNameReference{baseName="FLAG-OK", qualifiers=[], subscriptGroups=[]}` com span dos tokens escritos; nenhum `DataReference` substituto.
- `IF FLAG-OK OF GROUP-A` → `ConditionNameReference{baseName="FLAG-OK", qualifiers=[OF GROUP-A]}`; único qualifier ocupa a última posição → target `DATA_OR_FILE` (grammar não distingue `inData`/`inFile` ali).
- `IF FLAG-88 OF CUSTOMER(I, J)` → qualification e dois subscripts coexistem; o grupo de subscripts cobre o trecho `(I, J)` escrito; target do qualifier `CUSTOMER` = `DATA_OR_FILE`.
- `IF FLAG-OK OF SUB-GRP OF GROUP-A` → dois qualifiers na ordem escrita; `SUB-GRP` (não-final) → `DATA`; `GROUP-A` (final) → `DATA_OR_FILE`.
- `IF FLAG-OK OF CUSTOMER(SUB OF SUB-GROUP)` → root qualifiers = `[CUSTOMER]` somente; o subscript é `DataReference(baseName="SUB", qualifiers=[OF SUB-GROUP])` próprio — `SUB-GROUP` NUNCA vira qualifier da referência raiz (Erro H).
- `A = B OR C` → `ContextualConditionTail` cujo `nominalReference` é `ConditionNameReference{baseName="C"}` (campo tipado `NominalReference`); nenhuma decisão DATA/INDEX/CONDITION.
- `A = B OR FLAG-ON(IDX)` → tail com `nominalReference` estruturalmente completo (baseName `FLAG-ON`, subscript `IDX`).
- `NOT FLAG-OK`, `FLAG-OK AND A = B`, `(FLAG-OK)` → o node novo participa das shapes existentes (`NegatedCondition`, `LogicalCondition`, `GroupedCondition`) sem mudança de topology além da substituição do node nominal.
- Forma aceita pela grammar sem suporte modelado: `PreservedExpression` fail-closed, como hoje.

## Comportamento diante de incerteza

- A distinção condition-name surface versus contextual tail é posicional/estrutural (estado de abbreviation aberto), nunca por `grammarRule`.
- DATA/FILE no último qualifier permanece incerto pela parse tree e é modelado como `DATA_OR_FILE` (enum pré-existente; sem novo valor); MNEMONIC é lacuna documentada e bounded (SPECIAL-NAMES não modelado; resolução não suportada).
- Subscript `ALL`, shapes grammar-only (`FLAG-OK(I)(J)`) e validação de hierarquia/dimensões permanecem sem decisão nesta fase: a surface preserva o que foi escrito e a validação continua post-binding (`ConditionValidation`).
- O falso gap do collector (`grammarRule == conditionNameReference ⇒ CONDITION`) permanece observável após este slice; é defeito reservado ao Slice 5.
- Nenhuma validade de tipo é afirmada; nenhum candidate é inventado.

## Fora de escopo

Lookup nominal, symbol IDs, declaration links, `admissibleKinds` definitivos, expansão de predicates herdados, `ConditionSemantics`, `ConditionValidation`, ocorrências contextuais (Slice 5), política nominal do collector/resolver, `SEARCH WHEN` (Slice 6), CFG/dataflow, alteração de grammar, alteração do manifesto de coverage, mudanças nos paths `identifier`/`qualifiedDataName`/`tableCall` (SET/EVALUATE/relation operands) e resolução de qualifier occurrences de file-names (a occurrence `QUALIFIER_COMPONENT` de um file-name continua sem candidate, como hoje). A única exceção ao "resolver intocado" é a adaptação estrutural descrita abaixo — política de resolução inalterada.

## Consumer impact analysis

Classificação: **A** = realmente DATA-specific; **B** = nominal-reference structural; **C** = incidental coupling ao tipo atual; **D** = fora do Slice 4.

| Consumer | Assumption atual | Quebra com node novo? | Semântica ou mecânica? | Ação necessária |
| --- | --- | --- | --- | --- |
| `DataAndIndexReferenceResolver` (`baseName` L413, `qualifiedReference` L92, `applyQualification` L186, `qualifyExtend` L192, `qualifierConstraints`) | a occurrence referencia `DataReference`; extrai baseName + qualifiers por `instanceof` | **SIM** — baseName cai para `occurrence.writtenText()` (`"FLAG-88 OF CUSTOMER"`), `qualifiedReference=false`, qualification ignorada, `qualifyExtend` vazio → regressão semântica de binding | Semântica | **B** — adaptação estrutural: consumir `NominalReference` (mesmos campos, mesmo algoritmo). Sem mudança de política (prova no Erro I). Sai de `must_not_change` com anotação `STRUCTURAL ADAPTATION ONLY — NO RESOLUTION POLICY CHANGE` |
| `ReferenceOccurrenceCollector` (`visit` L47, `addDataReference`) | surface de condição É `DataReference` (occurrence raiz + qualifiers + subscripts) | **SIM** — sem branch, a occurrence CONDITION raiz some e os children seriam percorridos com roles errados | Semântica | **B** — branch para `NominalReference` reusando a lógica extraída de `addDataReference`: mesma política (falso gap `grammarRule` preservado), mesmos roles (`QUALIFIER_COMPONENT`, `SUBSCRIPT`), mesmos admissibleKinds |
| `Ast.children` (branch `DataReference`) | children = qualifiers + subscriptGroups (+modifier) | **SIM** — node novo sem branch fica invisível ao pre-order | Mecânica | **B** — novo branch: qualifiers + subscriptGroups (ordem escrita); identidade/pre-order de INV-AST-003 |
| `AstScopeIndex` | usa `Ast.children` | indireto (depende do branch acima) | Mecânica | **B** — nada além do branch de children |
| `AstSnapshot` (label L128/L145, attributes L229, `expressionLabel` L283) | `instanceof DataReference` para label/attributes; tail usa `nominalReference().writtenName()` | não-crash; label cai no default (`getClass().getSimpleName()`), attributes vazias, tail label não compila contra o contrato | Mecânica/apresentação | **B** — novo branch (baseName/writtenText); tail label usa `writtenText()` do contrato |
| `CoverageSnapshot` (metrics `dataReferences*`) | conta condition surfaces como `DataReference` | métrica `dataReferences` cai; baselines mudam | Mecânica (métrica de apresentação) | **C** — documentar delta; baselines ajustadas no Commit 2 com diff explicado; nenhum finding de coverage novo |
| `CicsIntrinsicClassifier` (`instanceof DataReference` + `hasSupportedShape`) | raiz classificável precisa ser `DataReference` com shape CICS | não quebra (condition surfaces não são CICS intrinsics; hoje o baseName sequestrado já não casa `SUPPORTED_BASE_NAMES` = DFHRESP/DFHVALUE) | Incidental | **C** — nenhuma ação |
| `CobolReferenceResolver.resolveFile` L220 | `FileReference`/`DataReference` baseName | não (occurrence FILE nunca referencia o node novo; qualifier payloads continuam `DataReference`) | — | **D** — nenhuma ação (permanece `must_not_change`) |
| `SymbolTableBuilder.addRelation` L145 | `DataReference`/`IndexReference` writtenText em relações declarativas | não (clauses de dados não produzem o node novo) | — | **D** — nenhuma ação |
| `AstBuilder` (`dataReference` via `firstDescendant(qualifiedDataName)`) | fonte da corrupção atual | — | — | **B** — novo helper `conditionNameReference(ctx)` com children diretos; `buildBareNominal` usa o helper nos dois ramos; `visitConditionNameReference` uniforme; `dataReference`/`tableReference` intocados |
| `Ast` (`Node`/`Expression` sealed `permits`) | — | node novo precisa entrar nos `permits` | Mecânica | **B** — `NominalReference` no permits de `Node`; `ConditionNameReference` no permits de `Expression` |
| `ConditionSurfaceAstTest`/`SemanticConditionContextDiscoveryTest`/`ConditionNameSurfaceDiscoveryTest` | tail inner é `DataReference` (accessor/`instanceof`) | **SIM** nos filters `instanceof DataReference` e nos asserts que caracterizam a corrupção atual | Mecânica (testes) | **B** — migração mecânica no Commit 2 (accessors `baseName()`/`qualifiers()` continuam compilando via contrato); discovery tests que caracterizam o estado pré-fix migram para caracterizar a nova surface |
| `AstSemanticBoundaryCharacterizationTest`/`AstBoundaryTestSupport.assertActualProductsJoin`/`AstPreorderInvariantTest` | traversal genérico via `Ast.children` | não | — | **D** — nenhuma ação (cobertos pelo branch de children) |
| `SemanticModelBaselineCharacterizationTest`/`DynamicCallVariantTest`/`NominalReferenceAstTest`/`ReferenceResolutionBaselineCharacterizationTest` | data paths (CALL/GO TO/corpus relation operands — `ACCTSIDI OF CACTUPAI` é relation operand) | não (fora da superfície de condição) | — | **D** — nenhuma ação |

**Prova da adaptação do resolver (Erro I — invariância):** candidate selection permanece igual (mesmos `compatibleCandidates`/`localAndInheritedGlobal`); namespace policy igual; scope igual; ambiguity igual (INV-RES-001); qualification algorithm igual (`orderedSubsequence`/`exactQualification`); nenhum novo `admissibleKind` (o mapeamento `DATA_OR_FILE → {DATA, FILE}` pré-existe no collector E no resolver). Único delta de resolução documentado e esperado: **root reference de condition-name qualificada por file-name** passa de `DECLARATION_NOT_FOUND` (input inventava `DATA`) para `RESOLVED/QUALIFIED_HIERARCHY_MATCH` quando inequívoca — comportamento IBM que o modelo atual não consegue expressar; todo caso que já resolvia continua resolvendo identicamente (o constraint `DATA` é subconjunto de `{DATA, FILE}`). A occurrence `QUALIFIER_COMPONENT` do file-name permanece sem candidate (boundary pré-existente, fora do slice).

## Regras de domínio relacionadas

`docs/domain/conditional-expressions.md` (autoridade IBM registrada; COND-P08), `docs/domain/semantic-ast.md`, `docs/domain/provenance.md`, `docs/evals/conditional-expression-oracles.md` (COND-P08, COND-A08, COND-N05).

## ADRs/invariantes relacionados

ADR-0003, ADR-0005, ADR-0009, ADR-0012 (Accepted); INV-AST-001, INV-AST-002, INV-AST-003, INV-PROV-002, INV-COND-001, INV-COND-002, INV-RES-001, INV-DET-001.
