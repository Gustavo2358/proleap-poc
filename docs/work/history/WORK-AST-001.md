# WORK-AST-001 — Construção da AST dirigida por contextos tipados

Status: concluído em 2026-08-29. Risco: médio.

O `AstBuilder` passou a usar `CobolBaseVisitor<Ast.Node>` e contextos gerados nos despachos e nas decisões estruturais relevantes. A API externa continua aceitando `Parser`, mas valida internamente o `CobolParser` configurado.

Os oráculos adversariais cobrem atributos de programa cujo nome coincide com keyword, terminadores de `IF` e `EVALUATE` aninhados, `WHEN OTHER` direto e `FILLER ... REDEFINES`. Eles confirmam que buscas recursivas genéricas podiam selecionar elementos de construções aninhadas ou de outro papel gramatical.

A exceção deliberada é a leitura delimitada de subscritos de `tableCall`: a gramática não expõe um contexto por grupo. O manifesto de cobertura continua sendo a garantia fechada de cobertura; Visitor não o substitui.

As correções removeram nós e referências espúrios no corpus COACTUPC. Os baselines e artefatos regenerados registram a alteração aprovada, incluindo AST de 9.189 para 9.127 nós e referências de 3.058 para 3.042.

Evidências: `AstBuilderTypedTraversalTest`, `SemanticModelBaselineCharacterizationTest`, `check-fast`, `check-semantic`, `source-normalizer-regression.sh full` e `verify-naming.sh`.
