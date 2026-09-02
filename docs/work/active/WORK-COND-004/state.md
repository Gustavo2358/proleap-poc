# Estado — WORK-COND-004

## Onde estamos

Discovery (Fase 1) **corrigido e aprofundado** na branch `implementation/work-cond-004-condition-name-surface`, sem nenhuma alteração de produção. Após o review humano, a proposta original foi falsificada e substituída (ver "Semantic challenge pass"): o node novo não pode conviver com `DataAndIndexReferenceResolver` intocado, o `QualifierTarget.DATA` por branch da grammar foi rejeitado, o oracle nested-subscript foi adicionado e o contrato de provenance foi alinhado ao modelo (Contrato A). A decisão revisada é a **Alternativa C — contrato nominal estrutural compartilhado** (`Ast.NominalReference`) + node de superfície `Ast.ConditionNameReference`; o resolver sai de `must_not_change` com anotação `STRUCTURAL ADAPTATION ONLY — NO RESOLUTION POLICY CHANGE`. Conclusão: **READY_FOR_IMPLEMENTATION** — os 10 checkpoints de prontidão do review estão fechados; nenhuma decisão arquitetural nova é necessária (sem ADR novo: a evolução continua prevista pelo contrato surface-lossless de ADR-0012).

## Verde conhecido

- `ConditionNameSurfaceDiscoveryTest`: **13 testes verdes** (10 originais + 3 FATOS novos desta rodada: nested qualifier dentro de subscript; file declaration atrás do branch `inData`; granularidade de provenance sem node de nome-base).
- Gates completos verdes com diff exclusivamente documental/testes (registro abaixo).
- Diff entre `main` e a branch não contém `src/main/` — production behavior intacto.

## Restante

- Review humano do Discovery (Commit 1) — STOP obrigatório.
- Após aprovação explícita, na MESMA branch/PR: Commit 2 (implementação do contrato da spec, oracles CN-01..CN-12 e erros A..I em `ConditionNameSurfaceAstTest`, adaptação estrutural do resolver, remoção do prefixo `planned:` no `work-item.yaml`).

## Descobertas que afetam o plano

1. **Grammar (fato de parse tree):** `conditionNameReference: conditionName (inData* inFile? conditionNameSubscriptReference* | inMnemonic*)` — subscripts vêm DEPOIS de toda qualification (`FLAG-OK OF CUSTOMER(I)`), em conformidade com o IBM Format 1; `FLAG-88(I) OF CUSTOMER` é grammar-rejeitado.
2. **Corrupção atual:** quando há subscripts, `dataReference(conditionNameReference)` captura o primeiro `qualifiedDataName` DESCENDENTE — que é o subscript — então `FLAG-88(I)` vira `DataReference(baseName="I", subscriptGroups=[])`; qualification também é perdida. Com subscript qualificado (`FLAG-88 OF CUSTOMER(SUB OF SUB-GROUP)`), o qualifier do subscript é roubado para a raiz (novo fato caracterizado).
3. **Assimetria de branches:** em simple-condition position QUALQUER nominal (`TBL(I)`, `ELEM OF GRP-TBL(I)`) parseia como `conditionNameReference`; em relation-operand position os mesmos textos parseiam como `tableCall`/`qualifiedDataName` com subscript anexado ao QUALIFIER (`inTable`). A attachment estrutural do subscript difere entre branches — diferença de surface que sobrevive com node tipado, sem `grammarRule`; a equivalência semântica (qual tabela é subscriptada) pertence ao binding.
4. **Fronteiras do collector:** `grammarRule == "conditionNameReference" ⇒ CONDITION` permanece o falso gap (Slice 5); subscripts de condition-name hoje não geram occurrence `SUBSCRIPT` (estruturalmente descartados). A implementação preserva a política e apenas atravessa o node novo — os subscripts recuperados entram pela política `SUBSCRIPT`/INDEX/{DATA, INDEX} pré-existente.
5. **Branches inalcançáveis e namespace de qualifier:** `dataName`/`fileName`/`mnemonicName` são todos `cobolWord` — `inFile?`/`inMnemonic*` são sombreados por `inData` (fato de grammar). Consequência revisada: NÃO se fecha `QualifierTarget.DATA` pelo branch; o target deriva da POSIÇÃO na estrutura da rule (não-final `DATA`, final `DATA_OR_FILE` — precedente de `qualifiedDataNameFormat1`), e MNEMONIC fica como lacuna bounded (SPECIAL-NAMES não modelado; resolução não suportada). Challenge de falsificação: `IF FLAG-88 OF CUSTOMER-FILE` com `FD CUSTOMER-FILE` — branch `inData`, declaração `Namespace.FILE` (fato caracterizado).
6. **Decisão revisada sem ADR novo:** Alternativa C — contrato nominal compartilhado `NominalReference` + node `ConditionNameReference`; `ContextualConditionTail.nominalReference` tipado pelo contrato neutro; resolver com adaptação estritamente estrutural (prova de invariância no Erro I). Alternativas A, B e D rejeitadas com justificativa nos challenges.

## Semantic challenge pass

### 1. Proposta original

Node independente `Ast.ConditionNameReference(Meta meta, String name, List<DataQualifier> qualifiers, List<SubscriptGroup> subscriptGroups, String writtenText)` reutilizando `DataQualifier`/`SubscriptGroup`; `ContextualConditionTail.nominalReference` re-tipado; qualifiers com target `DATA` (porque `inFile`/`inMnemonic` seriam "inalcançáveis"); `DataAndIndexReferenceResolver` em `must_not_change`.

### 2. Como ela falhou

- **Finding 1 (P1):** o resolver consome `Ast.DataReference` estruturalmente em 5 pontos (`baseName` L413, `qualifiedReference` L92, `applyQualification` L186, `qualifyExtend` L192, `qualifierConstraints`). Com o node novo e o resolver intocado, `FLAG-OK OF GROUP-A` viraria `baseName="FLAG-OK OF GROUP-A"`, `qualifiedReference=false`, qualification ignorada → regressão semântica de binding (baseLine `qualified-condition` de `SemanticConditionContextDiscoveryTest` quebraria).
- **Finding 2 (P1):** `parse branch = inData` não prova `semantic namespace = DATA`: `dataName`/`fileName`/`mnemonicName` são o mesmo conjunto de tokens. A proposta fechava `QualifierTarget.DATA` por branch — rejeitada pelo challenge com `FD CUSTOMER-FILE`.
- **Finding 3 (P2):** faltava oracle adversarial para qualification dentro do subscript; a grammar permite (`subscript → qualifiedDataName`, IBM: "data-name-3 can be qualified") e um `nearestDescendants` produziria root qualifiers `[CUSTOMER, SUB-GROUP]` — incorreto.
- **Finding 4 (P2):** CN-08 prometia span independente para o nome-base, mas o modelo proposto não possui Meta para o nome — contrato e modelo divergiam.

### 3. Alternativas avaliadas

| Alternativa | Surface fidelity | Resolver compatibility | Ambiguity | Incisão | Resultado |
| --- | --- | --- | --- | --- | --- |
| A — node independente | alta | quebra (exige adaptação) | tail re-tipado com nome que sugere CONDITION | média | rejeitada (duplica contrato nominal; naming overclaim no tail) |
| B — wrapper + payload `DataReference` interno | baixa | perfeita por construção | ok | mínima | rejeitada (payload = node sintético, span duplicado, occurrence rotulada `DataReference` nos produtos; viola CN-07/Erro G e tensiona INV-PROV-002) |
| C — contrato nominal compartilhado + node de superfície | alta | preservada por adaptação estrutural comprovada | tail com campo neutro | maior | **escolhida** |
| D — só corrigir o lowering (sem node) | alta | perfeita | ok | mínima | rejeitada (Slice 5 voltaria a depender de `grammarRule`/parent inspection — hand-off proibido) |

### 4. Decisão revisada

A menor solução que satisfaz todos os contratos: **`Ast.NominalReference`** (sealed; `baseName`/`writtenText`/`qualifiers`/`subscriptGroups`/`meta` — implementado por `DataReference` e pelo novo `Ast.ConditionNameReference`) + node de superfície tipado próprio. Qualifier targets por posição gramatical (não-final `DATA`, final `DATA_OR_FILE`); MNEMONIC bounded; proveniência pelo Contrato A (sem Meta do nome-base; CN-08 reescrito). Resolver e collector passam a consumir o contrato sem mudança de política (Erro I); `must_not_change` atualizado com a anotação estrutural.

### 5. Por que ela não antecipa Slice 5

O Slice 5 (occurrences contextuais) receberá: (a) a distinção estrutural standalone `ConditionNameReference` versus `ContextualConditionTail` (posição, sem `grammarRule`); (b) o payload nominal completo no contrato compartilhado; (c) o falso gap do collector preservado e explícito. Nada decide DATA/INDEX/CONDITION, nenhuma ocorrência contextual é criada, nenhuma policy de `admissibleKinds` muda. O Slice 5 poderá remover `grammarRule ⇒ CONDITION` consumindo node type + tail wrapper — sem reparse textual, sem parent inspection, sem heurística, sem reconstrução da parse tree.

### Challenges 1–10 (vereditos)

1. **Phase ownership:** cada campo do node deriva de context/token (nome escrito, conectores, targets por posição gramatical, subscripts, spans). Nada depende de declaration kind/symbol table/scope/compiler option/tipo/runtime. `QualifierTarget` por posição é estrutura da rule, não binding. ✓
2. **Downstream compatibility:** as 7 shapes (do simples ao `A = B OR FLAG-OK(I)`) foram seguidas de parse → AST → occurrence → resolver input → candidate lookup → qualification → resolução esperada na tabela de consumer impact; o contrato fornece baseName+qualifiers (resolver) e qualifiers+subscripts (collector). ✓
3. **Declaration substitution:** a surface não muda quando `C` é DATA/CONDITION/INDEX/RENAMES/unresolved (fatos: matrix de `SemanticConditionContextDiscoveryTest` + M5/M4 metamórficos). O tail permanece contextual. ✓
4. **Grammar branch substitution:** `ELEM OF GRP-TBL(I)` difere entre simple-condition (subscripts da referência) e relation-operand (subscripts do qualifier via `inTable`) — diferença de surface que sobrevive estruturalmente; o que é semântico (qual tabela é subscriptada) fica no binding. ✓
5. **Qualifier namespace:** `inData ⇒ DATA` invalidado com DATA/FILE/MNEMONIC (fato `dataQualifierBranchCanCarryAFileDeclaration` + IBM Format 1/2); uncertainty preservada por `DATA_OR_FILE` + lacuna MNEMONIC documentada. ✓
6. **Nested structure:** `FLAG-88 OF CUSTOMER(SUB OF SUB-GROUP)` — oracle CN-12 + Erro H matam o `nearestDescendants`; o builder usa children diretos. ✓
7. **Resolver invariance:** baseline `qualified-condition` (`A = B OR C OF G` → RESOLVED/QUALIFIED_HIERARCHY_MATCH) preservado pela adaptação estrutural; único delta = file-qualified roots (antes `DECLARATION_NOT_FOUND`, input inventava DATA) passam a resolver quando inequívocas — correção de fidelidade, não política. ✓
8. **Occurrence cardinality:** `FLAG-88 OF CUSTOMER(I)` → exatamente FLAG-88 (CONDITION), CUSTOMER (QUALIFIER_COMPONENT), I (SUBSCRIPT); sem duplicatas, sem ocorrência de container, sem perda (CN-07). ✓
9. **Identity/pre-order:** árvore desenhada: `ConditionNameReference → [DataQualifier(reference), SubscriptGroup(subscript expression)]`; cada node aparece uma única vez, sem child compartilhado, sem clone para reaproveitar binding; pre-order determinístico via `Ast.children` (INV-AST-003). ✓
10. **Slice 5 compatibility:** ver item 5 acima; o node type substitui `grammarRule` como evidência estrutural da surface. ✓

### Checklist de prontidão do review

- [x] O resolver consegue consumir corretamente a nova representação — adaptação estritamente estrutural demonstrada (Erro I) e incluída no `source_scope`.
- [x] Nenhuma qualification é fechada como DATA apenas por branch da grammar.
- [x] DATA/FILE/MNEMONIC uncertainty modelada (`DATA_OR_FILE` por posição) e MNEMONIC explicitamente bounded.
- [x] Nested qualification dentro de subscript possui oracle adversarial (CN-12 + Erro H + fato caracterizado).
- [x] O contrato de provenance corresponde exatamente ao modelo proposto (Contrato A; CN-08 reescrito).
- [x] ContextualConditionTail continua binding-dependent (campo `NominalReference` neutro).
- [x] A solução foi testada conceitualmente contra DATA, CONDITION, INDEX, RENAMES, unresolved e qualification (Challenges 3/7 + baseline da matrix).
- [x] O Slice 5 consegue consumir a estrutura sem `grammarRule` e sem reparse textual.
- [x] Nenhum novo conhecimento pertencente a binding entrou na AST.
- [x] Não existe regressão conhecida para condition-name qualification que já funcionava antes do Slice 4.

## Gates (checkpoint Discovery corrigido, head da branch)

- `./scripts/harness/check-fast.sh` — **passed**
- `./scripts/harness/check-performance.sh` — **passed**
- `./scripts/harness/check-semantic.sh` — **passed**
- `./scripts/harness/check-full.sh` — **passed**
