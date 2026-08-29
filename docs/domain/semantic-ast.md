# AST semântica

## Propósito e escopo

`Ast` é o modelo estrutural imutável produzido a partir da parse tree. Ele aproxima conceitos COBOL úteis às análises posteriores sem armazenar symbol tables, bindings, CFG ou dataflow.

## Entradas e saídas

- **Entrada:** parser/parse tree, texto preprocessado, `SourceMap` e índices de origem da parse tree.
- **Saída:** programs por compilation unit, coverage, diagnostics e metadados de origem em cada nó.

Cada `Meta` contém ID, span, origem gramatical e provenance. IDs seguem ordem determinística de construção.

## Superfície modelada

O modelo inclui program attributes, divisions, sections, arquivos, hierarquia de dados, clauses relevantes, assinatura de procedure, sentences, statements de fluxo/valor, expressões estruturadas e referências nominais DATA/INDEX/FILE/PROGRAM/PROCEDURE. `CALL` separa forma sintática literal de identifier/expression; linkage efetivo pertence à resolução.

Statements ou clauses parcialmente compreendidos usam `Modeled*`, `Preserved*` ou `Unsupported*` com regra, texto, filhos/referências alcançáveis e coverage correspondente. `EXEC SQL`, `EXEC CICS` e `EXEC SQLIMS` permanecem `EmbeddedLanguageStatement` opacos.

## Fronteiras e incerteza

- AST não executa lookup nem guarda candidate IDs.
- Texto é preservado para fidelidade, não reparsed para recuperar estrutura já disponível na gramática.
- `PRESERVED_UNINTERPRETED`, `UNSUPPORTED` e `DEPENDENCY_UNKNOWN` continuam visíveis.
- Modelagem de um statement para name binding não implica que seus efeitos de memória ou controle estejam interpretados.

## Construção, complexidade e provenance

O `AstBuilder` despacha as fronteiras semânticas por `CobolBaseVisitor` e contextos gerados do ANTLR. Decisões como atributos de programa, terminadores e alternativas de statement usam filhos e tokens diretos do contexto tipado; nomes de regra e texto permanecem somente como metadados ou fidelidade lexical. Helpers locais podem percorrer subárvores para relações que a produção não separa em contextos próprios, mas não substituem decisões estruturais já expressas pela gramática.

A construção cria o inventário semântico em ordem determinística. Metadados carregam source span, parse origin, arquivo físico e include chain por composição do `SourceMap`.

## Evidência executável

`AstBuilderTest`, `AstBuildCoverageTest`, `AstBuilderTypedTraversalTest`, `StructuredExpressionAstTest`, `StatementModelAstTest`, `NominalReferenceAstTest`, `DeclarationModelAstTest` e fixtures em `src/test/resources/cobol/semantic/`.

## Relações

Evals: EVAL-AST-001 a EVAL-AST-004, EVAL-COV-001, EVAL-COV-002 e EVAL-ARCH-001. Invariantes: INV-AST-001, INV-AST-002, INV-PROV-002, INV-COV-001, INV-COV-002 e INV-EMB-001. ADRs: ADR-0002, ADR-0003, ADR-0007, ADR-0008 e ADR-0009.
