# Avaliação — WORK-COND-007

## O que prova corretude

Esta avaliação prova caracterização, não completude normativa de COBOL. A evidência primária é a cadeia de produtos da pipeline: source/provenance → parse tree → AST semântica → symbol table/scopes → occurrences → resolution/diagnostics. A relação durável exigida quando a forma é compreendida é `nominal escrito → nominal AST → occurrence → resolution entry`, com `referenceAstNodeId`, scope e provenance coerentes. Um reference expression com qualifier, subscript ou reference modification não é reduzido a um único token.

Contagens de AST, occurrences e resolução abaixo são produtos observados no commit `8c6f449` antes da expansão, ou na mesma pipeline após a cópia do corpus. Não são snapshots normativos globais. Para source que termina antes da AST, a célula é explicitamente parcial e não é usada como oracle semântico.

## Classes positivas

- relation completa e abreviada, com `AND`, `OR`, `NOT` e parênteses preservados;
- condition-name standalone e declaração nível 88 sem desaparecimento;
- qualification, subscripts e componentes com policies próprias;
- `PERFORM UNTIL`, `EVALUATE` booleano e não booleano, `SEARCH WHEN` e `SEARCH ALL` preservados estruturalmente;
- `DATA`, `INDEX` e `CONDITION` routed de acordo com a forma estrutural;
- provenance original/expanded, IDs e ownership de branches estáveis;
- análise parcial fechada para COPY ausente ou construct não suportado.

## Classes negativas

- nominal presente no source mas ausente do AST;
- AST nominal sem occurrence, occurrence sem entry de resolution, entry duplicada ou candidate inexistente;
- qualifier, subscript, precedência, `NOT`, `AND`/`OR`, branch ownership ou `SEARCH WHEN` desaparecidos;
- sucesso artificial após COPY inventado, normalização do source real ou fallback textual;
- classificação de um `WHEN` como CONDITION somente por nome, programa ou regex;
- conversão de unresolved/ambiguous/unsupported em resolved.

## Classes ambíguas

Ambiguidades legítimas, como dois símbolos visíveis com o mesmo nome sob `REDEFINES`, permanecem `AMBIGUOUS`. A semântica não escolhe um candidate arbitrariamente. `SEARCH ALL` é preservado como `all=true`, mas sua validade normativa não é inferida deste checkpoint.

## Casos adversariais

Foram usados, quando a pipeline permitiu, programas grandes e pequenos; CICS, SQL, IMS/DLI, MQ/VSAM, batch, CALL, COPY e 88-level. Os casos que não atravessaram a pipeline também são adversariais para as fronteiras de source format e preprocessing. A seleção inclui um `SEARCH ALL`, múltiplos `EVALUATE TRUE`, condições agrupadas e abreviadas, tabelas e subscripts. Não há `SEARCH` ordinário/`SEARCH WHEN` adicional no CardDemo fixado.

## Casos de regressão

O oracle WAUX-like durável permanece em `ContextualConditionOccurrenceTest.longAbbreviatedRelationChainResolvesEveryContextualDataTail`: `IF INPUT-CODE = X1 OR X2 OR X3 OR X4`, com X1–X4 DATA, exige três tails contextuais, admissibility `{DATA, INDEX, CONDITION}`, candidate DATA e uma ocorrência/entry para cada nominal. O fonte real WAUX original não está disponível no corpus autorizado: `real WAUX source unavailable`.

O fixture existente `src/test/resources/cobol/resolution/evaluate-condition-names.cbl` continua sendo o contraexemplo sintético para selectors booleanos simples. A regressão proposta para o finding F-01 abaixo é deliberadamente proposta, não implementada neste checkpoint.

## Propriedades/relações metamórficas

As seguintes transformações foram usadas para estruturar a refutação e ficam propostas como regressions futuras onde o caso real for confirmado:

- trocar a declaração do tail entre DATA, INDEX e CONDITION, sem mudar a surface da relation;
- remover a declaração e observar unresolved, sem alterar AST/occurrence;
- adicionar/remover qualification e subscript, esperando somente a mudança estrutural/policy prevista;
- adicionar/remover `OR` tail, `AND` tail, `NOT` e parênteses equivalentes, preservando precedência e ownership;
- trocar `SEARCH` por `SEARCH ALL` apenas quando a forma for válida para comparação, preservando `all` e sem inventar validation;
- trocar `EVALUATE TRUE` por `EVALUATE DATA-ITEM` para provar que somente o contexto subject booleano altera a policy;
- trocar `WHEN condition-name` por `WHEN DATA-ITEM = literal` para refutar a hipótese de que todo selector seja CONDITION;
- duplicar uma branch `WHEN` ou adicionar `ALSO` para validar bijeção e correspondência posicional;
- remover o COPY que contém a declaration e confirmar `COPY_NOT_FOUND`/unresolved, nunca um stub.

## Expectativas de escala

As medições são observacionais, executadas uma vez por programa com `ANALYZER_LOG_LEVEL=DEBUG`, portanto não caracterizam desempenho de hardware. O pipeline permaneceu finito nos sete programas completos; não foi possível medir peak memory porque o repositório não oferece mecanismo existente para isso.

## Corpus existente

O corpus versionado antes do Slice 7 tinha três programas. Os contadores lexicais de IF/EVALUATE/PERFORM/SEARCH são triagem de source; condition count, condition-name references, unresolved, ambiguous e unsupported vêm dos produtos AST/resolution quando disponíveis. `qualified` e `subscripted` contam referências com a forma correspondente; `abbreviated` conta tails abreviados materializados.

| Program | LOC | COPYs usados | Conditions | IF | EVALUATE | PERFORM UNTIL | SEARCH | Condition-name refs | Abbreviated | Qualified | Subscripted | Unresolved | Ambiguous | Unsupported |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| CBSTM03A.CBL | 923 | COSTM01, CUSTREC, CVACT01Y, CVACT03Y | 42 | 30 | 5 | 0 | 0 | 3 | 9 | 0 | 15 | 0 | 0 | 95 |
| CBSTM03D.CBL | 931 | COSTM01, CUSTREC, CVACT01Y, CVACT03Y | 42 | 30 | 5 | 0 | 0 | 3 | 9 | 0 | 15 | 0 | 0 | 95 |
| COACTUPC.cbl | 4236 | COACTUP, COCOM01Y, COTTL01Y, CSDAT01Y, CSLKPCDY, CSMSG01Y, CSMSG02Y, CSSETATY, CSUSR01Y, CSUTLDPY, CSSTRPFY, CSUTLDWY, CVACT01Y, CVACT03Y, CVCRD01Y, CVCUS01Y, DFHAID, DFHBMSCA | 463 | 327 | 10 | 0 | 0 | 277 | 3 | 491 | 10 | 140 | 2 | 0 |

`DFHAID` e `DFHBMSCA` não estão no corpus existente; os dois COPYs são parte dos `unresolved` observados. O source existente não foi alterado.

## Candidate screening and diversity selection

Foram avaliados 44 candidatos COBOL do checkout fixado, em `app` e módulos do CardDemo. A tabela abaixo é a tabela de triagem usada antes do congelamento. `COPY count` é ocorrência textual, inclusive repetições; os demais contadores de construct são triagem por source. `Diversity score` é apenas uma soma reproduzível de presença de IF, EVALUATE, PERFORM, SEARCH (bônus maior), 88, qualification, tables, CICS/SQL, CALL, logical conditions e tamanho extremo; não é uma métrica semântica nem um oracle.

| Program | LOC | COPY count | IF | EVALUATE | PERFORM | SEARCH | 88-level | Qualification | Tables | CICS/SQL | Diversity score |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | ---: |
| COACTUPC | 4236 | 56 | 327 | 20 | 61 | 0 | 212 | yes | yes | yes | 9 |
| COCRDSLC | 887 | 13 | 68 | 8 | 19 | 0 | 28 | yes | yes | yes | 9 |
| COPAUS1C | 604 | 10 | 34 | 10 | 34 | 1 (`SEARCH ALL`) | 23 | yes | yes | yes | 10 |
| COTRTUPC | 1702 | 13 | 98 | 26 | 40 | 0 | 64 | yes | yes | yes | 9 |
| COTRTLIC | 2098 | 11 | 172 | 32 | 69 | 0 | 68 | yes | yes | yes | 9 |
| COACCT01 | 620 | 7 | 20 | 18 | 32 | 0 | 0 | no | no | yes (MQ/VSAM) | 7 |
| CBSTM03A | 924 | 4 | 30 | 9 | 33 | 0 | 6 | yes | yes | no | 9 |
| CBPAUP0C | 386 | 2 | 36 | 4 | 19 | 0 | 22 | yes | no | no (IMS/DLI) | 5 |
| CBACT01C | 430 | 2 | 44 | 0 | 36 | 0 | 2 | yes | yes | no | 7 |
| COUSR01C | 299 | 8 | 8 | 6 | 20 | 0 | 27 | yes | yes | yes | 8 |

This is not an alphabetical first-ten selection: it deliberately contains a large CICS base, CICS/SQL and MQ/VSAM, batch, IMS/DLI, CALL-heavy code, tables, 88s, qualifications and the only CardDemo `SEARCH ALL` found. CardDemo has no ordinary `SEARCH WHEN` sample beyond this `SEARCH ALL` branch and no distinct `PERFORM UNTIL` across every selected style; the synthetic oracle covers the missing shape.

## Selection self-refutation

- **CS-01 — mesmos subsystem?** Não. A seleção cobre base CICS, transaction type DB2, authorization IMS/DB2/MQ, VSAM/MQ e batch.
- **CS-02 — tamanho semelhante?** Não. Vai de 299 a 4236 LOC; há pequenos, médios e grandes.
- **CS-03 — ignorando SEARCH/EVALUATE?** Não. `COPAUS1C` contém o único `SEARCH ALL`; há EVALUATE em nove selecionados e muitos branches em `COTRTUPC`/`COTRTLIC`.
- **CS-04 — ignorando condition-names?** Não. Nove têm 88-level; `COACTUPC` tem 212 declarações 88 na triagem.
- **CS-05 — escolhidos só porque passam?** Não. `COPAUS1C`, `CBPAUP0C` e `COTRTLIC` foram mantidos apesar de falhas de preprocessing/normalização.
- **CS-06 — excluído mais diverso?** A triagem dos 44 foi revisada; nenhum excluído acrescentou simultaneamente construct raro e uma integração ausente na seleção. A ausência de `SEARCH` ordinário é uma propriedade do corpus, não filtro de sucesso.
- **CS-07 — closure fecha declarations?** Fecha as declarations disponíveis; oito nomes de COPY permanecem explicitamente `COPY_NOT_FOUND`, sem stub. A closure não fecha dependências de `EXEC SQL INCLUDE`/sistema.
- **CS-08 — programa adiciona pouca diversidade?** `CBACT01C` é pequeno, mas é o controle batch/table sem integração e tem 44 IF; removê-lo elimina esse contraste. `COUSR01C` adiciona o extremo pequeno CICS/88/qualification.
- **CS-09 — bias para simples?** Não. Cinco programas têm mais de 887 LOC e quatro têm mais de 26 EVALUATE/IF ou 88; o maior programa domina volume, mas não é o único perfil.
- **CS-10 — substituição melhora coverage?** Não materialmente nesta rodada. Substituições por candidatos sem SEARCH ou sem nova integração reduziram diversidade; o candidato com maior score adicional foi `COPAUS1C`, já selecionado.

## CardDemo condition inventory

Os números abaixo são occurrences/AST materializados nos sete programas que completaram a pipeline. Para `COPAUS1C` e `COTRTLIC`, somente a presença em source é afirmada, porque o produto não atravessou preprocessing/normalização.

| Categoria | Number of examples | Selected program examples | Unexpected findings |
| --- | ---: | --- | --- |
| standalone conditions | 420 condition-name reference entries na análise completa; source 88s em todos os perfis com 88 | `COACTUPC`, `COCRDSLC`, `COTRTUPC` | F-01 em subset combinado com `AND`/`NOT` |
| relations | 457 nodes de relation completos | todos os sete completos | nenhum novo além de gaps de declarations |
| abbreviated relations | 15 tails materializados | `CBSTM03A`, `COUSR01C` e conditions relacionais no conjunto | nenhum; WAUX-like passa |
| AND | 95 connectors | `COACTUPC`, `COTRTUPC`, `COCRDSLC`, `CBACT01C` | F-01 quando a primeira parte é condition-name em `EVALUATE TRUE` |
| OR | 164 connectors | `COACTUPC`, `COTRTUPC`, `CBSTM03A` | nenhum |
| NOT | 17 negated nodes | `COACTUPC`, `COTRTUPC`, `COUSR01C` | nenhum novo |
| nested/grouped | 65 grouped nodes | `COACTUPC`, `COTRTUPC`, `COCRDSLC`, `CBSTM03A` | nenhum |
| condition-names | 420 `conditionNameReference` entries materializadas; 88-level source em nove candidatos selecionados | `COACTUPC`, `COTRTUPC`, `COCRDSLC` | F-01: 34 entries com invalid namespace no total |
| qualification | 615 qualified references no conjunto completo observado | `COACTUPC`, `COCRDSLC`, `COTRTUPC`, `COUSR01C` | nenhum desaparecimento estrutural |
| subscripts | 35 occurrence roles de subscript nos sete completos | `CBACT01C`, `CBSTM03A`, `COACCT01`, `COACTUPC`, `COCRDSLC`, `COUSR01C` | nenhum desaparecimento estrutural |
| EVALUATE | 47 AST statements nos sete completos; 20 adicionais aparecem na triagem source de `COACTUPC` | `COACTUPC`, `COTRTUPC`, `COACCT01` | F-01 em selectors combinados; value selectors não devem virar CONDITION |
| PERFORM UNTIL | 8 source examples nos dez selecionados; 4 atravessam a pipeline e `COTRTLIC` ainda tem 4 VARYING UNTIL | `COACCT01`, `CBSTM03A`, `CBPAUP0C`, `CBACT01C`, `COTRTLIC` | `EXEC DLI`/TAB impedem produto em dois perfis |
| SEARCH | 1 | `COPAUS1C` | somente `SEARCH ALL`; validação normativa permanece futura |
| SEARCH ALL | 1 | `COPAUS1C:319–328` | nenhum desaparecimento source-level; pipeline stopped at `EXEC DLI` |

Os números de condition-name entries corrigem uma distinção importante: 88-level declarations não são a mesma coisa que references; o inventory não transforma a contagem de declarations em occurrences. A soma de occurrences de condition-name por programa completo é `7 + 3 + 4 + 277 + 24 + 103 + 2 = 420`.

## CardDemo baseline

`AST` é quantidade de nodes AST; `Occurrences` é quantidade de entries coletadas; `Resolution` mostra status resolvidos/externalizados quando o produto existe. `Condition findings` registra somente findings relevantes à surface/contexto, não todos os gaps de cobertura da aplicação.

| Program | Parse | AST | Occurrences | Resolution | Unresolved | Ambiguous | Unsupported | Condition findings |
| --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| CBACT01C | PASS | 943 | 216 | 210 resolved + 2 external | 0 | 0 | 4 | none; relation 20, logical 5, class 1 |
| CBSTM03A | PASS | 2740 | 580 | 471 resolved + 14 external | 0 | 0 | 95 | none; relation 18 + 9 abbreviated, logical 11, grouped 2, negated 2 |
| COACCT01 | PASS | 1122 | 404 | 329 resolved + 9 external | 66 | 0 | 0 | missing MQ COPYs drive unresolved; relation 5, logical 1 |
| COACTUPC | PASS | 10542 | 3097 | 2954 resolved + 1 external | 140 | 2 | 0 | F-01: 5 `INVALID_NAMESPACE_FOR_CONTEXT`; relation 263, tails 3, logical 168, grouped 45, negated 6, class 6 |
| COCRDSLC | PASS | 2148 | 489 | 409 resolved | 80 | 0 | 0 | F-01: 1 `INVALID_NAMESPACE_FOR_CONTEXT`; relation 53, tail 1, logical 15, grouped 1, negated 1, class 2 |
| COPAUS1C | PARTIAL: preprocessing rejects `EXEC DLI` | — | — | — | — | — | — | source has `SEARCH ALL` and EVALUATE; F-03 |
| COTRTLIC | PARTIAL: normalization rejects upstream TAB | — | — | — | — | — | — | source has SQL/CICS, EVALUATE and PERFORM UNTIL; F-04 |
| COTRTUPC | PASS | 3375 | 838 | 702 resolved | 134 | 2 | 0 | F-01: 28 `INVALID_NAMESPACE_FOR_CONTEXT`; relation 76, tails 2, logical 53, grouped 17, negated 6 |
| COUSR01C | PASS | 944 | 183 | 169 resolved | 14 | 0 | 0 | none; relation 13 + 6 abbreviated, logical 6, negated 2 |

Across the seven complete programs: 21814 AST nodes, 5807 occurrences, 5244 resolved (sum `210+471+329+2954+409+702+169`); plus 26 external (`2+14+9+1=26`), 434 unresolved (`0+0+66+140+80+134+14`), 4 ambiguous, 99 unsupported. The selected `COACTUPC` and existing `COACTUPC` share the source identity but their condition-surface count differs (491 vs 463) because the CardDemo closure/source variant is being characterized independently; no baseline was overwritten.

## Finding extraction

### Raw findings

| ID | Observation | Primary classification | Evidence |
| --- | --- | --- | --- |
| F-01 | In `EVALUATE TRUE`, a condition-name used as the first operand of a combined `WHEN` (`AND`/`NOT` plus value relation) is emitted as `DATA/{DATA}`, status unresolved, reason `INVALID_NAMESPACE_FOR_CONTEXT`, although the 88 declaration is present. | `NEEDS_ARCHITECTURAL_DECISION` | COACTUPC 5, COCRDSLC 1, COTRTUPC 28; exact original provenance retained |
| F-02 | Missing system/MQ COPYs cause unresolved symbols and/or incomplete products. | `EXPECTED_UNRESOLVED` | 8 unique names; no stubs; `COPY_NOT_FOUND` in provenance |
| F-03 | `EXEC DLI` stops preprocessing with the existing closed failure policy. | `GRAMMAR_GAP` | COPAUS1C and CBPAUP0C; source retained |
| F-04 | `COTRTLIC` contains one TAB in fixed-format source and normalization stops. | `CORPUS_INVALID` | upstream byte preserved; line 1811 |
| F-05 | `WS-EDIT-DATE-X` has two valid visible declarations under REDEFINES and remains ambiguous. | `EXPECTED_BEHAVIOR` | COACTUPC and COTRTUPC, 2 each |
| F-06 | Existing `PRESERVED_NAMED`/unsupported record forms remain `UNSUPPORTED_GRAMMAR_FORM`. | `UNSUPPORTED` | CBSTM03A has 95; no condition surface disappearance |
| F-07 | `SEARCH ALL` is structurally present but normative sorted/key validation is not claimed. | `NORMATIVE_VALIDATION_GAP` | COPAUS1C source lines 319–328; product blocked by F-03 |
| F-08 | No ordinary `SEARCH WHEN` sample exists in the fixed CardDemo candidate set. | `EXPECTED_BEHAVIOR` | source scan of all 44 candidates; synthetic SEARCH WHEN suite remains oracle |

Classification is per finding record and intentionally not per occurrence. `CONFIRMED_BUG` is zero in this checkpoint. F-01 is a real, reproducible semantic concern, but it is already delimited as `BACKLOG-RES-003` and requires an architectural context decision; it is not authorized as a Slice 7 production fix.

## Bug-refutation record — F-01

**BR-01 — Qual comportamento foi observado?**

In `COACTUPC.cbl:964–965`, `WHEN ACUP-DETAILS-NOT-FETCHED AND CDEMO-PGM-ENTER` produces two condition-name reference occurrences with `kind=DATA`, `admissibleKinds={DATA}`, `role=VALUE_READ`, `status=UNRESOLVED`, `reason=INVALID_NAMESPACE_FOR_CONTEXT`, and no candidates. Equivalent observations occur at `COCRDSLC.cbl:339–340` and 28 locations in `COTRTUPC.cbl`.

**BR-02 — Qual contrato atual aparentemente foi violado?**

The condition contract and `BACKLOG-RES-003` say a condition-name selector corresponding to a boolean `EVALUATE TRUE/FALSE` subject must be routed to CONDITION. The current `EVALUATE TRUE` simple-selector fixture passes, but the combined selector loses that context.

**BR-03 — O source é válido segundo a grammar?**

Yes for the three complete products: parser errors are zero and the AST contains the `EVALUATE` branches and written nominals. `COACTUPC`, `COCRDSLC` and `COTRTUPC` all complete normalization, preprocessing, parsing and AST construction.

**BR-04 — O source é normativamente válido COBOL ou apenas grammar-accepted?**

The `EVALUATE TRUE` form with a condition-name combined with a condition/relation is a valid condition surface in the configured COBOL dialect. This judgment is supported by the existing IBM-oriented contract and the upstream programs; full dialect validation remains outside the current parser evidence.

**BR-05 — O AST realmente perdeu informação?**

No written nominal is absent: each finding has an AST-backed `conditionNameReference`, exact original line/column and an `Evaluate` branch. The loss is contextual: the current lowered selector path does not carry enough boolean-subject context through the occurrence collector for this combined form. This is not an AST token disappearance.

**BR-06 — A occurrence está errada ou apenas unresolved?**

The occurrence is wrong for the desired contract, not merely unresolved: it is `DATA/{DATA}` instead of a CONDITION-capable context. Its unresolved result is consistent with that wrong input because the resolver correctly finds no DATA candidate for a condition-name symbol.

**BR-07 — O resolver recebeu a occurrence correta?**

No. The resolver received exactly the collected occurrence, but that occurrence already had the DATA-only policy. No resolver candidate-selection defect was observed.

**BR-08 — A declaration realmente pertence ao namespace esperado?**

Yes semantically. The symbol products contain local `CONDITION_NAME` symbols for the affected names, such as `ACUP-DETAILS-NOT-FETCHED`, `CDEMO-PGM-ENTER`, `TTUP-SHOW-DETAILS` and `TTUP-DETAILS-NOT-FOUND`, each with level `88`/`CONDITION_88`. The symbol table stores the containing DATA hierarchy as `ns=DATA`; that is not evidence that the 88 declaration is a DATA candidate.

**BR-09 — Qualification/scope explicam o resultado?**

No for the representative entries. They are bare local condition-names with no qualifier or subscript, and the declarations are visible in the same program unit. Missing `DFHAID` explains some other unresolved names, but not local TTUP/ACUP/CDEMO declarations used in F-01.

**BR-10 — Existe ambiguidade legítima?**

Not for F-01: those entries have zero candidates, not multiple candidates. The separate `WS-EDIT-DATE-X` cases have two candidates and are classified F-05 expected ambiguity.

**BR-11 — Consigo construir um programa mínimo em que o comportamento atual está correto?**

Yes. `EVALUATE DATA-ITEM WHEN DATA-ITEM` and `EVALUATE TRUE WHEN DATA-ITEM = 'Y'` must remain DATA/value relations. The existing simple `EVALUATE TRUE WHEN STATUS-OPEN` oracle also demonstrates that a direct condition-name path can be recognized without converting every selector to CONDITION.

**BR-12 — Consigo construir um contraexemplo que falsifica minha hipótese de bug?**

Yes, partially: a simple standalone `WHEN STATUS-OPEN` resolves as CONDITION in the current suite, and an `EVALUATE TRUE ALSO DATA-ITEM WHEN FLAG-ON ALSO DATA-ITEM` case must preserve CONDITION only in the boolean position and DATA in the value position. These counterexamples refute a claim that the whole EVALUATE implementation or resolver is broken. They do not refute the narrower combined-selector context gap.

**BR-13 — O comportamento contradiz IBM/ADR/invariant/test durável?**

It contradicts the intended domain contract and is explicitly listed as the known `BACKLOG-RES-003` gap. It does not contradict the current conservative resolver behavior given the occurrence it receives, nor does it justify changing ADR-0012 in this checkpoint.

**BR-14 — A correção exigiria heurística local?**

It must not. A source-text, grammar-rule, name-prefix or CardDemo-specific branch would be rejected. A viable future correction requires an explicit selector/subject context derived from the parse/AST structure, with adversarial tests for boolean/value positions and `ALSO`; that is an architectural follow-up.

**BR-15 — Qual é a classificação final?**

`NEEDS_ARCHITECTURAL_DECISION`, with a known grammar/AST-context design gap component. It is not `CONFIRMED_BUG` in this Discovery checkpoint because the behavior is already documented, the resolver itself is correct for its input, and implementation authorization is absent. No production change was made.

### Minimal reproducer for F-01

```cobol
       IDENTIFICATION DIVISION.
       PROGRAM-ID. EVAL-CONTEXT-GAP.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  FLAGS PIC X.
           88  FLAG-ON VALUE 'Y'.
       01  OTHER-FLAG PIC X.
           88  OTHER-ON VALUE 'Y'.
       PROCEDURE DIVISION.
           EVALUATE TRUE
               WHEN FLAG-ON AND OTHER-ON
                   CONTINUE
               WHEN FLAG-ON
                   CONTINUE
               WHEN OTHER
                   CONTINUE
           END-EVALUATE
           GOBACK.
```

Expected future oracle: `FLAG-ON` in the boolean `WHEN` condition must remain CONDITION-capable; the value/relation counterexample `EVALUATE FLAGS WHEN FLAGS = 'Y'` must remain DATA. The current simple selector oracle is already covered; the combined-selector oracle is proposed, not enabled as a passing production expectation.

## Finding self-refutation

- **FR-01:** 1 raw finding initially looked like a production bug (F-01).
- **FR-02:** 0 survived as a newly confirmed production bug; F-01 survived as a bounded architectural gap already tracked by `BACKLOG-RES-003`.
- **FR-03:** 1 finding record is `EXPECTED_UNRESOLVED` (F-02), with 8 missing COPY names and 434 unresolved entries across complete products, not 434 independent bugs.
- **FR-04:** 1 finding record is a grammar/preprocessor gap (F-03); the 95 preserved forms in F-06 are an additional unsupported boundary.
- **FR-05:** 1 finding record is corpus/source invalid (F-04); upstream TAB was not removed.
- **FR-06:** 0 `CONFIRMED_BUG` findings.
- **FR-07:** Yes, F-01 would be heuristic if implemented by grammar rule, source text, name prefix or program special-case; such a fix is rejected.
- **FR-08:** Yes, F-01 needs an architectural decision about explicit EVALUATE selector context; no decision is made here.
- **FR-09:** F-01 is consistent with the already documented `BACKLOG-RES-003` and does not contradict ADR-0012 or product-identity invariants.
- **FR-10:** No. F-01 was checked through AST node/provenance, occurrence fields, resolution entries and symbol products; source text supplied locations, not the classification alone.

## Minimal reproducers and proposed regressions

| Finding | Minimal reproducer / mutation | Proposed regression oracle |
| --- | --- | --- |
| F-01 | `EVALUATE TRUE WHEN FLAG-ON AND OTHER-ON`; contrast `EVALUATE FLAGS WHEN FLAGS = 'Y'` and `EVALUATE TRUE ALSO FLAGS WHEN FLAG-ON ALSO FLAGS = 'Y'` | Assert selector context, CONDITION vs DATA admissibility, positional `ALSO`, one occurrence/entry per written nominal, and no name-based heuristic. Requires future architectural authorization. |
| F-02 | remove one available COPY or retain a missing `DFHAID` | Assert `COPY_NOT_FOUND`, partial claim and unresolved provenance; never synthesize declaration. Existing `PartialAnalysisMissingCopyTest` is the regression anchor. |
| F-03 | `EXEC DLI GU ...` in `COPAUS1C`/`CBPAUP0C` | Characterize closed preprocessing failure and preserve source-level inventory; do not treat failure as condition success. |
| F-04 | upstream `COTRTLIC.cbl` with its single TAB | Assert fixed-format normalization diagnostic and byte/source provenance; no fixture rewrite. |
| F-05 | duplicate visible `WS-EDIT-DATE-X` under REDEFINES | Assert two candidates and `AMBIGUOUS`, no arbitrary selection. Existing resolution ambiguity tests are the anchor. |
| F-07/F-08 | `SEARCH ALL ... WHEN ...` plus synthetic ordinary `SEARCH WHEN` | Assert `SearchStatement`/`SearchWhen`, `all=true`, ownership, contextual policy, and leave sorted/key normative validation future. Existing SearchWhen adversarial suite is the anchor. |
| WAUX-like | `IF INPUT-CODE = X1 OR X2 OR X3 OR X4` | Existing `ContextualConditionOccurrenceTest` asserts all tails and one-to-one product chain; real WAUX source unavailable. |

## Implementation recommendations

1. Human review F-01 as a separate architectural follow-up, not a local CardDemo exception.
2. If authorized, preserve explicit `EVALUATE` selector/subject position in AST products and derive occurrence routing structurally; add adversarial boolean/value/`ALSO` tests before production changes.
3. Keep F-02/F-03/F-04 as explicit completeness data until separate work items authorize broader dialect/source-format support.
4. Add a reusable corpus characterization harness only if it consumes the existing products and reports per-node provenance; do not freeze whole-program cardinality snapshots.
5. Consider adding the selected CardDemo source paths to a stable, opt-in corpus command rather than the mandatory semantic gate if execution cost or environment dependencies grow.

## Performance observacional

Timings are milliseconds from the current debug phase logger, one run per program. `Parse` means lexer + parser where relevant; `AST build`, `Occurrences` and `Resolution` are the corresponding semantic phases. `Total` is total analysis elapsed time. Failed rows stopped at the indicated phase.

| Program | LOC | Normalize | Preprocess | Parse | AST build | Occurrences | Resolution | Total | Peak memory |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| CBACT01C | 430 | 12 | 152 | 306 | 81 | 5 | 37 | 644 | unavailable |
| CBSTM03A | 924 | 19 | 197 | 402 | 118 | 14 | 50 | 865 | unavailable |
| COACCT01 | 620 | 18 | 213 | 510 | 92 | 12 | 50 | 947 | unavailable |
| COACTUPC | 4236 | 52 | 441 | 2682 | 278 | 30 | 147 | 3749 | unavailable |
| COCRDSLC | 887 | 21 | 204 | 633 | 123 | 15 | 60 | 1118 | unavailable |
| COTRTUPC | 1702 | 35 | 250 | 925 | 158 | 19 | 76 | 1540 | unavailable |
| COUSR01C | 299 | 11 | 190 | 426 | 91 | 12 | 39 | 821 | unavailable |
| COPAUS1C | 604 | 17 | 160 | — | — | — | — | 177 | unavailable; preprocessing failed |
| CBPAUP0C | 386 | 12 | 146 | — | — | — | — | 158 | unavailable; preprocessing failed |
| COTRTLIC | 2098 | 31 | — | — | — | — | — | 31 | unavailable; normalization failed |

The logger timings varied slightly between invocations and are retained only as a gross-regression observation. There is no peak-memory mechanism in the current harness.

## Gates and repository integrity

Pre-expansion baseline on `8c6f449`: `check-fast.sh` PASS; `check-semantic.sh` PASS. The final gate results, commit SHAs and PR are appended to this document after all corpus/document changes and before handoff. Required integrity check: `git diff --name-only -- src/main grammar semantic manifest` must be empty for this checkpoint.

After corpus and findings work, before the final documentation checkpoint: `verify-naming.sh` PASS and `check-fast.sh` PASS. The first post-corpus `check-full.sh` reached PASS for fast, semantic, structural artifact invariants and source-normalizer regression, then stopped only at naming because this document used a reserved legacy measurement word; the wording was corrected without changing the corpus. A clean final `check-full.sh` is required after this document commit.
