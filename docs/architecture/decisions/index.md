# Índice de decisões arquiteturais

ADRs são a fonte canônica para decisões arquiteturais aceitas. Eles não devem ser usados para registrar cada bug, classe ou regra pura da linguagem COBOL.

## Decisões aceitas

| ID | Decisão | Tipo |
| --- | --- | --- |
| ADR-0001 | [Normalização fechada de comment entries](0001-comment-entry-normalization.md) | Contemporary |
| ADR-0002 | [Provenance começa no fonte físico](0002-provenance-originates-in-physical-source.md) | Retrospective |
| ADR-0003 | [Produtos de análise semântica permanecem separados](0003-separate-semantic-analysis-products.md) | Retrospective |
| ADR-0004 | [Binding nominal não resolve valores de runtime](0004-nominal-binding-excludes-runtime-values.md) | Retrospective |
| ADR-0005 | [Program unit é fronteira de identidade e análise](0005-program-unit-is-analysis-boundary.md) | Retrospective |
| ADR-0006 | [Programas externos dependem de catálogo explícito](0006-explicit-external-program-catalog.md) | Retrospective, superseded |
| ADR-0007 | [Linguagens embarcadas usam fronteiras dedicadas](0007-dedicated-embedded-language-boundaries.md) | Retrospective |
| ADR-0008 | [Incompletude é resultado de primeira classe](0008-incompleteness-is-first-class.md) | Retrospective |
| ADR-0009 | [Superfície gramatical possui cobertura explícita](0009-explicit-grammar-coverage.md) | Retrospective |
| ADR-0010 | [Dependências literais externas são observadas por artefato](0010-observe-external-literal-dependencies-per-artifact.md) | Contemporary |
| ADR-0011 | [Classificação de plataforma permanece ortogonal à semântica COBOL](0011-orthogonal-platform-classification.md) | Contemporary |
| ADR-0012 | [Condições contextuais usam projeção pós-binding](0012-contextual-conditions-use-post-binding-projection.md) | Contemporary |

ADRs retrospectivos só são criados quando código, testes e fontes documentais sustentam a decisão; não se inventa narrativa histórica.

Formato esperado:

```text
ADR-XXXX — título
Status: Accepted | Superseded | Deprecated | Proposed
Type: Contemporary | Retrospective
Recorded: YYYY-MM-DD
```

Um ADR retrospectivo declara que a decisão o antecede e aponta para a evidência atual que permitiu reconstruí-la.
