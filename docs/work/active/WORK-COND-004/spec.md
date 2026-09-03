# Spec — WORK-COND-004

## Problema

Uma condition-name reference verdadeira (simple condition escrita como condition-name, com ou sem qualification e subscripts) não possui representação estrutural correta na surface AST. O lowering atual de `conditionNameReference` reutiliza `Ast.DataReference` e, quando subscripts são escritos, corrompe a estrutura:

- o `baseName` passa a ser o nome do primeiro subscript (o lowering captura o primeiro `qualifiedDataName` descendente, que é o subscript), e o nome da condition é perdido da estrutura;
- `subscriptGroups` fica sempre vazio — os subscripts sobrevivem apenas em `writtenText`;
- quando o nome-base é sequestrado pelo subscript, a qualification também é perdida;
- quando o subscript é ele próprio qualificado (`FLAG-OK OF CUSTOMER(SUB OF SUB-GROUP)`), o qualifier do subscript é roubado para a referência raiz e o qualifier raiz some (fato caracterizado em `ConditionNameSurfaceDiscoveryTest`);
- a única evidência de que o uso nominal é uma condition-name surface é a POSIÇÃO na árvore (containers tipados do Slice 3 + traversal do collector) — nunca `grammarRule` como autoridade semântica (ADR-0012/INV-COND-001).

Há ainda um risco de resolução acoplado à correção: classificar o qualifier como `DATA_OR_FILE` (ampliando a constraint do resolver de `{DATA}` para `{DATA, FILE}`) adiciona candidates e pode converter `RESOLVED` em `UNSUPPORTED_DIALECT_OPTION`/`AMBIGUOUS` (Finding P1 do round 2, caracterizado em `qualifiedLocalDataNameCollidesWithOuterGlobalFileNameAcrossPrograms`). A seção "Resolver precedence / namespace challenge" fecha esse risco.

`ContextualConditionTail` herda a mesma corrupção em seu `nominalReference` interno. Este slice NÃO corrige o bug original de WAUX-371 (a política nominal/contextual do collector e do resolver pertence ao Slice 5).

## Objetivo

Dar à surface AST uma representação estrutural completa, pré-binding e lossless para condition-name references verdadeiras: nome nominal escrito, qualification IN/OF em ordem, subscripts como children tipados, writtenText e provenance por token — **corrigindo o lowering do payload nominal existente `Ast.DataReference`** a partir dos children diretos do context, sem criar node novo, sem contrato novo, sem lookup/binding/expansão e sem decisão DATA/INDEX/CONDITION. A distinção posicional (standalone vs tail contextual vs relation operand) permanece nos containers tipados existentes e no contexto estrutural que o collector já carrega em sua própria recursão.

## Domínio de entrada suportado

- `simpleCondition → conditionNameReference` em posição standalone (estado de abbreviation fechado): `IF FLAG-OK`, operandos de `AND`/`OR`, sob `NOT`, dentro de `GroupedCondition`;
- `conditionNameReference` em posição de bare nominal tail com estado de abbreviation aberto — permanece `ContextualConditionTail`, com o `nominalReference` interno estruturalmente completo;
- shapes gramaticais aceitas pela rule `conditionNameReference`: condition-name simples; qualificado por `inData+` (`OF`/`IN` data-name, repetível em ordem); subscript lists pós-qualificação (`conditionNameSubscriptReference*`, com `COMMACHAR?` entre subscripts);
- forms do `subscript` aceitas pela grammar: integer literal, `qualifiedDataName` (inclusive qualificado — o subscript pode ser ele próprio uma referência qualificada, forma IBM válida), `indexName`, relative subscript (`+`/`-` integer) e aritmética;
- paths fora da superfície de condição (`SET`, selectors de `EVALUATE`, operandos de relation) continuam usando as shapes atuais de `identifier`/`qualifiedDataName`/`tableCall` — já estruturalmente completas — e ficam fora do escopo de mudança.

Fora do domínio modelado: fallbacks `PreservedExpression` e `PreservedStatement` continuam válidos (ex.: `SEARCH WHEN`, `abbreviation` recursiva).

## Classes semânticas

1. **Condition-name reference estruturada = `Ast.DataReference` corrigido** (decisão D): o payload nominal existente passa a ser construído dos children diretos do `conditionNameReference` — `baseName` = `conditionName()`, qualifiers = `inData()` em ordem, `subscriptGroups` = `conditionNameSubscriptReference()` em ordem, `writtenText` do trecho escrito. A mesma classe cobre condition-name simples, qualificado e subscriptado — diferenças de cardinalidade estrutural, não de classe. Nenhum node novo, nenhum contrato novo: `DataReference` já é o payload nominal neutro do repositório (targets de `SET condition-name TO TRUE` já são `DataReference`).
2. **Qualification** (`DataQualifier` reutilizado): connector `OF`/`IN`, target por posição gramatical (não-final `DATA`; final `UNSPECIFIED` — valor novo do enum), reference nominal do qualifier e writtenText; ordem escrita preservada. O valor `UNSPECIFIED` afirma exatamente o que a parse tree sabe: o namespace do último qualifier não é classificável (DATA/FILE/MNEMONIC possíveis).
3. **Subscripting** (`SubscriptGroup` reutilizado): cada `conditionNameSubscriptReference` escrito vira um grupo com seus subscripts como expression children; um subscript `qualifiedDataName` preserva seus próprios qualifiers (nunca promovidos à referência raiz); subscripts são nominais/expressões por si e preservam spans próprios.
4. **ContextualConditionTail**: permanece a alternativa contextual binding-dependent com `DataReference` interno; nenhuma mudança de tipo.
5. **Posição como estrutura**: standalone condition surface, tail contextual e relation operand são distinguidos pelos containers tipados existentes (`IfStatement.condition`, `ContextualConditionTail`, `RelationCondition` etc.) e pelo contexto que o collector carrega na própria recursão — não por `grammarRule`, não por node novo, não por parent inspection.

## Premissas

- `LANGUAGE_GUARANTEED` (IBM Enterprise COBOL for z/OS 6.4 Language Reference, SC27-8713-03, atualização de 28/06/2024, cap. 7 "Scope of names"/"Resolution of names", cap. 8 "Condition-name"/"Subscripting" e cap. 27 "Condition-name condition"): o formato de referência escrito é `condition-name-1 [IN|OF data-name-1]... [IN|OF file-name-1] [(subscript)...]` (Format 1 — DATA DIVISION) ou `condition-name-1 [IN|OF mnemonic-name-1]` (Format 2 — SPECIAL-NAMES). Qualification vem antes dos subscripts; `data-name-1` pode ser record-name; `file-name-1` deve ser identificado por FD/SD, é único no programa e só pode ocupar a última posição da cadeia; a qualification usa a hierarquia da conditional variable; quando a conditional variable exige subscripting, a condition-name exige a mesma combinação.
- `LANGUAGE_GUARANTEED` (cap. 7, "Resolution of names — Names within programs"): programas contidos e contendo PODEM definir condition-name/data-name/file-name/record-name com a mesma user-defined word; o conjunto de resolução é {nomes definidos em B} ∪ {nomes GLOBAL definidos em A e nos contendo}; a qualification é aplicada a esse conjunto; se mais de um recurso for identificado, no máximo um é local a B — o local a B vence e, sem local, vence o contendo mais próximo (step 3). A regra vale para DATA e FILE igualmente; condition-name subordinado a entry com GLOBAL é global ("A condition-name ... is global if that entry is subordinate to another entry that specifies the GLOBAL clause"); file-name é global se o FD tem GLOBAL. **O resolver atual NÃO implementa o step 3** — ver "Resolver precedence / namespace challenge" (dependência `BACKLOG-RES-004`).
- `LANGUAGE_GUARANTEED` (cap. 8 "Subscripting"): subscript é `integer | ALL | data-name [+|- integer] | index-name [+|- integer]`; `ALL` não pode ser especificado para condition-name; **data-name subscript pode ser qualificado**; o número de subscripts deve igualar as dimensões da tabela — validação post-binding/`ConditionValidation`.
- `ARCHITECTURE_GUARANTEED` (ADR-0012): a AST é produto de superfície anterior ao binding; não decide DATA/INDEX/CONDITION, não materializa lookup, candidates, predicates herdados ou expansão.
- `ARCHITECTURE_GUARANTEED` (INV-AST-002/003): estrutura deriva de contexts/tokens; todo node é alcançado exatamente uma vez em pre-order canônico. O builder do lowering corrigido usa SOMENTE children diretos do context (`conditionName()`, `inData()`, `conditionNameSubscriptReference()`) — nunca `nearestDescendants`/primeiro descendente (Erro H).
- `OBSERVED_IN_CURRENT_GRAMMAR` (fato de parse tree): `dataName`/`fileName`/`mnemonicName` são todos `cobolWord`; `inFile`/`inMnemonic` são sombreados por `inData`. A única informação de posição que sobrevive é a estrutura estática da rule: posições não-finais têm slots `inData*`; a última posição tem slot `inFile?`. Consequência: não-final → `QualifierTarget.DATA` (grammar-provado por posição); final → `QualifierTarget.UNSPECIFIED` (novo valor; a parse tree não classifica o namespace).
- `RESOLVER_BOUNDARY` (BACKLOG-RES-004): o resolver mapeia `UNSPECIFIED` → `{DATA}` conservadoramente — o conjunto de constraints é IDÊNTICO ao atual, a seleção de candidates é provadamente inalterada e nenhum caso que já resolvia muda. A ampliação para `{DATA, FILE}` (que destravaria condition-names qualificadas por file-name, hoje `DECLARATION_NOT_FOUND`) fica BLOQUEADA até o step 3 de "Resolution of names" existir — a ampliação sem a regra converte `RESOLVED` em `UNSUPPORTED_DIALECT_OPTION`/`AMBIGUOUS` no contracaso local-DATA × GLOBAL-FILE.
- `BOUNDED_UNSUPPORTED` (MNEMONIC): qualifier de mnemonic-name (IBM Format 2) chega à parse tree como `inData`; o modelo não possui namespace MNEMONIC nem coleta SPECIAL-NAMES — a surface preserva o qualifier escrito como `UNSPECIFIED` e a resolução permanece não suportada. Lacuna documentada.
- `UNCERTAIN`/post-binding: qual qualifier é a conditional variable, hierarquia e dimensões — pertencem a binding/`ConditionValidation`.
- `PROVENANCE CONTRACT A`: `DataReference.meta` = span da referência completa; `DataQualifier.meta` = span de cada qualification escrita; `SubscriptGroup.meta` = span do grupo; subscript expression tem meta própria; o baseName NÃO possui Meta independente (sem node sintético; precedente `DataReference.baseName` String).

## Resolver precedence / namespace challenge

**Contracaso (IBM-válido, executável na grammar atual, caracterizado em `qualifiedLocalDataNameCollidesWithOuterGlobalFileNameAcrossPrograms`):** OUTER com `FD Q IS GLOBAL` + record `OUTER-REC` + `88 C`; INNER (contido) com `01 Q` + `88 C`; INNER escreve `IF C OF Q` e `MOVE CUST-STATUS OF Q TO X`.

**Regra IBM (cap. 7, "Resolution of names"):** (1) o recurso é identificado aplicando qualification e regras de unicidade ao conjunto {nomes de B} ∪ {nomes GLOBAL de A e contendo}; (2) se sobrar mais de um, no máximo um é local a B; o local a B vence e, sem local, o contendo mais próximo. Precedência DEPOIS da qualification; igual para condition-name/data-name/file-name/record-name; independe de `QUALIFY(STANDARD/EXTEND)`.

**Candidate universe hoje (`C OF Q`):** occurrence CONDITION (falso gap do collector, preservado); `compatibleCandidates(C)` = [INNER C (local), OUTER C (GLOBAL — 88 subordinado a FD GLOBAL)]; `localAndInheritedGlobal` mantém ambos; `applyQualification(Q)`:
- **antes (hoje):** constraint `Q@{DATA}` — INNER C (ancestry `[Q@DATA]`) sobrevive; OUTER C (ancestry `[..., Q@FILE]`) é excluído por namespace → 1 candidate → `RESOLVED/QUALIFIED_HIERARCHY_MATCH` (candidate local por EXCLUSÃO acidental, não pela regra).
- **depois (hipotético DATA_OR_FILE):** constraint `Q@{DATA, FILE}` — ambos sobrevivem → 2 candidates → `qualifyExtend` mantém só o local fully-qualified → policy UNSPECIFIED → `UNSUPPORTED_DIALECT_OPTION` (com `QUALIFY(STANDARD)` seria `AMBIGUOUS`). **Regressão:** `RESOLVED → UNSUPPORTED_DIALECT_OPTION/AMBIGUOUS`, e o resultado continua errado perante o step 3 (que selecionaria o local em qualquer modo).

**A referência DATA equivalente (`CUST-STATUS OF Q`, target `DATA_OR_FILE` pré-existente de `qualifiedDataNameFormat1`) já devolve hoje `UNSUPPORTED_DIALECT_OPTION` com 2 candidates** — o defeito é preexistente, geral (afeta DATA e CONDITION, namespaces DATA e FILE) e independente deste slice; o slice apenas o exporia na superfície de condition-name ao ampliar o target.

**Conclusão de escopo:** a ampliação NÃO pode entrar no Slice 4 sem o step 3. O step 3 é regra geral de resolução (programa-local após qualification), não pertence a este slice → registrada como **`BACKLOG-RES-004`** (work item de risco alto, futuro). O Slice 4 usa o alvo conservador `UNSPECIFIED` com mapeamento `{DATA}` no resolver — nenhum caso atual muda, nenhuma política é alterada, e a resolução de condition-names qualificadas por file-name permanece `DECLARATION_NOT_FOUND` (como hoje) até a dependência existir.

## Linguagem versus grammar

| Forma COBOL | IBM permite? | Grammar branch observado | Informação semanticamente comprovada |
| --- | --- | --- | --- |
| `condition-name OF data-name` | Sim (Format 1; data-name pode ser record-name) | `inData` | escrita; target DATA somente em posição não-final (grammar `inData*`) |
| `condition-name IN data-name` | Sim (IN/OF equivalentes) | `inData` | idem |
| `condition-name OF file-name` | Sim (Format 1; FD/SD, última posição) | `inData` (inFile sombreado) | escrita; posição final → UNSPECIFIED; resolução por file-name bloqueada por BACKLOG-RES-004 |
| `condition-name IN file-name` | Sim | `inData` (inFile sombreado) | idem |
| `condition-name OF mnemonic-name` | Sim (Format 2, SPECIAL-NAMES) | `inData` (inMnemonic sombreado) | escrita; MNEMONIC não distinguível e não modelado — bounded |
| `condition-name OF data-name(subscript)` | Sim (subscripts após qualification) | `inData` + `conditionNameSubscriptReference` | subscripts pertencem à referência (attach estrutural) |
| `condition-name(subscript) OF data-name` | Não (formato IBM) | grammar-rejeitado | rejeição sintática |
| `condition-name OF data-name(sub OF sub-group)` | Sim ("data-name-3 can be qualified") | `inData` raiz + `qualifiedDataName` no subscript | o qualifier interno pertence ao subscript, NÃO à raiz |
| `condition-name OF data-name OF file-name` | Sim (file-name por último) | dois `inData` | não-final → DATA; final → UNSPECIFIED |

## Decisão de modelagem — C vs D revisitado

O round 1 escolheu C (node novo + contrato compartilhado) e rejeitou D argumentando que o Slice 5 dependeria de `grammarRule`/parent inspection/reparse. O challenge do round 2 falsificou esse argumento: a árvore do Slice 3 já carrega a posição (containers tipados + contexto da recursão do collector), e o coletor já faz distinção posicional por papel/contexto em outros pontos (`EvaluateSelectorContext`, `StatementOperandContext`, `ReferenceRole`).

| Critério | C — new node + contract | D — DataReference corrigido |
| --- | --- | --- |
| informação lossless | completa (node + payload) | completa (mesmo builder corrigido; posição vive na árvore) |
| distingue contextual tail | wrapper existente + type do inner | wrapper existente (Slice 3) |
| exige grammarRule no Slice 5 | não | não (contexto estrutural carregado pelo próprio collector) |
| exige parent inspection frágil | não | não (o collector carrega o contexto na própria recursão; nenhum consumer atual precisa da distinção fora dele) |
| resolver changes | re-tipo do contrato + mapping UNSPECIFIED | apenas 1 case `UNSPECIFIED → {DATA}` (policy-preserving) |
| collector changes | branch novo para o node | **zero** (`addDataReference` existente já percorre qualifiers/subscripts do `DataReference` corrigido) |
| snapshot changes | label/attributes + tail label | zero |
| coverage | métrica `dataReferences` perde condition surfaces (baseline) | zero (métricas inalteradas) |
| provenance | meta única do node; Contrato A | idem (meta do `DataReference` corrigido) |
| identity/pre-order | node novo em `permits`/children; tail re-tipado | identidade atual preservada (mesmo tipo, fields corrigidos) |
| risco de over-modeling | node cuja única informação além do payload é a posição — já derivável | nenhum tipo novo; consistente com precedentes SET/EVALUATE (payload neutro + context) |
| OCP/abstração | sealed interface com 2 implementadores nascida de refactor | nenhuma abstração nova |
| custo de implementação | alto (Ast/AstBuilder/collector/resolver/snapshot/coverage/testes) | baixo (AstBuilder + valor de enum + 1 case no resolver + testes) |
| CICS classifier | sem delta | delta bounded: shape degenerada `IF DFHRESP(X)` passaria a casar `hasSupportedShape` (documentado/monitorado) |

**Conclusão: D.** O node novo não carrega informação de surface que a árvore já não possua; seu único acréscimo seria codificar a posição num tipo — exatamente o fato que o repositório já modela com containers tipados + contextos estruturais (SET/EVALUATE). C dividiria o mesmo nominal escrito (`FLAG-88`) entre `DataReference` (SET) e `ConditionNameReference` (IF) por posição, criando inconsistência nova; e o contrato `NominalReference` nasceria de um refactor sem segundo domínio real (Finding 3: "nominal" no repositório é o adjetivo do domínio de binding como um todo — DATA/CONDITION/INDEX/PROCEDURE/FILE/PROGRAM —, não o contrato de uma referência qualificada). D preserva identidade, provenance, métricas de coverage e o comportamento do resolver com o menor blast radius; o Slice 5 recebe o hand-off por contexto estrutural (pseudocódigo em `state.md`, round 2, challenge 8).

## Comportamento esperado

- `IF FLAG-OK` → `DataReference{baseName="FLAG-OK", qualifiers=[], subscriptGroups=[]}` com span dos tokens escritos; nenhum clone, nenhuma corrupção; a posição standalone é a estrutura (condition de `IfStatement`).
- `IF FLAG-88 OF CUSTOMER` → `DataReference{baseName="FLAG-88", qualifiers=[OF CUSTOMER]}`; único qualifier = última posição → target `UNSPECIFIED`.
- `IF FLAG-88 OF CUSTOMER(I, J)` → qualification e dois subscripts coexistem; grupo `(I, J)` com children próprios; `I` e `J` ganham occurrence `SUBSCRIPT` pela política pré-existente (sem mudança de collector).
- `IF FLAG-OK OF SUB-GRP OF GROUP-A` → dois qualifiers na ordem; `SUB-GRP` → `DATA`; `GROUP-A` → `UNSPECIFIED`.
- `IF FLAG-OK OF CUSTOMER(SUB OF SUB-GROUP)` → root qualifiers = `[CUSTOMER]` somente; o subscript é `DataReference(baseName="SUB", qualifiers=[OF SUB-GROUP])` próprio.
- `A = B OR C` → `ContextualConditionTail` com `DataReference{baseName="C"}` interno (inalterado em tipo, corrigido em estrutura); `A = B OR FLAG-ON(IDX)` → tail com interno completo.
- `NOT FLAG-OK`, `FLAG-88 AND A = B`, `(FLAG-88)` → as shapes existentes participam sem mudança de topology.
- Forma aceita pela grammar sem suporte modelado: `PreservedExpression` fail-closed, como hoje.

## Comportamento diante de incerteza

- A distinção condition surface vs contextual tail vs relation operand é posicional/estrutural (containers tipados + contexto do collector), nunca por `grammarRule`.
- O namespace do último qualifier é `UNSPECIFIED` na surface; o resolver o consome conservadoramente como `{DATA}` (nenhum caso muda) até `BACKLOG-RES-004` permitir `{DATA, FILE}`. MNEMONIC permanece lacuna bounded.
- Subscript `ALL`, shapes grammar-only (`FLAG-88(I)(J)`) e validação de hierarquia/dimensões permanecem sem decisão nesta fase.
- O falso gap do collector (`grammarRule == conditionNameReference ⇒ CONDITION`) permanece observável após este slice (Slice 5).

## Fora de escopo

Lookup nominal, symbol IDs, declaration links, `admissibleKinds` definitivos, expansão de predicates herdados, `ConditionSemantics`, `ConditionValidation`, ocorrências contextuais (Slice 5), `SEARCH WHEN` (Slice 6), CFG/dataflow, alteração de grammar, alteração do manifesto de coverage, mudanças nos paths `identifier`/`qualifiedDataName`/`tableCall` (SET/EVALUATE/relation operands), resolução de qualifier occurrences de file-names, e **o step 3 de "Resolution of names" (`BACKLOG-RES-004`) e a ampliação `UNSPECIFIED → {DATA, FILE}` no resolver** — registrados como dependência separada; o slice não os implementa nem os expõe como regressão.

## Consumer impact analysis

Classificação: **A** = realmente DATA-specific; **B** = nominal-reference structural; **C** = incidental coupling; **D** = fora do slice.

| Consumer | Assumption atual | Quebra com a correção D? | Semântica ou mecânica? | Ação necessária |
| --- | --- | --- | --- | --- |
| `AstBuilder.dataReference` (`firstDescendant(qualifiedDataName)`) | fonte da corrupção | — | — | **B** — novo lowering por children diretos (`conditionName()`, `inData()`, `conditionNameSubscriptReference()`); targets: não-final `DATA`, final `UNSPECIFIED`; `visitConditionNameReference` uniforme; `dataReference`/`tableReference` (identifier paths) intocados |
| `Ast.QualifierTarget` | enum {DATA, FILE, DATA_OR_FILE} | — | Mecânica | **B** — novo valor `UNSPECIFIED` |
| `DataAndIndexReferenceResolver.qualifierConstraints` | switch exaustivo sobre target | novo valor sem case não compila | Mecânica | **B** — 1 case `UNSPECIFIED → {DATA}` (policy-preserving; constraint idêntica à atual); ampliação bloqueada por `BACKLOG-RES-004` |
| `ReferenceOccurrenceCollector` | `addDataReference` percorre qualifiers/subscripts | não (o `DataReference` corrigido passa pelo MESMO branch; subscripts recuperados pela política existente) | — | **D** — zero mudanças (voltou a `must_not_change`) |
| `Ast.children`/`AstScopeIndex` | children do `DataReference` | não | — | **D** — zero |
| `AstSnapshot` | branch `DataReference` existente | não | — | **D** — zero (voltou a `must_not_change`) |
| `CoverageSnapshot` | conta `DataReference` | não (condition surfaces continuam `DataReference`) | — | **D** — zero; baselines de corpus mudam apenas onde a estrutura legítima foi materializada (qualifiers/subscripts que hoje não existiam) |
| `CicsIntrinsicClassifier` | `DataReference` com shape CICS | delta bounded: `IF DFHRESP(X)` (shape degenerada) passaria a casar `hasSupportedShape` | Incidental | **C** — nenhuma mudança de código; delta documentado e coberto por monitoramento |
| `CobolReferenceResolver`/`SymbolTableBuilder`/`ReferenceOccurrences`/`ReferenceResolution` | — | não | — | **D** — zero (permanecem `must_not_change`) |
| Testes | asserts que caracterizam a corrupção atual | migram no Commit 2 para a estrutura corrigida; `SemanticConditionContextDiscoveryTest`/`ConditionSurfaceAstTest` permanecem verdes sem migração de tipo (tail interno continua `DataReference`) | Mecânica | **B** — migração dos asserts de caracterização + novos oracles |

## Regras de domínio relacionadas

`docs/domain/conditional-expressions.md` (autoridade IBM registrada; COND-P08), `docs/domain/semantic-ast.md`, `docs/domain/provenance.md`, `docs/domain/reference-resolution.md` (relido integralmente nesta rodada: reconhece explicitamente que "estrutura de scopes não é, por si só, a relação completa de visibilidade COBOL" — GLOBAL/shadowing/qualificação participam; o step 3 é a lacuna registrada em `BACKLOG-RES-004`), `docs/evals/conditional-expression-oracles.md` (COND-P08, COND-A08, COND-N05).

## ADRs/invariantes relacionados

ADR-0003, ADR-0005, ADR-0009, ADR-0012 (Accepted); INV-AST-001, INV-AST-002, INV-AST-003, INV-PROV-002, INV-COND-001, INV-COND-002, INV-RES-001, INV-DET-001.
