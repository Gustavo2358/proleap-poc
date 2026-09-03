# Estado — WORK-COND-004

## Onde estamos

Checkpoint 1 (Discovery) **aprofundado no round 2** na branch `implementation/work-cond-004-condition-name-surface`, sem nenhuma alteração de produção. O round 2 falsificou a afirmação de invariância do round 1 (ampliar `DATA → DATA_OR_FILE` PODE regredir: `RESOLVED → UNSUPPORTED_DIALECT_OPTION`/`AMBIGUOUS` no contracaso local-DATA × outer-GLOBAL-FILE, fato `qualifiedLocalDataNameCollidesWithOuterGlobalFileNameAcrossPrograms`) e reabriu C vs D. Decisões finais: (a) **modelagem D** — `DataReference` corrigido, sem node novo, sem contrato novo; (b) **alvo de qualifier `UNSPECIFIED`** com mapeamento conservador `{DATA}` no resolver (policy-preserving, zero mudança de candidates); (c) a regra IBM faltante (resolution-of-names step 3 — precedência de programa local APÓS qualification) é **dependência separada, `BACKLOG-RES-004`**, que destravará `{DATA, FILE}` no futuro. `reference-resolution.md` foi relido integralmente e voltou ao `must_read`. Conclusão: **READY_FOR_IMPLEMENTATION** — checklist do round 2 10/10 fechado.

## Verde conhecido

- `ConditionNameSurfaceDiscoveryTest`: **14 testes verdes** (10 originais + 3 fatos do round 1 + 1 contracaso do round 2: declarações/namespaces/visibilidade across units, comportamento atual do resolver para a condição e para a referência DATA equivalente, e as ancestries que provam que a ampliação adicionaria candidate).
- Gates completos verdes com diff exclusivamente documental/testes (registro abaixo).
- Diff entre `main` e a branch não contém `src/main/`.

## Restante

- Review humano do Discovery (Checkpoint 1) — STOP obrigatório.
- Após aprovação explícita, na MESMA branch/PR: Checkpoint 2 (implementação do contrato da spec, oracles CN-01..CN-12 e erros A..M em `ConditionNameSurfaceAstTest`, remoção do prefixo `planned:` no `work-item.yaml`).
- Futuro, em work item próprio: `BACKLOG-RES-004` (step 3 de resolution-of-names + ampliação `UNSPECIFIED → {DATA, FILE}`).

## Descobertas que afetam o plano

1. **Grammar:** `conditionNameReference: conditionName (inData* inFile? conditionNameSubscriptReference* | inMnemonic*)` — subscripts DEPOIS de toda qualification; `FLAG-88(I) OF CUSTOMER` grammar-rejeitado.
2. **Corrupção atual:** `firstDescendant(qualifiedDataName)` captura o subscript — `FLAG-88(I)` vira `baseName="I"`; qualification perdida; com subscript qualificado o qualifier interno é roubado para a raiz.
3. **Assimetria de branches:** `ELEM OF GRP-TBL(I)` em simple-condition (subscripts da referência) vs relation-operand (subscripts do qualifier via `inTable`) — diferença de surface que sobrevive estruturalmente no `DataReference` corrigido.
4. **Fronteiras do collector:** `grammarRule == "conditionNameReference" ⇒ CONDITION` permanece o falso gap (Slice 5); com a decisão D os subscripts recuperados entram pela política `SUBSCRIPT`/INDEX/{DATA, INDEX} pré-existente **sem nenhuma mudança de collector**.
5. **Namespace de qualifier (round 1+2):** `dataName`/`fileName`/`mnemonicName` são `cobolWord`; `inFile`/`inMnemonic` sombreados. Target por posição (não-final `DATA`; final `UNSPECIFIED`); MNEMONIC bounded. **Round 2:** ampliar para `{DATA, FILE}` sem o step 3 do IBM LR regride (COND-A08c) → mapeamento conservador até `BACKLOG-RES-004`.
6. **Decisão de modelagem (round 2):** **D** (`DataReference` corrigido) vence C (node novo + contrato) — o node novo só codificaria a posição, já estrutural nos containers tipados e no contexto do collector; C dividiria o mesmo nominal escrito entre dois tipos por posição (inconsistência nova frente ao precedente SET/EVALUATE) e exigiria uma sealed interface nascida de refactor (Finding 3). Sem ADR novo.

## Semantic challenge pass — round 2

### 1. Hipótese

"Ampliar `QualifierTarget.DATA → DATA_OR_FILE` é invariável para casos existentes porque `DATA ⊂ {DATA, FILE}`."

### 2. Contracaso

OUTER: `FD Q IS GLOBAL` + `01 OUTER-REC` + `88 C` (condition-name GLOBAL — IBM: "A condition-name ... is global if that entry is subordinate to another entry that specifies the GLOBAL clause"). INNER (contido): `01 Q` (DATA local) + `88 C`; escreve `IF C OF Q` e `MOVE CUST-STATUS OF Q TO X`. Fixture IBM-válida e grammar-aceita (fato executável; `STATUS` é palavra reservada do lexer — renomeada para `CUST-STATUS`).

### 3. Resultado normativo

IBM Enterprise COBOL for z/OS 6.4 LR, cap. 7 "Resolution of names — Names within programs" (pp. 63–66): programas contidos e contendo podem definir condition-name/data-name/file-name/record-name com a mesma user-defined word; o conjunto de resolução é {nomes de B} ∪ {nomes GLOBAL de A e contendo}; a qualification e as regras de unicidade são aplicadas a esse conjunto; **se mais de um recurso for identificado, no máximo um é local a B — o local vence e, sem local, o contendo mais próximo (step 3)**. Precedência DEPOIS da qualification; uniforme para DATA e FILE; independe de `QUALIFY(STANDARD/EXTEND)`. File-name é global se o FD tem GLOBAL; condition-name participa da visibilidade GLOBAL por subordinação.

### 4. Comportamento do repo (fato executável)

- `IF C OF Q` hoje: occurrence CONDITION; `compatibleCandidates(C)` = [INNER C local, OUTER C GLOBAL]; `applyQualification(Q@{DATA})` exclui OUTER C por namespace → `RESOLVED/QUALIFIED_HIERARCHY_MATCH` com o candidate local (exclusão ACIDENTAL, não a regra).
- Com ampliação hipotética `{DATA, FILE}`: ambos sobrevivem → `qualifyExtend` mantém só o local → policy UNSPECIFIED → `UNSUPPORTED_DIALECT_OPTION` (`AMBIGUOUS` sob `QUALIFY(STANDARD)`) — **regressão `RESOLVED → UNSUPPORTED_DIALECT_OPTION/AMBIGUOUS`**, e ainda errado frente ao step 3 (local em qualquer modo).
- `MOVE CUST-STATUS OF Q TO X` (referência DATA com `DATA_OR_FILE` pré-existente de `qualifiedDataNameFormat1`) já devolve hoje `UNSUPPORTED_DIALECT_OPTION` com 2 candidates — **o defeito é preexistente e geral**, não introduzido pelo slice.
- A occurrence `QUALIFIER_COMPONENT` de `Q` resolve para o data item local — evidência estrutural de que a correção futura passa por resolver os nomes dos qualifiers antes do filtering.

### 5. Consequência arquitetural

A ampliação não pode entrar no Slice 4. A regra faltante é geral (DATA e CONDITION; namespaces DATA e FILE; todos os programas) e preexistente → **Opção 2: dependência separada `BACKLOG-RES-004`** (registrada no backlog; a promoção deverá definir invariante próprio para a precedência de programa local após qualification). O Slice 4 escolhe a representação que NÃO expõe a regressão: `QualifierTarget.UNSPECIFIED` na surface + mapeamento `{DATA}` no resolver (policy-preserving; constraint efetiva idêntica à atual). Nenhum caso que já resolvia muda; a resolução de condition-names qualificadas por file-name permanece `DECLARATION_NOT_FOUND` até a dependência.

### 6. C vs D revisitado

A tabela completa está em `spec.md` "Decisão de modelagem — C vs D revisitado". Resumo concreto: com D, o collector precisa de ZERO mudanças (o `addDataReference` existente percorre qualifiers/subscripts do `DataReference` corrigido), snapshots/coverage/Ast.children/`ContextualConditionTail` ficam byte-identical, o resolver muda apenas 1 case, e os precedentes do repositório (SET/EVALUATE: payload neutro + contexto estrutural) são mantidos. O node de C carregaria somente a posição — informação que a árvore já possui — e criaria inconsistência (mesmo nominal como `DataReference` no SET e `ConditionNameReference` no IF) além de uma sealed interface nascida de refactor.

### 7. Decisão final

**D — `DataReference` corrigido** + `QualifierTarget.UNSPECIFIED` (última posição) + mapeamento conservador no resolver. Sem node novo, sem contrato novo, sem `NominalReference` (Finding 3 resolvido por eliminação: "nominal" no repositório é o adjetivo do domínio de binding como um todo — uma interface `NominalReference` com dois implementadores data-shaped redefiniria o vocabulário). O goal do work item foi ajustado para refletir D.

### 8. Decisão de lifecycle para resolver

**Work item próprio futuro (`BACKLOG-RES-004`)** — a regra afeta resolution de forma geral (dados já falham hoje), envolve precedência local/GLOBAL, atinge vários namespaces e exige etapa nova no resolver (entre `applyQualification` e a decisão de qualify mode). O Slice 4 não a implementa, não a esconde sob "structural adaptation" e não é bloqueado por ela (o alvo conservador evita a exposição).

### Challenges 1–10 (round 2, vereditos)

1. **Candidate-set monotonicity:** a falácia `old ⊆ new ⇒ resultado preservado` foi atacada diretamente — refutada pelo contracaso (1 candidate → 2 candidates → `UNSUPPORTED_DIALECT_OPTION`/`AMBIGUOUS`). ✓
2. **Local vs inherited GLOBAL:** regra IBM localizada (step 3): qualification ANTES, precedência de programa local DEPOIS, na decisão. ✓
3. **Namespace collision across program units:** mesma spelling local DATA × outer GLOBAL FILE executada e caracterizada (fato). ✓
4. **Representation necessity (provar C desnecessário):** o `ConditionNameReference` carrega apenas a posição, já derivável de containers tipados + contexto do collector — nenhum consumer atual precisa da distinção em node próprio. ✓
5. **Representation insufficiency (provar D insuficiente):** nenhuma shape encontrada em que dois usos com o mesmo `DataReference` estrutural exijam significados de surface diferentes não deriváveis do parent/container tipado (standalone = `IfStatement.condition`; tail = wrapper; relation = `RelationCondition`/`DistributedOperandGroup`; class = `ClassCondition.subject`; EVALUATE = `EvaluateSelectorContext`). Registrado: D não perde informação para os consumidores existentes. ✓
6. **Accidental architecture:** a sealed interface `NominalReference` existiria para reduzir `instanceof`, não por domínio compartilhado real (Finding 3) — rejeitada. ✓
7. **Downstream blast radius:** C toca Ast/AstBuilder/collector/resolver/snapshot/coverage/testes/classifiers; D toca AstBuilder + 1 valor de enum + 1 case do resolver + testes. Menos arquivos não decide sozinho, mas o benefício semântico de C não compensa o raio (tabela na spec). ✓
8. **Slice 5 hand-off real (pseudocódigo):** com D, o collector do Slice 5 carrega o contexto estrutural na própria recursão — `visitConditionSurface(expr, ...)` ramificando por `ContextualConditionTail` (admissível aberto), `DataReference` direto sob a surface (política de simple condition), `RelationCondition`/`LogicalCondition`/`NegatedCondition`/`GroupedCondition`/`ClassCondition` — sem `grammarRule`, sem parent inspection, sem reparse; precedente `EvaluateSelectorContext`/`StatementOperandContext`. Com C, o Slice 5 trocaria a string por `instanceof ConditionNameReference` — 1:1 o mesmo fato (o node só nasce daquela rule), apenas tipado. ✓
9. **Ambiguity preservation:** nenhuma opção transforma `A = B OR C` em CONDITION antes do binding (tail permanece wrapper; M5 metamórfico). ✓
10. **Lifecycle correctness:** correção semântica do resolver = work item próprio (`BACKLOG-RES-004`), não deste slice, conforme work-item protocol e os critérios de Opção 2. ✓

### Checklist de prontidão (round 2)

- [x] A ampliação DATA → DATA_OR_FILE não introduz regressão — demonstrado que introduziria; a regra necessária (IBM step 3) está explicitamente modelada e corretamente escopada em `BACKLOG-RES-004`.
- [x] Local vs inherited GLOBAL testado (fato executável) e normativamente resolvido (cap. 7 do IBM LR).
- [x] A escolha C vs D foi refeita usando pseudocódigo real do Slice 5 (challenge 8).
- [x] A nova abstraction possui necessidade semântica comprovada — NÃO possui; por isso foi removida (D).
- [x] O naming do contrato não conflita com o vocabulário nominal do repo — contrato eliminado (Finding 3).
- [x] `reference-resolution.md` foi relido integralmente e incorporado à decisão (voltou ao `must_read`).
- [x] Nenhuma mudança de política do resolver está escondida sob "structural adaptation" — o único delta é o case `UNSPECIFIED → {DATA}`, policy-preserving, com a ampliação explicitamente bloqueada por `BACKLOG-RES-004`.
- [x] O lifecycle da eventual correção de resolver está explícito (`BACKLOG-RES-004`).
- [x] O Slice 5 continua possível sem reparse/string heuristics (hand-off por contexto estrutural).
- [x] Zero production code alterado nesta rodada.

## Gates (checkpoint Discovery round 2, head da branch)

- `./scripts/harness/check-fast.sh` — **passed**
- `./scripts/harness/check-performance.sh` — **passed**
- `./scripts/harness/check-semantic.sh` — **passed**
- `./scripts/harness/check-full.sh` — **passed**
