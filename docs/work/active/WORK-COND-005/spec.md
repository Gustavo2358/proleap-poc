# Especificação — occurrences contextuais de condições

## Problema

`ReferenceOccurrenceCollector` ainda usa `DataReference.meta().origin().grammarRule()` como autoridade nominal: `conditionNameReference` vira `kind=CONDITION` e `admissibleKinds={CONDITION}`. Em `A = B OR C`, porém, a branch ANTLR apenas reconheceu a forma; a `ContextualConditionTail` da surface AST já registra que o significado de C depende do binding. Se C declara DATA, INDEX ou level-66 RENAMES, o pré-filtro atual produz `INVALID_NAMESPACE_FOR_CONTEXT` antes que o resolver possa selecionar a declaração admissível.

O coupling também aparece no `ReferenceResolutionManifest`, que publica `conditionNameReference → CONDITION` como `REFERENCE_ORIGIN`. Embora esse manifesto não governe o lookup, ele é uma policy nominal por nome de grammar rule e precisa deixar de prometer uma categoria única. O Discovery Round 1 não definiu o contrato substituto; o Round 2 fecha esse contrato como `CONTEXTUAL_REFERENCE_ORIGIN`.

O Review Humano do Discovery Round 1 acrescentou dois problemas que o Round 1 não modelava:

- a policy de uma occurrence não pode ser função apenas da posição AST; ela é função da posição semântica **e** da shape nominal escrita. Um nominal index-admissible e um nominal qualified/subscripted/reference-modified não possuem o mesmo universo de namespaces admissíveis: um index-name só participa de relation condition como operando nu; nunca como root qualified, subscripted ou reference-modified. Por isso a spec genérica `ContextualConditionTail → CONDITION / {DATA, INDEX, CONDITION}` e a policy preexistente `RelationCondition.object → INDEX / {DATA, INDEX}` são amplas demais para `C OF G`, `C(I)` e `C(1:2)`;
- diagnostics humanos hoje são produzidos a partir de `occurrence.kind()` e podem afirmar `UNRESOLVED CONDITION reference 'C'` para uma occurrence contextual cujo universo é `{DATA, INDEX, CONDITION}`. Isso precisa deixar de mentir sobre a classe semântica.

## Objetivo

Projetar a futura implementação para derivar `kind` e `admissibleKinds` da posição semântica na typed surface AST **e** da shape nominal escrita, preservando:

- uma occurrence por uso nominal escrito;
- AST, symbol tables, occurrences e resolution como produtos separados;
- `kind` como routing/hint primário de superfície, nunca como categoria final;
- `admissibleKinds` como universo nominal permitido antes do lookup;
- `Candidate.kind` / `selectedCandidate()` como categoria final quando `RESOLVED`;
- policies próprias para qualifiers e subscripts;
- resolver nominal-only e futura `ConditionSemantics` pós-binding;
- manifesto de resolução como classificação de coverage/origin, não como tabela de occurrence policy.

Conceitualmente a policy passa a ser `occurrencePolicy(position, nominalShape)`, onde `nominalShape` é derivada de `DataReference.qualifiers()`, `DataReference.subscriptGroups()` e `DataReference.referenceModification()` tipados — nunca de `grammarRule`, texto escrito, regex, token spelling ou parent grammar. Para o domínio de INDEX, a propriedade normativa não se chama apenas `bare`: usa-se `indexAdmissibleNominalShape(ref)`.

O checkpoint atual produz desenho, evidência e oracles. Nenhuma solução de produção é implementada.

## Domínio de entrada suportado

Entram no Slice 5 as condições já materializadas pelo Slice 3 e as referências lossless do Slice 4:

- `LogicalCondition`, `GroupedCondition`, `RelationCondition`, `NegatedCondition`, `ContextualConditionTail`, `DistributedOperandGroup` e `ClassCondition`;
- condições em IF, subjects condicionais de EVALUATE e controles UNTIL de PERFORM;
- contexts SET/EVALUATE já tipados apenas como regressões;
- root nominal simples, qualification `IN`/`OF`, subscripts e reference modification já estruturados;
- shapes do Round 2: bare, qualified, subscripted e qualified+subscripted para standalone, relation operand e contextual tail;
- reference modification como dimensão própria da shape nominal, inclusive em relation/distributed operands.

Não entra `SEARCH WHEN`: o statement ainda é preservado e perde parte da condition surface; materializá-lo é o Slice 6. Shapes aceitas pela grammar mas negativas segundo IBM continuam negativas/unsupported e não ampliam a policy.

## Classes semânticas

### Position × Nominal Shape Policy (matriz Round 3)

| Position | Shape | Primary | admissibleKinds |
| --- | --- | --- | --- |
| standalone | `C` | CONDITION | `{CONDITION}` |
| standalone | `C OF G` | CONDITION | `{CONDITION}` |
| standalone | `C(I)` | CONDITION | `{CONDITION}` |
| relation | `C` | INDEX | `{DATA, INDEX}` |
| relation | `C OF G` | DATA | `{DATA}` |
| relation | `C(I)` | DATA | `{DATA}` |
| relation | `C(1:2)` | DATA | `{DATA}` |
| contextual | `C` | CONDITION | `{DATA, INDEX, CONDITION}` |
| contextual | `C OF G` | CONDITION | `{DATA, CONDITION}` |
| contextual | `C(I)` | CONDITION | `{DATA, CONDITION}` |
| contextual | `C OF G(I)` | CONDITION | `{DATA, CONDITION}` |

As linhas foram confirmadas contra os contratos atuais de symbol/resolver e contra a autoridade IBM 6.4 registrada nas premissas: um index-name só comparece em relation condition como operando nu (identifier); qualifiers e subscripts anexam-se a data-name/condition-name. Se IBM 6.4 provar divergência em alguma linha, a correção exige citação normativa e explicação — nunca decisão intuitiva.

### Contextual Policy Composition

A policy contextual é a união de dois significados possíveis:

```text
contextual tail = possible abbreviated relation object  UNIÃO  possible standalone condition-name

contextualPolicy(shape) = relationOperandPolicy(shape)  UNIÃO  standaloneConditionPolicy(shape)
```

Não existe um conjunto `{DATA, INDEX, CONDITION}` fixo para todo tail. A composição aparece explicitamente aqui e nos helpers futuros:

- `indexAdmissibleNominalShape(ref)` se verifica quando `ref.qualifiers().isEmpty()`, `ref.subscriptGroups().isEmpty()` e `ref.referenceModification() == null`;
- `relationOperandPolicy(ref)` retorna `{DATA, INDEX}` somente quando `indexAdmissibleNominalShape(ref)` é verdadeiro; caso contrário retorna `{DATA}`;
- `standaloneConditionPolicy(ref) = {CONDITION}`; logo `contextualPolicy(ref) = relationOperandPolicy(ref) UNIÃO standaloneConditionPolicy(ref)`;
- portanto, para uma shape com reference modification, mesmo sem qualifier e sem subscript group, `contextualPolicy(ref) = {DATA, CONDITION}` e não inclui INDEX.

O qualifier `G` e o subscript `I` continuam occurrences independentes com policy própria; a composição aplica-se somente ao root nominal escrito.

### Relation Operand Shape Policy

A policy preexistente aproximada `INDEX / {DATA, INDEX}` para todo `RelationCondition.object` (e operands de `DistributedOperandGroup`) é shape-insensitive e ampla demais para roots qualified/subscripted/reference-modified. A regra fechada neste Round:

- `A = C` (index-admissible nominal shape) → `INDEX / {DATA, INDEX}`;
- `A = C OF G` (qualified) → `DATA / {DATA}`;
- `A = C(I)` (subscripted/table element) → `DATA / {DATA}`;
- `A = C(1:2)` (reference modification) → `DATA / {DATA}`;
- `A = C OF G(I)` → `DATA / {DATA}`.

RENAMES continua representado por DATA. INDEX nominalmente admissível (index-admissible nominal shape) não declara a relation válida: a compatibilidade concreta entre os operandos permanece em `ConditionValidation` (etapa posterior). A exclusão de INDEX em roots qualified, subscripted ou reference-modified é restrição da forma nominal admissível (namespace/shape), pertence ao Slice 5, e não é type validation.

### Contextual tail e children

`ContextualConditionTail` cria exatamente uma occurrence para `nominalReference`, com a policy contextual da shape do root. `DataQualifier` e subscripts continuam occurrences independentes: o override do root nunca é recursivamente aplicado aos children (`C OF G(I)` não torna G nem I contextuais).

### Typed contexts existentes (positions)

| Contexto AST | Significado pré-binding | Policy da occurrence raiz |
| --- | --- | --- |
| `DataReference` visitado como simple condition standalone | a única classe nominal COBOL que forma essa condição é condition-name | `CONDITION/{CONDITION}` |
| `NegatedCondition` / `GroupedCondition` / `LogicalCondition` | containers que preservam escopo lógico e encaminham o contexto aos fragments | policy do fragmento filho, sem lexical heuristic |
| `RelationCondition.subject/object` | operand de general relation | shape-sensitive: index-admissible `INDEX/{DATA, INDEX}`; qualified/subscripted/reference-modified `DATA/{DATA}` |
| `DistributedOperandGroup.operands` | objects do relational operator distribuído | mesma policy shape-sensitive do relation operand |
| `ContextualConditionTail.nominalReference` | condition-name standalone ou object abreviado, conforme binding | `CONDITION/{DATA, INDEX, CONDITION}` (index-admissible) ou `CONDITION/{DATA, CONDITION}` (qualified/subscripted/reference-modified) |
| `ClassCondition.subject` | identifier de class test, não condition-name | `DATA/{DATA}` pelo contexto do subject atual |
| qualifier de `DataReference` | componente de qualification, não value read | policy própria por `QualifierTarget`; `UNSPECIFIED → DATA/{DATA}` enquanto `BACKLOG-RES-004` estiver pendente |
| subscript de `DataReference` | expressão de subscript | `INDEX/{DATA, INDEX}` |
| `StatementOperandContext.SET_CONDITION_TARGET` | target de `SET ... TO TRUE/FALSE` | `CONDITION/{CONDITION}` |
| `EvaluateSelectorContext.BOOLEAN_SUBJECT_NOMINAL` | selector nominal contra subject TRUE/FALSE | `CONDITION/{CONDITION}` |
| `EvaluateSelectorContext.VALUE_COMPARISON` | selector de valor | `INDEX/{DATA, INDEX}` |

`PerformStatement.controlExpressions` não distingue ainda VALUE de CONDITION. A futura implementação deve substituir essa lista semanticamente não marcada por uma view tipada equivalente a `PerformControl(expression, context)` com pelo menos `VALUE` e `CONDITION`, sem tornar o wrapper um `Ast.Node` e sem consumir novo ID. O caso `UNTIL` fornece CONDITION; TIMES, VARYING, FROM e BY fornecem VALUE. `Ast.children` continua publicando apenas as expressions na mesma ordem. Quando `PERFORM UNTIL C` entrar em `visitConditionSurface`, o helper shape-sensitive de policy será reutilizado; PERFORM não cresce neste round.

### Context matrix final

| Source shape | Parse path relevante | AST position | Inheritance | Binding possibilities (Round 3) | Expected occurrence policy |
| --- | --- | --- | --- | --- | --- |
| `IF C` | `condition → combinableCondition → simpleCondition → conditionNameReference` | `IfStatement.condition = DataReference` | fechada | CONDITION | `CONDITION/{CONDITION}` |
| `IF NOT C` | mesmo path com `combinableCondition.NOT` | `NegatedCondition(DataReference)` | fechada | CONDITION | `CONDITION/{CONDITION}` |
| `A = C` | `relationArithmeticComparison` | `RelationCondition.object` | abre após a relation, mas C é object escrito | DATA, INDEX; RENAMES como DATA | `INDEX/{DATA, INDEX}` |
| `A = C OF G` | `relationArithmeticComparison` com object qualified | `RelationCondition.object = DataReference` qualificado | idem | DATA; RENAMES como DATA | `DATA/{DATA}` |
| `A = C(I)` | `relationArithmeticComparison` com object subscripted | `RelationCondition.object = DataReference` subscripted | idem | DATA; RENAMES como DATA | `DATA/{DATA}` |
| `A = C(1:2)` | `relationArithmeticComparison` com reference modifier | `RelationCondition.object = DataReference` reference-modified | idem | DATA; RENAMES como DATA | `DATA/{DATA}` |
| `A = B OR C` | tail reconhecido como `conditionNameReference` | `ContextualConditionTail` | aberta antes de C; binding decide se continua ou termina | DATA, INDEX, CONDITION; RENAMES como DATA | `CONDITION/{DATA, INDEX, CONDITION}` |
| `A = B AND C` | igual ao anterior, connector AND | `ContextualConditionTail` | aberta | DATA, INDEX, CONDITION | mesma policy; connector não decide namespace |
| `A = B OR C OF G` | tail qualified | root em `ContextualConditionTail`; G em `DataQualifier` | aberta | DATA, CONDITION; RENAMES como DATA | root `CONDITION/{DATA, CONDITION}`; G `QUALIFIER_COMPONENT` com policy própria |
| `A = B OR C(I)` | tail subscripted | root contextual; I em `SubscriptGroup` | aberta | root DATA, CONDITION; I DATA/INDEX | root `CONDITION/{DATA, CONDITION}`; I `SUBSCRIPT` `{DATA, INDEX}` |
| `A = B OR C OF G(I)` | tail qualified+subscripted | root C; qualifier G; subscript I | aberta | root DATA, CONDITION | root `CONDITION/{DATA, CONDITION}`; G e I independentes |
| `(A = B OR C)` | grupo contém a relation e o tail | tail dentro de `GroupedCondition` | aberta para C; fecha depois do `)` | DATA, INDEX, CONDITION | contextual para C |
| `(A = B) OR C` | grupo fecha antes do tail | segundo operand é `DataReference`, não contextual | fechada antes de C | CONDITION | standalone `CONDITION/{CONDITION}` |
| `A = (B OR C)` | `relationCombinedComparison` | B/C em `DistributedOperandGroup` | operator distribuído; estado permanece aberto após o grupo | DATA, INDEX; RENAMES como DATA | B/C shape-sensitive do relation operand |
| `A = B OR (C)` | grupo abre à direita do subject corrente | `GroupedCondition(ContextualConditionTail)` | permanece aberta | DATA, INDEX, CONDITION | contextual para C |
| `A = B OR NOT C` | logical NOT no combinable tail | `NegatedCondition(ContextualConditionTail)` | aberta; NOT nega apenas o fragmento | DATA, INDEX, CONDITION | contextual para C |
| `A NOT = C` | NOT integra `relationalOperator` | C é `RelationCondition.object` | relation escrita | DATA, INDEX; RENAMES como DATA | relation-operand policy shape-sensitive |
| `NOT A = B OR C` | NOT lógico sobre a primeira relation; tail abreviado em seguida | `LogicalCondition(NegatedCondition(RelationCondition(A,B)), ContextualConditionTail(C))` | aberta para C após a relation negada | DATA, INDEX, CONDITION | C permanece contextual; NOT da primeira relation não termina a abbreviation nem é herdado |
| `C IS NUMERIC` | `classCondition` | `ClassCondition.subject` | encerra abbreviation | DATA | `DATA/{DATA}` |
| `PERFORM UNTIL C` | `performUntil → condition → ... → conditionNameReference` | hoje `controlExpressions[0] = DataReference` sem tag | fechada | CONDITION | após tag CONDITION: `CONDITION/{CONDITION}` |

As formas distribuídas proibidas pela IBM — simple condition dentro da distribuição, outro relational operator no scope distribuído e logical NOT imediatamente após o `(` — são negativas. A grammar aceitar uma shape não a promove a input suportado.

### Surface shape, occurrence e binding

Os três níveis permanecem distintos:

1. `DataReference C` é a shape nominal lossless (base, qualifiers, subscriptGroups, referenceModification).
2. A posição tipada decide se a occurrence admite `{CONDITION}`, `{DATA, INDEX}`, `{DATA}` ou `{DATA, INDEX, CONDITION}` / `{DATA, CONDITION}` conforme a shape escrita.
3. O resolver escolhe uma declaração concreta e publica `Candidate.kind`, ou mantém `UNRESOLVED` / `AMBIGUOUS`.

O collector não consulta `SymbolTable`, não decide compatibilidade PIC/USAGE e não materializa `A = C` para um tail abreviado. A policy nunca usa `grammarRule`, `writtenText`, regex, token spelling ou parent grammar; usa `DataReference.qualifiers()`, `DataReference.subscriptGroups()` e `DataReference.referenceModification()`.

### Reference modification no contextual tail

A grammar de `conditionNameReference` é `conditionName (inData* inFile? conditionNameSubscriptReference* | inMnemonic*)`: ela não possui alternative `referenceModifier`. O root nominal de `ContextualConditionTail` é construído exclusivamente de `ConditionNameReferenceContext` com `referenceModification = null`.

Conclusão documentada:

```text
reference modification: not applicable to ContextualConditionTail root in current grammar
```

Não há policy a definir para reference modification no root contextual: a forma não é produzível. Subscripts de condition-name podem conter arithmetic expressions com reference-modified identifiers; esses são occurrences independentes com policy própria, não reference modification do root. O escopo não é ampliado.

Isto não elimina reference modification da definição geral de shape: ela é uma dimensão estrutural do `DataReference` e deve ser considerada pelo helper de admissibilidade de INDEX nos relation/distributed operands. No contextual tail atual, `conditionNameReference` não contém `referenceModifier`, portanto `referenceModification == null` por construção e nenhuma shape contextual adicional é inventada.

### Discovery Round 3 — reference modification shape closure

O caso `A = C(1:2)` fecha a definição que o Round 2 chamava vagamente de `bare`. A AST deve expor `baseName = C`, `qualifiers = []`, `subscriptGroups = []` e `referenceModification != null`. Essa shape não é index-admissible: a policy futura é `DATA/{DATA}` para o root de relation. O contextual tail atual não produz essa shape e permanece com `referenceModification == null` por construção.

O collector atual ainda observa `INDEX/{DATA, INDEX}` em `RelationCondition.object` e em operand de `DistributedOperandGroup`, porque sua policy preexistente ignora `referenceModification`; o teste registra isso como `PREEXISTING_RELATION_REFERENCE_MODIFICATION_OVERADMISSIBILITY`. A projeção what-if `DATA/{DATA}` resolve o `C` DATA com o resolver atual, demonstrando que a correção é de admissibility e não de algoritmo de resolução.

O contracaso `C(1:2)` com `C` somente index-name é um what-if nominal controlado, não uma fixture IBM-válida: a norma não admite reference-modified index-name root. Sob a policy antiga o candidate INDEX seria aceito; sob `{DATA}` ele é excluído. `C(I)` permanece distinto: `subscriptGroups()` não nulo e `referenceModification() == null` representam table element/subscript, enquanto `C(1:2)` tem o inverso.

`A = (C(1:2) OR D)` é materializado pela grammar como `DistributedOperandGroup`; o root `C(1:2)` recebe a mesma policy futura `DATA/{DATA}`. Não se cria shape contextual `C(1:2)`: `conditionNameReference` continua sem `referenceModifier` no root.

#### Round 3 challenge pass

- **Challenge R3-1 — false bare detection:** uma implementação de `qualifiers.empty && subscriptGroups.empty → INDEX` é rejeitada por `C(1:2)`, cujo `referenceModification != null` exclui INDEX.
- **Challenge R3-2 — parenthesis textual heuristic:** distinguir `C(I)` de `C(1:2)` por `writtenText` é rejeitado; `subscriptGroups()` e `referenceModification()` já são a autoridade typed AST.
- **Challenge R3-3 — duplicated shape logic:** relation e distributed visitors devem reutilizar a definição única `indexAdmissibleNominalShape(ref)`; policies divergentes são incorretas.

### RENAMES

RENAMES continua representado pelo namespace existente `DATA`; não se cria `ReferenceKind.RENAMES`. Bare contextual `{DATA, INDEX, CONDITION}` inclui RENAMES via DATA; qualified/subscripted contextual `{DATA, CONDITION}` também o inclui via DATA. Para RENAMES subscriptado não se inventa validade: nenhum oracle obrigatório usa essa forma enquanto a validade IBM da combinação específica não estiver fechada.

## Premissas

### Normativas

- `LANGUAGE_GUARANTEED`: uma condition-name é uma simple condition; a forma `IF C` usa a condition-name condition. Um DATA/INDEX/RENAMES nu não forma uma simple condition por si. Fonte: [IBM Condition-name condition](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=expressions-condition-name-condition) e [IBM Conditional expressions](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=structure-conditional-expressions).
- `LANGUAGE_GUARANTEED`: relations consecutivas podem omitir subject ou subject/operator; os últimos escritos são inseridos. Inserção termina diante de outra simple condition, condition-name ou do `)` que corresponde a `(` à esquerda do subject. Fonte: [IBM Abbreviated combined relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=expressions-abbreviated-combined-relation-conditions).
- `LANGUAGE_GUARANTEED`: AND e OR admitem abbreviation; a policy não pode depender do connector. NOT junto do relational operator integra o operator; nos demais pontos é logical NOT local.
- `LANGUAGE_GUARANTEED`: general relation operands podem ser identifiers, literals, arithmetic expressions ou index-names. Compatibilidade de INDEX é type-sensitive e posterior ao binding. Fontes: [IBM General relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=expressions-general-relation-conditions) e [IBM Comparison of index-names and index data items](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=conditions-comparison-index-names-index-data-items).
- `LANGUAGE_GUARANTEED` (fechada no Round 2): um index-name não é um data-name e não ocupa hierarquia de dados: em uma expression/reference, a única shape escrita de um index-name como operand de relation é o identifier nu (também pode aparecer como subscript de table element e em controles próprios de índice). Index-name nunca é root qualified (`C OF G`), root subscripted (`C(I)`) ou root reference-modified (`C(1:2)`); essas shapes pertencem a data-name/condition-name ou a function-identifier quando reference modification é permitida. Como subscript (`TABLE-ELEMENT(I)`), o index-name participa sem tornar o root INDEX. Logo, para roots qualified, subscripted ou reference-modified, INDEX não é namespace admissível em Enterprise COBOL 6.4. Fonte: [IBM General relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=expressions-general-relation-conditions), [IBM Reference modification](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-modification), [IBM References to DATA DIVISION names](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-references-data-division-names) e [IBM Comparison of index-names and index data items](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=conditions-comparison-index-names-index-data-items).
- `LANGUAGE_GUARANTEED` (Round 3): reference modification é aplicável a `data-name-1` e, quando permitido, a `function-name-1`; cria um data item derivado da referência original. Isso não transforma um `index-name` em data-name nem cria uma forma válida de index-name root. A distinção `C(I)` versus `C(1:2)` é tipada: `subscriptGroups()` representa table element/subscript; `referenceModification()` representa reference modification. Fonte: [IBM Reference modification](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-modification), [IBM General relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=expressions-general-relation-conditions) e [IBM References to DATA DIVISION names](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-references-data-division-names).
- `LANGUAGE_GUARANTEED`: condition-name usa qualification/subscripts necessários da conditional variable; essas shapes (qualified/subscripted) são formas legítimas de condition-name. Fonte: [IBM Condition-name](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-condition-name).
- `LANGUAGE_GUARANTEED`: level-66 RENAMES declara um data-name, não namespace próprio. Fonte: [IBM RENAMES clause](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-renames-clause).
- `LANGUAGE_GUARANTEED` (fechada no Round 2): `NOT A = B OR C` interpreta-se conceitualmente `(NOT (A = B)) OR (A = C)` quando C é relation object abreviado: o NOT lógico da primeira relation não transforma C em standalone nem é herdado por C. Fonte: seções **NOT** do contrato de domínio e [IBM Abbreviated combined relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=expressions-abbreviated-combined-relation-conditions).

### Arquiteturais

- `ARCHITECTURE_GUARANTEED`: ADR-0012 e INV-COND-001/002 mantêm a AST pre-binding e `ConditionSemantics` posterior à resolution.
- `ARCHITECTURE_GUARANTEED`: uma occurrence escrita não é duplicada por candidate kind; `ReferenceOccurrences` valida unicidade por `referenceAstNodeId`.
- `ARCHITECTURE_GUARANTEED`: `kind` deve pertencer a `admissibleKinds`; o construtor de `Occurrence` valida isso.
- `ARCHITECTURE_GUARANTEED` (Round 2): ADR-0009 exige classificação explícita e versionada de cada grammar rule; INV-COV-002 exige que toda regra do frontend versionado possua classificação. A reclassificação de `conditionNameReference` no `ReferenceResolutionManifest` é parte dessa decisão de coverage e sobe o `VERSION` do manifesto.
- `ARCHITECTURE_GUARANTEED` (Round 2): `ReferenceResolutionManifest` responde "qual o papel de coverage desta grammar rule?"; `ReferenceOccurrenceCollector` responde "qual policy nominal este uso possui nesta posição da AST?". O manifesto NÃO vira tabela de occurrence policy e NÃO determina `{DATA, INDEX, CONDITION}` por si.
- `ARCHITECTURE_GUARANTEED` (Round 2): o resolver candidate algorithm (selection, scope, qualification, ambiguity, dispatch, compatibleCandidates, local/global visibility, selectedCandidate) permanece inalterado. Adaptação em resolver files, se necessária, é somente wording de diagnóstico baseado no occurrence contract.
- `OBSERVED_AND_TESTED`: DATA, INDEX, CONDITION e RENAMES resolvem com o resolver atual quando uma única occurrence CONDITION-hint recebe `{DATA, INDEX, CONDITION}`; ausência permanece `DECLARATION_NOT_FOUND`.
- `OBSERVED_AND_TESTED`: CICS não lê `kind` nem `admissibleKinds`; usa shape AST, coerência da occurrence e status unresolved.
- `OBSERVED_AND_TESTED` (Round 2, FACT): com a mesma AST, re-projetar a occurrence root de `C OF G` / `C(I)` para `{DATA, CONDITION}` faz o resolver atual rejeitar um candidate INDEX (único candidato do spelling); `{DATA, INDEX}` / `{DATA, INDEX, CONDITION}` selecionaria INDEX quando o candidate existe. Isso prova no modelo que a admissibility shape-sensitive é o único gate e que o resolver não precisa mudar.

### Alternativas avaliadas

| Critério | A — context enum global | B — helpers com AST atual | C — anotar todo DataReference | D — helpers + PERFORM tipado + shape-sensitive |
| --- | ---: | ---: | ---: | ---: |
| usa somente typed AST | sim | não para `PERFORM UNTIL C` | sim | sim |
| elimina grammarRule semantic coupling | sim | não integralmente | sim | sim |
| representa contextual admissibility por shape | não | parcial | sim | sim |
| preserva uma occurrence por nome escrito | sim | sim | sim | sim |
| mantém resolver nominal-only | sim | sim | sim | sim |
| mantém AST pre-binding | sim | sim | sim | sim |
| blast radius | médio | baixo, mas incompleto | alto | baixo/médio |
| facilidade de testar | alta | alta | média | alta |
| risco de context leakage | médio/alto | baixo | médio | baixo |
| compatibilidade com Slice 6 | média | média | média | alta: helper de condition surface reutilizável |
| compatibilidade com future ConditionSemantics | alta | alta | alta | alta |
| necessidade de abstraction nova | enum traversal transversal | nenhuma, mas falta informação | field/enum em toda referência | wrapper/tag não-node só para controls PERFORM |

Decisão: alternativa D (reconfirmada no Round 2 e fechada quanto a reference modification no Round 3). Reutilizar `visitRelationalOperand` e acrescentar helper específico `visitConditionSurface`/equivalente com policy shape-sensitive. O helper despacha pelos containers AST tipados e usa `qualifiers()` / `subscriptGroups()` / `referenceModification()`; não recebe source text, grammar parent ou symbol table. `ContextualConditionTail` recebe `contextualKinds(ref)`; direct `DataReference` alcançado pelo slot condition recebe standalone; relation/distributed operands recebem `relationOperandKinds(ref)`. Para PERFORM, uma pequena abstraction tipada distingue controles VALUE e CONDITION.

Rejeições:

- A propaga um enum por caminhos que os nodes já distinguem e aumenta o risco de um contexto vazar para qualifiers/subscripts.
- B é suficiente para IF e nodes de condição, mas não para `PERFORM UNTIL C`; aceitar B exigiria conservar grammarRule ou interpretar `writtenControl`.
- C duplica posição semântica em toda `DataReference`, amplia constructors e permite divergência entre annotation e container.
- Um novo `ReferenceKind.CONTEXTUAL` não é necessário: o contrato existente representa incerteza em `admissibleKinds`; o enum novo expandiria switches/resolvers/relatórios sem criar informação de binding. Contextualidade é apresentada por label derivado (ver contrato de diagnóstico).
- (Round 2) Manter `ContextualConditionTail → {DATA, INDEX, CONDITION}` fixo para toda shape: viola a restrição de index-name qualified/subscripted e é morto pelos NEG oracles.
- (Round 3) Definir `bare` apenas por `qualifiers` e `subscriptGroups`: `C(1:2)` mata essa implementação, pois `referenceModification != null` exclui INDEX.

## Comportamento esperado

### Contrato do primary kind (fechado no Round 2)

`kind` é **routing / primary surface hint**, nunca `resolved nominal category`. Ele serve a três usos: despacho entre famílias de resolver (DATA/INDEX/CONDITION caem no mesmo caminho), telemetria sintática e wording diagnóstico. A categoria final é `selectedCandidate().kind()`.

Para `ContextualConditionTail`, o primary kind permanece `CONDITION`, agora derivado do container tipado e da shape, e não da grammar rule. Isso preserva o hint de superfície e a compatibilidade dos consumers; todos os resolvers agrupam DATA/CONDITION/INDEX no mesmo caminho. A incerteza real fica em `admissibleKinds`. `kind` continua obrigatoriamente membro de `admissibleKinds`.

- Standalone condition-name: `CONDITION`.
- Relation/distributed operand: shape-sensitive — `INDEX` somente em `indexAdmissibleNominalShape`; qualified/subscripted/reference-modified `DATA`.
- Contextual tail, qualquer shape suportada: `CONDITION` (porque CONDITION pertence à policy standalone que compõe com `{DATA, INDEX}` ou `{DATA}` conforme `indexAdmissibleNominalShape`).

Afirmar `kind = CONDITION` não afirma que o nome é um condition-name; `RESOLVED` usa `selectedCandidate().kind()`.

### Decisão sobre `admissibleKinds`

- standalone simple condition nominal: `{CONDITION}`;
- relation/distributed operand: `indexAdmissibleNominalShape` `{DATA, INDEX}`; qualified/subscripted/reference-modified `{DATA}`;
- contextual tail com inheritance aberta: index-admissible `{DATA, INDEX, CONDITION}`; qualified/subscripted/reference-modified `{DATA, CONDITION}`;
- RENAMES entra por `DATA`, pois `compatible(symbol, DATA)` já admite `SymbolKind.RENAMES`;
- qualifier e subscript não herdam o conjunto do root;
- SET/EVALUATE mantêm suas policies existentes.

### Helper de policy esperado (não implementado)

Funções puras sobre `DataReference` tipado:

```text
relationOperandKinds(ref):
    if indexAdmissibleNominalShape(ref)               -> {DATA, INDEX}
    otherwise                                         -> {DATA}

indexAdmissibleNominalShape(ref):
    ref.qualifiers().isEmpty()
    AND ref.subscriptGroups().isEmpty()
    AND ref.referenceModification() == null

contextualKinds(ref) = relationOperandKinds(ref) UNIÃO {CONDITION}
```

Isso reduz duplicação e evita divergência entre `RelationCondition`, `DistributedOperandGroup` e `ContextualConditionTail`. A implementação futura deve introduzir primeiro esse helper estrutural e só então aplicá-lo às policies do collector; não deve espalhar testes separados para qualifier, subscript e reference modification por múltiplos visitors. No contextual tail atual, a terceira condição é sempre verdadeira por construção da grammar (`referenceModification == null`).

### Cardinalidade e children

Uma `ContextualConditionTail` cria exatamente uma occurrence para `nominalReference`. Não cria occurrences para o wrapper, subject/operator omitidos ou para cada kind admissível. `DataQualifier` e expressions de `SubscriptGroup` continuam occurrences distintas porque são nomes efetivamente escritos.

Em `C OF G(I)`:

- C recebe `CONDITION/{DATA, CONDITION}` (contextual);
- G permanece `QUALIFIER_COMPONENT`, hoje `DATA/{DATA}` para `UNSPECIFIED`;
- I permanece `SUBSCRIPT`, `INDEX/{DATA, INDEX}`;
- o override do root nunca é recursivamente aplicado aos children.

### ReferenceResolutionManifest contract (fechado no Round 2)

O primeiro Discovery afirmou que `conditionNameReference` deixaria de ser `REFERENCE_ORIGIN/CONDITION`, mas não definiu o substituto. Round 2 define:

- Nova classificação conceitual `CONTEXTUAL_REFERENCE_ORIGIN` em `ReferenceResolutionManifest.RuleClass` (nome pode variar por convenção; o significado não):
  > A grammar rule materializa uma referência nominal escrita, mas não possui um único ReferenceKind semanticamente válido independentemente da typed AST position.
- `conditionNameReference` é o primeiro caso: `rule = conditionNameReference`, `ruleClass = CONTEXTUAL_REFERENCE_ORIGIN`, `referenceKind = null`. Isso NÃO significa "não é referência" nem "é apenas container"; significa "é origem estrutural de referência + nominal kind decidido pela typed AST occurrence context".
- `REFERENCE_CONTAINER` não serve: `conditionNameReference` não é um container com referências apenas nos filhos; ela materializa o root nominal escrito. O manifesto precisa distinguir container de contextual reference origin.
- Invariantes propostos para o construtor do manifesto:
  - `REFERENCE_ORIGIN → referenceKind obrigatório`;
  - `QUALIFIER_COMPONENT → referenceKind obrigatório`;
  - `DECLARATION_RELATION → referenceKind obrigatório`;
  - `CONTEXTUAL_REFERENCE_ORIGIN → referenceKind deve ser null`; se `!= null`, fail fast (evita regressão a `conditionNameReference → CONDITION`).
- `qualifiedDataName` continua `REFERENCE_ORIGIN/DATA` enquanto aplicável; nem toda referência vira contextual.
- O manifesto permanece `exhaustive`, `versioned`, `closed` (INV-COV-002 e ADR-0009).
- O manifesto NÃO carrega occurrence admissibility: `conditionNameReference → CONTEXTUAL_REFERENCE_ORIGIN` não determina `{DATA, INDEX, CONDITION}`; typed AST position + nominal shape determinam. É proibido resolver F2 colocando `conditionNameReference → {DATA, INDEX, CONDITION}` no manifesto.
- A futura implementação sobe `ReferenceResolutionManifest.VERSION` porque o significado público/versionado da classificação muda. Registrado no plan/eval; não implementado agora.
- ADR-0009 é leitura obrigatória do work item e entra em related_decisions; INV-COV-002 entra em related_invariants: o manifesto é parte explícita da decisão de coverage versionada.

### Human diagnostic contract (fechado no Round 2)

Hoje existem mensagens equivalentes a `UNRESOLVED CONDITION reference 'C'` produzidas a partir de `occurrence.kind()`. Isso é aceitável para `IF C` onde `{CONDITION}` é singleton; não é aceitável para `A = B OR C` contextual.

Regra de presentation label:

```text
se admissibleKinds contém CONDITION E admissibleKinds.size() > 1:
    apresentar humanamente como CONTEXTUAL_CONDITION
    (ou wording equivalente: "contextual condition reference")
senão, para occurrence CONDITION:
    apresentar como CONDITION reference (standalone continua correto)
```

Não se cria novo `ReferenceKind`; é um display/diagnostic label derivado do semantic occurrence product (`kind` + `admissibleKinds`). A única adaptação futura permitida em resolver files é diagnostic wording baseado no occurrence contract — nenhuma mudança de resolution policy.

### NOT + Abbreviation (fechado no Round 2)

`NOT A = B OR C` é o oracle obrigatório do NOT lógico sobre a primeira relation em sequência abreviável. A regra IBM registrada: o `NOT` da primeira relation não transforma C em standalone nem é herdado por C. Interpretação conceitual quando C é relation object abreviado:

```text
(NOT (A = B)) OR (A = C)
```

A AST real observada preserva essa estrutura de forma equivalente: `LogicalCondition(NegatedCondition(RelationCondition(A,B)), ContextualConditionTail(C))` (validade confirmada por FACT). Aplicar NOT também à relation herdada de C (`NOT(A = C)`) é rejeitado (R2-9). Os três casos permanecem estruturalmente distintos:

- `NOT C` → `NegatedCondition(DataReference)` — standalone `{CONDITION}`;
- `NOT A = B OR C` → logical NOT sobre a primeira relation + `ContextualConditionTail(C)` — C usa `contextualPolicy(shape(C))`, bare `{DATA, INDEX, CONDITION}`;
- `A NOT = C` → `RelationCondition` com NOT integrado ao relational operator — C é relation operand shape-sensitive.

### Consumer impact (Round 2)

| Consumer | Impacto esperado | Functional semantics? | Presentation only? | No change? |
| --- | --- | --- | --- | --- |
| `ReferenceOccurrenceCollector` | occurrence policy funcional shape-sensitive | sim | — | — |
| `ReferenceOccurrences` | contract/Javadoc/helper se necessário | sim (contrato) | — | — |
| `DataAndIndexReferenceResolver` | no resolution policy; diagnostic wording only se necessário | — | sim | — |
| `CobolReferenceResolver` | no resolution policy; diagnostic wording only se necessário | — | sim | — |
| `ResolutionAnalysisReport` | contextual gap wording; semantic counts inalterados | — | sim | — |
| `ResolutionSnapshot` | expõe kind + admissibleKinds; nenhuma ambiguidade semântica escondida | — | sim | — |
| `ReferenceResolutionManifest` | coverage contract change + version bump | sim (coverage) | — | — |
| `CicsIntrinsicClassifier` | no change | — | — | sim |
| SET/EVALUATE | no behavior change | — | — | sim |
| PERFORM | typed control metadata | sim (metadata) | — | — |
| SEARCH | no change | — | — | sim |

`ResolutionSnapshot` pode continuar expondo `kind` + `admissibleKinds` (ambos explicitamente nomeados); não há necessidade de esconder `kind`.

### Resolver impact

Nenhuma alteração de algoritmo de resolver é necessária. A evidência FACT reconstrói em teste occurrences com as policies shape-sensitive e as entrega ao resolver atual: DATA/RENAMES selecionam DATA, INDEX seleciona INDEX quando admissível, condition-name seleciona CONDITION, nome ausente permanece `UNRESOLVED/DECLARATION_NOT_FOUND`, candidate INDEX fora de `{DATA, CONDITION}` é excluído.

O resolver já filtra por `admissibleKinds`, mapeia RENAMES sob DATA, preserva múltiplos candidates e publica selected candidate kind. Ele não reconstrói predicates.

`BACKLOG-RES-004` continua uma limitação independente para precedência local/GLOBAL após qualification e para ampliar qualifier `UNSPECIFIED` a DATA/FILE. O Slice 5 não muda esse mapping nem promete resolver colisões qualificadas já registradas.

### Métricas

`ResolutionAnalysisReport.syntacticKindCounts` pode continuar contando `kind = CONDITION` porque o campo é explicitamente `syntactic`. `resolvedSemanticKindCounts` continua usando `selectedCandidate().kind()`. Registrar como invariant/eval; não renomear métricas neste slice.

### Superfície provável de produção

- `ReferenceOccurrenceCollector.java`: remover a branch por `grammarRule`; introduzir traversal helper por condition surface com policy `relationOperandKinds(ref)` / `contextualKinds(ref)`; aplicar policies root sem contaminar children.
- `Ast.java` e `AstBuilder.java`: representar controls PERFORM como expression + contexto VALUE/CONDITION, sem node/ID novo e construído dos contexts `performTimes`, `performUntil` e `performVarying`.
- `ReferenceOccurrences.java`: esclarecer que o primary contextual é hint de superfície e que `admissibleKinds` governa o universo pré-binding.
- `ReferenceResolutionManifest.java`: reclassificar `conditionNameReference` como `CONTEXTUAL_REFERENCE_ORIGIN` com `referenceKind == null` e subir `VERSION`; manter `qualifiedDataName → REFERENCE_ORIGIN/DATA`.
- Resolver/report files (somente se necessário): `DataAndIndexReferenceResolver.java`, `CobolReferenceResolver.java`, `ResolutionAnalysisReport.java` — adaptação de wording para contextual occurrences; nenhuma mudança de resolução/algorithm.

`ResolutionContracts`, symbol tables, resolution product, grammar e CICS permanecem must-not-change.

### Lifecycle do gap preexistente de relation operands (decisão Round 2)

O Discovery confirmou que relation operands qualified/subscripted recebem hoje `{DATA, INDEX}` (collector shape-insensitive). Decisão registrada: **corrigir no próprio Slice 5** porque:

- usa exatamente o mesmo helper `relationOperandPolicy(shape)` / `relationOperandKinds(ref)` exigido pelo contextual tail;
- não exige mudança de resolver (candidate filtering existente honra `admissibleKinds`);
- é consequência direta da policy shape-sensitive necessária ao contextual tail;
- cabe nos mesmos oracles e source scope.

Não se separa dependency nem se cria heurística para evitar a decisão. Marcador de caracterização: `PREEXISTING_RELATION_OCCURRENCE_OVERADMISSIBILITY` registrado nos testes FACT e fechado pelo helper shape-sensitive no Checkpoint 2.

## Comportamento diante de incerteza

- Nome ausente: uma occurrence contextual, zero candidates, `UNRESOLVED/DECLARATION_NOT_FOUND`; apresentação contextual, não `CONDITION` exclusiva.
- Mesmo nome com candidates COBOL realmente válidos múltiplos: uma occurrence, todos os candidates, `AMBIGUOUS`, nenhum selected candidate.
- DATA+CONDITION homônimos no mesmo programa: source IBM inválido; não criar regra de precedência para fazê-lo passar.
- Compatibilidade PIC/USAGE de INDEX: binding pode selecionar INDEX (index-admissible nominal shape), mas validade da relation permanece para futura `ConditionValidation`; INDEX fora da shape admissível (qualified/subscripted/reference-modified root) não entra nem como candidate.
- Shape aceita pela grammar sem autoridade IBM: preservar/unsupported; não ampliar admissibility intuitivamente.
- Contexto PERFORM não tipado: futura implementação deve primeiro preservar a tag; se isso não couber no scope revisável, parar como `ARCHITECTURAL_DECISION_REQUIRED`, nunca retornar ao grammarRule.
- Reference modification no root contextual: não produzível pela grammar (documentado acima); subscripts com reference-modified identifiers são occurrences independentes.
- RENAMES subscriptado: sem oracle obrigatório enquanto a validade IBM da forma não estiver fechada.

## Fora de escopo

- `ConditionSemantics` e `ConditionValidation`;
- CFG, dataflow, reaching definitions, constant/possibility propagation e predicate normalization;
- materialização de subject/operator herdados ou occurrences sintéticas;
- SEARCH WHEN (Slice 6);
- `BACKLOG-RES-004` e IBM resolution-of-names step 3;
- type checking de relation operands;
- mudanças em grammar, symbol model, candidate model ou em qualquer algoritmo de resolver;
- novo `ReferenceKind.CONTEXTUAL` ou `ReferenceKind.RENAMES`;
- manifesto como tabela de occurrence policy;
- redesign de SET/EVALUATE, CICS ou snapshots fora das migrações necessárias;
- implementação de produção neste checkpoint (zero `src/main/`).

## Regras de domínio relacionadas

- `docs/domain/conditional-expressions.md`
- `docs/domain/semantic-ast.md`
- `docs/domain/reference-resolution.md`
- `docs/domain/symbol-model.md`
- `docs/domain/provenance.md`
- `docs/evals/conditional-expression-oracles.md`

## ADRs/invariantes relacionados

ADR-0009 e ADR-0012; INV-AST-001, INV-AST-002, INV-AST-003, INV-SYM-001, INV-COND-001, INV-COND-002, INV-COV-002, INV-PROV-002, INV-RES-001, INV-DET-001 e INV-PERF-001.
