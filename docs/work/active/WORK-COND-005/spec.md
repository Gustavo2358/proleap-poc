# Especificação — occurrences contextuais de condições

## Problema

`ReferenceOccurrenceCollector` ainda usa `DataReference.meta().origin().grammarRule()` como autoridade nominal: `conditionNameReference` vira `kind=CONDITION` e `admissibleKinds={CONDITION}`. Em `A = B OR C`, porém, a branch ANTLR apenas reconheceu a forma; a `ContextualConditionTail` da surface AST já registra que o significado de C depende do binding. Se C declara DATA, INDEX ou level-66 RENAMES, o pré-filtro atual produz `INVALID_NAMESPACE_FOR_CONTEXT` antes que o resolver possa selecionar a declaração admissível.

O coupling também aparece no `ReferenceResolutionManifest`, que publica `conditionNameReference → CONDITION` como `REFERENCE_ORIGIN`. Embora esse manifesto não governe o lookup, ele é uma policy nominal por nome de grammar rule e precisa deixar de prometer uma categoria única.

O Discovery encontrou ainda uma lacuna de representação fora do caso central: `PerformStatement.controlExpressions` mistura controles de valor (`N TIMES`, VARYING/FROM/BY) e `UNTIL condition` numa `List<Expression>` sem tag. `PERFORM N TIMES` e `PERFORM UNTIL C` expõem um `DataReference` no mesmo índice e só diferem hoje por `grammarRule`. Portanto remover integralmente o coupling exige uma marcação tipada e não-node para os controles de PERFORM; inferir por `writtenControl`, token order ou origin seria a mesma fragilidade sob outro nome.

## Objetivo

Projetar a futura implementação para derivar `kind` e `admissibleKinds` exclusivamente da posição semântica na typed surface AST, preservando:

- uma occurrence por uso nominal escrito;
- AST, symbol tables, occurrences e resolution como produtos separados;
- `kind` como hint primário de superfície, nunca como binding final;
- `admissibleKinds` como universo nominal permitido antes do lookup;
- `Candidate.kind`/`selectedCandidate()` como categoria final quando `RESOLVED`;
- policies próprias para qualifiers e subscripts;
- resolver nominal-only e futura `ConditionSemantics` pós-binding.

O checkpoint atual produz desenho, evidência e oracles. Nenhuma solução de produção é implementada.

## Domínio de entrada suportado

Entram no Slice 5 as condições já materializadas pelo Slice 3 e as referências lossless do Slice 4:

- `LogicalCondition`, `GroupedCondition`, `RelationCondition`, `NegatedCondition`, `ContextualConditionTail`, `DistributedOperandGroup` e `ClassCondition`;
- condições em IF, subjects condicionais de EVALUATE e controles UNTIL de PERFORM;
- contexts SET/EVALUATE já tipados apenas como regressões;
- root nominal simples, qualification `IN`/`OF`, subscripts e reference modification já estruturados.

Não entra `SEARCH WHEN`: o statement ainda é preservado e perde parte da condition surface; materializá-lo é o Slice 6. Shapes aceitas pela grammar mas negativas segundo IBM continuam negativas/unsupported e não ampliam a policy.

## Classes semânticas

### Typed contexts existentes

| Contexto AST | Significado pré-binding | Policy da occurrence raiz |
| --- | --- | --- |
| `DataReference` visitado como simple condition standalone | a única classe nominal COBOL que forma essa condição é condition-name | `CONDITION/{CONDITION}` |
| `NegatedCondition`/`GroupedCondition`/`LogicalCondition` | containers que preservam escopo lógico e encaminham o contexto aos fragments | policy do fragmento filho, sem lexical heuristic |
| `RelationCondition.subject/object` | operand de general relation | `INDEX/{DATA, INDEX}` (contrato preexistente de hint) |
| `DistributedOperandGroup.operands` | objects do relational operator distribuído | `INDEX/{DATA, INDEX}` |
| `ContextualConditionTail.nominalReference` | condition-name standalone ou object abreviado, conforme binding | `CONDITION/{DATA, INDEX, CONDITION}` |
| `ClassCondition.subject` | identifier de class test, não condition-name | `DATA/{DATA}` pelo contexto do subject atual |
| qualifier de `DataReference` | componente de qualification, não value read | policy própria por `QualifierTarget`; `UNSPECIFIED → DATA/{DATA}` enquanto `BACKLOG-RES-004` estiver pendente |
| subscript de `DataReference` | expressão de subscript | `INDEX/{DATA, INDEX}` |
| `StatementOperandContext.SET_CONDITION_TARGET` | target de `SET ... TO TRUE/FALSE` | `CONDITION/{CONDITION}` |
| `EvaluateSelectorContext.BOOLEAN_SUBJECT_NOMINAL` | selector nominal contra subject TRUE/FALSE | `CONDITION/{CONDITION}` |
| `EvaluateSelectorContext.VALUE_COMPARISON` | selector de valor | `INDEX/{DATA, INDEX}` |

`PerformStatement.controlExpressions` não distingue ainda VALUE de CONDITION. A futura implementação deve substituir essa lista semanticamente não marcada por uma view tipada equivalente a `PerformControl(expression, context)` com pelo menos `VALUE` e `CONDITION`, sem tornar o wrapper um `Ast.Node` e sem consumir novo ID. O caso `UNTIL` fornece CONDITION; TIMES, VARYING, FROM e BY fornecem VALUE. `Ast.children` continua publicando apenas as expressions na mesma ordem.

### Context matrix final

| Source shape | Parse path relevante | AST position | Inheritance | Binding possibilities | Expected occurrence policy |
| --- | --- | --- | --- | --- | --- |
| `IF C` | `condition → combinableCondition → simpleCondition → conditionNameReference` | `IfStatement.condition = DataReference` | fechada | CONDITION | `CONDITION/{CONDITION}` |
| `IF NOT C` | mesmo path com `combinableCondition.NOT` | `NegatedCondition(DataReference)` | fechada | CONDITION | `CONDITION/{CONDITION}` |
| `A = C` | `relationArithmeticComparison` | `RelationCondition.object` | abre após a relation, mas C é object escrito | DATA, INDEX; RENAMES como DATA | `INDEX/{DATA, INDEX}` |
| `A = B OR C` | tail reconhecido como `conditionNameReference` | `ContextualConditionTail` | aberta antes de C; binding decide se continua ou termina | DATA, INDEX, CONDITION; RENAMES como DATA | `CONDITION/{DATA, INDEX, CONDITION}` |
| `A = B AND C` | igual ao anterior, connector AND | `ContextualConditionTail` | aberta | DATA, INDEX, CONDITION; RENAMES como DATA | mesma policy; connector não decide namespace |
| `(A = B OR C)` | grupo contém a relation e o tail | tail dentro de `GroupedCondition` | aberta para C; fecha depois do `)` | DATA, INDEX, CONDITION | contextual para C |
| `(A = B) OR C` | grupo fecha antes do tail | segundo operand é `DataReference`, não contextual | fechada antes de C | CONDITION | standalone `CONDITION/{CONDITION}` |
| `A = (B OR C)` | `relationCombinedComparison` | B/C em `DistributedOperandGroup` | operator distribuído; estado permanece aberto após o grupo | DATA, INDEX; RENAMES como DATA | B/C `INDEX/{DATA, INDEX}` |
| `A = B OR (C)` | grupo abre à direita do subject corrente | `GroupedCondition(ContextualConditionTail)` | permanece aberta | DATA, INDEX, CONDITION | contextual para C |
| `A = B OR NOT C` | logical NOT no combinable tail | `NegatedCondition(ContextualConditionTail)` | aberta; NOT nega apenas o fragmento | DATA, INDEX, CONDITION | contextual para C |
| `A NOT = C` | NOT integra `relationalOperator` | C é `RelationCondition.object` | relation escrita | DATA, INDEX; RENAMES como DATA | relation-operand policy |
| `A = B OR FLAG OF GROUP` | tail qualificado | root em `ContextualConditionTail`; GROUP em `DataQualifier` | aberta | root DATA/INDEX/CONDITION | root contextual; GROUP `QUALIFIER_COMPONENT` com policy própria |
| `A = B OR FLAG(I)` | tail subscriptado | root contextual; I em `SubscriptGroup` | aberta | root DATA/INDEX/CONDITION | root contextual; I `SUBSCRIPT` com `{DATA, INDEX}` |
| `C IS NUMERIC` | `classCondition` | `ClassCondition.subject` | encerra abbreviation | DATA | `DATA/{DATA}` |
| `PERFORM UNTIL C` | `performUntil → condition → ... → conditionNameReference` | hoje `controlExpressions[0] = DataReference` sem tag | fechada | CONDITION | após tag CONDITION: `CONDITION/{CONDITION}` |

As formas distribuídas proibidas pela IBM — simple condition dentro da distribuição, outro relational operator no scope distribuído e logical NOT imediatamente após o `(` — são negativas. A grammar aceitar uma shape não a promove a input suportado.

### Surface shape, occurrence e binding

Os três níveis permanecem distintos:

1. `DataReference C` é a shape nominal lossless.
2. A posição tipada decide se a occurrence admite `{CONDITION}`, `{DATA, INDEX}` ou `{DATA, INDEX, CONDITION}`.
3. O resolver escolhe uma declaração concreta e publica `Candidate.kind`, ou mantém `UNRESOLVED`/`AMBIGUOUS`.

O collector não consulta `SymbolTable`, não decide compatibilidade PIC/USAGE e não materializa `A = C` para um tail abreviado.

## Premissas

### Normativas

- `LANGUAGE_GUARANTEED`: uma condition-name é uma simple condition; a forma `IF C` usa a condition-name condition. Um DATA/INDEX/RENAMES nu não forma uma simple condition por si. Fonte: [IBM Condition-name condition](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=expressions-condition-name-condition) e [IBM Conditional expressions](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=structure-conditional-expressions).
- `LANGUAGE_GUARANTEED`: relations consecutivas podem omitir subject ou subject/operator; os últimos escritos são inseridos. Inserção termina diante de outra simple condition, condition-name ou do `)` que corresponde a `(` à esquerda do subject. Fonte: [IBM Abbreviated combined relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=expressions-abbreviated-combined-relation-conditions).
- `LANGUAGE_GUARANTEED`: AND e OR admitem abbreviation; a policy não pode depender do connector. NOT junto do relational operator integra o operator; nos demais pontos é logical NOT local.
- `LANGUAGE_GUARANTEED`: general relation operands podem ser identifiers, literals, arithmetic expressions ou index-names. Compatibilidade de INDEX é type-sensitive e posterior ao binding. Fontes: [IBM General relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=expressions-general-relation-conditions) e [IBM Comparison of index-names and index data items](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=conditions-comparison-index-names-index-data-items).
- `LANGUAGE_GUARANTEED`: level-66 RENAMES declara um data-name, não namespace próprio. Fonte: [IBM RENAMES clause](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-renames-clause).
- `LANGUAGE_GUARANTEED`: condition-name usa qualification/subscripts necessários da conditional variable. Fonte: [IBM Condition-name](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-condition-name).

### Arquiteturais

- `ARCHITECTURE_GUARANTEED`: ADR-0012 e INV-COND-001/002 mantêm a AST pre-binding e `ConditionSemantics` posterior à resolution.
- `ARCHITECTURE_GUARANTEED`: uma occurrence escrita não é duplicada por candidate kind; `ReferenceOccurrences` valida unicidade por `referenceAstNodeId`.
- `ARCHITECTURE_GUARANTEED`: `kind` deve pertencer a `admissibleKinds`; o construtor de `Occurrence` valida isso.
- `OBSERVED_AND_TESTED`: DATA, INDEX, CONDITION e RENAMES resolvem com o resolver atual quando uma única occurrence CONDITION-hint recebe `{DATA, INDEX, CONDITION}`; ausência permanece `DECLARATION_NOT_FOUND`.
- `OBSERVED_AND_TESTED`: CICS não lê `kind` nem `admissibleKinds`; usa shape AST, coerência da occurrence e status unresolved.

### Alternativas avaliadas

| Critério | A — context enum global | B — helpers com AST atual | C — anotar todo DataReference | D — helpers + PERFORM tipado |
| --- | ---: | ---: | ---: | ---: |
| usa somente typed AST | sim | não para `PERFORM UNTIL C` | sim | sim |
| elimina grammarRule semantic coupling | sim | não integralmente | sim | sim |
| preserva uma occurrence por nome escrito | sim | sim | sim | sim |
| representa contextual admissibility | sim | sim | sim | sim |
| mantém resolver nominal-only | sim | sim | sim | sim |
| mantém AST pre-binding | sim | sim | sim | sim |
| blast radius | médio | baixo, mas incompleto | alto | baixo/médio |
| facilidade de testar | alta | alta | média | alta |
| risco de context leakage | médio/alto | baixo | médio | baixo |
| compatibilidade com Slice 6 | média | média | média | alta: helper de condition surface reutilizável |
| compatibilidade com future ConditionSemantics | alta | alta | alta | alta |
| necessidade de abstraction nova | enum traversal transversal | nenhuma, mas falta informação | field/enum em toda referência | wrapper/tag não-node só para controls PERFORM |

Decisão: alternativa D. Reutilizar `visitRelationalOperand` e acrescentar helper específico `visitConditionSurface`/equivalente. O helper despacha pelos containers AST tipados; não recebe source text, grammar parent ou symbol table. `ContextualConditionTail` recebe a policy contextual; direct `DataReference` alcançado pelo slot condition recebe standalone. Para PERFORM, uma pequena abstraction tipada distingue controles VALUE e CONDITION porque esses são dois domínios reais já presentes na grammar e consumidos diferentemente pelo collector.

Rejeições:

- A propaga um enum por caminhos que os nodes já distinguem e aumenta o risco de um contexto vazar para qualifiers/subscripts.
- B é suficiente para IF e nodes de condição, mas não para `PERFORM UNTIL C`; aceitar B exigiria conservar grammarRule ou interpretar `writtenControl`.
- C duplica posição semântica em toda `DataReference`, amplia constructors e permite divergência entre annotation e container.
- Um novo `ReferenceKind.CONTEXTUAL` não é necessário: o contrato existente representa incerteza em `admissibleKinds`; o enum novo expandiria switches/resolvers/relatórios sem criar informação de binding.

## Comportamento esperado

### Decisão sobre `kind`

`kind` não é authoritative para DATA/INDEX/CONDITION. Ele serve a três usos: despacho entre famílias de resolver, telemetria/snapshot como `syntacticKinds` e wording diagnóstico. A categoria final é `selectedCandidate().kind()`.

Para `ContextualConditionTail`, o primary kind permanece `CONDITION`, agora derivado do container tipado e não da grammar rule. Isso preserva o hint de superfície e a compatibilidade dos consumers; todos os resolvers agrupam DATA/CONDITION/INDEX no mesmo caminho. A incerteza real fica em `{DATA, INDEX, CONDITION}`. `kind` continua obrigatoriamente membro de `admissibleKinds`.

Standalone condition-name continua `CONDITION`; relation/distributed operands preservam o primary `INDEX` já usado para `{DATA, INDEX}`. Nenhum consumer pode tratar o primary como binding final.

### Decisão sobre `admissibleKinds`

- standalone simple condition nominal: `{CONDITION}`;
- relation operand e distributed operand: `{DATA, INDEX}`;
- contextual tail com inheritance aberta: `{DATA, INDEX, CONDITION}`;
- RENAMES entra por `DATA`, pois `compatible(symbol, DATA)` já admite `SymbolKind.RENAMES`;
- qualifier e subscript não herdam o conjunto do root;
- SET/EVALUATE mantêm suas policies existentes.

### Cardinalidade e children

Uma `ContextualConditionTail` cria exatamente uma occurrence para `nominalReference`. Não cria occurrences para o wrapper, subject/operator omitidos ou para cada kind admissível. `DataQualifier` e expressions de `SubscriptGroup` continuam occurrences distintas porque são nomes efetivamente escritos.

Em `FLAG OF GROUP(I)`:

- FLAG recebe `CONDITION/{DATA, INDEX, CONDITION}`;
- GROUP permanece `QUALIFIER_COMPONENT`, hoje `DATA/{DATA}` para `UNSPECIFIED`;
- I permanece `SUBSCRIPT`, `INDEX/{DATA, INDEX}`;
- o override do root nunca é recursivamente aplicado aos children.

### Consumer impact

| Consumer | Usa `kind`? | Usa `admissibleKinds`? | Pode mudar? | Risco/decisão |
| --- | ---: | ---: | ---: | --- |
| `DataAndIndexReferenceResolver` | sim, só dispatch DATA/CONDITION/INDEX | sim, candidate filtering | não | alto; teste what-if prova que o código atual resolve DATA/INDEX/CONDITION/RENAMES e preserva missing |
| `CobolReferenceResolver` | sim, mesmo dispatch agrupado | sim, apenas alternativa FILE | não | baixo; contextual set não inclui FILE |
| diagnostics dos resolvers | sim, label/hint | não | outcomes mudam; wording fica estável | médio; não confundir label primário com selected kind |
| `ResolutionSnapshot` | sim | sim | sim, só `admissibleKinds` e resultados dos tails | esperado e revisável; IDs/cardinalidade permanecem |
| `ResolutionAnalysisReport` | sim em `syntacticKinds`/gaps | indiretamente via resolution | sim nos status e `resolvedSemanticKinds` | primary CONDITION estável; falsas lacunas desaparecem |
| `ReferenceResolutionManifest` | publica kind por grammar rule | não | sim | remover `conditionNameReference → CONDITION`; classificá-la sem kind nominal e subir versão |
| `CicsIntrinsicClassifier` | não | não | não | usa shape, identity/coherence e status; regressão obrigatória |
| coverage | manifesto de resolution | não | somente a classificação acima | não mudar grammar/semantic coverage |
| facts/reporting/logging | sim como hint | candidate final para semântica | deltas bounded | nunca usar primary como resultado final |
| testes/helpers | vários asserts | vários asserts | sim | migrar characterization do falso gap; preservar SET/EVALUATE/pre-order/CICS |

### Resolver impact

Nenhuma alteração de resolver é necessária para a policy raiz. A evidência FACT reconstrói em teste uma única occurrence com primary CONDITION e `{DATA, INDEX, CONDITION}` e a entrega ao resolver atual: DATA e RENAMES selecionam candidate DATA, INDEX seleciona INDEX, condition-name seleciona CONDITION e nome ausente permanece `UNRESOLVED/DECLARATION_NOT_FOUND`.

O resolver já filtra por `admissibleKinds`, mapeia RENAMES sob DATA, preserva múltiplos candidates e publica selected candidate kind. Ele não reconstrói predicates.

`BACKLOG-RES-004` continua uma limitação independente para precedência local/GLOBAL após qualification e para ampliar qualifier `UNSPECIFIED` a DATA/FILE. O Slice 5 não muda esse mapping nem promete resolver colisões qualificadas já registradas.

### Superfície provável de produção

- `ReferenceOccurrenceCollector.java`: remover a branch por `grammarRule`; introduzir traversal helper por condition surface; aplicar policies root sem contaminar children.
- `Ast.java` e `AstBuilder.java`: representar controls PERFORM como expression + contexto VALUE/CONDITION, sem node/ID novo e construído dos contexts `performTimes`, `performUntil` e `performVarying`.
- `ReferenceOccurrences.java`: esclarecer que o primary contextual é hint de superfície e que `admissibleKinds` governa o universo pré-binding.
- `ReferenceResolutionManifest.java`: retirar kind autoritativo da rule `conditionNameReference` e versionar a policy.

`ResolutionContracts`, symbol tables, resolvers, resolution product, grammar e CICS permanecem must-not-change.

## Comportamento diante de incerteza

- Nome ausente: uma occurrence contextual, zero candidates, `UNRESOLVED/DECLARATION_NOT_FOUND`.
- Mesmo nome com candidates COBOL realmente válidos múltiplos: uma occurrence, todos os candidates, `AMBIGUOUS`, nenhum selected candidate.
- DATA+CONDITION homônimos no mesmo programa: source IBM inválido; não criar regra de precedência para fazê-lo passar.
- Compatibilidade PIC/USAGE de INDEX: binding pode selecionar INDEX, mas validade da relation permanece para futura `ConditionValidation`.
- Shape aceita pela grammar sem autoridade IBM: preservar/unsupported; não ampliar admissibility intuitivamente.
- Contexto PERFORM não tipado: futura implementação deve primeiro preservar a tag; se isso não couber no scope revisável, parar como `ARCHITECTURAL_DECISION_REQUIRED`, nunca retornar ao grammarRule.

## Fora de escopo

- `ConditionSemantics` e `ConditionValidation`;
- CFG, dataflow, reaching definitions, constant/possibility propagation e predicate normalization;
- materialização de subject/operator herdados ou occurrences sintéticas;
- SEARCH WHEN (Slice 6);
- `BACKLOG-RES-004` e IBM resolution-of-names step 3;
- type checking de relation operands;
- mudanças em grammar, symbol model, candidate model ou resolver;
- redesign de SET/EVALUATE, CICS ou snapshots fora das migrações necessárias.

## Regras de domínio relacionadas

- `docs/domain/conditional-expressions.md`
- `docs/domain/semantic-ast.md`
- `docs/domain/reference-resolution.md`
- `docs/domain/symbol-model.md`
- `docs/domain/provenance.md`
- `docs/evals/conditional-expression-oracles.md`

## ADRs/invariantes relacionados

ADR-0012; INV-AST-001, INV-AST-002, INV-AST-003, INV-SYM-001, INV-COND-001, INV-COND-002, INV-PROV-002, INV-RES-001, INV-DET-001 e INV-PERF-001.
