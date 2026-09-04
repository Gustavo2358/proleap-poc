# WORK-COND-006 — Discovery de `SEARCH WHEN`

## Problema

`SEARCH` é reconhecido pela grammar, mas ainda atravessa o lowering como `Ast.PreservedStatement`. O caminho genérico materializa alguns operands nominais e cada `searchWhen` como `StatementClause`; não materializa o `condition` filho. Assim, a parse tree contém usos nominais da condition que não alcançam `Ast.children`, `ReferenceOccurrenceCollector`, `ReferenceOccurrences`, `ReferenceResolution` ou diagnostics.

O problema é específico da fronteira AST. O Slice 5 já fechou a policy contextual shape-sensitive para uma condition surface recebida pelo collector e provou que o resolver nominal funciona quando a occurrence correta existe.

## Objetivo

Caracterizar o caminho atual completo, confirmar a semântica IBM Enterprise COBOL 6.4 relevante, separar SEARCH serial de SEARCH ALL e fechar o menor contrato de implementação capaz de reutilizar a condition surface e a policy de occurrences existentes. Este checkpoint não implementa o contrato.

## Domínio de entrada suportado

SEARCH serial com uma ou mais `WHEN`, conditions simples, relações completas/abreviadas, logical `NOT`, qualification e subscripts aceitos pela grammar configurada, além de `AT END`, statements de branch, `VARYING` e terminador explícito. A caracterização inclui a variante `SEARCH ALL` para demonstrar sua shape compartilhada e suas restrições normativas distintas.

Parser errors, preprocessing incompleto e formas que a grammar aceita mas IBM rejeita não viram claims de validade. A compatibilidade type-sensitive de relações continua responsabilidade conceitual futura de `ConditionValidation`.

## Classes semânticas

- `searched table`: referência do `qualifiedDataName` após `SEARCH`; é um operand de tabela, não uma condition.
- `VARYING/index`: referência do `searchVarying`; mantém policy própria de DATA/INDEX.
- `relation operands`: subjects/objects de uma relation completa, com policy relacional do Slice 5.
- `condition-name`: standalone `level 88`, qualification e subscript preservados como nominal shape; em branch contextual, a classe final depende do binding.
- `condition surface`: relations, `LogicalCondition`, `NegatedCondition`, `ContextualConditionTail`, `GroupedCondition` e `DistributedOperandGroup` quando produzidos pela lowering existente.
- `branch statements` e `AT END`: fluxo estrutural separado da condition, com ownership preservado.

## Premissas

1. A grammar configurada é a autoridade sintática local; IBM Enterprise COBOL for z/OS 6.4 é a autoridade normativa COBOL.
2. `searchStatement` é a regra do statement; `searchWhen` contém `WHEN condition (NEXT SENTENCE | statement*)`.
3. `SEARCH ALL` usa a mesma regra `searchWhen` na grammar local, mas não é semanticamente equivalente a SEARCH serial.
4. `grammarRule` é provenance/coverage, não autoridade de namespace ou `ReferenceKind`.
5. Uma ocorrência futura por condition deve apontar para o mesmo AST node nominal escrito; não se coletará novamente a parse tree.

## Comportamento esperado

O futuro lowering deve materializar a boundary tipada de cada branch e declarar explicitamente a posição semântica da condition ao collector. `Ast.children` garante reachability estrutural, IDs, scope, provenance e pre-order; por si só não escolhe a policy `CONDITION`. A recomendação mínima é:

```text
SearchStatement (Statement)
  all: boolean
  searchedReference: DataReference
  varying: DataReference?
  atEnd: StatementClause?
  whens: List<SearchWhen>

SearchWhen (Node)
  condition: Expression
  statements: List<Statement>
```

`SearchWhen` permanece `Ast.Node` porque representa uma branch escrita com identidade estrutural própria, provenance, ownership entre condition e statements e posição determinística na AST; `EvaluateBranch` é o precedente existente. A decisão não depende de IDs dos children: condition e statements já têm seus próprios IDs. `SearchStatement` precisa ser `Ast.Statement` para substituir o preserved container sem introduzir uma segunda representação paralela. `SearchWhen` recebe ID próprio por ser Node; `all` não recebe ID. `AT END` pode reutilizar `StatementClause` existente (`atEndPhrase`), `VARYING` é uma referência estrutural opcional e `SEARCH ALL` cabe no mesmo shape com `all=true`, mantendo a distinção para validação futura.

`Ast.children(SearchStatement)` deve seguir ordem escrita: `searchedReference`, `varying` se presente, `atEnd` se presente e `whens`; `Ast.children(SearchWhen)` deve seguir `condition` e depois statements. O contador de IDs deve então continuar pre-order, sem IDs para metadata de controle, sem clones de relations e sem children compartilhados.

Na condition, o futuro builder deve chamar o lowering existente (`buildConditionSurface`/equivalente `buildCondition`) no `SearchWhenContext.condition()`. O routing futuro deve ser explícito e tipado:

```text
SearchStatement
  searchedReference → search-specific/value role
  varying          → SEARCH_VARYING policy
  atEnd            → normal statement traversal
  each SearchWhen
    condition      → typed CONDITION position
                    → ReferenceOccurrenceCollector.visitConditionSurface(condition)
    statements     → normal statement traversal
```

Assim, a mesma condition surface reutiliza `RelationCondition`, `ContextualConditionTail`, `LogicalCondition`, `NegatedCondition`, `DistributedOperandGroup` e as policies `relationOperandKinds`, `contextualKinds`, standalone `CONDITION`, qualification e subscript do Slice 5. Nenhuma nova condition policy de SEARCH deve ser adicionada ao collector e nenhuma regra deve ser adicionada ao resolver.

### SEARCH VARYING

IBM permite que o `VARYING` seja um `index-name` ou um identificador que seja item índice ou item elementar inteiro. Na grammar local ambos chegam como `searchVarying : VARYING qualifiedDataName`, portanto o contrato nominal futuro é uma posição distinta, sem criar `ReferenceKind` novo:

```text
SEARCH_VARYING
  role            = CONTEXT_DEPENDENT
  primary kind    = DATA
  admissibleKinds = {DATA, INDEX}
```

Esta é a policy de namespace, não uma policy de condition. O binding seleciona `INDEX` para `SEARCH-IDX` declarado por `INDEXED BY` e `DATA` para um item elementar inteiro válido, mantendo `searchedReference`, `varying`, condition, qualifiers e subscripts em posições semânticas separadas. A forma qualificada/subscriptada deve aplicar a shape admissibility já existente; não se deve transformar qualquer identifier dentro de SEARCH em `CONDITION`.

### NEXT SENTENCE

`searchWhen` não tem `nextSentenceStatement` na grammar: sua alternativa é o token literal `NEXT SENTENCE`, fora de `statement()`. O AST futuro deve reutilizar `Ast.NextSentenceStatement` como a única ação da branch quando essa alternativa estiver escrita:

```text
SearchWhen
  condition
  statements = [Ast.NextSentenceStatement]
```

Essa escolha é lossless para a alternativa estrutural, preserva a convenção existente de AST/IDs e mantém `Ast.children` e futuros consumers estruturais uniformes. O builder deve reconhecer explicitamente os tokens `NEXT`/`SENTENCE` ao materializar a branch; depender apenas de `searchWhen.statement()` perderia a ação. Sem antecipar semântica de CFG, o nó deve conservar a provenance/span do caminho escrito.

## Comportamento diante de incerteza

SEARCH ALL não deve ser promovido a semanticamente válido apenas porque a grammar aceitou a condition. O bit `all` deve permanecer observável e as restrições IBM (key, ordem dos preceding keys, igualdade, AND-only e compatibilidade) devem ser validadas em etapa própria quando autorizada. A implementação de materialization pode preservar a surface de uma forma sintaticamente reconhecida sem afirmar validade SEARCH ALL.

Se uma forma de WHEN não puder ser baixada pelo lowering tipado, deve permanecer explicitamente preservada e coberta como incompleta; não se deve recuperar referências por texto. Ambiguity, unresolved e unsupported permanecem estados do resolver recebido, não são resolvidos pela AST.

## Fora de escopo

- Implementação neste checkpoint, alterações em `src/main`, grammar, snapshots ou baselines.
- Alteração de `ReferenceResolution`, `ResolutionContracts`, `SymbolTable`, candidate filtering, scope walk, qualification ou diagnostics existentes.
- `ConditionSemantics`, `ConditionValidation`, CFG, dataflow, valores de runtime e targets dinâmicos.
- Validação type-sensitive de comparação e validação normativa completa de SEARCH ALL.
- Reparse de `writtenText`, regex, token scan e coleta direta de parse tree.
- Regressão ampla de corpus do Slice 7.

## Regras de domínio relacionadas

`docs/domain/conditional-expressions.md`, `docs/domain/semantic-ast.md`, `docs/domain/reference-resolution.md` e `docs/domain/provenance.md`.

## ADRs/invariantes relacionados

ADR-0012; INV-AST-001, INV-AST-002, INV-AST-003, INV-PROV-002, INV-COND-001, INV-COND-002, INV-RES-001, INV-RES-002 e INV-DET-001.

## Evidência do pipeline atual

```text
searchStatement
  └─ searchWhen+
       └─ condition
            └─ relationCondition / conditionNameReference / ...
```

`AstBuilder.visitSearchStatement` chama `preserved(ctx)`. `buildStructuredStatement` chama `collectStatementOperands` e procura `isFlowClauseContext`; `searchWhen` vira `StatementClause` com `nestedStatements`, mas `buildStatementClause` fixa `recognizedNodes` como lista vazia. Portanto o `condition` é visitado pela grammar, porém não é convertido em AST. `Ast.children(PreservedStatement)` só devolve operands e clauses; como a clause não tem recognized condition, o subtree não contém nenhuma condition node.

O collector tem a branch `PreservedStatement` → `visitStatementOperands`; ele visita os operands materializados e as clauses. Como `StatementClause.recognizedNodes()` está vazia, o collector nunca executa `visitConditionSurface` para `searchWhen`. O resolver recebe somente as occurrences que sobraram: `TABLE-ITEM` e operands de relations completas, mais operands/payloads preservados que o generic path reconheça. `FLAG-ON`, `SEARCH-C` em `A = B OR C`, a condition root qualificada e `NOT` desaparecem antes de occurrences; consequentemente não há resolution entry nem diagnostic para esses usos.

## Alternativas

- **A — recomendada:** `SearchStatement` + `SearchWhen` tipados e chamada ao lowering de condition existente. O collector adiciona somente routing tipado no boundary de `SearchStatement`/`SearchWhen`: `condition` chama `visitConditionSurface`, enquanto `Ast.children` continua a traversal estrutural. Assim relation, contextual tail, logical, negated, distributed e qualification/subscript policies são reaproveitadas sem policy especial de SEARCH.
- **B — rejeitada salvo prova de inviabilidade de A:** policy especial de SEARCH no collector. Violaria a fronteira AST → occurrences e duplicaria regras já existentes.
- **C — rejeitada:** reparse de source text, regex ou token scan do statement preservado.
- **D — rejeitada:** coletar diretamente da parse tree no collector, contornando AST, IDs e provenance.

Não há decisão arquitetural material aberta depois da caracterização; a mudança futura é um slice de lowering/traversal localizado e não exige alteração do resolver.

## Efeitos do preserved path e migração

Hoje `searchStatement` e `searchWhen` aparecem no manifest como `PRESERVED_UNINTERPRETED`/`DEPENDENCY_UNKNOWN`. A futura implementação deve classificar a fronteira materializada como `MODELED` sem apagar a distinção de `all` ou os gaps de validação. O `SearchStatement` substituirá um único preserved container; operands de tabela, varying, relation e qualifiers não podem ser coletados duas vezes. A migração deve:

1. construir cada referência escrita uma vez através da nova AST;
2. remover a coleta equivalente do generic preserved path para SEARCH;
3. conservar statements por WHEN e AT END no mesmo ownership;
4. aceitar mudanças legítimas de type counts, IDs locais, snapshots e cardinalidades somente após oracles explicarem a nova superfície;
5. manter `AstSnapshot`/`CoverageSnapshot` genericamente consistentes com a nova classe e atualizar seus testes apenas se o novo tipo alterar métricas intencionais.

## Decisão de saída

`READY_FOR_IMPLEMENTATION`. Os testes S1–S6, o controle negativo e a inspeção de código fecharam sem alterar produção. A autorização de implementação continua dependente de review humano posterior.
