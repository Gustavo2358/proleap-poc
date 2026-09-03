# Estado — WORK-COND-004

## Onde estamos

**Checkpoint 2 — Implementation concluído** na branch `implementation/work-cond-004-condition-name-surface`, sobre o Discovery aprovado (`69e715e`), seguindo o contrato da decisão **D — `DataReference` corrigido** sem node novo, sem contrato novo e sem ampliação de resolver. Status: **IMPLEMENTATION_COMPLETE — aguardando review humano final do PR #18** (merge é decisão humana; não executado).

O Checkpoint 1 (Discovery, rounds 1–2) permanece resumido abaixo: a decisão D venceu C; o alvo de qualifier `UNSPECIFIED` com mapeamento compatibility-preserving `{DATA}` foi escolhido porque a ampliação `{DATA, FILE}` regride `RESOLVED → UNSUPPORTED_DIALECT_OPTION/AMBIGUOUS` no contracaso local-DATA × outer-GLOBAL-FILE; a regra IBM faltante (resolution-of-names step 3) é a dependência separada `BACKLOG-RES-004`.

## Verde conhecido

- `ConditionNameSurfaceAstTest`: **15 testes verdes** — oracles CN-01..CN-12, S4-BOUNDARY-01, pre-order/identity com `assertActualProductsJoin` e o consumer-impact check CICS.
- `ConditionNameSurfaceDiscoveryTest`: **14 testes verdes** (facts de grammar preservados; caracterizações do bug pré-fix migradas para oracles positivos; contracaso local/GLOBAL mantido como regressão).
- `SemanticConditionContextDiscoveryTest` (8, 1 skip pré-existente), `ConditionSurfaceAstTest` (30), `AstPreorderInvariantTest` (5), `DataAndIndexReferenceResolverTest` (23): todos verdes.
- Suíte Maven completa: **295 testes, 0 falhas, 0 erros, 3 skips pré-existentes**.

## Restante

- **Review humano final do PR #18** — STOP obrigatório; sem merge automático.
- Futuro, em work item próprio: `BACKLOG-RES-004` (IBM resolution-of-names step 3 + ampliação `UNSPECIFIED → {DATA, FILE}`).
- Slice 5 (ocorrências contextuais / remoção do falso gap `grammarRule == conditionNameReference ⇒ CONDITION`) — intocado e não antecipado.

## Descobertas que afetam o plano

1. **Grammar:** `conditionNameReference: conditionName (inData* inFile? conditionNameSubscriptReference* | inMnemonic*)` — subscripts DEPOIS de toda qualification; `FLAG-88(I) OF CUSTOMER` grammar-rejeitado.
2. **Corrupção pré-fix (eliminada):** `firstDescendant(qualifiedDataName)` capturava o subscript — `FLAG-88(I)` virava `baseName="I"`; qualification perdida; qualifier do subscript qualificado era roubado para a raiz.
3. **Assimetria de branches:** `ELEM OF GRP-TBL(I)` em simple-condition (subscripts da referência) vs relation-operand (subscripts do qualifier via `inTable`) — sobrevive estruturalmente no `DataReference` corrigido.
4. **Fronteiras do collector:** `grammarRule == "conditionNameReference" ⇒ CONDITION` permanece o falso gap (Slice 5); os subscripts/qualifiers recuperados entram pelas políticas `SUBSCRIPT`/`QUALIFIER_COMPONENT` pré-existentes **sem nenhuma mudança de collector**.
5. **Namespace de qualifier:** `dataName`/`fileName`/`mnemonicName` são `cobolWord`; `inFile`/`inMnemonic` sombreados. Target por posição (não-final `DATA`; final `UNSPECIFIED`); MNEMONIC bounded.
6. **Decisão de modelagem:** **D** (`DataReference` corrigido) — o node novo de C só codificaria a posição, já estrutural; C dividiria o mesmo nominal escrito entre tipos por posição. Sem ADR novo.

## Checkpoint 2 — Implementation

### Production changes

| Arquivo | Mudança | Classificação (audit §37) |
| --- | --- | --- |
| `Ast.java` | `QualifierTarget.UNSPECIFIED` + javadoc (fato de surface; a parse tree não classifica o namespace do qualifier final) | A |
| `AstBuilder.java` | `conditionNameReference(ctx)` construído SOMENTE de children diretos (`conditionName()`, `inData()`, `conditionNameSubscriptReference()`); `conditionNameSubscriptGroup` via `expression(subscript, ...)` existente; extração `buildQualifier` compartilhada (paths de identifier com comportamento byte-for-byte idêntico); `visitConditionNameReference` e `buildBareNominal` roteados ao novo lowering | A |
| `DataAndIndexReferenceResolver.java` | 1 case `UNSPECIFIED → {DATA}` + javadoc do boundary `BACKLOG-RES-004` | B |

Nenhuma linha classificada como D (inesperada). Proibido e não utilizado: `firstDescendant`/`nearestDescendants` a partir da referência, reparse textual, `instanceof InDataContext ⇒ DATA`, `DATA_OR_FILE` na surface de condition-name.

### Testes

- **Novo** `ConditionNameSurfaceAstTest` (oracles CN-01..CN-12 + S4-BOUNDARY-01 + pre-order/identity + CICS).
- **Migrados** (`ConditionNameSurfaceDiscoveryTest`): `subscriptedConditionNameReferencesKeepBaseQualifiersAndSubscripts`, `contextualTailInnerReferenceCarriesTheCompleteWrittenStructure`, `nestedQualifierInsideSubscriptBelongsToTheSubscriptNotTheReference` (AST corrigido), `dataQualifierBranchCanCarryAFileDeclaration` (alvo `UNSPECIFIED`; resolução continua `DECLARATION_NOT_FOUND`), `qualifiedLocalDataNameCollidesWithOuterGlobalFileNameAcrossPrograms` (alvo `UNSPECIFIED`; regressão `RESOLVED/QUALIFIED_HIERARCHY_MATCH` local preservada), renome de `currentAstPreservesQualifiersOnlyWhileSubscriptsAreAbsent` → `qualificationOrderAndConnectorsSurviveStructurally`.
- **Migrado** `SemanticConditionContextDiscoveryTest.characterizesConditionNameSubscriptCorruption`: baseName `FLAG-ON`, 1 `SubscriptGroup`, occurrence `IDX` com role `SUBSCRIPT`, resolução do root `RESOLVED/UNIQUE_VISIBLE_DECLARATION` (recuperação legítima: a occurrence agora carrega o nome correto; nenhuma política de resolver mudou).
- `work-item.yaml`: prefixo `planned:` removido no mesmo checkpoint que criou o arquivo.

### Semantic challenge pass — implementation

| Challenge | Ataque | Veredito |
| --- | --- | --- |
| A — first descendant shortcut | `firstDescendant(qualifiedDataName)` | **Morto** — CN-04/CN-12 falham sob o lowering antigo (evidência: as caracterizações pré-fix falhavam exatamente nesses asserts) |
| B — flattening | qualifiers como string | **Morto** — CN-03 exige dois `DataQualifier` na ordem escrita |
| C — drop subscripts | descartar `(I, J)` | **Morto** — CN-04/CN-05 exigem `SubscriptGroup` com children |
| D — root qualifier stealing | traversal recursivo promove `SUB-GROUP` | **Morto** — CN-12 exige root qualifiers = `[CUSTOMER]` somente |
| E — DATA_OR_FILE shortcut | `UNSPECIFIED → {DATA, FILE}` | **Morto empiricamente por mutação**: a mutação fez falhar `cn11`, `dataQualifierBranchCanCarryAFileDeclaration` e o contracaso local/GLOBAL (3 oracles) |
| F — condition specialization | `conditionNameReference ⇒ CONDITION` no builder/resolver | **Ausente** — nenhuma linha nova faz isso; o falso gap do collector permanece byte-identical (Slice 5) |
| G — Slice 5 leakage | diff em `ReferenceOccurrenceCollector`/`admissibleKinds`/`ReferenceKind.CONDITION`/`grammarRule` | **Ausente** — grep no diff de produção: zero ocorrências |
| H — reparse textual | `split`/`substring`/`Pattern`/`Matcher`/`regex` no lowering novo | **Ausente** — grep no diff do builder: zero; estrutura vem da parse tree |
| I — identity/pre-order | IDs contíguos, sem clone, ordem determinística | **Verde** — `conditionNameSurfacesPreserveCanonicalPreOrderAndProductJoins` + `AstPreorderInvariantTest` + `assertActualProductsJoin` |
| J — consumer blast radius | ver seção abaixo | **Explicável pela estrutura recuperada**; sem regressão silenciosa |

### Consumer impact

- `ReferenceOccurrenceCollector` — **byte-identical**: o `addDataReference` existente passa a enxergar os qualifiers/subscripts recuperados; o ternary pré-existente `target == FILE ? FILE : DATA` mapeia `UNSPECIFIED` para `DATA` (compatibility-preserving). Subscripts de condition-name geram occurrence `SUBSCRIPT` pela política pré-existente (oracles CN-04/05/06/07).
- `DataAndIndexReferenceResolver` — 1 case novo; candidate universe provadamente inalterado (contracaso + mutação E).
- `CoverageSnapshot`/`AstSnapshot` — sem mudança de código; métricas mudam apenas onde a estrutura legítima foi materializada; nenhuma baseline congelada quebrou (suíte completa verde).
- `CicsIntrinsicClassifier` — sem mudança de código. **Delta bounded executável** (`cicsClassifierConsumerImpactIsBoundedToTheRecoveredSurfaceShape`): `IF DFHRESP(X)` em posição de simple condition, antes com `baseName=X` (shape nunca casava), agora recupera `DataReference(DFHRESP, [(X)])` e passa a receber a MESMA classificação `POSSIBLE_INTRINSIC/INFERRED` já emitida para o path de relation-operand (`IF DFHRESP(X) = DFHVALUE(NORMAL)`). Julgamento: **comportamento legítimo decorrente de AST anteriormente corrompida**, consistente com o contrato do classifier (hipótese INFERRED para referência COBOL não resolvida com shape conhecida); nenhum workaround heurístico aplicado.

### Regressões verificadas

- Contracaso local/GLOBAL (`IF C OF Q`): continua `RESOLVED/QUALIFIED_HIERARCHY_MATCH` com o candidate local — `{DATA, FILE}` NÃO foi introduzido.
- `IF FLAG-88 OF CUSTOMER-FILE` continua `UNRESOLVED/DECLARATION_NOT_FOUND` (boundary `BACKLOG-RES-004`, não regressão).
- `ConditionSurfaceAstTest` (Slice 3) verde sem migração de tipo; tail interno continua `DataReference`.
- SET/EVALUATE paths de identifier com shapes e targets pré-existentes (S4-BOUNDARY-01).

### Must-not-change audit

`git diff --exit-code 69e715e -- Cobol.g4 AstSnapshot.java ReferenceOccurrenceCollector.java CobolReferenceResolver.java ReferenceOccurrences.java ReferenceResolution.java grammar-rule-manifest.tsv` → **zero** (byte-identical).

### Gates (Checkpoint 2, working tree pós-implementação)

- `./scripts/harness/check-fast.sh` — **passed**
- `./scripts/harness/check-performance.sh` — **passed**
- `./scripts/harness/check-semantic.sh` — **passed**
- `./scripts/harness/check-full.sh` — **passed**

### Commits do checkpoint

- `3c4e0a4` — `feat: preserve condition-name reference structure`
- `dd45bff` — `test: cover condition-name surface semantics`
- commit de docs deste checkpoint (registra o contrato implementado; SHAs completos no corpo do PR #18)

### Remaining limitations

- Resolução de condition-names qualificadas por file-name: `DECLARATION_NOT_FOUND` (hoje e antes do slice).
- MNEMONIC (IBM Format 2): qualifier preservado como `UNSPECIFIED`; namespace MNEMONIC não modelado (bounded).
- Falso gap do collector (`grammarRule ⇒ CONDITION`) preservado; occurrence contextuais pertencem ao Slice 5.
- `FLAG-88(I)(J)`/`FLAG-88(ALL)` permanecem shapes grammar-only preservadas sem alegação de validade.

### BACKLOG-RES-004

Não implementado: sem step 3 de resolution-of-names, sem precedência de programa local, sem ampliação `UNSPECIFIED → {DATA, FILE}`. O mapeamento atual é compatibility-preserving `{DATA}` e a dependência permanece registrada no backlog.

### Slice 5 não antecipado

Nenhuma política contextual nova: collector, `admissibleKinds`, `ReferenceKind.CONDITION` e `grammarRule` seguem exatamente como antes; `ContextualConditionTail` continua a alternativa contextual com `DataReference` interno; o hand-off do Slice 5 permanece por contexto estrutural.
