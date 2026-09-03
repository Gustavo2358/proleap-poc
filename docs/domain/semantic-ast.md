# AST semântica

## Propósito e escopo

`Ast` é o modelo estrutural imutável produzido a partir da parse tree. Ele aproxima conceitos COBOL úteis às análises posteriores sem armazenar symbol tables, bindings, CFG ou dataflow.

## Entradas e saídas

- **Entrada:** parser/parse tree, texto preprocessado, `SourceMap` e índices de origem da parse tree.
- **Saída:** programs por compilation unit, coverage, diagnostics e metadados de origem em cada nó.

Cada `Meta` contém ID, span, origem gramatical e provenance. Dentro de cada `ProgramUnit`,
o ID de um `Ast.Node` é sua posição no pre-order canônico definido por `Ast.children`,
começando em zero.

Esse namespace estrutural é local à unit, determinístico e contíguo: a traversal
canônica de uma árvore com `N` nodes produz exatamente os IDs `0..N-1`, sem ciclos,
filhos nulos ou compartilhamento da mesma instância por mais de um caminho. Metadata
de diagnostics e de outros produtos que não implementam `Ast.Node` não recebe uma
nova identidade nesse namespace; quando precisa apontar para uma declaração, reutiliza
a `Meta` do node estrutural correspondente.

Pre-order e contiguidade são contrato representacional, não semântica COBOL nem
igualdade com ordem textual ou parse-tree order. Os IDs não prometem estabilidade
entre edições do fonte. Consumidores futuros não podem inferir deles source order,
ordem de execução ou qualquer propriedade adicional sem nova decisão arquitetural.

## Superfície modelada

O modelo inclui program attributes, divisions, sections, arquivos, hierarquia de dados, clauses relevantes, assinatura de procedure, sentences, statements de fluxo/valor, expressões estruturadas e referências nominais DATA/INDEX/FILE/PROGRAM/PROCEDURE. Operações preservam a grafia do operador e uma categoria semântica tipada; relações são classificadas pelo contexto `relationArithmeticComparison`, não por reconhecimento posterior da string. Em `SET`, o alvo de `setToStatement` cujo valor é `booleanLiteral` carrega contexto `SET_CONDITION_TARGET`; os demais operandos de SET preservam `SET_DATA_OR_INDEX`. Em `EVALUATE`, cada selector preserva expressão, índice do subject correspondente por `ALSO` e contexto derivado de `evaluateSelect`, `evaluateCondition`, `evaluateValue`, `evaluateThrough` e `booleanLiteral`: somente identifier nominal direto ligado a subject `TRUE`/`FALSE` recebe `BOOLEAN_SUBJECT_NOMINAL`; comparações nominais de valor recebem `VALUE_COMPARISON`; literal, intervalo, condição composta e selector sem subject posicional correspondente ficam `OTHER`. `CALL` separa forma sintática literal de identifier/expression; linkage efetivo pertence à resolução.

Statements ou clauses parcialmente compreendidos usam `Modeled*`, `Preserved*` ou `Unsupported*` com regra, texto, filhos/referências alcançáveis e coverage correspondente. `EXEC SQL`, `EXEC CICS` e `EXEC SQLIMS` permanecem `EmbeddedLanguageStatement` opacos.

Condições combinadas/abreviadas possuem surface lossless desde `WORK-COND-003` (Slice 3 de `BACKLOG-COND-001`): `LogicalCondition` (AND/OR com precedência estrutural, sem `MIXED_LOGICAL`), `GroupedCondition` (parênteses escritos com boundary observável), `RelationCondition` (subject/operator escritos ou `null` quando omitidos; `NOT` relacional integra o operator canônico; object pode ser `DistributedOperandGroup`), `NegatedCondition` (NOT lógico sobre o fragmento imediato), `ContextualConditionTail` (bare nominal cuja interpretação DATA/INDEX/CONDITION depende do binding) e `ClassCondition` (simple condition estrutural). Nenhum node é criado para subject/operator omitidos e nenhuma expansão é materializada: a especialização pertence ao produto pós-binding futuro (`ConditionSemantics`), conforme ADR-0012. Occurrences contextuais e a remoção do acoplamento do collector ao `grammarRule` `conditionNameReference` pertencem ao Slice 5.

Desde `WORK-COND-004` (Slice 4), uma condition-name reference verdadeira produz um `DataReference` estruturalmente completo, construído dos children diretos do context: `baseName` vem de `conditionName()`, os qualifiers `IN`/`OF` escritos são preservados em ordem como `DataQualifier` e cada `conditionNameSubscriptReference` é materializado como `SubscriptGroup` com subscripts como expression children (um subscript qualificado preserva os próprios qualifiers, nunca promovidos à referência raiz). O alvo de qualifier deriva da posição na rule, não do branch ANTLR: posições não-finais são `DATA`; a última posição é `UNSPECIFIED`, porque a parse tree atual não classifica DATA/FILE/MNEMONIC atrás do branch `inData`. O resolver consome `UNSPECIFIED` por mapeamento compatibility-preserving para `{DATA}` (candidate universe inalterado); a classificação completa DATA/FILE depende de `BACKLOG-RES-004`. Provenance segue o Contrato A: `DataReference.meta` cobre a referência inteira, cada `DataQualifier` cobre a qualification escrita, cada `SubscriptGroup` cobre o grupo escrito e cada subscript tem meta própria; o baseName não possui Meta independente.

## Fronteiras e incerteza

- AST não executa lookup nem guarda candidate IDs.
- Texto é preservado para fidelidade, não reparsed para recuperar estrutura já disponível na gramática.
- `PRESERVED_UNINTERPRETED`, `UNSUPPORTED` e `DEPENDENCY_UNKNOWN` continuam visíveis.
- Modelagem de um statement para name binding não implica que seus efeitos de memória ou controle estejam interpretados.
- Cada `Statement`, `DataEntry`, `DataClause` e `PreservedExpression` materializado possui exatamente um finding concreto com o mesmo `Meta`, provenance e `astNodeId`; wrappers gramaticais que apenas encaminham um filho não criam finding adicional.
- `ConstructionCoverage.MODELED` descreve estrutura tipada, não completude semântica. Entries DATA tipadas são containers não dependency-bearing porque suas clauses possuem findings próprios. `PICTURE` e `USAGE` são não dependency-bearing para a capability nominal atual, sem alegar layout; `VALUE`, `OCCURS`, `REDEFINES` e `RENAMES` permanecem `DEPENDENCY_UNKNOWN` porque valores, cardinalidade ou aliases afetam consumidores futuros. Containers SQL e fallbacks realmente opacos continuam `PRESERVED_UNINTERPRETED`.

## Construção, complexidade e provenance

O `AstBuilder` despacha as fronteiras semânticas por `CobolBaseVisitor` e contextos gerados do ANTLR. Decisões como atributos de programa, declaradores de DATA, categorias de operação, terminadores e alternativas de statement usam accessors, filhos e tokens diretos do contexto tipado; nomes de regra e texto permanecem somente como metadados ou fidelidade lexical. Helpers locais podem percorrer subárvores para relações que a produção não separa em contextos próprios, mas não substituem decisões estruturais já expressas pela gramática.

A construção cria o inventário semântico em ordem determinística. Metadados carregam source span, parse origin, arquivo físico e include chain por composição do `SourceMap`.

## Evidência executável

`AstBuilderTest`, `AstBuildCoverageTest`, `AstBuilderTypedTraversalTest`, `StructuredExpressionAstTest`, `StatementModelAstTest`, `NominalReferenceAstTest`, `DeclarationModelAstTest`, `AstSemanticBoundaryCharacterizationTest`, `AstSemanticBoundaryRequiredOracleTest` e fixtures em `src/test/resources/cobol/semantic/`.

## Relações

Evals: EVAL-AST-001 a EVAL-AST-005, EVAL-COV-001, EVAL-COV-002 e EVAL-ARCH-001. Invariantes: INV-AST-001 a INV-AST-003, INV-COND-001, INV-COND-002, INV-PROV-002, INV-COV-001, INV-COV-002 e INV-EMB-001. ADRs: ADR-0002, ADR-0003, ADR-0005, ADR-0007, ADR-0008, ADR-0009 e ADR-0012.
