# Discovery — contextualização semântica de condições COBOL

Data da investigação: 2026-09-01

Base de produção observada: `main` em `9aba9a897cc7f45ba7da3a25079d66aee838ba55`

Branch de investigação original: `discovery/work-ast-002-slice-2-cross-product-integrity`

Branch de entrega: `discovery/semantic-condition-contextualization`

Escopo: parse tree → AST lowering → `ReferenceOccurrenceCollector` → symbol resolution
Restrição: nenhuma alteração de produção, gramática, parser, AST ou resolução foi realizada.

## Executive summary

O caso `IF A = B OR C OR D` reproduz uma classe real de defeitos, não um erro isolado do resolver. A causa imediata é esta cadeia:

```text
AND/OR seguido de bare word
  → a gramática escolhe combinableCondition/simpleCondition/conditionNameReference
  → AstBuilder cria DataReference cuja Meta preserva conditionNameReference
  → o collector transforma a grammarRule em CONDITION/{CONDITION}
  → o resolver filtra corretamente segundo admissibleKinds
  → DATA_ITEM homônimo existe, mas não é compatível
  → UNRESOLVED/INVALID_NAMESPACE_FOR_CONTEXT
```

Confiança na cadeia do caso inicial: **muito alta**. Ela foi observada em parse tree, AST, occurrence, symbol table e resolution para fixture mínima controlada.

O escopo observado é uma **classe limitada ao domínio de condições, com uma falha arquitetural na fronteira sintaxe → semântica → occurrence**. Não há evidência de defeito geral no algoritmo de candidate filtering do resolver. Há, porém, mais de uma lacuna sob essa fronteira:

1. bare tails de abbreviated relation são congelados como CONDITION antes de a declaração permitir distinguir condition-name de DATA/INDEX;
2. abbreviations com operador explícito e `relationCombinedComparison` são preservadas sem sujeito/operador herdados e sem admissibilidade DATA/INDEX completa;
3. `AND`/`OR` mistos são achatados em `MIXED_LOGICAL`, sem a precedência semântica;
4. `SEARCH WHEN` perde referências nominais que aparecem como condition-name ou bare tail;
5. condition-name subscriptado escolhe o identificador do subscript como base e perde o subscript na AST;
6. uma alternativa repetida aceita pela gramática pode ser truncada porque o lowering seleciona apenas `abbreviation(0)`.

Severidade: **alta para correção nominal e para a futura fronteira CFG/predicate analysis**. O comportamento cria falsos gaps para DATA, INDEX e RENAMES; também pode omitir occurrences. A incompletude de statements/expressions preservados ainda bloqueia a claim global de readiness, o que reduz o risco de uma falsa claim global, mas não torna os produtos nominais individualmente corretos.

Nenhuma correção deve ser reduzida a uma exceção em `grammarRule == "conditionNameReference"`. Essa exceção não resolveria semântica herdada, precedência, `SEARCH`, subscripts nem as formas já reconhecidas como `abbreviation`/`relationCombinedComparison`.

## Autoridade semântica COBOL

O dialeto de referência do projeto é IBM Enterprise COBOL. A documentação IBM estabelece que:

- após a primeira relation-condition, o sujeito ou o sujeito e o operador podem ser omitidos; o último sujeito e o último operador declarados são inseridos semanticamente ([IBM — Abbreviated combined relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=expressions-abbreviated-combined-relation-conditions));
- a herança termina, entre outros casos, quando uma simple condition ou condition-name é encontrada; portanto `C` em `A = B OR C` não pode ser decidido apenas pela grafia ou pelo ramo ANTLR;
- a herança também termina no `)` que corresponde a um `(` situado à esquerda do sujeito; assim, em `(A = B OR C) AND D`, C pode herdar `A =`, mas D já está fora dessa sequência abreviada;
- `NOT` imediatamente associado a um relational operator integra o operador; nas demais posições é negação lógica somente da relação imediatamente seguinte;
- `AND` possui precedência sobre `OR`, salvo alteração por parênteses ([IBM — Complex conditions](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=expressions-complex-conditions));
- operandos de general relation podem incluir identifier, literal, arithmetic expression e index-name ([IBM — General relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=expressions-general-relation-conditions));
- comparações com index-name são definidas contra data-name somente quando ele é numérico inteiro, além de literal numérico inteiro, outro index-name ou arithmetic expression ([IBM — Comparison of index-names and index data items](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=conditions-comparison-index-names-index-data-items));
- condition-name precisa ser único/qualificado e requer os mesmos subscripts da conditional variable quando aplicável ([IBM — Condition-name](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=reference-condition-name));
- em nested programs, a busca de declaração para no primeiro nível nominal com qualquer match, mesmo se o tipo for incompatível ([IBM — Scope of names](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=programs-scope-names)).

Premissas usadas:

| Premissa | Classe |
| --- | --- |
| sujeito e operador omitidos são herdados | `LANGUAGE_GUARANTEED` |
| condition-name encerra a inserção herdada | `LANGUAGE_GUARANTEED` |
| `)` correspondente a `(` à esquerda do sujeito encerra a inserção; distribuição após operador é caso distinto | `LANGUAGE_GUARANTEED` |
| index-name é operando relacional possível | `LANGUAGE_GUARANTEED` |
| parse tree não é verdade semântica | `ARCHITECTURE_GUARANTEED` |
| AST, occurrence e resolution são produtos separados | `ARCHITECTURE_GUARANTEED` |
| homônimo DATA + CONDITION no mesmo program tem uma precedência única neste contexto | `UNCERTAIN`; requer oracle IBM específico antes da implementação |
| frequência observada nos três dists representa outros sistemas COBOL | `OBSERVED_IN_CURRENT_CORPUS_ONLY`; não generalizar |

## Reprodução mínima completa

Fixture: `src/test/resources/cobol/resolution/abbreviated-condition-context.cbl`.

```cobol
01 A PIC X.
01 B PIC X.
01 C PIC X.
01 D PIC X.

IF A = B OR C OR D
    CONTINUE
END-IF
```

### Parse tree relevante

```text
condition
├─ combinableCondition
│  └─ simpleCondition
│     └─ relationCondition
│        └─ relationArithmeticComparison
│           ├─ arithmeticExpression → identifier → qualifiedDataName → dataName(A)
│           ├─ relationalOperator(=)
│           └─ arithmeticExpression → identifier → qualifiedDataName → dataName(B)
├─ andOrCondition(OR)
│  └─ combinableCondition → simpleCondition → conditionNameReference(C)
└─ andOrCondition(OR)
   └─ combinableCondition → simpleCondition → conditionNameReference(D)
```

Não há `AbbreviationContext` para `C` ou `D`. Como `conditionName` e `dataName` terminam em `cobolWord`, o ramo selecionado é uma categoria sintática, não prova de declaration kind.

### AST atual

```text
OperationExpression(category=OTHER, operator=OR, origin=condition)
├─ OperationExpression(category=RELATIONAL, operator="=",
│  origin=relationArithmeticComparison)
│  ├─ DataReference(A, origin=qualifiedDataName)
│  └─ DataReference(B, origin=qualifiedDataName)
├─ DataReference(C, origin=conditionNameReference)
└─ DataReference(D, origin=conditionNameReference)
```

O sujeito `A` e o operador `=` não são associados semanticamente a `C` ou `D`.

### Occurrences, symbols e resolução

| Nome | Parse/AST origin | AST node | `ReferenceKind` | `admissibleKinds` | Role | Symbol homônimo | Resultado |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A | `qualifiedDataName` sob relation | `DataReference` | INDEX (hint) | `{DATA, INDEX}` | VALUE_READ | DATA_ITEM | RESOLVED → DATA |
| B | `qualifiedDataName` sob relation | `DataReference` | INDEX (hint) | `{DATA, INDEX}` | VALUE_READ | DATA_ITEM | RESOLVED → DATA |
| C | `conditionNameReference` | `DataReference` | CONDITION | `{CONDITION}` | VALUE_READ | DATA_ITEM | UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT |
| D | `conditionNameReference` | `DataReference` | CONDITION | `{CONDITION}` | VALUE_READ | DATA_ITEM | UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT |

O resolver usa `admissibleKinds` em `compatibleCandidates`. Quando encontra nome igual e nenhum compatible candidate, produz `INVALID_NAMESPACE_FOR_CONTEXT`. Esse resultado é consistente com a occurrence recebida; a interpretação incorreta surgiu antes.

## Matriz adversarial de condições

Foram executadas 19 formas aceitas sem syntax error pela gramática configurada.

| Forma | Parse/lowering relevante | Resultado nominal atual |
| --- | --- | --- |
| `A = B` | `relationArithmeticComparison` → RELATIONAL | A/B `{DATA, INDEX}`, resolvidos DATA |
| `A = B OR C` | C → `conditionNameReference` | C `{CONDITION}`, INVALID_NAMESPACE |
| `A = B OR C OR D` | C/D → `conditionNameReference` | C/D `{CONDITION}`, INVALID_NAMESPACE |
| `A = B AND C` | C → `conditionNameReference` | C `{CONDITION}`, INVALID_NAMESPACE |
| `A = B OR A = C` | duas relations completas | todos `{DATA, INDEX}`, resolvidos |
| `A = B OR C = D` | segunda relation completa | todos `{DATA, INDEX}`, resolvidos |
| `A > B OR C` | C → `conditionNameReference` | C `{CONDITION}`, INVALID_NAMESPACE |
| `A NOT = B OR C` | C → `conditionNameReference` | C `{CONDITION}`, INVALID_NAMESPACE |
| `A = B OR NOT C` | `NOT(DataReference(C/conditionNameReference))` | C `{CONDITION}`, INVALID_NAMESPACE para DATA |
| `A = B AND C OR D` | `MIXED_LOGICAL` plano | C/D `{CONDITION}`, ambos inválidos para DATA |
| `A = B OR C AND D` | `MIXED_LOGICAL` plano | C/D `{CONDITION}`, ambos inválidos para DATA |
| `(A = B OR C) AND D` | GROUP + logical nodes | C `{CONDITION}` é falso gap para DATA; D `{CONDITION}` está fora da herança e DATA é semanticamente inválido nessa posição |
| `(A = B) OR C` | GROUP + OR | o `)` encerra a herança; C precisa iniciar simple condition válida, como condition-name, e DATA não é abreviação |
| `A = (B OR C)` | `PreservedExpression(relationCombinedComparison)` | A/B/C `{DATA}`; INDEX não é admissível |
| `A = (B AND C)` | igual ao anterior | A/B/C `{DATA}`; INDEX não é admissível |
| `A = B OR < C` | `PreservedExpression(abbreviation)` | C `{DATA}`, resolve DATA; INDEX seria rejeitado |
| `A = B OR NOT = C` | `PreservedExpression(abbreviation)` | C `{DATA}`, resolve DATA; operador herdado não materializado |
| `A = B OR < C > D` | parse tem dois `abbreviation`; AST conserva só o primeiro | C coletado; D desaparece |
| `A = B OR C = D OR E` | full relation redefine sujeito corrente; E vira `conditionNameReference` | E `{CONDITION}`, inválido para DATA |

Foram observadas 15 occurrences rigidamente classificadas como CONDITION e com `INVALID_NAMESPACE_FOR_CONTEXT` para declarações DATA nessa matriz. Após aplicar a regra IBM de término no parêntese correspondente, **13 são falsos gaps de abbreviated relation**. As outras duas — D em `(A = B OR C) AND D` e C em `(A = B) OR C` — estão depois do boundary de herança e rejeitam DATA corretamente; nessas posições um condition-name real continua válido.

O caso `A = B OR < C > D` é evidência de perda para input aceito pela gramática, não afirma que a sequência seja válida segundo IBM. A validade normativa dessa forma permanece aberta; como o frontend atual a aceita sem diagnostic, o lowering não deveria descartar silenciosamente o segundo filho.

## Matriz de declaration kinds

Com o mesmo bare tail após `A = B OR`, o resultado atual é:

| Declaração do tail | Origin | Occurrence | Resultado |
| --- | --- | --- | --- |
| DATA_ITEM | `conditionNameReference` | CONDITION / `{CONDITION}` | INVALID_NAMESPACE |
| CONDITION_NAME (88) | `conditionNameReference` | CONDITION / `{CONDITION}` | RESOLVED → CONDITION |
| INDEX_NAME, com A/B `PIC 9(4)` | `conditionNameReference` | CONDITION / `{CONDITION}` | INVALID_NAMESPACE apesar de `A = C` ser comparação válida de data-name numérico inteiro com index-name |
| RENAMES (66) | `conditionNameReference` | CONDITION / `{CONDITION}` | INVALID_NAMESPACE |
| inexistente | `conditionNameReference` | CONDITION / `{CONDITION}` | DECLARATION_NOT_FOUND |
| homônimo DATA + CONDITION | `conditionNameReference` | CONDITION / `{CONDITION}` | RESOLVED → CONDITION; DATA nem entra nos candidates |
| DATA qualificado `C OF G` | `conditionNameReference` | CONDITION / `{CONDITION}` | INVALID_NAMESPACE |
| CONDITION qualificado `C OF G` | `conditionNameReference` | CONDITION / `{CONDITION}` | RESOLVED → CONDITION |

O caso homônimo é um sentinel de false-success, mas não foi contado como defeito confirmado: a documentação IBM prova que encontrar um condition-name interrompe a herança; a regra exata quando DATA e CONDITION homônimos coexistem no mesmo nível precisa de oracle de compilador/documentação antes de escolher entre precedência, ambiguidade ou diagnóstico.

Em nested programs, uma DATA local `C` diante de uma CONDITION global externa faz a busca parar corretamente no nível local, mas a occurrence fechada em CONDITION produz `INVALID_NAMESPACE_FOR_CONTEXT`. Isso confirma que visibility/search não é a causa do caso; o resolver não pula indevidamente para a declaration externa.

COPY também não é causa: uma DATA declaration expandida de `SEMCOND.cpy` preserva provenance/include chain corretamente e falha da mesma forma no bare tail.

## Auditoria do `AstBuilder`

### Decisões contextuais existentes

Há bons precedentes no desenho atual:

- relation completa recebe `OperationCategory.RELATIONAL`;
- `SET condition-name TO TRUE/FALSE` recebe `StatementOperandContext.SET_CONDITION_TARGET`;
- outros formatos de SET recebem `SET_DATA_OR_INDEX`;
- selector nominal de EVALUATE sob subject `TRUE/FALSE` recebe `BOOLEAN_SUBJECT_NOMINAL`;
- selector nominal sob subject de valor recebe `VALUE_COMPARISON`.

Esses casos mostram que contexto semanticamente relevante já pode ser carregado pela AST sem usar spelling. Os testes existentes diferenciam os formatos.

### Lacunas comprovadas

1. `conditionExpression(condition)` escolhe, para cada tail, `combinableCondition` ou apenas `abbreviation(0)`. O primeiro caminho preserva a escolha sintática `conditionNameReference`; o segundo perde filhos adicionais.
2. `RelationCombinedComparisonContext` e `AbbreviationContext` caem no fallback `preservedExpression`; a grammar contém sujeito, operador, conectores e operands, mas a AST não materializa a relação herdada.
3. o builder reduz conectores diferentes a um único operador `MIXED_LOGICAL`, sem preservar a sequência AND/OR em estrutura. Apenas `writtenText` mantém a informação, e consumidores não podem reparseá-lo segundo INV-AST-002.
4. `dataReference(conditionNameReference)` procura primeiro qualquer descendant `QualifiedDataName`. Em `FLAG-ON(IDX)`, encontra o `qualifiedDataName` do subscript e cria `DataReference(baseName=IDX, writtenText=FLAG-ON(IDX), subscriptGroups=[])`.
5. `SEARCH` é `PreservedStatement`; o coletor genérico de statement operands reconhece `identifier`/`qualifiedDataName`, mas não materializa a `condition` de `searchWhen`. Em `WHEN A = B OR C` coleta A/B e perde C; em `WHEN FLAG-ON` perde FLAG-ON inteiro. Na fixture, FLAG/88 FLAG-ON é independente da tabela pesquisada e não exige subscript, isolando o finding de SEARCH do bug de condition-name subscriptado.

### Outros pontos auditados sem bug novo comprovado

- IF, EVALUATE subject e PERFORM UNTIL usam o mesmo lowering de `condition`: a fixture compartilhada produz três ocorrências C igualmente inválidas, confirmando propagação, não três causas.
- SET condition-name e EVALUATE boolean selector possuem overrides tipados e oracles existentes; não foi encontrada evidência reproduzível de erro nesses caminhos.
- subscript DATA/INDEX comum e relação completa possuem `{DATA, INDEX}` e testes existentes; o defeito adicional está no subscript específico de condition-name.
- qualification funciona para condition-name real; DATA qualificado em bare tail sofre a mesma classificação precoce.
- special registers, class conditions e reference modification foram inspecionados, mas esta sessão não encontrou contraexemplo reproduzível adicional.

## Auditoria do `ReferenceOccurrenceCollector`

Inventário de `admissibleKinds` atual:

| Contexto | Primary kind | `admissibleKinds` | Fonte da decisão |
| --- | --- | --- | --- |
| DataReference padrão | DATA ou CONDITION | singleton | `grammarRule == conditionNameReference` |
| relation operand tipado | INDEX (hint) | `{DATA, INDEX}` | `OperationCategory.RELATIONAL` |
| subscript DataReference | INDEX (hint) | `{DATA, INDEX}` | role SUBSCRIPT |
| `Ast.IndexReference` | INDEX | `{INDEX}` | tipo AST |
| SET condition target | CONDITION | `{CONDITION}` | `StatementOperandContext` |
| SET data/index | DATA (normalmente) | `{DATA, INDEX}` | `StatementOperandContext` |
| EVALUATE boolean selector | CONDITION | `{CONDITION}` | `EvaluateSelectorContext` |
| EVALUATE value selector | INDEX (hint) | `{DATA, INDEX}` | `EvaluateSelectorContext` |
| CALL BY REFERENCE DataReference | DATA | `{DATA, FILE}` | posição no CALL |
| qualifier DATA_OR_FILE | DATA | `{DATA, FILE}` | qualifier target |
| qualifier DATA ou FILE | DATA/FILE | singleton | qualifier target |
| procedure/file/program/preserved name | respectivo kind | singleton | tipo AST |

O único uso direto de `Meta.origin.grammarRule()` como decisão de occurrence kind está no fallback de DataReference. O `ReferenceResolutionManifest` repete o acoplamento ao declarar `conditionNameReference → CONDITION` e afirma que qualifiers/subscripts são estruturados, afirmação contrariada por `FLAG-ON(IDX)`.

Conclusão do inventário: `admissibleKinds` foi desenhado corretamente para conservar polimorfismo em subscript, relation, SET e EVALUATE, mas o bare tail fecha cedo demais. A solução futura deve tornar a ambiguidade contextual explícita; apenas trocar o primary kind não basta.

## Corpus impact

Foram regenerados os três dists em `/tmp` com a produção atual e minerados por `scripts/discovery/semantic-context-mining.mjs`.

| Fonte | Entries | RESOLVED | UNRESOLVED | AMBIGUOUS | UNSUPPORTED | EXTERNAL_OBSERVED | INVALID_NAMESPACE |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| COACTUPC | 3.097 | 2.959 | 135 | 2 | 0 | 1 | 0 |
| CBSTM03A | 580 | 471 | 0 | 0 | 95 | 14 | 0 |
| CBSTM03D | 582 | 487 | 0 | 0 | 95 | 0 | 0 |
| **Total** | **4.259** | **3.917** | **135** | **2** | **190** | **15** | **0** |

Resultados dos clusters:

- os 135 UNRESOLVED são `DECLARATION_NOT_FOUND`; nenhum possui declaration homônima na symbol table;
- maiores clusters aproximados: 57 `qualifiedDataName/DATA/{DATA}/VALUE_READ` próximos de MOVE e 54 `qualifiedDataName/INDEX/{DATA,INDEX}/VALUE_READ` próximos de SET;
- amostragem nominal mostra predominância de nomes CICS externos (`DFHRED` 41, `EIBAID` 28, `DFHRESP(...)` e argumentos); não há cluster de `conditionNameReference` inválido nesses três artefatos;
- 283 occurrences com grammar rule `conditionNameReference` existem: 277 em COACTUPC e 3 em cada CBSTM03*. Todas resolvem exclusivamente para CONDITION_NAME;
- há 18 `PreservedExpression(abbreviation)` reais: 9 em CBSTM03A e 9 em CBSTM03D, todos os objetos literais `'04'` em condições como `WS-M03B-RC = '00' OR '04'`;
- não há `relationCombinedComparison` nem `MIXED_LOGICAL` nos três dists atuais;
- o fonte WAUX/LTRANS reportado não está no repositório, portanto sua cardinalidade real não pôde ser incluída.

O localizador de statement do script é explicitamente aproximado e serve apenas para triagem; os findings semânticos deste relatório usam parse tree/AST reais, não essa heurística.

### Interpretação

O corpus versionado prova exposição real a abbreviated conditions, mas não contém o subtipo nominal DATA/INDEX após o conector que gera `INVALID_NAMESPACE`. Assim, a prevalência observada é baixa/zero neste corpus e desconhecida fora dele. Isso não reduz a confiança na causa do caso WAUX reproduzido; apenas limita a estimativa quantitativa.

## False-success risk

Resultado quantificado nos dists: **zero RESOLVED suspeitos por homônimo DATA/CONDITION/INDEX** segundo o minerador e **zero casos confirmados de binding para o kind semanticamente errado**.

Riscos que não devem ser confundidos com esse número:

1. o homônimo sintético DATA + CONDITION resolve CONDITION e exclui DATA antes da candidate list; é sentinel obrigatório, mas a regra IBM específica ainda precisa ser fechada;
2. operands DATA em `abbreviation` com operador explícito resolvem nominalmente, embora o predicate herdado não exista na AST. O binding pode estar correto e a semântica da condição continuar incompleta;
3. `SEARCH` pode omitir occurrences inteiras. Elas não aparecem como falso RESOLVED; simplesmente não entram no produto. A coverage `PRESERVED_UNINTERPRETED/DEPENDENCY_UNKNOWN` bloqueia readiness global, mas um consumidor que olhe só para resolution pode ignorar o uso;
4. as 18 abbreviations literais não possuem occurrence nominal no tail e por isso não entram na contagem de false success, embora continuem opacas para futuras análises.

## Findings classificados

| ID | Finding | Classe | Severidade | Confiança |
| --- | --- | --- | --- | --- |
| COND-001 | bare tail é parseado como `conditionNameReference` e congelado em CONDITION singleton | A, B, C, F, G | alta | muito alta |
| COND-002 | `abbreviation` e `relationCombinedComparison` preservam operands, mas não relação herdada nem DATA/INDEX completo | B, E, F, G | alta | muito alta |
| COND-003 | `MIXED_LOGICAL` achata AND/OR e não representa precedência | A, B, E, F, G | alta para predicate/CFG | alta |
| COND-004 | `abbreviation(0)` descarta filhos adicionais aceitos pela gramática | A, E, F | média; validade IBM do exemplo aberta | alta quanto à perda |
| COND-005 | SEARCH preservado perde bare tail e condition-name occurrences | B, E, F, G | alta | muito alta |
| COND-006 | condition-name subscriptado usa o subscript como base e perde a estrutura do subscript | B, C, E, F, G | alta | muito alta |
| COND-007 | manifest/evals tratam `conditionNameReference` como semanticamente fechado e afirmam subscript estruturado sem oracle adversarial | F, G | média/alta | alta |

Não foi classificado finding D para o caso principal. Diante da occurrence rígida recebida, candidate filtering, first nominal level, qualification e reason do resolver foram coerentes.

## Architectural assessment

### Parser / grammar

Deve enumerar estruturas sintáticas possíveis. A grammar atual não pode distinguir `dataName` de `conditionName` por token: ambos são `cobolWord`. Reordenar alternativas apenas trocaria qual caso legítimo quebra. A parse tree pode preservar uma escolha necessária ao reconhecimento sem que essa escolha seja meaning final.

### Semantic AST / lowering

Deve preservar:

- a sequência de relation conditions;
- sujeito e operador correntes/herdados;
- NOT como operador relacional versus lógico;
- conectores e precedência;
- parênteses/distribuição;
- provenance do que foi escrito versus herdado;
- uma forma contextual ainda não especializada quando a declaration kind é necessária.

O lowering atual faz isso para relation completa, SET e parte de EVALUATE, mas não para abbreviated/combined relations e SEARCH.

### Occurrence collection

Deve projetar o contexto semântico da AST em um conjunto de kinds admissíveis sem lookup. `kind` é hint; `admissibleKinds` é o contrato relevante. O collector atual já segue esse desenho em relation/subscript/SET/EVALUATE, mas o fallback por grammarRule viola a separação nos bare tails.

### Resolver

Deve selecionar declarações entre kinds semanticamente admissíveis, aplicar visibility/qualification e preservar ambiguidade. Não deveria reconstruir sujeito, operador, NOT ou precedência a partir de source text. Pode participar da especialização final quando apenas declaration kind distingue condition-name de abbreviated relation, mas isso exige um contrato explícito e não um patch pelo nome da grammar rule.

### Diagnóstico arquitetural

A responsabilidade está hoje dividida de forma insuficiente para construções cujo meaning depende do binding. A AST é construída antes da symbol table e não pode sozinha saber se o bare word é condition-name ou object herdado. A arquitetura precisa representar essa incerteza até o binding ou introduzir uma fase semanticamente nomeada depois dele. Hoje a incerteza é eliminada pelo parser e reaproveitada pelo collector como verdade.

## Modelagem conceitual da AST

Nenhuma opção é escolhida definitivamente neste Discovery.

### Opção A — preservar a abreviação

```text
ContextualConditionTail(
  writtenOperand = C,
  possibleMeaning = CONDITION_NAME_OR_ABBREVIATED_RELATION,
  inheritedSubject = A,
  inheritedOperator = "="
)
```

Vantagens:

- source fidelity e provenance naturais;
- não duplica a occurrence escrita de A;
- snapshots/debug mostram o que foi omitido;
- permite manter incerteza até o binding.

Custos:

- CFG, predicate analysis e constant propagation precisam interpretar a forma contextual ou consumir uma projeção normalizada;
- inherited subject/operator não podem ser filhos AST compartilhados sem violar o pre-order único;
- exige contrato explícito para links/descritores herdados e para a especialização após resolution.

### Opção B — normalizar durante lowering

```text
Relation(
  left = Inherited(A),
  operator = Inherited("="),
  right = C
)
```

Vantagens:

- consumers posteriores recebem predicates completos;
- facilita CFG, dataflow, predicate analysis e simplificação;
- equivalência semântica fica direta.

Custos:

- lowering pré-binding ainda não sabe se C é condition-name;
- clonar A pode criar IDs, provenance e occurrences sintéticas indevidas;
- compartilhar o node A viola INV-AST-003;
- snapshots precisam distinguir escrito de herdado;
- cardinalidades/IDs/pre-order mudam e exigem migração explícita.

### Opção C — representação dual/fase pós-binding

Preservar uma AST de superfície contextual e derivar um semantic predicate tree normalizado após nominal resolution.

Vantagens:

- separa source fidelity de forma pronta para análise;
- permite usar declaration kind para especializar CONDITION versus DATA/INDEX;
- evita reparse de texto e duplicação de source occurrences.

Custos:

- adiciona um produto/fase e joins explícitos;
- exige ADR/invariant para ownership, identidade, provenance e fail-closed;
- CFG futuro precisa escolher claramente qual produto consome.

## Candidate solution directions

1. **Ampliar apenas `admissibleKinds` no collector**: incisão pequena e útil como experimento, mas insuficiente como solução. Não representa herança, NOT, precedência, SEARCH ou subscripts e pode introduzir ambiguidade nova sem explicar meaning.
2. **Lowering semântico próprio para condition sequences**: adequado para full relations, abbreviations explícitas, distribuição, parênteses e precedência. Para bare tails deve preservar uma alternativa contextual até saber declaration kind.
3. **Especialização pós-binding**: richer occurrence admite CONDITION/DATA/INDEX; resolution escolhe declaration; uma fase semanticamente explícita produz predicate final. É a opção mais capaz de lidar com a dependência real entre meaning e declaration, mas tem maior impacto arquitetural.
4. **Alteração de grammar**: somente se for necessária para preservar alternativas/estrutura. Reordenar `combinableCondition | abbreviation+` não resolve o problema e quebraria condition-name real.
5. **Patch no resolver**: não recomendado como owner da semântica herdada. O resolver pode suportar admissible kinds mais ricos, mas não deve inferir predicates nem escanear parent/source text.

Nenhuma direção foi implementada.

## Test gaps fechados e ainda abertos

### Artefatos adicionados

- fixture mínima `abbreviated-condition-context.cbl`;
- copybook `SEMCOND.cpy` para provenance/COPY;
- `SemanticConditionContextDiscoveryTest` com caracterização verde da produção atual;
- oracles de requisito opt-in sob `-Dsemantic.condition.required=true`, deliberadamente vermelhos;
- minerador reproduzível `scripts/discovery/semantic-context-mining.mjs`.

### Classes cobertas

1. DATA em abbreviated relation;
2. CONDITION real no mesmo bare tail `A = B OR C` e depois de boundary parentético;
3. homônimo DATA/CONDITION;
4. INDEX em relation e abbreviated tail com sujeito/objeto DATA numérico inteiro compatível;
5. relation explícita versus abreviada;
6. múltiplas abbreviations consecutivas;
7. AND/OR mistos;
8. parênteses/distribuição, incluindo término da herança no `)` correspondente;
9. operador herdado, operador declarado e NOT;
10. nome ausente;
11. qualificação OF;
12. COPY-expanded declaration;
13. nested scope/shadowing;
14. IF/EVALUATE/PERFORM compartilhando lowering;
15. SEARCH preserved boundary;
16. condition-name subscriptado.

### Gaps restantes antes de implementar

- oracle IBM para homônimo DATA + CONDITION no mesmo program/nível;
- matriz normativa completa de parênteses/distributed operator, incluindo formas IBM extension;
- eventual compilação da fixture original WAUX quando o fonte/copybooks forem disponibilizados;
- decisão de identidade/provenance para nodes herdados ou produto pós-binding;
- oracle de precedência que compare semantic predicate tree, não apenas ausência de `MIXED_LOGICAL`;
- corpus adicional representativo de aplicações que usam bare data/index tails.

## Validação executada

- `mvn -q -Dtest=SemanticConditionContextDiscoveryTest test`: passou; sete testes de caracterização da produção atual;
- `mvn -q -Dtest=SemanticConditionContextDiscoveryTest -Dsemantic.condition.required=true test`: falhou como oracle futuro, em sete grupos sem erro de infraestrutura — DATA, INDEX, RENAMES, AND/OR misto, DATA herdado dentro de parênteses, qualificação e múltiplas abbreviations; condition-names reais no bare tail e após o boundary passam como controles negativos;
- `node --check scripts/discovery/semantic-context-mining.mjs`: passou;
- execução do minerador sobre COACTUPC, CBSTM03A e CBSTM03D: passou e reproduziu as 4.259 entries e os clusters registrados acima;
- `./scripts/harness/check-fast.sh`: passou;
- `./scripts/harness/check-semantic.sh`: passou;
- `./scripts/harness/check-performance.sh`: passou;
- `./scripts/harness/check-full.sh`: passou, incluindo regressão E2E e naming.

O teste opt-in permanece desabilitado por padrão para não tornar os gates normais vermelhos antes da implementação autorizada.

## Roteamento para trabalho futuro

As direções candidatas foram consolidadas em [BACKLOG-COND-001](../../work/backlog.md#backlog-cond-001--contextualizar-condições-combinadas-e-referências-nominais). O backlog preserva a ordem dos slices — contrato normativo, decisão arquitetural, lowering lossless, condition-name/subscripts, occurrences, SEARCH e regressão — sem criar work items ativos ou autorizar implementação.

Quando houver autorização, a promoção deve seguir `docs/engineering/work-item-protocol.md`, escolher apenas o primeiro slice revisável e declarar seus próprios `source_scope`, `must_not_change`, evals e gates. Este relatório permanece evidência histórica, não especificação normativa nem estado corrente da implementação.

## Critério de parada

Discovery concluído com causa reproduzida, extensão delimitada, findings adicionais comprovados, testes de caracterização e oracles futuros. Nenhum arquivo de produção ou grammar foi alterado. A implementação deve aguardar review humano e autorização explícita.
